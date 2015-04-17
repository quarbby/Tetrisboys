import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerSkeleton implements PlayerListener {
	
	volatile static int running = 2;
	
	int NUM_OF_PLAYERS = 8; // clones
	int TIMES_TO_TRAIN = 100; // repeats of clone learning
	boolean SHOW_BOARD = false; // display the tetris board?
	
	// change this to FALSE to only run once for "exploitation"
	final static boolean TRAINING_MODE = false;
	
	static int trained = 0;
	
	public Trainer trainer = new Trainer();	

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		int bestMove = trainer.pickMove(s, legalMoves);
		return bestMove;
	}
	
	@Override
	public synchronized void setRunning(int i) {
		running = i;
	}
	
	@Override
	public synchronized void reportFinished() {
		// threads will report finished when done.
		// this decrements the number of running threads.
		if (TRAINING_MODE) {
			running -= 1;
			if (running == 0) {
				System.out.println("save files");
				CountsAndValues.saveAndExitInstance();
				
				// when no more running threads,
				// go to next iteration
				train();
			}
		} else {
			System.out.println("save files");
			CountsAndValues.saveAndExitInstance();
		}
	}
	
	public void train() {

		System.out.println("read files");
		CountsAndValues.getInstance();
		
		// training mode on: we train for x times.
		if (TRAINING_MODE) {
			// if we have trained to the max,
			// then stop training and reset the count. 
			if (trained >= TIMES_TO_TRAIN) {
				trained = 0;
				return;
			}
			
			System.out.println("Training set " + trained);
			
			// else we can still train.
			// remember the number of running threads,
			// then run the threads.
			setRunning(NUM_OF_PLAYERS);
			for (int i = 0; i < NUM_OF_PLAYERS; i++) {
				new Thread(new Player(i, SHOW_BOARD, this)).start();
			}
			
			// increment the number of times we trained
			trained++;
		} else {
			// training mode off: just play once.
			new Thread(new Player(0, true, this)).start();
		}
	}
	
	public static void main(String[] args) {
		new PlayerSkeleton().train();
	}
	
}

interface PlayerListener {
	void setRunning(int i);
	void reportFinished();
}

class Player implements Runnable {
	
	int i;
	boolean q;
	PlayerListener p;
	
	public Player(int i, boolean q, PlayerListener p) {
		this.i = i;
		this.q = q;
		this.p = p;
	}

	@Override
	public void run() {
		System.out.println("Starting thread " + i);
		
		State s = new State();
		TFrame t = q ? new TFrame(s) : null;
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			if (q) {
				s.draw();
				s.drawNext(0,0);
			}
			//System.out.println(s.getRowsCleared());
			if (s.getRowsCleared() % 1000 == 0) {
				//System.out.println("Thread " + i + " " + s.getRowsCleared());
			}
		}
		
		System.out.println("Thread " + i + " ended");
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
		
		if (q) {
			t.dispose();
		}
		
		p.reportFinished();
	}
}

class Trainer {

	CountsAndValues c;

	public Trainer() {
		c = CountsAndValues.getInstance();
	}
	
	public int pull_arm(){
		//  choose an arm to pull
		int weight = 0; // by default set to size - 1
		int total_counts = 0;

		for (int m = 0; m < c.counts_size(); m++){
			total_counts = total_counts + c.counts_get(m);
			if (c.counts_get(m) == 0){
				return m;
			}
		}

		double max_reward = -1.0;

		for (int o = 0; o < c.rewards_size(); o++) {
			double bonus = Math.pow(
					(Math.log(total_counts + 1) * 1.0) / Math.log((c.counts_get(o)) + 1), 0.5
					) * (1.0 / 500);
			if (c.rewards_get(o) + bonus > max_reward) {
				max_reward = c.rewards_get(o) + bonus;
				weight = o;
			}
		}
		
		return weight;
	}
	
	
	public int pickMove(State old_state, int[][] legalMoves){
		NewState original = convertState(old_state);
		
		int[][] field = original.getField();
		int nextPiece = original.getNextPiece();
		int top[] = original.getTop();	
		int turn = original.getTurnNumber();
		
		double max_reward = -100000000000000000000.0;
		int move = 0;
		int weight = pull_arm();
		double max_row = 0.0;
		int converted_weight = Integer.parseInt(convert(weight, c.range));
		int[] array_weights = new int[c.num_of_arms];
		
		String temp = Integer.toString(converted_weight);

		int counter = 0;

		for (int i = temp.length() - 1; i >= 0; i--) {
			int index = c.num_of_arms - 1 - counter;
		    array_weights[index] = temp.charAt(i) - '0';
			counter++;
		}
	
		for (int i=0; i<legalMoves.length; i++) {
			NewState s = new NewState(field, nextPiece, top, turn);
			s.newMove(i);
			double row_cleared = s.getRowsCleared();
			double reward = 0.0;
			
			// add 1 because weight starts from 0 => we can be sure that we want to include the weight inside
			
			reward += -1.0 * Math.pow(array_weights[0] + 1.0, 2.0) * landingHeight(original, s, row_cleared);
			reward += Math.pow(array_weights[1] + 1.0, 2.0) * row_cleared;
			reward += -1.0 * Math.pow(array_weights[2] + 1.0, 2.0) * rowTransitions(s);
			reward += -1.0 * Math.pow(array_weights[3] + 1.0, 2.0) * colTransitions(s);
			reward += -1.0 * Math.pow(array_weights[4] + 1.0, 2.0) * getNumHoles(s);
			reward += -1.0 * Math.pow(array_weights[5] + 1.0, 2.0) * sum_of_wells(s);
						
			if (reward > max_reward){
				max_reward = reward;
				move = i;
				max_row = row_cleared;
			}
			
			// the bigger the reward the better
		}
		
		c.counts_set(weight, c.counts_get(weight) + 1);
		int n = c.counts_get(weight);
		double new_reward = ( (n - 1) / (n * 1.0)) * c.rewards_get(weight) + (1.0 / n) * (max_row * 1.0);
		c.rewards_set(weight, new_reward);
		
		return move;
	}
	
	private double landingHeight(NewState original, NewState after, double row_cleared){
		int[] oldHeights = original.getTop();
		int[] newHeights = after.getTop();
		
		// find the max height difference
		int col = 0;
		double max_diff = 0.0;
		
		for (int i = 0; i < oldHeights.length; i++){
			if (newHeights[i] + row_cleared != oldHeights[i]){
				// compare difference
				if (max_diff < newHeights[i] + row_cleared - oldHeights[i]){
					col = i;
					max_diff = newHeights[i] + row_cleared - oldHeights[i];
				}
			}
		}
		
		return oldHeights[col] + (max_diff / 2.0);
		
	}
	
	private double rowTransitions(NewState s){
		int[][] field = s.getField();
		
		// normalize everything to one for easy comparison
		for (int i = 0; i < field.length; i++){
			for (int j = 0; j < field[i].length; j++){
				if (field[i][j] != 0){
					field[i][j] = 1;
				}
			}
		}
		
		int sum = 0;
		for (int i = 0; i < 20; i++){
			
			// wrap around check from the other side
			int prev = field[i][9];
			for (int j = 0; j < field[i].length; j++){
				
				if (field[i][j] != prev){
					sum++;
				}
				prev = field[i][j];
			}
		}
		
		return (sum * 1.0);
	}

	private double colTransitions(NewState s){
		int[][] field = s.getField();
		
		// normalize everything to one for easy comparison
		for (int i = 0; i < field.length; i++){
			for (int j = 0; j < field[i].length; j++){
				if (field[i][j] != 0){
					field[i][j] = 1;
				}
			}
		}
		
		int sum = 0;
		
		// wrap around check from the other side
		for (int i = 0; i < field[0].length; i++){
			
			int prev = 1;
			for (int j = 0; j < 20; j++){
				if (field[j][i] != prev){
					sum++;
				}
				prev = field[j][i];
			}
		}
		
		return (sum * 1.0);
	}
	
	private double sum_of_wells(NewState s){

		int[][] field = s.getField();
		
		int[] colHeights = s.getTop();
		
		int sum = 0;
		
		for (int i = 19; i >= 0; i--){
				
			for (int j = 0; j < 10; j++){
				
				if (i < colHeights[j]){
					
					// do not count blocks below the top block
					continue;
				}
				
				int left = 1;
				int right = 1;
				
				if (j != 0){
					left = field[i][j - 1];
				}
				
				if (j != 9){
					right = field[i][j + 1];
				}
				
				if (left != 0 && right != 0 && field[i][j] == 0){
					// drill down for more
					sum++;
					int counter = i - 1;
					while (counter >= 0 && field[counter][j] == 0){
						sum++;
						counter--;
					}
				}
			}
		}
		
		return (sum * 1.0);
	}
	
	private double getNumHoles(NewState s) {
		int numHoles = 0;
		int[][] field = s.getField();
		int[] colHeights = s.getTop();
		int total = 0;
		
		for (int i = 0; i < colHeights.length; i++) {

			int count = 0;
			for (int j = 0; j < colHeights[i]; j++){
				// check if block at field[i][j] is occupied
				
				if (field[j][i] == 0){
					count++;
				}
			}
			numHoles = count + numHoles;
			total = total + colHeights[i];
		}
		
		return (numHoles * 1.0);
	}
	
	
	public static String convert(int number, int base) {
	    int quotient = number / base;
	    int remainder = number % base;

	    if(quotient == 0) { // base case
	        return Integer.toString(remainder);      
	    } else {
	        return convert(quotient, base) + Integer.toString(remainder);
	    }            
	}
	
	private NewState convertState(State curState) {
		NewState s = new NewState(curState.getField(), curState.getNextPiece(), curState.getTop(), curState.getTurnNumber());
		return s;
	}
	
}

class CountsAndValues {
	
	private List<Double> rewards = Collections.synchronizedList(new ArrayList<Double>());
	private List<Integer> counts = Collections.synchronizedList(new ArrayList<Integer>());

	final int num_of_arms = 6;
	final int range = 3; // 0 - 2 has 3 numbers
	final int total_num_of_combos = (int) Math.pow(range, num_of_arms);
	
	private static CountsAndValues instance = null;
	
	// methods
	
	public static CountsAndValues getInstance() {
		if (instance == null) {
			instance = new CountsAndValues();
		}
		return instance;
	}
	
	private CountsAndValues() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("rewards.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				rewards.add(Double.parseDouble(line.trim()));
			}
			br.close();
		} catch (IOException e) {
			// init rewards and count		
			for (int i = 0; i < total_num_of_combos; i++){
				rewards.add(0.0);
			}
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("counts.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				counts.add(Integer.parseInt(line.trim()));
			}
			br.close();
		} catch (IOException e) {
			// init rewards and count
			for (int i = 0; i < total_num_of_combos; i++){
				counts.add(0);
			}
		}
		
		if (counts.size() != total_num_of_combos){
			for (int i = 0; i < total_num_of_combos; i++){
				counts.add(0);
			}
		}		
		
		if (rewards.size() != total_num_of_combos){
			for (int i = 0; i < total_num_of_combos; i++){
				rewards.add(0.0);
			}
		}
	}
	
	public static void saveAndExitInstance() {

		String getWriteString = getWriteString(instance.getRewards());
		try{
			File file = new File("rewards.txt");
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(getWriteString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String getWriteAnotherString = getWriteAnotherString(instance.getCounts());
		
		try{
			File file = new File("counts.txt");
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(getWriteAnotherString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int num_of_arms() {
		return num_of_arms;
	}
	
	public int range() {
		return range;
	}
	
	public int total_num_of_combos() {
		return total_num_of_combos;
	}
	
	public int rewards_size() {
		return rewards.size();
	}
	
	public double rewards_get(int i) {
		return rewards.get(i);
	}
	
	public void rewards_set(int i, double v) {
		rewards.set(i, v);
	}
	
	public int counts_size() {
		return counts.size();
	}
	
	public int counts_get(int i) {
		return counts.get(i);
	}
	
	public void counts_set(int i, int v) {
		counts.set(i, v);
	}
	
	private List<Double> getRewards() {
		return rewards;
	}
	
	private List<Integer> getCounts() {
		return counts;
	}

	private static String getWriteString(List<Double> rewards2) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < rewards2.size(); i++) {
			sb.append(rewards2.get(i) + "\n");
		}
		return sb.toString();
	}
	
	private static String getWriteAnotherString(List<Integer> counts2) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < counts2.size(); i++) {
			sb.append(counts2.get(i) + "\n");
		}
		return sb.toString();
	}
	
}

class NewState {
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;

	public boolean lost = false;
	
	//current turn
	private int turn = 0;
	private int cleared = 0;
	
	//each square in the grid - int means empty - other values mean the turn it was placed
	private int[][] field = new int[ROWS][COLS];
	//top row+1 of each column
	//0 means empty
	private int[] top = new int[COLS];
	
	//number of next piece
	protected int nextPiece;
	
	//all legal moves - first index is piece type - then a list of 2-length arrays
	protected static int[][][] legalMoves = new int[N_PIECES][][];
	
	//indices for legalMoves
	public static final int ORIENT = 0;
	public static final int SLOT = 1;
	
	//possible orientations for a given piece type
	protected static int[] pOrients = {1,2,4,4,4,2,2};
	
	//the next several arrays define the piece vocabulary in detail
	//width of the pieces [piece ID][orientation]
	protected static int[][] pWidth = {
			{2},
			{1,4},
			{2,3,2,3},
			{2,3,2,3},
			{2,3,2,3},
			{3,2},
			{3,2}
	};
	//height of the pieces [piece ID][orientation]
	private static int[][] pHeight = {
			{2},
			{4,1},
			{3,2,3,2},
			{3,2,3,2},
			{3,2,3,2},
			{2,3},
			{2,3}
	};
	private static int[][][] pBottom = {
		{{0,0}},
		{{0},{0,0,0,0}},
		{{0,0},{0,1,1},{2,0},{0,0,0}},
		{{0,0},{0,0,0},{0,2},{1,1,0}},
		{{0,1},{1,0,1},{1,0},{0,0,0}},
		{{0,0,1},{1,0}},
		{{1,0,0},{0,1}}
	};
	private static int[][][] pTop = {
		{{2,2}},
		{{4},{1,1,1,1}},
		{{3,1},{2,2,2},{3,3},{1,1,2}},
		{{1,3},{2,1,1},{3,3},{2,2,2}},
		{{3,2},{2,2,2},{2,3},{1,2,1}},
		{{1,2,2},{3,2}},
		{{2,2,1},{2,3}}
	};
	
	//initialize legalMoves
	{
		//for each piece type
		for(int i = 0; i < N_PIECES; i++) {
			//figure number of legal moves
			int n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//number of locations in this orientation
				n += COLS+1-pWidth[i][j];
			}
			//allocate space
			legalMoves[i] = new int[n][2];
			//for each orientation
			n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//for each slot
				for(int k = 0; k < COLS+1-pWidth[i][j];k++) {
					legalMoves[i][n][ORIENT] = j;
					legalMoves[i][n][SLOT] = k;
					n++;
				}
			}
		}
	}
	
	
	public int[][] getField() {
		return field;
	}

	public int[] getTop() {
		return top;
	}

    public static int[] getpOrients() {
        return pOrients;
    }
    
    public static int[][] getpWidth() {
        return pWidth;
    }

    public static int[][] getpHeight() {
        return pHeight;
    }

    public static int[][][] getpBottom() {
        return pBottom;
    }

    public static int[][][] getpTop() {
        return pTop;
    }

	public int getNextPiece() {
		return nextPiece;
	}
	
	public boolean hasLost() {
		return lost;
	}
	
	public int getRowsCleared() {
		return cleared;
	}
	
	public int getTurnNumber() {
		return turn;
	}
	
	//constructor
	public NewState() {
		nextPiece = randomPiece();
	}
	
	//random integer, returns 0-6
	private int randomPiece() {
		return (int)(Math.random()*N_PIECES);
	}
	
	//gives legal moves for 
	public int[][] legalMoves() {
		return legalMoves[nextPiece];
	}
	
	//make a move based on the move index - its order in the legalMoves list
	public void newMove(int move) {
		newMove(legalMoves[nextPiece][move]);
	}
	
	//make a move based on an array of orient and slot
	public void newMove(int[] move) {
		newMove(move[ORIENT],move[SLOT]);
	}
	
	//returns false if you lose - true otherwise
	public boolean newMove(int orient, int slot) {

		//System.out.println("ORIENT:" + orient + "SLOT:" + slot);

		turn++;
		//height if the first column makes contact
				
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}
		
		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			lost = true;
			return false;
		}

		
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}
		
		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}
		
		int rowsCleared = 0;
		
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				cleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}
	

		//pick a new piece
		nextPiece = randomPiece();
		

		
		return true;
	}
	
	// extra functions
	
	public NewState(int[][] newField, int newNextPiece, int[] newTop, int newTurn) {
		
		// http://stackoverflow.com/questions/1564832/how-do-i-do-a-deep-copy-of-a-2d-array-in-java
		// only way to do a deep copy is to iterate through the array and copy each element
		for (int i = 0; i < newField.length; i++){
			for (int j  = 0; j < newField[i].length; j++){
				this.field[i][j] = newField[i][j];
			}
		}

		this.nextPiece = newNextPiece;
		
		this.turn = newTurn;
		
		for (int i = 0; i < newTop.length; i++){
			this.top[i] = newTop[i];
		}

	}
	
	
	public int[][][] allLegalMoves() {
		return legalMoves;
	}
	
	
}


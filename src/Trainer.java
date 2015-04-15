import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class Trainer {

	ArrayList<Double> rewards = new ArrayList<Double>();

	ArrayList<Integer> counts = new ArrayList<Integer>();

	int num_of_arms = 6;

	int range = 3; // 0 - 2 has 3 numbers

	public Trainer(){
		
		int total_num_of_combos = (int) Math.pow(range, num_of_arms);
		
		int count = 0;
		
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
	
	public int pull_arm(){
		//  choose an arm to pull
		
		double[] probs = new double[counts.size()];
		
		int weight = 0; // by default set to size - 1	

		int total_counts = 0;

		for (int m = 0; m < counts.size(); m++){
			
			total_counts = total_counts + counts.get(m);
			
			if (counts.get(m) == 0){
				return m;
			}
		}
		
			
				int max_weight = 0;

				double max_reward = -1.0;

				for (int o = 0; o < rewards.size(); o++){
					
					double bonus = Math.pow((  Math.log(total_counts + 1) * 1.0) / Math.log( (counts.get(o)) + 1), 0.5) * (1.0 / 500);
										
					if (rewards.get(o) + bonus > max_reward){
						max_reward = rewards.get(o) + bonus;
						weight = o;
					}
				}

		//System.out.println(weight);


			return weight;
	}

	public int pickMove(State old_state, int[][] legalMoves){
/*
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
*/
				
		NewState original = convertState(old_state);
		
		int[][] field = original.getField();
		int nextPiece = original.getNextPiece();
		int top[] = original.getTop();	
		int turn = original.getTurnNumber();
		
		double max_reward = -100000000000000000000.0;
				
		int move = 0;		

		int weight = pull_arm();
		
		double max_row = 0.0;
		
		//System.out.println(weight);
		
				
		///		
		
		int converted_weight = Integer.parseInt(convert(weight, range));


		
		int[] array_weights = new int[num_of_arms];
		
		String temp = Integer.toString(converted_weight);

		int counter = 0;

		for (int i = temp.length() - 1; i >= 0; i--)
		{
			
			
		    array_weights[num_of_arms - 1 - counter] = temp.charAt(i) - '0';
			counter++;
		}
	
		int[][][] allLegalMoves = original.allLegalMoves();
	
				
		for (int i=0; i<legalMoves.length; i++) {		
		
			NewState s = new NewState(field, nextPiece, top, turn);
			s.newMove(i);
			
			double row_cleared = s.getRowsCleared();

			double reward = 0.0;
			/*
			reward += -4.500158825082766 * landingHeight(original, s, row_cleared);
			
			reward += 3.4181268101392694 * row_cleared;
			
			reward += -3.2178882868487753 * rowTransitions(s);
			
			reward += -9.348695305445199 * colTransitions(s);
			
			reward += -7.899265427351652 * getNumHoles(s);
			
			reward += -3.3855972247263626 * sum_of_wells(s);
			*/
			
			// add 1 because weight starts from 0 => we can be sure that we want to include the weight inside
			
			reward += -1.0 * Math.pow(array_weights[0] + 1.0, 2.0) * landingHeight(original, s, row_cleared);

			reward += Math.pow(array_weights[1] + 1.0, 2.0) * row_cleared;

			reward += -1.0 * Math.pow(array_weights[2] + 1.0, 2.0) * rowTransitions(s);

			reward += -1.0 * Math.pow(array_weights[3] + 1.0, 2.0) * colTransitions(s);

			reward += -1.0 * Math.pow(array_weights[4] + 1.0, 2.0) * getNumHoles(s);

			reward += -1.0 * Math.pow(array_weights[5] + 1.0, 2.0) * sum_of_wells(s);
			
			// System.out.println(array_weights[0] + "" + array_weights[1] + "" + array_weights[2] + "" + array_weights[3] + "" + array_weights[4] + "" + array_weights[5]);
			
			if (reward > max_reward){
				max_reward = reward;
				move = i;
				max_row = row_cleared;
			}
			
			// the bigger the reward the better
		}
				
		
		System.out.println(weight);
		
		counts.set(weight, counts.get(weight) + 1);
				
		int n = counts.get(weight);
				
		double new_reward = ( (n - 1) / (n * 1.0)) * rewards.get(weight) + (1.0 / n) * (max_row * 1.0);
		
		rewards.set(weight, new_reward);
		
		
		String getWriteString = getWriteString(rewards);
		try{
			File file = new File("rewards.txt");
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(getWriteString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String getWriteAnotherString = getWriteAnotherString(counts);
		
		try{
			File file = new File("counts.txt");
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(getWriteAnotherString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
	
	private double totalBlocks(NewState s){
		
		int[][] field = s.getField();
		
		double sum = 0.0;
		
		for (int i = 0; i < field.length; i++) {
			
			for (int j = 0; j < field[i].length; j++){
				
				if (field[i][j] != 0){
					sum = sum + 1.0;
				}
				
			}
			
		}
		
		return sum;
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
	
	private double height_diff_from_max(NewState s){
		
		int count = 0;
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		int max_height = getMaxHeight(s);
		
		for (int i = 0; i < colHeights.length; i++) {
			sum = sum + colHeights[i];
		}
		
		//System.out.println((10 * max_height) - sum);
		
		
		return ( (10 * max_height) - sum);
	}
	
	private double averageHeight(NewState s){
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length; i++) {
			sum = sum + colHeights[i];
		}
				
		return (sum / 10.0);
	}
	
	private double adjacentHeightDifferenceSquare(NewState s){
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length - 1; i++) {
			sum = sum + Math.pow(colHeights[i] - colHeights[i + 1], 2);
		}
				
		return sum;
	}
	
	private double percent_area_below_max_height(NewState s){
		int[][] field = s.getField();
		
		int max_height = getMaxHeight(s);
				
		int count = 0;
		
		for (int i=0; i<field.length; i++) {
			for (int j=0; j<field[0].length; j++) {
				if (field[i][j] != 0) {
					count++;
				}
			}
		}
		
		if (count == 0){
			return 1.5; // everything below area // no blocks
		}		
		
		return ( count / (getMaxHeight(s) * 10.0) );
	}
	
	private double compactness(NewState s){
		int[][] field = s.getField();
		
		int[] colHeights = s.getTop();
		
		double sum = 0.0; // sum the area occupied by the blocks
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length; i++) {
			sum = sum + colHeights[i];
		}
						
		int count = 0;
		
		for (int i=0; i<field.length; i++) {
			for (int j=0; j<field[0].length; j++) {
				if (field[i][j] != 0) {
					count++;
				}
			}
		}
		
		
		if (count == 0){
			return 1.5; // everything below area // no blocks
		}
		
						
		return ( count / (sum) );
	}
	
	
	public int getMaxHeight(NewState s){
		int[] colHeights = s.getTop();
		
		int max = -1;
		
		for (int i=0; i<colHeights.length; i++) {
			if (colHeights[i] > max) {
				max = colHeights[i]; 
			}
		}
		
		return max;
	}
	
	public int max_min_diff(NewState s){
		
		int[] colHeights = s.getTop();
		
		int min = 100;
		int max = -1;
		
		for (int i=0; i<colHeights.length; i++) {
			if (colHeights[i] > max) {
				max = colHeights[i]; 
			}
			
			if (colHeights[i] < min) {
				min = colHeights[i]; 
			}
		}
		
		return max-min;
	}
	
	public static String convert(int number, int base)
	{
	    int quotient = number / base;
	    int remainder = number % base;

	    if(quotient == 0) // base case
	    {
	        return Integer.toString(remainder);      
	    }
	    else
	    {
	        return convert(quotient, base) + Integer.toString(remainder);
	    }            
	}
	
	private NewState convertState(State curState) {
		NewState s = new NewState(curState.getField(), curState.getNextPiece(), curState.getTop(), curState.getTurnNumber());
		return s;
	}
	
	
	private static String getWriteString(ArrayList<Double> arr) {
		String writeString = "";
		
		for (int i=0; i<arr.size(); i++) {
			writeString = writeString + arr.get(i) + "\n";
		}
				
		return writeString;
	}
	
	private static String getWriteAnotherString(ArrayList<Integer> arr) {
		String writeString = "";
		
		for (int i=0; i<arr.size(); i++) {
			writeString = writeString + arr.get(i) + "\n";
		}
				
		return writeString;
	}
	
}
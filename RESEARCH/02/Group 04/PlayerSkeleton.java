import java.util.*;
public class PlayerSkeleton {

    double[] curFeatures;
    double[] prevFeautures;
    private Features4 featureFn;
    private NextState ns = null;
    private static double[] testW = new double[] {-7.720365371906888, 5.940915554719386, -0.12124613960138278, 0.27983173173592785, -2.477321983110656, -7.227672112968805, -0.14518808292558213, 3.1585370155208734, -1.1860124403007994, -0.7243385364234114};

    public PlayerSkeleton(){
    	featureFn = new Features4();
    }
    
    public PlayerSkeleton(double[] w){ //not used in this Player
    	featureFn = new Features4(w);
    }
    
	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		if (ns == null) ns = new NextState();
        ns.resetState(s);
        int maxMove = maxUtilityMove(s,legalMoves,curFeatures);
        return maxMove;
                
	}
	public int maxUtilityMove(State s, int[][] legalMoves, double[] features) {
        double maxScore = Double.NEGATIVE_INFINITY;
        int start = (int)(Math.random()*legalMoves.length);
        int move = (start+1)%(legalMoves.length);
        int maxMove = move;
        double score;
        while (move != start) {
            ns.makeMove(move);
            if (! ns.hasLost()) {
                score = featureFn.score(s,ns, legalMoves[move]);
                if (maxScore < score) {
                    maxMove = move;
                    maxScore = score;
                }
            }
            ns.resetState(s);
            move = (move+1)%(legalMoves.length);
        }
        return maxMove;
    }
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
		//	try {
				//Thread.sleep(1);
		//	} catch (InterruptedException e) {
		//		e.printStackTrace();
		//	}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
    public class NextState extends State {
    //current turn
    private int turn = 0;
    private int cleared = 0;

    //each square in the grid - int means empty - other values mean the turn it was placed
    private int[][] field = new int[ROWS][COLS];
    //top row+1 of each column
    //0 means empty
    private int[] top = new int[COLS];

    private int[][][] pBottom = State.getpBottom();
    private int[][] pHeight = State.getpHeight();
    private int[][][] pTop = State.getpTop();

    protected int nextPiece;

    public int[][] getField() {
        return field;
    }

    public int[] getTop() {
        return top;
    }
    public int getNextPiece() {
        return nextPiece;
    }
    public void setNextPiece(int nextPiece){
        this.nextPiece = nextPiece;
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
    //gives legal moves for
    public int[][] legalMoves() {
        return legalMoves[nextPiece];
    }
    //make a move based on the move index - its order in the legalMoves list
    public void makeMove(int move) {
        makeMove(legalMoves[nextPiece][move]);
    }

    //make a move based on an array of orient and slot
    public void makeMove(int[] move) {
        makeMove(move[ORIENT],move[SLOT]);
    }
    //returns false if you lose - true otherwise
    public boolean makeMove(int orient, int slot) {
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
                    while(top[c]>=1 && field[top[c]-1][c]==0)   top[c]--;
                }
            }
        }

        return true;
    }
    public void resetState(State s) {
        int[][] field = s.getField();
        this.nextPiece = s.getNextPiece();
        this.lost = s.hasLost();
        this.cleared = s.getRowsCleared();
        this.turn = s.getTurnNumber();
        Arrays.fill(this.top,0);
        for(int i=field.length-1;i>=0;i--) {
            System.arraycopy(field[i], 0, this.field[i], 0, field[i].length);
            for(int j=0;j<top.length;j++) if(top[j]==0 && field[i][j]>0) top[j]=i+1;
        }
    }
}

	public class Features4  {
    private int count = 0;
    
    /*
     *          Order of features follows ref1.pdf page 33
     */
    
    /*
     *  NOTES ON FEATURES
     * 
     *  DELTA prefix: Abs(Feature(State) - Feature(NextState))
     *  
     *  ABS_HEIGHT_DIFF: Absolute difference between heights of adjacent columns
     *  Example: Column 1 have height 8, column 2 have height 10
     *           ABS_HEIGHT_DIFF between col 1 and col 2 = 2
     *  SUM_ABS_HEIGHT_DIFF: Sum of ABS_HEIGHT_DIFF of col 1-2, col 2-3, ..., col 9-10
     */
    
    final private  int CONSTANT   = count++;
    final private  int ROWS_CLEARED = count++;
//    final private static int ROWS_CLEARED_SQUARED = count++; /**/
//    final private static int MAX_HEIGHT   = count++; //took this out from fc1
//    final private static int DELTA_MAX_HEIGHT = count++;
//    final private static int MAX_HEIGHT_SQUARED = count++; /**/
    final private  int COVERED_GAPS = count++;

    // final private static int DELTA_COVERED_GAPS = count++;
    // final private static int COVERED_GAPS_SQUARED = count++; /**/
    final private  int AVG_HEIGHT = count++;
    // final private static int DELTA_AVG_HEIGHT = count++;
    // final private static int AVG_HEIGHT_SQUARED = count++; /**/
    final private  int SUM_ABS_HEIGHT_DIFF = count++;
    // final private static int DELTA_SUM_ABS_HEIGHT_DIFF = count++;
    // final private static int SUM_ABS_HEIGHT_DIFF_SQUARED = count++; /**/
    final private  int SUM_SQUARED_HEIGHT_DIFF = count++; /**/
    //final private static int STACKS_ON_EXISTING_GAP = count++; /**/
    final private  int ROW_TRANSITIONS = count++;
    final private  int COLUMN_TRANSITIONS = count++;
    //final priva ic int BLOCKADE = count++;
    final private int IS_SUICIDE = count++;
    //wells
    //is over a gap
    final public int NUMBER_FEATURES = count;
    private double[] curFeatures = new double[NUMBER_FEATURES];
    private double[] pastFeatures = new double[NUMBER_FEATURES];

    double[] weights;
    public Features4(){
        Random random = new Random();
        //this is the set of weights we derived from iterative runs of our Breeder
        weights =  new double[] {
            -9.601434011245342E-6, 6.220389129888194E-8, -7.25722989951555E-7, 3.87295004028969E-8, -7.113950335019432E-8, -2.128328195173797E-8, -9.528753577395118E-7, -2.057322654518275E-8, 0.21522552000444517
                };
    }
    
    public Features4(double[] w){
        weights =  w;
    }
    
    //getFeatures calls evaluateFeatures, and does comparisons with the current state if necessary
    public double[] getFeatures(State s, NextState n, int[] move) {
        evaluateFeatures(s, pastFeatures);
        evaluateFeatures(n, curFeatures);
        
        curFeatures[ROWS_CLEARED] = curFeatures[ROWS_CLEARED] - pastFeatures[ROWS_CLEARED];
    //    curFeatures[ROWS_CLEARED_SQUARED] = Math.pow(curFeatures[ROWS_CLEARED], 2);
    //    curFeatures[DELTA_MAX_HEIGHT] = curFeatures[MAX_HEIGHT] - pastFeatures[MAX_HEIGHT];
    //    curFeatures[MAX_HEIGHT_SQUARED] = Math.pow(curFeatures[MAX_HEIGHT], 2);
    //    curFeatures[DELTA_COVERED_GAPS] = curFeatures[COVERED_GAPS] - pastFeatures[COVERED_GAPS];
    //    curFeatures[COVERED_GAPS_SQUARED] = Math.pow(curFeatures[COVERED_GAPS], 2);
    //    curFeatures[DELTA_AVG_HEIGHT] = curFeatures[AVG_HEIGHT] - pastFeatures[AVG_HEIGHT];
    //    curFeatures[AVG_HEIGHT_SQUARED] = Math.pow(curFeatures[AVG_HEIGHT], 2);
    //    curFeatures[DELTA_SUM_ABS_HEIGHT_DIFF] = curFeatures[SUM_ABS_HEIGHT_DIFF] - pastFeatures[SUM_ABS_HEIGHT_DIFF];
    //    curFeatures[SUM_ABS_HEIGHT_DIFF_SQUARED] = Math.pow(curFeatures[SUM_ABS_HEIGHT_DIFF], 2);
        return curFeatures;
    }

    //the function score calculates the score of a NextState based on our given set of features
    public double score(State s, NextState ns, int[] move) {
        double[] f = this.getFeatures(s,ns,move);
        double total = 0;
        for (int i =0 ;i < f.length; i++) {
            double score = f[i]*weights[i];
            total+=score;
        }
        return total;
    }

    //evaluateFeatures fills the features array
    public void evaluateFeatures(State s, double[] fs) {
        int[][] field = s.getField();
        int[] top = s.getTop();
        double maxHeight = 0,
               minHeight = Integer.MAX_VALUE,
               sumHeight = 0;

        for (int i =0;i < State.COLS; i++) {
            maxHeight = Math.max(maxHeight, top[i]);
            minHeight = Math.min(minHeight, top[i]);
            sumHeight += top[i];
        }
        evaluateCells(s,fs,field,top);
       // fs[CONSTANT] = 1;
        fs[ROWS_CLEARED] = s.getRowsCleared();
        //fs[MAX_HEIGHT] = maxHeight;
        fs[AVG_HEIGHT] = (sumHeight/State.COLS);
        if (s.hasLost()) {
            fs[IS_SUICIDE] = Double.NEGATIVE_INFINITY;
        } else {
            fs[IS_SUICIDE] = 0.0;
        }
        

        fs[SUM_ABS_HEIGHT_DIFF] = 0;
        fs[SUM_SQUARED_HEIGHT_DIFF] = 0;

        for (int i = 0; i < State.COLS - 1; i++){
            fs[SUM_ABS_HEIGHT_DIFF] += Math.abs(top[i] - top[i+1]);
            fs[SUM_SQUARED_HEIGHT_DIFF] += Math.pow(top[i] - top[i+1], 2);
        }
    }

    //evaluateCells is called by evaluateFeatures to fill in certain features that require cell by cell processing
    public void evaluateCells(State s, double[] fs, int [][] field, int[] top) {
        int coveredGaps = 0;
        int blockades = 0;
        int row_transitions = 0;
        int col_transitions = 0;
        for (int i= 0; i< State.ROWS; i++) {
            for (int j=0 ; j< State.COLS; j++) {
                if (field[i][j] == 0) { //if the cell is unoccupied
                    if (i < top[j]) { // if the cell is below an occupied cell
                        coveredGaps++; //add one to the coveredGaps counts
                        blockades += top[j] - i; //add all the occupied cells to the blockade count
                    }
                    try {
                        if (field[i][j+1] == 1) { //if an adjacent cell in the next column to the unoccupied cell is occupied
                            row_transitions++;
                        } 
                    } catch (Exception e) {
                        //do nothign
                    }
                    try {
                        if (field[i][j-1] == 1) { //if an adjacent cell in the previous column to the unoccupied cell is occupied
                            row_transitions++;
                        }                     
                    } catch (Exception e) {
                        //do nothign
                    }
                    try {
                        if (field[i+1][j] == 1) { //if an adjacent cell in the row above the unoccupied cell is occupied
                            col_transitions++;
                        }                        
                    } catch (Exception e) {
                        //do nothign
                    }
                    try {
                        if (field[i-1][j] == 1) { //if an adjacent cell in the row below the unoccupied cell is occupied
                            col_transitions++;
                        }                      
                    } catch (Exception e) {
                        //do nothign
                    }
                }
            }
        }

        fs[COVERED_GAPS] = coveredGaps;
        fs[ROW_TRANSITIONS] = row_transitions;
        fs[COLUMN_TRANSITIONS] = col_transitions;
        //fs[BLOCKADE] = blockades;
    }

}
}
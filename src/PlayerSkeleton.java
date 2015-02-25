import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class PlayerSkeleton {
	private static final int TIMES_TO_TRAIN = 3;	
	private static final int NUM_FEATURES = 22;
	private static final double LEARNING_RATE = 0.1;
	private static final String WEIGHTS_FILE = "weights.txt";
	
	private static double[] weights = new double[NUM_FEATURES];
	private static int[] curFeatures = new int[NUM_FEATURES];
	private static int[] moveFeatures = new int[NUM_FEATURES];
	
	public static void main(String[] args) {
		initialiseWeights();
		PlayerSkeleton p = new PlayerSkeleton();
		
		// TODO: Is there a way not to pop up so many TFrames? 
		for (int i=0; i<TIMES_TO_TRAIN; i++) {
			State s = new State();
			new TFrame(s);
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s,s.legalMoves()));
				s.draw();
				s.drawNext(0,0);
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("You have completed " + s.getRowsCleared() + " rows.");
		}
		
		saveWeights();
	}

	/**
	 * Reads from weights file 
	 */
	private static void initialiseWeights() {
		int i=0;
		try { 
		BufferedReader br = new BufferedReader(new FileReader(WEIGHTS_FILE));
		try {
			String line = br.readLine();
			
			while (line != null) {
				weights[i] = Double.parseDouble(line.trim());
				line = br.readLine();
				i++;
			}
		} finally {
			br.close();
		}
		} catch (IOException e) {
			generateRandomWeights();
		}
	}

	/**
	 * Save weights to file to use again
	 */
	private static void saveWeights() {
		String weightsString = getWeightsString();
		try{
			File file = new File(WEIGHTS_FILE);
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(weightsString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public int pickMove(State s, int[][] legalMoves) {
		int move = 0;
		
		curFeatures = getFeatures(s);
		double curValue = getValueFunction(curFeatures);
		
		move = getMoveWithMaxUtility(s, legalMoves);
		
		double moveValue = getValueFunction(moveFeatures);
		updateWeights(curValue, moveValue);
		
		/*
		Random rand = new Random();
		move = rand.nextInt(legalMoves.length);
		*/

		return move;
	}
	
	/**
	 * Choose the move from list of legalMoves with maximum utility
	 * @param curState
	 * @param legalMoves
	 * @return
	 */
	private int getMoveWithMaxUtility(State curState, int[][] legalMoves) {
		double maxUtility = -1.0;
		int reward = 0;
		int moveIndex = -1;
		
		for (int i=0; i<legalMoves.length; i++) {
			State s = cloneCurState(curState);
			s.makeMove(i);
			reward = s.getRowsCleared();
			
			int[] features = getFeatures(s);
			double moveValue = getValueFunction(features);
			double utility = reward + moveValue;
			
			// Find the move with highest utility
			if (utility > maxUtility) {
				moveIndex = i;
				moveFeatures = features;
				maxUtility = utility;
			}
		}
						
		// No valid move, play a random move
		// TODO: search further down the tree
		if (moveIndex < 0) {
			Random rand = new Random();
			moveIndex = rand.nextInt(legalMoves.length);
		}
		
		return moveIndex;
	}

	private State cloneCurState(State curState) {
		State s = new State(curState.getField(), curState.getNextPiece());
		return s;
	}

	/**
	 * Update weights via gradient descent
	 * Compares curFeatures to moveFeatures
	 */
	private void updateWeights(double curValue, double moveValue) {
		double targetMinusObj = moveValue - curValue;
		double[] changeInWeights = calculateChangeInWeights(targetMinusObj);
		updateIndividualWeights(changeInWeights);
	}
	

	/**
	 * To get the features as defined in project handout
	 * @param s
	 * @return array of features 
	 */
	private int[] getFeatures(State s) {
		int[] features = new int[NUM_FEATURES];
		features[0] = 1;	// First feature is always 1
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i=1; i<=colHeights.length; i++) {
			features[i] = colHeights[i-1];
		}
		
		// Features indexed 11 to 19 are absolute difference between adjacent col heights
		int j = 11;
		for (int i=1; i<=colHeights.length-1; i++) {
			features[j] = colHeights[i] - colHeights[i-1];
			j++;
		}
		
		// Feature 20 is maximum column height
		features[20] = getMaxHeight(colHeights);
		
		// Feature 21 is number of holes in wall 
		features[21] = getNumHoles(s);
		
		return features;
	}

	/*
	 * Helper methods for getFeatures
	 */
	
	private int getMaxHeight(int[] colHeights) {
		int max = -1;
		
		for (int i=0; i<colHeights.length; i++) {
			if (colHeights[i] > max) {
				max = colHeights[i]; 
			}
		}
		
		return max;
	}
	
	/**
	 * Count a hole if all eight around are walls or filled
	 * @param state
	 * @return number of holes
	 */
	private int getNumHoles(State s) {
		int numHoles = 0;
		int[][] field = s.getField();
		
		for (int i=0; i<field.length; i++) {
			for (int j=0; j<field[0].length; j++) {
				if (field[i][j] == 0) {
					if (isHole(s, field, i, j)) {
						numHoles++;
					}
				}
			}
		}
		
		
		return numHoles;
	}

	private boolean isHole(State s, int[][] field, int i, int j) {
		int count = 0;
		
		if (i-1 < 0 || j-1 < 0 || field[i-1][j-1] == 1) {
			count++;
		}
		
		if (i-1 < 0 || field[i-1][j] == 1) {
			count++;
		}
		
		if (i-1 < 0 || j+1 >= s.COLS || field[i-1][j+1] == 1) {
			count++;
		}
		
		if (j-1 < 0 || field[i][j-1] == 1) {
			count++;
		}
		
		if (j+1 >= s.COLS || field[i][j+1] == 1) {
			count++;
		}
		
		if (i+1 >= s.ROWS || j+1 >= s.COLS || field[i+1][j+1] == 1) {
			count++;
		}
		
		if (i+1 >= s.ROWS || field[i+1][j] == 1) {
			count++;
		}
		
		if (i+1 >= s.ROWS || j+1 >= s.COLS || field[i+1][j+1] == 1) {
			count++;
		}
		
		
		if (count == 8) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get the value function
	 * @param features
	 * @return Value of current board, i.e. summation of weights * features
	 */
	private static double getValueFunction(int[] features) {
		double value = 0.0;
		
		for (int i=0; i<weights.length; i++) {
			value += weights[i] * features[i];
		}
		
		return value;
	}
	
	/*
	 * Helper methods for file I/O
	 */
	
	private static void generateRandomWeights() {
		for (int i=0; i<weights.length; i++) {
			weights[i] = getRandomDouble();
		}		
	}
	
	private static double getRandomDouble() {
		Random rand = new Random();
		return rand.nextDouble();
	}
	
	private static String getWeightsString() {
		String weightString = null;
		
		for (int i=0; i<weights.length; i++) {
			weightString = weightString + weights[i] + "\n";
		}
		
		return weightString;
	}
	
	/*
	 * Helper methods for update Weights
	 */
	private double[] calculateChangeInWeights(double targetMinusObj) {
		double[] changeInWeights = new double[NUM_FEATURES];
		
		for (int i=0; i<changeInWeights.length; i++) {
			changeInWeights[i] = LEARNING_RATE * targetMinusObj * curFeatures[i];
		}
		
		return changeInWeights;
	}
	
	private void updateIndividualWeights(double[] changeInWeights) {
		for (int i=0; i<weights.length; i++) {
			weights[i] += changeInWeights[i];
		}
	}
	
}

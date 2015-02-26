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
	private static double hiddenNodeWeight = 0.3;
	
	private static double curValue = 0.0;
	private static double outputValue = 0.0;
	private static double hiddenNodeValue = 0.3;
	
	private static int[] curFeatures = new int[NUM_FEATURES];
	private static int[] moveFeatures = new int[NUM_FEATURES];
	
	// Temp var to toggle between neural and gradient descent for now. 
	// Eventually should switch to neural when it's working
	private static boolean isNeural = false; 	
	
	public static void main(String[] args) {
		initialiseWeights();
		PlayerSkeleton p = new PlayerSkeleton();
		isNeural = true;
		
		for (int i=0; i<TIMES_TO_TRAIN; i++) {
			State s = new State();
			new TFrame(s);
			while(!s.hasLost()) {
				if (isNeural) {
					s.makeMove(p.pickMoveNeuralNet(s,s.legalMoves()));
				} else {
					s.makeMove(p.pickMove(s,s.legalMoves()));
				}
				
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
		try {
			BufferedReader br = new BufferedReader(new FileReader(WEIGHTS_FILE));
			
			String line = null;
			while ((line = br.readLine()) != null) {
				for (int i=0; i<weights.length; i++) {
					weights[i] = Double.parseDouble(line.trim());
				}
				
				// Last weight is neural weight if neural
				if (isNeural) {
					hiddenNodeWeight = Double.parseDouble(line.trim());
				}
			}
			
			br.close();
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
		curValue = getValueFunction(curFeatures);
		
		move = getMoveWithMaxUtility(s, legalMoves);
		
		outputValue = getValueFunction(moveFeatures);
		updateWeights(curValue, outputValue);

		return move;
	}
	
	/**
	 * For using neural network with 1 hidden node and sigmoid function to pick move
	 * @param s
	 * @param legalMoves
	 * @return
	 */
	public int pickMoveNeuralNet(State s, int[][] legalMoves) {
		int move = 0;
		
		curFeatures = getFeatures(s);
		curValue = getValueFunctionNeural(curFeatures);
		move = getMoveWithMaxUtility(s, legalMoves);
		outputValue = getValueFunctionNeural(moveFeatures);
		
		updateWeightsNeural();
				
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
			double moveValue = 0.0;
			if (isNeural) {
				moveValue = getValueFunctionNeural(features);
			} else {
				moveValue = getValueFunction(features);
			}
			
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
			moveIndex = getRandomInteger(legalMoves.length);
		}
		
		return moveIndex;
	}

	private State cloneCurState(State curState) {
		State s = new State(curState.getField(), curState.getNextPiece(), curState.getTop());
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
	
    //================================================================================
    // Get Features of Board and Helper Methods
    //================================================================================

	/**
	 * To get the features as defined in project handout
	 * @param s
	 * @return array of features 
	 */
	private int[] getFeatures(State s) {
		int[] features = new int[NUM_FEATURES];
		// First feature is always 1
		features[0] = 1;	
		
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
	
    //================================================================================
    // Helper methods for file I/O
    //================================================================================
	
	private static void generateRandomWeights() {
		for (int i=0; i<weights.length; i++) {
			weights[i] = getRandomDouble();
		}		
	}
	
	private static String getWeightsString() {
		String weightString = "";
		
		for (int i=0; i<weights.length; i++) {
			weightString = weightString + weights[i] + "\n";
		}
		
		weightString += hiddenNodeWeight;
		
		return weightString;
	}
	
    //================================================================================
    // Helper Methods for Picking Move with Neural Network
    //================================================================================
	
	private double getValueFunctionNeural(int[] features) {
		double value = 0.0;
		
		double hiddenNodeVal = calculateWeightedSum(features);
		double sigmoidSum = calculateSigmoid(hiddenNodeVal);
		
		value = calculateSigmoid(sigmoidSum * hiddenNodeWeight);
		
		return value;
	}

	private double calculateWeightedSum(int[] features) {
		double sum = 0.0;
		
		for (int i=0; i<weights.length; i++) {
			sum += weights[i] * features[i];
		}
		
		return sum;
	}
	
	private double calculateSigmoid(double value) {
		return 1/(1+Math.pow(Math.E, -value));
	}

	private void updateWeightsNeural() {
		double deltaOutput = outputValue*(1-outputValue)*(outputValue-curValue);
		updateHiddenNodeWeight(deltaOutput); 
		
		//TODO: Check formula for deltaHidden
		double deltaHidden = hiddenNodeValue*(1-hiddenNodeValue)*(hiddenNodeWeight*deltaOutput);
		double[] changeInWeights = calculateChangeInWeightsNeural(deltaHidden);
		updateIndividualWeights(changeInWeights);
		
	}

	private void updateHiddenNodeWeight(double deltaOutput) {
		double changeInWeight = LEARNING_RATE * deltaOutput * hiddenNodeValue;
		hiddenNodeWeight = hiddenNodeWeight + changeInWeight;
	}
	
	private double[] calculateChangeInWeightsNeural(double deltaHidden) {
		double[] changeInWeights = new double[NUM_FEATURES];
		
		for (int i=0; i<NUM_FEATURES; i++) {
			changeInWeights[i] = LEARNING_RATE * deltaHidden * curFeatures[i];
			
		}
		
		return changeInWeights;
	}

    //================================================================================
    // Helper Methods for Update Weights
    //================================================================================
	
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
	
    //================================================================================
    // Other General Helper Methods 
    //================================================================================
	
	private static double getRandomDouble() {
		Random rand = new Random();
		return rand.nextDouble();
	}
	
	private static int getRandomInteger(int max) {
		Random rand = new Random();
		return rand.nextInt(max);
	}
}

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class PlayerSkeleton {
	private static final int TIMES_TO_TRAIN = 30;	
	private static final int NUM_FEATURES = 5;
	private static final double LEARNING_RATE = 0.1;
	private static final String WEIGHTS_FILE = "weights.txt";
	
	private static double[] weights = new double[NUM_FEATURES];
	private static double hiddenNodeWeight = 0.3;
	
	private static double curValue = 0.0;
	private static double outputValue = 0.0;
	private static double hiddenNodeValue = 0.3;
	
	private static double[] curFeatures = new double[NUM_FEATURES];
	private static double[] moveFeatures = new double[NUM_FEATURES];
	
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
					//Thread.sleep(300);
					Thread.sleep(5);
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
		
		int maxRow = 0;
		
		for (int i=0; i<legalMoves.length; i++) {
			State s = cloneCurState(curState);
			s.makeMove(i);
			reward = s.getRowsCleared();
			
			double[] features = getFeatures(s);
			double moveValue = 0.0;
			if (isNeural) {
				moveValue = getValueFunctionNeural(features);
			} else {
				moveValue = getValueFunction(features);
			}
			
			NewState original = convertState(s);
			
			int[][][] allLegalMoves = original.allLegalMoves();
			
			int[][] sField = s.getField();
			//int s_nextPiece = s.getNextPiece();
			int sTop[] = s.getTop();	
			int sTurn = s.getTurnNumber();

			int nextReward = 0;

			for (int j = 0; j < 7; j++){
				for (int o = 0; o < allLegalMoves[j].length; o++){

					NewState secondState = new NewState(sField, j, sTop, sTurn);
					
					secondState.newMove(o);	
					
					if (nextReward < secondState.getRowsCleared()){
						nextReward = secondState.getRowsCleared();
					}
				}
			}	
			
			double utility = moveValue;
			
			reward = reward + nextReward;
						
			// Find the move with highest utility

			if (reward > maxRow){
				maxRow = reward;
				moveIndex = i;
				maxUtility = utility;
				
			} else if (maxRow == reward) {
				if (utility > maxUtility) {
					moveIndex = i;
					moveFeatures = features;
					maxUtility = utility;
				}
			}
		}
						
		// No valid move, play a random move
		// TODO: search further down the tree
		if (moveIndex < 0) {
			moveIndex = getRandomInteger(legalMoves.length);
		}
		
		return moveIndex;
	}


	private NewState convertState(State curState) {
		NewState s = new NewState(curState.getField(), curState.getNextPiece(), curState.getTop(), curState.getTurnNumber());
		return s;
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
	private double[] getFeatures(State s) {
		double[] features = new double[NUM_FEATURES];
		// First feature is always 1
		features[0] = 1.0 / ( adjacentHeightDifferenceSquare(s) + 0.1 );	

		features[1] = 1.0 / ( averageHeight(s) + 0.1 );	

		features[2] = 1.0 / ( getMaxHeight(s) + 0.1 );	

		features[3] = ( compactness(s) + 0.1 );	

		features[4] = ( percentAreaBelowMaxHeight(s) + 0.1 );	

		return features;
	}
	
	private double adjacentHeightDifferenceSquare(State s){
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length - 1; i++) {
			sum = sum + Math.pow(colHeights[i] - colHeights[i + 1], 2);
		}
				
		return sum;
	}
	
	private double percentAreaBelowMaxHeight(State s){
		int[][] field = s.getField();
		
		int maxHeight = getMaxHeight(s);
				
		int count = 0;
		
		for (int i=0; i<field.length; i++) {
			for (int j=0; j<field[0].length; j++) {
				if (field[i][j] != 0) {
					count++;
				}
			}
		}
		
		return ( count / (maxHeight * 10.0) );
	}
	
	private double averageHeight(State s){
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length; i++) {
			sum = sum + colHeights[i];
		}
				
		return (sum / 10.0);
	}
	
	
	private double compactness(State s){
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
				
		return ( count / (sum) );
	}
	
	private int getMaxHeight(State s) {
		
		int[] colHeights = s.getTop();
		
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
		
		if (i-1 < 0 || j+1 >= State.COLS || field[i-1][j+1] == 1) {
			count++;
		}
		
		if (j-1 < 0 || field[i][j-1] == 1) {
			count++;
		}
		
		if (j+1 >= State.COLS || field[i][j+1] == 1) {
			count++;
		}
		
		if (i+1 >= State.ROWS || j+1 >= State.COLS || field[i+1][j+1] == 1) {
			count++;
		}
		
		if (i+1 >= State.ROWS || field[i+1][j] == 1) {
			count++;
		}
		
		if (i+1 >= State.ROWS || j+1 >= State.COLS || field[i+1][j+1] == 1) {
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
	private static double getValueFunction(double[] features) {
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
	
	private double getValueFunctionNeural(double[] features) {
		double value = 0.0;
		
		/*
		double hiddenNodeVal = calculateWeightedSum(features);
		double sigmoidSum = calculateSigmoid(hiddenNodeVal);
		
		value = calculateSigmoid(sigmoidSum * hiddenNodeWeight);
		*/
		value = features[0];
		value += features[1];
		value += features[2];
		value += features[3];
		value += features[4];
		
		return value;
	}

	private double calculateWeightedSum(double[] features) {
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

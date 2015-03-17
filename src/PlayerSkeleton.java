import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class PlayerSkeleton {
	private static final int TIMES_TO_TRAIN = 30;	
	private static final int NUM_FEATURES = 4;
	private static final int NUM_WEIGHTS = 5;
	private static final double LEARNING_RATE = 0.1;
	private static final String WEIGHTS_FILE = "weights.txt";
	
	private static double[] weights = new double[NUM_WEIGHTS];
	
	private static double curValue = 0.0;
	private static double outputValue = 0.0;
	private static double hiddenNodeValue = 0.3;
	
	private static double[] curFeatures = new double[NUM_FEATURES];
	private static double[] moveFeatures = new double[NUM_FEATURES];
	
	// Temp var to toggle between neural and gradient descent for now. 
	// Eventually should switch to neural when it's working
	private static boolean isNeural = true; 	
	
	public static void main(String[] args) {
		int totalRowsCleared = 0;
		initialiseWeights();
		PlayerSkeleton p = new PlayerSkeleton();
		
		for (int i = 0; i < TIMES_TO_TRAIN; i++) {
			State s = new State();
			TFrame frame = new TFrame(s);
			while(!s.hasLost()) {
				if (isNeural) {
					s.makeMove(p.pickMoveNeural(s,s.legalMoves()));
				} else {
					s.makeMove(p.pickMoveNonNeural(s,s.legalMoves()));
				}
				
				s.draw();
				s.drawNext(0,0);
				
				// pause for x seconds after a move is played
				pause(000);
			}
			
			totalRowsCleared += s.getRowsCleared();
			System.out.println("You have completed " + s.getRowsCleared() + " rows.");
			
			// pause for x seconds after a board is completed
			pause(000);
			frame.dispose();
		}
		
		System.out.println(totalRowsCleared);
		saveWeights();
	}

	/**
	 * Reads from weights file 
	 */
	private static void initialiseWeights() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(WEIGHTS_FILE));
			
			// new code
			String line = br.readLine();
			int i = 0;
			while (line != null && i < weights.length) {
				weights[i] = Double.parseDouble(line.trim());
				i++;
				line = br.readLine();
			}
			
			br.close();
			
		} catch (IOException e) {
			generateRandomWeights();
		}
		
		System.out.println("loaded weights = "
				+ Arrays.toString(weights));
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

	public int pickMoveNonNeural(State s, int[][] legalMoves) {
		int move = 0;
		
		// get the features of the current state
		StateFeatureExtractor extractor = 
				new StateFeatureExtractor(
				s, 
				NUM_FEATURES);
		curFeatures = extractor.getFeatures();
		
		System.out.println("features before move = "
				+ Arrays.toString(curFeatures));
		
		curValue = getWeightedLinearCombination(curFeatures);
		
		System.out.println("value before move = "
				+ curValue);
		
		move = getBestMove(s, legalMoves);
		
		System.out.println("chosen move = " + move);
		System.out.println("features after move = "
				+ Arrays.toString(moveFeatures));
		
		outputValue = getWeightedLinearCombination(moveFeatures);
		
		System.out.println("value after move = "
				+ outputValue);
		
		System.out.println("weights before update = "
				+ Arrays.toString(weights));
		
		updateWeightsNonNeural(curValue, outputValue);
		
		System.out.println("weights after update = "
				+ Arrays.toString(weights));
		System.out.println("\n");
		
		return move;
	}
	
	/**
	 * For using neural network with 1 hidden node and sigmoid function to pick move
	 * @param s
	 * @param legalMoves
	 * @return
	 */
	public int pickMoveNeural(State s, int[][] legalMoves) {
		int move = 0;
		
		// get the features of the current state
		StateFeatureExtractor extractor = 
				new StateFeatureExtractor(
				s, 
				NUM_FEATURES);
		curFeatures = extractor.getFeatures();
		
		/*
		System.out.println("features before move = "
				+ Arrays.toString(curFeatures));
				*/
		
		curValue = getWeightedNeuralCombination(curFeatures);
		
		/*
		System.out.println("value before move = "
				+ curValue);
		*/
		
		move = getBestMove(s, legalMoves);
		
		/*
		System.out.println("chosen move = " + move);
		System.out.println("features after move = "
				+ Arrays.toString(moveFeatures));
		*/
		
		outputValue = getWeightedNeuralCombination(moveFeatures);
		
		/*
		System.out.println("value after move = "
				+ outputValue);
		
		System.out.println("weights before update = "
				+ Arrays.toString(weights));
				*/
		
		updateWeightsNeural();
		
		/*
		System.out.println("weights after update = "
				+ Arrays.toString(weights));
		System.out.println("\n");
		*/
				
		return move; 
	}
	
	/**
	 * Choose the move from list of legalMoves with maximum utility
	 * @param curState
	 * @param legalMoves
	 * @return
	 */
	private int getBestMove(State curState, int[][] legalMoves) {
		// the best move is the move with the highest utility.
		// this algo only looks at the current state.
		
		// the best move, decided after the algo runs.
		int bestMove = -1;
		
		// the utility value that determines which is the best move
		double maxUtility = -1.0 * Double.MAX_VALUE;
		
		// iterate through all the legal moves and play each one
		for (int i = 0; i < legalMoves.length; i++) {
			
			// create a test state and play the move on it.
			TestState testState = makeTestState(curState);
			testState.makeMove(i);
			
			// extract the features of the test state after the move
			TestStateFeatureExtractor extractor = 
					new TestStateFeatureExtractor(
					testState, 
					NUM_FEATURES, 
					curState.getRowsCleared());
			double[] features = extractor.getFeatures();
			
			// get the utility value of the move
			double utility = 0.0;
			if (isNeural) {
				utility = getWeightedLinearCombination(features);
			} else {
				utility = getWeightedLinearCombination(features);
			}

			// choose this move if it's the highest utility
			if (utility > maxUtility) {
				maxUtility = utility;
				bestMove = i;
				for (int j = 0; j < features.length; j++) {
					moveFeatures[j] = features[j];
				}
			}
			
		}
		
		return bestMove;
	}
	
	private TestState makeTestState(State curState) {
		
		// prepare the data to be copied to the test states
		int[][] originalField = curState.getField();
		int originalNextPiece = curState.getNextPiece();
		int originalTop[] = curState.getTop();	
		int originalTurnNumber = curState.getTurnNumber();
		int originalRowsCleared = curState.getRowsCleared();
		
		// make the test state from the data
		TestState testState = new TestState(
				originalField, 
				originalNextPiece, 
				originalTop, 
				originalTurnNumber,
				originalRowsCleared);
		
		return testState;
	}

	/**
	 * Update weights via gradient descent
	 * Compares curFeatures to moveFeatures
	 */
	private void updateWeightsNonNeural(double curValue, double moveValue) {
		double changeInValue = moveValue - curValue;
		
		System.out.println("change in value = "
				+ changeInValue);
		
		double[] changeInWeights = calculateChangeInWeights(changeInValue);
		
		System.out.println("change in weights = "
				+ Arrays.toString(changeInWeights));
		
		updateFeatureWeights(changeInWeights);
	}

	
	/**
	 * Get the value function
	 * @param features
	 * @return Value of current board, i.e. summation of weights * features
	 */
	private double getWeightedLinearCombination(double[] features) {
		double value = 0.0;
		
		for (int i = 0; i < NUM_FEATURES; i++) {
			value += weights[i] * features[i];
		}
		
		return value;
	}
	
    //================================================================================
    // Helper methods for file I/O
    //================================================================================
	
	private static void generateRandomWeights() {
		for (int i = 0; i < weights.length; i++) {
			weights[i] = getRandomDouble();
		}		
	}
	
	private static String getWeightsString() {
		StringBuilder sb = new StringBuilder();
		String weightString = "";
		
		for (int i = 0; i < weights.length; i++) {
			sb.append(weights[i]).append("\n");
			weightString = weightString + weights[i] + "\n";
		}
		
		return sb.toString();
	}
	
    //================================================================================
    // Helper Methods for Picking Move with Neural Network
    //================================================================================
	
	private double getWeightedNeuralCombination(double[] features) {
		double value = 0.0;
		
		hiddenNodeValue = getWeightedLinearCombination(features);
		double sigmoidSum = calculateSigmoid(hiddenNodeValue);
		
		value = calculateSigmoid(sigmoidSum * getHiddenNodeWeight());
		
		return value;
	}
	
	private double calculateSigmoid(double value) {
		return 1 / (1 + Math.pow(Math.E, -value));
	}

	private void updateWeightsNeural() {
		double deltaOutput = outputValue 
				* (1 - outputValue) 
				* (outputValue - curValue);
		//System.out.println("deltaOutput = " + deltaOutput);
		updateHiddenNodeWeight(deltaOutput); 
		
		//TODO: Check formula for deltaHidden
		double deltaHidden = hiddenNodeValue 
				* (1 - hiddenNodeValue) 
				* (getHiddenNodeWeight() * deltaOutput);
				
		double[] changeInWeights = calculateChangeInWeightsNeural(deltaHidden);
		updateFeatureWeights(changeInWeights);			

	}

	private void updateHiddenNodeWeight(double deltaOutput) {
		double changeInWeight = LEARNING_RATE 
				* deltaOutput 
				* hiddenNodeValue;
		setHiddenNodeWeight(getHiddenNodeWeight() + changeInWeight);
	}
	
	private double[] calculateChangeInWeightsNeural(double deltaHidden) {
		double[] changeInWeights = new double[NUM_FEATURES];
		
		for (int i = 0; i < NUM_FEATURES; i++) {
			changeInWeights[i] = LEARNING_RATE 
					* deltaHidden 
					* moveFeatures[i];
			
		}
		
		return changeInWeights;
	}

    //================================================================================
    // Helper Methods for Update Weights
    //================================================================================
	
	private double[] calculateChangeInWeights(double changeInValue) {
		double[] changeInWeights = new double[NUM_FEATURES];
		
		for (int i = 0; i < changeInWeights.length; i++) {
			changeInWeights[i] = 
					LEARNING_RATE 
					* changeInValue 
					* moveFeatures[i];
		}
		
		return changeInWeights;
	}
	
	private void updateFeatureWeights(double[] changeInWeights) {
		for (int i = 0; i < NUM_FEATURES; i++) {
			double newWeight = weights[i] + changeInWeights[i] * i;
			if (i<3 && newWeight < 0) {
				weights[i] = newWeight;
			}
		}
	}
	
    //================================================================================
    // Other General Helper Methods 
    //================================================================================
	
	public double getHiddenNodeWeight() {
		return weights[weights.length - 1];
	}
	
	public void setHiddenNodeWeight(double w) {
		weights[weights.length - 1] = w;
	}
	
	private static double getRandomDouble() {
		Random rand = new Random();
		return rand.nextDouble();
	}
	
	private static int getRandomInteger(int max) {
		Random rand = new Random();
		return rand.nextInt(max);
	}
	
	private static void pause(int n) {
		try {
			Thread.sleep(n);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
}

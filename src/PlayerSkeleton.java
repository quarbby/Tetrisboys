import java.util.Random;

public class PlayerSkeleton {
	private static double learningRate = 0.1;
	private static double[] weights = new double[22];
	
	//TODO: implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		int move = 0;
		
		int[] features = getFeatures(s);
		
		move = getMoveWithMaxUtility(s, legalMoves);
		
		//We should return the index of the move in the legal moves list
		return move;
	}

	/**
	 * To get the features as defined in project handout
	 * @param s
	 * @return array of features 
	 */
	private int[] getFeatures(State s) {
		int[] features = new int[22];
		features[0] = 1;	// First feature is always 1
		
		return features;
	}

	private int getMoveWithMaxUtility(State curState, int[][] legalMoves) {
		State prevState = curState; 
		int maxUtility = -1;
		int reward = 0;
		int moveIndex = -1;
		
		for (int i=0; i<legalMoves.length; i++) {
			State s = prevState;
			s.makeMove(i);
			reward = s.getRowsCleared();
			
			int utility = reward;
			
			// Find the move with highest utility
			if (utility > maxUtility) {
				moveIndex = i;
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

	/**
	 * Initialise weights to small random doubles first 
	 */
	private static void initialiseWeights() {
		for (int i=0; i<weights.length; i++) {
			Random rand = new Random();
			weights[i] = rand.nextDouble();
		}
	}

	public static void main(String[] args) {
		initialiseWeights();
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
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
	
}

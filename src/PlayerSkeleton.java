import java.util.Random;

public class PlayerSkeleton {
	private double learningRate = 0.1;
	private int[] features = new int[21];
	
	//TODO: implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		int move = 0;
		
		move = getMoveWithMaxUtility(s, legalMoves);
		
		//We should return the index of the move in the legal moves list
		return move;
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

	public static void main(String[] args) {
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

import java.util.Random;

public class PlayerSkeleton {

	//TODO: implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		int move = 0;
		
		/* Randomly generate an index to use
		Random rand = new Random();
		move = rand.nextInt(legalMoves.length);
		System.out.println(move);
		*/
		
		//We should return the index of the move in the legal moves list
		return move;
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

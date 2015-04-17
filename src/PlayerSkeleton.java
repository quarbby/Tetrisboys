public class PlayerSkeleton {

	public Trainer trainer = new Trainer();	

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		int bestMove = trainer.pickMove(s, legalMoves);
		return bestMove;
	}
	
	public static void main(String[] args) {
		
		int NUM_OF_PLAYERS = 4;
		boolean SHOW_BOARD = true;
		
		for (int i = 0; i < NUM_OF_PLAYERS; i++) {
			
			// play in a new thread
			new Thread(new Player(i, SHOW_BOARD)).start();
			
		}
	}
	
}

class Player implements Runnable {
	
	int i;
	boolean q;
	
	public Player(int i, boolean q) {
		this.i = i;
		this.q = q;
	}

	@Override
	public void run() {
		CountsAndValues.getInstance();
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
				System.out.println("Thread " + i + " " + s.getRowsCleared());
			}
		}
		
		CountsAndValues.saveAndExitInstance();
		
		System.out.println("Thread " + i + " ended");
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
		
		if (q) {
			t.dispose();
		}
	}
	
}

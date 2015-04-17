interface PlayerListener {
	void setRunning(int i);
	void reportFinished();
}

public class PlayerSkeleton implements PlayerListener {
	
	volatile static int running = 2;
	

	int NUM_OF_PLAYERS = 80;
	int TIMES_TO_TRAIN = 1000;
	boolean SHOW_BOARD = false;
	
	// change this to false to only run once for "exploitation"
	final static boolean TRAINING_MODE = true;
	
	static int trained = 0;
	
	public Trainer trainer = new Trainer();	

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		int bestMove = trainer.pickMove(s, legalMoves);
		return bestMove;
	}
	
	@Override
	public synchronized void setRunning(int i) {
		running = i;
	}
	
	@Override
	public synchronized void reportFinished() {
		// threads will report finished when done.
		// this decrements the number of running threads.
		if (TRAINING_MODE) {
			running -= 1;
			if (running == 0) {
				System.out.println("save files");
				CountsAndValues.saveAndExitInstance();
				
				// when no more running threads,
				// go to next iteration
				train();
			}
		} else {
			System.out.println("save files");
			CountsAndValues.saveAndExitInstance();
		}
	}
	
	public void train() {

		System.out.println("read files");
		CountsAndValues.getInstance();
		
		// training mode on: we train for x times.
		if (TRAINING_MODE) {
			// if we have trained to the max,
			// then stop training and reset the count. 
			if (trained >= TIMES_TO_TRAIN) {
				trained = 0;
				return;
			}
			
			System.out.println("Training set " + trained);
			
			// else we can still train.
			// remember the number of running threads,
			// then run the threads.
			setRunning(NUM_OF_PLAYERS);
			for (int i = 0; i < NUM_OF_PLAYERS; i++) {
				new Thread(new Player(i, SHOW_BOARD, this)).start();
			}
			
			// increment the number of times we trained
			trained++;
		} else {
			// training mode off: just play once.
			new Thread(new Player(0, true, this)).start();
		}
	}
	
	public static void main(String[] args) {
		new PlayerSkeleton().train();
	}
	
}

class Player implements Runnable {
	
	int i;
	boolean q;
	PlayerListener p;
	
	public Player(int i, boolean q, PlayerListener p) {
		this.i = i;
		this.q = q;
		this.p = p;
	}

	@Override
	public void run() {
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
				//System.out.println("Thread " + i + " " + s.getRowsCleared());
			}
		}
		
		System.out.println("Thread " + i + " ended");
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
		
		if (q) {
			t.dispose();
		}
		
		p.reportFinished();
	}
	
}

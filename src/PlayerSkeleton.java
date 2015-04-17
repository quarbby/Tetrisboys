
public class PlayerSkeleton {

	public Trainer trainer = new Trainer();	

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		
		int bestMove = trainer.pickMove(s, legalMoves);
		
		return bestMove;
	}
	
	public static void main(String[] args) {
		State s = new State();
		TFrame t = new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			/*try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			System.out.println(s.getRowsCleared());
		}
		t.dispose();
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}

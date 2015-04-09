
public class PlayerSkeleton {

    //implement this function to have a working system
    public int pickMove(State s, int[][] legalMoves) {
        Features_Evaluator f=new Features_Evaluator();
        return f.evaluate_moves(s, legalMoves);
    }
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
                int piececount=0;
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			piececount++;
                        s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
                System.out.println("Total piece dropped: "+piececount);
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}

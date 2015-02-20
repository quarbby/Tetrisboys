import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class PlayerSkeleton {
	// make constants for the piece names
	public static final int PIECE_BOX = 0;
	public static final int PIECE_STRAIGHT = 1;
	public static final int PIECE_L = 2;
	public static final int PIECE_REVERSE_L = 3;
	public static final int PIECE_T = 4;
	public static final int PIECE_REVERSE_Z = 5;
	public static final int PIECE_Z = 6;
	
	// make constants for the orientation
	public static final int ORIENTATION_UP = 0;
	public static final int ORIENTATION_RIGHT = 1;
	public static final int ORIENTATION_DOWN = 2;
	public static final int ORIENTATION_LEFT = 3;
	
	
	// For printing the legalMoves array
	private String movesToString(int[][] array) {
		ArrayList<String> list = new ArrayList<String>(); 
		
		for (int i = 0; i < array.length; i++) {
			list.add(Arrays.toString(array[i]));
		}
		
		return list.toString();
	}
	
	private void printNextPiece(State s) {
		int next = s.getNextPiece();
		StringBuilder sb = new StringBuilder("Next piece: ");
		switch (next) {
			case 0 :
				sb.append("BOX");
				break;
				
			case 1 :
				sb.append("STRAIGHT");
				break;
				
			case 2 :
				sb.append("L");
				break;
				
			case 3 :
				sb.append("REVERSE L");
				break;
				
			case 4 :
				sb.append("T");
				break;
				
			case 5 :
				sb.append("REVERSE Z");
				break;
				
			case 6 :
				sb.append("Z");
				break;
				
			default :
				sb.append("UNKNOWN (");
				sb.append(next);
				sb.append(")");
				break;
		}
		System.out.println(sb.toString());
	}

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		// i wanna see what info we have
		printNextPiece(s);
		System.out.println(movesToString(legalMoves));
		/**
		 * so the legal moves is an array of pairs
		 * [a, b] where 'a' is the orientation and
		 * 'b' is the column that the leftmost block 
		 * of the piece can go to.
		 * it is a linear array sorted by the rotation 
		 * position.
		 */
		
		// discover piece orientations
		
		if (s.getNextPiece() == PIECE_STRAIGHT) {
			// orientation 0 is |  
			// 1 is --
			return 5;
		}
		
		if (s.getNextPiece() == PIECE_L) {
			// orientation 0 is actual L
			// 1 is ,-- shaped
			// 2 is '| shape
			// 3 is __|
			return 6;
		} // assume reverse-L is _| .__ |' --,
		
		if (s.getNextPiece() == PIECE_REVERSE_L) {
			// orientation 0 is _|
			// 1 is .__ shaped
			// 2 is |' shape
			// 3 is --,
			return 9;
		}
		
		if (s.getNextPiece() == PIECE_T) {
			// orientation 0 is |-
			// 1 is actual T
			// 2 is -|
			// 3 is _|_
			return 7;
		}
		
		if (s.getNextPiece() == PIECE_Z) {
			// orientation 0 is actual Z
			// 1 is .-'
			return 4;
		}
		
		if (s.getNextPiece() == PIECE_REVERSE_Z) {
			// orientation 0 is _;-
			// 1 is '-.
			return 5;
		}  
		
		Random r = new Random(System.currentTimeMillis());
		return r.nextInt(legalMoves.length);
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
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}

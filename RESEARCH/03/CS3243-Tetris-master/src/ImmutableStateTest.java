import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ImmutableStateTest {
	private State mutableState;
	private PlayerSkeleton.ImmutableState immutableState;

	@Before
	public void setUp() {
		mutableState = new State();
		immutableState = new PlayerSkeleton.ImmutableState(mutableState);
	}

	@Test
	public void creationTest() {
		assertStateEquals(mutableState, immutableState);
		assertStateEquals(mutableState, new PlayerSkeleton.ImmutableState());
	}

	@Test
	public void immutabilityTest() {
		immutableState.move(0, 0, 0);
		assertStateEquals(mutableState, immutableState);
		immutableState.move(1, 0, 0);
		assertStateEquals(mutableState, immutableState);
	}

	@Test
	public void consistencyTest() {
		Random random = new Random();
		for(int i = 0; i < 10000; ++i) {//Play 10000 games
			PlayerSkeleton.MoveResult lastResult = null;
			int lastScore = 0;
			while(!mutableState.hasLost()) {
				int piece = mutableState.getNextPiece();
				int[][] moves = mutableState.legalMoves();
				int move = random.nextInt(moves.length);
				int orientation = moves[move][0];
				int slot = moves[move][1];

				//Advance game state
				mutableState.makeMove(move);
				int turnScore = mutableState.getRowsCleared() - lastScore;
				lastScore = mutableState.getRowsCleared();

				//Create new immutable state to compare
				lastResult = immutableState.move(piece, orientation, slot);
				assertStateEquals(mutableState, lastResult.getState());
				assertEquals(turnScore, lastResult.getRowsCleared());

				immutableState = lastResult.getState();
			}
			assertTrue(lastResult.hasLost());

			setUp();//start a new game
		}
	}

	private static void assertStateEquals(State mutableState, PlayerSkeleton.ImmutableState immutableState) {
		assertEquals(mutableState.getTurnNumber(), immutableState.getTurn());
		assertArrayEquals(mutableState.getField(), immutableState.getField());
		assertArrayEquals(mutableState.getTop(), immutableState.getTop());
	}
}

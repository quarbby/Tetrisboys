import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Trainer {

	CountsAndValues c;

	public Trainer() {
		c = CountsAndValues.getInstance();
	}
	
	public int pull_arm(){
		//  choose an arm to pull
		int weight = 0; // by default set to size - 1
		int total_counts = 0;

		for (int m = 0; m < c.counts_size(); m++){
			total_counts = total_counts + c.counts_get(m);
			if (c.counts_get(m) == 0){
				return m;
			}
		}

		double max_reward = -1.0;

		for (int o = 0; o < c.rewards_size(); o++) {
			double bonus = Math.pow(
					(Math.log(total_counts + 1) * 1.0) / Math.log((c.counts_get(o)) + 1), 0.5
					) * (1.0 / 500);
			if (c.rewards_get(o) + bonus > max_reward) {
				max_reward = c.rewards_get(o) + bonus;
				weight = o;
			}
		}
		
		return weight;
	}
	
	
	public int pickMove(State old_state, int[][] legalMoves){
		NewState original = convertState(old_state);
		
		int[][] field = original.getField();
		int nextPiece = original.getNextPiece();
		int top[] = original.getTop();	
		int turn = original.getTurnNumber();
		
		double max_reward = -100000000000000000000.0;
		int move = 0;
		int weight = pull_arm();
		double max_row = 0.0;
		int converted_weight = Integer.parseInt(convert(weight, c.range));
		int[] array_weights = new int[c.num_of_arms];
		
		String temp = Integer.toString(converted_weight);

		int counter = 0;

		for (int i = temp.length() - 1; i >= 0; i--) {
			int index = c.num_of_arms - 1 - counter;
		    array_weights[index] = temp.charAt(i) - '0';
			counter++;
		}
	
		for (int i=0; i<legalMoves.length; i++) {
			NewState s = new NewState(field, nextPiece, top, turn);
			s.newMove(i);
			double row_cleared = s.getRowsCleared();
			double reward = 0.0;
			
			// add 1 because weight starts from 0 => we can be sure that we want to include the weight inside
			
			reward += -1.0 * Math.pow(array_weights[0] + 1.0, 2.0) * landingHeight(original, s, row_cleared);
			reward += Math.pow(array_weights[1] + 1.0, 2.0) * row_cleared;
			reward += -1.0 * Math.pow(array_weights[2] + 1.0, 2.0) * rowTransitions(s);
			reward += -1.0 * Math.pow(array_weights[3] + 1.0, 2.0) * colTransitions(s);
			reward += -1.0 * Math.pow(array_weights[4] + 1.0, 2.0) * getNumHoles(s);
			reward += -1.0 * Math.pow(array_weights[5] + 1.0, 2.0) * sum_of_wells(s);
						
			if (reward > max_reward){
				max_reward = reward;
				move = i;
				max_row = row_cleared;
			}
			
			// the bigger the reward the better
		}
		
		c.counts_set(weight, c.counts_get(weight) + 1);
		int n = c.counts_get(weight);
		double new_reward = ( (n - 1) / (n * 1.0)) * c.rewards_get(weight) + (1.0 / n) * (max_row * 1.0);
		c.rewards_set(weight, new_reward);
		
		return move;
	}
	
	private double landingHeight(NewState original, NewState after, double row_cleared){
		int[] oldHeights = original.getTop();
		int[] newHeights = after.getTop();
		
		// find the max height difference
		
				
		int col = 0;
		
		double max_diff = 0.0;
		
		for (int i = 0; i < oldHeights.length; i++){
			if (newHeights[i] + row_cleared != oldHeights[i]){
				// compare difference
				
				if (max_diff < newHeights[i] + row_cleared - oldHeights[i]){
					col = i;
					max_diff = newHeights[i] + row_cleared - oldHeights[i];
				}
				
			}
		}
		
		return oldHeights[col] + (max_diff / 2.0);
		
	}
	
	private double rowTransitions(NewState s){
		int[][] field = s.getField();
		
		
		// normalize everything to one for easy comparison
		for (int i = 0; i < field.length; i++){
			for (int j = 0; j < field[i].length; j++){
				if (field[i][j] != 0){
					field[i][j] = 1;
				}
			}
		}
		
		int sum = 0;
		
		for (int i = 0; i < 20; i++){
			
			// wrap around check from the other side
			
			int prev = field[i][9];
			
			for (int j = 0; j < field[i].length; j++){
				
				if (field[i][j] != prev){
					sum++;
				}
				
				prev = field[i][j];
			}
			
		}
		
		return (sum * 1.0);
	}

	private double colTransitions(NewState s){
		int[][] field = s.getField();
		
		// normalize everything to one for easy comparison
		for (int i = 0; i < field.length; i++){
			for (int j = 0; j < field[i].length; j++){
				if (field[i][j] != 0){
					field[i][j] = 1;
				}
			}
		}
		
		int sum = 0;
		
		// wrap around check from the other side
		
		
		for (int i = 0; i < field[0].length; i++){
			
			int prev = 1;
			
			for (int j = 0; j < 20; j++){
				
				if (field[j][i] != prev){
					sum++;
				}
				
				prev = field[j][i];
			}

		}
		
		return (sum * 1.0);
	}
	
	private double sum_of_wells(NewState s){

		int[][] field = s.getField();
		
		int[] colHeights = s.getTop();
		
		int sum = 0;
		
		for (int i = 19; i >= 0; i--){
				
			for (int j = 0; j < 10; j++){
				
				if (i < colHeights[j]){
					
					// do not count blocks below the top block
					continue;
				}
				
				int left = 1;
				int right = 1;
				
				if (j != 0){
					left = field[i][j - 1];
				}
				
				if (j != 9){
					right = field[i][j + 1];
				}
								
				
				if (left != 0 && right != 0 && field[i][j] == 0){
					// drill down for more
					
					sum++;
										
					int counter = i - 1;
					
					while (counter >= 0 && field[counter][j] == 0){
						sum++;
						counter--;
					}
				}
				
			}
			
		}
		
		return (sum * 1.0);
	}
	
	private double getNumHoles(NewState s) {
		int numHoles = 0;
		int[][] field = s.getField();
		
		int[] colHeights = s.getTop();
		
		int total = 0;
		
		for (int i = 0; i < colHeights.length; i++) {

			int count = 0;
			
			for (int j = 0; j < colHeights[i]; j++){
				// check if block at field[i][j] is occupied
				
				if (field[j][i] == 0){
					count++;
				}
				
			}
			
			numHoles = count + numHoles;

			total = total + colHeights[i];
		}
		
		return (numHoles * 1.0);
	}
	
	
	public static String convert(int number, int base) {
	    int quotient = number / base;
	    int remainder = number % base;

	    if(quotient == 0) { // base case
	        return Integer.toString(remainder);      
	    } else {
	        return convert(quotient, base) + Integer.toString(remainder);
	    }            
	}
	
	private NewState convertState(State curState) {
		NewState s = new NewState(curState.getField(), curState.getNextPiece(), curState.getTop(), curState.getTurnNumber());
		return s;
	}
	
}
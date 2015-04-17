import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CountsAndValues {
	
	private List<Double> rewards = Collections.synchronizedList(new ArrayList<Double>());
	private List<Integer> counts = Collections.synchronizedList(new ArrayList<Integer>());

	final int num_of_arms = 6;
	final int range = 3; // 0 - 2 has 3 numbers
	final int total_num_of_combos = (int) Math.pow(range, num_of_arms);
	
	private static CountsAndValues instance = null;
	
	// methods
	
	public static CountsAndValues getInstance() {
		if (instance == null) {
			instance = new CountsAndValues();
		}
		return instance;
	}
	
	private CountsAndValues() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("rewards.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				rewards.add(Double.parseDouble(line.trim()));
			}
			br.close();
		} catch (IOException e) {
			// init rewards and count		
			for (int i = 0; i < total_num_of_combos; i++){
				rewards.add(0.0);
			}
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("counts.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				counts.add(Integer.parseInt(line.trim()));
			}
			br.close();
		} catch (IOException e) {
			// init rewards and count
			for (int i = 0; i < total_num_of_combos; i++){
				counts.add(0);
			}
		}
		
		if (counts.size() != total_num_of_combos){
			for (int i = 0; i < total_num_of_combos; i++){
				counts.add(0);
			}
		}		
		
		if (rewards.size() != total_num_of_combos){
			for (int i = 0; i < total_num_of_combos; i++){
				rewards.add(0.0);
			}
		}
	}
	
	public static void saveAndExitInstance() {

		String getWriteString = getWriteString(instance.getRewards());
		try{
			File file = new File("rewards.txt");
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(getWriteString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String getWriteAnotherString = getWriteAnotherString(instance.getCounts());
		
		try{
			File file = new File("counts.txt");
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			output.write(getWriteAnotherString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int num_of_arms() {
		return num_of_arms;
	}
	
	public int range() {
		return range;
	}
	
	public int total_num_of_combos() {
		return total_num_of_combos;
	}
	
	public int rewards_size() {
		return rewards.size();
	}
	
	public double rewards_get(int i) {
		return rewards.get(i);
	}
	
	public void rewards_set(int i, double v) {
		rewards.set(i, v);
	}
	
	public int counts_size() {
		return counts.size();
	}
	
	public int counts_get(int i) {
		return counts.get(i);
	}
	
	public void counts_set(int i, int v) {
		counts.set(i, v);
	}
	
	private List<Double> getRewards() {
		return rewards;
	}
	
	private List<Integer> getCounts() {
		return counts;
	}

	private static String getWriteString(List<Double> rewards2) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < rewards2.size(); i++) {
			sb.append(rewards2.get(i) + "\n");
		}
		return sb.toString();
	}
	
	private static String getWriteAnotherString(List<Integer> counts2) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < counts2.size(); i++) {
			sb.append(counts2.get(i) + "\n");
		}
		return sb.toString();
	}
	
}

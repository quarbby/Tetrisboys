import java.util.ArrayList;
import java.util.Random;

public class genetic {
	
	private static int NUM_FEATURES = 4;
	private static int NUM_POPULATION = 16;
	
	private double[] finalChromosome = new double[NUM_FEATURES];;
	private ArrayList<double[]> population = new ArrayList<double[]>();
	private double[] features;
	
	public genetic() {
		initializePopulation();
	}
	
	public double[] getOffspring(double[] features) {
		this.features = features;
		
		selection();
		
		// Cross over 2-by-2 by index
		for (int i=0; i<NUM_POPULATION; i+=2) {
			ArrayList<double[]> crossoverList = crossover(population.get(i), population.get(i+1));
			population.set(i, crossoverList.get(0));
			population.set(i+1, crossoverList.get(1));
		}
		
		// Mutate every one 
		for (int j=0; j<NUM_POPULATION; j++) {
			double[] mutatedChromosome = mutation(population.get(j));
			population.set(j, mutatedChromosome);
		}
		
		// Set the first as final chromosome
		this.finalChromosome = population.get(0);
		
		return this.finalChromosome;
	}
	
	private void initializePopulation() {
		for (int i=0; i<NUM_POPULATION; i++) {
			double[] pop = new double[NUM_FEATURES];
			for (int j=0; i<NUM_FEATURES; j++) {
				pop[j] = getRandomDouble();
			}
			population.add(pop);
		}
		
	}
	
    //================================================================================
    // Selection
    //================================================================================
	
	/*
	 * Randomly pair the population and pick the stronger of the pair 
	 */
	//TODO: Implement selection
	private double[] selection() {
		double[] winningChromosome = new double[NUM_FEATURES];
		
		return winningChromosome;
	}

	private double[] pickStronger(double[] chromosome1, double[] chromosome2) {
		if (evaluateFitness(chromosome1) > evaluateFitness(chromosome2)) {
			return chromosome1;
		} else {
			return chromosome2;
		}
	}
	
	private double evaluateFitness(double[] chromosome) {
		double fitness = 0.0;
		for (int i=0; i<this.features.length; i++) {
			fitness += this.features[i] * chromosome[i];
		}
		return fitness;
	}
	
    //================================================================================
    // Crossover
    //================================================================================
	private ArrayList<double[]> crossover(double[] chromosome1, double[] chromosome2) {
		ArrayList<double[]> crossoverChromosome = new ArrayList<double[]>();
		int length = chromosome1.length;
		int rand = getRandomInteger(length);
		
		// Set Chromosome 1
		double[] cross1 = new double[length];
		for (int i=0; i<length; i++) {
			if (i < rand) {
				cross1[i] = chromosome2[i];
			} else {
				cross1[i] = chromosome1[i];
			}
		}
		
		// Set Chromosome 2
		double[] cross2 = new double[length];
		for (int i=0; i<length; i++) {
			if (i > rand) {
				cross2[i] = chromosome2[i];
			} else {
				cross2[i] = chromosome1[i];
			}
		}
		
		crossoverChromosome.add(cross1);
		crossoverChromosome.add(cross2);
		return crossoverChromosome;
		
	}
	
	
    //================================================================================
    // Mutation
    //================================================================================
	private double[] mutation(double[] chromosome) {
		boolean toMutate = toMutate();
		if (toMutate) {
			int indexToMutate = getRandomInteger(chromosome.length);
			chromosome[indexToMutate] = getRandomDouble();
			return chromosome;
		} else {
			return chromosome;
		}
	}

	private boolean toMutate() {
		double mutation = getRandomDouble();
		if (mutation > 0.5) {
			return true;
		} else {
			return false;
		}
	}
		
    //================================================================================
    // Other General Helper Methods 
    //================================================================================

	private static double getRandomDouble() {
		Random rand = new Random();
		return rand.nextDouble();
	}
	
	private static int getRandomInteger(int max) {
		Random rand = new Random();
		return rand.nextInt(max);
	}
	
}

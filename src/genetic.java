import java.util.ArrayList;
import java.util.Random;

public class genetic {
	
	private static int NUM_FEATURES = 4;
	private static int NUM_POPULATION = 16;
	
	private double[] finalChromosome = new double[NUM_FEATURES];;
	private ArrayList<double[]> population = new ArrayList<double[]>();
	private double[] popVals = new double[NUM_POPULATION];
	private double[] features;
	
	public genetic() {
		initializePopulation();
	}
	
	/*
	 * Features is the current array of features of current board 
	 * After returning final chromosome, use that as weights to find the maximum value
	 * of the legalMoves of the next board
	 */
	public double[] getOffspring(double[] features) {
		this.features = features;
		
		selection();
		
		// Mutate every one based on a probability
		for (int j=0; j<NUM_POPULATION; j++) {
			double[] mutatedChromosome = mutation(population.get(j));
			population.set(j, mutatedChromosome);
		}
		
		// Find the maximum val as final chromosome & randomise the min (kill off the lousiest of the lot and make new)
		this.finalChromosome = getPopVals();
						
		return this.finalChromosome;
	}

	private void initializePopulation() {
		for (int i=0; i<NUM_POPULATION; i++) {
			double[] pop = new double[NUM_FEATURES];
			for (int j=0; j<NUM_FEATURES; j++) {
				pop[j] = getRandomDouble();
			}
			population.add(pop);
		}
		
	}
	
	private double[] getPopVals() {
		double max = -99999;
		double min = 99999;
		int maxIndex = -1;
		int minIndex = -1;
		
		for (int i=0; i<popVals.length; i++) {
			popVals[i] = evaluateFitness(population.get(i));
			if (popVals[i] < min) {
				min = popVals[i];
				minIndex = i;
			} 
			if (popVals[i] > max) {
				max = popVals[i];
				maxIndex = i;
			}
		}
		
		// Randomise Min
		randomiseMin(minIndex);
		
		return population.get(maxIndex);
		
	}
	
	private void randomiseMin(int minIndex) {
		double[] newChromosome = new double[NUM_FEATURES];
		for (int i=0; i<NUM_FEATURES; i++) {
			newChromosome[i] = getRandomDouble();
		}		
		population.add(minIndex, newChromosome);
	}
	
    //================================================================================
    // Selection
    //================================================================================

	/*
	 * Randomly pair the population and pick the stronger of the pair 
	 * Stronger chromosome leave it
	 * Weaker chromosome incorporate genes from stronger one at one crossover point
	 */
	private void selection() {
		for (int i=0; i<NUM_POPULATION; i++) {
			double[] chromosome1 = population.get(i);
			double[] chromosome2 = population.get(i+1);
			
			if (chromosome1IsStronger(chromosome1, chromosome2)) {
				ArrayList<double[]> crossoverList = crossover(chromosome1, chromosome2);
				population.set(i, crossoverList.get(0));
				population.set(i+1, crossoverList.get(1));
			} else {
				ArrayList<double[]> crossoverList = crossover(chromosome2, chromosome1);
				population.set(i+1, crossoverList.get(0));
				population.set(i, crossoverList.get(1));
			}
		}
		
	}

	private boolean chromosome1IsStronger(double[] chromosome1, double[] chromosome2) {
		if (evaluateFitness(chromosome1) > evaluateFitness(chromosome2)) {
			return true;
		} else {
			return false;
		}
	}
	

	
    //================================================================================
    // Crossover
    //================================================================================
	
	/*
	 * Chromosome 1 is stronger chromosome, no change
	 * Chromosome 2 incorporate genes from Chromsome 1 after crossover point
	 */
	
	private ArrayList<double[]> crossover(double[] chromosome1, double[] chromosome2) {
		ArrayList<double[]> crossoverChromosome = new ArrayList<double[]>();
		int length = chromosome1.length;
		int rand = getRandomInteger(length);
		
		// Set Chromosome 2
		double[] cross2 = new double[length];
		for (int i=0; i<length; i++) {
			if (i < rand) {
				cross2[i] = chromosome2[i];
			} else {
				cross2[i] = chromosome1[i];
			}
		}
		
		crossoverChromosome.add(chromosome1);
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

	private double evaluateFitness(double[] chromosome) {
		double fitness = 0.0;
		for (int i=0; i<this.features.length; i++) {
			fitness += this.features[i] * chromosome[i];
		}
		return fitness;
	}
	
	private static double getRandomDouble() {
		Random rand = new Random();
		return rand.nextDouble();
	}
	
	private static int getRandomInteger(int max) {
		Random rand = new Random();
		return rand.nextInt(max);
	}
	
}

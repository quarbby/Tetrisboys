public class StateFeatureExtractor {
	
	private int NUM_FEATURES;
	private State s;

	public StateFeatureExtractor(State s, int numFeatures) {
		this.s = s;
		this.NUM_FEATURES = numFeatures;
	}
	

	
    //================================================================================
    // Get Features of Board and Helper Methods
    //================================================================================

	/**
	 * To get the features as defined in project handout
	 * @param s
	 * @return array of features 
	 */
	public double[] getFeatures() {
		double[] features = new double[NUM_FEATURES];
		
		features[0] = getFeatureAggregateHeight();
		features[1] = getFeatureNumHoles();
		features[2] = getFeatureBumpiness();
		features[3] = getFeatureFullRows(); 
		
		return features;
	}
	
	private double getFeatureFullRows() {
		// There should be no full rows for s, since it's a settled state.
		// so this feature is always 0
		return 0;
	}
	
	public double getFeatureAggregateHeight() {
		double aggHeight = 0;
		
		for (int i = 0; i < s.getTop().length; i++) {
			aggHeight += s.getTop()[i];
		}
		
		return aggHeight;
	}

	public double getFeatureBumpiness(){
		
		double bumpiness = 0.0;
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length - 1; i++) {
			// old method squares the diffs.
			//sum = sum + Math.pow(colHeights[i] - colHeights[i + 1], 2);
			
			// new method doesn't square, but takes the abs value instead
			bumpiness += Math.abs(colHeights[i] - colHeights[i + 1]);
		}
				
		return bumpiness;
	}
	public int getMaxHeight() {
		
		int[] colHeights = s.getTop();
		
		int max = 0;
		
		for (int i=0; i<colHeights.length; i++) {
			if (colHeights[i] > max) {
				max = colHeights[i]; 
			}
		}
		
		return max;
	}
	
	/**
	 * Count a hole if all eight around are walls or filled
	 * @param state
	 * @return number of holes
	 */
	public int getFeatureNumHoles() {
		int numHoles = 0;
		int[][] field = s.getField();
		
		for (int column = 0; column < field.length; column++) {
			for (int row = 0; row < field[column].length; row++) {
				if (field[column][row] == 0) {
					if (isHole(field, column, row)) {
						numHoles++;
					}
				}
			}
		}
		
		return numHoles;
	}

	public boolean isHole(int[][] field, int col, int row) {
		int minCol = 0;
		int maxCol = field.length - 1;
		int minRow = 0;
		int maxRow = field[col].length - 1;
		
		int leftCol = col - 1;
		int rightCol = col + 1;
		int bottomRow = row - 1;
		int topRow = row + 1;
		
		boolean isBottomMostRow = bottomRow < minRow;
		boolean isTopMostRow = topRow > maxRow;
		boolean isLeftMostCol = leftCol < minCol;
		boolean isRightMostCol = rightCol > maxCol;
		
		if (isBottomMostRow) {
			boolean isAboveFull = field[col][topRow] > 0;
			if (isLeftMostCol) {
				// |xxxxxxxxxx|
				// | xxxxxxxxx|
				
				boolean isRightFull = field[rightCol][row] > 0; 
				return isRightFull && isAboveFull;
				
			} else if (isRightMostCol) {
				// |xxxxxxxxxx|
				// |xxxxxxxxx |
				
				boolean isLeftFull = field[leftCol][row] > 0;
				return isLeftFull && isAboveFull;
				
			} else {
				// |xxxxxxxxxx|
				// |xxx xxxxxx|
				
				boolean isLeftFull = field[leftCol][row] > 0;
				boolean isRightFull = field[rightCol][row] > 0;
				return isLeftFull && isRightFull && isAboveFull;
			}
			
		} else if (isTopMostRow) {
			boolean isBelowFull = field[col][bottomRow] > 0;
			
			if (isLeftMostCol) {
				// | xxxxxxxxx|
				// |xxxxxxxxxx|
				
				boolean isRightFull = field[rightCol][row] > 0; 
				return isRightFull && isBelowFull;
				
			} else if (isRightMostCol) {
				// |xxxxxxxxx |
				// |xxxxxxxxxx|
				
				boolean isLeftFull = field[leftCol][row] > 0;
				return isLeftFull && isBelowFull;
				
			} else {
				// |xxx xxxxxx|
				// |xxxxxxxxxx|
				
				boolean isLeftFull = field[leftCol][row] > 0;
				boolean isRightFull = field[rightCol][row] > 0;
				return isLeftFull && isRightFull && isBelowFull;
			}
			
		} else {
			boolean isAboveFull = field[col][topRow] > 0;
			boolean isBelowFull = field[col][bottomRow] > 0;
			
			if (isLeftMostCol) {
				// |xxxxxxxxxx|
				// | xxxxxxxxx|
				// |xxxxxxxxxx|
				
				boolean isRightFull = field[rightCol][row] > 0; 
				return isRightFull && isBelowFull && isAboveFull;
				
			} else if (isRightMostCol) {
				// |xxxxxxxxxx|
				// |xxxxxxxxx |
				// |xxxxxxxxxx|
				
				boolean isLeftFull = field[leftCol][row] > 0;
				return isLeftFull && isBelowFull && isAboveFull;
				
			} else {
				// |xxxxxxxxxx|
				// |xxx xxxxxx|
				// |xxxxxxxxxx|
				
				boolean isLeftFull = field[leftCol][row] > 0;
				boolean isRightFull = field[rightCol][row] > 0;
				return isLeftFull && isRightFull && isBelowFull && isAboveFull;
			}
		}
	}
	
	
	
	// old methods

	public double percentAreaBelowMaxHeight(){
		double percent = 1;
		
		int[][] field = s.getField();
		
		int maxHeight = getMaxHeight();
		
		if (maxHeight == 0) {
			return percent;
		}
				
		int numBlocks = 0;
		
		for (int i=0; i<field.length; i++) {
			for (int j=0; j<field[0].length; j++) {
				if (field[i][j] != 0) {
					numBlocks++;
				}
			}
		}
		
		percent = numBlocks / (maxHeight * 10.0);
		
		return ( numBlocks / (maxHeight * 10.0) );
	}
	
	public double averageHeight(){
		
		double sum = 0.0;
		
		int[] colHeights = s.getTop();
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length; i++) {
			sum = sum + colHeights[i];
		}
				
		return (sum / 10.0);
	}
	
	
	public double compactness(){
		double compactness = 1; // as a percentage
		
		int[][] field = s.getField();
		int[] colHeights = s.getTop();
		
		double occupiedArea = 0.0; // sum the area occupied by the blocks
		
		// Features indexed 1 to 10 are 10 column heights of wall
		for (int i = 0; i < colHeights.length; i++) {
			occupiedArea = occupiedArea + colHeights[i];
		}
		
		// if there is no area, it's already maximum compact.
		if (occupiedArea <= 0) {
			return compactness;
		}
						
		int numBlocks = 0;
		
		for (int i=0; i<field.length; i++) {
			for (int j=0; j<field[0].length; j++) {
				if (field[i][j] != 0) {
					numBlocks++;
				}
			}
		}
		
		compactness = numBlocks / occupiedArea;
				
		return compactness;
	}
	

}

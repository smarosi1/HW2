/******************************************************************************
*  A Teaching GA					  Developed by Hal Stringer & Annie Wu, UCF
*  Version 2, January 18, 2004
*******************************************************************************/

import java.io.*;
import java.util.*;
import java.text.*;

public class Chromo
{
/*******************************************************************************
*                            INSTANCE VARIABLES                                *
*******************************************************************************/

	public University[] unis;
	public double rawFitness;
	public double sclFitness;
	public double proFitness;

/*******************************************************************************
*                            INSTANCE VARIABLES                                *
*******************************************************************************/

	private static double randnum;

/*******************************************************************************
*                              CONSTRUCTORS                                    *
*******************************************************************************/

	public Chromo(){

		University[] temp = new University[48];
		this.unis = new University[48];

		try {
            File myObj = new File("university-of-state-list.txt");
            Scanner myReader = new Scanner(myObj);

			String data = myReader.nextLine();
			data = myReader.nextLine();
			data = myReader.nextLine();
			
			int i = 0;
			while (myReader.hasNextLine()) {
				data = myReader.nextLine();
				String[] arrOfStr;
				if (i < 10) {
					arrOfStr = data.split(" ", 7);

					Integer index = Integer.parseInt(arrOfStr[2]);
					Double latitude = Double.parseDouble(arrOfStr[4]);
					Double longitude = Double.parseDouble(arrOfStr[5]);
					String text = arrOfStr[6];

					temp[i] = new University(text, latitude, longitude, index);
				} else {
					arrOfStr = data.split(" ", 6);
					
					Integer index = Integer.parseInt(arrOfStr[1]);
					Double latitude = Double.parseDouble(arrOfStr[3]);
					Double longitude = Double.parseDouble(arrOfStr[4]);
					String text = arrOfStr[5];

					temp[i] = new University(text, latitude, longitude, index);
				}
				
				
				i++;
			  }

            myReader.close();
          } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
          }

		Random rand = new Random();
		for (int i = 0; i < temp.length; i++) {
			Integer randIndex = rand.nextInt(48);
			while (temp[randIndex].index == -1) {
				randIndex = rand.nextInt(48);
			}

			this.unis[i] = new University(temp[randIndex].text, temp[randIndex].latitude, temp[randIndex].longitude, temp[randIndex].index);
			temp[randIndex].index = -1;
		}

		this.rawFitness = -1;   //  Fitness not yet evaluated
		this.sclFitness = -1;   //  Fitness not yet scaled
		this.proFitness = -1;   //  Fitness not yet proportionalized
	}


/*******************************************************************************
*                                MEMBER METHODS                                *
*******************************************************************************/

	//  Mutate a Chromosome Based on Mutation Type *****************************

	public void doMutation(){

		Random rand = new Random();

		Integer randIndex = rand.nextInt(47);
		Integer randIndex2 = rand.nextInt(48);
		//randIndex2 > randIndex
		while (randIndex2 <= randIndex) {
			randIndex2 = rand.nextInt(48);
		}

		randnum = Search.r.nextDouble();
		if (randnum < Parameters.mutationRate){
			
		


			switch (Parameters.mutationType){

			case 1:     //  Swap Mutation

				University temp = this.unis[randIndex];
				this.unis[randIndex] = this.unis[randIndex2];
				this.unis[randIndex2] = temp;
					
				
				break;
			
			case 2:		//Scramble mutation

				for (int i = randIndex; i <= randIndex2; i++) {
					Integer randIndex3 = rand.nextInt(randIndex, randIndex2+1);
					while (randIndex != randIndex2 && randIndex3 == i) {
						randIndex3 = rand.nextInt(randIndex, randIndex2+1);
					}

					temp = this.unis[i];
					this.unis[i] = this.unis[randIndex3];
					this.unis[randIndex3] = temp;

				}
				
				break;

			case 3:		//Inversion mutation

				if (randIndex != randIndex2) {
					int size = Math.abs(randIndex2-randIndex)+1;
					University[] temparray = new University[size];

					//copy section to be inverted to temparray
					for (int i = randIndex, j = 0; i <= randIndex2; i++, j++) {
						temparray[j] = new University(this.unis[i].text, this.unis[i].latitude, this.unis[i].longitude, this.unis[i].index);
					}
					

					//copy elements back in reverse order
					for (int i = randIndex, j=size-1; i <= randIndex2; i++, j--) {
						this.unis[i] = new University(temparray[j].text, temparray[j].latitude, temparray[j].longitude, temparray[j].index);
					}
				}
				
				
				break;

			default:
				System.out.println("ERROR - No mutation method selected");
			}
		}
	}

/*******************************************************************************
*                             STATIC METHODS                                   *
*******************************************************************************/

	//  Select a parent for crossover ******************************************

	public static int selectParent(){

		double rWheel = 0;
		int j = 0;
		int k = 0;

		switch (Parameters.selectType){

		case 1:     // Proportional Selection
			randnum = Search.r.nextDouble();
			for (j=0; j<Parameters.popSize; j++){
				rWheel = rWheel + Search.member[j].proFitness;
				if (randnum < rWheel) return(j);
			}
			break;

		case 3:     // Random Selection
			randnum = Search.r.nextDouble();
			j = (int) (randnum * Parameters.popSize);
			return(j);

		case 2:     //  Tournament Selection

		default:
			System.out.println("ERROR - No selection method selected");
		}
	return(-1);
	}

	//  Produce a new child from two parents  **********************************

	public static void mateParents(int pnum1, int pnum2, Chromo parent1, Chromo parent2, Chromo child1, Chromo child2){

		Random rand = new Random();

		Integer randIndex = rand.nextInt(47);
		Integer randIndex2 = rand.nextInt(48);
		//randIndex2 > randIndex
		while (randIndex2 <= randIndex) {
			randIndex2 = rand.nextInt(48);
		}

		switch (Parameters.xoverType){

		case 1:     //  Order Crossover

			//child1

			//copy section of parent1
			for (int i = randIndex; i <= randIndex2; i++) {
				child1.unis[i] = new University(parent1.unis[i].text, parent1.unis[i].latitude, parent1.unis[i].longitude, parent1.unis[i].index);
			}

			//iterate through remaining number of spaces
			//j tracks parent2, k tracks child1
			for (int i = 0, j = randIndex2 + 1, k = randIndex2 + 1; i < 48; i++, j++) {
				Integer inChild = 0;
				for (int l = randIndex; l < k; l++) {
					if (child1.unis[l%48].index == parent2.unis[j%48].index) {
						inChild = 1;
					}
				}

				if (inChild == 0) {
					child1.unis[k%48] = new University(parent2.unis[j%48].text, parent2.unis[j%48].latitude, parent2.unis[j%48].longitude, parent2.unis[j%48].index);
					k++;
				}
			}

			//child2
			for (int i = randIndex; i <= randIndex2; i++) {
				child2.unis[i] = new University(parent2.unis[i].text, parent2.unis[i].latitude, parent2.unis[i].longitude, parent2.unis[i].index);
			}

			//iterate through remaining number of spaces
			//j tracks parent1, k tracks child2
			for (int i = 0, j = randIndex2 + 1, k = randIndex2 + 1; i < 48; i++, j++) {
				Integer inChild = 0;
				for (int l = randIndex; l < k; l++) {
					if (child2.unis[l%48].index == parent1.unis[j%48].index) {
						inChild = 1;
					}
				}

				if (inChild == 0) {
					child2.unis[k%48] = new University(parent1.unis[j%48].text, parent1.unis[j%48].latitude, parent1.unis[j%48].longitude, parent1.unis[j%48].index);
					k++;
				}
			}

			break;

		case 2:     //  Partially Mapped Crossover

			Integer[] filled = new Integer[48];
			Arrays.fill(filled, -1);

			//child 1
			//copying crossover section
			for (int i = randIndex; i <= randIndex2; i++) {
				child1.unis[i] = new University(parent1.unis[i].text, parent1.unis[i].latitude, parent1.unis[i].longitude, parent1.unis[i].index);
				filled[i] = child1.unis[i].index;
			}

			//dealing with mapping
			for (int i = randIndex; i <= randIndex2; i++) {
				Integer inChild = 0;
				for (int j = 0; j < 48; j++) {
					if (filled[j] == parent2.unis[i].index) {
						inChild = 1;
					}
				}

				//get opposite (in p1)
				//until element i is placed:
					//find opposite's position (j) in p2
						//if j in child is empty
							//place p2[i] at child[j]
						//else
							//new opposite is p1[j]

				Integer opposite = parent1.unis[i].index;
				while (inChild == 0) {
					for (int j = 0; j < 48; j++) {
						//found opposite's position in p2
						if (parent2.unis[j].index == opposite) {
							//filling child[j]
							if (filled[j] == -1) {
								child1.unis[j] = new University(parent2.unis[i].text, parent2.unis[i].latitude, parent2.unis[i].longitude, parent2.unis[i].index);
								filled[j] = child1.unis[j].index;
								inChild = 1;
								break;
							} else {
								opposite = parent1.unis[j].index;
							}
						}
					}
				}
			}

			//copying nonmapped elements
			for (int i = 0; i < 48; i++) {
				if (filled[i] == -1) {
					child1.unis[i] = new University(parent2.unis[i].text, parent2.unis[i].latitude, parent2.unis[i].longitude, parent2.unis[i].index);
					filled[i] = child1.unis[i].index;
				}
			}


			//child 2

			Integer[] filled2 = new Integer[48];
			Arrays.fill(filled2, -1);

			//child 1
			//copying crossover section
			for (int i = randIndex; i <= randIndex2; i++) {
				child2.unis[i] = new University(parent2.unis[i].text, parent2.unis[i].latitude, parent2.unis[i].longitude, parent2.unis[i].index);
				filled2[i] = child2.unis[i].index;
			}

			//dealing with mapping
			for (int i = randIndex; i <= randIndex2; i++) {
				Integer inChild = 0;
				for (int j = 0; j < 48; j++) {
					if (filled2[j] == parent1.unis[i].index) {
						inChild = 1;
					}
				}

				Integer opposite = parent2.unis[i].index;
				while (inChild == 0) {
					for (int j = 0; j < 48; j++) {
						//found opposite's position in p2
						if (parent1.unis[j].index == opposite) {
							//filling child[j]
							if (filled2[j] == -1) {
								child2.unis[j] = new University(parent1.unis[i].text, parent1.unis[i].latitude, parent1.unis[i].longitude, parent1.unis[i].index);
								filled2[j] = child2.unis[j].index;
								inChild = 1;
								break;
							} else {
								opposite = parent2.unis[j].index;
							}
						}
					}
				}
			}

			//copying nonmapped elements
			for (int i = 0; i < 48; i++) {
				if (filled2[i] == -1) {
					child2.unis[i] = new University(parent1.unis[i].text, parent1.unis[i].latitude, parent1.unis[i].longitude, parent1.unis[i].index);
					filled2[i] = child2.unis[i].index;
				}
			}
			

			break;

		case 3:     //  Cycle Crossover

			Integer[] indexestouched = new Integer[48];

			Arrays.fill(indexestouched, -1);


			Integer[][] p1cycles = new Integer[48][48]; 
			Integer[][] p2cycles = new Integer[48][48];

			for (Integer[] row: p1cycles) {
				Arrays.fill(row, -1);
			}
			for (Integer[] row: p2cycles) {
				Arrays.fill(row, -1);
			}

			int numCycles = 0;

			/*
			it = [-1, -1, -1, -1]

			 p1 = [1, 2, 3, 4]
			 p2 = [2, 1, 3, 4]

			 p1cycles[0] = [1, -1, -1, -1]
			 p2cycles[0] = [2, -1, -1, -1]

			 */

			for (int i = 0; i < 48; i++) {
				if (indexestouched[i] != -1) {
					continue;
				}

				Integer indextracker = i;

				while(p1cycles[numCycles][indextracker] == -1) {
					p1cycles[numCycles][indextracker] = parent1.unis[indextracker].index;
					p2cycles[numCycles][indextracker] = parent2.unis[indextracker].index;
					indexestouched[indextracker] = 1;
					Integer value = p2cycles[numCycles][indextracker];
					for (int j = 0; j < 48; j++) {
						if (parent1.unis[j].index == value) {
							indextracker = j;
						}
					}
				}
				numCycles++;
			}

			Integer parent = 1;
			Integer[][] current = p1cycles;
			//child 1

			//go through each cycle
			for (int i = 0; i < numCycles; i++) {
				//fill child 1
				for (int j = 0; j < 48; j++) {
					if (current[i][j] != -1) {
						if (parent == 1) {
							for (int k = 0; k < 48; k++) {
								if (parent1.unis[k].index == current[i][j]) {
									child1.unis[j] = new University(parent1.unis[k].text, parent1.unis[k].latitude, parent1.unis[k].longitude, parent1.unis[k].index);
								}
							}
						} else {
							for (int k = 0; k < 48; k++) {
								if (parent2.unis[k].index == current[i][j]) {
									child1.unis[j] = new University(parent2.unis[k].text, parent2.unis[k].latitude, parent2.unis[k].longitude, parent2.unis[k].index);
								}
							}
						}
					}
				}
				if (parent == 1) {
					current = p2cycles;
					parent = 2;
				} else {
					current = p1cycles;
					parent = 1;
				}
			}
			
			//child 2
			parent = 2;
			current = p2cycles;

			//go through each cycle
			for (int i = 0; i < numCycles; i++) {
				//fill child 1
				for (int j = 0; j < 48; j++) {
					if (current[i][j] != -1) {
						if (parent == 1) {
							for (int k = 0; k < 48; k++) {
								if (parent1.unis[k].index == current[i][j]) {
									child2.unis[j] = new University(parent1.unis[k].text, parent1.unis[k].latitude, parent1.unis[k].longitude, parent1.unis[k].index);
								}
							}
						} else {
							for (int k = 0; k < 48; k++) {
								if (parent2.unis[k].index == current[i][j]) {
									child2.unis[j] = new University(parent2.unis[k].text, parent2.unis[k].latitude, parent2.unis[k].longitude, parent2.unis[k].index);
								}
							}
						}
					}
				}
				if (parent == 1) {
					current = p2cycles;
					parent = 2;
				} else {
					current = p1cycles;
					parent = 1;
				}
			}

			break;

		default:
			System.out.println("ERROR - Bad crossover method selected");
		}

		//  Set fitness values back to zero
		child1.rawFitness = -1;   //  Fitness not yet evaluated
		child1.sclFitness = -1;   //  Fitness not yet scaled
		child1.proFitness = -1;   //  Fitness not yet proportionalized
		child2.rawFitness = -1;   //  Fitness not yet evaluated
		child2.sclFitness = -1;   //  Fitness not yet scaled
		child2.proFitness = -1;   //  Fitness not yet proportionalized
	}

	//  Produce a new child from a single parent  ******************************

	public static void mateParents(int pnum, Chromo parent, Chromo child){

		//  Create child chromosome from parental material
		for (int i = 0; i < 48; i++) {
			child.unis[i] = new University(parent.unis[i].text, parent.unis[i].latitude, parent.unis[i].longitude, parent.unis[i].index);
		}

		//  Set fitness values back to zero
		child.rawFitness = -1;   //  Fitness not yet evaluated
		child.sclFitness = -1;   //  Fitness not yet scaled
		child.proFitness = -1;   //  Fitness not yet proportionalized
	}

	//  Copy one chromosome to another  ***************************************

	public static void copyB2A (Chromo targetA, Chromo sourceB){
		for (int i = 0; i < 48; i++) {
			targetA.unis[i] = new University(sourceB.unis[i].text, sourceB.unis[i].latitude, sourceB.unis[i].longitude, sourceB.unis[i].index);
		}

		targetA.rawFitness = sourceB.rawFitness;
		targetA.sclFitness = sourceB.sclFitness;
		targetA.proFitness = sourceB.proFitness;
		return;
	}

}   // End of Chromo.java ******************************************************

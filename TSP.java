import java.io.*;
import java.util.*;
import java.text.*;

public class TSP extends FitnessFunction{

/*******************************************************************************
*                            INSTANCE VARIABLES                                *
*******************************************************************************/


/*******************************************************************************
*                            STATIC VARIABLES                                  *
*******************************************************************************/


/*******************************************************************************
*                              CONSTRUCTORS                                    *
*******************************************************************************/

	public TSP(){
		name = "Traveling Salesman Problem";
	}

/*******************************************************************************
*                                MEMBER METHODS                                *
*******************************************************************************/

//stolen from stackoverflow
public double distance(double lat1, double lat2, double lon1,
double lon2) {

    final int R = 6371; // Radius of the earth

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = R * c; // distance in kilometers

    return distance;
}

//  COMPUTE A CHROMOSOME'S RAW FITNESS *************************************

	public void doRawFitness(Chromo X){

        double totaldistance = 0;
        
        for (int i = 0; i < 47; i++) {
            University a = X.unis[i];
            University b = X.unis[i+1];

            totaldistance += distance(a.latitude, b.latitude, a.longitude, b.longitude);
        }

        University penultimate = X.unis[47];
        University ultimate = X.unis[0];

        totaldistance += distance(penultimate.latitude, ultimate.latitude, penultimate.longitude, ultimate.longitude);


		X.rawFitness = totaldistance;
	}

//  PRINT OUT AN INDIVIDUAL GENE TO THE SUMMARY FILE *********************************

	public void doPrintGenes(Chromo X, FileWriter output) throws java.io.IOException{

		for (int i=0; i<Parameters.numGenes; i++){
            Hwrite.right(X.unis[i].text,35,output);
			Hwrite.right(X.unis[i].latitude,11,output);
            Hwrite.right(X.unis[i].longitude,11,output);
		}

		output.write("   <-Chromosome");
		output.write("\n        ");
		Hwrite.right(X.rawFitness,20,output);
		output.write("\n\n");
		return;
	}



/*******************************************************************************
*                             STATIC METHODS                                   *
*******************************************************************************/

}


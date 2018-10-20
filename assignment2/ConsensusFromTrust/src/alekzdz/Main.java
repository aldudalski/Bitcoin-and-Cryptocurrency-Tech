package alekzdz;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Main {

    // There are four required command line arguments: p_graph (.1, .2, .3),
    // p_malicious (.15, .30, .45), p_txDistribution (.01, .05, .10),
    // and numRounds (10, 20). You should try to test your CompliantNode
    // code for all 3x3x3x2 = 54 combinations.

    // public static boolean DEBUG = false;

    public static void main(String[] args) {

        Double[] p_graph_a = {.1, .2, .3};
        Double[] p_malicious_a = {.15, .30, .45};
        Double[] p_txDistribution_a = {.01, .05, .10};
        Integer[] numRounds_a = {10, 20};

        for (Double p_graph : p_graph_a){
            for (Double p_malicious : p_malicious_a){
                for (Double p_txDistribution : p_txDistribution_a){
                    for (Integer numRounds : numRounds_a){

                        System.out.println("\n\n\n\n==========================================================\n\n\n");
                        String [] sim = {p_graph.toString(), p_malicious.toString(), p_txDistribution.toString(), numRounds.toString()};
                        System.out.println("\n\nEXPERIMENT START\n");
                        System.out.println("\n\n\tARGUMENTS:\n");
                        System.out.printf("\t\t p_graph: %s \n\t\t p_malicious: %s \n\t\t p_txDistribution: %s \n\t\t numRounds %s \n", p_graph.toString(), p_malicious.toString(), p_txDistribution.toString(), numRounds.toString());
                        System.out.println("\n\n----------------------------------------------------------\n\n");
                        Simulation.main(sim);
                        System.out.println("\n\n-------------------FIN------------------------------------\n\n");

                    }
                }
            }
        }


    }

}

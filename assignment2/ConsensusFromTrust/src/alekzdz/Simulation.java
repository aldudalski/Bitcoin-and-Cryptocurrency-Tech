// Example of a Simulation. This test runs the nodes on a random graph.
// At the end, it will print out the Transaction ids which each node
// believes consensus has been reached upon. You can use this simulation to
// test your nodes. You will want to try creating some deviant nodes and
// mixing them in the network to fully test.
package alekzdz;

import java.util.*;

public class Simulation {

   // initialize a set of 500 valid Transactions with random ids
   int numTx = 50;//500
   int numNodes = 10;// 100

   HashMap<Integer, LinkedList<Integer>> followeesMap = new HashMap<Integer, LinkedList<Integer>>(); //maybe this
   String[] args;

   /**
    * There are four required command line arguments:
    * p_graph (.1, .2, .3) - the pairwise connectivity probability of the random graph: e.g. {.1, .2, .3}
    * p_malicious (.15, .30, .45), - the probability that a node will be set to be malicious: e.g {.15, .30, .45}
    * p_txDistribution (.01, .05, .10) - the probability that each of the initial valid transactions will be communicated
    * numRounds (10, 20). the number of rounds in the simulation e.g. {10, 20}
    * You should try to test your CompliantNode code for all 3x3x3x2 = 54 combinations.
   */


   public Simulation(String[] args){
      this.args = args;
   }


   void findFolloweesList(Integer nodeOfInterest, boolean[][] followees, LinkedList<Integer> nodeFollowees){

      for (Integer i=0; i<followees[nodeOfInterest].length; i++){
         if (followees[nodeOfInterest][i]) {
            if(!nodeFollowees.contains(i)){ //prevent infinite loops by checking if I've done this one before
               nodeFollowees.add(i);
               findFolloweesList(i,followees,nodeFollowees);
            }
         }

      }


   }

   public void buildFolloweesMap (Node[] nodes, boolean[][] followeesGrid){

      //prepare empty lists to follow each node
      for (Integer i = 0; i<nodes.length; i++) {

         LinkedList<Integer> nodeFollowers = new LinkedList<>();
         followeesMap.put(i, nodeFollowers);

      }

      for (Integer i = 0; i<nodes.length; i++) {
         findFolloweesList(i, followeesGrid, followeesMap.get(i));
         try {
            if (followeesMap.get(i).contains(i)) {
               followeesMap.get(i).remove(i); //check that when two nodes follow each other that they do not end up listing themselves as a followee
            }
         }catch (Exception e){
            System.err.println("FOLLOWEES MAP EXCEPTION: "+e);
         }

      }
   }


   public void runSimulation() {




      double p_graph = Double.parseDouble(args[0]); // parameter for random graph: prob. that an edge will exist
      double p_malicious = Double.parseDouble(args[1]); // prob. that a node will be set to be malicious
      double p_txDistribution = Double.parseDouble(args[2]); // probability of assigning an initial transaction to each node
      int numRounds = Integer.parseInt(args[3]); // number of simulation rounds your nodes will run for

      // pick which nodes are malicious and which are compliant
      Node[] nodes = new Node[numNodes];
      for (int i = 0; i < numNodes; i++) {
         if (Math.random() < p_malicious) {
            // When you are ready to try testing with malicious nodes, replace the
            // instantiation below with an instantiation of a MaliciousNode
            nodes[i] = new MalDoNothing(p_graph, p_malicious, p_txDistribution, numRounds, i);
         } else {
            nodes[i] = new CompliantNode(p_graph, p_malicious, p_txDistribution, numRounds, i);
         }

      }
      System.out.println();


      // initialize random follow graph
      boolean[][] followees = new boolean[numNodes][numNodes]; // followees[i][j] is true iff i follows j
      for (int i = 0; i < numNodes; i++) {
         System.out.println("\nFollowees [" + i + "]: ");
         for (int j = 0; j < numNodes; j++) {
            if (i == j) continue;
            if (Math.random() < p_graph) { // p_graph is .1, .2, or .3
               followees[i][j] = true;
               System.out.print("\t[" + i + "]" + ((nodes[i] instanceof CompliantNode) ? "C" : "M") + " follows [" + j + "]" + ((nodes[j] instanceof CompliantNode) ? "C" : "M"));
            }
         }
      }

      buildFolloweesMap(nodes, followees);

      System.out.println();
      for (int i = 0; i < numNodes; i++) {
//         System.out.println("node[" + i + "] followees: " + followeesMap.get(i).toString());
         System.out.print("node[" + i +((nodes[i] instanceof CompliantNode) ? "C" : "M")+ "] followees:   ");

         LinkedList<Integer> sorted = followeesMap.get(i);
         Collections.sort(sorted);
         for (Integer j : sorted){
            System.out.printf("%s%s, ",j,((nodes[j] instanceof CompliantNode) ? "C" : "M"));
         }
         System.out.printf("\b\b \n");
      }

      // notify all nodes of their followees
      for (int i = 0; i < numNodes; i++)
         nodes[i].setFollowees(followees[i]);

      HashSet<Integer> validTxIds = new HashSet<Integer>();
      Random random = new Random();
      for (int i = 0; i < numTx; i++) {
         int r = random.nextInt();
         validTxIds.add(r);
      }

      HashMap<Integer, Transaction> transactionLookup = new HashMap<Integer, Transaction>();

      // distribute the 500 Transactions throughout the nodes, to initialize
      // the starting state of Transactions each node has heard. The distribution
      // is random with probability p_txDistribution for each Transaction-Node pair.
      for (int i = 0; i < numNodes; i++) {
         HashSet<Transaction> pendingTransactions = new HashSet<Transaction>();
         for (Integer txID : validTxIds) {
            if (Math.random() < p_txDistribution) {
               // p_txDistribution is .01, .05, or .10.
               Transaction tx = new Transaction(txID);
               pendingTransactions.add(tx);
               transactionLookup.put(txID, tx);
            }
         }
         nodes[i].setPendingTransaction(pendingTransactions);
      }


      // Simulate for numRounds times
      for (int round = 0; round < numRounds; round++) { // numRounds is either 10 or 20

         // gather all the proposals into a map. The key is the index of the node receiving
         // proposals. The value is an ArrayList containing 1x2 Integer arrays. The first
         // element of each array is the id of the transaction being proposed and the second
         // element is the index # of the node proposing the transaction.
         HashMap<Integer, Set<Candidate>> allProposals = new HashMap<>();

         for (int i = 0; i < numNodes; i++) {
            Set<Transaction> proposals = nodes[i].sendToFollowers();
            for (Transaction tx : proposals) {
               if (!validTxIds.contains(tx.id))
                  continue; // ensure that each tx is actually valid

               for (int j = 0; j < numNodes; j++) {
                  if (!followees[j][i]) continue; // tx only matters if j follows i

                  if (!allProposals.containsKey(j)) {
                     Set<Candidate> candidates = new HashSet<>();
                     allProposals.put(j, candidates);
                  }

                  Candidate candidate = new Candidate(tx, i);
                  allProposals.get(j).add(candidate);
               }

            }
         }

         // Distribute the Proposals to their intended recipients as Candidates
         for (int i = 0; i < numNodes; i++) {
            if (allProposals.containsKey(i))
               nodes[i].receiveFromFollowees(allProposals.get(i));
         }
      }

      // print results
      if (CompliantNode.DEBUG) System.err.println();
      for (int i = 0; i < numNodes; i++) {
         Set<Transaction> transactions = nodes[i].sendToFollowers();
         if (CompliantNode.DEBUG) System.err.println("Transaction ids that Node " + i + " believes consensus on:");
         for (Transaction tx : transactions)
            if (CompliantNode.DEBUG) System.err.print(tx.id + "  |  ");
         if (CompliantNode.DEBUG) System.err.println();
         if (CompliantNode.DEBUG) System.err.println();
      }


//      for (Transaction txConsidered : transactionLookup.values()) {
//         System.out.println("Transaction : " + txConsidered.id);
//         for (int i = 0; i < numNodes; i++) {
//            Set<Transaction> transactions = nodes[i].sendToFollowers();
//
//            for (Transaction t : transactions) {
//               if (t.equals(txConsidered)) {
//                  System.out.printf("\t Node agrees : %s", i);
//                  LinkedList<Integer> nodeTrusts = followeesMap.get(i);
//                  System.out.printf(" {followees: ");
//                  for (Integer followee : nodeTrusts) {
//                     System.out.printf("%s%s, ", followee, ((nodes[followee] instanceof CompliantNode) ? "C" : "M"));
//                  }
//                  System.out.printf("\b\b}");
//
//               }
//            }
//         }
//         System.out.println();
//
//      }


      HashMap<Set<Transaction>, LinkedList<Integer>> transactionSets = new HashMap<>();

      for (Integer i = 0; i < numNodes; i++) {

         Set<Transaction> transactions = nodes[i].sendToFollowers();
         if (transactionSets.containsKey(transactions)){
            transactionSets.get(transactions).add(i);
         }
         else{
            LinkedList<Integer> l = new LinkedList<>();
            l.add(i);
            transactionSets.put(transactions, l);
         }
      }

      for(Set<Transaction> ts: transactionSets.keySet()){
         System.out.println("\nnodes in agreement on transactions");
         if (ts.size()>0) {
            for (Transaction t:ts){
               System.out.printf("%s, ", t.id);
            }
            System.out.println("\b\b  ");
         }
         else{
            System.out.println("NONE");
         }
         System.out.println("Nodes: "+transactionSets.get(ts));
      }
   }


   public static void main(String[] args) {

      if (args.length<0){
         /**
          * There are four required command line arguments:
          * p_graph (.1, .2, .3) - the pairwise connectivity probability of the random graph: e.g. {.1, .2, .3}
          * p_malicious (.15, .30, .45), - the probability that a node will be set to be malicious: e.g {.15, .30, .45}
          * p_txDistribution (.01, .05, .10) - the probability that each of the initial valid transactions will be communicated
          * numRounds (10, 20). the number of rounds in the simulation e.g. {10, 20}
          * You should try to test your CompliantNode code for all 3x3x3x2 = 54 combinations.
          */
         String p_graph = ".1";
         String p_malicious = "0.0";
         String p_txDistribution = ".01";
         String numRounds = "50";
         String [] newargs = {p_graph, p_malicious, p_txDistribution, numRounds};
         args = newargs;
      }
      Simulation a = new Simulation(args);
      a.runSimulation();
   }
}

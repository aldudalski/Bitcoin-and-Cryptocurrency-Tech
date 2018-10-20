package alekzdz;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    public static final boolean DEBUG=false;

    boolean[] followees;

    Set<Transaction> pendingTransactions;

    int numRounds;

    int currentRound = 0;

    public class ConsensusStatus {
        int votes = 0;
        boolean consensus = false;
        int roundFirstSeen;
        int concensusThreshold;
        LinkedList<Integer> nodesPromoting;
        int trustRating=0;
        int minTrustRating;
        Transaction proposedTransaction;

        public ConsensusStatus(Transaction proposedTransaction, int roundFirstSeen, int minTrustRating){
                this.roundFirstSeen = roundFirstSeen;
                this.concensusThreshold = concensusThreshold;
                nodesPromoting = new LinkedList<>();
                incVotes(roundFirstSeen);
                this.proposedTransaction = proposedTransaction;
                this.minTrustRating = minTrustRating;
        }

        public Transaction getProposedTransaction(){
            return proposedTransaction;
        }

        public void addNodeInAgreement (Integer nodeNumber, int round){
            // try reduce memory usage
            //nodesPromoting.add(nodeNumber);
            calcTrustRating(round);
        }

        public LinkedList<Integer> getNodesInAgreement(){
            return nodesPromoting;
        }

        void calcTrustRating(int round){
            // my logic
            // 1. Have we seen this transaction on multiple rounds?
            // 2. Are many nodes promoting this transaction?
            // 3. Have nodes consistently promoted this transaction?
            // simple product of these factors gives the trust rating
            //trustRating = (round - roundFirstSeen)*nodesPromoting.size()*votes;
            //trustRating = nodesPromoting.size()*votes;
            trustRating = votes;
            if (trustRating > minTrustRating) {
                consensus = true;
                trustRating *= 10; //arbitrary order of mag increase if we feel we're at concensus might be useful when tallying votes.
            }

            if (DEBUG) System.err.printf("Trust rating %s, concensus status %s \n", trustRating, consensus);

        }

        public void incVotes(int round){
            ++votes;
            calcTrustRating(round);
        }

        public boolean isConsensus(){
            return consensus;
        }

        public int getTrustRating(int round){
            calcTrustRating(round);
            return trustRating;
        }
    }

    public class FolloweeTrustTracker {
        private HashMap<Integer, Integer> followeeTrustRating;

        public FolloweeTrustTracker(){
            followeeTrustRating = new HashMap<>();
        }

        public Integer get(Integer followee){
            return followeeTrustRating.get(followee);
        }

        public double getMedianFolloweeTrustability(){
            Integer[] numArray = new Integer[followeeTrustRating.values().size()];
            numArray = followeeTrustRating.values().toArray(numArray);

            // this median calculation method thanks to: https://stackoverflow.com/questions/11955728/how-to-calculate-the-median-of-an-array
            Arrays.sort(numArray);
            double median;
            if (numArray.length % 2 == 0)
                median = ((double)numArray[numArray.length/2] + (double)numArray[numArray.length/2 - 1])/2;
            else
                median = (double) numArray[numArray.length/2];

            return median;

        }

        public void addTrustToFollowee (Integer followee, Integer trustRating){
            if (followeeTrustRating.containsKey(followee)){
                int currentScore = followeeTrustRating.get(followee);
                followeeTrustRating.put(followee, currentScore + trustRating);
                if (DEBUG) System.err.printf("followee %s currentScore %s trustrating %s \n", followee, currentScore, trustRating);
            }
            else{
                followeeTrustRating.put(followee, trustRating);
            }
        }


    }


    HashMap<Transaction, ConsensusStatus> transactionConsensus = new HashMap<>();


    //Set<Candidate> candidates;

    int trustRatingMinimum;
    FolloweeTrustTracker followeeTrustTracker = new FolloweeTrustTracker();

    private void CN (double p_graph, double p_malicious, double p_txDistribution, int numRounds){
        this.numRounds=numRounds;

        // Create a consensus sooner if the nodes are more trustworthy is the concept
        trustRatingMinimum = 2; //(Integer) Math.round((float)(numRounds)*(float)(p_malicious));
        if (DEBUG) System.err.printf("numRounds: %d, trustRatingMinimum: %d \n", numRounds, trustRatingMinimum);

    }

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        CN(p_graph, p_malicious,p_txDistribution, numRounds);
    }

    public Integer nodeNum=0;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds, Integer nodeNum) {
        this.nodeNum = nodeNum;
        CN(p_graph, p_malicious,p_txDistribution, numRounds);
    }

    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
        this.followees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS

        if (currentRound > numRounds-1) {
            pendingTransactions.clear();
            for (Transaction t : transactionConsensus.keySet()) {
                if(transactionConsensus.get(t).isConsensus()) {
                    pendingTransactions.add(t);
                }
            }
        }

        if (currentRound < numRounds){
            currentRound++;

            for (Transaction t : transactionConsensus.keySet()){
                //if we don't have the transaction in the pending list and we do think we've reached consensus add to the list
                if (!pendingTransactions.contains(t) && transactionConsensus.get(t).isConsensus()){
                    pendingTransactions.add(t);
                };
            }
        }
        else{
            for (Transaction t:pendingTransactions){
                try{
                    if(!transactionConsensus.get(t).isConsensus()) pendingTransactions.remove(t);
                } catch (Exception e){}//ignore
            }
        }

        return pendingTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS
        //this.candidates = candidates;

        for (Candidate c : candidates){
            ConsensusStatus cs;
            if (transactionConsensus.containsKey(c.tx)){
                cs = transactionConsensus.get(c.tx);
                cs.incVotes(currentRound);
                try {
                    if (followeeTrustTracker.get((Integer) c.sender) >= followeeTrustTracker.getMedianFolloweeTrustability()) {
                        cs.incVotes(currentRound);
                    }
                } catch (Exception e){}//swollow ok if doesn't exist.
                cs.addNodeInAgreement(c.sender, currentRound);
            }
            else{
                cs = new ConsensusStatus(c.tx, currentRound, trustRatingMinimum);
                transactionConsensus.put(c.tx, cs);
                cs.addNodeInAgreement(c.sender, currentRound);
            }

            //add to followee trust here
            followeeTrustTracker.addTrustToFollowee(c.sender, cs.getTrustRating(currentRound));
        }

        // let's try change pending transactions to be only the ones I've received
//        if (currentRound >= numRounds) {
//            pendingTransactions.clear();
//            for (Transaction t : transactionConsensus.keySet()) {
//                if(transactionConsensus.get(t).isConsensus()) {
//                    pendingTransactions.add(t);
//                }
//            }
//        }

    }
}

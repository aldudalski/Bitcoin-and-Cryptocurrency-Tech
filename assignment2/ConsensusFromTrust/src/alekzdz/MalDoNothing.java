package alekzdz;

/**
 * @author alek.zdziarski 2018-01-16
 */
public class MalDoNothing extends MaliciousNode{

    public MalDoNothing(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        super (p_graph, p_malicious, p_txDistribution, numRounds);
    }
    public Integer nodeNum=0;

    public MalDoNothing(double p_graph, double p_malicious, double p_txDistribution, int numRounds, Integer nodeNum) {
        super (p_graph, p_malicious, p_txDistribution, numRounds);
        this.nodeNum = nodeNum;
    }
}

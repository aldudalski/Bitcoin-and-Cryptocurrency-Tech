package alekzdz;

import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.*;

/**
 *
 * @author alek.zdziarski
 */
public class MaxFeeTxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */

    HashMap<Transaction, Double> transactionFees = new HashMap<>();

    UTXOPool utxoPool;

    public MaxFeeTxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);

    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        final boolean VALID = true;//assume transaction is true unless proven otherwise
        double sumInputs = 0, sumOutputs = 0;

        HashMap<Transaction.Input, PublicKey> inputPublicKeyHashMap = new HashMap<Transaction.Input, PublicKey>();

        LinkedList<UTXO> utxoRegister = new LinkedList<UTXO>();

        //REQUIREMENT (1) all outputs claimed by {@code tx} are in the current UTXO pool,
        // I assume here that the 'outputs claimed' are the tx inputs compared to the UTXO's

        HashMap<Transaction.Input, UTXO> transactionMap = new HashMap<>();
        ArrayList<Transaction.Input> unMappedInputs = new ArrayList<>();

        unMappedInputs = (ArrayList<Transaction.Input> )tx.getInputs().clone();

        for (UTXO u : utxoPool.getAllUTXO()){ //go through the bigger list then the smaller list iteratively I guess.
            for (Transaction.Input i : tx.getInputs()){

                int uindex = u.getIndex();
                byte[] uhash = u.getTxHash();

                int iindex = i.outputIndex;
                byte[] ihash = i.prevTxHash;

                boolean hashesMatch = Arrays.equals(uhash, ihash); // was important to use Arrays.equals -- .equals doesn't give the correct result for arrays.
                boolean indexesMatch = uindex == iindex;

                if (hashesMatch && indexesMatch) {

                    transactionMap.put(i,u); //map the transaction input to the utxo
                    unMappedInputs.remove(i); //remove from the unmapped list of transactions

                    // I'd also like to collect the public key for use with signature check in (2)
                    Transaction.Output t = utxoPool.getTxOutput(u); // find the transaction output
                    PublicKey key = t.address; // identify the address
                    inputPublicKeyHashMap.put(i,key); // recognize as the public key
                    // I'd also like to keep sum of inputs for chec (5)
                    sumInputs+=t.value;

                    // REQUIREMENT (3) no UTXO is claimed multiple times by {@code tx}

                    // Also add to tracking whether this UTXO used once
                    if (utxoRegister.contains(u)) return !VALID;
                    utxoRegister.add(u);

                }
            }
        }

        if (unMappedInputs.size()!=0){ //if there are any unmapped inputs in the transaction then not valid
            return !VALID;
        }


        //REQUIREMENT (2) the signatures on each input of {@code tx} are valid

        int index = 0;
        for (Transaction.Input i : tx.getInputs()){
            Boolean foundMatchingOut = false;

            byte [] signature = i.signature;
            PublicKey pubKey = inputPublicKeyHashMap.get(i);
            if (pubKey == null) return !VALID;
            byte [] message = tx.getRawDataToSign(index);//i.outputIndex);
            index++; //bit hacky would be better if this was in the for loop iterator.

            try {
                if (!Crypto.verifySignature(pubKey,message,signature)) return !VALID;
            } catch (Exception e) {
                e.printStackTrace();
                return !VALID;
            }
        }

        //REQUIREMENT (3) Note: solved above as byproduct of REQUIREMENT (1) code above

        //REQUIREMENT (4) all of {@code tx}s output values are non-negative, and

        for (Transaction.Output o : tx.getOutputs()){
            if (o.value < 0) return !VALID;
            sumOutputs += o.value;
        }

        //REQUIREMENT (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output values; and false otherwise.

        Double fee = sumInputs - sumOutputs;

        if (fee < 0) return !VALID;
//        else if (fee==0) System.out.println("Found no fee transaction");
//        else System.out.println("Found high fee transaction: " + fee);

        return VALID;
    }

    // this concatenate array code snippet as per: https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
    private <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }


    public Double findMaxPossibleFeesByWalkingTransaction(Transaction feetx, HashMap<byte[], Transaction> transactionLookup){
        Double fee;

        //find fees per transaction
        for (Transaction t: transactionLookup.values()){
            Double sumOutputs=0.0, sumInputs=0.0, localfee;
            for (Transaction.Output o:t.getOutputs()){
                sumOutputs += o.value;
            }
            for (Transaction.Input i:t.getInputs()){
                try {
                    sumInputs += ((transactionLookup.get(i.prevTxHash)).getOutput(i.outputIndex)).value;
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                UTXO n = null;
                try {
                    n = new UTXO(i.prevTxHash, i.outputIndex);
                    sumInputs += utxoPool.getTxOutput(n).value;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            localfee = sumInputs - sumOutputs;
            transactionFees.put(t, localfee);
        }

        fee = transactionFees.get(feetx);
        for (Transaction t : transactionLookup.values()){
            for (Transaction.Input i: feetx.getInputs()){
                if (t.getHash().equals(i.prevTxHash)){
                    fee += findMaxPossibleFeesByWalkingTransaction(t, transactionLookup);
                }
            }

        }
        return fee;
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS

        List<Transaction> validTxs; //this will be working set
        Transaction[] validTxsArray = {}; //this will be final return set

        do {
            validTxs = new LinkedList<>();
            // find valid transactions and delete associated UTXO's

            // for Maxfee first need to order the transactions from max to min fees
            TreeMap<Double, Transaction> feeOrderedTransactions = new TreeMap<>();

            HashMap<byte[], Transaction> transactionLookup = new HashMap<>();

            for (Transaction t: possibleTxs){
                transactionLookup.put(t.getHash(),t);
            }

            for (Transaction t : possibleTxs) {
                if (isValidTx(t)) {
                    Double fee = findMaxPossibleFeesByWalkingTransaction(t, transactionLookup);
                    feeOrderedTransactions.put(fee, t);
                }
            }

            ArrayList<Transaction> feeOrderedTxArray = new ArrayList<>();

            for (Double f : feeOrderedTransactions.descendingKeySet()) {
                feeOrderedTxArray.add(feeOrderedTransactions.get(f));
            }

            for (Transaction t : feeOrderedTxArray) {
                if (isValidTx(t)) {

                    ArrayList<Transaction.Input> inputs = t.getInputs();

                    // go through all UTXO's and remove if transaction uses them up
                    for (UTXO u : utxoPool.getAllUTXO()) {
                        for (Transaction.Input i : inputs) {
                            if (Arrays.equals(i.prevTxHash, u.getTxHash()) && i.outputIndex == u.getIndex()) {
                                utxoPool.removeUTXO(u); // remove from the UTXO pool the now spent output
                            }
                        }
                    }

                    validTxs.add(t);
                }
            }

            // add new UTXO's to pool

            for (Transaction t : validTxs) {
                ArrayList<Transaction.Output> outputs = t.getOutputs();
                int outputIndexes = outputs.size();

                for (int index = 0; index < outputIndexes; index++) {
                    UTXO u = new UTXO(t.getHash(), index);
                    utxoPool.addUTXO(u, t.getOutput(index));
                }
            }

            Transaction[] t = {};

            validTxsArray = concatenate(validTxsArray, validTxs.toArray(t));
        } while (!validTxs.isEmpty());
        return validTxsArray;
    }

}

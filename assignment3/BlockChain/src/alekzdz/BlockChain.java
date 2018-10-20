// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.
package alekzdz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BlockChain {

    public static final boolean DEBUG = true;

    public static final int CUT_OFF_AGE = 10;

    /*
    NOTES:
        remember to implement blocklevel pruning for all older levels that < maxwidth

● DONE A new genesis block won’t be mined. If you receive a block which claims to be a genesis block (parent is a null hash) in the addBlock(Block b) function, you can return false .

● DONE If there are multiple blocks at the same height, return the oldest block in getMaxHeightBlock() function.

●  Assume for simplicity that a coinbase transaction of a block is available to be spent in the next block mined on top of it. (This is contrary to the actual Bitcoin protocol when there is a “maturity” period of 100 confirmations before it can be spent).

●  Maintain only one global Transaction Pool for the block chain and keep adding transactions to it on receiving transactions and remove transactions from it if a new block is received or created. It’s okay if some transactions get dropped during a block chain reorganization, i.e., when a side branch becomes the new longest branch. Specifically, transactions present in the original main branch (and thus removed from the transaction pool) but absent in the side branch might get lost.

●  The coinbase value is kept constant at 25 bitcoins whereas in reality it halves roughly every 4 years and is currently 12.5 BTC.

●  DONE When checking for validity of a newly received block, just checking if the transactions form a valid set is enough. The set need not be a maximum possible set of transactions. Also, you needn’t do any proof-of-work checks.



     */



    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    static Integer blockCounter = 0;

    class BlockDetails {
        Block block;
        Integer blockDetailsId;
        UTXOPool utxoPoolOut;
        Transaction.Output coinbaseTxO=null;
        UTXO coinbaseUTXO=null;

        private void prepDetails(Block b){
            //Initialize UTXOPool
            // Assume for simplicity that a coinbase transaction of a block is available to be spent in the next block mined on top of it.
            // Looks like coinbase transactions should be index 0 of genesis block
            block = b;
            // Track coinbase transaction as a new UTXO
            coinbaseUTXO = new UTXO(block.getCoinbase().getHash(), 0);
            coinbaseTxO = block.getCoinbase().getOutput(0);

            blockDetailsId = blockCounter++;

        }

        BlockDetails(){}
        BlockDetails(Block b){
            prepDetails(b);
            utxoPoolOut = new UTXOPool();
            utxoPoolOut.addUTXO(coinbaseUTXO, coinbaseTxO);
        }
        BlockDetails(Block b, UTXOPool up){
            prepDetails(b);
            utxoPoolOut = new UTXOPool(up);
            utxoPoolOut.addUTXO(coinbaseUTXO, coinbaseTxO);
        }

    }




    TransactionPool transactionPool = new TransactionPool();

    ArrayList<HashMap<ByteArrayWrapper, BlockDetails>> blockChainLevels = new ArrayList<>(); //an array of levels with a map of block hash to block details
    HashMap<ByteArrayWrapper, Integer> blockLevelLookup = new HashMap<>(); //lookup of a block and which level its on -- will need pruning
    ArrayList<Integer> blockChainWidths = new ArrayList<>(); //maintains a view on the number of forks

    Integer widestLevel = 0, maxWidth = 0, maxHeight = -1; //first height will be zero at creation of getNewBlockChainLevel with genesis block

    // may want to consider adding pruning code here
    // PRUNING SHOULD - look delete all levels from the blockchain levels below
    private HashMap<ByteArrayWrapper, BlockDetails> getNewBlockChainLevel (){
        HashMap<ByteArrayWrapper, BlockDetails> newLevel = new HashMap<>();
        blockChainLevels.add(newLevel);
        maxHeight++;
        return newLevel;

    }

    //need error handling if level
    //obviously next challenge will be figuring out which blocks are at which level. fun fun fun
    private boolean addBlockDetailsToLevel (Integer level, BlockDetails blockDetails){
        HashMap<ByteArrayWrapper, BlockDetails> levelMap;

        // figure out if level is valid and if we have
        if (level>maxHeight+1) {
            return false;
        } else if (level>maxHeight){
            levelMap = getNewBlockChainLevel();
        }
        else{
            levelMap = blockChainLevels.get(level);
        }


        levelMap.put(new ByteArrayWrapper(blockDetails.block.getHash()), blockDetails);
        blockLevelLookup.put(new ByteArrayWrapper(blockDetails.block.getHash()), level);

        int width = levelMap.size();
        blockChainWidths.add(level, width);
        if (width>=maxWidth){
            widestLevel = level;
            maxWidth = width; //technically unnecessary if they are equal already
        }
        return true;
    }




    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS

        addBlockDetailsToLevel(0, new BlockDetails(genesisBlock));

    }

    private BlockDetails getMaxHeightBlockDetails(){
        HashMap<ByteArrayWrapper, BlockDetails> levelBlocks = blockChainLevels.get(maxHeight);
        BlockDetails retBD = null;
        Integer min=Integer.MAX_VALUE;
        //find the oldest block
        for (BlockDetails b: levelBlocks.values()){
            if (b.blockDetailsId < min) {
                retBD = b;
                min = b.blockDetailsId;
            }
        }
        return retBD;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // DONE - IMPLEMENT THIS
        return getMaxHeightBlockDetails().block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // DONE - IMPLEMENT THIS
        return getMaxHeightBlockDetails().utxoPoolOut;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // DONE - IMPLEMENT THIS
        return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        // notes this code figures out which level a block would connect to based on it's prior
        // should we check transaction validity or does BlockHandler do that? not for processBlock only for create block.

        Integer previousLevel;
        UTXOPool utxoPoolPrev;
        try{
            //check previous block
            ByteArrayWrapper prevBlockHash = new ByteArrayWrapper(block.getPrevBlockHash());
            previousLevel = blockLevelLookup.get(prevBlockHash);

            //check level condition

            Integer level = previousLevel + 1;
            if (level <= (maxHeight - CUT_OFF_AGE)) return false;

            //now check transaction validity

            utxoPoolPrev = new UTXOPool(blockChainLevels.get(previousLevel).get(prevBlockHash).utxoPoolOut);

            TxHandler tx = new TxHandler(utxoPoolPrev);

            Block previousBlock = blockChainLevels.get(previousLevel).get(prevBlockHash).block;

            Transaction[] t = {}; //type conversion assist on to Array and then use as Temp array.
            ArrayList<Transaction> txsL = block.getTransactions();
            Transaction[] txs = txsL.toArray(t);

            t = tx.handleTxs(txs);

            // the transactions in the received block should be a perfect match to the transactions we calculate for the block
            if (!Arrays.equals(txs, t)) return false;

            addBlockDetailsToLevel(level, new BlockDetails(block, tx.getUTXOPool()));

            return true;
        } catch (Exception e){
            return false; //fail if no predecessor entry for this block - this should handle genesis blocks too
        }
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // DONE - IMPLEMENT THIS
        transactionPool.addTransaction(tx);
    }
}
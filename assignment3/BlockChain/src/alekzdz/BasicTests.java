package alekzdz;

import java.security.*;

/**
 * @author alek.zdziarski 2018-02-11
 */
public class BasicTests {
    public static void main(String... args) throws NoSuchAlgorithmException {

        KeyPair pk_genesis = KeyPairGenerator.getInstance("RSA").generateKeyPair(); //interesting point here someone owns the genesisi block

        Block genesis = new Block(null, pk_genesis.getPublic());
        genesis.finalize();
        BlockChain bc = new BlockChain(genesis);
        System.out.printf("Genesis block hash %s \n", genesis.getHash());
        System.out.printf("bc height after genesis %s isEmpty %s \n", bc.blockChainLevels.size(), bc.blockChainLevels.isEmpty());
        System.out.println("bc width after genesis "+ bc.blockChainWidths.get(0));
        System.out.printf("get max block : %s", bc.getMaxHeightBlock().getHash());
    }
}

package scroogecoin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Unspent Transaction Output Pool Class
 * Represents the current set of UTXOs (inputs)
 */
public class UTXOPool {

    /**
     * The current collection of UTXOs, with each one mapped to its corresponding transaction output
     */
    private HashMap<UTXO, Transaction.Output> H;

    /** Creates a new empty UTXOPool */
    public UTXOPool() {
        H = new HashMap<>();
    }

    /** Creates a new UTXOPool that is a copy of {@code uPool} */
    public UTXOPool(UTXOPool utxoPool) {
        H = new HashMap<>(utxoPool.H);
    }

    /** Adds a mapping from UTXO {@code utxo} to transaction output @code{txOut} to the pool */
    public void addUTXO(UTXO utxo, Transaction.Output txOut) {
        H.put(utxo, txOut);
    }

    /** Removes the UTXO {@code utxo} from the pool */
    public void removeUTXO(UTXO utxo) {
        H.remove(utxo);
    }

    /**
     * @return the transaction output corresponding to UTXO {@code utxo}, or null if {@code utxo} is
     *         not in the pool.
     */
    public Transaction.Output getTxOutput(UTXO utxo) {
        return H.get(utxo);
    }

    /** @return true if UTXO {@code utxo} is in the pool and false otherwise */
    public boolean contains(UTXO utxo) {
        return H.containsKey(utxo);
    }

    /** Returns an {@code ArrayList} of all UTXOs in the pool */
    public ArrayList<UTXO> getAllUTXO() {
        Set<UTXO> setUTXO = H.keySet();
        return new ArrayList<>(setUTXO);
    }
}

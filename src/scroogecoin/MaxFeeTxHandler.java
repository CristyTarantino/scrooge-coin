package scroogecoin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.TreeSet;

public class MaxFeeTxHandler {

    // The current UTXO Pool
    private UTXOPool currentUtxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        currentUtxoPool = new UTXOPool(utxoPool);
    }

    /**
     * Validate transaction
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // list of UTXO used to check that no UTXO is claimed twice
        ArrayList<UTXO> uniqueUtxoList = new ArrayList<>();

        double inputValueSum = 0.0;
        double outputValueSum = 0.0;

        // for every input in the transaction
        for (int i = 0; i < tx.numInputs(); i++) {

            // (1) all 'outputs claimed' (inputs) by the Transaction are in the current UTXO pool
            Transaction.Input in = tx.getInput(i);
            // re-create the input previous condition of output
            UTXO checkUTXO = new UTXO(in.prevTxHash, in.outputIndex);
            // verify that the current wallet contains the previous transaction
            if (!currentUtxoPool.contains(checkUTXO)) return false;


            // (2) the signatures on each input of {@code tx} are valid
            Transaction.Output out = currentUtxoPool.getTxOutput(checkUTXO);
            // verify that the signature against output with pubKey, message, signature
            if (!Crypto.verifySignature(out.address, tx.getRawDataToSign(i), in.signature)) return false;


            // (3) no UTXO is claimed multiple times by {@code tx}
            // if the list of UTXO contains already the currently analysed UXTO
            if (uniqueUtxoList.contains(checkUTXO)) return false;

            // otherwise add it to the list of UTXO
            uniqueUtxoList.add(checkUTXO);

            // (5.1) Sum the value of the inputs.
            // Look in the current UTXO pool for in.prevTxHash and use the
            // value of the matching Output as the value of the input
            inputValueSum += currentUtxoPool.getTxOutput(checkUTXO).value;
        }

        // (4) all of {@code tx}s output values are non-negative
        for (Transaction.Output out : tx.getOutputs()) {
            if (out.value < 0.0) return false;

            outputValueSum += out.value;
        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of
        // its output values
        return inputValueSum >= outputValueSum;
    }

    private double calcTxFees(Transaction tx) {
        double inputValueSum = 0;
        double outputValueSum = 0;

        for (Transaction.Input in : tx.getInputs()) {
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            // skip if not valid
            if (!currentUtxoPool.contains(utxo) || !isValidTx(tx)) continue;
            Transaction.Output txOutput = currentUtxoPool.getTxOutput(utxo);
            inputValueSum += txOutput.value;
        }

        for (Transaction.Output out : tx.getOutputs()) {
            outputValueSum += out.value;
        }

        return inputValueSum - outputValueSum;
    }

    /**
     * Process transaction
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Set<Transaction> txSortedByFeeSet = new TreeSet<>((tx1, tx2) -> {
            double tx1Fees = calcTxFees(tx1);
            double tx2Fees = calcTxFees(tx2);
            return Double.compare(tx1Fees, tx2Fees);
        });

        Collections.addAll(txSortedByFeeSet, possibleTxs);

        Set<Transaction> acceptedTxSet = new HashSet<>();
        for (Transaction tx : txSortedByFeeSet) {
            if (isValidTx(tx)) {
                acceptedTxSet.add(tx);
                for (Transaction.Input in : tx.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    currentUtxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output out = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    currentUtxoPool.addUTXO(utxo, out);
                }
            }
        }

        Transaction[] validTxArray = new Transaction[acceptedTxSet.size()];
        return acceptedTxSet.toArray(validTxArray);
    }
}

/*
 * Main test code for Cousera cryptocurrency assignment1
 * Based on code by Sven Mentl and Pietro Brunetti
 *
 * Copyright:
 * - Sven Mentl
 * - Pietro Brunetti
 * - Bruce Arden
 * - Tero Keski-Valkama
 */

import scroogecoin.Transaction;
import scroogecoin.TxHandler;
import scroogecoin.UTXO;
import scroogecoin.UTXOPool;

import java.math.BigInteger;
import java.security.*;

/**
 * The Input and Output classes are arguably the most important concept to understand within this whole assignment.
 * You must understand that an Input is the receiving of BTC (BitcCoin), and an Output is a sending of BTC.
 * Every transaction has an Input and an Output, and every transaction requires an input and an output.
 * The reason a Transaction is created is to send an amount of BTC, but that output must have been received by an input of an earlier date.
 * Transactions keep track of the Output and the Input. For this you must understand a concept within Bitcoin called UTXO.
 * UTXO stands for Unspent Transaction Output. In cryptocurrency lingo that simply means the output.
 * The way that cryptocurrencies work, specifically Bitcoins,
 * whenever you send an amount of money to another address you utilize past transactions (inputs) send to your address.
 *
 * These inputs are never actually destroyed when they reach your address, this means that your total Bitcoins
 * is dependent on the sum of all independent inputs to your address.
 * If you receive three transactions of 0.5 BTC, 1.0 BTC, and 0.2 BTC, your sum would be 1.7 BTC...
 * yet your inputs would still be the 0.5, 1.0, and 0.2 BTC.
 * At no point does the independent transactions (inputs) destroy themselves and become the 1.7 BTC exclusively.
 *
 * With that in mind, UTXO pool keeps track of all your unspent BTC (UTXO). In the example above,
 * it would be the 0.5 BTC, 1.0 BTC, and 0.2 BTC, which is your unspent transaction output.
 * When you do decide to spend a quantity of BTC, your Bitcoin wallet will utilize your UTXOs as inputs for another transaction.
 *
 * Here is an example of these transactions:
 *
 * Alice output 0.5 BTC to Bob.
 * Steve output 1.0 BTC to Bob.
 * Craig output 0.2 BTC to Bob.
 *
 * Bob receives 0.5 BTC input from Alice.
 * Bob receives 1.0 BTC input from Steve.
 * Bob receives 0.2 BTC input from Craig.
 *
 * These inputs to Bob's wallet become UTXO (unspend transaction outputs) because Bob has yet to use them.
 *
 * Bob UTXOPool: [0.5, 1.0, 0.2]
 *
 * I own a UTXOPool full of BTC inputs from past transactions.
 * When I want to send some BTC over to Bob,
 * I'll create a new transaction, filling its input and output details, signing it to verify my identity,
 * and sending it over to the network for processing.
 */
public class Main {

    public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        /*
         * Generate key pairs, for Scrooge & Alice
         */
        KeyPair pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_alice   = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        /*
         * Set up the root transaction:
         *
         * Generating a root transaction tx that won't be validated, so that Scrooge owns a coin of value 10
         * Needed to get a proper Transaction.Output which I then can put in the UTXOPool, which will be passed
         * to the TXHandler.
         */
        Tx tx = new Tx();
        tx.addOutput(10, pk_scrooge.getPublic());

        // This value has no meaning, but tx.getRawDataToSign(0) will access it in prevTxHash;
        byte[] initialHash = BigInteger.valueOf(0).toByteArray();
        tx.addInput(initialHash, 0);

        // Sign the transaction
        tx.signTx(pk_scrooge.getPrivate(), 0);

        /*
         * Set up the UTXOPool
         */
        // The transaction output of the root transaction is the initial unspent output.
        UTXOPool utxoPool = new UTXOPool();
        // Create an unspent output for the root transaction
        UTXO utxo = new UTXO(tx.getHash(),0);
        // Add the unspent transaction to the pool
        utxoPool.addUTXO(utxo, tx.getOutput(0));

        /*
         * Set up a test Transaction
         */
        Tx tx2 = new Tx();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx2.addInput(tx.getHash(), 0);

        // I split the coin of value 10 into 3 coins and send all of them for simplicity to
        // the same address (Alice)
        tx2.addOutput(5, pk_alice.getPublic());
        tx2.addOutput(3, pk_alice.getPublic());
        tx2.addOutput(2, pk_alice.getPublic());
        // Note that in the real world fixed-point types would be used for the values, not doubles.
        // Doubles exhibit floating-point rounding errors. This type should be for example BigInteger
        // and denote the smallest coin fractions (Satoshi in Bitcoin).

        // There is only one (at position 0) Transaction.Input in tx2
        // and it contains the coin from Scrooge, therefore I have to sign with the private key from Scrooge
        tx2.signTx(pk_scrooge.getPrivate(), 0);

        /*
         * Start the test
         */
        // Remember that the utxoPool contains a single unspent Transaction.Output which is
        // the coin from Scrooge.
        TxHandler txHandler = new TxHandler(utxoPool);
        System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));
        System.out.println("txHandler.handleTxs(new Transaction[]{tx2}) returns: " +
                txHandler.handleTxs(new Transaction[]{tx2}).length + " transaction(s)");
    }


    public static class Tx extends Transaction {
        public void signTx(PrivateKey sk, int input) throws SignatureException {
            Signature sig = null;
            try {
                sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(sk);
                sig.update(this.getRawDataToSign(input));
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
            this.addSignature(sig.sign(),input);
            // Note that this method is incorrectly named, and should not in fact override the Java
            // object finalize garbage collection related method.
            this.finalize();
        }
    }
}
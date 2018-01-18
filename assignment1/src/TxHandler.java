import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
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
        UTXOPool uniqueUtxoPool = new UTXOPool();
        double prevTxoutTotal = 0;
        double currTxoutTotal = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input txin = tx.getInput(i);
            Transaction.Output txout = tx.getOutput(i);
            UTXO utxo = new UTXO(txin.prevTxHash, txin.outputIndex);

            if (!utxoPool.contains(utxo)) {
                return false;
            }

            if (!Crypto.verifySignature(txout.address, tx.getRawDataToSign(i),
                    txin.signature)) {
                return false;
            }

            if (txout.value < 0) {
                return false;
            }

            if (uniqueUtxoPool.contains(utxo)) {
                return false;
            }

            uniqueUtxoPool.addUTXO(utxo, txout);
            prevTxoutTotal += txout.value;
        }

        for (int j = 0; j < tx.numOutputs(); j++) {
            currTxoutTotal += tx.getOutput(j).value;
        }

        return prevTxoutTotal >= currTxoutTotal;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for (int i = 0; i < possibleTxs.length; i++) {
            Transaction tx = possibleTxs[i];
            if (isValidTx(tx)) {
                validTxs.add(tx);

                for (int j = 0; j < tx.numInputs(); j++) {
                    UTXO utxo = new UTXO(tx.getInput(j).prevTxHash, tx.getInput(j).outputIndex);
                    utxoPool.removeUTXO(utxo);
                }

                for (int k = 0; k < tx.numOutputs(); k++) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, tx.getOutput(k));
                }

            }
        }
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}

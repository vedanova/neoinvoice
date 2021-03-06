package neons;

import java.math.BigInteger;
import org.neo.smartcontract.framework.Helper;
import org.neo.smartcontract.framework.ScriptContainer;
import org.neo.smartcontract.framework.SmartContract;
import org.neo.smartcontract.framework.services.neo.Blockchain;
import org.neo.smartcontract.framework.services.neo.Runtime;
import org.neo.smartcontract.framework.services.neo.Storage;
import org.neo.smartcontract.framework.services.neo.Transaction;
import org.neo.smartcontract.framework.services.neo.TransactionOutput;
import org.neo.smartcontract.framework.services.system.ExecutionEngine;

/**
 * 
 * 
 * ███████╗    ██╗    ███╗   ███╗    ██████╗     ██╗         ██╗
 * ██╔════╝    ██║    ████╗ ████║    ██╔══██╗    ██║         ██║
 * ███████╗    ██║    ██╔████╔██║    ██████╔╝    ██║         ██║ 
 * ╚════██║    ██║    ██║╚██╔╝██║    ██╔═══╝     ██║         ██║
 * ███████║    ██║    ██║ ╚═╝ ██║    ██║         ███████╗    ██║
 * ╚══════╝    ╚═╝    ╚═╝     ╚═╝    ╚═╝         ╚══════╝    ╚═╝         
 * 
 *
 */

public class NeoNS extends SmartContract {
    
    private static final String neoGasAssetId = "602c79718b16e442de58778e148d0b1084e3b2dffd5de6b7b16cee7969282de7";
    private static final byte[] neoGasAssetHash = new byte[] {96,44,121,113,-117,22,-28,66,-34,88,119,-114,20,-115,11,16,-124,-29,-78,-33,-3,93,-26,-73,-79,108,-18,121,105,40,45,-2};
    
    private static final String neoDNSownerKey = "02d1bd778aa3ef7647ae65c6d583740c068ff84b0b4046868592a19e9ebaaf6733";
    private static final byte[] newDNSownerHash = new byte[] {2,-47,-67,119,-118,-93,-17,118,71,-82,101,-58,-43,-125,116,12,6,-113,-8,75,11,64,70,-122,-123,-110,-95,-98,-98,-70,-81,103,51};
    
    private static final long feeRegisterDomainGas = 1;
    private static final int hashLength256 = 32; //256 bits
    private static final int hashLength160 = 20; //160 bits
    
//    public static void main(String[] args) {
//        byte[] b = HexToBytes("02d1bd778aa3ef7647ae65c6d583740c068ff84b0b4046868592a19e9ebaaf6733");
//        for (byte item : b)  {
//            System.out.print("," + item); 
//        }
//    }
//
//    public static byte[] HexToBytes(String s)
//    {
//        return DatatypeConverter.parseHexBinary(s);
//    }
    
    public static Object Main(String method, Object[] args) {
        
        if ("name".equals(method)) {
            return name();
        }
        else if ("symbol".equals(method)) {
            return symbol();
        }
        else if (method == "readDomain") {
            return readDomain((String)args[0]);
        }
        else if (method == "listDomains") {
            return listDomains((byte[])args[0]);
        }
        else if (method == "registerDomain") {
            return registerDomain((String)args[0]);
        }
        else if (method == "saveMeta") {
            return saveMeta((String)args[0], (String)args[1]);
        }
        else if (method == "addSlaveAsMaster") {
            return addSlaveAsMaster((String)args[0], (byte[])args[1]);
        }
        else if (method == "addDomainAsSlave") {
            return addDomainAsSlave((String)args[0]);
        }
        else if (method == "removeSlaveAsMaster") {
            return removeSlaveAsMaster((String)args[0], (byte[])args[1]);
        }
        else if (method == "transferDomain") {
            return transferDomain((String)args[0], (byte[])args[1]);
        }
        else if (method == "createPendingTransaction") {
            return createPendingTransaction((String)args[0], (BigInteger)args[1], (byte[])args[2]);
        }
        else if (method == "removePendingTransaction") {
            return removePendingTransaction((String)args[0], (BigInteger)args[1]);
        }
        else if (method == "completePendingTransaction") {
            return completePendingTransaction((String)args[1], (BigInteger)args[2]);
        }
        
        return false;
    }
    
    public static String name() {
        return "NeoDNS";
    }
    
    public static String symbol() {
        return "DNS";
    }
    
    private static String[] readDomain(String domainName) {
        byte[] ownerHash = Storage.get(Storage.currentContext(), domainName);
        if (ownerHash == null) {
            return null;
        }
        String[] domainInfos = new String[6];
        BigInteger score = Helper.asBigInteger(Storage.get(Storage.currentContext(), createDomainScoreKey(domainName)));
        domainInfos[0] = score.longValue() + "";
        String owner = Helper.asString(ownerHash);
        domainInfos[1] = owner;
        domainInfos[2] = arrayContentToString(createDomainSlavesKey(domainName), hashLength160);
        domainInfos[3] = Helper.asString(Storage.get(Storage.currentContext(), createDomainMetaKey(domainName)));
        domainInfos[4] = arrayContentToString(createDomainPendingTransactionsKey(domainName), hashLength256);
        domainInfos[5] = arrayContentToString(createDomainCompletedTransactionsKey(domainName), hashLength256);
        
        return domainInfos;
    }
    
    private static String listDomains(byte[] publicKey) {
        String key = createHashDomainsKey(Helper.asString(publicKey));
        return arrayContentToString(key, hashLength256);
    }
    
    private static String arrayContentToString(String key, int length) {
        String result = "";
        byte[] content = Storage.get(Storage.currentContext(), key);
        for (int rangeStart = 0; rangeStart < content.length; rangeStart = rangeStart + length) {
            int rangeEnd = rangeStart + length;
            byte[] indexTransaction = Helper.range(content, rangeStart, rangeEnd);
            result = result + Helper.asString(indexTransaction) + ";";
        }
        return result;
    }
    
    private static boolean existDomain(String domainName) {
        byte[] domainExist = Storage.get(Storage.currentContext(), domainName);
        return domainExist != null;
    }
    
    private static boolean owner(byte[] publicKey, String domainName) {
        byte[] publicOwner = Storage.get(Storage.currentContext(), domainName);
        return publicKey == publicOwner;
    }
    
    private static boolean existDomainAndOwner(String domainName) {
        byte[] contractExecutor = ExecutionEngine.executingScriptHash();
        return existDomainAndOwnerWithHash(contractExecutor, domainName);
    }
    
    private static boolean existDomainAndOwnerWithHash(byte[] publicKey, String domainName) {
        if (!existDomain(domainName)) {
            return false;
        }
        return owner(publicKey, domainName);
    }
    
    private static boolean existDomainAndOwnerOrSlave(String domainName) {
        byte[] contractExecutor = ExecutionEngine.executingScriptHash();
        return existDomainAndOwnerOrSlaveWithHash(contractExecutor, domainName);
    }
    
    private static boolean existDomainAndOwnerOrSlaveWithHash(byte[] publicKey, String domainName) {
        if (!existDomain(domainName)) {
            return false;
        }
        if (!owner(publicKey, domainName)) {
            String slaveKey = Helper.asString(publicKey);
            return existSlave(slaveKey, domainName);
        }
        
        return true;
    }
    
    private static boolean existSlave(String slaveKey, String domainName) {
        String key = createDomainSlaveKey(domainName, slaveKey);
        byte[] domain = Storage.get(Storage.currentContext(), key);
        return domain != null;
    }
    
    private static byte[] existPendingTransaction(String domainName, long idPendingTransaction) {
        String key = createDomainPendingTransactionKey(domainName, idPendingTransaction);
        byte[] pendingTransaction = Storage.get(Storage.currentContext(), key);
        if (pendingTransaction == null) {
            return null;
        }
        
        return pendingTransaction;
    }
    
    private static boolean registerDomain(String domainName) {
        if (domainName.length() > 256) {
            return false;
        }
        
        if (!existDomain(domainName)) {
            Runtime.log("Domain dont exist");
            return false;
        }
        ScriptContainer ctx = ExecutionEngine.scriptContainer();
        Transaction transaction = (Transaction)ctx;
        TransactionOutput[] refs = transaction.references();
        if (refs.length < 1) {
            Runtime.log("Payment is empty");
            return false;
        }
        
        TransactionOutput firstOutput = refs[0];
        if (firstOutput.assetId() != Helper.asByteArray(neoGasAssetId)) {
            return false;
        }
        
        byte[] receiver = Helper.asByteArray(neoDNSownerKey);
        
        long totalGas = 0;
        
        for (TransactionOutput output : transaction.outputs()) {
            if (output.scriptHash() == receiver) {
                totalGas += output.value();
            }
        }
        if (totalGas < feeRegisterDomainGas) {
            Runtime.log("Payment value below fee");
            return false;
        }
        
        byte[] contractExecutor = ExecutionEngine.executingScriptHash();
        String executorKey = Helper.asString(contractExecutor);
        
        Storage.put(Storage.currentContext(), domainName, contractExecutor);
        Storage.put(Storage.currentContext(), createDomainLastTransactionKey(domainName), BigInteger.ZERO);
        Storage.put(Storage.currentContext(), createDomainScoreKey(domainName), BigInteger.ZERO);
        
        addDomainToStorage(executorKey, domainName);

        return true;
    }
    
    private static boolean saveMeta(String domainName, String meta) {
        if (!existDomainAndOwner(domainName)) {
            return false;
        }
        Storage.put(Storage.currentContext(), createDomainMetaKey(domainName), meta);
        
        return true;
    }
    
    private static boolean addSlaveAsMaster(String domainName, byte[] slaveWalletKey) {
        if (!existDomainAndOwner(domainName)) {
            return false;
        }
        String slaveKey = Helper.asString(slaveWalletKey);
        if (existSlave(slaveKey, domainName)) {
            BigInteger masterApproved = Helper.asBigInteger(Storage.get(Storage.currentContext(), createDomainMasterApprovedKey(domainName, slaveKey)));
            if (masterApproved == null || masterApproved.intValue()== 0) {
                return false;
            }
            String keyHashes = createDomainSlavesKey(domainName);
            byte[] allHashes = Storage.get(Storage.currentContext(), keyHashes);
            if (allHashes == null) {
                Storage.put(Storage.currentContext(), keyHashes, allHashes);
            }
            else {
                allHashes = Helper.concat(allHashes, slaveWalletKey);
                Storage.put(Storage.currentContext(), keyHashes, allHashes);
            }
            
            addDomainToStorage(slaveKey, domainName);
        }
        else {
            Storage.put(Storage.currentContext(), createDomainSlaveKey(domainName, slaveKey), slaveWalletKey);
            Storage.put(Storage.currentContext(), createDomainSlaveApprovedKey(domainName, slaveKey), BigInteger.ZERO);
        }
        Storage.put(Storage.currentContext(), createDomainMasterApprovedKey(domainName, slaveKey), BigInteger.ONE);

        return true;
    }
    
    private static boolean addDomainAsSlave(String domainName) {
        byte[] contractExecutor = ExecutionEngine.executingScriptHash();
        if (!existDomainAndOwnerWithHash(contractExecutor, domainName)) {
            return false;
        }
        String slaveKey = Helper.asString(contractExecutor);
        if (existSlave(slaveKey, domainName)) {
            BigInteger slaveApproved = Helper.asBigInteger(Storage.get(Storage.currentContext(), createDomainSlaveApprovedKey(domainName, slaveKey)));
            if (slaveApproved == null || slaveApproved.intValue() == 0) {
                return false;
            }
            String keyHashes = domainName+"/slaves";
            byte[] allHashes = Storage.get(Storage.currentContext(), keyHashes);
            if (allHashes == null) {
                Storage.put(Storage.currentContext(), keyHashes, allHashes);
            }
            else {
                allHashes = Helper.concat(allHashes, contractExecutor);
                Storage.put(Storage.currentContext(), keyHashes, allHashes);
            }
            
            addDomainToStorage(slaveKey, domainName);
        }
        else {
            Storage.put(Storage.currentContext(), createDomainSlaveKey(domainName, slaveKey), contractExecutor);
            Storage.put(Storage.currentContext(), createDomainMasterApprovedKey(domainName, slaveKey), BigInteger.ZERO);
        }
        Storage.put(Storage.currentContext(), createDomainSlaveApprovedKey(domainName, slaveKey), BigInteger.ONE);
        
        return true;
    }
    
    private static boolean removeSlaveAsMaster(String domainName, byte[] slaveHash) {
        if (!existDomainAndOwner(domainName)) {
            return false;
        }
        String slaveKey = Helper.asString(slaveHash);
        if (existSlave(slaveKey, domainName)) {
            Storage.delete(Storage.currentContext(), createDomainSlaveKey(domainName, slaveKey));
            Storage.delete(Storage.currentContext(), createDomainSlaveApprovedKey(domainName, slaveKey));
            Storage.delete(Storage.currentContext(), createDomainMasterApprovedKey(domainName, slaveKey));
            
            String keyHashes = createDomainSlavesKey(domainName);
            
            removeHashFromHashes(keyHashes, hashLength160, slaveHash);
        
            removeDomainFromStorage(slaveKey, domainName);

            return true;
        }
        else {
            return false;
        }
    }
    
    private static boolean transferDomain(String domainName, byte[] newMasterWalletKey) {
        byte[] contractExecutor = ExecutionEngine.executingScriptHash();
        if (!existDomainAndOwnerWithHash(contractExecutor, domainName)) {
            return false;
        }
        if (contractExecutor == newMasterWalletKey) {
            return false;
        }
        Storage.put(Storage.currentContext(), domainName, newMasterWalletKey);
        
        addDomainToStorage(Helper.asString(newMasterWalletKey), domainName);
        removeDomainFromStorage(Helper.asString(contractExecutor), domainName);
        
        return true;
    }
    
    private static BigInteger createPendingTransaction(String domainName, BigInteger expirationDate, byte[] transactionHash) {
        byte[] contractExecutor = ExecutionEngine.executingScriptHash();
        if (!existDomainAndOwnerOrSlaveWithHash(contractExecutor, domainName)) {
            return BigInteger.ZERO;
        }
        
        if (Blockchain.getHeader(Blockchain.height()).timestamp() > expirationDate.intValue()) {
            return BigInteger.ZERO;
        }
        
        Transaction transaction = Blockchain.getTransaction(transactionHash);
        TransactionOutput[] refs = transaction.references();
        if (refs.length < 1) {
            Runtime.log("Payment is empty");
            return BigInteger.ZERO;
        }
        
        TransactionOutput firstOutput = refs[0];
        if (firstOutput.assetId() != Helper.asByteArray(neoGasAssetId)) {
            return BigInteger.ZERO;
        }
        
        long totalGas = 0;
        
        for (TransactionOutput output : transaction.outputs()) {
            if (output.scriptHash() == contractExecutor) {
                totalGas += output.value();
            }
        }
        if (totalGas == 0) {
            return BigInteger.ZERO;
        }
        
        byte[] bytesLastPendingTransaction = Storage.get(Storage.currentContext(), createDomainLastTransactionKey(domainName));
        BigInteger lastPendingTransaction = Helper.asBigInteger(bytesLastPendingTransaction);
        long lLast = lastPendingTransaction.longValue();
        lLast++;
        String key = createDomainPendingTransactionKey(domainName, lLast);
        Storage.put(Storage.currentContext(), key, transactionHash);
        Storage.put(Storage.currentContext(), createExecutedKey(key), BigInteger.ZERO);
        Storage.put(Storage.currentContext(), createExpirationDateKey(key), expirationDate);
        Storage.put(Storage.currentContext(), createReceiverKey(key), contractExecutor);
        Storage.put(Storage.currentContext(), createDomainLastTransactionKey(domainName), BigInteger.valueOf(lLast));
        
        String keyAllPending = createDomainPendingTransactionsKey(domainName);
        
        addHashToHashes(keyAllPending, transactionHash);
        
        return BigInteger.valueOf(lLast);
    }
    
    private static boolean removePendingTransaction(String domainName, BigInteger idPendingTransaction) {
        if (!existDomainAndOwnerOrSlave(domainName)) {
            return false;
        }
        if (existPendingTransaction(domainName, idPendingTransaction.longValue()) == null) {
            return false;
        }
        String key = createDomainPendingTransactionKey(domainName, idPendingTransaction.longValue());
        byte[] pendingHash = Storage.get(Storage.currentContext(), key);
        
        Storage.delete(Storage.currentContext(), key);
        Storage.delete(Storage.currentContext(), createExecutedKey(key));
        Storage.delete(Storage.currentContext(), createExpirationDateKey(key));
        Storage.delete(Storage.currentContext(), createReceiverKey(key));
        
        String keyAllPending = createDomainPendingTransactionsKey(domainName);
        
        removeHashFromHashes(keyAllPending, hashLength256, pendingHash);
        
        return true;
    }
    
    private static boolean completePendingTransaction(String domainName, BigInteger idPendingTransaction) {
        byte[] executorHash = ExecutionEngine.executingScriptHash();
        byte[] pendingTransactionHash = existPendingTransaction(domainName, idPendingTransaction.longValue());
        if (pendingTransactionHash == null) {
            return false;
        }
        
        String key = createDomainPendingTransactionKey(domainName, idPendingTransaction.longValue());
        BigInteger expirationDate = Helper.asBigInteger(Storage.get(Storage.currentContext(), createExpirationDateKey(key)));
        
        if (Blockchain.getHeader(Blockchain.height()).timestamp() > expirationDate.intValue()) {
            return false;
        }
        
        byte[] receiverHash = Storage.get(Storage.currentContext(), createReceiverKey(key));
        
        if (receiverHash == executorHash) {
            return false;
        }
        
        ScriptContainer ctx = ExecutionEngine.scriptContainer();
        Transaction currentTransaction = (Transaction)ctx;
        TransactionOutput[] refs = currentTransaction.references();
        if (refs.length < 1) {
            Runtime.log("Payment is empty");
            return false;
        }
        
        TransactionOutput firstOutput = refs[0];
        if (firstOutput.assetId() != Helper.asByteArray(neoGasAssetId)) {
            return false;
        }
        
        long currentTotalGas = 0;
        
        for (TransactionOutput output : currentTransaction.outputs()) {
            if (output.scriptHash() == receiverHash) {
                currentTotalGas += output.value();
            }
        }
        
        Transaction pendingTransaction = Blockchain.getTransaction(pendingTransactionHash);
        
        long pendingGas = 0;
        
        for (TransactionOutput output : pendingTransaction.outputs()) {
            if (output.scriptHash() == receiverHash) {
                pendingGas += output.value();
            }
        }
        
        if (pendingGas != currentTotalGas) {
            return false;
        }
        
        Storage.put(Storage.currentContext(), key, BigInteger.ONE);
        
        String scoreKey = createDomainScoreKey(domainName);
        BigInteger score = Helper.asBigInteger(Storage.get(Storage.currentContext(), scoreKey));
        Storage.put(Storage.currentContext(), scoreKey, BigInteger.valueOf(score.longValue() + 1));
        
        String keyAllPending = createDomainPendingTransactionsKey(domainName);
        removeHashFromHashes(keyAllPending, hashLength256, pendingTransactionHash);
        
        String keyAllCompleted = createDomainCompletedTransactionsKey(domainName);
        addHashToHashes(keyAllCompleted, pendingTransactionHash);
        
        return true;
    }

    private static void addDomainToStorage(String hashKey, String domain) { 
        byte[] maxSize = new byte[256];
        byte[] domainHash = Helper.asByteArray(domain);
        domainHash = Helper.concat(domainHash, maxSize);
        domainHash = Helper.take(domainHash, hashLength256);

        String keyDomains = createHashDomainsKey(hashKey);
        addHashToHashes(keyDomains, domainHash);
    }
    
    private static void removeDomainFromStorage(String hashKey, String domain) {
        byte[] maxSize = new byte[256];
        byte[] domainHash = Helper.asByteArray(domain);
        domainHash = Helper.concat(domainHash, maxSize);
        domainHash = Helper.take(domainHash, hashLength256);
        
        String keyDomains = createHashDomainsKey(hashKey);
        removeHashFromHashes(keyDomains, hashLength256, domainHash);
    }

    private static void addHashToHashes(String key, byte[] toAdd) {
        byte[] allHashes = Storage.get(Storage.currentContext(), key);
        if (allHashes == null) {
            Storage.put(Storage.currentContext(), key, toAdd);
        }
        else {
            allHashes = Helper.concat(allHashes, toAdd);
            Storage.put(Storage.currentContext(), key, allHashes);
        }
    }
    
    private static void removeHashFromHashes(String key, int hashLength, byte[] toRemove) {
        byte[] allHashes = Storage.get(Storage.currentContext(), key);
        byte[] newAllHashes = new byte[] { };
        for (int rangeStart = 0; rangeStart < allHashes.length; rangeStart = rangeStart + hashLength) {
            int rangeEnd = rangeStart + hashLength;
            byte[] indexHash = Helper.range(allHashes, rangeStart, rangeEnd);
            if (indexHash != toRemove) {
                newAllHashes = Helper.concat(newAllHashes, indexHash);
            }
        }
            
        Storage.put(Storage.currentContext(), key, newAllHashes);
    }
    
    private static String createDomainSlavesKey(String domainName) {
        return domainName+"/slaves";
    }
    
    private static String createDomainSlaveKey(String domainName, String slaveKey) {
        return domainName+"/slave/"+slaveKey;
    }
    
    private static String createDomainSlaveApprovedKey(String domainName, String slaveKey) {
        return createDomainSlaveKey(domainName, slaveKey)+"/slaveApproved";
    }
    
    private static String createDomainMasterApprovedKey(String domainName, String slaveKey) {
        return createDomainSlaveKey(domainName, slaveKey)+"/masterApproved";
    }
    
    private static String createHashDomainsKey(String owner) {
        return owner+"/domains";
    }
    
    private static String createDomainScoreKey(String domainName) {
        return domainName+"/score";
    }
    
    private static String createDomainMetaKey(String domainName) {
        return domainName+"/meta";
    }
    
    private static String createDomainLastTransactionKey(String domainName) {
        return domainName+"/lastPendingTransaction";
    }
    
    private static String createDomainPendingTransactionsKey(String domainName) {
        return domainName+"/pendingTransactions";
    }
    
    private static String createDomainCompletedTransactionsKey(String domainName) {
        return domainName+"/completedTransactions";
    }
    
    private static String createDomainPendingTransactionKey(String domainName, long idPendingTransaction) {
        return domainName+"/pendingTransaction/"+idPendingTransaction;
    }
    
    private static String createExecutedKey(String key) {
        return key+"/executed";
    }
    
    private static String createExpirationDateKey(String key) {
        return key+"/expirationDate";
    }
    
    private static String createReceiverKey(String key) {
        return key+"/receiver";
    }
}

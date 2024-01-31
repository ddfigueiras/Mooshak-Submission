package aed.tables;

import java.util.*;

public class ForgettingCuckooHashTable<Key, Value> implements ISymbolTable<Key, Value> {

    public static void test1()
    {
        System.out.println("Testes simples com uma tabela pequena");
        ForgettingCuckooHashTable<String,Integer> hashTable = new ForgettingCuckooHashTable<String,Integer>();

        hashTable.put(new String("ABC"), 10);
        hashTable.put("Xpto", 189);
        hashTable.put("hello", 2746);
        hashTable.put("102", 102);
        hashTable.put("five", 5);

        System.out.println("Size " + hashTable.size());
        System.out.println("isEmpty: " + hashTable.isEmpty());
        System.out.println("Capacity: " + hashTable.getCapacity());
        System.out.println("LoadFactor: " + String.format(Locale.US,"%.02f",hashTable.getLoadFactor()));

        System.out.println("Contains key \"ABC\": " + hashTable.containsKey(new String("ABC")));
        System.out.println("Contains key \"five\": " + hashTable.containsKey("five"));
        System.out.println("Contains key \"abc\": " + hashTable.containsKey("abc"));
        System.out.println("get \"ABC\": " + hashTable.get(new String("ABC")));
        System.out.println("get \"five\": " + hashTable.get("five"));
        System.out.println("get \"102\": " + hashTable.get("102"));
        System.out.println("get \"102\": " + hashTable.get("102"));
        System.out.println("get \"103\": " + hashTable.get("103"));
    }
    public static void main(String[] args)
    {
        test1();
    }
    private static int[] primesTable0 = {
            7, 17, 37, 79, 163, 331,
            673, 1361, 2729, 5471, 10949,
            21911, 43853, 87719, 175447, 350899,
            701819, 1403641, 2807303, 5614657,
            11229331, 22458671, 44917381, 89834777, 179669557
    };
    private static int[] primesTable1 = {
            11, 19, 41, 83, 167, 337,
            677, 1367, 2731, 5477, 10957,
            21929, 43867, 87721, 175453, 350941,
            701837, 1403651, 2807323, 5614673,
            11229341, 22458677, 44917399, 89834821, 179669563
    };
    private int sizeT0;  // Size of table T0
    private int sizeT1;  // Size of table T1
    private HashMap<Key, Value>[] tableT0;  // HashMap for table T0
    private HashMap<Key, Value>[] tableT1;  // HashMap for table T1
    private Random random;  // Random number generator
    private boolean swapLogging;  // ativar/desativar o registo de trocas
    private int[] swapCounts;  // Armazena o numero de trocas das últimas 100 inserções
    private int swapCount;
    private int swapIndex;  // Índice atual para swapCounts
    @SuppressWarnings("unchecked")
    public ForgettingCuckooHashTable(int primeIndex) {
        initializeTables(primesTable0[primeIndex], primesTable1[primeIndex]);
        swapLogging = false;
        swapCounts = new int[100];
        swapIndex = 0;
    }
    @SuppressWarnings("unchecked")
    private void initializeTables(int sizeT0, int sizeT1) {
        this.sizeT0 = sizeT0;
        this.sizeT1 = sizeT1;
        tableT0 = new HashMap[sizeT0];
        tableT1 = new HashMap[sizeT1];
        random = new Random();
        for (int i = 0; i < sizeT0; i++) {
            tableT0[i] = new HashMap<>();
        }
        for (int i = 0; i < sizeT1; i++) {
            tableT1[i] = new HashMap<>();
        }
    }

    public ForgettingCuckooHashTable() {
        initializeTables(primesTable0[0], primesTable1[0]);
        swapLogging = false;
        swapCounts = new int[100];
        swapIndex = 0;
    }

    public int size() {
        return sizeT0 + sizeT1;
    }


    @Override
    public boolean isEmpty() {
        return sizeT0 == 0 && sizeT1 == 0;
    }

    public int getCapacity() {
        return sizeT0 + sizeT1;
    }

    public float getLoadFactor() {
        return (float) size() / getCapacity();
    }

    private int calculateHash(Key key, int tableSize) {
        if (tableSize == 0) {
            return 0;
        }
        return Math.abs(key.hashCode()) % tableSize;
    }

    private int h0(Key key) {
        return calculateHash(key, sizeT0);
    }

    private int h1(Key key) {
        return calculateHash(key, sizeT1);
    }

    public boolean containsKey(Key k) {
        int hash0 = h0(k);
        int hash1 = h1(k);
        return tableT0[hash0].containsKey(k) || tableT1[hash1].containsKey(k);
    }

    public Value get(Key k) {
        int hash0 = h0(k);
        int hash1 = h1(k);
        if (tableT0[hash0].containsKey(k)) {
            return tableT0[hash0].get(k);
        } else if (tableT1[hash1].containsKey(k)) {
            return tableT1[hash1].get(k);
        }
        return null;
    }
    public void put(Key k, Value v) {
        if (k == null) {
            throw new IllegalArgumentException("Chave não pode ser nula.");
        }

        int hash0 = h0(k);
        int hash1 = h1(k);

        // Verifica se a chave já existe na tabela
        if (tableT0[hash0].containsKey(k)) {
            tableT0[hash0].put(k, v);
        } else if (tableT1[hash1].containsKey(k)) {
            tableT1[hash1].put(k, v);
        } else {
            // A chave não existe, é uma inserção ou uma atualização
            if ((float) (sizeT0 + sizeT1) / getCapacity() >= 0.5) {
                // Redimensiona a tabela se o fator de carga for >= 0.5
                resizeAndRehash();
                hash0 = h0(k);
                hash1 = h1(k);
            }

            // Verifica se a inserção viola a regra de não permitir 3 chaves com o mesmo hashcode
            if (tableT0[hash0].size() == 1 && tableT1[hash1].size() == 1) {
                int hash2 = h1(tableT0[hash0].keySet().iterator().next());
                if (hash1 == hash2) {
                    throw new IllegalArgumentException("Não pode ter 3 chaves com o mesmo hashcode.");
                }
            }

            // Insere a chave na tabela
            if (!tableT0[hash0].containsKey(k)) {
                tableT0[hash0].put(k, v);
                sizeT0++;
            } else if (!tableT1[hash1].containsKey(k)) {
                tableT1[hash1].put(k, v);
                sizeT1++;
            } else {
                // Se chegou aqui, algo deu errado - talvez uma lógica adicional seja necessária
                throw new IllegalStateException("Algo deu errado durante a inserção.");
            }
        }
    }
/*
    public void put(Key k, Value v) {
        if (k == null) {
            throw new IllegalArgumentException("Chave não pode ser nula.");
        }

        int hash0 = h0(k);
        int hash1 = h1(k);

        if (containsKey(k)) {
            if (tableT0[hash0].containsKey(k)) {
                tableT0[hash0].put(k, v);
            } else if (tableT1[hash1].containsKey(k)) {
                tableT1[hash1].put(k, v);
            }
            return;
        }

        if ((float) (sizeT0 + sizeT1) / getCapacity() >= 0.7) {
            resizeAndRehash();
            hash0 = h0(k);
            hash1 = h1(k);
        }

        swapCount = 0;

        if (tableT0[hash0].size() == 1 && tableT1[hash1].size() == 1) {
            int hash2 = h1(tableT0[hash0].keySet().iterator().next());
            if (hash1 == hash2) {
                throw new IllegalArgumentException("Não pode ter 3 chaves num hashcode.");
            }
        }

        if (!tableT0[hash0].containsKey(k)) {
            tableT0[hash0].put(k, v);
            sizeT0++;
            return;
        }

        if (!tableT1[hash1].containsKey(k)) {
            tableT1[hash1].put(k, v);
            sizeT1++;
            return;
        }

        if (random.nextBoolean()) {
            Key keyToMove = tableT0[hash0].keySet().iterator().next();
            Value valueToMove = tableT0[hash0].remove(keyToMove);
            tableT0[hash0].put(k, v);
            put(keyToMove, valueToMove);
            swapCount++;
        } else {
            Key keyToMove = tableT1[hash1].keySet().iterator().next();
            Value valueToMove = tableT1[hash1].remove(keyToMove);
            tableT1[hash1].put(k, v);
            put(keyToMove, valueToMove);
            swapCount++;
        }

        if (swapLogging) {
            if (swapIndex < swapCounts.length) {
                swapCounts[swapIndex++] = swapCount;
            } else {
                System.arraycopy(swapCounts, 1, swapCounts, 0, swapCounts.length - 1);
                swapCounts[swapCounts.length - 1] = swapCount;
            }
        }
    }
*/
    @SuppressWarnings("unchecked")
    private void resizeAndRehash() {
        int newSizeT0 = Math.min(sizeT0 * 2, primesTable0[0]);
        int newSizeT1 = Math.min(sizeT1 * 2, primesTable1[0]);

        HashMap<Key, Value>[] newTableT0 = new HashMap[newSizeT0];
        HashMap<Key, Value>[] newTableT1 = new HashMap[newSizeT1];

        for (int i = 0; i < newSizeT0; i++) {
            newTableT0[i] = new HashMap<>();
        }
        for (int i = 0; i < newSizeT1; i++) {
            newTableT1[i] = new HashMap<>();
        }

        rehashAndMoveEntries(tableT0, newTableT0, newSizeT0);
        rehashAndMoveEntries(tableT1, newTableT1, newSizeT1);

        tableT0 = newTableT0;
        tableT1 = newTableT1;
        sizeT0 = 0;
        sizeT1 = 0;

        // Atualiza os tamanhos corretamente
        for (HashMap<Key, Value> entry : tableT0) {
            sizeT0 += entry.size();
        }

        for (HashMap<Key, Value> entry : tableT1) {
            sizeT1 += entry.size();
        }
    }

    private void rehashAndMoveEntries(HashMap<Key, Value>[] oldTable, HashMap<Key, Value>[] newTable, int newTableSize) {
        for (HashMap<Key, Value> entry : oldTable) {
            for (Map.Entry<Key, Value> keyValuePair : entry.entrySet()) {
                Key key = keyValuePair.getKey();
                Value value = keyValuePair.getValue();
                int newHash = calculateHash(key, newTableSize);

                newTable[newHash].put(key, value);
            }
        }
    }
    public void delete(Key k) {
        int hash0 = h0(k);
        int hash1 = h1(k);

        if (tableT0[hash0].containsKey(k)) {
            tableT0[hash0].remove(k);
            sizeT0--;
        } else if (tableT1[hash1].containsKey(k)) {
            tableT1[hash1].remove(k);
            sizeT1--;
        }

        if (getLoadFactor() < 0.125 && (sizeT0 + sizeT1) > primesTable0[0] * 2) {
            resizeAndRehash();
        }
    }

    public Iterable<Key> keys() {
        return new KeyIterator();
    }

    private class KeyIterator implements Iterator<Key>, Iterable<Key> {
        private int currentIndexT0 = 0;
        private int currentIndexT1 = 0;
        private Iterator<Key> currentIterator = null;

        KeyIterator() {
            moveToNextIterator();
        }

        private void moveToNextIterator() {
            if (currentIndexT0 < sizeT0) {
                currentIterator = tableT0[currentIndexT0].keySet().iterator();
                currentIndexT0++;
            } else if (currentIndexT1 < sizeT1) {
                currentIterator = tableT1[currentIndexT1].keySet().iterator();
                currentIndexT1++;
            } else {
                currentIterator = null;
            }
        }

        public boolean hasNext() {
            return currentIterator != null && currentIterator.hasNext();
        }

        public Key next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Key key = currentIterator.next();

            if (!currentIterator.hasNext()) {
                moveToNextIterator();
            }

            return key;
        }

        public void remove() {
            throw new UnsupportedOperationException("Iterator doesn't support removal");
        }

        @Override
        public Iterator<Key> iterator() {
            return this;
        }
    }



    public void setSwapLogging(boolean state) {
        swapLogging = state;
    }

    public float getSwapAverage() {
        if (!swapLogging || swapIndex == 0) {
            return 0.0f;
        }
        int sum = 0;
        for (int i = 0; i < swapIndex; i++) {
            sum += swapCounts[i];
        }
        return (float) sum / swapIndex;
    }

    public float getSwapVariation() {
        if (!swapLogging || swapIndex == 0) {
            return 0.0f;
        }
        float avg = getSwapAverage();
        float sum = 0;
        for (int i = 0; i < swapIndex; i++) {
            sum += Math.pow(swapCounts[i] - avg, 2);
        }
        return sum / swapIndex;
    }

    public void advanceTime(int hours)
    {
        //TODO: implement
    }


}
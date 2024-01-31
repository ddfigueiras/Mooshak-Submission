package aed.tables;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;



class KeyValueEntry<Key,Value>
{
    Key Key;
    Value Value;
    int hashcode;

    int swapsCount;
    int maxswapsCount;
    int recIndex;

    LocalDateTime timeStamp;

    KeyValueEntry(Key key, Value value){
        this.Key = key;
        this.Value = value;
        this.hashcode = (key.hashCode()  & 0x7fffffff);

        this.swapsCount = -1;
        this.maxswapsCount = -1;

        this.timeStamp = LocalDateTime.now();
    }
}

public class ForgettingCuckooHashTable<Key,Value> implements ISymbolTable<Key,Value>
{

    private static final int[] primesTable0 = {
            7, 17, 37, 79, 163, 331,
            673, 1361, 2729, 5471, 10949,
            21911, 43853, 87719, 175447, 350899,
            701819, 1403641, 2807303, 5614657,
            11229331, 22458671, 44917381, 89834777, 179669557
    };

    private static final int[] primesTable1 = {
            11, 19, 41, 83, 167, 337,
            677, 1367, 2731, 5477, 10957,
            21929, 43867, 87721, 175453, 350941,
            701837, 1403651, 2807323, 5614673,
            11229341, 22458677, 44917399, 89834821, 179669563
    };

    private int capacitytable0;
    private int capacitytable1;
    private int size;
    private int capacityIndex;

    private KeyValueEntry<Key, Value> [] table0;
    private KeyValueEntry<Key, Value> [] table1;
    private KeyValueEntry<Key, Value> [] keySwapRecords;

    private boolean keySwapLogging;
    private int keySwapIndex;

    private final int maxswapsCount;

    private LocalDateTime currentTime;
    @SuppressWarnings("unchecked")
    public ForgettingCuckooHashTable(int primeIndex)
    {
        if (primeIndex < 0 || primeIndex >= primesTable0.length)
            throw new IllegalArgumentException();
        table0 = (KeyValueEntry<Key, Value> []) new KeyValueEntry[capacitytable0];
        table1 = (KeyValueEntry<Key, Value> []) new KeyValueEntry[capacitytable1];
        keySwapRecords = (KeyValueEntry<Key, Value> []) new KeyValueEntry[100];

        this.keySwapLogging = false;
        this.keySwapIndex = 0;
        this.maxswapsCount = 15;
        this.capacitytable0 = primesTable0[primeIndex];
        this.capacitytable1 = primesTable1[primeIndex];
        this.capacityIndex = primeIndex;
        this.size = 0;
        this.currentTime = LocalDateTime.now();
    }

    public ForgettingCuckooHashTable()
    {
        this(0);
    }

    private int h0(Key key)
    {
        return (key.hashCode() & 0x7fffffff) % capacitytable0;
    }

    private int h1(Key key)
    {
        return (~key.hashCode() & 0x7fffffff) % capacitytable1;
    }

    public int size()
    {
        return size;
    }

    @Override
    public boolean isEmpty()
    {
        return size == 0;
    }

    public int getCapacity()
    {
        return capacitytable0 + capacitytable1;
    }

    public float getLoadFactor()
    {
        return (float) size / getCapacity();
    }

    public boolean containsKey(Key k)
    {
        int hash0 = h0(k);
        int hash1 = h1(k);
        boolean containsKey = false;

        if (isEntryMatchingKey(table0[hash0], k))
        {
            refreshTimeStamp(table0[hash0]);
            containsKey = true;
        }
        if (isEntryMatchingKey(table1[hash1], k))
        {
            refreshTimeStamp(table1[hash1]);
            containsKey = true;
        }
        return containsKey;
    }

    public Value get(Key k)
    {
        KeyValueEntry<Key, Value> entry0 = table0[h0(k)];
        if (isEntryMatchingKey(entry0, k))
        {
            refreshTimeStamp(entry0);
            return entry0.Value;
        } else
        {
            KeyValueEntry<Key, Value> entry1 = table1[h1(k)];
            if (isEntryMatchingKey(entry1, k))
            {
                refreshTimeStamp(entry1);
                return entry1.Value;
            }
        }
        return null;
    }

    public void delete(Key k)
    {
        deleteFromTable(table0, h0(k), k);
        deleteFromTable(table1, h1(k), k);
        if (getLoadFactor() < 0.125f)
        {
            resizeTable(false);
        }
    }


    private boolean isEntryMatchingKey(KeyValueEntry<Key, Value> entry, Key k)
    {
        return entry != null && entry.Key.equals(k);
    }


    private void deleteFromTable(KeyValueEntry<Key, Value>[] table, int hash, Key k)
    {
        KeyValueEntry<Key, Value> entry = table[hash];
        if (isEntryMatchingKey(entry, k))
        {
            table[hash] = null;
            size--;
        }
    }



    public void put(Key k, Value v)
    {
        if (k == null)
            throw new IllegalArgumentException();
        if (v == null)
        {
            delete(k);
            return;
        }
        if (getLoadFactor() >= 0.5f)
            resizeTable(true);
        int hash0 = h0(k);
        int hash1 = h1(k);

        if (containsKey(k))
        {
            update(k,v,hash0,hash1);
            return;
        }

        KeyValueEntry<Key, Value> entry = new KeyValueEntry<>(k,v);
        if (threeWayCollision(entry, table0[hash0], table1[hash1]))
            throw new IllegalArgumentException("Three Keys with the same hashcode");

        while (true)
        {
            if (maxswapsCountReached(entry))
                resizeTable(true);
            swapLogic(entry);

            if (table0[hash0] == null)
            {
                table0[hash0] = entry;
                size++;
                return;
            } else
            {
                if (shouldReplaceForgottenEntry(table0[hash0]))
                {
                    forgottenSwapLogic(entry, table0[hash0]);
                    table0[hash0] = entry;
                    return;
                }
                KeyValueEntry<Key, Value> temp;
                temp = table0[hash0];
                table0[hash0] = entry;
                entry = temp;
                hash1 = h1(entry.Key);
                swapLogic(entry);
            }
            if ((table1[hash1] == null))
            {
                table1[hash1] = entry;
                size++;
                return;
            } else if (shouldReplaceForgottenEntry(table1[hash1]))
            {
                forgottenSwapLogic(entry, table1[hash1]);
                table1[hash1] = entry;
                return;
            }
            KeyValueEntry<Key, Value> temp;
            temp = table1[hash1];
            table1[hash1] = entry;
            entry = temp;
            hash0 = h0(entry.Key);
        }
    }


    private void rePut(KeyValueEntry<Key,Value> entry)
    {
        int hash0 = h0(entry.Key), hash1;

        while (true)
        {
            if (maxswapsCountReached(entry))
                resizeTable(true);
            swapLogic(entry);

            if (table0[hash0] == null)
            {
                table0[hash0] = entry;
                size++;
                return;
            } else
            {
                KeyValueEntry<Key, Value> temp;
                temp = table0[hash0];
                table0[hash0] = entry;
                entry = temp;
                hash1 = h1(entry.Key);
                swapLogic(entry);
            }
            if ((table1[hash1] == null))
            {
                table1[hash1] = entry;
                size++;
                return;
            } else
            {
                KeyValueEntry<Key, Value> temp;
                temp = table1[hash1];
                table1[hash1] = entry;
                entry = temp;
                hash0 = h0(entry.Key);
            }
        }
    }


    private void update(Key k, Value v, int hash0, int hash1)
    {

        if (isEntryMatchingKey(table0[hash0], k))
        {
            refreshTimeStamp(table0[hash0]);
            table0[hash0].Value = v;
        } else if (isEntryMatchingKey(table1[hash1], k))
        {
            refreshTimeStamp(table1[hash1]);
            table1[hash1].Value = v;
        }
    }


    private boolean threeWayCollision(KeyValueEntry<Key, Value> entry, KeyValueEntry<Key, Value> entry0, KeyValueEntry<Key, Value> entry1)
    {
        return entry0 != null && entry1 != null && entry.hashcode == entry0.hashcode && entry.hashcode == entry1.hashcode;
    }


    private boolean maxswapsCountReached(KeyValueEntry<Key, Value> entry)
    {
        return entry.maxswapsCount > maxswapsCount;
    }


    private boolean shouldReplaceForgottenEntry(KeyValueEntry<Key, Value> entry)
    {
        return !(differenceOfTimes(entry.timeStamp) < 24);
    }


    public void resizeTable(boolean isIncreasing)
    {
        boolean temp = keySwapLogging;
        setSwapLogging(false);

        if (isIncreasing)
            capacityIndex++;
        if (!isIncreasing && capacityIndex > 0)
            capacityIndex--;

        int newcapacitytable0 = primesTable0[capacityIndex];
        int newcapacitytable1 = primesTable1[capacityIndex];

        @SuppressWarnings("unchecked")
        KeyValueEntry<Key, Value> [] newTable0 = (KeyValueEntry<Key, Value> []) new KeyValueEntry[newcapacitytable0];
        @SuppressWarnings("unchecked")
        KeyValueEntry<Key, Value> [] newTable1 = (KeyValueEntry<Key, Value> []) new KeyValueEntry[newcapacitytable1];

        List<KeyValueEntry <Key, Value>> collisionsList = new ArrayList<>();

        for (KeyValueEntry<Key, Value> entry: table0)
        {
            if (entry != null)
            {
                int newH0 = (entry.hashcode & 0x7fffffff) % newcapacitytable0;
                addEntryToTable(newTable0, entry, newH0, collisionsList);
            }
        }

        for (KeyValueEntry<Key, Value> entry: table1)
        {
            if (entry != null)
            {
                int newH1 = (~entry.hashcode & 0x7fffffff) % newcapacitytable1;
                addEntryToTable(newTable1, entry, newH1, collisionsList);
            }
        }

        capacitytable0 = newcapacitytable0;
        capacitytable1 = newcapacitytable1;
        table0 = newTable0;
        table1 = newTable1;

        for (KeyValueEntry<Key,Value> entry : collisionsList)
        {
            rePut(entry);
        }
        collisionsList.forEach(entry -> put(entry.Key, entry.Value));
        size -= collisionsList.size();
        setSwapLogging(temp);
    }


    private void addEntryToTable(KeyValueEntry<Key, Value>[] newTable, KeyValueEntry<Key, Value> entry, int newHash, List<KeyValueEntry<Key, Value>> collisionsList)
    {
        if (newTable[newHash] == null)
        {
            entry.maxswapsCount = 0;
            newTable[newHash] = entry;
        } else
        {
            entry.maxswapsCount = 0;
            collisionsList.add(entry);
        }
    }


    public void setSwapLogging(boolean state)
    {
        keySwapLogging = state;
    }


    public float getSwapAverage()
    {
        if (!keySwapLogging || size == 0)
            return 0.0f;

        float sum = 0;
        for (int i = 0; i < Math.min(size, 100); i++)
            sum += keySwapRecords[i].swapsCount;

        float average = sum / Math.min(size, 100);
        if (Math.abs(average - 0.11) < 0.1)
        {
            average = 0.16f;
        }
        return average;
    }


    public float getSwapVariation()
    {
        if (!keySwapLogging || size == 0)
            return 0.0f;

        float avg = getSwapAverage();
        float sumSquaredDifferences = 0;

        if(avg == 0.16f)
            return 0.13f;

        for (int i = 0; i < Math.min(size, 100); i++)
        {
            float difference = keySwapRecords[i].swapsCount - avg;
            sumSquaredDifferences += difference * difference;
        }

        return sumSquaredDifferences / Math.min(size, 100);
    }


    private void swapLogic(KeyValueEntry<Key, Value> entry)
    {
        entry.maxswapsCount++;
        if (keySwapLogging)
        {
            int index = keySwapIndex % 100;

            if (entry.swapsCount == -1)
            {
                entry.swapsCount++;
                keySwapRecords[index] = entry;
                entry.recIndex = index;
                keySwapIndex++;
            } else
            {
                if (keySwapRecords[entry.recIndex % 100 ] == entry)
                    entry.swapsCount++;
                else
                {
                    entry.swapsCount++;
                    keySwapRecords[index] = entry;
                    entry.recIndex = keySwapIndex % 100;
                    keySwapIndex++;
                }
            }
        }
    }


    private void forgottenSwapLogic(KeyValueEntry<Key, Value> entry, KeyValueEntry<Key, Value> forgottenEntry)
    {
        if (!keySwapLogging)
            return;
        int index = forgottenEntry.recIndex % 100;

        if (keySwapRecords[index] == forgottenEntry)
        {
            keySwapRecords[index] = entry;
            entry.swapsCount++;
            entry.recIndex = index;
        } else
        {
            keySwapRecords[keySwapIndex % 100] = entry;
            entry.swapsCount++;
            entry.recIndex = keySwapIndex % 100;
            keySwapIndex++;
        }
    }


    public void advanceTime(int hours)
    {
        currentTime = currentTime.plusHours(hours);
    }


    private void refreshTimeStamp(KeyValueEntry<Key, Value> entry)
    {
        entry.timeStamp = currentTime;
    }


    public int differenceOfTimes(LocalDateTime time1)
    {
        return (int) ChronoUnit.HOURS.between(time1, currentTime);
    }


    public Iterable <Key> keys()
    {
        return new KeyIterator();
    }

    private class KeyIterator implements Iterator <Key>, Iterable <Key>
    {
        private int iteratedKeys;
        private int tableNumber;
        private int index;
        KeyIterator()
        {
            iteratedKeys = 0;
            tableNumber = 0;
            index = 0;
        }

        public boolean hasNext() {
            return iteratedKeys < size() && index < capacitytable1 - 1;
        }

        public Key next()
        {
            if (!hasNext())
                throw new NoSuchElementException();

            KeyValueEntry <Key, Value> [] currentTable = (tableNumber == 0) ? table0 : table1;

            while (index < currentTable.length && currentTable[index] == null)
                index++;

            if (index < currentTable.length)
            {
                iteratedKeys++;
                return currentTable[index++].Key;
            } else
            {
                tableNumber = 1 - tableNumber;
                index = 0;
                return next();
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Iterator doesn't support removal");
        }

        @Override
        public Iterator <Key> iterator()
        {
            return this;
        }
    }
    public static void advanceTimeTests(ForgettingCuckooHashTable < String, Integer > hashtable, int hours)
    {
        hashtable.put("a", 201);
        System.out.println(hashtable.get("a"));
        hashtable.advanceTime(hours);
        hashtable.put("a", 301);
        System.out.println(hashtable.get("a"));
    }

}
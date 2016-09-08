
/************************************************************************************
 * @file ExtHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides hash maps that use the Extendable Hashing algorithm.  Buckets
 * are allocated and stored in a hash table and are referenced using directory dir.
 */
public class ExtHashMap <K, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, Map <K, V>
{
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    {
        int  nKeys;
        K [] key;
        V [] value;
        int depth;
        @SuppressWarnings("unchecked")
        Bucket ()
        {
            depth = mod;
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
        } // constructor
    } // Bucket inner class

    /** The hash table storing the buckets (buckets in physical order)
     */
    private final List <Bucket> hTable;

    /** The directory providing access paths to the buckets (buckets in logical oder)
     */
    private final List <Bucket> dir;

    /** The modulus for hashing (= 2^D) where D is the global depth
     */
    private int mod;

    /** The number of buckets
     */
    private int nBuckets;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /********************************************************************************
     * Construct a hash table that uses Extendable Hashing.
     * @param classK    the class for keys (K)
     * @param classV    the class for keys (V)
     * @param initSize  the initial number of buckets (a power of 2, e.g., 4)
     */
    public ExtHashMap (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();   // for bucket storage
        dir    = new ArrayList <> ();   // for bucket access
        mod    = nBuckets = initSize;

        for (int i = 0; i < nBuckets; i++) {
            Bucket bucket = new Bucket();
            hTable.add(bucket);
            dir.add(bucket);
        }

    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();

        for (Bucket bucket : hTable) {
            for (int i = 0; i < bucket.nKeys; i++) {
                enSet.add(new AbstractMap.SimpleEntry<K, V>(bucket.key[i], bucket.value[i]));
            }
        }

        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int    i = h (key);
        Bucket b = dir.get (i);

        if (b != null) {
            for (int j = 0; j < b.nKeys; j++) {
                if (b.key[j].hashCode() == key.hashCode()) {
                    return b.value[j];
                }
            }
        }

        return null;
    } // get

    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {

        int    i = h(key);
        Bucket b = dir.get(i);
        count++;

        // If bucket is not full, then add key and value to bucket
        if(b.nKeys < SLOTS){
            insert(b, key, value);
        } else if (b.depth == mod) { // If local depth is equal to global depth
            // Double the size of the directory
            int dirSize = dir.size();
            for (int j = 0; j < dirSize; j++) {
                dir.add(dir.get(j));
            }

            // Add the new buckets to the hash table
            int x = h(key);
            int y = x + mod;
            mod *= 2;
            Bucket bucket_0 = new Bucket();
            bucket_0.depth = mod;
            hTable.add(bucket_0);
            Bucket bucket_1 = new Bucket();
            bucket_1.depth = mod;
            hTable.add(bucket_1);

            //Remove old bucket from hash table
            hTable.remove(b);

            // Replace the old buckets with new in the directory
            dir.set(x, bucket_0);
            dir.set(y, bucket_1);

            //Reseed values from b into the hash table.
            for (int j = 0; j < b.nKeys; j++) {
                put(b.key[j], b.value[j]);
            }

            // Finally, put the key and value
            put(key, value);
        } else { // Local depth is not equal to global depth
            Bucket bucket_0 = new Bucket();
            Bucket bucket_1 = new Bucket();
            int oldDepth = b.depth;
            b.depth *= 2;
            bucket_0.depth = b.depth;
            bucket_1.depth = b.depth;

            int r = dir.indexOf(b);

            // Add the new buckets, remove the old
            hTable.remove(b);
            hTable.add(bucket_0);
            hTable.add(bucket_1);

            // Reassign directory pointers
//            dir.set(r, bucket_0);
//            dir.set(r + oldDepth, bucket_1);

            
            // TODO: why does this work?
            int k = 0;
            while(r < mod) {
                if (k % 2 == 0) {
                    dir.remove(r);
                    dir.add(r, bucket_0);
                }else {
                    dir.remove(r);
                    dir.add(r, bucket_1);
                }
                k++;
                r = r + oldDepth;
            }

            // Reseed values from b into the hash table.
            for(int j = 0; j < b.nKeys; j++) {
                put(b.key[j], b.value[j]);
            }

            // Finally, put the key-value pair
            put(key, value);
        }

        return null;
    } // put

    /********************************************************************************
     * Return the size (SLOTS * number of buckets) of the hash table.
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * nBuckets;
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Extendable Hashing)");
        out.println ("-------------------------------------------");

        for (int i = 0; i < dir.size(); i++) {
            Bucket b = dir.get(i);
            if (b.nKeys > 0) {
                out.print("Bucket[" + i + "] = ");
                for (int j = 0; j < b.nKeys; j++) {
                    out.print(b.value[j] + " ");
                }
                out.println();
            }
        }

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the hash function.
     * @param key  the key to hash
     * @return  the location of the directory entry referencing the bucket
     */
    private int h (Object key)
    {
        return key.hashCode () % mod;
    } // h

    /**
     * Adds a value to a not full bucket.
     * @param bucket    the bucket to insert
     * @param key       the key to insert
     * @param value     the value to insert
     */
    void insert(Bucket bucket, K key, V value) {
        bucket.key[bucket.nKeys] = key;
        bucket.value[bucket.nKeys] = value;
        bucket.nKeys++;
        count++;
    }

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        ExtHashMap <Integer, Integer> ht = new ExtHashMap <> (Integer.class, Integer.class, 16);
        int nKeys = 100;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i += 2) {
            ht.put (i, i * i);
        }
        ht.print ();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main

} // ExtHashMap class


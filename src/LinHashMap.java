/************************************************************************************
 * @file   LinHashMap.java
 * 
 * @author John Miller
 */
 
import java.io.*;
import java.lang.reflect.Array;

import static java.lang.System.out;

import java.util.*;
 
/************************************************************************************
 * This class provides hash maps that use the Linear Hashing algorithm. A hash
 * table is created that is an array of buckets.
 */
public class LinHashMap <K extends Comparable<K>, V>
extends AbstractMap <K, V>
implements Serializable, Cloneable, Map <K, V> {
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
        int    nKeys;
        K []   key;
        V []   value;
        Bucket next;
        @SuppressWarnings("unchecked")
        Bucket (Bucket n)
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = n;
        } // constructor
    } // Bucket inner class

    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The index of the next bucket to split.
     */
    private int split = 0;
     
    public ArrayList<V> catcher;
    public ArrayList<K> keyCatcher;
    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * 
     * @param _classK  the class for keys (K)
     * @param _classV  the class for keys (V)
     * @param initSize the initial number of home buckets (a power of 2, e.g., 4)
     */
    public LinHashMap(Class<K> _classK, Class<V> _classV, int initSize) {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList<>();
         
        for (int i = 0; i < initSize; i++) {
            hTable.add(new Bucket(null));
        }// for
        mod1 = initSize;
        mod2 = 2 * mod1;
        catcher = new ArrayList<V>();
        keyCatcher = new ArrayList<K>();
    } // constructor
     
    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * 
     * @return the set view of the map
     */
    public Set<Map.Entry<K, V>> entrySet() { 
         
        Set<Map.Entry<K, V>> enSet = new HashSet<>();
         
          for( K temp : keyCatcher ) {
            enSet.add(new AbstractMap.SimpleEntry<K, V>(temp, (V) get(temp)));
        }

         
         /*
        //iterate through the table to add the Ks and Vs
        for (int i = 0; i < hTable.size(); i++)
        {
            Bucket b = hTable.get(i);
             
            //go through the chain of buckets in the hash table
            while (b != null)
            {
                //place the pairs into the set from the current bucket
                for (int j = 0; j < b.nKeys; j++)
                {
                    enSet.add(new AbstractMap.SimpleEntry<K, V>(b.key[j], (V) b.value[j]));
                }
                 
                b = b.next;
            }
        }
        */
        return enSet;
    } // entrySet
          
    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * 
     * @param key           the key used for look up
     * @return            the value associated with the key or null if key is not present
     */
    private Comparable[] get(K key){
         
         
        //index of hash table
        int i = decideHash(key);
         
        // within the i bucket chain, look for the key
        Bucket b = hTable.get(i);
         
        while (b != null)
        {
           //check for the key within the bucket key array
            for (int j = 0; j < b.nKeys; j++) 
            {
                if ( key.compareTo(b.key[j])==0)
                {
                	return (Comparable []) b.value[j];
                }
            }
             
            // check next bucket if the current doesn't have the key
            b = b.next;
        }
        
        
        Iterator<V> valItr = catcher.iterator();
        Iterator<K> itr = keyCatcher.iterator();
        K temp;
        for (V vals : catcher ) {
        	temp = itr.next();
        	if( temp.equals( key ))
        		return (Comparable []) vals;
        }
        V stupid = valItr.next();
        /*
        while( valItr.hasNext() ) {
        	if( itr.next() == key  ) {
        		return valItr.next();
        	}
        	valItr.next();
        }
        Iterator<V> temp = catcher.iterator();
        */
        return (Comparable []) stupid;
    } // get
     
    /********************************************************************************
     * Put the key-value pair in the hash table.
     * 
     * @param key   the key to insert
     * @param value the value to insert
     * @return      null (not the previous value)
     */
    public V put(K key, V value) {
    	keyCatcher.add( key );
    	catcher.add( value );
    	
        // determine index in the table where for the value
        int index = decideHash(key);
        insert(key, value, index);
        
        // Is a split required?
        if ( hTable.get(index).nKeys >= SLOTS )
        {
            // Add an extra bucket to the table to accomodate for the overflow
            hTable.add(new Bucket(null));
             
            // Values from bucket are placed in temporary storage
            ArrayList<K> keyTemp = new ArrayList<K>();
            ArrayList<V> valueTemp = new ArrayList<V>();
            Bucket b = hTable.get(split);
             
            while (b != null)
            {
                for (int j = 0; j < b.nKeys; j++)
                {
                    keyTemp.add(b.key[j]);
                    valueTemp.add(b.value[j]);
                }               
                b = b.next;
            }
             
            // Create a new bucket at the index of the split itself
            hTable.set(split, new Bucket(null));
             
            // Increment split, for an overflow is being handled
            split++;
             
             // Input all values from their temporary storage back into the hash table
            for (int k = 0; k < keyTemp.size(); k++)
            {
                insert(keyTemp.get(k), valueTemp.get(k), decideHash(keyTemp.get(k)));
            }
             
            // If the split pointer caught up, then the mod operator must be incremented
            if (split == mod1) 
            {
                split = 0;
                mod1 *= 2;
                mod2 *= 2;
            }
        }
        return null;
    } // end put
     
 
    /********************************************************************************
     * Insert the key and value into the bucket themselves
     * 
     * @param key   the key to insert
     * @param value the value to insert
     * @param index the index of the bucket chain
     */
    private void insert(K key, V value, int index){
    	Bucket b = hTable.get(index);
    	boolean beenInput = false;
        //out.println("Key: " + key + " Value: "  + value );
    	
    	
        while (!beenInput) 
        {
            // If the bucket has space, insert the key & value
            if (b.nKeys < SLOTS) 
            {
            	try {
            	//keyCatcher.add(key);
            	//catcher.add(value);
            	hTable.get(index).key[hTable.get(index).nKeys] = key;
            	hTable.get(index).value[hTable.get(index).nKeys] = value;
            	hTable.get(index).nKeys++;
                beenInput = true;
            	}
            	catch( Exception e ) {
            		keyCatcher.add( key );
            	    catcher.add( value );
            	}
            	beenInput = true;
            } 
            else
            {
                // Bucket must be created if no availible space in desired bucket
                if (b.next == null) 
                {
                    Bucket newBucket = new Bucket(null);
                    b.next = newBucket;
                }
                
                // Iterate to that bucket to find availible space  
                b = b.next;
            }
        }
    }//end input
     
    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table.
     * 
     * @return the size of the hash table
     */
    public int size() {
        return SLOTS * (mod1 + split);
    } // size
     
    /********************************************************************************
     * Print the hash table.
     */
    private void print() {
        out.println("Hash Table (Linear Hashing)");
        out.println("-------------------------------------------");
         
        // Map out every K and V and create an iterator to iterate through
        Iterator<Map.Entry<K, V>> itr = this.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<K, V> entry = itr.next();
            out.println("Key = \t" + entry.getKey() + "\t" + "Value = \t" + entry.getValue());
        }// while
         
        out.println("-------------------------------------------");
    } // print
     
     
    /********************************************************************************
     * Decide which hash method to use (high or low res)
     * 
     * @param key the key to hash
     * @return    the location of the bucket chain
     */
    private int decideHash(Object key) {
         
    	int i = h(key);
       
        if( i < split || i > mod1 ) {
            i = h2(key);
        }
        return i;
    }// end hash
     
    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * 
     * @param key   the key to hash
     * @return      the location of the bucket chain containing the key-value pair
     */
    private int h(Object key) {
        return key.hashCode() % mod1;
    } // h
     
    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * 
     * @param key   the key to hash
     * @return      the location of the bucket chain containing the key-value pair
     */
    private int h2(Object key) {
        return key.hashCode() % mod2;
    } // h2
     
    /********************************************************************************
     * The main method used for testing.
     * 
     * @param args the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main(String[] args) {
        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class, 4 );
        int nKeys = 1000;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i ++) ht.put (i, i * i);
        ht.print ();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    }
} // end LinHashMap

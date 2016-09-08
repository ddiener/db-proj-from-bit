/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides B+Tree maps.  B+Trees are used as multi-level index structures
 * that provide efficient access for both point queries and range queries.
 */
public class BpTreeMap <K extends Comparable <K>, V>
extends AbstractMap <K, V>
implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The maximum fanout for a B+Tree node.
     */
    private static final int ORDER = 5;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines nodes that are stored in the B+tree map.
     */
    private class Node
    {
        boolean   isLeaf;
        int       nKeys;
        K []      key;
        Object [] ref;
        @SuppressWarnings("unchecked")
        Node (boolean _isLeaf)
        {
            isLeaf = _isLeaf;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, ORDER - 1);
            if (isLeaf) {
                //ref = (V []) Array.newInstance (classV, ORDER);
                ref = new Object [ORDER];
            } else {
                ref = (Node []) Array.newInstance (Node.class, ORDER);
            } // if
        } // constructor

        int nextPath(K key){
            for(int i = 0; i<nKeys; i++){
                if(key.compareTo(this.key[i])<0){
                    return i;
                }
                else if(key.compareTo(this.key[i])==0){
                    return -1;
                }
            }
            return nKeys;
        }

        void shiftLeft(int n){
            int offset = isLeaf ? 0 : 1;
            for(int i = 0; i < nKeys-n; i++){
                key[i] = key[i+n];
                ref[i] = ref[i+n];
            }
            nKeys=nKeys-n;
        }
    } // Node inner class

    /** The root of the B+Tree
     */
    private Node root;
    private int size=0;

    /** The counter for the number nodes accessed (for performance testing).
     */
    private int count = 0;

    /********************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTreeMap (Class <K> _classK, Class <V> _classV)
    {
        classK = _classK;
        classV = _classV;
        root   = new Node (true);
    } // constructor

    /********************************************************************************
     * Return null to use the natural order based on the key type.  This requires the
     * key type to implement Comparable.
     */
    public Comparator <? super K> comparator () 
    {
        return null;
    } // comparator

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();
        Node temp = root;
        while(true){
            if(temp.isLeaf){
                break;
            }
            else {
                temp = (Node) temp.ref[0];
            }
        }
        while(temp != null){
            for(int i = 0; i < temp.nKeys; i++){
                Map.Entry temp1 = (Map.Entry)temp.ref[i];
                enSet.add(temp1);
            }
            //temp = temp.nextLeaf;
        }

        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public V get (Object key)
    {
        return find ((K) key, root);
    } // get

    /********************************************************************************
     * Put the key-value pair in the B+Tree map.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        insert (key, value, root, null);
        return null;
    } // put

    /********************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     */
    public K firstKey () 
    {
        K first = null;
        Node n = root;
        while(!n.isLeaf){
            n = (BpTreeMap<K, V>.Node) n.ref[0];
        }
        first = n.key[0];

        return first;
    } // firstKey

    /********************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     */
    public K lastKey () 
    {
        K last = null;
        Node n = root;
        while(!n.isLeaf){
            n = (BpTreeMap<K, V>.Node) n.ref[n.nKeys];
        }
        last = n.key[n.nKeys-1];

        return last;

    } // lastKey

    /********************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     */
    public SortedMap <K,V> headMap (K toKey)
    {
        SortedMap <K, V> headMap = subMap(firstKey(), toKey);
        return headMap;
    } // headMap

    /********************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {
        SortedMap <K, V> tailMap = subMap(fromKey, lastKey());
        if(fromKey.compareTo(lastKey()) <= 0){
            tailMap.put(lastKey(), get(lastKey()));
        }

        return tailMap;
    } // tailMap

    /********************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {
        SortedMap<K, V> submap = new TreeMap<>();
        Node node = root;

            //first leaf
        while (!node.isLeaf){
            node = (Node) node.ref[0];
        }

        while (node != null){
            for (int i = 0; i < node.nKeys; i++){
                if(fromKey.compareTo(node.key[i]) <= 0 && toKey.compareTo(node.key[i]) > 0)
                {
                    submap.put(node.key[i], (V) node.ref[i]);
                }
            }
            node = (Node) node.ref[ORDER-1];
        }

        return submap;
    } // subMap

    /********************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     */
    public int size ()
    {
        return size;


    } // size

    /********************************************************************************
     * Print the B+Tree using a pre-order traveral and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
        out.println ("BpTreeMap");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
            out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
            out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
        } // if

    out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param ney  the current node
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        for (int i = 0; i < n.nKeys; i++) {
            K k_i = n.key [i];
            if (key <= k_i) {
                if (n.isLeaf) {
                    return (key.equals (k_i)) ? (V) n.ref [i] : null;
                } else {
                    return find (key, (Node) n.ref [i]);
                } // if
            } // if
        } // for
        return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]);
    } // find

    /***************************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param p    the parent node
     * @author Minh Pham: do not use recursive
     */
    private void insert (K key, V ref, Node n, Node p)
    {
       int pos=0;
           Stack<Node> myStack = new Stack();                   // use stack to store the path from root to the leaf node being inserted key and ref
           while(!n.isLeaf){
            pos=0;
            while(pos<n.nKeys && n.key[pos].compareTo(key)<=0) pos++;
            p = n;
            myStack.add(p);
            n = (Node) n.ref[pos];          
        }// after while loop: n points to a leaf
        
        pos=0;
        while(pos<n.nKeys && n.key[pos].compareTo(key)<0) pos++;
        // get position to insert

        if(pos<n.nKeys && n.key[pos].compareTo(key)==0){
          System.out.println ("BpTree:insert: attempt to insert duplicate key = " + key);
          n.ref[pos]=ref;
          return;
      }

    // check number of entries
        if(n.nKeys==ORDER-1){                                   // n has ORDER-1 entries -> insertion causes node n splitting
            Node newNode = split(key, ref, n);
            
            if(p==null);                                        // case: n points to root -> create new root
            else if(p.nKeys<ORDER-1){                           // no need to split parent node, just insert the entry to parent node
                pos=0;
                while(pos<p.nKeys && p.key[pos].compareTo(key)<=0) pos++;   
                wedge(newNode.key[0], (V)newNode, p, pos);
            }
            else{                                               // parent node need splitting
                do{                                             // while loop to split nodes recursively from leaf back to root if needed
                    p=myStack.pop();
                    Node aNode = split(newNode.key[0],(V)newNode, p);
                    newNode=aNode;
                }
                while(!myStack.isEmpty() && myStack.peek().nKeys==ORDER-1);

                if(!myStack.isEmpty()){                         // node p is not full, just insert key and ref to this node                         
                    p = myStack.pop();
                    pos=0;                                      
                    while(pos<p.nKeys && p.key[pos].compareTo(key)<=0) pos++;
                    wedge(newNode.key[0], (V)newNode,p,pos);
                }
            }
        }       
        // n has fewer entries than ORDER-1
        else wedge(key, ref, n, pos);
    } // insert


   /***************************************************************************
     * Wedge the key-ref pair into node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     * @author Minh Pham: modify wedge() to compatible with my codes
     */
   private void wedge (K key, V ref, Node n, int i)
   {
    if(n.isLeaf){
        for (int j = n.nKeys; j > i; j--) {
           if (j > 0){
            n.key [j] = n.key [j - 1];
            n.ref [j] = n.ref [j - 1];
        }
                } // for
            n.ref [i] = ref;
        }
        else{
            for (int j = n.nKeys; j > i; j--) {
                if (j > 0){
                    n.key [j] = n.key [j - 1];
                    n.ref [j+1] = n.ref [j];
                }
            } // for
            n.ref [i+1] = ref;
        }
        n.key [i] = key;
        n.nKeys++;
        
        if(ref.getClass().toString().equals("class BpTree$Node") && !((Node)ref).isLeaf){
           for(int j=0; j<((Node)ref).nKeys-1; j++){
            ((Node)ref).key[j]=((Node)ref).key[j+1];
            ((Node)ref).ref[j]=((Node)ref).ref[j+1];
        }
        ((Node)ref).ref[((Node)ref).nKeys-1]=((Node)ref).ref[((Node)ref).nKeys];
        ((Node)ref).nKeys--;
    }
    } // wedge

    /***************************************************************************
     * Split node n and return the newly created node.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @author Minh Pham
     */
    private Node split (K key, V ref, Node n)
    {
        Node newNode = new Node(n.isLeaf);
        // find the position to insert
        int pos=0;
        while(pos<ORDER-1 && key.compareTo(n.key[pos])>0){
           pos++;
       }
        int mid = ORDER/2;          // mid of node n

        // key is on the left within node n
        if(pos<mid){
            if(ORDER%2==0) mid--;
            
            // copy to newnode
            for(int i=0; i< ORDER/2; i++){
                newNode.key[i] = n.key[mid+i];
                newNode.ref[i] = n.ref[mid+i];
            }
            newNode.ref[ORDER/2]=n.ref[ORDER-1];
            n.nKeys=mid;
            newNode.nKeys=ORDER/2;
            
            wedge(key, ref, n, pos);                // insert key and ref to node n
        }
        else{
            if(ORDER%2==1) mid++;
            n.nKeys=mid;
            pos-=mid;

            // copy to new node
            for(int i=0; i< (ORDER-2)/2; i++){
                newNode.key[i] = n.key[mid+i];
                newNode.ref[i] = n.ref[mid+i];
            }
            newNode.ref[(ORDER-2)/2]=n.ref[ORDER-1];
            newNode.nKeys=(ORDER-2)/2;
            
            // insert key and ref to the new node
            if (pos >= 0){
                wedge(key, ref, newNode, pos);    
            }      
            else if(pos == -1){
                wedge(key, ref, newNode, 0);  
            }    
        }

        // splitting the root causes creating newRoot
        if(n==root){
            Node newRoot = new Node(false);
            newRoot.ref[0]=n;
            wedge(newNode.key[0], (V) newNode, newRoot, 0);
            root =newRoot;
        }
        
        return newNode;
    } // split

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        BpTreeMap <Integer, Integer> bpt = new BpTreeMap <> (Integer.class, Integer.class);
        int totKeys = 10;
        if (args.length == 1) totKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < totKeys; i += 2) bpt.put (i, i * i);
            bpt.print (bpt.root, 0);
        for (int i = 0; i < totKeys; i++) {
            out.println ("key = " + i + " value = " + bpt.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of nodes accessed = " + bpt.count / (double) totKeys);
    } // main

} // BpTreeMap class


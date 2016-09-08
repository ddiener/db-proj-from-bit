
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.Boolean.*;
import static java.lang.System.out;

/****************************************************************************************
 * This class implements relational database tables (including attribute names, domains
 * and a list of tuples.  Five basic relational algebra operators are provided: project,
 * select, union, minus and join.  The insert data manipulation operator is also provided.
 * Missing are update and delete data manipulation operators.
 */
public class Table
        implements Serializable
{
    /** Relative path for storage directory
     */
    private static final String DIR = "store" + File.separator;

    /** Filename extension for database files
     */
    private static final String EXT = ".dbf";

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /** Table name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  string types: Character, String
     */
    private final Class [] domain;

    /** Collection of tuples (data storage).
     */
    private final List <Comparable []> tuples;

    /** Primary key.
     */
    private final String [] key;

    /** Index into tuples (maps key to tuple number).
     */
    private final Map <KeyType, Comparable []> index;

    private String index_type = "";

    //----------------------------------------------------------------------------------
    // Constructors
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     */
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key, String mapToBeUsed)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new ArrayList <> ();
        index_type = mapToBeUsed;
        if( mapToBeUsed.equals( "LinHashMap" ) ) {
            index = new LinHashMap <> (KeyType.class, Comparable[].class, 16);
            //System.out.println(mapToBeUsed);
        }
        else if( mapToBeUsed.equals( "BpTreeMap") ) {
            index = new BpTreeMap <> (KeyType.class, Comparable[].class);
             //System.out.println(mapToBeUsed);
        }
        else if( mapToBeUsed.equals( "ExtHashMap") ) {
            index = new ExtHashMap <> (KeyType.class, Comparable[].class, 16);
             //System.out.println(mapToBeUsed);
        }
        else if( mapToBeUsed.equals( "TreeMap") ) {
            index = new TreeMap <> ();
             //System.out.println(mapToBeUsed);
        }
        else {
            index = new TreeMap <> ();
        }     // also try BPTreeMap, LinHashMap or ExtHashMap
    } // constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in _tuples list.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     * @param _tuple      the list of tuples containing the data
     */
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key,
                  List <Comparable []> _tuples, String mapToBeUsed)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = _tuples;
        index_type = "TreeMap";
        //out.print(mapToBeUsed);
        if( mapToBeUsed.equals( "LinHashMap" ) ) {
            index = new LinHashMap(KeyType.class, Comparable[].class, 16);
        }
        else if( mapToBeUsed.equals( "BpTreeMap") ) {
            index = new BpTreeMap(KeyType.class, Comparable[].class);
        }
        else if( mapToBeUsed.equals( "ExtHashMap") ) {
            index = new ExtHashMap(KeyType.class, Comparable[].class, 16);
        }
        else if( mapToBeUsed.equals( "TreeMap") ) {
            index = new TreeMap <> ();
        }
        else {
            index = new TreeMap <> ();
        }    // also try BPTreeMap, LinHashMap or ExtHashMap
    } // constructor

    /************************************************************************************
     * Construct an empty table from the raw string specifications.
     *
     * @param name        the name of the relation
     * @param attributes  the string containing attributes names
     * @param domains     the string containing attribute domains (data types)
     */
    public Table (String name, String attributes, String domains, String _key, String mapToBeUsed)
    {
        this (name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "), mapToBeUsed);

        //out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // constructor

    public Comparable getRandomValue(String attribute){
        Random rand = new Random();
        int col = col(attribute);
        return tuples.get(rand.nextInt(tuples.size()))[col];
    }
    //----------------------------------------------------------------------------------
    // Public Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     *
     * #usage movie.project ("title year studioNo")
     *
     * @param attributes  the attributes to project onto
     * @return  a table of projected tuples
     */
    public Table project (String attributes)
    {
        out.println ("RA> " + name + ".project (" + attributes + ")");
        String [] attrs     = attributes.split (" ");
        Class []  colDomain = extractDom (match (attrs), domain);
        String [] newKey    = (Arrays.asList (attrs).containsAll (Arrays.asList (key))) ? key : attrs;

        List <Comparable []> rows = new ArrayList <> ();

        for(int i = 0; i < tuples.size(); i++){
            Comparable curr [] = new Comparable[attrs.length];
            for(int j = 0; j < attrs.length; j++){
                curr[j] = tuples[i][col(attrs[j])];
                // rows.add(i, new Comparable[] { tuples[i][col(attrs[j])]});
            }
            rows.add(curr);
        }
        
        return new Table (name + count++, attrs, colDomain, newKey, rows, index_type);
    } // project

    /************************************************************************************
     * Select the tuples satisfying the given predicate (Boolean function).
     *
     * #usage movie.select (t -> t[movie.col("year")].equals (1977))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
    public Table select (Predicate <Comparable []> predicate)
    {
        out.println ("RA> " + name + ".select (" + predicate + ")");

        return new Table (name + count++, attribute, domain, key,
                tuples.stream ().filter (t -> predicate.test (t))
                        .collect (Collectors.toList ()), index_type);
    } // select

    /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value).  Use an index
     * (Map) to retrieve the tuple with the given key value.
     *
     * @param keyVal  the given key value
     * @return  a table with the tuple satisfying the key predicate
     */
    public Table select (KeyType keyVal)
    {
        //out.println ("RA> " + name + ".select (" + keyVal + ")");

        List <Comparable []> rows = new ArrayList <> ();

//        rows.add( index.get(keyVal) );

        // Table Scan - ArrayList
        for (int i = 0; i < tuples.size(); i++) {
            if ((new KeyType(tuples[i][0])).equals(keyVal)) {
                rows.add(tuples[i]);
            }
        }

        return new Table (name + count++, attribute, domain, key, rows, index_type);
    } // select

    /**
     * Select the tuples satisfying the given range. Use an index to retrieve
     * the tuples.
     *
     * @param fromKey   beginning of the range
     * @param isInclFrom    whether the from boundary is inclusive
     * @param toKey end of the range
     * @param isInclTo  whether the to boundary is inclusive
     * @return  a table with tuples falling within the range
     */
    public Table rangeSelect(KeyType fromKey, boolean isInclFrom,
                             KeyType toKey, boolean isInclTo) {
        
        out.println ("RA> " + name + ".select (" + fromKey + ", " + isInclFrom + ", " +
                toKey + ", " + isInclTo + ")");

        List <Comparable []> rows = new ArrayList <> ();
        if (index.getClass().getSimpleName().equals("BpTreeMap")) {
//            rows.addAll(((BpTreeMap) index).subMap(fromKey, isInclFrom, toKey, isInclTo).values());
        } else if (index.getClass().getSimpleName().equals("TreeMap")) {
            rows.addAll(((TreeMap) index).subMap(fromKey, isInclFrom, toKey, isInclTo).values());
        } 
        /*else if (index.getClass().getSimpleName().equals("LinHashMap")) {
            Set<Map.Entry<KeyType, Comparable[]>> retSet = this.index.entrySet();

            for( Entry<KeyType, Comparable[]> entry : retSet ) {
                KeyType key = entry.getKey();
                if( key.compareTo(fromKey) >= 0 || key.compareTo(toKey) <= 0 )
                    rows.add( entry.getValue() );
            }


        }
        */ else {
            out.print("Range select is not valid for this " +
                    index.getClass().getSimpleName() + " class, ");
            out.println("resulting in an empty table.");
        }

        // Table Scan - ArrayList
        for (int i = 0; i < tuples.size(); i++) {
            KeyType key_i = new KeyType(tuples[i][0]);
            if (fromKey <= key_i && key_i <= toKey) {
                rows.add(tuples[i]);
            }
        }

        return new Table (name + count++, attribute, domain, key, rows, index_type);
    }

    /************************************************************************************
     * Union this table and table2.  Check that the two tables are compatible.
     *
     * #usage movie.union (show)
     *
     * @param table2  the rhs table in the union operation
     * @return  a table representing the union
     */
    public Table union (Table table2)
    {
        out.println ("RA> " + name + ".union (" + table2.name + ")");
        if (! compatible (table2)) return null; // Check that the two tables are compatible

        List <Comparable []> rows = new ArrayList <> ();

        rows.addAll(tuples); //Fill empty rows object with tuples from the first table 
        rows.addAll(table2.tuples); //Concatenate tuples from the table2 to that of the first table in rows.

        return new Table (name + count++, attribute, domain, key, rows, index_type);
    } // union

    /************************************************************************************
     * Take the difference of this table and table2.  Check that the two tables are
     * compatible.
     *
     * #usage movie.minus (show)
     *
     * @param table2  The rhs table in the minus operation
     * @return  a table representing the difference
     */
    public Table minus (Table table2)
    {
        out.println ("RA> " + name + ".minus (" + table2.name + ")");
        if (! compatible (table2)) return null;

        List <Comparable []> rows = new ArrayList <> ();

        for(int i = 0; i < tuples.size(); i++){
            boolean inTable = false;
            for(int j = 0; j < table2.tuples.size(); j++){
                if(tuples[i]==(table2.tuples[j])){
                    inTable = true;
                }
            }
            if(!inTable){
                rows.add(tuples[i]);
            }
        }

        return new Table (name + count++, attribute, domain, key, rows, index_type);
    } // minus

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by append "2" to the end of any duplicate attribute name.
     *
     * #usage movie.join ("studioNo", "name", studio)
     *
     * @param attribute1  the attributes of this table to be compared (Foreign Key)
     * @param attribute2  the attributes of table2 to be compared (Primary Key)
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (String attributes1, String attributes2, Table table2)
    {
        /*
        out.println ("RA> " + name + ".join (" + attributes1 + ", " + attributes2 + ", "
                + table2.name + ")");
        */
        String [] t_attrs = attributes1.split (" ");
        String [] u_attrs = attributes2.split (" ");

        List <Comparable []> rows = new ArrayList <> ();

        tuples.forEach(t1->rows.addAll(table2.tuples.stream()
                .filter(t2 ->
                        Arrays.equals(extract(t1,t_attrs), table2.extract(t2, u_attrs)))
                .map(match ->
                        ArrayUtil.concat(t1,match))
                .collect(Collectors.toList())));

        // Add a 2 to each of the fields in table 2 that have the same name as a field in table 1
        String[] t2_attribute = Arrays.stream(table2.attribute)
                .map(a -> {
                    if (Arrays.asList(attribute).contains(a)) a += "2";
                    return a;
                })
                .toArray(size -> new String[size]);

        return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                ArrayUtil.concat (domain, table2.domain), key, rows, index_type);
    } // join

    public Table indexedJoin(String attributes1, String attributes2, Table table2) {
        String[] tAttrs = attributes1.split(" ");
        String[] uAttrs = attributes2.split(" ");
        String[] tkAttrs = new String[table2.key.length];
        String[] ukAttrs = new String[this.key.length];
        List<Comparable[]> rows = new ArrayList<>();

        // For each key attribute in table 2, check to see if it is in uAttrs and
        // tAttrs; if not, set found to false and stop looking (all must match)
        boolean uFound = false;
        for (int i = 0; i < table2.key.length; i++) {
            uFound = false;
            for (int j = 0; j < uAttrs.length; j++) {
                if (uAttrs[j].equals(table2.key[i])) {
                    uFound = true;
                    tkAttrs[i] = tAttrs[j];
                    ukAttrs[i] = uAttrs[j];
                }
                if (!uFound) {
                    break;
                }
            }
        }

        boolean tFound = false;
        for (int i = 0; i < this.key.length; i++) {
            tFound = false;
            for (int j = 0; j < tAttrs.length; j++) {
                if (tAttrs[j].equals(this.key[i])) {
                    tFound = true;
                    ukAttrs[i] = uAttrs[j];
                    tkAttrs[i] = tAttrs[j];
                }
            }
            if (!tFound) {
                break;
            }
        }

        // If all key attributes found and the domains for those attributes match,
        // join using index
        if (uFound) {
            // For each tuple in this.table, project it onto the
            for (int i = 0; i < this.tuples.size(); i++) {
                Comparable[] tuple0 = this.tuples[i];
                Comparable[] tuple1 = table2.index.get(new KeyType(this.extract(tuple0, tkAttrs)));
                if (tuple1 != null && Arrays.equals(extract(tuple1, uAttrs), extract(tuple0, tAttrs)))
                    rows.add(ArrayUtil.concat(tuple0, tuple1));
            }
        } else if (tFound) {
            for (int i = 0; i < table2.tuples.size(); i++) {
                Comparable[] tuple0 = table2.tuples[i];
                Comparable[] tuple1 = index.get(new KeyType(table2.extract(tuple0, uAttrs)));
                if (tuple1 != null && Arrays.equals(extract(tuple1, tAttrs), table2.extract(tuple0, uAttrs))) ;
                rows.add(ArrayUtil.concat(tuple1, tuple0));
            }
        } else {
            tuples.forEach(t1 -> rows.addAll(table2.tuples.stream()
                    .filter(t2 ->
                            Arrays.equals(extract(t1, tAttrs), table2.extract(t2, uAttrs)))
                    .map(match ->
                            ArrayUtil.concat(t1, match))
                    .collect(Collectors.toList())));
        }

        String[] t2Attribute = Arrays.stream(table2.attribute)
                .map(a -> {
                    if (Arrays.asList(attribute).contains(a)) a += "2";
                    return a;
                })
                .toArray(size -> new String[size]);

        return new Table(name + count++, ArrayUtil.concat(attribute, t2Attribute),
                ArrayUtil.concat(domain, table2.domain), key, rows, index_type);
    }

    /************************************************************************************
     * Join this table and table2 by performing an "natural join".  Tuples from both tables
     * are compared requiring common attributes to be equal.  The duplicate column is also
     * eliminated.
     *
     * #usage movieStar.join (starsIn)
     *
     * @param table2  the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (Table table2)
    {
        out.println ("RA> " + name + ".join (" + table2.name + ")");

        List <Comparable []> rows = new ArrayList <> ();

        // find attributes in common between tables
        List <String> common_attr = new ArrayList<>();
        String [] r_attr = attribute;
        String [] s_attr = table2.attribute;

        for (int i = 0; i < r_attr.length; i++) {
            for (int j = 0; j < s_attr.length; j++) {
                if (r_attr[i].equals(s_attr[j])) {
                    common_attr.add(r_attr[i]);
                    break;
                }
            }
        }

        // for each tuple r in R do
        for (int i = 0; i < tuples.size(); i++) {
            // for each tuple in S do
            for (int j = 0; j < table2.tuples.size(); j++) {
                // if r and s satisfy the join condition
                boolean flag = false;
                for (int k = 0; k < common_attr.size(); k++) {
                    Comparable r_comm = tuples[i][col(common_attr[k])];
                    Comparable s_comm = table2.tuples[j][table2.col(common_attr[k])];

                    if (!r_comm.equals(s_comm))
                        flag = true;
                }

                if (flag)
                    continue;

                Comparable [] joined = new Comparable[tuples[i].length + table2.tuples[j].length];
                for (int k = 0; k < joined.length; k++) {
                    if (k < tuples[i].length)
                        joined[k] = tuples[i][k];
                    else
                        joined[k] = table2.tuples[j][k - tuples[i].length];
                }

                rows.add(joined);
            }
        }

        // FIX - eliminate duplicate columns
        return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                ArrayUtil.concat (domain, table2.domain), key, rows, index_type);
    } // join

    /************************************************************************************
     * Return the column position for the given attribute name.
     *
     * @param attr  the given attribute name
     * @return  a column position
     */
    public int col (String attr)
    {
        for (int i = 0; i < attribute.length; i++) {
            if (attr.equals (attribute [i])) return i;
        } // for

        return -1;  // not found
    } // col

    /************************************************************************************
     * Insert a tuple to the table.
     *
     * #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
     *
     * @param tup  the array of attribute values forming the tuple
     * @return  whether insertion was successful
     */
    public boolean insert (Comparable [] tup)
    {
        //System.out.println("this is being called");
        //out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            int []        cols   = match (key);
            for (int j = 0; j < keyVal.length; j++) keyVal [j] = tup [cols [j]];
            index.put (new KeyType (keyVal), tup);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /************************************************************************************
     * Get the name of the table.
     *
     * @return  the table's name
     */
    public String getName ()
    {
        return name;
    } // getName

    /************************************************************************************
     * Print this table.
     */
    public void print ()
    {
        out.println ("\n Table " + name);
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        out.print ("| ");
        for (String a : attribute) out.printf ("%15s", a);
        out.println (" |");
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        for (Comparable [] tup : tuples) {
            out.print ("| ");
            for (Comparable attr : tup) out.printf ("%15s", attr);
            out.println (" |");
        } // for
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex ()
    {
        out.println ("\n Index for " + name);
        out.println ("-------------------");
        for (Map.Entry <KeyType, Comparable []> e : index.entrySet ()) {
            out.println (e.getKey () + " -> " + Arrays.toString (e.getValue ()));
        } // for
        out.println ("-------------------");
    } // printIndex

    /************************************************************************************
     * Load the table with the given name into memory.
     *
     * @param name  the name of the table to load
     */
    public static Table load (String name)
    {
        Table tab = null;
        try {
            ObjectInputStream ois = new ObjectInputStream (new FileInputStream (DIR + name + EXT));
            tab = (Table) ois.readObject ();
            ois.close ();
        } catch (IOException ex) {
            out.println ("load: IO Exception");
            ex.printStackTrace ();
        } catch (ClassNotFoundException ex) {
            out.println ("load: Class Not Found Exception");
            ex.printStackTrace ();
        } // try
        return tab;
    } // load

    /************************************************************************************
     * Save this table in a file.
     */
    public void save ()
    {
        try {
            ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (DIR + name + EXT));
            oos.writeObject (this);
            oos.close ();
        } catch (IOException ex) {
            out.println ("save: IO Exception");
            ex.printStackTrace ();
        } // try
    } // save

    /**
     * Pack the given tuple into a byte array.
     *
     * @param tup   the tuple to be packed
     * @return      the tuple now packed
     */
    public byte[] pack (Comparable[] tup) {
        byte[] b = null;
        int i = 0;
        int tupleSize = 0;

        for (int j = 0; j < domain.length; j++) {
            ByteBuffer buffer;
            switch (domain[j].getName()) {
                case "java.lang.Integer":
                    buffer = ByteBuffer.allocate(4);
                    tupleSize += 4;
                    buffer.putInt((Integer) tup[j]);
                    b = buffer.array();
                    break;
                case "java.lang.String":
                    tupleSize += ((String) tup[j]).getBytes().length;
                    b = ((String) tup[j]).getBytes();
                    break;
                case "java.lang.Short":
                    buffer = ByteBuffer.allocate(2);
                    tupleSize += 2;
                    buffer.putShort((Short) tup[j]);
                    b = buffer.array();
                    break;
                case "java.lang.Float":
                    buffer = ByteBuffer.allocate(4);
                    tupleSize += 4;
                    buffer.putFloat((Float) tup[j]);
                    b = buffer.array();
                    break;
                case "java.lang.Long":
                    buffer = ByteBuffer.allocate(8);
                    tupleSize += 8;
                    buffer.putLong((Long) tup[j]);
                    b = buffer.array();
                    break;
            }

            if (b == null) {
                out.println("Empty byte array");
                return null;
            }

            byte[] record = new byte[tupleSize];
            for (int k = 0; k < b.length; k++) {
                record[i++] = b[k];

                return record;
            }
        }

        return null;
    }

    /**
     * Unpacks a given byte record into a tuple.
     *
     * @param record    the byte record to be turned into a tuple
     * @return the unpacked byte array
     */
    public Comparable[] unpack(byte[] record) {
        Comparable[] tuple = new Comparable[domain.length];
        int offset = 0;

        for (int i = 0; i < domain.length; i++) {
            switch (domain[i].getName()) {
                case "java.lang.Integer":
                    byte[] intBytes = new byte[4];
                    for (int j = 0; j < 4; j++)
                        intBytes[j] = record[offset + j];
                    ByteBuffer intBuf = ByteBuffer.wrap(intBytes);
                    tuple[i] = intBuf.getInt();
                    offset += 4;
                    break;
                case "java.lang.String":
                    ByteBuffer stringBuf = ByteBuffer.wrap(record);
                    int size = stringBuf.getInt(offset + 60);
                    byte[] string = new byte[size];
                    for (int j = 0; j < size; j++)
                        string[j] = record[offset + j];
                    String newString = new String(string);
                    tuple[i] = newString;
                    offset += 64;
                    break;
                case "java.lang.Short":
                    byte[] shortBytes = new byte[2];
                    for (int j = 0; j < 2; j++)
                        shortBytes[j] = record[offset + j];
                    ByteBuffer shortBuf = ByteBuffer.wrap(shortBytes);
                    tuple[i] = shortBuf.getShort();
                    offset += 2;
                    break;
                case "java.lang.Float":
                    byte[] floatBytes = new byte[4];
                    for (int j = 0; j < 4; j++)
                        floatBytes[j] = record[offset + j];
                    ByteBuffer floatBuf = ByteBuffer.wrap(floatBytes);
                    tuple[i] = floatBuf.getFloat();
                    offset += 4;
                    break;
                case "java.lang.Double":
                    byte[] doubleBytes = new byte[8];
                    for (int j = 0; j < 8; j++)
                        doubleBytes[j] = record[offset + j];
                    ByteBuffer doubleBuf = ByteBuffer.wrap(doubleBytes);
                    tuple[i] = doubleBuf.getDouble();
                    offset += 8;
                    break;
                case "java.lang.Long":
                    byte[] longBytes = new byte[8];
                    for (int j = 0; j < 8; j++)
                        longBytes[j] = record[offset + j];
                    ByteBuffer longBuf = ByteBuffer.wrap(longBytes);
                    tuple[i] = longBuf.getLong();
                    offset += 8;
                    break;
            }
        }

        return tuple;
    }

    //----------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param table2  the rhs table
     * @return  whether the two tables are compatible
     */
    private boolean compatible (Table table2)
    {
        if (domain.length != table2.domain.length) {
            out.println ("compatible ERROR: table have different arity");
            return false;
        } // if
        for (int j = 0; j < domain.length; j++) {
            if (domain [j] != table2.domain [j]) {
                out.println ("compatible ERROR: tables disagree on domain " + j);
                return false;
            } // if
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Match the column and attribute names to determine the domains.
     *
     * @param column  the array of column names
     * @return  an array of column index positions
     */
    private int [] match (String [] column)
    {
        int [] colPos = new int [column.length];

        for (int j = 0; j < column.length; j++) {
            boolean matched = false;
            for (int k = 0; k < attribute.length; k++) {
                if (column [j].equals (attribute [k])) {
                    matched = true;
                    colPos [j] = k;
                } // for
            } // for
            if ( ! matched) {
                out.println ("match: domain not found for " + column [j]);
            } // if
        } // for

        return colPos;
    } // match

    /************************************************************************************
     * Extract the attributes specified by the column array from tuple t.
     *
     * @param t       the tuple to extract from
     * @param column  the array of column names
     * @return  a smaller tuple extracted from tuple t
     */
    private Comparable [] extract (Comparable [] t, String [] column)
    {
        Comparable [] tup = new Comparable [column.length];
        int [] colPos = match (column);
        for (int j = 0; j < column.length; j++) tup [j] = t [colPos [j]];
        return tup;
    } // extract

    /************************************************************************************
     * Check the size of the tuple (number of elements in list) as well as the type of
     * each value to ensure it is from the right domain.
     *
     * @param t  the tuple as a list of attribute values
     * @return  whether the tuple has the right size and values that comply
     *          with the given domains
     */
    private boolean typeCheck (Comparable [] t)
    {
        if (t.length != attribute.length)
            return false;

        for (int i = 0; i < t.length; i++) {
            if (t[i].getClass() != domain[i])
                return false;
        }

        return true;
    } // typeCheck

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  an array of Java classes
     */
    private static Class [] findClass (String [] className)
    {
        Class [] classArray = new Class [className.length];

        for (int i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param colPos the column positions to extract.
     * @param group  where to extract from
     * @return  the extracted domains
     */
    private Class [] extractDom (int [] colPos, Class [] group)
    {
        Class [] obj = new Class [colPos.length];

        for (int j = 0; j < colPos.length; j++) {
            obj [j] = group [colPos [j]];
        } // for

        return obj;
    } // extractDom
} // Table class



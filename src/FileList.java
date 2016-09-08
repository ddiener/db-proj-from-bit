
/*******************************************************************************
 * @file  FileList.java
 *
 * @author   John Miller
 */

import static java.lang.System.out;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;

/*******************************************************************************
 * This class allows data tuples/tuples (e.g., those making up a relational table)
 * to be stored in a random access file.  This implementation requires that each
 * tuple be packed into a fixed length byte array.
 */
public class FileList
       extends AbstractList <Comparable []>
       implements List <Comparable []>, RandomAccess
{
    /** File extension for data files.
     */
    private static final String EXT = ".dat";

    /** The random access file that holds the tuples.
     */
    private RandomAccessFile file;

    /** The name of table.
     */
    private final String tableName;

    /** The number bytes required to store a "packed tuple"/record.
     */
    private int recordSize;

    /** Counter for the number of tuples in this list.
     */
    private int nRecords = 0;
    
    private int[] fieldSizes = null;

    /***************************************************************************
     * Construct a FileList.
     * @param _tableName   the name of the table
     */
    public FileList (String _tableName)
    {
        tableName  = _tableName;

        try {
            file = new RandomAccessFile (tableName + EXT, "rw");
        } catch (FileNotFoundException ex) {
            file = null;
            out.println ("FileList.constructor: unable to open - " + ex);
        } // try
    } // constructor

    /***************************************************************************
     * Add a new tuple into the file list by packing it into a record and writing
     * this record to the random access file.  Write the record at the
     * end-of-file.
     * @param tuple  the tuple to add
     * @return  whether the addition succeeded
     */
    public boolean add (Comparable [] tuple)
    {
    	// For first record, get the byte size of each field in the tuple
    	if (nRecords == 0)
    		fieldSizes = new int[tuple.length];
    	
    	// Pack the tuple into the byte array
    	byte[] record = null;
		try {
			record = pack(tuple);
		} catch (IOException e) {
			e.printStackTrace();
		}

        if (record.length != recordSize) {
            out.println ("FileList.add: wrong record size " + record.length + " Correct: " + recordSize);
            return false;
        } // if
        
        // Seek to the end of the file so you don't overwrite
    	try {
        	file.seek(nRecords * recordSize);
        } catch (IOException e) {
        	out.println("FileList.add: unable to seek - " + e);
        	return false;
        }
    	
    	// Then write the byte record to the file
    	try {
    		file.write(record);
    	} catch (IOException e) {
    		out.println("FileList.add: unable to write record to file - " + e);
    		return false;
    	}

        nRecords++; 
        return true;
    } // add
    
    /**
     * Get the total size in bytes of a tuple.
     * @param tuple the tuple to get the size of
     * @return the total size of the tuple in bytes
     */
    public static int getSize(Comparable[] tuple) {
    	int totalSize = 0;
    	for (int i = 0; i < tuple.length; i++) 
    		try {
    		totalSize += serialize(tuple[i]).length;
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	return totalSize;
    }
    
    /**
     * Pack the contents of a tuple in to a byte array. If this is the first record
     * for the table, add the size of each field to an array keeping track of all 
     * the field sizes.
     * @param tuple the tuple to pack into a byte array
     * @return the packed byte array
     * @throws IOException
     */
    public byte[] pack(Comparable[] tuple) throws IOException {
    	byte[] record = { };
    	byte[] serialized = null;
    	
    	for (int i = 0; i < tuple.length; i++) {
    		serialized = serialize(tuple[i]);
    		if (nRecords == 0) {
    			fieldSizes[i] = serialized.length;
    			recordSize += serialized.length;
    		}
    		record = concat(record, serialized);
    	}
    	return record;
    }

    /**
     * Concatenate two arrays 
     * @param first the starting array (first in the result)
     * @param second the array to attach to the end of the first array
     * @return an array of the elements from the first array followed by the elements of the second array
     */
    public static byte[] concat(byte[] first , byte[] second) {
    	byte[] result = Arrays.copyOf(first, first.length + second.length);
    	System.arraycopy(second, 0, result, first.length, second.length);
    	return result;
	}
    
    /***************************************************************************
     * Get the ith tuple by seeking to the correct file position and reading the
     * record.
     * @param i  the index of the tuple to get
     * @return  the ith tuple
     */
    public Comparable [] get (int i)
    {
        byte [] record = new byte [recordSize];

        // Seek to the correct spot in the record.
        // If each record is the same size, you can go to the correct spot right away.
        try {
        	file.seek(i * recordSize);
        } catch (IOException e) {
        	out.println("FileList.get: unable to seek - " + e);
        }
        
        // Read the file, putting the bytes into an array to later unpack
        try {
        	file.read(record);
        } catch (IOException e) {
        	out.println("FileList.get: unable to read file - " + e);
        }
        
        // unpack the byte array and return the tuple
        return unpack(record);
    } // get
    
    /**
     * Iterate through the array of field sizes. For each field, create a subarray of the 
     * byte array consisting of the number of bytes recorded in the fieldSizes array. 
     * Deserialize each of these and place in their respective spots in a Comparable array.
     * @param record the byte array to unpack into a Comparable array
     * @return an array of the unpacked and deserialized tuple fields
     */
    public Comparable[] unpack(byte[] record) {
    	Comparable [] tuple = new Comparable[fieldSizes.length];
    	byte[] subRec = null;
    	int runningTotal = 0;
    	
    	for (int i = 0; i < fieldSizes.length; i++) {
    		subRec = Arrays.copyOfRange(record, runningTotal, runningTotal + fieldSizes[i]);
    		try {
				tuple[i] = (Comparable) deserialize(subRec);
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
    		// Keep a running total of the field sizes so you can skip to the correct subarray location
    		runningTotal += fieldSizes[i];
    	}
    	return tuple;
    }
    
    /**
     * Convert an object to a byte array
     * @param obj the object to serialize
     * @return a byte array representing the object
     * @throws IOException
     */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutputStream oStream = new ObjectOutputStream(bStream);
        oStream.writeObject(obj);
        return bStream.toByteArray();
    }

    /**
     * Convert a byte array to the object it represents
     * @param bytes the byte array to convert to an object
     * @return the object converted from a byte array
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bStream = new ByteArrayInputStream(bytes);
        ObjectInputStream oStream = new ObjectInputStream(bStream);
        return oStream.readObject();
    }

    /***************************************************************************
     * Return the size of the file list in terms of the number of tuples/records.
     * @return  the number of tuples
     */
    public int size ()
    {
        return nRecords;
    } // size

    /***************************************************************************
     * Close the file.
     */
    public void close ()
    {
        try {
            file.close ();
        } catch (IOException ex) {
            out.println ("FileList.close: unable to close - " + ex);
        } // try
    } // close
} // FileList class


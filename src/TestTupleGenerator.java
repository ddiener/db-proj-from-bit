/*****************************************************************************************
 * @file  TestTupleGenerator.java
 *
 * @author   Sadiq Charaniya, John Miller
 */

import static java.lang.System.out;

import java.time.*;
import java.util.Scanner;

/*****************************************************************************************
 * This class tests the TupleGenerator on the Student Registration Database defined in the
 * Kifer, Bernstein and Lewis 2006 database textbook (see figure 3.6).  The primary keys
 * (see figure 3.6) and foreign keys (see example 3.2.2) are as given in the textbook.
 */
public class TestTupleGenerator
{
    /*************************************************************************************
     * The main method is the driver for TestGenerator.
     * @param args  the command-line arguments
     */
    public static void main (String [] args)
    {
        TupleGenerator test = new TupleGeneratorImpl ();

        test.addRelSchema ("Student",
                           "id name address status",
                           "Integer String String String",
                           "id",
                           null);
        
        test.addRelSchema ("Professor",
                           "id name deptId",
                           "Integer String String",
                           "id",
                           null);
        
        test.addRelSchema ("Course",
                           "crsCode deptId crsName descr",
                           "String String String String",
                           "crsCode",
                           null);
        
        test.addRelSchema ("Teaching",
                           "crsCode semester profId",
                           "String String Integer",
                           "crcCode semester",
                           new String [][] {{ "profId", "Professor", "id" },
                                            { "crsCode", "Course", "crsCode" }});
        
        test.addRelSchema ("Transcript",
                           "studId crsCode semester grade",
                           "Integer String String String",
                           "studId crsCode semester",
                           new String [][] {{ "studId", "Student", "id"},
                                            { "crsCode", "Course", "crsCode" },
                                            { "crsCode semester", "Teaching", "crsCode semester" }});

        String [] tables = { "Student", "Professor", "Course", "Teaching", "Transcript" };
        
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Enter amount of tuples to be tested: ");
        int userChoice = keyboard.nextInt();
        System.out.println();
        
        int tups [] = new int [] { userChoice, 1, 2, 5, 5000 };
        
        Comparable [][][] resultTest = test.generate (tups);
        
        // What index method is to be used; TreeMap as default
        String mapType = "TreeMap";
        
        Table student = new Table("Student", "id name address status", "Integer String String String", "id", mapType);
        for (int i = 0; i < resultTest[0].length; i++){
            //out.println("size of ref: "+(FileList.getSize(resultTest[0][i])));
            student.insert(resultTest[0][i]);
        }
        
        Table professor = new Table("Professor", "id name deptId", "Integer String String", "id", mapType );
        
        for (int i = 0; i < resultTest[1].length; i++)
            professor.insert(resultTest[1][i]);
        
        Table course = new Table("Course", "crsCode deptId crsName descr", "String String String String", "crsCode", mapType);
        
        for (int i = 0; i < resultTest[2].length; i++)
            course.insert(resultTest[2][i]);
        
        Table teaching = new Table("Teaching", "crsCode semester profId", "String String Integer", "crsCode semester", mapType);
        
        for (int i = 0; i < resultTest[3].length; i++)
            teaching.insert(resultTest[3][i]);
        
        Table transcript = new Table("Transcript", "studId crsCode semester grade", "Integer String String String", "studId crsCode semester", mapType);
        
        for (int i = 0; i < resultTest[4].length; i++){
            transcript.insert(resultTest[4][i]);
}
 
        
        
        
        // NUMBER OF TRIALS FOR EACH METHOD
        int iters = 5;
        
        // Duration array to hold each trial of each table
        Duration selectTrialTimes[][] = new Duration[4][iters];
        Instant start, finish;
        
        // Initialize and fill all tables with various indexing structures 
        mapType = "ExtHashMap";
    	Table studentExt = new Table("StudentExt", "id name address status", "Integer String String String", "id", mapType);
    	for (int j = 0; j < resultTest[0].length; j++)
            studentExt.insert(resultTest[0][j]);
    	
    	mapType = "BpTreeMap";
        Table studentBp = new Table("StudentBp", "id name address status", "Integer String String String", "id", mapType);
        for (int j = 0; j < resultTest[0].length; j++)
            studentBp.insert(resultTest[0][j]);
        
        mapType = "LinHashMap";
        Table studentLin = new Table("StudentLin", "id name address status", "Integer String String String", "id", mapType);
        for (int j = 0; j < resultTest[0].length; j++)
            studentLin.insert(resultTest[0][j]);

        // Loop to test all differently implemented tables on the select( predicate ) method
        for( int i = 0; i < iters; i ++ ) {
        	Comparable comp = student.getRandomValue("id");
        	
        	start = Instant.now();
        	Table tree_select = student.select(new KeyType( comp ));
            //tree_select.print();
        	finish = Instant.now();
        	selectTrialTimes[0][i] = Duration.between( start, finish );  
            
            start = Instant.now();
            Table ext_select = studentExt.select(new KeyType( comp ));
            //ext_select.print();
            finish = Instant.now();
            selectTrialTimes[3][i] = Duration.between( start, finish );  

            start = Instant.now();
        	Table bp_select = studentBp.select(new KeyType( comp ));
            //bp_select.print();
        	finish = Instant.now();
        	selectTrialTimes[1][i] = Duration.between( start, finish );  
            
            start = Instant.now();
        	Table lin_select = studentLin.select(new KeyType( comp ));
            //lin_select.print();
        	finish = Instant.now();
        	selectTrialTimes[2][i] = Duration.between( start, finish );  
        	
        }
        
        
        // Array values will be stored in for the ranged select
        Duration selectPartTwo[][] = new Duration[4][iters];
        
        // Tables to be used for the ranged select
        mapType = "TreeMap";
    	Table studentFirst = new Table("StudentFirst", "id name address status", "Integer String String String", "id", mapType);
    	for (int j = 0; j < resultTest[0].length; j++)
            studentFirst.insert(resultTest[0][j]);
    	mapType = "ExtHashMap";
    	Table studentSecond = new Table("StudentSecond", "id name address status", "Integer String String String", "id", mapType);
    	for (int j = 0; j < resultTest[0].length; j++)
            studentSecond.insert(resultTest[0][j]);
    	
    	mapType = "BpTreeMap";
        Table studentThird = new Table("StudentThird", "id name address status", "Integer String String String", "id", mapType);
        for (int j = 0; j < resultTest[0].length; j++)
            studentThird.insert(resultTest[0][j]);
        
        mapType = "LinHashMap";
        Table studentFourth = new Table("StudentFourth", "id name address status", "Integer String String String", "id", mapType);
        for (int j = 0; j < resultTest[0].length; j++)
            studentFourth.insert(resultTest[0][j]);
    	
    	
        for( int i = 0; i < iters; i++ ) {
        	
        	Comparable firstVal = studentFirst.getRandomValue( "id" );
        	Comparable secondVal = studentFirst.getRandomValue( "id" );
            if (firstVal > secondVal){
                Comparable temp = firstVal;
                firstVal = secondVal;
                secondVal = temp;
            }
        	
        	start = Instant.now();
        	Table tree_range = studentFirst.rangeSelect(new KeyType(firstVal), false, new KeyType( secondVal ), false);
            //tree_range.print();
        	finish = Instant.now();
        	selectPartTwo[0][i] = Duration.between(start, finish);
        	
        	start = Instant.now();
        	Table ext_range = studentSecond.rangeSelect(new KeyType(firstVal), false, new KeyType( secondVal ), false);
            //ext_range.print();
        	finish = Instant.now();
        	selectPartTwo[1][i] = Duration.between(start, finish);
        	
        	start = Instant.now();
        	Table bp_range = studentThird.rangeSelect(new KeyType(firstVal), false, new KeyType( secondVal ), false);
            //bp_range.print();
        	finish = Instant.now();
        	selectPartTwo[2][i] = Duration.between(start, finish);
        	
        	start = Instant.now();
        	Table lin_range = studentFourth.rangeSelect(new KeyType(firstVal), false, new KeyType( secondVal ), false);
           //lin_range.print();
        	finish = Instant.now();
        	selectPartTwo[3][i] = Duration.between(start, finish);
        	
        }
        
        
        Duration joinTrialTimes[][] = new Duration[4][iters];
        // This for loop is for the join call on the student table
        for ( int i = 0; i < iters; i++ ) {
        	start = Instant.now();
            Table result = student.join("id", "studId", transcript);
            //result.print();
        	finish = Instant.now();
        	joinTrialTimes[0][i] = Duration.between( start, finish); 
        	
        	start = Instant.now();
        	result = studentExt.join( "id", "studId", transcript);
            //result.print();
        	finish = Instant.now();
        	joinTrialTimes[3][i] = Duration.between( start, finish );
        
        	start = Instant.now();
        	result = studentLin.join( "id", "studId", transcript);
            //result.print();
        	finish = Instant.now();
        	joinTrialTimes[2][i] = Duration.between( start, finish );
        	
        	start = Instant.now();
        	result = studentBp.join( "id", "studId", transcript);
            //result.print();
        	finish = Instant.now();
        	joinTrialTimes[1][i] = Duration.between( start, finish );
        }
        
        
        
        
        
        
        
        // FINAL FOR LOOP THAT PRINTS ALL DURATIONS
        // ALL VALUES PRINTED AND AVERAGED
        
        System.out.println("NUMBER OF TUPLES = " + userChoice );
        System.out.println("select( keyVal ) on TreeMap in ms");
        double sum = 0.0;
        double temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = selectTrialTimes[0][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("select( keyVal ) on BpTreeMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = selectTrialTimes[1][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("select( keyVal ) on LinHashMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = selectTrialTimes[2][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("select( keyVal ) on ExtHashMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = selectTrialTimes[3][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        System.out.println("----------------------");
        System.out.println();
        
        System.out.println("select( keyVal1, keyVal2 ) on TreeMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = selectPartTwo[0][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("select( keyVal1, keyVal2 ) on BpTreeMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = selectPartTwo[1][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("select( keyVal1, keyVal2 ) on LinHashMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = selectPartTwo[2][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("select( keyVal1, keyVal2 ) on ExtHashMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = selectPartTwo[3][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        System.out.println("----------------------");
        System.out.println();
        System.out.println("join( id, studId, transcript ) on TreeMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = joinTrialTimes[0][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("join( id, studId, transcript ) on BpTreeMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = joinTrialTimes[1][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("join( id, studId, transcript ) on LinHashMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = joinTrialTimes[2][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        System.out.println("join( id, studId, transcript ) on ExtHashMap in ms");
        sum = 0.0;
        temp = 0.0;
        for( int i = 0; i < iters; i++ ) {
        	temp = joinTrialTimes[3][i].toMillis();
        	System.out.println("Trial #" + i + ": " + temp);
        	sum += temp;
        }
        
        System.out.println("Average: " + sum/iters);
        System.out.println();
        
        
    } // main

} // TestTupleGenerator
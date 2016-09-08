-- Before 0.1137 seconds
SELECT s.name
FROM student s
WHERE NOT EXISTS (
    SELECT c.crsCode
    FROM course c
    WHERE c.deptId = 'deptId175201' AND
        c.crsCode NOT IN (
            SELECT t.crsCode
            FROM transcript t
            WHERE s.id = t.studId
            )
    );
	
-- Added primary keys to all tables
-- Added index to student.name, course.deptId
	
-- After 0.0004 seconds
SELECT s.name
FROM student s
WHERE NOT EXISTS (
    SELECT c.crsCode
    FROM course c
    WHERE c.deptId = 'deptId175201' AND
        c.crsCode NOT IN (
            SELECT t.crsCode
            FROM transcript t
            WHERE s.id = t.studId
            )
    );
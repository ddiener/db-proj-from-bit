-- Before 0.0033 seconds
SELECT s.name
FROM student s
WHERE s.id > 5000 AND
	s.id <= 10000;
	
-- Added primary key on student.id

-- After 0.0003 seconds
SELECT s.name
FROM student s
WHERE s.id > 5000 AND
	s.id <= 10000;
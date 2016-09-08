-- Before 8.3386 seconds
SELECT s.name
FROM prof p, teaching te, transcript tr, student s
WHERE p.name = 'name111111' AND
	p.id = te.profId AND
    (te.crsCode = tr.crsCode AND
     te.semester = tr.semester) AND
	tr.studId = s.id;

-- Added primary keys to all tables
-- Added indexes on professor.name, student.name
	
-- After 0.0003 seconds
SELECT s.name
FROM prof p, teaching te, transcript tr, student s
WHERE p.name = 'name111111' AND
	p.id = te.profId AND
    (te.crsCode = tr.crsCode AND
     te.semester = tr.semester) AND
	tr.studId = s.id;

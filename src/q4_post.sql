SELECT student.name
FROM professor p
JOIN teaching t ON t.profId = p.id
JOIN transcript tr ON (t.csrCode = tr.crsCode AND t.semester = tr.semester)
JOIN student s ON tr.studId = s.id
WHERE p.name = 'name890218';
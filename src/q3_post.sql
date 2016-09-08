SELECT student.name
FROM transcript, student
WHERE transcript.studId = student.id AND transcript.crsCode = crsCode787234;
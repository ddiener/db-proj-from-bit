CREATE TABLE `course` (
 `crsCode` varchar(20),
 `deptId` varchar(20),
 `crsName` varchar(20),
 `descr` varchar(20)
);

CREATE TABLE `prof` (
 `id` int(11),
 `name` varchar(20),
 `deptId` varchar(20)
);

CREATE TABLE `student` (
 `id` int(11),
 `name` varchar(20),
 `address` varchar(20),
 `status` varchar(20)
);

CREATE TABLE `teaching` (
 `crsCode` varchar(20),
 `semester` varchar(20),
 `profId` int(11)
);

CREATE TABLE `transcript` (
 `studId` int(11),
 `crsCode` varchar(20),
 `semester` varchar(20),
 `grade` varchar(20)
);
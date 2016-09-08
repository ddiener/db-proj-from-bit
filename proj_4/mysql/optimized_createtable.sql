CREATE TABLE `course` (
 `crsCode` varchar(20) DEFAULT NULL,
 `deptId` varchar(20) DEFAULT NULL,
 `crsName` varchar(20) DEFAULT NULL,
 `descr` varchar(20) DEFAULT NULL
);

CREATE TABLE `prof` (
 `id` int(11) NOT NULL DEFAULT '0',
 `name` varchar(20) DEFAULT NULL,
 `deptId` varchar(20) DEFAULT NULL,
 PRIMARY KEY (`id`),
 KEY `name` (`name`)
);

CREATE TABLE `student` (
 `id` int(11) DEFAULT NULL,
 `name` varchar(20) DEFAULT NULL,
 `address` varchar(20) DEFAULT NULL,
 `status` varchar(20) DEFAULT NULL
);

CREATE TABLE `teaching` (
 `crsCode` varchar(20) NOT NULL DEFAULT '',
 `semester` varchar(20) NOT NULL DEFAULT '',
 `profId` int(11) DEFAULT NULL,
 PRIMARY KEY (`crsCode`,`semester`),
 KEY `profId` (`profId`)
);

CREATE TABLE `transcript` (
 `studId` int(11) DEFAULT NULL,
 `crsCode` varchar(20) DEFAULT NULL,
 `semester` varchar(20) DEFAULT NULL,
 `grade` varchar(20) DEFAULT NULL
);
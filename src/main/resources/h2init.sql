DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS projects;

CREATE TABLE users (
  id int NOT NULL AUTO_INCREMENT,
  username varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT '1',
  role varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY username (username)
);

CREATE TABLE projects (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  project_name varchar(255) DEFAULT NULL,
  duration varchar(255) DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);
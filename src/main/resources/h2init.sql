CREATE TABLE IF NOT EXISTS projects (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  project_name varchar(255) DEFAULT NULL,
  duration varchar(255) DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  sub_task_name varchar(255) DEFAULT NULL,
  time_spent int DEFAULT NULL,
  expected_time_in_total int DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS subtasks (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  project_name varchar(255) DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  duration varchar(255) DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  sub_task_name varchar(255) DEFAULT NULL,
  time_spent double DEFAULT NULL,
  time_to_spend double DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS tasks (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  project_name varchar(255) DEFAULT NULL,
  duration varchar(255) DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  time_spent double DEFAULT NULL,
  time_to_spend double DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users (
  id int NOT NULL AUTO_INCREMENT,
  username varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT '1',
  role varchar(255) NOT NULL DEFAULT 'User',
  PRIMARY KEY (id),
  UNIQUE KEY username (username)
);

CREATE TABLE IF NOT EXISTS resources_subtasks (
  id int NOT NULL AUTO_INCREMENT,
  resource_name varchar(255) DEFAULT NULL,
  sub_task_id int DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_subtask (sub_task_id),
  CONSTRAINT fk_subtask FOREIGN KEY (sub_task_id) REFERENCES subtasks (id)
);

CREATE TABLE IF NOT EXISTS resources_tasks (
  id int NOT NULL AUTO_INCREMENT,
  resource_name varchar(255) DEFAULT NULL,
  task_id int DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_task (task_id),
  CONSTRAINT fk_task FOREIGN KEY (task_id) REFERENCES tasks (id)
);

INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, expected_time_in_total)
VALUES ('1', 'Tester', '1', '2011-11-01', '2011-11-02', 'Omar', '1', '1');

INSERT INTO tasks (WBS, project_name, task_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, time_to_spend)
VALUES ('1.1', 'Tester', 'Test', '0.5', '2011-11-01', '2011-11-01', 'Omar', '0.5', '0.5');

INSERT INTO subtasks (WBS, project_name, task_name, sub_task_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, time_to_spend)
VALUES ('1.1.1', 'Tester', 'Test', 'Test2', '0.5', '2011-11-02', '2011-11-02', 'Omar', '0.5', '0.5');

INSERT INTO resources_subtasks (resource_name, sub_task_id)
VALUES ('Subtask resource', '1');

INSERT INTO resources_tasks (resource_name, task_id)
VALUES ('Task resource', '1');
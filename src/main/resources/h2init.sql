DROP TABLE IF EXISTS resources_subtasks;
DROP TABLE IF EXISTS resources_tasks;
DROP TABLE IF EXISTS time_spent_tasks;
DROP TABLE IF EXISTS time_spent_subtasks;
DROP TABLE IF EXISTS subtasks;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS users;

-- Create the projects table first
CREATE TABLE IF NOT EXISTS projects (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  project_name varchar(255) DEFAULT NULL,
  duration int DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  time_spent double DEFAULT '0',
  expected_time_in_total int DEFAULT NULL,
  status varchar(255) DEFAULT 'not started',
  PRIMARY KEY (id),
  UNIQUE KEY unique_project_name (project_name)
);

-- Create the tasks table, which references projects
CREATE TABLE IF NOT EXISTS tasks (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  project_name varchar(255) DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  duration int DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  time_spent double DEFAULT '0',
  time_to_spend double DEFAULT NULL,
  status varchar(255) DEFAULT 'not started',
  PRIMARY KEY (id),
  UNIQUE KEY unique_task_name (task_name, project_name),
  KEY fk_project_name (project_name),
  CONSTRAINT fk_project_name FOREIGN KEY (project_name) REFERENCES projects (project_name) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the subtasks table, which references tasks
CREATE TABLE IF NOT EXISTS subtasks (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  project_name varchar(255) DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  sub_task_name varchar(255) DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  duration int DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  time_spent double DEFAULT '0',
  time_to_spend double DEFAULT NULL,
  status varchar(255) DEFAULT 'not started',
  PRIMARY KEY (id),
  UNIQUE KEY sub_task_name (sub_task_name, task_name),
  KEY fk_task_name (task_name),
  KEY fk_project_name_idx (project_name),
  CONSTRAINT fk_project_name_sub FOREIGN KEY (project_name) REFERENCES projects (project_name) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_task_name FOREIGN KEY (task_name) REFERENCES tasks (task_name) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the resources_tasks table, which references tasks
CREATE TABLE IF NOT EXISTS resources_tasks (
  id int NOT NULL AUTO_INCREMENT,
  resource_name varchar(255) DEFAULT NULL,
  task_id int DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_task (task_id),
  CONSTRAINT fk_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the resources_subtasks table, which references subtasks
CREATE TABLE IF NOT EXISTS resources_subtasks (
  id int NOT NULL AUTO_INCREMENT,
  resource_name varchar(255) DEFAULT NULL,
  sub_task_id int DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_subtask (sub_task_id),
  CONSTRAINT fk_subtask FOREIGN KEY (sub_task_id) REFERENCES subtasks (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the time_spent_tasks table, which references tasks
CREATE TABLE IF NOT EXISTS time_spent_tasks (
  id int NOT NULL AUTO_INCREMENT,
  days_date date DEFAULT NULL,
  time_spent double DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_task_name_new (task_name),
  CONSTRAINT fk_task_name_new FOREIGN KEY (task_name) REFERENCES tasks (task_name) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the time_spent_subtasks table, which references subtasks
CREATE TABLE IF NOT EXISTS time_spent_subtasks (
  id int NOT NULL AUTO_INCREMENT,
  days_date date DEFAULT NULL,
  time_spent double DEFAULT NULL,
  sub_task_name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_sub_task_name_new (sub_task_name),
  CONSTRAINT fk_sub_task_name_new FOREIGN KEY (sub_task_name) REFERENCES subtasks (sub_task_name) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create the users table
CREATE TABLE IF NOT EXISTS users (
  id int NOT NULL AUTO_INCREMENT,
  username varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT '1',
  role varchar(255) NOT NULL DEFAULT 'User',
  last_login date DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY username (username)
);

-- Insert into projects first
INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, expected_time_in_total, status)
VALUES ('1', 'Tester', '1', '2011-11-01', '2011-11-02', 'Omar', '1', '1', 'delayed');

-- Insert into tasks, using the project_name that exists in projects
INSERT INTO tasks (WBS, project_name, task_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, time_to_spend, status)
VALUES ('1.1', 'Tester', 'Test', '1', '2011-11-01', '2011-11-02', 'Omar', '0.5', '0.5', 'delayed');

-- Insert into subtasks, using task_name that exists in tasks
INSERT INTO subtasks (WBS, project_name, task_name, sub_task_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, time_to_spend, status)
VALUES ('1.1.1', 'Tester', 'Test', 'Test2', '0', '2011-11-02', '2011-11-02', 'Omar', '0.5', '0.5', 'delayed');

-- Insert into resources_subtasks (sub_task_id must exist in subtasks)
INSERT INTO resources_subtasks (resource_name, sub_task_id)
VALUES ('Subtask resource', '1');

-- Insert into resources_tasks (task_id must exist in tasks)
INSERT INTO resources_tasks (resource_name, task_id)
VALUES ('Task resource', '1');

-- Insert into time_spent_tasks (task_name must exist in tasks)
INSERT INTO time_spent_tasks (days_date, time_spent, task_name)
VALUES ('2011-11-01', '0.5', 'Test');

-- Insert into time_spent_subtasks (sub_task_name must exist in subtasks)
INSERT INTO time_spent_subtasks (days_date, time_spent, sub_task_name)
VALUES ('2011-11-02', '0.5', 'Test2');
CREATE TABLE IF NOT EXISTS projects (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  project_name varchar(255) DEFAULT NULL,
  duration int DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  time_spent double DEFAULT '0',
  expected_time_in_total double DEFAULT NULL,
  status varchar(255) DEFAULT 'not started',
  PRIMARY KEY (id),
  UNIQUE KEY unique_project_name (project_name)
);

CREATE TABLE IF NOT EXISTS projects_change_log (
  log_id int NOT NULL AUTO_INCREMENT,
  project_id int NOT NULL,
  action enum('insert','update','delete') NOT NULL,
  action_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  old_WBS varchar(255) DEFAULT NULL,
  old_project_name varchar(255) DEFAULT NULL,
  old_duration int DEFAULT NULL,
  old_planned_start_date date DEFAULT NULL,
  old_planned_finish_date date DEFAULT NULL,
  old_assigned varchar(255) DEFAULT NULL,
  old_time_spent double DEFAULT NULL,
  old_expected_time_in_total double DEFAULT NULL,
  old_status varchar(255) DEFAULT NULL,
  new_WBS varchar(255) DEFAULT NULL,
  new_project_name varchar(255) DEFAULT NULL,
  new_duration int DEFAULT NULL,
  new_planned_start_date date DEFAULT NULL,
  new_planned_finish_date date DEFAULT NULL,
  new_assigned varchar(255) DEFAULT NULL,
  new_time_spent double DEFAULT NULL,
  new_expected_time_in_total double DEFAULT NULL,
  new_status varchar(255) DEFAULT NULL,
  new_project_id int DEFAULT NULL,
  PRIMARY KEY (log_id),
  KEY project_id (project_id),
  CONSTRAINT projects_change_log_ibfk_1 FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
);

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
  UNIQUE KEY unique_task_name (task_name,project_name),
  KEY fk_project_name (project_name),
  CONSTRAINT fk_project_name FOREIGN KEY (project_name) REFERENCES projects (project_name) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS tasks_change_log (
  log_id int NOT NULL AUTO_INCREMENT,
  task_id int NOT NULL,
  action enum('insert','update','delete') NOT NULL,
  action_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  old_WBS varchar(255) DEFAULT NULL,
  old_project_name varchar(255) DEFAULT NULL,
  old_task_name varchar(255) DEFAULT NULL,
  old_assigned varchar(255) DEFAULT NULL,
  old_duration int DEFAULT NULL,
  old_planned_start_date date DEFAULT NULL,
  old_planned_finish_date date DEFAULT NULL,
  old_time_spent double DEFAULT NULL,
  old_time_to_spend double DEFAULT NULL,
  old_status varchar(255) DEFAULT NULL,
new_WBS varchar(255) DEFAULT NULL,
  new_project_name varchar(255) DEFAULT NULL,
  new_task_name varchar(255) DEFAULT NULL,
  new_assigned varchar(255) DEFAULT NULL,
  new_duration int DEFAULT NULL,
  new_planned_start_date date DEFAULT NULL,
  new_planned_finish_date date DEFAULT NULL,
  new_time_spent double DEFAULT NULL,
  new_time_to_spend double DEFAULT NULL,
  new_status varchar(255) DEFAULT NULL,
  new_task_id int DEFAULT NULL,
  PRIMARY KEY (log_id),
  KEY task_id (task_id),
  CONSTRAINT tasks_change_log_ibfk_1 FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS resources_tasks (
  id int NOT NULL AUTO_INCREMENT,
  resource_name varchar(255) DEFAULT NULL,
  task_id int DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_task (task_id),
  CONSTRAINT fk_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS time_spent_tasks (
  id int NOT NULL AUTO_INCREMENT,
  days_date date DEFAULT NULL,
  time_spent double DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_task_name_new (task_name),
  CONSTRAINT fk_task_name_new FOREIGN KEY (task_name) REFERENCES tasks (task_name) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS subtasks (
  id int NOT NULL AUTO_INCREMENT,
  WBS varchar(255) DEFAULT NULL,
  task_name varchar(255) DEFAULT NULL,
  sub_task_name varchar(255) DEFAULT NULL,
  assigned varchar(255) DEFAULT NULL,
  duration int DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_finish_date date DEFAULT NULL,
  time_spent double DEFAULT '0',
  time_to_spend double DEFAULT NULL,
  status varchar(255) DEFAULT 'not started',
  project_name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY sub_task_name (sub_task_name,task_name),
  KEY fk_task_name (task_name),
  KEY fk_project_name_idx (project_name),
  CONSTRAINT fk_project_name_sub FOREIGN KEY (project_name) REFERENCES projects (project_name) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_task_name FOREIGN KEY (task_name) REFERENCES tasks (task_name) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS subtasks_change_log (
  log_id int NOT NULL AUTO_INCREMENT,
  subtask_id int NOT NULL,
  action enum('insert','update','delete') NOT NULL,
  action_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  old_WBS varchar(255) DEFAULT NULL,
  old_task_name varchar(255) DEFAULT NULL,
  old_sub_task_name varchar(255) DEFAULT NULL,
  old_assigned varchar(255) DEFAULT NULL,
  old_duration int DEFAULT NULL,
  old_planned_start_date date DEFAULT NULL,
  old_planned_finish_date date DEFAULT NULL,
  old_time_spent double DEFAULT NULL,
  old_time_to_spend double DEFAULT NULL,
  old_status varchar(255) DEFAULT NULL,
  old_project_name varchar(255) DEFAULT NULL,
  new_WBS varchar(255) DEFAULT NULL,
  new_task_name varchar(255) DEFAULT NULL,
  new_sub_task_name varchar(255) DEFAULT NULL,
  new_assigned varchar(255) DEFAULT NULL,
  new_duration int DEFAULT NULL,
  new_planned_start_date date DEFAULT NULL,
  new_planned_finish_date date DEFAULT NULL,
  new_time_spent double DEFAULT NULL,
  new_time_to_spend double DEFAULT NULL,
  new_status varchar(255) DEFAULT NULL,
  new_project_name varchar(255) DEFAULT NULL,
  new_subtask_id int DEFAULT NULL,
  PRIMARY KEY (log_id),
  KEY subtask_id (subtask_id),
  CONSTRAINT subtasks_change_log_ibfk_1 FOREIGN KEY (subtask_id) REFERENCES subtasks (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS resources_subtasks (
  id int NOT NULL AUTO_INCREMENT,
  resource_name varchar(255) DEFAULT NULL,
  sub_task_id int DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_subtask (sub_task_id),
  CONSTRAINT fk_subtask FOREIGN KEY (sub_task_id) REFERENCES subtasks (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS time_spent_subtasks (
  id int NOT NULL AUTO_INCREMENT,
  days_date date DEFAULT NULL,
  time_spent double DEFAULT NULL,
  sub_task_name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_sub_task_name_new (sub_task_name),
  CONSTRAINT fk_sub_task_name_new FOREIGN KEY (sub_task_name) REFERENCES subtasks (sub_task_name) ON DELETE CASCADE ON UPDATE CASCADE
);

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

INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, expected_time_in_total, status)
VALUES ('1', 'Project', '7', '2024-12-14', '2024-12-21', 'Omar', '0.0', '10.0', 'delayed');

INSERT INTO tasks (WBS, project_name, task_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, time_to_spend, status)
VALUES ('1.1', 'Project', 'Project_Task', '7', '2024-12-14', '2024-12-21', 'Omar', '0.0', '5.0', 'delayed');

INSERT INTO subtasks (WBS, project_name, task_name, sub_task_name, duration, planned_start_date, planned_finish_date, assigned, time_spent, time_to_spend, status)
VALUES ('1.1.1', 'Project', 'Project_Task', 'Task_Subtask', '7', '2024-12-14', '2024-12-21', 'Omar', '0.0', '5.0', 'delayed');

INSERT INTO resources_subtasks (resource_name, sub_task_id)
VALUES ('Subtask resource', '1');

INSERT INTO resources_tasks (resource_name, task_id)
VALUES ('Task resource', '1');

INSERT INTO time_spent_tasks (days_date, time_spent, task_name)
VALUES ('2024-12-14', '1.0', 'Project_Task');

INSERT INTO time_spent_subtasks (days_date, time_spent, sub_task_name)
VALUES ('2024-12-14', '1.0', 'Task_Subtask');

INSERT INTO projects_change_log (project_id, action, old_WBS, old_project_name, old_duration, old_planned_start_date, old_planned_finish_date, old_assigned, old_time_spent, old_expected_time_in_total, old_status, new_WBS, new_project_name, new_duration, new_planned_start_date, new_planned_finish_date, new_assigned, new_time_spent, new_expected_time_in_total, new_status)
VALUES (
    LAST_INSERT_ID(),
    'insert',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    '1', 'Project', '7', '2024-12-14', '2024-12-21', 'Omar', '0.0', '10.0', 'delayed'
);

INSERT INTO tasks_change_log (task_id, action, old_WBS, old_project_name, old_task_name, old_assigned, old_duration, old_planned_start_date, old_planned_finish_date, old_time_spent, old_time_to_spend, old_status, new_WBS, new_project_name, new_task_name, new_assigned, new_duration, new_planned_start_date, new_planned_finish_date, new_time_spent, new_time_to_spend, new_status)
VALUES (
    LAST_INSERT_ID(),
    'insert',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    '1.1', 'Project', 'Project_Task', 'Omar', '7', '2024-12-14', '2024-12-21', '0.0', '5.0', 'delayed'
);

INSERT INTO subtasks_change_log (subtask_id, action, old_WBS, old_task_name, old_sub_task_name, old_assigned, old_duration, old_planned_start_date, old_planned_finish_date, old_time_spent, old_time_to_spend, old_status, old_project_name, new_WBS, new_task_name, new_sub_task_name, new_assigned, new_duration, new_planned_start_date, new_planned_finish_date, new_time_spent, new_time_to_spend, new_status, new_project_name)
VALUES (
    LAST_INSERT_ID(),
    'insert',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    '1.1.1', 'Project_Task', 'Task_Subtask', 'Omar', '7', '2024-12-14', '2024-12-21', '0.0', '5.0', 'delayed', 'Project'
);
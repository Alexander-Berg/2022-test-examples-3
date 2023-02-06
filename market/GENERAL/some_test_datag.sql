insert into employees (employee_login, employee_name, employee_department_id, employee_role)
values ('mstattest', 'Мстат Тестовицки', 1, 'dev');

insert into employees (employee_login, employee_name, employee_department_id, employee_role)
values ('khrzhstetskhee', 'Рстислав Кхржстецки', 1, 'dev');

-- delete from planner_roles where planner_role_login = 'mstattest';
-- insert into planner_roles (planner_role_login, planner_role, planner_role_created_by)
-- values ('mstattest', 'admin', 'oroboros');

delete from planner_roles where planner_role_login = 'mstattest';
insert into planner_roles (planner_role_login, planner_role, planner_role_created_by)
values ('mstattest', 'resowner', 'oroboros');


insert into departments (department_id, department_parent_id, department_name)
values (96, 1694, 'root');

insert into departments (department_id, department_parent_id, department_name)
values (1, 96, 'root_dep_1');

insert into departments (department_id, department_parent_id, department_name)
values (11, 1, 'dep_1_1');

insert into departments (department_id, department_parent_id, department_name)
values (12, 1, 'dep_1_2');

insert into departments (department_id, department_parent_id, department_name)
values (2, 96, 'root_dep_2');


update departments set department_head = null where department_head = 'mstattest';
update departments set department_head = 'mstattest' where department_id in (1);


-- commit

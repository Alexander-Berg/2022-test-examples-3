create table workflow_state_transitions (
  id bigint,
  workflow_id UUID,
  from_state int,
  to_state int,
  created_at timestamp,
  transition_order_num int,
  data_class_name varchar(1024),
  data bytea,
  primary key(id),
  foreign key(workflow_id) references workflows(id)
);

create sequence workflow_state_transitions_id_seq start with 1000;

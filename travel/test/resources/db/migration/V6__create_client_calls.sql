create table client_calls(
  id varchar,
  response_class_name varchar(512),
  response_data bytea,
  created_at timestamp,

  primary key (id)
);

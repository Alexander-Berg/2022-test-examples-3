create table entity_state_transitions (
  id bigint,
  workflow_id UUID,
  entity_id UUID,
  entity_type varchar(256),
  from_state int,
  previous_transition_at timestamp,
  to_state int,
  transition_at timestamp,
  primary key(id)
);

create sequence entity_state_transition_id_seq;

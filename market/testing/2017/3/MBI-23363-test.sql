--liquibase formatted sql

--changeset vbauer:MBI-23363_setup-CAN_CASH_ON_DELIVERY-for-10206336
insert into shops_web.param_value(param_value_id, param_type_id, entity_id, num_value)
  values (shops_web.s_param_value.nextval, 102, 10206336, 1);

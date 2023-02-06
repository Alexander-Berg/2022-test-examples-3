--liquibase formatted sql

--changeset vlepihin:MBI-16199_add_AGENCY_CLIENT_WHITE_LIST_FOR_TESTING

insert into shops_web.param_value(param_value_id, param_type_id, entity_id, num, num_value)
values (shops_web.s_param_value.nextval, 82, 322057, 2, 380594537);
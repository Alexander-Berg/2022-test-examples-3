delete from shops_web.term where dictionary_id = 14 and code = 48
/
insert into shops_web.term (select shops_web.s_term.nextval, 14, 48, 'Название магазина' from dual)
/
update shops_web.param_type set PARAM_NAME = 'Название магазина' where PARAM_TYPE_ID = 48
/
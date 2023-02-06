-- Набор данных для интеграционного тестирования
-- по идее надо настроить liquibase так, чтобы он раскатывал это только в тестинг,
-- но у меня возникли сомнения с тем, как у нас сейчас это работает и я побоялся всё сломать.
-- Поэтому оставил их просто в отельном файле

insert into loyalty_promo (id, promo_id, promo_name, bind_only_once, action_once_restriction_type, promo_group_id) values (1, 10430, 'test_promo', false, 'CHECK_USER', null);
insert into loyalty_promo (id, promo_id, promo_name, bind_only_once, action_once_restriction_type, promo_group_id) values (2, 10431, 'test_promo_2', false, '', null);


insert into market_user_ids (id, user_id, id_type, glue_id) values (1, 4028510170, 'uid', 1);
insert into market_user_ids (id, user_id, id_type, glue_id) values (2, 4028510172, 'uid', 1);
insert into market_user_ids (id, user_id, id_type, glue_id) values (3, 4029431892, 'uid', 2);
insert into market_user_ids (id, user_id, id_type, glue_id) values (4, 4029432526, 'uid', 2);

insert into coin_history (id, coin_id, promo_id, uid, status) values (1, 1, 10430, 4028510170, 'USED');
insert into coin_history (id, coin_id, promo_id, uid, status) values (2, 2, 10430, 4029431892, 'USED');
insert into coin_history (id, coin_id, promo_id, uid, status) values (3, 3, 10431, 4029431892, 'USED');

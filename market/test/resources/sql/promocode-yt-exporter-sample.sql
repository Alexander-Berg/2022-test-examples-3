INSERT INTO account (id, type, balance, version, matter, budget_threshold, can_be_restored) VALUES
(305375, 'ACTIVE', 1000000, 0, 'MONEY', 5000.00, true),
(305377, 'ACTIVE', 11900, 0, 'PIECE', null, false),
(305376, 'PASSIVE', 0, 0, 'MONEY', null, false),
(305378, 'PASSIVE', 0, 0, 'PIECE', null, false);

INSERT INTO coin_props (id, type, nominal, promo_id, days_to_expire, to_end_of_promo, coin_description_id, expiration_policy, expiration_policy_param) VALUES
(8415283, 'FIXED', 100.00, null, null, true, null, 'TO_END_OF_PROMO', null),
(8415284, 'FIXED', 100.00, null, null, true, null, 'TO_END_OF_PROMO', null),
(8415285, 'FIXED', 100.00, null, null, true, null, 'TO_END_OF_PROMO', null),
(8415286, 'FIXED', 100.00, null, null, true, null, 'TO_END_OF_PROMO', null),
(8415287, 'FIXED', 100.00, null, null, true, null, 'TO_END_OF_PROMO', null),
(8415288, 'FIXED', 100.00, null, null, true, null, 'TO_END_OF_PROMO', null);

INSERT INTO promo (id, name, description, creation_time, budget_acc_id, spending_acc_id, status, modification_time, end_date, emission_budget_acc_id, emission_spending_acc_id, promo_type, start_date, promo_key, version, promo_subtype, platform, start_publish_date, coin_props_id, ticket_number, ticket_text, sorry_mechanic, action_code, action_code_internal, cashback_props_id) VALUES
(95275, 'Тестовая акция с промокодом для разработки', null, '2020-12-07 14:09:13.518000', 305375, 305376, 'ACTIVE', '2020-12-07 17:16:39.942000', '2029-12-28 00:00:01.000000', 305377, 305378, 'SMART_SHOPPING', '2020-12-07 00:00:00.000000', '9v3A65btoC3-P-hXPbtUZw', -1, 'PROMOCODE', 'BLUE', null, 8415283, null, null, false, 'DEVTEST', null, null),
(95276, 'Тестовая акция с промокодом на первый заказ для разработки', null, '2020-12-07 14:09:13.518000', 305375, 305376, 'ACTIVE', '2020-12-07 17:16:39.942000', '2029-12-28 00:00:00.000000', 305377, 305378, 'SMART_SHOPPING', '2020-12-07 00:00:00.000000', '9v3A65btoC3-P-hXPbtUZw', -1, 'PROMOCODE', 'BLUE', null, 8415284, null, null, false, 'DEVTEST_FIRST_ORDER', null, null),
(95277, 'Тестовая акция Не выгружать в индексатор', null, '2020-12-07 14:09:13.518000', 305375, 305376, 'ACTIVE', '2020-12-07 17:16:39.942000', now(), 305377, 305378, 'SMART_SHOPPING', '2020-12-07 00:00:00.000000', '9v3A65btoC3-P-hXPbtUZw', -1, 'PROMOCODE', 'BLUE', null, 8415285, null, null, false, 'DEVTEST_DO_NOT_UPLOAD', null, null),
(95278, 'Тестовая акция Не выгружать при отсутствии ограничений', null, '2020-12-07 14:09:13.518000', 305375, 305376, 'ACTIVE', '2020-12-07 17:16:39.942000', now(), 305377, 305378, 'SMART_SHOPPING', '2020-12-07 00:00:00.000000', '9v3A65btoC3-P-hXPbtUZw', -1, 'PROMOCODE', 'BLUE', null, 8415285, null, null, false, 'NO_RULES_TEST', null, null),
(95279, 'Тестовая акция Не выгрузится в индексатор из-за валидации (должны быть заполнены ADDITIONAL_CONDITIONS_TEXT и MIN_ORDER_TOTAL)', null, '2020-12-07 14:09:13.518000', 305375, 305376, 'ACTIVE', '2020-12-07 17:16:39.942000', now(), 305377, 305378, 'SMART_SHOPPING', '2020-12-07 00:00:00.000000', '9v3A65btoC3-P-hXPbtUZw', -1, 'PROMOCODE', 'BLUE', null, 8415287, null, null, false, 'DEVTEST_DO_NOT_UPLOAD', null, null),
(95280, 'Тестовая акция Не выгрузится в индексатор из-за валидации (уже есть такой action_code на те же даты DUPLICATED_PROMOCODE_TO_YT)', null, '2020-12-07 14:09:13.518000', 305375, 305376, 'ACTIVE', '2020-12-07 17:16:39.942000', '2030-12-28 00:00:00.000000', 305377, 305378, 'SMART_SHOPPING', '2029-12-28 00:00:00.000000', '9v3A65btoC3-P-hXPbtUZw', -1, 'PROMOCODE', 'BLUE', null, 8415288, null, null, false, 'DEVTEST', null, null);

UPDATE coin_props SET promo_id = 95275 WHERE id = 8415283;
UPDATE coin_props SET promo_id = 95276 WHERE id = 8415284;
UPDATE coin_props SET promo_id = 95277 WHERE id = 8415285;
UPDATE coin_props SET promo_id = 95278 WHERE id = 8415286;
UPDATE coin_props SET promo_id = 95279 WHERE id = 8415287;
UPDATE coin_props SET promo_id = 95280 WHERE id = 8415288;

INSERT INTO promo_params (id, promo_id, name, value) VALUES
(163272, 95275, 'PROMO_OFFER_AND_ACCEPTANCE', 'https://wiki.yandex-team.ru/market/pokupka/projects/users-2019/bluevendors/promocodemarket40/dev/'),
(163271, 95275, 'MARKET_DEPARTMENT', 'PRODUCT'),
(163270, 95275, 'BUDGET_SOURCES', 'VENDOR'),
(163269, 95275, 'EMISSION_FOLDING', 'false'),
(163268, 95275, 'COIN_CREATION_REASON', 'FOR_USER_ACTION'),
(163267, 95275, 'COUPON_EMISSION_DATE_TO', '2020-12-28 00:00:00'),
-- (163266, 95275, 'LANDING_URL', 'https://pokupki.market.yandex.ru/special/black-friday'),
(163265, 95275, 'NOMINAL_STRATEGY', '{"type":"DefaultStrategy"}'),
(163264, 95275, 'BIND_ONLY_ONCE', 'false'),
(163263, 95275, 'GENERATED_PROMOCODE', 'false'),
(163262, 95275, 'COUPON_EMISSION_DATE_FROM', '2020-12-07 00:00:00'),
(163273, 95275, 'NO_LANDING_URL', 'true'),
(163261, 95275, 'ADDITIONAL_CONDITIONS_TEXT', 'Текст дополнительных условий акции-should');

INSERT INTO promo_params (id, promo_id, name, value) VALUES
(163285, 95276, 'PROMO_OFFER_AND_ACCEPTANCE', 'https://wiki.yandex-team.ru/market/pokupka/projects/users-2019/bluevendors/promocodemarket40/dev/'),
(163274, 95276, 'MARKET_DEPARTMENT', 'PRODUCT'),
(163275, 95276, 'BUDGET_SOURCES', 'VENDOR'),
(163276, 95276, 'EMISSION_FOLDING', 'false'),
(163277, 95276, 'COIN_CREATION_REASON', 'FOR_USER_ACTION'),
(163278, 95276, 'COUPON_EMISSION_DATE_TO', '2020-12-28 00:00:00'),
(163279, 95276, 'LANDING_URL', 'https://pokupki.market.yandex.ru/special/black-friday'),
(163280, 95276, 'NOMINAL_STRATEGY', '{"type":"DefaultStrategy"}'),
(163281, 95276, 'BIND_ONLY_ONCE', 'false'),
(163282, 95276, 'GENERATED_PROMOCODE', 'false'),
(163283, 95276, 'COUPON_EMISSION_DATE_FROM', '2020-12-07 00:00:00');

INSERT INTO promo_params (id, promo_id, name, value) VALUES
(163295, 95277, 'PROMO_OFFER_AND_ACCEPTANCE', 'https://wiki.yandex-team.ru/market/pokupka/projects/users-2019/bluevendors/promocodemarket40/dev/'),
(163294, 95277, 'MARKET_DEPARTMENT', 'PRODUCT'),
(163293, 95277, 'BUDGET_SOURCES', 'VENDOR'),
(163292, 95277, 'EMISSION_FOLDING', 'false'),
(163291, 95277, 'COIN_CREATION_REASON', 'FOR_USER_ACTION'),
(163290, 95277, 'COUPON_EMISSION_DATE_TO', '2020-12-28 00:00:00'),
(163289, 95277, 'NOMINAL_STRATEGY', '{"type":"DefaultStrategy"}'),
(163288, 95277, 'BIND_ONLY_ONCE', 'false'),
(163287, 95277, 'GENERATED_PROMOCODE', 'false'),
(163286, 95277, 'COUPON_EMISSION_DATE_FROM', '2020-12-07 00:00:00'),
(163296, 95277, 'NO_LANDING_URL', 'true'),
(163284, 95277, 'DO_NOT_UPLOAD_TO_IDX', 'true');

INSERT INTO promo_params (id, promo_id, name, value) VALUES
(163297, 95279, 'PROMO_OFFER_AND_ACCEPTANCE', 'https://wiki.yandex-team.ru/market/pokupka/projects/users-2019/bluevendors/promocodemarket40/dev/'),
(163298, 95279, 'MARKET_DEPARTMENT', 'PRODUCT'),
(163299, 95279, 'BUDGET_SOURCES', 'VENDOR'),
(163300, 95279, 'EMISSION_FOLDING', 'false'),
(163301, 95279, 'COIN_CREATION_REASON', 'FOR_USER_ACTION'),
(163302, 95279, 'COUPON_EMISSION_DATE_TO', '2020-12-28 00:00:00'),
(163303, 95279, 'LANDING_URL', 'https://pokupki.market.yandex.ru/special/black-friday'),
(163304, 95279, 'NOMINAL_STRATEGY', '{"type":"DefaultStrategy"}'),
(163305, 95279, 'BIND_ONLY_ONCE', 'false'),
(163306, 95279, 'GENERATED_PROMOCODE', 'false'),
(163307, 95279, 'COUPON_EMISSION_DATE_FROM', '2020-12-07 00:00:00');

INSERT INTO coin_rule (id, coin_props_id, rule_type, bean) VALUES
(16649756, 8415283, 2, 'mskuFilterRule'),
(16649757, 8415283, 3, 'minOrderTotalCuttingRule'),
(16649755, 8415283, 2, 'excludedOffersFilterRule'),
(16649754, 8415284, 3, 'firstOrderCuttingRule'),
(16649758, 8415284, 2, 'mskuFilterRule'),
(16649759, 8415287, 2, 'mskuFilterRule'),
(16649760, 8415287, 3, 'minOrderTotalCuttingRule'),
(16649761, 8415287, 2, 'excludedOffersFilterRule');

INSERT INTO coin_rule_params (id, coin_rule_id, name, value) VALUES
(10000, 16649757, 'MIN_ORDER_TOTAL', '121'),
(10001, 16649760, 'MIN_ORDER_TOTAL', '2671');

INSERT INTO promocode (id, code, promo_id, coin_id, coupon_id, creation_time) VALUES
(1, 'DEVTEST', 95275, null, null, '2020-12-07 14:09:14.222477'),
(2, 'DEVTEST_FIRST_ORDER', 95276, null, null, '2020-12-07 14:09:14.222477'),
(3, 'DEVTEST_DO_NOT_UPLOAD', 95277, null, null, '2020-12-07 14:09:14.222477'),
(4, 'NO_RULES_TEST', 95278, null, null, '2020-12-07 14:09:14.222477');

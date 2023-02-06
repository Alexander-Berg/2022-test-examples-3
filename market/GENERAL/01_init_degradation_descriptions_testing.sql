insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'mstat-antifraud-orders.tst.vs.market.yandex.net/antifraud/detect',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'mstat-antifraud-orders.tst.vs.market.yandex.net/antifraud/loyalty/detect',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'mstat-antifraud-orders.tst.vs.market.yandex.net/antifraud/loyalty/restrictions',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'mstat-antifraud-orders.tst.vs.market.yandex.net/antifraud/loyalty/restrictions/bonus',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'mstat-antifraud-orders.tst.vs.market.yandex.net/antifraud/loyalty/orders-count/v2',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'mstat-antifraud-orders.tst.vs.market.yandex.net/glue/nodes/fast',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'mstat-antifraud-orders.tst.vs.market.yandex.net/glue/nodes/accurate',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'market-loyalty.tst.vs.market.yandex.net/perk/status',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'market-loyalty.tst.vs.market.yandex.net/discount/spend/v3',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

insert into degradation_descriptions (name, config, updated_at, added_at) values
(
    'market-loyalty.tst.vs.market.yandex.net/discount/calc/v3',
    '{"updatePeriod":300,"degradationMode":[10, 30, 90]}' :: JSONB,
    NOW(),
    NOW()
) on conflict do nothing ;

-- Создаем дефолтный фулфилмент магазин для синего:
insert into shop_meta_data
(
    shop_id,
    campaign_id,
    client_id,
    sandbox_class,
    prod_class,
    prepay_type,
    ya_money_id,
    articles,
    inn,
    phone_number,
    commission,
    order_visibility
)
VALUES (431782, 21421814, 34859091, 2, 2, 1, null, null, 7704357909, 84959743543, null, null)
on conflict (shop_id) do nothing;

-- Создаем дефолтного саплаера для OrderItemProvider
insert into shop_meta_data
(
    shop_id,
    campaign_id,
    client_id,
    sandbox_class,
    prod_class,
    prepay_type,
    ya_money_id,
    articles,
    inn,
    phone_number,
    commission,
    order_visibility
)
VALUES (123, -1, -1, 2, 2, 2, null, null, null, null, null, null)
on conflict (shop_id) do nothing;


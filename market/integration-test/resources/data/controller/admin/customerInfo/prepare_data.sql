insert into partner (id, name, status, type, billing_client_id, rating)
values (1, 'Delivery service 1', 'active', 'DELIVERY', 123, 1),
       (2, 'Delivery service 2', 'active', 'DELIVERY', 123, 1);

insert into partner_customer_info (name, phones, track_order_site, track_code_source)
values ('PartnerOne', '{"+7-(912)-345-67-89"}', 'www.partner1-site.ru', 'ORDER_NO'),
       ('PartnerTwo', '{"+7-(912)-345-67-88"}', 'www.partner2-site.ru', 'DS_TRACK_CODE');

update partner
set partner_customer_info_id = 2
where id = 2;

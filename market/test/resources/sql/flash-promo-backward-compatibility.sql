INSERT INTO promo (id, promo_key, name, promo_type, promo_subtype, platform, creation_time, modification_time, status, start_date, end_date, shop_promo_id) VALUES
(1001, 'some_promo_key', 'some promo', 'BLUE_FLASH', 'BLUE_FLASH', 'BLUE', now(), now(), 'ACTIVE', now(), now() + interval '1 day', 'some promo');

INSERT INTO flash_promo (id, feed_id, shop_promo_id, promo_id, promo_key, status, start_time, end_time, creation_time, source, anaplan_id) VALUES
(1001, 123, 'some promo', 1001, 'some_promo_key', 'ACTIVE', now(), now() + interval '1 day', now(), 'FIRST_PARTY_PIPELINE', '#123');

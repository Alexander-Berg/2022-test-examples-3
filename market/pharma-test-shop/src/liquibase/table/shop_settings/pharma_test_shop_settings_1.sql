--changeset katejud:create_pharma_test_shop_settings
CREATE TABLE IF NOT EXISTS pharma_test_shop_settings
(
    shop_id   BIGINT NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL ,
    message VARCHAR(100) NOT NULL ,
    delivery_types VARCHAR(30) NOT NULL ,
    from_date_express DATE,
    to_date_express DATE,
    from_date_delivery DATE,
    to_date_delivery DATE,
    from_date_pickup DATE,
    to_date_pickup DATE,
    payment_method_express varchar(200),
    payment_method_delivery varchar(200),
    payment_method_pickup varchar(200),
    salesModel varchar(100),
    oauthToken varchar(400),
    oauthClientId varchar(400),
    campaignId varchar(400)

);

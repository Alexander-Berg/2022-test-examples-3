set mode MSSQLServer;

CREATE TABLE DPCalcPricesIN
(
    ID bigint IDENTITY(1,1) NOT NULL,
    MSKU bigint NOT NULL,
    PRICE money NOT NULL,
    MOD_TS datetime NOT NULL,
    EXPORT_TS datetime NOT NULL,
    IMPORT_TS datetime NULL,
    IMPORT_STATUS int NOT NULL,
    COMMENT nvarchar(max) NULL,
    PRICEGROUP nvarchar(30) NOT NULL,
    SSKU nvarchar(131) NOT NULL,
    WAREHOUSE_ID int NULL
);

-- в Аксапте это вьюха, а здесь мы просто заглушку ставим
CREATE TABLE RETAILPERIODICDISCOUNT
(
    msku bigint NOT NULL,
    ssku nvarchar(131) NOT NULL,
    Status nvarchar(30) NOT NULL,
    OfferPrice money NOT NULL,
    PriceGroupId nvarchar(30) NOT NULL,
    ValidFrom datetime NOT NULL,
    ValidTo datetime NOT NULL,
    OfferId nvarchar(20) NOT NULL
);

insert into RETAILPERIODICDISCOUNT(msku, ssku, Status, OfferPrice, PriceGroupId, ValidFrom, ValidTo, OfferId)
values
    -- не попадет в выборку, т. к. в прошлом
    (123, '123.abc', 'Включено', 110.0, 'РРЦ', '2022-03-30 00:00:00', '2022-03-30 00:00:00', 'Disc-000000001'),
    -- эти попадут в выборку
    (124, '124.abc', 'Включено', 111.0, 'РРЦ', '2022-03-30 00:00:00', '2122-03-30 00:00:00', 'Disc-000000001'),
    (125, '125.abc', 'Включено', 112.0, 'РРЦ-день', '2022-03-29 00:00:00', '2122-03-30 00:00:00', 'Disc-000000001'),
    -- дубль предыдущего, попадет в выборку именно он, т. к. у него ValidFrom начинается позже
    (125, '125.abc', 'Включено', 112.1, 'РРЦ-день', '2022-03-30 00:00:00', '2122-03-30 00:00:00', 'Disc-000000002'),
    -- попадет в выборку, т. к. ценовая группа другая
    (125, '125.abc', 'Включено', 112.2, 'РРЦ', '2022-03-29 00:00:00', '2122-03-30 00:00:00', 'Disc-000000002'),
    -- эти 2 строки попадут в выборку, т. к. ценовая группа разная
    (126, '126.abc', 'Включено', 113.1, 'РРЦ', '2022-03-30 00:00:00', '2122-03-30 00:00:00', 'Disc-000000002'),
    (126, '126.abc', 'Включено', 113.2, 'ФиксЦен', '2022-03-30 00:00:00', '2122-03-30 00:00:00', 'Disc-000000002'),
    -- попадет в выборку только строка с макс. offer_id - Disc-000000003
    (127, '127.abc', 'Включено', 114.1, 'ФиксЦен', '2022-03-30 00:00:00', '2122-03-30 00:00:00', 'Disc-000000002'),
    (127, '127.abc', 'Включено', 114.2, 'ФиксЦен', '2022-03-30 00:00:00', '2122-03-30 00:00:00', 'Disc-000000003'),
    -- промоакция, попадет в выборку
    (128, '128.abc', 'Включено', 115.0, 'Промоакция', '2022-03-30 00:00:00', '2122-03-30 00:00:00', 'Disc-000000004');


CREATE TABLE DPAssortment(
    msku bigint,
    ssku nvarchar(131),
    purch_price real,
    promo_purch_price real,
    available_for_business bit,
    vat nvarchar(30),
    auto_price_block bit
);

insert into DPAssortment(msku, ssku, purch_price, promo_purch_price, available_for_business,
                         vat, auto_price_block)
values
    (123, '123.abc', 6.2, 5, 1, 'VAT_10', 1),
    (124, '124.abc', 7, 6.2, 0, 'VAT_20', 0),
    (125, '125.abc', 7, 6.2, 0, 'NO_VAT', 0),
    (126, '126.abc', 7, 6.2, 0, 'VAT_20', 0),
    (117, '127.abc', 9, 7.1, 1, 'VAT_10', 1);

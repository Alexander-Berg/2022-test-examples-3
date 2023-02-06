create table if not exists stats_day
(
    id bigint unsigned auto_increment
        primary key,
    shop_id int not null,
    offer_id bigint unsigned not null,
    date date not null,
    clicks_count int not null,
    clicks_spending decimal(12,2) not null,
    orders_count int not null,
    orders_revenue decimal(12,2) not null,
    created_at datetime not null,
    updated_at datetime null,
    cpa_order_items int default 0 not null,
    cpa_spending decimal(12,2) default 0.00 not null,
    constraint date_offer_idx
        unique (offer_id, date),
    constraint FK_8DB2093B4D16C4DD
        foreign key (shop_id) references shop (id),
    constraint FK_8DB2093B53C674EE
        foreign key (offer_id) references shop_offer (id)
            on update cascade on delete cascade
);

create index IDX_8DB2093B4D16C4DD
    on stats_day (shop_id);

create index IDX_8DB2093B53C674EE
    on stats_day (offer_id);

create index date_idx
    on stats_day (date);

create index shop_date_idx
    on stats_day (date);



create table if not exists stats_shop
(
    id bigint unsigned auto_increment
        primary key,
    shop_id int not null,
    date date not null,
    total_offers int null,
    card_offers int null,
    no_card_offers int null,
    clicks_count int null,
    clicks_spending decimal(12,2) null,
    popular_offers longtext null comment '(DC2Type:json_array)',
    detached_card_offers int null,
    hide_offers int null,
    constraint shop_date_idx
        unique (shop_id, date),
    constraint FK_ED0624E24D16C4DD
        foreign key (shop_id) references shop (id)
            on delete cascade
);

create index IDX_ED0624E24D16C4DD
    on stats_shop (shop_id);



create table if not exists stats_month
(
    shop_id int not null,
    month date not null,
    clicks_count int default 0 not null,
    clicks_spending decimal(12,2) default 0.00 not null,
    orders_count int default 0 not null,
    orders_revenue decimal(12,2) default 0.00 not null,
    cpa_order_items int default 0 not null,
    cpa_spending decimal(12,2) default 0.00 not null,
    primary key (shop_id, month),
    constraint stats_month_ibfk_1
        foreign key (shop_id) references shop (id)
            on update cascade on delete cascade
);

create index month_idx
    on stats_month (month);


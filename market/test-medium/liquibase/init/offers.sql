create table shop_category
(
    id bigint unsigned auto_increment
        primary key,
    parent_id bigint unsigned null,
    tree_left int unsigned null,
    tree_right int unsigned null,
    ya_id bigint not null,
    ya_parent_id bigint null,
    feed_id int not null,
    name varchar(255) default '' not null,
    status enum('ACTIVE', 'DELETED') default 'ACTIVE' not null,
    update_status enum('OLD', 'NEW') default 'NEW' not null,
    created_at datetime null,
    updated_at datetime null,
    shop_id int not null,
    offer_count int unsigned null,
    tree_lvl smallint(5) unsigned default 0 not null,
    constraint ya_id_shop_id
        unique (ya_id, shop_id),
    constraint fk_category_shop_yml1
        foreign key (feed_id) references shop_feed (id)
            on update cascade on delete cascade,
    constraint fk_shop_category_shop1
        foreign key (shop_id) references shop (id),
    constraint fk_shop_category_shop_category1
        foreign key (parent_id) references shop_category (id)
);

create index feed_id_ya_id
    on shop_category (feed_id, ya_id);

create index fk_category_shop_yml1_idx
    on shop_category (feed_id);

create index fk_shop_category_shop1_idx
    on shop_category (shop_id);

create index fk_shop_category_shop_category1_idx
    on shop_category (parent_id);

create index tree_l_r
    on shop_category (tree_left, tree_right, shop_id);

create index tree_l_shop_id
    on shop_category (tree_left, shop_id);

create index tree_r_id
    on shop_category (tree_right, id);

create index tree_r_shop_id
    on shop_category (tree_right, shop_id);




create table if not exists shop_offer
(
    id bigint unsigned auto_increment
        primary key,
    ya_id varchar(255) null,
    vendor_code varchar(255) null,
    ya_category_id bigint not null,
    category_id bigint unsigned null,
    name varchar(512) default '' not null,
    price decimal(12,2) null,
	ya_model_id int null,
	last_ya_model_id int null,
	created_at datetime null,
	updated_at datetime null,
	status enum('ACTIVE', 'DELETED') default 'ACTIVE' not null,
	update_status enum('OLD', 'NEW') default 'NEW' not null,
	feed_id int not null,
	shop_id int not null,
	purchase_price decimal(12,2) null,
	vendor_id bigint unsigned null,
	in_stock_count int default 1 not null,
	url varchar(7000) null,
	shipping_cost decimal(12,2) null,
	type varchar(100) null,
	in_model_count int default 1 not null,
	market_category_id int unsigned null,
	min_price_offer_id bigint unsigned null,
	oldprice decimal(12,2) unsigned null,
	is_cpa tinyint(1) default 0 not null,
	is_hide_on_market tinyint(1) default 0 not null,
	strategy_id int unsigned null,
	is_hide_ttl_hours smallint(5) unsigned null,
	query varchar(512) null,
	is_cutprice tinyint(1) default 0 not null,
	constraint ya_id_shop_id2
		unique (ya_id, shop_id),
	constraint FK_EEC0DD6CF603EE735
		foreign key (vendor_id) references shop_vendor (id),
	constraint fk_shop_offer_shop_category5
		foreign key (category_id) references shop_category (id),
	constraint fk_shop_offer_shop_feed5
		foreign key (feed_id) references shop_feed (id),
	constraint fk_shop_offer_shop_id
		foreign key (shop_id) references shop (id)
			on update cascade on delete cascade
);

create index IDX_EEC0DD6CF603EE73
    on shop_offer (vendor_id);

create index fk_product_category1_idx
    on shop_offer (category_id);

create index fk_shop_offer_shop_feed1_idx
    on shop_offer (feed_id);

create index offer_name
    on shop_offer (name, feed_id);

create index shop_id_model_id_status_feed_id
    on shop_offer (shop_id, ya_model_id, status, feed_id);

create index shop_status_idx
    on shop_offer (shop_id, status);

create index shop_vendor_idx
    on shop_offer (shop_id, vendor_id, status);




create table if not exists broker_bid
(
    id bigint unsigned auto_increment
        primary key,
    offer_id bigint unsigned null,
    bid decimal(12,2) null,
    cbid decimal(12,2) null,
    updated_at datetime null,
    search_bid_1 decimal(12,2) null,
    search_bid_5 decimal(12,2) null,
    shop_id int null,
    strategy_type enum('MAIN', 'RESERVE') null,
    strategy_status enum('NO_STRATEGY', 'REACH_BOTH', 'REACH_CARD', 'REACH_SEARCH', 'REACH_NONE') default 'NO_STRATEGY' not null,
    min_bid decimal(12,2) null,
    min_cbid decimal(12,2) null,
    current_pos_all smallint unsigned null,
    current_pos_top smallint unsigned null,
    top_offers_count smallint unsigned null,
    min_fee decimal(6,2) null,
    fee decimal(6,2) null,
    ms_model_count smallint unsigned null,
    ms_bid_1 decimal(6,2) null,
    ms_bid_2 decimal(6,2) null,
    ms_bid_3 decimal(6,2) null,
    ms_bid_4 decimal(6,2) null,
    ms_bid_5 decimal(6,2) null,
    ms_bid_6 decimal(6,2) null,
    ms_bid_7 decimal(6,2) null,
    ms_bid_8 decimal(6,2) null,
    ms_bid_9 decimal(6,2) null,
    ms_bid_10 decimal(6,2) null,
    ms_bid_11 decimal(6,2) null,
    ms_bid_12 decimal(6,2) null,
    ms_fee_1 decimal(6,2) null,
    ms_fee_2 decimal(6,2) null,
    ms_fee_3 decimal(6,2) null,
    ms_fee_4 decimal(6,2) null,
    ms_fee_5 decimal(6,2) null,
    ms_fee_6 decimal(6,2) null,
    ms_fee_7 decimal(6,2) null,
    ms_fee_8 decimal(6,2) null,
    ms_fee_9 decimal(6,2) null,
    ms_fee_10 decimal(6,2) null,
    ms_fee_11 decimal(6,2) null,
    ms_fee_12 decimal(6,2) null,
    quality_factor decimal(6,4) null,
    conv_b decimal(10,8) null,
    cbid_cpo_1 decimal(6,2) null,
    cbid_cpo_2 decimal(6,2) null,
    cbid_cpo_3 decimal(6,2) null,
    cbid_cpo_4 decimal(6,2) null,
    cbid_cpo_5 decimal(6,2) null,
    cbid_cpo_6 decimal(6,2) null,
    cbid_cpo_7 decimal(6,2) null,
    cbid_cpo_8 decimal(6,2) null,
    cbid_cpo_9 decimal(6,2) null,
    cbid_cpo_10 decimal(6,2) null,
    fee_cpo_1 decimal(6,2) null,
    fee_cpo_2 decimal(6,2) null,
    fee_cpo_3 decimal(6,2) null,
    fee_cpo_4 decimal(6,2) null,
    fee_cpo_5 decimal(6,2) null,
    fee_cpo_6 decimal(6,2) null,
    fee_cpo_7 decimal(6,2) null,
    fee_cpo_8 decimal(6,2) null,
    fee_cpo_9 decimal(6,2) null,
    fee_cpo_10 decimal(6,2) null,
    is_strategy_reach tinyint(1) default 0 not null,
    is_dont_up_to_min tinyint(1) default 0 not null,
    constraint fk_broker_bid_shop_offer1_idx
        unique (offer_id),
    constraint fk_broker_bid_shop1
        foreign key (shop_id) references shop (id),
    constraint fk_broker_bid_shop_offer1
        foreign key (offer_id) references shop_offer (id)
            on update cascade on delete cascade
);

create index fk_broker_bid_shop1_idx
    on broker_bid (shop_id);

create index updated_at
    on broker_bid (updated_at);




create table shop_offer_param
(
    id int unsigned auto_increment primary key,
    shop_id int not null,
    name varchar(255) not null,
    created_at datetime not null,
    constraint shop_name_idx
        unique (shop_id, name),
    constraint FK_F5A0F7704D16C4DD
        foreign key (shop_id) references shop (id)
            on delete cascade
);

create index IDX_F5A0F7704D16C4DD
    on shop_offer_param (shop_id);


create table if not exists shop_offer_param_offer
(
    offer_id bigint unsigned not null,
    param_id int unsigned not null,
    shop_id int not null,
    feed_id int not null,
    value int null,
    update_status enum('NEW', 'OLD') null,
    primary key (offer_id, param_id),
    constraint FK_ACBC42104D16C4DD
        foreign key (shop_id) references shop (id)
            on delete cascade,
    constraint FK_ACBC421051A5BC03
        foreign key (feed_id) references shop_feed (id)
            on delete cascade,
    constraint FK_ACBC421053C674EE
        foreign key (offer_id) references shop_offer (id)
            on delete cascade,
    constraint FK_ACBC42105647C863
        foreign key (param_id) references shop_offer_param (id)
            on delete cascade
);

create index IDX_ACBC42104D16C4DD
    on shop_offer_param_offer (shop_id);

create index IDX_ACBC421051A5BC03
    on shop_offer_param_offer (feed_id);

create index IDX_ACBC421053C674EE
    on shop_offer_param_offer (offer_id);

create index IDX_ACBC42105647C863
    on shop_offer_param_offer (param_id);

create index shop_param_value_idx
    on shop_offer_param_offer (shop_id, param_id, value);


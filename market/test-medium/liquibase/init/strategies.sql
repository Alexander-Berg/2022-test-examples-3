create table broker_fast_models
(
    ya_region_id int unsigned not null,
    ya_model_id int unsigned not null,
    update_status varchar(3) null,
    primary key (ya_region_id, ya_model_id)
);


create table shop_offer_filter_directory
(
    id int unsigned auto_increment
        primary key,
    shop_id int not null,
    name varchar(255) not null,
    constraint FK_2081B984D16C4DD
        foreign key (shop_id) references shop (id)
            on delete cascade
);

create index IDX_2081B984D16C4DD
    on shop_offer_filter_directory (shop_id);


create table shop_offer_filter
(
    id int auto_increment
        primary key,
    shop_id int null,
    name varchar(255) null,
    query varchar(20000) null,
    category varchar(255) null,
    price_from int null,
    price_to int null,
    has_card tinyint(1) null,
    feed_id int null,
    created_at datetime null,
    updated_at datetime null,
    sort_order int null,
    purchase_price_from int null,
    purchase_price_to int null,
    click_days int null,
    click_count_from int null,
    click_count_to int null,
    click_price_from int null,
    click_price_to int null,
    order_days int null,
    order_count_from int null,
    order_count_to int null,
    order_cpo_from int null,
    order_cpo_to int null,
    is_available tinyint(1) null,
    card_position_from int null,
    card_position_to int null,
    bid_card_from decimal(12,2) null,
    bid_card_to decimal(12,2) null,
    bid_search_from decimal(12,2) null,
    bid_search_to decimal(12,2) null,
    is_shipping tinyint(1) null,
    card_price_from_type varchar(20) null,
    card_price_from_delta double null,
    card_price_to_type varchar(20) null,
    card_price_to_delta double null,
    is_free_shipping tinyint(1) null,
    is_query_only_id tinyint(1) null,
    competitors_rating_from int null,
    competitors_rating_to int null,
    is_local_delivery tinyint(1) null,
    ids_list longtext null comment '(DC2Type:json_array)',
    is_count_offers_in_model tinyint(1) null,
    has_strategy tinyint(1) null,
    type enum('SHOP', 'PRESET') default 'SHOP' not null,
    description varchar(255) null,
    order_conversion_from double null,
    order_conversion_to double null,
    category_id bigint unsigned null,
    reach_strategy tinyint(1) null,
    bid_card_recommended_position smallint(6) null,
    bid_card_recommended_from decimal(12,2) null,
    bid_card_recommended_to decimal(12,2) null,
    experiment_type varchar(20) null,
    is_discount tinyint(1) null,
    is_cpa tinyint(1) null,
    min_cbid_from decimal(12,2) unsigned null,
    min_cbid_to decimal(12,2) unsigned null,
    min_bid_from decimal(12,2) unsigned null,
    min_bid_to decimal(12,2) unsigned null,
    min_match_cbid smallint(6) null,
    min_match_bid smallint(6) null,
    hash varchar(255) null,
    is_hide_on_market tinyint(1) null,
    category_type enum('NAME', 'ID') default 'NAME' null,
    quality_factor_from decimal(4,2) null,
    quality_factor_to decimal(4,2) null,
    order_drr_from decimal(5,2) null,
    order_drr_to decimal(5,2) null,
    directory_id int unsigned null,
    is_dont_up_to_min tinyint(1) null,
    bid_from decimal(12,2) null,
    bid_to decimal(12,2) null,
    bid_recommended_position smallint(6) null,
    bid_recommended_from decimal(12,2) null,
    bid_recommended_to decimal(12,2) null,
    is_cutprice tinyint(1) null,
    constraint FK_BBF44F2E2C94069F
        foreign key (directory_id) references shop_offer_filter_directory (id)
            on delete set null,
    constraint FK_BBF44F2E4D16C4DD
        foreign key (shop_id) references shop (id)
);

create index IDX_BBF44F2E4D16C4DD
    on shop_offer_filter (shop_id);


create table shop_offer_filter_shop_category
(
    shop_offer_filter_id int not null,
    shop_category_id bigint unsigned not null,
    primary key (shop_offer_filter_id, shop_category_id),
    constraint FK_D587E61399A3CBAE
        foreign key (shop_offer_filter_id) references shop_offer_filter (id)
            on delete cascade
);

create index IDX_D587E61399A3CBAE
    on shop_offer_filter_shop_category (shop_offer_filter_id);

create index IDX_D587E613C0316BF2
    on shop_offer_filter_shop_category (shop_category_id);


create table shop_offer_filter_shop_vendor
(
    shop_offer_filter_id int not null,
    shop_vendor_id bigint unsigned not null,
    primary key (shop_offer_filter_id, shop_vendor_id),
    constraint FK_491B30E699A3CBAE
        foreign key (shop_offer_filter_id) references shop_offer_filter (id)
            on delete cascade,
    constraint FK_491B30E6EFB460F8
        foreign key (shop_vendor_id) references shop_vendor (id)
            on delete cascade
);

create index IDX_491B30E699A3CBAE
    on shop_offer_filter_shop_vendor (shop_offer_filter_id);

create index IDX_491B30E6EFB460F8
    on shop_offer_filter_shop_vendor (shop_vendor_id);



create table broker_strategy
(
    id int auto_increment primary key,
    object_id bigint unsigned not null,
    object_type enum('SHOP', 'CATEGORY', 'OFFER', 'FILTER', 'SHOP_OFF') not null,
    card_position int null,
    card_delta decimal(12,2) null,
	card_max decimal(12,2) null,
	card_is_use_reserve tinyint(1) null,
	search_position int null,
	search_delta decimal(12,2) null,
	search_max decimal(12,2) null,
	card_reserve_position int null,
	card_reserve_delta double null,
	card_reserve_max double null,
	created_at datetime null,
	updated_at datetime null,
	shop_id int default 0 not null,
	card_delta_type varchar(20) default 'PERCENT' not null,
	search_delta_type varchar(20) default 'PERCENT' not null,
	card_reserve_delta_type varchar(20) default 'PERCENT' not null,
	search_is_use_reserve tinyint(1) null,
	search_reserve_position int null,
	search_reserve_delta double null,
	search_reserve_delta_type varchar(20) default 'PERCENT' not null,
	search_reserve_max double null,
	card_is_use_marginality tinyint(1) null,
	search_is_use_marginality tinyint(1) null,
	card_marginality_coeff_conversion_type varchar(20) default 'DEFAULT' not null,
	card_marginality_coeff_conversion_value decimal(12,2) null,
	card_marginality_coeff_phone_type varchar(20) default 'DEFAULT' not null,
	card_marginality_coeff_phone_value decimal(12,2) null,
	card_marginality_coeff_marketing_type varchar(20) default 'DEFAULT' not null,
	card_marginality_coeff_marketing_value decimal(12,2) null,
	search_marginality_coeff_conversion_type varchar(20) default 'DEFAULT' not null,
	search_marginality_coeff_conversion_value decimal(12,2) null,
	search_marginality_coeff_phone_type varchar(20) default 'DEFAULT' not null,
	search_marginality_coeff_phone_value decimal(12,2) null,
	search_marginality_coeff_marketing_type varchar(20) default 'DEFAULT' not null,
	search_marginality_coeff_marketing_value decimal(12,2) null,
	marginality_coeff_conversion_recommended decimal(12,2) null,
	card_marginality_default_max decimal(12,2) null,
	search_marginality_default_max decimal(12,2) null,
	is_card_marginality_auto_conversion tinyint(1) null,
	card_marginality_min_conversion_value int null,
	card_marginality_min_conversion_days int null,
	is_search_marginality_auto_conversion tinyint(1) null,
	search_marginality_min_conversion_value int null,
	search_marginality_min_conversion_days int null,
	card_cpo_is_use tinyint(1) null,
	card_cpo_target_price decimal(12,2) null,ga_position int null,
    ga_max_cpa decimal(6,2) null,
    ga_max_cpc decimal(6,2) null,
    ga_custom_value decimal(6,2) null,
    is_dont_up_to_min tinyint(1) default 0 not null,
    ga_delta decimal(6,2) null,
    ga_delta_type varchar(20) default 'PERCENT' not null,
    ga_reserve_position int null,
    ga_reserve_max_cpa decimal(6,2) null,
    ga_reserve_max_cpc decimal(6,2) null,
    ga_reserve_custom_value decimal(6,2) null,
    ga_reserve_delta decimal(6,2) null,
    ga_reserve_delta_type varchar(20) default 'PERCENT' not null,
    is_reserve_dont_up_to_min tinyint(1) default 0 not null,
    is_native_conversion tinyint(1) default 0 not null,
    native_conversion_percent decimal(6,2) null,
    native_conversion_coef decimal(6,2) null,
    is_reserve_native_conversion tinyint(1) default 0 not null,
    reserve_native_conversion_percent decimal(6,2) null,
    reserve_native_conversion_coef decimal(6,2) null,
    native_conversion decimal(10,8) null,
	search_cpo_is_use tinyint(1) null,
	search_cpo_target_price decimal(12,2) null,
	card_max_recommended double null,
	card_reserve_max_recommended double null,
	fee_position int null,
	fee_max decimal(6,2) null,
	fee_value decimal(6,2) null,
	cpa_recommended decimal(6,2) null,
	is_ga tinyint(1) default 0 not null,
	constraint shop_object_type_object_id_idx
		unique (shop_id, object_type, object_id),
	constraint fk_shop_id
		foreign key (shop_id) references shop (id)
			on update cascade on delete cascade
);


create table shop_offer_param_filter
(
    id int unsigned auto_increment
        primary key,
    filter_id int not null,
    param_id int unsigned not null,
    value_from int null,
    value_to int null,
    constraint filter_param_idx
        unique (filter_id, param_id),
    constraint FK_A39738175647C863
        foreign key (param_id) references shop_offer_param (id)
            on delete cascade,
    constraint FK_A3973817D395B25E
        foreign key (filter_id) references shop_offer_filter (id)
            on delete cascade
);

create index IDX_A39738175647C863
    on shop_offer_param_filter (param_id);

create index IDX_A3973817D395B25E
    on shop_offer_param_filter (filter_id);




create table if not exists content_region
(
    id int unsigned not null
        primary key,
    parent_id int unsigned null,
    lft int not null,
    lvl int not null,
    rgt int not null,
    root int null,
    name varchar(255) not null,
    type int not null,
    update_status varchar(255) null,
    created_at datetime not null,
    updated_at datetime null,
    model_clicks_count int null,
    constraint FK_675481DA727ACA70
        foreign key (parent_id) references content_region (id)
            on delete cascade
);

create index IDX_675481DA727ACA70
    on content_region (parent_id);


create table content_category_conversion
(
    region varchar(10) not null,
    category_id int unsigned not null,
    clicks double unsigned default '0' not null,
    orders double unsigned default '0' not null,
    conv double unsigned default '0' not null,
    primary key (region, category_id)
);

ALTER TABLE broker_strategy ADD COLUMN type ENUM('MAIN', 'SEARCH') NOT NULL DEFAULT 'MAIN' AFTER id;
ALTER TABLE broker_strategy ADD COLUMN parent_id INT(11) NULL DEFAULT NULL AFTER type;
ALTER TABLE broker_strategy ADD UNIQUE INDEX type_parent_id_idx (type ASC, parent_id ASC);
ALTER TABLE broker_strategy ADD INDEX fk_parent_id_idx (parent_id ASC);

ALTER TABLE broker_strategy ADD CONSTRAINT fk_parent_id FOREIGN KEY (parent_id)
    REFERENCES broker_strategy (id)
    ON DELETE CASCADE ON UPDATE CASCADE;


create table shop_offer_filter_ya_category
(
	filter_id int not null,
	ya_category_id bigint not null,
	primary key (filter_id, ya_category_id),
	constraint FK_27D3DF49D395B25E
		foreign key (filter_id) references shop_offer_filter (id)
			on delete cascade
);

create index IDX_27D3DF49D395B25E
	on shop_offer_filter_ya_category (filter_id);


create table shop_offer_filter_vendor
(
	filter_id int not null,
	vendor varchar(255) not null,
	primary key (filter_id, vendor),
	constraint FK_1A5A110BD395B25E
		foreign key (filter_id) references shop_offer_filter (id)
			on delete cascade
);

create index IDX_1A5A110BD395B25E
	on shop_offer_filter_vendor (filter_id);


create table shop_offer_param_name_filter
(
	filter_id int not null,
	param varchar(255) not null,
	value_from int null,
	value_to int null,
	primary key (filter_id, param),
	constraint FK_D68A01B1D395B25E
		foreign key (filter_id) references shop_offer_filter (id)
			on delete cascade
);

create index IDX_D68A01B1D395B25E
	on shop_offer_param_name_filter (filter_id);


ALTER TABLE `shop_offer_filter` ADD COLUMN `shard` VARCHAR(20) NULL;
ALTER TABLE `broker_strategy` ADD COLUMN `shard` VARCHAR(20) NULL;

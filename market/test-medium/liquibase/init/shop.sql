create table shop_vendor
(
    id bigint unsigned auto_increment
        primary key,
    name varchar(255) not null,
    created_at datetime null,
    constraint UNIQ_708015745E237E06
        unique (name)
);

create table  shop (
    id int auto_increment primary key,
    user_id int not null,
    ya_campaign_id int not null,
    ya_shop_id bigint not null,
    ya_region_id int null,
    custom_ya_region_id int null,
    status enum('ACTIVE', 'INACTIVE', 'ACTIVATING', 'DEACTIVATING', 'MOVING_ACTIVE', 'MOVING_INACTIVE') null,
    name text null,
    domain varchar(255) not null,
    ya_state tinyint not null,
    ya_state_reasons varchar(255) null,
    id_field varchar(255) null,
    default_bid decimal(12,2) null,
    default_cbid decimal(12,2) null,
    schedule longtext null comment '(DC2Type:json_array)',
    balance_day_limit decimal(12,2) null,
	is_balance_day_limit_reached tinyint default 0 not null,
	created_at datetime null,
	updated_at datetime null,
	settings longtext null comment '(DC2Type:json_array)',
	ya_content_api_key varchar(255) null,
	is_token_valid tinyint(1) default 1 not null,
	is_api_day_limit_reached tinyint(1) default 0 not null,
	default_bid_book decimal(12,2) null,
	default_cbid_book decimal(12,2) null,
	is_cpa tinyint(1) default 0 not null,
	default_fee decimal(6,2) null,
	default_fee_book decimal(6,2) null,
	last_activity datetime null,
	shard varchar(20) not null,
	constraint ya_campaign_id unique (ya_campaign_id),
	constraint ya_shop_id unique (ya_shop_id)
);

create index fk_shop_user_idx
    on shop (user_id);

--

create table  user
(
    id int auto_increment
        primary key,
    username varchar(255)  not null,
    username_canonical varchar(255)  not null,
    email varchar(255)  not null,
    email_canonical varchar(255)  not null,
    enabled tinyint(1) not null,
    salt varchar(255)  not null,
    password varchar(255)  not null,
    last_login datetime null,
    locked tinyint(1) not null,
    expired tinyint(1) not null,
    expires_at datetime null,
    confirmation_token varchar(255)  null,
    password_requested_at datetime null,
    roles longtext  not null comment '(DC2Type:array)',
    credentials_expired tinyint(1) not null,
    credentials_expire_at datetime null,
    ya_id bigint(11) null,
    ya_login varchar(255)  null,
    passport_login varchar(255) null,
    ya_access_token varchar(255)  null,
    ya_name varchar(255)  null,
    ya_sex varchar(255)  null,
    created_at datetime null,
    updated_at datetime null,
    phone varchar(255)  null,
    name varchar(255)  null,
    second_name varchar(255)  null,
    surname varchar(255) null,
    comment text null,
    settings longtext null comment '(DC2Type:json_array)',
    temporary_password varchar(255) null,
    ga_access_token varchar(255) null,
    ga_refresh_token varchar(255) null,
    ga_expire_time datetime null,
    market_cpa_registration_site varchar(255) null,
    stats_referer varchar(2000) null,
    stats_url varchar(2000) null,
    stats_keyword varchar(255) null,
    stats_source varchar(255) null,
    last_contact datetime null,
    next_contact datetime null,
    ya_metrika_id int null,
    ya_metrika_login varchar(255) null,
    ya_metrika_access_token varchar(255) null,
    ya_metrika_name varchar(255) null,
    ya_metrika_sex varchar(255) null,
    is_subscribed tinyint(1) default 1 not null,
    is_ya_agreement_accepted tinyint(1) default 0 not null,
    ya_agreement_accepted_at datetime null,
    shard varchar(20) default 'shard1' not null,
    status enum('ACTIVE', 'MOVING') default 'ACTIVE' not null,
    version tinyint default 1 not null,
    constraint UNIQ_8D93D64992FC23A8
        unique (username_canonical),
    constraint UNIQ_8D93D649A0D96FBF
        unique (email_canonical)
);

create table shop_feed
(
    id int auto_increment
        primary key,
    shop_id int not null,
    ya_id int not null,
    url text null,
    login varchar(45) null,
    password varchar(45) null,
    name varchar(45) null,
    uploaded_at datetime null,
    expire_at datetime null,
    status enum('ACTIVE', 'DELETED') default 'ACTIVE' not null,
    created_at datetime null,
    updated_at datetime null,
    has_offers_hide_on_market tinyint(1) default 0 not null,
    constraint fk_shop_feed_shop1
        foreign key (shop_id) references shop (id)
            on update cascade on delete cascade
);

create index fk_shop_feed_shop1_idx
    on shop_feed (shop_id);


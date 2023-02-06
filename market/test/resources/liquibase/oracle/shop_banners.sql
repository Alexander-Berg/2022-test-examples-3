--liquibase formatted sql

--changeset s-myachenkov:test-ora
create table shops_web.shop_banners (
    shop_id bigint not null,
    banner_id varchar(64) not null,
    uber smallint,
    business_display_type varchar(16),
    constraint pk_shoban_shoid_banid primary key (shop_id,banner_id)
) ;

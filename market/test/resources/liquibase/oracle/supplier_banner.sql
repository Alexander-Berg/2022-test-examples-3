--liquibase formatted sql

--changeset s-myachenkov:test-ora-supplier-banner
create table shops_web.supplier_banner (
                                           supplier_id bigint not null,
                                           banner_id varchar(256) not null
) ;
create index i_supban_supid_banid on shops_web.supplier_banner (supplier_id, banner_id);
create index i_supban_banid on shops_web.supplier_banner (banner_id);



--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.supplier_feed (
                                         id bigint not null default nextval('shops_web.s_datafeed'),
                                         supplier_id bigint not null,
                                         url varchar(200),
                                         upload_id bigint,
                                         login varchar(200),
                                         password varchar(200),
                                         updated_at timestamp with time zone not null default current_timestamp,
                                         ff_service_id bigint,
                                         validation_id bigint,
                                         timeout int,
                                         period int,
                                         is_default smallint not null default 0,
                                         constraint pk_supfee_id primary key (id)
) ;
alter table shops_web.supplier_feed add constraint c_supfee_isdef_url check ((is_default = 0 and url is not null) or (is_default = 1 and url is null));
create index i_supfee_supid on shops_web.supplier_feed (supplier_id);
create index i_supfee_ffserid on shops_web.supplier_feed (ff_service_id);
create unique index i_supfee_isdef_supid on shops_web.supplier_feed ((case when is_default=1 then supplier_id else null end));
alter table shops_web.supplier_feed add constraint fk_supfee_supid_sup_id foreign key (supplier_id) references shops_web.supplier(id);

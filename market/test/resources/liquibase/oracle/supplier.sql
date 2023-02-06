--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.supplier (
                                    id bigint not null default nextval('shops_web.s_datasource'),
                                    campaign_id bigint not null default nextval('market_billing.s_campaign_info'),
                                    name varchar(512) not null,
                                    domain varchar(512),
                                    client_id bigint,
                                    created_at timestamp(6) with time zone,
                                    prepay_request_id bigint,
                                    commit_id bigint,
                                    type smallint not null default 3,
                                    has_mapping smallint not null default 0,
                                    constraint pk_sup_id primary key (id)
);

alter table shops_web.supplier add constraint i_sup_camid unique (campaign_id);
alter table shops_web.supplier add constraint fk_sup_id_par_id foreign key (id) references shops_web.partner(id);

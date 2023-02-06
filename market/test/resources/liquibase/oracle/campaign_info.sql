--liquibase formatted sql
--changeset stani:test-ora
create table market_billing.campaign_info (
	campaign_id bigint not null,
    product_id bigint,
    datasource_id bigint not null,
    end_date timestamp with time zone,
    start_date timestamp with time zone not null,
    billing_type bigint not null default 0,
	client_id bigint not null default 0,
	constraint pk_caminf_camid primary key (campaign_id)
) ;
comment on table market_billing.campaign_info is e'levong todo: new_billing_date->start_date (дата начала кампании).
при смене типа биллинга создавать новую кампанию';
create index i_caminf_enddat_datid on market_billing.campaign_info (end_date, datasource_id);
create index i_caminf_datid on market_billing.campaign_info (datasource_id);
create index i_caminf_cliid on market_billing.campaign_info (client_id);
create index i_caminf_biltyp on market_billing.campaign_info (billing_type);

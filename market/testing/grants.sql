--liquibase formatted sql

--changeset skiftcha:MARKETBILLING-501-grants
grant usage on schema market_billing to mbi_billing_testing;
grant usage on schema shops_web to mbi_billing_testing;
grant select, insert, update, delete on all tables in schema market_billing to mbi_billing_testing;
grant select, insert, update, delete on all tables in schema shops_web to mbi_billing_testing;
grant usage, select on all sequences in schema market_billing to mbi_billing_testing;
grant usage, select on all sequences in schema shops_web to mbi_billing_testing;
alter default privileges in schema market_billing grant select, insert, update, delete on tables to mbi_billing_testing;
alter default privileges in schema shops_web grant select, insert, update, delete on tables to mbi_billing_testing;
alter default privileges in schema market_billing grant usage, select on sequences to mbi_billing_testing;
alter default privileges in schema shops_web grant usage, select on sequences to mbi_billing_testing;

--changeset rhrrd:MARKETBILLING-3356-grants-to-market_stat-refresh
grant usage on schema market_billing to market_stat;
grant select on market_billing.v_yt_payout_group_payment_order to market_stat;
grant select on market_billing.v_yt_payout_correction to market_stat;
grant select on market_billing.v_yt_payout to market_stat;
grant select on market_billing.v_yt_payment_order to market_stat;
grant select on market_billing.v_yt_storage_billing to market_stat;
grant select on market_billing.v_yt_accrual to market_stat;
grant select on market_billing.v_yt_accrual_correction to market_stat;
grant select on market_billing.v_yt_partner_gmv to market_stat;
grant select on market_billing.v_yt_payment_order_draft to market_stat;
grant select on market_billing.v_yt_installment_return_billed_amount to market_stat;

--changeset voznyuk-da:MARKETBILLING-51-grants-to-mbi_billing_testing
grant market_billing_testing to mbi_billing_testing;

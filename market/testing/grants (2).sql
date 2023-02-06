--liquibase formatted sql

--changeset andreybystrov:MARKETBILLING-204-grants-testing
grant usage on schema mbi_tariffs to mbi_tariffs_testing;
grant usage on schema public to mbi_tariffs_testing;

grant usage on schema mbi_tariffs to mbi_tariffs_testing_ro;
grant usage on schema public to mbi_tariffs_testing_ro;

grant usage on schema mbi_tariffs to mbi_tariffs_testing_rw;
grant usage on schema public to mbi_tariffs_testing_rw;

grant select, insert, update, delete on all tables in schema mbi_tariffs to mbi_tariffs_testing;
grant select, insert, update, delete on all tables in schema public to mbi_tariffs_testing;

grant select on all tables in schema mbi_tariffs to mbi_tariffs_testing_ro;
grant select on all tables in schema public to mbi_tariffs_testing_ro;

grant select, insert, update, delete on all tables in schema mbi_tariffs to mbi_tariffs_testing_rw;
grant select, insert, update, delete on all tables in schema public to mbi_tariffs_testing_rw;

alter default privileges in schema mbi_tariffs grant select, insert, update, delete on tables to mbi_tariffs_testing;
alter default privileges in schema public grant select, insert, update, delete on tables to mbi_tariffs_testing;

alter default privileges in schema mbi_tariffs grant select on tables to mbi_tariffs_testing_ro;
alter default privileges in schema public grant select on tables to mbi_tariffs_testing_ro;

alter default privileges in schema mbi_tariffs grant select, insert, update, delete on tables to mbi_tariffs_testing_rw;
alter default privileges in schema public grant select, insert, update, delete on tables to mbi_tariffs_testing_rw;

--changeset dfirsa:MBI-65608-grant-v_yt_exp_fee_tariffs-grant-testing-schema
grant usage on schema mbi_tariffs to market_stat;

--changeset dfirsa:MBI-65608-grant-v_yt_exp_fee_tariffs-grant-testing
grant select on mbi_tariffs.v_yt_exp_fee_tariffs to market_stat;

--liquibase formatted sql
--changeset jaguar1337:MBI-79783-create-shops-web-datasources-in-testing-view
create view shops_web.v_yt_exp_datasource_in_testing as
select id,
       datasource_id,
       ready,
       approved,
       in_progress,
       cancelled,
       push_ready_count,
       fatal_cancelled,
       iter_count,
       updated_at,
       recommendations,
       start_date,
       testing_type,
       claim_link,
       status,
       attempt_num,
       quality_check_required,
       clone_check_required,
       shop_program
from shops_web.datasources_in_testing;

--changeset jaguar1337:MBI-81117
grant select on shops_web.v_yt_exp_datasource_in_testing to market_stat;

--liquibase formatted sql

--changeset fbokovikov:VNDMARKET-2501_fix_campaigns_in_testing
update cs_billing.campaign
set is_offer = 0, act_text = null
where cs_id = 206;
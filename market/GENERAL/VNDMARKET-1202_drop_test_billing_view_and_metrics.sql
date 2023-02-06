--liquibase formatted sql

--changeset vsubhuman:VNDMARKET-1202_drop_test_billing_view_and_metrics

DROP TABLE CS_BILLING_TMS.FAST_BILLING_VIEW_TEST;

DROP VIEW CS_BILLING.V_CAMPAIGNS_TO_BILL_2;
--liquibase formatted sql

--changeset prof:MARKETBILLING-2583-remove-monitoring-marketing_tlog_export
call monitor.drop_monitoring('market_mbi_billing', 'mbi-billing', 2662);

--changeset prof:MARKETBILLING-2583-remove-monitoring-marketing_tlog_collection
call monitor.drop_monitoring('market_mbi_billing', 'mbi-billing', 2663);

--changeset prof:MARKETBILLING-2583-remove-monitoring-marketing_tlog_consistency
call monitor.drop_monitoring('market_mbi_billing', 'mbi-billing', 2664);

--changeset prof:MARKETBILLING-2583-remove-monitoring-marketing_balance_acts_ok
call monitor.drop_monitoring('market_mbi_billing', 'mbi-billing', 2665);

--liquibase formatted sql

--changeset evmaksimenko:MBI-77970-remove-monitoring-mon_delivered_payout_trantimes
call monitor.drop_monitoring('market_mbi_billing', 'mbi-billing', 2742);

--changeset evmaksimenko:MBI-77970-remove-monitoring-mon_payout_trantime_wo_accrual
call monitor.drop_monitoring('market_mbi_billing', 'mbi-billing', 2723);

--changeset evmaksimenko:MBI-77970-remove-monitoring-mon_unprcssd_payout_trantimes
call monitor.drop_monitoring('market_mbi_billing', 'mbi-billing', 2762);

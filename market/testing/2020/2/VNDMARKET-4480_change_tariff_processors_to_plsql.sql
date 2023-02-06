--liquibase formatted sql

--changeset rpanasenkov:VNDMARKET-4480_change_tariff_processors_to_plsql
BEGIN
    --PROCESSORS
    UPDATE CS_BILLING.TARIFF_SERVICE
    SET CHARGE_SQL = 'BEGIN :charges := CHARGE_CLICKS(:billing); END;'
    WHERE CHARGE_SQL LIKE '%ChargeCentClicksTariffProcessor%';

    UPDATE CS_BILLING.TARIFF_SERVICE
    SET CHARGE_SQL = '%BEGIN :charges := CHARGE_CAMPAIGN_EXTRA_SUM(:billing); END;'
    WHERE CHARGE_SQL LIKE 'CampaignExtraChargeTariffProcessor%';

    --POST PROCESSORS
    UPDATE CS_BILLING.TARIFF_POSTPROCESSOR
    SET PROCESS_SQL = 'BEGIN :charges := COMPENSATE_OVERSPENDING(:billing); END;'
    WHERE PROCESS_SQL LIKE '%CompensateOverspendingLastBalanceTariffProcessor%';

    UPDATE CS_BILLING.TARIFF_POSTPROCESSOR
    SET PROCESS_SQL = 'BEGIN :charges := CHARGE_CAMPAIGN_EXTRA_SUM(:billing); END;'
    WHERE PROCESS_SQL LIKE '%CampaignExtraChargeTariffProcessor%';
END;
/

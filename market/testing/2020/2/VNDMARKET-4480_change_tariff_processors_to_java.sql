--liquibase formatted sql

--changeset rpanasenkov:VNDMARKET-4480_change_tariff_processors_to_java
BEGIN
    --PROCESSORS
    UPDATE CS_BILLING.TARIFF_SERVICE
    SET CHARGE_SQL = 'ChargeCentClicksTariffProcessor'
    WHERE CHARGE_SQL LIKE '%BEGIN :charges := CHARGE_CLICKS(:billing); END;%';

    UPDATE CS_BILLING.TARIFF_SERVICE
    SET CHARGE_SQL = 'CampaignExtraChargeTariffProcessor'
    WHERE CHARGE_SQL LIKE '%BEGIN :charges := CHARGE_CAMPAIGN_EXTRA_SUM(:billing); END;%';

    --POST PROCESSORS
    UPDATE CS_BILLING.TARIFF_POSTPROCESSOR
    SET PROCESS_SQL = 'CompensateOverspendingLastBalanceTariffProcessor'
    WHERE PROCESS_SQL LIKE '%BEGIN :charges := COMPENSATE_OVERSPENDING(:billing); END;%';

    UPDATE CS_BILLING.TARIFF_POSTPROCESSOR
    SET PROCESS_SQL = 'CampaignExtraChargeTariffProcessor'
    WHERE PROCESS_SQL LIKE '%BEGIN :charges := CHARGE_CAMPAIGN_EXTRA_SUM(:billing); END;%';

    DELETE
    FROM CS_BILLING.TARIFF_POSTPROCESSOR
    WHERE PROCESS_SQL LIKE '%BEGIN :charges := CUTOFF_BY_FINANCE_SHORTAGE(:billing); END;%';
END;
/

--liquibase formatted sql

--changeset vsubhuman:VNDMARKET-971_revert_tariff_change_for_vendor endDelimiter:///
BEGIN
  DELETE FROM CS_BILLING.CAMPAIGN_TARIFF
  WHERE CS_ID = 132
    AND CAMPAIGN_ID = 2900
    AND START_DATE > DATE '2017-09-30';

  UPDATE CS_BILLING.CAMPAIGN_TARIFF
    SET END_DATE = NULL
  WHERE CS_ID = 132
    AND CAMPAIGN_ID = 2900
    AND trunc(END_DATE) = DATE '2017-09-30';
END;
///

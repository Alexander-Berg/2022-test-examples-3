--liquibase formatted sql

--changeset mexicano:MARKETBILLING-4205 endDelimiter://
CREATE OR REPLACE
PROCEDURE market_billing.rollback_day_completion(i_campaign_id IN number) IS
  l_last_closed_day date;
  prior_last_closed_day date;
  l_sum_clicks number;
  l_sum_service number;
  l_sum_operation number;
  l_sum_orders_fee number;
  l_sum_orders number;
  l_cpa_orders number;
  l_cpa_fee number;
  op_id number;
  act_id number;

  prev_sum_spent number := 0;
  prev_cpc_spent number := 0;
  prev_sum_operations number := 0;
  prev_cpa_sum number := 0;
  prev_cpa_orders number := 0;
  prev_cpa_spent number := 0;
  prev_cpa_fee number := 0;


  BEGIN

    -- Get info about the last daily completion for campaign
    SELECT max(eventtime) INTO l_last_closed_day
    FROM market_billing.daily_completions WHERE campaign_id = i_campaign_id;

    IF l_last_closed_day IS NULL THEN
      RETURN;
    END IF;

    SELECT
      sum_clicks, sum_service, sum_operation, sum_orders_fee, sum_orders, cpa_orders, cpa_fee
    INTO l_sum_clicks, l_sum_service, l_sum_operation, l_sum_orders_fee, l_sum_orders, l_cpa_orders, l_cpa_fee
    FROM market_billing.daily_completions
    WHERE campaign_id = i_campaign_id AND eventtime = l_last_closed_day;

    -- Get info about the prior-to-last daily completion for campaign
    SELECT nvl(max(eventtime), l_last_closed_day - 1) INTO prior_last_closed_day
    FROM market_billing.daily_completions
    WHERE campaign_id = i_campaign_id AND eventtime < l_last_closed_day;

    -- Rollback operations' completion
    UPDATE market_billing.operation
    SET is_completed = 0
    WHERE TRUNC(trantime) = l_last_closed_day
        AND campaign_id = i_campaign_id
        AND is_completed = 1;

    -- Rollback overshipment compensation, overshipment and service payment
    DELETE FROM market_billing.operation op
    WHERE op.id in (
      SELECT
        o.id
      FROM market_billing.operation o
      WHERE operation_type_id in (2, 6)
            AND trantime >= l_last_closed_day
            AND is_completed = 0
            AND campaign_id = i_campaign_id
    );

    begin
      select SNAP_TOTAL_SUM_SPENT, SNAP_TOTAL_CPC_SPENT, SNAP_TOTAL_SUM_OPERATIONS, SNAP_TOTAL_CPA_FEE
      into prev_sum_spent, prev_cpc_spent, prev_sum_operations, prev_cpa_fee
      from market_billing.daily_completions
      where CAMPAIGN_ID = i_campaign_id and EVENTTIME = prior_last_closed_day;
    EXCEPTION
      WHEN NO_DATA_FOUND then null;-- Campaign was never completed before. So just set all values to zero.
    end;

    -- Rollback campaign balance
    UPDATE market_billing.campaign_balance_spent
    SET sum_spent = prev_sum_spent,
      last_closed = prior_last_closed_day,
      cpc_spent = prev_cpc_spent,
      sum_operations = prev_sum_operations,
      cpa_fee = prev_cpa_fee
    WHERE campaign_id = i_campaign_id AND last_closed = l_last_closed_day;

    -- Rollback daily completion itself
    DELETE FROM market_billing.daily_completions
    WHERE campaign_id = i_campaign_id
        AND eventtime = l_last_closed_day;

  END;
//

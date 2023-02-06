/* syntax version 1 */
PRAGMA Library("lib.sql");
IMPORT lib SYMBOLS $calc_max_prices;

$preresult = ( 
    SELECT  
        $calc_max_prices(
            max_old_price_by_prices_history,
            max_old_price_by_current_regular_prices,
            max_old_price_by_regular_prices_history,
            ref_min_soft_old_price,
            ref_max_soft_regular_price,
            markup_to_ref_min_soft_old,
            min_percent_delta,
            min_abs_delta
        ) AS max_prices
    FROM Input
);

SELECT CAST(max_prices[0] AS FLOAT) AS max_price,
    CAST(max_prices[1] AS FLOAT) AS max_old_price FROM $preresult;

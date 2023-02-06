------------------------ TESTS -----------------------------------

DEFINE ACTION $one_wallet_entry_per_date_test($date_from, $date_to) AS
    $data = (
        SELECT
            `date`,
            GroupOrderID,
            COUNT(*) AS cnt
        FROM
            $get_direct_wallet_limits($date_from, $date_to)
        GROUP BY
            `date`,
            GroupOrderID
        HAVING COUNT(*) > 1
    );

    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "There are " || Unwrap(CAST(COUNT(*) AS String)) || " entries of same wallet per date!")
    FROM
        $data;
END DEFINE;

DEFINE ACTION $one_order_id_entry_per_date_test($date_from, $date_to) AS
    $data = (
        SELECT
            `date`,
            OrderID,
            GroupOrderID,
            COUNT(*) AS cnt
        FROM
            $get_direct_order_limits($date_from, $date_to)
        GROUP BY
            `date`,
            OrderID,
            GroupOrderID
        HAVING COUNT(*) > 1
    );

    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "There are " || Unwrap(CAST(COUNT(*) AS String)) || " entries of same order_id per date!")
    FROM
        $data;
END DEFINE;

DEFINE ACTION $one_order_id_entry_per_date_in_raw_table_test($date_from, $date_to) AS
    $data = (
        SELECT
            `date`,
            OrderID,
            GroupOrderID,
            COUNT(*) AS cnt
        FROM
            $get_direct_daily_limits($date_from, $date_to)
        GROUP BY
            `date`,
            OrderID,
            GroupOrderID
        HAVING COUNT(*) > 1
    );

    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "There are " || Unwrap(CAST(COUNT(*) AS String)) || " entries of same order_id per date in RAW table!")
    FROM
        $data;
END DEFINE;

DEFINE ACTION $day_weight_test($date_from, $date_to) AS
    $data = (
        SELECT
            WeeklyDayWeight,
        FROM
            $get_direct_daily_limits($date_from, $date_to)
        WHERE
            (WeeklyDayWeight IS NULL OR NOT Math::IsFinite(WeeklyDayWeight))
            -- TODO
            -- OR (PeriodDayWeight IS NULL OR NOT Math::IsFinite(PeriodDayWeight))
    );

    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "WeeklyDayWeight IS NULL or infinite in  " || Unwrap(CAST(COUNT(*) AS String)) || " rows!")
    FROM
        $data;
END DEFINE;


EXPORT $one_wallet_entry_per_date_test, $one_order_id_entry_per_date_test, $one_order_id_entry_per_date_in_raw_table_test, $day_weight_test
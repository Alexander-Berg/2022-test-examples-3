USE hahn;
PRAGMA AnsiInForEmptyOrNullableItemsCollections;

$current_order_info_table = '//home/yabs/dict/replica/OrderInfo';
$NaT = 'NaT';

$timezone = "Europe/Moscow";

$tz = ($utc_ts) -> {
    return AddTimezone(DateTime::FromSeconds(CAST($utc_ts AS UInt32)), $timezone);
};

$get_date = ($utc_ts) -> {
    return DateTime::Format("%Y-%m-%d")($tz($utc_ts));
};
$is_correct_date = Pire::Match("[0-9]{4}-[0-9]{2}-[0-9]{2}");
$all_true = [true, true, true, true, true, true, true, true, true];
$timetarg_no_89 = [true, true, true, true, true, true, true, false, false];
$working_days = [true, true, true, true, true, false, false, false, false];
$timetarg_test_cases = {
    "1HILMNOPQRSTUVW2HILMNOPQRSTUVW3HILMNOPQRSTUVW4HILMNOPQRSTUVW5HILMNOPQRSTUVW6HILMNOPQRSTUVW7HILMNOPQRSTUVW": $timetarg_no_89,
    "1KLMNOPQRS2KLMNOPQRS3KLMNOPQRS4KLMNOPQRS5KLMNOPQRS89":
    [true, true, true, true, true, false, false, false, true],
    "1JKLMN2JKLMNOPQR3JKLMNOPQR4JKLMNOPQR5JKLMNOPQR678JKLMNOPQR9;p:o":
    [true, true, true, true, true, false, false, true, true],
    "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX89;p:o":
    [true, true, true, true, true, true, true, false, true],
    -- cid 38733197
    "1IJKLMNOPQR2IJKLMNOPQR3IJKLMNOPQR4IJKLMNOPQR5IJKLMNOPQR67;p:o":
    [true, true, true, true, true, false, false, false, false],
    -- 47036363
    ";p:o": $timetarg_no_89,
    -- 20135164
    ";p:a": $timetarg_no_89,
    -- 33581872
    "1JKLMNOPQRSTUV2JKLMNOPQRSTUV3JKLMNOPQRSTUV4JKLMNOPQRSTUV5JKLMNOPQRSTUV6JKLMNOPQRSTUV78JKLMNOPQRSTUV9;p:o":
    [true, true, true, true, true, true, false, true, true],
    -- 48936163
    "1ABCDEFGHIJKLMNOPQRSTUV2ABCDEFGHIJKLMNOPQRSTUV345679;p:o":
    [true, true, false, false, false, false, false, false, true],
    -- 49185142
    "1JKLMNOPQ2JKLMNOPQ3JKLMNOPQ4JKLMNOPQ5JKLMNOPQ67;p:o":
   [true, true, true, true, true, false, false, false, false],
    -- 49308884
    "1JKLMNOPQRSTUVW2JKLMNOPQRSTUVW3JKLMNOPQRSTUVW4JKLMNOPQRSTUVW5JKLMNOPQRSTUVW679":
    [true, true, true, true, true, false, false, false, true],
    -- 48266641
    "1AbBbCbDbEbFbGcHdIfJhKjLMiNhOhPgQgReSeTbUbVbWbXb2AbBbCbDbEbFbGcHdIfJhKjLMiNhOhPgQgReSeTbUbVbWbXb3AbBbCbDbEbFbGcHdIfJhKjLMiNhOhPgQgReSeTbUbVbWbXb4AbBbCbDbEbFbGcHdIfJhKjLMiNhOhPgQgReSeTbUbVbWbXb5AbBbCbDbEbFbGcHdIfJhKjLMiNhOhPgQgReSeTbUbVbWbXb67RiSiTiUiViWiXi9":
    [true, true, true, true, true, false, true, false, true],
    -- 39255041
    "12JKLMNOPQRS3JKLMNOPQRS4JKLMNOPQRS5JKLMNOPQRS67;p:o":
    [false, true, true, true, true, false, false, false, false],
    -- 33190805
    "12IJMNPQSTUV3HILMOPSTUV4IJMNPQSTUV5HILMOPSTUV6LMNOP7":
    [false, true, true, true, true, true, false, false, false],
    -- 48934725
    "1ABCDEFGHIJKLMNOPQRSTUV2ABCDEFGHIJKLMNOPQRSTUV345679;p:o":
    [true, true, false, false, false, false, false, false, true],
    -- 42819866
    "12ABCDEFGHIJKLMNOPQRSTUVWX34ABCDEFGHIJKLMNOPQRSTUVWX56ABCDEFGHIJKLMNOPQRSTUVWX7OPQRSTUVWX9;p:o":
    [false, true, false, true, false, true, true, false, true],
    -- 35607802
    "1KLMNOPQRSTU2KLMNOPQRSTU4KLMNOPQRSTU5KLMNOPQRSTU6KLMNOPQRSTU;p:o":
    [true, true, false, true, true, true, false, false, false],
    -- 35607804
    "1ACEGIJLNPQRTVX3ACEGIJLNPQRTVX5ACEGIJLNPQRTVX6ACEGIJLNPQRTVX;p:o":
    [true, false, true, false, true, true, false, false, false],
    -- 5720426
    "3KLMNOPQRSTUVWX4KLMNOPQRSTUVWX5KLMNOPQRSTUVWX6KLMNOPQRSTUVWX7KLMNOPQRSTUVWX":
    [false, false, true, true, true, true, true, false, false],
    -- 37629171
    "2ABCDEFGHWX3ABCDEFGHWX4ABCDEFGHWX5ABCDEFGHWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX8ABCDEFGHIJKLMNOPQRSTUVWX":
    [false, true, true, true, true, true, true, true, false],
    -- 37743014
    "2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX":
    [false, true, true, false, false, false, false, false, false],
    -- 46422642
    "2ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX9":
    [false, true, false, true, false, true, false, false, true],
    -- 42176738
    "4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX":
    [false, false, false, true, true, true, true, false, false],
    -- 33439940
    "4ABCDEFGHIJKLMNOPQRST5ABCDEFGHIJKLMNOPQRST":
    [false, false, false, true, true, false, false, false, false],
    -- 23888648
    "-------ABCDEFGHI---------------":
    $timetarg_no_89
  };

DEFINE ACTION $run_time_targeting_tests($parse_function) AS
    $result = (
        SELECT
        ListMap(
            ListZipAll(
                ListMap(DictKeys($timetarg_test_cases), $parse_function),
                DictPayloads($timetarg_test_cases)
            ), ($t) -> {return $t.0 == $t.1; }
        ) AS test_result
    );

    SELECT
        Ensure($result,
               ListSum(CAST(test_result AS List<Int64>)) = ListLength($timetarg_test_cases),
               "One or more time targeting test has been failed!")
    FROM $result;
END DEFINE;

DEFINE ACTION $run_empty_row_test($table) AS
    -- Empty rows
    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "Parsing resulted in " || CAST(COUNT(*) AS String) || " empty rows!")
    FROM $table()
    WHERE
        period_limit IS NULL
        AND weekly_limit IS NULL
        AND clicks_limit IS NULL
        AND strategy_finish IS NULL
        AND strategy_start IS NULL
        AND strategy_name IS NULL
        AND strategy_data IS NULL
        AND time_target IS NULL
        AND finish_time IS NULL
        AND start_time IS NULL
        AND daily_limit IS NULL
        AND platform IS NULL
        AND is_archived IS NULL
        AND status_moderate IS NULL;

    -- Wrong date format
    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "Parsing resulted in wrong date formats for " || CAST(COUNT(*) AS String) || " rows!")
    FROM $table()
    WHERE
        (finish_time IS NOT NULL AND NOT $is_correct_date(finish_time))
        OR (start_time IS NOT NULL AND NOT $is_correct_date(start_time))
        OR (strategy_finish IS NOT NULL AND NOT ($is_correct_date(strategy_finish) OR strategy_finish = $NaT))
        OR (strategy_start IS NOT NULL AND NOT ($is_correct_date(strategy_start) OR strategy_start = $NaT));

    -- No strategy name (bad JSON)
    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "Parsing resulted in `strategy_name IS NULL` in " || CAST(COUNT(*) AS String) || " rows!")
    FROM $table()
    WHERE
        strategy_data IS NOT NULL
        AND strategy_name IS NULL;

    -- Negative floats
    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "Parsing resulted in negative floats in " || CAST(COUNT(*) AS String) || " rows!")
    FROM $table()
    WHERE
        (daily_limit IS NOT NULL AND daily_limit < 0)
        OR (period_limit IS NOT NULL AND period_limit < 0)
        OR (weekly_limit IS NOT NULL AND weekly_limit < 0)
        OR (clicks_limit IS NOT NULL AND clicks_limit < 0);

    -- Wrong time targeting
    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "Parsing resulted in wrong time targeting format in " || CAST(COUNT(*) AS String) || " rows!")
    FROM $table()
    WHERE
        time_target IS NOT NULL
        AND ListLength(time_target) < 9;

    -- Wrong time targeting type
    DISCARD SELECT
        EnsureType(time_target,
                ParseType("List<Bool>?"),
                "Time targeting format is not 'List<Bool>?'!"),
    FROM $table();
END DEFINE;

DEFINE ACTION $run_limits_test($table) AS
    -- Ensure that at least one limit is NOT NULL if strategy is defined
    -- AND that we do not have limits without strategy (except for daily limit)
    DISCARD SELECT
        Ensure(NULL,
               COUNT(*) = 0,
               "Parsing resulted in " || CAST(COUNT(*) AS String) || " empty rows!")
    FROM $table()
    WHERE
        (strategy_name IS NOT NULL AND (daily_limit IS NULL OR weekly_limit IS NULL OR period_limit IS NULL OR clicks_limit IS NULL))
        OR
        (strategy_name IS NULL AND (weekly_limit IS NOT NULL OR period_limit IS NOT NULL OR clicks_limit IS NOT NULL));

    -- day_budget should be $inf OR NULL in case of wrong strategy
    DISCARD SELECT
        Ensure(NULL,
              COUNT(*) = 0,
              "Found day_budget IS NOT NULL and not $inf with wrong strategy in " || CAST(COUNT(*) AS String) || " rows!")
    FROM $table()
    WHERE
        daily_limit IS NOT NULL
        AND Math::IsFinite(daily_limit)
        AND strategy_name IS NOT NULL
        AND strategy_name NOT IN ["default", "cpm_default", "no_premium"];

    -- All limits except for day_budget should be $inf OR NULL for these 3 strategies
    DISCARD SELECT
        Ensure(NULL,
              COUNT(*) = 0,
              "Found limits is not null or not $inf for strategies ['default', 'cpm_default', 'no_premium'] in " || CAST(COUNT(*) AS String) || " rows!")
    FROM $table()
    WHERE
        strategy_name IS NOT NULL
        AND strategy_name IN ["default", "cpm_default", "no_premium"]
        AND (
            (weekly_limit IS NOT NULL AND Math::IsFinite(weekly_limit))
            OR (period_limit IS NOT NULL AND Math::IsFinite(period_limit))
            OR (clicks_limit IS NOT NULL AND Math::IsFinite(clicks_limit)));

    -- Limits should not equal to zero (should be $inf instead)
    DISCARD SELECT
        Ensure(NULL,
              COUNT(*) = 0,
              "Found limits equal to zero in " || CAST(COUNT(*) AS String) || " rows!")
    FROM $table()
    WHERE
        daily_limit IS NOT NULL AND daily_limit = 0
        OR weekly_limit IS NOT NULL AND weekly_limit = 0
        OR period_limit IS NOT NULL AND period_limit = 0
        OR clicks_limit IS NOT NULL AND clicks_limit = 0;

    $limit_is_set = ($x) -> { return IF($x IS NULL OR NOT Math::IsFinite($x), 0, 1); };

    -- There should be no more than one limit at a time
    DISCARD SELECT
        Ensure(NULL,
              COUNT(*) = 0,
              "More than one limits is set in " || CAST(COUNT(*) AS String) || " rows!")
    FROM $table()
    WHERE
        ListSum([$limit_is_set(daily_limit),
                 $limit_is_set(weekly_limit),
                 $limit_is_set(period_limit),
                 $limit_is_set(clicks_limit)]) > 1;
END DEFINE;

DEFINE ACTION $more_rows_than_test($table1, $table2) AS
    $count1 = SELECT COUNT(*) FROM $table1();
    $count2 = SELECT COUNT(*) FROM $table2();

    DISCARD SELECT
        Ensure(NULL,
               $count1 > $count2,
               "Table 1 has no more rows than Table 2: " || Unwrap(CAST($count1 AS String)) || ' vs ' || Unwrap(CAST($count2 AS String)) || "!");
END DEFINE;

DEFINE ACTION $same_set_of_dates_test($table1, $table2) AS
    $different_row_count = (
        SELECT COUNT(*)
        FROM
            (SELECT DISTINCT `date` FROM $table1()) AS a
            FULL OUTER JOIN
            (SELECT DISTINCT `date` FROM $table2()) AS b
            USING (`date`)
        WHERE
            a.`date` IS NULL
            OR b.`date` IS NULL
    );

    DISCARD SELECT
        Ensure(NULL,
               $different_row_count = 0,
               "Table 1 and Table 2 have different sets of dates!");
END DEFINE;

-- Last date should not contain NULLs
DEFINE ACTION $status_moderate_null_test($table) AS
    $max_date = SELECT MAX(`date`) FROM $table();

    DISCARD SELECT
    Ensure(NULL,
           COUNT(*) = 0,
           "There are " || Unwrap(CAST(COUNT(*) AS String)) || " last date rows with status_moderate, platform or is_archived IS NULL!")
    FROM $table()
    WHERE
        `date` = $max_date
        AND (status_moderate IS NULL OR is_archived IS NULL);
END DEFINE;

EXPORT $run_time_targeting_tests, $timetarg_no_89, $run_empty_row_test, $is_correct_date, $run_limits_test, $get_date, $more_rows_than_test, $NaT, $same_set_of_dates_test, $status_moderate_null_test;

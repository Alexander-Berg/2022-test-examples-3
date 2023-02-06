CREATE ALIAS my_trunc FOR "ru.yandex.market.DatabaseFunctions.myTrunc";
CREATE ALIAS my_last_day FOR "ru.yandex.market.DatabaseFunctions.myLastDay";

-- ClickHouse functions
CREATE ALIAS toStartOfMonth FOR "ru.yandex.market.DatabaseFunctions.toStartOfMonth";
CREATE ALIAS toStartOfYear FOR "ru.yandex.market.DatabaseFunctions.toStartOfYear";
CREATE ALIAS toDate FOR "ru.yandex.market.DatabaseFunctions.toDate";
CREATE ALIAS toMonday FOR "ru.yandex.market.DatabaseFunctions.toMonday";
CREATE ALIAS subtractYears FOR "ru.yandex.market.DatabaseFunctions.subtractYears";
CREATE ALIAS subtractDays FOR "ru.yandex.market.DatabaseFunctions.subtractDays";
CREATE ALIAS addDays FOR "ru.yandex.market.DatabaseFunctions.addDays";
CREATE ALIAS toYear FOR "ru.yandex.market.DatabaseFunctions.toYear";
CREATE ALIAS lowerUTF8 FOR "ru.yandex.market.DatabaseFunctions.lowerUTF8";
CREATE ALIAS has FOR "ru.yandex.market.DatabaseFunctions.has";
CREATE ALIAS divide FOR "ru.yandex.market.DatabaseFunctions.divide";
CREATE ALIAS ifNotFinite FOR "ru.yandex.market.DatabaseFunctions.ifNotFinite";
CREATE ALIAS hasAny FOR "ru.yandex.market.DatabaseFunctions.hasAny";
CREATE AGGREGATE sumIf FOR "ru.yandex.market.clickhouse.SumIf";
CREATE AGGREGATE uniqExactDistinctIf FOR "ru.yandex.market.clickhouse.UniqExactDistinctIf";

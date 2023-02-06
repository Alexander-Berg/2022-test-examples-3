import datetime

import pytest
import pytz

from crypta.s2s.lib.proto.conversion_pb2 import TConversion
from crypta.s2s.services.transfer_conversions_to_yt.lib import parsers
from crypta.s2s.services.transfer_conversions_to_yt.lib.default_csv_column_names import DefaultCsvColumnNames


STATIC_GOAL_ID = 1111
TIMESTAMP_RANGE = (1628000000, 1629000000)

CONVERSION_NAME_TO_GOAL_IDS = {
    "conversion-name-xxx": [STATIC_GOAL_ID],
    "conversion-name-yyy": [2222],
    "conversion-name-many": [3333, 4444],
}

CUSTOM_COLUMN_NAMES = {
    DefaultCsvColumnNames.yclid: "Custom Yclid",
    DefaultCsvColumnNames.conversion_name: "Custom Conversion Name",
    DefaultCsvColumnNames.conversion_time: "Custom Conversion Time",
    DefaultCsvColumnNames.conversion_value: "Custom Conversion Value",
    DefaultCsvColumnNames.conversion_currency: "Custom Conversion Currency",
}


TEST_CASES_FOR_CONVERSION_NAME = [
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Missing column 'Conversion Name'"),
        id="missing-conversion-name",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Empty value of column 'Conversion Name'"),
        id="empty-conversion-name",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-zzz",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Unknown value 'conversion-name-zzz' of column 'Conversion Name'"),
        id="unknown-conversion-name",
    ),
]

TEST_CASES_FOR_MANY = [
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-many",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Success([
            TConversion(
                Yclid="11111111",
                GoalId=3333,
                Timestamp=1628355714,
                Value=0.17,
                Currency="RUB",
            ),
            TConversion(
                Yclid="11111111",
                GoalId=4444,
                Timestamp=1628355714,
                Value=0.17,
                Currency="RUB",
            ),
        ]),
        id="success-many",
    ),
]

TEST_CASES_FOR_THE_REST = [
    pytest.param(
        {
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Missing column 'Yclid'"),
        id="missing-yclid",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Empty value of column 'Yclid'"),
        id="empty-yclid",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Missing column 'Conversion Time'"),
        id="missing-conversion-time",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Empty value of column 'Conversion Time'"),
        id="empty-conversion-time",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "xxx",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Invalid value 'xxx' of column 'Conversion Time'"),
        id="invalid-conversion-time",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "2021-08-03 14:13:19",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Timestamp '1627999999' for value '2021-08-03 14:13:19' of column 'Conversion Time' is too old or from future"),
        id="old-conversion-time",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "2021-08-15 04:00:01",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Timestamp '1629000001' for value '2021-08-15 04:00:01' of column 'Conversion Time' is too old or from future"),
        id="conversion-time-from-future",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "xxx",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Invalid value 'xxx' of column 'Conversion Value'"),
        id="invalid-conversion-value",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "xxx",
        },
        parsers.Failure("Invalid value 'xxx' of column 'Conversion Currency'"),
        id="invalid-conversion-currency",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Failure("Non-empty value 'RUB' of column 'Conversion Currency' while column 'Conversion Value' is missing or empty"),
        id="empty-conversion-value-non-empty-conversion-currency",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "4",
        },
        parsers.Failure("Non-empty value '4.0' of column 'Conversion Value' while column 'Conversion Currency' is missing or empty"),
        id="empty-conversion-currency-non-empty-conversion-value",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
            DefaultCsvColumnNames.conversion_value: "0.17",
            DefaultCsvColumnNames.conversion_currency: "RUB",
        },
        parsers.Success([TConversion(
            Yclid="11111111",
            GoalId=1111,
            Timestamp=1628355714,
            Value=0.17,
            Currency="RUB",
        )]),
        id="success",
    ),
    pytest.param(
        {
            DefaultCsvColumnNames.yclid: "11111111",
            DefaultCsvColumnNames.conversion_name: "conversion-name-xxx",
            DefaultCsvColumnNames.conversion_time: "08/07/2021 5:01:54 PM",
        },
        parsers.Success([TConversion(
            Yclid="11111111",
            GoalId=1111,
            Timestamp=1628355714,
        )]),
        id="success-without-conversion-value-conversion-currency",
    ),
]

TEST_CASES = TEST_CASES_FOR_CONVERSION_NAME + TEST_CASES_FOR_MANY + TEST_CASES_FOR_THE_REST
TEST_CASES_FOR_STATIC_GOAL_ID = TEST_CASES_FOR_THE_REST


@pytest.mark.parametrize("row,result", TEST_CASES)
def test_default_conversion_parser(row, result):
    parser = parsers.ConversionParser(
        timestamp_range=TIMESTAMP_RANGE,
        conversion_name_to_goal_ids=CONVERSION_NAME_TO_GOAL_IDS,
    )
    assert result == parser.parse(row)


@pytest.mark.parametrize("row,result", TEST_CASES)
def test_custom_conversion_parser(row, result):
    parser = parsers.ConversionParser(
        timestamp_range=TIMESTAMP_RANGE,
        conversion_name_to_goal_ids=CONVERSION_NAME_TO_GOAL_IDS,
        column_names=CUSTOM_COLUMN_NAMES,
    )
    row = {CUSTOM_COLUMN_NAMES[k]: v for k, v in row.items()}
    result = _convert_to_result_with_custom_column_names(result)

    assert result == parser.parse(row)


@pytest.mark.parametrize("column_names", [
    pytest.param(None, id="None"),
    pytest.param({}, id="default"),
    pytest.param(CUSTOM_COLUMN_NAMES, id="custom"),
])
@pytest.mark.parametrize("conversion_name_to_goal_ids,static_goal_id", [
    pytest.param(None, None, id="both_None"),
    pytest.param(CONVERSION_NAME_TO_GOAL_IDS, 1111, id="both"),
])
def test_parser_init_fail(column_names, conversion_name_to_goal_ids, static_goal_id):
    with pytest.raises(Exception):
        parsers.ConversionParser(
            timestamp_range=TIMESTAMP_RANGE,
            column_names=column_names,
            conversion_name_to_goal_ids=conversion_name_to_goal_ids,
            static_goal_id=static_goal_id,
        )


@pytest.mark.parametrize("row,result", TEST_CASES_FOR_STATIC_GOAL_ID)
def test_default_conversion_parser_with_static_goal_id(row, result):
    parser = parsers.ConversionParser(
        timestamp_range=TIMESTAMP_RANGE,
        static_goal_id=STATIC_GOAL_ID,
    )
    row = {k: v for k, v in row.items() if k != DefaultCsvColumnNames.conversion_name}
    assert result == parser.parse(row)


@pytest.mark.parametrize("row,result", TEST_CASES_FOR_STATIC_GOAL_ID)
def test_custom_conversion_parser_with_static_goal_id(row, result):
    parser = parsers.ConversionParser(
        timestamp_range=TIMESTAMP_RANGE,
        static_goal_id=STATIC_GOAL_ID,
        column_names=CUSTOM_COLUMN_NAMES,
    )

    row = {CUSTOM_COLUMN_NAMES[k]: v for k, v in row.items() if k != DefaultCsvColumnNames.conversion_name}
    result = _convert_to_result_with_custom_column_names(result)

    assert result == parser.parse(row)


@pytest.mark.parametrize("f,obj,result", [
    pytest.param(parsers.is_success, parsers.Failure(error_msg=""), False, id="is-success-False"),
    pytest.param(parsers.is_success, parsers.Success(result=""), True, id="is-success-True"),
    pytest.param(parsers.is_failure, parsers.Failure(error_msg=""), True, id="is-failure-True"),
    pytest.param(parsers.is_failure, parsers.Success(result=""), False, id="is-failure-False"),
])
def test_helpers(f, obj, result):
    assert result == f(obj)


@pytest.mark.parametrize("s,dt", [
    ("08/07/2021 5:01:54 PM", datetime.datetime(year=2021, month=8, day=7, hour=17, minute=1, second=54)),
    ("Aug 7, 2021 5:01:54 PM", datetime.datetime(year=2021, month=8, day=7, hour=17, minute=1, second=54)),
    ("08/07/2021 17:01:54", datetime.datetime(year=2021, month=8, day=7, hour=17, minute=1, second=54)),
    ("2021-08-07 17:01:54", datetime.datetime(year=2021, month=8, day=7, hour=17, minute=1, second=54)),
    ("2021-08-07T17:01:54", datetime.datetime(year=2021, month=8, day=7, hour=17, minute=1, second=54)),
    ("2021-08-07T16:01:54-0100", datetime.datetime(year=2021, month=8, day=7, hour=16, minute=1, second=54, tzinfo=datetime.timezone(-datetime.timedelta(hours=1)))),
    ("2021-08-07T20:01:54+0300", datetime.datetime(year=2021, month=8, day=7, hour=20, minute=1, second=54, tzinfo=datetime.timezone(datetime.timedelta(hours=3)))),
    ("2021-08-07 20:01:54 Europe/Moscow", pytz.timezone("Europe/Moscow").localize(datetime.datetime(year=2021, month=8, day=7, hour=20, minute=1, second=54))),
    ("2021-08-07T20:01:54 Europe/Moscow", pytz.timezone("Europe/Moscow").localize(datetime.datetime(year=2021, month=8, day=7, hour=20, minute=1, second=54))),
])
def test_parse_conversions_to_datetime(s, dt):
    assert dt == parsers._parse_conversion_time_to_datetime(s)


@pytest.mark.parametrize("s", [
    "08/07/2021 5:01:54 PM",
    "Aug 7, 2021 5:01:54 PM",
    "08/07/2021 17:01:54",
    "2021-08-07 17:01:54",
    "2021-08-07T17:01:54",
    "2021-08-07T16:01:54-0100",
    "2021-08-07T20:01:54+0300",
    "2021-08-07 20:01:54 Europe/Moscow",
    "2021-08-07T20:01:54 Europe/Moscow",
    "1628355714",
])
def test_parse_conversion_time_to_timestamp(s):
    assert 1628355714 == parsers._parse_conversion_time_to_timestamp(s)


def _convert_to_result_with_custom_column_names(result):
    if isinstance(result, parsers.Failure):
        result = parsers.Failure(error_msg=result.error_msg)
        for k, v in CUSTOM_COLUMN_NAMES.items():
            result.error_msg = result.error_msg.replace(k, v)

    return result

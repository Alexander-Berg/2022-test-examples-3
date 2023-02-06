from datetime import date, timedelta

import pytest

from lib.database.chyt_util.concat_tables_parsers import (AllFromDayParser,
                                                          AllFromMonthParser,
                                                          AllTablesParser,
                                                          ConcatYTTablesParser,
                                                          LastXDaysParser)

PARTITION_TODAY = date.today().isoformat()
PARTITION_1_DAY_AGO = (date.today() - timedelta(1)).isoformat()
PARTITION_2_DAYS_AGO = (date.today() - timedelta(2)).isoformat()
PARTITION_3_DAYS_AGO = (date.today() - timedelta(3)).isoformat()


class FakeYtClientMonthly:
    def list(self, yt_path):
        return ['2021-11', '2021-12', '2022-01', '2022-03']


class FakeYtClientDaily:
    def list(self, yt_path):
        return ['2022-03-30', '2022-03-31', '2022-04-01', '2022-04-03']


class FakeYtClientLastDays:
    def list(self, yt_path):
        return [PARTITION_3_DAYS_AGO, PARTITION_2_DAYS_AGO, PARTITION_1_DAY_AGO, PARTITION_TODAY]


class DummyYTClient:
    def list(self, yt_path):
        return []


@pytest.fixture
def dummy_yt_client():
    return DummyYTClient()


@pytest.fixture()
def mock_yt_client_monthly():
    return FakeYtClientMonthly()


@pytest.fixture()
def mock_yt_client_daily():
    return FakeYtClientDaily()


@pytest.fixture()
def mock_yt_client_last_days():
    return FakeYtClientLastDays()


def check_parser_test_case(Parser, yt_client, test_case):
    parser = Parser(test_case['expression'])
    assert parser.matches() == test_case['expected_match']
    if 'expected_tables' in test_case:
        actual_tables = parser.get_tables(yt_client)
        assert set(actual_tables) == set(test_case['expected_tables'])


# Test concatYtTablesRange(YT_DIR)
@pytest.mark.parametrize(
    "test_case",
    [
        {
            'expression': "concatYtTablesRange( '//home/some_dir' )",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2021-11',
                '//home/some_dir/2021-12',
                '//home/some_dir/2022-01',
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir')",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2021-11',
                '//home/some_dir/2021-12',
                '//home/some_dir/2022-01',
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange(\"//home/some_dir\")",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2021-11',
                '//home/some_dir/2021-12',
                '//home/some_dir/2022-01',
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange(`//home/some_dir`)",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2021-11',
                '//home/some_dir/2021-12',
                '//home/some_dir/2022-01',
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': """concatYtTablesRange(
                '//home/some_dir'
            )""",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2021-11',
                '//home/some_dir/2021-12',
                '//home/some_dir/2022-01',
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange()",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', ANY_OTHER_ARG)",
            'expected_match': False
        },
    ]
)
def test_all_tables_parser(mock_yt_client_monthly, test_case):
    check_parser_test_case(
        AllTablesParser,
        mock_yt_client_monthly,
        test_case
    )


# Test concatYtTablesRange(YT_DIR, YYYY-MM)
@pytest.mark.parametrize(
    "test_case",
    [
        {
            'expression': "concatYtTablesRange( '//home/some_dir' , '2021-12' )",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2021-12',
                '//home/some_dir/2022-01',
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2021-12')",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2021-12',
                '//home/some_dir/2022-01',
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-01')",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-01',
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02')",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange(\"//home/some_dir\", \"2022-02\")",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange(`//home/some_dir`, `2022-02`)",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': """concatYtTablesRange(
                '//home/some_dir',
                '2022-02'
            )""",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-03'
            ]
        },
        {
            'expression': "concatYtTablesRange()",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', 'YYYY-MM')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02-01')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02-01 00:00:00')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02-01T00:00:00')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02', ANY_OTHER_ARG)",
            'expected_match': False
        },
    ]
)
def test_all_from_month_parser(mock_yt_client_monthly, test_case):
    check_parser_test_case(
        AllFromMonthParser,
        mock_yt_client_monthly,
        test_case
    )


# Test concatYtTablesRange(YT_DIR, YYYY-MM-DD)
@pytest.mark.parametrize(
    "test_case",
    [
        {
            'expression': "concatYtTablesRange( '//home/some_dir' , '2022-03-31' )",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-03-31',
                '//home/some_dir/2022-04-01',
                '//home/some_dir/2022-04-03'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-03-31')",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-03-31',
                '//home/some_dir/2022-04-01',
                '//home/some_dir/2022-04-03'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-04-01')",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-04-01',
                '//home/some_dir/2022-04-03'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-04-02')",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-04-03'
            ]
        },
        {
            'expression': "concatYtTablesRange(\"//home/some_dir\", \"2022-04-02\")",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-04-03'
            ]
        },
        {
            'expression': "concatYtTablesRange(`//home/some_dir`, `2022-04-02`)",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-04-03'
            ]
        },
        {
            'expression': """concatYtTablesRange(
                '//home/some_dir',
                '2022-04-02'
            )""",
            'expected_match': True,
            'expected_tables': [
                '//home/some_dir/2022-04-03'
            ]
        },
        {
            'expression': "concatYtTablesRange()",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-04')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', 'YYYY-MM-DD')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-04-02 00:00:00')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-04-02T00:00:00')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-04-02', ANY_OTHER_ARG)",
            'expected_match': False
        },
    ]
)
def test_all_from_day_parser(mock_yt_client_daily, test_case):
    check_parser_test_case(
        AllFromDayParser,
        mock_yt_client_daily,
        test_case
    )


# Test concatYtTablesRange(YT_DIR, today - DAYS())
@pytest.mark.parametrize(
    "test_case",
    [
        {
            'expression': "concatYtTablesRange( '//home/some_dir' , toString( today() - 2 ) )",
            'expected_match': True,
            'expected_tables': [
                f'//home/some_dir/{PARTITION_2_DAYS_AGO}',
                f'//home/some_dir/{PARTITION_1_DAY_AGO}',
                f'//home/some_dir/{PARTITION_TODAY}'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString(today() - 2))",
            'expected_match': True,
            'expected_tables': [
                f'//home/some_dir/{PARTITION_2_DAYS_AGO}',
                f'//home/some_dir/{PARTITION_1_DAY_AGO}',
                f'//home/some_dir/{PARTITION_TODAY}'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString(today()-2))",
            'expected_match': True,
            'expected_tables': [
                f'//home/some_dir/{PARTITION_2_DAYS_AGO}',
                f'//home/some_dir/{PARTITION_1_DAY_AGO}',
                f'//home/some_dir/{PARTITION_TODAY}'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString(today() - 1))",
            'expected_match': True,
            'expected_tables': [
                f'//home/some_dir/{PARTITION_1_DAY_AGO}',
                f'//home/some_dir/{PARTITION_TODAY}'
            ]
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString(today() - 0))",
            'expected_match': True,
            'expected_tables': [
                f'//home/some_dir/{PARTITION_TODAY}'
            ]
        },
        {
            'expression': "concatYtTablesRange(\"//home/some_dir\", toString(today() - 0))",
            'expected_match': True,
            'expected_tables': [
                f'//home/some_dir/{PARTITION_TODAY}'
            ]
        },
        {
            'expression': "concatYtTablesRange(`//home/some_dir`, toString(today() - 0))",
            'expected_match': True,
            'expected_tables': [
                f'//home/some_dir/{PARTITION_TODAY}'
            ]
        },
        {
            'expression': """concatYtTablesRange(
                '//home/some_dir',
                toString(
                    today() - 0
                )
            )""",
            'expected_match': True,
            'expected_tables': [
                f'//home/some_dir/{PARTITION_TODAY}'
            ]
        },
        {
            'expression': "concatYtTablesRange()",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString())",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', today() - 1)",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString(today())",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString(today() + 1))",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString(today() - DAYS))",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02-01')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02-01 00:00:00')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', '2022-02-01T00:00:00')",
            'expected_match': False
        },
        {
            'expression': "concatYtTablesRange('//home/some_dir', toString(today() - 1), ANY_OTHER_ARG)",
            'expected_match': False
        },
    ]
)
def test_last_x_days_parser(mock_yt_client_last_days, test_case):
    check_parser_test_case(
        LastXDaysParser,
        mock_yt_client_last_days,
        test_case
    )


@pytest.mark.parametrize(
    "test_case",
    [
        {
            "expression": """concatYtTables(
                    "//home/some_dir",
                    '//home/another_dir'
                    ,`//home/last_dir`
                )""",
            "expected_match": True,
            "expected_tables": [
                "//home/some_dir",
                "//home/another_dir",
                "//home/last_dir",
            ]
        },
        {
            "expression": "concatYtTables(\"//home/some_dir\",'//home/another_dir',`//home/last_dir`)",
            "expected_match": True,
            "expected_tables": [
                "//home/some_dir",
                "//home/another_dir",
                "//home/last_dir",
            ]
        },
        {
            "expression": """concatYtTables(
                    "//home/some_dir",
                    "//home/another_dir"
                    ,"//home/last_dir",
                )""",
            "expected_match": False,
        },
        {
            "expression": """concatYtTables(
                    "//home/some_dir"
                )""",
            "expected_match": True,
            "expected_tables": [
                "//home/some_dir",
            ]
        },
        {
            "expression": 'concatYtTables("/home/some_dir")',
            "expected_match": False,
        },
        {
            "expression": "concatYtTables('//home/some_dir')",
            "expected_match": True,
            "expected_tables": [
                "//home/some_dir",
            ]
        },
        {
            "expression": "concatYtTables(`//home/some_dir`)",
            "expected_match": True,
            "expected_tables": [
                "//home/some_dir",
            ]
        },
        {
            "expression": """concatYtTables(
                    "//home/some_dir",
                    '//home/another_dir'
                    ,`//home/last_dir`,
                    "//home/4_dir",
                    "//home/5_dir",
                    "//home/6_dir",
                    "//home/7_dir",
                    "//home/8_dir",
                    "//home/9_dir",
                    "//home/10_dir",
                    "//home/11_dir"
                )""",
            "expected_match": True,
            "expected_tables": [
                "//home/some_dir",
                "//home/another_dir",
                "//home/last_dir",
                "//home/4_dir",
                "//home/5_dir",
                "//home/6_dir",
                "//home/7_dir",
                "//home/8_dir",
                "//home/9_dir",
                "//home/10_dir",
                "//home/11_dir",
            ]
        },
    ]
)
def test_concat_yt_table_parser(dummy_yt_client, test_case):
    check_parser_test_case(
        ConcatYTTablesParser,
        dummy_yt_client,
        test_case
    )

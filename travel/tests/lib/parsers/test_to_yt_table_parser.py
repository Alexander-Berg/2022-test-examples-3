import copy
from datetime import datetime

from travel.avia.country_restrictions.lib.parsers.to_yt_table_parser import ToYtTableParser
from travel.avia.country_restrictions.lib.types import Environment, InformationTable
from travel.avia.country_restrictions.lib.types.metric_type.bool_metric_type import BoolMetricType
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text

from travel.avia.country_restrictions.tests.matrices import n, matrix_to_information_table
from travel.avia.country_restrictions.tests.matrices.parser import generate_test_parser
from travel.avia.country_restrictions.tests.mocks.geo_format_manager import default_mock_geo_format_manager


MOCK_POINT_KEY = 'l1'


def assert_parse_data(old, new, metric_types, result):
    class TestParserWithMockedOldData(ToYtTableParser):
        METRIC_TYPES = metric_types

        def get_initial_data(self) -> InformationTable:
            return old

        def get_data(self, old_data: InformationTable) -> InformationTable:
            return new

        def write_to_yt(self, data: InformationTable):
            assert data == result

    parser = TestParserWithMockedOldData(
        environment=Environment.UNSTABLE,
        version=1,
        solomon_token=None,
        geo_format_manager=default_mock_geo_format_manager,
    )
    parser.run()


def generate_data(metric_type, metric_value, mod_date: datetime):
    metric = metric_type.generate_metric(metric_value)
    metric.updated_time = mod_date
    metric.last_modification_time = mod_date
    return {MOCK_POINT_KEY: {'m': metric}}


def new_bool_metric_type():
    return BoolMetricType(
        name='m',
        title=new_rich_text(''),
        icon24='',
        short_if_no_advanced_info=False,
        true_text='yes',
        false_text='no',
    )


def test_add_new_metric_with_no_initial_data():
    metric_type = new_bool_metric_type()
    new_data = generate_data(metric_type, True, datetime(year=2020, month=1, day=30))

    assert_parse_data(old={}, new=new_data, metric_types=[metric_type], result=new_data)


def test_add_new_metric_with_expired_initial_data():
    metric_type = new_bool_metric_type()

    old_timestamp = datetime(year=2021, month=1, day=1)
    old_data = generate_data(metric_type, True, old_timestamp)

    new_timestamp = datetime(year=2021, month=1, day=30)
    new_data = generate_data(metric_type, False, new_timestamp)

    assert_parse_data(old=old_data, new=new_data, metric_types=[metric_type], result=new_data)


def test_add_new_metric_with_actual_initial_data():
    metric_type = new_bool_metric_type()

    old_timestamp = datetime(year=2021, month=1, day=1)
    old_data = generate_data(metric_type, True, old_timestamp)

    new_timestamp = datetime(year=2021, month=1, day=30)
    new_data = generate_data(metric_type, True, new_timestamp)

    expected = copy.deepcopy(old_data)
    expected[MOCK_POINT_KEY]['m'].updated_time = new_timestamp

    assert_parse_data(old=old_data, new=new_data, metric_types=[metric_type], result=expected)


def test_no_new_metric_with_initial_data():
    metric_type = new_bool_metric_type()

    old_timestamp = datetime(year=2021, month=1, day=1)
    old_data = generate_data(metric_type, True, old_timestamp)
    new_data = {MOCK_POINT_KEY: {}}

    assert_parse_data(old=old_data, new=new_data, metric_types=[metric_type], result=old_data)


def matrix_test(
    initial_matrix,
    new_data_matrix,
    result_matrix,
    solomon_stats,
    solomon_errors,
):
    countries, metric_types, parser = generate_test_parser(
        parser_class=ToYtTableParser,
        source_matrices={},
        initial_data_matrix=initial_matrix,
        result_matrix=result_matrix,
        solomon_stats_values=solomon_stats,
        solomon_error_values=solomon_errors,
    )

    class TestParser(parser):
        def __init__(self):
            super().__init__()

        def get_data(self, old_data):
            return matrix_to_information_table(countries, metric_types, new_data_matrix)

    TestParser().run()


def test_solomon_metrics():
    matrix_test(
        initial_matrix=[
            [0, 0, 0, n],
            [0, n, n, n],
            [0, n, n, n],
            [n, n, n, n],
        ],
        new_data_matrix=[
            [0, n, 1, n],
            [n, n, 1, n],
            [1, 1, n, n],
            [n, n, n, n],
        ],
        result_matrix=[
            [0, 0, 1, n],
            [0, n, 1, n],
            [1, 1, n, n],
            [n, n, n, n],
        ],
        solomon_stats={
            'updated_geo': 3,
            'updated_metrics': 3,
            'updated_cells': 4,
            'null_geo': 1,
            'null_metrics': 1,
            'null_cells': 9,
            'total_geo': 4,
            'total_metrics': 4,
            'total_cells': 16,
        },
        solomon_errors={},
    )

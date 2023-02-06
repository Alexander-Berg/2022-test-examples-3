from typing import Dict, List, Optional

from travel.avia.country_restrictions.lib.types.metric_type.int_metric_type import IntMetricType
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text
from travel.avia.country_restrictions.lib.solomon_pusher import SolomonPusher
from travel.avia.country_restrictions.lib.types import Environment, InformationTable

from travel.avia.country_restrictions.tests.matrices import matrix_to_rows, information_table_to_matrix, GeoPoint
from travel.avia.country_restrictions.tests.mocks.geo_format_manager import default_mock_geo_format_manager
from travel.avia.country_restrictions.tests.mocks.yt_client import MultiTablesMockYtClient


class SolomonMockPusher(SolomonPusher):
    def __init__(self, stats_values: Dict[str, int], errors_dict: Dict[str, int]):
        super().__init__(environment=Environment.TESTING, token='', parser_name='')
        self.stats_values = stats_values
        self.errors_dict = errors_dict

    def push_stats(self, **kwargs):
        if self.stats_values is not None:
            assert kwargs == self.stats_values

    def push_errors(self, errors: Dict[str, int]):
        if self.errors_dict is not None:
            assert self.errors_dict == errors

    def push(self, *args, **kwargs):
        pass


def generate_test_parser(
    parser_class,
    source_matrices: dict,
    initial_data_matrix,
    result_matrix,
    solomon_stats_values: Optional[Dict[str, int]] = None,
    solomon_error_values: Optional[Dict[str, int]] = None,
    geo_points: List[GeoPoint] = None,
    metric_types=None,
    mock_geo_format_manager=None,
):
    if mock_geo_format_manager is None:
        mock_geo_format_manager = default_mock_geo_format_manager

    if geo_points is None:
        geo_points = [GeoPoint(f'l{i}', i) for i in range(len(result_matrix))]

    if metric_types is None:
        metric_count = len(result_matrix[0])
        metric_types = [
            IntMetricType(
                name=f'm{i}',
                title=new_rich_text(''),
                icon24='',
                short_if_no_advanced_info=False,
                prefix='',
                singular_unit='',
                few_units='',
                many_units='',
            )
            for i in range(metric_count)
        ]

    class TestParser(parser_class):
        METRIC_TYPES = metric_types

        def __init__(self):
            super().__init__(
                base_path='/',
                environment=Environment.TESTING,
                version=1,
                solomon_token=None,
                geo_format_manager=mock_geo_format_manager,
            )

            def mock_updating_table_name():
                return '//updating'
            self.get_updating_table_fullname = mock_updating_table_name

            yt_data = {
                '//' + name: matrix_to_rows(geo_points, metric_types, matrix)
                for name, matrix in source_matrices.items()
            }
            yt_data[self.get_updating_table_fullname()] = matrix_to_rows(geo_points, metric_types, initial_data_matrix)
            self.yt_client = MultiTablesMockYtClient(yt_data)
            self.solomon_pusher = SolomonMockPusher(solomon_stats_values, solomon_error_values)

        def write_to_yt(self, data: InformationTable):
            actual = information_table_to_matrix(data, geo_points, metric_types)
            assert actual == result_matrix

    return geo_points, metric_types, TestParser

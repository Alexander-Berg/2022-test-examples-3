from travel.avia.country_restrictions.aggregator.hierarchy_applier import HierarchyApplierParser
from travel.avia.country_restrictions.lib.types import Environment
from travel.avia.country_restrictions.lib.types.metric_type.bool_metric_type import BoolMetricType
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text

from travel.avia.country_restrictions.tests.matrices import n
from travel.avia.country_restrictions.tests.matrices.parser import generate_test_parser, GeoPoint
from travel.avia.country_restrictions.tests.mocks.geo_format_manager import MockGeoFormatManager
from travel.avia.country_restrictions.tests.mocks.yt_client import MultiTablesMockYtClient


def internal_test_parser(source_matrix, result_matrix, geo_points, geo_format_manager):
    countries, metrics, parser_cls = generate_test_parser(
        parser_class=HierarchyApplierParser,
        source_matrices={'testing/v1/combiner-as-columns': source_matrix},
        geo_points=geo_points,
        initial_data_matrix=[],
        result_matrix=result_matrix,
        mock_geo_format_manager=geo_format_manager,
    )
    parser = parser_cls()
    parser.run()


def test_hierarchy_applier_parser_hard():
    internal_test_parser(
        source_matrix=[
            [n, n, n, 0],  # l1
            [n, 1, 1, n],  # r1
            [2, n, 2, 2],  # c1
            [3, n, 3, n],  # c2
            [n, 4, 4, n],  # c3
        ],
        result_matrix=[
            [n, n, n, 0],  # l1: no parents for the country => no changes
            [n, 1, 1, 0],  # r1: m4 is taken from the parent (l1)
            [2, 1, 2, 2],  # c1: m2 is taken from the parent (r1)
            [3, 1, 3, 0],  # c2: m2 is taken from parent (r1), m4 from grand-parent (l1)
            [n, 4, 4, 0],  # c3: m4 from grand-parent (l1), case for testing node with no parent, only grand-parent
        ],
        geo_points=[
            GeoPoint('l1', 1),
            GeoPoint('r1', 2),
            GeoPoint('c1', 3),
            GeoPoint('c2', 4),
            GeoPoint('c3', 5),
        ],
        geo_format_manager=MockGeoFormatManager(
            connections=[
                ('l1', 1),
                ('r1', 2),
                ('c1', 3),
                ('c2', 4),
                ('c3', 5),
            ],
            parents={
                'r1': ['l1'],
                'c1': ['r1', 'l1'],
                'c2': ['r1', 'l1'],
                'c3': ['r2', 'l1'],  # r2 does not exist in matrix!!!
            }
        )
    )


def test_custom_parents():
    internal_test_parser(
        source_matrix=[
            [n, n, n, 0],  # l1
            [n, 1, 1, n],  # r1
            [2, n, 2, n],  # r10572
        ],
        result_matrix=[
            [n, n, n, 0],  # l1
            [n, 1, 1, 0],  # r1
            [2, n, 2, n],  # r10572, the result should not be [2, n, 2, 0], because parent is overwritten
        ],
        geo_points=[
            GeoPoint('l1', 1),
            GeoPoint('r1', 2),
            GeoPoint('r10572', 10572),
        ],
        geo_format_manager=MockGeoFormatManager(
            connections=[
                ('l1', 1),
                ('r1', 2),
                ('r10572', 10572),
            ],
            parents={
                'r1': ['l1'],
                'r10572': ['l1'],  # this should be overwritten
            }
        )
    )


def test_save_parent_point_key():
    metric_type = BoolMetricType(
        name='m',
        title=new_rich_text(''),
        icon24='',
        short_if_no_advanced_info=False,
        true_text='yes',
        false_text='no',
    )
    metric = metric_type.generate_metric(True)

    parser = HierarchyApplierParser(
        base_path='/',
        environment=Environment.TESTING,
        version=1,
        solomon_token=None,
        geo_format_manager=MockGeoFormatManager(
            connections=[('l1', 1), ('r1', 2)],
            parents={'r1': ['l1']}
        ),
    )
    parser.yt_client = MultiTablesMockYtClient({
        '//testing/v1/combiner-as-columns': [
            {
                'point_key': 'l1',
                'key': 1,
                'm': metric,
            },
            {
                'point_key': 'r1',
                'key': 2,
            }
        ]
    })
    res = parser.get_data({})
    assert res['r1']['m'].point_key == 'l1'

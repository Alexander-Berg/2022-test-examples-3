from travel.avia.country_restrictions.aggregator.combiner import CombinerParser
from travel.avia.country_restrictions.lib.types import SourcePriorityInfo

from travel.avia.country_restrictions.tests.matrices import n
from travel.avia.country_restrictions.tests.matrices.parser import generate_test_parser


def internal_test_parser(
    source_priorities,
    source_matrices,
    result_matrix,
):
    countries, metrics, parser_cls = generate_test_parser(
        parser_class=CombinerParser,
        source_matrices=source_matrices,
        initial_data_matrix=[],
        result_matrix=result_matrix,
    )
    parser = parser_cls()
    parser.parsers_priority = source_priorities
    parser.input_sources = {name: '//' + name for name in source_matrices.keys()}

    # test
    parser.run()


def test_full_info_parser():
    internal_test_parser(
        source_priorities=[
            SourcePriorityInfo(source='a', geo=['l0', 'l1'], metrics=['m0', 'm1']),
            SourcePriorityInfo(source='b', geo=['l2']),
            SourcePriorityInfo(source='c', metrics=['m2']),
            SourcePriorityInfo(source='a'),
            SourcePriorityInfo(source='d'),
        ],
        source_matrices={
            'a': [
                [0, n, 0, n],
                [n, 0, n, 0],
                [0, n, 0, 0],
                [n, 0, 0, n],
            ],
            'b': [
                [1, 1, 1, 1],
                [1, 1, 1, 1],
                [1, n, 1, n],
                [1, 1, 1, 1],
            ],
            'c': [
                [2, 2, 2, 2],
                [2, 2, n, 2],
                [2, 2, 2, 2],
                [2, 2, n, 2],
            ],
            'd': [
                [3, 3, 3, 3],
                [3, 3, 3, 3],
                [3, 3, 3, n],
                [3, 3, n, n],
            ],
        },
        result_matrix=[
            [0, 3, 2, 3],
            [3, 0, 3, 0],
            [1, 3, 1, 0],
            [3, 0, 0, n],
        ],
    )


def test_priority_with_null():
    internal_test_parser(
        source_priorities=[
            SourcePriorityInfo(source='high'),
            SourcePriorityInfo(source='low'),
        ],
        source_matrices={
            'high': [[0, n, n]],
            'low': [[1, 1, n]]
        },
        result_matrix=[[0, 1, n]]
    )


def test_priority_with_metric():
    internal_test_parser(
        source_priorities=[
            SourcePriorityInfo(source='high', metrics=['m0']),
            SourcePriorityInfo(source='low'),
        ],
        source_matrices={
            'high': [[0, 0, 0]],
            'low': [[1, 1, n]],
        },
        result_matrix=[[0, 1, n]],
    )


def test_priority_with_geo():
    internal_test_parser(
        source_priorities=[
            SourcePriorityInfo(source='high', geo=['l0']),
            SourcePriorityInfo(source='low'),
        ],
        source_matrices={
            'high': [[0], [0], [0]],
            'low': [[1], [1], [n]],
        },
        result_matrix=[[0], [1], [n]],
    )

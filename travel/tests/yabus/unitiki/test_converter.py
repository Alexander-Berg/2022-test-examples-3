# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from logging import Logger

import hamcrest
import mock
import pytest

from travel.proto.dicts.buses.common_pb2 import EPointKeyType
from travel.buses.connectors.tests.yabus.common.library.test_utils import make_matching_provider, make_point_relations_provider

from yabus.providers import supplier_provider
from yabus.common.exceptions import PointNotFound
from yabus.unitiki.converter import UnitikiPointRelationConverter


class TestUnitikiConverter(object):

    def setup_method(self, _):
        self._supplier_code = supplier_provider.get_by_code("").code

    def test_map_to_single_variant(self):
        fake_logger = mock.Mock(Logger)
        c = UnitikiPointRelationConverter(self._supplier_code, converter_logger=mock.Mock(Logger))
        c.setup(
            point_matchings=make_matching_provider({
                's0': [('s_code_s0', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's1': [('s_code_s1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's2': [('s_code_s2', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's3': [('s_code_s3', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c1': [('c_code_c1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c2': [('c_code_c2', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c4': [('c_code_c4', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c5': [('c_code_c5', EPointKeyType.POINT_KEY_TYPE_STATION),
                       ('s_code_s5', EPointKeyType.POINT_KEY_TYPE_STATION)],
            }),
            relations_provider=make_point_relations_provider({
                'c1': ['s0', 's1', 's2', 's4'],
                'c2': ['s5', 's6'],
                'c3': ['s1'],
                'c4': ['s0'],
                'c5': ['s5'],
            }),
        )

        assert c.map('s1') == frozenset(['s_code_s1', 'c_code_c1'])
        assert c.map('s2') == frozenset(['s_code_s2', 'c_code_c1'])
        assert c.map('s3') == frozenset(['s_code_s3'])
        assert c.map('c1') == frozenset(['c_code_c1'])
        assert c.map('s0') == frozenset(['c_code_c1', 's_code_s0', 'c_code_c4'])
        assert c.map('c5') == frozenset(['c_code_c5'])
        assert fake_logger.warn.call_count == 0

        with pytest.raises(PointNotFound):
            c.map('c3')

        hamcrest.assert_that(
            tuple(c.gen_map_segments([
                ('c_code_c1', 's_code_s3'),
                ('s_code_s3', 'c_code_c2'),
            ])),
            hamcrest.contains_inanyorder(
                ('c1', 's3'),
                ('s0', 's3'),
                ('s1', 's3'),
                ('s2', 's3'),
                ('s3', 'c2'),
            )
        )

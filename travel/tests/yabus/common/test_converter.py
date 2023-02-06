# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from logging import Logger

import hamcrest
import mock
import pytest

from travel.proto.dicts.buses.common_pb2 import EPointKeyType
from travel.buses.connectors.tests.yabus.common.library.test_utils import make_matching_provider, make_point_relations_provider

from yabus.common.pointconverter import PointConverter, PointRelationConverter
from yabus.common.exceptions import PointNotFound
from yabus.providers.supplier_provider import supplier_provider


class TestConverter(object):

    def setup_method(self, _):
        self._supplier_code = supplier_provider.get_by_code("").code

    def test_map_to_single_variant(self):
        fake_logger = mock.Mock(Logger)

        c = PointConverter(self._supplier_code, converter_logger=fake_logger)
        c.setup(point_matchings=make_matching_provider(
            {
                's1': [('supplier1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c2': [('supplier2', EPointKeyType.POINT_KEY_TYPE_SETTLEMENT)],
            }),
        )

        assert c.map('s1') == frozenset(['supplier1'])
        assert c.map('c2') == frozenset(['supplier2'])
        assert fake_logger.warn.call_count == 0

    def test_map_to_many_variant(self):
        fake_logger = mock.Mock(Logger)
        c = PointConverter(self._supplier_code, converter_logger=fake_logger)
        c.setup(point_matchings=make_matching_provider(
            {
                's1': [
                    ('supplier1', EPointKeyType.POINT_KEY_TYPE_STATION),
                    ('supplier2', EPointKeyType.POINT_KEY_TYPE_STATION)
                ],
            }),
        )

        assert c.map('s1') == frozenset(['supplier1', 'supplier2'])
        assert fake_logger.warn.call_count == 0

    def test_map_by_unknown_key(self):
        fake_logger = mock.Mock(Logger)
        c = PointConverter(self._supplier_code, converter_logger=fake_logger)
        c.setup(point_matchings=make_matching_provider(
            {
                's1': [('supplier1', EPointKeyType.POINT_KEY_TYPE_STATION)],
            }),
        )

        with pytest.raises(PointNotFound):
            c.map('unknown')

        assert fake_logger.warn.call_args_list == [
            mock.call('Mapping does not have the key %s', 'unknown'),
        ]

    def test_backmap(self):
        fake_logger = mock.Mock(Logger)
        c = PointConverter(self._supplier_code, converter_logger=fake_logger)
        c.setup(point_matchings=make_matching_provider(
            {
                's1': [('supplier1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's2': [('supplier2', EPointKeyType.POINT_KEY_TYPE_STATION),
                       ('supplier3', EPointKeyType.POINT_KEY_TYPE_STATION)],
            }),
        )

        assert c.backmap('supplier1') == 's1'
        assert c.backmap('supplier2') == 's2'
        assert c.backmap('supplier3') == 's2'

        assert fake_logger.warn.call_count == 0

    def test_backmap_with_duplicates(self):
        fake_logger = mock.Mock(Logger)
        c = PointConverter(self._supplier_code, converter_logger=fake_logger)
        c.setup(point_matchings=make_matching_provider(
            {
                's1': [('supplier1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's2': [('supplier1', EPointKeyType.POINT_KEY_TYPE_STATION)],
            }),
        )

        assert c.backmap('supplier1') in ('s1', 's2')
        assert fake_logger.warn.call_count == 0

    def test_backmap_by_unknown_key(self):
        fake_logger = mock.Mock(Logger)
        c = PointConverter(self._supplier_code, converter_logger=fake_logger)
        c.setup(point_matchings=make_matching_provider(
            {
                's1': [('supplier1', EPointKeyType.POINT_KEY_TYPE_STATION)],
            }),
        )

        with pytest.raises(PointNotFound):
            c.backmap('unknown')

        assert fake_logger.warn.call_args_list == [
            mock.call('Back mapping does not have the key %s', 'unknown'),
        ]

    def test_gen_map_segments(self):
        fake_logger = mock.Mock(Logger)
        c = PointConverter(self._supplier_code, converter_logger=fake_logger)
        c.setup(point_matchings=make_matching_provider(
            {
                'c1': [('supplier1', EPointKeyType.POINT_KEY_TYPE_SETTLEMENT)],
                's2': [('supplier2', EPointKeyType.POINT_KEY_TYPE_STATION),
                       ('supplier3', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c3': [('supplier4', EPointKeyType.POINT_KEY_TYPE_SETTLEMENT)],
            }),
        )
        assert list(c.gen_map_segments([
            ('supplier1', 'supplier2'),
            ('supplier2', 'supplier1'),
            ('supplier3', 'supplier4'),
            ('unknown1', 'supplier4'),
            ('supplier4', 'unknown2'),
        ])) == [
            ('c1', 's2'),
            ('s2', 'c1'),
            ('s2', 'c3'),
        ]

        assert fake_logger.warn.call_args_list == [
            mock.call('Back mapping does not have the key %s', 'unknown1'),
            mock.call('Back mapping does not have the key %s', 'unknown2'),
        ]

    def make_converter(self):
        point_converter = PointConverter(self._supplier_code, converter_logger=mock.Mock(Logger))
        point_converter.setup(
            point_matchings=make_matching_provider({
                's1': [('code_s1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's2': [('code_s2', EPointKeyType.POINT_KEY_TYPE_STATION),
                       ('code_s2_1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's3': [('code_s3', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c1': [('code_c1', EPointKeyType.POINT_KEY_TYPE_SETTLEMENT)],
                's7': [('code_s4', EPointKeyType.POINT_KEY_TYPE_STATION)],
            }),
        )
        return point_converter

    def test_converter(self):
        c = self.make_converter()
        assert c.map('s3') == frozenset(['code_s3'])
        assert c.map('s2') == frozenset(['code_s2', 'code_s2_1'])
        assert c.backmap('code_c1') == 'c1'
        with pytest.raises(PointNotFound):
            c.map('xx')
        with pytest.raises(PointNotFound):
            c.backmap('code_xx')

    def make_relation_converter(self):
        point_relation_converter = PointRelationConverter(self._supplier_code, converter_logger=mock.Mock(Logger))
        point_relation_converter.setup(
            point_matchings=make_matching_provider({
                's1': [('code_s1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's2': [('code_s2', EPointKeyType.POINT_KEY_TYPE_STATION),
                       ('code_s2_1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's3': [('code_s3', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c1': [('code_c1', EPointKeyType.POINT_KEY_TYPE_SETTLEMENT)],
                's7': [('code_s4', EPointKeyType.POINT_KEY_TYPE_STATION)],
            }),
            relations_provider=make_point_relations_provider({
                'c1': ['s1', 's2', 's4'],
                'c2': ['s5', 's6'],
                'c3': ['s1'],
                'c4': ['s7'],
            }),
        )
        return point_relation_converter

    def test_relation_converter(self):
        c = self.make_relation_converter()
        assert c.map('s3') == frozenset(['code_s3'])
        with pytest.raises(PointNotFound):
            c.map('c5')
        assert c.map('s1') == frozenset(['code_s1'])
        assert c.map_to_parents('s1') == frozenset(['code_c1'])
        assert c.map_to_parents('c1') == frozenset([])
        assert c.map_to_parents('s5') == frozenset([])
        assert c.map_to_children('c1') == frozenset(['code_s1', 'code_s2', 'code_s2_1'])
        assert c.map_to_children('s1') == frozenset([])
        assert c.backmap_to_parents('code_s2_1') == frozenset(['c1'])
        assert c.backmap_to_parents('code_s3') == frozenset([])
        assert c.backmap_to_parents('code_c1') == frozenset([])
        assert c.backmap_to_children('code_c1') == frozenset(['s1', 's2'])
        assert c.backmap_to_children('code_s1') == frozenset([])

    def test_deprecated_map(self):
        c = self.make_relation_converter()
        assert c.deprecated_map('s1') == frozenset(['code_s1'])
        assert c.deprecated_map('s2') == frozenset(['code_s2', 'code_s2_1'])
        assert c.deprecated_map('c1') == frozenset(['code_c1', 'code_s1', 'code_s2', 'code_s2_1'])
        assert c.deprecated_map('c3') == frozenset(['code_s1'])
        with pytest.raises(PointNotFound):
            c.deprecated_map('s4')
        hamcrest.assert_that(
            tuple(c.gen_map_segments([
                ('code_s1', 'code_s2'),
                ('code_s3', 'code_c1'),
            ])),
            hamcrest.contains_inanyorder(
                ('c3', 's2'),
                ('s1', 's2'),
                ('s3', 'c1'),
                ('c1', 'c1'),
                ('c3', 'c1'),
                ('c1', 's2'),
                ('s1', 'c1'),
            )
        )

    def test_map_use_relations(self):
        c = self.make_relation_converter()
        with pytest.raises(PointNotFound):
            c.map('c4')
        with pytest.raises(PointNotFound):
            c.map('c5')
        with pytest.raises(PointNotFound):
            c.map('c5', use_relations=True)
        assert c.map('c4', use_relations=True) == frozenset(['code_s4'])
        assert c.map('s2') == frozenset(['code_s2', 'code_s2_1'])
        assert c.map('s1') == frozenset(['code_s1'])
        assert c.map('c1', use_relations=True) == frozenset(['code_c1'])
        assert c.map('c1') == frozenset(['code_c1'])
        assert c.map('c3', use_relations=True) == frozenset(['code_s1'])

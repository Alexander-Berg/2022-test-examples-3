# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.models.factories import create_external_direction
from common.models.geo import Settlement, Station
from common.tester.factories import (
    create_settlement, create_station, create_suburban_zone, create_country
)
from common.tester.testcase import TestCase
from geosearch.views.pointlist import PointList
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    GeosearchResult, GeosearchState
)
from travel.rasp.morda_backend.tests.search.parse_context.geosearch_wrapper.factories import (
    create_testing_input_search_context
)


class TestGeosearchResult(TestCase):
    """
    Тесты на конструктор GeosearchResult
    """
    def test_state_with_errors(self):
        """
        Результат обработки поискового запроса содержит ошибки.
        """
        input_context = create_testing_input_search_context()
        point_from = create_settlement()
        point_to = create_settlement()
        result = self._create_geosearch_result(input_context, point_from, point_to, 'errors')

        assert result.input_context == input_context
        assert result.point_from == point_from
        assert result.point_to == point_to
        assert result.errors == 'errors'

    def test_state_without_errors(self):
        """
        Результат обработки поискового запроса не содержит ошибок.
        """
        input_context = create_testing_input_search_context()
        point_from = create_settlement()
        point_to = create_settlement()
        result = self._create_geosearch_result(input_context, point_from, point_to)

        assert result.input_context == input_context
        assert result.point_from == point_from
        assert result.point_to == point_to
        assert result.errors == []

    def test_same_suburban_zone_no_zones(self):
        input_context = create_testing_input_search_context()
        point_from = create_settlement()
        point_to = create_settlement()
        result = self._create_geosearch_result(input_context, point_from, point_to)

        assert not result.same_suburban_zone()

    @mock.patch.object(Settlement, 'get_externaldirections')
    @mock.patch.object(Station, 'get_externaldirections')
    def test_same_suburban_zone(self, m_station_get_externaldirections, m_settlement_get_externaldirections):
        input_context = create_testing_input_search_context()

        zone1 = create_suburban_zone(id=1001, code='a', settlement_id=Settlement.MOSCOW_ID)
        zone2 = create_suburban_zone(id=1002, code='b', settlement_id=Settlement.MOSCOW_ID)
        zone3 = create_suburban_zone(id=1003, code='c', settlement_id=Settlement.MOSCOW_ID)

        m_station_get_externaldirections.return_value = [
            create_external_direction(suburban_zone=zone1),
            create_external_direction(suburban_zone=zone2)
        ]

        m_settlement_get_externaldirections.return_value = [
            create_external_direction(suburban_zone=zone2),
            create_external_direction(suburban_zone=zone3)
        ]

        point_from = create_station()
        point_to = create_settlement()
        result = self._create_geosearch_result(input_context, point_from, point_to)

        assert result.same_suburban_zone()

    @mock.patch.object(Settlement, 'get_externaldirections')
    @mock.patch.object(Station, 'get_externaldirections')
    def test_same_suburban_zone_with_errors(self, m_station_get_externaldirections,
                                            m_settlement_get_externaldirections):
        input_context = create_testing_input_search_context()

        zone1 = create_suburban_zone(id=1001, code='a', settlement_id=Settlement.MOSCOW_ID)
        zone2 = create_suburban_zone(id=1002, code='b', settlement_id=Settlement.MOSCOW_ID)
        zone3 = create_suburban_zone(id=1003, code='c', settlement_id=Settlement.MOSCOW_ID)

        m_station_get_externaldirections.return_value = [
            create_external_direction(suburban_zone=zone1),
            create_external_direction(suburban_zone=zone2)
        ]

        m_settlement_get_externaldirections.return_value = [
            create_external_direction(suburban_zone=zone2),
            create_external_direction(suburban_zone=zone3)
        ]

        point_from = create_station()
        point_to = create_settlement()
        result = self._create_geosearch_result(input_context, point_from, point_to, 'errors')

        assert not result.same_suburban_zone()

    @mock.patch.object(Settlement, 'get_externaldirections')
    @mock.patch.object(Station, 'get_externaldirections')
    def test_not_same_suburban_zone(self, m_station_get_externaldirections, m_settlement_get_externaldirections):
        input_context = create_testing_input_search_context()

        zone1 = create_suburban_zone(id=1001, code='a', settlement_id=Settlement.MOSCOW_ID)
        zone2 = create_suburban_zone(id=1002, code='b', settlement_id=Settlement.MOSCOW_ID)
        zone3 = create_suburban_zone(id=1003, code='c', settlement_id=Settlement.MOSCOW_ID)
        zone4 = create_suburban_zone(id=1004, code='d', settlement_id=Settlement.MOSCOW_ID)

        m_station_get_externaldirections.return_value = [
            create_external_direction(suburban_zone=zone1),
            create_external_direction(suburban_zone=zone2)
        ]

        m_settlement_get_externaldirections.return_value = [
            create_external_direction(suburban_zone=zone3),
            create_external_direction(suburban_zone=zone4)
        ]

        point_from = create_station()
        point_to = create_settlement()
        result = self._create_geosearch_result(input_context, point_from, point_to)
        assert not result.same_suburban_zone()

    def test_suburban_zone_if_some_points_are_country(self):
        point_from = create_country()
        point_to = create_settlement()
        result = self._create_geosearch_result(None, point_from, point_to)
        assert not result.same_suburban_zone()

        point_to = create_country()
        point_from = create_settlement()
        result = self._create_geosearch_result(None, point_from, point_to)
        assert not result.same_suburban_zone()

    @staticmethod
    def _create_geosearch_result(input_context=None, point_from=None, point_to=None, errors=None):
        state = GeosearchState(input_context, PointList(point_from), PointList(point_to), errors)
        return GeosearchResult(state, None)


@pytest.mark.parametrize("errors,expected", [
    (
        [{'type': 'ambiguous'}, {'type': 'another_error'}],
        False
    ),
    (
        [{'type': 'same_points'}, {'type': 'another_error'}],
        True
    ),
    (
        [],
        True
    )
])
def test_no_ambiguous_errors(errors, expected):
    result = GeosearchResult(GeosearchState(None, None, None, errors), None)
    assert expected == result.no_ambiguous_errors()

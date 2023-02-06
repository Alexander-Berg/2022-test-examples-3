# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from geosearch.views.pointlist import PointList
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    GeosearchState, check_same_points
)
from travel.rasp.morda_backend.tests.search.parse_context.geosearch_wrapper.factories import (
    create_testing_input_search_context
)


class TestCheckSamePoints(TestCase):
    def setUp(self):
        super(TestCheckSamePoints, self).setUp()
        self.input_context = create_testing_input_search_context()

    def test_different_points(self):
        """
        Разные пункты отправления и прибытия.
        """
        point_from = create_settlement(id=555)
        point_list_from = PointList(point_from)

        point_to = create_settlement(id=666)
        point_list_to = PointList(point_to)

        initial_state = GeosearchState(self.input_context, point_list_from, point_list_to)
        state = check_same_points(initial_state)

        assert state.input_context == self.input_context
        assert state.point_list_from == point_list_from
        assert state.point_list_to == point_list_to
        assert state.errors is None

    def test_same_points(self):
        """
        Пункты отправления и прибытия совпадают.
        """
        point = create_settlement(id=555)
        point_list_from = PointList(point)
        point_list_to = PointList(point)

        initial_state = GeosearchState(self.input_context, point_list_from, point_list_to)
        state = check_same_points(initial_state)

        assert state.input_context == self.input_context
        assert state.point_list_from == point_list_from
        assert state.point_list_to == point_list_to
        assert state.errors == [
            {'fields': ['from', 'to'], 'type': 'same_points'}
        ]

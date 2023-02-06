# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from geosearch.views.pointlist import PointList
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import GeosearchState
from travel.rasp.morda_backend.tests.search.parse_context.geosearch_wrapper.factories import (
    create_testing_input_search_context
)


class TestGeosearchState(TestCase):
    """
    Тесты на конструктор GeosearchState
    """
    def setUp(self):
        super(TestGeosearchState, self).setUp()
        self.input_context = create_testing_input_search_context()

    def test_state_with_point_lists_without_errors(self):
        """
        State содержит PointList'ы для пунктов отправления, прибытия, не содержит ошибок.
        """
        point_list_from = PointList(create_settlement())
        point_list_to = PointList(create_settlement())
        state = GeosearchState(self.input_context, point_list_from, point_list_to)

        assert state.input_context == self.input_context
        assert state.point_list_from == point_list_from
        assert state.point_list_to == point_list_to
        assert state.errors is None

    def test_state_without_point_lists_with_errors(self):
        """
        State содержит ошибки.
        """
        state = GeosearchState(self.input_context, errors='errors')

        assert state.input_context == self.input_context
        assert state.point_list_from is None
        assert state.point_list_to is None
        assert state.errors == 'errors'

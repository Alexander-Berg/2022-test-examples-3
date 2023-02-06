# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from geosearch.views.pointtopoint import SamePointError
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    GeosearchState, process_point_lists, InputSearchContext
)
from travel.rasp.morda_backend.tests.search.parse_context.geosearch_wrapper.factories import create_testing_point_list


class TestProcessPointLists(TestCase):
    """
    Тесты на фильтрацию PointList'ов.
    """
    def setUp(self):
        super(TestProcessPointLists, self).setUp()
        self.create_settlement_id = 999
        self.client_settlement = create_settlement(id=self.create_settlement_id)
        self.initial_point_list_from = create_testing_point_list()
        self.initial_point_list_to = create_testing_point_list()

    def test_process_train_without_errors(self):
        """
        Фильтрация PointList'ов при поиске "тип транспорта - поезд", ошибок не возникло.
        В функцию process_points_lists из сабмодуля geosearch передается параметр suburban=False
        """
        input_context = InputSearchContext(None, None, None, None, 'train', self.create_settlement_id, None, None)
        initial_state = GeosearchState(input_context, self.initial_point_list_from, self.initial_point_list_to)

        point_list_from = create_testing_point_list()
        point_list_to = create_testing_point_list()

        with mock.patch('geosearch.views.pointtopoint.process_points_lists',
                        return_value=(point_list_from, point_list_to)) as m_process_points_lists:
            state = process_point_lists(initial_state)

            assert state.input_context == input_context
            assert state.point_list_from == point_list_from
            assert state.point_list_to == point_list_to
            assert state.errors is None

        self._assert_process_points_lists_mock(m_process_points_lists, suburban=False)

    def test_process_any_transport_type_without_errors(self):
        """
        Фильтрация PointList'ов при поиске "тип транспорта - любой", ошибок не возникло.
        В функцию process_points_lists из сабмодуля geosearch передается параметр suburban=False
        """
        input_context = InputSearchContext(None, None, None, None, None, self.create_settlement_id, None, None)
        initial_state = GeosearchState(input_context, self.initial_point_list_from, self.initial_point_list_to)

        point_list_from = create_testing_point_list()
        point_list_to = create_testing_point_list()

        with mock.patch('geosearch.views.pointtopoint.process_points_lists',
                        return_value=(point_list_from, point_list_to)) as m_process_points_lists:
            state = process_point_lists(initial_state)

            assert state.input_context == input_context
            assert state.point_list_from == point_list_from
            assert state.point_list_to == point_list_to
            assert state.errors is None

        self._assert_process_points_lists_mock(m_process_points_lists, suburban=False)

    def test_process_train_with_error(self):
        """
        Фильтрация PointList'ов при поиске "тип транспорта - поезд", обнаружены ошибки.
        В функцию process_points_lists из сабмодуля geosearch передается параметр suburban=False
        """
        input_context = InputSearchContext(None, None, None, None, 'train', self.create_settlement_id, None, None)
        initial_state = GeosearchState(input_context, self.initial_point_list_from, self.initial_point_list_to)

        p_process_points_lists = mock.patch(
            'geosearch.views.pointtopoint.process_points_lists',
            side_effect=SamePointError(self.initial_point_list_from, self.initial_point_list_to))
        with p_process_points_lists as m_process_points_lists:
            state = process_point_lists(initial_state)

            assert state.input_context == input_context
            assert state.point_list_from == self.initial_point_list_from
            assert state.point_list_to == self.initial_point_list_to
            assert state.errors == [{'fields': ['from', 'to'], 'type': 'same_points'}]

        self._assert_process_points_lists_mock(m_process_points_lists, suburban=False)

    def test_process_suburban_without_errors(self):
        """
        Фильтрация PointList'ов при поиске "тип транспорта - электричка", ошибок не возникло.
        В функцию process_points_lists из сабмодуля geosearch передается параметр suburban=True
        """
        input_context = InputSearchContext(None, None, None, None, 'suburban', self.create_settlement_id, None, None)
        initial_state = GeosearchState(input_context, self.initial_point_list_from, self.initial_point_list_to)

        point_list_from = create_testing_point_list()
        point_list_to = create_testing_point_list()

        with mock.patch('geosearch.views.pointtopoint.process_points_lists',
                        return_value=(point_list_from, point_list_to)) as m_process_points_lists:
            state = process_point_lists(initial_state)

            assert state.input_context == input_context
            assert state.point_list_from == point_list_from
            assert state.point_list_to == point_list_to
            assert state.errors is None

        self._assert_process_points_lists_mock(m_process_points_lists, suburban=True)

    def _assert_process_points_lists_mock(self, m_process_points_lists, suburban):
        m_process_points_lists.assert_called_once_with(self.initial_point_list_from, self.initial_point_list_to,
                                                       client_city=self.client_settlement,
                                                       suburban=suburban,
                                                       disable_reduce_from=False, disable_reduce_to=False)

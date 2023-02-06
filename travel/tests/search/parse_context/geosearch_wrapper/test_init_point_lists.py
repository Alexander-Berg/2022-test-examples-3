# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models.transport import TransportType
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    InputSearchContext, GeosearchState, init_point_lists
)
from travel.rasp.morda_backend.tests.search.parse_context.geosearch_wrapper.factories import create_testing_point_list


class TestInitPointLists(TestCase):
    """
    Тесты на получение первичных PointList'ов для пунктов отправления, прибытия.
    """
    def setUp(self):
        super(TestInitPointLists, self).setUp()
        self.from_key = 'from key'
        self.from_title = 'from title'
        self.from_slug = 'from slug'
        self.to_key = 'to key'
        self.to_title = 'to title'
        self.to_slug = 'to slug'
        self.t_type_code = 'train'
        self.t_type = TransportType.objects.get(id=TransportType.TRAIN_ID)
        self.national_version = 'ua'
        self.language = 'uk'

    def test_init_point_lists_without_errors(self):
        """
        PointList'ы успешно найдены и для пункта отправения, и для пункта прибытия.
        """
        input_context = InputSearchContext(self.from_key, self.from_title, self.to_key, self.to_title,
                                           self.t_type_code, None, self.national_version, self.language,
                                           from_slug=self.from_slug, to_slug=self.to_slug)
        initial_state = GeosearchState(input_context)

        point_list_from = create_testing_point_list()
        point_list_to = create_testing_point_list()

        def safe_get_point_list_side_effect(key, title, t_type, slug):
            return (point_list_from, None) if key == self.from_key else (point_list_to, None)

        p_safe_get_point_list = mock.patch(
            'travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper.safe_get_point_list',
            side_effect=safe_get_point_list_side_effect)

        with p_safe_get_point_list as m_safe_get_point_list:
            state = init_point_lists(initial_state)

            assert state.input_context == input_context
            assert state.point_list_from == point_list_from
            assert state.point_list_to == point_list_to
            assert state.errors is None

        self._assert_safe_get_point_list_mock(m_safe_get_point_list)

    def test_init_point_lists_with_from_error(self):
        """
        Для пункта отправления PointList не найден, для пункат отправления найден.
        """
        input_context = InputSearchContext(self.from_key, self.from_title, self.to_key, self.to_title,
                                           self.t_type_code, None, self.national_version, self.language,
                                           from_slug=self.from_slug, to_slug=self.to_slug)
        initial_state = GeosearchState(input_context)

        point_list_to = create_testing_point_list()

        def safe_get_point_list_side_effect(key, title, t_type, slug):
            return (None, 'from_error') if key == self.from_key else (point_list_to, None)

        p_safe_get_point_list = mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper.safe_get_point_list',
                                           side_effect=safe_get_point_list_side_effect)
        with p_safe_get_point_list as m_safe_get_point_list:
            state = init_point_lists(initial_state)

            self._assert_state_with_errors(state, input_context, [
                {'fields': ['from'], 'type': 'from_error'}
            ], point_list_to=point_list_to)

        self._assert_safe_get_point_list_mock(m_safe_get_point_list)

    def test_init_point_lists_with_to_error(self):
        """
        Для пункта отправления PointList найден, для пункат отправления не найден.
        """
        input_context = InputSearchContext(self.from_key, self.from_title, self.to_key, self.to_title,
                                           self.t_type_code, None, self.national_version, self.language,
                                           from_slug=self.from_slug, to_slug=self.to_slug)
        initial_state = GeosearchState(input_context)

        point_list_from = create_testing_point_list()

        def safe_get_point_list_side_effect(key, title, t_type, slug):
            return (point_list_from, None) if key == self.from_key else (None, 'to_error')

        p_safe_get_point_list = mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper.safe_get_point_list',
                                           side_effect=safe_get_point_list_side_effect)
        with p_safe_get_point_list as m_safe_get_point_list:
            state = init_point_lists(initial_state)

            self._assert_state_with_errors(state, input_context, [
                {'fields': ['to'], 'type': 'to_error'}
            ], point_list_from=point_list_from)

        self._assert_safe_get_point_list_mock(m_safe_get_point_list)

    def test_init_point_lists_with_from_and_to_errors(self):
        """
        PointList'ы не найдены ни для пункта отправения, ни для пункта прибытия.
        """
        input_context = InputSearchContext(self.from_key, self.from_title, self.to_key, self.to_title,
                                           self.t_type_code, None, self.national_version, self.language,
                                           from_slug=self.from_slug, to_slug=self.to_slug)
        initial_state = GeosearchState(input_context)

        def safe_get_point_list_side_effect(key, title, t_type, slug):
            return (None, 'from_error') if key == self.from_key else (None, 'to_error')

        p_safe_get_point_list = mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper.safe_get_point_list',
                                           side_effect=safe_get_point_list_side_effect)
        with p_safe_get_point_list as m_safe_get_point_list:
            state = init_point_lists(initial_state)

            self._assert_state_with_errors(state, input_context, [
                {'fields': ['from'], 'type': 'from_error'},
                {'fields': ['to'], 'type': 'to_error'}
            ])

        self._assert_safe_get_point_list_mock(m_safe_get_point_list)

    def _assert_state_with_errors(self, state, input_context, errors, point_list_from=None, point_list_to=None):
        assert state.input_context == input_context
        assert state.point_list_from == point_list_from
        assert state.point_list_to == point_list_to
        assert state.errors == errors

    def _assert_safe_get_point_list_mock(self, m_safe_get_point_list):
        m_safe_get_point_list.assert_has_calls([
            mock.call(self.from_key, self.from_title, self.t_type, self.from_slug),
            mock.call(self.to_key, self.to_title, self.t_type, self.to_slug)
        ])

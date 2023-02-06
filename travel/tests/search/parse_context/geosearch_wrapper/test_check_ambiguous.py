# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from geosearch.views.pointlist import PointList
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    GeosearchState, check_ambiguous, InputSearchContext, AmbiguousVariant
)


class TestCheckAmbiguous(TestCase):
    def setUp(self):
        super(TestCheckAmbiguous, self).setUp()
        self.from_title = 'from title'
        self.to_title = 'to title'
        self.national_version = 'ua'
        self.language = 'uk'
        self.client_settlement_id = 789
        self.client_settlement = create_settlement(id=self.client_settlement_id)
        self.input_context = InputSearchContext('from key', self.from_title, 'to key', self.to_title, None,
                                                self.client_settlement_id, self.national_version, self.language)

        self.point_from_id = 111
        self.point_list_from = PointList(create_settlement(id=self.point_from_id))

        self.point_to_id = 222
        self.point_list_to = PointList(create_settlement(id=self.point_to_id))

        self.from_variant1 = create_settlement()
        self.from_variant2 = create_settlement()
        self.to_variant1 = create_settlement()
        self.to_variant2 = create_settlement()

    def test_from_ambiguous(self):
        """
        Не удалось однозначно определить пункт отправления.
        """
        # Инициализируем поле variants списком из любых двух элементов -
        # это обеспечит условие point_list_from.has_variants() == True
        self.point_list_from.variants = [self.from_variant1, self.from_variant2]
        initial_state = GeosearchState(self.input_context, self.point_list_from, self.point_list_to)

        from_variants = [AmbiguousVariant(self.from_variant1, False), AmbiguousVariant(self.from_variant2, False)]
        p_get_ambiguous_variants = mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper.get_ambiguous_variants',
                                              return_value=from_variants)
        p_ambiguous_variants_json = mock.patch(
            'travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.ambiguous_variants_json',
            return_value='from variants json')

        with p_get_ambiguous_variants as m_get_ambiguous_variants, \
                p_ambiguous_variants_json as m_ambiguous_variants_json:
            state = check_ambiguous(initial_state)

            assert state.input_context == self.input_context
            assert state.point_list_from == self.point_list_from
            assert state.point_list_to == self.point_list_to
            assert state.errors == [
                {'fields': ['from'], 'type': 'ambiguous', 'variants': 'from variants json'}
            ]

        m_get_ambiguous_variants.assert_called_once_with(self.point_list_from, self.from_title,
                                                         self.client_settlement, self.national_version)

        m_ambiguous_variants_json.assert_called_once_with(from_variants, self.national_version, self.language)

    def test_to_ambiguous(self):
        """
        Не удалось однозначно определить пункт прибытия.
        """
        # Инициализируем поле variants списком из любых двух элементов -
        # это обеспечит условие point_list_to.has_variants() == True
        self.point_list_to.variants = [self.to_variant1, self.to_variant2]
        initial_state = GeosearchState(self.input_context, self.point_list_from, self.point_list_to)

        to_variants = [AmbiguousVariant(self.to_variant1, False), AmbiguousVariant(self.to_variant2, False)]
        p_get_ambiguous_variants = mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper.get_ambiguous_variants',
                                              return_value=to_variants)
        p_ambiguous_variants_json = mock.patch(
            'travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.ambiguous_variants_json',
            return_value='to variants json')

        with p_get_ambiguous_variants as m_get_ambiguous_variants, \
                p_ambiguous_variants_json as m_ambiguous_variants_json:
            state = check_ambiguous(initial_state)

            assert state.input_context == self.input_context
            assert state.point_list_from == self.point_list_from
            assert state.point_list_to == self.point_list_to
            assert state.errors == [
                {'fields': ['to'], 'type': 'ambiguous', 'variants': 'to variants json'}
            ]

        m_get_ambiguous_variants.assert_called_once_with(self.point_list_to, self.to_title,
                                                         self.client_settlement, self.national_version)

        m_ambiguous_variants_json.assert_called_once_with(to_variants, self.national_version, self.language)

    def test_from_and_to_ambiguous(self):
        """
        Не удалось однозначно определить ни пункт отправления, ни пункт прибытия.
        """
        self.point_list_from.variants = [self.from_variant1, self.from_variant2]
        self.point_list_to.variants = [self.to_variant1, self.to_variant2]
        initial_state = GeosearchState(self.input_context, self.point_list_from, self.point_list_to)

        from_variants = [AmbiguousVariant(self.from_variant1, False), AmbiguousVariant(self.from_variant2, False)]
        to_variants = [AmbiguousVariant(self.to_variant1, False), AmbiguousVariant(self.to_variant2, False)]
        p_get_ambiguous_variants = mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper.get_ambiguous_variants',
                                              side_effect=[from_variants, to_variants])
        p_ambiguous_variants_json = mock.patch(
            'travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.ambiguous_variants_json',
            side_effect=['from variants json', 'to variants json'])

        with p_get_ambiguous_variants as m_get_ambiguous_variants, \
                p_ambiguous_variants_json as m_ambiguous_variants_json:
            state = check_ambiguous(initial_state)

            assert state.input_context == self.input_context
            assert state.point_list_from == self.point_list_from
            assert state.point_list_to == self.point_list_to
            assert state.errors == [
                {'fields': ['from'], 'type': 'ambiguous', 'variants': 'from variants json'},
                {'fields': ['to'], 'type': 'ambiguous', 'variants': 'to variants json'}
            ]

        m_get_ambiguous_variants.assert_has_calls([
            mock.call(self.point_list_from, self.from_title, self.client_settlement, self.national_version),
            mock.call(self.point_list_to, self.to_title, self.client_settlement, self.national_version)
        ])

        m_ambiguous_variants_json.assert_has_calls([
            mock.call(from_variants, self.national_version, self.language),
            mock.call(to_variants, self.national_version, self.language)
        ])

    def test_without_ambiguous(self):
        """
        Однозначно определены пункты отправления и прибытия.
        """
        initial_state = GeosearchState(self.input_context, self.point_list_from, self.point_list_to)

        state = check_ambiguous(initial_state)

        assert state.input_context == self.input_context
        assert state.point_list_from == self.point_list_from
        assert state.point_list_to == self.point_list_to
        assert state.errors is None

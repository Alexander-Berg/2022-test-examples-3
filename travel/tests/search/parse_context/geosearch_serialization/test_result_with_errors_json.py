# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    InputSearchContext, GeosearchResult, GeosearchState
)
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization import result_with_errors_json


class TestResultWithErrorsJson(TestCase):
    """
    Тесты на JSON-сериализацию результата разбора поискового контекста, содержащего ошибки.
    """
    def test_result_with_errors_json(self):
        from_original_key = 'from - original key'
        from_original_title = 'from - original title'
        from_original_slug = 'from - original slug'
        to_original_key = 'to - original key'
        to_original_title = 'to - original title'
        to_original_slug = 'to - original slug'
        input_context = InputSearchContext(from_original_key, from_original_title, to_original_key, to_original_title,
                                           None, None, None, None, from_slug=from_original_slug,
                                           to_slug=to_original_slug)
        geosearch_result = GeosearchResult(GeosearchState(input_context, errors='context errors'), input_context)

        p_result_with_errors_point_json = mock.patch(
            'travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.result_with_errors_point_json',
            side_effect=lambda title, key, slug: '{}, {}, {}'.format(title, key, slug))

        p_transport_type_json = mock.patch(
            'travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.input_context_transport_type_json',
            return_value='train')

        with p_result_with_errors_point_json as m_result_with_errors_point_json, \
                p_transport_type_json as m_transport_type_json:
            assert result_with_errors_json(geosearch_result) == {
                'transportType': 'train',
                'from': 'from - original title, from - original key, from - original slug',
                'originalFrom': 'from - original title, from - original key, from - original slug',
                'to': 'to - original title, to - original key, to - original slug',
                'originalTo': 'to - original title, to - original key, to - original slug',
                'errors': 'context errors'
            }
        m_result_with_errors_point_json.assert_has_calls([
            mock.call(from_original_title, from_original_key, from_original_slug),
            mock.call(to_original_title, to_original_key, to_original_slug)
        ])

        m_transport_type_json.assert_called_once_with(input_context)

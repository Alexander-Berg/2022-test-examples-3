# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from hamcrest import assert_that, has_entries

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from geosearch.views.pointlist import PointList
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    InputSearchContext, GeosearchResult, GeosearchState
)
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization import (
    result_without_errors_json, result_without_errors_point_json
)


@mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.result_without_errors_point_json',
            side_effect=result_without_errors_point_json)
@mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.input_context_transport_type_json',
            return_value='train')
class TestResultWithoutErrorsJson(TestCase):
    """
    Тесты на JSON-сериализацию результата разбора поискового контекста, не содержащего ошибки.
    """
    def test_result_without_errors_json(self, m_transport_type_json, m_point_json):
        from_id = 777
        from_settlement = create_settlement(id=from_id, slug='slug_777')

        to_id = 888
        to_settlement = create_settlement(id=to_id)
        to_settlement.slug = None

        language = 'uk'

        input_context = InputSearchContext(None, None, None, None, None, None, None, language)
        geosearch_result = GeosearchResult(
            GeosearchState(input_context, PointList(from_settlement), PointList(to_settlement)),
            None
        )

        assert_that(result_without_errors_json(geosearch_result), has_entries({
            'transportType': 'train',
            'from': has_entries({
                'key': 'c777',
                'slug': 'slug_777'
            }),
            'to': has_entries({
                'key': 'c888',
                'slug': None
            }),
            'errors': [],
            'sameSuburbanZone': False
        }))

        m_point_json.assert_has_calls([
            mock.call(from_settlement, language),
            mock.call(to_settlement, language)
        ])

        m_transport_type_json.assert_called_once_with(input_context)

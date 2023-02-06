# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import InputSearchContext
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization import (
    input_context_transport_type_json
)


class TestInputContextTransportTypeJson(TestCase):
    """
    Тесты на JSON-сериализацию типа транспорта из поискового контекста.
    """
    def test_train(self):
        input_context = InputSearchContext(None, None, None, None, 'train', None, None, None)
        assert input_context_transport_type_json(input_context) == 'train'

    def test_all(self):
        input_context = InputSearchContext(None, None, None, None, 'all', None, None, None)
        assert input_context_transport_type_json(input_context) == 'all'

    def test_default_transport_type(self):
        input_context = InputSearchContext(None, None, None, None, None, None, None, None)
        assert input_context_transport_type_json(input_context) == 'all'

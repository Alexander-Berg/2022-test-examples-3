# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    InputSearchContext, TransportTypeNotFoundError, ClientSettlementNotFoundError)


class TestInputSearchContext(TestCase):
    """
    Тесты на конструктор InputSearchContext.
    """
    def setUp(self):
        super(TestInputSearchContext, self).setUp()
        self.from_key = 'from key'
        self.from_title = 'from title'
        self.to_key = 'to key'
        self.to_title = 'to title'
        self.t_type_code = 'train'
        self.national_version = 'ua'
        self.language = 'uk'

    def test_context_with_transport_type_and_client_settlement(self):
        """
        Контекст содержит тип транспорта и город клиента.
        """
        client_settlement_id = 999
        client_settlement = create_settlement(id=client_settlement_id)

        input_context = InputSearchContext(self.from_key, self.from_title, self.to_key, self.to_title,
                                           'train', client_settlement_id, self.national_version, self.language)

        assert input_context.from_key == self.from_key
        assert input_context.from_title == self.from_title
        assert input_context.to_key == self.to_key
        assert input_context.t_type == TransportType.objects.get(id=TransportType.TRAIN_ID)
        assert input_context.client_settlement == client_settlement
        assert input_context.national_version == self.national_version
        assert input_context.language == self.language

    def test_context_without_transport_type_and_client_settlement(self):
        """
        Контекст не содержит типа транспорта и города клиента.
        """
        input_context = InputSearchContext(self.from_key, self.from_title, self.to_key, self.to_title, None, None,
                                           self.national_version, self.language)

        assert input_context.from_key == self.from_key
        assert input_context.from_title == self.from_title
        assert input_context.to_key == self.to_key
        assert input_context.t_type is None
        assert input_context.client_settlement is None
        assert input_context.national_version == self.national_version
        assert input_context.language == self.language

    def test_transport_type_not_found(self):
        with pytest.raises(TransportTypeNotFoundError):
            InputSearchContext(self.from_key, self.from_title, self.to_key, self.to_title, 'Tesla Model X', None,
                               self.national_version, self.language)

    def test_client_settlement_not_found(self):
        with pytest.raises(ClientSettlementNotFoundError):
            InputSearchContext(self.from_key, self.from_title, self.to_key, self.to_title, 'train', 42,
                               self.national_version, self.language)

# -*- coding: utf-8 -*-
from __future__ import absolute_import

from mock import patch

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.transport_models import TransportModelRepository


class TestTransportModelRepository(TestCase):
    def setUp(self):
        with patch.object(TransportModelRepository, '_load_models') as load_models_mock:
            load_models_mock.return_value = get_models()
            self._repository = TransportModelRepository()
            self._repository.pre_cache()

    def test_get_by_code_en_none(self):
        result = self._repository.get_by_code_en('CR1')
        assert result is None

    def test_get_by_code_en(self):
        result = self._repository.get_by_code_en('CE1')
        assert result.code_en == 'CE1'
        assert result.code == 'CR1'

    def test_get_all(self):
        result = self._repository.get_all()
        assert len(result) == 2
        for d in result:
            assert d.code_en in ('CE1', 'CE2')
            assert d.code in ('CR1', 'CR2')


def get_models():
    # type: () -> list
    t1 = {
        'code': 'CR1',
        'code_en': 'CE1',
        'title': 'Заголовок1',
        'title_en': 'title1',
        'is_cargo': False,
        'is_propeller_flight': False,
        'plane_body_type': 'standard',
        'producer__title': 'Airbus',
    }
    t2 = {
        'code': 'CR2',
        'code_en': 'CE2',
        'title': 'Заголовок2',
        'title_en': 'title2',
        'is_cargo': True,
        'is_propeller_flight': True,
        'plane_body_type': 'standard',
        'producer__title': 'Airbus',
    }

    return [t1, t2]

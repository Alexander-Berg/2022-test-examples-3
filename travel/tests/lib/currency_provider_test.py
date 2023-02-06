from logging import Logger

import requests_mock
from mock import Mock
from typing import cast
from unittest import TestCase

from travel.avia.price_index.lib.currency_provider import CurrencyProvider
from travel.avia.price_index.lib.national_version_provider import NationalVersionProvider, NationalVersionModel
from travel.avia.price_index.lib.settings import Settings


class CurrencyProviderTest(TestCase):
    def setUp(self):
        settings = Settings()
        settings.BACKEND_HOST = 'http://mock-avia-backend-host.yandex-team.ru'
        self._fake_national_version_provider = Mock()
        self._provider = CurrencyProvider(
            settings=settings,
            national_version_provider=cast(NationalVersionProvider, self._fake_national_version_provider),
            logger=cast(Logger, Mock()),
        )

        self._nv_model = NationalVersionModel(pk=13, code='nv_code')
        self._other_nv_model = NationalVersionModel(pk=130, code='other_nv_code')

        self._fake_national_version_provider.get_all = Mock(return_value=[self._nv_model, self._other_nv_model])

    def test_fetch(self):
        with requests_mock.mock() as m:
            m.get(
                'http://mock-avia-backend-host.yandex-team.ru/rest/currencies/nv_code/ru',
                json={
                    'status': 'ok',
                    'data': [
                        {
                            'id': 101,
                            'code': 'One',
                        },
                        {
                            'id': 102,
                            'code': 'Two',
                        },
                    ],
                },
            )

            m.get(
                'http://mock-avia-backend-host.yandex-team.ru/rest/currencies/other_nv_code/ru',
                json={
                    'status': 'ok',
                    'data': [
                        {
                            'id': 103,
                            'code': 'Three',
                        }
                    ],
                },
            )

            self._provider.fetch()
            assert len(self._provider.get_all(self._nv_model.pk)) == 2
            assert len(self._provider.get_all(self._nv_model.pk)) == 2

            assert self._provider.get_by_id(101, self._nv_model.pk).code == 'One'
            assert self._provider.get_by_id(102, self._nv_model.pk).code == 'Two'
            assert self._provider.get_by_id(103, self._nv_model.pk) is None

            assert self._provider.get_by_code('One', self._nv_model.pk).pk == 101
            assert self._provider.get_by_code('Two', self._nv_model.pk).pk == 102
            assert self._provider.get_by_code('Three', self._nv_model.pk) is None

            assert self._provider.get_by_id(101, self._other_nv_model.pk) is None
            assert self._provider.get_by_id(102, self._other_nv_model.pk) is None
            assert self._provider.get_by_id(103, self._other_nv_model.pk).code == 'Three'

            assert self._provider.get_by_code('One', self._other_nv_model.pk) is None
            assert self._provider.get_by_code('Two', self._other_nv_model.pk) is None
            assert self._provider.get_by_code('Three', self._other_nv_model.pk).pk == 103

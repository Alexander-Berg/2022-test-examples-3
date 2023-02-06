# coding=utf-8
from __future__ import absolute_import

from logging import Logger
from typing import cast

import ujson
from mock import Mock

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.currency.rates import RatesView
from travel.avia.backend.repository.currency import CurrencyRepository, CurrencyModel
from travel.avia.backend.repository.currency_rates import CurrencyRatesRepository
from travel.avia.backend.repository.currency_translation import CurrencyTranslationRepository


class RatesViewTest(TestCase):
    def setUp(self):
        self._currency_repository = Mock()
        self._currency_rates_repository = Mock()
        self._translation_repository = cast(CurrencyTranslationRepository,
                                            Mock())

        self._view = RatesView(
            rates_repository=cast(
                CurrencyRatesRepository,
                self._currency_rates_repository
            ),
            currency_repository=cast(
                CurrencyRepository,
                self._currency_repository
            ),
            logger=cast(Logger, Mock())
        )

        self._rub = CurrencyModel(
            translation_repository=self._translation_repository,
            pk=1,
            code=u'RUR',
            iso_code=u'ISO_RUR'
        )

        self._usd = CurrencyModel(
            translation_repository=self._translation_repository,
            pk=2,
            code=u'USD',
            iso_code=u'ISO_USD'
        )

    def test_view(self):
        self._currency_rates_repository.get_rates_for = Mock(
            return_value={
                u'RUB': 1,
                u'USD': 2
            }
        )
        self._currency_rates_repository.get_base_currency = Mock(
            return_value=self._rub
        )

        self._currency_repository.get_by_code = Mock(
            side_effect={u'RUB': self._rub, u'USD': self._usd}.get
        )

        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
        })

        assert ujson.loads(result.response[0]) == {
            u'status': u'ok',
            u'data': {
                u'rates': [
                    {
                        u'currency_id': 1,
                        u'rate': 1,
                    },
                    {
                        u'currency_id': 2,
                        u'rate': 2,
                    },
                ],
                u'base_currency_id': 1
            }
        }

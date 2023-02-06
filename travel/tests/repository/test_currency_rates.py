from __future__ import absolute_import

from logging import Logger

from datetime import datetime
from mock import Mock
from typing import cast

from travel.avia.backend.repository.currency import CurrencyRepository
from travel.avia.backend.repository.currency_rates import CurrencyRatesRepository
from travel.avia.library.python.tester.testcase import TestCase


class CurrencyRatesRepositoryTest(TestCase):
    def setUp(self):
        self._currency_repository = Mock()
        self._rates_provider = Mock()
        self._dohop_rates_provider = Mock()
        self._environment = Mock()
        self._environment.now = Mock(
            return_value=datetime(2017, 9, 1)
        )

        self._repository = CurrencyRatesRepository(
            currency_repository=cast(
                CurrencyRepository,
                self._currency_repository
            ),
            rates_provider=self._rates_provider,
            dohop_rates_provider=self._dohop_rates_provider,
            environment=self._environment,
            logger=cast(Logger, Mock())
        )

    def test_fetch_for_ru_from_common_source(self):
        self._currency_repository.get_all_codes = Mock(return_value={
            'RUR', 'USD', 'AZAZA'
        })

        self._rates_provider.fetch_rates = Mock(
            return_value=('some', {
                'RUR': 1,
                'USD': 39,
                'ZZZ': 1000
            })
        )

        self._repository.fetch(['ru'])

        assert self._rates_provider.fetch_rates.call_count == 1
        assert self._dohop_rates_provider.fetch_rates.call_count == 0

        rates = self._repository.get_rates_for('ru')

        assert rates == {
            'RUR': 1,
            'USD': 39
        }

    def test_fetch_for_ru_from_dohop_source(self):
        self._currency_repository.get_all_codes = Mock(return_value={
            'RUR', 'USD', 'AZAZA'
        })

        self._rates_provider.fetch_rates = Mock(
            return_value=(None, {})
        )

        self._dohop_rates_provider.fetch_rates = Mock(
            return_value=('some',  {
                'RUR': 1,
                'USD': 45,
                'ZZZ': 1000
            })
        )

        self._repository.fetch(['com'])

        assert self._rates_provider.fetch_rates.call_count == 1
        assert self._dohop_rates_provider.fetch_rates.call_count == 1

        rates = self._repository.get_rates_for('com')

        assert rates == {
            'RUR': 1,
            'USD': 45
        }

    def test_can_not_fetch_inited_rates_in_ru_use_default(self):
        self._currency_repository.get_all_codes = Mock(return_value={
            'RUR', 'USD'
        })
        self._rates_provider.fetch_rates = Mock(
            return_value=(None, {})
        )
        self._dohop_rates_provider.fetch_rates = Mock(
            return_value=(None, {})
        )

        self._repository.fetch(['ru'])

        assert self._rates_provider.fetch_rates.call_count == 3
        assert self._dohop_rates_provider.fetch_rates.call_count == 0

    def test_can_not_fetch_inited_rates_in_com_use_default(self):
        self._currency_repository.get_all_codes = Mock(return_value={
            'RUR', 'USD'
        })
        self._rates_provider.fetch_rates = Mock(
            return_value=(None, {})
        )
        self._dohop_rates_provider.fetch_rates = Mock(
            return_value=(None, {})
        )

        self._repository.fetch(['com'])

        assert self._rates_provider.fetch_rates.call_count == 3
        assert self._dohop_rates_provider.fetch_rates.call_count == 3

    def test_do_not_die_if_previous_index_is_existed(self):
        self._currency_repository.get_all_codes = Mock(return_value={
            'RUR', 'USD'
        })
        self._rates_provider.fetch_rates = Mock(
            return_value=('uii', {
                'RUR': 1,
                'USD': 45,
                'ZZZ': 1000
            })
        )

        self._repository.fetch(['ru'])
        assert self._repository.get_rates_for('ru') == {
            'RUR': 1,
            'USD': 45
        }

        self._rates_provider.fetch_rates = Mock(
            side_effect=Exception('Boom!')
        )

        self._repository.fetch(['ru'])
        assert self._repository.get_rates_for('ru') == {
            'RUR': 1,
            'USD': 45
        }

# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.library.python.dicts.factories.country import TCountryFactory
from travel.library.python.dicts.factories.settlement import TSettlementFactory
from travel.rasp.rasp_data.resource_checker.checks.check_capitals import (
    check_capitals, check_moscow_is_a_capital_of_russia, RUSSIA_ID, MOSCOW_ID, CAPITAL_MAJORITY
)

from travel.rasp.rasp_data.resource_checker.tests.utils import assert_check, create_data_provider


class TestCheckMoscowIsACapitalOfRussia(object):
    def test_check_moscow_is_a_capital_of_russia(self):
        moscow = TSettlementFactory(Id=MOSCOW_ID, CountryId=RUSSIA_ID, Majority=CAPITAL_MAJORITY)

        data_provider = create_data_provider(settlements=[moscow])
        assert_check(check_moscow_is_a_capital_of_russia, data_provider)

    def test_check_moscow_is_a_capital_of_russia_without_capitals(self):
        not_moscow = TSettlementFactory(Id=MOSCOW_ID, CountryId=-1, Majority=CAPITAL_MAJORITY)

        data_provider = create_data_provider(settlements=[not_moscow])
        assert_check(check_moscow_is_a_capital_of_russia, data_provider, expected_fail='Russia has no capitals')

    def test_check_moscow_is_a_capital_of_russia_with_multiple_capitals(self):
        real_moscow = TSettlementFactory(Id=MOSCOW_ID, CountryId=RUSSIA_ID, Majority=CAPITAL_MAJORITY)
        fake_moscow = TSettlementFactory(Id=MOSCOW_ID+1, CountryId=RUSSIA_ID, Majority=CAPITAL_MAJORITY)

        data_provider = create_data_provider(settlements=[real_moscow, fake_moscow])
        assert_check(
            check_moscow_is_a_capital_of_russia, data_provider,
            expected_fail='Russia has multiple capitals: {}, {}'.format(real_moscow.Id, fake_moscow.Id)
        )

    def test_check_moscow_is_a_capital_of_russia_with_wrong_capital(self):
        some_russia_capital = TSettlementFactory(Id=-1, CountryId=RUSSIA_ID, Majority=CAPITAL_MAJORITY)

        data_provider = create_data_provider(settlements=[some_russia_capital])
        assert_check(
            check_moscow_is_a_capital_of_russia, data_provider,
            expected_fail='Wrong capital of Russia: {}'.format(some_russia_capital.Id)
        )


class TestCheckCapitals(object):
    def _create_capital(self, country_id):
        return TSettlementFactory(CountryId=country_id, Majority=CAPITAL_MAJORITY)

    def test_check_capitals(self):
        country_1 = TCountryFactory()
        capital_1 = self._create_capital(country_1.Id)

        country_2 = TCountryFactory()
        capital_2 = self._create_capital(country_2.Id)

        data_provider = create_data_provider(countries=[country_1, country_2], settlements=[capital_1, capital_2])
        assert_check(check_capitals, data_provider)

    def test_check_capitals_without_country_id(self):
        capital = self._create_capital(0)

        data_provider = create_data_provider(settlements=[capital])
        assert_check(check_capitals, data_provider, expected_fail='Capital without country: {}'.format(capital.Id))

    def test_check_capitals_wrong_country_id(self):
        country = TCountryFactory(Id=123)
        capital = self._create_capital(456)

        data_provider = create_data_provider(countries=[country], settlements=[capital])
        assert_check(
            check_capitals, data_provider,
            expected_fail="Can't find country with id `{}` for capital `{}`".format(capital.CountryId, capital.Id)
        )

    def test_check_capitals_multiple_capitals(self):
        country = TCountryFactory()
        capital_1 = self._create_capital(country.Id)
        capital_2 = self._create_capital(country.Id)

        data_provider = create_data_provider(countries=[country], settlements=[capital_1, capital_2])
        assert_check(
            check_capitals, data_provider,
            expected_fail='There are countries with multiple capitals: ({}: [{}, {}])'.format(
                country.Id, capital_1.Id, capital_2.Id
            )
        )

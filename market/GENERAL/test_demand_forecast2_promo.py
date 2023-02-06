#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main  # noqa
from core.matcher import Matcher, NotEmpty, Contains, NotEmptyList, IsNumber, Round, GreaterEq
from core.expectations_builder import ForecastExpectationsBuilder, calendar as clndr, make_forecast_expectation
from core.types.demand_prediction import Sales, DemandPredictionOnePInput
from core.logs import EventExpectation, Events
import core.report
from datetime import datetime, timedelta
import itertools


def calendar(start="2018-01-01", duration=10):  # use default values
    for i in clndr(start, duration):
        yield i


class T(TestCase):
    @classmethod
    def add_demand_prediction_sales(cls, resources, msku):
        """ Add sales prediction: Source ID is 1 and sales amount is 4
        """
        start_date = datetime(year=2018, month=1, day=1)
        for day in range(0, 100):
            date = start_date + timedelta(days=day)
            resources.sales.append(
                Sales(msku_id=msku, date=date, source_id=1, sales=4)
            )

    def get_forecast(self, params, duration=10):
        return self.service.request_json(
            name="demand_forecast",
            params="start-date=2018-01-01&duration={}&{}".format(duration, params)
        )

    @classmethod
    def prepare_3p_promo(cls, resources, report):
        # Offer weights (show probabilities):
        # 100 - 0.94722
        # 101 - 0.05262
        # 103 - 0.00016

        msku_info = report.msku_info()

        cls.add_demand_prediction_sales(resources=resources, msku=100)
        msku_info.add(msku=100, offers=[
            msku_info.create_offer(supplier_id=1, price=100),
            msku_info.create_offer(supplier_id=2, price=101, first_party=True),
            msku_info.create_offer(supplier_id=3, price=103)
        ], promo=msku_info.create_promo("2018-01-03", "2018-01-07", prices=(110, 99)))

    def test_current_prices_while_3P_promo(self):
        """ Check that forecast for current prices not influenced by 3P promo
        """
        expected = ForecastExpectationsBuilder()
        for date in calendar():
            expected.add(date=date, supplier_id=1, price_type="currentPrice", value=Round(4 * 0.94722, 4))
            expected.add(date=date, supplier_id=2, price_type="currentPrice", value=Round(4 * 0.05262, 4))
            expected.add(date=date, supplier_id=3, price_type="currentPrice", value=Round(4 * 0.00016, 4))

        response = self.get_forecast("market-sku=100")
        for i in expected:
            self.assertFragmentIn(response, i)

    def test_no_1P_offer_in_3P_promo(self):
        """ Check that 1P offer has no forecast for 3P promo
        """
        expected = ForecastExpectationsBuilder()
        for date in calendar():
            expected.add(date=date, supplier_id=2, price_type="promoPrice")

        response = self.get_forecast("market-sku=100")
        for i in expected:
            self.assertFragmentNotIn(response, i)

    def test_3P_offers_in_3P_promo(self):
        """ Check that all 3P offers has no forecast for 3P promo, check promo forecast dates
        """
        expected = ForecastExpectationsBuilder()
        for date in calendar("2018-01-03", 4):
            expected.add(date=date, supplier_id=1, price_type="promoPrice")
            expected.add(date=date, supplier_id=3, price_type="promoPrice")

        response = self.get_forecast("market-sku=100")
        for i in expected:
            self.assertFragmentIn(response, i)

    def test_3P_promo_dates(self):
        """ Check that 3P promo forecast absent for non-promo dates
        """
        expected = ForecastExpectationsBuilder()
        for date in itertools.chain(calendar("2018-01-01", 2), calendar("2018-01-07", 4)):
            expected.add(date=date, supplier_id=1, price_type="promoPrice")
            expected.add(date=date, supplier_id=3, price_type="promoPrice")

        response = self.get_forecast("market-sku=100")
        for i in expected:
            self.assertFragmentNotIn(response, i)

    def test_3P_promo_values(self):
        """ Check 3P promo values. Only target offer takes promo price, others stay unchanged.
        """

        val1 = Round(4 * 0.9971, 4) # 99 vs [101, 103]
        val3 = Round(4 * 0.9462, 4) # 99 vs [100, 101]

        expected = ForecastExpectationsBuilder()
        for date in calendar("2018-01-03", 4):
            expected.add(date=date, supplier_id=1, price_type="promoPrice", value=val1)
            expected.add(date=date, supplier_id=3, price_type="promoPrice", value=val3)

        response = self.get_forecast("market-sku=100")
        for i in expected:
            self.assertFragmentIn(response, i)

    @classmethod
    def prepare_1p_promo(cls, resources, report):
        msku_info = report.msku_info()

        cls.add_demand_prediction_sales(resources=resources, msku=200)
        resources.one_p_input.append(DemandPredictionOnePInput(msku_id=200, date="2018-01-03", old_price=102, new_price=100))
        resources.one_p_input.append(DemandPredictionOnePInput(msku_id=200, date="2018-01-06", old_price=99, new_price=99))
        msku_info.add(msku=200, offers=[
            msku_info.create_offer(supplier_id=1, price=100),
            msku_info.create_offer(supplier_id=2, price=101, first_party=True),
            msku_info.create_offer(supplier_id=3, price=103)
        ])

        # zero price in 1P-input that somehow happens
        cls.add_demand_prediction_sales(resources=resources, msku=201)
        resources.one_p_input += [
            DemandPredictionOnePInput(msku_id=201, date="2018-01-03", old_price=110, new_price=0),
            DemandPredictionOnePInput(msku_id=201, date="2018-01-05", old_price=100, new_price=0),
            DemandPredictionOnePInput(msku_id=201, date="2018-01-07", old_price=0, new_price=0)
        ]
        msku_info.add(msku=201, offers=[
            msku_info.create_offer(supplier_id=1, price=100, first_party=True)
        ])

    def test_current_prices_while_1P_promo(self):
        """ Check forecast for current prices while 1P promo:
        When 1P offer is a rival it applies promo new price
        When 1P offer is a target it applies promo old price
        """

        expected = ForecastExpectationsBuilder()

        # before 1P promo
        val1 = Round(4 * 0.94722, 4)  # 100 vs [101, 103]
        val2 = Round(4 * 0.05262, 4)  # 101 vs [100, 103]
        val3 = Round(4 * 0.00016, 4)  # 103 vs [100, 101]
        for date in calendar("2018-01-01", 2):
            expected.add(date=date, supplier_id=1, price_type="currentPrice", value=val1)
            expected.add(date=date, supplier_id=2, price_type="currentPrice", value=val2)
            expected.add(date=date, supplier_id=3, price_type="currentPrice", value=val3)

        # first 1P promo
        val1 = Round(4 * 0.49996, 4)  # 100 vs [100, 103]
        val2 = Round(4 * 0.00308, 4)  # 102 vs [100, 103]
        val3 = Round(4 * 0.00009, 4)  # 103 vs [100, 100]
        for date in calendar("2018-01-03", 3):
            expected.add(date=date, supplier_id=1, price_type="currentPrice", value=val1)
            expected.add(date=date, supplier_id=2, price_type="currentPrice", value=val2)
            expected.add(date=date, supplier_id=3, price_type="currentPrice", value=val3)

        # 1P offer simply gets "new price" - is not a promo (old price == new price)
        val1 = Round(4 * 0.05119, 4)  # 100 vs [99, 103]
        val2 = Round(4 * 0.94880, 4)  # 99 vs [100, 103]
        val3 = Round(4 * 0.00001, 4)  # 103 vs [99, 100]
        for date in calendar("2018-01-06", 5):
            expected.add(date=date, supplier_id=1, price_type="currentPrice", value=val1)
            expected.add(date=date, supplier_id=2, price_type="currentPrice", value=val2)
            expected.add(date=date, supplier_id=3, price_type="currentPrice", value=val3)

        response = self.get_forecast("market-sku=200")
        for i in expected:
            self.assertFragmentIn(response, i)

    def test_no_3P_offers_in_1P_promo(self):
        """ Check that 3P offers have no forecast for 1P promo
        """

        expected = ForecastExpectationsBuilder()

        for date in calendar():
            expected.add(date=date, supplier_id=1, price_type="promoPrice")
            expected.add(date=date, supplier_id=3, price_type="promoPrice")

        response = self.get_forecast("market-sku=200")
        for i in expected:
            self.assertFragmentNotIn(response, i)

    def test_dates_and_values_of_1P_promo(self):
        """ Check dates and values of 1P promo forecast
        """

        expected = ForecastExpectationsBuilder()
        absent = ForecastExpectationsBuilder()

        val = Round(4 * 0.49996, 4)  # 100 vs [100, 103]
        for date in calendar("2018-01-03", 3):
            expected.add(date=date, supplier_id=2, price_type="promoPrice", price=100, value=val)
        for date in itertools.chain(calendar("2018-01-01", 2), calendar("2018-01-06", 5)):
            absent.add(date=date, supplier_id=2, price_type="promoPrice")

        response = self.get_forecast("market-sku=200")
        for i in expected:
            self.assertFragmentIn(response, i)
        for i in absent:
            self.assertFragmentNotIn(response, i)

    def test_zero_price_in_1p_input(self):
        """ Drop promo and use original offer price if zero price is in 1P-input
        """
        response = self.get_forecast("market-sku=201")
        for date in calendar("2018-01-01", 10):
            self.assertFragmentIn(response,
                make_forecast_expectation(date=date, supplier_id=1, price=100, price_type="currentPrice")
            )

        # 2018-01-01 and 2018-01-02 are OK (has no entries in 1P-input), rest days are invalid
        self.eventLog().expect(event=Events.ERROR_ZERO_1P_PRICE, message="zero 1P price for MSKU 201", number=8)

    @classmethod
    def prepare_1p_and_3p_promo(cls, resources, report):
        msku_info = report.msku_info()

        cls.add_demand_prediction_sales(resources=resources, msku=300)

        resources.one_p_input.append(DemandPredictionOnePInput(msku_id=300, date="2018-01-03", old_price=103, new_price=100))
        resources.one_p_input.append(DemandPredictionOnePInput(msku_id=300, date="2018-01-06", old_price=101, new_price=101))

        msku_info.add(msku=300, offers=[
            msku_info.create_offer(supplier_id=1, price=100),
            msku_info.create_offer(supplier_id=2, price=101, first_party=True),
            msku_info.create_offer(supplier_id=3, price=103)
        ], promo=msku_info.create_promo("2018-01-03", "2018-01-06", prices=(110, 100)))

    def test_1p_and_3p_promos(self):
        """ Check forecast for simultaneous 1P & 3P promos
        """

        expected = ForecastExpectationsBuilder()

        # current prices while promo
        val1 = Round(4 * 0.49996, 4)  # 100 vs [100, 103]
        val2 = Round(4 * 0.00017, 4)  # 103 vs [100, 103]
        val3 = Round(4 * 0.00009, 4)  # 103 vs [100, 100]
        for date in calendar("2018-01-03", 3):
            expected.add(date=date, supplier_id=1, price_type="currentPrice", value=val1)
            expected.add(date=date, supplier_id=2, price_type="currentPrice", value=val2)
            expected.add(date=date, supplier_id=3, price_type="currentPrice", value=val3)

        # promo prices
        val1 = Round(4 * 0.49996, 4)  # 100 vs [100, 103]
        val2 = Round(4 * 0.49996, 4)  # 100 vs [100, 103]
        val3 = Round(4 * 0.33333, 4)  # 100 vs [100, 100]
        for date in calendar("2018-01-03", 3):
            expected.add(date=date, supplier_id=1, price_type="promoPrice", value=val1)
            expected.add(date=date, supplier_id=2, price_type="promoPrice", value=val2)
            expected.add(date=date, supplier_id=3, price_type="promoPrice", value=val3)


        response = self.get_forecast("market-sku=300")
        for i in expected:
            self.assertFragmentIn(response, i)


if __name__ == '__main__':
    main()


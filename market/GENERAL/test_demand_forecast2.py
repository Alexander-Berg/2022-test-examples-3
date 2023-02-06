#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main  # noqa
from core.matcher import Matcher, NotEmpty, Contains, NotEmptyList, IsNumber, Round, GreaterEq
from core.expectations_builder import ForecastExpectationsBuilder, calendar as clndr, make_forecast_expectation
from core.types.demand_prediction import Sales
from core.logs import EventExpectation, Events
import core.report
from datetime import datetime, timedelta


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
    def prepare_msku_100(cls, resources, report):
        msku_info = report.msku_info()

        # single offer
        cls.add_demand_prediction_sales(resources=resources, msku=100)
        msku_info.add(msku=100, offers=[
            msku_info.create_offer(supplier_id=1, price=100, first_party=False),
            msku_info.create_offer(supplier_id=2, price=1, first_party=False, out_of_stock=True),
            msku_info.create_offer(supplier_id=3, price=1, first_party=True, out_of_stock=True),
        ])

        cls.add_demand_prediction_sales(resources=resources, msku=101)
        msku_info.add(msku=101, offers=[
            msku_info.create_offer(supplier_id=1, price=100, first_party=True),
            msku_info.create_offer(supplier_id=2, price=1, first_party=False, out_of_stock=True),
            msku_info.create_offer(supplier_id=3, price=1, first_party=True, out_of_stock=True),
        ])

        # a few offers with different prices
        cls.add_demand_prediction_sales(resources=resources, msku=200)
        msku_info.add(msku=200, offers=[
            msku_info.create_offer(supplier_id=1, price=100),
            msku_info.create_offer(supplier_id=2, price=101),
            msku_info.create_offer(supplier_id=3, price=103)
        ])

        cls.add_demand_prediction_sales(resources=resources, msku=201)
        msku_info.add(msku=201, offers=[
            msku_info.create_offer(supplier_id=1, price=100),
            msku_info.create_offer(supplier_id=2, price=101),
            msku_info.create_offer(supplier_id=3, price=103, first_party=True)
        ])

        # a few offers with equal prices
        cls.add_demand_prediction_sales(resources=resources, msku=300)
        msku_info.add(msku=300, offers=[
            msku_info.create_offer(supplier_id=1, price=100),
            msku_info.create_offer(supplier_id=2, price=100),
            msku_info.create_offer(supplier_id=3, price=100)
        ])

        cls.add_demand_prediction_sales(resources=resources, msku=301)
        msku_info.add(msku=301, offers=[
            msku_info.create_offer(supplier_id=1, price=100),
            msku_info.create_offer(supplier_id=2, price=100),
            msku_info.create_offer(supplier_id=3, price=100, first_party=True)
        ])

        # only out-of-stock offers
        cls.add_demand_prediction_sales(resources=resources, msku=400)
        msku_info.add(msku=400, offers=[
            msku_info.create_offer(supplier_id=1, price=110, out_of_stock=True),
            msku_info.create_offer(supplier_id=2, price=100, out_of_stock=True),
            msku_info.create_offer(supplier_id=3, price=120, out_of_stock=True, first_party=True)
        ])

        # no offers at all
        cls.add_demand_prediction_sales(resources=resources, msku=500)
        msku_info.add(msku=500, offers=[])

    def test_single_offer(self):
        """ Only one supplier - it gets all sales, no difference for 1P/3P, out-of-stock offers don't affect
        """
        for msku in (100, 101):
            expected = ForecastExpectationsBuilder()
            for date in calendar():
                expected.add(msku_id=msku, date=date, supplier_id=1, price=100, price_type="currentPrice", source_id=1, value=4)

            response = self.get_forecast("market-sku={}".format(msku))
            for expectation in expected:
                self.assertFragmentIn(response, expectation)

    def test_multiple_offers_with_different_prices(self):
        """ Check that sales are shared accordingly their prices, no difference for 1P/3P
        """

        val1 = Round(4 * 0.94722, 4)  # 100 vs [101, 103]
        val2 = Round(4 * 0.05262, 4)  # 101 vs [100, 103]
        val3 = Round(4 * 0.00016, 4)  # 103 vs [100, 101]

        for msku in (200, 201):
            expected = ForecastExpectationsBuilder()
            for date in calendar("2018-01-01", 10):
                expected.add(msku_id=msku, date=date, supplier_id=1, price=100, price_type="currentPrice", value=val1)
                expected.add(msku_id=msku, date=date, supplier_id=2, price=101, price_type="currentPrice", value=val2)
                expected.add(msku_id=msku, date=date, supplier_id=3, price=103, price_type="currentPrice", value=val3)

            response = self.get_forecast("market-sku={}".format(msku))
            for expectation in expected:
                self.assertFragmentIn(response, expectation)

    def test_multiple_offers_with_equal_prices(self):
        """ Check that all offers get equal sales, no difference for 1P/3P
        """

        val = Round(4 / 3.0, 4)

        for msku in (300, 301):
            expected = ForecastExpectationsBuilder()
            for date in calendar("2018-01-01", 10):
                expected.add(msku_id=msku, date=date, supplier_id=1, price=100, price_type="currentPrice", value=val)
                expected.add(msku_id=msku, date=date, supplier_id=2, price=100, price_type="currentPrice", value=val)
                expected.add(msku_id=msku, date=date, supplier_id=3, price=100, price_type="currentPrice", value=val)

            response = self.get_forecast("market-sku={}".format(msku))
            for expectation in expected:
                self.assertFragmentIn(response, expectation)

    def test_out_of_stock_only(self):
        """ Check that when all offers are out-of-stock forecast for the cheapest one is made
        """

        response = self.get_forecast("market-sku=400")
        for date in calendar("2018-01-01", 10):
            self.assertFragmentIn(response, make_forecast_expectation(date=date, supplier_id=2, price=100, price_type="currentPrice"))
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=1))
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=3))

    def test_no_offers_at_all(self):
        """ Check that when there are no offers at all forecast for a fake one is made
        """

        response = self.get_forecast("market-sku=500")
        for date in calendar("2018-01-01", 10):
            self.assertFragmentIn(response,
                make_forecast_expectation(date=date, supplier_id=0, price=1, price_type="currentPrice", value=4.0)
            )


if __name__ == '__main__':
    main()


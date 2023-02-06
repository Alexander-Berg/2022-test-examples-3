#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main  # noqa
from core.matcher import EmptyList
from core.expectations_builder import make_forecast_expectation, calendar as clndr
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

    def get_forecast(self, params, supplier_id=None, duration=10, show_all=False):
        p = "start-date=2018-01-01&duration={}&{}".format(duration, params)
        if supplier_id is not None:
            p += "&supplier-id={}".format(supplier_id)
        if show_all:
            p += "&show-all"
        return self.service.request_json(name="demand_forecast", params=p)

    @classmethod
    def prepare_msku_100(cls, resources, report):
        msku_info = report.msku_info()

        cls.add_demand_prediction_sales(resources=resources, msku=100)
        msku_info.add(msku=100, offers=[
            msku_info.create_offer(supplier_id=1, price=100, first_party=False),
            msku_info.create_offer(supplier_id=2, price=101, first_party=True),
            msku_info.create_offer(supplier_id=3, price=99, first_party=False, out_of_stock=True)
        ])

        # the same as above but supplier 3 is active
        cls.add_demand_prediction_sales(resources=resources, msku=101)
        msku_info.add(msku=101, offers=[
            msku_info.create_offer(supplier_id=1, price=100, first_party=False),
            msku_info.create_offer(supplier_id=2, price=101, first_party=True),
            msku_info.create_offer(supplier_id=3, price=99, first_party=False, out_of_stock=False)
        ])

        # all suppliers are out-of-stock
        cls.add_demand_prediction_sales(resources=resources, msku=102)
        msku_info.add(msku=102, offers=[
            msku_info.create_offer(supplier_id=1, price=110, first_party=True, out_of_stock=True),
            msku_info.create_offer(supplier_id=2, price=100, first_party=False, out_of_stock=True),
            msku_info.create_offer(supplier_id=3, price=105, first_party=False, out_of_stock=True)
        ])

    def test_no_other_suppliers(self):
        """ Check that forecast for a particular supplier doesn't contain forecast for others
        """
        response = self.get_forecast(params="market-sku=100", supplier_id=1)
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=2))

        response = self.get_forecast(params="market-sku=100", supplier_id=2)
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=1))

    def test_show_all(self):
        """ Check that forecast is made for all available suppliers, not only for a specified one
        """
        for supplier_id in (1, 2):
            response = self.get_forecast(params="market-sku=100", supplier_id=supplier_id, show_all=True)
            self.assertFragmentIn(response, make_forecast_expectation(supplier_id=1))
            self.assertFragmentIn(response, make_forecast_expectation(supplier_id=2))
            self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=3))  # absent because it's out-of-stock

        response = self.get_forecast(params="market-sku=100", supplier_id=3, show_all=True)
        self.assertFragmentIn(response, make_forecast_expectation(supplier_id=1))
        self.assertFragmentIn(response, make_forecast_expectation(supplier_id=2))
        self.assertFragmentIn(response, make_forecast_expectation(supplier_id=3))  # present because it's explicitly specified

    def test_show_all_when_specified_supplier_is_absent(self):
        """ Check that forecast is made if specified supplier is absent but show-all flag is set
        """
        response = self.get_forecast(params="market-sku=100", supplier_id=4, show_all=True)
        self.assertFragmentIn(response, make_forecast_expectation(supplier_id=1))
        self.assertFragmentIn(response, make_forecast_expectation(supplier_id=2))
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=3))  # absent because it's out-of-stock

    def test_forecast_for_active_supplier(self):
        """ Check that forecast for active suppliers is the same as in general response
        """

        generalResponse = self.get_forecast(params="market-sku=100")  # contains reference forecast values

        response = self.get_forecast(params="market-sku=100", supplier_id=1)
        by_supplier = response.get_all_matched(make_forecast_expectation(supplier_id=1, price_type="currentPrice"))
        self.assertFragmentIn(generalResponse, by_supplier)

        response = self.get_forecast(params="market-sku=100", supplier_id=2)
        by_supplier = response.get_all_matched(make_forecast_expectation(supplier_id=2, price_type="currentPrice"))
        self.assertFragmentIn(generalResponse, by_supplier)

    def test_forecast_for_out_of_stock_supplier(self):
        """ Check that forecast for out-of-stock suppliers is the same as if it was active
        """

        response = self.get_forecast(params="market-sku=100", supplier_id=3)
        by_passive_supplier = response.get_all_matched(make_forecast_expectation(supplier_id=3, price_type="currentPrice"))

        response = self.get_forecast(params="market-sku=101", supplier_id=3)
        by_active_supplier = response.get_all_matched(make_forecast_expectation(supplier_id=3, price_type="currentPrice"))
        by_active_supplier[0]["mskuId"] = 100  # the only difference

        self.assertEqual(by_passive_supplier, by_active_supplier)

    def test_forecast_for_non_existing_supplier(self):
        """ Check that forecast for non-existing supplier fails
        """

        response = self.get_forecast(params="market-sku=100", supplier_id=9)
        self.assertFragmentIn(response, {"results": [{"mskuId": 100, "value": EmptyList()}]})
        self.eventLog().expect(event=Events.ERROR_SUPPLIER_DOES_NOT_EXIST, message="forecast for MSKU 100 failed")

    def test_out_of_stock_only(self):
        """ Check that when all offers are out-of-stock forecast for a target one is made
        """

        response = self.get_forecast("market-sku=102", supplier_id=1)
        for date in calendar("2018-01-01", 10):
            self.assertFragmentIn(response, make_forecast_expectation(date=date, supplier_id=1, price=110, price_type="currentPrice"))
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=2))
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=3))

        response = self.get_forecast("market-sku=102", supplier_id=2)
        for date in calendar("2018-01-01", 10):
            self.assertFragmentIn(response, make_forecast_expectation(date=date, supplier_id=2, price=100, price_type="currentPrice"))
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=1))
        self.assertFragmentNotIn(response, make_forecast_expectation(supplier_id=3))



if __name__ == '__main__':
    main()


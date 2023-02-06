#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main  # noqa
from core.expectations_builder import calendar as clndr, make_forecast_expectation
from core.types.demand_prediction import Sales
from core.matcher import Contains, Round
from datetime import datetime, timedelta

# Temporary test to check that SalesAndVariance mmap-table works somehow


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
                Sales(msku_id=msku, date=date, source_id=1, sales=4, variance=0.1)
            )

    def get_forecast(self, params, duration=10):
        return self.service.request_json(
            name="demand_forecast",
            params="start-date=2018-01-01&duration={}&{}".format(duration, params)
        )

    @classmethod
    def prepare_all(cls, resources, report):
        msku_info = report.msku_info()
        msku_info.add(msku=100, offers=[
            msku_info.create_offer(supplier_id=1, price=100, first_party=True)
        ])

        cls.add_demand_prediction_sales(resources=resources, msku=100)
        resources.sales_mode = Sales.SALES_AND_VARIANCE

    def test_forecast(self):
        response = self.get_forecast("debug&market-sku={}".format(100))
        for date in calendar():
            self.assertFragmentIn(
                response,
                make_forecast_expectation(msku_id=100, date=date, supplier_id=1, source_id=1, value=4,
                                          variance=Round(0.1, 5))
            )
        self.assertFragmentIn(response, {"debug": {
            "trace": [Contains("sales format=DEMAND-PREDICTION-SALES-AND-VARIANCE; version=1")]
        }})


if __name__ == '__main__':
    main()


#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main  # noqa
from core.expectations_builder import make_forecast_expectation, calendar as clndr
from core.types.demand_prediction import Sales
from core.matcher import Round
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

    def get_forecast(self, params, supplier_id=None, duration=10):
        p = "start-date=2018-01-01&duration={}&{}".format(duration, params)
        return self.service.request_json(name="demand_forecast", params=p)

    @classmethod
    def prepare_msku_100(cls, resources, report):
        resources.region_weights = {
            # regions of Moscow warehouse (0.75)
            "10802": 0.1, "10174": 0.1, "10176": 0.1, "10231": 0.1, "10233": 0.1, "10243": 0.1, "10251": 0.15,
            # regions of Rostov-on-Don warehouse (0.25)
            "10946": 0.1, "10950": 0.1, "10995": 0.05
        }

        msku_info = report.msku_info()

        cls.add_demand_prediction_sales(resources=resources, msku=100)
        msku_info.add(msku=100, offers=[
            msku_info.create_offer(supplier_id=1, price=100, first_party=False)
        ])

    def test_warehouse_splits(self):
        """ Check whether there is a split for each warehouse per day; check warehouse sales proportions
        See MARKETFORECASTS-93 - proportion Moscow/Rostov-on-Don is always 0.95/0.05
        """
        response = self.get_forecast(params="market-sku=100")
        for date in calendar():
            self.assertFragmentIn(response, make_forecast_expectation(
                date=date, warehouse_name="Moscow", warehouse_id=145, value=Round(4 * 0.95, 6)
            ))
            self.assertFragmentIn(response, make_forecast_expectation(
                date=date, warehouse_name="Rostov-on-Don", warehouse_id=147, value=Round(4 * 0.05, 6)
            ))


if __name__ == '__main__':
    main()


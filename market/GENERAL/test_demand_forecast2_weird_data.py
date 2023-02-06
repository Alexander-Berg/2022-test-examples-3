#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main  # noqa
from core.expectations_builder import ForecastExpectationsBuilder, make_forecast_expectation, calendar as clndr
from core.types.demand_prediction import Sales, DemandPredictionOnePInput
from core.logs import Events
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
    def prepare_tests(cls, resources, report):
        msku_info = report.msku_info()

        # MSKU with zero old price since 2018-01-06
        cls.add_demand_prediction_sales(resources=resources, msku=100)
        resources.one_p_input.append(DemandPredictionOnePInput(msku_id=100, date="2018-01-03", old_price=102, new_price=99))
        resources.one_p_input.append(DemandPredictionOnePInput(msku_id=100, date="2018-01-06", old_price=0, new_price=100))
        msku_info.add(msku=100, offers=[
            msku_info.create_offer(supplier_id=1, price=100, first_party=True),
            msku_info.create_offer(supplier_id=2, price=100, first_party=False)
        ])

    def test_zero_old_price(self):
        """ Use 'new price' instead of zero 'old price' of 1P promo
        """
        response = self.get_forecast("market-sku=100")
        for date in calendar("2018-01-01", 2):
            self.assertFragmentIn(response, make_forecast_expectation(date=date, supplier_id=1, price_type="currentPrice", price=100))
        for date in calendar("2018-01-03", 3):
            self.assertFragmentIn(response, make_forecast_expectation(date=date, supplier_id=1, price_type="currentPrice", price=102))
            self.assertFragmentIn(response, make_forecast_expectation(date=date, supplier_id=1, price_type="promoPrice", price=99))
        for date in calendar("2018-01-06", 5):
            self.assertFragmentIn(response, make_forecast_expectation(date=date, supplier_id=1, price_type="currentPrice", price=100))


if __name__ == '__main__':
    main()


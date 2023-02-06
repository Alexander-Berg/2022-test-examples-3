#!/usr/bin/env python
# -*- coding: utf-8 -*-
# TODO: tests for offline forecaster should be detached from HTTP-service tests; use this LITE-engine just for now =)

from core.testcase import TestCase, main  # noqa
from core.types.demand_prediction import Sales, DemandPredictionOnePInput
from datetime import datetime, timedelta
from core.types.blue_offer import BlueOffer
import json


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

    def check_output(self, output, expected):
        with open(output, "r") as f:
            line = f.readline()
            while line:
                self.assertIn(json.loads(line), expected)
                line = f.readline()

    @classmethod
    def prepare_blue_dump(cls, resources, report):
        cls.add_demand_prediction_sales(resources=resources, msku=10)
        cls.add_demand_prediction_sales(resources=resources, msku=11)
        cls.add_demand_prediction_sales(resources=resources, msku=12)

        resources.blues += [  # msku, supplier, type, price
            BlueOffer(10, 1, 1, 200),
            BlueOffer(11, 2, 1, 300),
            BlueOffer(11, 3, 3, 300),
            BlueOffer(12, 4, 3, 400),
        ]


    def test_with_blue_dump(self):
        expected = [
            {"date": "2018-01-01", "market_sku": 10, "price": 200, "source_id": 1, "supplier_id": 1, "price_type": "currentPrice",
             "value": 3.8, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
            {"date": "2018-01-01", "market_sku": 10, "price": 200, "source_id": 1, "supplier_id": 1, "price_type": "currentPrice",
             "value": 0.2, "variance": 0, "warehouse_id": 147, "warehouse_name": "Rostov-on-Don"},

            {"date": "2018-01-01", "market_sku": 11, "price": 300, "source_id": 1, "supplier_id": 2, "price_type": "currentPrice",
             "value": 1.9, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
            {"date": "2018-01-01", "market_sku": 11, "price": 300, "source_id": 1, "supplier_id": 2, "price_type": "currentPrice",
             "value": 0.1, "variance": 0, "warehouse_id": 147, "warehouse_name": "Rostov-on-Don"},
            {"date": "2018-01-01", "market_sku": 11, "price": 300, "source_id": 1, "supplier_id": 3, "price_type": "currentPrice",
             "value": 1.9, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
            {"date": "2018-01-01", "market_sku": 11, "price": 300, "source_id": 1, "supplier_id": 3, "price_type": "currentPrice",
             "value": 0.1, "variance": 0, "warehouse_id": 147, "warehouse_name": "Rostov-on-Don"},

            {"date": "2018-01-01", "market_sku": 12, "price": 400, "source_id": 1, "supplier_id": 4,
             "price_type": "currentPrice", "value": 3.8, "variance": 0, "warehouse_id": 145, "warehouse_name": "Moscow"},
            {"date": "2018-01-01", "market_sku": 12, "price": 400, "source_id": 1, "supplier_id": 4,
             "price_type": "currentPrice", "value": 0.2, "variance": 0, "warehouse_id": 147, "warehouse_name": "Rostov-on-Don"}
        ]

        args = [
            "--start-date", "2018-01-01",
            "--duration", "1",
            "--blue-dump", self.resources.blue_dump_path
        ]

        # for given MSKUs and for all MSKUs
        for a in (args + ["--market-sku", "10,11,12"], args):
            output = self.runOfflineService(a)
            self.check_output(output, expected)

    def test_with_blue_dump_split_by_warehouses_only(self):
        expected = [
            {"date": "2018-01-01", "market_sku": 10, "warehouse_id": 145, "warehouse_name": "Moscow", "value": 3.8},
            {"date": "2018-01-01", "market_sku": 10, "warehouse_id": 147, "warehouse_name": "Rostov-on-Don", "value": 0.2},
            {"date": "2018-01-01", "market_sku": 11, "warehouse_id": 145, "warehouse_name": "Moscow", "value": 3.8},
            {"date": "2018-01-01", "market_sku": 11, "warehouse_id": 147, "warehouse_name": "Rostov-on-Don", "value": 0.2},
            {"date": "2018-01-01", "market_sku": 12, "warehouse_id": 145, "warehouse_name": "Moscow", "value": 3.8},
            {"date": "2018-01-01", "market_sku": 12, "warehouse_id": 147, "warehouse_name": "Rostov-on-Don", "value": 0.2}
        ]

        args = [
            "--split-by-warehouses-only",
            "--start-date", "2018-01-01",
            "--duration", "1",
            "--blue-dump", self.resources.blue_dump_path
        ]

        # for given MSKUs and for all MSKUs
        for a in (args + ["--market-sku", "10,11,12"], args):
            output = self.runOfflineService(a)
            self.check_output(output, expected)


if __name__ == '__main__':
    main()

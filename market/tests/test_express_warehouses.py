# -+- coding: utf-8 -+-

from getter import core
from getter.service import yt_express_warehouses
from getter.validator import validate_express_warehouses, VerificationError

from market.pylibrary import yenv
from market.pylibrary.pbufsn_utils import read_pbufsn
from market.pylibrary.yatestwrap.yatestwrap import source_path
import market.proto.delivery.ExpressWarehouses_pb2 as ExpressWarehousesProto

import unittest
import os


class Test(unittest.TestCase):

    def test_express_warehouses_validator(self):
        DATA_DIR = source_path("market/getter/tests/data/")

        VALID_PATH = os.path.join(DATA_DIR, "express_warehouses-valid.pbuf.sn")
        INVALID_PATH = os.path.join(DATA_DIR, "express_warehouses-invalid.pbuf.sn")

        validate_express_warehouses(VALID_PATH)

        with self.assertRaises(VerificationError):
            validate_express_warehouses(INVALID_PATH)

    def test_express_warehouses_resource(self):
        EXPECTED_YT_PATHS = [
            (yenv.TESTING, "//home/market/testing/indexer/combinator/express.v2/recent&"),
            (yenv.PRODUCTION, "//home/market/production/indexer/combinator/express.v2/recent&"),
        ]

        file_path = 'express_warehouses.pbuf.sn'

        for envtype, expected_yt_path in EXPECTED_YT_PATHS:
            yenv.set_environment_type(envtype)

            resources_map = {file_path : yt_express_warehouses.WarehouseResourceBuilder()}

            resources = core.create_resources(resources_map)
            self.assertEqual(len(resources), 1)
            self.assertIn(file_path, resources)
            resource = resources[file_path]
            self.assertEqual(resource._path_to_yt_resource(), expected_yt_path)
            self.assertEqual(resource.name, file_path)

    def test_shops_schedule_resource(self):
        EXPECTED_YT_PATHS = [
            (yenv.TESTING, "//home/taxi/testing/replica/postgres/eats_retail_market_integration/places_info&"),
            (yenv.PRODUCTION, "//home/taxi/production/replica/postgres/eats_retail_market_integration/places_info&"),
        ]

        file_path = 'retail_shops_schedule.pbuf.sn'

        for envtype, expected_yt_path in EXPECTED_YT_PATHS:
            yenv.set_environment_type(envtype)

            resources_map = {file_path : yt_express_warehouses.FoodtechShopScheduleResourceBuilder()}

            resources = core.create_resources(resources_map)
            self.assertEqual(len(resources), 1)
            self.assertIn(file_path, resources)
            resource = resources[file_path]
            self.assertEqual(resource._path_to_yt_resource(), expected_yt_path)
            self.assertEqual(resource.name, file_path)

    def test_warehouse_and_schedule_converter(self):
        DATA_DIR = source_path("market/getter/tests/data/")

        warehouses_file = os.path.join(DATA_DIR, "express_warehouses-only.pbuf.sn")
        shops_schedule_file = os.path.join(DATA_DIR, "shops_schedule.pbuf.sn")
        shop_to_warehouse_file = os.path.join(DATA_DIR, "shop_to_warehouse_map.json")

        result_path = "shop_to_warehouse_map.json"

        yt_express_warehouses.warehouse_and_schedule_converter(result_path, warehouses_file, shops_schedule_file, shop_to_warehouse_file)

        result = read_pbufsn(result_path, ExpressWarehousesProto.Root, "EXWH").next()
        # проверим, что в итоге получили 10 складов
        self.assertEqual(len(result.express_warehouses), 10)

    def test_parse_schedule(self):
        schedule_str = "[{\"from\": \"11:10\", \"weekday\": \"monday\", \"duration\": 485}, \
                         {\"from\": \"11:10\", \"weekday\": \"tuesday\", \"duration\": 485}, \
                         {\"from\": \"11:10\", \"weekday\": \"wednesday\", \"duration\": 485}, \
                         {\"from\": \"11:10\", \"weekday\": \"thursday\", \"duration\": 485}, \
                         {\"from\": \"11:10\", \"weekday\": \"friday\", \"duration\": 485}, \
                         {\"from\": \"11:10\", \"weekday\": \"saturday\", \"duration\": 485}, \
                         {\"from\": \"00:00\", \"weekday\": \"sunday\", \"duration\": 1440}]"

        partner_id = 0
        schedule = yt_express_warehouses.parseScheduleFromJson(schedule_str, partner_id)
        self.assertEqual(len(schedule), 7)
        for i in range(0, 6):
            weekday_schedule = schedule[i]
            self.assertEqual(weekday_schedule.key, i+1)
            self.assertEqual(len(weekday_schedule.intervals), 1)
            interval = weekday_schedule.intervals[0]
            fromField = getattr(interval, "from")
            self.assertEqual(fromField.hour, 11)
            self.assertEqual(fromField.min, 10)
            self.assertEqual(interval.to.hour, 19)
            self.assertEqual(interval.to.min, 15)

        sunday = schedule[6]
        self.assertEqual(sunday.key, 7)
        self.assertEqual(len(sunday.intervals), 1)
        interval = sunday.intervals[0]
        fromField = getattr(interval, "from")
        self.assertEqual(fromField.hour, 0)
        self.assertEqual(fromField.min, 0)
        self.assertEqual(interval.to.hour, 24)
        self.assertEqual(interval.to.min, 0)


if __name__ == '__main__':
    unittest.main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import subprocess
import unittest
import yatest


SHOP_OUTLET_V3_XML = yatest.common.source_path("market/tools/getter_validators/delivery-services-generator/ut/shops_outlet.v3.xml")
SHOP_OUTLET_V3_MMAP = yatest.common.output_path("shops_outlet.v3.mmap")
XML2MMAP_CONVERTER = yatest.common.binary_path("market/tools/getter_validators/shops-outlet-xml2mmap-converter/shops-outlet-xml2mmap-converter")
DELIVERY_SERVICES_GENERATOR = yatest.common.binary_path("market/tools/getter_validators/delivery-services-generator/delivery-services-generator")
DELIVERY_SERVICES_JSON = yatest.common.output_path("delivery_service_flags.json")


CANONICAL_OUTPUT = {
    "150": {
        "DEPOT": 1
    },
    "123": {
        "DEPOT": 2,
        "RETAIL": 4,
        "post_term": 8
    },
    "103": {
        "DEPOT": 16
    }
}


class T(unittest.TestCase):
    maxDiff = None

    def setUp(self):
        cmd_list = [
            XML2MMAP_CONVERTER,
            "--xml", SHOP_OUTLET_V3_XML,
            "--mmap", SHOP_OUTLET_V3_MMAP,
            "--mode", "xml",
        ]
        subprocess.check_call(args=cmd_list)

    def test_delivery_services_generator(self):
        subprocess.check_call([DELIVERY_SERVICES_GENERATOR, "--src", SHOP_OUTLET_V3_MMAP, "--dst", DELIVERY_SERVICES_JSON])
        with open(DELIVERY_SERVICES_JSON, "r") as fn:
            output = json.load(fn)
        assert output == CANONICAL_OUTPUT

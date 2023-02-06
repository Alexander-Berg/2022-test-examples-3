#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from test_delivery_access import TMarketAccessDelivery
from test_geo import T as TBase

from core.testcase import main


class T(TBase):
    access_driver = None

    @classmethod
    def prepare(cls):
        cls.access_driver = TMarketAccessDelivery(cls)
        cls.settings.enable_access_delivery_for_search = True
        cls.access_driver.enable_report_market_dynamic_from_access()

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        cls.access_driver.setup_market_access_resources(access_server, shade_host_port)

    def test_access_loads_fb_delivery_files(self):
        self.__class__.access_driver.wait_market_access(self)


if __name__ == '__main__':
    main()

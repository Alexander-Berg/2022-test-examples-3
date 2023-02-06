#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main  # noqa
from core.resources import Resources  # noqa
from core.matcher import NotEmpty
from core.logs import Events


class T(TestCase):
    @classmethod
    def prepare_test(cls, resources, report):
        pass

    def test_ping(self):
        response = self.service.request_text("ping")
        self.assertFragmentIn(response, "0;OK")

    def test_info(self):
        response = self.service.request_json("info")
        self.assertFragmentIn(response, {
            "date": NotEmpty(),
            "name": "forecaster",
            "revision": NotEmpty()
        })

    def test_too_big_duration(self):
        response = self.service.request_json(name="demand_forecast", params="market-sku=1&duration=91")
        self.eventLog().expect(event=Events.ERROR_INVALID_REQUEST, message="too big forecast duration")

    def test_too_many_msku(self):
        mskus = ",".join(str(i) for i in range(0, 51))
        response = self.service.request_json(name="demand_forecast", params="market-sku={}".format(mskus))
        self.eventLog().expect(event=Events.ERROR_INVALID_REQUEST, message="too many MSKUs")

    def test_msku_info_failure(self):
        response = self.service.request_json(name="demand_forecast", params="market-sku=1")
        self.eventLog().expect(event=Events.ERROR_MSKU_INFO_FAILED, message="forecast for MSKU 1 failed")


if __name__ == '__main__':
    main()

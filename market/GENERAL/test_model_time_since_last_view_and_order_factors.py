#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import Model
from core.testcase import TestCase, main
from core.report import REQUEST_TIMESTAMP
from core.bigb import BeruPersHistoryModelViewLastTimeCounter, ModelLastSeenEvent
from core.bigb import BeruModelOrderLastTimeCounter, ModelLastOrderEvent
from core.bigb import BeruModelOrderCounter, ModelOrderEvent
from core.bigb import BeruCategoryOrderLastTimeCounter, CategoryLastOrderEvent


class T(TestCase):
    @classmethod
    def prepare_time_since_last_view_factor(cls):

        cls.index.models += [
            Model(title='iphone Xr 256', hyperid=2721111, hid=2401301),
            Model(title='samsung galaxy tab 7', hyperid=2721112, hid=2401302),
            Model(title='Робот пылесос 123456', hyperid=123456, hid=2401302),
        ]

        counters = [
            BeruPersHistoryModelViewLastTimeCounter(
                model_view_events=[
                    ModelLastSeenEvent(model_id=2721111, timestamp=REQUEST_TIMESTAMP - 1),  # iphone Xr 256
                    ModelLastSeenEvent(model_id=2721112, timestamp=REQUEST_TIMESTAMP - 10),  # samsung galaxy tab 7
                ]
            ),
            BeruModelOrderCounter(
                model_order_events=[ModelOrderEvent(model_id=123456, order_count=2)]  # Робот пылесос 123456
            ),
            BeruModelOrderLastTimeCounter(
                model_order_events=[
                    ModelLastOrderEvent(model_id=123456, timestamp=REQUEST_TIMESTAMP - 10000)  # Робот пылесос 123456
                ]
            ),
            BeruCategoryOrderLastTimeCounter(
                category_order_events=[
                    CategoryLastOrderEvent(
                        category_id=2401302, timestamp=REQUEST_TIMESTAMP - 55
                    )  # Робот пылесос 123456
                ]
            ),
        ]

        cls.bigb.on_request(yandexuid='111', client='merch-machine').respond(counters=counters)
        cls.bigb.on_default_request().respond()

    def test_time_since_last_view_factor(self):
        response = self.report.request_json('place=prime&text=iphone Xr 256&debug=da&yandexuid=111')
        self.assertFragmentIn(
            response,
            {"titles": {"raw": "iphone Xr 256"}, "debug": {"factors": {"BLUE_MODEL_TIME_SINCE_LAST_VIEW": "1"}}},
        )

        response = self.report.request_json('place=prime&text=samsung galaxy tab 7&debug=da&yandexuid=111')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "samsung galaxy tab 7"},
                "debug": {"factors": {"BLUE_MODEL_TIME_SINCE_LAST_VIEW": "10"}},
            },
        )

        response = self.report.request_json('place=prime&text=samsung galaxy tab 7&debug=da')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "samsung galaxy tab 7"},
                "debug": {
                    "factors": {
                        "BLUE_MODEL_TIME_SINCE_LAST_VIEW": "1000000000",
                        "TIME_SINCE_LAST_MODEL_ORDER": "1000000000",
                    }
                },
            },
        )

        response = self.report.request_json('place=prime&text=Робот пылесос 123456&debug=da&yandexuid=111')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Робот пылесос 123456"},
                "debug": {
                    "factors": {
                        "BLUE_MODEL_TIME_SINCE_LAST_VIEW": "1000000000",
                        "TIME_SINCE_LAST_MODEL_ORDER": "10000",
                        "USER_PERIOD_MODEL_NO_ORDER": "10000",
                        "USER_COUNT_MODEL_ORDER": "2",
                        "USER_PERIOD_CATEGORY_NO_ORDER": "55",
                    }
                },
            },
        )


if __name__ == '__main__':
    main()

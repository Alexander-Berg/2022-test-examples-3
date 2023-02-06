#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import HyperCategory, Model, Offer, MarketAccessSingleFbResource

from core.types.hypercategory import (
    CategoryStreamRecord,
    CategoryStreamsStorage,
)

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        catstream_resource = MarketAccessSingleFbResource(
            access_server=access_server,
            shade_host_port=shade_host_port,
            meta_paths=cls.meta_paths,
            resource_name="report_catstreams",
            publisher_name="report",
        )

        catstreams_map = [
            CategoryStreamRecord(hid=123456, category_stream=12),
            CategoryStreamRecord(hid=321456, category_stream=7),
        ]

        catstream_resource.create_version(CategoryStreamsStorage(catstreams_map, cls.meta_paths))

    @classmethod
    def prepare_category_streams(cls):
        cls.index.hypertree += [
            HyperCategory(hid=123456),
            HyperCategory(hid=321456),
        ]

        # Заводим модель, приматченную к одной из категорий...
        cls.index.models += [Model(title="test_model_123456", hyperid=123456, hid=123456)]

        cls.index.offers += [Offer(title="offer_321456", price=1000, hyperid=321456, hid=321456)]

    def test_category_streams(self):
        """
        https://st.yandex-team.ru/MARKETOUT-43287
        Проверяем, что в feature-лог появились факторы катстримов
        """

        request = 'place=prime&text=test_model_123456&debug=da'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "CATSTREAM_UNKNOWN": "1",
            },
        )

        request = 'place=prime&text=offer_321456&debug=da'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "CATSTREAM_BEAUTY": "1",
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "CATSTREAM_UNKNOWN": "1",
            },
        )


if __name__ == '__main__':
    main()

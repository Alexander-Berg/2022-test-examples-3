#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from unittest import skip


from core.testcase import TestCase, main
from core.types import DeliveryOption, HyperCategory, HyperCategoryType, Model, Offer, Opinion
from core.matcher import NotEmpty


# https://st.yandex-team.ru/MARKETOUT-13994
class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.hypertree += [HyperCategory(hid=1, output_type=HyperCategoryType.GURU)]
        cls.index.offers += [
            Offer(title='offer doc301', hid=1, hyperid=301, manufacturer_warranty=True),
            Offer(title='offer doc302', hid=1, hyperid=302, manufacturer_warranty=True),
            Offer(title='offer doc303', hid=1, hyperid=303, manufacturer_warranty=True),
            Offer(
                title='offer doc304',
                hid=1,
                hyperid=304,
                manufacturer_warranty=False,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc305-1',
                hid=1,
                hyperid=305,
                manufacturer_warranty=False,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc305-2',
                hid=1,
                hyperid=305,
                manufacturer_warranty=False,
                delivery_options=[
                    DeliveryOption(price=30, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc306',
                hid=1,
                hyperid=306,
                manufacturer_warranty=True,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc307',
                hid=1,
                hyperid=307,
                manufacturer_warranty=True,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc308-1',
                hid=1,
                hyperid=308,
                manufacturer_warranty=True,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc308-2',
                hid=1,
                hyperid=308,
                manufacturer_warranty=True,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc309',
                hid=1,
                hyperid=309,
                manufacturer_warranty=False,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc310',
                hid=1,
                hyperid=310,
                manufacturer_warranty=False,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title='offer doc311',
                hid=1,
                hyperid=311,
                manufacturer_warranty=False,
                delivery_options=[
                    DeliveryOption(price=10, day_from=1, day_to=2, order_before=23),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                title='model doc301',
                hyperid=301,
                hid=1,
                created_ts=1500000000.029648,
                model_clicks=300,
                opinion=Opinion(total_count=10, rating=1),
            ),
            Model(
                title='model doc302',
                hyperid=302,
                hid=1,
                created_ts=1500000001.029648,
                model_clicks=300,
                opinion=Opinion(total_count=14, rating=4),
            ),
            Model(title='model doc307', hyperid=307, hid=1, created_ts=1500000006.029648, model_clicks=300),
            Model(
                title='model doc303',
                hyperid=303,
                hid=1,
                created_ts=1500000002.029648,
                model_clicks=300,
                opinion=Opinion(total_count=7, rating=3.0),
                randx=10000,
            ),
            Model(
                title='model doc304',
                hyperid=304,
                hid=1,
                created_ts=1500000003.029648,
                model_clicks=300,
                opinion=Opinion(total_count=9, rating=2.5),
                randx=100,
            ),
            Model(title='model doc308', hyperid=308, hid=1, created_ts=1500000007.029648, model_clicks=300),
            Model(title='model doc305', hyperid=305, hid=1, created_ts=1500000004.029648, model_clicks=300),
            Model(
                title='model doc306',
                hyperid=306,
                hid=1,
                created_ts=1500000005.029648,
                model_clicks=300,
                opinion=Opinion(total_count=40, rating=5),
            ),
            Model(
                title='model doc309',
                hyperid=309,
                hid=1,
                created_ts=1400000008.029648,
                model_clicks=300,
                opinion=Opinion(total_count=20, rating=2),
            ),
            Model(
                title='model doc310',
                hyperid=310,
                hid=1,
                created_ts=1400000009.029648,
                model_clicks=300,
                opinion=Opinion(total_count=7, rating=1),
            ),
            Model(
                title='model doc311',
                hyperid=311,
                hid=1,
                created_ts=1400000010.029648,
                model_clicks=300,
                opinion=Opinion(total_count=35, rating=4),
            ),
        ]

    def test_quality_with_warranty(self):  # 301, 302, 303, 306, 307, 308
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=1&numdoc=12&how=quality&manufacturer_warranty=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 306, "entity": "product"},  # 5
                    {"id": 302, "entity": "product"},  # 4
                    {"id": 303, "entity": "product"},  # 3
                    {"id": 301, "entity": "product"},  # 1
                    NotEmpty(),
                    NotEmpty(),
                ]
            },
            preserve_order=True,
        )

    def test_opinions_with_warranty(self):  # 301, 302, 303, 306, 307, 308
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=1&numdoc=12&how=opinions&manufacturer_warranty=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 306, "entity": "product"},  # 40
                    {"id": 302, "entity": "product"},  # 14
                    {"id": 301, "entity": "product"},  # 10
                    {"id": 303, "entity": "product"},  # 7
                    NotEmpty(),  # None
                    NotEmpty(),  # None
                ]
            },
            preserve_order=True,
        )

    @skip('Ignore until MARKETOUT-14365 done')
    def test_ddate_with_warranty(self):  # hyperid: 301, 302, 303, 306, 307, 308
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=1&numdoc=12&how=ddate&manufacturer_warranty=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 308, "entity": "product"},  # 007.x
                    {"id": 307, "entity": "product"},  # 006.x
                    {"id": 306, "entity": "product"},  # 005.x
                    {"id": 303, "entity": "product"},  # 002.x
                    {"id": 302, "entity": "product"},  # 001.x
                    {"id": 301, "entity": "product"},  # 000.x
                ]
            },
            preserve_order=True,
        )

    def test_quality_with_delivery(self):  # hyperid: 304-311
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=1&numdoc=12&how=quality&offer-shipping=delivery'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 306, "entity": "product"},  # 5
                    {"id": 311, "entity": "product"},  # 4
                    {"id": 304, "entity": "product"},  # 2.5
                    {"id": 309, "entity": "product"},  # 2
                    {"id": 310, "entity": "product"},  # 1
                    NotEmpty(),  # None
                    NotEmpty(),  # None
                    NotEmpty(),  # None
                ]
            },
            preserve_order=True,
        )

    def test_opinions_with_delivery(self):  # hyperid: 304-311
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=1&numdoc=12&how=opinions&offer-shipping=delivery'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 306, "entity": "product"},  # 40
                    {"id": 311, "entity": "product"},  # 35
                    {"id": 309, "entity": "product"},  # 20
                    {"id": 304, "entity": "product"},  # 14
                    {"id": 310, "entity": "product"},  # 7
                    NotEmpty(),  # None
                    NotEmpty(),  # None
                    NotEmpty(),  # None
                ]
            },
            preserve_order=True,
        )

    @skip('Ignore until MARKETOUT-14365 done')
    def test_ddate_with_delivery(self):  # hyperid: 304-311
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=1&numdoc=12&how=ddate&offer-shipping=delivery'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 308, "entity": "product"},  # 007.x
                    {"id": 307, "entity": "product"},  # 006.x
                    {"id": 306, "entity": "product"},  # 005.x
                    {"id": 305, "entity": "product"},  # 004.x
                    {"id": 304, "entity": "product"},  # 003.x
                    {"id": 309, "entity": "product"},  # 4..008.x
                    {"id": 310, "entity": "product"},  # 4..009.x
                    {"id": 311, "entity": "product"},  # 4..010.x
                ]
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()

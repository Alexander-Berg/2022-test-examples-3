#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Model
from core.bigb import WeightedValue, BigBKeyword
from core.dj import DjModel
from core.testcase import TestCase, main


DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.models += [
            Model(hyperid=hyperid, hid=102, title='model{}'.format(hyperid)) for hyperid in range(11, 14)
        ]

        cls.bigb.on_request(yandexuid='0002', client='merch-machine').respond(keywords=DEFAULT_PROFILE)

        def blue_offer(shop_sku, hyperid, price):
            return BlueOffer(
                offerid=shop_sku,
                price=price,
                hyperid=hyperid,
            )

        cls.index.mskus += [
            MarketSku(sku=hyperid * 10, hyperid=hyperid, blue_offers=[blue_offer("blue_offer", hyperid, 400)])
            for hyperid in range(11, 14)
        ]

        cls.index.low_ue_mskus += [hyperid * 10 for hyperid in range(11, 12)]

    @classmethod
    def prepare_dj(cls):
        cls.dj.on_request(yandexuid='0003', exp='user_feed_exp_name').respond(
            [
                DjModel(id='13', title='model{13}'),
                DjModel(id='11', title='model{11}'),
                DjModel(id='12', title='model{12}'),
            ]
        )

    def test_dj(self):
        """
        Проверяем, что под флагом market_dj_exp_for_user_feed плейс user_feed ходит в dj за моделями
        и сохраняет их порядок на выдаче
        """
        response = self.report.request_json(
            'place=user_feed&yandexuid=0003&rearr-factors=market_dj_exp_for_user_feed=user_feed_exp_name'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"entity": "offer", "model": {"id": 13}},
                    {"entity": "offer", "model": {"id": 12}},
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {"entity": "offer", "model": {"id": 11}},
                ]
            },
        )


if __name__ == '__main__':
    main()

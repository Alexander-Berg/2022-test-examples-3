#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MnPlace, Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Greater, Round, NoKey

from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.shops += [
            Shop(fesh=10, priority_region=213, regions=[213]),
            Shop(fesh=11, priority_region=213, regions=[213]),
            Shop(fesh=12, priority_region=213, regions=[213]),
            Shop(fesh=13, priority_region=213, regions=[213]),
            Shop(fesh=14, priority_region=213, regions=[213]),
            Shop(fesh=15, priority_region=213, regions=[213]),
            Shop(fesh=16, priority_region=213, regions=[213]),
        ]

        # Для задания вендорских мин ставок: minVendorBid в данном тесте будет равна цена / 1000
        cls.index.offers += [
            Offer(title='offer301', hid=1, fesh=10, hyperid=301, price=1000),
            Offer(title='offer302', hid=1, fesh=12, hyperid=302, price=2000),
            Offer(title='offer303', hid=1, fesh=11, hyperid=303, price=3000),
            Offer(title='offer304', hid=1, fesh=10, hyperid=304, price=1000),
            Offer(title='offer305', hid=1, fesh=10, hyperid=305, price=2000),
            Offer(title='offer306', hid=1, fesh=11, hyperid=306, price=3000),
            Offer(title='offer307', hid=1, fesh=10, hyperid=307, price=4000),
        ]

        # Для наглядности единственное, что будет играть роль при ранжировании моделей - вендорская ставка
        # (разница стоимость клика и минимальной ставки)
        cls.index.models += [
            Model(title='model301', hyperid=301, hid=1, vendor_id=1, vbid=2, randx=1),
            Model(title='model302', hyperid=302, hid=1, vendor_id=2, vbid=5),
            Model(title='model303', hyperid=303, hid=1, vendor_id=3, vbid=0),
            Model(title='model304', hyperid=304, hid=1, vendor_id=1, vbid=5, randx=1),
            Model(title='model305', hyperid=305, hid=1, vendor_id=2, vbid=6, randx=2),
            Model(title='model306', hyperid=306, hid=1, vendor_id=3, vbid=9),
            Model(title='model307', hyperid=307, hid=1, vendor_id=1, vbid=3, randx=2),
            # Модели без офферов окажутся в последней группе и будут соревноваться между собой
            Model(title='model308', hyperid=308, hid=1, vendor_id=2, vbid=0),
            Model(title='model309', hyperid=309, hid=1, vendor_id=3, vbid=10),
            Model(title='model310', hyperid=310, hid=1, vendor_id=4, vbid=1),
        ]

    def _test_auction_autobroker_basic_prime(self, rearr=''):
        """Проверяем, что на прайме автоматически включается режим Cpc для автоброкера модельных ставок:
        Проверяем работу автоброкера модельных ставок на прайме (для моделей 306, 302, 309 цена клика уменьшена)
        Проверяем, что для модели 307 ненулевая ставка меньше минимальной ставка поднята до минимальной,
        для моделей 303 и 308 нулевая ставка осталась нулевой
        """
        response = self.report.request_json('place=prime&hid=1&rids=213&show-urls=external&debug=1&numdoc=20' + rearr)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "model306"},
                        "debug": {
                            "sale": {"vBid": 9, "vendorClickPrice": 8},
                            "properties": {"VBID": "9", "MIN_VBID": "4"},
                        },
                    },
                    {
                        "titles": {"raw": "model305"},
                        "debug": {
                            "sale": {"vBid": 6, "vendorClickPrice": 6},
                            "properties": {"VBID": "6", "MIN_VBID": "3"},
                        },
                    },
                    {
                        "titles": {"raw": "model304"},
                        "debug": {
                            "sale": {"vBid": 5, "vendorClickPrice": 5},
                            "properties": {"VBID": "5", "MIN_VBID": "2"},
                        },
                    },
                    {
                        "titles": {"raw": "model302"},
                        "debug": {
                            "sale": {"vBid": 5, "vendorClickPrice": 4},
                            "properties": {"VBID": "5", "MIN_VBID": "3"},
                        },
                    },
                    {
                        "titles": {"raw": "model307"},
                        "debug": {
                            "sale": {"vBid": 4, "vendorClickPrice": 4},
                            "properties": {"VBID": "4", "MIN_VBID": "4"},
                        },
                    },
                    {
                        "titles": {"raw": "model301"},
                        "debug": {
                            "sale": {"vBid": 2, "vendorClickPrice": 1},
                            "properties": {"VBID": "2", "MIN_VBID": "2"},
                        },
                    },
                    {
                        "titles": {"raw": "model303"},
                        "debug": {
                            "sale": {"vBid": 0, "vendorClickPrice": 0},
                            "properties": {"VBID": "0", "MIN_VBID": "4"},
                        },
                    },
                ]
            },
            preserve_order=True,
        )
        return

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "model309"},
                        "debug": {
                            "sale": {"vBid": 10, "vendorClickPrice": 2},
                            "properties": {"VBID": "10", "MIN_VBID": "1"},
                        },
                    },
                    {
                        "titles": {"raw": "model310"},
                        "debug": {
                            "sale": {"vBid": 1, "vendorClickPrice": 1},
                            "properties": {"VBID": "1", "MIN_VBID": "1"},
                        },
                    },
                    {
                        "titles": {"raw": "model308"},
                        "debug": {
                            "sale": {"vBid": 0, "vendorClickPrice": 0},
                            "properties": {"VBID": "0", "MIN_VBID": "1"},
                        },
                    },
                ]
            },
            preserve_order=True,
        )

    def test_auction_autobroker_basic_prime(self):
        self._test_auction_autobroker_basic_prime()

    def test_auction_autobroker_basic_prime_search_rank_min_bid(self):
        """Проверим, что если использовать в автоброкере "старые" поисковые ставки (а их там и надо использовать), то все тоже будет ок"""
        self._test_auction_autobroker_basic_prime(rearr='&rearr-factors=market_use_old_min_bids_on_meta=1;')

    @classmethod
    def prepare_collapsing(cls):
        cls.index.offers += [
            Offer(
                title='test_collapsing',
                hid=2,
                fesh=10,
                hyperid=400,
                price=4000,
                vendor_id=1,
                bid=10,
                vbid=15,
                datasource_id=1,
                ts=1001,
            ),
            Offer(
                title='test_collapsing',
                hid=2,
                fesh=12,
                hyperid=400,
                price=4000,
                vendor_id=1,
                bid=13,
                vbid=13,
                datasource_id=2,
                ts=1002,
            ),
            Offer(
                title='test_collapsing',
                hid=2,
                fesh=11,
                hyperid=400,
                price=4000,
                vendor_id=1,
                bid=14,
                vbid=11,
                datasource_id=3,
                ts=1003,
            ),
            Offer(
                title='test_collapsing',
                hid=2,
                fesh=13,
                hyperid=400,
                price=4000,
                vendor_id=1,
                bid=15,
                vbid=7,
                datasource_id=4,
                ts=1004,
            ),
            Offer(
                title='test_collapsing',
                hid=2,
                fesh=14,
                hyperid=401,
                price=6000,
                vendor_id=1,
                bid=16,
                vbid=6,
                datasource_id=1,
                ts=1005,
            ),
            Offer(
                title='test_collapsing',
                hid=2,
                fesh=15,
                hyperid=401,
                price=6000,
                vendor_id=1,
                bid=17,
                vbid=5,
                datasource_id=2,
                ts=1006,
            ),
            Offer(
                title='test_collapsing',
                hid=2,
                fesh=16,
                hyperid=402,
                price=3000,
                vendor_id=1,
                bid=21,
                vbid=12,
                datasource_id=3,
                ts=1007,
            ),
        ]

        for i in range(1, 8):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000 + i).respond(0.5 - 0.001 * i)

        cls.index.models += [
            Model(hyperid=400, hid=2, vendor_id=1, datasource_id=10, vbid=0),  # minVendorBid = 4
            Model(hyperid=401, hid=2, vendor_id=1, datasource_id=11, vbid=34),  # minVendorBid = 6
            Model(hyperid=402, hid=2, vendor_id=1, datasource_id=12, vbid=20),
        ]

    @skip("experimental flags will break, see https://st.yandex-team.ru/MARKETOUT-40044")
    def test_collapsing(self):
        """Модельные ставки участвуют в автоброкере для схлопнутых офферов"""

        response = self.report.request_json(
            'place=prime&text=test_collapsing&debug=1&allow-collapsing=1&rids=213&pp=7'
            '&rearr-factors=market_use_vendor_bid_for_models_on_text_search=1'
        )

        self.assertFragmentIn(response, {"search": {"total": 3, "totalOffers": 0, "totalModels": 3}})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 401,
                        "debug": {
                            "isCollapsed": True,
                            "sale": {"vBid": 34, "vendorClickPrice": 22},
                            "tech": {"auctionMultiplier": Round(1.167)},
                            "properties": {"AUCTION_MULTIPLIER": NoKey("AUCTION_MULTIPLIER")},
                        },
                    },
                    {
                        "id": 402,
                        "debug": {
                            "sale": {"vBid": 20, "vendorClickPrice": 3},
                            "tech": {"auctionMultiplier": Round(1.117)},
                        },
                    },
                    {
                        "id": 400,
                        "debug": {
                            "sale": {"vBid": 0, "vendorClickPrice": 0},
                            "tech": {"auctionMultiplier": "1"},
                        },
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @skip("experimental flags will break, see https://st.yandex-team.ru/MARKETOUT-40044")
    def test_collapsing_with_model_vendor_auction(self):
        """MARKETOUT-30048
        Проверяем, что для моделей, которые появилиь в выдаче из-за схлопывания офферов
        На мете vbid для аукциона VENDOR_MODEL берется от модели, в которую схлопнулся оффер
        На базовых vbid модели у офферов нет, поэтому там нет аукциона
        """

        for rearr in [
            '&rearr-factors=market_use_vendor_bid_for_models_on_text_search=1;market_use_vendor_model_bid_on_meta=1',
            '&rearr-factors=market_force_use_vendor_bid=1',
        ]:
            response = self.report.request_json(
                'place=prime&text=test_collapsing&debug=1&allow-collapsing=1&rids=213&pp=7' + rearr
            )
            self.assertFragmentIn(response, {"search": {"total": 3, "totalOffers": 0, "totalModels": 3}})
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "id": 401,
                            "debug": {
                                "isCollapsed": True,
                                "sale": {"vBid": 34, "vendorClickPrice": 22},
                                "tech": {"auctionMultiplier": Round(1.167)},
                                "properties": {"AUCTION_MULTIPLIER": NoKey("AUCTION_MULTIPLIER")},
                            },
                        },
                        {"id": 402, "debug": {"sale": {"vBid": 20, "vendorClickPrice": 3}}},
                        {"id": 400, "debug": {"sale": {"vBid": 0, "vendorClickPrice": 0}}},
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

    @classmethod
    def prepare_cpm_rounding(cls):
        """Зададим модели с большими ставками, на этом уровне увеличение ставки дает небольшой прирост cpm"""
        cls.index.models += [
            Model(ts=902, hyperid=902, hid=3, vendor_id=3, vbid=201, datasource_id=3, randx=2),
            Model(ts=903, hyperid=903, hid=3, vendor_id=1, vbid=500, datasource_id=1, randx=3),
        ]

    def test_cpm_rounding(self):
        """Проверим, что CPM при работе автоброкера ставок на модель приводится к целому числу
        (как и при расчете релевантности)
         -  для модели 903 с излишне высокой ставкой, цена клика значительно больше, чем ставка предыдущ. модели + 1
        """
        response = self.report.request_json('place=prime&rids=213&hid=3&debug=da')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 903, "debug": {"sale": {"vBid": 500, "vendorClickPrice": Greater(201)}}},
                    {"id": 902, "debug": {"sale": {"vBid": 201, "vendorClickPrice": 1}}},
                ]
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()

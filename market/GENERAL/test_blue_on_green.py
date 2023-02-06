#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DeliveryBucket,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    ReferenceShop,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import Absent, LikeUrl
import json


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)

        cls.index.shops += [
            Shop(fesh=1),
            Shop(fesh=2),  # reference
            Shop(fesh=3),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
            ),
            Shop(fesh=5),
            Shop(fesh=6),
            Shop(fesh=7),
        ]

        cls.index.shops += [
            Shop(
                fesh=100,
                datafeed_id=100,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.reference_shops += [
            ReferenceShop(hid=1, fesh=2),
        ]

        cls.index.outlets += [
            Outlet(fesh=100, region=213, point_type=Outlet.FOR_PICKUP, point_id=1001),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=100,
                carriers=[101],
                options=[PickupOption(outlet_id=1001)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=1),
            Model(hyperid=102, hid=1),
            Model(hyperid=103, hid=1),
            Model(hyperid=104, hid=1),
            Model(hyperid=105, hid=1),
            Model(hyperid=106, hid=1),
            Model(hyperid=107, hid=1),
        ]

        # No blue offers at all
        cls.index.offers += [
            Offer(hyperid=101, fesh=1, price=100, ts=101001),  # DO
            Offer(hyperid=101, fesh=2, price=300),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101001).respond(0.02)

        # DO is not blue
        # Blue is not cheapest among referenced
        cls.index.offers += [
            Offer(hyperid=102, fesh=1, price=100, bid=1000, ts=102001),  # DO
            Offer(hyperid=102, fesh=2, price=300, bid=1000, ts=102002),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=102,
                sku=1102,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=100, waremd5='Sku1Price5-IiLVm1Goleg', ts=102003),
                ],
                pickup_buckets=[5001],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102002).respond(0.019)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102003).respond(0.018)

        # DO is not blue
        # Blue is cheapest among referenced
        cls.index.offers += [
            Offer(hyperid=103, fesh=1, price=100, ts=103001),  # DO
            Offer(hyperid=103, fesh=2, price=300),
            Offer(hyperid=103, fesh=3, price=300),
            Offer(hyperid=103, fesh=5, price=300),
            Offer(hyperid=103, fesh=6, price=300),
            Offer(hyperid=103, fesh=7, price=300),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=103,
                sku=1103,
                blue_offers=[
                    BlueOffer(price=200, vat=Vat.NO_VAT, feedid=100, ts=103004),
                ],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103004).respond(0.015)

        # DO is blue
        cls.index.offers += [
            Offer(hyperid=104, fesh=2, price=300),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=104,
                sku=1104,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=100, ts=104004),
                ],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104004).respond(0.02)

        # DO is not blue
        # No referenced shops at all
        cls.index.offers += [
            Offer(hyperid=105, fesh=1, price=100, ts=105001),  # DO
            Offer(hyperid=105, fesh=3, price=300),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=105,
                sku=1105,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=100),
                ],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105001).respond(0.02)

        # offers for 'no-relax' test
        cls.index.offers += [
            Offer(hyperid=110, fesh=1, price=100, ts=110001),
            Offer(hyperid=110, fesh=3, price=120, ts=110003),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=110,
                sku=1110,
                blue_offers=[
                    BlueOffer(price=110, vat=Vat.NO_VAT, feedid=100),
                ],
                pickup=False,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 110001).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 110003).respond(0.02)

        # offers for MARKETOUT-18738
        # DO and Blue offers have equal prices
        cls.index.offers += [Offer(hyperid=111, fesh=1, price=100, ts=111001)]  # DO
        cls.index.mskus += [
            MarketSku(
                hyperid=111,
                sku=1111,
                blue_offers=[
                    BlueOffer(price=100, vat=Vat.NO_VAT, feedid=100, ts=111004),
                ],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 111001).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 111004).respond(0.02)

        # offers for MARKETOUT-18748 (no offers from reference shops)
        # Blue offer price is greater than average
        cls.index.offers += [
            Offer(hyperid=106, fesh=1, price=100, randx=101),
            Offer(hyperid=106, fesh=3, price=800, randx=100),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=106,
                sku=1106,
                randx=0,
                blue_offers=[BlueOffer(price=460, vat=Vat.NO_VAT, feedid=100)],
            )
        ]
        # Blue offer price is less than average
        cls.index.offers += [
            Offer(hyperid=107, fesh=1, price=100, randx=101),
            Offer(hyperid=107, fesh=3, price=800, randx=100),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=107,
                sku=1107,
                randx=0,
                blue_offers=[BlueOffer(price=440, vat=Vat.NO_VAT, feedid=100)],
            )
        ]

        # several MSKU offers for a single model
        cls.index.offers += [
            Offer(hyperid=108, fesh=1, price=200, bid=2000, ts=108001),
            Offer(hyperid=108, fesh=2, price=200, bid=1000, ts=108002),
        ]
        cls.index.mskus += [
            # more CPM but expensive
            MarketSku(hyperid=108, sku=1108, bid=500, blue_offers=[BlueOffer(price=200, feedid=100, ts=108003)]),
            # less CPM but cheaper
            MarketSku(hyperid=108, sku=2108, bid=100, blue_offers=[BlueOffer(price=199, feedid=100, ts=108004)]),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 108001).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 108002).respond(0.029)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 108003).respond(0.028)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 108004).respond(0.027)

    def check_offer(self, response, shop_id, benefit, pp, is_blue_offer):
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop': {
                    'id': shop_id,
                },
                'benefit': {
                    'type': benefit,
                },
            },
        )
        self.show_log.expect(shop_id=shop_id, pp=pp, is_blue_offer=1 if is_blue_offer else 0)

    def test_default_list__offers_order(self):
        """
        Проверяем, что по умолчанию приходит только ДО
        """
        response = self.report.request_json(
            'place=productoffers&show-urls=external,cpa&pp=6&hid=1'
            '&hyperid=103'
            '&offers-set=defaultList'
            '&rgb=green_with_blue'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'benefit': {
                        'type': 'cheapest',
                    },
                },
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    'benefit': {
                        'type': 'recommended',
                    },
                },
            ],
        )

    def test_prefer_blue_in_offers_list_experiment(self):
        """
        Проверяем, что с prefer-own-marketplace синий оффер выходит на первое место в списке офферов.
        """
        request = (
            'place=productoffers&show-urls=external,cpa&hid=1'
            '&hyperid=102'
            '&rgb=green_with_blue&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0'
        )
        # Baseline
        _ = self.report.request_json(request)
        self.show_log.expect(shop_id=4, is_blue_offer=1, position=3)

        # Test
        _ = self.report.request_json(request + '&prefer-own-marketplace=1')
        self.show_log.expect(shop_id=4, is_blue_offer=1, position=1)

    def test_prefer_cheapest_blue_in_offers_list(self):
        """
        Проверяем, что с prefer-own-marketplace самый дешевый синий оффер выходит на первое место в списке офферов.
        """
        request = 'place=productoffers&show-urls=external,cpa&hyperid=108&rgb=green_with_blue&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0'

        # Baseline
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 1}},
                    {"shop": {"id": 2}},
                    {"shop": {"id": 4}, "marketSku": "1108"},
                    {"shop": {"id": 4}, "marketSku": "2108"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Test
        response = self.report.request_json(request + '&prefer-own-marketplace=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 4}, "marketSku": "2108"},
                    {"shop": {"id": 4}, "marketSku": "1108"},
                    {"shop": {"id": 1}},
                    {"shop": {"id": 2}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # with group by shop
        response = self.report.request_json(request + '&prefer-own-marketplace=1&grhow=shop')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"shop": {"id": 4}, "marketSku": "2108"},
                    {"shop": {"id": 1}},
                    {"shop": {"id": 2}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def check_blue_offer_url_with_clid(self, place, clid="", rgb="", extra=""):
        """
        Проверяем, что для белого маркета генерируются переходы на синий с параметром clid, а на синем не генерируются.
        """
        req = "place={}&show-urls=direct&rids=213&regset=2&offerid=Sku1Price5-IiLVm1Goleg&rgb={}".format(place, rgb)
        req += extra
        response = self.report.request_json(req)
        url_matcher = LikeUrl(
            url_path="/product/1102", url_params=Absent() if rgb == "blue" or clid == "" else {"clid": clid}
        )
        self.assertFragmentIn(
            response, {"urls": {"direct": Absent() if (rgb == "blue" and place == "prime") else url_matcher}}
        )

    def check_blue_offer_url_for_widget(self, place, rgb):
        # with clid for widget
        self.check_blue_offer_url_with_clid(place=place, rgb=rgb, clid="777", extra="&clid=777&client=widget")
        self.check_blue_offer_url_with_clid(
            place=place, rgb=rgb, clid="777", extra="&clid=777&content-api-client=11652"
        )
        self.check_blue_offer_url_with_clid(
            place=place, rgb=rgb, clid="777", extra="&clid=777&content-api-client=14012"
        )
        # with clid-vid for widget (see MARKETOUT-22408)
        self.check_blue_offer_url_with_clid(
            place=place, rgb=rgb, clid="777", extra="&clid=777-666&content-api-client=14012"
        )

    def test_blue_offer_url_for_widget_prime(self):
        self.check_blue_offer_url_with_clid(place='prime', rgb="")

    def test_blue_offer_url_for_widget_prime_green(self):
        self.check_blue_offer_url_with_clid(place='prime', rgb="green")

    def test_blue_offer_url_for_widget_prime_green_on_blue(self):
        self.check_blue_offer_url_with_clid(place='prime', rgb="green_on_blue")

    def test_blue_offer_url_for_widget_prime_blue(self):
        self.check_blue_offer_url_with_clid(
            place='prime', rgb="blue", extra="&rearr-factors=market_money_return_vendor_urls_even_for_zero_v_bid=0"
        )

    def test_blue_offer_url_for_widget_offerinfo(self):
        self.check_blue_offer_url_with_clid(place='offerinfo', rgb="")

    def test_blue_offer_url_for_widget_offerinfo_green(self):
        self.check_blue_offer_url_with_clid(place='offerinfo', rgb="green")

    def test_blue_offer_url_for_widget_offerinfo_green_on_blue(self):
        self.check_blue_offer_url_with_clid(place='offerinfo', rgb="green_on_blue")

    def test_blue_offer_url_for_widget_offerinfo_blue(self):
        self.check_blue_offer_url_with_clid(place='offerinfo', rgb="blue")

    def test_removing_blue_offer_from_top_6__not_DO(self):
        """
        Проверяем, что для модели у которой синий оффер не попадает в ДО - оффер остаётся в топ-6
        """
        response = self.report.request_json(
            'place=productoffers&hid=1' '&hyperid=102' '&offers-set=defaultList,list' '&rgb=green_with_blue'
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop': {'id': 4},
                'benefit': Absent(),
            },
        )

    def test_removing_blue_offer_from_top_6__DO(self):
        """
        Проверяем, что для модели у которой синий оффер попадает в ДО - оффер остаётся в топ-6
        """
        response = self.report.request_json(
            'place=productoffers&hid=1' '&hyperid=104' '&offers-set=defaultList,list' '&rgb=green_with_blue'
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop': {'id': 4},
                'benefit': Absent(),
            },
        )

    def check_blue_offer_url_with_pof(self, place, client=None, rgb=None, expected=None):
        pof = {'clid': [5, 6], 'mclid': 7, 'distr_type': 8, 'vid': 9}
        base_request = "place={}&offerid=Sku1Price5-IiLVm1Goleg&show-urls=direct&rids=213&regset=2&pof={}".format(
            place, json.dumps(pof)
        )
        if client:
            base_request += "&client={}".format(client)
        if rgb:
            base_request += "&rgb={}".format(rgb)

        response = self.report.request_json(base_request)
        self.assertFragmentIn(response, {"urls": {"direct": expected}})

    def test_blue_offer_url_pof_prime(self):
        self.check_blue_offer_url_with_pof(
            'prime',
            client='widget',
            expected=LikeUrl.of(
                "https://pokupki.market.yandex.ru/product/1102?offerid=Sku1Price5-IiLVm1Goleg&mclid=7&distr_type=8&vid=9"
            ),
        )

    def test_blue_offer_url_pof_offerinfo(self):
        self.check_blue_offer_url_with_pof(
            'offerinfo',
            client='widget',
            expected=LikeUrl.of(
                "https://pokupki.market.yandex.ru/product/1102?offerid=Sku1Price5-IiLVm1Goleg&mclid=7&distr_type=8&vid=9"
            ),
        )

    def test_blue_offer_url_pof_sku_offers(self):
        self.check_blue_offer_url_with_pof(
            'sku_offers&market-sku=1102',
            client='widget',
            expected=LikeUrl.of(
                "https://pokupki.market.yandex.ru/product/1102?offerid=Sku1Price5-IiLVm1Goleg&mclid=7&distr_type=8&vid=9"
            ),
        )

    def test_blue_offer_url_pof_sku_offers_null_client(self):
        self.check_blue_offer_url_with_pof(
            'sku_offers&market-sku=1102',
            expected=LikeUrl.of("https://pokupki.market.yandex.ru/product/1102?offerid=Sku1Price5-IiLVm1Goleg"),
        )

    def test_blue_offer_url_pof_sku_offers_blue(self):
        self.check_blue_offer_url_with_pof(
            'sku_offers&market-sku=1102',
            expected=LikeUrl.of("https://pokupki.market.yandex.ru/product/1102?offerid=Sku1Price5-IiLVm1Goleg"),
        )


if __name__ == '__main__':
    main()

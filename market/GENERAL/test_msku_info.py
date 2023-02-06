#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime
from core.types import BlueOffer, HyperCategory, MarketSku, Model, Offer, Promo, PromoMSKU, PromoType, Shop, Vat
from core.testcase import TestCase, main
from core.matcher import Absent

import market.idx.datacamp.proto.offer.OfferMeta_pb2 as OfferMeta


class T(TestCase):
    @classmethod
    def prepare_demand_prediction(cls):
        cls.index.shops += [
            Shop(fesh=1, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=1001),
            Shop(fesh=2001, datafeed_id=2001, blue='REAL', supplier_type=Shop.FIRST_PARTY),
            Shop(fesh=2002, datafeed_id=2002, blue='REAL', supplier_type=Shop.THIRD_PARTY),
            Shop(fesh=2003, datafeed_id=2003, blue='REAL', supplier_type=Shop.THIRD_PARTY),
            Shop(fesh=2004, datafeed_id=20041, blue='REAL', supplier_type=Shop.THIRD_PARTY, warehouse_id=145),
            Shop(fesh=2004, datafeed_id=20042, blue='REAL', supplier_type=Shop.THIRD_PARTY, warehouse_id=147),
        ]

        cls.index.hypertree = [
            HyperCategory(
                hid=10000,
                children=[
                    HyperCategory(
                        hid=11000,
                        children=[HyperCategory(hid=11100), HyperCategory(hid=11200), HyperCategory(hid=11300)],
                    ),
                    HyperCategory(hid=12000),
                ],
            )
        ]

        cls.index.models += [
            Model(hyperid=1001, hid=1),  # absent in category tree
            Model(hyperid=1002, hid=11200),  # ancestry categories: 11200, 11000, 10000
            Model(hyperid=1003, hid=11300),  # ancestry categories: 11300, 11000, 10000
            Model(hyperid=1005, hid=1),  # absent in category tree
            Model(hyperid=1006, hid=1),
        ]

        cls.index.offers += [  # Non-Blue offers (shouldn't be considered)
            Offer(fesh=1001, hyperid=1001, price=14, sku=1),
            Offer(fesh=1001, hyperid=1002, price=14, sku=2),
            Offer(fesh=1001, hyperid=1003, price=14),
        ]

        cls.index.mskus += [
            # test_some_offers_with_model_id
            MarketSku(
                hyperid=1001,
                sku=1,
                blue_offers=[
                    BlueOffer(price=100, vat=Vat.VAT_18, offerid='1', feedid=2001),
                    BlueOffer(price=105, vat=Vat.VAT_18, offerid='1', feedid=2002),
                    BlueOffer(price=110, vat=Vat.VAT_18, offerid='1', feedid=2003),
                ],
            ),
            # test_one_offer_with_model_id
            MarketSku(
                hyperid=1002,
                sku=2,
                blue_offers=[
                    BlueOffer(price=100, vat=Vat.VAT_18, offerid='2', feedid=2001),
                ],
            ),
            # test_no_offers
            MarketSku(hyperid=1003, sku=3, blue_offers=[]),
            # test_offers_without_model_id
            MarketSku(
                sku=4,
                blue_offers=[
                    BlueOffer(price=100, vat=Vat.VAT_18, offerid='4', feedid=2001),
                    BlueOffer(price=105, vat=Vat.VAT_18, offerid='4', feedid=2002),
                ],
                hid=12000,
            ),
            # test_best_offer_with_model_id_
            MarketSku(
                hyperid=1005,
                sku=5,
                blue_offers=[
                    BlueOffer(price=100, offerid='5', feedid=2001),
                    BlueOffer(price=100, offerid='5', feedid=2002),
                    BlueOffer(price=100, offerid='5', feedid=2003),
                ],
            ),
            # test_out_of_stock_offers
            MarketSku(
                hyperid=1006,
                sku=6,
                blue_offers=[
                    BlueOffer(price=100, offerid='6', feedid=2001),
                    BlueOffer(
                        price=100,
                        offerid='6',
                        feedid=2002,
                        disabled_flags=T.build_offer_disabled_flags([OfferMeta.MARKET_STOCK]),
                    ),
                    BlueOffer(
                        price=100,
                        offerid='6',
                        feedid=2003,
                        disabled_flags=T.build_offer_disabled_flags([OfferMeta.MARKET_STOCK]),
                    ),
                ],
            ),
            # test_offers_from_different_warehouses
            MarketSku(
                hyperid=1007,
                sku=7,
                blue_offers=[
                    BlueOffer(price=100, offerid='7', feedid=20041),
                    BlueOffer(
                        price=100,
                        offerid='7',
                        feedid=20042,
                        disabled_flags=T.build_offer_disabled_flags([OfferMeta.MARKET_STOCK]),
                    ),
                ],
            ),
        ]

        cls.index.promos += [
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='TestPromo',
                start_date=datetime.datetime(2018, 1, 1),
                end_date=datetime.datetime(2030, 1, 1),
                mskus=[
                    PromoMSKU(
                        msku='5',
                        market_promo_price=80,
                        market_old_price=120,
                    ),
                ],
            ),
        ]

    @classmethod
    def build_offer_disabled_flags(cls, value_bits):
        """
        Построение флагов скрытия
        :param value_bits: Биты признака скрытия (младшие 16 бит). Значение 1 в бите => оффер скрыт по соответствующему
        номеру бита источнику из OfferMeta::DataSource.
        :return: Значение offer_disabled в индексе
        """
        result = 0
        for b in value_bits:
            result |= 1 << b
        return result

    def test_some_offers_with_model_id(self):
        """
        Проверяем, что для заданного msku в выдаче присутствет нужная информация о привязанных
        к msku синих офферах, и не содержится информации о зелёных (allow_different_len=False).
        """
        response = self.report.request_json("place=msku_info&market-sku=1")
        self.assertFragmentIn(
            response,
            {
                "ModelId": 1001,
                "CategoryIds": [1, 90401],
                "Offers": [
                    {"SupplierId": 2001, "Price": 100, "Type": "1"},
                    {"SupplierId": 2002, "Price": 105, "Type": "3"},
                    {"SupplierId": 2003, "Price": 110, "Type": "3"},
                ],
            },
            allow_different_len=False,
        )

    def test_one_offer_with_model_id(self):
        response = self.report.request_json("place=msku_info&market-sku=2")
        self.assertFragmentIn(
            response,
            {
                "ModelId": 1002,
                "CategoryIds": [11200, 11000, 10000],
                "Offers": [{"SupplierId": 2001, "Price": 100, "Type": "1"}],
            },
        )

    def test_msku_without_blue_offers(self):
        response = self.report.request_json("place=msku_info&market-sku=3", strict=False)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "ModelId": 1003,
                    "CategoryIds": [
                        11300,
                        11000,
                        10000,
                        90401,
                    ],
                    "Offers": [],
                }
            },
        )

    def test_offers_without_model_id(self):
        response = self.report.request_json("place=msku_info&market-sku=4")
        self.assertFragmentIn(
            response,
            {
                "ModelId": Absent(),
                "CategoryIds": [12000, 10000],
                "Offers": [
                    {"SupplierId": 2001, "Price": 100, "Type": "1"},
                    {"SupplierId": 2002, "Price": 105, "Type": "3"},
                ],
            },
        )

    def test_choose_best_offer(self):
        """Check if the place Best Offer is the same as 'sku_offers' returns"""

        def get_best_offer_feed_id(uid):
            response = self.report.request_json("place=sku_offers&market-sku=5" + uid)
            return int(response.root["search"]["results"][0]["offers"]["items"][0]["supplier"]["id"])

        for i in range(1, 20):
            uid = "&yandexuid={}".format(i)
            feed_id = get_best_offer_feed_id(uid)
            response = self.report.request_json("place=msku_info&market-sku=5&choose-best=da" + uid)
            self.assertFragmentIn(
                response, {"ModelId": 1005, "Offers": [{"SupplierId": feed_id}]}, allow_different_len=False
            )

    def test_info_with_promo(self):
        response = self.report.request_json("place=msku_info&market-sku=5&start-date=2018-01-01")
        self.assertFragmentIn(
            response,
            {"Promo": {"StartTimestamp": 1514764800, "EndTimestamp": 1893456000, "PromoPrice": 80, "OldPrice": 120}},
            allow_different_len=False,
        )

    def test_out_of_stock_offers(self):
        response = self.report.request_json("place=msku_info&market-sku=6&start-date=2018-01-01")
        self.assertFragmentIn(
            response,
            {
                "Offers": [
                    {"SupplierId": 2001, "OutOfStock": False},
                    {"SupplierId": 2002, "OutOfStock": True},  # out-of-stock
                    {"SupplierId": 2003, "OutOfStock": True},
                ]
            },
            allow_different_len=False,
        )

    def test_offers_of_different_warehouses(self):
        response = self.report.request_json("place=msku_info&market-sku=7")
        self.assertFragmentIn(
            response, {"Offers": [{"SupplierId": 2004, "OutOfStock": False, "Price": 100}]}, allow_different_len=False
        )


if __name__ == '__main__':
    main()

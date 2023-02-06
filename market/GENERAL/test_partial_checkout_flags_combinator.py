#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    BlueOffer,
    MarketSku,
    Model,
    Shop,
)
from core.testcase import TestCase, main
from core.combinator import make_offer_id, DeliveryStats

DEFAULT_REGION = 213
DEFAULT_WAREHOUSE = 172


class _EnabledFlag:
    fesh = 1
    data_feed = 1
    category = 1
    model = 1
    sku = 1
    offer_wmd5 = 'TestOfferPartial_____g'


class _DisabledFlag:
    fesh = 2
    data_feed = 2
    category = 2
    model = 2
    sku = 2
    offer_wmd5 = 'TestOfferNoPartial___g'


class _Shops:
    partial_shop = Shop(
        fesh=_EnabledFlag.fesh,
        datafeed_id=_EnabledFlag.data_feed,
        priority_region=DEFAULT_REGION,
        supplier_type=Shop.FIRST_PARTY,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=DEFAULT_WAREHOUSE,
        fulfillment_program=True,
    )
    no_partial_shop = Shop(
        fesh=_DisabledFlag.fesh,
        datafeed_id=_DisabledFlag.data_feed,
        priority_region=DEFAULT_REGION,
        supplier_type=Shop.FIRST_PARTY,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=DEFAULT_WAREHOUSE,
        fulfillment_program=False,
    )


class _Offers:
    partial_offer = BlueOffer(
        offerid="Partial",
        feedid=_Shops.partial_shop.datafeed_id,
        waremd5=_EnabledFlag.offer_wmd5,
        supplier_id=_Shops.partial_shop.fesh,
    )
    no_partial_offer = BlueOffer(
        offerid="NoPartial",
        feedid=_Shops.no_partial_shop.datafeed_id,
        waremd5=_DisabledFlag.offer_wmd5,
        supplier_id=_Shops.no_partial_shop.fesh,
    )


class _Mskus:
    partial_msku = MarketSku(hyperid=_EnabledFlag.model, sku=_EnabledFlag.sku, blue_offers=[_Offers.partial_offer])
    no_partial_msku = MarketSku(
        hyperid=_DisabledFlag.model, sku=_DisabledFlag.sku, blue_offers=[_Offers.no_partial_offer]
    )


def get_request(model, category):
    request = 'place=productoffers&rids=213&hyperid={}&pp=18&hid={}'.format(model, category)
    return request


class T(TestCase):
    """
    Проверяем что корректно отдается признак оффера с доступным частичным выкупом
    """

    @classmethod
    def prepare(cls):

        cls.index.models += [
            Model(hyperid=_EnabledFlag.model, hid=_EnabledFlag.category),
            Model(hyperid=_DisabledFlag.model, hid=_DisabledFlag.category),
        ]
        cls.index.mskus += [
            _Mskus.partial_msku,
            _Mskus.no_partial_msku,
        ]
        cls.index.shops += [
            _Shops.partial_shop,
            _Shops.no_partial_shop,
        ]
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.partial_offer, _Shops.partial_shop),
            courier_stats=DeliveryStats(cost=0, day_from=4, day_to=5),
            partial_delivery_available=True,
        )
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.no_partial_offer, _Shops.no_partial_shop),
            courier_stats=DeliveryStats(cost=0, day_from=4, day_to=5),
            partial_delivery_available=False,
        )

    def test_partial_offer_has_partial_flag(self):
        """
        Проверяем, что оффер с признаком частичного выкупа доступен для частичного выкупа
        """
        request = get_request(model=_EnabledFlag.model, category=_EnabledFlag.category)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'isPartialCheckoutAvailable': True,
                    'wareId': _EnabledFlag.offer_wmd5,
                }
            ],
            use_regex=True,
        )

    def test_no_partial_offer_has_not_partial_flag(self):
        """
        Проверяем, что оффер без признака частичного выкупа не доступен для частичного выкупа
        """
        request = get_request(model=_DisabledFlag.model, category=_DisabledFlag.category)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'isPartialCheckoutAvailable': False,
                    'wareId': _DisabledFlag.offer_wmd5,
                }
            ],
            use_regex=True,
        )

    def test_no_partial_offer_has_not_partial_flag_when_rear_is_false(self):
        """
        Проверяем, что оффер без признака частичного выкупа не доступен для частичного выкупа
        """
        request = get_request(model=_DisabledFlag.model, category=_DisabledFlag.category)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'isPartialCheckoutAvailable': False,
                    'wareId': _DisabledFlag.offer_wmd5,
                }
            ],
            use_regex=True,
        )


if __name__ == '__main__':
    main()

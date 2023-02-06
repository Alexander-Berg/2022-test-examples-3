#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

'''
Тестирование опций доставки для оферов Еды
'''

from core.types import (
    Offer,
    Shop,
)
from core.testcase import TestCase, main
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)
from core.types.express_partners import EatsWarehousesEncoder

HID = EATS_CATEG_ID


class SurgeInfo:
    def __init__(cls, delivery_time_minutes, delivery_price_min, delivery_price_max, free_delivery_threshold):
        cls.delivery_time_minutes = delivery_time_minutes
        cls.delivery_price_min = delivery_price_min
        cls.delivery_price_max = delivery_price_max
        cls.free_delivery_threshold = free_delivery_threshold


class _Currency:
    currency_1 = "EU"
    currency_2 = "RUR"


class _Shops:
    shop_eats_1 = Shop(
        business_fesh=1,
        fesh=11,
        datafeed_id=111,
        warehouse_id=111,
        cpa=Shop.CPA_REAL,
        is_eats=True,
    )

    shop_lavka = Shop(
        business_fesh=2,
        fesh=21,
        datafeed_id=121,
        warehouse_id=121,
        cpa=Shop.CPA_REAL,
        is_lavka=True,
    )


class _Offers:
    eats_offer_1 = Offer(
        waremd5=Offer.generate_waremd5('eats_offer_1'),
        hid=HID,
        shop=_Shops.shop_eats_1,
        is_eda_retail=True,
        is_express=True,
        offerid="eats_offer_1",
    )

    lavka_offer = Offer(
        waremd5=Offer.generate_waremd5('lavka_offer'),
        hid=HID,
        shop=_Shops.shop_lavka,
        is_lavka=True,
        is_express=True,
        offerid="lavka_offer",
    )


class _SugreInfoData:

    surgeInfo_1 = SurgeInfo(15, 0, 49, 999)
    surgeInfo_2 = SurgeInfo(31, 0, 299, 2999)


class T(TestCase):
    @classmethod
    def prepare_shops(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(HID, Stream.FMCG.value),
        ]
        cls.index.shops += [
            _Shops.shop_eats_1,
            _Shops.shop_lavka,
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _Offers.eats_offer_1,
            _Offers.lavka_offer,
        ]

    def test_eats_hyperlocality(self):
        '''
        Проверяем опции доствки офферов еды
        '''

        eats_wh_request = "&eats-warehouses-compressed={}"

        other_client = "&client=other"

        request_prime = "place=prime&hid={hid}&enable-foodtech-offers=eda_retail,lavka".format(hid=HID)

        def check(request, flags, offer, surgeInfo, currency):

            response = self.report.request_json(request + flags)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'delivery': {
                        'options': [
                            {
                                'deliveryTimeMinutes': surgeInfo.delivery_time_minutes,
                                'deliveryPriceMin': {'value': surgeInfo.delivery_price_min, 'currency': currency},
                                'deliveryPriceMax': {'value': surgeInfo.delivery_price_max, 'currency': currency},
                                'freeDeliveryThreshold': {
                                    'value': surgeInfo.free_delivery_threshold,
                                    'currency': currency,
                                },
                            }
                        ]
                    },
                },
                allow_different_len=False,
            )

        # проверяем что данные по опциям доставки из surge есть и корректны на выдаче
        wh = (
            EatsWarehousesEncoder()
            .add_warehouse(
                wh_id=_Shops.shop_eats_1.warehouse_id,
                delivery_time_minutes=_SugreInfoData.surgeInfo_1.delivery_time_minutes,
                delivery_price_min=_SugreInfoData.surgeInfo_1.delivery_price_min,
                delivery_price_max=_SugreInfoData.surgeInfo_1.delivery_price_max,
                free_delivery_threshold=_SugreInfoData.surgeInfo_1.free_delivery_threshold,
            )
            .set_currency(currency=_Currency.currency_1)
            .encode()
        )

        check(
            request_prime,
            eats_wh_request.format(wh) + other_client,
            _Offers.eats_offer_1,
            _SugreInfoData.surgeInfo_1,
            _Currency.currency_1,
        )

        wh = (
            EatsWarehousesEncoder()
            .add_warehouse(
                wh_id=_Shops.shop_eats_1.warehouse_id,
                delivery_time_minutes=_SugreInfoData.surgeInfo_2.delivery_time_minutes,
                delivery_price_min=_SugreInfoData.surgeInfo_2.delivery_price_min,
                delivery_price_max=_SugreInfoData.surgeInfo_2.delivery_price_max,
                free_delivery_threshold=_SugreInfoData.surgeInfo_2.free_delivery_threshold,
            )
            .add_warehouse(
                wh_id=_Shops.shop_lavka.warehouse_id,
                delivery_time_minutes=_SugreInfoData.surgeInfo_1.delivery_time_minutes,
                delivery_price_min=_SugreInfoData.surgeInfo_1.delivery_price_min,
                delivery_price_max=_SugreInfoData.surgeInfo_1.delivery_price_max,
                free_delivery_threshold=_SugreInfoData.surgeInfo_1.free_delivery_threshold,
            )
            .set_currency(currency=_Currency.currency_2)
            .encode()
        )

        check(
            request_prime,
            eats_wh_request.format(wh) + other_client,
            _Offers.eats_offer_1,
            _SugreInfoData.surgeInfo_2,
            _Currency.currency_2,
        )
        check(
            request_prime,
            eats_wh_request.format(wh) + other_client,
            _Offers.lavka_offer,
            _SugreInfoData.surgeInfo_1,
            _Currency.currency_2,
        )


if __name__ == '__main__':
    main()

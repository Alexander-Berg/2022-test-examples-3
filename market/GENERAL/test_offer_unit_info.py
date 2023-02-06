#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    GLParam,
    GLType,
    GLValue,
    MarketSku,
    Model,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    OfferDimensions,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    generate_dsbs,
)
from core.matcher import Absent


TEST_HID = 1234
TEST_OFFER_WARE = Offer.generate_waremd5("test")
TEST_OFFER_SKU = 5345
TEST_OFFER_VIRTUAL_SKU = 2000000004335
TEST_OFFER_VIRTUAL_WARE = Offer.generate_waremd5("virtual")

MAIN_UNIT_PARAM_ID = 28763635
MAIN_UNIT_VALUE = "уп"

ADDITIONAL_UNIT_PARAM_ID = 28762958
ADDITIONAL_UNIT_VALUE = "м²"

CONVERT_COEFFICIENT_PARAM_ID = 28763686

TEST_OFFER_WARE_NO_COEF = Offer.generate_waremd5("test_nocoef")
TEST_OFFER_SKU_NO_COEF = 5348

TEST_OFFER_WARE_CHEAP = Offer.generate_waremd5("test_cheap")
TEST_OFFER_SKU_CHEAP = 5349

TEST_OFFER_WARE_NO_MAIN = Offer.generate_waremd5("test_no_main")
TEST_OFFER_SKU_NO_MAIN = 5350


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(hid=TEST_HID, hyperid=TEST_HID, title='Model test'),
        ]
        cls.settings.default_search_experiment_flags = ["cpa_enabled_countries=149"]

        dsbs_shop = Shop(
            fesh=42,
            datafeed_id=4240,
            priority_region=213,
            regions=[213],
            name='Все мечи',
            client_id=11,
            cpa=Shop.CPA_REAL,
            warehouse_id=64858,
        )
        blue_shop = Shop(
            fesh=4,
            datafeed_id=4,
            priority_region=213,
            name='blue_shop',
            supplier_type=Shop.THIRD_PARTY,
            blue='REAL',
            warehouse_id=145,
        )
        cls.index.shops += [dsbs_shop, blue_shop]

        cls.index.regiontree = [
            Region(
                rid=149,
                name='Беларусь',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=20729, name='Бобруйск'),
                ],
            ),
            Region(rid=213, name='Москва'),
        ]

        cls.index.gltypes += [
            GLType(
                hid=TEST_HID,
                xslname="unit_main",
                param_id=MAIN_UNIT_PARAM_ID,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=956, text=MAIN_UNIT_VALUE)],
            ),
            GLType(
                hid=TEST_HID,
                xslname="unit_add",
                param_id=ADDITIONAL_UNIT_PARAM_ID,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=595, text=ADDITIONAL_UNIT_VALUE)],
            ),
            GLType(hid=TEST_HID, xslname="conv_coef", param_id=CONVERT_COEFFICIENT_PARAM_ID, gltype=GLType.NUMERIC),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=TEST_OFFER_SKU,
                hid=TEST_HID,
                glparams=[
                    GLParam(param_id=MAIN_UNIT_PARAM_ID, value=956),
                    GLParam(param_id=ADDITIONAL_UNIT_PARAM_ID, value=595),
                    GLParam(param_id=CONVERT_COEFFICIENT_PARAM_ID, value=3),
                ],
                blue_offers=[
                    BlueOffer(
                        shop=blue_shop,
                        price=1000,
                        waremd5=TEST_OFFER_WARE,
                        weight=5,
                        blue_weight=5,
                        dimensions=OfferDimensions(height=10, length=30, width=20),
                        blue_dimensions=OfferDimensions(height=10, length=30, width=20),
                    )
                ],
            ),
            MarketSku(
                sku=TEST_OFFER_SKU_NO_COEF,
                hid=TEST_HID,
                glparams=[
                    GLParam(param_id=MAIN_UNIT_PARAM_ID, value=956),
                ],
                blue_offers=[
                    BlueOffer(
                        shop=blue_shop,
                        price=1000,
                        waremd5=TEST_OFFER_WARE_NO_COEF,
                        weight=5,
                        blue_weight=5,
                        dimensions=OfferDimensions(height=10, length=30, width=20),
                        blue_dimensions=OfferDimensions(height=10, length=30, width=20),
                    )
                ],
            ),
            MarketSku(
                sku=TEST_OFFER_SKU_CHEAP,
                hid=TEST_HID,
                glparams=[
                    GLParam(param_id=MAIN_UNIT_PARAM_ID, value=956),
                    GLParam(param_id=ADDITIONAL_UNIT_PARAM_ID, value=595),
                    GLParam(param_id=CONVERT_COEFFICIENT_PARAM_ID, value=1500),
                ],
                blue_offers=[
                    BlueOffer(
                        shop=blue_shop,
                        price=50,
                        waremd5=TEST_OFFER_WARE_CHEAP,
                        weight=5,
                        blue_weight=5,
                        dimensions=OfferDimensions(height=10, length=30, width=20),
                        blue_dimensions=OfferDimensions(height=10, length=30, width=20),
                    )
                ],
            ),
            MarketSku(
                sku=TEST_OFFER_SKU_NO_MAIN,
                hid=TEST_HID,
                glparams=[
                    GLParam(param_id=ADDITIONAL_UNIT_PARAM_ID, value=595),
                    GLParam(param_id=CONVERT_COEFFICIENT_PARAM_ID, value=3),
                ],
                blue_offers=[
                    BlueOffer(
                        shop=blue_shop,
                        price=1000,
                        waremd5=TEST_OFFER_WARE_NO_MAIN,
                        weight=5,
                        blue_weight=5,
                        dimensions=OfferDimensions(height=10, length=30, width=20),
                        blue_dimensions=OfferDimensions(height=10, length=30, width=20),
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="virtual",
                waremd5=TEST_OFFER_VIRTUAL_WARE,
                shop=dsbs_shop,
                virtual_model_id=TEST_OFFER_VIRTUAL_SKU,
                price=1000,
                cpa=Offer.CPA_REAL,
                hid=TEST_HID,
                delivery_buckets=[
                    802,
                ],
                weight=5,
                dimensions=OfferDimensions(height=10, length=30, width=20),
                glparams=[
                    GLParam(param_id=MAIN_UNIT_PARAM_ID, value=956),
                    GLParam(param_id=ADDITIONAL_UNIT_PARAM_ID, value=595),
                    GLParam(param_id=CONVERT_COEFFICIENT_PARAM_ID, value=3),
                ],
            ),
        ]
        cls.settings.nordstream_autogenerate = False
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=802,
                fesh=42,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=rid,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=3, shop_delivery_price=10),
                        ],
                    )
                    for rid in (213, 20729)
                ],
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(145, [145]),
            DynamicWarehouseLink(64858, [64858]),
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                ware,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=50000,
                            max_dim_sum=220,
                            max_dimensions=[80, 80, 80],
                            min_days=0,
                            max_days=100,
                        )
                    ],
                    20729: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=50000,
                            max_dim_sum=220,
                            max_dimensions=[80, 80, 80],
                            min_days=0,
                            max_days=100,
                        )
                    ],
                    225: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=50000,
                            max_dim_sum=220,
                            max_dimensions=[80, 80, 80],
                            min_days=0,
                            max_days=100,
                        )
                    ],
                },
            )
            for ware in [145, 64858]
        ]
        cls.dynamic.nordstream += generate_dsbs(cls.index)

    def test_units(self):
        """
        Проверяем формат информации о юнитах
        """
        enable_unit_flag = "&rearr-factors=market_enable_offer_unit_info={}"
        for sku, offer_ware in [(TEST_OFFER_SKU, TEST_OFFER_WARE), (TEST_OFFER_VIRTUAL_SKU, TEST_OFFER_VIRTUAL_WARE)]:
            requests = [
                'place=prime&hid={}&rids=213'.format(TEST_HID),
                'place=offerinfo&offerid={}&regset=2&show-urls=cpa&rids=213'.format(offer_ware),
                'rids=213&place=sku_offers&market-sku={}'.format(sku),
            ]
            for flag, show in [(0, False), (None, True), (1, True)]:
                for request in requests:
                    response = self.report.request_json(
                        request + (enable_unit_flag.format(flag) if flag is not None else "")
                    )
                    self.assertFragmentIn(
                        response,
                        {
                            "entity": "offer",
                            "wareId": offer_ware,
                            "unitInfo": {
                                "mainUnit": MAIN_UNIT_VALUE,
                                "referenceUnits": [
                                    {
                                        "unitName": "м²",
                                        "unitCount": 3,
                                        "unitPrice": {"value": str(333), "currency": "RUR"},
                                    }
                                ],
                            }
                            if show
                            else Absent(),
                        },
                    )

    def test_units_prime_bobruysk(self):
        """
        Проверяем, что вне РФ единицы измерения не показываются
        """
        enable_unit_flag = "&rearr-factors=market_enable_offer_unit_info={}"
        for sku, offer_ware in [
            (TEST_OFFER_SKU, TEST_OFFER_WARE),
        ]:
            requests = [
                'place=prime&hid={}&rids=20729'.format(TEST_HID),
                'place=offerinfo&offerid={}&regset=2&show-urls=cpa&rids=20729'.format(offer_ware),
                'rids=20729&place=sku_offers&market-sku={}'.format(sku),
            ]
            for request in requests:
                response = self.report.request_json(request + enable_unit_flag.format(1))
                self.assertFragmentIn(response, {"entity": "offer", "wareId": offer_ware, "unitInfo": Absent()})

    def test_units_with_optional_main(self):
        """
        Проверяем формат информации о юнитах без mainUnit
        """
        enable_unit_flag = "&rearr-factors=market_enable_offer_unit_info=1;market_enable_empty_main_unit={}"
        for sku, offer_ware in [(TEST_OFFER_SKU_NO_MAIN, TEST_OFFER_WARE_NO_MAIN)]:
            requests = [
                'place=prime&hid={}&rids=213'.format(TEST_HID),
                'place=offerinfo&offerid={}&regset=2&show-urls=cpa&rids=213'.format(offer_ware),
                'rids=213&place=sku_offers&market-sku={}'.format(sku),
            ]
            for flag, show in [(0, False), (None, True), (1, True)]:
                for request in requests:
                    response = self.report.request_json(
                        request + (enable_unit_flag.format(flag) if flag is not None else "")
                    )
                    self.assertFragmentIn(
                        response,
                        {
                            "entity": "offer",
                            "wareId": offer_ware,
                            "unitInfo": {
                                "mainUnit": Absent(),
                                "referenceUnits": [
                                    {
                                        "unitName": "м²",
                                        "unitCount": 3,
                                        "unitPrice": {"value": str(333), "currency": "RUR"},
                                    }
                                ],
                            }
                            if show
                            else Absent(),
                        },
                    )


if __name__ == '__main__':
    main()

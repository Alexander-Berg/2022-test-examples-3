#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    MarketSku,
    Model,
    Offer,
    Outlet,
    Payment,
    PaymentRegionalGroup,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    Shop,
    ShopPaymentMethods,
)
from core.testcase import TestCase, main


class _C:

    rid_mo = 1
    rid_msk = 213
    rid_no_hide = 194

    model_id = 10
    hid = 11

    sku_hide = '101218632776'
    sku_no_hide = '100'

    fesh_card_only = 1119
    fesh_cash_only = 1120
    fesh_both = 1121
    fesh_no_hide_rid = 1200


class T(TestCase):
    """
    Набор тестов для скрыти cash_only офферов.
    См. https://st.yandex-team.ru/MARKETOUT-41902
    """

    @classmethod
    def prepare(cls):
        cls.index.hide_cash_only_conditions_rids += [1, 213]
        cls.index.hide_cash_only_conditions_msku += [
            "101218603767",
            "101218632776",
            "101218632777",
            "101218632778",
            "101218635767",
            "101218635768",
        ]

        # 4 магазина: card_only, cash_only, both_payment и cash_only из региона не из списка
        cls.index.shops += [
            Shop(
                fesh=_C.fesh_card_only, regions=[_C.rid_msk, _C.rid_mo], priority_region=_C.rid_msk, cpa=Shop.CPA_REAL
            ),
            Shop(
                fesh=_C.fesh_cash_only, regions=[_C.rid_msk, _C.rid_mo], priority_region=_C.rid_msk, cpa=Shop.CPA_REAL
            ),
            Shop(fesh=_C.fesh_both, regions=[_C.rid_msk, _C.rid_mo], priority_region=_C.rid_msk, cpa=Shop.CPA_REAL),
            Shop(fesh=_C.fesh_no_hide_rid, regions=[_C.rid_no_hide], priority_region=_C.rid_no_hide, cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(point_id=_C.fesh_cash_only, region=_C.rid_msk),
            Outlet(point_id=_C.fesh_no_hide_rid, region=_C.rid_no_hide),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=_C.fesh_card_only,
                fesh=_C.fesh_card_only,
                options=[PickupOption(outlet_id=_C.fesh_cash_only, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=_C.fesh_cash_only,
                fesh=_C.fesh_cash_only,
                options=[PickupOption(outlet_id=_C.fesh_cash_only, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=_C.fesh_both,
                fesh=_C.fesh_both,
                options=[PickupOption(outlet_id=_C.fesh_cash_only, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=_C.fesh_no_hide_rid,
                fesh=_C.fesh_no_hide_rid,
                options=[PickupOption(outlet_id=_C.fesh_no_hide_rid, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=_C.fesh_card_only,
                fesh=_C.fesh_card_only,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=_C.rid_msk,
                        options=[
                            DeliveryOption(day_from=0, day_to=1, order_before=6, shop_delivery_price=100),
                        ],
                    )
                ],
            ),
            DeliveryBucket(
                bucket_id=_C.fesh_cash_only,
                fesh=_C.fesh_cash_only,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=_C.rid_msk,
                        options=[
                            DeliveryOption(day_from=0, day_to=1, order_before=6, shop_delivery_price=100),
                        ],
                    )
                ],
            ),
            DeliveryBucket(
                bucket_id=_C.fesh_both,
                fesh=_C.fesh_both,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=_C.rid_msk,
                        options=[
                            DeliveryOption(day_from=0, day_to=1, order_before=6, shop_delivery_price=100),
                        ],
                    )
                ],
            ),
            DeliveryBucket(
                bucket_id=_C.fesh_no_hide_rid,
                fesh=_C.fesh_no_hide_rid,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=_C.rid_no_hide,
                        options=[
                            DeliveryOption(day_from=0, day_to=1, order_before=6, shop_delivery_price=100),
                        ],
                    )
                ],
            ),
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=_C.fesh_card_only,
                payment_groups=[
                    PaymentRegionalGroup(included_regions=[_C.rid_msk], payment_methods=[Payment.PT_CARD_ON_DELIVERY]),
                ],
            ),
            ShopPaymentMethods(
                fesh=_C.fesh_cash_only,
                payment_groups=[
                    PaymentRegionalGroup(included_regions=[_C.rid_msk], payment_methods=[Payment.PT_CASH_ON_DELIVERY]),
                ],
            ),
            ShopPaymentMethods(
                fesh=_C.fesh_both,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[_C.rid_msk],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=_C.fesh_no_hide_rid,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[_C.rid_no_hide], payment_methods=[Payment.PT_CASH_ON_DELIVERY]
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=_C.model_id, sku=_C.sku_hide),
            MarketSku(hyperid=_C.model_id, sku=_C.sku_no_hide),
        ]

        cls.index.models += [
            Model(hyperid=_C.model_id, hid=_C.hid),
        ]

        cls.index.offers += [
            Offer(
                title='hide_cash_only_offer 1',
                cpa=Offer.CPA_REAL,
                offerid=1019,
                fesh=_C.fesh_card_only,
                hyperid=_C.model_id,
                price=3000,
            ),
            Offer(
                title='hide_cash_only_offer 2',
                cpa=Offer.CPA_REAL,
                offerid=1020,
                fesh=_C.fesh_cash_only,
                hyperid=_C.model_id,
                price=2000,
            ),
            Offer(
                title='hide_cash_only_offer 3',
                cpa=Offer.CPA_REAL,
                offerid=1021,
                fesh=_C.fesh_both,
                hyperid=_C.model_id,
                price=1000,
            ),
            Offer(
                title='hide_cash_only_offer 4',
                cpa=Offer.CPA_REAL,
                offerid=1022,
                fesh=_C.fesh_cash_only,
                hyperid=_C.model_id,
                price=500,
                sku=_C.sku_hide,
            ),
            Offer(
                title='hide_cash_only_offer 5',
                cpa=Offer.CPA_REAL,
                offerid=1023,
                fesh=_C.fesh_cash_only,
                hyperid=_C.model_id,
                price=400,
                sku=_C.sku_no_hide,
            ),
            Offer(
                title='hide_cash_only_offer 6',
                cpa=Offer.CPA_REAL,
                offerid=1024,
                fesh=_C.fesh_no_hide_rid,
                hyperid=_C.model_id,
                price=300,
                sku=_C.sku_hide,
            ),
        ]

    def test_hide_cash_only(self):
        """
        Проверяем, что скрываются все оффера, у которых оплата только наличными
        (поведение по-умолчанию) и отключить это скрытие можно с помощью rearr-флага
        market_hide_cash_only_offers_enabled=0
        """

        rearr = "&rearr-factors=market_hide_cash_only_offers_enabled=0"
        request = "place=productoffers&hyperid={}&rids={}".format(_C.model_id, _C.rid_msk)

        # Скрытие отключено (rearr-флаг market_hide_cash_only_offers_enabled=0) - возвращаем все офферы
        response = self.report.request_json(request + rearr)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 5"},
                        "prices": {"rawValue": "400"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 4"},
                        "prices": {"rawValue": "500"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 3"},
                        "prices": {"rawValue": "1000"},
                        "payments": {
                            "deliveryCard": True,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 2"},
                        "prices": {"rawValue": "2000"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 1"},
                        "prices": {"rawValue": "3000"},
                        "payments": {
                            "deliveryCard": True,
                            "deliveryCash": False,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        # Поведение по-умолчанию (rearr-флаг market_hide_cash_only_offers_enabled=1) -- оффер cash_only должен быть скрыт
        response = self.report.request_json(request + "&debug=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 5"},
                        "prices": {"rawValue": "400"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 3"},
                        "prices": {"rawValue": "1000"},
                        "payments": {
                            "deliveryCard": True,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 2"},
                        "prices": {"rawValue": "2000"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 1"},
                        "prices": {"rawValue": "3000"},
                        "payments": {
                            "deliveryCard": True,
                            "deliveryCash": False,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {"brief": {"filters": {"HIDE_CASH_ONLY": 1}}})

    def test_hide_cash_only_rid_no_hide(self):
        """
        Проверяем, что в для региона не из списка скрытия cash_only оффер с market_sku из списка не скрывается.
        """

        request = "place=productoffers&hyperid={}&rids={}".format(_C.model_id, _C.rid_no_hide)

        # Без rearr-флага market_hide_cash_only_offers_enabled
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 6"},
                        "prices": {"rawValue": "300"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        rearr = "&rearr-factors=market_hide_cash_only_offers_enabled=1"

        # rearr-флаг market_hide_cash_only_offers_enabled=1 -- оффер по прежнему не скрыт
        response = self.report.request_json(request + rearr)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "hide_cash_only_offer 6"},
                        "prices": {"rawValue": "300"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DeliveryBucket,
    DeliveryOption,
    ExpressSupplier,
    GLParam,
    GLType,
    HyperCategory,
    Model,
    Offer,
    Outlet,
    Payment,
    PaymentRegionalGroup,
    PickupBucket,
    PickupOption,
    PrescriptionManagementSystem,
    Region,
    RegionalDelivery,
    Shop,
    ShopPaymentMethods,
)
from core.matcher import NoKey
from core.types.hypercategory import ALCOHOL_VINE_CATEG_ID


# Used hyperid in this file: 1, 2, 3, 4, 5, 10, 9015

BAA_PARAM_ID = 17766785
DRUGS_CATEGORY = 15758037


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.regiontree += [
            Region(
                rid=1,
                name='MO',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=213,
                        name='Москва',
                        children=[
                            Region(rid=1010, name='VAO'),
                        ],
                    ),
                ],
            ),
            Region(
                rid=44,
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=23, name='Federative city'),
                    Region(rid=35),
                ],
            ),
            Region(rid=55),
            Region(rid=75, name='Gorod2'),
            Region(rid=10),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=198119,
                name="Electronics",
                children=[
                    HyperCategory(hid=91491, name="Mobile telephones"),
                    HyperCategory(hid=9000, name="Mobile telephones 2"),
                ],
            ),
            HyperCategory(hid=10470548, name="category_bad"),
        ]

        cls.index.shops += [
            Shop(fesh=123456789, name="ok_shop_1", regions=[213, 75, 44, 55], priority_region=75),
            Shop(
                fesh=654321654,
                name="ok_shop_2",
                regions=[213, 75],
                priority_region=75,
                medicine_courier=True,
                prescription_management_system=PrescriptionManagementSystem.PS_MEDICATA,
            ),
            Shop(fesh=10000, name="bad_shop", regions=[213, 75], priority_region=75),
            Shop(fesh=777, name="ok_shop_3", regions=[213], priority_region=75),
            Shop(fesh=999, name="only_pickup_shop", regions=[213], priority_region=75),
            Shop(fesh=666, name="medical_courier_shop", regions=[23], priority_region=23, medicine_courier=True),
            Shop(
                fesh=888, name="non_medical_courier_shop", regions=[213, 75], priority_region=75, medicine_courier=False
            ),
            Shop(fesh=1414, name="shop_with_digital_goods", regions=[123], priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(point_id=1, region=10),
            Outlet(point_id=2, region=23),
            Outlet(point_id=3, region=213),
            Outlet(point_id=4, region=75),
            Outlet(point_id=5, region=75),
            Outlet(point_id=6, region=75),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1,
                fesh=999,
                options=[
                    PickupOption(outlet_id=1, day_from=1, day_to=2, price=100),
                    PickupOption(outlet_id=2, day_from=1, day_to=2, price=100),
                    PickupOption(outlet_id=3, day_from=1, day_to=2, price=200),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2,
                fesh=888,
                options=[
                    PickupOption(outlet_id=4, day_from=1, day_to=2, price=100),
                    PickupOption(outlet_id=5, day_from=1, day_to=2, price=100),
                    PickupOption(outlet_id=6, day_from=1, day_to=2, price=200),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hid=91491, hyperid=1),
            Model(hid=DRUGS_CATEGORY, hyperid=2),
            Model(hid=91491, hyperid=3),
            Model(hid=ALCOHOL_VINE_CATEG_ID, hyperid=4),
            # non prescription drug
            Model(hid=15756503, hyperid=5),
        ]

        cls.index.offers += [
            Offer(title='Offer 1', offerid=101, fesh=123456789, hyperid=1, price=3000),
            Offer(title='Offer 2', offerid=201, fesh=654321654, hyperid=1, price=3000),
            Offer(title='Offer 3', fesh=999, pickup_buckets=[1]),
            Offer(
                title='Offer 4',
                offerid=301,
                fesh=654321654,
                hyperid=2,
                price=3000,
                is_medicine=True,
                is_prescription=True,
            ),
            Offer(
                title='Offer 5',
                offerid=401,
                fesh=654321654,
                hyperid=3,
                price=3000,
                glparams=[
                    GLParam(param_id=BAA_PARAM_ID, value=1),
                ],
            ),
            Offer(title='Offer 6', offerid=501, fesh=654321654, hyperid=4, price=3000),
            Offer(
                title='Non_prescription_drug_in_non_medical_shop',
                offerid=601,
                fesh=888,
                hyperid=5,
                price=3000,
                pickup_buckets=[2],
                is_medicine=True,
            ),
            Offer(
                title='Non_prescription_drug_in_medical_shop_with_license',
                offerid=601,
                fesh=666,
                hyperid=6,
                price=3000,
                is_medicine=True,
            ),
            Offer(
                title='Usual offer from shop with digital offer',
                cpa=Offer.CPA_REAL,
                hyperid=9015,
                fesh=1414,
                delivery_buckets=[418],
                download=False,
            ),
            Offer(
                title='Digital offer',
                cpa=Offer.CPA_REAL,
                hyperid=9015,
                fesh=1414,
                has_delivery_options=False,
                pickup=False,
                store=False,
                available=False,
                download=True,
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=BAA_PARAM_ID, hid=91491, gltype=GLType.BOOL),
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=123456789,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    ),
                    PaymentRegionalGroup(included_regions=[75], payment_methods=[Payment.PT_CARD_ON_DELIVERY]),
                    PaymentRegionalGroup(
                        included_regions=[44, 55], excluded_regions=[23], payment_methods=[Payment.PT_CASH_ON_DELIVERY]
                    ),
                    PaymentRegionalGroup(included_regions=[23], payment_methods=[Payment.PT_PREPAYMENT_CARD]),
                ],
            ),
            ShopPaymentMethods(
                fesh=654321654,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_CASH_ON_DELIVERY,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_PREPAYMENT_CARD,
                        ],
                    ),
                    PaymentRegionalGroup(
                        included_regions=[75], payment_methods=[Payment.PT_PREPAYMENT_CARD, Payment.PT_PREPAYMENT_OTHER]
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=999,
                payment_groups=[
                    PaymentRegionalGroup(included_regions=[10], payment_methods=[Payment.PT_CASH_ON_DELIVERY]),
                    PaymentRegionalGroup(
                        included_regions=[23], payment_methods=[Payment.PT_PREPAYMENT_CARD, Payment.PT_CASH_ON_DELIVERY]
                    ),
                    PaymentRegionalGroup(included_regions=[213], payment_methods=[Payment.PT_CASH_ON_DELIVERY]),
                ],
            ),
            ShopPaymentMethods(
                fesh=666,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[23],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=1414,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=418,
                fesh=1414,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(day_from=0, day_to=1, order_before=6, shop_delivery_price=140),
                        ],
                    )
                ],
            )
        ]

    def test_payments_without_delivery(self):
        """
        Проверяем, что если у оффера есть только самовывоз (нет доставки курьером), то способы оплаты deliveryCard, deliveryCash не показываем
        """
        # в регионе 10 оффер доступен только в точке самовывоза, предоплата не поддерживается
        response = self.report.request_json("place=prime&fesh=999&rids=10&local-offers-first=0")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Offer 3"},
                "payments": {
                    "deliveryCard": False,
                    "deliveryCash": False,
                    "prepaymentCard": False,
                    "prepaymentOther": False,
                },
                "delivery": {"hasPickup": True, "hasLocalStore": True, "isCountrywide": False, "hasPost": False},
            },
        )
        self.assertFragmentNotIn(response, {"id": "payments"})
        response = self.report.request_json("place=prime&fesh=999&rids=10&payments=delivery_cash&local-offers-first=0")
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)
        response = self.report.request_json(
            "place=prime&fesh=999&rids=10&payments=prepayment_card&local-offers-first=0"
        )
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

        # в регионе 213 оффер может быть доставлен курьером или самовывозом
        # предоплата не поддерживается, но можно заплатить наличкой курьеру
        response = self.report.request_json("place=prime&fesh=999&rids=213")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Offer 3"},
                "payments": {
                    "deliveryCard": False,
                    "deliveryCash": True,
                    "prepaymentCard": False,
                    "prepaymentOther": False,
                },
                "delivery": {"hasPickup": True, "hasLocalStore": True, "isCountrywide": True, "hasPost": False},
            },
        )
        response = self.report.request_json("place=prime&fesh=999&rids=213&payments=delivery_cash&local-offers-first=0")
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Offer 3"}}]}, allow_different_len=False)
        response = self.report.request_json(
            "place=prime&fesh=999&rids=213&payments=prepayment_card&local-offers-first=0"
        )
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

        # в регионе 23 оффер доступен только в точке самовывоза, предоплата доступна
        response = self.report.request_json("place=prime&fesh=999&rids=23")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Offer 3"},
                "payments": {
                    "deliveryCard": False,
                    "deliveryCash": False,
                    "prepaymentCard": True,
                    "prepaymentOther": False,
                },
                "delivery": {"hasPickup": True, "hasLocalStore": True, "isCountrywide": False, "hasPost": False},
            },
        )
        response = self.report.request_json(
            "place=prime&fesh=999&rids=23&payments=prepayment_card&local-offers-first=0"
        )
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Offer 3"}}]}, allow_different_len=False)
        response = self.report.request_json("place=prime&fesh=999&rids=23&payments=delivery_card&local-offers-first=0")
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    def test_payment_methods_in_offer_info(self):
        """
        Check getting payment methods
        """
        model = "hid=91491&hyperid=1"

        rids = "rids=44"
        response = self.report.request_json("place=productoffers&{}&{}&grhow=offer".format(model, rids))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer 1"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
        )

        rids = "rids=35"
        response = self.report.request_json("place=productoffers&{}&{}&grhow=offer".format(model, rids))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer 1"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
        )

        rids = "rids=23"
        response = self.report.request_json("place=productoffers&{}&{}&grhow=offer".format(model, rids))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer 1"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": False,
                            "prepaymentCard": True,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "id": "payments",
                "name": "Способы оплаты",
                "values": [
                    {"initialFound": 1, "found": 1, "id": "prepayment_card", "value": "Картой на сайте"},
                    {
                        "initialFound": 0,
                        "checked": NoKey("checked"),
                        "found": 0,
                        "id": "delivery_card",
                        "value": "Картой курьеру",
                    },
                    {
                        "initialFound": 0,
                        "checked": NoKey("checked"),
                        "found": 0,
                        "id": "delivery_cash",
                        "value": "Наличными курьеру",
                    },
                ],
            },
            allow_different_len=False,
        )

        rids = "rids=55"
        response = self.report.request_json("place=productoffers&{}&{}&grhow=offer".format(model, rids))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer 1"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
        )

        rids = "rids=213"
        response = self.report.request_json("place=productoffers&{}&{}&grhow=offer".format(model, rids))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer 1"},
                        "payments": {
                            "deliveryCard": True,
                            "deliveryCash": True,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer 2"},
                        "payments": {
                            "deliveryCard": True,
                            "deliveryCash": True,
                            "prepaymentCard": True,
                            "prepaymentOther": False,
                        },
                    },
                ]
            },
        )

        rids = "rids=75"
        response = self.report.request_json("place=productoffers&{}&{}&grhow=offer".format(model, rids))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer 1"},
                        "payments": {
                            "deliveryCard": True,
                            "deliveryCash": False,
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer 2"},
                        "payments": {
                            "deliveryCard": False,
                            "deliveryCash": False,
                            "prepaymentCard": True,
                            "prepaymentOther": True,
                        },
                    },
                ]
            },
        )

    def test_prepayment_disabled_for_prescription(self):
        """
        Проверяем, что рецептурные препараты нельзя оплатить картой.
        """
        response = self.report.request_json(
            "place=productoffers&hid=15758037&hyperid=2&rids=75&grhow=offer&rearr-factors=enable_prescription_drugs_delivery=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "payments": {
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_prepayment_disabled_for_non_prescription_drugs_from_shop_without_license(self):
        """
        Проверяем, что нерецептурные препараты нельзя оплатить картой для магазина без лицензии.
        """
        response = self.report.request_json(
            "place=productoffers&hid=15756503&hyperid=5&rids=75&grhow=offer&rearr-factors=market_not_prescription_drugs_delivery=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "payments": {
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_prepayment_enabled_for_non_prescription_drugs_from_shop_with_license(self):
        """
        Проверяем, что нерецептурные препараты можно оплатить картой для магазина с соответствующей лицензией MedicalCourier() -> true
        """
        response = self.report.request_json(
            "place=productoffers&hid=15756503&hyperid=6&rids=23&grhow=offer&rearr-factors=market_not_prescription_drugs_delivery=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "payments": {
                            "prepaymentCard": True,
                            "prepaymentOther": True,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_prepayment_disabled_for_baa(self):
        response = self.report.request_json("place=productoffers&hid=91491&hyperid=3&rids=75&grhow=offer")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "payments": {
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_prepayment_disabled_for_baa_2(self):
        response = self.report.request_json(
            "place=productoffers&hid={}&hyperid=4&rids=75&grhow=offer".format(ALCOHOL_VINE_CATEG_ID)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "payments": {
                            "prepaymentCard": False,
                            "prepaymentOther": False,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_payment_filter(self):
        """
        Проверяем работу фильтра по способам оплаты

        Если выбран хотя бы один пункт в фильтре,
        то выфильтровываем те оффера, которые не имеют ни одного из выбранных способом оплаты

        Способ оплаты YANDEX считается равносильным способу PREPAYMENT_CARD,
        поэтому при фильтре по предоплату картой оффера с YANDEX тоже остаются

        """
        model = "hid=91491&hyperid=1"

        only_delivery_cash = "payments=delivery_cash"
        only_delivery_card = "payments=delivery_card"
        only_prepayment_card = "payments=prepayment_card"

        delivery_cash_and_card = "payments=delivery_card,delivery_cash"
        _ = "payments=delivery_cash,prepayment_card"
        delivery_card_and_prepayment_card = "payments=delivery_card,prepayment_card"

        all_payment_methods = "payments=delivery_card,delivery_cash,prepayment_card"

        all_offers_213 = [
            {
                "entity": "offer",
                "titles": {"raw": "Offer 1"},
                "payments": {
                    "deliveryCard": True,
                    "deliveryCash": True,
                    "prepaymentCard": False,
                    "prepaymentOther": False,
                },
            },
            {
                "entity": "offer",
                "titles": {"raw": "Offer 2"},
                "payments": {
                    "deliveryCard": True,
                    "deliveryCash": True,
                    "prepaymentCard": True,
                    "prepaymentOther": False,
                },
            },
        ]

        offers_213_with_prepayment_card = [
            {
                "entity": "offer",
                "titles": {"raw": "Offer 2"},
                "payments": {
                    "deliveryCard": True,
                    "deliveryCash": True,
                    "prepaymentCard": True,
                    "prepaymentOther": False,
                },
            },
        ]

        all_offers_75 = [
            {
                "entity": "offer",
                "titles": {"raw": "Offer 1"},
                "payments": {
                    "deliveryCard": True,
                    "deliveryCash": False,
                    "prepaymentCard": False,
                    "prepaymentOther": False,
                },
            },
            {
                "entity": "offer",
                "titles": {"raw": "Offer 2"},
                "payments": {
                    "deliveryCard": False,
                    "deliveryCash": False,
                    "prepaymentCard": True,
                    "prepaymentOther": True,
                },
            },
        ]

        rids = "rids=213"

        # 213 region : all offers for every filter value
        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, only_delivery_card)
        )
        self.assertFragmentIn(
            response, {"results": all_offers_213 + [{"entity": "regionalDelimiter"}]}, allow_different_len=False
        )

        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "payments",
                        "name": "Способы оплаты",
                        "values": [
                            {
                                "initialFound": 1,
                                "found": 1,
                                "checked": NoKey("checked"),
                                "id": "prepayment_card",
                                "value": "Картой на сайте",
                            },
                            {
                                "initialFound": 2,
                                "checked": True,
                                "found": 2,
                                "id": "delivery_card",
                                "value": "Картой курьеру",
                            },
                            {
                                "initialFound": 2,
                                "checked": NoKey("checked"),
                                "found": 2,
                                "id": "delivery_cash",
                                "value": "Наличными курьеру",
                            },
                        ],
                    }
                ],
            },
        )

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, only_delivery_cash)
        )
        self.assertFragmentIn(
            response, {"results": all_offers_213 + [{"entity": "regionalDelimiter"}]}, allow_different_len=False
        )

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, only_prepayment_card)
        )
        self.assertFragmentIn(
            response,
            {"results": offers_213_with_prepayment_card + [{"entity": "regionalDelimiter"}]},
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}&debug=da".format(model, rids, delivery_cash_and_card)
        )
        self.assertFragmentIn(
            response, {"results": all_offers_213 + [{"entity": "regionalDelimiter"}]}, allow_different_len=False
        )

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, all_payment_methods)
        )
        self.assertFragmentIn(
            response, {"results": all_offers_213 + [{"entity": "regionalDelimiter"}]}, allow_different_len=False
        )

        only_123456789_in_75 = [
            {
                "entity": "offer",
                "titles": {"raw": "Offer 1"},
                "payments": {
                    "deliveryCard": True,
                    "deliveryCash": False,
                    "prepaymentCard": False,
                    "prepaymentOther": False,
                },
            },
        ]

        only_654321654_in_75 = [
            {
                "entity": "offer",
                "titles": {"raw": "Offer 2"},
                "payments": {
                    "deliveryCard": False,
                    "deliveryCash": False,
                    "prepaymentCard": True,
                    "prepaymentOther": True,
                },
            },
        ]

        rids = "rids=75"

        # 75 region
        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}&debug=da".format(model, rids, only_delivery_card)
        )
        self.assertFragmentIn(response, {"results": only_123456789_in_75}, allow_different_len=False)

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, only_delivery_cash)
        )
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, only_prepayment_card)
        )
        self.assertFragmentIn(response, {"results": only_654321654_in_75}, allow_different_len=False)

        self.assertFragmentIn(
            response,
            {
                "id": "payments",
                "name": "Способы оплаты",
                "values": [
                    {
                        "initialFound": 1,
                        "found": 1,
                        "checked": True,
                        "id": "prepayment_card",
                        "value": "Картой на сайте",
                    },
                    {
                        "initialFound": 1,
                        "checked": NoKey("checked"),
                        "found": 1,
                        "id": "delivery_card",
                        "value": "Картой курьеру",
                    },
                    {
                        "initialFound": 0,
                        "checked": NoKey("checked"),
                        "found": 0,
                        "id": "delivery_cash",
                        "value": "Наличными курьеру",
                    },
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, delivery_cash_and_card)
        )
        self.assertFragmentIn(response, {"results": only_123456789_in_75}, allow_different_len=False)

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, delivery_card_and_prepayment_card)
        )
        self.assertFragmentIn(response, {"results": all_offers_75}, allow_different_len=False)

        self.assertFragmentIn(
            response,
            {
                "id": "payments",
                "name": "Способы оплаты",
                "values": [
                    {
                        "initialFound": 1,
                        "found": 1,
                        "checked": True,
                        "id": "prepayment_card",
                        "value": "Картой на сайте",
                    },
                    {"initialFound": 1, "checked": True, "found": 1, "id": "delivery_card", "value": "Картой курьеру"},
                    {
                        "initialFound": 0,
                        "checked": NoKey("checked"),
                        "found": 0,
                        "id": "delivery_cash",
                        "value": "Наличными курьеру",
                    },
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&{}&{}&grhow=offer&{}".format(model, rids, all_payment_methods)
        )
        self.assertFragmentIn(response, {"results": all_offers_75}, allow_different_len=False)

    def test_model_filter(self):
        """
        Check: filter not collapsing models
        """

        _ = "_-factors=market_show_payment_methods=true"

        response = self.report.request_json("place=prime&hid=91491&rids=75")
        self.assertFragmentIn(response, {"results": [{"entity": "product"}]})

        response = self.report.request_json("place=prime&hid=91491&payments=delivery_cash&rids=75")
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    def test_payment_on_delivery_disable_for_digital_offers(self):
        """
        Проверяем, что у цифровых товаров нет опций оплаты после доставки как у частного
        случая товаров без физической доставки
        """
        response = self.report.request_json("place=prime&fesh=1414&rids=213&local-offers-first=0")
        # у обычных товаров существуют все способы оплаты
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Usual offer from shop with digital offer"},
                "payments": {
                    "deliveryCard": True,
                    "deliveryCash": True,
                    "prepaymentCard": True,
                    "prepaymentOther": True,
                },
            },
        )

        # у цифровых товаров "отрываются" все непредоплатные способы оплаты
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Digital offer"},
                "payments": {
                    "deliveryCard": False,
                    "deliveryCash": False,
                    "prepaymentCard": True,
                    "prepaymentOther": True,
                },
            },
        )

    @classmethod
    def prepare_card_payment_only_filtering(cls):
        # Create 3 shops: one with card only, one with cash only, one with both types.
        # Add an offer to every shop

        cls.index.shops += [
            Shop(fesh=1119, regions=[213, 75], priority_region=75),
            Shop(fesh=1120, regions=[213, 75], priority_region=75),
            Shop(fesh=1121, regions=[213, 75], priority_region=75),
        ]

        cls.index.outlets += [
            Outlet(point_id=1120, region=75),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1119,
                fesh=1119,
                options=[PickupOption(outlet_id=1120, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=1120,
                fesh=1120,
                options=[PickupOption(outlet_id=1120, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=1121,
                fesh=1121,
                options=[PickupOption(outlet_id=1120, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1119,
                fesh=1119,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=75,
                        options=[
                            DeliveryOption(day_from=0, day_to=1, order_before=6, shop_delivery_price=100),
                        ],
                    )
                ],
            ),
            DeliveryBucket(
                bucket_id=1120,
                fesh=1120,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=75,
                        options=[
                            DeliveryOption(day_from=0, day_to=1, order_before=6, shop_delivery_price=100),
                        ],
                    )
                ],
            ),
            DeliveryBucket(
                bucket_id=1121,
                fesh=1121,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=75,
                        options=[
                            DeliveryOption(day_from=0, day_to=1, order_before=6, shop_delivery_price=100),
                        ],
                    )
                ],
            ),
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=1119,
                payment_groups=[
                    PaymentRegionalGroup(included_regions=[75], payment_methods=[Payment.PT_CARD_ON_DELIVERY]),
                ],
            ),
            ShopPaymentMethods(
                fesh=1120,
                payment_groups=[
                    PaymentRegionalGroup(included_regions=[75], payment_methods=[Payment.PT_CASH_ON_DELIVERY]),
                ],
            ),
            ShopPaymentMethods(
                fesh=1121,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[75],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(title='hide_cash_only_offer 1', offerid=1019, fesh=1119, hyperid=10, price=3000),
            Offer(title='hide_cash_only_offer 2', offerid=1020, fesh=1120, hyperid=10, price=2000),
            Offer(title='hide_cash_only_offer 3', offerid=1021, fesh=1121, hyperid=10, price=1000),
            Offer(title='hide_cash_only_offer 4', offerid=1022, fesh=1120, hyperid=10, price=500),
        ]

    def test_card_payment_only_filtering(self):
        """
        Проверяем, что в случае выставленного rearr-flag=market_hide_cash_only_offers=All скрываются все оффера,
        у которых есть только оплата наличными.
        Если выставлен флаг rearr-flag=market_hide_cash_only_offers=All скрываются все оффера, у которых есть только
        оплата наличными И существует оффер с оплатой картой, у которого цена не выше
        """

        rids_75 = "&rids=75"
        rearr_all = "&rearr-factors=market_hide_cash_only_offers=All"
        rearr_has_better_card_price = "&rearr-factors=market_hide_cash_only_offers=IfHasBetterCardPrice"
        request = "place=productoffers&hyperid=10&grhow=offer"

        # Test without rearr-flags - just return all offers
        response = self.report.request_json(request + rids_75)
        self.assertFragmentIn(
            response,
            {
                "results": [
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

        # # Включаем реарр-флаг в режиме резать все, смотрим, что оффера с (только наличные) исчезли, остальные на месте
        response = self.report.request_json(request + rids_75 + rearr_all)
        self.assertFragmentIn(
            response,
            {
                "results": [
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

        # Включаем реарр-флаг в режиме резать частично, смотрим, что оффер за 2000 с (только наличные) исчез, потому что есть оффер
        # за 1000 с оплатой картой, остальные на месте
        response = self.report.request_json(request + rids_75 + rearr_has_better_card_price + "&debug=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
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

    @classmethod
    def prepare_disable_cash_payment_when_express_delivery(cls):
        cls.settings.lms_autogenerate = False
        cls.index.shops += [Shop(fesh=9999, priority_region=213, datafeed_id=9999, warehouse_id=9999)]
        cls.index.express_partners.suppliers += [ExpressSupplier(feed_id=9999, supplier_id=9999, warehouse_id=9999)]
        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=9999,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    ),
                ],
            )
        ]
        cls.index.hypertree += [HyperCategory(hid=9999, name="Electronics")]
        cls.index.models += [Model(hid=9999, hyperid=9999)]
        cls.index.offers += [
            Offer(title='Express offer 1', offerid=2021, fesh=9999, hyperid=9999, price=9999, is_express=True)
        ]

    def test_disable_cash_payment_when_express_delivery(self):
        response = self.report.request_json("place=productoffers&rids=213&hyperid=9999&grhow=offer&debug=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Express offer 1"},
                        "payments": {
                            "deliveryCash": False,
                        },
                    },
                ]
            },
        )
        self.assertFragmentIn(response, 'Removing post-delivery cash payment method due to express delivery')


if __name__ == '__main__':
    main()

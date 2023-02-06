#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
import itertools

# test_promo_payment был перенесен из файла test_actual_delivery, т.к. из-за него происходило много таймаутов в TestEnv'е

from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicQPromos,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GpsCoord,
    Outlet,
    OutletDeliveryOption,
    Payment,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    RegionalDelivery,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.matcher import Absent
from core.types.sku import MarketSku, BlueOffer
from core.report import REQUEST_TIMESTAMP
from datetime import datetime
from core.types.offer_promo import OffersMatchingRules


class T(TestCase):
    @classmethod
    def prepare(cls):
        """Данные отсюда -- это необходимое подмножество данных из файла test_actual_delivery.py,
        которые нужны, чтобы работал test_promo_payment (возможно, отсюда можно вырезать еще что-то).
        """
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[
                    2001,
                    4001,
                    4002,
                ],
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                warehouse_id=145,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=789,
                title="sku with promo",
                hyperid=789,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        waremd5='789_payment_gggggggggg',
                        feedid=3,
                        offerid='blue.offer.789.1',
                        weight=5,
                        dimensions=OfferDimensions(length=7, width=8, height=9),
                    )
                ],
                delivery_buckets=[801, 802],
                pickup_buckets=[5002],
                post_buckets=[7001],
            ),
        ]
        cls.delivery_calc.on_request_offer_buckets(weight=5, height=8, width=7, length=9).respond([801, 802], [5], [7])

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicDeliveryServiceInfo(id=103, name='c_103'),
            DynamicDeliveryServiceInfo(id=157, name='c_157', rating=1),
            DynamicDeliveryServiceInfo(id=158, name='c_158', rating=2),
            DynamicDeliveryServiceInfo(id=201, name='c_201'),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 160, 164]),
            DynamicWarehousesPriorityInRegion(region=2, warehouses=[300]),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=service,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            )
            for service in [103, 157, 158, 201]
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=4001,
                delivery_service_id=201,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115200,
                delivery_option=OutletDeliveryOption(
                    shipper_id=201, day_from=2, day_to=4, price=400
                ),  # В этот ПВЗ доставится быстрее, чем в 4101 (сроки смотри в бакетах)
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
            Outlet(
                point_id=4002,
                delivery_service_id=201,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115201,
                delivery_option=OutletDeliveryOption(shipper_id=201, day_from=1, day_to=1, price=500),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed"],
                gps_coord=GpsCoord(67.7, 55.8),
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=801,
                dc_bucket_id=801,
                fesh=1,
                carriers=[158],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_YANDEX],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=802,
                dc_bucket_id=802,
                fesh=1,
                carriers=[157],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=3, day_from=4, day_to=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_YANDEX],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5002,
                dc_bucket_id=5,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=2001)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Post buckets stored in same buckets as outlets
            PickupBucket(
                bucket_id=7001,
                dc_bucket_id=7,
                fesh=1,
                carriers=[201],
                options=[
                    PickupOption(outlet_id=4001, day_from=1, day_to=2, price=7),
                    PickupOption(outlet_id=4002, day_from=1, day_to=1, price=6),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.settings.loyalty_enabled = True

    def check_promo_payment(self, promo_payments, courier_id, courier_payments, pickup_payments, post):
        """
        Проверяем ограничение способов оплаты, которые накладывает акция
        """
        # Тикеты, в которых вносились изменения в метод: MARKETOUT-21605, MARKETOUT-21994, MARKETOUT-29521

        self.dynamic.qpromos_generation = datetime.fromtimestamp(REQUEST_TIMESTAMP)
        self.dynamic.qpromos = [
            DynamicQPromos(
                [
                    Promo(
                        promo_type=PromoType.PROMO_CODE,
                        key='Payment-Promo',
                        allowed_payment_methods=promo_payments,
                        feed_id=31,
                        shop_promo_id=3,
                        discount_value=5,
                        discount_currency='RUR',
                        offers_matching_rules=[
                            # В случае, если способы оплаты не заданы, убираем промо для данного СКУ
                            OffersMatchingRules(mskus=[789 if promo_payments is not None else 1528]),
                        ],
                        generation_ts=REQUEST_TIMESTAMP + 1,
                    ),
                ]
            )
        ]

        payment_rearr = '&rearr-factors=enable_payment_methods_restriction=1'
        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
        force_use_delivery_calc_rearr = '&force-use-delivery-calc=1'
        request = (
            'place=actual_delivery&offers-list=789_payment_gggggggggg:1{}&rids=213&pickup-options=grouped&pickup-options-extended-grouping=1'
            + payment_rearr
            + '&combinator=0&rearr-factors=enable_fast_promo_matcher=1;enable_fast_promo_new_promos=1'
        )
        disable_restrictions = '&rearr-factors=enable_payment_methods_restriction=0'

        iterable = itertools.product(
            ('', force_use_delivery_calc_rearr),  # force_use_delivery_calc
            ('', disable_post_as_pickup_rearr),  # disable_post_as_pickup
        )

        pickup = [
            {"paymentMethods": pickup_payments, "outletIds": [2001]},
        ]
        for force_use_delivery_calc, disable_post_as_pickup in iterable:
            # Нет разницы будет ли задан запрос к калькулятору доставки или нет. Ограничение по способам оплаты работает всегда
            response = self.report.request_json(request.format('') + force_use_delivery_calc + disable_post_as_pickup)
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {
                                "serviceId": str(courier_id),
                                "paymentMethods": courier_payments,
                            }
                        ],
                        "pickupOptions": pickup if disable_post_as_pickup else pickup + post,
                        "postOptions": post if disable_post_as_pickup else Absent(),
                    },
                },
                allow_different_len=False,
            )

        options = [
            {"serviceId": str(courier_id), "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY"]},
        ]
        pickup_options = [
            {"paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"], "outletIds": [2001]},
        ]
        post_options = [
            {"paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"], "outletIds": [4001]},
            {"paymentMethods": ["YANDEX", "CASH_ON_DELIVERY"], "outletIds": [4002]},
        ]

        full_answer = {
            "entity": "deliveryGroup",
            "delivery": {
                "options": options,
                "pickupOptions": pickup_options,
                "postOptions": post_options,
            },
        }

        # Для запроса, в котором указаны параметры офера, нельзя вычислить их разрешенный способ оплаты. Поэтому ограничения нет. Но, данный запрос идет после оформления заказа
        response = self.report.request_json(request.format(';w:5;d:7x8x9;p:135') + disable_post_as_pickup_rearr)
        self.assertFragmentIn(response, full_answer)

        # Если логика ограничения способов оплаты отключена, то отображаются все способы оплаты
        response = self.report.request_json(request.format('') + disable_restrictions + disable_post_as_pickup_rearr)
        self.assertFragmentIn(response, full_answer)

        full_answer_post_as_pickup = {
            "entity": "deliveryGroup",
            "delivery": {
                "options": options,
                "pickupOptions": pickup_options + post_options,
                "postOptions": Absent(),
            },
        }

        # Для запроса, в котором указаны параметры офера, нельзя вычислить их разрешенный способ оплаты. Поэтому ограничения нет. Но, данный запрос идет после оформления заказа
        response = self.report.request_json(request.format(';w:5;d:7x8x9;p:135'))
        self.assertFragmentIn(response, full_answer_post_as_pickup)

        # Если логика ограничения способов оплаты отключена, то отображаются все способы оплаты
        response = self.report.request_json(request.format('') + disable_restrictions)
        self.assertFragmentIn(response, full_answer_post_as_pickup)

    @staticmethod
    def __post(outlets, payments):
        return {
            "paymentMethods": payments,
            "outletIds": outlets,
        }

    def test_promo_payment(self):
        # Тикеты, в которых вносились изменения в метод: MARKETOUT-21605, MARKETOUT-21994

        # Нет акции для МСКУ, нет ограничения на способы оплаты
        self.check_promo_payment(
            None,
            158,
            ["YANDEX", "CASH_ON_DELIVERY"],
            ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
            [
                T.__post([4001], ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]),
                T.__post([4002], ["YANDEX", "CASH_ON_DELIVERY"]),
            ],
        )

        # В акции участвует только предоплата
        self.check_promo_payment(
            Payment.PT_YANDEX, 158, ["YANDEX"], ["YANDEX"], [T.__post([4001], ["YANDEX"]), T.__post([4002], ["YANDEX"])]
        )

        # В акции предоплата и наличный расчет при доставке
        self.check_promo_payment(
            Payment.PT_YANDEX + Payment.PT_CASH_ON_DELIVERY,
            158,
            ["YANDEX", "CASH_ON_DELIVERY"],
            ["YANDEX", "CASH_ON_DELIVERY"],
            [T.__post([4001], ["YANDEX", "CASH_ON_DELIVERY"]), T.__post([4002], ["YANDEX", "CASH_ON_DELIVERY"])],
        )

        # В акции доступны все опции, но у доставки только две
        self.check_promo_payment(
            Payment.PT_YANDEX + Payment.PT_CASH_ON_DELIVERY + Payment.PT_CARD_ON_DELIVERY,
            158,
            ["YANDEX", "CASH_ON_DELIVERY"],
            ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
            [
                T.__post([4001], ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]),
                T.__post([4002], ["YANDEX", "CASH_ON_DELIVERY"]),
            ],
        )

        # В пересечении доступных опций у доставки только предоплата
        self.check_promo_payment(
            Payment.PT_YANDEX + Payment.PT_CARD_ON_DELIVERY,
            158,
            ["YANDEX"],
            ["YANDEX", "CARD_ON_DELIVERY"],
            [T.__post([4001], ["YANDEX", "CARD_ON_DELIVERY"]), T.__post([4002], ["YANDEX"])],
        )

        # В приоритетной службе доставки нет оплаты картой на месте
        # ПВЗ и почта 4001 имеют
        # Отделение почты 4002 не имеет пересечения
        # При отсуствии пересечения не применяем ограничения
        self.check_promo_payment(
            Payment.PT_CARD_ON_DELIVERY,
            158,
            ["YANDEX", "CASH_ON_DELIVERY"],
            ["CARD_ON_DELIVERY"],
            [T.__post([4001], ["CARD_ON_DELIVERY"]), T.__post([4002], Absent())],
        )


if __name__ == '__main__':
    main()

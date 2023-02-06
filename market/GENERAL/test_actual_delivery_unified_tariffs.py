#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    Dimensions,
    Offer,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Shop,
)

from core.types.delivery import BlueDeliveryTariff
from core.testcase import TestCase, main


UNIFIED_EXP = 'unified'
UNIFIED_RID = 213
SCALE = 4

shop_dsbs = Shop(
    fesh=42,
    datafeed_id=4240,
    priority_region=UNIFIED_RID,
    name='Dsbs партнер',
    client_id=11,
    cpa=Shop.CPA_REAL,
)

shop_dsbs1 = Shop(
    fesh=43,
    datafeed_id=4243,
    priority_region=54,
    name='Dsbs партнер 54',
    client_id=12,
    cpa=Shop.CPA_REAL,
)

shop_dsbs_in_vladik = Shop(
    fesh=175,
    priority_region=75,
    name='Dsbs партнер 75',
    cpa=Shop.CPA_REAL,
)

KGT = 30.0  # Сейчас захардкожено KGT == 30


offer_dsbs_100 = Offer(
    title="dsbs цена 100",
    hyperid=100,
    fesh=shop_dsbs.fesh,
    waremd5='Dsbs0________________g',
    price=100,
    cpa=Offer.CPA_REAL,
    dimensions=OfferDimensions(length=5, width=5, height=5),
    weight=KGT / 5,
    delivery_options=[DeliveryOption(price=25, day_from=2, day_to=5, order_before=14)],
    pickup_buckets=[6001],
)

offer_dsbs_200 = Offer(
    title="dsbs цена 200",
    hyperid=200,
    fesh=shop_dsbs.fesh,
    waremd5='Dsbs1________________g',
    price=200,
    cpa=Offer.CPA_REAL,
    delivery_options=[DeliveryOption(price=50, day_from=4, day_to=5, order_before=14)],
    pickup_buckets=[6001],
)

offer_dsbs_300 = Offer(
    title="dsbs без веса",
    hyperid=100,
    fesh=shop_dsbs.fesh,
    waremd5='DsbsNoWeight_________g',
    price=300,
    cpa=Offer.CPA_REAL,
    dimensions=OfferDimensions(length=5, width=5, height=5),
    delivery_options=[DeliveryOption(price=25, day_from=2, day_to=5, order_before=14)],
    pickup_buckets=[6001],
)


offer_dsbs_kgt = Offer(
    title="dsbs цена 200",
    hyperid=200,
    fesh=shop_dsbs.fesh,
    waremd5='DsbsKGT______________g',
    price=1000,
    weight=KGT + 10,
    cpa=Offer.CPA_REAL,
    dimensions=OfferDimensions(length=10, width=10, height=10),
    delivery_options=[DeliveryOption(price=75, day_from=4, day_to=5, order_before=14)],
    pickup_buckets=[6001],
)

offer_almost_kgt = Offer(
    title="dsbs цена 150",
    hyperid=200,
    fesh=shop_dsbs.fesh,
    waremd5='DsbsAlmostKGT________g',
    price=150,
    weight=KGT - 1,
    cpa=Offer.CPA_REAL,
    dimensions=OfferDimensions(length=10, width=10, height=10),
    delivery_options=[DeliveryOption(price=75, day_from=4, day_to=5, order_before=14)],
    pickup_buckets=[6001],
)

offer_in_vladik = Offer(
    title="dsbs во Владике",
    fesh=shop_dsbs_in_vladik.fesh,
    waremd5='DsbsInVladik_________g',
    cpa=Offer.CPA_REAL,
    price=150,
    weight=KGT - 1,
    dimensions=OfferDimensions(length=10, width=10, height=10),
    delivery_options=[DeliveryOption(price=75, day_from=4, day_to=5, order_before=14)],
)


class Dsbs_Payment(object):
    tarrifs = [
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=0,
            price_to=200,
            dsbs_payment=10,
            courier_price=11,
            pickup_price=12,
            post_price=13,
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True, large_size=0, dsbs_payment=110, courier_price=111, pickup_price=112, post_price=113
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=1,
            dsbs_payment=1010,
            courier_price=1011,
            pickup_price=1012,
            post_price=1013,
        ),
    ]

    tarrifs_default = [
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=0,
            price_to=200,
            dsbs_payment=10 * SCALE,
            courier_price=11 * SCALE,
            pickup_price=12 * SCALE,
            post_price=13 * SCALE,
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=0,
            dsbs_payment=110 * SCALE,
            courier_price=111 * SCALE,
            pickup_price=112 * SCALE,
            post_price=113 * SCALE,
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=1,
            dsbs_payment=1010 * SCALE,
            courier_price=1011 * SCALE,
            pickup_price=1012 * SCALE,
            post_price=1013 * SCALE,
        ),
    ]

    tarrifs_in_vladik = [
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=0,
            dsbs_payment=999,
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=1,
            dsbs_payment=999,
        ),
    ]


class Unified(object):
    tarrifs = [
        BlueDeliveryTariff(large_size=0, price_to=200, user_price=1, courier_price=2, pickup_price=3, post_price=4),
        BlueDeliveryTariff(large_size=0, user_price=10, courier_price=11, pickup_price=12, post_price=13),
        BlueDeliveryTariff(large_size=1, user_price=110, courier_price=111, pickup_price=112, post_price=113),
    ]

    tarrifs_default = [
        BlueDeliveryTariff(
            large_size=0,
            price_to=200,
            user_price=1 * SCALE,
            courier_price=2 * SCALE,
            pickup_price=3 * SCALE,
            post_price=4 * SCALE,
        ),
        BlueDeliveryTariff(
            large_size=0,
            user_price=10 * SCALE,
            courier_price=11 * SCALE,
            pickup_price=12 * SCALE,
            post_price=13 * SCALE,
        ),
        BlueDeliveryTariff(
            large_size=1,
            user_price=110 * SCALE,
            courier_price=111 * SCALE,
            pickup_price=112 * SCALE,
            post_price=113 * SCALE,
        ),
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [shop_dsbs, shop_dsbs1, shop_dsbs_in_vladik]

        cls.index.outlets += [
            Outlet(
                fesh=shop_dsbs.fesh,
                point_id=144,
                region=UNIFIED_RID,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=248, day_from=1, day_to=3, price=400),
                working_days=[i for i in range(15)],
                dimensions=Dimensions(width=1000, height=1000, length=1000),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=6001,
                dc_bucket_id=21,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=144, day_from=1, day_to=3, price=70)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
        ]

        cls.index.offers += [
            offer_dsbs_100,
            offer_dsbs_200,
            offer_dsbs_300,
            offer_dsbs_kgt,
            offer_almost_kgt,
            offer_in_vladik,
        ]

    @classmethod
    def prepare_unified_modifiers(cls):
        cls.index.blue_delivery_modifiers.add_modifier(
            exp_name=UNIFIED_EXP, tariffs=Unified.tarrifs, regions=[UNIFIED_RID]
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(exp_name=UNIFIED_EXP, tariffs=Unified.tarrifs_default)

    @classmethod
    def prepare_unified_dsbs_seller_payment_modifiers(cls):
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=Dsbs_Payment.tarrifs,
            regions=[UNIFIED_RID],
            is_dsbs_payment=True,
            also_use_for_dsbs_in_priority_region=True,  # Этот модификатор указывает выплату (11 руб. курьерка), которая будет использоваться
            # для DSBS доставки в приоритетный регион 75
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=Dsbs_Payment.tarrifs_default,
            is_dsbs_payment=True,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=Dsbs_Payment.tarrifs_in_vladik,
            regions=[75],
            is_dsbs_payment=True,  # Этот модификатор  нужен, чтобы проверить, что без фикса https://st.yandex-team.ru/MARKETOUT-45383
            # выплата для DSBS доставки в приоритетный регион 75 (будет 999 руб.) отличается от указанной выше
        )

    def check_all_delivery_options(
        self, response, price_courier, supplier_price_courier, price_pickup, supplier_price_pickup
    ):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "price": {
                                        "currency": "RUR",
                                        "value": str(price_courier),
                                    },
                                    "supplierPrice": {
                                        "currency": "RUR",
                                        "value": str(supplier_price_courier),
                                    },
                                    "supplierDiscount": {
                                        "currency": "RUR",
                                        "value": str(supplier_price_courier - price_courier),
                                    },
                                },
                            ],
                            "pickupOptions": [
                                {
                                    "price": {
                                        "currency": "RUR",
                                        "value": str(price_pickup),
                                    },
                                    "supplierPrice": {
                                        "currency": "RUR",
                                        "value": str(supplier_price_pickup),
                                    },
                                    "supplierDiscount": {
                                        "currency": "RUR",
                                        "value": str(supplier_price_pickup - price_pickup),
                                    },
                                }
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_unified_dsbs_seller_payment(self):
        """
        MARKETOUT-39001
        В рамках новых тарифов были добавлены компенсации dsbs партнерам
        При походе в плейс actual_delivery теперь в опциях возвращаются
        новый поля:
            supplierPrice - стоимость для поставщика;
            supplierDiscount - "скидка" поставщика, стоимость для поставщика - стоимость для пользователя (supplierPrice - price);

        Пока проверяем только курьерку и пвз
        TODO: Когда заработает почта, нужно добавить тестов и на нее
        """

        # Проверяем, что вес и цена, прееданные в параметрах запроса не вияют на тарифы для поставщика
        # Влияют сумма цен и весов товаров в посылке
        for total_price, total_weight, greater_than_threshold in ((10, 10, False), (1000, 1000, True)):
            total_cgis = '&total-price={}&total-weight-kg={}'.format(total_price, total_weight)

            # Делаем запрос только для оффера Dsbs0________________g, цена посылки - 10 (1000)
            # Значит по Unified таррифам курьерка будет стоить 2 (12) - пойдет в поле price
            # А по Dsbs_Payment courier_price=11 - пойдет в поле supplierPrice
            # В supplierDiscount будет 11 - 2 (12) == 9 (0)
            flags = '&force-white-offer-options=1&rearr-factors=market_unified_tariffs=1;market_dsbs_tariffs=1'

            request = (
                'place=actual_delivery&offers-list=Dsbs0________________g:1&rids=213&regset=1&pickup-options=raw'
                + flags
                + total_cgis
            )
            response = self.report.request_json(request)
            self.check_all_delivery_options(
                response,
                price_courier=Unified.tarrifs[1].courier_price
                if greater_than_threshold
                else Unified.tarrifs[0].courier_price,
                supplier_price_courier=Dsbs_Payment.tarrifs[0].courier_price,
                price_pickup=Unified.tarrifs[1].pickup_price
                if greater_than_threshold
                else Unified.tarrifs[0].pickup_price,
                supplier_price_pickup=Dsbs_Payment.tarrifs[0].pickup_price,
            )

            # Делаем запрос для офферов Dsbs0________________g, Dsbs1________________g цена посылки - 10 (100)
            # Значит курьерка будет стоить 1 (11)
            # А по Dsbs_Payment courier_price=111
            request = (
                'place=actual_delivery&offers-list=Dsbs0________________g:1,Dsbs1________________g:1&rids=213&regset=1&pickup-options=raw'
                + flags
                + total_cgis
            )
            response = self.report.request_json(request)
            self.check_all_delivery_options(
                response,
                price_courier=Unified.tarrifs[1].courier_price
                if greater_than_threshold
                else Unified.tarrifs[0].courier_price,
                supplier_price_courier=Dsbs_Payment.tarrifs[1].courier_price,
                price_pickup=Unified.tarrifs[1].pickup_price
                if greater_than_threshold
                else Unified.tarrifs[0].pickup_price,
                supplier_price_pickup=Dsbs_Payment.tarrifs[1].pickup_price,
            )

            # Делаем запрос для кгт офферов DsbsKGT______________g цена посылки - 10 (100)
            # Значит курьерка будет стоить 111
            # А по Dsbs_Payment courier_price=1011
            request = (
                'place=actual_delivery&offers-list=DsbsKGT______________g:1&rids=213&regset=1&pickup-options=raw'
                + flags
                + total_cgis
            )
            response = self.report.request_json(request)
            self.check_all_delivery_options(
                response,
                price_courier=Unified.tarrifs[2].courier_price,
                supplier_price_courier=Dsbs_Payment.tarrifs[2].courier_price,
                price_pickup=Unified.tarrifs[2].pickup_price,
                supplier_price_pickup=Dsbs_Payment.tarrifs[2].pickup_price,
            )

    def test_dsbs_weight_output(self):
        """
        Проверяем, что для dsbs возвращается вес по посылке
        """

        flags = '&force-white-offer-options=1&rearr-factors=market_unified_tariffs=1;market_dsbs_tariffs=1'
        # вес Dsbs0________________g == KGT / 5
        request = (
            'place=actual_delivery&offers-list=Dsbs0________________g:2&rids=213&regset=1&pickup-options=raw' + flags
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "deliveryGroup", "weight": str(int(2 * (KGT / 5)))}]},
            allow_different_len=False,
        )

        # вес Dsbs0________________g == KGT / 5, DsbsKGT______________g == KGT + 10
        request = (
            'place=actual_delivery&offers-list=Dsbs0________________g:2,DsbsKGT______________g:1&rids=213&regset=1&pickup-options=raw'
            + flags
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "deliveryGroup", "weight": str(int(2 * (KGT / 5)) + int(KGT + 10))}]},
            allow_different_len=False,
        )

        # у Dsbs1________________g нет веса => 0
        request = (
            'place=actual_delivery&offers-list=Dsbs1________________g:1&rids=213&regset=1&pickup-options=raw' + flags
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, {"results": [{"entity": "deliveryGroup", "weight": '0'}]}, allow_different_len=False
        )

    def test_unified_dsbs_user_price(self):
        """
        Проверяем, что при рассчете доставки для посылки из ДСБС офферов
        с параметром force-white-offer-options=1 цена переписывается и считается по стоимости и весу всей посылки
        """

        def check_delivery_options(response, price_courier, price_pickup):
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "options": [
                                    {
                                        "price": {
                                            "currency": "RUR",
                                            "value": str(price_courier),
                                        },
                                    },
                                ],
                                "pickupOptions": [
                                    {
                                        "price": {
                                            "currency": "RUR",
                                            "value": str(price_pickup),
                                        },
                                    }
                                ],
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

        flags = '&force-white-offer-options=1&rearr-factors=market_unified_tariffs=1;market_dsbs_tariffs=1'
        total_cgi = '&total-price={price}'

        request = (
            'place=actual_delivery&offers-list=DsbsAlmostKGT________g:1&rids=213&regset=1&pickup-options=raw'
            + flags
            + total_cgi.format(price=150)
        )
        response = self.report.request_json(request)
        # В корзине один не КГТ оффер ценой 150 (меньше трешхолда)
        check_delivery_options(
            response, price_courier=Unified.tarrifs[0].courier_price, price_pickup=Unified.tarrifs[0].pickup_price
        )

        request = (
            'place=actual_delivery&offers-list=DsbsAlmostKGT________g:1&rids=213&regset=1&pickup-options=raw'
            + flags
            + total_cgi.format(price=300)
        )
        response = self.report.request_json(request)
        # В корзине не один оффер, цена корзины выше трешхолда - 300
        check_delivery_options(
            response, price_courier=Unified.tarrifs[1].courier_price, price_pickup=Unified.tarrifs[1].pickup_price
        )

        request = (
            'place=actual_delivery&offers-list=DsbsAlmostKGT________g:2&rids=213&regset=1&pickup-options=raw'
            + flags
            + total_cgi.format(price=300)
        )
        response = self.report.request_json(request)
        # Два оффера, весом больше порога КГТ и ценой больше 200
        check_delivery_options(
            response, price_courier=Unified.tarrifs[2].courier_price, price_pickup=Unified.tarrifs[2].pickup_price
        )

    def test_delivery_thresholds(self):
        """
        Проверяем, что при рассчете полей cheaperDeliveryThreshold и offersTotalPrice для ДСБС офферов
        учитывается total-price при наличии
        """

        def check_price_and_threshold(response, total_price, threshold):
            remainder = threshold - total_price
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "offersTotalPrice": {"currency": "RUR", "value": str(total_price)},
                        "cheaperDeliveryThreshold": {"currency": "RUR", "value": str(threshold)},
                        "cheaperDeliveryRemainder": {
                            "currency": "RUR",
                            "value": str(remainder) if remainder > 0 else str(0),
                        },
                    }
                },
                allow_different_len=False,
            )

        flags = '&force-white-offer-options=1&rearr-factors=market_unified_tariffs=1;market_dsbs_tariffs=1'
        total_cgi = '&total-price={price}'

        request = (
            'place=actual_delivery&offers-list=DsbsAlmostKGT________g:1&rids=213&regset=1&pickup-options=raw'
            + flags
            + total_cgi.format(price=167)
        )
        response = self.report.request_json(request)
        # total_price = 167 (меньше трешхолда), трешхолд 200
        check_price_and_threshold(response, total_price=167, threshold=200)

        request = (
            'place=actual_delivery&offers-list=DsbsAlmostKGT________g:1&rids=213&regset=1&pickup-options=raw' + flags
        )
        response = self.report.request_json(request)
        # не указываем total_price, цена берется из оффера (150)
        check_price_and_threshold(response, total_price=150, threshold=200)

    def test_check_non_zero_weight(self):
        '''
        Проверяем, что вес посылки нормально считается, даже если у какого-нибудь оффера из запроса он отсутсвует (должен проигнорится)
        '''
        flags = '&force-white-offer-options=0&rearr-factors=market_unified_tariffs=1;market_dsbs_tariffs=1'
        total_price, total_weight = 100 + 1000, KGT + 10
        total_cgis = '&total-price={}&total-weight-kg={}'.format(total_price, total_weight)

        request = (
            'place=actual_delivery&offers-list=DsbsKGT______________g:1,DsbsNoWeight_________g:1&rids=213&regset=1&pickup-options=raw'
            + flags
            + total_cgis
        )
        response = self.report.request_json(request)
        self.check_all_delivery_options(
            response,
            price_courier=Unified.tarrifs[2].courier_price,
            supplier_price_courier=Dsbs_Payment.tarrifs[2].courier_price,
            price_pickup=Unified.tarrifs[2].pickup_price,
            supplier_price_pickup=Dsbs_Payment.tarrifs[2].pickup_price,
        )

        self.assertFragmentIn(
            response,
            {"results": [{"entity": "deliveryGroup", "weight": str(int(KGT + 10))}]},
            allow_different_len=False,
        )

    def test_dsbs_delivery_in_priority_region(self):
        for use_dsbs_delivery_modifiers_in_priority_regions in [None, False, True]:
            rearr_factors = ""
            if use_dsbs_delivery_modifiers_in_priority_regions is not None:
                rearr_factors = "&rearr-factors=use_dsbs_delivery_modifiers_in_priority_regions={rearr}".format(
                    rearr=use_dsbs_delivery_modifiers_in_priority_regions
                )

            response = self.report.request_json(
                "place=actual_delivery&offers-list=DsbsInVladik_________g:1&rids=75" + rearr_factors
            )

            # Выплата 11 руб. берется из модификатора, размеченного параметром
            # also_use_for_dsbs_in_priority_region=True (в регионе 75)
            # А выплата 999 руб. прилетает только без флага для проверки воспроизводимости проблемы,
            # которую починили в https://st.yandex-team.ru/MARKETOUT-45383
            price = 11 if use_dsbs_delivery_modifiers_in_priority_regions is not False else 999

            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "supplierPrice": {"value": str(price)},
                            }
                        ],
                    },
                },
            )


if __name__ == '__main__':
    main()

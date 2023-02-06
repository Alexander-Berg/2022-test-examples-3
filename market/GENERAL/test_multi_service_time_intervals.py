#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    GLParam,
    RegionalDelivery,
    Shop,
    TimeInfo,
    TimeIntervalInfo,
    TimeIntervalsForDaysInfo,
    TimeIntervalsForRegion,
)
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax


class _Shops(object):
    blue_shop_1 = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=2,
        name='blue_shop_1',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
    )


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=5,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )
    sku2_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price55-iLVm1Goleg',
        weight=7,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
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
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            _Shops.blue_shop_1,
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicDaysSet(key=20, days=[5, 6]),
            DynamicDaysSet(key=21, days=[7, 9]),
            DynamicDaysSet(key=22, days=[10]),
            DynamicDeliveryServiceInfo(
                id=401,
                rating=1,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=2)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=1, days_key=20),
                            TimeIntervalsForDaysInfo(intervals_key=5, days_key=21),
                            TimeIntervalsForDaysInfo(intervals_key=3, days_key=22),
                        ],
                    )
                ],
            ),
            DynamicDaysSet(key=23, days=[5]),
            DynamicDaysSet(key=24, days=[6]),
            DynamicDaysSet(key=25, days=[7]),
            DynamicDaysSet(key=26, days=[8]),
            DynamicDeliveryServiceInfo(
                id=402,
                rating=1,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=2)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=1, days_key=23),
                            TimeIntervalsForDaysInfo(intervals_key=6, days_key=24),
                            TimeIntervalsForDaysInfo(intervals_key=5, days_key=25),
                            TimeIntervalsForDaysInfo(intervals_key=2, days_key=26),
                        ],
                    )
                ],
            ),
            DynamicDaysSet(key=27, days=[5, 6, 7, 8, 9, 10]),
            DynamicDeliveryServiceInfo(
                id=403,
                rating=1,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=2)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=1, days_key=27),
                        ],
                    )
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=404,
                rating=1,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=2)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=1, days_key=27),
                        ],
                    )
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=157,
                rating=1,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=2)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=401,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=402,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=403,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=404,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            ),
            DynamicDaysSet(key=1, days=[0, 1, 2, 3]),
            DynamicDaysSet(key=2, days=[]),
            DynamicTimeIntervalsSet(
                key=0,
                intervals=[
                    TimeIntervalInfo(TimeInfo(19, 15), TimeInfo(23, 45)),
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(18, 30)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=1,
                intervals=[
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(17, 30)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=2,
                intervals=[
                    TimeIntervalInfo(TimeInfo(11, 0), TimeInfo(17, 00)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=3,
                intervals=[
                    TimeIntervalInfo(TimeInfo(11, 0), TimeInfo(15, 10)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=4,
                intervals=[
                    TimeIntervalInfo(TimeInfo(9, 0), TimeInfo(14, 30)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=5,
                intervals=[
                    TimeIntervalInfo(TimeInfo(9, 0), TimeInfo(14, 30)),
                    TimeIntervalInfo(TimeInfo(15, 0), TimeInfo(17, 30)),
                    TimeIntervalInfo(TimeInfo(18, 45), TimeInfo(22, 0)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=6,
                intervals=[
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(17, 30)),
                    TimeIntervalInfo(TimeInfo(18, 0), TimeInfo(19, 30)),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=801,
                fesh=1,
                carriers=[157],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=10, shop_delivery_price=4),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=2),
                        ],
                    ),
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=905,
                dc_bucket_id=201,
                fesh=1,
                carriers=[401],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=1, day_to=2, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=906,
                dc_bucket_id=202,
                fesh=1,
                carriers=[402],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=30, day_from=1, day_to=2, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=907,
                dc_bucket_id=203,
                fesh=1,
                carriers=[403],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=40, day_from=1, day_to=10, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=908,
                dc_bucket_id=204,
                fesh=1,
                carriers=[404],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=50, day_from=1, day_to=6, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku1_offer1],
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[801],
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=1,
                sku=2,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku2_offer1],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[801],
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=24, width=32, height=32, length=32).respond(
            [201, 202, 203, 204], [], []
        )

    @staticmethod
    def expected_options(with_multi_service):
        BEST_SERVICE_OPIONS = [
            {  # Опция с диапазоном дней. Перекрытия по времени с этой опцией не учитываются (перекрытия по времени будет учитываться только для предрассчитанных репортом опций для одного дня).
                "dayFrom": 5,
                "dayTo": 6,
                "timeIntervals": [{"from": "10:00", "to": "17:30", "isDefault": True}],
                "isDefault": True,
                "serviceId": "401",
            },
            {
                "dayFrom": 6,
                "dayTo": 6,
                "timeIntervals": [{"from": "10:00", "to": "17:30", "isDefault": True}],
                "isDefault": False,
                "serviceId": "401",
            },
            {
                "dayFrom": 7,
                "dayTo": 7,
                "timeIntervals": [
                    {"from": "09:00", "to": "14:30", "isDefault": True},
                    {"from": "15:00", "to": "17:30", "isDefault": False},
                    {"from": "18:45", "to": "22:00", "isDefault": False},
                ],
                "isDefault": False,
                "serviceId": "401",
            },
            {
                "dayFrom": 9,
                "dayTo": 9,
                "timeIntervals": [
                    {"from": "09:00", "to": "14:30", "isDefault": True},
                    {"from": "15:00", "to": "17:30", "isDefault": False},
                    {"from": "18:45", "to": "22:00", "isDefault": False},
                ],
                "isDefault": False,
                "serviceId": "401",
            },
            {
                "dayFrom": 10,
                "dayTo": 10,
                "timeIntervals": [{"from": "11:00", "to": "15:10", "isDefault": True}],
                "isDefault": False,
                "serviceId": "401",
            },
        ]

        MULTI_SERVICE_OPTIONS = [
            {  # 8 день отсутствует для службы 401, поэтому он целиком добавился от службы 402
                "dayFrom": 8,
                "dayTo": 8,
                "timeIntervals": [{"from": "11:00", "to": "17:00", "isDefault": True}],
                "serviceId": "402",
            },
            {  # для 404 службы выведена опция для 10 дня, т.к. 10 день попадает в интервал [dayForm; dayTo + 4]  (т.е. [5, 10]) для службы 401
                "dayFrom": 10,
                "dayTo": 10,
                "timeIntervals": [{"from": "10:00", "to": "17:30", "isDefault": True}],
                "orderBefore": "20",
                "isDefault": False,
                "serviceId": "404",
            },
            {
                "dayFrom": 6,
                "dayTo": 6,
                "timeIntervals": [  # расписание для 6 дня для службы 402 частично совпадает с расписаниеием для 401: интервал ["10:00"; "17:30"] был удален для слубы 402, т.к. ее опция менее приоритетна (цена доставка для 402 выше, чем у 401).  # noqa
                    {
                        "from": "18:00",
                        "to": "19:30",
                        "isDefault": False,  # значение равно False, т.к. у более приоритетной службы 401 есть "isDefault":True интервал для 6 дня
                    }
                ],
                "orderBefore": "20",
                "isDefault": False,
                "serviceId": "402",
                "shipmentDay": 4,
                "partnerType": "market_delivery",
                "region": {
                    "entity": "region",
                    "id": 213,
                    "name": "RID-213",
                    "lingua": {
                        "name": {
                            "genitive": "RID-213",
                            "preposition": " ",
                            "prepositional": "RID-213",
                            "accusative": "RID-213",
                        }
                    },
                },
            }
            # Опция (dayFrom:5 ; dayTo:6) отсутствует для службы 402, т.к. опции с интервалом дней, помеченные isDefault:False, удаляются.
            # Опция для дня 7 для службы 402 отсутствует, т.к. ее временнЫе интервалы полностью совпадают с опцией для 7 дня для службы 401
            # Опции службы 403 отсутствуют, т.к. интервал доставки в 5-14 дней не помещается целиком и не совпадает с интервалом [dayForm; dayTo + 4] (т.е. [5, 10])
        ]

        return BEST_SERVICE_OPIONS + (MULTI_SERVICE_OPTIONS if with_multi_service else [])

    def test_multi_service_time_intervals(self):
        """Проверяются различные комбинации временных интервалов для различных служб доставки. Функциональность включается флагом &show-multi-service-intervals=1
        для эесперимента, заведен флаг rearr-factors=market_blue_show_multi_service_intervals
        unset - исспользуется флаг show-multi-service-intervals
        true - соответсвует поведению &show-multi-service-intervals=1 вне зависимости от значения show-multi-service-intervals
        false - соответсвует поведению &show-multi-service-intervals=0 вне зависимости от значения show-multi-service-intervals
        """
        request = 'place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:2,Sku1Price5-IiLVm1Goleg:2&rids=213&force-use-delivery-calc=1'
        baseFlag = "&show-multi-service-intervals="
        experiment = "&rearr-factors=market_blue_show_multi_service_intervals="
        flagsIncludedMultiService = [
            baseFlag + "1",  # Старая логика
            experiment + "true",  # логика эксперимента
            baseFlag + "0" + experiment + "true",  # логика эксперимента важнее старой логики.
        ]
        flagsNotIncludedMultiService = [
            "",  # Старая логика
            baseFlag + "0",  # Старая логика
            experiment + "false",  # логика эксперимента
            baseFlag + "1" + experiment + "false",  # логика эксперимента важнее старой логики.
        ]

        for flag in flagsIncludedMultiService:
            response = self.report.request_json(request + flag)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "delivery": {
                                "options": T.expected_options(True),
                            },
                        }
                    ],
                },
            )

        for flag in flagsNotIncludedMultiService:
            response = self.report.request_json(request + flag)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "delivery": {
                                "options": T.expected_options(False),
                            },
                        }
                    ],
                },
            )


if __name__ == '__main__':
    main()

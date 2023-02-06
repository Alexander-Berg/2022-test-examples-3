#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    MnPlace,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
    SortingCenterReference,
    VCluster,
)
from core.testcase import TestCase, main
from core.matcher import Contains, NoKey, NotEmpty


class Delivery(dict):
    '''заполняет тег delivery для json-ответа'''

    def add(self, dict):
        self.update(dict)
        return self

    def priority(self):
        return self.add({"isPriorityRegion": True, "isCountrywide": True, "isAvailable": True})

    def country(self):
        return self.add({"isPriorityRegion": False, "isCountrywide": True, "isAvailable": True})

    def exists(self):
        return self.add({"isPriorityRegion": False, "isCountrywide": False, "isAvailable": True})

    def no(self):
        return self.add({"isPriorityRegion": False, "isCountrywide": False, "isAvailable": False})

    def pickup(self):
        return self.add({"hasPickup": True})

    def no_pickup(self):
        return self.add({"hasPickup": False})

    def store(self):
        return self.add({"hasLocalStore": True})

    def no_store(self):
        return self.add({"hasLocalStore": False})


class T(TestCase):
    """
    Tests that offer delivery is calculated according to https://wiki.yandex-team.ru/users/piupiu/deliverytop5/.
    Note that the wiki document describes &place=offers report, but we test delivery on
    &place=prime, because the logic should be the same everywhere, and testing &place=prime is easier.

    What is tested:
      * delivery type on the basesearch side;
      * delivery type on the meta search side (the contents of the <delivery> tag);
      * absence/presence of the priority region delimiter.
      * shop w/o delivery into priority region shown correctly for delivery regions(MARKETOUT-7175), shop id = 10
    """

    @classmethod
    def prepare_blue_delivery_price(cls):
        '''
        Отключаем цену доставки синих оферов от пользователя, т.к. в этом тесте исторически много завязок на цену в тарифе
        https://st.yandex-team.ru/MARKETOUT-34206
        '''
        cls.settings.blue_delivery_price_enabled = False

    @classmethod
    def prepare(cls):
        cls.settings.enable_testing_features = False
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(
                rid=3,
                name='Центральный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        name='Московская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=213, name='Москва'),
                            Region(rid=10758, name='Химки'),
                        ],
                    ),
                    Region(
                        rid=10650,
                        name='Брянская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=191, name='Брянск'),
                        ],
                    ),
                ],
            ),
            Region(
                rid=17,
                name='Северо-западный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=10174,
                        name='Санкт-Петербург и Ленинградская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=2, name='Санкт-Петербург'),
                        ],
                    ),
                    Region(
                        rid=10897,
                        name='Мурманская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[Region(rid=23, name='Мурманск'), Region(rid=20155, name='Оленегорск')],
                    ),
                ],
            ),
            Region(
                rid=118, name='Нидерланды', region_type=Region.COUNTRY, children=[Region(rid=10466, name='Амстердам')]
            ),
            Region(
                rid=10029,
                name='Кокосовые острова',
                region_type=Region.OVERSEAS,
                children=[Region(rid=10148, name='Уэст-Айленд', region_type=Region.CITY)],
            ),
        ]
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225], name='Московская пепячечная "Доставляем"'),
            Shop(fesh=2, priority_region=213, regions=[225], name='Московская пепячечная "Вывози сам из Москвы"'),
            Shop(fesh=3, priority_region=213, regions=[225], name='Московская пепячечная "Вывози сам из Брянска"'),
            Shop(fesh=4, priority_region=213, regions=[225], name='Московская пепячечная "Вывози сам из Химок"'),
            Shop(fesh=5, priority_region=213, regions=[225], name='Московская пепячечная "Приходи в Москву"'),
            Shop(fesh=6, priority_region=213, regions=[225], name='Московская пепячечная "Приходи в Брянск"'),
            Shop(fesh=7, priority_region=213, regions=[225], name='Московская пепячечная "Приходи в Химки"'),
            Shop(fesh=8, priority_region=213, regions=[225], name='Московская пепячечная "Доставляем, если повезет"'),
            Shop(fesh=10, priority_region=0, priority_region_original=213, regions=[191], name='Хачапуречная'),
            Shop(fesh=11, priority_region=191, regions=[225], name='Брянская пепячечная "Доставляем"'),
            Shop(
                fesh=12,
                priority_region=213,
                regions=[225],
                name='Московская пепячечная "Доставляем или приходи в Брянск"',
            ),
            Shop(
                fesh=13, priority_region=10466, regions=[225, 118], home_region=118, name='Лучшая пепячечная Амстердама'
            ),
            Shop(
                fesh=14,
                priority_region=2,
                regions=[2],
                name='Питерская пепячечная, доставляем в постамат',
                delivery_service_outlets=[555],
                cpa=Shop.CPA_REAL,
            ),
        ]
        cls.index.outlets += [
            Outlet(fesh=2, region=213, point_type=Outlet.FOR_PICKUP, point_id=10002),
            Outlet(fesh=3, region=191, point_type=Outlet.FOR_PICKUP, point_id=776),
            Outlet(fesh=4, region=10758, point_type=Outlet.FOR_PICKUP, point_id=10004),
            Outlet(fesh=5, region=213, point_type=Outlet.FOR_STORE, point_id=10005),
            Outlet(fesh=6, region=191, point_type=Outlet.FOR_STORE, point_id=777),
            Outlet(fesh=7, region=10758, point_type=Outlet.FOR_STORE, point_id=10007),
            Outlet(fesh=12, region=191, point_type=Outlet.FOR_STORE, point_id=10012),
            Outlet(region=2, point_type=Outlet.FOR_POST_TERM, point_id=555, delivery_service_id=103),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5002,
                fesh=2,
                carriers=[99],
                options=[PickupOption(outlet_id=10002)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=3,
                carriers=[99],
                options=[PickupOption(outlet_id=776)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=4,
                carriers=[99],
                options=[PickupOption(outlet_id=10004)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=5,
                carriers=[99],
                options=[PickupOption(outlet_id=10005)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5006,
                fesh=6,
                carriers=[99],
                options=[PickupOption(outlet_id=777)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5007,
                fesh=7,
                carriers=[99],
                options=[PickupOption(outlet_id=10007)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5012,
                fesh=12,
                carriers=[99],
                options=[PickupOption(outlet_id=10012)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5103,
                carriers=[103],
                options=[PickupOption(outlet_id=555)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        local_delivery = [DeliveryOption(price=100, day_from=0, day_to=2, order_before=24)]
        delivery_on_russia = [RegionalDelivery(rid=225, options=[DeliveryOption(price=100)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=1225, fesh=1, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=3225, fesh=3, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=6225, fesh=6, carriers=[4], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=8225, fesh=8, carriers=[1], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=10225, fesh=10, carriers=[1], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=11225, fesh=11, carriers=[1], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=12225, fesh=12, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=13225, fesh=13, carriers=[5, 7], regional_options=delivery_on_russia),
        ]

        cls.index.offers += [
            Offer(
                ts=101,
                fesh=1,
                title='moscow-pepyaka-with-delivery',
                hyperid=2,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
            ),
            Offer(
                ts=108,
                fesh=8,
                title='moscow-pepyaka-with-delivery-and-forbidden',
                hyperid=2,
                delivery_options=local_delivery,
                forbidden_regions=[213, 10758],
                delivery_buckets=[8225],
            ),
            Offer(
                ts=102,
                fesh=2,
                title='moscow-pepyaka-with-pickup-in-moscow',
                has_delivery_options=False,
                pickup_buckets=[5002],
            ),
            Offer(
                ts=103,
                fesh=3,
                title='moscow-pepyaka-with-pickup-in-bryansk',
                hyperid=49,
                has_delivery_options=False,
                pickup_buckets=[5003],
            ),
            Offer(
                ts=104,
                fesh=4,
                title='moscowPepyakaWithPickupInKhimki',
                has_delivery_options=False,
                pickup_buckets=[5004],
            ),
            Offer(
                ts=105,
                fesh=5,
                title='moscow-pepyaka-with-store-in-moscow',
                has_delivery_options=False,
                pickup_buckets=[5005],
            ),
            Offer(
                ts=106,
                fesh=6,
                title='moscow-pepyaka-with-store-in-bryansk',
                hyperid=50,
                has_delivery_options=False,
                pickup_buckets=[5006],
            ),
            Offer(
                ts=107,
                fesh=7,
                title='moscow-pepyaka-with-store-in-khimki',
                has_delivery_options=False,
                pickup_buckets=[5007],
            ),
            Offer(ts=110, fesh=10, title='no-a-priority-region-delivery', hyperid=100, delivery_buckets=[10225]),
            Offer(
                ts=111,
                fesh=11,
                title='bryansk-pepyaka-with-delivery-and-forbidden',
                hyperid=2,
                has_delivery_options=True,
                delivery_options=local_delivery,
                forbidden_regions=[213],
                delivery_buckets=[11225],
            ),
            Offer(
                ts=112,
                fesh=12,
                title='moscow-pepyaka-with-delivery-and-store-in-bryansk',
                has_delivery_options=True,
                delivery_options=local_delivery,
                delivery_buckets=[12225],
                pickup_buckets=[5012],
            ),
            Offer(
                ts=113,
                fesh=13,
                title='amsterdam-pepyaka-with-delivery',
                has_delivery_options=True,
                delivery_options=local_delivery,
                delivery_buckets=[13225],
            ),
            Offer(
                ts=114,
                fesh=14,
                title='spbPepyakaWithPickup',
                has_delivery_options=False,
                store=False,
                post_term_delivery=True,
                cpa=Offer.CPA_REAL,
                pickup_buckets=[5103],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.36)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.34)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 106).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 108).respond(0.32)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 111).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 112).respond(0.33)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 113).respond(0.44)

        cls.index.offers += [
            Offer(ts=10011, fesh=1, hyperid=1001, title='with-delivery-from-moscow', delivery_buckets=[1225], randx=11),
            Offer(
                ts=10012,
                fesh=3,
                hyperid=1001,
                title='with-delivery-from-moscow-and-pickup-in-bryansk',
                delivery_buckets=[3225],
                randx=12,
                pickup_buckets=[5003],
            ),
            Offer(
                ts=10013,
                fesh=6,
                hyperid=1001,
                title='with-delivery-from-moscow-and-store-in-bryansk',
                delivery_buckets=[6225],
                randx=13,
                pickup_buckets=[5006],
            ),
            Offer(
                ts=10014,
                fesh=13,
                hyperid=1001,
                title='with-delivery-from-amsterdam',
                delivery_buckets=[13225],
                randx=14,
            ),
            Offer(
                ts=10015,
                fesh=11,
                hyperid=1001,
                title='with-delivery-from-bryansk',
                delivery_options=local_delivery,
                randx=15,
            ),
            Offer(fesh=1, hyperid=11001),
            Offer(fesh=3, hyperid=11001, pickup_buckets=[5003]),
            Offer(fesh=6, hyperid=11001, pickup_buckets=[5006]),
            Offer(fesh=6, hyperid=11001, pickup_buckets=[5006]),
            Offer(fesh=7, hyperid=11001, pickup_buckets=[5007]),
            Offer(fesh=7, hyperid=11001, pickup_buckets=[5007]),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10011).respond(0.036)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10012).respond(0.035)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10013).respond(0.034)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10014).respond(0.033)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10015).respond(0.032)

        cls.index.offers += [
            Offer(
                ts=20011,
                fesh=1,
                vclusterid=1000000001,
                title='with-delivery-from-moscow',
                delivery_buckets=[1225],
                randx=11,
            ),
            Offer(
                ts=20012,
                fesh=3,
                vclusterid=1000000001,
                title='with-delivery-from-moscow-and-pickup-in-bryansk',
                delivery_buckets=[3225],
                randx=12,
                pickup_buckets=[5003],
            ),
            Offer(
                ts=20013,
                fesh=6,
                vclusterid=1000000001,
                title='with-delivery-from-moscow-and-store-in-bryansk',
                delivery_buckets=[6225],
                randx=13,
                pickup_buckets=[5006],
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, title='model-without-offers'),
            Model(hyperid=2, title='model-with-offers'),
        ]

        # сохранение типа доставки для оффера при regset=1
        cls.index.offers += [Offer(fesh=106, pickup_buckets=[5106])]

        cls.index.outlets += [Outlet(fesh=106, region=1, point_type=Outlet.FOR_PICKUP, point_id=11106)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5106,
                fesh=106,
                carriers=[99],
                options=[PickupOption(outlet_id=11106)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        # shop priority country MARKETOUT-9596
        cls.index.shops += [
            Shop(fesh=101, priority_region=213, regions=[225], name='Moskvichoff'),
            Shop(fesh=102, priority_region=10466, regions=[118, 225], name='Amsterdamoff'),
            Shop(fesh=103, priority_region=10148, regions=[10029, 225], name='Kokosoff'),
        ]
        cls.index.offers += [
            Offer(fesh=101, hyperid=201, price=123, title="Moskovskaya plitka"),
            Offer(fesh=102, hyperid=202, price=234, title="Red fonar"),
            Offer(fesh=103, hyperid=203, price=345, title="Kokosy"),
        ]

        # test_numeric_day_from_to
        cls.index.shops += [Shop(fesh=19, priority_region=213)]

        cls.index.offers += [
            Offer(fesh=19, title='day_from_to', delivery_options=[DeliveryOption(day_from=1, day_to=2)])
        ]

        # calendars for MarDo program
        cls.index.shipment_service_calendars += [
            DeliveryCalendar(
                fesh=757575,
                calendar_id=9,
                date_switch_hour=5,
                holidays=[0, 1, 2],
                sc_references=[SortingCenterReference(sc_id=7891, duration=1, default=True)],
            ),
            DeliveryCalendar(
                fesh=757575, calendar_id=7891, date_switch_hour=5, holidays=[0, 1, 2, 3], is_sorting_center=True
            ),
            DeliveryCalendar(fesh=757576, calendar_id=9, date_switch_hour=5, holidays=[]),
            DeliveryCalendar(fesh=757576, calendar_id=10, date_switch_hour=5, holidays=[]),
            DeliveryCalendar(fesh=757576, calendar_id=21, date_switch_hour=5, holidays=[]),
            DeliveryCalendar(fesh=757576, calendar_id=22, date_switch_hour=5, holidays=[]),
            DeliveryCalendar(fesh=757576, calendar_id=23, date_switch_hour=5, holidays=[]),
            DeliveryCalendar(fesh=757576, calendar_id=31, date_switch_hour=5, holidays=[]),
            DeliveryCalendar(fesh=757576, calendar_id=32, date_switch_hour=5, holidays=[]),
        ]

    def test_offer_moscow_moscow_with_delivery(self):
        response = self.report.request_json(
            'place=prime&rids=213&debug=da&text="moscow-pepyaka-with-delivery"&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "value": "3", "width": "2"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"}, preserve_order=True)
        self.assertFragmentIn(
            response,
            {"delivery": {"isPriorityRegion": True, "isCountrywide": True, "isAvailable": True}},
            preserve_order=True,
        )

    def test_offer_moscow_bryansk_with_delivery(self):
        response = self.report.request_json(
            'place=prime&rids=191&debug=da&text="moscow-pepyaka-with-delivery"&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "value": "2", "width": "2"}, preserve_order=True)
        self.assertFragmentIn(response, {"entity": "regionalDelimiter"}, preserve_order=True)
        self.assertFragmentIn(
            response,
            {"delivery": {"isPriorityRegion": False, "isCountrywide": True, "isAvailable": True}},
            preserve_order=True,
        )

    # черта Доставка из других регионов не должна рисоваться если флаг market_hide_regional_delimiter=1
    def test_invisible_region_delimiter(self):
        # prime
        response = self.report.request_json(
            'place=prime&rids=191&debug=da&text="moscow-pepyaka-with-delivery"&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"entity": "regionalDelimiter"}, preserve_order=True)

        response = self.report.request_json(
            'place=prime&rids=191&debug=da&text="moscow-pepyaka-with-delivery"&rearr-factors=market_hide_regional_delimiter=0&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"entity": "regionalDelimiter"}, preserve_order=True)

        response = self.report.request_json(
            'place=prime&rids=191&debug=da&text="moscow-pepyaka-with-delivery"&rearr-factors=market_hide_regional_delimiter=1&rearr-factors=offer_regions_literal=1;;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"}, preserve_order=True)

    def test_offer_moscow_moscow_with_pickup_in_moscow(self):
        response = self.report.request_json(
            'place=prime&rids=213&debug=da&text=moscow-pepyaka-with-pickup-in-moscow&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "value": "3", "width": "2"})
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"})
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "moscow-pepyaka-with-pickup-in-moscow"},
                "delivery": {"isPriorityRegion": False, "hasPickup": True, "isAvailable": False},
            },
        )

    def test_offer_moscow_moscow_with_pickup_in_bryansk(self):
        response = self.report.request_json(
            'place=prime&rids=213&debug=da&text=moscow-pepyaka-with-pickup-in-bryansk&debug-doc-count=10'
            '&rearr-factors=disable_panther_quorum=0;offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"DROP_REASON": "DELIVERY"}, preserve_order=True)

    def test_offer_moscow_bryansk_with_pickup_in_bryansk(self):
        # Если у магазина есть аутлет в регионе пользователя, то мы такой оффер покажем
        # Данную логику ввели в https://st.yandex-team.ru/MARKETOUT-8364
        # Запросим оффер из региона пользователя rids=191
        # Оффер не должен отфильтроваться и у него должен быть самовывоз (pickup)
        # При этом оффер должен быть показан под чертой, т.к. приоритетный регион магазина не совпадает с регионом пользователя
        response = self.report.request_json(
            'place=prime&rids=191&debug=da&text="moscow-pepyaka-with-pickup-in-bryansk"&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"delivery": {"hasPickup": True}})
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "value": "2", "width": "2"}, preserve_order=True)
        self.assertFragmentIn(response, {"entity": "regionalDelimiter"}, preserve_order=True)  # Показ под чертой

    def test_offer_moscow_user_moscow_pickup_in_moscow(self):
        # Магазин в Москве. У него нет курьерской доставки, нет стора. Но есть точка самовывоза.
        # Пользователь тоже из москвы.
        # Проверяем, что товар показывается и показывается НАД чертой
        response = self.report.request_json(
            'place=prime&rids=213&debug=da&text=moscow-pepyaka-with-pickup-in-moscow&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response, {"titles": {"raw": "moscow-pepyaka-with-pickup-in-moscow"}, "delivery": {"hasPickup": True}}
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "value": "3", "width": "2"})
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"})  # Показ над чертой

    def test_offer_moscow_moscow_with_store_in_moscow(self):
        response = self.report.request_json(
            'place=prime&rids=213&debug=da&text=moscow-pepyaka-with-store-in-moscow&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "value": "3", "width": "2"})
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"})
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "moscow-pepyaka-with-store-in-moscow"},
                "delivery": {"isAvailable": False, "hasLocalStore": True},
            },
        )

    def test_offer_moscow_moscow_with_store_in_bryansk(self):
        response = self.report.request_json(
            'place=prime&rids=213&debug=da&text=moscow-pepyaka-with-store-in-bryansk&debug-doc-count=10'
            '&rearr-factors=disable_panther_quorum=0;offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"DROP_REASON": "DELIVERY"})

    def test_offer_moscow_bryansk_with_store_in_bryansk(self):
        response = self.report.request_json(
            'place=prime&rids=191&debug=da&text=moscow-pepyaka-with-store-in-bryansk&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "value": "3", "width": "2"})
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "moscow-pepyaka-with-store-in-bryansk"},
                "delivery": {"isAvailable": False, "isPriorityRegion": False, "hasLocalStore": True},
            },
        )

    def test_priority_forbidden_region(self):
        response = self.report.request_json(
            'place=prime&rids=213&debug=da&debug-doc-count=10&text="moscow-pepyaka-with-delivery-and-forbidden"'
            '&rearr-factors=market_no_strict_distances=0;disable_panther_quorum=0;offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentNotIn(response, {"results": [{"entity": "offer"}]})
        self.assertFragmentIn(response, {"DROP_REASON": "DELIVERY"})

        response = self.report.request_json(
            'place=prime&rids=10758&debug=da&debug-doc-count=10&text="moscow-pepyaka-with-delivery-and-forbidden"'
            '&rearr-factors=market_no_strict_distances=0;disable_panther_quorum=0;offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentNotIn(response, {"results": [{"entity": "offer"}]})
        self.assertFragmentIn(response, {"DROP_REASON": "DELIVERY"})

        response = self.report.request_json(
            'place=prime&rids=191&debug=da&text="moscow-pepyaka-with-delivery-and-forbidden"'
            '&rearr-factors=market_no_strict_distances=0;offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "width": "2", "value": "2"})
        self.assertFragmentIn(response, {"entity": "regionalDelimiter"})
        self.assertFragmentIn(response, {"delivery": {"isCountrywide": True, "isAvailable": True}})

    def test_non_priority_forbidden_region(self):
        response = self.report.request_json(
            'place=prime&rids=213&debug=da&debug-doc-count=10&text="bryansk-pepyaka-with-delivery-and-forbidden"'
            '&rearr-factors=market_no_strict_distances=0;disable_panther_quorum=0;offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentNotIn(response, {"results": [{"entity": "offer"}]})
        self.assertFragmentIn(response, {"DROP_REASON": "DELIVERY"})

        response = self.report.request_json(
            'place=prime&rids=191&debug=da&text="bryansk-pepyaka-with-delivery-and-forbidden"&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "width": "2", "value": "3"})
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}]})

    def test_outlets_count_for_store(self):
        response = self.report.request_json(
            'place=productoffers&rids=191&bsformat=1&hyperid=50&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'shop': {'outletsCount': 1, 'name': 'Московская пепячечная "Приходи в Брянск"'},
                        'titles': {'raw': 'moscow-pepyaka-with-store-in-bryansk'},
                        'outlet': {'id': '777', 'isMarketBranded': False},
                    }
                ]
            },
        )
        self.assertEqual(response.count({'entity': 'outlet'}, True), 1)

        response = self.report.request_json(
            'place=productoffers&rids=191&bsformat=1&hyperid=49&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'shop': {'outletsCount': 1, 'name': 'Московская пепячечная "Вывози сам из Брянска"'},
                        'titles': {'raw': 'moscow-pepyaka-with-pickup-in-bryansk'},
                        'outlet': {'id': '776'},
                    }
                ]
            },
        )
        self.assertEqual(response.count({'entity': 'outlet'}, True), 1)

        response = self.report.request_json(
            'place=productoffers&rids=191&bsformat=1&hyperid=100&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'shop': {'outletsCount': 0, 'name': 'Хачапуречная'},
                        'titles': {'raw': 'no-a-priority-region-delivery'},
                    }
                ]
            },
        )
        self.assertEqual(response.count({'entity': 'outlet'}, True), 0)

    # предложения с доставкой из любого региона поднимаются над черту
    # при local-offers-first=0

    def test_pessimization_delivery_from_non_priority_region_on_prime(self):
        # запрос в регионе 191 (Брянск) с local-offers-first=1  - офферы с доставкой из других областей находятся под чертой
        # офферы с доставкой из Брянска, офферы с самовывозом из Брянска и офферы из магазинов Брянска -  над чертой
        # офферы с самовывозом (с доставкой до точки самовывоза) и офферы с доставкой из других регионов - под чертой
        # офферы с доставкой из другой страны под чертой (и даже под второй чертой, т.е. идут последними)

        expected_delimiter = {
            'results': [
                {
                    "titles": {"raw": "bryansk-pepyaka-with-delivery-and-forbidden"},
                    "debug": {"formulaValue": "0.400000006"},
                    "delivery": Delivery().priority().no_pickup().no_store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-delivery-and-store-in-bryansk"},
                    "debug": {"formulaValue": "0.330000013"},
                    "delivery": Delivery().country().no_pickup().store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-store-in-bryansk"},
                    "debug": {"formulaValue": "0.300000012"},
                    "delivery": Delivery().no().no_pickup().store(),
                },
                {"entity": "regionalDelimiter"},
                {
                    "titles": {"raw": "moscow-pepyaka-with-delivery"},
                    "debug": {"formulaValue": "0.360000014"},
                    "delivery": Delivery().country().no_pickup().no_store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-pickup-in-bryansk"},
                    "debug": {"formulaValue": "0.340000004"},
                    "delivery": Delivery().no().pickup().no_store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-delivery-and-forbidden"},
                    "debug": {"formulaValue": "0.319999993"},
                    "delivery": Delivery().country().no_pickup().no_store(),
                },
                {
                    "titles": {"raw": "amsterdam-pepyaka-with-delivery"},
                    "debug": {"formulaValue": "0.439999998"},
                    "delivery": Delivery().exists().no_pickup().no_store(),
                },
            ]
        }

        response = self.report.request_json(
            'place=prime&rids=191&text=pepyaka&debug=da'
            + '&local-offers-first=1&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, expected_delimiter, preserve_order=True, allow_different_len=False)

        # запрос без флага с local-offers-first=0
        # запрос с фактором пессимизации 1.0 и local-offers-first=0
        # переводит офферы с доставкой из других регионов (из Москвы) над черту
        # переводит офферы с доставкой из других стран над черту
        # переводит офферы с самовывозом (с доставкой из других регионов) над черту
        # (также для офферов с курьерской доставкой isPriorityRegion=True)
        # все офферы над чертой, поэтому отсутствует regionalDelimiter

        expected_no_delimiter = {
            'results': [
                {
                    "titles": {"raw": "amsterdam-pepyaka-with-delivery"},
                    "debug": {"formulaValue": "0.439999998"},
                    "delivery": Delivery().exists().no_pickup().no_store(),
                },
                {
                    "titles": {"raw": "bryansk-pepyaka-with-delivery-and-forbidden"},
                    "debug": {"formulaValue": "0.400000006"},
                    "delivery": Delivery().priority().no_pickup().no_store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-delivery"},
                    "debug": {"formulaValue": "0.360000014"},
                    "delivery": Delivery().country().no_pickup().no_store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-pickup-in-bryansk"},
                    "debug": {"formulaValue": "0.340000004"},
                    "delivery": Delivery().no().pickup().no_store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-delivery-and-store-in-bryansk"},
                    "debug": {"formulaValue": "0.330000013"},
                    "delivery": Delivery().country().no_pickup().store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-delivery-and-forbidden"},
                    "debug": {"formulaValue": "0.319999993"},
                    "delivery": Delivery().country().no_pickup().no_store(),
                },
                {
                    "titles": {"raw": "moscow-pepyaka-with-store-in-bryansk"},
                    "debug": {"formulaValue": "0.300000012"},
                    "delivery": Delivery().no().no_pickup().store(),
                },
            ]
        }

        response = self.report.request_json(
            'place=prime&rids=191&text=pepyaka&debug=da'
            + '&local-offers-first=0&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, expected_no_delimiter, preserve_order=True, allow_different_len=False)

    # MARKETOUT-9596
    def test_shop_priority_country_russia_to_russia(self):
        response = self.report.request_json('place=productoffers&hyperid=201&rids=213')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "model": {"id": 201},
                "delivery": {
                    "shopPriorityRegion": {
                        "entity": "region",
                        "id": 213,  # Moscow
                    },
                    "shopPriorityCountry": {
                        "entity": "region",
                        "id": 225,  # Russia
                    },
                },
            },
        )

    def test_shop_priority_country_netherlands_to_russia(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=202&rids=213&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "model": {"id": 202},
                "delivery": {
                    "shopPriorityRegion": {
                        "entity": "region",
                        "id": 10466,  # Amsterdam
                    },
                    "shopPriorityCountry": {
                        "entity": "region",
                        "id": 118,  # Netherlands
                    },
                },
            },
        )

    def test_shop_priority_country_kokosy_to_russia(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=203&rids=213&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "model": {"id": 203},
                "delivery": {
                    "shopPriorityRegion": {
                        "entity": "region",
                        "id": 10148,  # West Island
                    },
                    "shopPriorityCountry": None,
                },
            },
        )

    def test_offer_sbp_pickup(self):
        # Магазин в Спб. Есть только самовывоз.
        # Пользователь тоже из спб.
        # Проверяем, что товар показывается и показывается НАД чертой
        response = self.report.request_json(
            'place=prime&rids=2&text=spbPepyakaWithPickup&debug=da&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "delivery": {"hasPickup": True, "availableServices": [{"serviceId": 103, "isMarketBranded": True}]},
                "outlet": {"isMarketBranded": False},
            },
        )
        self.assertFragmentIn(response, {"name": "DELIVERY_TYPE", "value": "3"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"}, preserve_order=True)  # Показ над чертой

    def test_numeric_day_from_to(self):
        response = self.report.request_json(
            'place=prime&text=day_from_to&rids=213&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "day_from_to"},
                "delivery": {"options": [{"dayFrom": 1, "dayTo": 2}]},
            },
        )

    @classmethod
    def prepare_outlet_type_statistics(cls):
        cls.index.shops += [
            Shop(
                fesh=50,
                priority_region=1,
                regions=[1],
                name='Пепячечная N1',
                delivery_service_outlets=[100, 101, 102, 103],
                cpa=Shop.CPA_REAL,
            )
        ]
        cls.index.outlets += [
            Outlet(fesh=50, point_id=200, region=1),
            Outlet(fesh=50, point_id=201, region=1, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=50, point_id=203, region=1, point_type=Outlet.FOR_STORE),
            Outlet(fesh=50, point_id=204, region=1, point_type=Outlet.FOR_STORE),
            Outlet(delivery_service_id=103, point_id=100, region=1, point_type=Outlet.FOR_POST_TERM),
            Outlet(delivery_service_id=103, point_id=101, region=1, point_type=Outlet.FOR_POST_TERM),
            Outlet(delivery_service_id=103, point_id=102, region=1, point_type=Outlet.FOR_POST_TERM),
            Outlet(delivery_service_id=103, point_id=103, region=1, point_type=Outlet.FOR_POST_TERM),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=7050,
                fesh=50,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=200),
                    PickupOption(outlet_id=201),
                    PickupOption(outlet_id=203),
                    PickupOption(outlet_id=204),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=7103,
                carriers=[103],
                options=[
                    PickupOption(outlet_id=100),
                    PickupOption(outlet_id=101),
                    PickupOption(outlet_id=102),
                    PickupOption(outlet_id=103),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                ts=200,
                fesh=50,
                title='pepyaka-with-multiple-points_cpa_real',
                store=True,
                post_term_delivery=True,
                cpa=Offer.CPA_REAL,
                pickup_buckets=[7050, 7103],
            ),
            Offer(
                ts=201,
                fesh=50,
                title='pepyaka-with-multiple-points_cpa_no',
                store=True,
                post_term_delivery=True,
                cpa=Offer.CPA_NO,
                pickup_buckets=[7050, 7103],
            ),
        ]

    def test_offer_outlet_stats(self):
        response = self.report.request_json(
            'place=prime&rids=1&text=pepyaka-with-multiple-points_cpa_real&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "outletsCount": 8,
                    "storesCount": 3,
                    "pickupStoresCount": 5,
                    "postomatStoresCount": 4,
                }
            },
        )

    def test_offer_outlet_stats_cpa_no(self):
        response = self.report.request_json(
            'place=prime&rids=1&text=pepyaka-with-multiple-points_cpa_no&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "outletsCount": 8,
                    "storesCount": 3,
                    "pickupStoresCount": 5,
                    "postomatStoresCount": 4,
                }
            },
        )

    @classmethod
    def prepare_for_differ_models_by_delivery(cls):
        '''Тестируем фичу: Перевод кластеров и моделей без локальных офферов под черту
        с включенной галкой local-offers-first=1 (сначала предложения из моего региона)
        модели/кластера - НЕ имеющие локальных офферов или оффлайн-офферов (задается в статистиках индексатора)
        переводятся под черту "Доставка из других регионов"

        Заводим разных моделей/кластеров с различным соотношением
        - количества локальных офферов (доставляемых из региона пользователя)
        - количества оффлайн-офферов (продаваемых в оффлайн магазинах или загружаемых через интернет)
        - количества региональных офферов
        по отношению к региону 213 Москва
        (в том числе модели/кластера которые имеют только локальные офферы, только оффлайн-офферы,
        ни тех ни других или вообще не имеют информации о количестве офферов в данном регионе)
        '''
        cls.index.models += [
            Model(hyperid=2001, ts=2001, title='Oculus Rift model with local and offline offers in Moscow'),
            Model(hyperid=2002, ts=2002, title='Oculus Rift model with only regional offers in Moscow'),
            Model(hyperid=2003, ts=2003, title='Oculus Rift model with only local offers in Moscow'),
            Model(hyperid=2004, ts=2004, title='Oculus Rift model with only offline offers in Moscow'),
            Model(hyperid=2005, ts=2005, title='Oculus Rift model without offers'),
            Model(hyperid=2006, ts=2006, title='Oculus Rift model without information about offers'),
        ]

        cls.index.vclusters += [
            VCluster(vclusterid=2000000007, ts=2007, title='Oculus Rift cluster with only regional offers in Moscow'),
            VCluster(vclusterid=2000000008, ts=2008, title='Oculus Rift cluster with only local offers in Moscow'),
            VCluster(vclusterid=2000000009, ts=2009, title='Oculus Rift cluster with only offline offers in Moscow'),
            VCluster(vclusterid=2000000010, ts=2010, title='Oculus Rift cluster with information only globally'),
        ]

        # фиксируем релевантность докумкентов - чем меньше ts - тем релевантнее
        for ts in range(2001, 2010 + 1):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.1 + 0.01 * (20 - ts % 20))

        cls.index.regional_models += [
            # есть локальные и оффлайн офферы в Москве
            RegionalModel(hyperid=2001, rids=[213], offers=20, local_offers=5, offline_offers=3),
            RegionalModel(hyperid=2001, offers=100, local_offers=50, offline_offers=20),
            # нет ни локальных ни оффлайн офферов, есть только региональные офферы
            RegionalModel(hyperid=2002, rids=[213], offers=12, local_offers=0, offline_offers=0),
            RegionalModel(hyperid=2002, offers=30, local_offers=30, offline_offers=9),
            # есть только локальные офферы
            RegionalModel(hyperid=2003, rids=[213], offers=5, local_offers=2, offline_offers=0),
            RegionalModel(hyperid=2004, offers=18, local_offers=6, offline_offers=4),
            # есть только оффлайн офферы
            RegionalModel(hyperid=2004, rids=[213], offers=5, local_offers=0, offline_offers=3),
            RegionalModel(hyperid=2004, offers=23, local_offers=0, offline_offers=0),
            # вообще нет офферов - модель не в продаже в Москве
            RegionalModel(hyperid=2005, rids=[213], offers=0, local_offers=0, offline_offers=0),
            RegionalModel(hyperid=2005, offers=2, local_offers=1, offline_offers=1),
            # есть только региональные офферы
            RegionalModel(hyperid=2000000007, rids=[213], offers=23, local_offers=0, offline_offers=0),
            RegionalModel(hyperid=2000000007, offers=145, local_offers=24, offline_offers=0),
            # есть только локальные офферы
            RegionalModel(hyperid=2000000008, rids=[213], offers=3, local_offers=1, offline_offers=0),
            RegionalModel(hyperid=2000000008, offers=12, local_offers=0, offline_offers=0),
            # есть только оффлайн офферы
            RegionalModel(hyperid=2000000009, rids=[213], offers=18, local_offers=0, offline_offers=6),
            RegionalModel(hyperid=2000000009, offers=45, local_offers=0, offline_offers=0),
            # есть локальные, оффлайн и региональные офферы
            RegionalModel(hyperid=2000000010, offers=130, local_offers=20, offline_offers=10),
        ]

        cls.index.shops += [
            Shop(fesh=20201, name='Moscow Shop', priority_region=213, regions=[213]),
            Shop(fesh=20202, name='New Moscow Stone Shop', priority_region=213312),
            Shop(fesh=20203, name='Piter shop', priority_region=2, regions=[225]),
        ]

        delivery_on_russia = [RegionalDelivery(rid=225, options=[DeliveryOption(price=100, day_from=3, day_to=5)])]
        cls.index.delivery_buckets += [DeliveryBucket(bucket_id=20203, fesh=20203, regional_options=delivery_on_russia)]

        cls.index.outlets += [Outlet(fesh=20202, point_type=Outlet.FOR_STORE, region=213, point_id=2020201)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=7202,
                fesh=20202,
                carriers=[99],
                options=[PickupOption(outlet_id=2020201)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            # Oculus Rift model with local and offline offers in Moscow
            Offer(
                hyperid=2001, fesh=20202, has_delivery_options=False, pickup=False, store=True, pickup_buckets=[7202]
            ),
            Offer(hyperid=2001, fesh=20201),
            # Oculus Rift model with only regional offers in Moscow
            Offer(hyperid=2002, fesh=20203, delivery_buckets=[20203]),
            # Oculus Rift model with only local offers in Moscow
            Offer(hyperid=2003, fesh=20201),
            # Oculus Rift model with only offline offers in Moscow
            Offer(
                hyperid=2004, fesh=20202, has_delivery_options=False, pickup=False, store=True, pickup_buckets=[7202]
            ),
            # Oculus Rift cluster with only regional offers in Moscow
            Offer(vclusterid=2000000007, fesh=20203, delivery_buckets=[20203]),
            # Oculus Rift cluster with only local offers in Moscow
            Offer(vclusterid=2000000008, fesh=20201),
            # Oculus Rift cluster with only offline offers in Moscow
            Offer(
                vclusterid=2000000009,
                fesh=20202,
                has_delivery_options=False,
                pickup=False,
                store=True,
                pickup_buckets=[7202],
            ),
        ]

    def test_differ_models_by_delivery(self):
        '''Тестируем фичу: Перевод кластеров и моделей без локальных офферов под черту

        Проверяем что при local-offers-first=1 (по умолчнанию 1)
        модели, имеющие локальные или оффлайн офферы - НАД чертой "Доставка из других регионов"
        модели, не имеющие ни локальных ни оффлайн офферов - ПОД чертой "Доставка из других регионов

        Также проверяем что берутся статистики для 213 региона
        (или 0 если для этого региона нет статистик, например для кластера 2000000010 - он будет под чертой)
        '''

        # состояние с local-offers-first=0: нет разделения по доставке все модели/кластера находятся над чертой
        # на выдаче модели/кластера не в продаже опускаются в конец страницы
        without_delimiter = {
            "results": [
                {"titles": {"raw": "Oculus Rift model with local and offline offers in Moscow"}, "id": 2001},
                {"titles": {"raw": "Oculus Rift model with only regional offers in Moscow"}, "id": 2002},
                {"titles": {"raw": "Oculus Rift model with only local offers in Moscow"}, "id": 2003},
                {"titles": {"raw": "Oculus Rift model with only offline offers in Moscow"}, "id": 2004},
                {"titles": {"raw": "Oculus Rift model without offers"}, "id": 2005},
                {"titles": {"raw": "Oculus Rift model without information about offers"}, "id": 2006},
                {"titles": {"raw": "Oculus Rift cluster with only regional offers in Moscow"}, "id": 2000000007},
                {"titles": {"raw": "Oculus Rift cluster with only local offers in Moscow"}, "id": 2000000008},
                {"titles": {"raw": "Oculus Rift cluster with only offline offers in Moscow"}, "id": 2000000009},
                {"titles": {"raw": "Oculus Rift cluster with information only globally"}, "id": 2000000010},
            ]
        }
        # порядок документов с базового соотеветствует описанному выше
        basesearch_order_without_delimiter = [2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010]
        trace_without_delimiter = {
            'logicTrace': [Contains('Apply doc : {}'.format(ts)) for ts in basesearch_order_without_delimiter]
        }

        # состояние когда модели и кластера без локальных/оффлайн/загружаемых офферов находятся под чертой
        # Примечание:
        # В тесте можно заметить, что модели и кластера не в продаже при ранжировании с чертой
        # оказываются под чертой, но не ранжируются ниже чем модели с офферами
        # причина: DELIVERY_TYPE обновляется только у моделей с офферами
        # а у моделей не в продаже DELIVERY_TYPE остается равным Country как на базовых
        with_delimiter = {
            "results": [
                {"titles": {"raw": "Oculus Rift model with local and offline offers in Moscow"}, "id": 2001},
                {"titles": {"raw": "Oculus Rift model with only local offers in Moscow"}, "id": 2003},
                {"titles": {"raw": "Oculus Rift model with only offline offers in Moscow"}, "id": 2004},
                {"titles": {"raw": "Oculus Rift cluster with only local offers in Moscow"}, "id": 2000000008},
                {"titles": {"raw": "Oculus Rift cluster with only offline offers in Moscow"}, "id": 2000000009},
                {"entity": "regionalDelimiter"},
                {"titles": {"raw": "Oculus Rift model with only regional offers in Moscow"}, "id": 2002},
                {"titles": {"raw": "Oculus Rift cluster with only regional offers in Moscow"}, "id": 2000000007},
                {"titles": {"raw": "Oculus Rift model without offers"}, "id": 2005},
                {"titles": {"raw": "Oculus Rift model without information about offers"}, "id": 2006},
                {"titles": {"raw": "Oculus Rift cluster with information only globally"}, "id": 2000000010},
            ]
        }

        basesearch_order_with_delimiter = [2001, 2003, 2004, 2008, 2009, 2002, 2007, 2005, 2006, 2010]
        trace_with_delimiter = {
            'logicTrace': [Contains('Apply doc : {}'.format(ts)) for ts in basesearch_order_with_delimiter]
        }

        # для теста нам понадобятся в выдаче кластера не в продаже
        cgi = '&debug=da&rearr-factors=market_not_filter_out_vclusters_not_onstock=1&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'

        # галка "сначала предложения из моего региона" включена - модели разделяются чертой
        response = self.report.request_json('place=prime&rids=213&text=Oculus+Rift&local-offers-first=1' + cgi)
        self.assertFragmentIn(response, with_delimiter, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(response, trace_with_delimiter, preserve_order=True)

        # галка "сначала предложения из моего региона" выключена - все модели замешаны в одну кучу, черты нет
        response = self.report.request_json('place=prime&rids=213&text=Oculus+Rift&local-offers-first=0' + cgi)
        self.assertFragmentIn(response, without_delimiter, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(response, trace_without_delimiter, preserve_order=True)

        # галка "показывать предложения из других регионов" включена (regset=1)
        # галка "сначала предложения из моего региона" включена (local-offers-first=1)
        # модели сортируются в соответствии с правилами доставки
        response = self.report.request_json('place=prime&rids=213&text=Oculus+Rift&local-offers-first=1&regset=1' + cgi)
        self.assertFragmentIn(response, with_delimiter, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(response, trace_with_delimiter, preserve_order=True)

        # галка "показывать предложения из других регионов" включена (regset=1)
        # галка "сначала предложения из моего региона" выключена (local-offers-first=0)
        # модели сортируются в соответствии - черты нет
        response = self.report.request_json('place=prime&rids=213&text=Oculus+Rift&local-offers-first=0&regset=1' + cgi)
        self.assertFragmentIn(response, without_delimiter, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(response, trace_without_delimiter, preserve_order=True)

    @classmethod
    def prepare_delivery_program(cls):
        cls.index.shops += [
            Shop(fesh=757575, priority_region=213),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=757570,
                fesh=757575,
                carriers=[5],
                regional_options=[RegionalDelivery(rid=213)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=757571,
                fesh=757575,
                carriers=[6],
                regional_options=[RegionalDelivery(rid=213)],
                delivery_program=DeliveryBucket.FF_LIGHT_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=757574,
                fesh=757575,
                carriers=[9],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(day_from=1, day_to=2)])],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(fesh=757575, hyperid=757570, delivery_buckets=[757570]),
            Offer(fesh=757575, hyperid=757571, delivery_buckets=[757571]),
            Offer(fesh=757575, hyperid=757572, delivery_buckets=[757572]),
            Offer(fesh=757575, hyperid=757573, delivery_buckets=[757573]),
            Offer(fesh=757575, hyperid=757574, delivery_buckets=[757574], waremd5='DuE098x_rinQLZn3KKrELw'),
        ]

    def _check_delivery_program(self, hyperid, program):
        response = self.report.request_json(
            'place=prime&rids=213&hyperid={}&raw_delivery_options=1&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'.format(
                hyperid
            )
        )
        self.assertFragmentIn(
            response,
            {
                "partnerType": program,
                "shipmentDay": NotEmpty()
                if program == DeliveryBucket.MARKET_DELIVERY_PROGRAM
                else NoKey("shipmentDay"),
            },
        )
        return response

    def test_delivery_program_regular(self):
        self._check_delivery_program(757570, DeliveryBucket.REGULAR_PROGRAM)

    def test_delivery_program_ff_light(self):
        self._check_delivery_program(757571, DeliveryBucket.FF_LIGHT_PROGRAM)

    def _check_market_delivery_dates(self, inlet_shipment_day, day_from, day_to):
        response = self.report.request_json(
            'place=offerinfo&rids=213&offerid=DuE098x_rinQLZn3KKrELw&regset=1&show-urls=decrypted&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1&inlet-shipment-day='
            + str(inlet_shipment_day)
        )
        self.assertFragmentIn(
            response,
            {
                "partnerType": DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                "dayFrom": day_from,
                "dayTo": day_to,
                "orderBefore": "5",
                "serviceId": "9",
                "shipmentDay": inlet_shipment_day,
            },
        )

    def test_delivery_program_market_delivery(self):
        response = self._check_delivery_program(757574, DeliveryBucket.MARKET_DELIVERY_PROGRAM)
        """Проверяется, что shipmentDay, rawDayTo, сроки доставки рассчитываются на основе календаря calendar_id=9"""
        self.assertFragmentIn(
            response,
            {
                "partnerType": DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                "dayFrom": 6,
                "dayTo": 7,  # 4(shipmentDay) + 1(sorting) + 2(rawDayTo)
                "orderBefore": "5",
                "serviceId": "9",
                "shipmentDay": 4,  # by shop+sorting_center common calendar
                "rawDayTo": "2",  # days after 4(shipment) [and 1(sorting)] days: 7 - (4 + 1)
            },
        )

        """Проверяется, что &inlet-shipment-day изменяет дату отгрузки из магазина &inlet-shipment-day=5 увеличивает сроки доставки на 1 день"""
        """    При &inlet-shipment-day=3 значение dayTo = 3(shipmentDay) + 1(sorting) + 2(rawDayTo)"""
        self._check_market_delivery_dates(3, 5, 6)
        """    При &inlet-shipment-day=5 значение dayTo = 5(shipmentDay) + 1(sorting) + 2(rawDayTo)"""
        self._check_market_delivery_dates(5, 7, 8)

    # Индексы бакетов DeliveryBucketMardoIndex LowPrice(LP), HighPrice(HP), LowTime(LT), HighTime(HT), Rating(R), HighShopDeliveryPrice(HSDP), LowShopDeliveryPrice(LSDP)
    DBMI_LP_LT = 757590
    DBMI_LP_HT = 757591
    DBMI_HP_LT = 757592
    DBMI_HP_HT = 757593
    DBMI_HP_R1 = 757594
    DBMI_HP_R2 = 757595
    DBMI_LP_R3 = 757596
    DBMI_LP_HSDP = 757597
    DBMI_HP_LSDP = 757598

    # Индексы бакетов DeliveryBucketRegularIndex ZeroPrice(ZP), LowPrice(LP), HighPrice(HP), ZeroTime(ZT), LowTime(LT), HighTime(HT)
    DBRI_ZP_ZT = 757580
    DBRI_LP_ZT = 757581
    DBRI_LP_HT = 757583
    DBRI_HP_ZT = 757584

    def test_empty_request(self):
        response = self.report.request_json(
            'place=prime&rids=213&&debug-all-courier-options=1&rearr-factors=offer_regions_literal=1;market_metadoc_search=no',
            strict=False,
        )
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST"}})

    @classmethod
    def prepare_best_market_delivery(cls):
        cls.index.shops += [
            Shop(fesh=757576, priority_region=213),
        ]

        cls.dynamic.lms += [
            # чем больше значение рейтинга, тем больший приоритет имеет служба
            DynamicDeliveryServiceInfo(21, "carrier_21", 3),
            DynamicDeliveryServiceInfo(22, "carrier_22", 2),
            DynamicDeliveryServiceInfo(23, "carrier_23", 1),
        ]

        def create_bucket(price, bucket_id, carrier_id, shop_delivery_price=None):
            return DeliveryBucket(
                bucket_id=bucket_id,
                fesh=757576,
                carriers=[carrier_id],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(
                                price=price,
                                day_from=0,
                                day_to=15,
                                order_before=6,
                                shop_delivery_price=shop_delivery_price,
                            ),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )

        cls.index.delivery_buckets += [
            # Магазинная доставка с самой низкой ценой. Будет дефолтной всегда
            DeliveryBucket(
                bucket_id=cls.DBRI_ZP_ZT,
                fesh=757576,
                carriers=[5],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=0, day_from=0, day_to=0, order_before=6),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            # Магазинная доставка быстрее МарДо. Будет дефолтной, если цена одинаковая
            DeliveryBucket(
                bucket_id=cls.DBRI_LP_ZT,
                fesh=757576,
                carriers=[6],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=1, day_from=0, day_to=0, order_before=6),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            # Магазинная доставка медленее МарДо
            DeliveryBucket(
                bucket_id=cls.DBRI_LP_HT,
                fesh=757576,
                carriers=[7],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=1, day_from=0, day_to=20, order_before=6),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            # Магазинная доставка с самой высокой ценой
            DeliveryBucket(
                bucket_id=cls.DBRI_HP_ZT,
                fesh=757576,
                carriers=[8],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100500, day_from=0, day_to=0, order_before=6),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            # Доставка МарДо
            # Низкая цена, низкий срок доставки (среди МарДо)
            DeliveryBucket(
                bucket_id=cls.DBMI_LP_LT,
                fesh=757576,
                carriers=[9],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=1, day_from=0, day_to=10, order_before=6),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Высокая цена, низкий срок доставки (среди МарДо)
            DeliveryBucket(
                bucket_id=cls.DBMI_HP_LT,
                fesh=757576,
                carriers=[10],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100, day_from=0, day_to=10, order_before=6),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Низкая цена, большой срок доставки (среди МарДо)
            DeliveryBucket(
                bucket_id=cls.DBMI_LP_HT,
                fesh=757576,
                carriers=[11],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=1, day_from=0, day_to=15, order_before=6),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Высокая цена, большой срок доставки (среди МарДо)
            DeliveryBucket(
                bucket_id=cls.DBMI_HP_HT,
                fesh=757576,
                carriers=[12],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100, day_from=0, day_to=15, order_before=6),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Бакеты для проверки рейтинга
            create_bucket(100, cls.DBMI_HP_R1, 21),
            create_bucket(100, cls.DBMI_HP_R2, 22),
            create_bucket(1, cls.DBMI_LP_R3, 23),
            # Бакеты для проверки цены доставки для магазина
            create_bucket(100, cls.DBMI_LP_HSDP, 31, 200),
            create_bucket(100, cls.DBMI_HP_LSDP, 32, 100),
        ]

        cls.index.offers += [
            # Оферы для проверки правильного выбора МарДо бакета
            Offer(
                fesh=757576, hyperid=757580, delivery_buckets=[cls.DBMI_LP_LT, cls.DBMI_HP_LT]
            ),  # Будет выбран бакет с низкой ценой
            Offer(
                fesh=757576, hyperid=757581, delivery_buckets=[cls.DBMI_LP_LT, cls.DBMI_HP_HT]
            ),  # Будет выбран бакет с низкой ценой
            Offer(
                fesh=757576, hyperid=757582, delivery_buckets=[cls.DBMI_LP_LT, cls.DBMI_LP_HT]
            ),  # Будет выбран бакет с более быстрой доставкой
            Offer(
                fesh=757576, hyperid=757583, delivery_buckets=[cls.DBMI_HP_LT, cls.DBMI_HP_HT]
            ),  # Будет выбран бакет с более быстрой доставкой
            Offer(
                fesh=757576, hyperid=757590, delivery_buckets=[cls.DBMI_HP_R1, cls.DBMI_HP_R2]
            ),  # Будет выбран бакет с более приоритетной службой
            Offer(
                fesh=757576, hyperid=757591, delivery_buckets=[cls.DBMI_LP_R3, cls.DBMI_HP_R2]
            ),  # Будет выбран бакет с низкой ценой, рейтинг при этом не учитывается
            Offer(
                fesh=757576, hyperid=757592, delivery_buckets=[cls.DBMI_LP_HSDP, cls.DBMI_HP_LSDP]
            ),  # Будет выбран бакет с низкой ценой для магазина
            # Оферы для проверки выбора дефолтного бакета МарДо или магазинный
            Offer(
                fesh=757576, hyperid=757584, delivery_buckets=[cls.DBRI_ZP_ZT, cls.DBMI_LP_LT, cls.DBMI_HP_LT]
            ),  # Будет выбран магазинный по цене
            Offer(
                fesh=757576, hyperid=757585, delivery_buckets=[cls.DBRI_LP_ZT, cls.DBMI_LP_LT, cls.DBMI_HP_LT]
            ),  # Будет выбран магазинный по срокам
            Offer(
                fesh=757576, hyperid=757586, delivery_buckets=[cls.DBRI_LP_HT, cls.DBMI_LP_LT, cls.DBMI_HP_LT]
            ),  # Будет выбран МарДо по срокам
            Offer(
                fesh=757576, hyperid=757587, delivery_buckets=[cls.DBRI_HP_ZT, cls.DBMI_LP_LT, cls.DBMI_HP_LT]
            ),  # Будет выбран МарДо по цене
        ]

    def __check_mardo_buckets(self, hyperid, buckets, default_bucket_id=None):
        def create_bucket_from_index(bucket_id, default_bucket_id):
            if default_bucket_id is None:
                default_bucket_id = buckets[0]

            for bucket in self.index.delivery_buckets:
                if str(bucket.bucket_id) == str(bucket_id):
                    regional_option = bucket.regional_options[0]
                    delivery_option = regional_option.delivery_options[0]
                    return {
                        'price': {'value': str(delivery_option.price)},
                        'dayFrom': delivery_option.day_from,
                        'dayTo': delivery_option.day_to,
                        'isDefault': bucket_id == default_bucket_id,
                        "serviceId": str(bucket.carriers[0]),
                        'partnerType': bucket.delivery_program,
                    }

        response = self.report.request_json(
            'place=prime&rids=213&hyperid={}&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'.format(
                hyperid
            )
        )
        self.assertFragmentIn(
            response,
            {"options": [create_bucket_from_index(bucket_id, default_bucket_id) for bucket_id in buckets]},
            allow_different_len=False,
        )

    def test_mardo_choose_price(self):
        # Что проверяем: выбор МарДо бакета сделан на основе цены
        self.__check_mardo_buckets(757580, [self.DBMI_LP_LT])
        self.__check_mardo_buckets(757581, [self.DBMI_LP_LT])

    def test_mardo_choose_time(self):
        # Что проверяем: выбор МарДо бакета сделан на основе сроков доставки, т.к. цена одинаковая
        self.__check_mardo_buckets(757582, [self.DBMI_LP_LT])
        self.__check_mardo_buckets(757583, [self.DBMI_HP_LT])

    def test_mardo_default_option(self):
        # Что проверяем: МарДо опция отображается, если она дефолтная и не дефолтная. И выбирается только одна опция МарДо
        self.__check_mardo_buckets(757584, [self.DBRI_ZP_ZT, self.DBMI_LP_LT], self.DBRI_ZP_ZT)
        self.__check_mardo_buckets(757585, [self.DBRI_LP_ZT, self.DBMI_LP_LT], self.DBRI_LP_ZT)
        self.__check_mardo_buckets(757586, [self.DBRI_LP_HT, self.DBMI_LP_LT], self.DBMI_LP_LT)
        self.__check_mardo_buckets(757587, [self.DBRI_HP_ZT, self.DBMI_LP_LT], self.DBMI_LP_LT)
        self.__check_mardo_buckets(757590, [self.DBMI_HP_R1])
        self.__check_mardo_buckets(757591, [self.DBMI_LP_R3])
        self.__check_mardo_buckets(757592, [self.DBMI_HP_LSDP])

        """Проверяется, что с &debug-all-courier-options=1 выводятся все МарДо опции"""
        response = self.report.request_json(
            'place=prime&rids=213&hyperid=757591&debug-all-courier-options=1&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'
        )
        self.assertFragmentIn(response, {"options": [{"serviceId": "22"}, {"serviceId": "23"}]})

    @classmethod
    def prepare_payment_methods(cls):
        cls.index.shops += [
            Shop(fesh=299999, priority_region=213, regions=[2], cpa=Shop.CPA_REAL),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=299999,
                fesh=299999,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100500, day_from=0, day_to=0, order_before=6),
                        ],
                    ),
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=100500, day_from=0, day_to=0, order_before=6),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=299999,
                hyperid=299,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[299999],
                waremd5="EpnWVxDQxj4wg7vVI1ElnA",
            ),
        ]

    def _test_payment_methods(
        self, hyperid, homeRegion_cpaPartner_paymentMethods, nonHomeRegion_cpaPartner_paymentMethods=None
    ):
        # === В свой регион ===

        for place in ['prime', 'productoffers']:
            for aux in ['', '&client=checkout&co-from=shopadmin-stub']:
                response = self.report.request_json(
                    'place={}&rids=213&hyperid={}{}&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'.format(
                        place, hyperid, aux
                    )
                )
                self.assertFragmentIn(response, {"paymentMethods": homeRegion_cpaPartner_paymentMethods})

        """Проверим, что у не ПИ и не FF-light магазина"""
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=213&hyperid=299&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'.format(
                    place
                )
            )
            self.assertFragmentNotIn(response, {"paymentMethods": []})

        """
        Для ПИ заглушки, place=offersinfo считаем, что магазин якобы всегда работает через ПИ, независимо от настроек в shops.dat
        MARKETOUT-15399
        """
        response = self.report.request_json(
            'place=offerinfo&rids=213&offerid=EpnWVxDQxj4wg7vVI1ElnA&show-urls=all&regset=1&client=checkout&co-from=shopadmin-stub&rearr-factors=offer_regions_literal=1'
        )
        self.assertFragmentIn(response, {"paymentMethods": homeRegion_cpaPartner_paymentMethods})

        # === В чужой регион ===

        for place in ['prime', 'productoffers']:
            for aux in ['', '&client=checkout&co-from=shopadmin-stub']:
                response = self.report.request_json(
                    'place={}&rids=2&hyperid={}&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'.format(
                        place, hyperid
                    )
                )
            if nonHomeRegion_cpaPartner_paymentMethods:
                self.assertFragmentIn(response, {"paymentMethods": nonHomeRegion_cpaPartner_paymentMethods})
            else:
                self.assertFragmentNotIn(response, {"paymentMethods": []})

        """
        Для ПИ заглушки, place=offersinfo считаем, что магазин якобы всегда работает через ПИ, независимо от настроек в shops.dat
        MARKETOUT-15399
        """
        response = self.report.request_json(
            'place=offerinfo&rids=2&offerid=EpnWVxDQxj4wg7vVI1ElnA&show-urls=all&regset=1&client=checkout&co-from=shopadmin-stub&rearr-factors=offer_regions_literal=1;offer_regions_literal_reduce_exp=1'  # noqa
        )
        if nonHomeRegion_cpaPartner_paymentMethods:
            self.assertFragmentIn(response, {"paymentMethods": nonHomeRegion_cpaPartner_paymentMethods})
        else:
            self.assertFragmentNotIn(response, {"paymentMethods": []})

    @classmethod
    def prepare_payment_methods__without_prepay(cls):
        cls.index.shops += [
            Shop(fesh=300000, priority_region=213, regions=[2], is_cpa_partner=True),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=300000,
                fesh=300000,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100500, day_from=0, day_to=0, order_before=6),
                        ],
                    ),
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=100500, day_from=0, day_to=0, order_before=6),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=300000, hyperid=300, delivery_buckets=[300000]),
        ]

    def test_payment_methods__without_prepay(self):
        """
        Проверим, что для ПИ магазинов выводятся способы доставки. А для ПИ не выводятся.
        (без предоплаты)
        MARKETOUT-14885
        """

        self._test_payment_methods(300, ["CASH_ON_DELIVERY"], [])


if __name__ == '__main__':
    main()

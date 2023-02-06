#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BookingAvailability,
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
)

from core.testcase import TestCase, main
from core.matcher import NoKey
from core.cpc import Cpc
from core.click_context import ClickContext
from unittest import skip

SEARCH_FILTERS = (
    '&mcpricefrom=100&mcpriceto=800&&offer-shipping=store&manufacturer_warranty=1&qrfrom=4&free_delivery=1&'
    'home_region_filter=225&delivery_interval=2&fesh=18001,18002,18003,18004,18006,18007&show-book-now-only=1&filter-discount-only=1'
)
SEARCH_GL_FILTER = '&glfilter=1203:1'
RED, GREEN, BLUE = 1, 2, 3

FREE_OPTION = DeliveryOption(price=0, day_from=1, day_to=1)

BOOKING1 = BookingAvailability(outlet_id=18201, region_id=213, amount=15)
BOOKING2 = BookingAvailability(outlet_id=18202, region_id=213, amount=15)
BOOKING3 = BookingAvailability(outlet_id=18203, region_id=213, amount=15)
BOOKING4 = BookingAvailability(outlet_id=18204, region_id=213, amount=15)
BOOKING5 = BookingAvailability(outlet_id=18205, region_id=213, amount=15)
BOOKING6 = BookingAvailability(outlet_id=18206, region_id=213, amount=15)
BOOKING11 = BookingAvailability(outlet_id=18211, region_id=54, amount=15)
BOOKING12 = BookingAvailability(outlet_id=18212, region_id=54, amount=15)
BOOKING13 = BookingAvailability(outlet_id=18213, region_id=54, amount=15)
BOOKING14 = BookingAvailability(outlet_id=18214, region_id=54, amount=15)
BOOKING15 = BookingAvailability(outlet_id=18215, region_id=54, amount=15)
BOOKING16 = BookingAvailability(outlet_id=18216, region_id=54, amount=15)


class T(TestCase):
    @classmethod
    def prepare(cls):
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        """
        Создаем категорию с типом CPA_WITH_CPC_PESSIMIZATION, магазины
        и аутлеты для них, GL-параметр второго рода
        """
        cls.settings.is_archive_new_format = True

        cls.index.regiontree += [Region(rid=54, name='Екатеринбург'), Region(rid=213, name='Москва')]

        cls.index.cpa_categories += [
            CpaCategory(hid=18100, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.shops += [
            Shop(
                fesh=18001,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5001],
            ),
            Shop(
                fesh=18002,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5002],
            ),
            Shop(
                fesh=18003,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5003],
            ),
            Shop(
                fesh=18004,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                cpa=Shop.CPA_REAL,
                home_region=134,
                pickup_buckets=[5004],
            ),
            Shop(
                fesh=18005,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5005],
            ),
            Shop(
                fesh=18006,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                pickup_buckets=[5006],
            ),
            Shop(
                fesh=18007,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5007],
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=18201, fesh=18001, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18202, fesh=18002, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18203, fesh=18003, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18204, fesh=18004, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18205, fesh=18005, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18206, fesh=18006, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18211, fesh=18001, region=54, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18212, fesh=18002, region=54, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18213, fesh=18003, region=54, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18214, fesh=18004, region=54, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18215, fesh=18005, region=54, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18216, fesh=18006, region=54, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18217, fesh=18001, region=54, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=18218, fesh=18001, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=18219, fesh=18007, region=213, point_type=Outlet.FOR_STORE),
            Outlet(point_id=18220, fesh=18007, region=54, point_type=Outlet.FOR_STORE),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=18001,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=18201),
                    PickupOption(outlet_id=18211),
                    PickupOption(outlet_id=18217),
                    PickupOption(outlet_id=18218),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=18002,
                carriers=[99],
                options=[PickupOption(outlet_id=18202), PickupOption(outlet_id=18212)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=18003,
                carriers=[99],
                options=[PickupOption(outlet_id=18203), PickupOption(outlet_id=18213)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=18004,
                carriers=[99],
                options=[PickupOption(outlet_id=18204), PickupOption(outlet_id=18214)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=18005,
                carriers=[99],
                options=[PickupOption(outlet_id=18205), PickupOption(outlet_id=18215)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5006,
                fesh=18006,
                carriers=[99],
                options=[PickupOption(outlet_id=18206), PickupOption(outlet_id=18216)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5007,
                fesh=18007,
                carriers=[99],
                options=[PickupOption(outlet_id=18219), PickupOption(outlet_id=18220)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=310,
                fesh=18001,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=0, day_from=1, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=311,
                fesh=18001,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=0, day_from=2, day_to=5, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=312,
                fesh=18001,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=100, day_from=1, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=313,
                fesh=18001,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=100, day_from=2, day_to=5, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=314,
                fesh=18007,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=0, day_from=1, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=315,
                fesh=18001,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100, day_from=1, day_to=2, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=320,
                fesh=18002,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=0, day_from=1, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=330,
                fesh=18003,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=0, day_from=1, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=340,
                fesh=18004,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=0, day_from=1, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=350,
                fesh=18005,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=0, day_from=1, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=360,
                fesh=18006,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=54,
                        options=[
                            DeliveryOption(price=0, day_from=1, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=1203,
                hid=18100,
                gltype=GLType.ENUM,
                values=[RED, GREEN, BLUE],
                unit_name="Color",
                cluster_filter=True,
            ),
        ]

    @classmethod
    def prepare_filters_ignorance(cls):
        """
        Создаем два набора офферов. В первом наборе для модели 18101
        присуствует оффер, подходящий под все фильтры из таблицы
        https://wiki.yandex-team.ru/users/msheglov/Kontekstnost-vydachi/#filtrytretegotipa
        и набор офферов, каждый из которых не подходит ровно под один из фильтров

        Во втором наборе для модели 18102 тоже есть набор офферов с CPA_NO, которые не подходят под
        один из фильтров, но нет оффера, подходящего под все фильтры
        """

        cls.index.models += [
            Model(hyperid=18101, hid=18100),
            Model(hyperid=18102, hid=18100),
        ]

        cls.index.offers += [
            Offer(
                hyperid=18101,
                fesh=18001,
                title='ideal-offer',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18101,
                fesh=18002,
                title='low-rating',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[320],
                booking_availabilities=[BOOKING2, BOOKING12],
            ),
            Offer(
                hyperid=18101,
                fesh=18001,
                title='too-cheap',
                price=10,
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18101,
                fesh=18001,
                title='too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18101,
                fesh=18001,
                title='no-discount',
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18101,
                fesh=18001,
                title='no-warranty',
                manufacturer_warranty=False,
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18101,
                fesh=18003,
                title='no-prepay',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[330],
                booking_availabilities=[BOOKING3, BOOKING13],
            ),
            Offer(
                hyperid=18101,
                fesh=18001,
                title='non-free-delivery',
                price_old=150,
                cpa=Offer.CPA_REAL,
                booking_availabilities=[BOOKING1, BOOKING11],
                delivery_buckets=[312],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=1),
                ],
            ),
            Offer(
                hyperid=18101,
                fesh=18004,
                title='from-another-country',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[340],
                booking_availabilities=[BOOKING4, BOOKING14],
            ),
            Offer(
                hyperid=18101,
                fesh=18001,
                title='slow-delivery',
                booking_availabilities=[BOOKING1, BOOKING11],
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[311],
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=5),
                ],
            ),
            Offer(
                hyperid=18101,
                fesh=18005,
                title='filtered-shop',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
            ),
            Offer(
                hyperid=18101,
                fesh=18001,
                title='without-book-now',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[310],
                delivery_options=[FREE_OPTION],
            ),
            Offer(
                hyperid=18101,
                fesh=18006,
                title='no-cpa',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[360],
                booking_availabilities=[BOOKING6, BOOKING16],
            ),
            Offer(
                hyperid=18102,
                fesh=18002,
                title='no-cpa-low-rating',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[320],
                booking_availabilities=[BOOKING2, BOOKING12],
            ),
            Offer(
                hyperid=18102,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18102,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18102,
                fesh=18001,
                title='no-cpa-no-discount',
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18102,
                fesh=18001,
                title='no-cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_NO,
                manufacturer_warranty=False,
                delivery_buckets=[310],
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
            ),
            Offer(
                hyperid=18102,
                fesh=18003,
                title='no-cpa-no-prepay',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[330],
                booking_availabilities=[BOOKING3, BOOKING13],
            ),
            Offer(
                hyperid=18102,
                fesh=18001,
                title='no-cpa-non-free-delivery',
                price_old=150,
                cpa=Offer.CPA_NO,
                booking_availabilities=[BOOKING1, BOOKING11],
                delivery_buckets=[312],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=1),
                ],
            ),
            Offer(
                hyperid=18102,
                fesh=18004,
                title='no-cpa-from-another-country',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[340],
                booking_availabilities=[BOOKING4, BOOKING14],
            ),
            Offer(
                hyperid=18102,
                fesh=18001,
                title='no-cpa-slow-delivery',
                booking_availabilities=[BOOKING1, BOOKING11],
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_buckets=[311],
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=5),
                ],
            ),
            Offer(
                hyperid=18102,
                fesh=18005,
                title='no-cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
            ),
            Offer(
                hyperid=18102,
                fesh=18001,
                title='no-cpa-without-book-now',
                delivery_buckets=[310],
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
            ),
        ]

    def test_offers_page_without_filters(self):
        """
        Что тестируем: запрос за офферами на странице offers без явного указания фильтров
        возвращает все офферы модели, доступные по cpa и cpc

        Делаем запрос за офферами на странице offers без фильтров для модели 18101
        Ожидаем, что возвращается 13 офферов
        """
        response = self.report.request_json('place=productoffers&hyperid=18101&hid=18100&numdoc=20&rids=213')
        self.assertFragmentIn(response, {"search": {"total": 13}})
        self.assertEqual(response.count({"entity": "offer"}), 13)

    def test_offers_page_with_filters(self):
        """
        Что тестируем: запрос за офферами на странице offers с явным указанием фильтров,
        перечисленных на странице
        https://wiki.yandex-team.ru/users/msheglov/Kontekstnost-vydachi/#filtrytretegotipa
        кроме "Заказать на маркете" (он должен проставиться автоматически) возвращает
        офферы с учетом фильтров

        Делаем запрос за офферами на странице offers с фильтрами для модели 18101
        Ожидаем, что возвращается один "идеальный" оффер
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=productoffers&hyperid=18101&hid=18100&rids=213' + SEARCH_FILTERS + unified_off_flags
        )
        self.assertFragmentIn(response, {"search": {"total": 3}})
        self.assertEqual(response.count({"entity": "offer"}), 3)
        self.assertFragmentIn(response, {"titles": {"raw": "ideal-offer"}})

    @skip('MARKETOUT-43348  фильтр cpa=real не должен игнорироваться')
    def test_default_offer_cpa_filter_relax(self):
        """
        Что тестируем: запрос за дефолтным оффером в регионах 213 и 54
        с явным указанием &cpa=real и &relax-filters=1 проигнорирует фильтр cpa=real
        и вернет cpc оффер в качестве ДО
        При этом флаг "Заказать на маркете" сбрасывается

        Однако &relax-filters=1 не действует если запрос идет с rgb=blue

        Делаем запрос за дефолтным оффером с фильтрами для модели 18102
        и флагами &cpa=real &relax-filters=1
        Ожидаем, что возвращается оффер, хотя cpa-офферов для модели нет

        """
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18102&hid=18100&rids={}&relax-filters=1&cpa=real'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"entity": "offer", "cpa": NoKey("cpa")})

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18102&hid=18100&rids={}&relax-filters=1&cpa=real&rgb=blue'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentNotIn(response, {"entity": "offer"})

            #  на приложении если оно не пришлет rgb=blue тоже покажется cpc оффер
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18102&hid=18100&rids={}&relax-filters=1&cpa=real&client=IOS'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"entity": "offer", "cpa": NoKey("cpa")})

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18102&hid=18100&rids={}&relax-filters=1&rgb=blue&client=IOS'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentNotIn(response, {"entity": "offer"})

    def test_offers_page_with_rec_businesses(self):
        """
        Что тестируем: запрос за офферами на странице offers с указанием рекмагов в параметре cpc

        Делаем запрос за офферами на странице offers для модели 18101
        Проставляем в рекмаги в cpc несколько бизнесов.
        Ожидаем, что вернутся только их оффера
        """
        model_cpc = Cpc.create_for_model(
            model_id=18101,
            vendor_click_price=500,
            vendor_bid=1200,
            pp=84,
            rec_businesses=[18001, 18003],
        )
        response = self.report.request_json(
            'place=productoffers&hyperid=18101&hid=18100&rids=213&debug=da&rearr-factors=market_report_click_context_enabled=0&cpc={}'.format(
                str(model_cpc)
            )
        )
        for result in response["search"]["results"]:
            business_id = result["shop"]["business_id"]
            self.assertTrue(business_id in {18001, 18003})

    def test_offers_page_with_rec_businesses_from_cc(self):
        """
        Что тестируем: запрос за офферами на странице offers с указанием рекмагов в параметре cc

        Делаем запрос за офферами на странице offers для модели 18101
        Проставляем в рекмаги в cc несколько бизнесов.
        Ожидаем, что вернутся только их оффера
        """
        click_context = ClickContext(
            pp=84,
            rec_businesses=[18001, 18003],
        )
        response = self.report.request_json(
            'place=productoffers&hyperid=18101&hid=18100&rids=213&debug=da&rearr-factors=market_report_click_context_enabled=1&cc={}'.format(
                str(click_context)
            )
        )
        for result in response["search"]["results"]:
            business_id = result["shop"]["business_id"]
            self.assertTrue(business_id in {18001, 18003})

    @classmethod
    def prepare_default_offer_quality_rating_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190)
        Все офферы с CPA_NO, кроме офферов cpa-slow-non-free-delivery
        и cpa-no-store
        Оффера, подходящего под все фильтры, нет
        """

        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18105,
                fesh=18001,
                title='no-cpa-green',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18105,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18105,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18105,
                fesh=18001,
                title='no-cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_NO,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18105,
                fesh=18005,
                title='no-cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18105,
                fesh=18004,
                title='no-cpa-from-another-country',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[340],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18105,
                fesh=18001,
                title='no-cpa-without-book-now',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18105,
                fesh=18001,
                title='no-cpa-no-discount',
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18105,
                fesh=18001,
                title='cpa-no-store',
                price_old=150,
                cpa=Offer.CPA_REAL,
                store=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18105,
                fesh=18001,
                title='cpa-slow-non-free-delivery',
                booking_availabilities=[BOOKING1, BOOKING11],
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[313, 315],
                glparams=[gl_red],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=5),
                ],
            ),
            Offer(
                hyperid=18105,
                fesh=18002,
                title='no-cpa-low-rating',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[320],
                booking_availabilities=[BOOKING2, BOOKING12],
                glparams=[gl_red],
            ),
        ]

    def test_default_offer_quality_rating_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        с низким рейтингом офферам, у которых опции  не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-slow-non-free-delivery

        Делаем запрос запрос за ДО с фильтрами для модели 18105 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - "no-cpa-low-rating"
        Делаем запрос запрос за ДО с фильтрами для модели 18105 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер, найденным первым при "ослаблении"
        фильтров - "cpa-slow-non-free-delivery"
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18105&hid=18100&rids={}&relax-filters=1'.format(rids)
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
                + unified_off_flags
            )
            self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-low-rating"}, "benefit": {"type": "default"}})

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18105&hid=18100&rids={}&relax-filters=1&cpa=real'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
                + unified_off_flags
            )
            self.assertFragmentIn(
                response, {"titles": {"raw": "cpa-slow-non-free-delivery"}, "benefit": {"type": "default"}}
            )

    @classmethod
    def prepare_default_offer_delivery_options_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled и low_rating
        Все офферы с CPA_NO, кроме офферов cpa-no-store и
        cpa-no-discount
        Оффера, подходящего под все фильтры, нет
        """
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18106,
                fesh=18001,
                title='no-cpa-green',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18106,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18106,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18106,
                fesh=18001,
                title='no-cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_NO,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18106,
                fesh=18005,
                title='no-cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18106,
                fesh=18004,
                title='no-cpa-from-another-country',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[340],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18106,
                fesh=18001,
                title='no-cpa-without-book-now',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18106,
                fesh=18001,
                title='cpa-no-discount',
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18106,
                fesh=18001,
                title='cpa-no-store',
                price_old=150,
                cpa=Offer.CPA_REAL,
                store=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18106,
                fesh=18001,
                title='no-cpa-slow-non-free-delivery',
                booking_availabilities=[BOOKING1, BOOKING11],
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_buckets=[313],
                glparams=[gl_red],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=5),
                ],
            ),
        ]

    def test_default_offer_delivery_options_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        с дорогой доставкой офферам, у которых опции  не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-no-store

        Делаем запрос запрос за ДО с фильтрами для модели 18106 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - "no-cpa-slow-non-free-delivery"
        Делаем запрос запрос за ДО с фильтрами для модели 18106 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер, найденным первым при "ослаблении"
        фильтров - "cpa-no-store"
        """
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18106&hid=18100&rids={}&relax-filters=1&local-offers-first=0'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(
                response, {"titles": {"raw": "no-cpa-slow-non-free-delivery"}, "benefit": {"type": "default"}}
            )

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18106&hid=18100&rids={}&relax-filters=1&cpa=real&local-offers-first=0'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "cpa-no-store"}, "benefit": {"type": "default"}})

    @classmethod
    def prepare_default_offer_offer_shipping_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled, low_rating и delivery_options
        Все офферы с CPA_NO, кроме офферов cpa-without-book-now и
        cpa-no-discount
        Оффера, подходящего под все фильтры, нет
        """
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18107,
                fesh=18001,
                title='no-cpa-green',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18107,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18107,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18107,
                fesh=18001,
                title='no-cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_NO,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18107,
                fesh=18005,
                title='no-cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18107,
                fesh=18004,
                title='no-cpa-from-another-country',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[340],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18107,
                fesh=18001,
                title='cpa-without-book-now',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18107,
                fesh=18001,
                title='cpa-no-discount',
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18107,
                fesh=18001,
                title='no-cpa-no-store',
                price_old=150,
                cpa=Offer.CPA_NO,
                store=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
        ]

    def test_default_offer_offer_shipping_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        без доступности в каменных магазинах офферам, у которых опции  не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-no-discount

        Делаем запрос запрос за ДО с фильтрами для модели 18107 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - no-cpa-no-store"
        Делаем запрос запрос за ДО с фильтрами для модели 18107 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер, найденным первым при "ослаблении"
        фильтров - "cpa-without-book-now"
        """
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18107&hid=18100&rids={}&relax-filters=1&local-offers-first=0'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-store"}, "benefit": {"type": "default"}})

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18107&hid=18100&rids={}&relax-filters=1&cpa=real&local-offers-first=0'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "cpa-without-book-now"}, "benefit": {"type": "default"}})

    @classmethod
    def prepare_default_offer_discount_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled, low_rating, delivery_options и offer-shipping
        Все офферы с CPA_NO, кроме офферов cpa-without-book-now и
        cpa-from-another-country
        Оффера, подходящего под все фильтры, нет
        """
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18108,
                fesh=18001,
                title='no-cpa-green',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18108,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18108,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18108,
                fesh=18001,
                title='no-cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_NO,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18108,
                fesh=18005,
                title='no-cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18108,
                fesh=18004,
                title='cpa-from-another-country',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[340],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18108,
                fesh=18001,
                title='cpa-without-book-now',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18108,
                fesh=18001,
                title='no-cpa-no-discount',
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
        ]

    @classmethod
    def prepare_default_offer_booknow_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled, low_rating, delivery_options, offer-shipping и
        discount
        Все офферы с CPA_NO, кроме офферов cpa-filtered-shop и
        cpa-from-another-country
        Оффера, подходящего под все фильтры, нет
        """
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18109,
                fesh=18001,
                title='no-cpa-green',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18109,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18109,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18109,
                fesh=18001,
                title='no-cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_NO,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18109,
                fesh=18005,
                title='cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18109,
                fesh=18004,
                title='cpa-from-another-country',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[340],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18109,
                fesh=18001,
                title='no-cpa-without-book-now',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                glparams=[gl_red],
            ),
        ]

    def test_default_offer_booknow_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        без booknow офферам, у которых опции не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-from-another-country

        Делаем запрос запрос за ДО с фильтрами для модели 18109 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - no-cpa-without-book-now"
        Делаем запрос запрос за ДО с фильтрами для модели 18109 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер, найденным первым при "ослаблении"
        фильтров - "cpa-from-another-country"
        """
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18109&hid=18100&rids={}&relax-filters=1'.format(rids)
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(
                response, {"titles": {"raw": "no-cpa-without-book-now"}, "benefit": {"type": "default"}}
            )

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18109&hid=18100&rids={}&relax-filters=1&cpa=real'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(
                response, {"titles": {"raw": "cpa-from-another-country"}, "benefit": {"type": "default"}}
            )

    @classmethod
    def prepare_default_offer_home_region_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled, low_rating, delivery_options, offer-shipping,
        discount и booknow
        Все офферы с CPA_NO, кроме офферов cpa-filtered-shop и
        cpa-no-warranty
        Оффера, подходящего под все фильтры, нет
        """
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18110,
                fesh=18001,
                title='no-cpa-green',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18110,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18110,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18110,
                fesh=18001,
                title='cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_REAL,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18110,
                fesh=18005,
                title='cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18110,
                fesh=18004,
                title='no-cpa-from-another-country',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[340],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
        ]

    def test_default_offer_home_region_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        из другой страны офферам, у которых опции не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-filtered-shop

        Делаем запрос запрос за ДО с фильтрами для модели 18110 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - no-cpa-from-another-country"
        Делаем запрос запрос за ДО с фильтрами для модели 18110 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер, найденным первым при "ослаблении"
        фильтров - "cpa-filtered-shop"
        """

        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18110&hid=18100&rids={}&relax-filters=1'.format(rids)
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(
                response, {"titles": {"raw": "no-cpa-from-another-country"}, "benefit": {"type": "default"}}
            )

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18110&hid=18100&rids={}&relax-filters=1&cpa=real'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "cpa-filtered-shop"}, "benefit": {"type": "default"}})

    @classmethod
    def prepare_default_offer_shop_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled, low_rating, delivery_options, offer-shipping,
        discount, booknow и home_region
        Все офферы с CPA_NO, кроме офферов cpa-too-cheap и
        cpa-no-warranty
        Оффера, подходящего под все фильтры, нет
        """
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18111,
                fesh=18001,
                title='no-cpa-green',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18111,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18111,
                fesh=18001,
                title='cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18111,
                fesh=18001,
                title='cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_REAL,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18111,
                fesh=18005,
                title='no-cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[350],
                booking_availabilities=[BOOKING5, BOOKING15],
                glparams=[gl_red],
            ),
        ]

    def test_default_offer_shop_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        из невыбранных магазинов офферам, у которых опции не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-no-warranty

        Делаем запрос запрос за ДО с фильтрами для модели 18111 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - no-cpa-filtered-shop"
        Делаем запрос запрос за ДО с фильтрами для модели 18111 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер, найденным первым при "ослаблении"
        фильтров - "cpa-no-warranty"
        """
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18111&hid=18100&rids={}&relax-filters=1'.format(rids)
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-filtered-shop"}, "benefit": {"type": "default"}})

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18111&hid=18100&rids={}&relax-filters=1&cpa=real'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "cpa-no-warranty"}, "benefit": {"type": "default"}})

    @classmethod
    def prepare_default_offer_warranty_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled, low_rating, delivery_options, offer-shipping,
        discount, booknow, home_region и shop
        Все офферы с CPA_NO, кроме офферов cpa-too-cheap и
        cpa-too-expensive
        Оффера, подходящего под все фильтры, нет
        """
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18112,
                fesh=18001,
                title='no-cpa-green',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18112,
                fesh=18001,
                title='cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18112,
                fesh=18001,
                title='cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18112,
                fesh=18001,
                title='no-cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_NO,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
        ]

    def test_default_offer_warranty_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        без гарантии офферам, у которых опции не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-too-cheap

        Делаем запрос запрос за ДО с фильтрами для модели 18112 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - "no-cpa-no-warranty"
        Делаем запрос запрос за ДО с фильтрами для модели 18112 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер, найденным первым при "ослаблении"
        фильтров - "cpa-too-cheap"
        """
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18112&hid=18100&rids={}&relax-filters=1'.format(rids)
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-warranty"}, "benefit": {"type": "default"}})

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18112&hid=18100&rids={}&relax-filters=1&cpa=real'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "cpa-too-cheap"}, "benefit": {"type": "default"}})

    @classmethod
    def prepare_default_offer_price_from_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled, low_rating, delivery_options, offer-shipping,
        discount, booknow, home_region, shop и warranty
        Все офферы с CPA_NO, кроме офферов cpa-green и
        cpa-too-expensive
        Оффера, подходящего под все фильтры, нет
        """
        _ = BookingAvailability(outlet_id=18216, region_id=54, amount=15)
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18113,
                fesh=18001,
                title='cpa-green',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18113,
                fesh=18001,
                title='cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18113,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
        ]

    def test_default_offer_price_from_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        с ценой менее мин. запрошенной офферам, у которых опции не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-too-expensive

        Делаем запрос запрос за ДО с фильтрами для модели 18113 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - "no-cpa-too-cheap"
        Делаем запрос запрос за ДО с фильтрами для модели 18113 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер, найденным первым при "ослаблении"
        фильтров - "cpa-too-expensive"
        """
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18113&hid=18100&rids={}&relax-filters=1'.format(rids)
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-too-cheap"}, "benefit": {"type": "default"}})

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18113&hid=18100&rids={}&relax-filters=1&cpa=real'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "cpa-too-expensive"}, "benefit": {"type": "default"}})

    @classmethod
    def prepare_default_price_to_filter_relax(cls):
        """
        Создаем набор офферов с недостающими фильтрами по списку
        (см. https://st.yandex-team.ru/MARKETOUT-12190), кроме
        prepare_enabled, low_rating, delivery_options, offer-shipping,
        discount, booknow, home_region, shop, warranty и price_from
        Все офферы с CPA_NO, кроме офферов cpa-green
        Оффера, подходящего под все фильтры, нет
        """
        gl_red = GLParam(param_id=1203, value=RED)
        gl_green = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18114,
                fesh=18001,
                title='cpa-green',
                price_old=150,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=18114,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[310],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
        ]

    def test_default_offer_price_to_filter_relax(self):
        """
        Что тестируем: запрос за ДО в регионах 213 и 54
        без фильтра по CPA предпочитает оффер
        с ценой более макс. запрошенной офферам, у которых опции не подходят под
        другие фильтры, пусть они даже и CPA_REAL. Запрос с cpa=real
        вернет cpa-green

        Делаем запрос запрос за ДО с фильтрами для модели 18114 и флагом
        &relax-filters=1
        Ожидаем, что возвращается оффер, найденным первым при "ослаблении"
        фильтров - "no-cpa-too-expensive"
        Делаем запрос запрос за ДО с фильтрами для модели 18114 и флагами
        &relax-filters=1 и &cpa=real
        Ожидаем, что возвращается CPA-оффер "cpa-green", найденный первым при "ослаблении"
        фильтров, несмотря на то что gl-фильтры больше не участвуют в расслаблении
        (фильтр cpa=real не участвует в ослаблении)
        """
        for rids in [213, 54]:
            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18114&hid=18100&rids={}&relax-filters=1'.format(rids)
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-too-expensive"}, "benefit": {"type": "default"}})

            response = self.report.request_json(
                'place=productoffers&offers-set=default&hyperid=18114&hid=18100&rids={}&relax-filters=1&cpa=real'.format(
                    rids
                )
                + SEARCH_FILTERS
                + SEARCH_GL_FILTER
            )
            self.assertFragmentIn(response, {"titles": {"raw": "cpa-green"}, "benefit": {"type": "default"}})

    @classmethod
    def prepare_top6_price_prepay_warranty_region_shop_filter_ignorance(cls):
        """
        Создаем два набора офферов с hyperid 18116 b 18117 с недостающими
        фильтрами по списку (см. https://st.yandex-team.ru/MARKETOUT-12190),
        кроме guruligt
        Все офферы с CPA_NO
        В первом наборе 6 офферов, во втором 5
        """
        gl_red = GLParam(param_id=1203, value=RED)
        _ = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18116,
                fesh=18003,
                title='no-cpa-no-prepay',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18116,
                fesh=18001,
                title='no-cpa-too-expensive',
                price=900,
                price_old=1000,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18116,
                fesh=18001,
                title='no-cpa-too-cheap',
                price=90,
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18116,
                fesh=18001,
                title='no-cpa-no-warranty',
                price_old=150,
                cpa=Offer.CPA_NO,
                manufacturer_warranty=False,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18116,
                fesh=18005,
                title='no-cpa-filtered-shop',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING5, BOOKING15],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18116,
                fesh=18004,
                title='no-cpa-from-another-country',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18117,
                fesh=18001,
                title='no-cpa-without-book-now',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18117,
                fesh=18001,
                title='no-cpa-no-discount',
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18117,
                fesh=18001,
                title='no-cpa-no-store',
                price_old=150,
                cpa=Offer.CPA_NO,
                store=False,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING4, BOOKING14],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18117,
                fesh=18001,
                title='no-cpa-slow-non-free-delivery',
                booking_availabilities=[BOOKING1, BOOKING11],
                price_old=150,
                cpa=Offer.CPA_NO,
                glparams=[gl_red],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=5),
                ],
            ),
            Offer(
                hyperid=18117,
                fesh=18002,
                title='no-cpa-low-rating',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING2, BOOKING12],
                glparams=[gl_red],
            ),
        ]

    def test_top6_price_prepay_warranty_region_shop_filter_ignorance(self):
        """
        Что тестируем: запрос за топ-6 с &relax-filters=1 игнорирует
        все "магазинные" фильтры

        Делаем запросы за топ-6 с фильтрами для моделей 18116 и 18117 и флагом
        &relax-filters=1
        Ожидаем, что возвращаются запрос каждой модели возвращает все ее офферы,
        несмотря на то, что ни один из них не подходит под все фильтры
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=list&numdoc=6&hyperid=18116&hid=18100&rids=213&relax-filters=1'
            + SEARCH_FILTERS
        )
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-prepay"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-too-expensive"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-too-cheap"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-warranty"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-filtered-shop"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-from-another-country"}})

        response = self.report.request_json(
            'place=productoffers&offers-set=list&numdoc=6&hyperid=18117&hid=18100&rids=213&relax-filters=1'
            + SEARCH_FILTERS
        )
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-without-book-now"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-store"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-slow-non-free-delivery"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-low-rating"}})

    def test_default_offer_with_top_and_all_list(self):
        """
        Просим дефолтный оффер с релаксом фильтров и топ-6
        Проверяем, что магазинные фильтры зарелаксились
        Просим дефолтный оффер со всеми офферами
        Проверяем, что магазинные фильтры не зарелаксились
        Проверяем, что дефолтный оффер одинаковый
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=top,default&numdoc=6&hyperid=18116&hid=18100&rids=213&relax-filters=1'
            + SEARCH_FILTERS
        )

        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-prepay"}, "benefit": {"type": "default"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-prepay"}, "benefit": NoKey("benefit")})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-too-expensive"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-too-cheap"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-warranty"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-filtered-shop"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-from-another-country"}})
        self.assertFragmentIn(response, {"search": {"total": 6}})

        """
        проверяем, что на общем списке все фильтры применелись несмотря на relax-filters, а дефолтный оффер тем не менее нашелся
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=all,default&numdoc=6&hyperid=18116&hid=18100&rids=213&relax-filters=1'
            + SEARCH_FILTERS
        )

        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-prepay"}, "benefit": {"type": "default"}})
        self.assertFragmentIn(response, {"search": {"total": 1}})

        """
        Проверяем на общем списке без фильтров - все офферы возвращаются
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=all,default&numdoc=6&hyperid=18116&hid=18100&rids=213&relax-filters=1'
        )
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-prepay"}, "benefit": {"type": "default"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-prepay"}, "benefit": NoKey("benefit")})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-too-expensive"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-too-cheap"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-no-warranty"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-filtered-shop"}})
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa-from-another-country"}})
        self.assertFragmentIn(response, {"search": {"total": 6}})

    @classmethod
    def prepare_default_offer_delivery_filter_with_relaxing(cls):
        """
        Создаем два оффера с hyperid 18118, один подходящий под все фильтры,
        кроме &offer-shipping,но без доставки в регион 54 и другой,
        не подходящий под фильтр по CPA с доставкой в регион 54
        """
        gl_red = GLParam(param_id=1203, value=RED)
        _ = GLParam(param_id=1203, value=GREEN)

        cls.index.offers += [
            Offer(
                hyperid=18118,
                fesh=18007,
                title='no-store',
                price_old=150,
                cpa=Offer.CPA_REAL,
                store=False,
                delivery_options=[FREE_OPTION],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
            Offer(
                hyperid=18118,
                fesh=18007,
                title='no-cpa',
                price_old=150,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                delivery_buckets=[314],
                booking_availabilities=[BOOKING1, BOOKING11],
                glparams=[gl_red],
            ),
        ]

    @skip('MARKETOUT-43348  фильтр cpa=real не должен игнорироваться')
    def test_default_offer_delivery_filter_with_relaxing(self):
        """
        Что тестируем: фильтр по доставке действует при запросе за ДО с
        &relax-filters=1

        Делаем запрос за ДО в регионе 54 с фильтрами для модели 18118
        Проверяем, что на выдаче оффер no-cpa, не подходящий под фильтр по CPA,
        т.к. оффер, подходящий под фильтры, отфильтрован по доставке
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=18118&hid=18100&rids=54&relax-filters=1&cpa=real'
            + SEARCH_FILTERS
            + SEARCH_GL_FILTER
        )
        self.assertFragmentIn(response, {"titles": {"raw": "no-cpa"}, "benefit": {"type": "default"}})

    @classmethod
    def prepare_top6_filter_aggregation(cls):
        """
        Add two offers with color
        """

        cls.index.cpa_categories += [
            CpaCategory(hid=20000, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=2000, hid=20000, gltype=GLType.ENUM, values=[1, 2], unit_name="Color2", cluster_filter=True
            ),
        ]

        gl_red = GLParam(param_id=2000, value=1)
        gl_green = GLParam(param_id=2000, value=2)

        cls.index.shops += [
            Shop(
                fesh=20000, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=5.0), cpa=Shop.CPA_REAL
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=20000,
                fesh=20000,
                title='cpa-green1',
                price=325,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=20000,
                fesh=20000,
                title='no-cpa-green2',
                price=311,
                cpa=Offer.CPA_NO,
                delivery_options=[FREE_OPTION],
                glparams=[gl_green],
            ),
            Offer(
                hyperid=20000,
                fesh=20000,
                title='cpa-red',
                price=900,
                cpa=Offer.CPA_REAL,
                delivery_options=[FREE_OPTION],
                glparams=[gl_red],
            ),
        ]

    def test_top6_filter_aggregation(self):
        response = self.report.request_json(
            'pp=6&place=productoffers&offers-set=default,listCpa&hyperid=20000&hid=20000&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "2000",
                        "values": [
                            {"id": "1", "found": 1},
                            {"id": "2", "found": 2},
                        ],
                    }
                ]
            },
        )

    @classmethod
    def prepare_hide_nongl_filters(cls):

        cls.index.gltypes += [
            GLType(param_id=2240, hid=40000, gltype=GLType.ENUM, values=[1, 2], cluster_filter=True),
        ]

        cls.index.offers += [
            Offer(hyperid=30001, fesh=402, title='cpa-green1', price=325, glparams=[GLParam(param_id=2240, value=1)]),
            Offer(hyperid=30001, fesh=403, title='cpa-green1', price=325, glparams=[GLParam(param_id=2240, value=2)]),
        ]

    def test_hide_nongl_filters(self):
        """
        Проверяем, что не-гл фильтры возвращаются без cgi-параметра hidenonglfilters
        и возврашаются, если этот параметр установлен.
        Также проверяем, что гл-фильтр возвращается всегда.
        """
        response = self.report.request_json('place=productoffers&hyperid=30001&hid=40000')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "glprice"},
                    {"id": "2240"},
                    {"id": "manufacturer_warranty"},
                    {"id": "qrfrom"},
                    {"id": "offer-shipping"},
                    {"id": "fesh"},
                ]
            },
        )
        response = self.report.request_json('place=productoffers&hyperid=30001&hidenonglfilters=1&hid=40000')
        self.assertFragmentNotIn(response, {"filters": [{"id": "glprice"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "manufacturer_warranty"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "qrfrom"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "free-delivery"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "offer-shipping"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "fesh"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "2240", "values": [{"id": "1"}, {"id": "2"}]}]})

    def test_hide_gl_filters(self):
        """
        Проверяем, что глфильтры возвращаются без cgi-параметра hideglfilters
        и возврашаются, если этот параметр установлен.
        Также проверяем, что гл-фильтр возвращается всегда.
        """
        response = self.report.request_json(
            'pp=6&place=productoffers&offers-set=default,listCpa&hyperid=20000&hid=20000&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "2000",
                        "values": [
                            {"id": "1", "found": 1},
                            {"id": "2", "found": 2},
                        ],
                    }
                ]
            },
        )

        response = self.report.request_json('place=productoffers&hyperid=30001&hideglfilters=1&hid=40000')
        # проверим что gl-фильтров нет
        self.assertFragmentNotIn(response, {"filters": [{"id": "1"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "2"}]})

        # проверим что НЕgl-фильтры есть
        self.assertFragmentIn(response, {"filters": [{"id": "glprice"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "manufacturer_warranty"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "qrfrom"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "offer-shipping"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "fesh"}]})

    @classmethod
    def prepare_shops_do_not_vanish_in_filter(cls):
        cls.index.shops += [
            Shop(fesh=4400, priority_region=213),
            Shop(fesh=4401, priority_region=213),
            Shop(fesh=4402, priority_region=213),
            Shop(fesh=4403, priority_region=213),
        ]

        cls.index.offers += [
            Offer(hyperid=30002, fesh=4400, sku=4400, title='present_offer', price=125),
            Offer(hyperid=30002, fesh=4401, sku=4400, title='present_offer_2', price=225),
            Offer(hyperid=30002, fesh=4402, sku=4400, title='filtered_offer', price=325),
            Offer(hyperid=30002, fesh=4403, sku=4400, title='filtered_offer_2', price=425),
        ]

    def test_shops_do_not_vanish_in_filter(self):
        '''
        Тестируем, что если фильтр по магазинам включен (выбраны некоторые магазины), то остальные магазины из него не исчезают.
        Тестируем, что если выбран только один магазин, то сам фильтр не исчезает
        '''

        for addSkuStatsParameter in ["&add-sku-stats=0", "&add-sku-stats=1"]:
            request = (
                "place=productoffers&market-sku=4400&debug=1"
                "&hyperid=30002"
                "&fesh=4400%2C4401"
                "&relax-filters=1"
                "&show-cutprice=1"
                "&grhow=supplier"
                "&rearr-factors=enable_business_id=1"
            ) + addSkuStatsParameter
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "fesh",
                            "values": [
                                {
                                    "checked": True,
                                    "id": "4400",
                                },
                                {
                                    "checked": True,
                                    "id": "4401",
                                },
                                {"id": "4402"},
                                {"id": "4403"},
                            ],
                        }
                    ]
                },
            )

            request = (
                "place=productoffers&market-sku=4400&debug=1"
                "&hyperid=30002"
                "&fesh=4400"
                "&relax-filters=1"
                "&show-cutprice=1"
                "&grhow=supplier"
                "&rearr-factors=enable_business_id=1"
            ) + addSkuStatsParameter
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "fesh",
                            "values": [
                                {
                                    "checked": True,
                                    "id": "4400",
                                },
                                {
                                    "id": "4401",
                                },
                                {"id": "4402"},
                                {"id": "4403"},
                            ],
                        }
                    ]
                },
            )


if __name__ == '__main__':
    main()

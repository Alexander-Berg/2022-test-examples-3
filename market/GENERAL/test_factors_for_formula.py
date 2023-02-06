#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from simple_testcase import get_timestamp
from core.crypta import CryptaSearchQueriesFeature, CryptaFeature, CryptaName
from core.types import (
    BlueOffer,
    CardCategory,
    CategoryGenderStat,
    CategoryOverallStatsRecord,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MinBidsCategory,
    MinBidsModel,
    MinBidsPriceGroup,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    Opinion,
    Outlet,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
    YamarecCategoryPartition,
    YamarecPlace,
    YamarecPlaceReasonsToBuy,
)

from core.testcase import TestCase, main
from core.matcher import Contains, NoKey, Absent, Round, NotEmpty, Not

DOC_EMBEDDINGS_FACTORS_INDICES = {
    "marketclick": [10, 17, 34],
    "hard2": [0],
}


class T(TestCase):
    """Тесты на факторы для формулы на базовом
    Тесты, проверяющие cgi параметр &trace-factor и проверяющие,
    что fullFormulaInfo есть в недебажной выдаче репорта"""

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.default_search_experiment_flags += [
            'market_hide_long_delivery_offers=0'
        ]  # TODO: MARKETOUT-47769 удалить вместе с флагом

        cls.index.regiontree += [
            Region(
                rid=3,
                name='Центральный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        name='Москва и Московская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        latitude=55.815792,
                        longitude=37.380031,
                        population=45800000,
                        chief=213,
                        children=[
                            Region(
                                rid=213,
                                name='Москва',
                                latitude=55.815792,
                                longitude=37.380031,
                                population=15000000,
                                chief=213,
                            ),
                            Region(rid=10758, name='Химки', latitude=55.888796, longitude=37.430328, population=230000),
                        ],
                    ),
                    Region(
                        rid=10650,
                        name='Брянская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        latitude=52.909192,
                        longitude=33.422197,
                        chief=191,
                        children=[
                            Region(rid=191, name='Брянск', latitude=53.243325, longitude=34.363731),
                            Region(
                                rid=98726,
                                name='Злынковский район',
                                chief=20184,
                                children=[Region(rid=20184, name='Злынка', latitude=52.427384, longitude=31.737075)],
                            ),
                        ],
                    ),
                ],
            ),
            Region(
                rid=73,
                name="Дальневосточный федеральный округ",
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=11409,
                        name='Приморский край',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        latitude=45.041980,
                        longitude=134.709375,
                        population=1913000,
                        chief=75,
                        children=[
                            Region(
                                rid=75, name='Владивосток', latitude=43.115141, longitude=131.885341, population=604901
                            ),
                        ],
                    )
                ],
            ),
            Region(
                rid=20574,
                name="Кипр",
                region_type=Region.COUNTRY,
                children=[
                    Region(
                        rid=105225,
                        name="Район Пафос",
                        region_type=Region.FEDERATIVE_SUBJECT,
                        latitude=34.771183,
                        longitude=32.426127,
                        chief=21242,
                        children=[Region(rid=21242, name="Пафос", latitude=34.771183, longitude=32.426127)],
                    )
                ],
            ),
            Region(rid=12345, exist=False),
        ]

        cls.index.hypertree = [
            HyperCategory(
                hid=1,
                uniq_name="Услуги",
                children=[
                    HyperCategory(
                        hid=11,
                        uniq_name="Юридические услуги",
                        children=[
                            HyperCategory(
                                hid=111,
                                uniq_name="Регистрация и ликвидация предприятий",
                                children=[
                                    HyperCategory(
                                        hid=1111,
                                        tovalid=1,
                                        name="HID-1111",
                                        uniq_name="Ликвидация предприятий",
                                        output_type=HyperCategoryType.GURU,
                                    )
                                ],
                            ),
                            HyperCategory(hid=112, uniq_name="Покупка и продажа оффшоров"),
                        ],
                    ),
                    HyperCategory(hid=12, uniq_name="Киллерские услуги"),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                name="Магазин в Химках (с доставкой по России, есть опции для Брянска и Владивостока)",
                fesh=1,
                priority_region=10758,
                regions=[225],
                home_region=225,
                delivery_service_outlets=[222],
                new_shop_rating=NewShopRating(new_rating_total=4.0, rec_and_nonrec_pub_count=200000),
            ),
            Shop(
                name="Магазин в Москве с самовывозом во Москве, Владивостоке и с магазином в Брянске",
                fesh=2,
                priority_region=213,
                regions=[],
                home_region=225,
            ),
            Shop(
                name="Контора на Кипре (г.Пафос) с доставкой в Россию и Кипр",
                fesh=3,
                priority_region=21242,
                regions=[20574, 225],
                home_region=20574,
            ),
            Shop(name="Лавка", fesh=4, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_REAL),
            Shop(name='blue_shop', fesh=5, priority_region=213, warehouse_id=145),
        ]

        cls.index.outlets += [
            Outlet(point_id=1, region=75, point_type=Outlet.FOR_PICKUP, fesh=2),
            Outlet(point_id=2, region=213, point_type=Outlet.FOR_PICKUP, fesh=2),
            Outlet(point_id=3, region=191, point_type=Outlet.FOR_STORE, fesh=2),
            Outlet(point_id=222, region=213, point_type=Outlet.FOR_POST_TERM, delivery_service_id=103),
            Outlet(point_id=333, region=213, point_type=Outlet.FOR_POST, delivery_service_id=104),
            Outlet(point_id=334, region=213, point_type=Outlet.FOR_POST, delivery_service_id=104),
            Outlet(point_id=335, region=213, point_type=Outlet.FOR_POST, delivery_service_id=104),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5002,
                fesh=2,
                carriers=[99],
                options=[PickupOption(outlet_id=1), PickupOption(outlet_id=2), PickupOption(outlet_id=3)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5103,
                carriers=[103],
                options=[PickupOption(outlet_id=222, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5104,
                carriers=[104],
                options=[PickupOption(outlet_id=333), PickupOption(outlet_id=334), PickupOption(outlet_id=335)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                fesh=1,
                carriers=[1, 3, 5, 7, 9],
                regional_options=[
                    RegionalDelivery(
                        rid=191,
                        options=[
                            # доставка в Брянск
                            DeliveryOption(price=5000, day_from=1, day_to=3),
                            DeliveryOption(price=3000, day_from=5, day_to=10),
                        ],
                    ),
                    RegionalDelivery(
                        rid=75,
                        options=[
                            # доставка во Владивосток
                            DeliveryOption(price=8000, day_from=3, day_to=5),
                            DeliveryOption(price=5000, day_from=10, day_to=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=3,
                fesh=3,
                carriers=[],
                regional_options=[
                    RegionalDelivery(
                        rid=225,
                        options=[
                            # доставка в Россию
                            DeliveryOption(price=10000, day_from=15, day_to=15),
                            DeliveryOption(price=1000000, day_from=0, day_to=1),
                        ],
                    ),
                    RegionalDelivery(
                        rid=20574,
                        options=[
                            # доставка по Кипру
                            DeliveryOption(price=0, day_from=0, day_to=1)
                        ],
                    ),
                ],
            ),
        ]

        default_local_delivery = [DeliveryOption(price=200, day_from=1, day_to=2)]

        cls.index.offers += [
            Offer(
                fesh=1,
                hid=1111,
                title="Ликвидация ООО - опытные юристы в Химках",
                delivery_buckets=[1],
                delivery_options=default_local_delivery,
                price=50 * 1000,
                pickup_buckets=[5103],
            ),
            Offer(
                fesh=2,
                hid=111,
                title="Регистрация ООО - опытные юристы из Москвы в вашем городе",
                has_delivery_options=False,
                pickup=True,
                price=20 * 1000,
                pickup_buckets=[5002],
            ),
            Offer(
                fesh=3,
                hid=112,
                title="Купить оффшор на Кипре",
                delivery_buckets=[3],
                delivery_options=default_local_delivery,
                price=10 * 1000 * 1000,
            ),
            Offer(
                fesh=1,
                hid=12,
                title="Покончить с конкурентами",
                delivery_buckets=[1],
                delivery_options=default_local_delivery,
                price=1200 * 1000,
                pickup_buckets=[5103],
            ),
            Offer(
                fesh=1,
                hid=11,
                title="Опытный юрист срочно поставит закорючку на вашей бумажке",
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=0)],
                price=1000,
                pickup_buckets=[5103],
            ),
            Offer(
                fesh=1,
                hid=11,
                title="Опытный юрист оформит ваше вступление в наследство замком",
                delivery_options=[
                    DeliveryOption(price=0, day_from=32, day_to=32)
                ],  # TODO: MARKETOUT-47769 вернуть как было. Удалить значения
                price=5000,
                pickup_buckets=[5103],
            ),
            Offer(
                fesh=1,
                hid=12,
                title="Опытный киллер пришлет видео по эл.почте или вышлет труп посылкой",
                price=200,
                has_delivery_options=False,
                store=False,
                post_term_delivery=True,
                download=True,
                pickup_buckets=[5103],
                waremd5='09lEaAKkQll1XTaaaaaaaQ',
            ),
            Offer(
                fesh=1,
                hid=12,
                title="Начинающий киллер перешлет части вашей жертвы почтой россии",
                price=200,
                has_delivery_options=False,
                store=False,
                post_term_delivery=False,
                post_buckets=[5104],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=11201,
                hid=112,
                created_ts=get_timestamp(1985, 6, 15),
                clicks=10,
                model_clicks=5,
                title="Готовый бизнес. Легально. Недорого",
            )
        ]

        cls.index.offers += [
            Offer(
                fesh=4,
                hyperid=11201,
                title="Лаваш, лаваш, хороший лаваш, обычный лаваш, тепель в Лавке есть лаваш",
                cpa=Offer.CPA_REAL,
                is_cpc=False,
            ),
            Offer(fesh=4, hyperid=11201, title="Купи 3 пирожка - собери котенка", cpa=Offer.CPA_NO, is_cpc=True),
        ]

        cls.index.regional_models += [
            RegionalModel(
                hyperid=11201, rids=[213], offers=25, retailers=7, price_min=1200, price_max=1500, has_cpa=True
            ),
            RegionalModel(hyperid=11201, rids=[2], offers=3, retailers=1, price_min=1000, price_max=1300),
            RegionalModel(hyperid=11201, rids=[-1], offers=130, retailers=20, price_min=1000, price_max=2300),
        ]

        cls.index.cards += [
            CardCategory(hid=1111, popularity=0.64),
        ]

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=112,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0,
                search_conversion=0.05,
                card_conversion=0.02,
                full_card_conversion=1.0,
            )
        ]
        cls.index.min_bids_model_stats += [
            MinBidsModel(
                model_id=11201,
                geo_group_id=1,
                drr=0.01,
                search_clicks=30,
                search_orders=3,
                card_clicks=144,
                card_orders=9,
                full_card_orders=0,
                full_card_clicks=0,
            )
        ]
        cls.index.min_bids_price_groups += [MinBidsPriceGroup(0)]

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_cpa_factors(self):
        # В Москве модель имеет cpa-офферы и у нее в статистиках сохранено has_cpa=True
        response = self.report.request_json("place=prime&text=Бизнес Пирожки Лаваш&rids=213&debug=da")
        self.assertFragmentIn(
            response,
            {'titles': {'raw': 'Готовый бизнес. Легально. Недорого'}, 'debug': {'factors': {'MODEL_HAS_CPA': '1'}}},
        )

        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Лаваш, лаваш, хороший лаваш, обычный лаваш, тепель в Лавке есть лаваш'},
                'debug': {'factors': {'MODEL_HAS_CPA': '1', 'CPA1': '1', 'CPC': '1'}},  # магазин поддерживает cpc
            },
        )

        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Купи 3 пирожка - собери котенка'},
                'debug': {'factors': {'MODEL_HAS_CPA': '1', 'CPA1': NoKey('CPA1'), 'CPC': '1'}},
            },
        )

        # в Питере по статистикам у этой модели нет cpa-офферов
        response = self.report.request_json("place=prime&text=Бизнес Пирожки Лаваш&rids=2&debug=da")
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Готовый бизнес. Легально. Недорого'},
                'debug': {
                    'factors': {
                        'MODEL_HAS_CPA': NoKey('MODEL_HAS_CPA'),
                    }
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Лаваш, лаваш, хороший лаваш, обычный лаваш, тепель в Лавке есть лаваш'},
                'debug': {
                    'factors': {
                        'MODEL_HAS_CPA': NoKey('MODEL_HAS_CPA'),
                        'CPA1': '1',  # факторы никак не связаны, статистика считается в индексаторе
                    }
                },
            },
        )

    def test_trace_factor(self):
        """Провереям, что при указывании &trace-factor=FACTOR_XXX,FACTOR_YYY в json вернутся значения
        указанных факторов и только их.
        """

        cgi = '&trace-factor=SHOP_OPINION_COUNT,OFFER_PRICE,COURIER_DELIVERY_TYPE,FACTOR_THAT_DOES_NOT_EXIST'

        response = self.report.request_json('place=prime&text=ликвидация&rids=10758' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "Ликвидация ООО - опытные юристы в Химках"},
                            "trace": {
                                "factors": {
                                    "SHOP_OPINION_COUNT": 200000,
                                    "OFFER_PRICE": 50000,
                                    "FACTOR_THAT_DOES_NOT_EXIST": NoKey("FACTOR_THAT_DOES_NOT_EXIST"),
                                    "COURIER_DELIVERY_TYPE": 3,
                                }
                            },
                        }
                    ]
                }
            },
        )

        cgi = '&trace-factor=SHOP_OPINION_COUNT,COURIER_DELIVERY_TYPE,FACTOR_THAT_DOES_NOT_EXIST'

        response = self.report.request_json('place=prime&text=ликвидация&rids=10758' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "Ликвидация ООО - опытные юристы в Химках"},
                            "trace": {
                                "factors": {
                                    "SHOP_OPINION_COUNT": 200000,
                                    "OFFER_PRICE": NoKey("OFFER_PRICE"),
                                    "FACTOR_THAT_DOES_NOT_EXIST": NoKey("FACTOR_THAT_DOES_NOT_EXIST"),
                                    "COURIER_DELIVERY_TYPE": 3,
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_fullFormulaInfo(self):
        """Проверяем наличие fullFormulaInfo в "trace"->"fullFormulaInfo" """
        response = self.report.request_json('place=prime&text=ликвидация&rids=10758')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "Ликвидация ООО - опытные юристы в Химках"},
                            "trace": {
                                "fullFormulaInfo": [
                                    {"tag": "Default"},
                                    {"tag": "Meta"},
                                ],
                                "factors": NoKey("factors"),
                            },
                        }
                    ]
                }
            },
        )

        response = self.report.request_json('place=productoffers&hyperid=977773')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "collapsedOffer"},
                            "trace": {
                                "fullFormulaInfo": [
                                    {"tag": "CpcClick"},
                                    {"tag": "CpaBuy"},
                                ],
                                "factors": NoKey("factors"),
                            },
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=direct&offerid=qtZDmKlp7DGGgA1BL6erMQ&regset=1'
        )
        self.assertFragmentIn(
            response, {"search": {"results": [{"titles": {"raw": "collapsedOffer"}, "trace": NoKey("trace")}]}}
        )

    @classmethod
    def prepare_trace_factors_place_offerinfo(cls):
        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(price=1000, feedid=1, waremd5='Sku1Offer1-IiLVm1Goleg'),
                ],
            ),
        ]

    def test_trace_factors_place_offerinfo(self):
        """Проверяем, что на place=offerinfo факторы считаются только при наличии trace-factor
        https://st.yandex-team.ru/MARKETOUT-25973
        """

        trace_factor_cgi = '&trace-factor=OFFER_PRICE'

        # Без флага не считаем
        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=direct&offerid=qtZDmKlp7DGGgA1BL6erMQ&regset=1'
        )
        self.assertFragmentIn(response, {"search": {"results": [{"trace": NoKey("trace")}]}})

        # C флагом считаем
        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=direct&offerid=qtZDmKlp7DGGgA1BL6erMQ&regset=1' + trace_factor_cgi
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "trace": {
                                "factors": {
                                    "OFFER_PRICE": 100,
                                },
                                "fullFormulaInfo": [{"tag": "Default"}],
                            }
                        }
                    ]
                }
            },
        )

        # То же самое для синих офферов
        response = self.report.request_json('place=offerinfo&rids=0&regset=2&offerid=Sku1Offer1-IiLVm1Goleg')
        self.assertFragmentIn(response, {"search": {"results": [{"trace": NoKey("trace")}]}})

        response = self.report.request_json(
            'place=offerinfo&rids=0&regset=2&offerid=Sku1Offer1-IiLVm1Goleg' + trace_factor_cgi
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "trace": {
                                "factors": {
                                    "OFFER_PRICE": 1000,
                                },
                                "fullFormulaInfo": [{"tag": "Default"}],
                            }
                        }
                    ]
                }
            },
        )

    def test_trace_factor_unexpected_usage_place_prime(self):
        request = 'place=prime&text=ликвидация&rids=10758'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"search": {"results": [{"trace": {"factors": NoKey("factors")}}]}})

        response = self.report.request_json(request + '&trace-factor')
        self.assertFragmentIn(response, {"search": {"results": [{"trace": {"factors": NoKey("factors")}}]}})

        response = self.report.request_json(request + '&trace-factor=')
        self.assertFragmentIn(response, {"search": {"results": [{"trace": {"factors": NoKey("factors")}}]}})

        response = self.report.request_json(request + '&trace-factor=0')
        self.assertFragmentIn(response, {"search": {"results": [{"trace": {"factors": {}}}]}})

    def test_trace_factor_unexpected_usage_place_offerinfo(self):
        request = 'place=offerinfo&rids=0&regset=2&offerid=Sku1Offer1-IiLVm1Goleg'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"search": {"results": [{"trace": NoKey("trace")}]}})

        response = self.report.request_json(request + '&trace-factor')
        self.assertFragmentIn(response, {"search": {"results": [{"trace": NoKey("trace")}]}})

        response = self.report.request_json(request + '&trace-factor=')
        self.assertFragmentIn(response, {"search": {"results": [{"trace": NoKey("trace")}]}})

        response = self.report.request_json(request + '&trace-factor=0')
        self.assertFragmentIn(response, {"search": {"results": [{"trace": {"factors": {}}}]}})

    def test_courier_delivery_type_factors(self):
        """Проверяем фичи связанные с типом курьерской доставки (локальная, по стране, из других стран)
        Сроки доставки, цена доставки - из дефолтной опции доставки отображаемой в сниппете
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        cgi = '&debug=da' + unified_off_flags  # regional-delivery для вывода тега options

        # Магазин в Химках, запрос из Химок - локальная доставка
        response = self.report.request_json('place=prime&text=ликвидация&rids=10758' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Ликвидация ООО - опытные юристы в Химках"},
                            "delivery": {
                                "shopPriorityRegion": {"id": 10758, "name": "Химки"},
                                "options": [
                                    {
                                        "price": {"value": "200"},
                                        "dayFrom": 1,
                                        "dayTo": 2,
                                        "isDefault": True,
                                        "serviceId": "99",
                                    }
                                ],
                            },
                            "debug": {
                                "factors": {
                                    "SHOP_OPINION_COUNT": "200000",
                                    "LOG_SHOP_OPINION_COUNT": "17.60964775",
                                    "OFFER_PRICE": "50000",
                                    "COURIER_DELIVERY_TYPE": "3",
                                    "COURIER_DELIVERY_PRICE": "200",
                                    "COURIER_DELIVERY_PRICE_IN_PERCENT": Round(0.004),
                                    "COURIER_DELIVERY_FROM_DAYS": "1",
                                    "COURIER_DELIVERY_TO_DAYS": "2",
                                    "COURIER_DELIVERY_CARRIER": "99",
                                    "COURIER_DELIVERY_IS_SHOP_CARRIER": "1",
                                    "COURIER_DELIVERY_EXISTS": "1",
                                    "PICKUP_TYPE": NoKey("PICKUP_TYPE"),
                                    "PICKUP_EXISTS": NoKey("PICKUP_EXISTS"),
                                    "HAS_STORE": NoKey("HAS_STORE"),
                                }
                            },
                        }
                    ],
                }
            },
        )

        # Магазин в Химках, запрос из Владивостока - выбирается дефолтная опция доставки и описываются ее параметры
        response = self.report.request_json('place=prime&text=ликвидация&rids=75' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Ликвидация ООО - опытные юристы в Химках"},
                            "delivery": {
                                "shopPriorityRegion": {"id": 10758, "name": "Химки"},
                                "options": [
                                    {
                                        "price": {"value": "5000"},
                                        "dayFrom": 10,
                                        "dayTo": 24,
                                        "isDefault": True,
                                        "serviceId": "1",
                                    },
                                    {
                                        "price": {"value": "8000"},
                                        "dayFrom": 3,
                                        "dayTo": 5,
                                        "isDefault": False,
                                        "serviceId": "1",
                                    },
                                ],
                            },
                            "debug": {
                                "factors": {
                                    "OFFER_PRICE": "50000",
                                    "COURIER_DELIVERY_TYPE": "2",
                                    "COURIER_DELIVERY_PRICE": "5000",
                                    "COURIER_DELIVERY_PRICE_IN_PERCENT": Round(0.1),
                                    "COURIER_DELIVERY_FROM_DAYS": "10",
                                    "COURIER_DELIVERY_TO_DAYS": "24",
                                    "COURIER_DELIVERY_CARRIER": "1",
                                    "COURIER_DELIVERY_IS_SHOP_CARRIER": NoKey("COURIER_DELIVERY_IS_SHOP_CARRIER"),
                                    "COURIER_DELIVERY_EXISTS": "1",
                                    "PICKUP_TYPE": NoKey("PICKUP_TYPE"),
                                    "PICKUP_EXISTS": NoKey("PICKUP_EXISTS"),
                                    "HAS_STORE": NoKey("HAS_STORE"),
                                }
                            },
                        }
                    ],
                }
            },
        )

        # Магазин в Химках, запрос из Москвы - опции курьерской доставки не определены
        response = self.report.request_json('place=prime&text=ликвидация&rids=213' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Ликвидация ООО - опытные юристы в Химках"},
                            "delivery": {"shopPriorityRegion": {"id": 10758, "name": "Химки"}, "options": []},
                            "debug": {
                                "factors": {
                                    "COURIER_DELIVERY_TYPE": "2",
                                    "COURIER_DELIVERY_NO_OPTIONS": "1",
                                    "COURIER_DELIVERY_PRICE": NoKey("COURIER_DELIVERY_PRICE"),
                                    "COURIER_DELIVERY_EXISTS": "1",
                                    "PICKUP_TYPE": NoKey("PICKUP_TYPE"),
                                    "PICKUP_EXISTS": NoKey("PICKUP_EXISTS"),
                                    "HAS_STORE": NoKey("HAS_STORE"),
                                }
                            },
                        }
                    ],
                }
            },
        )

        # Магазин на Кипре, запрос из Брянска - междугородняя доставка
        response = self.report.request_json('place=prime&text=оффшор&rids=191' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Купить оффшор на Кипре"},
                            "delivery": {
                                "shopPriorityRegion": {"id": 21242, "name": "Пафос"},
                                "options": [
                                    {
                                        "price": {"value": "10000"},
                                        "dayFrom": 15,
                                        "dayTo": 15,
                                        "isDefault": True,
                                        "serviceId": "99",
                                    },
                                    {
                                        "price": {"value": "1000000"},
                                        "dayFrom": 0,
                                        "dayTo": 1,
                                        "isDefault": False,
                                        "serviceId": "99",
                                    },
                                ],
                            },
                            "debug": {
                                "factors": {
                                    "OFFER_PRICE": "10000000",
                                    "COURIER_DELIVERY_TYPE": "1",
                                    "COURIER_DELIVERY_PRICE": "10000",
                                    "COURIER_DELIVERY_PRICE_IN_PERCENT": Round(0.001),
                                    "COURIER_DELIVERY_FROM_DAYS": "15",
                                    "COURIER_DELIVERY_TO_DAYS": "15",
                                    "COURIER_DELIVERY_CARRIER": "99",
                                    "COURIER_DELIVERY_IS_SHOP_CARRIER": "1",
                                    "PICKUP_TYPE": NoKey("PICKUP_TYPE"),
                                    "HAS_STORE": NoKey("HAS_STORE"),
                                }
                            },
                        }
                    ],
                }
            },
        )

        # Магазин в Химках, запрос из Химок, бесплатная доставка от 0 дней
        response = self.report.request_json('place=prime&rids=10758&debug=da&text=поставить+закорючку+на+бумажке')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "Опытный юрист срочно поставит закорючку на вашей бумажке"},
                            "delivery": {
                                "shopPriorityRegion": {"id": 10758, "name": "Химки"},
                                "options": [
                                    {
                                        "price": {"value": "0"},
                                        "dayFrom": 0,
                                        "dayTo": 0,
                                        "isDefault": True,
                                    },
                                ],
                            },
                            "debug": {
                                "factors": {
                                    "COURIER_DELIVERY_TYPE": "3",
                                    "COURIER_DELIVERY_PRICE": NoKey("COURIER_DELIVERY_PRICE"),  # 0руб
                                    "COURIER_DELIVERY_PRICE_IN_PERCENT": NoKey(
                                        "COURIER_DELIVERY_PRICE_IN_PERCENT"
                                    ),  # 0%
                                    "COURIER_DELIVERY_FREE": "1",
                                }
                            },
                        }
                    ]
                }
            },
        )

        # Магазин в Химках, запрос из Химок, бесплатная доставка, cрок доставки неизвестен
        response = self.report.request_json('place=prime&rids=10758&debug=da&text=оформить+наследство')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Опытный юрист оформит ваше вступление в наследство замком"},
                            "delivery": {
                                "shopPriorityRegion": {"id": 10758, "name": "Химки"},
                                "options": [
                                    {
                                        "price": {"value": "0"},
                                        "dayFrom": NoKey("dayFrom"),
                                        "dayTo": NoKey("dayTo"),
                                        "isDefault": True,
                                    },
                                ],
                            },
                            "debug": {
                                "factors": {
                                    "COURIER_DELIVERY_TYPE": "3",
                                    "COURIER_DELIVERY_PRICE": NoKey("COURIER_DELIVERY_PRICE"),  # 0руб
                                    "COURIER_DELIVERY_PRICE_IN_PERCENT": NoKey(
                                        "COURIER_DELIVERY_PRICE_IN_PERCENT"
                                    ),  # 0%
                                    "COURIER_DELIVERY_FREE": "1",
                                    "COURIER_DELIVERY_FROM_DAYS": "32",
                                    "COURIER_DELIVERY_TO_DAYS": "32",
                                }
                            },
                        }
                    ],
                }
            },
        )

    def test_non_courier_delivery_factors(self):
        """Фичи связанные с самовывозом, почтаматами, оффлайн магазинами и загрузкой через интернет"""

        cgi = '&debug=da'  # regional-delivery для вывода тега options

        # Магазин в Москве с самовывозом из Владивостока, запрос из Владивостока
        response = self.report.request_json('place=prime&text=регистрация&rids=75' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Регистрация ООО - опытные юристы из Москвы в вашем городе"},
                            "debug": {
                                "factors": {
                                    "COURIER_DELIVERY_TYPE": NoKey("COURIER_DELIVERY_TYPE"),
                                    "PICKUP_TYPE": "2",
                                    "PICKUP_EXISTS": "1",
                                    "HAS_STORE": NoKey("HAS_STORE"),
                                }
                            },
                        }
                    ],
                }
            },
        )

        # Магазин в Москве с самовывозом из Москвы, запрос из Москвы
        response = self.report.request_json('place=prime&text=регистрация&rids=213' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Регистрация ООО - опытные юристы из Москвы в вашем городе"},
                            "debug": {
                                "factors": {
                                    "COURIER_DELIVERY_TYPE": NoKey("COURIER_DELIVERY_TYPE"),
                                    "PICKUP_TYPE": "3",
                                    "PICKUP_EXISTS": "1",
                                    "HAS_STORE": NoKey("HAS_STORE"),
                                    "STORE_OUTLETS": NoKey("STORE_OUTLETS"),
                                }
                            },
                        }
                    ],
                }
            },
        )

        # Магазин в Москве с магазином в Брянске, запрос из Брянска
        response = self.report.request_json('place=prime&text=регистрация&rids=191' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "titles": {"raw": "Регистрация ООО - опытные юристы из Москвы в вашем городе"},
                            "debug": {
                                "factors": {
                                    "COURIER_DELIVERY_TYPE": NoKey("COURIER_DELIVERY_TYPE"),
                                    "PICKUP_TYPE": NoKey("PICKUP_TYPE"),
                                    "PICKUP_EXISTS": NoKey("PICKUP_EXISTS"),
                                    "HAS_STORE": "1",
                                    "STORE_OUTLETS": "1",
                                }
                            },
                        }
                    ],
                }
            },
        )

        # Магазин в Химках, запрос из Химок, у оффера есть достава через интернет и доставка в inpost
        response = self.report.request_json('place=prime&text=труп+жертвы&rids=213' + cgi)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "titles": {"raw": "Опытный киллер пришлет видео по эл.почте или вышлет труп посылкой"},
                            "debug": {
                                "factors": {
                                    # загружаемый офферв воспринимается как курьерская доставка (без опций)
                                    "COURIER_DELIVERY_TYPE": "3",
                                    "COURIER_DELIVERY_NO_OPTIONS": "1",
                                    # inpost_доставка - это частный случай самовывоза
                                    "PICKUP_TYPE": "2",
                                    "PICKUP_EXISTS": "1",
                                    "HAS_POST_TERM_DELIVERY": "1",
                                    "HAS_DOWNLOAD": "1",
                                    # один пункт самовывоза
                                    "ALL_OUTLETS": "1",
                                    "ALL_SHIPPING_OUTLETS": "1",
                                    "PICKUP_OUTLETS": "1",
                                    "POST_TERM_OUTLETS": "1",
                                    "DEPOT_OUTLETS": NoKey("DEPOT_OUTLETS"),
                                    "POST_OUTLETS": NoKey("POST_OUTLETS"),
                                    "STORE_OUTLETS": NoKey("STORE_OUTLETS"),
                                    # бесплатный самовывоз
                                    "PICKUP_PRICE": NoKey("PICKUP_PRICE"),  # 0 руб
                                    "PICKUP_PRICE_IN_PERCENT": NoKey("PICKUP_PRICE"),  # 0%
                                    "PICKUP_FREE": "1",
                                    "MIN_DELIVERY_PRICE": NoKey("MIN_DELIVERY_PRICE"),  # 0 руб
                                    "MIN_DELIVERY_PRICE_IN_PERCENT": NoKey("MIN_DELIVERY_PRICE_IN_PERCENT"),  # 0%
                                }
                            },
                        },
                        {
                            "titles": {"raw": "Начинающий киллер перешлет части вашей жертвы почтой россии"},
                            "debug": {
                                "factors": {
                                    # доставка почтой - не является самовывозом
                                    "PICKUP_TYPE": NoKey("PICKUP_TYPE"),
                                    "PICKUP_EXISTS": NoKey("PICKUP_EXISTS"),
                                    "HAS_POST_DELIVERY": "1",
                                    "HAS_POST_TERM_DELIVERY": NoKey("HAS_POST_TERM_DELIVERY"),
                                    # TODO: почтовые аутлеты почему-то не входят в список всех аутлетов
                                    # "POST_OUTLETS": "3",
                                    # "ALL_OUTLETS": "3",
                                    # нет условий самовывоза
                                    "PICKUP_PRICE": NoKey("PICKUP_PRICE"),
                                    "PICKUP_PRICE_IN_PERCENT": NoKey("PICKUP_PRICE"),
                                    "PICKUP_FREE": NoKey("PICKUP_FREE"),
                                    # нет ни условий самовывоза ни условий доставки
                                    "MIN_DELIVERY_PRICE": "-1",
                                    "MIN_DELIVERY_PRICE_IN_PERCENT": "-1",
                                }
                            },
                        },
                    ],
                }
            },
        )

    def test_region_factors(self):
        """Фичи связанные с регионом пользователя и магазина"""

        # Магазин в Химках, пользователь во Владивостоке
        response = self.report.request_json("place=prime&text=ликвидация&debug=da&rids=75")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "USER_REGION_ID": "75",
                        "USER_REGION_LATITUDE": "43.11514282",
                        "USER_REGION_LONGITUDE": "131.8853455",
                        "USER_REGION_POPULATION": "604901",  # население Владивостока
                        "USER_FEDSUBJ_ID": "11409",
                        "USER_CHIEF_REGION_POPULATION": "604901",  # население центра Приморского края (Владивосток)
                        "USER_FEDDISTRICT_ID": "73",
                        "USER_FEDSUBJ_POPULATION": "1913000",  # население Приморского края
                        "USER_DISTANCE_TO_MOSCOW": "6435368",  # 6 435 km
                        "USER_DISTANCE_TO_CHIEF_REGION": NoKey(
                            "USER_DISTANCE_TO_CHIEF_REGION"
                        ),  # 0 km от Владивостока до центра Приморского края (Владивосток)
                        "USER_FROM_RUSSIA": "1",
                        "USER_REGION_IS_MEGACITY": NoKey("USER_REGION_IS_MEGACITY"),  # 0 = нет
                        "SHOP_REGION_ID": "10758",
                        "SHOP_REGION_LATITUDE": "55.88879776",
                        "SHOP_REGION_LONGITUDE": "37.43032837",
                        "SHOP_REGION_POPULATION": "230000",
                        "SHOP_FEDSUBJ_ID": "1",
                        "SHOP_FEDSUBJ_POPULATION": "45800000",  # население Московской области
                        "SHOP_FEDDISTRICT_ID": "3",
                        "SHOP_REGION_IS_MEGACITY": NoKey("SHOP_REGION_IS_MEGACITY"),
                        "SHOP_CHIEF_REGION_POPULATION": "15000000",
                        "SHOP_CHIEF_REGION_IS_MEGACITY": "1",  # Москва - город миллионник
                        "SHOP_DISTANCE_TO_CHIEF_REGION": "8724.118164",  # 8 km от Химок до Москвы
                        "SHOP_DISTANCE_TO_MOSCOW": "8724.118164",
                        "SHOP_FROM_RUSSIA": "1",
                        "SHOP_REGION_IS_UNKNOWN": NoKey("SHOP_REGION_IS_UNKNOWN"),
                        # Взаиморасположение пользователя и магазина
                        "SHOP_IN_USER_COUNTRY": "1",
                        "DIST_BETW_USER_AND_SHOP_REGIONS": "6428547",  # от Химок до Владивостока
                    }
                }
            },
        )

        # Магазин в Химках, пользователь в Злынке (Брянская область)
        # проверяем правильное определение областного центра
        response = self.report.request_json("place=prime&text=ликвидация&debug=da&rids=20184")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "USER_REGION_ID": "20184",
                        "USER_REGION_LATITUDE": "52.42738342",
                        "USER_REGION_LONGITUDE": "31.73707581",
                        "USER_FEDSUBJ_ID": "10650",
                        "USER_FEDDISTRICT_ID": "3",
                        "USER_CHIEF_REGION_IS_MEGACITY": NoKey(
                            "USER_CHIEF_REGION_IS_MEGACITY"
                        ),  # Брянск - не город миллионник
                        "USER_DISTANCE_TO_CHIEF_REGION": "198786.8438",  # 198 км от Злынки до Брянска
                        "USER_DISTANCE_TO_MOSCOW": "527308.9375",  # 527 км от Злынки до Москвы
                        "USER_FROM_RUSSIA": "1",
                    }
                }
            },
        )

        # Магазин в Химках, пользователь в Москве
        # проверяем взаиморасположение пользователя и магазина
        # проверяем определение что Москва и центр Московской области (Москва) - город-миллионник
        # проверяем что ip-регион не совпадает с запрошенным регионом
        response = self.report.request_json("place=prime&text=ликвидация&debug=da&rids=213&ip-rids=191")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "USER_REGION_ID": "213",
                        "USER_REGION_IS_MEGACITY": "1",
                        "USER_CHIEF_REGION_IS_MEGACITY": "1",
                        "USER_FROM_RUSSIA": "1",
                        "USER_REGION_EQUAL_IP_REGION": NoKey("USER_REGION_EQUAL_IP_REGION"),
                        "SHOP_IN_USER_REGION": NoKey("SHOP_IN_USER_REGION"),
                        "SHOP_IN_USER_FEDSUBJ": "1",
                        "SHOP_IN_USER_FEDDISTR": "1",
                        "SHOP_IN_USER_COUNTRY": "1",
                        "DIST_BETW_USER_AND_SHOP_REGIONS": "8724.118164",
                    }
                }
            },
        )

        # Магазин в Москве, пользователь в Брянске
        response = self.report.request_json("place=prime&text=регистрация&debug=da&rids=191&ip-rids=191")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "SHOP_REGION_ID": "213",
                        "SHOP_REGION_IS_MEGACITY": "1",
                        "SHOP_CHIEF_REGION_IS_MEGACITY": "1",
                        "USER_REGION_ID": "191",
                        "USER_REGION_IS_MEGACITY": NoKey("USER_REGION_IS_MEGACITY"),  # Брянск не город миллионник
                        "USER_REGION_EQUAL_IP_REGION": "1",
                    }
                }
            },
        )

        # Магазин на Кипре, пользователь в Брянске
        response = self.report.request_json("place=prime&text=купить+оффшор&debug=da&rids=191")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "USER_REGION_ID": "191",
                        "USER_FEDSUBJ_ID": "10650",
                        "USER_FEDDISTRICT_ID": "3",
                        "USER_DISTANCE_TO_CHIEF_REGION": NoKey("USER_DISTANCE_TO_CHIEF_REGION"),  # 0 km
                        "USER_DISTANCE_TO_MOSCOW": "346671.9688",
                        "USER_FROM_RUSSIA": "1",
                        "SHOP_REGION_ID": "21242",
                        "SHOP_REGION_LATITUDE": "34.77118301",
                        "SHOP_REGION_LONGITUDE": "32.42612839",
                        "SHOP_FEDSUBJ_ID": "105225",
                        "SHOP_DISTANCE_TO_MOSCOW": "2373176.75",
                        "SHOP_FROM_RUSSIA": NoKey("SHOP_FROM_RUSSIA"),
                        "SHOP_IN_USER_REGION": NoKey("SHOP_IN_USER_REGION"),
                        "SHOP_IN_USER_FEDSUBJ": NoKey("SHOP_IN_USER_FEDSUBJ"),
                        "SHOP_IN_USER_FEDDISTR": NoKey("SHOP_IN_USER_FEDDISTR"),
                        "SHOP_IN_USER_COUNTRY": NoKey("SHOP_IN_USER_COUNTRY"),
                        "DIST_BETW_USER_AND_SHOP_REGIONS": "2061708.125",
                    }
                }
            },
        )

        # Магазин на Кипре, пользователь на Кипре
        response = self.report.request_json("place=prime&text=купить+оффшор&debug=da&rids=21242")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "USER_REGION_ID": "21242",
                        "USER_FROM_RUSSIA": NoKey("USER_FROM_RUSSIA"),
                        "SHOP_REGION_ID": "21242",
                        "SHOP_IN_USER_REGION": "1",
                    }
                }
            },
        )

        # Неверно заданный регион пользователя
        response = self.report.request_json("place=prime&text=ликвидация&debug=da&rids=12345")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "USER_REGION_IS_UNKNOWN": "1",
                        "USER_DISTANCE_TO_CHIEF_REGION": "20000000",
                        "USER_DISTANCE_TO_MOSCOW": "20000000",
                        "DIST_BETW_USER_AND_SHOP_REGIONS": "20000000",
                    }
                }
            },
        )
        self.error_log.ignore('Region with ID 12345 is not valid')

        # Не определен регион магазина (например для модели)
        response = self.report.request_json("place=prime&text=готовый+бизнес&debug=da&rids=213")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "SHOP_REGION_IS_UNKNOWN": "1",
                        "SHOP_DISTANCE_TO_CHIEF_REGION": "20000000",
                        "SHOP_DISTANCE_TO_MOSCOW": "20000000",
                        "DIST_BETW_USER_AND_SHOP_REGIONS": "20000000",
                    }
                }
            },
        )

    def test_category_factors(self):
        """Проверяем определение факторов связанных с hid"""

        response = self.report.request_json("place=prime&text=ликвидация&debug=da&rids=213")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "CATEGORY_PARENT_1_ID": "1",
                        "CATEGORY_PARENT_2_ID": "11",
                        "CATEGORY_ID": "1111",
                        "CATEGORY_POPULARITY": "0.6399999857",
                    }
                }
            },
        )

        response = self.report.request_json("place=prime&text=купить+оффшор&debug=da&rids=75")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "CATEGORY_PARENT_1_ID": "1",
                        "CATEGORY_PARENT_2_ID": "11",
                        "CATEGORY_ID": "112",
                        "CATEGORY_CONVERSION": "0.03999999911",
                    }
                }
            },
        )
        self.assertFalse("CATEGORY_POPULARITY" in str(response))

        response = self.report.request_json("place=prime&text=купить+оффшор&debug=da&rids=213")
        self.assertFragmentIn(response, {"debug": {"factors": {"CATEGORY_CONVERSION": "0.05000000075"}}})

        response = self.report.request_json("place=prime&text=покончить+с+конкурентами&debug=da&rids=213")
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "CATEGORY_PARENT_1_ID": "1",
                        "CATEGORY_PARENT_2_ID": "12",
                        "CATEGORY_ID": "12",
                        "CATEGORY_CONVERSION": "0.03999999911",
                    }
                }
            },
        )

    def test_shop_factors(self):
        """Факторы связанные с магазином оффера"""

        response = self.report.request_json("place=prime&text=ликвидация&debug=da&rids=75")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Ликвидация ООО - опытные юристы в Химках"},
                        "shop": {"id": 1},
                        "debug": {
                            "factors": {
                                "SHOP_ID": "1",
                                "SHOP_RATING": "4",
                            }
                        },
                    }
                ]
            },
        )

        response = self.report.request_json("place=prime&text=регистрация&debug=da&rids=213")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Регистрация ООО - опытные юристы из Москвы в вашем городе"},
                        "shop": {"id": 2},
                        "debug": {
                            "factors": {
                                "SHOP_ID": "2",
                                "SHOP_RATING": "3",
                            }
                        },
                    }
                ]
            },
        )

    def test_model_factors(self):
        """Факторы считаемые для модели"""

        response = self.report.request_json('place=prime&text=готовый+бизнес&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "IS_MODEL": "1",
                        "CLICK_COUNT": "10",
                        "MODEL_POPULARITY": "24",
                        "GURU_POPULARITY": "1",
                        "MODEL_CONVERSION": "0.06000000238",
                        "MODEL_LIFETIME": "9",
                    }
                }
            },
        )

        response = self.report.request_json('place=prime&text=купить+оффшор&rids=213&debug=da')
        self.assertFragmentIn(response, {"debug": {"factors": {"MODEL_CONVERSION": "0.05000000075"}}})
        self.assertTrue("MODEL_LIFETIME" not in str(response))

    def test_model_stat_factors(self):
        """Факторы считаемые по model_regional_stats модели"""

        response = self.report.request_json('place=prime&text=готовый+бизнес&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "NUMBER_OFFERS": "25",
                        "RETAILER_COUNT": "7",
                        "TOTAL_RETAILER_COUNT": "20",
                        "TOTAL_NUMBER_OFFERS": "130",
                        "NUMBER_OFFERS_TO_TOTAL_NUMBER_OFFERS": Round(float(25) / 130),
                        "BLOCK_NUMBER_OFFERS": "5",
                    }
                }
            },
        )

        response = self.report.request_json('place=prime&text=готовый+бизнес&rids=2&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "IS_MODEL": "1",
                        "NUMBER_OFFERS": "3",
                        "RETAILER_COUNT": "1",
                        "TOTAL_RETAILER_COUNT": "20",
                        "TOTAL_NUMBER_OFFERS": "130",
                        "NUMBER_OFFERS_TO_TOTAL_NUMBER_OFFERS": Round(float(3) / 130),
                        "BLOCK_NUMBER_OFFERS": "3",
                    }
                }
            },
        )

    @classmethod
    def prepare_data_for_query_factors(cls):

        cls.reqwizard.on_default_request().respond()
        cls.reqwizard.on_request('купить эскорт услуги девушек от 180 недорого').respond(
            # dnorm: 180 девушка услуга эскорт
            qtree='cHicnZRNbExRFMfPue-1bq_RvIxUxhMxRqNPQwyrRkKpiYiFVG3kLRDtxEiTSqZI042pRhTRSoqFSHylZaOtlqQGJWKB1ZsVFuwsWFlYWbn3vo95X2j7Nu9-_M955_z-9122jyXoMq0-BWk0SBaSoEMGmmErbEtQ0ICvgwFZ2Fmzt6YdDsFRKOBVhJsI9xCmEV4i8OcdgoVd-gPC8swOozxMpEtUrlQGrLI1VylVzumYzmDaSV7rSw57QSQvDL984mUPRIa-lIUWbJtBihroAV0GDMxi-31iV1q8jCywn5Lfa4EOZej1B5M8nDDJ2MQxXooOxvJjlL-JNWUozuiFN5qVI6KTynl3ZD3lI6ahrlpz1rRcRS9GKAfdtcplQylAH_YTiiUE3qBuEdYVITXI63xeGbRmrNn_kvo6UVcF5QuMA_XFBeXThUGNCFDV_SkfqFcC1KRJxgWsSQ4LOSxlEbBsQLMctIvtmYdo2I9NwCo2BgqaTYmCRUHg2lWgfeBD-oWwEyGk9dYT66k1zdMM8Xc5AHVJDNSpz9e94xeKjeP6zeYaUobJrmchQXwnxVEMCMtBD947HkxydoSz43kkRzEuL8oFZT4u-PjeRnYwxJcHh35qEkO11OEylfooSbVthwQpt6v48Cjh-NYwuSxZYAvpwAtmlVuwwkFkB0IVKltasoECFVkgBgr80ewWKORxTjeKxnSxm1madR4-kZU6l2I_2FVkltGzWglTJK0YNdnINEFrNMZnqjNDjbh74kvmbUJpw5FyT06_QtihUDe1-d7Ok8VTOm7IoPGPy-HTzxVuR05IXFPj9vF1FA73UffY5pizkULnFOJFk9yZMfFx8L7Md_FDU8cPjXKi5zgfLtEUHXvl8SHF1miWoeiV29Qr44iM4wPQsclOwN0lDldVoDDAvKtwQofnOnP6iMLMKKHu-RC6dePrI-Jj1P03RsRl1B3DaBNzNnh39i9NhiZMHIt2V0AOY2OMfMzEKgzZuatujagXgE4kyEUTLNBBkWVtNEvJ9wdirEfjwqNVv_bl9GsKOxzyiHblz5zuLeS7Ay7R_7nkhcX59MD2ydOEndrMvK35ebUrJmCB_PfEpViEA-vi8oQ8iBcN_MMoVVMNcWVdwrVM2pZcSVFvoNjAPq7bvrox_XZHGjYJzFlPQ4Ka3d9_hzU8q6NRGs6_Od26GqDU6tMkZR41ySjtqKUkSfa324vEW0R7MeF8UaX1RbCnqlbrn1KN2lPe0B_yFFum'  # noqa
        )
        cls.index.offers += [
            Offer(
                title='Эскорт услуги',
                descr='Красивые и умные девушки сопроводят вас на встречах с партнерами'
                + '(исключительно блондинки с двумя образованиями, знанием иностранных языков, от 180 см, параметры 90-60-90)',
            )
        ]

    def test_dnorm_query_factors(self):
        response = self.report.request_json('place=prime&text=купить+эскорт+услуги+девушек+от+180+недорого&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "REQUEST_LENGTH": "44",
                        "REQUEST_ALL_LOWER": "1",
                        "REQUEST_CONTAINS_NUMBERS": "1",
                        "REQUEST_SYMBOLS_COUNT": "38",
                        "REQUEST_NUMBERS_COUNT": "3",
                        "REQUEST_DIGS_COUNT": "3",
                        "REQUEST_DIGS_RATIO": Round(3.0 / 38),
                        "REQUEST_CYRS_RATIO": Round(35.0 / 38),
                        "REQUEST_NUMS_COUNT": "1",
                        "REQUEST_WORDS_COUNT": "7",
                        "REQUEST_CYR_ONLY": "1",
                        "REQUEST_DNORM_LENGTH": "25",
                        "REQUEST_DNORM_WORDS_COUNT": "4",
                        "REQUEST_DNORM_SYMBOLS_COUNT": "22",
                        "REQUEST_DNORM_CONTAINS_NUMBER": "1",
                        "REQUEST_DNORM_LENGTH": "25",
                        "REQUEST_DNORM_DIGS_COUNT": "3",
                        "REQUEST_DNORM_NUMS_COUNT": "1",
                        "REQUEST_DNORM_CYR_ONLY": "1",
                        "REQUEST_DNORM_CYRS_RATIO": Round(19.0 / 22),
                        "REQUEST_DNORM_DIGS_RATIO": Round(3.0 / 22),
                        "REQUEST_CONTAINS_CODE": NoKey("REQUEST_CONTAINS_CODE"),
                    }
                }
            },
        )

        response = self.report.request_json('place=prime&text=купить девушек 90-60-90&debug=da')
        self.assertFragmentIn(response, {"debug": {"factors": {"REQUEST_CONTAINS_CODE": "1"}}})

    def test_body_factors(self):

        response = self.report.request_json('place=prime&text=красивые девушки эскорт&debug=da')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'Эскорт услуги'},
                'description': Contains('Красивые и умные девушки сопроводят вас на встречах с партнерами'),
                'debug': {
                    'factors': {'BM15_W_FULL': NotEmpty(), 'BM15_W_BODY': NotEmpty(), 'BM15_W_TITLE': NotEmpty()}
                },
            },
        )

    @classmethod
    def prepare_data_for_dssm_factors(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=33332,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=1, day_to=2)])],
            )
        ]

        cls.index.offers += [
            Offer(
                ts=977771,
                waremd5='Q89ofo8ZbfkzCKzXUZsF6w',
                title='dssmoffer 1',
                url='shop.ru/offers/dssmoffer1',
                hid=1111,
            ),
            Offer(
                ts=977772, waremd5='xrNtYkw4g4_wt5zDHw7evw', title='dssmoffer 2', url='http://shop.ru/offers/dssmoffer2'
            ),
        ]
        cls.index.models += [
            Model(title='blue dssm model', hyperid=33331, hid=2401301),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=33333,
                title="blue dssm",
                hyperid=33331,
                delivery_buckets=[33332],
                ts=977773,
                blue_offers=[
                    BlueOffer(feedid=1, title="blue dssm offer", price=1000, waremd5='bluedssmoffer12345678w')
                ],
            ),
        ]

        cls.index.dssm.meta_multiclick.on(
            query='dssmoffer', url="http://shop.ru/offers/dssmoffer1", title="dssmoffer 1"
        ).set(0.3, 0.35, -0.1)
        cls.index.dssm.meta_multiclick.on(
            query='dssmoffer', url="http://shop.ru/offers/dssmoffer2", title="dssmoffer 2"
        ).set(-0.4, 0.13)

        cls.index.dssm.meta_dwelltime.on(
            query='dssmoffer', url="http://shop.ru/offers/dssmoffer1", title="dssmoffer 1"
        ).set(0.5)
        cls.index.dssm.meta_dwelltime.on(
            query='dssmoffer', url="http://shop.ru/offers/dssmoffer2", title="dssmoffer 2"
        ).set(-0.3)

        cls.index.dssm.query_embedding.on(query='dssmoffer').set(-0.5, -0.4, -0.3, -0.2)
        cls.index.dssm.query_embedding.on(query='suggest').set(-0.1, -0.9, -0.3, -0.5)
        cls.index.dssm.query_embedding.on(query='HID-1111').set(-0.2, -0.9, -0.1, -0.8)
        cls.index.dssm.query_embedding.on(query='Ликвидация предприятий').set(-0.4, -0.6, -0.8, -0.7)

        cls.index.dssm.hard2_query_embedding.on(query='suggest').set(0.1, 0.6, 0.2, -0.8)
        cls.index.dssm.hard2_query_embedding.on(query='HID-1111').set(0.6, 0.2, 0.4, -0.2)
        cls.index.dssm.hard2_query_embedding.on(query='Ликвидация предприятий').set(0.4, 0.6, 0.8, -0.7)

        cls.index.dssm.hard2_query_embedding.on(query='dssmoffer').set(0.05, 0.04, 0.03, -0.02)
        cls.index.dssm.reformulation_query_embedding.on(query='dssmoffer').set(0.55, 0.44, 0.33, -0.22)
        cls.index.dssm.bert_query_embedding.on(query='dssmoffer').set(0.8, 0.45, 0.87, -0.44)
        cls.index.dssm.bert_query_embedding.on(query='HID-1111').set(0.8, 0.45, 0.87, -0.44)

        # На входе суперэмбеда сконкатенированные дссм в порядке
        # metaDwelltime hard2 marketClick metaMulticlick reformulation bert
        cls.index.dssm.meta_dwelltime.on(
            query='dssmoffer3',
        ).set(0.5)
        cls.index.dssm.hard2_query_embedding.on(query='dssmoffer3').set(0.05, 0.04, 0.03, -0.02)
        cls.index.dssm.query_embedding.on(query='dssmoffer3').set(-0.5, -0.4, -0.3, -0.2)
        # эта модель нормализуется в коде: поэтому такие значения:
        cls.index.dssm.meta_multiclick.on(query='dssmoffer3').set(*[-336280] * 300)
        cls.index.dssm.reformulation_query_embedding.on(query='dssmoffer3').set(0.55, 0.44, 0.33, -0.22)
        cls.index.dssm.bert_query_embedding.on(query='dssmoffer3').set(0.8, 0.45, 0.87, -0.44)

        cls.index.dssm.pictures_query_embedding.on(query='dssmoffer3').set(0.3, 0.45, 0.87, -0.44)

        cls.index.dssm.assessment_binary_query_embedding.on(
            corrected_query='dssmoffer3', qfuf10='', full_region=''
        ).set(0.7, 0.6, 0.5)

        cls.index.dssm.super_embed_query_embedding.on(
            meta_dwelltime=[0.5],
            hard2=[0.05, 0.04, 0.03, -0.02],
            market_click=[-0.5, -0.4, -0.3, -0.2],
            meta_multiclick=[0] * 300,
            reformulation=[0.55, 0.44, 0.33, -0.22],
            bert=[0.8, 0.45, 0.87, -0.44],
        ).set(0.2, 0.3, 0.5)

        cls.index.dssm.category_model_stupid.on(corrected_query='dssmoffer3').set(0.7, 0.6, 0.5)

        cls.index.dssm.dssm_values_binary.on(ts=977771).set(-0.3, -0.2, -0.1)
        cls.index.dssm.dssm_values_binary.on(ts=977772).set(-0.05, -0.15)

        cls.index.dssm.hard2_dssm_values_binary.on(ts=977771).set(0.15, 0.45, -0.37, -0.3, -0.3)
        cls.index.dssm.hard2_dssm_values_binary.on(ts=977772).set(0.12, -0.06, 0.4, 0.4, 0.4)

        cls.index.dssm.reformulation_dssm_values_binary.on(ts=977771).set(0.2, 0.4, -0.23)
        cls.index.dssm.reformulation_dssm_values_binary.on(ts=977772).set(0.53, -0.19)

        cls.index.dssm.bert_dssm_values_binary.on(ts=977771).set(0.1, 0.8, 0.34)
        cls.index.dssm.bert_dssm_values_binary.on(ts=977772).set(0.87, -0.2, -0.5)

        cls.index.dssm.assessment_binary_values_binary.on(ts=977771).set(0.1, 0.8, 0.34)
        cls.index.dssm.assessment_binary_values_binary.on(ts=977772).set(0.87, -0.2, -0.5)

        cls.index.dssm.super_embed_dssm_values_binary.on(ts=977771).set(0.1, 0.2, 0.3)
        cls.index.dssm.super_embed_dssm_values_binary.on(ts=977772).set(0.2, 0.2, 0.2)

        cls.index.dssm.pictures_dssm_values_binary.on(ts=977771).set(0.1, 0.2, 0.3, 0.15)
        cls.index.dssm.pictures_dssm_values_binary.on(ts=977772).set(0.2, 0.2, 0.2, 0.324)

    def test_dssm_factors(self):
        # Значение факторов DSSM_MARKET_CLICK и DSSM_HARD2
        # вычисляется на основе скалярного произведения факторов запроса и документа.
        # Зависит и от оффера и от запроса и отличается для разных офферов и для разных запросов.
        # Фактор запроса рассчитывается на основе параметров (в порядке приоритета): text, suggest_text, hid name|uniq_name.
        response = self.report.request_json('place=prime&text=dssmoffer&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 2'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.6666),
                                "DSSM_HARD2": Round(0.6697),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.7064),
                                "DSSM_HARD2": Round(0.6567),
                            }
                        },
                    },
                ]
            },
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Embedding for query [dssmoffer]")]})

        response = self.report.request_json('place=prime&text=dssmoffer+1+2&suggest_text=suggest&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 2'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.659),
                                "DSSM_HARD2": Round(0.686),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.647),
                                "DSSM_HARD2": Round(0.65),
                            }
                        },
                    },
                ]
            },
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Embedding for query [dssmoffer 1 2]")]})

        response = self.report.request_json('place=prime&suggest_text=suggest&hid=1111&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.6975),
                                "DSSM_HARD2": Round(0.736),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'Ликвидация ООО - опытные юристы в Химках'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.6102),
                                "DSSM_HARD2": Round(0.664),
                            }
                        },
                    },
                ]
            },
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Embedding for query [suggest]")]})

        response = self.report.request_json('place=prime&suggest_text=&hid=1111&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.6937),
                                "DSSM_HARD2": Round(0.673),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'Ликвидация ООО - опытные юристы в Химках'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.604),
                                "DSSM_HARD2": Round(0.684),
                            }
                        },
                    },
                ]
            },
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Embedding for query [HID-1111]")]})

        response = self.report.request_json(
            'place=prime&suggest_text=&hid=1111&debug=da' '&rearr-factors=market_use_category_uniq_name_in_dssm=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.7079),
                                "DSSM_HARD2": Round(0.703),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'Ликвидация ООО - опытные юристы в Химках'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.5875),
                                "DSSM_HARD2": Round(0.686),
                            }
                        },
                    },
                ]
            },
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Embedding for query [Ликвидация предприятий]")]})

    def test_bert_dssm(self):
        """
        Проверяем что на запрос фомируются факторы DSSM_BERT
        """

        response = self.report.request_json('place=prime&text=dssmoffer&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_BERT": Round(0.7685),
                                "HEAD_AVG_DSSM_BERT": Round((0.7685 + 0.6721) / 2),
                                "BERT_CHEBYSHEV": NotEmpty(),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'dssmoffer 2'},
                        'debug': {
                            'factors': {
                                "DSSM_BERT": Round(0.6721),
                                "HEAD_AVG_DSSM_BERT": Round((0.7685 + 0.6721) / 2),
                                "BERT_CHEBYSHEV": NotEmpty(),
                            }
                        },
                    },
                ]
            },
        )

        response = self.report.request_json('place=prime&debug=da&hid=1111')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_BERT": Round(0.7685),
                            }
                        },
                    },
                ]
            },
        )

    def test_head_avg_factors(self):
        """Проверяем что считаются факторы HEAD_AVG_* - аггрегированные факторы по переранжируемым документам"""

        response = self.report.request_json('place=prime&text=dssmoffer&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 2'},
                        'debug': {
                            'factors': {
                                "DSSM_HARD2": Round(0.67),
                                "HEAD_AVG_DSSM_HARD2": Round((0.67 + 0.657) / 2),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_HARD2": Round(0.657),
                                "HEAD_AVG_DSSM_HARD2": Round((0.67 + 0.657) / 2),
                            }
                        },
                    },
                ]
            },
        )

    def test_dssm_hard2_factors(self):
        """Вычисляются модели DSSM_HARD2 и DSSM_REFORMULATION вместо DSSM_HARD"""

        response = self.report.request_json('place=prime&text=dssmoffer&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_HARD": Absent(),
                                "HARD_CHEBYSHEV": Absent(),
                                "HARD_MAHALANOBIS": Absent(),
                                "HEAD_AVG_DSSM_HARD": Absent(),
                                "DOC_EMBEDDING_HARD2_0": Round(0.15, 2),
                                "DOC_EMBEDDING_HARD2_1": Round(0.45, 2),
                                "DOC_EMBEDDING_HARD2_2": Round(-0.37, 2),
                                "DSSM_HARD2": Round(0.6567),
                                "HARD2_CHEBYSHEV": NotEmpty(),
                                "HEAD_AVG_DSSM_HARD2": Round((0.6697 + 0.6567) / 2),
                                "DOC_EMBEDDING_REFORMULATION_0": Round(0.2, 2),
                                "DOC_EMBEDDING_REFORMULATION_1": Round(0.4, 2),
                                "DOC_EMBEDDING_REFORMULATION_2": Round(-0.23, 2),
                                "DSSM_REFORMULATION": Round(0.6954),
                                "REFORMULATION_CHEBYSHEV": NotEmpty(),
                                "HEAD_AVG_DSSM_REFORMULATION": Round((0.6954 + 0.7013) / 2),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'dssmoffer 2'},
                        'wareId': 'xrNtYkw4g4_wt5zDHw7evw',
                        'debug': {
                            'factors': {
                                "DSSM_HARD": Absent(),
                                "HARD_CHEBYSHEV": Absent(),
                                "HARD_MAHALANOBIS": Absent(),
                                "HEAD_AVG_DSSM_HARD": Absent(),
                                "DOC_EMBEDDING_HARD2_0": Round(0.12, 2),
                                "DOC_EMBEDDING_HARD2_1": Round(-0.06, 2),
                                "DSSM_HARD2": Round(0.6697),
                                "HARD2_CHEBYSHEV": NotEmpty(),
                                "HEAD_AVG_DSSM_HARD2": Round((0.6697 + 0.6567) / 2),
                                "DOC_EMBEDDING_REFORMULATION_0": Round(0.53, 2),
                                "DOC_EMBEDDING_REFORMULATION_1": Round(-0.19, 2),
                                "DSSM_REFORMULATION": Round(0.7013),
                                "REFORMULATION_CHEBYSHEV": NotEmpty(),
                                "HEAD_AVG_DSSM_REFORMULATION": Round((0.6954 + 0.7013) / 2),
                            }
                        },
                    },
                ]
            },
        )

        self.feature_log.expect(
            ware_md5='xrNtYkw4g4_wt5zDHw7evw',
            dssm_hard=Absent(),
            dssm_hard2=Round(0.6697),
            dssm_reformulation=Round(0.7013),
            doc_embedding_hard2_0=Round(0.12, 2),
            doc_embedding_reformulation_0=Round(0.53, 2),
        ).once()

    def test_use_dssm_hard_on_blue_textless_by_default(self):
        response = self.report.request_json('place=prime&rids=213&hid=2401301&market-sku=33333&debug=da&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "offers": {
                            "items": [
                                {
                                    'titles': {'raw': 'blue dssm offer'},
                                    'debug': {
                                        'factors': {
                                            "DSSM_HARD": NoKey("DSSM_HARD"),
                                            "DSSM_HARD2": NotEmpty(),
                                        }
                                    },
                                },
                            ],
                        },
                    },
                ]
            },
        )
        self.feature_log.expect(ware_md5='bluedssmoffer12345678w', dssm_hard=NoKey('dssm_hard'), dssm_hard2=NotEmpty())

    def test_use_dssm_hard2_on_blue_text_by_default(self):
        response = self.report.request_json('place=prime&rids=213&text=blue dssm&debug=da&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "offers": {
                            "items": [
                                {
                                    'titles': {'raw': 'blue dssm offer'},
                                    'debug': {
                                        'factors': {
                                            "DSSM_HARD": NoKey("DSSM_HARD"),
                                            "DSSM_HARD2": NotEmpty(),
                                        }
                                    },
                                },
                            ],
                        },
                    },
                ]
            },
        )
        self.feature_log.expect(ware_md5='bluedssmoffer12345678w', dssm_hard=NoKey('dssm_hard'), dssm_hard2=NotEmpty())

    @classmethod
    def prepare_dssm_factors_on_collapsed_document(cls):
        cls.index.hypertree += [
            HyperCategory(hid=97, output_type=HyperCategoryType.GURU, uniq_name='collapsedcategory')
        ]
        cls.index.models += [Model(ts=977773, hyperid=977773, hid=97, title="collapsedModel")]
        cls.index.offers += [
            Offer(
                ts=977774,
                hyperid=977773,
                hid=97,
                title="collapsedOffer",
                url="http://shop.ru/offers/collapsedOffer",
                waremd5="qtZDmKlp7DGGgA1BL6erMQ",
            )
        ]

        cls.index.dssm.meta_multiclick.on(query='collapsedOffer').set(0.25, 0.26, 0.27, 0.28, 0.29)
        cls.index.dssm.meta_multiclick.on(
            query='collapsedOffer', url="http://market.yandex.ru/product/977773", title="collapsedModel"
        ).set(0.55, 0.56, 0.57, 0.58, 0.59)
        cls.index.dssm.meta_multiclick.on(url="http://market.yandex.ru/product/977773", title="collapsedModel").set(
            -0.45, -0.46, -0.47, -0.48, -0.49
        )
        cls.index.dssm.meta_multiclick.on(
            query='collapsedOffer', url="http://shop.ru/offers/collapsedOffer", title="collapsedOffer"
        ).set(0.35, 0.36, 0.37, 0.38, 0.39)
        cls.index.dssm.meta_multiclick.on(url="http://shop.ru/offers/collapsedOffer", title="collapsedOffer").set(
            -0.25, -0.26, -0.27, -0.28, -0.29
        )

        cls.index.dssm.query_embedding.on(query='collapsedOffer').set(0.4, 0.6, -0.99)
        cls.index.dssm.dssm_values_binary.on(ts=977773).set(0.25, 0.67, 0.78)
        cls.index.dssm.dssm_values_binary.on(ts=977774).set(-0.1, -0.65, -0.34)

        cls.index.dssm.category_model_by_queries.on(query='collapsedoffer').set(0.3, 0.3, 0.3)
        cls.index.dssm.category_model_by_queries.on(query='collapsedOffer').set(0.3, 0.3, 0.3)
        cls.index.dssm.category_model_by_queries.on(hid='97').set(-0.3, -0.3, -0.3)
        cls.index.dssm.category_model_by_queries.on(query='collapsedOffer', hid='97').set(0.23)
        cls.index.dssm.category_model_by_queries.on(query='collapsedoffer', hid='97').set(0.23)

        cls.index.dssm.category_model_by_cat_uniqname.on(query='collapsedOffer').set(*[0.45] * 50)
        cls.index.dssm.category_model_by_cat_uniqname.on(category_name='collapsedcategory').set(*[0.45] * 50)
        cls.index.dssm.category_model_by_cat_uniqname.on(query='collapsedOffer', category_name='collapsedcategory').set(
            0.8
        )
        cls.index.dssm.category_model_by_cat_uniqname.on(query='collapsedoffer', category_name='collapsedcategory').set(
            0.8
        )

        cls.index.dssm.category_model_by_cat_uniqname_blue.on(query='collapsedOffer').set(*[0.24] * 50)
        cls.index.dssm.category_model_by_cat_uniqname_blue.on(category_name='collapsedcategory').set(*[0.24] * 50)
        cls.index.dssm.category_model_by_cat_uniqname_blue.on(
            query='collapsedOffer', category_name='collapsedcategory'
        ).set(0.58)
        cls.index.dssm.category_model_by_cat_uniqname_blue.on(
            query='collapsedoffer', category_name='collapsedcategory'
        ).set(0.58)

    def test_dssm_factors_on_collapsed_document(self):
        '''Проверяем что CLPS_* факторы по метаформуле считаются и по модели
        Проверяем что считаются факторы-метрики расстояний CBQ_EUQLID, HARD_MAHALANOBIS и т.п.
        '''

        response = self.report.request_json('place=prime&text=collapsedOffer&hid=97&allow-collapsing=1&debug=da')
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'titles': {'raw': 'collapsedModel'},
                'debug': {
                    'isCollapsed': True,
                    'factors': {
                        # факторы по различным метрикам над dssm-ками
                        'CLICK_CHEBYSHEV': NotEmpty(),
                        'CLICK_MAHALANOBIS': NotEmpty(),
                        'HARD2_CHEBYSHEV': NotEmpty(),
                        'HEAD_AVG_DSSM_HARD2': NotEmpty(),
                        # факторы наследуемые от категории
                        'CBQ_CHEBYSHEV': NotEmpty(),
                        'CBQ_EUQLID': NotEmpty(),
                        'CBQ_MAHALANOBIS': NotEmpty(),
                        #
                        "HEAD_AVG_CATEGORY_DSSM_BY_QUERIES": Round(0.7058),  # avg NormDotProduct
                        "CATEGORY_DSSM_BY_QUERIES": Round(0.7058),  # NormDotProduct(qEmb, dEmb)
                        "DSSM_BY_QUERIES": Round(0.7058),  # same
                        "HEAD_AVG_CATEGORY_DSSM_BY_UNIQNAME": Round(0.955),  # avg NormDotProduct
                        "CATEGORY_DSSM_BY_UNIQNAME": Round(0.955),  # NormDotProduct(qEmb, dEmb)
                        "DSSM_BY_UNIQNAME": Round(0.846),  # same
                        # факторы по синей дссм-ке теперь считаются и на белом
                        "HEAD_AVG_CATEGORY_DSSM_BY_UNIQNAME_BLUE": NotEmpty(),  # avg NormDotProduct
                        "CATEGORY_DSSM_BY_UNIQNAME_BLUE": NotEmpty(),  # NormDotProduct(qEmb, dEmb)
                        "DSSM_BY_UNIQNAME_BLUE": NotEmpty(),
                    },
                },
            },
        )

    @staticmethod
    def get_embeddings(indices, dimension):
        embeddings = [-0.5] * dimension
        for n, idx in enumerate(indices):
            if idx < dimension:
                embeddings[idx] = (n + 1) / 100.0

        return embeddings

    @classmethod
    def prepare_doc_embeddings_tests(cls):
        """
        set up for tests:
        * test_doc_embeddings_factors
        * test_doc_embeddings_factors_multiclick_and_dwelltime
        * test_error_if_not_enough_dwelltime_doc_embedding_factors
        * test_error_if_not_enough_multiclick_doc_embedding_factors
        """
        cls.crypta.on_request_profile(yandexuid='1').respond(
            features=[
                CryptaSearchQueriesFeature('blablabla'),
                CryptaSearchQueriesFeature('papapam'),
                CryptaSearchQueriesFeature('tadam'),
            ]
        )

        cls.index.offers += [
            Offer(
                ts=1234567,
                hyperid=1234567,
                title='dssmembeddingsoffer',
                url='http://shop.ru/offers/dssmembeddingsoffer',
                hid=4321,
            ),
        ]

        cls.index.dssm.dssm_values_binary.on(ts=1234567).set(
            *cls.get_embeddings(DOC_EMBEDDINGS_FACTORS_INDICES['marketclick'], 50)
        )
        cls.index.dssm.hard2_dssm_values_binary.on(ts=1234567).set(
            *cls.get_embeddings(DOC_EMBEDDINGS_FACTORS_INDICES['hard2'], 50)
        )

    def test_doc_embeddings_factors(self):
        def check_embeddings_factors(response, indices, name):
            factors = {
                'DOC_EMBEDDING_{name}_{num}'.format(name=name, num=idx): Round((n + 1) / 100.0, 2)
                for n, idx in enumerate(indices)
            }
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'titles': {'raw': 'dssmembeddingsoffer'},
                            'debug': {'factors': factors},
                        }
                    ]
                },
                preserve_order=True,
            )

        response = self.report.request_json(
            'place=prime&yandexuid=1&debug=da&text=dssmembeddingsoffer query'
            '&rearr-factors=market_enable_speedups_for_metadoc_search=0'
        )
        for name, indices in DOC_EMBEDDINGS_FACTORS_INDICES.iteritems():
            check_embeddings_factors(response, indices, name.upper())

    @classmethod
    def prepare_request_dssm_clusters(cls):
        cls.index.offers += [Offer(title='request_dssm_clusters')]

    def test_request_dssm_clusters(self):
        """Запросные факторы: расстояния до центров кластеров, построенных на запросных
        dssm-эмбеддингах

        https://st.yandex-team.ru/MARKETOUT-16402
        """

        response = self.report.request_json('place=prime&text=request_dssm_clusters&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "HARD_QUERY_TOYS": NotEmpty(),
                        "HARD_QUERY_BEAUTY": NotEmpty(),
                        "HARD_QUERY_FIND_SHOP": NotEmpty(),
                        "HARD_QUERY_PRESENTS": NotEmpty(),
                        "HARD_QUERY_CARS_BIG_ACCESSORIES": NotEmpty(),
                        "HARD_QUERY_PRINTERS": NotEmpty(),
                        "HARD_QUERY_CARS_SMALL_ACCESSORIES": NotEmpty(),
                        "HARD_QUERY_NEWBORN": NotEmpty(),
                        "HARD_QUERY_HEATING_AND_AIRING": NotEmpty(),
                        "HARD_QUERY_AUDIO": NotEmpty(),
                        "HARD_QUERY_PHONES": NotEmpty(),
                        "HARD_QUERY_HOUSEHOLD_EQUIPMENT": NotEmpty(),
                        "HARD_QUERY_TIRES": NotEmpty(),
                        "MODEL_BY_QUERIES_SANITARY_EQUIPMENT": NotEmpty(),
                        "MODEL_BY_QUERIES_HOUSE_RENOVATION": NotEmpty(),
                        "MODEL_BY_QUERIES_LAPTOPS": NotEmpty(),
                        "MODEL_BY_QUERIES_PHONES": NotEmpty(),
                        "MODEL_BY_QUERIES_MEDICINE": NotEmpty(),
                        "MODEL_BY_QUERIES_COSMETIC": NotEmpty(),
                        "MODEL_BY_QUERIES_AUTOPARTS": NotEmpty(),
                    }
                }
            },
        )

        response = self.report.request_json(
            'place=prime&text=request_dssm_clusters&debug=da&rearr-factors=market_disable_request_hard_dssm_features=1'
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "HARD_QUERY_TOYS": Absent(),
                        "HARD_QUERY_BEAUTY": Absent(),
                        "HARD_QUERY_FIND_SHOP": Absent(),
                        "HARD_QUERY_PRESENTS": Absent(),
                        "HARD_QUERY_CARS_BIG_ACCESSORIES": Absent(),
                        "HARD_QUERY_PRINTERS": Absent(),
                        "HARD_QUERY_CARS_SMALL_ACCESSORIES": Absent(),
                        "HARD_QUERY_NEWBORN": Absent(),
                        "HARD_QUERY_HEATING_AND_AIRING": Absent(),
                        "HARD_QUERY_AUDIO": Absent(),
                        "HARD_QUERY_PHONES": Absent(),
                        "HARD_QUERY_HOUSEHOLD_EQUIPMENT": Absent(),
                        "HARD_QUERY_TIRES": Absent(),
                        "MODEL_BY_QUERIES_SANITARY_EQUIPMENT": NotEmpty(),
                        "MODEL_BY_QUERIES_HOUSE_RENOVATION": NotEmpty(),
                        "MODEL_BY_QUERIES_LAPTOPS": NotEmpty(),
                        "MODEL_BY_QUERIES_PHONES": NotEmpty(),
                        "MODEL_BY_QUERIES_MEDICINE": NotEmpty(),
                        "MODEL_BY_QUERIES_COSMETIC": NotEmpty(),
                        "MODEL_BY_QUERIES_AUTOPARTS": NotEmpty(),
                    }
                }
            },
        )

    @classmethod
    def prepare_request_reformulation_dssm_clusters(cls):
        cls.index.offers += [Offer(title='request_reformulation_dssm_clusters')]

    def test_request_reformulation_dssm_clusters(self):
        """Запросные факторы: расстояния до центров кластеров, построенных на запросных
        reformulation-dssm-эмбеддингах

        https://st.yandex-team.ru/MARKETOUT-36141
        """

        response = self.report.request_json('place=prime&text=request_reformulation_dssm_clusters&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "REFORMULATION_QUERY_FOOD": NotEmpty(),
                        "REFORMULATION_QUERY_PHONES_MODELS": NotEmpty(),
                        "REFORMULATION_QUERY_BEAUTY": NotEmpty(),
                        "REFORMULATION_QUERY_LAPTOPS": NotEmpty(),
                        "REFORMULATION_QUERY_SPORTS_EQUIPMENT": NotEmpty(),
                        "REFORMULATION_QUERY_COMPUTER_HARDWARE": NotEmpty(),
                        "REFORMULATION_QUERY_HEATING_AND_AIRING": NotEmpty(),
                        "REFORMULATION_QUERY_DRUGS": NotEmpty(),
                        "REFORMULATION_QUERY_ENTERTAINMENT": NotEmpty(),
                        "REFORMULATION_QUERY_CLOTHES": NotEmpty(),
                        "REFORMULATION_QUERY_AUTO": NotEmpty(),
                        "REFORMULATION_QUERY_CONSTRUCTION": NotEmpty(),
                        "REFORMULATION_QUERY_HOUSEHOLD_EQUIPMENT": NotEmpty(),
                        "REFORMULATION_QUERY_PHONES_BARGAIN": NotEmpty(),
                        "REFORMULATION_QUERY_FURNITURE": NotEmpty(),
                    }
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "categories_ranking_json": [
                        {
                            "factors": {
                                "REFORMULATION_QUERY_FOOD": NotEmpty(),
                                "REFORMULATION_QUERY_PHONES_MODELS": NotEmpty(),
                                "REFORMULATION_QUERY_BEAUTY": NotEmpty(),
                                "REFORMULATION_QUERY_LAPTOPS": NotEmpty(),
                                "REFORMULATION_QUERY_SPORTS_EQUIPMENT": NotEmpty(),
                                "REFORMULATION_QUERY_COMPUTER_HARDWARE": NotEmpty(),
                                "REFORMULATION_QUERY_HEATING_AND_AIRING": NotEmpty(),
                                "REFORMULATION_QUERY_DRUGS": NotEmpty(),
                                "REFORMULATION_QUERY_ENTERTAINMENT": NotEmpty(),
                                "REFORMULATION_QUERY_CLOTHES": NotEmpty(),
                                "REFORMULATION_QUERY_AUTO": NotEmpty(),
                                "REFORMULATION_QUERY_CONSTRUCTION": NotEmpty(),
                                "REFORMULATION_QUERY_HOUSEHOLD_EQUIPMENT": NotEmpty(),
                                "REFORMULATION_QUERY_PHONES_BARGAIN": NotEmpty(),
                                "REFORMULATION_QUERY_FURNITURE": NotEmpty(),
                            }
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_request_bert_dssm_clusters(cls):
        cls.index.offers += [Offer(title='пылесос-газонокосилка')]
        cls.index.dssm.bert_query_embedding.on(query='нож для газонокосилки sterwins 340 ep 3 34 см').set(
            -0.02,
            -0.01,
            0.05,
            -0.02,
            0.01,
            -0.06,
            -0.03,
            0.08,
            -0.01,
            0.06,
            -0.17,
            0.01,
            0.03,
            -0.02,
            -0.06,
            -0.1,
            0.04,
            0.06,
            0.05,
            0.09,
            -0.03,
            -0.02,
            0.07,
            -0.13,
            0.05,
            0.03,
            -0.0,
            0.07,
            0.12,
            0.04,
            -0.0,
            -0.06,
            0.04,
            -0.05,
            -0.03,
            -0.1,
            0.06,
            -0.0,
            0.01,
            -0.05,
            -0.04,
            0.03,
            0.02,
            0.03,
            -0.03,
            -0.07,
            -0.91,
            0.07,
            0.02,
            -0.05,
        )
        cls.index.dssm.bert_query_embedding.on(query='пылесос с чашей').set(
            -0.04,
            0.03,
            -0.12,
            -0.09,
            0.08,
            -0.05,
            0.08,
            0.05,
            -0.0,
            -0.03,
            -0.19,
            -0.18,
            0.12,
            0.12,
            -0.11,
            0.08,
            0.08,
            0.1,
            0.09,
            -0.03,
            0.11,
            -0.04,
            -0.08,
            -0.13,
            0.06,
            -0.01,
            -0.01,
            0.04,
            -0.05,
            0.01,
            -0.06,
            0.03,
            -0.08,
            -0.14,
            -0.1,
            -0.2,
            0.05,
            0.09,
            -0.08,
            -0.09,
            -0.08,
            -0.01,
            0.12,
            -0.0,
            -0.03,
            0.01,
            -0.8,
            0.03,
            -0.0,
            -0.05,
        )

    def test_request_bert_dssm_clusters(self):
        """Запросные факторы: близость запроса к кластерам на основе dssm bert

        https://st.yandex-team.ru/ECOMQUALITY-23
        """

        response = self.report.request_json('place=prime&text=нож+для+газонокосилки+sterwins+340+ep+3+34+см&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {"BERT_DSSM_QUERY_LAWNMOVER": Round(1.0, 2), "BERT_DSSM_VACUUM_CLEANER": NotEmpty()}
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "categories_ranking_json": [
                        {
                            "factors": {
                                "BERT_DSSM_QUERY_LAWNMOVER": Round(1.0, 2),
                                "BERT_DSSM_VACUUM_CLEANER": NotEmpty(),
                            }
                        }
                    ]
                }
            },
        )

        response = self.report.request_json('place=prime&text=пылесос+с+чашей&debug=da')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {"BERT_DSSM_QUERY_LAWNMOVER": NotEmpty(), "BERT_DSSM_VACUUM_CLEANER": Round(1.0, 2)}
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "categories_ranking_json": [
                        {
                            "factors": {
                                "BERT_DSSM_QUERY_LAWNMOVER": NotEmpty(),
                                "BERT_DSSM_VACUUM_CLEANER": Round(1.0, 2),
                            }
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_region_source(cls):
        cls.disable_check_empty_output()
        cls.index.offers += [
            Offer(title='offer_bucket_region', offer_region_source_flag=1),
            Offer(title='offer_shops_dat_region', offer_region_source_flag=2),
            Offer(title='offer_external_table_region', offer_region_source_flag=4),
            Offer(title='offer_earth_region', offer_region_source_flag=8),
            Offer(title='offer_unknown_region_source'),
        ]

    def test_region_source(self):
        """Офферный фактор - источник регионального литерала - внешняя таблица
        https://st.yandex-team.ru/ECOMQUALITY-211
        """

        response = self.report.request_json('place=prime&text=offer_bucket_region&debug=da')
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'offer_bucket_region'},
                "debug": {
                    "factors": {
                        "OFFER_REGION_SOURCE_FLAG_EXTERNAL_TABLE": Absent(),
                        "OFFER_REGION_SOURCE_FLAG_EARTH": Absent(),
                    }
                },
            },
        )

        response = self.report.request_json('place=prime&text=offer_shops_dat_region&debug=da')
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'offer_shops_dat_region'},
                "debug": {
                    "factors": {
                        "OFFER_REGION_SOURCE_FLAG_EXTERNAL_TABLE": Absent(),
                        "OFFER_REGION_SOURCE_FLAG_EARTH": Absent(),
                    }
                },
            },
        )

        response = self.report.request_json('place=prime&text=offer_external_table_region&debug=da')
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'offer_external_table_region'},
                "debug": {
                    "factors": {
                        "OFFER_REGION_SOURCE_FLAG_EXTERNAL_TABLE": "1",
                        "OFFER_REGION_SOURCE_FLAG_EARTH": Absent(),
                    }
                },
            },
        )

        response = self.report.request_json('place=prime&text=offer_earth_region&debug=da')
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'offer_earth_region'},
                "debug": {
                    "factors": {
                        "OFFER_REGION_SOURCE_FLAG_EXTERNAL_TABLE": Absent(),
                        "OFFER_REGION_SOURCE_FLAG_EARTH": "1",
                    }
                },
            },
        )

        response = self.report.request_json('place=prime&text=offer_unknown_region_source&debug=da')
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'offer_unknown_region_source'},
                "debug": {
                    "factors": {
                        "OFFER_REGION_SOURCE_FLAG_EXTERNAL_TABLE": Absent(),
                        "OFFER_REGION_SOURCE_FLAG_EARTH": Absent(),
                    }
                },
            },
        )

    @classmethod
    def prepare_device_factors(cls):
        cls.index.offers += [Offer(title='device_factors')]

    def test_device_factors(self):
        for is_touch in (0, 1):
            response = self.report.request_json("place=prime&text=device_factors&debug=da&touch={}".format(is_touch))
            self.assertFragmentIn(
                response,
                {
                    "debug": {
                        "factors": {
                            "IS_TOUCH": "1" if is_touch else NoKey("IS_TOUCH"),
                            "IS_PAD": NoKey("IS_PAD"),
                            "IS_SMART": NoKey("IS_SMART"),
                        }
                    }
                },
            )

        for device_type in ("touch", "tablet", "smart", "desktop"):
            response = self.report.request_json(
                "place=prime&text=device_factors&debug=da&device={}".format(device_type)
            )
            self.assertFragmentIn(
                response,
                {
                    "debug": {
                        "factors": {
                            "IS_TOUCH": "1" if device_type == "touch" else NoKey("IS_TOUCH"),
                            "IS_PAD": "1" if device_type == "tablet" else NoKey("IS_PAD"),
                            "IS_SMART": "1" if device_type == "smart" else NoKey("IS_SMART"),
                        }
                    }
                },
            )

    def test_client_factors(self):
        response = self.report.request_json("place=prime&text=device_factors&debug=da")
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_APP": NoKey("IS_APP")}}})

        response = self.report.request_json(
            "place=prime&text=device_factors&debug=da&api=content&content-api-client=101"
        )
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_APP": "1"}}})

        response = self.report.request_json(
            "place=prime&text=device_factors&debug=da&api=content&content-api-client=14252&disable-testing-features=1"
        )
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_APP": "1"}}})

        response = self.report.request_json(
            "place=prime&text=device_factors&debug=da&api=content&content-api-client=18932"
        )
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_APP": "1"}}})

        response = self.report.request_json(
            "place=prime&text=device_factors&debug=da&api=content&content-api-client=12345"
        )  # кой-то левый
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_APP": NoKey("IS_APP")}}})

        response = self.report.request_json("place=prime&text=device_factors&debug=da&client=sovetnik")
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_APP": NoKey("IS_APP")}}})
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_SOVETNIK": "1"}}})

        response = self.report.request_json("place=prime&text=device_factors&debug=da&client=IOS")
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_APP": "1", "REQUEST_CGI_IS_IOS": "1"}}})

        response = self.report.request_json("place=prime&text=device_factors&debug=da&client=ANDROID")
        self.assertFragmentIn(response, {"debug": {"factors": {"IS_APP": "1", "REQUEST_CGI_IS_ANDROID": "1"}}})

    @classmethod
    def prepare_crypta_features(cls):
        cls.crypta.on_request_profile(yandexuid='52').respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value='922000'),
                CryptaFeature(name=CryptaName.GENDER_FEMALE, value='78000'),
                CryptaFeature(name=CryptaName.AGE_0_17, value='6787'),
                CryptaFeature(name=CryptaName.AGE_18_24, value='27605'),
                CryptaFeature(name=CryptaName.AGE_25_34, value='864000'),
                CryptaFeature(name=CryptaName.AGE_35_44, value='68868'),
                CryptaFeature(name=CryptaName.AGE_45_54, value='32740'),
                CryptaFeature(name=CryptaName.AGE6_0_17, value='56120'),
                CryptaFeature(name=CryptaName.AGE6_18_24, value='125034'),
                CryptaFeature(name=CryptaName.AGE6_25_34, value='295131'),
                CryptaFeature(name=CryptaName.AGE6_35_44, value='243381'),
                CryptaFeature(name=CryptaName.AGE6_45_54, value='149175'),
                CryptaFeature(name=CryptaName.AGE6_55_99, value='131158'),
                CryptaFeature(name=CryptaName.REVENUE5_A, value='105463'),
                CryptaFeature(name=CryptaName.REVENUE5_B1, value='248817'),
                CryptaFeature(name=CryptaName.REVENUE5_B2, value='217707'),
                CryptaFeature(name=CryptaName.REVENUE5_C1, value='366690'),
                CryptaFeature(name=CryptaName.REVENUE5_C2, value='61323'),
            ]
        )

        cls.index.offers += [Offer(title='crypta_features')]

    def test_crypta_features(self):
        response = self.report.request_json('place=prime&yandexuid=52&debug=da&text=crypta_features')
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'factors': {
                        'USER_GENDER_MALE': '922000',
                        'USER_GENDER_FEMALE': '78000',
                        'USER_AGE_0': '6787',
                        'USER_AGE_1': '27605',
                        'USER_AGE_2': '864000',
                        'USER_AGE_3': '68868',
                        'USER_AGE_4': '32740',
                        'USER_AGE6_0': '56120',
                        'USER_AGE6_1': '125034',
                        'USER_AGE6_2': '295131',
                        'USER_AGE6_3': '243381',
                        'USER_AGE6_4': '149175',
                        'USER_AGE6_5': '131158',
                        'USER_REVENUE5_0': '105463',
                        'USER_REVENUE5_1': '248817',
                        'USER_REVENUE5_2': '217707',
                        'USER_REVENUE5_3': '366690',
                        'USER_REVENUE5_4': '61323',
                    }
                }
            },
        )

    class TGenderStatConstants:
        # Мебель для кукол
        mebel_for_toys = CategoryGenderStat(
            male_orders_share=0.2, male_clicks=0.004, female_clicks=0.007, male_orders=0.001, female_orders=0.003
        )

        # Рыболовные принадлежности
        fish_stuff = CategoryGenderStat(
            male_orders_share=0.75, male_clicks=0.03, female_clicks=0.002, male_orders=0.008, female_orders=0.00001
        )

        # Телевизионные антенны
        # данные из дефолтной таблички market/report/data/factors/temporary_male_order_share_per_category.tsv
        tele_stuff = CategoryGenderStat(
            male_orders_share=0.78744941,
            male_clicks=0.03711066,
            female_clicks=0.02137767,
            male_orders=0.00397614,
            female_orders=0.00475059,
        )

    @classmethod
    def prepare_gender_category_features(cls):

        cls.index.hypertree += [
            HyperCategory(hid=10510546, name="Телевизионные антенны"),
            HyperCategory(hid=10682584, name="Мебель для кукол"),
            HyperCategory(hid=12319709, name="Рыболовные принадлежности"),
        ]

        # Заводим для них общие статистики
        cls.index.overall_category_stats += [
            # Мебель для кукол
            CategoryOverallStatsRecord(hid=10682584, price_avg=1000, gender_stat=T.TGenderStatConstants.mebel_for_toys),
            # Рыболовные принадлежности
            CategoryOverallStatsRecord(hid=12319709, price_avg=100, gender_stat=T.TGenderStatConstants.fish_stuff),
            # для Телевизионные антенны
            # данные из статичной таблички market/report/data/factors/temporary_male_order_share_per_category.tsv
            # не будем грузить в то что выгружается из индексатора
        ]

        cls.index.offers += [
            Offer(hid=10510546, descr='gender_category', title='Телевизионные антенны'),
            Offer(hid=10682584, descr='gender_category', title='Мебель для кукол'),
            Offer(hid=12319709, descr='gender_category', title='Рыболовные принадлежности'),
        ]

        # мужчина
        cls.crypta.on_request_profile(yandexuid='729304').respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value='922000'),
                CryptaFeature(name=CryptaName.GENDER_FEMALE, value='78000'),
            ]
        )
        # женщина
        cls.crypta.on_request_profile(yandexuid='8937837').respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value='122000'),
                CryptaFeature(name=CryptaName.GENDER_FEMALE, value='878000'),
            ]
        )
        # бисексуал
        cls.crypta.on_request_profile(yandexuid='9343384').respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value='450000'),
                CryptaFeature(name=CryptaName.GENDER_FEMALE, value='550000'),
            ]
        )

    def _check_gender_category_features(self, yandexuid, user_gender_male, user_gender_female):

        is_male = float(user_gender_male) / (user_gender_male + user_gender_female)
        is_female = 1.0 - is_male

        response = self.report.request_json('place=prime&yandexuid={}&debug=da&text=gender_category'.format(yandexuid))

        for (title, stat) in [
            ('Мебель для кукол', T.TGenderStatConstants.mebel_for_toys),
            ('Рыболовные принадлежности', T.TGenderStatConstants.fish_stuff),
            ('Телевизионные антенны', T.TGenderStatConstants.tele_stuff),
        ]:
            # статистика для категории Мебель для кукол
            male_orders_share = stat.male_orders_share
            male_clicks = stat.male_clicks
            female_clicks = stat.female_clicks
            male_orders = stat.male_orders
            female_orders = stat.female_orders

            self.assertFragmentIn(
                response,
                {
                    'titles': {'raw': title},
                    'debug': {
                        'factors': {
                            'USER_GENDER_MALE': Round(user_gender_male, 0),
                            'USER_GENDER_FEMALE': Round(user_gender_female, 0),
                            'MALE_ORDERS_SHARE_IN_CATEGORY': Round(male_orders_share),
                            'USER_GENDER_ORDERS_SHARE_IN_CATEGORY': Round(
                                is_male * male_orders_share + is_female * (1.0 - male_orders_share)
                            ),
                            'MALE_ORDERS_SHARE_IN_CATEGORY_FOR_MALE_USER': Round(male_orders_share)
                            if is_male > 0.66
                            else Absent(),
                            'FEMALE_ORDERS_SHARE_IN_CATEGORY_FOR_FEMALE_USER': Round(1.0 - male_orders_share)
                            if is_female > 0.66
                            else Absent(),
                            'MALE_CLICKS_TARGET_IN_CATEGORY': Round(male_clicks),
                            'FEMALE_CLICKS_TARGET_IN_CATEGORY': Round(female_clicks),
                            'GENDER_CLICKS_TARGET_PROPORTION_IN_CATEGORY': Round(
                                male_clicks / (male_clicks + female_clicks)
                            ),
                            'USER_GENDER_CLICKS_TARGET_IN_CATEGORY': Round(
                                male_clicks * is_male + female_clicks * is_female
                            ),
                            'MALE_ORDERS_TARGET_IN_CATEGORY': Round(male_orders),
                            'FEMALE_ORDERS_TARGET_IN_CATEGORY': Round(female_orders),
                            'GENDER_ORDERS_TARGET_PROPORTION_IN_CATEGORY': Round(
                                male_orders / (male_orders + female_orders)
                            ),
                            'USER_GENDER_ORDERS_TARGET_IN_CATEGORY': Round(
                                male_orders * is_male + female_orders * is_female
                            ),
                        }
                    },
                },
            )

    def test_gender_category_features(self):
        """factors by category in arcadia/market/report/data/factors/temporary_male_order_share_per_category.tsv"""
        self._check_gender_category_features('729304', 922000, 78000)
        self._check_gender_category_features('8937837', 122000, 878000)
        self._check_gender_category_features('9343384', 450000, 550000)

    @classmethod
    def prepare_reasons_to_buy_factors(cls):

        cls.index.models += [Model(hyperid=123456, title="Робот пылесос 123456")]

        reasons_123456 = [
            {
                "factor_name": "удобство модели",
                "type": "consumerFactor",
                "factor_id": "298",
                "value": 0.9952830076,
                "factor_priority": "1",
                "id": "best_by_factor",
            },
            {
                "factor_name": "Качество уборки",
                "type": "consumerFactor",
                "factor_id": "295",
                "value": 0.959882021,
                "factor_priority": "2",
                "id": "best_by_factor",
            },
            {
                "factor_name": "низкий уровень шума",
                "type": "consumerFactor",
                "factor_id": "296",
                "value": 0.9355029464,
                "factor_priority": "3",
                "id": "best_by_factor",
            },
            {
                "factor_name": "пылесборник",
                "type": "consumerFactor",
                "factor_id": "297",
                "value": 0.9178001285,
                "factor_priority": "4",
                "id": "best_by_factor",
            },
            {"value": 61.60257721, "type": "consumerFactor", "id": "bestseller"},
            {"value": 3696.155029, "type": "statFactor", "id": "bought_n_times"},
            {
                "author_puid": "12112466",
                "feedback_id": "75067150",
                "value": 5,
                "type": "consumerFactor",
                "text": "Каждый найдет их уйму. Работает по построенной собой же карте. Лучший вариант из всех, что я видел. до 50 000р",
                "id": "positive_feedback",
            },
            {"value": 109994, "type": "statFactor", "id": "viewed_n_times"},
        ]

        blue_reasons_123456 = [
            {
                "factor_name": "удобство модели",
                "type": "consumerFactor",
                "factor_id": "298",
                "value": 0.9919759035,
                "factor_priority": "1",
                "id": "best_by_factor",
            },
            {
                "factor_name": "Качество уборки",
                "type": "consumerFactor",
                "factor_id": "295",
                "value": 0.9367470145,
                "factor_priority": "2",
                "id": "best_by_factor",
            },
            {
                "factor_name": "низкий уровень шума",
                "type": "consumerFactor",
                "factor_id": "296",
                "value": 0.9046185017,
                "factor_priority": "3",
                "id": "best_by_factor",
            },
            {
                "factor_name": "пылесборник",
                "type": "consumerFactor",
                "factor_id": "297",
                "value": 0.8763819337,
                "factor_priority": "4",
                "id": "best_by_factor",
            },
            {"value": 1.25, "type": "consumerFactor", "id": "bestseller"},
            {"value": 75, "type": "statFactor", "id": "bought_n_times"},
            {"value": 0.9642184377, "type": "consumerFactor", "id": "customers_choice"},
            {"value": 3719, "type": "statFactor", "id": "viewed_n_times"},
        ]

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy(blue=False).new_partition().add(hyperid=123456, reasons=reasons_123456)
        ]

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy(blue=True).new_partition().add(hyperid=123456, reasons=blue_reasons_123456)
        ]

    def test_reasons_to_buy_factors(self):
        """Проверяем факторы REASON_* по причинам купить
        Значения факторов соответствуют значениям прични купить
        """

        response = self.report.request_json('place=prime&text=робот пылесос 123456&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 123456,
                'titles': {'raw': 'Робот пылесос 123456'},
                'reasonsToBuy': NotEmpty(),
                'debug': {
                    'factors': {
                        "REASON_BESTSELLER": Round(61.6, 1),
                        "REASON_BOUGHT_N_TIMES": Round(3696.155),
                        "REASON_IEWED_N_TIMES": "109994",
                        "REASON_CONVERSION_VIEWED_TO_BOUGHT": Round(3696.155 / 109994),
                        "REASON_POSITIVE_FEEDBACK": "5",
                        "REASON_CUSTOMERS_CHOICE": NoKey("REASON_CUSTOMERS_CHOICE"),
                        "REASON_BEST_BY_FACTOR_1": Round(0.9953, 4),
                        "REASON_BEST_BY_FACTOR_2": Round(0.9599, 4),
                        "REASON_BEST_BY_FACTOR_3": Round(0.9355, 4),
                        "REASON_BEST_BY_FACTOR_4": Round(0.9178, 4),
                        "REASON_BEST_BY_FACTOR_AVG": Round(0.9521, 4),
                        "REASON_BLUE_BESTSELLER": Round(61.6, 1),
                        "REASON_BLUE_BOUGHT_N_TIMES": Round(3696.155),
                        "REASON_BLUE_VIEWED_N_TIMES": "109994",
                        "REASON_BLUE_CONVERSION_VIEWED_TO_BOUGHT": Round(3696.155 / 109994),
                        "REASON_BLUE_POSITIVE_FEEDBACK": "5",
                        "REASON_BLUE_CUSTOMERS_CHOICE": NoKey("REASON_CUSTOMERS_CHOICE"),
                        "REASON_BLUE_BEST_BY_FACTOR_1": Round(0.9953, 4),
                        "REASON_BLUE_BEST_BY_FACTOR_2": Round(0.9599, 4),
                        "REASON_BLUE_BEST_BY_FACTOR_3": Round(0.9355, 4),
                        "REASON_BLUE_BEST_BY_FACTOR_4": Round(0.9178, 4),
                        "REASON_BLUE_BEST_BY_FACTOR_AVG": Round(0.9521, 4),
                    }
                },
            },
        )

    @classmethod
    def prepare_promo_and_discount(cls):
        cls.index.shops += [
            Shop(
                fesh=2401301,
                name="Первый магазин",
                priority_region=213,
                new_shop_rating=NewShopRating(rec_and_nonrec_pub_count=200000),
            ),
        ]

        promocode = Promo(
            promo_type=PromoType.PROMO_CODE,
            key='promokey_promocode',
            promo_code='promo3k',
            description='Сэкономь 3 тысячи рублей',
            discount_value=3000,
            discount_currency='RUR',
        )

        cls.index.offers += [
            Offer(
                title='Налетай покупай! Акция! Супер скидка только сегодня!',
                hyperid=200001,
                hid=2401301,
                fesh=2401301,
                price=150,
                price_old=200,
                promo_price=49,
                promo=Promo(promo_type=PromoType.FLASH_DISCOUNT, key='promokey_flashdiscount'),
            ),
            Offer(
                title='Налетай покупай! Выгодно и со скидкой!',
                hyperid=200002,
                hid=2401301,
                fesh=2401301,
                price=6500,
                price_old=10000,
            ),
            Offer(
                title='Налетай покупай! Промокод на скидку!',
                hyperid=200003,
                hid=2401301,
                fesh=2401301,
                price=27000,
                price_old=30000,
                promo=promocode,
            ),
        ]

        cls.index.models += [
            Model(title='Налетай покупай! есть акции', hyperid=200001, hid=2401301),
            Model(title='Налетай покупай! есть скидки', hyperid=200002, hid=2401301),
            Model(title='Налетай покупай! есть промокод', hyperid=200003, hid=2401301),
        ]
        cls.index.regional_models += [
            RegionalModel(
                rids=[213], hyperid=200001, promo_types=[PromoType.FLASH_DISCOUNT], promo_count=1, white_promo_count=1
            ),
            RegionalModel(rids=[213], hyperid=200002, max_discount=40, price_min=6000, price_old_min=10000),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)])],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="Налетай покупай! Беру!",
                hyperid=200002,
                hid=2401301,
                sku=200002001,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=1300, price_old=1500, feedid=1)],
            )
        ]

    def test_promo_and_discount_factors(self):
        """Проверяем факторы по акциям, скидкам и промокодам"""

        response = self.report.request_json('place=prime&rids=213&text=налетай покупай&debug=da')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! Акция! Супер скидка только сегодня!"},
                "model": {"id": 200001},
                "debug": {
                    "factors": {
                        "SHOP_OPINION_COUNT": "200000",
                        "LOG_SHOP_OPINION_COUNT": "17.60964775",
                        "OFFER_OLD_PRICE": "150",
                        "OFFER_PRICE": "49",
                        "DISCOUNT": Round(0.67),
                        "DISCOUNT_IN_RUB": "101",
                        "HAS_ACTIVE_PROMO": "1",
                        "MODEL_HAS_PROMO": "1",
                        "MODEL_DISCOUNT": NoKey("MODEL_DISCOUNT"),
                    }
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! Выгодно и со скидкой!"},
                "model": {"id": 200002},
                "debug": {
                    "factors": {
                        "OFFER_OLD_PRICE": "10000",
                        "OFFER_PRICE": "6500",
                        "DISCOUNT": Round(0.35),
                        "DISCOUNT_IN_RUB": "3500",
                        "HAS_ACTIVE_PROMO": NoKey("HAS_ACTIVE_PROMO"),
                        "MODEL_HAS_PROMO": NoKey("MODEL_HAS_PROMO"),
                        "MODEL_DISCOUNT": Round(0.4),
                    }
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! Промокод на скидку!"},
                "model": {"id": 200003},
                "debug": {
                    "factors": {
                        "HAS_PROMOCODE": "1",
                    }
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! есть акции"},
                "id": 200001,
                "debug": {
                    "factors": {
                        "DISCOUNT": NoKey("DISCOUNT"),
                        "DISCOUNT_IN_RUB": NoKey("DISCOUNT_IN_RUB"),
                        "HAS_ACTIVE_PROMO": NoKey("HAS_ACTIVE_PROMO"),
                        "MODEL_HAS_PROMO": "1",
                        "MODEL_DISCOUNT": NoKey("MODEL_DISCOUNT"),
                    }
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! есть скидки"},
                "id": 200002,
                "debug": {
                    "factors": {
                        "DISCOUNT": Round(0.4),
                        "DISCOUNT_IN_RUB": "4000",
                        "HAS_ACTIVE_PROMO": NoKey("HAS_ACTIVE_PROMO"),  # only for offer
                        "MODEL_HAS_PROMO": NoKey("MODEL_HAS_PROMO"),
                        "MODEL_DISCOUNT": Round(0.4),
                    }
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! есть промокод"},
                "id": 200003,
                "debug": {"factors": {"HAS_PROMOCODE": NoKey("HAS_PROMOCODE")}},
            },
        )

    def test_promo_and_discount_factors_when_market_hide_discount_on_basesearch(self):
        """Под флагом market_hide_dicount_on_baseserch=1.0 скрываются также факторы DISCOUNT и DISCOUNT_IN_RUB"""

        response = self.report.request_json('place=prime&rids=213&text=налетай покупай&debug=da&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! Беру!"},
                "model": {"id": 200002},
                "prices": {"discount": {"percent": 13}},
                "debug": {
                    "factors": {
                        "OFFER_OLD_PRICE": "1500",
                        "OFFER_PRICE": "1300",
                        "DISCOUNT": Round(0.13),
                        "DISCOUNT_IN_RUB": "200",
                        "MODEL_DISCOUNT": Round(0.4),
                    }
                },
            },
        )

        response = self.report.request_json(
            'place=prime&rids=213&text=налетай покупай&debug=da&rgb=blue'
            '&rearr-factors=market_hide_discount_on_basesearch=1.0'
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! Беру!"},
                "model": {"id": 200002},
                "prices": {"discount": NoKey("discount")},
                "debug": {
                    "factors": {
                        "OFFER_OLD_PRICE": NoKey("OFFER_OLD_PRICE"),
                        "OFFER_PRICE": "1300",
                        "DISCOUNT": NoKey("DISCOUNT"),
                        "DISCOUNT_IN_RUB": NoKey("DISCOUNT_IN_RUB"),
                        "MODEL_DISCOUNT": Round(0.4),
                    }
                },
            },
        )

    def test_promo_and_discount_factors_when_market_hide_discount_on_basesearch_2(self):
        """Под флагом market_hide_dicount_on_output=1.0 факторы DISCOUNT и DISCOUNT_IN_RUB не меняются"""

        response = self.report.request_json('place=prime&rids=213&text=налетай покупай&debug=da&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! Беру!"},
                "model": {"id": 200002},
                "prices": {"discount": {"percent": 13}},
                "debug": {
                    "factors": {
                        "OFFER_OLD_PRICE": "1500",
                        "OFFER_PRICE": "1300",
                        "DISCOUNT": Round(0.13),
                        "DISCOUNT_IN_RUB": "200",
                        "MODEL_DISCOUNT": Round(0.4),
                    }
                },
            },
        )

        response = self.report.request_json(
            'place=prime&rids=213&text=налетай покупай&debug=da&rgb=blue'
            '&rearr-factors=market_hide_discount_on_output=1.0'
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Налетай покупай! Беру!"},
                "model": {"id": 200002},
                "prices": {"discount": NoKey("discount")},
                "debug": {
                    "factors": {
                        "OFFER_OLD_PRICE": "1500",
                        "OFFER_PRICE": "1300",
                        "DISCOUNT": Round(0.13),
                        "DISCOUNT_IN_RUB": "200",
                        "MODEL_DISCOUNT": Round(0.4),
                    }
                },
            },
        )

    def test_factors_for_category_ranking(self):

        for flag_req_hard in [False, True]:
            flag = '&rearr-factors=market_disable_request_hard_dssm_features=1' if flag_req_hard else ''
            response = self.report.request_json('place=prime&text=услуги юриста или киллера&rids=213&debug=da' + flag)
            self.assertFragmentIn(
                response,
                {
                    "categories_ranking_json": [
                        {
                            "id": 12,
                            "name": "Киллерские услуги",
                            "relevanceValue": NotEmpty(),
                            "relevanceFormula": "full_mode_f",
                            "redirectValue": NotEmpty(),
                            "redirectFormula": "full_mode_f",
                            "factors": {
                                "FIXED_BOCM_TITLE_Q0": NotEmpty(),
                                "FIXED_BOCM_TITLE_Q100": NotEmpty(),
                                "FIXED_BOCM_TITLE_Q25": NotEmpty(),
                                "FIXED_BOCM_TITLE_Q50": NotEmpty(),
                                "FIXED_BOCM_TITLE_Q75": NotEmpty(),
                                "FIXED_DOCUMENT_QUERY_CTR_Q0": NotEmpty(),  # на самом деле здесь тоже есть Q25, Q50 и т.д.
                                "DSSM_BY_QUERIES": NotEmpty(),
                                "DSSM_BY_UNIQNAME": NotEmpty(),
                                "FIXED_OFFER_PRICE_Q0": 200,
                                "FIXED_SHOP_OPINION_COUNT_Q0": 200000,
                                "GOODS_RATIO_IN_TOP": NotEmpty(),
                                "POSITION_OF_FIRST_DOCUMENT_IN_TOP": NotEmpty(),
                                "RANK_CATEGORY_BY_RELEVANCE_IN_TOP": NotEmpty(),
                                "GOOD_FOR_REDIRECT": 2,
                                "HARD_QUERY_AUDIO": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_BEAUTY": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_CARS_BIG_ACCESSORIES": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_CARS_SMALL_ACCESSORIES": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_FIND_SHOP": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_HEATING_AND_AIRING": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_HOUSEHOLD_EQUIPMENT": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_NEWBORN": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_PHONES": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_PRESENTS": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_PRINTERS": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_TIRES": Absent() if flag_req_hard else NotEmpty(),
                                "HARD_QUERY_TOYS": Absent() if flag_req_hard else NotEmpty(),
                                "LEVEL_RATIO": 100,
                                "MATRIXNET_VALUE_Q0": Round(0.3),
                                "MODEL_BY_QUERIES_AUTOPARTS": NotEmpty(),
                                "MODEL_BY_QUERIES_COSMETIC": NotEmpty(),
                                "MODEL_BY_QUERIES_HOUSE_RENOVATION": NotEmpty(),
                                "MODEL_BY_QUERIES_LAPTOPS": NotEmpty(),
                                "MODEL_BY_QUERIES_MEDICINE": NotEmpty(),
                                "MODEL_BY_QUERIES_PHONES": NotEmpty(),
                                "MODEL_BY_QUERIES_SANITARY_EQUIPMENT": NotEmpty(),
                                "OFFERS_COUNT_RATIO": Round(0.333),
                                "OFFERS_COUNT_RATIO_TOP_1_CATEGORY": 1000,
                                "OFFERS_COUNT_RATIO_TOP_2_CATEGORY": 666,
                                "OFFERS_COUNT_RATIO_TOP_3_CATEGORY": 333,
                                "OFFERS_COUNT_RATIO_TOP_4_CATEGORY": 333,
                                "OFFERS_COUNT_RATIO_TOP_5_CATEGORY": 166,
                                "REQUEST_ALL_LOWER": 1,
                                "REQUEST_CYRS_RATIO": 1,
                                "REQUEST_CYR_ONLY": 1,
                                "REQUEST_DNORM_CYRS_RATIO": 1,
                                "REQUEST_DNORM_CYR_ONLY": 1,
                                "REQUEST_DNORM_LENGTH": 23,
                                "REQUEST_DNORM_SYMBOLS_COUNT": 20,
                                "REQUEST_DNORM_WORDS_COUNT": 4,
                                "REQUEST_LENGTH": 25,
                                "REQUEST_SYMBOLS_COUNT": 22,
                                "REQUEST_WORDS_COUNT": 4,
                                "FIXED_SHOP_OPINION_COUNT_Q0": 200000,
                                "TRIGRAM_COUNT": 8,
                                "TRIGRAM_RATIO_CATEGORY": Round(0.55, 2),
                                "TRIGRAM_RATIO_QUERY": Round(0.43, 2),
                            },
                        },
                        {"id": 1111, "name": "Ликвидация предприятий", "factors": NotEmpty()},
                        {
                            "id": 11,
                            "name": "Юридические услуги",
                            "factors": {
                                "LEVEL_RATIO": 50,
                                "OFFERS_COUNT_RATIO": Round(0.666),
                                "TRIGRAM_COUNT": 5,
                                "TRIGRAM_RATIO_CATEGORY": Round(0.36, 2),
                                "TRIGRAM_RATIO_QUERY": Round(0.3, 1),
                            },
                        },
                        {"id": 111, "name": "Регистрация и ликвидация предприятий", "factors": NotEmpty()},
                        {"id": 1, "name": "Услуги", "factors": NotEmpty()},
                    ]
                },
                allow_different_len=False,
                preserve_order=False,  # TODO: MSSUP-763 - переход на новые факторы приводит к константному значению тестовой формулы по умолчанию
            )

    @classmethod
    def prepare_rounded_factors(cls):
        cls.index.offers += [
            Offer(hid=8475955, title='Категория не должна округляться до 8475960', waremd5='uOS--7efpIiiA3Y9iH7bGA')
        ]

    def test_rounded_factors(self):
        """Большие и маленькие числа должны записываться без округления
        MARKETOUT-26287
        """
        response = self.report.request_json('place=prime&text=категория+не+должна+округляться+до+8475960&debug=da')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Категория не должна округляться до 8475960"},
                            "wareId": "uOS--7efpIiiA3Y9iH7bGA",
                            "debug": {"factors": {"CATEGORY_ID": "8475955"}},
                        }
                    ]
                }
            },
        )
        # категория записывается правильно а не в научном формате 8.47596e+6
        self.feature_log.expect(ware_md5="uOS--7efpIiiA3Y9iH7bGA", category_id=8475955).times(1)

    @classmethod
    def prepare_query_classifier_factors(cls):
        cls.matrixnet.on_place(MnPlace.QUERY_MODEL_GROUP_CLASSIFIER, 'iphone Xr 256').respond(0.7)
        cls.matrixnet.on_place(MnPlace.QUERY_SMTH_CLASSIFIER, 'iphone Xr 256').respond(0.1)

        cls.matrixnet.on_place(MnPlace.QUERY_MODEL_GROUP_CLASSIFIER, 'samsung').respond(0.2)
        cls.matrixnet.on_place(MnPlace.QUERY_SMTH_CLASSIFIER, 'samsung').respond(0.8)

        cls.index.models += [
            Model(title='iphone Xr 256', hyperid=2721111, hid=2401301),
            Model(title='samsung galaxy tab 7', hyperid=2721112, hid=2401302),
        ]

        cls.index.offers += [
            Offer(title='iphone Xr 256', hyperid=2721111, hid=2401301),
            Offer(title='samsung galaxy tab 7', hyperid=2721112, hid=2401302),
        ]

    def test_query_classifier_factors(self):
        """Проверяем, что факторы, которые выдают новые формулы,
        пишутся в фичалог (а значит, пробрасываются на базовые)
        """
        _ = self.report.request_json('place=prime&text=iphone Xr 256')
        self.feature_log.expect(query_model_group_classifier=Round(0.7, 2), query_smth_classifier=Round(0.1, 2)).times(
            2
        )  # from model and offer

        _ = self.report.request_json('place=prime&text=samsung')
        self.feature_log.expect(query_model_group_classifier=Round(0.2, 2), query_smth_classifier=Round(0.8, 2)).times(
            2
        )  # from model and offer

    @classmethod
    def prepare_commonly_purchased_factors(cls):
        universal_categories = [278374]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_UNIVERSAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[YamarecCategoryPartition(category_list=universal_categories, splits=['*'])],
            )
        ]

        cls.index.offers += [Offer(title='expert universal', hid=278374), Offer(title='common category', hid=111111)]

    def test_commonly_purchased_factors(self):
        """
        Проверяем, что факторы основанные на экспертном списке частотных категорий
        вычисляются верно.
        MARKETOUT-28107
        """
        response = self.report.request_json('place=prime&text=expert universal&debug=da')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "expert universal"},
                "debug": {"factors": {"BLUE_IS_COMMONLY_PURCHASED_CATEGORY_BY_EXPERTS": "1"}},
            },
        )

        response = self.report.request_json('place=prime&text=common category&debug=da')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "common category"},
                "debug": {
                    "factors": {
                        "BLUE_IS_COMMONLY_PURCHASED_CATEGORY_BY_EXPERTS": NoKey(
                            "BLUE_IS_COMMONLY_PURCHASED_CATEGORY_BY_EXPERTS"
                        )
                    }
                },
            },
        )

    def test_write_meta_factors_in_case_no_meta_formula(self):
        """Проверяем что даже если отключена мета формула
        под флагом market_write_meta_factors=1
        метафакторы все равно будут посчитаны и записаны
        """
        # дефолтное состояние: считаются мета-факторы и вычисляется мета-формула
        response = self.report.request_json('debug=da&place=prime&text=услуги юриста или киллера&rids=213')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'trace': {'fullFormulaInfo': [{'tag': 'Default'}, {'tag': 'Meta'}]},
                'debug': {'factors': {'HEAD_AVG_DOC_REL': NotEmpty()}},
            },
            allow_different_len=False,
        )

        # запрещено переранжирование на мете: мета факторы и мета формула не вычисляется
        response = self.report.request_json(
            'debug=da&place=prime&text=услуги юриста или киллера&rids=213'
            '&rearr-factors=market_enable_meta_head_rearrange=0'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'trace': {'fullFormulaInfo': [{'tag': 'Default'}]},
                'debug': {'factors': {'HEAD_AVG_DOC_REL': NoKey('HEAD_AVG_DOC_REL')}},
            },
            allow_different_len=False,
        )
        self.feature_log.expect(ware_md5='09lEaAKkQll1XTaaaaaaaQ', all_matrixnet_values=Not(Contains("Meta:None")))

        # запрещено переранжирование на мете, но включена запись мета факторов
        # метафакторы вычисляются, а мета формула - нет
        response = self.report.request_json(
            'debug=da&place=prime&text=услуги юриста или киллера&rids=213'
            '&rearr-factors=market_enable_meta_head_rearrange=0;market_write_meta_factors=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'trace': {'fullFormulaInfo': [{'tag': 'Default'}]},
                'debug': {'factors': {'HEAD_AVG_DOC_REL': NotEmpty()}},
            },
            allow_different_len=False,
        )
        self.feature_log.expect(ware_md5='09lEaAKkQll1XTaaaaaaaQ', all_matrixnet_values=Contains("Meta:None"))

    def test_override_factors(self):
        """флагом market_override_factors можно занулять/переопределять один или несколько факторов"""

        response = self.report.request_json('place=prime&text=dssmoffer&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.7064),
                                "DSSM_HARD2": Round(0.657),
                                "HEAD_AVG_DSSM_HARD2": Round((0.67 + 0.657) / 2),
                                "HARD2_CHEBYSHEV": NotEmpty(),
                            }
                        },
                    }
                ]
            },
        )

        response = self.report.request_json(
            'place=prime&text=dssmoffer&debug=da&rearr-factors=market_override_factors=DSSM_HARD2'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.7064),
                                "DSSM_HARD2": Absent(),
                                "HEAD_AVG_DSSM_HARD2": Absent(),  # because DSSM_HARD=0 on base
                                "HARD2_CHEBYSHEV": NotEmpty(),
                            }
                        },
                    }
                ]
            },
        )

        response = self.report.request_json(
            'place=prime&text=dssmoffer&debug=da&rearr-factors=market_override_factors=.*HARD2.*'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.7064),
                                "DSSM_HARD2": NoKey("DSSM_HARD"),
                                "HEAD_AVG_DSSM_HARD2": Absent(),
                                "HARD2_CHEBYSHEV": Absent(),
                            }
                        },
                    }
                ]
            },
        )

        response = self.report.request_json(
            'place=prime&text=dssmoffer&debug=da'
            '&rearr-factors=market_override_factors=DSSM_MARKET_CLICK=0.1,.*DSSM_HARD2.*=0.2'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_MARKET_CLICK": Round(0.1),
                                "DSSM_HARD2": Round(0.2),
                                "HEAD_AVG_DSSM_HARD2": Round(0.2),
                                "HARD2_CHEBYSHEV": NotEmpty(),
                            }
                        },
                    }
                ]
            },
        )

        response = self.report.request_json('place=productoffers&hyperid=33331&rids=213&regset=2&debug=da')
        self.assertFragmentIn(response, {'results': [{'debug': {'factors': {'CBID': '1'}}}]})
        response = self.report.request_json(
            'place=productoffers&hyperid=33331&rids=213&regset=2&debug=da'
            '&rearr-factors=market_override_factors=CBID=234'
        )
        self.assertFragmentIn(response, {'results': [{'debug': {'factors': {'CBID': '234'}}}]})

    @classmethod
    def prepare_multitoken_factors(cls):

        cls.index.offers += [
            # xiaomi - чтобы не испортить запросы из предыдущих тестов со словом samsung
            Offer(title="bla bla xiaomi galaxy s10 8gb xiaomi", descr="s10 s20 8gb"),
            Offer(
                title="Видеокарта PNY Quadro 2000 625Mhz PCIE 2.0 1024Mb 2600Mhz 128 bit DVI",
                descr="название видеокарты: NVIDIA Quadro 2000, линейка: Quadro, разработчик видеокарты: NVIDIA, объем видеопамяти: меньше 2 ГБ, тип памяти: GDDR5, разрядность шины памяти: 128 бит, частота видеопроцессора: 625 МГц, частота памяти: 2600 МГц, Версия PCI Express: 2.0, тип подключения: PCI-E 16x, Выход DisplayPort",  # noqa
            ),
            Offer(title="15 ноутбук rb012ur ноутбук hp amd 2j", descr="15-rb013ur je. 15-rb012ur 15-rb012ur"),
            Offer(title="E2j ноутбук hp amd E2j E2j", descr="15-rb012uR ноутбук hp. 15-rb012uR ноутбук hp amd E2j"),
            Offer(
                title="e2j1 ноутбук hp amd e2j34 e2jwe", descr="15-rb012ur ноутбук hp. 15-rb012ur ноутбук hp amd e2j"
            ),
        ]
        cls.reqwizard.on_request(text='abcd samsung abcd galaxy s10 8gb').respond(
            qtree="cHic7VttbBTHGZ53d30sw2GdQKhkG7cnt4IzLa2pZOdKlaZx-UG_Ekq_wv2oOBvXRmr54VNTsNTigyR1CWC-SttAGpQIcL6MAVsBYgyEilBIpL1KrZQqyo-0TX80bSJVqaoqEp2P3dmZ2bnbM5gKUluybnfmnbln3ved92vm8Jdx2p2baVyIspCzWtE85KFmtAR9Bi1PuyiDSDvKoVb0hYaVDavQA2gt6oVdgB4D9CSgE4DOAiJ_lwH5sM57FvA3MR_mkmF0OqfY2bXOg6yY05HmRCsRnbP3zweH5oaTsgHaxK0oDx1fciGDPNbdjHLQCqv2WhxP3xLMmhfSZpRHq63BkQIc7kwRepSb3zmLfMLinN0LG61-y7UGAJFv814AvEZDO6tU_EHphxt6KGDIBohnGRC_uSDEGw4xQV7FIIcUOupP4bBnIZs8Dtwln9biEkGOFOTPAf5WNT4L2LcOoy8D_o4GN9VT_H5x4yYFcMoA-O2WEG4wwgS4iwEOCHTIHTjoEEyGwYJ1eKRzdgY8e3034a7ATR4cDzbV4PxuwF_VlgIlsmPEIsCwiEtOuAgoxfHbHW1kFHhQ0qHfgaEkWI1CLve6HNFaCDCVAd-nYbKWtcp7zmKgQAF1HoegCHEcldXxMTIMPNLZPKc1_FvWyhAGdqAfCQw_ibMlH2eLiuCPDYIteRNbmhkAyEvf35qv8v37AH9N50FPp2TKLINchl3Bgp5OEwvuYoIhnbpkPEwak0XTPNfdnBmAhVbWzjW0TvGVYrbYG7C3hkzK3Ef3QOE34A4ODMAChPpXeMcAf1fjhuuP-scrj_hjHrQ0Q66GjTi4_43nrZAxYphp332R7TtBEjDpqZBJWSy6jKwCwqqQUQ4Fm0OFiyl3F1vG-U0rvJEUXolDL2TV451q-KYJw84typvEtHNPRTu3GOeA09HJOADFSD9grUWWvghDkdkbyFurrZ1-Aak2cgM3LH0foXQQ0NmDh0ciQs3w_Dyu4NCpmE8T_oOzBH6DgkPHco4_pt8EV6ewl1PG1ZWI61CEq6sGri4Drq76cJ0wiHtdkqF-1xawjJ7xAQ4r5hYJrHVMjGZYfXfSfuF-tlUD3Zxxr12zMwMDDtnaTs5urauFIlN2kVMYtqVdtNeexl1E7Oz9up0tdsZ9jcrXN86LGQm1ibN3Mc6SToOlLdawtIG09xpcYJcibhOsgWUClTkSClDF4yCCqiuSeN3iNIjqosXtdsvQJ4nBs3CvtozGSpkY0YlKubLVP-ufVKy3KTB9f-KOcFHaUNMCXwC2Qo1SN-Q7AGsU0h48f6VgDY-wiEoJW_1REk_xpwnxdIo9WZ5VeTh88sfZEwS9mDw5_qR_QrROiDFbmdVU3Qbxg8Rt3Pbez8k4M95vxvvNeL8b8n50F814v9vC-3FR-Q63278-t3iF96KD1-vezz9JjOiL5P8McUCnEr3fpb_8R_BaG2ta4UWbuz-VUrfki3BaJpDEETo1KpCGvjVYm4iwCIJNcYF4yWeOkX_iKYePcY95lHweJZ9HyOcR8vk0-XyaetJjsjfthY3QV6g6tTN44Nxvb2juJXieOndlS2W7onNW6LL6doIKpDKkAznCviRw3ePC8Z8O3Xllp3D8p1ioQNu2a730aSgpfKDgFR-ayqSIDz0aqBTb_XuceEDln6zskFQqsgUmhVLCAW2oSaMOCI2SKXWNYpoiE0yzphinniZNkedO1JSI-JbTFKfwliP5iYozjX7iZcDfrl7wRDP1zhuqd_4Yf0WH1CaJylRqjLwCtJkKfUGpsU0uNbaZS42GoCRWjZMUzZ1RtA-SorUnKNrrUQzdXkPR2mVFa582RRuUI99_WvHjLS6wUoLTG95xRfi8cIhJPR61-PlWQKJz-eM47JHcm5IdAMkOlsapapwqmckPF2BEJS8F5PfGyQMtjZ2xcX-hqC2botkwRVldhBT1hqKopNxDQhQT05nKnzHGNGqYnK0p35ko-f81Svb2G1LFSjnJzr4l7CwhNlWCeMWCdEZawUtBTdhiusBrPPBTJfmXg7La2SLfU6_L5u3l6UzsPzh-dB_gr2PuJ4SAS21JtQAlzSH0pkW0cRmX2oIF7HGickCpzehOUT-aEe_NF2_7FMVriEwi8babxNs-DeJ9BqTE3FA45XaoVj06wQzBNJghx3UZ2OfBHRhwF9D5vV_xW00yz-0Sv2AhuG4ncZ2OqF5ko7063z-MaWsC4ylenEOFx21398AuegCycoU3yPcOBYwCwNX2TmplKmHv_DJ-14gE5rOziz6dW5Zd1pKNCnJTj9DhOiP0YDMMxu8VsYsvLc0RvOu_AQN13YAJ5BBF5k4rVTJQ5OMUxh23LORzyKlbPsm2rWridP1iueHEacCwu4lg9Ns4UxJHw9QuJO03sKWbqkYtA3NktmBLtyl7W87Z0m04AelOOJgRWZ1-radaRsf1ZhRCvSGG6EDcEDnERHRLjE080mQDTMr-WWaIWLduie7ErLkuU-QUjtsEcmjnD9ZvipJVfQziCXkxwWcknWGu4SKNnWE2VT3DrPfo8qbuhqldz6uhZpjJ7HeyzM5Op8x-b1i8P5ogtBPidi2hNUltI5Ma6TQ4en80cvRbVUevdz9Su_uheJhwOwm17EhCfdvGPbGqwZg_TrPTyhZ_tDKUdBJybI-wjtpIkzn5lxUUDRRKvWiwlWbKCoV0kHzuilKowSSlpTdD1CSZtVa2Ss-PSjdKqt88CZJnJvZLCgoywWmBArYV7MMj5zrTZGiKZPMn_Ukx0RABxdsn5Xbp60_HgdBLLMH4ZPiG8bUWosSxXAeOyht7j42_V0MHJv3xBB146cwckwqQgSYVeDyuAoQyXjfSCMxupu9qXEjSnQJ7eOQCldSFKpKyq0hq_i0jqX-AJKnXAHdpkkpLi98uycmUtW57Z7-wyMo4k5h6mZQUMl1G92KlO2L8wwUYpqXa6P5WWKoKUljKrioLfsiSFvwO1DJPle2VnQmqOfanv6cMuklHmhbdr6smJTSVNNVlKyVNsjZe72vB2kSR-SjXyDQ5G56TI7xfAF6tp5pFxa0kJ5rFmolm0ZhoFutINHF9Aak_OjW8bECNgJR2B4i3SAEpba4T8t_knfUHzmHlpzMPrl-_PiGGHhv3BGBKbgL8IAdMu_UE4ROYNVc74YiqPfTAYUmMuOr5hnLwkM6kyWrfl1f7V5MdOeFPVsr-uD-RaEf2PHVVsiPSuOrLV8j0DXU_VrrjdkRhCDO16YzjpSrb_YnodijdYQarwpe_Q3Z4_7YMVoUBYDMmBz2XH9snWRVlpIkFW0KPp1CaPJ5CUMXjjYJCOKl5PBqW8Jho9H8QikzdrXF5PCsV0oh1-4Zu3fLsVzpTqKPlTT_VgY52bt7y4rc6CyPrlq_yWx3ZVKQzjTlUuGKH1q1_hTc-nXlQtR9IqVWam_cDKd9g9_y9_m65JGIqHZ9pjAw1ITclLRtYMuYQxTseqjqIDc-atevi13kMp6VgNfIfKkynMCa7qif4EYCsew15xoAp-Co-wqR-n2PqR_ojJgyGTGjCvL0uHXQKb8p18ld5vV8VG5kr4ZdtstQItQnxhsC9SlKzbqLUxCIb6SLJ2zb4KGbX9-d9yAVvvgsL3ru2-Z6mnnIln7WX_mhy8z06RWrB1Vdeubvp9Luv5rNoKV0Op3AzWFD87KX3Pt9Ufm3-3YJiPqY_tbDmzXHd1bNcmGfft6qbtzoZN2i1pNaUaJVpcabRQJsWrSFtmn0bEZLb2If4K12A9OpkHPmVfp30ijNp-ZUwi78S3v0XfsK8Gw,,"  # noqa
        )
        cls.reqwizard.on_request(text="abcd samsung abcd galaxy s10 s20").respond(
            qtree="cHic7Vt9bBTHFZ_ZXY5luFhXu6jkGreWW5EzCu3Zkh1CRdJQKtGvlNIvuD8qzsa1T2mi6K5VDVLFGQK1CMaOSWgSiIQaAU5KjIMhAceAKRFJIJV2K0VRpCh_9CNSpbSJVDVKozZ0ZnZ3bnb23e4dPiQ7NZK5vZ03c795vzfvzXs7S75J4iZKoKWoSUtpaVSPkqgZLUdtaJV3H6VQGn11wboF69FGtBn14mGMnsDotxidxOgcRvTfKxhZeEvyd5j8wB3OpN3YcEa2s2tLEjeJMQ1pTLQOsTF7_3Jo6CZvUN5BGTiNVuI13zBxAiV5czNK4TRev19z8OSXE357KbuNVqIN2sBYBh_pjFF5lGroXEg_8a0pvRf3ads0UytiRH8t-QImmxS0CwvZ-wq_uL-HAcZNLuKFAOI_LfHwel0gyOs5ZE9CRf0l4rUs5YMHgZv0U7u1QJEjH_LjmPywnJ4F7Nmj6Fcw-bECN9aT_Vm2b6sPcAwA_G6LB9ftAQHu4oBdARXyGuI2CCXjgYx2ZKxzUQIn9Vw31a7ATS-MJN4aovmHMfm2MhVcoCtGTAIDk7hseJPAhSB-fU077YWTuKBCv5ngglA18rTcazqINmMXUz8m9yiYtNa0vOY0Dgr7QE0TDxQVDqLS1nyBdsNJ2ti8OO39a01zhK4f2IYEhtmolyIm31H10paW3Bukluc_JdTSFqaWNlktbWXU0nyTuT1RxEu1Jj21IF3lV4ZP499wRCNbBJmXsDlQLOIlCG1bmzyByU-UqZvWuPWcvds6lcQtzTgV4iQOHXj7Wc3TgugGLbyv8YUnRFyenvJ4aiKiCaQLU7o8TRkMbAplLsXMYT6N6a1rk2Mxsq668BQSnKYAE83KqwQy0TMlE80GNWCs6eQawNmSieLNGp36MoKz3OHgldoGbZ-VQX4neb_jWfKfY3LYldMHjoyVBBXP82jQmnGnz39C-A8tFPg7IQZXOfg71SVGcXUKh1k1rq5IXIdLuLpCcHUBuLoqw3USoHtLlEd6XxewwNC40YEViIsU1hZOIwwrfwtrF_FnTznQzQnz2jU9USwadGkbKT1d0R2GzLeKjMyoLq2i_XoNV9EjmHxXdarZzmCw8ev17WkxIpWGNHs71yxtVFWbJPRmeW_vsr0fiIFdProhWMVWgQreCrmoghshiqqrxHjFdAJUXdIcv923fAV1eBrpVaZRZ_dTJzpl99s7rXPWhM97QzvT_0zd7E1K6QpN8AXMZ6hIqo58EBNFQlqD01cy2ugY31L59q3WON1QOVdT4uoMv9KSmr3Lu7JO8yvsthJ6ZVjnrZPi7pTos5N7TX_YoHGQho05H_2MhDEf_eaj33z0m1H0Y6toPvrNiejnUGUZjt_-4PaWtckXDZJTo581QZ3oi_RvkgagM5HR7_JfPxK6VvpCM7ykO-HPL6l68mUkLgtIdHhBjRGyIL-JKANRFWF3UVykUfKZE_SPRsrRE07EPEY_j9HPo_TzKP18mn4-zSLpCTma9uI-nM-UHdoYOHjh1RmNvZzU-8e2d9h7fTaneSErvw_7gdhDKpCj_Efc0H1aBP6zXji394nAf4ZvFdi9vUoruxqK2j4w8L4YGkvEaAw95poUX_0jRnBDZU3Yg5JJlXwBZFC-7YDSFbKog8KiZEnVorilyAI1thRw6BpZijx2pKWUhGedpRiZdwwpTthGDePEy5j8qHzFE80XPGdU2PsV-ZYKqV2iCgNlvVJUwO1QVa-ZV_Vwu1TUS7eXKeoFNyWBapxkaOa8oX2SDK0jwtDeKu2hO0IMrUM2tI6aGdqAvPP9pxZ8vuUQVogIeqODV0TM87pA5vGQ5jzgckVULX-ReC1SePNlB5hmByuCUiGPlWDxIxk85hcvuOJ3B8VdKw08ZHPihc9s-RDNwBD9_klIu16PCjtmHhZUTNUylZ8E9zT-bXJTKL_zu-T_111y8gCQKtr9UX72HeFnqTBUCXIqFrSxZBVOKaiRaNwWnBoP_rUv-Zc3ZeHZorOm3pLd28u1TOw_OXH0EUy-R5w4IQgutEfVAnxpDpWHJtHucFxodycwYpTKAYV2MJyibWie3htPb0eV9AI7kxK9HRC9HTWg9xksJeZA4dTxQ2H16Ag3hGvghgzT5GCfxWaxaC5h4ycfd441yTrXC84JC6F1PUrrrEf5IhtrVfX-WcLuRiie4SUplHlSNx8uDrMHIOvWJgectcMAIxdwubUTWxeLWDuPBQ8b0Y35oqZlX061NrW2NJUKctXv0PF17tDdxTAQPFjET760NJfgXf8RGFzRERiXh9LO3EgzI8M-fozMacPsF_wcNirmJ9q3lU2crp-WG3L0pjXq6E0EHQuqO5F0AFBLNzONMAdzdJFQSzeUva1y1NINPAHpjngwI7I6_7me8hmdYzfj2LMb6ogOBh2RQV1Et6TYyEeavANk7HdwR8SbVU90C-G3K3JFRuY5nUL2_Pyhyl1RtKmfwsGEPBsRM6KeYW5yKA08w2ws-wyz0keXN3Q1VHc-L8TMCOfsjzJn52rJ2evA5K3xCNJOiuO1VBZirY-zRhuBQG-NlwL9Tn-gV5t3hzc_GNwmzCVS-w2J1Hd10hOoGpyyTrPs1N5hjdtDUU9CTowI76j0hNzJB5pbNPBJqkWDnSxT9klID5IvXPEVaghNadnJEH-SzO_aO6Xrh6QTJeVPnrjJM6f9sg8FHeCsQIH3ZPQjYxc647RrjGbzE9Z5MdAQBeXcPy_fl37-bBAIO8Ti9o-GD_QPm4hvH-vYwDF5YY_o5KchNnDeOh1hA7-fXAyZAO0ImcCTQROgksG6kSIAh5n81SBJ0pkCfXTsImPqYhmm9DJMNcwapv6BJabexKRLYSouTX6vxBOUte5574DwyL5-EE29nCWfmMrR3cTXXFL8rgweZaXa0vktr1TlprBMXWUm_KAmTfg9HOae7L32vgjTPPXnv8cA22Q9oUlvU02TCUIlTf-0fSVNOjen3tdClIFK7qM_JNN01HBc3uH9BpMNaqqZ9YWV6EQzG5poZsFEM1tBokkq25Ba49Xh5R1CNqSs2UW8Q9qQstsVQp4jhYd4Iu6Ygyg80NT7-4HCA3-FoZq6A_QeQ6nu0AbWHdqidRtP1KVQZlCu2X2o1XDjOAxs9gsRPH3y3ygpv-djfBiZfYyPYZePf2s1rKEOgXWGiBOklZR_0PWWf7YHH2X4FwdEx0uLQ-lwij4ogo6Kqp4OIXQ9D4e690Jbde79Rq5nIzMi7xf_W8v1_Ciwnnsi1nPRFOYDvGCpe_WYwKuVNDvvqbAeM7sXdR0nZVwm5WAts_MJgJRcBClWXJCSg0jZ6JCSA0jJld50HRyRHvG7x4SN3H3d9_oMVXcN9ba5RtmgTNkNj4v5CMo-Kp1KyYfExTzghvNzPS46fIzK2RV0XrqvL4DXr8MPbaFEKhxyXrqvDzgv3dcXFc4EVFvOi6Y08oACtZ7mGidpgnvK3m2N05R50ldOWxQVQIDu0GTOOW8GAdJqlkQ3BoCU5H6VOs71vxMEHI_gSbpXGZoEck1Hp1fl5fi8Tn6u6PTTygR4mUDSKonSKjgApNcJpygCyquavY2AYmUKJK8HeFCLJMbo2PSrGf3o2DT7r7JSScOsKmrVgZuUiBXC1DCDFcK6Q0x-DK4QJq3yaFXEzBVWv5qusn5VO1IaZkjKx7KH_Rsm9yqkJBQNyDUsE6Bk8vELIggG-kJ8PMDpCIiqZHydBERmWs9yFPCE7Ld3RVulv6ZVtVWWKWzthq0SqG4tBxUBV7hWBAy44iqXu4WVreOgkwbJyjHsXfauiAj82PGkKBoxcWj6X3GKRqxZnS_dX7LbFbyMWedlbuGFuZ4qM7ee0MytB8zceirI3CqEm6sSbi4Ubg6Em6sd3HyVcPOhcPMg3HylcOm3PfjzhL9uXP8ZEycbTLzkX9e239XY02-vbNJX_PL89rtUidiSq6-9trrx7Pt_WNmEVjBcjoSZIELiDvLGnY39bzas9knQXFxILHrjqdWNE_ktdwqJBsJeHtfqF5vmhoUmrtfvWd_t3DUSpntXk-7GxF1ZltDfCMrGxV1PNs5_ja5Qsy6PnK9sitJXI2HIX9nPSV9JIi5_pep0vlLt_g-MjUtW"  # noqa
        )
        cls.reqwizard.on_request(text="samsung galaxy s8 10gb").respond(
            qtree="cHic7V17bBTHGZ9vdn0sg0GWESq51q3rVsmRCnT4lWskmgYhlSZpQlEfcH9hp6lN1UaqT00NaosNSeoS8yhQqAKRoiCDSYkxD7dAzAFFIpQk0l6jpg9FlZoqaaWSplIfqiJUOjP7mpmdu72DA2N7kdDt7XwzfI_f95hv9hbyAKm1auoS81EjpHAa1aMkakJ3o2Z0b62F6hC9j1IojT5bs7xmBVqF1qBu2A7oWUAvADoG6Awg-ucSIBu-lvw5kFXEmWbRaWy5GbmOb-W-83hXEhr9ZWcIy6LliC3b_fY8b1VvhrJ4GmVg6QoL6lDSo2hCKUjDip3YYatnEfFG5vPFM2glHhjJwlBngs5CqbmdFv3Ed-VSRjfqxeuxhfsA0X83eQnIVxXGE10d3-zoXcf4hkaX8YSG8SsLPMbdGTq-H-V8uwQq20uJO-BzDQNZPDTSObMOksbaxyi7nVRlSbiLX5hJWFdClB8DeUgRBXLUpr4QoBHioukJAbkw_8bSNjoLkpBTWb-DQG4-u8e4Rp6auy2HozXg8vSDMEsZERAOSyCx9Ican6VMmCW8tMnCjKVM06y0_yfDuXNRuh75_34_kIcVBvDitKgUrOHgHPE4oMQ6rXyCs0AHBR4Wp4vwsAvIF1QeujoFZ8MauwxbPgtdnTot3MMNQwdVyyQJvRltmqbZ1oa6PpiPG41UTTr0lXGF-TeQvoXGGMqzF7A10NcH8z6eXrgsOYJJtyLvnEK_PWqPF_oLm-wz9vEkLGiCVImYcHX8Dk98ZarOx34B3MkUSlcv-z29bAGiUPheZwycu5zFwyPc96SIYY9Sz3Ouxv2rk_wKJ3HhKe_KHuNX4I4SemXaefuYf3fcn7OJOy5QW3iWMJmuUih7LGFtZ1pE59YtSz6XIMuJF4hxOQE6Ds9xeK4wPH-fPKiy1CqATBedrxo-R60lonOrGJ1b9ZGxqc66ds2o6-szaTAxU0ZadycUbASnwSkzdprYaSbeadoinOYDH2XQVsJp2kSnaYudJnaaqew07RFO81aQadpLOE276DTtN9FpPgDBad4B8nVFoNku8lzUBMLN1qh73-4_voQ9-eSZOvR8m6NHplMt8Xkij_tWqQxLIGLJF_6vM6znfeHfmFHFiPESJl1R24XSgWNa7RaSp83w9so-Tjl9mf49TXk-Kbq6Tl8X3wnykTJVp7ALhqMwmVJV2J2kViQQQoKnBhYUanpWE2Wh-eBSGgPnqV5fPEL_Ut0OH3F0fJB-HqSfB-jnAfp5iH4eYro_IuqfohZ6skWXNgf2nv3VDa19N6mX1y5sLAxKcQ-7cQ96toLMSGGbysgB_o-4xh7zoXLKA0Bhqw-Vkxxc7N6gMsqutkUBjjEvbDVDUU8T5wo1gquP18SuPmGuvh-TtUVcfaw8V5fSjDJXp7GfgeTrY1G-PlbS1wOHdBfyKakfnLshh6wQ04cTAqb3VLPgjTFdGab_VxzT-RvAdL44pp_GEqbzUZjOl5m_8qH8da5a-Utd-obzV4Xucl6s9g7H1d7EuctOU-cuhS3lV3uqu4hzdRrb65d7IqWqMe4GIkGVyzjt0lUq48S1I8u4gHiSlXHDNcJ2dWc1y7i4xzPxPZ5dQL5InI6Ff6iYaw0fbJaIBZReJ0QbF4IOugLsMIODxVyrttWD1qPy-ywxVGOo4lxbhVDVtM0DqLbpoNoWQzWGajWg2l4hVDXN6gCq7Tqott8UqMaHPFMZqpPkkKc54pDniu840FzikKdZPORpvkmHPGbsNLHT3A5O0xLhNH3ByWhLCadpEZ2mJXaa2GmmstNMpmdwYqeJnea2cJrJ9AwOc5ohIhxMbCfxwcSEHUy8rD2YkB5DEX02fg4lfg5FQM8eII-ov4kp9EuI0QXId_0ASanDGDGX3uu0WQr9ASxgDaawaCCYgwGlIYNXwg-zQZiUcf29cNxuDcdtOUoOlnwmHniURKWfia8gRkb1flicfM8S4uTvrDhOTlic3GWSb0Qc4EbFyfgEd8qd4CZ3a36YSANgRIF48-Pfdcab-FhkKm9mtMcizRUei2jau8GxSLPuWKS56sciMVSnJVRbKoSqpqkaQLVFB9WWGKoxVKfrcxExVKclVCfjcxEMqrbpvKzi2tHUsrLaWBEvq4jbWNO1jUUhRV0ihbIHXUjx6LdD-wM95ZHtUmEr3u9Puf2-ixQz-64p5MmCWcU8-QqQrxRPN2gSZZtJcnY2sQfOxU_KMM9yMdCmDNAm9pA2CmgDhgC0f2KyWi3WHYPlIpLe8JbLfs7zpujg8Yzzaz6PRNXyJ4k3IqS3oRGh3Qm9uGdhmEop6j0QFScfysKITO6-PaHn_jC5i9LQlsHJFxJs-RJNmiX6ZSGklzQ4piiIv3Idr-YjJWW8dCBiLxZXydO1Sp5EZx2a8PaWGN5eMeI8Ook7WbF5p3T3R2PeF0HYmP9E8yJWHofQ9YchqEIYMi2TM3sIrL4-ax5bn_G6MqTzjNQ4LEPpmvfnBkrP6JSeiVA64zWRQtkfiX7zL8z9hjGLXGav32-2Q7gCzkUY6dbX5JmImryKLzEuUYEzW5jZYfZisu3gAGcnkBUa4JR-2_Atgo2Z_RtY_T5s3nQwLurVfGLt2rUCs6aG2RNjSY9VTq5j9gnOLB9Wbf4pwm8XK_ylF6HRCkwlLlr2S_V4gkt7VZT2L0AeVaSttY_ZeVrXjdnjhUFBal1S2LH_Vd9npHnFxZfI1NL5ESINB4niqSwMhxTCq79amigShUF7PHgMqjDIRkLxzBF_iyGI_1_Nc2MOA3zF0cK2iA3ipWd3JYINhDRTp4KN3ltfJMrwBkIh0OO4ZxQkwrx9KlDY5qwxNHKW6gMnTbYE1VktvU5QquN2XvMQmV9xB_Uze3DMrZ-3ct3StQqbhOtnhPnFH1Zzq_Mi9ngeLOQEia2aGLsqIsa-GbSzVhWPD9DrqvgNCGJsb_EY67No1VmUxaMiZPYZVUwrJzQid0SIfDJIKx26HchqJ610BCHGy_zQ4Sd-vNWWMj_u-SgbBndY6U4gJfX0aWoW_gJ7OZxX9P56XNH760ukH8dmvxZtdqaaNvuNRnh7NMJox_xDX0qrs1ovtxod1BRs9mhQsG2SCzZ1-OnSw0-Gy73JZNR-UzDqFUMTu0_YY6zLUNhYRuzed2THzCB2SzN1oeQ_XuyWKNXYvYnFZImCO5XT1Tl7WWq4OcE5rzQ7qhBmey5KXNAFTvlceJnByQajYja4BVlCM7_sfOFg4KDo2DuM0AtvRe3n7bEIDPzy9CwdBOhEHQSeC0OAUurSt0RQJH2_GjaSmL6HR84zS50vYimjiKXm3jaW-rtYaP5eV2gGwkcVmpvf3y0UmsI8nZm63UJTIFNtdD-RhsOFZvBkvddyFIrOIgI_iQWB34dS4YmVqBHQPPHn9xIabLKZOqHXq9BkhLrWtCy21Jqmsjl92wVEWSgIH_0lOgaOGg4zu3s7vz3hloHRIaUVI2rrx-h1Et_DJWaj6ubvw4Tdjdz9OeyOiuzuBfIlhV1aPlfGL5-gY_jTzv6PDbscb_Q4_gjht8tn2e_J7APyZZXjxWn-X_n4TRndhlVmmc0owTIbdln-qcAyu10GyySFsm-LPa_Xnd6dtMemYexoREPg9JxAxZRax-_jrorpcHhvyW8rP8G5zoMQFfmECmnSb5vhY4T_zzn1H7IgOdeCef--tuG-hq7-QqbRWPjd_Ib7VIrEvNdee2hJw6l_vJ5pRAuZOB4F9ikOf-5PSxouPGB9RqKgivUpZv52_5IGsmhRQDGXr2HWz7KslTMsXG88vOIx5y7274JwN0FXC9Na_l2PttZd17Tm9CDna6LOEr9SXThfqWr-D_zb6cU,"  # noqa
        )
        cls.reqwizard.on_request(text="купить видеокарту PCI-e x16").respond(
            qtree="cHic7VldbBRFHJ__7F67DJQcrUhdrTZnDCdCbDFWxCCm8tAIYkUweg-KyGkbEjQLkqOacG1zplo-ih_QoA8G0_LdVqCKpAjGigRJ3H3hQxNifCC88GJ8UMTgzOzHze3N7V3likW5pN29mf9_9je__-fOkSfIBK0sPLESVUMU16BypKMImoZmotkTNBRGdBxFUQ16LNQQakTPoaWoCboAbQO0HdB-QEcA0c8JQCYs13_FZA2x1TSqxpa7xdxvHjIPmoPmV-Zhc8BKWm1Wuw7VEah2HkKEh6AGxB7S9PGW8_uw-xjpEr5H16BZUH8JNAgjXSofQVGogcZPsb0FYxMQiZw5UMmBzEKLlI6jJ2N4Z9-ykjDoKKos0-jS2Bzw7oa8u0P8DuvYSrl35iC9I1RTpYt_yUeBzm_0JNujFc5YZ1RpQgkwpssAWe2VDDgDhCgSapxoRZOWQC1YgyQgSpG-A8jTPtKV119u1mFGmmNFwvGPTS7BTFzGZwOnk8067L3vsjeNsFEPGu7oi0GPi89mZeoqui9IYIoUO0g_BPKkDynEuS_McHCCBGfvOBcnxGUoZ3OUEPdjvJNAXLBmT18szSDKwLUZyAI_rgQNA48-GazTXhhAIhuWUv8g1QIdEn5YtxFISG3KES11rZrM5grX1gmxiTkmyMB0lriYqGw2KFx_N1WjPldbFxlf435q6zhAJ7ZbkAshUqatCyehElcr0VANirBHYv6NYg-cZGaJ_QRaRzIJkxFqmacfB7LMtxuypnl5_LUVLxmr39Dh3ghEC00HgqLMHZ7l7iAI-Q1QS4TJEbpwRGV7iKLYDyGti-_u2Np5-uEQaSButsSFZNGAHPo5Ji_6qUrnBRYt7rohCVOXT3orC1oynk7YyVKQ8qfIJUSYrAQvkr6meXF3P_3ro_mxn-XIGN5BrzvotZdee-l1F73uotceeu3pE-IOjHsylnUDFNoywtMv5j4dWjPFhDzYjckLWcR5eV0MZxlxVxakefOUZLztcXnzpPy8pYCQnAWlJ7ugVBSpoCiZBSVNTSSsXb2qhJNJlUaoGlXsCM3waDX2p-jRF2569Bjw6OEAj7Y683n0z49kebTVKePtjM-jrU4_bxuCPHqnQ1BP_6i1SmnPtpmMEAFsvgapIO-_oAreb6lF9H4Lkzd9Rpwiazx5y5YOhbJ8ZTDXIjILf2dbOJeKY-72iGPu-0kuSTnXxkxSKVOg_1sFDe4B1hauE6I6ei4da4v0OaEW5ATGB5KGLZ6vYRv1PjKvo6nU0Y6C4Gi7gSzy7UOlLMcFZ1MlOxk-PcndCheX7eYJvhs-7d_QfYQPj7z1oS3eDb8DZoO_FGEHF5UiBnsvkEbpu1g6sMfGq9hpIE_5Xy_iGe88WPbOs8vbOpWW4VzLcdJJP8w5hA6mi15HTOlOV9E0TDvlx9ldKYvGqbyXwUYVV89RWguPQRwuofa_hAX7n8VkiY-JEHOuhOAEsvL6y7Z2LxfbCjI-frMzry3g52Q6sccpLfmNZ8z1S1MScaEcsgUez1rgnRj-5EAMPstcI76ca9KXV7bEOLqY0rzyVWeR6qxFkoItsuON8X3OfRVkfA9LXmzNQYFsWe6-WO_5nTko47nZ9jtz0N-6LCB00HMc3LH5Wjo9oalQaV5jueQLsXHYXszGQV7khExy87BkDB2WZOWd7CMRwXM06jn7xCrUXcwqNEY9pxvIM8RmxSuOCW6mwHPKjK6XKcjQPWTXRzrr4HtPdfDdTthojpcEVFDVsO1Fs1hrYBazUnmy2PkSz-OsVEAWs1KSLGalRiWLlfAMvRe0ZFKbzOCMZTMxuFoUxTYogil-xzx0GF7k4P3noTMamUQZUSbRu4DMlyXYIM8Kzq_4GvNrQCor4aGxQUxlRbVH8cn4LxSbfPb4RoyPPqWI9jg-CnlP4XkPrkveu5GsuJFZscux4h-4iA3COrJQsnehQZBt_koocPPAN4_ybN5hf1Pxm0jgcY1GHNcFdQC2QYZFg_QXs2P7FrItQsMqT8s25vqJHI6Vdaowao5VkBn3gmNG1u1stQ-wMrqd2jqxxORvdqh8QLNDZx36u4Vmh44W1OxwuF2BcBMZeevf7c3U2IDI7kdAFvvgqlZqZHi5ggzww_YBIZt2ELe5iO8gfLhAyKPXTsraFyNPtbyseGnOCGhfDEmaM2709kXj9jgmvmntAfK8D2_pqubE6nh8pQC6VELk9uQZj0pXReZH87gfuRJ-WiPEnUkfP6ZyHj-6W7i-HZg5lK8DK0ufnw0FdGDmkOz8bOj_14HZVtyKBSu2YZLw4b3V6jAHrVarzTxiDpgHrbfNAavNWi_sYXy-7JZjCZmfvsX9NIeC327zSQ5B0YthJztu5r-tOr_Msi58vXsQzH6DzTow0AqrocYIi5IRWJQMaVEyCsjwWmFFyRwaYVFiCgFFiU1LihIbLgwy_fYu3EX479PlUzTQK7SSyedaFs-tQig5txrNYM-1JehGPInaBw48WrWwc0WmBC1znsT3p07NqXpl0u45nkQFX0MtH69pi0o1KFcWNsbtUaaXOTqBI1LLVW2igeyvTIh_pej_BrpegCU,"  # noqa
        )
        cls.reqwizard.on_request(text="15-rb012ur ноутбук hp amd e2j").respond(
            qtree='cHic7VltbBzFGZ53dn3eTC7WYeMSthgsQ8WBcOWcFBEBiouLqrThI4Sqotf-IKecsI1CwhVECEK5JAS5-bJJ0oAKlSpSf0XhYtK4TdLYMS2VKKjSnoSg_VE-_kWAoK1aRPlReGd2d252du7WbqyqlTjp7nZn3pl95pnZ933mHfYdlnaWZVqWk07I0h7SSlzSRa4nOXJT2iEZguUkS3rIN5rWNK0j95L7SD-MAPkpkBeAnAQyAwQ_rwLxYKO7nd3J_FYOtuK90RUrXeiWHVLRIYgOyRrCO-yfag77Q2Otxx6yCvqu5s1crOxa2hN-VqzMQg-EgLaRMhDsxZ0E9kPGH0MlguZSoWdF7pGSC51d0B3gaFYGFuJ4_sjbL9IQStjKhKfXAcQTWnQRgeSg7UMpXcXCmuW8hqwi60khhQ1Itq3f2Uok1hcpu19jq8Wb8Waru6o7vZequ7yzAnJnA8j_Xhni1VqaYP8aBG7NMoB_lAbw9wPTLJaLZ-IwrKG51_J0opKno5VwRAWHT403lbWCq3Py6rS4oi6t7g6vvGlxBUEtwyvbm_VOytJzss2urNVPtsI26kDA11Fgd-mrq39LhCNq4OjIJXJ59W8x8fItQQtWBlQcCqm4jmGhHD0dquRhNDrua38kUFJESQOUZ4DdraG0NmzaGIFpGWB63wxhcnMTzrzAyWt1oLcwXiqRwlB8hjjSQjMyC_4FceHaGPYjwG7XsEMR3YEEDgbgY0tC4FCMw7b6bsJW4EJRB30lg6KytEYr-dprEsC6L5z6J9haHVdOcVNg8CofylcZcnFYTX1duFgQVk7xKT25qE-Rjx-B-PMHY8-P0vKnK-TzB-PPp30rfVoGdVouZzBodBwRRrp8H0c7LW7ZtczZnimDuG1a6G20p8gdX3b5_Zazo1yGdjL32G3up5StCfwrCZhIiBcNooWJ1lICrZ9ZklaDb7ZCWksGWs3-OLrQysDuMEQw0jCA7Us3DGBUBDBICGDmmeUzEJTbHFXWzr-gzsd-i23V8H7FO18dRt894015p6pPeVPVndW93hllDEuTgl-dLkxO6SkqvFKdFnpweQNYHcua89qTtycqGGis0cpcIY0eK4WWv_RmZWQYxiu_fFYtV2LLGS2eED-eBO0PiMhDXRtDXO16b6R9m94-Fs3ARxKNUXKShqkySZ8Be1ibpEsNLOxTpoglTZGxA9MEbRXzYzTXZ-fbzGhWm5rdeZjgMbAWwX0e9oVRhvNZh5HjgIyMICP4cZ8Btl6TaVYp8p6ZQmSEAG5vGu-NfpTEWl2YfZXx0saiTMKdsZ1yOeOjnbSF0_OdG71Ip3fEoGJKBR5kuxuIGG-HHDhamxzfjcLxYaXu-VyGhfNwfU8CW6cLF5SxsTmJ-r7ZMemQubUp0l4jvB-vVd1fvVhr4scX76QBP6f_VuPHJNppyM8jsciA_NSR6o0ibn0vjeVZkn-nqbZ-Xm9axPUzbNBoEXoWHjVBRE2y4KgZzNhhA6JCkmp8Xm76wLCcQahGRBRbzagaCwmq8cv1PK_1nHE-_9zKlMs2rmI7ayUrwcgKt_O_VTxkZTE95HN-cFAZs6tl76WYCotStmf_prBLYW5aVTeLVSWq9eB3BRPFRuLUXeiXy2rx3KSdfxecIamTXgf2fW1MzoObHy4WNm9-QOHXMQzscH84LtnCNP8bxfxLE32MtzJZddGbaRyinbFxiE-qev1jyh6KScGzKGTPofLaVz2A2mu6Wq7uTZCC7y-t6UBDa9PQP4ZACBrs9XfhaWCtcTs9D3QCiTkREGP9B3mghOxPoC33CscOpRuYEXodOafqT38axlVFfpCyH-grLUx6KdTbBuovTB-Xrku2MRE-4RMubXSS0ZHIOkmtPfTc3B8uMr82X17jSt1n6heWUx4KmRq22CaNqdZoepA_SeFsiYGzDy8NGTO0NXF3yN9VGqx1Fn-clK4cu5glakpVtplIFUv0emYAbA4opa_jRjhmG2yEa_Y0HoDkNJ0MXSfZdpv7M2Df1WMmbo7Pu3BdF2QbRIS_fyRTyKJBg6jJqw1RkxcnRU0EncqkUBZPgfIWmgK9N1MtJwT6wyNLapDRvBFkrDZBnhHebD6Q7fxvVJ7H4iGKpyfOeaciTBt3rxMHpQYI2tQ_XAgMdOxXsqBiHuidjIOEf2A5P5eEv2ktokwbBXZvPSo6G-_j_5tMuL-H-KGUN520P7nQJ4WSN21CN-CfGnjTOrLbGRYqpwZP5-kxDJXHKn7I5Mcn4_g_XuG-Cb_4P4n_k_xY5YTq-jX3HNPrETnlZBiu1T-rcuoVQy5TDLzRTvF_b9w2KiA-tgOWMyLH9q_FzEP_xJCHLibw9H956kEXcurRQLuzTBonBAXOiMzoHfK3RWpGjxZzCZ48ktBD8_qpCazU03m4ISnmErN5PtTjgHtVx0f6LLB79NxjMTeoOoTk5CM2aJB8xFpD8hFL5wG3BZ32uBolDxpe44GBBGYv3CppHRiojxQrDfu8gYGknA8iTQtiP1FV9XuUFTSkaRQ1O1HmlFHeqEcQKQPmT361LAQdaWaC_1dfXEfsdIf0KrBIvXrEYB2rnM9b4_xnjP9M4o9y7CCOBLypeR01xI4KFnTUsPADBp_4f6hL5B1g34uF4lPeSW8qIaP-7FvLaoFYNDCR_XgQiIWBznKeBRXqMYE1waX36By6_Zfx_2VlNwPqLgWFb422YZUA87Dxbg9cxYQEbr3MAbfNSbW3736gt4OQcm8n6ebIQ4uUtLik0tzb8faGDasjFilpAe1_PLqlt-P02uqqTqv70dntvbpFqp29dfXqjr_cMbYq0geKLNnHjg-qqzvOPvi1W7CP3_2zH_toE304rUsdZ32zA63WneuKfim-50EpVUrTsjS0TTNxINFqOy0l4t_ygSm3HIFyi_5OvW3BHsUtkvgFRE170g,,'  # noqa
        )

    def test_multitoken_factors(self):
        """
        Проверяем, что факторы вычисленные по мультитокенам считаются верно
        MARKETOUT-31116
        """
        response = self.report.request_json('place=prime&text=abcd samsung abcd galaxy s10 8gb&debug=da')

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "bla bla xiaomi galaxy s10 8gb xiaomi"},
                "debug": {
                    "factors": {
                        "REQUEST_CONTAINS_MULTITOKEN": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_RATIO": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO": "1",
                        "MULTITOKEN_MATCH_TITLE_STR_RATIO": "1",
                    }
                },
            },
        )

        response = self.report.request_json('place=prime&text=abcd samsung abcd galaxy s10 s20&debug=da')

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "bla bla xiaomi galaxy s10 8gb xiaomi"},
                "debug": {
                    "factors": {
                        "REQUEST_CONTAINS_MULTITOKEN": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_RATIO": "0.5",
                        "MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO": "1",
                        "MULTITOKEN_MATCH_TITLE_STR_RATIO": "0.5",
                    }
                },
            },
        )
        response = self.report.request_json('place=prime&text=samsung galaxy s8 10gb&debug=da')

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "bla bla xiaomi galaxy s10 8gb xiaomi"},
                "debug": {
                    "factors": {
                        "REQUEST_CONTAINS_MULTITOKEN": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_RATIO": NoKey("MULTITOKEN_COMPLETELY_MATCH_RATIO"),
                        "MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO": NoKey("MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO"),
                        "MULTITOKEN_MATCH_TITLE_STR_RATIO": NoKey("MULTITOKEN_MATCH_TITLE_STR_RATIO"),
                    }
                },
            },
        )

        response = self.report.request_json('place=prime&text=Купить оффшор на Кипре&debug=da')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Купить оффшор на Кипре"},
                "debug": {
                    "factors": {
                        "REQUEST_CONTAINS_MULTITOKEN": NoKey("REQUEST_CONTAINS_MULTITOKEN"),
                        "MULTITOKEN_COMPLETELY_MATCH_RATIO": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO": "1",
                        "MULTITOKEN_MATCH_TITLE_STR_RATIO": "1",
                    }
                },
            },
        )

        #  pci-e и pcie не матчатся так как это синонимы:
        response = self.report.request_json('place=prime&text=купить видеокарту PCI-e x16&debug=da')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Видеокарта PNY Quadro 2000 625Mhz PCIE 2.0 1024Mb 2600Mhz 128 bit DVI"},
                "debug": {
                    "factors": {
                        "REQUEST_CONTAINS_MULTITOKEN": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_RATIO": NoKey("MULTITOKEN_COMPLETELY_MATCH_RATIO"),
                        "MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO": "0.5",
                        "MULTITOKEN_MATCH_TITLE_STR_RATIO": NoKey("MULTITOKEN_MATCH_TITLE_STR_RATIO"),
                    }
                },
            },
        )

        # запрос с длинными мультитокенами: они должны считаться как один
        response = self.report.request_json('place=prime&text=15-rb012ur ноутбук hp amd e2j&debug=da')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "15 ноутбук rb012ur ноутбук hp amd 2j"},
                "debug": {
                    "factors": {
                        "REQUEST_CONTAINS_MULTITOKEN": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_RATIO": NoKey("MULTITOKEN_COMPLETELY_MATCH_RATIO"),
                        "MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO": "0.5",
                        "MULTITOKEN_MATCH_TITLE_STR_RATIO": NoKey("MULTITOKEN_COMPLETELY_MATCH_RATIO"),
                    }
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "E2j ноутбук hp amd E2j E2j"},
                "debug": {
                    "factors": {
                        "REQUEST_CONTAINS_MULTITOKEN": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_RATIO": "0.5",
                        "MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO": "1",
                        "MULTITOKEN_MATCH_TITLE_STR_RATIO": "0.5",
                    }
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "e2j1 ноутбук hp amd e2j34 e2jwe"},
                "debug": {
                    "factors": {
                        "REQUEST_CONTAINS_MULTITOKEN": "1",
                        "MULTITOKEN_COMPLETELY_MATCH_RATIO": "0.5",
                        "MULTITOKEN_COMPLETELY_MATCH_ANN_RATIO": "1",
                        "MULTITOKEN_MATCH_TITLE_STR_RATIO": NoKey("MULTITOKEN_MATCH_TITLE_STR_RATIO"),
                    }
                },
            },
        )

    @classmethod
    def prepare_cgi_has_glfilter_factor(cls):

        cls.index.offers += [
            Offer(fesh=1, hyperid=11200, title="test model free", glparams=[GLParam(param_id=101, value=1)])
        ]
        cls.index.models += [
            Model(hyperid=11200, hid=112, title="test model", glparams=[GLParam(param_id=101, value=1)])
        ]

        cls.index.gltypes = [GLType(param_id=101, hid=112)]

    def test_cgi_has_glfilter_factor(self):

        # В рамках MARKETOUT-33684
        # Проверяем, что при указании gl-фильтра через cgi-параметр в факторах появляется REQUEST_CGI_HAS_GLFILTER

        # Запрос без glfilter
        response = self.report.request_json('place=prime&hid=112&text=test+model&debug=da')
        self.assertFragmentIn(
            response, {"debug": {"factors": {"REQUEST_CGI_HAS_GLFILTER": NoKey("REQUEST_CGI_HAS_GLFILTER")}}}
        )

        # Запрос с текстом и c glfilter
        response = self.report.request_json('place=prime&hid=112&text=test+model&glfilter=101:1&debug=da')
        self.assertFragmentIn(response, {"debug": {"factors": {"REQUEST_CGI_HAS_GLFILTER": "1"}}})

        # Запрос без текста и c glfilter
        response = self.report.request_json('place=prime&hid=112&glfilter=101:1&debug=da')
        self.assertFragmentIn(response, {"debug": {"factors": {"REQUEST_CGI_HAS_GLFILTER": "1"}}})

    @classmethod
    def prepare_cgi_has_cpa_factor(cls):
        cls.index.shops += [
            Shop(fesh=123, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве'),
        ]
        cls.index.offers += [
            Offer(fesh=123, title="some offer", cpa=Offer.CPA_REAL),
        ]

    def test_cgi_has_cpa_factor(self):
        """
        Проверяем что при указании в cgi cpa фильтра
        в факторы пробрасывается REQUEST_CGI_HAS_CPA
        https://st.yandex-team.ru/MARKETOUT-36606
        """
        response = self.report.request_json('place=prime&text=some offer&debug=da')
        self.assertFragmentIn(response, {"debug": {"factors": {"REQUEST_CGI_HAS_CPA": NoKey("REQUEST_CGI_HAS_CPA")}}})

        response = self.report.request_json('place=prime&text=some offer&cpa=real&debug=da')
        self.assertFragmentIn(response, {"debug": {"factors": {"REQUEST_CGI_HAS_CPA": "1"}}})

    # def test_superembed_dssm(self):
    # """
    # Проверяем, что при включенном флаге суперэмбеда
    # запросная часть считается на сконкатенированных векторах
    # """

    # response = self.report.request_json(
    # 'place=prime&text=dssmoffer3&debug=da'
    # '&rearr-factors=market_use_super_embed=1'
    # )

    # superembed_query = [0.2, 0.3, 0.5] + [0.1] * 47
    # self.assertFragmentIn(response, {"logicTrace": [Contains("Embedding for query by superembed equal: %s" % str(superembed_query))]})

    # self.assertFragmentIn(
    # response,
    # {
    # 'results': [
    # {
    # 'titles': {'raw': 'dssmoffer 2'},
    # 'debug': {
    # 'factors': {
    # "SUPER_EMBED_DSSM": Round(0.7018),
    # "SUPER_EMBED_DOC_EMBEDDING_0": Round(0.20249),
    # "SUPER_EMBED_DOC_EMBEDDING_1": Round(0.20249),
    # "SUPER_EMBED_DOC_EMBEDDING_37": Round(0.1009),
    # }
    # },
    # },
    # {
    # 'titles': {'raw': 'dssmoffer 1'},
    # 'debug': {
    # 'factors': {
    # "SUPER_EMBED_DSSM": Round(0.7067),
    # "SUPER_EMBED_DOC_EMBEDDING_0": Round('0.1009'),
    # "SUPER_EMBED_DOC_EMBEDDING_10": NotEmpty(),
    # "SUPER_EMBED_DOC_EMBEDDING_37": NotEmpty(),
    # }
    # },
    # },
    # ]
    # },
    # )

    def test_category_stupid_dssm(self):
        """
        Проверяем корректность рассчета category_stupid
        """

        response = self.report.request_json('place=prime&text=dssmoffer3&debug=da')

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_IS_STUPID": Round(0.841),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'dssmoffer 2'},
                        'debug': {
                            'factors': {
                                "DSSM_IS_STUPID": Round(0.841),
                            }
                        },
                    },
                ]
            },
        )

    def test_assessment_binary_dssm(self):
        """
        Проверяем корректность рассчета assessment_binary
        """

        response = self.report.request_json('place=prime&text=dssmoffer3&debug=da')

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'Купи 3 пирожка - собери котенка'},
                        'debug': {
                            'factors': {
                                "DSSM_DISTIL_ASSESSMENT_BINARY": Round(0.654),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'dssmoffer 2'},
                        'debug': {
                            'factors': {
                                "DSSM_DISTIL_ASSESSMENT_BINARY": Round(0.668),
                            }
                        },
                    },
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {
                            'factors': {
                                "DSSM_DISTIL_ASSESSMENT_BINARY": Round(0.749),
                            }
                        },
                    },
                ]
            },
        )

    @classmethod
    def prepare_hype_goods_factor(cls):
        cls.index.offers += [
            Offer(title="hype offer", ts=55450, hyperid=55450, hid=447, glparams=[GLParam(param_id=27625090, value=1)]),
            Offer(title="standard offer"),
        ]

    def test_hype_goods_factor(self):
        response = self.report.request_json('place=prime&text=hype offer&debug=da&allow-collapsing=0')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'hype offer'},
                        'entity': 'offer',
                        'debug': {
                            'factors': {
                                "HYPE_GOODS": "1",
                            }
                        },
                    },
                ]
            },
        )

        # Для схлопнутых моделей фактор тоже должен выставляться
        response = self.report.request_json('place=prime&text=hype offer&debug=da&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'type': 'model',
                        'debug': {
                            'isCollapsed': True,
                            'offerTitle': 'hype offer',
                            'factors': {
                                "HYPE_GOODS": "1",
                            },
                        },
                    },
                ]
            },
        )

        response = self.report.request_json('place=prime&text=standard offer&debug=da&allow-collapsing=0')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'standard offer'},
                        'entity': 'offer',
                        'debug': {
                            'factors': {
                                "HYPE_GOODS": NoKey("HYPE_GOODS"),
                            }
                        },
                    },
                ]
            },
        )

    @classmethod
    def prepare_opinion_factors(cls):

        cls.index.models += [
            Model(
                hyperid=101,
                opinion=Opinion(
                    total_count=10, positive_count=6, rating=4.5, precise_rating=4.71, rating_count=15, reviews=5
                ),
            ),
            Model(hyperid=102),
        ]

        cls.index.shops += [
            Shop(fesh=1001, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=1002, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(hyperid=101, fesh=1001, price=300, cpa=Offer.CPA_REAL),
            Offer(hyperid=102, fesh=1001, price=300, cpa=Offer.CPA_REAL),
        ]

    def test_opinion_factors(self):
        """Проверяем факторы связанные с отзывами на модель"""

        factors = {
            'MODEL_RATING_2': Round(4.5),
            'MODEL_RATING_PRECISE_2': Round(4.71),
            'MODEL_RATING_COUNT': '15',
            'MODEL_OPINIONS_COUNT': '10',
            'MODEL_REVIEW_COUNT': '5',
            'MODEL_POSITIVE_OPINIONS_PART': Round(6.0 / 10.0, 2),
        }
        response = self.report.request_json('place=prime&modelid=101&debug=da')
        self.assertFragmentIn(response, {'entity': 'product', 'id': 101, 'debug': {'factors': factors}})
        self.assertFragmentIn(response, {'entity': 'offer', 'model': {'id': 101}, 'debug': {'factors': factors}})

        response = self.report.request_json('place=prime&modelid=102&debug=da')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 102},
                'debug': {
                    'factors': {
                        'MODEL_RATING_2': NoKey('MODEL_RATING_2'),
                        'MODEL_RATING_PRECISE_2': NoKey('MODEL_RATING_PRECISE_2'),
                    }
                },
            },
        )

    @classmethod
    def prepare_compatible_with_ios_factor(cls):
        APPLE_VENDOR_ID = 153043
        cls.index.offers += [
            Offer(title='apple', vendor_id=APPLE_VENDOR_ID),
            Offer(title='galaxy', vendor_id=153061),
        ]

    def test_compatible_with_ios_factor(self):
        response = self.report.request_json('place=prime&text=apple&debug=da&allow-collapsing=0&client=IOS')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'factors': {
                                "REQUEST_IOS_COMPATIBLE": "1",
                            }
                        },
                    },
                ]
            },
        )
        response = self.report.request_json('place=prime&text=galaxy&debug=da&allow-collapsing=0&client=IOS')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'factors': {
                                "REQUEST_IOS_COMPATIBLE": NoKey('REQUEST_IOS_COMPATIBLE'),
                            }
                        },
                    },
                ]
            },
        )

    @classmethod
    def prepare_reqwizard_category_matching(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=45612,
                uniq_name="Одежда, обувь аксессуары",
                children=[
                    HyperCategory(
                        hid=1045612,
                        uniq_name="Детская одежда",
                        children=[
                            HyperCategory(
                                hid=2045612,
                                uniq_name="Верхняя одежда для девочек",
                                children=[
                                    HyperCategory(
                                        hid=3045612,
                                        uniq_name="Куртки и пуховики для девочек",
                                    )
                                ],
                            ),
                        ],
                    ),
                ],
            ),
        ]

        # todo: add offer in my categ and other categ
        cls.index.offers += [
            Offer(title="Куртка Legla", hid=3045612),
            Offer(title="Детская сумка", hid=45612 + 100),
        ]

        cls.reqwizard.on_request("десткая одежда legla").respond(
            found_main_categories=[1045612],
            found_extra_categories=[7812030, 7812090, 1045612, 7812131],
        )

    def test_reqwizard_category_matching(self):
        """Проверяем факторы матчинга запроса и категории в бегемоте
        https://st.yandex-team.ru/MARKETOUT-45612
        """
        response = self.report.request_json('place=prime&text=десткая одежда legla&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'factors': {
                                'IS_CATEGORY_IN_MAIN_CATEGORIES_DIV_MATCHED_MAIN_CATEGORIES_COUNT': '1',
                                'IS_CATEGORY_IN_EXTRA_CATEGORIES_DIV_MARCHED_EXTRA_CATEGORIES_COUNT': Round(0.25),
                            }
                        },
                    },
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                'categories_ranking_json': [
                    {
                        'id': 1045612,
                        'name': 'Детская одежда',
                        'factors': {
                            'IS_CATEGORY_IN_MAIN_CATEGORIES_DIV_MATCHED_MAIN_CATEGORIES_COUNT': 1,
                            'IS_CATEGORY_IN_EXTRA_CATEGORIES_DIV_MARCHED_EXTRA_CATEGORIES_COUNT': Round(0.25),
                        },
                    },
                    {
                        'id': 2045612,
                        'name': 'Верхняя одежда для девочек',
                        'factors': {
                            'IS_CATEGORY_IN_MAIN_CATEGORIES_DIV_MATCHED_MAIN_CATEGORIES_COUNT': NoKey(
                                'IS_CATEGORY_IN_MAIN_CATEGORIES_DIV_MATCHED_MAIN_CATEGORIES_COUNT'
                            ),
                            'IS_CATEGORY_IN_EXTRA_CATEGORIES_DIV_MARCHED_EXTRA_CATEGORIES_COUNT': NoKey(
                                'IS_CATEGORY_IN_EXTRA_CATEGORIES_DIV_MARCHED_EXTRA_CATEGORIES_COUNT'
                            ),
                        },
                    },
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=десткая сумка&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {
                            'factors': {
                                "IS_CATEGORY_IN_MAIN_CATEGORIES_DIV_MATCHED_MAIN_CATEGORIES_COUNT": NoKey(
                                    'IS_CATEGORY_IN_MAIN_CATEGORIES_DIV_MATCHED_MAIN_CATEGORIES_COUNT'
                                ),
                                "IS_CATEGORY_IN_EXTRA_CATEGORIES_DIV_MARCHED_EXTRA_CATEGORIES_COUNT": NoKey(
                                    'IS_CATEGORY_IN_EXTRA_CATEGORIES_DIV_MARCHED_EXTRA_CATEGORIES_COUNT'
                                ),
                            }
                        },
                    },
                ]
            },
        )

    def test_pictures_dssm(self):
        """
        Проверяем, что при включенном флаге картиночной дссм i2t_v12
        запросная часть считается
        """
        response = self.report.request_json(
            "place=prime&text=dssmoffer3&debug=da&rearr-factors=market_use_pictures_dssm=1"
        )

        query_embedding = [0.3, 0.45, 0.87, -0.44] + [0.1] * 196
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("Embedding for query [dssmoffer3] by model Pictures equal: %s" % str(query_embedding))
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'dssmoffer 1'},
                        'debug': {'factors': {"DSSM_I2T_V12": Round(0.657)}},
                    },
                    {
                        'titles': {'raw': 'dssmoffer 2'},
                        'debug': {'factors': {"DSSM_I2T_V12": Round(0.6508)}},
                    },
                ]
            },
        )

        # проверяем наличие у синего оффера (берется от ску)
        response = self.report.request_json(
            "place=prime&text=blue dssm&debug=da&rearr-factors=market_use_pictures_dssm=1"
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'blue dssm offer'},
                        'debug': {'factors': {"DSSM_I2T_V12": Round(0.6436)}},
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()

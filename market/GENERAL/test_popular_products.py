#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.types import (
    CategoryStatsRecord,
    Currency,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    Model,
    ModelDescriptionTemplates,
    Offer,
    Opinion,
    Region,
    Shop,
    Tax,
    Vat,
    YamarecMatchingPartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.matcher import ElementCount, EmptyList
from core.report import DefaultFlags
from core.bigb import SkuPurchaseEvent, BeruSkuOrderCountCounter


# fragment of hardcoded accessory categories file
ACCESSORY_CATEGORIES = {
    1001410: [15561854, 90906, 90987, 90976, 90996, 90928, 90854, 90983, 90967, 90905],
    1001393: [6203660, 90569, 454690, 13239041, 90567, 90586, 6203657, 766157, 90570, 91161],
}


class _Offers(object):
    waremd5s = [
        'Sku1Price500-LVm1Goleg',
        'Sku2Price50-iLVm1Goleg',
        'Sku3Price45-iLVm1Goleg',
        'Sku4Price36-iLVm1Goleg',
        'Sku5Price15-iLVm1Goleg',
        'Sku6Price16-iLVm1Goleg',
        'Sku7Price11-iLVm1Goleg',
        'Sku8Price12-iLVm1Goleg',
        'Sku9Price10-iLVm1Goleg',
    ]
    feed_ids = [4] * len(waremd5s)
    prices = [500, 50, 45, 36, 15, 16, 11, 12, 10]
    discounts = [9, 10, 45, 36, 15, None, None, None, None]
    shop_skus = ['Feed_{feedid}_sku{i}'.format(feedid=feedid, i=i + 1) for i, feedid in enumerate(feed_ids)]
    model_ids = list(range(1, len(waremd5s) + 1))
    sku_offers = [
        BlueOffer(price=price, vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, waremd5=waremd5, discount=discount)
        for feedid, waremd5, price, shop_sku, discount in zip(feed_ids, waremd5s, prices, shop_skus, discounts)
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Набор тестов для популярных товаров
        """

        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.settings.rgb_blue_is_cpa = True

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=200,
                children=[
                    HyperCategory(hid=201, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=202, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=203, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=204, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=205, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ACCESSORY_CATEGORIES,
                kind=YamarecPlace.Type.MATCHING,
                partitions=[
                    YamarecMatchingPartition(matching=ACCESSORY_CATEGORIES, splits=['*']),
                ],
            )
        ]

        # shops
        cls.index.shops += [
            # blue shop
            Shop(
                fesh=431782,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=4, datafeed_id=4, priority_region=213, currency=Currency.RUR, blue=Shop.BLUE_REAL, warehouse_id=145
            ),
            # green shop
            Shop(fesh=2, priority_region=213),
        ]

        cls.index.models += [
            # models with 'category duplicates'
            Model(hyperid=1, hid=201),
            Model(hyperid=2, hid=201),
            Model(hyperid=3, hid=201),
            Model(hyperid=4, hid=202),
            Model(hyperid=5, hid=202),
            Model(hyperid=6, hid=203),
            Model(hyperid=7, hid=204),
            Model(hyperid=8, hid=205),
            Model(hyperid=9, hid=201),
            Model(hyperid=10, hid=201),
            Model(hyperid=11, hid=201),
            Model(hyperid=12, hid=202),
            Model(hyperid=13, hid=202),
            Model(hyperid=14, hid=203),
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                ts=hyperid * 1000,
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku='{i}'.format(i=hyperid),
                waremd5='Sku{i}-wdDXWsIiLVm1goleg'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(_Offers.model_ids, _Offers.shop_skus, _Offers.sku_offers)
        ]

        # green offers
        cls.index.offers += [Offer(fesh=2, hyperid=hyperid, ts=hyperid * 1000) for hyperid in range(1, 17)]

        # personal categories ordering
        ichwill_answer = {
            'models': ['9', '10', '11', '12', '13', '14', '7', '8'],
            'timestamps': ['8', '7', '6', '5', '4', '3', '2', '1'],
        }
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1001').respond(ichwill_answer)
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1001', item_count=40, with_timestamps=True
        ).respond(ichwill_answer)

    def test_fesh_filtering(self):
        """
        Проверка фильтрации по магазину
        """

        response = self.report.request_json(
            'place=popular_products&numdoc=100&rids=213&yandexuid=1001&rgb=green&dj-output-items=offers&fesh=431782&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'offer', 'shop': {'id': 431782}},
                        {'entity': 'offer', 'shop': {'id': 431782}},
                        {'entity': 'offer', 'shop': {'id': 431782}},
                        {'entity': 'offer', 'shop': {'id': 431782}},
                        {'entity': 'offer', 'shop': {'id': 431782}},
                    ]
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=popular_products&numdoc=100&rids=213&yandexuid=1001&rgb=green&dj-output-items=offers&fesh=2&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'offer', 'shop': {'id': 2}},
                        {'entity': 'offer', 'shop': {'id': 2}},
                        {'entity': 'offer', 'shop': {'id': 2}},
                        {'entity': 'offer', 'shop': {'id': 2}},
                        {'entity': 'offer', 'shop': {'id': 2}},
                    ]
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_ordering(self):
        """
        Проверка ранжирования результатов: порядок персональных категорий должен быть сохранён,
        """

        """
        Для всех цветов маркета порядок категорий сохранен
        """
        response = self.report.request_json(
            'place=popular_products&numdoc=100&rids=213&yandexuid=1001&rgb=green&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 201}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 202}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 203}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 204}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 205}]},
                    ]
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )
        """При этом для синего маркета или при наличии фильтра
           нет ограничения на количество результатов для категории,
           но категории должны чередоваться
        """
        response = self.report.request_json(
            'place=popular_products&numdoc=8&rids=213&hid=200&yandexuid=1001&hid=200&rgb=green&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 201}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 202}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 203}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 204}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 205}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 201}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 202}]},
                        {'entity': 'product', 'type': 'model', 'categories': [{'id': 203}]},
                    ]
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&numdoc=8&rids=213&hid=200&yandexuid=1001&{}'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 201}]},
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 202}]},
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 203}]},
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 204}]},
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 205}]},
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 201}]},
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 202}]},
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 201}]},
                        ]
                    },
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_guru_categories(cls):
        """
        Несколько категорий из файла со списком аксессуарных категорий
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=9300,
                children=[
                    HyperCategory(hid=1001393, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=6203660, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=90569, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=454690, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=13239041, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=90567, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=90586, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=6203657, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=766157, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=90570, output_type=HyperCategoryType.GURULIGHT),
                    HyperCategory(hid=91161, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=61, hid=1001393),
            Model(hyperid=62, hid=91161),
        ]

        sku_offers = [
            BlueOffer(price=price, vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid + 0, discount=discount)
            for feedid, price, shop_sku, discount in zip(
                _Offers.feed_ids, _Offers.prices, _Offers.shop_skus, _Offers.discounts
            )
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                ts=hyperid * 1000,
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku='{i}'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(list(range(61, 63)), _Offers.shop_skus, sku_offers)
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1008').respond({'models': ['61']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1008', item_count=40, with_timestamps=True
        ).respond({'models': ['61'], 'timestamps': ['1']})

    def test_guru_categories(self):
        """
        Проверка фильтра категорий на этапе определения персональных категорий
        Есть кандидаты из списка аксессуаров к элементу истории- не guru категории,
        они должны отфильтровываться и при этом замещаться следующими guru категориями из списка
        """
        for param in ['&rgb=green', '&rgb=blue', '', '&cpa=real']:
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&numdoc=100&rids=213&hid=9300&yandexuid=1008{}'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 2,
                        'results': [
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 1001393}]},
                            {'entity': 'product', 'type': 'model', 'categories': [{'id': 91161}]},
                        ],
                    }
                },
                preserve_order=False,
            )

    def test_position(self):
        _ = self.report.request_json(
            'place=popular_products&numdoc=100&rids=213&hid=200&yandexuid=1001&page=1&numdoc=2&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.show_log.expect(shop_id=99999999, position=0)
        self.show_log.expect(shop_id=99999999, position=1)
        _ = self.report.request_json(
            'place=popular_products&numdoc=100&rids=213&hid=200&yandexuid=1001&page=2&numdoc=2&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.show_log.expect(shop_id=99999999, position=2)
        self.show_log.expect(shop_id=99999999, position=3)
        _ = self.report.request_json(
            'place=popular_products&numdoc=100&rids=213&hid=200&yandexuid=1001&page=3&numdoc=2&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.show_log.expect(shop_id=99999999, position=4)

    def test_missing_pp(self):
        response = self.report.request_json(
            'place=popular_products&yandexuid=1001&ip=127.0.0.1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0',
            strict=False,
            add_defaults=DefaultFlags.BS_FORMAT,
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    @classmethod
    def prepare_glparam(cls):

        cls.index.hypertree += [
            HyperCategory(
                hid=9500,
                children=[
                    HyperCategory(hid=9501, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.gltypes = [
            GLType(param_id=501, hid=9501, gltype=GLType.ENUM),
            GLType(param_id=502, hid=9501, gltype=GLType.BOOL),
            GLType(param_id=503, hid=9501, gltype=GLType.NUMERIC),
        ]

        cls.index.models += [
            # models with 'category duplicates'
            Model(
                hyperid=5001,
                hid=9501,
                glparams=[
                    GLParam(param_id=501, value=601),
                    GLParam(param_id=502, value=1),
                    GLParam(param_id=503, value=603),
                ],
            ),
        ]

        cls.index.offers.append(Offer(hyperid=5001, fesh=2))
        # personal categories ordering
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1003').respond({'models': ['5001']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1003', item_count=40, with_timestamps=True
        ).respond({'models': ['5001'], 'timestamps': ['1']})

    def test_glparam(self):
        """Проверяем что у единственной модели с фильтрами(hyperid=1):
        -параметр 501 со значением 601 есть в выдаче
        -параметр 502 со значением 1 есть в выдаче, с 0 нет
        -параметр 503 с минимальным и максимальным значением 603 есть в выдаче
        """
        response = self.report.request_json(
            'place=popular_products&numdoc=100&rids=213&hid=9500&yandexuid=1003&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(response, {"entity": "product", "id": 5001})
        self.assertFragmentIn(response, {"filters": [{"id": "501", "values": [{"id": "601"}]}]})
        self.assertFragmentIn(
            response, {"filters": [{"id": "502", "values": [{"id": "1", "found": 1}, {"id": "0", "found": 0}]}]}
        )
        self.assertFragmentIn(response, {"filters": [{"id": "503", "values": [{"min": "603", "max": "603"}]}]})

    @classmethod
    def prepare_test_model_descriptions_existance(cls):
        """
        Создаем категорию с простыми шаблонами описания
        Создаем минимальную конфигурацию для популярной модели по yandexuid=003 (split=3)
        """

        cls.index.hypertree += [
            HyperCategory(
                hid=9400,
                children=[
                    HyperCategory(hid=9401, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=9401,
                micromodel="{Type}",
                friendlymodel=["{Type}"],
                model=[("Технические характеристики", {"Тип": "{Type}"})],
                seo="{return $Type; #exec}",
            )
        ]

        cls.index.gltypes += [GLType(hid=9401, param_id=2000, name=u"Тип", xslname="Type", gltype=GLType.STRING)]

        cls.index.models += [
            Model(hid=9401, hyperid=20001, glparams=[GLParam(param_id=2000, string_value="наушники")], accessories=[])
        ]

        cls.index.offers.append(Offer(hyperid=20001, fesh=2))
        # personal categories ordering
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1002').respond({'models': ['20001']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1002', item_count=40, with_timestamps=True
        ).respond({'models': ['20001'], 'timestamps': ['1']})

    def test_model_descriptions_existance(self):
        # Проверяем, то на place=popular_products работают все виды характеристик модели
        response = self.report.request_json(
            'place=popular_products&yandexuid=1002&rids=213&show-models-specs=full,friendly&hid=9400&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                "description": "наушники",
                "specs": {
                    "friendly": ["наушники"],
                    "full": [
                        {
                            "groupName": "Технические характеристики",
                            "groupSpecs": [{"name": "Тип", "value": "наушники"}],
                        }
                    ],
                },
                "lingua": {
                    "type": {
                        "nominative": "наушники-nominative",
                        "genitive": "наушники-genitive",
                        "dative": "наушники-dative",
                        "accusative": "наушники-accusative",
                    }
                },
            },
        )

    def test_show_log(self):
        """
        Проверка поля url_hash в show log
        """
        self.report.request_json(
            'place=popular_products&numdoc=24&rids=213&hid=200&yandexuid=1001&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    @classmethod
    def prepare_model_relevance(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=9200,
                children=[
                    HyperCategory(hid=9201, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9202, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=91, hid=9201, model_clicks=200, opinion=Opinion(total_count=300, rating=4.0)),
            Model(hyperid=92, hid=9201, model_clicks=300, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=93, hid=9201, model_clicks=100, opinion=Opinion(total_count=300, rating=3.0)),
            Model(hyperid=94, hid=9202, model_clicks=200, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=95, hid=9202, model_clicks=100, opinion=Opinion(total_count=300, rating=4.0)),
            Model(hyperid=96, hid=9201, model_clicks=200, opinion=Opinion(total_count=300, rating=4.0)),
            Model(hyperid=97, hid=9201, model_clicks=300, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=98, hid=9201, model_clicks=100, opinion=Opinion(total_count=300, rating=3.0)),
            Model(hyperid=99, hid=9202, model_clicks=200, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=100, hid=9202, model_clicks=100, opinion=Opinion(total_count=300, rating=4.0)),
        ]

        sku_offers = [
            BlueOffer(price=price, vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, discount=discount)
            for feedid, price, shop_sku, discount in zip(
                _Offers.feed_ids, _Offers.prices, _Offers.shop_skus, _Offers.discounts
            )
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                ts=hyperid * 1000,
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku='{i}'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(list(range(91, 101)), _Offers.shop_skus, sku_offers[:11])
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1009').respond(
            {'models': ['96', '97', '98', '99', '100']}
        )
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1009', item_count=40, with_timestamps=True
        ).respond({'models': ['96', '97', '98', '99', '100'], 'timestamps': ['1', '2', '3', '4', '5']})

        # то же самое для юзера 1010, но добавляется бмгб счетчик для уже купленных ску
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1010').respond(
            {'models': ['96', '97', '98', '99', '100']}
        )
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1010', item_count=40, with_timestamps=True
        ).respond({'models': ['96', '97', '98', '99', '100'], 'timestamps': ['1', '2', '3', '4', '5']})

        cls.index.mskus += [
            MarketSku(
                ts=96 * 1000,
                title='Sku 1001',
                hyperid=97,
                sku=1001,
                blue_offers=[
                    BlueOffer(
                        price=16,
                        vat=Vat.VAT_10,
                        offerid='Feed_4_sku5',
                        feedid=4,
                        waremd5='Sku6Price16-iLVm1G1001',
                        discount=None,
                    )
                ],
            )
        ]

        sku_purchases_counter = BeruSkuOrderCountCounter(
            [
                SkuPurchaseEvent(sku_id=1001, count=1),
            ]
        )
        cls.bigb.on_request(yandexuid='1010', client='merch-machine').respond(counters=[sku_purchases_counter])
        cls.bigb.on_default_request().respond(counters=[])

    def test_model_relevance(self):
        """
        Проверка фильтра дублей категорий и выбора моделей
        """
        response = self.report.request_json(
            'place=popular_products&rids=213&hid=9200&yandexuid=1009&rgb=green&numdoc=2&page=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'id': 97},
                        {'entity': 'product', 'id': 99},
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        """
        Если в bigb для этого юзера и этой модели есть данные о покупках конкретных мску, то нужно ее использовать
        Так что sku 1001 должна победить sku 97
        """
        response = self.report.request_json(
            'place=popular_products&rids=213&hid=9200&yandexuid=1010&rgb=green&numdoc=2&page=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'id': 97,
                            "offers": {
                                "items": [
                                    {
                                        "sku": "1001",
                                    }
                                ]
                            },
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        """ Для синего маркета нет ограничения на количество моделей в категории,
            но должно быть чередование категорий, при этом модели внутри категории
            всё ещё ранжируются по популярности
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&rids=213&hid=9200&yandexuid=1009&numdoc=5&page=1&{}'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 97},  # best of 1st category
                            {'entity': 'product', 'id': 99},  # best of 2nd category
                            {'entity': 'product', 'id': 92},  # top 2 of 1st category
                            {'entity': 'product', 'id': 94},  # top 2 of 2nd category
                            {'entity': 'product', 'id': 96},  # last of 1st category
                        ]
                    },
                },
                allow_different_len=False,
                preserve_order=True,
            )

    @classmethod
    def prepare_rgb_noisy_models(cls):
        """
        Blue and green offers for a model
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=9600,
                children=[
                    HyperCategory(hid=9601, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=9601, model_clicks=100, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=102, hid=9601, model_clicks=100, opinion=Opinion(total_count=300, rating=3.0)),
            Model(hyperid=103, hid=9601, model_clicks=100, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=104, hid=9601, model_clicks=100, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=105, hid=9601, model_clicks=100, opinion=Opinion(total_count=300, rating=5.0)),
        ]

        # market skus
        sku_offer = BlueOffer(
            price=_Offers.prices[0],
            vat=Vat.VAT_10,
            offerid=_Offers.shop_skus[0],
            feedid=_Offers.feed_ids[0],
            discount=_Offers.discounts[0],
        )
        cls.index.mskus += [
            MarketSku(
                ts=102000,
                title='Blue offer {sku}'.format(sku=_Offers.shop_skus[0]),
                hyperid=102,
                sku='{i}'.format(i=102),
                blue_offers=[sku_offer],
            ),
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1101').respond({'models': ['101']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1101', with_timestamps=True, item_count=40
        ).respond({'models': ['101'], 'timestamps': ['1']})

        # green offers for noisy models
        cls.index.offers += [
            Offer(hyperid=101, fesh=2),
            Offer(hyperid=103, fesh=2),
            Offer(hyperid=104, fesh=2),
            Offer(hyperid=105, fesh=2),
        ]

    def test_rgb_noisy_models(self):
        """
        Проверка поиска по цвету маркета:
        на этапе запроса моделей по найденным категориям должны найтись только модели запрошенного "цвета",
        иначе модели отфильтруются потом по статистике наличия предложений,
        если статистика умеет(а она умеет) разделять по "цвету".
        Данные:
        Для некоторых персональных категорий имеются модели с офферами из разных маркетов,
        причем модель не из того маркета более релевантна.
        Проверяем, что для таких категорий в выдаче имеется нужная модель
        (а не взята модель не того цвета с последующим отфильтровыванием без замены на подходящюу)
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&numdoc=100&rids=213&hid=9600&yandexuid=1101&{}'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {'entity': 'product', 'id': 102},
                        ],
                    }
                },
            )

    @classmethod
    def prepare_rgb_noisy_categories(cls):
        """
        Blue and green offers for a model
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=9700, children=[HyperCategory(hid=i, output_type=HyperCategoryType.GURU) for i in range(9701, 9721)]
            ),
        ]

        cls.index.models += [Model(hyperid=111 + i, hid=9701 + i) for i in range(20)]

        cls.index.models += [Model(hyperid=132, hid=9720)]

        # market skus
        sku_offer_1 = BlueOffer(
            price=_Offers.prices[0],
            vat=Vat.VAT_10,
            offerid=_Offers.shop_skus[0],
            feedid=_Offers.feed_ids[0],
            discount=_Offers.discounts[0],
        )
        sku_offer_2 = BlueOffer(price=10, feedid=4)
        cls.index.mskus += [
            MarketSku(
                ts=130000,
                title='Blue offer {sku}'.format(sku=_Offers.shop_skus[0]),
                hyperid=130,  # 130 is blue!
                sku='{i}'.format(i=130),
                blue_offers=[sku_offer_1],
            ),
            MarketSku(
                ts=130001,
                title='Blue offer {sku}'.format(sku=_Offers.shop_skus[0]),
                hyperid=132,  # 132 is blue!
                sku='{i}'.format(i=132),
                blue_offers=[sku_offer_2],
            ),
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1102').respond(
            {'models': map(str, list(range(111, 131)))}
        )
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1102', item_count=40, with_timestamps=True
        ).respond({'models': map(str, list(range(111, 131))), 'timestamps': map(str, list(range(111, 131)))})

        # green offers for noisy categories
        cls.index.offers += [Offer(hyperid=i, fesh=2) for i in range(111, 130)]  # not including 130 which is blue!

    def test_rgb_noisy_categories(self):
        """
        Проверка поиска по цвету маркета:
        при отборе персональных категорий должны отсеяться категории,в которых заведомо нет синих моделей,
        иначе некоторые хорошие категории могут просто не попасть в лимит,
        а модели прошедших плохих категорий отфильтруются потом по статистике наличия предложений,
        если статистика умеет(а она умеет) разделять по "цвету"
        Данные:
        Синие офферы имеет только одна категория, эта категория представлена в истории
        в позициях в списке моделей далеко за лимитом на количество категорий
        Проверяем, что все равно находится модель с синим оффером
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&numdoc=100&rids=213&hid=9700&yandexuid=1102&{}'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 2,
                        'results': [
                            {'entity': 'product', 'id': 130},
                            {'entity': 'product', 'id': 132},
                        ],
                    }
                },
            )

    @classmethod
    def prepare_blue_nonguru(cls):
        """
        Синий оффер из GURULIGHT категории
        """
        # guru light categories
        cls.index.hypertree += [
            HyperCategory(hid=19201, output_type=HyperCategoryType.GURULIGHT),
        ]

        # cheat-model to catch a GURULIGHT category in history
        cls.index.models += [Model(hyperid=19201001, hid=19201)]

        # market skus
        sku_offer = BlueOffer(price=1000, vat=Vat.VAT_10, feedid=4)
        cls.index.mskus += [
            MarketSku(
                title='Blue offer for gurulight cat',  # .format(sku=_Offers.shop_skus[0]),
                hyperid=19201001,  # hyperid=130, # 130 is blue!
                sku=19201001,
                blue_offers=[sku_offer],
            ),
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1103').respond({'models': ['19201001']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1103', item_count=40, with_timestamps=True
        ).respond({'models': ['19201001'], 'timestamps': ['1']})

    def test_blue_nonguru(self):
        """
        На синем публикуются предложения в негурушных категориях, проверяем, что они не отсеиваются
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj_no_nid_check=0&numdoc=100&rids=213&yandexuid=1103&{}'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 19201001, 'categories': [{'id': 19201}]},
                        ]
                    }
                },
                allow_different_len=True,
            )

    @classmethod
    def prepare_cross_dep_accessories(cls):
        """
        Пример категорий в разных департаментах, где одна из них аксессуарная для второй
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=1111,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=1001410, output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(
                hid=1112,
                output_type=HyperCategoryType.GURU,
                children=[
                    # accessory category in different department
                    HyperCategory(hid=15561854, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]
        cls.index.models += [
            Model(hyperid=1001410001, hid=1001410),
            Model(hyperid=1001410002, hid=1001410),
            Model(hyperid=1556185401, hid=15561854),
        ]

        # green offers
        cls.index.offers += [
            Offer(fesh=2, hyperid=1001410001),
            Offer(fesh=2, hyperid=1001410002),
            Offer(fesh=2, hyperid=1556185401),
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                hyperid=1001410002, sku=9001410002, blue_offers=[BlueOffer(price=1000, vat=Vat.VAT_10, feedid=4)]
            ),
            MarketSku(
                hyperid=1556185401, sku=95561854001, blue_offers=[BlueOffer(price=1000, vat=Vat.VAT_10, feedid=4)]
            ),
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1104').respond({'models': ['1001410001']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1104', item_count=40, with_timestamps=True
        ).respond({'models': ['1001410001'], 'timestamps': ['1']})

    def test_cross_dep_accessories(self):
        """
        Проверяем фильтрацию категорий по заданному департаменту:
        аксессуарные категории к некоторым нужным категориям могут быть вне департамента
        """
        for rgb in ['green', 'blue']:
            # hid=1111
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&yandexuid=1104&hid=1111&rgb={}'.format(
                    rgb
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 1001410002, 'categories': [{'id': 1001410}]},
                        ]
                    }
                },
                allow_different_len=True,
            )
            """Модель аксессуарной категоии из другого департамента не должна проходить"""
            self.assertFragmentNotIn(response, {'entity': 'product', 'id': 1556185401})
            # no hid filter
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&yandexuid=1104&rgb={}'.format(
                    rgb
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 1001410002, 'categories': [{'id': 1001410}]},
                            {'entity': 'product', 'id': 1556185401, 'categories': [{'id': 15561854}]},
                        ]
                    }
                },
                allow_different_len=True,
            )

    @classmethod
    def prepare_blue_offers(cls):
        """Модели с какими-то предложениями, но без синих предложений, проходящих все фильтры"""

        # personal categories
        cls.index.hypertree += [
            HyperCategory(
                hid=3333,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=33330, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1105').respond({'models': ['33331']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1105', item_count=40, with_timestamps=True
        ).respond({'models': ['33331'], 'timestamps': ['1']})
        # models
        cls.index.models += [
            Model(hyperid=33331, hid=33330),
            Model(hyperid=33332, hid=33330),
            Model(hyperid=33333, hid=33330),
        ]
        # green offers
        cls.index.offers += [
            Offer(fesh=1, hyperid=33331, price=100, price_old=120),
            Offer(fesh=1, hyperid=33332, price=100, price_old=120),
            Offer(fesh=1, hyperid=33333),
        ]
        # market skus
        cls.index.mskus += [
            MarketSku(
                hyperid=33331, sku=3333101, blue_offers=[BlueOffer(price=1000, vat=Vat.VAT_10, feedid=1, discount=0)]
            ),
            MarketSku(
                hyperid=33332, sku=3333201, blue_offers=[BlueOffer(price=1000, vat=Vat.VAT_10, feedid=1, discount=0)]
            ),
        ]

    def test_blue_offers(self):
        """Проверяем, что товары не в наличии не отображаются
        Фильтр должен действовать не только при поиске офферов для моделей, но и
        при отборе моделей
        """

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&yandexuid=1105&hid=3333&filter-discount-only=1&{}'.format(
                    param
                )
            )
            self.assertFragmentIn(response, {'search': {'total': 0, 'results': EmptyList()}})

    @classmethod
    def prepare_msku_without_offers(cls):
        """Модель с оффером из чужого склада из категории с хорошей статистикой по нашему региону"""
        cls.index.hypertree += [
            HyperCategory(
                hid=4444,
                children=[
                    HyperCategory(hid=44440),
                ],
            ),
        ]
        cls.index.blue_category_region_stat += [
            CategoryStatsRecord(hid=44440, region=39, n_offers=3, n_discounts=3),
            CategoryStatsRecord(hid=44440, region=213, n_offers=3, n_discounts=3),
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1106').respond({'models': ['44441']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1106', item_count=40, with_timestamps=True
        ).respond({'models': ['44441'], 'timestamps': ['1']})
        cls.index.models += [
            Model(hyperid=44441, hid=44440),
        ]
        cls.index.shops += [
            Shop(
                fesh=80001,
                datafeed_id=70001,
                priority_region=213,
                currency=Currency.RUR,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
            ),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=44441, sku=4444101, blue_offers=[BlueOffer(price=1000, vat=Vat.VAT_10, feedid=70001)]),
        ]

    def test_msku_without_offers(self):
        """Проверяем, что товары имеющие msku, но не имеющие офферов в регионе, не отображаются"""
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=popular_products&rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0&yandexuid=1106&hid=4444&rids=213&{}&rearr-factors=market_nordstream=0'.format(  # noqa
                    param
                )
            )
            self.assertFragmentIn(response, {'search': {'total': 0, 'results': EmptyList()}})


if __name__ == '__main__':
    main()

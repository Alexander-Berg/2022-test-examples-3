#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DynamicShop,
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Offer,
    Picture,
    Region,
    Shop,
    VCluster,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.types.picture import thumbnails_config

CATEG_PICS = '&get-category-pictures=1'


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Id values:
            hid: [201:207]
        """

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург'),
            Region(rid=64, name='Екатеринбург'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=201, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=202, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=203, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=204, output_type=HyperCategoryType.GURU),
            HyperCategory(
                hid=207,
                children=[
                    HyperCategory(hid=205, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=206, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        hids = list(range(201, 207)) + list(range(220, 232))
        models = [10000 + hid for hid in hids]

        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid, hid in zip(models, hids)]

        # sort personal categories
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:", item_count=1000).respond({"models": []})
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:004", item_count=1000).respond({"models": []})
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:001", item_count=1000).respond(
            {"models": map(str, list(reversed(models[:6])))}
        )
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:002", item_count=1000).respond(
            {"models": map(str, models[:3])}
        )
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:003", item_count=1000).respond(
            {"models": map(str, models[3:6])}
        )
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:005", item_count=1000).respond(
            {"models": map(str, models[9:15])}
        )
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:006", item_count=1000).respond(
            {
                "models": [
                    '10231',
                    '10230',
                    '10229',
                    '10228',
                    '10227',
                    '10225',
                    '10224',
                    '10223',
                    '10226',
                    '10222',
                    '10221',
                    '10220',
                ]
            }
        )

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [201, 6],
                            [202, 5],
                            [203, 4],
                            [204, 3],
                            [205, 2],
                            [206, 1],
                        ],
                        splits=['1'],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [201, 1],
                            [202, 2],
                            [203, 3],
                        ],
                        splits=['2'],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [204, 1],
                            [205, 2],
                            [206, 3],
                        ],
                        splits=['3'],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [210, 6],
                            [211, 5],
                            [212, 4],
                            [213, 3],
                            [214, 2],
                            [215, 2],
                        ],
                        splits=['5'],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [220, 12],
                            [221, 11],
                            [222, 10],
                            [226, 9],
                            [223, 8],
                            [224, 7],
                            [225, 6],
                            [227, 5],
                            [228, 4],
                            [229, 3],
                            [230, 2],
                            [231, 1],
                        ],
                        splits=['6'],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [1674101, 6],
                            [1674102, 5],
                            [1674103, 4],
                            [1674104, 3],
                            [1674105, 2],
                            [1674106, 1],
                        ],
                        splits=['7'],
                    ),
                ],
            )
        ]

    def test_config(self):
        response = self.report.request_json('place=personal_categories&yandexuid=004&numdoc=10')
        self.error_log.expect('Personal category config is not available for user 004.').once()

        request = 'place=personal_categories&yandexuid=001'
        response = self.report.request_json(request + '&numdoc=10')
        self.assertFragmentIn(response, {"total": 6}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "201"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "202"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "203"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "204"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "205"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "206"}, preserve_order=True)
        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='6')
        response = self.report.request_json(request + '&numdoc=5')
        self.assertFragmentIn(response, {"total": 5})
        self.access_log.expect(total_renderable='5')

        response = self.report.request_json('place=personal_categories&yandexuid=002&numdoc=10')
        self.assertFragmentIn(response, {"total": 3}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "201"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "202"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "203"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"hid": "204"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"hid": "205"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"hid": "206"}, preserve_order=True)

        response = self.report.request_json('place=personal_categories&yandexuid=003&numdoc=10')
        self.assertFragmentIn(response, {"total": 3}, preserve_order=True)
        self.assertFragmentNotIn(response, {"hid": "201"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"hid": "202"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"hid": "203"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "204"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "205"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "206"}, preserve_order=True)

    def test_sort(self):
        response = self.report.request_json('place=personal_categories&yandexuid=001&numdoc=10')
        self.assertFragmentIn(
            response,
            [
                {"link": {"params": {"hid": "206"}}},
                {"link": {"params": {"hid": "205"}}},
                {"link": {"params": {"hid": "204"}}},
                {"link": {"params": {"hid": "203"}}},
                {"link": {"params": {"hid": "202"}}},
                {"link": {"params": {"hid": "201"}}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_categories&yandexuid=002&numdoc=10')
        self.assertFragmentIn(
            response,
            [
                {"link": {"params": {"hid": "201"}}},
                {"link": {"params": {"hid": "202"}}},
                {"link": {"params": {"hid": "203"}}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_categories&yandexuid=003&numdoc=10')
        self.assertFragmentIn(
            response,
            [
                {"link": {"params": {"hid": "204"}}},
                {"link": {"params": {"hid": "205"}}},
                {"link": {"params": {"hid": "206"}}},
            ],
            preserve_order=True,
        )

    def test_param(self):
        response = self.report.request_json('place=personal_categories')
        self.error_log.expect('Personal category config is not available for user .').once()

        response = self.report.request_json('place=personal_categories&yandexuid=001')
        self.assertFragmentIn(response, {"total": 5}, preserve_order=True)

        response = self.report.request_json('place=personal_categories&yandexuid=001&numdoc=3')
        self.assertFragmentIn(response, {"total": 3}, preserve_order=True)

        response = self.report.request_json('place=personal_categories&yandexuid=001&numdoc=20')
        self.assertFragmentIn(response, {"total": 6}, preserve_order=True)

        response = self.report.request_json('place=personal_categories&yandexuid=001&hid=207')
        self.assertFragmentIn(response, {"total": 2}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "205"}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "206"}, preserve_order=True)

        response = self.report.request_json('place=personal_categories&yandexuid=001&hid=201')
        self.assertFragmentIn(response, {"total": 1}, preserve_order=True)
        self.assertFragmentIn(response, {"hid": "201"}, preserve_order=True)

    def test_missing_pp(self):
        self.report.request_json('place=personal_categories&yandexuid=001&numdoc=10&ip=127.0.0.1', add_defaults=False)

    @classmethod
    def prepare_catgories_with_pictures(cls):
        """Создаем категорийное и навигационное деревья, по две родительских
        категории и по две дочерих разных типов внутри каждой из родительских
        Создаем офферы в этих категориях, как с картинками, так и без них
        Создаем модель и кластер
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=210,
                output_type=HyperCategoryType.GURULIGHT,
                children=[
                    HyperCategory(hid=211, output_type=HyperCategoryType.SIMPLE),
                    HyperCategory(hid=212, output_type=HyperCategoryType.CLUSTERS, visual=True),
                ],
            ),
            HyperCategory(
                hid=213,
                output_type=HyperCategoryType.GURULIGHT,
                children=[
                    HyperCategory(hid=214, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=215, output_type=HyperCategoryType.GURULIGHT),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=300,
                hid=210,
                children=[
                    NavCategory(
                        nid=301,
                        hid=211,
                        children=[
                            NavCategory(nid=302, hid=212),
                        ],
                    ),
                ],
            ),
            NavCategory(
                nid=303,
                hid=213,
                children=[
                    NavCategory(nid=304, hid=214),
                    NavCategory(nid=305, hid=215),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=401, hid=214, picinfo='//mdata.yandex.net/i?path=iphone.jpg', add_picinfo=''),
        ]

        cls.index.offers += [
            Offer(title="guru offer WO picture", bid=100, no_picture=True, hid=211),
            Offer(title="visual offer WO picture", vclusterid=1000000001, bid=100, no_picture=True, hid=212),
            Offer(title="non-guru offer WO picture", bid=100, no_picture=True, hid=215),
            Offer(title="guru offer WO picture", hyperid=401, no_picture=True, hid=214),
            Offer(
                title="guru offer WITH picture",
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl18Q',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
                hid=211,
            ),
            Offer(title="visual offer WITH picture", vclusterid=1000000001, hid=212),
            Offer(
                title="non-guru offer WITH picture",
                picture=Picture(
                    picture_id='Nin3dIIHBidheII_HBIE3w',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
                hid=215,
            ),
        ]

        cls.index.vclusters += [
            VCluster(
                vclusterid=1000000001,
                hid=212,
                pictures=[
                    Picture(
                        picture_id='IWn9efjIWifefpJ_WIN35Q',
                        width=900,
                        height=1200,
                        thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                        group_id=1234,
                    )
                ],
            ),
        ]

    def test_categories_with_pictures(self):
        """Что тестируем: в плейсе personal_categories для категорий выдаются
        картинки наиболее популярных офферов и моделей

        Задаем запросы к плейсу personal_categories за родительскими категориями
        300 и 303. Проверяем, что на выдаче есть только дочерние категории, в
        которых заполнено поле icons в правильном формате
        """
        response = self.report.request_json('place=personal_categories&yandexuid=005&nid=300' + CATEG_PICS)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'navnode',
                        'id': 302,
                        'icons': [
                            {
                                'entity': 'picture',
                                'thumbnails': [
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/1234/market_IWn9efjIWifefpJ_WIN35Q/900x1200',
                                        'width': 900,
                                        'height': 1200,
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'entity': 'navnode',
                        'id': 301,
                        'icons': [
                            {
                                'entity': 'picture',
                                'thumbnails': [
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/1234/market_AQJnjRkieZz5Eis_EDl18Q/900x1200',
                                        'width': 900,
                                        'height': 1200,
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'entity': 'navnode',
                        'id': 300,
                    },
                ]
            },
        )

        response = self.report.request_json('place=personal_categories&yandexuid=005&nid=303' + CATEG_PICS)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'navnode',
                        'id': 305,
                        'icons': [
                            {
                                'entity': 'picture',
                                'thumbnails': [
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/1234/market_Nin3dIIHBidheII_HBIE3w/900x1200',
                                        'width': 900,
                                        'height': 1200,
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'entity': 'navnode',
                        'id': 304,
                        'icons': [
                            {
                                'entity': 'picture',
                                'thumbnails': [
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=1',
                                        'width': 50,
                                        'height': 50,
                                    },
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=2',
                                        'width': 100,
                                        'height': 100,
                                    },
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=3',
                                        'width': 75,
                                        'height': 75,
                                    },
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=4',
                                        'width': 150,
                                        'height': 150,
                                    },
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=5',
                                        'width': 200,
                                        'height': 200,
                                    },
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=6',
                                        'width': 250,
                                        'height': 250,
                                    },
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=7',
                                        'width': 120,
                                        'height': 120,
                                    },
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=8',
                                        'width': 240,
                                        'height': 240,
                                    },
                                    {
                                        'url': '//mdata.yandex.net/i?path=iphone.jpg&size=9',
                                        'width': 500,
                                        'height': 500,
                                    },
                                ],
                            }
                        ],
                    },
                    {
                        'entity': 'navnode',
                        'id': 303,
                    },
                ]
            },
        )

    @classmethod
    def prepare_shop_categories(cls):
        """Создаем категорийное и навигационное деревья, по две родительских
        категории и по две дочерих разных типов внутри каждой из родительских
        Создаем офферы в этих категориях, как с картинками, так и без них
        Создаем модель и кластер
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=220,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=221, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=222, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=223, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=224, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=225, output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(
                hid=226,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=227, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=228, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=229, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=230, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=231, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=320,
                hid=220,
                children=[
                    NavCategory(nid=321, hid=221),
                    NavCategory(nid=322, hid=222),
                    NavCategory(nid=323, hid=223),
                    NavCategory(nid=324, hid=224),
                    NavCategory(nid=325, hid=225),
                ],
            ),
            NavCategory(
                nid=326,
                hid=226,
                children=[
                    NavCategory(nid=327, hid=227),
                    NavCategory(nid=328, hid=228),
                    NavCategory(nid=329, hid=229),
                    NavCategory(nid=330, hid=230),
                    NavCategory(nid=331, hid=231),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=401, priority_region=213, regions=[2]),
            Shop(fesh=402, priority_region=213, regions=[2]),
        ]

        cls.index.offers += [
            Offer(
                hid=220,
                fesh=401,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl19A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=221,
                fesh=401,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl20A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=222,
                fesh=401,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl21A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=223,
                fesh=402,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl22A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=224,
                fesh=402,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl23A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=225,
                fesh=402,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl24A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=226,
                fesh=401,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl25A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=227,
                fesh=401,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl26A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=228,
                fesh=401,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl27A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=229,
                fesh=401,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl28A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=227,
                fesh=402,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl29A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=228,
                fesh=402,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl30A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=229,
                fesh=402,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl31A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=230,
                fesh=402,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl32A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
            Offer(
                hid=231,
                fesh=402,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl33A',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
            ),
        ]

    def test_shop_categories(self):
        """Что тестируем: в плейсе personal_categories при фильтрации по магазину
        возвращаются только категории, представленные в магазине в регионе
        пользователя

        Задаем запросы к плейсу personal_categories для магазина 401
        в регионах 213 и 2
        Проверяем, что на выдаче есть только листовые категории этого магазина
        в правильном порядке

        Задаем запрос для этого магазина в регионе 64, проверяем, что
        выдача пуста
        """
        for rids in [213, 2]:
            # Запрос за тремя категориями
            response = self.report.request_json(
                'place=personal_categories&yandexuid=006&fesh=401&numdoc=3&rids={}'.format(rids)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'navnode',
                            'id': 329,
                        },
                        {
                            'entity': 'navnode',
                            'id': 328,
                        },
                        {
                            'entity': 'navnode',
                            'id': 327,
                        },
                    ]
                },
                allow_different_len=False,
            )

            # Запрос за пятью категориями
            response = self.report.request_json(
                'place=personal_categories&yandexuid=006&fesh=401&numdoc=5&rids={}'.format(rids)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'navnode',
                            'id': 329,
                        },
                        {
                            'entity': 'navnode',
                            'id': 328,
                        },
                        {
                            'entity': 'navnode',
                            'id': 327,
                        },
                        {
                            'entity': 'navnode',
                            'id': 322,
                        },
                        {
                            'entity': 'navnode',
                            'id': 321,
                        },
                    ]
                },
                allow_different_len=False,
            )

        # Запрос в регионе без офферов магазина
        response = self.report.request_json('place=personal_categories&yandexuid=006&fesh=401&numdoc=3&rids=64')
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'entity': 'navnode',
                    }
                ]
            },
        )

    def test_shop_categories_pictures(self):
        """Что тестируем: в плейсе personal_categories в режиме запроса картинок
        при фильтрации по магазину возвращаются только категории, представленные
        в магазине в регионе пользователя

        Задаем запросы к плейсу personal_categories для магазина 401 в регионах
        2 и 213. Проверяем, что на выдаче есть только категории этого магазина
        в правильном порядке с картинками из офферов магазина в этой категории

        Задаем запрос для этого магазина в регионе 64, проверяем, что
        выдача пуста
        """
        for rids in [213, 2]:
            response = self.report.request_json(
                'place=personal_categories&yandexuid=006&fesh=401&numdoc=3&rids={}'.format(rids) + CATEG_PICS
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'navnode',
                            'id': 329,
                            'icons': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {
                                            'url': 'http://avatars.mdst.yandex.net/get-marketpic/1234/market_AQJnjRkieZz5Eis_EDl28A/900x1200',
                                            'width': 900,
                                            'height': 1200,
                                        }
                                    ],
                                }
                            ],
                        },
                        {
                            'entity': 'navnode',
                            'id': 328,
                            'icons': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {
                                            'url': 'http://avatars.mdst.yandex.net/get-marketpic/1234/market_AQJnjRkieZz5Eis_EDl27A/900x1200',
                                            'width': 900,
                                            'height': 1200,
                                        }
                                    ],
                                }
                            ],
                        },
                        {
                            'entity': 'navnode',
                            'id': 327,
                            'icons': [
                                {
                                    'entity': 'picture',
                                    'thumbnails': [
                                        {
                                            'url': 'http://avatars.mdst.yandex.net/get-marketpic/1234/market_AQJnjRkieZz5Eis_EDl26A/900x1200',
                                            'width': 900,
                                            'height': 1200,
                                        }
                                    ],
                                }
                            ],
                        },
                    ]
                },
            )

        # Запрос в регионе без офферов магазина
        response = self.report.request_json(
            'place=personal_categories&yandexuid=006&fesh=401&numdoc=3&rids=64' + CATEG_PICS
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'entity': 'navnode',
                    }
                ]
            },
        )

    def test_shop_dynamic(self):
        """Что тестируем: в плейсе personal_categories при фильтрации по магазину
        не возвращаются категории магазинов, отключенных через динамик

        Задаем запросы к плейсу personal_categories для магазина 401
        в регионах 213 и 2
        Проверяем, что на выдаче есть категории

        Выключаем магазин 401 динамиком
        Проверяем, что по тому же запросу выдача пуста
        """
        for rids in [213, 2]:
            # Запрос за тремя категориями
            response = self.report.request_json(
                'place=personal_categories&yandexuid=006&fesh=401&numdoc=3&rids={}'.format(rids)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'navnode',
                            'id': 329,
                        },
                        {
                            'entity': 'navnode',
                            'id': 328,
                        },
                        {
                            'entity': 'navnode',
                            'id': 327,
                        },
                    ]
                },
                allow_different_len=False,
            )

        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(401)]

        for rids in [213, 2]:
            # Запрос за тремя категориями
            response = self.report.request_json(
                'place=personal_categories&yandexuid=006&fesh=401&numdoc=3&rids={}'.format(rids)
            )
            self.assertFragmentNotIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'navnode',
                        }
                    ]
                },
            )

    @classmethod
    def prepare_catgories_with_trimmed_thumbs(cls):
        """Создаем категорийное и навигационное деревья, по две родительских
        категории и по две дочерих разных типов внутри каждой из родительских
        Создаем офферы в этих категориях, как с картинками, так и без них
        Создаем модель и кластер
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=1674100,
                output_type=HyperCategoryType.GURULIGHT,
                children=[
                    HyperCategory(hid=1674101, output_type=HyperCategoryType.SIMPLE),
                    HyperCategory(hid=1674102, output_type=HyperCategoryType.CLUSTERS, visual=True),
                ],
            ),
            HyperCategory(
                hid=1674103,
                output_type=HyperCategoryType.GURULIGHT,
                children=[
                    HyperCategory(hid=1674104, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=1674105, output_type=HyperCategoryType.GURULIGHT),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=16741000,
                hid=1674100,
                children=[
                    NavCategory(
                        nid=16741010,
                        hid=1674101,
                        children=[
                            NavCategory(nid=16741020, hid=1674102),
                        ],
                    ),
                ],
            ),
            NavCategory(
                nid=16741030,
                hid=1674103,
                children=[
                    NavCategory(nid=16741040, hid=1674104),
                    NavCategory(nid=16741050, hid=1674105),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1674110,
                hid=1674104,
                picinfo='//avatars.mdst.yandex.net/get-mpic/209514/iphone.jpg#640#480',
                add_picinfo='//avatars.mdst.yandex.net/get-mpic/209524/iphone_back.jpg#640#480',
            ),
        ]

        hids = [
            1674101,
            1674102,
            1674103,
            1674104,
            1674105,
            1674106,
        ]
        models = [hid * 100 + 1 for hid in hids]
        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid, hid in zip(models, hids)]
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:007", item_count=1000).respond(
            {"models": map(str, list(reversed(models)))}
        )

        cls.index.offers += [
            Offer(title="guru offer WO picture", hyperid=1674110, no_picture=True, hid=1674104),
            Offer(
                title="simple offer WITH picture",
                picture=Picture(picture_id='AQJnjRkieZz5Eis_EDl18Q', width=525, height=420, group_id=167410111),
                hid=1674101,
            ),
            Offer(title="visual offer WITHOUT picture", vclusterid=1674102001, hid=1674102),
            Offer(
                title="non-guru offer WITH picture",
                picture=Picture(picture_id='Nin3dIIHBidheII_HBIE3w', width=525, height=420, group_id=167410511),
                hid=1674105,
            ),
        ]

        cls.index.vclusters += [
            VCluster(
                vclusterid=1674102001,
                hid=1674102,
                pictures=[Picture(picture_id='IWn9efjIWifefpJ_WIN35Q', width=525, height=420, group_id=167410211)],
            ),
        ]

    def test_categories_with_trimmed_thumbs(self):
        """Что тестируем: вывод "урезанных" тамбнейлов картинок в плейсе
        personal_categories для категорий разных типов

        Задаем запросы к плейсу personal_categories за родительскими категориями
        300 и 303. Проверяем, что на выдаче есть дочерние категории, в
        которых заполнено поле icons в правильном формате
        """
        response = self.report.request_json(
            'place=personal_categories&trim-thumbs=1&yandexuid=007&nid=16741000' + CATEG_PICS
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'navnode',
                        'id': 16741020,
                        'icons': [
                            {
                                'entity': 'picture',
                                "original": {
                                    "url": "http://avatars.mdst.yandex.net/get-marketpic/167410211/market_IWn9efjIWifefpJ_WIN35Q/orig",
                                    "width": 525,
                                    "height": 420,
                                },
                                'thumbnails': [
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410211/market_IWn9efjIWifefpJ_WIN35Q/x124_trim',
                                        'width': 166,
                                        'height': 124,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410211/market_IWn9efjIWifefpJ_WIN35Q/x166_trim',
                                        'width': 248,
                                        'height': 166,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410211/market_IWn9efjIWifefpJ_WIN35Q/x248_trim',
                                        'width': 332,
                                        'height': 248,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410211/market_IWn9efjIWifefpJ_WIN35Q/x332_trim',
                                        'width': 496,
                                        'height': 332,
                                    },
                                ],
                            }
                        ],
                    },
                    {
                        'entity': 'navnode',
                        'id': 16741010,
                        'icons': [
                            {
                                'entity': 'picture',
                                "original": {
                                    "url": "http://avatars.mdst.yandex.net/get-marketpic/167410111/market_AQJnjRkieZz5Eis_EDl18Q/orig",
                                    "width": 525,
                                    "height": 420,
                                },
                                'thumbnails': [
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410111/market_AQJnjRkieZz5Eis_EDl18Q/x124_trim',
                                        'width': 166,
                                        'height': 124,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410111/market_AQJnjRkieZz5Eis_EDl18Q/x166_trim',
                                        'width': 248,
                                        'height': 166,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410111/market_AQJnjRkieZz5Eis_EDl18Q/x248_trim',
                                        'width': 332,
                                        'height': 248,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410111/market_AQJnjRkieZz5Eis_EDl18Q/x332_trim',
                                        'width': 496,
                                        'height': 332,
                                    },
                                ],
                            }
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=personal_categories&trim-thumbs=1&yandexuid=007&nid=16741030' + CATEG_PICS
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'navnode',
                        'id': 16741040,
                        'icons': [
                            {
                                'entity': 'picture',
                                "original": {
                                    "url": "//avatars.mdst.yandex.net/get-mpic/209514/iphone.jpg",
                                    "width": 640,
                                    "height": 480,
                                },
                                'thumbnails': [
                                    {
                                        'url': '//avatars.mdst.yandex.net/get-mpic/209514/x124_trim',
                                        'width': 166,
                                        'height': 124,
                                    },
                                    {
                                        'url': '//avatars.mdst.yandex.net/get-mpic/209514/x166_trim',
                                        'width': 248,
                                        'height': 166,
                                    },
                                    {
                                        'url': '//avatars.mdst.yandex.net/get-mpic/209514/x248_trim',
                                        'width': 332,
                                        'height': 248,
                                    },
                                    {
                                        'url': '//avatars.mdst.yandex.net/get-mpic/209514/x332_trim',
                                        'width': 496,
                                        'height': 332,
                                    },
                                ],
                            },
                            {
                                'entity': 'picture',
                                "original": {
                                    "url": "//avatars.mdst.yandex.net/get-mpic/209524/iphone_back.jpg",
                                    "width": 640,
                                    "height": 480,
                                },
                                'thumbnails': [
                                    {
                                        'url': '//avatars.mdst.yandex.net/get-mpic/209524/x124_trim',
                                        'width': 166,
                                        'height': 124,
                                    },
                                    {
                                        'url': '//avatars.mdst.yandex.net/get-mpic/209524/x166_trim',
                                        'width': 248,
                                        'height': 166,
                                    },
                                    {
                                        'url': '//avatars.mdst.yandex.net/get-mpic/209524/x248_trim',
                                        'width': 332,
                                        'height': 248,
                                    },
                                    {
                                        'url': '//avatars.mdst.yandex.net/get-mpic/209524/x332_trim',
                                        'width': 496,
                                        'height': 332,
                                    },
                                ],
                            },
                        ],
                    },
                    {
                        'entity': 'navnode',
                        'id': 16741050,
                        'icons': [
                            {
                                'entity': 'picture',
                                "original": {
                                    "url": "http://avatars.mdst.yandex.net/get-marketpic/167410511/market_Nin3dIIHBidheII_HBIE3w/orig",
                                    "width": 525,
                                    "height": 420,
                                },
                                'thumbnails': [
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410511/market_Nin3dIIHBidheII_HBIE3w/x124_trim',
                                        'width': 166,
                                        'height': 124,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410511/market_Nin3dIIHBidheII_HBIE3w/x166_trim',
                                        'width': 248,
                                        'height': 166,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410511/market_Nin3dIIHBidheII_HBIE3w/x248_trim',
                                        'width': 332,
                                        'height': 248,
                                    },
                                    {
                                        'url': 'http://avatars.mdst.yandex.net/get-marketpic/167410511/market_Nin3dIIHBidheII_HBIE3w/x332_trim',
                                        'width': 496,
                                        'height': 332,
                                    },
                                ],
                            }
                        ],
                    },
                    {
                        'entity': 'navnode',
                        'id': 16741030,
                    },
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

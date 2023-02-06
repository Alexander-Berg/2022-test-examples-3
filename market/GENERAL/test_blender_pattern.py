#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CardCategory,
    ClickType,
    Const,
    GLParam,
    GLType,
    HyperCategory,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Shop,
    Suggestion,
    UGCItem,
    VendorBanner,
)
from core.testcase import TestCase, main
from core.matcher import Regex, ElementCount, EmptyList, Contains, LikeUrl, NotEmptyList, EqualToOneOf, NoKey, NotEmpty


class Images:
    IMAGE = (
        '{"thumbnails": [{"densities": [{"url": "//avatars.mds.yandex.net/'
        'get-marketcms/1490511/img-0f5d09ca-e4a3-455f-83e3-feb1701eec24.jpeg/'
        'optimize", "id": "1", "entity": "density"}], "containerHeight": "625"'
        ', "averageColors": {"borders": {"top": "a7927f", "right": "bebbb3",'
        '"left": "b3a396", "bottom": "d0c3b7"}, "full": "b1a396"}, '
        '"containerWidth": "1000", "height": "437", "width": "700", "entity": '
        '"thumbnail", "id": "1000x625"}, {"densities": [{"url": "//avatars.mds'
        '.yandex.net/get-marketcms/1776516/img-41240ddb-9956-490c-8224-'
        '5d79e38e8815.jpeg/optimize", "id": "1", "entity": "density"}], '
        '"containerHeight": "313", "averageColors": {"borders": {"top": '
        '"a7917e", "right": "bfbbb4", "left": "b3a396", "bottom": "d0c3b7"}, '
        '"full": "b1a396"}, "containerWidth": "500", "height": "313", "width":'
        ' "500", "entity": "thumbnail", "id": "500x313"}, {"densities": [{"url'
        '": "//avatars.mds.yandex.net/get-marketcms/1668019/img-adf71cf8-70e5-'
        '4bfa-bc59-13e7fb2efdb1.jpeg/optimize", "id": "1", "entity": "density"'
        '}, {"url": "//avatars.mds.yandex.net/get-marketcms/1668019/img-'
        '00fad0ab-ed47-4781-907b-f4791069056b.jpeg/optimize", "id": "2", '
        '"entity": "density"}], "containerHeight": "188", "averageColors": '
        '{"borders": {"top": "a7917e", "right": "bebbb3", "left": "b3a396", '
        '"bottom": "d0c3b7"}, "full": "b1a396"}, "containerWidth": "300", '
        '"height": "188", "width": "300", "entity": "thumbnail", "id": '
        '"300x188"}], "url": "//avatars.mds.yandex.net/get-marketcms/1668019'
        '/img-190ed917-cb78-42e6-b2e5-c995774088d6.jpeg/optimize", "height": '
        '"500", "width": "700", "entity": "picture", "isNewTab": false}'
    )

    AVATAR = (
        '{"thumbnails": [{"densities": [{"url": "//avatars.mds.yandex.net/'
        'get-marketcms/879900/img-6540725b-f86a-4ba1-96af-294ce2cdfeba.png/'
        'optimize", "id": "1", "entity": "density"}, {"url": "//avatars.mds.'
        'yandex.net/get-marketcms/879900/img-2001de53-00f4-4139-ada8-'
        'f56162d63a6d.png/optimize", "id": "2", "entity": "density"}], '
        '"containerHeight": "62", "averageColors": {"borders": {"top": '
        '"d3d5c5", "right": "e7e5e6", "left": "c7c7be", "bottom": "a5a4ad"}, '
        '"full": "bcb1a9"}, "containerWidth": "62", "height": "62", "width": '
        '"62", "entity": "thumbnail", "id": "62x62"}], "url": "//avatars.mds.'
        'yandex.net/get-marketcms/879900/img-261c9fed-c00a-459b-a6e0-'
        '6feb893b2f16.png/optimize", "height": "300", "selectedThumb": "62x62"'
        ', "width": "300", "entity": "picture", "isNewTab": false}'
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    # тест для проверки логики работы market_report_blender_pattern
    @classmethod
    def prepare_blender_patterns(cls):
        titles = ["Air", "Breath", "Fresh", "Oxy", "Воздух", "Дыши", "Вдох", "Выдох", "Кислород", "Небо"]
        cls.index.models += [
            Model(hid=663, hyperid=663 + i, title="Модель {}".format(titles[i]), ts=100320 + i) for i in range(0, 10)
        ]

        cls.index.shops += [
            Shop(
                fesh=663 + i, priority_region=213, shop_fee=100, cpa=Shop.CPA_REAL, name='CPA Shop {}'.format(titles[i])
            )
            for i in range(0, 10)
        ]

        cls.index.offers += [
            Offer(
                fesh=663 + i,
                hyperid=663 + i,
                hid=663,
                fee=90 + i,
                ts=100320 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Сникерс {}".format(titles[i]),
            )
            for i in range(0, 10)
        ]

        cls.index.offers += [
            Offer(
                fesh=1663,
                hyperid=1663,
                hid=663,
                fee=90,
                ts=100320,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Просто небо",
            )
        ]

        for i in range(0, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020 + i).respond(0.04)

    @classmethod
    def prepare_blender_patterns_materials_incut_from_saas(cls):
        embs = [0.44] * 50

        cls.saas_ugc.on_request(embs=embs, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=588795807,
                    attributes={
                        'page_id': '46122',
                        'compiled_generation': '20210704_0600',
                        'pages_generation': '20210708_0800',
                        'title': 'Шоколадный батончик своими руками',
                        'type': 'knowledge',
                        'semantic_id': 'shokoladny-batonchik-svoimi-rukami',
                        'image': Images.IMAGE,
                    },
                ),
                UGCItem(
                    relevance=582847537,
                    attributes={
                        'page_id': '46124',
                        'title': 'Как выбрать начинку для батончика',
                        'subtitle': 'Как же её выбрать',
                        'type': 'expertise',
                        'semantic_id': 'kak-vybrat-nachinku-dla-batonchika',
                        'image': Images.IMAGE,
                        'author_name': 'Федюков Иван Станиславович младший',
                        'author_description': 'Мой папа с батончиками на ты',
                        'author_avatar': Images.AVATAR,
                    },
                    plain_attributes={
                        'RequestIdInMultiRequest': '0',
                    },
                ),
                UGCItem(
                    relevance=581100722,
                    attributes={
                        'page_id': '46125',
                        'title': '7 правил хорошего питания',
                        'description': 'Семь, то есть девять без двух',
                        'type': 'blog',
                        'semantic_id': '7-pravil-horoshego-pitaniya',
                        'image': Images.IMAGE,
                        'author_uid': '2345671',
                        'author_url': 'instagram.com/rules',
                    },
                ),
            ]
        )

        cls.index.dssm.hard2_query_embedding.on(query='сникерс').set(*embs)
        cls.index.dssm.hard2_query_embedding.on(query='твикс').set(*embs)

    # проверяем, что market_report_blender_pattern=0, а также любое невалидное значение
    # для этого параметра отключает использование паттернов,
    # логика работы блендера в этом случае остается старой,
    # о чем должна появиться запись в logicTrace:
    # ее наличие означает, что в блендер суют какую-то дичь, а не то, что он сломался.
    def test_blender_patterns_disabled(self):
        request = (
            'pp=18&place=blender&text=сникерс&additional_entities=articles&rearr-factors=market_report_blender_pattern='
        )

        pattern_disabled = '0'
        response = self.report.request_json(request + pattern_disabled + '&debug=1')
        self.assertFragmentIn(
            response,
            {'incuts': {'results': EmptyList()}},
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {"logicTrace": [Contains("predefined.cpp", "GetPattern()", "disable", "blender will use default logic")]},
        )

    # проверяем имя паттерна, которого нет в предопределенных.
    def test_blender_patterns_not_found(self):
        request = (
            'pp=18&place=blender&text=сникерс&additional_entities=articles&rearr-factors=market_report_blender_pattern='
        )

        pattern_not_found = 'unknown_predefined_pattern_name'
        response = self.report.request_json(request + pattern_not_found + '&debug=1')
        self.assertFragmentIn(
            response,
            {'incuts': {'results': EmptyList()}},
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "predefined.cpp",
                        "GetPatternFromNameList()",
                        pattern_not_found,
                        "does not contain",
                        "blender will use default logic",
                    )
                ]
            },
        )

    # проверяем отключение паттернов при невалидном значении параметра.
    def test_blender_patterns_invalid(self):
        request = (
            'pp=18&place=blender&text=сникерс&additional_entities=articles&rearr-factors=market_report_blender_pattern='
        )
        # при невалидном жсоне
        pattern_invalid1 = '{i am an invalid json}'
        response = self.report.request_json(request + pattern_invalid1 + '&debug=1')
        self.assertFragmentIn(
            response,
            {'incuts': {'results': EmptyList()}},
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "predefined.cpp", "GetPatternFromJson()", pattern_invalid1, "blender will use default logic"
                    )
                ]
            },
        )

        # при невалидной base64-строке.
        pattern_invalid2 = '^_^ i am an invalid base64 encoded protobuf T_T'
        response = self.report.request_json(request + '_' + pattern_invalid2 + '&debug=1')
        self.assertFragmentIn(
            response,
            {'incuts': {'results': EmptyList()}},
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "predefined.cpp", "GetPatternFromProtobuf()", pattern_invalid2, "blender will use default logic"
                    )
                ]
            },
        )

        # при валидной base64-строке с невалидным протобуфом.
        pattern_invalid3 = 'iamaninvalidprotobuf12,,'
        response = self.report.request_json(request + '_' + pattern_invalid3 + '&debug=1')
        self.assertFragmentIn(
            response,
            {'incuts': {'results': EmptyList()}},
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "predefined.cpp", "GetPatternFromProtobuf()", pattern_invalid3, "blender will use default logic"
                    )
                ]
            },
        )

    # в случае валидного значения в market_report_blender_pattern блендер должен работать по нему.

    # проверяем, что при передаче пустого протобуфа блендер не создает врезок
    # и пишет протобуф в logicTrace.
    # значения из трейса также можно использовать в запросе.
    def test_blender_patterns_empty(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test0 = '_CfdyO-RQ1II,'
        response = self.report.request_json(request + pattern_test0 + '&debug=1')
        self.assertFragmentIn(response, {'incuts': {'results': EmptyList()}})
        self.assertFragmentIn(response, {"logicTrace": [Contains("impl.cpp", "TImpl()", pattern_test0)]})

    # протобуф с одной врезкой в 1 позиции поиска. по умолчанию максимум 5 офферов,
    # также проверяем наличие записи в logicTrace.
    def test_blender_patterns_1_incut(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test1 = '{"Id":"test1","Places":[{"Id":"search_1","Place":2,"Position":1,"ShopIncut":[{"Pp":8}]}]}'
        response = self.report.request_json(request + pattern_test1 + "&debug=1")
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'position': 1,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Небо'}},
                                {'titles': {'raw': 'Сникерс Кислород'}},
                                {'titles': {'raw': 'Сникерс Выдох'}},
                                {'titles': {'raw': 'Сникерс Вдох'}},
                                {'titles': {'raw': 'Сникерс Дыши'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("impl.cpp", "TImpl()", pattern_test1)]})

    # паттерн с двумя врезками в поисковой вызаче.
    # также проверяем для выдачи и врезок наличие правильных pp в show логе:
    # для выдачи - дефолтовое 18, для врезок - из паттерна.
    def test_blender_patterns_2_incuts_with_pp(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test2 = (
            '{"Id":"test2","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"ShopIncut":[{"Pp":9}]},'
            '{"Id":"search_6","Place":2,"Position":6,"ShopIncut":[{"Pp":10}]}]}'
        )
        response = self.report.request_json(request + pattern_test2 + "&show-urls=external")
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'position': 1,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Небо'}},
                                {'titles': {'raw': 'Сникерс Кислород'}},
                                {'titles': {'raw': 'Сникерс Выдох'}},
                                {'titles': {'raw': 'Сникерс Вдох'}},
                                {'titles': {'raw': 'Сникерс Дыши'}},
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 6,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Воздух'}},
                                {'titles': {'raw': 'Сникерс Oxy'}},
                                {'titles': {'raw': 'Сникерс Fresh'}},
                                {'titles': {'raw': 'Сникерс Breath'}},
                                {'titles': {'raw': 'Сникерс Air'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.show_log.expect(pp=18, title="Сникерс Небо")
        self.show_log.expect(pp=18, title="Сникерс Кислород")
        self.show_log.expect(pp=18, title="Сникерс Выдох")
        self.show_log.expect(pp=18, title="Сникерс Вдох")
        self.show_log.expect(pp=18, title="Сникерс Дыши")
        self.show_log.expect(pp=18, title="Сникерс Воздух")
        self.show_log.expect(pp=18, title="Сникерс Oxy")
        self.show_log.expect(pp=18, title="Сникерс Fresh")
        self.show_log.expect(pp=18, title="Сникерс Breath")
        self.show_log.expect(pp=18, title="Сникерс Air")
        self.show_log.expect(pp=9, title="Сникерс Небо")
        self.show_log.expect(pp=9, title="Сникерс Кислород")
        self.show_log.expect(pp=9, title="Сникерс Выдох")
        self.show_log.expect(pp=9, title="Сникерс Вдох")
        self.show_log.expect(pp=9, title="Сникерс Дыши")
        self.show_log.expect(pp=10, title="Сникерс Воздух")
        self.show_log.expect(pp=10, title="Сникерс Oxy")
        self.show_log.expect(pp=10, title="Сникерс Fresh")
        self.show_log.expect(pp=10, title="Сникерс Breath")
        self.show_log.expect(pp=10, title="Сникерс Air")

    # паттерн с тремя врезками, так как 10 офферов не хватит на все три,
    # третья врезка не должна попадать в результат.
    def test_blender_patterns_insufficient_offers(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test3 = (
            '{"Id":"test3","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":[{"Pp":8,"MaxDocs":3}]'
            '},'
            '{"Id":"search_6","Place":2,"Position":6,'
            '"ShopIncut":[{"Pp":8,"MaxDocs":3}]'
            '},'
            '{"Id":"search_10","Place":2,"Position":10,'
            '"ShopIncut":[{"Pp":8,"MinDocs":5}]'
            '}'
            ']}'
        )
        response = self.report.request_json(request + pattern_test3)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'position': 1,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Небо'}},
                                {'titles': {'raw': 'Сникерс Кислород'}},
                                {'titles': {'raw': 'Сникерс Выдох'}},
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 6,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Вдох'}},
                                {'titles': {'raw': 'Сникерс Дыши'}},
                                {'titles': {'raw': 'Сникерс Воздух'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # далее проверяем логику работы FillingMethod:
    # по количеству офферов в ответе от cpa_shop_incut документы распределяются по врезкам.

    # первая врезка заполнится не по максимуму, чтобы хватило для второй.
    def test_blender_patterns_filling_min(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test4 = (
            '{"Id":"test4","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":[{"Pp":8,"MaxDocs":7}]'
            '},'
            '{"Id":"search_6","Place":2,"Position":6,'
            '"ShopIncut":[{"Pp":8,"MinDocs":4}]'
            '}'
            ']}'
        )
        response = self.report.request_json(request + pattern_test4)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'position': 1,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Небо'}},
                                {'titles': {'raw': 'Сникерс Кислород'}},
                                {'titles': {'raw': 'Сникерс Выдох'}},
                                {'titles': {'raw': 'Сникерс Вдох'}},
                                {'titles': {'raw': 'Сникерс Дыши'}},
                                {'titles': {'raw': 'Сникерс Воздух'}},
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 6,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Oxy'}},
                                {'titles': {'raw': 'Сникерс Fresh'}},
                                {'titles': {'raw': 'Сникерс Breath'}},
                                {'titles': {'raw': 'Сникерс Air'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # второй врезке не должно хватить, так как первая заполнится по максимуму.
    def test_blender_patterns_filling_max(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test5 = (
            '{"Id":"test5","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":[{"Pp":8,"MaxDocs":7,"FillingMethod":"FM_FILL_MAX"}]'
            '},'
            '{"Id":"search_6","Place":2,"Position":6,'
            '"ShopIncut":[{"Pp":8,"MinDocs":4}]'
            '}'
            ']}'
        )
        response = self.report.request_json(request + pattern_test5)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'position': 1,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Небо'}},
                                {'titles': {'raw': 'Сникерс Кислород'}},
                                {'titles': {'raw': 'Сникерс Выдох'}},
                                {'titles': {'raw': 'Сникерс Вдох'}},
                                {'titles': {'raw': 'Сникерс Дыши'}},
                                {'titles': {'raw': 'Сникерс Воздух'}},
                                {'titles': {'raw': 'Сникерс Oxy'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # второй врезке должно хватить, так как первая заполнится по видимым.
    def test_blender_patterns_filling_visible(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test6 = (
            '{"Id":"test6","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":[{"Pp":8,"VisibleDocs":5,"MaxDocs":7}]'
            '},'
            '{"Id":"search_6","Place":2,"Position":6,'
            '"ShopIncut":[{"Pp":8,"MinDocs":4}]'
            '}'
            ']}'
        )
        response = self.report.request_json(request + pattern_test6)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'position': 1,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Небо'}},
                                {'titles': {'raw': 'Сникерс Кислород'}},
                                {'titles': {'raw': 'Сникерс Выдох'}},
                                {'titles': {'raw': 'Сникерс Вдох'}},
                                {'titles': {'raw': 'Сникерс Дыши'}},
                                {'titles': {'raw': 'Сникерс Воздух'}},
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 6,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Oxy'}},
                                {'titles': {'raw': 'Сникерс Fresh'}},
                                {'titles': {'raw': 'Сникерс Breath'}},
                                {'titles': {'raw': 'Сникерс Air'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # второй врезке должно хватить, так как первая заполнится по минимуму,
    # третья врезка далеко за выдачей - ее не будет.
    def test_blender_patterns_filling_min_with_visible(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test7 = (
            '{"Id":"test7","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":[{"Pp":8,"VisibleDocs":7,"MaxDocs":7,"FillingMethod":"FM_FILL_MIN"}]'
            '},'
            '{"Id":"search_6","Place":2,"Position":6,'
            '"ShopIncut":[{"Pp":8,"MinDocs":5}]'
            '},'
            '{"Id":"search_600","Place":2,"Position":600,'
            '"ShopIncut":[{"Pp":8,"MinDocs":1}]'
            '}'
            ']}'
        )
        response = self.report.request_json(request + pattern_test7)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'position': 1,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Небо'}},
                                {'titles': {'raw': 'Сникерс Кислород'}},
                                {'titles': {'raw': 'Сникерс Выдох'}},
                                {'titles': {'raw': 'Сникерс Вдох'}},
                                {'titles': {'raw': 'Сникерс Дыши'}},
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 6,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Воздух'}},
                                {'titles': {'raw': 'Сникерс Oxy'}},
                                {'titles': {'raw': 'Сникерс Fresh'}},
                                {'titles': {'raw': 'Сникерс Breath'}},
                                {'titles': {'raw': 'Сникерс Air'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # здесь обоим врезкам должно достаться по 5 офферов.
    def test_blender_patterns_filling_visible_fair(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test8 = (
            '{"Id":"test8","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":[{"Pp":8,"VisibleDocs":4,"MaxDocs":7,"FillingMethod":"FM_FILL_MIN"}]'
            '},'
            '{"Id":"search_6","Place":2,"Position":6,'
            '"ShopIncut":[{"Pp":8,"VisibleDocs":5,"MaxDocs":7,"FillingMethod":"FM_FILL_MIN"}]'
            '}'
            ']}'
        )
        response = self.report.request_json(request + pattern_test8)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'position': 1,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Небо'}},
                                {'titles': {'raw': 'Сникерс Кислород'}},
                                {'titles': {'raw': 'Сникерс Выдох'}},
                                {'titles': {'raw': 'Сникерс Вдох'}},
                                {'titles': {'raw': 'Сникерс Дыши'}},
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 6,
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Сникерс Воздух'}},
                                {'titles': {'raw': 'Сникерс Oxy'}},
                                {'titles': {'raw': 'Сникерс Fresh'}},
                                {'titles': {'raw': 'Сникерс Breath'}},
                                {'titles': {'raw': 'Сникерс Air'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_blender_patterns_vendor_incuts(cls):
        cls.index.hypertree += [HyperCategory(hid=317, name="батончики", visual=True)]
        cls.index.navtree += [
            NavCategory(nid=1521, hid=317),
        ]

        cls.index.cards += [CardCategory(hid=317)]
        cls.suggester.on_request(part='батончики').respond(
            suggestions=[
                Suggestion(part='батончики', url='/catalog/1521?hid=317&suggest_text=батончики'),
            ]
        )

        cls.index.models += [
            Model(hid=317, hyperid=31422, title='твикс 31422', vbid=60, vendor_id=334, datasource_id=1),
            Model(hid=317, hyperid=31423, title='твикс 31423', vbid=50, vendor_id=334, datasource_id=1),
            Model(hid=317, hyperid=31424, title='твикс 31424', vbid=40, vendor_id=334, datasource_id=1),
            Model(hid=317, hyperid=31425, title='твикс 31425', vbid=30, vendor_id=334, datasource_id=1),
            Model(hid=317, hyperid=31426, title='твикс 31426', vbid=20, vendor_id=334, datasource_id=1),
            Model(hid=317, hyperid=31427, title='твикс 31427', vbid=10, vendor_id=334, datasource_id=1),
            Model(hid=317, hyperid=31432, title='твикс 31432', vbid=61, vendor_id=335, datasource_id=1),
            Model(hid=317, hyperid=31433, title='твикс 31433', vbid=51, vendor_id=335, datasource_id=1),
            Model(hid=317, hyperid=31434, title='твикс 31434', vbid=41, vendor_id=335, datasource_id=1),
            Model(hid=317, hyperid=31435, title='твикс 31435', vbid=31, vendor_id=335, datasource_id=1),
            Model(hid=317, hyperid=31436, title='твикс 31436', vbid=21, vendor_id=335, datasource_id=1),
            Model(hid=317, hyperid=31442, title='твикс 31442', vbid=62, vendor_id=336, datasource_id=1),
            Model(hid=317, hyperid=31443, title='твикс 31443', vbid=52, vendor_id=336, datasource_id=1),
            Model(hid=317, hyperid=31444, title='твикс 31444', vbid=42, vendor_id=336, datasource_id=1),
            Model(hid=317, hyperid=31445, title='твикс 31445', vbid=32, vendor_id=336, datasource_id=1),
            Model(hid=317, hyperid=31452, title='твикс 31452', vbid=1063, vendor_id=337, datasource_id=1),
            Model(hid=317, hyperid=31453, title='твикс 31453', vbid=1053, vendor_id=337, datasource_id=1),
            Model(hid=317, hyperid=31454, title='твикс 31454', vbid=1043, vendor_id=337, datasource_id=1),
            Model(hid=317, hyperid=31462, title='твикс 31462', vbid=1064, vendor_id=338, datasource_id=1),
            Model(hid=317, hyperid=31463, title='твикс 31463', vbid=1054, vendor_id=338, datasource_id=1),
            Model(hid=317, hyperid=31472, title='твикс 31472', vbid=1, vendor_id=339, datasource_id=1),
        ]

        cls.index.shops += [
            Shop(fesh=311, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #1'),
            Shop(fesh=312, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2'),
            Shop(fesh=313, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #3'),
        ]

        cls.index.offers += [
            Offer(
                hyperid=31422,
                fesh=311,
                hid=317,
                ts=31101,
                cpa=Offer.CPA_REAL,
                price=511,
                title="твикс 31422",
                bid=500,
                fee=50,
            ),
            Offer(
                hyperid=31422,
                fesh=312,
                hid=317,
                ts=31102,
                cpa=Offer.CPA_REAL,
                price=511,
                title="твикс 31422",
                bid=480,
                fee=20,
            ),
            Offer(
                hyperid=31422,
                fesh=313,
                hid=317,
                ts=31103,
                cpa=Offer.CPA_REAL,
                price=511,
                title="твикс 31422",
                bid=10,
                fee=30,
            ),
            Offer(
                hyperid=31423,
                fesh=311,
                hid=317,
                ts=31201,
                cpa=Offer.CPA_REAL,
                price=530,
                title="твикс 31423",
                bid=10,
                fee=40,
            ),
            Offer(
                hyperid=31423,
                fesh=312,
                hid=317,
                ts=31202,
                cpa=Offer.CPA_REAL,
                price=530,
                title="твикс 31423",
                bid=450,
                fee=50,
            ),
            Offer(
                hyperid=31424,
                fesh=311,
                hid=317,
                ts=31111,
                cpa=Offer.CPA_REAL,
                price=540,
                title="твикс 31424",
                bid=500,
                fee=60,
            ),
            Offer(
                hyperid=31425,
                fesh=311,
                hid=317,
                ts=31111,
                cpa=Offer.CPA_REAL,
                price=550,
                title="твикс 31425",
                bid=300,
                fee=70,
            ),
            Offer(
                hyperid=31426,
                fesh=311,
                hid=317,
                ts=31111,
                cpa=Offer.CPA_REAL,
                price=560,
                title="твикс 31426",
                bid=800,
                fee=80,
            ),
            Offer(
                hyperid=31427,
                fesh=311,
                hid=317,
                ts=31111,
                cpa=Offer.CPA_REAL,
                price=570,
                title="твикс 31427",
                bid=800,
                fee=90,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31101).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31102).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31103).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31201).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31202).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31111).respond(0.005)

        cls.index.vendors_banners += [
            VendorBanner(1, 334, 317, 1, 1000),
            VendorBanner(1, 335, 317, 1, 1200),
            VendorBanner(1, 336, 317, 1, 1400),
            VendorBanner(1, 339, 317, 1, 1600),
        ]

    # сумма ставок для 4 моделей выше у вендора 335, вторым будет 334
    # также сверяем записи в логе показов с правильными параметрами
    # у 334 вендора проверяем наличие ДО
    def test_blender_patterns_vendor_incuts_bids_wo_banner(self):
        request = (
            'pp=18&place=blender&text=твикс&hid=317&use-default-offers=1&show-urls=productVendorBid,cpa'
            '&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_report_blender_pattern_factors=,,5,,1;'
            'market_vendor_incut_hide_undeliverable_models=0;market_report_blender_pattern='
        )
        pattern_vi_test1 = (
            '{"Id":"vi_test1","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":['
            '{"Pp":230,"MinDocs":4,"MaxDocs":5}'
            ']},'
            '{"Id":"search_6","Place":2,"Position":6,"VendorIncut":['
            '{"Pp":231,"MinDocs":4,"MaxDocs":6}'
            ']}'
            ']}'
        )
        response = self.report.request_json(request + pattern_vi_test1)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'position': 1,
                            'inClid': 3,
                            'brand_id': 335,
                            'items': [
                                {'titles': {'raw': 'твикс 31432'}},
                                {'titles': {'raw': 'твикс 31433'}},
                                {'titles': {'raw': 'твикс 31434'}},
                                {'titles': {'raw': 'твикс 31435'}},
                                {'titles': {'raw': 'твикс 31436'}},
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 6,
                            'inClid': 3,
                            'brand_id': 334,
                            'items': [
                                {
                                    'titles': {'raw': 'твикс 31422'},
                                    'offers': {
                                        'count': 3,
                                        'items': [
                                            {
                                                'entity': 'offer',
                                                'urls': {'cpa': Contains('/pp=231/', '/position=1/')},
                                                'shop': {'id': 311},
                                            }
                                        ],
                                    },
                                },
                                {
                                    'titles': {'raw': 'твикс 31423'},
                                    'offers': {
                                        'count': 2,
                                        'items': [
                                            {
                                                'entity': 'offer',
                                                'urls': {'cpa': Contains('/pp=231/', '/position=2/')},
                                                'shop': {'id': 312},
                                            }
                                        ],
                                    },
                                },
                                {
                                    'titles': {'raw': 'твикс 31424'},
                                    'offers': {
                                        'count': 1,
                                        'items': [
                                            {
                                                'entity': 'offer',
                                                'urls': {'cpa': Contains('/pp=231/', '/position=3/')},
                                                'shop': {'id': 311},
                                            }
                                        ],
                                    },
                                },
                                {
                                    'titles': {'raw': 'твикс 31425'},
                                    'offers': {
                                        'count': 1,
                                        'items': [
                                            {
                                                'entity': 'offer',
                                                'urls': {'cpa': Contains('/pp=231/', '/position=4/')},
                                                'shop': {'id': 311},
                                            }
                                        ],
                                    },
                                },
                                {
                                    'titles': {'raw': 'твикс 31426'},
                                    'offers': {
                                        'count': 1,
                                        'items': [
                                            {
                                                'entity': 'offer',
                                                'urls': {'cpa': Contains('/pp=231/', '/position=5/')},
                                                'shop': {'id': 311},
                                            }
                                        ],
                                    },
                                },
                                {
                                    'titles': {'raw': 'твикс 31427'},
                                    'offers': {
                                        'count': 1,
                                        'items': [
                                            {
                                                'entity': 'offer',
                                                'urls': {'cpa': Contains('/pp=231/', '/position=6/')},
                                                'shop': {'id': 311},
                                            }
                                        ],
                                    },
                                },
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        # 1 врезка
        self.show_log.expect(
            pp=230,
            inclid=3,
            title='твикс 31432',
            position=1,
            incut_position=1,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=230,
            inclid=3,
            title='твикс 31433',
            position=2,
            incut_position=1,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=230,
            inclid=3,
            title='твикс 31434',
            position=3,
            incut_position=1,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=230,
            inclid=3,
            title='твикс 31435',
            position=4,
            incut_position=1,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=230,
            inclid=3,
            title='твикс 31436',
            position=5,
            incut_position=1,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        # 2 врезка, модели
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31422',
            position=1,
            incut_position=6,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31423',
            position=2,
            incut_position=6,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31424',
            position=3,
            incut_position=6,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31425',
            position=4,
            incut_position=6,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31426',
            position=5,
            incut_position=6,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31427',
            position=6,
            incut_position=6,
            url_type=ClickType.MODEL,
            url=LikeUrl(url_host='market.yandex.ru', url_params='hid=317'),
        )
        # 2 врезка, ДО
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31422',
            position=1,
            incut_position=6,
            url_type=ClickType.CPA,
            url=LikeUrl(url_host='www.shop-311.ru'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31423',
            position=2,
            incut_position=6,
            url_type=ClickType.CPA,
            url=LikeUrl(url_host='www.shop-312.ru'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31424',
            position=3,
            incut_position=6,
            url_type=ClickType.CPA,
            url=LikeUrl(url_host='www.shop-311.ru'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31425',
            position=4,
            incut_position=6,
            url_type=ClickType.CPA,
            url=LikeUrl(url_host='www.shop-311.ru'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31426',
            position=5,
            incut_position=6,
            url_type=ClickType.CPA,
            url=LikeUrl(url_host='www.shop-311.ru'),
        )
        self.show_log.expect(
            pp=231,
            inclid=3,
            title='твикс 31427',
            position=6,
            incut_position=6,
            url_type=ClickType.CPA,
            url=LikeUrl(url_host='www.shop-311.ru'),
        )

    def test_blender_patterns_vendor_incuts_without_banner_from_apps(self):
        """
        приложения не умеют рисовать врезку с баннером, но передают, что умеют
        проверяем, что врезка будет без баннера, если приходим из приложения
        """
        response = self.make_cap_request(3, client="ANDROID")
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'inClid': 3,
                            'brand_id': 1334,
                            'items': [
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.make_cap_request(3)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'inClid': 3,
                            'brand_id': 1334,
                            'items': [
                                {'entity': 'vendorBanner'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                                {'entity': 'product'},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # с баннером победит 336, вторым будет 335
    # также проверим, что &cpa=real не влияет на формирование врезок
    def test_blender_patterns_vendor_incuts_bids_with_banner(self):
        request = (
            'pp=18&place=blender&text=твикс&hid=317&cpa=real&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_report_blender_pattern_factors=,,5,,1'
            ';market_report_blender_pattern_min_results_for_incuts=1;market_vendor_incut_enable_banners=1'
            ';market_vendor_incut_hide_undeliverable_models=0;market_report_blender_pattern='
        )
        pattern_vi_test2 = (
            '{"Id":"vi_test2","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":['
            '{"Pp":230,"MinDocs":4,"MaxDocs":5,"Dimensions":{}}'
            ']},'
            '{"Id":"search_6","Place":2,"Position":6,"VendorIncut":['
            '{"Pp":230,"MinDocs":4,"MaxDocs":6,"Dimensions":{}}'
            ']}'
            ']}'
        )
        response = self.report.request_json(request + pattern_vi_test2)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'position': 1,
                            'inClid': 3,
                            'brand_id': 336,
                            'items': [
                                {'entity': 'vendorBanner'},
                                {'titles': {'raw': 'твикс 31442'}},
                                {'titles': {'raw': 'твикс 31443'}},
                                {'titles': {'raw': 'твикс 31444'}},
                                {'titles': {'raw': 'твикс 31445'}},
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 6,
                            'inClid': 3,
                            'brand_id': 335,
                            'items': [
                                {'entity': 'vendorBanner'},
                                {'titles': {'raw': 'твикс 31432'}},
                                {'titles': {'raw': 'твикс 31433'}},
                                {'titles': {'raw': 'твикс 31434'}},
                                {'titles': {'raw': 'твикс 31435'}},
                                {'titles': {'raw': 'твикс 31436'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # для смешанного варианта победят 338 и 337 со ставками на моделях больше,
    # чем у вендоров с баннерами, третьим будет 334
    def test_blender_patterns_vendor_incuts_bids_mixed(self):
        request = (
            'pp=18&place=blender&text=твикс&hid=317&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_report_blender_pattern_factors=,,5,,1'
            ';market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0;market_report_blender_pattern='
        )
        pattern_vi_test3 = (
            '{"Id":"vi_test3","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":['
            '{"Pp":230,"MinDocs":2,"MaxDocs":2},'
            '{"Pp":230,"MinDocs":2,"MaxDocs":6,"Dimensions":{}}'
            ']},'
            '{"Id":"search_6","Place":2,"Position":6,"VendorIncut":['
            '{"Pp":230,"MinDocs":2,"MaxDocs":6},'
            '{"Pp":230,"MinDocs":2,"MaxDocs":6,"Dimensions":{}}'
            ']},'
            '{"Id":"search_9","Place":2,"Position":9,"VendorIncut":['
            '{"Pp":230,"MinDocs":2,"MaxDocs":6},'
            '{"Pp":230,"MinDocs":6,"MaxDocs":6,"Dimensions":{}}'
            ']}'
            ']}'
        )
        response = self.report.request_json(request + pattern_vi_test3)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'position': 1,
                            'inClid': 3,
                            'brand_id': 338,
                            'items': [
                                {'titles': {'raw': 'твикс 31462'}},
                                {'titles': {'raw': 'твикс 31463'}},
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 6,
                            'inClid': 3,
                            'brand_id': 337,
                            'items': [
                                {'titles': {'raw': 'твикс 31452'}},
                                {'titles': {'raw': 'твикс 31453'}},
                                {'titles': {'raw': 'твикс 31454'}},
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 9,
                            'inClid': 3,
                            'brand_id': 334,
                            'items': [
                                {'entity': 'vendorBanner'},
                                {'titles': {'raw': 'твикс 31422'}},
                                {'titles': {'raw': 'твикс 31423'}},
                                {'titles': {'raw': 'твикс 31424'}},
                                {'titles': {'raw': 'твикс 31425'}},
                                {'titles': {'raw': 'твикс 31426'}},
                                {'titles': {'raw': 'твикс 31427'}},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # покажутся только баннеры в порядке уменьшения ставок
    # еще проверим, как формируются урлы для баннеров
    def test_blender_patterns_vendor_incuts_bids_banners_only(self):
        request = (
            'pp=18&place=blender&text=твикс&hid=317&show-urls=decrypted,external,direct'
            '&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_report_blender_pattern_factors=,,5,,1'
            ';market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0;market_report_blender_pattern='
        )
        pattern_vi_test4 = (
            '{"Id":"vi_test4","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":['
            '{"Pp":230,"MinDocs":0,"MaxDocs":0,"Dimensions":{}}'
            ']},'
            '{"Id":"search_6","Place":2,"Position":6,"VendorIncut":['
            '{"Pp":230,"MinDocs":0,"MaxDocs":0,"Dimensions":{}}'
            ']},'
            '{"Id":"search_9","Place":2,"Position":9,"VendorIncut":['
            '{"Pp":230,"MinDocs":0,"MaxDocs":0,"Dimensions":{}}'
            ']}'
            ']}'
        )
        response = self.report.request_json(request + pattern_vi_test4)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'position': 1,
                            'inClid': 3,
                            'brand_id': 339,
                            'items': [
                                {
                                    'entity': 'vendorBanner',
                                    'urls': {
                                        'direct': NotEmpty(),
                                        'decrypted': NoKey('decrypted'),
                                    },
                                }
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 6,
                            'inClid': 3,
                            'brand_id': 336,
                            'items': [
                                {
                                    'entity': 'vendorBanner',
                                    'urls': {
                                        'direct': NotEmpty(),
                                        'decrypted': NoKey('decrypted'),
                                    },
                                }
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 9,
                            'inClid': 3,
                            'brand_id': 335,
                            'items': [
                                {
                                    'entity': 'vendorBanner',
                                    'urls': {
                                        'direct': NotEmpty(),
                                        'decrypted': NoKey('decrypted'),
                                    },
                                }
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # проверяем работу флага market_report_blender_pattern_min_results_for_incuts,
    # отключающего врезки при недостаточном количестве результатов основной выдачи
    def test_blender_patterns_vendor_incuts_min_results_exp(self):
        request = (
            'pp=18&place=blender&text=твикс&hid=317&rearr-factors=market_vendor_incut_with_CPA_offers_only=0'
            ';market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0;market_report_blender_pattern='
        )
        pattern_vi_test5 = (
            '{"Id":"vi_test2","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":['
            '{"Pp":230,"MinDocs":4,"MaxDocs":5,"Dimensions":{}}'
            ']},'
            '{"Id":"search_6","Place":2,"Position":6,"VendorIncut":['
            '{"Pp":230,"MinDocs":4,"MaxDocs":6,"Dimensions":{}}'
            ']}'
            ']}'
        )
        response = self.report.request_json(
            request + pattern_vi_test5 + ";market_report_blender_pattern_min_results_for_incuts=500"
        )
        self.assertFragmentIn(response, {'incuts': {'results': EmptyList()}})

    # проверяем смешаный шаблон с врезкой или мимикрией.
    # при достаточном количестве офферов показывается врезка
    def test_blender_patterns_mixed_incut_10_offers(self):
        request = 'pp=18&place=blender&text=сникерс&rearr-factors=market_report_blender_pattern='
        pattern_test_mixed1 = (
            '{"Id":"test9","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":['
            '{"Pp":8,"MinDocs":5,"MaxDocs":10},'
            '{"Pp":8,"MinDocs":2,"MaxDocs":2}'
            ']}]}'
        )
        response = self.report.request_json(request + pattern_test_mixed1)
        self.assertFragmentIn(response, {'incuts': {'results': [{"items": ElementCount(10)}]}})

    # при недостаточном для врезки количестве офферов показывается мимикрия
    # также проверим, что &cpa=no не влияет на выдачу врезок
    def test_blender_patterns_mixed_incut_2_offers_grid_touch(self):
        request = 'pp=18&place=blender&text=небо&cpa=no&rearr-factors=market_report_blender_pattern='
        pattern_test_mixed2 = (
            '{"Id":"test9","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":['
            '{"Pp":8,"MinDocs":5,"MaxDocs":10},'
            '{"Pp":8,"MinDocs":2,"MaxDocs":2}'
            ']}]}'
        )
        response = self.report.request_json(
            request + pattern_test_mixed2 + ";market_report_blender_pattern_min_results_for_incuts=1"
        )
        self.assertFragmentIn(response, {'incuts': {'results': [{"items": ElementCount(2)}]}})

    # при недостаточном для мимикрии количестве офферов не показывается ничего
    def test_blender_patterns_mixed_incut_2_offers_grid_desktop(self):
        request = 'pp=18&place=blender&text=небо&rearr-factors=market_report_blender_pattern='
        pattern_test_mixed3 = (
            '{"Id":"test9","Places":['
            '{"Id":"search_1","Place":2,"Position":1,'
            '"ShopIncut":['
            '{"Pp":8,"MinDocs":5,"MaxDocs":10},'
            '{"Pp":8,"MinDocs":3,"MaxDocs":3}'
            ']}]}'
        )
        response = self.report.request_json(
            request + pattern_test_mixed3 + ";market_report_blender_pattern_min_results_for_incuts=1"
        )
        self.assertFragmentIn(response, {'incuts': {'results': EmptyList()}})

    # проверяем статейную врезку и showUid для врезки внутри поисковой выдачи
    def test_blender_patterns_materials_incut_from_saas(self):
        request = 'pp=18&place=blender&text=сникерс&additional_entities=articles&rearr-factors=market_ugc_saas_enabled=1;market_report_blender_pattern='
        pattern_test_materials = (
            '{"Id":"test_materials","Places":['
            '{"Id":"search_4","Place":2,"Position":4,'
            '"Materials":'
            '{"MinDocs":2,"MaxDocs":3}'
            '}]}'
        )
        response = self.report.request_json(request + pattern_test_materials)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "materialEntrypoints",
                            "position": 4,
                            "showUid": Regex("[0-9]{21}34004"),
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Шоколадный батончик своими руками",
                                    "showUid": Regex("[0-9]{21}35001"),
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как выбрать начинку для батончика",
                                    "showUid": Regex("[0-9]{21}35002"),
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "7 правил хорошего питания",
                                    "showUid": Regex("[0-9]{21}35003"),
                                },
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_blender_patterns_capabilities(cls):
        cls.index.shops += [
            Shop(fesh=1311, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #1'),
        ]

        cls.index.models += [Model(hid=1317, hyperid=131422 + i, vbid=60 + i, vendor_id=1334) for i in range(0, 600)]
        cls.index.models += [Model(hid=1317, hyperid=132422 + i, vbid=20 + i, vendor_id=1336) for i in range(0, 600)]

        cls.index.offers += [
            Offer(hid=1317, hyperid=131422 + i, bid=60 + i, fee=100, vendor_id=1334, fesh=1311, cpa=Offer.CPA_REAL)
            for i in range(0, 600)
        ]
        cls.index.offers += [
            Offer(hid=1317, hyperid=132422 + i, bid=60 + i, fee=100, vendor_id=1336, fesh=1311, cpa=Offer.CPA_REAL)
            for i in range(0, 600)
        ]

        cls.index.vendors_banners += [
            VendorBanner(1, 1334, 1317, 1, 4000),
        ]
        cls.index.vendors_banners += [
            VendorBanner(1, 1336, 1317, 1, 3000),
        ]

    # проверяем то, как блендер формирует шаблон, основываясь на возможностях фронта,
    # которые передаются в названии шаблона битовой маской в hex
    def make_cap_request(self, caps, extra="", factors=',,5,,1', page=1, touch=0, viewtype='list', client='frontend'):
        request = (
            'pp=18&place=blender&hid=1317&cpa=no&use-default-offers=1&show-urls=productVendorBid,cpa'
            '&page={}&numdoc=48&touch={}&viewtype={}'
            '&rearr-factors=market_report_blender_pattern_min_results_for_incuts=1;market_vendor_incut_hide_undeliverable_models=0;'
            'market_vendor_incut_enable_banners=1;market_report_blender_pattern_factors={};'
            'market_report_blender_pattern={}{}'
            '&client={}'
        )
        return self.report.request_json(request.format(page, touch, viewtype, factors, hex(caps), extra, client))

    # фронт, который не умеет ничего, получает тоже ничего
    def test_blender_patterns_capabilities_no_caps(self):
        response = self.make_cap_request(0)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': EmptyList(),
                }
            },
        )

    # умеет вендорскую врезку
    def test_blender_patterns_capabilities_vendor_incut(self):
        response = self.make_cap_request(1)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'items': NotEmptyList(),
                        }
                    ]
                }
            },
        )

    # умеет вендорскую врезку с баннером или без
    def test_blender_patterns_capabilities_vendor_incut_with_banner(self):
        response = self.make_cap_request(3)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'items': [
                                {
                                    'entity': 'vendorBanner',
                                }
                            ],
                        }
                    ]
                }
            },
        )

    # умеет магазинную врезку в 1 позиции
    def test_blender_patterns_capabilities_shop_incut_pos1(self):
        response = self.make_cap_request(0x10)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 1,
                            'items': ElementCount(10),
                        }
                    ]
                }
            },
        )

    # умеет мимикрию в 1 позиции
    def test_blender_patterns_capabilities_mimicry_pos1(self):
        response = self.make_cap_request(0x20)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 1,
                            'items': ElementCount(1),
                        }
                    ]
                }
            },
        )

    # умеет магазинную врезку в 3 позиции
    def test_blender_patterns_capabilities_shop_incut_pos3(self):
        response = self.make_cap_request(0x40)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 3,
                            'items': ElementCount(10),
                        }
                    ]
                }
            },
        )

    # умеет мимикрию в 4 позиции
    def test_blender_patterns_capabilities_mimicry_pos4(self):
        response = self.make_cap_request(0x80)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 4,
                            'items': ElementCount(1),
                        }
                    ]
                }
            },
        )

    # умеет богатый сниппет и мимикрию в 4 позиции (отобразится богатый сниппет)
    def test_blender_patterns_capabilities_rich_snippet_pos4(self):
        response = self.make_cap_request(0x4080)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'richSnippet',
                            'place': 2,
                            'position': 4,
                            'items': ElementCount(1),
                        }
                    ]
                }
            },
        )

    # умеет богатый сниппет в 5 позиции
    def test_blender_patterns_capabilities_rich_snippet_pos5(self):
        response = self.make_cap_request(0x8000)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'richSnippet',
                            'place': 2,
                            'position': 5,
                            'items': ElementCount(1),
                        }
                    ]
                }
            },
        )

    # умеет богатый сниппет в 8 позиции
    def test_blender_patterns_capabilities_rich_snippet_pos8(self):
        response = self.make_cap_request(0x10000)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'richSnippet',
                            'place': 2,
                            'position': 8,
                            'items': ElementCount(1),
                        }
                    ]
                }
            },
        )

    # умеет адовую карусель в 4 позиции
    def test_blender_patterns_capabilities_adv_carousel_pos4(self):
        response = self.make_cap_request(0x40000)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'advertisingCarouselIncut',
                            'place': 2,
                            'position': 4,
                            'items': ElementCount(10),
                        }
                    ]
                }
            },
        )

    # умеет адовую карусель в 5 позиции
    def test_blender_patterns_capabilities_adv_carousel_pos5(self):
        response = self.make_cap_request(0x80000)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'advertisingCarouselIncut',
                            'place': 2,
                            'position': 5,
                            'items': ElementCount(10),
                        }
                    ]
                }
            },
        )

    # умеет адовую карусель в 7 позиции
    def test_blender_patterns_capabilities_adv_carousel_pos8(self):
        response = self.make_cap_request(0x100000)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'advertisingCarouselIncut',
                            'place': 2,
                            'position': 8,
                            'items': ElementCount(10),
                        }
                    ]
                }
            },
        )

    # умеет статейную врезку в 8 позиции
    def test_blender_patterns_capabilities_materials_incut_from_saas(self):
        request = 'pp=18&place=blender&text=сникерс&additional_entities=articles&rearr-factors=market_report_blender_pattern=0x100;market_ugc_saas_enabled=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'materialEntrypoints',
                            'place': 2,
                            'position': 8,
                            'items': ElementCount(3),
                        }
                    ]
                }
            },
        )

    # если есть вендорская врезка, магазинные не отображаются
    def test_blender_patterns_capabilities_vendor_incut_removes_shop_incuts(self):
        response = self.make_cap_request(2 + 0x10 + 0x40)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'items': NotEmptyList(),
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                        }
                    ]
                }
            },
        )

    # переопределим значение минимальной позиции магазинной врезки при наличии
    # вендорской в шапке, тогда магазинная тоже отобразится
    def test_blender_patterns_capabilities_vendor_incut_does_not_remove_shop_incuts(self):
        response = self.make_cap_request(2 + 0x10, factors=',1,5')
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 1,
                            'items': NotEmptyList(),
                        },
                    ]
                }
            },
        )

    # если есть вендорская врезка, мимикрия все равно отображается
    def test_blender_patterns_capabilities_vendor_incut_does_not_remove_mimicry(self):
        response = self.make_cap_request(2 + 0x20 + 0x80)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 1,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 4,
                            'items': ElementCount(1),
                        },
                    ]
                }
            },
        )

    # если есть вендорская врезка, мимикрия не отобразится с дефолтовым значением позиции мимикрии
    def test_blender_patterns_capabilities_vendor_incut_does_not_remove_mimicry_2(self):
        response = self.make_cap_request(2 + 0x20 + 0x80, factors=',,5')
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'items': NotEmptyList(),
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAdsSnippets',
                        }
                    ]
                }
            },
        )

    # если есть магазинная в 1 позиции, то в 3 не отображается
    def test_blender_patterns_capabilities_shop_incut_pos1_removes_pos3(self):
        response = self.make_cap_request(0x10 + 0x40)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 1,
                            'items': NotEmptyList(),
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 3,
                        }
                    ]
                }
            },
        )

    # проверим, как market_report_blender_pattern_disabled_caps отключит
    # магазинную в 1 позиции, чтобы отобразилась в 3
    def test_blender_patterns_capabilities_shop_incut_pos1_disabled(self):
        response = self.make_cap_request(0x10 + 0x40, ";market_report_blender_pattern_disabled_caps=0x10")
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 3,
                            'items': NotEmptyList(),
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 1,
                        }
                    ]
                }
            },
        )

    # проверим, как работает генерация шаблона для фронта,
    # который умеет рисовать в любых позициях

    # десктоп, лист, умеет все
    def test_blender_patterns_dynamic_desktop_list_default(self):
        response = self.make_cap_request(0x1E07)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'brand_id': 1334,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 4,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 8,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 15,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 25,
                            'items': ElementCount(10),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 28,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 42,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 43,
                            'items': ElementCount(1),
                        },
                    ]
                }
            },
        )
        response = self.make_cap_request(0x1E07, page=2)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'brand_id': 1336,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 51,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 54,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 58,
                            'items': ElementCount(10),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 65,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 66,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 67,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 79,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 92,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 93,
                            'items': ElementCount(1),
                        },
                    ]
                }
            },
        )

    # десктоп, грид. проверим также renderMap и width
    def test_blender_patterns_dynamic_desktop_grid_default(self):
        response = self.make_cap_request(0x1E07, viewtype='grid')
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'brand_id': 1334,
                            'width': 3,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 7,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 8,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 9,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 19,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 20,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 21,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 25,
                            'width': 3,
                            'items': ElementCount(10),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 40,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 41,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 42,
                            'items': ElementCount(1),
                        },
                    ]
                },
                'renderMap': {
                    'gridWidth': 3,
                    'header': Regex("[0-9]{21}43000"),
                    'list': [
                        Regex("[0-9]{21}16001"),
                        Regex("[0-9]{21}16002"),
                        Regex("[0-9]{21}16003"),
                        Regex("[0-9]{21}16004"),
                        Regex("[0-9]{21}16005"),
                        Regex("[0-9]{21}16006"),
                        Regex("[0-9]{21}53007"),
                        Regex("[0-9]{21}53008"),
                        Regex("[0-9]{21}53009"),
                        Regex("[0-9]{21}16007"),
                        Regex("[0-9]{21}16008"),
                        Regex("[0-9]{21}16009"),
                        Regex("[0-9]{21}16010"),
                        Regex("[0-9]{21}16011"),
                        Regex("[0-9]{21}16012"),
                        Regex("[0-9]{21}16013"),
                        Regex("[0-9]{21}16014"),
                        Regex("[0-9]{21}16015"),
                        Regex("[0-9]{21}53019"),
                        Regex("[0-9]{21}53020"),
                        Regex("[0-9]{21}53021"),
                        Regex("[0-9]{21}16016"),
                        Regex("[0-9]{21}16017"),
                        Regex("[0-9]{21}16018"),
                        Regex("[0-9]{21}38025"),
                        Regex("[0-9]{21}16019"),
                        Regex("[0-9]{21}16020"),
                        Regex("[0-9]{21}16021"),
                        Regex("[0-9]{21}16022"),
                        Regex("[0-9]{21}16023"),
                        Regex("[0-9]{21}16024"),
                        Regex("[0-9]{21}16025"),
                        Regex("[0-9]{21}16026"),
                        Regex("[0-9]{21}16027"),
                        Regex("[0-9]{21}16028"),
                        Regex("[0-9]{21}16029"),
                        Regex("[0-9]{21}16030"),
                        Regex("[0-9]{21}53040"),
                        Regex("[0-9]{21}53041"),
                        Regex("[0-9]{21}53042"),
                        Regex("[0-9]{21}16031"),
                        Regex("[0-9]{21}16032"),
                        Regex("[0-9]{21}16033"),
                        Regex("[0-9]{21}16034"),
                        Regex("[0-9]{21}16035"),
                        Regex("[0-9]{21}16036"),
                        Regex("[0-9]{21}16037"),
                        Regex("[0-9]{21}16038"),
                        Regex("[0-9]{21}16039"),
                        Regex("[0-9]{21}16040"),
                        Regex("[0-9]{21}16041"),
                        Regex("[0-9]{21}16042"),
                        Regex("[0-9]{21}16043"),
                        Regex("[0-9]{21}16044"),
                        Regex("[0-9]{21}16045"),
                        Regex("[0-9]{21}16046"),
                        Regex("[0-9]{21}16047"),
                        Regex("[0-9]{21}16048"),
                    ],
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.make_cap_request(0x1E07, viewtype='grid', page=2, factors=',1,5')
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'brand_id': 1336,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 58,
                            'items': ElementCount(10),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 67,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 68,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 69,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 91,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 92,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 93,
                            'items': ElementCount(1),
                        },
                    ]
                },
                'renderMap': {
                    'gridWidth': 3,
                    'header': Regex("[0-9]{21}43000"),
                    'list': [
                        Regex("[0-9]{21}16049"),
                        Regex("[0-9]{21}16050"),
                        Regex("[0-9]{21}16051"),
                        Regex("[0-9]{21}16052"),
                        Regex("[0-9]{21}16053"),
                        Regex("[0-9]{21}16054"),
                        Regex("[0-9]{21}16055"),
                        Regex("[0-9]{21}16056"),
                        Regex("[0-9]{21}16057"),
                        Regex("[0-9]{21}38058"),
                        Regex("[0-9]{21}16058"),
                        Regex("[0-9]{21}16059"),
                        Regex("[0-9]{21}16060"),
                        Regex("[0-9]{21}16061"),
                        Regex("[0-9]{21}16062"),
                        Regex("[0-9]{21}16063"),
                        Regex("[0-9]{21}53067"),
                        Regex("[0-9]{21}53068"),
                        Regex("[0-9]{21}53069"),
                        Regex("[0-9]{21}16064"),
                        Regex("[0-9]{21}16065"),
                        Regex("[0-9]{21}16066"),
                        Regex("[0-9]{21}16067"),
                        Regex("[0-9]{21}16068"),
                        Regex("[0-9]{21}16069"),
                        Regex("[0-9]{21}16070"),
                        Regex("[0-9]{21}16071"),
                        Regex("[0-9]{21}16072"),
                        Regex("[0-9]{21}16073"),
                        Regex("[0-9]{21}16074"),
                        Regex("[0-9]{21}16075"),
                        Regex("[0-9]{21}16076"),
                        Regex("[0-9]{21}16077"),
                        Regex("[0-9]{21}16078"),
                        Regex("[0-9]{21}16079"),
                        Regex("[0-9]{21}16080"),
                        Regex("[0-9]{21}16081"),
                        Regex("[0-9]{21}16082"),
                        Regex("[0-9]{21}16083"),
                        Regex("[0-9]{21}16084"),
                        Regex("[0-9]{21}53091"),
                        Regex("[0-9]{21}53092"),
                        Regex("[0-9]{21}53093"),
                        Regex("[0-9]{21}16085"),
                        Regex("[0-9]{21}16086"),
                        Regex("[0-9]{21}16087"),
                        Regex("[0-9]{21}16088"),
                        Regex("[0-9]{21}16089"),
                        Regex("[0-9]{21}16090"),
                        Regex("[0-9]{21}16091"),
                        Regex("[0-9]{21}16092"),
                        Regex("[0-9]{21}16093"),
                        Regex("[0-9]{21}16094"),
                        Regex("[0-9]{21}16095"),
                        Regex("[0-9]{21}16096"),
                    ],
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

    # тач, лист
    def test_blender_patterns_dynamic_touch_list_default(self):
        response = self.make_cap_request(0x1E07, touch=1)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'brand_id': 1334,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 4,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 8,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 15,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 25,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 31,
                            'items': ElementCount(10),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 39,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 40,
                            'items': ElementCount(1),
                        },
                    ]
                }
            },
        )
        response = self.make_cap_request(0x1E07, touch=1, page=2)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 53,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'vendorIncut',
                            'place': 2,
                            'position': 61,
                            'brand_id': 1336,
                            'items': ElementCount(10),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 64,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 75,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 81,
                            'items': ElementCount(10),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 87,
                            'items': ElementCount(1),
                        },
                    ]
                }
            },
        )

    # тач, грид
    def test_blender_patterns_dynamic_touch_grid_default(self):
        response = self.make_cap_request(0x1E07, touch=1, viewtype='grid')
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 1,
                            'brand_id': 1334,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 5,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 6,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 13,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 14,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 27,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 28,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAds',
                            'place': 2,
                            'position': 41,
                            'items': ElementCount(10),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 47,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 48,
                            'items': ElementCount(1),
                        },
                    ]
                }
            },
        )
        response = self.make_cap_request(0x1E07, touch=1, page=2, viewtype='grid')
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'place': 2,
                            'position': 61,
                            'brand_id': 1336,
                            'items': NotEmptyList(),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 71,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 72,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 93,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 94,
                            'items': ElementCount(1),
                        },
                    ]
                }
            },
        )

    # проверим, работает ли флаг для фронтовых врезок,
    # мимикрия должна переместиться ниже
    def test_blender_patterns_dynamic_external_incut(self):
        response = self.make_cap_request(
            0x1E07, touch=0, viewtype='grid', extra=';market_report_blender_pattern_external_incuts=3'
        )
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'externalIncut',
                            'place': 2,
                            'position': 7,
                            'items': [
                                {
                                    'entity': 'externalIncutItem',
                                }
                            ],
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'place': 2,
                            'position': 13,
                            'items': ElementCount(1),
                        },
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'place': 2,
                            'position': EqualToOneOf(4, 5, 6, 8, 9, 10, 11, 12),
                        }
                    ]
                }
            },
        )

    def test_blender_patterns_redirect(self):
        response = self.report.request_json(
            'place=blender&cvredirect=1&pp=18&text=батончики+вкусные&rearr-factors=market_report_blender_pattern=0x0'
        )
        self.assertFragmentIn(response, {'redirect': {'params': {"hid": ['317']}}})

    @classmethod
    def prepare_blender_patterns_glfilter_for_incuts(cls):
        cls.index.shops += [
            Shop(fesh=331, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=332, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=333, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
        ]

        cls.index.gltypes += [
            GLType(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, vendor=True, hid=1318),
        ]

        cls.index.models += [
            Model(
                hyperid=322,
                hid=1318,
                vendor_id=4114,
                vbid=30,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4114)],
            ),
            Model(
                hyperid=323,
                hid=1318,
                vendor_id=4115,
                vbid=20,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4115)],
            ),
            Model(
                hyperid=324,
                hid=1318,
                vendor_id=4116,
                vbid=10,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4116)],
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=1318,
                hyperid=322,
                cpa=Offer.CPA_REAL,
                vbid=30,
                fesh=331,
                price=5000,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4114)],
            ),
            Offer(
                hid=1318,
                hyperid=323,
                cpa=Offer.CPA_REAL,
                vbid=20,
                fesh=332,
                price=5000,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4115)],
            ),
            Offer(
                hid=1318,
                hyperid=324,
                cpa=Offer.CPA_REAL,
                fee=1,
                vbid=10,
                fesh=333,
                price=5000,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4116)],
            ),
            Offer(
                hid=1318,
                hyperid=325,
                cpa=Offer.CPA_REAL,
                fee=1,
                vbid=10,
                fesh=333,
                price=5000,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4116)],
            ),
            Offer(
                hid=1318,
                hyperid=326,
                cpa=Offer.CPA_REAL,
                fee=1,
                vbid=10,
                fesh=333,
                price=5000,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4116)],
            ),
            Offer(
                hid=1318,
                hyperid=327,
                cpa=Offer.CPA_REAL,
                fee=1,
                vbid=10,
                fesh=333,
                price=5000,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4116)],
            ),
            Offer(
                hid=1318,
                hyperid=328,
                cpa=Offer.CPA_REAL,
                fee=1,
                vbid=10,
                fesh=333,
                price=5000,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4116)],
            ),
            Offer(
                hid=1318,
                hyperid=329,
                cpa=Offer.CPA_REAL,
                fee=1,
                vbid=10,
                fesh=333,
                price=5000,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=4116)],
            ),
        ]

    # проверим, как работает glfilter в подзапросах за врезками.
    # в поиск должны попасть 6 офферов от 4116 вендора, в магазинную врезку - они же,
    # вендорские врезки должны заполняться без учета фильтра, только по ставкам
    def test_blender_patterns_glfilter_for_incuts(self):
        request = (
            'pp=18&place=blender&hid=1318&use-default-offers=1&show-urls=productVendorBid,cpa&glfilter=7893318:4116'
            '&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_report_blender_pattern_factors=,,5,,1'
            ';market_vendor_incut_hide_undeliverable_models=0;market_report_blender_pattern_min_results_for_incuts=1'
            ';market_report_blender_pattern='
        )
        pattern_vi_test_gl = (
            '{"Id":"vi_test_gl","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":['
            '{"Pp":230,"MinDocs":1,"MaxDocs":2}'
            ']},'
            '{"Id":"search_2","Place":2,"Position":2,"ShopIncut":['
            '{"Pp":8,"MinDocs":4,"MaxDocs":8}'
            ']},'
            '{"Id":"search_3","Place":2,"Position":3,"VendorIncut":['
            '{"Pp":231,"MinDocs":1,"MaxDocs":2}'
            ']}'
            ']}'
        )
        response = self.report.request_json(request + pattern_vi_test_gl)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': ElementCount(7),
                },
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'position': 1,
                            'inClid': 3,
                            'brand_id': 4114,
                            'items': ElementCount(1),
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 2,
                            'inClid': 2,
                            'items': ElementCount(6),
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 3,
                            'inClid': 3,
                            'brand_id': 4115,
                            'items': ElementCount(1),
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

    # проверим, что при недостаточном количестве документов статейная врезка не генерится
    # и не попадает в renderMap
    def test_blender_patterns_materials_below_search(self):
        response = self.report.request_json(
            "pp=18&place=blender&text=твикс&use-default-offers=1&additional_entities=articles"
            "&show-urls=productVendorBid,cpa&entities=offer&rearr-factors=market_report_blender_pattern=0x100"
        )
        self.assertFragmentIn(
            response,
            {
                'search': {'results': ElementCount(6)},
                'incuts': {'results': EmptyList()},
                'renderMap': {'list': ElementCount(6)},
            },
        )

    # проверим, что корректно обрабатывается ситуация, когда &numdoc= меньше ширины гридовой сетки
    def test_blender_patterns_low_numdoc_value(self):
        request = (
            "pp=18&place=blender&text=твикс&use-default-offers=1&additional_entities=articles"
            "&show-urls=productVendorBid,cpa&entities=offer&rearr-factors=market_report_blender_pattern=0x100"
            "&viewtype=grid&numdoc="
        )
        response = self.report.request_json(request + "1&debug=1")
        self.assertFragmentIn(
            response,
            {
                'search': {'results': ElementCount(1)},
                'incuts': {'results': EmptyList()},
                'renderMap': NoKey('renderMap'),
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains(
                                "predefined.cpp",
                                "GetPattern()",
                                "disabled",
                                "numdoc=1",
                                "blender will use default logic",
                            )
                        ],
                    }
                },
            },
        )
        response = self.report.request_json(request + "2")
        self.assertFragmentIn(
            response,
            {
                'search': {'results': ElementCount(2)},
                'incuts': {'results': EmptyList()},
                'renderMap': {'list': ElementCount(2)},
            },
        )

    # проверим, что корректно обрабатывается ситуация,
    # когда &numdoc= меньше market_report_blender_pattern_min_results_for_incuts,
    # но всего документов больше
    def test_blender_patterns_numdoc_value_lower_than_min_results(self):
        request = (
            'pp=18&place=blender&hid=1317&cpa=any&use-default-offers=1'
            '&show-urls=productVendorBid,cpa&page=1&numdoc=48'
            '&rearr-factors=market_report_blender_pattern_min_results_for_incuts=100'
            ';market_vendor_incut_hide_undeliverable_models=0'
            ';market_vendor_incut_enable_banners=1'
            ';market_report_blender_pattern=0x1e07'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {'results': ElementCount(48)},
                'incuts': {'results': NotEmptyList()},
            },
        )

    # тестируем замену pp для приложенек (android)
    def test_blender_patterns_apps_pp_substitution_android(self):
        request = (
            'pp=18&place=blender&text=твикс&hid=317&cpa=any&use-default-offers=1'
            '&show-urls=productVendorBid,cpa&page=1&numdoc=6&platform=touch'
            '&rearr-factors=market_report_blender_pattern_min_results_for_incuts=1'
            ';market_vendor_incut_hide_undeliverable_models=0'
            ';market_vendor_incut_enable_banners=1'
            ';market_report_blender_pattern_factors=,,6,,1'
            ';market_vendor_incut_with_CPA_offers_only=0;'
            ';market_report_blender_pattern={"Id":"test1","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":[{"Pp":608,"MinDocs":2,"MaxDocs":2}]},'
            '{"Id":"search_2","Place":2,"Position":2,"ShopIncut":[{"Pp":621,"MinDocs":1,"MaxDocs":1}]},'
            '{"Id":"search_3","Place":2,"Position":3,"ShopIncut":[{"Pp":620,"MinDocs":4,"MaxDocs":4}]},'
            '{"Id":"search_4","Place":2,"Position":4,"VendorIncut":[{"Pp":608,"MinDocs":2,"MaxDocs":2,"Dimensions":{}}]},'
            '{"Id":"search_5","Place":2,"Position":5,"VendorIncut":[{"Pp":698,"MinDocs":1,"MaxDocs":1}]},'
            '{"Id":"search_6","Place":2,"Position":6,"AdvCarousel":[{"Pp":654,"MinDocs":1,"MaxDocs":2}]}]}'
            '&client=ANDROID'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {'results': ElementCount(6)},
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'position': 1,
                            'items': [
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31462'},
                                    'urls': {'encrypted': Contains('/pp=1708/')},
                                }
                            ],
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'position': 2,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'titles': {'raw': 'твикс 31423'},
                                    'urls': {'cpa': Contains('/pp=1710/')},
                                }
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 3,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'titles': {'raw': 'твикс 31422'},
                                    'urls': {'cpa': Contains('/pp=1709/')},
                                }
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 4,
                            'items': [
                                {
                                    'entity': 'vendorBanner',
                                    'urls': {'encrypted': Contains('/pp=1708/', '/position=0/')},
                                },
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31442'},
                                    'urls': {'encrypted': Contains('/pp=1708/', '/position=1/')},
                                },
                            ],
                        },
                        {
                            'entity': 'richSnippet',
                            'position': 5,
                            'items': [
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31452'},
                                    'urls': {'encrypted': Contains('/pp=1798/', '/position=1/')},
                                },
                            ],
                        },
                        {
                            'entity': 'advertisingCarouselIncut',
                            'position': 6,
                            'items': [
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31427'},
                                    'urls': {'encrypted': Contains('/pp=1754/')},
                                },
                            ],
                        },
                    ]
                },
            },
        )
        self.show_log.expect(pp=1708, incut_position=1, title="твикс 31462")
        self.show_log.expect(pp=1710, incut_position=2, title="твикс 31423")
        self.show_log.expect(pp=1709, incut_position=3, title="твикс 31422")
        self.show_log.expect(pp=1708, incut_position=4, title="твикс 31442")
        self.show_log.expect(pp=1798, incut_position=5, title="твикс 31452")
        self.show_log.expect(pp=1754, incut_position=6, title="твикс 31427")

    # тестируем замену pp для приложенек (ios)
    def test_blender_patterns_apps_pp_substitution_ios(self):
        request = (
            'pp=18&place=blender&text=твикс&hid=317&cpa=any&use-default-offers=1'
            '&show-urls=productVendorBid,cpa&page=1&numdoc=6&platform=touch'
            '&rearr-factors=market_report_blender_pattern_min_results_for_incuts=1'
            ';market_vendor_incut_hide_undeliverable_models=0'
            ';market_vendor_incut_enable_banners=1'
            ';market_report_blender_pattern_factors=,,6,,1'
            ';market_vendor_incut_with_CPA_offers_only=0;'
            ';market_report_blender_pattern={"Id":"test1","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":[{"Pp":608,"MinDocs":2,"MaxDocs":2}]},'
            '{"Id":"search_2","Place":2,"Position":2,"ShopIncut":[{"Pp":621,"MinDocs":1,"MaxDocs":1}]},'
            '{"Id":"search_3","Place":2,"Position":3,"ShopIncut":[{"Pp":620,"MinDocs":4,"MaxDocs":4}]},'
            '{"Id":"search_4","Place":2,"Position":4,"VendorIncut":[{"Pp":608,"MinDocs":2,"MaxDocs":2,"Dimensions":{}}]},'
            '{"Id":"search_5","Place":2,"Position":5,"VendorIncut":[{"Pp":698,"MinDocs":1,"MaxDocs":1}]},'
            '{"Id":"search_6","Place":2,"Position":6,"AdvCarousel":[{"Pp":654,"MinDocs":1,"MaxDocs":2}]}]}'
            '&client=IOS'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {'results': ElementCount(6)},
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'position': 1,
                            'items': [
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31462'},
                                    'urls': {'encrypted': Contains('/pp=1808/')},
                                }
                            ],
                        },
                        {
                            'entity': 'premiumAdsSnippets',
                            'position': 2,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'titles': {'raw': 'твикс 31423'},
                                    'urls': {'cpa': Contains('/pp=1810/')},
                                }
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 3,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'titles': {'raw': 'твикс 31422'},
                                    'urls': {'cpa': Contains('/pp=1809/')},
                                }
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 4,
                            'items': [
                                {
                                    'entity': 'vendorBanner',
                                    'urls': {'encrypted': Contains('/pp=1808/', '/position=0/')},
                                },
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31442'},
                                    'urls': {'encrypted': Contains('/pp=1808/', '/position=1/')},
                                },
                            ],
                        },
                        {
                            'entity': 'richSnippet',
                            'position': 5,
                            'items': [
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31452'},
                                    'urls': {'encrypted': Contains('/pp=1898/', '/position=1/')},
                                },
                            ],
                        },
                        {
                            'entity': 'advertisingCarouselIncut',
                            'position': 6,
                            'items': [
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31427'},
                                    'urls': {'encrypted': Contains('/pp=1854/')},
                                },
                            ],
                        },
                    ]
                },
            },
        )
        self.show_log.expect(pp=1808, incut_position=1, title="твикс 31462")
        self.show_log.expect(pp=1810, incut_position=2, title="твикс 31423")
        self.show_log.expect(pp=1809, incut_position=3, title="твикс 31422")
        self.show_log.expect(pp=1808, incut_position=4, title="твикс 31442")
        self.show_log.expect(pp=1898, incut_position=5, title="твикс 31452")
        self.show_log.expect(pp=1854, incut_position=6, title="твикс 31427")

    # тестируем отсутствие вендорских врезок с запросом без &hid=
    def test_blender_patterns_apps_pp_substitution_ios_2(self):
        request = (
            'pp=18&place=blender&text=твикс&cpa=any&use-default-offers=1'
            '&show-urls=productVendorBid,cpa&page=1&numdoc=5&platform=touch'
            '&rearr-factors=market_report_blender_pattern_min_results_for_incuts=1'
            ';market_vendor_incut_hide_undeliverable_models=0'
            ';market_vendor_incut_enable_banners=1'
            ';market_report_blender_pattern_factors=,,5,,1'
            ';market_vendor_incut_with_CPA_offers_only=0;'
            ';market_report_blender_pattern={"Id":"test1","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":[{"Pp":608,"MinDocs":1,"MaxDocs":1}]},'
            '{"Id":"search_2","Place":2,"Position":2,"ShopIncut":[{"Pp":620,"MinDocs":1,"MaxDocs":1}]},'
            '{"Id":"search_3","Place":2,"Position":3,"ShopIncut":[{"Pp":620,"MinDocs":4,"MaxDocs":4}]},'
            '{"Id":"search_4","Place":2,"Position":4,"VendorIncut":[{"Pp":608,"MinDocs":1,"MaxDocs":1,"Dimensions":{}}]}]}'
            '&client=IOS'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {'results': ElementCount(5)},
                'incuts': {
                    'results': [
                        {
                            'entity': 'premiumAdsSnippets',
                            'position': 2,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'titles': {'raw': 'твикс 31423'},
                                    'urls': {'cpa': Contains('/pp=1809/')},
                                }
                            ],
                        },
                        {
                            'entity': 'premiumAds',
                            'position': 3,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'titles': {'raw': 'твикс 31422'},
                                    'urls': {'cpa': Contains('/pp=1809/')},
                                }
                            ],
                        },
                    ]
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                        }
                    ]
                },
            },
        )

    # тестируем отсутствие магазинных врезок с запросом с сортировкой
    def test_blender_patterns_apps_pp_substitution_ios_3(self):
        request = (
            'pp=18&place=blender&text=твикс&hid=317&how=aprice&cpa=any&use-default-offers=1'
            '&show-urls=productVendorBid,cpa&page=1&numdoc=5&platform=touch'
            '&rearr-factors=market_report_blender_pattern_min_results_for_incuts=1'
            ';market_vendor_incut_hide_undeliverable_models=0'
            ';market_vendor_incut_enable_banners=1'
            ';market_report_blender_pattern_factors=,,5,,1'
            ';market_vendor_incut_with_CPA_offers_only=0;'
            ';market_report_blender_pattern={"Id":"test1","Places":['
            '{"Id":"search_1","Place":2,"Position":1,"VendorIncut":[{"Pp":608,"MinDocs":2,"MaxDocs":2}]},'
            '{"Id":"search_2","Place":2,"Position":2,"ShopIncut":[{"Pp":620,"MinDocs":1,"MaxDocs":1}]},'
            '{"Id":"search_3","Place":2,"Position":3,"ShopIncut":[{"Pp":620,"MinDocs":4,"MaxDocs":4}]},'
            '{"Id":"search_4","Place":2,"Position":4,"VendorIncut":[{"Pp":608,"MinDocs":2,"MaxDocs":2,"Dimensions":{}}]}]}'
            '&client=IOS'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {'results': ElementCount(5)},
                'incuts': {
                    'results': [
                        {
                            'entity': 'vendorIncut',
                            'position': 1,
                            'items': [
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31462'},
                                    'urls': {'encrypted': Contains('/pp=1808/')},
                                }
                            ],
                        },
                        {
                            'entity': 'vendorIncut',
                            'position': 4,
                            'items': [
                                {
                                    'entity': 'vendorBanner',
                                    'urls': {'encrypted': Contains('/pp=1808/', '/position=0/')},
                                },
                                {
                                    'entity': 'product',
                                    'titles': {'raw': 'твикс 31442'},
                                    'urls': {'encrypted': Contains('/pp=1808/', '/position=1/')},
                                },
                            ],
                        },
                    ]
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': Contains('premiumAds'),
                        }
                    ]
                },
            },
        )


if __name__ == '__main__':
    main()

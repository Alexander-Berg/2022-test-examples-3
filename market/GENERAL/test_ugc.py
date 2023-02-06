#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, HyperCategory, HyperCategoryType, MarketSku, MnPlace, NavCategory, Offer, UGCItem
from core.testcase import TestCase, main
from core.matcher import Absent, Round

from json import loads


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
    def prepare_entrypoints_from_saas(cls):
        embs = [0.4] * 50

        cls.saas_ugc.on_request(embs=embs, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=988795807,
                    attributes={
                        'page_id': '56122',
                        'compiled_generation': '20200604_0600',
                        'pages_generation': '20200608_0800',
                        'title': 'Как сделать эко-люстру',
                        'type': 'knowledge',
                        'semantic_id': 'kak-sdelat-ehko-lyustru',
                        'image': Images.IMAGE,
                        'nid': '["1001", "2001", "2123",]',
                    },
                ),
                UGCItem(
                    relevance=892847537,
                    attributes={
                        'page_id': '56124',
                        'title': 'Как выбрать люстру',
                        'subtitle': 'Как же её выбрать',
                        'type': 'expertise',
                        'semantic_id': 'kak-vybrat-lustru',
                        'image': Images.IMAGE,
                        'author_name': 'Федюков Иван Станиславович',
                        'author_description': 'С люстрами на ты',
                        'author_avatar': Images.AVATAR,
                        'nid': '["1001",]',
                    },
                    plain_attributes={
                        'RequestIdInMultiRequest': '0',
                    },
                ),
                UGCItem(
                    relevance=885100722,
                    attributes={
                        'page_id': '56125',
                        'title': '7 правил хорошего освещения',
                        'description': 'Семь, то есть три плюс четыре',
                        'type': 'blog',
                        'semantic_id': '7-pravil-horoshego-osveshhenija',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'nid': '["1001", "1234",]',
                    },
                ),
            ]
        )

        cls.index.navtree += [
            NavCategory(hid=1, nid=1001, is_blue=False, name='Люстры'),
        ]

        cls.index.dssm.hard2_query_embedding.on(query='люстры').set(*embs)
        cls.index.dssm.hard2_query_embedding.on(query='канделябры').set(*embs)

        # ----------------------------------------------------------------
        embs_adult = [0.2] * 50
        cls.saas_ugc.on_request(embs=embs_adult, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=885100722,
                    attributes={
                        'page_id': '12340001',
                        'title': '7 правил хорошего времяпрепровождения',
                        'description': 'Семь, то есть три плюс четыре',
                        'type': 'blog',
                        'semantic_id': '7-pravil-horoshego-vremyapreprovozhdeniya',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'nid': '["3000",]',
                    },
                ),
            ]
        )
        cls.index.dssm.hard2_query_embedding.on(query='вибраторы').set(*embs_adult)
        cls.index.navtree += [NavCategory(hid=6091783, nid=3000, is_blue=False, name='Взрослая категория')]

        # статьи для проверки фильтрации по nid'ам ----------------------------
        embs_trimmers = [0.5] * 50
        cls.index.dssm.hard2_query_embedding.on(query='триммеры').set(*embs_trimmers)

        cls.index.navtree += [
            NavCategory(
                hid=40,
                nid=4000,
                is_blue=False,
                name='Газонокосилки и триммеры',
                children=[
                    NavCategory(hid=41, nid=4001, is_blue=False, name='Триммеры'),
                ],
            ),
            NavCategory(hid=50, nid=5000, is_blue=False, name='Мужские электробритвы'),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=40,
                output_type=HyperCategoryType.GURU,
                name='газонокосилки и триммеры',
                uniq_name='газонокосилки и триммеры',
            ),
            HyperCategory(hid=41, output_type=HyperCategoryType.GURU, name='триммеры', uniq_name='триммеры'),
            HyperCategory(
                hid=50,
                output_type=HyperCategoryType.GURU,
                name='мужские электробритвы',
                uniq_name='мужские электробритвы',
            ),
        ]
        cls.saas_ugc.on_request(embs=embs_trimmers, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=887100722,
                    attributes={
                        'page_id': '57000',
                        'title': 'Обзор триммеров для дачного участка',
                        'description': 'Чем лучше косить траву?',
                        'type': 'overview',
                        'semantic_id': 'obzor-trimmerov-dlya-dachnogo-uchastka',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'nid': '["9999", "4000",]',  # first one doesn't exist
                    },
                ),
                UGCItem(
                    relevance=886100722,
                    attributes={
                        'page_id': '57001',
                        'title': 'Выбор триммера для разных типов бород',
                        'description': 'Как же выбрать триммер для бороды?',
                        'type': 'choose',
                        'semantic_id': 'vybor-trimmera-dlya-raznih-tipov-borod',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'nid': '["5000",]',
                    },
                ),
                UGCItem(
                    relevance=885100722,
                    attributes={
                        'page_id': '57002',
                        'title': 'История триммеров',
                        'description': 'Как люди жили без триммеров?',
                        'type': 'research',
                        'semantic_id': 'istoriya-trimmerov',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        # no nid
                    },
                ),
            ]
        )

        # статьи для проверки того, что статьи с типом journal/blog попадают в конец врезки
        embs_headphones = [0.6] * 50
        cls.index.dssm.hard2_query_embedding.on(query='наушники').set(*embs_headphones)
        cls.saas_ugc.on_request(embs=embs_headphones, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=996000000,
                    attributes={
                        'page_id': '57003',
                        'title': 'Топ наушников этого года',
                        'description': 'Протестировали сами все модели',
                        'type': 'overview',
                        'semantic_id': 'top-naushnikov-etogo-goda',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                    },
                ),
                UGCItem(
                    relevance=995100721,
                    attributes={
                        'page_id': '57004',
                        'title': 'Обзор моих любимых наушников',
                        'description': 'Это самые лучшие наушники в мире!',
                        'type': 'blog',
                        'semantic_id': 'obzor-moih-lubimih-naushnikov',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                    },
                ),
                UGCItem(
                    relevance=885100721,
                    attributes={
                        'page_id': '57005',
                        'title': 'История наушников',
                        'description': 'Как люди жили без наушников?',
                        'type': 'research',
                        'semantic_id': 'istoriya-naushnikov',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                    },
                ),
                UGCItem(
                    relevance=884100721,
                    attributes={
                        'page_id': '57006',
                        'title': 'Развёрнутый отзыв на наушники',
                        'description': 'Изучил со всех сторон',
                        'type': 'blog',
                        'semantic_id': 'razvernutiy-otzyv-na-naushniki',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                    },
                ),
            ]
        )
        # ------------------------------------------------------
        cls.index.offers += [
            Offer(title="люстры 1", ts=1, price=800, hid=1),
            Offer(title="люстры 2", ts=2, price=700, hid=1),
            Offer(title="люстры 3", ts=3, price=600, hid=1),
            Offer(title="люстры 4", ts=4, price=500, hid=1),
            Offer(title="люстры 5", ts=5, price=400, hid=1),
            Offer(title="люстры 6", ts=6, price=300, hid=1),
            Offer(title="люстры 7", ts=7, price=200, hid=1),
            Offer(title="люстры 8", ts=8, price=100, hid=1),
            Offer(title="люстры 9", ts=9, price=90, hid=1),
            Offer(title="люстры 10", ts=10, price=80, hid=1),
            Offer(title="люстры 11", ts=11, price=70, hid=1),
            Offer(title="люстры 12", ts=12, price=60, hid=1),
            Offer(title="люстры 13", ts=13, price=50, hid=1),
            Offer(title="люстры 14", ts=14, price=40, hid=1),
            Offer(title="люстры 15", ts=15, price=30, hid=1),
        ]

        # 16 - 23
        for i in range(1, 9):
            cls.index.offers.append(Offer(title="вибраторы", ts=15 + i, price=i * 100, hid=6091783))
        # 24 - 38
        for i in range(1, 16):
            cls.index.offers.append(Offer(title="триммеры", ts=23 + i, price=i * 50, hid=41))
        # 39 - 46
        for i in range(1, 9):
            cls.index.offers.append(Offer(title="наушники", ts=38 + i, price=i * 110, hid=60, fesh=41))

        cls.index.mskus += [
            MarketSku(hyperid=1, sku=1, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=2, sku=2, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=3, sku=3, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=4, sku=4, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=5, sku=5, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6, sku=6, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=7, sku=7, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=8, sku=8, title='канделябры', blue_offers=[BlueOffer()]),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(1.0)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.95)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.85)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.75)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.65)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.45)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 14).respond(0.35)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 15).respond(0.3)

    @classmethod
    def prepare_error(cls):
        cls.saas_ugc.needs_default = False

    def test_error(self):
        """
        Проверяем, что, если статьи не нашлись, репорт нормально отработает
        """

        self.error_log.expect(code=3799).once()

        response = self.report.request_json(
            "place=prime&text=большие люстры&numdoc=8&additional_entities=articles&"
            "rearr-factors=market_ugc_saas_enabled=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
        )

    def test_entrypoints_from_saas(self):
        """
        Проверяем, что без обратного флага market_ugc_saas_enabled=0 в выдачу
        замешиваются статьи, полученные из saas. Проверяем т.ж. флаги
        market_ugc_saas_position -- номер документа в выдаче, и
        market_ugc_saas_relevance_threshold -- порог релевантности для статей
        """

        request = "place=prime&text=люстры&numdoc=8&viewtype=grid&additional_entities=articles"
        request_no_grid = "place=prime&text=люстры&numdoc=8&additional_entities=articles"
        request_numdoc_4 = "place=prime&text=люстры&numdoc=4&additional_entities=articles"
        request_numdoc_15 = "place=prime&text=люстры&numdoc=15&additional_entities=articles"
        request_desktop = "place=prime&text=люстры&numdoc=8&platform=desktop&additional_entities=articles"
        flag_off = "&rearr-factors=market_ugc_saas_enabled=0"
        flag_pos = "&rearr-factors=market_ugc_saas_position=%d;market_ugc_saas_enabled=1"
        flag_thr = "&rearr-factors=market_ugc_saas_relevance_threshold=%.2f;market_ugc_saas_enabled==1"

        # 1. С обратным флагом
        response = self.report.request_json(request + flag_off)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 2. С флагом
        # На гридовой выдаче последний элемент НЕ вытесняется
        response = self.report.request_json(request + flag_pos % 4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "materialEntrypoints"},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # И сами статьи
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "relevance": Round(0.99, 2),
                        "compiledGeneration": "20200604_0600",
                        "pagesGeneration": "20200608_0800",
                        "id": 56122,
                        "title": "Как сделать эко-люстру",
                        "semanticId": "kak-sdelat-ehko-lyustru",
                        "type": "knowledge",
                        "subtitle": Absent(),
                        "description": Absent(),
                        "author": Absent(),
                        "image": loads(Images.IMAGE),
                    },
                    {
                        "entity": "materialEntrypoint",
                        "relevance": Round(0.89, 2),
                        "id": 56124,
                        "title": "Как выбрать люстру",
                        "semanticId": "kak-vybrat-lustru",
                        "type": "expertise",
                        "subtitle": "Как же её выбрать",
                        "description": Absent(),
                        "image": loads(Images.IMAGE),
                        "author": {
                            "name": "Федюков Иван Станиславович",
                            "description": "С люстрами на ты",
                            "avatar": loads(Images.AVATAR),
                        },
                    },
                    {
                        "entity": "materialEntrypoint",
                        "relevance": Round(0.88, 2),
                        "id": 56125,
                        "title": "7 правил хорошего освещения",
                        "semanticId": "7-pravil-horoshego-osveshhenija",
                        "type": "blog",
                        "subtitle": Absent(),
                        "description": "Семь, то есть три плюс четыре",
                        "image": loads(Images.IMAGE),
                        "author": {
                            "uid": 1234567,
                            "company": {
                                "url": "instagram.com/rules",
                            },
                        },
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # На негридовой выдаче последний элемент тоже НЕ вытесняется
        response = self.report.request_json(request_no_grid + flag_pos % 4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "materialEntrypoints"},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 3. С другими настройками позиции
        response = self.report.request_json(request + (flag_pos % 1))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "materialEntrypoints"},
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(request + (flag_pos % 5))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "materialEntrypoints"},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Не влез в numdoc
        response = self.report.request_json(request + (flag_pos % 9))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 4. При разных порогах

        # Все статьи не прошли порог, в выдаче их нет
        response = self.report.request_json(request + (flag_thr % 0.99) + flag_pos % 4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Не все прошли порог
        response = self.report.request_json(request + (flag_thr % 0.89) + flag_pos % 4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "materialEntrypoints"},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "semanticId": "kak-sdelat-ehko-lyustru",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "semanticId": "kak-vybrat-lustru",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 5. Статьи не должны замешиваться

        # Если страница не первая
        response = self.report.request_json(
            request_numdoc_4 + '&page=2&rearr-factors=market_ugc_saas_enabled=1' + flag_pos % 4
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # На синем
        response = self.report.request_json("place=prime&text=канделябры&rgb=blue&numdoc=8" + flag_pos % 4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product"},
                    {"entity": "product"},
                    {"entity": "product"},
                    {"entity": "product"},
                    {"entity": "product"},
                    {"entity": "product"},
                    {"entity": "product"},
                    {"entity": "product"},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # На бестексте
        response = self.report.request_json("place=prime&hid=1&numdoc=8" + flag_pos % 4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # На контентном АПИ
        response = self.report.request_json(request + '&api=content' + flag_pos % 4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 6. Статьи замешиваются на сортировках

        response = self.report.request_json(
            request + '&how=aprice&rearr-factors=market_ugc_saas_enabled=1' + flag_pos % 4
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 15"}},
                    {"entity": "offer", "titles": {"raw": "люстры 14"}},
                    {"entity": "offer", "titles": {"raw": "люстры 13"}},
                    {"entity": "materialEntrypoints"},
                    {"entity": "offer", "titles": {"raw": "люстры 12"}},
                    {"entity": "offer", "titles": {"raw": "люстры 11"}},
                    {"entity": "offer", "titles": {"raw": "люстры 10"}},
                    {"entity": "offer", "titles": {"raw": "люстры 9"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 7. Дефолтная позиция на десктопе 7, на таче 13
        response = self.report.request_json(request_numdoc_15 + "&rearr-factors=market_ugc_saas_enabled=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "materialEntrypoints"},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                    {"entity": "offer", "titles": {"raw": "люстры 9"}},
                    {"entity": "offer", "titles": {"raw": "люстры 10"}},
                    {"entity": "offer", "titles": {"raw": "люстры 11"}},
                    {"entity": "offer", "titles": {"raw": "люстры 12"}},
                    {"entity": "offer", "titles": {"raw": "люстры 13"}},
                    {"entity": "offer", "titles": {"raw": "люстры 14"}},
                    {"entity": "offer", "titles": {"raw": "люстры 15"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(request_numdoc_15 + '&touch=1&rearr-factors=market_ugc_saas_enabled=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                    {"entity": "offer", "titles": {"raw": "люстры 9"}},
                    {"entity": "offer", "titles": {"raw": "люстры 10"}},
                    {"entity": "offer", "titles": {"raw": "люстры 11"}},
                    {"entity": "offer", "titles": {"raw": "люстры 12"}},
                    {"entity": "materialEntrypoints"},
                    {"entity": "offer", "titles": {"raw": "люстры 13"}},
                    {"entity": "offer", "titles": {"raw": "люстры 14"}},
                    {"entity": "offer", "titles": {"raw": "люстры 15"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 8. С флагом десктопа на прайме нет статей (они в блендере)
        response = self.report.request_json(request_desktop + "&rearr-factors=market_ugc_saas_enabled=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "люстры 1"}},
                    {"entity": "offer", "titles": {"raw": "люстры 2"}},
                    {"entity": "offer", "titles": {"raw": "люстры 3"}},
                    {"entity": "offer", "titles": {"raw": "люстры 4"}},
                    {"entity": "offer", "titles": {"raw": "люстры 5"}},
                    {"entity": "offer", "titles": {"raw": "люстры 6"}},
                    {"entity": "offer", "titles": {"raw": "люстры 7"}},
                    {"entity": "offer", "titles": {"raw": "люстры 8"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_adult_material_filter(self):
        # проверяем, что взрослые статьи не отображаются в запросах без хида
        request = "place=prime&text=вибраторы&numdoc=8&viewtype=grid&additional_entities=articles"
        filter_off = "&rearr-factors=market_filter_material_entrypoints_adult=0;market_ugc_saas_enabled=1"
        filter_on = "&rearr-factors=market_filter_material_entrypoints_adult=1;market_ugc_saas_enabled=1"

        response = self.report.request_json(request + filter_off)
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "7 правил хорошего времяпрепровождения",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(request + filter_on)
        self.assertFragmentNotIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "7 правил хорошего времяпрепровождения",
                    },
                ],
            },
            preserve_order=True,
        )

    def test_hid_material_filter(self):
        request = "place=prime&hid=41&text=триммеры&numdoc=8&viewtype=grid&additional_entities=articles"
        filter_off = "&rearr-factors=market_filter_material_entrypoints_by_hid=0;market_ugc_saas_enabled=1"
        filter_on = "&rearr-factors=market_filter_material_entrypoints_by_hid=1;market_ugc_saas_enabled=1"

        response = self.report.request_json(request + filter_off)
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "Обзор триммеров для дачного участка",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "Выбор триммера для разных типов бород",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "История триммеров",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(request + filter_on)
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "Обзор триммеров для дачного участка",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_blogs_last_in_incut(self):
        # проверяем, что статьи типа journal/blog попадают в конец врезки, сохраняя при этом относительный порядок
        request = "place=prime&text=наушники&numdoc=8&viewtype=grid&additional_entities=articles&rearr-factors=market_ugc_saas_enabled=1"

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "Топ наушников этого года",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "История наушников",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "Обзор моих любимых наушников",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "Развёрнутый отзыв на наушники",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_ugc_relevance_threshold_with_hid(self):
        # проверяем market_ugc_saas_relevance_threshold_with_hid
        request = (
            "place=prime&text=люстры&numdoc=8&viewtype=grid&additional_entities=articles"
            "&rearr-factors=market_ugc_saas_relevance_threshold=0.90"
            "&rearr-factors=market_ugc_saas_relevance_threshold_with_hid=0.80"
            "&rearr-factors=market_ugc_saas_enabled=1"
        )
        add_hid = "&hid=1"

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "Как сделать эко-люстру",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(request + add_hid)
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "Как сделать эко-люстру",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "Как выбрать люстру",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "7 правил хорошего освещения",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_materials_entrypoints_max_count(self):
        # проверяем market_material_entrypoints_max_count
        request = "place=prime&text=люстры&numdoc=8&viewtype=grid&additional_entities=articles&rearr-factors=market_ugc_saas_enabled=1"
        limit_on = "&rearr-factors=market_material_entrypoints_max_count=1"

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "Как сделать эко-люстру",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "Как выбрать люстру",
                    },
                    {
                        "entity": "materialEntrypoint",
                        "title": "7 правил хорошего освещения",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(request + limit_on)
        self.assertFragmentIn(
            response,
            {
                "entity": "materialEntrypoints",
                "items": [
                    {
                        "entity": "materialEntrypoint",
                        "title": "Как сделать эко-люстру",
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()

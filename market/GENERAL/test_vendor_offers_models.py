#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClickType, HyperCategory, HyperCategoryType, MnPlace, Model, ModelGroup, Offer, Region, Shop
from core.testcase import TestCase, main
from core.matcher import NotEmpty, NoKey

from core.report import DefaultFlags


class T(TestCase):
    @classmethod
    def prepare(cls):
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        '''Для вендора 101 создаем 4 модели с различными GURU_POPULARITY,
        привязываем к ним разное количество оферов.
        Категории разных типов.
        '''
        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2, output_type=HyperCategoryType.CLUSTERS),
            HyperCategory(hid=3, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=4, output_type=HyperCategoryType.SIMPLE),
        ]

        cls.index.models += [
            Model(
                hyperid=301,
                hid=1,
                title='301',
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                picinfo='//mdata.yandex.net/i?path=iphone.jpg',
                model_clicks=1000,  # guru_popularity=10
            ),
            Model(
                hyperid=302,
                hid=2,
                title='302',
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                model_clicks=2000,  # guru_popularity=40
                new=True,
            ),
            Model(
                hyperid=303,
                hid=3,
                title='303',
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                model_clicks=750,  # guru_popularity=30
            ),
            Model(
                hyperid=304,
                hid=4,
                title='304',
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                model_clicks=400,  # guru_popularity=20
                new=True,
            ),
        ]

        cls.index.offers += [
            Offer(
                title='301_1',
                hid=1,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=301,
                randx=111,
            ),  # Total 2 docs (1 offer)
            Offer(
                title='302_1',
                hid=2,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=302,
                randx=110,
            ),
            Offer(
                title='302_2',
                hid=2,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=302,
                randx=109,
            ),  # Total 3 docs (2 offers)
            Offer(
                title='303_1',
                hid=3,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=303,
                randx=108,
            ),
            Offer(
                title='303_2',
                hid=3,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=303,
                randx=107,
            ),
            Offer(
                title='303_3',
                hid=3,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=303,
                randx=106,
            ),
            Offer(
                title='303_4',
                hid=3,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=303,
                randx=105,
            ),  # Total 5 docs (4 offers)
            Offer(
                title='304_1',
                hid=4,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=304,
                randx=104,
            ),
            Offer(
                title='304_2',
                hid=4,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=304,
                randx=103,
            ),
            Offer(
                title='304_3',
                hid=4,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=304,
                randx=102,
            ),
            Offer(
                title='304_4',
                hid=4,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=304,
                randx=101,
            ),
            Offer(
                title='304_5',
                hid=4,
                vendor_id=101,
                licensor=101,
                hero_global=101,
                pers_model=101,
                hyperid=304,
                randx=100,
            ),  # Total 6 docs (5 offers)
        ]

        cls.index.offers += [Offer(hid=5)]

        cls.index.regiontree += [
            Region(rid=1201, name='Москва'),
            Region(rid=1202, name='Заполярье'),
            Region(rid=1203, name='A galaxy far far away'),
        ]
        cls.index.shops += [
            Shop(fesh=1301, priority_region=1201, regions=[1201, 1202]),
            Shop(fesh=1302, priority_region=1202, regions=[1202]),
        ]

        cls.index.models += [
            Model(hyperid=1301, hid=1001, vendor_id=1101, licensor=1101, hero_global=1101, pers_model=1101),
            Model(hyperid=1302, hid=1001, vendor_id=1101, licensor=1101, hero_global=1101, pers_model=1101),
        ]
        cls.index.offers += [
            Offer(hid=1001, vendor_id=1101, licensor=1101, hero_global=1101, pers_model=1101, fesh=1301, hyperid=1301),
            Offer(hid=1001, vendor_id=1101, licensor=1101, hero_global=1101, pers_model=1101, fesh=1302, hyperid=1302),
        ]

    def _test_order(self, filter):
        '''Проверяем, что на выдаче документы через один достаются из:
          1. топа без привязки к категории (смотрим на GURU_POPULARITY и RANDX)
              (302, 303, 304, 301, оферы по убыванию randx)
          2. топа по категориям (сортируются по типу: GURU и CLUSTERS выше,
            затем по количеству оферов в них),
            причем достается сначала по первому из каждой, потом по второму... (в ширину)
              (304, 303, 302, 301, 304_1, 303_1, 302_1, 301_1, 304_2...)

              (302, 301, 304, 303, 302_1, 301_1, 304_1, 303_1, 302_2...)
        Убираются дубликаты (если документ уже был берется следующий в очереди).
        '''
        response = self.report.request_json('place=vendor_offers_models&numdoc=100&' + filter)
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': '302'}},
                {'titles': {'raw': '301'}},
                {'titles': {'raw': '303'}},
                {'titles': {'raw': '304'}},
                {'titles': {'raw': '301_1'}},
                {'titles': {'raw': '302_1'}},
                {'titles': {'raw': '302_2'}},
                {'titles': {'raw': '304_1'}},
                {'titles': {'raw': '303_1'}},
                {'titles': {'raw': '304_2'}},
                {'titles': {'raw': '303_2'}},
                {'titles': {'raw': '304_3'}},
                {'titles': {'raw': '303_3'}},
                {'titles': {'raw': '304_4'}},
                {'titles': {'raw': '303_4'}},
                {'titles': {'raw': '304_5'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_order(self):
        self._test_order('vendor_id=101')
        self._test_order('licensor=101')
        self._test_order('franchise=101')
        self._test_order('character=101')

    def _test_paging(self, filter):
        response = self.report.request_json('place=vendor_offers_models&numdoc=6&' + filter)
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': '302'}},
                {'titles': {'raw': '301'}},
                {'titles': {'raw': '303'}},
                {'titles': {'raw': '304'}},
                {'titles': {'raw': '301_1'}},
                {'titles': {'raw': '302_1'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.report.request_json('place=vendor_offers_models&numdoc=6&page=2&' + filter)
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': '302_2'}},
                {'titles': {'raw': '304_1'}},
                {'titles': {'raw': '303_1'}},
                {'titles': {'raw': '304_2'}},
                {'titles': {'raw': '303_2'}},
                {'titles': {'raw': '304_3'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.report.request_json('place=vendor_offers_models&numdoc=6&page=3&' + filter)
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': '303_3'}},
                {'titles': {'raw': '304_4'}},
                {'titles': {'raw': '303_4'}},
                {'titles': {'raw': '304_5'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_paging(self):
        self._test_paging('vendor_id=101')
        self._test_paging('licensor=101')
        self._test_paging('franchise=101')
        self._test_paging('character=101')

    def test_new_products(self):
        '''Проверяем, что при задании параметра show-only-new-products в выдаче плейса
        остаются только модели с признаком новинки (is_new)
        '''
        response = self.report.request_json(
            'place=vendor_offers_models&numdoc=100&vendor_id=101&show-only-new-products=1'
        )
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': '302'}},
                {'titles': {'raw': '304'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def _test_urls(self, filter):
        self.report.request_json('place=vendor_offers_models&numdoc=6&show-urls=external&' + filter)
        self.click_log.expect(ClickType.EXTERNAL, hyper_cat_id=1)
        self.click_log.expect(ClickType.EXTERNAL, hyper_cat_id=2)

    def test_urls_vendor(self):
        self._test_urls('vendor_id=101')

    def test_urls_licensor(self):
        self._test_urls('licensor=101')

    def test_urls_franchise(self):
        self._test_urls('franchise=101')

    def test_urls_character(self):
        self._test_urls('character=101')

    def _test_regions(self, filter):
        response = self.report.request_json('place=vendor_offers_models&rids=1201&' + filter)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 1301,
                },
                {
                    'entity': 'offer',
                    'model': {'id': 1301},
                },
            ],
            allow_different_len=False,
        )

        response = self.report.request_json('place=vendor_offers_models&rids=1202&' + filter)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 1301,
                },
                {
                    'entity': 'offer',
                    'model': {'id': 1301},
                },
                {
                    'entity': 'product',
                    'id': 1302,
                },
                {
                    'entity': 'offer',
                    'model': {'id': 1302},
                },
            ],
            allow_different_len=False,
        )

        response = self.report.request_json('place=vendor_offers_models&rids=1203&' + filter)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 1301,
                },
                {
                    'entity': 'product',
                    'id': 1302,
                },
            ],
            allow_different_len=False,
        )

    def test_regions(self):
        self._test_regions('vendor_id=1101')
        self._test_regions('licensor=1101')
        self._test_regions('franchise=1101')
        self._test_regions('character=1101')

    def test_missing_pp(self):
        response = self.report.request_json(
            'place=vendor_offers_models&vendor_id=101&numdoc=100&ip=127.0.0.1',
            strict=False,
            add_defaults=DefaultFlags.BS_FORMAT,
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    @classmethod
    def prepare_no_pictures_pessimization_data(cls):
        '''Создаем по оферу с картинкой и без в одной категории,
        офер без картинки в другой.
        '''
        cls.index.offers += [
            Offer(hid=10, vendor_id=102, licensor=102, hero_global=102, pers_model=102, no_picture=False, ts=1),
            Offer(hid=10, vendor_id=102, licensor=102, hero_global=102, pers_model=102, no_picture=True, ts=2),
            Offer(hid=11, vendor_id=103, licensor=103, hero_global=103, pers_model=103, no_picture=True),
        ]

    def _test_no_pictures_pessimization(self, filter1, filter2):
        '''Проверяем в дебаг-выдаче, что:
        - у документов стоят правильные флаги HAS_PICTURES
        - верный порядок полей релевантности (HAS_PICTURES первое)
        - на выдаче с vendor_id=102 сначала офер с картинкой, потом без
        - на выдаче с vendor_id=103 только офер без картинки, т.к. с картинкой нет
        '''
        response = self.report.request_json('place=vendor_offers_models&numdoc=10&debug=da&' + filter1)
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "1",
                    "HAS_PICTURE": "1",
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "2",
                    "HAS_PICTURE": "0",
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "HAS_PICTURE"},
                    {"name": "DELIVERY_TYPE"},
                    {"name": "IS_MODEL"},
                    {"name": "IS_NOT_MODIFICATION"},
                    {"name": "GURU_POPULARITY"},
                    {"name": "ONSTOCK"},
                    {"name": "RANDX"},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"pictures": NotEmpty()},
                    {"pictures": NoKey("pictures")},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.report.request_json('place=vendor_offers_models&numdoc=10&' + filter2)
        self.assertFragmentIn(response, {"results": [{"pictures": NoKey("pictures")}]})

    def test_no_pictures_pessimization(self):
        self._test_no_pictures_pessimization('vendor_id=102', 'vendor_id=103')
        self._test_no_pictures_pessimization('licensor=102', 'licensor=103')
        self._test_no_pictures_pessimization('franchise=102', 'franchise=103')
        self._test_no_pictures_pessimization('character=102', 'character=103')

    @classmethod
    def prepare_limits(cls):
        '''Создаем более 120 оферов в одной категории с CPM (GURU_POPULARITY) выше,
        чем у более 5 оферов в другой.
        '''
        for i in range(5 + 1):
            ts = 1000 + i
            cls.index.offers.append(Offer(hid=13, vendor_id=104, licensor=104, hero_global=104, pers_model=104, ts=ts))
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.0001 * ts)
        for i in range(120 + 1):
            ts = 1100 + i
            cls.index.offers.append(Offer(hid=12, vendor_id=104, licensor=104, hero_global=104, pers_model=104, ts=ts))
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.0001 * ts)

    def _test_limits(self, filter):
        '''Запрашиваем 1000 документов, проверяем, что на выдаче
        есть 5 документов из категории 13 (не попадают в 120 из топ-документов),
        остальные 120 - из 12.
        '''
        response = self.report.request_json('place=vendor_offers_models&numdoc=1000&' + filter)
        self.assertFragmentIn(
            response,
            {
                "results": [{"categories": [{"id": 13}]} for _ in range(5)]
                + [{"categories": [{"id": 12}]} for _ in range(120)]
            },
            preserve_order=False,
            allow_different_len=False,
        )

    def test_limits(self):
        self._test_limits('vendor_id=104')
        self._test_limits('licensor=104')
        self._test_limits('franchise=104')
        self._test_limits('character=104')

    @classmethod
    def prepare_max_entities(cls):
        '''Создаем более 120 категорий с более 5 оферов в каждой,
        создаем более 120 оферов в одной категории.
        '''
        cls.index.offers += [
            Offer(hid=h, vendor_id=105, licensor=105, hero_global=105, pers_model=105)
            for h in range(101, 101 + 120 + 1)
            for _ in range(5 + 1)
        ]
        cls.index.offers += [
            Offer(hid=100, vendor_id=105, licensor=105, hero_global=105, pers_model=105) for _ in range(120 + 1)
        ]

    def _test_max_entities(self, filter):
        '''Запрашиваем 1000 документов, проверяем, что на выдаче
        715 документов (5 общих в обеих очередях)
        '''
        response = self.report.request_json('place=vendor_offers_models&numdoc=1000&' + filter)
        self.assertFragmentIn(
            response,
            {
                "results": [NotEmpty() for _ in range(715)],
            },
            allow_different_len=False,
        )

    def test_max_entities(self):
        self._test_max_entities('vendor_id=105')
        self._test_max_entities('licensor=105')
        self._test_max_entities('franchise=105')
        self._test_max_entities('character=105')

    @classmethod
    def prepare_use_constant_bid_for_cpm_data(cls):
        '''Для вендора создаем оферы с различными bid, ctr и randx.'''
        cls.index.offers += [
            Offer(
                title='106_1',
                hid=6,
                vendor_id=106,
                licensor=106,
                hero_global=106,
                pers_model=106,
                bid=100,
                randx=100,
                ts=3,
            ),
            Offer(
                title='106_2',
                hid=6,
                vendor_id=106,
                licensor=106,
                hero_global=106,
                pers_model=106,
                bid=200,
                randx=900,
                ts=4,
            ),
            Offer(
                title='106_3',
                hid=6,
                vendor_id=106,
                licensor=106,
                hero_global=106,
                pers_model=106,
                bid=300,
                randx=800,
                ts=5,
            ),
            Offer(
                title='106_4',
                hid=6,
                vendor_id=106,
                licensor=106,
                hero_global=106,
                pers_model=106,
                bid=400,
                randx=700,
                ts=6,
            ),
            Offer(
                title='106_5',
                hid=6,
                vendor_id=106,
                licensor=106,
                hero_global=106,
                pers_model=106,
                bid=500,
                randx=600,
                ts=7,
            ),
            Offer(
                title='106_6',
                hid=6,
                vendor_id=106,
                licensor=106,
                hero_global=106,
                pers_model=106,
                bid=600,
                randx=500,
                ts=8,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.7)

    def _test_use_constant_bid_for_cpm(self, filter):
        '''Проверяем, что оферы отранжированы по (-CTR, -RANDX)'''
        response = self.report.request_json('place=vendor_offers_models&numdoc=10&' + filter)
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': '106_5'}},
                {'titles': {'raw': '106_6'}},
                {'titles': {'raw': '106_2'}},
                {'titles': {'raw': '106_1'}},
                {'titles': {'raw': '106_3'}},
                {'titles': {'raw': '106_4'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_use_constant_bid_for_cpm(self):
        self._test_use_constant_bid_for_cpm('vendor_id=106')
        self._test_use_constant_bid_for_cpm('licensor=106')
        self._test_use_constant_bid_for_cpm('franchise=106')
        self._test_use_constant_bid_for_cpm('character=106')

    @classmethod
    def prepare_model_modifications_pessimization_data(cls):
        '''Создаем групповую модель и ее модификации,
        две обычные модели.
        Привязываем по одному оферу для гуру-популярности.
        '''
        cls.index.model_groups += [
            ModelGroup(hid=14, hyperid=305, vendor_id=107),
        ]
        cls.index.models += [
            # create group model directly (ModelGroup doesn't create it)
            Model(
                hid=14, hyperid=305, vendor_id=107, licensor=107, hero_global=107, pers_model=107
            ),  # guru_popularity=40 (10 + 30)
            Model(
                hid=14,
                hyperid=306,
                group_hyperid=305,
                vendor_id=107,
                licensor=107,
                hero_global=107,
                pers_model=107,
                model_clicks=1000,
            ),  # guru_popularity=10
            Model(
                hid=14,
                hyperid=307,
                group_hyperid=305,
                vendor_id=107,
                licensor=107,
                hero_global=107,
                pers_model=107,
                model_clicks=3000,
            ),  # guru_popularity=30
            Model(
                hid=14, hyperid=308, vendor_id=107, licensor=107, hero_global=107, pers_model=107, model_clicks=5000
            ),  # guru_popularity=50
            Model(
                hid=14, hyperid=309, vendor_id=107, licensor=107, hero_global=107, pers_model=107, model_clicks=2000
            ),  # guru_popularity=20
        ]
        cls.index.offers += [
            Offer(
                title='306_1',
                hid=14,
                vendor_id=107,
                licensor=107,
                hero_global=107,
                pers_model=107,
                hyperid=306,
                randx=1,
            ),
            Offer(
                title='307_1',
                hid=14,
                vendor_id=107,
                licensor=107,
                hero_global=107,
                pers_model=107,
                hyperid=307,
                randx=2,
            ),
            Offer(
                title='308_1',
                hid=14,
                vendor_id=107,
                licensor=107,
                hero_global=107,
                pers_model=107,
                hyperid=308,
                randx=3,
            ),
            Offer(
                title='309_1',
                hid=14,
                vendor_id=107,
                licensor=107,
                hero_global=107,
                pers_model=107,
                hyperid=309,
                randx=4,
            ),
        ]

    def _test_model_modifications_pessimization(self, filter):
        '''Проверяем, что на выдаче сначала идут
        упорядоченые по гуру-популярности:
         - модели, которые не являются модификациями
         - затем модификации
        После моделей оферы, упорядоченные по randx
        '''
        response = self.report.request_json('place=vendor_offers_models&numdoc=10&' + filter)
        self.assertFragmentIn(
            response,
            [
                {"id": 308},
                {"id": 305},
                {"id": 309},
                {"id": 307},
                {"id": 306},
                {'titles': {'raw': '309_1'}},
                {'titles': {'raw': '308_1'}},
                {'titles': {'raw': '307_1'}},
                {'titles': {'raw': '306_1'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_model_modifications_pessimization(self):
        self._test_model_modifications_pessimization('vendor_id=107')
        self._test_model_modifications_pessimization('licensor=107')
        self._test_model_modifications_pessimization('franchise=107')
        self._test_model_modifications_pessimization('character=107')


if __name__ == '__main__':
    main()

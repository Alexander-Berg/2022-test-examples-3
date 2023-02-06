#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Offer,
    Region,
    Shop,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        """Конфигурация индекса:
        -1 магазин
        -12 офферов
        -4 категории
        """

        cls.index.gltypes = [
            GLType(param_id=201, hid=101, gltype=GLType.BOOL, cluster_filter=True),
        ]

        cls.index.hypertree += [HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in range(101, 105)]

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург'),
            Region(rid=64, name='Екатеринбург'),
        ]

        cls.index.shops += [
            Shop(fesh=100000, regions=[213], cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                cpa=Offer.CPA_REAL,
                title='offer1',
                fesh=100000,
                hid=101,
                randx=300,
                glparams=[GLParam(param_id=201, value=1)],
            ),
            Offer(cpa=Offer.CPA_REAL, title='offer2', fesh=100000, hid=101, randx=200),
            Offer(cpa=Offer.CPA_REAL, title='offer3', fesh=100000, hid=101, randx=100),
            Offer(cpa=Offer.CPA_REAL, title='offer4', fesh=100000, hid=102, randx=300),
            Offer(cpa=Offer.CPA_REAL, title='offer5', fesh=100000, hid=102, randx=200),
            Offer(cpa=Offer.CPA_REAL, title='offer6', fesh=100000, hid=102, randx=100),
            Offer(cpa=Offer.CPA_REAL, title='offer7', fesh=100000, hid=103, randx=300),
            Offer(cpa=Offer.CPA_REAL, title='offer8', fesh=100000, hid=103, randx=200),
            Offer(cpa=Offer.CPA_REAL, title='offer9', fesh=100000, hid=103, randx=100),
            Offer(cpa=Offer.CPA_REAL, title='offer10', fesh=100000, hid=104, randx=300),
            Offer(cpa=Offer.CPA_REAL, title='offer11', fesh=100000, hid=104, randx=200),
            Offer(cpa=Offer.CPA_REAL, title='offer12', fesh=100000, hid=104, randx=100),
        ]

        cls.index.models += [
            Model(hyperid=10000 + hid, hid=hid) for hid in list(range(101, 105)) + list(range(220, 232))
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:', with_timestamps=False).respond(
            # порядок офферов: 1,4,7,10,2,5,8,11,3,6,9,12
            {'models': map(str, list(range(10101, 10105)))}
        )
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:001', with_timestamps=False).respond(
            # порядок офферов: 7,4,1,8,5,2,9,6,3
            {'models': ['10103', '10102', '10101']}
        )
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:002', with_timestamps=False).respond(
            {
                'models': [
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
                name=YamarecPlace.Name.CATEGORY_DISCOUNT,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        formula_id=152888,
                        splits=['*'],
                        feature_keys=['category_id'],
                        feature_names=['category_id', 'position'],
                        features=[],
                    )
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[],
                        splits=['*'],
                    )
                ],
            ),
        ]

    def test_second_type_filters(self):
        """
        Тестируем наличие параметров офера в выдаче
        """
        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000')
        self.assertFragmentIn(
            response,
            {"entity": "offer", "filters": [{"id": "201", "kind": 2, "values": [{"initialFound": 1, "id": "1"}]}]},
            preserve_order=True,
        )

    def test_pager(self):
        """Тестируем пейджер:
        -запрос без параметров: нашли 12 офферов, вернули 10(по умолчанию)
        -запросили 15 документов: нашли 12, вернули 12
        -запросили 5 документов: нашли 12, вернули 5
        -запросили 1 документ: нашли 12 вернули 1
        -запросили 3 документа без страницы: нашли 12, вернули первые 3 оффера (1-3)
        -запросили 3 документа на 1 странице: нашли 12, вернули первые 3 оффера (1-3)
        -запросили 3 документа на 2 странице: нашли 12, вернули вторые 3 оффера (4-6)
        -запросили 3 документа на 2 странице с пропуском 1 документа: нашли 12, вернули 3 оффера (5-7)
        """
        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(response, [{"entity": "offer"}] * 10, preserve_order=True, allow_different_len=False)

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=15')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(response, [{"entity": "offer"}] * 12, preserve_order=True, allow_different_len=False)

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=5')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(response, [{"entity": "offer"}] * 5, preserve_order=True, allow_different_len=False)

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=1')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(response, [{"entity": "offer"}] * 1, preserve_order=True, allow_different_len=False)

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=3')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer1"}},
                {"titles": {"raw": "offer4"}},
                {"titles": {"raw": "offer7"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=3&page=1')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer1"}},
                {"titles": {"raw": "offer4"}},
                {"titles": {"raw": "offer7"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=3&page=2')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer10"}},
                {"titles": {"raw": "offer2"}},
                {"titles": {"raw": "offer5"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=3&page=2&skip=1')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer2"}},
                {"titles": {"raw": "offer5"}},
                {"titles": {"raw": "offer8"}},
            ],
            preserve_order=True,
        )

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='12').times(8)

    def test_personalization(self):
        """Тестируем персонализацию
        -делаем запрос без yandexuid: получаем порядок категорий из дефолта: 1,4,7,10,2,5,8,11,3,6,9,12
        -делаем запрос для yandexuid=001: получаем порядок категорий для пользователя 001: 7,4,1,8,5,2,9,6,3
        """
        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=15')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer1"}},
                {"titles": {"raw": "offer4"}},
                {"titles": {"raw": "offer7"}},
                {"titles": {"raw": "offer10"}},
                {"titles": {"raw": "offer2"}},
                {"titles": {"raw": "offer5"}},
                {"titles": {"raw": "offer8"}},
                {"titles": {"raw": "offer11"}},
                {"titles": {"raw": "offer3"}},
                {"titles": {"raw": "offer6"}},
                {"titles": {"raw": "offer9"}},
                {"titles": {"raw": "offer12"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&yandexuid=001')
        self.assertFragmentIn(response, {"total": 9}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer7"}},
                {"titles": {"raw": "offer4"}},
                {"titles": {"raw": "offer1"}},
                {"titles": {"raw": "offer8"}},
                {"titles": {"raw": "offer5"}},
                {"titles": {"raw": "offer2"}},
                {"titles": {"raw": "offer9"}},
                {"titles": {"raw": "offer6"}},
                {"titles": {"raw": "offer3"}},
            ],
            preserve_order=True,
        )

    def test_hid(self):
        """Проверяем фильтрацию по hid:
        -делаем запрос без hid: получаем все 12 офферов
        -делаем запрос с hid=101: получаем 3 оффера из категории 101
        -делаем запрос с hid=101,102: получаем 6 офферов из категории 101 и 102
        -делаем запрос с hid=-101: получаем 9 офферов из категорий 102, 103, 104
        -делаем запрос с hid=-101,-102: получаем 6 офферов из категорий 103, 104
        """
        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=15')
        self.assertFragmentIn(response, {"total": 12}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer1"}},
                {"titles": {"raw": "offer4"}},
                {"titles": {"raw": "offer7"}},
                {"titles": {"raw": "offer10"}},
                {"titles": {"raw": "offer2"}},
                {"titles": {"raw": "offer5"}},
                {"titles": {"raw": "offer8"}},
                {"titles": {"raw": "offer11"}},
                {"titles": {"raw": "offer3"}},
                {"titles": {"raw": "offer6"}},
                {"titles": {"raw": "offer9"}},
                {"titles": {"raw": "offer12"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=15&hid=101')
        self.assertFragmentIn(response, {"total": 3}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer1"}},
                {"titles": {"raw": "offer2"}},
                {"titles": {"raw": "offer3"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=15&hid=101,102')
        self.assertFragmentIn(response, {"total": 6}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer1"}},
                {"titles": {"raw": "offer4"}},
                {"titles": {"raw": "offer2"}},
                {"titles": {"raw": "offer5"}},
                {"titles": {"raw": "offer3"}},
                {"titles": {"raw": "offer6"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=15&hid=-101')
        self.assertFragmentIn(response, {"total": 9}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer4"}},
                {"titles": {"raw": "offer7"}},
                {"titles": {"raw": "offer10"}},
                {"titles": {"raw": "offer5"}},
                {"titles": {"raw": "offer8"}},
                {"titles": {"raw": "offer11"}},
                {"titles": {"raw": "offer6"}},
                {"titles": {"raw": "offer9"}},
                {"titles": {"raw": "offer12"}},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&numdoc=15&hid=-101,-102')
        self.assertFragmentIn(response, {"total": 6}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer7"}},
                {"titles": {"raw": "offer10"}},
                {"titles": {"raw": "offer8"}},
                {"titles": {"raw": "offer11"}},
                {"titles": {"raw": "offer9"}},
                {"titles": {"raw": "offer12"}},
            ],
            preserve_order=True,
        )

    def test_sort(self):
        """Проверяем сортировку:
        -сначала ранжируются категории в порядке: 103,102,101
        -берем первый оффер из категории 103: offer7
        -берем первый оффер из категории 102: offer4
        -берем первый оффер из категории 101: offer1
        -берем второй оффер из категории 103: offer8
        -берем второй оффер из категории 102: offer5
        -берем второй оффер из категории 101: offer2
        -берем третий оффер из категории 103: offer9
        -берем третий оффер из категории 102: offer6
        -берем третий оффер из категории 101: offer3
        """
        response = self.report.request_json('place=personal_offers&rids=213&fesh=100000&yandexuid=001')
        self.assertFragmentIn(response, {"total": 9}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "offer7"}},
                {"titles": {"raw": "offer4"}},
                {"titles": {"raw": "offer1"}},
                {"titles": {"raw": "offer8"}},
                {"titles": {"raw": "offer5"}},
                {"titles": {"raw": "offer2"}},
                {"titles": {"raw": "offer9"}},
                {"titles": {"raw": "offer6"}},
                {"titles": {"raw": "offer3"}},
            ],
            preserve_order=True,
        )

    @classmethod
    def prepare_shop_offers(cls):
        """Создаем 2 магазина, категорийное и навигационное деревья,
        по две родительскихкатегории и по 5 дочерих внутри каждой из родительских
        Создаем по три оффера одного из магазинов в этих категориях
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=233,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=220, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=221, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=222, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=223, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=224, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=225, output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(
                hid=232,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=226, output_type=HyperCategoryType.GURU),
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

        for seq in range(1, 4):
            cls.index.offers += [
                Offer(hid=220, fesh=401, title='Offer 220-{}'.format(seq), price_old=200, randx=100 * seq),
                Offer(hid=221, fesh=401, title='Offer 221-{}'.format(seq), randx=100 * seq),
                Offer(hid=222, fesh=401, title='Offer 222-{}'.format(seq), price_old=200, randx=100 * seq),
                Offer(hid=223, fesh=402, title='Offer 223-{}'.format(seq), randx=100 * seq),
                Offer(hid=224, fesh=402, title='Offer 224-{}'.format(seq), randx=100 * seq),
                Offer(hid=225, fesh=402, title='Offer 225-{}'.format(seq), randx=100 * seq),
                Offer(hid=226, fesh=401, title='Offer 226-{}'.format(seq), price_old=200, randx=100 * seq),
                Offer(hid=227, fesh=401, title='Offer 227-{}'.format(seq), price_old=200, randx=100 * seq),
                Offer(hid=228, fesh=401, title='Offer 228-{}'.format(seq), randx=100 * seq),
                Offer(hid=229, fesh=401, title='Offer 229-{}'.format(seq), randx=100 * seq),
                Offer(hid=227, fesh=402, title='Offer 227-2-{}'.format(seq), price_old=200, randx=100 * seq),
                Offer(hid=228, fesh=402, title='Offer 228-2-{}'.format(seq), price_old=200, randx=100 * seq),
                Offer(hid=229, fesh=402, title='Offer 229-2-{}'.format(seq), price_old=200, randx=100 * seq),
                Offer(hid=230, fesh=402, title='Offer 230-{}'.format(seq), price_old=200, randx=100 * seq),
                Offer(hid=231, fesh=402, title='Offer 231-{}'.format(seq), price_old=200, randx=100 * seq),
            ]

    def test_shop_offers(self):
        """Что тестируем: в плейсе personal_offers при фильтрации по магазину
        возвращаются только офферы, представленные в магазине в регионе
        пользователя

        Задаем запросы к плейсу personal_offers для магазина 401
        в регионах 213 и 2
        Проверяем, что на выдаче есть только офферы этого магазина
        в правильном порядке (группами по 5 офферов из наиболее
        релевантных категорий (229-228-227-226-222))

        Задаем запрос для этого магазина в регионе 64, проверяем, что
        выдача пуста
        """
        for rids in [213, 2]:
            # Запрос за тремя офферами
            response = self.report.request_json(
                'place=personal_offers&yandexuid=002&fesh=401&numdoc=3&rids={}'.format(rids)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {"titles": {"raw": "Offer 229-3"}},
                        {"titles": {"raw": "Offer 228-3"}},
                        {"titles": {"raw": "Offer 227-3"}},
                    ]
                },
                allow_different_len=False,
            )

            # Запрос за пятнадцатью офферами
            response = self.report.request_json(
                'place=personal_offers&yandexuid=002&fesh=401&numdoc=15&rids={}'.format(rids)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {"titles": {"raw": "Offer 229-3"}},
                        {"titles": {"raw": "Offer 228-3"}},
                        {"titles": {"raw": "Offer 227-3"}},
                        {"titles": {"raw": "Offer 226-3"}},
                        {"titles": {"raw": "Offer 222-3"}},
                        {"titles": {"raw": "Offer 229-2"}},
                        {"titles": {"raw": "Offer 228-2"}},
                        {"titles": {"raw": "Offer 227-2"}},
                        {"titles": {"raw": "Offer 226-2"}},
                        {"titles": {"raw": "Offer 222-2"}},
                        {"titles": {"raw": "Offer 229-1"}},
                        {"titles": {"raw": "Offer 228-1"}},
                        {"titles": {"raw": "Offer 227-1"}},
                        {"titles": {"raw": "Offer 226-1"}},
                        {"titles": {"raw": "Offer 222-1"}},
                    ]
                },
                allow_different_len=False,
            )

        # Запрос в регионе без офферов магазина
        response = self.report.request_json('place=personal_offers&yandexuid=002&fesh=401&numdoc=3&rids=64')
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

    def test_shop_offers_with_discount(self):
        """Что тестируем: в плейсе personal_offers при фильтрации по магазину и наличию скидки
        возвращаются только офферы со скидками, представленные в магазине в регионе
        пользователя

        Задаем запросы к плейсу personal_offers для магазина 401 с фильтром по скидке
        в регионах 213 и 2
        Проверяем, что на выдаче есть только офферы этого магазина
        в правильном порядке (группами по 4 оффера из наиболее
        релевантных категорий (227-226-222-220))
        """
        for rids in [213, 2]:
            # Запрос за тремя офферами
            response = self.report.request_json(
                'place=personal_offers&filter-discount-only=1&yandexuid=002&fesh=401&numdoc=3&rids={}'.format(rids)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {"titles": {"raw": "Offer 227-3"}},
                        {"titles": {"raw": "Offer 226-3"}},
                        {"titles": {"raw": "Offer 222-3"}},
                    ]
                },
                allow_different_len=False,
            )

            # Запрос за пятнадцатью (есть только 12 со скидкой) офферами
            response = self.report.request_json(
                'place=personal_offers&filter-discount-only=1&yandexuid=002&fesh=401&numdoc=15&rids={}'.format(rids)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {"titles": {"raw": "Offer 227-3"}},
                        {"titles": {"raw": "Offer 226-3"}},
                        {"titles": {"raw": "Offer 222-3"}},
                        {"titles": {"raw": "Offer 220-3"}},
                        {"titles": {"raw": "Offer 227-2"}},
                        {"titles": {"raw": "Offer 226-2"}},
                        {"titles": {"raw": "Offer 222-2"}},
                        {"titles": {"raw": "Offer 220-2"}},
                        {"titles": {"raw": "Offer 227-1"}},
                        {"titles": {"raw": "Offer 226-1"}},
                        {"titles": {"raw": "Offer 222-1"}},
                        {"titles": {"raw": "Offer 220-1"}},
                    ]
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_new_personal_categories(cls):
        cls.index.models += [
            Model(hyperid=1111, hid=111),
        ]
        cls.index.offers += [
            Offer(hid=111, fesh=401, title='super offer', price_old=200),
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:9999', with_timestamps=False).respond(
            {'models': ['1111']}
        )

    def test_new_personal_categories(self):
        response = self.report.request_json(
            'place=personal_offers&yandexuid=9999&fesh=401&rids=213&rearr-factors=market_use_recommender=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'super offer'}},
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.testcase import TestCase, main
from core.types import (
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    Picture,
    Promo,
    PromoType,
    Region,
    Shop,
)
from core.matcher import NotEmpty, Round, NoKey, Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        '''
        Подготовка данных для проверки схлопывания
        '''
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        # Создаем 5 категорий: простую, три гуру и гурулайт
        cls.index.hypertree += [
            HyperCategory(hid=10, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=20),
            HyperCategory(hid=30, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=40, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=50, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=60, output_type=HyperCategoryType.GURU),
        ]

        # Создаем 2 магазина из региона 2, но доставляющие в регон 1
        cls.index.shops += [
            Shop(fesh=51, regions=[1], priority_region=2),
            Shop(fesh=52, regions=[1], priority_region=2),
        ]

        # Создаем 4 модели в гуру категории с разным количеством отзывов и разным рейтингом,
        # одну модель в протой категории, одну в гурлайт категории и еще одну в книжной
        # и дополнительно создеаем одну модель, к которой привяжем офферы из другого региона
        # и модель в гуру-категории для тестирования при сортировке по скидке
        # а также модель в гуру-категории для тестирования отключения схлопывания с фильтром субсидий
        cls.index.models += [
            Model(title='black substance', ts=1011, hid=10, opinion=Opinion(total_count=10, rating=5.0), hyperid=10011),
            Model(title='dark substance', ts=1012, hid=10, opinion=Opinion(total_count=20, rating=4.5), hyperid=10012),
            Model(title='light substance', ts=1013, hid=10, opinion=Opinion(total_count=30, rating=4.0), hyperid=10013),
            Model(title='white substance', ts=1014, hid=10, opinion=Opinion(total_count=40, rating=3.5), hyperid=10014),
            Model(title='black liquid', hid=20, hyperid=21),
            Model(title='white liquid', hid=30, hyperid=31),
            Model(title='book model', hid=90829, hyperid=41),
            Model(title='multiregional model', hyperid=51),
            Model(title='model for discount', hid=40, hyperid=61),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1011).respond(0.34)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1012).respond(0.33)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1013).respond(0.32)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1014).respond(0.31)

        # Создаем 9 офферов в гуру категории - по 2 оффера на модель и один без модели
        # все офферы должны быть привязаны к разным магазинам
        # для первых четырех офферов убираем гарантию производителя
        # и назначаем всем разные цены
        # У всех офферов в названии должно присутсвоввть одно и то же слово

        # Создаем два оффера в простой категории
        # привязываем их к разным магазинам, но к кодной модели (из этой же категории)
        # У офферов в названии должно присутсвоввть одно и то же слово

        # Создаем два оффера гурулайт категории
        # привязываем их к разным магазинам, но к кодной модели (из этой же категории)
        # У офферов в названии должно присутсвоввть одно и то же слово
        # (такое же как и в простой категории)

        # Создаем два оффера в категории книг
        # привязываем их к разным магазинам, но к кодной модели (из этой же категории)
        # у модели и офферов должно быть одно и тоже слово в названии

        # Создаем два оффера, которые привязываем к магазинам из региона 2 и
        # один из них привязываем к специально созданной для этого модели
        # у них в названии должно присутсвоввть одно и то же слово

        # Создаем оффер, привязанный к модели для тестирования при сортировке по скидке
        # Создаем оффер, привязанный к модели для тестирования отключения схлопывания с фильтром субсидий

        cls.index.offers += [
            Offer(
                title='black powder',
                hid=10,
                fesh=11,
                ts=11,
                price=100,
                hyperid=10011,
                manufacturer_warranty=False,
                waremd5='aOgVX7lvufxf5cgCc5gaTA',
            ),
            Offer(title='black powder', hid=10, fesh=12, ts=12, price=200, hyperid=10011, manufacturer_warranty=False),
            Offer(title='dark powder', hid=10, fesh=13, ts=13, price=300, hyperid=10012, manufacturer_warranty=False),
            Offer(title='dark powder', hid=10, fesh=14, ts=14, price=400, hyperid=10012, manufacturer_warranty=False),
            Offer(title='light powder', hid=10, fesh=15, ts=15, price=500, hyperid=10013),
            Offer(title='light powder', hid=10, fesh=16, ts=16, price=600, hyperid=10013),
            Offer(title='white powder', hid=10, fesh=17, ts=17, price=700, hyperid=10014),
            Offer(title='colorless powder', hid=10, fesh=19, price=900, ts=19),
            Offer(title='black water', hid=20, fesh=21, hyperid=21),
            Offer(title='black water', hid=20, fesh=22, hyperid=21),
            Offer(title='white water', hid=30, fesh=31, hyperid=31),
            Offer(title='white water', hid=30, fesh=32, hyperid=31),
            Offer(title='book offer', hid=90829, fesh=41, hyperid=41),
            Offer(title='book offer', hid=90829, fesh=42, hyperid=41),
            Offer(title='from area 1', fesh=51, hyperid=51),
            Offer(title='from area 2', fesh=52),
            Offer(title='offer for discount', hyperid=61, hid=40),
        ]

        # выставляем значение формулы офферам из гуру категории, привязанным к модели меньше,
        # чем офферу из этой же категории, но не привязанному к модели
        for i in range(1, 9):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10 + i).respond(0.2)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.58)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.57)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13).respond(0.56)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 14).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 15).respond(0.54)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 16).respond(0.53)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 17).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 18).respond(0.51)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 19).respond(1.0)

    def test_guru_category_collapsing(self):
        '''
        Проверка схлопывания в гуру категории
        '''

        # проверяем, что по запросу к place prime в категорию 10 с включенным флагом allow-collapsing
        # получим всего 4 документа и все они модели
        response = self.report.request_json('place=prime&hid=10&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {"entity": "product", "titles": {"raw": "black substance"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}},
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_allow_collapsing_anywhere(self):
        """Схлопывание разрешено везде (в том числе и на негуру категориях)
        если оно не запроещено см needToDisableCollapsing в prime_base.cpp"""

        # проверяем, что есть схлапывание по запросу к place prime в категорию 20 (простая)
        response = self.report.request_json(
            'place=prime&hid=20&allow-collapsing=1' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"total": 1, "results": [{"entity": "product", "titles": {"raw": "black liquid"}}]},
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что есть схлапывание по запросу к place prime в категорию 30 (гурулайт)
        response = self.report.request_json(
            'place=prime&hid=30&allow-collapsing=1' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"total": 1, "results": [{"entity": "product", "titles": {"raw": "white liquid"}}]},
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что есть схлапывание по запросу к place prime в категорию 90829 (книги)
        response = self.report.request_json(
            'place=prime&hid=90829&allow-collapsing=1' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"total": 1, "results": [{"entity": "product", "titles": {"raw": "book model"}}]},
            preserve_order=True,
            allow_different_len=False,
        )

        # если схлапываниее явно запрещено флагом allow-collapsing=0 - оно не происходит
        response = self.report.request_json(
            'place=prime&hid=30&allow-collapsing=0' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 3,
                "results": [
                    {"entity": "product", "titles": {"raw": "white liquid"}},
                    {"entity": "offer", "titles": {"raw": "white water"}},
                    {"entity": "offer", "titles": {"raw": "white water"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # если схлапывание запрещено другими причинами (например указан offerid) то оно тоже не происходит
        response = self.report.request_json(
            'place=prime&hid=10&allow-collapsing=1&offerid=aOgVX7lvufxf5cgCc5gaTA'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {"entity": "offer"},
                ],
            },
            allow_different_len=False,
        )

        # если схлапывание запрещено другими причинами (например включена сортировка по скидке) то оно тоже не происходит
        response = self.report.request_json(
            'place=prime&hid=40&allow-collapsing=1&how=discount_p' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"entity": "product", "titles": {"raw": "model for discount"}},
                    {"entity": "offer", "titles": {"raw": "offer for discount"}},
                ],
            },
        )

    def test_tail_collapsing(self):
        '''
        Проверка схлопывания в хвосте выдачи
        '''

        # Делаем запрос к place prime к категории 10 с включенным флагом allow-collapsing
        # страницами по два документа

        # Для построения первой страницы используются первые 4 оффера (head)
        # (группировка dsrcid - ее размер 4: 2 документа на странице умноженное на 2),
        # они схлопываются в 2 первые модели.
        # Проверяем, что в выдаче два документа и это первые две модели
        response = self.report.request_json('place=prime&hid=10&numdoc=2&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "black substance"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Вторая страница формируется уже из четырех офферов из tail, которые схлопываются в оставшиеся две модели.
        # Запрашиваем 2ую страницу и проверяем, что выдаче 2 документа и это последние две модели.
        response = self.report.request_json('place=prime&hid=10&numdoc=2&page=2&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_collapsing_on_text_requets(self):
        '''
        Проверка схлопывания на текстовых запросах
        '''

        # Проверяем, что по запросу к place prime со включенным флагоми allow-collapsing и текстом
        # выдача сформирована по релевантности:
        # оффер из гуру категории не привязанный к модели, а за ним 4 модели
        response = self.report.request_json('place=prime&allow-collapsing=1&text=powder')
        self.assertFragmentIn(
            response,
            {
                "total": 5,
                "results": [
                    {"entity": "offer", "titles": {"raw": "colorless powder"}},
                    {"entity": "product", "titles": {"raw": "black substance"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}},
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что по запросу к place prime со включенным флагоми allow-collapsing в листовой гуру-категории и текстом
        # в выдаче документы идут по релевантности
        # оффер из гуру категории не привязанный к модели, а за ним 4 модели
        response = self.report.request_json('place=prime&allow-collapsing=1&text=powder&hid=10')
        self.assertFragmentIn(
            response,
            {
                "total": 5,
                "results": [
                    {"entity": "offer", "titles": {"raw": "colorless powder"}},
                    {"entity": "product", "titles": {"raw": "black substance"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}},
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что по запросу к place prime со включенным флагоми allow-collapsing и текстом
        # офферы схлапываются и в выдаче остается только одна модель
        response = self.report.request_json('place=prime&allow-collapsing=1&text=book')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {"entity": "product", "titles": {"raw": "book model"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что по запросу к place prime со включенным флагоми allow-collapsing в гурулайт категории и текстом
        # в выдаче остается только одна модель из гурулайт категории
        response = self.report.request_json('place=prime&allow-collapsing=1&text=water&hid=30')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {"entity": "product", "titles": {"raw": "white liquid"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_collapsing_on_sorting(self):
        '''
        Проверка схлопывания на сортировках
        '''

        # проверяем, что по запросу к place prime в категорию 10 со включенным флагом allow-collapsing
        # и сортировкой по цене
        # в выдаче 4 модели, выстроенные в порядке возрастания цены привязанных к ним офферов

        response = self.report.request_json('place=prime&hid=10&allow-collapsing=1&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {"entity": "product", "titles": {"raw": "black substance"}, "prices": {"min": "100", "max": "200"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}, "prices": {"min": "300", "max": "400"}},
                    {"entity": "product", "titles": {"raw": "light substance"}, "prices": {"min": "500", "max": "600"}},
                    {"entity": "product", "titles": {"raw": "white substance"}, "prices": {"min": "700", "max": "700"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что по запросу к place prime в категорию 10 со включенным флагом allow-collapsing
        # и сортировкой по рейтингу
        # в выдаче 4 модели, выстроенные в порядке убывания их рейтинга

        response = self.report.request_json('place=prime&hid=10&allow-collapsing=1&how=quality')
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {"entity": "product", "titles": {"raw": "black substance"}, "rating": 5},
                    {"entity": "product", "titles": {"raw": "dark substance"}, "rating": 4.5},
                    {"entity": "product", "titles": {"raw": "light substance"}, "rating": 4},
                    {"entity": "product", "titles": {"raw": "white substance"}, "rating": 3.5},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что по запросу к place prime в категорию 10 со включенным флагом allow-collapsing
        # и сортировкой по отзывам
        # в выдаче 4 модели, выстроенные в порядке убывания количества отзывов

        response = self.report.request_json('place=prime&hid=10&allow-collapsing=1&how=opinions')
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {"entity": "product", "titles": {"raw": "white substance"}, "opinions": 40},
                    {"entity": "product", "titles": {"raw": "light substance"}, "opinions": 30},
                    {"entity": "product", "titles": {"raw": "dark substance"}, "opinions": 20},
                    {"entity": "product", "titles": {"raw": "black substance"}, "opinions": 10},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_collapsing_on_sorting_with_text_request(self):
        '''
        Проверка схлопывания на текстовых запросах и сортировке, отдичной от сортировки по умолчанию
        '''

        # Проверяем, что по запросу к place prime со включенным флагоми allow-collapsing и текстом с сортировкой по цене
        # в выдаче 4 модели, а за ними оффер из гуру категории не привязанный к модели,
        # выстроенные в порядке возрастания цены привязанных к ним офферов
        response = self.report.request_json('place=prime&allow-collapsing=1&text=powder&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "total": 5,
                "results": [
                    {"entity": "product", "titles": {"raw": "black substance"}, "prices": {"min": "100", "max": "200"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}, "prices": {"min": "300", "max": "400"}},
                    {"entity": "product", "titles": {"raw": "light substance"}, "prices": {"min": "500", "max": "600"}},
                    {"entity": "product", "titles": {"raw": "white substance"}, "prices": {"min": "700", "max": "700"}},
                    {"entity": "offer", "titles": {"raw": "colorless powder"}, "prices": {"value": "900"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_collapsing_with_filters(self):
        '''
        Проверка схлопывания вместе с репортными фильтрами
        '''

        # Проверяем, что по запросу к place prime с включенным флагом allow-collapsing
        # с фильтром manufacturer_warranty
        # в выдаче 2 модели, для офферов которых не была отключена гарантия производителя
        response = self.report.request_json('place=prime&hid=10&allow-collapsing=1&manufacturer_warranty=1')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_default_collapsing(self):
        '''
        Проверка включения схлопывания по умолчанию
        '''

        # Эта функциональность доджна быть отключена после решения
        # https://st.yandex-team.ru/MARKETVERSTKA-20374

        # Проверям, что по запросу к place prime к категории 10 и с указанием pp 7
        # Включается схлопывание, т.е. в выдаче остается только 4 модели.
        response = self.report.request_json('pp=7&place=prime&hid=10', add_defaults=False)
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {"entity": "product", "titles": {"raw": "black substance"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}},
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_regional_delimiter_with_collapsing(self):
        '''
        Проверка работы черты, отделяющей нелокальную доставку, вметсе со схлопыванием
        '''

        # Проверяем, что по запросу к place prime со включенным флагоми allow-collapsing и заданным регионом 1 и текстом
        # в выдаче сначала идет модель, к которой был привязан оффер с доставкой из региона 2,
        # потом черта, а потом оффер с доставкой из региона  не привязанный к модели
        response = self.report.request_json('place=prime&allow-collapsing=1&rids=1&text=from+area')

        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"entity": "product", "titles": {"raw": "multiregional model"}},
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer", "titles": {"raw": "from area 2"}},
                ],
            },
        )

    def test_discount_sort_no_collapsing(self):
        """
        Проверка отключения схлопывания при сортировке по размеру скидки
        """
        # Проверяем, что по запросу к place prime в категорию 40 с включенным флагом allow-collapsing
        # получим всего 1 документ - модель, то есть произойдет схлопывание
        response = self.report.request_json('place=prime&hid=40&allow-collapsing=1')
        self.assertFragmentIn(
            response, {"total": 1, "results": [{"entity": "product", "titles": {"raw": "model for discount"}}]}
        )
        # Проверяем, что по запросу к place prime в категорию 40 с включенным флагом allow-collapsing
        # и сортировкой по размеру скидки (how=discount_p) получим 2 документа - модель и оффер,
        # то есть схлопывания не произойдет
        response = self.report.request_json('place=prime&hid=40&allow-collapsing=1&how=discount_p')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"entity": "product", "titles": {"raw": "model for discount"}},
                    {"entity": "offer", "titles": {"raw": "offer for discount"}},
                ],
            },
        )

    def test_no_offer_collapring_on_offerid_requets(self):
        # check collapsing working
        response = self.report.request_json('place=prime&hid=10&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {"entity": "product", "titles": {"raw": "black substance"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}},
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # check collapring disabled
        response = self.report.request_json('place=prime&hid=10&allow-collapsing=1&offerid=aOgVX7lvufxf5cgCc5gaTA')

        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {"entity": "offer"},
                ],
            },
            allow_different_len=False,
        )

    def test_collapsing_on_touch_text(self):
        '''
        Проверка автоматической активации схлопывания на текстовых запросах при
        pp=48 (тач)
        '''

        # Проверяем, что по запросу к place prime
        # и флагом тача (touch=1)
        # и листовой гуру категории
        # и текстом
        # в выдаче документы идут по релевантности
        # (схлопывание отработало)
        response = self.report.request_json('touch=1&place=prime&text=powder&hid=10')
        self.assertFragmentIn(
            response,
            {
                "total": 5,
                "results": [
                    {"entity": "offer", "titles": {"raw": "colorless powder"}},
                    {"entity": "product", "titles": {"raw": "black substance"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}},
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что по запросу к place prime
        # и флагом тача (touch=1)
        # и текстом равным слову из названия офферов из гуру категории
        # выдача сформирована по релевантности:
        # оффер из гуру категории не привязанный к модели, а за ним 4 модели
        # (схлопывание отработало)
        response = self.report.request_json('touch=1&place=prime&text=powder')
        self.assertFragmentIn(
            response,
            {
                "total": 5,
                "results": [
                    {"entity": "offer", "titles": {"raw": "colorless powder"}},
                    {"entity": "product", "titles": {"raw": "black substance"}},
                    {"entity": "product", "titles": {"raw": "dark substance"}},
                    {"entity": "product", "titles": {"raw": "light substance"}},
                    {"entity": "product", "titles": {"raw": "white substance"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_aggregate_counters(self):
        '''
        MARKETOUT-12852 Проверка общего количества офферов и моделек
        при включенном схлопывании
        '''

        # Задаем запрос к place prime со включенным схлопыванием и
        # текстом из названия офферов (powder) из гуру категории
        # и проверяем, что общее количество найденного будет равно 5,
        # количество найденных офферов — 1 (один оффер без модели),
        # а количесво моделей 4
        response = self.report.request_json('place=prime&text=powder&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 5,
                    "totalOffers": 1,
                    "totalModels": 4,
                }
            },
        )

        # Задаем запрос к place prime со включенным схлопыванием
        # в гуру категорию 10
        # и проверяем, что общее количество найденного будет равно 4,
        # количество найденных офферов — 0 (оффер без модели удален),
        # а количество моделей 4
        response = self.report.request_json('place=prime&hid=10&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "totalOffers": 0,
                    "totalModels": 4,
                }
            },
        )

    @classmethod
    def prepare_missing_head(cls):
        '''
        MARKETOUT-12818
        Подготовока данных для проверки схлопывания в голове выдачи, когда
        срабатывает ограничение по количеству офферов от одного магазина
        '''

        # создаем 20 офферов в гуру категории
        # 10 первых привязаны к одному магазину, остальные 10 — ко воторому
        # (у всех должна быть своя картинка, чтобы не отсекались как дубликаты)
        # каждый из офферов привязан к своей модели
        # у всех офферов в названии есть одно и то же слово (hidden);
        # этого слова не должно быть в названии модели
        cls.index.hypertree += [HyperCategory(hid=100, output_type=HyperCategoryType.GURU)]
        for i in range(20):
            hyperid = 140 + i
            offer_ts = 120 + i
            picture = Picture(width=100, height=100, group_id=1234)

            cls.index.offers += [
                Offer(
                    title="hidden offer " + str(i + 1),
                    fesh=110 + i / 10,
                    hid=100,
                    hyperid=hyperid,
                    ts=offer_ts,
                    picture=picture,
                )
            ]
            cls.index.models += [
                Model(title="visible model " + str(i + 1), hid=100, hyperid=hyperid),
            ]

            # выставляем значение формулы для гарантированного порядка в выдаче
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, offer_ts).respond(0.01 * (20 - i))

    def test_missing_head(self):
        '''
        MARKETOUT-12818
        Проверка схлопывания в голове выдачи, когда
        срабатывает ограничение по количеству офферов от одного магазина
        '''

        # делаем запрос в place prime с флагом включения схлопывания с текстом из названия офферов
        # ожидаем увидеть модели с 1 по 5 и с 11 по 15.
        # (офферы с 6 по 10 удалены из-за ограничения по количеству офферов от одного магазина)

        # проверяем, что были запрошены модели с 1 по 20,
        # т.е. больше количества документов на странице (при запросе была обработана группировка dsrcid)
        response = self.report.request_json('place=prime&hid=100&allow-collapsing=1&text=hidden&numdoc=48')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "visible model 1"}},
                    {"entity": "product", "titles": {"raw": "visible model 2"}},
                    {"entity": "product", "titles": {"raw": "visible model 3"}},
                    {"entity": "product", "titles": {"raw": "visible model 4"}},
                    {"entity": "product", "titles": {"raw": "visible model 5"}},
                    {"entity": "product", "titles": {"raw": "visible model 11"}},
                    {"entity": "product", "titles": {"raw": "visible model 12"}},
                    {"entity": "product", "titles": {"raw": "visible model 13"}},
                    {"entity": "product", "titles": {"raw": "visible model 14"}},
                    {"entity": "product", "titles": {"raw": "visible model 15"}},
                ]
            },
        )

    @classmethod
    def prepare_model_redirect_with_collapsing(cls):
        '''
        MARKETOUT-12873
        Подготовка данных для проверки работы модельного редиректа вместе со схлопыванием
        '''

        # Создаем три модели, к каждой из которых привязываем по три оффера
        # В названии моделй и офферов должно быть одно и то же слово (redirect)
        # Также выставляем для моделей занчение формулы,
        # чтобы получить гарантированный порядок ранжирования
        for i in range(3):
            hyperid = 210 + i
            model_ts = 220 + i
            cls.index.models += [Model(title="model for redirect " + str(i + 1), hyperid=hyperid, ts=model_ts)]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, model_ts).respond(1.0 - i * 0.1)
            for j in range(3):
                offer_index = 3 * i + j
                offer_ts = 230 + offer_index
                cls.index.offers += [
                    Offer(
                        title="offer for model redirect " + str(offer_index + 1),
                        fesh=240 + offer_index,
                        hyperid=hyperid,
                        ts=offer_ts,
                    )
                ]

    def test_model_redirect_with_collapsing(self):
        '''
        Проверка работы модельного редиректа вместе со схлопыванием
        '''

        # Делаем запрос в place prime с флагом схлопывания и разрешением редиректа
        # модельный редирект НЕ должен произойти, поэтому проверяем отсутсвие данных уикальных
        # для данного типа редиректа

        # Схлопывание изменяет количество найденных документов (total),
        # поэтому может влиять на логику редиректов
        # Данный тест проверяет, что редиректы работают так же как и без схлопывания
        # (total для редиректов не изменился)
        response = self.report.request_json('place=prime&text=redirect&allow-collapsing=1&cvredirect=1')
        self.assertFragmentNotIn(
            response,
            {
                "redirect": {
                    "params": {
                        "modelid": ["210"],
                        "rt": ["4"],
                    }
                }
            },
        )

    @classmethod
    def prepare_total(cls):
        '''
        MARKETOUT-13012
        Подготовка данных для проверки подсчета общего количества
        найденных докуметнов при схлопывании
        '''

        # Создаем гуру категорию
        # 50 офферов и 50 моделей (офферы привязаны к каждый к своей модели)
        # и 50 офферов не привязанных к моделям
        # у всех офферов в названии должно быть одно и то же слово (massive)
        # все офферы и модели привязаны к ранее созданной категории
        cls.index.hypertree += [HyperCategory(hid=300, output_type=HyperCategoryType.GURU)]

        for i in range(50):
            cls.index.offers += [Offer(title="massive offer " + str(i + 1), hyperid=350 + i, hid=300)]
            cls.index.offers += [Offer(title="massive offer " + str(i + 1), hid=300)]

            cls.index.models += [Model(title="popular model " + str(i + 1), hyperid=350 + i, hid=300)]

    def test_total(self):
        '''
        MARKETOUT-13012
        проверка подсчета общего количества
        найденных докуметнов при схлопывании
        '''

        # делаем запрос в place prime к ранее созданной категории
        # со включенным флагом схлопывания
        # ожидаем увидеть общее количество документов 50 из них 50 моделей и 0 офферов
        # (офферы не привязанные к модели удалены)
        response = self.report.request_json(
            'place=prime&hid=300&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 50,
                    "totalOffers": 0,
                    "totalModels": 50,
                }
            },
        )

        # делаем запрос в place prime с текстом из названия офферов
        # со включенным флагом схлопывания
        # ожидаем увидеть общее количество документов 100 из них 50 моделей и 50 офферов
        response = self.report.request_json(
            'place=prime&text=massive&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 100,
                    "totalOffers": 50,
                    "totalModels": 50,
                }
            },
        )

    def test_total_only_offers(self):
        """https://st.yandex-team.ru/MARKETOUT-30678
        Проверяем что итерация документов будет при схлопывании даже если запрос идет только в офферуню коллекцию
        """

        response = self.report.request_json('place=prime&hyperid=350&hyperid=351&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {"entity": "product", "id": 350},
                        {"entity": "product", "id": 351},
                    ],
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_double_regional_delimiter(cls):
        '''
        Подготовка данных для проверки отсутствия повторной черты
        '''

        # MARKETOUT-13249, MARKETOUT-13383
        # Между моделью и оффером с доставкой из другого региона на каждой странице выдачи появлялась черта.
        # Создаем данные на две страницы выдачи с переходом от модели к нелокальному офферу на второй странице.

        # создаем 2 региона
        cls.index.regiontree += [
            Region(rid=450, name='Test region 1'),
            Region(rid=451, name='Test region 2'),
        ]

        # создаем магазин и магазин с доставкой
        cls.index.shops += [
            Shop(fesh=460, priority_region=450),
            Shop(fesh=461, priority_region=451, regions=[450]),
        ]

        # создаем модель
        cls.index.models += [
            Model(hyperid=470, title='Test_offer model 1'),
        ]

        # создаем офферы в регионе
        cls.index.offers += [
            Offer(title='Test_offer 1', fesh=460),
            Offer(title='Test_offer 2', fesh=460),
        ]
        # создаем несколько офферов, чтобы было две страницы выдачи по 12 предложений
        cls.index.offers += [Offer(title="Test_offer {0}".format(oid), fesh=461) for oid in range(3, 13)]
        # создаем оффер в модели и два оффера, которые опустим в конец выдачи
        cls.index.offers += [
            Offer(title='Test_offer 13', fesh=461, hyperid=471),
            Offer(title='Test_offer 14', fesh=461, ts=414),
            Offer(title='Test_offer 15', fesh=461, ts=415),
        ]

        # опускаем два последних оффера в конец выдачи,
        # чтобы модель оказалась перед ними
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 414).respond(0.0)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 415).respond(0.0)

    def test_double_regional_delimiter(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-13249
        https://st.yandex-team.ru/MARKETOUT-13383
        Проверка отсутствия повторной черты
        '''

        # Так как у модели delivery_priority=priority, то без этого фикса
        # между моделью и оффером с доставкой из другого региона
        # на каждой странице выдачи появлялась черта.
        request = (
            "place=prime&rids=450&text=Test_offer&local-offers-first=1"
            "&deliveryincluded=0&offer-shipping=delivery&allow-collapsing=1&page={0}"
        )

        # Проверяется наличие черты на первой странице выдачи
        response = self.report.request_json(request.format(1))
        self.assertFragmentIn(response, {"entity": "regionalDelimiter"})
        # Проверяется отсутсвие черты на второй странице выдачи
        response = self.report.request_json(request.format(2))
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"})

    @classmethod
    def prepare_collapsed_factors(cls):
        cls.index.models += [Model(ts=401, hyperid=400, hid=60, title='model for collapsedfactors test')]

        cls.index.offers += [Offer(ts=40101, hyperid=400, hid=60, title='offer for collapsedfactors test')]

        cls.index.dssm.hard2_query_embedding.on(query='collapsedfactors').set(0.3, 0.3, 0.3)

        cls.index.dssm.hard2_dssm_values_binary.on(ts=401).set(0.4, 0.2, -0.6)
        cls.index.dssm.hard2_dssm_values_binary.on(ts=40101).set(0.5, 0.5, 0.2)

    def test_collapsed_factors(self):
        """Факторы CLPS_* передаются документу от модели, в которую был схлопнут оффер"""

        # dssm_hard несхлопнутого оффера отличается от dssm_hard несхлопнутой модели
        response = self.report.request_json('place=prime&text=collapsedfactors&allow-collapsing=0&hid=60&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'titles': {'raw': 'model for collapsedfactors test'},
                        'debug': {
                            'factors': {'DSSM_HARD2': Round(0.6836), 'CLPS_DSSM_HARD2': NoKey('CLPS_DSSM_HARD2')}
                        },
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'offer for collapsedfactors test'},
                        'debug': {
                            'factors': {'DSSM_HARD2': Round(0.7296), 'CLPS_DSSM_HARD2': NoKey('CLPS_DSSM_HARD2')}
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        # при схлапывапывании CLPS_DSSM_HARD2 берется от модели
        response = self.report.request_json(
            'place=prime&text=collapsedfactors&allow-collapsing=1&hid=60&debug=da&entities=offer'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'titles': {'raw': 'model for collapsedfactors test'},
                        'debug': {
                            'isCollapsed': True,  # модель схлопнутая
                            'tech': {'docPriority': NotEmpty()},  # docPriority прокидывается для схлопнутых моделей
                            'factors': {
                                'DSSM_HARD2': Round(0.7296),  # базовый фактор TG_BASE от оффера
                                'CLPS_DSSM_HARD2': Round(0.6836),  # фактор TG_BASE_COLLAPSED от схлопнутой модели
                            },
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_collapsed_category_stats(cls):
        '''
        MARKETOUT-13393

        Подгтовка данных для проверки статистики по количеству найденного в категориях
        при включенном схлопывании
        '''

        # создаем две гуру-категории, одну модель с двумя офферами в первой категории
        # модель с двумя офферами и оффер без модели во воторой категории
        # в названии офферов должно быть одно и то же слово, кторого нету в названии моделей
        cls.index.hypertree += [
            HyperCategory(hid=500, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=501, output_type=HyperCategoryType.GURU),
        ]
        cls.index.navtree += [
            NavCategory(nid=500, hid=500),
            NavCategory(nid=501, hid=501),
        ]
        cls.index.models += [
            Model(title='model_categ_stat', hyperid=510, hid=500),
            Model(title='model_categ_stat', hyperid=511, hid=501),
        ]
        cls.index.offers += [
            Offer(title='offer_categ_stat', hyperid=510, fesh=520, hid=500),
            Offer(title='offer_categ_stat', hyperid=510, fesh=521, hid=500),
            Offer(title='offer_categ_stat', hyperid=511, fesh=522, hid=501),
            Offer(title='offer_categ_stat', hyperid=511, fesh=523, hid=501),
            Offer(title='offer_categ_stat', fesh=524, hid=501),
        ]

    def test_collapsed_category_stats(self):
        '''
        MARKETOUT-13393
        проверка статистики по количеству найденного в категориях
        при включенном схлопывании
        '''

        # в резултате запросов ожидаем увидеть:
        # количество найденного в первой категории равное 1 (два оффера заменены на одну модель)
        # колечество найденного во второй категории равное 2
        # (два оффера заменены на одну модель и оффер без модели остался как есть)
        expected_hid = {
            'intents': [
                {
                    'ownCount': 1,
                    'category': {
                        'hid': 500,
                    },
                },
                {
                    'ownCount': 2,
                    'category': {
                        'hid': 501,
                    },
                },
            ]
        }

        expected_nid = {
            'intents': [
                {
                    'ownCount': 3,
                    'category': {
                        'hid': 500,
                    },
                },
                {
                    'ownCount': 4,
                    'category': {
                        'hid': 501,
                    },
                },
            ]
        }

        for flag in ['&rearr-factors=turn_off_nid_intents_on_serp=0', '&rearr-factors=market_return_nids_in_intents=0']:
            expected = expected_hid if 'turn_off_nid_intents_on_serp' not in flag else expected_nid
            # задаем запрос со словом из названия офееров к place prime со включенным схлопыванием
            # проверяем ожидания
            response = self.report.request_json('place=prime&text=offer_categ_stat&allow-collapsing=1' + flag)
            self.assertFragmentIn(response, expected)

    @classmethod
    def prepare_collapsing_on_discount_sorting(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=1,
                children=[
                    HyperCategory(hid=90569, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=12345, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=67890, output_type=HyperCategoryType.SIMPLE),
                    HyperCategory(hid=42424, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.shops += [Shop(fesh=720), Shop(fesh=721)]

        cls.index.models += [
            Model(hyperid=14115324, title="Фен Saturn ST-HC7355", hid=90569, vendor_id=3827090),
            Model(hyperid=14115325, title="Фен Saturn ST-HC7355 теперь с промо!", hid=42424),
        ]

        cls.index.offers += [
            Offer(
                hyperid=14115324, title="Фен Saturn ST-HC7355", fesh=720, price_old=1213, price=342, vendor_id=3827090
            ),
            Offer(
                hyperid=14115325,
                title="Фен Saturn ST-HC7355 теперь с промо!",
                fesh=721,
                promo=Promo(key='super_promo', promo_type=PromoType.PROMO_CODE),
            ),
        ]

    def test_not_collapse_on_sort_by_discount_and_no_filter_discount_only(self):
        """
        Проверяем, что не схлопывается, если сортировка по скидке и НЕТ фильтра по скидке
        В результат попадает и оффер, и модель 14115324
        """
        response = self.report.request_json('place=prime&allow-collapsing=1&hid=90569&how=discount_p')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "product",
                            "id": 14115324,
                        },
                        {"entity": "offer", "model": {"id": 14115324}},
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_collapse_on_sort_by_discount_and_filter_discount_only(self):
        """
        Проверяем, что схлопывается, если сортировка по скидке И фильтр по скидке
        """
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=90569&how=discount_p&filter-discount-only=1'
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [{"entity": "product", "id": 14115324}]}},
            allow_different_len=False,
        )

    def test_collapse_on_sort_by_discount_and_filter_discount_only_and_multiple_hids(self):
        """
        Проверяем, что схлопывается, если сортировка по скидке И фильтр по скидке И несколько hids
        См. MARKETOUT-18078
        """
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=90569,12345&how=discount_p&filter-discount-only=1'
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [{"entity": "product", "id": 14115324}]}},
            allow_different_len=False,
        )

    def test_collapse_on_sort_by_discount_and_filter_discount_only_and_multiple_hids_with_at_least_one_guru(self):
        """
        Проверяем, что схлопывается, если сортировка по скидке И фильтр по скидке И несколько hids
        И есть хотя бы одна гуру-категория

        90569 - гуру
        12345 - гуру
        67890 - не-гуру

        См. MARKETOUT-18078
        """
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=90569,12345,67890&how=discount_p&filter-discount-only=1'
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [{"entity": "product", "id": 14115324}]}},
            allow_different_len=False,
        )

    def test_collapse_on_sort_by_discount_and_filter_discount_only_and_multiple_hids_with_at_least_one_guru_and_guru_hid_is_not_the_first(
        self,
    ):
        """
        Проверяем, что схлопывается, если сортировка по скидке И фильтр по скидке И несколько hids
        И есть хотя бы одна гуру-категория
        И эта одна гуру-категория - первая в списке

        90569 - гуру
        12345 - гуру
        67890 - не-гуру

        См. MARKETOUT-18078
        """
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=67890,90569,12345&how=discount_p&filter-discount-only=1'
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [{"entity": "product", "id": 14115324}]}},
            allow_different_len=False,
        )

    def test_collapse_on_has_promo(self):
        """
        Проверяем, что схлопывается, если есть has-promo в запросе

        См. MARKETOUT-26912
        """
        response = self.report.request_json('place=prime&allow-collapsing=1&has-promo=1')
        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [{"entity": "product", "id": 14115325}]}},
            allow_different_len=False,
        )

    def test_collapse_debug(self):
        """
        Проверяем debug-выдачу

        MARKETOUT-18146
        """
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=90569,12345,67890&how=discount_p&filter-discount-only=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "logicTrace": [
                            Contains("Categories to collapse: 90569, category type = 1"),
                            Contains("Categories to collapse: 12345, category type = 1"),
                            Contains("Categories to collapse: 67890, category type = 0"),
                            Contains("isAtLeastOneGuruCategory = 1"),
                            Contains("isLeafCategory = 1"),
                            Contains("isClusterCategory = 0"),
                            Contains("hasTextRequest = 0"),
                            Contains("isProperPp = 0"),
                            Contains("isTouch = 0"),
                            Contains("isCgiAllowCollapsing = 1"),
                            Contains("isCgiHomeRegionFilterSet = 0"),
                            Contains("isCgiOfferIdSet = 0"),
                            Contains("isCgiSortingBad = 0"),
                            Contains("isCgiAllowCollapsingExplicitlyDisabled = 0"),
                            Contains("isCgiInNoModelsExp = 0"),
                            Contains("needToEnableCollapsing = 1"),
                            Contains("needToDisableCollapsing = 0"),
                            Contains("enableCollapsing = 1"),
                        ]
                    }
                }
            },
        )

    def test_skk(self):
        """СКК ходит во фронт по урлу вида market.yandex.ru/search?fesh=12345 и просматриваем все офферы
        Надо чтобы они не схлапывались (allow-collapsing=0 сейчас на фронте не работает)
        """

        offer = {'results': [{"entity": "offer", "titles": {"raw": "Фен Saturn ST-HC7355"}}]}

        model = {'results': [{"entity": "product", "titles": {"raw": "Фен Saturn ST-HC7355"}}]}

        rearr = '&allow-collapsing=1'

        # запрос от СКК не содержит текста, hid, nid или vendor_id
        response = self.report.request_json('place=prime&fesh=720' + rearr)
        self.assertFragmentIn(response, offer, allow_different_len=False)

        # а эти запросы уже могут быть заданы обычными пользователями
        response = self.report.request_json('place=prime&fesh=720&hid=90569' + rearr)
        self.assertFragmentIn(response, model, allow_different_len=False)

        response = self.report.request_json('place=prime&fesh=720&text=фен' + rearr)
        self.assertFragmentIn(response, model, allow_different_len=False)

        response = self.report.request_json('place=prime&fesh=720&vendor_id=3827090' + rearr)
        self.assertFragmentIn(response, model, allow_different_len=False)


if __name__ == '__main__':
    main()

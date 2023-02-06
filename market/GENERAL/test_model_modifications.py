#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DeliveryOption, GLParam, GLType, GLValue, Model, Offer, Promo, PromoType, RegionalModel, Shop
from core.testcase import TestCase, main
from core.matcher import ElementCount, NoKey, NotEmpty


class T(TestCase):
    @classmethod
    def prepare_format_and_show_log(cls):
        """
        Подготовка к проверке формата и шоулога
        Создаем 1 групповую модель и две модификации, по одному офферу на каждую
        """
        cls.index.models += [
            Model(hyperid=11, group_hyperid=1, title='wheel 11'),
            Model(hyperid=12, group_hyperid=1, title='wheel 12'),
        ]

        cls.index.offers += [Offer(hyperid=11), Offer(hyperid=12)]

    def test_invalid_user_cgi(self):
        """
        Запрос без параметра hyperid ведёт к ошибке
        """
        response = self.report.request_json('place=model_modifications&pp=18', strict=False)
        self.assertFragmentIn(
            response,
            {
                "error": {
                    "code": 1010,
                }
            },
        )
        self.error_log.expect('Cannot find CGI-parameter \'hyperid\'')
        self.error_log.ignore(code=1010)
        self.assertEqual(500, response.code)

    def test_format_and_show_log(self):
        """
        Проверка формата и шоулога
        Задаем запрос в model_modifications, фиксируем выдачу, проверяем, что в шоулог попало ровно 2 показа
        """
        response = self.report.request_json('place=model_modifications&hyperid=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'totalOffers': 0,
                    'totalModels': 2,
                    'results': [
                        {
                            'showUid': NotEmpty(),
                            'entity': 'product',
                            'vendor': NotEmpty(),
                            'titles': {'raw': 'wheel 12', 'highlighted': [{'value': 'wheel 12'}]},
                            'description': '',
                            'eligibleForBookingInUserRegion': False,
                            'categories': NotEmpty(),
                            'navnodes': NotEmpty(),
                            'pictures': NotEmpty(),
                            'type': 'modification',
                            'id': 12,
                            'parentId': 1,
                            'offers': {'count': 1},
                            'retailersCount': 1,
                            'isNew': False,
                        },
                        {
                            'showUid': NotEmpty(),
                            'entity': 'product',
                            'vendor': NotEmpty(),
                            'titles': {'raw': 'wheel 11', 'highlighted': [{'value': 'wheel 11'}]},
                            'description': '',
                            'eligibleForBookingInUserRegion': False,
                            'categories': NotEmpty(),
                            'navnodes': NotEmpty(),
                            'pictures': NotEmpty(),
                            'type': 'modification',
                            'id': 11,
                            'parentId': 1,
                            'offers': {'count': 1},
                            'retailersCount': 1,
                            'isNew': False,
                        },
                    ],
                },
            },
        )

        # Проверяем формат и содержание доступных сортировок
        self.assertFragmentIn(
            response,
            {
                'search': NotEmpty(),
                'sorts': [
                    {"text": "по популярности"},
                    {"text": "по цене", "options": [{"id": "aprice", "type": "asc"}, {"id": "dprice", "type": "desc"}]},
                    {"text": "по новизне", "options": [{"id": "ddate"}]},
                ],
            },
        )

        self.show_log.expect(show_uid='04884192001117778888816000').times(1)
        self.show_log.expect(show_uid='04884192001117778888816001').times(1)

    @classmethod
    def prepare_paging(cls):
        """
        Подготовка к проверки пейджинга
        Создаем 5 модификаций у одной групповой модели
        """
        cls.index.models += [
            Model(hyperid=21, group_hyperid=2),
            Model(hyperid=22, group_hyperid=2),
            Model(hyperid=23, group_hyperid=2),
            Model(hyperid=24, group_hyperid=2),
            Model(hyperid=25, group_hyperid=2),
            Model(hyperid=26, group_hyperid=2),
        ]

    def test_paging(self):
        """
        Проверка пейджинга
        Задаем запрос с кол-вом документов на странице 2 -- ожидаем 2 модификации
        Задаем запрос с кол-вом документов на странице 4, первую страницу -- ожидаем 4 документа
        Задаем запрос с кол-вом документов на странице 4, вторую страницу -- ожидаем 2 документа
        Задаем запрос с кол-вом документов на странице 4, большую страницу -- ожидаем 2 документа (как на последней)
        """
        response = self.report.request_json('place=model_modifications&hyperid=2&numdoc=2')
        self.assertFragmentIn(response, {'search': {"total": 6, 'results': ElementCount(2)}})

        response = self.report.request_json('place=model_modifications&hyperid=2&numdoc=4&page=1')
        self.assertFragmentIn(response, {'search': {"total": 6, 'results': ElementCount(4)}})

        response = self.report.request_json('place=model_modifications&hyperid=2&numdoc=4&page=2')
        self.assertFragmentIn(response, {'search': {"total": 6, 'results': ElementCount(2)}})

        response = self.report.request_json('place=model_modifications&hyperid=2&numdoc=4&page=22')
        self.assertFragmentIn(response, {'search': {"total": 6, 'results': ElementCount(2)}})

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='6').times(4)

    @classmethod
    def prepare_sorting(cls):
        """
        Подготовка к проверке сортировок
        Создаем три модели так, чтобы:
         - кол-во кликов возрастало для каждой следующей (формируем популярность: т.к. кол-во офферов постоянно (100),
         популярность определяют клики)
         - минимальная цена возрастала для каждой следующей
         - 2я более новая, потом 1я, потом 3я
         - 4я без офферов в регионе доставки, но самая новая
        """
        cls.index.models += [
            Model(hyperid=31, group_hyperid=3, model_clicks=10, created_ts=200000000),
            Model(hyperid=32, group_hyperid=3, model_clicks=20, created_ts=500000000),
            Model(hyperid=33, group_hyperid=3, model_clicks=30, created_ts=100000000),
            Model(hyperid=34, group_hyperid=3, model_clicks=50, created_ts=600000000),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=31, offers=100, price_min=10, rids=[213]),
            RegionalModel(hyperid=32, offers=100, price_min=11, rids=[213]),
            RegionalModel(hyperid=33, offers=100, price_min=12, rids=[213]),
        ]

    def test_sorting(self):
        """
        Задаем запрос с сортировкой по популярности, ожидаем получить модификации в порядке: 3я, 2я, 1я
        Задаем запрос с сортировкой по цене по-убыванию, ожидаем получить модификации в порядке: 1я, 2я, 3я
        Задаем запрос с сортировкой по цене по-возрастанию, ожидаем получить модификации в порядке: 3я, 2я, 1я
        Задаем запрос с сортировкой по дате добавления, ожидаем получить модификации в порядке: 2я, 1я, 3я
        """
        response = self.report.request_json('place=model_modifications&hyperid=3&how=guru_popularity&rids=213')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 33},
                    {'id': 32},
                    {'id': 31},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=model_modifications&hyperid=3&how=aprice&rids=213')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 31},
                    {'id': 32},
                    {'id': 33},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=model_modifications&hyperid=3&how=dprice&rids=213')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 33},
                    {'id': 32},
                    {'id': 31},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=model_modifications&hyperid=3&how=ddate&rids=213')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 32},
                    {'id': 31},
                    {'id': 33},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=model_modifications&hyperid=3&how=ddate&rids=213&local-offers-first=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 34},
                    {'id': 32},
                    {'id': 31},
                    {'id': 33},
                ]
            },
            preserve_order=True,
        )

        # Check there are no errors
        for sorting in ('model_card', 'search'):
            response = self.report.request_json('place=model_modifications&hyperid=3&how={}&rids=213'.format(sorting))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'id': 34},
                        {'id': 32},
                        {'id': 31},
                        {'id': 33},
                    ]
                },
                preserve_order=False,
            )

    @classmethod
    def prepare_test_filters(cls):
        """
        Создаем по два параметра каждого типа (boolean, enum, numeric) для модификаций, по одному из параметров модификации
        будут отличаться, по другому параметру они все будут идентичны
        """
        cls.index.gltypes += [
            GLType(
                param_id=401,
                hid=4,
                gltype=GLType.BOOL,
                cluster_filter=True,
                xslname="LTE",
                has_model_filter_index=False,
            ),
            GLType(param_id=402, hid=4, gltype=GLType.BOOL, cluster_filter=True, xslname="WiFi"),
            GLType(
                param_id=403,
                hid=4,
                gltype=GLType.ENUM,
                cluster_filter=True,
                xslname="OS",
                values=[
                    GLValue(value_id=1, text="Windows"),
                    GLValue(value_id=2, text="DOS"),
                ],
                model_filter_index=4,
            ),
            GLType(
                param_id=404,
                hid=4,
                gltype=GLType.ENUM,
                cluster_filter=True,
                xslname="ProcType",
                values=[
                    GLValue(value_id=1, text="Core i5"),
                    GLValue(value_id=2, text="Core i7"),
                ],
                has_model_filter_index=False,
            ),
            GLType(param_id=405, hid=4, gltype=GLType.NUMERIC, cluster_filter=True, xslname="HDD"),
            GLType(param_id=406, hid=4, gltype=GLType.NUMERIC, cluster_filter=True, xslname="Memory"),
            GLType(
                param_id=407,
                hid=4,
                cluster_filter=True,
                xslname="Color",
                values=[GLValue(value_id=200, text="Red"), GLValue(value_id=300, text="Green")],
                model_filter_index=100,
            ),
        ]

        # создаем две модификации и присваиваем им параметры в соответствии со схемой выше
        cls.index.models += [
            Model(
                group_hyperid=4000,
                hid=4,
                glparams=[
                    GLParam(param_id=401, value=0),
                    GLParam(param_id=402, value=1),
                    GLParam(param_id=403, value=1),
                    GLParam(param_id=404, value=1),
                    GLParam(param_id=405, value=500),
                    GLParam(param_id=406, value=4),
                ],
            ),
            Model(
                group_hyperid=4000,
                hid=4,
                glparams=[
                    GLParam(param_id=401, value=0),
                    GLParam(param_id=402, value=0),
                    GLParam(param_id=403, value=1),
                    GLParam(param_id=404, value=2),
                    GLParam(param_id=405, value=600),
                    GLParam(param_id=406, value=8),
                ],
                hyperid=4090,
            ),
        ]

        cls.index.shops += [Shop(fesh=7990, regions=[213])]
        cls.index.offers += [
            Offer(
                hyperid=4090,
                fesh=7990,
                glparams=[GLParam(param_id=407, value=200)],
                delivery_options=[DeliveryOption(day_to=1)],
            )
        ]
        cls.index.offers += [
            Offer(
                hyperid=4090,
                fesh=7990,
                glparams=[GLParam(param_id=407, value=300)],
                delivery_options=[DeliveryOption(day_to=1)],
            )
        ]

    def test_modification_filters(self):
        """
        Проверяем, что:
            1. В корне выдачи есть фильтры
            2. Они агреируются по всем модификациям
            3. Фильтра с неотличающися enum-фильтром нет
            4. Фильтра, которого нет ни у одной модификации, тоже нет
            5. Фильтра без model_filter_index - нет
            6. Есть фильтр с параметрами 2 рода, которые есть только на офферах, привязанных к офферам модификаций
        """
        response = self.report.request_json('place=model_modifications&hyperid=4000&hid=4')
        self.assertFragmentIn(
            response,
            {
                "search": NotEmpty(),
                "filters": [
                    {"id": "402", "values": [{"id": "0", "found": 1}, {"id": "1", "found": 1}]},
                    {"id": "405", "values": [{"id": "500~500", "found": 1}, {"id": "600~600", "found": 1}]},
                    {"id": "406", "values": [{"id": "4~4", "found": 1}, {"id": "8~8", "found": 1}]},
                    {"id": "407", "values": [{"id": "300", "found": 1}, {"id": "200", "found": 1}]},
                ],
            },
        )

        self.assertFragmentNotIn(response, {"search": {"filters": [{"id": "401"}]}})
        self.assertFragmentNotIn(response, {"search": {"filters": [{"id": "403"}]}})
        self.assertFragmentNotIn(response, {"search": {"filters": [{"id": "404"}]}})

    @classmethod
    def prepare_test_onstock_filter(cls):
        """Создаем две модели: в продаже и не в продаже. К той, что в продаже, приматчиваем оффер"""
        cls.index.models += [
            Model(title="onstock_model", hyperid=5001, group_hyperid=5000),
            Model(title="not_onstock_model", hyperid=5002, group_hyperid=5000),
        ]

        cls.index.offers += [Offer(hyperid=5001)]

    def test_onstock_filter(self):
        # без onstock=1 находятся все модификации
        response = self.report.request_json('place=model_modifications&hyperid=5000&hid=4')
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'onstock_model'}}, {'titles': {'raw': 'not_onstock_model'}}]},
            allow_different_len=False,
        )

        # c onstock=1 находится только та модификация, что в продаже
        response = self.report.request_json('place=model_modifications&hyperid=5000&hid=4&onstock=1')
        self.assertFragmentIn(response, {'results': [{'titles': {'raw': 'onstock_model'}}]}, allow_different_len=False)

    @classmethod
    def prepare_test_ignore_cpa_filter(cls):
        """Просто создаем модификацию, она должна показываться не зависимо от фильтра по cpa"""
        cls.index.models += [Model(title="model", hyperid=6001, group_hyperid=6000)]

    def test_ignore_cpa_filter(self):
        # Даже с фильтром по cpa модель присутствует в выдаче

        response = self.report.request_json('place=model_modifications&hyperid=6000&cpa=real')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'model'}},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_test_cpa20_no_throws_out_nonCpa20(cls):
        """Создаем модель и 3 модификации, 2 из них содержат cpa20-оффер одна нет"""

        cls.index.models += [
            Model(hyperid=7001, group_hyperid=7000, title="cpa20model_1"),
            Model(hyperid=7002, group_hyperid=7000, title="cpa20model_2"),
            Model(hyperid=7003, group_hyperid=7000),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=7001, rids=[213], offers=2, cpa20=True),
            RegionalModel(hyperid=7002, rids=[213], offers=2, cpa20=True),
        ]

        cls.index.shops += [
            # Магазин по программе CPA 2.0
            Shop(fesh=17001, priority_region=213, cpa=Shop.CPA_REAL, cpa20=True),
            # Обычный магазин
            Shop(fesh=17002, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(hyperid=7001, fesh=17001, bid=10, fee=100, cpa=Offer.CPA_REAL, price=2000, title="cpa20-offer1"),
            Offer(hyperid=7002, fesh=17001, bid=10, fee=100, cpa=Offer.CPA_REAL, price=2000, title="cpa20-offer2"),
            Offer(hyperid=7003, fesh=17002, bid=10, fee=100, cpa=Offer.CPA_REAL, price=2000, title="non_cpa20_offer"),
        ]

    def test_cpa20_throws_out_nonCpa20(self):
        """
        Проверяем, что в модификациях пришли только 2 модели
        """
        # Запрос без фильтрации
        response = self.report.request_json('place=model_modifications&hyperid=7000&rids=213&debug=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "totalModels": 3,
                    "results": [
                        {
                            "type": "modification",
                            "id": 7001,
                        },
                        {
                            "type": "modification",
                            "id": 7002,
                        },
                        {"type": "modification", "id": 7003, "cpa": NoKey("cpa")},
                    ],
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_test_promo_filter(cls):
        cls.index.models += [
            Model(hyperid=19609001, group_hyperid=19609000),
            Model(hyperid=19609002, group_hyperid=19609000),
        ]

        cls.index.shops += [Shop(fesh=19609001, priority_region=213)]

        cls.index.offers += [
            Offer(
                fesh=19609001,
                hyperid=19609001,
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='promo19609001_key000'),
            ),
            Offer(fesh=19609001, hyperid=19609002),
        ]

    def test_promo_filter(self):
        response = self.report.request_json(
            'place=model_modifications&hyperid=19609000&rids=213&debug=1&rearr-factors=market_do_not_split_promo_filter=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "totalModels": 2,
                    "results": [
                        {
                            "type": "modification",
                            "id": 19609001,
                        },
                        {
                            "type": "modification",
                            "id": 19609002,
                        },
                    ],
                },
                "filters": [{"id": "filter-promo-or-discount"}],
            },
        )

        response = self.report.request_json(
            'place=model_modifications&hyperid=19609000&rids=213&debug=1&filter-promo-or-discount=1&rearr-factors=market_do_not_split_promo_filter=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "totalModels": 1,
                    "results": [
                        {
                            "type": "modification",
                            "id": 19609001,
                        }
                    ],
                },
                "filters": [{"id": "filter-promo-or-discount"}],
            },
        )


if __name__ == '__main__':
    main()

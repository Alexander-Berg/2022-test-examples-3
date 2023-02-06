#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClothesIndex,
    Model,
    NewShopRating,
    Offer,
    Picture,
    PictureSignature,
    RegionalModel,
    Shop,
    VCluster,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import main
from core.matcher import NoKey, ElementCount

import simple_testcase
from simple_testcase import SimpleTestCase
from datetime import datetime, timedelta

from unittest import skip


def to_timestamp(dt):
    epoch = datetime(1970, 1, 1)
    return int((dt - epoch).total_seconds())


class T(SimpleTestCase):
    """
    Набор тестов для метода better_price
    """

    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(hyperid=1),
            Model(hyperid=2),
            Model(hyperid=3),
            Model(hyperid=201),
            Model(hyperid=202),
            Model(hyperid=203),
        ]

        cls.index.shops += [Shop(fesh=1, priority_region=213, regions=[225], cpa=Shop.CPA_REAL)]

        cls.index.regional_models += [
            RegionalModel(hyperid=201, price_min=501, rids=[213]),
            RegionalModel(hyperid=202, price_min=502, rids=[213]),
            RegionalModel(hyperid=203, price_min=503, rids=[213]),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=1, price=100),
            Offer(fesh=1, hyperid=2, price=100500),
            Offer(fesh=1, hyperid=3, price=100600),
            Offer(hyperid=201, price=501, fesh=1),
            Offer(hyperid=202, price=502, fesh=1),
            Offer(hyperid=203, price=503, fesh=1),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name="better-price",
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # явно все параметры
                    YamarecSettingPartition(
                        params={
                            "show-history": "1",
                            "filter-by-price": "0",
                            "filter-by-assistant": "0",
                        },
                        splits=[{"split": "1"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "show-history": "0",
                            "filter-by-price": "1",
                            "filter-by-assistant": "0",
                        },
                        splits=[{"split": "2"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "show-history": "0",
                            "filter-by-price": "0",
                            "filter-by-assistant": "1",
                            "assistant-threshold": "0.5",
                        },
                        splits=[{"split": "3"}],
                    ),
                    # пустой конфиг
                    YamarecSettingPartition(params={}, splits=[{"split": "4"}]),
                    # Явно только взведённые флаги и порог
                    YamarecSettingPartition(
                        params={
                            "show-history": "1",
                        },
                        splits=[{"split": "5"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                        },
                        splits=[{"split": "6"}],
                    ),
                    YamarecSettingPartition(
                        params={
                            "filter-by-assistant": "1",
                            "assistant-threshold": "0.5",
                        },
                        splits=[{"split": "7"}],
                    ),
                    # Порог тоже не указан
                    YamarecSettingPartition(
                        params={
                            "filter-by-assistant": "1",
                        },
                        splits=[{"split": "8"}],
                    ),
                    # test_price_threshold
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                            "price-threshold": "0.9",
                        },
                        splits=[{"split": "9"}],
                    ),
                    # test_offers
                    YamarecSettingPartition(
                        params={
                            "show-offers": "1",
                            "filter-by-price": "1",
                        },
                        splits=[{"split": "10"}],
                    ),
                    # test_offers_price_threshold
                    YamarecSettingPartition(
                        params={
                            "show-offers": "1",
                            "filter-by-price": "1",
                            "price-threshold": "0.9",
                        },
                        splits=[{"split": "11"}],
                    ),
                    # test_default_offer
                    # Конфигурация для проверки выбора оферов a la default offer
                    # splits=[{"split": "10"}] - определёно в prepare_offer - неявное задание флага в "0" по умолчанию
                    # Явно сброшенный флаг:
                    YamarecSettingPartition(
                        params={
                            "show-offers": "1",
                            "min-price-offer": "0",
                            "filter-by-price": "1",
                        },
                        splits=[{"split": "12"}],
                    ),
                    # Явно отключаем default offer и подбираем оферы по цене
                    YamarecSettingPartition(
                        params={
                            "show-offers": "1",
                            "min-price-offer": "1",
                            "filter-by-price": "1",
                        },
                        splits=[{"split": "13"}],
                    ),
                    # "время жизни" записей в истории для for test_timestamp_filter
                    # явно 0 - значит, не задано
                    YamarecSettingPartition(
                        params={"filter-by-price": "1", "history-ttl": "0"}, splits=[{"split": "14"}]
                    ),
                    # Задаём "время жизни"
                    YamarecSettingPartition(
                        params={"filter-by-price": "1", "history-ttl": "48"}, splits=[{"split": "15"}]
                    ),
                    # Задаём "время жизни" 2
                    YamarecSettingPartition(
                        params={"show-history": "1", "history-ttl": "48"}, splits=[{"split": "17"}]
                    ),
                    # Конфигурация для проверки данных о лучшей цене в оферной выдачи
                    # кейс полной выдачи без фильтра
                    YamarecSettingPartition(
                        params={
                            "show-offers": "1",
                            "show-history": "1",
                            "filter-by-price": "0",
                        },
                        splits=[{"split": "16"}],
                    ),
                    # Объединение filter-by-price & filter-by-assistant
                    YamarecSettingPartition(
                        params={
                            "show-history": "0",
                            "filter-by-price": "1",
                            "filter-by-assistant": "1",
                            "assistant-threshold": "0.5",
                        },
                        splits=[{"split": "18"}],
                    ),
                    # Конфигурация для проверки ранжирования дешевле-в-топ в модельной выдаче
                    # Флаг явно выключен
                    YamarecSettingPartition(
                        params={
                            "show-history": "1",
                            "cheaper-atop": "0",
                        },
                        splits=[{"split": "19"}],
                    ),
                    # Конфигурация для проверки ранжирования дешевле-в-топ в модельной выдаче
                    YamarecSettingPartition(
                        params={
                            "show-history": "1",
                            "cheaper-atop": "1",
                        },
                        splits=[{"split": "20"}],
                    ),
                    # Конфигурация для проверки ранжирования дешевле-в-топ в офферной выдаче
                    # Флаг явно выключен
                    YamarecSettingPartition(
                        params={
                            "show-offers": "1",
                            "show-history": "1",
                            "cheaper-atop": "0",
                        },
                        splits=[{"split": "21"}],
                    ),
                    # Конфигурация для проверки ранжирования дешевле-в-топ в офферной выдаче
                    YamarecSettingPartition(
                        params={
                            "show-offers": "1",
                            "show-history": "1",
                            "cheaper-atop": "1",
                        },
                        splits=[{"split": "22"}],
                    ),
                ],
            )
        ]

        # для простоты для всех сплитов одна и та же выдача ихвиля:
        for i in [None] + list(range(9)):
            yandexuid = None if i is None else "yandexuid:100{}".format(i)
            cls.recommender.on_request_we_have_cheaper(user_id=yandexuid, item_count=100,).respond(
                {
                    "we_have_cheaper": [
                        {"model_id": 1, "price": 2500.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                        {"model_id": 2, "price": 3500.0, "success_requests_share": 0.8, "timestamp": "1495206745"},
                        {"model_id": 3, "price": 100.0, "success_requests_share": 0.6, "timestamp": "1495206745"},
                        {"model_id": 4, "price": 500.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                        {"model_id": None, "price": 500.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    ]
                }
            )

            cls.recommender.on_request_viewed_models(user_id=yandexuid, item_count=100).respond(
                {"models": map(str, list(range(201, 204)))}
            )

    def make_query(self, split=None, yandexuid=1001, use_simple_better_price=False, sorting_order=None):
        yandexuid_param = "" if yandexuid is None else "&yandexuid={yandexuid}".format(yandexuid=yandexuid)
        rearr_params = []
        if split:
            rearr_params.append("split={split}".format(split=split))
        if sorting_order:
            rearr_params.append("sorting_order_in_better_price={sorting_order}".format(sorting_order=sorting_order))
        rearr_str = "" if not rearr_params else "&rearr-factors={}".format(";".join(rearr_params))
        use_sbp = (
            ""
            if use_simple_better_price is None
            else "&use-simple-better-price={}".format(int(use_simple_better_price))
        )
        return "place=better_price&rids=213{yandexuid_param}{rearr_param}{use_sbp}&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0".format(
            yandexuid_param=yandexuid_param, rearr_param=rearr_str, use_sbp=use_sbp
        )

    def test_simple_better_price(self):
        query = self.make_query(split=1, use_simple_better_price=True)
        self.assertModelsInResponse(query=query, ids=[1, 2, 3, 201, 202, 203])

    @classmethod
    def prepare_simple_better_price_reorder(cls):
        cls.index.models += [
            Model(hyperid=205, hid=104),
            Model(hyperid=206, hid=104),
            Model(hyperid=207, hid=104),
            Model(hyperid=208, hid=105),
            Model(hyperid=209, hid=106),
            Model(hyperid=210, hid=104),
            Model(hyperid=211, hid=104),
            Model(hyperid=212, hid=106),
            Model(hyperid=213, hid=104),
            Model(hyperid=214, hid=106),
            Model(hyperid=215, hid=106),
            Model(hyperid=216, hid=106),
            Model(hyperid=217, hid=106),
            Model(hyperid=218, hid=106),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=model_id, price_min=500, rids=[213]) for model_id in range(205, 219)
        ]

        cls.recommender.on_request_we_have_cheaper(user_id='yandexuid:101010', item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": model_id, "price": 2500.0, "success_requests_share": 0.1, "timestamp": "1495206745"}
                    for model_id in range(205, 219)
                ]
            }
        )

        cls.recommender.on_request_viewed_models(user_id='yandexuid:101010', item_count=100).respond({"models": []})

    def test_simple_better_price_reorder(self):
        query = self.make_query(split=1, use_simple_better_price=True, yandexuid=101010, sorting_order='No')
        self.assertModelsBlockInResponse(
            query=query, ids=[model_id for model_id in range(205, 219)], preserve_order=True
        )
        query = self.make_query(
            split=1, use_simple_better_price=True, yandexuid=101010, sorting_order='OneModelByHidInSuccession'
        )
        self.assertModelsBlockInResponse(
            query=query, ids=[205, 208, 209, 206, 212, 207, 214, 210, 215, 211, 216, 213, 217, 218], preserve_order=True
        )
        query = self.make_query(
            split=1, use_simple_better_price=True, yandexuid=101010, sorting_order='FiveModelsByHidInSuccession'
        )
        self.assertModelsBlockInResponse(
            query=query, ids=[205, 206, 207, 210, 211, 208, 209, 212, 214, 215, 216, 213, 217, 218], preserve_order=True
        )

    @classmethod
    def prepare_empty_simple_better_price(cls):
        cls.recommender.on_request_we_have_cheaper(
            user_id='yandexuid:9999',
            item_count=100,
        ).respond({"we_have_cheaper": []})

        cls.recommender.on_request_viewed_models(user_id='yandexuid:9999', item_count=100).respond({"models": []})

    def test_empty_simple_better_price(self):
        query = self.make_query(yandexuid=9999, split=1, use_simple_better_price=True)
        response = self.report.request_json(query)
        self.assertFragmentIn(response, {"total": 0, "totalOffers": 0, "totalOffersBeforeFilters": 0, "totalModels": 0})

    def test_noconfig(self):
        """
        Проверка работоспособности при отсутствии конфига:
        если нет конфига, то 1) не падаем и 2) это равносильно тому, что все флаги false
        Конфиг может отсутствовать для сплита, а может быть просто пустым
        """
        simple_query = self.make_query()
        self.assertResponseIsEmpty(simple_query)
        self.assertEqualJsonResponses(simple_query, self.make_query(split="", yandexuid=None))
        self.assertEqualJsonResponses(simple_query, self.make_query(split=0))
        self.assertEqualJsonResponses(simple_query, self.make_query(split=4))

    def test_show_history(self):
        """
        Тест режима отображения всей истории: взведён только флаг show-history,
        и в выдаче есть все модели от ichwill, имеющие предложения, но не зависимо от
        нашей цены
        """
        self.assertModelsInResponse(query=self.make_query(split=1), ids=[1, 2, 3])

    def test_filter_by_price(self):
        """
        Тест режима отбора по цене: если в конфиге взведён флаг "filter-by-price",
        то в выдаче должны быть только модели, для которых существуют предложения дешевле,
        чем по информации из ichwill
        """
        self.assertOnlyModelsInResponse(query=self.make_query(split=2), ids=[1], all_ids=[1, 2, 3])

    def test_filter_by_assistant(self):
        """
        Тест режима отбора по порогу для данных советника: взведен флаг "filter-by-assistant"
        Если порог не задан, то считаем его 0.0, то есть пропускаем всё - равносильно "show-history"
        В выдаче присутствуют только модели из данных ichwill проходящие по порогу, для которых
        есть предложения на маркете
        """
        self.assertOnlyModelsInResponse(query=self.make_query(split=3), ids=[2, 3], all_ids=[1, 2, 3])

    def test_default_param_values(self):
        """
        Тестирование дефолтных значений в параметрах конфигурации:
        Флаги show-history, filter-by-price, filter-by-assistant по умолчанию - False, порог советника по умолчанию - 0
        """
        # Ответы при задании флагов с явными false равносильны случаям неявных false
        self.assertEqualJsonResponses(self.make_query(split=1), self.make_query(split=5))
        self.assertEqualJsonResponses(self.make_query(split=2), self.make_query(split=6))
        self.assertEqualJsonResponses(self.make_query(split=3), self.make_query(split=7))
        # Порог не задан для сплита 8, поэтому получаем все ихвилевые модели в выдаче:
        self.assertModelsInResponse(query=self.make_query(split=8), ids=[1, 2, 3])

    def test_paging(self):
        """
        Тестирование пэйджинга: должны учитываться параметры numdoc и paging
        """
        self.assertPagingSupportedForModels(base_query=self.make_query(split=1), ids=[1, 2, 3])

    @classmethod
    def prepare_ordering(cls):
        """
        Специальный респонс от ichwill с перестановкой
        """
        cls.index.models += [
            Model(hyperid=5),
        ]

        cls.index.offers += [Offer(fesh=1, hyperid=5, price=100)]

        yandexuid = "yandexuid:1111"
        cls.recommender.on_request_we_have_cheaper(user_id=yandexuid, item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 5, "price": 10.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 3, "price": 10.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 1, "price": 10.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 2, "price": 10.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )

    def test_ordering(self):
        """
        Тест сортировки результата: порядок ichwill должен быть сохранён
        """
        self.assertModelsBlockInResponse(
            query=self.make_query(split=1, yandexuid=1111), ids=[5, 3, 1, 2], preserve_order=True
        )

    @classmethod
    def prepare_price_threshold(cls):
        """
        Специальный конфиг для фильтра с относительным порогом цены
        и респонс от ichwill с подходящими ценами
        """
        cls.index.models += [
            Model(hyperid=21),
            Model(hyperid=22),
            Model(hyperid=23),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=21, price=95),
            Offer(fesh=1, hyperid=22, price=80),
            Offer(fesh=1, hyperid=23, price=90),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1112", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 21, "price": 100.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 22, "price": 100.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 23, "price": 100.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )

    def test_price_threshold(self):
        """
        Тест фильтрации по относительному порогу цены.
        В конфигурации задаём порог цены, как коэффициент для цен от ихвиля, ожидаем цены ниже этого порога
        """
        self.assertOnlyModelsInResponse(query=self.make_query(split=9, yandexuid=1112), ids=22, all_ids=[21, 22, 23])

    @staticmethod
    def offer_fragment(title):
        return {"titles": {"raw": title}}

    @classmethod
    def prepare_offers(cls):
        """
        Конфиг для Partition с офферным флагом и несколько офферов в индекс для тестирования офферной выдачи
        """

        cls.index.offers += [
            # for model not in history
            Offer(title="30", hyperid=30, fesh=1, cpa=Offer.CPA_REAL),
            # good offers
            Offer(title="31", hyperid=31, fesh=1, price=1100, cpa=Offer.CPA_REAL),
            Offer(title="32", hyperid=32, fesh=1, price=1000, cpa=Offer.CPA_REAL),
            Offer(title="33_1000", hyperid=33, fesh=1, price=1000, cpa=Offer.CPA_REAL),
            Offer(title="33_800", hyperid=33, fesh=1, price=800, cpa=Offer.CPA_REAL),
            # non-cpa
            Offer(title="34", hyperid=34, fesh=1, price=500, cpa=Offer.CPA_NO),
            # not from region
            Offer(title="35", hyperid=35, fesh=2, price=500, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1113", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 31, "price": 1500.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 32, "price": 900.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 33, "price": 900.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 34, "price": 1200.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 35, "price": 1200.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )

    def test_offers(self):
        """
        Тестирование офферной выдачи.
          * Параметр в конфигурации show-offers должен приводить к офферной выдаче
          * Для каждой модели выводится не более 1 оффера
          * Цена оффера подходит по критериям поиска: проверяем фильтрацию по цене,
          * Оффер должен быть из региона пользователя
          * Оффер должен быть CPA
        """
        response = self.report.request_json(self.make_query(split=10, yandexuid=1113))
        # в выдаче - офферы;
        #   только один офер для 33й модели
        #   нет оферов 30 модели (нет в истории), 32 модели (цена выше, чем в истории),
        #   34 модели (не CPA оффер), 35 модели (нет оферов в регионе),
        self.assertOnlyItemsIn(
            response,
            ids=["31", "33_800"],
            all_ids=["30", "31", "32", "33_1000", "33_800", "34", "35"],
            is_offer=None,
            item_factory=T.offer_fragment,
        )

    @classmethod
    def prepare_offer_price_threshold(cls):
        """
        Конфиг для фильтра с относительным порогом цены
        и респонс от ichwill с подходящими ценами на случай офферной выдачи
        """

        cls.index.offers += [
            Offer(hyperid=41, fesh=1, price=95, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(hyperid=42, fesh=1, price=80, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(hyperid=43, fesh=1, price=90, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1114", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 41, "price": 100.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 42, "price": 100.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 44, "price": 100.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )

    def test_offer_price_threshold(self):
        """
        Тест фильтрации по относительному порогу цены для офферной выдачи
        """
        self.assertOnlyOffersInResponse(
            query=self.make_query(split=11, yandexuid=1114), model_ids=42, all_model_ids=[41, 42, 43]
        )

    @classmethod
    def prepare_offer_paging(cls):
        """
        Дополнительные данные для тестирования пэйджинга офферной выдачи
        """

        cls.index.offers += [
            Offer(hyperid=51, fesh=1, price=100, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(hyperid=52, fesh=1, price=200, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(hyperid=53, fesh=1, price=300, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(hyperid=54, fesh=1, price=400, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1115", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 51, "price": 200.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 52, "price": 300.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 53, "price": 400.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 54, "price": 500.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )

    def test_offer_paging(self):
        """
        Тестирование пэйджинга для офферной выдачи
        """
        self.assertPagingSupportedForOffers(
            base_query=self.make_query(split=10, yandexuid=1115), model_ids=[51, 52, 53, 54]
        )

    @classmethod
    def prepare_default_offer(cls):
        """
        Данные для теста офферной выдачи, при работе в режиме default offer
        """

        # Магазины с разным рэйтингом
        cls.index.shops += [
            Shop(
                fesh=3,
                priority_region=213,
                regions=[225],
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
            Shop(
                fesh=4,
                priority_region=213,
                regions=[225],
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
            ),
        ]

        # На модель по несколько оферов, различающихся randx или рэйтингом магазна
        cls.index.offers += [
            Offer(fesh=3, hyperid=60, title="60_3_10", price=90, bid=1, cpa=Offer.CPA_REAL, randx=10),
            Offer(fesh=4, hyperid=60, title="60_4_11", price=100, bid=1, cpa=Offer.CPA_REAL, randx=11),
            Offer(fesh=4, hyperid=60, title="60_3_9", price=210, bid=1, cpa=Offer.CPA_REAL, randx=9),
            Offer(fesh=4, hyperid=61, title="61_4_1", price=110, bid=5, cpa=Offer.CPA_REAL, randx=1),
            Offer(fesh=4, hyperid=61, title="61_4_2", price=130, bid=10, cpa=Offer.CPA_REAL, randx=2),
            Offer(fesh=4, hyperid=61, title="61_4_3", price=140, bid=15, cpa=Offer.CPA_REAL, randx=3),
            Offer(fesh=4, hyperid=62, title="62_4_12", price=150, bid=1, cpa=Offer.CPA_REAL, randx=12),
        ]

        # еще один кейс для ichwill для моделей данного теста
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1116", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 60, "price": 200.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 61, "price": 200.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 62, "price": 100.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )

    @skip('Логика описанная в теста не валидна. По словам @yacoder - это всё должно быть выпилено')
    def test_default_offer(self):
        """
        1. Тест офферной выдачи, при работе в режиме default offer
        Проверяем, что для модели выбирается оффер из топа от ранжирования методом default offer:
          * Из имеющихся данных учитываются CPM, рэйтинг магазина, randx
          * Для модели #60 офер с лучшей ценой проигрывает оферу из лучшего магазина, а самый лучший
            default offer, отбрасывается, так как его цена не проходит фильтр (у нас дешевле),
          * Для модели #61 лучший оффер - 61_4_3 - по CPM
          * модель 62 также не проходит, так как нет офферов дешевле, чем в истории
        Проверяем, что в конфиге флаг use-default-offer по умолчанию взведен: запросы для сплитов
            10 и 12 дают один результат
        2. Тест работы без use-default-offer.
        Проверяем, что если use-default-offer выключен, то выигрывает самый дешёвый оффер.
            Например, для модели #61 это - 61_4_1, а для модели #60 - 60_3_10
        """

        # use-default-offer offer включен
        self.assertOnlyItemsInResponse(
            query=self.make_query(split=12, yandexuid=1116),
            ids=["60_4_11", "61_4_3"],
            all_ids=["60_3_10", "60_4_11", "60_3_9", "61_4_3", "61_4_2", "61_4_1", "62_4_12"],
            item_factory=T.offer_fragment,
        )

        # Проверяем, что конфигурации 10 и 12 равносильны, а значит, use-default-offer
        # по умолчанию установлен
        self.assertEqualJsonResponses(
            self.make_query(split=10, yandexuid=1116), self.make_query(split=12, yandexuid=1116)
        )

        # use-default-offer выключен
        self.assertOnlyItemsInResponse(
            query=self.make_query(split=13, yandexuid=1116),
            ids=["60_3_10", "61_4_1"],
            all_ids=["60_3_10", "60_4_11", "60_3_9", "61_4_3", "61_4_2", "61_4_1", "62_4_12"],
            item_factory=T.offer_fragment,
        )

    def test_access_log(self):
        """
        Тестирование total_renderable в access log
        Проверяется, что общее количество для показа = total
        """
        response = self.report.request_json(self.make_query(split=1))
        self.assertFragmentIn(response, {"search": {"total": 3}})
        self.access_log.expect(total_renderable="3").times(1)

    @classmethod
    def prepare_timestamp_filter(cls):
        """
        ichwill конфигурация для we_have_cheaper с разными timestamp и ценами
        и еще модели в индекс
        """
        cls.index.models += [
            Model(hyperid=70),
            Model(hyperid=71),
            Model(hyperid=72),
            Model(hyperid=73),
        ]

        cls.index.offers += [
            Offer(hyperid=71, price=500, fesh=1),
            Offer(hyperid=72, price=500, fesh=1),
            Offer(hyperid=73, price=500, fesh=1),
        ]

        long_ago = str(to_timestamp(datetime.now() - timedelta(days=3)))
        just_now = str(to_timestamp(datetime.now() - timedelta(days=1)))

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2001", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    # Старые записи с низкой ценой
                    {"model_id": 70, "price": 100.0, "success_requests_share": 0.1, "timestamp": long_ago},
                    # Старые записи с высокой ценой
                    {"model_id": 71, "price": 2500.0, "success_requests_share": 0.1, "timestamp": long_ago},
                    # Новые записи с низкой ценой
                    {"model_id": 72, "price": 100.0, "success_requests_share": 0.1, "timestamp": just_now},
                    # Новые записи с высокой ценой
                    {"model_id": 73, "price": 2500.0, "success_requests_share": 0.1, "timestamp": just_now},
                ]
            }
        )

    def test_timestamp_filter(self):
        """
        Проверка фильтрации по меткам времени в истории пользователя.
         * Если время жизни не указано в конфиге, то фильтр выключен
         * Если время жизни указано и равно 0, то фильтр выключен
         * Если время жизни указано и не 0, то из выдачи исключаются все позиции,
            относящиеся к устаревшим моделям в истории пользователя
        """
        # Фильтр не задан
        self.assertModelsInResponse(query=self.make_query(split=6, yandexuid=2001), ids=[71, 73])

        # Фильтр явно выключен
        self.assertEqualJsonResponses(
            self.make_query(split=6, yandexuid=2001), self.make_query(split=14, yandexuid=2001)
        )

        # Фильтр задан
        self.assertOnlyModelsInResponse(
            query=self.make_query(split=15, yandexuid=2001), ids=[73], all_ids=[70, 71, 72, 73]
        )

        # Фильтр по времени задан, но остальные отключены
        self.assertOnlyModelsInResponse(
            query=self.make_query(split=17, yandexuid=2001), ids=[72, 73], all_ids=[70, 71, 72, 73]
        )

    @classmethod
    def prepare_price_output(cls):
        """
        Модели с различными минимальными ценами: как выше цены в истории пользователя, так и ниже цены в истории.
        Аналогично - офферы
        """
        # для модельной выдачи
        cls.index.models += [
            Model(hyperid=81),
            Model(hyperid=82),
        ]

        # для офферной выдачи
        cls.index.offers += [
            Offer(fesh=1, hyperid=81, price=100),
            Offer(fesh=1, hyperid=82, price=500),
            Offer(fesh=1, hyperid=83, title="83", price=100, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(fesh=1, hyperid=84, title="84", price=500, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2002", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 81, "price": 300.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 82, "price": 300.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 83, "price": 300.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 84, "price": 300.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_price_output(self):
        """
        Проверка данных о разнице в цене "у нас дешевле" в модельной и оферной выдаче
         * Для конфигурации show-history проверяем, что для моделей, которые у нас не дешевле, этот блок есть, но нет
            поля разницы в цене
         * Проверяем блок для моделей
         * Проверяем блок для офферов
        Ожидаем сплиты в конфигурации:
         split=1 --> show-history
         split=16 --> show-offers & show-history
        """
        # Запрос всех моделей (show-history)
        response = self.report.request_json(self.make_query(split=1, yandexuid=2002))

        # поле priceDifference должно появиться только у дешёвых
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "type": "model",
                            "id": 81,
                            "prices": {
                                "betterPrice": {"theirPrice": "300", "ourPrice": "100", "priceDifference": "200"}
                            },
                        },
                        {
                            "type": "model",
                            "id": 82,
                            "prices": {
                                "betterPrice": {
                                    "theirPrice": "300",
                                    "ourPrice": "500",
                                    "priceDifference": NoKey("priceDifference"),
                                }
                            },
                        },
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )

        # Запрос всех оферов (show-offers & show-history)
        response = self.report.request_json(self.make_query(split=16, yandexuid=2002))
        # блок должен появиться только у дешёвых офферов
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "model": {"id": 83},
                            "prices": {
                                "betterPrice": {"theirPrice": "300", "ourPrice": "100", "priceDifference": "200"}
                            },
                        },
                        {
                            "entity": "offer",
                            "model": {"id": 84},
                            "prices": {
                                "betterPrice": {
                                    "theirPrice": "300",
                                    "ourPrice": "500",
                                    "priceDifference": NoKey("priceDifference"),
                                }
                            },
                        },
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )

    @classmethod
    def prepare_price_rounding(cls):
        """
        Цены, требующие округления при рендеринге
        """

        cls.index.models += [
            Model(hyperid=91),
            Model(hyperid=92),
        ]
        cls.index.offers += [
            Offer(fesh=1, hyperid=91, price=100.16),
            Offer(fesh=1, hyperid=92, price=100.96),
        ]
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2003", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 91, "price": 300.99, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 92, "price": 300.99, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_price_rounding(self):
        """
        Проверяем, что округлённая разность округлённых цен, оказавшаяся в выдаче, корректна
        """

        response = self.report.request_json(self.make_query(split=1, yandexuid=2003))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "type": "model",
                            "id": 91,
                            "prices": {
                                "betterPrice": {"theirPrice": "301", "ourPrice": "100", "priceDifference": "201"}
                            },
                        },
                        {
                            "type": "model",
                            "id": 92,
                            "prices": {
                                "betterPrice": {"theirPrice": "301", "ourPrice": "101", "priceDifference": "200"}
                            },
                        },
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )

    @classmethod
    def prepare_pictures(cls):
        """
        Офферы и модели без картинок
        """
        cls.index.models += [
            Model(hyperid=95, no_picture=True, no_add_picture=True),
            Model(hyperid=96, no_picture=False),
        ]
        cls.index.offers += [
            Offer(fesh=1, hyperid=95),
            Offer(fesh=1, hyperid=96),
            Offer(fesh=1, hyperid=97, price=100, no_picture=True, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(fesh=1, hyperid=98, price=100, no_picture=False, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2004", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 95, "price": 300, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 96, "price": 300, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 97, "price": 300, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 98, "price": 300, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_pictures(self):
        """
        Модели не попадают в выдачу, если у них нет изображений
        В сплите ожидается поиск моделей с фильтрацией по цене
        """
        self.assertModelsInResponse(query=self.make_query(split=6, yandexuid=2004), ids=[96])
        self.assertModelsNotInResponse(query=self.make_query(split=6, yandexuid=2004), ids=[95])

        """
        Офферы не попадают в выдачу, если у них нет изображений
        В сплите ожидается поиск оферов с фильтрацией по цене
        """
        self.assertOffersInResponse(query=self.make_query(split=10, yandexuid=2004), model_ids=[98])
        self.assertOffersNotInResponse(query=self.make_query(split=10, yandexuid=2004), model_ids=[97])

    @classmethod
    def prepare_price(cls):
        """
        Нулевая цена
        """
        cls.index.models += [
            Model(hyperid=99, no_picture=False),
        ]
        cls.index.offers += [
            Offer(fesh=1, hyperid=99, price=0, no_picture=False, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2005", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 99, "price": 300, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_price(self):
        """
        Модели и оферы с нулевой ценой не попадают в выдачу
        """
        self.assertModelsNotInResponse(query=self.make_query(split=6, yandexuid=2005), ids=[99])
        self.assertOffersNotInResponse(query=self.make_query(split=10, yandexuid=2005), model_ids=[99])

    @classmethod
    def prepare_history_duplicates(cls):
        """
        В истории для некоторой модели есть несколько записей с разной ценой
        """
        cls.index.models += [
            Model(hyperid=100),
            Model(hyperid=101),
        ]
        cls.index.offers += [
            Offer(fesh=1, hyperid=100, price=100),
            Offer(fesh=1, hyperid=101, price=100, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2006", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 100, "price": 300, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 101, "price": 300, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 100, "price": 200, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 101, "price": 250, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_history_duplicates(self):
        """
        Из всех вхождений модели в истории должна быть выбрана запись с наименьшей ценой
        Проверяем, что  theirPrice всегда соответствует наименьшей цене:
          * При фильтрации по цене (split=6)
          * С помощью советника (split=7)
          * Для объединённого режима filter-by-price + assistant (split=18)
          * Для офферов (split=10)
        """

        """
        Для моделей
        """
        for s in [6, 7, 18]:
            response = self.report.request_json(self.make_query(split=s, yandexuid=2006))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [
                            {
                                "type": "model",
                                "id": 100,
                                "prices": {
                                    "betterPrice": {"theirPrice": "200", "ourPrice": "100", "priceDifference": "100"}
                                },
                            },
                            {
                                "type": "model",
                                "id": 101,
                                "prices": {
                                    "betterPrice": {"theirPrice": "250", "ourPrice": "100", "priceDifference": "150"}
                                },
                            },
                        ],
                    }
                },
                preserve_order=False,
            )

        """
        Для офферов
        """
        response = self.report.request_json(self.make_query(split=10, yandexuid=2006))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "model": {"id": 101},
                            "prices": {
                                "betterPrice": {"theirPrice": "250", "ourPrice": "100", "priceDifference": "150"}
                            },
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_min_cpa_price_output(cls):
        """
        Модели, для которых есть cpa и не cpa оферы
        """

        cls.index.models += [
            Model(hyperid=110),
            Model(hyperid=111),
            Model(hyperid=112),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=110, price=300, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(
                waremd5="qtZDmKlp7DGGgA1BL6erMQ",
                fesh=1,
                hyperid=110,
                price=200,
                bid=1,
                cpa=Offer.CPA_REAL,
                override_cpa_check=True,
            ),
            Offer(fesh=1, hyperid=110, price=100, bid=1, cpa=Offer.CPA_NO, override_cpa_check=True),
            Offer(
                waremd5="91t1fTRZw-k-mN2re5A5OA",
                fesh=1,
                hyperid=111,
                price=100,
                bid=1,
                cpa=Offer.CPA_REAL,
                override_cpa_check=True,
            ),
            Offer(fesh=1, hyperid=111, price=200, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(fesh=1, hyperid=111, price=300, bid=1, cpa=Offer.CPA_NO, override_cpa_check=True),
            Offer(fesh=1, hyperid=112, price=90, bid=1, cpa=Offer.CPA_NO, override_cpa_check=True),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2007", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 110, "price": 500.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 111, "price": 500.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 112, "price": 500.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_min_cpa_price_output(self):
        """
        Проверяем наличие в модельной выдаче минимальной цены cpa офера
        """

        # Запрос всех моделей (show-history)
        response = self.report.request_json(self.make_query(split=1, yandexuid=2007))

        """
        Мин цена для модели 110 не у CPA-оффера -- в minCpaOfferPrice должна взяться цена, мин среди CPA-офферов,
        Для модели 111 минимальная цена CPA-офферов совпадает с минимальной ценой всех офферов
        """
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "type": "model",
                            "id": 110,
                            "prices": {
                                "betterPrice": {
                                    "minCpaOfferPrice": "200",
                                    "minCpaOfferWareMd5": "qtZDmKlp7DGGgA1BL6erMQ",
                                }
                            },
                        },
                        {
                            "type": "model",
                            "id": 111,
                            "prices": {
                                "betterPrice": {
                                    "minCpaOfferPrice": "100",
                                    "minCpaOfferWareMd5": "91t1fTRZw-k-mN2re5A5OA",
                                }
                            },
                        },
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )

        """
        Нет CPA-оффера -- нет minCpaOfferPrice.
        """
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "type": "model",
                            "id": 112,
                            "prices": {
                                "betterPrice": {
                                    "minCpaOfferPrice": "90",
                                }
                            },
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_price_priority(cls):
        """
        Модели и оферы, проходящие фильтр цены по истории пользователей, и не проходящие фильтр
        """
        cls.index.models += [
            Model(hyperid=120),
            Model(hyperid=121),
            Model(hyperid=122),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=120, price=300, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(fesh=1, hyperid=121, price=300, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(fesh=1, hyperid=122, price=300, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2008", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 120, "price": 200.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 121, "price": 500.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 122, "price": 200.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_price_priority(self):
        """
        Проверяем, что при отключенном фильтре по цене и включенном флаге cheaper-atop те модели и оферы,
        что дешевле соответствующих позиций в истории, имеют больший ранк в выдаче.
        Также проверяем выключенность флага по умолчанию
        Ожидаем сплиты в конфигурации:
         split=1 --> show-history
         split=16 --> show-offers && show-history
         split=19 --> show-history && ~cheaper-atop
         split=20 --> show-history && cheaper-atop
         split=21 --> show-offers && show-history && ~cheaper-atop
         split=22 --> show-offers && show-history && cheaper-atop
        """

        """
        Тест для моделей. Флаг выключен. Запрос всех моделей (show-history)
        """
        self.assertModelsBlockInResponse(
            query=self.make_query(split=19, yandexuid=2008), ids=[120, 121, 122], preserve_order=True
        )

        """
        Тест для моделей. Флаг выключен по умолчанию
        """
        self.assertEqualJsonResponses(
            self.make_query(split=1, yandexuid=2008), self.make_query(split=19, yandexuid=2008)
        )

        """
        Тест для моделей. Флаг включен Запрос всех моделей (show-history && cheaper-atop)
        """
        self.assertModelsBlockInResponse(
            query=self.make_query(split=20, yandexuid=2008), ids=[121, 120, 122], preserve_order=True
        )

        """
        Тест для офферов. Флаг выключен. Запрос всех офферов (show-history && show-offers)
        """
        self.assertOffersBlockInResponse(
            query=self.make_query(split=21, yandexuid=2008), model_ids=[120, 121, 122], preserve_order=True
        )

        """
        Тест для офферов. Флаг выключен по умолчанию
        """
        self.assertEqualJsonResponses(
            self.make_query(split=16, yandexuid=2008), self.make_query(split=21, yandexuid=2008)
        )

        """
        Тест для офферов. Флаг включен. Запрос всех офферов (show-history && show-offers)
        """
        self.assertOffersBlockInResponse(
            query=self.make_query(split=22, yandexuid=2008), model_ids=[121, 120, 122], preserve_order=True
        )

    @classmethod
    def prepare_price_precision(cls):
        """
        Модели и оферы, с достаточно большими ценами, равными ценам из истории
        """
        cls.index.models += [
            Model(hyperid=130),
            Model(hyperid=131),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=130, price=2499, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(fesh=1, hyperid=131, price=5776.8, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2009", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 130, "price": 2499.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 131, "price": 5777.01, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_price_precision(self):
        """
        Тест фикса ошибки точности при фильтрации по цене:
        Проверяем, что фильтр работает корректно, когда цены в истории и у офера равны и достаточно велики.
        Фильтр по цене не должен пропускать модели, которые у нас не дешевле
        Ожидаем сплиты в конфигурации:
         split=2 --> filter-by-price
        """
        self.assertOnlyModelsInResponse(query=self.make_query(split=2, yandexuid=2009), ids=[131], all_ids=[130, 131])

    @classmethod
    def prepare_vclusters(cls):
        """
        Модели и кластеры с офферами
        """
        cls.index.models += [
            Model(hyperid=140),
        ]
        cls.index.vclusters += [
            VCluster(
                vclusterid=1000000141,
                clothes_index=[ClothesIndex([179], [179], [179])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=10)])],
            ),
            VCluster(vclusterid=1000000142, no_pictures=True),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=140, price=100, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(fesh=1, vclusterid=1000000141, price=100, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(fesh=1, vclusterid=1000000142, price=100, bid=1, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2010", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 140, "price": 200, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 1000000141, "price": 200, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": 1000000142, "price": 200, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_vclusters(self):
        """
        Проверяем, что работает выдача визуальных кластеров и при этом
        отфильтровываются кластеры без картинок (вдруг такие найдутся)
        Ожидаем сплиты в конфигурации:
         split=2 --> filter-by-price
        """

        def create_product(hyperid):
            return simple_testcase.create_product(
                hyperid=hyperid, product_type="model" if hyperid < 10**6 else "cluster"
            )

        self.assertOnlyItemsInResponse(
            query=self.make_query(split=2, yandexuid=2010),
            ids=[140, 1000000141],
            all_ids=[140, 1000000141, 1000000142],
            item_factory=create_product,
        )

    def test_show_log(self):
        """
        Проверка поля url_hash в show log
        """
        self.report.request_json(self.make_query(split=1))
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    @classmethod
    def prepare_price_conformance(cls):
        """
        Модель, оффер и неактуальная региональная статистика
        """
        cls.index.models += [
            Model(hyperid=160),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=160, price_min=100, rids=[213]),
        ]

        cls.index.offers += [
            # default offer
            Offer(
                waremd5="BH8EPLtKmdLQhLUasgaOnA",
                fesh=1,
                hyperid=160,
                price=120,
                cpa=Offer.CPA_REAL,
                override_cpa_check=True,
            ),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:2160", item_count=100).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 160, "price": 200, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

    def test_price_conformance(self):
        """
        Цена в better_price должна соответствовать цене дефолтного оффера
        Ожидаем в сплите 2 конфиг для режима фильтрации по цене
        """

        """
        1. Региональная статистика содержит устаревшую информацию о цене модели 100,
        но в индексе есть оффер ценой 120. Ожидаем, цену 120 в поле betterPrice
        """
        response = self.report.request_json(self.make_query(split=2, yandexuid=2160))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "type": "model",
                            "id": 160,
                            "prices": {
                                "betterPrice": {
                                    "theirPrice": "200",
                                    "ourPrice": "120",
                                }
                            },
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "BH8EPLtKmdLQhLUasgaOnA",
                                    }
                                ]
                            },
                        },
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )


if __name__ == '__main__':
    main()

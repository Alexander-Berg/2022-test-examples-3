#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip

from core.types import (
    BlueOffer,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    ModelDescriptionTemplates,
    Offer,
    RegionalModel,
    Shop,
    YamarecMatchingPartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import main
from core.matcher import ElementCount, Absent
from simple_testcase import SimpleTestCase

from core.report import DefaultFlags
from core.bigb import SkuPurchaseEvent, BeruSkuOrderCountCounter
from market.proto.recom.exported_dj_user_profile_pb2 import (
    TEcomVersionedDjUserProfile,
    TVersionedProfileData,
    TFashionDataV1,
    TFashionSizeDataV1,
)  # noqa pylint: disable=import-error

YANDEX_PHONE_ID = 177547282


class T(SimpleTestCase):
    """
    Набобр тестов для place=product_accessories
    """

    UNKNOWN_MODEL_ID = 901
    INVALID_ID = 2**64 - 1

    @classmethod
    def prepare(cls):
        cls.settings.rgb_blue_is_cpa = True

        cls.index.models += [
            Model(hyperid=1, hid=101, title='Model without accessories', accessories=[]),
            Model(hyperid=2, hid=101, title='Simple model', accessories=[11, 16]),
            Model(hyperid=3, hid=101, title='Other model', accessories=[11, 12]),
            Model(hyperid=4, hid=101, title='Super model', accessories=[12, 11, 13]),
            Model(hyperid=5, hid=101, title='Ultra model', accessories=[11, 12, 13, 14]),
            Model(hyperid=6, hid=101, title='Ultra model', accessories=[14, 15]),
            Model(hyperid=7, hid=101, title='Model with purchase history', accessories=[17]),
            Model(hyperid=11, hid=102, title='Accessory for model #2', accessories=[]),
            Model(hyperid=12, hid=103, title='Accessory of category#103', accessories=[]),
            Model(hyperid=13, hid=104, title='Accessory of category#104', accessories=[]),
            Model(hyperid=14, hid=104, title='Accessory#14', accessories=[]),
            Model(hyperid=15, hid=104, title='Foreign accessory', accessories=[]),
            Model(hyperid=16, hid=102, title='Accessory for model #2 not on stock', accessories=[]),
            Model(hyperid=17, hid=102, title='Accessory with purchase history', accessories=[]),
            # Модели 18, 19, 20, 21 используются в тесте test_clothes_boosting_from_dj_profile
        ]

        cls.index.shops += [
            Shop(fesh=1, regions=[1001]),
            Shop(fesh=2, regions=[1009]),
            Shop(fesh=13, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=16, offers=9, rids=[1001]),
            RegionalModel(hyperid=17, offers=9, rids=[1001]),
        ]

        model_ids = list(range(1, 17))
        shop_ids = [1] * 14 + [2, 2]
        model_shops = dict(zip(model_ids, shop_ids))

        cls.index.offers += [Offer(fesh=model_shops[hyperid], hyperid=hyperid) for hyperid in range(1, 17)]

        cls.index.offers += [
            Offer(fesh=1, hyperid=17, sku=171, price=171, title="MSKU-171"),  # эту пользователь ранее не покупал
            Offer(fesh=1, hyperid=17, sku=170, price=170, title="MSKU-170"),  # а эту покупал
        ]

        cls.index.mskus += [
            MarketSku(
                hid=102,
                hyperid=17,
                sku=170,
                title="MSKU-170",
                blue_offers=[BlueOffer(ts=10, price=170, fesh=1)],
            ),
            MarketSku(
                hid=102,
                hyperid=17,
                sku=171,
                title="MSKU-171",
                blue_offers=[BlueOffer(ts=10, price=171, fesh=1)],
            ),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{'split': '1'}]),
                    YamarecSettingPartition(params={'version': '2'}, splits=[{'split': '2'}]),
                    YamarecSettingPartition(params={'version': '3'}, splits=[{'split': '3'}]),
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '0',
                            'use-local': '1',
                        },
                        splits=[{'split': '4'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': '3',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{'split': '5'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '0',
                            'use-local': '0',
                        },
                        splits=[{'split': '6'}],
                    ),
                    # blue market test
                    YamarecSettingPartition(
                        params={
                            'version': '10',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{'split': '25'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': '10',
                            'use-external': '0',
                            'use-local': '1',
                        },
                        splits=[{'split': '26'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': 'MODEL/MSKUv1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{'split': 'model_with_msku'}],
                    ),
                ],
            )
        ]

        cls.recommender.on_request_accessory_models(model_id=4, item_count=1000, version='1').respond(
            {'models': ['11', '12', '13']}
        )

        cls.recommender.on_request_accessory_models(model_id=4, item_count=1000, version='2').respond(
            {'models': ['13', '12', '11']}
        )

        cls.recommender.on_request_accessory_models(model_id=4, item_count=1000, version='3').respond({'models': []})
        # cls.recommender.on_request_accessory_models для версий 4,5,6 не должно быть, их отсутствие
        # используется в test_external_request

        cls.recommender.on_request_accessory_models(model_id=7, item_count=1000, version='7').respond(
            {'models': ['17']}
        )

        # for test_external_request
        cls.bigb.on_default_request().respond(counters=[])

    def test_empty_model_id(self):
        """
        Если пришёл пустой id
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_unknown_model(self):
        """
        Тест для несуществующей модели
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid={id}&rearr-factors=market_disable_product_accessories=0'.format(
                id=T.UNKNOWN_MODEL_ID
            ),
            add_defaults=DefaultFlags.STANDARD,
        )

        self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_invalid_id(self):
        """
        Крэш-тест для невалидного hyperid
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid={id}&rearr-factors=market_disable_product_accessories=0'.format(
                id=T.INVALID_ID
            ),
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_empty(self):
        """
        Тест с пустым результатом для существующей модели
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=1&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_simple(self):
        """
        Проверка наличия в выдаче аксессуара для указанной в запросе модели
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=3&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response, {"search": {"total": 2, "results": [{"type": "model", "id": 11}, {"type": "model", "id": 12}]}}
        )

    def test_enable_product_accessories(self):
        """
        Проверка наличия в выдаче аксессуара для указанной в запросе модели.
        Аксессуары отключены флагом market_disable_product_accessories, но
        список запрещённых pp не пуст и не содержит pp запроса
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=3&pp=1048'
            '&rearr-factors=market_disable_product_accessories=1;market_disabled_placements_for_product_accessories=1042,1642',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response, {"search": {"total": 2, "results": [{"type": "model", "id": 11}, {"type": "model", "id": 12}]}}
        )

    def test_region_filter(self):
        """
        Тест фильтра по региону:
        У модели есть аксессуары с предложениями в разных регионах.
        Должны выводиться только модели, имеющиеся в наличии в запрошенном регионе
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=6&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 1, "results": [{"type": "model", "id": 14}]}})

    def test_paging(self):
        """
        Тестирование пэйджинга по некоторой выдаче длиной в 4
        1. Одна первая страница покрывает всё
        2. Одна первая страница и ей не хватает данных
        3. Одна последняя страница и ей не хватает данных
        4. Страница в середине коллекции
        5. Последняя полная страница
        """

        # !FIXME: copied-pasted from report/lite/test_recommendations.py:662

        total4_query = (
            'place=product_accessories&rids=1001&hyperid=5&rearr-factors=market_disable_product_accessories=0'
        )
        result_collection = [
            {'id': 11},
            {'id': 12},
            {'id': 13},
            {'id': 14},
        ]

        # numdoc=total
        response = self.report.request_json(total4_query + '&numdoc=4&page=1', add_defaults=DefaultFlags.STANDARD)
        self.assertFragmentIn(
            response,
            {"search": {"total": 4, "results": result_collection}},
            allow_different_len=False,
            preserve_order=False,
        )
        # page out of range
        response = self.report.request_json(total4_query + '&numdoc=5&page=1', add_defaults=DefaultFlags.STANDARD)
        self.assertFragmentIn(
            response,
            {"search": {"total": 4, "results": result_collection}},
            allow_different_len=False,
            preserve_order=False,
        )
        # incomplete
        response = self.report.request_json(total4_query + '&numdoc=3&page=2', add_defaults=DefaultFlags.STANDARD)
        self.assertFragmentIn(
            response,
            {"search": {"total": 4, "results": result_collection[3:]}},
            allow_different_len=False,
            preserve_order=False,
        )
        # regular
        response = self.report.request_json(total4_query + '&numdoc=1&page=2', add_defaults=DefaultFlags.STANDARD)
        self.assertFragmentIn(
            response,
            {"search": {"total": 4, "results": result_collection[1:2]}},
            allow_different_len=False,
            preserve_order=False,
        )
        # last
        response = self.report.request_json(total4_query + '&numdoc=2&page=2', add_defaults=DefaultFlags.STANDARD)
        self.assertFragmentIn(
            response,
            {"search": {"total": 4, "results": result_collection[2:]}},
            allow_different_len=False,
            preserve_order=False,
        )

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='4').times(5)

    def test_hid_filter(self):
        """
        Тест фильтра по категории: пустой идентификатор
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=3&hid=&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 2, "results": [{"id": 11}, {"id": 12}]}})

    def test_category_filter(self):
        """
        Проверка наличия в выдаче аксессуара для указанной в запросе модели,
        отвечающего условию фильтрации по категории
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=3&hid=103&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 1, "results": [{"id": 12}]}})

    def test_category_filter_exclude(self):
        """
        Тест исключающего фильтра.
        * Одиночный фильтр. Ожидается, что по сравнению с исходным запросом добавление "-103"
        исключает один элемент выдачи категории #103 и не исключает для #102
        * Фильтр из двух категорий
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=3&hid=-103&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 1, "results": [{"id": 11}]}})
        self.assertFragmentNotIn(response, [{"id": 12}])

        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&hid=-102,-103&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {"search": {"total": 1, "results": [{"id": 13}]}})
        self.assertFragmentNotIn(response, [{"id": 11}])
        self.assertFragmentNotIn(response, [{"id": 12}])

    def test_ranging(self):
        """
        Тест ранжирования: порядок в выдаче соответствует порядку в данных по формуле
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response, {'search': {'total': 3, "results": [{'id': 12}, {'id': 11}, {'id': 13}]}}, preserve_order=True
        )

    def test_logging(self):
        """
        Проверка записи в стандартном логе показа модели
        """
        _ = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=2&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.show_log_tskv.expect(hyper_id=11, title='Accessory for model #2')

    def test_position(self):
        _ = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&page=1&numdoc=2&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.show_log.expect(shop_id=99999999, position=0)
        self.show_log.expect(shop_id=99999999, position=1)
        _ = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&page=2&numdoc=2&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.show_log.expect(shop_id=99999999, position=2)

    def test_contents(self):
        """
        Проверка выводимых для модели данных
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=2&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Accessory for model #2"},
                "categories": [{"entity": "category", "id": 102}],
                "type": "model",
                "id": 11,
                "offers": {"count": 1},
            },
        )

    def test_disabled_accessories(self):
        """
        Проверка что по умолчанию аксессуары отключены
        """
        self.assertResponseIsEmpty(query='place=product_accessories&rids=1001&hyperid=2')

    def test_external_request(self):
        """
        Тест для аксессуаров из IchWill. Хождение в IchWill настроено для пользователей,
        yandexuid которых заканчивается на 1,2 или 3 с версиями аксессуаров 1,2 и 3 соответственно
            -сделаем запрос без пользователя: ichwill не настроен, аксессуары возьмутся из памяти репорта(12,11,13)
            -сделаем запрос с yandexuid=001: сходим в ichwill c параметрами model_id=4, item_count=1000, version=1
             и получим аксессуары: 11, 12, 13. Проверим правильный порядок
            -сделаем запрос с yandexuid=002: сходим в ichwill c параметрами model_id=4, item_count=1000, version=2
             и получим аксессуары: 13, 12, 11. Проверим правильный порядок
            -сделаем запрос с yandexuid=003: сходим в ichwill c параметрами model_id=4, item_count=1000, version=3
             и получим пустой список аксессуаров.
             Проверим что произойдет откат на аксессуары из памяти репорта(12,11,13)
            -сделаем запрос с yandexuid=004: не пойдем в ichwill c параметрами model_id=4, item_count=1000, version=1
             хотя могли получить 11, 12, 13, откатимся на аксессуары из памяти(12,11,13)
            -сделаем запрос с yandexuid=005: пойдем в ichwill c параметрами model_id=4, item_count=1000, version=3
             не найдем аксессуары в ichwill и не откатимся на аксессуары из памяти(12,11,13), получим пустой список
            -сделаем запрос с yandexuid=005: не пойдем в ichwill c параметрами model_id=4, item_count=1000, version=1
             хотя могли получить 11, 12, 13, и не откатимся на аксессуары из памяти(12,11,13), получим пустой список
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response, {'search': {'total': 3, "results": [{'id': 12}, {'id': 11}, {'id': 13}]}}, preserve_order=True
        )

        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&yandexuid=001&rearr-factors=split=1;market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response, {'search': {'total': 3, "results": [{'id': 11}, {'id': 12}, {'id': 13}]}}, preserve_order=True
        )

        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&yandexuid=002&rearr-factors=split=2;market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response, {'search': {'total': 3, "results": [{'id': 13}, {'id': 12}, {'id': 11}]}}, preserve_order=True
        )

        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&yandexuid=003&rearr-factors=split=3;market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response, {'search': {'total': 3, "results": [{'id': 12}, {'id': 11}, {'id': 13}]}}, preserve_order=True
        )

        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&yandexuid=004&rearr-factors=split=4;market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(
            response, {'search': {'total': 3, "results": [{'id': 12}, {'id': 11}, {'id': 13}]}}, preserve_order=True
        )

        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&yandexuid=005&rearr-factors=split=5;market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {'total': 0})

        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=4&yandexuid=006&rearr-factors=split=6;market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )
        self.assertFragmentIn(response, {'total': 0})

    @classmethod
    def prepare_test_model_descriptions_existance(cls):
        """
        Создаем категорию с простыми шаблонами описания
        Создаем минимальную конфигурацию для поиска аксессуара (id=20001) по модели (id=20000)
        """

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=222,
                micromodel="{Type}",
                friendlymodel=["{Type}"],
                model=[("Технические характеристики", {"Тип": "{Type}"})],
                seo="{return $Type; #exec}",
            )
        ]

        cls.index.gltypes += [GLType(hid=222, param_id=2000, name=u"Тип", xslname="Type", gltype=GLType.STRING)]

        cls.index.models += [
            Model(hid=222, hyperid=20000, accessories=[20001]),
            Model(hid=222, hyperid=20001, glparams=[GLParam(param_id=2000, string_value="наушники")], accessories=[]),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=20001, offers=123, rids=[1001]),
        ]
        cls.index.offers += [Offer(fesh=1, hyperid=hyperid) for hyperid in [20000, 20001]]

    def test_model_descriptions_existance(self):
        # Ищем аксессуар и проверяем, то на place=product_accessories работают все виды характеристик модели для аксессуара
        response = self.report.request_json(
            'place=product_accessories&hyperid=20000&rids=1001&bsformat=2&show-models-specs=full,friendly&rearr-factors=market_disable_product_accessories=0'
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

    def test_multiquery(self):
        """
        Проверка запроса аксессуаров длял нескольких моделей.
        MARKETOUT-15082: Должны выдаваться аксессуары первой модели
        Рассчитываем на данные из prepare
        """
        self.assertOnlyModelsInResponse(
            query="place=product_accessories&rids=1001&hyperid=5,1,2,3,4,6,7&rearr-factors=market_disable_product_accessories=0",
            ids=list(range(11, 15)),
            all_ids=list(range(1, 16)),
            preserve_order=True,
        )

    def test_duplicates(self):
        """
        Проверка отсутствия в выдче моделей с ид из входного набора
        Рассчитываем на данные из prepare
        """
        self.assertModelsNotInResponse(
            query="place=product_accessories&rids=1001&hyperid=5,11,14&rearr-factors=market_disable_product_accessories=0",
            ids=[5, 11, 14],
        )

    def test_show_log(self):
        """
        Проверка поля url_hash в show log
        """
        self.report.request_json(
            "place=product_accessories&rids=1001&hyperid=2&rearr-factors=market_disable_product_accessories=0",
            add_defaults=DefaultFlags.STANDARD,
        )
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    @classmethod
    def prepare_blue_market(cls):
        """
        Конфигурация yamarec для blue маркет и ichwill для ещё двух версий,
        а также ещё модели для выдачи одной новой версии ichwill для blue market
        """
        cls.index.models += [
            Model(hyperid=91, hid=101, accessories=[]),
            Model(hyperid=92, hid=101, accessories=[]),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '11',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{'split': '25'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': '11',
                            'use-external': '0',
                            'use-local': '1',
                        },
                        splits=[{'split': '26'}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.MODEL_CARD_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.MATCHING,
                partitions=[
                    YamarecMatchingPartition(
                        name='accessory_blue_market_matching', matching={4: [92, 91]}, splits=['*']
                    )
                ],
            ),
        ]

        cls.recommender.on_request_accessory_models(model_id=4, item_count=1000, version='10').respond(
            {'models': ['11', '12', '13']}
        )

        # blue market
        cls.recommender.on_request_accessory_models(model_id=4, item_count=1000, version='11').respond(
            {'models': ['91', '92']}
        )

    def test_blue_market(self):
        """
        Тест разделения конфигурации для синего и зеленого маркета
        Проверяем, что выбор product_accessories/product_accessories_blue переключает на конфигурацию синего маркета
        Так же проверяем, что фильтр cpa=real переключает на конфигурацию синего маркета
        """

        all_model_ids = list(range(1, 16)) + [91, 92]

        """
        Проверяем, что если определены сплиты для синего и зеленого маркета и названия сплитов совпадают, то
            для use-external выбирается соответствующий плэйсу (*_blue или нет) вызов к ichwill,
        * rearr-flags=split=25
        Зеленый сплит с use-external=1, use-local=0, version=10
        Синий сплит с use-external=1, use-local=0, version=11
        ichwill для version=10 и отличающийся - для версии 11
        """
        self.assertOnlyModelsInResponse(
            query='place=product_accessories&hyperid=4&rids=1001&rearr-factors=split=25;market_disable_product_accessories=0',
            ids=[11, 12, 13],
            all_ids=all_model_ids,
            preserve_order=True,
        )

        # в индексе нет синих офферов
        self.assertResponseIsEmpty(
            query='place=product_accessories_blue&rgb=blue&hyperid=4&rids=1001&rearr-factors=split=25;market_disable_product_accessories=0'
        )
        self.assertResponseIsEmpty(
            query='place=product_accessories&cpa=real&hyperid=4&rids=1001&rearr-factors=split=25;market_disable_product_accessories=0'
        )

        """
        Проверяем, что если определены сплиты для синего и зеленого маркета и названия сплитов совпадают, то
            для use-local выбирается соответствующий плэйсу (*_blue или нет) локальный файл
        * rearr-flags=split=26
        Зеленый сплит (blue-market=0) с use-external=0, use-local=1
        Синий сплит (blue-market=1) с use-external=0, use-local=1
        """
        self.assertOnlyModelsInResponse(
            query='place=product_accessories&hyperid=4&rids=1001&rearr-factors=split=26;market_disable_product_accessories=0',
            ids=[12, 11, 13],
            all_ids=all_model_ids,
            preserve_order=True,
        )

        # в индексе нет синих офферов
        self.assertResponseIsEmpty(
            query='place=product_accessories_blue&rgb=blue&hyperid=4&rids=1001&rearr-factors=split=26;market_disable_product_accessories=0'
        )
        self.assertResponseIsEmpty(
            query='place=product_accessories&cpa=real&hyperid=4&rids=1001&rearr-factors=split=26;market_disable_product_accessories=0'
        )

    def test_rgb(self):
        """
        Плэйс предназначен для rgb=green маркета, но должен вызываться и работать и для rgb=green_with_blue
        """
        for market_type in ['', 'green', 'green_with_blue']:
            response = self.report.request_json(
                'place=product_accessories&rids=1001&hyperid=4&rgb={}&rearr-factors=market_disable_product_accessories=0'.format(
                    market_type
                ),
                add_defaults=DefaultFlags.STANDARD,
            )
            self.assertFragmentIn(
                response, {'search': {'total': 3, "results": [{'id': 12}, {'id': 11}, {'id': 13}]}}, preserve_order=True
            )

    @classmethod
    def prepare_yandex_phone_accessories(cls):
        cls.index.models += [
            Model(hyperid=204461147, hid=2184400, title='Чехол силиконовый'),
            Model(hyperid=204463104, hid=2184400, title='Чехол текстильный'),
            Model(hyperid=216962023, hid=2184400, title='Защитное стекло'),
            Model(hyperid=1971204201, hid=2184400, title='Яндекс.Станция'),
            Model(hyperid=216949030, hid=2184400, title='Защитная пленка'),
            Model(hyperid=59836721, hid=2184400, title='Наушники Marshall'),
            Model(hyperid=1728840311, hid=2184400, title='Часы Polar'),
        ]

        cls.index.offers += [
            Offer(hyperid=204461147),
            Offer(hyperid=204463104),
            Offer(hyperid=216962023),
            Offer(hyperid=1971204201),
            Offer(hyperid=216949030),
            Offer(hyperid=59836721),
            Offer(hyperid=1728840311),
        ]

    def test_yandex_phone_accessories(self):
        """
        Проверяем, что для Яндекс.Телефона возвращаем захардкоженный список результатов
        """
        for market_type in ['', 'green', 'green_with_blue']:
            response = self.report.request_json(
                'place=product_accessories&numdoc=10&hyperid={}&rgb={}&rearr-factors=market_disable_product_accessories=0'.format(
                    YANDEX_PHONE_ID, market_type
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 7,
                        'results': [
                            {"titles": {"raw": "Защитная пленка"}},
                            {"titles": {"raw": "Часы Polar"}},
                            {"titles": {"raw": "Наушники Marshall"}},
                            {"titles": {"raw": "Защитное стекло"}},
                            {"titles": {"raw": "Чехол текстильный"}},
                            {"titles": {"raw": "Яндекс.Станция"}},
                            {"titles": {"raw": "Чехол силиконовый"}},
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_filter_model_not_on_stock(self):
        """
        Плейс product_accessories всегда фильтрует модели без дефолтного оффера.
        В том числе и товары не в наличии.
        """
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=2&rearr-factors=market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'id': 11,
                    'offers': {
                        'count': 1,
                    },
                },
            ],
            allow_different_len=False,
        )

    @classmethod
    def prepare_sku_enrichment_from_bigb(cls):
        # Строим счетчик бигб
        sku_purchases = [
            SkuPurchaseEvent(sku_id=70, count=1),
            SkuPurchaseEvent(sku_id=170, count=1),
        ]
        sku_purchases_counter = BeruSkuOrderCountCounter(sku_purchases)
        cls.bigb.on_request(yandexuid='007', client='merch-machine').respond(counters=[sku_purchases_counter])

    def test_sku_enrichment_from_bigb(self):
        """
        Если пользователь покупал какую-то конкретную ску данной модели, то в выводе плейса будет предложена именно
        эта ску. Данные берутся из бигб
        Для этого делаем запрос с yandexuid=007, чтобы было на что счетчик возвращать
        """

        # Запрос за моделью, у которых пользователь ничего не покупал
        response = self.report.request_json(
            'place=product_accessories&rids=1001&hyperid=2&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "type": "model",
                            "id": 11,
                            "offers": {"items": [{"marketSku": Absent(), "sku": Absent(), "titles": {"raw": ""}}]},
                            "titles": {"raw": "Accessory for model #2"},
                        }
                    ],
                }
            },
        )

        # Запрос за моделью, у которой пользователь что-то покупал ранее
        # В выводе ожидаем, что подберется именно правильная ску
        response = self.report.request_json(
            'place=product_accessories&rids=1001&yandexuid=007&hyperid=7&rearr-factors=market_disable_product_accessories=0',
            add_defaults=DefaultFlags.STANDARD,
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "type": "model",
                            "id": 17,
                            "offers": {"items": [{"marketSku": "170", "sku": "170", "titles": {"raw": "MSKU-170"}}]},
                            "titles": {"raw": "Accessory with purchase history"},
                        }
                    ],
                }
            },
        )
        self.assertFragmentNotIn(response, {"sku": "171"})

    @classmethod
    def prepare_test_recom_response_with_msku(cls):
        cls.recommender.on_request_accessory_models_with_msku(
            model_id=1, item_count=1000, version='MODEL/MSKUv1'
        ).respond({'models': ['17/171']})

    def test_recom_response_with_msku(self):
        response = self.report.request_json(
            'place=product_accessories&hyperid=1'
            '&rids=1001'  # accessory model (id = 17) is regional model
            '&rearr-factors=split=model_with_msku;market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "id": 17,
                            "offers": {"items": [{"marketSku": "171", "sku": "171", "titles": {"raw": "MSKU-171"}}]},
                        }
                    ],
                }
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_clothes_boosting_from_dj_profile(cls):
        cls.settings.set_default_reqid = False

        ClothesSizeParamId = 26417130
        ClothesSizeParamValues = [
            (27016810, 'XS'),
            (27016830, 'S'),
            (27016891, 'M'),
            (27016892, 'L'),
            (27016910, 'XL'),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
                children=[
                    HyperCategory(
                        hid=7811873,
                        name='Женская одежда',
                        children=[
                            HyperCategory(hid=7811945, name='Женские платья'),
                        ],
                    ),
                ],
            ),
            HyperCategory(hid=11111, name='Какая-то категория не одежды'),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=ClothesSizeParamId,
                hid=7811945,
                gltype=GLType.ENUM,
                xslname='size_clothes_new',
                values=[
                    GLValue(value_id=value_id, text=param_name) for (value_id, param_name) in ClothesSizeParamValues
                ],
            ),
            GLType(
                param_id=ClothesSizeParamId,
                hid=7811873,
                gltype=GLType.ENUM,
                xslname='size_clothes_new',
                values=[
                    GLValue(value_id=value_id, text=param_name) for (value_id, param_name) in ClothesSizeParamValues
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=18, hid=7812186, title='Куртка мужская Adidas', accessories=[19]),
            Model(hyperid=19, hid=7811945, title='Платье женское Baon'),
            Model(hyperid=20, hid=11111, title='Модель не одежды', accessories=[21]),
            Model(hyperid=21, hid=11111, title='Модель не одежды 2'),
        ]

        for seq, (value_id, param_name) in enumerate(ClothesSizeParamValues):
            cls.index.offers += [
                Offer(
                    hid=7811945,
                    hyperid=19,
                    price=100 - seq * 10,
                    fesh=1,
                    title='Платье женское Baon размер ' + param_name,
                    glparams=[
                        GLParam(param_id=ClothesSizeParamId, value=value_id),
                    ],
                    ts=4423750 + seq,
                    randx=seq,
                ),
            ]

        for seq in range(15, 20):
            cls.index.offers += [
                Offer(
                    hid=11111,
                    hyperid=21,
                    price=100,
                    fesh=1,
                    title='Не одежда (hyperid = 21) ' + str(seq),
                    glparams=[
                        GLParam(param_id=15, value=30 - seq),
                    ],
                    ts=4423850 + seq,
                    randx=seq,
                ),
            ]

        for seq in range(0, 50):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4423750 + seq).respond(0.5 - seq * 0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4423850 + seq).respond(0.5 - seq * 0.01)

        cls.recommender.on_request_accessory_models(model_id=18, item_count=1000, version='1').respond(
            {'models': ['19']}
        )
        cls.recommender.on_request_accessory_models(model_id=20, item_count=1000, version='1').respond(
            {'models': ['21']}
        )

        # настраиваем Dj, чтобы возвращал нам профиль для пользователя с yandexuid = 011
        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                FashionV1=TFashionDataV1(
                    SizeClothesFemale=TFashionSizeDataV1(
                        Sizes={"46": 0.288602501, "48": 0.355698764, "S": 0.395698764, "M": 0.288602501}
                    ),
                )
            )
        )

        cls.dj.on_request(yandexuid='011', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )
        cls.bigb.on_request(yandexuid='011', client='merch-machine').respond(counters=[])

    @skip('deleted old booster')
    def test_clothes_boosting_from_dj_profile(self):
        """ """

        # если одежда и не выставлены флаги, то ничего не бустим. Побеждает XS, потому что у него значение матрикснет меньше
        for rearr in ('', ';fetch_recom_profile_for_model_place=0'):
            response = self.report.request_json(
                'place=product_accessories&rids=1001&hyperid=18&yandexuid=011&rearr-factors=market_disable_product_accessories=0{}'.format(
                    rearr
                ),
                add_defaults=DefaultFlags.STANDARD,
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "id": 19,
                                "offers": {
                                    "items": [
                                        {
                                            "titles": {"raw": "Платье женское Baon размер XS"},
                                        }
                                    ]
                                },
                            }
                        ]
                    }
                },
            )

        # если одежда и выставлены флаги, то побеждает оффер размера S, потому что его бустят
        # (потому что он указан в еком профиле)
        rearr_factors = [
            'market_boost_single_personal_gl_param_coeff=1.2',
            'fetch_recom_profile_for_model_place=1',
        ]
        request = 'place=product_accessories&rids=1001&hyperid=18&yandexuid=011&rearr-factors=market_disable_product_accessories=0;{}'.format(
            ';'.join(rearr_factors)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 19,
                            "offers": {
                                "items": [
                                    {
                                        "titles": {"raw": "Платье женское Baon размер S"},
                                    }
                                ]
                            },
                        }
                    ]
                }
            },
        )

        # если не одежда - бустинга не происходит, что с флагами, что без
        for rearr in (
            "",
            "&rearr-factors=market_boost_single_personal_gl_param_coeff=1.2;fetch_recom_profile_for_model_place=1",
            "&rearr-factors=market_boost_single_personal_gl_param_coeff=1.2;fetch_recom_profile_for_model_place=0",
        ):
            response = self.report.request_json(
                'place=product_accessories&rids=1001&hyperid=20&yandexuid=011&rearr-factors=market_disable_product_accessories=0{}'.format(
                    rearr
                )
            )
            self.assertFragmentIn(response, {"titles": {"raw": "Не одежда (hyperid = 21) 15"}})


if __name__ == '__main__':
    main()

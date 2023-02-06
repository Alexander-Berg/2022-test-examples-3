#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import itertools
from core.types import (
    ClothesIndex,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Picture,
    PictureSignature,
    RegionalModel,
    Shop,
    VCluster,
    YamarecFeaturePartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty
from core.dj import DjModel


class T(TestCase):
    """
    Тестирование gl-фильтров в выдаче некоторых методов
    """

    TYPES = set()

    @classmethod
    def prepare(cls):
        """
        Общие данные для тестирования gl-фильтров
        """
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls._add_gl_model(101)

        cls.index.hypertree += [
            HyperCategory(hid=201, output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops.append(Shop(fesh=1, regions=[1001]))

    @classmethod
    def _add_glparams(cls, hid, is_offer=False):
        if hid not in cls.TYPES:
            cls.index.gltypes += [
                GLType(param_id=501, hid=hid, gltype=GLType.ENUM),
                GLType(param_id=502, hid=hid, gltype=GLType.BOOL),
                GLType(param_id=503, hid=hid, gltype=GLType.NUMERIC),
                GLType(param_id=504, hid=hid, gltype=GLType.ENUM, cluster_filter=True),
            ]
            cls.TYPES.add(hid)

        return (
            [
                GLParam(param_id=501, value=601),
                GLParam(param_id=502, value=1),
                GLParam(param_id=503, value=603),
            ]
            if not is_offer
            else [
                GLParam(param_id=504, value=5),
            ]
        )

    @classmethod
    def _add_gl_model(cls, hyperid, hid=201, discount=None):
        cls.index.models.append(
            Model(hyperid=hyperid, title='model', hid=hid, model_clicks=100500, glparams=cls._add_glparams(hid))
        )
        cls.index.regional_models += [
            RegionalModel(hyperid=hyperid, offers=1000, price_min=100, price_old_min=500, max_discount=50, rids=[1001]),
        ]
        cls.index.offers.append(
            Offer(
                hyperid=hyperid,
                title='model-offer',
                fesh=1,
                discount=discount,
                glparams=cls._add_glparams(hid, is_offer=True),
            )
        )

    def _test_glparam_json(self, req, model_id=101):
        """Проверяем что у модели с фильтрами:
        -параметр 501 со значением 601 есть в выдаче
        -параметр 502 со значением 1 есть в выдаче, с 0 нет
        -параметр 503 с минимальным и максимальным значением 603 есть в выдаче
        """
        response = self.report.request_json(req)
        self.assertFragmentIn(response, {"id": model_id})
        self.assertFragmentIn(response, {"filters": [{"id": "501", "values": [{"id": "601"}]}]})
        self.assertFragmentIn(
            response, {"filters": [{"id": "502", "values": [{"id": "1", "found": 1}, {"id": "0", "found": 0}]}]}
        )
        self.assertFragmentIn(response, {"filters": [{"id": "503", "values": [{"min": "603", "max": "603"}]}]})

    def _test_glparam_xml(self, req, model_id=101):
        """Проверяем что у модели с фильтрами:
        -параметр 501 со значением 601 есть в выдаче
        -параметр 502 со значением 1 есть в выдаче, с 0 нет
        -параметр 503 с минимальным и максимальным значением 603 есть в выдаче
        """
        response = self.report.request_xml(req)
        self.assertFragmentIn(response, """<model id="{model_id}">""".format(model_id=model_id))
        self.assertFragmentIn(response, """<filter id="501" type="enum"><value id="601"/></filter>""")
        self.assertFragmentIn(
            response,
            """<filter id="502" type="boolean">
            <value found="1" id="1"/>
            <value found="0" id="0"/>
        </filter>""",
        )
        self.assertFragmentIn(
            response, """<filter id="503" type="number"><value id="found" max="603" min="603"/></filter>"""
        )

    @classmethod
    def prepare_bestdeals(cls):
        cls._add_gl_model(hyperid=999, hid=299, discount=35)
        cls.index.hypertree += [
            HyperCategory(hid=299, output_type=HyperCategoryType.GURU),
        ]
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:", item_count=1000).respond(
            {"models": ["999"]}
        )

    def test_bestdeals(self):
        self.error_log.ignore('Personal category config is not available')
        self._test_glparam_xml(req="place=bestdeals&hid=299&rids=1001", model_id=999)

    @classmethod
    def prepare_personal_category_models(cls):
        """
        place=personalcategorymodels
        """
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [201, 1],
                        ],
                        splits=['9'],
                    ),
                ],
            )
        ]
        cls.bigb.on_request(yandexuid="009", client='merch-machine').respond(counters=[])
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:009", item_count=1000).respond(
            {"models": ["101"]}
        )

    def test_personal_category_models(self):
        """
        place=personalcategorymodels
        """
        self._test_glparam_json("place=personalcategorymodels&yandexuid=009&rids=1001")

    @classmethod
    def prepare_product_accessories(cls):
        """
        place=product_accessories
        """
        cls.index.models.append(Model(hyperid=103, hid=201, accessories=[101]))

    def test_product_accessories(self):
        """
        place=product_accessories
        """
        self._test_glparam_json(
            "place=product_accessories&hyperid=103&rids=1001&rearr-factors=market_disable_product_accessories=0"
        )

    @classmethod
    def _make_yamarec_partition(cls, formula_id, splits):
        names = keys = ['model_id', 'category_id']
        return YamarecFeaturePartition(
            feature_names=names, feature_keys=keys, features=[[101, 201]], formula_id=formula_id, splits=splits
        )

    @classmethod
    def _reg_ichwill_request(cls, user_id, models, item_count=40):
        cls.recommender.on_request_models_of_interest(
            user_id=user_id, item_count=item_count, with_timestamps=True, version=4
        ).respond({'models': map(str, models), 'timestamps': map(str, list(range(len(models), 0, -1)))})
        cls.bigb.on_request(yandexuid=user_id.replace('yandexuid:', ''), client='merch-machine').respond(counters=[])

    @classmethod
    def prepare_products_by_history(cls):
        """
        place=test_products_by_history
        """

        for h in range(104, 107):
            cls.index.models.append(Model(hyperid=h, hid=201))
            cls.index.regional_models.append(RegionalModel(hyperid=h, offers=1000, rids=[1001]))
            cls.index.offers.append(Offer(hyperid=h, fesh=1))
        cls._reg_ichwill_request('yandexuid:10009', [101, 104, 105, 106])
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='10009').respond(
            [DjModel(id='101'), DjModel(id='104'), DjModel(id='105'), DjModel(id='106')]
        )

    def test_products_by_history(self):
        """
        place=test_products_by_history
        """
        self._test_glparam_json("place=products_by_history&yandexuid=10009&rids=1001")
        self._test_glparam_json(
            "place=products_by_history&yandexuid=10009&rids=1001&rearr-factors=market_disable_dj_for_recent_findings%3D1"
        )

    @classmethod
    def prepare_visualanalogs(cls):
        """
        place=visualanalogs
        """

        cls.index.vclusters += [
            VCluster(
                hid=203,
                vclusterid=1000000001,
                title="visual cluster 1",
                clothes_index=[ClothesIndex([11], [11], [11])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=11)])],
            ),
            VCluster(
                hid=203,
                vclusterid=1000000111,
                title="visual cluster 111",
                clothes_index=[ClothesIndex([11], [11], [11])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=11)])],
                glparams=cls._add_glparams(203),
            ),
        ]

        cls.index.offers += [
            Offer(vclusterid=1000000111),
        ]

    def test_visualanalogs(self):
        """
        place=visualanalogs
        """
        self._test_glparam_json("place=visualanalog&vclusterid=1000000001&hid=203&analog-filter=1", 1000000111)

    @classmethod
    def prepare_also_viewed(cls):
        """
        Конфигурация для получения непустой выдачи also_viewed
        """
        cls.index.hypertree += [
            HyperCategory(hid=301, output_type=HyperCategoryType.GURU),
        ]
        cls._add_gl_model(hyperid=121, hid=301)
        cls._add_gl_model(hyperid=122, hid=301)
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '66'}, splits=[{'split': '1'}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=121, item_count=1000, version='66').respond(
            {'models': ['122']}
        )

    def test_also_viewed(self):
        """
        place=also_viewed
        """
        self._test_glparam_json(req='place=also_viewed&rids=1001&rearr-factors=split=1&hyperid=121', model_id=122)

    @classmethod
    def prepare_test_prime_common_filter_statistic(cls):
        cls.index.hypertree += [
            HyperCategory(hid=401, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=501, output_type=HyperCategoryType.CLUSTERS),
        ]
        cls._add_gl_model(401, hid=401)
        cls._add_gl_model(501, hid=501)

    def test_prime_common_filter_statistic(self):
        """
        Проверяем, что на place=prime в независимости от схлопывания и текста и вида категории в общей статистике
        по фильтрам есть:
        1. GL модельный фильтр
        2. GL офферный фильтр
        3. Магазинный фильтр
        """
        for req in itertools.chain.from_iterable(
            (
                'place=prime&hid={}'.format(hid),
                'place=prime&hid={}&allow-collapsing=1'.format(hid),
                'place=prime&text=model&hid={}&allow-collapsing=1'.format(hid),
            )
            for hid in (201, 401, 501)
        ):
            response = self.report.request_json(req)
            self.assertFragmentIn(
                response,
                {
                    'search': NotEmpty(),
                    'filters': [
                        {'id': '504', 'values': [{'id': '5'}]},
                        {'id': '501', 'values': [{'id': '601'}]},
                        {'id': 'fesh', 'values': [{'id': '1'}]},
                    ],
                },
            )


if __name__ == '__main__':
    main()

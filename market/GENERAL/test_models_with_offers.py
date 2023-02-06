#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    ModelGroup,
    Offer,
    RegionalModel,
    Shop,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main


class T(TestCase):
    """
    Набор тестов для модельных плэйсов,
    требующих наличие актуалных офферов, соответствующих региону
    """

    @classmethod
    def prepare(cls):
        """
        Магазины в различных регионах
        """
        cls.index.shops += [
            Shop(fesh=1, regions=[1]),
            Shop(fesh=2, regions=[2]),
            Shop(fesh=3, regions=[3]),
            Shop(fesh=4, regions=[3]),
            Shop(fesh=5, regions=[2, 4]),
            Shop(fesh=11, regions=[11]),
        ]

        # models with offers in index
        cls.register_model_collection(ids=list(range(1, 6)))

    @classmethod
    def register_model_collection(
        cls, ids, hids=None, prices=None, need_create_models=True, position=0, need_bad_mrs=True
    ):
        _hids = hids if hids is not None else [None] * len(ids)
        _prices = prices if prices is not None else [None] * len(ids)
        if need_create_models:
            cls.index.models += [Model(hyperid=hyperid, hid=_hids[i]) for i, hyperid in enumerate(ids)]
        cls.index.offers += [
            Offer(fesh=i + position + 1, hyperid=hyperid, price=_prices[i], hid=_hids[i])
            for i, hyperid in enumerate(ids)
        ]

        if need_bad_mrs:
            # noisy models without offers in index but with mrs
            cls.index.models += [Model(hyperid=hyperid + 9000, hid=_hids[i]) for i, hyperid in enumerate(ids)]
            cls.index.regional_models += [
                RegionalModel(hyperid=hyperid + 9000, offers=123, rids=list(range(1, 5))) for hyperid in ids
            ]

    @classmethod
    def register_mixed_model_collection(
        cls, ids, group_ids, hids=None, g_hids=None, prices=None, g_prices=None, need_bad_mrs=True
    ):
        cls.register_model_collection(
            ids=ids, hids=hids, prices=prices, need_create_models=True, need_bad_mrs=need_bad_mrs
        )
        gh = g_hids if g_hids is not None else [None] * len(group_ids)
        gp = g_prices if g_prices is not None else [None] * len(group_ids)
        cls.index.model_groups += [ModelGroup(hyperid=hyperid, hid=hid) for hyperid, hid in zip(group_ids, gh)]
        m_ids = [hyperid + 5000 for hyperid in group_ids]
        cls.index.models += [
            Model(hyperid=hyperid, hid=hid, group_hyperid=group_hyperid)
            for hyperid, group_hyperid, hid in zip(m_ids, group_ids, gh)
        ]
        cls.register_model_collection(
            ids=group_ids + m_ids, hids=gh + gh, prices=gp + gp, need_create_models=False, position=len(ids)
        )

    def __test(self, query, ids, shop_ids=None):
        """
        Проверяем, что в выдаче по запросу query присутствуют те и только те модели,
        для которых в индексе есть офферы запрошенного региона
        Ожидаем, что ids - список из 5 ид моделей, для которых заданы офферы
        из магазинов таких, что
            у 1й модели магазин с regions=[1]
            у 2й -- regions=[2]
            у 3й -- regions=[3]
            у 4й -- regions=[3]
            у 5й -- regions=[2, 4]
        """

        self.assertGreaterEqual(len(ids), 5)

        response = self.report.request_json(query + "&rids=1")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"entity": "product", "id": ids[0]},
                    ],
                }
            },
            preserve_order=False,
        )

        response = self.report.request_json(query + "&rids=2")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"entity": "product", "id": ids[1]},
                        {"entity": "product", "id": ids[4]},
                    ],
                }
            },
            preserve_order=False,
        )

        response = self.report.request_json(query + "&rids=3")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"entity": "product", "id": ids[2]},
                        {"entity": "product", "id": ids[3]},
                    ],
                }
            },
            preserve_order=False,
        )

        response = self.report.request_json(query + "&rids=4")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"entity": "product", "id": ids[4]},
                    ],
                }
            },
            preserve_order=False,
        )

    @classmethod
    def prepare_also_viewed(cls):
        """
        Конфигурация для получения нескольких моделей на выдаче also_viewed
        """
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '66'}, splits=[{"split": "also_viewed"}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=6, item_count=1000, version='66').respond(
            {'models': ['1', '2', '3', '4', '5']}
        )

    def test_also_viewed(self):
        """
        Проверяем also_viewed на наличие офферов для выдачи
        """
        self.__test(query="place=also_viewed&rearr-factors=split=also_viewed&hyperid=6", ids=list(range(1, 6)))

    @classmethod
    def prepare_better_price(cls):
        """
        Конфигурация для place=better_price
        """
        cls.register_model_collection(ids=list(range(11, 16)), prices=[100] * 5)
        cls.index.yamarec_places += [
            YamarecPlace(
                name="better-price",
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                        },
                        splits=[{"split": "better_price"}],
                    ),
                ],
            )
        ]
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1001", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 11, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 12, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 13, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 14, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 15, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )

    def test_better_price(self):
        """
        Проверяем better_price на наличие офферов для выдачи
        """
        self.__test(
            query='place=better_price&yandexuid=1001&rearr-factors=split=better_price&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0',
            ids=list(range(11, 16)),
        )

    @classmethod
    def prepare_product_accessories(cls):
        """
        Данные для непустой выдачи product_accessories и соответствующих офферов
        """
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{'split': 'product_accessories'}],
                    ),
                ],
            )
        ]

        cls.recommender.on_request_accessory_models(model_id=6, item_count=1000, version='1').respond(
            {'models': ['1', '2', '3', '4', '5']}
        )

    def test_product_accessories(self):
        """
        Проверяем product_accessories на наличие офферов для выдачи
        """
        self.__test(
            query='place=product_accessories&rearr-factors=split=product_accessories;market_disable_product_accessories=0&hyperid=6',
            ids=list(range(1, 6)),
        )

    @classmethod
    def prepare_popular_products(cls):
        """
        Конфигурация для popular_products
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=140,
                children=[HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in range(141, 146)],
            ),
        ]
        cls.register_model_collection(ids=list(range(41, 46)), hids=list(range(141, 146)), need_bad_mrs=False)
        cls.register_model_collection(ids=list(range(46, 51)), hids=list(range(141, 146)), need_bad_mrs=False)
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1002').respond(
            {'models': map(str, list(range(46, 51)))}
        )
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1002', item_count=40, with_timestamps=True
        ).respond({'models': map(str, list(range(46, 51))), 'timestamps': map(str, list(range(46, 51)))})

    def __test_popular_products(self, query, ids, shop_ids=None):
        """
        Проверяем, что в выдаче по запросу query присутствуют те и только те модели,
        для которых в индексе есть офферы запрошенного региона
        Ожидаем, что ids - список из 5 ид моделей, для которых заданы офферы
        из магазинов таких, что
            у 1й модели магазин с regions=[1]
            у 2й -- regions=[2]
            у 3й -- regions=[3]
            у 4й -- regions=[3]
            у 5й -- regions=[2, 4]
        """

        self.assertGreaterEqual(len(ids), 5)

        response = self.report.request_json(query + "&rids=1")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"entity": "product", "id": ids[0]},
                    ],
                }
            },
            preserve_order=False,
        )

        response = self.report.request_json(query + "&rids=2")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "results": [
                        {"entity": "product", "id": ids[1]},
                        {"entity": "product", "id": ids[4]},
                    ],
                }
            },
            preserve_order=False,
        )

        response = self.report.request_json(query + "&rids=3")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "results": [
                        {"entity": "product", "id": ids[2]},
                        {"entity": "product", "id": ids[3]},
                    ],
                }
            },
            preserve_order=False,
        )

        response = self.report.request_json(query + "&rids=4")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"entity": "product", "id": ids[4]},
                    ],
                }
            },
            preserve_order=False,
        )

    def test_popular_products(self):
        """
        Проверяем popular_products на наличие офферов для всех моделей выдачи
        """
        self.__test_popular_products(
            query="place=popular_products&yandexuid=1002&hid=140&rearr-factors=switch_popular_products_to_dj_no_nid_check=0",
            ids=list(range(41, 46)),
        )


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    RegionalModel,
    Shop,
    YamarecCategoryRanksPartition,
    YamarecPlace,
)
from core.types.autogen import Const
from core.testcase import main
from core.matcher import Empty, Greater, NoKey
from simple_testcase import SimpleTestCase
from core.crypta import CryptaName, CryptaFeature
from core.dj import DjModel


COLDSTART_CATEGORY_IDS = [91491, 90555]


class T(SimpleTestCase):
    @classmethod
    def prepare(cls):

        # 1. Small departments
        hyper_ids = list(range(12))
        hids = [100 + hyperid for hyperid in hyper_ids[:10]] + COLDSTART_CATEGORY_IDS

        cls.index.hypertree += [
            HyperCategory(hid=hid, name="Department#{}".format(hid), output_type=HyperCategoryType.GURU) for hid in hids
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COLDSTART_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY_RANKS,
                partitions=[
                    YamarecCategoryRanksPartition(category_list=COLDSTART_CATEGORY_IDS, splits=['*']),
                ],
            )
        ]

        cls.index.models += [Model(hyperid=hyp_id, hid=hid) for hyp_id, hid in zip(hyper_ids, hids)]

        # 2. Large departments

        # all hids more than 200
        # hyperids 10 times bigger than hids

        department1 = [HyperCategory(hid=i) for i in range(211, 220)]
        department2 = [HyperCategory(hid=i) for i in range(221, 225)]
        department2.append(
            HyperCategory(
                hid=225,
                children=[
                    HyperCategory(hid=226),
                ],
            )
        )
        department3 = [
            HyperCategory(
                hid=228,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=229, output_type=HyperCategoryType.GURU),
                ],
            )
        ]
        # trees
        cls.index.hypertree += [
            HyperCategory(hid=210, name="Department#210", children=department1),
            HyperCategory(hid=220, name="Department#220", children=department2),
            HyperCategory(hid=230, name="Department#230", children=department3),
        ]
        hids2 = list(range(211, 231))
        hypers_n_hids2 = [(hid * 10 + 1, hid) for hid in hids2]
        cls.index.models += [Model(hyperid=entry[0], hid=entry[1]) for entry in hypers_n_hids2]
        hyperids2_all = [e[0] for e in hypers_n_hids2]
        hyperids_all = hyper_ids + hyperids2_all
        hyperid2_main = hypers_n_hids2[0][0]
        hyperids2 = hyperids2_all[:-2]

        # 3. Offers
        cls.index.shops += [
            Shop(fesh=1, regions=[1001]),
        ]

        cls.index.offers += [Offer(hyperid=hyperid, fesh=1) for hyperid in hyperids_all]

        cls.index.regional_models += [RegionalModel(hyperid=hyperid, offers=1, rids=[1001]) for hyperid in hyperids_all]

        # 4. Responses
        for yuid, response_items in [
            (1, ["1", "2", "3", "4", "5"]),
            (2, ["1", "2", "3", "4", "5"]),
            (3, []),
            (4, [str(h) for h in [hyperid2_main] + hyperids2[1:][:4]]),
            (5, ["2231", "2241", "2251", "2261"]),
        ]:
            cls.recommender.on_request_viewed_models(user_id="yandexuid:100{yuid}".format(yuid=yuid)).respond(
                {"models": response_items}
            )
            cls.recommender.on_request_models_of_interest(user_id="yandexuid:100{yuid}".format(yuid=yuid)).respond(
                {"models": response_items}
            )
            cls.recommender.on_request_models_of_interest(
                user_id="yandexuid:100{yuid}".format(yuid=yuid), item_count="40", with_timestamps=True, version=4
            ).respond({'models': response_items, 'timestamps': map(str, list(range(len(response_items), 0, -1)))})
            cls.crypta.on_request_profile(yandexuid=1000 + yuid).respond(
                features=[
                    CryptaFeature(name=CryptaName.GENDER_FEMALE, value=100),
                ]
            )
            cls.settings.set_default_reqid = False
            cls.dj.on_request(yandexuid=1000 + yuid).respond([DjModel(id=modelid) for modelid in response_items])

    def test_empty_hist(self):
        """
        Проверка, срабатывает ли переключение на популярные, если нет данных
        для истории
        """
        all_ids = list(range(1, 12))
        popular_ids = [10, 11]
        # 1. Для пользователя нет рекомендаций по истории, ожидаем популярные товары
        #  Нет истории
        self.assertOnlyModelsInResponse(
            "place=products_by_history_ex&rids=1001&yandexuid=1003&page=1&numdoc=3&debug=1&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            ids=popular_ids,
            all_ids=all_ids,
        )
        self.assertOnlyModelsInResponse(
            "place=products_by_history_ex&rids=1001&yandexuid=1003&page=1&numdoc=3&debug=1",
            ids=popular_ids,
            all_ids=all_ids,
        )

        # 2. Ожидаем рекоммендации по истории просмотренного
        self.assertModelsInResponse(
            "place=products_by_history_ex&rids=1001&yandexuid=1001&page=1&numdoc=4&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            ids=[1, 2, 3, 4],
        )
        self.assertModelsInResponse(
            "place=products_by_history_ex&rids=1001&yandexuid=1001&page=1&numdoc=4", ids=[1, 2, 3, 4]
        )

    def test_filter_by_hid(self):
        """
        Проверка работы фильтра по категориям
        """
        # Работает включающий фильтр
        self.assertOnlyModelsInResponse(
            "place=products_by_history_ex&rids=1001&yandexuid=1001&hid=101,102&page=1&numdoc=11&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            ids=[1, 2],
            all_ids=list(range(1, 12)),
        )

        # Работает исключающий фильтр
        self.assertOnlyModelsInResponse(
            "place=products_by_history_ex&rids=1001&yandexuid=1001&hid=-102,-105&page=1&numdoc=11&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            ids=[1, 3, 4],
            all_ids=list(range(1, 12)),
        )

        # Фильтр может привести к исключению всех рекомендаций по истории просмотров
        # Проверяем, что это в этом случае приходят популярные товары
        # self.assertOnlyModelsInResponse("place=products_by_history_ex&rids=1001&yandexuid=1001&hid=-101,-102,-103,-104,-105&page=1&numdoc=11", ids=[10, 11], all_ids=range(1, 12))
        # self.assertOnlyModelsInResponse("place=products_by_history_ex&rids=1001&yandexuid=1001&hid=110&page=1&numdoc=11", ids=[10], all_ids=range(1, 12))

    def test_department_filter(self):
        """
        Проверка работы фильтра по категориям.
        Включающий фильтр должен расширяться до всего департамента
        """

        def create_category_model(hid):
            return {"categories": [{"id": hid}]}

        # Включение департамента
        self.assertOnlyItemsInResponse(
            "place=products_by_history_ex&rids=1001&yandexuid=1004&hid=210&page=1&numdoc=4&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            ids=list(range(211, 215)),
            all_ids=list(range(211, 231)),
            item_factory=create_category_model,
        )

        # Исключение департамента
        # Все модели из products_by_history отфильтровываются, ожидаем ответ от popular_products
        self.assertOnlyItemsInResponse(
            "place=products_by_history_ex&rids=1001&yandexuid=1004&hid=-210,91491,90555&page=1&numdoc=4&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            ids=[91491, 90555],
            all_ids=list(range(211, 231)) + [91491, 90555],
            item_factory=create_category_model,
        )

        # Включение подкатегории департамента: ожидаем как результат - включение всего департамента
        #
        # 1 Есть история
        # .1 Листовая категория
        query_f = "place=products_by_history_ex&rids=1001&yandexuid=1005&hid={hid}&page=1&numdoc=4"
        query226 = query_f.format(hid=226)
        self.assertOnlyItemsInResponse(
            query226, ids=list(range(223, 227)), all_ids=list(range(211, 231)), item_factory=create_category_model
        )
        # .2 Нелистовая категория
        query225 = query_f.format(hid=225)
        self.assertEqualJsonResponses(query225, query226)
        #
        # 2 Нет истории
        query_f = "place=products_by_history_ex&rids=1001&yandexuid=1003&hid={hid}&page=1&numdoc=4"
        query229 = query_f.format(hid='229,91491,90555')
        self.assertOnlyItemsInResponse(
            query229,
            ids=[91491, 90555],
            all_ids=list(range(211, 231)) + [91491, 90555],
            item_factory=create_category_model,
        )
        # .2 Нелистовая категория
        query228 = query_f.format(hid='228,91491,90555')
        self.assertEqualJsonResponses(query228, query229)

    def test_root_hid_in_filter(self):
        """
        Проверяем, что корневая категория в hid-фильтре просто игнорируется и не приводит к ошибке
        """
        response = self.report.request_json(
            "place=products_by_history_ex&rids=1001&yandexuid=1001&hid={root_hid}".format(root_hid=Const.ROOT_HID)
        )
        self.assertFragmentIn(response, {"error": NoKey("error"), "search": {"total": Greater(0)}})

    def test_metainfo(self):
        """
        Проверка наличия метаинформации об источнике данных в выдаче
        """
        response = self.report.request_json("place=products_by_history_ex&rids=1001&yandexuid=1001&page=1&numdoc=4")
        self.assertFragmentNotIn(response=response, fragment={"meta": Empty()})
        self.assertFragmentNotIn(response=response, fragment={"meta": {"place": Empty()}})
        self.assertFragmentIn(response=response, fragment={"meta": {"place": "products_by_history"}})

        # response = self.report.request_json("place=products_by_history_ex&rids=1001&yandexuid=1001&page=1&numdoc=3&hid=91491,90555")
        # self.assertFragmentNotIn(response=response, fragment={"meta": Empty()})
        # self.assertFragmentNotIn(response=response, fragment={"meta": {"place": Empty()}})
        # self.assertFragmentIn(response=response, fragment={"meta":{"place":"popular_products"}})


if __name__ == "__main__":
    main()

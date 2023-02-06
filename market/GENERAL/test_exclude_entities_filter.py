#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Const, Model, Offer, RegionalModel, Shop
from core.testcase import main
from simple_testcase import SimpleTestCase, create_model, create_offer


class T(SimpleTestCase):
    """
    Набор тестов для отдельного исключающего фильтра моделей и офферов '?exclude='
    """

    def check_filter(self, base_query, all_ids, item_factory):
        """
        Проверка работы исключающего фильтра
        """

        def exclude(ids):
            return "{base_query}&exclude={ids}".format(base_query=base_query, ids=",".join(map(str, ids)))

        self.assertItemsInResponse(query=base_query, ids=all_ids, item_factory=item_factory)
        excluded_sz = min(2, len(all_ids) - 1)
        if excluded_sz < 1:
            return
        excluded_ids = all_ids[:excluded_sz]
        included_ids = all_ids[excluded_sz:]
        response = self.report.request_json(exclude(excluded_ids))
        self.assertItemsIn(response=response, ids=included_ids, item_factory=item_factory)
        self.assertItemsNotIn(response=response, ids=excluded_ids, item_factory=item_factory)

    @classmethod
    def prepare_product_accessories(cls):
        cls.index.models += [
            Model(hyperid=2, hid=101, accessories=[21, 22, 23]),
            Model(hyperid=21, hid=102, accessories=[]),
            Model(hyperid=22, hid=102, accessories=[]),
            Model(hyperid=23, hid=102, accessories=[]),
        ]
        cls.index.shops += [
            Shop(fesh=1, home_region=Const.ROOT_COUNTRY, regions=[1001], cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [Offer(fesh=1, hyperid=hyperid) for hyperid in [2, 21, 22, 23]]
        cls.index.regional_models += [
            RegionalModel(hyperid=2, offers=1, rids=[1001]),
            RegionalModel(hyperid=21, offers=123, rids=[1001]),
            RegionalModel(hyperid=22, offers=1, rids=[1001]),
            RegionalModel(hyperid=23, offers=1, rids=[1001]),
        ]

    def test_product_accessories(self):
        """
        Проверка работы исключающего фильтра моделей для product_accessories
        """
        self.check_filter(
            base_query="place=product_accessories&rids=1001&hyperid=2&rearr-factors=market_disable_product_accessories=0",
            all_ids=[21, 22, 23],
            item_factory=create_model,
        )

    @classmethod
    def prepare_offer_accessories(cls):
        cls.index.shops += [
            Shop(fesh=1010, home_region=Const.ROOT_COUNTRY, regions=[1001], cpa=Shop.CPA_REAL),
        ]
        cls.index.models += [
            Model(hyperid=3, hid=101),
            Model(hyperid=31, hid=102),
            Model(hyperid=32, hid=102),
            Model(hyperid=33, hid=102),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=3, offers=1, rids=[1001]),
            RegionalModel(hyperid=31, offers=1, rids=[1001]),
            RegionalModel(hyperid=32, offers=1, rids=[1001]),
            RegionalModel(hyperid=33, offers=1, rids=[1001]),
        ]
        cls.index.offers += [
            Offer(
                hyperid=3,
                hid=101,
                price=100,
                cpa=Offer.CPA_REAL,
                fesh=1010,
                waremd5="BH8EPLtKmdLQhLUasgaOnA",
                rec=["KXGI8T3GP_pqjgdd7HfoHQ", "yRgmzyBD4j8r4rkCby6Iuw", "xzFUFhFuAvI1sVcwDnxXPQ"],
            ),
            Offer(hyperid=31, hid=102, cpa=Offer.CPA_REAL, fesh=1010, waremd5="KXGI8T3GP_pqjgdd7HfoHQ"),
            Offer(hyperid=32, hid=102, cpa=Offer.CPA_REAL, fesh=1010, waremd5="yRgmzyBD4j8r4rkCby6Iuw"),
            Offer(
                hyperid=33,
                hid=102,
                price=100,
                descr="descr",
                cpa=Offer.CPA_REAL,
                fesh=1010,
                waremd5="xzFUFhFuAvI1sVcwDnxXPQ",
            ),
        ]

    def test_offer_accessories(self):
        """
        Проверка работы исключающего фильтра офферов для accessories & product_accessories_ex
        """

        def create_offer_by_model_id(offer_id):
            ids = {
                "BH8EPLtKmdLQhLUasgaOnA": 3,
                "KXGI8T3GP_pqjgdd7HfoHQ": 31,
                "yRgmzyBD4j8r4rkCby6Iuw": 32,
                "xzFUFhFuAvI1sVcwDnxXPQ": 33,
            }
            return create_offer(hyperid=ids[offer_id], waremd5=offer_id)

        all_offer_ids = ["KXGI8T3GP_pqjgdd7HfoHQ", "yRgmzyBD4j8r4rkCby6Iuw", "xzFUFhFuAvI1sVcwDnxXPQ"]
        self.check_filter(
            base_query="place=accessories&rids=1001&hyperid=3&fesh=1010&offerid=BH8EPLtKmdLQhLUasgaOnA&price=100",
            all_ids=all_offer_ids,
            item_factory=create_offer_by_model_id,
        )

    def test_mixed(self):
        """
        Тест корректной работы смешанного фильтра: в списке exclude присутствуют как модели, так и офферы,
        при этом репорт не падает и фильтр работает на моделях и на офферах
        Ожидаются данные из prepare_product_accessories
        """
        self.assertOffersNotInResponse(
            query="place=accessories&rids=1001&hyperid=3&fesh=1010&offerid=BH8EPLtKmdLQhLUasgaOnA&price=100&exclude=21,yRgmzyBD4j8r4rkCby6Iuw,3,xzFUFhFuAvI1sVcwDnxXPQ",
            model_ids=[32, 33],
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
        Конфигурация для получения хотя бы 3 моделей на выдаче products_by_history
        """
        cls._reg_ichwill_request("yandexuid:1001", [4, 5, 6, 7])
        cls.index.models += [
            Model(hyperid=4, hid=101, analogs=[5, 6]),
            Model(hyperid=5, hid=101),
            Model(hyperid=6, hid=101),
            Model(hyperid=7, hid=101),
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=4, offers=123),
            RegionalModel(hyperid=5, offers=123),
            RegionalModel(hyperid=6, offers=123),
            RegionalModel(hyperid=7, offers=123),
        ]
        cls.index.offers += [
            Offer(hyperid=4, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(hyperid=5, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(hyperid=6, cpa=Offer.CPA_REAL, override_cpa_check=True),
            Offer(hyperid=7, cpa=Offer.CPA_REAL, override_cpa_check=True),
        ]

    def test_products_by_history(self):
        """
        Проверка работы исключающего фильтра моделей для products_by_history
        """
        self.check_filter(
            base_query="place=products_by_history&yandexuid=1001&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            all_ids=[4, 5, 6, 7],
            item_factory=create_model,
        )


if __name__ == '__main__':
    main()

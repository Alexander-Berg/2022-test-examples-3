#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop, YamarecPlace, YamarecSettingPartition
from core.testcase import main
from core.types.autogen import Const
import simple_testcase
from simple_testcase import SimpleTestCase


class T(SimpleTestCase):
    """
    Набор тестов для офферных аксессуаров. Продолжение
    (начало в test_recommendations.py)
    """

    @classmethod
    def prepare(cls):
        """
        Оффер и аксессуары к нему из разных магазинов,
        а также конфигурация для офферных аксессуаров с различными комбинациями
        источников поиска аксессуаров
        """
        cls.index.models += [
            Model(hyperid=1, hid=101, accessories=[2, 3]),
            Model(hyperid=2, hid=101),
            Model(hyperid=3, hid=101),
        ]

        main_offer = "EpnWVxDQxj4wg7vVI1ElnA"
        accessories = ["pCl2on9YL4fCV8poq57hRg", "xzFUFhFuAvI1sVcwDnxXPQ", "bpQ3a9LXZAl_Kz34vaOpSg"]

        cls.index.offers += [
            Offer(
                hyperid=1, price=50, cpa=Offer.CPA_REAL, fesh=1001, waremd5=main_offer, rec=accessories
            ),  # протестим и такое, хотя, в теге rec не может оказаться чужой оффер
            Offer(hyperid=2, price=90, cpa=Offer.CPA_REAL, fesh=1001, waremd5=accessories[0]),
            Offer(hyperid=2, price=90, cpa=Offer.CPA_REAL, fesh=1002, waremd5=accessories[1]),
            Offer(hyperid=3, price=90, cpa=Offer.CPA_REAL, fesh=1002, waremd5=accessories[2]),
        ]
        cls.index.shops += [
            Shop(fesh=1001, home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
            Shop(fesh=1002, home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.OFFER_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        # включаем только ichwill
                        params={
                            "use-external": "1",
                            "use-product": "0",
                            "use-shop": "0",
                            "version": "1",
                        },
                        splits=[{"split": "1"}],
                    ),
                    # включаем только оффлайн-формулу
                    YamarecSettingPartition(
                        params={
                            "use-external": "0",
                            "use-product": "1",
                            "use-shop": "0",
                            "version": "1",
                        },
                        splits=[{"split": "2"}],
                    ),
                    # включаем только REC
                    YamarecSettingPartition(
                        params={
                            "use-external": "0",
                            "use-product": "0",
                            "use-shop": "1",
                            "version": "1",
                        },
                        splits=[{"split": "3"}],
                    ),
                    # включаем 1 + 2
                    YamarecSettingPartition(
                        params={
                            "use-external": "1",
                            "use-product": "1",
                            "use-shop": "0",
                            "version": "1",
                        },
                        splits=[{"split": "4"}],
                    ),
                    # включаем 1 +3
                    YamarecSettingPartition(
                        params={
                            "use-external": "1",
                            "use-product": "0",
                            "use-shop": "1",
                            "version": "1",
                        },
                        splits=[{"split": "5"}],
                    ),
                    # включаем 2 + 3
                    YamarecSettingPartition(
                        params={
                            "use-external": "0",
                            "use-product": "1",
                            "use-shop": "1",
                            "version": "1",
                        },
                        splits=[{"split": "6"}],
                    ),
                ],
            )
        ]
        cls.recommender.on_request_accessory_offers(offer_id=main_offer, item_count=100, version="1").respond(
            {"offers": accessories}
        )

    def assertOnlyAccessoriesInResponse(self, query, waremd5s, all_waremd5s):
        return self.assertOnlyItemsInResponse(
            query=query, ids=waremd5s, all_ids=all_waremd5s, item_factory=create_offer
        )

    def test_best_shop(self):
        """
        Оффер-аксессуар должен быть из того же магазина, что и главный оффер
        """
        main_offer = "EpnWVxDQxj4wg7vVI1ElnA"
        accessories = ["pCl2on9YL4fCV8poq57hRg", "xzFUFhFuAvI1sVcwDnxXPQ", "bpQ3a9LXZAl_Kz34vaOpSg"]
        _ = [main_offer] + accessories

        for split in range(1, 7):
            query = "place=accessories&pp=143&fesh=1001&hyperid=1&offerid={waremd5}&price=50&rearr-factors=split={split}".format(
                waremd5=main_offer, split=split
            )
        self.assertOnlyAccessoriesInResponse(query=query, waremd5s=accessories[0:1], all_waremd5s=accessories)


def create_offer(waremd5):
    offer = simple_testcase.create_offer(hyperid=0)
    offer["wareId"] = waremd5
    del offer["model"]
    return offer


if __name__ == "__main__":
    main()

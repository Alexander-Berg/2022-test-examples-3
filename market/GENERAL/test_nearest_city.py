#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Region, Shop
from core.testcase import TestCase, main
from core.matcher import EmptyList


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.regiontree += [
            Region(
                rid=11119,
                name='Республика Татарстан',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=120895,
                        name='Городской округ Набережные Челны',
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        children=[
                            Region(rid=236, name='Набережные Челны', region_type=Region.CITY),
                        ],
                    ),
                    Region(
                        rid=99781,
                        name='Тукаевский район',
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        children=[
                            Region(
                                rid=175783,
                                name='Малошильнинское сельское поселение',
                                region_type=Region.SETTLEMENT,
                                children=[
                                    Region(rid=141174, name='Малая Шильна', region_type=Region.VILLAGE),
                                ],
                            ),
                        ],
                    ),
                ],
            )
        ]

        cls.index.shops += [
            Shop(fesh=101, priority_region=236, name='magazin'),
        ]

        cls.index.models += [
            Model(hyperid=201, hid=1, title='tovar'),
        ]

        cls.index.offers += [
            Offer(title='tovar', hyperid=201, price=400, fesh=101),
        ]

    def test_in_small_village(self):
        """
        Запрашиваем деревню, и НЕ получаем оффер из города
        """
        response = self.report.request_json('place=prime&hyperid=201&rids=141174')
        self.assertFragmentIn(response, {"search": {"results": EmptyList()}})

    def test_in_small_village_but_from_nearest(self):
        """
        Запрашиваем деревню, получаем оффер из города
        """
        response = self.report.request_json('place=prime&hyperid=201&rids=141174&show-nearest-region=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "delivery": {
                                "shopPriorityRegion": {
                                    "id": 236,
                                    "name": "Набережные Челны",
                                },
                                "isForcedRegion": True,
                            },
                        }
                    ]
                }
            },
        )
        self.show_log_tskv.expect(fuid='1592=1')
        self.click_log.expect(fuid='1592=1')


if __name__ == '__main__':
    main()

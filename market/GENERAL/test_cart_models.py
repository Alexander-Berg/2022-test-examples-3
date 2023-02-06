#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, Offer, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                name="Котиковый магазин",
            ),
            Shop(
                fesh=2,
                name="Песиковый магазин",
            ),
        ]

        cls.index.models += [Model(hyperid=1), Model(hyperid=2)]

        cls.index.offers += [
            Offer(hyperid=1, fesh=1),
            Offer(hyperid=1, fesh=2),
            Offer(hyperid=2, fesh=2),
            Offer(fesh=2, waremd5="2n1MV5Ja-4lEJv7A64Rr8g"),
        ]

    def test_sanity(self):
        # просто проверяем, что плейс срабатывает
        response = self.report.request_json('place=cart_models&modelid=1,2&offerid=2n1MV5Ja-4lEJv7A64Rr8g')
        self.assertFragmentIn(
            response,
            [
                {
                    "shop": {
                        "entity": "shop",
                        "id": 1,
                        "name": "Котиковый магазин",
                    },
                    "models": [{"model_id": 1}],
                },
                {
                    "shop": {
                        "entity": "shop",
                        "id": 2,
                        "name": "Песиковый магазин",
                    },
                    "models": [{"model_id": 0}, {"model_id": 1}, {"model_id": 2}],
                },
            ],
        )


if __name__ == '__main__':
    main()

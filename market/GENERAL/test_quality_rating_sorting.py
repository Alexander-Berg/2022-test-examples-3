#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import NewShopRating, Offer, Region, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=10758, name='Химки'),
                ],
            ),
            Region(
                rid=10650,
                name='Брянская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=191, name='Брянск'),
                ],
            ),
        ]
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=11, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=111, priority_region=191, new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=2, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=2.0)),
            Shop(fesh=22, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=2.0)),
            Shop(fesh=222, priority_region=191, new_shop_rating=NewShopRating(new_rating_total=2.0)),
            Shop(fesh=3, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=33, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=4, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(fesh=44, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(fesh=5, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=55, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=6, priority_region=213, regions=[225]),
            Shop(fesh=66, priority_region=213, regions=[225]),
        ]
        cls.index.offers += [
            Offer(hyperid=1001, fesh=1, price=99),
            Offer(hyperid=1001, fesh=11, price=100),
            Offer(hyperid=1001, fesh=2, price=99),
            Offer(hyperid=1001, fesh=22, price=100),
            Offer(hyperid=1001, fesh=3, price=97),
            Offer(hyperid=1001, fesh=33, price=98),
            Offer(hyperid=1001, fesh=4, price=99),
            Offer(hyperid=1001, fesh=44, price=100),
            Offer(hyperid=1001, fesh=5, price=97),
            Offer(hyperid=1001, fesh=55, price=98),
            Offer(hyperid=1001, fesh=6, price=95),
            Offer(hyperid=1001, fesh=66, price=96),
            Offer(hyperid=1002, fesh=1, price=100),
            Offer(hyperid=1002, fesh=11, price=99),
            Offer(hyperid=1002, fesh=2, price=98),
            Offer(hyperid=1002, fesh=22, price=97),
            Offer(hyperid=1002, fesh=3, price=102),
            Offer(hyperid=1002, fesh=33, price=100),
            Offer(hyperid=1002, fesh=4, price=96),
            Offer(hyperid=1002, fesh=44, price=95),
            Offer(hyperid=1002, fesh=5, price=98),
            Offer(hyperid=1002, fesh=55, price=97),
            Offer(hyperid=1002, fesh=6, price=101),
            Offer(hyperid=1002, fesh=66, price=99),
            Offer(hyperid=1002, fesh=111, price=150),
            Offer(hyperid=1002, fesh=222, price=200),
        ]


if __name__ == '__main__':
    main()

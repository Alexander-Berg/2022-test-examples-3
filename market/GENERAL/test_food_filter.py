#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Model, Offer, OfferDimensions, Shop
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)

FOOD_OFFER = 'food_offer'
FOOD_OFFER_CHILD = 'child_food_offer'
HEALTH_OFFER = 'health_offer'
BOOK_OFFER = 'book_offer'

HEALTH_CATEGORY = 8475840
FOOD_CATEGORY = EATS_CATEG_ID
CHILDREN_FOOD_CATEGORY = 15696738
BOOKS_CATEGORY = 13858284

BASE_REQUEST = 'place=offerinfo&rids=213&regset=1&show-urls=&offerid={}&debug=1&enable-foodtech-offers=eda_retail,eda_restaurants,lavka'
FULL_REQUEST = BASE_REQUEST + '&client={}&rearr-factors=market_eats_show_all_categories={}'


class T(TestCase):
    shop = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        regions=[213],
        name='shop',
        cpa=Shop.CPA_REAL,
    )

    food_offer = Offer(
        title=FOOD_OFFER,
        offerid=FOOD_OFFER,
        hyperid=101,
        fesh=shop.fesh,
        waremd5='Food----Eda---Vm1Goleg',
        price=1100,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
        is_eda=True,
    )

    child_food_offer = Offer(
        title=FOOD_OFFER_CHILD,
        offerid=FOOD_OFFER_CHILD,
        hyperid=102,
        fesh=shop.fesh,
        waremd5='ChildFoodLavkaVm1Goleg',
        price=10530881,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
        is_lavka=True,
    )

    health_offer = Offer(
        title=HEALTH_OFFER,
        offerid=HEALTH_OFFER,
        hyperid=103,
        fesh=shop.fesh,
        waremd5='Health---Eda--Vm1Goleg',
        price=10599881,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
        is_eda=True,
    )

    book_offer = Offer(
        title=BOOK_OFFER,
        offerid=BOOK_OFFER,
        hyperid=104,
        fesh=shop.fesh,
        waremd5='Book---Lavka--Vm1Goleg',
        price=9881,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=10, length=15),
        is_lavka=True,
    )

    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(FOOD_CATEGORY, Stream.FMCG.value),
            CategoryStreamRecord(CHILDREN_FOOD_CATEGORY, Stream.FMCG.value),
        ]
        cls.index.hypertree += [
            HyperCategory(
                hid=FOOD_CATEGORY,
                name='Еда',
                children=[
                    HyperCategory(hid=CHILDREN_FOOD_CATEGORY, name='Детская еда'),
                ],
            ),
            HyperCategory(
                hid=HEALTH_CATEGORY,
                name='Лекарства',
            ),
            HyperCategory(
                hid=BOOKS_CATEGORY,
                name='Книжки',
            ),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=FOOD_CATEGORY, title='hyperid_101'),
            Model(hyperid=102, hid=CHILDREN_FOOD_CATEGORY, title='hyperid_102'),
            Model(hyperid=103, hid=HEALTH_CATEGORY, title='hyperid_103'),
            Model(hyperid=104, hid=BOOKS_CATEGORY, title='hyperid_104'),
        ]

        cls.index.shops += [T.shop]

        cls.index.offers += [T.food_offer, T.child_food_offer, T.health_offer, T.book_offer]

    def test_food_eats_lavka_offers(self):
        '''
        Проверяем что офферы продуктов еды/лавки не скрываются вне зависимости от клиента и значения реар-флага
        '''
        for offer in [T.food_offer, T.child_food_offer]:
            for client in ['eats', 'lavka', 'any_client']:
                for flag in [0, 1]:
                    response = self.report.request_json(FULL_REQUEST.format(offer.waremd5, client, flag))
                    self.assertFragmentIn(
                        response,
                        {
                            'wareId': offer.waremd5,
                        },
                    )

    def test_no_food_eats_lavka_offers(self):
        '''
        Проверяем что непродуктовые офферы еды/лавки не скрываются если client=eda/lavka или market_eats_show_all_categories=1
        '''
        for offer in [self.health_offer, self.book_offer]:
            for additional_flags in [
                '&client=eats',
                '&client=lavka',
                '&rearr-factors=market_eats_show_all_categories=1',
            ]:
                response = self.report.request_json(BASE_REQUEST.format(offer.waremd5) + additional_flags)
                self.assertFragmentIn(
                    response,
                    {
                        'wareId': offer.waremd5,
                    },
                )

        '''
        Проверяем что непродуктовые офферы еды/лавки скрываются если client!=eda/lavka или market_eats_show_all_categories=0
        '''
        for offer in [self.health_offer, self.book_offer]:
            for additional_flags in ['&client=any_client', '&rearr-factors=market_eats_show_all_categories=0']:
                response = self.report.request_json(BASE_REQUEST.format(offer.waremd5) + additional_flags)
                self.assertFragmentNotIn(
                    response,
                    {
                        'wareId': offer.waremd5,
                    },
                )


if __name__ == '__main__':
    main()

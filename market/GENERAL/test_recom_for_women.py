#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Shop,
    Tax,
    Vat,
    YamarecCategoryRanksPartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.crypta import CryptaName, CryptaFeature

# ranged list of categories for coldstart hardcoded into report_bin resources

COLDSTART_CATEGORIES = [
    90799,
    13360751,
    90796,
    10470548,
]

COLDSTART_CATEGORIES_POPULAR = [
    8476097,
    8476099,
    15927546,
    8476098,
]

COLDSTART_CATEGORIES_DEALS = [
    4854062,
    91183,
    91184,
    16336734,
]

PERSONAL_CATEGORIES = list(range(101, 131))

ALL_CATEGORIES_SET = set(
    COLDSTART_CATEGORIES + COLDSTART_CATEGORIES_POPULAR + COLDSTART_CATEGORIES_DEALS + PERSONAL_CATEGORIES
)


class T(TestCase):
    """
    Набор тестов для выдачи рекомендаций на колдстарте,
    ориентированных на женскую аудиторию
    """

    @classmethod
    def prepare(cls):
        cls.settings.rgb_blue_is_cpa = True

        """coldstart categories"""
        cls.index.hypertree += [
            HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in ALL_CATEGORIES_SET
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COLDSTART_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY_RANKS,
                partitions=[
                    YamarecCategoryRanksPartition(category_list=COLDSTART_CATEGORIES, splits=['*']),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COLDSTART_CATEGORIES_DEALS,
                kind=YamarecPlace.Type.CATEGORY_RANKS,
                partitions=[
                    YamarecCategoryRanksPartition(category_list=COLDSTART_CATEGORIES_DEALS, splits=['*']),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COLDSTART_CATEGORIES_POPULAR_PRODUCTS,
                kind=YamarecPlace.Type.CATEGORY_RANKS,
                partitions=[
                    YamarecCategoryRanksPartition(category_list=COLDSTART_CATEGORIES_POPULAR, splits=['*']),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1, regions=[1]),
            Shop(
                fesh=2,
                datafeed_id=1,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

        models = [(hid * 10 + 1, hid) for hid in ALL_CATEGORIES_SET]
        cls.index.models += [Model(hyperid=m[0], hid=m[1]) for m in models]

        cls.index.offers += [Offer(hyperid=m[0], fesh=1) for m in models]

        # С флагом &rearr-factors=market_disable_blue_3p_discount_profitability_check=1 скидки размером больше 75%
        # признаются невалидными, поэтому устанавливаем скидку в размере 60% (60 = 100 - 40)
        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid, sku=hyperid * 10 + 1, blue_offers=[BlueOffer(vat=Vat.VAT_10, feedid=1, discount=40)]
            )
            for hyperid in [m[0] for m in models]
        ]

        viewed_models = [hid * 10 + 1 for hid in PERSONAL_CATEGORIES]
        history = {
            '1001': viewed_models,
            '1002': viewed_models,
            '1003': viewed_models,
            '1004': [],
            '1005': [],
            '1006': [],
            '': [],
        }
        for yandexuid, ids in list(history.items()):
            for item_count in [40, 1000]:
                for with_timestamps in [False, True]:
                    cls.recommender.on_request_models_of_interest(
                        user_id='yandexuid:{}'.format(yandexuid), item_count=item_count, with_timestamps=with_timestamps
                    ).respond(
                        {
                            'models': list(map(str, ids)),
                            'timestamps': list(map(str, reversed(list(range(1, len(ids) + 1))))),
                        }
                    )

        genders = {'1001': 200000, '1002': 600000, '1003': None, '1004': 100000, '1005': 700000, '1006': None, '': None}
        for yandexuid, female_weight in list(genders.items()):
            if female_weight is None:
                cls.crypta.on_request_profile(yandexuid=yandexuid).respond(features=[])
            else:
                cls.crypta.on_request_profile(yandexuid=yandexuid).respond(
                    features=[
                        CryptaFeature(name=CryptaName.GENDER_FEMALE, value=female_weight),
                    ]
                )

    def test_popular_products(self):
        """Проверяем автопереключение колдстарта на категории для женской аудитории"""
        # skipping of top 6 history models in popular products was added in MARKETOUT-21899
        #                                            ... and was deleted in MARKETRECOM-2510
        POPULAR_PERSONAL_PRODUCTS = [{'entity': 'product', 'id': hid * 10 + 1} for hid in PERSONAL_CATEGORIES[:4]]
        DEALS_PERSONAL_PRODUCTS = [{'entity': 'product', 'id': hid * 10 + 1} for hid in PERSONAL_CATEGORIES[:4]]
        PRODUCTS_DEFAULT = [{'entity': 'product', 'id': hid * 10 + 1} for hid in COLDSTART_CATEGORIES]
        POPULAR_PRODUCTS_W = [{'entity': 'product', 'id': hid * 10 + 1} for hid in COLDSTART_CATEGORIES_POPULAR]
        DEALS_W = [{'entity': 'product', 'id': hid * 10 + 1} for hid in COLDSTART_CATEGORIES_DEALS]
        for rgb in ['green', 'blue']:
            """History is not empty: normal response"""
            for yandexuid in ['1001', '1002', '1003']:
                response = self.report.request_json(
                    'place=popular_products&rgb={rgb}&yandexuid={yandexuid}&numdoc=4&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                        rgb=rgb, yandexuid=yandexuid
                    )
                )
                self.assertFragmentIn(
                    response, {'search': {'results': POPULAR_PERSONAL_PRODUCTS}}, allow_different_len=False
                )
                response = self.report.request_json(
                    'place=deals&rgb={rgb}&filter-discount-only=1&show-personal=1&yandexuid={yandexuid}&numdoc=4&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                        rgb=rgb, yandexuid=yandexuid
                    )
                )
                self.assertFragmentIn(
                    response, {'search': {'results': DEALS_PERSONAL_PRODUCTS}}, allow_different_len=False
                )
            """No history and gender is male: default coldstart"""
            for yandexuid in ['1004']:
                response = self.report.request_json(
                    'place=popular_products&rgb={rgb}&yandexuid={yandexuid}&numdoc=4&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                        rgb=rgb, yandexuid=yandexuid
                    )
                )
                self.assertFragmentIn(response, {'search': {'results': PRODUCTS_DEFAULT}}, allow_different_len=False)
                response = self.report.request_json(
                    'place=deals&rgb={rgb}&filter-discount-only=1&show-personal=1&yandexuid={yandexuid}&numdoc=4&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                        rgb=rgb, yandexuid=yandexuid
                    )
                )
                self.assertFragmentIn(response, {'search': {'results': PRODUCTS_DEFAULT}}, allow_different_len=False)
        """No history, no profile or gender is female: women targeted coldstart"""
        for yandexuid in ['1006', '1005']:
            """rgb = 'green'"""
            response = self.report.request_json(
                'place=popular_products&yandexuid={yandexuid}&numdoc=4&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                    yandexuid=yandexuid
                )
            )
            self.assertFragmentIn(response, {'search': {'results': POPULAR_PRODUCTS_W}}, allow_different_len=False)
            response = self.report.request_json(
                'place=deals&filter-discount-only=1&show-personal=1&yandexuid={yandexuid}&numdoc=4&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                    yandexuid=yandexuid
                )
            )
            self.assertFragmentIn(response, {'search': {'results': DEALS_W}}, allow_different_len=False)

            """ rgb = 'blue'"""
            response = self.report.request_json(
                'place=popular_products&rgb=blue&yandexuid={yandexuid}&numdoc=4&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                    yandexuid=yandexuid
                )
            )
            self.assertFragmentIn(response, {'search': {'results': PRODUCTS_DEFAULT}}, allow_different_len=False)
            response = self.report.request_json(
                'place=deals&rgb=blue&filter-discount-only=1&show-personal=1&yandexuid={yandexuid}&numdoc=4&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                    yandexuid=yandexuid
                )
            )
            self.assertFragmentIn(response, {'search': {'results': PRODUCTS_DEFAULT}}, allow_different_len=False)


if __name__ == '__main__':
    main()

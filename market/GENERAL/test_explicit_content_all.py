#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    CategoryRestriction,
    CpaCategory,
    CpaCategoryType,
    Disclaimer,
    HyperCategory,
    Model,
    Offer,
    RegionalRestriction,
    Shop,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    # MARKETOUT-9059
    @classmethod
    def prepare_restrictions_for_bids_recommender(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[905901],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=True,
                        rids=[213],
                        disclaimers=[
                            Disclaimer(
                                name='medicine1',
                                text='Лекарство длинный текст',
                                short_text='Лекарство',
                            )
                        ],
                    ),
                ],
            )
        ]

        cls.index.hypertree += [
            HyperCategory(hid=905901, name='Лекарства'),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=905901, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        cls.index.models += [
            Model(
                hyperid=905901,
                title='Нимесил',
                hid=905901,
                # disclaimers_model='medicine1',
            )
        ]

        cls.index.shops += [
            Shop(fesh=905901, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=905902, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=905903, cpa=Shop.CPA_REAL, priority_region=213),
        ]

        cls.index.offers += [
            Offer(hyperid=905901, fesh=905901, cpa=Offer.CPA_REAL, price=5000, fee=521, bid=21),
            Offer(hyperid=905901, fesh=905902, cpa=Offer.CPA_REAL, price=5000, fee=522, bid=22),
            Offer(hyperid=905901, fesh=905903, cpa=Offer.CPA_REAL, price=5000, fee=523, bid=23),
        ]

    def test_bids_recommender_for_restricted(self):
        """
        Делаем запрос, и модель в ограниченной категории всё равно появляется
        (срабатывает неявный show_explicit_content=all)
        """
        response = self.report.request_xml('place=bids_recommender&fesh=905901&rids=213&hyperid=905901&type=card')
        print(response)
        expected_response = '''
        <search_results>
            <offers>
                <offer>
                    <raw-title/>
                    <hyper_id>905901</hyper_id>
                    <hidd>905901</hidd>
                    <price currency="RUR">5000</price>
                </offer>
            </offers>
        </search_results>
        '''
        self.assertFragmentIn(response, expected_response)


if __name__ == '__main__':
    main()

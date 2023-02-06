#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.5)

        cls.index.regiontree += [
            Region(rid=1, name='CPA city', region_type=Region.FEDERATIVE_SUBJECT),
            Region(rid=2, name='non CPA city', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=101, regions=[1], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION, fee=500),
        ]

        cls.index.shops += [
            # Locla shops
            Shop(fesh=201, priority_region=1, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=202, priority_region=1, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=203, priority_region=1, regions=[225], cpa=Shop.CPA_REAL),
            # Non-local shops
            Shop(fesh=211, priority_region=2, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=212, priority_region=2, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=213, priority_region=2, regions=[225], cpa=Shop.CPA_REAL),
        ]

        delivery_on_russia = [RegionalDelivery(rid=225, options=[DeliveryOption(price=0, day_from=5, day_to=10)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=211, fesh=211, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=212, fesh=212, carriers=[99], regional_options=delivery_on_russia),
            DeliveryBucket(bucket_id=213, fesh=213, carriers=[4], regional_options=delivery_on_russia),
        ]

    # TODO(richard): Add test for hybrid auction

    @classmethod
    def prepare_search(cls):
        # Local offers
        cls.index.offers += [
            Offer(title='local item', hid=102, fesh=201, bid=50),
            Offer(title='local item', hid=102, fesh=202, bid=30),
            Offer(title='local item', hid=102, fesh=203, bid=10),
        ]
        # Non-local offers
        cls.index.offers += [
            Offer(title='non local item', hid=102, fesh=211, bid=40, delivery_buckets=[211]),
            Offer(title='non local item', hid=102, fesh=212, bid=20, delivery_buckets=[212]),
            Offer(title='non local item', hid=102, fesh=213, bid=5, delivery_buckets=[213]),
        ]

    def test_search_local_shop(self):
        """
        Проверяем поисковые рекомендации с учётом локальных и не-локальных офферов
        для локального региона магазина.
        """
        response = self.report.request_xml(
            'place=bids_recommender' '&rids=1' '&type=market_search&text=item&text2=item' '&fesh=203'
        )
        self.assertFragmentIn(
            response,
            '''
        <search-recommendations>
            <position bid="2" code="0" fee="0" pos="1"/>
            <position bid="2" code="0" fee="0" pos="2"/>
            <position bid="2" code="0" fee="0" pos="3"/>
            <position bid="2" code="0" fee="0" pos="4"/>
            <position bid="2" code="0" fee="0" pos="5"/>
            <position bid="1" code="0" fee="0" pos="6"/>
            <position bid="1" code="0" fee="0" pos="7"/>
            <position bid="1" code="0" fee="0" pos="8"/>
            <position bid="1" code="0" fee="0" pos="9"/>
            <position bid="1" code="0" fee="0" pos="10"/>
            <position bid="1" code="0" fee="0" pos="11"/>
            <position bid="1" code="0" fee="0" pos="12"/>
        </search-recommendations>
        ''',
            preserve_order=True,
            allow_different_len=False,
        )

    def test_search_non_local_shop(self):
        """
        Проверяем поисковые рекомендации с учётом локальных и не-локальных офферов
        для не-локального региона магазина.
        """
        response = self.report.request_xml(
            'place=bids_recommender' '&rids=1' '&type=market_search&text=item&text2=item' '&fesh=213'
        )
        self.assertFragmentIn(
            response,
            '''
        <search-recommendations>
            <position bid="2" code="0" fee="0" pos="1"/>
            <position bid="2" code="0" fee="0" pos="2"/>
            <position bid="2" code="0" fee="0" pos="3"/>
            <position bid="2" code="0" fee="0" pos="4"/>
            <position bid="2" code="0" fee="0" pos="5"/>
            <position bid="1" code="0" fee="0" pos="6"/>
            <position bid="1" code="0" fee="0" pos="7"/>
            <position bid="1" code="0" fee="0" pos="8"/>
            <position bid="1" code="0" fee="0" pos="9"/>
            <position bid="1" code="0" fee="0" pos="10"/>
            <position bid="1" code="0" fee="0" pos="11"/>
            <position bid="1" code="0" fee="0" pos="12"/>
        </search-recommendations>
        ''',
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

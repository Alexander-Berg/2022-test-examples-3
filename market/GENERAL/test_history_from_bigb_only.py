#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Offer,
    Shop,
    YamarecFeaturePartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import main
from core.bigb import ModelLastSeenEvent, MarketModelLastTimeCounter, BeruModelViewLastTimeCounter
from simple_testcase import SimpleTestCase

history_from_recommender = '&rearr-factors=recom_history_from_bigb_only=0&debug=1'
history_from_bigb_without_beru = (
    '&rearr-factors=recom_history_from_bigb_only=1;recom_history_from_bigb_include_beru=0&debug=1'
)
history_from_bigb_with_beru = (
    '&rearr-factors=recom_history_from_bigb_only=1;recom_history_from_bigb_include_beru=1&debug=1'
)


class T(SimpleTestCase):
    '''
    Test some places that call Big B internally. It isn't exaustive testing.
    I'm counting on switching all ordinal tests to work with bigb mocks in the
    future. This tests expected to be deleted later.
    '''

    @classmethod
    def prepare(cls):
        '''
        hyperid=1..
        hid=2..
        vendor_id=3..
        yandexuid=4..
        region=5..
        fesh=6..
        price=7..
        '''

        cls.index.models += [
            Model(hyperid=100, hid=200, vendor_id=300),
            Model(hyperid=101, hid=200, vendor_id=301),
            Model(hyperid=102, hid=200, vendor_id=302),
            Model(hyperid=103, hid=201, vendor_id=300),
            Model(hyperid=104, hid=201, vendor_id=301),
            Model(hyperid=105, hid=201, vendor_id=302),
            Model(hyperid=106, hid=202, vendor_id=303),
            Model(hyperid=107, hid=202, vendor_id=303),
            Model(hyperid=108, hid=202, vendor_id=303),
            Model(hyperid=109, hid=202, vendor_id=303),
        ]

        cls.index.offers += [
            Offer(hyperid=100, fesh=600, discount=0),
            Offer(hyperid=101, fesh=600, discount=35),
            Offer(hyperid=102, fesh=600, discount=40),
            Offer(hyperid=103, fesh=600, discount=35),
            Offer(hyperid=104, fesh=600, discount=40),
            Offer(hyperid=107, fesh=600, discount=90),
        ]

        cls.index.shops += [
            Shop(fesh=600, priority_region=500),
            Shop(fesh=601, priority_region=500),
            Shop(fesh=602, priority_region=501),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=203,
                children=[HyperCategory(hid=child, output_type=HyperCategoryType.GURU) for child in range(200, 203)],
            ),
        ]

        cls.index.navtree += [
            NavCategory(hid=200),
            NavCategory(hid=201),
            NavCategory(hid=202),
            NavCategory(hid=203),
        ]

        cls.recommender.on_request_models_of_interest(user_id="yandexuid:400", item_count=1000).respond(
            {"models": ['100']}
        )

        cls.bigb.on_request(yandexuid='400', client='merch-machine').respond(
            counters=[
                MarketModelLastTimeCounter(
                    model_view_events=[
                        ModelLastSeenEvent(model_id=103, timestamp=0),
                    ]
                ),
                BeruModelViewLastTimeCounter(
                    model_view_events=[
                        ModelLastSeenEvent(model_id=106, timestamp=1),
                    ]
                ),
            ]
        )

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[],
                    ),
                ],
            )
        ]

    def test_personal_categories(self):
        '''
        Test `recom_history_from_bigb_only` and `recom_history_from_bigb_include_beru`
        rearr flags applied to personal categories place. Output should change with
        history source.
        '''

        personal_categories = 'place=personal_categories&yandexuid=400'

        response = self.report.request_json(personal_categories + history_from_recommender)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {'link': {'params': {'hid': '200'}}},
                    ],
                }
            },
        )

        response = self.report.request_json(personal_categories + history_from_bigb_without_beru)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {'link': {'params': {'hid': '201'}}},
                    ],
                }
            },
        )

        response = self.report.request_json(personal_categories + history_from_bigb_with_beru)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {'link': {'params': {'hid': '201'}}},
                        {'link': {'params': {'hid': '202'}}},
                    ],
                }
            },
        )

    def test_best_deal(self):
        '''
        Test `recom_history_from_bigb_only` and `recom_history_from_bigb_include_beru`
        rearr flags applied to best deal place. Output should change with history source.
        '''

        bestdeals = 'place=bestdeals&yandexuid=400'

        response = self.report.request_json(bestdeals + history_from_recommender)
        self.error_log.ignore("Personal category config is not available")
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {'entity': 'product', 'id': 101},
                        {'entity': 'product', 'id': 102},
                    ],
                }
            },
        )

        response = self.report.request_json(bestdeals + history_from_bigb_without_beru)
        self.error_log.ignore("Personal category config is not available")
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {'entity': 'product', 'id': 103},
                        {'entity': 'product', 'id': 104},
                    ],
                }
            },
        )

        response = self.report.request_json(bestdeals + history_from_bigb_with_beru)
        self.error_log.ignore("Personal category config is not available")
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {'entity': 'product', 'id': 103},
                        {'entity': 'product', 'id': 104},
                        {'entity': 'product', 'id': 107},
                    ],
                }
            },
        )

    @classmethod
    def prepare_disable_best_price_dedup(cls):

        cls.index.models += [
            Model(hyperid=110, hid=210, vendor_id=310),
            Model(hyperid=111, hid=210, vendor_id=310),
        ]

        cls.index.offers += [
            Offer(hyperid=110, fesh=610, price=733, discount=10),
            Offer(hyperid=111, fesh=610, discount=10),
        ]

        cls.index.shops += [
            Shop(fesh=610, priority_region=510),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=210, output_type=HyperCategoryType.GURU),
        ]

        cls.recommender.on_request_models_of_interest(user_id="yandexuid:410", item_count=1000).respond(
            {"models": ['110']}
        )

        cls.bigb.on_request(yandexuid='410', client='merch-machine').respond(
            counters=[
                MarketModelLastTimeCounter(
                    model_view_events=[
                        ModelLastSeenEvent(model_id=110, timestamp=0),
                    ]
                ),
            ]
        )

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:410", item_count=100).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 110, "price": 766.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

        cls.index.yamarec_places += [
            YamarecPlace(
                name="better-price",
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                        },
                        splits=[{"split": "1"}],
                    ),
                ],
            )
        ]

    def test_disable_best_price_dedup(self):
        '''
        Test `recom_disable_best_price_dedup` rearr-flag.  Without the flag
        model 110 should be filtered out by the better price filter.
        '''

        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=410&hid=210&rearr-factors=split=1;recom_disable_best_price_dedup=0'
        )
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [
                        {'subEntity': {'entity': 'product', 'id': 110}},
                    ]
                }
            },
        )

        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=410&hid=210&rearr-factors=split=1;recom_disable_best_price_dedup=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'subEntity': {'entity': 'product', 'id': 110}},
                    ]
                }
            },
        )

        # basically ignore all errors, we testing only difference between two requests
        self.error_log.ignore(category='REPORT')


if __name__ == '__main__':
    main()

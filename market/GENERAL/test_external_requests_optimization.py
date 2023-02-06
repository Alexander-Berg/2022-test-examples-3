#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    NewShopRating,
    Offer,
    Shop,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main


# List of google bots user-agents: https://support.google.com/webmasters/answer/1061943?hl=en
BOT_AGENTS = [
    'Googlebot',
    'AdsBot-Google (+http://www.google.com/adsbot.html)',
    'FeedFetcher-Google; (+http://www.google.com/feedfetcher.html)',
    'Googlebot-Image/1.0',
    'Googlebot/2.1 (+http://www.google.com/bot.html)',
    'Googlebot/2.1 (+http://www.googlebot.com/bot.html)',
    'Mediapartners-Google',
    'Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) '
    'Chrome/41.0.2272.96 Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)',
    'Mozilla/5.0 (compatible) Feedfetcher-Google; (+http://www.google.com/feedfetcher.html)',
    'Mozilla/5.0 (compatible; Googlebot/2.0; +http://www.google.com/bot.html)',
    'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)',
    'Mozilla/5.0 (compatible; Googlebot/2.1; startmebot/1.0; +https://start.me/bot)',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) '
    'Version/9.0 Mobile/13B143 Safari/601.1 (compatible; AdsBot-Google-Mobile; +http://www.google.com/mobile/adsbot.html)',
    'Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; Googlebot/2.1; +http://www.google.com/bot.html) Safari/537.36',
]

ENABLE_FILTER_PARAM = '&rearr-factors=disable_bot_filter=0'


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.bigb.on_request(yandexuid='', client='merch-machine').respond(counters=[], keywords=[])
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:', item_count=1000).respond({'models': []})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:').respond({'models': []})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:', item_count=40, with_timestamps=True
        ).respond({'models': [], 'timestamps': []})
        cls.recommender.on_request_accessory_models(model_id=190, item_count=1000, version='1').respond({'models': []})

        cls.index.hypertree += [
            HyperCategory(hid=335, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(hyperid=150, hid=335),
        ]
        # bestdeals
        cls.index.shops += [
            Shop(fesh=5, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=4.0), cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(hyperid=150, waremd5='RuDq59UhfHnD72tMISuMbw', fesh=5, price=1000, cpa=Offer.CPA_REAL, discount=5),
        ]
        # better_price
        cls.index.yamarec_places += [
            YamarecPlace(
                name='better-price',
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'filter-by-price': '1',
                        },
                        splits=[{}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{}]),
                ],
            ),
        ]
        cls.recommender.on_request_we_have_cheaper(
            user_id='yandexuid:',
            item_count=100,
        ).respond({'we_have_cheaper': []})

    def test_bot_filter(self):
        queries = [
            'place=popular_products&hid=335&rearr-factors=switch_popular_products_to_dj_no_nid_check=0',
            'place=bestdeals&hid=335',
            'place=personalcategorymodels&rearr-factors=split=personalcategorymodels',
            'place=products_by_history&rearr-factors=market_disable_dj_for_recent_findings%3D1',
            'place=better_price&rids=213&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0',
            'place=also_viewed&hyperid=190',
        ]
        for q in queries:
            blocked_filter_query = q + '&debug=1'
            query = blocked_filter_query + ENABLE_FILTER_PARAM
            for user_agent in BOT_AGENTS:
                headers = {'User-Agent': user_agent}
                response = self.report.request_json(query, headers=headers)
                self.assertFragmentIn(response, 'WARNING Skip external request: bot detected')
                response = self.report.request_json(blocked_filter_query, headers=headers)
                self.assertFragmentNotIn(response, 'bot detected')
        self.error_log.ignore('Personal category config is not available for user')
        self.error_log.ignore('Category filtration is turned off and vendor filtration is turned off')

    def test_503_for_googlebot(self):
        expected = (
            'Server error: 503 ({"error":{"code":"CALM_DOWN_BOT","message":"Stop bots (in case of a high rps)"}})'
        )
        queries = [
            'place=prime&hid=335',
        ]
        filter_param = '&rearr-factors=force_503_to_googlebot=1'
        for q in queries:
            query_debug = q + '&debug=1'
            query_debug_filtered = query_debug + filter_param

            # просто пользователь с запросом
            response = self.report.request_json(query_debug)
            self.assertFragmentIn(response, {"search": {"total": 2}})

            for user_agent in BOT_AGENTS:
                headers = {'User-Agent': user_agent}

                # бот без стопкран-флага
                response = self.report.request_json(query_debug, headers=headers)
                self.assertFragmentIn(response, {"search": {"total": 2}})

                try:
                    # бот со стопкран-флагом
                    response = self.report.request_json(query_debug_filtered, headers=headers)
                except RuntimeError as e:
                    self.assertTrue(str(e).startswith(expected))
                    self.error_log.expect(code=3043)
                    continue
                self.assertTrue(False)
        self.error_log.ignore(code=3043)


if __name__ == '__main__':
    main()

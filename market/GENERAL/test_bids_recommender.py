#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CpaCategory,
    CpaCategoryType,
    Currency,
    DynamicShop,
    ExchangeRate,
    HybridAuctionParam,
    HyperCategory,
    HyperCategoryType,
    MinBidsCategory,
    MinBidsModel,
    MnPlace,
    Model,
    ModelGroup,
    Offer,
    Picture,
    Region,
    Shop,
    TopQueries,
)
from core.types.picture import thumbnails_config
from core.testcase import TestCase, main
from core.matcher import NotEmpty

import base64
import re


class T(TestCase):

    title_regex = re.compile(r'(<raw-title>)[^<]*(</raw-title>)')

    @classmethod
    def drop_title_if_needed(cls, fragment):
        if not cls.settings.disable_snippet_request:
            return fragment
        return cls.title_regex.sub(r'\1\2', fragment)

    @classmethod
    def optional_title(cls, title):
        return title if not cls.settings.disable_snippet_request else ''

    @classmethod
    def prepare(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.05)

        cls.index.regiontree += [
            Region(rid=1, name='CPA city', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.index.hypertree += [HyperCategory(hid=101, output_type=HyperCategoryType.GURU)]

        cls.index.shops += [
            Shop(fesh=201, priority_region=1),
        ]

        cls.index.offers += [
            Offer(title="some text", hid=101, hyperid=301, bid=60, fesh=201, cmagic='ffffffffffffffffffffffffffffffff'),
        ]

        # Offers without pulling to min bid
        cls.index.offers += [
            Offer(hyperid=303, bid=50, fesh=201, price=1, pull_to_min_bid=False),
        ]

    @classmethod
    def prepare_search_auction(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.hypertree += [
            HyperCategory(hid=301, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=302, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hid=302, hyperid=301002, title="search cpc or cpa auction"),
        ]

        for seq in range(1, 6):
            cls.index.shops += [
                Shop(fesh=300 + seq, priority_region=213),
                Shop(fesh=1000 + 300 + seq, priority_region=2),
            ]

            cls.index.offers += [
                # CPA offers in CPA category
                Offer(
                    title="search cpa auction",
                    hid=301,
                    bid=seq * 10,
                    fesh=300 + seq,
                    feedid=300 + seq,
                    offerid=seq,
                    popular_queries_all=[
                        TopQueries('search cpa auction', 10, 1, 1.5),
                        TopQueries('Молоток резиновый', 44, 12, 3.3),
                    ],
                    popular_queries_offer=[
                        TopQueries('search cpa', 55, 22, 1.5),
                        TopQueries('Молоток резиновый', 44, 12, 3.3),
                    ],
                ),
                # CPC offers in CPC category
                Offer(
                    title="search cpc auction",
                    hid=302,
                    bid=seq * 10,
                    fesh=300 + seq,
                    feedid=1000 + 300 + seq,
                    offerid=1000 + 300 + seq,
                ),
                # CPC offers in CPA category
                Offer(
                    title="supersearch cpa auction",
                    hid=301,
                    bid=seq * 10,
                    fesh=1000 + 300 + seq,
                    feedid=10000 + 300 + seq,
                    offerid=10000 + 300 + seq,
                ),
                # CPA offers in CPA category, offers matched to model cards - check for warning
                Offer(
                    title="ultrasearch model matched",
                    hid=301,
                    fesh=300 + seq,
                    feedid=20000 + 300 + seq,
                    offerid=20000 + 300 + seq,
                    hyperid=200300,
                ),
            ]

        # To check non-CPA offer in CPA auction - warning should be shown
        cls.index.shops += [
            Shop(fesh=310, priority_region=213),
        ]

        cls.index.offers += [
            Offer(title="search cpa auction", hid=301, bid=50, fesh=310, feedid=2000, offerid=2000),
        ]

        # Add offer without delivery options (they will be pessimized to local tail on KM)
        cls.index.shops += [
            Shop(fesh=306, priority_region=2, regions=[213]),
            Shop(fesh=307, priority_region=2, regions=[213]),
        ]

        cls.index.offers += [
            Offer(title="search cpc auction", hid=302, bid=80, fesh=306, feedid=306, offerid=6),
            Offer(title="search cpc auction", hid=302, bid=70, fesh=307, feedid=307, offerid=7),
        ]

    @classmethod
    def prepare_offer_incut_data(cls):
        pic = Picture(width=100, height=100, thumb_mask=thumbnails_config.get_mask_by_names(['100x100']), group_id=1234)
        cls.index.shops += [
            Shop(fesh=400, priority_region=213),
            Shop(fesh=402, priority_region=213),
            Shop(fesh=403, priority_region=213),
            Shop(fesh=404, priority_region=213),
            Shop(fesh=405, priority_region=213),
            Shop(fesh=406, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                title='pelmen-1',
                picture=pic,
                fesh=400,
                ts=1,
                url="http://pelmennaya.ru/pelmens?id=1",
                hid=12345,
                bid=100,
                feedid=801,
                offerid=1,
            ),
            Offer(
                title='pelmen-2',
                picture=pic,
                fesh=401,
                ts=2,
                url="http://pelmennaya.ru/pelmens?id=2",
                hid=12345,
                bid=200,
                feedid=802,
                offerid=1,
            ),
            Offer(
                title='pelmen-3',
                picture=pic,
                fesh=402,
                ts=3,
                url="http://pelmennaya.ru/pelmens?id=3",
                hid=12345,
                bid=300,
                feedid=803,
                offerid=1,
            ),
            Offer(
                title='pelmen-4',
                picture=pic,
                fesh=403,
                ts=4,
                url="http://pelmennaya.ru/pelmens?id=4",
                hid=12345,
                bid=400,
                feedid=804,
                offerid=1,
            ),
            Offer(
                title='shashlyik-1',
                picture=pic,
                fesh=404,
                ts=5,
                url="http://pelmennaya.ru/pelmens?id=4",
                hid=12345,
                bid=500,
                feedid=805,
                offerid=1,
            ),
            Offer(
                title='pelmen-6 other hid',
                picture=pic,
                fesh=405,
                ts=6,
                url="http://pelmennaya.ru/pelmens?id=4",
                hid=12346,
                bid=600,
                feedid=806,
                offerid=1,
            ),
            Offer(
                title='pelmen_no_pic',
                no_picture=True,
                fesh=406,
                ts=7,
                url="http://pelmennaya.ru/pelmens?id=4",
                hid=12345,
                bid=700,
                feedid=807,
                offerid=1,
            ),
        ]

        # Check case with many relevant offers

        for seq in range(1, 30):
            cls.index.shops += [Shop(fesh=2000 + seq, priority_region=213)]

            cls.index.offers += [
                Offer(
                    title="incut-%s" % seq,
                    no_picture=seq % 2,
                    hid=12347,
                    fesh=2000 + seq,
                    bid=10 + seq,
                    feedid=3000 + seq,
                    offerid=seq,
                )
            ]

    def test_actual_values(self):
        response = self.report.request_xml('place=bids_recommender&fesh=201&rids=1&hyperid=301')
        self.assertFragmentIn(response, '<offer><bids bid="60" pull_to_min_bid="true"/></offer>')

    def test_dynamic_filter(self):
        """
        Проверяем, что основной оффер будет найден и рекомендации будут, даже если он динамически отключен.
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(201)]
        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(201)]
        response = self.report.request_xml('place=bids_recommender&fesh=201&rids=1&hyperid=301')
        self.assertFragmentIn(response, '<offer/>')
        self.assertFragmentIn(response, '<recommendations/>')

    def test_not_pull_to_min_bid_high_bid(self):
        """
        Проверяем  наличие флага "не поднимать ставку до минимальной" для оффера с флагом "не поднимать".
        Ставка оффера достаточно высокая, что бы он не отфильтровывался.
        """
        response = self.report.request_xml('place=bids_recommender&fesh=201&rids=1&hyperid=303')
        self.assertFragmentIn(response, '<offer><bids bid="50" pull_to_min_bid="false"/></offer>')

    def test_min_values(self):
        response = self.report.request_xml('place=bids_recommender&fesh=201&rids=1&hyperid=301')
        self.assertFragmentIn(response, '<recommendations min-bid="1" />')

    def test_types(self):
        # Legacy output
        response = self.report.request_xml('place=bids_recommender&fesh=201&rids=1&hyperid=301')
        self.assertFragmentIn(response, '<card-recommendations/>')
        self.assertFragmentNotIn(response, '<search-recommendations/>')
        self.assertEqual(response.count('<position/>'), 26)

        # Modern output
        response = self.report.request_xml('place=bids_recommender&fesh=201&rids=1&hyperid=301&type=card')
        self.assertFragmentIn(response, '<card-recommendations/>')
        self.assertFragmentNotIn(response, '<search-recommendations/>')

        response = self.report.request_xml(
            'place=bids_recommender&fesh=201&rids=1&hyperid=301&text=some+text&type=market_search'
        )
        self.assertFragmentNotIn(response, '<card-recommendations/>')
        self.assertFragmentIn(response, '<search-recommendations/>')

        response = self.report.request_xml(
            'place=bids_recommender&fesh=201&rids=1&hyperid=301&text=some+text&type=card,card_cpa,market_search'
        )
        self.assertFragmentIn(response, '<search_results invalid-user-cgi="1"/>')
        self.error_log.expect(code=3043)

    def test_missing_pp(self):
        self.report.request_xml('place=bids_recommender&fesh=201&rids=1&hyperid=301&ip=127.0.0.1', add_defaults=False)

    def test_search_auction_by_cpc_simple(self):
        '''
        Рекоммендации по bid.
        Проверяем что есть рекоммендации по bid для позиций выше current-pos-all,
        а для последующих - в рекоммендациях минставка.
        (при рекоммендациях с type=market_search офферы без СиС не пессимизируются, т.к. они не пессимизируются на поиске
         локальные офферы также конкурируют с офферами без СиС, например с (feedid=306, fesh=306) из неприоритетного региона)

        Проверяем, что все факторы заполнены правильно и используется нужная формула релевантности.
        В дебаг выдаче рекоммендаций предыдущего тесткейса находим документ с той же ставкой и проверяем,
        что AUCTION_MULTIPLIER такой же как и для рассчитанный для этого же предложения в поисковом запросе
        к place=prime (в запросе не нужно указывать фильтрацию по магазину и feed_shoffer_id).

        Проверяем, что в для категории данного предложения (302) нашлась одна модель (model-count параметр)
        '''

        response = self.report.request_xml(
            'place=bids_recommender&fesh=301&rids=213&feed_shoffer_id=1301-1301&text=search+cpc+auction&type=market_search'
            '&debug=1&debug-doc-count=100'
        )
        self.assertFragmentNotIn(response, '<card-recommendations/>')
        self.assertFragmentIn(
            response,
            '''
        <search-recommendations current-pos-all="7" model-count="1">
            <position bid="81" code="0" pos="1">
                <offer-debug-info bid="80" shop="306"/>
            </position>
            <position bid="71" code="0" pos="2"/>
            <position bid="51" code="0" pos="3"/>
            <position bid="41" code="0" pos="4"/>
            <position bid="31" code="0" pos="5"/>
            <position bid="21" code="0" pos="6"/>
            <position bid="1" code="0" pos="7"/>
            <position bid="1" code="0" pos="8"/>
            <position bid="1" code="0" pos="9"/>
            <position bid="1" code="0" pos="10"/>
            <position bid="1" code="0" pos="11"/>
            <position bid="1" code="0" pos="12"/>
        </search-recommendations>
        ''',
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            '''
        <properties>
            <AUCTION_MULTIPLIER value="1.010973096"/>
            <BID value="10"/>
        </properties>
        ''',
            preserve_order=False,
        )

        # pruning anf quorum for both offer and search recommendations
        self.assertTrue(
            response.count('<value>qspWordWidth:0.3,PrunCount:10000.0,FadeCount:500.0,LoWordLerp:0.1</value>') == 2
        )

        self.assertTrue(response.count('precalculated_factors') > 0)

        # check we have set correct factor prerequisites
        response = self.report.request_json(
            'place=prime&rids=213&text=search+cpc+auction&type=market_search' '&debug=1&debug-doc-count=100'
        )
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "AUCTION_MULTIPLIER": "1.010973096",
                    "BID": "10",
                }
            },
            preserve_order=True,
        )
        self.assertTrue(str(response).find('precalculated_factors') > 0)

    def test_search_auction_by_cpc_delivery_options_for_non_local_offer(self):
        """
        Проверяем, что оффер  (feedid=306, fesh=306) из не приоритетного региона,
        несмотря на то что он не имеет СиС, может занять верхние места (и занимает первое место прямо сейчас)
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=306&rids=213&feed_shoffer_id=306-6&text=search+cpc+auction&type=market_search'
        )

        self.assertFragmentIn(
            response,
            '''
            <search-recommendations current-pos-all="1" model-count="1">
                <position bid="71" code="0" pos="1"/>
                <position bid="51" code="0" pos="2"/>
                <position bid="41" code="0" pos="3"/>
                <position bid="31" code="0" pos="4"/>
                <position bid="21" code="0" pos="5"/>
                <position bid="11" code="0" pos="6"/>
                <position bid="1" code="0" pos="7"/>
                <position bid="1" code="0" pos="8"/>
                <position bid="1" code="0" pos="9"/>
                <position bid="1" code="0" pos="10"/>
                <position bid="1" code="0" pos="11"/>
                <position bid="1" code="0" pos="12"/>
            </search-recommendations>
        ''',
        )

    def test_search_auction_simple_warnings_match_model(self):
        '''
        Рекоммендаций по fee больше нет.
        Проверяем что в Питере есть рекоммендации для не CPA предложения
        и вернулось предупреждение
        '''
        response = self.report.request_xml(
            'place=bids_recommender&fesh=301&rids=213&feed_shoffer_id=20301-20301&text=ultrasearch+model+matched&type=market_search'
            '&debug=1&debug-doc-count=100'
        )
        self.assertFragmentNotIn(response, '<card-recommendations/>')
        self.assertFragmentIn(response, '<warnings matched-to-model="true"/>')
        self.assertFragmentIn(
            response,
            '''
        <search-recommendations>
            <position code="2" pos="1"/>
        </search-recommendations>
        ''',
            preserve_order=True,
        )

    def test_offer_incut(self):
        '''
        Рекоммендации для параллельного (офферная врезка).
        Проверяем, что офферы отсортированы по bid-y, есть рекомендации для 2 позиций,
        остальные 4 оффера отфильтрованы по тексту, категории, региону и отсутствию картинки.
        Для последующих позиций - указываем мин. ставку.
        '''
        response = self.report.request_xml(
            'place=bids_recommender&fesh=400&rids=213&feed_shoffer_id=801-1&text=pelmen&type=parallel_search'
        )
        self.assertFragmentNotIn(response, '<card-recommendations/>')
        self.assertFragmentNotIn(response, '<search-recommendations/>')
        self.assertFragmentIn(
            response,
            '''
        <parallel-search-recommendations current-pos-all="3">
            <position bid="401" code="0" pos="1"/>
            <position bid="301" code="0" pos="2"/>
            <position bid="1" code="0" pos="3"/>
            <position bid="1" code="0" pos="4"/>
            <position bid="1" code="0" pos="5"/>
            <position bid="1" code="0" pos="6"/>
            <position bid="1" code="0" pos="7"/>
            <position bid="1" code="0" pos="8"/>
            <position bid="1" code="0" pos="9"/>
        </parallel-search-recommendations>
        ''',
            preserve_order=True,
        )

    def test_prun(self):
        response = self.report.request_xml(
            'place=bids_recommender&fesh=400&rids=213&feed_shoffer_id=801-1&text=pelmen&type=parallel_search&debug=1'
        )
        # Real prun count provided to basesearch is 2/3 of amount at meta-search.
        self.assertFragmentIn(response, 'pron=pruncount334')

    def test_offer_incut_no_pics_filtering(self):
        '''
        Рекоммендации для параллельного (офферная врезка).
        Проверяем, что офферы без картинок отфильтровываются из выдачи.
        '''
        response = self.report.request_xml(
            'place=bids_recommender&fesh=2001&rids=213&feed_shoffer_id=3001-1&text=incut&type=parallel_search'
        )
        self.assertFragmentNotIn(response, '<card-recommendations/>')
        self.assertFragmentNotIn(response, '<search-recommendations/>')
        self.assertFragmentIn(
            response,
            '''
        <parallel-search-recommendations>
            <position bid="39" code="0" pos="1"/>
            <position bid="37" code="0" pos="2"/>
            <position bid="35" code="0" pos="3"/>
            <position bid="33" code="0" pos="4"/>
            <position bid="31" code="0" pos="5"/>
            <position bid="29" code="0" pos="6"/>
            <position bid="27" code="0" pos="7"/>
            <position bid="25" code="0" pos="8"/>
            <position bid="23" code="0" pos="9"/>
        </parallel-search-recommendations>
        ''',
            preserve_order=True,
        )

    def test_top_queries_recommendations(self):
        '''
        Рекоммендации для market_search по популярным запросам (MARKETOUT-12568)
        Для кажого оффера feedid 30* заполнили список top популярных текстовых запросов
        Включаем show-top-queries=1, Проверяем, что в выдаче есть рекомендации для
        релевантных TopQueries ("search cpa auction", "search cpa")
        '''

        response = self.report.request_xml(
            'place=bids_recommender&fesh=301&rids=213&feed_shoffer_id=301-1&type=market_search' '&show-top-queries=1'
        )

        self.assertFragmentIn(
            response,
            '''
        <top-queries-recommendations>
            <query type="top_all">
                <text>search cpa auction</text>
                <search-recommendations current-pos-all="6">
                    <position bid="51" code="0" pos="1"/>
                    <position bid="51" code="0" pos="2"/>
                    <position bid="41" code="0" pos="3"/>
                    <position bid="31" code="0" pos="4"/>
                    <position bid="21" code="0" pos="5"/>
                    <position bid="1" code="0" pos="6"/>
                    <position bid="1" code="0" pos="7"/>
                    <position bid="1" code="0" pos="8"/>
                    <position bid="1" code="0" pos="9"/>
                    <position bid="1" code="0" pos="10"/>
                </search-recommendations>
            </query>
            <query type="top_offer">
                <text>search cpa</text>
                <search-recommendations current-pos-all="6">
                    <position bid="51" code="0" pos="1"/>
                    <position bid="51" code="0" pos="2"/>
                    <position bid="41" code="0" pos="3"/>
                    <position bid="31" code="0" pos="4"/>
                    <position bid="21" code="0" pos="5"/>
                    <position bid="1" code="0" pos="6"/>
                    <position bid="1" code="0" pos="7"/>
                    <position bid="1" code="0" pos="8"/>
                    <position bid="1" code="0" pos="9"/>
                    <position bid="1" code="0" pos="10"/>
                </search-recommendations>
            </query>
        </top-queries-recommendations>
        ''',
            preserve_order=True,
        )

    @classmethod
    def prepare_hybrid_auction_offers(cls):
        cls.index.currencies += [
            Currency(name=Currency.UE, exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=0.0333333)])
        ]

        cls.index.hybrid_auction_settings += [
            HybridAuctionParam(
                category=90401,
                cpc_ctr_for_cpc=0.1,
                cpc_ctr_for_cpc_msk=0.1,
            )
        ]

        cls.index.hypertree += [HyperCategory(hid=501, output_type=HyperCategoryType.GURU)]

        for i in range(1, 11):
            cls.index.shops += [Shop(fesh=5000 + i, priority_region=213)]
        # Add offer without delivery options for being pessimized to local_tail
        cls.index.shops += [
            Shop(fesh=5011, priority_region=2, regions=[213]),
            Shop(fesh=5012, priority_region=2, regions=[213]),
        ]

        cls.index.offers += [
            Offer(hyperid=5001, hid=501, fesh=5001, price=10000, bid=50, ts=1),
            Offer(hyperid=5001, hid=501, fesh=5002, price=10000, bid=52, ts=2),
            Offer(hyperid=5001, hid=501, fesh=5003, price=10000, bid=15, ts=3),
            Offer(hyperid=5001, hid=501, fesh=5004, price=10000, bid=40, ts=4),
            Offer(hyperid=5001, hid=501, fesh=5005, price=10000, bid=20, ts=5),
            Offer(hyperid=5001, hid=501, fesh=5006, price=10000, bid=19, ts=6),
            Offer(hyperid=5001, hid=501, fesh=5007, price=10000, bid=18, ts=7),
            Offer(hyperid=5001, hid=501, fesh=5008, price=10000, bid=17, ts=8),
            Offer(hyperid=5001, hid=501, fesh=5009, price=10000, bid=16, ts=9),
            Offer(hyperid=5001, hid=501, fesh=5010, price=10000, bid=15, ts=10),
            Offer(hyperid=5001, hid=501, fesh=5011, price=10000, bid=150, ts=11),
        ]
        cls.index.offers += [
            Offer(hyperid=5002, hid=501, fesh=5001, price=10000, bid=50, ts=1),
            Offer(hyperid=5002, hid=501, fesh=5002, price=10000, bid=52, ts=2),
            Offer(hyperid=5002, hid=501, fesh=5003, price=10000, bid=15, ts=3),
            Offer(hyperid=5002, hid=501, fesh=5004, price=10000, bid=30, ts=4),
            Offer(hyperid=5002, hid=501, fesh=5005, price=10000, bid=20, ts=5),
            Offer(hyperid=5002, hid=501, fesh=5011, price=10000, bid=150, ts=11),
            Offer(hyperid=5002, hid=501, fesh=5012, price=10000, bid=100, ts=12),
        ]
        cls.index.offers += [
            Offer(hyperid=5003, hid=501, fesh=5001, price=10000, bid=2, pull_to_min_bid=False),
            Offer(hyperid=5004, hid=501, fesh=5001, price=10000, bid=2, pull_to_min_bid=True),
        ]

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=1,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
            MinBidsCategory(
                category_id=90401,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
            MinBidsCategory(
                category_id=1,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
            MinBidsCategory(
                category_id=90401,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
        ]

        cls.index.min_bids_model_stats += [
            MinBidsModel(
                model_id=1000,
                geo_group_id=0,
                drr=0.01,
                search_clicks=1,
                search_orders=3,
                card_clicks=144,
                card_orders=9,
                full_card_clicks=0,
                full_card_orders=0,
            )
        ]

    def test_original_shop_bids_in_offer_info_partner_api(self):
        '''
        MARKETOUT-20604
        Не подтягивать ставки до минимальных в информации об оффере (если это запрещено)
        Тест для партнерского АПИ
        '''

        response = self.report.request_xml(
            'place=bids_recommender&fesh=5001&rids=213&hyperid=5003&type=card&api=partner'
        )
        self.assertFragmentIn(response, '<offer><bids bid="2" /></offer>')

        response = self.report.request_xml(
            'place=bids_recommender&fesh=5001&rids=213&hyperid=5004&type=card&api=partner'
        )
        self.assertFragmentIn(response, '<offer><bids bid="13" /></offer>')

    def test_original_shop_bids_in_offer_info_partner_interface(self):
        '''
        MARKETOUT-20604
        Не подтягивать ставки до минимальных в информации об оффере (если это запрещено)
        Тест для партнерского интерфейса
        '''

        response = self.report.request_xml(
            'place=bids_recommender&fesh=5001&rids=213&hyperid=5003&type=card&client=partnerinterface'
        )
        self.assertFragmentIn(response, '<offer><bids bid="2" /></offer>')

        response = self.report.request_xml(
            'place=bids_recommender&fesh=5001&rids=213&hyperid=5004&type=card&client=partnerinterface'
        )
        self.assertFragmentIn(response, '<offer><bids bid="13" /></offer>')

    def test_numdoc(self):
        response = self.report.request_xml(
            'place=bids_recommender&fesh=5011&rids=213&hyperid=5002&type=card&recommendations-count=50'
        )
        self.assertFragmentIn(
            response,
            '''
            <position pos="50"/>
        ''',
        )

        # We have 2 output block by 50 position and top-6 block, so 106 positions in total should be returned
        self.assertEqual(106, response.count('<position />'))

    @classmethod
    def prepare_same_cpm(cls):
        cls.index.offers += [
            Offer(hyperid=5401, hid=501, fesh=5001, price=10000, bid=53, ts=401),
            Offer(hyperid=5401, hid=501, fesh=5002, price=10000, bid=53, ts=402),
            Offer(hyperid=5401, hid=501, fesh=5003, price=10000, bid=53, ts=403),
        ]

    def test_same_cpm(self):
        """
        Проверяем, что при рекоммендуется ставка, гарантирована перебивающая CPM следующего оффера.
        53 - взято из реального бага, при этом значении рекоммендуется ровно 53 для равенства CPM.
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=5003&rids=213&hyperid=5401&type=card&rearr-factors=market_uncollapse_supplier=0'
        )
        self.assertFragmentIn(
            response,
            '''
            <card-recommendations>
                <position bid="54" code="0" pos="1"/>
                <position bid="54" code="0" pos="2"/>
            </card-recommendations>
        ''',
            preserve_order=True,
        )

    @classmethod
    def prepare_local_top6(cls):
        cls.index.offers += [
            # Local offers
            Offer(hyperid=10001, hid=501, fesh=5001, price=10000, bid=50),
            Offer(hyperid=10001, hid=501, fesh=5002, price=10000, bid=52),
            Offer(hyperid=10001, hid=501, fesh=5003, price=10000, bid=15),
            Offer(hyperid=10001, hid=501, fesh=5004, price=10000, bid=40),
            Offer(hyperid=10001, hid=501, fesh=5010, price=10000, bid=15),
            # Non-local offer (but still in top-6)
            Offer(hyperid=10001, hid=501, fesh=5011, price=10000, bid=150),
        ]

    def test_last_local_in_top6(self):
        """
        Проверяем, что для локального оффера для попадания в топ-6 на позиции 4-6 всегда возвращается
        не менее порогового значения:
        - если он подперт локальным (4ая позиция)
        - если он подперт не-локальным (5ая позиция)
        - если он не подперт вообще (6ая позиция)
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=5010&rids=213&hyperid=10001&type=card&rearr-factors=market_uncollapse_supplier=0'
        )
        self.assertFragmentIn(
            response,
            '''
            <card-top-recommendations>
                <position bid="41" pos="4"/>
                <position bid="41" pos="5"/>
                <position bid="41" pos="6"/>
            </card-top-recommendations>
        ''',
            preserve_order=True,
        )

    @classmethod
    def prepare_search_auction_new_min_bid(cls):
        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=10300,
                geo_group_id=0,
                price_group_id=0,
                drr=0.01,
                search_conversion=0.01,
                card_conversion=1.0,
                full_card_conversion=1.0,
            ),
        ]

        for seq in range(6):
            cls.index.shops += [
                Shop(fesh=10300 + seq, priority_region=213),
            ]

            cls.index.offers += [
                # CPC offers in CPC category, with big new min bid
                Offer(
                    title="alloha",
                    hid=302,
                    bid=20 + seq * 10,
                    price=20000 + 1000 * seq,
                    fesh=10300 + seq,
                    feedid=30000 + 300 + seq,
                    offerid=10000 + 300 + seq,
                    ts=10000 + seq,
                ),
                # CPC offers in CPC category, with small new min bid
                Offer(
                    title="hawaii",
                    hid=10300,
                    bid=20 + seq * 10,
                    price=20000 + 1000 * seq,
                    fesh=10300 + seq,
                    feedid=40000 + 300 + seq,
                    offerid=20000 + 300 + seq,
                    ts=20000 + seq,
                ),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10000 + seq).respond(0.01 + 0.0001 * seq)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 20000 + seq).respond(0.01 + 0.0001 * seq)
        # Very low CTR offer
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10000).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 20000).respond(0.001)

    def test_search_auction_new_min_bid_big(self):
        """
        Проверяем работу рекомендатора для поиска, новая мин.ставка больше старой
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=10301&feed_shoffer_id=30301-10301&rids=213&text=alloha&type=market_search'
            '&rearr-factors=market_tweak_search_auction_params=0.34,0.04,1.0'
        )
        # 1st: MN=0.0105, AuctionMult=1.135            -> 0.0119175
        # MyMN=0.0101, MAX_BID grants AuctionMult=1.17 -> 0.011817 = unreacheable
        # 2nd: MN=0.0104, AuctionMult=1.120        -> 0.011648
        # MyMN=0.0101, 90 grants AuctionMult=1.154 -> 0.0116554
        # 3rd: MN=0.0103, AuctionMult=1.100         -> 0.01133
        # MyMN=0.0101, 61 grants AuctionMult=1.1234 -> 0.01134634
        # 4th: MN=0.0102, AuctionMult=1.079        -> 0.0110058
        # MyMN=0.0101, 45 grants AuctionMult=1.091 -> 0.0110191
        # 5th: MN=0.0010, AuctionMult=1.037            -> 0.001037
        # MyMN=0.0101, min bid grants AuctionMult=1.04 -> 0.010504
        # 6th: min bid
        self.assertFragmentIn(
            response,
            '''
            <search-recommendations>
                <position code="2" pos="1"/>
                <position bid="90" pos="2"/>
                <position bid="61" pos="3"/>
                <position bid="45" pos="4"/>
                <position bid="27" pos="5"/>
                <position bid="27" pos="6"/>
            </search-recommendations>
        ''',
            preserve_order=True,
        )

    def test_search_auction_new_min_bid_small(self):
        """
        Проверяем работу рекомендатора для поиска, новая мин.ставка больше старой
        Если достаточно старой мин. ставки (т.е. усиление тсавкой не нужно), то достаточно и новой мин. ставки
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=10301&feed_shoffer_id=40301-20301&rids=213&text=hawaii&type=market_search'
            '&rearr-factors=market_tweak_search_auction_params=0.34,0.04,1.0'
        )
        # 1st: MN=0.0105, AuctionMult=1.135            -> 0.0119175
        # MyMN=0.0101, MAX_BID grants AuctionMult=1.17 -> 0.011817 = unreacheable
        # 2nd: MN=0.0104, AuctionMult=1.120        -> 0.011648
        # MyMN=0.0101, 90 grants AuctionMult=1.154 -> 0.0116554
        # 3rd: MN=0.0103, AuctionMult=1.100         -> 0.01133
        # MyMN=0.0101, 61 grants AuctionMult=1.1234 -> 0.01134634
        # 4th: MN=0.0102, AuctionMult=1.079        -> 0.0110058
        # MyMN=0.0101, 45 grants AuctionMult=1.091 -> 0.0110191
        # 5th: MN=0.0010, AuctionMult=1.037         -> 0.001037
        # MyMN=0.0101, min bid grants AuctionMult=1 -> 0.0101
        # It is importnat that new min bid (7) is recommended, not old min bid (15)
        # 6th: min bid
        self.assertFragmentIn(
            response,
            '''
            <search-recommendations>
                <position code="2" pos="1"/>
                <position bid="90" pos="2"/>
                <position bid="61" pos="3"/>
                <position bid="45" pos="4"/>
                <position bid="7" pos="5"/>
                <position bid="7" pos="6"/>
            </search-recommendations>
        ''',
            preserve_order=True,
        )

    @classmethod
    def prepare_cpm_rounding_error_for_recommendation(cls):
        cls.index.shops += [
            Shop(fesh=500, regions=[213]),  # LQ shop
            Shop(fesh=501, regions=[213]),  # HQ shop
        ]
        cls.index.offers += [
            Offer(fesh=500, hyperid=500, hid=500, bid=12, title='LQ shop', price=1000, randx=1),
            Offer(fesh=501, hyperid=500, hid=500, bid=12, title='HQ shop', price=1000, randx=0),
        ]

    def test_cpm_rounding_error_for_recommendation(self):
        """
        Check if CPM rounding error doesn't cause wrong recommendations
        See https://st.yandex-team.ru/MARKETOUT-16410
        """

        # Check thath CPM difference doesn't matter in 'productoffers'
        response = self.report.request_json('place=productoffers&hyperid=500&pp=6')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "LQ shop"}},
                    {"entity": "offer", "titles": {"raw": "HQ shop"}},
                ]
            },
            preserve_order=True,
        )

        # Check that CPM difference doesn't matter in recommendation
        response = self.report.request_xml(
            'place=bids_recommender&fesh=501&hyperid=500&rids=213&type=card&rearr-factors=market_uncollapse_supplier=0'
        )

        self.assertFragmentIn(
            response,
            '''
            <card-recommendations current-pos-all="2">
                <position bid="13" code="0" pos="1"/>
            </card-recommendations>
        ''',
        )

    @classmethod
    def prepare_test_card_recommendations(cls):
        cls.index.shops += [
            Shop(fesh=980, regions=[213]),
            Shop(fesh=981, regions=[213]),
            Shop(fesh=982, regions=[213]),
            Shop(fesh=983, regions=[213]),
            Shop(fesh=984, regions=[213]),
            Shop(fesh=985, regions=[213]),
            Shop(fesh=986, regions=[213]),
            Shop(fesh=987, regions=[213]),
            Shop(fesh=988, regions=[213]),
            Shop(fesh=987, regions=[213]),
            Shop(fesh=988, regions=[213]),
            Shop(fesh=989, regions=[213]),
            Shop(fesh=990, regions=[213]),
        ]

        cls.index.models += [
            Model(hid=54321, hyperid=654321),
        ]

        cls.index.offers += [
            Offer(hid=54321, hyperid=654321, bid=60, price=100, fesh=980),
            Offer(hid=54321, hyperid=654321, bid=50, price=100, fesh=981),
            Offer(hid=54321, hyperid=654321, bid=40, price=100, fesh=982),
            Offer(hid=54321, hyperid=654321, bid=30, price=100, fesh=983),
            Offer(hid=54321, hyperid=654321, bid=20, price=100, fesh=984),
            Offer(hid=54321, hyperid=654321, bid=10, price=100, fesh=985),
            Offer(hid=54321, hyperid=654321, bid=9, price=100, fesh=986),
            Offer(hid=54321, hyperid=654321, bid=8, price=100, fesh=987),
            Offer(hid=54321, hyperid=654321, bid=7, price=100, fesh=988),
            Offer(hid=54321, hyperid=654321, bid=6, price=100, fesh=989),
            Offer(hid=54321, hyperid=654321, bid=5, price=100, fesh=990),
        ]

    def test_card_recommendations(self):
        '''
        Проверяем, что запрос рекомендаций на карточке возвращает 3 блока:
            card-recommendations, card-top-recommendations, card-price-recommentations
        '''

        response = self.report.request_xml(
            'place=bids_recommender&fesh=990&hyperid=654321&rids=213&rearr-factors=market_ha_threshold_mult=6;market_uncollapse_supplier=0'
        )

        # Проверяем ставки. Посчитанный threshold здесь должен быть 25, с учетом выставленного threshold_mult в 6.
        self.assertFragmentIn(
            response,
            '''
        <card-top-recommendations top-offers-count="4">
            <position bid="61" code="0" pos="1"/>
            <position bid="51" code="0" pos="2"/>
            <position bid="41" code="0" pos="3"/>
            <position bid="31" code="0" pos="4"/>
            <position bid="25" code="0" pos="5"/>
            <position bid="25" code="0" pos="6"/>
        </card-top-recommendations>
        ''',
        )

        self.assertFragmentIn(
            response,
            '''
        <card-recommendations top-offers-count="4">
            <position bid="61" code="0" pos="1"/>
            <position bid="51" code="0" pos="2"/>
            <position bid="41" code="0" pos="3"/>
            <position bid="31" code="0" pos="4"/>
            <position bid="25" code="0" pos="5"/>
            <position bid="25" code="0" pos="6"/>
            <position bid="10" code="0" pos="7"/>
            <position bid="9" code="0" pos="8"/>
            <position bid="8" code="0" pos="9"/>
            <position bid="7" code="0" pos="10"/>
        </card-recommendations>
        ''',
        )

        self.assertFragmentIn(
            response,
            '''
        <card-price-recommendations>
            <position bid="61" code="0" pos="1"/>
            <position bid="51" code="0" pos="2"/>
            <position bid="41" code="0" pos="3"/>
            <position bid="31" code="0" pos="4"/>
            <position bid="21" code="0" pos="5"/>
            <position bid="11" code="0" pos="6"/>
            <position bid="10" code="0" pos="7"/>
            <position bid="9" code="0" pos="8"/>
            <position bid="8" code="0" pos="9"/>
            <position bid="7" code="0" pos="10"/>
        </card-price-recommendations>
        ''',
        )

        response = self.report.request_xml(
            'place=bids_recommender&fesh=980&hyperid=654321&rids=213&rearr-factors=market_ha_threshold_mult=6'
        )
        # Проверяем временный вывод current-pos-all, current-pos-top, top-offers-count
        self.assertFragmentIn(
            response,
            '<card-recommendations current-pos-all="1" current-pos-top="1" top-offers-count="3"></card-recommendations>',
        )
        self.assertFragmentIn(response, '<card-price-recommendations current-pos-all="1"></card-price-recommendations>')
        self.assertFragmentIn(
            response, '<card-top-recommendations current-pos-top="1" top-offers-count="3"></card-top-recommendations>'
        )

    @classmethod
    def prepare_offers_from_cpa_category(cls):
        cls.index.cpa_categories += [
            CpaCategory(hid=701, regions=[213], cpa_type=CpaCategoryType.CPA_NON_GURU),
        ]
        cls.index.shops += [
            Shop(fesh=701, regions=[213]),
            Shop(fesh=702, regions=[213]),
            Shop(fesh=703, regions=[213]),
        ]
        cls.index.offers += [
            Offer(title="First offer of CPA category", hid=701, price=100, bid=100, fesh=701, feedid=701, offerid=701),
            Offer(title="Second offer of CPA category", hid=701, price=105, bid=105, fesh=702, feedid=702, offerid=702),
            Offer(title="Third offer of CPA category", hid=701, price=110, bid=110, fesh=703, feedid=703, offerid=703),
        ]

    def test_ignore_cpa_categories_on_search(self):
        """Check that offers from CPA categories don't get fee recommendations"""
        response = self.report.request_xml(
            "place=bids_recommender&feed_shoffer_id=701-701&rids=213&type=market_search&text=offer+of+CPA+category"
        )
        self.assertFragmentIn(
            response,
            """
        <search-recommendations>
            <position code="0" bid="111" pos="1"/>
            <position code="0" bid="106" pos="2"/>
            <position code="0" bid="1" pos="3"/>
        </search-recommendations>
        """,
        )

    @classmethod
    def prepare_empty_recommendations(cls):
        cls.index.shops += [
            Shop(fesh=10201, priority_region=213),
        ]

        cls.index.models += [
            Model(hid=19991, hyperid=687548),
        ]

        cls.index.offers += [
            Offer(offerid=99999, hid=19991, hyperid=687548, bid=60, price=100, fesh=10201),
        ]

    def test_empty_recommendations(self):
        response = self.report.request_xml("place=bids_recommender&fesh=10201&rids=2&hyperid=687548")
        self.assertFragmentIn(
            response,
            """
        <offer>
            <raw-title/>
            <hyper_id>687548</hyper_id>
            <hidd>19991</hidd>
            <price currency="RUR">100</price>
            <bids bid="60" pull_to_min_bid="true" />
            <cpc-enabled>true</cpc-enabled>
            <cpa-enabled>false</cpa-enabled>
            <quality-rating>0.6</quality-rating>
        </offer>
        """,
        )

    @classmethod
    def prepare_card_recommendations_for_offers_with_the_same_classifier_magic_id(cls):
        cls.index.shops += [Shop(fesh=4000, regions=[213])]
        cls.index.models += [
            Model(hid=400000, hyperid=400000),
        ]

        cmagic = 'f2dfc75bbd15ae22fbd2e35b21675aab'
        cls.index.offers += [
            Offer(hid=400000, hyperid=400000, bid=50, price=100, fesh=4000, feedid=4000, offerid=4001, cmagic=cmagic),
            Offer(hid=400000, hyperid=400000, bid=40, price=100, fesh=4000, feedid=4000, offerid=4002, cmagic=cmagic),
            Offer(hid=400000, hyperid=400000, bid=30, price=100, fesh=4000, feedid=4000, offerid=4003, cmagic=cmagic),
        ]

    def test_card_recommendations_for_offers_with_the_same_classifier_magic_id(self):
        """See MARKETOUT-24847 - bug with offers with the same classifier_magic_id
        Check if WareId is used to compare offers
        """

        req = 'place=bids_recommender&fesh=4000&hyperid=400000&rids=213&feed_shoffer_id=4000-'

        resp = self.report.request_xml(req + '4001')
        self.assertFragmentIn(resp, '<card-top-recommendations current-pos-all="1"></card-top-recommendations>')

        resp = self.report.request_xml(req + '4002')
        self.assertFragmentNotIn(resp, '<card-top-recommendations current-pos-all="1"></card-top-recommendations>')

        resp = self.report.request_xml(req + '4003')
        self.assertFragmentNotIn(resp, '<card-top-recommendations current-pos-all="1"></card-top-recommendations>')

    @classmethod
    def prepare_shop_is_switched_off_by_schedule(cls):
        cls.index.offers += [
            Offer(hyperid=5500, hid=501, fesh=5001, price=10000, bid=53, ts=401),
            Offer(hyperid=5500, hid=501, fesh=5002, price=10000, bid=53, ts=402),
            Offer(hyperid=5500, hid=501, fesh=5003, price=10000, bid=53, ts=403),
        ]

    def test_shop_is_switched_off_by_schedule(self):
        """
        При отключении магазина по расписанию, рекомендации должны работать.
        """

        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(5003)]

        response = self.report.request_xml(
            'place=bids_recommender&fesh=5003&rids=213&hyperid=5500&type=card&rearr-factors=market_uncollapse_supplier=0'
        )
        self.assertFragmentIn(
            response,
            '''
            <card-recommendations>

                <position bid="54" code="0" pos="1"/>
                <position bid="54" code="0" pos="2"/>
            </card-recommendations>
        ''',
            preserve_order=True,
        )

        self.dynamic.market_dynamic.disabled_cpc_shops.clear()

    @classmethod
    def prepare_switched_off_shop_with_cpa_category(cls):
        for seq in range(4):
            cls.index.shops += [Shop(fesh=500000 + seq, priority_region=213)]
            cls.index.offers += [
                Offer(
                    hid=100002,
                    fesh=500000 + seq,
                    price=10000,
                    bid=53 + seq,
                    ts=4000 + seq,
                    offerid=10 + seq,
                    feedid=10 + seq,
                    title="Firespray-31",
                )
            ]

    def test_switched_off_shop_with_cpa_category(self):
        """see MARKETOUT-29315 - bids_recommender внезапно аукцион по СРА, MARKETOUT-29185 - отсутствуют рекомендации ставок bids_recommender, market_search
        При отключении магазина по расписанию, рекомендации должны работать. Даже для магазинов с товарами из CPA категорий.
        """

        """
        Сначала проверяем, что есть рекомендации при включенном магазине
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=500002&rids=213&feed_shoffer_id=12-12&type=market_search&text=Firespray-31'
        )
        self.assertFragmentIn(
            response,
            '''
            <search-recommendations>
                <position bid="57" code="0" fee="0" pos="1"/>
                <position bid="55" code="0" fee="0" pos="2"/>
                <position bid="54" code="0" fee="0" pos="3"/>
                <position bid="13" code="0" fee="0" pos="4"/>
            </search-recommendations>
        ''',
            preserve_order=True,
        )

        """
        Выключаем магазины (ночь таки, мафия выходит на дорогу)(но не все!) и проверяем, что рекомендации остались
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(500002),
            DynamicShop(500003),
            DynamicShop(500004),
        ]

        """
        Теперь проверяем, что есть рекомендации при выключенном магазине
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=500002&rids=213&feed_shoffer_id=12-12&type=market_search&text=Firespray-31'
        )
        self.assertFragmentIn(
            response,
            '''
            <search-recommendations>
                <position bid="55" code="0" fee="0" pos="1"/>
                <position bid="54" code="0" fee="0" pos="2"/>
                <position bid="13" code="0" fee="0" pos="3"/>
                <position bid="13" code="0" fee="0" pos="4"/>
            </search-recommendations>
        ''',
            preserve_order=True,
        )

        self.dynamic.market_dynamic.disabled_cpc_shops.clear()

    @classmethod
    def prepare_search_by_title(cls):
        cls.index.offers += [Offer(title='Search by title', fesh=5013)]

    def test_search_by_title(self):
        """
        Тестируем поиск по тайтлу
        """
        response = self.report.request_xml('place=bids_recommender&fesh=5013&title=Search by title&rids=213')
        self.assertFragmentIn(
            response,
            self.drop_title_if_needed(
                '''
            <offers>
                <offer>
                <raw-title>Search by title</raw-title>
                </offer>
            </offers>
        '''
            ),
            preserve_order=True,
        )

    @classmethod
    def prepare_bids_group_model(cls):
        cls.index.shops += [Shop(fesh=6011, priority_region=1), Shop(fesh=6012, priority_region=1)]

        cls.index.model_groups += [ModelGroup(hyperid=2020)]

        cls.index.models += [Model(hyperid=20201, group_hyperid=2020)]

        cls.index.offers += [Offer(hyperid=20201, fesh=6011, bid=100), Offer(hyperid=2020, fesh=6012, bid=200)]

    def test_bids_group_model(self):
        """
        Проверяем, что для оффера, привязанного к групповой модели, при
        рекомендациях учитываются оффера дочерних моделей
        """

        response = self.report.request_xml(
            'place=bids_recommender&fesh=6012&hyperid=2020&rids=1&rearr-factors=market_uncollapse_supplier=0'
        )
        self.assertFragmentIn(
            response,
            '''
            <card-price-recommendations>
                <position bid="101" pos="1"/>
            </card-price-recommendations>
        ''',
        )

        self.assertFragmentIn(
            response,
            '''
            <card-recommendations>
                <position bid="101" pos="1"/>
            </card-recommendations>
        ''',
        )

        self.assertFragmentIn(
            response,
            '''
            <card-top-recommendations>
                <position bid="101" pos="1"/>
            </card-top-recommendations>
        ''',
        )

    @classmethod
    def prepare_feed_shoffer_id_in_base64(cls):
        cls.index.shops += [
            Shop(fesh=6004, priority_region=1),
        ]
        cls.index.offers += [
            Offer(hyperid=1011, fesh=6004, bid=8401, offerid='a.a', feedid=42),
        ]

    def test_feed_shoffer_id_in_base64(self):
        """
        Проверяем, что оффер, с feed_shoffer_id закодированным в base64, найдется
        """

        response = self.report.request_xml(
            "place=bids_recommender&fesh=6004&rids=2&feed_shoffer_id_base64={}".format(
                base64.urlsafe_b64encode('42-a.a')
            )
        )
        self.assertFragmentIn(
            response,
            """
            <offer>
                <hyper_id>1011</hyper_id>
            </offer>
        """,
        )

    @classmethod
    def prepare_ignore_text_in_search_bids_recommendations_flag(cls):
        cls.index.offers += [
            Offer(
                title="kebab",
                hid=1000,
                bid=10,
                fesh=301,
                feedid=5000,
                offerid=5000,
                ts=5001,
                popular_queries_all=[
                    TopQueries('kebab', 10, 1, 1.5),
                ],
                popular_queries_offer=[
                    TopQueries('lulya', 55, 22, 1.5),
                ],
            ),
            Offer(title="kebab", hid=1000, bid=20, fesh=302, ts=5002),
            Offer(title="kebab", hid=1000, bid=30, fesh=303, ts=5003),
            Offer(title="lulya", hid=1000, bid=40, fesh=304, ts=5004),
            Offer(title="lulya", hid=1000, bid=50, fesh=304, ts=5005),
        ]

    def test_ignore_text_in_search_bids_recommendations_flag(self):
        """
        Проверяем что реарр-флаг market_ignore_text_in_search_bids_recommendations форсирует рекомендации по категории, а не по текстовому запросу.
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=301&rids=213&feed_shoffer_id=5000-5000&type=market_search&text=kebab'
            '&rearr-factors=market_ignore_text_in_search_bids_recommendations=1000,100500'
        )
        # Во флаге указана, в том числе, правильная категория.
        # В рекомендациях участвуют все оффера из категории целевого оффера, в том числе с "неправильным" тайтлом.
        self.assertFragmentIn(
            response,
            '''
            <search-recommendations>
                <position bid="51" pos="1"/>
                <position bid="41" pos="2"/>
                <position bid="31" pos="3"/>
                <position bid="21" pos="4"/>
                <position bid="1"  pos="5"/>
            </search-recommendations>
        ''',
        )

        response = self.report.request_xml(
            'place=bids_recommender&fesh=301&rids=213&feed_shoffer_id=5000-5000&type=market_search&text=kebab'
            '&rearr-factors=market_ignore_text_in_search_bids_recommendations=100500'
        )
        # Во флаге не указана правильная категория.
        # Проверяем, что флаг включает только для указанных категорий.
        self.assertFragmentNotIn(
            response,
            '''
            <search-recommendations>
                <position bid="51" pos="1"/>
                <position bid="41" pos="2"/>
                <position bid="31" pos="3"/>
                <position bid="21" pos="4"/>
                <position bid="1"  pos="5"/>
            </search-recommendations>
        ''',
        )

    def test_ignore_text_in_search_bids_recommendations_flag_with_show_top_queries(self):
        """
        Проверяем что реарр-флаг market_ignore_text_in_search_bids_recommendations форсирует рекомендации по категории, а не по текстовому запросу.
        """
        response = self.report.request_xml(
            'place=bids_recommender&fesh=301&rids=213&feed_shoffer_id=5000-5000&type=market_search&show-top-queries=1'
            '&rearr-factors=market_ignore_text_in_search_bids_recommendations=1000'
        )
        # В каждом блоке рекомендаций участвуют все оффера из категории целевого оффера, в том числе с "неправильным" title
        # Рекомендации одинаковые
        for text in ('kebab', 'lulya'):
            self.assertFragmentIn(
                response,
                '''
                <query>
                    <text>{}</text>
                    <search-recommendations>
                        <position bid="51" pos="1"/>
                        <position bid="41" pos="2"/>
                        <position bid="31" pos="3"/>
                        <position bid="21" pos="4"/>
                        <position bid="1"  pos="5"/>
                    </search-recommendations>
                </query>
            '''.format(
                    text
                ),
            )

    @classmethod
    def prepare_batch_recommendations(cls):
        cls.index.shops += [
            Shop(fesh=9000, regions=[213]),
            Shop(fesh=9001, regions=[213]),
            Shop(fesh=9002, regions=[213]),
        ]
        cls.index.models += [
            Model(hid=900000, hyperid=900001),
            Model(hid=900000, hyperid=900002),
        ]

        cls.index.offers += [
            Offer(title="bunny", hid=900000, hyperid=900001, bid=10, price=110, fesh=9000, feedid=9000, offerid=9001),
            Offer(title="wolf", hid=900000, hyperid=900002, bid=14, price=120, fesh=9000, feedid=9000, offerid=9002),
            Offer(
                title="bunny zayts", hid=900000, hyperid=900001, bid=11, price=110, fesh=9001, feedid=9001, offerid=9001
            ),
            Offer(
                title="wolf volk", hid=900000, hyperid=900002, bid=19, price=120, fesh=9001, feedid=9001, offerid=9002
            ),
            Offer(
                title="bunny zayts krolik",
                hid=900000,
                hyperid=900001,
                bid=15,
                price=110,
                fesh=9002,
                feedid=9002,
                offerid=9001,
            ),
            Offer(
                title="wolf volk seriy",
                hid=900000,
                hyperid=900002,
                bid=16,
                price=120,
                fesh=9002,
                feedid=9002,
                offerid=9002,
            ),
        ]

    def test_batch_recommendations_card_type(self):
        """
        Проверяем батч запрос для карточных офферов. MARKETOUT-28173
        """
        response = self.report.request_xml(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=card&feed_shoffer_id=9000-9001,9001-9002&rearr-factors=market_uncollapse_supplier=0'
        )

        fragment_9000 = self.drop_title_if_needed(
            '''
            <bids_recommendations feed_shoffer_id="9000-9001">
                <offers>
                    <offer>
                        <raw-title>bunny</raw-title>
                        <hyper_id>900001</hyper_id>
                    </offer>
                </offers>
                <recommendations min-bid="1" min-fee="0">
                    <card-price-recommendations current-pos-all="3" current-pos-top="3" top-offers-count="3">
                        <position bid="16" code="0" pos="1"/>
                        <position bid="12" code="0" pos="2"/>
                    </card-price-recommendations>
                </recommendations>
            </bids_recommendations>'''
        )

        fragment_9001 = self.drop_title_if_needed(
            '''
            <bids_recommendations feed_shoffer_id="9001-9002">
                <offers>
                    <offer>
                        <raw-title>wolf volk</raw-title>
                        <hyper_id>900002</hyper_id>
                    </offer>
                </offers>
                <recommendations min-bid="1" min-fee="0">
                    <card-price-recommendations current-pos-all="1" current-pos-top="1" top-offers-count="3">
                        <position bid="17" code="0" pos="1"/>
                        <position bid="15" code="0" pos="2"/>
                    </card-price-recommendations>
                </recommendations>
            </bids_recommendations>'''
        )

        self.assertFragmentIn(response, fragment_9000)
        self.assertFragmentIn(response, fragment_9001)

        # Проверка с одним feed_shoffer_id
        response = self.report.request_xml(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=card&feed_shoffer_id=9000-9001&rearr-factors=market_uncollapse_supplier=0'
        )
        self.assertFragmentIn(response, fragment_9000)

    def test_batch_recommendations_market_search(self):
        """
        Проверяем батч запрос типа market_search. MARKETOUT-28173
        """
        response = self.report.request_xml(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=market_search&batch-bids-text=search,cpa,auction&batch-bids-text=search,cpc,auction&feed_shoffer_id=301-1,1301-1301'
        )

        fragment_301 = self.drop_title_if_needed(
            '''
            <bids_recommendations feed_shoffer_id="301-1">
                <offers>
                    <offer>
                        <raw-title>search cpa auction</raw-title>
                        <hidd>301</hidd>
                        <price currency="RUR">100</price>
                        <bids bid="10" fee="0" pull_to_min_bid="true"/>
                    </offer>
                </offers>
                <recommendations min-bid="1" min-fee="0">
                    <search-recommendations current-pos-all="6" model-count="0">
                        <position bid="51" code="0" fee="0" pos="1"/>
                        <position bid="51" code="0" fee="0" pos="2"/>
                        <position bid="41" code="0" fee="0" pos="3"/>
                        <position bid="31" code="0" fee="0" pos="4"/>
                        <position bid="21" code="0" fee="0" pos="5"/>
                    </search-recommendations>
                </recommendations>
            </bids_recommendations>'''
        )

        fragment_1301 = self.drop_title_if_needed(
            '''
            <bids_recommendations feed_shoffer_id="1301-1301">
                <offers>
                    <offer>
                        <raw-title>search cpc auction</raw-title>
                        <hidd>302</hidd>
                        <price currency="RUR">100</price>
                        <bids bid="10" fee="0" pull_to_min_bid="true"/>
                    </offer>
                </offers>
                <recommendations min-bid="1" min-fee="0">
                    <search-recommendations current-pos-all="7" model-count="1">
                        <position bid="81" code="0" fee="0" pos="1"/>
                        <position bid="71" code="0" fee="0" pos="2"/>
                        <position bid="51" code="0" fee="0" pos="3"/>
                        <position bid="41" code="0" fee="0" pos="4"/>
                        <position bid="31" code="0" fee="0" pos="5"/>
                        <position bid="21" code="0" fee="0" pos="6"/>
                    </search-recommendations>
                </recommendations>
            </bids_recommendations>'''
        )

        self.assertFragmentIn(response, fragment_301)
        self.assertFragmentIn(response, fragment_1301)

        # Проверяем соответсвие текста и оффера согласно порядку в списке
        response = self.report.request_xml(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=market_search&batch-bids-text=auction&batch-bids-text=cpc&feed_shoffer_id=301-1,1301-1301'
        )
        self.assertFragmentIn(response, fragment_301)
        self.assertFragmentIn(response, fragment_1301)

        response = self.report.request_xml(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=market_search&batch-bids-text=cpa&batch-bids-text=auction&feed_shoffer_id=301-1,1301-1301'
        )
        self.assertFragmentIn(response, fragment_301)
        self.assertFragmentIn(response, fragment_1301)

    def test_batch_recommendations_top_queries(self):
        """
        Проверяем батч запрос c top queries. MARKETOUT-28173
        """
        response = self.report.request_xml(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=market_search&show-top-queries=1&feed_shoffer_id=301-1,302-2'
        )

        fragment_301 = '''
            <bids_recommendations feed_shoffer_id="301-1">
               <offers>
                  <offer>
                     <raw-title>search cpa auction</raw-title>
                     <hidd>301</hidd>
                     <price currency="RUR">100</price>
                     <bids bid="10" fee="0" pull_to_min_bid="true" />
                  </offer>
               </offers>
               <recommendations min-bid="1" min-fee="0">
                  <top-queries-recommendations>
                     <query average_offer_position="1.5" offer_show_count="1" query_show_count="10" type="top_all">
                        <text>search cpa auction</text>
                        <search-recommendations current-pos-all="6" model-count="0">
                           <position bid="51" code="0" fee="0" pos="1" />
                           <position bid="51" code="0" fee="0" pos="2" />
                           <position bid="41" code="0" fee="0" pos="3" />
                           <position bid="31" code="0" fee="0" pos="4" />
                           <position bid="21" code="0" fee="0" pos="5" />
                        </search-recommendations>
                     </query>
                     <query average_offer_position="1.5" offer_show_count="22" query_show_count="55" type="top_offer">
                        <text>search cpa</text>
                        <search-recommendations current-pos-all="6" model-count="0">
                           <position bid="51" code="0" fee="0" pos="1" />
                           <position bid="51" code="0" fee="0" pos="2" />
                           <position bid="41" code="0" fee="0" pos="3" />
                           <position bid="31" code="0" fee="0" pos="4" />
                           <position bid="21" code="0" fee="0" pos="5" />
                        </search-recommendations>
                     </query>
                  </top-queries-recommendations>
               </recommendations>
            </bids_recommendations>'''

        fragment_302 = '''
            <bids_recommendations feed_shoffer_id="302-2">
               <offers>
                  <offer>
                     <raw-title>search cpa auction</raw-title>
                     <hidd>301</hidd>
                     <price currency="RUR">100</price>
                     <bids bid="20" fee="0" pull_to_min_bid="true" />
                  </offer>
               </offers>
               <recommendations min-bid="1" min-fee="0">
                  <top-queries-recommendations>
                     <query average_offer_position="1.5" offer_show_count="1" query_show_count="10" type="top_all">
                        <text>search cpa auction</text>
                        <search-recommendations current-pos-all="5" model-count="0">
                           <position bid="51" code="0" fee="0" pos="1" />
                           <position bid="51" code="0" fee="0" pos="2" />
                           <position bid="41" code="0" fee="0" pos="3" />
                           <position bid="31" code="0" fee="0" pos="4" />
                           <position bid="11" code="0" fee="0" pos="5" />
                        </search-recommendations>
                     </query>
                     <query average_offer_position="1.5" offer_show_count="22" query_show_count="55" type="top_offer">
                        <text>search cpa</text>
                        <search-recommendations current-pos-all="5" model-count="0">
                           <position bid="51" code="0" fee="0" pos="1" />
                           <position bid="51" code="0" fee="0" pos="2" />
                           <position bid="41" code="0" fee="0" pos="3" />
                           <position bid="31" code="0" fee="0" pos="4" />
                           <position bid="11" code="0" fee="0" pos="5" />
                        </search-recommendations>
                     </query>
                  </top-queries-recommendations>
               </recommendations>
            </bids_recommendations>'''

        self.assertFragmentIn(response, fragment_301)
        self.assertFragmentIn(response, fragment_302)

    def test_batch_recommendations_card_type_json_output(self):
        """
        Проверяем батч запрос для карточных офферов. Tип выдачи JSON. MARKETOUT-28173
        """
        response = self.report.request_json(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=card&feed_shoffer_id=9000-9001,9001-9002&bsformat=2&rearr-factors=market_uncollapse_supplier=0'
        )

        expected_response = {
            "searchResults": {
                "bidsRecommendations": [
                    {
                        "feedShofferId": "9000-9001",
                        "offer": {
                            "rawTitle": self.optional_title("bunny"),
                            "url": NotEmpty(),
                            "hyperId": 900001,
                            "hid": 900000,
                            "wareMd5": NotEmpty(),
                            "price": {"currency": "RUR", "value": "110"},
                            "bids": {"bid": 10, "pulToMinBid": True},
                            "cpcEnabled": True,
                            "cpaEnabled": False,
                            "qualityRating": 0.6,
                        },
                        "recommendations": {
                            "minBid": 1,
                            "cardPriceRecommendations": {
                                "currentPosTop": 3,
                                "currentPosAll": 3,
                                "topOffersCount": 3,
                                "position": [{"bid": 16, "code": 0}, {"bid": 12, "code": 0}, {"bid": 1, "code": 0}],
                            },
                            "cardRecommendations": {
                                "currentPosTop": 3,
                                "currentPosAll": 3,
                                "topOffersCount": 3,
                                "position": [{"bid": 16, "code": 0}, {"bid": 12, "code": 0}, {"bid": 1, "code": 0}],
                            },
                            "cardTopRecommendations": {
                                "currentPosTop": 3,
                                "currentPosAll": 3,
                                "topOffersCount": 3,
                                "position": [{"bid": 16, "code": 0}, {"bid": 12, "code": 0}, {"bid": 1, "code": 0}],
                            },
                        },
                    },
                    {
                        "feedShofferId": "9001-9002",
                        "offer": {
                            "rawTitle": self.optional_title("wolf volk"),
                            "url": NotEmpty(),
                            "hyperId": 900002,
                            "hid": 900000,
                            "wareMd5": NotEmpty(),
                            "price": {"currency": "RUR", "value": "120"},
                            "bids": {"bid": 19, "pulToMinBid": True},
                            "cpcEnabled": True,
                            "cpaEnabled": False,
                            "qualityRating": 0.6,
                        },
                        "recommendations": {
                            "minBid": 1,
                            "cardPriceRecommendations": {
                                "currentPosTop": 1,
                                "currentPosAll": 1,
                                "topOffersCount": 3,
                                "position": [{"bid": 17, "code": 0}, {"bid": 15, "code": 0}, {"bid": 1, "code": 0}],
                            },
                            "cardRecommendations": {
                                "currentPosTop": 1,
                                "currentPosAll": 1,
                                "topOffersCount": 3,
                                "position": [{"bid": 17, "code": 0}, {"bid": 15, "code": 0}, {"bid": 1, "code": 0}],
                            },
                            "cardTopRecommendations": {
                                "currentPosTop": 1,
                                "currentPosAll": 1,
                                "topOffersCount": 3,
                                "position": [{"bid": 17, "code": 0}, {"bid": 15, "code": 0}, {"bid": 1, "code": 0}],
                            },
                        },
                    },
                ]
            }
        }

        self.assertFragmentIn(response, expected_response)

    def test_batch_recommendations_market_search_json_output(self):
        """
        Проверяем батч запрос типа market_search. Tип выдачи JSON. MARKETOUT-28173
        """
        response = self.report.request_json(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=market_search&'
            'batch-bids-text=search,cpa+auction&batch-bids-text=search+cpc,auction&feed_shoffer_id=301-1,1301-1301&bsformat=2'
        )

        expected_response = {
            "searchResults": {
                "bidsRecommendations": [
                    {
                        "feedShofferId": "301-1",
                        "offer": {
                            "rawTitle": self.optional_title("search cpa auction"),
                            "url": NotEmpty(),
                            "hid": 301,
                            "wareMd5": NotEmpty(),
                            "price": {"currency": "RUR", "value": "100"},
                            "bids": {"bid": 10, "pulToMinBid": True},
                            "cpcEnabled": True,
                            "cpaEnabled": False,
                            "qualityRating": 0.6,
                        },
                        "recommendations": {
                            "minBid": 1,
                            "searchRecommendations": {
                                "currentPosAll": "6",
                                "modelCount": 0,
                                "position": [
                                    {"bid": 51, "code": 0},
                                    {"bid": 51, "code": 0},
                                    {"bid": 41, "code": 0},
                                    {"bid": 31, "code": 0},
                                    {"bid": 21, "code": 0},
                                ],
                            },
                        },
                    },
                    {
                        "feedShofferId": "1301-1301",
                        "offer": {
                            "rawTitle": self.optional_title("search cpc auction"),
                            "url": NotEmpty(),
                            "hid": 302,
                            "wareMd5": NotEmpty(),
                            "price": {"currency": "RUR", "value": "100"},
                            "bids": {"bid": 10, "pulToMinBid": True},
                            "cpcEnabled": True,
                            "cpaEnabled": False,
                            "qualityRating": 0.6,
                        },
                        "recommendations": {
                            "minBid": 1,
                            "searchRecommendations": {
                                "currentPosAll": "7",
                                "modelCount": 1,
                                "position": [
                                    {"bid": 81, "code": 0},
                                    {"bid": 71, "code": 0},
                                    {"bid": 51, "code": 0},
                                    {"bid": 41, "code": 0},
                                    {"bid": 31, "code": 0},
                                    {"bid": 21, "code": 0},
                                ],
                            },
                        },
                    },
                ]
            }
        }

        self.assertFragmentIn(response, expected_response)

    def test_batch_recommendations_top_queries_json_output(self):
        """
        Проверяем батч запрос c top queries. Tип выдачи JSON. MARKETOUT-28173
        """
        response = self.report.request_json(
            'place=bids_recommender&batch-bids-recommendations=1&rids=213&type=market_search&feed_shoffer_id=301-1,302-2&show-top-queries=1&bsformat=2'
        )

        expected_response = {
            "searchResults": {
                "bidsRecommendations": [
                    {
                        "feedShofferId": "301-1",
                        "offer": {
                            "rawTitle": "search cpa auction",
                            "url": NotEmpty(),
                            "hid": 301,
                            "wareMd5": NotEmpty(),
                            "price": {"currency": "RUR", "value": "100"},
                            "bids": {"bid": 10, "pulToMinBid": True},
                            "cpcEnabled": True,
                            "cpaEnabled": False,
                            "qualityRating": 0.6,
                        },
                        "recommendations": {
                            "minBid": 1,
                            "topQueriesRecommendations": [
                                {
                                    "query": {
                                        "type": "top_all",
                                        "offerShowCount": 1,
                                        "queryShowCount": 10,
                                        "averageOfferPosition": 1.5,
                                        "text": "search cpa auction",
                                        "searchRecommendations": {
                                            "currentPosAll": "6",
                                            "modelCount": 0,
                                            "position": [
                                                {"bid": 51, "code": 0},
                                                {"bid": 51, "code": 0},
                                                {"bid": 41, "code": 0},
                                                {"bid": 31, "code": 0},
                                                {"bid": 21, "code": 0},
                                            ],
                                        },
                                    }
                                },
                                {
                                    "query": {
                                        "type": "top_offer",
                                        "offerShowCount": 22,
                                        "queryShowCount": 55,
                                        "averageOfferPosition": 1.5,
                                        "text": "search cpa",
                                        "searchRecommendations": {
                                            "currentPosAll": "6",
                                            "modelCount": 0,
                                            "position": [
                                                {"bid": 51, "code": 0},
                                                {"bid": 51, "code": 0},
                                                {"bid": 41, "code": 0},
                                                {"bid": 31, "code": 0},
                                                {"bid": 21, "code": 0},
                                            ],
                                        },
                                    }
                                },
                            ],
                        },
                    },
                    {
                        "feedShofferId": "302-2",
                        "offer": {
                            "rawTitle": "search cpa auction",
                            "url": NotEmpty(),
                            "hid": 301,
                            "wareMd5": NotEmpty(),
                            "price": {"currency": "RUR", "value": "100"},
                            "bids": {"bid": 20, "pulToMinBid": True},
                            "cpcEnabled": True,
                            "cpaEnabled": False,
                            "qualityRating": 0.6,
                        },
                        "recommendations": {
                            "minBid": 1,
                            "topQueriesRecommendations": [
                                {
                                    "query": {
                                        "type": "top_all",
                                        "offerShowCount": 1,
                                        "queryShowCount": 10,
                                        "averageOfferPosition": 1.5,
                                        "text": "search cpa auction",
                                        "searchRecommendations": {
                                            "currentPosAll": "5",
                                            "modelCount": 0,
                                            "position": [
                                                {"bid": 51, "code": 0},
                                                {"bid": 51, "code": 0},
                                                {"bid": 41, "code": 0},
                                                {"bid": 31, "code": 0},
                                                {"bid": 11, "code": 0},
                                            ],
                                        },
                                    }
                                },
                                {
                                    "query": {
                                        "type": "top_offer",
                                        "offerShowCount": 22,
                                        "queryShowCount": 55,
                                        "averageOfferPosition": 1.5,
                                        "text": "search cpa",
                                        "searchRecommendations": {
                                            "currentPosAll": "5",
                                            "modelCount": 0,
                                            "position": [
                                                {"bid": 51, "code": 0},
                                                {"bid": 51, "code": 0},
                                                {"bid": 41, "code": 0},
                                                {"bid": 31, "code": 0},
                                                {"bid": 11, "code": 0},
                                            ],
                                        },
                                    }
                                },
                            ],
                        },
                    },
                ]
            }
        }

        self.assertFragmentIn(response, expected_response)
        self.access_log.expect(
            snippet_requests_made=5,
            snippets_fetched=6,
        )

    @classmethod
    def prepare_test_cpc_with_cpa_recomendations(cls):
        cls.index.cpa_categories += [
            CpaCategory(hid=64321, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        cls.index.shops += [
            Shop(fesh=680, regions=[213]),
            Shop(fesh=681, regions=[213]),
            Shop(fesh=682, regions=[213]),
            Shop(fesh=683, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=684, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=685, regions=[213]),
            Shop(fesh=686, regions=[213]),
            Shop(fesh=687, regions=[213]),
            Shop(fesh=688, regions=[213]),
            Shop(fesh=687, regions=[213]),
            Shop(fesh=688, regions=[213]),
            Shop(fesh=689, regions=[213]),
            Shop(fesh=690, regions=[213]),
        ]

        cls.index.models += [
            Model(hid=64321, hyperid=7654321),
        ]

        cls.index.offers += [
            Offer(hid=64321, hyperid=7654321, bid=60, price=100, fesh=680),
            Offer(hid=64321, hyperid=7654321, bid=50, price=100, fesh=681),
            Offer(hid=64321, hyperid=7654321, bid=40, price=100, fesh=682),
            Offer(hid=64321, hyperid=7654321, bid=30, price=100, fesh=683, cpa=Offer.CPA_REAL),
            Offer(hid=64321, hyperid=7654321, bid=20, price=100, fesh=684, cpa=Offer.CPA_REAL),
            Offer(hid=64321, hyperid=7654321, bid=10, price=100, fesh=685),
            Offer(hid=64321, hyperid=7654321, bid=9, price=100, fesh=686),
            Offer(hid=64321, hyperid=7654321, bid=8, price=100, fesh=687),
            Offer(hid=64321, hyperid=7654321, bid=7, price=100, fesh=688),
            Offer(hid=64321, hyperid=7654321, bid=6, price=100, fesh=689),
            Offer(hid=64321, hyperid=7654321, bid=5, price=100, fesh=690),
        ]

    def test_cpc_with_cpa_recomendations(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-35873
        Проверяем, что cpa оффера ранжируются выше cpc и позиции cpa офферов недоступны (code = 2)
        '''

        response = self.report.request_xml(
            'place=bids_recommender&fesh=690&hyperid=7654321&rids=213&rearr-factors=use_offer_type_priority_as_main_factor_in_top=1;market_uncollapse_supplier=0'
        )

        self.assertFragmentIn(
            response,
            '''
        <card-top-recommendations top-offers-count="6">
            <position code="4" pos="1"/>
            <position code="4" pos="2"/>
            <position bid="61" code="0" pos="3"/>
            <position bid="51" code="0" pos="4"/>
            <position bid="41" code="0" pos="5"/>
            <position bid="11" code="0" pos="6"/>
        </card-top-recommendations>
        ''',
        )

        self.assertFragmentIn(
            response,
            '''
        <card-recommendations top-offers-count="6">
            <position code="4" pos="1"/>
            <position code="4" pos="2"/>
            <position bid="61" code="0" pos="3"/>
            <position bid="51" code="0" pos="4"/>
            <position bid="41" code="0" pos="5"/>
            <position bid="11" code="0" pos="6"/>
            <position bid="10" code="0" pos="7"/>
            <position bid="9" code="0" pos="8"/>
            <position bid="8" code="0" pos="9"/>
            <position bid="7" code="0" pos="10"/>
        </card-recommendations>
        ''',
        )

        self.assertFragmentIn(
            response,
            '''
        <card-price-recommendations>
            <position code="4" pos="1"/>
            <position code="4" pos="2"/>
            <position bid="61" code="0" pos="3"/>
            <position bid="51" code="0" pos="4"/>
            <position bid="41" code="0" pos="5"/>
            <position bid="11" code="0" pos="6"/>
            <position bid="10" code="0" pos="7"/>
            <position bid="9" code="0" pos="8"/>
            <position bid="8" code="0" pos="9"/>
            <position bid="7" code="0" pos="10"/>
        </card-price-recommendations>
        ''',
        )

    def test_cpc_with_cpa_recomendations_disabled(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-35873
        Проверяем, что cpa оффера ранжируются вперемежку с cpc, если не задан флаг use_offer_type_priority_as_main_factor_in_top=1
        '''

        response = self.report.request_xml(
            'place=bids_recommender&fesh=690&hyperid=7654321&rids=213&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_ranging_cpa_by_ue_in_top=0;market_uncollapse_supplier=0'
        )

        # Проверяем ставки. Посчитанный threshold здесь должен быть 25, с учетом выставленного threshold_mult в 6.
        self.assertFragmentIn(
            response,
            '''
        <card-top-recommendations top-offers-count="6">
            <position bid="61" code="0" pos="1"/>
            <position bid="51" code="0" pos="2"/>
            <position bid="41" code="0" pos="3"/>
            <position bid="31" code="0" pos="4"/>
            <position bid="21" code="0" pos="5"/>
            <position bid="11" code="0" pos="6"/>
        </card-top-recommendations>
        ''',
        )

        self.assertFragmentIn(
            response,
            '''
        <card-recommendations top-offers-count="6">
            <position bid="61" code="0" pos="1"/>
            <position bid="51" code="0" pos="2"/>
            <position bid="41" code="0" pos="3"/>
            <position bid="31" code="0" pos="4"/>
            <position bid="21" code="0" pos="5"/>
            <position bid="11" code="0" pos="6"/>
            <position bid="10" code="0" pos="7"/>
            <position bid="9" code="0" pos="8"/>
            <position bid="8" code="0" pos="9"/>
            <position bid="7" code="0" pos="10"/>
        </card-recommendations>
        ''',
        )

        self.assertFragmentIn(
            response,
            '''
        <card-price-recommendations>
            <position bid="61" code="0" pos="1"/>
            <position bid="51" code="0" pos="2"/>
            <position bid="41" code="0" pos="3"/>
            <position bid="31" code="0" pos="4"/>
            <position bid="21" code="0" pos="5"/>
            <position bid="11" code="0" pos="6"/>
            <position bid="10" code="0" pos="7"/>
            <position bid="9" code="0" pos="8"/>
            <position bid="8" code="0" pos="9"/>
            <position bid="7" code="0" pos="10"/>
        </card-price-recommendations>
        ''',
        )

    @classmethod
    def prepare_test_cpc_cpa_priority_with_uncollapse(cls):
        cls.index.shops += [
            Shop(fesh=4800, business_fesh=2, name="dsbs магазин Васи", regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=4801, business_fesh=3, name="dsbs магазин Пети", regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=4802, business_fesh=3, name="белый магазин Пети", regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=4803, business_fesh=1, regions=[213]),
            Shop(fesh=4804, business_fesh=4, regions=[213]),
            Shop(fesh=4805, business_fesh=5, regions=[213]),
            Shop(fesh=4806, business_fesh=5, regions=[213]),
            Shop(fesh=4807, business_fesh=6, regions=[213]),
        ]

        cls.index.models += [
            Model(hid=74421, hyperid=654441),
        ]

        cls.index.offers += [
            Offer(
                title="DSBS Offer",
                hid=74421,
                hyperid=654441,
                bid=30,
                price=100,
                fesh=4800,
                business_id=2,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                title="DSBS Offer 2",
                hid=74421,
                hyperid=654441,
                bid=50,
                price=100,
                fesh=4801,
                business_id=3,
                cpa=Offer.CPA_REAL,
            ),
            Offer(hid=74421, hyperid=654441, bid=60, price=100, fesh=4802, business_id=3, cpa=Offer.CPA_REAL),
            Offer(hid=74421, hyperid=654441, bid=20, price=100, fesh=4803, business_id=1),
            Offer(hid=74421, hyperid=654441, bid=40, price=100, fesh=4804, business_id=4),
            Offer(title="CPC Offer 1", hid=74421, hyperid=654441, bid=33, price=100, fesh=4805, business_id=5),
            Offer(title="CPC Offer 2", hid=74421, hyperid=654441, bid=44, price=100, fesh=4806, business_id=5),
            Offer(title="CPC Offer 3", hid=74421, hyperid=654441, bid=35, price=100, fesh=4807, business_id=6),
        ]

    def test_cpc_cpa_priority_with_uncollapse(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-36211
        Проверка работы рекомендатора с расхлопыванием
        '''

        # Проверка работы с выключенным флагом расхопывания.
        # Первые два места занимают DSBS оффера и эти позиции недоступны cpc
        # Все остальные офферы на месте
        response = self.report.request_xml(
            'place=bids_recommender&fesh=4804&hyperid=654441&rids=213&rearr-factors=market_uncollapse_supplier=0;enable_business_id=1'
        )
        self.assertFragmentIn(
            response,
            '''
        <card-price-recommendations current-pos-top="5">
            <position code="4" pos="1"/>
            <position code="4" pos="2"/>
            <position code="4" pos="3"/>
            <position bid="45" code="0" fee="0" pos="4"/>
            <position bid="36" code="0" fee="0" pos="5"/>
            <position bid="34" code="0" fee="0" pos="6"/>
            <position bid="21" code="0" fee="0" pos="7"/>
        </card-price-recommendations>
        ''',
        )

        # Проверка работы с включенным флагом расхопывания.
        response = self.report.request_xml(
            'place=bids_recommender&fesh=4804&hyperid=654441&rids=213&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1'
        )
        self.assertFragmentIn(
            response,
            '''
        <card-price-recommendations current-pos-top="4">
            <position code="4" pos="1"/>
            <position code="4" pos="2"/>
            <position bid="45" code="0" fee="0" pos="3"/>
            <position bid="36" code="0" fee="0" pos="4"/>
            <position bid="21" code="0" fee="0" pos="5"/>
        </card-price-recommendations>
        ''',
        )


if __name__ == '__main__':
    main()

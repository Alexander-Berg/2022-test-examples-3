#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Const,
    Currency,
    DynamicShop,
    GLParam,
    GLType,
    HybridAuctionParam,
    HyperCategory,
    MarketSku,
    MinBidsCategory,
    MinBidsModel,
    MnPlace,
    Model,
    NavCategory,
    NewShopRating,
    Offer,
    Opinion,
    Picture,
    Promo,
    PromoType,
    RegionalModel,
    Shop,
    Tax,
    UrlType,
    Vat,
)
from core.testcase import TestCase, main
from core.types.picture import thumbnails_config
from core.types.bid_correction import BidCorrectionPpGroup, BidCorrection
from core.types.autogen import b64url_md5
from core.types.offer_promo import PromoBlueCashback
from core.report import REQUEST_TIMESTAMP

from datetime import datetime, timedelta
from itertools import count


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta = timedelta(days=10)
nummer = count()

RED, GREEN, BLUE = 1, 2, 3


def get_ware_md5(offer):
    return offer.WareMd5


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        pic_1 = Picture(
            picture_id='IyC4nHslqLtqZJLygVAHe1',
            width=200,
            height=250,
            group_id=466729,
            thumb_mask=thumbnails_config.get_mask_by_names(['190x250', '200x200']),
        )
        pic_2 = Picture(
            picture_id='IyC2nHslqLtqZJLygVAHe2',
            width=300,
            height=300,
            group_id=466729,
            thumb_mask=thumbnails_config.get_mask_by_names(['190x250', '300x300']),
        )
        pic_3 = Picture(
            picture_id='IyC1nHslqLtqZJLygVAHe3',
            width=600,
            height=600,
            group_id=466729,
            thumb_mask=thumbnails_config.get_mask_by_names(['190x250', '600x600']),
        )

        cls.index.offers += [
            Offer(title='offer_01', waremd5='EWyLmZVj2_dabDcT9KJ77g', pictures=[pic_1, pic_2, pic_3]),
            Offer(title='offer_02', waremd5='foATaee2pzREZ_VAxSrngQ', adult=1),
            Offer(title='offer_03', waremd5='AAATaee2pzREZ_VAxSrngQ', adult=1),
        ]

        cls.index.offers += [
            Offer(title='offer_04', waremd5='AAAAAAAAAAAAA_AAAAAAAA', hyperid=1),
            Offer(title='offer_05', waremd5='BAAAAAAAAAAAA_AAAAAAAA', vclusterid=1000000001),
            Offer(title='offer_06', waremd5='CAAAAAAAAAAAA_AAAAAAAA', hyperid=1),
            Offer(title='offer_07', waremd5='DAAAAAAAAAAAA_AAAAAAAA', vclusterid=1000000001),
            Offer(title='offer_08', waremd5='FAAAAAAAAAAAA_AAAAAAAA', vclusterid=1000000002),
            Offer(title='offer_09', waremd5='GAAAAAAAAAAAA_AAAAAAAA'),
            Offer(title='offer_10', waremd5='HAAAAAAAAAAAA_AAAAAAAA', bid=1234),
        ]

    def test_adult(self):
        offers = self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ'
        )
        self.assertEqual(1, len(offers))
        self.assertEqual(offers[0].Title, 'offer_01')

        offers = self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ&adult=1'
        )
        self.assertEqual(3, len(offers))

        expected = set(['offer_01', 'offer_02', 'offer_03'])
        self.assertEqual(set(), expected.difference([offer.Title for offer in offers]))

    def test_model_offers_count(self):
        offers = self.report.request_images('place=images&offerid=AAAAAAAAAAAAA_AAAAAAAA')
        self.assertEqual(1, len(offers))
        self.assertEqual(offers[0].Title, 'offer_04')
        self.assertEqual(offers[0].ModelId, '1')
        self.assertEqual(offers[0].ModelOfferCount, '2')

        offers = self.report.request_images('place=images&offerid=BAAAAAAAAAAAA_AAAAAAAA')
        self.assertEqual(1, len(offers))
        self.assertEqual(offers[0].Title, 'offer_05')
        self.assertEqual(offers[0].ModelId, '1000000001')
        self.assertEqual(offers[0].ModelOfferCount, '2')

        offers = self.report.request_images('place=images&offerid=FAAAAAAAAAAAA_AAAAAAAA')
        self.assertEqual(1, len(offers))
        self.assertEqual(offers[0].Title, 'offer_08')
        self.assertEqual(offers[0].ModelId, '1000000002')
        self.assertEqual(offers[0].ModelOfferCount, '1')

        offers = self.report.request_images('place=images&offerid=GAAAAAAAAAAAA_AAAAAAAA')
        self.assertEqual(1, len(offers))
        self.assertEqual(offers[0].Title, 'offer_09')
        self.assertEqual(offers[0].ModelId, '')
        self.assertEqual(offers[0].ModelOfferCount, '')

    def test_empty_response(self):
        offers = self.report.request_images('place=images&offerid=EAAAAAAAAAAAA_AAAAAAAA')
        self.assertEqual(0, len(offers))

    def test_missing_pp(self):
        self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ&ip=127.0.0.1',
            add_defaults=False,
        )
        self.show_log.expect(pp=41)

    def test_missing_pp_touch(self):
        self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ&ip=127.0.0.1&device=touch',
            add_defaults=False,
        )
        self.show_log.expect(pp=641)

    def test_custom_pp(self):
        self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ&ip=127.0.0.1&pp=234',
            add_defaults=False,
        )
        self.show_log.expect(pp=234)

    def test_images_pp_desktop(self):
        self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ&ip=127.0.0.1&client=images',
            add_defaults=False,
        )
        self.show_log.expect(pp=41)

    def test_images_pp_touch(self):
        self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ&ip=127.0.0.1&device=touch&client=images',
            add_defaults=False,
        )
        self.show_log.expect(pp=641)

    def test_images_similar_pp_desktop(self):
        self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ&ip=127.0.0.1&client=images_similar',
            add_defaults=False,
        )
        self.show_log.expect(pp=43)

    def test_images_similar_pp_touch(self):
        self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ&ip=127.0.0.1&device=touch&client=images_similar',
            add_defaults=False,
        )
        self.show_log.expect(pp=643)

    @classmethod
    def prepare_test_html_in_images(cls):
        cls.index.offers += [Offer(waremd5='OFFER_WITH_HTML_123456', descr='<b>html description</b> here')]

    def test_html_in_images(self):
        offers = self.report.request_images('place=images&offerid=OFFER_WITH_HTML_123456')
        self.assertEqual(offers[0].Description, 'html description here')

    def test_ybid(self):
        offers = self.report.request_images('place=images&offerid=HAAAAAAAAAAAA_AAAAAAAA')
        self.assertEqual(offers[0].Bid, '1234')

    @classmethod
    def prepare_category_ids(cls):
        cls.index.hypertree += [HyperCategory(hid=172131)]

        cls.index.navtree += [NavCategory(nid=172132, hid=172131)]

        cls.index.offers += [Offer(title='test_category_ids', waremd5='cL0S1xwt8gORyTwrvlFxwA', hid=172131)]

    def test_category_ids(self):
        """https://st.yandex-team.ru/MARKETOUT-17213"""

        offers = self.report.request_images('place=images&offerid=cL0S1xwt8gORyTwrvlFxwA')
        self.assertEqual(offers[0].CategoryHid, '172131')
        self.assertEqual(offers[0].CategoryNid, '172132')

    def test_clid_in_url(self):
        """
        https://st.yandex-team.ru/MARKETOUT-18564
        """
        offers = self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&offerid=foATaee2pzREZ_VAxSrngQ&offerid=AAATaee2pzREZ_VAxSrngQ'
        )
        url = offers[0].Url
        self.assertIn('/pof={"clid":["2322165"],"distr_type":"1"}/', url)

    @classmethod
    def prepare_regional_stats(cls):
        cls.index.regional_models += [
            RegionalModel(hyperid=101, price_min=20),
        ]

        cls.index.offers += [
            Offer(hyperid=101, price=50, waremd5='xtQn1DP1JGbMkQreTWCoo1'),
            Offer(hyperid=101, price=60, waremd5='xtQn1DP1JGbMkQreTWCoo2'),
            Offer(hyperid=101, price=70, waremd5='xtQn1DP1JGbMkQreTWCoo3'),
        ]

    def test_regional_stats(self):
        """
        Проверяем, что подтягиваем региональные статистики в картинках
        https://st.yandex-team.ru/MARKETOUT-37620
        """
        offers = self.report.request_images('place=images&offerid=xtQn1DP1JGbMkQreTWCoo1&pp=18')
        self.assertEqual(1, len(offers))
        self.assertEqual('20', offers[0].MinModelPrice)
        self.assertEqual('60', offers[0].AvgModelPrice)

    @classmethod
    def prepare_models_opinions(cls):
        cls.index.models += [Model(hyperid=3, opinion=Opinion(total_count=73, rating=3.85))]

        cls.index.offers += [Offer(hyperid=3, waremd5='KXGI8T3GP_pqjgdd7HfoHQ')]

    def test_models_opinions(self):
        """
        Проверяем, что пробрасывается информация о рейтингах и отзывах моделей
        """
        offers = self.report.request_images('place=images&offerid=KXGI8T3GP_pqjgdd7HfoHQ')
        self.assertEqual(1, len(offers))
        self.assertEqual('3.85', offers[0].ModelRating)
        self.assertEqual('73', offers[0].ModelOpinionsCount)

    @classmethod
    def prepare_union_bid(cls):
        cls.index.offers += [
            Offer(title='offer2_01', waremd5='ACAAAAAAAAAAA_AAAAAA1A', bid=1234, hid=2312, price=10000),
            Offer(title='offer2_02', waremd5='ACAAAAAAAAAAA_AAAAAA1B', bid=234, hid=2312, price=10000),
            Offer(title='offer2_05', waremd5='ACAAAAAAAAAAA_AAAAAAAA', hyperid=10, hid=2312, bid=12345, price=10000),
            Offer(title='offer2_06', waremd5='ACAAAAAAAAAAA_AAAAAAAB', hyperid=10, hid=2312, bid=2345, price=10000),
        ]
        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=2312, geo_group_id=0, price_group_id=0, drr=0.04, search_conversion=0.05, card_conversion=1
            ),
        ]
        cls.index.min_bids_model_stats += [
            MinBidsModel(
                model_id=10, geo_group_id=0, drr=0.01, search_clicks=3, search_orders=1, card_clicks=144, card_orders=9
            )
        ]
        cls.index.bid_correction_pp_groups += [
            BidCorrectionPpGroup(pp_id=41, group_id=4),
        ]
        #        cls.index.bid_correction_geo_groups += [
        #            BidCorrectionGeoGroup(region_id=213, group_id=4),
        #        ]
        cls.index.bid_correction_data += [
            BidCorrection(client_id=-1, category_id=Const.ROOT_HID, geo_group_id=0, pp_group_id=4, bid_coefficient=0.5),
        ]

    # conversion = (9 + 6) / (144 + 6 / 1) = 0.1
    MIN_BID_CARD = 34  # ceil(0.01 * 0.1 * 10000 / 0.3) = 34
    MIN_BID_SEARCH = 67  # ceil(0.02 * 0.05 * 10000 / 0.3) = 67

    # conversion = (1 + 6) / (3 + 6 / 0.05) = 0.1
    MIN_BID_SEARCH_FOR_MODEL = 19  # ceil(0.01 * 0.05691 * 10000 / 0.3) = 19

    MIN_BID_CARD_WITH_BID_CORRECTION = 17
    MIN_BID_SEARCH_WITH_BID_CORRECTION = 34

    def test_mbid(self):
        offers = self.report.request_images(
            'place=images&offerid=ACAAAAAAAAAAA_AAAAAA1A&offerid=ACAAAAAAAAAAA_AAAAAA1B&pp=41'
        )
        self.assertEqual(offers[0].Bid, '1234')
        self.assertEqual(offers[1].Bid, '234')
        self.assertIn('/cp={}/'.format(T.MIN_BID_SEARCH), offers[0].Url)
        self.assertIn('/cp={}/'.format(T.MIN_BID_SEARCH), offers[1].Url)

    def test_union_bid(self):
        offers = self.report.request_images(
            'place=images&offerid=ACAAAAAAAAAAA_AAAAAAAA&offerid=ACAAAAAAAAAAA_AAAAAAAB&pp=41'
        )
        self.assertEqual(offers[0].Bid, '12345')
        self.assertEqual(offers[1].Bid, '2345')
        self.assertIn('/cp={}/'.format(T.MIN_BID_CARD), offers[0].Url)
        self.assertIn('/cp={}/'.format(T.MIN_BID_CARD), offers[1].Url)

    @classmethod
    def prepare_premium_offer(cls):
        cls.index.hybrid_auction_settings += [HybridAuctionParam(category=Const.ROOT_HID, cpc_ctr_for_cpc=0.003)]

        cls.index.navtree += [NavCategory(nid=2934200, hid=2934200, primary=True)]

        cls.index.shops += [
            Shop(fesh=2934210, name='Белый магазин 1', priority_region=213),
            Shop(
                fesh=2934220,
                name='Белый магазин 2',
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=4.25),
            ),
            Shop(fesh=2934230, name='Белый магазин 3', priority_region=213),
            Shop(
                fesh=2934240,
                name='БЕРУ',
                datafeed_id=293424001,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                supplier_type=Shop.FIRST_PARTY,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=12345,
                name='Реально синий магазин',
                datafeed_id=12345,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]
        cls.index.models += [
            Model(
                hyperid=2934211,
                hid=2934200,
                vendor_id=29342001,
                opinion=Opinion(total_count=42, rating=4.85, precise_rating=4.96),
            ),
        ]

        for seq in range(3):
            cls.index.models += [
                Model(
                    hyperid=2934201 + seq,
                    hid=2934200,
                    vendor_id=29342001,
                    opinion=Opinion(total_count=42, rating=4.85, precise_rating=4.96),
                ),
            ]

            cls.index.offers += [
                Offer(
                    hyperid=2934201 + seq,
                    fesh=2934210,
                    price=10000,
                    bid=1,
                    waremd5="AAAAAAAA{}AAAAAAAAAAAAA".format(seq + 1),
                    ts=2934201 + seq,
                ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            ]

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2934201 + seq).respond(0.001)

            if seq % 3 != 1:
                cls.index.offers += [
                    Offer(
                        hyperid=2934201 + seq,
                        fesh=2934220,
                        title='Offer B' + str(seq + 1),
                        descr='Description Offer B' + str(seq + 1),
                        url='www.shop-2934220.ru/offer-b' + str(seq + 1),
                        price=10000,
                        discount=17,
                        bid=50,
                        vendor_id=29342001,
                        vbid=15,
                        adult=1,
                        waremd5="BBBBBBBB{}BBBBBBBBBBBBB".format(seq + 1),
                        ts=2934211 + seq,
                    ),  # premium & top-1; CPM = 100000 * 50 * 0.01 ~ 50000
                    Offer(
                        hyperid=2934201 + seq,
                        fesh=2934220,
                        title='Offer D' + str(seq + 1),
                        price=10000,
                        bid=49,
                        vbid=14,
                        waremd5="DDDDDDDD{}DDDDDDDDDDDDD".format(seq + 1),
                        ts=2934231 + seq,
                    ),  # premium & top-1; CPM = 100000 * 50 * 0.01 ~ 50000
                    Offer(
                        hyperid=2934201 + seq,
                        fesh=2934230,
                        price=5000,
                        bid=10,
                        waremd5="CCCCCCCC{}CCCCCCCCCCCCC".format(seq + 1),
                        ts=2934221 + seq,
                    ),  # CPM = 100000 * 10 * 0.01 ~ 10000
                ]
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2934211 + seq).respond(0.01)
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2934231 + seq).respond(0.01)
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2934221 + seq).respond(0.0001)

        cls.index.mskus += [
            MarketSku(
                hyperid=2934203,
                sku=29342002,
                blue_offers=[
                    BlueOffer(
                        price=500,
                        title='Blue offer E3',
                        descr='Description Blue offer E3',
                        bid=1000,
                        vbid=1000,
                        vat=Vat.NO_VAT,
                        feedid=12345,
                        adult=1,
                        #    url='beru.ru/offer-e3',
                        vendor_id=29342001,
                        waremd5="EEEEEEEE3EEEEEEEEEEEEE",
                        ts=2934243,
                    ),
                ],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2934243).respond(0.1)

    def check_premium_offer(self, offers, original_ware_id=None):
        self.assertEqual(1, len(offers))
        self.assertEqual(offers[0].Title, 'Offer B1')
        self.assertEqual(offers[0].WareMd5, 'BBBBBBBB1BBBBBBBBBBBBA')
        self.assertEqual(offers[0].Currency, 'RUR')
        self.assertEqual(offers[0].Price, '10000')
        self.assertEqual(offers[0].OldPrice, '12048')
        self.assertEqual(offers[0].DiscountPercent, '17')
        self.assertEqual(offers[0].Description, 'Description Offer B1')
        self.assertEqual(offers[0].ShopId, '2934220')
        self.assertEqual(offers[0].ShopRating, '4.25')
        self.assertEqual(offers[0].ShopDomain, 'www.shop-2934220.ru')
        self.assertEqual(offers[0].ShopUrl, 'http://www.shop-2934220.ru/offer-b1')
        self.assertEqual(offers[0].ModelId, '2934201')
        self.assertEqual(offers[0].ModelOfferCount, '4')
        self.assertEqual(offers[0].MinModelPrice, '5000')
        self.assertEqual(offers[0].AvgModelPrice, '10000')
        self.assertEqual(offers[0].ModelRating, '4.96')
        self.assertEqual(offers[0].ModelOpinionsCount, '42')
        self.assertEqual(offers[0].Bid, '50')
        self.assertEqual(offers[0].CategoryHid, '2934200')
        self.assertEqual(offers[0].CategoryNid, '2934200')
        self.assertEqual(offers[0].IsAdult, True)
        self.assertEqual(offers[0].IsPremium, True)
        self.assertEqual(offers[0].OriginalWareMd5, original_ware_id)

        self.show_log.expect(
            hyper_id=2934201,
            click_price=7,
            cpm=50000,
            next_offer_cpm=0.007,
            bid=50,
            min_bid=7,
            is_premium_offer=1,
            price=10000,
            hyper_cat_id=2934200,
            nid=2934200,
            position=0,
            shop_id=2934220,
            vendor_click_price=0,
            vendor_id=29342001,
            vbid=0,
        )

    def check_blue_premium_offer(self, offers, original_ware_id=None):
        self.assertEqual(1, len(offers))
        self.assertEqual(offers[0].Title, 'Blue offer E3')
        self.assertEqual(offers[0].WareMd5, 'EEEEEEEE3EEEEEEEEEEEEA')
        self.assertEqual(offers[0].Currency, 'RUR')
        self.assertEqual(offers[0].Price, '500')
        self.assertEqual(offers[0].Description, 'Description Blue offer E3')
        self.assertEqual(offers[0].ShopId, '2934240')
        self.assertEqual(offers[0].ShopRating, '5')
        self.assertEqual(offers[0].ShopDomain, 'beru.ru')
        self.assertEqual(offers[0].ModelId, '2934203')
        self.assertEqual(offers[0].ModelOfferCount, '5')
        self.assertEqual(offers[0].MinModelPrice, '500')
        self.assertEqual(offers[0].AvgModelPrice, '7100')
        self.assertEqual(offers[0].ModelRating, '4.96')
        self.assertEqual(offers[0].ModelOpinionsCount, '42')
        self.assertEqual(offers[0].Bid, '1000')
        self.assertEqual(offers[0].CategoryHid, '2934200')
        self.assertEqual(offers[0].CategoryNid, '2934200')
        self.assertEqual(offers[0].IsAdult, True)
        self.assertEqual(offers[0].IsPremium, True)
        self.assertEqual(offers[0].OriginalWareMd5, original_ware_id)

        self.show_log.expect(
            hyper_id=2934203,
            click_price=7,
            cpm=20000000,
            next_offer_cpm=0.65,
            bid=1000,
            min_bid=1,
            is_premium_offer=1,
            price=500,
            hyper_cat_id=2934200,
            nid=2934200,
            position=0,
            shop_id=2934240,
            vendor_click_price=0,
            vendor_id=29342001,
            vbid=1000,
        )

    def test_premium_offer(self):
        """
        Проверяем, что при наличии hyperid под флагом market_premium_offer_in_images_place
        на плейсе images возвращается премиальный оффер, если он существует
        """
        rearrs = ';'.join(
            ['market_premium_offer_in_images_place=1', 'market_ranging_cpa_by_ue_in_top_cpa_multiplier=1']
        )

        for hyperid in ['2934201', '']:
            # Белый премиальный оффер
            offers = self.report.request_images(
                'place=images&offerid=CCCCCCCC1CCCCCCCCCCCCC&hyperid={}&rearr-factors={}&rids=213&adult=1&show-urls=external&pp=18'.format(
                    hyperid, rearrs
                )
            )
            self.check_premium_offer(offers, 'CCCCCCCC1CCCCCCCCCCCCA')

            # Белый премиальный не выводится дважды при запросе его самого
            offers = self.report.request_images(
                'place=images&offerid=BBBBBBBB1BBBBBBBBBBBBB&hyperid={}&rearr-factors={}&rids=213&adult=1&pp=18'.format(
                    hyperid, rearrs
                )
            )
            self.check_premium_offer(offers, 'BBBBBBBB1BBBBBBBBBBBBA')

        for hyperid in ['2934202', '']:
            # Нет премиального оффера
            offers = self.report.request_images(
                'place=images&offerid=AAAAAAAA2AAAAAAAAAAAAA&hyperid={}&rearr-factors={}&rids=213&adult=1&pp=18'.format(
                    hyperid, rearrs
                )
            )
            self.assertEqual(1, len(offers))
            self.assertEqual(offers[0].WareMd5, 'AAAAAAAA2AAAAAAAAAAAAA')
            self.assertEqual(offers[0].IsPremium, False)

    def _inspect_images_unistat(self, query):
        # Сохраняем счётчики до запроса
        tass_data = self.report.request_tass()

        # Делаем запрос
        self.report.request_images(query)

        # Сохраняем счётчики после запроса
        tass_data_new = self.report.request_tass()

        # Сравниваем
        # Общее число запросов к нужному плейсу
        self.assertEqual(
            tass_data.get('images_premium_request_count_dmmm', 0) + 1,
            tass_data_new.get('images_premium_request_count_dmmm', 0),
        )
        # Тайминги как-то считаются
        self.assertIn('images_full_request_time_hgram', tass_data_new.keys())
        self.assertIn('images_premium_request_time_hgram', tass_data_new.keys())

    def test_premium_offer_timeout(self):
        """
        Проверяем, что при наличии hyperid под флагом market_premium_offer_in_images_place
        на плейсе images возвращается премиальный оффер, если он существует
        """
        # Белый премиальный оффер не находится из-за таймаута
        _ = self._inspect_images_unistat(
            'place=images&offerid=CCCCCCCC1CCCCCCCCCCCCC&hyperid=2934201&rearr-factors=market_premium_offer_in_images_place=1;market_images_premium_timeout=50&rids=213&adult=1&pp=18'
        )

    @classmethod
    def prepare_premium_offer_gl_filters(cls):

        cls.index.navtree += [NavCategory(nid=2964300, hid=2964300, primary=True)]

        cls.index.gltypes += [
            GLType(
                param_id=296430001,
                hid=2964300,
                gltype=GLType.ENUM,
                values=[RED, GREEN, BLUE],
                xslname="color",
                cluster_filter=True,
            ),
            GLType(
                param_id=296430002,
                hid=2964300,
                gltype=GLType.ENUM,
                values=[RED, GREEN, BLUE],
                xslname="color_glob",
                cluster_filter=True,
            ),
            GLType(
                param_id=296430003,
                hid=2964300,
                gltype=GLType.ENUM,
                values=[RED, GREEN, BLUE],
                xslname="vendor_color",
                cluster_filter=True,
            ),
            GLType(
                param_id=296430004,
                hid=2964300,
                gltype=GLType.ENUM,
                values=[7, 8, 9],
                xslname="size",
                cluster_filter=True,
            ),
            GLType(
                param_id=296430005,
                hid=2964300,
                gltype=GLType.ENUM,
                values=[10, 11, 12],
                xslname="model_param",
            ),
        ]

        for seq in range(0, 4):
            cls.index.models += [Model(hyperid=2964301 + seq, hid=2964300)]

        cls.index.offers += [
            Offer(
                hyperid=2964301,
                price=10000,
                bid=1,
                glparams=[
                    GLParam(param_id=296430001, value=RED),
                    GLParam(param_id=296430002, value=RED),
                    GLParam(param_id=296430004, value=7),
                    GLParam(param_id=296430005, value=10),
                ],
                waremd5="MMMMMMMM1MMMMMMMMMMMMM",
                ts=2964301,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964301,
                price=10000,
                bid=50,
                glparams=[
                    GLParam(param_id=296430001, value=RED),
                    GLParam(param_id=296430002, value=GREEN),
                    GLParam(param_id=296430004, value=8),
                    GLParam(param_id=296430005, value=11),
                ],
                waremd5="NNNNNNNN1NNNNNNNNNNNNN",
                ts=2964311,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964302,
                price=10000,
                bid=1,
                glparams=[
                    GLParam(param_id=296430001, value=RED),
                    GLParam(param_id=296430002, value=RED),
                    GLParam(param_id=296430004, value=7),
                    GLParam(param_id=296430005, value=10),
                ],
                waremd5="MMMMMMMM2MMMMMMMMMMMMM",
                ts=2964302,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964302,
                price=10000,
                bid=50,
                glparams=[
                    GLParam(param_id=296430001, value=GREEN),
                    GLParam(param_id=296430002, value=GREEN),
                    GLParam(param_id=296430004, value=8),
                    GLParam(param_id=296430005, value=11),
                ],
                waremd5="NNNNNNNN2NNNNNNNNNNNNN",
                ts=2964312,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964303,
                price=10000,
                bid=1,
                glparams=[
                    GLParam(param_id=296430001, value=RED),
                    GLParam(param_id=296430002, value=RED),
                    GLParam(param_id=296430004, value=7),
                    GLParam(param_id=296430005, value=10),
                ],
                waremd5="MMMMMMMM3MMMMMMMMMMMMM",
                ts=2964303,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964303,
                price=10000,
                bid=50,
                glparams=[
                    GLParam(param_id=296430004, value=8),
                    GLParam(param_id=296430005, value=11),
                ],
                waremd5="NNNNNNNN3NNNNNNNNNNNNN",
                ts=2964313,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964304,
                price=10000,
                bid=1,
                glparams=[
                    GLParam(param_id=296430004, value=7),
                    GLParam(param_id=296430005, value=10),
                ],
                waremd5="MMMMMMMM4MMMMMMMMMMMMM",
                ts=2964304,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964304,
                price=10000,
                bid=50,
                glparams=[
                    GLParam(param_id=296430001, value=RED),
                    GLParam(param_id=296430002, value=RED),
                    GLParam(param_id=296430004, value=8),
                    GLParam(param_id=296430005, value=11),
                ],
                waremd5="NNNNNNNN4NNNNNNNNNNNNN",
                ts=2964314,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964305,
                price=10000,
                bid=1,
                glparams=[
                    GLParam(param_id=296430003, value=RED),
                    GLParam(param_id=296430002, value=RED),
                    GLParam(param_id=296430004, value=7),
                    GLParam(param_id=296430005, value=10),
                ],
                waremd5="MMMMMMMM5MMMMMMMMMMMMM",
                ts=2964305,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=2964305,
                price=10000,
                bid=50,
                glparams=[
                    GLParam(param_id=296430003, value=RED),
                    GLParam(param_id=296430002, value=GREEN),
                    GLParam(param_id=296430004, value=8),
                    GLParam(param_id=296430005, value=11),
                ],
                waremd5="NNNNNNNN5NNNNNNNNNNNNN",
                ts=2964315,
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
        ]
        for seq in range(0, 4):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2964311 + seq).respond(0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2964301 + seq).respond(0.001)

    def test_premium_offer_gl_filters(self):
        """
        Проверяем, что под флагом мы не выводим премиальный оффер,
        если его цвет не совпадает с цветом исходного
        """
        # Сохраняем счётчики до запросов
        tass_data = self.report.request_tass()

        # цвета совпадают - на выдаче премиальные офферы
        for hyperid in ['2964301,2964305', '']:
            offers = self.report.request_images(
                'place=images&offerid=MMMMMMMM1MMMMMMMMMMMMM,MMMMMMMM5MMMMMMMMMMMMM&hyperid={}&rearr-factors=market_premium_offer_in_images_place=1;market_images_premium_compare_colors=1'.format(
                    hyperid
                )
            )
            self.assertEqual(2, len(offers))
            offer_ids = [offers[0].WareMd5, offers[1].WareMd5]
            self.assertEqual(sorted(offer_ids), ['NNNNNNNN1NNNNNNNNNNNNA', 'NNNNNNNN5NNNNNNNNNNNNA'])

        # цвета не совпадают - на выдаче исходный оффер
        for hyperid in ['2964302', '']:
            offers = self.report.request_images(
                'place=images&offerid=MMMMMMMM2MMMMMMMMMMMMM&hyperid={}&rearr-factors=market_premium_offer_in_images_place=1;market_images_premium_compare_colors=1'.format(
                    hyperid
                )
            )
            self.assertEqual(1, len(offers))
            self.assertEqual(offers[0].WareMd5, 'MMMMMMMM2MMMMMMMMMMMMA')

        # цвет премиального оффера не известен - на выдаче исходный оффер
        for hyperid in ['2964303', '']:
            offers = self.report.request_images(
                'place=images&offerid=MMMMMMMM3MMMMMMMMMMMMM&hyperid={}&rearr-factors=market_premium_offer_in_images_place=1;market_images_premium_compare_colors=1'.format(
                    hyperid
                )
            )
            self.assertEqual(1, len(offers))
            self.assertEqual(offers[0].WareMd5, 'MMMMMMMM3MMMMMMMMMMMMA')

        # цвет исходного оффера не известен, а у премиального он есть - на выдаче исходный оффер
        for hyperid in ['2964304', '']:
            offers = self.report.request_images(
                'place=images&offerid=MMMMMMMM4MMMMMMMMMMMMM&hyperid={}&rearr-factors=market_premium_offer_in_images_place=1;market_images_premium_compare_colors=1'.format(
                    hyperid
                )
            )
            self.assertEqual(1, len(offers))
            self.assertEqual(offers[0].WareMd5, 'MMMMMMMM4MMMMMMMMMMMMA')

        # Сохраняем счётчики после запросов
        tass_data_new = self.report.request_tass()
        # Проверяем счетчик отфильтрованных
        self.assertEqual(
            tass_data.get('images_premium_gl_filtered_count_dmmm', 0) + 6,
            tass_data_new.get('images_premium_gl_filtered_count_dmmm', 0),
        )

    @classmethod
    def prepare_category_ban(cls):
        cls.index.hypertree += [
            HyperCategory(hid=16335429, children=[HyperCategory(hid=8429451)]),
            HyperCategory(hid=90669),
            HyperCategory(hid=91012),
            HyperCategory(hid=2933400),
        ]

        cls.index.offers += [
            Offer(title='offer_legal_category', waremd5='PPPPPPPP1PPPPPPPPPPPPA', hid=2933400),
            Offer(title='offer_banned_1_category', waremd5='PPPPPPPP2PPPPPPPPPPPPA', hid=16335429),
            Offer(title='offer_banned_1_category_child', waremd5='PPPPPPPP3PPPPPPPPPPPPA', hid=8429451),
            Offer(title='offer_banned_1_2_category', waremd5='PPPPPPPP4PPPPPPPPPPPPA', hid=90669),
            Offer(title='offer_banned_1_2_3_category', waremd5='PPPPPPPP5PPPPPPPPPPPPA', hid=91012),
        ]

    def test_category_ban(self):
        offer_ids_full = [
            'PPPPPPPP1PPPPPPPPPPPPA',
            'PPPPPPPP2PPPPPPPPPPPPA',
            'PPPPPPPP3PPPPPPPPPPPPA',
            'PPPPPPPP4PPPPPPPPPPPPA',
            'PPPPPPPP5PPPPPPPPPPPPA',
        ]
        offer_ids_str = ','.join(offer_ids_full)
        offers = self.report.request_images('place=images&offerid={}'.format(offer_ids_str))
        self.assertEqual(5, len(offers))
        offer_ids = (offers[i].WareMd5 for i in range(0, len(offers)))
        self.assertEqual(sorted(offer_ids), offer_ids_full)

        offers = self.report.request_images(
            'place=images&rearr-factors=market_images_category_ban=1&offerid={}'.format(offer_ids_str)
        )
        self.assertEqual(1, len(offers))
        offer_ids = (offers[i].WareMd5 for i in range(0, len(offers)))
        self.assertEqual(sorted(offer_ids), ['PPPPPPPP1PPPPPPPPPPPPA'])

        offers = self.report.request_images(
            'place=images&rearr-factors=market_images_category_ban=2&offerid={}'.format(offer_ids_str)
        )
        self.assertEqual(3, len(offers))
        offer_ids = (offers[i].WareMd5 for i in range(0, len(offers)))
        self.assertEqual(
            sorted(offer_ids), ['PPPPPPPP1PPPPPPPPPPPPA', 'PPPPPPPP2PPPPPPPPPPPPA', 'PPPPPPPP3PPPPPPPPPPPPA']
        )

        offers = self.report.request_images(
            'place=images&rearr-factors=market_images_category_ban=3&offerid={}'.format(offer_ids_str)
        )
        self.assertEqual(4, len(offers))
        offer_ids = (offers[i].WareMd5 for i in range(0, len(offers)))
        self.assertEqual(
            sorted(offer_ids),
            ['PPPPPPPP1PPPPPPPPPPPPA', 'PPPPPPPP2PPPPPPPPPPPPA', 'PPPPPPPP3PPPPPPPPPPPPA', 'PPPPPPPP4PPPPPPPPPPPPA'],
        )

    @classmethod
    def prepare_model_urls(cls):
        cls.index.models += [
            Model(
                title='Модель 01',
                hyperid=3008601,
                hid=3008600,
                vendor_id=30086001,
                vbid=15,
                opinion=Opinion(total_count=42, rating=4.85, precise_rating=4.96),
            ),
            Model(
                title='Модель 02',
                hyperid=3008602,
                hid=3008600,
                vendor_id=30086001,
                vbid=15,
                opinion=Opinion(total_count=42, rating=4.85, precise_rating=4.96),
            ),
            Model(
                title='Модель 03 без офферов',
                hyperid=3008603,
                hid=3008600,
                vendor_id=30086001,
                vbid=15,
                opinion=Opinion(total_count=42, rating=4.85, precise_rating=4.96),
            ),
        ]

        cls.index.offers += [
            Offer(
                title='matched_offer_01',
                descr='description_offer_01',
                hyperid=3008601,
                hid=3008600,
                fesh=3008610,
                waremd5='QQQQQQQQ1QQQQQQQQQQQQQ',
                price=10000,
                discount=21,
            ),
            Offer(hyperid=3008601, fesh=3008620, price=7000),
            Offer(hyperid=3008601, fesh=3008630, waremd5='TTTTTTTT1TTTTTTTTTTTTT', price=5000),
            Offer(
                title='matched_offer_02',
                hyperid=3008602,
                hid=3008600,
                adult=1,
                fesh=3008620,
                waremd5='RRRRRRRR1RRRRRRRRRRRRR',
            ),
            Offer(title='non_matched_offer_03', waremd5='SSSSSSSS1SSSSSSSSSSSSS', fesh=3008620),
        ]

        cls.index.navtree += [NavCategory(nid=300860000, hid=3008600, primary=True)]

        cls.index.shops += [
            Shop(
                fesh=3008610,
                name='Белый магазин 1',
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
            Shop(
                fesh=3008620,
                name='Белый магазин 2',
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
            Shop(
                fesh=3008630,
                name='Белый магазин 3',
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
        ]

    def check_model_urls(self, offers, touch, clid):
        self.assertEqual(3, len(offers))
        sorted_offers = sorted(offers, key=get_ware_md5)

        touch_prefix = 'm.' if touch else ''

        self.assertEqual(sorted_offers[0].Title, 'matched_offer_01')
        self.assertEqual(sorted_offers[0].WareMd5, 'QQQQQQQQ1QQQQQQQQQQQQQ')
        self.assertEqual(sorted_offers[0].Currency, 'RUR')
        self.assertEqual(sorted_offers[0].Price, '10000')
        self.assertEqual(sorted_offers[0].OldPrice, '12658')
        self.assertEqual(sorted_offers[0].DiscountPercent, '21')
        self.assertEqual(sorted_offers[0].Description, 'description_offer_01')
        self.assertEqual(sorted_offers[0].ShopId, '3008610')
        self.assertEqual(sorted_offers[0].ShopRating, '4.96')
        self.assertEqual(sorted_offers[0].ShopDomain, '{}market.yandex.ru'.format(touch_prefix))
        self.assertTrue(
            'https://{}market.yandex.ru/product--model-01/3008601?clid={}&hid=3008600&lr=213&nid=300860000&wprid='.format(
                touch_prefix, clid
            )
            in sorted_offers[0].ShopUrl
        )
        self.assertTrue(
            'https://{}market.yandex.ru/product--model-01/3008601?clid={}&hid=3008600&lr=213&nid=300860000&wprid='.format(
                touch_prefix, clid
            )
            in sorted_offers[0].Url
        )
        self.assertEqual(sorted_offers[0].ModelId, '3008601')
        self.assertEqual(sorted_offers[0].ModelOfferCount, '3')
        self.assertEqual(sorted_offers[0].MinModelPrice, '5000')
        self.assertEqual(sorted_offers[0].AvgModelPrice, '7000')
        self.assertEqual(sorted_offers[0].ModelRating, '4.96')
        self.assertEqual(sorted_offers[0].ModelOpinionsCount, '42')
        self.assertEqual(sorted_offers[0].Bid, '')
        self.assertEqual(sorted_offers[0].CategoryHid, '3008600')
        self.assertEqual(sorted_offers[0].CategoryNid, '300860000')
        self.assertEqual(sorted_offers[0].IsAdult, False)
        self.assertEqual(sorted_offers[0].OriginalWareMd5, 'QQQQQQQQ1QQQQQQQQQQQQQ')

        self.show_log.expect(
            hyper_id=3008601,
            click_price=0,
            bid=0,
            min_bid=0,
            price=0,
            hyper_cat_id=3008600,
            nid=300860000,
            position=0,
            shop_id=99999999,
            vendor_price=15,
            vendor_id=30086001,
            vc_bid=15,
        )

        self.assertEqual(sorted_offers[1].Title, 'matched_offer_02')
        self.assertEqual(sorted_offers[1].WareMd5, 'RRRRRRRR1RRRRRRRRRRRRQ')
        self.assertEqual(sorted_offers[1].ShopId, '3008620')
        self.assertTrue(
            'https://{}market.yandex.ru/product--model-02/3008602?clid={}&hid=3008600&lr=213&nid=300860000&wprid='.format(
                touch_prefix, clid
            )
            in sorted_offers[1].ShopUrl
        )
        self.assertTrue(
            'https://{}market.yandex.ru/product--model-02/3008602?clid={}&hid=3008600&lr=213&nid=300860000&wprid='.format(
                touch_prefix, clid
            )
            in sorted_offers[1].Url
        )
        self.assertEqual(sorted_offers[1].IsAdult, True)

        self.show_log.expect(hyper_id=3008602, click_price=0, bid=0, position=0, shop_id=99999999)

        self.assertEqual(sorted_offers[2].WareMd5, 'SSSSSSSS1SSSSSSSSSSSSQ')
        self.assertNotEqual(sorted_offers[2].ShopDomain, '{}market.yandex.ru'.format(touch_prefix))

        self.show_log.expect(bid=1, ware_md5='SSSSSSSS1SSSSSSSSSSSSQ')

    def test_model_urls(self):
        """Проверяем замену магазинного урла на маркетный"""
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(3008630)]

        rearrs = ';'.join(['market_replace_url_images_onstock=1'])
        offer_ids = ','.join(['QQQQQQQQ1QQQQQQQQQQQQQ', 'RRRRRRRR1RRRRRRRRRRRRR', 'SSSSSSSS1SSSSSSSSSSSSS'])
        request = 'place=images&offerid={}&rids=213&rearr-factors={}&adult=1&show-urls={}&pp=18'

        for device in ['', '&device=desktop']:
            offers = self.report.request_images(request.format(offer_ids, rearrs, device))
            self.check_model_urls(offers, touch=False, clid=840)

        for device in ['&device=touch', '&device=tablet']:
            offers = self.report.request_images(request.format(offer_ids, rearrs, device))
            self.check_model_urls(offers, touch=True, clid=803)

    def check_model_no_offer(self, offers, touch, clid):
        self.assertEqual(1, len(offers))
        sorted_offers = sorted(offers, key=get_ware_md5)

        touch_prefix = 'm.' if touch else ''

        self.assertEqual(sorted_offers[0].Title, 'Модель 01')
        self.assertEqual(sorted_offers[0].WareMd5, '')
        self.assertEqual(sorted_offers[0].Bid, '')
        self.assertEqual(str(sorted_offers[0].Price), '5000')
        self.assertEqual(sorted_offers[0].ShopId, '')
        self.assertEqual(sorted_offers[0].ShopRating, '4.96')
        self.assertEqual(sorted_offers[0].ShopDomain, '{}market.yandex.ru'.format(touch_prefix))
        self.assertTrue(
            'https://{}market.yandex.ru/product--model-01/3008601?clid={}&hid=3008600&lr=213&nid=300860000&wprid='.format(
                touch_prefix, clid
            )
            in sorted_offers[0].ShopUrl
        )
        self.assertTrue(
            'https://{}market.yandex.ru/product--model-01/3008601?clid={}&hid=3008600&lr=213&nid=300860000&wprid='.format(
                touch_prefix, clid
            )
            in sorted_offers[0].Url
        )
        self.assertEqual(sorted_offers[0].ModelId, '3008601')
        self.assertEqual(sorted_offers[0].ModelOfferCount, '3')
        self.assertEqual(sorted_offers[0].MinModelPrice, '5000')
        self.assertEqual(sorted_offers[0].AvgModelPrice, '7000')
        self.assertEqual(sorted_offers[0].ModelRating, '4.96')
        self.assertEqual(sorted_offers[0].ModelOpinionsCount, '42')
        self.assertEqual(sorted_offers[0].Bid, '')
        self.assertEqual(sorted_offers[0].CategoryHid, '3008600')
        self.assertEqual(sorted_offers[0].CategoryNid, '300860000')
        self.assertEqual(sorted_offers[0].IsAdult, False)
        self.assertEqual(sorted_offers[0].OriginalWareMd5, '')

        self.show_log.expect(
            hyper_id=3008601,
            click_price=0,
            bid=0,
            min_bid=0,
            price=0,
            hyper_cat_id=3008600,
            nid=300860000,
            position=0,
            shop_id=99999999,
            vendor_price=15,
            vendor_id=30086001,
            vc_bid=15,
        )

    def test_model_urls_has_gone(self):
        """Проверяем замену магазинного урла на маркетный
        в случае, если исходного оффера нет на маркете
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(3008630)]

        rearrs = ';'.join(['market_replace_url_images_onstock=1', 'market_replace_url_images_has_gone=1'])
        offer_request = 'place=images&offerid=TTTTTTTT1TTTTTTTTTTTTT&hyperid=3008601&rids=213&rearr-factors={}&adult=1&show-urls={}&pp=18'
        no_offer_request = 'place=images&hyperid=3008603&rids=213&rearr-factors={}&adult=1&show-urls={}&&pp=18'

        for device in ['', '&device=desktop']:
            offers = self.report.request_images(offer_request.format(rearrs, device))
            self.check_model_no_offer(offers, touch=False, clid=840)

            # Проверяем, что модель "Не в продаже" не находится
            offers = self.report.request_images(no_offer_request.format(rearrs, device))
            self.assertEqual(0, len(offers))

        for device in ['&device=touch', '&device=tablet']:
            offers = self.report.request_images(offer_request.format(rearrs, device))
            self.check_model_no_offer(offers, touch=True, clid=803)

            # Проверяем, что модель "Не в продаже" не находится
            offers = self.report.request_images(no_offer_request.format(rearrs, device))
            self.assertEqual(0, len(offers))

    def test_multiple_pictures(self):
        offers = self.report.request_images(
            'place=images&offerid=EWyLmZVj2_dabDcT9KJ77g&rearr-factors=market_images_place_add_pictures=1'
        )
        self.assertEqual(1, len(offers))
        self.assertEqual(
            offers[0].PictureUrl,
            'http://avatars.mdst.yandex.net/get-marketpic/466729/market_IyC4nHslqLtqZJLygVAHew/190x250',
        )
        self.assertEqual(offers[0].PictureWidth, '200')
        self.assertEqual(offers[0].PictureHeight, '250')
        self.assertEqual(3, len(offers[0].Pictures))
        self.assertEqual(
            offers[0].Pictures[0].Url,
            'http://avatars.mdst.yandex.net/get-marketpic/466729/market_IyC4nHslqLtqZJLygVAHew/190x250',
        )
        self.assertEqual(offers[0].Pictures[0].Width, '200')
        self.assertEqual(offers[0].Pictures[0].Height, '250')
        self.assertEqual(
            offers[0].Pictures[1].Url,
            'http://avatars.mdst.yandex.net/get-marketpic/466729/market_IyC2nHslqLtqZJLygVAHew/190x250',
        )
        self.assertEqual(offers[0].Pictures[1].Width, '250')
        self.assertEqual(offers[0].Pictures[1].Height, '250')
        self.assertEqual(
            offers[0].Pictures[2].Url,
            'http://avatars.mdst.yandex.net/get-marketpic/466729/market_IyC1nHslqLtqZJLygVAHew/190x250',
        )
        self.assertEqual(offers[0].Pictures[2].Width, '250')
        self.assertEqual(offers[0].Pictures[2].Height, '250')

    @classmethod
    def prepare_additional_offers(cls):
        cls.index.models += [
            Model(hyperid=3496401),
            Model(hyperid=3496402),
            Model(hyperid=3496403),
        ]

        cls.index.offers += [
            Offer(hyperid=3496401, bid=203, waremd5='349640101_pqjgdd7HfoHQ'),
            Offer(hyperid=3496401, bid=153, waremd5='349640102_pqjgdd7HfoHQ'),
            Offer(hyperid=3496401, bid=103, waremd5='349640103_pqjgdd7HfoHQ'),
            Offer(hyperid=3496401, bid=53, waremd5='349640104_pqjgdd7HfoHQ'),
            Offer(hyperid=3496401, bid=13, waremd5='349640105_pqjgdd7HfoHQ'),
            Offer(hyperid=3496402, bid=202, waremd5='349640201_pqjgdd7HfoHQ'),
            Offer(hyperid=3496402, bid=152, waremd5='349640202_pqjgdd7HfoHQ'),
            Offer(hyperid=3496402, bid=102, waremd5='349640203_pqjgdd7HfoHQ'),
            Offer(hyperid=3496402, bid=52, waremd5='349640204_pqjgdd7HfoHQ'),
            Offer(hyperid=3496402, bid=12, waremd5='349640205_pqjgdd7HfoHQ'),
            Offer(hyperid=3496403, bid=201, waremd5='349640301_pqjgdd7HfoHQ'),
            Offer(hyperid=3496403, bid=151, waremd5='349640302_pqjgdd7HfoHQ'),
            Offer(hyperid=3496403, bid=101, waremd5='349640303_pqjgdd7HfoHQ'),
            Offer(hyperid=3496403, bid=51, waremd5='349640304_pqjgdd7HfoHQ'),
            Offer(hyperid=3496403, bid=11, waremd5='349640305_pqjgdd7HfoHQ'),
        ]

    def test_additional_offers(self):
        """Проверяем возврат дополнительных офферов,
        запрашиваем 3 основных и по 3 дополнительных
        проверяем, что суммарно 12 офферов
        """

        offers = self.report.request_images(
            'place=images&offerid=349640103_pqjgdd7HfoHQ,349640205_pqjgdd7HfoHQ,349640301_pqjgdd7HfoHQ&hyperid=3496401,3496402,3496403&rearr-factors=market_images_additional_offers=3'
        )
        self.assertEqual(12, len(offers))
        self.assertEqual(offers[0].WareMd5, '349640301_pqjgdd7HfoHQ')
        self.assertEqual(offers[1].WareMd5, '349640103_pqjgdd7HfoHQ')
        self.assertEqual(offers[2].WareMd5, '349640205_pqjgdd7HfoHQ')
        self.assertEqual(offers[3].WareMd5, '349640201_pqjgdd7HfoHQ')
        self.assertEqual(offers[4].WareMd5, '349640202_pqjgdd7HfoHQ')
        self.assertEqual(offers[5].WareMd5, '349640203_pqjgdd7HfoHQ')
        self.assertEqual(offers[6].WareMd5, '349640302_pqjgdd7HfoHQ')
        self.assertEqual(offers[7].WareMd5, '349640303_pqjgdd7HfoHQ')
        self.assertEqual(offers[8].WareMd5, '349640304_pqjgdd7HfoHQ')
        self.assertEqual(offers[9].WareMd5, '349640101_pqjgdd7HfoHQ')
        self.assertEqual(offers[10].WareMd5, '349640102_pqjgdd7HfoHQ')
        self.assertEqual(offers[11].WareMd5, '349640104_pqjgdd7HfoHQ')

        self.show_log.expect(position=1, ware_md5='349640301_pqjgdd7HfoHQ')
        self.show_log.expect(position=2, ware_md5='349640103_pqjgdd7HfoHQ')
        self.show_log.expect(position=3, ware_md5='349640205_pqjgdd7HfoHQ')
        self.show_log.expect(position=4, ware_md5='349640201_pqjgdd7HfoHQ')
        self.show_log.expect(position=5, ware_md5='349640202_pqjgdd7HfoHQ')
        self.show_log.expect(position=6, ware_md5='349640203_pqjgdd7HfoHQ')
        self.show_log.expect(position=7, ware_md5='349640302_pqjgdd7HfoHQ')
        self.show_log.expect(position=8, ware_md5='349640303_pqjgdd7HfoHQ')
        self.show_log.expect(position=9, ware_md5='349640304_pqjgdd7HfoHQ')
        self.show_log.expect(position=10, ware_md5='349640101_pqjgdd7HfoHQ')
        self.show_log.expect(position=11, ware_md5='349640102_pqjgdd7HfoHQ')
        self.show_log.expect(position=12, ware_md5='349640104_pqjgdd7HfoHQ')

    @classmethod
    def prepare_cpa_offers(cls):
        cls.index.models += [
            Model(hyperid=3534301),
            Model(hyperid=3534302),
            Model(hyperid=3534303),
            Model(hyperid=3534304),
        ]

        blue_cashback_1 = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            key=b64url_md5(next(nummer)),
            start_date=now - delta,
            end_date=now + delta,
            blue_cashback=PromoBlueCashback(share=0.15, version=1, priority=1),
        )

        cls.index.mskus += [
            MarketSku(
                hyperid=3534303,
                sku=35343003,
                blue_offers=[
                    BlueOffer(
                        title='Blue offer E3',
                        bid=1000,
                        vbid=1000,
                        vat=Vat.NO_VAT,
                        feedid=12345,
                        cpa=Offer.CPA_REAL,
                        waremd5="353430302_pqjgdd7HfoHQ",
                        promo=blue_cashback_1,
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=3534310, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=3534320, priority_region=213, cpa=Shop.CPA_NO),
        ]

        cls.index.offers += [
            Offer(hyperid=3534301, bid=203, waremd5='353430101_pqjgdd7HfoHQ', fesh=3534320),
            Offer(
                hyperid=3534301,
                bid=153,
                waremd5='353430102_pqjgdd7HfoHQ',
                cpa=Offer.CPA_REAL,
                fesh=3534310,
                has_url=False,
            ),
            Offer(hyperid=3534302, bid=202, waremd5='353430201_pqjgdd7HfoHQ', cpa=Offer.CPA_REAL, fesh=3534310),
            Offer(hyperid=3534302, bid=152, waremd5='353430202_pqjgdd7HfoHQ', fesh=3534320),
            Offer(hyperid=3534303, bid=201, waremd5='353430301_pqjgdd7HfoHQ', fesh=3534320),
            Offer(hyperid=3534304, bid=201, waremd5='353430401_pqjgdd7HfoHQ', fesh=3534320),
        ]

    def test_params_in_market_url(self):
        """
        https://st.yandex-team.ru/MARKETOUT-38632
        Проверяем, что пишем в шифрованные урлы на маркет
        параметры clid, wprid, utm_source_service
        и icookie под отдельным флагом
        """
        rearr_factors = ['market_images_add_params_to_url=1']
        offers = self.report.request_images(
            'place=images&src_pof=712&offerid=353430302_pqjgdd7HfoHQ&rids=213&pp=18&x-yandex-encrypted-icookie=1123456&reqid=cb8f7a995444ab4dc3215cad33fcd6bb&rearr-factors={}'.format(
                ';'.join(rearr_factors)
            )
        )
        url = offers[0].Url
        self.assertIn('src_pof=712&utm_source_service=img&wprid=cb8f7a995444ab4dc3215cad33fcd6bb', url)

        rearr_factors = ['market_images_add_params_to_url=1', 'market_images_add_icookie_to_url=1']
        offers = self.report.request_images(
            'place=images&src_pof=712&offerid=353430302_pqjgdd7HfoHQ&rids=213&pp=18&x-yandex-encrypted-icookie=1123456&reqid=cb8f7a995444ab4dc3215cad33fcd6bb&rearr-factors={}'.format(
                ';'.join(rearr_factors)
            )
        )
        url = offers[0].Url
        self.assertIn(
            'src_pof=712&utm_source_service=img&wprid=cb8f7a995444ab4dc3215cad33fcd6bb',
            url,
        )
        self.assertIn('&icookie=1123456', url)

        # для немаркетного оффера доп. параметры в урл не добаляются
        offers = self.report.request_images(
            'place=images&src_pof=712&offerid=AAAAAAAA1AAAAAAAAAAAAA&rids=213&pp=18&x-yandex-encrypted-icookie=1123456&rearr-factors={}'.format(
                ';'.join(rearr_factors)
            )
        )
        url = offers[0].Url
        self.assertNotIn('src_pof', url)
        self.assertNotIn('icookie', url)

    def test_cpa_offers_cashback(self):
        """
        https://st.yandex-team.ru/MARKETOUT-39261
        Проверяем, что под флагом market_images_cpa_offers=1
        репорт пытается заменить исходный оффер на cpa,
        если такой существует
        """
        offer_ids = [
            '353430302_pqjgdd7HfoHQ',
        ]
        offers = self.report.request_images(
            'place=images&offerid={}&rearr-factors=market_images_cpa_offers_cashback=1&rids=213&pp=18'.format(
                ','.join(offer_ids)
            )
        )
        self.assertEqual(offers[0].Cashback, '15')

    def test_cpa_offers_url_type(self):
        """Проверяем, что под флагом market_images_cpa_offers_url_type=External ссылки CPA офферов ведут в магазин
        https://st.yandex-team.ru/MARKETOUT-39798
        """
        offer_ids = ['353430302_pqjgdd7HfoHQ']
        request = 'place=images&offerid={}&rids=213&pp=18'.format(','.join(offer_ids))

        # Без флага ссылка ведет на Покупки
        offers = self.report.request_images(request)
        self.assertEqual(offers[0].WareMd5, '353430302_pqjgdd7HfoHQ')
        self.assertIn(
            '//pokupki.market.yandex.ru/product/35343003?clid=2322165&lr=213&offerid=353430302_pqjgdd7HfoHQ',
            offers[0].Url,
        )

        # Под флагом market_images_cpa_offers_url_type=External ссылка ведет в магазин
        offers = self.report.request_images(request + '&rearr-factors=market_images_cpa_offers_url_type=External')
        self.assertEqual(offers[0].WareMd5, '353430302_pqjgdd7HfoHQ')
        self.assertIn('//market-click2.yandex.ru/redir/dtype=market/', offers[0].Url)

    @classmethod
    def prepare_cpa_offers_by_sku(cls):
        cls.index.mskus += [
            MarketSku(
                hyperid=3982101,
                sku=39821011,
                blue_offers=[
                    BlueOffer(
                        title='Blue offer H1',
                        bid=1000,
                        vbid=1000,
                        price=1000,
                        feedid=12345,
                        cpa=Offer.CPA_REAL,
                        waremd5="398210101_pqjgdd7HfoHQ",
                    ),
                    BlueOffer(
                        title='Blue offer H2',
                        bid=100,
                        vbid=100,
                        price=100,
                        feedid=12345,
                        cpa=Offer.CPA_REAL,
                        waremd5="398210102_pqjgdd7HfoHQ",
                    ),
                ],
            ),
            MarketSku(
                hyperid=3982101,
                sku=39821012,
                blue_offers=[
                    BlueOffer(
                        title='Blue offer H3',
                        bid=10,
                        vbid=10,
                        price=10,
                        feedid=12345,
                        cpa=Offer.CPA_REAL,
                        waremd5="398210201_pqjgdd7HfoHQ",
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=3982110, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                hyperid=3982103,
                price=10000,
                bid=202,
                waremd5='398210301_pqjgdd7HfoHQ',
                cpa=Offer.CPA_REAL,
                fesh=3982110,
            ),
            Offer(
                hyperid=3982104,
                price=10000,
                bid=200,
                waremd5='398210401_pqjgdd7HfoHQ',
                cpa=Offer.CPA_REAL,
                has_url=False,
                fesh=3982110,
            ),
        ]

    def test_cpa_offers_by_sku(self):
        rearr_factors = [
            'market_images_cpa_offers_by_sku=1',
            'market_images_allow_dsbs_offers=1',
            'market_images_cpa_offers_url_type=PokupkiIncut',
        ]
        offers = [
            '398210101_pqjgdd7HfoHQ',
            '398210301_pqjgdd7HfoHQ',
            '398210401_pqjgdd7HfoHQ',
        ]
        market_skus = ['39821011', '39821012']
        req = 'place=images&offerid={}&market-sku={}&rearr-factors={}&rids=213&pp=18'.format(
            ','.join(offers), ','.join(market_skus), ';'.join(rearr_factors)
        )
        offers = self.report.request_images(req)
        self.assertEqual(4, len(offers))
        self.assertEqual(offers[0].WareMd5, '398210301_pqjgdd7HfoHQ')
        self.assertEqual(offers[2].WareMd5, '398210201_pqjgdd7HfoHQ')
        self.assertEqual(offers[3].WareMd5, '398210102_pqjgdd7HfoHQ')
        self.assertIn('//market-click2.yandex.ru/redir/dtype=offercard/', offers[0].Url)

        offers = self.report.request_images(req + '&device=touch')
        self.assertEqual(offers[0].WareMd5, '398210301_pqjgdd7HfoHQ')
        self.assertEqual(offers[0].ShopUrl, 'm.market.yandex.ru/offer/398210301_pqjgdd7HfoHQ')
        self.assertEqual(offers[0].ShopDomain, 'm.market.yandex.ru')
        self.assertIn('//market-click2.yandex.ru/redir/dtype=offercard/', offers[0].Url)
        self.show_log.expect(url_type=UrlType.OFFERCARD).times(4)


if __name__ == '__main__':
    main()

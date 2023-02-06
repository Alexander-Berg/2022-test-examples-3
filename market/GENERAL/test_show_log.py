#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types.autogen import Const
from core.types import (
    BidSettings,
    CategoryBidSettings,
    ClickType,
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Picture,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
    UrlType,
    VCluster,
)
from core.types.picture import thumbnails_config
from core.testcase import TestCase, main
from core.matcher import LikeUrl, NotEmpty, EqualToOneOf, Contains, LessEq
from core.cpc import Cpc
from core.click_context import ClickContext
from core.report import ReportSearchType

import os

offer_ware_ids = [
    'XKkwTsf0kvemSnwPPl054A',
    'XkWzl8wUboFRL_9-4Wa-pg',
    'Wn59W8WdR42xd0SK8trU7w',
    'XDuyP2NDoU05WZ6IvCbvTg',
    'oYSj3UzunZMv86UI8JZCdA',
    'JpCaNeZTH061HSziA-AO1A',
    'c7kYbB5e0ataMlSj9qhS_w',
    'bVGXQMYClL_cgadUijhEmg',
    'vK5vvkEgsp4TwT5y4wCsQg',
    'lJ6RKOrkpvarYA06_0Zmdg',
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.cloud_service = 'test_report_lite'

        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        cls.index.fixed_index_generation = '20200101_0300'

        cls.index.shops += [
            Shop(fesh=1, home_region=Const.ROOT_COUNTRY),
        ]
        cls.index.vclusters += [
            VCluster(hid=1, vclusterid=1000000001),
        ]
        cls.index.offers = [
            Offer(title='iphone'),
            Offer(vclusterid=1000000001, title='Платье'),
            Offer(title='apple Phone phones Apples'),
            Offer(title='теЛефОн тЕлеФонЫ телЕфоНов телефоном'),
        ]

        # show-uid uniqueness
        cls.index.offers += [
            Offer(hyperid=10),
            Offer(vclusterid=1000000020),
        ]

        # recommendations
        cls.index.offers += [
            Offer(hyperid=30, hid=200, cpa=Offer.CPA_REAL, fesh=100, price=100),
            Offer(hyperid=40, hid=200, cpa=Offer.CPA_REAL, fesh=100, price=10),
            Offer(hyperid=50, hid=200, cpa=Offer.CPA_REAL, fesh=100, price=10),
        ]

        cls.index.models += [
            Model(hyperid=30, hid=200, accessories=[40, 50]),
            Model(hyperid=40, hid=200, accessories=[40, 50]),
        ]

        # ukraine url
        # 187 -- hardcoded wellknown region id for Ukraine
        cls.index.regiontree += [Region(rid=187, region_type=Region.COUNTRY, children=[Region(rid=2)])]

        cls.index.models += [
            Model(hyperid=31, title='ukraine'),
        ]

        cls.index.offers += [
            Offer(visual=True, title='ukraine', fesh=2),
        ]

        cls.index.shops += [
            Shop(fesh=2, regions=[2]),
        ]

        # CPC test
        cls.index.category_bid_settings += [
            CategoryBidSettings(
                category=777,
                search_settings=BidSettings(coefficient=0.245, power=0.56, maximumBid=90),
            ),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=887, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.shops += [
            Shop(fesh=601, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=602, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                hyperid=501,
                fesh=601,
                price=500,
                bid=80,
                cpa=Offer.CPA_REAL,
                waremd5='RcSMzi4tf73qGvxRx8atJg',
                offerid='500',
                title='bid=80, fee=800 offer',
            ),
            Offer(
                hyperid=502,
                fesh=601,
                price=40000,
                bid=12,
                cpa=Offer.CPA_REAL,
                waremd5='ZRK9Q9nKpuAsmQsKgmUtyg',
                title='bid=12, fee=200 offer',
                hid=777,
            ),
            Offer(
                hyperid=503,
                fesh=602,
                price=500,
                bid=70,
                cpa=Offer.CPA_REAL,
                hid=887,
                waremd5='trsNdTQmbxiUncMbd6lyjA',
                title='bid=70, fee=500 offer',
            ),
            Offer(
                hyperid=504,
                hid=111,
                fesh=602,
                price=10,
                bid=70,
                cpa=Offer.CPA_NO,
                waremd5='NotCpaOfferForCPCTestg',
                title='Not Cpa Offer',
            ),
        ]
        # CPC-cpa test
        cls.index.hypertree += [HyperCategory(hid=888, output_type=HyperCategoryType.GURU)]

        cls.index.cpa_categories += [
            CpaCategory(hid=888, regions=[225], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.shops += [
            Shop(fesh=133, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=134, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=135, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                fesh=133,
                title="500 fee",
                hid=888,
                picture_flags=1,
                cpa=Offer.CPA_REAL,
                bid=50,
                waremd5='xMpCOKC5I4INzFCab3WEmQ',
            ),
            Offer(
                fesh=134,
                title="400 fee",
                hid=888,
                picture_flags=1,
                cpa=Offer.CPA_REAL,
                bid=60,
                waremd5='EWyLmZVj2_dabDcT9KJ77g',
            ),
            Offer(
                fesh=135,
                title="310 fee",
                hid=888,
                picture_flags=1,
                cpa=Offer.CPA_REAL,
                bid=70,
                waremd5='JqsNuQ1y4orQuh4i7lEFEA',
            ),
        ]

        # wide show log testing
        cls.index.offers += [
            Offer(
                title='samsung',
                bid=20,
                fesh=100,
                cpa=Offer.CPA_REAL,
                hyperid=300,
                price=100,
                discount=50,
                hid=3,
                glparams=[GLParam(param_id=2, value=3)],
                cmagic='c20ad4d76fe97759aa27a0c99bff6710',
                waremd5='09lEaAKkQll1XTjm0WPoIA',
                vendor_id=12,
                datasource_id=701,
            ),
            Offer(title='sony 1', randx=2, fesh=200, waremd5='wgrU12_pd1mqJ6DJm_9nEA'),
            Offer(title='sony 2', randx=1, fesh=200),
            Offer(title='nike', fesh=300, waremd5='m_Mcf_Bik2qW08i9H48v8w'),
            Offer(title='radio', fesh=100, feedid=201),
            Offer(
                title='PriceFrom',
                bid=20,
                fesh=100,
                cpa=Offer.CPA_REAL,
                hyperid=300,
                discount=50,
                hid=3,
                glparams=[GLParam(param_id=2, value=3)],
                cmagic='3bcdefabcde97759aa27a0c99bff6710',
                waremd5='PriceFrom_l1XTjm0WPoIA',
                vendor_id=12,
                datasource_id=701,
                feedid=201,
                pricefrom=True,
            ),
            Offer(
                title='InvalidDiscount',
                bid=20,
                fesh=100,
                cpa=Offer.CPA_REAL,
                hyperid=300,
                price=100,
                price_history=100,
                discount=50,
                hid=3,
                glparams=[GLParam(param_id=2, value=3)],
                cmagic='ffffd4d76fe97759aa27a0c99bff6710',
                waremd5='FFFFaAKkQll1XTjm0WPoIA',
                vendor_id=12,
                datasource_id=701,
            ),
        ]

        cls.index.offers += [
            Offer(title='ForRotation', waremd5='FFFEaAKkQll1XTjm0WPoIA', for_rotation=True),
            Offer(
                title='NotForRotation',
                waremd5='FFFDaAKkQll1XTjm0WPoIA',
            ),
        ]

        cls.index.models += [
            Model(hyperid=300, hid=3),
            Model(title='topor', hyperid=13, hid=17, vendor_id=37),
            Model(title='radio megamodel'),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=13, bid1=7, offers=1, max_discount=20),
            RegionalModel(hyperid=1000000005, bid1=8, offers=1),
        ]

        cls.index.offers += [Offer(hyperid=13, discount=20, bid=7)]

        cls.index.vclusters += [
            VCluster(vclusterid=1000000005, title='topor visual', hid=18),
            VCluster(title='radio megacluster', vclusterid=1000000007),
        ]

        cls.index.shops += [
            Shop(cpa=Shop.CPA_REAL, fesh=100, priority_region=213, home_region=226, name='apple.com'),
            Shop(fesh=200, pickup_buckets=[5002]),
            Shop(fesh=300, pickup_buckets=[5003]),
            Shop(fesh=400, pickup_buckets=[5004]),
            Shop(fesh=1044, priority_region=213),
        ]

        cls.index.outlets += [
            Outlet(point_id=1, fesh=200, region=213),
            Outlet(point_id=2, fesh=200, region=213),
            Outlet(point_id=3, fesh=300, region=213),
            Outlet(point_id=4, fesh=400, region=213),
            Outlet(point_id=5, fesh=200, region=64),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5002,
                fesh=200,
                carriers=[99],
                options=[PickupOption(outlet_id=1), PickupOption(outlet_id=2), PickupOption(outlet_id=5)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=300,
                carriers=[99],
                options=[PickupOption(outlet_id=3)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=400,
                carriers=[99],
                options=[PickupOption(outlet_id=4)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(tovalid=2, hid=3),
            HyperCategory(tovalid=21, hid=17),
            HyperCategory(tovalid=22, hid=18, visual=True),
        ]

        cls.index.navtree += [NavCategory(nid=4, hid=3), NavCategory(nid=10, hid=17), NavCategory(nid=11, hid=18)]

        cls.index.models += [Model(title='molot-3000', hyperid=500, hid=3)]

        pic = Picture(width=100, height=100, thumb_mask=thumbnails_config.get_mask_by_names(['100x100']), group_id=1234)
        for ts in range(1, 10):
            cls.index.shops.append(
                Shop(fesh=400 + ts, priority_region=213, home_region=226, name='molotok.com', cpa=Shop.CPA_REAL),
            )
            cls.index.offers.append(
                Offer(
                    url='molotok.com/molotok',
                    title='molotok',
                    ts=ts,
                    picture=pic,
                    picture_flags=ts,
                    bid=10,
                    fesh=400 + ts,
                    cpa=Offer.CPA_REAL,
                    hyperid=500,
                    discount=50,
                    hid=3,
                    cmagic='c20ad4d76fe97759aa27a0c99bff6710',
                    waremd5=offer_ware_ids[ts - 1],
                    vendor_id=12,
                )
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.5)

        cls.index.offers += [
            Offer(vclusterid=1000000005, fesh=1044),
            Offer(vclusterid=1000000007, fesh=1044),
        ]

        # delivery types test
        cls.index.regiontree += [
            Region(rid=5, region_type=Region.COUNTRY),
        ]

        cls.index.shops += [
            Shop(fesh=3, priority_region=2, regions=[187], home_region=187, pickup_buckets=[6001]),
        ]

        cls.index.offers += [
            Offer(title='delivery_courier', waremd5='VZJptjZEjnC7k00uGbFa-A', fesh=3, hyperid=4),
            Offer(title='delivery_downloadable', waremd5='Ym--g_jH8mcLkKdztzvxfA', fesh=3, hyperid=5, download=True),
        ]

        cls.index.outlets += [
            Outlet(point_id=38, fesh=3, region=187),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=6001,
                fesh=3,
                carriers=[99],
                options=[PickupOption(outlet_id=38)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        # for a promo code support
        cls.index.shops += [
            Shop(fesh=10001, regions=[187], cpa=Shop.CPA_REAL, subsidies=Shop.SUBSIDIES_ON),
        ]
        cls.index.offers += [Offer(title='мяучий оффер', fesh=10001, cpa=Offer.CPA_REAL)]
        cls.index.offers += [Offer(title='не мяучий оффер', fesh=10001, cpa=Offer.CPA_NO)]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.1)

        cls.index.offers += [
            Offer(
                title="схлопни-меня",
                hyperid=38389,
                url='http://hlop-shop.ru/offer/skhlopni-menya',
                waremd5='UEzMt5qbMxpDW6a2Ts6_3g',
            )
        ]

        # for prev_pp log test
        cls.index.models += [Model(title='jah ith ber')]
        cls.index.offers += [Offer(title="dol um ber ist", url='http://rw-shop.aq/offers/CoH/')]

        # test meta param ts,original ts logging
        cls.index.offers += [
            Offer(
                title='совсем другой оффер 1',
                hyperid=3303,
                hid=200,
                url='http://hlop-shop1.ru/offer/skhlopni-menya',
                ts=13596,
            ),
            Offer(
                title='совсем другой оффер 2',
                hyperid=4303,
                hid=200,
                url='http://hlop-shop2.ru/offer/skhlopni-menya',
                ts=124,
            ),
            Offer(title='совсем другой оффер 3', url='http://hlop-shop3.ru/offer/skhlopni-menya', ts=125),
        ]

        cls.index.models += [
            Model(hyperid=3303, hid=200, ts=1233),
            Model(hyperid=4303, hid=200, ts=1234),
            Model(title="совсем другой оффер 4", hyperid=5303, hid=200, ts=126),
        ]

    def test_drop_shows_for_pp(self):
        self.report.request_json(
            'place=prime&text=iphone&show-urls=external&rearr-factors=market_drop_shows_for_pp=1,2&pp=2'
        )
        self.click_log.expect(ClickType.EXTERNAL)
        self.show_log.expect(url_type=ClickType.EXTERNAL).never()

    def test_num_columns(self):
        """Тест-ограничение на размер записи лога. Правила добавления колонок в лог:
        https://wiki.yandex-team.ru/market/procedures/pakt-o-degradacii/#praviladobavlenijanovyxlogovidobavlenienovyxpolejjvtekushhielogi
        """
        self.report.request_json('place=prime&text=iphone&show-urls=external')
        self.show_log.expect_num_columns(num_columns=LessEq(124)).all()

    def test_meta_ts(self):
        _ = self.report.request_json('place=prime&text=совсем+другой+оффер&debug=da&allow-collapsing=1')
        self.show_log.expect(
            hyper_id=3303,
            original_url='http://hlop-shop1.ru/offer/skhlopni-menya',
            original_title="совсем другой оффер 1",
            ts_out_document=1233,
            ts_orig_document=13596,
        )
        self.show_log.expect(
            hyper_id=4303,
            original_url='http://hlop-shop2.ru/offer/skhlopni-menya',
            original_title="совсем другой оффер 2",
            ts_out_document=1234,
            ts_orig_document=124,
        )
        self.show_log.expect(ts_out_document=125, ts_orig_document=125, title="совсем другой оффер 3")
        self.show_log.expect(ts_out_document=126, ts_orig_document=126, title="совсем другой оффер 4")

    def test_original_url_title(self):
        response = self.report.request_json('place=prime&text=схлопни+меня&debug=da&allow-collapsing=1')
        self.assertFragmentIn(
            response, {'entity': 'product', 'debug': {'isCollapsed': True, 'wareId': 'UEzMt5qbMxpDW6a2Ts6_3g'}}
        )
        self.show_log.expect(
            hyper_id=38389,
            original_url='http://hlop-shop.ru/offer/skhlopni-menya',
            original_ware_md5='UEzMt5qbMxpDW6a2Ts6_3g',
            original_title="схлопни-меня",
        )

    def test_normalized(self):
        self.report.request_json('place=prime&text=apple+Phone+phones+Apples&show-urls=external')
        self.report.request_json('place=prime&text=теЛефОн+тЕлеФонЫ+телЕфоНов+телефоном&show-urls=external')

        # There should be two records in logs for each normalization: one for
        # prime, on for parallel
        map(
            lambda o: o.once(),
            [
                self.show_log.expect(normalized_to_lower_query='apple phone phones apples'),
                self.show_log.expect(normalized_to_lower_and_sorted_query='apple apples phone phones'),
                self.show_log.expect(normalized_by_dnorm_query='apple apple phone phone'),
                # Can't multiplex here because of encoding
                self.show_log_tskv.expect(normalized_to_lower_query='телефон телефоны телефонов телефоном'),
                self.show_log_tskv.expect(normalized_to_lower_and_sorted_query='телефон телефонов телефоном телефоны'),
                self.show_log_tskv.expect(normalized_by_dnorm_query='телефон телефон телефон телефон'),
            ],
        )

    def test_query(self):
        self.report.request_json('place=prime&text=iphone&show-urls=external')
        self.show_log.expect(
            query_context='iphone',
            original_query='iphone',
            cloud_service='test_report_lite',
            search_type=ReportSearchType.META_ONLY,
        )

    def test_redundant_showuid_in_defaultoffer(self):
        self.report.request_xml('place=defaultoffer&hyperid=10&hyperid=1000000020&pp=200&show-urls=external')
        self.show_log.expect(show_uid="04884192001117778888800001").once()
        self.show_log.expect(show_uid="04884192001117778888800002").once()

    def test_home_region(self):
        self.report.request_json('place=prime&text=Платье&show-urls=external')
        self.show_log.expect(home_region=str(Const.ROOT_COUNTRY))

    def test_ukranian_model_urls_prime(self):
        self.report.request_json('place=prime&rids=2&text=ukraine')
        self.show_log.expect(url=LikeUrl(url_host='market.yandex.ua'))

    def test_source_base_field(self):
        self.report.request_json('place=productoffers&hyperid=10&client=sovetnik&source_base=avito.ru')
        self.show_log.expect(source_base='avito.ru')

    def test_cpc_base_offer_id_matches_in_prime(self):
        # offer_id and shop_id matches
        # for RcSMzi4tf73qGvxRx8atJg: bid=80
        cpc = Cpc.create_for_offer(
            click_price=71,
            offer_id='RcSMzi4tf73qGvxRx8atJg',
            bid=80,
            shop_id=601,
            shop_fee=800,
            fee=900,
            minimal_fee=100,
        )
        self.report.request_json('place=prime&hyperid=501&fesh=601&show-urls=external,cpa&cpc={}'.format(cpc))

        self.click_log.expect(ClickType.EXTERNAL, cp=71, cb=80).times(1)
        self.show_log.expect(url_type=ClickType.EXTERNAL, click_price=71, bid=80).times(1)

    def test_cpc_bid_type_matches(self):
        """
        tests that bid_type from client is written to logs
        """
        for bid_type in ["ybid", "minbid", "mbid"]:
            cpc = Cpc.create_for_offer(
                click_price=71, offer_id='RcSMzi4tf73qGvxRx8atJg', bid=80, shop_id=601, bid_type=bid_type
            )
            for place in ['prime']:
                self.report.request_json('place={}&hyperid=501&fesh=601&cpc={}&show-urls=external'.format(place, cpc))

            self.click_log.expect(ClickType.EXTERNAL, cp=71, cb=80, bid_type=bid_type).times(1)

    # TODO, propbably bug - passing fesh to mainreport leads to 10c bids ignoring CPC for offers with another waremd5
    # Now it's done by design, but really looks strange: this code is used for links from "Shop Reviews"
    def test_cpc_base_shop_id_matches_with_shop_prime(self):
        # offer_id doesn't match and shop_id matches
        # for RcSMzi4tf73qGvxRx8atJg: bid=80, fee=800
        # shown offer ZRK9Q9nKpuAsmQsKgmUtyg: bid=12
        cpc = Cpc.create_for_offer(click_price=31, offer_id='RcSMzi4tf73qGvxRx8atJg', bid=80, shop_id=601)
        response = self.report.request_json(
            'place=prime&hyperid=502&fesh=601&cpc={}&show-urls=external,cpa'.format(cpc)
        )
        self.assertFragmentIn(response, {"titles": {"raw": "bid=12, fee=200 offer"}})

        self.click_log.expect(ClickType.EXTERNAL, cp=51, cb=51, min_bid=51)
        self.show_log.expect(url_type=ClickType.EXTERNAL, click_price=51, bid=51, min_bid=51).times(1)

    def test_cpc_base_shop_id_not_matches_with_shop_productoffers(self):
        # offer_id doesn't match and shop_id doesn't matches
        # shown offer trsNdTQmbxiUncMbd6lyjA: bid=70
        # shown offer for shop 602, but cpc for shop 601
        # click price and comission is according to auction and in this case - minimal bids/fee
        cpc = Cpc.create_for_offer(click_price=71, offer_id='RcSMzi4tf73qGvxRx8atJg', bid=80, shop_id=601)
        response = self.report.request_json(
            'place=productoffers&hyperid=503&cpc={}&show-urls=external,cpa'
            '&hid=887&rids=213&treat-as-cpc-pessimization-category=1'.format(cpc)
        )
        self.assertFragmentIn(response, {"titles": {"raw": "bid=70, fee=500 offer"}})
        self.click_log.expect(ClickType.EXTERNAL, cp=1, cb=70)

    def test_cpc_base_shop_id_matches_wo_shop(self):
        # offer_id doesn't match and shop_id matches
        for place in ['prime']:
            cpc = Cpc.create_for_offer(click_price=31, offer_id='RcSMzi4tf73qGvxRx8atJg', bid=50, shop_id=601)
            response = self.report.request_json('place={}&hyperid=502&show-urls=external&cpc={}'.format(place, cpc))
            self.assertFragmentIn(response, {"titles": {"raw": "bid=12, fee=200 offer"}})
        # prime
        self.click_log.expect(cp=51, cb=51, min_bid=51)
        self.show_log.expect(click_price=51, bid=51, min_bid=51)

    def helper_cpc_incorrect_cpc_for_place(self, place, expect_cp, bid):
        # pass invalid cpc parameter: cp equals to shop bid, fuid is rised to minbid
        response = self.report.request_json('place={}&hyperid=502&cpc=false&show-urls=external'.format(place))
        self.assertFragmentIn(response, {"titles": {"raw": "bid=12, fee=200 offer"}})
        self.click_log.expect(cp=expect_cp, cb=bid)
        self.show_log.expect(click_price=expect_cp, bid=bid)

    def test_cpc_incorrect_cpc_for_prime(self):
        self.helper_cpc_incorrect_cpc_for_place('prime', 51, 51)

    def test_cpc_minbid_experiment_pass(self):
        for place in ['prime']:
            cpc = Cpc.create_for_offer(click_price=59, offer_id='ZRK9Q9nKpuAsmQsKgmUtyg', bid=59, shop_id=601)
            response = self.report.request_json(
                'place={}&hyperid=502&cpc={}&show-urls=external&debug=1'.format(place, cpc)
            )
            self.assertFragmentIn(
                response,
                {
                    "sessionContext": Contains(
                        'generation_time: 488419200', 'offer_id: \"ZRK9Q9nKpuAsmQsKgmUtyg\"', 'click_price: 59'
                    )
                },
            )
        expect_cp = 59
        # bid=12, but cpc has click cp=59
        # offer ID matches the one specified in CPC -> current shop BID value is not applied as a maximum
        self.click_log.expect(cp=expect_cp, cb=expect_cp).times(1)
        self.show_log.expect(click_price=expect_cp, bid=expect_cp).times(1)

    # TODO: remove with CPA, with EBrokeredField and so on
    # def test_cpc_cpa(self):
    #     result = [
    #         ("xMpCOKC5I4INzFCab3WEmQ", str(Cpc.create_for_offer(
    #             offer_id="xMpCOKC5I4INzFCab3WEmQ",
    #             hid=888,
    #             click_price=1,
    #             click_price_before_bid_correction=1,
    #             bid=50,
    #             shop_id=133,
    #             minimal_bid=1,
    #             bid_type="mbid"))),
    #         ("EWyLmZVj2_dabDcT9KJ77g", str(Cpc.create_for_offer(
    #             offer_id="EWyLmZVj2_dabDcT9KJ77g",
    #             hid=888,
    #             click_price=1,
    #             click_price_before_bid_correction=1,
    #             bid=60,
    #             shop_id=134,
    #             minimal_bid=1,
    #             bid_type="mbid"))),
    #         ("JqsNuQ1y4orQuh4i7lEFEA", str(Cpc.create_for_offer(
    #             offer_id="JqsNuQ1y4orQuh4i7lEFEA",
    #             hid=888,
    #             click_price=1,
    #             click_price_before_bid_correction=1,
    #             bid=70,
    #             shop_id=135,
    #             minimal_bid=1,
    #             bid_type="mbid")))
    #     ]

    #     response = self.report.request_json(
    #         'place=prime&hid=888&rids=213'
    #         '&rearr-factors=market_guru_collapsing_reverse=1&show-urls=cpa'
    #         '')

    #     for ware_md5, cpc in result:
    #         self.assertFragmentIn(response, {
    #             "wareId": ware_md5,
    #             "cpc": cpc
    #         })

    #     for ware_md5, cpc in result:
    #         for place in ["prime" , "offerinfo"]:
    #             response = self.report.request_json(
    #                 'place={0}&offerid={1}&cpc={2}'
    #                 '&rids=213&regset=1&show-urls=cpa'.format(place, ware_md5, cpc)
    #             )

    #     self.show_log.expect(ware_md5="xMpCOKC5I4INzFCab3WEmQ").times(3)
    #     self.show_log.expect(ware_md5="EWyLmZVj2_dabDcT9KJ77g").times(3)
    #     self.show_log.expect(ware_md5="JqsNuQ1y4orQuh4i7lEFEA").times(3)

    #     self.click_log.expect(ClickType.CPA, ware_md5="xMpCOKC5I4INzFCab3WEmQ").times(3)
    #     self.click_log.expect(ClickType.CPA, ware_md5="EWyLmZVj2_dabDcT9KJ77g").times(3)
    #     self.click_log.expect(ClickType.CPA, ware_md5="JqsNuQ1y4orQuh4i7lEFEA").times(3)

    def test_prime_geo_urls(self):
        self.report.request_json('place=prime&text=nike&show-urls=shop,outlet,point-info,geo&geo=1')
        self.show_log.expect(
            url_type=UrlType.GEO_OUTLET,
            url=LikeUrl.of('//market.yandex.ru/geo?fesh=300&offerid=m_Mcf_Bik2qW08i9H48v8w&point_id=3'),
        )

        self.show_log.expect(
            url_type=UrlType.GEO_OUTLET_INFO,
            url=LikeUrl.of('//market.yandex.ru/gate/maps/getpointinfo.xml?offerid=m_Mcf_Bik2qW08i9H48v8w&point_id=3'),
        )

    def test_prime_geo_outlets_urls(self):
        self.report.request_json('place=prime&text=sony 1&show-urls=geo&geo=1')
        self.show_log.expect(
            url_type=UrlType.GEO, url=LikeUrl.of('//market.yandex.ru/geo?fesh=200&offerid=wgrU12_pd1mqJ6DJm_9nEA')
        )

    def test_geo_link_uid(self):
        self.report.request_json('place=geo&text=sony&geo=1&show-urls=external')

        # link-uid depends on click-url logging in code and can change
        self.show_log.expect(show_uid="04884192001117778888800001", super_uid="04884192001117778888800001")
        self.show_log.expect(show_uid="04884192001117778888800002", super_uid="04884192001117778888800001")

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, uid="04884192001117778888800001", link_id="04884192001117778888800001"
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, uid="04884192001117778888800002", link_id="04884192001117778888800001"
        )

    def test_parallel_show_log_plitka(self):
        self.report.request_bs(
            'place=parallel&text=molotok&ignore-mn=1&pp=1&pof=1&wprid=1'
            '&stat-block-id=2000000000020160101000000&reqid=abc123&rids=213&yandexuid=1&ip=127.0.0.1&test-buckets=1,0,0&rearr-factors=market_offers_wizard_incut_url_type=External'
        )
        for i in range(1, 10):
            self.show_log.expect(
                autobroker_enabled=1,
                best_deal=1,
                bid=10,
                bs_block_id=2000000000020160101000000,
                category_id=2,
                click_price=1 if i == 9 else 10,
                click_type_id=0,
                cooked_query='molotok',
                cpa=1,
                ctr=20000000,
                discount_ratio=0.5,
                discount=1,
                event_time=488419200,
                geo_id=213,
                home_region=226,
                hyper_id=500,
                ip='127.0.0.1',
                is_price_from=0,
                url='http://molotok.com/molotok',
                nid=4,
                oldprice=200,
                original_query='molotok',
                onstock=1,
                pof=1,
                position=i,
                pp=405,
                price=100,
                query_context='molotok',
                reqid='abc123',
                shop_category='molotok.com:Unknown',
                shop_name='molotok.com',
                shop_id=NotEmpty(),
                show_block_id="048841920011177788888",
                show_uid="0488419200111777888880000" + str(i),
                super_uid="0488419200111777888880000" + str(i),
                test_buckets='1,0,0',
                title='molotok',
                touch=0,
                url_type=UrlType.EXTERNAL,
                vbid=0,
                vendor_click_price=0,
                wprid=1,
                yandex_uid='1',
                record_type=0,
                vendor_id=12,
                hyper_cat_id=3,
            )
            self.click_log.expect(
                clicktype=ClickType.EXTERNAL,
                bd=1,
                bs_block_id=2000000000020160101000000,
                categid=2,
                cb=10,
                cb_vnd=0,
                cp=1 if i == 9 else 10,
                cp_vnd=0,
                cpa=1,
                data_ts=488419200,
                data_url='http%3A%2F%2Fmolotok.com%2Fmolotok',
                dtype='market',
                geo_id=213,
                hyper_cat_id=3,
                hyper_id=500,
                is_price_from=0,
                link_id="0488419200111777888880000" + str(i),
                nav_cat_id=4,
                onstock=1,
                pof=1,
                position=i,
                pp=405,
                price=100,
                reqid='abc123',
                shop_id=NotEmpty(),
                show_block_id="048841920011177788888",
                show_time=488419200,
                touch=0,
                type_id=0,
                uid="0488419200111777888880000" + str(i),
                url_type=UrlType.EXTERNAL,
                vnd_id=12,
                yandexuid=1,
            )

    def test_parallel_show_log_plitka_touch(self):
        _ = self.report.request_bs(
            'place=parallel&text=molotok&ignore-mn=1&pp=1&pof=1&wprid=1&touch=1'
            '&stat-block-id=2000000000020160101000000&reqid=abc123&rids=213&yandexuid=1&ip=127.0.0.1&test-buckets=1,0,0'
            '&rearr-factors=offers_touch=1;market_offers_wizard_incut_url_type=External'
        )
        for i in range(1, 10):
            self.show_log.expect(
                autobroker_enabled=1,
                best_deal=1,
                bid=10,
                bs_block_id=2000000000020160101000000,
                category_id=2,
                click_price=1 if i == 9 else 10,
                click_type_id=0,
                cooked_query='molotok',
                cpa=1,
                ctr=20000000,
                discount_ratio=0.5,
                discount=1,
                event_time=488419200,
                geo_id=213,
                home_region=226,
                hyper_id=500,
                ip='127.0.0.1',
                is_price_from=0,
                url='http://molotok.com/molotok',
                nid=4,
                oldprice=200,
                original_query='molotok',
                onstock=1,
                pof=1,
                position=i,
                pp=403,
                price=100,
                query_context='molotok',
                reqid='abc123',
                shop_category='molotok.com:Unknown',
                shop_name='molotok.com',
                shop_id=NotEmpty(),
                show_block_id="048841920011177788888",
                show_uid="0488419200111777888880000" + str(i),
                super_uid="0488419200111777888880000" + str(i),
                test_buckets='1,0,0',
                title='molotok',
                touch=1,
                url_type=UrlType.EXTERNAL,
                vbid=0,
                vendor_click_price=0,
                wprid=1,
                yandex_uid='1',
                record_type=0,
                vendor_id=12,
                hyper_cat_id=3,
            )
            self.click_log.expect(
                clicktype=ClickType.EXTERNAL,
                bd=1,
                bs_block_id=2000000000020160101000000,
                categid=2,
                cb=10,
                cb_vnd=0,
                cp=1 if i == 9 else 10,
                cp_vnd=0,
                cpa=1,
                data_ts=488419200,
                data_url='http%3A%2F%2Fmolotok.com%2Fmolotok',
                dtype='market',
                geo_id=213,
                hyper_cat_id=3,
                hyper_id=500,
                is_price_from=0,
                link_id="0488419200111777888880000" + str(i),
                nav_cat_id=4,
                onstock=1,
                pof=1,
                position=i,
                pp=403,
                price=100,
                reqid='abc123',
                shop_id=NotEmpty(),
                show_block_id="048841920011177788888",
                show_time=488419200,
                touch=1,
                type_id=0,
                uid="0488419200111777888880000" + str(i),
                url_type=UrlType.EXTERNAL,
                vnd_id=12,
                yandexuid=1,
            )

    def test_parallel_show_log_model_incut(self):
        self.report.request_bs(
            'place=parallel&text=molot+3000&ignore-mn=1&pp=1&pof=1&wprid=1&user_type=1&reqid=123456789abcdef'
            '&stat-block-id=2000000000020160101000000&rids=213&yandexuid=1&ip=127.0.0.1&test-buckets=1,0,0'
        )
        for i in range(1, 6):
            self.show_log.expect(
                autobroker_enabled=1,
                best_deal=1,
                bid=10,
                bs_block_id=2000000000820160101000000,
                category_id=2,
                click_price=10,
                click_type_id=0,
                cpa=1,
                ctr=20000000,
                discount_ratio=0.5,
                discount=1,
                event_time=488419200,
                geo_id=213,
                home_region=226,
                hyper_id=500,
                ip='127.0.0.1',
                is_price_from=0,
                url='http://molotok.com/molotok',
                nid=4,
                oldprice=200,
                onstock=1,
                position=i,
                pp=404,
                price=100,
                query_context='b956d5f0d5c4dopaliha666 3',
                reqid='123456789abcdef',
                shop_category='molotok.com:Unknown',
                shop_id=NotEmpty(),
                shop_name='molotok.com',
                show_block_id="048841920011177788888",
                show_uid="0488419200111777888880000" + str(i),
                super_uid="0488419200111777888880000" + str(i),
                test_buckets='1,0,0',
                title='molotok',
                touch=0,
                url_type=UrlType.EXTERNAL,
                vbid=0,
                vendor_click_price=0,
                wprid=1,
                ware_md5=EqualToOneOf(*offer_ware_ids),
                yandex_uid='1',
                record_type=0,
                hyper_cat_id=3,
                vendor_id=12,
            )
            self.click_log.expect(
                clicktype=ClickType.EXTERNAL,
                bd=1,
                bs_block_id=2000000000820160101000000,
                categid=2,
                cb=10,
                cb_vnd=0,
                cp=10,
                cp_vnd=0,
                cpa=1,
                data_ts=488419200,
                data_url='http%3A%2F%2Fmolotok.com%2Fmolotok',
                dtype='market',
                geo_id=213,
                hyper_cat_id=3,
                hyper_id=500,
                is_price_from=0,
                link_id="0488419200111777888880000" + str(i),
                nav_cat_id=4,
                onstock=1,
                position=i,
                pp=404,
                price=100,
                reqid='123456789abcdef',
                shop_id=NotEmpty(),
                show_block_id="048841920011177788888",
                show_time=488419200,
                touch=0,
                type_id=0,
                uid="0488419200111777888880000" + str(i),
                url_type=UrlType.EXTERNAL,
                vnd_id=12,
                ware_md5=EqualToOneOf(*offer_ware_ids),
                yandexuid=1,
            )

    def test_parallel_show_log_model_incut_touch(self):
        self.report.request_bs(
            'place=parallel&text=molot+3000&ignore-mn=1&pp=1&pof=1&wprid=1&user_type=1&touch=1&reqid=123456789abcdef'
            '&stat-block-id=2000000000020160101000000&rids=213&yandexuid=1&ip=127.0.0.1&test-buckets=1,0,0'
        )
        for i in range(1, 6):
            self.show_log.expect(
                autobroker_enabled=1,
                best_deal=1,
                bid=10,
                bs_block_id=2000000000820160101000000,
                category_id=2,
                click_price=10,
                click_type_id=0,
                cpa=1,
                ctr=20000000,
                discount_ratio=0.5,
                discount=1,
                event_time=488419200,
                geo_id=213,
                home_region=226,
                hyper_id=500,
                ip='127.0.0.1',
                is_price_from=0,
                url='http://molotok.com/molotok',
                nid=4,
                oldprice=200,
                onstock=1,
                position=i,
                pp=402,
                price=100,
                query_context='b956d5f0d5c4dopaliha666 3',
                reqid='123456789abcdef',
                shop_category='molotok.com:Unknown',
                shop_id=NotEmpty(),
                shop_name='molotok.com',
                show_block_id="048841920011177788888",
                show_uid="0488419200111777888880000" + str(i),
                super_uid="0488419200111777888880000" + str(i),
                test_buckets='1,0,0',
                title='molotok',
                touch=1,
                url_type=UrlType.EXTERNAL,
                vbid=0,
                vendor_click_price=0,
                wprid=1,
                ware_md5=EqualToOneOf(*offer_ware_ids),
                yandex_uid='1',
                record_type=0,
                hyper_cat_id=3,
                vendor_id=12,
            )
            self.click_log.expect(
                clicktype=ClickType.EXTERNAL,
                bd=1,
                bs_block_id=2000000000820160101000000,
                categid=2,
                cb=10,
                cb_vnd=0,
                cp=10,
                cp_vnd=0,
                cpa=1,
                data_ts=488419200,
                data_url='http%3A%2F%2Fmolotok.com%2Fmolotok',
                dtype='market',
                geo_id=213,
                hyper_cat_id=3,
                hyper_id=500,
                is_price_from=0,
                link_id="0488419200111777888880000" + str(i),
                nav_cat_id=4,
                onstock=1,
                position=i,
                pp=402,
                price=100,
                reqid='123456789abcdef',
                shop_id=NotEmpty(),
                show_block_id="048841920011177788888",
                show_time=488419200,
                touch=1,
                type_id=0,
                uid="0488419200111777888880000" + str(i),
                url_type=UrlType.EXTERNAL,
                vnd_id=12,
                ware_md5=EqualToOneOf(*offer_ware_ids),
                yandexuid=1,
            )

    def test_prime_encrypted_url(self):
        self.report.request_json(
            'place=prime&text=samsung&stat-block-id=yura&rids=213'
            '&glfilter=2:3&hid=3&ip=8.8.8.8&pof=123&pp=321&reqid=yuraaka&user_type=moron'
            '&uuid=100500&show-urls=encrypted,cpa,showPhone'
            '&test_bits=8&test_tag=16&utm_source=50&utm_campaign=51'
            '&utm_medium=52&utm_term=53&yandexuid=200500'
        )

        self.show_log.expect(
            autobroker_enabled=1,
            best_deal=1,
            bid=20,
            bs_block_id='yura',
            category_id=2,
            click_price=1,
            click_type_id=0,
            cooked_query='samsung::1284',
            cpa=1,
            ctr=20000000,
            discount_ratio=0.5,
            discount=1,
            event_time=488419200,
            geo_id=213,
            gl_filters='2:3',
            home_region=226,
            hyper_id=300,
            ip='8.8.8.8',
            is_price_from=0,
            nid=4,
            url='http://www.shop-100.ru/c20ad4d76fe97759aa27a0c99bff6710',
            oldprice=200,
            original_query='samsung',
            pof=123,
            position=1,
            pp=321,
            price=100,
            query_context='samsung',
            reqid='yuraaka',
            shop_category='apple.com:Unknown',
            shop_id=100,
            shop_name='apple.com',
            show_block_id="048841920011177788888",
            show_uid="04884192001117778888800001",
            super_uid="04884192001117778888800001",
            test_bits=8,
            test_tag=16,
            title='samsung',
            touch=0,
            url_type=UrlType.EXTERNAL,
            utm_campaign=51,
            utm_medium=52,
            utm_source=50,
            utm_term=53,
            user_type='moron',
            uuid=100500,
            vbid=0,
            vendor_click_price=0,
            ware_md5='09lEaAKkQll1XTjm0WPoIA',
            yandex_uid='200500',
            record_type=0,
        )

        self.click_log.expect(clicktype=ClickType.SHOW_PHONE, url_type=UrlType.SHOW_PHONE)

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            ae=1,
            bd=1,
            bs_block_id='yura',
            categid=2,
            cb=20,
            cb_vnd=0,
            cp=1,
            cp_vnd=0,
            cpa=1,
            data_ts=488419200,
            data_uid=200500,
            data_url='http%3A%2F%2Fwww.shop-100.ru%2Fc20ad4d76fe97759aa27a0c99bff6710',
            dtsrc_id=701,
            dtype='market',
            geo_id=213,
            hyper_cat_id=3,
            hyper_id=300,
            is_price_from=0,
            link_id="04884192001117778888800001",
            nav_cat_id=4,
            onstock=1,
            pof=123,
            position=1,
            pp=321,
            price=100,
            reqid='yuraaka',
            shop_id=100,
            show_block_id="048841920011177788888",
            show_time=488419200,
            test_bits=8,
            test_tag=16,
            touch=0,
            type_id=0,
            url_type=UrlType.EXTERNAL,
            uid="04884192001117778888800001",
            utm_campaign=51,
            utm_medium=52,
            utm_source=50,
            utm_term=53,
            uuid=100500,
            vnd_id=12,
            ware_md5='09lEaAKkQll1XTjm0WPoIA',
            yandexuid=200500,
        )

    def test_model_log_in_prime(self):

        self.report.request_json('place=prime&text=topor')
        self.show_log.expect(
            bid=0,  # shop_bid для модели равна 0 @see MARKETOUT-22297
            category_id=22,
            click_price=0,
            hyper_id=1000000005,
            nid=11,
            shop_category='',
            shop_name='Yandex.Market.Models',
            show_block_id=NotEmpty(),
            shop_id=99999999,
            show_uid=NotEmpty(),
            super_uid=NotEmpty(),
            title='topor visual',
            url=LikeUrl.of('//market.yandex.ru/product/1000000005?hid=18&nid=11'),
            vcluster_id=1000000005,
            position=1,
        )
        self.show_log.expect(
            hyper_id=13,
            title='topor',
            position=2,
            hyper_cat_id=17,
            vendor_id=37,
        )

    def test_mixed_log_in_prime(self):
        self.report.request_json('place=prime&text=radio&show-urls=encrypted&rids=213')
        self.show_log.expect(show_uid="04884192001117778888800001")
        self.show_log.expect(show_uid="04884192001117778888817002")
        self.show_log.expect(show_uid="04884192001117778888816003")

    def test_prime_no_logging_without_explicit_showurl_set(self):
        self.report.request_json('place=prime&text=samsung&rids=0&pp=18', add_defaults=False)
        self.show_log.expect().times(0)
        self.click_log.expect().times(0)

    def test_prime_many_offers_log(self):
        self.report.request_json('place=prime&text=sony&show-urls=encrypted')
        self.show_log_tskv.expect(
            title='sony 1', show_uid="04884192001117778888800001", super_uid="04884192001117778888800001"
        )
        self.show_log_tskv.expect(
            title='sony 2', show_uid="04884192001117778888800002", super_uid="04884192001117778888800002"
        )

    def test_output_cpc_in_prime(self):
        response = self.report.request_json('place=prime&text=samsung&show-urls=external')
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "cpc": NotEmpty()}]})

    def test_utm_and_test_cgi_parameters(self):
        self.report.request_json(
            'place=prime&text=samsung&rids=213'
            '&test_bits=16&test_tag=42'
            '&utm_source=54&utm_campaign=55'
            '&utm_medium=56&utm_term=57'
            '&show-urls=external'
        )

        self.show_log.expect(test_bits=16, test_tag=42, utm_source=54, utm_campaign=55, utm_medium=56, utm_term=57)

    def test_delivery_type_fields(self):
        # Запрашиваем с регионом 2
        for place in ['defaultoffer']:
            self.report.request_xml('place={}&hyperid=4&rids=2&show-urls=external'.format(place))
        for place in ['prime', 'productoffers']:
            self.report.request_json('place={}&hyperid=4&rids=2&show-urls=external'.format(place))
        self.report.request_xml(
            'place=api_offerauction&hyperid=4&api_offerid_to_rate=VZJptjZEjnC7k00uGbFa-A&api_shopid_to_rate=3&rids=2&pp=18'
        )
        self.show_log_tskv.expect(
            delivery_type='Priority',
            courier_type='Priority',
            pickup_type='None',
            store_type='None',
            downloadable_type='None',
        ).times(4)

        # То же самое, но с регионом 187 - офферы уезжают под черту
        for place in ['defaultoffer']:
            self.report.request_xml('place={}&hyperid=4&rids=187&show-urls=external,callPhone'.format(place))
        for place in ['prime', 'productoffers']:
            self.report.request_json('place={}&hyperid=4&rids=187&show-urls=external'.format(place))
        self.report.request_xml(
            'place=api_offerauction&hyperid=4&api_offerid_to_rate=VZJptjZEjnC7k00uGbFa-A&api_shopid_to_rate=3&rids=187&pp=18'
        )
        self.show_log_tskv.expect(
            delivery_type='Priority',
            courier_type='Country',
            pickup_type='Country',
            store_type='Priority',
            downloadable_type='None',
        ).times(5)

    def test_delivery_type_downloadable_field(self):
        for place in ['defaultoffer']:
            self.report.request_xml('place={}&hyperid=5&rids=5&show-urls=external'.format(place))
        for place in ['prime', 'productoffers']:
            self.report.request_json('place={}&hyperid=5&rids=5&show-urls=external'.format(place))
        self.report.request_xml(
            'place=api_offerauction&hyperid=5&api_offerid_to_rate=Ym--g_jH8mcLkKdztzvxfA&api_shopid_to_rate=3&rids=5&pp=18'
        )
        self.show_log_tskv.expect(
            delivery_type='Priority',
            courier_type='Priority',
            pickup_type='None',
            store_type='None',
            downloadable_type='Priority',
        ).times(4)

    def test_cpa_fraud_flags_field(self):
        """Значение CGI параметра cflags записывается в поле cpa_fraud_flags лога показов"""
        # Делаем запрос с текстом iphone и show-urls=external.
        self.report.request_json('place=prime&text=iphone&cflags=10&show-urls=external')
        # Проверяем запись в tskv лог
        self.show_log_tskv.expect(cpa_fraud_flags=10)

    def test_cpa_fraud_flags_field_default(self):
        """В отсутствие CGI параметра cflags в поле cpa_fraud_flags лога показов записывается 0"""
        # Делаем запрос с текстом iphone и show-urls=external.
        self.report.request_json('place=prime&text=iphone&show-urls=external')
        # Проверяем запись в tskv лог
        self.show_log_tskv.expect(cpa_fraud_flags=0)

    def test_feed_id(self):
        _ = self.report.request_json('place=prime&text=radio&show-urls=encrypted&rids=213')
        self.show_log.expect(feed_id=201)
        self.show_log.expect(feed_id='').times(2)

    @classmethod
    def prepare_url_type_tests(cls):
        cls.index.models += [Model(title='urlTypeModel')]
        cls.index.vclusters += [VCluster(title='urlTypeVcluster', vclusterid=1000000009)]

        cls.index.offers += [Offer(vclusterid=1000000009)]

    def test_model_url_type(self):
        """https://st.yandex-team.ru/MARKETOUT-10980"""

        self.report.request_json('place=prime&text=urlTypeModel')
        self.show_log.expect(show_uid="04884192001117778888816001")  # 16

    def test_vcluster_url_type(self):
        """https://st.yandex-team.ru/MARKETOUT-10980"""

        self.report.request_json('place=prime&text=urlTypeVcluster')
        self.show_log.expect(show_uid="04884192001117778888817001")  # 17

    @classmethod
    def prepare_tariff(cls):
        cls.index.shops += [
            Shop(fesh=701, home_region=Const.ROOT_COUNTRY, tariff="CLICKS", pickup_buckets=[7001]),
            Shop(fesh=702, home_region=Const.ROOT_COUNTRY, tariff="FIX", online=False, pickup_buckets=[7002]),
            Shop(fesh=703, home_region=Const.ROOT_COUNTRY, tariff="FREE", pickup_buckets=[7003]),
        ]
        cls.index.outlets += [
            Outlet(point_id=701, fesh=701, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=702, fesh=702, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=703, fesh=703, region=213, point_type=Outlet.FOR_PICKUP),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=7001,
                fesh=701,
                carriers=[99],
                options=[PickupOption(outlet_id=701)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=7002,
                fesh=702,
                carriers=[99],
                options=[PickupOption(outlet_id=702)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=7003,
                fesh=703,
                carriers=[99],
                options=[PickupOption(outlet_id=703)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]
        cls.index.offers += [
            Offer(hyperid=710, fesh=701),
            Offer(hyperid=710, fesh=702),
            Offer(hyperid=710, fesh=703),
        ]

    def test_tariff(self):
        self.report.request_json('place=geo&hyperid=710&show-urls=external')
        self.show_log_tskv.expect(shop_id=701, tariff=0)
        self.show_log_tskv.expect(shop_id=702, tariff=1)
        self.show_log_tskv.expect(shop_id=703, tariff=2)

    @classmethod
    def prepare_test_model_vendor_bids(cls):
        # Создаем модель с вендорными ставками и без
        cls.index.models += [Model(title='ModelWithBid', datasource_id=42, vbid=10), Model(title='ModelWithoutBid')]

    def test_test_model_vendor_bids(self):
        # У модели со ставками ставки появляются в show-log
        self.report.request_json('place=prime&text=ModelWithBid&rearr-factors=market_force_use_vendor_bid=1')
        self.show_log.expect(vendor_ds_id=42, vendor_price=1, vc_bid=10)  # Автоброкер

        # У модели без ставок в show-log нули
        self.report.request_json('place=prime&text=ModelWithoutBid&rearr-factors=market_force_use_vendor_bid=1')
        self.show_log.expect(vendor_ds_id=0, vendor_price=0, vc_bid=0)

    def test_tab_replacing(self):
        """
        Проверяем, что \t заменятся на пробел в шоу-логе
        """
        self.report.request_json('place=prime&text=iphone&show-urls=external&utm_source=123\t456')
        self.show_log.expect(utm_source='123 456')

    def test_linefeed_replacing(self):
        """
        Проверяем, что \n заменятся на пробел в шоу-логе
        """
        self.report.request_json('place=prime&text=iphone&show-urls=external&utm_source=123\n456')
        self.show_log.expect(utm_source='123 456')
        self.error_log.expect("Illegal clickdaemon symbol")

    def test_new_reqid(self):
        # new reqid may contain /, expect report to url encode it
        response = self.report.request_json(
            'reqid=1467811624/2f343e80a77ef49b721339ff39f52d05'
            '&place=prime&text=samsung&stat-block-id=yura&rids=213'
            '&glfilter=2:3&hid=3&ip=8.8.8.8&pof=123&pp=321&user_type=moron'
            '&uuid=100500&show-urls=encrypted,cpa'
            '&rearr-factors=market_new_minimal_bid=0'
            '&test_bits=8&test_tag=16&utm_source=50&utm_campaign=51'
            '&utm_medium=52&utm_term=53&yandexuid=200500'
        )
        self.assertFragmentIn(
            response,
            {"results": [{'urls': {'encrypted': Contains('reqid=1467811624%2F2f343e80a77ef49b721339ff39f52d05')}}]},
        )

    def test_escaped_reqid(self):
        response = self.report.request_json(
            'reqid=1467811624%2F2f343e80a77ef49b721339ff39f52d05'
            '&place=prime&text=samsung&stat-block-id=yura&rids=213'
            '&glfilter=2:3&hid=3&ip=8.8.8.8&pof=123&pp=321&user_type=moron'
            '&uuid=100500&show-urls=encrypted,cpa'
            '&rearr-factors=market_new_minimal_bid=0'
            '&test_bits=8&test_tag=16&utm_source=50&utm_campaign=51'
            '&utm_medium=52&utm_term=53&yandexuid=200500'
        )
        self.show_log.expect(reqid='1467811624/2f343e80a77ef49b721339ff39f52d05')
        self.assertFragmentIn(
            response,
            {"results": [{'urls': {'encrypted': Contains('reqid=1467811624%2F2f343e80a77ef49b721339ff39f52d05')}}]},
        )

    def test_fee_value_over_cpc(self):
        """
        @see MARKETOUT-11290
        Create cpc with not set fee, minimal_fee and shop_fee
        Current auction fee value should taken once offer is rendered with this cpc
        """
        cpc = str(Cpc.create_for_offer(click_price=91, offer_id='RcSMzi4tf73qGvxRx8atJg', bid=80, shop_id=601))

        response = self.report.request_json(
            'place=productoffers&hyperid=501&fesh=601&cpc={}&show-urls=external,cpa'.format(cpc)
        )
        self.assertFragmentIn(response, {'titles': {'raw': 'bid=80, fee=800 offer'}}, preserve_order=False)

        response = self.report.request_json(
            'place=offerinfo&offerid=RcSMzi4tf73qGvxRx8atJg&cpc={}&show-urls=external,cpa&rids=213&regset=2'.format(cpc)
        )

        # productoffers and offerinfo
        self.click_log.expect(ClickType.EXTERNAL, cp=91, cb=80, ware_md5='RcSMzi4tf73qGvxRx8atJg').times(1)

        # offerinfo and productoffers
        # fee in click equals to shop fee because no AB is on offerinfo and fee in click equals to min fee because of hybrid AB
        self.show_log.expect(
            url_type=ClickType.EXTERNAL, click_price=91, bid=80, ware_md5='RcSMzi4tf73qGvxRx8atJg'
        ).times(1)

    def test_non_cpa_offer_has_no_fee_fields_in_cpc(self):
        """
        @see MARKETOUT-11290
        Create cpc with not set fee, minimal_fee and shop_fee
        Request a card with single non CPA (CPC-only) offer
        No fee related fields in CPC expected.
        Cpc.create_for_offer method do not set fee fields, hence, serialized cpc strings should be equal
        """
        cpc = str(
            Cpc.create_for_offer(
                click_price=1,
                click_price_before_bid_correction=1,
                offer_id='NotCpaOfferForCPCTestg',
                hid=111,
                bid=70,
                minimal_bid=1,
                shop_id=602,
                bid_type='cbid',
                pp=18,
            )
        )
        response = self.report.request_json('place=productoffers&hyperid=504&show-urls=external,cpa')
        self.assertFragmentIn(response, {'cpc': str(cpc)}, preserve_order=False)

    def test_url_hash(self):
        """
        Проверяем, что url_hash пишется в лог показов.
        """
        _ = self.report.request_json('place=productoffers&hyperid=30&show-urls=external')
        self.show_log.expect(url_hash=NotEmpty())

    def test_x_yandex_icookie(self):
        """
        Проверяем, что зашифрованное значение icookie, попадает в шоу-лог в расшифрованном виде
        BCEmkyAbCsICEPzTQsKKZiwaEphOTfJYcplJsb6WeCNz9ThbLjUw5pw1K8G40cyJPs%2BVrWxAzPzzs34zCBQWvGkphV4%3D => 6774478491508471626
        """
        self.report.request_json('place=prime&text=iphone&show-urls=external&x-yandex-icookie=6774478491508471626')
        self.show_log_tskv.expect(icookie='6774478491508471626')

    def test_decrypted_url(self):
        """
        Проверяем, что для decrypted ссылки не пишется показ
        """
        _ = self.report.request_json('place=productoffers&hyperid=30&show-urls=external,decrypted')
        self.show_log.expect().times(1)

    def test_min_price_and_ctrs_from_hybrid_auction(self):
        """
        Check that model_min_price and 3 types of ctr and number of snippets
        are written to show_log
        """

        self.report.request_json('&place=productoffers' '&hyperid=30' '&show-urls=external')

        self.show_log.expect(
            model_min_price=100,
            cpm=25000000,
            cpc_ctr=0.1,
            cpa_ctr=0,  # cpa_ctr = 0  потому что для cpa-офферов берутся cpc настройки аукциона
            snippets_number=1,
        )

    def test_puid(self):
        self.report.request_json('&place=productoffers' '&hyperid=30' '&puid=1000' '&show-urls=external')

        self.show_log.expect(passport_uid='1000')

    def test_offer_id(self):
        """
        Check if offer ID is written to show-log for productoffers & prime
        """

        requests = [
            'place=productoffers&hyperid=503&show-urls=external&rids=213&pp=6',
            'place=prime&text=не мяучий оффер',
        ]
        for req in requests:
            self.report.request_json(req)
            self.show_log.expect(offer_id=NotEmpty())

    def test_log_disabled_during_lockdown(self):
        """
        Проверяем, что ничего не пишется в show-tskv.log во время локдауна.
        """

        def get_log_sizes():
            log_path = os.path.join(self.meta_paths.logs, 'show-tskv.log')
            self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
            size_before = os.path.getsize(log_path)
            self.report.request_json('place=prime&text=мяучий&show-urls=external')
            self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
            size_after = os.path.getsize(log_path)
            return size_before, size_after

        self.report.request_plain('place=report_status&report-lockdown=1')
        size_before, size_after = get_log_sizes()
        self.assertEqual(size_after, size_before)

        self.report.request_plain('place=report_status&report-lockdown=0')
        size_before, size_after = get_log_sizes()
        self.assertGreater(size_after, size_before)

    def test_log_for_rotation(self):
        self.report.request_json('place=prime&text=ForRotation&show-urls=external')
        self.show_log.expect(query_context='forrotation', original_query='ForRotation', for_rotation=1)

    def test_log_for_rotation_negative(self):
        self.report.request_json('place=prime&text=NotForRotation&show-urls=external')
        self.show_log.expect(query_context='notforrotation', original_query='NotForRotation', for_rotation=None)

    def test_next_offer_cpm_existance_for_productoffers(self):
        """
        Check if next offer CPM is written to show-log for productoffers
        """
        _ = self.report.request_json('place=productoffers&hyperid=503&show-urls=external&rids=213&pp=6')
        self.show_log.expect(next_offer_cpm=NotEmpty())

    def test_index_generation(self):
        """
        Check that the right index generation is written to show-log
        """
        self.report.request_json('place=productoffers&hyperid=30&show-urls=external')
        self.show_log.expect(record_type=0, hyper_id=30, index_generation=self.index.fixed_index_generation)

        self.report.request_json('place=prime&text=urlTypeModel')
        self.show_log.expect(
            record_type=1, show_uid='04884192001117778888816001', index_generation=self.index.fixed_index_generation
        )

        self.report.request_json('place=prime&text=urlTypeVcluster')
        self.show_log.expect(
            record_type=1, show_uid='04884192001117778888817001', index_generation=self.index.fixed_index_generation
        )

    @classmethod
    def prepare_delivery_terms_type(cls):
        cls.index.shops += [
            Shop(fesh=7003, priority_region=213),
            Shop(fesh=7004, priority_region=213),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1703,
                fesh=7003,
                carriers=[77],
                regional_options=[RegionalDelivery(rid=75, options=[DeliveryOption(price=200, day_from=3, day_to=6)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1704,
                fesh=7003,
                carriers=[78],
                regional_options=[
                    RegionalDelivery(rid=76, options=[DeliveryOption(price=50, day_from=1, day_to=2)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=2302,
                options=[PickupOption(outlet_id=12345672, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2303,
                options=[PickupOption(outlet_id=12345673, price=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=12345672, region=1, gps_coord=GpsCoord(37.15, 55.15), fesh=7004),
            Outlet(point_id=12345673, region=2, gps_coord=GpsCoord(37.16, 55.16), fesh=7004),
        ]

        cls.index.offers += [
            Offer(fesh=7003, title='offer 1', delivery_buckets=[1703, 1704]),
            Offer(fesh=7004, title='offer 2', has_delivery_options=False, pickup_buckets=[2302, 2303]),
        ]

    def test_delivery_terms_type(self):
        self.report.request_json('place=prime&fesh=7003&rids=75&onstock=1')
        self.show_log.expect(courier_delivery_terms_type=2)

        self.report.request_json('place=prime&fesh=7003&rids=76&onstock=1')
        self.show_log.expect(courier_delivery_terms_type=1)

        self.report.request_json('place=prime&fesh=7004')
        self.show_log.expect(courier_delivery_terms_type=0)

        self.report.request_json('place=prime&fesh=7004&rids=1&pickup-options=grouped')
        self.show_log.expect(pickup_delivery_terms_type=2)

        self.report.request_json('place=prime&fesh=7004&rids=2&pickup-options=grouped&mega-points=1')
        self.show_log.expect(pickup_delivery_terms_type=1)

        self.report.request_json('place=prime&fesh=7003')
        self.show_log.expect(pickup_delivery_terms_type=0)

    def test_x_market_req_id(self):
        headers = {'X-Market-Req-ID': "x_market_req_id"}
        self.report.request_json('place=prime&fesh=7003&rids=75&onstock=1&reqid=123')
        self.show_log.expect(x_market_req_id="")

        self.report.request_json('place=prime&fesh=7003&rids=75&onstock=1&reqid=123', headers=headers)
        self.show_log.expect(x_market_req_id="x_market_req_id")

    def test_previous_pp(self):
        cpc = str(Cpc.create_for_model(model_id=31, pp=246))
        response = self.report.request_json(
            'place=prime&text=jah+ith+ber&pp=247&rearr-factors=market_report_click_context_enabled=0&cpc=' + cpc
        )
        self.assertFragmentIn(response, {"raw": "jah ith ber"})
        self.assertFragmentIn(response, {"raw": "dol um ber ist"})
        self.show_log.expect(pp=247, prev_pp=246, title="jah ith ber")
        self.show_log.expect(pp=247, prev_pp=246, title="dol um ber ist")

        cpc = str(Cpc.create_for_offer(click_price=71, offer_id='RcSMzi4tf73qGvxRx8atJg', bid=80, shop_id=71, pp=248))
        response = self.report.request_json(
            'place=prime&text=dol+um+ber+ist&pp=249&rearr-factors=market_report_click_context_enabled=0&cpc=' + cpc
        )
        self.assertFragmentIn(response, {"raw": "jah ith ber"})
        self.assertFragmentIn(response, {"raw": "dol um ber ist"})
        self.show_log.expect(pp=249, prev_pp=248, title="jah ith ber")
        self.show_log.expect(pp=249, prev_pp=248, title="dol um ber ist")

    def test_previous_pp_from_cc(self):
        cc = str(ClickContext(pp=246))
        response = self.report.request_json(
            'place=prime&text=jah+ith+ber&pp=247&rearr-factors=market_report_click_context_enabled=1&cc=' + cc
        )
        self.assertFragmentIn(response, {"raw": "jah ith ber"})
        self.assertFragmentIn(response, {"raw": "dol um ber ist"})
        self.show_log.expect(pp=247, prev_pp=246, title="jah ith ber")
        self.show_log.expect(pp=247, prev_pp=246, title="dol um ber ist")

        cc = str(ClickContext(pp=248))
        response = self.report.request_json(
            'place=prime&text=dol+um+ber+ist&pp=249&rearr-factors=market_report_click_context_enabled=1&cc=' + cc
        )
        self.assertFragmentIn(response, {"raw": "jah ith ber"})
        self.assertFragmentIn(response, {"raw": "dol um ber ist"})
        self.show_log.expect(pp=249, prev_pp=248, title="jah ith ber")
        self.show_log.expect(pp=249, prev_pp=248, title="dol um ber ist")

    def test_search_params(self):
        _ = self.report.request_json(
            'place=prime&utm_source_service=web&src_pof=703&x-yandex-src-icookie=7111647341623827291&baobab_event_id=kuiggrsq9q&text=dol+um+ber+ist'
        )
        self.show_log.expect(utm_source_service='web')
        self.show_log.expect(src_pof='703')
        self.show_log.expect(src_icookie='7111647341623827291')
        self.show_log.expect(baobab_event_id='kuiggrsq9q')


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Book,
    CardCategory,
    CardVendor,
    CategoryRestriction,
    ClickType,
    CpaCategory,
    CpaCategoryType,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    ExchangeRate,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    ModelGroup,
    NavCategory,
    NewShopRating,
    Offer,
    Opinion,
    Outlet,
    PickupBucket,
    PickupOption,
    Picture,
    QueryEntityCtr,
    Region,
    RegionalModel,
    RegionalRestriction,
    ReportState,
    Shop,
    UrlType,
    VCluster,
    Vendor,
    VendorLogo,
    YamarecPlaceReasonsToBuy,
)
from core.types.picture import thumbnails_config
from core.testcase import TestCase, main
from core.matcher import (
    LikeUrl,
    Absent,
    NotEmpty,
    Contains,
    NoKey,
    ElementCount,
    EmptyList,
    NotEmptyList,
    Round,
    Regex,
    Capture,
)

from core.types.vendor import PublishedVendor

from core.blackbox import BlackboxUser
from core.types.hypercategory import TOBACCO_CATEG_ID
from core.carter import CartOffer

import re
import urllib


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]

        # implicit model wizard
        cls.index.hypertree += [HyperCategory(hid=30, name='Китайские мобилы')]
        cls.index.navtree += [NavCategory(nid=10, hid=30)]
        # for category_title tests
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 30).respond(0.7)

        cls.index.models += [
            Model(
                hyperid=101,
                ts=101000,
                hid=30,
                title="panasonic tumix 5000",
                opinion=Opinion(rating=4.5, rating_count=10, total_count=12, precise_rating=4.46),
                picinfo='//avatars.mds.yandex.net/get-mpic/5678/test9901/orig#100#100',
            ),
            Model(
                hyperid=102,
                ts=102000,
                hid=30,
                title="panasonic tumix pt6000",
                opinion=Opinion(rating=2.5, precise_rating=2.73),
                picinfo='//avatars.mds.yandex.net/get-mpic/5678/test99/orig#100#100',
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=100500,
                ts=103000,
                hid=1500,
                title="barbie doll without offers",
                opinion=Opinion(rating=5, rating_count=10, total_count=12, precise_rating=5),
                picinfo='//avatars.mds.yandex.net/get-mpic/5678/test9901/orig#100#100',
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101000).respond(0.65)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102000).respond(0.54)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103000).respond(0.44)

        cls.index.offers += [
            Offer(hyperid=101, price=666, fesh=12),
            Offer(hyperid=101, price=700),
            Offer(hyperid=102, price=777),
            Offer(hyperid=102, price=800),
            # offer for testing shop_count
            Offer(title='panasonic tumix', waremd5='obmaDftYV4bsBdqYgc5WmQ'),
        ]

        cls.index.gltypes += [
            GLType(param_id=13213456, hid=30, gltype=GLType.ENUM, values=[100, 16358321]),
        ]

        cls.index.models += [
            Model(
                hyperid=3333,
                ts=104000,
                hid=30,
                title="xiaomi redmi note 9 pro",
                opinion=Opinion(rating=4.5, rating_count=10, total_count=12, precise_rating=4.46),
                glparams=[GLParam(param_id=13213456, value=16358321)],
                picinfo='//avatars.mds.yandex.net/get-mpic/5678/test9901/orig#100#100',
            ),
        ]
        cls.index.offers += [
            Offer(hyperid=3333, price=700),
        ]

        # implicit model wizard with models from different categories
        cls.index.models += [
            Model(hyperid=103, hid=31, title="sony xperia z5", opinion=Opinion(rating=3.5)),
            Model(hyperid=104, hid=32, title="sony xperia z4", opinion=Opinion(rating=1.5)),
        ]

        cls.index.offers += [Offer(hyperid=103), Offer(hyperid=104)]

        # model wizard
        cls.index.shops += [
            Shop(fesh=12, priority_region=213),
            Shop(fesh=13, priority_region=213, main_fesh=13),
            Shop(fesh=14, priority_region=213, main_fesh=13),
        ]

        cls.index.offers += [
            Offer(hyperid=106, price=888, title="pepelac 2000", fesh=12, bid=500),
            Offer(hyperid=106, price=888, title="pepelac 2000", fesh=13, bid=500),
            Offer(hyperid=106, price=888, title="pepelac 2000", fesh=14, bid=500),
        ]

        cls.index.models += [Model(hyperid=106, hid=30, title="pepelac 2000", opinion=Opinion(rating=2.5))]

        # offers wizard
        cls.index.hypertree += [HyperCategory(hid=130, name='Киянки')]
        cls.index.navtree += [NavCategory(nid=130, hid=130)]
        cls.index.shops += [
            Shop(fesh=100, priority_region=213),
            Shop(fesh=101, priority_region=213),
            Shop(fesh=102, priority_region=213),
        ]
        cls.index.offers += [
            Offer(
                title="kiyanka",
                url='http://kiyanochnaya.ru/kiyanki?id=1',
                fesh=100,
                hid=130,
                waremd5='f7_BYaO4c78hGceI7ZPR9A',
                offer_url_hash='12345123451',
                ts=201,
            ),
            Offer(title="kiyanka rezinovaya", fesh=101, ts=202),
            Offer(title="kiyanka rezinovaya s ruchkoi", fesh=102, ts=203),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 202).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 203).respond(0.7)

        # non guru wizard
        cls.index.regiontree += [Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в')]

        cls.index.shops += [
            Shop(fesh=1, name="shop 1", priority_region=213, regions=[225], cpa=Shop.CPA_REAL, pickup_buckets=[5001]),
            Shop(fesh=2, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=3, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=4, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=5, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
            Shop(fesh=6, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(fesh=1, region=213, point_id=1),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
        ]

        pic = Picture(
            width=100,
            height=100,
            thumb_mask=thumbnails_config.get_mask_by_names(['100x100']),
            group_id=1234,
            picture_id='5VHT3b-HvdrDaXZb1YITpQ',
        )
        for i in range(1, 10):
            ts = 19980 + i
            cls.index.offers.append(
                Offer(
                    title='molot-molotok-{}'.format(i),
                    ts=ts,
                    picture=pic,
                    picture_flags=ts,
                    bid=100 + 30 - i,
                    fesh=300 + i,
                    hid=130,
                    waremd5='B0F30-tjceMsFsVaO49waw' if i == 1 else None,
                )
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(i * 0.01)
            # test_no_offer_incut_ctr_restriction: хотим ctr < 0.001
            cls.index.ctr.parallel.ts += [QueryEntityCtr('molot molotok', ts, 1, 100000)]

        for i in range(1, 25):
            ts = 18950 + i
            cls.index.offers.append(
                Offer(title='kuvalda-{}'.format(i), ts=ts, picture=pic, picture_flags=ts, bid=100 + 30 - i)
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(i * 0.01)

        cls.index.currencies = [Currency('BYN', exchange_rates=[ExchangeRate(to=Currency.RUR, rate=33)], country=149)]

        cls.index.offers += [
            Offer(hid=40, fesh=1, ts=23450, randx=1, title="iphone5 i5 white recovered"),  # ok
            Offer(
                hid=40, fesh=2, ts=23451, randx=2, title="iphone5 without picture", no_picture=True
            ),  # NOT ok (no picture)
            Offer(hid=40, fesh=3, ts=23452, randx=3, title="iphone5 silver"),  # ok
            Offer(hid=40, fesh=4, ts=23453, randx=4, title="iphone5"),  # ok
            Offer(hid=40, fesh=5, ts=23454, randx=5, title="white5"),  # NOT ok (no word iphone)
            Offer(hid=40, fesh=6, ts=23455, randx=6, title="iphone5 i5 with old diamond"),  # ok
        ]

        for i in range(6):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23450 + i).respond(0.9 - 0.01 * i)

        # test_plitka_incut_with_offercard_urls

        cls.index.shops += [
            Shop(
                fesh=400,
                cpa=Shop.CPA_REAL,
                shop_grades_count=10,
                priority_region=213,
                new_shop_rating=NewShopRating(
                    new_rating=4.5,
                    new_rating_total=3.9,
                    new_grades_count_3m=123,
                    new_grades_count=456,
                    rec_and_nonrec_pub_count=789,
                ),
            ),
            Shop(fesh=401, priority_region=213),
            Shop(
                fesh=402,
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=4.0, abo_old_rating=3, rec_and_nonrec_pub_count=20),
                shop_grades_count=20,
                priority_region=213,
            ),
            Shop(
                fesh=403,
                priority_region=213,
                new_shop_rating=NewShopRating(
                    new_rating_total=3.4,
                    rec_and_nonrec_pub_count=100,
                ),
            ),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=300, name='PelmenVendor0NoLogo'),
            Vendor(
                vendor_id=301,
                name='PelmenVendor1',
                logos=[VendorLogo(url='//avatars.mdst.yandex.net/get-mpic/301/test301/orig')],
            ),
            Vendor(
                vendor_id=302,
                name='PelmenVendor2',
                logos=[VendorLogo(url='//avatars.mdst.yandex.net/get-mpic/302/test302/orig')],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='pelmen-1',
                picture=pic,
                waremd5="09lEaAKkQll1XTjm0WPoIA",
                fesh=400,
                ts=1,
                url="http://pelmennaya.ru/pelmens?id=1",
                hid=12345,
                vendor_id=300,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                title='pelmen-2',
                picture=pic,
                waremd5="xMpCOKC5I4INzFCab3WEmQ",
                fesh=401,
                ts=2,
                url="http://pelmennaya.ru/pelmens?id=2",
                hid=12345,
                vendor_id=301,
                cpa=Offer.CPA_NO,
            ),
            Offer(
                title='pelmen-3',
                picture=pic,
                waremd5="lpc2G9gcBPtOqJHWMQSlow",
                fesh=402,
                ts=3,
                url="http://pelmennaya.ru/pelmens?id=3",
                hid=12345,
                vendor_id=301,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                title='pelmen-4',
                picture=pic,
                waremd5="JMzqt__BEY9dqqzhPGcIhQ",
                fesh=403,
                ts=4,
                url="http://pelmennaya.ru/pelmens?id=4",
                hid=12345,
                vendor_id=302,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.PLITKA_INCUT, 1).respond(0.5)
        cls.matrixnet.on_place(MnPlace.PLITKA_INCUT, 2).respond(0.4)
        cls.matrixnet.on_place(MnPlace.PLITKA_INCUT, 3).respond(0.3)
        cls.matrixnet.on_place(MnPlace.PLITKA_INCUT, 4).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.2)

        # test_table_incut_with_offercard_urls

        cls.index.models += [
            Model(hyperid=308, title="shashlyk mashlyk"),
        ]

        cls.index.shops += [
            Shop(fesh=404, priority_region=213),
            Shop(fesh=405, cpa=Shop.CPA_REAL, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                title="shashlyk mashlyk 1",
                waremd5="iXMWkpF2Rk68mtCF8x5yhA",
                fesh=404,
                ts=5,
                hyperid=308,
                url="http://shashlychnaya.ru/shashlyk?id=1",
                cpa=Offer.CPA_NO,
            ),
            Offer(
                title="shashlyk mashlyk 2",
                waremd5="sCYyTGkEsqnLS4jW1hyB0Q",
                fesh=405,
                ts=6,
                hyperid=308,
                url="http://shashlychnaya.ru/shashlyk?id=2",
                cpa=Offer.CPA_REAL,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.04)

        # tires wizard
        cls.reqwizard.on_default_request().respond()
        cls.reqwizard.on_request('shini+na+skoda').respond(tires_mark='Skoda')
        cls.reqwizard.on_request('shini+na+skoda+octavia').respond(tires_mark='Skoda', tires_model='Octavia')

        # ext_category_wizard
        cls.index.vendors += [
            Vendor(vendor_id=10, name='bakery', picinfo='//mdata.yandex.net/i?path=vendor-picture-123.jpg'),
            Vendor(vendor_id=11, name='dairy', picinfo='//mdata.yandex.net/i?path=vendor-picture-456.jpg'),
            Vendor(vendor_id=12, name='butcher', picinfo='//mdata.yandex.net/i?path=vendor-picture-789.jpg'),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=110, name='bakery', picinfo='//avatars.mds.yandex.net/get-mpic/123/test99/orig'),
            Vendor(vendor_id=111, name='dairy', picinfo='//avatars.mds.yandex.net/get-mpic/456/test100/orig'),
            Vendor(vendor_id=112, name='butcher', picinfo='//avatars.mds.yandex.net/get-mpic/789/test101/orig'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, name='food', output_type=HyperCategoryType.GURU),
            HyperCategory(hid=111, name='good', output_type=HyperCategoryType.GURU),
            HyperCategory(hid=112, name='hood', output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [
            NavCategory(hid=100, nid=100),
            NavCategory(hid=111, nid=111),
            NavCategory(hid=112, nid=112),
        ]

        cls.index.cards += [
            CardCategory(hid=100, vendor_ids=[10, 11, 12]),
            CardCategory(hid=111, vendor_ids=[110, 111, 112]),
            CardCategory(hid=112, vendor_ids=[110, 111, 112]),
        ]

        cls.index.models += [
            Model(hyperid=309, hid=112, title='hood 1'),
            Model(hyperid=310, hid=112, title='hood 2'),
        ]

        cls.index.published_vendors += [PublishedVendor(vendor_id=1), PublishedVendor(vendor_id=400)]

        # add direct offers from external shops
        for shop_id in range(10):
            for id in range(1, 11):
                shop_id = 1000 + shop_id
                cls.index.offers += [
                    # add offer with same title as market offers
                    Offer(
                        fesh=shop_id,
                        offerid=shop_id + id,
                        price=id * shop_id + 1,
                        title='sony kiyanka molotok {}-{}'.format(id, shop_id),
                        is_direct=True,
                        url='http://direct_shop_{}.ru/offers?id={}'.format(shop_id, id),
                    )
                ]

    def test_model_universal(self):
        """
        Проверяем модельный колдунщик под конструктором
        https://st.yandex-team.ru/MARKETOUT-25779
        """

        request = "place=parallel&text=panasonic+tumix&rearr-factors=showcase_universal_model=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=502&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "adGUrl": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=914&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=704&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "adGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=920&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "offersUrl": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101/offers?text=panasonic%20tumix&grhow=shop&hid=30&hyperid=101&nid=10&clid=502&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "offersUrlAdG": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101/offers?text=panasonic%20tumix&grhow=shop&hid=30&hyperid=101&nid=10&clid=914&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "offersUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&grhow=shop&hid=30&nid=10&clid=704&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "offersUrlAdGTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&grhow=shop&hid=30&nid=10&clid=920&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "adGMoreUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&clid=914&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "cartUrl": LikeUrl.of(
                        "//pokupki.market.yandex.ru/my/cart?purchase-referrer=market_wizard&clid=502"
                    ),
                    "cartUrlTouch": LikeUrl.of(
                        "//m.pokupki.market.yandex.ru/my/cart?purchase-referrer=market_wizard&clid=704"
                    ),
                    "title": {"__hl": {"text": "Panasonic tumix 5000", "raw": True}},
                    "subtype": "market_model",
                    "type": "market_constr",
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=502&lr=0&utm_medium=cpc&utm_referrer=wizards", ignore_len=False
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=704&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru?clid=914&lr=0&utm_medium=cpc&utm_referrer=wizards", ignore_len=False
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=920&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "Яндекс.Маркет",
                        }
                    ],
                    "modelId": 101,
                    "categoryId": 30,
                    "category_name": "Китайские мобилы",
                    "sitelinksWithCount": "several",
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--panasonic-tumix-5000/101/spec?hid=30&nid=10&text=panasonic%20tumix&clid=502&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101/spec?hid=30&nid=10&text=panasonic%20tumix&clid=704&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/product--panasonic-tumix-5000/101/spec?text=panasonic%20tumix&hid=30&nid=10&clid=914&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101/spec?text=panasonic%20tumix&hid=30&nid=10&clid=920&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "specs",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--panasonic-tumix-5000/101/offers?grhow=shop&hid=30&hyperid=101&nid=10&text=panasonic%20tumix&lr=0&clid=502&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=704&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/product--panasonic-tumix-5000/101/offers?grhow=shop&hid=30&hyperid=101&nid=10&text=panasonic%20tumix&lr=0&clid=914&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=920&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "prices",
                            "hint": "2",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&clid=502&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&clid=704&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&clid=914&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&clid=920&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "opinions",
                            "hint": "12",
                        },
                    ],
                    "picture": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/5678/test9901/2hq", ignore_len=False),
                    "pictureTouch": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/5678/test9901/7hq", ignore_len=False),
                    "pictureTouchHd": LikeUrl.of(
                        "//avatars.mds.yandex.net/get-mpic/5678/test9901/8hq", ignore_len=False
                    ),
                    "pictures": [
                        LikeUrl.of("//avatars.mds.yandex.net/get-mpic/5678/test9901/8hq", ignore_len=False),
                    ],
                    "rating": "4.46",
                }
            },
        )

        request = "place=parallel&text=lenovo+p780&rids=213&rearr-factors=showcase_universal_model=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "picture": LikeUrl.of(Model.DEFAULT_PIC_URL + "&size=2", ignore_len=False),
                    "pictureTouch": LikeUrl.of(Model.DEFAULT_PIC_URL + "&size=7", ignore_len=False),
                    "pictureTouchHd": LikeUrl.of(Model.DEFAULT_PIC_URL + "&size=8", ignore_len=False),
                    "pictures": [
                        LikeUrl.of("//mdata.yandex.net/8hq", ignore_len=False),
                    ],
                    "rating": "3.5",
                    "price": {"type": "min", "priceMin": "8000", "currency": "RUR"},
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of("http://www.shop-1.ru/lenovo_p780_offer1", ignore_len=False),
                                    "adGUrl": LikeUrl.of("http://www.shop-1.ru/lenovo_p780_offer1", ignore_len=False),
                                    "directOffercardUrl": LikeUrl.of(
                                        "//market.yandex.ru/offer/H__rIZIhXqNM4Kq2NlBTSg?text=lenovo%20p780&hid=200&hyperid=301090&lr=213&modelid=301090&nid=2000&clid=914&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_params=['rs', 'cpc'],
                                        ignore_len=False,
                                    ),
                                    "text": {"__hl": {"text": "lenovo p780 cpa offer 1", "raw": True}},
                                },
                                "gradeCount": 0,
                                "price": {
                                    "currency": "RUR",
                                    "type": "average",
                                    "priceMax": "8000",
                                    "priceMin": Absent(),
                                },
                                "greenUrl": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-1/1/reviews?cmid=H__rIZIhXqNM4Kq2NlBTSg&clid=502&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-1/1/reviews?cmid=H__rIZIhXqNM4Kq2NlBTSg&clid=914&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "text": "shop 1",
                                },
                                "delivery": {"text": "Доставка бесплатно"},
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of("http://www.shop-2.ru/lenovo_p780_offer2", ignore_len=False),
                                    "text": {"__hl": {"text": "lenovo p780 cpa offer 2", "raw": True}},
                                },
                                "gradeCount": 0,
                                "price": {"currency": "RUR", "type": "average", "priceMax": "8000"},
                                "greenUrl": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-2/2/reviews?cmid=RxxQrRoVbXJW7d_XR9d5MQ&clid=502&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "text": "SHOP-2",
                                },
                                "delivery": {"currency": "RUR", "price": "100"},
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of("http://www.shop-3.ru/lenovo_p780_offer3", ignore_len=False),
                                    "text": {"__hl": {"text": "lenovo p780 cpa offer 3", "raw": True}},
                                },
                                "gradeCount": 0,
                                "price": {"currency": "RUR", "type": "average", "priceMax": "8000"},
                                "greenUrl": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-3/3/reviews?cmid=1ZSxqUW11kXlniH4i9LYOw&clid=502&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "text": "SHOP-3",
                                },
                                "delivery": {"currency": "RUR", "price": "100"},
                            },
                        ]
                    },
                }
            },
        )

    def test_reask_gallery(self):
        # This test does not replace corresponding integration test because
        # we can't implement checks connected with forced region
        # (that implies 'lr=' in wizard urls)
        # TODO implement after ReqWizard appears in LITE

        # additional g-param is inspired by MARKETINCIDENTS-2791
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "url": "",
                    "urlTouch": "",
                    "cartUrl": LikeUrl.of(
                        "//pokupki.market.yandex.ru/my/cart?purchase-referrer=market_wizard&clid=698"
                    ),
                    "cartUrlTouch": LikeUrl.of(
                        "//m.pokupki.market.yandex.ru/my/cart?purchase-referrer=market_wizard&clid=721"
                    ),
                    "priority": "0",
                    "type": "market_constr",
                    "subtype": "market_reask_gallery",
                    "text": [],
                    "favicon": {"faviconDomain": ""},
                    "model_count": "2",
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%205000&lr=0&modelid=101",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%205000&lr=0&modelid=101",
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                },
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/5678/test9901/2hq", ignore_len=False
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/5678/test9901/5hq", ignore_len=False
                                    ),
                                    "height": "100",
                                    "width": "100",
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%205000&lr=0&modelid=101",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%205000&lr=0&modelid=101",
                                        ignore_len=True,
                                    ),
                                },
                                "price": {"type": "min", "priceMin": "666", "currency": "RUR"},
                                "rating": {"value": "4.46"},
                                "relevance": Regex(".*"),
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%20pt6000&lr=0&modelid=102",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%20pt6000&lr=0&modelid=102",
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}},
                                },
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/5678/test99/2hq", ignore_len=False
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/5678/test99/5hq", ignore_len=False
                                    ),
                                    "height": "100",
                                    "width": "100",
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%20pt6000&lr=0&modelid=102",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%20pt6000&lr=0&modelid=102",
                                        ignore_len=True,
                                    ),
                                },
                                "price": {"type": "min", "priceMin": "777", "currency": "RUR"},
                                "rating": {"value": "2.73"},
                                "relevance": Regex(".*"),
                            },
                        ]
                    },
                }
            },
        )

        log_content = ['"wiz_id":"market_reask_gallery"', '"models":["101","102"]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

    def test_reask_gallery_with_filters(self):
        # This test does not replace corresponding integration test because
        # we can't implement checks connected with forced region
        # (that implies 'lr=' in wizard urls)
        # TODO implement after ReqWizard appears in LITE

        # additional g-param is inspired by MARKETINCIDENTS-2791
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_filters=1;market_include_reask_gallery_wizard_common_filters=1;market_enable_reask_gallery_wizard_models=1;market_reask_gallery_wizard_filtering_algo_id=test;market_reask_gallery_wizard_filter_representation_kind=Short'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%205000&lr=0&modelid=101",
                                        ignore_len=True,
                                        no_params=["prev_query_qtree"],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%205000&lr=0&modelid=101",
                                        ignore_len=True,
                                        no_params=["prev_query_qtree"],
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                },
                            }
                        ]
                    },
                }
            },
        )

        log_content = [
            '"wiz_id":"market_reask_gallery"',
            '"models":["101","102"]',
        ]
        self.access_log.expect(
            wizard_elements=Regex(r'.*' + '.*'.join(re.escape(x).replace(r'\:', ':') for x in log_content) + '.*')
        )

    def test_reask_gallery_get_models_without_offers(self):
        without_offers_fragment = {
            "market_reask_gallery": {
                "showcase": {
                    "items": [
                        {
                            "title": {
                                "text": {"__hl": {"text": "barbie doll without offers", "raw": True}},
                            }
                        }
                    ]
                }
            }
        }

        response = self.report.request_bs_pb(
            'place=parallel&text=barbie+doll+without+offers&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1;market_enable_reask_gallery_wizard_models_without_offers=1'  # noqa
        )
        self.assertFragmentIn(response, without_offers_fragment)

        response = self.report.request_bs_pb(
            'place=parallel&text=barbie+doll+without+offers&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1;'
        )
        self.assertFragmentNotIn(response, without_offers_fragment)

    def test_reask_gallery_do_not_get_obvious_filters(self):
        response = self.report.request_bs_pb(
            'place=parallel&text=xiaomi+redmi+note+9+pro&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_filters=1;market_enable_reask_gallery_wizard_models=1;market_reask_gallery_wizard_filtering_algo_id=test;market_reask_gallery_wizard_filter_threshold_value=0'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "xiaomi redmi note 9 pro", "raw": True}},
                                }
                            }
                        ]
                    },
                }
            },
        )

        # Это очевидный фильтр, так как модель xiaomi redmi note 9 pro уже подходит под этот фильтр
        # При значении market_reask_gallery_wizard_filter_threshold_value=0 необходима больше 0 моделей в выдаче, которые удовлетворяет данному фильтру
        self.assertFragmentNotIn(
            response,
            {
                "market_reask_gallery": {
                    "filters": [
                        {
                            "text": "a",
                        },
                        {
                            "text": "b",
                        },
                    ],
                }
            },
        )

    def test_implicit_model(self):
        # This test does not replace corresponding integration test because
        # we can't implement checks connected with forced region
        # (that implies 'lr=' in wizard urls)
        # TODO implement after ReqWizard appears in LITE

        # additional g-param is inspired by MARKETINCIDENTS-2791
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&g=1.market.10.1.-1...-1.....'
            '&rearr-factors=market_parallel_feature_log_rate=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "priority": "9",
                    "type": "market_constr",
                    "subtype": "market_implicit_model",
                    "title": "\7[Panasonic tumix\7]",
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards", ignore_len=False
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=panasonic%20tumix&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "text": "Panasonic tumix",
                        },
                    ],
                    "text": [
                        {
                            "__hl": {
                                "text": "Цены, характеристики, отзывы на panasonic tumix. Выбор по параметрам. 1 магазин.",
                                "raw": True,
                            }
                        }
                    ],
                    "favicon": {"faviconDomain": "market.yandex.ru"},
                    "offer_count": 1,  # just one offer has 'panasonic tumix' in title
                    "shop_count": "1",
                    "model_count": "2",
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=698&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=721&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "Отзывы",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/geo?text=panasonic%20tumix&lr=0&clid=698&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/geo?text=panasonic%20tumix&lr=0&clid=721&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "На карте",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=698&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=721&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "С доставкой завтра",
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                },
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/5678/test9901/2hq", ignore_len=False
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/5678/test9901/5hq", ignore_len=False
                                    ),
                                    "height": "100",
                                    "width": "100",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                },
                                "price": {"type": "min", "priceMin": "666", "currency": "RUR"},
                                "rating": {"value": "4.46"},
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-pt6000/102?text=panasonic%20tumix&hid=30&nid=10&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-pt6000/102?text=panasonic%20tumix&hid=30&nid=10&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}},
                                },
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/5678/test99/2hq", ignore_len=False
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/5678/test99/5hq", ignore_len=False
                                    ),
                                    "height": "100",
                                    "width": "100",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-pt6000/102?text=panasonic%20tumix&hid=30&nid=10&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-pt6000/102?text=panasonic%20tumix&hid=30&nid=10&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                },
                                "price": {"type": "min", "priceMin": "777", "currency": "RUR"},
                                "rating": {"value": "2.73"},
                            },
                        ]
                    },
                }
            },
        )

        log_content = ['"wiz_id":"market_implicit_model"', '"models":["101","102"]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

        self.feature_log.expect(model_id=101, position=1, other={'request_all_lower': 1, 'has_all_words_ex_full': 1})
        self.feature_log.expect(model_id=102, position=2, other={'request_all_lower': 1, 'has_all_words_ex_full': 1})

    @classmethod
    def prepare_implicit_model_no_shop_count(cls):
        # Implicit model wizard requires 2 ore more models
        cls.index.models += [
            Model(hyperid=107, hid=30, title="nikon d5200 kit"),
            Model(hyperid=108, hid=30, title="nikon d5200 body"),
        ]

        # This models must have regional prices. We introduce model offers with prices to satisfy this requirement.
        # There's no offer with title 'nikon d5200', so no offers will be found on request [nikon d5200].
        cls.index.offers += [
            Offer(hyperid=107, price=888),
            Offer(hyperid=108, price=999),
        ]

    def test_implicit_model_no_shop_count(self):
        """Не отображаем количество магазинов в колдунщике неявной модели, если оно 0"""

        # На запрос [nikon d5200] не находятся офферы, поэтому количество магазинов 0
        # и оно не должно пробрасываться в колдунщик.

        response = self.report.request_bs_pb('place=parallel&text=nikon+d5200')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "offer_count": 0,
                    "shop_count": Absent(),
                }
            },
        )

    def test_cvredirect_offers(self):
        response = self.report.request_bs_pb('place=parallel&text=kiyanka&rearr-factors=market_wiz_offer_titleredir=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=kiyanka&cvredirect=2&from_yandex=1&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=kiyanka&cvredirect=2&from_yandex=1&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "cartUrl": LikeUrl.of(
                        "//pokupki.market.yandex.ru/my/cart?purchase-referrer=market_wizard&clid=545"
                    ),
                    "cartUrlTouch": LikeUrl.of(
                        "//m.pokupki.market.yandex.ru/my/cart?purchase-referrer=market_wizard&clid=708"
                    ),
                    "title": "\7[Kiyanka\7]",
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards", ignore_len=False
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=kiyanka&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=kiyanka&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                    ],
                }
            },
        )

    def test_viewtype_offers(self):
        # при наличии market_wiz_offer_titleredir=1 и market_wiz_offer_viewtype добавляется параметр viewtype
        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka&rearr-factors=market_wiz_offer_titleredir=1;market_wiz_offer_viewtype=list'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?clid=545&cvredirect=2&from_yandex=1&text=kiyanka&viewtype=list&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?clid=708&cvredirect=2&from_yandex=1&text=kiyanka&viewtype=list&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                }
            },
        )
        # без market_wiz_offer_titleredir=1 параметр добавляться не должен
        response = self.report.request_bs_pb('place=parallel&text=kiyanka&rearr-factors=market_wiz_offer_viewtype=list')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?clid=545&text=kiyanka&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=kiyanka&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                }
            },
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka&rearr-factors=market_wiz_offer_titleredir=1;market_wiz_offer_viewtype=abc'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?clid=545&cvredirect=2&from_yandex=1&text=kiyanka&viewtype=abc&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                }
            },
        )

        # без market_wiz_offer_titleredir нет ни cvredirect, ни viewtype
        response = self.report.request_bs_pb('place=parallel&text=kiyanka&rearr-factors=market_wiz_offer_viewtype=abc')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?clid=545&text=kiyanka&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                }
            },
        )

    def test_cvredirect_implicit_model(self):
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors=market_wiz_offer_titleredir=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&cvredirect=2&from_yandex=1&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&cvredirect=2&from_yandex=1&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards", ignore_len=False
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=panasonic%20tumix&cvredirect=2&from_yandex=1&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=panasonic%20tumix&cvredirect=2&from_yandex=1&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        },
                    ],
                }
            },
        )

    def test_viewtype_implicit_model(self):
        # при наличии market_wiz_offer_titleredir=1 и market_wiz_offer_viewtype добавляется параметр viewtype
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors=market_wiz_offer_titleredir=1;market_wiz_offer_viewtype=grid'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&cvredirect=2&from_yandex=1&viewtype=grid&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&cvredirect=2&from_yandex=1&viewtype=grid&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                }
            },
        )

        # без market_wiz_offer_titleredir=1 параметр добавляться не должен
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors=market_wiz_offer_viewtype=grid'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(
                        '//market.yandex.ru/search?text=panasonic%20tumix&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards',
                        ignore_len=False,
                        ignore_params=['rs'],
                    )
                }
            },
        )

    def test_implicit_model_with_different_categories(self):
        response = self.report.request_bs_pb('place=parallel&text=sony+xperia')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of("//market.yandex.ru/product--sony-xperia-z4/104?hid=32&clid=698"),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--sony-xperia-z4/104?hid=32&clid=721"
                                    ),
                                    "text": {"__hl": {"text": "sony xperia z4", "raw": True}},
                                }
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of("//market.yandex.ru/product--sony-xperia-z5/103?hid=31&clid=698"),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--sony-xperia-z5/103?hid=31&clid=721"
                                    ),
                                    "text": {"__hl": {"text": "sony xperia z5", "raw": True}},
                                }
                            },
                        ]
                    }
                }
            },
        )

    def test_implicit_model_short_query(self):
        response = self.report.request_bs_pb('place=parallel&text=tumix&rearr-factors=market_no_restrictions=1')
        self.assertFragmentIn(response, {"market_implicit_model": {}})

    def test_no_offer_incut_ctr_restriction(self):
        """Проверяем, что фильтрация оферов по CTR в оферной врезке (порог 0.001) отключена.
        https://st.yandex-team.ru/MARKETOUT-17772
        """
        # market_offers_wiz_top_offers_threshold=0.01 нужен, чтобы оферный не отфильтровывался по топ-4
        request = 'place=parallel&text=molot+molotok&rearr-factors=market_offers_incut_threshold=0.0;market_offers_wiz_top_offers_threshold=0;'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": NotEmptyList()}}})

    def test_plitka_relevance_threshold(self):
        """Проверяем, что если сумма значений формулы топ-4 оферов
        из оферной врезки меньше market_offers_incut_threshold,
        то врезка не показывается.
        """
        # выключаем флаг market_offers_incut_threshold_disable, ставим низкий порог на формирование оферного
        query = 'place=parallel&text=molotok\
                               &rearr-factors=market_offers_incut_threshold_disable=0;market_offers_wiz_top_offers_threshold=0;'
        response = self.report.request_bs_pb(query + 'market_offers_incut_threshold=0.25;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "molot-molotok-9", "raw": True}}}},  # 0.09
                            {"title": {"text": {"__hl": {"text": "molot-molotok-8", "raw": True}}}},  # 0.08
                            {"title": {"text": {"__hl": {"text": "molot-molotok-7", "raw": True}}}},  # 0.07
                            {
                                "title": {"text": {"__hl": {"text": "molot-molotok-6", "raw": True}}}
                            },  # 0.06 - top 4 sum = 0.3 > 0.25
                            {"title": {"text": {"__hl": {"text": "molot-molotok-5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-1", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_bs_pb(query + 'market_offers_incut_threshold=0.35;')
        self.assertFragmentIn(response, {"market_offers_wizard": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

    def test_plitka_relevance_default_threshold(self):
        """Проверяем дефолтные пороги для оферной врезки.
        Для device=desktop свое значение порога.
        """
        # выключаем флаг market_offers_incut_threshold_disable
        query = 'place=parallel&text=molotok&trace_wizard=1&rearr-factors=market_offers_incut_threshold_disable=0;'

        response = self.report.request_bs_pb(query)
        self.assertIn(
            'OffersWizard.TopOffersMnValue.Base.Threshold: market_offers_incut_threshold=-100',
            response.get_trace_wizard(),
        )
        response = self.report.request_bs_pb(query + 'device=desktop;')
        self.assertIn(
            'OffersWizard.TopOffersMnValue.Base.Threshold: market_offers_incut_threshold=-100',
            response.get_trace_wizard(),
        )

    def test_offers_wizard_top_offers_threshold(self):
        """Проверяем, что если сумма значений формулы топ-4 оферов
        из оферной врезки меньше market_offers_wiz_top_offers_threshold,
        то оферный колдунщик не строится.
        Также проверяем, что порог по показу оферной врезки не влияет
        на порог по показу оферного колдунщика.
        """
        # выключаем флаг market_offers_incut_threshold_disable
        query = 'place=parallel&text=molotok&rearr-factors=market_offers_incut_threshold_disable=0;'

        # проверяем работу порога
        response = self.report.request_bs_pb(query + 'market_offers_wiz_top_offers_threshold=0.25;')
        self.assertFragmentIn(response, {"market_offers_wizard": {}})
        response = self.report.request_bs_pb(query + 'market_offers_wiz_top_offers_threshold=0.35;')
        self.assertFragmentNotIn(response, {"market_offers_wizard": {}})

        # проверяем независимость порога от порога оферной врезки
        response = self.report.request_bs_pb(
            query + 'market_offers_wiz_top_offers_threshold=0.25;' 'market_offers_incut_threshold=0.25'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": NotEmptyList()}}})
        response = self.report.request_bs_pb(
            query + 'market_offers_wiz_top_offers_threshold=0.25;' 'market_offers_incut_threshold=0.35'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {}})

        # проверяем отсутсвие врезки
        self.assertFragmentIn(response, {"market_offers_wizard": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        response = self.report.request_bs_pb(
            query + 'market_offers_wiz_top_offers_threshold=0.35;' 'market_offers_incut_threshold=0.25'
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard": {}})

    def test_plitka_relevance_threshold_by_offer(self):
        """Проверяем что флаг market_offers_incut_mnvalue_threshold задает порог
        по которому отсекаются офферы для врезки имеющие mnValue ниже заданного значения
        порог работает только для врезки и офферный колдунщик будет сформирован
        даже если офферов для врезки оказалось недостаточно

        Есть флаг market_relevance_formula_threshold_on_parallel_for_offers
        Фильтрующий офферы на базовых
        @see test_relevance_formula_threshold_on_parallel_for_offers (test_filtering_by_relevance)
        """
        # market_offers_incut_threshold=0.25 нужен чтобы плитка не отфильтровывалась по дефолтному порогу
        # market_offers_wiz_top_offers_threshold=0 нужен, чтобы оферный не отфильтровывался по топ-4
        request = (
            'place=parallel&text=molotok&trace_wizard=1'
            '&rearr-factors=market_offers_incut_threshold=0.25;market_offers_wiz_top_offers_threshold=0;'
        )

        response = self.report.request_bs_pb(request + 'market_offers_incut_mnvalue_threshold=0.055;')
        self.assertIn(
            'OfferIncut.OneOfferMnValue.Base.Threshold: market_offers_incut_mnvalue_threshold=0.055',
            response.get_trace_wizard(),
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard_center_incut': {
                    "offer_count": 9,
                    # во врезке всего 3 оффера
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "molot-molotok-9", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-8", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-7", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # с увеличением порога врезка перестает формироваться
        # но это не влияет на офферынй колдунщик
        response = self.report.request_bs_pb(request + 'market_offers_incut_mnvalue_threshold=0.09;')
        self.assertIn(
            'OfferIncut.OneOfferMnValue.Base.Threshold: market_offers_incut_mnvalue_threshold=0.09',
            response.get_trace_wizard(),
        )
        self.assertFragmentIn(response, {'market_offers_wizard': {"offer_count": 9}}, preserve_order=True)
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

    def test_plitka_bid_threshold_by_offer_on_meta(self):
        """Проверяем что флаг market_offers_incut_bid_threshold задает порог
        по которому отсекаются офферы для врезки имеющие ставку ниже заданного значения
        отсечение происходит только на мете, поэтому даже при отсутствии врезки
        офферный колдунщик будет сформирован
        """
        # market_offers_wiz_top_offers_threshold=0 нужен, чтобы оферный не отфильтровывался по топ-4
        request = (
            'place=parallel&text=molotok&trace_wizard=1'
            '&rearr-factors=market_offers_incut_threshold=0.01;market_offers_wiz_top_offers_threshold=0;'
        )

        response = self.report.request_bs_pb(request + 'market_offers_incut_bid_threshold=124;')
        self.assertIn('Threshold by bid is enabled: 124', response.get_trace_wizard())
        # офферы упорядочены по убыванию релевантности
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "offer_count": 9,
                    # во врезке только 6 офферов
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "molot-molotok-6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-1", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        # офферы с низким bid отсечены (хотя они имеют высокую релевантность)
        self.assertIn('Bid: 121 less then bid threshold', response.get_trace_wizard())
        self.assertIn('Bid: 122 less then bid threshold', response.get_trace_wizard())
        self.assertIn('Bid: 123 less then bid threshold', response.get_trace_wizard())
        # bid 124 и выше дает право им попасть во врезку
        self.assertNotIn('Bid: 124 less then bid threshold', response.get_trace_wizard())

        # фильтрация по bid происходит только на мете и только для врезки
        # офферный колдунщик формируется даже если осталось недостаточное количество офферов для врезки
        response = self.report.request_bs_pb(request + 'market_offers_incut_bid_threshold=150;')
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "offer_count": 9,
                    # врезка отсутствует
                    "showcase": {"items": EmptyList()},
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_bid_threshold_foreoffer_on_parallel(self):
        """Проверяем что флаг market_parallel_bid_threshold задает порог
        по которому отсекаются офферы имеющие ставку ниже заданного значения
        отсечение происходит на базовом, поэтому этот флаг влияет на все колдунщики
        """
        # market_offers_wiz_top_offers_threshold=0.01 нужен, чтобы оферный не отфильтровывался по топ-4
        request = 'place=parallel&text=molotok&rearr-factors=market_offers_incut_threshold=0.01;market_offers_wiz_top_offers_threshold=0.01;'

        response = self.report.request_bs_pb(request + 'market_parallel_bid_threshold=124;')
        # офферы упорядочены по убыванию релевантности
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    # и во врезке и в самом колдунщике 6 офферов
                    "offer_count": 6,
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "molot-molotok-6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-1", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # фильтрация по bid происходит на базовых
        # офферный колдунщик не сформируется если не осталось офферов
        response = self.report.request_bs_pb(request + 'market_parallel_bid_threshold=150;')
        self.assertFragmentNotIn(response, {'market_offers_wizard': {}})

    def test_plitka_sort_by_bid(self):
        """Проверяем что флаг market_offers_incut_sort_by_bid=1 упорядочивает
        офферы для врезки в порядке убывания ставки
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=molotok&trace_wizard=1'
            '&rearr-factors=market_offers_incut_sort_by_bid=1;market_cpc_incut_fix_auction_sorting=0;'
            'market_offers_incut_threshold=0.01'
        )
        # офферы упорядочены по убыванию ставки
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "molot-molotok-1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-7", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-8", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "molot-molotok-9", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_parallel_sort_by_bid(self):
        """Проверяем что флаг market_parallel_sort_by_bid
        сортирует документы по ставке на базовых
        (при этом во врезке они сортируются по релевантности)
        Но вы можете также отсортировать их по bid
        c помощью флага market_offers_incut_sort_by_bid
        """

        # обычное состояние - отсортированы по уменьшению релевантности
        # с базовых также поступают наиболее релевантные документы
        response = self.report.request_bs_pb('place=parallel&text=kuvalda')
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "offer_count": 24,
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "kuvalda-24", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-23", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-22", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-21", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-20", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-19", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-18", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-17", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-16", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # офферы во врезке отсортированы по уменьшению релевантности
        # с базовых поступают не самые релевантные документы
        # а те у которых больше ставка
        response = self.report.request_bs_pb(
            'place=parallel&text=kuvalda'
            '&rearr-factors=market_parallel_sort_by_bid=1;market_cpc_incut_fix_auction_sorting=0;'
            'market_offers_incut_threshold=0.01'
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "offer_count": 24,
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "kuvalda-9", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-8", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-7", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-1", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # офферы во врезке отсортированы по уменьшению ставки
        # с базовых поступают не самые релевантные документы
        # а те у которых больше ставка
        response = self.report.request_bs_pb(
            'place=parallel&text=kuvalda'
            '&rearr-factors=market_parallel_sort_by_bid=1;market_cpc_incut_fix_auction_sorting=0;'
            ';market_offers_incut_sort_by_bid=1'
            ';market_offers_incut_threshold=0.01'
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "offer_count": 24,
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "kuvalda-1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-7", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-8", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "kuvalda-9", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_plitka_min_processed_offers(self):
        """Флаг market_offers_incut_min_process задает
        минимальное число обрабатываемых офферов для составления плитки
        (соответственно с базовых тоже запрашивается
        не менее данного числа офферов и не менее 20)
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=molotok&trace_wizard=1' '&rearr-factors=market_offers_incut_min_process=50;'
        )

        self.assertIn("Minimum processed items: 50", response.get_trace_wizard())

    @classmethod
    def prepare_offers_for_plitka_autobroker(cls):
        """Офферы должны быть доступны в одном из регионов россии
        Офферы различаются по релевантности и по ставке
        """
        pic = Picture(
            width=100, height=100, thumb_mask=thumbnails_config.get_mask_by_names(['100x100']), group_id=17670
        )

        for i in range(1, 10):
            ts = 17670 + i
            fesh = ts
            cls.index.shops += [Shop(fesh=fesh, priority_region=213, regions=[225])]
            cls.index.offers.append(
                Offer(
                    title='pasatizi-1{}'.format(i),
                    ts=ts,
                    fesh=fesh,
                    picture=pic,
                    picture_flags=ts,
                    bid=1000 - 100 * i,
                    url='https://test.pasatizishop.ru/offer/id1{}'.format(i),
                    waremd5='hLBp69KH1GfLf9JuU8agyQ' if i == 1 else None,
                )
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.3 + 0.05 * i)

        cls.turbo.fill_urls(
            {
                'https://test.pasatizishop.ru/offer/id11': '/turbo?text=https://test.pasatizishop.ru/offer/id11',
            }
        )

        # Документы с очень маленьким различием в mnValue
        # На них сильно влияет аукцион - и документы с большим bid получат
        # больший cpm на базовом, несмотря на меньшее значение mnValue
        # В автоброкер документы попадут в порядке, в котором они пришли с базового,
        # т.е. по уменьшению cpm а не по уменьшению mnValue
        # На выдаче документы уже после автоброкера будут пересортированы в порядке уменьшения mnValue
        for i in range(1, 5):
            ts = 17650 + i
            fesh = ts
            cls.index.shops += [Shop(fesh=fesh, priority_region=213, regions=[225])]
            cls.index.offers.append(
                Offer(
                    title='ploskogubcy-{}'.format(i), ts=ts, fesh=fesh, picture=pic, picture_flags=ts, bid=100 - 20 * i
                )
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.3 + 0.001 * i)

    def test_plitka_autobroker_default(self):
        """Проверяем цену клика на офферной врезке в дефолте
        Ожидаем: все документы находятся в одной collection priority группе (0)
        Цена клика для всех документов кроме последнего не равняется минставке (работает автоброкер)
        """
        click_prices1 = [30, 300, 1, 24, 21, 18, 13, 9, 1]

        for (min_bid_multiplier, prices) in [(1.0, click_prices1)]:
            response = self.report.request_bs(
                'place=parallel&text=pasatizi&rids=213&debug=da'
                '&rearr-factors=market_offers_wizard_incut_url_type=External;market_ha_min_bid_multiplier={}'.format(
                    min_bid_multiplier
                )
            )

            # и первый и последний документ находятся в группе 0
            self.assertFragmentIn(response, 'auctioned: 0 POSITION: 0')
            self.assertFragmentIn(response, 'auctioned: 0 POSITION: 8')
            # нет документов входящих в другие группы
            self.assertFragmentNotIn(response, 'auctioned: 1')

            self.assertFragmentIn(
                response,
                {
                    'market_offers_wizard': [
                        {
                            "offer_count": 9,
                            "showcase": {
                                "items": [
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-18", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=200/min_bid=1/'.format(prices[0])),
                                        }
                                    },
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-17", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=300/min_bid=1/'.format(prices[1])),
                                        }
                                    },
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-19", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=100/min_bid=1/'.format(prices[2])),
                                        }
                                    },
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-16", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=400/min_bid=1/'.format(prices[3])),
                                        }
                                    },
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-15", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=500/min_bid=1/'.format(prices[4])),
                                        }
                                    },
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-14", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=600/min_bid=1/'.format(prices[5])),
                                        }
                                    },
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-13", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=700/min_bid=1/'.format(prices[6])),
                                        }
                                    },
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-12", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=800/min_bid=1/'.format(prices[7])),
                                        }
                                    },
                                    {
                                        "title": {
                                            "text": {"__hl": {"text": "pasatizi-11", "raw": True}},
                                            "urlForCounter": Contains('/cp={}/cb=900/min_bid=1/'.format(prices[8])),
                                        }
                                    },
                                ]
                            },
                        }
                    ]
                },
                preserve_order=True,
                allow_different_len=True,
            )

    def test_plitka_autobroker_fees_and_bids_null(self):
        """
        Проверяем, что флаг market_set_fees_and_bids_null зануляет все ставки и fee
        Все bid и fee должны быть нулевыми
        """
        response = self.report.request_bs(
            'place=parallel&text=pasatizi&rids=213&debug=da'
            '&rearr-factors=market_offers_wizard_incut_url_type=External;market_ha_min_bid_multiplier=1;market_set_fees_and_bids_null=1'
        )
        # Use complicated captures as we should parse properties from "not-jsoned" string
        offer_cnt = 9  # Answer contain 9 docs
        cpm_capture = [Capture() for _ in range(offer_cnt)]
        auction_params_capture = [Capture() for _ in range(offer_cnt)]

        # All offers in response are CPC, for them bid should be equal to minbid
        assert_template = {
            "offerFactors": [
                {
                    "full_bid_info": "MinimalBid:1 RankingMinimalBid:1 ShopBid:1 VendorBid:0 MinimalVendorBid:0 RankingMinimalVendorBid:0 BidType:mbid Fee:0 MinimalFee:0 OfferVendorFee:0 ReservePriceFee:0 UeToPrice:0 NeedLocalBoost:0",  # noqa
                    "min_bid": 1,
                    "properties": {
                        "AUCTION_PARAMS": NotEmpty(capture=auction_params_capture[i]),
                        "BID": "1",
                        "CPM": NotEmpty(capture=cpm_capture[i]),
                        "DOCUMENT_AUCTION_TYPE": "CPC",
                    },
                }
                for i in range(offer_cnt)
            ]
        }

        self.assertFragmentIn(response, assert_template, preserve_order=True)

        for i in range(offer_cnt):
            # Find formula value via regexp, don't forget to use non-greedy mode
            # findall returns the list of "matches", each "match" is the tuple of the "bracket-captured" objects - we need the second object of the first (the only) "match"
            formula_value = re.findall(r"(FormulaValue:(\d*?\.?\d*?) )", auction_params_capture[i].value)[0][1]
            # CPM and scaled formula_value should be equal due to the null fees & bids (and another params like price are equal)
            self.assertAlmostEqual(int(cpm_capture[i].value), int(float(formula_value) * 100000), delta=100)
            # Check that bids and fees are nulls
            self.assertTrue("Fee:0" in auction_params_capture[i].value and "Bid:0" in auction_params_capture[i].value)

    def test_plitka_autobroker_default_not_fare(self):
        """Проверяем в каком порядке возвращаются документы с базового
        За счет ставок документы с меньшим mnValue получают больший cpm на базовом
        Автоброкер вычисляет цену клика исходя из порядка документов на базовом
        И только затем документы пересортировываются на выдаче в порядке релевантности
        """

        cpm1 = (1.0, [(1.108767986, 33373), (1.079052329, 32587), (1.050534368, 31831), (1.023671389, 31119)])

        for min_bid_multiplier, cpm in [cpm1]:
            response = self.report.request_bs(
                'place=parallel&text=ploskogubcy&rids=213&debug=da&debug-doc-count=10'
                '&rearr-factors=market_offers_wizard_incut_url_type=External;market_ha_min_bid_multiplier={}'.format(
                    min_bid_multiplier
                )
            )
            debug = response.extract_debug_response()

            self.assertFragmentIn(
                debug,
                '''
                <document>
                    <properties>
                        <TS value="17651" />
                        <BID value="80" />
                        <MATRIXNET_VALUE value="0.300999999" />
                        <AUCTION_MULTIPLIER value="{}" />
                        <CPM value="{}" />
                    </properties>
                </document>
            '''.format(
                    cpm[0][0], cpm[0][1]
                ),
            )

            self.assertFragmentIn(
                debug,
                '''
                <document>
                    <properties>
                        <TS value="17652" />
                        <BID value="60" />
                        <MATRIXNET_VALUE value="0.3019999862" />
                        <AUCTION_MULTIPLIER value="{}" />
                        <CPM value="{}" />
                    </properties>
                </document>
            '''.format(
                    cpm[1][0], cpm[1][1]
                ),
            )

            self.assertFragmentIn(
                debug,
                '''
                <document>
                    <properties>
                        <TS value="17653" />
                        <BID value="40" />
                        <MATRIXNET_VALUE value="0.3030000031" />
                        <AUCTION_MULTIPLIER value="{}" />
                        <CPM value="{}" />
                    </properties>
                </document>
            '''.format(
                    cpm[2][0], cpm[2][1]
                ),
            )

            self.assertFragmentIn(
                debug,
                '''
                <document>
                    <properties>
                        <TS value="17654" />
                        <BID value="20" />
                        <MATRIXNET_VALUE value="0.3039999902" />
                        <AUCTION_MULTIPLIER value="{}" />
                        <CPM value="{}" />
                    </properties>
                </document>
            '''.format(
                    cpm[3][0], cpm[3][1]
                ),
            )

            # автоброкер торгует документы в порядке 1, 2, 3, 4 (по cpm с базового)
            # на выдаче документы в порядке уменьшения mn_value (4, 3, 2, 1)
            self.assertFragmentIn(
                response,
                {
                    'market_offers_wizard': [
                        {
                            "offer_count": 4,
                            "showcase": {
                                "items": [
                                    {
                                        "title": {  # последний документ на стадии автоброкера - снимается минставка
                                            "text": {"__hl": {"text": "ploskogubcy-1", "raw": True}},
                                            "urlForCounter": Contains('/cp=64/cb=80/min_bid=1/'),
                                        }
                                    },
                                    {
                                        "title": {  # подпирается документом ploskogubcy-4: click_price > сb(ploskogubcy-4)
                                            "text": {"__hl": {"text": "ploskogubcy-2", "raw": True}},
                                            "urlForCounter": Contains('/cp=42/cb=60/min_bid=1/'),
                                        }
                                    },
                                    {
                                        "title": {  # подпирается документом ploskogubcy-3: click_price > сb(ploskogubcy-3)
                                            "text": {"__hl": {"text": "ploskogubcy-3", "raw": True}},
                                            "urlForCounter": Contains('/cp=22/cb=40/min_bid=1/'),
                                        }
                                    },
                                ]
                            },
                        }
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_plitka_autobroker_next_bid_plus_one(self):
        """Проверяем цену клика на офферной врезке при сортировке по ставке
        Ожидаем:
        Документы отсортированы по уменьшению ставки
        Все документы находятся в одной collection priority группе (0)
        Цена клика равна ставке предыдущего документа + 1
        """
        response = self.report.request_bs(
            'place=parallel&text=pasatizi&rids=213&debug=da'
            '&rearr-factors=market_offers_incut_sort_by_bid=1;market_offers_wizard_incut_url_type=External'
        )

        # и первый и последний документ находятся в группе 0
        self.assertFragmentIn(response, 'auctioned: 0 POSITION: 0')
        self.assertFragmentIn(response, 'auctioned: 0 POSITION: 8')
        # нет документов входящих в другие группы
        self.assertFragmentNotIn(response, 'auctioned: 1')

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': [
                    {
                        "offer_count": 9,
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-11", "raw": True}},
                                        "urlForCounter": Contains('/cp=801/cb=900/min_bid=1/'),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-12", "raw": True}},
                                        "urlForCounter": Contains('/cp=701/cb=800/min_bid=1/'),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-13", "raw": True}},
                                        "urlForCounter": Contains('/cp=601/cb=700/min_bid=1/'),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-14", "raw": True}},
                                        "urlForCounter": Contains('/cp=501/cb=600/min_bid=1/'),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-15", "raw": True}},
                                        "urlForCounter": Contains('/cp=401/cb=500/min_bid=1/'),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-16", "raw": True}},
                                        "urlForCounter": Contains('/cp=301/cb=400/min_bid=1/'),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-17", "raw": True}},
                                        "urlForCounter": Contains('/cp=201/cb=300/min_bid=1/'),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-18", "raw": True}},
                                        "urlForCounter": Contains('/cp=101/cb=200/min_bid=1/'),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pasatizi-19", "raw": True}},
                                        "urlForCounter": Contains('/cp=1/cb=100/min_bid=1/'),
                                    }
                                },
                            ]
                        },
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_plitka_sort_by_cpm(self):
        """Офферы в офферной врезке упорядочены по mnValue * bid
        Цена клика не равна минимальной ставке (работает автоброкер)
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=pasatizi&trace_wizard=1&rids=213&debug=da'
            '&rearr-factors=market_offers_incut_sort_by_cpm=1;market_offers_wizard_incut_url_type=External'
        )

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "offer_count": 9,
                    "showcase": {
                        "items": [
                            {
                                "title": {  # 0.40 * 800 = 320   0.40 * 788 = 315.2
                                    "text": {"__hl": {"text": "pasatizi-12", "raw": True}},
                                    "urlForCounter": Contains('/cp=788/cb=800/min_bid=1/'),
                                }
                            },
                            {
                                "title": {  # 0.45 * 700 = 315
                                    "text": {"__hl": {"text": "pasatizi-13", "raw": True}},
                                    "urlForCounter": Contains('/cp=700/cb=700/min_bid=1/'),
                                }
                            },
                            {
                                "title": {  # 0.35 * 900 = 315    0.35 * 858 = 300.3
                                    "text": {"__hl": {"text": "pasatizi-11", "raw": True}},
                                    "urlForCounter": Contains('/cp=858/cb=900/min_bid=1/'),
                                }
                            },
                            {
                                "title": {  # 0.5 * 600 = 300   0.5 * 551 = 275.5
                                    "text": {"__hl": {"text": "pasatizi-14", "raw": True}},
                                    "urlForCounter": Contains('/cp=551/cb=600/min_bid=1/'),
                                }
                            },
                            {
                                "title": {  # 0.55 * 500 = 275
                                    "text": {"__hl": {"text": "pasatizi-15", "raw": True}},
                                    "urlForCounter": Contains('/cp=437/cb=500/min_bid=1/'),
                                }
                            },
                            {
                                "title": {  # 0.6 * 400 = 240
                                    "text": {"__hl": {"text": "pasatizi-16", "raw": True}},
                                    "urlForCounter": Contains('/cp=325/cb=400/min_bid=1/'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "pasatizi-17", "raw": True}},
                                    "urlForCounter": Contains('/cp=216/cb=300/min_bid=1/'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "pasatizi-18", "raw": True}},
                                    "urlForCounter": Contains('/cp=108/cb=200/min_bid=1/'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "pasatizi-19", "raw": True}},
                                    "urlForCounter": Contains('/cp=1/cb=100/min_bid=1/'),
                                }
                            },
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_newby_implicit_model(self):
        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix&currency=BYN')
        self.assertFragmentIn(
            response,
            {"market_implicit_model": {"showcase": {"items": [{"price": {"priceMin": "23.55", "currency": "BYN"}}]}}},
        )

    def test_universal_wizard_highlighting(self):
        # test adding __hl key for response text highlighting via SerpData
        response = self.report.request_bs_pb('place=parallel&text=pepelac&ignore-mn=1&rids=213')
        self.assertFragmentIn(response, {"title": {"__hl": NotEmpty()}, "text": [{"__hl": NotEmpty()}]})

    def test_model_wizard_filter_by_main(self):
        response = self.report.request_bs_pb('place=parallel&text=pepelac+2000&ignore-mn=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {
                                    "text": "SHOP-12",
                                },
                                "price": {
                                    "priceMax": "888",
                                },
                            },
                            {
                                "price": {
                                    "priceMax": "888",
                                }
                            },
                        ]
                    }
                }
            },
            preserve_order=False,
        )

        log_content = ['"wiz_id":"market_model"', '"models":["106"]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

    def test_tires_wizard(self):
        had_requests = self.reqwizard.counter.overall()
        response = self.report.request_bs(
            "place=parallel&text=shini+na+skoda&rearr-factors=market_parallel_wizard=1;market_parallel_wprids=1&askreqwizard=1&wprid=user_wprid"
        )
        self.assertEqual(self.reqwizard.counter.overall(), had_requests + 1)
        self.assertFragmentIn(
            response,
            {
                "market_tires": [
                    {
                        "url": LikeUrl.of(
                            "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&wprid=user_wprid"
                        ),
                        "title": {
                            "title": "shini na skoda на Маркете",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&wprid=user_wprid"
                            ),
                        },
                        "body": NotEmpty(),
                        "green_url": LikeUrl.of(
                            "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&wprid=user_wprid"
                        ),
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_tires": [
                    {
                        "sitelinksWithCount": "none",
                        "sitelinks": [
                            {
                                "sitelink_title": "Octavia",
                                "sitelink_url-touch": LikeUrl.of(
                                    "//m.market.yandex.ru/catalog?clid=821&hid=90490&mark=Skoda&model=Octavia&nid=54469&wprid=user_wprid"
                                ),
                                "sitelink_url": LikeUrl.of(
                                    "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&model=Octavia&wprid=user_wprid"
                                ),
                            },
                            {
                                "sitelink_url": LikeUrl.of(
                                    "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&model=Yeti&wprid=user_wprid"
                                ),
                                "sitelink_title": "Yeti",
                                "sitelink_url-touch": LikeUrl.of(
                                    "//m.market.yandex.ru/catalog?clid=821&hid=90490&mark=Skoda&model=Yeti&nid=54469&wprid=user_wprid"
                                ),
                            },
                            {
                                "sitelink_url": LikeUrl.of(
                                    "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&model=Fabia&wprid=user_wprid"
                                ),
                                "sitelink_title": "Fabia",
                                "sitelink_url-touch": LikeUrl.of(
                                    "//m.market.yandex.ru/catalog?clid=821&hid=90490&mark=Skoda&model=Fabia&nid=54469&wprid=user_wprid"
                                ),
                            },
                        ],
                    }
                ]
            },
        )

        had_requests = self.reqwizard.counter.overall()
        response = self.report.request_bs(
            "place=parallel&text=shini+na+skoda+octavia&rearr-factors=market_parallel_wizard=1&askreqwizard=1"
        )
        self.assertEqual(self.reqwizard.counter.overall(), had_requests + 1)
        self.assertFragmentIn(
            response,
            {
                "market_tires": [
                    {
                        "url": LikeUrl.of(
                            "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&model=Octavia"
                        ),
                        "title": {
                            "title": "shini na skoda octavia на Маркете",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&model=Octavia"
                            ),
                        },
                        "body": NotEmpty(),
                        "green_url": LikeUrl.of(
                            "//market.yandex.ru/catalog/54469?clid=820&hid=90490&mark=Skoda&model=Octavia"
                        ),
                        "sitelinks": [],
                    }
                ]
            },
        )

        self.error_log.ignore(code=3021)

    def test_plitka_incut_log(self):
        response = self.report.request_bs(
            "place=parallel&text=pelmen&rearr-factors=market_offers_wizard_incut_url_type=External"
        )

        self.assertFragmentIn(
            response,
            {
                "showcase": {
                    "items": [
                        {
                            "title": {
                                "url": "http://pelmennaya.ru/pelmens?id=1",
                                "urlTouch": "http://pelmennaya.ru/pelmens?id=1",
                                "text": {"__hl": {"text": "pelmen-1", "raw": True}},
                            },
                            "greenUrl": {
                                "text": "SHOP-400",
                                "url": "//market.yandex.ru/shop--shop-400/400/reviews?cmid=09lEaAKkQll1XTjm0WPoIA&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545",
                                "urlTouch": "//m.market.yandex.ru/grades-shop.xml?shop_id=400&cmid=09lEaAKkQll1XTjm0WPoIA&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708",
                            },
                        },
                        {
                            "title": {
                                "url": "http://pelmennaya.ru/pelmens?id=2",
                                "urlTouch": "http://pelmennaya.ru/pelmens?id=2",
                                "text": {"__hl": {"text": "pelmen-2", "raw": True}},
                            },
                            "greenUrl": {
                                "text": "SHOP-401",
                                "url": "//market.yandex.ru/shop--shop-401/401/reviews?cmid=xMpCOKC5I4INzFCab3WEmQ&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545",
                                "urlTouch": "//m.market.yandex.ru/grades-shop.xml?shop_id=401&cmid=xMpCOKC5I4INzFCab3WEmQ&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708",
                            },
                        },
                        {
                            "title": {
                                "url": "http://pelmennaya.ru/pelmens?id=3",
                                "urlTouch": "http://pelmennaya.ru/pelmens?id=3",
                                "text": {"__hl": {"text": "pelmen-3", "raw": True}},
                            },
                            "greenUrl": {
                                "text": "SHOP-402",
                                "url": "//market.yandex.ru/shop--shop-402/402/reviews?cmid=lpc2G9gcBPtOqJHWMQSlow&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545",
                                "urlTouch": "//m.market.yandex.ru/grades-shop.xml?shop_id=402&cmid=lpc2G9gcBPtOqJHWMQSlow&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708",
                            },
                        },
                    ]
                }
            },
        )

        # урл оффера логгируется
        show_block_id = "048841920011177788888"
        base_show_uid = "{}{:02}{:03}".format(show_block_id, ClickType.EXTERNAL, 1)

        self.show_log.expect(
            url=LikeUrl.of('http://pelmennaya.ru/pelmens?id=1'),
            show_block_id=show_block_id,
            show_uid=base_show_uid,
            title="pelmen-1",
            ware_md5='09lEaAKkQll1XTjm0WPoIA',
        )

        self.click_log.expect(
            data_url=LikeUrl.of('http://pelmennaya.ru/pelmens?id=1', unquote=True),
            ware_md5='09lEaAKkQll1XTjm0WPoIA',
            dtype='market',
        )

    def test_offers_wizard_urls(self):
        """
        Проверка корректного значения url и green_url в колдунщике market_offers_wizard
        """
        response = self.report.request_bs_pb('place=parallel&text=kiyanka')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=kiyanka&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=kiyanka&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards", ignore_len=False
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=kiyanka&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=kiyanka&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                    ],
                }
            },
        )

    def test_model_wizard_urls(self):
        """
        Проверка корректного значения
        url, url-touch, url_analogues, url_touch_analogues и green_url
        в колдунщике market_model
        """
        response = self.report.request_bs_pb('place=parallel&text=pepelac+2000&ignore-mn=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/product--pepelac-2000/106?text=pepelac%202000&clid=502&hid=30&nid=10&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--pepelac-2000/106?text=pepelac%202000&clid=704&hid=30&nid=10&lr=213&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=502&lr=213&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        }
                    ],
                }
            },
        )

    def test_ext_category_wizard_urls(self):
        """
        Проверка корректного значения url, urlTouch и greenUrl в колдунщике market_ext_category
        """
        response = self.report.request_bs_pb("place=parallel&text=food")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/catalog--food/100?hid=100&clid=500&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/catalog?hid=100&nid=100&clid=707&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=500&lr=0&utm_medium=cpc&utm_referrer=wizards", ignore_len=False
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=707&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/catalog--food/100?hid=100&clid=500&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/catalog?hid=100&nid=100&clid=707&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "text": "Food",
                        },
                    ],
                }
            },
        )

    def test_ext_category_wizard_pictures(self):
        """
        Проверка корректного значения source и retinaSource в колдунщике market_ext_category
        """
        response = self.report.request_bs_pb("place=parallel&text=food")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "source": LikeUrl.of("//mdata.yandex.net/i?path=vendor_10_in_hid_100.jpg&size=2"),
                                    "retinaSource": LikeUrl.of(
                                        "//mdata.yandex.net/i?path=vendor_10_in_hid_100.jpg&size=5"
                                    ),
                                }
                            },
                            {
                                "thumb": {
                                    "source": LikeUrl.of("//mdata.yandex.net/i?path=vendor_11_in_hid_100.jpg&size=2"),
                                    "retinaSource": LikeUrl.of(
                                        "//mdata.yandex.net/i?path=vendor_11_in_hid_100.jpg&size=5"
                                    ),
                                }
                            },
                            {
                                "thumb": {
                                    "source": LikeUrl.of("//mdata.yandex.net/i?path=vendor_12_in_hid_100.jpg&size=2"),
                                    "retinaSource": LikeUrl.of(
                                        "//mdata.yandex.net/i?path=vendor_12_in_hid_100.jpg&size=5"
                                    ),
                                }
                            },
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb("place=parallel&text=good")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "source": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/hid_111/vendor_110/2hq"),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/hid_111/vendor_110/5hq"
                                    ),
                                }
                            },
                            {
                                "thumb": {
                                    "source": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/hid_111/vendor_111/2hq"),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/hid_111/vendor_111/5hq"
                                    ),
                                }
                            },
                            {
                                "thumb": {
                                    "source": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/hid_111/vendor_112/2hq"),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/hid_111/vendor_112/5hq"
                                    ),
                                }
                            },
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_clusters_in_model_wizard_experiment(cls):
        cls.index.hypertree += [HyperCategory(hid=140, name='Coats', visual=True)]

        cls.index.navtree += [NavCategory(nid=150, hid=140)]

        cls.index.gltypes += [
            GLType(param_id=600100, hid=140, position=1, cluster_filter=1, gltype=GLType.BOOL, name=u'Капюшон'),
            GLType(
                param_id=600101,
                hid=140,
                position=2,
                cluster_filter=1,
                gltype=GLType.ENUM,
                name=u'Сезон',
                values=[GLValue(value_id=1010, text='весна')],
            ),
            GLType(
                param_id=600102,
                hid=140,
                position=3,
                cluster_filter=1,
                gltype=GLType.ENUM,
                name=u'Цвет',
                subtype='color',
                values=[
                    GLValue(2010, code='#FF0000', tag='red', text='красный'),
                    GLValue(2011, code='#00FF00', tag='green', text='зелёный'),
                ],
            ),
            GLType(
                param_id=600103,
                hid=140,
                position=4,
                cluster_filter=1,
                gltype=GLType.ENUM,
                subtype='size',
                name=u'Размер',
                unit_param_id=600104,
                values=[
                    GLValue(value_id=1, text='42', unit_value_id=1),
                    GLValue(value_id=2, text='44', unit_value_id=1),
                    GLValue(value_id=3, text='46', unit_value_id=1),
                    GLValue(value_id=4, text='36', unit_value_id=2),
                    GLValue(value_id=5, text='38', unit_value_id=2),
                    GLValue(value_id=6, text='40', unit_value_id=2),
                ],
            ),
            GLType(
                param_id=600104,
                hid=140,
                position=None,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='RU', default=True), GLValue(value_id=2, text='EU')],
            ),
        ]

        cls.index.vclusters += [
            VCluster(
                vclusterid=1000000013,
                title='Coat Quelle',
                hid=140,
                pictures=[
                    Picture(
                        picture_id='tiSs0dxrqgAIXqUICMZUOQ',
                        width=180,
                        height=240,
                        thumb_mask=thumbnails_config.get_mask_by_names(
                            ['100x100', '90x120', '55x70', '180x240', '120x160']
                        ),
                        group_id=1234,
                    )
                ],
                glparams=[
                    GLParam(param_id=600100, value=1),  # с капюшоном
                    GLParam(param_id=600101, value=1010),  # сезон: весна
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=1,
                title='Coat Quelle',
                vclusterid=1000000013,
                price=10500,
                glparams=[GLParam(param_id=600102, value=2010), GLParam(param_id=600103, value=1)],  # red  # размер 42
            ),
            Offer(
                fesh=1,
                title='Coat Quelle',
                vclusterid=1000000013,
                price=11000,
                glparams=[
                    GLParam(param_id=600102, value=2011),  # green
                    GLParam(param_id=600103, value=3),  # размер 46
                ],
            ),
        ]

    def test_clusters_in_model_wizard_experiment(self):
        """Эксперимент с показом кластеров в модельном колдунщике.
        Включается флагом market_clusters_in_model_wizard=1.
        Также проверяем, что врезка оферов для кластеров включается флагом market_offers_when_clusters_in_model_wizard=1.

        https://st.yandex-team.ru/MARKETOUT-10117
        https://st.yandex-team.ru/MARKETOUT-28699
        """

        # отключаем схлопнутые из оферов модели
        request = 'place=parallel&text=coat+quelle&ignore-mn=1&rids=213&rearr-factors=market_parallel_use_collapsing=0;'

        response = self.report.request_bs_pb(request)
        self.assertFragmentNotIn(response, {"market_model": NotEmpty()})

        response = self.report.request_bs_pb(request + 'market_clusters_in_model_wizard=1;')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "categoryId": 140,
                    "category_name": "Coats",
                    "url": LikeUrl.of("//market.yandex.ru/product--coat-quelle/1000000013?clid=502&hid=140&nid=150"),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--coat-quelle/1000000013?clid=704&hid=140&nid=150"
                    ),
                    "title": {"__hl": {"raw": True, "text": "Coat Quelle"}},
                    "offersUrl": LikeUrl.of(
                        "//market.yandex.ru/product--coat-quelle/1000000013/offers?clid=502&grhow=shop&hid=140&hyperid=1000000013&nid=150"
                    ),
                    "offersUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--coat-quelle/1000000013?clid=704&grhow=shop&hid=140&nid=150"
                    ),
                    "picture": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_tiSs0dxrqgAIXqUICMZUOQ/100x100"
                    ),
                    "pictureTouch": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_tiSs0dxrqgAIXqUICMZUOQ/90x120"
                    ),
                    "pictureTouchHd": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_tiSs0dxrqgAIXqUICMZUOQ/180x240"
                    ),
                    "price": {"priceMin": "10500", "priceMax": "11000", "currency": "RUR"},
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=502"),
                        }
                    ],
                    "sitelinks": [
                        {
                            "text": "prices",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--coat-quelle/1000000013/offers?clid=502&grhow=shop&hid=140&hyperid=1000000013&nid=150"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--coat-quelle/1000000013?clid=704&grhow=shop&hid=140&nid=150"
                            ),
                            "hint": "2",
                        }
                    ],
                }
            },
        )

        # Проверяем, что нет офферной врезки
        self.assertFragmentNotIn(response, {"market_model": {"showcase": {"items": NotEmptyList()}}})

        # С флагом market_offers_when_clusters_in_model_wizard=1 оферная врезка есть
        response = self.report.request_bs_pb(
            request + 'market_clusters_in_model_wizard=1;market_offers_when_clusters_in_model_wizard=1'
        )
        self.assertFragmentIn(response, {"market_model": {"showcase": {"items": NotEmptyList()}}})

    @classmethod
    def prepare_clusters_in_implicit_model_wizard(cls):
        cls.index.vclusters += [
            VCluster(vclusterid=1000000014, title='Jacket Asos'),
            VCluster(vclusterid=1000000015, title='Jacket Burton'),
        ]
        cls.index.offers += [Offer(vclusterid=1000000014), Offer(vclusterid=1000000015)]

    def test_clusters_in_implicit_model_wizard(self):
        """Проверяем, что в эксперименте с показом кластеров в модельном колдунщике
        кластера не пролазят в колдунщик неявной модели.
        Но с включенным market_clusters_in_implicit_model начинают показываться.

        https://st.yandex-team.ru/MARKETOUT-10117
        https://st.yandex-team.ru/MARKETOUT-28699
        """

        request = 'place=parallel&text=jacket&ignore-mn=1&rearr-factors=market_clusters_in_model_wizard=1;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentNotIn(response, {"market_implicit_model": NotEmpty()})

        response = self.report.request_bs_pb(request + 'market_clusters_in_implicit_model=1')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Jacket Asos", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Jacket Burton", "raw": True}}}},
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_empty_thumb_data(cls):
        '''Создаем документ с пустыми картинками'''
        cls.index.models += [
            Model(title='empty_data', hyperid=1224, picinfo='', add_picinfo=''),
        ]

    def test_empty_thumb(self):
        '''Проверяем, что для модели с пустой картинкой
        репорт не падает'''
        response = self.report.request_bs_pb('place=parallel&text=empty_data')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "for_wiz": Absent(),
                    "pic_src": Absent(),
                    "pic_src-touch": Absent(),
                    "pic_src-hd-touch": Absent(),
                    "big_pic": Absent(),
                }
            },
        )

    @classmethod
    def prepare_offers_geo_and_text(cls):
        cls.index.offers += [
            Offer(title="GeoTextOffer for geo and text test", fesh=1),
        ]

    def test_offers_geo_and_text(self):
        response = self.report.request_bs_pb('place=parallel&text=GeoTextOffer&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "text": [
                        {
                            "__hl": {
                                "text": "1 магазин. Выбор по параметрам. Доставка из магазинов Москвы и других регионов.",
                                "raw": True,
                            }
                        }
                    ],
                    "geo": {
                        "title": "Адреса магазинов в Москве",
                        "url": LikeUrl.of("//market.yandex.ru/geo?clid=545&text=geotextoffer"),
                        "url-touch": LikeUrl.of("//m.market.yandex.ru/search?clid=708&text=geotextoffer"),
                    },
                }
            },
        )

    @classmethod
    def prepare_offerincut(cls):
        """
        Создаем 5 офферов которые попадут в топ6 модели и 2 оффера, которые не попадут
        """
        cls.index.cpa_categories += [
            CpaCategory(hid=200, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]
        cls.index.models += [
            Model(
                hyperid=301090,
                hid=200,
                title="lenovo p780",
                opinion=Opinion(rating=3.5, total_count=100, forum=5, reviews=10),
            ),
        ]
        cls.index.navtree += [
            NavCategory(nid=2000, hid=200),
        ]
        cls.index.offers += [
            Offer(
                title='lenovo p780 cpa offer 1',
                hid=200,
                hyperid=301090,
                fesh=1,
                bid=100,
                price=8000,
                waremd5='H__rIZIhXqNM4Kq2NlBTSg',
                url="http://www.shop-1.ru/lenovo_p780_offer1",
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=2, order_before=23)],
            ),
            Offer(
                title='lenovo p780 cpa offer 2',
                hid=200,
                hyperid=301090,
                fesh=2,
                bid=90,
                price=8000,
                waremd5='RxxQrRoVbXJW7d_XR9d5MQ',
                url="http://www.shop-2.ru/lenovo_p780_offer2",
            ),
            Offer(
                title='lenovo p780 cpa offer 3',
                hid=200,
                hyperid=301090,
                fesh=3,
                bid=80,
                price=8000,
                waremd5='1ZSxqUW11kXlniH4i9LYOw',
                url="http://www.shop-3.ru/lenovo_p780_offer3",
            ),
            Offer(
                title='lenovo p780 cpa offer 4',
                hid=200,
                hyperid=301090,
                fesh=4,
                bid=70,
                price=8000,
                waremd5='SlM1kXY9-nQ6E6_6sahXkw',
                url="http://www.shop-4.ru/lenovo_p780_offer4",
            ),
            Offer(
                title='lenovo p780 cpa offer 5',
                hid=200,
                hyperid=301090,
                fesh=5,
                bid=60,
                price=8000,
                waremd5='ZD3nz3unacdGbMvErf1_rA',
                url="http://www.shop-5.ru/lenovo_p780_offer5",
            ),
            Offer(
                title='lenovo p780 cpc offer 1',
                hid=200,
                hyperid=301090,
                fesh=6,
                bid=10,
                price=8000,
                waremd5='5RNatoyv6c_AyxpGFgp1og',
            ),
            Offer(
                title='lenovo p780 cpc offer 2',
                hid=200,
                hyperid=301090,
                fesh=7,
                bid=1000,
                price=8000,
                waremd5='tsW-9atNBM2fCzqVZD3Efw',
            ),
        ]
        cls.index.regional_models += [RegionalModel(hyperid=301090, rids=[213], offers=100, geo_offers=20)]

    def test_offers_in_offersincut(self):
        """
        Проверка отображения корректных офферов в оферной врезке колдунщика market_model
        """
        response = self.report.request_bs_pb('place=parallel&text=lenovo p780&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"raw": True, "text": "lenovo p780 cpa offer 1"}}}},
                            {"title": {"text": {"__hl": {"raw": True, "text": "lenovo p780 cpa offer 2"}}}},
                            {"title": {"text": {"__hl": {"raw": True, "text": "lenovo p780 cpa offer 3"}}}},
                            {"title": {"text": {"__hl": {"raw": True, "text": "lenovo p780 cpa offer 4"}}}},
                            {"title": {"text": {"__hl": {"raw": True, "text": "lenovo p780 cpa offer 5"}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"raw": True, "text": "lenovo p780 cpc offer 1"}}}},
                            {"title": {"text": {"__hl": {"raw": True, "text": "lenovo p780 cpc offer 2"}}}},
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_offer_info_in_offerincut(cls):
        cls.index.models += [
            Model(hyperid=301091, hid=201, title="Самокат Молния"),
        ]
        cls.index.shops += [
            Shop(
                fesh=15,
                name='Самокат',
                shop_grades_count=15,
                priority_region=213,
                url='samokat-molniya.ru',
                regions=[225],
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=4.0, rec_and_nonrec_pub_count=123),
            ),
        ]
        cls.index.offers += [
            Offer(
                title='Супер самокат Молния',
                hid=201,
                hyperid=301091,
                fesh=15,
                cpa=Offer.CPA_REAL,
                fee=200,
                price=8000,
                delivery_options=[DeliveryOption(price=200, day_from=0, day_to=2, order_before=23)],
                url='https://www.samokat-molniya.ru/molniya_1',
                waremd5='wf9fCDXkniqrpGHVkgj-6w',
                picture=Picture(group_id=301091, picture_id='samokat_pict', width=200, height=200),
            ),
        ]

    def test_offer_info_in_offerincut(self):
        """
        Проверка корректного отображения информации об оффере в офферной врезке
        """

        # Под конструктором
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_bs_pb(
            'place=parallel&text=самокат молния&rids=213&rearr-factors=showcase_universal_model=1' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {
                                    "text": "Самокат",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/shop--samokat/15/reviews?cmid=wf9fCDXkniqrpGHVkgj-6w&clid=502"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/grades-shop.xml?shop_id=15&cmid=wf9fCDXkniqrpGHVkgj-6w&clid=704"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/shop--samokat/15/reviews?cmid=wf9fCDXkniqrpGHVkgj-6w&clid=914"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/grades-shop.xml?shop_id=15&cmid=wf9fCDXkniqrpGHVkgj-6w&clid=920"
                                    ),
                                },
                                "title": {
                                    "text": {"__hl": {"text": "Супер самокат Молния", "raw": True}},
                                    "url": LikeUrl.of("https://www.samokat-molniya.ru/molniya_1"),
                                    "urlTouch": LikeUrl.of("https://www.samokat-molniya.ru/molniya_1"),
                                    "adGUrl": LikeUrl.of("https://www.samokat-molniya.ru/molniya_1"),
                                    "adGUrlTouch": LikeUrl.of("https://www.samokat-molniya.ru/molniya_1"),
                                    "directOffercardUrl": LikeUrl.of(
                                        "//market.yandex.ru/offer/wf9fCDXkniqrpGHVkgj-6w?clid=914&hid=201&hyperid=301091&lr=213&modelid=301091&rs=eJyz4uSYwizEIMGoxAAACPMBMA%2C%2C"
                                    ),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                },
                                "thumb": {
                                    "source": "//avatars.mdst.yandex.net/get-marketpic/301091/market_samokat_pict/100x100",
                                    "width": "100",
                                    "height": "100",
                                    "retinaSource": "//avatars.mdst.yandex.net/get-marketpic/301091/market_samokat_pict/200x200",
                                    "url": LikeUrl.of("https://www.samokat-molniya.ru/molniya_1"),
                                    "urlTouch": LikeUrl.of("https://www.samokat-molniya.ru/molniya_1"),
                                    "adGUrl": LikeUrl.of("https://www.samokat-molniya.ru/molniya_1"),
                                    "adGUrlTouch": LikeUrl.of("https://www.samokat-molniya.ru/molniya_1"),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                },
                                "delivery": {
                                    "price": "200",
                                    "currency": "RUR",
                                },
                                "gradeCount": 123,
                                "price": {
                                    "type": "average",
                                    "currency": "RUR",
                                    "priceMin": Absent(),
                                    "priceMax": "8000",
                                },
                                "shopRating": {"value": "4"},
                            }
                        ]
                    }
                }
            },
        )

    def test_offers_urls_in_market_model(self):
        """
        Проверка корретных url-ов offers-url, offers-url-aprice, offers-url-touch, offers-url-aprice-touch
        """
        response = self.report.request_bs_pb('place=parallel&text=lenovo p780&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"raw": True, "text": "Lenovo p780"}},
                    "offersUrl": LikeUrl.of(
                        "//market.yandex.ru/product--lenovo-p780/301090/offers?clid=502&grhow=shop&hid=200&hyperid=301090&nid=2000"
                    ),
                    "offersUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--lenovo-p780/301090?clid=704&grhow=shop&hid=200&nid=2000"
                    ),
                }
            },
        )

    def test_market_sitelinks_in_market_model(self):
        """
        Проверка блока market_sitelinks в колдунщике market_model
        """
        response = self.report.request_bs_pb('place=parallel&text=lenovo p780&rids=213')
        self.assertFragmentIn(
            response,
            {
                "sitelinksWithCount": "several",
                "sitelinks": [
                    {
                        "text": "specs",
                        "adGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090/spec?hid=200&nid=2000&text=lenovo%20p780&clid=920&lr=213"
                        ),
                        "adGUrl": LikeUrl.of(
                            "//market.yandex.ru/product--lenovo-p780/301090/spec?hid=200&nid=2000&text=lenovo%20p780&clid=914&lr=213"
                        ),
                        "hint": Absent(),
                    },
                    {
                        "text": "prices",
                        "url": LikeUrl.of(
                            "//market.yandex.ru/product--lenovo-p780/301090/offers?clid=502&grhow=shop&hid=200&hyperid=301090&nid=2000&lr=213"
                        ),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090?clid=704&grhow=shop&hid=200&nid=2000&lr=213"
                        ),
                        "hint": "6",
                    },
                    {
                        "text": "opinions",
                        "url": LikeUrl.of("//market.yandex.ru/product--lenovo-p780/301090/reviews?clid=502&lr=213"),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090/reviews?clid=704&lr=213"
                        ),
                        "hint": "100",
                    },
                    {
                        "text": "articles",
                        "url": LikeUrl.of(
                            "//market.yandex.ru/product--lenovo-p780/301090/articles?clid=502&hid=200&nid=2000&lr=213"
                        ),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090/reviews?clid=704&hid=200&nid=2000&lr=213"
                        ),
                        "hint": "10",
                    },
                    {
                        "text": "forums",
                        "url": LikeUrl.of(
                            "//market.yandex.ru/product--lenovo-p780/301090/forum?clid=502&hid=200&nid=2000&lr=213"
                        ),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090?clid=704&hid=200&nid=2000&lr=213"
                        ),
                        "hint": "5",
                    },
                ],
            },
        )

    @classmethod
    def prepare_group_model_for_market_model(cls):
        cls.index.hypertree += [
            HyperCategory(hid=500, name='Suit'),
        ]
        cls.index.navtree += [
            NavCategory(nid=5000, hid=500),
        ]
        cls.index.model_groups += [
            ModelGroup(hyperid=599, title='Superman suit', hid=500, description='group model description')
        ]
        cls.index.models += [
            Model(
                hyperid=501,
                group_hyperid=599,
                title='Superman suit 1',
                hid=500,
                description='model 501 description',
                randx=3,
            ),
            Model(
                hyperid=502,
                group_hyperid=599,
                title='Superman suit 2',
                hid=500,
                description='model 502 description',
                randx=2,
            ),
            Model(
                hyperid=503,
                group_hyperid=599,
                title='Superman suit 3',
                hid=500,
                description='model 503 description',
                randx=1,
            ),
        ]
        cls.index.offers += [
            Offer(hyperid=501, title='offer 501_1', price=500),
            Offer(hyperid=501, title='offer 501_2', price=1000),
            Offer(hyperid=502, title='offer 502', price=300),
            Offer(hyperid=503, title='offer 503', price=200),
        ]

    def test_market_model_for_group_model(self):
        """
        Проверяется, что в случае наличия модификации групповой модели, колдунщик market_model будет сформирован
        для модификациии
        """
        response = self.report.request_bs_pb('place=parallel&text=Superman suit')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"raw": True, "text": "Superman suit 1"}},
                    "price": {
                        "priceMin": "500",
                        "priceMax": "1000",
                    },
                }
            },
        )

        log_content = ['"wiz_id":"market_model"', '"models":["501"]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

    @classmethod
    def prepare_counter_prefix_in_model_wizard(cls):
        cls.index.hypertree += [
            HyperCategory(hid=600),
        ]

        cls.index.navtree += [
            NavCategory(nid=6000, hid=600),
        ]

        cls.index.model_groups += [
            ModelGroup(hyperid=699, title='Deuter AC Lite 22', hid=600, opinion=Opinion(rating=5, rating_count=12))
        ]

        # Для формирования колдунщика групповой модели нужно, чтобы у неё было не меньше 5 модификаций
        # (иначе сформируется колдунщик для модификации)
        cls.index.models += [
            Model(hyperid=601, group_hyperid=699, title='Deuter AC Lite 22 grey'),
            Model(hyperid=602, group_hyperid=699, title='Deuter AC Lite 22 red'),
            Model(hyperid=603, group_hyperid=699, title='Deuter AC Lite 22 blue'),
            Model(hyperid=604, group_hyperid=699, title='Deuter AC Lite 22 yellow'),
            Model(hyperid=605, group_hyperid=699, title='Deuter AC Lite 22 green'),
        ]

    def test_category_wizards_with_categ_redirect_formula(self):
        """Проверяем, что при включенном эксперименте market_categ_wiz_with_redirect_formula
        категорийные колдунщики формируются с низким порогом и отсутствуют при высоком.
        https://st.yandex-team.ru/MARKETOUT-13046
        """
        # отключаем market_redirect_to_alone_category
        rearrs_fmt = (
            '&rearr-factors=market_no_restrictions=1;market_redirect_to_alone_category=0;'
            'market_categ_wiz_with_redirect_formula=1;market_category_redirect_treshold={}'
        )

        response = self.report.request_bs_pb('place=parallel&text=hood&rids=213' + rearrs_fmt.format(-3))
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "url": LikeUrl.of("//market.yandex.ru/catalog--hood/112?hid=112&clid=500"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=112&nid=112&clid=707"),
                    "priority": "8",
                    "type": "market_constr",
                    "subtype": "market_ext_category",
                    "title": {"__hl": {"text": "Hood на Маркете", "raw": True}},
                    "category_name": "Hood",
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=500"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=707"),
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/catalog--hood/112?hid=112&clid=500"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=112&nid=112&clid=707"),
                            "text": "Hood",
                        },
                    ],
                    "showcase": {},
                }
            },
        )
        response = self.report.request_bs_pb('place=parallel&text=hood&rids=213' + rearrs_fmt.format(5))
        self.assertFragmentNotIn(response, "market_ext_category")

    def test_ext_category_wizard_text_in_urls(self):
        """Проверка наличия text= в урлах колдунщика market_ext_category
        под флагом market_ext_category_wiz_text=1
        """
        response = self.report.request_bs_pb("place=parallel&text=food&rearr-factors=market_ext_category_wiz_text=1")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "url": LikeUrl.of("//market.yandex.ru/catalog--food/100?hid=100&text=food&clid=500"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=100&nid=100&text=food&clid=707"),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=500"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=707"),
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/catalog--food/100?hid=100&text=food&clid=500"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=100&nid=100&text=food&clid=707"),
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&text=food&clid=500"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&text=food&clid=707"
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&text=food&clid=500"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&text=food&clid=707"
                                    ),
                                },
                            },
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-dairy/100/list?glfilter=7893318%3A11&hid=100&text=food&clid=500"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-dairy/100/list?glfilter=7893318%3A11&hid=100&text=food&clid=707"
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-dairy/100/list?glfilter=7893318%3A11&hid=100&text=food&clid=500"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-dairy/100/list?glfilter=7893318%3A11&hid=100&text=food&clid=707"
                                    ),
                                },
                            },
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-butcher/100/list?glfilter=7893318%3A12&hid=100&text=food&clid=500"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-butcher/100/list?glfilter=7893318%3A12&hid=100&text=food&clid=707"
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-butcher/100/list?glfilter=7893318%3A12&hid=100&text=food&clid=500"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-butcher/100/list?glfilter=7893318%3A12&hid=100&text=food&clid=707"
                                    ),
                                },
                            },
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_models_without_offers(cls):
        """
        Подготовка для проверки модельного и неявной модели колдунщиков без офферов
        """
        cls.index.models += [Model(title="model no offers 1"), Model(title="model no offers 2")]

    def test_implicit_model_without_offers(self):
        """
        Проверка отсутствия колдунщика неявной модели без офферов
        https://st.yandex-team.ru/MARKETOUT-13896
        """
        response = self.report.request_bs_pb('place=parallel&text=nooffers')
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})

    def test_model_without_offers(self):
        """
        Проверка модельного колдунщика на отсутствие цены
        https://st.yandex-team.ru/MARKETOUT-13896
        """
        response = self.report.request_bs_pb('place=parallel&text=no+offers')
        self.assertFragmentIn(response, {"market_model": {"price": NoKey("price")}})

    @classmethod
    def prepare_removing_quotes_from_direct_url_offers_incut(cls):
        """
        Подготовка для проверки удаления одиночных кавычек из direct_url офферных врезок в колдунщиках
        https://st.yandex-team.ru/MARKETOUT-13055
        """
        cls.index.models += [
            Model(hyperid=701, title="direct_url"),
        ]
        cls.index.offers += [
            Offer(
                title='direct_url',
                hyperid=701,
                url="http://offers.ru?utm_source=sou'rce&utm_medium='medium&utm_term=term'&utm_campaign=c''am'pa'ign",
            ),
            Offer(
                title='direct_url',
                url="http://offers.ru?utm_source=sou'rce&utm_medium='medium&utm_term=term'&utm_campaign=c''am'pa'ign",
            ),
            Offer(
                title='direct_url',
                url="http://offers.ru?utm_source=sou'rce&utm_medium='medium&utm_term=term'&utm_campaign=c''am'pa'ign",
            ),
            Offer(
                title='direct_url',
                url="http://offers.ru?utm_source=sou'rce&utm_medium='medium&utm_term=term'&utm_campaign=c''am'pa'ign",
            ),
        ]

    def test_removing_quotes_from_direct_url_offers_incut(self):
        """
        Проверка удаления одиночных кавычек из direct_url офферных врезок в колдунщиках
        https://st.yandex-team.ru/MARKETOUT-13055
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=direct_url&rearr-factors=market_offers_wizard_incut_url_type=External'
        )
        # Проверка плиточной врезки офферного колдунщика
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": "http://offers.ru?utm_source=source&utm_medium=medium&utm_term=term&utm_campaign=campaign"
                                },
                                "thumb": {
                                    "url": "http://offers.ru?utm_source=source&utm_medium=medium&utm_term=term&utm_campaign=campaign"
                                },
                            },
                            {
                                "title": {
                                    "url": "http://offers.ru?utm_source=source&utm_medium=medium&utm_term=term&utm_campaign=campaign"
                                },
                                "thumb": {
                                    "url": "http://offers.ru?utm_source=source&utm_medium=medium&utm_term=term&utm_campaign=campaign"
                                },
                            },
                            {
                                "title": {
                                    "url": "http://offers.ru?utm_source=source&utm_medium=medium&utm_term=term&utm_campaign=campaign"
                                },
                                "thumb": {
                                    "url": "http://offers.ru?utm_source=source&utm_medium=medium&utm_term=term&utm_campaign=campaign"
                                },
                            },
                        ]
                    }
                }
            },
        )
        # Проверка модельного колдунщика
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": "http://offers.ru?utm_source=source&utm_medium=medium&utm_term=term&utm_campaign=campaign"
                                }
                            }
                        ]
                    }
                }
            },
        )

    def test_no_parallel_short_query_restriction_experiment(self):
        """Проверяем, что rearr-флаг market_no_parallel_short_query_restriction
        отключает фильтрацию моделей на параллельном при однословном поиске.
        Проверяем, что флаг включен по умолчанию.
        """
        # Проверяем колдунщик неявной модели
        response = self.report.request_bs_pb('place=parallel&text=panasonic')
        self.assertFragmentIn(response, {"market_implicit_model": {}})
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic&rearr-factors=market_no_parallel_short_query_restriction=0'
        )
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})

        # Проверяем модельный колдунщик
        response = self.report.request_bs_pb('place=parallel&text=pepelac')
        self.assertFragmentIn(response, {"market_model": {}})
        # 1. Без флага market_parallel_use_collapsing=0 модель находится по схлопыванию
        response = self.report.request_bs_pb(
            'place=parallel&text=pepelac&rearr-factors=market_no_parallel_short_query_restriction=0'
        )
        self.assertFragmentIn(response, {"market_model": {}})
        # 2. Под флагом market_parallel_use_collapsing=0 модель не находится
        response = self.report.request_bs_pb(
            'place=parallel&text=pepelac&rearr-factors=market_no_parallel_short_query_restriction=0;market_parallel_use_collapsing=0'
        )
        self.assertFragmentNotIn(response, {"market_model": {}})

    @classmethod
    def prepare_model_formula_data(cls):
        """Создаем модели с привязанными оферами,
        и фиксируем для моделей значения формулы.
        """
        cls.index.models += [
            Model(hyperid=711, hid=30, title='smart hero h11000', ts=711),
            Model(hyperid=712, hid=30, title='smart hero h12000', ts=712),
            Model(hyperid=713, hid=30, title='smart hero h13000', ts=713),
            Model(hyperid=714, hid=30, title='smart hero h14000', ts=714),
            Model(hyperid=715, hid=30, title='smart hero h15000', ts=715),
            Model(hyperid=716, hid=30, title='smart hero h16000 bad', ts=716),
            Model(hyperid=717, hid=30, title='smart hero h17000 bad', ts=717),
        ]

        cls.index.offers += [
            Offer(hyperid=711, title='smart hero h11000 offer', price=101),
            Offer(hyperid=712, title='smart hero h12000 offer', price=102),
            Offer(hyperid=713, title='smart hero h13000 offer', price=103),
            Offer(hyperid=714, title='smart hero h14000 offer', price=104),
            Offer(hyperid=715, title='smart hero h15000 offer', price=105),
            Offer(hyperid=716, title='smart hero h16000 bad offer', price=106),
            Offer(hyperid=717, title='smart hero h17000 bad offer', price=107),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 711).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 712).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 713).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 714).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 715).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 716).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 717).respond(0.04)

    def test_model_formula_ranking(self):
        """Проверяем, что флагом market_model_search_mn_algo выбирается формула для сортировки
        на параллельном для моделей.
        """

        # проверяем, что при включенной формуле для моделей
        # выбирается указанная формула
        response = self.report.request_bs(
            'place=parallel&text=smart+hero&debug=1&debug-doc-count=10'
            '&rearr-factors=market_search_mn_algo=MNA_RelevanceAndCtr;'
            'market_model_search_mn_algo=MNA_Relevance;'
            'market_model_wiz_top_model_threshold=0;'
        )
        self.assertFragmentIn(
            response,
            {
                'offerFactors': [
                    {"title": "smart hero h17000 bad offer", "ranked_with": [{"formula": "MNA_RelevanceAndCtr"}]}
                ],
                'modelFactors': [
                    {
                        "title": "smart hero h11000",
                        "url": "http://market.yandex.ru/product/711",
                        "ranked_with": [{"formula": "MNA_Relevance"}],
                    }
                ],
            },
        )

    def test_model_wiz_top_model_formula(self):
        """Проверяем, что при установленном значении market_model_wiz_top_model_threshold
        работает выбор по формуле топ-1 модели для модельного колдунщика.
        А если значение формулы для модели меньше market_model_wiz_top_model_threshold,
        то колдунщик не строится.
        https://st.yandex-team.ru/MARKETOUT-13819
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=smart+hero' '&rearr-factors=market_model_wiz_top_model_threshold=0.5;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"raw": True, "text": "Smart hero h11000"}},
                }
            },
        )

        # Без флага market_parallel_use_collapsing=0 модель не проходит порог по формуле топ-1, но находится по схлопыванию
        response = self.report.request_bs_pb(
            'place=parallel&text=smart+hero' '&rearr-factors=market_model_wiz_top_model_threshold=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"raw": True, "text": "Smart hero h11000"}},
                }
            },
        )

        # Под флагом market_parallel_use_collapsing=0 модель не проходит порог по формуле топ-1
        response = self.report.request_bs_pb(
            'place=parallel&text=smart+hero'
            '&rearr-factors=market_model_wiz_top_model_threshold=1;'
            'market_parallel_use_collapsing=0'
        )
        self.assertFragmentNotIn(response, {"market_model": []})

        # ранжирование по базовой формуле
        response = self.report.request_bs_pb('place=parallel&text=smart+hero&trace_wizard=1')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"raw": True, "text": "Smart hero h11000"}},
                }
            },
        )
        self.assertIn('31 1 Найдено 7 моделей', response.get_trace_wizard())

    def test_implicit_model_wiz_top_models_formula(self):
        """Проверяем, что при установленном значении market_implicit_model_wiz_top_models_threshold
        работает выбор порядка моделей по формуле для колдунщика неявной модели.
        А если сумма значений формулы для первых 4 моделей меньше
        market_implicit_model_wiz_top_models_threshold, то колдунщик не строится.
        Проверяем дефолтный порог для тача при выключенном обратном.
        https://st.yandex-team.ru/MARKETOUT-13819
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=smart+hero'
            '&rearr-factors=market_implicit_model_wiz_top_models_threshold=2;'
            'market_implicit_model_wizard_meta_threshold=-100'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "smart hero h11000", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "smart hero h12000", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "smart hero h15000", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "smart hero h14000", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=smart+hero'
            '&rearr-factors=market_implicit_model_wiz_top_models_threshold=4;'
            'market_implicit_model_wizard_meta_threshold=-100'
        )
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})

        # device=touch
        import re

        top4_models_re = re.compile(
            r'У первых 4 моделей sumMatrixnetValue=([\d.]+) < market_implicit_model_wiz_top_models_threshold=([\d.]+)'
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=bad'
            '&rearr-factors=device=touch;'
            'market_old_parallel_model_formula_ranking_logic=0;'
            'market_implicit_model_wizard_meta_threshold=-100;'
            'market_relevance_formula_threshold_on_parallel_for_offers=0;'
            '&trace_wizard=1'
        )
        match_obj = top4_models_re.search(str(response.get_trace_wizard()))
        self.assertIsNotNone(match_obj)
        mn_value, threshold = list(map(float, match_obj.groups()))
        self.assertAlmostEqual(mn_value, 0.08)
        self.assertAlmostEqual(threshold, 0.2)

        response = self.report.request_bs_pb(
            'place=parallel&text=smart+hero+bad'
            '&rearr-factors=device=touch;'
            'market_implicit_model_wizard_meta_threshold=-100;'
            '&trace_wizard=1'
        )
        match_obj = top4_models_re.search(str(response.get_trace_wizard()))
        self.assertIsNone(match_obj)

        # ранжирование по базовой формуле
        response = self.report.request_bs_pb('place=parallel&text=smart+hero' '&trace_wizard=1')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "smart hero h11000", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "smart hero h12000", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "smart hero h15000", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "smart hero h14000", "raw": True}}}},
                        ]
                    }
                }
            },
        )
        self.assertIn('29 2 Получено 7 моделей', response.get_trace_wizard())

    def test_disable_wizards(self):
        """Проверяем флаги отключения колдунщиков.

        https://st.yandex-team.ru/MARKETOUT-14122
        https://st.yandex-team.ru/MARKETOUT-24436
        """

        wiz_infos = [
            {'wiz_name': 'market_ext_category', 'query': 'food', 'flag': 'market_disable_ext_category_wiz'},
            {'wiz_name': 'market_model', 'query': 'smart+hero', 'flag': 'market_disable_model_wiz'},
            {'wiz_name': 'market_offers_wizard', 'query': 'kiyanka', 'flag': 'market_disable_offers_wiz'},
            {
                'wiz_name': 'market_implicit_model',
                'query': 'panasonic+tumix',
                'flag': 'market_disable_implicit_model_wiz',
            },
        ]

        for wiz_info in wiz_infos:
            response = self.report.request_bs_pb('place=parallel&text={}'.format(wiz_info['query']))
            self.assertFragmentIn(response, {wiz_info['wiz_name']: NotEmpty()})

            response = self.report.request_bs_pb(
                'place=parallel&text={}&rearr-factors={}=1'.format(wiz_info['query'], wiz_info['flag'])
            )
            self.assertFragmentNotIn(response, {wiz_info['wiz_name']: NotEmpty()})

    @classmethod
    def prepare_rating_count_model_wizard(cls):
        cls.index.models += [
            Model(
                hyperid=110,
                ts=101000,
                hid=30,
                title="Samsung Galaxy S8",
                opinion=Opinion(rating=4.5, rating_count=10, total_count=12),
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=110, price=666, fesh=12),
            Offer(hyperid=110, price=700),
        ]

    def test_rating_count_model_wizard(self):
        """Проверка количества оценок в модельном колдунщике
        https://st.yandex-team.ru/MARKETOUT-14130
        """
        # Проверка одиночной модели под конструктором
        response = self.report.request_bs_pb('place=parallel&text=Samsung Galaxy S8&rearr-factors=showcase_universal=1')
        self.assertFragmentIn(
            response, {"market_model": {"title": {"__hl": {"raw": True, "text": "Samsung Galaxy S8"}}, "rating": "4.5"}}
        )

    def test_empty_title_offers_wizard(self):
        """Проверка отсутствия исключения при наличии флага market_parallel_wizard=1
        и отсутствии данных ﻿wizard-rules (пустой тайтл колдунщика)
        https://st.yandex-team.ru/MARKETOUT-16013
        """
        # Проверка без флага market_parallel_wizard=1
        response = self.report.request_bs_pb('place=parallel&text=kiyanka')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Kiyanka\7]",
                }
            },
        )

        # Проверка с флагом market_parallel_wizard=1 и без wizard-rules
        response = self.report.request_bs_pb('place=parallel&text=kiyanka&rearr-factors=market_parallel_wizard=1')
        self.assertFragmentIn(response, {"market_offers_wizard": {"title": {"__hl": {"text": "", "raw": True}}}})

    @classmethod
    def prepare_snippet_text(cls):
        cls.index.models += [
            Model(
                title="tesla",
                hyperid=213,
                description="the description",
            )
        ]

    def test_snippet_text(self):
        """Проверка текстов сниппетов в модельном, категорийно вендорном, неявной модели и гуру категории колдунщиках
        под конструктором.
        https://st.yandex-team.ru/MARKETOUT-15779
        """
        # обычная модель
        response = self.report.request_bs_pb('place=parallel&text=tesla&rearr-factors=showcase_universal=1')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "text": [{"__hl": {"raw": True, "text": "the description"}}],
                }
            },
        )

        # неявная модель
        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "text": [
                        {
                            "__hl": {
                                "text": "Цены, характеристики, отзывы на panasonic tumix. Выбор по параметрам. 1 магазин.",
                                "raw": True,
                            }
                        }
                    ]
                }
            },
        )

        # гуру категория
        response = self.report.request_bs_pb("place=parallel&text=food")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "text": [
                        {
                            "__hl": {
                                "text": "Food — купить по выгодной цене с доставкой. "
                                "100 моделей в проверенных интернет-магазинах: популярные новинки и лидеры продаж. "
                                "Поиск по параметрам, удобное сравнение моделей и цен на Яндекс.Маркете.",
                                "raw": True,
                            }
                        }
                    ]
                }
            },
        )

    def test_offers_wizard_incut_shop_rating(self):
        """Проверка добавления рейтинга магазина во врезке офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-16282
        """
        # Проверка добавления рейтинга под флагом market_offers_wizard_shop_any_rating
        response = self.report.request_bs_pb(
            "place=parallel&text=pelmen" "&rearr-factors=market_offers_wizard_shop_any_rating=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "pelmen-1", "raw": True}}},
                                "greenUrl": {"text": "SHOP-400"},
                                "shopRating": {"value": "4.5"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "pelmen-2", "raw": True}}},
                                "greenUrl": {"text": "SHOP-401"},
                                "shopRating": NoKey("shopRating"),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "pelmen-3", "raw": True}}},
                                "greenUrl": {"text": "SHOP-402"},
                                "shopRating": {
                                    "value": "4",
                                },
                            },
                        ]
                    }
                }
            },
        )

        # Проверка отсутствия рейтинга без флага market_offers_wizard_shop_any_rating
        response = self.report.request_bs_pb("place=parallel&text=pelmen")
        self.assertFragmentNotIn(response, {"market_offers_wizard": {"showcase": {"items": [{"shopRating": {}}]}}})

    def test_offers_wizard_incut_shop_review(self):
        """Проверка добавления количества отзывов магазина во врезке офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-16282
        """
        # Проверка добавления количества отзывов под флагом market_offers_wizard_shop_reviews
        response = self.report.request_bs_pb(
            "place=parallel&text=pelmen" "&rearr-factors=market_offers_wizard_shop_reviews=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "pelmen-1", "raw": True}}},
                                "greenUrl": {"text": "SHOP-400"},
                                "shopReviews": {"count": "789"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "pelmen-2", "raw": True}}},
                                "greenUrl": {"text": "SHOP-401"},
                                "shopReviews": NoKey("shopReviews"),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "pelmen-3", "raw": True}}},
                                "greenUrl": {"text": "SHOP-402"},
                                "shopReviews": {"count": "20"},
                            },
                        ]
                    }
                }
            },
        )

        # Проверка отсутствия количества отзывов без флага market_offers_wizard_shop_reviews
        response = self.report.request_bs_pb("place=parallel&text=pelmen")
        self.assertFragmentNotIn(response, {"market_offers_wizard": {"showcase": {"items": [{"shopReviews": {}}]}}})

    def test_offers_wizard_category_title(self):
        """Проверяем для оферного, что при включенном флаге market_offers_wiz_use_category_title
        в заголовок подставляется название категории вместо запроса.
        Категория выбирается формулой категорийного редиректа,
        порог регулируется флагом market_offers_market_category_title_threshold.
        С флагом market_wizards_no_category_title_if_full_match замены не происходит,
        если тайтл одного из документов содержит подстрокой запрос.
        https://st.yandex-team.ru/MARKETOUT-16266
        """
        # запрос без флагов
        request = 'place=parallel&text=panasonic+tumix'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Panasonic tumix\7]",
                }
            },
        )

        # низкий порог
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wiz_use_category_title=1;'
            'market_offers_wiz_category_title_threshold=0.6;market_use_sequential_category_redirect=0;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "Китайские мобилы и другие товары",
                }
            },
        )

        # высокий порог
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wiz_use_category_title=1;'
            'market_offers_wiz_category_title_threshold=0.75;market_use_sequential_category_redirect=0;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Panasonic tumix\7]",
                }
            },
        )

        # флаг для отмены замены, если есть подстрока
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wiz_use_category_title=1;'
            'market_offers_wiz_category_title_threshold=0.6;'
            'market_wizards_no_category_title_if_full_match=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Panasonic tumix\7]",
                }
            },
        )

        # флаг для отмены замены, но подстроки нет
        response = self.report.request_bs_pb(
            'place=parallel&text=tumix+panasonic&rearr-factors='
            'market_offers_wiz_use_category_title=1;'
            'market_offers_wiz_category_title_threshold=0.6;'
            'market_wizards_no_category_title_if_full_match=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "Китайские мобилы и другие товары",
                }
            },
        )

    def test_implicit_model_category_title(self):
        """Проверяем для неявной модели, что при включенном флаге market_implicit_model_wiz_use_category_title
        в заголовок подставляется название категории вместо запроса.
        Категория выбирается формулой категорийного редиректа,
        порог регулируется флагом market_implicit_model_market_category_title_threshold.
        С флагом market_wizards_no_category_title_if_full_match замены не происходит,
        если тайтл одного из документов содержит подстрокой запрос.
        https://st.yandex-team.ru/MARKETOUT-16266
        """
        # запрос без флагов
        request = 'place=parallel&text=panasonic+tumix&rearr-factors=market_use_sequential_category_redirect=0;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "\7[Panasonic tumix\7]",
                }
            },
        )

        # низкий порог
        response = self.report.request_bs_pb(
            request + 'market_implicit_model_wiz_use_category_title=1;'
            'market_implicit_model_wiz_category_title_threshold=0.6;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "Китайские мобилы и другие товары",
                }
            },
        )

        # высокий порог
        response = self.report.request_bs_pb(
            request + 'market_implicit_model_wiz_use_category_title=1;'
            'market_implicit_model_wiz_category_title_threshold=3.0;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "\7[Panasonic tumix\7]",
                }
            },
        )

        # флаг для отмены замены, если есть подстрока
        response = self.report.request_bs_pb(
            request + 'market_implicit_model_wiz_use_category_title=1;'
            'market_implicit_model_wiz_category_title_threshold=0.6;'
            'market_wizards_no_category_title_if_full_match=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "\7[Panasonic tumix\7]",
                }
            },
        )

        # флаг для отмены замены, но подстроки нет
        response = self.report.request_bs_pb(
            'place=parallel&text=tumix+panasonic&rearr-factors='
            'market_implicit_model_wiz_use_category_title=1;'
            'market_implicit_model_wiz_category_title_threshold=0.6;'
            'market_wizards_no_category_title_if_full_match=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "Китайские мобилы и другие товары",
                }
            },
        )

    @classmethod
    def prepare_wizards_access_log(cls):
        """Подготовка данных для проверки логирования колдунщиков в access.log"""
        cls.index.hypertree += [
            HyperCategory(hid=700, tovalid=700, name='access-log-ext-category', output_type=HyperCategoryType.GURU),
        ]
        cls.index.vendors += [
            Vendor(vendor_id=700),
            Vendor(vendor_id=701),
            Vendor(vendor_id=702),
        ]
        cls.index.cards += [
            CardCategory(hid=700, vendor_ids=[700, 701, 702]),
        ]

    def test_wizards_access_log(self):
        """Проверка логирования колдунщиков в access.log
        https://st.yandex-team.ru/MARKETOUT-16396
        https://st.yandex-team.ru/MARKETOUT-17036
        """
        wiz_infos = [
            {'wiz_name': 'market_ext_category', 'tag': 'market_ext_category', 'query': 'access-log-ext-category'},
            {'wiz_name': 'market_model', 'tag': 'market_model', 'query': 'smart+hero'},
            {
                'wiz_name': 'market_model',
                'tag': 'market_model',
                'query': 'smart+hero',
                'rearr': 'showcase_universal_model=1',
            },
            {
                'wiz_name': 'market_model_right_incut',
                'tag': 'market_model_right_incut',
                'query': 'lenovo p780',
                'rearr': 'market_enable_model_wizard_right_incut=1',
            },
            {'wiz_name': 'market_offers_wizard', 'tag': 'market_offers_wizard', 'query': 'smart+hero'},
            {
                'wiz_name': 'market_offers_wizard_right_incut',
                'tag': 'market_offers_wizard_right_incut',
                'query': 'smart+hero',
                'rearr': 'market_enable_offers_wiz_right_incut=1',
            },
            {
                'wiz_name': 'market_offers_wizard_center_incut',
                'tag': 'market_offers_wizard_center_incut',
                'query': 'smart+hero',
                'rearr': 'market_enable_offers_wiz_center_incut=1',
            },
            {
                'wiz_name': 'market_offers_adg_wizard',
                'tag': 'market_offers_adg_wizard',
                'query': 'smart+hero',
                'rearr': 'market_enable_offers_adg_wiz=1',
            },
            {'wiz_name': 'market_implicit_model', 'tag': 'market_implicit_model', 'query': 'smart+hero'},
            {
                'wiz_name': 'market_implicit_model_adg_wizard',
                'tag': 'market_implicit_model_adg_incut',
                'query': 'smart+hero',
                'rearr': 'market_enable_implicit_model_adg_wiz=1',
            },
            {
                'wiz_name': 'market_implicit_model_center_incut',
                'tag': 'market_implicit_model_center_incut',
                'query': 'smart+hero',
                'rearr': 'market_enable_implicit_model_wiz_center_incut=1',
            },
            {
                'wiz_name': 'market_implicit_model_without_incut',
                'tag': 'market_implicit_model_without_incut',
                'query': 'smart+hero',
                'rearr': 'market_enable_implicit_model_wiz_without_incut=1',
            },
        ]

        for wiz_info in wiz_infos:
            flags = '&place=parallel{}&rearr-factors={}'.format(
                '&rids={}'.format(wiz_info['rids']) if wiz_info.get('rids') else '',
                '{};'.format(wiz_info['rearr']) if wiz_info.get('rearr') else '',
            )
            query = 'text={}{}'.format(wiz_info['query'], flags)

            response = self.report.request_bs_pb(query)
            # Проверка, что колдунщик сформировался
            self.assertFragmentIn(response, {wiz_info['wiz_name']: {}})
            # Проверка, что в access.log есть запись с тегом колдунщика без кворумного перезапроса
            self.access_log.expect(fuzzy_search_used=0, wizards=Contains(wiz_info['tag']))

    @classmethod
    def prepare_offers_wizard_categories(cls):
        """Подготовка данных для проверка добавления в офферный колдунщик данных о топ-5 категориях
        https://st.yandex-team.ru/MARKETOUT-16472
        """
        # Создаем категории
        cls.index.hypertree += [
            HyperCategory(hid=150, tovalid=150, name='Мобильные телефоны', output_type=HyperCategoryType.GURU),
            HyperCategory(hid=151, tovalid=151, name='Чехлы', output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=152, tovalid=152, name='Аккумуляторы', output_type=HyperCategoryType.SIMPLE),
            HyperCategory(hid=153, tovalid=153, name='Защитные пленки', output_type=HyperCategoryType.SIMPLE),
            HyperCategory(hid=154, tovalid=154, name='Зарядные устройства', output_type=HyperCategoryType.GURU),
            HyperCategory(hid=155, tovalid=155, name='Запасные части', output_type=HyperCategoryType.SIMPLE),
        ]

        # Задаем значения формуле релевантности категорийного редиректа
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 150).respond(0.9)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 151).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 152).respond(0.7)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 153).respond(0.4)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 154).respond(0.6)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 155).respond(0.5)

        # Создаем модели
        cls.index.models += [
            Model(
                hyperid=1150,
                hid=150,
                title='Iphone X',
                picinfo='//avatars.mds.yandex.net/get-mpic/category_150/model_1150/orig',
            ),
            Model(
                hyperid=1151,
                hid=151,
                title='Nokia 3310 model 1151',
                no_picture=True,
                add_picinfo='//avatars.mds.yandex.net/get-marketpic/category_151/model_1151/orig#100#100',
            ),
            Model(
                hyperid=1153,
                hid=153,
                title='Nokia 3310 model 1153',
                picinfo='//avatars.mds.yandex.net/get-marketpictesting/category_153/model_1153/orig#100#100',
            ),
            Model(hyperid=1155, hid=155, title='Nokia 3310 model 1155', no_picture=True, no_add_picture=True),
        ]

        # Создаем офферы
        cls.index.offers += [
            Offer(title='Nokia 3310 1', hid=150),
            Offer(title='Nokia 3310 2', hid=150),
            Offer(
                title='Nokia 3310 3',
                hid=152,
                picture=Picture(group_id=152, picture_id='categ_offer_152w', width=100, height=100),
            ),
            Offer(title='Nokia 3310 4', hid=154, no_picture=True),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=154, picinfo='//avatars.mds.yandex.net/get-mpic/hid_154/vendor_154/orig')
        ]

        # Создаем карточку гуру категории
        cls.index.cards += [CardCategory(hid=150, hyperids=[1150]), CardCategory(hid=154, vendor_ids=[154])]

    def test_offers_wizard_categories(self):
        """Проверка добавления в офферный колдунщик данных о топ-5 категориях
        https://st.yandex-team.ru/MARKETOUT-16472
        https://st.yandex-team.ru/MARKETOUT-18509
        """
        # Формула ранжирования категорийного редиректа отдает категории в последовательности: 150, 151, 152, 154, 155, 153
        # В sitelinks офферного колдунщика попадают 5 топ категорий.
        # Картинка для гуру категории 150 берется из топ модели карточки категории.
        # Картинки для категорий 151 и 153 берутся из моделей.
        # Картинка для категории 152 берется из офферов.
        # Картинка для гуру категории 154 берется из вендора карточки категории.
        # Для 155 картинки нет - ее пропускаем.
        response = self.report.request_bs_pb(
            'place=parallel&text=Nokia+3310&rearr-factors='
            'market_top_categories_in_offers_wizard=1;market_categ_wiz_dssm_factor_fast_calc=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "sitelinksWithCount": "all",
                    "sitelinks": [
                        {
                            "text": "Мобильные телефоны",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=150&text=nokia%203310&clid=545"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=150&text=nokia%203310&clid=708"),
                            "hint": "2",
                            "pictures": [
                                {
                                    "source": "//avatars.mds.yandex.net/get-mpic/category_150/model_1150/2hq",
                                    "retinaSource": "//avatars.mds.yandex.net/get-mpic/category_150/model_1150/5hq",
                                    "width": "100",
                                    "height": "100",
                                }
                            ],
                        },
                        {
                            "text": "Чехлы",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=151&text=nokia%203310&clid=545"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=151&text=nokia%203310&clid=708"),
                            "hint": "1",
                            "pictures": [
                                {
                                    "source": "//avatars.mds.yandex.net/get-marketpic/category_151/model_1151/100x100",
                                    "retinaSource": "//avatars.mds.yandex.net/get-marketpic/category_151/model_1151/200x200",
                                    "width": "100",
                                    "height": "100",
                                }
                            ],
                        },
                        {
                            "text": "Аккумуляторы",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=152&text=nokia%203310&clid=545"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=152&text=nokia%203310&clid=708"),
                            "hint": "1",
                            "pictures": [
                                {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/152/market_categ_offer_152w/100x100"
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/152/market_categ_offer_152w/200x200"
                                    ),
                                    "width": "100",
                                    "height": "100",
                                }
                            ],
                        },
                        {
                            "text": "Зарядные устройства",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=154&text=nokia%203310&clid=545"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=154&text=nokia%203310&clid=708"),
                            "hint": "1",
                            "pictures": [
                                {
                                    "source": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/hid_154/vendor_154/2hq"),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/hid_154/vendor_154/5hq"
                                    ),
                                    "width": "100",
                                    "height": "100",
                                }
                            ],
                        },
                        {
                            "text": "Защитные пленки",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=153&text=nokia%203310&clid=545"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=153&text=nokia%203310&clid=708"),
                            "hint": "1",
                            "pictures": [
                                {
                                    "source": "//avatars.mds.yandex.net/get-marketpictesting/category_153/model_1153/100x100",
                                    "retinaSource": "//avatars.mds.yandex.net/get-marketpictesting/category_153/model_1153/200x200",
                                    "width": "100",
                                    "height": "100",
                                }
                            ],
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=Nokia+3310&rearr-factors='
            'market_top_categories_in_offers_wizard=1;market_categ_wiz_dssm_factor_fast_calc=1'
            ';market_offers_wizard_category_without_request_text=1'
        )  # with flag show sitelink without request text parameter in urls

        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "sitelinks": [
                        {
                            "text": "Мобильные телефоны",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=150&clid=545", no_params=['text']),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=150&clid=708", no_params=['text']),
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_offers_wizard_min_category_count(cls):
        cls.index.hypertree += [
            HyperCategory(hid=160, tovalid=160),
            HyperCategory(hid=161, tovalid=161),
            HyperCategory(hid=162, tovalid=162),
            HyperCategory(hid=163, tovalid=163),
        ]

        cls.index.offers += [
            Offer(title='DigmaA105 phone 1', hid=160),
            Offer(title='DigmaA105 phone 2', hid=160),
            Offer(title='DigmaA105 original case', hid=161),
            Offer(title='DigmaA105 accum', hid=162),
            Offer(title='DigmaA105 original port', hid=163, no_picture=True),
            Offer(title='MAXVI C20', hid=160),
            Offer(title='MAXVI C20 original case', hid=161),
            Offer(title='MAXVI C20 original port', hid=163, no_picture=True),
        ]

    def test_offers_wizard_min_category_count(self):
        """Проверяем, что категории в оферном отдаются по флагу market_top_categories_in_offers_wizard,
        если их хотя бы 3.
        https://st.yandex-team.ru/MARKETOUT-18193
        https://st.yandex-team.ru/MARKETOUT-18509
        """
        # есть 3 категории (1 без картинки пропускается)
        response = self.report.request_bs_pb(
            'place=parallel&text=DigmaA105' '&rearr-factors=market_top_categories_in_offers_wizard=1'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {"sitelinks": []}})

        # только 2 категории (1 без картинки пропускается)
        response = self.report.request_bs_pb(
            'place=parallel&text=MAXVIC' '&rearr-factors=market_top_categories_in_offers_wizard=1'
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard": {"sitelinks": []}})

    def test_incut_on_touch(self):
        """Проверяем, что врезка для оферного и параметрического не строится
        на таче (&touch=1), если нет флага offers_touch.
        """
        # market_offers_incut_threshold_disable - отключает порог и ограничение на хотя бы 4 офера во врезке
        # устанавливаем минимальный порог market_offers_incut_meta_threshold
        touch_request = (
            'place=parallel&text=iphone5+i5+white+recovered&touch=1&rids=213'
            '&rearr-factors=market_offers_incut_threshold_disable=1;'
            'market_offers_incut_meta_threshold=0.0;'
            'market_enable_offers_wiz_center_incut=0;'
        )
        response = self.report.request_bs_pb(touch_request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {"showcase": {"items": EmptyList()}},
            },
        )
        response = self.report.request_bs_pb(touch_request + 'offers_touch=1')
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": NotEmptyList()}}})

    @classmethod
    def prepare_offers_per_shop_in_offers_incut(cls):
        """Подготовка данных для проверки отключения ограничения на количество офферов от магазина
        при формировании врезки офферного колдунщика
        """
        cls.index.shops += [
            Shop(fesh=20, priority_region=213),
            Shop(fesh=21, priority_region=213),
        ]
        cls.index.offers += [
            Offer(title='testOfferPerShop 21', fesh=20),
            Offer(title='testOfferPerShop 22', fesh=20),
            Offer(title='testOfferPerShop 23', fesh=20),
            Offer(title='testOfferPerShop 24', fesh=21),
            Offer(title='testOfferPerShop 25', fesh=21),
            Offer(title='testOfferPerShop 26', fesh=21),
            Offer(title='testOfferPerShop 27', fesh=21),
            Offer(title='testOfferPerShop 28', fesh=21),
            Offer(title='testOfferPerShop 29', fesh=21),
        ]

    def test_offers_per_shop_in_offers_incut(self):
        """Проверка отключения ограничения на количество офферов от магазина
        при формировании врезки офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-16950
        """
        # Без флага market_max_offers_per_shop_count_parallel действует ограничение 1 оффер от магазина.
        # Офферов для врезки 2, врезка не формируется.
        response = self.report.request_bs_pb('place=parallel&text=testOfferPerShop')
        self.assertFragmentIn(response, {"market_offers_wizard": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        # С флагом market_max_offers_per_shop_count_parallel=2 действует ограничение 2 оффера от магазина.
        # Офферов для врезки 4, формируется врезка с 3 офферами.
        response = self.report.request_bs_pb(
            'place=parallel&text=testOfferPerShop' '&rearr-factors=market_max_offers_per_shop_count_parallel=2'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": ElementCount(3)}}})

        # С флагом market_max_offers_per_shop_count_parallel=3 действует ограничение 3 оффера от магазина.
        # Офферов для врезки 6, формируется врезка с 6 офферами.
        response = self.report.request_bs_pb(
            'place=parallel&text=testOfferPerShop' '&rearr-factors=market_max_offers_per_shop_count_parallel=3'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": ElementCount(6)}}})

        # С флагом market_max_offers_per_shop_count_parallel=0 вообще нет ограничения на количество офферов от магазина.
        # Офферов для врезки 6, формируется врезка с 9 (3+6) офферами.
        response = self.report.request_bs_pb(
            'place=parallel&text=testOfferPerShop' '&rearr-factors=market_max_offers_per_shop_count_parallel=0'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": ElementCount(9)}}})

    @classmethod
    def prepare_implicit_model_wizard_by_top_offers(cls):
        """Подготовка данных для проверка формирования колдунщика неявной модели по топ офферам."""
        cls.index.models += [
            Model(hyperid=401, title='electricKettle Polaris model 1', ts=401),
            Model(hyperid=402, title='electricKettle REDMONDkettle model 1', ts=402),
            Model(hyperid=403, title='electricKettle REDMONDkettle model 2', ts=403),
            Model(hyperid=404, title='kettle model 1', ts=404),
            Model(hyperid=405, title='kettle model 2', ts=405),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 401).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 402).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 403).respond(0.3)

        cls.index.offers += [
            Offer(title='electricKettle Polaris', hyperid=401, price=100, ts=411),
            Offer(title='electricKettle REDMONDkettle', hyperid=402, price=200, ts=412),
            Offer(hyperid=403, price=300),
            Offer(title='otherKettle 1', hyperid=404, price=300, ts=413),
            Offer(title='otherKettle 2', hyperid=405, price=400, ts=414),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 411).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 412).respond(0.4)

    def test_implicit_model_wizard_by_top_offers(self):
        """Проверка формирования колдунщика неявной модели по новой логике под флагом market_implicit_model_wizard_by_offers.
        Модели беруться из топ офферов по ранжированию на параллельном.
        https://st.yandex-team.ru/MARKETOUT-16948
        """
        # С флагом market_implicit_model_wizard_by_offers колдунщик неявной модели формируется по офферам
        response = self.report.request_bs_pb(
            'place=parallel&text=electricKettle' '&rearr-factors=market_implicit_model_wizard_by_offers=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "2",
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "electricKettle Polaris model 1", "raw": True}},
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "electricKettle REDMONDkettle model 1", "raw": True}},
                                }
                            },
                        ]
                    },
                }
            },
            preserve_order=True,
        )

        log_content = ['"wiz_id":"market_implicit_model"', '"models":["401","402"]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

        # Без флага market_implicit_model_wizard_by_offers колдунщик неявной модели формируется по моделям
        response = self.report.request_bs_pb('place=parallel&text=electricKettle')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "3",
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "electricKettle Polaris model 1", "raw": True}},
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "electricKettle REDMONDkettle model 1", "raw": True}},
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "electricKettle REDMONDkettle model 2", "raw": True}},
                                }
                            },
                        ]
                    },
                }
            },
            preserve_order=True,
        )

        log_content = ['"wiz_id":"market_implicit_model"', '"models":["401","402","403"]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

        # Без флага market_implicit_model_wizard_by_offers
        # 1. Без флага market_parallel_use_collapsing=0 колдунщик неявной модели формируется, так как модели находятся по схлопыванию
        response = self.report.request_bs_pb('place=parallel&text=otherKettle')
        self.assertFragmentIn(response, {"market_implicit_model": {}})
        # 2. Под флагом market_parallel_use_collapsing=0 колдунщик неявной модели не формируется, так как модели не находятся
        response = self.report.request_bs_pb(
            'place=parallel&text=otherKettle&rearr-factors=market_parallel_use_collapsing=0'
        )
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})

        # С флагом market_implicit_model_wizard_by_offers по запросу 'electric kettle REDMOND'
        # не находится нужного количества офферов и колдунщик неявной модели формируется по найденным моделям
        response = self.report.request_bs_pb(
            'place=parallel&text=REDMONDkettle' '&rearr-factors=market_implicit_model_wizard_by_offers=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "2",
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "electricKettle REDMONDkettle model 1", "raw": True}},
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "electricKettle REDMONDkettle model 2", "raw": True}},
                                },
                            },
                        ]
                    },
                }
            },
        )

        log_content = ['"wiz_id":"market_implicit_model"', '"models":["402","403"]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

    @classmethod
    def prepare_offers_incut_meta_formula_data(cls):
        cls.index.offers += [
            Offer(title='incutMetaFormula 1', ts=501),
            Offer(title='incutMetaFormula 2', ts=502),
            Offer(title='incutMetaFormula 3', ts=503),
            Offer(title='incutMetaFormula 4', ts=504),
            Offer(title='incutMetaFormula 5', ts=505),
            Offer(title='incutMetaFormula 6', ts=506),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 503).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 504).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 505).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 506).respond(0.6)

        cls.matrixnet.on_place(MnPlace.INCUT_META, 501).respond(0.4)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 502).respond(0.3)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 503).respond(0.5)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 504).respond(0.6)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 505).respond(0.1)
        cls.matrixnet.on_place(MnPlace.INCUT_META, 506).respond(0.2)

    def test_offers_incut_meta_formula(self):
        """Проверяем, что при заданных флагах врезка не показывается,
        если сумма значений заданной формулы по топ-3 оферам врезки
        меньше заданного порога. Отдельные флаги с fuzzy=1 и без.
        Проверяем формулы по дефолту.
        https://st.yandex-team.ru/MARKETOUT-17148
        """
        request = 'place=parallel&text=incutMetaFormula&debug=1'
        full_answer = {
            "market_offers_wizard": [
                {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "incutMetaFormula 6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "incutMetaFormula 5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "incutMetaFormula 4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "incutMetaFormula 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "incutMetaFormula 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "incutMetaFormula 1", "raw": True}}}},
                        ]
                    }
                }
            ]
        }

        # с выключенным порогом
        for i in (0, 1):
            response = self.report.request_bs(
                request + '&rearr-factors=fuzzy={};'.format(i) + 'market_offers_incut_meta_threshold=-100;'
            )
            self.assertFragmentIn(response, full_answer, preserve_order=True, allow_different_len=False)

        # порог без fuzzy, порог не пройден
        response = self.report.request_bs(
            request + '&rearr-factors=market_offers_incut_meta_mn_algo=MNA_mn201477;'
            'market_offers_incut_meta_threshold=1.0;'
        )
        self.assertFragmentIn(response, 'Using offers incut meta MatrixNet formula: MNA_mn201477')
        self.assertFragmentIn(response, 'market_offers_wizard')
        self.assertFragmentNotIn(response, 'market_offers_wizard_right_incut')
        self.assertFragmentNotIn(response, 'market_offers_wizard_center_incut')

        # порог без fuzzy, порог пройден
        response = self.report.request_bs(
            request + '&rearr-factors=market_offers_incut_meta_mn_algo=MNA_mn201477;'
            'market_offers_incut_meta_threshold=0.8;'
        )
        self.assertFragmentIn(response, 'Using offers incut meta MatrixNet formula: MNA_mn201477')
        self.assertFragmentIn(response, full_answer, preserve_order=True, allow_different_len=False)

        # формула по умолчанию
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, 'Using offers incut meta MatrixNet formula: MNA_fml_formula_785353')

    def test_offers_incut_meta_formula_if_touch(self):
        request = 'place=parallel&text=incutMetaFormula&debug=1&rearr-factors=device=touch'
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, 'Using offers incut meta MatrixNet formula: MNA_fml_formula_785353')
        self.assertFragmentIn(
            response, 'Using MatrixNet formula for search ranking: MNA_wiz_offer_785353_017_x_803473_083'
        )

    def test_offers_wizard_top_offers_meta_threshold(self):
        """Проверяем, что если сумма значений мета-формулы топ-3 оферов
        из оферной врезки меньше market_offers_wiz_top_offers_meta_threshold,
        то оферный колдунщик не строится.
        Также проверяем, что порог по показу оферной врезки не влияет
        на порог по показу оферного колдунщика.
        https://st.yandex-team.ru/MARKETOUT-18495
        """
        query = 'place=parallel&text=incutMetaFormula'

        full_answer = {
            "market_offers_wizard": {
                "showcase": {
                    "items": [
                        {"title": {"text": {"__hl": {"text": "incutMetaFormula 6", "raw": True}}}},
                        {"title": {"text": {"__hl": {"text": "incutMetaFormula 5", "raw": True}}}},
                        {"title": {"text": {"__hl": {"text": "incutMetaFormula 4", "raw": True}}}},
                        {"title": {"text": {"__hl": {"text": "incutMetaFormula 3", "raw": True}}}},
                        {"title": {"text": {"__hl": {"text": "incutMetaFormula 2", "raw": True}}}},
                        {"title": {"text": {"__hl": {"text": "incutMetaFormula 1", "raw": True}}}},
                    ]
                }
            }
        }

        for fuzzy in (0, 1):
            rearrs = '&rearr-factors=fuzzy={};'.format(fuzzy)

            # с выключенным порогом
            response = self.report.request_bs_pb(query + rearrs + 'market_offers_incut_meta_threshold=-100;')
            self.assertFragmentIn(response, full_answer, preserve_order=True, allow_different_len=False)

            # порог оферного пройден, формулы включены (по умолчанию), порог врезки пройден
            for threshold in ('', 'market_offers_wiz_top_offers_meta_threshold=0.8;'):
                response = self.report.request_bs_pb(
                    query + rearrs + threshold + 'market_offers_incut_meta_mn_algo=MNA_mn201477;'
                    'market_offers_incut_meta_threshold=0.8;'
                )
                self.assertFragmentIn(response, full_answer, preserve_order=True, allow_different_len=False)

            # порог оферного пройден, формулы включены (по умолчанию), порог врезки не пройден
            for threshold in ('', 'market_offers_wiz_top_offers_meta_threshold=0.8;'):
                response = self.report.request_bs_pb(
                    query + rearrs + threshold + 'market_offers_incut_meta_mn_algo=MNA_mn201477;'
                    'market_offers_incut_meta_threshold=1.0;'
                )
                self.assertFragmentIn(response, {"market_offers_wizard": {}})
                self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
                self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

            # порог оферного не пройден, формулы включены (по умолчанию), порог врезки пройден
            response = self.report.request_bs_pb(
                query + rearrs + 'market_offers_wiz_top_offers_meta_threshold=1.0;'
                'market_offers_incut_meta_mn_algo=MNA_mn201477;'
                'market_offers_incut_meta_threshold=1.0;'
            )
            self.assertFragmentNotIn(response, {"market_offers_wizard": {}})

            # порог оферного не пройден, формулы включены (по умолчанию), порог врезки не пройден
            response = self.report.request_bs_pb(
                query + rearrs + 'market_offers_wiz_top_offers_meta_threshold=1.0;'
                'market_offers_incut_meta_mn_algo=MNA_mn201477;'
                'market_offers_incut_meta_threshold=1.0;'
            )
            self.assertFragmentNotIn(response, {"market_offers_wizard": {}})

    def test_offers_wizard_meta_relevance_in_market_factors(self):
        """Проверка добавления значения формулы офферной врезки на мете
        в market_factors для проброса на верхний
        https://st.yandex-team.ru/MARKETOUT-18351
        """
        # порог пройден, значение добавляется
        response = self.report.request_bs(
            'place=parallel&text=incutMetaFormula' '&rearr-factors=market_offers_wiz_top_offers_meta_threshold=0.1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "OffersIncutRelevance": NotEmpty(),
                    }
                ]
            },
        )

        # порог не пройден, значение добавляется
        response = self.report.request_bs(
            'place=parallel&text=incutMetaFormula' '&rearr-factors=market_offers_wiz_top_offers_meta_threshold=1.0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "OffersIncutRelevance": NotEmpty(),
                    }
                ]
            },
        )

        # порог не задан, значение добавляется
        response = self.report.request_bs('place=parallel&text=incutMetaFormula')
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "OffersIncutRelevance": NotEmpty(),
                    }
                ]
            },
        )

    def test_offers_incut_count(self):
        """Проверка количества офферов и выравнивания во врезке офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-17200
        """

        def test_items_count(response, count):
            self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"items": ElementCount(count)}}})

        # По дефолту во врезке 9 офферов
        request = 'place=parallel&text=kuvalda'
        response = self.report.request_bs_pb(request)
        test_items_count(response, 9)

        # С максимальным количеством офферов 8 и выравниванием по дефолту 3 во врезке 6 офферов
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_incut_max_count=8')
        test_items_count(response, 6)

        # С максимальным количеством офферов по дефолту 9 и выравниванием 2 во врезке 8 офферов
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_incut_align=2')
        test_items_count(response, 8)

        # С максимальным количеством офферов 6 и выравниванием 2 во врезке 6 офферов
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_incut_max_count=6;market_offers_incut_align=2'
        )
        test_items_count(response, 6)

        # С максимальным количеством офферов 5 и выравниванием 1 во врезке 5 офферов
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_incut_max_count=5;market_offers_incut_align=1'
        )
        test_items_count(response, 5)

    @classmethod
    def prepare_model_titles_and_pictures_in_offers_wizard(cls):
        """Подготовка данных для проверки замены заголовка и картинки офферов в офферном колдунщике на модельные
        https://st.yandex-team.ru/MARKETOUT-17264
        """
        cls.index.models += [
            Model(
                hyperid=901,
                hid=20,
                title='hoover model 1',
                picinfo='//avatars.mds.yandex.net/get-mpic/group-700/model_901/orig#100#100',
                no_add_picture=True,
                opinion=Opinion(rating=4.5, rating_count=10, total_count=12, precise_rating=4.32),
            ),
            Model(hyperid=902, hid=20, title='hoover model 2', no_picture=True, no_add_picture=True),
        ]
        cls.index.offers += [
            Offer(
                ts=701,
                fesh=701,
                title='hoover 1',
                picture=Picture(group_id=700, picture_id='hoover_1', width=200, height=200),
                hyperid=901,
                price=100.1,
                waremd5='LuEE8VOUlal6_JoFbM54ug',
            ),
            Offer(
                ts=702,
                fesh=702,
                title='hoover 2',
                picture=Picture(group_id=700, picture_id='hoover_2', width=200, height=200),
                hyperid=902,
                price=200,
                waremd5='VZVYn_bQ5W7wWFX_oxhqDA',
            ),
            Offer(
                ts=703,
                fesh=703,
                title='hoover offer only hooverOffer 3',
                picture=Picture(group_id=700, picture_id='hoover_3', width=200, height=200),
                price=300,
                waremd5='8livnbpm1kEqXvJZAAgGWA',
            ),
            Offer(
                ts=704,
                fesh=704,
                title='hoover offer only hooverOffer 4',
                picture=Picture(group_id=700, picture_id='hoover_4', width=200, height=200),
                price=400,
                waremd5='ETilhrJXTizDDQ3sU5QAnA',
            ),
            Offer(
                ts=705,
                fesh=705,
                title='hoover 5',
                picture=Picture(group_id=700, picture_id='hoover_5', width=200, height=200),
                hyperid=901,
                price=150,
                waremd5='De_bfRIdWGPX2ZzQYELo7w',
            ),
            Offer(
                ts=706,
                fesh=706,
                title='hoover 6',
                picture=Picture(group_id=700, picture_id='hoover_6', width=200, height=200),
                hyperid=902,
                price=150.6,
                waremd5='7rci91_zdRk39Qy22Hc3FQ',
            ),
            Offer(
                ts=707,
                fesh=707,
                title='hoover offer only hooverOffer 7',
                picture=Picture(group_id=700, picture_id='hoover_7', width=200, height=200),
                price=500,
                waremd5='5noNOBlMAuWR2YH8lERzpA',
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 701).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 702).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 703).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 704).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 705).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 706).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 707).respond(0.3)

    def test_model_titles_in_offers_wizard(self):
        """Проверка замены заголовка офферов в офферном колдунщике на модельные
        https://st.yandex-team.ru/MARKETOUT-17264
        """
        # Без флага market_offers_wizard_model_titles в выдаче тайтлы офферов
        response = self.report.request_bs_pb("place=parallel&text=hoover")
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "hoover 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "hoover 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "hoover offer only hooverOffer 3", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        # С флагом market_offers_wizard_model_titles в выдаче тайтлы офферов заменяются на тайтлы моделей
        response = self.report.request_bs_pb(
            "place=parallel&text=hoover&rearr-factors=market_offers_wizard_model_titles=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "hoover model 1", "raw": True}}}},  # Тайтл модели
                            {"title": {"text": {"__hl": {"text": "hoover model 2", "raw": True}}}},  # Тайтл модели
                            {
                                "title": {"text": {"__hl": {"text": "hoover offer only hooverOffer 3", "raw": True}}}
                            },  # У оффера нет модели, тайтл оффера
                        ]
                    }
                }
            },
        )

    def test_model_pictures_in_offers_wizard(self):
        """Проверка замены  картинки офферов в офферном колдунщике на модельные
        https://st.yandex-team.ru/MARKETOUT-17264
        """
        # Без флага market_offers_wizard_model_pictures в выдаче картинки офферов
        response = self.report.request_bs_pb("place=parallel&text=hoover")
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "hoover 1", "raw": True}}},
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_1/100x100"
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_1/200x200"
                                    ),
                                    "height": "100",
                                    "width": "100",
                                },
                            },
                            {
                                "title": {"text": {"__hl": {"text": "hoover 2", "raw": True}}},
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/100x100"
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/200x200"
                                    ),
                                    "height": "100",
                                    "width": "100",
                                },
                            },
                            {
                                "title": {"text": {"__hl": {"text": "hoover offer only hooverOffer 3", "raw": True}}},
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/100x100"
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/200x200"
                                    ),
                                    "height": "100",
                                    "width": "100",
                                },
                            },
                        ]
                    }
                }
            },
        )

        # С флагом market_offers_wizard_model_pictures в выдаче картинки офферов заменяются на картинки моделей
        response = self.report.request_bs_pb(
            "place=parallel&text=hoover&rearr-factors=market_offers_wizard_model_pictures=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "hoover 1", "raw": True}}},
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/group-700/model_901/2hq"
                                    ),  # Картинка модели
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/group-700/model_901/5hq"
                                    ),
                                    "height": "100",
                                    "width": "100",
                                },
                            },
                            {
                                "title": {"text": {"__hl": {"text": "hoover 2", "raw": True}}},
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/100x100"
                                    ),  # У модели нет картинки, картинка оффера
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/200x200"
                                    ),
                                    "height": "100",
                                    "width": "100",
                                },
                            },
                            {
                                "title": {"text": {"__hl": {"text": "hoover offer only hooverOffer 3", "raw": True}}},
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/100x100"
                                    ),  # У оффера нет модели, картинка оффера
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/200x200"
                                    ),
                                    "height": "100",
                                    "width": "100",
                                },
                            },
                        ]
                    }
                }
            },
        )

    def test_model_docs_in_offers_wizard(self):
        """Проверяем добавление моделей в начало врезки по флагу market_offers_wizard_model_docs_min_size.
        Модели берутся без дублей из оферов врезки.
        Добавление моделей происходит, если их количество не меньше заданного
        в флаге market_offers_wizard_model_docs_min_size.
        После добавления обрезаем и выравниванием до нормальных размеров врезки.

        https://st.yandex-team.ru/MARKETOUT-24859
        """
        request = 'place=parallel&text=hoover&rearr-factors='

        offers_incut = [
            {
                "modelContent": Absent(),
                "title": {
                    "text": {"__hl": {"text": "hoover 1", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_1/100x100"),
                    "retinaSource": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_1/200x200"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "price": {"priceMax": "100"},
                "greenUrl": {"text": "SHOP-701"},
            },
            {
                "modelContent": Absent(),
                "title": {
                    "text": {"__hl": {"text": "hoover 2", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/100x100"),
                    "retinaSource": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/200x200"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "price": {"priceMax": "200"},
                "greenUrl": {"text": "SHOP-702"},
            },
            {
                "modelContent": Absent(),
                "title": {
                    "text": {"__hl": {"text": "hoover offer only hooverOffer 3", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/100x100"),
                    "retinaSource": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/200x200"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "price": {"priceMax": "300"},
                "greenUrl": {"text": "SHOP-703"},
            },
            {
                "modelContent": Absent(),
                "title": {
                    "text": {"__hl": {"text": "hoover offer only hooverOffer 4", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_4/100x100"),
                    "retinaSource": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_4/200x200"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "price": {"priceMax": "400"},
                "greenUrl": {"text": "SHOP-704"},
            },
            {
                "modelContent": Absent(),
                "title": {
                    "text": {"__hl": {"text": "hoover 5", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_5/100x100"),
                    "retinaSource": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_5/200x200"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "price": {"priceMax": "150"},
                "greenUrl": {"text": "SHOP-705"},
            },
            {
                "modelContent": Absent(),
                "title": {
                    "text": {"__hl": {"text": "hoover 6", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_6/100x100"),
                    "retinaSource": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_6/200x200"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "price": {"priceMax": "151"},  # округление
                "greenUrl": {"text": "SHOP-706"},
            },
            {
                "modelContent": Absent(),
                "title": {
                    "text": {"__hl": {"text": "hoover offer only hooverOffer 7", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_7/100x100"),
                    "retinaSource": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_7/200x200"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                },
                "price": {"priceMax": "500"},
                "greenUrl": {"text": "SHOP-707"},
            },
        ]

        models_in_offers_incut = [
            {
                "modelContent": {
                    "title": "hoover model 1",
                    "price": "100",
                    "pictureUrl": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/group-700/model_901/2hq"),
                    "pictureWidth": "100",
                    "pictureHeight": "100",
                    "pictureHdUrl": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/group-700/model_901/5hq"),
                    "rating": "4.32",
                    "opinion_count": "12",
                },
                "title": {
                    "text": {"__hl": {"text": "hoover model 1", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                    "url": LikeUrl.of("//market.yandex.ru/product--hoover-1/901?clid=545"),
                    "urlForCounter": LikeUrl.of("//market.yandex.ru/product--hoover-1/901?clid=545"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/group-700/model_901/2hq"),
                    "retinaSource": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/group-700/model_901/5hq"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                    "url": LikeUrl.of("//market.yandex.ru/product--hoover-1/901?clid=545"),
                    "urlForCounter": LikeUrl.of("//market.yandex.ru/product--hoover-1/901?clid=545"),
                },
                "price": {"priceMax": "100"},
                "greenUrl": {"text": "Яндекс.Маркет"},
            },
            {
                "modelContent": {
                    "title": "hoover model 2",
                    "price": "151",  # мин. цена модели - у модели есть офер с меньшей ценой
                    "pictureUrl": "",  # у модели нет картинки
                    "pictureWidth": "",
                    "pictureHeight": "",
                    "pictureHdUrl": "",
                    "rating": "",  # нет рейтинга и отзывов
                    "opinion_count": "",
                },
                "title": {
                    "text": {"__hl": {"text": "hoover model 2", "raw": True}},
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                    "url": LikeUrl.of("//market.yandex.ru/product--hoover-2/902?clid=545"),
                    "urlForCounter": LikeUrl.of("//market.yandex.ru/product--hoover-2/902?clid=545"),
                },
                "thumb": {
                    "source": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/100x100"),
                    "retinaSource": LikeUrl.of("//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/200x200"),
                    "height": "100",
                    "width": "100",
                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                    "url": LikeUrl.of("//market.yandex.ru/product--hoover-2/902?clid=545"),
                    "urlForCounter": LikeUrl.of("//market.yandex.ru/product--hoover-2/902?clid=545"),
                },
                "price": {"priceMax": "151"},
                "greenUrl": {"text": "Яндекс.Маркет"},
            },
        ]

        # Без флага market_offers_wizard_model_docs_min_size в выдаче только оферы.
        # При заданном пороге market_offers_wizard_model_docs_min_size
        # модели не добавляются, если их количество меньше порога.
        for addit_rearrs in ('', 'market_offers_wizard_model_docs_min_size=3;'):
            response = self.report.request_bs_pb(request + addit_rearrs)
            self.assertFragmentIn(
                response,
                {"market_offers_wizard": {"incut_model_content": Absent(), "showcase": {"items": offers_incut[:6]}}},
                preserve_order=True,
                allow_different_len=False,
            )

        # С флагом market_offers_wizard_model_docs_min_size добавляются модели.
        # Количество моделей должно быть не меньше заданного.
        response = self.report.request_bs_pb(request + 'market_offers_wizard_model_docs_min_size=2;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "incut_model_content": "1",
                    "showcase": {"items": models_in_offers_incut + offers_incut},
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что максимальное количество документов во врезке сохраняется
        response = self.report.request_bs_pb(
            request + 'market_offers_incut_max_count=6;market_offers_wizard_model_docs_min_size=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "incut_model_content": "1",
                    "showcase": {"items": models_in_offers_incut + offers_incut[:4]},
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что выравнивание документов во врезке сохраняется
        response = self.report.request_bs_pb(
            request + 'market_offers_incut_align=5;market_offers_wizard_model_docs_min_size=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "incut_model_content": "1",
                    "showcase": {"items": models_in_offers_incut + offers_incut[:3]},
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_model_content_in_offers_wizard(self):
        """Проверяем добавление модельных данных в оферном колдунщике
        с флагом market_offers_wizard_model_content.
        Информация о модели остается только у первого соответствующего ей офера (схлопывание),
        у оферов без моделей информации нет.
        Добавление информации происходит, если размер врезки с моделями не меньше заданного
        через флаг market_offers_wizard_model_content_min_size.
        https://st.yandex-team.ru/MARKETOUT-20069
        https://st.yandex-team.ru/MARKETOUT-20386
        """
        # Без флага market_offers_wizard_model_content в выдаче только данные оферов.
        # С флагом market_offers_wizard_model_content при заданном пороге market_offers_wizard_model_content_min_size
        # данные не добавляются, если размер врезки с моделями меньше порога.
        # При device=desktop по дефолту значения флагов market_offers_wizard_model_content=1 market_offers_wizard_model_content_min_size=3
        for addit_rearrs in (
            '',
            'market_offers_wizard_model_content=1;market_offers_wizard_model_content_min_size=3;',
            'device=desktop',
        ):
            response = self.report.request_bs_pb('place=parallel&text=hoover&rearr-factors=' + addit_rearrs)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "incut_model_content": Absent(),
                        "showcase": {
                            "items": [
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover 1", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_1/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_1/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "100"},
                                },
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover 2", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "200"},
                                },
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover offer only hooverOffer 3", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "300"},
                                },
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover offer only hooverOffer 4", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_4/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_4/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "400"},
                                },
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover 5", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_5/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_5/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "150"},
                                },
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover 6", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_6/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_6/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "151"},  # округление
                                },
                            ]
                        },
                    }
                },
                allow_different_len=False,
            )

        # С флагом market_offers_wizard_model_content добавляются данные моделей
        # (если модели нет - у офера нет блока данных).
        # Данные добавляются первому соответствующему модели оферу (схлопывание).
        # Если задан порог market_offers_wizard_model_content_min_size,
        # то размер врезки с моделями должен быть не меньше.
        # При device=desktop по дефолту значения флагов market_offers_wizard_model_content=1 market_offers_wizard_model_content_min_size=3
        for addit_rearrs, host_prefix, clid in [
            ('market_offers_wizard_model_content=1;', '', 913),
            ('device=desktop;market_offers_wizard_model_content_min_size=2;', '', 913),
            ('market_offers_wizard_model_content=1;market_offers_wizard_model_content_min_size=2;', '', 913),
            # Touch
            ('market_offers_wizard_model_content=1;offers_touch=1&touch=1', 'm.', 919),
            (
                'market_offers_wizard_model_content=1;market_offers_wizard_model_content_min_size=2;offers_touch=1&touch=1',
                'm.',
                919,
            ),
        ]:
            response = self.report.request_bs_pb('place=parallel&text=hoover&rearr-factors=' + addit_rearrs)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "incut_model_content": "1",
                        "showcase": {
                            "items": [
                                {
                                    "modelContent": {
                                        "title": "hoover model 1",
                                        "price": "100",
                                        "pictureUrl": LikeUrl.of(
                                            "//avatars.mds.yandex.net/get-mpic/group-700/model_901/2hq"
                                        ),
                                        "pictureWidth": "100",
                                        "pictureHeight": "100",
                                        "pictureHdUrl": LikeUrl.of(
                                            "//avatars.mds.yandex.net/get-mpic/group-700/model_901/5hq"
                                        ),
                                        "rating": "4.32",
                                        "opinion_count": "12",
                                    },
                                    "title": {
                                        "text": {"__hl": {"text": "hoover 1", "raw": True}},
                                        "offercardUrl": LikeUrl.of(
                                            "//{0}market.yandex.ru/product--hoover-1/901?clid={1}".format(
                                                host_prefix, clid
                                            )
                                        ),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_1/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_1/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": LikeUrl.of(
                                            "//{0}market.yandex.ru/product--hoover-1/901?clid={1}".format(
                                                host_prefix, clid
                                            )
                                        ),
                                    },
                                    "price": {"priceMax": "100"},
                                },
                                {
                                    "modelContent": {
                                        "title": "hoover model 2",
                                        "price": "151",  # мин. цена модели - у модели есть офер с меньшей ценой
                                        "pictureUrl": "",  # у модели нет картинки
                                        "pictureWidth": "",
                                        "pictureHeight": "",
                                        "pictureHdUrl": "",
                                        "rating": "",  # нет рейтинга и отзывов
                                        "opinion_count": "",
                                    },
                                    "title": {
                                        "text": {"__hl": {"text": "hoover 2", "raw": True}},
                                        "offercardUrl": LikeUrl.of(
                                            "//{0}market.yandex.ru/product--hoover-2/902?clid={1}".format(
                                                host_prefix, clid
                                            )
                                        ),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_2/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": LikeUrl.of(
                                            "//{0}market.yandex.ru/product--hoover-2/902?clid={1}".format(
                                                host_prefix, clid
                                            )
                                        ),
                                    },
                                    "price": {"priceMax": "200"},
                                },
                                {
                                    # у офера нет модели
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover offer only hooverOffer 3", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "300"},
                                },
                                {
                                    # у офера нет модели
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover offer only hooverOffer 4", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_4/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_4/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "400"},
                                },
                                {
                                    # офер схлопнут с hoover 1
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover 5", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_5/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_5/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "150"},
                                },
                                {
                                    # офер схлопнут с hoover 2
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover 6", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_6/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_6/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "151"},
                                },
                            ]
                        },
                    }
                },
            )

        # Проверяем, что для 0 моделей информация не добавляется
        for addit_rearrs in (
            '',
            'market_offers_wizard_model_content=1;',
            'device=desktop;market_offers_wizard_model_content_min_size=0;',
        ):
            # market_check_offers_incut_size=0 отключает проверку на хотя бы 4 офера
            response = self.report.request_bs_pb(
                'place=parallel&text=hooverOffer&rearr-factors=market_check_offers_incut_size=0;' + addit_rearrs
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "incut_model_content": Absent(),
                        "showcase": {
                            "items": [
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover offer only hooverOffer 3", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_3/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "300"},
                                },
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover offer only hooverOffer 4", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_4/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_4/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "400"},
                                },
                                {
                                    "modelContent": Absent(),
                                    "title": {
                                        "text": {"__hl": {"text": "hoover offer only hooverOffer 7", "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_7/100x100"
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mdst.yandex.net/get-marketpic/700/market_hoover_7/200x200"
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "price": {"priceMax": "500"},
                                },
                            ]
                        },
                    }
                },
                allow_different_len=False,
            )

    def test_implicit_model_reviews(self):
        """Проверка наличия отзывов в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-17565
        https://st.yandex-team.ru/MARKETOUT-31265
        """
        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "reviewsUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&rs=eJwzEvBiFWI2NDCEUEYRDAAXRwKI&show-reviews=1&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "reviewsUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&rs=eJwzEvBiFWI2NDCEUEYRDAAXRwKI&show-reviews=1&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "reviewsAdGUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&rs=eJwzEvBiFWI2NDCEUEYRDAAXRwKI&show-reviews=1&clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "reviewsAdGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&rs=eJwzEvBiFWI2NDCEUEYRDAAXRwKI&show-reviews=1&clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}}},
                                "reviews": {
                                    "count": "12",  # На модель есть 12 отзывов
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?clid=698&utm_medium=cpc&utm_referrer=wizards"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?clid=721&utm_medium=cpc&utm_referrer=wizards"
                                    ),
                                },
                            },
                            {
                                "title": {"text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}}},
                                "reviews": NoKey("reviews"),  # На модель нет отзывов
                            },
                        ]
                    },
                }
            },
        )

    def test_model_wizard_shop_count(self):
        """Проверка количества магазинов во врезке модельного колдунщика
        https://st.yandex-team.ru/MARKETOUT-17689
        """
        # Количество магазинов задается флагом market_model_wizard_shop_count
        for i in range(1, 7):
            response = self.report.request_bs_pb(
                'place=parallel&text=lenovo p780&rids=213' '&rearr-factors=market_model_wizard_shop_count={}'.format(i)
            )
            self.assertFragmentIn(response, {"market_model": {"showcase": {"items": ElementCount(i)}}})

        # Без флага в выдаче 5 магазинов
        response = self.report.request_bs_pb('place=parallel&text=lenovo p780&rids=213')
        self.assertFragmentIn(response, {"market_model": {"showcase": {"items": ElementCount(5)}}})

    @classmethod
    def prepare_model_wizards_meta_formulas_data(cls):
        cls.index.models += [
            Model(title='ModelMetaFormulas model 1', hyperid=911, ts=911),
            Model(title='ModelMetaFormulas model 2', hyperid=912, ts=912),
            Model(title='ModelMetaFormulas model 3', hyperid=913, ts=913),
            Model(title='ModelMetaFormulas model 4', hyperid=914, ts=914),
        ]
        cls.index.offers += [
            Offer(title='ModelMetaFormulas offer 1', hyperid=911),
            Offer(title='ModelMetaFormulas offer 2', hyperid=912),
            Offer(title='ModelMetaFormulas offer 3', hyperid=913),
            Offer(title='ModelMetaFormulas offer 4', hyperid=914),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 911).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 912).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 913).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 914).respond(0.4)

        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 911).respond(0.4)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 912).respond(0.3)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 913).respond(0.2)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 914).respond(0.1)

        cls.matrixnet.on_place(MnPlace.MODEL_WIZARD_META, 914).respond(0.3)

    def test_implicit_model_meta_formula(self):
        """Проверяем, что при заданных флагах неявная модель не отдается,
        если сумма значений формулы по топ-3 моделям
        меньше указанного порога.
        https://st.yandex-team.ru/MARKETOUT-17852
        https://st.yandex-team.ru/MARKETOUT-18306
        """
        request = 'place=parallel&text=ModelMetaFormulas&debug=1'

        # без флагов (проверяем формулу по умолчанию)
        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response, 'Using implicit white model wizard meta MatrixNet formula: MNA_fml_formula_785353'
        )

        # порог выставлен, есть флаг включения, порог не пройден
        response = self.report.request_bs(
            request + '&rearr-factors=market_implicit_model_wizard_meta_mn_algo=MNA_mn201477;'
            'market_implicit_model_wizard_meta_threshold=0.7;'
        )
        self.assertFragmentNotIn(response, {'market_implicit_model': []})
        self.assertFragmentIn(response, 'Using implicit white model wizard meta MatrixNet formula: MNA_mn201477')

        # порог выставлен, есть флаг включения, порог пройден
        response = self.report.request_bs(
            request + '&rearr-factors=market_implicit_model_wizard_meta_mn_algo=MNA_mn201477;'
            'market_implicit_model_wizard_meta_threshold=0.5;'
        )
        self.assertFragmentIn(response, {'market_implicit_model': []})
        self.assertFragmentIn(response, 'Using implicit white model wizard meta MatrixNet formula: MNA_mn201477')

    def test_implicit_mode_wizard_meta_relevance_in_market_factors(self):
        """Проверка добавления значения формулы колдунщика неявной модели на мете
        в market_factors для проброса на верхний
        https://st.yandex-team.ru/MARKETOUT-18351
        """
        # порог пройден, значение добавляется
        response = self.report.request_bs(
            'place=parallel&text=ModelMetaFormulas'
            '&rearr-factors=market_implicit_model_wizard_meta_mn_algo=MNA_mn201477;'
            'market_implicit_model_wizard_meta_threshold=0.1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ImplicitModelWizardRelevance": NotEmpty(),
                    }
                ]
            },
        )

        # порог не пройден, значение добавляется
        response = self.report.request_bs(
            'place=parallel&text=ModelMetaFormulas'
            '&rearr-factors=market_implicit_model_wizard_meta_mn_algo=MNA_mn201477;'
            'market_implicit_model_wizard_meta_threshold=1.0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ImplicitModelWizardRelevance": NotEmpty(),
                    }
                ]
            },
        )

        # порог не задан, значение добавляется
        response = self.report.request_bs(
            'place=parallel&text=ModelMetaFormulas'
            '&rearr-factors=market_implicit_model_wizard_meta_mn_algo=MNA_mn201477'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ImplicitModelWizardRelevance": NotEmpty(),
                    }
                ]
            },
        )

    def test_model_wizard_meta_formula(self):
        """Проверяем, что при заданных флагах модельный колдунщик не отдается,
        если значение формулы у модели меньше указанного порога.
        https://st.yandex-team.ru/MARKETOUT-17852
        https://st.yandex-team.ru/MARKETOUT-18306
        """
        request = 'place=parallel&text=ModelMetaFormulas&debug=1&rearr-factors=market_parallel_use_collapsing=0'

        # без флагов (проверяем формулу по умолчанию)
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, 'Using model wizard meta MatrixNet formula: MNA_fml_formula_785353')

        # порог выставлен, есть флаг включения, порог не пройден
        response = self.report.request_bs(
            request + '&rearr-factors=market_model_wizard_meta_mn_algo=MNA_mn201477;'
            'market_model_wizard_meta_threshold=0.4;'
        )
        self.assertFragmentNotIn(response, {'market_model': []})
        self.assertFragmentIn(response, 'Using model wizard meta MatrixNet formula: MNA_mn201477')

        # порог выставлен, есть флаг включения, порог пройден
        response = self.report.request_bs(
            request + '&rearr-factors=market_model_wizard_meta_mn_algo=MNA_mn201477;'
            'market_model_wizard_meta_threshold=0.2;'
        )
        self.assertFragmentIn(response, {'market_model': []})
        self.assertFragmentIn(response, 'Using model wizard meta MatrixNet formula: MNA_mn201477')

    def test_mode_wizard_meta_relevance_in_market_factors(self):
        """Проверка добавления значения формулы модельного колдунщика на мете
        в market_factors для проброса на верхний
        https://st.yandex-team.ru/MARKETOUT-18351
        """
        # порог пройден, значение добавляется
        response = self.report.request_bs(
            'place=parallel&text=ModelMetaFormulas'
            '&rearr-factors=market_model_wizard_meta_mn_algo=MNA_mn201477;'
            'market_model_wizard_meta_threshold=0.1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ModelWizardRelevance": NotEmpty(),
                    }
                ]
            },
        )

        # порог не пройден, значение добавляется
        response = self.report.request_bs(
            'place=parallel&text=ModelMetaFormulas'
            '&rearr-factors=market_model_wizard_meta_mn_algo=MNA_mn201477;'
            'market_model_wizard_meta_threshold=1.0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ModelWizardRelevance": NotEmpty(),
                    }
                ]
            },
        )

        # порог не задан, значение добавляется
        response = self.report.request_bs(
            'place=parallel&text=ModelMetaFormulas' '&rearr-factors=market_model_wizard_meta_mn_algo=MNA_mn201477'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ModelWizardRelevance": NotEmpty(),
                    }
                ]
            },
        )

    @classmethod
    def prepare_vendors_in_ext_category_wizard(cls):
        """Подготовка данных для проверки вендорного колдунщика под видом колдунщика расширенной категории"""
        categories_count = 10
        models_count = 4

        # Добавляем вендора
        cls.index.vendors += [
            Vendor(
                vendor_id=800,
                name='xiaomi',
                hids=[120 + i for i in range(categories_count)],
                hyperids=[1200 + i for i in range(models_count)],
            )
        ]

        # Создаем карточку вендора
        cls.index.cards += [CardVendor(vendor_id=800, model_count=models_count)]

        # Создаем модели
        cls.index.models += [
            Model(hid=120, vendor_id=800, hyperid=1200 + i, title='Top model {0}'.format(i))
            for i in range(models_count)
        ]

        # Добавляем по офферу к моделям
        cls.index.offers += [Offer(hyperid=1200 + i, price=100) for i in range(models_count)]

    def test_wizards_offercard_urls(self):
        """Проверяем наличие урла на страницу офера
        в оферной и модельной врезках.
        https://st.yandex-team.ru/MARKETOUT-18195
        Проверяем, что у него есть clid: https://st.yandex-team.ru/MARKETOUT-18840
        Проверяем, что в &rs есть pp=106: https://st.yandex-team.ru/MARKETOUT-25838
        """

        def test_logs(waremd5, rs, clid, is_touch):
            show_block_id = "048841920011177788888"
            base_show_uid = "{}{:02}{:02}".format(
                show_block_id, ClickType.OFFERCARD, 0
            )  # 1 symbol left for generating final

            host = '{}market.yandex.ru'.format('m.' if is_touch else '')
            for i in range(len(waremd5)):
                offercard_url = "//{host}/offer/{ware_md5}?rs={rs}&clid={clid}&lr=213".format(
                    host=host, ware_md5=waremd5[i], rs=rs[i], clid=clid
                )

                self.show_log.expect(
                    url=LikeUrl.of(offercard_url),
                    show_block_id=show_block_id,
                    show_uid=base_show_uid + str(i + 1),
                    ware_md5=waremd5[i],
                )

                self.click_log.expect(
                    data_url=LikeUrl.of(offercard_url, unquote=True),
                    ware_md5=waremd5[i],
                    dtype='offercard',
                )

        # market_offers_wizard
        request = (
            "place=parallel"
            "&text=pelmen"
            "&subreqid=ololo"
            "&test-buckets=1,2,3"
            "&rids=213"
            "&rearr-factors=market_offers_wizard_incut_url_type=External"
        )
        response = self.report.request_bs(request)

        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pelmen-1", "raw": True}},
                                        "url": "http://pelmennaya.ru/pelmens?id=1",
                                        "urlTouch": "http://pelmennaya.ru/pelmens?id=1",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pelmen-2", "raw": True}},
                                        "url": "http://pelmennaya.ru/pelmens?id=2",
                                        "urlTouch": "http://pelmennaya.ru/pelmens?id=2",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "pelmen-3", "raw": True}},
                                        "url": "http://pelmennaya.ru/pelmens?id=3",
                                        "urlTouch": "http://pelmennaya.ru/pelmens?id=3",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "thumb": {
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                },
                            ]
                        }
                    }
                ]
            },
        )

        waremd5 = ["09lEaAKkQll1XTjm0WPoIA", "xMpCOKC5I4INzFCab3WEmQ", "lpc2G9gcBPtOqJHWMQSlow"]

        # The hardcoded value rs[0] represents the following:
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJyzEuaYyizEaqhjpGMswajEmp8DhAAmggRY")))' | protoc --decode_raw
        # 1: 106 (offercard)
        # 2: "1,2,3" (test-buckets from the request)
        # 3: 1 (this offer's position; 2, 3, 4 for the rest)
        # 4: "ololo" (subreqid from the request)
        rs = [
            "eJyzEuLIEmI11DHSMZZgVGLNzwFCACJ2BCk,",
            "eJyzEuLIEmI11DHSMZZgUmLNzwFCACJ-BCo,",
            "eJyzEuLIEmI11DHSMZZgVmLNzwFCACKGBCs,",
        ]

        test_logs(waremd5, rs, clid=913, is_touch=False)

        # touch
        self.report.request_bs(request + "&touch=1&rearr-factors=offers_touch=1")
        test_logs(waremd5, rs, clid=919, is_touch=True)

        # market_model
        request = "place=parallel" "&text=shashlyk+mashlyk" "&subreqid=ololo" "&test-buckets=1,2,3" "&rids=213"
        response = self.report.request_bs(request)

        waremd5 = ["sCYyTGkEsqnLS4jW1hyB0Q", "iXMWkpF2Rk68mtCF8x5yhA"]

        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"raw": True, "text": "Shashlyk mashlyk"}},
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"raw": True, "text": "shashlyk mashlyk 2"}},
                                        "url": "http://shashlychnaya.ru/shashlyk?id=2",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "offerId": waremd5[0],
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"raw": True, "text": "shashlyk mashlyk 1"}},
                                        "url": "http://shashlychnaya.ru/shashlyk?id=1",
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                    },
                                    "offerId": waremd5[1],
                                },
                            ]
                        },
                    }
                ]
            },
            preserve_order=False,
        )

        # waremd5 = ["sCYyTGkEsqnLS4jW1hyB0Q", "iXMWkpF2Rk68mtCF8x5yhA"]
        # The hardcoded value rs[0] represents the following:
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJyz4uOYwizEaqhjpGMswajEAAAUtwIo")))' | protoc --decode_raw
        # 1: 106 (offer card)
        # 2: "1,2,3" (test-buckets from the request)
        # 3: 1 (this offer's position; 2 for another)
        # 4: "" (subreqid from the request)
        rs = ["eJyz4uXIEmI11DHSMZZgVGIAABGoAfo,", "eJyz4uXIEmI11DHSMZZgUmIAABGrAfs,"]
        test_logs(waremd5, rs, clid=914, is_touch=False)

        # touch
        self.report.request_bs(request + "&touch=1")
        test_logs(waremd5, rs, clid=920, is_touch=True)

    def test_raw_offercard_urls(self):
        """Проверяем, что при включенном флаге market_adg_offer_url_type=DirectOfferCard
        отдаем сырые урлы на карточку офера.
        При флаге market_adg_offer_url_type=Direct отдаем прямые урлы на магазин.
        https://st.yandex-team.ru/MARKETOUT-19621
        https://st.yandex-team.ru/MARKETOUT-26492
        """
        # market_offers_wizard
        request = 'place=parallel&text=pelmen&rids=213'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                                },
                                "thumb": {
                                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                                },
                            }
                        ],
                    },
                }
            },
        )
        response = self.report.request_bs_pb(request + '&rearr-factors=market_adg_offer_url_type=DirectOfferCard')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": LikeUrl.of(
                                        "//market.yandex.ru/offer/09lEaAKkQll1XTjm0WPoIA?clid=913&lr=213"
                                    ),
                                },
                                "thumb": {
                                    "offercardUrl": LikeUrl.of(
                                        "//market.yandex.ru/offer/09lEaAKkQll1XTjm0WPoIA?clid=913&lr=213"
                                    ),
                                },
                            }
                        ],
                    },
                }
            },
        )
        response = self.report.request_bs_pb(request + '&rearr-factors=market_adg_offer_url_type=Direct')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": "http://pelmennaya.ru/pelmens?id=1",
                                },
                                "thumb": {
                                    "offercardUrl": "http://pelmennaya.ru/pelmens?id=1",
                                },
                            }
                        ],
                    },
                }
            },
        )

        # market_model
        request = 'place=parallel&text=shashlyk+mashlyk&rids=213'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": Contains("/redir/dtype=offercard/"),
                                }
                            }
                        ]
                    }
                }
            },
        )
        response = self.report.request_bs_pb(request + '&rearr-factors=market_adg_offer_url_type=DirectOfferCard')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": LikeUrl.of(
                                        "//market.yandex.ru/offer/iXMWkpF2Rk68mtCF8x5yhA?clid=914&lr=213"
                                    ),
                                }
                            }
                        ]
                    }
                }
            },
        )
        response = self.report.request_bs_pb(request + '&rearr-factors=market_adg_offer_url_type=Direct')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": "http://shashlychnaya.ru/shashlyk?id=1",
                                }
                            }
                        ]
                    }
                }
            },
        )

    def test_text_in_offercard_urls(self):
        """Проверяем, что добавляется текст запроса в урл карточки офера в оферном колдунщике.
        https://st.yandex-team.ru/MARKETOUT-20296
        https://st.yandex-team.ru/MARKETOUT-24340
        https://st.yandex-team.ru/MARKETOUT-30915
        """

        def test_logs(waremd5, text_param, subreqid):
            show_block_id = "048841920011177788888"
            base_show_uid = "{}{:02}{:02}".format(
                show_block_id, ClickType.OFFERCARD, 0
            )  # 1 symbol left for generating final

            no_params = [] if text_param else ['text']
            for i in range(len(waremd5)):
                offercard_url = "//market.yandex.ru/offer/{ware_md5}?{text_param}".format(
                    ware_md5=waremd5[i], text_param=text_param
                )

                self.show_log.expect(
                    url=LikeUrl.of(offercard_url, no_params=no_params),
                    show_block_id=show_block_id,
                    show_uid=base_show_uid + str(i + 1),
                    ware_md5=waremd5[i],
                    subrequest_id=subreqid,
                )

                self.click_log.expect(
                    data_url=LikeUrl.of(offercard_url, unquote=True, no_params=no_params),
                    ware_md5=waremd5[i],
                    dtype='offercard',
                    sub_request_id=subreqid,
                )

        # market_offers_wizard
        query = 'pelmen'
        waremd5 = ["09lEaAKkQll1XTjm0WPoIA", "xMpCOKC5I4INzFCab3WEmQ", "lpc2G9gcBPtOqJHWMQSlow"]

        for subreqid, rearr, text_param in (
            ('1', '&rearr-factors=device=desktop', 'text={}'.format(query)),
            (
                '2',
                '&rearr-factors=device=touch;offers_touch=1;market_offers_wiz_top_offers_meta_threshold=0',
                'text={}'.format(query),
            ),
        ):
            request = "place=parallel&text={}&rids=213{}&subreqid={}".format(query, rearr, subreqid)
            response = self.report.request_bs(request)
            self.assertFragmentIn(response, {"market_offers_wizard": []})
            test_logs(waremd5, text_param, subreqid)

    def test_modelwizardoffers_prun_count_tbs_value(self):
        """Проверяем, что флаги market_model_wizard_offers_prun_count и
        market_model_wizard_offers_tbs_value меняют prun-count и tbs-value
        в дозапросе place=modelwizardoffers модельного колдунщика.
        https://st.yandex-team.ru/MARKETOUT-18622
        https://st.yandex-team.ru/MARKETOUT-18665
        """
        request = 'place=parallel&text=pepelac+2000&rids=213&debug=1'

        # без указания prun-count tbs-value не применяется
        for rearrs in ('', '&rearr-factors=market_model_wizard_offers_tbs_value=25000;'):
            response = self.report.request_bs(request + rearrs)
            debug_xml = response.extract_debug_response()
            self.assertFragmentIn(
                debug_xml,
                '''
                <debug name="TModelWizardsOffers">
                    <report>
                        <context>
                            <request-params>
                                <collection name="*">
                                    <param name="pron">
                                        <value>tbs9000000</value>
                                    </param>
                                </collection>
                            </request-params>
                        </context>
                    </report>
                </debug>
            ''',
            )
            self.assertFragmentNotIn(
                debug_xml,
                '''
                <debug name="TModelWizardsOffers">
                    <report>
                        <context>
                            <request-params>
                                <collection name="*">
                                    <param name="pron">
                                        <value>pruncount334</value>
                                    </param>
                                </collection>
                            </request-params>
                        </context>
                    </report>
                </debug>
            ''',
            )

        response = self.report.request_bs(
            request + '&rearr-factors=market_model_wizard_offers_prun_count=500;'
            'market_model_wizard_offers_tbs_value=25000;'
        )
        debug_xml = response.extract_debug_response()
        self.assertFragmentIn(
            debug_xml,
            '''
            <debug name="TModelWizardsOffers">
                <report>
                    <context>
                        <request-params>
                            <collection name="*">
                                <param name="pron">
                                    <value>pruncount334</value>
                                    <value>tbs25000</value>
                                </param>
                            </collection>
                        </request-params>
                    </context>
                </report>
            </debug>
        ''',
        )

    @classmethod
    def prepare_modelwizardoffers_with_pictures_only(cls):
        """Создаем модель с оферами с картинкой и без.
        https://st.yandex-team.ru/MARKETOUT-23853
        """
        cls.index.models += [
            Model(hyperid=1300, title='ModelWizardOffersPicTest'),
        ]
        cls.index.shops += [
            Shop(fesh=50, priority_region=213),
            Shop(fesh=51, priority_region=213),
        ]
        cls.index.offers += [
            Offer(title='ModelWizardOffersPicTest', hyperid=1300, fesh=50, no_picture=True),
            Offer(
                title='ModelWizardOffersPicTest',
                hyperid=1300,
                fesh=51,
                picture=Picture(group_id=1234, picture_id='iyC3nHslqLtqZJLygVAHeA', width=100, height=100),
            ),
        ]

    def test_modelwizardoffers_with_pictures_only(self):
        """Проверяем, что с флагом market_model_wizard_offers_with_pictures_only
        отфильтровываются оферы без картинок
        в дозапросе place=modelwizardoffers модельного колдунщика.
        https://st.yandex-team.ru/MARKETOUT-23853
        """
        request = 'place=parallel&text=ModelWizardOffersPicTest&rids=213'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {"thumb": {"source": ""}},
                            {
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_iyC3nHslqLtqZJLygVAHeA/100x100"
                                    )
                                }
                            },
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_bs_pb(request + '&rearr-factors=market_model_wizard_offers_with_pictures_only=1')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_iyC3nHslqLtqZJLygVAHeA/100x100"
                                    )
                                }
                            }
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

    def test_offers_wizard_ad_g(self):
        """Проверяем наличие урлов с клидами для Рекламной Галереи
        у оферного колдунщика.
        https://st.yandex-team.ru/MARKETOUT-18840
        """
        # market_check_offers_incut_size=0 отключает проверку на хотя бы 4 офера
        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka&rids=213&rearr-factors=market_check_offers_incut_size=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=545"),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=913"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=708"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=919"),
                    "snippetUrl": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=545"),
                    "snippetAdGUrl": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=913"),
                    "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=708"),
                    "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=919"),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=545"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru?clid=913"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=708"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru?clid=919"),
                            "snippetUrl": LikeUrl.of("//market.yandex.ru?clid=545"),
                            "snippetAdGUrl": LikeUrl.of("//market.yandex.ru?clid=913"),
                            "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru?clid=708"),
                            "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru?clid=919"),
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=545"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=913"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=708"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=919"),
                            "snippetUrl": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=545"),
                            "snippetAdGUrl": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=913"),
                            "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=708"),
                            "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=919"),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=545"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=kiyanka&clid=913"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=708"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=kiyanka&clid=919"),
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A?text=kiyanka&lr=213&hid=130&nid=130&rs=eJyz4uSYyizEIMGoxAAACPsBMQ%2C%2C&clid=545"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&lr=213&cvredirect=0&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2&clid=913"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A?text=kiyanka&lr=213&hid=130&nid=130&rs=eJyz4uSYyizEIMGoxAAACPsBMQ%2C%2C&clid=708"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&lr=213&cvredirect=0&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2&clid=919"
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A?text=kiyanka&lr=213&hid=130&nid=130&rs=eJyz4uSYyizEIMGoxAAACPsBMQ%2C%2C&clid=545"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&lr=213&cvredirect=0&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2&clid=913"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A?text=kiyanka&lr=213&hid=130&nid=130&rs=eJyz4uSYyizEIMGoxAAACPsBMQ%2C%2C&clid=708"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//market.yandex.ru/search?text=kiyanka&lr=213&cvredirect=0&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2&clid=919"
                                    ),
                                },
                                "greenUrl": {
                                    "url": LikeUrl.of("//market.yandex.ru/shop--shop-100/100/reviews?clid=545"),
                                    "adGUrl": LikeUrl.of("//market.yandex.ru/shop--shop-100/100/reviews?clid=913"),
                                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/grades-shop.xml?shop_id=100&clid=708"),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/grades-shop.xml?shop_id=100&clid=919"
                                    ),
                                },
                            },
                        ],
                    },
                }
            },
        )

    def test_implicit_model_ad_g(self):
        """Проверяем наличие урлов с клидами для Рекламной Галереи
        у колдунщика неявной модели.
        https://st.yandex-team.ru/MARKETOUT-18840
        """
        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=panasonic%20tumix&clid=698"),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=panasonic%20tumix&clid=915"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921"),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=698"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru?clid=915"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=721"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru?clid=921"),
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=panasonic%20tumix&clid=698"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=panasonic%20tumix&clid=915"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921"),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=panasonic%20tumix&clid=698"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=panasonic%20tumix&clid=915"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921"),
                        },
                    ],
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=698"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=721"
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=915"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=921"
                            ),
                            "text": "Отзывы",
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/geo?text=panasonic%20tumix&lr=0&clid=698"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/geo?text=panasonic%20tumix&lr=0&clid=721"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/geo?text=panasonic%20tumix&lr=0&clid=915"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/geo?text=panasonic%20tumix&lr=0&clid=921"),
                            "text": "На карте",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=698"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=721"
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=915"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=921"
                            ),
                            "text": "С доставкой завтра",
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=698"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=915"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=721"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=921"
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=698"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=915"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=721"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=921"
                                    ),
                                },
                                "reviews": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?clid=698"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?clid=915"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?clid=721"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?clid=921"
                                    ),
                                },
                            },
                        ],
                    },
                }
            },
        )

    def test_ext_category_ad_g(self):
        """Проверяем наличие урлов с клидами для Рекламной Галереи
        у колдунщика гуру-категории.
        https://st.yandex-team.ru/MARKETOUT-18840
        """
        response = self.report.request_bs_pb("place=parallel&text=food")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": {
                    "url": LikeUrl.of("//market.yandex.ru/catalog--food/100?hid=100&clid=500"),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/catalog--food/100?hid=100&clid=916"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=100&clid=707"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=100&clid=922"),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=500"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru?clid=916"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=707"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru?clid=922"),
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/catalog--food/100?hid=100&clid=500"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/catalog--food/100?hid=100&clid=916"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=100&clid=707"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=100&clid=922"),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/catalog--food/100?hid=100&clid=500"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/catalog--food/100?hid=100&clid=916"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=100&clid=707"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?hid=100&clid=922"),
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&clid=500"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&clid=916"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&clid=707"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&clid=922"
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&clid=500"
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&clid=916"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&clid=707"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/catalog--food-bakery/100/list?glfilter=7893318%3A10&hid=100&clid=922"
                                    ),
                                },
                            },
                        ],
                    },
                }
            },
        )

    def test_model_ad_g(self):
        """Проверяем наличие урлов с клидами для Рекламной Галереи
        у модельного колдунщика.
        https://st.yandex-team.ru/MARKETOUT-18840
        """
        # одиночная модель
        for addit_cgis, ad_g_clid in (
            ('&rearr-factors=device=desktop', 914),
            ('&touch=1&rearr-factors=device=touch', 920),
        ):
            response = self.report.request_bs_pb('place=parallel&text=lenovo+p780&rids=213' + addit_cgis)
            self.assertFragmentIn(
                response,
                {
                    "market_model": {
                        "url": LikeUrl.of("//market.yandex.ru/product--lenovo-p780/301090?clid=502&hid=200&nid=2000"),
                        "adGUrl": LikeUrl.of(
                            "//market.yandex.ru/product--lenovo-p780/301090?clid=914&hid=200&nid=2000"
                        ),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090?clid=704&hid=200&nid=2000"
                        ),
                        "adGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090?clid=920&hid=200&nid=2000"
                        ),
                        "adGMoreUrl": LikeUrl.of("//market.yandex.ru/search?text=lenovo%20p780&clid={}".format(914)),
                        "offersUrl": LikeUrl.of(
                            "//market.yandex.ru/product--lenovo-p780/301090/offers?clid=502&grhow=shop&hid=200&hyperid=301090&nid=2000"
                        ),
                        "offersUrlAdG": LikeUrl.of(
                            "//market.yandex.ru/product--lenovo-p780/301090/offers?clid=914&grhow=shop&hid=200&hyperid=301090&nid=2000"
                        ),
                        "offersUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090?clid=704&grhow=shop&hid=200&nid=2000"
                        ),
                        "offersUrlAdGTouch": LikeUrl.of(
                            "//m.market.yandex.ru/product--lenovo-p780/301090?clid=920&grhow=shop&hid=200&nid=2000"
                        ),
                        "showcase": {
                            "items": [
                                {
                                    "greenUrl": {
                                        "url": LikeUrl.of("//market.yandex.ru/shop--shop-1/1/reviews?clid=502"),
                                        "adGUrl": LikeUrl.of("//market.yandex.ru/shop--shop-1/1/reviews?clid=914"),
                                    }
                                }
                            ]
                        },
                        "greenUrl": [
                            {
                                "url": LikeUrl.of("//market.yandex.ru?clid=502"),
                                "adGUrl": LikeUrl.of("//market.yandex.ru?clid=914"),
                            }
                        ],
                        "sitelinks": [
                            {
                                "text": "specs",
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/product--lenovo-p780/301090/spec?clid=914&hid=200&nid=2000"
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090/spec?clid=920&hid=200&nid=2000"
                                ),
                            },
                            {
                                "text": "prices",
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/product--lenovo-p780/301090/offers?clid=502&grhow=shop&hid=200&hyperid=301090&nid=2000"
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/product--lenovo-p780/301090/offers?clid=914&grhow=shop&hid=200&hyperid=301090&nid=2000"
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090?clid=704&grhow=shop&hid=200&nid=2000"
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090?clid=920&grhow=shop&hid=200&nid=2000"
                                ),
                            },
                            {
                                "text": "opinions",
                                "url": LikeUrl.of("//market.yandex.ru/product--lenovo-p780/301090/reviews?clid=502"),
                                "adGUrl": LikeUrl.of("//market.yandex.ru/product--lenovo-p780/301090/reviews?clid=914"),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090/reviews?clid=704"
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090/reviews?clid=920"
                                ),
                            },
                            {
                                "text": "articles",
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/product--lenovo-p780/301090/articles?clid=502&hid=200&nid=2000"
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/product--lenovo-p780/301090/articles?clid=914&hid=200&nid=2000"
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090/reviews?clid=704&hid=200&nid=2000"
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090/reviews?clid=920&hid=200&nid=2000"
                                ),
                            },
                            {
                                "text": "forums",
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/product--lenovo-p780/301090/forum?clid=502&hid=200&nid=2000"
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/product--lenovo-p780/301090/forum?clid=914&hid=200&nid=2000"
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090?clid=704&hid=200&nid=2000"
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/product--lenovo-p780/301090?clid=920&hid=200&nid=2000"
                                ),
                            },
                        ],
                    }
                },
            )

    @classmethod
    def prepare_models_wiz_reasons_to_buy(cls):
        """Добавляем reasons_to_buy для одиночных моделей 101, 102 и
        для групповой 699.
        В одном сплите валидный JSON, в другом нет.
        """

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition("split=1")
            .add(
                hyperid=101,
                reasons=[
                    {
                        "factor_name": "Мощный процессор",
                        "type": "consumerFactor",
                        "factor_priority": "1",
                    },
                    {
                        "factor_name": "Объем памяти",
                        "type": "consumerFactor",
                        "factor_priority": "2",
                    },
                    {
                        "type": "consumerFactor",
                        "id": "positive_feedback",
                        "author_puid": "26642513",
                        "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                    },
                ],
            )
            .add(
                hyperid=102,
                reasons=[
                    {
                        "factor_name": "Размер",
                        "type": "consumerFactor",
                        "factor_priority": "1",
                    },
                    {
                        "factor_name": "Мощный процессор",
                        "type": "consumerFactor",
                        "factor_priority": "2",
                    },
                    {
                        "type": "consumerFactor",
                        "id": "positive_feedback",
                        "author_puid": "374734804",
                        "text": "Очень хорошо держит аккумулятор, использую достаточно интенсивно",
                    },
                ],
            )
            .add(
                hyperid=699,
                reasons=[
                    {
                        "factor_name": "Надежность",
                        "type": "consumerFactor",
                        "factor_priority": "1",
                    }
                ],
            )
            .add(
                hyperid=771,
                reasons=[  # for test_market_offers_reasons_to_buy
                    {
                        "factor_name": "Красивый",
                        "type": "consumerFactor",
                        "factor_priority": "1",
                    },
                    {
                        "factor_name": "Добрый",
                        "type": "consumerFactor",
                        "factor_priority": "2",
                    },
                ],
            )
            .new_partition("split=2")
            .add(hyperid=101, reasons="{123")  # invalid json
        ]

        cls.blackbox.on_request(uids=['26642513', '374734804']).respond(
            [
                BlackboxUser(uid='26642513', name='name 1', avatar='avatar_1'),
                BlackboxUser(uid='374734804', name='name 2', avatar='avatar_2'),
            ]
        )

    def test_model_wiz_reasons_to_buy(self):
        """Проверяем блок reasons_to_buy в модельном колдунщике.
        https://st.yandex-team.ru/MARKETOUT-18877
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix+5000&rearr-factors=split=1;market_enable_model_wizard_right_incut=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model_right_incut": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix%205000&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=632"
                    ),
                    "reasonsToBuy": [
                        {
                            "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                            "type": "consumerFactor",
                        },
                        {
                            "factor_name": "Объем памяти",
                            "type": "consumerFactor",
                            "factor_priority": "2",
                        },
                        {
                            "factor_name": "Мощный процессор",
                            "type": "consumerFactor",
                            "factor_priority": "1",
                        },
                    ],
                }
            },
        )

        # У модели 101 в split=2 reasons_to_buy невалидный
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix+5000&rearr-factors=split=2;market_enable_model_wizard_right_incut=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model_right_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101?clid=632&hid=30&nid=10"),
                    "reasonsToBuy": EmptyList(),
                }
            },
        )

        # У модели 102 в split=2 нет reasons_to_buy
        response = self.report.request_bs_pb(
            'place=parallel&text=pt6000&rearr-factors=split=2;market_enable_model_wizard_right_incut=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model_right_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-pt6000/102?clid=632&hid=30&nid=10"),
                    "reasonsToBuy": EmptyList(),
                }
            },
        )

    def test_implicit_model_reasons_to_buy(self):
        """Проверяем блок reasonsToBuy в колдунщике неявной модели.
        https://st.yandex-team.ru/MARKETOUT-22188
        https://st.yandex-team.ru/MARKETOUT-30671
        https://st.yandex-team.ru/MARKETOUT-36869
        """
        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix&rearr-factors=device=desktop;split=1')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?clid=698&hid=30&nid=10"
                                    )
                                },
                                "reasonsToBuy": [
                                    {
                                        "factor_name": "Мощный процессор",
                                        "type": "consumerFactor",
                                        "factor_priority": "1",
                                    },
                                    {
                                        "factor_name": "Объем памяти",
                                        "type": "consumerFactor",
                                        "factor_priority": "2",
                                    },
                                    {
                                        "id": "positive_feedback",
                                        "type": "consumerFactor",
                                        "author_puid": "26642513",
                                        "author_name": "name 1",
                                        "author_avatar": "avatar_1",
                                        "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                                    },
                                ],
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-pt6000/102?clid=698&hid=30&nid=10"
                                    )
                                },
                                "reasonsToBuy": [
                                    {
                                        "factor_name": "Размер",
                                        "type": "consumerFactor",
                                        "factor_priority": "1",
                                    },
                                    {
                                        "factor_name": "Мощный процессор",
                                        "type": "consumerFactor",
                                        "factor_priority": "2",
                                    },
                                    {
                                        "id": "positive_feedback",
                                        "type": "consumerFactor",
                                        "author_puid": "374734804",
                                        "author_name": "name 2",
                                        "author_avatar": "avatar_2",
                                        "text": "Очень хорошо держит аккумулятор, использую достаточно интенсивно",
                                    },
                                ],
                            },
                        ]
                    }
                }
            },
        )

        # У модели 101 в split=2 reasonsToBuy невалидный, у 102 - нет информации
        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix&rearr-factors=split=2')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?clid=698&hid=30&nid=10"
                                    )
                                },
                                "reasonsToBuy": EmptyList(),
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-pt6000/102?clid=698&hid=30&nid=10"
                                    )
                                },
                                "reasonsToBuy": EmptyList(),
                            },
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_wizard_title_highlight(cls):
        """Подготовка данных для проверки подсветки тайтла офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-35243
        """
        cls.index.offers += [
            Offer(title="Ночник черепашка 1", fesh=157, hyperid=1),
            Offer(title="Ночник детский 2", fesh=158, hyperid=2, descr="ночник музыкальный черепашка"),
        ]

        cls.index.shops += [
            Shop(fesh=157),
            Shop(fesh=158),
        ]

        cls.index.models += [
            Model(hyperid=1, title="Черепашка model 1", description="музыкальный ночник"),
            Model(hyperid=2, title="Ночник черепашка model 2"),
        ]

    def test_wizard_title_highlight(self):
        """Проверка подсветки тайтла для офферного и неявного колдунщика
        https://st.yandex-team.ru/MARKETOUT-35243
        Выкачено на 100% здесь:
        https://st.yandex-team.ru/MARKETOUT-38989
        """
        # Запрос без флага market_wizard_title_highlight
        response = self.report.request_bs(
            'place=parallel&text=ночник+черепашка+детский+музыкальный'
            '&rearr-factors=market_offers_wiz_top_offers_threshold=0;showcase_universal_model=1;market_enable_model_offers_wizard=1;market_wizard_title_highlight=0'
        )

        # Офферный колдунщик
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {"title": {"__hl": {"text": "Ночник черепашка детский музыкальный", "raw": True}}}
                ]
            },
        )
        # Неявный колдунщик
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {"title": {"__hl": {"text": "Ночник черепашка детский музыкальный", "raw": True}}}
                ]
            },
        )
        # Модельно-офферный колдунщик
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": [
                    {
                        "title": {"__hl": {"text": "Ночник черепашка детский музыкальный", "raw": True}},
                        "cartUrl": LikeUrl.of(
                            "//pokupki.market.yandex.ru/my/cart?purchase-referrer=market_wizard&clid=545"
                        ),
                        "cartUrlTouch": LikeUrl.of(
                            "//m.pokupki.market.yandex.ru/my/cart?purchase-referrer=market_wizard&clid=708"
                        ),
                    }
                ]
            },
        )

        # Запрос с флагом market_wizard_title_highlight
        response = self.report.request_bs(
            'place=parallel&text=ночник+черепашка+детский+музыкальный'
            '&rearr-factors=market_offers_wiz_top_offers_threshold=0;'
            'showcase_universal_model=1;market_enable_model_offers_wizard=1'
        )

        # Офферный колдунщик под флагом market_wizard_title_highlight
        self.assertFragmentIn(
            response, {"market_offers_wizard": [{"title": "\7[Ночник черепашка детский музыкальный\7]"}]}
        )
        # Неявный колдунщик под флагом market_wizard_title_highlight
        self.assertFragmentIn(
            response, {"market_implicit_model": [{"title": "\7[Ночник черепашка\7] детский \7[музыкальный\7]"}]}
        )
        # Модельно-офферный колдунщик под флагом market_wizard_title_highlight
        self.assertFragmentIn(
            response, {"market_model_offers_wizard": [{"title": "\7[Ночник черепашка\7] детский \7[музыкальный\7]"}]}
        )

    def test_modelstatisticsplace_prun_count_tbs_value(self):
        """Проверяем, что флаги market_model_statistics_prun_count и
        market_model_statistics_tbs_value меняют prun-count и tbs-value
        в дозапросе к TModelStatisticsPlace в колдунщике неявной модели.
        https://st.yandex-team.ru/MARKETOUT-20450
        """
        request = 'place=parallel&text=panasonic+tumix&debug=1'

        # без указания prun-count tbs-value не применяется
        for rearrs in ('', '&rearr-factors=market_model_statistics_tbs_value=25000;'):
            response = self.report.request_bs(request + rearrs)
            debug_xml = response.extract_debug_response()
            self.assertFragmentIn(
                debug_xml,
                '''
                <debug name="models real time statistics">
                    <report>
                        <context>
                            <request-params>
                                <collection name="*">
                                    <param name="pron">
                                        <value>tbs9000000</value>
                                    </param>
                                </collection>
                            </request-params>
                        </context>
                    </report>
                </debug>
            ''',
            )
            self.assertFragmentNotIn(
                debug_xml,
                '''
                <debug name="models real time statistics">
                    <report>
                        <context>
                            <request-params>
                                <collection name="*">
                                    <param name="pron">
                                        <value>pruncount334</value>
                                    </param>
                                </collection>
                            </request-params>
                        </context>
                    </report>
                </debug>
            ''',
            )

        response = self.report.request_bs(
            request + '&rearr-factors=market_model_statistics_prun_count=500;'
            'market_model_statistics_tbs_value=25000;'
        )
        debug_xml = response.extract_debug_response()
        self.assertFragmentIn(
            debug_xml,
            '''
            <debug name="models real time statistics">
                <report>
                    <context>
                        <request-params>
                            <collection name="*">
                                <param name="pron">
                                    <value>pruncount334</value>
                                    <value>tbs25000</value>
                                </param>
                            </collection>
                        </request-params>
                    </context>
                </report>
            </debug>
        ''',
        )

    def test_offers_wizard_top_shops(self):
        """Проверяем, что с заданным количеством market_offers_wizard_top_shops_max_count
        выводится топ магазинов.
        https://st.yandex-team.ru/MARKETOUT-20865
        """
        request = 'place=parallel&text=pelmen'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "top_shops_title": Absent(),
                        "top_shops_url": Absent(),
                        "top_shops": Absent(),
                    }
                }
            },
        )

        # отключаем выравнивание врезки, т.к. срабатывает раньше, чем взятие магазинов у оферов
        rearrs = '&rearr-factors=market_offers_incut_align=0;market_offers_wizard_top_shops_max_count={}'

        def top_shops(host_prefix, clid):
            return [
                {
                    "name": "SHOP-400",
                    "url": LikeUrl.of(
                        "//{0}market.yandex.ru/search?text=pelmen&clid={1}&fesh=400".format(host_prefix, clid)
                    ),
                    "rating_to_show": 4.5,
                    "new_grades_count_3m": 123,
                    "overall_grades_count": 789,
                    "reviews_url": LikeUrl.of(
                        "//{0}market.yandex.ru/shop--shop-400/400/reviews?clid={1}".format(host_prefix, clid)
                    ),
                },
                {
                    "name": "SHOP-401",
                    "url": LikeUrl.of(
                        "//{0}market.yandex.ru/search?text=pelmen&clid={1}&fesh=401".format(host_prefix, clid)
                    ),
                    "rating_to_show": 0,  # нет рейтинга и отзывов
                    "new_grades_count_3m": 0,
                    "overall_grades_count": 0,
                    "reviews_url": LikeUrl.of(
                        "//{0}market.yandex.ru/shop--shop-401/401/reviews?clid={1}".format(host_prefix, clid)
                    ),
                },
                {
                    "name": "SHOP-402",
                    "url": LikeUrl.of(
                        "//{0}market.yandex.ru/search?text=pelmen&clid={1}&fesh=402".format(host_prefix, clid)
                    ),
                    "rating_to_show": 4,
                    "new_grades_count_3m": 0,
                    "overall_grades_count": 20,
                    "reviews_url": LikeUrl.of(
                        "//{0}market.yandex.ru/shop--shop-402/402/reviews?clid={1}".format(host_prefix, clid)
                    ),
                },
                {
                    "name": "SHOP-403",
                    "url": LikeUrl.of(
                        "//{0}market.yandex.ru/search?text=pelmen&clid={1}&fesh=403".format(host_prefix, clid)
                    ),
                    "rating_to_show": 3.4,
                    "new_grades_count_3m": 0,
                    "overall_grades_count": 100,
                    "reviews_url": LikeUrl.of(
                        "//{0}market.yandex.ru/shop--shop-403/403/reviews?clid={1}".format(host_prefix, clid)
                    ),
                },
            ]

        for touch_flag, host_prefix, clid in [('', '', 913), ('&touch=1', 'm.', 919)]:
            response = self.report.request_bs_pb(request + rearrs.format(5) + touch_flag)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "top_shops_title": {"__hl": {"text": "pelmen в магазинах на Маркете", "raw": True}},
                            "top_shops_url": LikeUrl.of(
                                "//{0}market.yandex.ru/search?text=pelmen&clid={1}".format(host_prefix, clid)
                            ),
                            "top_shops": top_shops(host_prefix, clid),
                        }
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            response = self.report.request_bs_pb(request + rearrs.format(3) + touch_flag)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "top_shops_title": {"__hl": {"text": "pelmen в магазинах на Маркете", "raw": True}},
                            "top_shops_url": LikeUrl.of(
                                "//{0}market.yandex.ru/search?text=pelmen&clid={1}".format(host_prefix, clid)
                            ),
                            "top_shops": top_shops(host_prefix, clid)[:3],
                        }
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_offers_wizard_top_vendors(self):
        """Проверяем, что с заданным количеством market_offers_wizard_top_vendors_max_count
        выводится топ производителей.
        https://st.yandex-team.ru/MARKETOUT-20945
        """
        request = 'place=parallel&text=pelmen'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "top_vendors": {
                            "title": Absent(),
                            "url": Absent(),
                            "green_title": Absent(),
                            "green_url": Absent(),
                            "all_vendors_title": Absent(),
                            "all_vendors_url": Absent(),
                            "items": EmptyList(),
                        },
                    }
                }
            },
        )

        # отключаем выравнивание врезки, т.к. срабатывает раньше, чем взятие брендов у оферов
        rearrs = '&rearr-factors=market_offers_incut_align=0;market_offers_wizard_top_vendors_max_count={}'

        def top_vendors_items(host_prefix, clid):
            return [
                {
                    "title": {
                        "text": {"__hl": {"text": "PelmenVendor0NoLogo", "raw": True}},
                        "url": LikeUrl.of(
                            "//{0}market.yandex.ru/brands--pelmenvendor0nologo/300?clid={1}".format(host_prefix, clid)
                        ),
                    },
                    "vendor_logo": Absent(),
                },
                {
                    "title": {
                        "text": {"__hl": {"text": "PelmenVendor1", "raw": True}},
                        "url": LikeUrl.of(
                            "//{0}market.yandex.ru/brands--pelmenvendor1/301?clid={1}".format(host_prefix, clid)
                        ),
                    },
                    "vendor_logo": {"source": LikeUrl.of("//avatars.mdst.yandex.net/get-mpic/301/test301/orig")},
                },
                {
                    "title": {
                        "text": {"__hl": {"text": "PelmenVendor2", "raw": True}},
                        "url": LikeUrl.of(
                            "//{0}market.yandex.ru/brands--pelmenvendor2/302?clid={1}".format(host_prefix, clid)
                        ),
                    },
                    "vendor_logo": {"source": LikeUrl.of("//avatars.mdst.yandex.net/get-mpic/302/test302/orig")},
                },
            ]

        for touch_flag, host_prefix, clid in [('', '', 913), ('&touch=1', 'm.', 919)]:
            response = self.report.request_bs_pb(request + rearrs.format(5) + touch_flag)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "top_vendors": {
                                "title": {"__hl": {"text": "Производители на Маркете", "raw": True}},
                                "url": LikeUrl.of("//{0}market.yandex.ru/brands?clid={1}".format(host_prefix, clid)),
                                "green_title": {"__hl": {"text": "Яндекс.Маркет", "raw": True}},
                                "green_url": LikeUrl.of("//{0}market.yandex.ru?clid={1}".format(host_prefix, clid)),
                                "all_vendors_title": {"__hl": {"text": "Больше производителей", "raw": True}},
                                "all_vendors_url": LikeUrl.of(
                                    "//{0}market.yandex.ru/brands?clid={1}".format(host_prefix, clid)
                                ),
                                "items": top_vendors_items(host_prefix, clid),
                            },
                        }
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            response = self.report.request_bs_pb(request + rearrs.format(2) + touch_flag)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "top_vendors": {
                                "title": {"__hl": {"text": "Производители на Маркете", "raw": True}},
                                "url": LikeUrl.of("//{0}market.yandex.ru/brands?clid={1}".format(host_prefix, clid)),
                                "green_title": {"__hl": {"text": "Яндекс.Маркет", "raw": True}},
                                "green_url": LikeUrl.of("//{0}market.yandex.ru?clid={1}".format(host_prefix, clid)),
                                "all_vendors_title": {"__hl": {"text": "Больше производителей", "raw": True}},
                                "all_vendors_url": LikeUrl.of(
                                    "//{0}market.yandex.ru/brands?clid={1}".format(host_prefix, clid)
                                ),
                                "items": top_vendors_items(host_prefix, clid)[:2],
                            },
                        }
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_offers_wizard_sortings(self):
        """Проверяем, что с включенным market_offers_wizard_add_sortings
        выводятся поисковые сортировки.
        https://st.yandex-team.ru/MARKETOUT-20887
        """
        request = 'place=parallel&text=pelmen'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "sortings": Absent(),
                    }
                }
            },
        )

        for touch_flag, host_prefix, clid in [('', '', 913), ('&touch=1', 'm.', 919)]:
            response = self.report.request_bs_pb(
                request + '&rearr-factors=market_offers_wizard_add_sortings=1' + touch_flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "sortings": [
                                {
                                    "name": "aprice",
                                    "url": LikeUrl.of(
                                        "//{0}market.yandex.ru/search?text=pelmen&clid={1}&how=aprice".format(
                                            host_prefix, clid
                                        )
                                    ),
                                    "human_readable_name": "по цене",
                                },
                                {
                                    "name": "discount_p",
                                    "url": LikeUrl.of(
                                        "//{0}market.yandex.ru/search?text=pelmen&clid={1}&how=discount_p".format(
                                            host_prefix, clid
                                        )
                                    ),
                                    "human_readable_name": "по размеру скидки",
                                },
                                {
                                    "name": "quality",
                                    "url": LikeUrl.of(
                                        "//{0}market.yandex.ru/search?text=pelmen&clid={1}&how=quality".format(
                                            host_prefix, clid
                                        )
                                    ),
                                    "human_readable_name": "по рейтингу",
                                },
                            ],
                        }
                    }
                },
            )

    @classmethod
    def prepare_model_wizard_offers_collapsing(cls):
        """Данные для проверки схлопывания в модельном колдунщике
        https://st.yandex-team.ru/MARKETOUT-20758
        """
        cls.index.models += [
            Model(title='ToysModel 1', hyperid=2001, ts=901),
            Model(title='ToysModel 2', hyperid=2002, ts=902),
            Model(title='PlaythingsModel 1', hyperid=2003, ts=903),
            Model(title='PlaythingsModel 2', hyperid=2004, ts=904),
        ]

        cls.index.offers += [
            Offer(title="ToysModel ToysOffer", hyperid=2003, ts=905),
            Offer(title="ToysModel ToysOffer", hyperid=2004, ts=906),
            Offer(hyperid=2001, ts=907),
            Offer(hyperid=2002, ts=908),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 905).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 906).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 907).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 908).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 901).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 902).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 903).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 904).respond(0.1)

        cls.matrixnet.on_place(MnPlace.MODEL_WIZARD_META_RANK, 2003).respond(0.7)
        cls.matrixnet.on_place(MnPlace.MODEL_WIZARD_META_RANK, 2004).respond(0.8)

        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META_RANK, 2003).respond(0.7)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META_RANK, 2004).respond(0.8)

        for model_id in range(2001, 2005):
            cls.matrixnet.on_place(MnPlace.MODEL_WIZARD_META, model_id).respond(0.5)
            cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, model_id).respond(0.2)

    def test_model_wizard_offers_collapsing(self):
        """Проверяем схлопывание в модельном колдунщике
        https://st.yandex-team.ru/MARKETOUT-20758
        https://st.yandex-team.ru/MARKETOUT-22977
        https://st.yandex-team.ru/MARKETOUT-26953
        """

        model = {"market_model": {"title": {"__hl": {"raw": True, "text": "ToysModel 1"}}}}

        collapsed_model = {"market_model": {"title": {"__hl": {"raw": True, "text": "PlaythingsModel 1"}}}}
        ranked_collapsed_model = {"market_model": {"title": {"__hl": {"raw": True, "text": "PlaythingsModel 2"}}}}

        base_formula = '&rearr-factors=market_model_wiz_top_model_threshold=0.5'

        meta_rank_formula = '&rearr-factors=market_model_wizard_collapsing_meta_rank_mn_algo=MNA_mn201477'

        meta_formula = (
            '&rearr-factors=market_model_wizard_meta_mn_algo=MNA_mn201477;' 'market_model_wizard_meta_threshold=0.6'
        )

        no_collapsing = '&rearr-factors=market_parallel_use_collapsing=0'

        for type in [0, 1, 2, 3]:
            # market_parallel_use_collapsing включен по умолчанию
            collapsing = '&rearr-factors=market_parallel_collapsing_type={};'.format(type)

            # Схлопывание выключено, модель нашлась
            response = self.report.request_bs_pb('place=parallel&text=ToysModel' + no_collapsing)
            self.assertFragmentIn(response, model)

            # Схлопывание включено, модель нашлась
            # При market_parallel_collapsing_type=3 группировка yg не запрашивается
            response = self.report.request_bs_pb('place=parallel&text=ToysModel' + collapsing)
            self.assertFragmentIn(response, collapsed_model if type == 3 else model)

            # Схлопывание выключено, модель не нашлась
            response = self.report.request_bs_pb('place=parallel&text=ToysOffer' + no_collapsing)
            self.assertFragmentNotIn(response, {"market_model": [{}]})

            # Схлопывание включено, модель не нашлась, нашлась модель по схлопыванию
            response = self.report.request_bs_pb('place=parallel&text=ToysOffer' + collapsing)
            self.assertFragmentIn(response, collapsed_model)

            # Порог по базовой формуле
            # Схлопывание выключено, модель нашлась, но не прошла порог по базовой формуле
            response = self.report.request_bs_pb('place=parallel&text=ToysModel' + base_formula + no_collapsing)
            self.assertFragmentNotIn(response, {"market_model": [{}]})

            # Схлопывание включено, модель нашлась, но не прошла порог по базовой формуле, нашлась модель по схлопыванию
            response = self.report.request_bs_pb('place=parallel&text=ToysModel' + base_formula + collapsing)
            self.assertFragmentIn(response, collapsed_model)

            # Порог на мете
            # Схлопывание выключено, модель нашлась, но не прошла порог на мете
            response = self.report.request_bs_pb(
                'place=parallel&text=ToysModel&trace_wizard=1' + meta_formula + no_collapsing
            )
            self.assertFragmentNotIn(response, {"market_model": {}})
            self.assertIn('31 2 Выбрана модель modelId = 2001', response.get_trace_wizard())
            self.assertIn('31 4 Did not pass: meta MatrixNet value is too low', response.get_trace_wizard())

            # Схлопывание включено, модель нашлась, но не прошла порог на мете, нашлась модель по схлопыванию
            response = self.report.request_bs_pb('place=parallel&text=ToysModel' + meta_formula + collapsing)
            self.assertFragmentIn(response, collapsed_model)

            # Отдельный порог по базовой формуле для схлопнутых моделей
            # Схлопывание включено, модель не нашлась, модель по схлопыванию не прошла порог по базовой формуле
            response = self.report.request_bs_pb(
                'place=parallel&text=ToysOffer'
                + collapsing
                + '&rearr-factors=market_model_wizard_collapsing_top_model_threshold=0.9'
            )
            self.assertFragmentNotIn(response, {"market_model": {}})

            # Схлопывание включено, модель не нашлась, модель по схлопыванию прошла порог порог по базовой формуле
            response = self.report.request_bs_pb(
                'place=parallel&text=ToysOffer'
                + collapsing
                + '&rearr-factors=market_model_wizard_collapsing_top_model_threshold=0.7'
            )
            self.assertFragmentIn(response, collapsed_model)

            # Ранжирующая при схлопывании мета-формула
            # Схлопывание включено, модель не нашлась, нет ранжирования по мета-формуле
            response = self.report.request_bs(
                'place=parallel&text=ToysOffer'
                + collapsing
                + '&rearr-factors=market_model_wizard_collapsing_top_model_threshold=0.1'
                '&debug=1'
            )
            self.assertNotIn('Using model wizard meta ranking MatrixNet formula:', str(response))
            # Схлопывание включено, модель не нашлась, модели по схлопыванию отранжировались по мета-формуле
            response = self.report.request_bs_pb(
                'place=parallel&text=ToysOffer'
                + collapsing
                + meta_rank_formula
                + '&rearr-factors=market_model_wizard_collapsing_top_model_threshold=0.1'
            )
            self.assertFragmentIn(response, ranked_collapsed_model)
            response = self.report.request_bs(
                'place=parallel&text=ToysOffer'
                + collapsing
                + meta_rank_formula
                + '&rearr-factors=market_model_wizard_collapsing_top_model_threshold=0.1'
                '&debug=1'
            )
            self.assertIn('Using model wizard meta ranking MatrixNet formula: MNA_mn201477', str(response))

            # Отдельный порог на мете для схлопнутых моделей
            # Схлопывание включено, модель не нашлась, модель по схлопыванию не прошла порог на мете
            response = self.report.request_bs_pb(
                'place=parallel&text=ToysOffer'
                + meta_formula
                + collapsing
                + '&rearr-factors=market_model_wizard_collapsing_meta_threshold=0.6'
            )
            self.assertFragmentNotIn(response, {"market_model": {}})

            # Схлопывание включено, модель не нашлась, модель по схлопыванию прошла порог на мете
            response = self.report.request_bs_pb(
                'place=parallel&text=ToysOffer'
                + meta_formula
                + collapsing
                + '&rearr-factors=market_model_wizard_collapsing_meta_threshold=0.4'
            )
            self.assertFragmentIn(response, collapsed_model)

            # Отдельная формула на мете для схлопнутых моделей
            # По умолчанию берется общая мета-формула
            response = self.report.request_bs(
                'place=parallel&text=ToysOffer'
                + meta_formula
                + collapsing
                + '&rearr-factors=market_model_wizard_collapsing_meta_threshold=0.4'
                '&debug=1'
            )
            self.assertIn('Using model wizard meta MatrixNet formula: MNA_mn201477', str(response))
            # При указании market_model_wizard_collapsing_meta_mn_algo для схлопнутых моделей берется она
            response = self.report.request_bs(
                'place=parallel&text=ToysOffer'
                + meta_formula
                + collapsing
                + '&rearr-factors=market_model_wizard_collapsing_meta_threshold=0.4;'
                'market_model_wizard_collapsing_meta_mn_algo=MNA_Trivial;'
                '&debug=1'
            )
            self.assertIn('Using model wizard meta MatrixNet formula: MNA_Trivial', str(response))

    def test_implicit_model_wizard_offers_collapsing(self):
        """Проверяем схлопывание в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-20758
        https://st.yandex-team.ru/MARKETOUT-22977
        https://st.yandex-team.ru/MARKETOUT-26953
        """
        models = {
            "market_implicit_model": {
                "showcase": {
                    "items": [
                        {"title": {"text": {"__hl": {"text": "ToysModel 1", "raw": True}}}},
                        {"title": {"text": {"__hl": {"text": "ToysModel 2", "raw": True}}}},
                    ]
                }
            }
        }

        collapsed_models = {
            "market_implicit_model": {
                "showcase": {
                    "items": [
                        {"title": {"text": {"__hl": {"text": "PlaythingsModel 1", "raw": True}}}},
                        {"title": {"text": {"__hl": {"text": "PlaythingsModel 2", "raw": True}}}},
                    ]
                }
            }
        }

        ranked_collapsed_models = {
            "market_implicit_model": {
                "showcase": {
                    "items": [
                        {"title": {"text": {"__hl": {"text": "PlaythingsModel 2", "raw": True}}}},
                        {"title": {"text": {"__hl": {"text": "PlaythingsModel 1", "raw": True}}}},
                    ]
                }
            }
        }

        base_formula = '&rearr-factors=market_implicit_model_wiz_top_models_threshold=0.8'

        meta_rank_formula = '&rearr-factors=market_implicit_model_wizard_collapsing_meta_rank_mn_algo=MNA_mn201477'

        # мета-формула включена по умолчанию
        no_meta_formula = '&rearr-factors=market_implicit_model_wizard_meta_threshold=0.0;'
        meta_formula = '&rearr-factors=market_implicit_model_wizard_meta_threshold=0.6'

        no_collapsing = '&rearr-factors=market_parallel_use_collapsing=0'

        model_request = 'place=parallel&text=ToysModel'
        offer_request = 'place=parallel&text=ToysOffer'

        for type in [0, 1, 2, 3]:
            # market_parallel_use_collapsing включен по умолчанию
            collapsing = '&rearr-factors=market_parallel_collapsing_type={};'.format(type)

            # Схлопывание выключено, нашлось две модели
            response = self.report.request_bs_pb(model_request + no_meta_formula + no_collapsing)
            self.assertFragmentIn(response, models, preserve_order=True)

            # Схлопывание включено, нашлось две модели
            response = self.report.request_bs_pb(model_request + no_meta_formula + collapsing)
            self.assertFragmentIn(response, models, preserve_order=True)

            # Схлопывание выключено, моделей не нашлось
            response = self.report.request_bs_pb(offer_request + no_collapsing)
            self.assertFragmentNotIn(response, {"market_implicit_model": {}})

            # Схлопывание включено, моделей не нашлось, нашлись модели по схлопыванию
            response = self.report.request_bs_pb(offer_request + collapsing)
            self.assertFragmentIn(response, collapsed_models, preserve_order=True)

            # Топ-4 порог по базовой формуле
            # Схлопывание выключено, модели нашлись, но не прошли порог по базовой формуле
            response = self.report.request_bs_pb(model_request + no_meta_formula + base_formula + no_collapsing)
            self.assertFragmentNotIn(response, {"market_implicit_model": {}})

            # Схлопывание включено, модели нашлись, но не прошли порог по базовой формуле, нашлись модели по схлопыванию
            response = self.report.request_bs_pb(model_request + no_meta_formula + base_formula + collapsing)
            self.assertFragmentIn(response, collapsed_models, preserve_order=True)

            # Топ-3 порог на мете
            # Схлопывание выключено, модели нашлись, но не прошли порог на мете
            response = self.report.request_bs_pb(model_request + meta_formula + no_collapsing + '&trace_wizard=1')
            self.assertFragmentNotIn(response, {"market_implicit_model": {}})
            self.assertIn('29 4 Did not pass: top 3 meta MatrixNet sum is too low', response.get_trace_wizard())

            # Схлопывание включено, модели нашлись, но не прошли порог на мете, нашлись модели по схлопыванию
            response = self.report.request_bs_pb(model_request + meta_formula + collapsing)
            self.assertFragmentIn(response, collapsed_models, preserve_order=True)

            # Отдельный порог Топ-4 по базовой формуле для схлопнутых моделей
            # Схлопывание включено, моделей не нашлось, модели по схлопыванию не прошли порог по базовой формуле
            response = self.report.request_bs_pb(
                offer_request
                + collapsing
                + '&rearr-factors=market_implicit_model_wizard_collapsing_top_models_threshold=6.6'
            )
            self.assertFragmentNotIn(response, {"market_implicit_model": {}})

            # Схлопывание включено, моделей не нашлось, модели по схлопыванию прошли порог по базовой формуле
            response = self.report.request_bs_pb(
                offer_request
                + collapsing
                + '&rearr-factors=market_implicit_model_wizard_collapsing_top_models_threshold=1.4'
            )
            self.assertFragmentIn(response, collapsed_models, preserve_order=True)

            # Ранжирующая при схлопывании мета-формула
            # Схлопывание включено, моделей не нашлось, нет ранжирования по мета-формуле
            response = self.report.request_bs(
                offer_request
                + collapsing
                + '&rearr-factors=market_implicit_model_wizard_collapsing_top_models_threshold=1.4'
                '&debug=1'
            )
            self.assertNotIn('Using implicit white model wizard meta ranking MatrixNet formula:', str(response))
            # Схлопывание включено, моделей не нашлось, модели по схлопыванию отранжировались по мета-формуле
            response = self.report.request_bs_pb(
                offer_request
                + collapsing
                + meta_rank_formula
                + '&rearr-factors=market_implicit_model_wizard_collapsing_top_models_threshold=1.4'
            )
            self.assertFragmentIn(response, ranked_collapsed_models, preserve_order=True)
            response = self.report.request_bs(
                offer_request
                + collapsing
                + meta_rank_formula
                + '&rearr-factors=market_implicit_model_wizard_collapsing_top_models_threshold=1.4'
                '&debug=1'
            )
            self.assertIn(
                'Using implicit white model wizard meta ranking MatrixNet formula: MNA_mn201477', str(response)
            )

            # Отдельный порог на мете для Топ-3 схлопнутых моделей
            # Схлопывание включено, моделей не нашлось, модели по схлопыванию не прошли порог на мете
            response = self.report.request_bs_pb(
                offer_request
                + meta_formula
                + collapsing
                + '&rearr-factors=market_implicit_model_wizard_collapsing_meta_threshold=0.7'
            )
            self.assertFragmentNotIn(response, {"market_implicit_model": {}})

            # Схлопывание включено, моделей не нашлось, модели по схлопыванию прошли порог на мете
            response = self.report.request_bs_pb(
                offer_request
                + meta_formula
                + collapsing
                + '&rearr-factors=market_implicit_model_wizard_collapsing_meta_threshold=0.4'
            )
            self.assertFragmentIn(response, collapsed_models, preserve_order=True)

            # Отдельная формула на мете для Топ-3 схлопнутых моделей
            # По умолчанию берется общая мета-формула
            response = self.report.request_bs(
                offer_request
                + meta_formula
                + collapsing
                + '&rearr-factors=market_implicit_model_wizard_collapsing_meta_threshold=0.4'
                '&debug=1'
            )
            self.assertIn(
                'Using implicit white model wizard meta MatrixNet formula: MNA_fml_formula_785353', str(response)
            )
            # При указании market_implicit_model_wizard_collapsing_meta_mn_algo для схлопнутых моделей берется она
            response = self.report.request_bs(
                offer_request
                + meta_formula
                + collapsing
                + '&rearr-factors=market_implicit_model_wizard_collapsing_meta_threshold=0.4;'
                'market_implicit_model_wizard_collapsing_meta_mn_algo=MNA_mn201477;'
                '&debug=1'
            )
            self.assertIn('Using implicit white model wizard meta MatrixNet formula: MNA_mn201477', str(response))

    def test_using_offers_factors_in_model_wizard_collapsing(self):
        """Проверяем использование офферных факторов на мете для схлопывания в модельном колдунщике
        https://st.yandex-team.ru/MARKETOUT-22671
        """
        # market_parallel_use_collapsing включен по умолчанию
        request = (
            'place=parallel&text=ToysOffer&debug=1&trace_wizard=1'
            '&rearr-factors=market_model_wizard_meta_mn_algo=MNA_mn201477;'
        )

        # Под флагом market_parallel_collapsing_use_offer_factors=1 по умолчанию при схлопывании используются офферные факторы
        response = self.report.request_bs_pb(request)
        self.assertIn('31 1 Offer factors are used for collapsed model', response.get_trace_wizard())

        # Без флага market_parallel_collapsing_use_offer_factors при схлопывании используются модельные факторы
        response = self.report.request_bs_pb(request + 'market_parallel_collapsing_use_offer_factors=0')
        self.assertNotIn('31 1 Offer factors are used for collapsed model', response.get_trace_wizard())

    def test_using_offers_factors_in_implicit_model_wizard_collapsing(self):
        """Проверяем использование офферных факторов на мете для схлопнутых моделей в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-22671
        """
        # market_parallel_use_collapsing включен по умолчанию
        request = (
            'place=parallel&text=toys+offer&debug=1&trace_wizard=1'
            '&rearr-factors=market_implicit_model_wizard_meta_mn_algo=MNA_mn201477;'
        )

        # Под флагом market_parallel_collapsing_use_offer_factors=1 по умолчанию при схлопывании используются офферные факторы
        response = self.report.request_bs_pb(request)
        self.assertIn('29 1 Offers factors are used for collapsed models', response.get_trace_wizard())

        # Без флага market_parallel_collapsing_use_offer_factors при схлопывании используются модельные факторы
        response = self.report.request_bs_pb(request + 'market_parallel_collapsing_use_offer_factors=0')
        self.assertNotIn('29 1 Offers factors are used for collapsed models', response.get_trace_wizard())

    @classmethod
    def prepare_implicit_model_extra_models(cls):
        for i in range(10):
            ts = 1400 + i
            cls.index.models += [Model(hyperid=ts, title='ExtraModel {}'.format(i), ts=ts)]
            cls.index.offers += [Offer(hyperid=ts, title='ExtraModel offer {}'.format(i))]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.1 + 0.01 * i)
        for i in range(3):
            ts = 1410 + i
            cls.index.models += [Model(hyperid=ts, title='NoextraModel {}'.format(i), ts=ts)]
            cls.index.offers += [Offer(hyperid=ts, title='NoextraModel offer {}'.format(i))]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.1 + 0.01 * i)

    def test_implicit_model_extra_models(self):
        """Проверяем, что в колдунщике неявной модели
        есть дополнительные 3 модели к стандартным 4 для РГ.
        https://st.yandex-team.ru/MARKETOUT-21628
        """
        response = self.report.request_bs_pb('place=parallel&text=ExtraModel')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "6",
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ExtraModel 9", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ExtraModel 8", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ExtraModel 7", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ExtraModel 6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ExtraModel 5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ExtraModel 4", "raw": True}}}},
                        ],
                        "extra_models": [
                            {"title": {"text": {"__hl": {"text": "ExtraModel 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ExtraModel 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ExtraModel 1", "raw": True}}}},
                        ],
                    },
                }
            },
        )
        log_content = ['"wiz_id":"market_implicit_model"', '"extra_models":["1403","1402","1401"]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

        # если моделей не хватает, то extra_models пустые
        response = self.report.request_bs_pb('place=parallel&text=NoextraModel')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "3",
                    "showcase": {
                        "items": ElementCount(3),
                        "extra_models": ElementCount(0),
                    },
                }
            },
        )
        log_content = ['"wiz_id":"market_implicit_model"', '"extra_models":[]']
        self.access_log.expect(wizard_elements=Regex('.*'.join(re.escape(x) for x in log_content)))

    def test_offers_wizard_incut_url_types(self):
        """Проверяем, что под флагом market_offers_wizard_incut_url_type
        url офферов во врезке ведут на разные посадочные страницы
        https://st.yandex-team.ru/MARKETOUT-22344
        """

        def test(flag, urlForCounter, device=None, url="http://kiyanochnaya.ru/kiyanki?id=1", cgi=None):
            # market_check_offers_incut_size=0 отключает проверку на хотя бы 4 офера, отключаем порог на врезку
            request = 'place=parallel&text=kiyanka&rearr-factors=market_check_offers_incut_size=0;market_offers_incut_threshold=0;offers_touch=1;'
            if flag:
                request += 'market_offers_wizard_incut_url_type={};'.format(flag)
            if device:
                request += 'device={};'.format(device)
            if cgi:
                request += cgi
            response = self.report.request_bs_pb(request)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "title": "\7[Kiyanka\7]",
                        "showcase": {
                            "items": [
                                {
                                    "title": {"url": url, "urlTouch": url, "urlForCounter": urlForCounter},
                                    "thumb": {"url": url, "urlTouch": url, "urlForCounter": urlForCounter},
                                }
                            ]
                        },
                    }
                },
            )

        # Default: OfferCard url
        # https://st.yandex-team.ru/MARKETOUT-25386 In case of offercard url-field is decrypted offercard url
        # https://st.yandex-team.ru/MARKETOUT-25838
        # https://st.yandex-team.ru/MARKETOUT-34097
        test(
            None,
            LikeUrl.of(
                '//market.yandex.ru/search?clid=545&cvredirect=0&text=kiyanka&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2'
            ),
            url=LikeUrl.of('//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A'),
        )
        test(
            None,
            LikeUrl.of(
                '//market.yandex.ru/search?clid=545&cvredirect=0&text=kiyanka&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2'
            ),
            device='desktop',
            url=LikeUrl.of('//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A'),
        )
        test(
            None,
            LikeUrl.of(
                '//market.yandex.ru/search?clid=708&cvredirect=0&text=kiyanka&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2'
            ),
            device='touch',
            url=LikeUrl.of('//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A'),
        )

        # OfferCard url
        test(
            'OfferCard',
            Contains('/redir/dtype=offercard/'),
            url=LikeUrl.of('//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A'),
        )
        test('OfferCard', Contains('/pp=405/'), url=LikeUrl.of('//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A'))

        # NailedInSearch url
        test(
            'NailedInSearch',
            LikeUrl.of(
                '//market.yandex.ru/search?clid=545&cvredirect=0&text=kiyanka&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2'
            ),
            url=LikeUrl.of('//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A'),
        )
        test(
            'NailedInSearch',
            LikeUrl.of(
                '//market.yandex.ru/search?clid=545&cvredirect=0&text=kiyanka&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2'
            ),
            device='desktop',
            url=LikeUrl.of('//market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A'),
        )
        test(
            'NailedInSearch',
            LikeUrl.of(
                '//m.market.yandex.ru/search?clid=708&cvredirect=0&text=kiyanka&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2'
            ),
            device='touch',
            url=LikeUrl.of('//m.market.yandex.ru/offer/f7_BYaO4c78hGceI7ZPR9A'),
            cgi='&touch=1',
        )

        # NailedInCatalog url
        test(
            'NailedInCatalog',
            LikeUrl.of(
                '//market.yandex.ru/catalog/130/list?clid=545&hid=130&text=kiyanka'
                '&rs=eJwzUvCS4xJLM493ikz0N0k2t8hwT071NI8KCLJ0lGBUYNBgAACUJQf2'
            ),
        )

    def test_nailed_models_in_implicit_model_wizard_title_url(self):
        """Проверяем, что под флагом market_implicit_model_wizard_nailed_models_count
        в ссылке тайтла колдунщика неявной модели в параметре rs передаются найденные модели
        https://st.yandex-team.ru/MARKETOUT-22484
        https://st.yandex-team.ru/MARKETOUT-26491
        """

        def parse_rs_models(rs):
            result = []
            rs = urllib.unquote(rs)
            common_report_state = ReportState.parse(rs)
            for nailed_doc in common_report_state.search_state.nailed_docs:
                if nailed_doc.model_id:
                    result.append(nailed_doc.model_id)
            return result

        # По запросу 'panasonic tumix' находятся модели 101 и 102
        # Под флагом market_implicit_model_wizard_nailed_models_count=0 параметр &rs в url отсутствует
        request = 'place=parallel&text=panasonic+tumix'
        url = '//market.yandex.ru/search?text=panasonic%20tumix&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards'
        url_touch = (
            '//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards'
        )
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_implicit_model_wizard_nailed_models_count=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(url, ignore_len=False),
                    "urlTouch": LikeUrl.of(url_touch, ignore_len=False),
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&clid=698"
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-pt6000/102?hid=30&nid=10&clid=698"
                                    ),
                                }
                            },
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # С флагом market_implicit_model_wizard_nailed_models_count=1 в параметре &rs передается одна модель 101
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_implicit_model_wizard_nailed_models_count=1'
        )
        rs = 'eJwz4vRiFWI2NDCMYAAACNUBig%2C%2C'
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                    "urlTouch": LikeUrl.of(url_touch + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        models = parse_rs_models(rs)
        self.assertEqual(models, ['101'])

        # С флагом market_implicit_model_wizard_nailed_models_count=2 в параметре &rs передаются две модели 101 и 102
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_implicit_model_wizard_nailed_models_count=2'
        )
        rs = 'eJwzEvBiFWI2NDCEUEYRDAAXRwKI'
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                    "urlTouch": LikeUrl.of(url_touch + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        models = parse_rs_models(rs)
        self.assertEqual(models, ['101', '102'])

    def test_nailed_offers_in_offers_wizard_title_url(self):
        """Проверяем, что под флагом ﻿market_offers_wizard_nailed_offers_count
        в ссылке тайтла офферного колдунщика в параметре rs передаются найденные офферы
        https://st.yandex-team.ru/MARKETOUT-22485
        https://st.yandex-team.ru/MARKETOUT-26491
        """

        def parse_rs_offers(rs):
            result = []
            rs = urllib.unquote(rs)
            common_report_state = ReportState.parse(rs)
            for nailed_doc in common_report_state.search_state.nailed_docs:
                if nailed_doc.ware_id:
                    result.append(nailed_doc.ware_id)
            return result

        # По запросу 'pelmen' находятся 3 оффера с ware_id: 09lEaAKkQll1XTjm0WPoIA, xMpCOKC5I4INzFCab3WEmQ, lpc2G9gcBPtOqJHWMQSlow
        # Под флагом market_offers_wizard_nailed_offers_count=0 параметр &rs в url отсутствует
        request = 'place=parallel&text=pelmen'
        url = '//market.yandex.ru/search?text=pelmen&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards'
        url_touch = '//m.market.yandex.ru/search?text=pelmen&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards'
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wizard_nailed_offers_count=0')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url, ignore_len=False),
                    "urlTouch": LikeUrl.of(url_touch, ignore_len=False),
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "pelmen-1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "pelmen-2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "pelmen-3", "raw": True}}}},
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # С флагом market_offers_wizard_nailed_offers_count=1 в параметре &rs передается один оффер: 09lEaAKkQll1XTjm0WPoIA
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wizard_nailed_offers_count=1')
        rs = 'eJwzUvKS4xIzsMxxTXT0zg7MyTGMCMnKNQgPyPd0lGBUYNBgiGAAAKmoCLU%2C'
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                    "urlTouch": LikeUrl.of(url_touch + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        offers = parse_rs_offers(rs)
        self.assertEqual(offers, ['09lEaAKkQll1XTjm0WPoIA'])

        # Флаг market_offers_wizard_nailed_offers_count=3 раскатан по дефолту. В параметре &rs передаются три оффера:
        # 09lEaAKkQll1XTjm0WPoIA, xMpCOKC5I4INzFCab3WEmQ, lpc2G9gcBPtOqJHWMQSlow
        response = self.report.request_bs_pb(request)
        rs = 'eJwzSvKS4xIzsMxxTXT0zg7MyTGMCMnKNQgPyPd0lGBUYNBgAMlX-BY4-3s7m3qaePpVuTknJhmHu-YGIuRzCpKN3C3Tk50CSvwLvTzCfQODc_LLIfIRDAAYNRll'
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                    "urlTouch": LikeUrl.of(url_touch + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        offers = parse_rs_offers(rs)
        self.assertEqual(offers, ['09lEaAKkQll1XTjm0WPoIA', 'xMpCOKC5I4INzFCab3WEmQ', 'lpc2G9gcBPtOqJHWMQSlow'])

    def test_use_nailed_docs_in_wizards_title_url(self):
        """Проверяем, что под флагом market_parallel_nailed_docs_on_empty_serp_only
        в ссылке тайтла колдунщиков неявной модели и офферного в параметре &rs передается флаг use_nailed_docs=false
        https://st.yandex-team.ru/MARKETOUT-23154
        https://st.yandex-team.ru/MARKETOUT-26491
        """

        def get_use_nailed_docs(rs):
            rs = urllib.unquote(rs)
            common_report_state = ReportState.parse(rs)
            return common_report_state.search_state.use_nailed_docs

        # Проверка для неявной модели
        request = 'place=parallel&text=panasonic+tumix'
        url = '//market.yandex.ru/search?text=panasonic%20tumix&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards'

        # С флагом market_parallel_nailed_docs_on_empty_serp_only=0 параметр use_nailed_docs = true
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_implicit_model_wizard_nailed_models_count=1;'
            'market_parallel_nailed_docs_on_empty_serp_only=0'
        )
        rs = 'eJwzYvdiFWI2NDAEAAWxATA%2C'
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        self.assertEqual(get_use_nailed_docs(rs), True)

        # По умолчанию флаг market_parallel_nailed_docs_on_empty_serp_only = 1
        # параметр use_nailed_docs = false
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_implicit_model_wizard_nailed_models_count=1'
        )
        rs = 'eJwz4vRiFWI2NDCMYAAACNUBig%2C%2C'
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        self.assertEqual(get_use_nailed_docs(rs), False)

        # Проверка для офферного
        request = 'place=parallel&text=pelmen'
        url = '//market.yandex.ru/search?text=pelmen&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards'

        # Под флагом market_parallel_nailed_docs_on_empty_serp_only=0 параметр use_nailed_docs = true
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wizard_nailed_offers_count=1;'
            'market_parallel_nailed_docs_on_empty_serp_only=0'
        )
        rs = 'eJwzUvCS4xIzsMxxTXT0zg7MyTGMCMnKNQgPyPd0lGBUYNBgAACX_Ahb'
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        self.assertEqual(get_use_nailed_docs(rs), True)

        # По умолчанию флаг market_parallel_nailed_docs_on_empty_serp_only = 1
        # параметр use_nailed_docs = false
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wizard_nailed_offers_count=1')
        rs = 'eJwzUvKS4xIzsMxxTXT0zg7MyTGMCMnKNQgPyPd0lGBUYNBgiGAAAKmoCLU%2C'
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        self.assertEqual(get_use_nailed_docs(rs), False)

    def test_nailed_offers_in_offers_wizard_empty_incut(self):
        """Проверяем, что под флагами ﻿market_offers_wizard_nailed_offers_count и
        market_offers_wizard_nailed_offers_on_empty_incut в ссылке тайтла офферного колдунщика без врезки
        в параметре rs передаются найденные офферы
        https://st.yandex-team.ru/MARKETOUT-23426
        https://st.yandex-team.ru/MARKETOUT-26491
        """

        def parse_rs_offers(rs):
            result = []
            rs = urllib.unquote(rs)
            common_report_state = ReportState.parse(rs)
            for nailed_doc in common_report_state.search_state.nailed_docs:
                if nailed_doc.ware_id:
                    result.append(nailed_doc.ware_id)
            return result

        # По запросу 'panasonic tumix' находится 1 оффер с ware_id: obmaDftYV4bsBdqYgc5WmQ
        # Офферная врезка не формируется
        # Под флагами market_offers_wizard_nailed_offers_count=0 и market_offers_wizard_nailed_offers_on_empty_incut=0 параметр &rs в url отсутствует
        request = 'place=parallel&text=panasonic+tumix'
        url = '//market.yandex.ru/search?text=panasonic%20tumix&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards'
        url_touch = (
            '//m.market.yandex.ru/search?text=panasonic%20tumix&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards'
        )
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wizard_nailed_offers_count=0;'
            'market_offers_wizard_nailed_offers_on_empty_incut=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url, ignore_len=False),
                    "urlTouch": LikeUrl.of(url_touch, ignore_len=False),
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        # Под флагами market_offers_wizard_nailed_offers_count=1 и market_offers_wizard_nailed_offers_on_empty_incut=1
        # в параметре &rs передается один оффер: obmaDftYV4bsBdqYgc5WmQ
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wizard_nailed_offers_count=1;'
            'market_offers_wizard_nailed_offers_on_empty_incut=1'
        )
        rs = 'eJwzUvKS4xLLT8pNdEkriQwzSSp2SimMTE82Dc8NlGBUYNBgiGAAAL2qCY8%2C'
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(url + "&rs={}".format(rs), ignore_len=False),
                    "urlTouch": LikeUrl.of(url_touch + "&rs={}".format(rs), ignore_len=False),
                }
            },
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        offers = parse_rs_offers(rs)
        self.assertEqual(offers, ['obmaDftYV4bsBdqYgc5WmQ'])

    @classmethod
    def prepare_user_region_in_wizards_title(cls):
        cls.index.shops += [Shop(fesh=200, priority_region=213)]
        cls.index.models += [Model(hyperid=1000, title="Xiaomi Mi 8"), Model(hyperid=1001, title="Xiaomi Mi 8 Lite")]
        cls.index.offers += [
            Offer(hyperid=1000, fesh=200, title="Xiaomi Mi 8"),
            Offer(hyperid=1001, fesh=200, title="Xiaomi Mi 8 Lite"),
        ]
        cls.reqwizard.on_request('xiaomi mi 8 в москве').respond(
            qtree='cHicpZTNaxNBFMDfm920w_TDkFIMA8UQ0K6VYvAUlEKRiq1gDdVq2ZMGheagQoRaCmL1VIpgQfAgniz5aNBN8QsFqy14aOslOehRUDz0H_DufOxuks3qxYV9uzPvzZvf-5hhZ1g37Yn2xiGBFklBDDgkYQiOwfFuClEQ82BBCkYj45EMzMBlmMUVhCcIzxBeImwgiGcboYZX-Stkl5heRsUy6a5jPnfl5vUcx4TvtaPJK4yD9Dr7-fdRz6u7IuA7BWk8OUkxCtw1SIKFKcw8IhoqP8xcRVwqIA1TZMmxsZAV-3Gw-rJUfMngLcuYxXmyQChZRBCb8rfIMgFqookx4SKTEOT1ZeIhk3DcGYVL2lFPMKIwQWHikk0KTjtmtjNKOOof4DgowKEF_A47G-DGdAs2Kmxswf5letSYDoNOymA5ppNdKf9JK3y3-gugt0_20LvRRYyThGFFUpCUWSLeSLqyvxiU9sPo-TH-3mDZACqr36s_rH2sfai943g4iZbLHAlJ9dPH31_4yW5aGMa_TVTWm6zc7K962V9F1qSNo9ctiys2qVRtUhJvUbxr4i1UbaPgfLLNsrO5YxtFZ1OKLdsoS1GQoiLURkmKohRrUlSkXUVqS_KvpFcIRbm6q3ztysWb0loJqa7ueC0g-hPz06GQxtLWrqasODYpi2_Z0cQlR1MXHU2-5ij6ps5Sbg-x3obb-v36A_-0gLAjyo7Oo-gydMtsyrJZYP9EUU3z9sExXkN2IVBNs62OYUfm9cY-r4rm3-p3Q5XPDCvcOaam_YPzX8mAQJCieUWQe0ihXxLxb8imgkFenJiYaLoezbAQ33A_RGkeFuKcDnEul_NvBnRDPMLUdKMp_3GF5YfajAs2Ni4SdWu0XXciVDNqWqYYLeMBpqob20-R99GO_uenf4wMDJ5aH0nAsITWFiI1rgVTuRn4ujedDliYrT5W45WGjz61ixnronSqk5KYMZm5pmeJP4vubLdLZNLePOih3L5pKOj1UATzBy5RgLM,',  # noqa
            non_region_query='Xiaomi mi 8',
            found_cities=[213],
        )

    def test_user_region_in_offers_wizard_title(self):
        """Проверяем, что под флагом ﻿market_offers_wizard_region_in_title в тайтл офферного колдунщика
        добавляется регион пользователя, а в ссылку добавляется параметр &lr
        https://st.yandex-team.ru/MARKETOUT-22731

        market_offers_wizard_region_in_title=1 по умолчанию
        https://st.yandex-team.ru/MARKETOUT-26568
        """

        # При market_offers_wizard_region_in_title=1(по умолчанию) в тайтл добавляется регион пользователя,
        # ссылка содержит параметр &lr
        response = self.report.request_bs_pb('place=parallel&text=xiaomi+mi+8&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Xiaomi mi 8\7] в Москве",
                    "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=545&lr=213"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=708&lr=213"),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of("//market.yandex.ru?clid=545"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=708"),
                        },
                        {
                            "text": "Xiaomi mi 8 в Москве",
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=545&lr=213"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=708&lr=213"),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=545&lr=213"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=708&lr=213"),
                        }
                    ],
                }
            },
        )

        # При market_offers_wizard_region_in_title=1(по умолчанию) в тайтл добавляется регион пользователя из запроса,
        # ссылка содержит параметр &lr с регионом из запроса
        response = self.report.request_bs_pb(
            'place=parallel&text=xiaomi+mi+8+в+москве&rids=54&askreqwizard=1' '&rearr-factors=market_parallel_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Xiaomi mi 8\7] в Москве",
                    "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=545&lr=213"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=708&lr=213"),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of("//market.yandex.ru?clid=545"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=708"),
                        },
                        {
                            "text": "Xiaomi mi 8 в Москве",
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=545&lr=213"),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=708&lr=213"
                            ),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=545&lr=213"),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=708&lr=213"
                            ),
                        }
                    ],
                }
            },
        )

        # При market_offers_wizard_region_in_title=0 регион пользователя в тайтл не добавляется,
        # ссылка содержит параметр &lr с регионом из запроса https://st.yandex-team.ru/MARKETOUT-32765
        response = self.report.request_bs_pb(
            'place=parallel&text=xiaomi+mi+8&rids=213&rearr-factors=market_offers_wizard_region_in_title=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Xiaomi mi 8\7]",
                    "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=545&lr=213"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=708&lr=213"),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of("//market.yandex.ru?clid=545"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=708"),
                        },
                        {
                            "text": "Xiaomi mi 8",
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=545&lr=213"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=708&lr=213"),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=545&lr=213"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=708&lr=213"),
                        }
                    ],
                }
            },
        )

    def test_user_region_in_implicit_model_wizard_title(self):
        """Проверяем, что под флагом ﻿market_implicit_model_wizard_region_in_title в тайтл колдунщика неявной модели
        добавляется регион пользователя, а в ссылку добавляется параметр &lr
        https://st.yandex-team.ru/MARKETOUT-22731

        market_implicit_model_wizard_region_in_title=1 по умолчанию
        https://st.yandex-team.ru/MARKETOUT-26568
        """

        # При market_implicit_model_wizard_region_in_title=1(по умолчанию) в тайтл добавляется регион пользователя,
        # ссылка содержит параметр &lr
        response = self.report.request_bs_pb('place=parallel&text=xiaomi+mi+8&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "\7[Xiaomi mi 8\7] в Москве",
                    "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=698&lr=213"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=721&lr=213"),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of("//market.yandex.ru?clid=698"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=721"),
                        },
                        {
                            "text": "Xiaomi mi 8 в Москве",
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=698&lr=213"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=721&lr=213"),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=698&lr=213"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=721&lr=213"),
                        }
                    ],
                }
            },
        )

        # При market_implicit_model_wizard_region_in_title=1(по умолчанию) в тайтл добавляется регион пользователя из запроса,
        # ссылка содержит параметр &lr с регионом из запроса
        response = self.report.request_bs_pb(
            'place=parallel&text=xiaomi+mi+8+в+москве&rids=54&askreqwizard=1' '&rearr-factors=market_parallel_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "\7[Xiaomi mi 8\7] в Москве",
                    "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=698&lr=213"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=721&lr=213"),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of("//market.yandex.ru?clid=698"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=721"),
                        },
                        {
                            "text": "Xiaomi mi 8 в Москве",
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=698&lr=213"),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=721&lr=213"
                            ),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=698&lr=213"),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=xiaomi+mi+8+в+москве&clid=721&lr=213"
                            ),
                        }
                    ],
                }
            },
        )

        # При market_implicit_model_wizard_region_in_title=0 регион пользователя в тайтл не добавляется,
        # ссылка содержит параметр &lr с регионом из запроса https://st.yandex-team.ru/MARKETOUT-32765
        response = self.report.request_bs_pb(
            'place=parallel&text=xiaomi+mi+8&rids=213' '&rearr-factors=market_implicit_model_wizard_region_in_title=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "\7[Xiaomi mi 8\7] на Маркете",
                    "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=698&lr=213"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=721&lr=213"),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of("//market.yandex.ru?clid=698"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=721"),
                        },
                        {
                            "text": "Xiaomi mi 8",
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=698&lr=213"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=721&lr=213"),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=xiaomi+mi+8&clid=698&lr=213"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=xiaomi+mi+8&clid=721&lr=213"),
                        }
                    ],
                }
            },
        )

    def test_offers_wizard_incut_show_always(self):
        """Проверяем, что под флагом market_offers_incut_show_always офферная врезка отдается всегда,
        в выдаче присутствует признак нужно-ли отображать врезку
        https://st.yandex-team.ru/MARKETOUT-23483

        В ответе теперь всегда присутсвует признак, нужно ли отображать врезку
        https://st.yandex-team.ru/MARKETOUT-36759
        """
        request = 'place=parallel&text=molotok&rearr-factors=market_offers_incut_threshold=0.1'
        request_no_incut = 'place=parallel&text=molotok&rearr-factors=market_offers_incut_threshold=0.9'

        # Врезка есть, без флага market_offers_incut_show_always врезка отдается, признак show_items=0
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response, {"market_offers_wizard": {"showcase": {"items": ElementCount(9), "show_items": "0"}}}
        )
        self.assertFragmentIn(
            response, {"market_offers_wizard_center_incut": {"showcase": {"items": ElementCount(9), "show_items": "1"}}}
        )

        # Врезки нет, без флага market_offers_incut_show_always врезка отдается, признак show_items=0
        response = self.report.request_bs_pb(request_no_incut)
        self.assertFragmentIn(
            response, {"market_offers_wizard": {"showcase": {"items": ElementCount(9), "show_items": "0"}}}
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        # Врезка есть, под флагом market_offers_incut_show_always признак show_items=1
        response = self.report.request_bs_pb(request + ';market_offers_incut_show_always=1')
        self.assertFragmentIn(
            response, {"market_offers_wizard": {"showcase": {"items": ElementCount(9), "show_items": "0"}}}
        )
        self.assertFragmentIn(
            response, {"market_offers_wizard_center_incut": {"showcase": {"items": ElementCount(9), "show_items": "1"}}}
        )

        # Врезки нет, но под флагом market_offers_incut_show_always врезка отдается, признак show_items=0
        response = self.report.request_bs_pb(request_no_incut + ';market_offers_incut_show_always=1')
        self.assertFragmentIn(
            response, {"market_offers_wizard": {"showcase": {"items": ElementCount(9), "show_items": "0"}}}
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

    def test_offers_wizard_split_viewtype(self):
        """Проверяем, что под флагами market_enable_offers_wiz_right_incut и market_enable_offers_wiz_center_incut
        формируются три офферных колдунщика market_offers_wizard, market_offers_wizard_right_incut и
        market_offers_wizard_center_incut,
        офферная врезка отдается всегда c признаком необходимости ее отображать
        https://st.yandex-team.ru/MARKETOUT-23485
        """
        request = 'place=parallel&text=molotok&rearr-factors=market_offers_incut_threshold=0.1;'
        request_no_incut = 'place=parallel&text=molotok&rearr-factors=market_offers_incut_threshold=0.9;'

        # Врезка есть, без флагов market_enable_offers_wiz_right_incut и market_enable_offers_wiz_center_incut
        # формируется колдунщик market_offers_wizard и market_offers_wizard_center_incut
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response, {"market_offers_wizard": {"showcase": {"items": ElementCount(9), "show_items": "0"}}}
        )
        self.assertFragmentIn(
            response, {"market_offers_wizard_center_incut": {"showcase": {"items": ElementCount(9), "show_items": "1"}}}
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})

        # Врезка есть, под флагами market_enable_offers_wiz_right_incut и market_enable_offers_wiz_center_incut
        # формируются колдунщики market_offers_wizard, market_offers_wizard_right_incut и market_offers_wizard_center_incut
        response = self.report.request_bs_pb(
            request + 'market_enable_offers_wiz_right_incut=1;'
            'market_enable_offers_wiz_center_incut=1;market_offers_center_incut_meta_threshold=0;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {"items": ElementCount(9), "show_items": "0"}  # Флаг - не отображать врезку
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_right_incut": {
                    "showcase": {"items": ElementCount(9), "show_items": "1"}  # Флаг - отображать врезку
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_center_incut": {
                    "showcase": {"items": ElementCount(9), "show_items": "1"}  # Флаг - отображать врезку
                }
            },
        )
        self.access_log.expect(
            wizards=Contains('market_offers_wizard_center_incut'),
            wizard_elements=Contains('market_offers_wizard_center_incut'),
        )
        self.access_log.expect(
            wizards=Contains('market_offers_wizard_right_incut'),
            wizard_elements=Contains('market_offers_wizard_right_incut'),
        )

        # Врезки нет, под флагами market_enable_offers_wiz_right_incut и market_enable_offers_wiz_center_incut
        # формируется только колдунщик market_offers_wizard
        response = self.report.request_bs_pb(
            request_no_incut + 'market_enable_offers_wiz_right_incut=1;' 'market_enable_offers_wiz_center_incut=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {"items": ElementCount(9), "show_items": "0"}  # Флаг - не отображать врезку
                }
            },
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        # Проверяем что для тача колдунщик market_offers_wizard_right_incut не формируется
        for flag in ['&touch=1', ';device=touch']:
            response = self.report.request_bs_pb(
                request + 'market_enable_offers_wiz_right_incut=1;'
                'market_enable_offers_wiz_center_incut=1;market_offers_center_incut_meta_threshold=0;'
                'market_relevance_formula_threshold_on_parallel_for_offers=0;'
                'market_offers_wiz_top_offers_meta_threshold=0;' + flag
            )
            self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
            self.assertFragmentIn(response, {"market_offers_wizard_center_incut": {}})

    def check_content_offers_wizards_with_incut(self, response, wizard_name, clid, clid_touch, has_adg):
        """Проверка выдачи офферного колдунщика со врезкой"""
        self.assertFragmentIn(
            response,
            {
                wizard_name: {
                    "type": "market_constr",
                    "subtype": wizard_name,
                    "counter": {"path": "/snippet/market/{}".format(wizard_name)},
                    "title": "\7[Molotok\7]",
                    "snippetTitle": {"__hl": {"text": "Molotok", "raw": True}},
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                            clid
                        ),
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                            clid_touch
                        ),
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "snippetUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                            clid
                        ),
                        ignore_len=False,
                    ),
                    "snippetUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                            clid_touch
                        ),
                        ignore_len=False,
                    ),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(clid),
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                            ),
                            "snippetText": "Яндекс.Маркет",
                            "snippetUrl": LikeUrl.of(
                                "//market.yandex.ru?clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(clid),
                                ignore_len=False,
                            ),
                            "snippetUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                            ),
                        },
                        {
                            "text": "Molotok",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid
                                ),
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                            ),
                            "snippetText": "Molotok",
                            "snippetUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid
                                ),
                                ignore_len=False,
                            ),
                            "snippetUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                            ),
                        },
                    ],
                    "favicon": {"faviconDomain": "market.yandex.ru"},
                    "text": [{"__hl": {"text": "9 магазинов. Выбор по параметрам.", "raw": True}}],
                    "button": [
                        {
                            "text": "Еще 9 предложений",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid
                                ),
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=molotok&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                    "offer_count": 9,
                    "showcase": {"items": ElementCount(9), "show_items": "1", "isAdv": 1},
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                wizard_name: {
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "source": Contains("//avatars.mdst.yandex.net/get-marketpic/"),
                                    "retinaSource": Contains("//avatars.mdst.yandex.net/get-marketpic/"),
                                    "height": "100",
                                    "width": "100",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/offer/B0F30-tjceMsFsVaO49waw?text=molotok&hid=130&nid=130&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards".format(  # noqa
                                            clid
                                        ),
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//market.yandex.ru/offer/B0F30-tjceMsFsVaO49waw?text=molotok&hid=130&nid=130&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards".format(  # noqa
                                            clid_touch
                                        ),
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/search?text=molotok&cvredirect=0&clid={}&utm_medium=cpc&utm_referrer=wizards".format(
                                            clid
                                        ),
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                },
                                "price": {"type": "average", "priceMax": "100", "currency": "RUR"},
                                "title": {
                                    "text": {"__hl": {"text": "molot-molotok-1", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/offer/B0F30-tjceMsFsVaO49waw?text=molotok&hid=130&nid=130&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards".format(  # noqa
                                            clid
                                        ),
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//market.yandex.ru/offer/B0F30-tjceMsFsVaO49waw?text=molotok&hid=130&nid=130&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards".format(  # noqa
                                            clid_touch
                                        ),
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/search?text=molotok&cvredirect=0&clid={}&utm_medium=cpc&utm_referrer=wizards".format(
                                            clid
                                        ),
                                        ignore_len=False,
                                        ignore_params=['rs', 'cpc'],
                                    ),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                                },
                                "greenUrl": {
                                    "text": "SHOP-301",
                                    "domain": "shop-301.ru",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-301/301/reviews?clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            clid
                                        )
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/grades-shop.xml?shop_id=301&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            clid_touch
                                        )
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )

        if has_adg:
            self.assertFragmentIn(
                response,
                {
                    wizard_name: {
                        "adGUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=molotok&clid=913&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "adGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=molotok&clid=919&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "snippetAdGUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=molotok&clid=913&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "snippetAdGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=molotok&clid=919&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "greenUrl": [
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid=913&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid=919&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetAdGUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid=913&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetAdGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid=919&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                            },
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molotok&clid=913&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molotok&clid=919&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetAdGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molotok&clid=913&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetAdGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molotok&clid=919&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                            },
                        ],
                        "button": [
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molotok&clid=913&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molotok&clid=919&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                            }
                        ],
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/search?text=molotok&cvredirect=0&lr=0&clid=913&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                            ignore_params=['rs'],
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//market.yandex.ru/search?text=molotok&cvredirect=0&lr=0&clid=919&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                            ignore_params=['rs'],
                                        ),
                                    },
                                    "title": {
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/search?text=molotok&cvredirect=0&lr=0&clid=913&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                            ignore_params=['rs'],
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//market.yandex.ru/search?text=molotok&cvredirect=0&lr=0&clid=919&utm_medium=cpc&utm_referrer=wizards&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                            ignore_params=['rs'],
                                        ),
                                    },
                                    "greenUrl": {
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/shop--shop-301/301/reviews?&clid=913&utm_medium=cpc&utm_referrer=wizards"
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/grades-shop.xml?shop_id=301&clid=919&utm_medium=cpc&utm_referrer=wizards"
                                        ),
                                    },
                                }
                            ]
                        },
                    }
                },
            )

    def test_offers_wizard_right_incut(self):
        """Проверяем колдунщик market_offers_wizard_right_incut
        https://st.yandex-team.ru/MARKETOUT-23485
        """
        # Проставляем market_region_in_offers_incut_urls=0, так как он по умолчанию true
        response = self.report.request_bs_pb(
            'place=parallel&text=molotok&rearr-factors=market_offers_incut_threshold=0.1'
            ';market_enable_offers_wiz_right_incut=1;market_region_in_offers_incut_urls=0'
        )
        self.check_content_offers_wizards_with_incut(
            response, "market_offers_wizard_right_incut", clid=830, clid_touch=831, has_adg=True
        )

    def test_offers_wizard_center_incut(self):
        """Проверяем колдунщик market_offers_wizard_center_incut
        https://st.yandex-team.ru/MARKETOUT-23485
        """
        # Проставляем market_region_in_offers_incut_urls=0, так как он по умолчанию true
        response = self.report.request_bs_pb(
            'place=parallel&text=molotok&rearr-factors=market_offers_incut_threshold=0.1'
            ';market_enable_offers_wiz_center_incut=1;market_region_in_offers_incut_urls=0'
        )
        self.check_content_offers_wizards_with_incut(
            response, "market_offers_wizard_center_incut", clid=832, clid_touch=833, has_adg=False
        )

    def test_offers_wizard_center_incut_meta_formula(self):
        """Проверяем работу метаформулы для врезки офферного колдунщика с врезкой в центре
        https://st.yandex-team.ru/MARKETOUT-23485
        """
        request = (
            'place=parallel&text=molotok&trace_wizard=1'
            '&rearr-factors=market_offers_incut_threshold=0.1;market_enable_offers_wiz_center_incut=1;'
        )

        # Под флагом market_offers_center_incut_meta_threshold=0.1 Топ-3 документов врезки проходит порог
        # колдунщик market_offers_wizard_center_incut формируется
        response = self.report.request_bs_pb(request + 'market_offers_center_incut_meta_threshold=0.1')
        self.assertFragmentIn(response, {"market_offers_wizard_center_incut": {}})
        self.assertIn(
            '10 1 OffersWizard.TopOffersMnValue.Center.Meta.Threshold: market_offers_center_incut_meta_threshold=0.1',
            response.get_trace_wizard(),
        )

        # Под флагом market_offers_center_incut_meta_threshold=0.99 Топ-3 документов врезки не проходит порог
        # колдунщик market_offers_wizard_center_incut не формируется
        response = self.report.request_bs_pb(request + 'market_offers_center_incut_meta_threshold=0.99')
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})
        self.assertIn(
            '10 1 OffersWizard.TopOffersMnValue.Center.Meta.Threshold: market_offers_center_incut_meta_threshold=0.99',
            response.get_trace_wizard(),
        )
        self.assertIn(
            '10 4 Did not pass: top 3 meta MatrixNet sum for offers wizard with center incut is too low',
            response.get_trace_wizard(),
        )

    def test_offers_wizard_center_incut_meta_defaults(self):
        """Проверяем, что мета-формула и порог для оферного с центральной врезкой по умолчанию
        берутся из значений оферного с врезкой справа.
        https://st.yandex-team.ru/MARKETOUT-26544
        """
        request = (
            'place=parallel&text=molotok&debug=1&trace_wizard=1'
            '&rearr-factors=market_offers_incut_threshold=0.1;market_enable_offers_wiz_center_incut=1;'
        )

        # без market_offers_center_incut_meta_threshold берется порог врезки справа
        response = self.report.request_bs_pb(request + 'market_offers_incut_meta_threshold=0.1')
        self.assertIn(
            '10 1 OffersWizard.TopOffersMnValue.Center.Meta.Threshold: market_offers_incut_meta_threshold=0.1',
            response.get_trace_wizard(),
        )
        response = self.report.request_bs_pb(request + 'market_offers_incut_meta_threshold=0.05')
        self.assertIn(
            '10 1 OffersWizard.TopOffersMnValue.Center.Meta.Threshold: market_offers_incut_meta_threshold=0.05',
            response.get_trace_wizard(),
        )

        # с market_offers_center_incut_meta_threshold берется указанное значение
        response = self.report.request_bs_pb(
            request + 'market_offers_incut_meta_threshold=0.1;market_offers_center_incut_meta_threshold=0.2;'
        )
        self.assertIn(
            '10 1 OffersWizard.TopOffersMnValue.Center.Meta.Threshold: market_offers_center_incut_meta_threshold=0.2',
            response.get_trace_wizard(),
        )

        # без market_offers_center_incut_meta_mn_algo берется формула врезки справа
        request += 'market_offers_center_incut_meta_threshold=0.2;'
        response = self.report.request_bs(request + 'market_offers_incut_meta_mn_algo=MNA_Trivial')
        self.assertIn(
            'Using offers incut meta MatrixNet formula for offers wizard with center incut: MNA_Trivial', str(response)
        )
        response = self.report.request_bs(request + 'market_offers_incut_meta_mn_algo=MNA_mn201477')
        self.assertIn(
            'Using offers incut meta MatrixNet formula for offers wizard with center incut: MNA_mn201477', str(response)
        )
        # с market_offers_center_incut_meta_mn_algo берется указанное значение
        response = self.report.request_bs(
            request
            + 'market_offers_incut_meta_mn_algo=MNA_Trivial;market_offers_center_incut_meta_mn_algo=MNA_mn201477;'
        )
        self.assertIn(
            'Using offers incut meta MatrixNet formula for offers wizard with center incut: MNA_mn201477', str(response)
        )

    def test_offers_wizard_incut_align(self):
        """Проверяем что под флагами market_enable_offers_wiz_right_incut и market_enable_offers_wiz_center_incut
        офферный колдунщик без врезки содержит все офферы, если нашлось меньше 3х офферов и после выравнивания врезка становится пустой
        https://st.yandex-team.ru/MARKETOUT-23999
        """
        # По запросу находится 1 оффер, в результате выравнивания до 3х врезка становится пустая,
        # под флагами market_enable_offers_wiz_right_incut и market_enable_offers_wiz_center_incut
        # офферные колдунщики с врезкой справа и с врезкой в центре не формируются,
        # офферный колдунщик без врезки содержит один оффер и флаг не отображать врезку
        for flag in [
            'market_enable_offers_wiz_right_incut=1;market_enable_offers_wiz_center_incut=0',
            'market_enable_offers_wiz_right_incut=1;market_enable_offers_wiz_center_incut=1',
            'market_enable_offers_wiz_right_incut=0;market_enable_offers_wiz_center_incut=1',
        ]:
            response = self.report.request_bs_pb(
                'place=parallel&text=panasonic+tumix&rearr-factors=market_offers_incut_threshold=0.1;' + flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "title": "\7[Panasonic tumix\7]",
                        "offer_count": 1,
                        "showcase": {"items": ElementCount(1), "show_items": "0"},
                    }
                },
            )
            self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
            self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        # По запросу находится 1 оффер, в результате выравнивания до 3х врезка становится пустая,
        # без флага market_enable_offers_wiz_right_incut и с market_enable_offers_wiz_center_incut=0
        # формируется только офферный колдунщик без врезки
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors=market_offers_incut_threshold=0.1;market_enable_offers_wiz_center_incut=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Panasonic tumix\7]",
                    "offer_count": 1,
                    "showcase": {"items": ElementCount(0)},
                }
            },
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

    def test_implicit_model_wizards(self):
        """Проверяем, что под флагами market_enable_implicit_model_wiz_center_incut и market_enable_implicit_model_wiz_without_incut
        формируются три колдунщика неявной модели market_implicit_model, market_implicit_model_center_incut и
        market_implicit_model_without_incut
        https://st.yandex-team.ru/MARKETOUT-24077
        """
        request = 'place=parallel&text=panasonic+tumix'
        request_no_incut = 'place=parallel&text=no_incut'

        # Без флагов market_enable_implicit_model_wiz_center_incut и market_enable_implicit_model_wiz_without_incut
        # формируется только колдунщик market_implicit_model
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {"market_implicit_model": {"showcase": {"items": ElementCount(2)}}})
        self.assertFragmentNotIn(response, {"market_implicit_model_center_incut": {}})
        self.assertFragmentNotIn(response, {"market_implicit_model_without_incut": {}})

        # Под флагами market_enable_implicit_model_wiz_center_incut и market_enable_implicit_model_wiz_without_incut
        # формируются колдунщики market_implicit_model, market_implicit_model_center_incut и market_implicit_model_without_incut
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;'
            'market_enable_implicit_model_wiz_without_incut=1'
        )
        self.assertFragmentIn(response, {"market_implicit_model": {"showcase": {"items": ElementCount(2)}}})
        self.assertFragmentIn(
            response, {"market_implicit_model_center_incut": {"showcase": {"items": ElementCount(2)}}}
        )
        self.assertFragmentIn(
            response, {"market_implicit_model_without_incut": {"showcase": {"items": ElementCount(0)}}}
        )

        # Врезки нет, под флагами market_enable_implicit_model_wiz_center_incut и market_enable_implicit_model_wiz_without_incut
        # формируется только колдунщик market_implicit_model_without_incut
        response = self.report.request_bs_pb(
            request_no_incut + '&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;'
            'market_enable_implicit_model_wiz_without_incut=1'
        )
        self.assertFragmentIn(
            response, {"market_implicit_model_without_incut": {"showcase": {"items": ElementCount(0)}}}
        )
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})
        self.assertFragmentNotIn(response, {"market_implicit_model_center_incut": {}})

        # Проверяем, что для тача колдунщик market_implicit_model_center_incut не формируется
        for flag in ['&touch=1', ';device=touch']:
            response = self.report.request_bs_pb(
                request + '&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;'
                'market_enable_implicit_model_wiz_without_incut=1' + flag
            )
            self.assertFragmentIn(response, {"market_implicit_model": {}})
            self.assertFragmentIn(response, {"market_implicit_model_without_incut": {}})
            self.assertFragmentNotIn(response, {"market_implicit_model_center_incut": {}})

    def check_content_implicit_model_wizard(self, response, wizard_name, clid, clid_touch, has_incut, has_adg):
        """Проверка выдачи колдунщика неявной модели"""
        self.assertFragmentIn(
            response,
            {
                wizard_name: {
                    "type": "market_constr",
                    "subtype": wizard_name,
                    "counter": {"path": "/snippet/market/{}".format(wizard_name)},
                    "title": "\7[Panasonic tumix\7]",
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                            clid
                        ),
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                            clid_touch
                        ),
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "reviewsUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&show-reviews=1&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                            clid
                        ),
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "reviewsUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&show-reviews=1&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                            clid_touch
                        ),
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "greenUrl": [
                        {
                            "text": "Яндекс.Маркет",
                            "snippetText": "Яндекс.Маркет",
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(clid),
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                            ),
                        },
                        {
                            "text": "Panasonic tumix",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid
                                ),
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        },
                    ],
                    "favicon": {"faviconDomain": "market.yandex.ru"},
                    "text": [
                        {
                            "__hl": {
                                "text": "Цены, характеристики, отзывы на panasonic tumix. Выбор по параметрам. 1 магазин.",
                                "raw": True,
                            }
                        }
                    ],
                    "button": [
                        {
                            "text": "Еще 1 предложение",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid
                                ),
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                    "offer_count": 1,
                    "sitelinksWithCount": "none",
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid
                                ),
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                            ),
                            "text": "Отзывы",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/geo?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid
                                ),
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/geo?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                            ),
                            "text": "На карте",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid
                                ),
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                    clid_touch
                                ),
                                ignore_len=False,
                            ),
                            "text": "С доставкой завтра",
                        },
                    ],
                    "shop_count": "1",
                    "model_count": "2",
                    "modelId": 101,
                    "showcase": {
                        "items": ElementCount(2 if has_incut else 0),
                    },
                }
            },
        )

        if has_incut:
            self.assertFragmentIn(
                response,
                {
                    wizard_name: {
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "source": LikeUrl.of(
                                            "//avatars.mds.yandex.net/get-mpic/5678/test9901/2hq", ignore_len=False
                                        ),
                                        "retinaSource": LikeUrl.of(
                                            "//avatars.mds.yandex.net/get-mpic/5678/test9901/5hq", ignore_len=False
                                        ),
                                        "height": "100",
                                        "width": "100",
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid
                                            ),
                                            ignore_len=False,
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid_touch
                                            ),
                                            ignore_len=False,
                                        ),
                                    },
                                    "price": {"type": "min", "priceMin": "666", "currency": "RUR"},
                                    "title": {
                                        "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid
                                            ),
                                            ignore_len=False,
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid_touch
                                            ),
                                            ignore_len=False,
                                        ),
                                    },
                                    "rating": {"value": "4.46"},
                                    "reviews": {
                                        "count": "12",
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid
                                            ),
                                            ignore_len=False,
                                        ),
                                        "urlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&clid={}&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                                clid_touch
                                            ),
                                            ignore_len=False,
                                        ),
                                    },
                                    "reasonsToBuy": [],
                                }
                            ]
                        }
                    }
                },
            )

        if has_adg:
            self.assertFragmentIn(
                response,
                {
                    wizard_name: {
                        "adGUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=panasonic%20tumix&clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "adGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "reviewsAdGUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=panasonic%20tumix&show-reviews=1&clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "reviewsAdGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=panasonic%20tumix&show-reviews=1&clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "greenUrl": [
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                            },
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=panasonic%20tumix&clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                            },
                        ],
                        "button": [
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=panasonic%20tumix&clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                            }
                        ],
                        "sitelinks": [
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                            },
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/geo?text=panasonic%20tumix&lr=0&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/geo?text=panasonic%20tumix&lr=0&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                            },
                            {
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                            },
                        ],
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                        ),
                                    },
                                    "title": {
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                        ),
                                    },
                                    "reviews": {
                                        "adGUrl": LikeUrl.of(
                                            "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&clid=915&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                        ),
                                        "adGUrlTouch": LikeUrl.of(
                                            "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&clid=921&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                            ignore_len=False,
                                        ),
                                    },
                                }
                            ],
                        },
                    }
                },
            )

    def test_implicit_model_wizard(self):
        """Проверяем колдунщик market_implicit_model
        https://st.yandex-team.ru/MARKETOUT-24077
        """
        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix')
        self.check_content_implicit_model_wizard(
            response, "market_implicit_model", clid=698, clid_touch=721, has_incut=True, has_adg=True
        )

    def test_implicit_model_wizard_center_incut(self):
        """Проверяем колдунщик market_implicit_model_center_incut
        https://st.yandex-team.ru/MARKETOUT-24077
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors=market_enable_implicit_model_wiz_center_incut=1'
        )
        self.check_content_implicit_model_wizard(
            response, "market_implicit_model_center_incut", clid=836, clid_touch=837, has_incut=True, has_adg=False
        )
        # Проверяем что заполняется access.log для центральной врезки
        self.access_log.expect(
            wizard_elements=Contains('market_implicit_model_center_incut'),
            wizards=Contains('market_implicit_model_center_incut'),
        )

        # Для десктопа флаг market_enable_implicit_model_wiz_center_incut=1 установлен по дефолту
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors=device=desktop;market_implicit_model_wizard_center_incut_meta_threshold=0'
        )
        self.check_content_implicit_model_wizard(
            response, "market_implicit_model_center_incut", clid=836, clid_touch=837, has_incut=True, has_adg=False
        )
        self.access_log.expect(
            wizard_elements=Contains('market_implicit_model_center_incut'),
            wizards=Contains('market_implicit_model_center_incut'),
        )

    def test_implicit_model_wizard_without_incut(self):
        """Проверяем колдунщик market_implicit_model_without_incut
        https://st.yandex-team.ru/MARKETOUT-24077
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors=market_enable_implicit_model_wiz_without_incut=1'
        )
        self.check_content_implicit_model_wizard(
            response, "market_implicit_model_without_incut", clid=834, clid_touch=835, has_incut=False, has_adg=False
        )

    def test_implicit_model_wizard_center_incut_meta_formula(self):
        """Проверяем, что метаформула для колдунщика неявной модели с врезкой в центре
        выставляется флагом market_implicit_model_wizard_center_incut_meta_mn_algo.
        https://st.yandex-team.ru/MARKETOUT-24077
        """
        request = (
            'place=parallel&text=panasonic+tumix'
            '&debug=1'
            '&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;'
            'market_implicit_model_wizard_center_incut_meta_threshold=0.1;'
        )

        # формула по умолчанию
        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response, 'Using implicit model wizard with center incut meta MatrixNet formula: MNA_fml_formula_785353'
        )

        response = self.report.request_bs(
            request + 'market_implicit_model_wizard_center_incut_meta_mn_algo=MNA_mn201477'
        )
        self.assertFragmentIn(
            response, 'Using implicit model wizard with center incut meta MatrixNet formula: MNA_mn201477'
        )

    def test_implicit_model_wizard_center_incut_meta_threshold(self):
        """Проверяем работу порога метаформулы для колдунщика неявной модели с врезкой в центре
        https://st.yandex-team.ru/MARKETOUT-24077
        """
        # Под флагом market_implicit_model_wizard_center_incut_meta_threshold=0.1 Топ-3 документов врезки проходит порог
        # колдунщик market_implicit_model_center_incut формируется
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix'
            '&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;'
            'market_implicit_model_wizard_center_incut_meta_threshold=0.1'
            '&trace_wizard=1'
        )
        self.assertFragmentIn(response, {"market_implicit_model_center_incut": {}})
        self.assertIn(
            '29 1 ImplicitModel.TopModelsMnValue.Center.Meta.Threshold: market_implicit_model_wizard_center_incut_meta_threshold=0.1',
            response.get_trace_wizard(),
        )

        # Под флагом market_implicit_model_wizard_center_incut_meta_threshold=0.99 Топ-3 документов врезки не проходит порог
        # колдунщик market_implicit_model_center_incut не формируется
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix'
            '&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;'
            'market_implicit_model_wizard_center_incut_meta_threshold=0.99'
            '&trace_wizard=1'
        )
        self.assertFragmentNotIn(response, {"market_implicit_model_center_incut": {}})
        self.assertIn(
            '29 1 ImplicitModel.TopModelsMnValue.Center.Meta.Threshold: market_implicit_model_wizard_center_incut_meta_threshold=0.99',
            response.get_trace_wizard(),
        )
        self.assertIn(
            '29 4 Did not pass: top 3 meta MatrixNet sum for implicit model wizard with center incut is too low',
            response.get_trace_wizard(),
        )

    def test_implicit_model_wizard_center_incut_without_right_incut(self):
        """Проверяем, что под флагом market_implicit_model_center_without_right_incut
        формирование колдунщика неявной модели с врезкой по центру не зависит от формироваия с правой врезкой
        https://st.yandex-team.ru/MARKETOUT-27599
        """
        # Без флага market_implicit_model_center_without_right_incut врезка по центру не формируется без правой врезки
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors='
            'market_enable_implicit_model_wiz_center_incut=1;'
            'market_implicit_model_wizard_meta_threshold=0.9;'
            'market_implicit_model_wizard_center_incut_meta_threshold=0.1'
        )
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})
        self.assertFragmentNotIn(response, {"market_implicit_model_center_incut": {}})

        # Под флагом market_implicit_model_center_without_right_incut врезка по центру формируется без правой врезки
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors='
            'market_enable_implicit_model_wiz_center_incut=1;'
            'market_implicit_model_center_without_right_incut=1;'
            'market_implicit_model_wizard_meta_threshold=0.9;'
            'market_implicit_model_wizard_center_incut_meta_threshold=0.1'
        )
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})
        self.assertFragmentIn(response, {"market_implicit_model_center_incut": {}})

    def test_implicit_model_wizard_center_incut_meta_defaults(self):
        """Проверяем, что с флагом market_fixed_implicit_model_center_incut_meta_defaults
        мета-формула и порог для неявной модели с центральной врезкой по умолчанию
        берутся из значений неявной модели с врезкой справа.
        https://st.yandex-team.ru/MARKETOUT-27598
        Флаг market_fixed_implicit_model_center_incut_meta_defaults=1 раскатан по дефолту.
        https://st.yandex-team.ru/MARKETOUT-28751
        """
        request = (
            'place=parallel&text=panasonic+tumix&debug=1&trace_wizard=1'
            '&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;'
        )

        # без market_implicit_model_wizard_center_incut_meta_threshold берется порог врезки справа c флагом market_fixed_implicit_model_center_incut_meta_defaults=0
        response = self.report.request_bs_pb(
            request
            + 'market_implicit_model_wizard_meta_threshold=0.1;market_fixed_implicit_model_center_incut_meta_defaults=0'
        )
        self.assertNotIn(
            '29 1 ImplicitModel.TopModelsMnValue.Center.Meta.Threshold: market_implicit_model_wizard_meta_threshold=0.1',
            response.get_trace_wizard(),
        )

        response = self.report.request_bs_pb(request + 'market_implicit_model_wizard_meta_threshold=0.1')
        self.assertIn(
            '29 1 ImplicitModel.TopModelsMnValue.Center.Meta.Threshold: market_implicit_model_wizard_meta_threshold=0.1',
            response.get_trace_wizard(),
        )

        response = self.report.request_bs_pb(request + 'market_implicit_model_wizard_meta_threshold=0.2')
        self.assertIn(
            '29 1 ImplicitModel.TopModelsMnValue.Center.Meta.Threshold: market_implicit_model_wizard_meta_threshold=0.2',
            response.get_trace_wizard(),
        )

        # с market_implicit_model_wizard_center_incut_meta_threshold берется указанное значение
        for flag in ('', 'market_fixed_implicit_model_center_incut_meta_defaults=0'):
            response = self.report.request_bs_pb(
                request
                + 'market_implicit_model_wizard_meta_threshold=0.1;market_implicit_model_wizard_center_incut_meta_threshold=0.2;'
                + flag
            )
            self.assertIn(
                '29 1 ImplicitModel.TopModelsMnValue.Center.Meta.Threshold: market_implicit_model_wizard_center_incut_meta_threshold=0.2',
                response.get_trace_wizard(),
            )

        # без market_implicit_model_wizard_center_incut_meta_mn_algo берется формула врезки справа
        request += 'market_implicit_model_wizard_center_incut_meta_threshold=0.2;'
        response = self.report.request_bs(
            request
            + 'market_implicit_model_wizard_meta_mn_algo=MNA_Trivial;market_fixed_implicit_model_center_incut_meta_defaults=0'
        )
        self.assertNotIn(
            'Using implicit model wizard with center incut meta MatrixNet formula: MNA_Trivial', str(response)
        )

        response = self.report.request_bs(request + 'market_implicit_model_wizard_meta_mn_algo=MNA_Trivial')
        self.assertIn(
            'Using implicit model wizard with center incut meta MatrixNet formula: MNA_Trivial', str(response)
        )

        response = self.report.request_bs(request + 'market_implicit_model_wizard_meta_mn_algo=MNA_mn201477')
        self.assertIn(
            'Using implicit model wizard with center incut meta MatrixNet formula: MNA_mn201477', str(response)
        )

        # с market_implicit_model_wizard_center_incut_meta_mn_algo берется указанное значение
        for flag in ('', 'market_fixed_implicit_model_center_incut_meta_defaults=0'):
            response = self.report.request_bs(
                request
                + 'market_implicit_model_wizard_meta_mn_algo=MNA_Trivial;market_implicit_model_wizard_center_incut_meta_mn_algo=MNA_mn201477;'
                + flag
            )
            self.assertIn(
                'Using implicit model wizard with center incut meta MatrixNet formula: MNA_mn201477', str(response)
            )

    def test_implicit_model_wizard_center_incut_collapsed_threshold(self):
        """Проверка, что под флагом market_fixed_implicit_model_center_incut_meta_defaults
        в колдунщике неявной модели для центральной врезки после схлопывания
        формула и порог используются такие-же, как для правой врезки
        https://st.yandex-team.ru/MARKETOUT-27599
        Флаг market_fixed_implicit_model_center_incut_meta_defaults=1 раскатан по дефолту.
        https://st.yandex-team.ru/MARKETOUT-28751
        """
        request = (
            '&place=parallel&trace_wizard=1&debug=1'
            '&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;'
            'market_implicit_model_wizard_meta_threshold=0.1;'
            'market_implicit_model_wizard_collapsing_meta_mn_algo=MNA_fml_formula_291791;'
            'market_implicit_model_wizard_collapsing_meta_threshold=0.2;'
            'market_implicit_model_wizard_center_incut_meta_mn_algo=MNA_mn201477;'
            'market_implicit_model_wizard_center_incut_meta_threshold=0.3;'
        )

        # Без схлопывания используется порог из market_implicit_model_wizard_center_incut_meta_threshold
        response = self.report.request_bs('text=panasonic+tumix' + request)
        self.assertFragmentIn(
            response,
            '29 1 ImplicitModel.TopModelsMnValue.Center.Meta.Threshold: market_implicit_model_wizard_center_incut_meta_threshold=0.3',
        )
        self.assertFragmentIn(
            response, 'Using implicit model wizard with center incut meta MatrixNet formula: MNA_mn201477'
        )

        # После схлопывания используется порог из market_implicit_model_wizard_collapsing_meta_threshold
        response = self.report.request_bs('text=otherKettle' + request)
        self.assertFragmentIn(response, '29 2 При схлопывании получено 2 моделей')
        self.assertFragmentIn(
            response,
            '29 1 ImplicitModel.TopCollapsingModelsMnValue.Center.Meta.Threshold: market_implicit_model_wizard_collapsing_meta_threshold=0.2',
        )
        self.assertFragmentIn(
            response, 'Using implicit model wizard with center incut meta MatrixNet formula: MNA_fml_formula_291791'
        )

    def test_implicit_model_adg_wizard_collapsed_threshold(self):
        """Проверка, что в колдунщике неявной модели для РГ после схлопывания
        формула и порог используются такие-же, как для правой врезки
        https://st.yandex-team.ru/MARKETOUT-27599
        """
        request = (
            '&place=parallel&trace_wizard=1&debug=1'
            '&rearr-factors=market_enable_implicit_model_adg_wiz=1;'
            'market_implicit_model_wizard_meta_threshold=0.1;'
            'market_implicit_model_wizard_collapsing_meta_mn_algo=MNA_fml_formula_291791;'
            'market_implicit_model_wizard_collapsing_meta_threshold=0.2;'
            'market_implicit_model_adg_wizard_meta_mn_algo=MNA_mn201477;'
            'market_implicit_model_adg_wizard_meta_threshold=0.3;'
        )

        # Без схлопывания используется порог из market_implicit_model_adg_wizard_meta_threshold
        response = self.report.request_bs('text=panasonic+tumix' + request)
        self.assertFragmentIn(
            response,
            '29 1 ImplicitModel.TopModelsMnValue.AdGIncut.Meta.Threshold: market_implicit_model_adg_wizard_meta_threshold=0.3',
        )
        self.assertFragmentIn(response, 'Using implicit model AdG wizard meta MatrixNet formula: MNA_mn201477')

        # После схлопывания используется порог из market_implicit_model_wizard_collapsing_meta_threshold
        response = self.report.request_bs('text=otherKettle' + request)
        self.assertFragmentIn(response, '29 2 При схлопывании получено 2 моделей')
        self.assertFragmentIn(
            response,
            '29 1 ImplicitModel.TopCollapsingModelsMnValue.AdGIncut.Meta.Threshold: market_implicit_model_wizard_collapsing_meta_threshold=0.2',
        )
        self.assertFragmentIn(
            response, 'Using implicit model AdG wizard meta MatrixNet formula: MNA_fml_formula_291791'
        )

    def test_implicit_model_adg_wizard_meta_formula(self):
        """Проверяем, что метаформула для колдунщика неявной модели для РГ
        выставляется флагом market_implicit_model_adg_wizard_meta_mn_algo.
        https://st.yandex-team.ru/MARKETOUT-26584
        https://st.yandex-team.ru/MARKETOUT-27599
        """
        request = (
            'place=parallel&text=panasonic+tumix'
            '&debug=1'
            '&rearr-factors=market_implicit_model_wizard_meta_mn_algo=MNA_Trivial;'
            'market_enable_implicit_model_adg_wiz=1;'
            'market_implicit_model_adg_wizard_meta_threshold=0.1;'
        )

        # без market_implicit_model_adg_wizard_meta_mn_algo берется формула врезки справа
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, 'Using implicit model AdG wizard meta MatrixNet formula: MNA_Trivial')
        # с market_implicit_model_adg_wizard_meta_mn_algo берется указанное значение
        response = self.report.request_bs(request + 'market_implicit_model_adg_wizard_meta_mn_algo=MNA_mn201477')
        self.assertFragmentIn(response, 'Using implicit model AdG wizard meta MatrixNet formula: MNA_mn201477')

        # При схлопывании по умолчанию используется market_implicit_model_wizard_collapsing_meta_mn_algo
        offer_request = (
            'place=parallel&text=ToysOffer'
            '&debug=1'
            '&rearr-factors=market_implicit_model_wizard_meta_mn_algo=MNA_Trivial;'
            'market_implicit_model_wizard_collapsing_meta_mn_algo=MNA_Relevance;'
            'market_enable_implicit_model_adg_wiz=1;'
            'market_implicit_model_adg_wizard_meta_threshold=0.1;'
        )

        # без market_implicit_model_adg_wizard_meta_mn_algo берется формула по умолчанию
        response = self.report.request_bs(offer_request)
        self.assertFragmentIn(response, 'Using implicit model AdG wizard meta MatrixNet formula: MNA_Relevance')
        # с market_implicit_model_adg_wizard_meta_mn_algo берется формула по умолчанию
        response = self.report.request_bs(offer_request + 'market_implicit_model_adg_wizard_meta_mn_algo=MNA_mn201477')
        self.assertFragmentIn(response, 'Using implicit model AdG wizard meta MatrixNet formula: MNA_Relevance')

    def test_implicit_model_adg_wizard_meta_threshold(self):
        """Проверяем работу порога метаформулы для колдунщика неявной модели для РГ
        https://st.yandex-team.ru/MARKETOUT-26584
        """
        request = (
            'place=parallel&text=panasonic+tumix'
            '&trace_wizard=1'
            '&rearr-factors=market_enable_implicit_model_adg_wiz=1;'
        )

        # без флага market_implicit_model_adg_wizard_meta_threshold берется порог врезки справа
        response = self.report.request_bs_pb(request + 'market_implicit_model_wizard_meta_threshold=0.1;')
        self.assertFragmentIn(response, {"market_implicit_model_adg_wizard": {}})
        self.assertIn(
            '29 1 ImplicitModel.TopModelsMnValue.AdGIncut.Meta.Threshold: market_implicit_model_wizard_meta_threshold=0.1',
            response.get_trace_wizard(),
        )
        response = self.report.request_bs_pb(request + 'market_implicit_model_wizard_meta_threshold=0.05;')
        self.assertFragmentIn(response, {"market_implicit_model_adg_wizard": {}})
        self.assertIn(
            '29 1 ImplicitModel.TopModelsMnValue.AdGIncut.Meta.Threshold: market_implicit_model_wizard_meta_threshold=0.05',
            response.get_trace_wizard(),
        )

        # при схлопывании без флага market_implicit_model_adg_wizard_meta_threshold
        # берется порог врезки справа для схлопывания
        request_with_collapsing = (
            'place=parallel&text=ToysOffer&trace_wizard=1' '&rearr-factors=market_enable_implicit_model_adg_wiz=1;'
        )
        response = self.report.request_bs_pb(
            request_with_collapsing + 'market_implicit_model_wizard_collapsing_meta_threshold=0.09'
        )
        self.assertFragmentIn(response, {"market_implicit_model_adg_wizard": {}})
        self.assertIn(
            '29 1 ImplicitModel.TopCollapsingModelsMnValue.AdGIncut.Meta.Threshold: market_implicit_model_wizard_collapsing_meta_threshold=0.09',
            response.get_trace_wizard(),
        )
        response = self.report.request_bs_pb(
            request_with_collapsing + 'market_implicit_model_wizard_collapsing_meta_threshold=0.08'
        )
        self.assertFragmentIn(response, {"market_implicit_model_adg_wizard": {}})
        self.assertIn(
            '29 1 ImplicitModel.TopCollapsingModelsMnValue.AdGIncut.Meta.Threshold: market_implicit_model_wizard_collapsing_meta_threshold=0.08',
            response.get_trace_wizard(),
        )

        # Под флагом market_implicit_model_adg_wizard_meta_threshold=0.1 топ-3 документов врезки проходит порог,
        # колдунщик market_implicit_model_adg_wizard формируется
        response = self.report.request_bs_pb(
            request + 'market_implicit_model_wizard_meta_threshold=0.05;'
            'market_implicit_model_adg_wizard_meta_threshold=0.1;'
        )
        self.assertFragmentIn(response, {"market_implicit_model_adg_wizard": {}})
        self.assertIn(
            '29 1 ImplicitModel.TopModelsMnValue.AdGIncut.Meta.Threshold: market_implicit_model_adg_wizard_meta_threshold=0.1',
            response.get_trace_wizard(),
        )

        # Под флагом market_implicit_model_adg_wizard_meta_threshold=0.99 топ-3 документов врезки не проходит порог,
        # колдунщик market_implicit_model_adg_wizard не формируется
        response = self.report.request_bs_pb(
            request + 'market_implicit_model_wizard_meta_threshold=0.05;'
            'market_implicit_model_adg_wizard_meta_threshold=0.99;'
        )
        self.assertFragmentNotIn(response, {"market_implicit_model_adg_wizard": {}})
        self.assertIn(
            '29 1 ImplicitModel.TopModelsMnValue.AdGIncut.Meta.Threshold: market_implicit_model_adg_wizard_meta_threshold=0.99',
            response.get_trace_wizard(),
        )
        self.assertIn(
            '29 4 Did not pass: top 3 meta MatrixNet sum for implicit model AdG wizard is too low',
            response.get_trace_wizard(),
        )

    @classmethod
    def prepare_delivery_and_discount_in_offer_incut(cls):
        cls.index.regiontree += [
            Region(rid=54, name='Екатеринбург', genitive='Екатеринбурга', locative='Екатеринбурге', preposition='в'),
            Region(rid=56, name='Челябинск', genitive='Челябинска', locative='Челябинске', preposition='в'),
        ]

        cls.index.shops += [
            Shop(fesh=23414, priority_region=54, pickup_buckets=[5004]),
            Shop(fesh=23415, priority_region=54, pickup_buckets=[5005]),
            Shop(fesh=23416, priority_region=54, pickup_buckets=[5006]),
            Shop(fesh=23417, priority_region=54, pickup_buckets=[5007]),
            Shop(fesh=23418, priority_region=56, regions=[54, 56], pickup_buckets=[5008]),
            Shop(fesh=23419, priority_region=54, pickup_buckets=[5009]),
        ]
        cls.index.outlets += [
            Outlet(fesh=23414, region=54, point_id=4),
            Outlet(fesh=23415, region=54, point_id=5),
            Outlet(fesh=23416, region=54, point_id=6),
            Outlet(fesh=23417, region=54, point_id=7),
            Outlet(fesh=23418, region=56, point_id=8),
            Outlet(fesh=23419, region=54, point_id=9),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5004,
                fesh=23414,
                carriers=[99],
                options=[PickupOption(outlet_id=4)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=23415,
                carriers=[99],
                options=[PickupOption(outlet_id=5)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5006,
                fesh=23416,
                carriers=[99],
                options=[PickupOption(outlet_id=6)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5007,
                fesh=23417,
                carriers=[99],
                options=[PickupOption(outlet_id=7)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5008,
                fesh=23418,
                carriers=[99],
                options=[PickupOption(outlet_id=8)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5009,
                fesh=23419,
                carriers=[99],
                options=[PickupOption(outlet_id=9)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            # Оффер для формирования большой врезки, у него ничего не будем проверять
            Offer(title='shakshuka-1', ts=23414, fesh=23414),
            # Оффер со скидкой
            Offer(title='shakshuka-2', ts=23415, fesh=23415, price=1234, price_old=2345),
            # Оффер с местной доставкой, в выдаче будет её цена
            Offer(title='shakshuka-3', ts=23416, fesh=23416),  # local
            # Бесплатная доставка
            Offer(
                title='shakshuka-4',
                ts=23417,
                fesh=23417,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=2, order_before=23)],
            ),
            # Оффер с доставкой из другого региона, ничего не будет в инфе о доставке
            Offer(title='shakshuka-5', ts=23418, fesh=23418),  # non-local
            # Самовывоз
            Offer(title='shakshuka-6', ts=23419, fesh=23419, has_delivery_options=False, pickup=True),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23414).respond(1 * 0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23415).respond(2 * 0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23416).respond(3 * 0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23417).respond(4 * 0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23418).respond(5 * 0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23419).respond(6 * 0.9)

    def test_delivery_and_discount_in_offer_incut(self):
        """Информация о доставке и скидке во врезке

        https://st.yandex-team.ru/MARKETOUT-23413
        """

        response = self.report.request_bs_pb('place=parallel&text=shakshuka&rids=54')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "shakshuka-6", "raw": True}},
                                },
                                "delivery": {
                                    "text": "Самовывоз",
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "shakshuka-4", "raw": True}},
                                },
                                "delivery": {
                                    "text": "Доставка бесплатно",
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "shakshuka-3", "raw": True}},
                                },
                                "delivery": {"price": "100", "currency": "RUR"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "shakshuka-2", "raw": True}},
                                },
                                "discount": {"percent": "47", "oldprice": "2345", "currency": "RUR"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "shakshuka-1", "raw": True}},
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "shakshuka-5", "raw": True}},
                                },
                                "delivery": {
                                    "text": NoKey("text"),
                                    "price": NoKey("price"),
                                    "currency": NoKey("currency"),
                                },
                            },
                        ]
                    }
                }
            },
        )

    def test_dup_hosts(self):
        """Проверяем, что в bsformat=7 у всех колдунщиков есть DupHostsAttr
        https://st.yandex-team.ru/MARKETOUT-24006
        https://st.yandex-team.ru/MARKETOUT-27229

        Баним все хосты вне зависимости от платформы https://st.yandex-team.ru/MARKETOUT-26023
        """

        for wizard, text in (
            ('market_ext_category', 'food'),
            ('market_model', 'smart+hero'),
            ('market_offers_wizard', 'kiyanka'),
            ('market_implicit_model', 'panasonic+tumix'),
        ):
            # 1. Десктоп
            response = self.report.request_bs_pb('place=parallel&text={0}'.format(text))
            self.assertEqual(
                response.get_wizard(wizard).dup_hosts,
                "market.yandex.ru|market.yandex.ua|market.yandex.by|market.yandex.kz|m.market.yandex.ru|m.market.yandex.ua|m.market.yandex.by|m.market.yandex.kz",
            )
            # 2. Тач
            response = self.report.request_bs_pb('place=parallel&text={0}&touch=1'.format(text))
            self.assertEqual(
                response.get_wizard(wizard).dup_hosts,
                "market.yandex.ru|market.yandex.ua|market.yandex.by|market.yandex.kz|m.market.yandex.ru|m.market.yandex.ua|m.market.yandex.by|m.market.yandex.kz",
            )

    def test_rearr_flags_priority(self):
        """Проверяем, что в rearr-флагах используется последнее значение флага
        https://st.yandex-team.ru/MARKETOUT-24528
        https://st.yandex-team.ru/MARKETOUT-36478
        https://st.yandex-team.ru/MARKETOUT-36898
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&trace_wizard=1&rearr-factors='
            'market_offers_incut_threshold=0.1;'
            'market_offers_incut_threshold=0.5;'
            'market_offers_incut_threshold=0.9'
        )
        self.assertIn(
            '10 1 OffersWizard.TopOffersMnValue.Base.Threshold: market_offers_incut_threshold=0.9',
            response.get_trace_wizard(),
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&trace_wizard=1&rearr-factors='
            'market_offers_incut_threshold=0.9;'
            'market_offers_incut_threshold=0.5;'
            'market_offers_incut_threshold=0.1'
        )
        self.assertIn(
            '10 1 OffersWizard.TopOffersMnValue.Base.Threshold: market_offers_incut_threshold=0.1',
            response.get_trace_wizard(),
        )

        # Проверка для флагов из TRearrFactors (market/report/library/cgi/params.cpp)
        response = self.report.request_bs(
            'place=parallel&text=iphone&debug=1'
            '&rearr-factors=panther_parallel_tpsz=100;panther_parallel_tpsz=200'
            '&rearr-factors=panther_parallel_tpsz=300;panther_parallel_tpsz=400'
        )
        self.assertFragmentIn(response, '<value>panther_top_size_=400</value>')

        response = self.report.request_bs(
            'place=parallel&text=iphone&debug=1'
            '&rearr-factors=panther_parallel_tpsz=400;panther_parallel_tpsz=300'
            '&rearr-factors=panther_parallel_tpsz=200;panther_parallel_tpsz=100'
        )
        self.assertFragmentIn(response, '<value>panther_top_size_=100</value>')

    def test_offers_wizard_model_landing(self):
        """При наличии модели у оффера переходим с врезки на КМ, прокидывая при этом ware_md5 оффера

        https://st.yandex-team.ru/MARKETOUT-25181
        """

        def do_test(request, expected):
            response = self.report.request_bs_pb(request)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": item_title, "raw": True}},
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                        "urlForCounter": url_for_counter,
                                    },
                                    "thumb": {
                                        "offercardUrl": Contains("/redir/dtype=offercard/"),
                                        "urlForCounter": url_for_counter,
                                    },
                                }
                                for item_title, url_for_counter in expected
                            ]
                        }
                    }
                },
            )

        request = 'place=parallel&text=hoover'

        # 1. По умолчанию urlForCounter это переход на search с бустингом оффера
        do_test(
            request,
            [
                (
                    "hoover 1",
                    LikeUrl.of("//market.yandex.ru/search?text=hoover&cvredirect=0&clid=545", ignore_params=['rs']),
                ),
                (
                    "hoover 2",
                    LikeUrl.of("//market.yandex.ru/search?text=hoover&cvredirect=0&clid=545", ignore_params=['rs']),
                ),
                (
                    "hoover offer only hooverOffer 3",
                    LikeUrl.of("//market.yandex.ru/search?text=hoover&cvredirect=0&clid=545", ignore_params=['rs']),
                ),
            ],
        )

        # С флагом market_offers_wizard_model_landing=1 вместо переходов в магазин
        # переход на КМ с параметром &chosen_offer_id
        do_test(
            request + '&rearr-factors=market_offers_wizard_model_landing=1',
            [
                (
                    "hoover 1",
                    LikeUrl.of(
                        "//market.yandex.ru/product--hoover-1/901?chosen_offer_id=LuEE8VOUlal6_JoFbM54ug&clid=545"
                    ),
                ),
                (
                    "hoover 2",
                    LikeUrl.of(
                        "//market.yandex.ru/product--hoover-2/902?chosen_offer_id=VZVYn_bQ5W7wWFX_oxhqDA&clid=545"
                    ),
                ),
                (
                    "hoover offer only hooverOffer 3",
                    LikeUrl.of("//market.yandex.ru/search?text=hoover&cvredirect=0&clid=545", ignore_params=['rs']),
                ),
            ],
        )

        # 2. Проверяем связку с https://st.yandex-team.ru/MARKETOUT-24859
        do_test(
            request + '&rearr-factors=market_offers_wizard_model_docs_min_size=1',
            [
                (
                    "hoover model 1",
                    LikeUrl.of("//market.yandex.ru/product--hoover-1/901?clid=545", no_params=['chosen_offer_id']),
                ),
                (
                    "hoover model 2",
                    LikeUrl.of("//market.yandex.ru/product--hoover-2/902?clid=545", no_params=['chosen_offer_id']),
                ),
                (
                    "hoover 1",
                    LikeUrl.of("//market.yandex.ru/search?text=hoover&cvredirect=0&clid=545", ignore_params=['rs']),
                ),
                (
                    "hoover 2",
                    LikeUrl.of("//market.yandex.ru/search?text=hoover&cvredirect=0&clid=545", ignore_params=['rs']),
                ),
                (
                    "hoover offer only hooverOffer 3",
                    LikeUrl.of("//market.yandex.ru/search?text=hoover&cvredirect=0&clid=545", ignore_params=['rs']),
                ),
            ],
        )

        # С флагом market_offers_wizard_model_landing=1 переходы на поднятые наверх
        # модели не портятся
        do_test(
            request + '&rearr-factors=market_offers_wizard_model_docs_min_size=1;market_offers_wizard_model_landing=1',
            [
                (
                    "hoover model 1",
                    LikeUrl.of("//market.yandex.ru/product--hoover-1/901?clid=545", no_params=['chosen_offer_id']),
                ),
                (
                    "hoover model 2",
                    LikeUrl.of("//market.yandex.ru/product--hoover-2/902?clid=545", no_params=['chosen_offer_id']),
                ),
                (
                    "hoover 1",
                    LikeUrl.of(
                        "//market.yandex.ru/product--hoover-1/901?chosen_offer_id=LuEE8VOUlal6_JoFbM54ug&clid=545"
                    ),
                ),
                (
                    "hoover 2",
                    LikeUrl.of(
                        "//market.yandex.ru/product--hoover-2/902?chosen_offer_id=VZVYn_bQ5W7wWFX_oxhqDA&clid=545"
                    ),
                ),
                (
                    "hoover offer only hooverOffer 3",
                    LikeUrl.of("//market.yandex.ru/search?text=hoover&cvredirect=0&clid=545", ignore_params=['rs']),
                ),
            ],
        )

        # 3. Проверяем связку с https://st.yandex-team.ru/MARKETOUT-22344
        do_test(
            request + '&rearr-factors=market_offers_wizard_incut_url_type=OfferCard',
            [
                ("hoover 1", Contains("/redir/dtype=offercard/")),
                ("hoover 2", Contains("/redir/dtype=offercard/")),
                ("hoover offer only hooverOffer 3", Contains("/redir/dtype=offercard/")),
            ],
        )

        # С флагом market_offers_wizard_model_landing=1 переходы на КО превращаются в
        # наши переходы на КМ
        do_test(
            request
            + '&rearr-factors=market_offers_wizard_incut_url_type=OfferCard;market_offers_wizard_model_landing=1',
            [
                (
                    "hoover 1",
                    LikeUrl.of(
                        "//market.yandex.ru/product--hoover-1/901?chosen_offer_id=LuEE8VOUlal6_JoFbM54ug&clid=545"
                    ),
                ),
                (
                    "hoover 2",
                    LikeUrl.of(
                        "//market.yandex.ru/product--hoover-2/902?chosen_offer_id=VZVYn_bQ5W7wWFX_oxhqDA&clid=545"
                    ),
                ),
                ("hoover offer only hooverOffer 3", Contains("/redir/dtype=offercard/")),
            ],
        )

        # 4. Проверяем хост и clid на таче
        do_test(
            request + '&rearr-factors=market_offers_wizard_model_landing=1;offers_touch=1&touch=1',
            [
                (
                    "hoover 1",
                    LikeUrl.of(
                        "//m.market.yandex.ru/product--hoover-1/901?chosen_offer_id=LuEE8VOUlal6_JoFbM54ug&clid=708"
                    ),
                )
            ],
        )

    def test_offer_id_in_incut(self):
        """Проверяем, что под &debug=1 у офферов во врезке выводятся поля wareMd5, docUrl и urlHash
        и они соответствуют полям ware_md5, url и url_hash в офферных факторах.
        https://st.yandex-team.ru/MARKETOUT-26115
        """
        response = self.report.request_bs(
            'place=parallel&text=kiyanka&debug=1&rearr-factors=market_check_offers_incut_size=0;market_offers_incut_threshold=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "title": "\7[Kiyanka\7]",
                        "showcase": {
                            "items": [
                                {
                                    "wareMd5": "f7_BYaO4c78hGceI7ZPR9A",
                                    "docUrl": "http://kiyanochnaya.ru/kiyanki?id=1",
                                    "urlHash": "12345123451",
                                }
                            ]
                        },
                    }
                ],
                "offerFactors": [
                    {
                        "ware_md5": "f7_BYaO4c78hGceI7ZPR9A",
                        "url": "http://kiyanochnaya.ru/kiyanki?id=1",
                        "url_hash": "12345123451",
                    }
                ],
            },
        )

    def test_offers_turbo_urls(self):
        """
        Проверяем, что подставляются турбо урлы для офферов в офферном колдунщике
        https://st.yandex-team.ru/MARKETOUT-26313
        """

        request = 'place=parallel&text=pasatizi&rearr-factors=market_offers_wizard_turbo_urls=1;market_offers_incut_show_always=1;'
        response = self.report.request_bs_pb(request + 'market_offers_wizard_incut_url_type=External')

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "pasatizi-11", "raw": True}},
                                    "turboUrl": Absent(),
                                    "url": LikeUrl.of("https://test.pasatizishop.ru/offer/id11"),
                                }
                            },
                            {"title": {"text": {"__hl": {"text": "pasatizi-12", "raw": True}}, "turboUrl": Absent()}},
                        ]
                    }
                }
            },
        )

        request += 'device=touch;'
        response = self.report.request_bs_pb(request + 'market_offers_wizard_incut_url_type=External')

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "pasatizi-11", "raw": True}},
                                    "turboUrl": Contains(
                                        'https%3A%2F%2Fhamster.yandex.ru%2Fturbo%3Ftext%3Dhttps%3A%2F%2Ftest.pasatizishop.ru%2Foffer%2Fid11%26clid%3D934'
                                    ),
                                    "url": LikeUrl.of("https://test.pasatizishop.ru/offer/id11"),
                                }
                            },
                            {"title": {"text": {"__hl": {"text": "pasatizi-12", "raw": True}}, "turboUrl": Absent()}},
                        ]
                    }
                }
            },
        )

        request += 'market_offers_wizard_incut_url_type=OfferCard;'
        response = self.report.request_bs_pb(request)

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "pasatizi-11", "raw": True}},
                                    "offercardTurboUrl": Contains(
                                        'https%3A%2F%2Fhamster.yandex.ru%2Fturbo%3Ftext%3Dhttps%253A%252F%252Fmarket.yandex.ru%252Foffer',
                                        "clid%253D935",
                                    ),
                                    "offercardUrl": Contains("//market-click2.yandex.ru/redir/dtype=offercard"),
                                }
                            }
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_offers_reasons_to_buy(cls):
        cls.index.models += [
            Model(hyperid=771, hid=1000, title='Кот'),
        ]
        cls.index.offers += [
            Offer(title='Кот 1', hyperid=771, price=1000),
            Offer(title='Кот 2', hyperid=771, price=1003),
            Offer(title='Кот 3', hyperid=771, price=1001),
            Offer(title='Кот 4', hyperid=771, price=1002),
        ]

    def test_market_offers_reasons_to_buy(self):
        response = self.report.request_bs_pb(
            'place=parallel&text=Кот&rearr-factors=market_offers_incut_show_always=1;split=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "reasonsToBuy": [
                                    {
                                        "factor_name": "Красивый",
                                        "type": "consumerFactor",
                                        "factor_priority": "1",
                                    },
                                    {
                                        "factor_name": "Добрый",
                                        "type": "consumerFactor",
                                        "factor_priority": "2",
                                    },
                                ]
                            }
                        ]
                    }
                }
            },
        )

    def test_market_offers_wizard_wprid(self):
        """https://st.yandex-team.ru/MARKETOUT-26310"""
        request = 'place=parallel&text=molot+molotok&rearr-factors=market_offers_incut_threshold=0.0;market_offers_wiz_top_offers_threshold=0;market_parallel_wprids=1&wprid=offers_wprid'
        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "url": LikeUrl.of(
                            "//market.yandex.ru/search?text=molot%20molotok&clid=545&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=molot%20molotok&clid=708&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "adGUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=molot%20molotok&clid=913&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "adGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=molot%20molotok&clid=919&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                            ignore_params=['rs'],
                        ),
                        "snippetUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=molot%20molotok&clid=545&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "snippetUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=molot%20molotok&clid=708&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "snippetAdGUrl": LikeUrl.of(
                            "//market.yandex.ru/search?text=molot%20molotok&clid=913&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "snippetAdGUrlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=molot%20molotok&clid=919&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "url_for_category_name": LikeUrl.of(
                            "//market.yandex.ru/search?text=molot%20molotok&clid=545&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "greenUrl": [
                            {
                                "snippetUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid=545&wprid=offers_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetAdGUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid=913&wprid=offers_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid=708&wprid=offers_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetAdGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid=919&wprid=offers_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "url": LikeUrl.of(
                                    "//market.yandex.ru?clid=545&wprid=offers_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru?clid=913&wprid=offers_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid=708&wprid=offers_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru?clid=919&wprid=offers_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                            },
                            {
                                "snippetUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molot%20molotok&clid=545&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetAdGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molot%20molotok&clid=913&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molot%20molotok&clid=708&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "snippetAdGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molot%20molotok&clid=919&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molot%20molotok&clid=545&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molot%20molotok&clid=913&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molot%20molotok&clid=708&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molot%20molotok&clid=919&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                ),
                            },
                        ],
                        "button": [
                            {
                                "url": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molot%20molotok&clid=545&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "adGUrl": LikeUrl.of(
                                    "//market.yandex.ru/search?text=molot%20molotok&clid=913&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "urlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molot%20molotok&clid=708&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                                "adGUrlTouch": LikeUrl.of(
                                    "//m.market.yandex.ru/search?text=molot%20molotok&clid=919&lr=0&wprid=offers_wprid&utm_medium=cpc&utm_referrer=wizards",
                                    ignore_len=False,
                                    ignore_params=['rs'],
                                ),
                            }
                        ],
                    }
                ]
            },
        )

        # NB: поле wprid= в кликлоге заполняется в любом случае, если есть указан
        # cgi-параметр &wprid=.
        # Проброс в урл же производится только под флагом и только на маркетные посадочные.
        # https://st.yandex-team.ru/MARKETOUT-27300
        self.click_log.expect(
            dtype="offercard", position=1, data_url=Contains("wprid%3Doffers_wprid"), wprid="offers_wprid"
        )

    def test_market_implicit_model_wprid(self):
        """https://st.yandex-team.ru/MARKETOUT-26310"""

        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&wprid=implicit_wprid&rearr-factors=market_parallel_wprids=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&clid=698&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "adGUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&clid=915&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "adGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?clid=698&wprid=implicit_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru?clid=915&wprid=implicit_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=721&wprid=implicit_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru?clid=921&wprid=implicit_wprid&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=panasonic%20tumix&clid=698&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=panasonic%20tumix&clid=915&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?text=panasonic%20tumix&clid=698&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?text=panasonic%20tumix&clid=915&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=721&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?text=panasonic%20tumix&clid=921&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                                ignore_params=['rs'],
                            ),
                        }
                    ],
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=698&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=721&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/geo?text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=698&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/geo?text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=721&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/geo?text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/geo?text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=698&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=721&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=915&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=panasonic%20tumix&lr=0&wprid=implicit_wprid&clid=921&utm_medium=cpc&utm_referrer=wizards",
                                ignore_len=False,
                            ),
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=698&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=915&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=721&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=921&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=698&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=915&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=721&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=921&lr=0&wprid=implicit_wprid&utm_medium=cpc&utm_referrer=wizards",  # noqa
                                        ignore_len=False,
                                    ),
                                },
                            }
                        ]
                    },
                }
            },
        )

    def test_market_model_wprid(self):
        """https://st.yandex-team.ru/MARKETOUT-26310"""

        request = "place=parallel&text=panasonic+tumix&wprid=model_wprid&rearr-factors=market_parallel_wprids=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=502",
                        ignore_len=False,
                    ),
                    "adGUrl": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=914",
                        ignore_len=False,
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=704",
                        ignore_len=False,
                    ),
                    "adGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=920",
                        ignore_len=False,
                    ),
                    "adGMoreUrl": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&clid=914&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "offersUrl": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101/offers?grhow=shop&hid=30&hyperid=101&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=502",  # noqa
                        ignore_len=False,
                    ),
                    "offersUrlAdG": LikeUrl.of(
                        "//market.yandex.ru/product--panasonic-tumix-5000/101/offers?grhow=shop&hid=30&hyperid=101&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=914",  # noqa
                        ignore_len=False,
                    ),
                    "offersUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=704",
                        ignore_len=False,
                    ),
                    "offersUrlAdGTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=920",
                        ignore_len=False,
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru?lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=502",
                                ignore_len=False,
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru?lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=914",
                                ignore_len=False,
                            ),
                        }
                    ],
                    "sitelinks": [
                        {
                            "adGUrl": "//market.yandex.ru/product--panasonic-tumix-5000/101/spec?hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=914",  # noqa
                            "adGUrlTouch": "//m.market.yandex.ru/product--panasonic-tumix-5000/101/spec?hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=920",  # noqa
                            "text": "specs",
                            "url": "//market.yandex.ru/product--panasonic-tumix-5000/101/spec?hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=502",
                            "urlTouch": "//m.market.yandex.ru/product--panasonic-tumix-5000/101/spec?hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=704",  # noqa
                        },
                        {
                            "adGUrl": "//market.yandex.ru/product--panasonic-tumix-5000/101/offers?grhow=shop&hid=30&hyperid=101&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=914",  # noqa
                            "adGUrlTouch": "//m.market.yandex.ru/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=920",  # noqa
                            "hint": "2",
                            "text": "prices",
                            "url": "//market.yandex.ru/product--panasonic-tumix-5000/101/offers?grhow=shop&hid=30&hyperid=101&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=502",  # noqa
                            "urlTouch": "//m.market.yandex.ru/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=704",  # noqa
                        },
                        {
                            "adGUrl": "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=914",
                            "adGUrlTouch": "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=920",
                            "hint": "12",
                            "text": "opinions",
                            "url": "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=502",
                            "urlTouch": "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&lr=0&wprid=model_wprid&utm_medium=cpc&utm_referrer=wizards&clid=704",
                        },
                    ],
                },
            },
        )

    def test_market_offers_image_in_text_wizard(self):
        """Проверяем, что под флагом market_offers_wizard_show_image_in_text появляется картинка в текстовом сниппете офферного
            и смотрим, что меняем описание в колдунщике под этим же флагом
        https://st.yandex-team.ru/MARKETOUT-27932
        """
        # Проверяем, что картинка появляется, когда есть врезка, и, что новое описание используется
        for flag in ['device=desktop', 'device=tablet', 'market_offers_wizard_show_image_in_text=1']:
            request = 'place=parallel&rids=213&text=iphone5+i5+white+recovered&rearr-factors=' + flag
            response = self.report.request_bs_pb(request)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "textWhenPicture": {
                            "__hl": {
                                "text": "Товары из магазина shop 1 (на фото) и еще 5. Доставка из Москвы и других регионов. Выбор по параметрам.",
                                "raw": True,
                            }
                        },
                        "snippetPicture": "//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                        "showcase": {"items": NotEmptyList()},
                    }
                },
            )
        # Проверяем, что картинка появляется и текст меняется, когда врезка не прошла порог
        response = self.report.request_bs_pb(
            'place=parallel&rids=213&text=iphone5+i5+white+recovered&rearr-factors=market_offers_wizard_show_image_in_text=1;market_offers_incut_meta_threshold=100'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "textWhenPicture": {
                        "__hl": {
                            "text": "Товары из магазина shop 1 (на фото) и еще 5. Доставка из Москвы и других регионов. Выбор по параметрам.",
                            "raw": True,
                        }
                    },
                    "snippetPicture": "//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                }
            },
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        # Проверяем, что картинка появляется и текст меняется правильно, когда магазин только один
        response = self.report.request_bs_pb(
            "place=parallel&rids=213&text=coat&rearr-factors=market_offers_wizard_show_image_in_text=1;"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "textWhenPicture": {
                        "__hl": {
                            "text": "Товары из магазина shop 1 (на фото). Доставка из Москвы и других регионов. Выбор по параметрам.",
                            "raw": True,
                        }
                    },
                    "snippetPicture": "//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                }
            },
        )
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

    def test_market_implicit_model_text_in_url(self):
        """Проверяем, что в колдунщике неявной модели
        в ссылки, ведущие на карточку модели, добавляется параметр &text с запросом
        https://st.yandex-team.ru/MARKETOUT-27704
        """

        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix')
        text = 'text=panasonic%20tumix'
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?" + text),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru"),
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                        }
                    ],
                    "sitelinks": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?" + text
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/product--panasonic-tumix-5000/101?" + text
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text
                                    ),
                                },
                            }
                        ]
                    },
                }
            },
        )

    def test_market_model_wizard_text_in_url(self):
        """Проверяем, что в модельном колдунщике в ссылки, ведущие на карточки офферов и моделей, добавляется параметр &text с запросом
        https://st.yandex-team.ru/MARKETOUT-27704
        """

        request = "place=parallel&text=panasonic+tumix"
        text = 'text=panasonic%20tumix'

        # Проверка формата под конструктором
        response = self.report.request_bs_pb(request + "&rearr-factors=showcase_universal_model=1;")
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "url": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                    "adGMoreUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                    "url_for_category_name": LikeUrl.of("//market.yandex.ru/catalog--kitaiskie-mobily/10?" + text),
                    "offersUrl": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101/offers?" + text),
                    "offersUrlTouch": LikeUrl.of("//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                    "offersUrlAdG": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101/offers?" + text),
                    "offersUrlAdGTouch": LikeUrl.of("//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/product--panasonic-tumix-5000/101?" + text),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard/",
                                        "%26text%3Dpanasonic%2520tumix",
                                    ),
                                    "directOffercardUrl": Contains("//market.yandex.ru/offer/", text),
                                },
                                "thumb": {
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard/",
                                        "%26text%3Dpanasonic%2520tumix",
                                    ),
                                },
                            }
                        ],
                    },
                    "sitelinks": [
                        {
                            "text": "specs",
                            "adGUrl": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101/spec?" + text),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101/spec?" + text
                            ),
                        },
                        {
                            "text": "opinions",
                            "url": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?" + text),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?" + text
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?" + text
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?" + text
                            ),
                        },
                    ],
                }
            },
        )

    def test_market_offers_wizard_text_in_url(self):
        """Проверяем, что в офферном колдунщике в ссылки, ведущие на карточки офферов и моделей, добавляется параметр &text с запросом
        https://st.yandex-team.ru/MARKETOUT-27704
        """

        request = 'place=parallel&text=hoover'

        # С флагом market_offers_wizard_incut_url_type=OfferCard вместо переходов в магазин переход на КО
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wizard_incut_url_type=OfferCard;')
        text = 'text=hoover'
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?" + text),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                    "snippetUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                    "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                    "snippetAdGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                    "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                    "url_for_category_name": LikeUrl.of("//market.yandex.ru/search?" + text),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=545"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru?clid=913"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=708"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru?clid=919"),
                            "snippetUrl": LikeUrl.of("//market.yandex.ru?clid=545"),
                            "snippetAdGUrl": LikeUrl.of("//market.yandex.ru?clid=913"),
                            "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru?clid=708"),
                            "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru?clid=919"),
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                            "snippetUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "snippetAdGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                            "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                        },
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?" + text),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?" + text),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of("//market.yandex.ru/offer/LuEE8VOUlal6_JoFbM54ug?" + text),
                                    "adGUrl": LikeUrl.of("//market.yandex.ru/offer/LuEE8VOUlal6_JoFbM54ug?" + text),
                                    "urlTouch": LikeUrl.of("//market.yandex.ru/offer/LuEE8VOUlal6_JoFbM54ug?" + text),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//market.yandex.ru/offer/LuEE8VOUlal6_JoFbM54ug?" + text
                                    ),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard", "%26text%3Dhoover"
                                    ),
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard", "%26text%3Dhoover"
                                    ),
                                },
                                "thumb": {
                                    "url": LikeUrl.of("//market.yandex.ru/offer/LuEE8VOUlal6_JoFbM54ug?" + text),
                                    "adGUrl": LikeUrl.of("//market.yandex.ru/offer/LuEE8VOUlal6_JoFbM54ug?" + text),
                                    "urlTouch": LikeUrl.of("//market.yandex.ru/offer/LuEE8VOUlal6_JoFbM54ug?" + text),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//market.yandex.ru/offer/LuEE8VOUlal6_JoFbM54ug?" + text
                                    ),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard", "%26text%3Dhoover"
                                    ),
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard", "%26text%3Dhoover"
                                    ),
                                },
                            }
                        ]
                    },
                }
            },
        )

        # С флагом market_offers_wizard_model_landing=1 вместо переходов в магазин переход на КМ
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wizard_model_landing=1;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "urlForCounter": LikeUrl.of("//market.yandex.ru/product--hoover-1/901?" + text),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard", "%26text%3Dhoover"
                                    ),
                                },
                                "thumb": {
                                    "urlForCounter": LikeUrl.of("//market.yandex.ru/product--hoover-1/901?" + text),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard", "%26text%3Dhoover"
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )

    def test_shop_and_model_ratings_in_offers_wizard_incut(self):
        """Проверка добавления рейтинга модели и рейтинга магазина во врезке офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-27855
        """
        # Проверка добавления рейтинга модели под флагом market_offers_wizard_model_rating
        response = self.report.request_bs_pb(
            'place=parallel&text=hoover&rearr-factors=market_offers_wizard_model_rating=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "hoover 1", "raw": True}}},
                                "rating": {"value": "4.32"},
                            }
                        ]
                    }
                }
            },
        )

        # Проверка добавления рейтинга магазина под флагом market_offers_wizard_shop_rating
        response = self.report.request_bs_pb(
            'place=parallel&text=pelmen' '&rearr-factors=market_offers_wizard_shop_rating=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "pelmen-1", "raw": True}}},
                                "greenUrl": {"text": "SHOP-400"},
                                "shopRating": {"value": "4.5"},
                                "shopReviews": {
                                    "count": "789",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-400/400/reviews?clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/shop--shop-400/400/reviews?clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "adGUrl": LikeUrl.of(
                                        "//market.yandex.ru/shop--shop-400/400/reviews?clid=913&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/shop--shop-400/400/reviews?clid=919&lr=0&utm_medium=cpc&utm_referrer=wizards",
                                        ignore_len=False,
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )

    def test_wizards_meta_relevance_and_threshold_in_market_factors(self):
        """Проверка добавления значений мета-формул и порогов колдунщиков в market_factors для проброса в Dumper
        https://st.yandex-team.ru/MARKETOUT-29209
        https://st.yandex-team.ru/MARKETOUT-30916
        """
        response = self.report.request_bs(
            'place=parallel&text=ModelMetaFormulas&rearr-factors='
            'market_implicit_model_wizard_meta_threshold=0.5;'
            'market_model_wizard_meta_threshold=0.2;'
            'market_offers_incut_meta_threshold=0.4;'
            'market_offers_wiz_top_offers_meta_threshold=0.1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ImplicitModelWizardRelevance": Round(0.6),
                        "ImplicitModelWizardThreshold": Round(0.5),
                        "ModelWizardRelevance": Round(0.3),
                        "ModelWizardThreshold": Round(0.2),
                        "OffersIncutRelevance": Round(0.9),
                        "OffersIncutThreshold": Round(0.4),
                        "OffersIncutTopOffersThreshold": Round(0.1),
                    }
                ]
            },
        )

        # Проверяем значения формул и порогов при схлопывании
        response = self.report.request_bs(
            'place=parallel&text=ToysOffer&rearr-factors='
            'market_implicit_model_wizard_collapsing_meta_threshold=0.2;'
            'market_model_wizard_collapsing_meta_threshold=0.3;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ImplicitModelWizardCollapsedRelevance": Round(0.4),
                        "ImplicitModelWizardCollapsedThreshold": Round(0.2),
                        "ModelWizardCollapsedRelevance": Round(0.5),
                        "ModelWizardCollapsedThreshold": Round(0.3),
                    }
                ]
            },
        )

    @classmethod
    def prepare_tobacco_filtering(cls):
        cls.index.models += [
            Model(hyperid=1501, hid=TOBACCO_CATEG_ID, title='IQOS 3 Duos'),
            Model(hyperid=1502, hid=TOBACCO_CATEG_ID, title='IQOS 2 Plus'),
        ]
        cls.index.offers += [
            Offer(title='IQOS 3 Duos 1', hyperid=1501),
            Offer(title='IQOS 3 Duos 2', hyperid=1501),
            Offer(title='IQOS 2 Plus 1', hyperid=1502),
            Offer(title='IQOS 2 Plus 2', hyperid=1502),
        ]

    def test_tobacco_filtering(self):
        """Проверяем, что в колдунщиках не отображаются офферы и модели из категории Табак (hid 16440100)
        https://st.yandex-team.ru/MARKETOUT-28668
        """
        response = self.report.request_bs_pb('place=parallel&text=IQOS')
        self.assertFragmentNotIn(response, {"market_offers_wizard": {}})
        self.assertFragmentNotIn(response, {"market_model": {}})
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})

    @classmethod
    def prepare_books_filtering(cls):
        cls.index.books += [
            Book(title='Everything about drugs', author='J. Smith', hyperid=300501, ts=300501),
        ]

        cls.index.models += [
            Model(title='Coal', hyperid=300502, ts=300502),
        ]

        cls.index.offers += [
            Offer(title="Cox", hyperid=300501, ts=300503, is_book=True),
            Offer(title="Cox", hyperid=300502, ts=300504),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300501).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300502).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300503).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300504).respond(0.6)

    def test_books_filtering(self):
        """Проверка отключения поиска книжек в подзапросах параллельного.
        По идее есть теоретическая возможность поиска книжек (см. метод CalcSearchForBooks),
        но я не представляю себе её реализации в продакшне, поэтому в тестах не
        стала этот кейс подпирать.

        https://st.yandex-team.ru/MARKETOUT-30050
        https://st.yandex-team.ru/MARKETOUT-31280 (выкатка флага, обратный оставлен)
        https://st.yandex-team.ru/MARKETOUT-31281 (полностью убрали флаг)
        """

        collapsed_model = {"market_model": {"title": 'J. Smith "Everything about drugs"', "is_book": "1"}}
        collapsed_implicit_model = {
            "market_implicit_model": {
                "showcase": {
                    "items": [
                        {
                            "title": {
                                "text": {"__hl": {"text": 'J. Smith "Everything about drugs"', "raw": True}},
                            }
                        }
                    ]
                }
            }
        }

        # Книжка не должна находиться
        response = self.report.request_bs_pb('place=parallel&text=cox')
        self.assertFragmentNotIn(response, collapsed_model)
        self.assertFragmentNotIn(response, collapsed_implicit_model)

    def test_region_in_wizard_urls(self):
        """Проверяем, что регион пробрасывается на маркет всегда
        https://st.yandex-team.ru/MARKETOUT-31031
        https://st.yandex-team.ru/MARKETOUT-32765
        """

        # offers wizard
        request = "place=parallel&text=lenovo p780&rids=213&rearr-factors=market_offers_incut_show_always=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'greenUrl': [
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                            'snippetUrl': Contains('lr=213'),
                            'snippetUrlTouch': Contains('lr=213'),
                            'snippetAdGUrl': Contains('lr=213'),
                            'snippetAdGUrlTouch': Contains('lr=213'),
                        },
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                            'snippetUrl': Contains('lr=213'),
                            'snippetUrlTouch': Contains('lr=213'),
                            'snippetAdGUrl': Contains('lr=213'),
                            'snippetAdGUrlTouch': Contains('lr=213'),
                        },
                    ],
                    'url_for_category_name': Contains('lr=213'),
                    'button': [
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                        }
                    ],
                    'geo': {
                        'url': Contains('lr=213'),
                        'url-touch': Contains('lr=213'),
                        'adGUrl': Contains('lr=213'),
                        'adGUrlTouch': Contains('lr=213'),
                    },
                    'showcase': {
                        'items': [
                            {
                                'greenUrl': {
                                    'url': Contains('lr=213'),
                                    'urlTouch': Contains('lr=213'),
                                    'adGUrl': Contains('lr=213'),
                                    'adGUrlTouch': Contains('lr=213'),
                                },
                                'thumb': {'offercardUrl': Contains('lr%3D213')},
                                'title': {'offercardUrl': Contains('lr%3D213')},
                            }
                        ]
                    },
                    'url': Contains('lr=213'),
                    'urlTouch': Contains('lr=213'),
                    'adGUrl': Contains('lr=213'),
                    'adGUrlTouch': Contains('lr=213'),
                    'snippetUrl': Contains('lr=213'),
                    'snippetUrlTouch': Contains('lr=213'),
                    'snippetAdGUrl': Contains('lr=213'),
                    'snippetAdGUrlTouch': Contains('lr=213'),
                }
            },
        )

        # model wizard
        request = "place=parallel&text=lenovo p780&rids=213&rearr-factors=showcase_universal_model=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_model': {
                    'greenUrl': [
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                        }
                    ],
                    'url_for_category_name': Contains('lr=213'),
                    'button': [
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                        }
                    ],
                    'sitelinks': [
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                        }
                    ],
                    'showcase': {
                        'items': [
                            {
                                'greenUrl': {
                                    'url': Contains('lr=213'),
                                    'urlTouch': Contains('lr=213'),
                                    'adGUrl': Contains('lr=213'),
                                    'adGUrlTouch': Contains('lr=213'),
                                },
                                'thumb': {'offercardUrl': Contains('lr%3D213')},
                                'title': {'offercardUrl': Contains('lr%3D213')},
                            }
                        ]
                    },
                    'url': Contains('lr=213'),
                    'urlTouch': Contains('lr=213'),
                    'adGUrl': Contains('lr=213'),
                    'adGUrlTouch': Contains('lr=213'),
                    'searchUrl': Contains('lr=213'),
                    'searchUrlAdG': Contains('lr=213'),
                    'adGMoreUrl': Contains('lr=213'),
                }
            },
        )

        # implicit model wizard
        request = (
            "place=parallel&text=xiaomi+mi+8+в+москве&rids=54&rearr-factors=market_parallel_wizard=1&askreqwizard=1"
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': {
                    'greenUrl': [
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                        }
                    ],
                    'button': [
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                        }
                    ],
                    'sitelinks': [
                        {
                            'url': Contains('lr=213'),
                            'urlTouch': Contains('lr=213'),
                            'adGUrl': Contains('lr=213'),
                            'adGUrlTouch': Contains('lr=213'),
                        }
                    ],
                    'showcase': {
                        'items': [
                            {
                                'thumb': {
                                    'url': Contains('lr=213'),
                                    'urlTouch': Contains('lr=213'),
                                    'adGUrl': Contains('lr=213'),
                                    'adGUrlTouch': Contains('lr=213'),
                                },
                                'title': {
                                    'url': Contains('lr=213'),
                                    'urlTouch': Contains('lr=213'),
                                    'adGUrl': Contains('lr=213'),
                                    'adGUrlTouch': Contains('lr=213'),
                                },
                            }
                        ]
                    },
                    'url': Contains('lr=213'),
                    'urlTouch': Contains('lr=213'),
                    'adGUrl': Contains('lr=213'),
                    'adGUrlTouch': Contains('lr=213'),
                }
            },
        )

    def test_precision_rating_in_wizards(self):
        """Проверяем, что в колдунщиках используется рейтинг с точностью 0.01
        https://st.yandex-team.ru/MARKETOUT-31267
        https://st.yandex-team.ru/MARKETOUT-32452
        """
        response = self.report.request_bs_pb('place=parallel&text=panasonic+tumix')

        # Колдунщик неявной модели
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}}},
                                "rating": {"value": "4.46"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}}},
                                "rating": {"value": "2.73"},
                            },
                        ]
                    }
                }
            },
        )

        # Модельный колдунщик под конструктором
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&rearr-factors=showcase_universal_model=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "type": "market_constr",
                    "title": {"__hl": {"text": "Panasonic tumix 5000", "raw": True}},
                    "rating": "4.46",
                }
            },
        )

        # Модели в офферном колдунщике
        response = self.report.request_bs_pb(
            'place=parallel&text=hoover&rearr-factors=market_offers_wizard_model_content=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "incut_model_content": "1",
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "hoover 1", "raw": True}}},
                                "modelContent": {
                                    "title": "hoover model 1",
                                    "rating": "4.32",
                                },
                            }
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_offers_wizard_incut_domain(cls):
        cls.index.offers += [
            Offer(title='screwdriver 1', ts=100, url='http://kuvalda.ru?id=1'),
            Offer(title='screwdriver 2', ts=101, url='http://220-volt.ru/screwdrivers?id=1'),
            Offer(title='screwdriver 3', ts=102, url='http://ВсеИнструменты.рф/шуруповерты/1'),
            Offer(title='screwdriver 4', ts=103),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.6)

    def test_offers_wizard_incut_domain(self):
        """Проверяем домен магазина во врезке офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-31451
        https://st.yandex-team.ru/MARKETINCIDENTS-4988
        """
        response = self.report.request_bs_pb('place=parallel&text=screwdriver&trace_wizard=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "screwdriver 1", "raw": True}}},
                                "greenUrl": {"domain": "kuvalda.ru"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "screwdriver 2", "raw": True}}},
                                "greenUrl": {"domain": "220-volt.ru"},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "screwdriver 3", "raw": True}}},
                                "greenUrl": {"domain": "ВсеИнструменты.рф"},
                            },
                        ]
                    }
                }
            },
        )

    def test_hd_picture(self):
        """Проверяем что при отсутствии hd картинки размером 200x200 используется обычная картинка размером 100x100
        https://st.yandex-team.ru/MARKETOUT-34016
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=molotok&trace_wizard=1&rearr-factors=market_offers_incut_threshold=0'
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "molot-molotok-1", "raw": True}}},
                                "thumb": {
                                    "source": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_5VHT3b-HvdrDaXZb1YITpQ/100x100"
                                    ),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_5VHT3b-HvdrDaXZb1YITpQ/100x100"
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )
        self.assertIn('12 3 нет hd картинки', response.get_trace_wizard())

    def test_wizards_clid_for_suggest(self):
        """https://st.yandex-team.ru/MARKETOUT-33906"""
        response = self.report.request_bs_pb(
            "place=parallel&text=iphone5&rids=213&rearr-factors=market_parallel_wizards_for_suggest=1"
        )
        self.assertFragmentIn(response, {'market_offers_wizard': {'url': Contains('clid=940')}})
        self.assertFragmentIn(response, {'market_model': {'url': Contains('clid=940')}})
        response = self.report.request_bs_pb(
            "place=parallel&text=text=sony+xperia&rearr-factors=market_parallel_wizards_for_suggest=1"
        )
        self.assertFragmentIn(response, {'market_implicit_model': {'url': Contains('clid=940')}})

    def test_wizards_utm_tags_in_urls(self):
        """https://st.yandex-team.ru/MARKETOUT-33521"""
        response = self.report.request_bs_pb(
            "place=parallel&text=iphone5&rids=213&rearr-factors=showcase_universal_model=1;market_parallel_use_utm_tags_in_urls=1"
        )
        self.assertFragmentIn(
            response, {'market_offers_wizard': {'url': Contains('utm_medium=cpc&utm_referrer=wizards')}}
        )
        self.assertFragmentIn(response, {'market_model': {'url': Contains('utm_medium=cpc&utm_referrer=wizards')}})
        response = self.report.request_bs_pb(
            "place=parallel&text=text=sony+xperia&rearr-factors=market_parallel_use_utm_tags_in_urls=1"
        )
        self.assertFragmentIn(
            response, {'market_implicit_model': {'url': Contains('utm_medium=cpc&utm_referrer=wizards')}}
        )

    def test_nailed_docs_in_text_wizards_title_url(self):
        """Проверяем что под флагом market_nailed_docs_on_text_wizards=0 в ссылки тайтла текстовых колдунщиков
        не добавляется параметр &rs с прибитыми документами
        По умолчанию раскатан флаг market_nailed_docs_on_text_wizards=1.
        https://st.yandex-team.ru/MARKETOUT-34602
        """
        # Офферный колдунщик
        request = 'place=parallel&text=pelmen&rearr-factors=market_offers_wizard_nailed_offers_count=1;market_enable_offers_wiz_right_incut=1;'

        # Под флагом market_nailed_docs_on_text_wizards=0 параметр rs не добавляется
        response = self.report.request_bs_pb(request + 'market_nailed_docs_on_text_wizards=0')
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=pelmen&lr=0&clid=545&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=pelmen&lr=0&clid=708&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                }
            },
        )

        # Под флагом market_nailed_docs_on_text_wizards=1 или без флага параметр rs добавляется
        for flag in ('', 'market_nailed_docs_on_text_wizards=1'):
            response = self.report.request_bs_pb(request + flag)
            self.assertFragmentIn(
                response,
                {
                    'market_offers_wizard': {
                        "url": LikeUrl.of(
                            "//market.yandex.ru/search?text=pelmen&lr=0&clid=545&rs=eJwzUvKS4xIzsMxxTXT0zg7MyTGMCMnKNQgPyPd0lGBUYNBgiGAAAKmoCLU%2C&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=pelmen&lr=0&clid=708&rs=eJwzUvKS4xIzsMxxTXT0zg7MyTGMCMnKNQgPyPd0lGBUYNBgiGAAAKmoCLU%2C&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                    }
                },
            )

        # Колдунщик неявной модели
        request = 'place=parallel&text=panasonic+tumix&rearr-factors=market_enable_implicit_model_wiz_without_incut=1'

        # Под флагом market_nailed_docs_on_text_wizards=0 параметр rs не добавляется
        response = self.report.request_bs_pb(request + '&rearr-factors=market_nailed_docs_on_text_wizards=0')
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model_without_incut': {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=panasonic%20tumix&lr=0&clid=834&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=panasonic%20tumix&lr=0&clid=835&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                    ),
                }
            },
        )

        # Под флагом market_nailed_docs_on_text_wizards=1 или без флага параметр rs добавляется
        for flag in ('', '&rearr-factors=market_nailed_docs_on_text_wizards=1'):
            response = self.report.request_bs_pb(request + flag)
            self.assertFragmentIn(
                response,
                {
                    'market_implicit_model_without_incut': {
                        "url": LikeUrl.of(
                            "//market.yandex.ru/search?text=panasonic%20tumix&lr=0&clid=834&rs=eJwzEvBiFWI2NDCEUEYRDAAXRwKI&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                        "urlTouch": LikeUrl.of(
                            "//m.market.yandex.ru/search?text=panasonic%20tumix&lr=0&clid=835&rs=eJwzEvBiFWI2NDCEUEYRDAAXRwKI&utm_medium=cpc&utm_referrer=wizards",
                            ignore_len=False,
                        ),
                    }
                },
            )

    @classmethod
    def prepare_parallel_stop_list_filter(cls):
        cls.index.offers += [
            Offer(title='подарочный набор 1', price=1000, ts=123),
            Offer(title='подарочный набор 2', price=1003, ts=124),
            Offer(title='подарочный набор 3', price=1001, ts=125),
            Offer(title='подарочный набор с зажигалкой', price=175, forbidden_market_mask=7, ts=126),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 123).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 124).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 125).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 126).respond(0.9)

    def test_parallel_stop_list_filter(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-35127
        https://st.yandex-team.ru/MARKETOUT-38987
        Проверяем, что происходит фильтрация офферов на параллельном
        '''
        response = self.report.request_bs_pb('place=parallel&text=подарочный+набор')
        self.assertFragmentNotIn(
            response,
            {
                "text": {
                    "__hl": {"text": "подарочный набор с зажигалкой", "raw": True}
                }  # Оффер забанен, так как у него проставлен forbidden_market_mask=7
            },
        )

    def test_ul_in_touch_urls(self):
        """Проверяем, что  под флагом market_parallel_add_ul_in_touch_urls в ссылки на маркет для тача добавляется путь /ul/
        https://st.yandex-team.ru/MARKETOUT-36053
        """
        # Офферный
        response = self.report.request_bs_pb(
            'place=parallel&text=kiyanka&touch=1&rearr-factors=device=touch;offers_touch=1;'
            'market_offers_incut_threshold=0;market_offers_wiz_top_offers_threshold=0;'
            'market_parallel_add_ul_in_touch_urls=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=708"),
                    "categoryUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=708"),
                    "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=708"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=919"),
                    "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=919"),
                    "greenUrl": [
                        {
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul?lr=0&clid=708"),
                            "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul?lr=0&clid=708"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul?lr=0&clid=919"),
                            "snippetAdGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul?lr=0&clid=919"),
                        },
                        {
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=708"),
                            "snippetAdGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=919"
                            ),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=919"),
                            "snippetUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=708"),
                        },
                    ],
                    "button": [
                        {
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=708"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=kiyanka&lr=0&clid=919"),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {
                                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul/grades-shop.xml?lr=0&clid=708"),
                                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/grades-shop.xml?lr=0&clid=919"),
                                },
                                "thumb": {
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/search?cvredirect=0&lr=0&text=kiyanka&clid=919"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/offer/f7_BYaO4c78hGceI7ZPR9A?hid=130&lr=0&nid=130&text=kiyanka&clid=708"
                                    ),
                                    "urlForCounter": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/search?cvredirect=0&lr=0&text=kiyanka&clid=708"
                                    ),
                                },
                                "title": {
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/search?cvredirect=0&lr=0&text=kiyanka&clid=919"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/offer/f7_BYaO4c78hGceI7ZPR9A?hid=130&lr=0&nid=130&text=kiyanka&clid=708"
                                    ),
                                    "urlForCounter": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/search?cvredirect=0&lr=0&text=kiyanka&clid=708"
                                    ),
                                },
                            }
                        ]
                    },
                }
            },
        )

        # Неявная модель
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&touch=1&rearr-factors=device=touch;market_parallel_add_ul_in_touch_urls=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=panasonic%20tumix&clid=721&lr=0"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=panasonic%20tumix&clid=921&lr=0"),
                    "greenUrl": [
                        {
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul?clid=721&lr=0"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul?clid=921&lr=0"),
                        },
                        {
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?text=panasonic%20tumix&clid=721&lr=0"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?text=panasonic%20tumix&clid=921&lr=0"
                            ),
                        },
                    ],
                    "button": [
                        {
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?text=panasonic%20tumix&clid=721&lr=0"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?text=panasonic%20tumix&clid=921&lr=0"
                            ),
                        }
                    ],
                    "sitelinks": [
                        {
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=721"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?show-reviews=1&text=panasonic%20tumix&lr=0&clid=921"
                            ),
                        },
                        {
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=721"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/search?delivery-interval=1&text=panasonic%20tumix&lr=0&clid=921"
                            ),
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=721&lr=0"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=921&lr=0"
                                    ),
                                },
                                "title": {
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=721&lr=0"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?text=panasonic%20tumix&hid=30&nid=10&clid=921&lr=0"
                                    ),
                                },
                            }
                        ]
                    },
                }
            },
        )

        # Модельный
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&touch=1&rearr-factors=device=touch;showcase_universal_model=1;'
            'market_parallel_add_ul_in_touch_urls=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=704"
                    ),
                    "categoryUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/ul/catalog?hid=30&hyperid=101&modelid=101&nid=10&text=panasonic%20tumix&lr=0&clid=704"
                    ),
                    "articlesUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101/reviews?hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=704"
                    ),
                    "reviewsUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&lr=0&clid=704"
                    ),
                    "searchUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/search?text=panasonic%20tumix&lr=0&clid=704"),
                    "offersUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=704"
                    ),
                    "adGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=920"
                    ),
                    "offersUrlAdGTouch": LikeUrl.of(
                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=920"
                    ),
                    "reviewsAdGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&lr=0&clid=920"
                    ),
                    "greenUrl": [
                        {
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul?lr=0&clid=704"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul?lr=0&clid=920"),
                        }
                    ],
                    "sitelinks": [
                        {
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101/spec?hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=704"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101/spec?hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=920"
                            ),
                        },
                        {
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=704"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?grhow=shop&hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=920"
                            ),
                        },
                        {
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&lr=0&clid=704"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101/reviews?text=panasonic%20tumix&lr=0&clid=920"
                            ),
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {
                                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/ul/grades-shop.xml?lr=0&clid=704"),
                                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/ul/grades-shop.xml?lr=0&clid=920"),
                                },
                            }
                        ]
                    },
                    "button": [
                        {
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=704"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/ul/product--panasonic-tumix-5000/101?hid=30&nid=10&text=panasonic%20tumix&lr=0&clid=920"
                            ),
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_parallel_adult_filtering(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='adult',
                hids=[6290267],
                regional_restrictions=[
                    RegionalRestriction(),
                ],
            ),
            CategoryRestriction(
                name='ask_18',
                hids=[6290267],
                regional_restrictions=[
                    RegionalRestriction(),
                ],
            ),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=6290267, name='Что-то очень взрослое'),
        ]
        cls.index.models += [
            Model(hyperid=50, hid=6290267, title='Взрослая модель 1', ts=50),
            Model(hyperid=51, hid=6290267, title='Взрослая модель 2', ts=51),
        ]
        cls.index.offers += [
            Offer(hyperid=50, hid=6290267, title='Взрослый оффер 1', price=100),
            Offer(hyperid=51, hid=6290267, title='Взрослый оффер 2', price=100),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 50).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51).respond(0.95)

    def test_parallel_adult_filtering(self):
        """Фильтрация документов из взрослых категорий
        https://st.yandex-team.ru/MARKETOUT-35545
        """
        request = 'place=parallel&text=взрослый&bsformat=1&pp=18'

        response = self.report.request_bs(request)
        self.assertFragmentNotIn(response, {"market_model": [{"title": {"__hl": {"text": "Взрослая модель 2"}}}]})
        self.assertFragmentNotIn(response, {"market_implicit_model": [{"title": {"__hl": {"text": "Взрослый"}}}]})

    def test_presence_factors(self):
        """Проверяем факторы наличия вьютайпов
        MARKETOUT-33897
        """
        # market_ext_category
        response = self.report.request_bs_pb("place=parallel&text=food")
        self.assertFragmentIn(response, {"market_ext_category": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_ext_category": 1}})

        # market_model
        response = self.report.request_bs_pb(
            "place=parallel&text=panasonic+tumix&rearr-factors=showcase_universal_model=1;"
        )
        self.assertFragmentIn(response, {"market_model": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_model": 1}})

        # market_model_right_incut
        response = self.report.request_bs_pb(
            "place=parallel&text=lenovo p780&rearr-factors=market_enable_model_wizard_right_incut=1"
        )
        self.assertFragmentIn(response, {"market_model_right_incut": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_model_right_incut": 1}})

        # market_offers_wizard
        response = self.report.request_bs_pb("place=parallel&text=kuvalda")
        self.assertFragmentIn(response, {"market_offers_wizard": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_offers_wizard": 1}})

        # market_offers_wizard_right_incut
        response = self.report.request_bs_pb(
            "place=parallel&text=molotok&rearr-factors=market_enable_offers_wiz_right_incut=1;market_offers_incut_threshold=0.1"
        )
        self.assertFragmentIn(response, {"market_offers_wizard_right_incut": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_offers_right_incut": 1}})

        # market_offers_wizard_center_incut
        response = self.report.request_bs_pb(
            "place=parallel&text=molotok&rearr-factors=market_enable_offers_wiz_center_incut=1;market_offers_incut_threshold=0.1"
        )
        self.assertFragmentIn(response, {"market_offers_wizard_center_incut": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_offers_center_incut": 1}})

        # market_offers_adg_wizard
        response = self.report.request_bs_pb(
            "place=parallel&text=kiyanka&rids=213&rearr-factors=market_enable_offers_adg_wiz=1;market_offers_incut_threshold_disable=1"
        )
        self.assertFragmentIn(response, {"market_offers_adg_wizard": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_offers_adg": 1}})

        # market_implicit_model
        response = self.report.request_bs_pb("place=parallel&text=nikon+d5200")
        self.assertFragmentIn(response, {"market_implicit_model": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_implicit_model": 1}})

        # market_implicit_model_adg_wizard
        response = self.report.request_bs_pb(
            "place=parallel&text=ToysOffer&trace_wizard=1&rearr-factors=market_enable_implicit_model_adg_wiz=1"
        )
        self.assertFragmentIn(response, {"market_implicit_model_adg_wizard": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_implicit_model_adg": 1}})

        # market_implicit_model_center_incut
        response = self.report.request_bs_pb(
            "place=parallel&text=panasonic+tumix&rearr-factors=market_enable_implicit_model_wiz_center_incut=1"
        )
        self.assertFragmentIn(response, {"market_implicit_model_center_incut": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_implicit_model_center_incut": 1}})

        # market_implicit_model_without_incut
        response = self.report.request_bs_pb(
            "place=parallel&text=panasonic+tumix&rearr-factors=market_enable_implicit_model_wiz_without_incut=1"
        )
        self.assertFragmentIn(response, {"market_implicit_model_without_incut": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_implicit_model_without_incut": 1}})

        # market_model_offers_wizard
        response = self.report.request_bs_pb(
            "place=parallel&text=kiyanka&rearr-factors=market_enable_model_offers_wizard=1"
        )
        self.assertFragmentIn(response, {"market_model_offers_wizard": NotEmpty()})
        self.assertFragmentIn(response, {"Market.Factors": {"wizard_market_model_offers_wizard": 1}})

    def test_parallel_no_models_wizards_for_images(self):
        '''Не формируем модельные колдунщики для запросов от Картинок
        https://st.yandex-team.ru/MARKETOUT-37266
        '''

        request = 'place=parallel&text=nikon'

        response = self.report.request_bs(request)
        self.assertFragmentIn(response, {"market_model": []})
        self.assertFragmentIn(response, {"market_implicit_model": []})

        response = self.report.request_bs(
            request + "&rearr-factors=market_offers_wizard_for_images=1;market_disable_models_wizard_for_pictures=1"
        )
        self.assertFragmentNotIn(response, {"market_model": []})
        self.assertFragmentNotIn(response, {"market_implicit_model": []})

    def test_device_desktop_as_default(self):
        """Проверяем, что под флагом market_use_nothing_in_device=1 дефолтные значения факторов для колдунщиков меняются"""
        # Проверяем пороги для врезки
        request = 'place=parallel&text=molotok&trace_wizard=1&rearr-factors=market_offers_incut_threshold_disable=0;'
        response = self.report.request_bs_pb(request + "market_use_nothing_in_device=0;")
        self.assertIn(
            'OffersWizard.TopOffersMnValue.Base.Threshold: market_offers_incut_threshold=-100',
            response.get_trace_wizard(),
        )
        response = self.report.request_bs_pb(request + "market_use_nothing_in_device=1;")
        self.assertIn(
            'OffersWizard.TopOffersMnValue.Base.Threshold: market_offers_incut_threshold=-100',
            response.get_trace_wizard(),
        )

        # Проверяем поиск моделей
        request = 'place=parallel&text=smart+hero&trace_wizard=1'
        response = self.report.request_bs_pb(request + "&rearr-factors=market_use_nothing_in_device=0;")
        self.assertIn('31 1 Найдено 7 моделей', response.get_trace_wizard())
        response = self.report.request_bs_pb(request + "&rearr-factors=market_use_nothing_in_device=1;")
        self.assertIn('31 1 Найдено 7 моделей', response.get_trace_wizard())

        # Проверяем формирование неявной модели
        request = 'place=parallel&text=smart+hero&rearr-factors=market_implicit_model_wiz_top_models_threshold=4;market_implicit_model_wizard_meta_threshold=-100;'
        response = self.report.request_bs_pb(request + "market_use_nothing_in_device=0;")
        self.assertFragmentIn(response, {"market_implicit_model": {}})
        response = self.report.request_bs_pb(request + "market_use_nothing_in_device=1;")
        self.assertFragmentNotIn(response, {"market_implicit_model": {}})

        # Проверяем формирование офферного справа
        request = (
            'place=parallel&text=molotok&rearr-factors=market_relevance_formula_threshold_on_parallel_for_offers=0;'
        )
        response = self.report.request_bs_pb(request + 'market_use_nothing_in_device=0;')
        self.assertFragmentIn(response, {"market_offers_wizard_right_incut": {}})
        response = self.report.request_bs_pb(request + 'market_use_nothing_in_device=1;')
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})

    def test_offers_wizard_clid_for_images(self):
        """Проверяем что под флагом market_offers_wizard_for_images_enable_search_url=1 clid в url для офферного колдунщика равен 961"""
        request = "place=parallel&text=kiyanka&rearr-factors=market_offers_wizard_for_images=1;"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl(url_params={"clid": 545}),
                }
            },
        )
        response = self.report.request_bs_pb(request + "market_offers_wizard_for_images_enable_search_url=1")
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl(url_params={"clid": 961}),
                }
            },
        )

    def test_wizard_doc_url(self):
        """Проверяем что в колдунщиках в элементах врезки есть поле docUrl
        Для офферов оно ведёт в магазин
        Для моделей оно ведёт на маркет
        """
        request = (
            "place=parallel&text={}&rearr-factors="
            "market_enable_model_offers_wizard=1"
            ";market_enable_offers_wiz_right_incut=1"
            ";market_enable_offers_adg_wiz=1"
            ";market_enable_implicit_model_adg_wiz=1"
            ";market_enable_implicit_model_wiz_center_incut=1"
            ";market_enable_implicit_model_adg_wiz=1"
            ";market_enable_implicit_model_wiz_center_incut=1"
            ";market_enable_model_wizard_right_incut=1"
            ";market_model_wizard_meta_threshold=0"
        )

        # Проверяем что у всех колдунщиков с офферами для каждой врезки есть docUrl
        response = self.report.request_bs_pb(request.format("pelmen"))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("http://pelmennaya.ru/pelmens?id=1")}]}
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_center_incut": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("http://pelmennaya.ru/pelmens?id=1")}]}
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_right_incut": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("http://pelmennaya.ru/pelmens?id=1")}]}
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_adg_wizard": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("http://pelmennaya.ru/pelmens?id=1")}]}
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [{"docUrl": LikeUrl.of("http://pelmennaya.ru/pelmens?id=1"), "documentType": "offer"}]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(request.format("lenovo+p780"))
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("http://www.shop-1.ru/lenovo_p780_offer1")}]}
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_model_right_incut": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("http://www.shop-1.ru/lenovo_p780_offer1")}]}
                }
            },
        )

        # # Проверяем что у всех колдунщиков с моделями для каждой врезки есть docUrl
        response = self.report.request_bs_pb(request.format("panasonic+tumix"))
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "docUrl": LikeUrl.of("//market.yandex.ru/product--panasonic-tumix-5000/101"),
                                "documentType": "model",
                            }
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(request.format("smart+hero"))
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("//market.yandex.ru/product--smart-hero-h11000/711")}]}
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model_adg_wizard": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("//market.yandex.ru/product--smart-hero-h11000/711")}]}
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model_center_incut": {
                    "showcase": {"items": [{"docUrl": LikeUrl.of("//market.yandex.ru/product--smart-hero-h11000/711")}]}
                }
            },
        )

    @classmethod
    def prepare_cart(cls):
        cls.index.offers += [
            Offer(title='cart offer 1', price=12341234),
            Offer(title='cart offer 2', hyperid=172, price=12341234),
            Offer(title='cart offer 3', hyperid=173, price=12341234),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=71,
                title="cart sku 1",
                blue_offers=[
                    BlueOffer(title='cart offer 1-1', waremd5='cart_offer_1_100000000', price=100, feedid=3),
                    BlueOffer(title='cart offer 1-2', waremd5='cart_offer_1_200000000', price=200, feedid=3),
                ],
            ),
            MarketSku(
                sku=72,
                hyperid=172,
                title="cart sku 2",
                blue_offers=[
                    BlueOffer(title='cart offer 2-1', waremd5='cart_offer_2_100000000', price=100, feedid=3),
                    BlueOffer(title='cart offer 2-2', waremd5='cart_offer_2_200000000', price=200, feedid=3),
                ],
            ),
            MarketSku(
                sku=73,
                hyperid=173,
                title="cart sku 3",
                blue_offers=[
                    BlueOffer(title='cart offer 3-1', waremd5='cart_offer_3_100000000', price=100, feedid=3),
                    BlueOffer(title='cart offer 3-2', waremd5='cart_offer_3_200000000', price=200, feedid=3),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=171, title="not in cart model 1", new=True),
            Model(hyperid=172, title="cart model 2"),
            Model(hyperid=173, title="cart model 3"),
        ]

        cls.carter.on_request(yandexuid='1', useLightList=True).respond(
            [
                CartOffer('cart_offer_2_200000000', 200, msku_id=72, title='cart offer 2-2'),
                CartOffer('cart_offer_3_100000000', 100, msku_id=73, title='cart offer 3-1'),
            ]
        )

    def test_parallel_cart(self):
        """Check cart state in parallel
        https://st.yandex-team.ru/MARKETOUT-37929
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=cart&yandexuid=1&rearr-factors=market_cpa_offers_incut_count=6;'
            'market_offers_wizard_cpa_offers_incut=1;'
            'market_cpa_offers_incut_threshold=0;'
            'market_offers_incut_threshold=0;'
            'market_parallel_enable_carter_request=1;'
            'market_parallel_allow_new_models=1'
        )

        # cpa-incut
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'cpaItems': [
                            {'title': {"text": {"__hl": {"text": "cart offer 1-1"}}}, 'isInCart': Absent()},
                            {'title': {"text": {"__hl": {"text": "cart offer 2-1"}}}, 'isInCart': True},
                            {'title': {"text": {"__hl": {"text": "cart offer 3-1"}}}, 'isInCart': True},
                        ]
                    }
                }
            },
        )

        # implicit model
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': {
                    'showcase': {
                        'items': [
                            {'title': {"text": {"__hl": {"text": "not in cart model 1"}}}, 'isInCart': Absent()},
                            {'title': {"text": {"__hl": {"text": "cart model 2"}}}, 'isInCart': True},
                            {'title': {"text": {"__hl": {"text": "cart model 3"}}}, 'isInCart': True},
                        ]
                    }
                }
            },
        )

    def test_cpc_offers_incut_logs(self):
        """Проверяем логирование CPС офферной врезки
        https://st.yandex-team.ru/MARKETOUT-39058
        https://st.yandex-team.ru/MARKETOUT-38890
        """
        offers_access_log_content = [
            '"wiz_id":"market_offers_wizard"',
            '"offers":["09lEaAKkQll1XTjm0WPoIA","xMpCOKC5I4INzFCab3WEmQ","lpc2G9gcBPtOqJHWMQSlow"]',
        ]

        request = 'place=parallel&text=pelmen&&wprid=user_wprid&rearr-factors=market_parallel_feature_log_rate=1;'

        # Ведем на КО
        _ = self.report.request_bs_pb(request + 'market_offers_wizard_incut_url_type=OfferCard&reqid=1')

        self.access_log.expect(
            reqid=1, wizard_elements=Regex('.*'.join(re.escape(x) for x in offers_access_log_content))
        )

        self.show_log.expect(
            reqid=1,
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/09lEaAKkQll1XTjm0WPoIA'),
            ware_md5='09lEaAKkQll1XTjm0WPoIA',
            wprid='user_wprid',
            incut_type='offers',
        )
        self.show_log.expect(
            reqid=1,
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/xMpCOKC5I4INzFCab3WEmQ'),
            ware_md5='xMpCOKC5I4INzFCab3WEmQ',
            wprid='user_wprid',
            incut_type='offers',
        )
        self.show_log.expect(
            reqid=1,
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/lpc2G9gcBPtOqJHWMQSlow'),
            ware_md5='lpc2G9gcBPtOqJHWMQSlow',
            wprid='user_wprid',
            incut_type='offers',
        )

        self.feature_log.expect(req_id=1, document_type=1, ware_md5='09lEaAKkQll1XTjm0WPoIA', incut_type='offers')
        self.feature_log.expect(req_id=1, document_type=1, ware_md5='xMpCOKC5I4INzFCab3WEmQ', incut_type='offers')
        self.feature_log.expect(req_id=1, document_type=1, ware_md5='lpc2G9gcBPtOqJHWMQSlow', incut_type='offers')

        # Ведем на Поиск
        self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=NailedInSearch&reqid=2')

        self.access_log.expect(
            reqid=2, wizard_elements=Regex('.*'.join(re.escape(x) for x in offers_access_log_content))
        )

        self.show_log.expect(
            reqid=2,
            url_type=UrlType.NAILED_IN_SEARCH,
            url=LikeUrl.of('//market.yandex.ru/search?text=pelmen'),
            ware_md5='09lEaAKkQll1XTjm0WPoIA',
            wprid='user_wprid',
            incut_type='offers',
        )
        self.show_log.expect(
            reqid=2,
            url_type=UrlType.NAILED_IN_SEARCH,
            url=LikeUrl.of('//market.yandex.ru/search?text=pelmen'),
            ware_md5='xMpCOKC5I4INzFCab3WEmQ',
            wprid='user_wprid',
            incut_type='offers',
        )
        self.show_log.expect(
            reqid=2,
            url_type=UrlType.NAILED_IN_SEARCH,
            url=LikeUrl.of('//market.yandex.ru/search?text=pelmen'),
            ware_md5='lpc2G9gcBPtOqJHWMQSlow',
            wprid='user_wprid',
            incut_type='offers',
        )

        self.feature_log.expect(req_id=2, document_type=1, ware_md5='09lEaAKkQll1XTjm0WPoIA', incut_type='offers')
        self.feature_log.expect(req_id=2, document_type=1, ware_md5='xMpCOKC5I4INzFCab3WEmQ', incut_type='offers')
        self.feature_log.expect(req_id=2, document_type=1, ware_md5='lpc2G9gcBPtOqJHWMQSlow', incut_type='offers')

    def test_implicit_model_logs(self):
        """Проверяем логирование колдунщика неявной модели
        https://st.yandex-team.ru/MARKETOUT-38890
        """
        access_log_content = ['"wiz_id":"market_implicit_model"', '"models":["101","102"]']

        _ = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&wprid=user_wprid&reqid=5&rearr-factors=market_parallel_feature_log_rate=1'
        )

        self.access_log.expect(reqid=5, wizard_elements=Regex('.*'.join(re.escape(x) for x in access_log_content)))

        self.show_log.expect(
            reqid=5,
            url_type=UrlType.MODEL,
            url=LikeUrl.of(
                '//market.yandex.ru/product--panasonic-tumix-5000/101?hid=30&lr=0&nid=10&text=panasonic%20tumix'
            ),
            hyper_id=101,
            wprid='user_wprid',
            position=1,
            incut_type='models',
        )
        self.show_log.expect(
            reqid=5,
            url_type=UrlType.MODEL,
            url=LikeUrl.of(
                '//market.yandex.ru/product--panasonic-tumix-pt6000/102?hid=30&lr=0&nid=10&text=panasonic%20tumix'
            ),
            hyper_id=102,
            wprid='user_wprid',
            position=2,
            incut_type='models',
        )

        self.feature_log.expect(req_id=5, document_type=2, model_id=101, position=1, incut_type='models')
        self.feature_log.expect(req_id=5, document_type=2, model_id=102, position=2, incut_type='models')

    def test_market_top_carousel(self):
        '''Проверяем что при заданном cgi &market_top_carousel=1 в офферном колдунщике появляется поле isMarketTopCarousel
        https://st.yandex-team.ru/MARKETOUT-39571
        '''

        # запрос без &market_top_carousel=1
        response = self.report.request_bs("place=parallel&text=iphone+купить")
        self.assertFragmentNotIn(response, {"market_offers_wizard": [{"isMarketTopCarousel": NotEmpty()}]})

        # запрос с &market_top_carousel=1
        response = self.report.request_bs("place=parallel&text=iphone+купить&market_top_carousel=1")
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "isMarketTopCarousel": True,
                    }
                ]
            },
        )

    def test_pp_for_products(self):
        '''Проверяем что под флагом market_offers_wizard_for_products, выставляются pp для PRODUCTS
        https://st.yandex-team.ru/GOODS-100
        '''

        # desktop
        response = self.report.request_bs(
            "place=parallel&text=iphone&rearr-factors=market_offers_wizard_for_products=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_center_incut": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone5 i5 white recovered", "raw": True}},
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", "/pp=7405/"
                                        ),
                                    }
                                }
                            ]
                        }
                    }
                ]
            },
        )

        # touch
        response = self.report.request_bs(
            "place=parallel&text=iphone&rearr-factors=market_offers_wizard_for_products=1&touch=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_center_incut": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone5 i5 white recovered", "raw": True}},
                                        "offercardUrl": Contains(
                                            "//market-click2.yandex.ru/redir/dtype=offercard/", "/pp=7403/"
                                        ),
                                    }
                                }
                            ]
                        }
                    }
                ]
            },
        )

    def test_rs_in_offers_wizard_response(self):
        """Проверяем, параметр rs в офферном колуднщике
        https://st.yandex-team.ru/GOODS-362
        """

        def get_use_nailed_docs(rs):
            rs = urllib.unquote(rs)
            common_report_state = ReportState.parse(rs)
            return common_report_state.search_state.use_nailed_docs

        def parse_rs_offers(rs):
            result = []
            rs = urllib.unquote(rs)
            common_report_state = ReportState.parse(rs)
            for nailed_doc in common_report_state.search_state.nailed_docs:
                if nailed_doc.ware_id:
                    result.append(nailed_doc.ware_id)
            return result

        request = 'place=parallel&text=pelmen'

        response = self.report.request_bs_pb(request)

        rs = 'eJwzSvCS4xIzsMxxTXT0zg7MyTGMCMnKNQgPyPd0lGBUYNBgAMlX-BY4-3s7m3qaePpVuTknJhmHu-YGIuRzCpKN3C3Tk50CSvwLvTzCfQODc_LLIfIA5JoZCw,,'
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_center_incut": {
                    "rs": rs,
                }
            },
        )

        offers = parse_rs_offers(rs)
        self.assertEqual(offers, ['09lEaAKkQll1XTjm0WPoIA', 'xMpCOKC5I4INzFCab3WEmQ', 'lpc2G9gcBPtOqJHWMQSlow'])
        self.assertEqual(get_use_nailed_docs(rs), True)

    @classmethod
    def prepare_products_hid_filter(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=90536,
                children=[
                    HyperCategory(hid=90537, children=[HyperCategory(hid=90538)]),
                ],
            )
        ]

        cls.index.offers += [
            Offer(title='offer africa 1', hid=90538, waremd5='OfferHid1____________g'),
            Offer(title='offer africa 2', hid=90537, waremd5='OfferHid2____________g'),
            Offer(title='offer africa 3', hid=90536, waremd5='OfferHid3____________g'),
        ]

    def test_products_hid_filter(self):
        """Фильтруем определенные категории для products
        https://st.yandex-team.ru/MARKETOUT-42675
        """

        # Без флага market_offers_wizard_for_products=1 оффер с hid 90538 не фильтруется
        response = self.report.request_bs("place=parallel&text=africa")
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "offer africa 1", "raw": True}},
                                    }
                                }
                            ]
                        }
                    }
                ]
            },
        )

        # С флагом market_offers_wizard_for_products=1 оффер с hid 90538 фильтруется, так как является подкатегорией hid 90536
        response = self.report.request_bs(
            "place=parallel&text=africa&rearr-factors=market_offers_wizard_for_products=1"
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "offer africa 1", "raw": True}},
                                    }
                                }
                            ]
                        }
                    }
                ]
            },
        )

        # С флагом market_offers_wizard_for_products=1;ignore_restrictions_for_products_wizard=1 оффер с hid 90538  не фильтруется
        response = self.report.request_bs(
            "place=parallel&text=africa&rearr-factors=market_offers_wizard_for_products=1;ignore_restrictions_for_products_wizard=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "offer africa 1", "raw": True}},
                                    }
                                }
                            ]
                        }
                    }
                ]
            },
        )

    @classmethod
    def prepare_used_goods(cls):
        cls.index.offers += [
            Offer(title='usedgoods 1', is_used_good=True),
            Offer(title='usedgoods 2', is_used_good=True),
            Offer(title='usedgoods 3', is_used_good=True),
            Offer(title='usedgoods 4', is_used_good=True),
        ]

    def test_used_goods(self):
        response = self.report.request_bs_pb('place=parallel&text=usedgoods')
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    "usedGoods": True,
                }
            },
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=usedgoods&rearr-factors=goods_enable_used_goods_filtering=1;goods_filter_used_goods=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "usedGoods": True,
                }
            },
        )

    @classmethod
    def prepare_navigational_scenario(cls):
        cls.index.offers += [
            Offer(title='navigational 1', fesh=10),
            Offer(title='navigational 2', fesh=11),
            Offer(title='navigational 3', fesh=12),
            Offer(title='navigational 4', fesh=13),
        ]

    def test_navigational_scenario(self):
        response = self.report.request_bs('place=parallel&text=navigational')
        self.assertFragmentNotIn(
            response,
            {
                "navigationalScenario": {"shopName": "wildberries", "shopIds": "10,11"},
            },
        )

        response = self.report.request_bs(
            'place=parallel&text=navigational&rearr-factors=navigational_scenario_shop_name=wildberries&fesh=10,11'
        )
        self.assertFragmentIn(
            response,
            {
                "navigationalScenario": {"shopName": "wildberries", "shopIds": "10,11"},
            },
        )


if __name__ == '__main__':
    main()

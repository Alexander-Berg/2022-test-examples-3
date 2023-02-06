#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip
from datetime import datetime
from core.testcase import TestCase, main
from core.types import (
    AboShopRating,
    BlueOffer,
    Book,
    CategoryRestriction,
    ClickType,
    ClothesIndex,
    Const,
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    Disclaimer,
    Elasticity,
    GLParam,
    GLType,
    GLValue,
    GpsCoord,
    HybridAuctionParam,
    HyperCategory,
    HyperCategoryType,
    LogosInfo,
    MarketSku,
    MnPlace,
    Model,
    ModelConversionRow,
    ModelGroup,
    NavCategory,
    NavRecipe,
    NavRecipeFilter,
    NewShopRating,
    Offer,
    Opinion,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Picture,
    PictureSignature,
    Region,
    RegionalDelivery,
    RegionalModel,
    RegionalRestriction,
    ReportState,
    Shop,
    UrlType,
    VCluster,
    Vendor,
    ViewType,
)
from core.matcher import Absent, Contains, Greater, LikeUrl, NoKey, NotEmpty, Regex, Round
from core.crypta import CryptaFeature, CryptaName
from core.cpc import Cpc
from core.types.picture import thumbnails_config
from core.types.fashion_parameters import FashionCategory


from core.types.delivery import OutletWorkingTime

import hashlib
import base64
import json

from core.report import DefaultFlags


def get_model_json(response, model_id):
    return next(obj for obj in response['search']['results'] if obj['id'] == model_id)


def get_shops_from_offers(model_json):
    do = model_json['offers']['items'][0]['shop']['id']
    top_offers_json = model_json['offers']['topOffers']
    return [do] + [offer_json['shopId'] for offer_json in top_offers_json]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        cls.reqwizard.on_default_request().respond()

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=2, name="Санкт-Петербург"),
                    Region(rid=193, name='Воронеж', preposition='в', locative='Воронеже'),
                    Region(rid=56, name='Челябинск', preposition='в', locative='Челябинске'),
                    Region(rid=35, name='Краснодар', preposition='в', locative='Краснодаре'),
                ],
            )
        ]
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                regions=[225],
                name='Московская пепячечная "Доставляем"',
                new_shop_rating=NewShopRating(
                    new_rating=3.0,
                ),
            ),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[225],
                name='Московская пепячечная "Доставляем"',
                new_shop_rating=NewShopRating(
                    new_rating=3.7,
                    rec_and_nonrec_pub_count=123,
                ),
            ),
        ]
        # Numeration rules:
        # gltype = { 202 ... 220 }
        # hid = { 1 ... 21 }, 10101, 90829
        # hyperid = { 300 .. 310 }
        # rids = { 226 ... 229 }

        cls.index.regiontree += [
            Region(
                rid=226,
                region_type=Region.COUNTRY,
                children=[
                    Region(
                        rid=227,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[Region(rid=229, region_type=Region.CITY)],
                    ),
                    Region(rid=228, region_type=Region.CITY),
                    Region(rid=230, region_type=Region.CITY),
                ],
            )
        ]

        cls.index.offers += [Offer(title="testPaging{0}".format(oid), price=oid * 100) for oid in range(1, 11)]
        cls.index.models += [
            Model(hyperid=307, hid=1, title='iphone 0'),
            Model(hyperid=308, hid=1, title='iphone 42', new=True),
        ]

        cls.index.models += [
            Model(hyperid=1307, title='document with black screen and many words in title'),
            Model(hyperid=1308, title='document with black screen and many words in title'),
        ]

        cls.index.offers += [
            Offer(title='iphone again', hyperid=307, waremd5='n9XlAsFkD2JzjIqQjT6w9w'),
            Offer(title='sales', price=322, price_old=420),
            Offer(title='adult', adult=True),
            Offer(
                fesh=1,
                title='has_delivery',
                price=100,
                delivery_options=[
                    DeliveryOption(price=100, day_from=31, day_to=31, order_before=24)
                ],  # TODO: MARKETOUT-47769 вернуть как было поменять значения на 32
            ),
        ]

        cls.index.offers += [Offer(title='urlless offer', url=None)]

        # start of test_delivery_included() data
        cls.index.offers += [
            Offer(fesh=1, title='deliveryIncluded1', price=100, delivery_options=[DeliveryOption(price=100)]),
            Offer(fesh=1, title='deliveryIncluded2', price=98, delivery_options=[DeliveryOption(price=104)]),
            Offer(fesh=2, title='deliveryIncluded3', price=99, delivery_options=[DeliveryOption(price=102)]),
        ]
        # end of test_delivery_included() data

        cls.index.offers += [
            Offer(title='вантуз', descr='хороший вантуз', comment='да правда отличный вантуз'),
            Offer(title=u'Рыба, щуĠка и тюль при異體字вет эȰтàİо я >Çиç< qwe \U000D0061a'.encode('utf8')),
        ]

        cls.index.offers += [
            Offer(hid=5, title='withDiscount', price=4, price_old=10),
            Offer(hid=5, title='noDiscount', price=5),
        ]

        cls.index.offers += [
            Offer(
                title='молоко',
                shop_category_path='категория 1\\категория 2\\категория 3',
                shop_category_path_ids='1\\2\\3',
                original_sku='milk123',
            )
        ]

        # test_offer_filters_for_models
        RED, GREEN, BLUE = 1, 2, 3
        MINI, MIDI = 43, 44
        cls.index.gltypes += [
            GLType(
                param_id=202,
                hid=1,
                gltype=GLType.ENUM,
                values=list(range(40, 51)),
                unit_name="Length",
                cluster_filter=True,
            ),
            GLType(
                param_id=203,
                hid=1,
                gltype=GLType.ENUM,
                values=[RED, GREEN, BLUE],
                unit_name="Color",
                cluster_filter=True,
            ),
            GLType(param_id=204, hid=1, gltype=GLType.BOOL, unit_name="WithZipper", cluster_filter=True),
            GLType(
                param_id=205,
                hid=1,
                gltype=GLType.ENUM,
                values=list(range(10)),
                unit_name="Vendor",
                cluster_filter=False,
            ),
            GLType(
                param_id=206,
                hid=1,
                gltype=GLType.ENUM,
                subtype="size",
                unit_name="Size",
                cluster_filter=True,
                unit_param_id=207,
                values=[
                    GLValue(value_id=1, text='XS', unit_value_id=1),
                    GLValue(value_id=2, text='XS', unit_value_id=1),
                    GLValue(value_id=3, text='XS', unit_value_id=1),
                    GLValue(value_id=5, text='M', unit_value_id=1),
                    GLValue(value_id=6, text='M', unit_value_id=1),
                    GLValue(value_id=41, text='10XL', unit_value_id=1),
                    GLValue(value_id=42, text='10XL', unit_value_id=1),
                ],
            ),
            GLType(
                param_id=207, hid=1, gltype=GLType.ENUM, position=None, values=[GLValue(value_id=1, text='american')]
            ),
            # For checking that multiple numeric enums in a single category are displayed correctly.
            GLType(
                param_id=208,
                hid=1,
                gltype=GLType.ENUM,
                subtype="size",
                unit_name="Size #2",
                cluster_filter=True,
                unit_param_id=209,
                values=[
                    GLValue(value_id=53, text='32', unit_value_id=1),
                ],
            ),
            GLType(
                param_id=209, hid=1, gltype=GLType.ENUM, position=None, values=[GLValue(value_id=1, text='russian')]
            ),
            GLType(param_id=212, hid=1, gltype=GLType.NUMERIC, cluster_filter=True),
        ]

        cls.index.models += [
            Model(title="dress", hid=1, hyperid=309, ts=3009, glparams=[GLParam(param_id=205, value=1)])
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3009).respond(0.1)

        cls.index.shops += [
            Shop(fesh=500, priority_region=227),
            Shop(fesh=501, priority_region=230),
            Shop(fesh=504, priority_region=228),
            Shop(
                fesh=513,
                name='blue_shop_1',
                priority_region=213,
                supplier_type=Shop.FIRST_PARTY,
                datafeed_id=3,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                business_fesh=4,
            ),
        ]

        cls.index.offers += [
            Offer(
                title="red dress midi with zipper 10XL",
                glparams=[
                    GLParam(param_id=202, value=MIDI),
                    GLParam(param_id=203, value=RED),
                    GLParam(param_id=204, value=1),
                    GLParam(param_id=206, value=41),
                    GLParam(param_id=208, value=53),
                    GLParam(param_id=212, value=1),
                ],
                hyperid=309,
                hid=1,
                fesh=500,
            ),
            Offer(
                title="red dress midi 10XL",
                glparams=[
                    GLParam(param_id=202, value=MIDI),
                    GLParam(param_id=203, value=RED),
                    GLParam(param_id=206, value=42),
                ],
                hyperid=42,
                hid=1,
                fesh=500,
            ),
            Offer(
                title="blue dress mini XS",
                glparams=[
                    GLParam(param_id=202, value=MINI),
                    GLParam(param_id=203, value=BLUE),
                    GLParam(param_id=206, value=2),
                    GLParam(param_id=212, value=5),
                ],
                hyperid=309,
                hid=1,
                fesh=500,
            ),
            Offer(
                title="green dress M",
                hyperid=309,
                hid=1,
                fesh=501,
                glparams=[GLParam(param_id=203, value=GREEN), GLParam(param_id=206, value=6)],
            ),
            Offer(hyperid=309, hid=1, glparams=[GLParam(param_id=212, value=17.2)]),
            Offer(hyperid=309, hid=1, glparams=[GLParam(param_id=212, value=12345)]),
            Offer(hyperid=309, hid=1, glparams=[GLParam(param_id=212, value=100500.45)]),
            Offer(hyperid=309, hid=1, glparams=[GLParam(param_id=212, value=9876543)]),
            Offer(hyperid=309, hid=1, fesh=504),
        ]

        # test_offer_filter_alternate_syntax
        cls.index.gltypes += [
            GLType(param_id=210, hid=1, gltype=GLType.BOOL, unit_name="GenericBoolParam"),
            GLType(param_id=211, hid=1, gltype=GLType.NUMERIC, unit_name="GenericNumericParam"),
        ]

        cls.index.offers += [
            Offer(title="alter_syntax yes", glparams=[GLParam(param_id=210, value=1)], hid=1),
            Offer(title="alter_syntax no", glparams=[GLParam(param_id=210, value=0)], hid=1),
            Offer(title="alter_syntax half", glparams=[GLParam(param_id=211, value=0.5)], hid=1),
        ]

        # autobroker
        cls.index.shops += [Shop(fesh=502, priority_region=300), Shop(fesh=503, priority_region=400, regions=[300])]

        cls.index.delivery_buckets += [
            DeliveryBucket.default(bucket_id=502, fesh=502),
            DeliveryBucket.default(bucket_id=503, fesh=503),
        ]

        std_options = [RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=5040, fesh=513, carriers=[157], regional_options=std_options),
        ]

        pic_1 = Picture(
            picture_id='IyC4nHslqLtqZJLygVAHe1',
            width=200,
            height=200,
            thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
        )
        pic_2 = Picture(
            picture_id='IyC2nHslqLtqZJLygVAHe2',
            width=300,
            height=300,
            thumb_mask=thumbnails_config.get_mask_by_names(['300x300']),
        )
        pic_3 = Picture(
            picture_id='IyC2nHslqLtqZJLygVAHe3',
            width=600,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['600x600']),
        )
        cls.index.offers += [
            Offer(
                title='nokia 7',
                bid=90,
                fesh=502,
                hid=8,
                delivery_buckets=[502],
                randx=900,
                price=10000,
                picture=pic_1,
            ),
            Offer(
                title='nokia 6',
                bid=80,
                fesh=502,
                hid=8,
                delivery_buckets=[502],
                randx=800,
                price=10000,
                picture=pic_2,
            ),
            Offer(
                title='nokia 5',
                bid=70,
                fesh=502,
                hid=8,
                delivery_buckets=[502],
                randx=700,
                price=10000,
                picture=pic_3,
            ),
            Offer(
                title='nokia 4',
                bid=95,
                fesh=503,
                hid=8,
                delivery_buckets=[503],
                randx=600,
                price=10000,
                picture=pic_1,
            ),
            Offer(
                title='nokia 3',
                bid=85,
                fesh=503,
                hid=8,
                delivery_buckets=[503],
                randx=500,
                price=10000,
                picture=pic_2,
            ),
            Offer(
                title='nokia 2',
                bid=75,
                fesh=503,
                hid=8,
                delivery_buckets=[503],
                randx=400,
                price=10000,
                picture=pic_3,
            ),
        ]

        # unique filter
        cls.index.offers += [
            Offer(title='nexus 1', cluster_id=1, randx=2000),
            Offer(title='nexus 2', cluster_id=1, randx=0),
        ]

        # unique picture
        pic_a = Picture(
            picture_id='iyC4nHslqLtqZJLygVAHeA',
            width=200,
            height=200,
            thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
        )
        pic_b = Picture(
            picture_id='IyC2nHslqLtqZJLygVAHeA',
            width=300,
            height=300,
            thumb_mask=thumbnails_config.get_mask_by_names(['300x300']),
        )
        cls.index.offers += [
            Offer(title='samsung 1', hid=9, picture=pic_a, fesh=504, randx=500),
            Offer(title='samsung 2', hid=9, picture=pic_a, fesh=504, randx=400),
            Offer(title='samsung 3', hid=9, picture=pic_b, fesh=504, randx=300),
        ]

        # offers per shop limit
        cls.index.shops += [Shop(fesh=505, priority_region=213), Shop(fesh=506, priority_region=213)]

        cls.index.hypertree += [
            HyperCategory(
                hid=10101,
                children=[
                    HyperCategory(hid=1),
                ],
            ),
        ]

        cls.index.models += [Model(title="dress", hid=10101, hyperid=1309)]

        OFFERS_PER_FESH = 20
        for ts in range(1, OFFERS_PER_FESH + 1):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(ts / 100.0)
            cls.index.offers.append(
                Offer(
                    fesh=505,
                    title='sony-{}'.format(ts),
                    hid=10,
                    ts=ts,
                    picture=Picture(width=100, height=100, group_id=1234),
                    picture_flags=ts,
                )
            )

        for ts in range(OFFERS_PER_FESH, OFFERS_PER_FESH * 2 + 1):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(ts / 100.0)
            cls.index.offers.append(
                Offer(
                    fesh=506,
                    title='sony-{}'.format(ts),
                    hid=10,
                    ts=ts,
                    picture=Picture(width=100, height=100, group_id=1234),
                    picture_flags=ts,
                )
            )

        # filter by fesh
        cls.index.shops += [
            Shop(fesh=507, priority_region=213),
            Shop(fesh=508, priority_region=213),
            Shop(fesh=509, priority_region=213),
        ]

        cls.index.offers += [
            Offer(title='nike-1', fesh=507),
            Offer(title='nike-2', fesh=508),
            Offer(title='nike-3', fesh=509),
        ]

        # writing metafeatures to feature log
        cls.index.offers += [
            Offer(title='metafeature', waremd5='Adi64pGx5HJEQzdWNMz6Dg'),
        ]

        cls.crypta.on_request_profile(yandexuid=1).respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value=100),
                CryptaFeature(name=CryptaName.GENDER_FEMALE, value=500),
                CryptaFeature(name=CryptaName.AGE_0_17, value=20),
                CryptaFeature(name=CryptaName.AGE_18_24, value=350),
                CryptaFeature(name=CryptaName.REVENUE_LOW, value=40),
                CryptaFeature(name=CryptaName.REVENUE_MED, value=490),
            ]
        )

        cls.index.models += [
            Model(hyperid=13579, title='metafeature with ichwill'),
            Model(hyperid=13570, title='metafeature without ichwill'),
        ]

        # writing offer features to feature log
        cls.index.offers += [Offer(title='featuredOffer', hid=11, price=134)]

        # writing model/cluster/etc. features to feature log
        cls.index.models += [
            Model(title='featuredModel', hid=12, opinion=Opinion(rating=2)),
            Model(title='featuredBook', hid=90829),
            Model(title='featuredModification', hid=12, group_hyperid=12345),
        ]
        cls.index.model_groups += [ModelGroup(title='featured-group', hid=12, hyperid=12345)]
        cls.index.vclusters += [VCluster(title='featured-cluster', hid=14, vclusterid=1000000701)]

        # test_json_quote
        cls.index.offers += [
            Offer(title='"название" json_quote', descr='"описание" json_quote', comment='"комментарий" к json_quote'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=2, visual=True),
        ]

        cls.index.shops += [
            Shop(fesh=1110, cpa=Shop.CPA_REAL),
            Shop(fesh=1111, cpa=Shop.CPA_REAL),
            Shop(fesh=1112, cpa=Shop.CPA_REAL),
            Shop(fesh=1113, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=1114, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=1115, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(hyperid=3101, fesh=1110, cpa=Offer.CPA_REAL),
            Offer(hyperid=3101, fesh=1110, cpa=Offer.CPA_NO),
            Offer(hyperid=3101, fesh=1111, cpa=Offer.CPA_REAL),
            Offer(hyperid=3101, fesh=1112, cpa=Offer.CPA_REAL),
            Offer(hyperid=3102, fesh=1113, cpa=Offer.CPA_REAL, title='prepayenabled offer'),
            Offer(hyperid=3102, fesh=1114, cpa=Offer.CPA_REAL, title='prepayenabled false offer'),
            Offer(fesh=1115, vclusterid=1000000101),
            Offer(fesh=1115, vclusterid=1000000701),
            Offer(fesh=1115, vclusterid=1000000001),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=21000,
                hyperid=6983,
                blue_offers=[BlueOffer(feedid=3, business_id=4, waremd5='OfferWithBusinessId_1g')],
                delivery_buckets=[5040],
            ),
        ]

        cls.index.vclusters += [
            VCluster(
                hid=2,
                vclusterid=1000000101,
                title='prepayenabled vcluster',
                clothes_index=[ClothesIndex([179], [179], [179])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=10)])],
            )
        ]

        cls.index.model_groups += [ModelGroup(hyperid=599, title='Superman suit')]

        cls.index.models += [
            Model(hyperid=501, group_hyperid=599, title='Superman suit 1'),
            Model(hyperid=502, group_hyperid=599, title='Superman suit 2'),
            Model(hyperid=503, group_hyperid=599, title='Superman suit 3'),
            Model(hyperid=504, title='prepayenabled model'),
        ]

        # start of data for test_group_categories()
        # TODO: REMOVE IT
        cls.index.hypertree += [
            HyperCategory(
                hid=13333,
                output_type=HyperCategoryType.SIMPLE,
                children=[
                    HyperCategory(
                        hid=1333,
                        output_type=HyperCategoryType.SIMPLE,
                        children=[
                            HyperCategory(
                                hid=133,
                                output_type=HyperCategoryType.SIMPLE,
                                children=[HyperCategory(hid=13, output_type=HyperCategoryType.GURU, has_groups=True)],
                            )
                        ],
                    )
                ],
            )
        ]

        cls.index.model_groups += [
            # 400X models are groups. Technically, clicks may be attached to groups directly, even though they will
            # be ignored when calculating popularity if a group has modifications.
            ModelGroup(
                hid=13,
                hyperid=4000,
                title='Lenovo laptop ABC',
                group_name='laptop ABC',
                model_clicks=40,
                glparams=[GLParam(param_id=302, value=19)],
            ),
            ModelGroup(
                hid=13,
                hyperid=4001,
                title='Macbook laptop',
                model_clicks=45,
                glparams=[GLParam(param_id=302, value=21)],
            ),
            # This group has no modifications, hence its own clicks/offer counts will be used when calculating popularity.
            ModelGroup(hid=13, hyperid=4002, title='Acer laptop', model_clicks=50),
        ]

        cls.index.models += [
            # 500X models are modifications attached to groups. These clicks are used for ranking groups.
            Model(
                hid=13,
                hyperid=5001,
                ts=5001,
                group_hyperid=4000,
                title='Lenovo laptop ABC 13 inch',
                model_clicks=50,
                glparams=[
                    GLParam(param_id=300, value=13),
                    GLParam(param_id=301, value=100),
                    GLParam(param_id=302, value=19),
                ],
            ),
            Model(
                hid=13,
                hyperid=5002,
                ts=5002,
                group_hyperid=4000,
                title='Lenovo laptop ABC 15 inch',
                model_clicks=70,
                glparams=[
                    GLParam(param_id=300, value=15),
                    GLParam(param_id=301, value=200),
                    GLParam(param_id=302, value=19),
                ],
            ),
            Model(
                hid=13,
                hyperid=5003,
                ts=5003,
                group_hyperid=4001,
                title='Macbook laptop 11 inch',
                model_clicks=30,
                glparams=[
                    GLParam(param_id=300, value=11),
                    GLParam(param_id=301, value=300),
                    GLParam(param_id=302, value=21),
                ],
            ),
            Model(
                hid=13,
                hyperid=5004,
                ts=5004,
                group_hyperid=4001,
                title='Macbook laptop 13 inch',
                model_clicks=30,
                glparams=[GLParam(param_id=300, value=13), GLParam(param_id=301, value=400)],
            ),
            Model(
                hid=13,
                hyperid=5005,
                ts=5005,
                group_hyperid=4001,
                title='Macbook laptop 15 inch',
                model_clicks=20,
                glparams=[GLParam(param_id=300, value=15), GLParam(param_id=301, value=500)],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5001).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5002).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5003).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5004).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5005).respond(0.5)

        cls.index.gltypes += [
            GLType(param_id=300, cluster_filter=True, hid=13, gltype=GLType.ENUM, values=[11, 13, 15]),
            GLType(param_id=301, cluster_filter=True, hid=13, gltype=GLType.NUMERIC),
            GLType(param_id=302, hid=13, gltype=GLType.ENUM, values=[19, 21]),
            GLType(param_id=7893318, hid=13, gltype=GLType.ENUM, values=[1, 2, 3, 4, 5, 6], vendor=True),
        ]

        cls.index.regional_models += [
            # Offers attached to groups will be ignored (we set offer counts directly for simplicity) if a group
            # has modifications.
            RegionalModel(hyperid=4000, offers=40, rids=[213]),
            RegionalModel(hyperid=4001, offers=40, rids=[213]),
            RegionalModel(hyperid=4002, offers=40, rids=[213]),
            # Offer counts for modifications. Used for ranking groups, too.
            RegionalModel(hyperid=5001, offers=20, rids=[213]),
            RegionalModel(hyperid=5002, offers=20, rids=[213]),
            RegionalModel(hyperid=5003, offers=20, rids=[213]),
            RegionalModel(hyperid=5004, offers=30, rids=[213]),
            RegionalModel(hyperid=5005, offers=40, rids=[213]),
        ]
        # end of data for test_group_categories()

        cls.index.offers += [
            Offer(title='offerinfo_test', waremd5='2b0-iAnHLZST2Ekoq4xElr', hid=111, fesh=420),
        ]

        # paranoidal check for MARKETOUT-9439
        cls.index.offers += [Offer(title='wrong-pp')]

        pictures = [Picture(width=100, height=100) for _ in range(4)]

        cls.index.offers += [
            Offer(ts=111, title='galaxy picture test 13', fesh=1, picture=pictures[0], picture_flags=1, hid=3),
            Offer(ts=112, title='galaxy picture test 17', fesh=1, picture=pictures[0], picture_flags=1, hid=3),
            Offer(ts=113, title='galaxy picture test 19', fesh=1, picture=pictures[0], picture_flags=1, hid=3),
            Offer(ts=114, title='galaxy picture test 23', fesh=1, picture=pictures[1], picture_flags=2, hid=3),
            Offer(ts=115, title='xiaomi picture test 13', fesh=1, picture=pictures[0], picture_flags=1, hid=4),
            Offer(ts=116, title='xiaomi picture test 17', fesh=1, picture=pictures[1], picture_flags=1, hid=4),
            Offer(ts=117, title='xiaomi picture test 19', fesh=1, picture=pictures[2], picture_flags=1, hid=4),
            Offer(ts=118, title='xiaomi picture test 23', fesh=1, picture=pictures[3], picture_flags=2, hid=4),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 111).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 112).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 113).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 114).respond(0.2)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 115).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 116).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 117).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 118).respond(0.2)

        # test_docs_restrictions
        cls.index.shops += [
            Shop(fesh=7101),
            Shop(fesh=7201),
            Shop(fesh=7301, cpa=Shop.CPA_REAL),
        ]
        pics = [
            Picture(
                picture_id=base64.b64encode(hashlib.md5(str(i)).digest())[:22], width=100, height=100, group_id=1234
            )
            for i in range(20)
        ]
        cls.index.offers += [
            Offer(
                title="test_docs_restrictions_fesh7101",
                picture=pics[x],
                picture_flags="{0}|{0}".format(x),
                hid=21,
                fesh=7101,
                price=x + 1,
            )
            for x in range(20)
        ]
        cls.index.offers += [
            Offer(
                title="test_docs_restrictions_fesh7201",
                picture=pics[x],
                picture_flags="{0}|{0}".format(x),
                hid=21,
                fesh=7201,
                price=100 * (x + 1),
            )
            for x in range(20)
        ]

        # test_default_offers
        cls.index.hypertree += [
            HyperCategory(hid=17, output_type=HyperCategoryType.GURU),
        ]
        cls.index.cpa_categories += [
            CpaCategory(hid=17, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]
        cls.index.models += [
            Model(title="spoon", hid=17, hyperid=305),
            Model(title="pan", hid=17, hyperid=304),
            Model(title="teapot", hid=17, hyperid=303),
        ]
        cls.index.shops += [Shop(fesh=1001, priority_region=213, cpa=Shop.CPA_REAL)]
        cls.index.offers += [
            Offer(title='gold spoon', hyperid=305, fesh=1001, price=1000, cpa=Offer.CPA_REAL),
            Offer(title='silver spoon', hyperid=305, fesh=1001, price=1000, cpa=Offer.CPA_REAL, is_express=True),
            Offer(title='pan 5 l', hyperid=304, fesh=1001, price=100, cpa=Offer.CPA_REAL),
            Offer(title='pan 3 l', hyperid=304, hid=17, fesh=1001, price=101, cpa=Offer.CPA_NO),
            Offer(title='green teapot', hyperid=303, fesh=1001, price=50, cpa=Offer.CPA_REAL),
            Offer(title='white teapot', hyperid=303, hid=17, fesh=1001, price=51, cpa=Offer.CPA_NO),
        ]

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

        cls.index.offers += [Offer(title='vel offer {}'.format(i), hyperid=108601) for i in range(50)]
        cls.index.offers += [Offer(title='vel offer {}'.format(i), hyperid=108602) for i in range(50, 80)]
        cls.index.offers += [Offer(title='vel offer {}'.format(i), hyperid=108603) for i in range(80, 100)]

        cls.index.hypertree += [
            HyperCategory(hid=13337, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(title='vel 1', hid=13337, hyperid=108601, model_clicks=10),
            Model(title='vel 1', hid=13337, hyperid=108602, model_clicks=10),
            Model(title='vel 1', hid=13337, hyperid=108603, model_clicks=10),
        ]

        cls.index.model_conversion += [
            ModelConversionRow(13337, 108601, 0.3),
            ModelConversionRow(13337, 108602, 1),
            ModelConversionRow(13337, 108603, 1),
        ]

        for i in range(10):
            cls.index.offers.append(Offer(price=1000, title='rock offer {}'.format(i), ts=1086 + 9 - i, randx=10 - i))
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1086 + i).respond(0.1 * i)

        for i in range(10):
            cls.index.mskus += [
                MarketSku(
                    sku=123 + i,
                    title='bock offer',
                    hyperid=1234 + i,
                    randx=i,
                    blue_offers=[
                        BlueOffer(
                            price=10,
                            offerid='Shop777_sku12300',
                        )
                    ],
                ),
            ]

    def test_paging(self):
        """
        Проверяем, что не отдаются офферы со страниц после последней при включенном флаге track-last-page
        """

        # Для первой страницы и с флагом и без найдутся все запрошенные 6 офферов
        response = self.report.request_json('place=prime&text=rock&how=aprice&numdoc=6&page=1')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}] * 6})
        response = self.report.request_json('place=prime&text=rock&how=aprice&numdoc=6&page=1&track-last-page=1')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}] * 6})

        # Для второй (и последней) страницы найдется по 4 оффера
        response = self.report.request_json('place=prime&text=rock&how=aprice&numdoc=6&page=2')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}] * 4})
        response = self.report.request_json('place=prime&text=rock&how=aprice&numdoc=6&page=2&track-last-page=1')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}] * 4})

        # А вот на третьей страницы (на которую на самом деле не найдется офферов)
        # без флага продублируется вывод последней (2й), как и раньше. А с флагом - результатов не будет
        response = self.report.request_json('place=prime&text=rock&how=aprice&numdoc=6&page=3')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}] * 4})
        response = self.report.request_json('place=prime&text=rock&how=aprice&numdoc=6&page=3&track-last-page=1')
        self.assertFragmentIn(response, {"results": Absent()})

        # Тестируем ограничение на количество страниц
        response = self.report.request_json(
            'place=prime&text=rock&how=aprice&numdoc=2&page=1&rearr-factors=output_max_page_count=4'
        )
        self.assertFragmentIn(
            response, {"total": Greater(8)}
        )  # до последней страницы еще далеко - выводим честное число
        response = self.report.request_json(
            'place=prime&text=rock&how=aprice&numdoc=2&page=2&rearr-factors=output_max_page_count=4'
        )
        self.assertFragmentIn(response, {"total": 7})  # numdoc*max_page_count-1=2*4-1=7
        response = self.report.request_json(
            'place=prime&text=rock&how=aprice&debug=da&numdoc=2&page=10&rearr-factors=output_max_page_count=4'
        )
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}] * 1})  # check content from page=4
        # проверяем что ограничение участвует в создании пейджера
        self.assertFragmentIn(response, {"logicTrace": [Contains("TPager constructor params", "maxPages=4")]})
        self.assertFragmentNotIn(response, {"logicTrace": [Contains("TPager constructor params", "maxPages=50")]})

        # fesh disable param output_max_page_count
        response = self.report.request_json(
            'place=prime&text=test&how=aprice&numdoc=2&page=2&rearr-factors=output_max_page_count=3'
        )
        self.assertFragmentIn(response, {"total": 5})
        response = self.report.request_json(
            'place=prime&text=test&how=aprice&numdoc=2&page=2&rearr-factors=output_max_page_count=3&fesh=1'
        )
        self.assertFragmentIn(response, {"total": 8})

        # Тестируем ограничение на количество страниц (синий)
        response = self.report.request_json(
            'place=prime&text=bock&rgb=blue&how=aprice&numdoc=2&page=1&rearr-factors=output_max_page_count=4'
        )
        self.assertFragmentIn(response, {"total": Greater(8)})  # до последней страницы еще далеко
        response = self.report.request_json(
            'place=prime&text=bock&rgb=blue&how=aprice&numdoc=2&page=2&rearr-factors=output_max_page_count=4'
        )
        self.assertFragmentIn(response, {"total": 7})  # numdoc*max_page_count-1=2*3-1=5
        response = self.report.request_json(
            'place=prime&text=bock&rgb=blue&how=aprice&numdoc=2&page=10&rearr-factors=output_max_page_count=4'
        )
        self.assertFragmentIn(response, {"results": [{"entity": "product"}] * 1})  # check content from page=4

    def test_max_numdoc_rear_flag(self):
        response = self.report.request_json('place=prime&text=rock&how=aprice&numdoc=6')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}] * 6})

        limits = {
            "geo": 1005000,
            "prime": 3,
        }
        response = self.report.request_json(
            'place=prime&text=rock&how=aprice&numdoc=6&rearr-factors=output_max_numdoc={}'.format(json.dumps(limits))
        )
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}] * 3})

    def test_guru_popularity_conversion(self):
        response = self.report.request_json('place=prime&hid=13337')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 108601},
                    {"entity": "product", "id": 108602},
                    {"entity": "product", "id": 108603},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&hid=13337&rearr-factors=market_use_model_conversion_for_guru=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 108601},
                    {"entity": "product", "id": 108602},
                    {"entity": "product", "id": 108603},
                ]
            },
            preserve_order=True,
        )

    def test_paging_with_no_params(self):
        response = self.report.request_json('place=prime&text=testPaging&how=aprice')
        self.assertEqual(10, response.count({"entity": "offer"}, preserve_order=True))

    def test_paging_by_one_page(self):
        for page in range(1, 11):
            response = self.report.request_json('place=prime&text=testPaging&how=aprice&numdoc=1&page={0}'.format(page))
            self.assertEqual(1, response.count({"entity": "offer"}, preserve_order=True))
            self.assertFragmentIn(response, {"raw": "testPaging{0}".format(page)}, preserve_order=True)
            """Проверяется, что общее количество для показа = total"""
            self.assertFragmentIn(response, {"total": 10})
        self.access_log.expect(total_renderable='10').times(10)

    def test_paging_incomplete_page(self):
        response = self.report.request_json('place=prime&text=testPaging&how=aprice&numdoc=7&page=2')
        self.assertEqual(3, response.count({"entity": "offer"}, preserve_order=True))
        self.assertFragmentIn(response, {"raw": "testPaging8"}, preserve_order=True)
        self.assertFragmentIn(response, {"raw": "testPaging9"}, preserve_order=True)
        self.assertFragmentIn(response, {"raw": "testPaging10"}, preserve_order=True)
        """Проверяется, что общее количество для показа = total"""
        self.assertFragmentIn(response, {"total": 10})
        self.access_log.expect(total_renderable='10')

    def test_paging_skip(self):
        response = self.report.request_json('place=prime&text=testPaging&how=aprice&skip=8')
        self.assertEqual(2, response.count({"entity": "offer"}, preserve_order=True))
        self.assertFragmentIn(response, {"raw": "testPaging9"}, preserve_order=True)
        self.assertFragmentIn(response, {"raw": "testPaging10"}, preserve_order=True)

    def test_paging_out_of_bound_page(self):
        """
        MARKETOUT-16261
        large page number must return 403 now
        error code 3635 must be written to error log
        """
        # output_max_page_count=0 задаёт максимальную страницу 50 и включает ошибку в случае превышения лимита
        response = self.report.request_json(
            'place=prime&text=testPaging&how=aprice&page=100500&numdoc=1&rearr-factors=output_max_page_count=0',
            strict=False,
        )
        self.assertNotEqual(1, response.count({"entity": "offer"}))
        self.assertFragmentNotIn(response, {"raw": "testPaging10"})
        self.assertFragmentIn(
            response,
            {
                "error": {
                    "code": "TOO_LARGE_PAGE",
                }
            },
        )
        self.assertEqual(response.code, 403)
        self.error_log.expect(code=3635).once()
        self.error_log.expect(code=3043)

    def test_paging_with_all_params(self):
        response = self.report.request_json('place=prime&text=testPaging&how=aprice&page=3&numdoc=2&skip=3')
        self.assertEqual(2, response.count({"entity": "offer"}, preserve_order=True))
        self.assertFragmentIn(response, {"raw": "testPaging8"}, preserve_order=True)
        self.assertFragmentIn(response, {"raw": "testPaging9"}, preserve_order=True)

    def test_offer_filters_for_models(self):
        # City #229 is a subregion of federative subject #227, therefore all offers from shop #500
        # (which has #227 as its priority region) can be delivered to #229.

        # dress 44th size exists
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=202:44')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # dress 44th size exists in #227 as well
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=227&glfilter=202:44')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # dress 50th size does not exists
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=202:50')
        self.assertFragmentNotIn(response, {"entity": "product"}, preserve_order=True)

        # red dress 44th size exists
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=202:44&glfilter=203:1')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # red dress 43th size does not exists
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=202:43&glfilter=203:1')
        self.assertFragmentNotIn(response, {"entity": "product"}, preserve_order=True)

        # blue dress 43th size exists
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=202:43&glfilter=203:3')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # dress 44th size not exist in city 228
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=228&glfilter=202:44')
        self.assertFragmentNotIn(response, {"entity": "product"}, preserve_order=True)

        # but dress 44th size exist somewhere
        response = self.report.request_json('place=prime&text=dress&hid=1&glfilter=202:44')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # red dress with zipper exist (bool filter test)
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=204:1&glfilter=203:1')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # red dress without zipper does not exist (bool filter test)
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=204:0&glfilter=203:1')
        self.assertFragmentNotIn(response, {"entity": "product"}, preserve_order=True)

        # cluster and not cluster filter together ( 44th size dress with vendor '2' does not exist)
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=202:44&glfilter=205:2')
        self.assertFragmentNotIn(response, {"entity": "product"}, preserve_order=True)

        # cluster and not cluster filter together ( 44th size dress with vendor '1' exist)
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229&glfilter=202:44&glfilter=205:1')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # dress 43 or 44 or 45 size exist somewhere
        response = self.report.request_json('place=prime&text=dress&hid=1&glfilter=202:43,44,45')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # dress 43 or 44 or 45 of RED or GREEN OR BLUE exist somewhere
        response = self.report.request_json('place=prime&text=dress&hid=1&glfilter=202:43,44,45&glfilter=203:1,2,3')
        self.assertFragmentIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # dress 43 or 45 of GREEN OR BLUE not exist
        response = self.report.request_json('place=prime&text=dress&hid=1&glfilter=202:44,45&glfilter=203:2,3')
        self.assertFragmentNotIn(response, {"entity": "product", "id": 309}, preserve_order=True)

        # test numeric ranges for param 209 that do NOT contain the 309 model
        invalid_ranges = ["2,3", "0,0.9", "12345.01,100499.99", "10123123,", ",0.1"]

        for r in invalid_ranges:
            response = self.report.request_json('place=prime&text=dress&hid=1&glfilter=212:{}'.format(r))
            self.assertFragmentNotIn(response, {"entity": "product"}, preserve_order=True)

        # ...and now the ranges that DO contain the model
        valid_ranges = [
            "1,6",
            "16,17.3",
            "10000,",
            ",10",
        ]
        for r in valid_ranges:
            response = self.report.request_json('place=prime&text=dress&hid=1&glfilter=212:{}'.format(r))
            self.assertFragmentIn(response, {"entity": "product"}, preserve_order=True)

    def test_groupmodel_filtering(self):
        response = self.report.request_json(
            'place=prime&hid=13&glfilter=302:19' '&rearr-factors=market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(
            response, {"results": [{"entity": "product", "id": 4000}]}, preserve_order=False, allow_different_len=False
        )

        response = self.report.request_json('place=prime&text=laptop&hid=13&glfilter=302:19')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 4000},
                ]
            },
            allow_different_len=False,
        )

    def test_model_boosting_leaf(self):
        response = self.report.request_json(
            'place=prime&text=dress&hid=1&rids=229&debug=da&local-offers-first=0&rearr-factors=market_use_model_boosting=1'
        )
        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "DELIVERY_TYPE"},
                    {"name": "IS_MODEL"},
                    {"name": "CPM"},
                ]
            },
            preserve_order=True,
            allow_different_len=True,
        )

    def test_model_boosting_leaf_notext(self):
        response = self.report.request_json('place=prime&hid=1&rids=229&local-offers-first=0')
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {"entity": "offer"},
                    {"entity": "product"},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&hid=1&rids=229&rearr-factors=market_no_model_boosting=1&local-offers-first=0'
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {"entity": "offer"},
                    {"entity": "product"},
                ]
            },
            preserve_order=True,
        )

    def test_no_model_boosting_leaf_text(self):
        response = self.report.request_json(
            'place=prime&text=dress&hid=1&rids=229&debug=da&rearr-factors=market_no_model_boosting=1&local-offers-first=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer"},
                    {"entity": "product"},
                ]
            },
            preserve_order=True,
            allow_different_len=True,
        )

    def test_model_boosting_non_leaf(self):
        response = self.report.request_json('place=prime&text=dress&hid=10101&rids=229&debug=da&local-offers-first=0')
        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "DELIVERY_TYPE"},
                    {"name": "CPM"},
                ]
            },
            preserve_order=True,
            allow_different_len=True,
        )

    def test_offer_filter_alternate_syntax(self):
        # Test alternate syntax of filter values
        # boolean 'select' for 1 and 'exclude' for 0
        # numeric: '~' instead of ','

        # one order with 210 param set to true
        response = self.report.request_json('place=prime&text=alter_syntax&hid=1&glfilter=210:select')
        self.assertEqual(1, response.count({"entity": "offer"}, preserve_order=True))

        # one order with 210 param set to false and one order without 210 param
        response = self.report.request_json('place=prime&text=alter_syntax&hid=1&glfilter=210:exclude')
        self.assertEqual(2, response.count({"entity": "offer"}, preserve_order=True))

        # one order with 211 param set to 0.5
        response = self.report.request_json('place=prime&text=alter_syntax&hid=1&glfilter=211:0.4~0.6')
        self.assertEqual(1, response.count({"entity": "offer"}, preserve_order=True))

    def test_model_second_kind_params(self):
        response = self.report.request_json('place=prime&text=dress&hid=1&rids=229')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "filters": [
                            {"id": "202", "values": [{"id": "43"}, {"id": "44"}]},
                            {"id": "203", "values": [{"id": "1"}, {"id": "3"}]},
                            {"id": "204", "values": [{"value": "0", "found": 0}, {"value": "1", "found": 1}]},
                            {"id": "205"},
                            {"id": "206", "units": [{"values": [{"value": "XS"}, {"value": "10XL"}]}]},
                            {"id": "208", "units": [{"values": [{"value": "32"}]}]},
                        ]
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&text=dress&hid=1&rids=228')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "filters": [
                            # First-kind filter only.
                            {"id": "205"}
                        ]
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=dress&hid=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "filters": [
                            {"id": "202", "values": [{"id": "43"}, {"id": "44"}]},
                            # Note that id #2 (GREEN) should appear in the output when no region is
                            # specified.
                            {"id": "203", "values": [{"id": "1"}, {"id": "2"}, {"id": "3"}]},
                            {"id": "204", "values": [{"value": "0", "found": 0}, {"value": "1", "found": 1}]},
                            {"id": "205"},
                            # The M size should appear in the output when no region is specified.
                            {"id": "206", "units": [{"values": [{"value": "XS"}, {"value": "M"}, {"value": "10XL"}]}]},
                            {"id": "208", "units": [{"values": [{"value": "32"}]}]},
                        ]
                    }
                ]
            },
        )

    def test_is_new(self):
        response = self.report.request_json('place=prime&text=iphone+42')
        self.assertFragmentIn(response, {"id": 308, "entity": "product", "isNew": True}, preserve_order=True)
        response = self.report.request_json('place=prime&text=iphone+0')
        self.assertFragmentIn(response, {"id": 307, "entity": "product", "isNew": False}, preserve_order=True)

    def test_product_type(self):
        response = self.report.request_json('place=prime&text=iphone+0')
        self.assertFragmentIn(response, {"id": 307, "entity": "product", "type": "model"}, preserve_order=True)

    @classmethod
    def prepare_root_attributes(cls):
        """
        Создаем три оффера, модельку, кластер, групповую модельку с 2мя модификациями с тайтлом total_and_shops
        Создаем просто два оффера с тайтлом just_offers
        """
        cls.index.offers += [
            Offer(title='total_and_shops', fesh=1101),
            Offer(title='total_and_shops', fesh=1101),
            Offer(title='total_and_shops', fesh=1102),
        ]

        cls.index.model_groups += [ModelGroup(title='total_and_shops', hyperid=3344)]

        cls.index.models += [
            Model(title='total_and_shops', hyperid=3399),
            Model(title='total_and_shops', group_hyperid=3344),
            Model(title='total_and_shops', group_hyperid=3344),
        ]

        cls.index.vclusters += [VCluster(title='total_and_shops', vclusterid=1000000004)]

        cls.index.offers += [Offer(vclusterid=1000000004)]

        cls.index.offers += [Offer(title='justOffers') for _ in range(2)]

    def test_root_attributes(self):
        """
        Тестируем атрибуты в корне search-элемента
        При поиске по total_and_shops ожидаем получить счетчики:
         - total=5,
         - totalOffers=3,
         - totalModels=3 (моделька + кластер + групп-моделька)
        При поиске по justOffers ожидаем получить счетчики:
        - total=2
        - totalOffers=2
        - totalModels=0
        """
        response = self.report.request_json('place=prime&text=adult')
        self.assertFragmentIn(response, {"adult": True, "restrictionAge18": True}, preserve_order=True)

        response = self.report.request_json('place=prime&text=sales')
        self.assertFragmentIn(response, {"salesDetected": True}, preserve_order=True)

        response = self.report.request_json('place=prime&text=total_and_shops&rearr-factors=disable_panther_quorum=0')
        self.assertFragmentIn(
            response,
            {
                "total": 6,
                "totalOffers": 3,
                "totalModels": 3,
                "shops": 2,
                "salesDetected": False,
                "adult": False,
                "restrictionAge18": False,
            },
        )

        response = self.report.request_json('place=prime&text=justOffers')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "totalOffers": 2,
                "totalModels": 0,
            },
        )

    def test_discount_sort(self):
        response = self.report.request_json('place=prime&text=withDiscount')
        self.assertFragmentIn(response, {"total": 1, "salesDetected": True})
        self.assertFragmentIn(
            response, {"text": u"по размеру скидки", "options": [{"id": "discount_p"}]}, preserve_order=True
        )

    def test_discount_no_sort(self):
        response = self.report.request_json('place=prime&text=noDiscount&rearr-factors=enable_sorting_by_discount=0')
        self.assertFragmentIn(response, {"total": 1, "salesDetected": False})
        self.assertFragmentNotIn(
            response, {"text": u"по размеру скидки", "options": [{"id": "discount_p"}]}, preserve_order=True
        )

    def test_discount_sort_mixed(self):
        response = self.report.request_json('place=prime&hid=5')
        self.assertFragmentIn(response, {"total": 2, "salesDetected": True})
        self.assertFragmentIn(
            response, {"text": u"по размеру скидки", "options": [{"id": "discount_p"}]}, preserve_order=True
        )

    def test_raw_price(self):
        response = self.report.request_json('place=prime&text=has_delivery&deliveryincluded=1&rids=225')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "prices": {"currency": "RUR", "value": "100", "rawValue": "100"},
            },
            preserve_order=True,
        )
        response = self.report.request_json('place=prime&text=has_delivery&deliveryincluded=1&rids=213')
        self.assertFragmentIn(
            response,
            {"entity": "offer", "prices": {"currency": "RUR", "value": "200", "rawValue": "100"}},
            preserve_order=True,
        )

    def test_reqwizard_region(self):
        response = self.report.request_json('place=prime&text=has_delivery&rids=213&debug=1')
        self.assertFragmentIn(response, {"logicTrace": [Contains("wizard?lr=213")]})

    def test_delivery_included(self):
        response = self.report.request_json('place=prime&text=deliveryIncluded&rids=213&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "deliveryIncluded2"}, "prices": {"value": "98", "rawValue": "98"}},
                    {"titles": {"raw": "deliveryIncluded3"}, "prices": {"value": "99", "rawValue": "99"}},
                    {"titles": {"raw": "deliveryIncluded1"}, "prices": {"value": "100", "rawValue": "100"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=deliveryIncluded&rids=213&how=aprice&deliveryincluded=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "deliveryIncluded1"}, "prices": {"value": "200", "rawValue": "100"}},
                    {"titles": {"raw": "deliveryIncluded3"}, "prices": {"value": "201", "rawValue": "99"}},
                    {"titles": {"raw": "deliveryIncluded2"}, "prices": {"value": "202", "rawValue": "98"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=deliveryIncluded&rids=213&how=dprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "deliveryIncluded1"}, "prices": {"value": "100", "rawValue": "100"}},
                    {"titles": {"raw": "deliveryIncluded3"}, "prices": {"value": "99", "rawValue": "99"}},
                    {"titles": {"raw": "deliveryIncluded2"}, "prices": {"value": "98", "rawValue": "98"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=deliveryIncluded&rids=213&how=dprice&deliveryincluded=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "deliveryIncluded2"}, "prices": {"value": "202", "rawValue": "98"}},
                    {"titles": {"raw": "deliveryIncluded3"}, "prices": {"value": "201", "rawValue": "99"}},
                    {"titles": {"raw": "deliveryIncluded1"}, "prices": {"value": "200", "rawValue": "100"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=deliveryIncluded&rids=213&how=rorp')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "deliveryIncluded2"},
                        "prices": {"value": "98", "rawValue": "98"},
                        "shop": {"qualityRating": 3},
                    },
                    {
                        "titles": {"raw": "deliveryIncluded3"},
                        "prices": {"value": "99", "rawValue": "99"},
                        "shop": {"qualityRating": 4, "ratingToShow": 3.7, "overallGradesCount": 123},
                    },
                    {
                        "titles": {"raw": "deliveryIncluded1"},
                        "prices": {"value": "100", "rawValue": "100"},
                        "shop": {"qualityRating": 3},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=deliveryIncluded&rids=213&how=rorp&deliveryincluded=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "deliveryIncluded1"},
                        "prices": {"value": "200", "rawValue": "100"},
                        "shop": {"qualityRating": 3},
                    },
                    {
                        "titles": {"raw": "deliveryIncluded3"},
                        "prices": {"value": "201", "rawValue": "99"},
                        "shop": {"qualityRating": 4},
                    },
                    {
                        "titles": {"raw": "deliveryIncluded2"},
                        "prices": {"value": "202", "rawValue": "98"},
                        "shop": {"qualityRating": 3},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_description_and_comment(self):
        response = self.report.request_json('place=prime&text=вантуз')
        self.assertFragmentIn(
            response,
            {"description": u"хороший вантуз", "seller": {"comment": u"да правда отличный вантуз"}},
            preserve_order=True,
        )

    def check_nokia(self, query):

        response = self.report.request_json(query)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "nokia 7"}},
                    {"titles": {"raw": "nokia 6"}},
                    {"titles": {"raw": "nokia 5"}},
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "nokia 4"}},
                    {"titles": {"raw": "nokia 3"}},
                    {"titles": {"raw": "nokia 2"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log.expect(title='nokia 7', click_price=81, min_bid=13, position=1)
        self.show_log.expect(title='nokia 6', click_price=71, min_bid=13, position=2)
        self.show_log.expect(title='nokia 5', click_price=13, min_bid=13, position=3)
        self.show_log.expect(title='nokia 4', click_price=86, min_bid=13, position=4)
        self.show_log.expect(title='nokia 3', click_price=76, min_bid=13, position=5)
        self.show_log.expect(title='nokia 2', click_price=13, min_bid=13, position=6)

    def test_autobroker_text(self):
        self.check_nokia('place=prime&text=nokia&rids=300&show-urls=external')

    def test_autobroker_no_text(self):
        self.check_nokia('place=prime&rids=300&show-urls=external&hid=8')

    def test_unique_filter(self):
        response = self.report.request_json('place=prime&text=nexus')
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "nexus 1"}})

        self.assertFragmentNotIn(response, {"entity": "offer", "titles": {"raw": "nexus 2"}})

        response = self.report.request_json('place=prime&text=nexus&hideduplicate=0')
        self.assertFragmentIn(
            response,
            [{"entity": "offer", "titles": {"raw": "nexus 1"}}, {"entity": "offer", "titles": {"raw": "nexus 2"}}],
        )

    def test_response_on_error(self):
        common = 'place=prime&rearr-factors=market_metadoc_search=no'

        response = self.report.request_json(common, strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST"}})

        response = self.report.request_json(common + '&rgb=blue', strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST"}})

        response = self.report.request_json(common + '&rids=213', strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST"}})

        response = self.report.request_json(common + '&rids=213&rgb=blue', strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST"}})

        # The following five symbols (slash, at, ampersand, comma, apostrophe) don't form a syntactically valid request.
        # '%%' and ':)' don't form a valid request either.
        # The client receives the same error as if the query was empty (may be changed in the future).
        for q in ['/', '%40', '%26', '%2C', '%27', '%25%25', '%3A%29']:
            response = self.report.request_json(common + '&text={}'.format(q), strict=False)
            self.assertEqual(response.code, 400)
            self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST"}})
        self.error_log.ignore('request syntax error')
        self.error_log.ignore('Request is empty')

        response = self.report.request_json(common + '&text=iphone')
        self.assertFragmentNotIn(response, {"error": {"code": NotEmpty()}})

        response = self.report.request_json(common + '&hid=1')
        self.assertFragmentNotIn(response, {"error": {"code": NotEmpty()}})

        # запрос без текста в корневую категорию.
        response = self.report.request_json(common + '&hid=90401', strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

        # One-letter queries (not counting punctuation) should result in a 400 error with code = TOO_SHORT_REQUEST.
        # ['x', '6', 'и', 'в', 'F', 'Ё', '0', '-1', 'v,', 'в:', 'U+1F602']
        for q in ['x', '6', '%D0%B8', '%D0%B2', 'F', '%D0%81', '0', '-1', 'v,', '%D0%B2%3A', '%F0%9F%98%82']:
            response = self.report.request_json(common + '&text={}'.format(q), strict=False)
            self.assertEqual(response.code, 400)
            self.assertFragmentIn(response, {"error": {"code": "TOO_SHORT_REQUEST"}})
            self.error_log.expect(code=3043)

        # Two-letter queries should result in success.
        # ['xz', 'аб', 'Dä', 'ЙЮ', '09', 'まだ', 'U+1F602'*2]
        for q in [
            'xz',
            '%D0%B0%D0%B1',
            'D%C3%A4',
            '%D0%99%D0%AE',
            '09',
            '%E3%81%BE%E3%81%A0',
            '%F0%9F%98%82%F0%9F%98%82',
        ]:
            response = self.report.request_json(common + '&text={}'.format(q))
            self.assertFragmentNotIn(response, {"error": {"code": NotEmpty()}})

    def test_unique_picture(self):
        # спрашиваем с текстом, ожидаем понижение позиции оффера с дублирующей картинкой (ушел из head в tail)
        response = self.report.request_json('place=prime&text=samsung')
        self.assertFragmentIn(
            response,
            [{"titles": {"raw": "samsung 1"}}, {"titles": {"raw": "samsung 3"}}, {"titles": {"raw": "samsung 2"}}],
            preserve_order=True,
        )

        # спрашиваем с реарром, ожидаем ослабление ограничения на 3 дубля
        response = self.report.request_json('place=prime&hid=9&rearr-factors=market_max_pics_duplicates_count=3')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "samsung 1"}},
                {"titles": {"raw": "samsung 2"}},
                {"titles": {"raw": "samsung 3"}},
            ],
            preserve_order=True,
        )

        # спрашиваем без текста, ожидаем понижение позиции оффера с дублирующей картинкой (ушел из head в tail)
        response = self.report.request_json('place=prime&hid=9')
        self.assertFragmentIn(
            response,
            [{"titles": {"raw": "samsung 1"}}, {"titles": {"raw": "samsung 3"}}, {"titles": {"raw": "samsung 2"}}],
            preserve_order=True,
        )

        # спрашиваем с флагом market_max_pics_duplicates_count_at_all и видим что оффер исчез
        response = self.report.request_json('place=prime&hid=9&rearr-factors=market_max_pics_duplicates_count_at_all=1')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "samsung 1"}},
                {"titles": {"raw": "samsung 3"}},
            ],
            preserve_order=True,
        )

    def test_offers_per_shop_limit(self):
        # спрашиваем с текстом, не ожидаем ограничения, получаем все sony-40 .. sony-1
        response = self.report.request_json('place=prime&text=sony&rids=213&numdoc=40')
        self.assertFragmentIn(
            response,
            [{"titles": {"raw": "sony-%d" % idx}} for idx in range(40, 0, -1)],
            preserve_order=True,
        )

        # спрашиваем без текста, не ожидаем ограничения, получаем все sony-40 .. sony-1
        response = self.report.request_json('place=prime&hid=10&rids=213&numdoc=40')
        self.assertFragmentIn(
            response,
            [{"titles": {"raw": "sony-%d" % idx}} for idx in range(40, 0, -1)],
            preserve_order=True,
        )

        # спрашиваем с текстом и реарром, ожидаем другие ограничения
        response = self.report.request_json(
            'place=prime&text=sony&rids=213&numdoc=20&rearr-factors=market_max_offers_per_shop_count=3'
        )

        limited_results = [
            # head
            {"titles": {"raw": "sony-40"}},
            {"titles": {"raw": "sony-39"}},
            {"titles": {"raw": "sony-38"}},
            {"titles": {"raw": "sony-20"}},
            {"titles": {"raw": "sony-19"}},
            {"titles": {"raw": "sony-18"}},
            # tail
            {"titles": {"raw": "sony-29"}},
            {"titles": {"raw": "sony-28"}},
            {"titles": {"raw": "sony-27"}},
            {"titles": {"raw": "sony-26"}},
            {"titles": {"raw": "sony-25"}},
            {"titles": {"raw": "sony-24"}},
        ]

        self.assertFragmentIn(
            response,
            limited_results,
            preserve_order=True,
        )

        # спрашиваем без текста, ожидаем применение ограничения (3 оффера с магаза)
        response = self.report.request_json(
            'place=prime&hid=10&rids=213&numdoc=20&rearr-factors=market_max_offers_per_shop_count=3'
        )
        self.assertFragmentIn(
            response,
            limited_results,
            preserve_order=True,
        )

    def test_fesh_filter(self):
        # спрашиваем без фильтра по магазу, ожидаем все магазы в фильтре и все офферы
        response = self.report.request_json('rids=213&place=prime&text=nike')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "nike-1"}},
                {"titles": {"raw": "nike-2"}},
                {"titles": {"raw": "nike-3"}},
            ],
        )

        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"found": 1, "value": "SHOP-507", "id": "507"},
                    {"found": 1, "value": "SHOP-508", "id": "508"},
                    {"found": 1, "value": "SHOP-509", "id": "509"},
                ],
            },
        )

        # спрашиваем с фильтром по магазам, ожидаем офферы только с выбранных магазов и все магазы в фильтре с отмеченным
        # под флагом market_use_fesh_literal=1 (выкачен по умолчанию) параметр fesh добавляется в литералы
        response = self.report.request_json(
            'rids=213&place=prime&text=nike&fesh=507&fesh=508&debug=da'
            '&rearr-factors=market_use_fesh_literal=1;market_join_gl_filters=0'
        )

        self.assertFragmentIn(
            response,
            {
                "report": {
                    "context": {
                        "collections": {
                            "SHOP": {
                                "text": [
                                    Contains('nike::', '(yx_ds_id:"507" | bsid:"507" | yx_ds_id:"508" | bsid:"508")')
                                ]
                            }
                        }
                    }
                }
            },
        )

        self.assertFragmentIn(
            response, [{"titles": {"raw": "nike-1"}}, {"titles": {"raw": "nike-2"}}], allow_different_len=False
        )
        # в фильтрах будут не только выбранные магазины т.к. фильтры получаются через отдельный допзапрос
        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"checked": True, "found": 1, "value": "SHOP-507", "id": "507"},
                    {"checked": True, "found": 1, "value": "SHOP-508", "id": "508"},
                    {"checked": NoKey("checked"), "found": 1, "value": "SHOP-509", "id": "509"},
                ],
            },
            allow_different_len=False,
        )

        # в фильтрах будут не только выбранные магазины, но некоторые офферы от выбранных магазинов в них учтутся дважды
        # т.к. фильтры получаются суммированием аггрегатов полученных через отдельный допзапрос с аггрегатами полученными по запросу с литералами
        # и некоторые офферы попадают и туда и туда
        response = self.report.request_json(
            'rids=213&place=prime&text=nike&fesh=507&fesh=508&debug=da'
            '&rearr-factors=market_use_fesh_literal=1;market_join_gl_filters=1'
        )

        self.assertFragmentIn(
            response,
            {
                "report": {
                    "context": {
                        "collections": {
                            "SHOP": {
                                "text": [
                                    Contains('nike::', '(yx_ds_id:"507" | bsid:"507" | yx_ds_id:"508" | bsid:"508")')
                                ]
                            }
                        }
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"checked": True, "found": 2, "value": "SHOP-507", "id": "507"},  # оффер один но учелся 2 раза
                    {"checked": True, "found": 2, "value": "SHOP-508", "id": "508"},  # оффер один но учелся 2 раза
                    {"checked": NoKey("checked"), "found": 1, "value": "SHOP-509", "id": "509"},
                ],
            },
            allow_different_len=False,
        )

        # Check that a fesh-only query (no &text=, &hid=, etc.) still gives expected results.
        response = self.report.request_json('rids=213&place=prime&fesh=507')
        self.assertFragmentIn(response, {"titles": {"raw": "nike-1"}})
        self.assertFragmentNotIn(response, {"titles": {"raw": "nike-2"}})

        # Check that a fesh-only query doesn't show offers that can't be delivered.
        response = self.report.request_json('rids=230&place=prime&fesh=507')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    @classmethod
    def prepare_supplier_id_filter(cls):
        cls.index.shops += [
            Shop(
                fesh=supplier_id,
                datafeed_id=supplier_id + 100,
                priority_region=213,
                fulfillment_program=True,
                cpa=Shop.CPA_REAL,
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.FIRST_PARTY,
            )
            for supplier_id in range(455101, 455104)
        ]
        cls.index.mskus += [
            MarketSku(
                hid=455000,
                sku=455001,
                hyperid=455001,
                blue_offers=[
                    BlueOffer(price=1000, feedid=455201),
                    BlueOffer(price=500, feedid=455202),
                ],
            ),
            MarketSku(
                hid=455000,
                sku=455002,
                hyperid=455002,
                blue_offers=[
                    BlueOffer(price=2000, feedid=455203),
                ],
            ),
        ]
        cls.index.models += [
            Model(hid=455000, hyperid=455001),
            Model(hid=455000, hyperid=455003),
        ]
        cls.index.offers += [
            Offer(hyperid=455001, price=300),
            Offer(hyperid=455003, price=200),
            Offer(hid=455000, title="single offer"),
        ]

    def test_supplier_id_filter(self):
        base = 'hid=455000&place=prime&allow-collapsing=1&use-default-offers=1'

        # Проверяем выдачу без фильтра, чтобы убедиться, что по запросу
        # возвращаются белые модели, синие модели и оффер
        response = self.report.request_json(base)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 455001,
                            "offers": {
                                "count": 3,
                                "items": [{"supplier": {"id": 455102}}],
                            },
                        },
                        {
                            "id": 455002,
                            "offers": {
                                "count": 1,
                                "items": [{"supplier": {"id": 455103}}],
                            },
                        },
                        {
                            "id": 455003,
                            "offers": {
                                "count": 1,
                                "items": [{"supplier": NoKey("supplier")}],
                            },
                        },
                        {"titles": {"raw": "single offer"}},
                    ]
                }
            },
            allow_different_len=False,
        )

        # Проверяем фильтрацию, когда supplier_id - поставщик ДО
        response = self.report.request_json(base + '&supplier-id=455102')
        self.assertFragmentIn(
            response,
            {
                "id": 455001,
                "offers": {
                    "count": 1,
                    "items": [{"supplier": {"id": 455102}}],
                },
            },
            allow_different_len=False,
        )

        # Проверяем, что фильтрация по supplier_id не ломается метадоковым поиском
        response = self.report.request_json(
            base + '&supplier-id=455102' + '&rearr-factors=market_metadoc_search=offers'
        )
        self.assertFragmentIn(
            response,
            {
                "id": 455001,
                "offers": {
                    "count": 1,
                    "items": [{"supplier": {"id": 455102}}],
                },
            },
            allow_different_len=False,
        )

        # Проверяем фильтрацию, когда supplier_id - НЕ поставщик ДО
        response = self.report.request_json(base + '&supplier-id=455101')
        self.assertFragmentIn(
            response,
            {
                "id": 455001,
                "offers": {
                    "count": 1,
                    "items": [{"supplier": {"id": 455101}}],
                },
            },
            allow_different_len=False,
        )

        # Проверяем фильтрацию по нескольким supplier-id
        response = self.report.request_json(base + '&supplier-id=455101&supplier-id=455103')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 455001,
                            "offers": {
                                "count": 1,
                                "items": [{"supplier": {"id": 455101}}],
                            },
                        },
                        {
                            "id": 455002,
                            "offers": {
                                "count": 1,
                                "items": [{"supplier": {"id": 455103}}],
                            },
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        # Проверяем фильтрацию с пустой выдачей
        response = self.report.request_json(base + '&supplier-id=455104')
        self.assertFragmentIn(response, {"search": {"total": 0, "results": []}}, allow_different_len=False)

    def test_model_features_logging(self):
        self.report.request_json('place=prime&text=featuredModel')
        self.feature_log.expect(categ_id=12, document_type=2)

    def test_cluster_features_logging(self):
        self.report.request_json('place=prime&text=featured-cluster')
        self.feature_log.expect(categ_id=14, document_type=3)

    def test_book_features_logging(self):
        """Под флагом market_use_books_pessimization=1 модели книжек отфильтровываются, если явно не указана книжная категория"""

        response = self.report.request_json(
            'place=prime&text=featuredBook&rearr-factors=market_use_books_pessimization=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

        response = self.report.request_json(
            'place=prime&text=featuredBook&hid=90829&rearr-factors=market_use_books_pessimization=1'
        )
        self.assertFragmentIn(response, {"search": {"total": 1}})
        self.feature_log.expect(categ_id=90829, document_type=4).times(1)

    def test_group_features_logging(self):
        self.report.request_json('place=prime&text=featured-group')
        self.feature_log.expect(categ_id=12, document_type=6)

    def test_offer_features_logging(self):
        self.report.request_json('place=prime&text=featuredOffer&show-urls=external')
        self.feature_log.expect(categ_id=11, offer_price=134, document_type=1)

    def test_offer_features_not_written_without_urls(self):
        self.report.request_json('place=prime&text=featuredOffer&pp=18', add_defaults=False)
        self.feature_log.expect(document_type=1).never()

    def test_json_quote(self):
        response = self.report.request_json('place=prime&text=json_quote')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": u'"название" json_quote'},
                "description": u'"описание" json_quote',
                "seller": {"comment": u'"комментарий" к json_quote'},
            },
            preserve_order=True,
        )

    def test_feed_info_in_offer(self):
        response = self.report.request_json('rids=213&place=prime&text=nike')
        self.assertFragmentIn(
            response, {"entity": "offer", "shop": {"feed": {"id": NotEmpty(), "offerId": NotEmpty()}}}
        )

    def test_isdeliveryincluded_not_specified(self):
        response = self.report.request_json('place=prime&fesh=507')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": False}})
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "offer", "titles": {"raw": "nike-1"}, "prices": {"isDeliveryIncluded": False}}]},
        )

    def test_offerinfo(self):
        # place=prime&offerid=2b0-iAnHLZST2Ekoq4xElr&cpc=some_cpc&pp=18&show-urls=offercard
        response = self.report.request_json(
            'place=prime'
            '&offerid=2b0-iAnHLZST2Ekoq4xElr'
            '&pp=18'
            '&show-urls=offercard'
            '&test-buckets=1,2,3'
            '&subreqid=test_subreqid'
        )

        cpc = str(
            Cpc.create_for_offer(
                click_price=1,
                click_price_before_bid_correction=1,
                offer_id='2b0-iAnHLZST2Ekoq4xElg',
                bid=10,
                hid=111,
                shop_id=420,
                minimal_bid=1,
                bid_type='mbid',
                pp=18,
            )
        )

        self.assertFragmentIn(response, {'search': {'results': [{'entity': 'offer', 'cpc': cpc}]}}, preserve_order=True)
        # Check that rs is right. The hardcoded value represents the following:
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJzjEBJiNdQx0jGWYFTiLUktLokvLk0qSi3MTAEAQp0G5g==")))' | protoc --decode_raw
        # 1: 18
        # 2: "1,2,3"
        # 3: 1
        # 4: "test_subreqid"
        self.show_log.expect(
            ware_md5='2b0-iAnHLZST2Ekoq4xElg',
            click_type_id=2,
            url=LikeUrl.of(
                '//market.yandex.ru/offer/2b0-iAnHLZST2Ekoq4xElg'
                '?cpc={}&rs=eJyzkuIQEmI11DHSMZZgVOItSS0uiS8uTSpKLcxMAQBLtQc6'.format(cpc)
            ),
            url_type=UrlType.OFFERCARD,
        ).times(1)
        self.click_log.expect(
            clicktype=ClickType.OFFERCARD,
            ware_md5='2b0-iAnHLZST2Ekoq4xElg',
            type_id=2,
            dtype='offercard',
            data_url=LikeUrl.of(
                '//market.yandex.ru/offer/2b0-iAnHLZST2Ekoq4xElg'
                '?cpc={}&rs=eJyzkuIQEmI11DHSMZZgVOItSS0uiS8uTSpKLcxMAQBLtQc6'.format(cpc),
                unquote=True,
            ),
            url_type=UrlType.OFFERCARD,
        ).times(1)

    @classmethod
    def prepare_test_modelinfo(cls):
        # Создаем модель с вендорными ставками и без вендорных ставок
        cls.index.models += [
            Model(title="model_with_vendor_bid", hyperid=3042, vbid=10, datasource_id=42),
            Model(title="model_without_vendor_bid", hyperid=3043),
        ]

    def test_modelinfo(self):
        # У модели с вендорными ставками cpc в выдаче присутствует
        response = self.report.request_json(
            'place=prime&text=model_with_vendor_bid&modelid=3042&rearr-factors=market_force_use_vendor_bid=1'
        )
        cpc = str(Cpc.create_for_model(model_id=3042, vendor_click_price=1, vendor_bid=10, pp=18))  # Автоброкер
        self.assertFragmentIn(
            response, {'search': {'results': [{'entity': 'product', 'cpc': cpc}]}}, preserve_order=True
        )

    def test_isdeliveryincluded_specified_and_true(self):
        response = self.report.request_json('place=prime&fesh=507&deliveryincluded=1')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": True}})
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "offer", "titles": {"raw": "nike-1"}, "prices": {"isDeliveryIncluded": True}}]},
        )

    def test_isdeliveryincluded_specified_and_false(self):
        response = self.report.request_json('place=prime&fesh=507&deliveryincluded=0')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": False}})
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "offer", "titles": {"raw": "nike-1"}, "prices": {"isDeliveryIncluded": False}}]},
        )

    def test_mobicard_passed_pp(self):
        self.report.request_json('place=prime&text=wrong-pp&touch=1&phone=1&pp=48&show-urls=phone')
        self.click_log.expect(clicktype=ClickType.PHONE, pp=48)

    def test_picture_duplicates(self):
        response = self.report.request_json('place=prime&text=galaxy picture test&hid=3')

        # Documents 17 and 19 are pessimized as duplicates by picture, even though their corresponding formula
        # values are strictly greater than that of document 23.
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "galaxy picture test 13"}},
                {"titles": {"raw": "galaxy picture test 23"}},
                {"titles": {"raw": "galaxy picture test 17"}},
                {"titles": {"raw": "galaxy picture test 19"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=xiaomi picture test&hid=4')

        # Even though all documents now have different pictures, document 17 and 19 are still duplicates of 13
        # because of picture uniqueness clusters (also known as picture_flags).
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "xiaomi picture test 13"}},
                {"titles": {"raw": "xiaomi picture test 23"}},
                {"titles": {"raw": "xiaomi picture test 17"}},
                {"titles": {"raw": "xiaomi picture test 19"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_missing_pp(self):
        response = self.report.request_json(
            'place=prime&text=dress&hid=1&rids=229&ip=127.0.0.1', strict=False, add_defaults=DefaultFlags.BS_FORMAT
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    def test_docs_restrictions(self):
        # with sort and text there are no restrictions on offer per shop count
        response = self.report.request_json(
            'place=prime&hid=21&text=test_docs_restrictions&how=aprice&rearr-factors=market_max_offers_per_shop_count=5'
        )
        self.assertGreaterEqual(response.count({"titles": {"raw": "test_docs_restrictions_fesh7101"}}), 5)
        self.assertFragmentNotIn(response, {"titles": {"raw": "test_docs_restrictions_fesh7201"}})

        # with sort only there are no restrictions on offer per shop count
        response = self.report.request_json(
            'place=prime&hid=21&how=aprice&rearr-factors=market_max_offers_per_shop_count=5'
        )
        self.assertGreaterEqual(response.count({"titles": {"raw": "test_docs_restrictions_fesh7101"}}), 5)
        self.assertFragmentNotIn(response, {"titles": {"raw": "test_docs_restrictions_fesh7201"}})

        # with text only there is restriction on offer per shop count
        response = self.report.request_json(
            'place=prime&hid=21&text=test_docs_restrictions&rearr-factors=market_max_offers_per_shop_count=5'
        )
        self.assertGreaterEqual(response.count({"titles": {"raw": "test_docs_restrictions_fesh7101"}}), 5)
        self.assertGreaterEqual(response.count({"titles": {"raw": "test_docs_restrictions_fesh7201"}}), 5)

        # with no text and no sorting there is restriction on offer per shop count
        response = self.report.request_json('place=prime&hid=21&rearr-factors=market_max_offers_per_shop_count=5')
        self.assertGreaterEqual(response.count({"titles": {"raw": "test_docs_restrictions_fesh7101"}}), 5)
        self.assertGreaterEqual(response.count({"titles": {"raw": "test_docs_restrictions_fesh7201"}}), 5)

    @classmethod
    def prepare_output_format(cls):
        cls.index.vendors += [
            Vendor(vendor_id=501, name='samsung', website='www.samsung.com'),
        ]

        cls.index.models += [
            Model(vendor_id=501, title='samsung galaxy s8', hyperid=601),
        ]

        cls.index.offers += [
            Offer(hyperid=601, vendor_id=501, title='samsung galaxy s8'),
        ]

    def test_output_format(self):
        """
        tests output format for place prime
        """
        response = self.report.request_json('place=prime&text=galaxy')
        self.assertFragmentIn(
            response,
            {'entity': 'offer', 'vendor': {'name': 'samsung', 'slug': 'samsung', 'website': 'www.samsung.com'}},
        )
        self.assertFragmentIn(
            response,
            {'entity': 'product', 'vendor': {'name': 'samsung', 'slug': 'samsung', 'website': 'www.samsung.com'}},
        )

    def test_empty_nid(self):
        """
        tests any search results with empty nid
        """
        response = self.report.request_json('place=prime&text=galaxy&nid=')
        self.assertFragmentIn(response, {'entity': 'product'})

    @classmethod
    def prepare_empty_website(cls):
        cls.index.vendors += [
            Vendor(vendor_id=502, name='lg'),
        ]

        cls.index.models += [
            Model(vendor_id=502, title='lg phone', hyperid=602),
        ]

        cls.index.offers += [
            Offer(hyperid=602, vendor_id=502, title='lg phone'),
        ]

    def test_empty_website(self):
        """
        tests that there is vendor no website for offers and models if website is not set
        """

        # test vendor without website
        response = self.report.request_json('place=prime&text=lg')
        self.assertFragmentIn(response, {'entity': 'offer', 'vendor': {'name': 'lg', 'website': NoKey('website')}})
        self.assertFragmentIn(response, {'entity': 'product', 'vendor': {'name': 'lg', 'website': NoKey('website')}})

    # MARKETOUT-10357
    # Проверяем, что при включенном флаге rearr-factors=market_remove_offers_with_hyper
    # в гуру категориях и листовых с кластерами удаляются офферы, которые имеют hyper_id,
    # с выключенной и включенной сортировкой
    @classmethod
    def prepare_exclude_offers_without_hyper(cls):
        # создаем необходимое дерево категорий
        cls.index.hypertree += [
            # Нелситовая гуру
            HyperCategory(
                hid=7001,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(),
                ],
            ),
            # Листовая гуру
            HyperCategory(hid=7002, output_type=HyperCategoryType.GURU),
            # Нелистовая с кластерами (проверим, что в ней офферы не удаляются)
            HyperCategory(
                hid=7003,
                output_type=HyperCategoryType.CLUSTERS,
                children=[
                    HyperCategory(),
                ],
            ),
            # Листовая с кластерами
            HyperCategory(hid=7004, output_type=HyperCategoryType.CLUSTERS),
            # Простая (проверим, что в ней офферы не удаляются)
            HyperCategory(hid=7005),
        ]

        # Добавим в каждую категорию модель и два оффера (один с привязкой к модели, а другой без модели)
        # Пусть в названиии всех офферов будет одно слово, которое потом используем для запроса
        for i in range(1, 6):
            cls.index.offers += [
                Offer(title='phone', hyperid=7000 + i, hid=7000 + i),
                Offer(title='cell phone', hid=7000 + i),
            ]
            cls.index.models += [Model(title='contact', hyperid=7000 + i, hid=7000 + i)]

    def test_exclude_offers_without_hyper(self):
        # Ожидаемый ответ репорта в случае удаления оффера с hyper_id
        excluded_result = {
            'total': 2,
            'results': [
                {"titles": {"raw": "contact"}},
                {"titles": {"raw": "cell phone"}},
            ],
        }
        # Ожидаемый ответ репорта в случае, когда оффер с hyper_id не был удален
        full_result = {
            'total': 3,
            'results': [
                {"titles": {"raw": "contact"}},
                {"titles": {"raw": "phone"}},
                {"titles": {"raw": "cell phone"}},
            ],
        }
        # Ожидаемый ответ репорта на текстовый запрос в случае удаления оффера с hyper_id
        excluded_text_result = {
            'total': 1,
            'results': [
                {"titles": {"raw": "cell phone"}},
            ],
        }
        # Ожидаемый ответ репорта на текстовый запрос в случае, когда оффер с hyper_id не был удален
        full_text_result = {
            'total': 2,
            'results': [
                {"titles": {"raw": "phone"}},
                {"titles": {"raw": "cell phone"}},
            ],
        }

        # Все запросы производятся с включенным флагом rearr-factors=market_remove_offers_with_hyper

        # Задаем запрос в нелистовую гуру категорию без сортировки с общим словом из названия офферов
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&text=phone&hid=7001&rearr-factors=market_remove_offers_with_hyper=1'
        )
        self.assertFragmentIn(response, excluded_text_result, allow_different_len=False)

        # Задаем запрос в нелистовую гуру категорию без сортировки и без текста
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&hid=7001&rearr-factors=market_remove_offers_with_hyper=1;market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(response, excluded_result, allow_different_len=False)

        # Задаем запрос в нелистовую гуру категорию с сортировкой по цене с общим словом из названия офферов
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&text=phone&hid=7001&how=aprice&rearr-factors=market_remove_offers_with_hyper=1'
        )
        self.assertFragmentIn(response, excluded_text_result, allow_different_len=False)

        # Задаем запрос в нелистовую гуру категорию с сортировкой по цене и без текста
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&hid=7001&how=aprice&rearr-factors=market_remove_offers_with_hyper=1;market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(response, excluded_result, allow_different_len=False)

        # Задаем запрос в листовую гуру категорию без сортировки с общим словом из названия офферов
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&text=phone&hid=7002&rearr-factors=market_remove_offers_with_hyper=1'
        )
        self.assertFragmentIn(response, excluded_text_result, allow_different_len=False)

        # Задаем запрос в листовую гуру категорию без сортировки и без текста
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&hid=7002&rearr-factors=market_remove_offers_with_hyper=1;market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(response, excluded_result, allow_different_len=False)

        # Задаем запрос в листовую гуру категорию с сортировкой по цене с общим словом из названия офферов
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&text=phone&hid=7002&how=arpice&rearr-factors=market_remove_offers_with_hyper=1'
        )
        self.assertFragmentIn(response, excluded_text_result, allow_different_len=False)

        # Задаем запрос в листовую гуру категорию с сортировкой по цене и без текста
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&hid=7002&how=arpice&rearr-factors=market_remove_offers_with_hyper=1;market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(response, excluded_result, allow_different_len=False)

        # Задаем запрос в нелистовую категорию с кластерами с общим словом из названия офферов
        # проверяем, что оффер с hyper_id остался
        response = self.report.request_json(
            'place=prime&text=phone&hid=7003&rearr-factors=market_remove_offers_with_hyper=1'
        )
        self.assertFragmentIn(response, full_text_result, allow_different_len=False)

        # Задаем запрос в нелистовую категорию с кластерами и без текста
        # проверяем, что оффер с hyper_id остался
        response = self.report.request_json(
            'place=prime&hid=7003&rearr-factors=market_remove_offers_with_hyper=1;market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(response, full_result, allow_different_len=False)

        # Задаем запрос в листовую категорию с кластерами без сортировки с общим словом из названия офферов
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&text=phone&hid=7004&rearr-factors=market_remove_offers_with_hyper=1'
        )
        self.assertFragmentIn(response, excluded_text_result, allow_different_len=False)

        # Задаем запрос в листовую категорию с кластерами без сортировки и без текста
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&hid=7004&rearr-factors=market_remove_offers_with_hyper=1;market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(response, excluded_result, allow_different_len=False)

        # Задаем запрос в листовую категорию с кластерами с сортировкой по цене с общим словом из названия офферов
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&text=phone&hid=7004&how=aprice&rearr-factors=market_remove_offers_with_hyper=1'
        )
        self.assertFragmentIn(response, excluded_text_result, allow_different_len=False)

        # Задаем запрос в листовую категорию с кластерами с сортировкой по цене и без текста
        # проверяем, что оффер с hyper_id был удален
        response = self.report.request_json(
            'place=prime&hid=7004&how=aprice&rearr-factors=market_remove_offers_with_hyper=1;market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(response, excluded_result, allow_different_len=False)

        # Задаем запрос в простую категорию с общим словом из названия офферов
        # проверяем, что оффер с hyper_id остался
        response = self.report.request_json(
            'place=prime&text=phone&hid=7005&rearr-factors=market_remove_offers_with_hyper=1'
        )
        self.assertFragmentIn(response, full_text_result, allow_different_len=False)

        # Задаем запрос в простую категорию и без текста
        # проверяем, что оффер с hyper_id остался
        response = self.report.request_json(
            'place=prime&hid=7005&rearr-factors=market_remove_offers_with_hyper=1;market_guru_collapsing_reverse=1'
        )
        self.assertFragmentIn(response, full_result, allow_different_len=False)

    def test_entities(self):
        '''
        1. Проверяем поле entities.
        2. Проверяем, что не искали в лишних коллекциях
        '''
        response = self.report.request_json('place=prime&text=dress&debug=da')
        self.assertFragmentIn(response, [{'entity': 'offer'}] * 4)
        self.assertFragmentIn(response, [{'entity': 'product'}] * 2)
        self.assertFragmentIn(response, {'TOTAL_DOCUMENTS_PROCESSED': 6})

        response = self.report.request_json('place=prime&text=dress&entities=offer&debug=da')
        self.assertFragmentIn(response, [{'entity': 'offer'}] * 4)
        self.assertFragmentNotIn(response, {'entity': 'product'})
        self.assertFragmentIn(response, {'TOTAL_DOCUMENTS_PROCESSED': 4})

        response = self.report.request_json('place=prime&text=dress&entities=product&debug=da')
        self.assertFragmentNotIn(response, {'entity': 'offer'})
        self.assertFragmentIn(response, [{'entity': 'product'}] * 2)
        self.assertFragmentIn(response, {'TOTAL_DOCUMENTS_PROCESSED': 2})

        response = self.report.request_json('place=prime&text=vcluster')
        self.assertFragmentIn(response, {'entity': 'product'})

        response = self.report.request_json('place=prime&text=vcluster&entities=product')
        self.assertFragmentIn(response, {'entity': 'product'})

        response = self.report.request_json('place=prime&text=vcluster&entities=offer')
        self.assertFragmentNotIn(response, {'entity': 'product'})

    # Проверяем, что entities=product корректно работает в плейсе prime в случае с cpa-товарами
    @classmethod
    def prepare_entities_product_works_for_cpa(cls):
        cls.index.hypertree += [
            HyperCategory(hid=7700, output_type=HyperCategoryType.GURU),
        ]
        cls.index.offers += [
            Offer(title='white_offer', hid=7700, hyperid=8802),
        ]
        cls.index.models += [
            Model(title='blue_model1', hid=7700, hyperid=8801),
            Model(title='white_model', hid=7700, hyperid=8802),
            Model(title='blue_model2', hid=7700, hyperid=8803),
        ]
        cls.index.mskus += [
            MarketSku(
                title='blue_msku_1',
                sku=88801,
                hyperid=8801,
                hid=7700,
                blue_offers=[BlueOffer(waremd5='ware_md5_blue_msku_000')],
            ),
            MarketSku(
                title='blue_msku_2', sku=88803, hid=7700, blue_offers=[BlueOffer(waremd5='ware_md5_blue_msku_2_0')]
            ),
        ]

    def test_entities_product_works_for_cpa(self):
        # Ожидаем получить в ответе модель ihpone model
        # samsung model не должна пролезть в ответ, поскольку оффер к ней не синий
        # xiaomi model не должна пролезть в ответ, поскольку офферов к ней нет
        expected_products = {
            'total': 1,
            'results': [
                {"entity": "product", "id": 8801, "titles": {"raw": "blue_model1"}},
            ],
        }

        expected_models = {
            'total': 3,
            'results': [
                {"entity": "product", "id": 8801, "titles": {"raw": "blue_model1"}},
                {"entity": "product", "id": 8802, "titles": {"raw": "white_model"}},
                {"entity": "product", "id": 8803, "titles": {"raw": "blue_model2"}},
            ],
        }

        # Запрос за entities=product должен найти синий оффер, увидеть enable-collapsing и догадаться, что в ответ попадёт нужная модель
        response_products = self.report.request_json(
            'place=prime&hid=7700&allow-collapsing=1&cpa=real&entities=product&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentNotIn(response_products, {'entity': 'offer'})
        self.assertFragmentIn(response_products, expected_products)

        # Без allow-collapsing ожидаем получить пустую выдачу: cpa=real ищет только по офферному шарду, а оффера показывать нельзя
        response_no_collapsing = self.report.request_json(
            'place=prime&hid=7700&cpa=real&entities=product&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response_no_collapsing, {'total': 0})

        # Без cpa=real ожидаем получить просто все модели из категории
        response_no_cpa = self.report.request_json(
            'place=prime&hid=7700&entities=product&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response_no_cpa, expected_models)

    @classmethod
    def prepare_shop_created_at(cls):
        '''
        Подготовка данных для теста даты добавления магазина.
        https://st.yandex-team.ru/MARKETOUT-10484
        '''
        cls.index.models += [Model(title="ball", hid=2221, hyperid=222309)]

        cls.index.shops += [
            Shop(fesh=222500, priority_region=227, created_at=datetime(2016, 12, 1)),
            Shop(fesh=222501, priority_region=227),
        ]

        cls.index.offers += [
            Offer(title="red ball with white line", hyperid=222309, hid=2221, fesh=222500),
            Offer(title="green ball with black cross", hyperid=222310, hid=2221, fesh=222501),
        ]

    def test_shop_created_at(self):
        '''
        Проверка поля даты добавления магазина.
        https://st.yandex-team.ru/MARKETOUT-10484
        Для магазина id=222500 было добавлено поле created_at, которое пробросилось на вывод как поле "createdAt" в изначальном формате ("YYYY-MM-DD" для нас это просто строка)
        Для магазина id=222501 этого поля нет и на выдаче created_at будет отсутствовать.
        '''
        response = self.report.request_json('place=prime&text=ball&hid=2221')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "shop": {
                            "id": 222500,
                            "createdAt": "2016-12-01",
                        },
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "shop": {
                            "id": 222501,
                            "createdAt": NoKey("createdAt"),
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_discount_doesnt_depend_on_delivery_cost(cls):
        """
        Создаем оффер со скидкой, который доставляется в 2 региона, в один из регионов достовка платная
        """
        cls.index.shops += [
            Shop(fesh=10, priority_region=213, regions=[225], name='shop 10'),
        ]

        cls.index.models += [
            Model(hyperid=3010, hid=400, title='model'),
        ]

        cls.index.offers += [
            Offer(
                fesh=10,
                hyperid=3010,
                title='has delivery and discount',
                price=1000,
                discount=50,
                delivery_options=[DeliveryOption(price=500, day_from=1, day_to=2, order_before=6)],
            ),
        ]

    def test_discount_doesnt_depend_on_delivery_cost(self):
        """
        Проверяем, что стоимость доставки не влияет на размер скидки оффера
        """
        response = self.report.request_json('place=prime&hyperid=3010&rids=213&deliveryincluded=1')
        self.assertFragmentIn(
            response,
            {
                "prices": {
                    "value": "1500",
                    "rawValue": "1000",
                    "discount": {
                        "oldMin": "2500",
                        "percent": 50,
                    },
                },
            },
        )

    # https://st.yandex-team.ru/MARKETOUT-10995
    @classmethod
    def prepare_show_decrypted_urls(cls):
        '''
        Подготовка данных для проверки явного включения незашифрованных ссылок
        '''

        # достаточно одного оффера, чтобы проверить его ссылки в выдаче
        cls.index.offers += [Offer(title='offerForShowUrls')]

    def test_show_decrypted_urls(self):
        '''
        Проверка явного включения незашифрованных ссылок
        '''

        # проверяем, что при запросе в place prime со включенным show-urls=encrypted
        # в выдаче нет незашифрованных ссылок
        response = self.report.request_json('place=prime&show-urls=encrypted&text=offerForShowUrls')
        self.assertFragmentNotIn(
            response,
            {
                "urls": {
                    "decrypted": NotEmpty(),
                }
            },
        )

        # проверяем, что при запросе в place prime со включенным show-urls=decrypted
        # в выдаче присутствуют незашифрованные ссылки
        response = self.report.request_json('place=prime&show-urls=decrypted&text=offerForShowUrls')
        self.assertFragmentIn(
            response,
            {
                "urls": {
                    "decrypted": NotEmpty(),
                }
            },
        )

    @classmethod
    def prepare_negative_shop_filters(cls):
        cls.index.offers += [Offer(fesh=510, title='iphone'), Offer(fesh=511, title='iphone')]

    def test_negative_shop_filter_with_text(self):
        """
        Создаем 2 оффера из разных магазинов с тайтлом iphone
        Проверяем, что 2 оффера находятся по тайтлу
        Проверяем, что в случае фильтрации по отрицательному магазину, оффер из данного магазина не показывается
        """
        response = self.report.request_json('place=prime&text=iphone')
        self.assertFragmentIn(response, {'entity': 'shop', 'id': 511})

        response = self.report.request_json('place=prime&text=iphone&fesh=-511')
        self.assertFragmentIn(response, {'entity': 'shop', 'id': 510})

        self.assertFragmentNotIn(response, {'entity': 'shop', 'id': 511})

    @classmethod
    def prepare_access_log(cls):
        """
        Создаем оффер с тайтлом access
        """
        cls.index.offers += [Offer(title='access')]

    def test_access_log(self):
        """
        Ищем его через плейс прайм, убеждаемся, что в аксес лог в поля total_processed, total_accepted и
        total rendered записалось 1
        """
        self.report.request_json('place=prime&text=access')
        self.access_log.expect(total_documents_processed=1, total_documents_accepted=1)

    @classmethod
    def prepare_is_cpa_prior(cls):
        """Создаются магазины со всеми возможными значениями is_cpa_prior"""
        cls.index.shops += [
            Shop(fesh=111, priority_region=213),
            Shop(fesh=222, priority_region=213, is_cpa_prior=False),
            Shop(fesh=333, priority_region=213, regions=[2], is_cpa_prior=True, cpa=Shop.CPA_REAL),
            # ФФ магазин
            Shop(
                fesh=444,
                priority_region=47,
                regions=[47, 43],
                is_cpa_prior=True,
                cpa=Shop.CPA_REAL,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            # CPA2.0 shop
            Shop(fesh=555, priority_region=213, regions=[47], is_cpa_prior=True, cpa=Shop.CPA_REAL, cpa20=True),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=80000,
                fesh=444,
                regional_options=[
                    RegionalDelivery(rid=47, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=43, options=[DeliveryOption(price=20, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=80001,
                fesh=555,
                regional_options=[
                    RegionalDelivery(rid=47, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(title='abcd', fesh=111),
            Offer(title='abcd', fesh=222),
            Offer(title='abcd', fesh=333, cpa=Offer.CPA_REAL),
            Offer(title='cpa2.0_offer', hyperid=555555, cpa=Offer.CPA_REAL, fesh=555, delivery_buckets=[80001]),
        ]

    def test_is_cpa_prior(self):
        """Проверяется, что is_cpa_prior выводится для prime"""
        response = self.report.request_json('place=prime&text=abcd&rids=213')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "shop": {"id": 111, "isCpaPrior": False}},
                {"entity": "offer", "shop": {"id": 222, "isCpaPrior": False}},
                {"entity": "offer", "shop": {"id": 333, "isCpaPrior": True}},
            ],
        )

    def test_is_cpa_prior_allow_cpc_pessimization(self):
        """Проверяется, что для cpa_prior=true магазина для неприоритетного региона выводится причина даунгрейда CPA оффера"""
        response = self.report.request_json(
            'place=prime&text=abcd&rids=2&fesh=333&debug=1&rearr-factors=market_filter_cpa_to_cpc_degradation=0'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "shop": {"id": 333},
                    "cpa": NoKey("cpa"),
                    "debug": {"properties": {"CPA_PESSIMIZATION_NON_PRIOR": "1"}},
                },
            ],
        )

    def test_is_cpa_prior_forbid_cpc_pessimization(self):
        """Проверяется, что для cpa_prior=true магазина для неприоритетного региона выводится причина скрытия CPA оффера"""
        response = self.report.request_json('place=prime&text=abcd&rids=2&fesh=333&debug=1')
        self.assertFragmentIn(response, {"filters": {"HIDE_CPA_PESSIMIZATION_NON_PRIOR": 1}})

    def test_nosearchresults(self):
        """Проверяем отсутствие результатов на выдаче при nosearchresults=1"""
        response = self.report.request_json('place=prime&text=iphone&nosearchresults=1&offer-shipping=delivery')
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentIn(response, {"search": {"total": Greater(0)}})
        # Проверяем, что фильтры не пропадают
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "delivery", "checked": True},
                        ],
                    },
                ]
            },
        )

    @classmethod
    def prepare_soft_distances(cls):
        cls.index.offers += [Offer(title='meizu mx6 32gb')]

        cls.reqwizard.on_default_request().respond()
        qtree = "cHiczZRPaBNBFMbfm92tw7RKSAnEkcK6F1exsJRAg2CVIFhUNFQECR5KsBIPAYkIMSBGajUoiKgUrT35B3uosRSKFqLt0YqHzdGL4E0o3j14cGZ2d1jTWA-CuJe8-ea9t7-8-WbZEdZHIQFpsIlLPEgCBwf2wBDsi3RwwYOD1qiVh9MwDiW8izCD8ARhEeEtgnjeI_h4lr9CdipsR0WZbGeVJ87XLnG0dVMr1hRGQTYtff94IWoaFHR09iCLuWMUE8CDfQdc9DB_nwRElb0s0NNShyyMkUazgM-LPaIC3P4iFb9k10XXKGGV1AgldQTxRj6N7EQHMSlXxQjQDnFJF9xn3_QMRPZGViM3LFiRi81OUM6EqCkhAizRAGscQ7Ar7GgHF2ZiJ4OKCn-h-mFEUJjZyERyjvgvyDHj9Hr6ySi68FxrEL3e2UqvJuqYJrbhWh44cgxErQS3PIzCukVpaqjpHeKfLFbsQGV-y1_yl_3X7UmOux10Nzn62enPL0kEHivs5oCvprJALCsc79NovNeR0WhXT9lszK6uFciLZjhtI7CDv6Cjlo7eqIhw0p6KIn9JREyMzvTf-YtKRV0jMyeVwYTWvq0MhpUpZBrSX9AgRmPlQ4HMSQ78S47liKN9ZxOOG905zMbjlbX_BGT134I4LOaervcQxD2MroEpbe1CYR5pvU5T0o78IbKTLLgQkd-NcjUT_2YYf_K5LOhm8GHlb7kbGvueGRp7B5Pqb4BB45KE6crVLdzJFHxyO0Weopi6tt4eGWidm8vaxuDMo8sHvDBHVIQ5Pan5w1_2D5y5-WDEhkFJJnP6VR8z2Uvp2BZKksbx_ESgEq1iqPapXJI06bYKBEtTJKml4PsJeI9YDQ,,"  # noqa
        cls.reqwizard.on_request("meizu mx4").respond(qtree=qtree)

    def test_soft_distances(self):
        """MARKETOUT-13691"""
        """Проверяем, что с флагом market_no_strict_distances=0 текстовым поиском не находятся документы, в которых есть только часть мультитокена"""
        response = self.report.request_json(
            'place=prime&text=meizu+mx4&rearr-factors=market_no_strict_distances=0;disable_panther_quorum=0'
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})

        response = self.report.request_json('place=prime&text=meizu+mx4')
        self.assertFragmentIn(response, {"entity": "offer"})

    def test_ip_written_to_log(self):
        """Проверка добавления полей ip и all_ip в market-feature.log
        https://st.yandex-team.ru/MARKETOUT-14044
        """

        # Один ip адрес
        self.report.request_json('place=prime&text=metafeature&ip=37.140.187.150')
        self.feature_log.expect(
            tskv_format='market-feature-log', other={"ip": "37.140.187.150", "all_ip": "37.140.187.150"}
        )
        # Несколько ip адресов
        self.report.request_json('place=prime&text=metafeature&ip=37.140.197.200,192.168.0.1,37.140.187.150')
        self.feature_log.expect(
            tskv_format='market-feature-log',
            other={"ip": "37.140.187.150", "all_ip": "37.140.197.200,192.168.0.1,37.140.187.150"},
        )

    @classmethod
    def prepare_offer_url_hash(cls):
        cls.index.offers += [
            Offer(title='url_hash_offer 1', offer_url_hash='12345123451'),
            Offer(title='url_hash_offer 2', offer_url_hash='12345123452'),
            Offer(title='url_hash_offer without hash'),
            Offer(title='superoffer', offer_url_hash='7333452744469835916'),
        ]

    def test_offer_url_hash(self):
        """Поисковый литерал и свойство offer_url_hash — хеш канонизированного урла
        https://st.yandex-team.ru/MARKETOUT-13796
        """

        # Задаем запрос и проверяем, что у офферов, у которых есть offer_url_hash в
        # индексе, он есть в выдаче
        response = self.report.request_json(
            'place=prime&text=url_hash_offer&debug=da&rearr-factors=disable_panther_quorum=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "url_hash_offer 1"}, "debug": {"offerUrlHash": "12345123451"}},
                {"entity": "offer", "titles": {"raw": "url_hash_offer 2"}, "debug": {"offerUrlHash": "12345123452"}},
                {
                    "entity": "offer",
                    "titles": {"raw": "url_hash_offer without hash"},
                    "debug": {"offerUrlHash": NoKey("offerUrlHash")},
                },
            ],
        )

        # Проверяем работу поискового литерала
        # Задаем запрос с offer-url-hash=12345123452
        # В выдаче должен быть только 'url_hash_offer 2'
        response = self.report.request_json('place=prime&text=url_hash_offer&offer-url-hash=12345123452')
        self.assertFragmentIn(response, {"titles": {"raw": "url_hash_offer 2"}})

        self.assertFragmentNotIn(response, {"titles": {"raw": "url_hash_offer 1"}})

        self.assertFragmentNotIn(response, {"titles": {"raw": "url_hash_offer without hash"}})

        # Проверяем работу поискового литерала с несколькими значениями
        # Задаем запрос с offer-url-hash=12345123451 и offer-url-hash=12345123452
        # В выдаче должны быть 'url_hash_offer 1' и 'url_hash_offer 2'
        # 'url_hash_offer without hash' отфильтруется
        response = self.report.request_json(
            'place=prime&text=url_hash_offer&offer-url-hash=12345123451&offer-url-hash=12345123452'
        )
        self.assertFragmentIn(response, {"titles": {"raw": "url_hash_offer 1"}})

        self.assertFragmentIn(response, {"titles": {"raw": "url_hash_offer 2"}})

        self.assertFragmentNotIn(response, {"titles": {"raw": "url_hash_offer without hash"}})

        response = self.report.request_json('place=prime&offer-url=supersite.com%2Fsuperoffer.html%3Fwombat%3D1')
        self.assertFragmentIn(response, {"titles": {"raw": "superoffer"}})

    @classmethod
    def prepare_random_empty_serp(cls):
        cls.crypta.on_default_request().respond(features=[])
        cls.index.offers += [Offer(title='ipad'), Offer(title='kijanka')]

    def test_random_empty_serp(self):
        '''
        проверяем, что для пары yandexuid-query, случайное число от которой больше порога, отдается пустой серп
        rand(12345678kijanka) = 0.08539641021
        rand(1234ipad) = 0.192304021
        '''
        response = self.report.request_json('place=prime&text=ipad&yandexuid=1234')
        self.assertFragmentIn(response, {"entity": "offer"})

        response = self.report.request_json(
            'place=prime&text=ipad&yandexuid=1234&rearr-factors=market_random_empty_serp=0.1'
        )
        self.assertFragmentIn(response, {"entity": "offer"})

        response = self.report.request_json(
            'place=prime&text=ipad&yandexuid=1234&rearr-factors=market_random_empty_serp=0.2'
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})

        response = self.report.request_json('place=prime&text=kijanka&yandexuid=12345678')
        self.assertFragmentIn(response, {"entity": "offer"})

        response = self.report.request_json(
            'place=prime&text=kijanka&yandexuid=12345678&rearr-factors=market_random_empty_serp=0.05'
        )
        self.assertFragmentIn(response, {"entity": "offer"})

        response = self.report.request_json(
            'place=prime&text=kijanka&yandexuid=12345678&rearr-factors=market_random_empty_serp=0.1'
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})

    @classmethod
    def prepare_no_picture(cls):
        '''
        проверяем что с флагом market_no_pictures=1 картинки пропадают
        '''
        cls.index.models += [
            Model(
                title='molotok',
                hyperid=906090,
                hid=6090,
                picinfo='//avatars.mdst.yandex.net/get-mpic/4138/test100/orig#100#200',
                add_picinfo='//avatars.mdst.yandex.net/get-mpic/1337/test100/orig#661#519',
            )
        ]
        cls.index.offers += [
            Offer(
                title='molotok',
                hyperid=906090,
                hid=6090,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    group_id=1234,
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
            )
        ]

    def test_no_picture_catalog(self):
        response = self.report.request_json('place=prime&hid=6090')

        self.assertFragmentIn(response, {"pictures": []})

        response = self.report.request_json('place=prime&hid=6090&rearr-factors=market_no_pictures=1')

        self.assertFragmentNotIn(response, {"pictures": []})

    def test_no_picture_search(self):
        response = self.report.request_json('place=prime&text=molotok')

        self.assertFragmentIn(response, {"pictures": []})

        response = self.report.request_json('place=prime&text=molotok&rearr-factors=market_no_pictures=1')

        self.assertFragmentNotIn(response, {"pictures": []})

    def test_modelid(self):
        response = self.report.request_json('place=prime&text=iphone')
        self.assertFragmentIn(response, {"total": 5, "totalOffers": 3, "totalModels": 2})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "type": "model",
                        "id": 307,
                    },
                    {
                        "type": "model",
                        "id": 308,
                    },
                ]
            },
        )

        response = self.report.request_json('place=prime&text=iphone&modelid=307')
        self.assertFragmentIn(response, {"total": 2, "totalOffers": 1, "totalModels": 1})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "type": "model",
                        "id": 307,
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&text=iphone&modelid=307&entities=offer')
        self.assertFragmentIn(response, {"total": 1, "totalOffers": 1, "totalModels": 0})
        self.assertFragmentIn(response, {"results": [{"wareId": "n9XlAsFkD2JzjIqQjT6w9w"}]})

        response = self.report.request_json('place=prime&text=iphone&modelid=307&entities=product')
        self.assertFragmentIn(response, {"total": 1, "totalOffers": 0, "totalModels": 1})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "type": "model",
                        "id": 307,
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&text=iphone&modelid=308,307')
        self.assertFragmentIn(response, {"total": 3, "totalOffers": 1, "totalModels": 2})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "type": "model",
                        "id": 307,
                    },
                    {
                        "type": "model",
                        "id": 308,
                    },
                    {"wareId": "n9XlAsFkD2JzjIqQjT6w9w"},
                ]
            },
        )

        response = self.report.request_json('place=prime&text=iphone&modelid=7981273981723')
        self.assertFragmentIn(response, {"total": 0})

        response = self.report.request_json(
            'place=prime&text=document with black screen and many words in title&modelid=1307'
        )
        self.assertFragmentIn(response, {"total": 1})

    def test_exact_text_match(self):
        """Проверяем поиск по точному соответствию
        С флагом &exact-match=1 должны находиться только документы содержащие все слова в title
        """

        # без флага находится все подряд
        response = self.report.request_json('place=prime&text=document with many words and black screen')
        self.assertFragmentIn(
            response,
            {
                'total': Greater(10),
                'results': [{'titles': {'raw': "green ball with black cross"}}, {'titles': {'raw': "total_and_shops"}}],
            },
        )

        # с флагом находятся только документы имеющие все слова в заголовке
        # порядок и пунктуация не важны
        response = self.report.request_json(
            'place=prime&text=document (with many words) and BLACK screen&exact-match=1'
        )
        self.assertFragmentIn(
            response,
            {
                'total': 2,
                'results': [
                    {'titles': {'raw': "document with black screen and many words in title"}},
                    {'titles': {'raw': "document with black screen and many words in title"}},
                ],
            },
            allow_different_len=False,
        )

    def test_default_offers(self):
        """
        Сортировка дефолтного оффера всегда SF_DEFAULT_OFFER_ORDERS
        """

        def check_default_offer_sorting(response):
            self.assertFragmentIn(response, 'Default offer: sorting SF_DEFAULT_OFFER_ORDERS')

        simple_prime_request = 'place=prime&allow-collapsing=1&rids=213&hid=17&use-default-offers=1&debug=1'
        response = self.report.request_json(simple_prime_request)
        self.assertFragmentIn(response, {'totalModels': 3})
        check_default_offer_sorting(response)

        response = self.report.request_json(simple_prime_request + '&mcpriceto=200')
        self.assertFragmentIn(response, {'totalModels': 2})
        check_default_offer_sorting(response)

        response = self.report.request_json(simple_prime_request + '&mcpricefrom=900')
        self.assertFragmentIn(response, {'totalModels': 1})
        check_default_offer_sorting(response)

        base_productoffers_request = 'place=productoffers&rids=213&hid=17&offers-set=default&debug=1'
        response = self.report.request_json(base_productoffers_request + '&hyperid=305')
        check_default_offer_sorting(response)
        response = self.report.request_json(base_productoffers_request + '&hyperid=304')
        check_default_offer_sorting(response)

    @skip('MARKETMPE-830: позиция очень нужна в рекомендаторе ставок мерчей')
    def test_default_offer_position(self):
        """
        Проверяем, что для ДО, запрошенных из прайма, позиция всегда 0
        Не можем  писать реальную позицию модели, так как делаем дозапрос до переранжирования
        """
        simple_prime_request = 'place=prime&allow-collapsing=1&rids=213&hid=17&use-default-offers=1&debug=1'
        response = self.report.request_json(simple_prime_request)
        self.assertFragmentIn(response, {'totalModels': 3})
        self.show_log_tskv.expect(record_type=0, url_type=6, position=0).times(3)

    def test_default_offer_position_old(self):
        """
        Проверяем, что под флагом для ДО, запрошенных из прайма, позиция > 0
        """
        simple_prime_request_old = 'place=prime&allow-collapsing=1&rids=213&hid=17&use-default-offers=1&rearr-factors=market_old_do_position=1&debug=1'
        response = self.report.request_json(simple_prime_request_old)
        self.assertFragmentIn(response, {'totalModels': 3})
        self.show_log_tskv.expect(record_type=0, url_type=6, position=0).times(0)

    def test_do_logs_for_sessions_models(self):
        """
        Для правильной сборки пользовательских сессий должны соблюдаться условия:
        - у всех элементов одного сниппета одинаковый super_uid (по нему будут собираться блоки в RALib)
        - ровно у одного элемента сниппета есть inclid (по нему будет определяться главный элемент блока)
        """
        simple_prime_request = 'place=prime&allow-collapsing=1&rids=213&hid=17&use-default-offers=1&debug=1'
        _ = self.report.request_json(simple_prime_request)
        for do_pos in range(1, 4):
            self.show_log_tskv.expect(record_type=0, url_type=6, position=do_pos, inclid=None).times(1)
        self.show_log_tskv.expect(record_type=0, url_type=6, inclid=NotEmpty()).times(0)
        self.show_log_tskv.expect(
            show_uid="04884192001117778888816001",
            record_type=1,
            url_type=16,
            position=1,
            super_uid="04884192001117778888816001",
            inclid=0,
        )
        # у ДО super_uid от модели, инклида нет, так как это не main result
        self.show_log_tskv.expect(
            show_uid="04884192001117778888806000",
            record_type=0,
            url_type=6,
            position=1,
            super_uid="04884192001117778888816001",
            inclid=None,
        )

        self.feature_log.expect(show_uid="04884192001117778888816001").times(1)
        # в фича логе записи для оффера с дозапросным шоуюидом (их 3, так как в тестах все шоу блок ид одинаковые)
        self.feature_log.expect(show_uid="04884192001117778888806000").times(3)

    def test_do_logs_for_sessions_models_blender(self):
        """
        в блендере все те же условия выполняются, что и в предыдущем тесте для прайма
        """
        simple_prime_request = 'place=blender&allow-collapsing=1&rids=213&hid=17&use-default-offers=1&debug=1'
        _ = self.report.request_json(simple_prime_request)
        for do_pos in range(1, 4):
            self.show_log_tskv.expect(record_type=0, url_type=6, position=do_pos, inclid=None).times(1)
        self.show_log_tskv.expect(record_type=0, url_type=6, inclid=NotEmpty()).times(0)
        self.show_log_tskv.expect(
            show_uid="04884192001117778888816001",
            record_type=1,
            url_type=16,
            position=1,
            super_uid="04884192001117778888816001",
            inclid=0,
        )
        # у ДО super_uid от модели, инклида нет, так как это не main result
        self.show_log_tskv.expect(
            show_uid="04884192001117778888806000",
            record_type=0,
            url_type=6,
            position=1,
            super_uid="04884192001117778888816001",
            inclid=None,
        )

        self.feature_log.expect(show_uid="04884192001117778888816001").times(1)
        # в фича логе записи для оффера с дозапросным шоуюидом (их 3, так как в тестах все шоу блок ид одинаковые)
        self.feature_log.expect(show_uid="04884192001117778888806000").times(3)

    def test_do_logs_for_sessions_offers(self):
        # проверяем для офферов
        _ = self.report.request_json('place=prime&text=iphone&show-urls=offercard%2Cencrypted')
        self.show_log_tskv.expect(
            show_uid="04884192001117778888800003",
            record_type=0,
            url_type=0,
            position=3,
            super_uid="04884192001117778888800003",
            inclid=0,
        )
        # у неглавного элемента нет инклида: (инклид ровно у одного элемента в сниппете)
        self.show_log_tskv.expect(
            show_uid="04884192001117778888808003",
            record_type=0,
            url_type=8,
            position=3,
            super_uid="04884192001117778888800003",
            inclid=None,
        )
        self.feature_log.expect(show_uid="04884192001117778888800003")

    def test_do_logs_for_sessions_offers_blender(self):
        # проверяем для офферов
        _ = self.report.request_json('place=blender&text=iphone&show-urls=offercard%2Cencrypted')
        self.show_log_tskv.expect(
            show_uid="04884192001117778888800003",
            record_type=0,
            url_type=0,
            position=3,
            super_uid="04884192001117778888800003",
            inclid=0,
        )
        # у неглавного элемента нет инклида: (инклид ровно у одного элемента в сниппете)
        self.show_log_tskv.expect(
            show_uid="04884192001117778888808003",
            record_type=0,
            url_type=8,
            position=3,
            super_uid="04884192001117778888800003",
            inclid=None,
        )
        self.feature_log.expect(show_uid="04884192001117778888800003")

    @classmethod
    def prepare_parent_model_id(cls):
        """
        Создаем три модели: негрупповую и группову с дочерней
        Создаем офферы для этих моделей
        """
        cls.index.models += [
            Model(title='non-child-model', hid=20, hyperid=320),
            Model(title='child-model', hid=20, hyperid=321, group_hyperid=322),
        ]

        cls.index.model_groups += [ModelGroup(title='parent-model', hid=20, hyperid=322)]

        cls.index.offers += [
            Offer(title='non-child-model-offer', fesh=520, hyperid=320, hid=20, randx=333, pickup_buckets=[1001]),
            Offer(title='child-model-offer', fesh=521, hyperid=321, hid=20, randx=444, pickup_buckets=[1002]),
            Offer(title='parent-model-offer', fesh=522, hyperid=322, hid=20, randx=555, pickup_buckets=[1003]),
        ]

        cls.index.outlets += [
            Outlet(point_id=523, fesh=520, region=213),
            Outlet(point_id=524, fesh=521, region=213),
            Outlet(point_id=525, fesh=522, region=213),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1001,
                fesh=520,
                options=[PickupOption(outlet_id=523, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=1002,
                fesh=521,
                options=[PickupOption(outlet_id=524, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=1003,
                fesh=522,
                options=[PickupOption(outlet_id=525, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_parent_model_id(self):
        """
        Запрашиваем плейсы prime, geo, productoffers и defaultoffer
        Проверяем, что для оффера дочерней модели есть parentId на выдаче,
        а у остальных офферов его нет
        """
        for place in ['prime', 'geo']:
            response = self.report.request_json('place={}&hid=20'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "titles": {"raw": "parent-model-offer"},
                            "model": {"id": 322, "parentId": Absent()},
                        },
                        {
                            "titles": {"raw": "child-model-offer"},
                            "model": {"id": 321, "parentId": 322},
                        },
                        {
                            "titles": {"raw": "non-child-model-offer"},
                            "model": {"id": 320, "parentId": Absent()},
                        },
                    ]
                },
            )

        for place in ['productoffers', 'defaultoffer']:
            response = self.report.request_json('place={}&hyperid=320'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "titles": {"raw": "non-child-model-offer"},
                            "model": {"id": 320, "parentId": Absent()},
                        }
                    ]
                },
            )

            response = self.report.request_json('place={}&hyperid=321'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "titles": {"raw": "child-model-offer"},
                            "model": {"id": 321, "parentId": 322},
                        }
                    ]
                },
            )

            response = self.report.request_json('place={}&hyperid=322'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "titles": {"raw": "parent-model-offer"},
                            "model": {"id": 322, "parentId": Absent()},
                        }
                    ]
                },
            )

    @classmethod
    def prepare_no_url(cls):
        '''Создаем два оффера без URL с cpa=REAL'''
        cls.index.shops += [
            Shop(fesh=1337, cpa=Shop.CPA_REAL, priority_region=213),
        ]

        cls.index.offers += [
            Offer(fesh=1337, waremd5='FsFdL4vdLurCNeU1Z8zjMg', title='nourl', has_url=False, cpa=Offer.CPA_REAL),
            Offer(fesh=1337, waremd5='gkE5_8fg7cQHxCW1I-FVPA', title='nourl2', has_url=False, cpa=Offer.CPA_REAL),
        ]

    def test_no_url(self):
        response = self.report.request_json('place=prime&text=nourl&show-urls=direct')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'gkE5_8fg7cQHxCW1I-FVPA',
                        'urls': {'direct': LikeUrl.of('//market.yandex.ru/offer/gkE5_8fg7cQHxCW1I-FVPA')},
                    },
                    {
                        'wareId': 'FsFdL4vdLurCNeU1Z8zjMg',
                        'urls': {'direct': LikeUrl.of('//market.yandex.ru/offer/FsFdL4vdLurCNeU1Z8zjMg')},
                    },
                ]
            },
        )

    @classmethod
    def prepare_test_cpa20_offer_count_always_one(cls):
        """Создаем 2 модели. Одна с 2 офферами, 1 из них CPA20, Вторая с 2 CPA10 офферами"""

        cls.index.hypertree += [
            HyperCategory(hid=71000, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=71001, hid=71000, title="cpa20model_1"),
            Model(hyperid=71002, hid=71000, title="cpa20model_2"),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=71001, rids=[213], offers=2, cpa20=True),
        ]

        cls.index.shops += [
            # Магазин по программе CPA 2.0
            Shop(fesh=17001, priority_region=213, cpa=Shop.CPA_REAL, cpa20=True),
            # Обычные магазины
            Shop(fesh=17002, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=17003, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(hyperid=71001, fesh=17001, bid=10, fee=100, cpa=Offer.CPA_REAL, price=2000, title="cpa20-offer1"),
            Offer(hyperid=71001, fesh=17002, bid=15, fee=150, cpa=Offer.CPA_REAL, price=1000, title="non_cpa20_offer1"),
            Offer(hyperid=71002, fesh=17002, bid=20, fee=200, cpa=Offer.CPA_REAL, price=1500, title="non_cpa20_offer2"),
            Offer(hyperid=71002, fesh=17003, bid=25, fee=250, cpa=Offer.CPA_REAL, price=1700, title="non_cpa20_offer3"),
        ]

    # https://st.yandex-team.ru/MARKETOUT-15805
    @classmethod
    def prepare_gift_ideas(cls):
        """
        создаем 3 магазина по 1 оффера разных категорий
        1 магазин 2 оффера в разных категориях
        1 магазин 3 оффера в 2 разных категориях
        всего 5 магазинов и 8 офферов в 3 категориях
        :return:
        """

        cls.index.hypertree += [
            HyperCategory(hid=1580501, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=1580502, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=1580503, output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops += [
            Shop(fesh=158051, cpa=Shop.CPA_REAL, business_fesh=2),
            Shop(fesh=158052, cpa=Shop.CPA_REAL, business_fesh=2),
            Shop(fesh=158053, cpa=Shop.CPA_REAL, business_fesh=2),
            Shop(fesh=158054, cpa=Shop.CPA_REAL, business_fesh=3),
            Shop(fesh=158055, cpa=Shop.CPA_REAL, business_fesh=3),
        ]

        cls.index.offers += [
            Offer(
                hid=15805001,
                fesh=158051,
                bid=10,
                fee=100,
                cpa=Offer.CPA_REAL,
                price=100,
                title="shop1-offer1",
                business_id=2,
            ),
            Offer(
                hid=15805002,
                fesh=158052,
                bid=10,
                fee=100,
                cpa=Offer.CPA_REAL,
                price=200,
                title="shop2-offer1",
                business_id=2,
            ),
            Offer(
                hid=15805003,
                fesh=158053,
                bid=10,
                fee=100,
                cpa=Offer.CPA_REAL,
                price=300,
                title="shop3-offer1",
                business_id=2,
            ),
            Offer(
                hid=15805001,
                fesh=158054,
                bid=10,
                fee=100,
                cpa=Offer.CPA_REAL,
                price=400,
                title="shop4-offer1",
                business_id=3,
            ),
            Offer(
                hid=15805002,
                fesh=158054,
                bid=10,
                fee=100,
                cpa=Offer.CPA_REAL,
                price=500,
                title="shop4-offer2",
                business_id=3,
            ),
            Offer(
                hid=15805001,
                fesh=158055,
                bid=10,
                fee=100,
                cpa=Offer.CPA_REAL,
                price=600,
                title="shop5-offer1",
                business_id=3,
            ),
            Offer(
                hid=15805002,
                fesh=158055,
                bid=10,
                fee=100,
                cpa=Offer.CPA_REAL,
                price=700,
                title="shop5-offer2",
                business_id=3,
            ),
            Offer(
                hid=15805003,
                fesh=158055,
                bid=10,
                fee=100,
                cpa=Offer.CPA_REAL,
                price=800,
                title="shop5-offer3",
                business_id=3,
            ),
        ]

    def test_gift_ideas_with_params(self):
        """
        Проверяем, что при запросе по категориям и магазинам выдачу попали корректные категория/магазин
        И в ней нет магазинов/категорий, отсутствющих в запросе
        """
        hidFeshQuery = "place=prime&hid=15805001&hid=15805003&fesh=158051&fesh=158054&use-default-offers=1"
        response = self.report.request_json(hidFeshQuery)

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "categories": [
                                {
                                    "id": 15805001,
                                }
                            ],
                            "shop": {
                                "id": 158051,
                            },
                        },
                        {
                            "categories": [
                                {
                                    "id": 15805001,
                                }
                            ],
                            "shop": {
                                "id": 158054,
                            },
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        # добавим огранчение по цене
        response = self.report.request_json(hidFeshQuery + "&mcpricefrom=300")

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "categories": [
                                {
                                    "id": 15805001,
                                }
                            ],
                            "shop": {
                                "id": 158054,
                            },
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        # теперь ограничение на количество
        response = self.report.request_json(hidFeshQuery + "&numdoc=1")

        self.assertFragmentIn(response, {"search": {"results": [{}]}}, allow_different_len=False)

    def test_business_id_through_fesh_filter(self):
        # проверяем, что фильтр fesh также фильтрует по business_id
        # проверка для белого запроса
        fesh_query = "place=prime&fesh={}"

        white_request = fesh_query.format(2) + '&hid=15805001'
        response = self.report.request_json(white_request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'shop': {'business_id': 2},
                        }
                    ],
                }
            },
        )
        white_request = fesh_query.format(3) + '&hid=15805002'
        response = self.report.request_json(white_request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'entity': 'offer',
                            'shop': {'business_id': 3},
                        }
                    ],
                }
            },
        )
        # проверка для cинего запроса
        blue_request = fesh_query.format(4) + '&hyperid=6983&rgb=blue'
        response = self.report.request_json(blue_request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'supplier': {'business_id': 4},
                                    }
                                ]
                            }
                        }
                    ],
                }
            },
        )

    def test_only_fesh_filter(self):
        # проверяем, что фильтр fesh работает через литералы, и запрос только с fesh (без текста и категории)
        # вернет офферы и не будет пустым
        fesh_query = "place=prime&fesh={}"

        white_request = fesh_query.format(2)
        response = self.report.request_json(white_request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': [
                        {
                            'entity': 'offer',
                            'shop': {'business_id': 2},
                        }
                    ],
                }
            },
        )
        # проверка для cинего запроса
        blue_request = fesh_query.format(4) + '&rgb=blue'
        response = self.report.request_json(blue_request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'supplier': {'business_id': 4},
                                    }
                                ]
                            }
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_prime_16995(cls):
        """
        Подготовка рейтингов
        """
        cls.index.shops += [
            Shop(
                fesh=1699501,
                new_shop_rating=NewShopRating(
                    new_rating=4.5,
                    new_rating_total=3.9,
                    skk_disabled=False,
                    force_new=False,
                    new_grades_count_3m=123,
                    new_grades_count=456,
                ),
                abo_shop_rating=AboShopRating(
                    shop_name="magazin.tld",
                    rating=2,  # не используется уже нигде
                    raw_rating=0.4,
                    status="oldshop",
                    cutoff="2017-01-30T15:23:51",
                    grade_base=0,
                    grade_total=9999,
                ),
            ),
            Shop(
                fesh=1699502,
                new_shop_rating=NewShopRating(
                    new_rating_total=3.9,
                ),
            ),
        ]

        cls.index.offers += [
            Offer(fesh=1699501, title='tovar16995', price=100),
            Offer(fesh=1699502, title='tovar16996', price=200),
        ]

    def test_rating(self):
        """
        Проверяем что рейтинг доезжает до выдачи правильно
        """
        response = self.report.request_json('place=prime&fesh=1699501')
        self.assertFragmentIn(
            response,
            {
                "entity": "shop",
                "id": 1699501,
                "gradesCount": 456,  # количество оценок
                "qualityRating": 5,  # округленный новый рейтинг (округление математическое, 4.5 -> 5)
                "isNewRating": True,  # флаг того что рейтинг "новый", по хорошему нигде не должент использоваться
                "newGradesCount": 456,  # оценок всего
                "newGradesCount3M": 123,  # оценок за 3 месяца
                "newQualityRating": 3.9,  # новый рейтинг всего
                "newQualityRating3M": 4.5,  # новый рейтинг за 3 месяца
            },
        )

        response = self.report.request_json('place=prime&fesh=1699502')
        self.assertFragmentIn(
            response,
            {
                "entity": "shop",
                "id": 1699502,
                "qualityRating": 4,  # при отсутствии параметра new_rating округляется new_rating_total
            },
        )

    @classmethod
    def prepare_book_restriction(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=90829,
                name="books",
                process_as_book_category=True,
                children=[HyperCategory(hid=90900, name="child", process_as_book_category=True)],
            )
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='age',
                hids_with_subtree=[90829],
                regional_restrictions=[
                    RegionalRestriction(
                        rids=[213],
                        show_offers=True,
                        disclaimers=[
                            Disclaimer(name='age', age="6year", text='Для взрослых', short_text='Для взрослых'),
                            Disclaimer(name='age', text='Дефолтный', short_text='Дефолтный'),
                        ],
                    ),
                ],
            )
        ]

        cls.index.books += [Book(hyperid=98201, title="My book 1", hid=90900)]

        cls.index.shops += [Shop(fesh=89000, priority_region=213)]

        cls.index.offers += [
            Offer(hyperid=98201, title="fairy tail", age='6', age_unit='year', hid=90900, fesh=89000, is_book=True)
        ]

    def test_book_restriction(self):
        """
        Проверяем, что на модели нет возраста и варнинга, а на оффере есть
        """
        # выводятся все дефолтные предупреждения, если нет возрастных
        default_warning = {
            "type": "age",
            "value": {
                "full": "Дефолтный",
                "short": "Дефолтный",
            },
        }
        response = self.report.request_json("hid=90900&place=prime&rids=213")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "age": NoKey("age"),
                        "titles": {"raw": "Book Writer \"My book 1\""},
                        "warnings": {"common": [default_warning]},
                    },
                    {
                        "entity": "offer",
                        "age": "6",
                        "titles": {"raw": "fairy tail"},
                        "model": {"id": 98201},
                        "warnings": {
                            "common": [
                                {"age": 6, "type": "age", "value": {"full": "Для взрослых", "short": "Для взрослых"}}
                            ]
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_offer_url_hash_model_filtration(cls):
        cls.index.hypertree += [HyperCategory(hid=14960834, name='Игры для приставок и ПК')]

        cls.index.shops += [
            Shop(fesh=1010, home_region=Const.ROOT_COUNTRY, regions=[213], cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(hyperid=7701, hid=14960831, title='GTA', offer_url_hash=111, fesh=1010),
            Offer(hyperid=7702, hid=14960832, title='NFS', offer_url_hash=222, fesh=1010),
            Offer(hyperid=7703, hid=14960833, title='PlayerUnknown’s Battlegrounds', offer_url_hash=333, fesh=1010),
            Offer(hyperid=7704, hid=14960834, title='Sword Art Online: Fatal Bullet', offer_url_hash=444, fesh=1010),
            Offer(hyperid=7705, hid=14960834, title='Brothers in Arms: Hell’s Highway', offer_url_hash=555, fesh=1010),
            Offer(hyperid=7706, hid=14960836, title='GTA V', offer_url_hash=666, fesh=1010),
        ]

        cls.index.models += [
            Model(hyperid=7701, hid=14960831, title='GTA'),
            Model(hyperid=7702, hid=14960832, title='NFS'),
            Model(hyperid=7703, hid=14960833, title='PlayerUnknown’s Battlegrounds'),
            Model(hyperid=7704, hid=14960834, title='Sword Art Online: Fatal Bullet'),
            Model(hyperid=7705, hid=14960834, title='Brothers in Arms: Hell’s Highway'),
            Model(hyperid=7706, hid=14960836, title='GTA V'),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=7701, offers=121, rids=[213]),
            RegionalModel(hyperid=7702, offers=122, rids=[213]),
            RegionalModel(hyperid=7703, offers=123, rids=[213]),
            RegionalModel(hyperid=7704, offers=124, rids=[213]),
            RegionalModel(hyperid=7705, offers=125, rids=[213]),
            RegionalModel(hyperid=7706, offers=126, rids=[213]),
        ]

    def test_offer_url_hash_model_filtration(self):
        '''Фильтрация моделей по offer-url-hash.

        Если в запросе задан параметр offer-url-hash, но необходимо найти офферы,
        которые подходят по тексту и offer-url-hash, их схлопнуть в модели и эти модели направить в выдачу.
        При этом не надо ходить в модельную коллекцию и искать еще модели, которые подходят просто по тексту.

        https://st.yandex-team.ru/MARKETOUT-19481
        '''

        response = self.report.request_json(
            'text=++скачать+игру+нА+КОМПЬЮТЕР+++++GTA&offer-url-hash=222&offer-url-hash=555&offer-url-hash=666&rids=213&dynamic-filters=0&nocache=da&timeout=1000000000&waitall=da&no-random=da&non-dummy-redirects=1&numdoc=20&place=prime&cvredirect=0&pp=7&debug=da'  # noqa
        )

        # Модель подходит по тексту и offer-url-hash и попадает в выдачу
        self.assertFragmentIn(response, {'results': [{'titles': {'raw': 'GTA V'}}]})

        # Модель подходит по тексту, но отсеивается по offer-url-hash и не попадает в выдачу
        self.assertFragmentNotIn(response, {'results': [{'titles': {'raw': 'GTA'}}]})

        # Модель подходит по offer-url-hash, но не подходит по тексту и не попадает в выдачу
        self.assertFragmentNotIn(response, {'results': [{'titles': {'raw': 'NFS'}}]})

        # Модель не подходит ни по тексту, ни по offer-url-hash и не попадает в выдачу
        self.assertFragmentNotIn(response, {'results': [{'titles': {'raw': 'PlayerUnknown’s Battlegrounds'}}]})

        # Модель подходит по названию категории, но не подходит ни по offer-url-hash, ни по тексту и не попадает в выдачу
        self.assertFragmentNotIn(response, {'results': [{'titles': {'raw': 'Sword Art Online: Fatal Bullet'}}]})

        # Модель подходит по названию категории и по offer-url-hash, но не подходит по тексту и не попадает в выдачу
        self.assertFragmentNotIn(response, {'results': [{'titles': {'raw': 'Brothers in Arms: Hell’s Highway'}}]})

    @classmethod
    def prepare_product_with_cyrillic(cls):
        cls.index.hypertree += [HyperCategory(hid=12345, name="Кириллица")]
        cls.index.models += [
            Model(hid=12345, title="Модель"),
            Model(hid=12345, title="ёлка ъ"),
            Model(hid=12345, title="hal9000"),
            Model(hid=12345, title="  processor  cores count — two  "),
            Model(hid=12345, title="съедобная пальма"),
            Model(hid=12345, title="Утюг с парогенератором BARELLİ BSM 2000"),
            Model(
                hid=12345,
                title="Ноутбук HP 650 (H5K65EA) (Pentium 2020M 2400 Mhz/15.6\"/1366x768/2048Mb/320Gb/DVD-RW/Wi-Fi/Bluetooth/Linux)",
            ),
            Model(hid=12345, title="салон IßÀÁÂÃÄÅÆÇÈÉÊËÌİÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸĀĄČŁŒŚŞŠ¡IƑ"),
            Model(hid=12345, title="Смесь NAN (Nestlé) Pre FM 85 (с рождения) 70 г"),
            Model(hid=12345, title="Very long string" * 1000),
            Model(hid=12345, title="qwe+rty+z+" * 110),
            Model(hid=12345, title="Яндекс станция"),
        ]

    def test_slug(self):
        """
        Slug формируется по следующим правилам:
        * кириллица транслитерируется по doc9303, "ь" и "ъ" удаляются
        * цифры не изменяюся
        * все символы, кроме латинских букв и цифр заменяются на "-", группа "-" схлопывается в один
        * результат приводится к нижнему регистру
        * "-" в начале и конце строки удаляются
        """
        response = self.report.request_json("hid=12345&place=prime&rids=213&text=модель")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Модель"}, "slug": "model"},
                ],
            },
        )

        response = self.report.request_json('place=prime&text=рыба')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Рыба, щуĠка и тюль при異體字вет эȰтàİо я >Çиç< qwe 󐁡a"},
                        "slug": "ryba-shchu-ka-i-tiul-pri-vet-e-taio-ia-cic-qwe-a",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&text=hal9000')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "hal9000"},
                        "slug": "hal9000",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&text=processor')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "processor cores count — two"},
                        "slug": "processor-cores-count-two",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&text=ёлка')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "ёлка ъ"},
                        "slug": "elka",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&text=пальма')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "съедобная пальма"},
                        "slug": "sedobnaia-palma",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&text=утюг')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Утюг с парогенератором BARELLİ BSM 2000"},
                        "slug": "utiug-s-parogeneratorom-barelli-bsm-2000",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&text=ноутбук')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {
                            "raw": "Ноутбук HP 650 (H5K65EA) (Pentium 2020M 2400 Mhz/15.6\"/1366x768/2048Mb/320Gb/DVD-RW/Wi-Fi/Bluetooth/Linux)"
                        },
                        "slug": "noutbuk-hp-650-h5k65ea-pentium-2020m-2400-mhz-15-6-1366x768-2048mb-320gb-dvd-rw-wi-fi-bluetooth-linux",
                    },
                ],
            },
        )

        # Транслитерация диакритиков
        response = self.report.request_json('place=prime&text=салон')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "салон IßÀÁÂÃÄÅÆÇÈÉÊËÌİÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸĀĄČŁŒŚŞŠ¡IƑ"},
                        "slug": "salon-issaaaaaaaeceeeeiiiiidnooooooeuuuuythyaacloesss-if",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&text=nestle')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Смесь NAN (Nestlé) Pre FM 85 (с рождения) 70 г"},
                        "slug": "smes-nan-nestle-pre-fm-85-s-rozhdeniia-70-g",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&text=яндекс')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Яндекс станция"},
                        "slug": "yandex-stantsiia",
                    },
                ],
            },
        )

    def test_slug_length(self):
        # Длина слага не больше 1000 символов
        response = self.report.request_json('place=prime&text=very')
        self.assertEqual(len(response.root['search']['results']), 1)
        self.assertEqual(len(response.root['search']['results'][0]['slug']), 1000)

        # После обрезания слага до нужной длины, с конца строки должны быть удалены не буквы и не цифры
        response = self.report.request_json('place=prime&text=rty')
        self.assertEqual(len(response.root['search']['results']), 1)
        self.assertEqual(len(response.root['search']['results'][0]['slug']), 999)

    @classmethod
    def prepare_no_offer_collection_request_without_hyper(cls):
        cls.index.gltypes += [
            GLType(
                param_id=100500,
                hid=910491,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='first'), GLValue(value_id=2, text='second')],
            ),
            GLType(
                param_id=100501,
                hid=910491,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='first'), GLValue(value_id=2, text='second')],
            ),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=910491, name='Мобильные телефоны', output_type=HyperCategoryType.GURU),
        ]
        cls.index.shops += [
            Shop(fesh=100000, home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
            Shop(fesh=100001, home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]
        cls.index.models += [
            Model(hyperid=770001, hid=910491, title='samsung1'),
            Model(hyperid=770002, hid=910491, title='samsung2'),
            Model(hyperid=770003, hid=910491, title='gnusmas1'),
            Model(hyperid=770004, hid=910491, title='gnusmas2'),
            Model(hyperid=770005, hid=910491, title='gnusmas3'),
            Model(hyperid=770006, hid=910491, title='gnusmas4'),
            Model(hyperid=770007, hid=910491, title='myphone6'),
            Model(hyperid=770008, hid=910491, title='lg'),
            Model(hyperid=770009, hid=910491, title='sony'),
            Model(hyperid=771000, hid=910491, title='xiaomi'),
        ]
        cls.index.offers += [
            Offer(
                hyperid=770001,
                hid=910491,
                title='samsung1 white',
                fesh=100000,
                glparams=[GLParam(param_id=100500, value=1)],
            ),
            Offer(
                hyperid=770002, hid=910491, title='samsung2', fesh=100000, glparams=[GLParam(param_id=100500, value=1)]
            ),
            Offer(
                hyperid=770001,
                hid=910491,
                title='samsung1 black',
                fesh=100001,
                glparams=[GLParam(param_id=100500, value=1)],
            ),
            Offer(
                hyperid=770003, hid=910491, title='gnusmas1', fesh=100001, glparams=[GLParam(param_id=100500, value=1)]
            ),
            Offer(
                hyperid=770004, hid=910491, title='gnusmas2', fesh=100001, glparams=[GLParam(param_id=100500, value=1)]
            ),
            Offer(
                hyperid=770005,
                hid=910491,
                title='gnusmas3 white',
                fesh=100001,
                glparams=[GLParam(param_id=100500, value=2)],
            ),
            Offer(
                hyperid=770006,
                hid=910491,
                title='gnusmas4 white',
                fesh=100001,
                glparams=[GLParam(param_id=100500, value=2)],
            ),
            Offer(
                hyperid=770005,
                hid=910491,
                title='gnusmas3 black',
                fesh=100000,
                glparams=[GLParam(param_id=100500, value=2)],
            ),
            Offer(
                hyperid=770006,
                hid=910491,
                title='gnusmas4 black',
                fesh=100000,
                glparams=[GLParam(param_id=100500, value=2)],
            ),
            Offer(
                hyperid=770007, hid=910491, title='myphone6', fesh=100000, glparams=[GLParam(param_id=100500, value=2)]
            ),
            Offer(hyperid=770008, hid=910491, title='lg', fesh=100000, glparams=[GLParam(param_id=100500, value=2)]),
            Offer(hyperid=770009, hid=910491, title='sony', fesh=100001, glparams=[GLParam(param_id=100500, value=2)]),
            Offer(
                hyperid=771000,
                hid=910491,
                title='xiaomi',
                fesh=100001,
                glparams=[GLParam(param_id=100501, value=1)],
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_no_offer_collection_request_without_hyper(self):
        # MARKETOUT-19716
        # Проверяем, что для обычных запросов без фильтров по ценам/параметрам магазинов офферные коллекции игнорируются
        response = self.report.request_json(
            'place=prime&hid=910491&pp=18&allow-collapsing=1&debug=da&rearr-factors=market_no_offer_collection_request_without_hyper=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 10,
                    'totalOffers': 0,  # Это в силу allow-collapsing=1
                    'totalOffersBeforeFilters': 0,  # Не ходили за офферами
                    'totalModels': 10,
                    'shops': 2,
                },
                # Проверяем, что с базовых пришли все фильтры по запрошенным офферам
                'filters': [
                    {'id': '100500', 'type': 'enum', 'noffers': 12, 'valuesCount': 2},
                    {'id': '100501', 'type': 'enum', 'noffers': 1, 'valuesCount': 1},
                ],
                'debug': {
                    'brief': {
                        'counters': {
                            'TOTAL_DOCUMENTS_PROCESSED': 10,
                            'OFFER_COUNT': 0,  # Офферные коллекции игнорируются
                        },
                    },
                },
            },
            preserve_order=False,
        )

        # Теперь добавим параметры, характерные именно для офферов, чтобы проверить, что офферные коллекции
        # посещать мы по-прежнему умеем. Полный список таких параметров можно посмотреть в функции
        # HasOfferSpecificParams из src/cgi/extensions.(h|cpp). В частности, fesh является таким параметром
        response = self.report.request_json(
            'place=prime&hid=910491&allow-collapsing=1&fesh=100000&debug=da&rearr-factors=market_no_offer_collection_request_without_hyper=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 6,
                    'totalOffers': 0,  # Это в силу allow-collapsing=1
                    'totalOffersBeforeFilters': 6,  # Зацепили все офферы
                    'totalModels': 6,
                },
                # Проверяем, что с базовых пришли все фильтры по запрошенным офферам
                'filters': [
                    {
                        'id': '100500',
                        'type': 'enum',
                        'noffers': 12,  # 6 в действительности, но из-за дозапроса за фильтрами оно суммируется
                        'valuesCount': 2,
                    },
                ],
                'debug': {
                    'brief': {
                        'counters': {
                            'TOTAL_DOCUMENTS_PROCESSED': 16,  # 16 потому что используем литерал по fesh
                            # А теперь зашли в офферные коллекции, чтобы захватить все офферы с fesh=100000
                            # и схлопнуть их в модели
                            'OFFER_COUNT': 6,
                        },
                    },
                },
            },
            preserve_order=False,
        )

        # Проверим, что при выключенном схлапывании ничего не ломается
        response = self.report.request_json(
            'place=prime&hid=910491&allow-collapsing=0&debug=da&rearr-factors=market_no_offer_collection_request_without_hyper=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {'total': 23, 'totalOffers': 13, 'totalModels': 10},
                'debug': {
                    'brief': {
                        'counters': {
                            'TOTAL_DOCUMENTS_PROCESSED': 23,
                            'OFFER_COUNT': 13,
                        },
                    },
                },
            },
            preserve_order=False,
        )

        # cpa тоже офферный фильтр
        response = self.report.request_json(
            'place=prime&hid=910491&pp=18&allow-collapsing=1&debug=da&rearr-factors=market_no_offer_collection_request_without_hyper=1&cpa=real'
        )
        self.assertFragmentIn(response, {'search': {'total': 1}})

    def test_collections_if_cpa_real(self):
        """Проверяем какие коллекции запрашиваются при поиске с cpa=real"""

        # не ищем в MODEL и BOOK
        response = self.report.request_json('place=prime&text=xiaomi&cpa=real&debug=da')
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                'SHOP_BLUE': Absent(),
                                'SHOP': NotEmpty(),
                                'MODEL': Absent(),
                                'BOOK': Absent(),
                            }
                        }
                    }
                }
            },
        )

    @classmethod
    def prepare_model_statistics_price_sort(cls):
        """Данные для проверки получения модельных статистик при сортировке по цене
        https://st.yandex-team.ru/MARKETOUT-20583
        """
        cls.index.models += [
            Model(hyperid=100855511, title='Диван Огонек'),
        ]

        cls.index.offers += [Offer(hyperid=100855511, ts=100855511)]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100855511).respond(None)

    def test_model_statistics_price_sort(self):
        """Проверяем, что при сортировке по цене по запросу модельных статистик
        офферы не отфильтровываются формулой на базовых
        https://st.yandex-team.ru/MARKETOUT-20583
        """
        # Проверка для десктопа
        response = self.report.request_json('place=prime&text=Диван+Огонек&how=aprice&use-default-offers=1')
        self.assertFragmentIn(
            response, {"results": [{"entity": "product", "titles": {"raw": "Диван Огонек"}, "offers": {"count": 1}}]}
        )

        # Проверка для тача
        response = self.report.request_json('place=prime&text=Диван+Огонек&how=aprice&use-default-offers=0&touch=1')
        self.assertFragmentIn(
            response, {"results": [{"entity": "product", "titles": {"raw": "Диван Огонек"}, "offers": {"count": 1}}]}
        )

    @classmethod
    def prepare_category_id(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=737373,
                children=[
                    HyperCategory(hid=121212),
                ],
            ),
        ]

        cls.index.shops += [Shop(fesh=7373, priority_region=213, name='shop')]

        cls.index.offers += [Offer(hid=121212, title='offer2', price=200, fesh=7373)]

    def test_category_id(self):
        _ = self.report.request_json('place=prime&hid=1&hid=121212&rids=213&glfilter=202:40')
        self.error_log.not_expect(code=3644)
        self.error_log.expect(
            "GlFactory returned null (wrong parameter or value ID?), glfilters: 202:40, offending filter: 202:40, category id: 121212"
        )

    @classmethod
    def prepare_data_for_waremd5_to_ignore(cls):
        cls.index.hypertree += [
            HyperCategory(hid=23472000),
            HyperCategory(hid=23472001),
            HyperCategory(hid=23472002),
        ]

        cls.index.models += [
            Model(hyperid=23472002, hid=23472002, title='танцующий рыбк'),
        ]

        cls.index.offers += [
            Offer(hid=23472000, waremd5="n9XlAsFkD2JzjI2347200w", title="23472offer0"),
            Offer(hid=23472001, waremd5="n9XlAsFkD2JzjI2347201w", title="23472offer1"),
            Offer(hyperid=23472002, waremd5="n9XlAsFkD2JzjI2347202w", title="23472offer2"),
        ]

    def test_waremd5_to_ignore_filter_offers(self):
        response = self.report.request_json('place=prime&hid=23472000,23472001&rids=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "23472offer0"}},
                    {"titles": {"raw": "23472offer1"}},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&hid=23472000,23472001&rids=0&waremd5-to-ignore=n9XlAsFkD2JzjI2347200w'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "23472offer1"}},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=multi_category&hid=23472000,23472001&rids=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "23472offer0"}},
                    {"titles": {"raw": "23472offer1"}},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&hid=23472000,23472001&rids=0&waremd5-to-ignore=n9XlAsFkD2JzjI2347200w'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "23472offer1"}},
                ]
            },
            allow_different_len=False,
        )

    def test_waremd5_to_ignor_filter_do(self):
        response = self.report.request_json('place=prime&hid=23472002&use-default-offers=1&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {"entity": "product", "offers": {"items": [{"entity": "offer", "titles": {"raw": "23472offer2"}}]}},
        )

        response = self.report.request_json(
            'place=prime&hid=23472002&use-default-offers=1&allow-collapsing=1&waremd5-to-ignore=n9XlAsFkD2JzjI2347202w'
        )
        self.assertFragmentIn(response, {"entity": "product", "offers": {"items": NoKey("items")}})

    @classmethod
    def prepare_mega_points(cls):
        cls.index.regiontree += [Region(rid=75, name='Vladivostok', tz_offset=36000)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=2300,
                carriers=[270],
                options=[PickupOption(outlet_id=12345670, price=500, day_from=1, day_to=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2301,
                carriers=[271],
                options=[PickupOption(outlet_id=12345671, price=200, day_from=1, day_to=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2302,
                carriers=[272],
                options=[PickupOption(outlet_id=12345672, price=300, day_from=1, day_to=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2303,
                fesh=73004,
                options=[PickupOption(outlet_id=12345673, price=400, day_from=1, day_to=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.shops += [
            Shop(fesh=73001, regions=[1], priority_region=1, delivery_service_outlets=[12345670, 12345671]),
            Shop(fesh=73002, regions=[1], priority_region=1, delivery_service_outlets=[12345670, 12345671]),
            Shop(fesh=73003, regions=[1], priority_region=1, delivery_service_outlets=[12345672]),
            Shop(fesh=73004, regions=[1], priority_region=1, delivery_service_outlets=[12345673]),
        ]

        cls.index.offers += [
            Offer(fesh=73001, title='offer from mega points 0 and 1 : 1', pickup_buckets=[2300, 2301]),
            Offer(fesh=73002, title='offer from mega points 0 and 1 : 2', pickup_buckets=[2300, 2301]),
            Offer(fesh=73003, title='offer from mega points 1 and 2 : 1', pickup_buckets=[2301, 2302]),
            Offer(fesh=73004, title='offer from own point 3 : 1', pickup_buckets=[2303]),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=12345670,
                region=75,
                gps_coord=GpsCoord(37.1, 55.1),
                delivery_service_id=270,
                delivery_option=OutletDeliveryOption(day_from=5, day_to=5, price=1000),
                working_days=[i for i in range(5)],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.FRIDAY,
                        hours_from='15:00',
                        hours_till='18:00',
                    )
                ],
            ),
            Outlet(
                point_id=12345671,
                region=1,
                gps_coord=GpsCoord(37.15, 55.1),
                delivery_service_id=271,
                delivery_option=OutletDeliveryOption(day_from=5, day_to=5, price=1000),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=12345672,
                region=1,
                gps_coord=GpsCoord(37.15, 55.15),
                delivery_service_id=272,
                delivery_option=OutletDeliveryOption(day_from=5, day_to=5, price=1000),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=12345673,
                region=1,
                gps_coord=GpsCoord(37.15, 55.15),
                fesh=73004,
                delivery_option=OutletDeliveryOption(day_from=5, day_to=5, price=1000),
                working_days=[i for i in range(10)],
            ),
        ]

    def test_outlet_id_filter(self):
        '''
        Проверяем, что при переданном outlet-id на prime возвращаются только те оффера, у которых есть самовывоз из этого аутлета.
        '''

        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0;market_nordstream=0'
        response = self.report.request_json(
            'place=prime&text=offer+from&rids=75&debug=da&point_id=12345670&offer-shipping=pickup&pickup-options=raw&debug=da'
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from mega points 0 and 1 : 1"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "500"},
                                }
                            ]
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from mega points 0 and 1 : 2"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "500"},
                                }
                            ]
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=offer+from&rids=75&debug=da&point_id=12345670&offer-shipping=pickup&pickup-options=raw&debug=da&rearr-factors=market_use_white_program_pickup_buckets=1&mega-points=true&touch=1'  # noqa
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from mega points 0 and 1 : 1"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "500"},
                                    "dayFrom": 1,
                                    "dayTo": 2,
                                }
                            ]
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from mega points 0 and 1 : 2"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "500"},
                                    "dayFrom": 1,
                                    "dayTo": 2,
                                }
                            ]
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=offer+from&rids=1&debug=da&point_id=12345671&offer-shipping=pickup&pickup-options=raw'
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from mega points 0 and 1 : 1"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "200"},
                                }
                            ]
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from mega points 0 and 1 : 2"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "200"},
                                }
                            ]
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from mega points 1 and 2 : 1"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "200"},
                                }
                            ]
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=offer+from&rids=1&debug=da&point_id=12345672&offer-shipping=pickup&pickup-options=raw'
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from mega points 1 and 2 : 1"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "300"},
                                }
                            ]
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=offer+from&rids=1&debug=da&point_id=12345673&offer-shipping=pickup&pickup-options=raw'
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer from own point 3 : 1"},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "price": {"value": "400"},
                                }
                            ]
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_main_stop_queries(cls):
        cls.index.offers += [Offer(title='электронный манок ЭМ41')]

        cls.reqwizard.on_request("купить электронный манок").respond(
            qtree="cHicvVJBaxNBFH5vdtcOk7QsSatxoLjktAiFxVMp2IoXi6iE4kFyqsVDzjkVT9EiSoO2Zy-K0lIQYqgE0tikPWoJ8vbivX_BX-DMZHdNN92re9k3b77ve9_MN-K-yPNpd6YEHvosgAJIKMNNuAVLeQ4uqD74EMAdZ9WpwBNYhxruIrxH-IRwiNBHUN8PBMJn8rUtNsWIxhVNy10N39J36lAvfBk2aEB96odNOpLoldGLxuTGxsAq6DG131-m4jEZEqnhASzi3YbF0QWZwSiDjwFWPrPRMeq_mMhAloyZRVjDRtU5aHVPz6r2nvpteG5OSjqhwSRPrTq-tcFdlIyOVCXdaTk7getQX-0Jl0lbrdqmBlV_o4FhM8nCnQSR1jT7BjnBmkDq6thUoDS3EvV3sXq4ZepRtxkjL-gU0zrUTRivYpz2W4NNfM44NhBUbvLcEuuplyDomNrqyAPqqSeWhO9cEv7H4YfkkY3RLkv8DzOJj6HSKS-IeLNH7SRY683JWZXtf43uWfH8oj5DvYliTKyECf70Z5XttSJ8zyQ3upHsu7mYStfcO5pph6mEWJRQ2s1Ophvl_qD1_x39y7ic55bLS8yzfCfQtnR_G28IW8deuMZRFvn1OZ3X_MMXj1c8WNB1jGAR4srccPjg9vy9yvlygihGiBzna1McC9ajytNtzKsuU12bz9S1gb9AAmIB"  # noqa
        )

    def test_main_stop_queries(self):

        response = self.report.request_json('place=prime&text=манок')
        self.assertFragmentIn(response, {"search": {"total": 1}})

        response = self.report.request_json('place=prime&text=электронный+манок')
        self.assertFragmentIn(response, {"search": {"total": 0}})

        response = self.report.request_json('place=prime&text=Электронный   МАНОК,:')
        self.assertFragmentIn(response, {"search": {"total": 0}})

        response = self.report.request_json('place=prime&text=купить электронный манок')
        self.assertFragmentIn(response, {"search": {"total": 0}})

    @classmethod
    def prepare_bgfactors_in_log(cls):
        cls.reqwizard.on_request("iphone").respond(relev='bgfactors=FeXQkj-1AT0Klz_AAQGSCgUVPQoHQA,,')

    def test_bgfactors_in_log(self):
        '''
        Провярем, что bgfactors обробатываются и содержатся в response
        '''
        response = self.report.request_json('place=prime&text=iphone&debug=da')

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "BG_DMOZ_QUERY_THEMES": "1.146999955",
                        "BG_NON_COMMERCIAL_QUERY": "1",
                        "BG_QUERY_COMMERCIALITY_MX": "1.179999948",
                        "BG_RANDOM_LOG_WORD_FACTORS_RANDOM_LOG_WORD_MAX_IS_LJ": "2.109999895",
                    }
                }
            },
        )

    @classmethod
    def prepare_family_banned(cls):
        cls.index.offers += [
            Offer(title='porn offer'),
            Offer(title='drug offer'),
            Offer(title='usual offer'),
            Offer(title='toy offer'),
        ]
        cls.reqwizard.on_request("porn").respond(family_stop_query=True)
        cls.reqwizard.on_request("drug").respond(family_stop_query=True)
        cls.reqwizard.on_request("sex toys").respond(relev='ad_cat_pb=CAFAAQ,,;ad_filtr=0;')

    def test_family_banned_requests(self):
        '''
        Проверяем, что при переданном флаге family=2 запросы, помеченные как стоп- для family возвращают
        пустую выдачу
        '''
        response = self.report.request_json('place=prime&text=porn&family=2')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=drug&family=2')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=usual&family=2')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}]}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=sex%20toys&family=2')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=sex%20toys&family=')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}]}, allow_different_len=False)

    def test_random_on_page_on_text_search(self):
        """флаг market_random_page_rearrange_on_text=1 пересортировывает документы на текстовом поиске
        (но только в пределах данной страницы)"""
        response = self.report.request_json('place=prime&text=sony&page=1&numdoc=6&entities=offer')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'titles': {'raw': 'sony-40'}},
                    {'titles': {'raw': 'sony-39'}},
                    {'titles': {'raw': 'sony-38'}},
                    {'titles': {'raw': 'sony-37'}},
                    {'titles': {'raw': 'sony-36'}},
                    {'titles': {'raw': 'sony-35'}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=sony&page=1&numdoc=6&entities=offer'
            '&rearr-factors=market_random_page_rearrange_on_text=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'titles': {'raw': 'sony-40'}},
                    {'titles': {'raw': 'sony-38'}},
                    {'titles': {'raw': 'sony-39'}},
                    {'titles': {'raw': 'sony-36'}},
                    {'titles': {'raw': 'sony-35'}},
                    {'titles': {'raw': 'sony-37'}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # вторая страница переранжируется независимо от первой (документы не перемешиваются)
        response = self.report.request_json('place=prime&text=sony&page=2&numdoc=6&entities=offer')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'titles': {'raw': 'sony-34'}},
                    {'titles': {'raw': 'sony-33'}},
                    {'titles': {'raw': 'sony-32'}},
                    {'titles': {'raw': 'sony-31'}},
                    {'titles': {'raw': 'sony'}},
                    {'titles': {'raw': 'sony-30'}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=sony&page=2&numdoc=6&entities=offer'
            '&rearr-factors=market_random_page_rearrange_on_text=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'titles': {'raw': 'sony-34'}},
                    {'titles': {'raw': 'sony-32'}},
                    {'titles': {'raw': 'sony-33'}},
                    {'titles': {'raw': 'sony'}},
                    {'titles': {'raw': 'sony-30'}},
                    {'titles': {'raw': 'sony-31'}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_random_on_page_on_textless_search(self):
        """флаг market_random_page_rearrange_on_textless=1 пересортировывает документы на бестексте
        (но только в пределах данной страницы)"""
        response = self.report.request_json('place=prime&hid=10&page=1&numdoc=6&entities=offer')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'titles': {'raw': 'sony-40'}},
                    {'titles': {'raw': 'sony-39'}},
                    {'titles': {'raw': 'sony-38'}},
                    {'titles': {'raw': 'sony-37'}},
                    {'titles': {'raw': 'sony-36'}},
                    {'titles': {'raw': 'sony-35'}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&hid=10&page=1&numdoc=6&entities=offer'
            '&rearr-factors=market_random_page_rearrange_on_textless=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'titles': {'raw': 'sony-37'}},
                    {'titles': {'raw': 'sony-39'}},
                    {'titles': {'raw': 'sony-38'}},
                    {'titles': {'raw': 'sony-40'}},
                    {'titles': {'raw': 'sony-36'}},
                    {'titles': {'raw': 'sony-35'}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_relev_in_title_hits(cls):
        cls.index.models += [
            Model(
                hyperid=1844901,
                hid=1844900,
                title='A300-30-22-80 кат. 220-240V AC Контактор 3п 300А (AC3), Uкат. 220V AC, доп. контакты 2но+2нз ABB 804325510660',
                description='Электрический компонент',
            ),
            Model(
                hyperid=1844903,
                hid=1844900,
                title='A300-40-22-80 кат. 220-240V AC Контактор 3п 300А (AC3), Uкат. 220V AC, доп. контакты 2но+2нз ABC 804325510660',
                description='Электрический компонент',
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=1844902,
                hid=1844900,
                title='A300-30-22-80 кат. 220-240V AC Контактор 3п 300А (AC3), Uкат. 220V AC, доп. контакты 2но+2нз ABB 804325510660',
                descr='Электрический компонент',
            ),
            Offer(
                hyperid=1844904,
                hid=1844900,
                title='A300-40-22-80 кат. 220-240V AC Контактор 3п 300А (AC3), Uкат. 220V AC, доп. контакты 2но+2нз ABC 804325510660',
                descr='Электрический компонент',
            ),
        ]

    def test_relev_in_title_hits(self):
        """
        Проверяем, что по тексту из конца сложного тайтла модели/офферы ищутся
        """
        response = self.report.request_json('place=prime&hid=1844900&text=ABB 804325510660&exact-match=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

        """
        Проверяем, что по тексту из конца сложного тайтла и описания выдача пустая
        """
        response = self.report.request_json('place=prime&hid=1844900&text=ABB 804325510660 Электрический&exact-match=1')
        self.assertFragmentIn(
            response,
            {
                "results": [],
            },
            allow_different_len=False,
        )

    def test_quoted_exact_match_experiment(self):
        """
        Проверяем флаги эксперимента с точным поиском по кавычкам
        """
        # Короткий запрос с игнорированием дистанции - возвращаются 2 точных рез-та
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат"&rearr-factors=market_quoted_exact_match=dequote_syntax&hid=1844900'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Длинный запрос с точкой с игнорированием дистанции - возвращаются 2 точных рез-та
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат. 220-240V AC Контактор 3п 300А (AC3)"&rearr-factors=market_quoted_exact_match=dequote_syntax'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Короткий запрос с исключением точек в запросе - возвращаются 2 точных рез-та
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат"&rearr-factors=market_quoted_exact_match=ignore_multisent'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Длинный запрос с точкой с исключением точек в запросе - возвращаются 2 точных
        # и 2 неточных рез-та, т.к. поиск по точному соответствию не включается
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат. 220-240V AC Контактор 3п 300А (AC3)"&rearr-factors=market_quoted_exact_match=ignore_multisent&hid=1844900'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                    {
                        "entity": "product",
                        "id": 1844903,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844904},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Короткий запрос с игнорированием дистанции - возвращаются 2 точных рез-та
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат"&rearr-factors=market_quoted_exact_match=all_quotes'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Длинный запрос с точкой с игнорированием дистанции - возвращаются 2 точных рез-та
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат. 220-240V AC Контактор 3п 300А (AC3)"&rearr-factors=market_quoted_exact_match=all_quotes'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Длинный запрос с точкой и несколькими блоками в кавычках - возвращаются 2 точных рез-та
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат. 220-240V" AC Контактор "3п 300А" (AC3)&rearr-factors=market_quoted_exact_match=all_quotes'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Короткий запрос с исключением точек в запросе - возвращаются 2 точных рез-та
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат"&rearr-factors=market_quoted_exact_match=all_quotes_ignore_multisent'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Длинный запрос с точкой с исключением точек в запросе - возвращаются 2 точных
        # и 2 неточных рез-та, т.к. поиск по точному соответствию не включается
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат. 220-240V AC Контактор 3п 300А (AC3)"&rearr-factors=market_quoted_exact_match=all_quotes_ignore_multisent&hid=1844900'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                    {
                        "entity": "product",
                        "id": 1844903,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844904},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Длинный запрос с точкой и несколькими блоками кавычек с исключением точек
        # в запросе - возвращаются 2 точных и 2 неточных рез-та, т.к. поиск по
        # точному соответствию не включается
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат. 220-240V" AC Контактор "3п 300А" (AC3)&rearr-factors=market_quoted_exact_match=all_quotes_ignore_multisent&hid=1844900'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                    {
                        "entity": "product",
                        "id": 1844903,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844904},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Длинный запрос без точки, но с несколькими блоками кавычек с исключением
        # точек в запросе - возвращаются 2 точных рез-та
        response = self.report.request_json(
            'place=prime&text="A300-30-22-80 кат" 220-240V AC Контактор "3п 300А" (AC3)&rearr-factors=market_quoted_exact_match=all_quotes_ignore_multisent'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1844901,
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1844902},
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_long_testbuckets(self):
        """
        Проверяем, что длинные тестбакеты ничего не ломают
        """

        test_buckets = ';'.join(['%d,0,21' % i for i in range(111234, 111334)])
        self.report.request_json('place=prime&test-buckets=%s&text=iphone' % test_buckets)

    @classmethod
    def prepare_full_description(cls):
        cls.index.shops += [Shop(fesh=393939, priority_region=213)]
        cls.index.hypertree += [
            HyperCategory(hid=393939),
        ]

        cls.index.models += [Model(hid=393939, hyperid=393939, full_description="very good description")]

        cls.index.offers += [Offer(fesh=393939, hyperid=393939)]

    def test_full_description(self):
        response = self.report.request_json("place=prime&rids=213&hid=393939")
        self.assertFragmentIn(response, {"entity": "product", "fullDescription": "very good description"})

    @classmethod
    def prepare_main_created_at(cls):
        cls.index.shops += [
            Shop(
                fesh=39391,
                created_at='2017-03-12',
                priority_region=213,
            ),
            Shop(fesh=39392, main_fesh=39391, created_at='2019-01-03', priority_region=213),
        ]

        cls.index.offers += [
            Offer(fesh=39391),
            Offer(fesh=39392),
        ]

    def test_main_created_at(self):
        # Проверяем, что в mainCreatedAt отдается дата создания первого магазина среди клонов
        response = self.report.request_json('place=prime&fesh=39391&rids=213')
        self.assertFragmentIn(
            response, {"entity": "offer", "shop": {"createdAt": "2017-03-12", "mainCreatedAt": "2017-03-12"}}
        )

        response = self.report.request_json('place=prime&fesh=39392&rids=213')
        self.assertFragmentIn(
            response, {"entity": "offer", "shop": {"createdAt": "2019-01-03", "mainCreatedAt": "2017-03-12"}}
        )

    @classmethod
    def prepare_model_without_nid(cls):
        cls.index.hypertree += [
            HyperCategory(hid=2694300, output_type=HyperCategoryType.GURU),
        ]

        cls.index.gltypes += [
            GLType(param_id=26943001, hid=2694300, gltype=GLType.ENUM, values=list(range(1, 2))),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=269430000,
                hid=2694300,
                primary=True,
                recipe=NavRecipe(
                    filters=[
                        NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=26943001, enum_values=[1]),
                    ]
                ),
            ),
        ]

        cls.index.models += [
            Model(hyperid=2694301, hid=2694300, title="model without nid"),
        ]

        cls.index.offers += [
            Offer(hid=2694300, hyperid=2694301, title="offer with nid", glparams=[GLParam(param_id=26943001, value=1)]),
        ]

    def test_model_without_nid(self):
        """Проверяем, что оффер, найденный по nid-у заменяется на модель,
        которая по этому nid-у не нашлась бы и эта модель есть на выдаче
        """

        response = self.report.request_json(
            'place=prime&hid=2694300&glfilter=26943001:1&pp=18&nid=269430000&allow-collapsing=1&force-show-offers-without-hyper=1'
        )
        self.assertFragmentIn(response, {'results': [{'entity': 'product', 'id': 2694301}]}, allow_different_len=False)

    @classmethod
    def prepare_exact_match_delimiter(cls):
        cls.matrixnet.on_place(MnPlace.QUERY_MODEL_GROUP_CLASSIFIER, 'asus zenfone Max').respond(0.7)
        cls.matrixnet.on_place(MnPlace.QUERY_SMTH_CLASSIFIER, 'asus zenfone Max').respond(0.2)

        cls.matrixnet.on_place(MnPlace.QUERY_MODEL_GROUP_CLASSIFIER, '').respond(0.7)
        cls.matrixnet.on_place(MnPlace.QUERY_SMTH_CLASSIFIER, '').respond(0.2)

        cls.index.gltypes += [
            GLType(
                param_id=2822201,
                hid=2808400,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='first'), GLValue(value_id=2, text='second')],
            ),
            GLType(
                param_id=2822202,
                hid=2808400,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=3, text='third'), GLValue(value_id=4, text='fourth')],
            ),
            GLType(
                param_id=2822201,
                hid=2808410,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='first'), GLValue(value_id=2, text='second')],
            ),
            GLType(
                param_id=2822202,
                hid=2808410,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=3, text='third'), GLValue(value_id=4, text='fourth')],
            ),
        ]

        cls.index.models += [
            Model(
                title='asus zenfone 8',
                hyperid=2808401,
                hid=2808400,
                ts=2808401,
                glparams=[
                    GLParam(param_id=2822201, value=1),
                    GLParam(param_id=2822202, value=3),
                ],
            ),
            Model(
                title='asus zenfone Max',
                hyperid=2808402,
                hid=2808400,
                ts=2808402,
                glparams=[
                    GLParam(param_id=2822201, value=1),
                    GLParam(param_id=2822202, value=3),
                ],
            ),
            Model(
                title='asus zenfone Max Pro',
                hyperid=2808403,
                hid=2808400,
                ts=2808403,
                glparams=[
                    GLParam(param_id=2822201, value=1),
                    GLParam(param_id=2822202, value=3),
                ],
            ),
            Model(
                title='asus zenfone Selfie Pro',
                hyperid=2808404,
                hid=2808400,
                ts=2808404,
                glparams=[
                    GLParam(param_id=2822201, value=1),
                ],
            ),
            Model(title='asus zenfone Lite', hyperid=2808405, hid=2808400, ts=2808405),
            Model(
                title='asus zenfone Max Selfie',
                hyperid=2808406,
                hid=2808400,
                ts=2808406,
                glparams=[
                    GLParam(param_id=2822202, value=3),
                ],
            ),
            Model(title='asus zenfone Laser', hyperid=2808407, hid=2808400, ts=2808407),
            Model(title='asus ROG Phone', hyperid=2808408, hid=2808400, ts=2808408),
        ]
        for seq in range(0, 8):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2808401 + seq).respond(0.9 - seq * 0.01)

        cls.index.hybrid_auction_settings += [HybridAuctionParam(category=Const.ROOT_HID, cpc_ctr_for_cpc=0.033)]

        cls.matrixnet.on_place(MnPlace.QUERY_MODEL_GROUP_CLASSIFIER, 'dell inspiron').respond(0.7)
        cls.matrixnet.on_place(MnPlace.QUERY_SMTH_CLASSIFIER, 'dell inspiron').respond(0.2)

        cls.matrixnet.on_place(MnPlace.QUERY_MODEL_GROUP_CLASSIFIER, 'philips smart').respond(0.7)
        cls.matrixnet.on_place(MnPlace.QUERY_SMTH_CLASSIFIER, 'philips smart').respond(0.2)

        cls.matrixnet.on_place(MnPlace.QUERY_MODEL_GROUP_CLASSIFIER, 'philips wide').respond(0.7)
        cls.matrixnet.on_place(MnPlace.QUERY_SMTH_CLASSIFIER, 'philips wide').respond(0.2)

        cls.index.shops += [
            Shop(fesh=2808410, priority_region=213, regions=[213]),
            Shop(fesh=2808411, priority_region=213, regions=[213, 2]),
            Shop(fesh=2808412, priority_region=213, regions=[213]),
            Shop(fesh=2808413, priority_region=213, regions=[213]),
            Shop(fesh=2808414, priority_region=2, regions=[213]),
        ]

        cls.index.offers += [
            Offer(title='Ноутбук DELL INSPIRON 5370', fesh=2808410, ts=2808410, bid=90),
            Offer(title='Ноутбук DELL Vostro 5481', fesh=2808411, ts=2808411, bid=70),
            Offer(title='Ноутбук DELL G5', fesh=2808412, ts=2808412, bid=50),
            Offer(title='Ноутбук DELL Inspiron 5584', fesh=2808413, ts=2808413, bid=40),
            Offer(
                title='Телевизор Philips 50PUT6023 Wide Smart',
                fesh=2808410,
                hid=2808410,
                ts=2808414,
                glparams=[
                    GLParam(param_id=2822201, value=1),
                ],
            ),
            Offer(title='Телевизор Philips 39PHT4003', fesh=2808414, hid=2808410, ts=2808415),
            Offer(
                title='Телевизор Philips 50PUT6023 Wide',
                fesh=2808414,
                hid=2808410,
                ts=2808417,
                glparams=[
                    GLParam(param_id=2822201, value=1),
                ],
            ),
            Offer(
                title='Телевизор Philips 39PHT4003 Smart',
                fesh=2808411,
                hid=2808410,
                ts=2808416,
                glparams=[
                    GLParam(param_id=2822201, value=1),
                ],
            ),
        ]
        for seq in range(0, 8):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2808410 + seq).respond(0.9 - seq * 0.01)

    def get_default_exact_match_models(self):
        return [
            {"titles": {"raw": "asus zenfone 8"}},
            {"titles": {"raw": "asus zenfone Max"}},
            {"titles": {"raw": "asus zenfone Max Pro"}},
            {"titles": {"raw": "asus zenfone Selfie Pro"}},
            {"titles": {"raw": "asus zenfone Lite"}},
            {"titles": {"raw": "asus zenfone Max Selfie"}},
            {"titles": {"raw": "asus zenfone Laser"}},
            {"titles": {"raw": "asus ROG Phone"}},
        ]

    def test_exact_match_delimiter(self):
        """Проверяем "умное точное соотвествие" - эксперимент, действующий под флагами
        exact-match=smart, market_query_model_group_threshold, market_query_smth_threshold
        """

        # без exact-match=smart точное соответствие не включается
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response, {'results': self.get_default_exact_match_models()}, preserve_order=True, allow_different_len=False
        )

        # при несоотвествии порога market_query_smth_threshold точное соответствие не включается
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.1'
        )
        self.assertFragmentIn(
            response, {'results': self.get_default_exact_match_models()}, preserve_order=True, allow_different_len=False
        )

        # при несоотвествии порога market_query_model_group_threshold точное соответствие не включается
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.9;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response, {'results': self.get_default_exact_match_models()}, preserve_order=True, allow_different_len=False
        )

        # со всеми порогами точное соответствие включается
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"entity": "exactTextMatchDelimiter"},
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем пейджинг с точным соотвествием, на первой странице у всех моделей точное соответствие есть
        response = self.report.request_json(
            'text=asus zenfone Max&numdoc=3&place=prime&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем пейджинг с точным соотвествием, на второй странице черта будет выше 1-й модели
        response = self.report.request_json(
            'text=asus zenfone Max&numdoc=3&page=2&place=prime&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"entity": "exactTextMatchDelimiter"},
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем пейджинг с точным соотвествием, на третьей странице черты нет, она выше
        response = self.report.request_json(
            'text=asus zenfone Max&numdoc=3&page=3&place=prime&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем пейджинг с точным соотвествием, на второй странице черта будет между 1-й моделью и 2-й
        response = self.report.request_json(
            'text=asus zenfone Max&numdoc=2&page=2&place=prime&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"entity": "exactTextMatchDelimiter"},
                    {"titles": {"raw": "asus zenfone 8"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем офферы, без точного соотвествия - порядок дефолтный
        response = self.report.request_json('text=dell inspiron&place=prime&rids=213')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Ноутбук DELL INSPIRON 5370"}},
                    {"titles": {"raw": "Ноутбук DELL Vostro 5481"}},
                    {"titles": {"raw": "Ноутбук DELL G5"}},
                    {"titles": {"raw": "Ноутбук DELL Inspiron 5584"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем офферы с точным соотвествием и аукцион - черта между 2-м и 3-м оффером
        # аукцион делится на два независимых котла, цена клика формируется в каждом котле от минставки
        response = self.report.request_json(
            'text=dell inspiron&show-urls=encrypted&place=prime&rids=213&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Ноутбук DELL INSPIRON 5370"}},
                    {"titles": {"raw": "Ноутбук DELL Inspiron 5584"}},
                    {"entity": "exactTextMatchDelimiter"},
                    {"titles": {"raw": "Ноутбук DELL Vostro 5481"}},
                    {"titles": {"raw": "Ноутбук DELL G5"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.show_log.expect(shop_id=2808410, click_price=14, bid=90)
        self.show_log.expect(shop_id=2808413, click_price=1, bid=40)
        self.show_log.expect(shop_id=2808411, click_price=42, bid=70)
        self.show_log.expect(shop_id=2808412, click_price=1, bid=50)

        # проверяем, что если позиции черты точного соотвествия и региональной черты совпадают,
        # то черта точного соотвествия приоритетнее
        response = self.report.request_json(
            'text=philips smart&place=prime&rids=213&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Телевизор Philips 50PUT6023 Wide Smart"}},
                    {"titles": {"raw": "Телевизор Philips 39PHT4003 Smart"}},
                    {"entity": "exactTextMatchDelimiter"},
                    {"titles": {"raw": "Телевизор Philips 39PHT4003"}},
                    {"titles": {"raw": "Телевизор Philips 50PUT6023 Wide"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что если позиция и региональной черты выше,
        # то региональная черта покажется
        response = self.report.request_json(
            'text=philips wide&place=prime&rids=213&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Телевизор Philips 50PUT6023 Wide Smart"}},
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "Телевизор Philips 50PUT6023 Wide"}},
                    {"entity": "exactTextMatchDelimiter"},
                    {"titles": {"raw": "Телевизор Philips 39PHT4003 Smart"}},
                    {"titles": {"raw": "Телевизор Philips 39PHT4003"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_strong_threshold_delimiter(cls):
        cls.index.offers += [
            Offer(title='tovar 1', fesh=2808414, ts=290900),
            Offer(title='tovar 2', fesh=2808414, ts=290901),
            Offer(title='tovar 3', fesh=2808411, ts=290902),
            Offer(title='tovar 4', fesh=2808411, ts=290903),
            Offer(title='nailed 1', ts=290904, waremd5='q-WVXRCCwqh9g6N-nCp5Vw', fesh=2808414),
            Offer(title='nailed 2', ts=290905, waremd5='U1S5zIgpekEel595IqMvYQ', fesh=2808414),
            Offer(title='not nailed', ts=290906, waremd5='U1S5zIgpekEel595IqMQQQ', fesh=2808414),
        ]

        for seq in range(0, 4):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 290900 + seq).respond(0.9 - seq * 0.1)

    def test_strong_threshold_delimiter(self):
        # если отсутствует exact-match=relevant, то черта не будет отображаться
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&rearr-factors=strong_assessor_threshold=0.5'
        )
        self.assertFragmentIn(
            response, {'results': self.get_default_exact_match_models()}, preserve_order=True, allow_different_len=False
        )

        # черта активируется на белом при exact-match=relevant, rearr-factors=market_strong_assessor_threshold=;
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&exact-match=relevant&rearr-factors=market_strong_assessor_threshold=0.85'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"entity": "strongThresholdDelimiter"},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем пейджинг с чертой недорелевантности, на первой странице все документы проходят strong порог
        response = self.report.request_json(
            'text=asus zenfone&numdoc=3&page=1&place=prime&exact-match=relevant&rearr-factors=market_strong_assessor_threshold=0.85'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем пейджинг с чертой недорелевантности, на второй стронице черта находится после второго документа
        response = self.report.request_json(
            'text=asus zenfone&numdoc=3&page=2&place=prime&exact-match=relevant&rearr-factors=market_strong_assessor_threshold=0.85'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"entity": "strongThresholdDelimiter"},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем пейджинг с чертой недорелевантности, на третьей странице черты нет
        response = self.report.request_json(
            'text=asus zenfone&numdoc=3&page=3&place=prime&exact-match=relevant&rearr-factors=market_strong_assessor_threshold=0.85'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что черта переносится на вторую страницу, если последний релевантный документ находится в конце страницы
        response = self.report.request_json(
            'text=asus zenfone&numdoc=5&page=2&place=prime&exact-match=relevant&rearr-factors=market_strong_assessor_threshold=0.85'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"entity": "strongThresholdDelimiter"},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что при совпадении региональной черты и недорелевантной черты, преимущество отдается недорелевантной
        response = self.report.request_json(
            'text=tovar&place=prime&exact-match=relevant&rids=2&rearr-factors=market_strong_assessor_threshold=0.79;market_relevance_formula_threshold=0.2'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'tovar 1'}},
                    {'titles': {'raw': 'tovar 2'}},
                    {'entity': 'strongThresholdDelimiter'},
                    {'titles': {'raw': 'tovar 3'}},
                    {'titles': {'raw': 'tovar 4'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что если региональная черта находится выше недорелевантной, то региональная черта отображается
        response = self.report.request_json(
            'text=tovar&place=prime&exact-match=relevant&rids=2&rearr-factors=market_strong_assessor_threshold=0.69;market_relevance_formula_threshold=0.2'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'tovar 1'}},
                    {'titles': {'raw': 'tovar 2'}},
                    {'entity': 'regionalDelimiter'},
                    {'titles': {'raw': 'tovar 3'}},
                    {'entity': 'strongThresholdDelimiter'},
                    {'titles': {'raw': 'tovar 4'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_min_model_price(cls):
        cls.index.hypertree += [
            HyperCategory(hid=19, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(title="bar", hid=19, hyperid=804),
        ]
        cls.index.shops += [Shop(fesh=9001, priority_region=213)]
        cls.index.offers += [
            Offer(title='bar 5 l', hyperid=804, hid=19, fesh=9001, price=100),
            Offer(title='bar 3 l', hyperid=804, hid=19, fesh=9001, price=101),
        ]

    def test_min_model_price(self):
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&rids=213&hid=19&use-default-offers=1&mcpricefrom=100&filter-by-min-model-price=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'product'},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&allow-collapsing=1&rids=213&hid=19&use-default-offers=1&mcpricefrom=101'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'product'},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&allow-collapsing=1&rids=213&hid=19&use-default-offers=1&mcpricefrom=101&filter-by-min-model-price=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {'entity': 'product'},
                ]
            },
        )

    def test_strong_threshold_delimiter_and_auction(self):
        # проверяем офферы с точным соотвествием и аукцион - черта между 2-м и 3-м оффером
        # аукцион делится на два независимых котла, цена клика формируется в каждом котле от минставки
        response = self.report.request_json(
            'text=dell inspiron&show-urls=encrypted&place=prime&rids=213&exact-match=relevant&rearr-factors=market_strong_assessor_threshold=0.88'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Ноутбук DELL INSPIRON 5370"}, 'shop': {'id': 2808410}},
                    {"titles": {"raw": "Ноутбук DELL Vostro 5481"}, 'shop': {'id': 2808411}},
                    {"entity": "strongThresholdDelimiter"},
                    {"titles": {"raw": "Ноутбук DELL G5"}, 'shop': {'id': 2808412}},
                    {"titles": {"raw": "Ноутбук DELL Inspiron 5584"}, 'shop': {'id': 2808413}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.show_log.expect(shop_id=2808410, click_price=62, bid=90)
        self.show_log.expect(shop_id=2808413, click_price=1, bid=40)
        self.show_log.expect(shop_id=2808411, click_price=1, bid=70)
        self.show_log.expect(shop_id=2808412, click_price=32, bid=50)

    def test_text_filters_delimiters_combination(self):
        """Проверяем взаимодействие разных черт - региональной,
        черты соответствия фильтрам и черты текстового точного соответствия,
        аналогичные проверки в случае использования недорелевантной черты
        вместо черты точного текстового соответсвия
        """
        # Если точное соответствие не включается по порогам, то рисуется обычная
        # фильтровая черта
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&hid=2808400&glfilter=2822201:1&exact-match=smart&rearr-factors=market_glfilter_delimiter=1;market_query_model_group_threshold=0.9;market_query_smth_threshold=0.4;'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Если включено точное соответствие или соответствие одновременно по релевантности и фильтрам,
        # то выводится только одна объединенная черта
        # При этом результаты под ней ранжируются по релевантности

        # черта по фильтрам должна была быть выше черты по текстовому точному соответствию
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&hid=2808400&glfilter=2822201:1&exact-match=smart&rearr-factors=market_glfilter_delimiter=1;market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4;'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"entity": "textFiltersDelimiter"},
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # черта по текстовому точному соответствию должна была быть выше черты по фильтрам
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&hid=2808400&glfilter=2822202:3&exact-match=smart&rearr-factors=market_glfilter_delimiter=1;market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4;'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"entity": "textFiltersDelimiter"},
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что недорелевантная черта объединяется с фильтровой чертой в общую черту
        response = self.report.request_json(
            'text=asus zenfone Max&place=prime&hid=2808400&glfilter=2822202:3&exact-match=relevant&rearr-factors=market_glfilter_delimiter=1;market_strong_assessor_threshold=0.87'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"entity": "textFiltersDelimiter"},
                    {
                        "titles": {"raw": "asus zenfone Max Selfie"}
                    },  # релвантность данного документа меньше, чем у последующего, но он ранжируется выше из-за
                    # функции GetRecommendedDocPriority из TCompareByDeliveryRelevance, в ней документы,
                    # которые не удовлетворяют фильтрам получают приоритет DocRangeNonFitersSuitable, что поднимает
                    # данный документ вверх
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что региональная черта покажется, если она выше
        # объединенной черты
        response = self.report.request_json(
            'text=philips wide&place=prime&hid=2808410&glfilter=2822201:1&rids=213&exact-match=smart&rearr-factors=market_glfilter_delimiter=1;market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Телевизор Philips 50PUT6023 Wide Smart"}},
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "Телевизор Philips 50PUT6023 Wide"}},
                    {"entity": "textFiltersDelimiter"},
                    {"titles": {"raw": "Телевизор Philips 39PHT4003 Smart"}},
                    {"titles": {"raw": "Телевизор Philips 39PHT4003"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # аналогичная проверка, в случае когда объеденная черта получается из
        # фильтрововой и недорелевантной черт
        response = self.report.request_json(
            'text=philips wide&place=prime&hid=2808410&glfilter=2822201:1&rids=213&exact-match=relevant&rearr-factors=market_glfilter_delimiter=1;market_strong_assessor_threshold=0.82'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "Телевизор Philips 50PUT6023 Wide Smart"}},
                    {"titles": {"raw": "Телевизор Philips 39PHT4003 Smart"}},
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "Телевизор Philips 50PUT6023 Wide"}},
                    {"entity": "textFiltersDelimiter"},
                    {"titles": {"raw": "Телевизор Philips 39PHT4003"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # без текстового запроса объединенная черта не показывается
        response = self.report.request_json(
            'place=prime&hid=2808400&exact-match=smart&rearr-factors=market_query_model_group_threshold=0.3;market_query_smth_threshold=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=False,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&hid=2808400&exact-match=relevant&rearr-factors=market_strong_assessor_threshold=0.89'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "asus zenfone Max"}},
                    {"titles": {"raw": "asus zenfone Max Pro"}},
                    {"titles": {"raw": "asus zenfone Max Selfie"}},
                    {"titles": {"raw": "asus zenfone 8"}},
                    {"titles": {"raw": "asus zenfone Selfie Pro"}},
                    {"titles": {"raw": "asus zenfone Lite"}},
                    {"titles": {"raw": "asus zenfone Laser"}},
                    {"titles": {"raw": "asus ROG Phone"}},
                ]
            },
            preserve_order=False,
            allow_different_len=False,
        )

    class Rs(object):
        def __init__(self):
            self.__state = ReportState.create()

        def nail(self, wareid, shop_cp=1, vendor_cp=0, fee=0):
            doc = self.__state.search_state.nailed_docs.add()
            doc.ware_id = wareid
            doc.shop_click_price = shop_cp
            doc.vendor_click_price = vendor_cp
            doc.fee = fee
            return self

        def to_str(self):
            return ReportState.serialize(self.__state)

    def test_strong_assessor_threshold_with_nailed_docs(self):
        """Проверяем, что дозапрос за приколоченными документами не
        взрывает недорелевантную черту из-за того, что не вычисляются факторы
        https://st.yandex-team.ru/MARKETINCIDENTS-4589
        """
        rs = T.Rs().nail('q-WVXRCCwqh9g6N-nCp5Vw').nail('U1S5zIgpekEel595IqMvYQ').to_str()
        # rs=eJwzcvCS4xIr1A0Piwhydi4vzLBMN_PTzXMuMA0rl2BUYNBgAMmHGgabVnmmF6Rmu6bmmFqaehb6lkUGQuQBRFkQ3A==

        response = self.report.request_json(
            'place=prime&text=nailed&rids=213&debug=da&rs={}&clid=708&touch=1&exact-match=relevant&rearr-factors=market_strong_assessor_threshold=0.2;market_relevance_formula_threshold=0.1'.format(
                rs
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "nailed 1"}},
                    {"titles": {"raw": "nailed 2"}},
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "not nailed"}},
                ]
            },
            preserve_order=False,
            allow_different_len=False,
        )

    A = ' очень '
    B = ' точное '
    C = ' соответствие '
    D = ' не '

    WARES = {
        A + B + C: 'FSqiKO1icV4qzU-I7w8qLg',
        D + A + B + C: 'KXGI8T3GP_pqjgdd7HfoHQ',
        A + B + A + B + C: 'yRgmzyBD4j8r4rkCby6Iuw',
        A + A + B + C: 'xzFUFhFuAvI1sVcwDnxXPQ',
        A + B + B + C: 'gpQxwKBuLtj5OIlRrvGwTw',
    }

    @classmethod
    def _get_ware(cls, s):
        return T.WARES[s]

    @classmethod
    def prepare_rough_exact_match_specific_cases(cls):
        a = T.A
        b = T.B
        c = T.C
        d = T.D

        def offer(s):
            return Offer(title=s, waremd5=T._get_ware(s))

        cls.index.offers += [
            offer(a + b + c),
            offer(d + a + b + c),
            offer(a + b + a + b + c),
            offer(a + a + b + c),
            offer(a + b + b + c),
        ]

    def test_rough_exact_match_specific_cases(self):
        """
        Проверяем всякие хитрые кейсы с повторяющимися словами при матчинге
        точного соответствия
        """
        a = T.A
        b = T.B
        c = T.C
        d = T.D

        def check_rough_prefix(title, text, length, rough, ratio):
            reqid = str(hash((title, text)))
            _ = self.report.request_json('place=prime&text=%s&reqid=%s' % (text, reqid))
            flength = str(length) if length > 1e-3 else Absent()
            frough = Round(1.0, 2) if rough else Absent()
            fratio = Round(ratio, 2) if ratio > 1e-3 else Absent()
            self.feature_log.expect(
                ware_md5=T._get_ware(title),
                rough_exact_match_length_title=flength,
                rough_exact_match_length_full=flength,
                rough_exact_match_title=frough,
                rough_exact_match_full=frough,
                rough_exact_match_ratio_title=fratio,
                rough_exact_match_ratio_full=fratio,
                req_id=reqid,
            )

        check_rough_prefix(title=a + b + c, text=a + b + c, length=3, rough=True, ratio=1)

        check_rough_prefix(title=d + a + b + c, text=a + b + c, length=3, rough=True, ratio=1)

        check_rough_prefix(title=a + b + c, text=d + a + b + c, length=0, rough=False, ratio=0)

        check_rough_prefix(title=a + b + c, text=a + d + b + c, length=1, rough=False, ratio=1.0 / 4)

        check_rough_prefix(title=a + b + a + b + c, text=a + b + c, length=3, rough=True, ratio=1)

        check_rough_prefix(title=a + b + a + b + c, text=a + b + a + c, length=3, rough=False, ratio=3.0 / 4)

        check_rough_prefix(title=a + b + a + b + c, text=a + a + b + c, length=1, rough=False, ratio=1.0 / 4)

        check_rough_prefix(title=a + a + b + c, text=a + a + b + c, length=4, rough=True, ratio=1)

        check_rough_prefix(title=a + a + b + c, text=a + b + c, length=3, rough=True, ratio=1)

        check_rough_prefix(title=a + b + b + c, text=a + b + c, length=2, rough=False, ratio=2.0 / 3)

        check_rough_prefix(title=a + b + b + c, text=a + b + b + c, length=4, rough=True, ratio=1)

    @classmethod
    def prepare_rough_exact_match_body_full(cls):
        cls.index.offers += [
            Offer(
                waremd5='bpQ3a9LXZAl_Kz34vaOpSg',
                title='супер крутой вообще нереально крутой айфон самой последней версии',
            ),
            Offer(
                waremd5='91t1fTRZw-k-mN2re5A5OA',
                title='ololo',
                descr='супер крутой вообще нереально крутой айфон самой последней версии',
            ),
            Offer(
                waremd5='BH8EPLtKmdLQhLUasgaOnA',
                title='ololo',
                body='супер крутой вообще нереально крутой айфон самой последней версии',
            ),
        ]

    def test_rough_exact_match_body_full(self):
        """
        Проверяем, что факторы rough_exact_match_* работают для body и full
        """

        _ = self.report.request_json('place=prime&text=%s&reqid=%s' % ('крутой айфон самой последней версии', '123'))
        self.feature_log.expect(
            ware_md5='bpQ3a9LXZAl_Kz34vaOpSg',
            rough_exact_match_length_full=Round(5.0, 2),
            rough_exact_match_full=Round(1.0, 2),
            rough_exact_match_ratio_full=Round(1.0, 2),
            req_id='123',
        )
        self.feature_log.expect(
            ware_md5='91t1fTRZw-k-mN2re5A5OA',
            rough_exact_match_length_body=Round(5.0, 2),
            rough_exact_match_body=Round(1.0, 2),
            rough_exact_match_ratio_body=Round(1.0, 2),
            req_id='123',
        )
        self.feature_log.expect(
            ware_md5='BH8EPLtKmdLQhLUasgaOnA',
            rough_exact_match_length_full=Round(5.0, 2),
            rough_exact_match_full=Round(1.0, 2),
            rough_exact_match_ratio_full=Round(1.0, 2),
            req_id='123',
        )

        _ = self.report.request_json('place=prime&text=%s&reqid=%s' % ('айфон крутой', '456'))
        self.feature_log.expect(
            ware_md5='bpQ3a9LXZAl_Kz34vaOpSg',
            rough_exact_match_length_full=Round(1.0, 2),
            rough_exact_match_full=Absent(),
            rough_exact_match_ratio_full=Round(0.5, 2),
            req_id='456',
        )
        self.feature_log.expect(
            ware_md5='91t1fTRZw-k-mN2re5A5OA',
            rough_exact_match_length_body=Round(1.0, 2),
            rough_exact_match_body=Absent(),
            rough_exact_match_ratio_body=Round(0.5, 2),
            req_id='456',
        )
        self.feature_log.expect(
            ware_md5='BH8EPLtKmdLQhLUasgaOnA',
            rough_exact_match_length_full=Round(1.0, 2),
            rough_exact_match_full=Absent(),
            rough_exact_match_ratio_full=Round(0.5, 2),
            req_id='456',
        )

        _ = self.report.request_json('place=prime&text=%s&reqid=%s' % ('крутой айфон', '789'))
        self.feature_log.expect(
            ware_md5='bpQ3a9LXZAl_Kz34vaOpSg',
            rough_exact_match_length_full=Round(2.0, 2),
            rough_exact_match_full=Round(1.0, 2),
            rough_exact_match_ratio_full=Round(1.0, 2),
            req_id='789',
        )
        self.feature_log.expect(
            ware_md5='91t1fTRZw-k-mN2re5A5OA',
            rough_exact_match_length_body=Round(2.0, 2),
            rough_exact_match_body=Round(1.0, 2),
            rough_exact_match_ratio_body=Round(1.0, 2),
            req_id='789',
        )
        self.feature_log.expect(
            ware_md5='BH8EPLtKmdLQhLUasgaOnA',
            rough_exact_match_length_full=Round(2.0, 2),
            rough_exact_match_full=Round(1.0, 2),
            rough_exact_match_ratio_full=Round(1.0, 2),
            req_id='789',
        )

    @classmethod
    def prepare_rough_exact_match_subtokens(cls):
        cls.index.offers += [Offer(waremd5='EpnWVxDQxj4wg7vVI1ElnA', title='127.0.0.1')]

    def test_rough_exact_match_subtokens(self):
        """
        Проверяем, что ratio-факторы корректно считаются на запросах
        с сабтокенами
        """

        _ = self.report.request_json(
            'place=prime&text=%s&reqid=%s&offerid=EpnWVxDQxj4wg7vVI1ElnA' % ('127.0.0.1', '234')
        )
        self.feature_log.expect(
            ware_md5='EpnWVxDQxj4wg7vVI1ElnA',
            rough_exact_match_length_full=Round(1),
            rough_exact_match_full=Round(1),
            rough_exact_match_ratio_full=Round(1),
            req_id='234',
        )

        _ = self.report.request_json(
            'place=prime&text=%s&reqid=%s&offerid=EpnWVxDQxj4wg7vVI1ElnA' % ('127.0.0.2', '345')
        )
        self.feature_log.expect(
            ware_md5='EpnWVxDQxj4wg7vVI1ElnA',
            rough_exact_match_length_full=Absent(),
            rough_exact_match_full=Absent(),
            rough_exact_match_ratio_full=Absent(),
            req_id='345',
        )

    def test_quotes_and_rough(self):
        """
        Проверяем, что под специальным флагом на запросы в кавычках не находятся документы
        без фактора жёсткого точного соответствия
        """

        good_request = 'place=prime&text=крутой+айфон+самой+последней+версии'
        quoted_good_request = 'place=prime&text="крутой+айфон+самой+последней+версии"'
        bad_request = 'place=prime&text=айфон+крутой'
        quoted_bad_request = 'place=prime&text="айфон+крутой"'
        flag = '&rearr-factors=market_only_rough_in_quotes=1'

        for r in (
            good_request,
            good_request + flag,
            quoted_good_request,
            quoted_good_request + flag,
            bad_request,
            bad_request + flag,
            quoted_bad_request,
        ):

            response = self.report.request_json(r)
            self.assertFragmentIn(response, {'wareId': 'bpQ3a9LXZAl_Kz34vaOpSg'})

        response = self.report.request_json(quoted_bad_request + flag)
        self.assertFragmentNotIn(response, {'wareId': 'bpQ3a9LXZAl_Kz34vaOpSg'})

    @classmethod
    def prepare_disable_beauty_new(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=90509,
                children=[
                    HyperCategory(hid=91159),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=3008, hid=90509, title='root beauty model', new=True),
            Model(hyperid=3020, hid=91159, title='child beauty model', new=True),
        ]

    def test_disable_beauty_new(self):
        response = self.report.request_json('place=prime&hid=90509&rearr-factors=market_disable_beauty_new=1')
        self.assertFragmentIn(response, {"id": 3008, "entity": "product", "isNew": False}, preserve_order=True)

        response = self.report.request_json('place=prime&hid=91159&rearr-factors=market_disable_beauty_new=1')
        self.assertFragmentIn(response, {"id": 3020, "entity": "product", "isNew": False}, preserve_order=True)

        response = self.report.request_json('place=prime&hid=90509')
        self.assertFragmentIn(response, {"id": 3008, "entity": "product", "isNew": True}, preserve_order=True)

        response = self.report.request_json('place=prime&hid=91159')
        self.assertFragmentIn(response, {"id": 3020, "entity": "product", "isNew": True}, preserve_order=True)

    @classmethod
    def prepare_suggest_text_highlight(cls):
        cls.index.offers += [
            Offer(
                title='кухонная плита красивая',
                hid=1050,
                glparams=[
                    GLParam(param_id=1, value=1),
                ],
            )
        ]

        qtree4market = "cHicdVJBaxNBGP2-mU0dxliWpMV0ILjktAjCopciSMVTEamhJ9mDaBUM4imn4qkYKpJg9eJBDx6USg9ighJI1W0vHqT0MHvy4k8R9JttZrpm0z09vnnf28d7n7wuy-KMP1uDAEMWQQUUNOA8XITLZQE-0BxCiOBqabnUhFtwB1r4EuENwjuEzwgJAn0_ETTeV3-5fCiP1gStGTlff0076abe04lO0p4eKgycvsjpwzIY_dbHOStfWJ34XQSLeO0pF-iDKnAbEGKEzffsyHI7lAVKzVBgEVZhbYYkIKy2xDq0B6xA3a1lHomKG7G30x8dxHy7P1qTPleeHuok5IQZ4UQPMgyEv-g9woLmLH3hGEM9zKaomN617xmzsFVgGvQ9Q0CaHae-ZdXJ9Zab9iyTdKon6-iR29i0r5lf5Qs1p_cphVwak_5bsI6PmcANBOpN_WDy9sQFSJL4pkfpk7RHp4XBuPvSlO67N233uaVprf_CrPUca7Lv5yjdqx64_viz_YOY7fRj9uFTzLb71DxS8zaKgUMnx2OLkbTpUSgu0lz0HTfr0VVRRO2GzJmdfnnHMTbKgvuixgIeliJj3My7eE56JtnKWYGqKhbmTSj1lT-XlgK4YLBlsDFjZv7V64Wl-tvowf8MlmMcHt64Un_0-94xozrWOC3E6imBFb7SvNvF8njqidm2sfgPMJobOA,,"  # noqa
        cls.reqwizard.on_request("кухонные плиты").respond(qtree=qtree4market)

    def test_suggest_text_highlight(self):
        request = "place=prime&debug=1&suggest_text=кухонные плиты&hid=1050&rearr-factors=market_enable_suggest_text_highlight=0;"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {
                                "highlighted": [{"value": "кухонная плита красивая", "highlight": NoKey("highlight")}]
                            }
                        }
                    ]
                }
            },
        )

        request = "place=prime&debug=1&suggest_text=кухонные плиты&hid=1050&rearr-factors=market_enable_suggest_text_highlight=1;"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {
                                "highlighted": [
                                    {"value": "кухонная", "highlight": True},
                                    {"value": " ", "highlight": NoKey("highlight")},
                                    {"value": "плита", "highlight": True},
                                    {"value": " красивая", "highlight": NoKey("highlight")},
                                ]
                            }
                        }
                    ]
                }
            },
        )

    def test_bad_rs(self):
        """При невалидном rs репорт продлолжает отвечать и логгирует ошибку"""
        self.error_log.expect(code=3630, message=Contains('can not decode report state from')).times(4)

        # non base64 rs
        response = self.report.request_json('place=prime&hid=1&rs=B@D_RS')
        self.assertFragmentIn(response, {"entity": "product"})

        # bad zlib inside of base64 (incorrect header check)
        response = self.report.request_json('place=prime&hid=1&rs=ejzjybcsmty0mjftmdaxtzy2mdawbllmdcqylrgnaum-bii%2c')
        self.assertFragmentIn(response, {"entity": "product"})

    def test_output_category_path(self):
        """
        tests output format for place prime
        """
        response = self.report.request_json(
            'place=prime&text=%D0%BC%D0%BE%D0%BB%D0%BE%D0%BA%D0%BE&entities=offer&allow-collapsing=0'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop_category_path': 'категория 1\\категория 2\\категория 3',
                'shop_category_path_ids': '1\\2\\3',
                'original_sku': 'milk123',
            },
        )

    def test_return_only_head_flag(self):
        """
        tests that flag for MARKETRANK-2 is working
        """
        response = self.report.request_json(
            'place=prime&rids=213&pp=18&text=iphone&debug=da&rearr-factors=return_only_head=1'
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Returning only head due to flag")]})

    def test_internal_subrequest_trace_log(self):
        """
        tests tracing for MARKETOUT-38464
        """
        self.report.request_json(
            'place=prime&text=laptop2&hid=13&glfilter=300:11&debug=da',
            headers={'X-Market-Req-ID': "market-request-id/42"},
        )

        self.external_services_trace_log.expect(
            request_id="market-request-id/42/0",
        )

        self.external_services_trace_log.expect(
            source_module="market-report",
            target_module="market-report",
            protocol="internal-subrequest",
            request_id=(Regex("market-request-id/42/[1-9]")),  # у внутренних и внешних подзапросов единый счетчик
            http_method=None,
        )

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
        response = self.report.request_json("place=prime&text=africa")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "offer africa 1"},
                    }
                ]
            },
        )

        # С флагом market_offers_wizard_for_products=1 оффер с hid 90538 фильтруется, так как является подкатегорией hid 90536
        response = self.report.request_json("place=prime&text=africa&rearr-factors=market_offers_wizard_for_products=1")
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "offer africa 1"},
                    }
                ]
            },
        )

    def test_sponsorred_offers(self):
        """
        https://st.yandex-team.ru/MARKETOUT-42844
        выдача спонсорский офферов (назначаются рандомно)
        количество задается в rearr
        :return:
        """
        request = 'place=prime&text=total_and_shops&rearr-factors=market_report_enable_sponsored=3'
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    # models
                    {"sponsored": NoKey("sponsored")},
                    {"sponsored": NoKey("sponsored")},
                    {"sponsored": True},
                    {"sponsored": NoKey("sponsored")},
                    {"sponsored": NoKey("sponsored")},
                    # offers
                    {"sponsored": True},
                    {"sponsored": NoKey("sponsored")},
                    {"sponsored": NoKey("sponsored")},
                    {"sponsored": True},
                    {"sponsored": NoKey("sponsored")},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_smart_viewtype(cls):
        cls.index.hypertree += [
            HyperCategory(hid=101, view_type=ViewType.LIST),
            HyperCategory(hid=102, view_type=ViewType.LIST),
            HyperCategory(hid=103, view_type=ViewType.GRID),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=1,
                display_style='GRID',
                children=[
                    NavCategory(nid=2, hid=101, name='pixel-nid-1'),
                    NavCategory(nid=3, hid=102, name='pixel-nid-2', display_style='GRID'),
                ],
            )
        ]

        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_CLOTHES", 7857072),
        ]

        cls.index.models += [
            Model(hid=101, title="pixel 1", hyperid=1231),
            Model(hid=102, title="pixel 2", hyperid=1232),
            Model(hid=101, title="surface 1", hyperid=1233),
            Model(hid=103, title="surface 2", hyperid=1234),
            Model(hid=101, title="alienware 1", hyperid=1235),
            Model(hid=7857072, title="alienware 2", hyperid=1236),
        ]

        cls.index.offers += [
            Offer(hid=101, hyperid=1231, title='pixel 1'),
            Offer(hid=102, hyperid=1232, title='pixel 2'),
            Offer(hid=101, hyperid=1233, title='surface 1'),
            Offer(hid=103, hyperid=1234, title='surface 2'),
            Offer(hid=101, hyperid=1235, title='alienware 1'),
            Offer(hid=7857072, hyperid=1236, title='alienware 2'),
        ]

    def test_smart_viewtype(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-43222
        Проверяем выставление grid выдачи в зависимости от найденных категорий
        '''

        # Запрос c флагом market_grid_view_grid_categories_percent. View меняется на grid, так как у "pixel 1" у нида хида выставлен display_style=GRID
        response = self.report.request_json(
            'place=prime&text=pixel&rearr-factors=market_grid_view_grid_categories_percent=0.4'
        )
        self.assertFragmentIn(response, {"search": {"view": "grid"}})

        # Запрос c флагом market_grid_view_grid_categories_percent. View меняется на grid, так как у "surface 2" у хида выставлен view_type=GRID
        response = self.report.request_json(
            'place=prime&text=surface&rearr-factors=market_grid_view_grid_categories_percent=0.4'
        )
        self.assertFragmentIn(response, {"search": {"view": "grid"}})

        # Запрос c флагом market_grid_view_fashion_categories_percent. View меняется на grid, так как у "alienware 2" хид относится к fashion.
        # Не меняется, так как расскатан market_grid_view_grid_categories_percent и флаг market_grid_view_fashion_categories_percent не срабатывыает
        response = self.report.request_json(
            'place=prime&text=alienware&rearr-factors=market_grid_view_fashion_categories_percent=0.4'
        )
        self.assertFragmentIn(response, {"search": {"view": "list"}})

    @classmethod
    def prepare_filters_without_hids(cls):

        RED, GREEN, BLUE = 1, 2, 3

        cls.index.hypertree += [
            HyperCategory(hid=2964300, view_type=ViewType.GRID),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=296430002,
                hid=2964300,
                gltype=GLType.ENUM,
                values=[RED, GREEN, BLUE],
                xslname="color_glob",
                cluster_filter=True,
            )
        ]

        cls.index.models += [
            Model(hid=2964300, title="baon 1", hyperid=1331),
            Model(hid=2964300, title="baon 2", hyperid=1332),
        ]

        cls.index.offers += [
            Offer(
                hid=2964300,
                hyperid=1331,
                title='baon 1',
                glparams=[
                    GLParam(param_id=296430002, value=RED),
                ],
            ),
            Offer(
                hid=2964300,
                hyperid=1332,
                title='baon 2',
                glparams=[
                    GLParam(param_id=296430002, value=GREEN),
                ],
            ),
        ]

    def test_filters_without_hids(self):
        '''
        MARKETOUT-43278
        Проверяем что под флагом возвращаются фильтры даже если не указан &hid
        '''

        # Запрос без флага. Флаг раскатан
        response = self.report.request_json('place=prime&text=baon&rearr-factors=market_show_filters_without_hid=0')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'filters': NoKey("filters")},
                    ]
                }
            },
        )

        # Запрос с флагом
        response = self.report.request_json(
            'place=prime&text=baon&rearr-factors=market_show_filters_without_hid=1;market_grid_view_grid_categories_percent=0.4'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'filters': [{'xslname': 'color_glob', 'values': [{'value': 'VALUE-1', 'id': '1'}]}]},
                    ]
                }
            },
        )

    def test_hide_express_offers(self):
        response = self.report.request_json('place=prime&text=spoon')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "slug": "silver-spoon",
                        },
                        {
                            "slug": "gold-spoon",
                        },
                        {
                            "slug": "spoon",
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=spoon&filter-offer-type=without_express&debug=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "slug": "spoon",
                        },
                        {
                            "slug": "gold-spoon",
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_remove_doc_from_position(cls):
        cls.index.offers += [
            Offer(title='storm 1', waremd5='n9XlAsFkD2JzjIqQjirk9w', ts=3456),
            Offer(title='storm 2', waremd5='n9XlAsFkD2JzjIqQjirp9w', ts=3457),
            Offer(title='storm 3', waremd5='n9XlAsFkD2JzjIqQjiro9w', ts=3458),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3456).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3457).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3458).respond(0.6)

    def test_remove_doc_from_position(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-44232
        Проверяем, что под флагом remove_doc_from_position из выдачи убирается документ с определенной позиции
        '''

        # под флагом remove_doc_from_position
        request = 'place=prime&text=storm'
        response = self.report.request_json(request + "&rearr-factors=market_remove_doc_from_position=2")

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "storm 1"},
                    },
                    {
                        "titles": {"raw": "storm 3"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log_tskv.not_expect(ware_md5="n9XlAsFkD2JzjIqQjirp9w")

        # без флага remove_doc_from_position
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "storm 1"},
                    },
                    {
                        "titles": {"raw": "storm 2"},
                    },
                    {
                        "titles": {"raw": "storm 3"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log_tskv.expect(ware_md5="n9XlAsFkD2JzjIqQjirp9w")

    @classmethod
    def prepare_elasticity_factor(cls):
        cls.index.mskus += [
            MarketSku(
                title='tornado 0',
                hid=455000,
                sku=465001,
                hyperid=455001,
                blue_offers=[
                    BlueOffer(
                        title="tornado 1",
                        price=1000,
                        feedid=465201,
                        buybox_elasticity=[
                            Elasticity(price_variant=1200, demand_mean=200),
                            Elasticity(price_variant=1200, demand_mean=200),
                        ],
                    ),
                ],
            )
        ]

    def test_elasticity_factor(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-44952
        Проверяем, что считается фактор PREODICTED_ELASTICITY
        '''

        request = 'place=prime&text=tornado&debug=1'
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "PREDICTED_ELASTICITY": "200",
                    }
                }
            },
        )

    def test_rs_with_models_and_skus_returned_models(self):
        modelsAndSkusReturned = [
            (1243, 132),
            (1242, 131),
            (1241, 130),
            (1240, 129),
            (1239, 128),
            (1238, 127),
            (1237, 126),
            (1236, 125),
            (1235, 124),
            (1234, 123),
        ]

        rs = ReportState.create()
        for modelId, msku in modelsAndSkusReturned:
            doc = rs.offer_card_state.also_viewed_docs.add()
            doc.model_id = str(modelId)
            doc.sku_id = str(msku)

        self.assertFragmentIn(
            self.report.request_json(
                'place=prime&text=bock&allow-collapsing=1&use-default-offers=1'
                '&rearr-factors=market_put_models_and_skus_to_rs=1&debug=1'
            ),
            {
                "results": [
                    {
                        "entity": "product",
                        "id": modelId,
                        "offers": {
                            "items": [
                                {"sku": str(msku)},
                            ],
                        },
                    }
                    for modelId, msku in modelsAndSkusReturned
                ],
                "reportState": ReportState.serialize(rs).replace('=', ','),
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_add_models_and_skus_to_rs(self):
        modelsAndSkusReturned = [
            (1243, 132),
            (1242, 131),
            (1241, 130),
            (1240, 129),
            (1239, 128),
            (1238, 127),
            (1237, 126),
            (1236, 125),
            (1235, 124),
            (1234, 123),
        ]

        rs = ReportState.parse("eJwz4ghgrGLheHyKFQAMHwLE")
        for modelId, msku in modelsAndSkusReturned:
            doc = rs.offer_card_state.also_viewed_docs.add()
            doc.model_id = str(modelId)
            doc.sku_id = str(msku)

        self.assertFragmentIn(
            self.report.request_json(
                'place=prime&text=bock&rs=eJwz4ghgrGLheHyKFQAMHwLE&allow-collapsing=1&use-default-offers=1&debug=1'
                '&rearr-factors=market_put_models_and_skus_to_rs=1'
            ),
            {
                "results": [
                    {
                        "entity": "product",
                        "id": modelId,
                        "offers": {
                            "items": [
                                {"sku": str(msku)},
                            ],
                        },
                    }
                    for modelId, msku in modelsAndSkusReturned
                ],
                "reportState": ReportState.serialize(rs).replace('=', ','),
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_rs_with_models_and_skus_returned_models_paging(self):
        modelsAndSkusReturned = [
            (1243, 132),
            (1242, 131),
            (1241, 130),
            (1240, 129),
            (1239, 128),
            (1238, 127),
            (1237, 126),
            (1236, 125),
            (1235, 124),
            (1234, 123),
        ]

        def results_range(start=0, end=None):
            return [
                {
                    "entity": "product",
                    "id": modelId,
                    "offers": {
                        "items": [
                            {"sku": str(msku)},
                        ],
                    },
                }
                for modelId, msku in modelsAndSkusReturned[start:end]
            ]

        rs = ReportState.create()
        for modelId, msku in modelsAndSkusReturned:
            doc = rs.offer_card_state.also_viewed_docs.add()
            doc.model_id = str(modelId)
            doc.sku_id = str(msku)

        request_text = (
            'place=prime&text=bock&allow-collapsing=1&use-default-offers=1&debug=1'
            '&rearr-factors=market_put_models_and_skus_to_rs=1&allow-collapsing=1'
        )
        self.assertFragmentIn(
            self.report.request_json(request_text + '&numdoc=2&page=1'),
            {
                "results": results_range(0, 2),
                "reportState": ReportState.serialize(rs).replace(
                    '=', ','
                ),  # DocCounter iterates all documents to a certain point
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_rs_with_models_and_skus_returned_offers(self):
        modelsAndSkusReturned = [
            (1243, 132),
            (1242, 131),
            (1241, 130),
            (1240, 129),
            (1239, 128),
            (1238, 127),
            (1237, 126),
            (1236, 125),
            (1235, 124),
            (1234, 123),
        ]

        rs = ReportState.create()
        for modelId, msku in modelsAndSkusReturned:
            doc = rs.offer_card_state.also_viewed_docs.add()
            doc.model_id = str(modelId)
            doc.sku_id = str(msku)

        self.assertFragmentIn(
            self.report.request_json('place=prime&text=bock&rearr-factors=market_put_models_and_skus_to_rs=1&debug=1'),
            {
                "results": [
                    {
                        "entity": "offer",
                        "model": {
                            "id": modelId,
                        },
                        "sku": str(msku),
                    }
                    for modelId, msku in modelsAndSkusReturned
                ],
                "reportState": ReportState.serialize(rs).replace('=', ','),
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_business_logos(cls):
        cls.index.shops += [
            Shop(
                fesh=530,
                priority_region=213,
                business_fesh=30,
                business_logos=LogosInfo()
                .set_brand_color("#FACADE")
                .set_shop_group("micro")
                .add_logo_url(
                    logo_type='square',
                    url='//avatars.ru/get-our-namespace/55555/hahaha123456/small',
                    img_width=100,
                    img_height=100,
                ),
            ),
        ]

        cls.index.offers += [Offer(hyperid=309, hid=1, fesh=530)]

    def test_business_logos(self):
        response = self.report.request_json('place=prime&fesh=530')

        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "brandColor": "#FACADE",
                    "shopGroup": "micro",
                    "logos": [
                        {
                            "entity": "picture",
                            "logoType": "square",
                            "original": {
                                "groupId": 55555,
                                "height": 100,
                                "key": "hahaha123456",
                                "namespace": "our-namespace",
                                "width": 100,
                            },
                        }
                    ],
                }
            },
        )

    def test_business_main_logo_set(self):
        for rearr in [
            '&rearr-factors=market_business_logo_name=square',
            '',
        ]:
            response = self.report.request_json('place=prime&fesh=530' + rearr)

            self.assertFragmentIn(
                response,
                {
                    "shop": {
                        "logo": {
                            "entity": "picture",
                            "url": "http://avatars.mdst.yandex.net/get-our-namespace/55555/hahaha123456/orig",
                            "height": 100,
                            "width": 100,
                            "extension": "",
                        },
                    }
                },
            )

    def test_business_main_logo_not_set(self):
        for rearr in [
            '&rearr-factors=market_business_logo_name=non_existent',
            '&rearr-factors=market_business_logo_name=',
        ]:
            response = self.report.request_json('place=prime&fesh=530' + rearr)

            self.assertFragmentIn(
                response,
                {"shop": {"logo": Absent()}},
            )


if __name__ == '__main__':
    main()

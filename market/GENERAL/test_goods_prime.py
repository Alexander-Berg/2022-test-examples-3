#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    AboShopRating,
    BlueOffer,
    ClickType,
    ClothesIndex,
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
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
    Picture,
    PictureSignature,
    Region,
    RegionalDelivery,
    RegionalModel,
    ReportState,
    Shop,
    VCluster,
    Vendor,
    ViewType,
)
from core.matcher import Absent, Contains, Greater, NoKey, NotEmpty
from core.crypta import CryptaFeature, CryptaName
from core.types.picture import thumbnails_config
from core.types.fashion_parameters import FashionCategory

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
        cls.settings.report_subrole = 'goods'
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
                delivery_options=[DeliveryOption(price=100, day_from=32, day_to=32, order_before=24)],
            ),
        ]

        cls.index.offers += [Offer(title='urlless offer', url=None)]

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
                randx=900,
                price=10000,
                picture=pic_1,
            ),
            Offer(
                title='nokia 6',
                bid=80,
                fesh=502,
                hid=8,
                randx=800,
                price=10000,
                picture=pic_2,
            ),
            Offer(
                title='nokia 5',
                bid=70,
                fesh=502,
                hid=8,
                randx=700,
                price=10000,
                picture=pic_3,
            ),
            Offer(
                title='nokia 4',
                bid=95,
                fesh=503,
                hid=8,
                randx=600,
                price=10000,
                picture=pic_1,
            ),
            Offer(
                title='nokia 3',
                bid=85,
                fesh=503,
                hid=8,
                randx=500,
                price=10000,
                picture=pic_2,
            ),
            Offer(
                title='nokia 2',
                bid=75,
                fesh=503,
                hid=8,
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
        response = self.report.request_json('place=prime&text=has_delivery&rids=225')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "prices": {"currency": "RUR", "value": "100", "rawValue": "100"},
            },
            preserve_order=True,
        )
        response = self.report.request_json('place=prime&text=has_delivery&rids=213')
        self.assertFragmentIn(
            response,
            {"entity": "offer", "prices": {"currency": "RUR", "value": "100", "rawValue": "100"}},
            preserve_order=True,
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
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "nokia 7"}},
                    {"titles": {"raw": "nokia 6"}},
                    {"titles": {"raw": "nokia 5"}},
                    {"titles": {"raw": "nokia 4"}},
                    {"titles": {"raw": "nokia 3"}},
                    {"titles": {"raw": "nokia 2"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_autobroker_text(self):
        self.check_nokia('place=prime&text=nokia&rids=300&show-urls=external')

    def test_autobroker_no_text(self):
        self.check_nokia('place=prime&rids=300&show-urls=external&hid=8')

    def test_unique_filter(self):
        response = self.report.request_json('place=prime&text=nexus')
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "nexus 1"}})

        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "nexus 2"}})

        response = self.report.request_json('place=prime&text=nexus&hideduplicate=0')
        self.assertFragmentIn(
            response,
            [{"entity": "offer", "titles": {"raw": "nexus 1"}}, {"entity": "offer", "titles": {"raw": "nexus 2"}}],
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

    def test_mobicard_passed_pp(self):
        self.report.request_json('place=prime&text=wrong-pp&touch=1&phone=1&pp=48&show-urls=phone')
        self.click_log.expect(clicktype=ClickType.PHONE, pp=48)

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

    def test_entities(self):
        '''
        1. Проверяем поле entities.
        2. Проверяем, что не искали в лишних коллекциях
        '''
        response = self.report.request_json('place=prime&text=dress&debug=da')
        self.assertFragmentIn(response, [{'entity': 'product'}] * 2)
        self.assertFragmentIn(response, {'TOTAL_DOCUMENTS_PROCESSED': 6})

        response = self.report.request_json('place=prime&text=dress&entities=product&debug=da')
        self.assertFragmentIn(response, [{'entity': 'product'}] * 2)
        self.assertFragmentIn(response, {'TOTAL_DOCUMENTS_PROCESSED': 2})

        response = self.report.request_json('place=prime&text=vcluster')
        self.assertFragmentIn(response, {'entity': 'product'})

        response = self.report.request_json('place=prime&text=vcluster&entities=product')
        self.assertFragmentIn(response, {'entity': 'product'})

        response = self.report.request_json('place=prime&text=vcluster&entities=offer')
        self.assertFragmentNotIn(response, {'entity': 'product'})

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
    def prepare_rough_exact_match_subtokens(cls):
        cls.index.offers += [Offer(waremd5='EpnWVxDQxj4wg7vVI1ElnA', title='127.0.0.1')]

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


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from collections import namedtuple
from core.types import (
    BlueOffer,
    Contex,
    Currency,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    EntityCtr,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    ModelDescriptionTemplates,
    OfferDimensions,
    Payment,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
    Tax,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.types.contex import create_experiment_mskus
from core.matcher import Absent


FORBIDDEN_MSKU_ID_1 = 1234321
FORBIDDEN_MSKU_ID_2 = FORBIDDEN_MSKU_ID_1 + 1
FORBIDDEN_MSKU_ID_3 = FORBIDDEN_MSKU_ID_1 + 2


class Offers(object):
    sku4_offer1 = BlueOffer(
        feedid=5,
        waremd5='Sku4Offer1-IiLVm1goleg',
        offerid="200031",
        dimensions=OfferDimensions(length=20, width=30, height=10),
        weight=5,
        price=5,
    )
    sku4_offer2 = BlueOffer(
        feedid=6,
        waremd5='Sku4Offer2-IiLVm1goleg',
        offerid="200032",
        dimensions=OfferDimensions(length=20, width=30, height=10),
        weight=5,
    )

    sku6_offer1 = BlueOffer(feedid=7, waremd5='Sku6Offer1-IiLVm1goleg', offerid="200033")

    sku20_offer1 = BlueOffer(price=5, feedid=14, offerid='blue.offer.20.1', waremd5='Sku20Offer1-ILVm1Goleg')
    sku20_offer2 = BlueOffer(price=7, feedid=15, offerid='blue.offer.20.2', waremd5='Sku20Offer2-ILVm1Goleg')
    sku21_offer1 = BlueOffer(price=6, feedid=16, offerid='blue.offer.21.1', waremd5='Sku21Offer1-ILVm1Goleg')

    sku30_offer1 = BlueOffer(price=5, feedid=17, offerid='blue.offer.30.1', waremd5='Sku30Offer1-ILVm1Goleg')
    sku30_offer2 = BlueOffer(price=7, feedid=18, offerid='blue.offer.30.2', waremd5='Sku30Offer2-ILVm1Goleg')
    sku31_offer1 = BlueOffer(price=6, feedid=19, offerid='blue.offer.31.1', waremd5='Sku31Offer1-ILVm1Goleg')

    sku40_offer1 = BlueOffer(price=5, feedid=20, offerid='blue.offer.40.1', waremd5='Sku40Offer1-ILVm1Goleg')
    sku40_offer2 = BlueOffer(price=7, feedid=21, offerid='blue.offer.40.2', waremd5='Sku40Offer2-ILVm1Goleg')
    sku41_offer1 = BlueOffer(price=6, feedid=22, offerid='blue.offer.41.1', waremd5='Sku41Offer1-ILVm1Goleg')

    sku60_offer1 = BlueOffer(
        price=5,
        fesh=50,
        feedid=25,
        offerid='blue.offer.60.1',
        waremd5='Sku60Offer1-ILVm1Goleg',
        delivery_buckets=[1],
    )
    sku62_offer1 = BlueOffer(
        price=5,
        fesh=50,
        feedid=26,
        offerid='blue.offer.60.2',
        waremd5='Sku62Offer1-ILVm1Goleg',
        delivery_buckets=[1],
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rgb_blue_is_cpa = True

        cls.index.regional_models += [
            RegionalModel(hyperid=123, offers=1, price_min=1, price_max=2, rids=[0, 213]),
            RegionalModel(hyperid=124, offers=2, price_min=1, price_max=2, rids=[213]),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=2,
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Какой-то параметр1": "{SomeParamOne}",
                            "Какой-то параметр2": "{SomeParamTwo}",
                        },
                    )
                ],
            )
        ]

        cls.index.gltypes += [
            GLType(
                param_id=401,
                hid=2,
                gltype=GLType.ENUM,
                xslname="SomeParamOne",
                values=[GLValue(value_id=1, text='First')],
            ),
            GLType(
                param_id=402,
                hid=2,
                gltype=GLType.ENUM,
                xslname="SomeParamTwo",
                values=[GLValue(value_id=1, text='Second')],
            ),
            GLType(
                param_id=501,
                hid=2,
                gltype=GLType.ENUM,
                xslname="SomeParamOne_",
                values=[GLValue(value_id=1, text='First')],
            ),
            GLType(
                param_id=502,
                hid=2,
                gltype=GLType.ENUM,
                xslname="SomeParamTwo_",
                values=[GLValue(value_id=1, text='Second')],
            ),
        ]

        cls.index.models += [
            Model(title='red phone', hyperid=122, hid=2, glparams=[GLParam(param_id=401, value=1)]),
            Model(title='green phone', hyperid=123, hid=2, glparams=[GLParam(param_id=401, value=1)], model_clicks=2),
            Model(
                title='blue phone',
                hyperid=124,
                hid=2,
                contex=Contex(parent_id=123, exp_name='new-title'),
                glparams=[GLParam(param_id=402, value=1)],
            ),
        ]

        cls.index.ctr.free.model += [EntityCtr(123, 265, 3)]

        cls.index.hypertree += [HyperCategory(hid=2, name='phones', output_type=HyperCategoryType.GURU)]

        # cls.index.pickup_buckets += [
        #     PickupBucket(bucket_id=5001, dc_bucket_id=4, fesh=1)
        # ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=888,
                dc_bucket_id=8888,
                fesh=1,
                carriers=[157],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=100, pickupBuckets=[5001]),
            DeliveryCalcFeedInfo(feed_id=200, pickupBuckets=[5002]),
        ]

        sku1_offer1 = BlueOffer(offerid='Shop1_sku1', feedid=1)
        sku1_offer2 = BlueOffer(offerid='Shop2_sku1', feedid=2)

        sku2_offer1 = BlueOffer(offerid='Shop1_sku2', feedid=3)
        sku2_offer2 = BlueOffer(offerid='Shop2_sku2', feedid=4)

        msku_red = MarketSku(
            title='offer sku1 red phone',
            feedid=100,
            offerid=100,
            hyperid=122,
            sku=1,
            waremd5='Sku1-wdDXWsIiLVm1goleg',
            blue_offers=[sku1_offer1, sku1_offer2],
            glparams=[
                GLParam(param_id=501, value=1),
            ],
            # pickup_buckets=[5001],
            # post_buckets=[5004],
            randx=1,
        )

        msku_green, msku_blue = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'offer sku2 green phone',
                'hyperid': 123,
                'sku': 2,
                'feedid': 200,
                'offerid': 200,
                'waremd5': 'Sku2-wdDXWsIiLVm1goleg',
                'blue_offers': [sku2_offer1, sku2_offer2],
                'glparams': [
                    GLParam(param_id=501, value=1),
                ],
                # delivery_buckets=[801],
                # pickup_buckets=[5001],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 124,
                'title': 'offer sku2 blue phone',
                'sku': 3,
            },
            exp_name='new-title',
        )

        cls.index.mskus += [msku_red, msku_green, msku_blue]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{'split': 'normal'}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=122, item_count=1000, version='1').respond(
            {'models': ['123']}
        )

        cls.settings.use_external_snippets = False

    def test_wide_query(self):
        """
        Ищем по тексту phone:
        - не в эксперименте -- находим модели red & green
        - в эксперименте -- находим модели red & blue

        У моделей green & blue на выдаче hyperid = 123 (т.е. базовой модели)
        """

        # вне эксперимента
        for glfilter in ("", "&glfilter=401:1", "&glfilter=501:1"):
            response = self.report.request_json(
                'place=prime&text=phone&hid=2&rgb=blue&rearr-factors=contex=1' + glfilter
            )

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'totalModels': 2,
                        'results': [
                            {
                                'titles': {'raw': 'red phone'},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku1 red phone'}},
                                    ]
                                },
                            },
                            {
                                'titles': {'raw': 'green phone'},
                                'id': 123,
                                'filters': [{'id': "401"}],
                                'offers': {
                                    'items': [
                                        {
                                            'titles': {'raw': 'offer sku2 green phone'},
                                            'filters': [{'id': "401"}, {'id': "501"}],
                                        }
                                    ]
                                },
                            },
                        ],
                    }
                },
                allow_different_len=False,
            )

        # в выключенном эксперименте
        for glfilter in ("", "&glfilter=401:1"):
            response = self.report.request_json(
                'place=prime&text=phone&hid=2&rearr-factors=new-title=0;contex=1&rgb=blue' + glfilter
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'totalModels': 2,
                        'results': [
                            {
                                'titles': {'raw': 'red phone'},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku1 red phone'}},
                                    ]
                                },
                            },
                            {
                                'titles': {'raw': 'green phone'},
                                'id': 123,
                                'filters': [{'id': "401"}],
                                'offers': {
                                    'items': [
                                        {
                                            'titles': {'raw': 'offer sku2 green phone'},
                                            'filters': [{'id': "401"}, {'id': "501"}],
                                        }
                                    ]
                                },
                            },
                        ],
                    }
                },
            )

        response = self.report.request_json('place=prime&text=phone&hid=2&rearr-factors=new-title=1;contex=1&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalModels': 2,
                    'results': [
                        {
                            'titles': {'raw': 'red phone'},
                            'offers': {
                                'items': [
                                    {'titles': {'raw': 'offer sku1 red phone'}},
                                ]
                            },
                        },
                        {
                            'titles': {'raw': 'blue phone'},
                            'id': 123,
                            'filters': [{'id': "402"}],
                            'offers': {
                                'items': [
                                    {
                                        'titles': {'raw': 'offer sku2 blue phone'},
                                        'filters': [{'id': "402"}, {'id': "501"}],
                                    }
                                ]
                            },
                        },
                    ],
                }
            },
        )

        # with glfilter
        response = self.report.request_json(
            'place=prime&text=phone&hid=2&rearr-factors=new-title=1;contex=1&rgb=blue&glfilter=402:1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {'raw': 'blue phone'},
                            'id': 123,
                            'filters': [{'id': "402"}],
                            'offers': {
                                'items': [
                                    {
                                        'titles': {'raw': 'offer sku2 blue phone'},
                                        'filters': [{'id': "402"}, {'id': "501"}],
                                    }
                                ]
                            },
                        }
                    ]
                }
            },
        )

    def test_narrow_query(self):
        """
        Ищем по hyperid базовой модели, ожидаем увидеть:
        - в эксперименте -- экспериментальную модель
        - вне эксперимента -- базовую
        """

        # вне эксперимента
        response = self.report.request_json(
            'place=modelinfo&hyperid=123&rids=0&bsformat=2&rgb=blue&rearr-factors=contex=1'
        )
        self.assertFragmentIn(
            response,
            {'total': 1, 'results': [{'titles': {'raw': "green phone"}, 'id': 123, 'filters': [{'id': "401"}]}]},
        )

        # в эксперименте
        response = self.report.request_json(
            'place=modelinfo&hyperid=123&rearr-factors=new-title=1;contex=1&rids=0&bsformat=2&rgb=blue'
        )
        self.assertFragmentIn(
            response,
            {'total': 1, 'results': [{'titles': {'raw': "blue phone"}, 'id': 123, 'filters': [{'id': "402"}]}]},
        )

        response = self.report.request_json(
            'place=modelinfo&hyperid=123&rearr-factors=new-title=1;contex=1&rids=0&bsformat=2&show-models-specs=full'
        )
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'results': [
                    {
                        'titles': {'raw': "blue phone"},
                        'id': 123,
                        'filters': [{'id': "402"}],
                        'specs': {"full": [{"groupSpecs": [{"usedParams": [{"id": 402}]}]}]},
                    }
                ],
            },
        )

    def test_sku_offers(self):
        # msku без экспериментальной
        for exp in ['', ';new-title=1']:
            response = self.report.request_json(
                'place=sku_offers&market-sku=1' '&rearr-factors=contex=1{}&rgb=blue&debug=da'.format(exp)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'id': '1',
                                'entity': 'sku',
                                'titles': {'raw': 'offer sku1 red phone'},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku1 red phone'}},
                                    ]
                                },
                                'product': {'id': 122},
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # вне эксперимента
        for exp in ['', ';new-title=0']:
            response = self.report.request_json(
                'place=sku_offers&market-sku=2' '&rearr-factors=contex=1{}&rgb=blue&debug=da'.format(exp)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'id': '2',
                                'entity': 'sku',
                                'titles': {'raw': 'offer sku2 green phone'},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku2 green phone'}},
                                    ]
                                },
                                'product': {'id': 123},
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # в эксперименте
        response = self.report.request_json(
            'place=sku_offers&market-sku=2' '&rearr-factors=contex=1;new-title=1&rgb=blue&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'id': '2',
                            'entity': 'sku',
                            'titles': {'raw': 'offer sku2 blue phone'},
                            'offers': {
                                'items': [
                                    {'titles': {'raw': 'offer sku2 blue phone'}},
                                ]
                            },
                            'product': {'id': 123},
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_model_wizard(cls):
        cls.index.regiontree += [Region(rid=213)]

        cls.index.models += [
            Model(
                hyperid=106,
                title='pepelac 2000',
                picinfo='//avatars.mds.yandex.net/get-mpic/1235/img_bas_model_pic_id/orig#1000#1000',
            ),
            Model(
                picinfo='//avatars.mds.yandex.net/get-mpic/1236/img_bas_model_pic_id/orig#1000#1000',
                title='pepelac 2000',
                contex=Contex(parent_id=106, exp_name='sp1'),
            ),
        ]

    def test_model_wizard(self):
        # вне эксперимента
        response = self.report.request_bs(
            'place=parallel&text=pepelac+2000&ignore-mn=1&rids=213&rgb=blue&rearr-factors=contex=1'
        )
        self.assertFragmentIn(
            response,
            {'market_model': [{'picture': '//avatars.mds.yandex.net/get-mpic/1235/img_bas_model_pic_id/2hq'}]},
            allow_different_len=False,
        )

        # в эксперименте
        response = self.report.request_bs(
            'place=parallel&text=pepelac+2000&ignore-mn=1&rids=213&rearr-factors=sp1=1;contex=1&rgb=blue'
        )
        self.assertFragmentIn(
            response,
            {'market_model': [{'picture': '//avatars.mds.yandex.net/get-mpic/1236/img_bas_model_pic_id/2hq'}]},
            allow_different_len=False,
        )

    @classmethod
    def prepare_collapsing(cls):
        cls.index.models += [
            Model(title='red tv', hid=3, hyperid=128),
            Model(title='green tv', hid=3, hyperid=129),
            Model(title='blue tv', hid=3, hyperid=130),
            Model(title='red tv exp', hid=3, hyperid=228, contex=Contex(parent_id=128, exp_name="tv")),
            Model(title='green tv exp', hid=3, hyperid=229, contex=Contex(parent_id=129, exp_name="tv")),
            Model(title='blue tv exp', hid=3, hyperid=230, contex=Contex(parent_id=130, exp_name="tv")),
        ]

        red_tv_offer = BlueOffer(offerid='Shop1_sku_tv', feedid=11)
        green_tv_offer = BlueOffer(offerid='Shop1_sku_tv', feedid=12)
        blue_tv_offer = BlueOffer(offerid='Shop1_sku_tv', feedid=13)

        sku_red_tv = MarketSku(
            title='super red tv',
            hyperid=128,
            sku=11,
            feedid=400,
            offerid=400,
            waremd5='Sku2-wdDXWsIiLVm1goleg',
            blue_offers=[red_tv_offer],
            glparams=[GLParam(param_id=501, value=1)],
            randx=2,
            contex=Contex(parent_id=128, exp_name="tv"),
        )

        sku_green_tv = MarketSku(
            title='super red tv',
            hyperid=129,
            sku=12,
            feedid=500,
            offerid=500,
            waremd5='Sku2-wdDXWsIiLVm1goleg',
            blue_offers=[green_tv_offer],
            glparams=[GLParam(param_id=501, value=1)],
            randx=2,
            contex=Contex(parent_id=129, exp_name="tv"),
        )

        sku_blue_tv = MarketSku(
            title='super red tv',
            hyperid=130,
            sku=13,
            feedid=600,
            offerid=600,
            waremd5='Sku2-wdDXWsIiLVm1goleg',
            blue_offers=[blue_tv_offer],
            glparams=[GLParam(param_id=501, value=1)],
            randx=2,
            contex=Contex(parent_id=130, exp_name="tv"),
        )

        cls.index.mskus += [sku_red_tv, sku_green_tv, sku_blue_tv]

    def test_collapsing(self):
        response = self.report.request_json(
            'place=prime&text=super&allow-collapsing=1&rearr-factors=tv=1;contex=1&rgb=blue&rids=0&debug=da'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': "red tv exp"}},
                    {'titles': {'raw': "green tv exp"}},
                    {'titles': {'raw': "blue tv exp"}},
                ]
            },
        )

    @classmethod
    def prepare_place_offer_info(cls):
        cls.index.models += [
            Model(title='some really informative model title', hyperid=200, hid=4),
            Model(title='model title', hyperid=201, hid=4, contex=Contex(parent_id=200, exp_name="brief-title")),
        ]

        msku_no_exp, msku_exp = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'title with all characteristics',
                'hyperid': 200,
                'sku': 4,
                'feedid': 300,
                'offerid': 300,
                'waremd5': 'Sku4-wdDXWsIiLVm1goleg',
                'blue_offers': [Offers.sku4_offer1, Offers.sku4_offer2],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 201,
                'sku': 5,
                'title': 'title',
            },
            exp_name='brief-title',
        )

        cls.index.mskus += [msku_no_exp, msku_exp]

    def test_place_offer_info(self):
        def _do_request(use_exp, offer_id):
            url = 'place=offerinfo&rgb=blue&rids=0&regset=2{}&rearr-factors=brief-title={};contex=1'.format(
                offer_id,
                int(use_exp),
            )
            return self.report.request_json(url)

        def _offer_by_waremd5(offer):
            return 'offerid=' + offer.waremd5

        def _offer_by_feed(offer):
            return 'feed_shoffer_id={}-{}'.format(offer.feedid, offer.offerid)

        expected_offers = {
            0: (Offers.sku4_offer1, 'title with all characteristics'),
            1: (Offers.sku4_offer2, 'title'),
        }

        for experiment_status in [0, 1]:
            expected_offer, expected_title = expected_offers[experiment_status]
            for msku_id in ['&market-sku=4', '']:
                for offer_id in [_offer_by_waremd5(expected_offer), _offer_by_feed(expected_offer)]:
                    offer_id = msku_id + '&' + offer_id
                    response = _do_request(experiment_status, offer_id)
                    self.assertFragmentIn(
                        response,
                        {
                            'search': {
                                'total': 1,
                                'totalOffers': 1,
                                'results': [
                                    {
                                        'titles': {
                                            'raw': expected_title,
                                        },
                                        'wareId': expected_offer.waremd5,
                                        'supplierSku': expected_offer.offerid,
                                    },
                                ],
                            }
                        },
                    )

    def test_model_clicks(self):
        """
        Проверяем, что клики по моделям берутся откуда надо
        """

        # вне эксперимента
        response = self.report.request_json(
            'place=prime&hid=2&rids=0&glfilter=401:1&debug=da&rgb=blue&rearr-factors=contex=1'
        )
        self.assertFragmentIn(response, {'titles': {'raw': "green phone"}})
        self.assertFragmentIn(response, {'MODEL_CLICKS': '265'})

        # в эксперименте
        response = self.report.request_json(
            'place=prime&hid=2&rearr-factors=new-title=1;contex=1&rids=0&glfilter=402:1&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {'titles': {'raw': "blue phone"}})
        self.assertFragmentIn(response, {'MODEL_CLICKS': '265'})

    @classmethod
    def prepare_place_product_offers(cls):
        cls.index.models += [
            Model(title='model title', hyperid=400, hid=5),
            Model(
                title='model title modified', hyperid=401, hid=5, contex=Contex(parent_id=400, exp_name="contex-exp-1")
            ),
        ]

        msku_no_exp, msku_exp = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'title contex-exp-1 NO EXP',
                'hyperid': 400,
                'sku': 6,
                'blue_offers': [Offers.sku6_offer1],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 401,
                'sku': 7,
                'title': 'title contex-exp-1 WITH EXP',
            },
            exp_name='contex-exp-1',
        )

        cls.index.mskus += [msku_no_exp, msku_exp]

    def test_place_product_offers(self):
        def _do_request(use_exp, hyper_id):
            url = 'place=productoffers&rgb=blue&rids=0&hid=5&hyperid={}&rearr-factors=contex-exp-1={};contex=1'.format(
                hyper_id,
                int(use_exp),
            )
            return self.report.request_json(url)

        def _check_response_not_empty(response, title):
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'totalOffers': 1,
                        'results': [
                            {
                                'entity': 'offer',
                                'titles': {
                                    'raw': title,
                                },
                                'model': {
                                    'id': 400,
                                },
                            },
                        ],
                    }
                },
            )

        response = _do_request(0, 400)
        _check_response_not_empty(response, 'title contex-exp-1 NO EXP')

        response = _do_request(1, 400)
        _check_response_not_empty(response, 'title contex-exp-1 WITH EXP')

    def test_actual_delivery_one_offer(self):
        request = "place=actual_delivery&rids=0&debug=1&rearr-factors=contex=1&combinator=0&offers-list={}"
        response = self.report.request_json(request.format('Sku4Offer1-IiLVm1goleg:1'))
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "5",
                "dimensions": ["30", "10", "20"],
                "offers": [
                    {"wareId": "Sku4Offer1-IiLVm1goleg"},
                ],
            },
        )

        response = self.report.request_json(request.format('Sku4Offer2-IiLVm1goleg:1&rearr-factors=new-title=1'))
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "5",
                "dimensions": ["30", "10", "20"],
                "offers": [
                    {"wareId": "Sku4Offer2-IiLVm1goleg"},
                ],
            },
        )

    def test_place_also_viewed(self):

        # вне эксперимента
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&cpa=real&hyperid=122&rearr-factors=split=normal;contex=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {'titles': {'raw': 'green phone'}},
                    ],
                }
            },
        )

        # в эксперименте
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&cpa=real&hyperid=122&rearr-factors=split=normal;contex=1;new-title=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {'titles': {'raw': 'blue phone'}},
                    ],
                }
            },
        )

    @classmethod
    def prepare_sku_offers_jump_table(cls):
        HID = 324

        cls.index.gltypes += [
            GLType(param_id=600, hid=HID, gltype=GLType.ENUM, values=[1, 2]),  # second kind param
            GLType(
                param_id=601, hid=HID, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=3, values=[10, 11]
            ),
            GLType(
                param_id=602, hid=HID, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=3, values=[12, 13]
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=500,
                hid=HID,
                title='original model',
                glparams=[
                    GLParam(param_id=600, value=1),
                ],
            ),
            Model(
                hyperid=501,
                hid=HID,
                title='experimental model',
                glparams=[
                    GLParam(param_id=600, value=2),
                ],
                contex=Contex(parent_id=500, exp_name='jump-exp-1'),
            ),
        ]

        msku_no_exp, msku_exp = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'title jump-exp-1 NO EXP',
                'hyperid': 500,
                'sku': 20,
                'blue_offers': [Offers.sku20_offer1, Offers.sku20_offer2],
                'glparams': [
                    GLParam(param_id=601, value=10),
                    GLParam(param_id=602, value=12),
                ],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 501,
                'sku': FORBIDDEN_MSKU_ID_1,
                'title': 'title jump-exp-1 WITH EXP',
                'glparams': [
                    GLParam(param_id=601, value=11),
                    GLParam(param_id=602, value=13),
                ],
            },
            exp_name='jump-exp-1',
        )

        msku_green = MarketSku(
            title='blue offer sku21',
            hyperid=500,
            sku=21,
            blue_offers=[Offers.sku21_offer1],
            glparams=[
                GLParam(param_id=601, value=11),
                GLParam(param_id=602, value=12),
            ],
            randx=2,
        )

        cls.index.mskus += [
            msku_no_exp,
            msku_exp,
            msku_green,
        ]

    def test_sku_offers_jump_table(self):
        """
        Проверяем, что переходы для тестовых MSKU отрабатывают корректно.
        На выдаче не должны появиться тестовые msku_id.
        """

        def _do_request(exp_status, pipeline):
            url = 'place=sku_offers&market-sku=20&show-urls=direct&rgb=blue&rearr-factors=contex=1;jump-exp-1={};use_new_jump_table_pipeline={}'.format(
                exp_status, pipeline
            )
            return self.report.request_json(url)

        # Проверяем оба пайплайна -- на старый, судя по коду репорта, может случиться фоллбек (теоретически)
        for pipeline in [0, 1]:
            # Вне эксперимента
            response = _do_request(0, pipeline)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '20',
                            'titles': {'raw': 'title jump-exp-1 NO EXP'},
                            'filters': [
                                {
                                    'id': '601',
                                    'values': [
                                        {
                                            'id': '10',  # Если показываются переходы, то в поле 'id' указывается значение варьируемого параметра
                                            'marketSku': '20',
                                            'fuzzy': Absent(),
                                            'checked': True,
                                            'slug': 'title-jump-exp-1-no-exp',
                                        },
                                        {
                                            'id': '11',
                                            'marketSku': '21',
                                            'fuzzy': Absent(),
                                            'checked': Absent(),
                                            'slug': 'blue-offer-sku21',
                                        },
                                    ],
                                }
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )
            self.assertFragmentNotIn(response, {'marketSku': str(FORBIDDEN_MSKU_ID_1)})
            self.assertFragmentNotIn(response, {'msku': str(FORBIDDEN_MSKU_ID_1)})
            self.assertFragmentNotIn(response, {'filters': [{'id': 600}]})

            # В эксперименте
            response = _do_request(1, pipeline)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '20',
                            'titles': {'raw': 'title jump-exp-1 WITH EXP'},
                            'filters': [
                                {
                                    'id': '602',
                                    'values': [
                                        {
                                            'id': '12',
                                            'marketSku': '21',
                                            'fuzzy': Absent(),
                                            'checked': Absent(),
                                            'slug': 'blue-offer-sku21',
                                        },
                                        {
                                            'id': '13',
                                            'marketSku': '20',
                                            'fuzzy': Absent(),
                                            'checked': True,
                                            'slug': 'title-jump-exp-1-with-exp',
                                        },
                                    ],
                                }
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )
            self.assertFragmentNotIn(response, {'marketSku': str(FORBIDDEN_MSKU_ID_1)})
            self.assertFragmentNotIn(response, {'msku': str(FORBIDDEN_MSKU_ID_1)})
            self.assertFragmentNotIn(response, {'filters': [{'id': 600}]})

    @classmethod
    def prepare_sku_offers_jump_table_modifications(cls):
        HID = 325

        cls.index.gltypes += [
            GLType(param_id=605, hid=HID, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=3, values=[1, 2]),
            GLType(param_id=606, hid=HID, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=3, values=[3, 4]),
            GLType(param_id=607, hid=HID, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=3, values=[5, 6]),
        ]

        cls.index.models += [
            Model(
                hyperid=505,
                hid=HID,
                title='original model',
            ),
            Model(
                hyperid=506,
                hid=HID,
                title='experimental model',
                contex=Contex(parent_id=505, exp_name='jump-exp-2'),
            ),
        ]

        msku_no_exp, msku_exp = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'title jump-exp-2 NO EXP',
                'hyperid': 505,
                'sku': 30,
                'blue_offers': [Offers.sku30_offer1, Offers.sku30_offer2],
                'glparams': [
                    GLParam(param_id=605, value=1),
                    GLParam(param_id=606, value=3),
                ],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 506,
                'sku': FORBIDDEN_MSKU_ID_2,
                'title': 'title jump-exp-2 WITH EXP',
                'glparams': [
                    GLParam(param_id=605, value=1),
                    GLParam(param_id=606, value=3),
                    GLParam(param_id=607, value=5),  # В эксперименте jump-exp-2 для MSKU был добавлен новый параметр
                ],
            },
            exp_name='jump-exp-2',
        )

        msku_green = MarketSku(
            title='blue offer sku31',
            hyperid=505,
            sku=31,
            blue_offers=[Offers.sku31_offer1],
            glparams=[
                GLParam(param_id=605, value=1),
                GLParam(param_id=606, value=3),
            ],
            randx=2,
        )

        cls.index.mskus += [
            msku_no_exp,
            msku_exp,
            msku_green,
        ]

    def test_sku_offers_jump_table_modifications(self):
        """
        Проверяем, что модификации для тестовых MSKU выводятся корректно.
        На выдаче не должны появиться тестовые msku_id.
        """

        def _do_request(exp_status):
            url = (
                'place=sku_offers&market-sku=30&show-urls=direct&rgb=blue&rearr-factors=contex=1;jump-exp-2={}'.format(
                    exp_status
                )
            )
            return self.report.request_json(url)

        # Вне эксперимента
        response = _do_request(0)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': '30',
                        'titles': {'raw': 'title jump-exp-2 NO EXP'},
                        'filters': [
                            {
                                'id': 'modifications',
                                'valuesCount': 2,
                                'values': [
                                    {
                                        'id': '30',  # Если показываются модицикации, то в поле 'id' указывается id соответствующего MSKU
                                        'marketSku': '30',
                                        'fuzzy': Absent(),
                                        'checked': True,
                                        'slug': 'title-jump-exp-2-no-exp',
                                    },
                                    {
                                        'id': '31',
                                        'marketSku': '31',
                                        'fuzzy': Absent(),
                                        'checked': Absent(),
                                        'slug': 'blue-offer-sku31',
                                    },
                                ],
                            }
                        ],
                        'offers': {
                            'items': [
                                {
                                    'filters': [
                                        {'id': '605'},
                                        {'id': '606'},
                                    ],
                                }
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False
            # Для модификаций никакой порядок на выдаче не гарантируется
        )
        self.assertFragmentNotIn(response, {'marketSku': str(FORBIDDEN_MSKU_ID_2)})
        self.assertFragmentNotIn(response, {'msku': str(FORBIDDEN_MSKU_ID_2)})

        # В эксперименте
        response = _do_request(1)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': '30',
                        'titles': {'raw': 'title jump-exp-2 WITH EXP'},
                        'filters': [
                            {
                                'id': 'modifications',
                                'valuesCount': 2,
                                'values': [
                                    {
                                        'id': '30',
                                        'marketSku': '30',
                                        'fuzzy': Absent(),
                                        'checked': True,
                                        'slug': 'title-jump-exp-2-with-exp',
                                    },
                                    {
                                        'id': '31',
                                        'marketSku': '31',
                                        'fuzzy': Absent(),
                                        'checked': Absent(),
                                        'slug': 'blue-offer-sku31',
                                    },
                                ],
                            }
                        ],
                        'offers': {
                            'items': [
                                {
                                    'filters': [
                                        # Проверяем, что добавленный фильтр появился на выдаче
                                        {'id': '605'},
                                        {'id': '606'},
                                        {'id': '607'},
                                    ],
                                }
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, {'marketSku': str(FORBIDDEN_MSKU_ID_2)})
        self.assertFragmentNotIn(response, {'msku': str(FORBIDDEN_MSKU_ID_2)})

    @classmethod
    def prepare_sku_offers_jump_table_added_params(cls):
        HID = 326

        cls.index.gltypes += [
            GLType(param_id=610, hid=HID, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=3, values=[1, 2]),
            GLType(param_id=611, hid=HID, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=3, values=[3, 4]),
            GLType(param_id=612, hid=HID, gltype=GLType.ENUM, cluster_filter=True, model_filter_index=3, values=[5, 6]),
        ]

        cls.index.models += [
            Model(
                hyperid=510,
                hid=HID,
                title='original model',
            ),
            Model(
                hyperid=511,
                hid=HID,
                title='experimental model',
                contex=Contex(parent_id=510, exp_name='jump-exp-3'),
            ),
        ]

        msku_no_exp, msku_exp = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'title jump-exp-3 NO EXP',
                'hyperid': 510,
                'sku': 40,
                'blue_offers': [Offers.sku40_offer1, Offers.sku40_offer2],
                'glparams': [
                    GLParam(param_id=610, value=2),
                    GLParam(param_id=611, value=3),
                ],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 511,
                'sku': FORBIDDEN_MSKU_ID_3,
                'title': 'title jump-exp-3 WITH EXP',
                'glparams': [
                    GLParam(param_id=610, value=2),
                    GLParam(param_id=611, value=3),
                    # Добавился новый параметр:
                    GLParam(param_id=612, value=5),
                ],
            },
            exp_name='jump-exp-3',
        )

        msku_green = MarketSku(
            title='blue offer sku41',
            hyperid=510,
            sku=41,
            blue_offers=[Offers.sku41_offer1],
            glparams=[
                GLParam(param_id=610, value=2),
                GLParam(param_id=611, value=3),
                GLParam(param_id=612, value=6),
            ],
            randx=2,
        )

        cls.index.mskus += [
            msku_no_exp,
            msku_exp,
            msku_green,
        ]

    def test_sku_offers_jump_table_2(self):
        """
        Особенность записей в jump_table состоит в том, что для моделей, у которых есть экспериментальные MSKU,
        все данные по экспериментальным MSKU записываются по ключу оригинальной модели, в одну кучу с данными по
        обычным MSKU, а записей по ключу экспериментальной модели вообще нет

        В тесте проверяем, что если эксперимент состоит в том, что для эксповой MSKU добавляется новый параметр,
        то это никак не ломает выдачу для оригинальной MSKU, даже с учетом того, что данные про эксповый MSKU
        были записаны по ключу оригинальной модели
        """

        def _do_request(exp_status, pipeline):
            url = 'place=sku_offers&market-sku=40&show-urls=direct&rgb=blue&rearr-factors=contex=1;jump-exp-3={};use_new_jump_table_pipeline={}'.format(
                exp_status, pipeline
            )
            return self.report.request_json(url)

        # Старый пайплайн в модификации не умеет, поэтому ожидаем, что список фильтров пуст
        response = _do_request(0, 0)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': '40',
                        'titles': {'raw': 'title jump-exp-3 NO EXP'},
                        'filters': [],
                    }
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, {'marketSku': str(FORBIDDEN_MSKU_ID_3)})
        self.assertFragmentNotIn(response, {'msku': str(FORBIDDEN_MSKU_ID_3)})

        # В случае нового пайплайна и отсутствия экспа, MSKU показываются как модификации
        # Уж такова логика -- все, что пришло из индекса, будет записано в модификации, если для таблицы переходов нет данных
        # (см. методы TJumpTableBuilder::ModificationsSet и TJumpTableBuilder::HandleTrivialCase)
        response = _do_request(0, 1)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': '40',
                        'titles': {'raw': 'title jump-exp-3 NO EXP'},
                        'filters': [
                            {
                                'id': 'modifications',
                                'valuesCount': 2,
                                'values': [
                                    {
                                        'id': '40',
                                        'marketSku': '40',
                                        'fuzzy': Absent(),
                                        'checked': True,
                                        'slug': 'title-jump-exp-3-no-exp',
                                    },
                                    {
                                        'id': '41',
                                        'marketSku': '41',
                                        'fuzzy': Absent(),
                                        'checked': Absent(),
                                        'slug': 'blue-offer-sku41',
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, {'marketSku': str(FORBIDDEN_MSKU_ID_3)})
        self.assertFragmentNotIn(response, {'msku': str(FORBIDDEN_MSKU_ID_3)})

        for pipeline in [0, 1]:
            # В эксперименте
            response = _do_request(1, pipeline)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '40',
                            'titles': {'raw': 'title jump-exp-3 WITH EXP'},
                            'filters': [
                                {
                                    'id': '612',
                                    'values': [
                                        {
                                            'id': '5',
                                            'marketSku': '40',
                                            'fuzzy': Absent(),
                                            'checked': True,
                                            'slug': 'title-jump-exp-3-with-exp',
                                        },
                                        {
                                            'id': '6',
                                            'marketSku': '41',
                                            'fuzzy': Absent(),
                                            'checked': Absent(),
                                            'slug': 'blue-offer-sku41',
                                        },
                                    ],
                                }
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )
            self.assertFragmentNotIn(response, {'marketSku': str(FORBIDDEN_MSKU_ID_3)})
            self.assertFragmentNotIn(response, {'msku': str(FORBIDDEN_MSKU_ID_3)})

    @classmethod
    def prepare_simultaneous_experiments(cls):
        HID = 250

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                dc_bucket_id=1,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=50)])],
                carriers=[1],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=50,
                datafeed_id=2,
                priority_region=213,
                regions=[213],
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.models += [
            Model(hyperid=550, hid=HID, title='абырвалг обыкновенный'),
            Model(
                hyperid=551,
                hid=HID,
                title='абырвалг обыкновенный УЛУЧШЕННЫЙ',
                contex=Contex(parent_id=550, exp_name='sim-exp-1'),
            ),
            Model(hyperid=552, hid=HID, title='абырвалг особенный'),
            Model(
                hyperid=553,
                hid=HID,
                title='абырвалг особенный УЛУЧШЕННЫЙ',
                contex=Contex(parent_id=552, exp_name='sim-exp-2'),
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=550, offers=2, rids=[213]),
            RegionalModel(hyperid=551, offers=2, rids=[213]),
            RegionalModel(hyperid=552, offers=2, rids=[213]),
            RegionalModel(hyperid=553, offers=2, rids=[213]),
        ]

        msku_no_exp_1, msku_exp_1 = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'абырвалг обыкновенный NO EXP',
                'hyperid': 550,
                'sku': 60,
                'blue_offers': [Offers.sku60_offer1],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 551,
                'sku': 61,
                'title': 'абырвалг обыкновенный УЛУЧШЕННЫЙ WITH EXP',
            },
            exp_name='sim-exp-1',
        )

        msku_no_exp_2, msku_exp_2 = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'абырвалг особенный NO EXP',
                'hyperid': 552,
                'sku': 62,
                'blue_offers': [Offers.sku62_offer1],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 553,
                'sku': 63,
                'title': 'абырвалг особенный УЛУЧШЕННЫЙ WITH EXP',
            },
            exp_name='sim-exp-2',
        )

        cls.index.mskus += [msku_no_exp_1, msku_exp_1, msku_no_exp_2, msku_exp_2]

    def test_simultaneous_experiments(self):
        """
        Проверяем, что разные эксперименты не зависят друг от друга, т.е. включая один эксп и выключая другой,
        мы не скрываем офферы и модели с литералом contex=classic у неактивного экспа.
        """
        ModelWithMsku = namedtuple('ModelWithMsku', ['model_id', 'model_title', 'msku_id', 'msku_title'])

        def _do_request(exp_1_status, exp_2_status):
            rearr_factors = 'rearr-factors=contex=1;sim-exp-1={};sim-exp-2={}'.format(exp_1_status, exp_2_status)
            return self.report.request_json('place=prime&text=абырвалг&rids=213&hid=250&rgb=blue&' + rearr_factors)

        def _render_model(model):
            return {
                'titles': {'raw': model.model_title},
                'id': model.model_id,
                'offers': {
                    'items': [
                        {
                            'titles': {'raw': model.msku_title},
                            'marketSku': str(model.msku_id),
                        }
                    ]
                },
            }

        def _expect_result(model_1, model_2):
            return {
                'search': {
                    'totalModels': 2,
                    'totalOffers': 0,
                    'results': [_render_model(model_1), _render_model(model_2)],
                }
            }

        # Оба экспа неактивны
        response = _do_request(0, 0)
        self.assertFragmentIn(
            response,
            _expect_result(
                ModelWithMsku(
                    model_id=550,
                    model_title='абырвалг обыкновенный',
                    msku_id=60,
                    msku_title='абырвалг обыкновенный NO EXP',
                ),
                ModelWithMsku(
                    model_id=552, model_title='абырвалг особенный', msku_id=62, msku_title='абырвалг особенный NO EXP'
                ),
            ),
            allow_different_len=False,
        )

        # Активен только эксп sim-exp-1
        response = _do_request(1, 0)
        self.assertFragmentIn(
            response,
            _expect_result(
                ModelWithMsku(
                    model_id=550,
                    model_title='абырвалг обыкновенный УЛУЧШЕННЫЙ',
                    msku_id=60,
                    msku_title='абырвалг обыкновенный УЛУЧШЕННЫЙ WITH EXP',
                ),
                ModelWithMsku(
                    model_id=552, model_title='абырвалг особенный', msku_id=62, msku_title='абырвалг особенный NO EXP'
                ),
            ),
            allow_different_len=False,
        )

        # Активен только эксп sim-exp-2
        response = _do_request(0, 1)
        self.assertFragmentIn(
            response,
            _expect_result(
                ModelWithMsku(
                    model_id=550,
                    model_title='абырвалг обыкновенный',
                    msku_id=60,
                    msku_title='абырвалг обыкновенный NO EXP',
                ),
                ModelWithMsku(
                    model_id=552,
                    model_title='абырвалг особенный УЛУЧШЕННЫЙ',
                    msku_id=62,
                    msku_title='абырвалг особенный УЛУЧШЕННЫЙ WITH EXP',
                ),
            ),
            allow_different_len=False,
        )

        # Активны оба экспа: sim-exp-1 и sim-exp-2
        response = _do_request(1, 1)
        self.assertFragmentIn(
            response,
            _expect_result(
                ModelWithMsku(
                    model_id=550,
                    model_title='абырвалг обыкновенный УЛУЧШЕННЫЙ',
                    msku_id=60,
                    msku_title='абырвалг обыкновенный УЛУЧШЕННЫЙ WITH EXP',
                ),
                ModelWithMsku(
                    model_id=552,
                    model_title='абырвалг особенный УЛУЧШЕННЫЙ',
                    msku_id=62,
                    msku_title='абырвалг особенный УЛУЧШЕННЫЙ WITH EXP',
                ),
            ),
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

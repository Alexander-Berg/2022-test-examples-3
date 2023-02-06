#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Contex,
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
    RegionalDelivery,
    RegionalModel,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.types.contex import create_experiment_mskus
from core.matcher import Absent, ElementCount


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['contex_multiply_models=1']
        cls.settings.rgb_blue_is_cpa = True

        cls.index.regional_models += [
            RegionalModel(hyperid=123, offers=7, price_min=1, price_max=3, rids=[0, 213]),
            RegionalModel(hyperid=124, offers=2, price_min=1, price_max=2, rids=[213]),
            RegionalModel(hyperid=125, offers=2, price_min=2, price_max=2, rids=[213]),
            RegionalModel(hyperid=126, offers=2, price_min=2, price_max=3, rids=[213]),
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
                title='iphone',
                hyperid=133,
                hid=2,
                glparams=[GLParam(param_id=401, value=4), GLParam(param_id=403, value=1)],
                model_clicks=3,
            ),
            Model(
                title='blue phone',
                hyperid=124,
                hid=2,
                contex=Contex(parent_id=123, exp_name='new-title'),
                glparams=[GLParam(param_id=402, value=1)],
            ),
            Model(
                title='blue phone',
                hyperid=125,
                hid=2,
                contex=Contex(parent_id=123, exp_name='new-title'),
                glparams=[GLParam(param_id=402, value=1)],
            ),
            Model(
                title='blue phone',
                hyperid=126,
                hid=2,
                contex=Contex(parent_id=123, exp_name='new-title'),
                glparams=[GLParam(param_id=402, value=1)],
            ),
            Model(
                title='iphone new',
                hyperid=134,
                hid=2,
                contex=Contex(parent_id=133, exp_name='new-title'),
                glparams=[GLParam(param_id=401, value=4), GLParam(param_id=403, value=2)],
            ),
            Model(
                title='iphone new',
                hyperid=135,
                hid=2,
                contex=Contex(parent_id=133, exp_name='new-title'),
                glparams=[GLParam(param_id=401, value=4), GLParam(param_id=403, value=2)],
            ),
        ]

        cls.index.ctr.free.model += [EntityCtr(123, 265, 3), EntityCtr(133, 365, 3)]

        cls.index.hypertree += [HyperCategory(hid=2, name='phones', output_type=HyperCategoryType.GURU)]

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

        cls.index.mskus += [msku_red]

        for hyperid in [124, 125, 126]:
            msku_green, msku_blue = create_experiment_mskus(
                base_class=MarketSku,
                offer_kwargs={
                    'title': 'offer sku2 green phone',
                    'hyperid': 123,
                    'sku': 2000 + hyperid,
                    'feedid': 200,
                    'offerid': 200,
                    'waremd5': 'Sku2-wdDXWsIiLVm1g{}g'.format(hyperid),
                    'blue_offers': [
                        BlueOffer(
                            offerid='Shop1_sku{}'.format(hyperid),
                            feedid=1000 + hyperid,
                            waremd5='Sku2-wdDXWsIiLVm1g{}g'.format(hyperid),
                            dimensions=OfferDimensions(length=20, width=30, height=10),
                            price=1,
                            weight=5,
                        ),
                        BlueOffer(
                            offerid='Shop2_sku{}'.format(hyperid),
                            feedid=2000 + hyperid,
                            dimensions=OfferDimensions(length=20, width=30, height=10),
                            weight=5,
                            price=2,
                        ),
                    ],
                    'glparams': [
                        GLParam(param_id=501, value=1),
                    ],
                    # delivery_buckets=[801],
                    # pickup_buckets=[5001],
                    'randx': 2,
                },
                offer_exp_kwargs={
                    'hyperid': hyperid,
                    'blue_offers': [
                        BlueOffer(
                            offerid='Shop1_sku{}_exp'.format(hyperid),
                            feedid=1000 + hyperid,
                            waremd5='Sku2-wdDXWsIiLVm1g{}g'.format(hyperid),
                            weight=5,
                            dimensions=OfferDimensions(length=hyperid, width=30, height=10),
                            price=5,
                        ),
                        BlueOffer(
                            offerid='Shop2_sku{}_exp'.format(hyperid),
                            dimensions=OfferDimensions(length=20, width=30, height=10),
                            feedid=2000 + hyperid,
                            weight=5,
                            price=6,
                        ),
                    ],
                    'title': 'offer sku2 blue phone {}'.format(hyperid),
                    'sku': 3000 + hyperid,
                },
                exp_name='new-title',
            )
            cls.index.mskus += [msku_green, msku_blue]

        for hyperid in [134, 135]:
            msku_green, msku_blue = create_experiment_mskus(
                base_class=MarketSku,
                offer_kwargs={
                    'title': 'offer iphone',
                    'hyperid': 133,
                    'sku': 2000 + hyperid,
                    'feedid': 200,
                    'offerid': 200,
                    'waremd5': 'Sku2-wdDXWsIiLVm1po' + str(hyperid),
                    'blue_offers': [
                        BlueOffer(offerid='Shop1_sku{}'.format(hyperid), feedid=1000 + hyperid),
                        BlueOffer(offerid='Shop2_sku{}'.format(hyperid), feedid=2000 + hyperid),
                    ],
                    'glparams': [
                        GLParam(param_id=501, value=1),
                    ],
                    # delivery_buckets=[801],
                    # pickup_buckets=[5001],
                    'randx': 2,
                },
                offer_exp_kwargs={
                    'hyperid': hyperid,
                    'blue_offers': [
                        BlueOffer(offerid='Shop1_sku{}_exp'.format(hyperid), feedid=1000 + hyperid, price=5),
                        BlueOffer(offerid='Shop2_sku{}_exp'.format(hyperid), feedid=2000 + hyperid, price=6),
                    ],
                    'title': 'offer iphone {0}'.format(hyperid),
                    'sku': 3000 + hyperid,
                },
                exp_name='new-title',
            )
            cls.index.mskus += [msku_green, msku_blue]

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
                                'entity': 'product',
                                'titles': {'raw': 'red phone'},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku1 red phone'}},
                                    ]
                                },
                            },
                            {
                                'titles': {'raw': 'green phone'},
                                'entity': 'product',
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
                                'entity': 'product',
                                'titles': {'raw': 'red phone'},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku1 red phone'}},
                                    ]
                                },
                            },
                            {
                                'titles': {'raw': 'green phone'},
                                'entity': 'product',
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

        response = self.report.request_json(
            'place=prime&text=phone iphone&hid=2&rearr-factors=new-title=1;contex=1&rgb=blue'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalModels': 6,
                    'results': [
                        {
                            'entity': 'product',
                            'titles': {'raw': 'red phone'},
                            'offers': {
                                'items': [
                                    {'titles': {'raw': 'offer sku1 red phone'}},
                                ]
                            },
                        }
                    ],
                }
            },
        )

        for hyperid in [124, 125, 126]:
            self.assertFragmentIn(
                response,
                {
                    'id': 123,
                    'entity': 'product',
                    'filters': [{'id': "402"}],
                    'offers': {
                        'items': [
                            {
                                'titles': {'raw': 'offer sku2 blue phone {0}'.format(hyperid)},
                                'filters': [{'id': "402"}, {'id': "501"}],
                            }
                        ]
                    },
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
                            'id': 123,
                            'entity': 'product',
                            'filters': [{'id': "402"}],
                            'offers': {
                                'items': [
                                    {
                                        'titles': {'raw': 'offer sku2 blue phone {0}'.format(hyperid)},
                                        'filters': [{'id': "402"}, {'id': "501"}],
                                    }
                                ]
                            },
                        }
                        for hyperid in [124, 125, 126]
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
            {
                'total': 1,
                'results': [
                    {'titles': {'raw': "green phone"}, 'entity': 'product', 'id': 123, 'filters': [{'id': "401"}]}
                ],
            },
        )

        # в эксперименте
        response = self.report.request_json(
            'place=modelinfo&hyperid=123&rearr-factors=new-title=1;contex=1&rids=0&bsformat=2&rgb=blue'
        )
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'results': [
                    {'id': 123, 'entity': 'product', 'titles': {'raw': "blue phone"}, 'filters': [{'id': "402"}]}
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

        for hyper in [124, 125, 126]:
            # вне эксперимента
            for exp in ['', ';new-title=0']:
                response = self.report.request_json(
                    'place=sku_offers&market-sku=2{0}' '&rearr-factors=contex=1{1}&rgb=blue&debug=da'.format(hyper, exp)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'results': [
                                {
                                    'id': '2{0}'.format(hyper),
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
                'place=sku_offers&market-sku=2{}' '&rearr-factors=contex=1;new-title=1&rgb=blue&debug=da'.format(hyper)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'id': '2{0}'.format(hyper),
                                'entity': 'sku',
                                'titles': {'raw': 'offer sku2 blue phone {0}'.format(hyper)},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku2 blue phone {0}'.format(hyper)}},
                                    ]
                                },
                                'product': {'id': 123},
                            }
                        ]
                    }
                },
                allow_different_len=False,
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
        self.assertFragmentIn(response, {'MODEL_CLICKS': '265'})

    def test_actual_delivery_one_offer(self):
        request = (
            "place=actual_delivery&rids=0&debug=da&offers-list=Sku2-wdDXWsIiLVm1g124g:1"
            "&combinator=0&rearr-factors=contex=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "5",
                "dimensions": ["10", "20", "30"],
                "offers": [{"wareId": "Sku2-wdDXWsIiLVm1g124g"}],
            },
        )

        response = self.report.request_json(request + ";new-title=1")
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "5",
                "dimensions": ["10", "30", "124"],
                "offers": [
                    {"wareId": "Sku2-wdDXWsIiLVm1g124g"},
                ],
            },
        )

    def test_place_also_viewed(self):

        # вне эксперимента
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&hyperid=122&rearr-factors=split=normal;contex=1'
        )
        self.assertFragmentIn(response, {'search': {'total': 1, 'results': [{'titles': {'raw': 'green phone'}}]}})

        # в эксперименте
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&hyperid=122&rearr-factors=split=normal;contex=1;new-title=1'
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
            GLType(param_id=600, hid=HID, gltype=GLType.ENUM, values=[1, 2, 3]),  # second kind param
            GLType(
                param_id=601,
                hid=HID,
                gltype=GLType.ENUM,
                cluster_filter=True,
                model_filter_index=3,
                values=[10, 11, 12],
            ),
            GLType(
                param_id=602,
                hid=HID,
                gltype=GLType.ENUM,
                cluster_filter=True,
                model_filter_index=3,
                values=[20, 21, 22, 23, 24, 25, 26],
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
                title='experimental model1',
                glparams=[
                    GLParam(param_id=600, value=1),
                    GLParam(param_id=601, value=11),
                ],
                contex=Contex(parent_id=500, exp_name='jump-exp-1'),
            ),
            Model(
                hyperid=502,
                hid=HID,
                title='experimental model2',
                glparams=[
                    GLParam(param_id=600, value=1),
                    GLParam(param_id=601, value=12),
                ],
                contex=Contex(parent_id=500, exp_name='jump-exp-1'),
            ),
        ]

        for hyperid in [501, 502]:
            for variant in [0, 1]:
                msku_no_exp, msku_exp = create_experiment_mskus(
                    base_class=MarketSku,
                    offer_kwargs={
                        'title': 'title {}_{} NO EXP'.format(hyperid, variant),
                        'hyperid': 500,
                        'sku': 20000 + variant * 1000 + hyperid,
                        'blue_offers': [
                            BlueOffer(offerid='Jump_sku{}_{}'.format(hyperid, variant), feedid=2000 + hyperid, price=6)
                        ],
                        'glparams': [
                            GLParam(param_id=601, value=10 + variant),
                            GLParam(param_id=602, value=20 + variant + (hyperid - 501) * 2),
                        ],
                        'randx': 2,
                    },
                    offer_exp_kwargs={
                        'hyperid': hyperid,
                        'sku': 30000 + variant * 1000 + hyperid,
                        'title': 'title {}_{} WITH EXP'.format(hyperid, variant),
                        'blue_offers': [
                            BlueOffer(
                                offerid='Jump_sku{}_{}_exp'.format(hyperid, variant), feedid=2000 + hyperid, price=6
                            )
                        ],
                        'glparams': [
                            GLParam(param_id=601, value=(hyperid - 501 + 11)),
                            # this parameter is fixed for one of exp model
                            GLParam(param_id=602, value=21 + variant + (hyperid - 501) * 2),
                        ],
                    },
                    exp_name='jump-exp-1',
                )
                cls.index.mskus += [msku_no_exp, msku_exp]

    def test_sku_offers_jump_table(self):
        """
        Проверяем, что переходы для тестовых MSKU отрабатывают корректно.
        На выдаче не должны появиться тестовые msku_id.
        """

        forbidden_msku_id_1 = 30501

        def _do_request(exp_status, sku):
            url = 'place=sku_offers&market-sku={}&show-urls=direct&rgb=blue&rearr-factors=contex=1;contex_multiply_models=1;jump-exp-1={};use_new_jump_table_pipeline=1'.format(
                sku, exp_status
            )
            return self.report.request_json(url)

        # Проверяем оба пайплайна -- на старый, судя по коду репорта, может случиться фоллбек (теоретически)
        # Вне эксперимента
        response = _do_request(0, 20501)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': '20501',
                        'titles': {'raw': 'title 501_0 NO EXP'},
                        'filters': [
                            {
                                'id': '601',
                                'values': [
                                    {
                                        'id': '10',
                                        'checked': True,
                                    },
                                    {
                                        'id': '11',
                                        'checked': Absent(),
                                    },
                                ],
                            },
                            {'id': '602', 'values': ElementCount(4)},
                        ],
                    }
                ]
            },
        )
        self.assertFragmentNotIn(response, {'marketSku': str(forbidden_msku_id_1)})
        self.assertFragmentNotIn(response, {'msku': str(forbidden_msku_id_1)})
        self.assertFragmentNotIn(response, {'filters': [{'id': 600}]})

        # В эксперименте
        response = _do_request(1, 20502)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': '20502',
                        'titles': {'raw': 'title 502_0 WITH EXP'},
                        'filters': [
                            {
                                'id': '602',
                                'values': [
                                    {
                                        'id': '23',
                                        'marketSku': '20502',
                                        'fuzzy': Absent(),
                                        'checked': True,
                                        'slug': 'title-502-0-with-exp',
                                    },
                                    {
                                        'id': '24',
                                        'marketSku': '21502',
                                        'fuzzy': Absent(),
                                        'checked': Absent(),
                                        'slug': 'title-502-1-with-exp',
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )
        self.assertFragmentNotIn(response, {'marketSku': str(forbidden_msku_id_1)})
        self.assertFragmentNotIn(response, {'msku': str(forbidden_msku_id_1)})
        self.assertFragmentNotIn(response, {'filters': [{'id': 600}]})
        self.assertFragmentNotIn(response, {'filters': [{'id': 601}]})


if __name__ == '__main__':
    main()

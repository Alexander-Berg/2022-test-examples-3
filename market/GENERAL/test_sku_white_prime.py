#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    ExchangeRate,
    GLParam,
    GLType,
    GLValue,
    MarketSku,
    MnPlace,
    Model,
    ModelDescriptionTemplates,
    Offer,
    Picture,
    Shop,
    VendorToGlobalColor,
)
from core.matcher import Absent
from core.types.picture import thumbnails_config


class T(TestCase):
    @classmethod
    def prepare_prefer_cpa(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=2, priority_region=213, regions=[225], name="Белый магазин"),
            Shop(fesh=3, priority_region=213, regions=[225], name="Белый магазин"),
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

        cls.index.models += [
            Model(hyperid=1, title='подмышник хозяйственный'),
            Model(
                hyperid=2,
                title='подмышник хозяйственный',
                hid=2,
                glparams=[GLParam(param_id=13887626, value=300), GLParam(param_id=13887626, value=400)],
            ),
            Model(hyperid=3, title='подмышник хозяйственный'),
            Model(hyperid=4),
            Model(hyperid=5),
            Model(hyperid=6),
        ]

        cls.index.currencies += [
            Currency(
                name=Currency.USD,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=60.0),
                ],
            ),
        ]

        cls.index.vendor_to_glob_colors += [
            VendorToGlobalColor(2, 100, [110, 120]),
            VendorToGlobalColor(2, 300, [310]),
            VendorToGlobalColor(2, 400, [410]),
        ]

        cls.index.offers += [
            Offer(hyperid=1, fesh=2, waremd5='jooaMuw3dToRIrcA3DG32A', ts=1),
            Offer(
                hyperid=2,
                fesh=3,
                waremd5='uqb4K8RseBZosGXlOs8MVw',
                ts=3,
                sku=12,
                price=111,
                glparams=[GLParam(param_id=14871214, value=110), GLParam(param_id=202, value=2)],
            ),
            Offer(hyperid=2, fesh=2, waremd5='uqb4K8RseBZosGXlOs8MV3', ts=4, price=55),
            Offer(hyperid=3, fesh=2, waremd5='22222222222222gggg404g'),
            Offer(hyperid=4, fesh=2, waremd5='_qQnWXU28-IUghltMZJwNw', ts=5, sku=13),
            Offer(title='подмышник хозяйственный', hyperid=4, fesh=2, waremd5='DuE098x_rinQLZn3KKrELw', ts=6),
            Offer(hyperid=5, fesh=2, waremd5='BH8EPLtKmdLQhLUasgaOnA', ts=8, sku=15),
            Offer(
                title='подмышник хозяйственный',
                hyperid=5,
                fesh=2,
                waremd5='KXGI8T3GP_pqjgdd7HfoHQ',
                ts=9,
                picture=pic_2,
                sku=16,
            ),
            Offer(hyperid=6, fesh=2, waremd5='V5Y7eJkIdDh0sMeCecijqw', ts=11, sku=18),
            Offer(title='подмышник хозяйственный', hyperid=6, fesh=2, waremd5='gpQxwKBuLtj5OIlRrvGwTw', ts=12),
        ]

        cls.index.gltypes += [
            GLType(
                hid=2,
                param_id=202,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='value1'),
                    GLValue(value_id=2, text='value2'),
                ],
                model_filter_index=1,
                xslname='sku_filter',
            ),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=13887626, hid=2, gltype=GLType.ENUM, values=[100, 200, 300, 400], cluster_filter=True
            ),  # базовый цвет
            GLType(
                param_id=14871214, hid=2, gltype=GLType.ENUM, values=[110, 120, 210, 310, 410], cluster_filter=True
            ),  # вендорский цвет
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=2,
                friendlymodel=['model friendly {sku_filter}'],
                model=[("Основное", {'model full': '{sku_filter}'})],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=10,
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='EQvXYayvR14UtaCEnnbmGg', ts=2)],
            ),
            MarketSku(
                hyperid=2,
                sku=11,
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='R4RpPv6gPSRso7ok6xDEAw', ts=4)],
            ),
            MarketSku(
                hyperid=2,
                sku=12,
                title='sku12 подмышник',
                blue_offers=[BlueOffer(price=2345, feedid=11, waremd5='uuENNVzevIeeT8bsxvY91w', ts=7)],
                glparams=[GLParam(param_id=202, value=2)],
            ),
            MarketSku(
                hyperid=4,
                sku=14,
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='otENNVzevIeeT8bsxvY91w', ts=7)],
            ),
            MarketSku(
                hyperid=5,
                sku=17,
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='yRgmzyBD4j8r4rkCby6Iuw', ts=10)],
            ),
            MarketSku(
                hyperid=6,
                sku=19,
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='xzFUFhFuAvI1sVcwDnxXPQ', ts=13)],
            ),
            MarketSku(hyperid=5, sku=16, picture=pic_1, title='sku16', waremd5='R444Pv6gPSRso7ok6xDEAw'),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.8)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.8)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.7)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10).respond(0.7)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13).respond(0.7)

    def test_prefer_cpa(self):
        """
        Проверяем, что на прайме под флагом в ДО берётся cpa-ДО, если он есть,
        иначе просто ДО. Проверяем т.ж., что под ещё одним флагом для
        схлопнутых моделей берётся оффер, из которого они схлопнулись, если он
        приматчен к ску
        """

        flag_cpa = '&rearr-factors=use_offer_type_priority_as_main_factor_in_do=1'
        no_cpa_priority = '&rearr-factors=use_offer_type_priority_as_main_factor_in_do=0;prefer_do_with_sku=0'
        param_no_sku_attr = '&sku-all=0'

        request = (
            'place=prime&use-default-offers=1&numdoc=20&'
            'text=подмышник+хозяйственный&allow-collapsing=1&'
            'show-models-specs=msku-friendly,msku-full'
            '&rearr-factors=market_metadoc_search=no'
        )

        def model(id, ware, sku):
            return {
                'entity': 'product',
                'id': id,
                'offers': {
                    'items': [
                        {
                            'wareId': ware,
                            'marketSku': sku,
                        }
                    ]
                },
            }

        response = self.report.request_json(request + param_no_sku_attr + no_cpa_priority)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        # у белого оффера, попавшего в ДО, нет ску
                        model(id=1, ware='jooaMuw3dToRIrcA3DG32A', sku=Absent()),
                        # у белого оффера, попавшего в ДО, есть ску
                        model(id=2, ware='uqb4K8RseBZosGXlOs8MVw', sku=Absent()),
                        # у белого оффера, попавшего в ДО, нет ску
                        model(id=3, ware='22222222222222gggg404g', sku=Absent()),
                        # модель схлопнулась из DuE098x_rinQLZn3KKrELw, но ДО
                        # у неё _qQnWXU28-IUghltMZJwNw
                        model(id=4, ware='_qQnWXU28-IUghltMZJwNw', sku=Absent()),
                        # то же самое
                        model(id=5, ware='BH8EPLtKmdLQhLUasgaOnA', sku=Absent()),
                        # то же самое
                        model(id=6, ware='V5Y7eJkIdDh0sMeCecijqw', sku=Absent()),
                    ]
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json(request + no_cpa_priority)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        # у белого оффера, попавшего в ДО, нет ску
                        model(id=1, ware='jooaMuw3dToRIrcA3DG32A', sku=Absent()),
                        # у белого оффера, попавшего в ДО, есть ску
                        model(id=2, ware='uqb4K8RseBZosGXlOs8MVw', sku='12'),
                        # у белого оффера, попавшего в ДО, нет ску
                        model(id=3, ware='22222222222222gggg404g', sku=Absent()),
                        # модель схлопнулась из DuE098x_rinQLZn3KKrELw, но ДО
                        # у неё _qQnWXU28-IUghltMZJwNw
                        model(id=4, ware='_qQnWXU28-IUghltMZJwNw', sku='13'),
                        # то же самое
                        model(id=5, ware='BH8EPLtKmdLQhLUasgaOnA', sku='15'),
                        # то же самое
                        model(id=6, ware='V5Y7eJkIdDh0sMeCecijqw', sku='18'),
                    ]
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json(request + flag_cpa)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        # синие офферы попадают в ДО для моделей 1 и 2
                        model(id=1, ware='EQvXYayvR14UtaCEnnbmGg', sku='10'),
                        model(id=2, ware='R4RpPv6gPSRso7ok6xDEAw', sku='11'),
                        # у модели 3 нет синих офферов, в качестве ДО берётся
                        # белый даже под флагом, у него нет ску
                        model(id=3, ware='22222222222222gggg404g', sku=Absent()),
                        # модель схлопнулась из белого DuE098x_rinQLZn3KKrELw, но
                        # cpa-ДО у неё otENNVzevIeeT8bsxvY91w
                        model(id=4, ware='otENNVzevIeeT8bsxvY91w', sku='14'),
                        # то же самое
                        model(id=5, ware='yRgmzyBD4j8r4rkCby6Iuw', sku='17'),
                        # то же самое
                        model(id=6, ware='xzFUFhFuAvI1sVcwDnxXPQ', sku='19'),
                    ]
                }
            },
            allow_different_len=True,
        )

    def test_color_filter(self):
        request = 'place=prime&numdoc=20&allow-collapsing=1&hyperid=2&debug=1&hid=2&add-vendor-color=1&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'filters': [
                    {
                        'id': '14871214',
                        'values': [
                            {
                                'id': '310',
                            },
                            {
                                'id': '410',
                            },
                        ],
                    }
                ],
            },
        )

    def test_sku_stats(self):
        request = 'place=prime&numdoc=20&allow-collapsing=1&hyperid=2&debug=1&hid=2&use-default-offers=1&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'skuStats': {
                    'totalCount': 2,
                },
            },
        )

    def test_filters_no_hid(self):
        """
        Проверяем, что параметр &allow-filters-without-hid=1 включает
        фильтры в ДО даже без параметра hid
        """

        request = (
            'place=prime&use-default-offers=1&numdoc=20&'
            'text=подмышник+хозяйственный&allow-collapsing=1&'
            'show-models-specs=msku-friendly,msku-full&'
        )
        param = '&allow-filters-without-hid=1'

        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'offers': {
                    'items': [
                        {
                            'marketSku': '12',
                            'filters': Absent(),
                        }
                    ]
                },
            },
        )

        response = self.report.request_json(request + param)

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'offers': {
                    'items': [
                        {
                            'marketSku': '12',
                            'filters': [
                                {
                                    'id': '202',
                                    'values': [
                                        {
                                            'id': '2',
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                },
            },
        )


if __name__ == '__main__':
    main()

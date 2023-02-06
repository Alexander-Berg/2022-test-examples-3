#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa

from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    HyperCategory,
    MarketSku,
    Model,
    ModelDescriptionTemplates,
    NavCategory,
    Offer,
    Opinion,
    Picture,
    PictureMbo,
    RegionalDelivery,
    Shop,
    Tax,
    Vendor,
    VirtualModel,
)
from core.testcase import TestCase, main
from core.types.picture import thumbnails_config

from core.matcher import ElementCount, Contains


class T(TestCase):
    @classmethod
    def prepare_modelinfo_sku(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[225],
                name='blue_shop_1',
                datafeed_id=2,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=808,
                fesh=2,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                title="Бутылка молока",
                proto_picture=PictureMbo('//avatars.mds.yandex.net/get-mpic/9/img_5/orig', width=500, height=600),
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=11,
                title="Бутылка синего молока",
                blue_offers=[BlueOffer(feedid=2)],
                delivery_buckets=[808],
                picture_flags=40,
                picture=Picture(picture_id='iyC4nHslqL_921_ygVAHeA', width=200, height=200, group_id=1234),
            ),
            MarketSku(
                hyperid=1,
                sku=12,
                title="Бутылка белого молока",
                picture_flags=41,
                picture=Picture(picture_id='iyC4nHslqL_922_ygVAHeA', width=200, height=200, group_id=1234),
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=1, sku=12, fesh=1, price=120),
            Offer(hyperid=1, sku=12, fesh=1, price=130),
            # White offers without sku should be used in market-sku=0 request
            Offer(hyperid=1, sku=0, fesh=1, price=90),
            Offer(hyperid=1, sku=0, fesh=1, price=125),
        ]

    def test_modelinfo_zero_sku_stats(self):
        """
        Тестируем что цены правильно считаются при запросе market-sku=0 (берутся все белые офферы без sku)
        """

        request = 'place=modelinfo&hyperid=1&rids=213&debug=1&show-models=1&market-sku=0'
        use_default_order_part = '&use-default-offers=1'

        for use_do in ("", use_default_order_part):
            response = self.report.request_json(request + use_do)
            self.assertFragmentIn(response, {'prices': {'min': '90', 'max': '125'}})

    def test_modelinfo_sku_stats(self):
        """
        Тестируем влияние sku на модельные статистики
        """
        response = self.report.request_json(
            'place=modelinfo&hyperid=1&rids=213&show-models=1&rearr-factors=market_cpa_only_fix_model_statistics=0'
        )
        self.assertFragmentIn(response, {'prices': {'min': '90'}})

        response = self.report.request_json('place=modelinfo&hyperid=1&rids=213&debug=1&show-models=1&market-sku=12')
        self.assertFragmentIn(response, {'prices': {'min': '120'}})

    def test_modelinfo_sku(self):
        """
        Проверяем, что плейс modelinfo под флагом начинает отдавать ску вместо
        модели, если она запрошена в параметре &market-sku
        Если запрошена market-sku=0, то отдается модель, но оффера учитываются только те, у которых sku не проставлена (=0)
        """

        model = {
            'id': 1,
            'entity': 'product',
            'offers': {'count': 5, 'items': ElementCount(1)},
            'titles': {
                'raw': 'Бутылка молока',
            },
            'pictures': [
                {'original': {'url': '//avatars.mds.yandex.net/get-mpic/9/img_5/orig'}},
            ],
            "prices": {"max": "130", "min": "90"},
        }

        sku11_pure = {
            'id': '11',
            'entity': 'sku',
            'titles': {
                'raw': 'Бутылка синего молока',
            },
            'pictures': [{'original': {'url': Contains('market_iyC4nHslqL_921_ygVAHeA/orig')}}],
        }

        sku11 = dict(
            sku11_pure,
            **{
                'product': model,
            }
        )

        sku12 = {
            'id': '12',
            'entity': 'sku',
            'titles': {
                'raw': 'Бутылка белого молока',
            },
            'pictures': [{'original': {'url': Contains('market_iyC4nHslqL_922_ygVAHeA/orig')}}],
            'product': model,
        }

        sku0 = {
            'id': 1,
            'entity': 'product',
            'offers': {'count': 2, 'items': ElementCount(1)},
            'titles': {
                'raw': 'Бутылка молока',
            },
            "prices": {"max": "125", "min": "90"},
            'pictures': [
                {'original': {'url': '//avatars.mds.yandex.net/get-mpic/9/img_5/orig'}},
            ],
        }

        # Запрос модели отдает модельные статистики - обрабатываются все оффера
        response = self.report.request_json('place=modelinfo&hyperid=1&rids=213&use-default-offers=1')

        self.assertFragmentIn(response, {'results': [model]})

        # Запрос ску отдает статистику конкретной ску - обрабатываются все оффера с таким ску
        # show-models=1 дополнительно говорит, что в поле product попадают статистики модели - также по всем офферам
        response = self.report.request_json(
            'place=modelinfo&hyperid=1&rids=213&debug=da&' 'use-default-offers=1&market-sku=11&show-models=1'
        )
        self.assertFragmentIn(response, {'results': [sku11]})

        # Запрос ску отдает статистику конкретной ску - обрабатываются все оффера с таким ску
        # Запрашиваем без модели
        response = self.report.request_json(
            'place=modelinfo&rids=213&debug=da&' 'use-default-offers=1&market-sku=12&show-models=1'
        )

        self.assertFragmentIn(response, {'results': [sku12]})

        response = self.report.request_json(
            'place=modelinfo&hyperid=1&rids=213&debug=da&' 'use-default-offers=1&market-sku=12&show-models=1'
        )

        self.assertFragmentIn(response, {'results': [sku12]})

        # При запросе нескольких ску в ответ попадут обе, каждая со своими отдельными статистиками
        response = self.report.request_json(
            'place=modelinfo&hyperid=1&rids=213&debug=da&'
            'use-default-offers=1&market-sku=11&market-sku=12&'
            'show-models=1'
        )

        self.assertFragmentIn(response, {'results': [sku11, sku12]})

        # при запросе market-sku=0 в ответе также будет модель, но статистики посчитаются только по офферам без ску
        # параметр show-models=1 ни на что не влияет
        for fragment in ('', '&show-models=1'):
            response = self.report.request_json(
                'place=modelinfo&hyperid=1&rids=213&debug=da&' 'use-default-offers=1&market-sku=0' + fragment
            )

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        sku0,
                    ]
                },
            )

    virtual_model_id_range_start = 2000000000
    virtual_model_id_range_finish = 3000000000
    virtual_model_id = (virtual_model_id_range_start + virtual_model_id_range_finish) // 2

    @classmethod
    def prepare_virtual_model_info(cls):
        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=T.virtual_model_id,
                opinion=Opinion(total_count=44, rating=4.3, precise_rating=4.31, rating_count=43, reviews=3),
            )
        ]

        cls.index.vendors += [
            Vendor(vendor_id=42, name="Virtucon", website="https://www.virtucon.com"),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=4242, name='Мечи'),
        ]

        cls.index.navtree += [NavCategory(nid=424242, hid=4242, name='Все Мечи')]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=4242,
                micromodel="{Quality#ifnz}Качественный{#endif} меч",
                friendlymodel=["{Quality#ifnz}Качественный{#endif}" "Меч"],
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Качество": "{Quality}",
                        },
                    ),
                ],
                seo="{return $Quality; #exec}",
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=142, hid=4242, gltype=GLType.BOOL, xslname="Quality"),
        ]

        cls.index.shops += [
            Shop(fesh=213, datafeed_id=14239, priority_region=213, regions=[213], client_id=11, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                waremd5='OfferWithVmid________g',
                title="Меч 0",
                fesh=213,
                vendor_id=42,
                hid=4242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=142, value=1)],
                virtual_model_id=T.virtual_model_id,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14239],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=14239,
                fesh=213,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_virtual_sku_card(self):
        """
        Проверяем, если передали виртуальный ску в запросе, то вернется виртуальная sku карточка
        """

        # Под флагом БК тоже должно работать
        for use_bk in ['', 'use_fast_cards=1']:
            # Под флагом market_cards_everywhere_model_info при наличии в запросе виртуального msku рисуем ску карточку
            flags = 'rearr-factors=market_cards_everywhere_model_info=1;market_cards_everywhere_range={}:{};{}'.format(
                T.virtual_model_id_range_start, T.virtual_model_id_range_finish, use_bk
            )
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&market-sku={}&rids=213&{}'.format(
                    T.virtual_model_id, T.virtual_model_id, flags
                )
            )

            virtualSku = {
                'id': str(T.virtual_model_id),
                'entity': 'sku',
                'titles': {
                    'raw': 'Меч 0',
                },
                "slug": "mech-0",
                "pictures": [
                    {
                        "entity": "picture",
                        "thumbnails": [
                            {
                                "containerWidth": 200,
                                "containerHeight": 200,
                                "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                                "width": 200,
                                "height": 200,
                            }
                        ],
                    }
                ],
                'product': {
                    "id": T.virtual_model_id,
                },
                "vendor": {
                    "entity": "vendor",
                    "id": 42,
                    "name": "Virtucon",
                    "slug": "virtucon",
                    "website": "https://www.virtucon.com",
                },
                "categories": [
                    {
                        "entity": "category",
                        "id": 4242,
                    }
                ],
                "navnodes": [
                    {
                        "entity": "navnode",
                        "id": 424242,
                    }
                ],
            }
            self.assertFragmentIn(response, {'results': [virtualSku]})

            virtualModel = {
                'id': T.virtual_model_id,
                'entity': 'product',
                'offers': {
                    'count': 1,
                },  # 'items': ElementCount(1)
                'titles': {
                    'raw': 'Меч 0',
                },
                "pictures": [
                    {
                        "entity": "picture",
                        "thumbnails": [
                            {
                                "containerWidth": 200,
                                "containerHeight": 200,
                                "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                                "width": 200,
                                "height": 200,
                            }
                        ],
                    }
                ],
                "prices": {"avg": "100", "currency": "RUR", "max": "100", "min": "100"},
                "rating": 4.3,
                "opinions": 44,
                "preciseRating": 4.31,
                "ratingCount": 43,
                "reviews": 3,
            }

            # Если задан show-models=1, то также отрисовываем виртуальную модельку
            response = self.report.request_json(
                'place=modelinfo&debug=1&hyperid={}&market-sku={}&rids=213&{}&show-models=1&use-default-offers=1'.format(
                    T.virtual_model_id, T.virtual_model_id, flags
                )
            )

            virtualSku['product'] = virtualModel
            self.assertFragmentIn(response, {'results': [virtualSku]})

            # Без флага будет пустая выдача
            response = self.report.request_json(
                'place=modelinfo&debug=1&hyperid={}&market-sku={}&rids=213&show-models=1&use-default-offers=1'.format(
                    T.virtual_model_id, T.virtual_model_id
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [],
                },
            )

    @classmethod
    def prepare_fast_cards_model_info_sku(cls):
        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=1580,
                opinion=Opinion(total_count=44, rating=4.3, precise_rating=4.31, rating_count=43, reviews=3),
            ),
            VirtualModel(
                virtual_model_id=1582,
                opinion=Opinion(total_count=47, rating=4.5, precise_rating=4.51, rating_count=45, reviews=5),
            ),
            VirtualModel(
                virtual_model_id=T.virtual_model_id + 10,
                opinion=Opinion(total_count=41, rating=4.1, precise_rating=4.11, rating_count=41, reviews=41),
            ),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=142, name="Гвинт", website="https://www.gwent.com"),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=14242),
        ]

        cls.index.navtree += [NavCategory(nid=104242, hid=14242, name='Гвинтокарты')]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=14242,
                micromodel="{Quality#ifnz}Качественная{#endif} карта",
                friendlymodel=["{Quality#ifnz}Качественная{#endif}" "Карта"],
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Качество": "{Quality}",
                        },
                    ),
                ],
                seo="{return $Quality; #exec}",
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=420, hid=14242, gltype=GLType.NUMERIC, xslname="Quality"),
        ]

        cls.index.shops += [
            Shop(fesh=12710, datafeed_id=14240, priority_region=213, regions=[213], client_id=12, cpa=Shop.CPA_REAL),
            Shop(
                fesh=12711,
                datafeed_id=14241,
                priority_region=213,
                regions=[213],
                client_id=13,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                waremd5='OfferFastModel0CPC___g',
                title="Геральт cpc",
                fesh=213,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=420, value=1)],
                price=150,
            ),
            Offer(
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                waremd5='OfferFastModel0CPA___g',
                title="Геральт cpa",
                fesh=12710,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=300,
                    height=300,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=420, value=2)],
                price=175,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14240],
            ),
            Offer(
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                waremd5='OfferFastModel0BLUE__g',
                title='Геральт синий',
                fesh=12711,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=400,
                    height=400,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=420, value=3)],
                price=250,
                delivery_buckets=[14241],
                blue_without_real_sku=True,
            ),
            Offer(
                sku=1582,
                virtual_model_id=1582,
                vmid_is_literal=False,
                waremd5='OfferFastModel1BLUE__g',
                title='Мильва синяя',
                fesh=12711,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=420, value=3)],
                price=333,
                delivery_buckets=[14241],
                blue_without_real_sku=True,
            ),
            # двойной маппинг
            Offer(
                sku=1583,
                virtual_model_id=T.virtual_model_id + 10,
                vmid_is_literal=False,
                waremd5='OfferFastModel2BLUE__g',
                title='Двойной маппинг',
                fesh=12711,
                vendor_id=142,
                hid=14242,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                price=555,
                delivery_buckets=[14241],
                blue_without_real_sku=True,
            ),
        ]

        # Добавим побольше, чтобы убедиться, что карточка строится из оффера с минимальным оффсетом
        cls.index.offers += [
            Offer(
                virtual_model_id=1581,
                waremd5='OfferFastModel{}______g'.format(i),
                title="Цири cpc 1581 {}".format(i),
                fesh=213,
                vendor_id=142,
                hid=14242,
            )
            for i in range(7)
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=14240,
                fesh=12710,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14241,
                fesh=12711,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_fast_cards_sku(self):
        """
        Проверяем работу быстрых карточек
        По сути - те же виртуальне карточки, только могут иметь несколько офферов и айдишник неотличим от мску
        Под флагом: use_fast_cards
        """

        fastSku = {
            "id": '1580',
            "categories": [{"entity": "category", "id": 14242, "nid": 104242, "slug": "hid-14242", "type": "simple"}],
            "entity": "sku",
            "filters": [
                {
                    "id": "420",
                    "type": "number",
                    "values": [{"id": "found", "initialMax": "1", "initialMin": "1", "max": "1", "min": "1"}],
                    "xslname": "Quality",
                },
            ],
            "navnodes": [
                {
                    "entity": "navnode",
                    "id": 104242,
                    "name": "Гвинтокарты",
                }
            ],
            "pictures": [
                {
                    "entity": "picture",
                    "original": {
                        "containerHeight": 200,
                        "containerWidth": 200,
                        "height": 200,
                        "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/orig",
                        "width": 200,
                    },
                },
            ],
            "product": {
                "id": 1580,
            },
            "titles": {"highlighted": [{"value": "Геральт cpc"}], "raw": "Геральт cpc"},
            "vendor": {"entity": "vendor", "id": 142, "name": "Гвинт", "website": "https://www.gwent.com"},
        }
        # Простой запрос с id быстрой карточки вместо ску,
        # моделька должна построиться из минимального по оффсету оффера - OfferFastModel0CPC___g
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json('place=modelinfo&market-sku=1580&rids=213&show-models-specs=full' + flags)
        self.assertFragmentIn(response, {'results': [fastSku], 'total': 1})

        # Пробуем запросить БК и обычную скю
        response = self.report.request_json(
            'place=modelinfo&market-sku=1580&market-sku=11&rids=213&show-models-specs=full' + flags
        )
        sku11 = {
            'id': '11',
            'entity': 'sku',
            'titles': {
                'raw': 'Бутылка синего молока',
            },
            'pictures': [{'original': {'url': Contains('market_iyC4nHslqL_921_ygVAHeA/orig')}}],
        }
        self.assertFragmentIn(response, {'results': [sku11, fastSku], 'total': 2})

        # проверяем, что с show-models в блоке product вернется быстрая карточка подукта из того же оффера
        response = self.report.request_json(
            'place=modelinfo&hyperid=1580&market-sku=1580&rids=213&show-models-specs=full&show-models=1&use-default-offers=1'
            + flags
        )
        fastModel = {
            'id': 1580,
            'entity': 'product',
            'offers': {
                # При использовании use-default-offers=1 для CPA применяется логика байбокса
                # из-за этого синий оффер отфильтровывается и не участвует в статистике
                # Такое поведение выглядит норм
                'count': 2,
                'items': [
                    {
                        'entity': 'offer',
                        'wareId': 'OfferFastModel0CPA___g',
                        "model": {
                            "id": 1580,
                        },
                    },
                ],
            },
            'titles': {
                'raw': 'Геральт cpc',
            },
            "pictures": [
                {
                    "entity": "picture",
                    "thumbnails": [
                        {
                            "containerWidth": 200,
                            "containerHeight": 200,
                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                            "width": 200,
                            "height": 200,
                        }
                    ],
                }
            ],
            "prices": {"avg": "163", "currency": "RUR", "max": "175", "min": "150"},
            "rating": 4.3,
            "opinions": 44,
            "preciseRating": 4.31,
            "ratingCount": 43,
            "reviews": 3,
        }
        fastSku['product'] = fastModel
        self.assertFragmentIn(response, {'results': [fastSku], 'total': 1})

        # Проверяем, что скю карточка нормально строится из синего оффера
        response = self.report.request_json(
            'place=modelinfo&hyperid=1582&market-sku=1582&rids=213&show-models-specs=full&show-models=1&use-default-offers=1'
            + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "id": '1582',
                        "categories": [
                            {
                                "entity": "category",
                                "id": 14242,
                                "nid": 104242,
                                "slug": "hid-14242",
                                "type": "simple",
                            }
                        ],
                        "entity": "sku",
                        "filters": [
                            {
                                "id": "420",
                                "type": "number",
                                "values": [
                                    {"id": "found", "initialMax": "3", "initialMin": "3", "max": "3", "min": "3"}
                                ],
                                "xslname": "Quality",
                            },
                        ],
                        "navnodes": [
                            {
                                "entity": "navnode",
                                "id": 104242,
                                "name": "Гвинтокарты",
                            }
                        ],
                        "pictures": [
                            {
                                "entity": "picture",
                                "original": {
                                    "containerHeight": 200,
                                    "containerWidth": 200,
                                    "height": 200,
                                    "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/orig",
                                    "width": 200,
                                },
                            },
                        ],
                        "product": {
                            'id': 1582,
                            'entity': 'product',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'wareId': 'OfferFastModel1BLUE__g',
                                        "model": {
                                            "id": 1582,
                                        },
                                        "offerColor": "blue",
                                        "marketSku": "1582",
                                    },
                                ],
                            },
                            'titles': {
                                'raw': 'Мильва синяя',
                            },
                            "pictures": [
                                {
                                    "entity": "picture",
                                    "thumbnails": [
                                        {
                                            "containerWidth": 200,
                                            "containerHeight": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                                            "width": 200,
                                            "height": 200,
                                        }
                                    ],
                                }
                            ],
                            "prices": {"avg": "333", "currency": "RUR", "max": "333", "min": "333"},
                            "rating": 4.5,
                            "opinions": 47,
                            "preciseRating": 4.51,
                            "ratingCount": 45,
                            "reviews": 5,
                        },
                        "titles": {"highlighted": [{"value": "Мильва синяя"}], "raw": "Мильва синяя"},
                        "vendor": {"entity": "vendor", "id": 142, "name": "Гвинт", "website": "https://www.gwent.com"},
                    }
                ],
                'total': 1,
            },
        )

        # без флага должна быть пустая выдача
        flags = '&rearr-factors=use_fast_cards=0'
        response = self.report.request_json('place=modelinfo&market-sku=1580&rids=213&show-models-specs=full' + flags)
        self.assertFragmentIn(response, {'results': [], 'total': 0})

    def test_double_mapping(self):
        """
        В рамках https://st.yandex-team.ru/MARKETOUT-43883 делаем быстрофикс для офферов с маппингом на БК + ВК

        Это временный быстрый хак, чтобы показывать такие карточки
        В таких случаях мы не сможем насчитать модельные статистики, тк vmid у этих офферов не поисковой литерал
        Тк это временное решеник (ждем индексатор), то должно быть +- ок
        """
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json(
            'place=modelinfo&market-sku=1583&hyperid={}&rids=213&show-models-specs=full&show-models=1'.format(
                T.virtual_model_id + 10
            )
            + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "id": '1583',
                        "categories": [
                            {
                                "entity": "category",
                                "id": 14242,
                                "nid": 104242,
                                "slug": "hid-14242",
                                "type": "simple",
                            }
                        ],
                        "entity": "sku",
                        "navnodes": [
                            {
                                "entity": "navnode",
                                "id": 104242,
                                "name": "Гвинтокарты",
                            }
                        ],
                        "pictures": [
                            {
                                "entity": "picture",
                                "original": {
                                    "containerHeight": 200,
                                    "containerWidth": 200,
                                    "height": 200,
                                    "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/orig",
                                    "width": 200,
                                },
                            },
                        ],
                        "product": {
                            'id': T.virtual_model_id + 10,
                            'entity': 'product',
                            'titles': {
                                'raw': 'Двойной маппинг',
                            },
                            "pictures": [
                                {
                                    "entity": "picture",
                                    "thumbnails": [
                                        {
                                            "containerWidth": 200,
                                            "containerHeight": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                                            "width": 200,
                                            "height": 200,
                                        }
                                    ],
                                }
                            ],
                            "rating": 4.1,
                            "opinions": 41,
                            "preciseRating": 4.11,
                            "ratingCount": 41,
                            "reviews": 41,
                        },
                        "titles": {"highlighted": [{"value": "Двойной маппинг"}], "raw": "Двойной маппинг"},
                        "vendor": {"entity": "vendor", "id": 142, "name": "Гвинт", "website": "https://www.gwent.com"},
                    }
                ],
                'total': 1,
            },
        )

    @classmethod
    def prepare_sku_specs_with_jump_table_filter(cls):
        cls.index.gltypes += [
            GLType(hid=47, param_id=201, xslname="param_201", cluster_filter=False, positionless=True),
            GLType(
                hid=47,
                param_id=202,
                xslname="param_202",
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[1, 2, 3, 4, 5],
                model_filter_index=1,
                unit_name='л',
                position=10,
            ),
            GLType(
                hid=47,
                param_id=203,
                xslname="param_203",
                cluster_filter=True,
                gltype=GLType.NUMERIC,
                model_filter_index=2,
                unit_name='кг',
                position=20,
                precision=3,
            ),
            GLType(
                hid=47,
                param_id=204,
                xslname="param_204",
                cluster_filter=True,
                gltype=GLType.BOOL,
                model_filter_index=3,
                positionless=True,
            ),
            GLType(
                hid=47,
                param_id=205,
                xslname="param_205",
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[1, 2, 3, 4, 5],
                model_filter_index=-1,
                unit_name='л',
                position=10,
            ),
            GLType(
                hid=47,
                param_id=206,
                xslname="param_206",
                cluster_filter=True,
                gltype=GLType.NUMERIC,
                model_filter_index=-1,
                unit_name='кг',
                position=20,
                precision=3,
            ),
            GLType(
                hid=47,
                param_id=207,
                xslname="param_207",
                cluster_filter=True,
                gltype=GLType.BOOL,
                model_filter_index=-1,
                positionless=True,
            ),
        ]

        cls.index.models += [
            Model(
                hid=47,
                title="Model 1",
                hyperid=1337,
                glparams=[
                    GLParam(param_id=100, value=1),
                    GLParam(param_id=101, value=1),
                ],
            ),
            Model(
                hid=47,
                title="Model 2",
                hyperid=1338,
                glparams=[
                    GLParam(param_id=100, value=0),
                    GLParam(param_id=101, value=2),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=47,
                hyperid=1337,
                sku=2001,
                title="Игрушка 1",
                fesh=11,
                blue_offers=[
                    BlueOffer(ts=9, price=200),
                    BlueOffer(price=300, feedid=3001, business_id=2, is_express=True),
                ],
                glparams=[
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=203, value=15),
                    GLParam(param_id=204, value=0),
                    GLParam(param_id=205, value=1),
                    GLParam(param_id=206, value=15),
                    GLParam(param_id=207, value=0),
                ],
            ),
            MarketSku(
                hid=47,
                hyperid=1337,
                sku=2002,
                title="Игрушка 2",
                fesh=11,
                blue_offers=[
                    BlueOffer(ts=10, price=100),
                    BlueOffer(price=200, feedid=3001, business_id=2, is_express=True),
                ],
                glparams=[
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=203, value=16),
                    GLParam(param_id=204, value=0),
                    GLParam(param_id=205, value=1),
                    GLParam(param_id=206, value=16),
                    GLParam(param_id=207, value=0),
                ],
            ),
            MarketSku(
                hid=47,
                hyperid=1337,
                sku=2003,
                title="Игрушка 3",
                fesh=11,
                blue_offers=[
                    BlueOffer(ts=11, price=200),
                    BlueOffer(price=300, feedid=3001, business_id=2, is_express=True),
                ],
                glparams=[
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=203, value=15),
                    GLParam(param_id=204, value=1),
                    GLParam(param_id=205, value=2),
                    GLParam(param_id=206, value=15),
                    GLParam(param_id=207, value=1),
                ],
            ),
            # no offers
            MarketSku(
                hid=47,
                hyperid=1337,
                sku=2004,
                title="Игрушка 4",
                glparams=[
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=203, value=15),
                    GLParam(param_id=204, value=1),
                    GLParam(param_id=205, value=2),
                    GLParam(param_id=206, value=15),
                    GLParam(param_id=207, value=1),
                ],
            ),
            # white offers only
            MarketSku(
                hid=47,
                hyperid=1337,
                sku=2005,
                fesh=11,
                title="Игрушка 5",
                glparams=[
                    GLParam(param_id=202, value=3),
                    GLParam(param_id=203, value=16),
                    GLParam(param_id=204, value=1),
                    GLParam(param_id=205, value=3),
                    GLParam(param_id=206, value=16),
                    GLParam(param_id=207, value=1),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                sku=2005,
                fesh=1,
                price=100,
                glparams=[
                    GLParam(param_id=202, value=3),
                    GLParam(param_id=203, value=16),
                    GLParam(param_id=204, value=1),
                    GLParam(param_id=205, value=3),
                    GLParam(param_id=206, value=16),
                    GLParam(param_id=207, value=1),
                ],
            ),
            Offer(hyperid=1377),
            Offer(hyperid=1377, glparams=[GLParam(param_id=202, value=4)]),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=47,
                micromodel="{param_202}, {param_203}, {param_204}, {param_205}, {param_206}, {param_207}",
                friendlymodel=[
                    "param_202 {param_202}",
                    "param_203 {param_203}",
                    "param_204 {param_204}",
                    "param_205 {param_205}",
                    "param_206 {param_206}",
                    "param_207 {param_207}",
                ],
                model=[
                    (
                        "Основное",
                        {
                            "param_202": "{param_202}",
                            "param_203": "{param_203}",
                            "param_204": "{param_204}",
                            "param_205": "{param_205}",
                            "param_206": "{param_206}",
                            "param_207": "{param_207}",
                        },
                    )
                ],
            )
        ]

    def test_sku_specs_with_jump_table_filter(self):
        """
        Проверяем, что при вызове modelinfo с market-sku и show-model-specs
        и filter-jumptable-params-in-specs = true
        Выводятся характеристики, по которым не строится карта переходов
        """

        specs_msku_response = {
            "specs": {
                "friendly": [
                    "param_205 VALUE-1",
                    "param_206 15",
                    "param_207 нет",
                ],
                "friendlyext": [
                    {
                        "value": "param_205 VALUE-1",
                        "usedParams": [205],
                    },
                    {
                        "value": "param_206 15",
                        "usedParams": [206],
                    },
                    {
                        "value": "param_207 нет",
                        "usedParams": [207],
                    },
                ],
            },
        }

        specs_not_in_response = {
            "specs": {
                "friendly": [
                    "param_202 VALUE-1",
                    "param_203 15",
                    "param_204 нет",
                ],
                "friendlyext": [
                    {
                        "usedParams": [202],
                    },
                    {
                        "usedParams": [203],
                    },
                    {
                        "usedParams": [204],
                    },
                ],
            },
        }

        request = 'place=modelinfo&hid=47&rids=0&hyperid=1337&market-sku=2001&show-models-specs=msku-friendly&filter-jumptable-params-in-specs=1'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, specs_msku_response)
        self.assertFragmentNotIn(response, specs_not_in_response)

    def test_sku_specs_without_jump_table_filter(self):
        """
        Проверяем, что при вызове modelinfo с market-sku и show-model-specs
        и filter-jumptable-params-in-specs = false
        Выводятся все характеристики
        """

        specs_msku_response = {
            "specs": {
                "friendly": [
                    "param_202 VALUE-1",
                    "param_203 15",
                    "param_204 нет",
                    "param_205 VALUE-1",
                    "param_206 15",
                    "param_207 нет",
                ],
                "friendlyext": [
                    {
                        "value": "param_202 VALUE-1",
                        "usedParams": [202],
                    },
                    {
                        "value": "param_203 15",
                        "usedParams": [203],
                    },
                    {
                        "value": "param_204 нет",
                        "usedParams": [204],
                    },
                    {
                        "value": "param_205 VALUE-1",
                        "usedParams": [205],
                    },
                    {
                        "value": "param_206 15",
                        "usedParams": [206],
                    },
                    {
                        "value": "param_207 нет",
                        "usedParams": [207],
                    },
                ],
            },
        }

        request = 'place=modelinfo&hid=47&rids=0&hyperid=1337&market-sku=2001&show-models-specs=msku-friendly&filter-jumptable-params-in-specs=0'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, specs_msku_response)


if __name__ == '__main__':
    main()

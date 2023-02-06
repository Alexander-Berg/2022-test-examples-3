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
    ModelDescriptionTemplates,
    NavCategory,
    Offer,
    OfferDimensions,
    Opinion,
    Picture,
    RegionalDelivery,
    Shop,
    Vat,
    VirtualModel,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty
from core.types.picture import thumbnails_config


shop_dsbs = Shop(
    fesh=42,
    datafeed_id=4240,
    priority_region=213,
    regions=[213],
    name='Все мечи',
    client_id=11,
    cpa=Shop.CPA_REAL,
)

virtual_model_id_range_start = int(2 * 1e12)
virtual_model_id_range_finish = int(virtual_model_id_range_start + 1e15)
virtual_model_id = (virtual_model_id_range_start + virtual_model_id_range_finish) // 2


offer_wo_mapping1 = Offer(
    title="Меч1 с msku == vmid",
    descr='Меч w_cpa с доставкой без модели',
    fesh=shop_dsbs.fesh,
    waremd5='WhiteCpaWoMSKUvmid001g',
    hid=4244,
    price=1000,
    virtual_model_id=virtual_model_id,
    cpa=Offer.CPA_REAL,
    picture=Picture(
        picture_id='iyC4nHslqLtqZJLygVAHeA',
        width=200,
        height=200,
        thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
        group_id=1234,
    ),
    glparams=[GLParam(param_id=42, value=1)],
    delivery_buckets=[4240],
)

offer_wo_mapping2 = Offer(
    title="Меч3 без msku и модельки",
    descr='Меч w_cpa с доставкой без модели',
    fesh=shop_dsbs.fesh,
    waremd5='WhiteCpaWoMSKUvmid002g',
    price=1000,
    virtual_model_id=virtual_model_id + 1,
    hid=4244,
    cpa=Offer.CPA_REAL,
    picture=Picture(
        picture_id='iyC4nHslqLtqZJLygVAHeA',
        width=200,
        height=200,
        thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
        group_id=1234,
    ),
    glparams=[GLParam(param_id=42, value=1)],
    delivery_buckets=[4240],
)

shop_blue = Shop(
    fesh=103,
    datafeed_id=1031,
    priority_region=213,
    name='Синий поставщик',
    client_id=12,
    cpa=Shop.CPA_REAL,
    blue=Shop.BLUE_REAL,
)

offer_blue = BlueOffer(
    fesh=shop_blue.fesh,
    feedid=shop_blue.datafeed_id,
    offerid='Shop_Blue_sku_0',
    vat=Vat.VAT_10,
    waremd5='Blue101______________Q',
    price=10000,
    weight=0.3,
    dimensions=OfferDimensions(length=1, width=22, height=33.3),
    delivery_buckets=[4343],
)

# msku с синим оффером
msku_0 = MarketSku(title="Verstak", hyperid=1002, sku=10020, blue_offers=[offer_blue])

offer_blue_1 = BlueOffer(
    fesh=shop_blue.fesh,
    feedid=shop_blue.datafeed_id,
    offerid='Shop_Blue_sku_1',
    vat=Vat.VAT_10,
    waremd5='Blue102______________Q',
    price=1000,
    weight=0.3,
    dimensions=OfferDimensions(length=1, width=22, height=33.3),
    delivery_buckets=[4343],
)

msku_1 = MarketSku(title="Verstak0", hyperid=1003, sku=10030, blue_offers=[offer_blue_1])

# buybox-ы быстрой карточки
fast_card_buybox_for_white = Offer(
    fesh=12711,
    waremd5='offer_blue_vmid_fc0_mQ',
    title='Оффер быстрокарточки 1580 blue - 0',
    descr='Описание быстрокарточки',
    price=250,
    delivery_buckets=[14241],
    sku=1580,
    virtual_model_id=1580,
    blue_without_real_sku=True,
    vmid_is_literal=False,
    hid=123,
    picture=Picture(
        picture_id='iyC4nHslqLtqZJLygVAHeA',
        width=200,
        height=200,
        thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
        group_id=1234,
    ),
    glparams=[GLParam(param_id=224, value=1)],
)

fast_card_buybox_for_blue = Offer(
    fesh=12710,
    waremd5='offer_cpa_vmid_fc0__mQ',
    title='Оффер быстрокарточки 1580 cpa - 0',
    descr='Описание быстрокарточки',
    price=150,
    cpa=Offer.CPA_REAL,
    delivery_buckets=[14240],
    sku=1580,
    virtual_model_id=1580,
    vmid_is_literal=False,
    hid=123,
    picture=Picture(
        picture_id='iyC4nHslqLtqZJLygVAHeA',
        width=200,
        height=200,
        thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
        group_id=1234,
    ),
    glparams=[GLParam(param_id=224, value=1)],
)


class T(TestCase):

    """
    ВАЖНО!
    СЕЙЧАС ВСЕ ФЛАГИ ВИРТУАЛЬНЫХ КАРТОЧЕК ПО-УМОЛЧАНИЮ ВКЛЮЧЕНЫ
    """

    @classmethod
    def prepare(cls):

        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=virtual_model_id,
                opinion=Opinion(total_count=44, rating=4.3, precise_rating=4.31, rating_count=43, reviews=3),
            ),
            VirtualModel(
                virtual_model_id=virtual_model_id + 1,
                opinion=Opinion(total_count=0, rating=0, precise_rating=0, rating_count=0, reviews=0),
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=4244, name='Мечи'),
        ]

        cls.index.navtree += [NavCategory(nid=424242, hid=4244, name='Все для мечей')]
        # Добавить это описание для sku
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=4244,
                friendlymodel=[
                    "{Material#ifnz}Железный{#endif} меч",
                ],
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Материал": "{Material}",
                        },
                    ),
                ],
                seo="{return $Material; #exec}",
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=42, hid=4244, gltype=GLType.BOOL, xslname="Material"),
        ]

        cls.index.shops += [
            shop_dsbs,
        ]

        cls.index.mskus += [
            msku_0,
            msku_1,
        ]

        cls.index.offers += [
            offer_wo_mapping1,
            offer_wo_mapping2,
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4240,
                fesh=shop_dsbs.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=4343,
                fesh=shop_blue.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=2)])],
            ),
        ]

    def test_virtual_sku_offers(self):
        """
        Если у оффера заполнено virtual_model_id, то с флагом market_cards_everywhere_sku_offers=1
        должна возвращаться "виртуальная" sku карточка, а в offers должен лежать наш оффер
        Тонкости с моделькой:
        1) Если у оффера есть модель, то она должна возвращаться из дозапроса modelinfo внутри sku_offers
        2) Если у оффера нет модели, то:
            -  c флагом market_cards_everywhere_model_info=1 в product должна возвращаться виртуальная моделька
            -  без флага оставляем поле product пустым

        ВАЖНО!
        СЕЙЧАС ВСЕ ФЛАГИ ВИРТУАЛЬНЫХ КАРТОЧЕК ПО-УМОЛЧАНИЮ ВКЛЮЧЕНЫ
        """

        def create_expected_sku(sku_id, model_id, ware_id, descr, offer_title, offer_count=0, need_full_product=True):
            return {
                "search": {
                    "total": 1,
                    "totalFreeOffers": 0,
                    "totalModels": 0,
                    "totalOffers": 1,
                    "cpaCount": 1,
                    "results": [
                        {
                            "categories": [
                                {
                                    "entity": "category",
                                    "id": 4244,
                                }
                            ],
                            "description": descr,
                            "entity": "sku",
                            "formattedDescription": {
                                "fullHtml": descr,
                                "fullPlain": descr,
                                "shortHtml": descr,
                                "shortPlain": descr,
                            },
                            "id": str(sku_id),
                            "isAdult": False,
                            "marketSkuCreator": "virtual",
                            "offers": {
                                "items": [
                                    {
                                        "cpa": "real",
                                        "description": descr,
                                        "entity": "offer",
                                        "marketSku": str(sku_id),
                                        "marketSkuCreator": "virtual",
                                        "sku": str(sku_id),
                                        "model": {"id": int(model_id)},
                                        "offerColor": "white",
                                        "supplier": {
                                            "business_id": 42,
                                        },
                                        "titles": {
                                            "highlighted": [
                                                {
                                                    "value": offer_title,
                                                }
                                            ],
                                            "raw": offer_title,
                                        },
                                        "urls": NotEmpty(),
                                        "wareId": ware_id,
                                    }
                                ],
                            },
                            "product": {
                                "categories": [
                                    {
                                        "id": 4244,
                                    }
                                ],
                                "entity": "product",
                                "id": int(model_id),
                                "offers": {"count": offer_count, "cutPriceCount": 0},
                                "pictures": NotEmpty(),
                                "type": "model",
                            }
                            if need_full_product
                            else {"id": int(model_id)},
                            "specs": {
                                "friendly": ["Железный меч"],
                                "friendlyext": [{"type": "spec", "usedParams": [42], "value": "Железный меч"}],
                                "full": [
                                    {
                                        "groupName": "Технические характеристики",
                                        "groupSpecs": [
                                            {
                                                "desc": "Материал parameter description",
                                                "name": "Материал",
                                                "usedParams": [{"id": 42, "name": "GLPARAM-42"}],
                                                "value": "есть",
                                            }
                                        ],
                                    }
                                ],
                            },
                        }
                    ],
                }
            }

        # Запрос для белого cpa оффера с моделькой, все флаги проставлены
        params = [
            'rgb=blue&rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
            'rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
        ]
        for flags in params:
            # Запрос для белого cpa оффера без модельки, все флаги проставлены
            # в этом случае id модельки и sku будут равны vmid-у
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                    virtual_model_id, flags
                )
            )
            self.assertFragmentIn(
                response,
                create_expected_sku(
                    sku_id=virtual_model_id,
                    model_id=virtual_model_id,
                    ware_id=offer_wo_mapping1.waremd5,
                    descr=offer_wo_mapping1.description,
                    offer_title=offer_wo_mapping1.title,
                    offer_count=1,
                ),
                allow_different_len=False,
            )

            # Запрос для белого cpa оффера без модельки, нет флага для modelinfo
            # В product будет только айдишник
            enrichedFlags = flags + ';market_cards_everywhere_model_info=0'
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                    virtual_model_id, enrichedFlags
                )
            )
            self.assertFragmentIn(
                response,
                create_expected_sku(
                    sku_id=virtual_model_id,
                    model_id=virtual_model_id,
                    ware_id=offer_wo_mapping1.waremd5,
                    descr=offer_wo_mapping1.description,
                    offer_title=offer_wo_mapping1.title,
                    offer_count=1,
                    need_full_product=False,
                ),
                allow_different_len=False,
            )
            self.assertFragmentNotIn(
                response,
                create_expected_sku(
                    sku_id=virtual_model_id,
                    model_id=virtual_model_id,
                    ware_id=offer_wo_mapping1.waremd5,
                    descr=offer_wo_mapping1.description,
                    offer_title=offer_wo_mapping1.title,
                    offer_count=1,
                    need_full_product=True,
                ),
            )

            # Запрос для белого cpa оффера с vmid
            # Для такого оффера без модели под флагом market_cards_everywhere_sku_offers тоже должно имитироваться наличие msku
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-urls=direct,cpa&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                    virtual_model_id + 1, flags
                )
            )
            self.assertFragmentIn(
                response,
                create_expected_sku(
                    sku_id=virtual_model_id + 1,
                    model_id=virtual_model_id + 1,
                    ware_id=offer_wo_mapping2.waremd5,
                    descr=offer_wo_mapping2.description,
                    offer_title=offer_wo_mapping2.title,
                    offer_count=1,
                ),
                allow_different_len=False,
            )

    def test_virtual_sku_offers_negative(self):
        # Запрос для белого cpa оффера без модельки, все флаги проставлены
        # Но vmid не лежит внутри market_cards_everywhere_range => пустая выдача
        params = [
            'rgb=blue&rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_finish - 2, virtual_model_id_range_finish
            ),
            'rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_finish - 2, virtual_model_id_range_finish
            ),
        ]
        for flags in params:
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                    virtual_model_id, flags
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [],
                },
                allow_different_len=False,
            )

        # Запрос для белого cpa оффера без модельки, без флага market_cards_everywhere_sku_offers
        # Пустая выдача
        params = [
            'rgb=blue&rearr-factors=market_cards_everywhere_sku_offers=0;market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
            'rearr-factors=market_cards_everywhere_sku_offers=0;market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
        ]
        for flags in params:
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                    virtual_model_id, flags
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [],
                },
                allow_different_len=False,
            )

        # Запрос для белого cpa оффера без модельки, без флага market_cards_everywhere_range
        # Пустая выдача
        params = [
            'rgb=blue&rearr-factors=market_cards_everywhere_range=0:0',
            'rearr-factors=market_cards_everywhere_range=0:0',
        ]
        for flags in params:
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                    virtual_model_id, flags
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [],
                },
                allow_different_len=False,
            )

        flags = (
            'rgb=blue&rearr-factors= market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
        )
        response = self.report.request_json(
            'place=sku_offers&market-sku={}&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                virtual_model_id, flags
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [],
            },
            allow_different_len=False,
        )

    def test_virtual_model_data(self):

        # Запрос для белого cpa оффера без модельки, все флаги проставлены
        params = [
            'rgb=blue&rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
            'rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
        ]
        for flags in params:
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-urls=direct,cpa&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                    virtual_model_id, flags
                )
            )

            # Инфа из виртуальной модельки
            self.assertFragmentIn(
                response,
                {
                    "product": {
                        "opinions": 44,
                        "rating": 4.3,
                        "preciseRating": 4.31,
                        "ratingCount": 43,
                        "reviews": 3,
                    }
                },
                allow_different_len=False,
            )

        # Запрос для белого cpa оффера без модельки, все флаги проставлены
        # с флагом market_cards_everywhere_sku_offers должны показывать даже нулеву информацию,
        # тк фронт в плейсе skuoffers хочет рейтинг в модельке
        params = [
            'rgb=blue&rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
            'rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
        ]
        for flags in params:
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-urls=direct,cpa&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(
                    virtual_model_id + 1, flags
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "product": {
                        "opinions": 0,
                        "rating": 0,
                        "preciseRating": 0,
                        "ratingCount": 0,
                        "reviews": 0,
                    }
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_fast_cards(cls):
        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=1580,
                opinion=Opinion(total_count=45, rating=4.5, precise_rating=4.51, rating_count=45, reviews=5),
            ),
        ]

        cls.index.shops += [
            Shop(fesh=12708, priority_region=213),
            Shop(fesh=12709, priority_region=213),
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
            Shop(fesh=12712, datafeed_id=14242, priority_region=213, regions=[213], client_id=14, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                fesh=12708,
                price=165,
                waremd5='offer_cpc_vmid_fc0__mQ',
                title='Оффер быстрокарточки 1580 cpc - 0',
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                hid=123,
            ),
            Offer(
                fesh=12709,
                price=175,
                waremd5='offer_cpc_vmid_fc1__mQ',
                title='Оффер быстрокарточки 1580 cpc - 1',
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                hid=123,
            ),
            Offer(
                fesh=12712,
                waremd5='offer_cpa_vmid_fc1__mQ',
                title='Оффер быстрокарточки 1580 cpa - 1',
                price=155,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14242],
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                hid=123,
            ),
            Offer(
                fesh=12711,
                waremd5='offer_blue_vmid_fc1_mQ',
                title='Оффер быстрокарточки 1580 blue - 1',
                price=350,
                delivery_buckets=[14241],
                sku=1580,
                virtual_model_id=1580,
                blue_without_real_sku=True,
                vmid_is_literal=False,
            ),
            fast_card_buybox_for_white,
            fast_card_buybox_for_blue,
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=123,
                friendlymodel=[
                    "{Check#ifnz}Быстрая{#endif}",
                ],
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Проверка": "{Check}",
                        },
                    ),
                ],
                seo="{return $Check; #exec}",
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=224, hid=123, gltype=GLType.BOOL, xslname="Check"),
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
            DeliveryBucket(
                bucket_id=14242,
                fesh=12712,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_fast_cards_sku_offers(self):
        '''
        Появились новые типы вуртуальных карточек - Быстрые карточки
        Их айдишник неотлечим от sku и лежит в нем же
        Но у офферов с скюшкой быстрых карточек в extraData лежит VirtualModelId == sku
        А литерала vmid нет
        Под флагом: use_fast_cards
        '''

        def create_expected_fast_sku(
            sku_id,
            model_id,
            ware_id,
            descr,
            offer_title,
            color,
            offer_count=0,
            need_full_product=True,
            min_p=0,
            max_p=0,
            avg_p=0,
        ):
            return {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "categories": [
                                {
                                    "entity": "category",
                                    "id": 123,
                                }
                            ],
                            "description": descr,
                            "entity": "sku",
                            "formattedDescription": {
                                "fullHtml": descr,
                                "fullPlain": descr,
                                "shortHtml": descr,
                                "shortPlain": descr,
                            },
                            "id": str(sku_id),
                            "isAdult": False,
                            "marketSkuCreator": "virtual",
                            "offers": {
                                "items": [
                                    {
                                        "cpa": "real",
                                        "description": descr,
                                        "entity": "offer",
                                        "marketSku": str(sku_id),
                                        "marketSkuCreator": "virtual",
                                        "sku": str(sku_id),
                                        "model": {"id": int(model_id)},
                                        "offerColor": color,
                                        "titles": {
                                            "highlighted": [
                                                {
                                                    "value": offer_title,
                                                }
                                            ],
                                            "raw": offer_title,
                                        },
                                        "urls": NotEmpty(),
                                        "wareId": ware_id,
                                    }
                                ],
                            },
                            "product": {
                                "categories": [
                                    {
                                        "id": 123,
                                    }
                                ],
                                "entity": "product",
                                "isVirtual": True,
                                "id": int(model_id),
                                "offers": {"count": offer_count, "cutPriceCount": 0},
                                "pictures": NotEmpty(),
                                "type": "model",
                                "opinions": 45,
                                "rating": 4.5,
                                "preciseRating": 4.51,
                                "ratingCount": 45,
                                "reviews": 5,
                                "prices": {
                                    "avg": str(avg_p),
                                    "currency": "RUR",
                                    "max": str(max_p),
                                    "min": str(min_p),
                                },
                            }
                            if need_full_product
                            else {"id": int(model_id)},
                            "specs": {
                                "friendly": ["Быстрая"],
                                "friendlyext": [{"type": "spec", "usedParams": [224], "value": "Быстрая"}],
                                "full": [
                                    {
                                        "groupName": "Технические характеристики",
                                        "groupSpecs": [
                                            {
                                                "desc": "Проверка parameter description",
                                                "name": "Проверка",
                                                "usedParams": [{"id": 224, "name": "GLPARAM-224"}],
                                                "value": "есть",
                                            }
                                        ],
                                    }
                                ],
                            },
                        }
                    ],
                }
            }

        # Делаем запрос за быстрой скю карточкой
        # Тут байбоксом будет синий оффер
        flags = 'rearr-factors=use_fast_cards=1'
        response = self.report.request_json(
            'place=sku_offers&market-sku=1580&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(flags)
        )
        self.assertFragmentIn(
            response,
            create_expected_fast_sku(
                sku_id=1580,
                model_id=1580,
                ware_id=fast_card_buybox_for_blue.waremd5,
                descr=fast_card_buybox_for_blue.description,
                offer_title=fast_card_buybox_for_blue.title,
                color='white',
                offer_count=6,
                min_p=150,
                max_p=350,
                avg_p=250,
            ),
            allow_different_len=False,
        )

        # Делаем запрос за быстрой скю карточкой c rgb == blue
        # Тут байбоксом будет самый дешевый cpa
        flags = 'rgb=blue&rearr-factors=use_fast_cards=1'
        response = self.report.request_json(
            'place=sku_offers&market-sku=1580&show-models=1&show-models-specs=msku-full,msku-friendly&{}'.format(flags)
        )
        self.assertFragmentIn(
            response,
            create_expected_fast_sku(
                sku_id=1580,
                model_id=1580,
                ware_id=fast_card_buybox_for_blue.waremd5,
                descr=fast_card_buybox_for_blue.description,
                offer_title=fast_card_buybox_for_blue.title,
                color='white',
                # тк cpc отфильтровались
                # Fочему-то на синем в модельных статистиках не учитывались dsbs, сейчас включил
                # Для БК в первую очередь важны синие, поэтому сейчас это не крит
                offer_count=4,
                min_p=150,
                max_p=350,
                avg_p=250,
            ),
            allow_different_len=False,
        )

        # Без флага будет пустая выдача
        response = self.report.request_json(
            'place=sku_offers&market-sku=1580&show-models=1&show-models-specs=msku-full,msku-friendly&rearr-factors=use_fast_cards=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "results": [],
                }
            },
        )

    def test_multi_combinations(self):
        """
        Проверям, что все ок:
        1) Запрос нескольких виртуальных sku
        2) Запрос виртуальных + обычных
        3) виртуальных + обычных + быстрых
        """
        params = [
            'rgb=blue&rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
            'rearr-factors=market_cards_everywhere_range={}:{}'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
        ]

        for flags in params:
            # Несколько виртуальных
            response = self.report.request_json(
                'place=sku_offers&market-sku={},{}&{}'.format(virtual_model_id, virtual_model_id + 1, flags)
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 2,
                    "results": [
                        {
                            "entity": "sku",
                            "id": str(virtual_model_id),
                        },
                        {
                            "entity": "sku",
                            "id": str(virtual_model_id + 1),
                        },
                    ],
                },
                allow_different_len=False,
            )

        # Виртуальные + обычные
        response = self.report.request_json(
            'place=sku_offers&market-sku={},{},{},{}&{}'.format(
                virtual_model_id, virtual_model_id + 1, msku_0.sku, msku_1.sku, flags
            )
        )
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {
                        "entity": "sku",
                        "id": str(virtual_model_id),
                    },
                    {
                        "entity": "sku",
                        "id": str(virtual_model_id + 1),
                    },
                    {
                        "entity": "sku",
                        "id": str(msku_0.sku),
                    },
                    {
                        "entity": "sku",
                        "id": str(msku_1.sku),
                    },
                ],
            },
            allow_different_len=False,
        )

        # Виртуальные + обычные + быстрые (под флагом)
        params = [
            'rgb=blue&rearr-factors=market_cards_everywhere_range={}:{};use_fast_cards=1'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
            'rearr-factors=market_cards_everywhere_range={}:{};use_fast_cards=1'.format(
                virtual_model_id_range_start, virtual_model_id_range_finish
            ),
        ]
        for flags in params:
            response = self.report.request_json(
                'place=sku_offers&market-sku={},{},{},{},{}&{}'.format(
                    virtual_model_id, virtual_model_id + 1, msku_0.sku, msku_1.sku, 1580, flags
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 5,
                    "results": [
                        {
                            "entity": "sku",
                            "id": str(virtual_model_id),
                        },
                        {
                            "entity": "sku",
                            "id": str(virtual_model_id + 1),
                        },
                        {
                            "entity": "sku",
                            "id": str(msku_0.sku),
                        },
                        {
                            "entity": "sku",
                            "id": str(msku_1.sku),
                        },
                        {"entity": "sku", "id": "1580"},
                    ],
                },
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()

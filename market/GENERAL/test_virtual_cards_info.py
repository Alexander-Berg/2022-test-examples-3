#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    Offer,
    Picture,
    HyperCategory,
    NavCategory,
    Shop,
    Vendor,
    VirtualModel,
    GLType,
    GLParam,
    DeliveryBucket,
    RegionalDelivery,
    DeliveryOption,
)
from core.types.picture import thumbnails_config
from core.matcher import NotEmpty


class T(TestCase):
    @classmethod
    def prepare_data(cls):
        cls.index.virtual_models += [
            VirtualModel(virtual_model_id=100500),
            VirtualModel(virtual_model_id=100501),
        ]

        cls.index.shops += [
            Shop(fesh=123, cpa=Shop.CPA_NO),
            Shop(fesh=1230, cpa=Shop.CPA_REAL),
        ]

        cls.index.vendors += [
            Vendor(
                vendor_id=1,
                name='Школа Волка',
                website="http://www.gwent.com/",
            )
        ]

        cls.index.hypertree += [HyperCategory(hid=10, name='Ведьмачье снаряжение')]

        cls.index.navtree += [NavCategory(nid=110, hid=10, name='Мастерское ведьмачье снаряжение')]

        cls.index.gltypes += [
            GLType(param_id=1, hid=10, gltype=GLType.NUMERIC, xslname="NumParam"),
            GLType(param_id=2, hid=10, gltype=GLType.ENUM, xslname="EnumParam", values=[1000, 1001]),
            GLType(param_id=3, hid=10, gltype=GLType.BOOL, xslname="BoolParam"),
        ]

        cls.index.offers += [
            Offer(
                virtual_model_id=100500,
                title='Меч из Школы Волка',
                title_no_vendor='Меч',
                descr='зачем тебе их два?',
                fesh=123,
                hid=10,
                vendor_id=1,
                glparams=[
                    GLParam(param_id=1, value=4),
                    GLParam(param_id=2, value=1000),
                    GLParam(param_id=3, value=1),
                ],
            ),
            Offer(
                virtual_model_id=100501,
                title='Доспех из Школы Волка',
                title_no_vendor='Доспех',
                descr='Может и их два возьмешь?',
                fesh=1230,
                hid=10,
                vendor_id=1,
                price=100,
                pictures=[
                    Picture(
                        picture_id='i0C4nHslqLtqZJLygVAHe0',
                        width=50,
                        height=10,
                        group_id=111,
                        namespace='marketpic',
                    ),
                    Picture(
                        picture_id='i1C4nHslqLtqZJLygVAHe1',
                        imagename='img_id1234567890.jpeg',
                        width=150,
                        height=250,
                        group_id=222,
                        namespace='mpic',
                    ),
                ],
                glparams=[
                    GLParam(param_id=1, value=5),
                    GLParam(param_id=2, value=1000),
                    GLParam(param_id=2, value=1001),
                    GLParam(param_id=3, value=0),
                ],
            ),
        ]

    def test_virtual_cards_info(self):
        """
        Проверяем плейс, который отдает инфу о виртуальных и быстрых карточках для content-storage
        Сейчас он отдает:
        1) Айдишник
        2) Инфу о категориях
        3) Тайтлы
        4) Сырое описание
        5) Картинки
        6) Параметры

        Айддишники виртульных карточек передаются в параметре hyperid
        """

        response = self.report.request_json(
            'place=virtual_cards_info&hyperid=100500,100501&rearr-factors=market_cards_everywhere_range=100500:100510'
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "card_id": 100500,
                    "hid": 10,
                    "nid": 110,
                    "raw_description": "зачем тебе их два?",
                    "title": "Меч из Школы Волка",
                    "title_no_vendor": "Меч",
                    "vendor_id": 1,
                    "pictures": NotEmpty(),
                    "params": [
                        {"id": 1, "type": "numeric", "value_num": 4},
                        {"id": 2, "type": "enum", "value_id": 1000, "values": [1000]},
                        {"id": 3, "type": "bool", "value_bool": True},
                    ],
                },
                {
                    "card_id": 100501,
                    "hid": 10,
                    "nid": 110,
                    "raw_description": "Может и их два возьмешь?",
                    "title": "Доспех из Школы Волка",
                    "title_no_vendor": "Доспех",
                    "vendor_id": 1,
                    "pictures": [
                        {
                            "groupId": 111,
                            "height": 10,
                            "width": 50,
                            "key": "market_i0C4nHslqLtqZJLygVAHew",
                            "namespace": "marketpic",
                        },
                        {
                            "groupId": 222,
                            "height": 250,
                            "width": 150,
                            "key": "img_id1234567890.jpeg",
                            "namespace": "mpic",
                        },
                    ],
                    "params": [
                        {"id": 1, "type": "numeric", "value_num": 5},
                        {"id": 2, "type": "enum", "value_id": 1000, "values": [1000, 1001]},
                        {"id": 3, "type": "bool", "value_bool": False},
                    ],
                },
            ],
        )

        # Несуществующая карточка
        response = self.report.request_json(
            'place=virtual_cards_info&hyperid=100502&rearr-factors=market_cards_everywhere_range=100500:100510'
        )
        self.assertFragmentIn(response, [])

    @classmethod
    def prepare_fast_cards(cls):
        cls.index.virtual_models += [
            VirtualModel(virtual_model_id=1580),
            VirtualModel(virtual_model_id=1581),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=142, name="Гвинт", website="https://www.gwent.com"),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=11),
        ]

        cls.index.navtree += [NavCategory(nid=111, hid=11, name='Гвинтокарты')]

        cls.index.gltypes += [
            GLType(param_id=101, hid=11, gltype=GLType.NUMERIC, xslname="NumParam 11"),
            GLType(param_id=202, hid=11, gltype=GLType.ENUM, xslname="EnumParam 11", values=[100, 101]),
            GLType(param_id=303, hid=11, gltype=GLType.BOOL, xslname="BoolParam 11"),
        ]

        cls.index.shops += [
            Shop(fesh=12710, datafeed_id=14240, priority_region=213, regions=[213], client_id=12, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                waremd5='OfferFastModel0CPC___g',
                title="Геральт cpc",
                title_no_vendor='Геральт (no vendor)',
                descr='ведьмак',
                fesh=213,
                vendor_id=142,
                hid=11,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[
                    GLParam(param_id=101, value=99),
                    GLParam(param_id=202, value=101),
                    GLParam(param_id=202, value=100),
                    GLParam(param_id=303, value=1),
                ],
            ),
            Offer(
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                waremd5='OfferFastModel0CPA___g',
                title="Геральт cpa",
                fesh=12710,
                vendor_id=142,
                hid=11,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=300,
                    height=300,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14240],
            ),
            Offer(
                sku=1581,
                virtual_model_id=1581,
                vmid_is_literal=False,
                waremd5='OfferFastModel1BLUE__g',
                title='Геральт синий',
                title_no_vendor='Геральт синий (no vendor)',
                descr='Геральт из беру.ру',
                fesh=12711,
                vendor_id=142,
                hid=11,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=400,
                    height=400,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                glparams=[
                    GLParam(param_id=101, value=999),
                    GLParam(param_id=303, value=0),
                ],
                delivery_buckets=[14241],
                blue_without_real_sku=True,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=14240,
                fesh=12710,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_fast_cards_info(self):
        """
        Проверяем, что плейс возвращает инфу и для быстрых карточек

        Айддишники быстрых карточек передаются в параметре market-sku
        """

        # Только быстрые
        response = self.report.request_json(
            'place=virtual_cards_info&market-sku=1580,1581&rearr-factors=market_cards_everywhere_range=100500:100510'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "card_id": 1580,
                    "hid": 11,
                    "nid": 111,
                    "raw_description": "ведьмак",
                    "title": "Геральт cpc",
                    "title_no_vendor": "Геральт (no vendor)",
                    "vendor_id": 142,
                    "pictures": NotEmpty(),
                    "params": [
                        {"id": 101, "type": "numeric", "value_num": 99},
                        {"id": 202, "type": "enum", "value_id": 100, "values": [100, 101]},
                        {"id": 303, "type": "bool", "value_bool": True},
                    ],
                },
                {
                    "card_id": 1581,
                    "hid": 11,
                    "nid": 111,
                    "raw_description": 'Геральт из беру.ру',
                    "title": 'Геральт синий',
                    "title_no_vendor": 'Геральт синий (no vendor)',
                    "vendor_id": 142,
                    "pictures": NotEmpty(),
                    "params": [
                        {"id": 101, "type": "numeric", "value_num": 999},
                        {"id": 303, "type": "bool", "value_bool": False},
                    ],
                },
            ],
        )

        # Быстрая и виртуальная
        response = self.report.request_json(
            'place=virtual_cards_info&hyperid=100500&market-sku=1580&rearr-factors=market_cards_everywhere_range=100500:100510'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "card_id": 100500,
                    "hid": 10,
                    "nid": 110,
                    "raw_description": "зачем тебе их два?",
                    "title": "Меч из Школы Волка",
                    "title_no_vendor": "Меч",
                    "vendor_id": 1,
                    "pictures": NotEmpty(),
                    "params": [
                        {"id": 1, "type": "numeric", "value_num": 4},
                        {"id": 2, "type": "enum", "value_id": 1000, "values": [1000]},
                        {"id": 3, "type": "bool", "value_bool": True},
                    ],
                },
                {
                    "card_id": 1580,
                    "hid": 11,
                    "nid": 111,
                    "raw_description": "ведьмак",
                    "title": "Геральт cpc",
                    "title_no_vendor": "Геральт (no vendor)",
                    "vendor_id": 142,
                    "pictures": NotEmpty(),
                    "params": [
                        {"id": 101, "type": "numeric", "value_num": 99},
                        {"id": 202, "type": "enum", "value_id": 100, "values": [100, 101]},
                        {"id": 303, "type": "bool", "value_bool": True},
                    ],
                },
            ],
        )

        # Несуществующая быстрая
        # Несуществующая карточка
        response = self.report.request_json(
            'place=virtual_cards_info&market-sku=1589&rearr-factors=market_cards_everywhere_range=100500:100510'
        )
        self.assertFragmentIn(response, [])


if __name__ == '__main__':
    main()

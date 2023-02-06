#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, MarketSku, Model, Offer, Shop
from core.matcher import Absent, Contains, NotEmpty


shop_blue = Shop(
    fesh=103,
    datafeed_id=1031,
    priority_region=213,
    name='Поставщик верстаков #1',
    client_id=12,
    cpa=Shop.CPA_REAL,
    blue=Shop.BLUE_REAL,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.models += [
            # Категория 90741 - в вайт листе для нулевого сплита, в котором расхлопываем все модели категорий
            # Категория 90409 - в бан листе эксперимента
            Model(title='red tv', hid=90741, hyperid=1721682874),  # сплит 60%
            Model(title='green tv', hid=3, hyperid=484649044),  # сплит 40%
            Model(title='gray tv', hid=3, hyperid=744732263),  # сплит 20%
            Model(title='blue tv', hid=90741, hyperid=130),  # не входит в выборку и категория не в бан группе
            Model(title='yellow tv', hid=90409, hyperid=100500),  # не входит в выборку, но категория в бан группе
            # Модельки для синих офферов
            Model(title='game blue 0', hid=90741, hyperid=478405028),  # сплит 40%
            Model(title='game blue 1', hid=90741, hyperid=800866352),  # сплит 60%
        ]

        cls.index.offers += [
            Offer(title="super red tv", hid=90741, hyperid=1721682874, offerid=300, fesh=10774, feedid=200),
            Offer(title="super puper red tv", hid=90741, hyperid=1721682874, offerid=301, fesh=10775, feedid=201),
            Offer(title="super green tv", hid=3, hyperid=484649044, offerid=400, fesh=10389, feedid=300),
            Offer(title="super gray tv", hid=3, hyperid=744732263, offerid=500, fesh=11774, feedid=400),
            Offer(title="super puper gray tv", hid=3, hyperid=744732263, offerid=501, fesh=11775, feedid=401),
            Offer(title="super blue tv", hid=90741, hyperid=130, fesh=10130, offerid=600),
            Offer(title="super yellow tv", hid=90409, hyperid=100500, offerid=700, fesh=12774, feedid=501),
            Offer(
                title="tv without model", hid=3, offerid=100137, auto_creating_model=False, fesh=10100
            ),  # оффер без модельки
        ]

        cls.index.mskus += [
            # сплит 40%
            MarketSku(
                title="game msku 1",
                hyperid=478405028,
                sku=101,
                blue_offers=[
                    BlueOffer(
                        fesh=shop_blue.fesh,
                        feedid=1000,
                        offerid='Shop1_sku_10',
                        waremd5='Blue100______________Q',
                        price=100,
                        title='game blue 100',
                    ),
                    BlueOffer(
                        fesh=shop_blue.fesh,
                        feedid=1001,
                        offerid='Shop1_sku_11',
                        waremd5='Blue101______________Q',
                        price=200,
                        title='game blue 101',
                    ),
                ],
            ),
            # сплит 60%
            MarketSku(
                title="game msku 2",
                hyperid=800866352,
                sku=201,
                blue_offers=[
                    BlueOffer(
                        fesh=shop_blue.fesh,
                        feedid=2000,
                        offerid='Shop1_sku_20',
                        waremd5='Blue200______________Q',
                        price=100,
                        title='game blue 200',
                    ),
                    BlueOffer(
                        fesh=shop_blue.fesh,
                        feedid=2001,
                        offerid='Shop1_sku_21',
                        waremd5='Blue201______________Q',
                        price=200,
                        title='game blue 201',
                    ),
                ],
            ),
        ]

    def test_collapsed_models(self):
        # Возможные сплиты - {0%, 20%, 40%, 60%}
        # Соответствующие значения флага market_collapse_from_list = {99, 1, 2, 4}

        # Во всех тестах ниже моделька 100500 должна схлопываться, тк ее категория в бан группе
        # Запрашиваем сплит 60% => должна схлопнуться 1721682874

        response = self.report.request_json(
            'place=prime&text=tv&allow-collapsing=1&rearr-factors=market_collapse_from_list=4'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 100500, "offers": {"count": 1}},
                    {"entity": "product", "id": 1721682874, "offers": {"count": 2}},
                    {
                        "entity": "offer",
                        "model": {"id": 484649044},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "400"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 744732263},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "500"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 744732263},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "501"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 130},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "600"}},
                    },
                    {
                        "entity": "offer",
                        "model": Absent(),
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "100137"}},
                    },
                ]
            },
            allow_different_len=False,
        )

        # Запрашиваем сплит 40% => схлопнется 484649044
        response = self.report.request_json(
            'place=prime&text=tv&allow-collapsing=1&rearr-factors=market_collapse_from_list=2'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 100500, "offers": {"count": 1}},
                    {
                        "entity": "offer",
                        "model": {"id": 1721682874},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "300"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1721682874},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "301"}},
                    },
                    {"entity": "product", "id": 484649044, "offers": {"count": 1}},
                    {
                        "entity": "offer",
                        "model": {"id": 744732263},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "500"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 744732263},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "501"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 130},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "600"}},
                    },
                    {
                        "entity": "offer",
                        "model": Absent(),
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "100137"}},
                    },
                ]
            },
            allow_different_len=False,
        )

        # Запрашиваем 20% => схлопнется 744732263
        response = self.report.request_json(
            'place=prime&text=tv&allow-collapsing=1&rearr-factors=market_collapse_from_list=1&debug=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        # print(response)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 100500, "offers": {"count": 1}},
                    {
                        "entity": "offer",
                        "model": {"id": 1721682874},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "300"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1721682874},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "301"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 484649044},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "400"}},
                    },
                    {"entity": "product", "id": 744732263, "offers": {"count": 2}},
                    {
                        "entity": "offer",
                        "model": {"id": 130},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "600"}},
                    },
                    {
                        "entity": "offer",
                        "model": Absent(),
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "100137"}},
                    },
                ]
            },
            allow_different_len=False,
        )

        # Проверка, что используется атрибут _virtual99
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("[ME]", "Apply", "www.shop-10774", "g:_virtual99", "ModeId:1721682874"),
                    Contains("[ME]", "Apply", "www.shop-10774", "g:dsrcid", "ModeId:1721682874"),
                    Contains("[ME]", "Apply", "www.shop-10130", "g:_virtual99", "ModeId:130"),
                    Contains("[ME]", "Apply", "www.shop-10130", "g:dsrcid", "ModeId:130"),
                    Contains("[ME]", "Apply", "product/744732263", "g:yg"),
                ]
            },
            allow_different_len=True,
        )

        # Соответственно hyper_ts совсем должен отсутсвовать
        self.assertFragmentNotIn(
            response,
            {
                "logicTrace": [
                    Contains("[ME]", "Apply", "g:hyper_ts"),
                ]
            },
        )

        # Запрашиваем сплит 0% => все, у кого категория 90741, расхлопываются (130, 1721682874)
        # Тк в нем расхлопываются все модели категорий из вайт листа
        response = self.report.request_json(
            'place=prime&text=tv&allow-collapsing=1&rearr-factors=market_collapse_from_list=99'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 100500, "offers": {"count": 1}},
                    {
                        "entity": "offer",
                        "model": {"id": 1721682874},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "300"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 1721682874},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "301"}},
                    },
                    {"entity": "product", "id": 484649044, "offers": {"count": 1}},
                    {"entity": "product", "id": 744732263, "offers": {"count": 2}},
                    {
                        "entity": "offer",
                        "model": {"id": 130},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "600"}},
                    },
                    {
                        "entity": "offer",
                        "model": Absent(),
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "100137"}},
                    },
                ]
            },
            allow_different_len=False,
        )

        # Запрос без флага - все модельки схлопываются
        response = self.report.request_json('place=prime&text=tv&allow-collapsing=1&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 100500, "offers": {"count": 1}},
                    {"entity": "product", "id": 1721682874, "offers": {"count": 2}},
                    {"entity": "product", "id": 484649044, "offers": {"count": 1}},
                    {"entity": "product", "id": 744732263, "offers": {"count": 2}},
                    {"entity": "product", "id": 130, "offers": {"count": 1}},
                    {
                        "entity": "offer",
                        "model": Absent(),
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "100137"}},
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_collapsed(self):
        # Проеряем, что синие оффера тоже схлопываются и расхлопываются

        # Запрос без флага
        response = self.report.request_json(
            'place=prime&text=game&allow-collapsing=1&rearr-factors=market_collapse_from_list=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 478405028, "offers": {"count": 2}},
                    {"entity": "product", "id": 800866352, "offers": {"count": 2}},
                ]
            },
            allow_different_len=False,
        )

        # сплит 40% - схлопывается 478405028
        response = self.report.request_json(
            'place=prime&text=game&allow-collapsing=1&rearr-factors=market_collapse_from_list=2'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 478405028, "offers": {"count": 2}},
                    {
                        "entity": "offer",
                        "model": {"id": 800866352},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "2000.Shop1_sku_20"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 800866352},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "2001.Shop1_sku_21"}},
                    },
                ]
            },
            allow_different_len=False,
        )

        # сплит 60% - схлопывается 800866352
        response = self.report.request_json(
            'place=prime&text=game&allow-collapsing=1&rearr-factors=market_collapse_from_list=4'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "model": {"id": 478405028},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "1000.Shop1_sku_10"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 478405028},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "1001.Shop1_sku_11"}},
                    },
                    {"entity": "product", "id": 800866352, "offers": {"count": 2}},
                ]
            },
            allow_different_len=False,
        )

        # сплит 0% - расхлопываются все, тк категория в вайл листе
        response = self.report.request_json(
            'place=prime&text=game&allow-collapsing=1&rearr-factors=market_collapse_from_list=99'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "model": {"id": 478405028},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "1000.Shop1_sku_10"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 478405028},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "1001.Shop1_sku_11"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 800866352},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "2000.Shop1_sku_20"}},
                    },
                    {
                        "entity": "offer",
                        "model": {"id": 800866352},
                        "urls": {"encrypted": NotEmpty()},
                        "shop": {"feed": {"offerId": "2001.Shop1_sku_21"}},
                    },
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()

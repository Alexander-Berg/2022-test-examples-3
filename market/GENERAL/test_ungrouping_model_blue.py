#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.types import BlueOffer, Currency, MarketSku, MnPlace, Model, Picture, Shop, Tax
from core.testcase import TestCase, main
from core.types.model import UngroupedModel
from core.types.picture import thumbnails_config


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']

        pic = Picture(
            picture_id="KdwwrYb4czANgt9-3poEQQ",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )
        pic2 = Picture(
            picture_id="KdwwrYb4caANgt9-3poEQQ",
            width=400,
            height=700,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='supplier_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue='REAL',
                supplier_type=Shop.FIRST_PARTY,
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title="Исходная модель 1",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=1,
                        title="Расхлопнутая модель 1.1",
                        key='1_1',
                    ),
                    UngroupedModel(
                        group_id=2,
                        title="Расхлопнутая модель 1.2",
                        key='1_2',
                    ),
                ],
            ),
            Model(
                hyperid=2,
                hid=2,
                title="Исходная модель 2",
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                fesh=1,
                title='ungrouped_sku 1.1 cheap',
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(price=5, feedid=2, ts=111, waremd5='Ws3Jyl2Zrmav3-HuoOOyaw'),
                ],
                ungrouped_model_blue=1,
                picture=pic,
            ),
            MarketSku(
                fesh=1,
                title='ungrouped_sku 1.1 expensive&relevant',
                hyperid=1,
                sku=2,
                blue_offers=[
                    BlueOffer(price=6, feedid=2, ts=222, waremd5='FoCCkpPyqFQe-iZHoCkPiA'),
                ],
                ungrouped_model_blue=1,
                picture=pic,
            ),
            MarketSku(
                fesh=1,
                title='ungrouped_sku 1.2',
                hyperid=1,
                sku=3,
                blue_offers=[
                    BlueOffer(price=15, feedid=2, ts=333, waremd5='tbqpwjrD4BWtKHsXo9Svow'),
                ],
                ungrouped_model_blue=2,
                picture=pic2,
            ),
        ]

        # более дорогой оффер - более релевантный, а значит будет выше в выдаче и именно он схлопнется в модель/скушку
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 111).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 222).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 333).respond(0.3)

    def test_uncollapsing(self):
        """
        Что проверяем: расхлопывание на синем маркете по текстовому запросу
        Модель 1 имеет две группы, которые будут отображаться раздельно, даже при включенном схлопывании.
        """
        request = 'place=prime&text=ungrouped_sku&rgb=blue'
        # Включено расхлопывание - на выдаче два варианта одной модели
        # По умолчанию сортировка по цене в рамках группы выключена. Берется наиболее релевантный офер
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1,
                        "titles": {"raw": "Исходная модель 1"},
                        "offers": {
                            'items': [
                                {
                                    "entity": "offer",
                                    "titles": {"raw": "ungrouped_sku 1.1 expensive&relevant"},
                                    "modelAwareTitles": {"raw": "Расхлопнутая модель 1.1"},
                                    "wareId": "FoCCkpPyqFQe-iZHoCkPiA",
                                    "prices": {"value": "6"},
                                }
                            ]
                        },
                    },
                    {
                        "entity": "product",
                        "id": 1,
                        "titles": {"raw": "Исходная модель 1"},
                        "offers": {
                            'items': [
                                {
                                    "entity": "offer",
                                    "titles": {"raw": "ungrouped_sku 1.2"},
                                    "modelAwareTitles": {"raw": "Расхлопнутая модель 1.2"},
                                    "wareId": "tbqpwjrD4BWtKHsXo9Svow",
                                    "prices": {"value": "15"},
                                }
                            ]
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        """Проверяем что за title пишется в show-log"""
        self.show_log.expect(ware_md5='tbqpwjrD4BWtKHsXo9Svow', title='Исходная модель 1').once()
        self.show_log.expect(ware_md5='FoCCkpPyqFQe-iZHoCkPiA', title='Исходная модель 1').once()

    def test_without_ungrouping(self):
        # Под флагом dsk_product_ungroup=old: показывается только одна модель
        # для нее в качестве ДО выбран наиболее релевантный оффер
        request = 'place=prime&text=ungrouped_sku&rgb=blue'
        response = self.report.request_json(request + "&rearr-factors=dsk_product_ungroup=old")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1,
                        "titles": {"raw": "Исходная модель 1"},
                        "offers": {
                            'items': [
                                {
                                    "entity": "offer",
                                    "titles": {"raw": "ungrouped_sku 1.2"},
                                    "modelAwareTitles": {"raw": "ungrouped_sku 1.2"},
                                    "wareId": "tbqpwjrD4BWtKHsXo9Svow",
                                    "prices": {"value": "15"},
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_group_attribute(self):
        """
        Что проверяем: используемый для расхлапывания группировочный атрибут
        """
        request = 'place=prime&text=ungrouped_sku&rgb=blue&debug=da&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"g": ["1._virtual98.100.1.-1"]})

        response = self.report.request_json(request + "&rearr-factors=dsk_product_ungroup=old")
        self.assertFragmentIn(response, {"g": ["1._virtual98.100.1.-1"]})

    def test_paging_on_white_market(self):
        """
        Проверяем, что на белый маркет синее расхлопывание не повлияло
        MARKETOUT-22896
        Была проблема, что неправильно считалось количество документов на выдаче
        """
        request = "place=prime&text=ungrouped_sku&allow-collapsing=1" "&rearr-factors=market_metadoc_search=no"

        # На белом маркете только один документ
        response = self.report.request_json(request + "&rgb=green")
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "totalOffers": 0,
                "totalOffersBeforeFilters": 3,
                "totalModels": 1,
            },
        )

        # На синем маркете есть расхлопывание
        response = self.report.request_json(request + "&rgb=blue")
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "totalOffers": 0,
                "totalOffersBeforeFilters": 3,
                "totalModels": 2,
            },
        )

    def test_prime_super_uid_in_show_log(self):
        """
        Проверяем связь показов офера и модели через super_id
        Для модели показывается ware_md5 его ДО и super_uid - show_uid этого же офера
        """
        show_uid = '048841920011177788888{}{}'
        offer1_show_uid = show_uid.format('06', '001')
        model1_show_uid = show_uid.format('16', '001')
        offer2_show_uid = show_uid.format('06', '002')
        model2_show_uid = show_uid.format('16', '002')

        offer1_ware_md5 = 'tbqpwjrD4BWtKHsXo9Svow'
        offer2_ware_md5 = 'FoCCkpPyqFQe-iZHoCkPiA'

        _ = self.report.request_json('place=prime&text=ungrouped_sku&rgb=blue&show-urls=external,cpa&')

        self.show_log.expect(
            ware_md5=offer1_ware_md5, show_uid=offer1_show_uid, super_uid=offer1_show_uid, supplier_id=2
        )  # offer
        self.show_log.expect(ware_md5=offer1_ware_md5, show_uid=model1_show_uid, super_uid=offer1_show_uid)  # model

        self.show_log.expect(
            ware_md5=offer2_ware_md5, show_uid=offer2_show_uid, super_uid=offer2_show_uid, supplier_id=2
        )  # offer
        self.show_log.expect(ware_md5=offer2_ware_md5, show_uid=model2_show_uid, super_uid=offer2_show_uid)  # model


if __name__ == '__main__':
    main()

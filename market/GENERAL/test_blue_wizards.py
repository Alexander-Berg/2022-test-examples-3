#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    Tax,
)
from core.testcase import TestCase, main

from core.matcher import (
    NotEmpty,
    NoKey,
    LikeUrl,
    Contains,
    Absent,
    Not,
    Round,
)

import json

INVALID_NAVIGATION_CATEGORY_ID = 18446744073709551615


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    @classmethod
    def prepare_blue_market_in_offers_wizard(cls):
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1770).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1771).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1772).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1773).respond(0.09)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1774).respond(0.08)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1775).respond(0.07)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1776).respond(0.06)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1777).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1778).respond(0.04)

        cls.index.shops += [
            Shop(
                fesh=1777,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                pickup_buckets=[5001],
                datafeed_id=100,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
                work_schedule='work schedule supplier 3',
            ),
        ]

        cls.index.models += [
            Model(hyperid=101777, hid=1777, title='buravchik blue & white model'),
            Model(hyperid=102777, hid=1777, title='buravchik blue only model', rgb_type=Model.RGB_BLUE),
            Model(hyperid=103777, hid=1777, title='buravchik white only model'),
        ]

        sku1_offer1 = BlueOffer(price=5, offerid='Shop1_sku1', waremd5='Sku1Price5-IiLVm1Goleg', ts=1770, feedid=3)
        sku1_offer2 = BlueOffer(price=5, offerid='Shop1_sku2', waremd5='Sku2Price5-IiLVm1Goleg', ts=1771, feedid=3)
        sku1_offer3 = BlueOffer(price=50, offerid='Shop2_sku1', waremd5='Sku3Price50-iLVm1Goleg', ts=1772, feedid=3)
        sku1_offer4 = BlueOffer(price=50, offerid='Shop2_sku2', waremd5='Sku4Price50-iLVm1Goleg', ts=1773, feedid=3)
        sku1_offer5 = BlueOffer(price=50, offerid='Shop2_sku2', waremd5='Sku5Price50-iLVm1Goleg', ts=1774, feedid=3)
        sku1_offer6 = BlueOffer(price=50, offerid='Shop2_sku2', waremd5='Sku6Price50-iLVm1Goleg', ts=1775, feedid=3)

        cls.index.mskus += [
            MarketSku(
                title="buravchik buravchiksku sku 1",
                hyperid=101777,
                sku=1017770001,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[sku1_offer1],
            ),
            MarketSku(
                title="buravchik buravchiksku sku 2",
                hyperid=101777,
                sku=1017770002,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[sku1_offer2],
            ),
            MarketSku(
                title="buravchik buravchiksku blue sku 1",
                hyperid=102777,
                sku=1027770001,
                waremd5='Sku3-wdDXWsIiLVm1goleg',
                blue_offers=[sku1_offer3],
            ),
            MarketSku(
                title="buravchik buravchiksku blue sku 2",
                hyperid=102777,
                sku=1027770002,
                waremd5='Sku4-wdDXWsIiLVm1goleg',
                blue_offers=[sku1_offer4],
            ),
            MarketSku(
                title="buravchik buravchiksku blue sku 3",
                hyperid=102777,
                sku=1027770003,
                waremd5='Sku5-wdDXWsIiLVm1goleg',
                blue_offers=[sku1_offer5],
            ),
            MarketSku(
                title="buravchik buravchiksku blue sku 4",
                hyperid=102777,
                sku=1027770004,
                waremd5='Sku6-wdDXWsIiLVm1goleg',
                blue_offers=[sku1_offer6],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=101777, title="buravchik 1", waremd5="AfFQXGZRcq-zCQLA1DpAtg", ts=1776),
            Offer(hyperid=101777, title="buravchik 2", waremd5="BfFQXGZRcq-zCQLA1DpAtg", ts=1777),
        ]

        # для блока geo
        cls.index.regiontree += [Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в')]
        cls.index.outlets += [
            Outlet(fesh=1777, region=213, point_id=1),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1777,
                carriers=[103],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[]),
            DynamicDaysSet(key=2, days=[0, 1, 2, 5, 6, 14, 20, 21, 27, 28]),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=2),
            DynamicDeliveryServiceInfo(
                103,
                "103",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=103,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=225)],
            ),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]

        cls.index.offers += [
            Offer(title="buravchik white", hyperid=103777, waremd5="d_cvMFtlIDImYfwkDbpbWA", ts=1778, fesh=2777),
        ]

    def test_blue_market_in_offers_wizard_on_empty_incut(self):
        """https://st.yandex-team.ru/MARKETOUT-18154"""

        # На синем Маркете по запросу находится 2 модели
        response = self.report.request_json('place=prime&text=buravchiksku&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "totalModels": 2,
                }
            },
        )

        # На параллельном формируется офферный колдунщик без врезки
        # market_offers_wiz_top_offers_threshold=0 нужен, чтобы оферный не отфильтровывался по топ-4
        response = self.report.request_bs_pb(
            'place=parallel&text=buravchiksku&rearr-factors=market_offers_wiz_top_offers_threshold=0;'
        )
        self.assertFragmentIn(response, {"market_offers_wizard": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

    @classmethod
    def prepare_blue_model_wizard(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=185467004,
                name='Asian stranges',
                children=[
                    HyperCategory(hid=185467002, name='Japanese stranges'),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(nid=185467001, hid=185467002, is_blue=False),
            NavCategory(nid=185467003, hid=185467004, is_blue=True),
        ]

        cls.index.models += [
            Model(
                hyperid=185467,
                hid=185467002,
                title="kumamon suga",
                has_blue_offers=True,
                opinion=Opinion(rating=4.5, rating_count=10, total_count=12),
                description='this is kumamon',
            ),
            Model(hyperid=185468, title="hisatsu orange", has_blue_offers=True),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=18546700,
                title="kumamon suga",
                hyperid=185467,
                blue_offers=[BlueOffer(price=1854671, feedid=3), BlueOffer(price=1854672, feedid=3)],
            ),
            MarketSku(
                sku=18546800,
                title="hisatsu orange",
                hyperid=185468,
                blue_offers=[BlueOffer(price=1854681, feedid=3), BlueOffer(price=1854681)],
            ),
        ]

    @classmethod
    def prepare_blue_implicit_model_wizard(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=19863002,
                name='Оптика',
                children=[
                    HyperCategory(hid=19863004, name='Контактные линзы 6', tovalid=1986304),
                    HyperCategory(hid=19863006, name='Контактные линзы 12', tovalid=1986306),
                    HyperCategory(hid=19863008, name='Контактные линзы 24', tovalid=1986308),
                ],
            ),
        ]
        cls.index.navtree += [
            NavCategory(nid=19863001, hid=19863002, is_blue=True),
            NavCategory(nid=19863003, hid=19863004, is_blue=False),
            NavCategory(nid=19863005, hid=19863006, is_blue=True),
            NavCategory(nid=19863007, hid=19863008, is_blue=True),
        ]

        cls.index.models += [
            Model(
                hyperid=198631,
                hid=19863004,
                vendor_id=19863001,
                ts=198631000,
                title="Acuvue OASYS with Hydraclear Plus (6 линз)",
            ),
            Model(
                hyperid=198632,
                hid=19863006,
                vendor_id=19863002,
                ts=198632000,
                title="Acuvue OASYS with Hydraclear Plus (12 линз)",
                has_blue_offers=True,
                opinion=Opinion(rating=3.5, rating_count=17, total_count=19),
            ),
            Model(
                hyperid=198633,
                hid=19863008,
                vendor_id=19863003,
                ts=198633000,
                title="Acuvue OASYS with Hydraclear Plus (24 линзы)",
                has_blue_offers=True,
            ),
            # Модели, которые не будут находиться, но их офферы будут учитываться в синих статистиках
            Model(hyperid=198634, hid=19863008, ts=198634000, title="Какие-то линзы 24 шт"),
            Model(hyperid=198635, hid=19863004, ts=198635000, title="Какие-то линзы 6 шт"),
        ]

        # фиксируем порядок
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 198631000).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 198632000).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 198633000).respond(0.3)
        # фиксируем порядок Синих моделей при переранжировании перед мета-классификатором
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META_RANK, 198632).respond(0.1)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META_RANK, 198633).respond(0.2)

        for i in range(20):
            cls.index.offers += [
                Offer(title='Acuvue OASYS 6', hid=19863004),
            ]
        for i in range(30):
            cls.index.offers += [
                Offer(title='Acuvue OASYS 12', hid=19863006),
            ]

        # Белые категорийные статистики. Баг 1: учитываются синие. Баг 2: учитывается модель.
        # 6 линз — 20 (белых без модели) + 7 (синих) + 1 (модель) = 28
        # 12 линз — 30 (белых без модели) + 1 (синих) + 1 (модель) = 32
        # 24 линз — 0 (белых без модели) + 6 (синих) + 1 (модель) = 7
        cls.index.offers += [
            Offer(hyperid=198631, price=234),  # 6 линз
            Offer(hyperid=198632, price=123),  # 12 линз
            # для hyperid=198633 оффера нет, поэтому она не попадет в белый колдунщик
        ]

        # Синие категорийные статистики (считаются msku)
        # 6 линз — 7 (sku=198635X)
        # 12 линз — 1 (sku=1986320)
        # 24 линз — 6 (sku=1986330 + sku=198634X)
        cls.index.mskus += [
            # для hyperid=198631 не будет синих офферов, поэтому она не попадет в синий колдунщик
            MarketSku(
                sku=1986320,
                title="Acuvue OASYS 12",
                hyperid=198632,
                blue_offers=[BlueOffer(price=1986301, feedid=3), BlueOffer(price=1986303, feedid=3)],
            ),
            MarketSku(
                sku=1986330,
                title="Acuvue OASYS 24",
                hyperid=198633,
                blue_offers=[BlueOffer(price=1986302, feedid=3), BlueOffer(price=1986304, feedid=3)],
            ),
        ]
        for i in range(5):
            cls.index.mskus += [
                MarketSku(
                    sku='198634{0}'.format(i),
                    title="На самом деле очень похожи на Acuvue OASYS 24",
                    hyperid=198634,
                    blue_offers=[BlueOffer(feedid=3)],
                ),
            ]
        for i in range(7):
            cls.index.mskus += [
                MarketSku(
                    sku='198635{0}'.format(i),
                    title="На самом деле очень похожи на Acuvue OASYS 6",
                    hyperid=198635,
                    blue_offers=[BlueOffer(feedid=3)],
                ),
            ]

    def test_implicit_model_wizard_with_blue_incut_url_encryption(self):
        """Отдельный тип кликурлов для ссылок на Беру из синей врезки
        колдунщика неявной модели (тайтл и модели)
        https://st.yandex-team.ru/MARKETOUT-25622
        """

        request = 'place=parallel&text=acuvue+oasys&rearr-factors='
        disable_nailed_docs = (
            '&rearr-factors=market_implicit_model_wizard_nailed_models_count=0;'
            'market_offers_wizard_nailed_offers_count=0;'
        )
        response = self.report.request_bs(
            request + 'market_implicit_blue_incut_model_count=3;'
            'market_blue_incut_encrypt_urls=1;market_blue_incut_model_click_price=100500;'
            'market_blue_incut_title_click_price=100501'
            '&yandexuid=123456789&wprid=987654321&rids=213&ip-rids=54'
            '&pof=ololo&test-buckets=12,34,56&reqid=13579&x-yandex-icookie=2222222222' + disable_nailed_docs
        )

        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {
                        "url": LikeUrl.of("//market.yandex.ru/search?text=acuvue%20oasys&clid=698"),
                        "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=acuvue%20oasys&clid=721"),
                        "blueUrl": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                        "blueUrlTouch": LikeUrl.of("//m.beru.ru/search?text=acuvue%20oasys&clid=721"),
                        "showcase": {
                            "items": [
                                {
                                    "is_blue": NoKey("is_blue"),
                                    "title": {
                                        "text": {
                                            "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                        },
                                        "url": LikeUrl.of(
                                            "//market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-12-linz/198632?hid=19863006&nid=19863005&clid=698"
                                        ),
                                    },
                                },
                                {
                                    "is_blue": "1",
                                    "title": {
                                        "text": {
                                            "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                        },
                                        "url": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                    },
                                },
                                {
                                    "is_blue": "1",
                                    "title": {
                                        "text": {
                                            "__hl": {
                                                "text": "Acuvue OASYS with Hydraclear Plus (24 линзы)",
                                                "raw": True,
                                            }
                                        },
                                        "url": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                    },
                                },
                            ]
                        },
                    }
                ]
            },
            preserve_order=True,
        )

        self.click_log.expect(
            dtype="market",
            type_id=5,
            geo_id=213,
            ip_geo_id=54,
            pof="ololo",
            url=LikeUrl.of("//beru.ru/search?text=acuvue%20oasys&clid=698", unquote=True),
            shop_id=1777,
            cp=100501,
            ae=0,
            cb=0,
            cpbbc=100501,
            cpa=0,
            categid=-1,
            hyper_cat_id=-1,
            nav_cat_id=INVALID_NAVIGATION_CATEGORY_ID,
            ware_md5="-1",
            pp=412,
            show_block_id="048841920011177788888",
            reqid=13579,
            wprid=987654321,
            test_buckets="12,34,56",
            position=1,
            yandexuid="123456789",
            url_type=26,
            icookie="2222222222",
        )
        self.click_log.expect(
            dtype="market",
            type_id=5,
            geo_id=213,
            ip_geo_id=54,
            pof="ololo",
            url=LikeUrl.of("//beru.ru/product/1986320?clid=698", unquote=True),
            shop_id=1777,
            msku=1986320,
            hyper_id=198632,
            vnd_id=19863002,
            cp=100500,
            ae=0,
            cb=0,
            cpbbc=100500,
            cpa=0,
            categid=1986306,
            hyper_cat_id=19863006,
            nav_cat_id=19863005,
            ware_md5="-1",
            pp=411,
            show_block_id="048841920011177788888",
            reqid=13579,
            wprid=987654321,
            test_buckets="12,34,56",
            position=1,
            yandexuid="123456789",
            url_type=26,
            icookie="2222222222",
        )
        self.click_log.expect(
            dtype="market",
            type_id=5,
            geo_id=213,
            ip_geo_id=54,
            pof="ololo",
            url=LikeUrl.of("//beru.ru/product/1986330?clid=698", unquote=True),
            shop_id=1777,
            msku=1986330,
            hyper_id=198633,
            vnd_id=19863003,
            cp=100500,
            ae=0,
            cb=0,
            cpbbc=100500,
            cpa=0,
            categid=1986308,
            hyper_cat_id=19863008,
            nav_cat_id=19863007,
            ware_md5="-1",
            pp=411,
            show_block_id="048841920011177788888",
            reqid=13579,
            wprid=987654321,
            test_buckets="12,34,56",
            position=2,
            yandexuid="123456789",
            url_type=26,
            icookie="2222222222",
        )

        self.show_log.expect(
            show_uid='04884192001117778888826001',
            event_time=488419200,
            click_price=100501,
            url=LikeUrl.of('//beru.ru/search?text=acuvue%20oasys&clid=698'),
            shop_id=1777,
            yandex_uid=123456789,
            ip="127.0.0.1",
            pp=412,
            pof="ololo",
            super_uid='04884192001117778888826001',
            user_agent_hash=NotEmpty(),
            reqid=13579,
            test_buckets="12,34,56",
            wprid=987654321,
            geo_id=213,
            position=1,
            show_block_id='048841920011177788888',
            url_type=26,
            icookie='2222222222',
        )
        self.show_log.expect(
            show_uid='04884192001117778888826001',
            event_time=488419200,
            click_price=100500,
            url=LikeUrl.of('//beru.ru/product/1986320?clid=698'),
            shop_id=1777,
            yandex_uid=123456789,
            ip="127.0.0.1",
            pp=411,
            pof="ololo",
            super_uid='04884192001117778888826001',
            user_agent_hash=NotEmpty(),
            reqid=13579,
            test_buckets="12,34,56",
            wprid=987654321,
            geo_id=213,
            position=1,
            show_block_id='048841920011177788888',
            vendor_id=19863002,
            msku=1986320,
            hyper_id=198632,
            category_id=1986306,
            hyper_cat_id=19863006,
            nid=19863005,
            url_type=26,
            icookie='2222222222',
        )
        self.show_log.expect(
            show_uid='04884192001117778888826002',
            event_time=488419200,
            click_price=100500,
            url=LikeUrl.of('//beru.ru/product/1986330?clid=698'),
            shop_id=1777,
            yandex_uid=123456789,
            ip="127.0.0.1",
            pp=411,
            pof="ololo",
            super_uid='04884192001117778888826002',
            user_agent_hash=NotEmpty(),
            reqid=13579,
            test_buckets="12,34,56",
            wprid=987654321,
            geo_id=213,
            position=2,
            show_block_id='048841920011177788888',
            vendor_id=19863003,
            msku=1986330,
            hyper_id=198633,
            category_id=1986308,
            hyper_cat_id=19863008,
            nid=19863007,
            url_type=26,
            icookie='2222222222',
        )

    def test_implicit_model_wizard_with_blue_incut(self):
        """Синяя врезка в колдунщике неявной модели

        https://st.yandex-team.ru/MARKETOUT-25163
        """

        request = 'place=parallel&text=acuvue+oasys&rearr-factors='

        # 1. Добавляем до 3 синих моделей во врезку
        # При добавлении синих моделей контент колдунщика не меняется
        # Синим моделям проставляется метка is_blue
        # Модели могут дублироваться, цены синих и белых моделей могут отличаться
        # Информация о рейтинге и отзывах прокидывается и для синих
        response = self.report.request_bs_pb(request + 'market_implicit_blue_incut_model_count=3')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "blue_market": Absent(),
                    "url": LikeUrl.of("//market.yandex.ru/search?text=acuvue%20oasys&clid=698"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=acuvue%20oasys&clid=721"),
                    "blueUrl": LikeUrl.of("//beru.ru/search?text=acuvue%20oasys&clid=698"),
                    "blueUrlTouch": LikeUrl.of("//m.beru.ru/search?text=acuvue%20oasys&clid=721"),
                    "favicon": {"faviconDomain": "market.yandex.ru"},
                    "title": "\7[Acuvue oasys\7]",
                    "text": [
                        {
                            "__hl": {
                                "text": "Цены, характеристики, отзывы на acuvue oasys. Выбор по параметрам. 51 магазин.",
                                "raw": True,
                            }
                        }
                    ],
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=698"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru?clid=721"),
                            "text": "Яндекс.Маркет",
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=acuvue%20oasys&clid=698"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=acuvue%20oasys&clid=721"),
                            "text": "Acuvue oasys",
                        },
                    ],
                    "sitelinks": [
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=acuvue%20oasys&lr=0&clid=698"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=acuvue%20oasys&lr=0&clid=721"
                            ),
                            "text": "Отзывы",
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/geo?text=acuvue%20oasys&lr=0&clid=698"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/geo?text=acuvue%20oasys&lr=0&clid=721"),
                            "text": "На карте",
                        },
                        {
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=acuvue%20oasys&lr=0&clid=698"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?delivery-interval=1&text=acuvue%20oasys&lr=0&clid=721"
                            ),
                            "text": "С доставкой завтра",
                        },
                    ],
                    "showcase": {
                        "items": [
                            {
                                "is_blue": NoKey("is_blue"),
                                "price": {"priceMin": "234"},
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (6 линз)", "raw": True}
                                    },
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-6-linz/198631?hid=19863004&nid=19863003&clid=698"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-6-linz/198631?hid=19863004&nid=19863003&clid=721"
                                    ),
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "price": {"priceMin": "123"},
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-12-linz/198632?hid=19863006&nid=19863005&clid=698"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-12-linz/198632?hid=19863006&nid=19863005&clid=721"
                                    ),
                                },
                                "rating": {"value": "3.5"},
                                "reviews": {
                                    "count": "19",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-12-linz/198632/reviews?clid=698"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-12-linz/198632/reviews?clid=721"
                                    ),
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "price": {"priceMin": "1986302"},
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-24-linzy/198633?hid=19863008&nid=19863007&clid=698"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--acuvue-oasys-with-hydraclear-plus-24-linzy/198633?hid=19863008&nid=19863007&clid=721"
                                    ),
                                },
                            },
                            {
                                "is_blue": "1",
                                "price": {"priceMin": "1986301"},
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                    "url": LikeUrl.of("//beru.ru/product/1986320?clid=698"),
                                    "urlTouch": LikeUrl.of("//m.beru.ru/product/1986320?clid=721"),
                                },
                                "rating": {"value": "3.5"},
                                "reviews": {
                                    "count": "19",
                                    "url": LikeUrl.of("//beru.ru/product/1986320/reviews?clid=698"),
                                    "urlTouch": LikeUrl.of("//m.beru.ru/product/1986320/reviews?clid=721"),
                                },
                            },
                            {
                                "is_blue": "1",
                                "price": {"priceMin": "1986302"},
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                    "url": LikeUrl.of("//beru.ru/product/1986330?clid=698"),
                                    "urlTouch": LikeUrl.of("//m.beru.ru/product/1986330?clid=721"),
                                },
                            },
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 2. При уменьшении market_implicit_blue_incut_model_count во врезку попадает только одна синяя модель из двух
        response = self.report.request_bs_pb(request + 'market_implicit_blue_incut_model_count=1')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (6 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": "1",
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 3. Проверяем применимость синей мета-формулы с порогом
        meta_flags = (
            ';market_implicit_blue_model_wizard_meta_mn_algo=MNA_mn201477'
            ';market_use_implicit_blue_model_wizard_meta_formula=1'
            ';market_implicit_blue_model_wizard_meta_threshold='
        )

        # 3.1 Проходим порог — есть модели
        response = self.report.request_bs_pb(request + 'market_implicit_blue_incut_model_count=2' + meta_flags + '0.5')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (6 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": "1",
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": "1",
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                },
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 3.2 Не проходим порог — нет моделей
        response = self.report.request_bs_pb(request + 'market_implicit_blue_incut_model_count=2' + meta_flags + '0.7')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (6 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                },
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 4. Проверяем, что market_implicit_blue_model_wizard_meta_rank_mn_algo меняет порядок синих моделей
        response = self.report.request_bs_pb(
            request + 'market_implicit_blue_incut_model_count=2'
            ';market_implicit_blue_model_wizard_meta_rank_mn_algo=MNA_mn201477'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (6 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": "1",
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": "1",
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 5. Проверяем, что для device=desktop market_implicit_blue_model_wizard_meta_rank_mn_algo по умолчанию задана и меняет порядок синих моделей
        response = self.report.request_bs_pb(
            request + 'market_implicit_blue_incut_model_count=2;'
            'device=desktop;'
            'market_implicit_blue_model_wizard_meta_threshold=0.1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (6 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": NoKey("is_blue"),
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": "1",
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (24 линзы)", "raw": True}
                                    },
                                },
                            },
                            {
                                "is_blue": "1",
                                "title": {
                                    "text": {
                                        "__hl": {"text": "Acuvue OASYS with Hydraclear Plus (12 линз)", "raw": True}
                                    },
                                },
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_implicit_wizard_wiz_elements1(self):
        """https://st.yandex-team.ru/MARKETOUT-23919"""

        request = 'place=parallel&text=acuvue+oasys'
        _ = self.report.request_bs_pb(request)

        self.access_log.expect(wizard_elements=Contains('market_implicit_model'))
        self.access_log.expect(wizard_elements=Not(Contains('blue_market_implicit_model')))

    def test_implicit_wizard_wiz_elements2(self):
        """https://st.yandex-team.ru/MARKETOUT-23919"""

        request = 'place=parallel&text=acuvue+oasys'
        _ = self.report.request_bs_pb(request)

        self.access_log.expect(wizard_elements=Contains('market_implicit_model'))
        self.access_log.expect(wizard_elements=Not(Contains('blue_market_implicit_model')))

    @classmethod
    def prepare_parallel_blue_base_search(cls):
        # Готовим белые офферы
        cls.index.offers += [
            Offer(title="ya yandeksofon white 1", ts=226641),
            Offer(title="ya yandeksofon white 2", ts=226642),
            Offer(title="ya yandeksofon white 3", ts=226643),
            Offer(title="ya yandeksofon white 4", ts=226644),
            Offer(title="ya yandeksofon white 5", ts=226645),
        ]

        # И несколько синих
        cls.index.mskus += [
            MarketSku(
                title="ya yandeksofon blue 1",
                sku=226640001,
                blue_offers=[BlueOffer(ts=226646, feedid=3, waremd5='gTL-3D5IXpiHAL-CvNRmNQ')],
            ),
            MarketSku(
                title="ya yandeksofon blue 2",
                sku=226640002,
                blue_offers=[BlueOffer(ts=226647, feedid=3, waremd5='jsFnEBncNV6VLkT9w4BajQ')],
            ),
            MarketSku(
                title="ya yandeksofon blue 3",
                sku=226640003,
                blue_offers=[BlueOffer(ts=226648, feedid=3, waremd5='pnO6jtfjEy9AfE4RIpBsnQ')],
            ),
        ]

        # Фиксируем релевантности
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226641).respond(0.99)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226642).respond(0.97)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226643).respond(0.95)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226644).respond(0.93)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226645).respond(0.91)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226646).respond(0.94)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226647).respond(0.98)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226648).respond(0.96)

    @classmethod
    def prepare_blue_offers_in_offers_wizard_incut_url_encryption(cls):
        # Готовим белые офферы

        cls.index.shops += [Shop(fesh=fesh, priority_region=213, regions=[225]) for fesh in range(3266401, 3266401 + 5)]

        cls.index.offers += [
            Offer(title="go pixel white 1", ts=326641, fesh=3266401),
            Offer(title="go pixel white 2", ts=326642, fesh=3266402),
            Offer(title="go pixel white 3", ts=326643, fesh=3266403),
            Offer(title="go pixel white 4", ts=326644, fesh=3266404),
            Offer(title="go pixel white 5", ts=326645, fesh=3266405),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=32664001, tovalid=32664004),
            HyperCategory(hid=32664002, tovalid=32664005),
            HyperCategory(hid=32664003, tovalid=32664006),
        ]
        cls.index.navtree += [
            NavCategory(nid=326640001, hid=32664001),
            NavCategory(nid=326640002, hid=32664002),
            NavCategory(nid=326640003, hid=32664003),
        ]

        cls.index.models += [
            Model(hyperid=326641, hid=32664001),
            Model(hyperid=326642, hid=32664002),
            Model(hyperid=326643, hid=32664003),
        ]

        # И несколько синих
        cls.index.mskus += [
            MarketSku(
                title="go pixel blue 1",
                sku=326640001,
                hyperid=326641,
                blue_offers=[
                    BlueOffer(
                        price=32664006,
                        ts=326646,
                        feedid=3,
                        offerid='blue_offer_326646',
                        waremd5='CPLbq42R5fOqd2LqGGbqPw',
                        vendor_id=3266401,
                    )
                ],
            ),
            MarketSku(
                title="go pixel blue 2",
                sku=326640002,
                hyperid=326642,
                blue_offers=[
                    BlueOffer(
                        price=32664007,
                        ts=326647,
                        feedid=3,
                        offerid='blue_offer_326647',
                        waremd5='xrNc0_bHLEvNqETSHowT_Q',
                        vendor_id=3266402,
                    )
                ],
            ),
            MarketSku(
                title="go pixel blue 3",
                sku=326640003,
                hyperid=326643,
                blue_offers=[
                    BlueOffer(
                        price=32664008,
                        ts=326648,
                        feedid=3,
                        offerid='blue_offer_326648',
                        waremd5='PlR-xcFGG1vbRbHt2rrrHg',
                        vendor_id=3266403,
                    )
                ],
            ),
        ]

        # Фиксируем релевантности
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 326641).respond(0.99)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 326642).respond(0.97)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 326643).respond(0.95)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 326644).respond(0.93)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 326645).respond(0.91)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 326646).respond(0.94)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 326647).respond(0.98)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 326648).respond(0.96)

    def test_blue_offer_from_white_shard(self):
        """Под флагом market_parallel_use_blue_base_search=0 ищем синие офферы в белом шарде

        https://st.yandex-team.ru/MARKETOUT-40785
        """

        response = self.report.request_json(
            'place=parallel&text=yandeksofon&debug=1' '&rearr-factors=market_parallel_use_blue_base_search=0'
        )

        self.assertFragmentIn(
            response, {"debug": {'metasearch': {'clients': {'SHOP': NotEmpty(), 'SHOP_BLUE': NoKey('SHOP_BLUE')}}}}
        )

        self.assertFragmentIn(
            response,
            {
                'documents': {
                    'main_dsrcid': [
                        {
                            'entity': 'offer',
                            'offerColor': 'blue',
                            'cpa': 'real',
                            'debug': {'tech': {"originBase": Not(Contains("basesearch-blue"))}},
                        }
                    ]
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                'documents': {
                    'main_dsrcid': [
                        {
                            'entity': 'offer',
                            'offerColor': 'white',
                            # 'cpa': 'real',
                            'debug': {'tech': {"originBase": Not(Contains("basesearch-blue"))}},
                        }
                    ]
                }
            },
        )

    def test_parallel_blue_base_search(self):
        """Под флагом market_parallel_use_blue_base_search=1 перестаем искать
        синие офферы в белом шарде и ходим за ними в синий шард как в базовый поиск.
        Флаг включен по умолчанию на всех платформах кроме десктопа.

        https://st.yandex-team.ru/MARKETOUT-22664
        https://st.yandex-team.ru/MARKETOUT-24424
        """

        # Сама функциональность рассчитана на то, что в лайте проверить
        # не представляется возможным: при хождении в отдельный синий шард мы
        # предположительно будем чаще успевать нагребать синие офферы и
        # соответственно у них чаще будет шанс пробиться во врезку

        # Проверим, что при добавлении флага выдача не изменится
        for rearr in ('', '&rearr-factors=market_parallel_use_blue_base_search=1'):
            response = self.report.request_bs_pb('place=parallel&text=yandeksofon' + rearr)
            # Во врезке будет только один синий оффер, т.к. не может быть нескольких офферов одного магазина
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "items": [
                                {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},  # 0.99
                                {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},  # 0.98
                                {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},  # 0.97
                                {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},  # 0.95
                                {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},  # 0.93
                                {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},  # 0.91
                            ]
                        }
                    }
                },
                allow_different_len=False,
            )

    def test_parallel_blue_base_search_formula(self):
        """Проверяем, что при хождении в Синий шард Синие документы
        ранжируются по формуле из market_parallel_blue_shard_search_mn_algo.
        https://st.yandex-team.ru/MARKETOUT-25806
        """

        def check_response(response, has_blue_formula):
            blue_docs_formula = "MNA_Relevance" if has_blue_formula else "MNA_mn201477"
            expected_result = [
                {"title": "ya yandeksofon white 1", "ranked_with": [{"formula": "MNA_mn201477", "value": 0.99}]},
                {"title": "ya yandeksofon blue 2", "ranked_with": [{"formula": blue_docs_formula, "value": 0.98}]},
                {"title": "ya yandeksofon white 2", "ranked_with": [{"formula": "MNA_mn201477", "value": 0.97}]},
                {"title": "ya yandeksofon white 3", "ranked_with": [{"formula": "MNA_mn201477", "value": 0.95}]},
                {"title": "ya yandeksofon white 4", "ranked_with": [{"formula": "MNA_mn201477", "value": 0.93}]},
                {"title": "ya yandeksofon white 5", "ranked_with": [{"formula": "MNA_mn201477", "value": 0.91}]},
            ]

            offer_factors = json.loads(response.get_searcher_props()['Market.Debug.offerFactors'])

            self.assertTrue(len(expected_result) <= len(offer_factors))
            for i in range(len(expected_result)):
                self.assertDictContainsSubset(expected_result[i], offer_factors[i])

        request = 'place=parallel&text=yandeksofon&debug=1&rearr-factors=market_search_mn_algo=MNA_mn201477;'

        # при хождении в Синий шард без отдельной формулы и при подмене формулы без Синего шарда
        # Синие документы ранжируются по Белой формуле
        for rearr in (
            'market_parallel_use_blue_base_search=0;market_parallel_blue_shard_search_mn_algo=MNA_Relevance;',
        ):
            response = self.report.request_bs_pb(request + rearr)
            check_response(response, has_blue_formula=False)

    def test_no_blue_offers_in_empty_incut(self):
        """Проверяем, что в случае пустой врезки синие офферы в неё добавляться не будут
        https://st.yandex-team.ru/MARKETOUT-25121
        """
        request = 'place=parallel&text=yandeksofon'

        # Зарезаем врезку порогом
        request += '&rearr-factors=market_offers_incut_meta_threshold=100500'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {"market_offers_wizard": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        # Проверяем, что под флагом синие офферы во врезку не добавляются
        response = self.report.request_bs_pb(request + ';market_offers_wizard_blue_offers_count=2')
        self.assertFragmentIn(response, {"market_offers_wizard": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

    def test_blue_offers_in_offers_wizard_incut_url_encryption(self):
        """Отдельный тип кликурлов для ссылок на Беру из синей врезки
        офферного колдунщика (тайтл и офферы)
        https://st.yandex-team.ru/MARKETOUT-25622
        """
        request = 'place=parallel&text=pixel'
        disable_nailed_docs = (
            'market_implicit_model_wizard_nailed_models_count=0;market_offers_wizard_nailed_offers_count=0;'
        )

        response = self.report.request_bs(
            request + '&rearr-factors=market_offers_wizard_blue_offers_count=2;'
            'market_blue_incut_encrypt_urls=1;'
            'market_blue_incut_title_click_price=100501;'
            'market_blue_incut_offer_click_price=100502;' + disable_nailed_docs + '&rids=213&ip-rids=54'
            '&yandexuid=123456789&wprid=987654321&pof=ololo&test-buckets=12,34,56'
            '&reqid=13579&x-yandex-icookie=2222222222'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "go pixel blue 2", "raw": True}},
                                        "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                    },
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "go pixel blue 3", "raw": True}},
                                        "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                                    },
                                },
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
        )

        # Проверяем, что ссылки blueUrl, blueUrlTouch ведут на Беру
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "url": LikeUrl.of("//market.yandex.ru/search?text=pixel&clid=545&lr=213"),
                        "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=pixel&clid=708&lr=213"),
                        "blueUrl": Contains("//market-click2.yandex.ru/redir/dtype=market/"),
                        "blueUrlTouch": LikeUrl.of("//m.beru.ru/search?text=pixel&clid=708&lr=213"),
                    }
                ]
            },
        )

        self.click_log.expect(
            dtype="market",
            type_id=5,
            geo_id=213,
            ip_geo_id=54,
            pof="ololo",
            url="%2F%2Fberu.ru%2Fsearch%3Ftext%3Dpixel%26clid%3D545",
            shop_id=1777,
            cp=100501,
            ae=0,
            cb=0,
            cpbbc=100501,
            cpa=0,
            categid=-1,
            hyper_cat_id=-1,
            nav_cat_id=INVALID_NAVIGATION_CATEGORY_ID,
            ware_md5="-1",
            pp=412,
            show_block_id="048841920011177788888",
            reqid=13579,
            wprid=987654321,
            test_buckets="12,34,56",
            position=1,
            yandexuid="123456789",
            url_type=26,
            icookie="2222222222",
        )
        self.click_log.expect(
            dtype="market",
            type_id=5,
            geo_id=213,
            ip_geo_id=54,
            pof="ololo",
            url="https%3A%2F%2Fpokupki.market.yandex.ru%2Fproduct%2F326640002%3Fofferid%3DxrNc0_bHLEvNqETSHowT_Q%26clid%3D545",
            shop_id=1777,
            # TODO(maryz) check msku
            hyper_id=326642,
            vnd_id=3266402,
            cp=100502,
            ae=0,
            cb=0,
            cpbbc=100502,
            cpa=0,
            categid=32664005,
            hyper_cat_id=32664002,
            nav_cat_id=326640002,
            pp=411,
            feed_id=100,
            onstock=1,
            price=32664007,
            ware_md5="xrNc0_bHLEvNqETSHowT_Q",
            offer_id="3.blue_offer_326647",
            show_block_id="048841920011177788888",
            reqid=13579,
            wprid=987654321,
            test_buckets="12,34,56",
            position=1,
            yandexuid="123456789",
            url_type=26,
            icookie="2222222222",
        )
        self.click_log.expect(
            dtype="market",
            type_id=5,
            geo_id=213,
            ip_geo_id=54,
            pof="ololo",
            url="https%3A%2F%2Fpokupki.market.yandex.ru%2Fproduct%2F326640003%3Fofferid%3DPlR-xcFGG1vbRbHt2rrrHg%26clid%3D545",
            shop_id=1777,
            # TODO(maryz) check msku
            hyper_id=326643,
            vnd_id=3266403,
            cp=100502,
            ae=0,
            cb=0,
            cpbbc=100502,
            cpa=0,
            categid=32664006,
            hyper_cat_id=32664003,
            nav_cat_id=326640003,
            pp=411,
            feed_id=100,
            onstock=1,
            price=32664008,
            ware_md5="PlR-xcFGG1vbRbHt2rrrHg",
            offer_id="3.blue_offer_326648",
            show_block_id="048841920011177788888",
            reqid=13579,
            wprid=987654321,
            test_buckets="12,34,56",
            position=2,
            yandexuid="123456789",
            url_type=26,
            icookie="2222222222",
        )

        self.show_log.expect(
            show_uid='04884192001117778888826001',
            event_time=488419200,
            click_price=100501,
            url='//beru.ru/search?text=pixel&clid=545',
            shop_id=1777,
            yandex_uid=123456789,
            ip="127.0.0.1",
            pp=412,
            pof="ololo",
            super_uid='04884192001117778888826001',
            user_agent_hash=NotEmpty(),
            reqid=13579,
            test_buckets="12,34,56",
            wprid=987654321,
            geo_id=213,
            position=1,
            show_block_id='048841920011177788888',
            url_type=26,
            icookie='2222222222',
        )
        self.show_log.expect(
            show_uid='04884192001117778888826001',
            event_time=488419200,
            click_price=100502,
            url='https://pokupki.market.yandex.ru/product/326640002?offerid=xrNc0_bHLEvNqETSHowT_Q&clid=545',
            shop_id=1777,
            yandex_uid=123456789,
            ip="127.0.0.1",
            pp=411,
            pof="ololo",
            super_uid='04884192001117778888826001',
            user_agent_hash=NotEmpty(),
            reqid=13579,
            test_buckets="12,34,56",
            wprid=987654321,
            geo_id=213,
            position=1,
            show_block_id='048841920011177788888',
            vendor_id=3266402,
            # TODO(maryz) check msku
            hyper_id=326642,
            category_id=32664005,
            hyper_cat_id=32664002,
            nid=326640002,
            url_type=26,
            icookie='2222222222',
        )
        self.show_log.expect(
            show_uid='04884192001117778888826002',
            event_time=488419200,
            click_price=100502,
            url='https://pokupki.market.yandex.ru/product/326640003?offerid=PlR-xcFGG1vbRbHt2rrrHg&clid=545',
            shop_id=1777,
            yandex_uid=123456789,
            ip="127.0.0.1",
            pp=411,
            pof="ololo",
            super_uid='04884192001117778888826002',
            user_agent_hash=NotEmpty(),
            reqid=13579,
            test_buckets="12,34,56",
            wprid=987654321,
            geo_id=213,
            position=2,
            show_block_id='048841920011177788888',
            vendor_id=3266403,
            # TODO(maryz) check msku
            hyper_id=326643,
            category_id=32664006,
            hyper_cat_id=32664003,
            nid=326640003,
            url_type=26,
            icookie='2222222222',
        )

    def test_blue_offers_in_offers_wizard_incut_sorted(self):
        """Проверяем синие офферы во врезке офферного колдунщика с честной сортировкой
        https://st.yandex-team.ru/MARKETOUT-24612
        https://st.yandex-team.ru/MARKETOUT-24979
        """
        request = 'place=parallel&text=yandeksofon'

        # Без флага market_offers_wizard_blue_offers_count добавляется один синий оффер
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},  # 0.99
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},  # 0.98
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},  # 0.97
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},  # 0.95
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},  # 0.93
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},  # 0.91
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_offers_wizard_blue_offers_count=1 добавляется 1 синий оффер
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wizard_blue_offers_count=1;'
            'market_offers_wizard_blue_offers_sorting=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},  # 0.99
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},  # 0.98
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},  # 0.97
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},  # 0.95
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},  # 0.93
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},  # 0.91
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_offers_wizard_blue_offers_count=2 добавляются 2 синих оффера
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wizard_blue_offers_count=2;'
            'market_offers_wizard_blue_offers_sorting=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},  # 0.99
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},  # 0.98
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},  # 0.97
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},  # 0.95
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},  # 0.93
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_offers_wizard_blue_offers_count=3 добавляются 3 синих оффера
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_offers_wizard_blue_offers_count=3;'
            'market_offers_wizard_blue_offers_sorting=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},  # 0.99
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},  # 0.98
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},  # 0.97
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},  # 0.95
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},  # 0.95
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},  # 0.95
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что ссылки blueUrl, blueUrlTouch ведут на Беру
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=yandeksofon&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=yandeksofon&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "blueUrl": LikeUrl.of(
                        "//beru.ru/search?text=yandeksofon&clid=545&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                    "blueUrlTouch": LikeUrl.of(
                        "//m.beru.ru/search?text=yandeksofon&clid=708&lr=0&utm_medium=cpc&utm_referrer=wizards",
                        ignore_len=False,
                        ignore_params=['rs'],
                    ),
                }
            },
        )

    def test_write_extra_blue_offers_to_feature_log1(self):
        """https://st.yandex-team.ru/MARKETOUT-25614
        Логируем синие офферы из дополнительной группировки в feature.log с меткой from_blue_stats=1
        """
        # Без запроса группировки в feature.log нет записей с from_blue_stats=1
        request = 'place=parallel&text=yandeksofon&debug=da&rearr-factors=market_parallel_feature_log_rate=1;market_cpa_offers_incut_count=0'
        _ = self.report.request_bs_pb(request)
        self.feature_log.expect(document_type=1, from_blue_stats=1).never()

    def test_write_extra_blue_offers_to_feature_log2(self):
        # При запросе группировки в feature.log появляются записи с from_blue_stats=1
        request = 'place=parallel&text=yandeksofon&debug=da&rearr-factors=market_parallel_feature_log_rate=1;market_offers_wizard_blue_offers_count=3;market_cpa_offers_incut_count=0&reqid=25614'
        _ = self.report.request_bs_pb(request)

        self.feature_log.expect(document_type=1, from_blue_stats=1).times(3)
        self.feature_log.expect(
            req_id=25614, ware_md5='jsFnEBncNV6VLkT9w4BajQ', document_type=1, from_blue_stats=1, position=1
        ).once()  # ya yandeksofon blue 2
        self.feature_log.expect(
            req_id=25614, ware_md5='pnO6jtfjEy9AfE4RIpBsnQ', document_type=1, from_blue_stats=1, position=2
        ).once()  # ya yandeksofon blue 3
        self.feature_log.expect(
            req_id=25614, ware_md5='gTL-3D5IXpiHAL-CvNRmNQ', document_type=1, from_blue_stats=1, position=3
        ).once()  # ya yandeksofon blue 1

    def test_blue_offers_urls_in_offers_wizard_incut(self):
        """Проверяем, что в эксперименте market_offers_wizard_blue_offers_count
        урлы синих офферов во врезке ведут на беру через клик-урл даже в эксперименте с КО
        https://st.yandex-team.ru/MARKETOUT-25270
        """
        for url_type_flag in ['', ';market_offers_wizard_incut_url_type=OfferCard']:
            response = self.report.request_bs_pb(
                'place=parallel&text=yandeksofon&rearr-factors=market_offers_wizard_blue_offers_count=3' + url_type_flag
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "ya yandeksofon blue 1", "raw": True}},
                                        "urlForCounter": Contains("/redir/dtype=market/"),
                                    },
                                    "thumb": {
                                        "urlForCounter": Contains("/redir/dtype=market/"),
                                    },
                                }
                            ]
                        }
                    }
                },
            )

    def test_wizards_clids_in_blue_clickurls(self):
        """При прямых перехода на беру с колдунщиков проставляем clid в урл
        https://st.yandex-team.ru/MARKETOUT-25389
        https://st.yandex-team.ru/MARKETOUT-25439
        """

        # 1. Офферная врезка, десктоп
        response = self.report.request_bs_pb(
            'place=parallel&text=yandeksofon&rearr-factors=market_offers_wizard_incut_url_type=External;market_adg_offer_url_type=External'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}},
                                    "urlForCounter": Not(Contains("clid%3D545")),
                                    "offercardUrl": Not(Contains("clid%3D913")),
                                },
                                "thumb": {
                                    "urlForCounter": Not(Contains("clid%3D545")),
                                    "offercardUrl": Not(Contains("clid%3D913")),
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}},
                                    "urlForCounter": Contains("clid%3D545"),
                                    "offercardUrl": Contains("clid%3D913"),
                                },
                                "thumb": {
                                    "urlForCounter": Contains("clid%3D545"),
                                    "offercardUrl": Contains("clid%3D913"),
                                },
                            },
                        ]
                    }
                }
            },
        )

        # 2. Офферная врезка, тач
        response = self.report.request_bs_pb(
            'place=parallel&text=yandeksofon&touch=1&rearr-factors=market_offers_wizard_incut_url_type=External;market_adg_offer_url_type=External;offers_touch=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}},
                                    "urlForCounter": Not(Contains("clid%3D708")),
                                    "offercardUrl": Not(Contains("clid%3D919")),
                                },
                                "thumb": {
                                    "urlForCounter": Not(Contains("clid%3D708")),
                                    "offercardUrl": Not(Contains("clid%3D919")),
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}},
                                    "urlForCounter": Contains("clid%3D708"),
                                    "offercardUrl": Contains("clid%3D919"),
                                },
                                "thumb": {
                                    "urlForCounter": Contains("clid%3D708"),
                                    "offercardUrl": Contains("clid%3D919"),
                                },
                            },
                        ]
                    }
                }
            },
        )

        # 3. Модельная врезка, десктоп
        response = self.report.request_bs_pb('place=parallel&text=kumamon+suga&rearr-factors=market_nordstream=0')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "kumamon suga", "raw": True}},
                                    "url": Contains("clid=502"),
                                    "directOffercardUrl": Contains("clid=914"),
                                }
                            }
                        ]
                    }
                }
            },
        )

        # 4. Модельная врезка, тач
        response = self.report.request_bs_pb(
            'place=parallel&text=kumamon+suga&rearr-factors=market_nordstream=0&touch=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "kumamon suga", "raw": True}},
                                    "url": Contains("clid=502"),
                                    "directOffercardUrl": Contains("clid=920"),
                                }
                            }
                        ]
                    }
                }
            },
        )

    def test_blue_offers_in_offers_wizard_incut(self):
        """Проверяем синие офферы во врезке офферного колдунщика.
        Синие офферы добавляются в конец врезки без выравнивания.
        https://st.yandex-team.ru/MARKETOUT-24612
        https://st.yandex-team.ru/MARKETOUT-24979
        https://st.yandex-team.ru/MARKETOUT-25006
        """
        request = 'place=parallel&text=yandeksofon'

        # Без флага market_offers_wizard_blue_offers_count добавляется один синий оффер
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_offers_wizard_blue_offers_count=1 добавляется 1 синий оффер
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wizard_blue_offers_count=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_offers_wizard_blue_offers_count=2 добавляются 2 синих оффера
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wizard_blue_offers_count=2')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 3", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_offers_wizard_blue_offers_count=3 добавляются 3 синих оффера
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wizard_blue_offers_count=3')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon white 5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 1", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_threshold_for_blue_offers_in_offers_wizard_incut(self):
        """Проверяем порог для показа синих офферов во врезке офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-24612
        """
        request = (
            'place=parallel&text=yandeksofon&trace_wizard=1&rearr-factors=market_offers_wizard_blue_offers_count=3'
        )

        # Cумма значений метаформулы синих офферов больше порога, синие офферы добавляются
        response = self.report.request_bs_pb(request + '&rearr-factors=market_blue_offers_incut_threshold=0.8')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "ya yandeksofon blue 1", "raw": True}}}},
                        ]
                    }
                }
            },
        )
        self.assertIn('10 1 Top 3 meta MatrixNet sum for blue offers incut: 0.9', response.get_trace_wizard())
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_blue_offers_incut_threshold=0.8',
            response.get_trace_wizard(),
        )
        market_factors = json.loads(response.get_searcher_props()['Market.Factors'])
        self.assertEqual(round(market_factors['OffersIncutBlueRelevance'], 2), 0.9)

        # Cумма значений метаформулы синих офферов меньше порога, синие офферы (1 и 3) не добавляются
        # Оффер из белой врезки (2) тоже отфильтровывается, см. https://st.yandex-team.ru/MARKETOUT-25807
        response = self.report.request_bs_pb(request + '&rearr-factors=market_blue_offers_incut_threshold=1')
        for title in ['ya yandeksofon blue 1', 'ya yandeksofon blue 2', 'ya yandeksofon blue 3']:
            self.assertFragmentNotIn(
                response,
                {
                    "market_offers_wizard": {
                        "showcase": {
                            "items": [{"title": {"text": {"__hl": {"text": "{}".format(title), "raw": True}}}}]
                        }
                    }
                },
            )
        self.assertIn('10 1 Top 3 meta MatrixNet sum for blue offers incut: 0.9', response.get_trace_wizard())
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_blue_offers_incut_threshold=1',
            response.get_trace_wizard(),
        )
        self.assertIn(
            '10 4 Did not pass: top 3 meta MatrixNet sum for blue offers incut is too low', response.get_trace_wizard()
        )

    def test_write_blue_blender_factors(self):
        """Проверяем, что под флагом market_parallel_fill_blue_blender_factors=1
        в market_factors пишутся синие факторы для Блендера
        https://st.yandex-team.ru/MARKETOUT-28081
        """
        request = (
            'place=parallel&text=yandeksofon&rearr-factors='
            'market_offers_wizard_blue_offers_count=3;'
            'market_blue_offers_incut_threshold=0.1;'
            'market_enable_offers_wiz_right_incut=1;'
            'market_enable_offers_wiz_center_incut=1;'
        )

        response = self.report.request_bs(request + 'market_parallel_fill_blue_blender_factors=1')
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "blue.offers_wizard_right_has_blue": 1,
                        "blue.offers_wizard_center_has_blue": 1,
                        "blue.blue_offers_count_from_base": 3,
                        "blue.offers_incut_blue_meta_relevance": Round(0.9),
                    }
                ]
            },
        )

        request = (
            'place=parallel&text=acuvue+oasys&trace_wizard=1&rearr-factors='
            'market_implicit_blue_incut_model_count=2;'
            'market_use_implicit_blue_model_wizard_meta_formula=1;'
            'market_enable_implicit_model_wiz_center_incut=1;'
        )
        response = self.report.request_bs(request + 'market_parallel_fill_blue_blender_factors=1')
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "blue.implicit_model_wizard_right_has_blue": 1,
                        "blue.implicit_model_wizard_center_has_blue": 1,
                        "blue.blue_models_count_from_base": 2,
                        "blue.implicit_model_blue_meta_relevance": Round(0.6),
                    }
                ]
            },
        )

    def test_wizards_meta_relevance_and_threshold_in_market_factors(self):
        """Проверка добавления значений мета-формул и порогов колдунщиков в market_factors для проброса в Dumper
        https://st.yandex-team.ru/MARKETOUT-29209
        https://st.yandex-team.ru/MARKETOUT-30916
        """
        # Проверка значений мета-формулы и порога по синим офферам врезки
        response = self.report.request_bs(
            'place=parallel&text=yandeksofon&rearr-factors='
            'market_offers_wizard_blue_offers_count=3;'
            'market_blue_offers_incut_threshold=0.8'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "OffersIncutBlueRelevance": Round(0.9),
                        "OffersIncutBlueThreshold": Round(0.8),
                    }
                ]
            },
        )

        # Проверка значений мета-формулы и порога по синим моделям в колдунщике неявной модели
        response = self.report.request_bs(
            'place=parallel&text=acuvue+oasys&rearr-factors='
            'market_implicit_blue_incut_model_count=2;'
            'market_use_implicit_blue_model_wizard_meta_formula=1;'
            'market_implicit_blue_model_wizard_meta_threshold=0.7;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_factors": [
                    {
                        "ImplicitModelWizardBlueRelevance": Round(0.6),
                        "ImplicitModelWizardBlueThreshold": Round(0.7),
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()

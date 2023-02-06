#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip

from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    DynamicBlueGenericBundlesPromos,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    Outlet,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    Region,
    RegionalDelivery,
    Shop,
    UrlType,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main
from core.types.autogen import b64url_md5
from core.types.offer_promo import (
    PromoBlueCashback,
    PromoRestrictions,
    OffersMatchingRules,
    make_generic_bundle_content,
)
from core.report import REQUEST_TIMESTAMP
from core.types.delivery import BlueDeliveryTariff
from core.types.region import GpsCoord
from core.bigb import make_profile, RegularCoords

from core.matcher import LikeUrl, Absent, ElementCount, Contains, Not, Round, Regex, NotEmpty, NotEmptyList

from datetime import datetime, timedelta
from itertools import count

import base64
import re


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta = timedelta(days=10)
nummer = count()


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0, price_to=100),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=399, large_size=1),
            ],
            large_size_weight=20,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0, price_to=100),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=399, large_size=1),
            ],
            regions=[54],
            large_size_weight=20,
        )

        cls.index.shops += [
            Shop(
                fesh=431782,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                priority_region=213,
                name='Яндекс.Маркет',
            ),
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225]),
            Shop(fesh=4, priority_region=213, regions=[225]),
            Shop(
                fesh=10,
                datafeed_id=10,
                priority_region=213,
                regions=[225],
                supplier_type=Shop.THIRD_PARTY,
                blue='REAL',
                name='3p shop',
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=1,
                fesh=431782,
                region=213,
                gps_coord=GpsCoord(37.5, 55.5),
                locality_name='Moscow',
                thoroughfare_name='Lenin av.',
                premise_number='100',
            ),
            Outlet(
                point_id=2,
                fesh=431782,
                region=213,
                gps_coord=GpsCoord(37.55, 55.5),
                locality_name='Moscow',
                thoroughfare_name='Lev Tolstoy st.',
                premise_number='23',
            ),
            Outlet(
                point_id=3,
                fesh=431782,
                region=213,
                gps_coord=GpsCoord(37.64, 55.64),
                locality_name='Moscow',
                thoroughfare_name='Random st.',
                premise_number='74',
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=201,
                fesh=431782,
                carriers=[1, 3],
                options=[PickupOption(outlet_id=1), PickupOption(outlet_id=2), PickupOption(outlet_id=3)],
            )
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=101,
                fesh=431782,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=500, day_from=1, day_to=3)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=102,
                fesh=431782,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=500, day_from=2, day_to=2)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=103,
                fesh=431782,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=500, day_from=0, day_to=5)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=104,
                fesh=431782,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=500, day_from=0, day_to=5)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        blue_cashback_1 = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            key=b64url_md5(next(nummer)),
            blue_cashback=PromoBlueCashback(share=0.2, version=1, priority=1),
            offers_matching_rules=[OffersMatchingRules(mskus=[1001])],
        )

        blue_cashback_2 = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            key=b64url_md5(next(nummer)),
            blue_cashback=PromoBlueCashback(
                share=0.5,
                version=1,
                priority=1,
            ),
            restrictions=PromoRestrictions(predicates=[{'perks': ['yandex_extra_cashback']}]),
            offers_matching_rules=[OffersMatchingRules(mskus=[1001])],
        )

        promo_code = Promo(
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_1_text',
            description='promocode_1_description',
            discount_value=10,
            key=b64url_md5(next(nummer)),
            shop_promo_id='promocode_1',
            offers_matching_rules=[OffersMatchingRules(mskus=[1001])],
        )

        generic_bundle = Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=777,
            key=b64url_md5(next(nummer)),
            url='http://localhost.ru/',
            generic_bundles_content=[
                make_generic_bundle_content('iphone-blue-1', 'air-pods-1'),
            ],
            offers_matching_rules=[OffersMatchingRules(feed_offer_ids=[[431782, 'iphone-blue-1']])],
        )

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[generic_bundle.key])]

        cls.index.mskus += [
            MarketSku(
                sku=1001,
                title="iphone blue 1",
                hyperid=101,
                blue_offers=[
                    BlueOffer(
                        ts=100101,
                        waremd5='gTL-3D5IXpiHAL-CvNRmNQ',
                        feedid=431782,
                        pickup_buckets=[201],
                        delivery_buckets=[101, 104],
                        offerid='iphone-blue-1',
                        price=100,
                        promo=[blue_cashback_1, blue_cashback_2, promo_code, generic_bundle],
                        hid=10,
                    )
                ],
            ),
            MarketSku(
                sku=1002,
                title="iphone blue 2",
                blue_offers=[
                    BlueOffer(ts=100201, waremd5='jsFnEBncNV6VLkT9w4BajQ', feedid=431782, delivery_buckets=[102])
                ],
            ),
            MarketSku(
                sku=1003,
                title="iphone blue 3",
                blue_offers=[BlueOffer(ts=100301, waremd5='pnO6jtfjEy9AfE4RIpBsnQ', feedid=10, delivery_buckets=[103])],
            ),
            MarketSku(sku=1100, title="air pods 1", blue_offers=[BlueOffer(feedid=431782, offerid='air-pods-1')]),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100201).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100301).respond(0.7)

        cls.index.offers += [
            Offer(title="iphone white 1", fesh=2, ts=101),
            Offer(title="iphone white 2", fesh=2, ts=102),
            Offer(title="iphone white 3", fesh=2, ts=103),
            Offer(title="iphone white 4", fesh=2, ts=104),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104).respond(0.3)

        # Implicit model wizard
        cls.index.hypertree += [HyperCategory(hid=10, output_type=HyperCategoryType.GURU)]
        cls.index.navtree += [NavCategory(nid=10, hid=10)]

        cls.index.models += [
            Model(hyperid=101, title="iphone blue model 1", opinion=Opinion(rating=4, precise_rating=4.5), hid=10),
            Model(hyperid=2001, title="iphone model 1", hid=10),
            Model(hyperid=2002, title="iphone model 2"),
            Model(hyperid=2003, title="iphone model 3"),
            Model(hyperid=2004, title="iphone model 4"),
            Model(hyperid=2005, title="iphone dsbs model 1", hid=10),
        ]

        cls.index.offers += [
            Offer(hyperid=2001, fesh=1),
            Offer(hyperid=2002, fesh=2),
            Offer(hyperid=2003, fesh=3),
            Offer(hyperid=2004, fesh=4),
        ]

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition()
            .add(hyperid=101, reasons=[{"id": "alisa_lives_here", "type": "specsFactor", "value": 1}])
        ]

        # DSBS offers
        cls.index.shops += [
            Shop(fesh=5, cpa=Shop.CPA_REAL),
            Shop(fesh=6, cpa=Shop.CPA_REAL),
            Shop(fesh=7, cpa=Shop.CPA_REAL),
            Shop(fesh=8, cpa=Shop.CPA_REAL),
            Shop(fesh=9, cpa=Shop.CPA_REAL),
        ]
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]
        cls.index.offers += [
            Offer(
                title="iphone dsbs 1",
                fesh=5,
                ts=201,
                cpa=Offer.CPA_REAL,
                hyperid=2001,
                hid=10,
                waremd5="A1c7MrJ0bm6MqYuRI_Ikmw",
            ),
            Offer(title="iphone dsbs 2", fesh=6, ts=202, cpa=Offer.CPA_NO),
            Offer(title="iphone dsbs 3", fesh=7, ts=203, cpa=Offer.CPA_REAL),
            Offer(
                title="iphone dsbs 4 without cpc links",
                fesh=8,
                ts=204,
                cpa=Offer.CPA_REAL,
                is_cpc=False,
                has_url=False,
                hyperid=2005,
                waremd5='3C819zhGvv1gpOdrTglNIA',
            ),
            Offer(
                title="iphone dsbs 5 without cpc links", fesh=9, ts=205, cpa=Offer.CPA_REAL, is_cpc=False, has_url=False
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 202).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 203).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 204).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 205).respond(0.1)

    def test_offers_wizard_cpa_incut(self):
        """Проверяем cpa офферы во врезке офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-32972
        https://st.yandex-team.ru/MARKETOUT-34291
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_threshold=0;market_cpa_offers_incut_hide_duplicates=0;'

        for add_rearr in ['', ';market_cpa_offers_incut_virtual_attr=1']:
            # Под флагами market_cpa_offers_incut_count=1 и market_offers_wizard_cpa_offers_incut=1 добавляется 1 cpa оффер
            response = self.report.request_bs_pb(
                request + 'market_cpa_offers_incut_count=1;market_offers_wizard_cpa_offers_incut=1{}'.format(add_rearr)
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "cpaUrl": LikeUrl.of("//pokupki.market.yandex.ru/search?lr=0&text=iphone&clid=545"),
                        "cpaOfferCount": 3,
                        "showcase": {
                            "cpaItems": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                        "url": LikeUrl.of(
                                            "https://pokupki.market.yandex.ru/product/1001?offerid=gTL-3D5IXpiHAL-CvNRmNQ"
                                        ),
                                        "urlForCounter": Contains(
                                            '//pokupki.market.yandex.ru/product/1001?lr=0&offerid=gTL-3D5IXpiHAL-CvNRmNQ&clid=1601'
                                        ),
                                    },
                                    "modelId": "101",
                                },
                            ]
                        },
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # Под флагами market_cpa_offers_incut_count=3 и market_offers_wizard_cpa_offers_incut=1 добавляется 3 cpa оффер
            response = self.report.request_bs_pb(
                request + 'market_cpa_offers_incut_count=3;market_offers_wizard_cpa_offers_incut=1{}'.format(add_rearr)
            )
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "cpaUrl": LikeUrl.of(
                            "//pokupki.market.yandex.ru/search?lr=0&rs=eJwzSvKS4xJLD_HRNXYx9YwoyPRw9NF1LvMLyvULlGBUYNBgAMlnFbvluTrlJfuFmYX5ZIdYlps4JWYhyRfk-ZtllaRluVZaOqa5mgR5FjgV50HlIxgAFQIZdg%2C%2C&text=iphone&clid=545"  # noqa
                        ),
                        "cpaOfferCount": 3,
                        "showcase": {
                            "cpaItems": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                        "url": LikeUrl.of(
                                            "https://pokupki.market.yandex.ru/product/1001?offerid=gTL-3D5IXpiHAL-CvNRmNQ"
                                        ),
                                        "urlForCounter": Contains(
                                            '//pokupki.market.yandex.ru/product/1001?lr=0&offerid=gTL-3D5IXpiHAL-CvNRmNQ&clid=1601'
                                        ),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone blue 2", "raw": True}},
                                        "url": LikeUrl.of(
                                            "https://pokupki.market.yandex.ru/product/1002?offerid=jsFnEBncNV6VLkT9w4BajQ"
                                        ),
                                        "urlForCounter": Contains(
                                            '//pokupki.market.yandex.ru/product/1002?lr=0&offerid=jsFnEBncNV6VLkT9w4BajQ&clid=1601'
                                        ),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone blue 3", "raw": True}},
                                        "url": LikeUrl.of(
                                            "https://pokupki.market.yandex.ru/product/1003?offerid=pnO6jtfjEy9AfE4RIpBsnQ"
                                        ),
                                        "urlForCounter": Contains(
                                            '//pokupki.market.yandex.ru/product/1003?lr=0&offerid=pnO6jtfjEy9AfE4RIpBsnQ&clid=1601'
                                        ),
                                    }
                                },
                            ]
                        },
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # Без флагов market_cpa_offers_incut_count и market_offers_wizard_cpa_offers_incut cpa офферы добавляются
            response = self.report.request_bs_pb(request)
            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "cpaUrl": LikeUrl.of(
                            "//pokupki.market.yandex.ru/search?lr=0&rs=eJwzSvKS4xJLD_HRNXYx9YwoyPRw9NF1LvMLyvULlGBUYNBgAMlnFbvluTrlJfuFmYX5ZIdYlps4JWYhyRfk-ZtllaRluVZaOqa5mgR5FjgV50HlIxgAFQIZdg%2C%2C&text=iphone&clid=545"  # noqa
                        ),
                        "cpaOfferCount": 3,
                        "showcase": {
                            "cpaItems": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                        "url": LikeUrl.of(
                                            "https://pokupki.market.yandex.ru/product/1001?offerid=gTL-3D5IXpiHAL-CvNRmNQ"
                                        ),
                                        "urlForCounter": Contains(
                                            '//pokupki.market.yandex.ru/product/1001?lr=0&offerid=gTL-3D5IXpiHAL-CvNRmNQ&clid=1601'
                                        ),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone blue 2", "raw": True}},
                                        "url": LikeUrl.of(
                                            "https://pokupki.market.yandex.ru/product/1002?offerid=jsFnEBncNV6VLkT9w4BajQ"
                                        ),
                                        "urlForCounter": Contains(
                                            '//pokupki.market.yandex.ru/product/1002?lr=0&offerid=jsFnEBncNV6VLkT9w4BajQ&clid=1601'
                                        ),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "iphone blue 3", "raw": True}},
                                        "url": LikeUrl.of(
                                            "https://pokupki.market.yandex.ru/product/1003?offerid=pnO6jtfjEy9AfE4RIpBsnQ"
                                        ),
                                        "urlForCounter": Contains(
                                            '//pokupki.market.yandex.ru/product/1003?lr=0&offerid=pnO6jtfjEy9AfE4RIpBsnQ&clid=1601'
                                        ),
                                    }
                                },
                            ]
                        },
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_threshold_for_cpa_offers_incut_in_offers_wizard(self):
        """Проверяем порог для показа cpa офферов во врезке офферного колдунщика,
        добавление значений метафильтра и порога в факторы
        https://st.yandex-team.ru/MARKETOUT-32972
        https://st.yandex-team.ru/MARKETOUT-34596
        """
        for add_rearr in ['', ';market_cpa_offers_incut_virtual_attr=1']:
            request = 'place=parallel&text=iphone&trace_wizard=1&rearr-factors=market_cpa_offers_incut_threshold=0;market_cpa_offers_incut_count=3;market_offers_wizard_cpa_offers_incut=1{};'.format(
                add_rearr
            )

            # Cумма значений метаформулы cpa офферов больше порога, cpa офферы добавляются
            response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_threshold=0.8')
            self.assertFragmentIn(
                response,
                {"market_offers_wizard": {"showcase": {"cpaItems": ElementCount(3)}}},
                preserve_order=True,
                allow_different_len=False,
            )
            trace_wizard = response.get_trace_wizard()
            self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', trace_wizard)
            self.assertIn(
                '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_cpa_offers_incut_threshold=0.8',
                trace_wizard,
            )

            self.assertFragmentIn(
                response,
                {"Market.Factors": {"MarketplaceIncutRelevance": Round(0.9), "MarketplaceIncutThreshold": Round(0.8)}},
            )

            # Cумма значений метаформулы синих офферов меньше порога, синие офферы не добавляются
            response = self.report.request_bs_pb(request + '&rearr-factors=market_cpa_offers_incut_threshold=1')
            self.assertFragmentIn(
                response,
                {"market_offers_wizard": {"showcase": {"cpaItems": Absent()}}},
                preserve_order=True,
                allow_different_len=False,
            )
            trace_wizard = response.get_trace_wizard()
            self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', trace_wizard)
            self.assertIn(
                '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_cpa_offers_incut_threshold=1', trace_wizard
            )
            self.assertIn('10 4 Did not pass: top 5 meta MatrixNet sum for cpa offers incut is too low', trace_wizard)

            self.assertFragmentIn(
                response,
                {"Market.Factors": {"MarketplaceIncutRelevance": Round(0.9), "MarketplaceIncutThreshold": Round(1)}},
            )

    def test_cpa_offers_incut_metafilter_top_count(self):
        """Проверяем, что флаг market_cpa_offers_incut_metafilter_top_count
        задает количество офферов для расчета значения метафильтра CPA офферной врезки
        https://st.yandex-team.ru/MARKETOUT-35024
        """
        for add_rearr in ['', ';market_cpa_offers_incut_virtual_attr=1']:
            request = 'place=parallel&text=iphone&trace_wizard=1&rearr-factors=market_cpa_offers_incut_threshold=0;market_cpa_offers_incut_count=3;market_offers_wizard_cpa_offers_incut=1{};'.format(
                add_rearr
            )

            # Под флагом market_cpa_offers_incut_metafilter_top_count=2 для расчета используются Топ 2 оффера
            response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_metafilter_top_count=2')
            self.assertIn('10 1 Top 2 meta MatrixNet sum for cpa offers incut: 0.6', response.get_trace_wizard())
            self.assertFragmentIn(
                response, {"Market.Factors": {"MarketplaceIncutRelevance": Round(0.6), "MarketplaceIncutTopCount": 2}}
            )

            # Под флагом market_cpa_offers_incut_metafilter_top_count=5 для расчета используются все 3 найденных оффера
            response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_metafilter_top_count=5')
            self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', response.get_trace_wizard())
            self.assertFragmentIn(
                response, {"Market.Factors": {"MarketplaceIncutRelevance": Round(0.9), "MarketplaceIncutTopCount": 3}}
            )

            # Без флага market_cpa_offers_incut_metafilter_top_count для расчета используются все 3 найденных оффера
            response = self.report.request_bs_pb(request)
            self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', response.get_trace_wizard())
            self.assertFragmentIn(
                response, {"Market.Factors": {"MarketplaceIncutRelevance": Round(0.9), "MarketplaceIncutTopCount": 3}}
            )

    def test_offers_wizard_cpa_incut_shopid(self):
        """Проверяем, что под флагом market_cpa_offers_incut_shopid в офферном колдунщике
        в ссылки с CPA врезки на КО добавляется параметр shopId
        https://st.yandex-team.ru/MARKETOUT-33021
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=1;market_offers_wizard_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0;market_cpa_offers_incut_url_type=OfferCard;'  # noqa

        # Под флагом market_cpa_offers_incut_shopid в ссылки добавляется параметр &shopId
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_shopid=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/offer/gTL-3D5IXpiHAL-CvNRmNQ?text=iphone&clid=1601&shopId=431782"
                                    ),
                                    "urlForCounter": Contains(
                                        '//market-click2.yandex.ru/redir/dtype=offercard', 'shopId%3D431782'
                                    ),
                                }
                            }
                        ]
                    }
                }
            },
            preserve_order=True,
        )

        # Без флага market_cpa_offers_incut_shopid параметра &shopId в ссылках нет
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/offer/gTL-3D5IXpiHAL-CvNRmNQ?text=iphone&clid=1601",
                                        no_params=['shopId'],
                                    ),
                                    "urlForCounter": Not(Contains('shopId%3D431782')),
                                }
                            }
                        ]
                    }
                }
            },
            preserve_order=True,
        )

    def test_implicit_model_wizard_cpa_incut(self):
        """Проверяем, что под флагом market_implicit_model_cpa_offers_incut в колдунщик неявной модели
        добавляется врезка с CPA офферами
        https://st.yandex-team.ru/MARKETOUT-33072
        https://st.yandex-team.ru/MARKETOUT-34291
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=3;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "cpaUrl": LikeUrl.of(
                        "//pokupki.market.yandex.ru/search?lr=0&rs=eJwzSvKS4xJLD_HRNXYx9YwoyPRw9NF1LvMLyvULlGBUYNBgAMlnFbvluTrlJfuFmYX5ZIdYlps4JWYhyRfk-ZtllaRluVZaOqa5mgR5FjgV50HlIxgAFQIZdg%2C%2C&text=iphone&clid=698"  # noqa
                    ),
                    "cpaOfferCount": 3,
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "url": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?offerid=gTL-3D5IXpiHAL-CvNRmNQ"
                                    ),
                                    "urlForCounter": Contains(
                                        '//pokupki.market.yandex.ru/product/1001?lr=0&offerid=gTL-3D5IXpiHAL-CvNRmNQ&clid=1601'
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 2", "raw": True}},
                                    "url": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1002?offerid=jsFnEBncNV6VLkT9w4BajQ"
                                    ),
                                    "urlForCounter": Contains(
                                        '//pokupki.market.yandex.ru/product/1002?lr=0&offerid=jsFnEBncNV6VLkT9w4BajQ&clid=1601'
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 3", "raw": True}},
                                    "url": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1003?offerid=pnO6jtfjEy9AfE4RIpBsnQ"
                                    ),
                                    "urlForCounter": Contains(
                                        '//pokupki.market.yandex.ru/product/1003?lr=0&offerid=pnO6jtfjEy9AfE4RIpBsnQ&clid=1601'
                                    ),
                                }
                            },
                        ]
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_cpa_incut_url_type(self):
        """Проверяем что флаг market_cpa_offers_incut_url_type задает тип ссылок CPA офферов
        https://st.yandex-team.ru/MARKETOUT-33305
        https://st.yandex-team.ru/MARKETOUT-34291
        https://st.yandex-team.ru/MARKETOUT-36946
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=1;market_offers_wizard_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0;'

        # Под флагом market_cpa_offers_incut_url_type=PokupkiIncut ссылки CPA офферов ведут на pokupki.market.yandex.ru
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=PokupkiIncut')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaUrl": LikeUrl.of("//pokupki.market.yandex.ru/search?text=iphone&lr=0&clid=545"),
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        '//pokupki.market.yandex.ru/product/1001?clid=1601&lr=0&offerid=gTL-3D5IXpiHAL-CvNRmNQ'
                                    ),
                                }
                            }
                        ]
                    },
                }
            },
        )

        # Под флагом market_cpa_offers_incut_url_type=OfferCard ссылки CPA офферов ведут на КО
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=OfferCard')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&cpa=1"),
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            }
                        ]
                    },
                }
            },
        )

        # Без флага market_cpa_offers_incut_url_type ссылки CPA офферов ведут на Покупки
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaUrl": LikeUrl.of("//pokupki.market.yandex.ru/search?text=iphone&lr=0&clid=545"),
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        '//pokupki.market.yandex.ru/product/1001?clid=1601&lr=0&offerid=gTL-3D5IXpiHAL-CvNRmNQ'
                                    ),
                                }
                            }
                        ]
                    },
                }
            },
        )

        # Под флагом market_cpa_offers_incut_url_type=ModelCard ссылки CPA офферов ведут на КМ
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=ModelCard')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&cpa=1"),
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        '//market.yandex.ru/product/101?hid=10&nid=10&lr=0&do-waremd5=gTL-3D5IXpiHAL-CvNRmNQ&clid=1601'
                                    ),
                                }
                            }
                        ]
                    },
                }
            },
        )

    @skip('not used in production')
    def test_cpa_incut_delivery_time(self):
        """Проверяем, что под флагом market_cpa_offers_incut_delivery_time во врезке
        с CPA офферами для регионов добавляются сроки доставки
        https://st.yandex-team.ru/MARKETOUT-33604
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&rids=54&rearr-factors=market_cpa_offers_incut_count=3;'
            'market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_delivery_time=1;market_cpa_offers_incut_threshold=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                },
                                "delivery": {"text": "Доставка 2-3 дня"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 2", "raw": True}},
                                },
                                "delivery": {"text": "Доставка 2-3 дня"},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 3", "raw": True}},
                                },
                                "delivery": {"text": "Доставка 2-3 дня"},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_implicit_model_cpa_incut_reasons_to_buy(self):
        """Проверяем reasonsToBuy во врезке с CPA офферами в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-33586
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=1;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                },
                                "reasonsToBuy": [{"id": "alisa_lives_here", "type": "specsFactor", "value": 1}],
                            }
                        ]
                    }
                }
            },
        )

    def test_clid_and_wprid_in_cpa_incut(self):
        """Проверяем clid и wprid в ссылках CPA врезки на покупки и рейтинги моделей
        https://st.yandex-team.ru/MARKETOUT-34028
        https://st.yandex-team.ru/MARKETOUT-34151
        https://st.yandex-team.ru/MARKETOUT-33717
        """
        request = (
            'place=parallel&text=iphone&wprid=user_wprid&rearr-factors=market_cpa_offers_incut_count=1;market_cpa_offers_incut_url_type=PokupkiIncut;'
            'market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0;'
        )

        # Проверяем в офферном колдунщике
        response = self.report.request_bs_pb(request + 'market_offers_wizard_model_rating=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "cpaUrl": LikeUrl.of(
                        "//pokupki.market.yandex.ru/search?text=iphone&lr=0&wprid=user_wprid&clid=545",
                    ),
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        '//pokupki.market.yandex.ru/product/1001?offerid=gTL-3D5IXpiHAL-CvNRmNQ&lr=0&wprid=user_wprid&clid=1601',
                                        ignore_len=False,
                                    ),
                                },
                                "rating": {"value": "4.5"},
                            }
                        ]
                    },
                }
            },
        )

        # Проверяем в колдунщике неявной модели
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "cpaUrl": LikeUrl.of(
                        "//pokupki.market.yandex.ru/search?text=iphone&lr=0&wprid=user_wprid&clid=698",
                    ),
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        '//pokupki.market.yandex.ru/product/1001?offerid=gTL-3D5IXpiHAL-CvNRmNQ&lr=0&wprid=user_wprid&clid=1601',
                                        ignore_len=False,
                                    ),
                                },
                                "rating": {"value": "4.5"},
                            }
                        ]
                    },
                }
            },
        )

    def test_cpa_incut_cashback(self):
        """Проверяем, что под флагом market_cpa_offers_incut_cashback в cpa офферую врезку добавляется информация о кешбеке
        https://st.yandex-team.ru/MARKETOUT-34494
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=1;'
            'market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;'
            'market_cpa_offers_incut_cashback=1;market_extra_cashback_on_parallel=0;market_cpa_offers_incut_threshold=0'
        )

        # Офферный колдунщик
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                },
                                "price": {"type": "average", "priceMax": "100", "currency": "RUR"},
                                "cashback": "20",
                            }
                        ]
                    }
                }
            },
        )

        # Колдунщик неявной модели
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                },
                                "price": {"type": "average", "priceMax": "100", "currency": "RUR"},
                                "cashback": "20",
                            }
                        ]
                    }
                }
            },
        )

    def test_cpa_incut_extra_cashback(self):
        """Проверяем, что под флагом market_extra_cashback_on_parallel в cpa офферую врезку
        добавляется информация о повышенном кешбеке
        Флаг market_extra_cashback_on_parallel=1 раскатан по дефолту.
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&rids=213&rearr-factors=market_cpa_offers_incut_count=1;'
            'market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;'
            'market_cpa_offers_incut_threshold=0'
        )

        # Офферный колдунщик
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                },
                                "price": {"type": "average", "priceMax": "100", "currency": "RUR"},
                                "cashback": "50",
                            }
                        ]
                    }
                }
            },
        )

        # Колдунщик неявной модели
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                },
                                "price": {"type": "average", "priceMax": "100", "currency": "RUR"},
                                "cashback": "50",
                            }
                        ]
                    }
                }
            },
        )

    def test_cpa_incut_src_pof(self):
        """Проверяем, что под флагом market_parallel_add_src_pof_in_urls в cpa офферую врезку
        добавляется src_pof=<clid>
        https://st.yandex-team.ru/MARKETOUT-40423
        """
        # Офферный колдунщик
        rearr_factors = [
            "market_parallel_add_src_pof_in_urls=1",
            "market_cpa_offers_incut_count=1",
            "market_offers_wizard_cpa_offers_incut=1",
            "market_cpa_offers_incut_threshold=0",
        ]
        request = "place=parallel&text=iphone&rearr-factors={}".format(";".join(rearr_factors))
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "adGUrl": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?clid=913&src_pof=913"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?clid=919&src_pof=919"
                                    ),
                                    "url": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?clid=1601&src_pof=1601"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?clid=1601&src_pof=1601"
                                    ),
                                    "urlForCounter": LikeUrl.of("/product/1001?clid=1601&src_pof=1601"),
                                },
                                "thumb": {
                                    "adGUrl": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?clid=913&src_pof=913"
                                    ),
                                    "adGUrlTouch": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?clid=919&src_pof=919"
                                    ),
                                    "url": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?clid=1601&src_pof=1601"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://pokupki.market.yandex.ru/product/1001?clid=1601&src_pof=1601"
                                    ),
                                    "urlForCounter": LikeUrl.of("/product/1001?clid=1601&src_pof=1601"),
                                },
                                "greenUrl": {
                                    "adGUrl": LikeUrl.of("/shop--yandex-market/431782/reviews?clid=913&src_pof=913"),
                                    "adGUrlTouch": LikeUrl.of("/grades-shop.xml?clid=919&src_pof=919"),
                                    "url": LikeUrl.of("/shop--yandex-market/431782/reviews?clid=1601&src_pof=1601"),
                                    "urlTouch": LikeUrl.of("/grades-shop.xml?clid=1601&src_pof=1601"),
                                },
                            }
                        ]
                    }
                }
            },
        )

    def test_cpa_incut_promo_info(self):
        """Проверяем наличие промокода в cpa-врезке
        https://st.yandex-team.ru/MARKETOUT-37482
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&rids=213&rearr-factors=market_cpa_offers_incut_count=1;'
            'market_offers_wizard_cpa_offers_incut=1;'
            'market_implicit_model_cpa_offers_incut=1;'
            'market_cpa_offers_incut_threshold=0;'
            'market_cpa_offers_gift_info_and_promo_code=1;'
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'cpaItems': [{'withGift': 1, 'promocode': {'code': 'promocode_1_text', 'percent': 10}}]
                    }
                }
            },
        )

    def test_cpa_offers_incut_on_top(self):
        """Проверяем что если значение метафильтра CPA офферной врезки превышает пороги, заданные
        флагами market_cpa_offers_incut_on_top_threshold и market_cpa_offers_incut_on_top_collapsing_threshold
        то во врезку добавляется признак для переноса врезки наверх
        https://st.yandex-team.ru/MARKETOUT-34776
        """
        request = (
            'place=parallel&text=iphone&trace_wizard=1&rearr-factors=market_cpa_offers_incut_count=3;'
            'market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0;'
        )

        # 1. По флагу market_cpa_offers_incut_on_top_threshold добавляется признак
        # для переноса наверх "cpaItemsOnTop": "top"
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_on_top_threshold=0.8')
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', response.get_trace_wizard())
        self.assertFragmentIn(
            response, {"market_offers_wizard": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": "top"}}}
        )
        self.assertFragmentIn(
            response, {"market_implicit_model": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": "top"}}}
        )

        # Если значение метафильтра меньше порога market_cpa_offers_incut_on_top_threshold
        # то признак для переноса наверх не добавляется
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_on_top_threshold=1')
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', response.get_trace_wizard())
        self.assertFragmentIn(
            response, {"market_offers_wizard": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": Absent()}}}
        )
        self.assertFragmentIn(
            response, {"market_implicit_model": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": Absent()}}}
        )

        # 2. По флагу market_cpa_offers_incut_on_top_collapsing_threshold добавляется признак
        # для переноса наверх "cpaItemsOnTop": "topCollapsing"
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_on_top_collapsing_threshold=0.7')
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', response.get_trace_wizard())
        self.assertFragmentIn(
            response,
            {"market_offers_wizard": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": "topCollapsing"}}},
        )
        self.assertFragmentIn(
            response,
            {"market_implicit_model": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": "topCollapsing"}}},
        )

        # 3. По флагу market_cpa_offers_incut_on_top_collapsing_threshold_for_model добавляется признак
        # для переноса наверх "cpaItemsOnTop": "topCollapsing"
        response = self.report.request_bs_pb(
            request + 'market_cpa_offers_incut_on_top_collapsing_threshold_for_model=0.7&modelid=12345'
        )
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', response.get_trace_wizard())
        self.assertFragmentIn(
            response,
            {"market_offers_wizard": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": "topCollapsing"}}},
        )
        self.assertFragmentIn(
            response,
            {"market_implicit_model": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": "topCollapsing"}}},
        )

        # Если значение метафильтра меньше порога market_cpa_offers_incut_on_top_collapsing_threshold
        # то признак для переноса наверх не добавляется
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_on_top_collapsing_threshold=1.2')
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', response.get_trace_wizard())
        self.assertFragmentIn(
            response, {"market_offers_wizard": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": Absent()}}}
        )
        self.assertFragmentIn(
            response, {"market_implicit_model": {"showcase": {"cpaItems": ElementCount(3), "cpaItemsOnTop": Absent()}}}
        )

    def test_cpa_offers_incut_dsbs(self):
        """Проверяем, что под флагом market_cpa_offers_incut_dsbs=1 в CPA офферную врезку добавляются DSBS офферы
        https://st.yandex-team.ru/MARKETOUT-34476
        https://st.yandex-team.ru/MARKETOUT-36946
        """
        request = (
            'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=5;'
            'market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;'
            'market_cpa_offers_incut_dsbs=1;market_cpa_offers_incut_threshold=0;'
        )

        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=PokupkiIncut')

        # Офферный колдунщик
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//pokupki.market.yandex.ru/product/1001?lr=0&offerid=gTL-3D5IXpiHAL-CvNRmNQ&clid=1601"
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 2", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//pokupki.market.yandex.ru/product/1002?lr=0&offerid=jsFnEBncNV6VLkT9w4BajQ&clid=1601"
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 3", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//pokupki.market.yandex.ru/product/1003?lr=0&offerid=pnO6jtfjEy9AfE4RIpBsnQ&clid=1601"
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 1", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 3", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 4 without cpc links", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 5 without cpc links", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Колдунщик неявной модели
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//pokupki.market.yandex.ru/product/1001?lr=0&offerid=gTL-3D5IXpiHAL-CvNRmNQ&clid=1601"
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 2", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//pokupki.market.yandex.ru/product/1002?lr=0&offerid=jsFnEBncNV6VLkT9w4BajQ&clid=1601"
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 3", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//pokupki.market.yandex.ru/product/1003?lr=0&offerid=pnO6jtfjEy9AfE4RIpBsnQ&clid=1601"
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 1", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 3", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 4 without cpc links", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 5 without cpc links", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_cpa_offers_incut_url_type=ModelCard DSBS офферы, у которых нет моделей, ведут на КО
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=ModelCard')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 1", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        '//market.yandex.ru/product/2001?hid=10&nid=10&lr=0&do-waremd5=A1c7MrJ0bm6MqYuRI_Ikmw&clid=1601'
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 3", "raw": True}},
                                    "urlForCounter": Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
                                }
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
        )

    def test_cpa_offers_incut_add_to_cart(self):
        """Проверяем, что в CPA врезку добавляются ссылки для добавления товара в корзину
        https://st.yandex-team.ru/MARKETOUT-34892
        https://st.yandex-team.ru/MARKETOUT-35716
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=1;'
            'market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0;'
        )
        addToCartUrl = '%2F%2Fpokupki.market.yandex.ru%2Fbundle%2F1001%3Fdata%3Doffer%252CgTL-3D5IXpiHAL-CvNRmNQ%252C1%26lr%3D0%26schema%3Dtype%252CobjId%252Ccount'

        # Офферный колдунщик
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {"text": {"__hl": {"text": "iphone blue 1", "raw": True}}},
                                "addToCartUrl": Contains(
                                    '//market-click2.yandex.ru/redir/dtype=cpa', 'data=url={}'.format(addToCartUrl)
                                ),
                                "offerId": "gTL-3D5IXpiHAL-CvNRmNQ",
                                "skuId": "1001",
                            }
                        ]
                    }
                }
            },
        )

        # Колдунщик неявной модели
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {"text": {"__hl": {"text": "iphone blue 1", "raw": True}}},
                                "addToCartUrl": Contains(
                                    '//market-click2.yandex.ru/redir/dtype=cpa', 'data=url={}'.format(addToCartUrl)
                                ),
                                "offerId": "gTL-3D5IXpiHAL-CvNRmNQ",
                                "skuId": "1001",
                            }
                        ]
                    }
                }
            },
        )

    def test_cpa_offers_incut_logs(self):
        """Проверяем логирование CPA офферной врезки
        https://st.yandex-team.ru/MARKETOUT-35123
        https://st.yandex-team.ru/MARKETOUT-38890
        """
        offers_access_log_content = [
            '"wiz_id":"market_offers_wizard"',
            '"cpa_offers":["gTL-3D5IXpiHAL-CvNRmNQ","jsFnEBncNV6VLkT9w4BajQ","pnO6jtfjEy9AfE4RIpBsnQ"]',
        ]
        implicit_model_accelss_log_content = [
            '"wiz_id":"market_implicit_model"',
            '"cpa_offers":["gTL-3D5IXpiHAL-CvNRmNQ","jsFnEBncNV6VLkT9w4BajQ","pnO6jtfjEy9AfE4RIpBsnQ"]',
        ]

        request = (
            'place=parallel&text=iphone&&wprid=user_wprid&rearr-factors=market_parallel_feature_log_rate=1;'
            'market_cpa_offers_incut_count=3;market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0;'
        )

        # Ведем на КО
        self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=OfferCard&reqid=1')

        self.access_log.expect(
            reqid=1, wizard_elements=Regex('.*'.join(re.escape(x) for x in offers_access_log_content))
        )
        self.access_log.expect(
            reqid=1, wizard_elements=Regex('.*'.join(re.escape(x) for x in implicit_model_accelss_log_content))
        )

        self.show_log.expect(
            reqid=1,
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/gTL-3D5IXpiHAL-CvNRmNQ'),
            ware_md5='gTL-3D5IXpiHAL-CvNRmNQ',
            wprid='user_wprid',
            incut_type='cpa_offers',
        )
        self.show_log.expect(
            reqid=1,
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/jsFnEBncNV6VLkT9w4BajQ'),
            ware_md5='jsFnEBncNV6VLkT9w4BajQ',
            wprid='user_wprid',
            incut_type='cpa_offers',
        )
        self.show_log.expect(
            reqid=1,
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/pnO6jtfjEy9AfE4RIpBsnQ'),
            ware_md5='pnO6jtfjEy9AfE4RIpBsnQ',
            wprid='user_wprid',
            incut_type='cpa_offers',
        )

        self.feature_log.expect(
            req_id=1, document_type=1, ware_md5='gTL-3D5IXpiHAL-CvNRmNQ', from_blue_stats=1, incut_type='cpa_offers'
        )
        self.feature_log.expect(
            req_id=1, document_type=1, ware_md5='jsFnEBncNV6VLkT9w4BajQ', from_blue_stats=1, incut_type='cpa_offers'
        )
        self.feature_log.expect(
            req_id=1, document_type=1, ware_md5='pnO6jtfjEy9AfE4RIpBsnQ', from_blue_stats=1, incut_type='cpa_offers'
        )

        # Ведем на Покупки
        self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=PokupkiIncut&reqid=2')

        self.access_log.expect(
            reqid=2, wizard_elements=Regex('.*'.join(re.escape(x) for x in offers_access_log_content))
        )
        self.access_log.expect(
            reqid=2, wizard_elements=Regex('.*'.join(re.escape(x) for x in implicit_model_accelss_log_content))
        )

        self.show_log.expect(
            reqid=2,
            url_type=UrlType.POKUPKI_INCUT_URL,
            url=LikeUrl.of('//pokupki.market.yandex.ru/product/1001?offerid=gTL-3D5IXpiHAL-CvNRmNQ&lr=0'),
            ware_md5='gTL-3D5IXpiHAL-CvNRmNQ',
            wprid='user_wprid',
            incut_type='cpa_offers',
        )
        self.show_log.expect(
            reqid=2,
            url_type=UrlType.POKUPKI_INCUT_URL,
            url=LikeUrl.of('//pokupki.market.yandex.ru/product/1002?offerid=jsFnEBncNV6VLkT9w4BajQ&lr=0'),
            ware_md5='jsFnEBncNV6VLkT9w4BajQ',
            wprid='user_wprid',
            incut_type='cpa_offers',
        )
        self.show_log.expect(
            reqid=2,
            url_type=UrlType.POKUPKI_INCUT_URL,
            url=LikeUrl.of('//pokupki.market.yandex.ru/product/1003?offerid=pnO6jtfjEy9AfE4RIpBsnQ&lr=0'),
            ware_md5='pnO6jtfjEy9AfE4RIpBsnQ',
            wprid='user_wprid',
            incut_type='cpa_offers',
        )

        self.feature_log.expect(
            req_id=2, document_type=1, ware_md5='gTL-3D5IXpiHAL-CvNRmNQ', from_blue_stats=1, incut_type='cpa_offers'
        )
        self.feature_log.expect(
            req_id=2, document_type=1, ware_md5='jsFnEBncNV6VLkT9w4BajQ', from_blue_stats=1, incut_type='cpa_offers'
        )
        self.feature_log.expect(
            req_id=2, document_type=1, ware_md5='pnO6jtfjEy9AfE4RIpBsnQ', from_blue_stats=1, incut_type='cpa_offers'
        )

        # Ведем на КМ
        self.report.request_bs_pb(request + 'market_cpa_offers_incut_url_type=ModelCard&reqid=3')

        self.access_log.expect(
            reqid=3, wizard_elements=Regex('.*'.join(re.escape(x) for x in offers_access_log_content))
        )
        self.access_log.expect(
            reqid=3, wizard_elements=Regex('.*'.join(re.escape(x) for x in implicit_model_accelss_log_content))
        )

        self.show_log.expect(
            reqid=3,
            url_type=UrlType.MODEL,
            url=LikeUrl.of(
                '//market.yandex.ru/product/101?do-waremd5=gTL-3D5IXpiHAL-CvNRmNQ&hid=10&lr=0&nid=10&wprid=user_wprid'
            ),
            ware_md5='gTL-3D5IXpiHAL-CvNRmNQ',
            wprid='user_wprid',
            incut_type='cpa_offers',
        )

        self.feature_log.expect(
            req_id=3, document_type=1, ware_md5='gTL-3D5IXpiHAL-CvNRmNQ', from_blue_stats=1, incut_type='cpa_offers'
        )
        self.feature_log.expect(
            req_id=3, document_type=1, ware_md5='jsFnEBncNV6VLkT9w4BajQ', from_blue_stats=1, incut_type='cpa_offers'
        )
        self.feature_log.expect(
            req_id=3, document_type=1, ware_md5='pnO6jtfjEy9AfE4RIpBsnQ', from_blue_stats=1, incut_type='cpa_offers'
        )

    @classmethod
    def prepare_cpa_incut_pickup_and_delivery_info(cls):
        cls.index.shops += [
            Shop(fesh=115, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=116, priority_region=75, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                fesh=115,
                pickup_option=DeliveryOption(price=350, day_from=1, day_to=2, order_before=6),
                cpa=Offer.CPA_REAL,
                title="Kilimanjaro",
            ),
            Offer(
                fesh=116,
                pickup_option=DeliveryOption(price=350, day_from=1, day_to=2, order_before=6),
                cpa=Offer.CPA_REAL,
                title="Kilimanjaro2",
            ),
        ]

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва', tz_offset=10800),
                    Region(rid=10758, name='Химки', tz_offset=10800),
                ],
            ),
            Region(
                rid=11409,
                name='Приморский край',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=75, name='Владивосток', tz_offset=36000),
                ],
            ),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=115, holidays=[1, 2]),
        ]

    def test_cpa_incut_pickup_and_delivery_info(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-34624
        Проверяем, что под флагом market_parallel_cpa_incut_pickup_and_delivery_info
        во врезку с CPA офферами добавляется информация по самовывозу и доставке
        '''

        # Проверяем наличие информации о доставке
        response = self.report.request_bs_pb(
            'place=parallel&text=iphone&rids=54&rearr-factors=market_cpa_offers_incut_count=3;'
            'market_implicit_model_cpa_offers_incut=1;market_parallel_cpa_incut_pickup_and_delivery_info=1;market_cpa_offers_incut_threshold=0'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                },
                                "deliveryOption": {"currency": "RUR", "dayFrom": 2, "dayTo": 3, "price": 0},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 2", "raw": True}},
                                },
                                "deliveryOption": {"currency": "RUR", "dayFrom": 2, "dayTo": 3, "price": 0},
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 3", "raw": True}},
                                },
                                "deliveryOption": {"currency": "RUR", "dayFrom": 2, "dayTo": 3, "price": 0},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем наличие информации о самовывозе с учетом пересчета рабочих дней в календарные
        response = self.report.request_bs_pb(
            'place=parallel&text=Kilimanjaro&rids=213&rearr-factors=market_cpa_offers_incut_count=3;'
            'market_offers_wizard_cpa_offers_incut=1;market_cpa_offers_incut_dsbs=1;market_parallel_cpa_incut_pickup_and_delivery_info=1;'
            'market_offers_wiz_top_offers_threshold=0;market_cpa_offers_incut_threshold=0'
        )
        self.assertFragmentIn(
            response,
            {
                "showcase": {
                    "cpaItems": [
                        {
                            "title": {
                                "text": {"__hl": {"text": "Kilimanjaro", "raw": True}},
                            },
                            "pickupOption": {"currency": "RUR", "dayFrom": 3, "dayTo": 4, "price": 350},
                        }
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем наличие информации о самовывозе с учетом order_before
        response = self.report.request_bs_pb(
            'place=parallel&text=Kilimanjaro&rids=75&rearr-factors=market_cpa_offers_incut_count=3;'
            'market_offers_wizard_cpa_offers_incut=1;market_cpa_offers_incut_dsbs=1;market_parallel_cpa_incut_pickup_and_delivery_info=1;'
            'market_offers_wiz_top_offers_threshold=0;market_cpa_offers_incut_threshold=0'
        )
        self.assertFragmentIn(
            response,
            {
                "showcase": {
                    "cpaItems": [
                        {
                            "title": {
                                "text": {"__hl": {"text": "Kilimanjaro2", "raw": True}},
                            },
                            "pickupOption": {
                                "currency": "RUR",
                                "dayFrom": 2,  # dayFrom из опции + 1 день
                                "dayTo": 3,  # dayTo из опции + 1 день
                                "price": 350,
                            },
                        }
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_supplier_shop_name_for_third_party_offers(self):
        """Проверяем, что 3p офферов отдаем название магазина поставщика
        https://st.yandex-team.ru/MARKETOUT-35713
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=3;market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0;market_offers_incut_supplier_shop_name=1'  # noqa
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                },
                                "greenUrl": {
                                    "text": "Яндекс.Маркет",
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 2", "raw": True}},
                                },
                                "greenUrl": {
                                    "text": "Яндекс.Маркет",
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 3", "raw": True}},
                                },
                                "greenUrl": {
                                    "text": "3p shop",
                                },
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_model_offers_wizard_cpa_incut(self):
        """Проверяем, что cpa-врезка появляется в модельно-офферном колдунщике
        https://st.yandex-team.ru/MARKETOUT-36523
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_enable_model_offers_wizard=1;market_cpa_offers_incut_count=3;market_model_offers_wizard_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0'  # noqa
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {'market_model_offers_wizard': {'showcase': {'cpaItems': NotEmpty()}}})

    def test_feature_log_writing(self):
        """Проверяем, что факторы CPA офферной врезки пишутся в фичелог на 10% запросов
        https://st.yandex-team.ru/MARKETOUT-35759
        https://st.yandex-team.ru/MARKETOUT-37086
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=3;market_offers_wizard_cpa_offers_incut=1'
        offers_count = 3

        # В лог пишутся факторы CPA офферов на каждый market_parallel_feature_log_rate запрос
        # Значение 0 отключает логирование
        for _ in range(10):
            self.report.request_bs_pb(request + ';market_parallel_feature_log_rate=0&reqid=101')
        self.feature_log.expect(req_id=101, document_type=1, from_blue_stats=1).times(0)

        # Под флагом market_parallel_feature_log_rate=1 в лог пишутся факторы CPA офферов всех запросов
        for req_id in range(102, 106):
            self.report.request_bs_pb(request + ';market_parallel_feature_log_rate=1&reqid={}'.format(req_id))
            self.feature_log.expect(req_id=req_id, document_type=1, from_blue_stats=1).times(offers_count)

    def test_presence_cpa_incut(self):
        """Проверяем факторы наличия CPA врезки в колдунщик
        MARKETOUT-36730
        """
        # market_implicit_model
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_count=1;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {"market_implicit_model": {"showcase": {"cpaItems": NotEmptyList()}}})
        self.assertFragmentIn(response, {"Market.Factors": {"cpa_incut.market_implicit_model": 1}})
        # market_offers_wizard
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_threshold=0;market_cpa_offers_incut_count=1;market_offers_wizard_cpa_offers_incut=1'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {"market_offers_wizard": {"showcase": {"cpaItems": NotEmptyList()}}})
        self.assertFragmentIn(response, {"Market.Factors": {"cpa_incut.market_offers_wizard": 1}})
        # market_model_offers_wizard
        request = 'place=parallel&text=iphone&rearr-factors=market_enable_model_offers_wizard=1;market_cpa_offers_incut_count=3;market_model_offers_wizard_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0'  # noqa
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {'market_model_offers_wizard': {'showcase': {'cpaItems': NotEmpty()}}})
        self.assertFragmentIn(response, {"Market.Factors": {"cpa_incut.market_model_offers_wizard": 1}})

    def test_cpa_filter_in_title_url(self):
        """Проверяем условия для добавления CPA фильтра в ссылки тайтла колдунщиков
        https://st.yandex-team.ru/MARKETOUT-37552
        """
        query = 'place=parallel&text=iphone&rearr-factors=market_cpa_filter_in_wizard_title_urls=1;'
        cpa_flags = 'market_cpa_offers_incut_count=1;market_offers_wizard_cpa_offers_incut=1;market_implicit_model_cpa_offers_incut=1;market_cpa_offers_incut_threshold=0;'

        # 1. Проверяем название вьютайпа
        # Под флагом market_cpa_filter_in_wizard_title_urls_vt=market_offers_wizard CPA фильтр добавляется только в офферный колдунщик
        response = self.report.request_bs_pb(query + 'market_cpa_filter_in_wizard_title_urls_vt=market_offers_wizard')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=698", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=721", no_params=['cpa']),
                }
            },
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls_vt=market_implicit_model CPA фильтр добавляется только в колдунщик неявной модели
        response = self.report.request_bs_pb(query + 'market_cpa_filter_in_wizard_title_urls_vt=market_implicit_model')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708", no_params=['cpa']),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=698&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=721&cpa=1"),
                }
            },
        )

        # 2. Проверяем порог классификатора товарных запросов
        # Под флагом market_cpa_filter_in_wizard_title_urls_product_threshold=0.5 значение товароного классификатора проходит порог, CPA фильтр добавляется
        response = self.report.request_bs_pb(query + 'market_cpa_filter_in_wizard_title_urls_product_threshold=0.5')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=698&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=721&cpa=1"),
                }
            },
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls_product_threshold=0.8 значение товароного классификатора не проходит порог, CPA фильтр не добавляется
        response = self.report.request_bs_pb(query + 'market_cpa_filter_in_wizard_title_urls_product_threshold=0.8')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708", no_params=['cpa']),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=698", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=721", no_params=['cpa']),
                }
            },
        )

        # 3. Проверяем наличие CPA врезки
        # Под флагом market_cpa_filter_in_wizard_title_urls_has_cpa_incut=1 CPA фильтр добавляется при наличии CPA врезки
        response = self.report.request_bs_pb(
            query + cpa_flags + 'market_cpa_filter_in_wizard_title_urls_has_cpa_incut=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708&cpa=1"),
                    "showcase": {"cpaItems": NotEmptyList()},
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=698&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=721&cpa=1"),
                    "showcase": {"cpaItems": NotEmptyList()},
                }
            },
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls_has_cpa_incut=1 CPA фильтр не добавляется если CPA врезки нет
        response = self.report.request_bs_pb(
            query
            + cpa_flags
            + 'market_cpa_filter_in_wizard_title_urls_has_cpa_incut=1;market_cpa_offers_incut_threshold=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708", no_params=['cpa']),
                    "showcase": {"cpaItems": Absent()},
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=698", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=721", no_params=['cpa']),
                    "showcase": {"cpaItems": Absent()},
                }
            },
        )

        # 4. Проверяем количество синих офферов
        # Под флагом market_cpa_filter_in_wizard_title_urls_blue_offers_count=2 количество найденных синих офферов больше порога, CPA фильтр добавляется
        response = self.report.request_bs_pb(
            query + cpa_flags + 'market_cpa_filter_in_wizard_title_urls_blue_offers_count=2'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=698&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=721&cpa=1"),
                }
            },
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls_blue_offers_count=4 количество найденных синих офферов меньше порога, CPA фильтр не добавляется
        response = self.report.request_bs_pb(
            query + cpa_flags + 'market_cpa_filter_in_wizard_title_urls_blue_offers_count=4'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=545", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=708", no_params=['cpa']),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&lr=0&clid=698", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&lr=0&clid=721", no_params=['cpa']),
                }
            },
        )

    def test_hide_cpc_offers_in_cpa_offers_incut(self):
        """Проверяем, что под флагом market_cpa_offers_incut_hide_duplicates если оффер содержится и в обычной и в CPA врезках,
        то в CPA врезке он помечается признаком для сокрытия
        https://st.yandex-team.ru/MARKETOUT-35176
        """
        request = (
            'place=parallel&text=iphone&trace_wizard=1&rearr-factors='
            'market_cpa_offers_incut_count=2;'
            'market_offers_wizard_cpa_offers_incut=1;'
            'market_cpa_offers_incut_hide_duplicates=1;'
        )

        # Под флагом market_cpa_offers_incut_hide_duplicates=1 офферы из обычной врезки не удаляются.
        # CPA врезка без дублей проходит порог мета-фильтра,
        # офферы, которые есть в обычной врезке, в CPA врезке помечаются признаком "hide".
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_threshold=0.1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                }
                            }
                        ],
                        "cpaItems": [
                            {"title": {"text": {"__hl": {"text": "iphone blue 1", "raw": True}}}, "hide": True},
                            {"title": {"text": {"__hl": {"text": "iphone blue 2", "raw": True}}}, "hide": Absent()},
                            {"title": {"text": {"__hl": {"text": "iphone blue 3", "raw": True}}}, "hide": Absent()},
                        ],
                    }
                }
            },
        )

        trace_wizard = response.get_trace_wizard()
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', trace_wizard)
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_cpa_offers_incut_threshold=0.1', trace_wizard
        )
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut no duplicates: 0.6', trace_wizard)
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_cpa_offers_incut_threshold_no_duplicates=0.1',
            trace_wizard,
        )

        self.assertFragmentIn(
            response,
            {
                "Market.Factors": {
                    "MarketplaceIncutThreshold": Round(0.1),
                    "MarketplaceIncutRelevance": Round(0.9),
                    "MarketplaceIncutRelevanceNoDuplicates": Round(0.6),
                }
            },
        )

        # Под флагом market_cpa_offers_incut_hide_duplicates=1 офферы из обычной врезки не удаляются.
        # CPA врезка без дублей не проходи порог мета-фильтра,
        # все офферы CPA врезке помечаются признаком "hide".
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_threshold=0.7')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                }
                            }
                        ],
                        "cpaItems": [
                            {"title": {"text": {"__hl": {"text": "iphone blue 1", "raw": True}}}, "hide": True},
                            {"title": {"text": {"__hl": {"text": "iphone blue 2", "raw": True}}}, "hide": True},
                            {"title": {"text": {"__hl": {"text": "iphone blue 3", "raw": True}}}, "hide": True},
                        ],
                    }
                }
            },
        )

        trace_wizard = response.get_trace_wizard()
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', trace_wizard)
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_cpa_offers_incut_threshold=0.7', trace_wizard
        )
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut no duplicates: 0.6', trace_wizard)
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_cpa_offers_incut_threshold_no_duplicates=0.7',
            trace_wizard,
        )

        self.assertFragmentIn(
            response,
            {
                "Market.Factors": {
                    "MarketplaceIncutThreshold": Round(0.7),
                    "MarketplaceIncutRelevance": Round(0.9),
                    "MarketplaceIncutRelevanceNoDuplicates": Round(0.6),
                }
            },
        )

        # Под флагом market_cpa_offers_incut_hide_duplicates=1 офферы из обычной врезки не удаляются.
        # CPA врезка с дублями не проходит порог мета-фильтра,
        # CPA врезка не формируется.
        response = self.report.request_bs_pb(request + 'market_cpa_offers_incut_threshold=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone blue 1", "raw": True}},
                                }
                            }
                        ],
                        "cpaItems": Absent(),
                    }
                }
            },
        )

        trace_wizard = response.get_trace_wizard()
        self.assertIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut: 0.9', trace_wizard)
        self.assertIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_cpa_offers_incut_threshold=1', trace_wizard
        )
        self.assertNotIn('10 1 Top 5 meta MatrixNet sum for cpa offers incut no duplicates:', trace_wizard)
        self.assertNotIn(
            '10 1 OfferIncut.TopBlueOffersMnValue.Meta.Threshold: market_cpa_offers_incut_threshold_no_duplicates=',
            trace_wizard,
        )

        self.assertFragmentIn(
            response,
            {
                "Market.Factors": {
                    "MarketplaceIncutThreshold": Round(1.0),
                    "MarketplaceIncutRelevance": Round(0.9),
                    "MarketplaceIncutRelevanceNoDuplicates": Absent(),
                }
            },
        )

    def test_nearest_outlet(self):
        """Проверяем нахождение ближайшего ПВЗ или постомата к координатам пользователя
        https://st.yandex-team.ru/MARKETOUT-39128
        https://st.yandex-team.ru/MARKETOUT-42426
        https://st.yandex-team.ru/MARKETOUT-43013
        """
        profile = make_profile(
            [],
            regular_coords=[
                RegularCoords(
                    longitude=37.55, latitude=55.55, region_id=213, location_type=RegularCoords.HOME, update_time=1
                ),
                RegularCoords(
                    longitude=37.45, latitude=55.45, region_id=213, location_type=RegularCoords.WORK, update_time=2
                ),
                RegularCoords(
                    longitude=37.65, latitude=55.65, region_id=213, location_type=RegularCoords.UNKNOWN, update_time=3
                ),
            ],
        )
        encoded_profile = base64.b64encode(profile.SerializeToString())
        request = 'place=parallel&text=iphone&rids=213&rearr-factors=market_nordstream_relevance=0;market_cpa_offers_incut_threshold=0;market_cpa_offers_incut_hide_duplicates=0;market_parallel_calc_nearest_outlet=1&bigb_response_market_parallel_b64={}'.format(  # noqa
            encoded_profile
        )

        # ПВЗ/постамат есть у 1 из 2 офферов, информация добавляется
        response = self.report.request_bs_pb(request + "&rearr-factors=market_cpa_offers_incut_count=2")
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'nearestOutlet': {
                        'type': 'pvz',
                        'from': 'deliveryPlace',
                        'distance': 1279,
                        'mapsUrl': 'maps.yandex.ru/?ll=37.64,55.64&text=Moscow, Random st., д. 74 OUTLET-431782-3&z=17&results=1',
                        'address': 'Moscow, Random st., д. 74',
                    }
                }
            },
        )

        # ПВЗ/постамат есть у 1 из 3 офферов (менее 50%), информация не добавляется
        response = self.report.request_bs_pb(request + "&rearr-factors=market_cpa_offers_incut_count=3")
        self.assertFragmentNotIn(response, {'market_offers_wizard': {'nearestOutlet': {}}})

        # убираем город из адреса под флагом
        response = self.report.request_bs_pb(
            request + "&rearr-factors=market_cpa_offers_incut_count=2;market_parallel_hide_city_from_nearest_outlet=1"
        )
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'nearestOutlet': {
                        'mapsUrl': 'maps.yandex.ru/?ll=37.64,55.64&text=Moscow, Random st., д. 74 OUTLET-431782-3&z=17&results=1',
                        'address': 'Random st., д. 74',
                    }
                }
            },
        )

    @classmethod
    def prepare_promocode(cls):
        promo_code_low = Promo(
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_low_text',
            description='promocode_low_description',
            discount_value=500,
            discount_currency='RUR',
            key=b64url_md5(next(nummer)),
            shop_promo_id='promocode_low',
            offers_matching_rules=[OffersMatchingRules(mskus=[5001])],
        )

        promo_code_high = Promo(
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_high_text',
            description='promocode_high_description',
            discount_value=10,  # promocodes without currency are in percents
            key=b64url_md5(next(nummer)),
            shop_promo_id='promocode_high',
            offers_matching_rules=[OffersMatchingRules(mskus=[5001])],
        )

        # promocodes with order_min_price field are cart-promocodes
        promo_code_in_cart = Promo(
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_in_cart_text',
            description='promocode_in_cart_description',
            discount_value=1000,
            discount_currency='RUR',
            key=b64url_md5(next(nummer)),
            shop_promo_id='promocode_in_cart',
            restrictions=PromoRestrictions(
                order_min_price={
                    'value': 13000,
                    'currency': 'RUR',
                }
            ),
            offers_matching_rules=[OffersMatchingRules(mskus=[5002])],
        )

        cls.index.mskus += [
            MarketSku(
                sku=5001,
                title="promocode offer 1",
                blue_offers=[
                    BlueOffer(
                        ts=105001,
                        feedid=1000,
                        offerid='promocode-offer-1',
                        price=9000,
                        price_old=10000,
                        promo=[promo_code_low, promo_code_high, promo_code_in_cart],
                        hid=10,
                    )
                ],
            ),
            MarketSku(
                sku=5002,
                title="promocode offer 2",
                blue_offers=[
                    BlueOffer(
                        ts=105002,
                        feedid=1000,
                        offerid='promocode-offer-2',
                        price=12000,
                        promo=[promo_code_in_cart],
                        hid=10,
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(title="offer with no promocode 1", fesh=2, ts=501),
            Offer(title="offer with no promocode 2", fesh=2, ts=502),
            Offer(title="offer with no promocode 3", fesh=2, ts=503),
            Offer(title="offer with no promocode 4", fesh=2, ts=504),
        ]

    def test_promocodes_apply(self):
        """Проверяем применение промокодов к цене cpa офферов
        https://st.yandex-team.ru/MARKETOUT-39918
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=promocode&rids=213&rearr-factors='
            'market_cpa_offers_incut_count=2;'
            'market_offers_wizard_cpa_offers_incut=1;'
            'market_implicit_model_cpa_offers_incut=1;'
            'market_cpa_offers_incut_threshold=0;'
            'market_cpa_offers_gift_info_and_promo_code=1;'
            'market_cpa_offers_apply_promo_code=1;'
        )

        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': {
                    'showcase': {
                        'cpaItems': [
                            {
                                'title': {
                                    "text": {"__hl": {"text": "promocode offer 1", "raw": True}},
                                },
                                'price': {
                                    "priceMax": "8100",  # 10000 - 1000(usual discount) - 10%(900 as promocode percent discount) = 8100
                                },
                                'discount': {'oldprice': "10000"},
                                'promocode': {
                                    'code': 'promocode_high_text',
                                    'percent': 19,  # (10000 - 8100) / 10000 * 100%
                                    'isApplied': True,
                                    'withDiscount': True,
                                },
                            },
                            {
                                'title': {
                                    "text": {"__hl": {"text": "promocode offer 2", "raw": True}},
                                },
                                'price': {
                                    "priceMax": "12000",  # Промокод не применяется
                                },
                                'promocode': {'code': 'promocode_in_cart_text', 'value': 1000},
                            },
                        ]
                    }
                }
            },
        )

        # Промокод не применяется, если цена оффера ниже цены применения промокода
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "promocode offer 2", "raw": True}},
                                },
                                "promocode": {
                                    "code": "promocode_in_cart_text",
                                    "value": 1000,
                                    "isApplied": True,
                                },
                            }
                        ]
                    }
                }
            },
        )

    def test_cpc_offers_incut_filter_no_cpc_links(self):
        """Проверяем, что под флагом market_cpc_offers_incut_filter_no_cpc_links=1 при включении DSBS
        в CPC офферной врезке нет офферов без ссылок в магазин
        https://st.yandex-team.ru/MARKETOUT-40682
        https://st.yandex-team.ru/MARKETOUT-41077
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_dsbs=1;market_offers_wizard_incut_url_type=External;'

        # Под флагом market_cpc_offers_incut_filter_no_cpc_links=1 офферы без ссылки в магазин отфильтровываются
        response = self.report.request_bs_pb(request + 'market_cpc_offers_incut_filter_no_cpc_links=1')
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "iphone dsbs 4 without cpc links", "raw": True}}}}
                        ]
                    }
                }
            },
        )

        # Без флага market_cpc_offers_incut_filter_no_cpc_links=1 офферы без ссылки в магазин ведут на Маркет
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 4 without cpc links", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/product/2005?do-waremd5=3C819zhGvv1gpOdrTglNIA&clid=545"
                                    ),
                                }
                            }
                        ]
                    }
                }
            },
        )

    def test_cpc_offers_incut_one_dsbs(self):
        """Проверяем, что под флагом market_cpc_offers_incut_one_dsbs=1 в CPC офферной врезке остается
        только один CPA или DSBS оффер, ведущий на Маркет
        https://st.yandex-team.ru/MARKETOUT-41077
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_offers_incut_align=0;market_cpa_offers_incut_dsbs=1;market_offers_wizard_incut_url_type=External;'
        response = self.report.request_bs_pb(request + 'market_cpc_offers_incut_one_dsbs=1;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {"text": "Яндекс.Маркет"},
                                "title": {"text": {"__hl": {"text": "iphone blue 1", "raw": True}}},
                            },
                            {
                                "greenUrl": {"text": "SHOP-2"},
                                "title": {"text": {"__hl": {"text": "iphone white 1", "raw": True}}},
                            },
                            {
                                "greenUrl": {"text": "SHOP-5"},
                                "title": {"text": {"__hl": {"text": "iphone dsbs 1", "raw": True}}},
                            },
                            {
                                "greenUrl": {"text": "SHOP-6"},
                                "title": {"text": {"__hl": {"text": "iphone dsbs 2", "raw": True}}},
                            },
                            {
                                "greenUrl": {"text": "SHOP-7"},
                                "title": {"text": {"__hl": {"text": "iphone dsbs 3", "raw": True}}},
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Без флага market_cpc_offers_incut_one_dsbs во врезке 2 оффера ведут на Маркет
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {"text": "Яндекс.Маркет"},
                                "title": {"text": {"__hl": {"text": "iphone blue 1", "raw": True}}},
                            },
                            {
                                "greenUrl": {"text": "Яндекс.Маркет"},
                                "title": {"text": {"__hl": {"text": "iphone dsbs 4 without cpc links", "raw": True}}},
                            },
                        ]
                    }
                }
            },
        )

    def test_cpc_offers_incut_dsbs_url_type(self):
        """Проверяем, что флаг market_offers_wizard_incut_dsbs_url_type задает тип ссылки для DSBS офферов без ссылки в магазин
        https://st.yandex-team.ru/MARKETOUT-41077
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_offers_incut_align=0;market_cpa_offers_incut_dsbs=1;market_offers_wizard_incut_url_type=External;'

        # Под флагом market_offers_wizard_incut_dsbs_url_type=OfferCard ведем на КО
        response = self.report.request_bs_pb(request + 'market_offers_wizard_incut_dsbs_url_type=OfferCard')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 4 without cpc links", "raw": True}},
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard"),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 5 without cpc links", "raw": True}},
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard"),
                                }
                            },
                        ]
                    }
                }
            },
        )

        # Под флагом market_offers_wizard_incut_dsbs_url_type=ModelCard ведем на КМ
        # Оффер без модели ведет на КО
        response = self.report.request_bs_pb(request + 'market_offers_wizard_incut_dsbs_url_type=ModelCard')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 4 without cpc links", "raw": True}},
                                    "urlForCounter": LikeUrl.of(
                                        "//market.yandex.ru/product/2005?do-waremd5=3C819zhGvv1gpOdrTglNIA&clid=545"
                                    ),
                                }
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "iphone dsbs 5 without cpc links", "raw": True}},
                                    "urlForCounter": Contains("//market-click2.yandex.ru/redir/dtype=offercard"),
                                }
                            },
                        ]
                    }
                }
            },
        )


if __name__ == '__main__':
    main()

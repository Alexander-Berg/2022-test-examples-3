#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    ClickType,
    MnPlace,
    Model,
    MarketSku,
    BlueOffer,
    Shop,
    ReportState,
)
from core.click_context import ClickContext
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare_sponsored_offers_pattern(cls):
        cls.index.shops += [
            Shop(
                fesh=13100 + i,
                datafeed_id=13100 + i,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
            )
            for i in range(1, 10)
        ]
        cls.index.models += [Model(hid=12900, hyperid=13100 + i, ts=13100 + i) for i in range(1, 10)]
        cls.index.mskus += [
            MarketSku(
                hid=12900,
                hyperid=13100 + i,
                sku=13300 + i,
                blue_offers=[BlueOffer(price=100000, fesh=13100 + i, ts=13100 + i)],
            )
            for i in range(1, 10)
        ]
        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13100 + i).respond(0.459 - i * 0.00018)

        cls.index.shops += [
            Shop(
                fesh=13200 + i,
                datafeed_id=13200 + i,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
            )
            for i in range(1, 10)
        ]
        cls.index.models += [Model(hid=12900, hyperid=13200 + i, ts=13200 + i) for i in range(1, 10)]
        cls.index.mskus += [
            MarketSku(
                hid=12900,
                hyperid=13200 + i,
                sku=13400 + i,
                blue_offers=[BlueOffer(price=100000, fesh=13200 + i, ts=13200 + i, fee=400)],
            )
            for i in range(1, 10)
        ]
        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13200 + i).respond(0.457 - i * 0.00018)

    # проверяем работу флага market_report_mimicry_in_serp_pattern, включающего трафареты.
    # для десктопа спонсорские документы должны быть с pp = 231
    def test_sponsorred_offers_pattern_desktop(self):
        request = (
            'pp=7&hid=12900'
            '&rearr-factors=market_white_ungrouping=1'
            ';market_report_mimicry_in_serp_pattern=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=desktop'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'sponsored': True,
                                    }
                                ]
                            },
                            'sponsored': True,
                        },
                    ]
                }
            },
        )
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13100 + i, pp=7, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13100 + i, pp=7, url_type=ClickType.CPA, shop_fee=0)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13200 + i, pp=231, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13200 + i, pp=231, url_type=ClickType.CPA, shop_fee=400)

    # Do the same request to get sponsored products in search result
    # Check that sponsored products do not reach reportState
    def test_sponsored_offers_not_in_report_state(self):
        modelsAndSkusReturned = [(13100 + i, 13300 + i) for i in range(1, 10)] + [(13209, 13409)]

        rs = ReportState.create()
        for modelId, msku in modelsAndSkusReturned:
            doc = rs.offer_card_state.also_viewed_docs.add()
            doc.model_id = str(modelId)
            doc.sku_id = str(msku)

        request = (
            'pp=7&hid=12900'
            '&rearr-factors=market_put_models_and_skus_to_rs=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )

        self.assertFragmentIn(
            self.report.request_json(request),
            {
                "total": 18,
                "reportState": ReportState.serialize(rs).replace('=', ','),
            },
            preserve_order=True,
            allow_different_len=False,
        )

    # проверяем заполнение поля cc с правильным pp для sponsored офферов
    def test_sponsorred_offers_pattern_click_context(self):
        request = (
            'pp=7&hid=12900'
            '&rearr-factors=market_white_ungrouping=1'
            ';market_report_mimicry_in_serp_pattern=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        click_context_search = str(ClickContext(pp=7))
        click_context_sponsored = str(ClickContext(pp=231))
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'cc': click_context_sponsored,
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'cc': click_context_sponsored,
                                        'sponsored': True,
                                    }
                                ]
                            },
                            'sponsored': True,
                            'debug': {'modelId': 13201},
                        },
                        {
                            'entity': 'product',
                            'cc': click_context_search,
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'cc': click_context_search,
                                        'sponsored': Absent(),
                                    }
                                ]
                            },
                            'sponsored': Absent(),
                            'debug': {'modelId': 13101},
                        },
                    ]
                }
            },
        )

    # проверяем что в блендере sponsored оффера логируются правильно
    def test_sponsorred_offers_pattern_pp_in_blender(self):
        request = (
            'pp=7&hid=12900'
            '&rearr-factors=market_white_ungrouping=1'
            ';market_report_mimicry_in_serp_pattern=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=desktop'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1&blender=1'
        )
        self.report.request_json(request)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13100 + i, pp=7, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13100 + i, pp=7, url_type=ClickType.CPA, shop_fee=0)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13200 + i, pp=231, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13200 + i, pp=231, url_type=ClickType.CPA, shop_fee=400)

    # проверим пп выдаче тача
    def test_sponsorred_offers_pattern_touch(self):
        request = (
            'pp=48&hid=12900'
            '&rearr-factors=market_white_ungrouping=1'
            ';market_report_mimicry_in_serp_pattern=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
            '&viewtype=grid&touch=1'
        )
        self.report.request_json(request)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13100 + i, pp=48, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13100 + i, pp=48, url_type=ClickType.CPA, shop_fee=0)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13200 + i, pp=621, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13200 + i, pp=621, url_type=ClickType.CPA, shop_fee=400)

    # проверяем pp для андроида
    def test_sponsorred_offers_pattern_android(self):
        request = (
            'pp=1707&hid=12900'
            '&rearr-factors=market_white_ungrouping=1'
            ';market_report_mimicry_in_serp_pattern=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
            '&client=ANDROID'
        )
        self.report.request_json(request)
        self.report.request_json(request)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13100 + i, pp=1707, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13100 + i, pp=1707, url_type=ClickType.CPA, shop_fee=0)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13200 + i, pp=1710, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13200 + i, pp=1710, url_type=ClickType.CPA, shop_fee=400)

    # проверяем pp для ios
    def test_sponsorred_offers_pattern_ios(self):
        request = (
            'pp=1807&hid=12900'
            '&rearr-factors=market_white_ungrouping=1'
            ';market_report_mimicry_in_serp_pattern=2'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
            '&viewtype=grid&client=IOS'
        )
        self.report.request_json(request)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13100 + i, pp=1807, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13100 + i, pp=1807, url_type=ClickType.CPA, shop_fee=0)
        for i in range(1, 8):
            self.show_log.expect(hyper_id=13200 + i, pp=1810, url_type=ClickType.MODEL)
            self.show_log.expect(hyper_id=13200 + i, pp=1810, url_type=ClickType.CPA, shop_fee=400)

    # проверим, что с нулевым влиянием ставки sponsored документов нет,
    # вся выдача без аукциона и отсортирована по mnValue
    def test_sponsorred_offers_pattern_zeroed_bids(self):
        request = (
            'pp=7&hid=12900'
            '&rearr-factors=market_white_ungrouping=1'
            ';market_report_mimicry_in_serp_pattern=1'
            ';market_tweak_search_auction_white_cpa_fee_no_text_params_desktop=0,0,0'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=desktop'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'sponsored': True,
                        }
                    ]
                }
            },
        )
        for i in range(1, 10):
            self.show_log.expect(hyper_id=13100 + i, position=i, pp=7, url_type=ClickType.MODEL)
        for i in range(1, 10):
            self.show_log.expect(hyper_id=13200 + i, position=9 + i, pp=7, url_type=ClickType.MODEL)

    # проверим, что выключенными версиями трафаретов,
    # вся выдача без аукциона и отсортирована по mnValue
    def test_disabled_sponsorred_offers(self):
        request = (
            'pp=7&hid=12900'
            '&rearr-factors=market_white_ungrouping=1'
            ';market_buybox_auction_search_sponsored_places_web=0'
            ';market_premium_ads_in_search_sponsored_places_web=0'
            ';market_report_mimicry_in_serp_pattern=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=desktop'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'sponsored': True,
                        }
                    ]
                }
            },
        )
        for i in range(1, 10):
            self.show_log.expect(hyper_id=13100 + i, position=i, pp=7, url_type=ClickType.MODEL)
        for i in range(1, 10):
            self.show_log.expect(hyper_id=13200 + i, position=9 + i, pp=7, url_type=ClickType.MODEL)


if __name__ == '__main__':
    main()

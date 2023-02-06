#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    Autostrategy,
    AutostrategyType,
    AutostrategyWithDatasourceId,
    ClickType,
    IncutBlackListFb,
    MnPlace,
    Model,
    Offer,
    Shop,
)
from core.matcher import GreaterFloat


class T(TestCase):
    @classmethod
    def prepare_sponsored_offers_black_list(cls):
        cls.index.shops += [
            Shop(fesh=14100 + i, cpa=Shop.CPA_REAL, cpa20=Shop.CPA_REAL, priority_region=213) for i in range(1, 10)
        ]
        cls.index.models += [
            Model(hid=13900, hyperid=14100 + i, ts=14100 + i, title="наноболт 8x{} мм".format(i + 15))
            for i in range(1, 10)
        ]
        cls.index.offers += [
            Offer(
                hid=13900,
                hyperid=14100 + i,
                ts=14100 + i,
                price=100000,
                cpa=Offer.CPA_REAL,
                fesh=14100 + i,
                title="наноболт 8x{} мм".format(i + 15),
            )
            for i in range(1, 10)
        ]
        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 14100 + i).respond(0.459 - i * 0.00018)

        cls.index.shops += [
            Shop(fesh=14200 + i, cpa=Shop.CPA_REAL, cpa20=Shop.CPA_REAL, priority_region=213) for i in range(1, 10)
        ]
        cls.index.models += [
            Model(
                hid=13900,
                hyperid=14200 + i,
                ts=14200 + i,
                title="наноболт 6x{} мм".format(i + 15),
                vbid=1000,
                datasource_id=13900 + i,
            )
            for i in range(1, 10)
        ]
        cls.index.offers += [
            Offer(
                hid=13900,
                hyperid=14200 + i,
                ts=14200 + i,
                price=100000,
                cpa=Offer.CPA_REAL,
                fesh=14200 + i,
                fee=400,
                bid=5000,
                cbid=400,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=13900 + i,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1000),
                ),
                title="наноболт 6x{} мм".format(i + 15),
            )
            for i in range(1, 10)
        ]
        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 14200 + i).respond(0.457 - i * 0.00018)

        cls.index.incut_black_list_fb += [
            IncutBlackListFb(texts=['наноболт'], inclids=['Mimicry']),
            IncutBlackListFb(subtreeHids=[13900], inclids=['Mimicry']),
        ]

    # проверяем работу флага market_output_advert_request_blacklist_fb,
    # выключающего спонсорские товары при наличии фраз из блэклиста в запросе
    def test_sponsorred_offers_black_list_text(self):
        request = (
            'pp=7&text=наноболт'
            '&rearr-factors=market_white_ungrouping=1;market_metadoc_search=no'
            ';market_report_mimicry_in_serp_pattern=1'
            ';market_output_advert_request_blacklist_fb=0'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'sponsored': True,
                        }
                    ]
                }
            },
        )
        request = (
            'pp=7&text=наноболт'
            '&rearr-factors=market_white_ungrouping=1;market_metadoc_search=no'
            ';market_report_mimicry_in_serp_pattern=1'
            ';market_output_advert_request_blacklist_fb=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'sponsored': True,
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(response, {'AUCTION_MULTIPLIER': GreaterFloat(1.0)})
        self.show_log.expect(hyper_id=14101, position=1, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14102, position=2, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14103, position=3, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14201, position=10, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14202, position=11, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14203, position=12, url_type=ClickType.MODEL)

    # проверяем работу флага market_output_advert_request_blacklist_fb,
    # выключающего спонсорские товары при наличии hid из блэклиста в запросе
    def test_sponsorred_offers_black_list_hid(self):
        request = (
            'pp=7&hid=13900'
            '&rearr-factors=market_white_ungrouping=1;market_metadoc_search=no'
            ';market_report_mimicry_in_serp_pattern=1'
            ';market_output_advert_request_blacklist_fb=0'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'sponsored': True,
                        }
                    ]
                }
            },
        )
        request = (
            'pp=7&hid=13900'
            '&rearr-factors=market_white_ungrouping=1;market_metadoc_search=no'
            ';market_report_mimicry_in_serp_pattern=1'
            ';market_output_advert_request_blacklist_fb=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'sponsored': True,
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(response, {'AUCTION_MULTIPLIER': GreaterFloat(1.0)})
        self.show_log.expect(hyper_id=14101, position=1, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14102, position=2, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14103, position=3, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14201, position=10, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14202, position=11, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14203, position=12, url_type=ClickType.MODEL)

    # проверяем работу флага market_output_advert_request_blacklist_fb,
    # выключающего аукцион при наличии фраз из блэклиста в запросе
    def test_autcion_black_list_text(self):
        request = (
            'pp=7&text=наноболт'
            '&rearr-factors=market_white_ungrouping=1;market_metadoc_search=no'
            ';market_report_mimicry_in_serp_pattern=0'
            ';market_output_advert_request_blacklist_fb=0'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        self.report.request_json(request)
        self.show_log.expect(hyper_id=14201, position=1, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14202, position=2, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14203, position=3, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14101, position=10, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14102, position=11, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14103, position=12, url_type=ClickType.MODEL)
        request = (
            'pp=7&text=наноболт'
            '&rearr-factors=market_white_ungrouping=1;market_metadoc_search=no'
            ';market_report_mimicry_in_serp_pattern=0'
            ';market_output_advert_request_blacklist_fb=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {'AUCTION_MULTIPLIER': GreaterFloat(1.0)})
        self.show_log.expect(hyper_id=14101, position=1, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14102, position=2, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14103, position=3, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14201, position=10, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14202, position=11, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14203, position=12, url_type=ClickType.MODEL)

    # проверяем работу флага market_output_advert_request_blacklist_fb,
    # выключающего аукцион при наличии hid из блэклиста в запросе
    def test_autcion_black_list_hid(self):
        request = (
            'pp=7&hid=13900'
            '&rearr-factors=market_white_ungrouping=1;market_metadoc_search=no'
            ';market_report_mimicry_in_serp_pattern=0'
            ';market_output_advert_request_blacklist_fb=0'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        self.report.request_json(request)
        self.show_log.expect(hyper_id=14201, position=1, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14202, position=2, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14203, position=3, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14101, position=10, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14102, position=11, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14103, position=12, url_type=ClickType.MODEL)
        request = (
            'pp=7&hid=13900'
            '&rearr-factors=market_white_ungrouping=1;market_metadoc_search=no'
            ';market_report_mimicry_in_serp_pattern=0'
            ';market_output_advert_request_blacklist_fb=1'
            '&show-urls=external,decrypted,direct%2Ccpa,productVendorBid&bsformat=2&place=prime&platform=touch'
            '&use-default-offers=1&allow-collapsing=1&rids=213&waitall=da&numdoc=48&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {'AUCTION_MULTIPLIER': GreaterFloat(1.0)})
        self.show_log.expect(hyper_id=14101, position=1, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14102, position=2, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14103, position=3, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14201, position=10, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14202, position=11, url_type=ClickType.MODEL)
        self.show_log.expect(hyper_id=14203, position=12, url_type=ClickType.MODEL)


if __name__ == '__main__':
    main()

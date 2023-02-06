#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import BlueOffer, MarketSku, Model, Offer, Shop
from core.testcase import TestCase, main
from core.logs import LogBrokerTopicBackend, ErrorboosterLogFrontend
from core.logbroker import LogBrokerClient, ERRORBOOSTER_TOPIC


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.logbroker_enabled = True
        cls.emergency_flags.add_flags(use_errorbooster=1)
        cls.index.models += [
            Model(title="kiyanka model has_pic", hyperid=1, vendor_id=1348),  # picinfo will be autogenerated
            Model(title="kiyanka model no_pic", no_picture=True, no_add_picture=True, hyperid=2),
        ]

        cls.index.offers += [
            Offer(title='kiyanka offer has_pic', picture="1", waremd5='wgrU12_pd1mqJ6DJm_9nEA', vendor_id=3491),
            Offer(title='kiyanka offer no_pic', no_picture=True, waremd5='ZRK9Q9nKpuAsmQsKgmUtyg'),
        ]

        # test_offer_recommended_by_vendor data
        cls.index.offers += [
            Offer(title='recommended', is_recommended=True, waremd5='EUhIXt-nprRmCEEWR-cysw'),
            Offer(title='nonrecommended', is_recommended=False, waremd5='VpUwTl5gv-d1vJpKS_S0zQ'),
        ]

        # for a promo code support
        cls.index.shops += [
            Shop(fesh=10001, regions=[187], cpa=Shop.CPA_REAL, subsidies=Shop.SUBSIDIES_ON),
        ]
        cls.index.offers += [Offer(title='мяучий оффер', fesh=10001, cpa=Offer.CPA_REAL)]
        cls.index.offers += [Offer(title='не мяучий оффер', fesh=10001, cpa=Offer.CPA_NO)]

        cls.index.offers += [Offer(title='котиковый оффер', price=100, price_old=200)]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213], name='Новые игрушки', cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [Offer(fesh=1, title='auction', price=100, price_old=200)]

        for i in range(10):
            cls.index.shops += [Shop(fesh=20000 + i, priority_region=213)]
            cls.index.offers += [Offer(title='offerincut', fesh=20000 + i)]

        # test_rgb_param
        cls.index.mskus += [
            MarketSku(sku=10000001, title="kiyanka blue sku", hyperid=100, blue_offers=[BlueOffer()]),
        ]

    def get_errorbooster_logbroker(self):
        errorbooster_logbroker = ErrorboosterLogFrontend()
        errorbooster_logbroker.bind(
            LogBrokerTopicBackend(self.logbroker, ERRORBOOSTER_TOPIC, LogBrokerClient.CODEC_RAW)
        )
        errorbooster_logbroker.dump('*')
        return errorbooster_logbroker

    def test_logbroker_got_log(self):  # timeout otherwise
        self.report.request_json('place=prime&text=мяучий&show-urls=external')


if __name__ == '__main__':
    main()
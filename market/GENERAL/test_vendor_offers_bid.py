#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip
from core.testcase import TestCase, main
from core.types import DynamicVendorOfferBid, Offer
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(title='armata', vbid=11, datasource_id=1),
            Offer(title='rucznica', vbid=18, datasource_id=2),
            Offer(title='mozdzierz', vbid=24, datasource_id=3),
        ]

    @skip('VBid disabled and should be removed')
    def test_vendor_offer_cutoff(self):
        """
        Проверяем, что выключение работает в динамике выключает ставки на оффера для вендоров из динамика
        """
        self.dynamic.disabled_vendor_offer_bids += [
            DynamicVendorOfferBid(1),
            DynamicVendorOfferBid(2),
        ]

        response = self.report.request_json(
            'place=prime&debug=da&rearr-factors=market_force_use_vendor_bid=1&text=rucznica&rearr-factors=disable_panther_quorum=0'
        )

        self.assertFragmentIn(response, {"debug": {"sale": {"vBid": 0}, "tech": {"vendorDatasourceId": 2}}})

        self.assertFragmentIn(response, {"debug": {"factors": {"VBID": Absent()}}})

    @skip('VBid disabled and should be removed')
    def test_vendor_offer_cutoff_does_not_affect_not_in_cutoff(self):
        """
        Проверяем, что выключение работает в динамике и НЕ выключает ставки для вендоров не попавших в динамик
        """
        self.dynamic.disabled_vendor_offer_bids += [
            DynamicVendorOfferBid(1),
            DynamicVendorOfferBid(2),
        ]

        response = self.report.request_json(
            'place=prime&debug=da&rearr-factors=market_force_use_vendor_bid=1&text=mozdzierz&rearr-factors=disable_panther_quorum=0'
        )

        self.assertFragmentIn(response, {"debug": {"sale": {"vBid": 24}, "tech": {"vendorDatasourceId": 3}}})

        self.assertFragmentIn(response, {"debug": {"factors": {"VBID": "24"}}})


if __name__ == '__main__':
    main()

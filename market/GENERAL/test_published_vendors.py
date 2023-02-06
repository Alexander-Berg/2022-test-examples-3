#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import CardVendor, Offer, RedirectWhiteListRecord, Vendor
from core.testcase import TestCase, main
from core.types.vendor import PublishedVendor


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.cards += [
            CardVendor(vendor_id=1),
            CardVendor(vendor_id=2),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=1, name='samsung'),
            Vendor(vendor_id=2, name='apple'),
        ]

        cls.index.offers += [Offer(title='samsung'), Offer(title='apple')]

        cls.index.published_vendors += [PublishedVendor(vendor_id=1)]

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='samsung', url='/brands.xml?brand=1'),
            RedirectWhiteListRecord(query='apple', url='/brands.xml?brand=2'),
        ]

    def test_vendor_redirect_on(self):
        response = self.report.request_json('place=prime&cvredirect=1&text=samsung&non-dummy-redirects=1')
        self.assertFragmentIn(response, {"params": {"brand": ["1"]}})

    def test_vendor_redirect_off(self):
        response = self.report.request_json('place=prime&cvredirect=1&text=apple&non-dummy-redirects=1')
        self.assertFragmentNotIn(response, {"params": {"brand": ["2"]}})


if __name__ == '__main__':
    main()

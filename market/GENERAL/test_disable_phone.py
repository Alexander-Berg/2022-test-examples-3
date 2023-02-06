#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import Offer, Shop
from core.testcase import TestCase, main

SHOP_DEFAULT = 1
SHOP_ENABLED_ALL = 2
SHOP_DISABLED_DESKTOP = 3
SHOP_DISABLED_ALL = 4


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers = [
            Offer(fesh=SHOP_DEFAULT),
            Offer(fesh=SHOP_ENABLED_ALL),
            Offer(fesh=SHOP_DISABLED_DESKTOP),
            Offer(fesh=SHOP_DISABLED_ALL),
        ]
        cls.index.shops = [
            Shop(fesh=SHOP_DEFAULT),
            Shop(fesh=SHOP_ENABLED_ALL, phone_display_options='*'),
            Shop(fesh=SHOP_DISABLED_DESKTOP, phone_display_options='-DESKTOP'),
            Shop(fesh=SHOP_DISABLED_ALL, phone_display_options='-'),
        ]

    def test_phone_desktop_default_shop(self):
        self._check_phone_in_output(fesh=SHOP_DEFAULT, is_touch=False)

    def test_phone_touch_default_shop(self):
        self._check_phone_in_output(fesh=SHOP_DEFAULT, is_touch=True)

    def test_phone_desktop_enabled_all_shop(self):
        self._check_phone_in_output(fesh=SHOP_ENABLED_ALL, is_touch=False)

    def test_phone_touch_enabled_all_shop(self):
        self._check_phone_in_output(fesh=SHOP_ENABLED_ALL, is_touch=True)

    def test_phone_desktop_disabled_desktop_shop(self):
        self._check_phone_not_in_output(fesh=SHOP_DISABLED_DESKTOP, is_touch=False)

    def test_phone_touch_disabled_desktop_shop(self):
        self._check_phone_in_output(fesh=SHOP_DISABLED_DESKTOP, is_touch=True)

    def test_phone_desktop_disabled_all_shop(self):
        self._check_phone_not_in_output(fesh=SHOP_DISABLED_ALL, is_touch=False)

    def test_phone_touch_disabled_all_shop(self):
        self._check_phone_not_in_output(fesh=SHOP_DISABLED_ALL, is_touch=True)

    def _check_phone_in_output(self, fesh, is_touch):
        response = self._request_shop(fesh, is_touch)

        self.assertFragmentIn(response, {"entity": "offer", "shop": {"id": fesh, "phones": {}}})

    def _check_phone_not_in_output(self, fesh, is_touch):
        response = self._request_shop(fesh, is_touch)

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": fesh,
                },
            },
        )
        self.assertFragmentNotIn(response, {"entity": "offer", "shop": {"phones": {}}})

    def _request_shop(self, fesh, is_touch):
        return self.report.request_json(
            'place=prime&fesh={fesh}&touch={is_touch}'.format(fesh=fesh, is_touch=int(is_touch))
        )


if __name__ == '__main__':
    main()

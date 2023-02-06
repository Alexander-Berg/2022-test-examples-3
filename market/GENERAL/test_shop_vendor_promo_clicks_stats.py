#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, HyperCategoryType, StatItem, StatType
from core.matcher import NotEmpty


class T(TestCase):

    # https://st.yandex-team.ru/MARKETOUT-21762
    @classmethod
    def prepare_shop_vendor_promo_clicks_stats(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1, name="hid-1", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2, name="hid-2", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=3, name="hid-3", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=4, name="hid-4", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=5, name="hid-5", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=6, name="hid-6", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=8, name="hid-8", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=10, name="hid-10", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=12, name="hid-12", output_type=HyperCategoryType.GURU),
        ]

        cls.index.shop_vendor_promo_clicks_stats += [
            StatItem(StatType.SHOP_ALLPROMO, 1, [1, 2]),
            StatItem(StatType.SHOP_ALLPROMO, 2, [2, 4]),
            StatItem(StatType.SHOP_PROMOCODE, 3, [3, 6]),
            StatItem(StatType.VENDOR_ALLPROMO, 4, [4, 8]),
            StatItem(StatType.VENDOR_PROMOCODE, 5, [5, 10]),
            StatItem(StatType.VENDOR_PROMOCODE, 6, [6, 12]),
        ]

    def test_shop_allpromo_multishop(self):
        response = self.report.request_json(
            'place=shop_vendor_promo_clicks_stats&fesh=1,2&promoclicks-stats-type=allpromo'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shopCategories",
                        "shopId": "1",
                        "promoType": "allpromo",
                        "categories": [
                            {"entity": "category", "id": 1, "name": "hid-1"},
                            {"entity": "category", "id": 2, "name": "hid-2"},
                        ],
                    },
                    {
                        "entity": "shopCategories",
                        "shopId": "2",
                        "promoType": "allpromo",
                        "categories": [
                            {"entity": "category", "id": 2, "name": "hid-2"},
                            {"entity": "category", "id": 4, "name": "hid-4"},
                        ],
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_shop_promocode(self):
        response = self.report.request_json(
            'place=shop_vendor_promo_clicks_stats&fesh=3&promoclicks-stats-type=promocode'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shopCategories",
                        "shopId": "3",
                        "promoType": "promocode",
                        "categories": [
                            {"entity": "category", "id": 3, "name": "hid-3"},
                            {"entity": "category", "id": 6, "name": "hid-6"},
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_vendor_allpromo(self):
        response = self.report.request_json(
            'place=shop_vendor_promo_clicks_stats&vendor_id=4&promoclicks-stats-type=allpromo'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "vendorCategories",
                        "vendorId": "4",
                        "promoType": "allpromo",
                        "categories": [
                            {"entity": "category", "id": 4, "name": "hid-4"},
                            {"entity": "category", "id": 8, "name": "hid-8"},
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_vendor_promocode_multivendor(self):
        response = self.report.request_json(
            'place=shop_vendor_promo_clicks_stats&vendor_id=5,6&promoclicks-stats-type=promocode'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "vendorCategories",
                        "vendorId": "5",
                        "promoType": "promocode",
                        "categories": [
                            {"entity": "category", "id": 5, "name": "hid-5"},
                            {"entity": "category", "id": 10, "name": "hid-10"},
                        ],
                    },
                    {
                        "entity": "vendorCategories",
                        "vendorId": "6",
                        "promoType": "promocode",
                        "categories": [
                            {"entity": "category", "id": 6, "name": "hid-6"},
                            {"entity": "category", "id": 12, "name": "hid-12"},
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_unknown_shop_correct(self):
        for type in ["allpromo", "promocode"]:
            response = self.report.request_json(
                'place=shop_vendor_promo_clicks_stats&fesh=999&promoclicks-stats-type={}'.format(type)
            )
            self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    def test_unknown_vendor_correct(self):
        for type in ["allpromo", "promocode"]:
            response = self.report.request_json(
                'place=shop_vendor_promo_clicks_stats&vendor_id=999&promoclicks-stats-type={}'.format(type)
            )
            self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    def test_errors(self):
        for part in [
            "fesh=333",
            "vendor_id=444",
            "promo-clicks-stats-type=allpromo",
            "promo-clicks-stats-type=promocode",
            "",
        ]:
            response = self.report.request_json('place=shop_vendor_promo_clicks_stats&')
            self.assertFragmentIn(response, {"error": NotEmpty()})
            self.error_log.expect(code=3043)


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import NoKey
from core.testcase import TestCase, main
from core.types import (
    CpaCategory,
    CpaCategoryType,
    Currency,
    DeliveryBucket,
    ExchangeRate,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)

CATEGORY_A = 503
CATEGORY_B = 504

# Аутлеты на "карте"
# Числа в скобках - координаты тайлов при zoom = 10
#           37.0(617)     37.2(617)       37.4(618)      37.6(618)       37.8(619)
# 55.8(321) |--------------|---------------|--------------|---------------
#           |              |               |              |
#           |              |               |              |
#           |              |               |              |
#           |              |               |              |
# 55.6(322) |--------------|---------------|--------------|---------------
#           |              |               |  III   (215, |
#           |              |               |         216, |
#           |              |               |      *  315, |
#           |              |               |         316) |
# 55.4(323) |--------------|---------------|--------------|---------------
#           |              | II            |              |
#           |              |      *(233,   |              |
#           |              |        234,   |              |
#           |              |    333, 334)  |              |
# 55.2(324) |--------------|---------------|--------------|---------------
#           | I    *We     |               |              |
#           |     *(231,   |               |              |
#           |       232,   |               |              |
#           |    331, 332) |               |              |
# 55.0(325) |--------------|---------------|--------------|---------------

# В секторе I (rids=213), категория 503 CPC_AND_CPA, категория 504 CPA_WITH_CPC_PESSIMIZATIONCPA_WITH_CPC_PESSIMIZATION
# В секторе II (rids=75), категория 503 CPC_AND_CPA, категория 504 CPA_WITH_CPC_PESSIMIZATIONCPA_WITH_CPC_PESSIMIZATION
# В секторе III (rids=2), категория 503 CPC_AND_CPA, категория 504 CPA_WITH_CPC_PESSIMIZATIONCPA_WITH_CPC_PESSIMIZATION

# Также есть категория 101, которая везде CPC_AND_CPA


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург', tz_offset=10800),
            Region(rid=75, name='Владивосток', tz_offset=36000),
        ]

        cls.index.shops += [
            Shop(fesh=101, name='Shop1', cpa=Shop.CPA_REAL),
            Shop(fesh=102, name='Shop2', cpa=Shop.CPA_REAL),
            Shop(fesh=103, name='Shop2', cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(fesh=101, title='good CPA', randx=1000, hid=CATEGORY_A, cpa=Offer.CPA_REAL, pickup_buckets=[5001]),
            Offer(fesh=102, title='good CPC', randx=1000, hid=CATEGORY_A, cpa=Offer.CPA_NO, pickup_buckets=[5002]),
            Offer(
                fesh=103,
                title='good CPA experiment',
                randx=1000,
                hid=CATEGORY_B,
                cpa=Offer.CPA_REAL,
                pickup_buckets=[5003],
            ),
            Offer(
                fesh=104,
                title='good CPC experiment',
                randx=1000,
                hid=CATEGORY_B,
                cpa=Offer.CPA_NO,
                pickup_buckets=[5004],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=101),
            HyperCategory(hid=CATEGORY_A, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=CATEGORY_B, output_type=HyperCategoryType.GURU),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=CATEGORY_A, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=CATEGORY_A, regions=[75], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=CATEGORY_B, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=CATEGORY_B, regions=[75], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.outlets += [
            Outlet(point_id=231, fesh=101, region=213, gps_coord=GpsCoord(37.12, 55.12)),
            Outlet(point_id=232, fesh=102, region=213, gps_coord=GpsCoord(37.12, 55.12)),
            Outlet(point_id=331, fesh=103, region=213, gps_coord=GpsCoord(37.12, 55.12)),
            Outlet(point_id=332, fesh=104, region=213, gps_coord=GpsCoord(37.12, 55.12)),
            Outlet(point_id=233, fesh=101, region=75, gps_coord=GpsCoord(37.32, 55.32)),
            Outlet(point_id=234, fesh=102, region=75, gps_coord=GpsCoord(37.32, 55.32)),
            Outlet(point_id=333, fesh=103, region=75, gps_coord=GpsCoord(37.32, 55.32)),
            Outlet(point_id=334, fesh=104, region=75, gps_coord=GpsCoord(37.32, 55.32)),
            Outlet(point_id=215, fesh=101, region=2, gps_coord=GpsCoord(37.5, 55.5)),
            Outlet(point_id=216, fesh=102, region=2, gps_coord=GpsCoord(37.5, 55.5)),
            Outlet(point_id=315, fesh=103, region=2, gps_coord=GpsCoord(37.5, 55.5)),
            Outlet(point_id=316, fesh=104, region=2, gps_coord=GpsCoord(37.5, 55.5)),
        ]

        cls.index.currencies = [
            Currency(
                'KZT',
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=0.2),
                ],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=101,
                carriers=[99],
                options=[PickupOption(outlet_id=231), PickupOption(outlet_id=233), PickupOption(outlet_id=215)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=102,
                carriers=[99],
                options=[PickupOption(outlet_id=232), PickupOption(outlet_id=234), PickupOption(outlet_id=216)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=103,
                carriers=[99],
                options=[PickupOption(outlet_id=331), PickupOption(outlet_id=333), PickupOption(outlet_id=315)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=104,
                carriers=[99],
                options=[PickupOption(outlet_id=332), PickupOption(outlet_id=334), PickupOption(outlet_id=316)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    # Test default behaivor (no rearr-factors)

    def test_geo_bounding_cpahybrid(self, aux_params=''):
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.2,55.2&geo_bounds_rt=37.4,55.4&text=good&rids=0&debug=da&show-geo-cpa=da&zoom=10&numdoc=20'
            + aux_params
        )
        # Сектор II
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "233",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "333",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "334",
                    },
                },
            ],
        )

    def test_geo_bounding_cpa_and_cpc(self, aux_params=''):
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.4,55.5&geo_bounds_rt=37.6,55.6&text=good&rids=0&debug=da&show-geo-cpa=da&zoom=10&numdoc=20'
            + aux_params
        )
        # Сектор III
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "215",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC", "cpa": NoKey("cpa")},
                    "outlet": {
                        "id": "216",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "315",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "316",
                    },
                },
            ],
        )

    def test_geo_bounding_mixed(self, aux_params=''):
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6&text=good&rids=0&debug=da&show-geo-cpa=da&zoom=10&numdoc=50'
            + aux_params
        )
        # Все сектора вместе
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "231",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "233",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "215",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "216",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "331",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "332",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "333",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "334",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "315",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "316",
                    },
                },
            ],
        )

    # Test 'cpa-and-cpc' experiment

    def test_geo_bounding_cpaonly__cpa_and_cpc(self, aux_params=''):
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.2,55.2&text=good&rids=0&debug=da&show-geo-cpa=da'
            + aux_params
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "231",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "232",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "331",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "332",
                    },
                },
            ],
        )

    def test_geo_bounding_cpahybrid__cpa_and_cpc(self, aux_params=''):
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.2,55.2&geo_bounds_rt=37.4,55.4&text=good&rids=0&debug=da&show-geo-cpa=da&zoom=10&numdoc=20'
            + aux_params
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "233",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "234",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "333",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "334",
                    },
                },
            ],
        )

    def test_geo_bounding_mixed__cpa_and_cpc(self, aux_params=''):
        response = self.report.request_json(
            'place=geo&geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.6,55.6&text=good&rids=0&debug=da&show-geo-cpa=da&zoom=10&numdoc=50'
            + aux_params
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "231",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "232",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "233",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "234",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA"},
                    "cpa": "real",
                    "outlet": {
                        "id": "215",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "216",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "331",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "332",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "333",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "334",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPA experiment"},
                    "cpa": "real",
                    "outlet": {
                        "id": "315",
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "good CPC experiment"},
                    "cpa": NoKey("cpa"),
                    "outlet": {
                        "id": "316",
                    },
                },
            ],
        )

    # All the same test with with how=distance

    def test_geo_bounding_cpahybrid__distance(self):
        return self.test_geo_bounding_cpahybrid('&how=distance')

    def test_geo_bounding_cpa_and_cpc___distance(self):
        return self.test_geo_bounding_cpa_and_cpc('&how=distance')

    def test_geo_bounding_mixed__distance(self):
        return self.test_geo_bounding_mixed('&how=distance')

    def test_geo_bounding_cpahybrid__cpa_and_cpc__distance(self):
        return self.test_geo_bounding_cpahybrid__cpa_and_cpc('&how=distance')

    def test_geo_bounding_mixed__cpa_and_cpc__distance(self):
        return self.test_geo_bounding_mixed__cpa_and_cpc('&how=distance')

    @classmethod
    def prepare_cpa_on_touch(cls):
        cls.index.offers += [
            Offer(hyperid=1501, hid=CATEGORY_B, cpa=Offer.CPA_REAL, title='CPA', fesh=101, pickup_buckets=[5001]),
            Offer(hyperid=1501, hid=CATEGORY_B, cpa=Offer.CPA_NO, title='CPC', fesh=102, pickup_buckets=[5002]),
            Offer(hyperid=1502, hid=CATEGORY_B, cpa=Offer.CPA_NO, title='CPC', fesh=103, pickup_buckets=[5003]),
        ]

    def test_cpa_on_touch_cpc(self):
        response = self.report.request_json(
            'place=geo&touch=1&rids=213&how=distance&hid=504&hyperid=1502&numdoc=5&pp=46'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'CPC'},
            },
        )

    def test_cpa_on_touch_cpc_cpa_real(self):
        response = self.report.request_json(
            'place=geo&touch=1&rids=213&how=distance&hid=504&hyperid=1502&numdoc=5&pp=46&cpa=real'
        )
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
            },
        )

    def test_cpa_on_touch_cpc_cpa_any(self):
        response = self.report.request_json(
            'place=geo&touch=1&rids=213&how=distance&hid=504&hyperid=1502&numdoc=5&pp=46&cpa=any'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'CPC'},
            },
        )


if __name__ == '__main__':
    main()

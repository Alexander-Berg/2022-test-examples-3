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
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    HyperCategory,
    MarketSku,
    NavCategory,
    OfferDimensions,
    Outlet,
    PickupBucket,
    PickupOption,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.types.hypercategory import TOBACCO_CATEG_ID

_TOBACCO_HIDS = (16440108, 16440141, 16440100)
_TOBACCO_NIDS_BLUE = (16440110, 16440143, 16440102)
_NON_TOBACCO_CATEGORY = 101
_TEST_OFFER_CATEGORIES = _TOBACCO_HIDS + (_NON_TOBACCO_CATEGORY,)


class _Outlets(object):
    pickup_outlet = Outlet(point_id=1001, fesh=1, region=213, point_type=Outlet.FOR_PICKUP, working_days=list(range(5)))
    post_term_outlet = Outlet(
        point_id=1002, fesh=1, region=213, point_type=Outlet.FOR_POST_TERM, working_days=list(range(5))
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.lms_autogenerate = False
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[1001, 1002],
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=TOBACCO_CATEG_ID,
                children=[HyperCategory(hid=hid) for hid in _TOBACCO_HIDS if hid != TOBACCO_CATEG_ID],
            ),
            HyperCategory(hid=_NON_TOBACCO_CATEGORY),
        ]
        cls.index.navtree += [NavCategory(nid=_NON_TOBACCO_CATEGORY, hid=_NON_TOBACCO_CATEGORY, name="not tobacco")]
        cls.index.navtree += [
            NavCategory(nid=nid, hid=nid, name="Category {}".format(nid))
            for nid, hid in zip(_TOBACCO_NIDS_BLUE, _TOBACCO_HIDS)
        ]

        cls.index.outlets += (_Outlets.pickup_outlet, _Outlets.post_term_outlet)
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=91,
                dc_bucket_id=1,
                fesh=1,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=1001, day_from=3, day_to=5, price=20),
                    PickupOption(outlet_id=1002, day_from=1, day_to=2, price=1),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=i,
                sku=i,
                hid=hid,
                post_term_delivery=True,
                blue_offers=[
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_18,
                        offerid='Cigarettes_{}'.format(hid),
                        feedid=2,
                        post_term_delivery=True,
                        waremd5='{}_____________g'.format(hid),
                        pickup_buckets=[91],
                        weight=1,
                        dimensions=OfferDimensions(length=10, width=5, height=20),
                    )
                ],
            )
            for i, hid in enumerate(_TOBACCO_HIDS)
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=_NON_TOBACCO_CATEGORY,
                sku=_NON_TOBACCO_CATEGORY,
                hid=_NON_TOBACCO_CATEGORY,
                blue_offers=[
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_18,
                        offerid='RegularSKU',
                        feedid=2,
                        post_term_delivery=True,
                        waremd5='Nicotineless_________g',
                        pickup_buckets=[91],
                        weight=1,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                    )
                ],
            )
        ]

        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[]),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicDeliveryServiceInfo(
                99,
                "self-delivery",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=99,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=22, region_to=225)],
            ),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=2, width=11, height=16, length=22).respond([], [1], [])

    def test_cigarettes_not_available_in_post_term(self):
        """Сигареты не должны доставляться в постаматы, т.к. там нельзя проверить возраст"""
        all_outlets = [_Outlets.pickup_outlet.point_id, _Outlets.post_term_outlet.point_id]
        outlets_no_post_term = [_Outlets.pickup_outlet.point_id]

        def check_response(color, offers, tobacco_offers=None):
            if tobacco_offers is None:
                tobacco_offers = []
            offers_list = ','.join((o + ':1' for o in offers))
            request = (
                'place=actual_delivery&offers-list={}&rids=213{}'.format(offers_list, color)
                + '&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1'
                + '&pickup-options=grouped&pickup-options-extended-grouping=1&adult=1'
                + '&combinator=0'
            )
            self.assertFragmentIn(
                self.report.request_json(request),
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "pickupOptions": [
                            {"outletIds": outlets_no_post_term if len(tobacco_offers) > 0 else all_outlets}
                        ]
                    },
                },
                allow_different_len=False,
            )

        for color in ('&rgb=blue', ''):
            regular_offer_id = 'Nicotineless_________g'
            check_response(color, [regular_offer_id])

            for hid in _TOBACCO_HIDS:
                cigarettes_offerid = '{}_____________g'.format(hid)
                check_response(color, [cigarettes_offerid], tobacco_offers=[0])

                mixed_basket_offers = (cigarettes_offerid, regular_offer_id)
                check_response(color, mixed_basket_offers, tobacco_offers=[0])


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent
from core.testcase import TestCase, main
from core.types import (
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicOutletDate,
    DynamicOutletInfo,
    DynamicOutletTime,
    DynamicOutletWorkingTime,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.types.delivery import OutletWorkingTime
from core.types.offer import OfferDimensions
from core.types.taxes import Tax
from core.types.sku import MarketSku, BlueOffer


class UseFastDataOutlets(object):
    UFDO_USE_INDEX = 0
    UFDO_USE_DYNAMIC_WITH_FALLBACK = 1
    UFDO_USE_DYNAMIC_ONLY = 2


RUS_RIDS = 225
MSK_RIDS = 213
VLAD_RIDS = 75

WAREHOUSE_ID = 145
DELIVERY_SERVICE_ID = 100
PICKUP_SHIPPER_ID = DELIVERY_SERVICE_ID
POST_SHIPPER_ID = 101

ACTUAL_DELIVERY_REQUEST = (
    'place=actual_delivery&'
    'rgb={color}&'
    'rids={rids}&'
    'pickup-options=grouped&'
    'pickup-options-extended-grouping=1&'
    'regset=2&'
    'offers-list={ware_md5}:1&'
    'combinator=0&'
    'rearr-factors=market_blue_use_dynamic_for_calendar={use_dynamic}&'
    'rearr-factors=market_use_lms_outlets={use_lms}&'
)


def __create_blue_offer(title, sku, shop_sku, ware_md5):
    blue_offer = BlueOffer(
        supplier_id=2,
        feedid=200,
        offerid=shop_sku,
        waremd5=ware_md5,
        price=5,
        weight=75,
        dimensions=OfferDimensions(length=80, width=60, height=200),
    )
    msku = MarketSku(
        title=title, hyperid=1, sku=sku, blue_offers=[blue_offer], pickup_buckets=[5000], post_buckets=[6000]
    )
    return msku, blue_offer


ALYONKA_MSKU, ALYONKA_OFFER = __create_blue_offer(
    title="Аленка",
    sku=1,
    shop_sku='Alyonka_145',
    ware_md5='Alyonka_145__________g',
)


class T(TestCase):
    @staticmethod
    def __create_outlet(id, alias_id, type, ds_id, shipper_id, day_from, day_to, working_days, rids=None):
        return Outlet(
            point_id=id,
            mbi_alias_point_id=alias_id,
            delivery_service_id=ds_id,
            region=rids if rids else MSK_RIDS,
            point_type=type,
            delivery_option=OutletDeliveryOption(
                shipper_id=shipper_id, day_from=day_from, day_to=day_to, order_before=2, work_in_holiday=True, price=150
            ),
            working_days=working_days,
            working_times=[
                OutletWorkingTime(
                    days_from=OutletWorkingTime.MONDAY,
                    days_till=OutletWorkingTime.SUNDAY,
                    hours_from='09:00',
                    hours_till='20:00',
                )
            ],
        )

    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=MSK_RIDS, name="Москва"),
            Region(rid=VLAD_RIDS, name="Владивосток", tz_offset=36000),
        ]

        cls.settings.loyalty_enabled = True

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=MSK_RIDS,
                name='Beru',
                fulfillment_virtual=True,
                delivery_service_outlets=[2001, 2002, 2003, 2004, 1997, 10000000005],
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=2,
                datafeed_id=10,
                priority_region=2,
                name='blue_supplier_145',
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
            ),
        ]

    @classmethod
    def prepare_outlets(cls):
        # pickup outlets
        outlet_info = [
            # (id, alias_id, day_from, day_to, rids)
            (1997, None, 1, 2, MSK_RIDS),
            (2001, None, 2, 3, MSK_RIDS),
            (2002, None, 3, 4, MSK_RIDS),
            (2003, None, 1, 1, VLAD_RIDS),
            (10000000005, 1997, 1, 2, MSK_RIDS),
        ]
        cls.index.outlets += [
            T.__create_outlet(
                id=id,
                alias_id=alias_id,
                type=Outlet.FOR_PICKUP,
                ds_id=DELIVERY_SERVICE_ID,
                shipper_id=PICKUP_SHIPPER_ID,
                day_from=day_from,
                day_to=day_to,
                working_days=[i for i in range(20)],
                rids=rids,
            )
            for id, alias_id, day_from, day_to, rids in outlet_info
        ]

        # post outlets
        outlet_info = [
            # (id, alias_id, day_from, day_to)
            (2004, None, 3, 4),
            (2005, None, 3, 4),
            (10000000002, 2005, 3, 4),
        ]
        cls.index.outlets += [
            T.__create_outlet(
                id=id,
                alias_id=alias_id,
                type=Outlet.FOR_POST,
                ds_id=DELIVERY_SERVICE_ID,
                shipper_id=POST_SHIPPER_ID,
                day_from=day_from,
                day_to=day_to,
                working_days=[i for i in range(20)],
            )
            for id, alias_id, day_from, day_to in outlet_info
        ]

    @classmethod
    def prepare_buckets(cls):
        # pickup buckets
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5000,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                options=[
                    PickupOption(outlet_id=2001, day_from=1, day_to=4, price=201),
                    PickupOption(outlet_id=2002, day_from=1, day_to=4, price=202),
                    PickupOption(outlet_id=2003, day_from=1, day_to=1, price=203),
                    PickupOption(outlet_id=10000000005, day_from=1, day_to=1, price=204),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]
        # post buckets
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=6000,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                options=[
                    PickupOption(outlet_id=2004, day_from=2, day_to=3, price=203),
                    PickupOption(outlet_id=10000000002, day_from=2, day_to=3, price=205),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [ALYONKA_MSKU]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False

        def delivery_service_region_to_region_info(region_from=MSK_RIDS, region_to=RUS_RIDS):
            return DeliveryServiceRegionToRegionInfo(region_from=region_from, region_to=region_to, days_key=1)

        def link_warehouse_to_delivery_service(warehouse_id, delivery_service_id, region_to=RUS_RIDS):
            return DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=warehouse_id,
                delivery_service_id=delivery_service_id,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=region_to)],
            )

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RIDS, holidays_days_set_key=1),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WAREHOUSE_ID, warehouse_to=WAREHOUSE_ID),
            DynamicDeliveryServiceInfo(
                id=PICKUP_SHIPPER_ID,
                name='carrier_100',
                region_to_region_info=[delivery_service_region_to_region_info()],
            ),
            DynamicDeliveryServiceInfo(
                id=POST_SHIPPER_ID, name='carrier_101', region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            link_warehouse_to_delivery_service(warehouse_id=WAREHOUSE_ID, delivery_service_id=PICKUP_SHIPPER_ID),
            link_warehouse_to_delivery_service(warehouse_id=WAREHOUSE_ID, delivery_service_id=POST_SHIPPER_ID),
            DynamicDaysSet(key=1, days=[]),
        ]

    @classmethod
    def prepare_fast_data_outlets(cls):
        working_times = [
            DynamicOutletWorkingTime(
                week_day=day, time_from=DynamicOutletTime(hour=9, min=0), time_to=DynamicOutletTime(hour=20, min=0)
            )
            for day in [
                OutletWorkingTime.SUNDAY,
                OutletWorkingTime.MONDAY,
                OutletWorkingTime.TUESDAY,
                OutletWorkingTime.WEDNESDAY,
                OutletWorkingTime.THURSDAY,
                OutletWorkingTime.FRIDAY,
                OutletWorkingTime.SATURDAY,
            ]
        ]

        calendar_holidays = [
            DynamicOutletDate(year=1985, month=6, day=25),
            DynamicOutletDate(year=1985, month=6, day=26),
        ]
        alternative_calendar_holidays = [
            DynamicOutletDate(year=1985, month=6, day=26),
        ]
        start_day = DynamicOutletDate(year=1985, month=6, day=24)
        end_day = DynamicOutletDate(year=1985, month=9, day=24)

        outlet_info = [
            # (id, alias_id, is_active, calendar_holidays)
            (2001, None, True, calendar_holidays),
            (2002, None, True, calendar_holidays),
            (2003, None, True, calendar_holidays),
            (2004, None, True, calendar_holidays),
            (10000000002, 2005, True, calendar_holidays),
            (10000000005, 1997, True, alternative_calendar_holidays),
        ]
        cls.dynamic.fast_data_outlets += [
            DynamicOutletInfo(
                id=fid,
                is_active=is_active,
                last_update_time='1985-05-01 05:07:33.419708',
                working_time=working_times,
                calendar_holidays=calend_holidays,
                start_day=start_day,
                end_day=end_day,
                mbi_id=alias_id,
            )
            for fid, alias_id, is_active, calend_holidays in outlet_info
        ]

    class DeliveryOptions(object):
        def __init__(self, outlet_ids, day_from, day_to):
            self.outlet_ids = outlet_ids
            self.day_from = day_from
            self.day_to = day_to

        def to_json(self):
            return {
                'dayFrom': self.day_from,
                'dayTo': self.day_to,
                'groupCount': len(self.outlet_ids),
                'outletIds': self.outlet_ids,
            }

    def check_actual_delivery_request(
        self,
        use_dynamic=UseFastDataOutlets.UFDO_USE_INDEX,
        use_lms=0,
        pickup_options=[],
        post_options=[],
        rids=MSK_RIDS,
    ):
        delivery_fragment = dict()
        disable_post_as_pickup_rearr = 'rearr-factors=market_use_post_as_pickup=0&'
        for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
            pickup_opts = [opt.to_json() for opt in pickup_options]
            post_opts = [opt.to_json() for opt in post_options]
            if pickup_options:
                delivery_fragment['pickupOptions'] = pickup_opts if disable_post_as_pickup else pickup_opts + post_opts
            if post_options:
                delivery_fragment['postOptions'] = post_opts if disable_post_as_pickup else Absent()

            response = self.report.request_json(
                ACTUAL_DELIVERY_REQUEST.format(
                    ware_md5=ALYONKA_OFFER.waremd5, color='blue', rids=rids, use_dynamic=use_dynamic, use_lms=use_lms
                )
                + disable_post_as_pickup
            )
            self.assertFragmentIn(response, {'entity': 'deliveryGroup', 'delivery': delivery_fragment})

    def test_place_actual_delivery(self):
        """
        Проверяем в place=actual_delivery:
            - зависимость расчетной даты доставки от каледаря выходных дней в динамике
            - нумерацию ПВЗ на выдаче
        """

        for use_lms_outlets in [0, 1]:
            for use_fast_data_outlets in [
                UseFastDataOutlets.UFDO_USE_DYNAMIC_WITH_FALLBACK,
                UseFastDataOutlets.UFDO_USE_DYNAMIC_ONLY,
            ]:
                self.check_actual_delivery_request(
                    use_dynamic=use_fast_data_outlets,
                    use_lms=use_lms_outlets,
                    pickup_options=[
                        T.DeliveryOptions(outlet_ids=[2001, 2002], day_from=3, day_to=4),
                        T.DeliveryOptions(
                            outlet_ids=[10000000005] if use_lms_outlets else [1997], day_from=1, day_to=1
                        ),
                    ],
                    post_options=[
                        T.DeliveryOptions(
                            outlet_ids=[2004, 10000000002] if use_lms_outlets else [2004, 2005], day_from=3, day_to=3
                        )
                    ],
                )

    def test_place_actual_delivery_tz_aware(self):
        """
        Проверяем в place=actual_delivery, что расчет даты доставки в ПВЗ
        происходит с учетом временной зоны самого ПВЗ и календаря его выходных
        дней из динамика
        """
        for use_lms_outlets in [0, 1]:
            for use_fast_data_outlets in [
                UseFastDataOutlets.UFDO_USE_DYNAMIC_WITH_FALLBACK,
                UseFastDataOutlets.UFDO_USE_DYNAMIC_ONLY,
            ]:
                self.check_actual_delivery_request(
                    use_dynamic=use_fast_data_outlets,
                    use_lms=use_lms_outlets,
                    pickup_options=[T.DeliveryOptions(outlet_ids=[2003], day_from=3, day_to=3)],
                    rids=VLAD_RIDS,
                )


if __name__ == '__main__':
    main()

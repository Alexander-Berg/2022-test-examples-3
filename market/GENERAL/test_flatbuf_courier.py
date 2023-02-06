#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BucketInfo, DeliveryBucket, DeliveryOption, Offer, OfferDeliveryInfo, Region, RegionalDelivery
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                tz_offset=10800,
                children=[
                    Region(
                        rid=213,
                        name='Москва',
                        tz_offset=10800,
                        children=[
                            Region(rid=216, name='Зеленоград', tz_offset=10800),
                            Region(
                                rid=114619,
                                name='Новомосковский административный округ',
                                region_type=Region.FEDERAL_DISTRICT,
                                children=[
                                    Region(rid=10720, name='Внуково', region_type=Region.VILLAGE),
                                    Region(rid=21624, name='Щербинка', region_type=Region.CITY),
                                ],
                            ),
                        ],
                    ),
                    Region(rid=10758, name='Химки', tz_offset=10800),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=2,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=4)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer_1',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1)]),
                delivery_buckets=[2],
            )
        ]

    def test_not_daas_buckets_in_flatbuf(self):
        """Тестируем привязки не DAAS бакетов через флатбуфер"""

        response = self.report.request_json('place=prime&text=offer_1&rids=213&exact-match=1')
        self.assertFragmentIn(response, {'entity': 'offer', 'delivery': {'options': [{'dayFrom': 1, 'dayTo': 3}]}})


if __name__ == '__main__':
    main()

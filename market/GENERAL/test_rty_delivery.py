#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DeliveryBucket, DeliveryOption, Offer, RegionalDelivery, RtyOffer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.settings.rty_delivery = True
        cls.settings.index_sort = 'feed-offer'
        cls.settings.rty_merger_policy = 'NONE'
        cls.disable_check_empty_output()
        cls.index.offers += [
            Offer(title='iphone', fesh=21, feedid=25, offerid='fff', price=300),
            Offer(title='iphone', fesh=21, feedid=25, offerid='ggg', price=300),
        ]
        delivery_225 = RegionalDelivery(rid=225, options=[DeliveryOption(price=100)])
        delivery_225_2 = RegionalDelivery(rid=225, options=[DeliveryOption(price=200)])
        delivery_13 = RegionalDelivery(rid=13, options=[DeliveryOption(price=300)])
        delivery_5 = RegionalDelivery(rid=5, options=[DeliveryOption(price=100), DeliveryOption(price=500)])
        cls.index.delivery_buckets += [
            DeliveryBucket(dc_bucket_id=1, regional_options=[delivery_225, delivery_5]),
            DeliveryBucket(dc_bucket_id=2, regional_options=[delivery_225_2]),
            DeliveryBucket(dc_bucket_id=3, regional_options=[delivery_225, delivery_13]),
        ]

    def _check(self, offerid, bucketids):
        response = self.report.request_json(
            'place=print_doc&feed_shoffer_id=25-{}&rearr-factors=rty_qpipe=1;ext_debug=1&debug=1'.format(offerid)
        )
        self.assertFragmentIn(response, {'MARKET_DELIVERY_MMAP': {'currier_bucket_ids': bucketids}})

    def _checkall(self):
        self._check('fff', [1, 2])
        self._check('ggg', [3])

    def test_merge(self):
        """
        Проверяем, что два сегмента в rtyserver смерджились нормально
        """
        self.rty.offers += [
            RtyOffer(
                feedid=25,
                offerid='fff',
                price=400,
                delivery_buckets=[DeliveryBucket(dc_bucket_id=1), DeliveryBucket(dc_bucket_id=2)],
            )
        ]
        self.rty_controller.reopen_indexes()
        self.rty.offers += [
            RtyOffer(feedid=25, offerid='ggg', price=400, delivery_buckets=[DeliveryBucket(dc_bucket_id=3)])
        ]

        self.rty_controller.reopen_indexes()
        self._checkall()

        info = self.rty_controller.info_server()
        # before merge must by 4 indexes (2 final, 1 memory and 1 disk)
        self.assertEqual(len(info.get('indexes')), 4)
        self.breakpoint('before merge')
        self.rty_controller.do_merge()

        info = self.rty_controller.info_server()
        # after merge must by 3 indexes (1 final, 1 memory and 1 disk)
        self.assertEqual(len(info.get('indexes')), 3)
        self.breakpoint('after merge')

        self._checkall()


if __name__ == '__main__':
    main()

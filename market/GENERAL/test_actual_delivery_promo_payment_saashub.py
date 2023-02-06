#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import main
import test_actual_delivery_promo_payment


class T(test_actual_delivery_promo_payment.T):
    @classmethod
    def beforePrepare(cls):
        cls.settings.use_saashub_delivery = True

    @classmethod
    def prepare(cls):
        super(T, cls).prepare()
        cls.index.delivery_buckets_saashub += cls.index.delivery_buckets
        cls.index.pickup_buckets_saashub += cls.index.pickup_buckets
        cls.index.new_pickup_buckets_saashub += cls.index.new_pickup_buckets


if __name__ == '__main__':
    main()

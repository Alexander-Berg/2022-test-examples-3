#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import main
import test_express_delivery


class T(test_express_delivery.T):
    @classmethod
    def beforePrepare(cls):
        cls.settings.default_search_experiment_flags += ['market_use_business_offer=2']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare(cls):
        super(T, cls).prepare()
        cls.index.delivery_buckets_saashub += cls.index.delivery_buckets
        cls.index.pickup_buckets_saashub += cls.index.pickup_buckets
        cls.index.new_pickup_buckets_saashub += cls.index.new_pickup_buckets


if __name__ == '__main__':
    main()

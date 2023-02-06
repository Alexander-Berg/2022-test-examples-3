#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import main
from core.types import ExpressSupplier, TimeIntervalByDay
import test_combine
from test_combine import _MSKUs, _Offers, _Shops
from core.matcher import ElementCount
from core.types import DynamicWarehouseInfo

"""
Данный набор тестов исходно был сделан для проверки запрета подмен экспресс-оферов.
Однако в дальнейшем запрет подмен был ограничен только экспресс-оферами из GO приложения.
Поэтому логика с признака экспресс переведена на no_replace флаг и проверяется здесь.
Оферы в тестах остались экспрессными. (Кажется, это никому не повредит.)
"""


class T(test_combine.T):
    @classmethod
    def prepare(cls):
        super(T, cls).prepare()

        cls.settings.default_search_experiment_flags += [
            'today_work_schedule_for_shop_from_lms=0',
            'is_express_from_lms=0',
            'enable_fast_promo_matcher=0',
            'enable_fast_promo_matcher_test=0',
            'enable_cart_split_on_combinator=0',
            'market_use_global_warehouse_priorities_filtering=0',
        ]
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        for msku in cls.index.mskus:
            for blue_offer in msku.get_blue_offers():
                if not blue_offer.is_fulfillment:
                    blue_offer.is_express = True

        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=shop.datafeed_id,
                supplier_id=shop.fesh,
                warehouse_id=shop.warehouse_id,
                working_schedule=[
                    TimeIntervalByDay(
                        day=i,
                        time_from='00:00',
                        time_to='24:00',
                    )
                    for i in range(0, 7)
                ],
            )
            for shop in cls.index.shops
            if shop.fulfillment_program is False
        ]

    @classmethod
    def prepare_add_express_warehouse(cls):
        cls.dynamic.lms += [DynamicWarehouseInfo(id=555, home_region=213, is_express=True)]
        cls.dynamic.lms += [DynamicWarehouseInfo(id=444, home_region=213, is_express=True)]
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=_Shops.LOW_PRIORITY_DROPSHIP_WAREHOUSE_ID, home_region=213, is_express=True)
        ]
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=_Shops.HIGH_PRIORITY_DROPSHIP_WAREHOUSE_ID, home_region=214, is_express=True)
        ]
        cls.dynamic.lms += [DynamicWarehouseInfo(id=666, home_region=213, is_express=True)]

    def test_single_offer(self):
        """
        Переопределен тест из test_combine.T
        Проверяем только дропшипы — поскольку они экспресс, то подмен нет!
        """

        for offer in (_Offers.fridge_555_offer, _Offers.fridge_444_offer):
            response = self.request_combine([_MSKUs.fridge], (offer,), no_replace_offers=[offer])
            # Исходный офер - нет подмен!
            self.check_offer_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, offer, cart_item_id=1)

    def test_combine_success(self):
        """
        Переопределен тест из test_combine.T
        Проверяем только дропшипы — поскольку они экспресс, то подмен нет!
        """
        replacements_flag = (
            '&split-strategy=consolidate-without-crossdock&rearr-factors=exclude_banned_warehouses_in_consolidation=0'
        )

        # Экспресс дропшип - 3 товара - нет подмен!
        response = self.request_combine(
            (_MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave),
            (_Offers.hoover_555_offer, _Offers.fridge_444_offer, _Offers.microwave_555_offer),
            flags=replacements_flag,
            no_replace_offers=[_Offers.hoover_555_offer, _Offers.fridge_444_offer, _Offers.microwave_555_offer],
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_555_offer, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_444_offer, cart_item_id=2
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.microwave_555_offer, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

    def test_invalid_offer_and_combine(self):
        """
        Переопределен тест из test_combine.T
        Проверяем только дропшипы — поскольку они экспресс, то подмен нет!
        """

        """
        В запросе приходит протухший мультиоффер — мы пытаемся вместо него использовать байбокс этого MSKU
        Количество суммируется по всем позициям мультиоффера — для дальнейшего использования в логике combine
        Здесь везде — протухший офер = несоответствующий своему MSKU (MSKUs.fridge и Offers.coke_*)
        """

        # replaced invalid multioffer and another offer from the same WH -> successful consolidation
        # another offer (hoover_555_offer) WH changed, because 444 has higher priority
        response = self.request_combine(
            (_MSKUs.fridge, _MSKUs.fridge, _MSKUs.hoover),
            (_Offers.coke_s1_172, _Offers.coke_s2_147, _Offers.hoover_555_offer),
            no_replace_offers=[_Offers.coke_s1_172, _Offers.coke_s2_147, _Offers.hoover_555_offer],
        )
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.coke_s2_147,
            2,
            cart_item_id=[1, 2],
            fail=True,
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_555_offer, 1, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

    def test_partial_replacement(self):
        """
        Переопределен тест из test_combine.T
        Проверяем только дропшипы — поскольку они экспресс, то подмен нет!
        """

        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave),
            (
                _Offers.alyonka_s1_172,
                _Offers.coke_s1_148,
                _Offers.hoover_444_offer,
                _Offers.fridge_555_offer,
                _Offers.microwave_555_offer,
            ),
            no_replace_offers=[_Offers.hoover_444_offer, _Offers.fridge_555_offer, _Offers.microwave_555_offer],
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, cart_item_id=2
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_444_offer, cart_item_id=3
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_555_offer, cart_item_id=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.microwave_555_offer, cart_item_id=5
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 3)

    def test_unused_bucket_position_in_strategy(self):
        """
        Переопределен тест из test_combine.T
        Проверяем только дропшипы — поскольку они экспресс, то подмен нет!
        """

        """
        Проверяем, что посылка с разобранными офферами идет последней
        """
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave, _MSKUs.notebook),
            (
                _Offers.alyonka_s1_172,
                _Offers.coke_s1_148,
                _Offers.hoover_444_offer,
                _Offers.fridge_555_offer,
                _Offers.microwave_555_offer,
                _Offers.notebook_out_of_stock,
            ),
            no_replace_offers=[
                _Offers.hoover_444_offer,
                _Offers.fridge_555_offer,
                _Offers.microwave_555_offer,
                _Offers.notebook_out_of_stock,
            ],
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, cart_item_id=2
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_444_offer, cart_item_id=3
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_555_offer, cart_item_id=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.microwave_555_offer, cart_item_id=5
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.notebook_out_of_stock, fail=True, cart_item_id=6
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "name": self.CONSOLIDATE_WITHOUT_CROSSDOCK,
                        "buckets": [
                            {"warehouseId": 444, "offers": ElementCount(1)},
                            {"warehouseId": 172, "offers": ElementCount(2)},
                            {"warehouseId": 555, "offers": ElementCount(2)},
                            {"warehouseId": 0, "offers": ElementCount(1)},
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    def test_delivery_and_delivery_partner_types(self):
        """
        Переопределен тест из test_combine.T
        Проверяем только дропшипы — поскольку они экспресс, то подмен нет!
        """

        # В норме для дропшипов delivery_partner_type - Маркет:
        # у 555 склада настроена партнерская служба - это нормальный кейс
        response = self.request_combine(
            (_MSKUs.fridge,),
            (_Offers.fridge_555_offer,),
            region=213,
            count=1,
            no_replace_offers=[_Offers.fridge_555_offer],
        )
        self.check_parcel_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            shop_id=_Shops.DROPSHIP_SUPPLIER_ID,
            has_courier=True,
            has_pickup=False,
            partner_type=self.MARKET_PARTNER_TYPE,
            is_automatic=True,
        )

    def test_invalid_express_offers(self):
        # Офер не доставляется - нет подмен!
        response = self.request_combine(
            [_MSKUs.fridge], (_Offers.fridge_555_offer,), region=2, no_replace_offers=[_Offers.fridge_555_offer]
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_555_offer, cart_item_id=1, fail=True
        )

        # Офера нет в индексе
        response = self.request_combine(
            [_MSKUs.fridge], (_Offers.fridge_555_offer_invalid,), no_replace_offers=[_Offers.fridge_555_offer_invalid]
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_555_offer_invalid, cart_item_id=1, fail=True
        )


if __name__ == '__main__':
    main()

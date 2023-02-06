#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    ClickType,
    Currency,
    ExperimentalBoostFeeGroup,
    ExperimentalBoostFeeReservePrice,
    MarketSku,
    Shop,
    Tax,
)
from core.testcase import TestCase, main
from core.matcher import Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
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
            ),
            Shop(
                fesh=111,
                datafeed_id=111,
                priority_region=213,
                name='in group 1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=222,
                datafeed_id=222,
                priority_region=213,
                name='in group 2',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=333,
                datafeed_id=333,
                priority_region=213,
                name='in group 3',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]

        cls.index.experimental_boost_fee_groups += [
            ExperimentalBoostFeeGroup(1, 111),
            ExperimentalBoostFeeGroup(2, 222),
            ExperimentalBoostFeeGroup(3, 333),
        ]
        cls.index.experimental_boost_fee_reserve_prices += [
            ExperimentalBoostFeeReservePrice(2, 888),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=1,
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(price=1000, feedid=333, fee=300, randx=100),  # from group 3
                    BlueOffer(price=1000, feedid=111, fee=200, randx=200),  # from group 1
                    BlueOffer(price=1000, feedid=222, fee=100, randx=300),  # from group 2
                ],
            ),
            MarketSku(
                hid=2,
                hyperid=2,
                sku=2,
                blue_offers=[
                    BlueOffer(price=1000, feedid=111, fee=200),  # from group 1
                ],
            ),
        ]

    def test_baseline(self):
        '''
        Проверяем ранжирование и цены в кликах вне эксперимента
        '''
        response = self.report.request_json(
            'place=productoffers&hyperid=1' '&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'supplier': {'id': 333},
                },
                {
                    'supplier': {'id': 111},
                },
                {
                    'supplier': {'id': 222},
                },
            ],
            preserve_order=True,
        )
        self.click_log.expect(ClickType.CPA, position=1, supplier_id=333, shop_fee=300, shop_fee_ab=201)
        self.click_log.expect(ClickType.CPA, position=2, supplier_id=111, shop_fee=200, shop_fee_ab=101)
        self.click_log.expect(ClickType.CPA, position=3, supplier_id=222, shop_fee=100, shop_fee_ab=0)

    def test_group_1(self):
        '''
        Проверяем ранжирование и цены в кликах при бустинге первой группы мерчей
        '''
        response = self.report.request_json(
            'place=productoffers&hyperid=1'
            '&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1'
            '&rearr-factors=market_money_boost_fee_to_rp_for_group_1=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'supplier': {'id': 111},
                },
                {
                    'supplier': {'id': 333},
                },
                {
                    'supplier': {'id': 222},
                },
            ],
            preserve_order=True,
        )
        # с бустанутых мерчей списываем 0
        self.click_log.expect(ClickType.CPA, position=1, supplier_id=111, shop_fee=520, shop_fee_ab=0)
        self.click_log.expect(ClickType.CPA, position=2, supplier_id=333, shop_fee=300, shop_fee_ab=101)
        self.click_log.expect(ClickType.CPA, position=3, supplier_id=222, shop_fee=100, shop_fee_ab=0)

    def test_group_2(self):
        '''
        Проверяем ранжирование и цены в кликах при бустинге второй группы мерчей
        '''
        response = self.report.request_json(
            'place=productoffers&hyperid=1'
            '&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1'
            '&rearr-factors=market_money_boost_fee_to_rp_for_group_2=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'supplier': {'id': 222},
                },
                {
                    'supplier': {'id': 333},
                },
                {
                    'supplier': {'id': 111},
                },
            ],
            preserve_order=True,
        )
        # с бустанутых мерчей списываем 0
        self.click_log.expect(ClickType.CPA, position=1, supplier_id=222, shop_fee=520, shop_fee_ab=0)
        self.click_log.expect(ClickType.CPA, position=2, supplier_id=333, shop_fee=300, shop_fee_ab=201)
        self.click_log.expect(ClickType.CPA, position=3, supplier_id=111, shop_fee=200, shop_fee_ab=0)

    def test_group_3(self):
        '''
        Проверяем ранжирование и цены в кликах при бустинге третьей группы мерчей
        '''
        response = self.report.request_json(
            'place=productoffers&hyperid=1'
            '&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1'
            '&rearr-factors=market_money_boost_fee_to_rp_for_group_3=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'supplier': {'id': 333},
                },
                {
                    'supplier': {'id': 111},
                },
                {
                    'supplier': {'id': 222},
                },
            ],
            preserve_order=True,
        )
        # с бустанутых мерчей списываем 0
        self.click_log.expect(ClickType.CPA, position=1, supplier_id=333, shop_fee=520, shop_fee_ab=0)
        self.click_log.expect(ClickType.CPA, position=2, supplier_id=111, shop_fee=200, shop_fee_ab=101)
        self.click_log.expect(ClickType.CPA, position=3, supplier_id=222, shop_fee=100, shop_fee_ab=0)

    def test_groups_1_2(self):
        '''
        Проверяем ранжирование и цены в кликах при бустинге первой и второй групп мерчей
        '''
        response = self.report.request_json(
            'place=productoffers&hyperid=1'
            '&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1'
            '&rearr-factors=market_money_boost_fee_to_rp_for_group_1=1;market_money_boost_fee_to_rp_for_group_2=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'supplier': {'id': 222},
                },
                {
                    'supplier': {'id': 111},
                },
                {
                    'supplier': {'id': 333},
                },
            ],
            preserve_order=True,
        )
        # с бустанутых мерчей списываем 0
        self.click_log.expect(ClickType.CPA, position=1, supplier_id=222, shop_fee=520, shop_fee_ab=0)
        self.click_log.expect(ClickType.CPA, position=2, supplier_id=111, shop_fee=520, shop_fee_ab=0)
        self.click_log.expect(ClickType.CPA, position=3, supplier_id=333, shop_fee=300, shop_fee_ab=0)

    def test_fuid(self):
        '''
        Проверяем запись денег в fuid
        '''
        _ = self.report.request_json(
            'place=productoffers&hyperid=1'
            '&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1'
            '&rearr-factors=market_money_boost_fee_to_rp_for_group_1=1;market_write_fee_to_fuid=1'
        )
        self.click_log.expect(
            ClickType.CPA, position=1, supplier_id=111, shop_fee=520, shop_fee_ab=0, fuid=Contains('fee=301')
        )

    def test_reserve_price(self):
        '''
        Проверяем что RP берется из файла
        '''
        _ = self.report.request_json(
            'place=productoffers&hyperid=2'
            '&rearr-factors=market_uncollapse_supplier=1;enable_business_id=1'
            '&rearr-factors=market_money_boost_fee_to_rp_for_group_1=1'
        )
        self.click_log.expect(ClickType.CPA, supplier_id=111, shop_fee=888)


if __name__ == '__main__':
    main()

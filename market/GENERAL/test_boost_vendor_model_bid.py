#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClickType, MnPlace, Model
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(hid=1, hyperid=1, ts=1, vendor_id=1),  # hasn't vbid, is not boosted
            Model(hid=1, hyperid=2, ts=2, vbid=10, vendor_id=1, datasource_id=1),  # has vbid, is not boosted
            Model(hid=1, hyperid=3, ts=3, vendor_id=33),  # hasn't vbid, is boosted
            Model(hid=1, hyperid=4, ts=4, vbid=10, vendor_id=33, datasource_id=33),  # has vbid, is boosted
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.13)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.111)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.105)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.1)

    def test_search__baseline(self):
        '''
        Проверяем, что по дефолту ставки, ранжирование и автоброкер работают ожидаемо
        '''
        response = self.report.request_json('place=prime&hid=1&show-urls=productVendorBid&rids=225')
        self.assertFragmentIn(
            response,
            [
                {
                    'id': 1,
                },
                {
                    'id': 2,
                },
                {
                    'id': 4,
                },
                {
                    'id': 3,
                },
            ],
            preserve_order=True,
        )
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=2, vendor_price=1, vc_bid=10)
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=3, vendor_price=0, vc_bid=0)
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=4, vendor_price=4, vc_bid=10)

    def test_search__boost_bid(self):
        '''
        Проверяем, что  при флаге market_money_boost_vendor_model_bid:
        - ставки указанных вендоров стали 84 у.е
        - ранжирование с учётом завышенной ставки
        - цена кликов у таких вендоров 0, а расчитанная - записана в fuid
        - цена кликов остальных вендоров расчитана с учётом завышенной
        '''
        response = self.report.request_json(
            'place=prime&hid=1&show-urls=productVendorBid&rids=225'
            '&rearr-factors='
            'market_money_boost_vendor_model_bid=1;'
            'market_money_boost_vendor_model_bid_ratio=10000;'
            'market_write_click_price_to_fuid=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'id': 3,
                },
                {
                    'id': 1,
                },
                {
                    'id': 4,
                },
                {
                    'id': 2,
                },
            ],
            preserve_order=True,
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=2, vendor_price=1, vc_bid=10)
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=3, vendor_price=0, vc_bid=8400, fuid='vcp=55'
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=4, vendor_price=0, vc_bid=8400, fuid='vcp=41'
        )

    def test_disabled_vendors_flag(self):
        '''
        Проверяем, что при флаге market_money_boost_vendor_model_bid для вендоров
        указанных в market_money_boost_vendor_model_bid_disabled_vendors эксперимент не работает
        '''
        response = self.report.request_json(
            'place=prime&hid=1&show-urls=productVendorBid&rids=225'
            '&rearr-factors='
            'market_money_boost_vendor_model_bid=1;'
            'market_money_boost_vendor_model_bid_ratio=10000;'
            'market_write_click_price_to_fuid=1;'
            'market_money_boost_vendor_model_bid_disabled_vendors=33'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'id': 1,
                },
                {
                    'id': 2,
                },
                {
                    'id': 4,
                },
                {
                    'id': 3,
                },
            ],
            preserve_order=True,
        )
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=2, vendor_price=1, vc_bid=10)
        # self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=3).times(0)
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=4, vendor_price=4, vc_bid=10)

    def test_boost_bid_ratio(self):
        '''
        Проверяем, что ставка увеличивается в market_money_boost_bid_ratio раз
        '''
        self.report.request_json(
            'place=prime&hid=1&show-urls=productVendorBid&rids=225'
            '&rearr-factors='
            'market_money_boost_vendor_model_bid=1;'
            'market_write_click_price_to_fuid=1;'
            'market_money_boost_vendor_model_bid_ratio=100'
        )
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=4, vc_bid=1000)

    def test_boost_bid_from_min_bid(self):
        '''
        Проверяем, что при наличии флага market_money_boost_bid_from_min_bid
        ставка увеличивается в market_money_boost_bid_ratio раз от минимальной
        '''
        self.report.request_json(
            'place=prime&hid=1&show-urls=productVendorBid&rids=225'
            '&rearr-factors='
            'market_money_boost_vendor_model_bid=1;'
            'market_write_click_price_to_fuid=1;'
            'market_money_boost_vendor_model_bid_from_min_bid=1;'
            'market_money_boost_vendor_model_bid_ratio=100'
        )
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=4, vc_bid=100)

    def test_boost_bid_from_min_bid_low_ratio(self):
        '''
        Проверяем, что при наличии флага market_money_boost_bid_from_min_bid
        ставка не может стать меньше указанной вендором
        '''
        self.report.request_json(
            'place=prime&hid=1&show-urls=productVendorBid&rids=225'
            '&rearr-factors='
            'market_money_boost_vendor_model_bid=1;'
            'market_write_click_price_to_fuid=1;'
            'market_money_boost_vendor_model_bid_from_min_bid=1;'
            'market_money_boost_vendor_model_bid_ratio=2'
        )
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=4, vc_bid=10)


if __name__ == '__main__':
    main()

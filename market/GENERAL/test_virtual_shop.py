#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=2, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=3, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_RED),
            Shop(fesh=4, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_RED),
            Shop(fesh=7, fulfillment_virtual=True),
        ]

    def test_virtual_shop(self):
        '''Проверка мониторинга наличия двух виртуальных магазинов и отсутствия цвета виртуального магазина в файле shops.dat'''
        self.error_log.expect('Duplicate feed for blue virtual shop').once()
        self.error_log.expect('Duplicate feed for red virtual shop').once()
        self.error_log.expect('Missing virtual shop color for shop_id: 7').once()
        self.base_logs_storage.error_log.expect('Duplicate feed for blue virtual shop').once()
        self.base_logs_storage.error_log.expect('Duplicate feed for red virtual shop').once()
        self.base_logs_storage.error_log.expect('Missing virtual shop color for shop_id: 7').once()


if __name__ == '__main__':
    main()

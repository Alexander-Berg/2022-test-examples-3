# -*- coding: utf-8 -*-
from attrdict import AttrDict
import mock
from nose_parameterized import parameterized

from unit.base import NoDBTestCase
from mpfs.core.billing.processing.common import stat
from mpfs.common.static.tags.billing import BUY_NEW, SUCCESS
from mpfs.core.billing import Product, Client


class StatTestCase(NoDBTestCase):
    @parameterized.expand([
        ('int', 1),
        ('str', '1.2'),
        ('float', 1.33),
    ])
    def test_price_types(self, case, price):
        client = Client('123')
        product = Product(pid="1tb_1m_apple_appstore_2019")
        order = AttrDict({'currency': 'RUB', 'auto': True, 'price': price, 'number': '111111'})
        params = {
            'action': BUY_NEW,
            'client': client,
            'order': order,
            'status': SUCCESS,
            'product': product
        }
        with mock.patch('mpfs.common.util.logger.MPFSLogger.error') as error_logger_mock:
            stat(**params)
            assert not error_logger_mock.called

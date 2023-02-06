# -*- coding: utf-8 -*-
from mpfs.core.billing import Product
from unit.base import NoDBTestCase


class ProductTestCase(NoDBTestCase):
    def test_getting_messages_for_non_auto(self):
        product = Product('10gb_1m_2015')
        names = product.get_names()
        assert names['en'] == u'10 GB for a month'

    def test_getting_messages_for_auto(self):
        product = Product('10gb_1m_2015')
        names = product.get_names(auto=True)
        assert names['en'] == u'10 GB monthly subscription'

    def test_product_without_sub_name_return_common_name(self):
        product = Product('test_1kb_for_one_second')
        names = product.get_names(auto=True)
        assert names['en'] == u'TEST: kb for one second'

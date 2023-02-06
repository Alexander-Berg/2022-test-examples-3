# -*- coding: utf-8 -*-

import unittest

from market.idx.snippets.src import delivery


class Offer(object):
    def __init__(self, data):
        self.currency_by_home_region = 'RUR'
        self.data = data

    def __getattr__(self, name):
        if name in self.data:
            return self.data[name]
        raise AttributeError(name)


Feed = Offer


def dd(**kwargs):
    return Feed(kwargs)


def calc_delivery(feed, offer):
    price, currency = delivery.calc_cost(feed, offer, None)
    return price


class TestDelivery(unittest.TestCase):
    def test_web_1(self):
        '''
        delivery_src=WEB
        delivery=false
        result=-1
        '''
        feed = dd(delivery_src='WEB')
        offer = dd(has_delivery=False, delivery_price=None, delivery_currency=None, downloadable=None)
        self.assertEqual(calc_delivery(feed, offer), -1)

    def test_web_2(self):
        '''
        delivery_src=YML
        delivery=false
        result=-1
        '''
        feed = dd(delivery_src='YML')
        offer = dd(has_delivery=False, delivery_price=None, delivery_currency=None, downloadable=None)
        self.assertEqual(calc_delivery(feed, offer), -1)

    def test_web_3(self):
        '''
        delivery_src=YML
        delivery=true
        result=-1
        '''
        local_delivery_cost = 100
        feed = dd(delivery_src='WEB', local_delivery_cost=local_delivery_cost)
        offer = dd(has_delivery=True, delivery_price=None, delivery_currency=None, downloadable=None)
        self.assertAlmostEqual(calc_delivery(feed, offer), local_delivery_cost / 100.0)

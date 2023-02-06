#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop

# Tests intents (also known as пупыри).
# See https://st.yandex-team.ru/MARKETOUT-7492.


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.shops += [Shop(fesh=100), Shop(fesh=101)]

        cls.index.offers = [
            Offer(
                title='Just The Offer',
                hid=1,
                feed_category_id=1986,
                fesh=100,
                price=5000,
                price_old=9000,
                hyperid=100,
                bid=0,
            ),
            Offer(
                title='Offer_2',
                hid=2,
                feed_category_id=8289309,
                fesh=100,
                price=10000,
                price_old=12500,
                hyperid=20,
                bid=0,
            ),
            Offer(hyperid=101, fesh=101, feedid=101101, price=10000, bid=1, pull_to_min_bid=False),
            Offer(hyperid=102, fesh=101, feedid=101102, price=10000, bid=1, pull_to_min_bid=True),
        ]

    def test_shooffers(self):
        response = self.report.request_xml('place=shopoffers&fesh=100&shop-offers-chunk=1&debug=1')
        self.assertFragmentIn(
            response,
            '''
            <offers>
                <offer>
                    <bid>13</bid>
                </offer>
            </offers>
        ''',
        )

    def test_categ_and_hid(self):
        """Проверяем что выводятся свойства документов
        hyper_id, hyper_categ_id (id категории в маркете)
        feed_category_id (id категории в магазине)
        """
        response = self.report.request_xml('place=shopoffers&fesh=100&shop-offers-chunk=1&debug=1')
        self.assertFragmentIn(
            response,
            '''
            <offers>
                <offer>
                    <raw-title>Just The Offer</raw-title>
                    <hyper_id>100</hyper_id>
                    <hyper_categ_id>1</hyper_categ_id>
                    <feed_category_id>1986</feed_category_id>
                </offer>
                <offer>
                    <raw-title>Offer_2</raw-title>
                    <hyper_id>20</hyper_id>
                    <hyper_categ_id>2</hyper_categ_id>
                    <feed_category_id>8289309</feed_category_id>
                </offer>
            </offers>
        ''',
        )

    def test_missing_pp(self):
        self.report.request_xml(
            'place=shopoffers&fesh=100&shop-offers-chunk=1&debug=1&ip=127.0.0.1', add_defaults=False
        )

    def test_total(self):
        response = self.report.request_xml('place=shopoffers&fesh=100&shop-offers-chunk=1')
        self.assertFragmentIn(response, '<search_results total="2"></search_results>')

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='2')

    def test_price_and_discount(self):
        """
        Проверка корректного отображения информации о price, oldprice, discount на place=shopoffers
        """
        response = self.report.request_xml('place=shopoffers&fesh=100&shop-offers-chunk=1')
        self.assertFragmentIn(
            response,
            '''
            <offers>
                <offer>
                    <raw-title>Offer_2</raw-title>
                    <price currency="RUR">10000</price>
                    <oldprice currency="RUR" discount-percent="20">12500</oldprice>
                </offer>
                <offer>
                    <raw-title>Just The Offer</raw-title>
                    <price currency="RUR">5000</price>
                    <oldprice currency="RUR" discount-percent="44">9000</oldprice>
                </offer>
            </offers>
        ''',
        )

    def test_original_bids(self):
        """
        Проверяем возвращаются ли исходные ставки, если магазин запретил подтягивать их до минимальных значений
        """

        response = self.report.request_xml('place=shopoffers&fesh=101&shop-offers-chunk=1&api=partner')
        self.assertFragmentIn(
            response,
            """
            <offers> <offer>
                <hyper_id>101</hyper_id>
                <bid>1</bid>
            </offer> <offer>
                <hyper_id>102</hyper_id>
                <bid>13</bid>
            </offer> </offers>
        """,
        )

    def test_filter_by_feed_id(self):
        """Check if offers are filtered by feed ID when it's set"""

        offer1 = "<offer><hyper_id>101</hyper_id><feed_id>101101</feed_id></offer>"
        offer2 = "<offer><hyper_id>102</hyper_id><feed_id>101102</feed_id></offer>"

        request = "place=shopoffers&fesh=101&shop-offers-chunk=1"

        response = self.report.request_xml(request)
        self.assertFragmentIn(response, "<offers>" + offer1 + offer2 + "</offers>")

        response = self.report.request_xml(request + "&feedid=101101")
        self.assertFragmentIn(response, offer1)
        self.assertFragmentNotIn(response, offer2)

        response = self.report.request_xml(request + "&feedid=101102")
        self.assertFragmentNotIn(response, offer1)
        self.assertFragmentIn(response, offer2)


if __name__ == '__main__':
    main()

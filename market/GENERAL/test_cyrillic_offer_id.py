#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, Offer, Shop, Tax
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=123,
                datafeed_id=100,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
            )
        ]

        cls.index.offers += [
            Offer(
                feedid=100,
                fesh=123,
                title="ЯБ001",
                offerid="ЯБ002",
            ),
            Offer(
                feedid=100,
                fesh=123,
                title="ОХ541",
                offerid="ОХ542",
            ),
            Offer(
                feedid=100,
                fesh=123,
                title="ABC26",
                offerid="ABC27",
            ),
        ]

    def test_batch_feed(self):
        # Все предложения из фида находятся
        response = self.report.request_json('place=print_doc&feed_shoffer_id=100-*')
        self.assertFragmentIn(
            response,
            {
                "documents_count": 3,
            },
        )

    def test_cyrillic_text(self):
        # Поиск по тексту идет
        response = self.report.request_json('place=print_doc&text=ЯБ001')
        self.assertFragmentIn(
            response,
            {
                "documents_count": 1,
            },
        )

    def test_cyrillic_offer_id(self):
        # Находится конкретный офер с кириллицей
        response = self.report.request_json('place=print_doc&feed_shoffer_id=100-ЯБ002')
        self.assertFragmentIn(response, {"documents_count": 1, "documents": [{"title": "ЯБ001"}]})

    def test_multiple_offers(self):
        # Находятся 2 кириллических оффера, переданные списком
        response = self.report.request_json('place=print_doc&feed_shoffer_id=100-ЯБ002,100-ОХ542')
        self.assertFragmentIn(response, {"documents_count": 2, "documents": [{"title": "ЯБ001"}, {"title": "ОХ541"}]})


if __name__ == '__main__':
    main()

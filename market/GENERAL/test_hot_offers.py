#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, Model, Region, Shop, Tax
from core.testcase import TestCase, main
from core.matcher import ElementCount, Less
from core.types.sku import MarketSku, BlueOffer


min_ref_price = 100


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=300, name='Minbari', region_type=Region.FEDERAL_DISTRICT)
        ]  # no offers from aliens

        cls.index.models += [
            Model(
                hyperid=120,
                hid=1,
                title="First Model",
            ),
            Model(
                hyperid=220,
                hid=2,
                title="Second Model",
            ),
            Model(
                hyperid=320,
                hid=3,
                title="Third Model",
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
            ),
            Shop(
                fesh=7,
                datafeed_id=7,
                priority_region=77,
                name='blue_shop_7',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=8,
                datafeed_id=8,
                priority_region=42,
                name='blue_shop_8',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Sku {}".format(i),
                fesh=7,
                hyperid=120,
                sku=i,
                randx=i,
                ref_min_price=min_ref_price,
                blue_offers=[
                    BlueOffer(price=49 * i, vendor_id=i % 3, feedid=7, hid=1),
                ],
            )
            for i in range(1, 6)
        ]
        cls.index.mskus += [
            MarketSku(
                title="Sku {}".format(i),
                fesh=8,
                hyperid=220,
                sku=i,
                randx=i,
                ref_min_price=100,
                blue_offers=[
                    BlueOffer(price=500 - (49 * i), feedid=100, hid=2),
                ],
            )
            for i in range(6, 11)
        ]
        cls.index.mskus += [
            MarketSku(
                title="Sku {}".format(i),
                fesh=7,
                hyperid=320,
                sku=i,
                randx=i,
                ref_min_price=min_ref_price,
                blue_offers=[
                    BlueOffer(price=500 + (49 * (i - 5) * ((-1) ** i)), feedid=102, hid=3),
                ],
            )
            for i in range(11, 16)
        ]

    @classmethod
    def prepare_offers_number(cls):
        cls.index.mskus += [
            MarketSku(
                title="very cheap sku {}".format(i),
                fesh=7,
                hyperid=120,
                sku=100 * i,
                randx=i,
                ref_min_price=min_ref_price,
                blue_offers=[
                    BlueOffer(price=49, vendor_id=i % 3, feedid=7, hid=1),
                ],
            )
            for i in range(1, 6)
        ]

    def test_offers_number(self):
        """
        Проверяем, что если возвращаются нужные оффера (с заданным hid) в нужном количесве (проверяем опцию numdoc)
        """
        response = self.report.request_json('place=hot_offers&hid=1&numdoc=3&rids=213')
        """
        Проверяем количество
        """
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        """
        Проверяем, что цены лучше чем мин реф прайс
        """
        self.assertFragmentIn(response, {"prices": {"value": Less(min_ref_price)}})
        """
        Проверяем, что нет офферов с неправильными
        """
        self.assertFragmentNotIn(response, {"categories": [{"entity": "category", "id": 2}]})
        self.assertFragmentNotIn(response, {"categories": [{"entity": "category", "id": 3}]})

    def test_offers_min_price(self):
        """
        Проверяем, что если возвращаются нужные оффера (с заданным hid) в нужном количесве: только те, у которых цена лучше ref_min_price
        """
        response = self.report.request_json('place=hot_offers&hid=1&rids=213')
        """
        Проверяем количество
        """
        self.assertFragmentIn(response, {"total": 7}, allow_different_len=True, preserve_order=True)
        self.assertFragmentIn(response, {"results": ElementCount(7)})
        """
        Проверяем, что цены лучше чем мин реф прайс
        """
        self.assertFragmentIn(response, {"prices": {"value": Less(min_ref_price)}})
        """
        Проверяем, что нет офферов с неправильными
        """
        self.assertFragmentNotIn(response, {"categories": [{"entity": "category", "id": 2}]})
        self.assertFragmentNotIn(response, {"categories": [{"entity": "category", "id": 3}]})

    def test_offers_by_multiple_hids(self):
        """
        Проверяем, что если возвращаются нужные оффера (c двумя hid из трех возможных) в нужном количестве: только те, у которых цена лучше ref_min_price
        """
        response = self.report.request_json('place=hot_offers&hid=3,2&rids=213')

        """
        Проверяем количество
        """
        self.assertFragmentIn(response, {"results": ElementCount(3)})

        """
        проверяем качество - выбрали лучшие
        """
        self.assertFragmentIn(response, {"prices": {"value": Less(min_ref_price)}})

        """
        Проверяем, что не закрались оффера с неправильным hid
        """
        self.assertFragmentNotIn(response, {"categories": [{"entity": "category", "id": 1}]})

    def test_region_filtering(self):
        """
        Проверяем, что фильтрация по региону работает, а hid - опциональный атррибут
        """
        response = self.report.request_json('place=hot_offers&numdoc=2&rids=300')
        self.assertFragmentIn(response, {})
        self.error_log.expect(code=3043)

    @classmethod
    def prepare_offers_order(cls):
        cls.index.models += [
            Model(
                hyperid=420,
                hid=4,
                title="Model for sorting",
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title="First",
                fesh=7,
                hyperid=420,
                sku=101002,
                ref_min_price=100,
                blue_offers=[
                    BlueOffer(price=60, feedid=7, hid=4),
                ],
            ),
            MarketSku(
                title="Third",
                fesh=7,
                hyperid=420,
                sku=102002,
                ref_min_price=200,
                blue_offers=[
                    BlueOffer(price=100, feedid=7, hid=4),
                ],
            ),
            MarketSku(
                title="Second",
                fesh=7,
                hyperid=420,
                sku=103002,
                ref_min_price=400,
                blue_offers=[
                    BlueOffer(price=300, feedid=7, hid=4),
                ],
            ),
        ]

    def test_offers_order(self):
        """
        Проверяем, что сортировка работает - чем больше разница в % с минимальной референсной минимальная (максимальна по модулю и отрицательна) тем выше в списке
        """
        response = self.report.request_json('place=hot_offers&hid=4&rids=213')
        self.assertFragmentIn(
            response,
            {"results": [{"prices": {"value": "100"}}, {"prices": {"value": "60"}}, {"prices": {"value": "300"}}]},
            allow_different_len=True,
            preserve_order=True,
        )

    @classmethod
    def prepare_paging(cls):
        cls.index.models += [
            Model(
                hyperid=520,
                hid=5,
                title="Model for paging",
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title="TROLOLO",
                fesh=7,
                hyperid=520,
                sku=5000 + i,
                ref_min_price=100,
                blue_offers=[
                    BlueOffer(
                        price=i, feedid=7, hid=5
                    ),  # rise in prices is used to get the predefined order to check paging correctness
                ],
            )
            for i in range(1, 21)
        ]

    def test_paging(self):
        """
        Проверяем, работу постраничного вывода
        """
        response = self.report.request_json('place=hot_offers&hid=5&rids=213&numdoc=4&page=2')

        self.assertFragmentIn(
            response,
            {"results": [{"sku": "5005"}, {"sku": "5006"}, {"sku": "5007"}, {"sku": "5008"}]},
            allow_different_len=True,
            preserve_order=True,
        )

    def test_price_restrictions(self):
        """
        Проверяем ограничение выдачи по ценам
        """
        response = self.report.request_json('place=hot_offers&hid=1&rids=213&mcpricefrom=51&mcpriceto=99')
        self.assertFragmentIn(response, {"results": ElementCount(1)})
        response = self.report.request_json('place=hot_offers&hid=1&rids=213&mcpricefrom=55')
        self.assertFragmentIn(response, {"results": ElementCount(1)})
        response = self.report.request_json('place=hot_offers&hid=1&rids=213&mcpriceto=55')
        self.assertFragmentIn(response, {"results": ElementCount(6)})

    def test_vendor_id(self):
        """
        Проверяем ограничение выдачи по vendor_id
        """
        response = self.report.request_json('place=hot_offers&hid=1&rids=213&vendor_id=2')
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"sku": "200", "vendor": {"id": 2}},
                    {"sku": "500", "vendor": {"id": 2}},
                    {"sku": "2", "vendor": {"id": 2}},
                ]
            },
        )

    def test_threshold(self):
        """
        Проверяем, что работает ограничение снизу по качеству - в выдачу попадают предложения, с ценой более чем на how-hot=26 процентов лучше чем min ref price
        """
        response = self.report.request_json('place=hot_offers&hid=4&rids=213&how-hot=26')
        self.assertFragmentIn(
            response,
            {"results": [{"prices": {"value": "100"}}, {"prices": {"value": "60"}}]},
            allow_different_len=False,
        )

    @classmethod
    def prepare_no_ref_min_price(cls):
        cls.index.models += [
            Model(
                hyperid=620,
                hid=6,
                title="Model for no ref min price",
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title="TROLOLO",
                fesh=8,
                hyperid=620,
                sku=6000 + i,
                blue_offers=[
                    BlueOffer(price=i, feedid=7, hid=6),
                ],
            )
            for i in range(1, 21)
        ]

    def test_no_ref_min_price(self):
        """
        Проверяем, что все нормально работает, если ref min price отсутствует
        """
        response = self.report.request_json('place=hot_offers&hid=6&rids=213&numdoc=3&page=2')
        self.assertFragmentIn(response, {"total": 0}, allow_different_len=True, preserve_order=True)

    @classmethod
    def prepare_sometimes_no_ref_min_price(cls):
        cls.index.models += [
            Model(
                hyperid=720,
                hid=7,
                title="Model for no ref min price",
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title="TROLOLO",
                fesh=8,
                hyperid=720,
                sku=7000 + i,
                blue_offers=[
                    BlueOffer(price=i, feedid=7, hid=7),
                ],
            )
            for i in range(1, 21)
        ]
        cls.index.mskus += [
            MarketSku(
                title="TROLOLO",
                fesh=8,
                hyperid=720,
                ref_min_price=100,
                sku=7100 + i,
                blue_offers=[
                    BlueOffer(price=i, feedid=7, hid=7),
                ],
            )
            for i in range(1, 21)
        ]

    def test_sometimes_no_ref_min_price(self):
        """
        Проверяем, что все нормально работает, если ref min price иногда отсутствует
        """
        response = self.report.request_json('place=hot_offers&hid=7&rids=213&numdoc=3&page=2')
        self.assertFragmentIn(
            response,
            {"total": 20, "results": [{"sku": "7104"}, {"sku": "7105"}, {"sku": "7106"}]},
            allow_different_len=True,
            preserve_order=True,
        )

    def test_search_info(self):
        """
        Проверяем, что в выдаче присутствует searchInfo
        """
        response = self.report.request_json('place=hot_offers&hid=7&rids=213&numdoc=3&page=2')
        self.assertFragmentIn(
            response,
            {
                "total": 20,
                "totalOffers": 20,
                "totalOffersBeforeFilters": 20,
            },
            allow_different_len=True,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()

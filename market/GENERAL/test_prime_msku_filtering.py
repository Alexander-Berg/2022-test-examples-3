#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, Currency, MarketSku, Model, Shop, Tax


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.reqwizard.on_request('айфон').respond(
            qtree='cHicbZG_SwJhGMef5z293l4cxJDkIDIdPALhqEUaMqqhUYIgbioxOhcLW6xJ-gFaS9QWDVEJTWZKUJDhGNJwTi3N_R2993onl9ct97zv-3m_3-NzbJMFKAQhQqOgEg1CzGyYr71j891sKxCDaZiBuQD1cwI4ARoswApkYB02wLj_abFzhCuEW3TdayK0EfjzgWBiToMULj4ixSAoLioGKmqYuSMiCotVZK7TiOhKwapU6XR18lDXSa2elXkEqGNZyt_EbKiSPb0NphcxEYX0TpzJfOYTC6Li48FNsYuDOxZ55Oz1zlTJgBIeEIr6hURpGDr7y0pZYmtDluT8rrFT2HIZkr2GjMNZx47N_2fmmggzNmFbuXSsJJl9EEF-YAkhlbqOtb8qEnv8w7FEinEPLlVqdR0cXEBpD4QVr14rMzvCBWF_AAUTdsuUN6A81LHkRao6uWnp-DSoEZk5EY79llFeJ-UL2_0e_htIGYE7jPks-6qPr05xkolVaJyiMkblsP8rnp74_uzORyFpueX8LxO4oOI,'  # noqa
        )
        cls.reqwizard.on_default_request().respond()

        cls.index.shops += [
            Shop(
                fesh=120,
                datafeed_id=1,
                priority_region=213,
                regions=[213],
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
        ]
        cls.index.models += [
            Model(hyperid=110, hid=100, title="glass sphere"),
            Model(hyperid=111, hid=101, title="wooden box"),
            Model(hyperid=112, hid=102, title="steel pyramid"),
            Model(hyperid=113, hid=103, title="iphone se"),
        ]
        cls.index.mskus += [
            MarketSku(
                title="red sphere",
                blue_offers=[BlueOffer(waremd5='AAAAAAAAAAAAAAAAAAAAAA', price=100)],
                sku=150,
                hyperid=110,
            ),
            MarketSku(
                title="blue sphere",
                blue_offers=[BlueOffer(waremd5='AAAAAAAAAAAAAAAAAAAAAB', price=100)],
                sku=151,
                hyperid=110,
            ),
            MarketSku(
                title="red box",
                blue_offers=[BlueOffer(waremd5='AAAAAAAAAAAAAAAAAAAAAC', price=100)],
                sku=152,
                hyperid=111,
            ),
            MarketSku(
                title="blue box",
                blue_offers=[BlueOffer(waremd5='AAAAAAAAAAAAAAAAAAAAAD', price=100)],
                sku=153,
                hyperid=111,
            ),
            MarketSku(
                title="iphone se 64Gb gold",
                blue_offers=[BlueOffer(waremd5='AAAAAAAAAAAAAAAAAAAAAE', price=10000)],
                sku=154,
                hyperid=113,
            ),
        ]

    def test_search(self):
        '''
        MALISA-512 возможность фильтровать поиск по msku для поиска по истории заказов из алисы
        sku из истории передаются через параметры market-sku
        поиск должен происходить только по ним
        '''

        # слово sphere встречается и в названии модели и в названиях sku, относящихся к ней,
        # проверям, что находим модель с обоими оферами
        response = self.report.request_json('place=prime&text=sphere&market-sku=150,151&rgb=blue')
        self.assertFragmentIn(
            response,
            {'search': {'total': 1, 'results': [{'entity': 'product', 'id': 110, 'offers': {'count': 2}}]}},
            preserve_order=False,
        )

        # слово red есть в названиях sku 150 и 152, но с учетом ограниченного множества документов
        # должен найтись только sku 150
        response = self.report.request_json('place=prime&text=red&market-sku=150,151&rgb=blue')
        self.assertFragmentIn(
            response,
            {'search': {'total': 1, 'results': [{'entity': 'product', 'id': 110, 'offers': {'count': 1}}]}},
            preserve_order=False,
        )

        # слово glass есть только в названии модели
        # проверяем, что она не найдется
        response = self.report.request_json('place=prime&text=glass&market-sku=150,151,152,153&rgb=blue')
        self.assertFragmentIn(response, {'search': {'total': 0}}, preserve_order=False)

        # слово айфон отсутвует в названиях, но визард обогащает запрос словом iphone
        # проверяем, что поиск по слову айфон находит iphone
        response = self.report.request_json('place=prime&text=айфон&market-sku=154&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {'entity': 'product', 'id': 113},
                    ],
                }
            },
            preserve_order=False,
        )


if __name__ == '__main__':
    main()

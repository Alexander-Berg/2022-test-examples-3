#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DynamicPriceControlData,
    MarketSku,
    Shop,
    Tax,
    RegionalMsku,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='blue_shop_2',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_3',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=213,
                name='blue_shop_4',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=213,
                name='blue_shop_5',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                priority_region=213,
                name='blue_shop_6',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=7,
                datafeed_id=7,
                priority_region=213,
                name='blue_shop_7',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]
        cls.dynamic.market_dynamic.dynamic_price_control += [
            DynamicPriceControlData(2, 100, 1),
            DynamicPriceControlData(3, 0, 1),  # equal to not specified
            DynamicPriceControlData(4, 9.5, 1),
            DynamicPriceControlData(5, 10, 1),
            DynamicPriceControlData(6, 20, 1),
            DynamicPriceControlData(7, 10, 0),  # drop price to buybox strategy
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1,
                ref_min_price=1050,
                is_golden_matrix=True,
                blue_offers=[
                    BlueOffer(
                        price=1000,
                        feedid=1,
                        waremd5='Sku1Offer1-IiLVm1Goleg',
                    ),  # просто дешевый оффер
                    BlueOffer(
                        price=1100,
                        feedid=7,
                        waremd5='Sku1Offer4-IiLVm1Goleg',  # оффер со стратегией попадать в BB
                        offerid=4004001,
                    ),
                    BlueOffer(
                        price=1150,
                        feedid=5,
                        waremd5='Sku1Offer5-IiLVm1Goleg',  # оффер со стратегией снижать до мин рефа
                        offerid=5005001,
                    ),
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=2,
                ref_min_price=None,
                is_golden_matrix=True,
                blue_offers=[BlueOffer(price=1500, feedid=2, offerid=2002002)],
            ),
            MarketSku(
                hyperid=3,
                sku=3,
                ref_min_price=1400,
                is_golden_matrix=False,
                blue_offers=[BlueOffer(price=1500, feedid=4, offerid=4004003)],
            ),
            MarketSku(
                hyperid=4,
                sku=4,
                ref_min_price=1400,
                is_golden_matrix=True,
                blue_offers=[BlueOffer(price=1500, feedid=5, offerid=5005005, waremd5='Sku4Offer5-IiLVm1Goleg')],
            ),
            MarketSku(
                hyperid=5,
                sku=5,
                ref_min_price=1400,
                is_golden_matrix=True,
                blue_offers=[BlueOffer(price=1500, feedid=1, offerid=6006006)],
            ),
            MarketSku(
                hyperid=6,
                sku=6,
                ref_min_price=1190,
                is_golden_matrix=True,
                blue_offers=[BlueOffer(price=1500, feedid=6, offerid=7007007, waremd5='Sku6Offer6-IiLVm1Goleg')],
            ),
            MarketSku(
                hyperid=7,
                sku=7,
                ref_min_price=1400,
                is_golden_matrix=True,
                blue_offers=[BlueOffer(price=1200, feedid=6, offerid=8008008)],
            ),
            MarketSku(
                hyperid=8,
                sku=8,
                ref_min_price=950,
                is_golden_matrix=True,
                blue_offers=[
                    BlueOffer(price=1000, feedid=1, waremd5='Sku8Offer1-IiLVm1Goleg'),  # просто дешевый оффер
                    BlueOffer(
                        price=1100,
                        feedid=7,
                        waremd5='Sku8Offer4-IiLVm1Goleg',  # оффер со стратегией попадать в BB
                        offerid=8004001,
                    ),
                    BlueOffer(
                        price=1150,
                        feedid=6,
                        waremd5='Sku8Offer5-IiLVm1Goleg',  # оффер со стратегией снижать до мин рефа
                        offerid=8005001,
                    ),
                ],
            ),
            MarketSku(
                hyperid=9,
                sku=9,
                ref_min_price=950,
                is_golden_matrix=True,
                blue_offers=[
                    BlueOffer(price=900, feedid=1, waremd5='Sku9Offer1-IiLVm1Goleg'),  # просто дешевый оффер
                    BlueOffer(
                        price=1200,
                        feedid=7,
                        waremd5='Sku9Offer4-IiLVm1Goleg',  # оффер со стратегией попадать в BB
                        offerid=9004001,
                    ),
                    BlueOffer(
                        price=1150,
                        feedid=6,
                        waremd5='Sku9Offer5-IiLVm1Goleg',  # оффер со стратегией снижать до мин рефа
                        offerid=9005001,
                    ),
                ],
            ),
        ]

        cls.index.blue_regional_mskus += [
            RegionalMsku(msku_id=1, offers=3, price_min=1000, price_max=1150, rids=[213]),
        ]

    def test_sku_offers_drop_price(self):
        """
        Проверяем подмену цен в place=sku_offers
        Цена снижается, так как:
        1. Магазин выставил нужную стратегию
        2. У msku есть минимальная референсная цена
        3. Msku из Золотой матрицы
        4. Максимальная скидка на товар позволяет достичь минимальной референсной цены (1500 * 0.9 < 1400)
        """
        self.assertFragmentIn(
            self.report.request_json('place=sku_offers&market-sku=4&rgb=blue'),
            {
                'supplier': {'id': 5},
                'prices': {'value': '1400'},
                'refMinPrice': {'value': '1400'},
                'isGoldenMatrix': True,
            },
        )

    def test_sku_offers_drop_to_ref_min_price_disabled(self):
        """
        Проверяем что цена не меняется, так как стратегия отключена стоп-краном
        """
        self.assertFragmentIn(
            self.report.request_json(
                'place=sku_offers&market-sku=4&rgb=blue&rearr-factors=market_blue_disable_ref_min_price_strategy=1'
            ),
            {
                'supplier': {'id': 5},
                'prices': {'value': '1500'},
                'refMinPrice': {'value': '1400'},
                'isGoldenMatrix': True,
            },
        )

    def test_sku_offers_price_not_changed_no_strategy(self):
        """
        Проверяем что цена не меняется, так как не выбрана стратегия снижать до минимальной референсной цены
        """
        self.assertFragmentIn(
            self.report.request_json('place=sku_offers&market-sku=5&rgb=blue'),
            {
                'supplier': {'id': 1},
                'prices': {'value': '1500'},
                'refMinPrice': {'value': '1400'},
                'isGoldenMatrix': True,
            },
        )

    def test_sku_offers_price_not_changed_too_low_discount(self):
        """
        Проверяем что цена не меняется, скидка недостаточна
        """
        self.assertFragmentIn(
            self.report.request_json('place=sku_offers&market-sku=6&rgb=blue'),
            {
                'supplier': {'id': 6},
                'prices': {'value': '1500'},
                'refMinPrice': {'value': '1190'},
                'isGoldenMatrix': True,
            },
        )

    def test_sku_offers_price_changed_not_from_golden_matrix(self):
        """
        Проверяем что цена меняется, если даже, если msku не из Золотой матрицы
        """
        self.assertFragmentIn(
            self.report.request_json('place=sku_offers&market-sku=3&rgb=blue'),
            {
                'supplier': {'id': 4},
                'prices': {'value': '1400'},
                'refMinPrice': {'value': '1400'},
                'isGoldenMatrix': False,
            },
        )

    def test_sku_offers_price_not_changed_no_ref_min_price(self):
        """
        Проверяем что цена не меняется, если у msku нет минимальной референсной цены
        """
        self.assertFragmentIn(
            self.report.request_json('place=sku_offers&market-sku=2&rgb=blue'),
            {
                'supplier': {'id': 2},
                'prices': {'value': '1500'},
                'isGoldenMatrix': True,
            },
        )

    def test_sku_offers_price_not_changed_below_ref_min_price(self):
        """
        Проверяем что цена не меняется, если у msku цена уже ниже минимальной референсной цены
        """
        self.assertFragmentIn(
            self.report.request_json('place=sku_offers&market-sku=7&rgb=blue'),
            {
                'supplier': {'id': 6},
                'prices': {'value': '1200'},
                'refMinPrice': {'value': '1400'},
                'isGoldenMatrix': True,
            },
        )

    def test_buybox_battle_buybox_strategy_win(self):
        """
        Битву за buybox выигрывает оффер со стратегией попадать в BB,
        при этом цена на оффер со стратегией попадать в мин рефе снижается,
        но ее недостаточно чтобы попасть в BB
        """
        self.assertFragmentIn(
            self.report.request_json(
                'place=sku_offers&market-sku=1&rgb=blue&yandexuuid=3&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;enable_offline_buybox_price=0'
            ),
            {
                'supplier': {'id': 7},
                'prices': {'value': '1000'},
            },
        )

        # c оффлайн байбоксом тоже работает при наличии явного buybox_price у оффера и rids
        # тут другой оффер выигрывает, так как без региона цены байбокса не будет
        self.assertFragmentIn(
            self.report.request_json(
                'place=sku_offers&market-sku=1&rgb=blue&yandexuuid=3&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;enable_offline_buybox_price=1'
            ),
            {
                'supplier': {'id': 1},
                'prices': {'value': '1000'},
            },
        )

        # а вот с регионом стратегия срабатывает
        self.assertFragmentIn(
            self.report.request_json(
                'place=sku_offers&rids=213&&market-sku=1&rgb=blue&yandexuuid=3&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;enable_offline_buybox_price=1'
            ),
            {
                'supplier': {'id': 7},
                'prices': {'value': '1000'},
            },
        )

    def test_check_prices_in_buybox_battle(self):
        self.assertFragmentIn(
            self.report.request_json(
                'place=check_prices&feed_shoffer_id=7-4004001&rearr-factors=enable_offline_buybox_price=0'
            ),
            {
                'price': {'value': '1000'},
            },
        )
        # c оффлайн байбоксом без указания региона, цена байбокса не определяется
        self.assertFragmentIn(
            self.report.request_json(
                'place=check_prices&feed_shoffer_id=7-4004001&rearr-factors=enable_offline_buybox_price=1'
            ),
            {
                'price': {'value': '1100'},
            },
        )

        # c rids=213 цена падает до наименьшей по Москве
        self.assertFragmentIn(
            self.report.request_json(
                'place=check_prices&rids=213&feed_shoffer_id=7-4004001&rearr-factors=enable_offline_buybox_price=1'
            ),
            {
                'price': {'value': '1000'},
            },
        )

        self.assertFragmentIn(
            self.report.request_json('place=check_prices&feed_shoffer_id=5-5005001'),
            {
                'price': {'value': '1050'},
            },
        )

    def test_buybox_battle_ref_min_strategy_win(self):
        """
        Битву за buybox выигрывает оффер со стратегией снижать цену до мин рефа,
        при этом цена на оффер со стратегией попадать в buybox НЕ снижается
        """
        self.assertFragmentIn(
            self.report.request_json('place=sku_offers&market-sku=8&rgb=blue&yandexuuid=1'),
            {
                'supplier': {'id': 6},
                'prices': {'value': '950'},
            },
        )

    def test_buybox_battle_no_strategy_win(self):
        """
        Битву за buybox выигрывает оффер с изначально самой низкой ценой,
        цена на оффер со стратегией попадать в мин рефе снижается,
        но ее недостаточно чтобы попасть в BB, цена на оффер со стратегией попадания
        в bubox не меняется
        """
        self.assertFragmentIn(
            self.report.request_json('place=sku_offers&market-sku=9&rgb=blue&yandexuuid=1'),
            {
                'supplier': {'id': 1},
                'prices': {'value': '900'},
            },
        )

    def test_offerinfo_with_offer_id(self):
        """Проверяем подмену цены в place=offerinfo"""
        # оффер который может сбросить цену до минимальной референсной
        for x in ('&market-sku=4', '&market-sku=', '', '&show-urls=cpa,external,beruOrder'):
            for id in (
                'offerid=Sku4Offer5-IiLVm1Goleg',
                'feed_shoffer_id=5-5005005',
            ):  # различные варианты запросить оффер
                response = self.report.request_json('place=offerinfo&rids=0&regset=2&{}'.format(id) + x)
                self.assertFragmentIn(
                    response,
                    {
                        'supplier': {'id': 5},
                        'prices': {'value': '1400'},
                    },
                )


if __name__ == '__main__':
    main()

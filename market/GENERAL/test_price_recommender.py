#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, timedelta

from core.matcher import Absent, Round, EmptyList
from core.types import Currency, Model, Offer, Promo, PromoMSKU, PromoType, ReferenceShop, Shop
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.testcase import TestCase, main
from core.types.demand_prediction import DemandPredictionSales


def CreateBShop(fesh, priority_region=None):
    """Creates and returns Blue shop"""
    return Shop(
        fesh=fesh, datafeed_id=fesh, priority_region=priority_region, name='blue_shop_' + str(fesh), blue='REAL'
    )


class T(TestCase):
    @classmethod
    def add_demand_prediction_sales(cls, source_id, msku_id, sales_amount):
        start_date = datetime(2018, 1, 1)
        for day in range(-2, 30):
            date = start_date + timedelta(days=day)
            cls.index.demand_prediction_sales += [
                DemandPredictionSales(
                    source_id=source_id, msku_id=msku_id, date=date.strftime("%Y-%m-%d"), sales_amount=sales_amount
                )
            ]

    @classmethod
    def add_offers(cls, hyperid, msku, ref_min_price, is_golden_matrix, is_psku, offers):
        """
        Helper method to add Blue and regular offers simultaneously. Each offer is described by
        a dictionary contains next fields: fesh, price, blue, price_history (should be True for Blue offers)
        """
        blueOffers = []
        forbidden_market_mask = Offer.IS_PSKU if is_psku else 0
        for o in offers:
            price = o["price"]
            fesh = o["fesh"]
            price_history = o.get("price_history", None)
            price_reference = o.get("price_reference", None)
            price_limit = o.get("price_limit", None)
            is_resale = o.get("is_resale", False)
            if o.get("blue", False):
                blueOffers.append(
                    BlueOffer(
                        price=price,
                        vat=Vat.VAT_10,
                        offerid='blue_shop_sku_' + str(fesh),
                        feedid=fesh,
                        price_history=price_history,
                        price_reference=price_reference,
                        has_gone=o.get("has_gone", None),
                        price_limit=price_limit,
                        forbidden_market_mask=forbidden_market_mask,
                        is_resale=is_resale,
                    )
                )
            else:
                cls.index.offers.append(Offer(fesh=fesh, price=price, hyperid=hyperid, sku=msku, is_resale=is_resale))

        if blueOffers:
            # shop with fesh = 1 is a virtual Blue shop
            cls.index.mskus.append(
                MarketSku(
                    hyperid=hyperid,
                    sku=str(msku),
                    ref_min_price=ref_min_price,
                    is_golden_matrix=is_golden_matrix,
                    forbidden_market_mask=forbidden_market_mask,
                    blue_offers=blueOffers,
                )
            )

    @classmethod
    def prepare(cls):
        # sales amount predictions (1 sale for each MSKU per day)
        cls.add_demand_prediction_sales(source_id=1, msku_id=1, sales_amount=1.0)
        cls.add_demand_prediction_sales(source_id=1, msku_id=2, sales_amount=1.0)
        cls.add_demand_prediction_sales(source_id=1, msku_id=3, sales_amount=1.0)
        cls.add_demand_prediction_sales(source_id=1, msku_id=5, sales_amount=1.0)
        cls.add_demand_prediction_sales(source_id=1, msku_id=6, sales_amount=1.0)

        cls.index.shops += [
            Shop(
                # virtual Blue shop
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=2),
            CreateBShop(fesh=3),
            CreateBShop(fesh=4),
        ]

        # hyperid's taken from market/report/data/offers_list_prediction.csv
        cls.index.models += [
            Model(hyperid=116335, hid=1),
            Model(hyperid=536797, hid=1),
            Model(hyperid=420298, hid=1),
            Model(
                hyperid=1, hid=2
            ),  # it's important that hyperid=1 is absent in market/library/blue_shows/resource_*_data.csv
            Model(hyperid=12345, hid=1),
            Model(hyperid=54321, hid=1),
        ]

        cls.index.reference_shops += [
            ReferenceShop(hid=1, fesh=5),
        ]

        cls.add_offers(
            hyperid=116335,
            msku=1,
            ref_min_price=3,
            is_golden_matrix=True,
            is_psku=False,
            offers=[dict(price=5, fesh=3, blue=True), dict(price=50, fesh=4, blue=True), dict(price=2, fesh=2)],
        )
        cls.add_offers(
            hyperid=536797,
            msku=2,
            ref_min_price=52,
            is_golden_matrix=False,
            is_psku=True,
            offers=[dict(price=55, fesh=3, blue=True), dict(price=50, fesh=4, blue=True)],
        )
        cls.add_offers(
            hyperid=420298,
            msku=3,
            ref_min_price=None,
            is_golden_matrix=False,
            is_psku=False,
            offers=[dict(price=55, fesh=3, blue=True), dict(price=3, fesh=2), dict(price=10, fesh=5)],
        )
        cls.add_offers(
            hyperid=1,
            msku=5,
            ref_min_price=None,
            is_golden_matrix=True,
            is_psku=False,
            offers=[dict(price=55, fesh=3, blue=True), dict(price=100, fesh=2)],
        )
        cls.add_offers(
            hyperid=None,
            msku=6,
            ref_min_price=None,
            is_golden_matrix=False,
            is_psku=False,
            offers=[dict(price=40, fesh=3, blue=True)],
        )
        cls.add_offers(
            hyperid=None,
            msku=8,
            ref_min_price=38,
            is_golden_matrix=False,
            is_psku=False,
            offers=[dict(price=40, fesh=3, blue=True, price_history=100.123)],
        )
        cls.add_offers(
            hyperid=None,
            msku=9,
            ref_min_price=None,
            is_golden_matrix=True,
            is_psku=False,
            offers=[dict(price=40, fesh=3, blue=True, price_reference=200.456)],
        )
        cls.add_offers(
            hyperid=None,
            msku=10,
            ref_min_price=None,
            is_golden_matrix=True,
            is_psku=False,
            offers=[dict(price=40, fesh=3, blue=True, price_history=100.123, price_reference=200.456)],
        )
        cls.add_offers(
            hyperid=None,
            msku=11,
            ref_min_price=39,
            is_golden_matrix=True,
            is_psku=False,
            offers=[dict(price=40, fesh=3, blue=True, price_history=300.789, price_reference=200.456)],
        )
        cls.add_offers(
            hyperid=12345,
            msku=12,
            ref_min_price=None,
            is_golden_matrix=False,
            is_psku=False,
            offers=[dict(price=10, fesh=3, blue=True, has_gone=True)],
        )
        cls.add_offers(
            hyperid=None,
            msku=13,
            ref_min_price=None,
            is_golden_matrix=True,
            is_psku=False,
            offers=[dict(price=40, fesh=3, blue=True, price_limit=1000)],
        )
        cls.add_offers(
            hyperid=None,
            msku=14,
            ref_min_price=None,
            is_golden_matrix=False,
            is_psku=True,
            offers=[dict(price=400, fesh=3, blue=True, price_limit=500)],
        )
        cls.add_offers(
            hyperid=None,
            msku=15,
            ref_min_price=None,
            is_golden_matrix=False,
            is_psku=True,
            offers=[dict(price=40, fesh=3, blue=True, price_history=100.456)],
        )
        cls.add_offers(
            hyperid=None,
            msku=16,
            ref_min_price=1222,
            is_golden_matrix=True,
            is_psku=False,
            offers=[dict(price=40, fesh=3, blue=True, price_limit=1000)],
        )
        cls.add_offers(
            hyperid=54321,
            msku=17,
            ref_min_price=None,
            is_golden_matrix=False,
            is_psku=False,
            offers=[
                dict(price=55, fesh=3, blue=True, is_resale=True),
                dict(price=3, fesh=2, is_resale=True),
                dict(price=10, fesh=5),
            ],
        )

        cls.index.mskus.append(
            MarketSku(hyperid=420298, sku=7, ref_min_price=100, is_golden_matrix=False, blue_offers=[])
        )

        cls.index.promos += [
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Promo1',
                start_date=datetime(2018, 1, 5),  # 1515110400
                end_date=datetime(2018, 1, 15),  # 1515974400
                mskus=[
                    PromoMSKU(
                        msku='1',
                        market_promo_price=4,
                        market_old_price=10,
                    ),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Promo2',
                start_date=datetime(2018, 1, 5),  # 1515110400
                end_date=datetime(2018, 1, 15),  # 1515974400
                mskus=[
                    PromoMSKU(
                        msku='2',
                        market_promo_price=3,
                        market_old_price=10,
                    ),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,  # should be skipped as not the nearest promo
                key='Promo2.2',
                start_date=datetime(2018, 1, 15),  # 1515974400
                end_date=datetime(2018, 1, 25),  # 1516838400
                mskus=[
                    PromoMSKU(
                        msku='2',
                        market_promo_price=2,
                        market_old_price=3,
                    ),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Promo3',
                start_date=datetime(2018, 1, 5),  # 1515110400
                end_date=datetime(2018, 1, 15),  # 1515974400
                mskus=[
                    PromoMSKU(
                        msku='6',
                        market_promo_price=5,
                        market_old_price=10,
                    )
                ],
            ),
        ]

    def request(self, msku, supplier_id=None, strict=True):
        req = "place=price_recommender&market-sku={}&start-date=2018-01-01".format(msku)
        if supplier_id is not None:
            req += "&supplier-id={}".format(supplier_id)
        return self.report.request_json(req, strict=strict)

    def test_first_sku(self):
        """
        Three offers: GREEN(2), BLUE(5), BLUE(50)
        RefMinPrice=3, IsGoldenMatrixSKU=true
        """
        response = self.request(msku=1, supplier_id=3)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '1',
                        'isGoldenMatrixSKU': True,
                        'priceRecommendations': [
                            {
                                'tag': 'minPriceMarket',
                                'price': 2,
                                'shows': Round(13.89903924),
                                # 'sales': Round(0.400292322 * 28),
                            },
                            {
                                'tag': 'defaultOffer',
                                'price': 3,
                                'shows': Round(9.281883592),
                                'sales': Round(28.0),
                            },
                            {
                                'tag': 'buybox',
                                'price': 5,
                                'shows': Round(0.04757223853),
                                'sales': Round(28.0),
                            },
                            {
                                'tag': 'promo',
                                'price': 4,
                                'startDate': 1515110400,
                                'endDate': 1515974400,
                            },
                            {
                                'tag': 'bySupplier',
                                'price': 5,
                                'shows': Round(0.04757224023),
                                'sales': Round(28.0),
                            },
                        ],
                    },
                ]
            },
        )

    def test_second_sku(self):
        """
        Two offers: BLUE(50), BLUE(55)
        RefMinPrice=52, IsGoldenMatrixSKU=false
        """
        response = self.request(msku=2, supplier_id=4)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '2',
                        'isGoldenMatrixSKU': False,
                        'priceRecommendations': [
                            {
                                'tag': 'promo',
                                'price': 3,
                                'sales': Round(10.0),
                                'startDate': 1515110400,
                                'endDate': 1515974400,
                            },
                            {'tag': 'bySupplier', 'price': 50, 'shows': Round(0.0299118571), 'sales': Round(28.0)},
                            {
                                'tag': 'minPriceMarket',
                                'price': 50,
                                'shows': Round(0.6241239959),
                                # 'sales': Round(0.01797477053 * 28),
                            },
                            {
                                'tag': 'defaultOffer',
                                'price': 52,
                                'shows': Round(0.6241239905),
                                'sales': Round(28.0),
                            },
                            {
                                'tag': 'buybox',
                                'price': 50,
                                'shows': Round(0.6241239959),
                                'sales': Round(28.0),
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    def test_third_sku(self):
        """
        Three offers: GREEN(3), BLUE(55)
        RefMinPrice=None, IsGoldenMatrixSku=false
        """
        response = self.request(msku=3)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '3',
                        'isGoldenMatrixSKU': False,
                        'priceRecommendations': [
                            {
                                'tag': 'minPriceMarket',
                                'price': 3,
                                'shows': Round(4.192069054),
                            },
                            {
                                'tag': 'buybox',
                                'price': 55,
                                'shows': Round(0.0164280699),
                                'sales': Round(14.0),
                            },
                        ],
                    }
                ]
            },
        )

    def test_empty_shows(self):
        """
        No data for prediction on Default Offer
        """
        response = self.request(msku=5, supplier_id=3)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '5',
                        'isGoldenMatrixSKU': True,
                        'priceRecommendations': [
                            {
                                'tag': "minPriceMarket",
                                'price': 55,
                                'shows': 0,
                            },
                            {
                                'tag': 'buybox',
                                'price': 55,
                                'shows': 0,
                            },
                            {
                                'tag': 'bySupplier',
                                'price': 55,
                                'shows': 0,
                            },
                        ],
                    }
                ]
            },
        )

    def test_supplier(self):
        """
        RefMinPrice=3, IsGoldenMatrixSKU=true
        """
        response = self.request(msku=1, supplier_id=3)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '1',
                        'isGoldenMatrixSKU': True,
                        'priceRecommendations': [
                            {
                                'tag': 'bySupplier',
                                'price': 5,
                                'shows': Round(0.04757224023),
                                'sales': Round(28.0),
                            },
                            {
                                'tag': 'defaultOffer',
                                'price': 3,
                                'shows': Round(9.281883592),
                                'sales': Round(28.0),
                            },
                        ],
                    }
                ]
            },
        )

        response = self.request(msku=1, supplier_id=4)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '1',
                        'isGoldenMatrixSKU': True,
                        'priceRecommendations': [
                            {
                                'tag': 'bySupplier',
                                'price': 50,
                                'shows': Round(0.04757224023),
                                'sales': Round(0.0),
                            },
                            {
                                'tag': 'defaultOffer',
                                'price': 3,
                                'shows': Round(9.281883592),
                                'sales': Round(28.0),
                            },
                        ],
                    }
                ]
            },
        )

    def test_only_promo(self):
        response = self.request(msku=6)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '6',
                        'isGoldenMatrixSKU': False,
                        'priceRecommendations': [
                            {
                                'tag': 'buybox',
                                'price': 40,
                                'sales': Round(14.0),
                                'shows': Absent(),
                            },
                            {'tag': 'promo', 'price': 5, 'startDate': 1515110400, 'endDate': 1515974400},
                        ],
                    }
                ]
            },
        )

    def test_without_blue_offers(self):
        response = self.request(msku=7)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "marketSKU": "7",
                        "priceRecommendations": [
                            {
                                'tag': 'minPriceMarket',
                                'price': 3,
                                'shows': 4.192069054,
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_max_old_price_no_history(self):
        """При отсутствии истории скидка недопустима"""
        response = self.request(msku=6)
        self.assertFragmentNotIn(
            response,
            {
                'tag': 'maxOldPrice',
                'source': 'history',
            },
        )

    def _test_max_old_price(self, msku, price, source, is_golden_matrix):
        response = self.request(msku=msku)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'marketSKU': str(msku),
                        'isGoldenMatrixSKU': is_golden_matrix,
                        'priceRecommendations': [
                            {
                                'tag': 'maxOldPrice',
                                'price': price,
                                'source': source,
                            }
                        ],
                    }
                ]
            },
        )

    def test_max_old_price_by_history(self):
        """Допустимая скидка по истории цен
        dict(price=40,  fesh=3, blue=True, price_history=100.123)
        """
        self._test_max_old_price(8, 100, 'history', is_golden_matrix=False)

    def test_max_old_price_by_dco(self):
        """Допустимая скидка по ДЦО
        dict(price=40,  fesh=3, blue=True, price_reference=200.456)
        """
        self._test_max_old_price(9, 200, 'dco', is_golden_matrix=True)

    def test_max_old_price_by_max_dco(self):
        """Допустимая скидка по максимуму из ДЦО и истории
        dict(price=40,  fesh=3, blue=True, price_history=100.123, price_reference=200.456)
        """
        self._test_max_old_price(10, 200, 'dco', is_golden_matrix=True)

    def test_max_old_price_by_max_history(self):
        """Допустимая скидка по максимуму из истории и ДЦО
        dict(price=40,  fesh=3, blue=True, price_history=300.789, price_reference=200.456)
        """
        self._test_max_old_price(11, 300, 'history', is_golden_matrix=True)

    def test_price_limit(self):
        """Предельная цена MSKU"""
        response = self.request(msku=13)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'marketSKU': "13",
                        'isGoldenMatrixSKU': True,
                        'priceRecommendations': [
                            {
                                'tag': 'priceLimit',
                                'price': 1000,
                            }
                        ],
                    }
                ]
            },
        )

    def test_new_unknown_out_of_stock_msku(self):
        """Check that there are no wrong or invalid data for "new" MSKU that is out-of-stock"""
        response = self.request(msku=12)
        self.assertFragmentIn(
            response,
            {"results": [{"marketSKU": "12", "isGoldenMatrixSKU": False, "priceRecommendations": EmptyList()}]},
            allow_different_len=False,
        )

    def test_psku_price_limit(self):
        """Предельная цена для PSKU"""
        response = self.request(msku=14)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'marketSKU': '14',
                        'isGoldenMatrixSKU': False,
                        'priceRecommendations': [
                            {
                                'tag': 'priceLimit',
                                'price': 500,
                            }
                        ],
                    }
                ]
            },
        )

    def test_psku_discount(self):
        """Историческая цена для PSKU"""
        response = self.request(msku=15)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'marketSKU': '15',
                        'isGoldenMatrixSKU': False,
                        'priceRecommendations': [
                            {
                                'tag': 'maxOldPrice',
                                'price': 100,
                                'source': 'history',
                            }
                        ],
                    }
                ]
            },
        )

    def test_common_min_price(self):
        """Предельная цена для PSKU"""
        response = self.request(msku=16)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'marketSKU': '16',
                        'isGoldenMatrixSKU': True,
                        'priceRecommendations': [
                            {
                                'tag': 'defaultOffer',
                                'price': 1000,
                            }
                        ],
                    }
                ]
            },
        )

    def test_empty_recomendations_for_zero_msku(self):
        response = self.request(msku=0)
        self.assertFragmentIn(response, {'results': []}, allow_different_len=False)

    def test_absence_of_resale_offers(self):
        """
        Three offers: BLUE(55, resale), WHITE(3, resale), WHITE(10, resale)
        As result no buybox (because blue is resale)
        And minPriceMarket is 10, not 3 because offer with price 3 is resale
        """
        response = self.request(msku=17)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '17',
                        'priceRecommendations': [
                            {
                                'tag': 'minPriceMarket',
                                'price': 10,
                            },
                        ],
                    }
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        'marketSKU': '17',
                        'priceRecommendations': [
                            {
                                'tag': 'buybox',
                                'price': 55,
                            },
                        ],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()

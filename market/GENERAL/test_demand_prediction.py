#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime
from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    Offer,
    Promo,
    PromoMSKU,
    PromoType,
    ReferenceShop,
    Region,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.types.demand_prediction import DemandPredictionSales, DemandPredictionOnePInput
from core.matcher import NotEmpty

msku111_offer1 = BlueOffer(
    price=49,
    vat=Vat.VAT_18,
    feedid=3,
    offerid='blue.offer.1.1',
    waremd5='Sku1Price5-IiLVm1Goleg',
    randx=3,
)
msku111_offer2 = BlueOffer(
    price=50,
    vat=Vat.VAT_18,
    feedid=4,
    offerid='blue.offer.1.2',
    waremd5='Sku1Price50-iLVm1Goleg',
    randx=4,
)
msku111_offer3 = BlueOffer(
    price=55,
    vat=Vat.VAT_18,
    feedid=5,
    offerid='blue.offer.2.1',
    waremd5='Sku2Price55-iLVm1Goleg',
    randx=5,
)
msku111111111111111_offer1 = BlueOffer(
    price=56,
    vat=Vat.VAT_18,
    feedid=5,
    offerid='blue.offer.2.1',
    waremd5='Sku2Price56-iLVm1Goleg',
    randx=6,
)

blue_shop_1 = Shop(
    fesh=223,
    datafeed_id=3,
    priority_region=2,
    name='blue_shop_1',
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.FIRST_PARTY,
    blue='REAL',
)
blue_shop_2 = Shop(
    fesh=224,
    datafeed_id=4,
    priority_region=2,
    name='blue_shop_2',
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue='REAL',
)
blue_shop_3 = Shop(
    fesh=225,
    datafeed_id=5,
    priority_region=2,
    name='blue_shop_3',
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue='REAL',
)


class T(TestCase):
    @classmethod
    def prepare_demand_prediction(cls):
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops += [
            Shop(
                # Виртуальный магазин синего маркета
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
            Shop(fesh=1001, priority_region=213),
            Shop(fesh=1002, priority_region=213),
            Shop(fesh=1003, priority_region=213),
            Shop(fesh=1004, priority_region=213),
            blue_shop_1,
            blue_shop_2,
            blue_shop_3,
        ]

        cls.index.reference_shops += [
            ReferenceShop(hid=100, fesh=1001),
            ReferenceShop(hid=100, fesh=1004),
        ]

        cls.index.models += [
            Model(hyperid=1000, hid=100),
        ]

        cls.index.offers += [
            Offer(title='offer1 1', fesh=1001, hid=100, hyperid=1000, price=51, bid=40, sku=111),
            Offer(title='offer1 2', fesh=1002, hid=100, hyperid=1000, price=52, bid=30, sku=111),
            Offer(title='offer1 3', fesh=1003, hid=100, hyperid=1000, price=48, bid=30, sku=111),
            Offer(title='offer1 3', fesh=1004, hid=100, hyperid=1000, price=47, bid=30),
        ]

        cls.index.mskus += [
            MarketSku(
                title="msku 111",
                hyperid=1000,
                sku=111,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[msku111_offer1, msku111_offer2, msku111_offer3],
                randx=1,
                ref_min_price=50,
            ),
            MarketSku(
                title="msku 111111111111111",
                hyperid=1,
                sku=111111111111111,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[msku111111111111111_offer1],
                randx=2,
            ),
        ]

        cls.add_demand_prediction_sales(1, 111, 123.4)
        cls.add_demand_prediction_sales(2, 111, 345.4)
        cls.add_demand_prediction_sales(3, 111, 678.4)

        cls.index.promos += [
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Promo1',
                start_date=datetime.datetime(2018, 1, 15),
                end_date=datetime.datetime(2030, 6, 30),
                mskus=[
                    PromoMSKU(
                        msku='111',
                        market_promo_price=40,
                        market_old_price=50,
                    ),
                ],
            ),
        ]

        cls.index.demand_prediction_one_p_input += [
            DemandPredictionOnePInput(111, "2018-01-01", 100, 100),
            DemandPredictionOnePInput(111, "2018-01-05", 100, 30),
            DemandPredictionOnePInput(111, "2018-01-06", 100, 100),
            DemandPredictionOnePInput(111, "2018-01-20", 100, 50),
            DemandPredictionOnePInput(111, "2018-01-25", 100, 100),
        ]

    @classmethod
    def add_demand_prediction_sales(cls, source_id, msku_id, sales_amount):
        start_date = datetime.date(2018, 1, 1)
        for day in range(-2, 30):
            date = start_date + datetime.timedelta(days=day)
            cls.index.demand_prediction_sales += [
                DemandPredictionSales(
                    source_id=source_id, msku_id=msku_id, date=date.strftime("%Y-%m-%d"), sales_amount=sales_amount
                )
            ]

    def get_predictions_check_data(self, source_id, price_name, price, sales_amount=None, period=list(range(28))):
        start_date = datetime.date(2018, 1, 1)
        results = {}
        for day in period:
            date = start_date + datetime.timedelta(days=day)
            day_result = {}
            results[date.strftime("%Y-%m-%d")] = day_result
            price_result = {"price": price, "sources": {str(source_id): sales_amount}}
            day_result[price_name] = price_result
        return {"results": results}

    def test_demand_prediction(self):
        '''
        Запрашиваем плейс demand_prediction

        Проверяем, что прогноз соответствует логике прогнозатора. Точные значения взяты из фактической выдачи, и
        проверяются для того, чтобы зафиксировать результаты работы логики, и видеть влияющие на неё изменения. То
        есть для регрессионного тестирования. Проверки по смыслу делаются в юнит-тестах.
        '''
        response = self.report.request_json(
            'place=demand_forecast&market-sku=111&supplier-id=225&start-date=2018-01-01'
        )
        self.assertFragmentIn(response, self.get_predictions_check_data(1, "currentPrice", 55, 5.221020872e-14))
        self.assertFragmentIn(response, self.get_predictions_check_data(2, "currentPrice", 55, 1.464432684e-13))
        self.assertFragmentIn(response, self.get_predictions_check_data(3, "currentPrice", 55, 2.877928578e-13))
        self.assertFragmentIn(response, self.get_predictions_check_data(1, "defaultOfferPrice", 49, 61.41577844))
        self.assertFragmentIn(response, self.get_predictions_check_data(2, "defaultOfferPrice", 49, 172.2637688))
        self.assertFragmentIn(response, self.get_predictions_check_data(3, "defaultOfferPrice", 49, 338.5357543))
        self.assertFragmentIn(response, self.get_predictions_check_data(1, "minRefPrice", 50, 0.3355078393))
        self.assertFragmentIn(response, self.get_predictions_check_data(2, "minRefPrice", 50, 0.9410585736))
        self.assertFragmentIn(response, self.get_predictions_check_data(3, "minRefPrice", 50, 1.849384675))
        self.assertFragmentIn(response, self.get_predictions_check_data(1, "minimumPrice", 49, 61.41577844))
        self.assertFragmentIn(response, self.get_predictions_check_data(2, "minimumPrice", 49, 172.2637688))
        self.assertFragmentIn(response, self.get_predictions_check_data(3, "minimumPrice", 49, 338.5357543))
        self.assertFragmentIn(response, self.get_predictions_check_data(1, "promoPrice", 40, 123, list(range(14, 28))))
        self.assertFragmentIn(response, self.get_predictions_check_data(2, "promoPrice", 40, 345, list(range(14, 28))))
        self.assertFragmentIn(response, self.get_predictions_check_data(3, "promoPrice", 40, 678, list(range(14, 28))))

    def test_demand_prediction_first_party(self):
        '''
        Запрашиваем плейс demand_prediction c supplier-id first party поставщика.

        Проверяем, что текущая и скидочная цены указаны правильно.
        '''
        response = self.report.request_json(
            'place=demand_forecast&market-sku=111&supplier-id=223&start-date=2018-01-01'
        )
        self.assertFragmentIn(response, self.get_predictions_check_data(1, "currentPrice", 100, NotEmpty()))
        self.assertFragmentIn(
            response, self.get_predictions_check_data(1, "promoPrice", 30, NotEmpty(), list(range(4, 5)))
        )
        self.assertFragmentIn(
            response, self.get_predictions_check_data(1, "promoPrice", 50, NotEmpty(), list(range(19, 24)))
        )

    def test_demand_prediction_big_msku_id(self):
        '''
        Запрашиваем плейс demand_prediction

        Проверяем, что прогноз соответствует логике.
        '''
        response = self.report.request_json(
            'place=demand_forecast&market-sku=111111111111111&supplier-id=225&start-date=2018-01-01'
        )
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "2018-01-01": {
                        "currentPrice": {
                            "price": 56,
                        },
                    }
                }
            },
        )

    def test_prediction_duration(self):
        """Проверяем длительность прогноза, 56 дней по-умолчанию"""

        def make_expectation(duration):
            start_date = datetime.date(2018, 1, 1)
            results = {}
            for day in range(duration):
                date = start_date + datetime.timedelta(days=day)
                results[date.strftime("%Y-%m-%d")] = NotEmpty()
            return {"results": results}

        request = "place=demand_forecast&market-sku=111&supplier-id=225&start-date=2018-01-01"
        self.assertFragmentIn(self.report.request_json(request), make_expectation(56), allow_different_len=False)
        self.assertFragmentIn(
            self.report.request_json(request + "&duration=28"), make_expectation(28), allow_different_len=False
        )
        self.assertFragmentIn(
            self.report.request_json(request + "&duration=90"), make_expectation(90), allow_different_len=False
        )

        # request with too big duration - requests with duration >90 should fail
        response = self.report.request_plain(request + "&duration=91", strict=False)
        self.assertNotEqual(response.code, 200)
        self.error_log.expect(message="Too big demand prediction duration", code=1010)


if __name__ == '__main__':
    main()

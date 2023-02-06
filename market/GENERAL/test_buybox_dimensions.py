#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Currency, GpsCoord, Model, Offer, Outlet, OutletDeliveryOption, Shop, Tax, Vat
from core.types.sku import MarketSku, BlueOffer
from core.types.offer import OfferDimensions


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                delivery_service_outlets=[2001],
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        sku1_offer1 = BlueOffer(
            price=5,
            price_old=8,
            vat=Vat.VAT_10,
            feedid=3,
            offerid='blue.offer.1.1',
            waremd5='Sku1Price5-IiLVm1Goleg',
            weight=5,
            dimensions=OfferDimensions(length=20, width=30, height=10),
        )
        sku1_offer2 = BlueOffer(
            price=50,
            vat=Vat.VAT_0,
            feedid=4,
            offerid='blue.offer.1.2',
            waremd5='Sku1Price50-iLVm1Goleg',
            weight=50,
            dimensions=OfferDimensions(length=200, width=300, height=100),
            blue_weight=51,
            blue_dimensions=OfferDimensions(length=201, width=301, height=101),
        )

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[sku1_offer1],
                randx=1,
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=1,
                sku=2,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[sku1_offer2],
                randx=2,
            ),
        ]

        cls.index.shops += [Shop(fesh=1001, priority_region=213)]

        cls.index.models += [
            Model(hyperid=7001),
        ]

        cls.index.offers += [
            Offer(
                hyperid=7001,
                fesh=1001,
                price=100,
                waremd5='whitewhiteoffer111111w',
                weight=5,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                blue_weight=11,
                blue_dimensions=OfferDimensions(length=51, width=151, height=501),
            ),
        ]

    def test_dimensions_in_buybox(self):
        """
        Проверяем протаскивание ВГХ в выдачу
        """
        response = self.report.request_json(
            'place=productoffers&market-sku=2&regset=213&rids=213&pp=6&offers-set=defaultList&yandexuid=3&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_with_delivery_context=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'wareId': 'Sku1Price50-iLVm1Goleg',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'Sku1Price50-iLVm1Goleg',
                                'Dimensions': {'Weight': 51, 'Length': 201, 'Width': 301, 'Height': 101},
                            }
                        ]
                    }
                },
            },
        )


if __name__ == '__main__':
    main()

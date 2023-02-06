#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DeliveryOption, Offer, Region, Shop, DynamicWarehouseInfo, DynamicWarehouseToWarehouseInfo
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.matcher import NoKey


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"
GET_RID_OF_DIRECT_SHIPPING = "&rearr-factors=get_rid_of_direct_shipping=1"


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=13,
                datafeed_id=13,
                priority_region=213,
                name='virtual_shop',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='1P supplier',
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                is_supplier=True,
                fulfillment_program=True,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='3P supplier',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                is_supplier=True,
                fulfillment_program=True,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                name='Dropship',
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                is_supplier=True,
                fulfillment_program=False,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                name='DropshipViaSC',
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                is_supplier=True,
                fulfillment_program=False,
                direct_shipping=False,
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                name="Click&Collect",
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                is_supplier=True,
                fulfillment_program=False,
                ignore_stocks=True,
            ),
            Shop(
                fesh=7,
                datafeed_id=7,
                name="DSBS",
                priority_region=213,
                blue=Shop.BLUE_NO,
                is_supplier=True,
                fulfillment_program=False,
                ignore_stocks=True,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='blue market sku1',
                sku=1,
                blue_offers=[
                    BlueOffer(title='1p offer', price=100, feedid=1, weight=10, waremd5='TestOffer_1P_________g')
                ],
            ),
            MarketSku(
                hyperid=1,
                title='blue market sku2',
                sku=2,
                blue_offers=[
                    BlueOffer(title='3p offer', price=100, feedid=2, weight=10, waremd5='TestOffer_3P_________g')
                ],
            ),
            MarketSku(
                hyperid=1,
                title='blue market sku4',
                sku=4,
                blue_offers=[BlueOffer(title='Dropship', price=100, feedid=4, waremd5='TestOffer_Dropship___g')],
            ),
            MarketSku(
                hyperid=1,
                title='blue market sku5',
                sku=5,
                blue_offers=[BlueOffer(title='DropshipViaSC', price=100, feedid=5, waremd5='TestOffer_DropshipSC_g')],
            ),
            MarketSku(
                hyperid=1,
                title='blue market sku6',
                sku=6,
                blue_offers=[
                    BlueOffer(title='Click&Collect', price=100, feedid=6, waremd5='TestOffer_ClickColle_g'),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=7,
                hyperid=1,
                waremd5='TestOffer_DSBS_______g',
                price=100,
                pickup_option=DeliveryOption(price=350, day_from=1, day_to=7, order_before=10),
                cpa=Offer.CPA_REAL,
                title="DSBS offer",
            ),
        ]

    def test_1p(self):
        """Факторы 1p поставщика"""
        response = self.report.request_json(
            'place=prime&place=prime&text={}&debug=da&pp=18&rearr-factors=market_metadoc_search=no'.format(
                '1P supplier'
            )
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'TestOffer_1P_________g',
                    'factors': {
                        'IS_FIRST_PARTY_SUPPLIER': '1',
                        'IS_THIRD_PARTY_SUPPLIER': NoKey('IS_THIRD_PARTY_SUPPLIER'),
                        'IS_FULFILLMENT_PROGRAM': '1',
                        'IS_CROSSDOCK': NoKey('IS_CROSSDOCK'),
                        'IS_DROPSHIP': NoKey('IS_DROPSHIP'),
                        'IS_DROPSHIP_VIA_SC': NoKey('IS_DROPSHIP_VIA_SC'),
                        'IS_CLICK_N_COLLECT': NoKey('IS_CLICK_N_COLLECT'),
                    },
                }
            },
        )

    def test_3p(self):
        """Факторы 3p поставщика"""
        response = self.report.request_json(
            'place=prime&place=prime&text={}&debug=da&pp=18&rearr-factors=market_metadoc_search=no'.format(
                '3P supplier'
            )
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'TestOffer_3P_________g',
                    'factors': {
                        'IS_FIRST_PARTY_SUPPLIER': NoKey('IS_FIRST_PARTY_SUPPLIER'),
                        'IS_THIRD_PARTY_SUPPLIER': '1',
                        'IS_FULFILLMENT_PROGRAM': '1',
                        'IS_CROSSDOCK': NoKey('IS_CROSSDOCK'),
                        'IS_DROPSHIP': NoKey('IS_DROPSHIP'),
                        'IS_DROPSHIP_VIA_SC': NoKey('IS_DROPSHIP_VIA_SC'),
                        'IS_CLICK_N_COLLECT': NoKey('IS_CLICK_N_COLLECT'),
                    },
                }
            },
        )

    def test_dropship(self):
        """Факторы dropship поставщика"""
        response = self.report.request_json(
            'place=prime&place=prime&text={}&debug=da&pp=18&rearr-factors=market_metadoc_search=no'.format('dropship')
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'TestOffer_Dropship___g',
                    'factors': {
                        'IS_FIRST_PARTY_SUPPLIER': NoKey('IS_FIRST_PARTY_SUPPLIER'),
                        'IS_THIRD_PARTY_SUPPLIER': '1',
                        'IS_FULFILLMENT_PROGRAM': NoKey('IS_FULFILLMENT_PROGRAM'),
                        'IS_CROSSDOCK': NoKey('IS_CROSSDOCK'),
                        'IS_DROPSHIP': '1',
                        'IS_DROPSHIP_VIA_SC': NoKey('IS_DROPSHIP_VIA_SC'),
                        'IS_CLICK_N_COLLECT': NoKey('IS_CLICK_N_COLLECT'),
                    },
                }
            },
        )

    def test_dropship_via_sc(self):
        """Факторы dropshipViaSC поставщика"""
        response = self.report.request_json(
            'place=prime&place=prime&text={}&debug=da&pp=18&rearr-factors=market_metadoc_search=no'.format(
                'dropshipViaSC'
            )
            + GET_RID_OF_DIRECT_SHIPPING
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'TestOffer_DropshipSC_g',
                    'factors': {
                        'IS_FIRST_PARTY_SUPPLIER': NoKey('IS_FIRST_PARTY_SUPPLIER'),
                        'IS_THIRD_PARTY_SUPPLIER': '1',
                        'IS_FULFILLMENT_PROGRAM': NoKey('IS_FULFILLMENT_PROGRAM'),
                        'IS_CROSSDOCK': NoKey('IS_CROSSDOCK'),
                        'IS_DROPSHIP': NoKey('IS_DROPSHIP'),
                        'IS_DROPSHIP_VIA_SC': '1',
                        'IS_CLICK_N_COLLECT': NoKey('IS_CLICK_N_COLLECT'),
                    },
                }
            },
        )

    def test_click_n_collect(self):
        """Факторы dropshipViaSC поставщика"""
        response = self.report.request_json(
            'place=prime&place=prime&text={}&debug=da&pp=18&rearr-factors=market_metadoc_search=no'.format('click')
            + GET_RID_OF_DIRECT_SHIPPING
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'TestOffer_ClickColle_g',
                    'factors': {
                        'IS_FIRST_PARTY_SUPPLIER': NoKey('IS_FIRST_PARTY_SUPPLIER'),
                        'IS_THIRD_PARTY_SUPPLIER': '1',
                        'IS_FULFILLMENT_PROGRAM': NoKey('IS_FULFILLMENT_PROGRAM'),
                        'IS_CROSSDOCK': NoKey('IS_CROSSDOCK'),
                        'IS_DROPSHIP': NoKey('IS_DROPSHIP'),
                        'IS_DROPSHIP_VIA_SC': NoKey('IS_DROPSHIP_VIA_SC'),
                        'IS_CLICK_N_COLLECT': '1',
                    },
                }
            },
        )

    def test_dsbs(self):
        """Проверяем, факторы ДСБС + что ДСБС с ignore_stocks=True не определяется как click&collect"""
        response = self.report.request_json(
            'place=prime&place=prime&text={}&debug=da&pp=18'.format('DSBS') + GET_RID_OF_DIRECT_SHIPPING
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'wareId': 'TestOffer_DSBS_______g',
                    'factors': {
                        'IS_FIRST_PARTY_SUPPLIER': NoKey('IS_FIRST_PARTY_SUPPLIER'),
                        'IS_THIRD_PARTY_SUPPLIER': NoKey('IS_THIRD_PARTY_SUPPLIER'),
                        'IS_FULFILLMENT_PROGRAM': NoKey('IS_FULFILLMENT_PROGRAM'),
                        'IS_CROSSDOCK': NoKey('IS_CROSSDOCK'),
                        'IS_DROPSHIP': NoKey('IS_DROPSHIP'),
                        'IS_DROPSHIP_VIA_SC': NoKey('IS_DROPSHIP_VIA_SC'),
                        'IS_CLICK_N_COLLECT': NoKey('IS_CLICK_N_COLLECT'),
                    },
                }
            },
        )


if __name__ == '__main__':
    main()

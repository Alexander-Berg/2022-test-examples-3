#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import os
from core.testcase import TestCase, main
from core.regional_prices import RegionalPrice, RegionalPrices

from core.types import Model, Shop, MarketSku, BlueOffer, Region
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare_report_regional_pricing_from_access(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_regional_prices_by_access = True

        region_tree = [
            Region(
                rid=1,
                name='РФ',
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=43, name='Казань'),
                ],
            ),
        ]
        cls.index.regiontree += region_tree

        cls.index.shops += [
            Shop(
                fesh=888,
                datafeed_id=888,
                priority_region=213,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='blue market sku1p',
                hyperid=112,
                sku=11200001,
                waremd5='MarketSku1-IiLVm1goleg',
                enable_auto_discounts=True,
                blue_offers=[
                    BlueOffer(
                        waremd5='Blue1POffer-1-MT-ACC-w',
                        price=1000,
                        feedid=888,
                        offerid='shop_sku_2',
                    ),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=112, hid=112, title='blue model 1'),
        ]

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        access_server.create_publisher(name='mbi')
        access_server.create_resource(name='market_regional_pricing', publisher_name='mbi')

        market_regional_pricing_v1_path = os.path.join(
            cls.meta_paths.access_resources, 'market_regional_pricing/1.0.0/market_regional_pricing.bin'
        )

        mds_market_regional_pricing_v1_url = cls._get_mds_url(shade_host_port, market_regional_pricing_v1_path)

        regional_prices = [
            RegionalPrice(msku=11200001, ssku='shop_sku_2', price=665, region_id=213),
        ]
        regional_prices = RegionalPrices(
            regional_prices=regional_prices, dst_path=cls.meta_paths.access_resources_tmp, table_name="test_table"
        )
        regional_prices.save(market_regional_pricing_v1_path)

        access_server.create_version('market_regional_pricing', http_url=mds_market_regional_pricing_v1_url)

    @staticmethod
    def _get_mds_url(shade_host_port, path):
        path = path if path.startswith('/') else '/' + path
        return '{host_port}/mds{path}'.format(
            host_port=shade_host_port,
            path=path,
        )

    def test_regional_prices_offerinfo(self):
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=888-shop_sku_2&rids=213&regset=2&rearr-factors=market_enable_regional_pricing=0"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "1000",
                    "priceSource": Absent(),
                    "priceSourceTableName": Absent(),
                },
            },
        )

        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=888-shop_sku_2&rids=213&regset=2&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "665",
                    "priceSource": "regional",
                    "priceSourceTableName": "test_table",
                },
            },
        )

        # for all regsets regional pricing works (regset = 0)
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=888-shop_sku_2&rids=213&regset=0&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "665",
                    "priceSource": "regional",
                    "priceSourceTableName": "test_table",
                },
            },
        )

        # for all regsets regional pricing works (regset=1)
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=888-shop_sku_2&rids=213&regset=1&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "665",
                    "priceSource": "regional",
                    "priceSourceTableName": "test_table",
                },
            },
        )

        # there is no Kazan in regional pricing table
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=888-shop_sku_2&rids=43&regset=1&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "1000",
                    "priceSource": Absent(),
                    "priceSourceTableName": Absent(),
                },
            },
        )

    def test_regional_prices_productoffers(self):
        response = self.report.request_json(
            "place=productoffers&market-sku=11200001&pp=18&rids=213&rearr-factors=market_enable_regional_pricing=0"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "1000",
                    "priceSource": Absent(),
                    "priceSourceTableName": Absent(),
                },
            },
        )

        response = self.report.request_json(
            "place=productoffers&market-sku=11200001&pp=18&rids=213&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "665",
                    "priceSource": "regional",
                    "priceSourceTableName": "test_table",
                },
            },
        )

        # without rids no regional pricing
        response = self.report.request_json(
            "place=productoffers&market-sku=11200001&pp=18&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "1000",
                    "priceSource": Absent(),
                    "priceSourceTableName": Absent(),
                },
            },
        )

    def test_regional_prices_sku_offers(self):
        response = self.report.request_json(
            "place=sku_offers&market-sku=11200001&rids=213&rearr-factors=market_enable_regional_pricing=0"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "1000",
                    "priceSource": Absent(),
                    "priceSourceTableName": Absent(),
                },
            },
        )

        response = self.report.request_json(
            "place=sku_offers&market-sku=11200001&rids=213&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "665",
                    "priceSource": "regional",
                    "priceSourceTableName": "test_table",
                },
            },
        )

        # without rids no regional pricing
        response = self.report.request_json(
            "place=sku_offers&market-sku=11200001&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "1000",
                    "priceSource": Absent(),
                    "priceSourceTableName": Absent(),
                },
            },
        )

    def test_regional_prices_prime(self):
        response = self.report.request_json(
            "place=prime&text='blue market sku1p'&rids=213&pp=18&rearr-factors=market_enable_regional_pricing=0"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "1000",
                    "priceSource": Absent(),
                    "priceSourceTableName": Absent(),
                },
            },
        )

        response = self.report.request_json(
            "place=prime&text='blue market sku1p'&rids=213&pp=18&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "665",
                    "priceSource": "regional",
                    "priceSourceTableName": "test_table",
                },
            },
        )

        # without rids no regional pricing
        response = self.report.request_json(
            "place=prime&text='blue market sku1p'&pp=18&rearr-factors=market_enable_regional_pricing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "Blue1POffer-1-MT-ACC-w",
                "prices": {
                    "value": "1000",
                    "priceSource": Absent(),
                    "priceSourceTableName": Absent(),
                },
            },
        )


if __name__ == '__main__':
    main()

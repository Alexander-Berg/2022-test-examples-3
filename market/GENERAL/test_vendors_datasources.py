#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import os
from core.testcase import TestCase, main
from core.vendors_datasources import VendorsDatasourceId, VendorsDatasources

from core.types import (
    ClickType,
    Model,
    Offer,
    Shop,
)
from core.matcher import Absent

CREATION_DATE = '1970-01-01T00:02:03'
CREATION_TIME = 123  # timestamp of CREATION_DATE


class T(TestCase):
    @classmethod
    def prepare_report_vendor_datasources_from_access(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_vendors_datasource_ids_for_search = True
        cls.settings.market_access_settings.use_access_vendors_datasource_ids_for_search = True

        cls.index.models += [
            Model(hyperid=271, hid=571, vbid=0, vendor_id=371, datasource_id=0),
            Model(hyperid=272, hid=571, vbid=100, vendor_id=372, datasource_id=472),
            Model(hyperid=273, hid=571, vbid=100, vendor_id=373, datasource_id=473),
            Model(hyperid=274, hid=571, vbid=0, vendor_id=374, datasource_id=0),
        ]
        cls.index.shops += [
            Shop(fesh=671, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=672, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=673, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=674, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(
                hid=571, hyperid=271, vendor_id=371, price=10000, fee=100, cpa=Offer.CPA_REAL, fesh=671, datasource_id=0
            ),
            Offer(
                hid=571,
                hyperid=272,
                vendor_id=372,
                price=10000,
                fee=100,
                cpa=Offer.CPA_REAL,
                fesh=672,
                datasource_id=472,
            ),
            Offer(
                hid=571,
                hyperid=273,
                vendor_id=373,
                price=10000,
                fee=100,
                cpa=Offer.CPA_REAL,
                fesh=673,
                datasource_id=473,
            ),
            Offer(
                hid=571, hyperid=274, vendor_id=374, price=10000, fee=100, cpa=Offer.CPA_REAL, fesh=674, datasource_id=0
            ),
        ]

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        access_server.create_publisher(name='mbi')
        access_server.create_resource(name='market_vendors_datasources', publisher_name='mbi')

        market_vendors_datasources_v1_path = os.path.join(
            cls.meta_paths.access_resources, 'market_vendors_datasources/1.0.0/market_vendors_datasources.tar.gz'
        )
        mds_market_vendors_datasources_v1_url = cls._get_mds_url(shade_host_port, market_vendors_datasources_v1_path)
        datasources = [
            VendorsDatasourceId(vendor_id=371, datasource_id=471, hid=571),
            VendorsDatasourceId(vendor_id=372, datasource_id=1472, hid=571),
        ]
        vendors_datasources = VendorsDatasources(
            datasources=datasources,
            src_path=cls.meta_paths.access_resources_tmp,
            dst_path=cls.meta_paths.access_resources_tmp,
            paths=cls.meta_paths,
            creation_time=CREATION_TIME,
        )
        vendors_datasources.save_archive(market_vendors_datasources_v1_path)
        access_server.create_version('market_vendors_datasources', http_url=mds_market_vendors_datasources_v1_url)

    @staticmethod
    def _get_mds_url(shade_host_port, path):
        path = path if path.startswith('/') else '/' + path
        return '{host_port}/mds{path}'.format(
            host_port=shade_host_port,
            path=path,
        )

    # проверяем работу флага market_disable_vendor_datasourceids_for_free_clicks,
    # при выключенном флаге в лог должны просаживаться id из vendors_datasources (для vendor_id=371).
    # при несовпадении dtsrc_id (для vendor_id=372) значение в урле не меняется
    def test_report_vendors_datasources_from_access_prime_ds_disabled(self):
        self.report.request_json(
            'place=prime&pp=18&hid=571&use-default-offers=1&show-urls=cpa,promotion,productVendorBid&debug=da'
            '&rearr-factors=market_disable_vendor_datasourceids_for_free_clicks=1'
        )
        # self.click_log.expect(ClickType.EXTERNAL, pp=18, dtype='modelcard', hyper_cat_id=571, hyper_id=271, vendor_ds_id=0)
        self.click_log.expect(
            ClickType.EXTERNAL, pp=18, dtype='modelcard', hyper_cat_id=571, hyper_id=272, vendor_ds_id=472
        )
        self.click_log.expect(
            ClickType.EXTERNAL, pp=18, dtype='modelcard', hyper_cat_id=571, hyper_id=273, vendor_ds_id=473
        )
        # self.click_log.expect(ClickType.EXTERNAL, pp=18, dtype='modelcard', hyper_cat_id=571, hyper_id=274, vendor_ds_id=0)
        self.click_log.expect(ClickType.CPA, pp=18, hyper_cat_id=571, hyper_id=271, dtsrc_id=Absent())
        self.click_log.expect(ClickType.CPA, pp=18, hyper_cat_id=571, hyper_id=272, dtsrc_id=472)
        self.click_log.expect(ClickType.CPA, pp=18, hyper_cat_id=571, hyper_id=273, dtsrc_id=473)
        self.click_log.expect(ClickType.CPA, pp=18, hyper_cat_id=571, hyper_id=274, dtsrc_id=Absent())

    def test_report_vendors_datasources_from_access_prime_ds_enabled(self):
        self.report.request_json(
            'place=prime&pp=28&hid=571&use-default-offers=1&show-urls=cpa,promotion,productVendorBid&debug=da'
            '&rearr-factors=market_disable_vendor_datasofurceids_for_free_clicks=0'
        )
        # self.click_log.expect(ClickType.EXTERNAL, pp=28, dtype='modelcard', hyper_cat_id=571, hyper_id=271, vendor_ds_id=471)
        self.click_log.expect(
            ClickType.EXTERNAL, pp=28, dtype='modelcard', hyper_cat_id=571, hyper_id=272, vendor_ds_id=472
        )
        self.click_log.expect(
            ClickType.EXTERNAL, pp=28, dtype='modelcard', hyper_cat_id=571, hyper_id=273, vendor_ds_id=473
        )
        # self.click_log.expect(ClickType.EXTERNAL, pp=28, dtype='modelcard', hyper_cat_id=571, hyper_id=274, vendor_ds_id=0)
        self.click_log.expect(ClickType.CPA, pp=28, hyper_cat_id=571, hyper_id=271, dtsrc_id=471)
        self.click_log.expect(ClickType.CPA, pp=28, hyper_cat_id=571, hyper_id=272, dtsrc_id=472)
        self.click_log.expect(ClickType.CPA, pp=28, hyper_cat_id=571, hyper_id=273, dtsrc_id=473)
        self.click_log.expect(ClickType.CPA, pp=28, hyper_cat_id=571, hyper_id=274, dtsrc_id=Absent())

    def test_report_vendors_datasources_from_access_cpa_shop_incut_ds_enabled(self):
        self.report.request_json(
            'place=cpa_shop_incut&pp=38&hid=571&use-default-offers=1&show-urls=cpa,promotion,productVendorBid&debug=da&min-num-doc=1'
            '&rearr-factors=market_disable_vendor_datasourceids_for_free_clicks=0'
        )
        self.click_log.expect(ClickType.PROMOTION, pp=38, hyper_cat_id=571, hyper_id=271, dtsrc_id=471)
        self.click_log.expect(ClickType.PROMOTION, pp=38, hyper_cat_id=571, hyper_id=272, dtsrc_id=1472)
        self.click_log.expect(ClickType.PROMOTION, pp=38, hyper_cat_id=571, hyper_id=273, dtsrc_id=0)
        self.click_log.expect(ClickType.PROMOTION, pp=38, hyper_cat_id=571, hyper_id=274, dtsrc_id=0)


if __name__ == '__main__':
    main()

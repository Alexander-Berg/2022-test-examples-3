#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.dynamic import MarketDynamic
from core.testcase import TestCase, main

import os

CREATION_DATE = '1970-01-01T00:02:03'
CREATION_TIME = 123  # timestamp of CREATION_DATE


class T(TestCase):
    @classmethod
    def prepare_report_market_dynamic_from_access(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_market_dynamic = True
        cls.settings.market_access_settings.use_market_dynamic_from_access_for_search = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        access_server.create_publisher(name='mbi')
        access_server.create_resource(name='market_dynamic', publisher_name='mbi')

        market_dynamic_v1_path = os.path.join(
            cls.meta_paths.access_resources, 'market_dynamic/1.0.0/market_dynamic.tar.gz'
        )
        mds_market_dynamic_v1_url = cls._get_mds_url(shade_host_port, market_dynamic_v1_path)
        dynamic = MarketDynamic(
            src_path=cls.meta_paths.access_resources_tmp,
            dst_path=cls.meta_paths.access_resources_tmp,
            paths=cls.meta_paths,
            creation_time=CREATION_TIME,
        )
        dynamic.save_archive(market_dynamic_v1_path)
        access_server.create_version('market_dynamic', http_url=mds_market_dynamic_v1_url)

    @staticmethod
    def _get_mds_url(shade_host_port, path):
        path = path if path.startswith('/') else '/' + path
        return '{host_port}/mds{path}'.format(
            host_port=shade_host_port,
            path=path,
        )

    def test_report_market_dynamic_from_access(self):
        response = self.report.request_xml('admin_action=versions&aquirestats=1')
        self.assertFragmentIn(response, '<cpcshopfilter timestamp="{}'.format(CREATION_DATE))


if __name__ == '__main__':
    main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import LikeUrl
from core.svn_data import SvnData
from core.types.redirect import RedirectWhiteListRecord


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.disable_random = 1
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_svn_data = True
        cls.settings.market_access_settings.use_svn_data = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        report_data = SvnData(access_server=access_server, shade_host_port=shade_host_port, meta_paths=cls.meta_paths)

        report_data.redirect_whitelist += [
            RedirectWhiteListRecord(query='билеты на поезд', url='/search.xml?text={}'),
            RedirectWhiteListRecord(query='детские наушники', url='/search?text={}&cvredirect=0'),
        ]

        report_data.redirect_whitelist_app += [
            RedirectWhiteListRecord(
                query='детские наушники', url='/product--naushniki-jbl-jr310/858525?sku=3828274930'
            ),
        ]

        report_data.create_version()

    def test_redirect_white_lists_from_access(self):
        """Проверяем редиректы файла redirect-white-list, доставленного с помощью Market Access
        https://st.yandex-team.ru/MARKETOUT-43352
        """
        response = self.report.request_json('place=prime&text=билеты+на+поезд&cvredirect=1')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of('/search.xml?text=билеты%20на%20поезд&was_redir=1&rt=10')}}
        )

        response = self.report.request_json('place=prime&text=детские%20наушники&cvredirect=1')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of('/search?text=детские%20наушники&cvredirect=0')}}
        )

    def test_redirect_white_list_app_from_access(self):
        """Проверяем редиректы файла redirect-white-list-app, доставленного с помощью Market Access
        https://st.yandex-team.ru/MARKETOUT-43352
        """
        response = self.report.request_json('place=prime&text=детские%20наушники&cvredirect=1&client=ANDROID')
        self.assertFragmentIn(
            response, {"redirect": {"url": LikeUrl.of('/product--naushniki-jbl-jr310/858525?sku=3828274930')}}
        )


if __name__ == '__main__':
    main()

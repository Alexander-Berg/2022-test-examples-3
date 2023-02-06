#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.report import ReportSearchType
from core.testcase import TestCase, main
from core.matcher import Contains, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.cloud_service = 'test_report_lite'

    def test_referer_in_log_error_in_search_place(self):
        """Как выглядит логгирование TReportError в TSearchPlace::Log"""
        self.report.request_json(
            "place=recipe_by_glfilters&referer=https%3A%2F%2Fdefault.market-exp.pepelac2ft.yandex.ru%2Fproduct%2F10813494%2Fspec%3Fhid%3D90584"
        )
        self.error_log.expect(
            Contains(
                "Exit with code 1 (INVALID_USER_CGI) because cgi param hid is required",
            ),
            url_hash=NotEmpty(),
            search_type=ReportSearchType.META_ONLY,
            referer="https://default.market-exp.pepelac2ft.yandex.ru/product/10813494/spec?hid=90584",
        )

    def test_referer_from_http_headers_in_log_error_in_search_place(self):
        """Как выглядит логгирование TReportError в TSearchPlace::Log если referer задан в headers запроса"""
        headers = {"Referer": "https://default.market-exp.pepelac2ft.yandex.ru/product/10813494/spec?hid=90584"}
        self.report.request_json("place=recipe_by_glfilters", headers=headers)
        self.error_log.expect(referer="https://default.market-exp.pepelac2ft.yandex.ru/product/10813494/spec?hid=90584")

    def test_referer_in_log_trace_err(self):
        """Как выглядит логгирование при TRACE_ERR"""

        self.report.request_json(
            "place=prime&text=text&rs=invalid@rs&referer=https%3A%2F%2Fdefault.market-exp.pepelac2ft.yandex.ru%2Fproduct%2F10813494%2Fspec%3Fhid%3D90584"
        )
        self.error_log.expect(
            Contains(
                "can not decode report state from",
            ),
            referer="https://default.market-exp.pepelac2ft.yandex.ru/product/10813494/spec?hid=90584",
            url_hash=NotEmpty(),
        )

    def test_referer_from_http_headers_in_log_trace_err(self):
        """Как выглядит логгирование при TRACE_ERR"""

        headers = {"Referer": "https://default.market-exp.pepelac2ft.yandex.ru/product/10813494/spec?hid=90584"}
        self.report.request_json("place=prime&text=text&rs=invalid@rs", headers=headers)
        self.error_log.expect(
            Contains(
                "can not decode report state from",
            ),
            referer="https://default.market-exp.pepelac2ft.yandex.ru/product/10813494/spec?hid=90584",
            url_hash=NotEmpty(),
        )

    def test_referer_truncation(self):
        self.report.request_json("place=prime&text=text&rs=invalid@rs&referer=" + "Q" * 10000)
        self.error_log.expect(referer="Q" * 4000, cloud_service='test_report_lite')


if __name__ == '__main__':
    main()

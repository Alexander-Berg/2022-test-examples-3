# -*- coding: utf-8 -*-

import os
import pytest
import json
import urlparse

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestUTF8(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("url"), [
        "/search/ads?text=%D0%BE%D0%BA%D0%BD%D0%B0&lr=213&export=json",
        "/search/direct?text=%D0%BE%D0%BA%D0%BD%D0%B0&lr=213&export=json",
        "/search/?text=%D0%BC%D0%B0%D0%B4%D0%BE%D0%BD%D0%BD%D0%B0&export=json",
        "/blogs/search?text=%D0%BA%D0%BE%D1%82%D0%B8%D0%BA%D0%B8%20facebook&export=json",
        "/blogs/search?text=journal:tema+author:tema&export=json",
        "/search/pre/?callback=a&ajax=%7B%22advanced-search%22%3A%7B%7D%7D&yu=" + YANDEXUID,
        "/yandsearch/pre/?callback=a&ajax=%7B%22advanced-search%22%3A%7B%7D%7D&yu=" + YANDEXUID,
    ])
    def test_unicode_export(self, query, url):
        query.set_yandexuid(YANDEXUID)
        query.set_url(url)
        self.request(query)


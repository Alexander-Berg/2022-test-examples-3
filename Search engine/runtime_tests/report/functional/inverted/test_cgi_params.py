# -*- coding: utf-8 -*-

import pytest
import zlib
import json
import os

from report.const import *
from report.proto import meta_pb2
from report.functional.web.base import BaseFuncTest


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestCgiParams(BaseFuncTest):

    @pytest.mark.skipif(os.environ.get('BETA_HOST') is not None, reason="Этот тест работает только при прямом хождении в апач")
    @pytest.mark.parametrize("srcrwr, host", [
        ('', None),
        ('HTTP_ADAPTER:sas1-0555.search.yandex.ru:13:500', 'sas1-0555.search.yandex.ru:13'),
        ('HTTP_ADAPTER3:sas1-0555.search.yandex.ru:13:500', None),
        ('HTTP_ADAPTER:127.0.0.1:13:500', '127.0.0.1:13'),
        ('HTTP_ADAPTER%3a127.0.0.1%3A13:500', None),
        ('HTTP_ADAPTER%3A[::1]%3a13:500', None),
        ('HTTP_ADAPTER:[::1]:13:500', '[::1]:13'),
    ])
    def test_srcrwr_HTTP_ADAPTER(self, query, srcrwr, host):
        """
        https://st.yandex-team.ru/WEBREPORT-665
        Тест на правильность функционирования параметра &srcrwr=HTTP_ADAPTER:*:*:*
        Тест применим только к apache + mod_proxy
        Апач нужно перевести в тестовый режим с помощью &srcrwr_test_mode=HTTP_ADAPTER
        В этом режиме апач начнет выдавать редирект на ручку /SRCRWR_HTTP_ADAPTER_TEST_OK
        """
        query.set_internal()
        if not host:
            query.replace_params({
                'srcrwr_test_mode': 'HTTP_ADAPTER'
            })
            resp = self.request(query, require_status=302)
            assert 'Location' in resp.headers
            location = resp.headers["Location"]
            pos = re.search('\/SRCRWR_HTTP_ADAPTER_TEST_STATUS\?\[(.*?)\]\&', location[0])
            host = pos.group(1)

        if srcrwr:
            query.replace_params({
                'srcrwr': srcrwr,
                'srcrwr_test_mode': 'HTTP_ADAPTER'
            })
        else:
            query.replace_params({
                'srcrwr_test_mode': 'HTTP_ADAPTER'
            })

        resp = self.request(query, require_status=302)
        assert 'Location' in resp.headers
        location = resp.headers["Location"]
        assert '/SRCRWR_HTTP_ADAPTER_TEST_STATUS?[{host}]&[{srcrwr}]'.format(host=host, srcrwr=srcrwr) in location[0]



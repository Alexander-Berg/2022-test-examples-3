# -*- coding: utf-8 -*-

import time
import traceback
from md5 import md5
from copy import deepcopy

import pytest

from runtime_tests.report.base import BaseReportTest
from runtime_tests.util.predef.handler.server.http import SimpleConfig
from runtime_tests.util.predef.http.response import ok

from report.const import *
from runtime_tests.report.base import Query


class BaseFuncTest(BaseReportTest):
    @classmethod
    @pytest.fixture(scope='class', autouse=True)
    def setup(cls, request, local_ip, report_ip, report_port, sock_family, schema_dir, global_sources):
        super(BaseFuncTest, cls).setup(
            request, local_ip, report_ip, report_port, sock_family, schema_dir, global_sources
        )

    def get_pageID(self, data):
        return data['metahost'][0].path.split('/')[-1]

    @pytest.fixture(scope='class', autouse=True)
    def ping_request(self, request, setup, sandbox):
        if not sandbox:
            return
        query = Query(self.host, self.port, self.additional_flags)
        query.set_url('/search/?text=ping_test')

        timeout = 45
        st = time.time()
        msg = 'No answer from report on %s' % request.config.option.host

        res = None
        while True:
            try:
                res = self.send_query(query, debug=False)
                break
            except Exception:
                if time.time() - st < timeout:
                    print traceback.format_exc()
                    print msg
                    time.sleep(1)
                else:
                    pytest.fail(msg)
        return res

    def get_hamster_upper(self, text, params=None):
        if isinstance(text, Query):
            query = deepcopy(text)
        else:
            query = Query(self.host, self.port, self.additional_flags)
            query.add_flags({'noapache_json_req': 1, 'noapache_json_res': 0})
            query.set_params({'text': text})
            query.replace_params(params)

        resp_beta = self.request(query, source='UPPER')
        resp_hamster = self.request(resp_beta.source.request_raw, host=query.hamster_host(), port=9080)
        assert resp_hamster

        return resp_hamster.content

    def get_config(self, data):
        return SimpleConfig(
            response=ok(
                headers=[
                    ('Content-Type', 'text/plain; charset=utf-8'),
                    ('Content-Encoding', 'deflate'),
                    ('Transfer-Encoding', 'chunked')
                ],
                data=hex(len(data))[2:] + '\r\n' + data + '\r\n' + '0\r\n\r\n'
            )
        )

    __noapache_resp_data = {}

    def noapache_resp(self, query):
        key = md5(str(query)).hexdigest()
        if key not in self.__noapache_resp_data:
            self.__noapache_resp_data[key] = self.get_hamster_upper(query)
        return deepcopy(self.__noapache_resp_data[key])

    def get_noapache_setup(self, query):
        query.set_internal()
        noapache_setup = self.json_dump_context(query, ['noapache_setup'])
        for ctx in noapache_setup:
            if 'client_ctx' in ctx:
                return ctx
        if len(noapache_setup) != 0:
            return noapache_setup[0]

        return {}

    def check_grouping(self, grouping, regexp):
        ok = False
        for g in grouping:
            if regexp.search(g):
                ok = True
                break
        assert ok, "got grouping: " + str(grouping)

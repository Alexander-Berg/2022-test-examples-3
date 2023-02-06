# -*- coding: utf-8 -*-

import os
import pytest
import base64
import json

from report.functional.web.base import BaseFuncTest
from report.const import *

@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestFlag(BaseFuncTest):
    # test for task SERP-64870
    def test_disable_flag_sections(self, query):
        query.set_internal()
        query.set_params({'json_dump': 'rdat.flags.all.TESTID.0'})
        query.add_params({'json_dump': 'rdat.flags.sources.experiments'})
        resp = self.json_request(query)
        j = json.loads(resp.content)

        testid = None
        for item in j["rdat.flags.sources.experiments"]:
            if len(item["TESTID"]) and item["TESTID"][0] == j["rdat.flags.all.TESTID.0"]:
                testid = item["TESTID"][0]
                break
        assert testid

        query.set_params({'json_dump': 'rdat.flags.sources.experiments', 'disable_flag_sections': testid})
        resp = self.json_request(query)
        j = json.loads(resp.content)
        for item in j["rdat.flags.sources.experiments"]:
            assert not(testid in item["TESTID"])

        # WEBREPORT-532
        query.set_custom_headers({'X-Yandex-ExpFlags': base64.b64encode(json.dumps([{"HANDLER":"REPORT","CONTEXT":{"MAIN":{"REPORT":{"disable_flag_sections":[testid]}}},"TESTID":["119327"]}]))})
        resp = self.json_request(query)
        j = json.loads(resp.content)
        for item in j["rdat.flags.sources.experiments"]:
            assert not(testid in item["TESTID"])

        query.headers.pop('X-Yandex-ExpFlags')
        query.set_params({'json_dump': 'rdat.flags.sources.experiments'})
        resp = self.json_request(query)
        j = json.loads(resp.content)
        flag = 0

        for item in j["rdat.flags.sources.experiments"]:
            if testid in item["TESTID"]:
                flag = 1
                break
        assert flag, "can not found testid {}".format(testid)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_its_priority(self, query):
        query.set_internal()
        key = 'rdat.flags.sources.production_upper_sas_Web'
        query.set_params({'json_dump': key})
        resp = self.json_request(query)
        assert key in resp.data

        flag = []
        for item in resp.data[key]:
            if ('CONDITION' not in item):
                for (k,v) in item.items():
                    if k =='app_host_srcskip':
                        continue

                    if type(v) == str or type(v) == type(u''):
                        flag.extend([k, v])
                        break
                break

        if flag:
            exp = [{"HANDLER": "REPORT", "TESTID": ["37"], "CONTEXT": {"MAIN": {"REPORT": {"increase_its_priority": 1, flag[0]: 'serg'}}}}]
            query.headers.set_custom_headers({'X-Yandex-ExpFlags': base64.b64encode(json.dumps(exp))})

            key = 'rdat.flags.all.' + flag[0]
            query.set_params({'json_dump': key})
            resp = self.json_request(query)

            assert key in resp.data
            assert resp.data[key]==flag[1]

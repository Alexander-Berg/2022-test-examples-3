# -*- coding: utf-8 -*-

import base64
import pytest

from report.const import  *
from report.functional.web.base import BaseFuncTest

def type_condition(query_type, condition):
    if query_type == condition and query_type != PAD:
        return True
    if condition == "desktop" and query_type == PAD:
        return True
    if condition == "mobile" and query_type in [SMART]:
        return True
    return False

TEST_ID = '7049'

MAIN_REARR_TEST_PARAM = "gamgy/MAIN_REARR_TEST_PARAM/main=1"
MAIN_YABS_REARR_TEST_PARAM = "gamgy/report/main/yabs=1"
GLOBAL_REARR_TEST_PARAM = "gamgy/report/global=1"
GLOBAL_REARR_TEST_PARAM = "gamgy/report/global1=1"
GLOBAL_REARR_TEST_PARAM = "gamgy/report/global2=1"
GLOBAL_REARR_TEST_PARAM = "gamgy/report/global3=1"
GLOBAL_REARR_TEST_PARAM_LIST = ["gamgy/report/global1=1", "gamgy/report/global2=1", "gamgy/report/global3=1"]

A_RELEV_TEST_PARAM = "gamgy/relev/a=1"
A_SNIP_TEST_PARAM = "gamgy/snip/a=1"

A_REARR_TEST_PARAM = "gamgy/report/a=1"
B_REARR_TEST_PARAM = "gamgy/report/b=1"
MAIN_REARR_TEST_PARAM = "gamgy/report/main=1"

A_METAHOST_TEST_PARAM = "gamgy/metahost/a=1"
B_METAHOST_TEST_PARAM = "gamgy/metahost/b=1"
MAIN_METAHOST_TEST_PARAM = "gamgy/metahost/main=1"

A_METAHOST2_TEST_PARAM = "gamgy/metahost2/a=1"
B_METAHOST2_TEST_PARAM = "gamgy/metahost2/b=1"
MAIN_METAHOST2_TEST_PARAM = "gamgy/metahost2/main=1"

A_SRCSKIP_TEST_PARAM = "gamgy/srcskip/a=1"
B_SRCSKIP_TEST_PARAM = "gamgy/srcskip/b=1"
MAIN_SRCSKIP_TEST_PARAM = "gamgy/srcskip/main=1"
MAIN_UPPER_SRCSKIP_TEST_PARAM = "UPPER/srcskip/main=1"
MAIN_WEB_ALL_SRCSKIP_TEST_PARAM = "WEB_ALL/src_skip/main=1"

A_PRON_TEST_PARAM = "gamgy/pron/a=1"
B_PRON_TEST_PARAM = "gamgy/pron/b=1"
MAIN_PRON_TEST_PARAM = "gamgy/pron/main=1"

A_WIZEXTRA_TEST_PARAM = "wz_a=111"
B_WIZEXTRA_TEST_PARAM = "wz_b=111"

WIZEXTRA_TEST_PARAM3213 = "wz_a=3213"
REARR_TEST_PARAM_LIST3213 = ["1gamgy/report/global1=3213", "2gamgy/report/global1=3213"]
D_REARR_TEST_PARAM3213 = "D_gamgy/report/global1=3213"

A_WIZEXTRA_WIZARD = "gamgy/WIZEXTRA/WIZARD/a=1"
B_WIZEXTRA_WIZARD = "gamgy/WIZEXTRA/WIZARD/b=1"
MAIN_WIZEXTRA_WIZARD = "gamgy/WIZEXTRA/WIZARD/main=1"
AIN_WIZEXTRA_WIZARD_TDI = "tdi=gamgy/WIZEXTRA/WIZARD/main=1"

A_RWR_WIZARD = "gamgy/RWR/WIZARD/a=1"
B_RWR_WIZARD = "gamgy/RWR/WIZARD/b=1"
MAIN_RWR_WIZARD = "gamgy/RWR/WIZARD/main=1"
MAIN_RWR_WIZARD_TDI = "tdi=gamgy/RWR/WIZARD/main=1"

A_WIZHOSTS_WIZARD = "gamgy/WIZHOSTS/WIZARD/a=1"
B_WIZHOSTS_WIZARD = "gamgy/WIZHOSTS/WIZARD/b=1"
MAIN_WIZHOSTS_WIZARD = "gamgy/WIZHOSTS/WIZARD/main=1"
MAIN_WIZHOSTS_WIZARD_TDI = "tdi=gamgy/WIZHOSTS/WIZARD/main=1"

A_WIZEXPERIMENT_WIZARD = "gamgy/WIZEXPERIMENT/WIZARD/a=1"
B_WIZEXPERIMENT_WIZARD = "gamgy/WIZEXPERIMENT/WIZARD/b=1"
MAIN_WIZEXPERIMENT_WIZARD = "gamgy/WIZEXPERIMENT/WIZARD/main=1"
MAIN_WIZEXPERIMENT_WIZARD_TDI = "tdi=gamgy/WIZEXPERIMENT/WIZARD/main=1"

TEST_ID_NOLOG = '7654321'
TEST_ID_LOG = '1234567'

NAME_7049 = 'context_name=7049'
NAME_A = 'context_name=A'

class Experiment(dict):
    def __init__(self, test_id=TEST_ID):
        self.test_id = test_id
        self['HANDLER'] = "REPORT"
        self['CONTEXT'] = dict()
        self['CONTEXT']['MAIN'] = {}
        self['TESTID'] = [self.test_id]

    def to_base64(self):
        return base64.b64encode(json.dumps([self]))

class ExperimentWizard(Experiment):
    def __init__(self, *args, **kwargs):
        super(ExperimentWizard, self).__init__(*args, **kwargs)
        self['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}
        self['CONTEXT']['GLOBAL'] = {"WEB_ALL": {"rearr": [GLOBAL_REARR_TEST_PARAM]}}
        self['CONTEXT']['A'] = {"WEB_ALL":{ "rearr":[A_REARR_TEST_PARAM]}, "WIZARD": {"wizextra": [A_WIZEXTRA_TEST_PARAM]} }
        self['CONTEXT']['B'] = {"WEB_ALL":{ "rearr":[B_REARR_TEST_PARAM]}, "WIZARD": {"wizextra": [B_WIZEXTRA_TEST_PARAM]} }

class BaseExperimentTest(BaseFuncTest):
    @classmethod
    @pytest.fixture(scope='function')
    def exp(self):
        return Experiment()

    @classmethod
    @pytest.fixture(scope='function')
    def exp_wizard(self):
        return ExperimentWizard()

    def base_assert(self, ctxs, result, source='WEB'):
        if result:
            assert MAIN_REARR_TEST_PARAM in ';'.join(ctxs['client_ctx'][source]['rearr'])
        else:
            if 'rearr' in ctxs['client_ctx'][source]:
                assert MAIN_REARR_TEST_PARAM not in ';'.join(ctxs['client_ctx'][source]['rearr'])

    def complex_assert(self, data):
        assert 'MAIN' in data
        assert TEST_ID in data

        assert 'YABS' in data['MAIN']
        assert 'YABS' not in data[TEST_ID]

        rearr_main = data['MAIN']['WEB']['rearr']
        rearr_testid = data[TEST_ID]['WEB']['rearr']

        assert MAIN_REARR_TEST_PARAM in rearr_main
        assert A_REARR_TEST_PARAM in rearr_main
        assert NAME_A in rearr_main

        assert MAIN_REARR_TEST_PARAM in rearr_testid
        assert B_REARR_TEST_PARAM in rearr_testid
        assert NAME_7049 in rearr_testid

    def base_test_experiments_log(self, query, exp, tld=RU, query_type=DESKTOP, i18n=L_RU, cgi=None, result=True):
        exp['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}
        resp = self.base_test_log(query, exp=exp, tld=tld, query_type=query_type, i18n=i18n, cgi=cgi)
        return resp

    def base_test_log(self, query, exp=None, tld=RU, query_type=DESKTOP, i18n=L_RU, cgi=None):
        query = self.base_query(query, exp, tld, query_type, i18n, cgi)
        resp = self.request(query)
        return resp

    def base_test_experiments(self, query, exp, tld=RU, query_type=DESKTOP, i18n=L_RU, cgi=None, result=True):
        exp['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}
        resp = self.base_test(query, exp, tld, query_type, i18n, cgi)
        self.base_assert(resp, result)
        return resp

    def base_query(self, query, exp=None, tld=RU, query_type=DESKTOP, i18n=L_RU, cgi=None):
        self.log(filename="condition.txt", data=exp)
        if exp != None:
            query.headers.set_custom_headers({
                'X-Yandex-ExpBoxes': TEST_ID + ',0,76',
                'X-Yandex-ExpConfigVersion': '3764',
                'X-Yandex-ExpFlags': exp.to_base64(),
            })

        query.set_host(tld)
        query.headers.cookie.set_my(COOKIEMY_LANG[i18n])
        if cgi:
            query.replace_params(cgi)
        #SERP-40943
        if tld == COM:
            query.replace_params({'lr': REGION[USA]})
        query.set_query_type(query_type)

        return query

    def base_test_ctxs(self, query, exp=None, tld=RU, query_type=DESKTOP, i18n=L_RU, cgi=None):
        query = self.base_query(query, exp, tld, query_type, i18n, cgi)

        text_expected = query.path.params['text']

        return self.json_dump_ctxs(query)

    def base_test(self, query, exp=None, tld=RU, query_type=DESKTOP, i18n=L_RU, cgi=None):
        query = self.base_query(query, exp, tld, query_type, i18n, cgi)

        text_expected = query.path.params['text']

        noapache_setup = self.get_noapache_setup(query)

        global_ctx = noapache_setup['global_ctx']
        assert global_ctx
        assert 'user_request' in global_ctx

        assert len(global_ctx['user_request']) == 1
        assert global_ctx['user_request'][0] == text_expected

        return noapache_setup

    def base_test_json(self, query, exp=None, tld=RU, query_type=DESKTOP, i18n=L_RU, cgi=None, dump_key=1):
        query = self.base_query(query, exp, tld, query_type, i18n, cgi)
        resp = self.json_dump_request(query, dump_key)

        # global_ctx = resp.source.glob
        # assert global_ctx
        # assert 'user_request' in global_ctx

        # assert len(global_ctx['user_request']) == 1
        # text_expected = query.path.params['text']
        # assert global_ctx['user_request'][0] == text_expected

        return resp

    def base_test_302(self, query, exp):
        self.log(filename="condition.txt", data=exp)

        query.set_https(False)
        query.headers.set_custom_headers({'X-Yandex-ExpBoxes': '7049,0,76', 'X-Yandex-ExpFlags': exp.to_base64(), 'X-Yandex-ExpConfigVersion': '3764'})

        return self.request(query, require_status=302)

    def base_test_experiments_conditions(self, query, exp, tld=RU, query_type=DESKTOP, i18n=L_RU, cgi=None, condition='', result=True):
        exp['CONDITION'] = condition
        return self.base_test_experiments(query, exp=exp, tld=tld, query_type=query_type, i18n=i18n, cgi=cgi, result=result)


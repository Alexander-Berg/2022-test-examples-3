# -*- coding: utf-8 -*-

import os
import pytest
import json
import urllib
import re
import base64

from report.functional.web.base import BaseFuncTest
from report.const import *

@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestReport(BaseFuncTest):
    @pytest.mark.ticket('SERP-46462')
    @pytest.mark.parametrize(
        ('l10n', 'language'),
        [
            ('ru', 'ru'),
            ('uz', 'uz'),
            ('foo', 'ru')
        ]
    )
    def test_flag_l10n(self, query, l10n, language):
        query.set_flags({'l10n': l10n})
        resp = self.json_test(query)
        assert resp.data['reqdata']['language'] == language

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_reqid_balancer(self, query):
        query.headers['X-Req-Id'] = '888-myreqid-'
        query.set_params({'p': 2})
        resp = self.request(query)
        assert re.match(r'^888-myreqid-\d+-.+$', resp.reqid)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_reqid_report(self, query):
        query.set_params({'p': 2})
        resp = self.request(query)
        assert re.match(r'^\d+-\d+-.+$', resp.reqid)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_uniq_grouping(self, query):
        query.set_internal()
        query.add_params({'json_dump': 'search.context.context.g'})

        resp = self.request(query)
        uniq = {}
        duplicate = {}
        for group in resp.data['search.context.context.g']:
            if uniq.get(group):
                duplicate[group] = 1
            else:
                uniq[group] = 1
        assert duplicate=={}, duplicate

        uniq = {}
        duplicate = []
        for group in resp.data['search.context.context.g']:
            parts = group.split('.')
            if uniq.get(parts[1]):
                if len(duplicate)==0:
                    duplicate.append(uniq[parts[1]])
                duplicate.append(group)
            else:
                uniq[parts[1]] = group
        assert duplicate==[], duplicate


    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_metahost1(self, query):
        param_name='metahost'
        query.add_params({
            'json_dump': [
                    'search.context.context.metahost',
                    'rdat.flags.all.metahost',
                    'search.app_host.sources.(_.name eq "REWRITE").results.0.rewrite'
            ],
            'metahost': ['GOSUSLUGI:mob.search.yandex.net:11003', 'GOSUSLUGI:localhost:11003'],
        })

        expect = {
            'one': {
                'rdat.flags.all.metahost': {'GOSUSLUGI': ['mob.search.yandex.net:11003','localhost:11003']},
                'search.context.context.metahost': [
                    'GOSUSLUGI:mob.search.yandex.net:11003',
                    'GOSUSLUGI:localhost:11003',
                    'GOSUSLUGI:mob.search.yandex.net:11003',
                    'GOSUSLUGI:localhost:11003',
                ],
            },
            'two': {
                'UPPER': {'metahost': ['GOSUSLUGI:mob.search.yandex.net:11003', 'GOSUSLUGI:localhost:11003']},
                'SEARCH': {'metahost': ['GOSUSLUGI:mob.search.yandex.net:11003', 'GOSUSLUGI:localhost:11003']}
            }
        }
        resp = self.json_request(query)

        def resp_helper(data, param_name):
            two = data.pop('search.app_host.sources.(_.name eq "REWRITE").results.0.rewrite')[0]
            for k in two.keys():
                if k not in ('UPPER', 'SEARCH'):
                    two.pop(k)

            for s in ['UPPER', 'SEARCH']:
                for k in two[s].keys():
                    if k != param_name:
                        two[s].pop(k)

            return {'one': data, 'two': two}
        data = resp_helper(resp.data, param_name)

        assert data == expect, data

        query.remove_params(param_name)
        query.add_params({
            'flag': ['{}=GOSUSLUGI=mob.search.yandex.net:11003\;localhost:11003'.format(param_name)]
        })
        resp = self.json_request(query)
        data = resp_helper(resp.data, param_name)

        assert data == expect, data

        query.remove_params('flag')
        query.set_custom_headers({
            'X-Yandex-Internal-Flags': base64.standard_b64encode(json.dumps({param_name:{'GOSUSLUGI': ['mob.search.yandex.net:11003','localhost:11003']}}))
        })
        resp = self.json_request(query)
        data = resp_helper(resp.data, param_name)
        assert data == expect, data

    @pytest.mark.skipif(True, reason="SEARCH-8142")
    def test_metaopts(self, query):
        param_name='metaopts'
        query.add_params({
            'json_dump': [
                    'search.context.context.{}'.format(param_name),
                    'rdat.flags.all.{}'.format(param_name),
            ],
            param_name: ['WEB:TimeOut=0s', 'WEB:TimeOut=1s'],
        })

        expect = {
            'rdat.flags.all.{}'.format(param_name): { 'WEB': ['TimeOut=0s','TimeOut=1s']},
            'search.context.context.{}'.format(param_name): [
                'WEB:TimeOut=0s',
                'WEB:TimeOut=1s'
            ],
        }
        resp = self.json_request(query)
        assert resp.data == expect, resp.data

        query.remove_params(param_name)
        query.add_params({
            'flag': ['{}=WEB=TimeOut=0s\;TimeOut=1s'.format(param_name)]
        })
        resp = self.json_request(query)
        assert resp.data == expect, resp.data

        query.remove_params('flag')
        query.set_custom_headers({
            'X-Yandex-Internal-Flags': base64.standard_b64encode(json.dumps({param_name:{'WEB': ['TimeOut=0s','TimeOut=1s']}}))
        })
        resp = self.json_request(query)
        assert resp.data == expect, resp.data

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_pron(self, query):
        param_name='pron'
        pron_params = [u'garbage', u'proxima=all']
        query.add_params({
            'json_dump': [
                    u'search.context.context.{}'.format(param_name),
                    u'rdat.flags.all.{}'.format(param_name),
            ],
            param_name: pron_params,
        })

        resp = self.json_request(query)
        for pron_param in pron_params:
            assert pron_param in resp.data[u'rdat.flags.all.pron'], resp.data
            assert pron_param in resp.data[u'search.context.context.pron'], resp.data

        query.remove_params(param_name)
        query.add_params({
            'flag': [('{}=' + ','.join(pron_params)).format(param_name)]
        })
        resp = self.json_request(query)
        for pron_param in pron_params:
            assert pron_param in resp.data[u'rdat.flags.all.pron'], resp.data
            assert pron_param in resp.data[u'search.context.context.pron'], resp.data

        query.remove_params('flag')
        query.set_custom_headers({
            'X-Yandex-Internal-Flags': base64.standard_b64encode(json.dumps({param_name: pron_params}))
        })
        resp = self.json_request(query)
        for pron_param in pron_params:
            assert pron_param in resp.data[u'rdat.flags.all.pron'], resp.data
            assert pron_param in resp.data[u'search.context.context.pron'], resp.data

# -*- coding: utf-8 -*-

import re
import time
import pytest

from report.const import *
from report.functional.web.base import BaseFuncTest
from jsonschema import ValidationError
from tests import TESTS

# how many retries to do until find subject with counter_prefix
MAX_RETRIES, SLEEP = 6, 5

TEST_CONFIG = 'testpalm, response_schema, counter_prefix, location, params, tld, path'
TESTS = filter(lambda t: t.get('location') and t.get('response_schema'), TESTS)
REPORT_TESTS = [map(t.get, TEST_CONFIG.split(', ')) for t in TESTS]
#REPORT_TESTS = filter(lambda t: t[1] == 'test_snippet_prices_706_0.json', REPORT_TESTS)
#REPORT_TESTS = filter(lambda t: 'snippet_suggest_fact/schema_725.json' in t[1], REPORT_TESTS)
#REPORT_TESTS = filter(lambda t: 'wiz_converter_currencies_569' in t[1], REPORT_TESTS)
#REPORT_TESTS = filter(lambda t: 'colors' in t[2], REPORT_TESTS)
#REPORT_TESTS = REPORT_TESTS[16:20]

LEN = len(REPORT_TESTS) / 4 or len(REPORT_TESTS)
REPORT_TESTS_1 = REPORT_TESTS[:LEN]
REPORT_TESTS_2 = REPORT_TESTS[LEN:2*LEN]
REPORT_TESTS_3 = REPORT_TESTS[2*LEN:3*LEN]
REPORT_TESTS_4 = REPORT_TESTS[3*LEN:]

class MyNotFoundException(Exception):
    """Search results are not empty but subject not found there"""


class TestReportResponse(BaseFuncTest):
    @pytest.mark.parametrize((TEST_CONFIG), REPORT_TESTS_1)
    def test_serp_features_1(self, query, schema_dir, testpalm, response_schema, counter_prefix, location, params, tld, path):
        test_config = dict(zip(TEST_CONFIG.split(', '), eval('(' + TEST_CONFIG + ')')))
        self.serp_feature(query, DESKTOP, test_config.get('testpalm'), test_config['response_schema'], schema_dir, test_config)

    @pytest.mark.parametrize((TEST_CONFIG), REPORT_TESTS_2)
    def test_serp_features_2(self, query, schema_dir, testpalm, response_schema, counter_prefix, location, params, tld, path):
        test_config = dict(zip(TEST_CONFIG.split(', '), eval('(' + TEST_CONFIG + ')')))
        self.serp_feature(query, DESKTOP, test_config.get('testpalm'), test_config['response_schema'], schema_dir, test_config)

    @pytest.mark.parametrize((TEST_CONFIG), REPORT_TESTS_3)
    def test_serp_features_3(self, query, schema_dir, testpalm, response_schema, counter_prefix, location, params, tld, path):
        test_config = dict(zip(TEST_CONFIG.split(', '), eval('(' + TEST_CONFIG + ')')))
        self.serp_feature(query, DESKTOP, test_config.get('testpalm'), test_config['response_schema'], schema_dir, test_config)

    @pytest.mark.parametrize((TEST_CONFIG), REPORT_TESTS_4)
    def test_serp_features_4(self, query, schema_dir, testpalm, response_schema, counter_prefix, location, params, tld, path):
        test_config = dict(zip(TEST_CONFIG.split(', '), eval('(' + TEST_CONFIG + ')')))
        self.serp_feature(query, DESKTOP, test_config.get('testpalm'), test_config['response_schema'], schema_dir, test_config)

    def serp_feature(self, query, query_type, testpalm, response_schema, schema_dir, test_config):
        with open(os.path.join(schema_dir, response_schema)) as f:
            content = f.read()
            schema = validate_json(content)

        query.headers.cookie.set_yandexuid(922657501440154446)
        query.set_params(test_config['params'])
        query.replace_params({'wizextra': 'misspell_timeout=500ms', 'wiztimeout': 1, 'timeout': 10000000, 'waitall': 'da', 'test-mode': 1})

        if 'tld' in test_config:
            query.set_host(test_config['tld'])

        if 'path' in test_config:
            query.set_url(test_config['path'])
        elif query_type:
            query.set_query_type(query_type)

        subjects, errors = self.get_subjects(query, schema, test_config)

        if subjects:
            pass # SERP-47826
        else:
            pytest.skip("feature disappeared from '%s'" % (query.target_host))

        """
        TODO validate mini scheme
        ========================
        if subject:
            for i, subj in enumerate(subjects):
                try:
                    if self.validate_json_scheme(subj, deepcopy(schema)):
                        return
                except ValidationError as e:
                    errors.append('subject#%d INVALID\n%s\n%s\n' % (i, subj, e))

            if errors:
                pytest.fail('\n'.join(['', testpalm or '', repr(query), 'ValidationErrors caught on beta:'] + errors))
                return
        """

        """
        TODO fix blinks with terry@-provided cgi-params, so require serp feature if uncomment
        =============================
        # test FAILED but try deside to skip it - search same object on hamster
        query.replace_params({'no-tests': 1})
        subjects_hamster, errors_hamster = self.get_subjects(query, schema, test_config, query.hamster_host(), 80)

        assert not subjects_hamster, '\n'.join(['', testpalm or '', query.hamster_url(), 'feature absent on beta but present on hamster', 'ERRORS caught on beta:'] + errors)

        pytest.skip("feature disappeared both from '%s' and '%s'" % (query.target_host, query.hamster_host))
        """

    def get_subjects(self, query, schema, test_config, host=None, port=None):
        if host and port:
            query.target_host = host
            query.target_port = port
        elif not host and not port:
            pass
        else:
            raise Exception('host and port is required')

        retry, subjects, errors = 0, [], []

        while not subjects and retry < MAX_RETRIES:
            error = None
            retry += 1
            resp = self.json_request(query)

            if not host:
                resp.validate_schema(schema)

            try:
                subjects = self.parse_subjects(resp.data, schema, test_config)
            except MyNotFoundException as e:
                error = str(e)

            if subjects:
                return subjects, []

            errors.append('query#%s %s: %s' % (str(retry), host or 'beta', error or 'No error'))
            print 'WARNING: ' + errors[-1]
            time.sleep(SLEEP)

        return [], errors

    def parse_subjects(self, resp, schema, test_config):
        location = test_config['location']
        counter_prefix = test_config['counter_prefix']

        if '~' in counter_prefix:
            counter_prefix, type_ = counter_prefix.split('~')
        else:
            type_ = None

        error = None
        subjects = []

        if location.startswith('searchdata.docs'):
            docs_kind = 'docs_right' if 'docs_right' in location else 'docs'

            assert 'searchdata' in resp, 'Report error: no searchdata key'
            assert docs_kind in resp['searchdata'], "Report error: no '" + docs_kind + "' key"

            for doc in resp['searchdata'][docs_kind]:
                m_kind = re.search(r'.snippets.(\w+)$', location)
                if m_kind:
                    snippet_kind = m_kind.group(1)

                    if snippet_kind not in doc['snippets'] or not doc['snippets'][snippet_kind]:
                        continue

                    container = doc['snippets'][snippet_kind]

                    if isinstance(container, dict):
                        if container['counter_prefix'] == counter_prefix and (not type_ or type_ == container.get('type', '')):
                            subjects.append(container)
                    elif isinstance(container, list):
                        for snip in container:
                            if snip['counter_prefix'] == counter_prefix and (not type_ or type_ == snip.get('type', '')):
                                subjects.append(snip)
                    else:
                        pytest.fail(location + ' NOT IMPLEMENTED')
                elif location.endswith('.construct'):
                    if 'construct' not in doc or not doc['construct']:
                        continue

                    assert isinstance(doc['construct'], list), 'construct_as_array must be enabled for all'

                    for container in doc['construct']:
                        if container['counter']['path'] == counter_prefix:
                            subjects.append(container)
                else:
                    pytest.fail(location + ' NOT IMPLEMENTED')

            if not subjects:
                if resp['searchdata'][docs_kind]:
                    error = "No '%s' found in search results '%s'" % (counter_prefix, location)
                else:
                    error = 'Empty searchdata.' + docs_kind
        elif location.startswith('wizplaces'):
            for wplace in resp.get('wizplaces', {}).values():
                for wiz in wplace:
                    if location == 'wizplaces.*.construct':
                        if 'construct' not in wiz:
                            continue
                        assert wiz.get('construct')
                        assert len(wiz) == 1, 'expected key "construct" only'
                        for construct in wiz['construct']:
                            if construct.get('counter', {}).get('path') == counter_prefix:
                                subjects.append(construct)
                    else:
                        if 'counter_prefix' not in wiz:
                            continue
                        if wiz['counter_prefix'] == counter_prefix:
                            subjects.append(wiz)
            if not subjects:
                if resp.get('wizplaces', {}):
                    error = "No wizard '%s' found in search results '%s'" % (counter_prefix, location)
                else:
                    error = "No wizplaces at all"
        elif location in ['searchdata', 'navi', 'banner']:
            subjects = [resp[location]]
        elif location.startswith('banner.data.'):
            name = location.split('.')[-1]
            assert name, 'wrong location: ' + location
            assert 'banner' in resp, str(resp) + '\nno banner here. It could be report error, CHECK IT'
            assert 'data' in resp['banner'], str(resp['banner']) + "\nit seems empty banner here, CHECK IT"
            container = resp['banner']['data']
            assert container, str(resp['banner']) + "\nit seems empty banner here, CHECK IT"

            for key, banners in container.items():
                if key == name:
                    if banners:
                        subjects.append(banners[0])

            if not subjects:
                error = "No '%s' found in search results '%s'" % (name, location)
        else:
            pytest.fail(location + ' NOT IMPLEMENTED')

        if not subjects and not error:
            error = "No '%s' was found" % location

        if error:
            raise MyNotFoundException(error)

        return subjects


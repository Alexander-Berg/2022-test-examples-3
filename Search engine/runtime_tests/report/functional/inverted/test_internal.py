# -*- coding: utf-8 -*-

import re
import zlib
import base64
import os
import pytest
import urllib

from lxml import etree

from report.const import *
from report.functional.web.base import BaseFuncTest

eventlog_sep = re.compile(r'\n+(?=\d+\t)')

_STATS = (
    '[{"name":"_STATS",'
    '"results":['
    '{"events":["1498763588784661\\t0\\tcustom event 1", '
    '"1498763588785385\\t0\\tcustom event 2"]}]}]'
)


def _get_eventlog_frames(content):
    frames_bin = []
    for x in re.findall(r'^<!-- //EventLogFrame=(\d+)?$(.*?)^//EventLogFrame=', content, re.S | re.M):
        frames_bin.append(x[1])
    assert frames_bin, 'No EventLogFrame markers'

    frames_raw = [zlib.decompress(base64.decodestring(x), zlib.MAX_WBITS | 16) for x in frames_bin]
    assert frames_raw

    frames = EventLog().parse([
        y for x in frames_raw for y in re.split(eventlog_sep, x)
    ])
    assert frames

    return frames


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestEventlog(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize("is_external", (False, True))
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_eventlog_kuka_app_host(self, query, enable_kuka, is_external):
        enable_kuka(query)
        query.set_external(is_external)
        resp = self.app_host_request(query)
        app_host = resp.sources['APP_HOST']
        assert app_host

        data = app_host.requests[0].data
        init = filter(lambda x: x['name'] == 'APP_HOST_PARAMS', data)
        assert init, data
        assert 'dump' not in init[0]['results'][0]

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize("is_external", (False, True))
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_eventlog_dump_app_host(self, query, is_external):
        query.set_external(is_external)
        query.add_params({'dump': 'eventlog', 'nocache': 'da'})
        resp = self.app_host_request(query)
        app_host = resp.sources['APP_HOST']
        assert app_host

        data = app_host.requests[0].data
        init = filter(lambda x: x['name'] == 'APP_HOST_PARAMS', data)
        assert init, data
        if is_external:
            assert 'dump' not in init[0]['results'][0]
        else:
            assert init[0]['results'][0]['dump'] == 'eventlog'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("is_external", (False, True))
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_show_eventlogs_debug_app_host(self, query, is_external):
        query.set_external(is_external)
        query.add_params({'debug': 'show_eventlogs', 'nocache': 'da'})
        resp = self.app_host_request(
            query,
            source=(
                'APP_HOST',
                _STATS
            )
        )
        app_host = resp.sources['APP_HOST']
        assert app_host

        data = app_host.requests[0].data
        init = filter(lambda x: x['name'] == 'APP_HOST_PARAMS', data)
        assert init, data
        expect = [] if is_external else ['show_eventlogs']
        assert init[0]['results'][0]['debug'] == expect

        try:
            frames = _get_eventlog_frames(resp.content)
        except AssertionError:
            if is_external:
                return
            else:
                raise

        assert len(filter(lambda x: x[2] == "custom event 1", frames)) == 1
        assert len(filter(lambda x: x[2] == "custom event 2", frames)) == 1

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("is_external", (False, True))
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_eventlog_debug_app_host(self, query, is_external):
        query.set_external(is_external)
        query.add_params({'debug': 'eventlogs', 'nocache': 'da'})
        resp = self.app_host_request(
            query, source=(
                'APP_HOST',
                _STATS
            )
        )
        app_host = resp.sources['APP_HOST']
        assert app_host

        data = app_host.requests[0].data
        init = filter(lambda x: x['name'] == 'APP_HOST_PARAMS', data)
        assert init, data
        expect = [] if is_external else ['eventlogs']
        assert init[0]['results'][0]['debug'] == expect

        try:
            frames = _get_eventlog_frames(resp.content)
        except AssertionError:
            if is_external:
                return
            else:
                raise

        assert len(filter(lambda x: x[2] == "custom event 1", frames)) == 1
        assert len(filter(lambda x: x[2] == "custom event 2", frames)) == 1

    def _eventlog_debug_common(self, query, flags, expect_sources, text, expect_requests, is_external, depth=0):
        resp = self.request(query)

        try:
            frames = _get_eventlog_frames(resp.content)
        except AssertionError:
            if is_external:
                return
            else:
                raise

        report_message = filter(lambda x: x[2] == 'ReportMessage', frames)
        assert len(report_message) == 1, report_message

        bad_source = {}
        for l in filter(lambda x: len(x) > 3 and x[3].startswith('UPPER'), frames):
            bad_source[l[3]] = l[2]

        if filter(lambda x: bad_source[x] == 'TSourceError', bad_source.keys()):
            if depth < 1:
                depth += 1
                return self._eventlog_debug_common(
                    query, flags, expect_sources, text, expect_requests, is_external, depth
                )
            assert 0, bad_source

        if 'UPPER' in expect_sources:
            upper = map(lambda x: x[0], filter(lambda x: x[2] == 'Base64PostBody', frames))
        elif 'APP_HOST' in expect_sources:
            upper = map(lambda x: x[0], filter(lambda x: x[2] == 'TSourceSuccess' and x[3] == 'UPPER', frames))
        else:
            raise Exception('TODO test UPPER request')

        assert uniq(upper) == upper, 'Duplicate events found'
        assert len(upper) == expect_requests

        reqid = report_message[0][3]
        assert reqid, report_message[0]
        requests = filter(lambda x: x[2] == 'RemoteRequestResult', frames)
        assert requests, frames
        requests_parsed = map(lambda x: (x[0], x[3].split(':')), requests)
        assert requests_parsed, requests

        for src in expect_sources:
            got_requests = filter(lambda x: x[1] == [src, reqid], requests_parsed)
            ts_requests = map(lambda x: x[0], got_requests)
            assert uniq(ts_requests) == ts_requests, JSD(got_requests) + '\nDuplicate eventlog'
            assert len(got_requests) == expect_requests, '%s\nFailed %s requests' % (JSD(requests_parsed), src)

        some_primary_source = map(
            lambda x: x[0],
            filter(lambda x: (x[2] in ['TSourceStart']) and (x[3] in ['SRC_SETUP_BATCH']), frames)
        )

        assert len(some_primary_source) == expect_requests, some_primary_source
        assert uniq(some_primary_source) == some_primary_source

        assert len(filter(lambda x: x[2] == 'ContextCreated', frames)) > 5
        assert len(filter(lambda x: x[2] == 'CreateYSRequest', frames)) > 5
        assert len(filter(lambda x: x[2] == 'RearrangeInfo', frames)) > 5

    @pytest.mark.skipif(True, reason="SERP-60410")
    @pytest.mark.parametrize("flags, expect_sources", [
        # new app_host UPPER request
        ({'noapache_json_req': 'app_host:upper', 'noapache_json_res': 'json'},
         ['APP_HOST', 'APP_HOST_WEB', 'APP_HOST_WEB']),
        # old ya-multi-json UPPER request
        ({'noapache_json_req': 1, 'noapache_json_res': 0},
         ['UPPER', 'APP_HOST_WEB', 'APP_HOST_WEB']),
    ])
    @pytest.mark.parametrize("text, expect_requests, is_external", [
        ('text', 1, True),
        ('"hello world asdfqwer12345"', 1, True),  # reask
        ('text', 1, False),
        ('"hello world asdfqwer12345"', 2, False)  # reask
    ])
    def test_eventlog_debug_common(self, query, flags, expect_sources, text, expect_requests, is_external):
        query.set_external(is_external)
        query.set_flags(flags)
        query.set_params({'text': text, 'debug': 'eventlogs', 'nocache': 'da', 'timeout': 15000000})
        query.set_yandexuid(YANDEXUID)
        query.set_auth()
        self._eventlog_debug_common(query, flags, expect_sources, text, expect_requests, is_external)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("is_external", (False, True))
    @pytest.mark.parametrize(('params'), [
        {},
        {'dump': 'eventlog'},
    ])
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_eventlog_report(self, query, params, is_external):
        query.set_external(is_external)
        query.add_params(params)
        resp = self.request(query, require_status=None)
        eventlog = resp.eventlog()

        report_messages = filter(lambda x: x[2] == 'ReportMessage', eventlog['parsed'])
        assert report_messages, frames
        assert len(filter(lambda x: x[4] == 'done', report_messages)) == 1

        # SERP-46804
        for msg in report_messages:
            assert msg[3] == resp.reqid, msg
            assert msg[4] in ['type', 'http_headers_in', 'done'], msg

        assert not filter(lambda x: x[2] in [
            'EnqueueYSRequest',
            'CreateYSRequest',
            'RearrangeInfo'
        ], eventlog['parsed'])

    def _eventlog_report_dump(self, query, is_external, depth=0):
        resp = self.request(query, require_status=None)

        if is_external:
            assert not resp.content.startswith('{')
        else:
            raw = resp.data['eventlog']
            records = map(lambda x: x.encode('utf-8'), re.split(eventlog_sep, raw))
            frames = EventLog().parse(records)
            assert frames

            report_messages = filter(lambda x: x[2] == 'ReportMessage', frames)
            assert report_messages, frames

            upper = {}
            for l in filter(lambda x: len(x) > 3 and x[3].startswith('UPPER'), frames):
                upper[l[3]] = l[2]
            if filter(lambda x: upper[x] == 'TSourceError', upper.keys()):
                if depth < 1:
                    depth += 1
                    return self._eventlog_report_dump(query, is_external, depth)
                assert 0, upper

            assert len(filter(lambda x: x[4] == 'http_headers_in', report_messages)) == 1
            assert not filter(lambda x: x[4] == 'done', report_messages)
            assert len(filter(lambda x: x[2] == 'RearrangeInfo', frames)) > 5

            reqs = filter(lambda x: x[2] == 'MainTaskStarted', frames)
            ts_reqs = map(lambda x: x[0], reqs)
            assert uniq(ts_reqs) == ts_reqs, JSD(reqs) + '\nDuplicate eventlog'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.skipif(True, reason="SERP-60410")
    @pytest.mark.parametrize("is_external", (False, True))
    def test_eventlog_report_dump(self, query, is_external):
        query.set_external(is_external)
        query.add_params({'json_dump': 'eventlog'})
        self._eventlog_report_dump(query, is_external)


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestKukaInSources(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_sbh(self, query):
        query.set_params({'sbh': 1, 'text': 'путин'})
        noapache_setup = self.get_noapache_setup(query)
        assert 'dump' not in noapache_setup['global_ctx']
        assert noapache_setup['client_ctx']['WEB']['fsgta'] == ['_SearcherHostname']
        assert 'search_info' not in noapache_setup['client_ctx']['WEB']


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestKukaInTemplates(BaseFuncTest):
    @pytest.mark.parametrize(('dbgwzr', 'expect'), (
        (-1, 0),
        (0, 0),
        (1, 1),
        (2, 2),
        (3, 2),
    ))
    def test_dbgwzr_tmpl(self, query, dbgwzr, expect):
        query.set_params({'dbgwzr': dbgwzr, 'text': 'hello'})
        resp = self.json_request(query)

        sp = resp.data['reqdata']['special_prefs']
        assert sp['debug_wizard'] == expect
        assert not sp['view_search_hosts']
        assert not sp['show_stuff']
        assert not sp['view_relevance']

    def test_sbh(self, query):
        query.set_params({'sbh': 1, 'text': 'путин'})

        resp = self.json_request(query)

        sp = resp.data['reqdata']['special_prefs']
        assert sp['view_search_hosts']
        assert not sp['show_stuff']
        assert not sp['debug_wizard']
        assert not sp['view_relevance']

        data = resp.data
        assert 'eventlog' not in data or data['eventlog'] == ''
#        assert data['url_event_name'] single value enum - dropped during migration

        rw = data['search']['request_wizards']
        # https://a.yandex-team.ru/arc/trunk/arcadia/web/report/lib/YxWeb/Util/Template/JS.pm?rev=2474873#L282
        assert not rw or len(rw) > 2

        props = data['search_props']['UPPER'][0]['properties']
        scheme = props.get('scheme.json.nodump')
        assert not scheme or not scheme.startswith('REPORT:')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_json_dump_sp(self, query):
        query.set_internal()
        query.set_params({'json_dump': 'search_props', 'text': 'путин'})
        resp = self.json_request(query)
        assert len(resp.data['search_props']) > 5

    def test_show_stuff(self, query):
        query.set_params({'show_stuff': 1})
        resp = self.json_request(query)
        sp = resp.data['reqdata']['special_prefs']
        assert sp['show_stuff']
        assert not sp['view_search_hosts']
        assert not sp['debug_wizard']
        assert not sp['view_relevance']


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestInternal(BaseFuncTest):
    """
    XXX temporary disable
    def test_admin(self, query):
        query.set_external()
        query.set_url('/admin?action=shutdown')
        self.request(query, require_status=403)
    """

    @pytest.mark.skipif(True, reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_reqid_custom(self, query):
        query.set_internal()
        query.set_params({'p': 2, 'myreqid': '888-myreqid-'})
        resp = self.request(query)
        assert resp.reqid == '888-myreqid-'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("path", [
        '/search/inforequest',
        '/search/checkconfig',
        '/search/viewconfig',
    ])
    @pytest.mark.parametrize("tld", [RU, UA])
    def test_nomda(self, query, tld, path, enable_kuka):
        enable_kuka(query)
        query.set_url(path)
        query.set_host(tld)
        query.reset_auth()
        self.request(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-43610')
    @pytest.mark.parametrize(('flag'), ('qqq', 'www'))
    def test_flag_via_cgi(self, query, flag):
        query.set_flags({'force_https': flag})
        resp = self.json_test(query)
        assert resp.data['reqdata']['flags']['force_https'] == flag

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(('is_external, content_type'), [
        (True, 'text/html'),
        (False, 'application/json'),
    ])
    @pytest.mark.parametrize(('params'), (
        {},
        {'test-mode': 0},
        {'test-mode': 1},
    ))
    def test_json_dump(self, query, params, is_external, content_type):
        query.set_external(is_external)
        query.set_params({'json_dump': 'reqdata.ua'})
        query.add_params(params)
        resp = self.request(query)
        assert resp.headers.get_one('Content-Type').startswith(content_type)
        if is_external:
            assert not resp.content.startswith('{')
        else:
            assert resp.data.get('reqdata.ua')


    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize("url", [
        "/search/check_condition?callback=cb234&condition=touch || smart",
    ])
    def test_search_check_condition_callback(self, query, url):
        query.set_internal()
        query.path.set_url(url)

        resp = self.request(query)
        assert 'cb234(' in resp.content

    @pytest.mark.parametrize(
        ("url", "status", "error_code", "condition"),
        [
            ("/search/check_condition?condition={}".format(urllib.quote(condition)), status, error_code, condition)
            for condition, status, error_code in [
                ("touch || smart", "OK", 0),
                ("123", "ERROR", 1),
                ("handler:YxWeb::Report::Touchphone || juh", "ERROR", 1),
                ("handler:YxWeb::Report::Touchphone || touch", "TOUCHPHONE_WITHOUT_SEARCHAPP_JSONPROXY", 0),
                (
                    "handler:YxWeb::Report::Touchphone || handler:YxWeb::Report::Searchapp",
                    "TOUCHPHONE_WITHOUT_SEARCHAPP_JSONPROXY",
                    0,
                ),
                (
                    "handler:YxWeb::Report::Touchphone || handler:YxWeb::Report::JSONProxy",
                    "TOUCHPHONE_WITHOUT_SEARCHAPP_JSONPROXY",
                    0,
                ),
                (
                    (
                        "handler:YxWeb::Report::Touchphone || "
                        "handler:YxWeb::Report::JSONProxy || "
                        "handler:YxWeb::Report::Searchapp"
                    ),
                    "OK",
                    0,
                ),
            ]
        ]
    )
    def test_search_check_condition_json(self, query, url, status, error_code, condition):
        query.set_internal()
        query.path.set_url(url)

        data = self.json_request(query).data

        assert data['status'] == status
        assert data['error_code'] == error_code
        assert data['condition'] == condition

    @pytest.mark.parametrize("url", [
        "/search/versions",
        "/search/v",
        "/search/vl"
    ])
    def test_search_versions(self, query, url):
        query.set_internal()
        query.set_url(url)

        resp = self.request(query)

        assert 'report' in resp.content
        assert 'apache' in resp.content

    def test_search_all_supported_params(self, query):
        query.set_internal()
        query.set_url("/search/all-supported-params")

        resp = self.request(query)

        assert 'Параметр' in resp.content
        assert 'json_dump' in resp.content

    def test_search_all_supported_flags(self, query):
        query.set_internal()
        query.set_url("/search/all-supported-flags")

        resp = self.request(query)

        assert 'Описание' in resp.content
        assert 'disable_https' in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_tail_log(self, query):
        query.set_internal()
        query.set_url("/search/tail-log")
        resp = self.request(query)
        assert resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_viewconfig(self, query):
        query.set_internal()
        query.set_url("/search/viewconfig")
        resp = self.request(query)
        assert '<SearchSource>' in resp.content

    @pytest.mark.ticket('SERP-41451')
    @pytest.mark.parametrize("flags", [
        'checkconfig:read-only:1;deep:gg',
        'wrong_param',
        'checkconfig:read-only:1;deep:-1',
    ])
    def test_info_checkconfig_error(self, query, flags):
        query.set_internal()  # TODO external
        query.set_params({'info': flags})
        query.set_url('/search/checkconfig')

        resp = self.request(query)
        assert not resp.source

        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)

        assert root.xpath('//check-config/error')

    @pytest.mark.ticket('SERP-41451')
    @pytest.mark.parametrize("flags", [
        'checkconfig:read-only:1;deep:1',
        'checkconfig:read-only:1;deep:2'
    ])
    def test_info_checkconfig_source_exists(self, query, flags):
        query.set_internal()  # TODO external
        query.set_params({'info': flags})
        query.set_url('/search/checkconfig')

        resp = self.request(query)
        assert not resp.source

        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)

        assert root.xpath('//source')

    @pytest.mark.ticket('SERP-41451')
    @pytest.mark.parametrize("flags", [
        'checkconfig:read-only:1;deep:0'
    ])
    def test_info_checkconfig_deep0(self, query, flags):
        query.set_internal()  # TODO external
        query.set_params({'info': flags})
        query.set_url('/search/checkconfig')

        resp = self.request(query)

        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)

        assert root.xpath('//source') == []

        dict = {}

        for child in root:
            if child.__len__() > 1:
                dict[child[0].text] = child[1].text
            else:
                dict[child[0].text] = ''

        assert 'revision' in dict
        assert dict['host']

    # Internal params work for these user agents
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.task('SERP-62360')
    @pytest.mark.parametrize("ua", [
        'Mozilla/5.0 (Linux; Android 6.0.1; SM-G925F Build/MMB29K; wv) AppleWebKit/537.36 (KHTML, like Gecko) '
        'Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 YandexSearch/7.30 YandexSearchWebView/7.30',
        USER_AGENT_SEARCHAPP_ANDROID,
        USER_AGENT_JSON_PROXY_ANDROID,
        'some other user agent',
        'YandexSearchBrowser',
        'YandexSearch-something'
    ])
    def test_user_agent_internal(self, query, ua):
        query.set_query_type(SEARCH_APP)
        query.set_params({'export': 'json'})
        query.set_internal()
        query.set_user_agent(ua)
        resp = self.request(query)
        assert resp.content.startswith('{')

    # Internal params do not work for these user agents
    @pytest.mark.task('SERP-62360')
    @pytest.mark.parametrize("ua", [
        'Some other Yandex user agent',
        'Some user agent for Yandex',
        'YandexSearchRobot',
        'YandexRobot'
    ])
    def test_robot_user_agent(self, query, ua):
        query.set_params({'export': 'json'})
        query.set_internal()
        query.set_user_agent(ua)
        resp = self.request(query)
        assert not resp.content.startswith('{')

    @pytest.mark.ticket('SERP-60902')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_app_host_timeout(self, query):
        query.add_params({'timeout': '130100'})
        params = self.json_dump_request(query, "search.app_host.sources.(_.name eq 'INPUT' || _.name  eq  'APP_HOST_PARAMS')")
        print(params)
        assert params[0]["results"][0]["timeout"] == 130

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('WEBREPORT-72')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_graphrwr_flag(self, query):
        value = 'test1:test2:80'
        query.add_flags({'graphrwr': value})
        params = self.json_dump_request(query, "search.app_host.sources.(_.name eq 'INPUT' || _.name eq 'APP_HOST_PARAMS')")
        assert value in params[0]["results"][0]["graphrwr"]

    @pytest.mark.skip(reason="RUNTIMETESTS-75")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.task('REPORTINFRA-237')
    def test_people_search_docs_isnt_empty(self, query):
        """
        Выдача на контрольный запрос должна быть.
        Т.е. docs > 0
        """
        dump_key = 'searchdata.docs'
        query.set_url("/people/search/")
        query.set_params({'text': 'Волож', 'lr': 213, 'json_dump': dump_key})
        query.set_internal()

        # for http_adapter (REPORTINFRA-269)
        query.set_timeouts()
        query.set_http_adapter()

        resp = self.request(query)
        data = resp.data
        assert dump_key in data
        assert len(data[dump_key]) > 0

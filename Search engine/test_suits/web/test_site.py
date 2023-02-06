# -*- coding: utf-8 -*-

import re
import pytest
from util.tsoy import TSoY
from util.const import HNDL, TLD, TEXT, PLATFORM, USERAGENT_BY_TYPE, CTXS


class TestSite():
    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-30572')
    @pytest.mark.parametrize(("scheme", 'result_status_code'), [
        ('http', 200),
        ('https', 200)
    ])
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COMTR,
    ])
    @TSoY.yield_test
    def test_site_no_redir_to_http(self, query, tld, scheme, result_status_code):
        """
        SERP-30572 Репорт редиректит с https на http
        Принудительно ставим флаг https и убеждаемся, что получаем 200, а не 302
        """
        query.SetDomain(tld)
        query.SetScheme(scheme)
        query.SetPath(HNDL.SEARCH_SITE)
        query.SetParams({
            'html': 1,
            'topdoc': (
                'https://keaz.ru/help/search?searchid=2124370'
                '&text=%D1%88%D0%B8%D1%80%D0%BE%D0%BA%D0%B8%D0%B9&web=0'
            ),
            'encoding': '',
            'tld': 'ru',
            'htmlcss': '1.x',
            'updatehash': 'true',
            'searchid': '2124370',
            'clid': '',
            'text': TEXT,
            'web': '0',
            'p': '',
            'surl': '',
            'constraintid': '',
            'date': '',
            'within': '',
            'from_day': '',
            'from_month': '',
            'from_year': '',
            'to_day': '',
            'to_month': '',
            'to_year': '',
            'l10n': 'ru',
            'callback': 'jQuery18305933488425875815_1423233158581',
            '_': '1427105835503',
        })

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(("query_type"), [
        (PLATFORM.DESKTOP),
        (PLATFORM.PAD),
        (PLATFORM.TOUCH),
        (PLATFORM.SMART)
    ])
    @TSoY.yield_test
    def test_site_ajax(self, query, query_type):
        query.SetPath(HNDL.SEARCH_SITE)
        query.SetUserAgent(USERAGENT_BY_TYPE[query_type])
        jq_token = 'Ya.Site.Results.triggerResultsDelivered'
        query.SetParams({
            'html': 1,
            'topdoc': (
                'https://keaz.ru/help/search?searchid=2124370'
                '&text=%D1%88%D0%B8%D1%80%D0%BE%D0%BA%D0%B8%D0%B9&web=0'
            ),
            'encoding': '', 'tld': 'ru', 'htmlcss': '1.x', 'updatehash': 'true', 'searchid': '2124370', 'clid': '',
            'text': TEXT, 'web': '0', 'p': '', 'surl': '', 'constraintid': '', 'date': '', 'within': '', 'from_day': '',
            'from_month': '', 'from_year': '', 'to_day': '', 'to_month': '', 'to_year': '', 'l10n': 'ru',
            'callback': jq_token,
            '_': '1427105835503',
        })
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.text.startswith(jq_token + '(')
        assert not resp.text.startswith(jq_token + '()')
        assert resp.text.count(jq_token + '(') == 1
        assert 'JSONP security token invalid' not in resp.text
        assert len(re.findall(r'\)', resp.text)) == len(re.findall(r'\(', resp.text))

    @pytest.mark.ticket('SEARCH-11536')
    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_site_without_text(self, query):
        query.SetPath(HNDL.SEARCH_SITE)
        query.SetUserAgent(USERAGENT_BY_TYPE[PLATFORM.GRANNY])
        jq_token = 'Ya.Site.Results.triggerResultsDelivered'
        query.SetParams({
            'html': 1,
            'topdoc': 'http://uzmovi.com/izlash.html?searchid=2452955&text=&web=0',
            'tld': 'ru',
            'htmlcss': '1.x',
            'updatehash': 'true',
            'searchid': '2452955',
            'text': '',
            'web': '0',
            'l10n': 'ru',
            'callback': jq_token,
            '_': '1427105835503',
            'template': 'granny_exp:phone',
            'touch_to_granny': 1,
        })
        query.SetInternal()
        query.SetRequireStatus(200)

        yield query

    @pytest.mark.ticket('SEARCH-11536')
    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(("web"), [0])
    @TSoY.yield_test
    def test_site_check_headline(self, query, web):
        query.SetPath(HNDL.SEARCH_SITE)
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams({
            'searchid': '3971490',
            'text': 'rivatuner',
            'web': web,
        })
        query.SetInternal()
        query.SetRequireStatus(200)

        resp = yield query
        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        data = tmpl[0]['data']
        assert ('searchdata' in data) and ('docs' in data['searchdata'])
        assert len(data['searchdata']['docs']) and ('headline' in data['searchdata']['docs'][0])

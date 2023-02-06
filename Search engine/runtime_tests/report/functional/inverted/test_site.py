# -*- coding: utf-8 -*-

import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestSite(BaseFuncTest):
    @pytest.mark.skip(reason="RUNTIMETESTS-114")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("tld", [
        RU,
        COMTR,
    ])
    def test_site_no_redir_to_http(self, query, tld):
        """
        SERP-30572 Репорт редиректит с https на http
        Принудительно ставим флаг https и убеждаемся, что получаем 200, а не 302
        """
        query.set_host(tld)
        query.set_url('/search/site/')
        query.set_params({
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
        # FIXME(mvel): REPORTINFRA-276
        query.set_internal()
        query.set_http_adapter()
        query.set_timeouts()

        self.request(query)

    @pytest.mark.skip(reason="RUNTIMETESTS-114")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("query_type", [
        DESKTOP,
        PAD,
        TOUCH,
        SMART,
    ])
    def test_site_ajax(self, query, query_type):
        query.set_url('/search/site/')
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        jq_token = 'Ya.Site.Results.triggerResultsDelivered'
        query.set_params({
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
        # FIXME(mvel): REPORTINFRA-276
        query.set_internal()
        query.set_http_adapter()
        query.set_timeouts()

        resp = self.request(query)

        assert resp.content.startswith(jq_token + '(')
        assert not resp.content.startswith(jq_token + '()')
        assert resp.content.count(jq_token + '(') == 1
        assert 'JSONP security token invalid' not in resp.content
        assert len(re.findall(r'\)', resp.content)) == len(re.findall(r'\(', resp.content))

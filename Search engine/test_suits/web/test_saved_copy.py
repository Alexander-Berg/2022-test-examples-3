# -*- coding: utf-8 -*-

import pytest
import time
import urllib
from util.tsoy import TSoY
from util.const import CTXS


class TestSavedCopy():
    @pytest.mark.ticket('')
    @TSoY.yield_test
    def test_saved_copy_url(self, query):
        """
        SERP-29168 - добавить протухание/смену ключа в подпись СК
        Проверяем, что в ссылке saved_copy есть параметр timestamp и его время=время формирования серпа
        (примерно текущий момент времени - время запроса)
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams({
            'text': 'mediamarkt'
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        data = tmpl[0]['data']
        assert data['searchdata']['docs'], 'No searcdocs here'

        docs_https = filter(lambda d: d['url'].startswith('https://'), data['searchdata']['docs'])
        assert docs_https, 'No https docs found. Tune query please'

        docs_url = filter(lambda d: d.get('signed_saved_copy_url'), docs_https)
        assert docs_url, 'No docs with signed_saved_copy_url found. Tune query please'

        for doc in docs_url:
            url = doc['signed_saved_copy_url']
            assert isinstance(url, str)
            parsed_url = urllib.parse.urlparse(url)
            params_dict = urllib.parse.parse_qs(parsed_url.query)
            assert 'tm' in params_dict

            # проверяем, что время в ссылке - это время формирования серпа
            assert int(params_dict['tm'][0]) < int(time.time() + 10000)
            assert int(params_dict['tm'][0]) > int(time.time() - 10000)

            # TODO SERP-37258 - Показывать СК для https-документов
            assert params_dict['url'][0].startswith('https://')
            # assert url['data']['scheme'] == 'https'

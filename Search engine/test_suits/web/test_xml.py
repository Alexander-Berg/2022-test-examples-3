# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import HNDL, TLD, CTXS, TEXT
from util.params import get_xml_partners, get_xml_banners_old_partners, get_xml_banners_new_partners
from util.helpers import Auth, XmlSchemaValidator
import re
import json
import logging
from lxml import etree

_XML_NEW_BANNER = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><request><query>{query}</query><page>{page}</page><groupings><groupby attr="banner" mode="deep" /></groupings></request>'''

_XML_BANNER = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><request><query>{query}</query><page>{page}</page></request>'''

_XML_SEARCH = """<?xml version="1.0" encoding="utf-8"?><request><query>{query}</query><groupings><groupby attr="d" mode="deep" groups-on-page="10" docs-in-group="1" /></groupings></request>"""


@pytest.mark.parametrize(('method'), [('GET'), ('POST')])
@pytest.mark.parametrize(('path'), [(HNDL.SEARCH_XML)])
class TestXml(Auth):
    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(('is_internal', 'is_auth', 'access_success'), [(False, False, False), (False, True, True), (True, False, True), (True, True, True)])
    @TSoY.yield_test
    def test_xml_200(self, xml_query, xml_auth, is_internal, is_auth, access_success, method, path):
        """
        Условия, при которых XML обязан ответить 200
        Для внешнего запроса без пользователя и ключа выдается ошибка 44
        Для запроса из внутренней сети запрос выполняется без проблем
        """
        xml_query.SetScheme('https')
        if is_internal:
            xml_query.SetInternal()
        else:
            xml_query.SetExternal()
        if is_auth:
            self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        if access_success:
            assert '<error code=' not in resp.text
        else:
            assert '<error code="44">' in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(('is_internal', 'is_auth', 'access_success'), [(False, False, False), (False, True, True)])
    @TSoY.yield_test
    def test_xml_302_307_http_to_https(self, xml_query, xml_auth, is_internal, is_auth, access_success, method, path):
        """
        Условия, при которых XML обязан ответить 302
        Это редирект http => https
        """
        xml_query.SetScheme('http')
        if is_internal:
            xml_query.SetInternal()
        else:
            xml_query.SetExternal()
        if is_auth:
            self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetRequireStatus(307 if xml_query.method == 'POST' else 302)

        resp = yield xml_query

        assert resp.GetLocation().scheme == 'https'
        assert resp.GetLocation().path == xml_query.path

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @TSoY.yield_test
    def test_user_limits(self, xml_query, xml_auth, method, path):
        """
        Проверяем, что ручка лимитов выдает тэг <limits>, в котором теоретически должен находиться список лимитов
        """
        self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetParams({
            'action': 'limits-info',
        })
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        assert '<limits>' in resp.text

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(('get_params', 'post_params', 'regexp'), [
        (
            {'groupby': 'attr=d.mode=1.groups-on-page=38'},
            {'groupings': '<groupby attr="d" mode="deep" groups-on-page="38" docs-in-group="1" />'},
            re.compile(r'^1\.d\.38\.1')
        ),
        (
            {'xml_flat_grouping': 1, 'groupby': 'attr=d'},
            {'xml_flat_grouping': 1, 'groupings': '<groupby attr="d" />'},
            re.compile(r'^0\.\.10\.1.*')
        )
    ])
    @TSoY.yield_test
    def test_xml_grouping(self, xml_query, xml_auth, get_params, post_params, regexp, method, path):
        self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetRequireStatus(200)
        xml_query.SetDumpFilter(resp=[CTXS.NOAPACHE, CTXS.XML_AUTH])

        if xml_query.method == 'POST':
            if 'xml_flat_grouping' in post_params:
                xml_query.SetParams({'xml_flat_grouping': post_params['xml_flat_grouping']})

            xml_query.SetData(
                '<?xml version="1.0" encoding="utf-8"?><request><query>{query}</query><groupings>{groupings}</groupings></request>'.format(
                    query=TEXT,
                    groupings=post_params['groupings']
                )
            )
        else:
            xml_query.SetParams({
                'query': TEXT
            })
            xml_query.SetParams(get_params)

        resp = yield xml_query

        # sometimes recieve error from XML_AUTH - RPS_LIMIT_EXCEEDED
        xml_auth_resp = resp.GetCtxs()['http_response'][0]
        assert 'Content' in xml_auth_resp
        xml_root = etree.fromstring(xml_auth_resp['Content'])
        assert len(xml_root.xpath('/data/errors')) == 0, 'some problems with XML_AUTH. we got limits: {}'.format(xml_auth_resp['Content'])

        def check_grouping(grouping, regexp):
            ok = False
            for g in grouping:
                if regexp.search(g):
                    ok = True
                    break
            assert ok, "got grouping: " + str(grouping)

        noapache_setup = resp.GetCtxs()['noapache_setup'][0]
        assert 'WEB' in noapache_setup['client_ctx']
        assert 'g' in noapache_setup['client_ctx']['WEB']
        check_grouping(noapache_setup['client_ctx']['WEB']['g'], regexp)

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @TSoY.yield_test
    def test_xml_fake_doc(self, xml_query, method, path):
        '''
        xml удаляет из выдачи фейковые документы(колдунщики), в случае когда вся выдача состоит из колдунщиков
        надо выдать ответ, что ничего не найдено
        '''
        text = 'exact_url:"https://rakip1.tv/"'
        if xml_query.method == 'POST':
            xml_query.SetData(_XML_SEARCH.format(
                query=text
            ))
        else:
            xml_query.SetParams({
                'query': text
            })
        xml_query.SetInternal()
        xml_query.SetDomain(TLD.COM)
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        assert '<error code="15">' in resp.text

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @TSoY.yield_test
    def test_xml_page_size(self, xml_query, method, path):
        '''
        Проверяем, что при явном указании размера группы XML-поиск возвращает требуемое количество документов
        '''
        text = 'котики'
        if xml_query.method == 'POST':
            query_data = """<?xml version="1.0" encoding="utf-8"?><request><query>{query}</query>
                <groupings><groupby attr="d" mode="deep" groups-on-page="20" docs-in-group="1" /></groupings>
                </request>"""
            xml_query.SetData(query_data.format(
                query=text
            ))
        else:
            xml_query.SetParams({
                'query': text,
                'groupby': 'attr=d.mode=1.groups-on-page=20'
            })

        xml_query.SetInternal()
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        xml_root = etree.fromstring(resp.content)

        for a in xml_root.xpath('/yandexsearch/response/results/grouping/@first'):
            assert a == '1'

        for a in xml_root.xpath('/yandexsearch/response/results/grouping/@last'):
            assert a == '20'

        res = xml_root.xpath('/yandexsearch/response/results/grouping/group')
        assert len(res) >= 18  # проверяем кол-во результатов. выставляем 18, потому что перл удаляет колдунщики

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @TSoY.yield_test
    def test_xml_page_size_multiple_groupings(self, xml_query, method, path):
        '''
        Проверяем, что при запросе нескольких групп размеры каждой группировки правильные
        '''
        text = 'site:yandex.ru'
        if xml_query.method == 'POST':
            query_data = """<?xml version="1.0" encoding="utf-8"?><request><query>{query}</query>
                <groupings>
                <groupby attr="banner" groups-on-page="1"/>
                <groupby attr="d" mode="deep" groups-on-page="20" docs-in-group="1" />
                </groupings>
                </request>"""
            xml_query.SetData(query_data.format(
                query=text
            ))
        else:
            xml_query.SetParams({
                'query': text,
                'groupby': ['attr=banner.groups-on-page=1', 'attr=d.mode=1.groups-on-page=20']
            })

        xml_query.SetParams({
            'bpage': '593769'
        })
        xml_query.SetInternal()
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        xml_root = etree.fromstring(resp.content)

        groupings = xml_root.xpath('/yandexsearch/response/results/grouping')
        assert len(groupings) == 2

        for grouping in groupings:
            attr = grouping.get('attr')
            for child in grouping:
                if child.tag == 'page':
                    if attr == 'd':
                        assert child.get('first') == '1'
                        assert child.get('last') == '20'
                    elif attr == 'banner':
                        assert child.get('first') == '1'
                        assert child.get('last') == '1'
                    else:
                        assert False

                if child.tag == 'group':
                    for subchild in child:
                        if subchild.tag == 'doccount':
                            assert int(subchild.text) > 0

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize('operator', [
        'site:yandex.ru',
        'host:yandex.ru',
        'rhost:ru.yandex',
        '(url:https://yandex.ru | url:https://yandex.ru/news/ | url:https://yandex.ru/about | url:https://yandex.ru/support/)'
    ])
    @TSoY.yield_test
    def test_xml_siteoperator(self, xml_query, xml_auth, operator, method, path):
        """
        По запросам с операторами, ограничивающими поиск результатами с одного сайта,
        XML-поиск должен разгруппировывать результаты, даже если не указана плоская группировка в &groupby
        """
        self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetParams({'query': 'Яндекс ' + operator})
        xml_query.SetInternal()
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        assert '<error code=' not in resp.text
        xml_root = etree.fromstring(resp.content)
        res = xml_root.xpath('//group')
        assert len(res) >= 3  # проверяем кол-во групп в ответе

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(('asc'), [(True), (False)])
    @pytest.mark.parametrize(('how'), [None, 'rlv', 'tm'])
    @pytest.mark.parametrize(('page'), [None, '0', '1'])
    @TSoY.yield_test
    def test_xml_parameters(self, xml_query, xml_auth, asc, how, page, method, path):
        self.SetXmlAuth(xml_query, xml_auth)
        text = 'test'
        if xml_query.method == 'POST':
            pageNum = '0' if page is None else page
            post_data = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <request>
                <query>{query}</query><page>{page}</page>
                {sortby}
               </request>'''
            sortby = ''
            if how is not None:
                order = ''
                if asc is not None:
                    order = ' order="{}"'.format('ascending' if asc else 'descending')
                sortby = '<sortby{order}>{how}</sortby>'.format(order=order, how=how)

            xml_query.SetData(post_data.format(query=text, page=pageNum, sortby=sortby))
        else:
            xml_query.SetParams({
                'query': text,
            })
            if page:
                xml_query.SetParams({
                    'page': page,
                })
            if asc:
                xml_query.SetParams({
                    'asc': '1',
                })
            if how is not None:
                xml_query.SetParams({
                    'how': how,
                })

        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        assert '<error code="42">' not in resp.text, '{user}: Invalid key. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=xml_auth['user'])
        assert '<error code="43">' not in resp.text, '{user}: Invalid key version. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=xml_auth['user'])
        assert '<error code="33">' not in resp.text, '{user}: Протух IP адрес. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=xml_auth['user'])
        assert '<error code="32">' not in resp.text, '{user}: limit exceeded for user'.format(user=xml_auth['user'])

        assert '<error code=' not in resp.text or '<error code="15">' in resp.text
        xml_root = etree.fromstring(resp.content)
        for e in xml_root.xpath('//request/query'):
            assert text in e.text

        for a in xml_root.xpath('/yandexsearch/request/sortby/@order'):
            expect = 'ascending' if (asc and how == 'tm') else 'descending'
            assert a == expect

        for a in xml_root.xpath('/yandexsearch/request/sortby'):
            assert a.text == 'rlv' if (how is None) else how

        for p in xml_root.xpath('/yandexsearch/request/page'):
            assert p.text == '0' if page is None else page

        res = xml_root.xpath('//group')
        assert len(res)  # проверяем кол-во результатов


class TestXmlGET(Auth):
    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(('method'), [('GET')])
    @pytest.mark.parametrize(('path'), [(HNDL.SEARCH_XML)])
    @pytest.mark.parametrize(('order'), ['ascending', 'descending'])
    @TSoY.yield_test
    def test_xml_cgi_sortby(self, xml_query, xml_auth, method, path, order):
        self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetParams({
            'sortby': 'tm.order={}'.format(order)
        })
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        xml_root = etree.fromstring(resp.content)
        assert [order] == xml_root.xpath('/yandexsearch/request/sortby/@order')


@pytest.mark.parametrize(('method'), [('GET'), ('POST')])
@pytest.mark.parametrize(('path'), [(HNDL.SEARCH_XML)])
class TestXmlPartners(Auth, XmlSchemaValidator):
    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(('text', 'site'), [
        ('1 site:lenta.ru', 'lenta.ru'),
        ('1 site:habrahabr.ru', 'habrahabr.ru'),
        ('1 site:vimeo.com', 'vimeo.com'),
    ])
    @pytest.mark.parametrize(('partner'), get_xml_partners())
    @pytest.mark.parametrize(('page'), [None, '1'])
    @TSoY.yield_test
    def test_xml_partners(self, xml_query, text, site, partner, page, method, path):
        logger = logging.getLogger("test_logger")
        logger.info('Partner: %s',  partner)
        xml_auth = {
            "ip": partner['ip'],
            "user": partner['user'],
            "key": partner['key']
        }
        self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetDomain(partner['tld'])
        if xml_query.method == 'POST':
            pageNum = '0' if page is None else page
            post_data = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <request>
                <query>{query}</query><page>{page}</page>
               </request>'''
            xml_query.SetData(post_data.format(query=text, page=pageNum))
        else:
            xml_query.SetParams({
                'query': text,
            })
            if page:
                xml_query.SetParams({
                    'page': page,
                })

        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        assert '<error code="42">' not in resp.text, '{user}: Invalid key. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="43">' not in resp.text, '{user}: Invalid key version. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="33">' not in resp.text, '{user}: Протух IP адрес. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="32">' not in resp.text, '{user}: limit exceeded for user'.format(user=partner['user'])

        assert '<error code=' not in resp.text or '<error code="15">' in resp.text
        # xml_root = self.parse_xml(resp.content, 'AnswerSchema.xsd')
        xml_root = self.parse_xml(resp.content)
        for e in xml_root.xpath('//request/query'):
            assert text in e.text

        for p in xml_root.xpath('/yandexsearch/request/page'):
            assert p.text == '0' if page is None else page

        res = xml_root.xpath('//group')
        assert len(res)  # проверяем кол-во результатов
        domains_cnt = 0
        for item in res:
            for e in item.xpath('//doc/domain'):
                assert site in e.text
                domains_cnt = domains_cnt + 1
        assert domains_cnt  # Проверить, что были документы с доменами


@pytest.mark.parametrize(('method'), [('GET'), ('POST')])
@pytest.mark.parametrize(('path'), [(HNDL.SEARCH_XML)])
class TestXmlBanners(Auth, XmlSchemaValidator):
    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(('partner'), get_xml_banners_old_partners())
    @pytest.mark.parametrize(('page'), [0, 2])
    @pytest.mark.parametrize(('asc'), [None, '1'])
    @TSoY.yield_test
    def test_old_banner_xml(self, xml_query, partner, page, asc, method, path):
        text = 'пластиковые окна'
        xml_auth = {
            "ip": partner['ip'],
            "user": partner['user'],
            "key": partner['key']
        }
        self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetDomain(partner['tld'])
        xml_query.SetParams({
            'bpage': partner['bpage'],
            'title-length-limit': 10000,
            'stat-id': 8811570
        })
        if xml_query.method == 'POST':
            xml_query.SetData(_XML_BANNER.format(query=text, page=1))
        else:
            xml_query.SetParams({
                'query': text,
                'page': 1,
            })
            if asc is not None:
                xml_query.SetParams({
                    'asc': asc,
                })

        xml_query.SetHeaders({
            'Referer': partner['referer']
        })
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        assert '<error code="42">' not in resp.text, '{user}: Invalid key. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="43">' not in resp.text, '{user}: Invalid key version. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="33">' not in resp.text, '{user}: Протух IP адрес. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="32">' not in resp.text, '{user}: limit exceeded for user'.format(user=partner['user'])

        assert '<error code=' not in resp.text or '<error code="15">' in resp.text
        xml_root = self.parse_xml(resp.content)

        for e in xml_root.xpath('//request/query'):
            assert text in e.text

        # проверяем кол-во баннеров, сейчас у мейла max 4+4 баннеров
        if 'max_banner_count_total' in partner:
            res = xml_root.xpath('/yandexsearch/response/banner/data/child::*[self::direct_premium or self::direct_halfpremium]')
            assert len(res) <= partner['max_banner_count_total']

        if 'max_banner_count_premium' in partner:
            res = xml_root.xpath('/yandexsearch/response/banner/data/child::*[self::direct_premium]')
            assert len(res) <= partner['max_banner_count_premium']

        if 'max_banner_count_halfpremium' in partner:
            res = xml_root.xpath('/yandexsearch/response/banner/data/child::*[self::direct_halfpremium]')
            assert len(res) <= partner['max_banner_count_halfpremium']

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(('partner'), get_xml_banners_new_partners())
    @pytest.mark.parametrize(('page'), [0, 2])
    @TSoY.yield_test
    def test_new_banner_xml(self, xml_query, partner, page, method, path):
        text = 'пластиковые окна'
        xml_auth = {
            "ip": partner['ip'],
            "user": partner['user'],
            "key": partner['key']
        }
        self.SetXmlAuth(xml_query, xml_auth)
        xml_query.SetDomain(partner['tld'])
        xml_query.SetParams({
            'bpage': partner['bpage'],
            'title-length-limit': 10000,
            'stat-id': 8811570
        })
        xml_query.SetHeaders({
            'Referer': partner['referer']
        })
        if xml_query.method == 'POST':
            xml_query.SetData(_XML_NEW_BANNER.format(
                query=text,
                page=page
            ))
        else:
            xml_query.SetParams({
                'query': text,
                'page': page,
                'groupby': 'attr=banner',
            })
        xml_query.SetRequireStatus(200)

        resp = yield xml_query

        assert '<error code="42">' not in resp.text, '{user}: Invalid key. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="43">' not in resp.text, '{user}: Invalid key version. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="33">' not in resp.text, '{user}: Протух IP адрес. See https://wiki.yandex-team.ru/jandeksxml/partners/'.format(user=partner['user'])
        assert '<error code="32">' not in resp.text, '{user}: limit exceeded for user'.format(user=partner['user'])

        assert '<error code=' not in resp.text or '<error code="15">' in resp.text
        xml_root = self.parse_xml(resp.content)

        for e in xml_root.xpath('//request/query'):
            assert text in e.text

        res = xml_root.xpath('/yandexsearch/response/results/grouping/group/doc/properties/data')

        assert len(res)
        assert "\\u0007" not in res[0].text
        jres = json.loads(res[0].text)["data"]

        # проверяем кол-во баннеров
        if 'banner_count_total' in partner:
            assert len(jres["direct_halfpremium"]) + len(jres["direct_premium"]) >= partner['banner_count_total']

        if 'banner_count_premium' in partner:
            assert len(jres["direct_premium"]) >= partner['banner_count_premium']

        if 'banner_count_halfpremium' in partner:
            assert len(jres["direct_halfpremium"]) >= partner['banner_count_halfpremium']

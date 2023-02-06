# -*- coding: utf-8 -*-

import pytest
import os
import re
from lxml import etree

from report.base import BaseReportTest
from report.const import *
import report.functional.web.test_internal as eventlogs

_XML_NEW_BANNER = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<request>
    <query>{text}</query>
    <page>{page}</page>
    <groupings>
        <groupby attr="banner" mode="deep" />
    </groupings>
</request>'''

_XML_BANNER = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<request>
    <query>{text}</query>
    <page>1</page>
</request>'''


class TestWebXML(BaseReportTest):
    xml_partners = [
        (
            'nnov-p', '85.26.168.0', RU,
            '03.15471625:5d42010d58b5070e73fed63deacb19ed.vir.3295de1aed32dbd7f432a1489bb61709'
        ),
        (
            'bleed74', '80.77.174.50', RU,
            '03.15050405:1fdea15e939f8628ea9f9fb5c639eb0c.vir.ad528664356d6b318384fe1046a33814'
        ),
        (
            'pulse-mts', '194.87.13.50', RU,
            '03.130869871:2b328f0cefacc8ce51614f6777b3643f.vir.b1286573b28af534988a7f85895b82e8'
        ),
        (
            'nigmap', '46.182.31.226', RU,
            '03.14679404:b882cda495908b7a404698c246779adb.vir.b3b9ec43b3056ef3e2a502713961c9e5'
        ),
        (
            'rambler-xml', '81.19.94.3', RU,
            '03.120258238:4c52fba4511fe21086d0295effcd32ed.reg:pi:sl:vir:fi.8561f54760bb027a3e05ddfc0474ed22'
        ),
        (
            'start-qip-ru', '213.221.39.73', RU,
            '03.42006458:62aad7a61beb669aae3a66cfca88f495.pi:fv:pv:vir:fi.3d3cbc44f7b121a6cc0d53dd346034c2'
        ),
        (
            'blekko-blekko', '98.158.30.100', COM,
            '03.167323033:82e95e07093e1b2b15c23048cd823ed5.vir.d97da2faefb89de9c1b528ab6f85e981'
        ),
        (
            'ceznam-yaxml-video', '77.75.74.226', COM,
            '03.124772122:'
            'ea0f1debe81e9ebd7d3703e67a758e35.pv:fv:pi:fi:vir:ycom:sl:reg:lr.9ed21937c78b3285fbbfb184d9c3431b'
        ),
        (
            'tutbycom', '178.172.160.23', RU,
            '03.5644717:b2eb4a70877d3cb28de86248e2c04454.pv:fv:pi:fi:vir.5293f3dd943fec69adf761db6e60628a'
        ),
        (
            'scorcher74', '195.95.253.26', RU,
            '03.18584482:bc80fc2b68d38474baf7b943bd3dae63.vir.9a2e6b6c19bb0f0bcddb90aceb64a871'
        ),
        (
            'avtorunews', '217.197.126.54', RU,
            '03.16354736:817a385c954c8fe17043fd054dd01ed8.vir.917ed2a655f03a15eb84bdc78d0883f7'
        ),
        (
            'duck-duck-go', '176.134.116.116', COM,
            '03.117816111:cc9749b9bac7ea8334767031660b0909.vir:ycom.25b99f3438dd6115eb19b827303181df'
        )
    ]
    resp_by_source = {
        'XML_AUTH': '''<?xml version="1.0" encoding="UTF-8"?>
<data servant="xmlsearch-counter" version="0" host="wmc-back04e.search.yandex.net" ''' +
        ''' actions="[accessControl]"  executing-time="[0]" >
<access>
    <access-allowed>true</access-allowed>
    <uid>%s</uid>
    <parallel-video-search>false</parallel-video-search>
    <full-video-search>false</full-video-search>
    <parallel-image-search>false</parallel-image-search>
    <full-image-search>false</full-image-search>
    <show-virus-info>true</show-virus-info>
    <yandex-com-search>false</yandex-com-search>
    <serp-links>false</serp-links>
    <special-region>false</special-region>
    <show-request-locality>false</show-request-locality>
    <advertisement>false</advertisement>
    <parallel-advertisement>false</parallel-advertisement>
</access>
</data>'''
    }

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize('hacker', (0, 1))
    @pytest.mark.parametrize(('text', 'domain'), [
        ('1 site:lenta.ru', 'lenta.ru'),
        ('1 site:habrahabr.ru', 'habrahabr.ru'),
        ('1 site:vimeo.com', 'vimeo.com'),
    ])
    # данные вот отсюда https://wiki.yandex-team.ru/jandeksxml/partners/
    # чтобы обновились данные обращаться к trifonova-j@yandex-team.ru
    @pytest.mark.parametrize(('user', 'ip', 'tld', 'key'), xml_partners)
    def test_web_xml(self, query, class_static_data_dir, user, ip, tld, key, text, domain, hacker):
        query.set_url(SEARCH_XML)
        query.set_method('POST')
        query.set_content_type('text/plain; charset=UTF-8')
        query.set_custom_headers({'X-Req-Id': '1448377292092299-1636191000294216473'})
        query.set_post_params('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<request>
    <query>%s</query>
    <page>0</page>
</request>''' % text)

        query.set_host(tld)
        query.set_params({'user': user, 'key': key})
        if hacker:
            query.add_params({'i-m-a-hacker': ip})
            query.set_internal()
        else:
            query.headers.set_forward_for_y(ip)

        # omit unanswers
        def __success(resp):
            assert '<error code=' not in resp.data.content or '<error code="15">' in resp.data.content

        resp = self.request(
            query, success_on=__success,
            sources=[('XML_AUTH', self.resp_by_source['XML_AUTH'] % key.split('.')[1].split(':')[0])]
        )

        # "Nothing found" is a problem, don't skip it
        # https://st.yandex-team.ru/SEARCHPRODINCIDENTS-2872#1510576659000
        # if '<error code="15">' in resp.content:
        #    pytest.skip('hamster unanswer')

        schema = etree.XMLSchema(file=os.path.join(class_static_data_dir, 'AnswerSchema.xsd'))
        parser = etree.XMLParser(schema=schema)

        root = etree.fromstring(resp.content, parser)

        # проверяем, что запрос присутствует в ответе
        assert text in resp.content
        # проверяем релевантный сайт найден
        assert domain in resp.content

        # проверяем, что нет внутренних параметров в выдаче
        invalid_nodes = [
            'internal_nodes=_IsFake', '_HilitedUrl', '_Markers', '_MetaSearcherHostname',
            '_MimeType', '_SearcherHostname', 'geo', 'geoa'
        ]
        for node in invalid_nodes:
            res = root.xpath('//%s' % node)
            if type(res) == bool:
                assert res is False, 'Found invalid node(for internal use only): %s' % node
            else:
                assert len(res) == 0, 'Found invalid node(for internal use only): %s' % node

        res = root.xpath('//group')
        assert len(res)  # проверяем кол-во результатов

    xml_partners_adv = [
        (
            'gomailru-ads',
            '217.69.143.2',
            RU,
            '03.217912896:d5fd65030a576a73c9c47b7fa2b98c8a.adv:pi:fi:vir.661d1c2c181705241593d5a5e59d5d0c',
            249819,
            'https://go.mail.ru/api/v1/web_search?'
            'fr=chxtn12.0.23&gp=811570'
            '&q=%D1%81%D0%BA%D0%B0%D1%87%D0%B0%D1%82%D1%8C%20dropbox%20%D0%B4%D0%BB%D1%8F%20windows%208'
            '&sbmt=1503978582745&csp_nonce=EGMkDGMcifkiyqcckGEhA5EAazFh7VkKVrmIHs9X',
            4,
            4
        )
    ]

    @pytest.mark.skip(reason='Mail go away, but they want come back')
    @pytest.mark.parametrize(('user', 'ip', 'tld', 'key', 'bpage', 'referer', 'banner_count_premium', 'banner_count_halfpremium'), xml_partners_adv)
    @pytest.mark.parametrize('hacker', (0, 1,))
    @pytest.mark.parametrize('post', (0, 1))
    def test_banner_xml(
        self, query, class_static_data_dir, user, ip, tld, key, bpage, referer, banner_count_premium, banner_count_halfpremium, hacker, post
    ):
        text = 'пластиковые окна'
        query.set_url(SEARCH_XML)

        query.set_content_type('text/plain; charset=UTF-8')
        query.set_custom_headers({
            'X-Req-Id': '1448377292092299-1636191000294216473',
            'Referer': referer,
            'X-Yandex-Internal-Request': 1
        })
        query.set_host(tld)
        query.set_params({
            'user': user, 'key': key, 'bpage': bpage, 'title-length-limit': 10000, 'stat-id': 8811570,
        })

        if post:
            query.set_method('POST')
            query.set_post_params(_XML_BANNER.format(text=text))
        else:
            query.add_params({
                'query': text,
                'page': 1,
            })

        if hacker:
            query.add_params({'i-m-a-hacker': ip})
            query.set_internal()
        else:
            query.headers.set_forward_for_y(ip)

        resp = self.request(query)

        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)

        # проверяем, что запрос присутствует в ответе
        assert text in resp.content

        res = root.xpath(
            '/yandexsearch/response/banner/data/child::*[self::direct_premium or self::direct_halfpremium]'
        )
        # проверяем кол-во баннеров, сейчас у мейла max 4+4 баннеров
        #assert len(res) >= banner_count
        res = root.xpath('/yandexsearch/response/banner/data/child::*[self::direct_premium]')
        assert len(res) and len(res) <= banner_count_premium

        res = root.xpath('/yandexsearch/response/banner/data/child::*[self::direct_halfpremium]')
        assert len(res) and len(res) <= banner_count_halfpremium

    xml_partners_adv = [
        (
            'rambler-p',
            '81.19.94.3',
            RU,
            '03.120143188:06d64c27fbb25757a8ffbfd0806b0484.adv:padv:pi:fi:vir:sl:reg.884cebc49e9dc1741a9546c6a180605b',
            324760,
            'http://rambler.ru',
            8
        )
    ]

    @pytest.mark.skip(reason='RUNTIMETESTS-114')
    @pytest.mark.parametrize(
        ('user', 'ip', 'tld', 'key', 'bpage', 'referer', 'banner_count'), xml_partners_adv
    )
    @pytest.mark.parametrize('page', (0, 2))
    @pytest.mark.parametrize('hacker', (0, 1,))
    @pytest.mark.parametrize('post', (0, 1,))
    def test_new_banner_xml(
        self, query, class_static_data_dir, user, ip, tld, key, bpage, referer,
        banner_count, hacker, post, page
    ):
        text = 'пластиковые окна'
        query.set_url(SEARCH_XML)

        query.set_content_type('text/plain; charset=UTF-8')
        query.set_custom_headers({
            'X-Req-Id': '1448377292092299-1636191000294216473',
            'Referer': referer,
            'X-Yandex-Internal-Request': 1
        })
        query.set_host(tld)
        query.set_params({
            'user': user,
            'key': key,
            'bpage': bpage,
            'title-length-limit': 10000,
            'stat-id': 8811570,
        })

        if post:
            query.set_method('POST')
            query.set_post_params(
                _XML_NEW_BANNER.format(
                    text=text,
                    page=page,
                )
            )
        else:
            query.add_params({
                'query': text,
                'page': page,
                'groupby': 'attr=banner',
            })

        if hacker:
            query.add_params({'i-m-a-hacker': ip})
            query.set_internal()
        else:
            query.headers.set_forward_for_y(ip)

        resp = self.request(query)
        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)

        # проверяем, что запрос присутствует в ответе
        assert text in resp.content

        res = root.xpath('/yandexsearch/response/results/grouping/group/doc/properties/data')
        assert not("\\u0007" in res[0].text)
        jres = json.loads(res[0].text)["data"]

        # проверяем кол-во баннеров
        assert len(jres["direct_halfpremium"]) + len(jres["direct_premium"]) >= banner_count

    # проверяем что параметры доходят до БК
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(
        ('user', 'ip', 'tld', 'key', 'bpage', 'referer', 'banner_count'), xml_partners_adv
    )
    @pytest.mark.parametrize('page', (0, 2))
    @pytest.mark.parametrize('post', (0, 1,))
    @pytest.mark.parametrize('x_real_ip', (None, '8.8.8.8'))
    @pytest.mark.parametrize('x_city_id', ('2'))
    @pytest.mark.parametrize('lr', (None, '143'))
    def test_params_banner_xml(
        self, query, class_static_data_dir, user, ip, tld, key, bpage, referer,
        banner_count, post, page, x_real_ip, x_city_id, lr
    ):
        text = 'пластиковые окна'
        query.set_url(SEARCH_XML)
        query.set_query_type(XML)

        query.set_content_type('text/plain; charset=UTF-8')
        custom_headers = {
            'X-Req-Id': '1448377292092299-1636191000294216473',
            'Referer': referer,
            'X-Yandex-Internal-Request': 1,
            'X-Forwarded-For': '9.9.9.9',
            'X-Region-Id': x_city_id,
            'X-Region-City-Id': x_city_id,
        }
        if x_real_ip:
            custom_headers["X-Real-IP"] = x_real_ip
        query.set_custom_headers(custom_headers)
        query.set_host(tld)
        query.set_params({
            'user': user,
            'key': key,
            'bpage': bpage,
            'raw_dump': 'eventlog',
            'dump_source_request':'YABS',
            'init_meta': 'pass-yabs-request-to-report',
            'lr': lr
        })

        special_params = {
            'title-length-limit': 10000,
            'stat-id': 8811570,
            'guid': 'asdasd234234',
            'title-font-id': 1,
            'title-font-size': 2,
            'title-pixel-length-limit': 3,
            'ext-uniq-id': 4,
        }
        query.add_params(special_params)

        if post:
            query.set_method('POST')
            query.set_post_params(
                _XML_NEW_BANNER.format(
                    text=text,
                    page=page,
                )
            )
        else:
            query.add_params({
                'query': text,
                'page': page,
                'groupby': 'attr=banner',
            })

        query.headers.set_forward_for_y(ip)

        resp = self.request(query)
        found = 0

        for line in resp.content.splitlines():
            #XXX what is this?
            #re.sub('\\\\.', line, '\\$')

            m = re.search('TSourceRequest\tYABS\t([^\t]+)', line)
            if m:
                found = 1
                line = m.group(1)
                #,"content":"\u001F▒\b\u0000\u0000\u0000\u0000\u0000\u0004\u0003▒▒▒n▒0\u0010▒_▒▒5Tȫ▒*d▒M▒\u0012'Ŧ▒(▒▒;▒t▒Jժ\u0017▒▒\u0004gf|▒▒wcK▒3;▒X\u0018فq▒Z#8~▒▒▒[.[V▒▒e▒\u000>cp%▒▒▒▒>\u001C\u001F▒▒_▒D▒Bꞗi*e▒)▒▒▒▒▒▒▒▒!▒[B▒r▒▒}▒▒▒MK▒d+v^▒▒wK\fo]▒▒▒▒▒5gkuפt▒U▒tT▒a▒C▒{\u0017▒▒\u0013▒▒Y▒X0\\kk\u0001\u001▒7▒\t3▒|▒l▒▒ĸ>▒▒i▒r▒L▒ص▒▒]|ş▒\b▒▒$:▒▒\n-▒▒▒▒i4▒\u001A-4VY٪R▒▒ǭ\u0016▒▒▒\u0003▒R▒▒▒▒\u0001[▒`\u001A\u001F9▒▒▒#▒u▒ȷY▒▒\b▒▒G▒p.Z▒▒Y▒ш▒ ▒▒\\(\u0001▒▒ ;k(W\u0004▒▒\u0018k▒FhkJ▒46▒,▒Rjl▒\u0014\n▒Ѝ5▒ߌ\f^▒▒▒\u001D~{CuaR▒▒▒Pr▒T▒▒▒g▒\"(O.Dv▒▒▒1\u0013y▒▒\u0007▒Ȟ\u001DF▒b▒|▒d▒\u001Dgwy▒Ɛ▒▒\u0003b\n*6▒▒z▒▒▒▒?2ZYL▒K.▒|\u000F▒▒Èh\u000E▒Ȳ\"}▒▒mŴ▒u▒\u0000a▒\u0017▒\u0013\b\u0000\u0000"}
                # Костыль, потому, что Аркадийный JSON совместим с бинарными данными
                p1 = line.find('"content":"')
                p2 = line.rfind('"}', p1)
                if p2 < 0:
                    p2 = line.find('",', p1)
                if p2 > p1+11:
                    line = line[0:p1+11] + line[p2:len(line)]
                # Конец костыля
                jdata = json.loads(line)
                if x_real_ip:
                    for item in jdata["headers"]:
                        if item[0].lower() == "x-real-ip":
                            assert item[1] == x_real_ip;

                for (k,v) in special_params.iteritems():
                    m1 = re.search(k+'=([^&]+)', jdata["path"])
                    assert m1 and m1.group(1)==str(v), "in key: "+k

                m1 = re.search('tune-region-id=([^&]+)', jdata["path"])
                assert m1 and m1.group(1)==lr if lr else x_city_id, "in key: tune-region-id"

                break
        assert found, 'Can not find marker TSourceRequest'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_web_xml_hacker_bad(self, query):
        '''
        параметр i-m-a-hacker работает только из внутренней сети
        '''
        text = 'test search'
        (user, ip, tld, key) = self.xml_partners[0]

        query.set_url(SEARCH_XML)
        query.set_method('POST')
        query.set_content_type('text/plain; charset=UTF-8')
        query.set_custom_headers({'X-Req-Id': '1448377292092299-1636191000294216473'})
        query.set_post_params('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<request>
    <query>%s</query>
    <page>0</page>
</request>''' % text)

        query.set_host(tld)
        query.set_params({'user': user, 'key': key})
        query.add_params({'i-m-a-hacker': ip})
        query.set_external()

        _XML_DATA = '''<?xml version="1.0" encoding="UTF-8"?>
<data servant="xmlsearch-counter" version="0" host="wmc-back03g.search.yandex.net" actions="[accessControl]"  executing-time="[0]" ><errors><error code="33"><code>QUERY_FROM_FORBIDDEN_IP</code><message>Allowed IP addresses are: 85.26.168.0/28, 85.26.168.0 u, 78.25.83.192/28, 194.226.54.0/24, that doesn&apos;t match user&apos;s IP: 8.8.8.8</message><user-text>Allowed IP addresses are: 85.26.168.0/28, 85.26.168.0 u, 78.25.83.192/28, 194.226.54.0/24, that doesn&apos;t match user&apos;s IP: 8.8.8.8</user-text><ip>8.8.8.8</ip><exception>ru.yandex.wmtools.common.error.UserException: Allowed IP addresses are: 85.26.168.0/28, 85.26.168.0 u, 78.25.83.192/28, 194.226.54.0/24, that doesn&apos;t match user&apos;s IP: 8.8.8.8</exception></error></errors></data>'''  # noqa

        resp = self.request(query, sources=[('XML_AUTH', _XML_DATA)])

        assert '<error code="33">' in resp.content

    def test_xml_fake_doc(self, query):
        '''
        xml удаляет из выдачи фейковые документы(колдунщики), в случае когда вся выдача состоит из колдунщиков
        надо выдать ответ, что ничего не найдено
        '''
        text = 'exact_url:"https://rakip1.tv/"'
        query.set_url(SEARCH_XML)
        query.set_internal()
        query.set_host(COM)

        query.set_params({
            'query': text,
        })

        resp = self.request(query)
        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)
        res = root.xpath('/yandexsearch/response/error[@code]')
        assert res
        assert res[0].get("code") == "15"

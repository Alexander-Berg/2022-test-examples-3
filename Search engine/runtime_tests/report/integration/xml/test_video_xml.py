# -*- coding: utf-8 -*-

import pytest
import os
from lxml import etree

from report.base import BaseReportTest
from report.const import *

class TestVideoXML(BaseReportTest):

    @pytest.mark.parametrize(('text', 'domain'), [
        ('lenta',     'lenta.ru'),
        ('хабрахабр', 'habrahabr.ru'),
        ('vimeo',     'vimeo.com'),
    ])
    @pytest.mark.parametrize(('user', 'ip', 'tld','key'), [
        ('start-qip-ru','213.221.39.73',    RU,     '03.42006458:62aad7a61beb669aae3a66cfca88f495.pi:fv:pv:vir:fi.3d3cbc44f7b121a6cc0d53dd346034c2'),
        ('ceznam-yaxml-video', '77.75.74.226', COM, '03.124772122:ea0f1debe81e9ebd7d3703e67a758e35.pv:fv:pi:fi:vir:ycom:sl:reg:lr.9ed21937c78b3285fbbfb184d9c3431b'),
        ('tutbycom',    '178.172.160.23',   RU,     '03.5644717:b2eb4a70877d3cb28de86248e2c04454.pv:fv:pi:fi:vir.5293f3dd943fec69adf761db6e60628a'),
    ])
    def test_video_xml(self, query, class_static_data_dir, user, ip, tld, key, text, domain):
        # данные вот отсюда https://wiki.yandex-team.ru/jandeksxml/partners/
        # чтобы обновились данные пинать naou@yandex-team.ru
        query.set_host(tld)
        query.set_url(SEARCH_XML)
        query.headers.set_custom_headers({'Content-Type': 'text/plain; charset=UTF-8', 'X-Req-Id': '1448377292092299-1636191000294216473'})
        query.set_params({'user': user, 'key': key, 'type': 'video', 'text': text})
        query.headers.set_forward_for_y(ip)

        resp = self.request(query)

        schema = etree.XMLSchema(file=os.path.join(class_static_data_dir, 'VideoExternalValidation.xsd'))
        parser = etree.XMLParser(schema = schema)

        # собственно проверка. Возбуждает исключения в случае несоответствия xml схеме.
        etree.fromstring(resp.content, parser)


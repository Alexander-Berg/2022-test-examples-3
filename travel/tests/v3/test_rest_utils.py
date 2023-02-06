# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, timedelta

import mock
from lxml import etree
from rest_framework.renderers import JSONRenderer, BrowsableAPIRenderer, StaticHTMLRenderer

from travel.rasp.api_public.api_public.v3.rest_utils import IgnoreClientContentNegotiation, ApiXMLRender
from travel.rasp.api_public.tests.v3.factories import create_request


def test_select_renderer():
    renders = [JSONRenderer(), ApiXMLRender(), BrowsableAPIRenderer(), StaticHTMLRenderer()]

    request = create_request(GET={'debug': 'true'})
    ignore_client_nego = IgnoreClientContentNegotiation()
    render, render_type = ignore_client_nego.select_renderer(request, renders)
    assert isinstance(render, BrowsableAPIRenderer)

    request = create_request(GET={'format': 'xml'})
    ignore_client_nego = IgnoreClientContentNegotiation()
    render, render_type = ignore_client_nego.select_renderer(request, renders)
    assert isinstance(render, ApiXMLRender)

    request = create_request()
    request.META['HTTP_ACCEPT'] = 'application/xml'
    ignore_client_nego = IgnoreClientContentNegotiation()
    render, render_type = ignore_client_nego.select_renderer(request, renders)
    assert isinstance(render, ApiXMLRender)

    request = create_request()
    request.META['HTTP_ACCEPT'] = 'application/json'
    ignore_client_nego = IgnoreClientContentNegotiation()
    render, render_type = ignore_client_nego.select_renderer(request, renders)
    assert isinstance(render, JSONRenderer)

    with mock.patch('travel.rasp.api_public.api_public.v3.rest_utils.IgnoreClientContentNegotiation.URLS_WITHOUT_RENDER',
                    new_callable=mock.PropertyMock) as m_urls:
        url = '/without_render/'
        m_urls.return_value = [url]
        request = create_request()
        request.path = url
        ignore_client_nego = IgnoreClientContentNegotiation()
        render, render_type = ignore_client_nego.select_renderer(request, renders)
        assert isinstance(render, StaticHTMLRenderer)
        assert render_type == 'application/json'

    for get_dict in [{}, {'format': 'json'}, {'format': '123'}]:
        request = create_request(GET=get_dict)
        ignore_client_nego = IgnoreClientContentNegotiation()
        render, render_type = ignore_client_nego.select_renderer(request, renders)
        assert isinstance(render, JSONRenderer)

    for get_dict, accept, expected_render in [({'format': 'json'}, 'application/xml', JSONRenderer),
                                              ({'format': 'xml'}, 'application/json', ApiXMLRender)]:
        request = create_request(GET=get_dict)
        request.META['HTTP_ACCEPT'] = accept
        ignore_client_nego = IgnoreClientContentNegotiation()
        render, render_type = ignore_client_nego.select_renderer(request, renders)
        assert isinstance(render, expected_render)


class TestApiXMLRender(object):
    xml_render = ApiXMLRender()

    def test_none_value(self):
        data = {'aaa': {'bbb': None}}
        xml = etree.fromstring(self.xml_render.render(data))
        assert xml.xpath('/response/aaa/bbb')[0].text == 'xsi:nil="true"'

    def test_bool_value(self):
        data = {'aaa': {'bbb': True}}
        xml = etree.fromstring(self.xml_render.render(data))
        assert xml.xpath('/response/aaa/bbb')[0].text == 'true'

        data = {'aaa': {'bbb': False}}
        xml = etree.fromstring(self.xml_render.render(data))
        assert xml.xpath('/response/aaa/bbb')[0].text == 'false'

    def test_plural(self):
        data = {'countries': [{'title': 'country_1'}, {'title': 'country_2'}]}
        xml = etree.fromstring(self.xml_render.render(data))
        assert len(xml.findall('countries')) == 0

        assert len(xml.xpath('/response/country')) == 2
        countries = xml.findall('country')
        assert {country.find('title').text for country in countries} == {'country_1', 'country_2'}

    def test_date_time_value(self):
        date_time = datetime(2000, 1, 1, 10, 20, 30)
        data = {'object': {
            'date_time': date_time,
            'time': date_time.time(),
            'date': date_time.date(),
            'time_delta': timedelta(days=1, minutes=1, seconds=1, hours=1)
        }}

        xml = etree.fromstring(self.xml_render.render(data))
        assert xml.xpath('/response/object/date_time')[0].text == '2000-01-01 10:20:30'
        assert xml.xpath('/response/object/date')[0].text == '2000-01-01'
        assert xml.xpath('/response/object/time')[0].text == '10:20'
        assert xml.xpath('/response/object/time_delta')[0].text == str(84600 + 3600 + 60 + 1)

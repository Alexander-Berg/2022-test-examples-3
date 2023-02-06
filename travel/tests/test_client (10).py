# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty

from travel.rasp.library.python.api_clients.krasnodar_bus.client import KrasnodarBusClient

HOST = 'http://test_krasnodar_bus.ru/'

DEST_RESPONSE = '''<?xml version="1.0" encoding="KOI8-R"?>
<response>
<destination id="10766">Абадзехская АК                </destination>
</response>
'''.encode('koi8-r')


def _get_client():
    return KrasnodarBusClient(
        host=HOST,
        login='fake_login',
        password='fake_pasword',
        timeout=1
    )


def _register_url(url_suffix, response_body=None, status_code=200):
    def request_callback(request, uri, response_headers):
        return [status_code, response_headers, response_body]

    httpretty.register_uri(
        httpretty.GET, '{}{}'.format(HOST, url_suffix), status=status_code,
        body=request_callback, content_type='text/xml'
    )


@httpretty.activate
def test_create_order():
    client = _get_client()
    _register_url(
        url_suffix='cgi-bin/sale.cgi',
        response_body=DEST_RESPONSE,
    )

    result = client.dest(241)
    assert len(result) == 1
    child = result[0]
    assert child.text.strip() == 'Абадзехская АК'
    assert child.attrib['id'] == '10766'

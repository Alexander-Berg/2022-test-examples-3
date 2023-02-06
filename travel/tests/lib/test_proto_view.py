# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import ujson

from django.test.client import RequestFactory

from travel.rasp.wizards.suburban_wizard_api.lib.proto_view import proto_view
from travel.rasp.wizards.wizard_lib.protobuf_models.urls_pb2 import Urls


def test_no_content():
    @proto_view('foo/bar')
    def empty_view(request):
        return None

    assert empty_view(RequestFactory().get('/')).status_code == 204

    response = empty_view(RequestFactory().get('/', {'json': 1}))
    response_data = ujson.loads(response.content)
    assert response['content-type'] == 'application/json'
    assert response_data == {'error': 'segments not found'}


def test_proto_content():
    @proto_view('foo/bar')
    def urls_view(request):
        return Urls(Desktop='foo', Mobile='bar')

    response = urls_view(RequestFactory().get('/'))
    urls_proto = Urls()
    urls_proto.ParseFromString(response.content)
    assert response['content-type'] == 'foo/bar'
    assert urls_proto.Desktop == 'foo'
    assert urls_proto.Mobile == 'bar'

    response = urls_view(RequestFactory().get('/', {'json': 1}))
    response_data = ujson.loads(response.content)
    assert response['content-type'] == 'application/json'
    assert response_data == {'Desktop': 'foo', 'Mobile': 'bar'}

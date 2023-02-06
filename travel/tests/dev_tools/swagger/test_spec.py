# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from collections import OrderedDict

from django.conf.urls import url

from common.dev_tools.swagger import build_spec
from common.tests.dev_tools.swagger.spec import some_view


def test_build_spec():
    urlpatterns = [
        url(r'^some_view/$', some_view),
    ]

    spec = build_spec(
        {
            'title': 'foo',
            'version': '1.0.0',
            'info': {'description': 'АПИ для бэкофиса'},
        },
        urlpatterns,
        ['common.tests.dev_tools.swagger.spec']
    )
    assert spec.to_dict() == {
        'info': {
            'version': u'1.0.0',
            'description': 'АПИ для бэкофиса',
            'title': u'foo'
        },
        'paths': OrderedDict([(u'/some_view/', {})]),
        'parameters': {},
        'tags': [],
        'definitions': {
            'SomeSchema': {
                'properties': {
                    'foo': {u'type': u'string'},
                    'bar': {u'type': u'number'}},
                'type': 'object'}},
        'swagger': '2.0'
    }

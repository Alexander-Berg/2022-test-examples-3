# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

from django.test.client import RequestFactory

from travel.rasp.morda_backend.morda_backend.middleware.set_exps_flags import SetExpsFlags


def test_set_exps_flags():
    factory = RequestFactory()
    middleware = SetExpsFlags()

    request = factory.get('/search', HTTP_X_YA_UAAS_EXPERIMENTS=json.dumps({'banner_hotels': 'enabled'}))
    middleware.process_request(request)
    assert request.exps_flags == {'banner_hotels'}

    request = factory.get('/search', HTTP_X_YA_UAAS_EXPERIMENTS=json.dumps({'banner_hotels': 'enabled', 'new_flag': 'enabled'}))
    middleware.process_request(request)
    assert request.exps_flags == {'banner_hotels', 'new_flag'}

    request = factory.get('/search', HTTP_X_YA_UAAS_EXPERIMENTS=json.dumps({'banner_hotels': '42', 'new_flag': 'enabled'}))
    middleware.process_request(request)
    assert request.exps_flags == {'new_flag'}

    request = factory.get('/search')
    middleware.process_request(request)
    assert hasattr(request, 'exps_flags') is False

    request = factory.get('/search',  HTTP_X_YA_UAAS_EXPERIMENTS=['trash'])
    middleware.process_request(request)
    assert hasattr(request, 'exps_flags') is False

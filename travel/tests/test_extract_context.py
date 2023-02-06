# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.test import RequestFactory
from ylog.context import get_log_context

from travel.rasp.library.python.common23.middleware.extract_context import ExtractRequestMiddleware
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


def extractor(request):
    return {
        'param': request.META['HTTP_X_REQUEST_ID']
    }


@replace_setting('PYLOGCTX_REQUEST_EXTRACTOR', extractor)
@pytest.mark.parametrize('wsgi_request, ctx', [
    (RequestFactory(HTTP_X_REQUEST_ID='20').get('/asdfs'), {'param': '20'}),
    (RequestFactory().get('/asdfs'), {}),  # Ignore Key Error
])
def test_extract_context(wsgi_request, ctx):
    ExtractRequestMiddleware().process_request(wsgi_request)
    assert get_log_context() == ctx
    ExtractRequestMiddleware().process_response(None, None)
    assert get_log_context() == {}

# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import base64
import json

import pytest
from django.conf.urls import url
from django.test import Client, override_settings
from rest_framework.response import Response

from common.apps.train_order.models import BackofficeUser
from travel.rasp.train_api.train_purchase.backoffice.base import backoffice_api_view


@backoffice_api_view()
def restricted_view(request):
    return Response({})


@backoffice_api_view(admin_only=True)
def admin_view(request):
    return Response({})


urlpatterns = [
    url(r'^restricted-view/$', restricted_view),
    url(r'^admin-view/$', admin_view),
]


@pytest.mark.dbuser
@pytest.mark.parametrize('is_active, is_admin, admin_only, expected_status_code', (
    (False, False, False, 403),  # noqa
    (False, False, True,  403),  # noqa
    (False, True,  False, 403),  # noqa
    (False, True,  True,  403),  # noqa
    (True,  False, False, 200),  # noqa
    (True,  False, True,  403),  # noqa
    (True,  True,  False, 200),  # noqa
    (True,  True,  True,  200),  # noqa
))
def test_backoffice_api_view(is_active, is_admin, admin_only, expected_status_code):
    BackofficeUser.objects.create(username='yndx.yaemployee', is_active=is_active, is_admin=is_admin)
    with override_settings(ROOT_URLCONF=__name__):
        response = Client().get(
            '/admin-view/' if admin_only else '/restricted-view/',
            HTTP_AUTHORIZATION=base64.b64encode(json.dumps({'type': 'direct', 'login': 'yndx.yaemployee'}))
        )
    assert response.status_code == expected_status_code

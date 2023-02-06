# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import base64
import json

import pytest
from django.conf.urls import url
from django.test import Client, override_settings
from hamcrest import assert_that, has_entries
from rest_framework.response import Response
from ylog.context import get_log_context

from common.apps.train_order.models import BackofficeUser
from travel.rasp.train_api.train_purchase.backoffice.base import backoffice_api_view


@backoffice_api_view()
def get_context(request):
    return Response(get_log_context())


urlpatterns = [
    url(r'^get-context/$', get_context),
]


@pytest.mark.dbuser
def test_backoffice_user_in_context():
    username = 'yndx.yaemployee'
    BackofficeUser.objects.create(username=username, is_active=True)
    with override_settings(ROOT_URLCONF=__name__):
        response = Client().get(
            '/get-context/',
            HTTP_AUTHORIZATION=base64.b64encode(json.dumps({'type': 'direct', 'login': username}))
        )
    assert response.status_code == 200
    assert_that(response.data, has_entries(backoffice_user=username))

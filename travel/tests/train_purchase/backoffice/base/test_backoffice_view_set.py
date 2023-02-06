# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import base64
import json

import pytest
from django.test import Client, override_settings
from rest_framework.response import Response
from rest_framework.routers import SimpleRouter

from common.apps.train_order.models import BackofficeUser
from travel.rasp.train_api.train_purchase.backoffice.base import BackofficeViewSet, BackofficeAdminViewSet


class RestrictedViewSet(BackofficeViewSet):
    def retrieve(self, request, pk):
        return Response({})


class AdminViewSet(BackofficeAdminViewSet):
    def retrieve(self, request, pk):
        return Response({})


view_set_router = SimpleRouter()
view_set_router.register('restricted', RestrictedViewSet, base_name='restricted')
view_set_router.register('admin', AdminViewSet, base_name='admin')

urlpatterns = view_set_router.urls


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
def test_backoffice_view_set(is_active, is_admin, admin_only, expected_status_code):
    BackofficeUser.objects.create(username='yndx.yaemployee', is_active=is_active, is_admin=is_admin)
    with override_settings(ROOT_URLCONF=__name__):
        response = Client().get(
            '/admin/1/' if admin_only else '/restricted/1/',
            HTTP_AUTHORIZATION=base64.b64encode(json.dumps({'type': 'direct', 'login': 'yndx.yaemployee'}))
        )
    assert response.status_code == expected_status_code

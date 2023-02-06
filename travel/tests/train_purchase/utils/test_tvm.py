# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from collections import namedtuple

import mock
import pytest
from django.conf.urls import url
from django.core.urlresolvers import reverse
from django.test import Client, override_settings
from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from travel.rasp.train_api.train_purchase.utils import tvm
from travel.rasp.train_api.train_purchase.utils.tvm import check_service_ticket


@api_view(['GET'])
@check_service_ticket(allowed_tvm_service_ids=('good',))
def view_ok(request):
    return Response('ok', status=status.HTTP_200_OK)


urlpatterns = [url(r'^ok/$', view_ok, name='view_ok')]

ServiceTicket = namedtuple('ServiceTicket', 'src')


@override_settings(ROOT_URLCONF=__name__)
@pytest.mark.parametrize('service_ticket, headers', [
    (ServiceTicket('good'), {}),
    (ServiceTicket('bad'), {}),
    (ServiceTicket('bad'), {'HTTP_X_YA_SERVICE_TICKET': 'some-ticket'}),
    (None, {'HTTP_X_YA_SERVICE_TICKET': 'some-ticket'}),
    (None, {}),
])
@mock.patch.object(tvm, 'tvm_factory', autospec=True)
def test_403_forbidden(m_tvm_factory, service_ticket, headers):
    m_tvm_factory.return_value.get_provider.return_value.check_service_ticket.return_value = service_ticket

    response = Client().get(
        path=reverse('view_ok'),
        **headers
    )

    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert 'errors' in response.data

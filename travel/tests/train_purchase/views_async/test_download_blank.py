# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
import requests
from bson import ObjectId
from django.test import Client
from django.test import override_settings
from hamcrest import assert_that
from hamcrest.library.collection.isdict_containingentries import has_entries
from rest_framework import status

from common.tester.matchers import has_json
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.ticket_blank import IM_TICKET_PDF_BLANK_ENDPOINT, IM_TICKET_HTML_BLANK_ENDPOINT
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, TicketFactory, RefundBlankFactory
)
from travel.rasp.train_api.train_purchase.core.models import TicketRefund, TrainPartner
from travel.rasp.train_api.train_purchase.workflow.booking import OrderState

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


@mock.patch.object(requests, 'get')
@pytest.mark.parametrize('request', [
    '/ru/api/download-blank/{}/',
    '/ru/api/download-blank/{}/?blank_id=1'
])
def test_download_blank_success_ufs(m_requests_get, request):
    m_requests_get.return_value = mock.Mock(ok=True, content=b'pdfcontent')

    order = TrainOrderFactory.create(**{
        'partner': TrainPartner.UFS,
        'status': OrderStatus.DONE,
        'process': {'state': OrderState.DONE},
        'partner_data': dict(operation_id='333'),
        'passengers': [PassengerFactory(
            tickets=[TicketFactory(
                blank_id='1'
            )]
        )]
    })

    with override_settings(ROOT_URLCONF='travel.rasp.train_api.urls_async'):
        response = Client().get(request.format(order.uid))

    assert response.status_code == 200
    assert response.content == b'pdfcontent'

    assert m_requests_get.call_count == 1
    assert_that(m_requests_get.call_args[1]['params'], has_entries({
        'idtrans': '333',
        'format': 'pdf'
    }))


def test_download_pdf_blank_success_im(httpretty, async_urlconf_client):
    mock_im(httpretty, IM_TICKET_PDF_BLANK_ENDPOINT, body=b'pdfcontent',
            adding_headers={'Content-Type': 'application/pdf'})

    order = TrainOrderFactory(
        status=OrderStatus.DONE,
        partner=TrainPartner.IM,
        process={'state': OrderState.DONE},
        partner_data=dict(im_order_id=1, operation_id='333'),
        passengers=[PassengerFactory(
            tickets=[TicketFactory(
                blank_id='1'
            )]
        )]
    )

    response = async_urlconf_client.get('/ru/api/download-blank/{}/'.format(order.uid))

    assert response.status_code == 200
    assert response.content == b'pdfcontent'

    assert_that(httpretty.last_request.body, has_json(has_entries({
        'OrderId': 1,
        'OrderItemId': 333
    })))


def test_download_html_blank_success_im(httpretty, async_urlconf_client):
    mock_im(httpretty, IM_TICKET_HTML_BLANK_ENDPOINT, body=b'htmlcontent', adding_headers={'Content-Type': 'text/html'})

    order = TrainOrderFactory(
        status=OrderStatus.DONE,
        partner=TrainPartner.IM,
        process={'state': OrderState.DONE},
        partner_data=dict(im_order_id=1, operation_id='333'),
        passengers=[PassengerFactory(
            tickets=[TicketFactory(
                blank_id='1'
            )]
        )]
    )

    response = async_urlconf_client.get('/ru/api/download-blank/{}/?ticketFormat=html'.format(order.uid))

    assert response.status_code == 200
    assert response.content == b'htmlcontent'

    assert_that(httpretty.last_request.body, has_json(has_entries({
        'OrderId': 1,
        'OrderItemId': 333
    })))


def test_download_kpc_success():
    refund_blank = RefundBlankFactory()
    order = TrainOrderFactory.create(**{
        'partner': TrainPartner.UFS,
        'status': OrderStatus.DONE,
        'process': {'state': OrderState.DONE},
        'partner_data': dict(operation_id='333'),
        'passengers': [PassengerFactory(
            tickets=[TicketFactory(
                blank_id='1',
                refund=TicketRefund(amount=10, operation_id='100', blank_id=refund_blank.id)
            )]
        )]
    })

    with override_settings(ROOT_URLCONF='travel.rasp.train_api.urls_async'):
        response = Client().get('/ru/api/download-blank/{}/?blank_id=1'.format(order.uid))

    assert response.status_code == 200
    assert response.content == b'pdfcontent'


@mock.patch.object(requests, 'get')
def test_download_kpc_success_ttl_expired(m_requests_get):
    m_requests_get.return_value = mock.Mock(ok=True, content=b'pdfcontent')

    order = TrainOrderFactory.create(**{
        'partner': TrainPartner.UFS,
        'status': OrderStatus.DONE,
        'process': {'state': OrderState.DONE},
        'partner_data': dict(operation_id='333'),
        'passengers': [PassengerFactory(
            tickets=[TicketFactory(
                blank_id='1',
                refund=TicketRefund(amount=10, operation_id='100', blank_id=ObjectId())
            )]
        )]
    })

    with override_settings(ROOT_URLCONF='travel.rasp.train_api.urls_async'):
        response = Client().get('/ru/api/download-blank/{}/?blank_id=1'.format(order.uid))

    assert response.status_code == 200
    assert response.content == b'pdfcontent'

    assert m_requests_get.call_count == 1
    assert_that(m_requests_get.call_args[1]['params'], has_entries({
        'idtrans': '100',
        'format': 'pdf'
    }))


@mock.patch.object(requests, 'get')
def test_download_kpc_wrong_blank_id(m_requests_get):
    order = TrainOrderFactory.create(**{
        'status': OrderStatus.DONE,
        'process': {'state': OrderState.DONE},
        'partner_data': dict(operation_id='333'),
        'passengers': [PassengerFactory(
            tickets=[TicketFactory(
                blank_id='1',
                refund=TicketRefund(amount=10, operation_id='100')
            )]
        )]
    })

    with override_settings(ROOT_URLCONF='travel.rasp.train_api.urls_async'):
        response = Client().get('/ru/api/download-blank/{}/?blank_id=2'.format(order.uid))

    assert response.status_code == 404
    assert response.json() == {'errors': {'blank_id': 'Wrong blankId, ticket not found'}}


@mock.patch.object(requests, 'get')
def test_download_blank_bad_order_status(m_requests_get):
    order = TrainOrderFactory.create()

    with override_settings(ROOT_URLCONF='travel.rasp.train_api.urls_async'):
        response = Client().get('/ru/api/download-blank/{}/'.format(order.uid))

    assert response.status_code == status.HTTP_409_CONFLICT
    assert json.loads(response.content)['errors']['order'] == 'Can not download blank in status "OrderStatus.RESERVED"'

    assert m_requests_get.call_count == 0

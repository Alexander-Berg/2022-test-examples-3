# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.train_partners.base.confirm_ticket import cancel_order
from travel.rasp.train_api.train_partners.im.confirm_ticket import CANCEL_TICKET_ENDPOINT as IM_CANCEL_TICKET_ENDPOINT
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.ufs.confirm_ticket import CONFIRM_TICKET_ENDPOINT as UFS_CONFIRM_TICKET_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory
from travel.rasp.train_api.train_purchase.core.models import TrainPartner


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_cancel_order(httpretty):
    mock_im(httpretty, IM_CANCEL_TICKET_ENDPOINT, body='{}')
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(im_order_id='456')
    )

    cancel_order(order)

    assert len(httpretty.latest_requests) == 1
    assert httpretty.last_request.parsed_body['OrderId'] == 456


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_cancel_order_custom_partner_data(httpretty):
    mock_im(httpretty, IM_CANCEL_TICKET_ENDPOINT, body='{}')
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data_history=[PartnerDataFactory(im_order_id='111'), PartnerDataFactory(im_order_id='222')]
    )

    cancel_order(order, order.partner_data_history[0])

    assert len(httpretty.latest_requests) == 1
    assert httpretty.last_request.parsed_body['OrderId'] == 111


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_ufs_cancel_order(httpretty):
    mock_ufs(httpretty, UFS_CONFIRM_TICKET_ENDPOINT, body="""
        <UFS_RZhD_Gate>
          <System>
            <CurrentTime timeOffset="+03:00">24.08.2017 19:36:13</CurrentTime>
          </System>
          <Status>0</Status>
          <IDTrans>105682844</IDTrans>
          <TransID>105682844</TransID>
          <ConfirmTimeLimit timeOffset="+03:00">24.08.2017 19:51:00</ConfirmTimeLimit>
          <IsTest>1</IsTest>
        </UFS_RZhD_Gate>""")
    order = TrainOrderFactory(
        partner=TrainPartner.UFS,
        partner_data=PartnerDataFactory(operation_id='123')
    )

    cancel_order(order)

    assert len(httpretty.latest_requests) == 1
    assert httpretty.last_request.querystring['IdTrans'] == ['123']
    assert httpretty.last_request.querystring['Confirm'] == ['0']

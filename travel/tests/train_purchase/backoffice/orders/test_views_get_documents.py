# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import pytest
from hamcrest import assert_that, has_entry, contains_inanyorder, has_entries

from travel.rasp.train_api.train_partners.im.factories.order_info import ImOrderInfoFactory, ImOrderCustomerFactory
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_purchase.core.enums import DocumentType, TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PassengerFactory, TicketFactory


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_get_documents(httpretty, backoffice_client):
    order_uid = '82b7e80df16c47a09477cc45aa664d81'
    first_passenger = PassengerFactory(first_name='Сидора', last_name='Сидорова', patronymic='Сидоровна', doc_id='3',
                                       doc_type=DocumentType.RUSSIAN_PASSPORT, tickets=[TicketFactory(blank_id='1')])
    second_passenger = PassengerFactory(first_name='Иван', last_name='Иванов', patronymic='Иванович', doc_id='4',
                                        doc_type=DocumentType.BIRTH_CERTIFICATE, tickets=[TicketFactory(blank_id='1')])
    third_passenger = PassengerFactory(first_name='Петр', last_name='Петров', patronymic='Петрович', doc_id='5',
                                       doc_type=DocumentType.RUSSIAN_PASSPORT, tickets=[TicketFactory(blank_id='2')])
    order = TrainOrderFactory(uid=order_uid, partner=TrainPartner.IM,
                              passengers=[first_passenger, second_passenger, third_passenger])
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(
        train_order=order,
        OrderCustomers=[
            ImOrderCustomerFactory(train_order_passenger=first_passenger, DocumentNumber='3'),
            ImOrderCustomerFactory(train_order_passenger=second_passenger, DocumentNumber='4'),
            ImOrderCustomerFactory(train_order_passenger=third_passenger, DocumentNumber='5'),
        ]
    ))

    response = backoffice_client.get('/ru/train-purchase-backoffice/orders/{}/documents/'.format(order_uid))

    assert response.status_code == 200
    result = json.loads(response.content)

    assert len(result['documents']) == 3
    assert_that(result, has_entry('documents', contains_inanyorder(
        has_entries(fio=first_passenger.fio, docType=first_passenger.doc_type.value, docId='3'),
        has_entries(fio=second_passenger.fio, docType=second_passenger.doc_type.value, docId='4'),
        has_entries(fio=third_passenger.fio, docType=third_passenger.doc_type.value, docId='5'),
    )))

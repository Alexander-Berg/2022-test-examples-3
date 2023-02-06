# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.core.urlresolvers import reverse
from hamcrest import assert_that, has_entries
from rest_framework import status

from common.tester.utils.replace_setting import replace_setting
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory, UserInfoFactory


ORDER_UID = 'some-uid'.ljust(32, '-')


@pytest.fixture(autouse=True)
def do_not_check_tvm_service_ticket():
    with replace_setting('CHECK_TVM_SERVICE_TICKET', False):
        yield


@pytest.mark.parametrize('data', [
    {'expressOrderNumber': 'some-order-number'},
    {'trainOrderUid': ORDER_UID}
])
@replace_setting('ORDER_AUTH_DETAILS_ALLOWED_SERVICE_IDS', ['good'])
def test_400_bad_request(async_urlconf_client, data):
    response = async_urlconf_client.get(
        path=reverse('order_auth_details'),
        data=data,
        **{'HTTP_X_YA_SERVICE_TICKET': 'some-ticket'}
    )

    assert response.status_code == status.HTTP_404_NOT_FOUND
    assert 'errors' in response.data


@pytest.mark.dbuser
@pytest.mark.mongouser
@pytest.mark.parametrize('data', [
    {'expressOrderNumber': 'some-order-number'},
    {'trainOrderUid': ORDER_UID}
])
@replace_setting('ORDER_AUTH_DETAILS_ALLOWED_SERVICE_IDS', ['good'])
def test_ok(async_urlconf_client, data):
    TrainOrderFactory(uid=ORDER_UID, partner_data_history=[PartnerDataFactory(order_num='some-order-number')],
                      user_info=UserInfoFactory(uid='passport-id', email='a@example.com', phone='+71231231212'))

    response = async_urlconf_client.get(
        path=reverse('order_auth_details'),
        data=data,
        **{'HTTP_X_YA_SERVICE_TICKET': 'some-ticket'}
    )

    assert response.status_code == status.HTTP_200_OK
    assert 'errors' not in response.data
    assert_that(response.data['result'], has_entries(
        trainOrderUid=ORDER_UID,
        expressOrderNumber='some-order-number',
        phone='+71231231212',
        email='a@example.com',
        passportId='passport-id',
    ))


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_removed_order_is_removed(async_urlconf_client):
    TrainOrderFactory(removed=True, uid=ORDER_UID)
    response = async_urlconf_client.get(
        path=reverse('order_auth_details'),
        data={'trainOrderUid': ORDER_UID},
        **{'HTTP_X_YA_SERVICE_TICKET': 'some-ticket'}
    )
    assert response.status_code == status.HTTP_404_NOT_FOUND

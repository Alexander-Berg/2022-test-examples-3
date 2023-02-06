# -*- coding: utf-8 -*-
from __future__ import absolute_import

import pytest
from mock import Mock

from travel.avia.library.python.balance_client.client import (
    Client as BalanceClient,
    BalanceException,
    BALANCE_AVIA_PRODUCT_ID,
    BALANCE_AVIA_SERVICE_ID,
)

BALANCE_HOST = 'http://dev-null.avia.dev.yandex.net/xmlrpc'
OPERATOR_UID = 'SOME_OPERATOR_UID'
PARTNER_NAME = 'SOME_PARTNER_NAME'
CLIENT_ID = 123456789  # 'SOME_CLIENT_ID'
UID = 'SOME_CLIENT_UID'
BILLING_ID = 'SOME_BILLING_ID'
ORDER = {
    'ServiceOrderID': BILLING_ID,
    'ServiceID': BALANCE_AVIA_SERVICE_ID,
    'ClientID': CLIENT_ID,
    'ProductID': BALANCE_AVIA_PRODUCT_ID,
}


# Check create_avia_client
def test_success_create_avia_client():
    client = _mock_balance_client()
    create_client_mock = Mock(return_value=[0, 'ok', CLIENT_ID])
    setattr(client._server, 'Balance.CreateClient', create_client_mock)
    create_association_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.CreateUserClientAssociation', create_association_mock)
    create_or_update_orders_batch_mock = Mock(return_value=[[0, 'ok']])
    setattr(client._server, 'Balance.CreateOrUpdateOrdersBatch', create_or_update_orders_batch_mock)
    remove_association_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.RemoveUserClientAssociation', remove_association_mock)

    client.create_avia_client(UID, BILLING_ID, PARTNER_NAME, OPERATOR_UID)

    create_client_mock.assert_called_once_with(OPERATOR_UID, {
        'SERVICE_ID': BALANCE_AVIA_SERVICE_ID,
        'NAME': PARTNER_NAME,
    })
    create_association_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)
    create_or_update_orders_batch_mock.assert_called_once_with(OPERATOR_UID, [ORDER])
    remove_association_mock.assert_not_called()


@pytest.mark.dbuser
def test_fail_create_avia_client_on_create_client():
    client = _mock_balance_client()
    create_client_mock = Mock(return_value=[1, 'fail', CLIENT_ID])
    setattr(client._server, 'Balance.CreateClient', create_client_mock)
    create_association_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.CreateUserClientAssociation', create_association_mock)
    create_or_update_orders_batch_mock = Mock(return_value=[[0, 'ok']])
    setattr(client._server, 'Balance.CreateOrUpdateOrdersBatch', create_or_update_orders_batch_mock)
    remove_association_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.RemoveUserClientAssociation', remove_association_mock)

    with pytest.raises(BalanceException):
        client.create_avia_client(UID, BILLING_ID, PARTNER_NAME, OPERATOR_UID)

    create_client_mock.assert_called_once_with(OPERATOR_UID, {
        'SERVICE_ID': BALANCE_AVIA_SERVICE_ID,
        'NAME': PARTNER_NAME,
    })
    create_association_mock.assert_not_called()
    create_or_update_orders_batch_mock.assert_not_called()
    remove_association_mock.assert_not_called()


@pytest.mark.dbuser
def test_fail_create_avia_client_on_create_association():
    client = _mock_balance_client()
    create_client_mock = Mock(return_value=[0, 'ok', CLIENT_ID])
    setattr(client._server, 'Balance.CreateClient', create_client_mock)
    create_association_mock = Mock(return_value=[1, 'ok'])
    setattr(client._server, 'Balance.CreateUserClientAssociation', create_association_mock)
    create_or_update_orders_batch_mock = Mock(return_value=[[0, 'ok']])
    setattr(client._server, 'Balance.CreateOrUpdateOrdersBatch', create_or_update_orders_batch_mock)
    remove_association_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.RemoveUserClientAssociation', remove_association_mock)

    with pytest.raises(BalanceException):
        client.create_avia_client(UID, BILLING_ID, PARTNER_NAME, OPERATOR_UID)

    create_client_mock.assert_called_once_with(OPERATOR_UID, {
        'SERVICE_ID': BALANCE_AVIA_SERVICE_ID,
        'NAME': PARTNER_NAME,
    })
    create_association_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)
    create_or_update_orders_batch_mock.assert_not_called()
    remove_association_mock.assert_not_called()


@pytest.mark.dbuser
def test_fail_create_avia_client_on_create_order():
    client = _mock_balance_client()
    create_client_mock = Mock(return_value=[0, 'ok', CLIENT_ID])
    setattr(client._server, 'Balance.CreateClient', create_client_mock)
    create_association_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.CreateUserClientAssociation', create_association_mock)
    create_or_update_orders_batch_mock = Mock(return_value=[[1, 'ok']])
    setattr(client._server, 'Balance.CreateOrUpdateOrdersBatch', create_or_update_orders_batch_mock)
    remove_association_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.RemoveUserClientAssociation', remove_association_mock)

    with pytest.raises(BalanceException):
        client.create_avia_client(UID, BILLING_ID, PARTNER_NAME, OPERATOR_UID)

    create_client_mock.assert_called_once_with(OPERATOR_UID, {
        'SERVICE_ID': BALANCE_AVIA_SERVICE_ID,
        'NAME': PARTNER_NAME,
    })
    create_association_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)
    create_or_update_orders_batch_mock.assert_called_once_with(OPERATOR_UID, [ORDER])
    remove_association_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)


@pytest.mark.dbuser
def test_fail_create_avia_client_on_create_order_and_rollback():
    client = _mock_balance_client()
    create_client_mock = Mock(return_value=[0, 'ok', CLIENT_ID])
    setattr(client._server, 'Balance.CreateClient', create_client_mock)
    create_association_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.CreateUserClientAssociation', create_association_mock)
    create_or_update_orders_batch_mock = Mock(return_value=[[1, 'ok']])
    setattr(client._server, 'Balance.CreateOrUpdateOrdersBatch', create_or_update_orders_batch_mock)
    remove_association_mock = Mock(return_value=[1, 'ok'])
    setattr(client._server, 'Balance.RemoveUserClientAssociation', remove_association_mock)

    with pytest.raises(BalanceException):
        client.create_avia_client(UID, BILLING_ID, PARTNER_NAME, OPERATOR_UID)

    create_client_mock.assert_called_once_with(OPERATOR_UID, {
        'SERVICE_ID': BALANCE_AVIA_SERVICE_ID,
        'NAME': PARTNER_NAME,
    })
    create_association_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)
    create_or_update_orders_batch_mock.assert_called_once_with(OPERATOR_UID, [ORDER])
    remove_association_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)


# Check _create_client
def test_success_create_client():
    client = _mock_balance_client()
    method_mock = Mock(return_value=[0, 'ok', CLIENT_ID])
    setattr(client._server, 'Balance.CreateClient', method_mock)

    assert client._create_client(OPERATOR_UID, PARTNER_NAME) == CLIENT_ID
    method_mock.assert_called_once_with(OPERATOR_UID, {
        'SERVICE_ID': BALANCE_AVIA_SERVICE_ID,
        'NAME': PARTNER_NAME,
    })


@pytest.mark.dbuser
def test_fail_create_client_because_http_error():
    client = _mock_balance_client()
    method_mock = Mock(side_effect=Exception('some_error'))
    setattr(client._server, 'Balance.CreateClient', method_mock)

    with pytest.raises(BalanceException):
        client._create_client(OPERATOR_UID, PARTNER_NAME)
    method_mock.assert_called_once_with(OPERATOR_UID, {
        'SERVICE_ID': BALANCE_AVIA_SERVICE_ID,
        'NAME': PARTNER_NAME,
    })


@pytest.mark.dbuser
def test_fail_create_client_because_internal_error():
    client = _mock_balance_client()
    method_mock = Mock(return_value=[1, 'fail', 'client_id'])
    setattr(client._server, 'Balance.CreateClient', method_mock)

    with pytest.raises(BalanceException):
        client._create_client(OPERATOR_UID, PARTNER_NAME)
    method_mock.assert_called_once_with(OPERATOR_UID, {
        'SERVICE_ID': BALANCE_AVIA_SERVICE_ID,
        'NAME': PARTNER_NAME,
    })


# Check _create_user_client_association
def test_success_create_user_client_association():
    client = _mock_balance_client()
    method_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.CreateUserClientAssociation', method_mock)

    client._create_user_client_association(OPERATOR_UID, CLIENT_ID, UID)
    method_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)


@pytest.mark.dbuser
def test_fail_create_user_client_association_because_http_error():
    client = _mock_balance_client()
    method_mock = Mock(side_effect=Exception('some_error'))
    setattr(client._server, 'Balance.CreateUserClientAssociation', method_mock)

    with pytest.raises(BalanceException):
        client._create_user_client_association(OPERATOR_UID, CLIENT_ID, UID)
    method_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)


@pytest.mark.dbuser
def test_fail_create_user_client_association_because_internal_error():
    client = _mock_balance_client()
    method_mock = Mock(return_value=[1, 'fail'])
    setattr(client._server, 'Balance.CreateUserClientAssociation', method_mock)

    with pytest.raises(BalanceException):
        client._create_user_client_association(OPERATOR_UID, CLIENT_ID, UID)
    method_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)


# Check _remove_user_client_association
def test_success_remove_user_client_association():
    client = _mock_balance_client()
    method_mock = Mock(return_value=[0, 'ok'])
    setattr(client._server, 'Balance.RemoveUserClientAssociation', method_mock)

    assert client._remove_user_client_association(OPERATOR_UID, CLIENT_ID, UID)
    method_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)


@pytest.mark.dbuser
def test_fail_remove_user_client_association_because_http_error():
    client = _mock_balance_client()
    method_mock = Mock(side_effect=Exception('some_error'))
    setattr(client._server, 'Balance.RemoveUserClientAssociation', method_mock)

    with pytest.raises(BalanceException):
        client._remove_user_client_association(OPERATOR_UID, CLIENT_ID, UID)
    method_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)


@pytest.mark.dbuser
def test_fail_remove_user_client_association_because_internal_error():
    client = _mock_balance_client()
    method_mock = Mock(return_value=[1, 'fail'])
    setattr(client._server, 'Balance.RemoveUserClientAssociation', method_mock)

    with pytest.raises(BalanceException):
        client._remove_user_client_association(OPERATOR_UID, CLIENT_ID, UID)
    method_mock.assert_called_once_with(OPERATOR_UID, CLIENT_ID, UID)


# Check _create_or_update_orders_batch
def test_success_create_or_update_orders_batch():
    client = _mock_balance_client()
    method_mock = Mock(return_value=[[0, 'ok']])
    setattr(client._server, 'Balance.CreateOrUpdateOrdersBatch', method_mock)

    assert client._create_or_update_orders_batch(OPERATOR_UID, ORDER)
    method_mock.assert_called_once_with(OPERATOR_UID, [ORDER])


@pytest.mark.dbuser
def test_fail_create_or_update_orders_batch_because_http_error():
    client = _mock_balance_client()
    method_mock = Mock(side_effect=Exception('some_error'))
    setattr(client._server, 'Balance.CreateOrUpdateOrdersBatch', method_mock)

    with pytest.raises(BalanceException):
        client._create_or_update_orders_batch(OPERATOR_UID, ORDER)
    method_mock.assert_called_once_with(OPERATOR_UID, [ORDER])


@pytest.mark.dbuser
def test_fail_create_or_update_orders_batch_because_internal_error():
    client = _mock_balance_client()
    method_mock = Mock(return_value=[[1, 'fail']])
    setattr(client._server, 'Balance.CreateOrUpdateOrdersBatch', method_mock)

    with pytest.raises(BalanceException):
        client._create_or_update_orders_batch(OPERATOR_UID, ORDER)
    method_mock.assert_called_once_with(OPERATOR_UID, [ORDER])


def _mock_balance_client():
    client = BalanceClient(BALANCE_HOST, 1)
    client._server = Mock()
    return client

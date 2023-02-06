from datetime import datetime
from unittest import mock

import factory
import pytest

from travel.rasp.bus.db.tests.factories import OrderFactory, OrderLogEntryFactory
from travel.rasp.bus.library import trust_client
from travel.rasp.bus.scripts.clearing_checker import (
    OrderPurchase,
    TrustClientFactory,
    check_and_clear_payments,
    list_uncleared_purchases,
)
from travel.rasp.bus.settings import Settings


@pytest.fixture
def m_trust_client():
    m_trust_client = mock.Mock(spec=trust_client.Client)
    with mock.patch.object(TrustClientFactory, "get_client", autospec=True, return_value=m_trust_client):
        yield m_trust_client


class TestTrustClientProvider:
    def test_get_trust_client(self):
        with mock.patch.object(
            Settings.Trust, "get_service_tokens", autospec=True, return_value={"123": "service_token_123"}
        ):
            assert TrustClientFactory.get_client(123)


def test_list_uncleared_purchases(session):
    create_order = factory.make_factory(
        OrderFactory,
        creation_ts=datetime(2000, 1, 15),
        status="confirmed",
        booking={"serviceId": 123},
        purchase={"purchase_token": "purchase_token_123"},
    )

    create_order(id="uncleared_old", creation_ts=datetime(2000, 1, 1))
    create_order(id="uncleared_new", creation_ts=datetime(2000, 1, 30))
    create_order(id="unconfirmed", status="unconfirmed")
    create_order(id="cleared", log_entries=[OrderLogEntryFactory(status="cleared")])
    create_order(id="uncleared")

    assert list_uncleared_purchases(min_creation_ts=datetime(2000, 1, 10), max_creation_ts=datetime(2000, 1, 20)) == (
        OrderPurchase(order_id="uncleared", service_id=123, purchase_token="purchase_token_123"),
    )
    assert list_uncleared_purchases(min_creation_ts=datetime(2000, 1, 10)) == (
        OrderPurchase(order_id="uncleared", service_id=123, purchase_token="purchase_token_123"),
        OrderPurchase(order_id="uncleared_new", service_id=123, purchase_token="purchase_token_123"),
    )

    with pytest.raises(ValueError):
        list_uncleared_purchases(min_creation_ts=datetime(2000, 1, 30), max_creation_ts=datetime(2000, 1, 1))


@pytest.mark.parametrize("dry_run", (False, True))
def test_check_and_clear_payments(m_trust_client, session, dry_run):
    cleared_token = "cleared_token"
    uncleared_token = "uncleared_token"

    create_order = factory.make_factory(OrderFactory, status="confirmed", booking={"serviceId": 123})
    create_order(creation_ts=datetime(2000, 1, 10), purchase={"purchase_token": cleared_token})
    create_order(creation_ts=datetime(2000, 1, 15), purchase={"purchase_token": uncleared_token})

    m_trust_client.get_payment_data.side_effect = [
        {"status": "success", "payment_status": "cleared"},
        {"status": "success", "payment_status": "processing"},
    ]

    check_and_clear_payments(min_creation_ts=datetime(2000, 1, 1), dry_run=dry_run)
    assert m_trust_client.get_payment_data.call_args_list == [mock.call(cleared_token), mock.call(uncleared_token)]
    assert dry_run ^ (m_trust_client.clear_payment.call_args_list == [mock.call(uncleared_token)])


def test_check_and_clear_payments_exception_handling(m_trust_client, session):
    exception_token = "exception_token"
    cleared_token = "cleared_token"

    create_order = factory.make_factory(OrderFactory, status="confirmed", booking={"serviceId": 123})
    create_order(creation_ts=datetime(2000, 1, 10), purchase={"purchase_token": exception_token})
    create_order(creation_ts=datetime(2000, 1, 15), purchase={"purchase_token": cleared_token})

    m_trust_client.get_payment_data.side_effect = [Exception, {"status": "success", "payment_status": "cleared"}]

    check_and_clear_payments(min_creation_ts=datetime(2000, 1, 1))
    assert m_trust_client.get_payment_data.call_args_list == [mock.call(exception_token), mock.call(cleared_token)]
    m_trust_client.clear_payment.assert_not_called()

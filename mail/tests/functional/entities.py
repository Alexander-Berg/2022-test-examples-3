import pytest

from mail.payments.payments.core.entities.enums import ModerationType, TransactionStatus
from mail.payments.payments.core.entities.moderation import Moderation
from mail.payments.payments.core.entities.subscription import Subscription
from mail.payments.payments.core.entities.transaction import Transaction


@pytest.fixture
def transaction_status():
    return TransactionStatus.ACTIVE


@pytest.fixture
async def transaction(storage, order, payment_url, trust_resp_code, transaction_status, trust_payment_id):
    if transaction_status is None:
        return None

    return await storage.transaction.create(Transaction(
        uid=order.uid,
        order_id=order.order_id,
        status=transaction_status,
        trust_purchase_token='xxx_purchase_token',
        trust_payment_url=payment_url,
        trust_resp_code=trust_resp_code,
        trust_payment_id=trust_payment_id,
    ))


@pytest.fixture
def subscription_uid(merchant):
    return merchant.uid


@pytest.fixture
async def subscription(storage, subscription_data, subscription_uid, service_merchant, service_client):
    return await storage.subscription.create(Subscription(
        uid=subscription_uid,
        service_client_id=service_client.service_client_id,
        service_merchant_id=service_merchant.service_merchant_id,
        **subscription_data,
    ))


@pytest.fixture
async def moderation_subscription(storage, subscription):
    return await storage.moderation.create(Moderation(
        uid=subscription.uid,
        entity_id=subscription.subscription_id,
        revision=subscription.revision,
        moderation_type=ModerationType.SUBSCRIPTION,
        approved=True,
    ))

from random import choice

import pytest

from mail.payments.payments.core.entities.image import Image
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.order import Order, OrderData, RefundStatus
from mail.payments.payments.core.entities.product import NDS, Product
from mail.payments.payments.core.entities.service import ServiceMerchant
from mail.payments.payments.core.entities.transaction import Transaction, TransactionStatus


@pytest.fixture
def ohio_client(mocker, client_mocker):
    from mail.payments.payments.interactions.ohio import OhioClient
    client = client_mocker(OhioClient)
    mocker.spy(client, 'post')
    yield client


@pytest.fixture
def merchant_uid(randn):
    return randn()


@pytest.fixture
def currency(rands):
    return rands()


@pytest.fixture
def create_image(merchant_uid, randn, rands):
    def _inner(image_is_empty: bool, image_is_stored: bool):
        if image_is_empty:
            return None
        return Image(
            uid=merchant_uid,
            image_id=randn(),
            md5=rands(),
            sha256=rands(),
            url=rands(),
            stored_path=rands() if image_is_stored else None,
        )
    return _inner


@pytest.fixture
def create_item(randn, rands, randdecimal, merchant_uid, currency, create_image):
    def _inner(image_is_empty: bool = True, image_is_stored: bool = True):
        return Item(
            uid=merchant_uid,
            amount=randdecimal(),
            product_id=randn(),
            order_id=randn(),
            product=Product(
                uid=merchant_uid,
                name=rands(),
                price=randdecimal(),
                nds=choice(list(NDS)),
                currency=currency,
            ),
            image=create_image(image_is_empty, image_is_stored)
        )

    return _inner


@pytest.fixture
def order(randn, rands, merchant_uid, create_item):
    return Order(
        uid=merchant_uid,
        order_id=randn(),
        customer_uid=randn(),
        shop_id=randn(),
        items=[
            create_item(image_is_empty=True),
            create_item(image_is_empty=False, image_is_stored=False),
            create_item(image_is_empty=False, image_is_stored=True),
        ],
        description=rands(),
        data=OrderData(service_data={rands(): rands()}),
    )


@pytest.fixture
def transaction(rands, order):
    return Transaction(
        uid=order.uid,
        order_id=order.order_id,
        trust_purchase_token=rands(),
        status=TransactionStatus.HELD,
    )


@pytest.fixture
def create_refund(merchant_uid, randn, rands, create_item):
    def _inner():
        return Order(
            uid=merchant_uid,
            shop_id=randn(),
            order_id=randn(),
            refund_status=choice(list(RefundStatus)),
            items=[create_item() for _ in range(2)],
            trust_refund_id=rands(),
        )
    return _inner


@pytest.fixture
def refunds(create_refund):
    return [create_refund() for _ in range(2)]


@pytest.fixture
def service_merchant(randn, rands, merchant_uid):
    return ServiceMerchant(
        uid=merchant_uid,
        service_id=randn(),
        entity_id=rands(),
        description=rands(),
        service_merchant_id=randn(),
    )


def dump_item(item):
    return {
        'image_url': item.image.url if item.image is not None else None,
        'image_path': item.image.stored.path if item.image is not None and item.image.stored is not None else None,
        'name': item.name,
        'nds': item.nds.value,
        'amount': str(item.amount),
        'price': str(item.price),
        'currency': item.currency,
    }


@pytest.mark.asyncio
class TestPostOrder:
    @pytest.fixture
    def revision(self, randn):
        return randn()

    @pytest.fixture
    def assert_call(self, ohio_client, order, transaction, refunds, service_merchant, revision):
        async def _inner(**kwargs):
            has_completed_refund = any((refund.refund_status == RefundStatus.COMPLETED for refund in refunds))
            transaction_paid = transaction.status in (TransactionStatus.HELD, TransactionStatus.CLEARED)
            if has_completed_refund:
                status = 'refunded'
            elif transaction_paid:
                status = 'paid'
            else:
                status = 'cancelled'

            await ohio_client.post_order(order, transaction, refunds, service_merchant, revision)
            ohio_client.post.assert_called_once_with(
                interaction_method='v1_internal_customer_orders',
                url=f'{ohio_client.BASE_URL}/v1/internal/customer/{order.customer_uid}/orders',
                json={
                    'payments_service_id': service_merchant.service_id,
                    'subservice_id': service_merchant.entity_id,
                    'merchant_uid': service_merchant.uid,
                    'service_merchant_id': service_merchant.service_merchant_id,
                    'trust_purchase_token': transaction.trust_purchase_token,
                    'created': transaction.created.isoformat(),
                    'revision': revision,
                    'payments_order_id': order.order_id,
                    'order_data': {
                        'total': str(order.price),
                        'currency': order.currency,
                        'description': order.description,
                        'items': list(map(dump_item, order.items)),
                        'refunds': [
                            {
                                'trust_refund_id': refund.trust_refund_id,
                                'refund_status': refund.refund_status.value,
                                'total': str(refund.price),
                                'currency': refund.currency,
                                'items': list(map(dump_item, refund.items))
                            }
                            for refund in refunds
                        ],
                    },
                    'status': status,
                    'service_data': order.service_data or {},
                    **kwargs,
                }
            )

        return _inner

    async def test_default(self, assert_call):
        await assert_call()

    @pytest.mark.parametrize('transaction_status', (TransactionStatus.HELD, TransactionStatus.CLEARED))
    async def test_paid(self, transaction, refunds, assert_call, transaction_status):
        transaction.status = transaction_status
        refunds[:] = []
        await assert_call(status='paid')

    async def test_cancelled(self, transaction, refunds, assert_call):
        transaction.status = TransactionStatus.CANCELLED
        refunds[:] = []
        await assert_call(status='cancelled')

    async def test_refunded(self, create_refund, refunds, assert_call):
        refunds[:] = [create_refund()]
        refunds[0].refund_status = RefundStatus.COMPLETED
        await assert_call(status='refunded')

    @pytest.mark.parametrize('refund_status', [
        refund_status
        for refund_status in RefundStatus
        if refund_status != RefundStatus.COMPLETED
    ])
    async def test_ignores_not_completed_refunds(self, transaction, create_refund, refunds, assert_call, refund_status):
        transaction.status = TransactionStatus.CLEARED
        refunds[:] = [create_refund()]
        refunds[0].refund_status = refund_status
        await assert_call(status='paid')

    async def test_empty_service_data(self, order, assert_call):
        order.data.service_data = None
        await assert_call(service_data={})

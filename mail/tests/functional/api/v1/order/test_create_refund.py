import pytest

from sendr_utils import enum_value

from hamcrest import assert_that, contains_inanyorder, has_entries, has_properties, is_, match_equality

from mail.payments.payments.core.entities.enums import (
    AcquirerType, MerchantRole, OrderKind, PayStatus, RefundStatus, TransactionStatus
)
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.tests.base import BaseAcquirerTest, BaseTestMerchantRoles
from mail.payments.payments.tests.utils import check_order
from mail.payments.payments.utils.helpers import without_none

from .base import BaseTestOrder


@pytest.mark.usefixtures('moderation')
class BaseTestCreateRefund(BaseAcquirerTest, BaseTestOrder):
    @pytest.fixture
    def kind(self):
        return None

    @pytest.fixture
    def create_order_data(self, kind):
        return without_none({
            'kind': enum_value(kind),
            'caption': 'Some test order',
            'description': 'Some description',
            'items': [
                {
                    'name': 'item 02',
                    'amount': 2,
                    'price': 100,
                    'nds': 'nds_10_110',
                    'currency': 'RUB',
                },
                {
                    'name': 'item 01',
                    'nds': 'nds_10_110',
                    'price': 100.77,
                    'amount': 3.33,
                    'currency': 'RUB',
                },
                {
                    'name': 'item 03',
                    'nds': 'nds_10_110',
                    'price': 10,
                    'amount': 34,
                    'currency': 'RUB',
                }
            ],
        })

    @pytest.fixture
    async def order(self, payments_settings, client, storage, merchant, create_order_data):
        raise NotImplementedError

    @pytest.fixture
    def partial_refund(self):
        return {
            'caption': 'Some test order',
            'description': 'Some description',
            'items': [
                {
                    'name': 'item 02',
                    'amount': 1,
                    'price': 100,
                    'nds': 'nds_10_110',
                    'currency': 'RUB',
                },
                {
                    'name': 'item 01',
                    'nds': 'nds_10_110',
                    'price': 100.77,
                    'amount': 1,
                    'currency': 'RUB',
                },
                {
                    'name': 'item 03',
                    'nds': 'nds_10_110',
                    'price': 10,
                    'amount': 3,
                    'currency': 'RUB',
                }
            ],
        }

    @pytest.fixture
    def exceeding_refund(self):
        return {
            'caption': 'Some test order',
            'description': 'Some description',
            'items': [
                {
                    'name': 'item 02',
                    'amount': 1,
                    'price': 100,
                    'nds': 'nds_10_110',
                    'currency': 'RUB',
                },
                {
                    'name': 'item 01',
                    'nds': 'nds_10_110',
                    'price': 100.77,
                    'amount': 1,
                    'currency': 'RUB',
                },
                {
                    'name': 'item 03',
                    'nds': 'nds_10_110',
                    'price': 10,
                    'amount': 300,
                    'currency': 'RUB',
                }
            ],
        }

    @pytest.fixture
    def existing_refunds(self):
        return [
            {
                'caption': 'Some test order',
                'description': 'Some description',
                'items': [
                    {
                        'name': 'item 02',
                        'amount': 1,
                        'price': 100,
                        'nds': 'nds_10_110',
                        'currency': 'RUB',
                    },
                    {
                        'name': 'item 01',
                        'nds': 'nds_10_110',
                        'price': 100.77,
                        'amount': 1.33,
                        'currency': 'RUB',
                    }
                ],
            },
            {
                'caption': 'Some test order',
                'description': 'Some description',
                'items': [
                    {
                        'name': 'item 01',
                        'nds': 'nds_10_110',
                        'price': 100.77,
                        'amount': 0.73,
                        'currency': 'RUB',
                    }
                ],
            }
        ]

    @pytest.fixture
    def trust_purchase_token(self):
        return 'some_secret_token'

    @pytest.fixture
    def trust_refund_id(self):
        return 'trust_refund_id_1234'

    @pytest.fixture
    def customer_uid(self):
        return 11300011122

    @pytest.fixture(autouse=True)
    async def setup(self, storage, moderation, order, kind, client, trust_purchase_token):
        await storage.transaction.create(Transaction(
            uid=order.uid,
            order_id=order.order_id,
            status=TransactionStatus.CLEARED,
            trust_purchase_token=trust_purchase_token,
        ))
        if order.kind == OrderKind.PAY:
            order.pay_status = PayStatus.PAID
        await storage.order.save(order)

    @pytest.fixture(autouse=True)
    def payment_get_mock(self, shop_type, trust_client_mocker, customer_uid):
        with trust_client_mocker(shop_type, 'payment_get', {'uid': customer_uid}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_create_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_create', trust_refund_id) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_start_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_start', trust_refund_id) as mock:
            yield mock

    @pytest.fixture
    def send_request(self, client, tvm):
        raise NotImplementedError

    @pytest.fixture
    async def refund_response(self, send_request, order, create_order_data):
        r = await send_request(order.uid, order.order_id, create_order_data)
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    async def refund(self, storage, merchant, refund_response):
        order_id = refund_response['data']['order_id']
        return await storage.order.get(uid=merchant.uid, order_id=order_id)

    @pytest.fixture
    async def refund_items(self, storage, refund):
        return [
            item
            async for item in storage.item.get_for_order(refund.uid, refund.order_id)
        ]

    @pytest.fixture
    async def oauth(self, merchant, order):
        if order.get_acquirer(merchant.acquirer) == AcquirerType.KASSA:
            return merchant.oauth[0]
        return None

    @pytest.fixture
    async def existing_failed_refund(self, storage, merchant, order, existing_refunds):
        # Creating refunds in database
        refund = await storage.order.create(Order(
            uid=order.uid,
            shop_id=order.shop_id,
            original_order_id=order.order_id,
            caption='test refund order',
            description='test duplicate refund',
            kind=OrderKind.REFUND,
            pay_status=None,
            refund_status=RefundStatus.FAILED,
        ))
        # Creating refund items in database
        for item_data in existing_refunds[0]['items']:
            product, _ = await storage.product.get_or_create(Product(
                uid=merchant.uid,
                name=item_data['name'],
                price=item_data['price'],
                nds=item_data['nds'],
            ))
            item = Item(
                uid=refund.uid,
                order_id=refund.order_id,
                product_id=product.product_id,
                amount=item_data['amount'],
                product=product
            )
            await storage.item.create(item)

    @pytest.fixture
    async def already_existing_refund(self, storage, merchant, order, existing_refunds, existing_failed_refund):
        # Creating refunds in database
        for refund_data in existing_refunds:
            refund = await storage.order.create(Order(
                uid=order.uid,
                shop_id=order.shop_id,
                original_order_id=order.order_id,
                caption="test refund order",
                description='test duplicate refund',
                kind=OrderKind.REFUND,
                pay_status=None,
                refund_status=RefundStatus.CREATED,
            ))
            # Creating refund items in database
            for item_data in refund_data['items']:
                product, _ = await storage.product.get_or_create(Product(
                    uid=merchant.uid,
                    name=item_data['name'],
                    price=item_data['price'],
                    nds=item_data['nds'],
                ))
                item = Item(
                    uid=refund.uid,
                    order_id=refund.order_id,
                    product_id=product.product_id,
                    amount=item_data['amount'],
                    product=product
                )
                await storage.item.create(item)

    def test_response(self, create_order_data, refund_response, refund):
        """
        Legacy refund post updates refund after creation, but returns old values for
        `revision`, `updated`.
        """
        check_order(
            refund,
            refund_response['data'],
            {
                'items': contains_inanyorder(*[
                    has_entries({
                        'amount': item['amount'],
                        'currency': item['currency'],
                        'name': item['name'],
                        'nds': item['nds'],
                        'price': item['price'],
                        'product_id': is_(int),
                    })
                    for item in create_order_data['items']
                ]),
                'original_order_id': refund.original_order_id,
                'price': round(sum([
                    item['price'] * item['amount']
                    for item in create_order_data['items']
                ]), 2),  # type: ignore
                'currency': 'RUB',
                'refund_status': refund.refund_status.value,
                'revision': is_(int),  # TODO: remove this
                'updated': is_(str),  # TODO: remove this
            }
        )

    def test_refund(self, order, refund):
        assert_that(
            refund,
            has_properties({
                'original_order_id': order.order_id,
            }),
        )

    def test_payment_get_call(self, merchant, order, refund_response, payment_get_mock, trust_purchase_token):
        payment_get_mock.assert_called_once_with(
            uid=order.uid,
            acquirer=merchant.acquirer,
            purchase_token=trust_purchase_token,
            with_terminal_info=True,
        )

    def test_refund_create_call(self,
                                merchant,
                                customer_uid,
                                trust_purchase_token,
                                refund_create_mock,
                                refund_response,
                                refund,
                                order,
                                oauth,
                                refund_items,
                                ):
        refund_create_mock.assert_called_once_with(
            uid=merchant.uid,
            acquirer=merchant.acquirer,
            original_order_id=refund.original_order_id,
            customer_uid=customer_uid,
            caption=refund.caption,
            purchase_token=trust_purchase_token,
            submerchant_id=merchant.get_submerchant_id(),
            oauth=oauth,
            items=match_equality(contains_inanyorder(*refund_items)),
            version=order.data.version,
        )

    def test_refund_start_call(self, refund_response, trust_refund_id, refund_start_mock):
        refund_start_mock.assert_not_called()

    @pytest.mark.parametrize('acquirer', (AcquirerType.TINKOFF,))
    @pytest.mark.asyncio
    async def test_error_check_trust_credentials_tinkoff(self, storage, merchant, order, create_order_data,
                                                         send_request):
        merchant.submerchant_id = None
        await storage.merchant.save(merchant)

        r = await send_request(order.uid, order.order_id, create_order_data)
        message = (await r.json())['data']['message']
        assert r.status == 400 and message == 'TINKOFF_INVALID_SUBMERCHANT_ID'

    @pytest.mark.parametrize('acquirer', (AcquirerType.KASSA,))
    @pytest.mark.asyncio
    async def test_error_check_trust_credentials_kassa(self, storage, merchant, send_request, order, create_order_data):
        for oauth in merchant.oauth:
            await storage.merchant_oauth.delete(oauth)
        merchant.oauth = []

        r = await send_request(order.uid, order.order_id, create_order_data)
        message = (await r.json())['data']['message']
        assert r.status == 400 and message == 'OAUTH_ABSENT'

    @pytest.mark.asyncio
    async def test_order_not_paid(self, storage, send_request, order, create_order_data):
        order.pay_status = PayStatus.NEW
        order = await storage.order.save(order)
        r = await send_request(order.uid, order.order_id, create_order_data)
        message = (await r.json())['data']['message']
        assert message == 'ORDER_ORIGINAL_ORDER_MUST_BE_PAID'

    @pytest.mark.parametrize('kind', (OrderKind.MULTI,))
    @pytest.mark.asyncio
    async def test_order_not_pay_kind(self, send_request, order, create_order_data):
        r = await send_request(order.uid, order.order_id, create_order_data)
        message = (await r.json())['data']['message']
        assert message == 'ORDER_ORIGINAL_ORDER_MUST_BE_PAY_KIND'

    @pytest.mark.asyncio
    async def test_new_item(self, send_request, order, order_data):
        order_data['items'][0]['name'] = 'some new order'
        r = await send_request(order.uid, order.order_id, order_data)
        message = (await r.json())['data']['message']
        assert message == 'ORDER_ITEM_NOT_PRESENT_IN_ORIGINAL_ORDER'

    @pytest.mark.asyncio
    async def test_request_already_refunded(self, already_existing_refund, order, order_data, send_request):
        duplicate_refund_req = await send_request(order.uid, order.order_id, order_data)
        response = await duplicate_refund_req.json()
        message = response['data']['message']
        assert message == 'ORDER_REQUESTED_ITEM_AMOUNT_EXCEEDS_PAID'

    @pytest.mark.asyncio
    async def test_exceeding_amount(self, order, exceeding_refund, send_request):
        duplicate_refund_req = await send_request(order.uid, order.order_id, exceeding_refund)
        response = await duplicate_refund_req.json()
        assert response['data']['message'] == 'ORDER_REQUESTED_ITEM_AMOUNT_EXCEEDS_PAID'

    @pytest.mark.asyncio
    async def test_partial_refund(self, storage, merchant, send_request, already_existing_refund, order,
                                  partial_refund):
        partial_refund_req = await send_request(order.uid, order.order_id, partial_refund)
        assert partial_refund_req.status == 200
        response = await partial_refund_req.json()
        order_id = response['data']['order_id']
        refund = await storage.order.get(uid=merchant.uid, order_id=order_id)
        check_order(
            refund,
            response['data'],
            {
                'items': contains_inanyorder(*[
                    has_entries({
                        'amount': item['amount'],
                        'currency': item['currency'],
                        'name': item['name'],
                        'nds': item['nds'],
                        'price': item['price'],
                        'product_id': is_(int),
                    })
                    for item in partial_refund['items']
                ]),
                'original_order_id': order.order_id,
                'price': round(sum([
                    item['price'] * item['amount']
                    for item in partial_refund['items']
                ]), 2),  # type: ignore
                'refund_status': refund.refund_status.value,
                'revision': is_(int),  # TODO: remove this
                'updated': is_(str),  # TODO: remove this
            }
        )


class TestCreateRefund(BaseTestMerchantRoles, BaseTestCreateRefund):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def send_request(self, client, tvm):
        async def _inner(uid, order_id, data):
            return await client.post(f'/v1/order/{uid}/{order_id}/refund', json=data)

        return _inner

    @pytest.fixture
    async def order(self, no_merchant_user_check, client, storage, merchant, create_order_data):
        with no_merchant_user_check():
            r = await client.post(
                f'/v1/order/{merchant.uid}/',
                json=create_order_data,
            )
        assert r.status == 200
        order_id = (await r.json())['data']['order_id']
        return await storage.order.get(uid=merchant.uid, order_id=order_id)


class TestCreateRefundInternal(BaseTestCreateRefund):
    @pytest.fixture
    def send_request(self, client, service_merchant, tvm):
        async def _inner(_, order_id, data):
            return await client.post(
                f'/v1/internal/order/{service_merchant.service_merchant_id}/{order_id}/refund', json=data)

        return _inner

    @pytest.fixture
    async def order(self, client, storage, merchant, service_merchant, create_order_data, tvm):
        r = await client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/',
            json=create_order_data,
        )
        assert r.status == 200
        order_id = (await r.json())['data']['order_id']
        return await storage.order.get(uid=merchant.uid, order_id=order_id)


class TestCreateCustomerSubscriptionTransactionRefund(BaseAcquirerTest, BaseTestOrder):
    @pytest.fixture(autouse=True)
    async def setup(self, storage, order_with_customer_subscription, client, trust_purchase_token,
                    customer_subscription, customer_subscription_transaction, rands):
        order_with_customer_subscription.pay_status = PayStatus.PAID
        await storage.order.save(order_with_customer_subscription)

        customer_subscription_transaction.payment_status = TransactionStatus.CLEARED
        customer_subscription_transaction.trust_order_id = rands()
        await storage.customer_subscription_transaction.save(customer_subscription_transaction)

    @pytest.fixture
    def create_refund_data(self):
        return {
            'caption': 'test-refund-caption',
            'description': 'test-refund-description'
        }

    @pytest.fixture
    def trust_purchase_token(self):
        return 'some_secret_token'

    @pytest.fixture
    def trust_refund_id(self):
        return 'trust_refund_id_1234'

    @pytest.fixture
    def customer_uid(self):
        return 11300011122

    @pytest.fixture
    async def oauth(self, merchant, order):
        if order.get_acquirer(merchant.acquirer) == AcquirerType.KASSA:
            return merchant.oauth[0]
        return None

    @pytest.fixture
    def send_request(self, client, merchant, customer_subscription, customer_subscription_transaction, tvm):
        async def _inner(data):
            uid = merchant.uid
            subs_id = customer_subscription.customer_subscription_id
            tx_id = customer_subscription_transaction.purchase_token
            return await client.post(
                f'/v1/customer_subscription/{uid}/{subs_id}/{tx_id}/refund',
                json=data
            )

        return _inner

    @pytest.fixture
    async def refund_response(self, send_request, create_refund_data):
        r = await send_request(create_refund_data)
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    async def refund(self, storage, merchant, refund_response):
        order_id = refund_response['data']['order_id']
        return await storage.order.get(uid=merchant.uid, order_id=order_id, select_customer_subscription=None)

    @pytest.fixture(autouse=True)
    def payment_get_mock(self, shop_type, trust_client_mocker, customer_uid):
        with trust_client_mocker(shop_type, 'payment_get', {'uid': customer_uid}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_create_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_create_single', trust_refund_id) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_start_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_start', trust_refund_id) as mock:
            yield mock

    def test_response(self, refund_response, refund):
        assert_that(
            refund_response['data'],
            has_entries({
                'order_id': refund.order_id,
                'original_order_id': refund.original_order_id,
                'kind': enum_value(OrderKind.REFUND)
            })
        )

    def test_refund(self, order_with_customer_subscription, refund):
        assert_that(
            refund,
            has_properties({
                'original_order_id': order_with_customer_subscription.order_id,
            }),
        )

    def test_payment_get_call(self, merchant, order_with_customer_subscription, refund_response, payment_get_mock,
                              customer_subscription_transaction):
        payment_get_mock.assert_called_once_with(
            uid=order_with_customer_subscription.uid,
            acquirer=merchant.acquirer,
            purchase_token=customer_subscription_transaction.purchase_token,
            with_terminal_info=True
        )

    def test_refund_create_call(self,
                                merchant,
                                customer_uid,
                                customer_subscription,
                                customer_subscription_transaction,
                                refund_create_mock,
                                refund_response,
                                refund,
                                order_with_customer_subscription,
                                oauth,
                                ):
        refund_create_mock.assert_called_once_with(
            uid=merchant.uid,
            acquirer=merchant.acquirer,
            customer_uid=customer_uid,
            caption=refund.caption,
            quantity=customer_subscription.quantity,
            trust_order_id=customer_subscription_transaction.trust_order_id,
            purchase_token=customer_subscription_transaction.purchase_token,
            submerchant_id=merchant.get_submerchant_id(),
            oauth=oauth,
        )

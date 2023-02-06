import pytest

from sendr_utils import alist

from mail.payments.payments.core.actions.order.send_to_history import SendToHistoryOrderAction
from mail.payments.payments.core.entities.enums import OrderKind, RefundStatus, TransactionStatus


@pytest.fixture(autouse=True)
def post_order_mock(mocker):
    async def dummy_(*args, **kwargs):
        return

    from mail.payments.payments.interactions.ohio import OhioClient
    return mocker.patch.object(
        OhioClient,
        'post_order',
        mocker.Mock(side_effect=dummy_),
    )


@pytest.fixture
def returned_func(order):
    async def _inner(**kwargs):
        kwargs = {'uid': order.uid, 'order_id': order.order_id, **kwargs}
        return await SendToHistoryOrderAction(**kwargs).run()

    return _inner


@pytest.fixture
async def order(randn, create_order, create_item, service_merchant):
    order = await create_order(
        customer_uid=randn(),
        service_merchant_id=service_merchant.service_merchant_id,
    )
    order.service_merchant = service_merchant
    order.items = [
        await create_item(order=order)
        for _ in range(2)
    ]
    return order


@pytest.fixture
async def transaction_data():
    return {'status': TransactionStatus.CLEARED}


@pytest.fixture
async def refunds(rands, create_order, create_item, order):
    refunds = []
    for _ in range(2):
        refund = await create_order(
            kind=OrderKind.REFUND,
            pay_status=None,
            refund_status=RefundStatus.COMPLETED,
            original_order_id=order.order_id,
            trust_refund_id=rands(),
        )
        refund.items = [await create_item(refund) for _ in range(2)]
        refunds.append(refund)
    return refunds


@pytest.mark.asyncio
class TestNotSent:
    @pytest.fixture(autouse=True)
    def setup(self, payments_settings, order, refunds, transaction):
        payments_settings.SEND_TO_OHIO = True

    async def test_default(self, returned, post_order_mock):
        # Checking that default test setup passes all checks and calls post_order
        post_order_mock.assert_called_once()

    async def test_sending_turned_off(self, payments_settings, returned_func, post_order_mock):
        payments_settings.SEND_TO_OHIO = True
        post_order_mock.assert_not_called()

    async def test_order_not_found(self, randn, merchant, returned_func, post_order_mock):
        await returned_func(uid=merchant.uid, order_id=randn())
        post_order_mock.assert_not_called()

    async def test_order_without_service_merchant(self, storage, order, returned_func, post_order_mock):
        order.service_merchant_id = None
        await storage.order.save(order)
        await returned_func()
        post_order_mock.assert_not_called()

    async def test_order_without_customer_uid(self, storage, order, returned_func, post_order_mock):
        order.customer_uid = None
        await storage.order.save(order)
        await returned_func()
        post_order_mock.assert_not_called()

    async def test_order_exclude_stats(self, storage, order, returned_func, post_order_mock):
        order.exclude_stats = True
        await storage.order.save(order)
        await returned_func()
        post_order_mock.assert_not_called()

    async def test_transaction_not_found(self, storage, create_order, transaction, returned_func, post_order_mock):
        # TransactionMapper has no delete method, but we can link transaction to another order
        new_order = await create_order()
        transaction.order_id = new_order.order_id
        await storage.transaction.save(transaction)
        await returned_func()
        post_order_mock.assert_not_called()

    @pytest.mark.parametrize('transaction_status', [
        status
        for status in TransactionStatus
        if not TransactionStatus.was_held(status)
    ])
    async def test_transaction_was_never_held(self, storage, transaction, returned_func, post_order_mock,
                                              transaction_status):
        transaction.status = transaction_status
        await storage.transaction.save(transaction)
        await returned_func()
        post_order_mock.assert_not_called()


@pytest.mark.asyncio
class TestSent:
    @pytest.fixture(autouse=True)
    def setup(self, payments_settings, order, transaction, refunds):
        payments_settings.SEND_TO_OHIO = True

    async def test_post_order_call(
        self,
        order,
        shop,
        transaction,
        refunds,
        service_merchant,
        returned,
        post_order_mock,
    ):
        revision = max(order.revision, transaction.revision)
        for refund in refunds:
            revision = max(revision, refund.revision)
        order.shop = shop
        post_order_mock.assert_called_once_with(
            order=order,
            transaction=transaction,
            refunds=refunds,
            service_merchant=service_merchant,
            revision=revision,
        )

    async def test_revision_order(self, storage, order, returned_func, post_order_mock):
        # Updating order so that it have the highest revision
        order = await storage.order.save(order)
        await returned_func()
        assert order.revision == post_order_mock.call_args[1]['revision']

    async def test_revision_transaction(self, storage, transaction, returned_func, post_order_mock):
        # Updating transaction so that it have the highest revision
        transaction = await storage.transaction.save(transaction)
        await returned_func()
        assert transaction.revision == post_order_mock.call_args[1]['revision']

    async def test_revision_refund(self, storage, refunds, returned_func, post_order_mock):
        # Updating refund so that it have the highest revision
        refund = await storage.order.save(refunds[0])
        await returned_func()
        assert refund.revision == post_order_mock.call_args[1]['revision']

    async def test_skips_not_created_in_trust_refunds(self, storage, refunds, returned_func, post_order_mock):
        refunds[0].trust_refund_id = None
        await storage.order.save(refunds[0])
        await returned_func()
        assert refunds[1:] == post_order_mock.call_args[1]['refunds']


@pytest.mark.asyncio
async def test_run_async(randn, storage):
    uid = randn()
    order_id = randn()
    SendToHistoryOrderAction.context.storage = storage
    await SendToHistoryOrderAction(uid=uid, order_id=order_id).run_async()
    tasks = await alist(storage.task.find())
    assert all((
        len(tasks) == 1,
        tasks[0].params['action_kwargs'] == {'uid': uid, 'order_id': order_id},
        tasks[0].action_name == 'send_to_history_order_action',
    ))

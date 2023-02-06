from datetime import datetime, timedelta

import pytest

from sendr_utils import alist

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.entities.enums import ShopType, TransactionStatus
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.shop import Shop, ShopSettings
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.storage.exceptions import TransactionNotFound
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def transaction(order):
    return Transaction(
        uid=order.uid,
        order_id=order.order_id,
        tx_id=111,
        revision=222,
        trust_purchase_token='xxx',
        trust_payment_url='yyy',
        trust_failed_result='zzz',
        trust_resp_code='ttt',
        trust_payment_id="vvv",
        check_at=utcnow() + timedelta(days=1),
        check_tries=333,
    )


@pytest.fixture
def setup_data():
    return [{'uid': 123}, {'uid': 234}]


@pytest.fixture(autouse=True)
async def merchants(create_merchant, setup_data):
    return [
        await create_merchant(**data)
        for data in setup_data
    ]


@pytest.fixture(autouse=True)
async def shops(storage, merchants):
    return [
        await storage.shop.create(
            Shop(uid=merchant.uid, name='asd', is_default=False, shop_type=ShopType.PROD, settings=ShopSettings())
        ) for merchant in merchants
    ]


@pytest.fixture
def transaction_dict(transaction):
    return {
        attr: getattr(transaction, attr)
        for attr in [
            'uid',
            'order_id',
            'tx_id',
            'revision',
            'created',
            'updated',
            'status',
            'trust_purchase_token',
            'trust_payment_url',
            'trust_failed_result',
            'trust_resp_code',
            'trust_payment_id',
            'trust_terminal_id',
            'poll',
            'check_at',
            'check_tries',
        ]
    }


def test_map(storage, transaction, transaction_dict):
    assert storage.transaction.map(transaction_dict) == transaction


def test_unmap(storage, transaction, transaction_dict):
    transaction_dict.pop('uid')
    transaction_dict.pop('tx_id')
    assert storage.transaction.unmap(transaction) == transaction_dict


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(TransactionNotFound):
        await storage.transaction.get(uid=123, tx_id=321)


@pytest.mark.asyncio
async def test_get_returns(storage, transaction):
    transaction = await storage.transaction.create(transaction)
    tx_from_db = await storage.transaction.get(transaction.uid, transaction.tx_id)
    assert tx_from_db == transaction


@pytest.mark.asyncio
async def test_create(storage, transaction):
    transaction = await storage.transaction.create(transaction)
    assert transaction == await storage.transaction.get(uid=transaction.uid, tx_id=transaction.tx_id)


class TestGetLastByOrder:
    @pytest.fixture
    async def create_transaction(self, storage, transaction):
        return await storage.transaction.create(transaction)

    @pytest.fixture
    def order_id(self, order):
        return order.order_id

    @pytest.fixture
    def uid(self, order):
        return order.uid

    @pytest.fixture
    def raise_(self):
        return False

    @pytest.fixture
    def returned(self, storage, uid, order_id, raise_):
        async def _inner():
            return await storage.transaction.get_last_by_order(uid=uid, order_id=order_id, raise_=raise_)

        return _inner

    @pytest.mark.asyncio
    async def test_not_found(self, returned):
        assert await returned() is None

    @pytest.mark.parametrize('raise_', [True])
    @pytest.mark.asyncio
    async def test_not_found_raises(self, returned):
        with pytest.raises(TransactionNotFound):
            await returned()

    @pytest.mark.asyncio
    async def test_found(self, create_transaction, order, shop, returned):
        res = await returned()
        assert res == create_transaction and res.order == order and res.order.shop == shop

    class TestNotFoundFiltered:
        @pytest.fixture
        def order_id(self, order):
            return order.order_id + 1

        @pytest.fixture
        def uid(self, order):
            return order.uid + 1

        @pytest.mark.asyncio
        async def test_not_found_filtered(self, create_transaction, returned):
            assert await returned() is None

    @pytest.mark.asyncio
    async def test_found_last(self, storage, create_transaction, returned):
        tx = create_transaction
        # create one more (not active/held to satisfy DB restriction)
        tx.status = TransactionStatus.CANCELLED
        tx = await storage.transaction.create(tx)
        res = await returned()
        assert res == tx


class TestGetForCheck:
    @pytest.fixture
    def transaction(self, transaction):
        transaction.status = TransactionStatus.ACTIVE
        transaction.check_at = utcnow() - timedelta(days=1)
        transaction.poll = True
        return transaction

    @pytest.mark.asyncio
    async def test_not_found(self, storage):
        with pytest.raises(TransactionNotFound):
            await storage.transaction.get_for_check()

    @pytest.mark.parametrize('status', [
        TransactionStatus.ACTIVE,
        TransactionStatus.HELD,
    ])
    @pytest.mark.asyncio
    async def test_filters_by_status_found(self, storage, transaction, status, order):
        transaction.status = status
        transaction = await storage.transaction.create(transaction)
        res = await storage.transaction.get_for_check()
        assert res == transaction and res.order == order and res.order.shop == order.shop

    @pytest.mark.parametrize('setup_func', (
        *[
            pytest.param(lambda tx: setattr(tx, 'status', status), id=f'final-status-{status.value}')
            for status in (TransactionStatus.FAILED, TransactionStatus.CANCELLED, TransactionStatus.CLEARED)
        ],
        pytest.param(lambda tx: setattr(tx, 'poll', False), id='polling-turned-off'),
        pytest.param(lambda tx: setattr(tx, 'check_at', utcnow() + timedelta(days=1)), id='check_at-not-ready'),
    ))
    @pytest.mark.asyncio
    async def test_filtered_not_found(self, storage, transaction, setup_func):
        setup_func(transaction)
        transaction = await storage.transaction.create(transaction)
        with pytest.raises(TransactionNotFound):
            await storage.transaction.get_for_check()


@pytest.mark.asyncio
async def test_save_returns(mocker, storage, transaction):
    transaction = await storage.transaction.create(transaction)

    revision = 17
    mocker.patch.object(
        storage.transaction, '_acquire_revision', mocker.Mock(return_value=dummy_coro(revision))
    )

    tx_from_db = await storage.transaction.save(transaction)
    assert_that(
        tx_from_db,
        has_properties({
            'revision': revision,
            'tx_id': transaction.tx_id,
            'uid': transaction.uid,
            'order_id': transaction.order_id,
            'status': transaction.status,
            'trust_purchase_token': transaction.trust_purchase_token,
            'trust_payment_url': transaction.trust_payment_url,
            'trust_failed_result': transaction.trust_failed_result,
            'trust_resp_code': transaction.trust_resp_code,
            'trust_payment_id': transaction.trust_payment_id,
        })
    )


@pytest.mark.asyncio
async def test_save_acquires_revision(mocker, storage, transaction):
    transaction = await storage.transaction.create(transaction)
    acquire_revision = mocker.patch.object(
        storage.transaction, '_acquire_revision', mocker.Mock(return_value=dummy_coro(11)))
    await storage.transaction.save(transaction)
    acquire_revision.assert_called_once_with(transaction.uid)


class TestCounts:
    @pytest.fixture
    def transactions_data(self):
        return [
            {'status': TransactionStatus.ACTIVE},
            {'status': TransactionStatus.FAILED},
        ]

    @pytest.fixture
    async def transactions(self, storage, order, transactions_data):
        return [
            await storage.transaction.create(Transaction(
                uid=order.uid,
                order_id=order.order_id,
                **data,
            ))
            for data in transactions_data
        ]

    @pytest.mark.asyncio
    async def test_count_unfinished(self, storage, transactions):
        assert await storage.transaction.count_unfinished() == 1

    @pytest.mark.asyncio
    async def test_all_counts(self, storage, transactions):
        assert await storage.transaction.get_transactions_count() == len(transactions)


class TestFind:
    @pytest.fixture
    async def orders(self, storage, randmail, randn, shops):
        created = []
        for s in shops:
            order = await storage.order.create(
                Order(uid=s.uid, shop_id=s.shop_id, user_email=randmail(), customer_uid=randn())
            )
            created.append(order)
        return created

    @pytest.fixture
    async def transactions(self, storage, orders):
        return [
            await storage.transaction.create(Transaction(
                uid=order.uid,
                order_id=order.order_id
            ))
            for order in orders
        ]

    @pytest.mark.asyncio
    async def test_email_query(self, storage, orders, transactions):
        found_transactions = [tx async for tx in storage.transaction.find(email_query=orders[0].user_email)]
        assert len(found_transactions) == 1

    @pytest.mark.asyncio
    async def test_found_count_email_query(self, storage, orders, transactions):
        assert await storage.transaction.get_found_count(email_query=orders[0].user_email) == 1

    @pytest.mark.asyncio
    async def test_customer_uid(self, storage, orders, transactions):
        found_transactions = [tx async for tx in storage.transaction.find(customer_uid=orders[0].customer_uid)]
        assert len(found_transactions) == 1

    @pytest.mark.asyncio
    async def test_find(self, storage, transaction, order):
        order.user_email = 'user_email'
        order = await storage.order.save(order)
        transaction = await storage.transaction.create(transaction)
        transaction.user_email = order.user_email

        from_date = datetime.fromtimestamp(transaction.created.timestamp() - 86400)
        to_date = datetime.fromtimestamp(transaction.created.timestamp() + 86400)

        transactions = [
            tx async for tx in
            storage.transaction.find(
                uid=transaction.uid,
                order_id=transaction.order_id,
                statuses=[TransactionStatus.ACTIVE],
                tx_id=transaction.tx_id,
                email_query=order.user_email,
                order_pay_statuses=[order.pay_status],
                created_from=from_date,
                created_to=to_date,
                updated_from=from_date,
                updated_to=to_date,
                limit=0,
                offset=0,
            )
        ]
        assert transactions == [transaction]

    @pytest.mark.parametrize('sort_by', (None, 'created', 'updated'))
    @pytest.mark.parametrize('descending', (True, False))
    @pytest.mark.asyncio
    async def test_sort(self, transaction, storage, merchant, shop, sort_by, descending):
        transactions = []
        transaction_ids = []
        for _ in range(3):
            order = await storage.order.create(Order(uid=merchant.uid, shop_id=shop.shop_id))
            transaction.order_id = order.order_id
            created = await storage.transaction.create(transaction)
            transaction_ids.append(created.tx_id)
            transactions.append(created)

        transactions = sorted(transactions,
                              key=lambda t: getattr(t, sort_by or 'tx_id'),
                              reverse=descending if sort_by else True)

        tx_from_db = [
            tx async for tx in
            storage.transaction.find(sort_by=sort_by, descending=descending)
            if tx.tx_id in transaction_ids
        ]

        assert tx_from_db == transactions

    @pytest.mark.asyncio
    async def test_map_order(self, storage, order, transaction):
        transaction = await storage.transaction.create(transaction)
        tx_from_db = [tx async for tx in storage.transaction.find(uid=transaction.uid, tx_id=transaction.tx_id)][0]
        order.shop = None
        assert tx_from_db.order == order

    @pytest.mark.asyncio
    async def test_filter_by_service(self, storage, orders, transactions, service_merchant):
        orders[0].service_merchant_id = service_merchant.service_merchant_id
        orders[0] = await storage.order.save(orders[0])
        tx_from_db = [tx async for tx in storage.transaction.find(services=[service_merchant.service_id])]
        assert len(tx_from_db) == 1


class TestFindLastByOrders:
    @pytest.fixture
    async def orders(self, storage, shops):
        created = []
        for s in shops:
            order = await storage.order.create(Order(uid=s.uid, shop_id=s.shop_id))
            created.append(order)
        return created

    @pytest.fixture
    async def transactions(self, storage, orders):
        return [
            await storage.transaction.create(Transaction(
                uid=order.uid,
                order_id=order.order_id,
                status=tx_status
            ))
            for order in orders for tx_status in (TransactionStatus.ACTIVE, TransactionStatus.FAILED)
        ]

    @pytest.mark.asyncio
    async def test_find_last_by_orders(self, storage, merchants, orders, transactions):
        uid_and_order_id_list = set((tx.uid, tx.order_id) for tx in transactions)
        transactions = await alist(storage.transaction.find_last_by_orders(uid_and_order_id_list))
        assert all([tx.tx_id == 2 for tx in transactions])


class TestDelete:
    @pytest.mark.asyncio
    async def test_delete(self, storage, transaction):
        with pytest.raises(NotImplementedError):
            await storage.transaction.delete(transaction)

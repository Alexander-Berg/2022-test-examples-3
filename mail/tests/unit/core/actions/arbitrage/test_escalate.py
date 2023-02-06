import pytest

from sendr_taskqueue.worker.storage import TaskState

from hamcrest import assert_that, contains_inanyorder, has_entries, has_properties, match_equality

from mail.payments.payments.core.actions.arbitrage.escalate import EscalateArbitrageAction, StartEscalateAction
from mail.payments.payments.core.actions.order.get import CoreGetOrderAction
from mail.payments.payments.core.entities.enums import ArbitrageStatus, TaskType
from mail.payments.payments.core.exceptions import ActiveArbitrageAbsentError, OrderNotFoundError
from mail.payments.payments.tests.base import BaseTestOrderAction


class TestEscalateArbitrageAction(BaseTestOrderAction):
    @pytest.fixture
    def order_id(self, order):
        return order.order_id

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def order_data(self, customer_uid):
        return {
            'customer_uid': customer_uid
        }

    @pytest.fixture
    def returned_func(self, order, crypto, order_id, customer_uid):
        async def _inner():
            return await EscalateArbitrageAction(order.uid, order_id, customer_uid).run()

        return _inner

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_id', (-1,))
    async def test_not_found(self, returned_func):
        with pytest.raises(OrderNotFoundError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_already_arbitrage(self, returned_func):
        with pytest.raises(ActiveArbitrageAbsentError):
            await returned_func()

    def test_returned(self, arbitrage, order, returned):
        assert_that(returned, has_properties({
            'uid': order.uid,
            'order_id': order.order_id,
            'status': ArbitrageStatus.ESCALATE
        }))

    @pytest.mark.asyncio
    async def test_tasks(self, arbitrage, returned, order, get_tasks):
        assert_that(
            await get_tasks(),
            contains_inanyorder(
                has_properties({
                    'action_name': 'start_escalate',
                    'task_type': TaskType.RUN_ACTION,
                    'state': TaskState.PENDING,
                    'params': has_entries({
                        'action_kwargs': {
                            'uid': order.uid,
                            'order_id': order.order_id
                        }
                    })
                })
            )
        )


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestStartEscalateAction(BaseTestOrderAction):
    @pytest.fixture
    def order_id(self, order):
        return order.order_id

    @pytest.fixture
    def escalate_id(self, rands):
        return rands()

    @pytest.fixture
    def arbiter_chat_id(self, rands):
        return rands()

    @pytest.fixture
    def messages(self):
        return []

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def order_data(self, customer_uid):
        return {
            'customer_uid': customer_uid
        }

    @pytest.fixture(autouse=True)
    def get_consultation_history_mock(self, floyd_client_mocker, messages):
        with floyd_client_mocker('get_consultation_history', messages) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def create_organizations_chat_mock(self, floyd_client_mocker, arbiter_chat_id):
        with floyd_client_mocker('create_organizations_chat', (arbiter_chat_id, None)) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def custom_action_mock(self, floyd_client_mocker):
        with floyd_client_mocker('custom_action') as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def update_chatbar_mock(self, floyd_client_mocker):
        with floyd_client_mocker('update_chatbar') as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def create_arbitrage_mock(self, escalate_id, arbiter_client_mocker):
        with arbiter_client_mocker('create_arbitrage', escalate_id) as mock:
            yield mock

    @pytest.fixture
    def returned_func(self, order, crypto, order_id, customer_uid):
        async def _inner():
            CoreGetOrderAction.context.crypto = crypto
            return await StartEscalateAction(order.uid, order_id).run()

        return _inner

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_id', (-1,))
    async def test_not_found(self, returned_func):
        with pytest.raises(OrderNotFoundError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_not_escalated(self, returned, get_consultation_history_mock, create_organizations_chat_mock,
                                 custom_action_mock, create_arbitrage_mock):
        mocks = [
            get_consultation_history_mock,
            create_organizations_chat_mock,
            custom_action_mock,
            create_arbitrage_mock
        ]
        for mock in mocks:
            mock.assert_not_called()

    @pytest.mark.usefixtures('base_merchant_action_data_mock', 'returned')
    class TestEscalated:
        @pytest.fixture(autouse=True)
        async def setup(self, mock_action, arbitrage, order, items, storage):
            mock_action(CoreGetOrderAction, order)
            arbitrage.status = ArbitrageStatus.ESCALATE
            await storage.arbitrage.save(arbitrage)

        @pytest.mark.asyncio
        async def test_escalated__result(self, arbitrage, arbiter_chat_id, escalate_id, storage):
            arbitrage = await storage.arbitrage.get(arbitrage.arbitrage_id)
            assert_that(arbitrage, has_properties({
                'arbiter_chat_id': arbiter_chat_id,
                'escalate_id': escalate_id
            }))

        @pytest.mark.asyncio
        async def test_escalated__get_consultation_history_mock(self, merchant, get_consultation_history_mock,
                                                                arbitrage):
            get_consultation_history_mock.assert_called_once_with(merchant.dialogs_org_id, arbitrage.consultation_id)

        @pytest.mark.asyncio
        async def test_escalated__update_chatbar_mock_mock(self, merchant, update_chatbar_mock,
                                                           arbitrage):
            update_chatbar_mock.assert_called_once_with(
                org_id=merchant.dialogs_org_id,
                consultation_id=arbitrage.consultation_id,
                role_id='client',
                chatbar={'title': None, 'button': None},
            )

        @pytest.mark.asyncio
        async def test_escalated__create_organizations_chat_mock(self, merchant, create_organizations_chat_mock,
                                                                 payments_settings):
            create_organizations_chat_mock.assert_called_once_with(merchant.dialogs_org_id,
                                                                   payments_settings.FLOYD_ARBITRAGE_ORG_ID)

        @pytest.mark.asyncio
        async def test_escalated__create_arbitrage_mock(self, merchant, create_arbitrage_mock, order, arbitrage,
                                                        messages):
            create_arbitrage_mock.assert_called_once_with(
                merchant,
                order,
                match_equality(has_properties({'arbitrage_id': arbitrage.arbitrage_id})),
                messages
            )

        @pytest.mark.asyncio
        async def test_escalated__custom_action_mock(self, merchant, arbitrage, custom_action_mock,
                                                     payments_settings):
            custom_action_mock.assert_called_once_with(
                merchant.dialogs_org_id,
                arbitrage.consultation_id,
                'startArbitrage',
                {'arbitrage_org_id': payments_settings.FLOYD_ARBITRAGE_ORG_ID}
            )

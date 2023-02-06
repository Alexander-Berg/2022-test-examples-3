import uuid
from datetime import datetime, timezone

import pytest

from sendr_utils import alist

from hamcrest import all_of, assert_that, contains_inanyorder, has_entries, has_properties, instance_of, is_

from mail.payments.payments.core.actions.interactions.yandex_pay_admin import MerchantModerationResultNotifyAction
from mail.payments.payments.core.actions.worker.moderation import (
    InitContractAction, InitProductsAction, NotifyAboutOrderModerationResultAction, ProcessFastModerationRequestAction,
    StartMerchantModerationAction, StartOrderModerationWorkerAction, StartSubscriptionModerationWorkerAction,
    UpdateModerationAction
)
from mail.payments.payments.core.actions.worker.moderation_result_notify import (
    MerchantModerationResultNotifyWorkerAction
)
from mail.payments.payments.core.entities.document import Document, DocumentType
from mail.payments.payments.core.entities.enums import CallbackMessageType, FunctionalityType
from mail.payments.payments.core.entities.functionality import MerchantFunctionality, YandexPayMerchantFunctionalityData
from mail.payments.payments.core.entities.log import MerchantModerationApprovedLog
from mail.payments.payments.core.entities.moderation import (
    FastModerationRequest, Moderation, ModerationResult, ModerationType
)
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.subscription import Subscription
from mail.payments.payments.core.entities.task import Task, TaskType
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.file_storage.yandex_pay_admin import YandexPayAdminFileStorage
from mail.payments.payments.interactions.mds import MDSClient
from mail.payments.payments.tests.base import BaseTestParent
from mail.payments.payments.tests.utils import dummy_async_context_manager, dummy_coro
from mail.payments.payments.utils.datetime import utcnow
from mail.payments.payments.utils.helpers import decimal_format, method_return_value_spy, temp_setattr


class BaseTestStartModeration:
    @pytest.fixture(params=(False, True))
    def entity_fast_moderation(self, request):
        return request.param

    @pytest.fixture(params=(False, True))
    def payments_settings(self, request, payments_settings):
        payments_settings.LB_ALLOW_FAST_MODERATION = request.param
        return payments_settings

    @pytest.fixture
    def producer_path(self, entity_fast_moderation, payments_settings):
        if entity_fast_moderation and payments_settings.LB_ALLOW_FAST_MODERATION:
            return 'mail.payments.payments.core.actions.worker.moderation.FastModerationRequestProducer'
        return 'mail.payments.payments.core.actions.worker.moderation.ModerationProducer'

    @pytest.fixture(autouse=True)
    def producer_cls_mock(self, mocker, producer_path, producer_mock):
        yield mocker.patch(
            producer_path,
            mocker.Mock(return_value=dummy_async_context_manager(producer_mock)),
        )


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestStartMerchantModerationAction(BaseTestStartModeration):
    @pytest.fixture
    async def merchant(self, storage, merchant, entity_fast_moderation):
        merchant.data.fast_moderation = entity_fast_moderation
        merchant = await storage.merchant.save(merchant)
        merchant.oauth = []
        return merchant

    @pytest.fixture
    def upload_documents_mock(self, mocker):
        coro = dummy_coro()
        yield mocker.patch.object(
            StartMerchantModerationAction,
            '_upload_documents',
            mocker.Mock(return_value=coro),
        )
        coro.close()

    @pytest.fixture(autouse=True)
    def producer_mock(self, mocker):
        coro = dummy_coro()
        mock = mocker.Mock()
        mock.write_merchant.return_value = coro
        yield mock
        coro.close()

    @pytest.fixture
    def task(self, params):
        return Task(
            task_type=TaskType.START_MODERATION,
            params=params,
        )

    @pytest.fixture
    def params(self, lb_factory_mock, merchant):
        with temp_setattr(StartMerchantModerationAction.context, 'lb_factory', lb_factory_mock):
            yield dict(merchant_uid=merchant.uid, functionality_type=FunctionalityType.PAYMENTS.value)

    @pytest.fixture
    def action(self, params):
        return StartMerchantModerationAction(**params)

    @pytest.fixture
    def returned_func(self, upload_documents_mock, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def created_moderation(self, storage, merchant, returned):
        return await storage.moderation.find(uid=merchant.uid).__anext__()

    def test_creates_moderation(self, merchant, created_moderation):
        assert created_moderation.uid == merchant.uid and created_moderation.revision == merchant.revision

    def test_write_merchant_call(self, producer_mock, merchant, created_moderation):
        producer_mock.write_merchant.assert_called_once_with(created_moderation, merchant)

    def test_write_merchant_call_with_loaded_parent_merchant(self, producer_mock, returned):
        producer_mock.write_merchant.call_args[0][1].client_id

    def test_write_merchant_call_with_loaded_data_merchant(self, producer_mock, returned):
        producer_mock.write_merchant.call_args[0][1].organization

    class TestOrdinary:
        def test_ordinary__upload_documents_call(self, upload_documents_mock, merchant, created_moderation):
            upload_documents_mock.assert_called_once_with(created_moderation, merchant)

    class TestReusesExistingModeration:
        @pytest.fixture
        def moderations_data(self, merchant):
            return [{'revision': merchant.revision}]

        @pytest.fixture
        def existing_moderation(self, moderations):
            return moderations[0]

        @pytest.mark.asyncio
        async def test_does_not_create_moderation(self, storage, merchant, existing_moderation, returned):
            moderations = [m async for m in storage.moderation.find(uid=merchant.uid)]
            assert len(moderations) == 1 and moderations[0] == existing_moderation

        def test_resuses_existing_moderation__upload_documents_call(self, upload_documents_mock, merchant,
                                                                    existing_moderation, returned):
            upload_documents_mock.assert_called_once_with(existing_moderation, merchant)

    class TestUploadDocuments:
        @pytest.fixture
        def moderations_data(self, merchant):
            return [{'revision': merchant.revision}]

        @pytest.fixture
        def existing_moderation(self, moderations):
            return moderations[0]

        @pytest.fixture
        def file_data(self):
            return 'async iter'

        @pytest.fixture
        def uploaded_path(self):
            return 'test-upload-documents-uploaded-path'

        @pytest.fixture(autouse=True)
        def download_mock(self, mds_client_mocker, file_data):
            with mds_client_mocker('download', ('headers', file_data)) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def upload_mock(self, mds_client_mocker, uploaded_path):
            with mds_client_mocker('upload', uploaded_path) as mock:
                yield mock

        @pytest.fixture
        def merchant_documents(self):
            return [
                Document(
                    document_type=DocumentType.PASSPORT,
                    path='test-passport-document-path',
                    size=1234,
                    name='test-passport-document',
                ),
            ]

        @pytest.fixture
        async def returned(self, action, existing_moderation, merchant):
            return await action._upload_documents(existing_moderation, merchant)

        def test_sets_moderation_url(self, merchant, returned, uploaded_path):
            assert uploaded_path in merchant.documents[0].moderation_url

        def test_download_call(self, merchant, returned, download_mock):
            download_mock.assert_called_once_with(merchant.documents[0].path)

        def test_upload_call(self, merchant, existing_moderation, returned, file_data, upload_mock):
            upload_mock.assert_called_once_with(f'moderation_{existing_moderation.moderation_id}', file_data)

        @pytest.mark.asyncio
        async def test_skips_unsupported_documents(self, merchant, action, existing_moderation, upload_mock):
            merchant.documents = [
                Document(
                    document_type=DocumentType.PCI_DSS_CERT,
                    path='test-pci-dss-cert-document-path',
                    size=1234,
                    name='test-pci-dss-cert-document',
                ),
            ]

            await action._upload_documents(existing_moderation, merchant)

            upload_mock.assert_not_called()


class TestStartOrderModerationWorkerAction(BaseTestParent, BaseTestStartModeration):
    """Действие по отправке модерации заказа вызывает ModerationProducer."""

    @pytest.fixture
    async def order(self, storage, order, entity_fast_moderation):
        order.data.fast_moderation = entity_fast_moderation
        return await storage.order.save(order)

    @pytest.fixture
    def parent_uid(self, parent_merchant):
        return parent_merchant.uid

    @pytest.fixture
    def producer_mock(self, mocker):
        mock = mocker.Mock()
        mock.write_order.return_value = dummy_coro()
        yield mock
        mock.write_order.return_value.close()

    @pytest.fixture
    async def moderation(self, storage, order: Order) -> Moderation:
        """Pending модерация для заказа."""
        moderation = Moderation(uid=order.uid,
                                revision=order.revision,
                                entity_id=order.order_id,
                                moderation_type=ModerationType.ORDER,
                                approved=None)  # pending
        return await storage.moderation.create(moderation)

    @pytest.fixture
    def task(self, params):
        return Task(
            task_type=TaskType.START_ORDER_MODERATION,
            params=params,
        )

    @pytest.fixture
    def params(self, lb_factory_mock, moderation):
        with temp_setattr(StartOrderModerationWorkerAction.context, 'lb_factory', lb_factory_mock):
            yield dict(moderation_id=moderation.moderation_id)

    @pytest.fixture
    async def returned(self, params):
        return await StartOrderModerationWorkerAction(**params).run()

    @pytest.fixture
    async def order_with_items(self, order, items):
        order.items = items
        return order

    @pytest.mark.asyncio
    async def test_write_order_call(self, producer_mock, order_with_items, merchant, moderation, returned, storage):
        merchant.parent = await storage.merchant.get(uid=merchant.parent_uid)
        merchant.client_id = merchant.parent.client_id
        merchant.person_id = merchant.parent.person_id
        merchant.contract_id = merchant.parent.contract_id
        merchant.submerchant_id = merchant.parent.submerchant_id
        merchant.data = merchant.parent.data

        producer_mock.write_order.assert_called_once_with(
            moderation=moderation,
            order=order_with_items,
            merchant=merchant
        )


class TestStartSubscriptionModerationWorkerAction(BaseTestParent, BaseTestStartModeration):
    """Действие по отправке модерации подписки вызывает ModerationProducer."""

    @pytest.fixture
    async def subscription(self, storage, subscription, entity_fast_moderation):
        subscription.data.fast_moderation = entity_fast_moderation
        return await storage.subscription.save(subscription)

    @pytest.fixture
    def producer_mock(self, mocker):
        mock = mocker.Mock()
        mock.write_subscription.return_value = dummy_coro()
        yield mock
        mock.write_subscription.return_value.close()

    @pytest.fixture
    async def moderation(self, storage, subscription: Subscription) -> Moderation:
        """Pending модерация для заказа."""
        moderation = Moderation(uid=subscription.uid,
                                revision=subscription.revision,
                                entity_id=subscription.subscription_id,
                                moderation_type=ModerationType.SUBSCRIPTION,
                                approved=None)  # pending
        return await storage.moderation.create(moderation)

    @pytest.fixture
    def task(self, params):
        return Task(
            task_type=TaskType.START_SUBSCRIPTION_MODERATION,
            params=params
        )

    @pytest.fixture
    def params(self, lb_factory_mock, moderation):
        with temp_setattr(StartSubscriptionModerationWorkerAction.context, 'lb_factory', lb_factory_mock):
            yield dict(moderation_id=moderation.moderation_id)

    @pytest.fixture
    async def returned(self, params):
        return await StartSubscriptionModerationWorkerAction(**params).run()

    @pytest.mark.asyncio
    async def test_write_order_call(self, producer_mock, merchant, moderation, subscription, returned, storage):
        producer_mock.write_subscription.assert_called_once_with(moderation=moderation,
                                                                 subscription=subscription,
                                                                 merchant=merchant)


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestUpdateModerationAction(BaseTestParent):
    @pytest.fixture
    def offer_external_id(self):
        return None

    @pytest.fixture(autouse=True)
    def spy_mocks(self, mocker):
        mocker.spy(UpdateModerationAction, '_update_moderation')

    @pytest.fixture(autouse=True)
    def init_contract(self, mock_action, merchant, offer_external_id):
        return mock_action(InitContractAction, (merchant, offer_external_id))

    @pytest.fixture(autouse=True)
    def init_products(self, mock_action):
        return mock_action(InitProductsAction)

    @pytest.fixture
    def moderation_type(self):
        return ModerationType.MERCHANT

    @pytest.fixture
    def moderation_approved(self):
        return None

    @pytest.fixture
    def moderation_unixtime(self):
        return None

    @pytest.fixture
    def moderation_ignore(self):
        return False

    @pytest.fixture
    def functionality_type(self, moderation_type):
        return FunctionalityType.PAYMENTS if moderation_type == ModerationType.MERCHANT else None

    @pytest.fixture
    async def moderation(self, randn, moderation_type, moderation_approved, moderation_unixtime,
                         moderation_ignore, storage, merchant, order, transaction, functionality_type) -> Moderation:
        # if moderation type is ORDER then put its order_id in entity_id
        entity_id = order.order_id if moderation_type == ModerationType.ORDER else None
        return await storage.moderation.create(Moderation(
            uid=merchant.uid,
            entity_id=entity_id,
            revision=randn(),
            moderation_type=moderation_type,
            functionality_type=functionality_type,
            approved=moderation_approved,
            unixtime=moderation_unixtime,
            ignore=moderation_ignore,
        ))

    @pytest.fixture
    def result_approved(self):
        return True

    @pytest.fixture
    def result_unixtime(self):
        return 111000

    @pytest.fixture
    def result_reasons(self):
        return []

    @pytest.fixture
    def moderation_result(self, result_approved, result_unixtime, result_reasons,
                          moderation, merchant) -> ModerationResult:
        return ModerationResult(
            moderation_id=moderation.moderation_id,
            client_id=merchant.client_id,
            submerchant_id=merchant.submerchant_id,
            approved=result_approved,
            reason='Some reason',
            reasons=result_reasons,
            unixtime=result_unixtime,
        )

    @pytest.fixture
    def params(self, moderation_result):
        return {
            'moderation_result': moderation_result,
        }

    @pytest.fixture
    def should_update(self):
        """Mock decision point of Update action with this fixture"""
        return True

    @pytest.fixture(autouse=True)
    def should_update_mock(self, should_update, mocker):
        mocker.patch.object(UpdateModerationAction, '_should_update', mocker.Mock(return_value=should_update))

    @pytest.fixture
    def action(self, params):
        return UpdateModerationAction(**params)

    @pytest.fixture
    def returned_func(self, action, params, should_update):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.mark.parametrize(
        ('moderation_unixtime', 'result_unixtime', 'should_update'),
        ([None, 100, True],
         [10, 100, True],
         [100, 10, False])
    )
    def test_when_should_update(self, action, moderation, moderation_result, moderation_unixtime, result_unixtime,
                                should_update):
        """Should update only for recent moderation result"""
        assert should_update == action._should_update(moderation=moderation, moderation_result=moderation_result)

    @pytest.mark.asyncio
    async def test_calls_update_method(self, returned):
        UpdateModerationAction._update_moderation.assert_called_once()

    @pytest.mark.asyncio
    async def test_update_method_call_args(self, returned, moderation, moderation_result):
        call_kwargs = UpdateModerationAction._update_moderation.call_args[1]
        assert_that(call_kwargs, has_entries({
            # moderation is changed
            'moderation': all_of(instance_of(Moderation), has_properties({'moderation_id': moderation.moderation_id})),
            'moderation_result': moderation_result,
        }))

    @pytest.mark.parametrize('should_update', [False])
    @pytest.mark.asyncio
    async def test_not_calls_update_if_should_not_update(self, should_update, returned):
        UpdateModerationAction._update_moderation.assert_not_called()

    @pytest.mark.parametrize('result_approved', [True, False])
    @pytest.mark.parametrize('result_unixtime', [200, 300])
    @pytest.mark.parametrize('reasons', [[1, 2, 3], [4, 5, 6]])
    @pytest.mark.asyncio
    async def test_update(self, storage, moderation_result, returned, result_approved, result_unixtime, reasons):
        moderation = await storage.moderation.get(moderation_result.moderation_id)
        assert_that(
            moderation,
            has_properties({
                'approved': moderation_result.approved,
                'reason': moderation_result.reason,
                'reasons': moderation_result.reasons,
                'unixtime': moderation_result.unixtime,
            }),
        )

    @pytest.mark.asyncio
    async def test_pass_when_moderation_not_found(self, storage, moderation_result, params):
        moderation_result.moderation_id += 1
        assert await UpdateModerationAction(**params).run() is None

    @pytest.mark.parametrize('moderation_type', [ModerationType.MERCHANT])
    @pytest.mark.parametrize('result_approved', [True, False])
    @pytest.mark.parametrize('offer_external_id', ['1', None])
    @pytest.mark.asyncio
    async def test_notification_task_created_when_merchant_moderation_updated(self,
                                                                              service_merchant,
                                                                              service_client,
                                                                              returned,
                                                                              merchant,
                                                                              merchant_api_callback_url,
                                                                              result_approved,
                                                                              moderation_result, params, storage,
                                                                              moderation_type, offer_external_id):
        assert_that(
            await alist(storage.task.find()),
            contains_inanyorder(
                has_properties(
                    task_type=TaskType.MERCHANT_MODERATION_RESULT_NOTIFY,
                    params=dict(
                        moderation_id=returned.moderation_id,
                        unixtime=moderation_result.unixtime,
                        offer_external_id=offer_external_id if result_approved else None,
                    )
                ),
                has_properties(
                    task_type=TaskType.API_CALLBACK,
                    params=has_entries({
                        'tvm_id': None,
                        'message': {'uid': merchant.uid, 'approved': returned.approved},
                        'callback_url': merchant_api_callback_url,
                        'callback_message_type': CallbackMessageType.MERCHANT_MODERATION_UPDATED.value
                    })
                ),
                has_properties(
                    task_type=TaskType.API_CALLBACK,
                    params=has_entries({
                        'tvm_id': service_client.tvm_id,
                        'message': {
                            'service_merchant_id': service_merchant.service_merchant_id,
                            'approved': returned.approved
                        },
                        'callback_url': service_client.api_callback_url,
                        'callback_message_type': CallbackMessageType.MERCHANT_MODERATION_UPDATED.value
                    })
                )
            )
        )

    class TestCallsOnApprovePayments:
        @pytest.fixture
        def result_approved(self):
            return True

        @pytest.fixture
        def moderation_type(self):
            return ModerationType.MERCHANT

        def test_init_contract_called(self, merchant, init_contract, returned):
            init_contract.assert_called_once_with(uid=merchant.uid)

        def test_init_products_called(self, merchant, init_products, returned):
            init_products.assert_called_once_with(uid=merchant.uid)

        @pytest.mark.parametrize('service_fee', [None, 1])
        def test_init_products_called_service_fee(self, service_fee, service_options, mocker, merchant, init_products,
                                                  service, service_merchant, returned):
            calls = [mocker.call(uid=merchant.uid)]
            if service_fee:
                calls.append(mocker.call(uid=merchant.uid, service_fee=service_options.service_fee))
            assert init_products.mock_calls == calls

        def test_moderation_approve_logged(self, merchant, returned, pushers_mock):
            assert_that(
                pushers_mock.log.push.call_args[0][0],
                all_of(
                    is_(MerchantModerationApprovedLog),
                    has_properties(dict(
                        merchant_uid=merchant.uid,
                        merchant_name=merchant.name,
                        merchant_acquirer=merchant.acquirer,
                        merchant_full_name=merchant.organization.full_name,
                        merchant_type=merchant.organization.type.value,
                        site_url=merchant.organization.site_url,
                    ))
                ),
            )

    class TestCallsOnApproveYandexPay:
        @pytest.fixture(autouse=True)
        def filestorage_mock(self, mocker):
            mock = mocker.Mock()
            mocker.patch.object(
                YandexPayAdminFileStorage,
                'acquire',
                mocker.Mock(return_value=dummy_async_context_manager(mock))
            )
            mock.upload_stream = mocker.Mock(return_value=dummy_async_context_manager(mock))
            mock.write = mocker.AsyncMock()
            return mock

        @pytest.fixture(autouse=True)
        def mock_result_notify(self, mock_action):
            return mock_action(MerchantModerationResultNotifyAction)

        @pytest.fixture
        def file_data(self):
            return 'file data data'

        @pytest.fixture(autouse=True)
        def download_mock(self, mocker, file_data):
            async def iterable():
                yield file_data

            return mocker.patch.object(
                MDSClient,
                'download',
                mocker.AsyncMock(
                    side_effect=lambda *args, **kw: ('headers', iterable())
                )
            )

        @pytest.fixture
        def result_approved(self):
            return True

        @pytest.fixture
        def moderation_type(self):
            return ModerationType.MERCHANT

        @pytest.fixture
        def functionality_type(self):
            return FunctionalityType.YANDEX_PAY

        @pytest.fixture
        def moderation_approved(self):
            return True

        @pytest.fixture
        def partner_id(self):
            return uuid.uuid4()

        @pytest.fixture(autouse=True)
        async def create_merchant_functionality(self, merchant, storage, partner_id):
            await storage.functionality.create(
                MerchantFunctionality(
                    uid=merchant.uid,
                    functionality_type=FunctionalityType.YANDEX_PAY,
                    data=YandexPayMerchantFunctionalityData(partner_id=partner_id),
                )
            )

        @pytest.mark.asyncio
        async def test_uploads_files_to_yandex_pay_admin_s3(
            self, mocker, merchant, returned_func, filestorage_mock, file_data
        ):
            await returned_func()

            filestorage_mock.upload_stream.assert_has_calls([mocker.call(d.path) for d in merchant.documents])
            filestorage_mock.write.assert_has_calls([mocker.call(file_data) for d in merchant.documents])

        @pytest.mark.asyncio
        async def test_calls_notify_yandex_pay_admin_action(
            self, merchant, partner_id, returned_func, mock_result_notify, moderation
        ):
            await returned_func()

            mock_result_notify.assert_called_once_with(
                partner_id=partner_id,
                verified=moderation.approved,
                documents=[document for document in merchant.documents],
            )

    class TestNoCalls:
        @pytest.fixture
        def moderation_type(self):
            return ModerationType.MERCHANT

        @pytest.mark.parametrize('result_approved,moderation_ignore', ((False, False), (True, True)))
        def test_init_contract_not_called(self, init_contract, returned):
            init_contract.assert_not_called()

        @pytest.mark.parametrize('result_approved,moderation_ignore', ((False, False), (True, True)))
        def test_init_products_not_called(self, init_products, returned):
            init_products.assert_not_called()

    class TestNotifyAboutOrderModerationResultCalls:
        @pytest.fixture(autouse=True)
        def notify_calls(self, mock_action):
            return mock_action(NotifyAboutOrderModerationResultAction).call_args_list

        @pytest.fixture
        def moderation_type(self):
            return ModerationType.ORDER

        @pytest.mark.parametrize('result_approved', [False])
        @pytest.mark.asyncio
        async def test_called(self, notify_calls, returned, moderation, storage, result_approved):
            _, kwargs = notify_calls[0]
            updated_moderation = await storage.moderation.get(moderation_id=moderation.moderation_id)
            assert_that(kwargs, has_entries({'order_moderation': updated_moderation}))

        @pytest.mark.parametrize('result_approved', [True])
        @pytest.mark.asyncio
        async def test_not_called(self, returned, notify_calls, result_approved):
            assert notify_calls == []

    class TestResetsTransactionCheckTries:
        @pytest.fixture
        def moderation_type(self):
            return ModerationType.ORDER

        @pytest.fixture
        def check_at(self):
            return datetime(2019, 8, 20, 10, 20, 30, tzinfo=timezone.utc)

        @pytest.fixture
        def check_tries(self):
            return 0

        @pytest.fixture(autouse=True)
        def reset_tries_mock(self, mocker, check_at, check_tries):
            def reset_check_tries_dummy(self):
                self.check_at = check_at
                self.check_tries = check_tries

            return mocker.patch.object(Transaction, 'reset_check_tries', reset_check_tries_dummy, )

        @pytest.mark.asyncio
        async def test_resets_transaction_tries(self, storage, transaction, returned, check_at, check_tries):
            tx = await storage.transaction.get(uid=transaction.uid, tx_id=transaction.tx_id)
            assert tx.check_at == check_at and tx.check_tries == check_tries


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestMerchantModerationResultNotifyWorkerAction:
    """Test handler for Merchant moderation result task"""

    @pytest.fixture(autouse=True)
    def sender_mock(self, sender_client_mocker):
        """Затычка для метода нотификации на sender client"""
        with sender_client_mocker('send_transactional_letter') as method_mock:
            yield method_mock

    @pytest.fixture
    def notify_method(self, mocker):
        mock = mocker.patch.object(
            MerchantModerationResultNotifyWorkerAction,
            'send_merchant_moderation_result_letter',
            return_value=dummy_coro(result='message_id'),
        )
        yield mock
        mock.return_value.close()

    @pytest.fixture(autouse=True)
    def mailing_id(self, mocker):
        mailing_id = str(uuid.uuid4())
        mocker.patch.object(
            MerchantModerationResultNotifyWorkerAction,
            'get_mailing_id',
            return_value=mailing_id,
        )
        return mailing_id

    @pytest.fixture
    def task_moderation_unixtime(self):
        return 100

    @pytest.fixture
    def moderation_unixtime(self):
        return 100

    @pytest.fixture
    def moderation_ignore(self):
        return False

    @pytest.fixture
    async def moderation(self, randn, moderation_ignore, moderation_unixtime, merchant, storage) -> Moderation:
        return await storage.moderation.create(Moderation(
            uid=merchant.uid,
            revision=randn(),
            moderation_type=ModerationType.MERCHANT,
            functionality_type=FunctionalityType.PAYMENTS,
            approved=True,
            unixtime=moderation_unixtime,
            ignore=moderation_ignore,
        ))

    @pytest.fixture
    def offer_external_id(self):
        return None

    @pytest.fixture
    async def task(self, params):
        return Task(task_type=TaskType.MERCHANT_MODERATION_RESULT_NOTIFY, params=params)

    @pytest.fixture
    def params(self, moderation, task_moderation_unixtime, offer_external_id):
        return dict(
            moderation_id=moderation.moderation_id,
            unixtime=task_moderation_unixtime,
            offer_external_id=offer_external_id
        )

    @pytest.fixture
    async def run(self, params):
        return await MerchantModerationResultNotifyWorkerAction(**params).run()

    @pytest.mark.parametrize('offer_external_id', [None, '1'])
    @pytest.mark.asyncio
    async def test_calls_sent_method_on_unixtime_match(self, moderation, merchant, notify_method, offer_external_id,
                                                       run):
        notify_method.assert_called_once_with(
            merchant=merchant,
            moderation=moderation,
            offer_external_id=offer_external_id,
        )

    @pytest.mark.asyncio
    async def test_send_method(self, moderation, merchant, offer_external_id, run, sender_mock, mailing_id, mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.utils.datetime.utcnow', mocker.Mock(return_value=now))
        sender_mock.assert_called_once_with(
            mailing_id=mailing_id,
            to_email=merchant.contact.email,
            render_context={
                'offer_external_id': offer_external_id,
                'offer_id': merchant.contract_id,
                'reasons': moderation.reasons or None,
                'date': now.strftime('%d.%m.%Y'),
            }
        )

    @pytest.mark.asyncio
    async def test_notification_sent_on_unixtime_match(self, moderation, merchant, notify_method, offer_external_id,
                                                       run):
        merchant.moderation = notify_method.call_args[1]['merchant'].moderation
        notify_method.assert_called_once_with(
            merchant=merchant,
            moderation=moderation,
            offer_external_id=offer_external_id,
        )

    class BaseTestNotSent:
        """Проверка ситуации, когда уведомление слать не нужно. Наследники в фикстурах описывают ситуации."""

        async def check_notification_not_sent(self, notify_method, run):
            notify_method.assert_not_called()

    class TestNotSentWhenUnixtimeMismatch(BaseTestNotSent):
        @pytest.fixture
        def task_moderation_unixtime(self):
            return 100

        @pytest.fixture
        def moderation_unixtime(self):
            return 200

        @pytest.mark.asyncio
        async def test_not_sent_when_unixtime_mismatch__notification_not_sent(self, notify_method, run):
            await self.check_notification_not_sent(notify_method, run)

    class TestNotSentWhenOngoingModeration(BaseTestNotSent):
        @pytest.fixture(autouse=True)
        async def ongoing_moderation(self, storage, merchant, moderation):
            merchant = await storage.merchant.save(merchant)
            second_moderation = Moderation(
                uid=merchant.uid,
                revision=merchant.revision,
                functionality_type=FunctionalityType.PAYMENTS,
                moderation_type=ModerationType.MERCHANT,
                approved=None,
            )
            return await storage.moderation.create(second_moderation)

        @pytest.mark.asyncio
        async def test_not_sent_when_ongoing_moderation__notification_not_sent(self, notify_method, run):
            await self.check_notification_not_sent(notify_method, run)

    class TestNotSentWhenModerationIgnored(BaseTestNotSent):
        @pytest.fixture(autouse=True)
        def moderation_ignore(self):
            return True

        @pytest.mark.asyncio
        async def test_not_sent_when_moderation_ignored__notification_not_sent(self, notify_method, run):
            await self.check_notification_not_sent(notify_method, run)


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestNotifyAboutOrderModerationResultAction(BaseTestParent):
    @pytest.fixture
    def order_data(self):
        return {'user_email': 'test'}  # Order must have email after moderation

    @pytest.fixture
    async def negative_order_moderation(self, storage, merchant, order) -> Moderation:
        moderation = Moderation(
            uid=merchant.uid,
            entity_id=order.order_id,
            revision=order.revision,
            moderation_type=ModerationType.ORDER,
            approved=False,
            unixtime=100500,
            ignore=False,
        )
        return await storage.moderation.create(moderation)

    @pytest.fixture
    def params(self, negative_order_moderation):
        return {'order_moderation': negative_order_moderation}

    @pytest.fixture
    def render_context_mock(self, mocker):
        return mocker.patch.object(
            NotifyAboutOrderModerationResultAction,
            'email_render_context',
            return_value={'a': 1},
        )

    @pytest.fixture
    async def returned(self, params):
        return await NotifyAboutOrderModerationResultAction(**params).run()

    def test_email_render_context(self, params, negative_order_moderation, merchant, items, order):
        order.items = items
        action = NotifyAboutOrderModerationResultAction(**params)
        render_context = action.email_render_context(moderation=negative_order_moderation, order=order,
                                                     merchant=merchant)
        expected_render_context = {
            'merchant': {
                'company_name': merchant.organization.full_name,
                'company_short_name': merchant.organization.name,
            },
            'order': {
                'order_id': order.order_id,
                'uid': order.uid,
                'caption': order.caption,
                'description': order.description,
            },
            'items': [
                {
                    'number': number,
                    'name': item.product.name,
                    'price': decimal_format(item.price),
                    'amount': decimal_format(item.amount),
                    'total': decimal_format(item.total_price),  # type: ignore
                } for number, item in enumerate(order.items, 1) if item.product is not None
            ],
            'moderation': {
                'moderation_id': negative_order_moderation.moderation_id,
                'reasons': negative_order_moderation.reasons,
                'approved': negative_order_moderation.approved,
                'unixtime': negative_order_moderation.unixtime,
            }
        }
        assert render_context == expected_render_context

    class TestPayerNotification:
        @pytest.fixture(autouse=True)
        def tasks(self, mocker):
            return method_return_value_spy(NotifyAboutOrderModerationResultAction, 'notify_payer', mocker)

        @pytest.mark.asyncio
        async def test_payer_notification_task_created(self, returned, tasks, storage):
            task = tasks[0]
            assert task == await storage.task.get(task_id=task.task_id)

        @pytest.mark.parametrize('order_data', [{'user_email': 'guido@eggs.ni'}])
        @pytest.mark.asyncio
        async def test_payer_notification_task_parameters(self, render_context_mock, payments_settings,
                                                          returned, tasks, order, order_data,
                                                          negative_order_moderation):
            task = tasks[0]
            to_email = order.user_email
            mailing_id = payments_settings.SENDER_MAILING_ORDER_MODERATION_NEGATIVE_FOR_CUSTOMER
            render_context = render_context_mock.return_value
            assert_that(task, has_properties({
                'params': has_entries(
                    action_kwargs=has_entries(
                        to_email=to_email,
                        render_context=render_context,
                        mailing_id=mailing_id
                    )
                ),
                'action_name': 'transact_email_action',
                'task_type': TaskType.RUN_ACTION,
            }))

    class TestMerchantNotification:
        @pytest.fixture(autouse=True)
        def tasks(self, mocker):
            return method_return_value_spy(NotifyAboutOrderModerationResultAction, 'notify_merchant', mocker)

        @pytest.mark.asyncio
        async def test_merchant_notification_task_created(self, returned, tasks, storage):
            task = tasks[0]
            assert task == await storage.task.get(task_id=task.task_id)

        @pytest.mark.asyncio
        async def test_merchant_notification_task_parameters(self, payments_settings,
                                                             render_context_mock, returned, tasks, order,
                                                             merchant, negative_order_moderation):
            task = tasks[0]
            to_email = merchant.contact.email
            mailing_id = payments_settings.SENDER_MAILING_ORDER_MODERATION_NEGATIVE_FOR_MERCHANT
            render_context = render_context_mock.return_value
            assert_that(task, has_properties({
                'params': has_entries(
                    action_kwargs=has_entries(
                        to_email=to_email,
                        render_context=render_context,
                        mailing_id=mailing_id
                    )
                ),
                'action_name': 'transact_email_action',
                'task_type': TaskType.RUN_ACTION,
            }))


class TestProcessFastModerationRequestAction:
    @pytest.fixture
    def moderation_request_data(self):
        return {
            'type': 'test-type',
            'service': 'test-service',
            'meta': {
                'test-key': 'test-value'
            },
            'unixtime': 111000
        }

    @pytest.fixture
    def time(self):
        return 222

    @pytest.fixture(autouse=True)
    def mock_time(self, time, mocker):
        mocker.patch('time.time', return_value=time)

    @pytest.fixture
    def expected_written_data(self, moderation_request_data, time):
        return {
            **moderation_request_data,
            'result': {
                'verdict': 'Yes'
            },
            'unixtime': time * 1000,
        }

    @pytest.fixture
    def producer_mock(self, mocker):
        mock = mocker.Mock()
        mock.write_dict.return_value = dummy_coro()
        yield mock
        mock.write_dict.return_value.close()

    @pytest.fixture(autouse=True)
    def producer_cls_mock(self, mocker, producer_mock):
        yield mocker.patch(
            'mail.payments.payments.core.actions.worker.moderation.FastModerationResponseProducer',
            mocker.Mock(return_value=dummy_async_context_manager(producer_mock)),
        )

    @pytest.fixture
    def moderation_request(self, moderation_request_data):
        return FastModerationRequest(
            service=moderation_request_data['service'],
            type_=moderation_request_data['type'],
            meta=moderation_request_data['meta'],
            unixtime=moderation_request_data['unixtime'],
        )

    @pytest.fixture
    def params(self, lb_factory_mock, moderation_request):
        with temp_setattr(ProcessFastModerationRequestAction.context, 'lb_factory', lb_factory_mock):
            yield dict(request=moderation_request)

    @pytest.fixture
    async def returned(self, params, moderation_request):
        return await ProcessFastModerationRequestAction(**params).run()

    def test_written_data(self, returned, producer_mock, expected_written_data):
        producer_mock.write_dict.assert_called_once_with(expected_written_data)

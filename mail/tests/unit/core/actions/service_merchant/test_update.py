import pytest

from sendr_utils import alist

from hamcrest import all_of, assert_that, has_properties, is_

from mail.payments.payments.core.actions.service_merchant.update import UpdateServiceMerchantAction
from mail.payments.payments.core.entities.enums import TaskType
from mail.payments.payments.core.entities.log import ServiceMerchantUpdatedLog
from mail.payments.payments.core.exceptions import ServiceMerchantNotFoundError


@pytest.fixture
def enabled():
    return True


@pytest.fixture
def description():
    return 'updated test description'


class TestUpdateServiceMerchant:
    @pytest.fixture
    def params(self, service_merchant, enabled, description):
        return {
            'uid': service_merchant.uid,
            'service_merchant_id': service_merchant.service_merchant_id,
            'enabled': enabled,
            'description': description
        }

    @pytest.fixture
    def action(self, action_cls, params):
        return action_cls(**params)

    @pytest.fixture
    def action_cls(self):
        return UpdateServiceMerchantAction

    @pytest.fixture(autouse=True)
    def create_service_merchant_updated_task_mock(self, mocker, action):
        return mocker.spy(action, 'create_service_merchant_updated_task')

    @pytest.fixture
    async def returned(self, action):
        return await action.run()

    @pytest.fixture
    async def updated_service_merchant(self, storage, returned):
        return await storage.service_merchant.get(returned.service_merchant_id)

    def test_returned(self, returned, service_merchant, enabled, description):
        assert_that(
            returned,
            has_properties({
                'service_merchant_id': service_merchant.service_merchant_id,
                'uid': service_merchant.uid,
                'service_id': service_merchant.service_id,
                'entity_id': service_merchant.entity_id,
                'description': description,
                'enabled': enabled,
                'created': service_merchant.created,
                'deleted': service_merchant.deleted,
            })
        )
        assert returned.updated > service_merchant.updated
        assert returned.revision > service_merchant.revision

    def test_service_merchant_updated_logged(self, service, service_merchant, returned, pushers_mock):
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(ServiceMerchantUpdatedLog),
                has_properties(dict(
                    merchant_uid=service_merchant.uid,
                    service_id=service.service_id,
                    service_name=service.name,
                    service_merchant_id=service_merchant.service_merchant_id,
                    enabled=service_merchant.enabled,
                ))
            ),
        )

    def test_sm_callback_called(self, create_service_merchant_updated_task_mock, service_merchant,
                                service_with_related, returned):
        create_service_merchant_updated_task_mock.assert_called_once_with(service_merchant.service)

    @pytest.mark.asyncio
    async def test_sm_task_created(self, storage, create_service_merchant_updated_task_mock, service_merchant,
                                   service_with_related, returned):
        found_tasks = await alist(storage.task.find(TaskType.API_CALLBACK))
        assert len(found_tasks) == 1


class TestUpdateServiceMerchantNotBelongToUid:
    @pytest.fixture
    def params(self, service_merchant):
        return {
            'uid': service_merchant.uid,
            'service_merchant_id': service_merchant.service_merchant_id * 10,
            'enabled': enabled,
            'description': description
        }

    @pytest.mark.asyncio
    async def test_returns_not_found(self, params):
        with pytest.raises(ServiceMerchantNotFoundError):
            await UpdateServiceMerchantAction(**params).run()

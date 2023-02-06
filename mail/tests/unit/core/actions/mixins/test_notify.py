import pytest

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.core.actions.mixins.notify import NotifyMixin
from mail.payments.payments.utils.helpers import temp_setattr


class TestNotifyMixin:
    @pytest.fixture
    def action(self, storage):
        with temp_setattr(NotifyMixin.context, 'storage', storage):
            yield NotifyMixin()

    @pytest.fixture
    async def returned(self, action, merchant, service, service_merchant):
        await action.notify_merchant_on_sm_created_enabled(service, service_merchant, merchant)

    @pytest.mark.asyncio
    async def test_single_task(self, storage, returned):
        assert len([task async for task in storage.task.find()]) == 1

    @pytest.mark.asyncio
    async def test_task_params(self, payments_settings, storage, returned, merchant, service, service_merchant):
        task = await storage.task.find().__anext__()
        assert_that(
            task,
            has_properties({
                'action_name': 'transact_email_action',
                'params': has_entries(
                    action_kwargs=has_entries({
                        'mailing_id': payments_settings.SENDER_MAILING_SERVICE_MERCHANT_CREATED,
                        'render_context': {
                            'service': {
                                'name': service.name,
                                'service_id': service_merchant.service_id,
                            },
                            'service_merchant': {
                                'service_merchant_id': service_merchant.service_merchant_id,
                                'description': service_merchant.description,
                                'enabled': service_merchant.enabled,
                                'entity_id': service_merchant.entity_id,
                            }
                        },
                        'to_email': merchant.contact.email
                    })
                ),
            })
        )

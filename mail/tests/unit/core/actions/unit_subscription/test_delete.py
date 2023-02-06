import pytest

from mail.beagle.beagle.core.actions.smtp_cache import GenerateSMTPCacheAction
from mail.beagle.beagle.core.actions.unit_subscription.delete import DeleteUnitSubscriptionAction
from mail.beagle.beagle.core.exceptions import UnitSubscriptionNotFoundError
from mail.beagle.beagle.storage.exceptions import UnitSubscriptionNotFound
from mail.beagle.beagle.tests.unit.core.actions.base import *  # noqa


@pytest.mark.asyncio
class TestDeleteUnitSubscription(BaseTestNotFound):  # noqa
    @pytest.fixture
    def action(self, params, mocker):
        return DeleteUnitSubscriptionAction

    @pytest.fixture(autouse=True)
    def action_generate_smtp_cache(self, mock_action):
        return mock_action(GenerateSMTPCacheAction)

    @pytest.fixture
    def params(self, org, unit_subscription):
        return {
            'org_id': unit_subscription.org_id,
            'mail_list_id': unit_subscription.mail_list_id,
            'unit_id': unit_subscription.unit_id
        }

    async def test_delete(self, params, storage, returned):
        with pytest.raises(UnitSubscriptionNotFound):
            await storage.unit_subscription.get(org_id=params['org_id'],
                                                mail_list_id=params['mail_list_id'],
                                                unit_id=params['unit_id'])

    async def test_cache(self, returned, params, action_generate_smtp_cache):
        action_generate_smtp_cache.assert_called_once_with(org_id=params['org_id'], mail_list_id=params['mail_list_id'])

    async def test_subscription_not_found(self, storage, unit_subscription, params, action):
        await storage.unit_subscription.delete(unit_subscription)
        with pytest.raises(UnitSubscriptionNotFoundError):
            await action(**params).run()

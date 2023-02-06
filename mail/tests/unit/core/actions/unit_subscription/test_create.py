import pytest

from mail.beagle.beagle.core.actions.smtp_cache import GenerateSMTPCacheAction
from mail.beagle.beagle.core.actions.unit_subscription.create import CreateUnitSubscriptionAction
from mail.beagle.beagle.core.entities.enums import SubscriptionType
from mail.beagle.beagle.core.exceptions import UnitNotFoundError, UnitSubscriptionAlreadyExistsError
from mail.beagle.beagle.tests.unit.core.actions.base import *  # noqa


@pytest.mark.asyncio
class TestUnitSubscription(BaseTestNotFound):  # noqa
    @pytest.fixture
    def action(self, params, mocker):
        return CreateUnitSubscriptionAction

    @pytest.fixture(autouse=True)
    def action_generate_smtp_cache(self, mock_action):
        return mock_action(GenerateSMTPCacheAction)

    @pytest.fixture
    def params(self, org, mail_list, unit):
        return {
            'org_id': org.org_id,
            'mail_list_id': mail_list.mail_list_id,
            'unit_id': unit.unit_id,
            'subscription_type': SubscriptionType.INBOX
        }

    async def test_create(self, params, storage, returned):
        created = await storage.unit_subscription.get(org_id=params['org_id'],
                                                      mail_list_id=params['mail_list_id'],
                                                      unit_id=params['unit_id'])
        assert created == returned

    async def test_cache(self, returned, params, action_generate_smtp_cache):
        action_generate_smtp_cache.assert_called_once_with(org_id=params['org_id'], mail_list_id=params['mail_list_id'])

    async def test_unit_not_found(self, params, randn, action):
        params.update({'unit_id': randn()})
        with pytest.raises(UnitNotFoundError):
            await action(**params).run()

    async def test_subscr_already_exists(self, params, unit_subscription, action):
        params.update({'unit_id': unit_subscription.unit_id})
        with pytest.raises(UnitSubscriptionAlreadyExistsError):
            await action(**params).run()

import pytest

from mail.beagle.beagle.core.actions.user_subscription.delete import DeleteUserSubscriptionAction
from mail.beagle.beagle.core.exceptions import UserSubscriptionNotFoundError
from mail.beagle.beagle.tests.unit.core.actions.base import *  # noqa


class TestDeleteUserSubscription(BaseTestNotFound):  # noqa
    @pytest.fixture
    def action(self, params, mocker):
        return DeleteUserSubscriptionAction

    @pytest.fixture
    def params(self, org, user_subscription):
        return {
            'org_id': user_subscription.org_id,
            'mail_list_id': user_subscription.mail_list_id,
            'uid': user_subscription.uid
        }

    class TestNotFound:
        @pytest.mark.asyncio
        async def test_subscription_not_found(self, storage, user_subscription, params, action):
            await storage.user_subscription.delete(user_subscription)
            with pytest.raises(UserSubscriptionNotFoundError):
                await action(**params).run()

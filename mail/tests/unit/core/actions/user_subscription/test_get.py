import pytest

from hamcrest import assert_that, contains_inanyorder, has_properties

from mail.beagle.beagle.core.actions.user_subscription.get import GetUserSubscriptionAction
from mail.beagle.beagle.tests.unit.core.actions.base import *  # noqa


class TestGetUserSubscription(BaseTestNotFound):  # noqa
    @pytest.fixture
    def action(self, params, mocker):
        return GetUserSubscriptionAction

    @pytest.fixture
    def params(self, org, mail_list):
        return {
            'org_id': org.org_id,
            'mail_list_id': mail_list.mail_list_id
        }

    @pytest.mark.asyncio
    async def test_returned(self, user_subscriptions, returned):
        assert_that(
            returned,
            contains_inanyorder(*[
                has_properties({
                    'uid': subscription.uid,
                    'mail_list_id': subscription.mail_list_id,
                    'subscription_type': subscription.subscription_type,
                    'org_id': subscription.org_id,
                    'created': subscription.created,
                    'updated': subscription.updated
                })
                for subscription in user_subscriptions
            ])
        )

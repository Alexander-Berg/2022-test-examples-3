import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.beagle.beagle.core.entities.enums import SubscriptionType


class TestSupport:
    @pytest.fixture
    def list_uid(self, smtp_cache):
        return smtp_cache.uid

    @pytest.mark.asyncio
    async def test_support(self, app, org, smtp_cache, list_uid):
        r = await app.get('/api/v1/support', params={'list_uid': list_uid})
        data = await r.json()
        assert_that(
            data['response'], contains_inanyorder(*[
                has_entries({'uid': subscr.uid,
                            'stype': SubscriptionType.INBOX.value})
                for subscr in smtp_cache.value.subscriptions
            ])
        )

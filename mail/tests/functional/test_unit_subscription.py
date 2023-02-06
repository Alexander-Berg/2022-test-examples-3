import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.beagle.beagle.core.entities.enums import SubscriptionType
from mail.beagle.beagle.utils.helpers import date_to_str


@pytest.fixture
def url(org_id, org, mail_list_id):
    return f'/api/v1/mail_list/{org_id}/{mail_list_id}/unit_subscription'


@pytest.mark.asyncio
class TestGetUnitSubscription:
    @pytest.fixture
    async def response_json(self, app, url, unit_subscriptions):
        r = await app.get(url)
        assert r.status == 200
        return await r.json()

    async def test_returned(self, unit_subscriptions, response_json):
        assert_that(
            response_json['data'],
            contains_inanyorder(*[
                has_entries({
                    'unit_id': unit_subscription.unit_id,
                    'mail_list_id': unit_subscription.mail_list_id,
                    'subscription_type': unit_subscription.subscription_type.value,
                    'org_id': unit_subscription.org_id,
                    'created': date_to_str(unit_subscription.created),
                    'updated': date_to_str(unit_subscription.updated),
                })
                for unit_subscription in unit_subscriptions
            ])
        )

    @pytest.mark.asyncio
    class TestNotFound:
        @pytest.fixture
        def mail_list_id(self, randn):
            return randn()

        async def test_mail_list_not_found(self, app, mail_list_id, url):
            r = await app.get(url)
            assert r.status == 404


@pytest.mark.asyncio
class TestPostUnitSubscription:
    @pytest.fixture
    def params(self, unit):
        return {'unit_id': unit.unit_id,
                'subscription_type': SubscriptionType.INBOX.value}

    async def test_success(self, app, params, url):
        r = await app.post(url, json=params)
        assert r.status == 200

    class TestNotFound:
        class TestMailListNotFound:
            @pytest.fixture
            def mail_list_id(self, randn):
                return randn()

            @pytest.mark.asyncio
            async def test_mail_list_not_found(self, app, mail_list_id, url, params, randn):
                r = await app.post(url, json=params)
                assert r.status == 404

        @pytest.mark.asyncio
        async def test_unit_not_found(self, app, url, params, randn):
            rand_unit_id = randn()
            params.update({'unit_id': rand_unit_id})
            r = await app.post(url, json=params)
            assert r.status == 404

    @pytest.mark.asyncio
    class TestAlreadyExists:
        @pytest.fixture
        def params(self, unit_subscription):
            return {'unit_id': unit_subscription.unit_id,
                    'subscription_type': SubscriptionType.INBOX.value}

        @pytest.fixture
        def mail_list_id(self, unit_subscription):
            return unit_subscription.mail_list_id

        async def test_already_exists(self, app, mail_list_id, params, url):
            r = await app.post(url, json=params)
            assert r.status == 409


@pytest.mark.asyncio
class TestDeleteUnitSubscription:
    @pytest.fixture
    def params(self, unit_subscription):
        return {'unit_id': unit_subscription.unit_id}

    @pytest.fixture
    def mail_list_id(self, unit_subscription):
        return unit_subscription.mail_list_id

    async def test_success(self, app, params, unit_subscription, url):
        r = await app.delete(url, json=params)
        assert r.status == 200

    class TestNotFound:
        class TestMailListNotFound:
            @pytest.fixture
            def mail_list_id(self, randn):
                return randn()

            @pytest.mark.asyncio
            async def test_mail_list_not_found(self, app, org, params, mail_list_id, url):
                r = await app.delete(url, json=params)
                assert r.status == 404

        @pytest.mark.asyncio
        async def test_unit_subscription_not_found(self, app, storage, unit_subscription, url, params, randn):
            await storage.unit_subscription.delete(unit_subscription)
            r = await app.delete(url, json=params)
            assert r.status == 404

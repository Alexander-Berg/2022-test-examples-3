import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.beagle.beagle.core.entities.enums import SubscriptionType
from mail.beagle.beagle.interactions.blackbox import UserInfo
from mail.beagle.beagle.utils.helpers import date_to_str


@pytest.fixture
def info(user):
    return [UserInfo(uid=user.uid, default_email="qq@yandex.ru")]


@pytest.fixture(autouse=True)
def blackbox_mock(blackbox_mocker, info):
    with blackbox_mocker('userinfo_by_uids', info) as mock:
        yield mock


@pytest.fixture
def url(org_id, org, mail_list_id):
    return f'/api/v1/mail_list/{org_id}/{mail_list_id}/user_subscription'


class TestGetUserSubscription:
    @pytest.fixture
    async def response_json(self, app, url, user_subscriptions):
        r = await app.get(url)
        assert r.status == 200
        return await r.json()

    @pytest.mark.asyncio
    async def test_returned(self, user_subscriptions, response_json):
        assert_that(
            response_json['data'],
            contains_inanyorder(*[
                has_entries({
                    'uid': subscription.uid,
                    'mail_list_id': subscription.mail_list_id,
                    'subscription_type': subscription.subscription_type.value,
                    'org_id': subscription.org_id,
                    'created': date_to_str(subscription.created),
                    'updated': date_to_str(subscription.updated),
                })
                for subscription in user_subscriptions
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


class TestPostUserSubscription:
    @pytest.fixture
    def params(self, user):
        return {'uid': user.uid,
                'subscription_type': SubscriptionType.INBOX.value}

    @pytest.mark.asyncio
    async def test_success(self, app, params, url):
        r = await app.post(url, json=params)
        assert r.status == 200

    class TestNotFound:
        class TestMailNotFound:
            @pytest.fixture
            def mail_list_id(self, randn):
                return randn()

            @pytest.mark.asyncio
            async def test_mail_list_not_found(self, app, org, params, mail_list_id, url):
                r = await app.post(url, json=params)
                assert r.status == 404

        @pytest.mark.asyncio
        async def test_user_not_found(self, app, url, params, randn):
            rand_uid = randn()
            params.update({'uid': rand_uid})
            r = await app.post(url, json=params)
            assert r.status == 404

        @pytest.mark.asyncio
        class TestAlreadyExists:
            @pytest.fixture
            def params(self, user_subscription):
                return {'uid': user_subscription.uid,
                        'subscription_type': SubscriptionType.INBOX.value}

            @pytest.fixture
            def mail_list_id(self, user_subscription):
                return user_subscription.mail_list_id

            async def test_already_exists(self, app, params, url):
                r = await app.post(url, json=params)
                assert r.status == 409


class TestDeleteUserSubscription:
    @pytest.fixture
    def params(self, user_subscription):
        return {'uid': user_subscription.uid}

    @pytest.fixture
    def mail_list_id(self, user_subscription):
        return user_subscription.mail_list_id

    @pytest.mark.asyncio
    async def test_success(self, app, params, user_subscription, url):
        r = await app.delete(url, json=params)
        assert r.status == 200

    class TestNotFound:
        class TestMailNotFound:
            @pytest.fixture
            def mail_list_id(self, randn):
                return randn()

            @pytest.mark.asyncio
            async def test_mail_list_not_found(self, app, mail_list_id, params, url):
                r = await app.delete(url, json=params)
                assert r.status == 404

        @pytest.mark.asyncio
        async def test_subscription_not_found(self, app, storage, user_subscription, params, url):
            await storage.user_subscription.delete(user_subscription)
            r = await app.delete(url, json=params)
            assert r.status == 404

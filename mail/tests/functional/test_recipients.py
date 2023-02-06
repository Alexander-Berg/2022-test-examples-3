import pytest

from sendr_utils import UserType

from hamcrest import assert_that, contains, has_entries

from mail.beagle.beagle.interactions.blackbox import BlackBoxClient, UserInfo


class TestRecipients:
    @pytest.fixture
    def mocked_user_type(self, mocker):
        mock = mocker.patch('mail.beagle.beagle.core.actions.recipients.get_user_type', return_value=UserType.CONNECT)
        return mock

    @pytest.fixture
    def user_uid_from(self, user):
        return user.uid

    @pytest.fixture
    def email_from(self, randmail):
        return randmail()

    @pytest.fixture
    def user_info_from(self, user_uid_from, email_from):
        return UserInfo(uid=user_uid_from, default_email=email_from)

    @pytest.fixture
    def user_info_to(self, mail_list, master_email_to):
        return UserInfo(uid=mail_list.uid, default_email=master_email_to)

    @pytest.fixture
    def userinfo_by_login(self, email_from, master_email_to, alias_email_to, user_info_from, user_info_to):
        async def _inner(login):
            if login == email_from:
                result = user_info_from
            elif login in [master_email_to, alias_email_to]:
                result = user_info_to
            else:
                result = Exception

            if isinstance(result, type) and issubclass(result, Exception):
                raise result
            else:
                return result

        return _inner

    @pytest.fixture(autouse=True)
    def setup(self, userinfo_by_login, mocker):
        mocker.patch.object(BlackBoxClient, 'userinfo_by_login', mocker.Mock(side_effect=userinfo_by_login))

    @pytest.fixture
    def expected_cache_subscriptions(self, cache_response):
        return [
            {'uid': subscription.uid, 'email': subscription.email.lower()}
            for subscription in cache_response.subscriptions
        ]

    @pytest.mark.asyncio
    async def test_recipients(self, mocked_user_type, app, org, smtp_cache, smtp_cache_value, user, email_from,
                              master_email_to, expected_cache_subscriptions):
        r = await app.get('/api/v1/recipients', params={'email_from': email_from, 'email_to': master_email_to})
        data = await r.json()

        assert_that(
            (r.status, data),
            contains(200, has_entries({'status': 'ok', 'response': {
                'subscriptions': expected_cache_subscriptions
            }}))
        )

    @pytest.mark.asyncio
    async def test_deleted_mail_list(self, app, storage, org, mail_list, smtp_cache, smtp_cache_value, user, email_from,
                                     master_email_to):
        await storage.mail_list.delete(mail_list)
        r = await app.get('/api/v1/recipients', params={'email_from': email_from, 'email_to': master_email_to})
        assert r.status == 404

    @pytest.mark.asyncio
    class TestStaff:
        @pytest.fixture
        def mocked_user_type(self, mocker):
            mock = mocker.patch('mail.beagle.beagle.core.actions.recipients.get_user_type', return_value=UserType.STAFF)
            return mock

        @pytest.fixture
        def master_domain(self):
            return 'yandex-team.ru'

        @pytest.mark.asyncio
        async def test_staff_recipients(self, mocked_user_type, app, org, smtp_cache,
                                        smtp_cache_value, user, email_from, master_email_to,
                                        expected_cache_subscriptions):
            r = await app.get('/api/v1/recipients', params={'email_from': email_from, 'email_to': master_email_to})
            data = await r.json()

            assert_that(
                (r.status, data),
                contains(200, has_entries({'status': 'ok', 'response': {
                    'subscriptions': expected_cache_subscriptions
                }}))
            )

    @pytest.mark.asyncio
    class TestPortal:
        @pytest.fixture
        def mocked_user_type(self, mocker):
            mock = mocker.patch('mail.beagle.beagle.core.actions.recipients.get_user_type',
                                return_value=UserType.PORTAL)
            return mock

        @pytest.fixture
        def master_domain(self):
            return 'yandex.ru'

        @pytest.mark.asyncio
        async def test_portal_recipients(self, mocked_user_type, app, org, smtp_cache, smtp_cache_value, user,
                                         email_from, master_email_to, expected_cache_subscriptions):
            r = await app.get('/api/v1/recipients', params={'email_from': email_from, 'email_to': master_email_to})
            data = await r.json()

            assert_that(
                (r.status, data),
                contains(200, has_entries({'status': 'ok', 'response': {
                    'subscriptions': expected_cache_subscriptions
                }}))
            )

    @pytest.mark.asyncio
    class TestAlias:
        @pytest.fixture
        def mocked_use_default(self, mocker, use_default):
            mock = mocker.patch('mail.beagle.beagle.conf.settings.USE_DEFAULT_DOMAIN', use_default)
            return mock

        class TestUseDefaultOn:
            @pytest.fixture
            def use_default(self):
                return True

            @pytest.mark.asyncio
            async def test_master_recipients(self, mocked_user_type, app, mocked_use_default, org, smtp_cache,
                                             smtp_cache_value, user, email_from, alias_email_to,
                                             expected_cache_subscriptions):
                r = await app.get('/api/v1/recipients', params={'email_from': email_from, 'email_to': alias_email_to})
                data = await r.json()

                assert_that(
                    (r.status, data),
                    contains(200, has_entries({'status': 'ok', 'response': {
                        'subscriptions': expected_cache_subscriptions
                    }}))
                )

        class TestUseDefaultOff:
            @pytest.fixture
            def use_default(self):
                return False

            @pytest.fixture
            def expected_cache_subscriptions(self, cache_response, mail_list, alias_email_to):
                subscriptions = [
                    {'uid': subscription.uid, 'email': subscription.email.lower()}
                    for subscription in cache_response.subscriptions if subscription.uid != mail_list.uid
                ]
                subscriptions.append({'uid': mail_list.uid, 'email': alias_email_to.lower()})
                return subscriptions

            @pytest.mark.asyncio
            async def test_alias_recipients(self, app, mocked_user_type, mocked_use_default, org, smtp_cache,
                                            mail_list, smtp_cache_value, user, email_from, alias_email_to,
                                            expected_cache_subscriptions):
                r = await app.get('/api/v1/recipients', params={'email_from': email_from, 'email_to': alias_email_to})
                data = await r.json()

                assert_that(
                    (r.status, data),
                    contains(200, has_entries({'status': 'ok', 'response': {
                        'subscriptions': expected_cache_subscriptions
                    }}))
                )

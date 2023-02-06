import pytest

from sendr_utils import UserType

from mail.beagle.beagle.core.actions.recipients import RecipientsAction
from mail.beagle.beagle.core.entities.smtp_cache import Recipient
from mail.beagle.beagle.core.exceptions import MailListNotFoundError
from mail.beagle.beagle.interactions import BlackBoxClient
from mail.beagle.beagle.interactions.blackbox import BlackBoxUserNotFound, UserInfo
from mail.beagle.beagle.storage.mappers.mail_list import MailListMapper


@pytest.fixture
def mocked_user_type(mocker):
    mock = mocker.patch('mail.beagle.beagle.core.actions.recipients.get_user_type', return_value=UserType.CONNECT)
    return mock


@pytest.fixture
def user_info_to(mail_list, master_email_to):
    return UserInfo(uid=mail_list.uid, default_email=master_email_to)


@pytest.mark.asyncio
class TestRecipientsAction:
    @pytest.fixture
    def email_from(self, randmail):
        return randmail()

    @pytest.fixture
    def user_uid_from(self, user):
        return user.uid

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
        mocker.spy(MailListMapper, 'find')
        mocker.patch.object(BlackBoxClient, 'userinfo_by_login', mocker.Mock(side_effect=userinfo_by_login))

    @pytest.fixture
    def returned_func(self, mocked_user_type, email_from, master_email_to):
        async def _inner():
            return await RecipientsAction(email_from=email_from, email_to=master_email_to).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_result(self, smtp_cache, cache_response, returned):
        assert cache_response == returned

    @pytest.mark.parametrize('user_info_to', (BlackBoxUserNotFound,))
    async def test_to_not_found(self, returned_func):
        with pytest.raises(MailListNotFoundError):
            await returned_func()

    async def test_cache_not_found(self, returned_func):
        with pytest.raises(MailListNotFoundError):
            await returned_func()

    async def test_deleted_mail_list(self, storage, mail_list, returned_func):
        mail_list = await storage.mail_list.delete(mail_list)
        with pytest.raises(MailListNotFoundError):
            await returned_func()

    @pytest.mark.asyncio
    class TestStaffUsers:
        @pytest.fixture
        def mocked_user_type(self, mocker):
            mock = mocker.patch('mail.beagle.beagle.core.actions.recipients.get_user_type', return_value=UserType.STAFF)
            return mock

        @pytest.fixture
        def master_domain(self):
            return 'yandex-team.ru'

        @pytest.fixture
        def returned_func(self, mocked_user_type, email_from, master_email_to):
            async def _inner():
                return await RecipientsAction(email_from=email_from, email_to=master_email_to).run()

            return _inner

        async def test_staff_result(self, smtp_cache, cache_response, returned):
            assert cache_response == returned

    @pytest.mark.asyncio
    class TestPortalUsers:
        @pytest.fixture
        def mocked_user_type(self, mocker):
            mock = mocker.patch('mail.beagle.beagle.core.actions.recipients.get_user_type',
                                return_value=UserType.PORTAL)
            return mock

        @pytest.fixture
        def master_domain(self):
            return 'yandex.ru'

        @pytest.fixture
        def returned_func(self, mocked_user_type, email_from, master_email_to):
            async def _inner():
                return await RecipientsAction(email_from=email_from, email_to=master_email_to).run()

            return _inner

        async def test_portal_result(self, smtp_cache, cache_response, returned):
            assert cache_response == returned

    @pytest.mark.asyncio
    class TestAliasDomain:
        @pytest.fixture
        def mocked_use_default(self, mocker, use_default):
            mock = mocker.patch('mail.beagle.beagle.conf.settings.USE_DEFAULT_DOMAIN', use_default)
            return mock

        @pytest.fixture
        def returned_func(self, mocked_use_default, mocked_user_type, email_from, alias_email_to):
            async def _inner():
                return await RecipientsAction(email_from=email_from, email_to=alias_email_to).run()

            return _inner

        class TestUseDefaultOn:
            @pytest.fixture
            def use_default(self):
                return True

            @pytest.mark.asyncio
            async def test_master_result(self, smtp_cache, cache_response, mail_list, alias_email_to, returned):
                assert cache_response == returned

        class TestUseDefaultOff:
            @pytest.fixture
            def use_default(self):
                return False

            @pytest.fixture
            def expected_cache_subscriptions(self, cache_response, mail_list, alias_email_to):
                subscriptions = [
                    Recipient(uid=subscription.uid, email=subscription.email)
                    for subscription in cache_response.subscriptions if subscription.uid != mail_list.uid
                ]
                subscriptions.append(Recipient(uid=mail_list.uid, email=alias_email_to))
                return subscriptions

            @pytest.mark.asyncio
            async def test_alias_result(self, smtp_cache, expected_cache_subscriptions, mail_list, alias_email_to,
                                        returned):
                assert expected_cache_subscriptions == returned.subscriptions

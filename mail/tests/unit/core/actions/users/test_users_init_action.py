from datetime import date

import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_properties

from mail.ipa.ipa.core.actions.collectors.inherit import InheritUserCollectorsAction
from mail.ipa.ipa.core.actions.users.init import InitUserAction
from mail.ipa.ipa.core.actions.users.remove_collectors import RemoveUserCollectorsAction
from mail.ipa.ipa.core.entities.password import Password
from mail.ipa.ipa.core.entities.user_info import UserInfo
from mail.ipa.ipa.core.exceptions import (
    InvalidBirthdayError, NotConnectUIDError, PasswordInvalidError, UnsupportedGenderError, UnsupportedLanguageError
)
from mail.ipa.ipa.interactions.blackbox import BlackboxClient
from mail.ipa.ipa.interactions.blackbox.exceptions import BlackboxBaseError
from mail.ipa.ipa.interactions.directory import DirectoryClient
from mail.ipa.ipa.interactions.directory.entities import PasswordMode
from mail.ipa.ipa.interactions.directory.exceptions import DirectoryBaseError, DirectoryNotFoundError
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestUserInitAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return InitUserAction

    @pytest.fixture
    def params(self, general_init_import_params, user, user_info):
        return {
            'user_id': user.user_id,
            'user_info': user_info,
            'admin_uid': general_init_import_params.admin_uid,
            'user_ip': general_init_import_params.user_ip,
        }

    @pytest.fixture(autouse=True)
    def mock_blackbox(self, mocker, coromock, suid):
        return mocker.patch.object(BlackboxClient, 'get_suid', coromock(suid))

    @pytest.fixture(autouse=True)
    def mock_directory_get_user(self, mocker, coromock, dir_user):
        return mocker.patch.object(DirectoryClient, 'get_user_by_login', coromock(dir_user))

    @pytest.fixture(autouse=True)
    def mock_directory_create_user(self, mocker, coromock, dir_user):
        return mocker.patch.object(DirectoryClient, 'create_user', coromock(dir_user))

    @pytest.fixture(autouse=True)
    def mock_inherit_collectors(self, mock_action):
        return mock_action(InheritUserCollectorsAction)

    @pytest.fixture(autouse=True)
    def mock_remove_user_collectors(self, mock_action):
        return mock_action(RemoveUserCollectorsAction)

    class TestNewUser:
        @pytest.fixture(autouse=True)
        def mock_password(self, mocker, ipa_settings):
            mocker.patch.object(Password, 'md5crypt', mocker.Mock(return_value='hashhash'))
            return ipa_settings

        @pytest.fixture(autouse=True)
        def mock_directory_get_user(self, mocker, coromock):
            return mocker.patch.object(DirectoryClient,
                                       'get_user_by_login',
                                       coromock(exc=DirectoryNotFoundError(404,
                                                                           code='not_found',
                                                                           service=None,
                                                                           method=None,
                                                                           )
                                                )
                                       )

        def test_new_user__returned(self, returned, uid, dst_email):
            _, dir_user = returned
            assert_that(dir_user.uid, equal_to(uid))
            assert_that(dir_user.email, equal_to(dst_email))

        def test_new_user__directory_get_user_called(self, returned, mock_directory_get_user, user):
            mock_directory_get_user.assert_called_once_with(user.org_id, user.login)

        def test_new_user__blackbox_called(self, returned, mock_blackbox, uid):
            mock_blackbox.assert_called_once_with(uid)

        def test_new_user__directory_create_user_called(self,
                                                        returned,
                                                        mock_directory_create_user,
                                                        user,
                                                        password,
                                                        admin_uid,
                                                        user_ip):
            mock_directory_create_user.assert_called_once_with(org_id=user.org_id,
                                                               login=user.login,
                                                               password=password.md5crypt(),
                                                               password_mode=PasswordMode.HASH,
                                                               admin_uid=admin_uid,
                                                               user_ip=user_ip,
                                                               )

        def test_new_user__remove_collectors_called(self, returned, mock_remove_user_collectors):
            mock_remove_user_collectors.assert_called_once()

        def test_new_user__inherit_collectors_called(self, returned, mock_inherit_collectors):
            mock_inherit_collectors.assert_called_once()

        @pytest.mark.asyncio
        async def test_new_user__user_updated(self, returned, storage, uid, suid, user):
            updated_user = await storage.user.get(user.user_id)
            assert_that(
                updated_user,
                has_properties({
                    'uid': uid,
                    'suid': suid,
                }),
            )

    class TestUserAlreadyExistsInDirectory:
        def test_user_exists__returned(self, returned, uid, dst_email):
            _, dir_user = returned
            assert_that(dir_user.uid, equal_to(uid))
            assert_that(dir_user.email, equal_to(dst_email))

        def test_user_exists__directory_get_user_called(self, returned, mock_directory_get_user, user):
            mock_directory_get_user.assert_called_once_with(user.org_id, user.login)

        def test_user_exists__blackbox_called(self, returned, mock_blackbox, uid):
            mock_blackbox.assert_called_once_with(uid)

        def test_user_exists__directory_create_user_not_called(self, returned, mock_directory_create_user):
            mock_directory_create_user.assert_not_called()

        def test_user_exists__remove_collectors_called(self, returned, mock_remove_user_collectors):
            mock_remove_user_collectors.assert_called_once()

        def test_user_exists__inherit_collectors_called(self, returned, mock_inherit_collectors):
            mock_inherit_collectors.assert_called_once()

        @pytest.mark.asyncio
        async def test_user_exists__user_updated(self, returned, storage, uid, suid, user):
            updated_user = await storage.user.get(user.user_id)
            assert_that(
                updated_user,
                has_properties({
                    'uid': uid,
                    'suid': suid,
                }),
            )

        class TestUserIsNotPDD:
            @pytest.fixture
            def uid(self):
                return 123

            @pytest.mark.asyncio
            async def test_user_exists__not_pdd_user_raises_exception(self, action_coro, uid, storage):
                with pytest.raises(NotConnectUIDError) as exc_info:
                    await action_coro
                assert_that(exc_info.value.params, equal_to({'uid': uid}))

    class TestUserExistsAndNotChanged:
        @pytest.fixture
        def user(self, existing_user):
            return existing_user

        def test_user_not_changed__returned(self, returned, uid, dst_email):
            _, dir_user = returned
            assert_that(dir_user.uid, equal_to(uid))
            assert_that(dir_user.email, equal_to(dst_email))

        def test_user_not_changed__directory_get_user_called(self, returned, mock_directory_get_user, user):
            mock_directory_get_user.assert_called_once_with(user.org_id, user.login)

        def test_user_not_changed__blackbox_not_called(self, returned, mock_blackbox):
            mock_blackbox.assert_not_called()

        def test_user_not_changed__directory_create_user_not_called(self, returned, mock_directory_create_user):
            mock_directory_create_user.assert_not_called()

        def test_user_not_changed__remove_collectors_not_called(self, returned, mock_remove_user_collectors):
            mock_remove_user_collectors.assert_not_called()

        def test_user_not_changed__inherit_collectors_called(self, returned, mock_inherit_collectors):
            mock_inherit_collectors.assert_called_once()

        class TestUserIsNotPDD:
            @pytest.fixture
            def uid(self):
                return 123

            @pytest.mark.asyncio
            async def test_user_not_changed__not_pdd_user_raises_exception(self, action_coro, uid, storage):
                with pytest.raises(NotConnectUIDError) as exc_info:
                    await action_coro
                assert_that(exc_info.value.params, equal_to({'uid': uid}))

    @pytest.mark.parametrize(
        'error, code',
        (
            (BlackboxBaseError(status=400, code='bbcode', service='bb', method='get'), 'bbcode'),
            (DirectoryBaseError(status=400, code='dircode', service='directory', method='get'), 'dircode'),
            (NotConnectUIDError(1515), NotConnectUIDError.message),
        )
    )
    class TestException:
        @pytest.fixture(autouse=True)
        def mock_directory_get_user(self, mocker, coromock, error):
            return mocker.patch.object(DirectoryClient,
                                       'get_user_by_login',
                                       coromock(exc=error))

        @pytest.mark.asyncio
        async def test_error_persisted(self, exc_info, storage, code):
            users = await alist(storage.user.find())
            assert_that(users[0].error, equal_to(code))

        @pytest.mark.asyncio
        async def test_remove_collectors_should_be_called(self, exc_info, mock_remove_user_collectors, user, code):
            mock_remove_user_collectors.assert_called_once_with(user.user_id)

    class TestGetUserInfo:
        class TestSuccess:
            @pytest.fixture(autouse=True)
            def mock_format(self, ipa_settings):
                ipa_settings.USER_BIRTHDAY_FORMATS = ('%d.%m.%Y', '%Y/%m/%d')

            @pytest.fixture
            def user_info(self, info, rands):
                password = Password.from_plain(info.pop('password', '123'))
                new_password = Password.from_plain(info.pop('new_password')) if 'new_password' in info else None
                return UserInfo(login=rands(),
                                src_login=rands(),
                                password=password,
                                new_password=new_password,
                                **info)

            @pytest.fixture(autouse=True)
            def mock_salt(self, mocker):
                import crypt
                return mocker.patch(
                    'mail.ipa.ipa.core.entities.password.crypt.mksalt',
                    mocker.Mock(return_value=crypt.mksalt(method=crypt.METHOD_MD5)),
                )

            @pytest.fixture
            def expected_data(self, expected):
                expected['password'] = Password.from_plain(expected['password']).md5crypt()
                if 'new_password' in expected and expected['new_password']:
                    expected['new_password'] = Password.from_plain(expected['new_password']).md5crypt()
                expected['password_mode'] = PasswordMode.HASH
                return expected

            @pytest.mark.parametrize('info, expected', (
                (
                    {'password': '123'},
                    {'password_mode': PasswordMode.HASH, 'password': '123'}
                ),
                (
                    {'password': '123', 'new_password': '456'},
                    {'password_mode': PasswordMode.HASH, 'password': '456'}
                ),
                (
                    {'password': '123', 'gender': 'male'},
                    {'password': '123', 'gender': 'male'},
                ),
                (
                    {'password': '123', 'language': 'ru'},
                    {'password': '123', 'language': 'ru'}
                ),
                (
                    {'password': '123', 'first_name': 'Alex', 'last_name': 'Whatever', 'middle_name': None},
                    {'password': '123', 'first_name': 'Alex', 'last_name': 'Whatever'},
                ),
                (
                    {'password': '123', 'birthday': '01.01.1990'},
                    {'password': '123', 'birthday': date(1990, 1, 1)}
                ),
                (
                    {'password': '123', 'birthday': '1990/01/01'},
                    {'password': '123', 'birthday': date(1990, 1, 1)}
                ),
            ))
            def test_get_user_info_result(self, action, expected_data):
                assert_that(action._get_connect_user_params(), equal_to(expected_data))

        class TestException:
            def test_raises_when_bday_invalid(self, action):
                with pytest.raises(InvalidBirthdayError):
                    action.info.birthday = '01/01/1990'
                    action._get_connect_user_params()

            def test_raises_when_gender_invalid(self, action):
                with pytest.raises(UnsupportedGenderError):
                    action.info.gender = 'not-supported'
                    action._get_connect_user_params()

            def test_raises_when_language_invalid(self, action):
                with pytest.raises(UnsupportedLanguageError):
                    action.info.language = 'languagelanguage'
                    action._get_connect_user_params()

            @pytest.mark.parametrize('password', ('123\b456', 'привет, мир!'))
            def test_raises_when_password_invalid(self, action, password):
                with pytest.raises(PasswordInvalidError):
                    action.info.password = Password.from_plain(password)
                    action._get_connect_user_params()

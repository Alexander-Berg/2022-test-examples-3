import pytest

from mail.ipa.ipa.core.actions.collectors.init import InitCollectorAction
from mail.ipa.ipa.core.actions.import_.user import InitUserImportAction
from mail.ipa.ipa.core.actions.users.init import InitUserAction
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestImportUserAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return InitUserImportAction

    @pytest.fixture
    def params(self, general_init_import_params, user_info, user):
        return {
            'import_params': general_init_import_params,
            'user_info': user_info,
            'user_id': user.user_id,
        }

    @pytest.fixture(autouse=True)
    def mock_init_user_action(self, mock_action, user, dir_user):
        return mock_action(InitUserAction, action_result=(user, dir_user))

    @pytest.fixture(autouse=True)
    def mock_init_collector_action(self, mock_action, user, dir_user):
        return mock_action(InitCollectorAction)

    def test_init_user_action_called(self, mock_init_user_action, params, returned):
        mock_init_user_action.assert_called_once_with(
            user_id=params['user_id'],
            user_info=params['user_info'],
            user_ip=params['import_params'].user_ip,
            admin_uid=params['import_params'].admin_uid,
        )

    def test_init_collector_action_called(self, mock_init_collector_action, params, user, dir_user, returned):
        mock_init_collector_action.assert_called_once_with(
            user=user,
            directory_user=dir_user,
            src_password=params['user_info'].password,
            import_params=params['import_params'],
            src_login=params['user_info'].src_login,
        )

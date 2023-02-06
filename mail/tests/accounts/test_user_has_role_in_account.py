import pytest
from fan.accounts.get import user_has_role_in_account


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm):
    pass


def test_user_has_role(user_id, account):
    assert user_has_role_in_account(user_id, account)


def test_user_does_not_have_role(foreign_user_id, account):
    assert not user_has_role_in_account(foreign_user_id, account)


def test_confirmed_on_recheck(user_id_to_recheck, account, mock_directory_user):
    mock_directory_user.users[user_id_to_recheck] = "user"
    assert user_has_role_in_account(user_id_to_recheck, account)


def test_revoked_on_recheck(user_id_to_recheck, account, mock_directory_user):
    assert not user_has_role_in_account(user_id_to_recheck, account)


def test_recheck_if_never_checked(user_id_never_checked, account, mock_directory_user):
    assert not user_has_role_in_account(user_id_never_checked, account)


def test_too_soon_to_recheck(user_id_too_soon_to_recheck, account, mock_directory_user):
    assert user_has_role_in_account(user_id_too_soon_to_recheck, account)


def test_ignores_directory_availability(user_id_never_checked, account, mock_directory_user):
    mock_directory_user.resp_code = 500
    mock_directory_user.resp_json = ""
    assert user_has_role_in_account(user_id_never_checked, account)

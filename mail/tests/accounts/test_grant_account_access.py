import pytest
from django.conf import settings
from fan.accounts.set import grant_account_access, AccountUsersLimitExceeded
from fan.models import Account, UserRole


pytestmark = pytest.mark.django_db


@pytest.fixture
def user_id_list_exceeding_limit():
    return ["user_id_{}".format(i) for i in range(settings.ACCOUNT_USERS_LIMIT + 1)]


@pytest.fixture
def new_account(org_id):
    return Account.objects.create(name="acc", org_id=org_id)


def test_no_access_to_new_account(new_account, user_ids):
    for user_id in user_ids:
        assert not user_has_role_in_account(user_id, new_account), user_id


def test_grant_access(new_account, user_ids):
    grant_account_access(new_account, user_ids)
    for user_id in user_ids:
        assert user_has_role_in_account(user_id, new_account), user_id


def test_grant_access_twice(new_account, user_ids):
    grant_account_access(new_account, user_ids)
    grant_account_access(new_account, user_ids)
    for user_id in user_ids:
        assert user_has_role_in_account(user_id, new_account), user_id


def test_too_long_user_list(new_account, user_id_list_exceeding_limit):
    with pytest.raises(AccountUsersLimitExceeded):
        grant_account_access(new_account, user_id_list_exceeding_limit)
    for user_id in user_id_list_exceeding_limit:
        assert not user_has_role_in_account(user_id, new_account), user_id


def test_too_long_user_list_by_parts(new_account, user_id_list_exceeding_limit):
    first_half_users, second_half_users = split_to_halfs(user_id_list_exceeding_limit)
    grant_account_access(new_account, first_half_users)
    with pytest.raises(AccountUsersLimitExceeded):
        grant_account_access(new_account, second_half_users)
    for user_id in first_half_users:
        assert user_has_role_in_account(user_id, new_account), user_id
    for user_id in second_half_users:
        assert not user_has_role_in_account(user_id, new_account), user_id


def user_has_role_in_account(user_id, account):
    return UserRole.objects.filter(user_id=user_id, account=account).exists()


def split_to_halfs(lst):
    n = len(lst)
    return lst[: n // 2], lst[n // 2 :]

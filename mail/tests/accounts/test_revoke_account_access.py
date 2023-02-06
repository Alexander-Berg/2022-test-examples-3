import pytest
from fan.accounts.set import revoke_account_access
from fan.models import UserRole


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def users_has_role(account, user_ids):
    for user_id in user_ids:
        UserRole.objects.get_or_create(user_id=user_id, account=account, role=UserRole.ROLES.USER)


def test_revoke_access(account, user_ids):
    for user_id in user_ids:
        assert user_has_role_in_account(user_id, account), user_id
    revoke_account_access(account, user_ids)
    for user_id in user_ids:
        assert not user_has_role_in_account(user_id, account), user_id


def test_revoke_access_twice(account, user_ids):
    revoke_account_access(account, user_ids)
    revoke_account_access(account, user_ids)
    for user_id in user_ids:
        assert not user_has_role_in_account(user_id, account), user_id


def user_has_role_in_account(user_id, account):
    return UserRole.objects.filter(user_id=user_id, account=account).exists()

import pytest
from django.utils.timezone import now as utcnow
from fan.accounts.organizations.users import user_has_role_in_org_accounts
from fan.models import UserRole, Account


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm):
    pass


@pytest.fixture
def user_is_org_member(mock_directory_user, user_id):
    mock_directory_user.users[user_id] = "user"


@pytest.fixture
def another_account_with_user_role(user_id, org_id):
    new_account = Account.objects.create(name="acc1", org_id=org_id)
    UserRole.objects.create(user_id=user_id, account=new_account, role=UserRole.ROLES.USER)
    return new_account


def test_user_has_role_in_org_account(user_id, account):
    assert user_has_role_in_org_accounts(user_id, account.org_id)


def test_user_does_not_have_any_roles(foreign_user_id, account, mock_directory_user):
    assert not user_has_role_in_org_accounts(foreign_user_id, account.org_id)


def test_confirmed_on_recheck(user_id_to_recheck, account, user_is_org_member):
    assert user_has_role_in_org_accounts(user_id_to_recheck, account.org_id)


def test_update_roles(
    user_id_to_recheck, account, another_account_with_user_role, user_is_org_member
):
    old_time = UserRole.objects.get(user_id=user_id_to_recheck, account=account).checked_at
    user_has_role_in_org_accounts(user_id_to_recheck, account.org_id)
    roles = UserRole.objects.filter(user_id=user_id_to_recheck)
    assert all(role.checked_at > old_time for role in roles)


def test_recheck_if_never_checked(user_id_never_checked, account, user_is_org_member):
    user_has_role_in_org_accounts(user_id_never_checked, account.org_id)
    checked_at = UserRole.objects.get(user_id=user_id_never_checked).checked_at
    assert checked_at is not None


def test_too_soon_to_recheck(user_id_too_soon_to_recheck, account):
    old_time = UserRole.objects.get(user_id=user_id_too_soon_to_recheck).checked_at
    user_has_role_in_org_accounts(user_id_too_soon_to_recheck, account.org_id)
    new_time = UserRole.objects.get(user_id=user_id_too_soon_to_recheck).checked_at
    assert new_time == old_time


def test_revoked_on_recheck_and_delete_user_roles(
    user_id_to_recheck, account, another_account_with_user_role, mock_directory_user
):
    assert not user_has_role_in_org_accounts(user_id_to_recheck, account.org_id)
    assert not UserRole.objects.filter(user_id=user_id_to_recheck).exists()


def test_dont_update_old_roles_if_up_to_date_exists(
    user_id_to_recheck, account, another_account_with_user_role
):
    UserRole.objects.filter(account=another_account_with_user_role).update(checked_at=utcnow())
    old_time = UserRole.objects.get(user_id=user_id_to_recheck, account=account).checked_at
    user_has_role_in_org_accounts(user_id_to_recheck, account.org_id)
    new_time = UserRole.objects.get(user_id=user_id_to_recheck, account=account).checked_at
    assert new_time == old_time

import pytest
from fan.accounts.get import get_accounts_by_org_id, get_account_users
from fan.models import Account
from fan.testutils.matchers import assert_lists_are_matching

pytestmark = pytest.mark.django_db


@pytest.fixture
def accounts(org_id):
    return [
        Account.objects.create(name="acc1", org_id=org_id),
        Account.objects.create(name="acc2", org_id="another_org"),
        Account.objects.create(name="acc3", org_id=org_id),
    ]


def accounts_to_dict(accounts):
    return [
        {
            "slug": account.name,
            "org_id": account.org_id,
        }
        for account in accounts
    ]


class TestGetAccountsByOrgId:
    def test_org_without_accounts(self, accounts):
        result = get_accounts_by_org_id("unexistent_org")
        assert len(result) == 0

    def test_org_with_accounts(self, accounts, org_id):
        expected_accounts = [account for account in accounts if account.org_id == org_id]
        result = get_accounts_by_org_id(org_id)
        assert_lists_are_matching(accounts_to_dict(result), accounts_to_dict(expected_accounts))


class TestGetAccountUsers:
    def test_account_without_users(self, account_without_users):
        result = get_account_users(account_without_users)
        assert len(result) == 0

    def test_account_with_non_user_roles(self, account_with_non_user_roles):
        result = get_account_users(account_with_non_user_roles)
        assert len(result) == 0

    def test_account_with_users(self, account_with_users, user_ids):
        result = get_account_users(account_with_users)
        assert sorted(result) == sorted(user_ids)

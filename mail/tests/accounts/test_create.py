import pytest
from fan.accounts.create import get_or_create_account

pytestmark = pytest.mark.django_db


@pytest.fixture
def unexisting_org_id():
    return "6666666"


def test_create_new_account(account_name, org_id):
    account = get_or_create_account(account_name, org_id)
    assert account.name == account_name
    assert account.org_id == org_id


def test_get_existing_account(account):
    account_res = get_or_create_account(account.name, account.org_id)
    assert account_res.name == account.name
    assert account_res.org_id == account.org_id


def test_get_existing_account_with_wrong_org_id(account, unexisting_org_id):
    with pytest.raises(Exception):
        get_or_create_account(account.name, unexisting_org_id)

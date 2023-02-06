import pytest
from fan.accounts.organizations.users import get_members_among


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, mock_directory_users):
    pass


@pytest.fixture
def users_belongs_to_org(mock_directory_users, user_ids):
    mock_directory_users.users = {user_id: "admin" for user_id in user_ids}


@pytest.fixture
def user_ids_with_foreign_user(user_ids):
    return user_ids + ["foreign_user"]


def test_passes_org_id(mock_directory_users, org_id, user_ids):
    get_members_among(org_id, user_ids)
    assert mock_directory_users.req_org_id == org_id


def test_passes_tvm_ticket(mock_directory_users, org_id, user_ids):
    get_members_among(org_id, user_ids)
    assert mock_directory_users.req_tvm_ticket == "TEST_TVM_TICKET"


def test_bad_json(mock_directory_users, org_id, user_ids):
    mock_directory_users.resp_code = 200
    mock_directory_users.resp_json = '{"result": []'
    with pytest.raises(Exception, match="response is not a valid json"):
        get_members_among(org_id, user_ids)


@pytest.mark.parametrize("resp_code", [400, 500])
def test_raises_exception(mock_directory_users, org_id, user_ids, resp_code):
    mock_directory_users.resp_code = resp_code
    mock_directory_users.resp_json = '{"result": []}'
    with pytest.raises(Exception):
        get_members_among(org_id, user_ids)


def test_org_without_members(org_id, user_ids):
    assert get_members_among(org_id, user_ids) == []


def test_org_with_members(org_id, user_ids, users_belongs_to_org):
    members = get_members_among(org_id, user_ids)
    assert sorted(members) == sorted(user_ids)


def test_filters_foreign_user(org_id, user_ids_with_foreign_user, user_ids, users_belongs_to_org):
    members = get_members_among(org_id, user_ids_with_foreign_user)
    assert sorted(members) == sorted(user_ids)


def test_returns_empty_result_on_empty_user_ids(org_id, users_belongs_to_org):
    assert get_members_among(org_id, []) == []

import pytest
from fan.accounts.organizations.users import is_member


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def mock_tvm_by_default(mock_tvm):
    pass


def test_passes_user_id(mock_directory_user, user_id, org_id):
    is_member(user_id, org_id)
    assert mock_directory_user.req_user_id == user_id


def test_passes_org_id(mock_directory_user, user_id, org_id):
    is_member(user_id, org_id)
    assert mock_directory_user.req_org_id == org_id


def test_passes_tvm_ticket(mock_directory_user, user_id, org_id):
    is_member(user_id, org_id)
    assert mock_directory_user.req_tvm_ticket == "TEST_TVM_TICKET"


def test_admin_is_member(mock_directory_user, user_id, org_id):
    mock_directory_user.users[user_id] = "admin"
    assert is_member(user_id, org_id)


def test_user_is_member(mock_directory_user, user_id, org_id):
    mock_directory_user.users[user_id] = "user"
    assert is_member(user_id, org_id)


def test_is_not_member(mock_directory_user, user_id, org_id):
    mock_directory_user.resp_code = 404
    assert not is_member(user_id, org_id)


def test_directory_returns_bad_json(mock_directory_user, user_id, org_id):
    mock_directory_user.resp_code = 200
    mock_directory_user.resp_json = '{"is_admin": true'
    with pytest.raises(Exception, match="response is not a valid json"):
        is_member(user_id, org_id)


@pytest.mark.parametrize("directory_resp_code", [400, 500])
def test_raises_exception(mock_directory_user, user_id, org_id, directory_resp_code):
    mock_directory_user.resp_code = directory_resp_code
    mock_directory_user.resp_json = '{"is_admin": true}'
    with pytest.raises(Exception):
        is_member(user_id, org_id)

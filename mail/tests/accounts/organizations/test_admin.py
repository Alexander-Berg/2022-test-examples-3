import pytest
from fan.accounts.organizations.users import is_admin

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_directory_user):
    pass


class TestIsAdmin:
    def test_passes_user_id(self, mock_tvm, mock_directory_user, user_id, org_id):
        res = is_admin(user_id, org_id)
        assert mock_directory_user.req_user_id == user_id

    def test_passes_org_id(self, mock_tvm, mock_directory_user, user_id, org_id):
        res = is_admin(user_id, org_id)
        assert mock_directory_user.req_org_id == org_id

    def test_passes_tvm_ticket(self, mock_tvm, mock_directory_user, user_id, org_id):
        res = is_admin(user_id, org_id)
        assert mock_directory_user.req_tvm_ticket == "TEST_TVM_TICKET"

    def test_is_admin(self, mock_tvm, user_id, user_admin_in_directory, org_id):
        res = is_admin(user_id, org_id)
        assert res == True

    def test_is_not_admin(self, mock_tvm, mock_directory_user, user_id, org_id):
        mock_directory_user.users[user_id] = "user"
        res = is_admin(user_id, org_id)
        assert res == False

    def test_user_not_in_organization(self, mock_tvm, user_id, org_id):
        res = is_admin(user_id, org_id)
        assert res == False

    def test_directory_returns_bad_json(self, mock_tvm, mock_directory_user, user_id, org_id):
        mock_directory_user.resp_code = 200
        mock_directory_user.resp_json = '{"is_admin": true'
        with pytest.raises(Exception, match="response is not a valid json"):
            res = is_admin(user_id, org_id)

    def test_directory_bad_is_admin_field(self, mock_tvm, mock_directory_user, user_id, org_id):
        mock_directory_user.resp_code = 200
        mock_directory_user.resp_json = '{"is_admin": 123}'
        with pytest.raises(Exception, match="is not bool"):
            res = is_admin(user_id, org_id)

    def test_directory_missing_is_admin_field(self, mock_tvm, mock_directory_user, user_id, org_id):
        mock_directory_user.resp_code = 200
        mock_directory_user.resp_json = '{"is_not_admin": true}'
        with pytest.raises(Exception, match='no "is_admin" field'):
            res = is_admin(user_id, org_id)

    @pytest.mark.parametrize("directory_resp_code", [400, 500])
    def test_raises_exception(
        self, mock_tvm, mock_directory_user, user_id, org_id, directory_resp_code
    ):
        mock_directory_user.resp_code = directory_resp_code
        mock_directory_user.resp_json = ""
        with pytest.raises(Exception):
            res = is_admin(user_id, org_id)

import pytest
import ujson

from sendr_utils import enum_value

from hamcrest import assert_that, contains_inanyorder, has_entries, has_items

from mail.payments.payments.api_admin.handlers.idm import ROLES, IDMBaseHandler
from mail.payments.payments.core.entities.enums import Role
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.storage.exceptions import ManagerRoleNotFound


@pytest.fixture
def uid(randn, unique_rand):
    return unique_rand(randn, basket='uid')


@pytest.fixture
def login():
    return 'abracadabra'


@pytest.fixture
def role():
    return Role.ADMIN


@pytest.fixture
def post_data(login, role):
    return {'login': login, 'role': ujson.dumps({'role': role.value})}


@pytest.fixture(autouse=True)
def blackbox_mock(blackbox_client_mocker, uid):
    with blackbox_client_mocker('userinfo', UserInfo(uid=uid)) as mock:
        yield mock


class TestInfo:
    @pytest.fixture(autouse=True)
    def enable_idm_tvm_check(self, payments_settings):
        payments_settings['TVM_CHECK_IDM_CLIENT_ID'] = True

    @pytest.fixture(autouse=True)
    def tvm_src(self, payments_settings):
        """Default tvm src for dummy check result object."""
        return payments_settings.TVM_IDM_CLIENT_ID

    @pytest.fixture(autouse=True)
    def tvm_check_result(self, mocker, tvm_src):
        """Inject dummy check ticket result object into handler."""

        class DummyTicketCheckResult:
            def __init__(self, src):
                self.src = src

        mocker.patch.object(IDMBaseHandler, 'tvm', DummyTicketCheckResult(src=tvm_src))

    @pytest.mark.parametrize('tvm_src', [-1])
    @pytest.mark.asyncio
    async def test_unknown_src_response(self, admin_client, tvm_src):
        response = await admin_client.get('/admin/idm/info/')
        json = await response.json()
        assert_that(json, has_entries({
            'code': 1,
            'error': 'Request source not allowed',
        }))

    @pytest.mark.asyncio
    async def test_correct_src_response(self, admin_client):
        response = await admin_client.get('/admin/idm/info/')
        json = await response.json()
        assert_that(json, has_entries({
            'code': 0,
            'roles': ROLES,
        }))

    @pytest.mark.asyncio
    async def test_all_roles(self, admin_client):
        response = await admin_client.get('/admin/idm/info/')
        json = await response.json()
        assert_that(json['roles']['values'].keys(), contains_inanyorder(*map(enum_value, Role)))


class TestAddRole:
    """Basic scenario: create ManagerRole and Manager on add role request"""

    @pytest.fixture
    async def response(self, post_data, admin_client):
        return await admin_client.post('/admin/idm/add-role/', data=post_data)

    @pytest.mark.asyncio
    async def test_response_format(self, response):
        json = await response.json()
        assert_that(json, has_entries({'code': 0}))

    @pytest.mark.asyncio
    async def test_manager_role_created(self, storage, response, uid, role):
        await storage.manager_role.get(manager_uid=uid, role=role)


class TestRemoveRole:
    @pytest.fixture
    def uid(self, manager_admin):
        return manager_admin.uid

    @pytest.fixture
    def login(self, manager_admin):
        return manager_admin.domain_login

    @pytest.fixture
    async def response(self, post_data, admin_client):
        return await admin_client.post('/admin/idm/remove-role/', data=post_data)

    @pytest.mark.asyncio
    async def test_format_response(self, response):
        json = await response.json()
        assert_that(json, has_entries({'code': 0}))

    @pytest.mark.asyncio
    async def test_removes_role(self, response, uid, role, storage):
        with pytest.raises(ManagerRoleNotFound):
            await storage.manager_role.get(manager_uid=uid, role=role)


class TestListRoles:
    @pytest.fixture
    async def response(self, manager_admin, manager_assessor, admin_client):
        return await admin_client.get('/admin/idm/get-all-roles/')

    @pytest.mark.asyncio
    async def test_response_format(self, response, manager_admin, manager_assessor):
        json = await response.json()
        assert_that(json, has_entries({
            'code': 0,
            'users': has_items(
                {'login': manager_admin.domain_login, 'roles': [{'role': Role.ADMIN.value}]},
                {'login': manager_assessor.domain_login, 'roles': [{'role': Role.ASSESSOR.value}]},
            )
        }))

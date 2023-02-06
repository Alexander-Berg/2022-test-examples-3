import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import MerchantRole


@pytest.fixture
def role():
    return MerchantRole.VIEWER


@pytest.fixture
def description(rands):
    return rands()


class TestCreateUserRole:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.user_role.create import CreateUserRoleAction
        return mock_action(CreateUserRoleAction)

    @pytest.fixture
    def notify(self):
        return True

    @pytest.fixture(params=('user_uid', 'user_email'))
    def user_key(self, request):
        return request.param

    @pytest.fixture
    def user_uid(self, randn):
        return randn()

    @pytest.fixture
    def user_email(self, randmail):
        return randmail()

    @pytest.fixture
    async def user_params(self, role, description, user_key, user_uid, user_email, notify):
        params = {
            'role': role.value,
            'description': description,
            'notify': notify,
        }
        params[user_key] = {
            'user_uid': user_uid,
            'user_email': user_email,
        }[user_key]
        return params

    @pytest.fixture
    def response_func(self, payments_client, merchant):
        async def _inner(user_params):
            return await payments_client.post(f'/v1/merchant/{merchant.merchant_id}/user_role', json=user_params)

        return _inner

    @pytest.fixture
    async def response(self, user_params, response_func):
        return await response_func(user_params)

    @pytest.mark.asyncio
    async def test_bad_request_user_role(self, user_params, response_func):
        user_params.pop('role')
        response = await response_func(user_params)
        assert_that(
            await response.json(),
            has_entries({
                'status': 'fail',
                'code': 400,
            })
        )

    @pytest.mark.parametrize('user_key', ('user_uid',))
    @pytest.mark.asyncio
    async def test_bad_request_no_user_id(self, user_params, response_func):
        user_params.pop('user_uid')
        response = await response_func(user_params)
        assert response.status == 400

    @pytest.mark.parametrize('user_key', ('user_uid',))
    @pytest.mark.asyncio
    async def test_bad_request_two_user_ids(self, user_params, user_email, response_func):
        user_params['user_email'] = user_email
        response = await response_func(user_params)
        assert response.status == 400

    @pytest.mark.asyncio
    async def test_params(self, merchant, response, user_params, role, action):
        user_params.update({'merchant_id': merchant.merchant_id, 'role': role})
        action.assert_called_once_with(**user_params)


class TestDeleteUserRole:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.user_role.delete import DeleteUserRoleAction
        return mock_action(DeleteUserRoleAction)

    @pytest.fixture
    async def user_params(self, uid):
        return {'user_uid': uid}

    @pytest.fixture
    def response_func(self, payments_client, merchant):
        async def _inner(user_params):
            return await payments_client.delete(f'/v1/merchant/{merchant.merchant_id}/user_role', json=user_params)

        return _inner

    @pytest.fixture
    async def response(self, user_params, response_func):
        return await response_func(user_params)

    @pytest.mark.asyncio
    async def test_bad_request(self, user_params, response_func):
        user_params.pop('user_email', None)
        user_params.pop('user_uid', None)
        response = await response_func(user_params)
        assert_that(
            await response.json(),
            has_entries({
                'status': 'fail',
                'code': 400,
            })
        )

    @pytest.mark.asyncio
    async def test_params(self, merchant, response, user_params, action):
        user_params.update({'merchant_id': merchant.merchant_id})
        action.assert_called_once_with(**user_params)


class TestMerchantUserRoleList:
    @pytest.fixture(params=('merchant', 'merchant_draft'))
    def mode(self, request):
        return request.param

    @pytest.fixture
    def merchant_id(self, mode, merchant, merchant_draft):
        return {
            'merchant': merchant.merchant_id,
            'merchant_draft': merchant_draft.merchant_id,
        }[mode]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, user_roles, user_roles_draft, mode):
        from mail.payments.payments.core.actions.user_role.get import GetUserRolesForMerchantAction
        return mock_action(GetUserRolesForMerchantAction, {
            'merchant': user_roles,
            'merchant_draft': user_roles_draft,
        }[mode])

    @pytest.fixture
    async def response(self, payments_client, merchant_id):
        return await payments_client.get(f'/v1/merchant/{merchant_id}/user_role')

    def test_params_merchant(self, response, action, merchant_id):
        action.assert_called_once_with(merchant_id=merchant_id)

    def test_response(self, uid, response, action):
        assert response.status == 200


class TestUserRoleList:
    @pytest.fixture(autouse=True)
    def action(self, mock_action, merchant, merchant_draft, user_roles, user_roles_draft):
        from mail.payments.payments.core.actions.user_role.get import GetUserRolesForUserAction

        roles = []
        for role in user_roles:
            role.merchant = merchant
            roles.append(role)
        for role in user_roles_draft:
            role.merchant = merchant_draft
            roles.append(role)

        return mock_action(GetUserRolesForUserAction, roles)

    @pytest.fixture
    def tvm(self, base_tvm, uid, tvm_client_id):
        base_tvm.src = tvm_client_id
        base_tvm.default_uid = uid
        return base_tvm

    @pytest.fixture
    async def response(self, payments_client, uid, tvm):
        return await payments_client.get(f'/v1/user_role/{uid}')

    def test_params(self, uid, response, action):
        action.assert_called_once_with(uid=uid)

    def test_response(self, uid, response, action):
        assert response.status == 200

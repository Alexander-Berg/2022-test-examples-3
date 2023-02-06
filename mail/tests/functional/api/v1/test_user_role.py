import itertools

import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.storage.exceptions import UserRoleNotFound
from mail.payments.payments.tests.utils import check_merchant_from_person, dummy_coro_generator


@pytest.fixture
def info(uid):
    return UserInfo(uid=uid, default_email="qq@yandex.ru")


@pytest.fixture(autouse=True)
def blackbox_mock(blackbox_client_mocker, info):
    with blackbox_client_mocker('userinfo', info) as mock:
        yield mock


@pytest.fixture(params=[
    MerchantRole.VIEWER,
    MerchantRole.OPERATOR,
    MerchantRole.ADMIN
])
def role(request):
    return request.param


@pytest.fixture
def description(rands):
    return rands()


@pytest.fixture
def acting_user_role():
    return MerchantRole.ADMIN


@pytest.fixture
async def acting_user(create_acting_user, acting_user_role, request):
    return await create_acting_user(role=acting_user_role)


@pytest.fixture
def tvm_uid(acting_user):
    return acting_user['user'].uid


class TestMerchantUserRolePost:
    @pytest.fixture(params=('user_uid', 'user_email'))
    def user_key(self, request):
        return request.param

    @pytest.fixture
    def user_email(self, randmail):
        return randmail()

    @pytest.fixture
    def user_params(self, user_key, uid, user_email, role, description):
        params = {
            'role': role.value,
            'description': description,
            user_key: {
                'user_uid': uid,
                'user_email': user_email,
            }[user_key]
        }
        return params

    @pytest.fixture
    async def make_response(self, client, merchant, user_params):
        async def _inner(expected_status=200):
            response = await client.post(
                f'/v1/merchant/{merchant.merchant_id}/user_role',
                json=user_params,
            )
            assert response.status == expected_status
            return await response.json()

        return _inner

    @pytest.mark.asyncio
    @pytest.mark.parametrize('role', [MerchantRole.VIEWER])
    async def test_user_not_authenticated(self, make_response, role):
        response = await make_response(expected_status=403)
        assert response == {'code': 403, 'data': {'message': 'USER_NOT_AUTHENTICATED'}, 'status': 'fail'}

    @pytest.mark.asyncio
    @pytest.mark.parametrize('acting_user_role', [MerchantRole.VIEWER, MerchantRole.OPERATOR])
    @pytest.mark.usefixtures('acting_user')
    @pytest.mark.usefixtures('tvm')
    async def test_user_not_authorized(self, make_response):
        response = await make_response(expected_status=403)
        assert response['data']['message'] == 'MERCHANT_USER_NOT_AUTHORIZED'
        assert response['status'] == 'fail'
        assert response['code'] == 403

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('acting_user')
    @pytest.mark.usefixtures('tvm')
    async def test_returned(self, user_params, merchant, info, make_response):
        response = await make_response(expected_status=200)
        assert_that(response['data'], has_entries({
            'role': user_params['role'],
            'description': user_params['description'],
            'merchant_id': merchant.merchant_id,
            'user_uid': info.uid,
            'user_email': user_params.get('user_email', info.default_email),
        }))


class TestMerchantUserRoleGet:
    @pytest.fixture
    async def user_roles(self, merchant, randn, rands, randmail, storage):
        user_roles = []
        for role in [MerchantRole.VIEWER, MerchantRole.OPERATOR, MerchantRole.ADMIN]:
            uid = randn()
            user = await storage.user.create(User(uid=uid, email=f'{uid}@ya.ru'))
            user_role = await storage.user_role.create(UserRole(
                uid=user.uid,
                merchant_id=merchant.merchant_id,
                role=role,
                description=rands(),
                email=randmail(),
            ))
            user_roles.append(user_role)
        return user_roles

    @pytest.fixture
    def make_response(self, client, merchant):
        async def _inner(expected_status=200):
            r = await client.get(
                f'/v1/merchant/{merchant.merchant_id}/user_role')
            assert r.status == expected_status
            return await r.json()

        return _inner

    @pytest.mark.parametrize('acting_user_role', [MerchantRole.VIEWER])
    @pytest.mark.usefixtures('acting_user')
    @pytest.mark.usefixtures('tvm')
    @pytest.mark.asyncio
    async def test_returned(self, user_roles, acting_user, make_response):
        response = await make_response(expected_status=200)
        assert_that(
            response['data'],
            contains_inanyorder(*[
                has_entries({
                    'user_uid': user_role.uid,
                    'user_email': user_role.email,
                    'merchant_id': user_role.merchant_id,
                    'role': user_role.role.value,
                    'description': user_role.description,
                })
                for user_role in itertools.chain(user_roles, [acting_user['user_role']])
            ])
        )


class TestMerchantUserRoleDelete:
    @pytest.fixture
    def uid(self, randn):
        return randn()

    @pytest.fixture
    def user_params(self, uid, role):
        return {'user_uid': uid}

    @pytest.fixture
    def user_entity(self, storage, uid):
        return User(uid=uid, email='test-email@ya.ru')

    @pytest.fixture
    async def user(self, storage, user_entity):
        return await storage.user.create(user_entity)

    @pytest.fixture
    async def user_role_entity(self, storage, merchant, role, uid):
        return UserRole(uid=uid, merchant_id=str(merchant.uid), role=role)

    @pytest.fixture
    async def user_role(self, storage, user, merchant, user_role_entity):
        return await storage.user_role.create(user_role_entity)

    @pytest.fixture
    async def make_response(self, client, user_role, user_params):
        async def _inner(expected_status=200):
            r = await client.delete(
                f'/v1/merchant/{user_role.merchant_id}/user_role',
                json=user_params
            )
            assert r.status == expected_status
            return await r.json()

        return _inner

    @pytest.mark.parametrize('acting_user_role', [MerchantRole.ADMIN])
    @pytest.mark.usefixtures('acting_user')
    @pytest.mark.usefixtures('tvm')
    @pytest.mark.asyncio
    async def test_response(self, make_response):
        response = await make_response()
        assert response['status'] == 'success'
        assert response['code'] == 200

    @pytest.mark.parametrize('acting_user_role', [MerchantRole.ADMIN])
    @pytest.mark.usefixtures('acting_user')
    @pytest.mark.usefixtures('tvm')
    @pytest.mark.asyncio
    async def test_deleted(self, storage, user_role_entity, make_response):
        await make_response()
        with pytest.raises(UserRoleNotFound):
            await storage.user_role.get(merchant_id=user_role_entity.merchant_id, uid=user_role_entity.uid)


class TestUserRoleGet:
    @pytest.fixture
    def uid(self, randn):
        return randn()

    @pytest.fixture
    def tvm_uid(self, uid):
        return uid

    @pytest.fixture
    async def user_roles(self, merchant, another_merchant, uid, rands, randmail, storage):
        user_roles = []
        user = await storage.user.create(User(uid=uid, email=randmail()))

        for merchant in [merchant, another_merchant]:
            user_role = await storage.user_role.create(UserRole(
                uid=user.uid,
                merchant_id=merchant.merchant_id,
                role=MerchantRole.VIEWER,
                description=rands(),
                email=randmail(),
            ))
            user_roles.append(user_role)
        return user_roles

    @pytest.fixture(autouse=True)
    def balance_person_mock(self, mocker, person_entity):
        yield mocker.patch(
            'mail.payments.payments.interactions.balance.BalanceClient.get_person',
            mocker.Mock(side_effect=dummy_coro_generator(person_entity)),
        )

    @pytest.fixture
    async def response(self, client, uid, balance_person_mock, tvm):
        r = await client.get(
            f'/v1/user_role/{uid}')
        assert r.status == 200
        return await r.json()

    def test_returned(self, user_roles, response):
        assert_that(
            response['data'],
            contains_inanyorder(*[
                has_entries({
                    'user_uid': user_role.uid,
                    'user_email': user_role.email,
                    'merchant_id': user_role.merchant_id,
                    'role': user_role.role.value,
                    'description': user_role.description,
                })
                for user_role in user_roles
            ])
        )

    def test_merchant(self, user_roles, stored_person_entity, merchant, another_merchant, response):
        for user_role, m in zip(response['data'], [merchant, another_merchant]):
            check_merchant_from_person(m, stored_person_entity, user_role['merchant'])

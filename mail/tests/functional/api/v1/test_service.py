from datetime import timezone

import pytest

from sendr_tvm.qloud_async_tvm import TicketCheckResult
from sendr_utils import alist, enum_value

from hamcrest import all_of, assert_that, contains_inanyorder, empty, has_entries, has_items

from mail.payments.payments.core.entities.enums import MerchantRole, TaskType
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole


@pytest.fixture(autouse=True)
def person_get_mock(balance_client_mocker, person_entity):
    with balance_client_mocker('get_person', person_entity) as mock:
        yield mock


def check_service_merchant(service_merchant, service_merchant_dict, person_entity, merchant):
    addresses = {
        'legal': {
            'city': person_entity.legal_address_city,
            'home': person_entity.legal_address_home,
            'country': 'RUS',
            'street': person_entity.legal_address_street,
            'zip': person_entity.legal_address_postcode,
        }
    }
    if person_entity.has_post_address:
        addresses['post'] = {
            'city': person_entity.address_city,
            'home': person_entity.address_home,
            'country': 'RUS',
            'street': person_entity.address_street,
            'zip': person_entity.address_postcode,
        }
    assert_that(
        service_merchant_dict,
        has_entries({
            'uid': service_merchant.uid,
            'service_id': service_merchant.service_id,
            'entity_id': service_merchant.entity_id,
            'description': service_merchant.description,
            'service_merchant_id': service_merchant.service_merchant_id,
            'enabled': service_merchant.enabled,
            'created': service_merchant.created.astimezone(timezone.utc).isoformat(),
            'updated': service_merchant.updated.astimezone(timezone.utc).isoformat(),
            'deleted': service_merchant.deleted,
            'organization': {
                'type': merchant.organization.type.value,
                'name': person_entity.name,
                'englishName': merchant.organization.english_name,
                'fullName': person_entity.longname,
                'inn': person_entity.inn,
                'kpp': person_entity.kpp,
                'ogrn': person_entity.ogrn,
                'scheduleText': merchant.organization.schedule_text,
                'siteUrl': merchant.organization.site_url,
                'description': merchant.organization.description,
            },
            'addresses': addresses,
            'service': {
                'service_id': service_merchant.service.service_id,
                'name': service_merchant.service.name,
            },
        })
    )


class TestGetService:
    @pytest.fixture
    def bad_service_merchant_id(self, randn):
        return randn()

    @pytest.fixture
    def response_func(self, client, service_merchant, tvm):
        async def _inner(status=200):
            r = await client.get(f'/v1/internal/service/{service_merchant.service_merchant_id}')
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.fixture
    async def service_merchant_not_found_response(self, client, bad_service_merchant_id, tvm):
        r = await client.get(f'/v1/internal/service/{bad_service_merchant_id}')
        assert r.status == 404
        return await r.json()

    @pytest.mark.asyncio
    async def test_get_service__success(self, response_func, service_merchant, stored_person_entity, merchant):
        response = await response_func()
        check_service_merchant(service_merchant, response['data'], stored_person_entity, merchant)

    @pytest.mark.asyncio
    async def test_not_found_response(self, service_merchant_not_found_response):
        assert_that(
            service_merchant_not_found_response,
            has_entries({
                'code': 404,
                'status': 'fail',
                'data': has_items('message'),
            }),
        )


class TestPostService:
    @pytest.fixture
    def user_uid(self, randn):
        return randn()

    @pytest.fixture
    def role(self):
        return MerchantRole.ADMIN

    @pytest.fixture
    def tvm_uid(self, user_uid):
        return user_uid

    @pytest.fixture
    async def user_role(self, merchant, user_uid, role, storage):
        user = await storage.user.create(User(uid=user_uid, email=f'{user_uid}@ya.ru'))
        user_role = await storage.user_role.create(UserRole(uid=user.uid, merchant_id=merchant.merchant_id,
                                                            role=role))
        return user_role

    @pytest.fixture
    async def owner(self, storage, user, merchant):
        entity = UserRole(uid=user.uid, merchant_id=str(merchant.uid), role=MerchantRole.OWNER)
        return await storage.user_role.create(entity)

    @pytest.fixture
    def request_json(self, merchant, user_role):
        return {
            'token': merchant.token,
            'uid': user_role.uid,
            'entity_id': 'entity_id',
            'description': 'test_description',
            'autoenable': True
        }

    @pytest.fixture
    async def service_merchant_response(self, client, merchant, owner, request_json, tvm):
        return await client.post('/v1/internal/service', json=request_json)

    class TestBaseSuccessResponse:
        def request_json(self, merchant, user_role):
            return {
                'token': merchant.token,
                'uid': user_role.uid,
                'entity_id': 'entity_id',
                'description': 'test_description',
                'autoenable': True
            }

        async def check_success(self, service_merchant_response, request_json, service, merchant, owner,
                                stored_person_entity):
            addresses = {
                'legal': {
                    'city': stored_person_entity.legal_address_city,
                    'country': 'RUS',
                    'home': stored_person_entity.legal_address_home,
                    'street': stored_person_entity.legal_address_street,
                    'zip': stored_person_entity.legal_address_postcode,
                }
            }
            if stored_person_entity.has_post_address:
                addresses['post'] = {
                    'city': stored_person_entity.address_city,
                    'country': 'RUS',
                    'home': stored_person_entity.address_home,
                    'street': stored_person_entity.address_street,
                    'zip': stored_person_entity.address_postcode,
                }
            assert service_merchant_response.status == 200
            resp_json = await service_merchant_response.json()
            assert_that(
                resp_json['data'],
                all_of(
                    has_entries({
                        'uid': merchant.uid,
                        'service_id': service.service_id,
                        'entity_id': request_json['entity_id'],
                        'description': request_json['description'],
                        'enabled': request_json.get('autoenable') or False,
                        'deleted': False,
                        'organization': {
                            'type': merchant.organization.type.value,
                            'name': stored_person_entity.name,
                            'englishName': merchant.organization.english_name,
                            'fullName': stored_person_entity.longname,
                            'inn': stored_person_entity.inn,
                            'kpp': stored_person_entity.kpp,
                            'ogrn': stored_person_entity.ogrn,
                            'scheduleText': merchant.organization.schedule_text,
                            'siteUrl': merchant.organization.site_url,
                            'description': merchant.organization.description,
                        },
                        'addresses': addresses,
                        'service': {
                            'service_id': service.service_id,
                            'name': service.name,
                        },
                    }),
                    has_items('service_merchant_id', 'created', 'updated'),
                ),
            )

    class TestBasicSuccessResponse(TestBaseSuccessResponse):
        @pytest.mark.asyncio
        async def test_basic__success(self, service_merchant_response, request_json, service, merchant, owner,
                                      stored_person_entity):
            await self.check_success(service_merchant_response, request_json, service, merchant, owner,
                                     stored_person_entity)

    class TestSuccessResponse(TestBaseSuccessResponse):
        @pytest.fixture
        def tvm_uid(self, merchant):
            return merchant.uid

        @pytest.fixture
        def request_json(self, merchant):
            return {
                'token': merchant.token,
                'uid': merchant.uid,
                'entity_id': 'entity_id',
                'description': 'test_description',
                'autoenable': True
            }

        @pytest.fixture(autouse=True, params=(
            pytest.param(lambda d: {d.pop('autoenable'), d.pop('uid')}, id='by_token'),
            pytest.param(lambda d: d, id='by_uid_token_and_autoenable'),
        ))
        def setup(self, request, request_json):
            request.param(request_json)

        @pytest.mark.asyncio
        async def test_response__success(self, service_merchant_response, request_json, service, merchant, owner,
                                         stored_person_entity):
            await self.check_success(service_merchant_response, request_json, service, merchant, owner,
                                     stored_person_entity)

    class TestBadRequest:
        @pytest.fixture(autouse=True, params=(
            pytest.param(lambda d: d.pop('token'), id='without_token'),
        ))
        def setup(self, request, request_json):
            mutator_function = request.param
            mutator_function(request_json)

        @pytest.mark.asyncio
        async def test_bad_request(self, service_merchant_response):
            assert service_merchant_response.status == 400

    class TestCreateByUidWithNonExistingUidResponse:
        @pytest.fixture
        def bad_uid(self, merchant, unique_rand, randn):
            return unique_rand(randn, basket='uid')

        @pytest.fixture
        def tvm_uid(self, bad_uid):
            return bad_uid

        @pytest.fixture
        def request_json(self, bad_uid, merchant, service):
            return {
                'uid': bad_uid,
                'token': merchant.token,
                'entity_id': 'entity_id',
                'description': 'test_description',
                'autoenable': True
            }

        @pytest.mark.asyncio
        async def test_create_by_uid_with_non_existing_uid__response_status_code(self, tvm, service_merchant_response):
            assert service_merchant_response.status == 403

        @pytest.mark.asyncio
        async def test_create_by_uid_with_non_existing_uid__response_format(self, service_merchant_response, bad_uid):
            json = await service_merchant_response.json()
            assert_that(json, has_entries({
                'status': 'fail',
                'code': 403,
                'data': has_entries({
                    'message': 'MERCHANT_USER_NOT_AUTHORIZED',
                })
            }))

    class TestCreateByTokenWithBadTokenResponse:
        @pytest.fixture
        def request_json(self, merchant, service):
            return {
                'token': merchant.token + 'hey' + merchant.token,
                'entity_id': 'entity_id',
                'description': 'test_description',
            }

        @pytest.mark.asyncio
        async def test_create_by_token_with_bad_token_response__response_status_code(self, service_merchant_response):
            assert service_merchant_response.status == 404

        @pytest.mark.asyncio
        async def test_create_by_token_with_bad_token_response__response_format(self, service_merchant_response):
            json = await service_merchant_response.json()
            assert_that(json, has_entries({
                'status': 'fail',
                'code': 404,
                'data': has_entries({
                    'message': 'MERCHANT_NOT_FOUND',
                })
            }))

    class TestForbidden:
        @pytest.fixture
        def request_json(self, merchant):
            return {
                'uid': merchant.uid,
                'token': merchant.token,
                'entity_id': 'entity_id',
                'description': 'test_descr',
                'autoenable': True}

        @pytest.fixture
        def tvm(self, mocker, merchant, service):
            mocker.patch.object(TicketCheckResult, 'default_uid', 111111111)

        @pytest.mark.asyncio
        async def test_forbidden__user_not_authorized(self, service_merchant_response):
            assert service_merchant_response.status == 403

    class TestNotAllowedRoles:
        @pytest.fixture(params=[
            MerchantRole.VIEWER,
            MerchantRole.OPERATOR
        ])
        def role(self, request):
            return request.param

        @pytest.fixture
        def request_json(self, merchant, user_role):
            return {
                'token': merchant.token,
                'uid': user_role.uid,
                'autoenable': True,
                'entity_id': 'entity_id',
                'description': 'test_descr',
            }

        @pytest.fixture
        def tvm(self, mocker, merchant, service):
            mocker.patch.object(TicketCheckResult, 'default_uid', 111111111)

        @pytest.mark.asyncio
        async def test_not_allowed_roles__user_not_authorized(self, service_merchant_response):
            assert service_merchant_response.status == 403

    class TestAlreadyExists:
        @pytest.fixture(params=(True, False))
        def service_merchant_enabled(self, request):
            return request.param

        @pytest.fixture(autouse=True)
        async def setup_enabled(self, storage, service_merchant, service_merchant_enabled):
            service_merchant.enabled = service_merchant_enabled
            await storage.service_merchant.save(service_merchant)

        @pytest.fixture
        def email_tasks(self, service_merchant, tasks):
            return [
                t for t in tasks
                if t.task_type == TaskType.RUN_ACTION and t.action_name == 'transact_email_action' and (
                    t.params.get('action_kwargs', {}).
                    get('render_context', {}).
                    get('service_merchant', {}).
                    get('service_merchant_id')
                ) == service_merchant.service_merchant_id
            ]

        @pytest.fixture
        def request_json(self, service_merchant, merchant):
            return {
                'token': merchant.token,
                'entity_id': service_merchant.entity_id,
                'description': 'test_descr',
            }

        @pytest.mark.asyncio
        async def test_service_merchant_already_exists(self, service_merchant_response):
            assert service_merchant_response.status == 409

        @pytest.mark.parametrize('service_merchant_enabled', (False,))
        @pytest.mark.asyncio
        async def test_transact_email_task_created_when_not_enabled(self, service_merchant_response, email_tasks):
            assert len(email_tasks) == 1

        @pytest.mark.parametrize('service_merchant_enabled', (True,))
        @pytest.mark.asyncio
        async def test_transact_email_task_not_created_when_enabled(self, service_merchant_response, email_tasks):
            assert len(email_tasks) == 0


class TestGetServiceList:
    @pytest.fixture
    async def make_request(self, service, tvm, client):
        async def _inner():
            return await client.get('/v1/service')

        return _inner

    @pytest.mark.asyncio
    async def test_response(self, storage, make_request):
        services_in_db = await alist(storage.service.find(hidden=False))
        assert len(services_in_db) > 0  # sanity check
        # запрос делаем ПОСЛЕ похода в базу, чтобы проверить, что данные в ответе объемлют над данными из базы
        response = await make_request()
        response_data = (await response.json())['data']

        db_ids = set([service.service_id for service in services_in_db])
        api_ids = set([el['service_id'] for el in response_data['services']])
        assert db_ids.issubset(api_ids)  # another sanity check

        assert_that(
            response_data,
            has_entries({
                'services': contains_inanyorder(*[
                    has_entries({
                        'name': service.name,
                        'service_id': service.service_id,
                        'options': has_entries({
                            'required_acquirer': enum_value(service.options.required_acquirer),
                            'require_online': service.options.require_online,
                            'can_skip_registration': service.options.can_skip_registration,
                            'icon_url': service.options.icon_url
                        })
                    })
                    for service in services_in_db if service.service_id in db_ids
                ])
            })
        )


class TestServiceMerchantDelete:
    @pytest.fixture
    def response_func(self, client, tvm, service_merchant):
        async def _inner(status=200):
            r = await client.delete(f'/v1/internal/service/{service_merchant.service_merchant_id}')
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_service_merchant_delete__success(self, response_func):
        response = await response_func()
        assert_that(response['data'], empty())


class TestServiceListByServiceMerchants:
    @pytest.fixture
    def response_func(self, client, tvm, service_merchant):
        async def _inner():
            r = await client.get(f'/v1/service/{service_merchant.uid}')
            assert r.status == 200
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_services_by_service_merchants(self, response_func, service):
        response_data = (await response_func())['data']
        assert_that(response_data, has_entries({
            'services': contains_inanyorder(
                has_entries({
                    'name': service.name,
                    'service_id': service.service_id,
                    'options': has_entries({
                        'required_acquirer': enum_value(service.options.required_acquirer),
                        'require_online': service.options.require_online,
                    })
                })
            )
        }))

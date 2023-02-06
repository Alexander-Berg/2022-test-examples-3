from datetime import timezone

import pytest

from hamcrest import assert_that, has_entries

from .base import BaseInternalHandlerTvmTest


class TestServicePost(BaseInternalHandlerTvmTest):
    @pytest.fixture(params=('merchant', 'merchant_preregistered'))
    def merchant_type(self, request):
        return request.param

    @pytest.fixture
    def merchant(self, merchant_type, merchant, merchant_preregistered):
        return {
            'merchant': merchant,
            'merchant_preregistered': merchant_preregistered
        }[merchant_type]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, service_merchant, merchant):
        from mail.payments.payments.core.actions.service_merchant.create import CreateServiceMerchantAction
        return mock_action(CreateServiceMerchantAction, (service_merchant, merchant))

    @pytest.fixture
    def request_json(self, merchant):
        return {
            'token': merchant.token,
            'uid': merchant.uid,
            'entity_id': 'entity_id',
            'description': 'test_descr',
            'autoenable': True,
        }

    @pytest.fixture
    async def response(self, payments_client, service_merchant, request_json):
        return await payments_client.post('/v1/internal/service', json=request_json)

    class TestSuccessRequest:
        @pytest.fixture(autouse=True, params=(
            pytest.param(lambda d: {d.pop('autoenable'), d.pop('uid')}, id='by_token'),
            pytest.param(lambda d: d, id='by_uid_token_autoenable'),
        ))
        def setup(self, request, request_json):
            request.param(request_json)

        def test_status(self, response):
            assert response.status == 200

        def test_params(self, service_merchant, response, action, request_json, tvm_client_id):
            request_json.update({'service_tvm_id': tvm_client_id})
            action.assert_called_once_with(**request_json)

    class TestBadRequest:
        @pytest.fixture(autouse=True, params=(
            pytest.param(lambda d: d.pop('token'), id='without_token'),
            pytest.param(lambda d: d.pop('uid'), id='with_token_and_enabled'),
        ))
        def setup(self, request, request_json):
            request.param(request_json)

        @pytest.mark.asyncio
        async def test_bad_request(self, response):
            assert response.status == 400

    class TestUserNotAuthorizedException:
        @pytest.fixture
        def request_json(self, merchant):
            return {
                'uid': merchant.uid,
                'token': merchant.token,
                'entity_id': 'entity_id',
                'description': 'test_descr',
                'autoenable': True
            }

        @pytest.fixture
        def tvm(self, mocker, merchant, tvm_client_id):
            mocker.patch('sendr_tvm.qloud_async_tvm.TicketCheckResult')
            mocker.src = tvm_client_id
            mocker.default_uid = 11111111111
            return mocker

        @pytest.mark.asyncio
        async def test_user_not_authorized(self, response):
            assert response.status == 403


class TestServiceMerchantGet:
    @pytest.fixture(params=('merchant', 'merchant_preregistered'))
    def merchant_type(self, request):
        return request.param

    @pytest.fixture
    def merchant(self, merchant_type, merchant, merchant_preregistered):
        return {
            'merchant': merchant,
            'merchant_preregistered': merchant_preregistered
        }[merchant_type]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, service_merchant):
        from mail.payments.payments.core.actions.service_merchant.get import GetServiceMerchantServiceAction
        return mock_action(GetServiceMerchantServiceAction, service_merchant)

    @pytest.fixture(autouse=True)
    def action_merchant(self, mock_action, merchant):
        from mail.payments.payments.core.actions.merchant.get import GetMerchantAction
        return mock_action(GetMerchantAction, merchant)

    @pytest.fixture
    async def response(self, payments_client, service_merchant):
        return await payments_client.get(f'/v1/internal/service/{service_merchant.service_merchant_id}')

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_status(self, response):
        assert response.status == 200

    def test_returned(self, merchant, service_merchant, service, response_json):
        assert_that(
            response_json,
            has_entries({
                'code': 200,
                'status': 'success',
                'data': has_entries({
                    'acquirer': merchant.acquirer.value,
                    'created': merchant.created.astimezone(timezone.utc).isoformat(),
                    'person_id': merchant.person_id,
                    'service_merchant_id': service_merchant.service_merchant_id,
                    'submerchant_id': merchant.submerchant_id,
                    'uid': merchant.uid,
                    'service': {'name': service.name, 'service_id': service.service_id},
                    'enabled': service_merchant.enabled,
                    'status': merchant.status.value,
                    'contract_id': merchant.contract_id,
                    'entity_id': service_merchant.entity_id,
                    'client_id': merchant.client_id,
                    'description': service_merchant.description,
                    'service_id': service.service_id,
                    'updated': merchant.updated.astimezone(timezone.utc).isoformat(),
                    'deleted': service_merchant.deleted
                })
            })
        )

    class TestUserNotAuthorizedException:
        @pytest.fixture(autouse=True)
        def action(self):
            pass

        @pytest.fixture(autouse=True)
        def run_action_calls(self):
            pass

        @pytest.fixture(autouse=True)
        def mock_tvm_src(self, mocker, service_client):
            mocker.patch(
                'mail.payments.payments.api.handlers.internal.base.BaseInternalHandler.tvm_src',
                service_client.tvm_id + 1
            )

        @pytest.mark.asyncio
        async def test_user_not_authorized(self, response):
            assert response.status == 404


class TestServiceMerchantDelete:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.service_merchant.delete import DeleteServiceMerchantServiceAction
        return mock_action(DeleteServiceMerchantServiceAction)

    @pytest.fixture
    async def response(self, payments_client, service_merchant):
        return await payments_client.delete(f'/v1/internal/service/{service_merchant.service_merchant_id}')

    @pytest.fixture
    def context_params(self, service_merchant, tvm_client_id):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': tvm_client_id
        }

    def test_context(self, context_params, response, action):
        action.assert_called_once_with(**context_params)

    @pytest.mark.asyncio
    async def test_merchant_service_returned(self, response):
        assert_that(
            await response.json(),
            has_entries({
                'data': {}
            })
        )

    class TestUserNotAuthorizedException:
        @pytest.fixture(autouse=True)
        def action(self):
            pass

        @pytest.fixture(autouse=True)
        def run_action_calls(self):
            pass

        @pytest.fixture(autouse=True)
        def mock_tvm_src(self, mocker, service_client):
            mocker.patch(
                'mail.payments.payments.api.handlers.internal.base.BaseInternalHandler.tvm_src',
                service_client.tvm_id + 1
            )

        @pytest.mark.asyncio
        async def test_user_not_authorized(self, response):
            assert response.status == 404

import pytest

from hamcrest import assert_that, contains, has_entries

from mail.payments.payments.core.actions.service import GetServiceListAction, GetServiceListByServiceMerchantsAction
from mail.payments.payments.core.entities.enums import AcquirerType
from mail.payments.payments.core.entities.service import Service, ServiceOptions


class TestServiceListHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        services = [
            Service(
                service_id=11,
                name='first_service',
                options=ServiceOptions(
                    require_online=False,
                    required_acquirer=AcquirerType.KASSA,
                    can_skip_registration=True,
                    icon_url='path_to_first_service_icon',
                ),
            ),
            Service(
                service_id=22,
                name='second_service',
            ),
        ]
        return mock_action(GetServiceListAction, services)

    @pytest.fixture
    def params(self):
        return {
            'hidden': 'true'
        }

    @pytest.fixture
    async def response(self, service, payments_client, params):
        return await payments_client.get('/v1/service', params=params)

    def test_called(self, response, action):
        action.assert_called_once_with(hidden=False)

    @pytest.mark.asyncio
    async def test_response_format(self, response):
        response_json = await response.json()
        assert_that(response_json, has_entries({
            'data': has_entries({
                'services': [
                    {
                        'service_id': 11,
                        'name': 'first_service',
                        'options': {
                            'require_online': False,
                            'required_acquirer': 'kassa',
                            'can_skip_registration': True,
                            'icon_url': 'path_to_first_service_icon',
                        },
                    },
                    {
                        'service_id': 22,
                        'name': 'second_service',
                        'options': {
                            'require_online': True,
                            'required_acquirer': None,
                            'can_skip_registration': False,
                            'icon_url': None,
                        },
                    }
                ]
            })
        }))


class TestServiceListByServiceMerchantsHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        action_response = [
            Service(
                service_id=1,
                name='service_name_1',
                options=ServiceOptions(
                    require_online=False,
                    required_acquirer=AcquirerType.KASSA,
                    can_skip_registration=True,
                    icon_url='path_to_service_1_icon',
                )
            ),
            Service(service_id=2, name='service_name_2'),
        ]
        return mock_action(GetServiceListByServiceMerchantsAction, action_response)

    @pytest.fixture
    async def response(self, action, payments_client, service_merchant):
        return await payments_client.get(f'/v1/service/{service_merchant.uid}')

    def test_action_call(self, action, service_merchant, response):
        action.assert_called_once_with(uid=service_merchant.uid)

    @pytest.mark.asyncio
    async def test_get(self, response):
        response = await response.json()
        assert_that(response, has_entries({
            'data': has_entries({
                'services': contains(
                    has_entries({
                        'service_id': 1,
                        'name': 'service_name_1',
                        'options': {
                            'require_online': False,
                            'required_acquirer': 'kassa',
                            'can_skip_registration': True,
                            'icon_url': 'path_to_service_1_icon',
                        }
                    }),
                    has_entries({
                        'service_id': 2,
                        'name': 'service_name_2',
                    })
                )
            })
        }))

import pytest
import ujson
from sendr_writers.base.pusher import InteractionResponseLog

from hamcrest import all_of, assert_that, has_entries, has_properties, is_

from mail.payments.payments.interactions.exceptions import TinkoffError
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.helpers import convert_to_camelcase


@pytest.fixture
def access_token():
    return 'SOME SECRET TOKEN'


class TestTinkoffClientGetToken:
    @pytest.fixture
    def response_json(self, access_token):
        return {
            'access_token': access_token,
            'other': 'string',
        }

    @pytest.mark.asyncio
    async def test_request_returns_token(self, tinkoff_client, access_token):
        assert await tinkoff_client.get_token() == access_token

    @pytest.mark.asyncio
    async def test_request(self, payments_settings, tinkoff_client):
        username = 'aaa'
        password = 'bbb'
        basic_login = 'ccc'
        basic_password = 'ddd'
        payments_settings.update({
            'TINKOFF_USERNAME': username,
            'TINKOFF_PASSWORD': password,
            'TINKOFF_BASIC_LOGIN': basic_login,
            'TINKOFF_BASIC_PASSWORD': basic_password,
        })
        await tinkoff_client.get_token()
        assert_that(
            tinkoff_client.call_kwargs,
            has_entries({
                'auth': has_properties({
                    'login': basic_login,
                    'password': basic_password,
                }),
                'data': {
                    'username': username,
                    'password': password,
                    'grant_type': 'password',
                },
            })
        )

    @pytest.mark.asyncio
    async def test_cache_caches(self, tinkoff_client, access_token):
        await tinkoff_client.get_token()
        assert tinkoff_client._token_cache['token'] == access_token

    @pytest.mark.asyncio
    async def test_cache_returns_token(self, tinkoff_client, access_token):
        tinkoff_client._token_cache['token'] = access_token
        assert await tinkoff_client.get_token() == access_token

    @pytest.mark.asyncio
    async def test_cache_no_request(self, tinkoff_client, access_token):
        tinkoff_client._token_cache['token'] = access_token
        await tinkoff_client.get_token()
        assert tinkoff_client.calls == []

    @pytest.mark.asyncio
    async def test_response_logged(self, tinkoff_client, pushers_mock, response_json):
        await tinkoff_client.get_token()
        assert_that(
            pushers_mock.response_log.push.call_args[0][0],
            all_of(
                is_(InteractionResponseLog),
                has_properties(dict(
                    response=response_json,
                    request_url=tinkoff_client.call_args[2],
                    request_method=tinkoff_client.call_args[1],
                    request_kwargs=tinkoff_client.call_kwargs,
                    request_id=tinkoff_client.request_id,
                    status=200,
                ))
            ),
        )


class TestTinkoffClientCreateMerchant:
    @pytest.fixture
    def submerchant_id(self, randn):
        return randn()

    @pytest.fixture
    def response_json(self, submerchant_id, randn):
        return {'terminals': [{'merchantId': randn()}], 'shopCode': submerchant_id}

    @pytest.fixture(autouse=True)
    def token_mock(self, mocker, tinkoff_client, access_token):
        mock = mocker.patch.object(
            tinkoff_client,
            'get_token',
            mocker.Mock(return_value=dummy_coro(access_token)),
        )
        yield
        mock.return_value.close()

    @pytest.fixture
    def kwargs(self, merchant):
        return {
            'billing_descriptor': merchant.organization.english_name,
            'full_name': merchant.organization.full_name,
            'name': merchant.organization.name,
            'inn': merchant.organization.inn,
            'kpp': merchant.organization.kpp,
            'ogrn': int(merchant.organization.ogrn),
            'email': merchant.contact.email,
            'addresses': [
                {
                    'type': address.type,
                    'zip': address.zip,
                    'country': address.country,
                    'city': address.city,
                    'street': address.street + ', ' + address.home,
                }
                for address in merchant.addresses
            ],
            'ceo': {
                'first_name': merchant.ceo.name,
                'last_name': merchant.ceo.surname,
                'middle_name': merchant.ceo.patronymic,
                'birth_date': merchant.ceo.birth_date.isoformat(),
                'phone': merchant.ceo.phone,
            },
            'site_url': merchant.organization.site_url,
        }

    @pytest.fixture
    def check_json(self, payments_settings):
        def _inner(json, data):
            assert json == {
                **convert_to_camelcase(data),
                'terminalTypes': [
                    payments_settings.TINKOFF_TERMINAL_3DS,
                    payments_settings.TINKOFF_TERMINAL_NON_3DS,
                ],
                'mcc': payments_settings.TINKOFF_DEFAULT_MCC,
            }

        return _inner

    @pytest.mark.asyncio
    async def test_request(self, tinkoff_client, check_json, merchant, kwargs):
        await tinkoff_client.create_merchant(merchant)
        check_json(tinkoff_client.call_kwargs['json'], kwargs)

    @pytest.mark.asyncio
    async def test_without_patronymic(self, tinkoff_client, check_json, merchant, kwargs):
        merchant.ceo.patronymic = None
        kwargs['ceo'].pop('middle_name')
        await tinkoff_client.create_merchant(merchant)
        check_json(tinkoff_client.call_kwargs['json'], kwargs)

    @pytest.mark.asyncio
    async def test_without_address_home(self, tinkoff_client, check_json, merchant, kwargs):
        merchant.addresses[0].home = None
        kwargs['addresses'][0]['street'] = merchant.addresses[0].street
        await tinkoff_client.create_merchant(merchant)
        check_json(tinkoff_client.call_kwargs['json'], kwargs)

    @pytest.mark.asyncio
    async def test_request_site_url(self, payments_settings, check_json, tinkoff_client, merchant, kwargs):
        payments_settings.TINKOFF_DEFAULT_SITE_URL = 'xxx'
        merchant.organization.site_url = None
        await tinkoff_client.create_merchant(merchant)
        check_json(tinkoff_client.call_kwargs['json'], {**kwargs, 'site_url': 'xxx'})

    @pytest.mark.asyncio
    async def test_request_auth_header(self, tinkoff_client, merchant, access_token):
        await tinkoff_client.create_merchant(merchant)
        assert tinkoff_client.call_kwargs['headers']['Authorization'] == f'Bearer {access_token}'

    @pytest.mark.asyncio
    async def test_returns_submerchant_id(self, tinkoff_client, merchant, submerchant_id):
        assert await tinkoff_client.create_merchant(merchant) == str(submerchant_id)

    @pytest.mark.asyncio
    async def test_create_merchant_response_logged(self, tinkoff_client, merchant, pushers_mock, response_json):
        await tinkoff_client.create_merchant(merchant)

        expected_logged_request_kwargs = tinkoff_client.call_kwargs
        expected_logged_request_kwargs['headers'] = {'Authorization': ''}
        assert_that(
            pushers_mock.response_log.push.call_args[0][0],
            all_of(
                is_(InteractionResponseLog),
                has_properties(dict(
                    response=response_json,
                    request_url=tinkoff_client.call_args[2],
                    request_method=tinkoff_client.call_args[1],
                    request_kwargs=expected_logged_request_kwargs,
                    request_id=tinkoff_client.request_id,
                    status=200,
                ))
            ),
        )


class TestTinkoffClientErrors:
    @pytest.fixture
    def response_status(self):
        return 400

    @pytest.fixture
    def response_json(self, response_status):
        return {
            "timestamp": "2018-07-16T13:10:11.158+0000",
            "status": response_status,
            "error": "Bad Request",
            "message": " billingDescriptor[shopArticleId]\n . : 044583999; / :40702810601500000584",
            "path": "/register"
        }

    @pytest.fixture
    def response_data(self, response_json):
        return ujson.dumps(response_json).encode('utf-8')

    @pytest.mark.asyncio
    async def test_raises_error(self, tinkoff_client, response_json):
        with pytest.raises(TinkoffError) as error:
            await tinkoff_client.get_token()

        assert error.value.params == {}
        assert error.value.message == response_json['message']

    @pytest.mark.asyncio
    async def test_client_errors_logged(self, tinkoff_client, merchant, pushers_mock, response_data, response_status):
        with pytest.raises(TinkoffError) as error:
            await tinkoff_client.get_token()
        assert_that(
            pushers_mock.response_log.push.call_args[0][0],
            all_of(
                is_(InteractionResponseLog),
                has_properties(dict(
                    response=response_data,
                    request_url=tinkoff_client.call_args[2],
                    request_method=tinkoff_client.call_args[1],
                    request_kwargs=tinkoff_client.call_kwargs,
                    request_id=tinkoff_client.request_id,
                    status=response_status,
                    exception_type=type(error.value).__name__,
                ))
            ),
        )


class TestTinkoffClientValidationErrors:
    @pytest.fixture
    def response_status(self):
        return 400

    @pytest.fixture
    def errors(self):
        return [
            {
                "field": "billingDescriptor",
                "defaultMessage": " ",
                "rejectedValue": "",
                "code": "NotEmpty"
            },
            {
                "field": "serviceProviderEmail",
                "defaultMessage": "email ",
                "rejectedValue": "bademeil",
                "code": "Email"
            },
            {
                "field": "billingDescriptor",
                "defaultMessage": " \"[A-z0-9.\\-_ ]+\"",
                "rejectedValue": "",
                "code": "Pattern"
            },
            {
                "field": "billingDescriptor",
                "defaultMessage": " 1 14",
                "rejectedValue": "",
                "code": "Size"
            }
        ]

    @pytest.fixture
    def response_json(self, errors, response_status):
        return {
            "timestamp": "2018-07-25T13:23:18.160+0000",
            "status": response_status,
            "error": "Bad Request",
            "errors": errors,
            "message": "Validation failed for object='merchant'. Error count: 1",
            "path": "/register"
        }

    @pytest.fixture
    def response_data(self, response_json):
        return ujson.dumps(response_json).encode('utf-8')

    @pytest.mark.asyncio
    async def test_raises_error(self, tinkoff_client, errors):
        with pytest.raises(TinkoffError) as error:
            await tinkoff_client.get_token()

        assert error.value.params.getall('validation_error') == errors

    @pytest.mark.asyncio
    async def test_validation_logged(self, tinkoff_client, merchant, pushers_mock, response_data, response_status):
        with pytest.raises(TinkoffError) as error:
            await tinkoff_client.get_token()
        assert_that(
            pushers_mock.response_log.push.call_args[0][0],
            all_of(
                is_(InteractionResponseLog),
                has_properties(dict(
                    response=response_data,
                    request_url=tinkoff_client.call_args[2],
                    request_method=tinkoff_client.call_args[1],
                    request_kwargs=tinkoff_client.call_kwargs,
                    request_id=tinkoff_client.request_id,
                    status=response_status,
                    exception_type=type(error.value).__name__,
                ))
            ),
        )

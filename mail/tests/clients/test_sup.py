import re

import pytest
from aiohttp import TCPConnector
from aioresponses import CallbackResult

from sendr_interactions.clients.sup import AbstractSUPClient
from sendr_interactions.clients.sup.entities import Data, Notification, PushRequest, ThrottlePolicies
from sendr_interactions.exceptions import InteractionResponseError


class SUPClient(AbstractSUPClient):
    BASE_URL = 'http://sup.test'
    OAUTH_TOKEN = 'oauth-token'
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


@pytest.fixture
async def sup_client(create_interaction_client):
    client: SUPClient = create_interaction_client(SUPClient)
    yield client
    await client.close()


@pytest.fixture
def push_response():
    return {
        'data': {
            'push_id': 'some_test_push_id',
            'topic_push': 'assist_disaster_push',
        },
        'dry_run': False,
        'flags': {
            'individual': True,
            'processingMode': 'Direct',
            'useSolverQueue': True,
        },
        'id': 'some_test_id',
        'max_expected_receivers': 2147483647,
        'notification': {
            'body': 'Транзакция 123',
            'link': 'https://pay.yandex.ru',
            'title': 'Hello from Yandex.Pay',
        },
        'priority': 2419000,
        'project': 'yandexpay',
        'push_type': 'test-test',
        'receiver': [
            "tag: ( uuid == '0727030299d44637b666a31f6a2d04ea' ) AND ( ( ( geo_3 NOT IN ('113', '114') ) ) )"
        ],
        'requestTime': 1627479251490,
        'schedule_date_time': '2021-07-28T13:34:11.491',
        'schedule_now': True,
        'schedule_priority': 0,
        'schedule_timezone': 'Z',
        'sender_roles': {'send_test_push': 2147483647},
        'throttle_policies': {
            'DEVICE_ID': {
                'superapp_video_translation_content_id': {
                    'daily': 100,
                    'hourly': 50,
                    'hourly12': 75,
                    'weekly': 1000,
                }
            },
            'INSTALL_ID': {'default_install_id': {'daily': 7, 'hourly': 3}},
            'REQID': {'default_reqid': {'daily': 1}},
        },
        'topicPush': 'assist_disaster_push',
        'transport': 'Native',
        'ttl': 3600,
    }


@pytest.fixture
def push_response_bad_request():
    return {
        'timestamp': 1627484014894,
        'status': 400,
        'error': 'Bad Request',
        'errors': [
            {
                'codes': [
                    'NotBlank.pushRequest.project',
                    'NotBlank.project',
                    'NotBlank.java.lang.String',
                    'NotBlank',
                ],
                'arguments': [
                    {
                        'codes': ['pushRequest.project', 'project'],
                        'defaultMessage': 'project',
                        'code': 'project',
                    }
                ],
                'defaultMessage': 'must not be blank',
                'objectName': 'pushRequest',
                'field': 'project',
                'bindingFailure': False,
                'code': 'NotBlank',
            }
        ],
        'message': 'Bad Request',
        'path': '/pushes',
    }


@pytest.fixture
def registrations_response():
    return {
        'appId': 'ru.yandex.searchplugin.dev',
        'appVersion': '20.73',
        'hardwareId': 'c7a4fe8ae59a4fc7b58e4d76448a637b',
        'pushToken': 'xxx',
        'pushTokens': {
            'gcm': 'xxx'
        },
        'platform': 'android',
        'deviceName': 'Redmi Note 9 Pro',
        'notifyDisabled': False,
        'active': True,
        'updatedAt': 1627476391321,
        'deviceType': 'phone',
        'icookie': '1322384740042791798',
        'createDate': 1626876141.000000000,
        'installId': '0727030299d44637b666a31f6a2d04ea',
        'deviceId': 'cf8b3fd1ed53b575cf7d0cd4b3393bdd',
        'vendorDeviceId': 'c7a4fe8ae59a4fc7b58e4d76448a637b',
    }


@pytest.fixture
def registrations_not_found():
    return {
        'timestamp': 1627486775200,
        'status': 404,
        'error': 'Not Found',
        'message': 'Not Found',
        'path': '/v2/registrations/test',
    }


@pytest.mark.asyncio
async def test_send_push_normal(sup_client: SUPClient, aioresponses_mocker, push_response):
    def callback(url, **kwargs):
        request = kwargs['json']
        assert request['receiver'] == ['uuid:1234567']
        assert request['notification']['title'] == 'World'
        assert request['notification']['body'] == 'Hello'
        assert request['project'] == 'yandexpay'
        assert request['data']['topic_push'] == 'some_topic'
        assert request['throttle_policies']['device_id'] == 'some_policy'

        return CallbackResult(
            payload=push_response,
            status=200
        )

    aioresponses_mocker.post(
        f'{sup_client.BASE_URL}/pushes?dry_run=0',
        callback=callback,
    )
    response = await sup_client.send_push(
        PushRequest(
            receiver=['uuid:1234567'],
            notification=Notification(body='Hello', title='World'),
            project='yandexpay',
            schedule='now',
            data=Data(topic_push='some_topic'),
            throttle_policies=ThrottlePolicies(
                device_id='some_policy',
            ),
        )
    )
    assert response.id == push_response['id']
    assert response.data['push_id'] == push_response['data']['push_id']
    assert len(response.receiver) == 1
    assert response.request_time == push_response['requestTime']


@pytest.mark.asyncio
async def test_send_push_error(sup_client: SUPClient, aioresponses_mocker, push_response_bad_request):
    def callback(url, **kwargs):
        request = kwargs['json']
        assert request['receiver'] == ['uuid:1234567']
        assert request['data']['push_id'] == 'some_id'
        assert request['notification']['title'] == 'hello'

        return CallbackResult(
            payload=push_response_bad_request,
            status=400
        )

    aioresponses_mocker.post(
        f'{sup_client.BASE_URL}/pushes?dry_run=0',
        callback=callback,
    )

    with pytest.raises(InteractionResponseError) as e:
        await sup_client.send_push(
            PushRequest(
                receiver=['uuid:1234567'],
                data=Data(push_id='some_id'),
                notification=Notification(title='hello'),
            )
        )
    exc_value = e.value
    assert exc_value.service == 'sup'
    assert exc_value.status_code == 400
    assert exc_value.params['status'] == 400
    assert exc_value.params['error'] == 'Bad Request'


@pytest.mark.asyncio
async def test_send_installation_normal(sup_client: SUPClient, aioresponses_mocker, registrations_response):
    aioresponses_mocker.get(
        re.compile(f'^{sup_client.BASE_URL}/registrations/some_installation_id/$'),
        payload=registrations_response,
    )
    response = await sup_client.get_installation('some_installation_id')

    assert response.app_id == registrations_response['appId']
    assert response.hardware_id == registrations_response['hardwareId']
    assert response.push_token == registrations_response['pushToken']
    assert response.updated_at == registrations_response['updatedAt']


@pytest.mark.asyncio
async def test_send_installation_error(sup_client: SUPClient, aioresponses_mocker, registrations_not_found):
    aioresponses_mocker.get(
        re.compile(f'^{sup_client.BASE_URL}/registrations/some_installation_id/$'),
        payload=registrations_not_found,
        status=404,
    )

    with pytest.raises(InteractionResponseError) as e:
        await sup_client.get_installation('some_installation_id')

    exc_value = e.value
    assert exc_value.service == 'sup'
    assert exc_value.status_code == 404
    assert exc_value.params['status'] == 404
    assert exc_value.params['error'] == 'Not Found'


@pytest.mark.asyncio
async def test_can_post_registration_with_expected_body(sup_client: SUPClient, aioresponses_mocker):
    registration_mock = aioresponses_mocker.post(
        re.compile(f'^{sup_client.BASE_URL}/v2/registrations$'),
        status=200,
        headers={'Content-Type': 'text/html; charset=utf-8'},
    )

    await sup_client.register_installation(
        app_id='some_app_id',
        app_version='some_app_version',
        hardware_id='some_hardware_id',
        push_token='some_push_token',
        platform='some_platform',
        device_name='some_device_name',
        zone_id='some_zone_id',
        notify_disabled=True,
        active=True,
        install_id='some_install_id',
        device_id='some_device_id',
        vendor_device_id='some_vendor_device_id',
        is_huawei=False,
    )

    _, call_kwargs = registration_mock.call_args
    assert call_kwargs['json'] == {
        'appId': 'some_app_id',
        'appVersion': 'some_app_version',
        'hardwareId': 'some_hardware_id',
        'pushToken': 'some_push_token',
        'platform': 'some_platform',
        'deviceName': 'some_device_name',
        'zoneId': 'some_zone_id',
        'notifyDisabled': True,
        'active': True,
        'installId': 'some_install_id',
        'deviceId': 'some_device_id',
        'vendorDeviceId': 'some_vendor_device_id',
    }


@pytest.mark.asyncio
async def test_can_post_registration_with_expected_body_for_huawei(sup_client: SUPClient, aioresponses_mocker):
    registration_mock = aioresponses_mocker.post(
        re.compile(f'^{sup_client.BASE_URL}/v2/registrations$'),
        status=200,
        headers={'Content-Type': 'text/html; charset=utf-8'},
    )

    await sup_client.register_installation(
        app_id='some_app_id',
        app_version='some_app_version',
        hardware_id='some_hardware_id',
        push_token='some_push_token',
        platform='some_platform',
        device_name='some_device_name',
        zone_id='some_zone_id',
        notify_disabled=True,
        active=True,
        install_id='some_install_id',
        device_id='some_device_id',
        vendor_device_id='some_vendor_device_id',
        is_huawei=True,
    )

    _, call_kwargs = registration_mock.call_args
    assert call_kwargs['json'] == {
        'appId': 'some_app_id',
        'appVersion': 'some_app_version',
        'hardwareId': 'some_hardware_id',
        'pushTokens': {
            'hms': 'some_push_token'
        },
        'platform': 'some_platform',
        'deviceName': 'some_device_name',
        'zoneId': 'some_zone_id',
        'notifyDisabled': True,
        'active': True,
        'installId': 'some_install_id',
        'deviceId': 'some_device_id',
        'vendorDeviceId': 'some_vendor_device_id',
    }


@pytest.mark.asyncio
async def test_should_raise_on_409(sup_client: SUPClient, aioresponses_mocker):
    aioresponses_mocker.post(
        re.compile(f'^{sup_client.BASE_URL}/v2/registrations$'),
        status=409,
    )

    with pytest.raises(InteractionResponseError) as e:
        await sup_client.register_installation(
            app_id='',
            app_version='',
            hardware_id='',
            push_token='',
            platform='',
            device_name='',
            zone_id='',
            notify_disabled=True,
            active=True,
            install_id='',
            device_id='',
            vendor_device_id='',
            is_huawei=False,
        )

    assert e.value.status_code == 409

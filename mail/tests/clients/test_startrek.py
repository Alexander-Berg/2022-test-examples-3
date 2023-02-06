import json
import re

import pytest
from aiohttp import TCPConnector
from aioresponses import CallbackResult

from sendr_interactions.clients.startrek import AbstractStartrekClient
from sendr_interactions.clients.startrek.entities import Issue, IssueResolution, IssueStatus, Link
from sendr_interactions.clients.startrek.exceptions import ForbiddenStartrekError, NotFoundStartrekError


class StartrekClient(AbstractStartrekClient):
    BASE_URL = 'http://startrek.test'
    OAUTH_TOKEN = 'oauth-token'
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


@pytest.fixture
def client(create_interaction_client):
    return create_interaction_client(StartrekClient)


class TestCreateIssue:
    @pytest.mark.asyncio
    async def test_params(self, client, aioresponses_mocker, issue_resp):
        calls = []

        async def callback(url, **kwargs):
            calls.append({'url': url, 'json': kwargs.get('json', {}), 'headers': kwargs.get('headers', {})})
            return CallbackResult(status=200, payload=issue_resp)

        aioresponses_mocker.post(re.compile(r'.*/v2/issues[\?]?.*'), callback=callback)

        await client.create_issue(
            queue='FOOBAR',
            summary='The ticket',
            unique='u-ni-qu-e',
            description='desc',
            parent='PARENT-1',
            attachment_ids=[1, 2, 3],
            tags=['taghey', 'tagho'],
            links=[Link(relationship='relates', issue='ABC-123')],
        )

        assert client.session.headers['Authorization'] == 'OAuth oauth-token'

        assert len(calls) == 1
        assert calls[0]['json'] == {
            'queue': 'FOOBAR',
            'summary': 'The ticket',
            'description': 'desc',
            'links': [{'relationship': 'relates', 'issue': 'ABC-123'}],
            'unique': 'u-ni-qu-e',
            'attachmentIds': [1, 2, 3],
            'parent': 'PARENT-1',
            'tags': ['taghey', 'tagho'],
        }

    @pytest.mark.asyncio
    async def test_result(self, client, aioresponses_mocker, issue_resp, expected_issue):
        aioresponses_mocker.post(re.compile(r'.*/v2/issues[\?]?.*'), status=200, payload=issue_resp)

        result = await client.create_issue(
            queue='FOOBAR',
            summary='The ticket',
        )

        assert result == expected_issue

    @pytest.mark.asyncio
    async def test_error(self, client, aioresponses_mocker, resp_err):
        # Не смущайтесь, что текст ошибки не соответствует коду
        aioresponses_mocker.post(re.compile(r'.*/v2/issues[\?]?.*'), status=403, payload=resp_err)

        with pytest.raises(ForbiddenStartrekError) as exc_info:
            await client.create_issue(
                queue='FOOBAR',
                summary='The ticket',
            )

        assert exc_info.value.HTTP_STATUS_CODE == 403
        assert exc_info.value.message == json.dumps(['Users [vasya, petya, kolya] do not exist'])

    @pytest.fixture
    def resp_err(self):
        return {
            'errors': {
                'originalEstimation': 'Months and years vary in length'
            },
            'errorMessages': [
                'Users [vasya, petya, kolya] do not exist'
            ],
            'statusCode': 422,
        }


class TestUpdateIssue:
    @pytest.mark.asyncio
    async def test_params(self, client, aioresponses_mocker, issue_resp):
        calls = []

        async def callback(url, **kwargs):
            calls.append({'url': url, 'json': kwargs.get('json', {}), 'headers': kwargs.get('headers', {})})
            return CallbackResult(status=200, payload=issue_resp)

        aioresponses_mocker.patch(re.compile(r'.*/v2/issues[\?]?.*'), callback=callback)

        await client.update_issue(
            issue_id='TICKET-1',
            summary='The ticket',
            description='desc',
        )

        assert client.session.headers['Authorization'] == 'OAuth oauth-token'

        assert len(calls) == 1
        assert calls[0]['json'] == {
            'summary': 'The ticket',
            'description': 'desc',
        }

    @pytest.mark.asyncio
    async def test_result(self, client, aioresponses_mocker, issue_resp, expected_issue):
        aioresponses_mocker.patch(re.compile(r'.*/v2/issues[\?]?.*'), status=200, payload=issue_resp)

        result = await client.update_issue(
            issue_id='TICKET-1',
            summary='The ticket',
            description='desc',
        )

        assert result == expected_issue


class TestExecuteIssueTransition:
    @pytest.mark.asyncio
    async def test_params(self, client, aioresponses_mocker, issue_resp):
        calls = []

        async def callback(url, **kwargs):
            calls.append({'url': url, 'json': kwargs.get('json', {}), 'headers': kwargs.get('headers', {})})
            return CallbackResult(status=200, payload={})

        aioresponses_mocker.post(
            re.compile(r'.*/v2/issues/TICKET-1/transitions/transition_id/_execute[\?]?.*'),
            callback=callback,
        )

        await client.execute_issue_transition(
            issue_id='TICKET-1',
            transition_id='transition_id',
            resolution_key='resolution_key',
        )

        assert client.session.headers['Authorization'] == 'OAuth oauth-token'

        assert len(calls) == 1
        assert calls[0]['json'] == {
            'resolution': 'resolution_key',
        }

    @pytest.mark.asyncio
    async def test_optional_params(self, client, aioresponses_mocker, issue_resp):
        calls = []

        async def callback(url, **kwargs):
            calls.append({'url': url, 'json': kwargs.get('json', {}), 'headers': kwargs.get('headers', {})})
            return CallbackResult(status=200, payload={})

        aioresponses_mocker.post(
            re.compile(r'.*/v2/issues/TICKET-1/transitions/transition_id/_execute[\?]?.*'),
            callback=callback,
        )

        await client.execute_issue_transition(
            issue_id='TICKET-1',
            transition_id='transition_id',
        )

        assert client.session.headers['Authorization'] == 'OAuth oauth-token'

        assert len(calls) == 1
        assert calls[0]['json'] == {}

    @pytest.mark.asyncio
    async def test_result(self, client, aioresponses_mocker, issue_resp):
        aioresponses_mocker.post(re.compile(r'.*/v2/issues[\?]?.*'), status=200, payload=issue_resp)

        result = await client.execute_issue_transition(
            issue_id='TICKET-1',
            transition_id='transition_id',
        )

        assert result is None


class TestGetIssueInfo:
    @pytest.mark.asyncio
    async def test_get_issue_without_resolution(
        self, client, aioresponses_mocker, issue_resp, expected_issue
    ):
        aioresponses_mocker.get(
            re.compile(r'.*/v2/issues/TICKET-1'),
            status=200,
            payload=issue_resp,
        )

        result = await client.get_issue_info(issue_id='TICKET-1')

        assert result == expected_issue

    @pytest.mark.asyncio
    async def test_get_issue_with_resolution(
        self, client, aioresponses_mocker, issue_resp, expected_issue
    ):
        issue_resp['resolution'] = {
            'key': 'fixed',
            'display': 'Решен',
            'id': '1',
            'self': 'https://st-api.yandex-team.ru/v2/resolutions/1',
        }
        expected_issue.resolution = IssueResolution(id='1', key='fixed')
        aioresponses_mocker.get(
            re.compile(r'.*/v2/issues/TICKET-1'),
            status=200,
            payload=issue_resp,
        )

        result = await client.get_issue_info(issue_id='TICKET-1')

        assert result == expected_issue

    @pytest.mark.asyncio
    async def test_error(self, client, aioresponses_mocker):
        resp_err = {
            'errors': {},
            'errorMessages': ['Issue does not exist'],
            'statusCode': 404,
        }
        aioresponses_mocker.get(
            re.compile(r'.*/v2/issues/TICKET-1'),
            status=404,
            payload=resp_err,
        )

        with pytest.raises(NotFoundStartrekError) as exc_info:
            await client.get_issue_info(issue_id='TICKET-1')

        assert exc_info.value.HTTP_STATUS_CODE == 404
        assert exc_info.value.message == json.dumps(['Issue does not exist'])


@pytest.fixture
def issue_resp():
    return {
        'self': 'https://st-api.test.yandex-team.ru/v2/issues/YANDEXPAYTEST-9',
        'id': '60aa970515e1fb16e8050f45',
        'key': 'YANDEXPAYTEST-9',
        'version': 1,
        'summary': 'Test Issue',
        'statusStartTime': '2021-05-23T17:55:17.884+0000',
        'updatedBy': {
            'self': 'https://st-api.test.yandex-team.ru/v2/users/1120000000148382',
            'id': 'hmnid',
            'display': 'Дмитрий Парамошкин'
        },
        'type': {
            'self': 'https://st-api.test.yandex-team.ru/v2/issuetypes/2',
            'id': '2',
            'key': 'task',
            'display': 'Задача'
        },
        'priority': {
            'self': 'https://st-api.test.yandex-team.ru/v2/priorities/2',
            'id': '2',
            'key': 'normal',
            'display': 'Средний'
        },
        'createdAt': '2021-05-23T17:55:17.834+0000',
        'createdBy': {
            'self': 'https://st-api.test.yandex-team.ru/v2/users/1120000000148382',
            'id': 'hmnid',
            'display': 'Дмитрий Парамошкин'
        },
        'commentWithoutExternalMessageCount': 0,
        'unique': 'foobarbaz3-hmnid',
        'votes': 0,
        'commentWithExternalMessageCount': 0,
        'queue': {
            'self': 'https://st-api.test.yandex-team.ru/v2/queues/YANDEXPAYTEST',
            'id': '4941',
            'key': 'YANDEXPAYTEST',
            'display': 'YANDEXPAY-TEST'
        },
        'updatedAt': '2021-05-23T17:55:17.834+0000',
        'status': {
            'self': 'https://st-api.test.yandex-team.ru/v2/statuses/1',
            'id': '1',
            'key': 'open',
            'display': 'Открыт'
        },
        'favorite': False
    }


@pytest.fixture
def expected_issue():
    return Issue(
        id='60aa970515e1fb16e8050f45',
        key='YANDEXPAYTEST-9',
        status=IssueStatus(id='1', key='open'),
    )

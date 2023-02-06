import asyncio
import io
import json
import logging

import pytest
import yarl
from aiohttp import ClientConnectionError, ClientResponse, ClientSession
from aiohttp import client as aiohttp_client
from multidict import CIMultiDict

import sendr_interactions
from sendr_interactions import ResponseFormat, retry_budget
from sendr_qlog import LoggerContext


class BaseInteractionClient(sendr_interactions.AbstractInteractionClient):
    DEBUG = False
    CONNECTOR = None
    REQUEST_RETRY_TIMEOUTS = ()
    TVM_CONFIG = 'test-tvm-config'

    SERVICE = 'test-service'
    BASE_URL = 'test-base-url'


@pytest.fixture
def logger(mocker):
    BaseInteractionClient.CONNECTOR = mocker.Mock()
    return LoggerContext(logging.getLogger(), {})


@pytest.fixture
def pushers():
    from sendr_writers.base.pusher import BaseLog, CommonPushers, Pusher

    class StreamPusher(Pusher):
        def __init__(self):
            self.stream = io.StringIO()
            self.calls = []

        async def push(self, log: BaseLog) -> None:
            data = log.dump()
            self.stream.write(json.dumps(data, default=str))
            self.stream.write('\n')
            self.calls.append(log)

    class Pushers(CommonPushers):
        def __init__(self):
            self.response_log = StreamPusher()

    pushers = Pushers()
    pushers.response_log = StreamPusher()
    return pushers


@pytest.fixture
def client(logger, pushers):
    assert pushers.response_log is not None
    return BaseInteractionClient(logger, '', pushers=pushers)


class Response:
    status = 200

    @property
    def headers(self):
        return CIMultiDict()

    async def text(self):
        return 'text-response'

    async def read(self):
        return b'binary-response'

    async def json(self):
        return {'json': 'response'}

    def __eq__(self, other):
        return isinstance(other, Response)


class TestBaseInteractionClient:
    @pytest.fixture
    def response(self):
        return Response()

    class TestGetSessionCls:
        def test_no_tvm(self, logger):
            class A(BaseInteractionClient):
                TVM_ID = None

            client = A(logger, '')
            assert client._get_session_cls() is ClientSession

        def test_get_session_class_returns_tvm_session(self, logger):
            class Session:
                pass

            class A(BaseInteractionClient):
                TVM_ID = 123
                TVM_SESSION_CLS = Session

            client = A(logger, '')
            assert client._get_session_cls() is Session

        def test_instantiates_tvm_session_if_tvm_id_set(self, logger):
            """Если на клиенте установлен tvm id, то для создания сессии используется класс твм сессий."""

            class Session:
                def __init__(self, *args, **kwargs):
                    pass

            class A(BaseInteractionClient):
                TVM_ID = 123
                TVM_SESSION_CLS = Session

            client = A(logger, '')
            assert isinstance(client.session, Session)

        def test_tvm_session_instantiated_once(self, logger):
            """
            Используя класс твм сессий клиент создает экземпляр сессии
            единожды за время своего существования.
            """

            class Session:
                counter = 0

                def __init__(self, *args, **kwargs):
                    self.count()

                @classmethod
                def count(cls):
                    cls.counter += 1

            Session()
            Session()
            assert Session.counter == 2
            Session.counter = 0

            class A(BaseInteractionClient):
                TVM_ID = 123
                TVM_SESSION_CLS = Session

            client = A(logger, '')
            client.session
            client.session
            assert Session.counter == 1

        def test_no_tvm_session_set(self, logger):
            """RuntimeError, если на клиенте есть твм id, но не предоставлен класс твм сессий."""

            class A(BaseInteractionClient):
                TVM_ID = 123
                TVM_SESSION_CLS = None

            with pytest.raises(RuntimeError):
                A(logger=logger, request_id='')._get_session_cls()

    class TestGetSessionKwargs:
        def test_no_tvm(self, logger):
            class A(BaseInteractionClient):
                TVM_ID = None

            client = A(logger, '')
            assert client._get_session_kwargs() == {
                'connector': A.CONNECTOR,
                'connector_owner': False,
                'trust_env': True,
            }

        def test_tvm(self, logger):
            class A(BaseInteractionClient):
                TVM_ID = 123

            client = A(logger, '')
            assert client._get_session_kwargs() == {
                'connector': A.CONNECTOR,
                'connector_owner': False,
                'tvm_dst': A.TVM_ID,
                'trust_env': True,
            }

    class TestSession:
        @pytest.fixture
        def session_cls(self, mocker):
            return mocker.Mock(return_value='session-instance')

        @pytest.fixture
        def session_kwargs(self):
            return {'key': 'value'}

        @pytest.fixture(autouse=True)
        def get_cls_mock(self, mocker, client, session_cls):
            mocker.patch.object(type(client), '_get_session_cls', mocker.Mock(return_value=session_cls))
            mocker.spy(client, '_get_session_cls')

        @pytest.fixture(autouse=True)
        def get_kwargs_mock(self, mocker, client, session_kwargs):
            mocker.patch.object(type(client), '_get_session_kwargs', mocker.Mock(return_value=session_kwargs))
            mocker.spy(client, '_get_session_kwargs')

        def test_session(self, client):
            assert client.session == 'session-instance'

        def test_session_cls_call(self, client, session_cls, session_kwargs):
            client.session
            session_cls.assert_called_once_with(**session_kwargs)

        def test_reuses_session(self, client):
            client.session
            client.session
            client._get_session_cls.assert_called_once()

    @pytest.mark.asyncio
    class TestMakeRequest:
        @pytest.fixture
        def url(self):
            return "http://127.0.0.1"

        @pytest.fixture
        def response(self, url, mocker):
            url = yarl.URL(url)
            response = aiohttp_client.ClientResponse(
                mocker.MagicMock(),
                url,
                writer=mocker.MagicMock(),
                continue100=mocker.MagicMock(),
                timer=mocker.MagicMock(),
                request_info=aiohttp_client.RequestInfo(
                    url,
                    mocker.MagicMock(),
                    mocker.MagicMock(),
                    mocker.MagicMock(),
                ),
                traces=mocker.MagicMock(),
                loop=mocker.MagicMock(),
                session=mocker.MagicMock(),
            )
            response.status = 200
            mocker.patch.object(response, '_headers', {})
            return response

        @pytest.fixture
        def request_mock(self, response):
            async def request(*args, **kwargs):
                return response

            return request

        @pytest.fixture(autouse=True)
        def session_mock(self, mocker, client, request_mock):
            mock = mocker.Mock()
            mock.request = mocker.Mock(side_effect=request_mock)
            mocker.patch.object(type(client), 'session', mock)

        async def test_calls_request(self, client):
            await client._make_request('a', 'b', 'c', other='value')
            client.session.request.assert_called_once_with('b', 'c', other='value', headers={})

        async def test_returns_response(self, client, response):
            assert await client._make_request('a', 'b', 'c') is response

        @pytest.mark.asyncio
        class TestRetries:
            class A(BaseInteractionClient):
                REQUEST_RETRY_TIMEOUTS = (0.1, 0.1, 0.1)
                RTT_ESTIMATE = 0.01

            @pytest.fixture
            def client(self, logger):
                return self.A(logger, '', retry_budget=retry_budget.RetryBudget())

            @pytest.fixture
            def fail_count(self):
                return 2

            @pytest.fixture
            def request_mock(self, response, fail_count):
                call_count = 0

                async def request(*args, **kwargs):
                    nonlocal call_count
                    if call_count < fail_count:
                        call_count += 1
                        raise ClientConnectionError
                    return response

                return request

            async def test_retries(self, client):
                await client._make_request('a', 'b', 'c')
                assert client.session.request.call_count == 3

            async def test_returns_response(self, client, response):
                assert await client._make_request('a', 'b', 'c') is response

            @pytest.mark.parametrize('fail_count', (4,))
            async def test_fails_after_retry_number_exceeded(self, client):
                with pytest.raises(ClientConnectionError):
                    await client._make_request('a', 'b', 'c')

            @pytest.mark.parametrize('fail_count', (7,))
            async def test_retry_budget(self, url: str, client):
                # 4 fails, use retry
                with pytest.raises(ClientConnectionError):
                    await client._make_request('a', 'b', url)

                assert client.retry_budget.can_retry(client.SERVICE)
                assert client.session.request.call_count == 4

                # 2 fails, budget is spent, 2 retries skipped
                with pytest.raises(ClientConnectionError):
                    await client._make_request('a', 'b', url)

                assert not client.retry_budget.can_retry(client.SERVICE)
                assert client.session.request.call_count == 6

                # 1 fails — without retry, budget is spent
                with pytest.raises(ClientConnectionError):
                    await client._make_request('a', 'b', url)

                assert not client.retry_budget.can_retry(client.SERVICE)
                assert client.session.request.call_count == 7

                # success
                await client._make_request('a', 'b', url)

                assert not client.retry_budget.can_retry(client.SERVICE)
                assert client.session.request.call_count == 8

                # deposit
                for _ in range(20):
                    await client._make_request('a', 'b', url)

                assert client.retry_budget.can_retry(client.SERVICE)
                assert client.session.request.call_count == 28

            async def test_request_deadline_timeout_ok(self, client, mocker):
                deadline = sendr_interactions.deadline.Deadline(0.3)

                await client._make_request('a', 'b', 'c', deadline=deadline)
                assert client.session.request.call_count == 3

            async def test_request_deadline_timeout_before_first_request(self, client):
                deadline = sendr_interactions.deadline.Deadline(0.)
                await asyncio.sleep(0.1)

                with pytest.raises(asyncio.TimeoutError):
                    await client._make_request('a', 'b', 'c', deadline=deadline)

                client.session.request.assert_not_called()

            async def test_request_deadline_timeout_long_first_request(self, client, mocker):
                deadline = sendr_interactions.deadline.Deadline(1)

                def seconds_to():
                    return 0.5 if client.session.request.call_count == 0 else -0.5

                with pytest.raises(asyncio.TimeoutError):
                    mocker.patch.object(deadline, 'seconds_to', side_effect=seconds_to)
                    await client._make_request('a', 'b', 'c', deadline=deadline)

                assert client.session.request.call_count == 1

            async def test_request_deadline_x_request_timeout_header(self, client, mocker):
                deadline = sendr_interactions.deadline.Deadline(1)
                mocker.patch.object(deadline, 'seconds_to', side_effect=lambda: self.A.RTT_ESTIMATE / 2)
                await client._make_request('a', 'b', 'c', deadline=deadline)

                assert client.session.request.call_args.kwargs['headers']['X-Request-Timeout'] == '0'

                deadline = sendr_interactions.deadline.Deadline(1)
                mocker.patch.object(deadline, 'seconds_to', side_effect=lambda: 1)
                await client._make_request('a', 'b', 'c', deadline=deadline)

                assert client.session.request.call_args.kwargs['headers']['X-Request-Timeout'] == '990'

        @pytest.mark.asyncio
        class TestFailedReasonOverriding:
            class A(BaseInteractionClient):
                REQUEST_RETRY_TIMEOUTS = (0.1,)

                async def _is_failed(
                    self,
                    response: ClientResponse,
                ) -> bool:
                    return True

            @pytest.fixture
            def client(self, logger):
                return self.A(logger, '')

            async def test_can_override_failed_reason_and_retry(self, client):
                await client._make_request('a', 'b', 'c')
                assert client.session.request.call_count == 2

    @pytest.mark.asyncio
    class TestRequest:
        @pytest.fixture
        def processed(self):
            return 'test-request-processed'

        @pytest.fixture(autouse=True)
        def make_request_mock(self, mocker, client, response):
            async def make_request(*args, **kwargs):
                return response

            mocker.patch.object(client, '_make_request', mocker.Mock(side_effect=make_request))

        @pytest.fixture(autouse=True)
        def process_response_mock(self, mocker, client, processed):
            async def process_response(*args, **kwargs):
                return processed

            mocker.patch.object(client, '_process_response', mocker.Mock(side_effect=process_response))

        async def test_calls_make_request(self, client):
            await client._request('a', 'b', 'c', other='value')
            client._make_request.assert_called_once_with('a', 'b', 'c', other='value')

        async def test_calls_process_response(self, client, response):
            await client._request('a', 'b', 'c', other='value')
            client._process_response.assert_called_once_with(response, 'a')

        async def test_returns_processed(self, client, processed):
            assert await client._request('a', 'b', 'c', other='value') == processed

        async def test_calls_response_log_pushers(self, client, response):
            await client._request('a', 'b', 'c', other='value')
            assert len(client.pushers.response_log.calls) == 1

        async def test_no_calls_response_log_pushers(self, client, response):
            await client._request('a', 'b', 'c', other='value', response_log=False)
            assert len(client.pushers.response_log.calls) == 0

    class TestFormatRequest:
        @pytest.fixture(autouse=True)
        def make_request_mock(self, mocker, client, response):
            mocker.patch.object(client, '_make_request', mocker.AsyncMock(return_value=response))

        @pytest.mark.asyncio
        @pytest.mark.parametrize(
            'response_format,expected_response',
            [
                (ResponseFormat.JSON, {'json': 'response'}),
                (ResponseFormat.TEXT, 'text-response'),
                (ResponseFormat.BYTES, b'binary-response'),
                (ResponseFormat.ORIGINAL, Response()),
            ]
        )
        async def test_format_response(self, client, response_format, expected_response):
            client.RESPONSE_FORMAT = response_format

            actual_response = await client._request('a', 'b', 'c')

            assert actual_response == expected_response

    class TestLogScrubber:
        @pytest.fixture
        def client(self, logger):
            BaseInteractionClient.LOGGING_EXPOSED_HEADERS = ('exposed-header-1', 'exposed-header-2')
            BaseInteractionClient.LOGGING_SENSITIVE_FIELDS = ('sensitive-field-1', 'sensitive-field-2')
            return BaseInteractionClient(logger, '')

        def test_scrubber_filters_sensitive_fields(self, client):
            filtered_kwargs = client._scrub_logging_request_kwargs({
                'params': {
                    'sensitive-field-1': 'secret-value-1',
                    'sensitive-field-2': 'secret-value-2',
                    'regular-field': 'value',
                },
            })
            assert filtered_kwargs == {'params': {'regular-field': 'value'}}

        def test_scrubber_exposes_exposed_headers(self, client):
            filtered_kwargs = client._scrub_logging_request_kwargs({
                'headers': {
                    'exposed-header-1': 'exposed-value-1',
                    'x-ya-service-ticket': '3:4:5',
                    'x-org-id': '123123',
                },
            })

            expected = {
                'headers': {
                    'exposed-header-1': 'exposed-value-1',
                    'x-ya-service-ticket': '',
                    'x-org-id': '',
                }
            }
            assert expected == filtered_kwargs

    @pytest.mark.parametrize('string', ('123', 'hello%3Aworld', 'card-x01234567890abcde'))
    def test_assert_urlsafe_string__success(self, string):
        BaseInteractionClient.assert_string_urlsafe_for_path(string)

    @pytest.mark.parametrize('string', ('ab/c', '..'))
    def test_assert_urlsafe_string__fail(self, string):
        with pytest.raises(AssertionError):
            BaseInteractionClient.assert_string_urlsafe_for_path(string)

    def test_assert_urlsafe_string__whitelist_success(self):
        BaseInteractionClient.assert_string_urlsafe_for_path('1234567890', '1234567890')

    def test_assert_urlsafe_string__whitelist_fail(self):
        with pytest.raises(AssertionError):
            BaseInteractionClient.assert_string_urlsafe_for_path('1234567890', '12345')

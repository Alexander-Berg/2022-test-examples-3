import logging
from concurrent.futures import Future

import kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api as pqlib
import pytest
from kikimr.public.sdk.python.persqueue.errors import SessionFailureResult

from sendr_logbroker import Consumer, Logbroker, LogbrokerException, Producer

PQStreamingAPI = 'kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api.PQStreamingAPI'


@pytest.fixture()
def logbroker_api_mock(mocker):
    def _api(consumer=None, producer=None):
        api_mock = mocker.Mock(pqlib.PQStreamingAPI)
        api_mock.create_consumer = mocker.Mock(
            return_value=consumer
        )
        api_mock.create_producer = mocker.Mock(
            return_value=producer
        )
        api_mock.create_retrying_producer = mocker.Mock(
            return_value=producer
        )
        return api_mock
    return _api


@pytest.fixture()
def logbroker_mock(mocker):
    def _lb(api):
        lb_mock = mocker.AsyncMock(Logbroker)
        lb_mock.__aenter__ = mocker.AsyncMock(
            return_value=api
        )
        return lb_mock
    return _lb


@pytest.fixture()
def logbroker_consumer_mock(mocker):
    def _consumer(messages='', cookie=42, start=True, max_size=1):
        messages = [messages] if isinstance(messages, str) else messages
        response_mock = mocker.Mock(spec=['type', 'message'])
        response_mock.type = pqlib.ConsumerMessageType.MSG_DATA
        response_mock.message = mocker.Mock(spec=['data'])
        batch = [mocker.Mock(spec=['message'])]
        start_idx = 0

        def future_event():
            nonlocal start_idx
            batch[0].message = []
            for msg in messages[start_idx:start_idx + max_size]:
                message = mocker.Mock(spec=['data'])
                message.data = msg
                batch[0].message.append(message)
                start_idx += 1

            response_mock.message.data.message_batch = batch
            response_mock.message.data.cookie = cookie
            future_event = Future()
            future_event.set_result(response_mock)
            return future_event

        future_start = Future()
        if start:
            future_start.set_result(True)
        else:
            future_start.set_result(SessionFailureResult("some reason"))
        consumer_mock = mocker.Mock(pqlib.PQStreamingConsumer)
        consumer_mock.start = mocker.Mock(return_value=future_start)
        consumer_mock.next_event = mocker.Mock(side_effect=future_event)
        consumer_mock.commit = mocker.Mock()

        return consumer_mock
    return _consumer


@pytest.fixture()
def logbroker_producer_mock(mocker):
    def _producer(write_callback=None, start=True):
        future_start = Future()
        future_result = mocker.Mock()
        future_result.init.max_seq_no = 0
        if start:
            future_start.set_result(future_result)
        else:
            future_start.set_result(SessionFailureResult("some reason"))

        def write(seq_no, data):
            future_write = Future()
            result = mocker.Mock()
            future_write.set_result(result)
            if write_callback:
                write_callback(seq_no, data)
            return future_write

        producer_mock = mocker.Mock(pqlib.PQStreamingProducer)
        producer_mock.start = mocker.Mock(return_value=future_start)
        producer_mock.write = write

        return producer_mock
    return _producer


@pytest.mark.asyncio
async def test_logbroker_client_success_start(mocker):
    future = Future()
    future.set_result(True)
    MockPQStreamingAPI = mocker.patch(PQStreamingAPI)
    instance = MockPQStreamingAPI.return_value
    instance.start.return_value = future

    async with Logbroker('localhost', 1234):
        pass

    assert instance.start.call_count == 1
    assert instance.stop.call_count == 1


@pytest.mark.asyncio
async def test_logbroker_client_fail_start(mocker):
    future = Future()
    future.set_result(False)
    MockPQStreamingAPI = mocker.patch(PQStreamingAPI)
    instance = MockPQStreamingAPI.return_value
    instance.start.return_value = future

    with pytest.raises(LogbrokerException):
        async with Logbroker('localhost', 1234):
            pass

    assert instance.start.call_count == 1
    assert instance.stop.call_count == 0


@pytest.mark.asyncio
async def test_logbroker_client_correct_close(mocker):
    future = Future()
    future.set_result(True)
    MockPQStreamingAPI = mocker.patch(PQStreamingAPI)
    instance = MockPQStreamingAPI.return_value
    instance.start.return_value = future

    with pytest.raises(Exception):
        async with Logbroker('localhost', 1234):
            raise Exception('fail')

    assert instance.start.call_count == 1
    assert instance.stop.call_count == 1


@pytest.mark.asyncio
async def test_consumer_read_messages(mocker, logbroker_api_mock, logbroker_consumer_mock):
    lb_consumer_mock = logbroker_consumer_mock(messages='test', cookie=42)
    consumer = Consumer(
        logbroker_api_mock(
            consumer=lb_consumer_mock
        ),
        mocker.Mock(),
        consumer='some/consumer',
        topic='some/topic',
        logger=logging.getLogger()
    )

    async with consumer:
        async with consumer.begin():
            for message in await consumer.messages():
                assert message == "test"
                assert len(consumer._cookies) == 1
                assert consumer._cookies[0] == 42
        assert len(consumer._cookies) == 0
        assert lb_consumer_mock.commit.call_count == 1


@pytest.mark.asyncio
async def test_consumer_start_failed(mocker, logbroker_api_mock, logbroker_consumer_mock):
    lb_consumer_mock = logbroker_consumer_mock(start=False)
    consumer = Consumer(
        logbroker_api_mock(
            lb_consumer_mock
        ),
        mocker.Mock(),
        consumer='some/consumer',
        topic='some/topic',
        logger=logging.getLogger()
    )

    with pytest.raises(LogbrokerException):
        async with consumer:
            pass
    assert consumer._consumer_client is None


@pytest.mark.asyncio
async def test_consumer_processing_failed(mocker, logbroker_api_mock, logbroker_consumer_mock):
    lb_consumer_mock = logbroker_consumer_mock()
    consumer = Consumer(
        logbroker_api_mock(
            consumer=lb_consumer_mock
        ),
        mocker.Mock(),
        consumer='some/consumer',
        topic='some/topic',
        logger=logging.getLogger()
    )

    with pytest.raises(Exception):
        async with consumer:
            async with consumer.begin():
                await consumer.messages()
                assert len(consumer._cookies) == 1
                raise Exception()
    assert len(consumer._cookies) == 1
    assert lb_consumer_mock.commit.call_count == 0


@pytest.mark.asyncio
async def test_producer_write_ok(mocker, logbroker_api_mock, logbroker_producer_mock):
    msg_num = 1

    def write_callback(seq_no, data):
        nonlocal msg_num
        assert data == 'hello world'
        assert seq_no == msg_num
        msg_num += 1

    producer = Producer(
        logbroker_api_mock(producer=logbroker_producer_mock(
            write_callback=write_callback
        )),
        mocker.Mock(),
        topic='some/topic',
        source_id='some-source-id',
        logger=logging.getLogger()
    )

    async with producer:
        await producer.write('hello world')


@pytest.mark.asyncio
async def test_producer_start_failed(mocker, logbroker_api_mock, logbroker_producer_mock):
    producer = Producer(
        logbroker_api_mock(producer=logbroker_producer_mock(
            start=False
        )),
        mocker.Mock(),
        topic='some/topic',
        source_id='some-source-id',
        logger=logging.getLogger()
    )

    with pytest.raises(LogbrokerException):
        async with producer:
            pass
    assert producer._producer_client is None

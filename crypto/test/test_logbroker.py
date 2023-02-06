import threading
import time

from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import WriterCodec
import six
from six.moves import range

from crypta.lib.python.lb_pusher import logbroker
from crypta.lib.python.lb_pusher.reader_config import ReaderConfig


def test_pq_client(logbroker_config):
    reader_config = ReaderConfig(topic=logbroker_config.topic, client_id=logbroker_config.consumer)

    pq_client = _pq_client(logbroker_config)
    pq_writer = pq_client.get_writer(logbroker_config.topic)
    pq_reader = pq_client.get_reader(reader_config)

    to_logbroker = ["x", six.b("y"), "z"]

    with pq_client:
        with pq_writer:
            for message in to_logbroker:
                pq_writer.write(message)

        with pq_reader:
            from_logbroker = []
            for _ in range(len(to_logbroker)):
                from_logbroker += list(pq_reader.get_messages())

            assert wrap(to_logbroker) == from_logbroker
            assert not list(pq_reader.get_messages())

            pq_reader.commit()

    pq_client = _pq_client(logbroker_config)
    pq_reader = pq_client.get_reader(reader_config)

    with pq_client:
        with pq_reader:
            assert not list(pq_reader.get_messages())


def test_batching(logbroker_config):
    reader_config = ReaderConfig(topic=logbroker_config.topic, client_id=logbroker_config.consumer)

    batch1 = [six.u("я"), six.b("y"), "д", "z", "ч"]
    batch2 = ["z"]
    to_logbroker = batch1 + batch2
    max_batch_size = len(six.b("").join(wrap(batch1)))

    pq_client = _pq_client(logbroker_config)
    pq_writer = pq_client.get_writer(logbroker_config.topic)
    batching_pq_writer = logbroker.BatchingPQWriter(pq_writer, max_batch_size=max_batch_size, codec=WriterCodec.RAW)
    pq_reader = pq_client.get_reader(reader_config)

    with pq_client:
        with batching_pq_writer, pq_reader:
            for message in to_logbroker:
                batching_pq_writer.write(message)

            assert wrap(batch1) == _unpack_batch(list(pq_reader.get_messages()))
            assert not list(pq_reader.get_messages())

            batching_pq_writer.flush()
            assert wrap(batch2) == _unpack_batch(list(pq_reader.get_messages()))
            assert not list(pq_reader.get_messages())

            pq_reader.commit()


def test_no_commit(logbroker_config):
    reader_config = ReaderConfig(topic=logbroker_config.topic, client_id=logbroker_config.consumer)

    pq_client = _pq_client(logbroker_config)

    pq_writer = pq_client.get_writer(logbroker_config.topic)
    pq_reader = pq_client.get_reader(reader_config)

    to_logbroker = ["x", six.b("y"), "z"]

    with pq_client:
        with pq_writer:
            for message in to_logbroker:
                pq_writer.write(message)

        with pq_reader:
            from_logbroker = []
            for _ in range(len(to_logbroker)):
                from_logbroker += list(pq_reader.get_messages())

            assert wrap(to_logbroker) == from_logbroker
            assert not list(pq_reader.get_messages())

    pq_client = _pq_client(logbroker_config)
    pq_reader = pq_client.get_reader(reader_config)

    with pq_client:
        with pq_reader:
            from_logbroker = []
            for _ in range(len(to_logbroker)):
                from_logbroker += list(pq_reader.get_messages())

            assert wrap(to_logbroker) == from_logbroker
            assert not list(pq_reader.get_messages())

            pq_reader.commit()


def test_async_pq_reader(logbroker_config):
    reader_config = ReaderConfig(topic=logbroker_config.topic, client_id=logbroker_config.consumer)

    pq_client = _pq_client(logbroker_config)
    pq_writer = pq_client.get_writer(logbroker_config.topic)
    pq_reader = pq_client.get_async_reader(reader_config, timeout=5)

    to_logbroker = ["x", six.b("y"), "z"]

    with pq_client:
        with pq_writer:
            for message in to_logbroker:
                pq_writer.write(message)

        with pq_reader:
            from_logbroker = []

            def process(batch, cookie):
                from_logbroker.extend(batch)
                pq_reader.done_cookies.put_nowait(cookie)

            thread = threading.Thread(target=pq_reader.loop, args=(process,))
            thread.start()
            time.sleep(5)
            pq_reader.stop()
            thread.join(10)

            assert wrap(to_logbroker) == from_logbroker

    pq_client = _pq_client(logbroker_config)
    pq_reader = pq_client.get_reader(reader_config)

    with pq_client:
        with pq_reader:
            assert not list(pq_reader.get_messages())


def _pq_client(logbroker_config):
    return logbroker.PQClient(
        url=logbroker_config.host,
        port=logbroker_config.port,
        tvm_id=None,
        tvm_secret=None,
        timeout=5,
    )


def _unpack_batch(messages):
    assert 1 == len(messages)
    return messages[0].split(six.b("\n"))


def wrap(l):
    return [six.ensure_binary(i) for i in l]

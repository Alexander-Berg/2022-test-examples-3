import logging
import zlib

from concurrent import futures
import kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api as pqlib
import six


logger = logging.getLogger(__name__)


def newline_splitter(chunk):
    return [six.ensure_str(message) for message in chunk.strip().split(six.b("\n"))]


def read_all(consumer, timeout=20, sort_messages=False, chunk_splitter=newline_splitter):
    # https://lb.yandex-team.ru/docs/how_to/develop_with_python/
    logger.info("Try to read all data and commit after")

    cookies_to_commit = []
    messages = []

    assert consumer.start().result(timeout=timeout).HasField("init")

    event_future = None
    while True:
        try:
            event_future = consumer.next_event()
            result = event_future.result(timeout=timeout)

            if result.type == pqlib.ConsumerMessageType.MSG_DATA:
                cookies_to_commit.append(result.message.data.cookie)
                for batch in result.message.data.message_batch:
                    for chunk in batch.message:
                        if chunk.meta.codec == pqlib.WriterCodec.RAW.value:
                            data = chunk.data
                        elif chunk.meta.codec == pqlib.WriterCodec.GZIP.value:
                            data = zlib.decompress(chunk.data, 16 + zlib.MAX_WBITS)
                        else:
                            raise NotImplementedError

                        messages += chunk_splitter(data)

        except futures.TimeoutError:
            break

    logger.info("Cookies to commit: %s", cookies_to_commit)
    if not cookies_to_commit:
        consumer.reads_done()
        consumer.stop()
        logger.info("Nothing to commit. Done")
        return messages

    logger.info("Commit")
    consumer.commit(cookies_to_commit)

    logger.info("Wait for ack on commits")

    last_committed_cookie = None
    while True:
        result = event_future.result(timeout=10)

        if result.type == pqlib.ConsumerMessageType.MSG_DATA:
            continue

        assert result.type == pqlib.ConsumerMessageType.MSG_COMMIT

        if last_committed_cookie is not None:
            assert result.message.commit.cookie[-1] > last_committed_cookie

        last_committed_cookie = result.message.commit.cookie[-1]

        if last_committed_cookie != cookies_to_commit[-1]:
            event_future = consumer.next_event()
        else:
            break

    consumer.reads_done()
    consumer.stop()
    logger.info("Done")

    if sort_messages:
        messages.sort()

    return messages

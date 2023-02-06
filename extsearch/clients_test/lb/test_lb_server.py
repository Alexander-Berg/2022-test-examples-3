import json
import os
import threading
import logging
import time
from collections import defaultdict

from vh.lib.lb_watcher import (
    LbWatcher
)
from vh.lib.lb_watcher.handlers.base import (
    BaseMessageHandler, RetryError, HandlerError
)
from contextvars import ContextVar
from vhs_lib.pq_reader import PQReaderFullContent
from vhs_lib.pq_writer import PQOrderedWriter, PQWriter
from utils.pq_writer import PQWriter as SimplePQWriter, ThreadSafePQWriter
from vh.lib.metrics import (
    MetricsManager
)
from kikimr.public.sdk.python.persqueue.errors import ActorTerminatedException


# to run locally need to start logbrocker localy
# doc: https://logbroker.yandex-team.ru/docs/how_to/local#finding-port

LOCAL_ENV = bool(int(os.getenv('LOCAL_ENV', 1)))

LB_HOST = 'localhost'
LB_PORT = int(os.getenv('LOGBROKER_PORT', 2135))
TEST_TOPIC = 'test_video_meta'
CLIENT_ID = 'client_id'
TVM_CLIENT = None

if not LOCAL_ENV:
    LB_HOST = 'lbkx.logbroker.yandex.net'
    LB_PORT = int(2135)
    TEST_TOPIC = '/video-rt-vh/moderation/test/test_video_meta'
    CLIENT_ID = '/video-rt-vh/moderation/test/ugc_moderation_test'

    def get_tvm_client():
        import tvmauth
        port = 1234
        return tvmauth.TvmClient(tvmauth.TvmToolClientSettings(
            self_alias='pq-client',
            port=port,
        ))
    TVM_CLIENT = get_tvm_client()


PARTITION_NUMBER = 16

id_var = ContextVar('')


class Message(dict):

    def validate(self):
        pass

    def data_id(self):
        return '1'


class Handler(BaseMessageHandler):
    message_type = Message

    def __init__(self):
        super(Handler, self).__init__()
        self.counter = defaultdict(int)
        self.ids = defaultdict(set)
        self.last_message = time.monotonic()

    def handle(self, message: dict) -> None:
        id_ = message.get('test', 1)
        with threading.Lock():
            self.last_message = time.monotonic()
            self.increase_counter(id_, message.get('type'))
        if id_ % 12 == 0:
            raise RetryError()
        if id_ % 11 == 0:
            raise HandlerError()
        if id_ % 13 == 0:
            raise RuntimeError()
        print(message)

    def increase_counter(self, id_, type_):
        if id_ in self.ids[type_]:
            return
        self.counter[type_] += 1
        self.ids[type_].add(id_)


def create_watcher(test_handler):
    reader = PQReaderFullContent(
        url=LB_HOST,
        port=LB_PORT,
        topic=TEST_TOPIC,
        client_id=CLIENT_ID,
        deadline=100,
        tvm_client=TVM_CLIENT
    )

    watcher = LbWatcher(
        reader=reader,
        handler=test_handler,
        dlq_writer=None,
        id_var=id_var,
        skip_invalid_message=False,
        metrics=MetricsManager()
    )
    return watcher


def writer():
    writer = PQWriter(
        url=LB_HOST,
        topic=TEST_TOPIC,
        port=LB_PORT,
        producers_num=16,
        tvm_client=TVM_CLIENT
    )
    for i in range(2):
        with writer as producer:
            producer.write(i, json.dumps({'test': i, 'type': 'context_writer'}))
    writer = SimplePQWriter(
        host=LB_HOST,
        topic=TEST_TOPIC,
        port=LB_PORT,
        writers_num=2,
        tvm_client=TVM_CLIENT
    )

    for i in range(10):
        try:
            writer.write(str(i), {'test': i, 'type': 'simple_writer'})
        except RuntimeError:
            continue
    writer.stop()
    assert writer.pq_api is None
    assert writer.pq_writer is None

    writer = PQOrderedWriter(
        url=LB_HOST,
        topic=TEST_TOPIC,
        port=LB_PORT,
        producers_num=5,
        tvm_client=TVM_CLIENT
    )
    with writer as producer:
        for i in range(15):
            producer.write(i, json.dumps({'test': i, 'type': 'context_writer_long'}))
    assert writer.pq_api is None
    assert writer._topic_writer is None


def run_watcher(watcher: LbWatcher, kill_event: threading.Event):
    while not kill_event.is_set():
        watcher.step()
    assert watcher.reader.pq_api is None
    watcher.stop()
    try:
        _ = watcher.reader.consumer
        assert False, 'ActorTerminatedException not raised'
    except ActorTerminatedException:
        pass
    assert watcher.reader._consumer is None


def test_main():
    logging.basicConfig(level=logging.DEBUG, format='%(levelname)s %(asctime)s %(module)s %(message)s')
    warn_loggers = ['ydb', 'kikimr']
    for name in warn_loggers:
        log = logging.getLogger(name)
        log.setLevel(logging.WARNING)

    test_handler = Handler()
    kill_event = threading.Event()

    threads = []
    writer_t = threading.Thread(target=writer)
    writer_t.start()
    threads.append(writer_t)
    for i in range(2):
        watcher = create_watcher(test_handler)
        thread = threading.Thread(target=run_watcher, args=(watcher, kill_event))
        thread.start()
        threads.append(thread)

    while True:
        with threading.Lock():
            if time.monotonic() - test_handler.last_message > 10:
                break
        time.sleep(1)

    assert test_handler.counter.get('context_writer') == 2
    assert test_handler.counter.get('simple_writer') == 10
    assert test_handler.counter.get('context_writer_long') == 15

    kill_event.set()

    for thread in threads:
        thread.join()


def test_thread_safe():
    writer = ThreadSafePQWriter(
        host=LB_HOST,
        topic=TEST_TOPIC,
        port=LB_PORT,
        writers_num=2,
        tvm_client=TVM_CLIENT
    )

    writer_params = [
        ('write_thread', 100),
        ('write_thread2', 100),
        ('write_thread3', 100),
        ('write_thread4', 100),
    ]

    threads = []
    for params in writer_params:
        args = [writer, ]
        args.extend(params)
        args = tuple(args)
        threads.append(
            threading.Thread(target=write_thread_safe, args=args)
        )
    for write_thread in threads:
        write_thread.start()

    for thread in threads:
        thread.join()

    reader = PQReaderFullContent(
        url=LB_HOST,
        port=LB_PORT,
        topic=TEST_TOPIC,
        client_id=CLIENT_ID,
        deadline=100,
        tvm_client=TVM_CLIENT,
        read_infly_count=100,
        max_count=100
    )
    counter = defaultdict(int)
    with reader as read:
        for batch in read:
            if not isinstance(batch, list):
                break
            for msg in batch:
                counter[json.loads(msg.decompressed_data).get('id', 1)] += 1
            read.commit()

    try:
        for writer_id, writer_count, *_ in writer_params:
            assert counter[writer_id] == writer_count
    finally:
        writer.stop()


def write_thread_safe(
    writer: ThreadSafePQWriter, writer_id: str,
    count: int = 20
):
    messages_writed = 0
    for i in range(count):
        try:
            writer.write(str(i), {'test': i, 'type': 'simple_writer_tread_safe', 'id': writer_id})
            messages_writed += 1
        except Exception:
            logging.exception('Error on write')
            continue


if __name__ == '__main__':
    test_main()
    test_thread_safe()

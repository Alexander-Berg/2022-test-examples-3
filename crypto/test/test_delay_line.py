import itertools
import json
import logging
import operator
import time

import pytest

from crypta.lib.python.logbroker.test_helpers import consumer_utils


logger = logging.getLogger(__name__)
FROZEN_TIME = 1700000000
seq_no_counter = itertools.count(1)
log_line_counter = itertools.count(1)
ERTELECOM = "ertelecom"
ROSTELECOM = "rostelecom"


def to_bytes(data):
    return data.encode("utf-8")


def write_to_lb(producer, msg):
    logger.info("Senging %s", msg)
    assert producer.write(next(seq_no_counter), to_bytes(msg)).result(timeout=10).HasField("ack")


@pytest.fixture(scope="session")
def ext_fp_event_delayed_log_producer(ext_fp_event_delayed_log_lb_client):
    producer = ext_fp_event_delayed_log_lb_client.create_producer()
    yield producer
    producer.stop().result()


def get_log_line(ip, has_yuid, unixtime, log_type, source_id):
    counter = next(log_line_counter)

    return (
        '{{'
        '"duid": {duid}, '
        '"hit_log_id": {hit_log_id}, '
        '"ip": "{ip}", '
        '"log_id": {log_id}, '
        '"log_type": "{log_type}", '
        '"port": {port}, '
        '"unixtime": {unixtime}, '
        '"user_agent": "Mozilla/5.0 (Windows NT РУС)", '
        '"watch_id": {watch_id}, '
        '"yuid": {yuid}, '
        '"domain": "{domain}", '
        '"current_timestamp": {current_timestamp}, '
        '"source_id": "{source_id}"'
        '}}'
    ).format(
        duid="{}{}".format(unixtime + counter, 1000 + counter),
        hit_log_id=100500 + counter,
        ip=ip,
        log_id=0,
        log_type=log_type,
        port=1024 + counter,
        unixtime=unixtime,
        watch_id=2 * 10**17 + counter,
        yuid="{}{}".format(1000 + counter, unixtime + counter) if has_yuid else 0,
        domain="domain-{}.ru".format(counter),
        current_timestamp=unixtime + counter + 1,
        source_id=source_id,
    )


def get_broken_line():
    return '{"log_type":'


def write_log(producer, event_ts):
    write_to_lb(producer, get_broken_line())

    ip_has_yuid_unixtime_log_type = [
        ("5.3.61.255", True, event_ts, "bs-watch-log", ERTELECOM),
        ("150.3.100.2", True, event_ts, "bs-watch-log", ROSTELECOM),
        ("150.3.100.3", True, event_ts, "bs-watch-log", "UNKNOWN"),
        ("127.0.0.1", True, event_ts, "bs-watch-log", "UNKNOWN"),
    ]
    for ip, has_yuid, unixtime, log_type, source_id in ip_has_yuid_unixtime_log_type:
        write_to_lb(producer, get_log_line(ip, has_yuid, unixtime, log_type, source_id))


def read_log(consumer):
    response = consumer_utils.read_all(consumer)

    return sorted([json.loads(line) for line in response], key=operator.itemgetter("duid"))


def run_test(producer, lb_client, wait_sec, event_ts):
    assert [] == read_log(lb_client.create_consumer())

    write_log(producer, event_ts)

    time.sleep(wait_sec)

    return read_log(lb_client.create_consumer())


def test_delay_line(delay_line_30sec_delay, ext_fp_event_delayed_log_producer, ext_fp_event_log_lb_client):
    wait_sec = 10  # 10 seconds for execution delays; events are expected to be processed instantly
    event_ts = FROZEN_TIME - 60
    return run_test(ext_fp_event_delayed_log_producer, ext_fp_event_log_lb_client, wait_sec, event_ts)


def test_delay_line_zero_delay(delay_line_zero_delay, ext_fp_event_delayed_log_producer, ext_fp_event_log_lb_client):
    wait_sec = 10
    event_ts = FROZEN_TIME
    return run_test(ext_fp_event_delayed_log_producer, ext_fp_event_log_lb_client, wait_sec, event_ts)


def test_delay_line_actually_wait(delay_line_30sec_delay, ext_fp_event_delayed_log_producer, ext_fp_event_log_lb_client):
    event_ts = FROZEN_TIME
    write_log(ext_fp_event_delayed_log_producer, event_ts)

    assert [] == read_log(ext_fp_event_log_lb_client.create_consumer())

    wait_sec = 30 + 10
    time.sleep(wait_sec)

    return read_log(ext_fp_event_log_lb_client.create_consumer())

import itertools
import json
import logging
import operator
import time

import pytest

import crypta.lib.python.ext_fp.constants as ext_fp_constants
from crypta.lib.python.logbroker.test_helpers import consumer_utils


logger = logging.getLogger(__name__)
FROZEN_TIME = 1700000000
seq_no_counter = itertools.count(1)
log_line_counter = itertools.count(1)

BEELINE = ext_fp_constants.BEELINE_SOURCE_ID
ERTELECOM = ext_fp_constants.ER_TELECOM_SOURCE_ID
INTENTAI = ext_fp_constants.INTENTAI_SOURCE_ID
MTS = ext_fp_constants.MTS_SOURCE_ID
ROSTELECOM = ext_fp_constants.ROSTELECOM_SOURCE_ID


def to_bytes(data):
    return data.encode("utf-8")


def write_to_lb(producer, msg):
    logger.info("Senging %s", msg)
    assert producer.write(next(seq_no_counter), to_bytes(msg)).result(timeout=10).HasField("ack")


@pytest.fixture(scope="session")
def fp_event_log_producer(fp_event_log_lb_client):
    producer = fp_event_log_lb_client.create_producer()
    yield producer
    producer.stop().result()


def get_log_line(source_id, ip, port, has_yuid, unixtime, log_type):
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
        duid="{}{}".format(unixtime + counter, 1000000 + counter),
        hit_log_id=100500 + counter,
        ip=ip,
        log_id=0,
        log_type=log_type,
        port=port,
        unixtime=unixtime,
        watch_id=2 * 10**17 + counter,
        yuid="{}{}".format(1000 + counter, unixtime + counter) if has_yuid else 0,
        domain="domain-{}.ru".format(counter),
        current_timestamp=unixtime + counter + 1,
        source_id=source_id,
    )


def get_broken_line():
    return '{"log_type":'


def test_matcher(matcher, fp_event_log_producer, ext_fp_match_log_lb_client):
    write_to_lb(fp_event_log_producer, get_broken_line())

    source_id_ip_has_yuid_unixtime_log_type = [
        (ERTELECOM, "5.3.61.255", 1111, True, FROZEN_TIME, "bs-watch-log"),
        (ERTELECOM, "5.3.62.0", 2222, True, FROZEN_TIME - 11, "bs-watch-log"),
        (ERTELECOM, "5.3.62.0", 2222, True, FROZEN_TIME - 10, "bs-watch-log"),
        (ERTELECOM, "5.3.62.255", 3333, False, FROZEN_TIME, "bs-watch-log"),
        (ERTELECOM, "5.3.63.0", 4444, True, FROZEN_TIME, "bs-watch-log"),
        (ERTELECOM, "5.3.100.0", 5555, True, FROZEN_TIME - 30, "bs-watch-log"),
        (ERTELECOM, "5.3.100.1", 6666, True, FROZEN_TIME - 31, "bs-watch-log"),

        (ROSTELECOM, "15.3.100.1", 7777, True, FROZEN_TIME, "bs-watch-log"),
        (ROSTELECOM, "15.3.100.2", 8888, True, FROZEN_TIME, "bs-watch-log"),
        (ROSTELECOM, "15.3.100.2", 9999, True, FROZEN_TIME - 5, "bs-watch-log"),
        (ROSTELECOM, "15.3.100.2", 8888, True, FROZEN_TIME - 10, "bs-watch-log"),
        (ROSTELECOM, "15.3.100.20", 8888, True, FROZEN_TIME - 5, "bs-watch-log"),
        (ROSTELECOM, "150.3.100.2", 11000, True, FROZEN_TIME, "bs-watch-log"),

        ("UNKNOWN", "150.3.100.2", 22000, True, FROZEN_TIME, "bs-watch-log"),

        (MTS, "160.1.2.3", 3333, True, FROZEN_TIME, "bs-watch-log"),
        (MTS, "160.1.2.4", 4444, True, FROZEN_TIME - 30, "bs-watch-log"),
        (MTS, "0.0.0.0", 5555, True, FROZEN_TIME, "bs-watch-log"),
        (MTS, "8.8.8.8", 6666, True, FROZEN_TIME, "bs-watch-log"),

        (BEELINE, "170.1.2.3", 3333, True, FROZEN_TIME, "bs-watch-log"),
        (BEELINE, "0.0.0.0", 4444, True, FROZEN_TIME, "bs-watch-log"),
        (BEELINE, "8.8.8.8", 5555, True, FROZEN_TIME, "bs-watch-log"),

        (INTENTAI, "87.241.189.111", 4444, True, FROZEN_TIME, "bs-watch-log"),
        (INTENTAI, "9.9.9.9", 5555, True, FROZEN_TIME, "bs-watch-log"),
    ]

    log_line = "\n".join(get_log_line(*args) for args in source_id_ip_has_yuid_unixtime_log_type)
    write_to_lb(fp_event_log_producer, log_line)

    time.sleep(10)

    response = consumer_utils.read_all(ext_fp_match_log_lb_client.create_consumer())

    return sorted([json.loads(line) for line in response], key=operator.itemgetter("duid"))

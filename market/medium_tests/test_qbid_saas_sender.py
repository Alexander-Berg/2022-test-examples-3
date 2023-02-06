#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import time
import re

from market.pylibrary.mindexerlib import util
from market.pylibrary.mindexerlib.recent_symlink_system import unixtime2generation

from market.idx.pylibrary.mindexer_core.qbid.saas_sender_service import QBidSaasSender


class MockMbiDeltaCurl(object):
    RESPONSE_OK = 200

    def download(self, url, destination_file):
        parts = url.split("/")
        if not parts:
            raise RuntimeError('bad url: {}'.format(url))

        delta_ts = int(parts[-1])
        if not delta_ts:
            raise RuntimeError('bad url: {}'.format(url))

        log = util.get_logger(self)
        log.info("Mocking download '{}' to {}'".format(url, destination_file))
        with open(destination_file, 'w') as f:
            f.write("This mock delta downloaded using delta_ts: {}".format(delta_ts))
        return dict()


def launch_sender(tmpdir, max_delta_period_sec=500, **kwargs):
    sender_kwargs = {
        "curl": MockMbiDeltaCurl(),
        "directory": str(tmpdir),
        "dryrun": True,
        "keep_generations": 2,
        "max_delta_period_sec": max_delta_period_sec,
        "mbi_exchange_url": "dummy_mbi_url",
        "saas_hub_addresses": "dummy_saas_hub_host",
        "saas_hub_schema": "post",
        "sender_bin": "dummy_sender_bin",
    }
    sender_kwargs.update(kwargs)
    sender = QBidSaasSender(**sender_kwargs)
    return sender.process()


def test_startup(tmpdir):
    """
    Тестируем, что в случае отсутствия delta_ts, он будет создан,
    и дальше не будет никаких действий (т.е. никаких директорий с поколениями не должно быть)
    """
    expected_delta_ts = launch_sender(tmpdir)

    with open(str(tmpdir / 'delta.ts')) as f:
        fact_delta_ts = int(f.read().strip())

    assert expected_delta_ts == fact_delta_ts
    assert len(os.listdir(str(tmpdir))) == 1


def test_trivial(tmpdir):
    """
    Тестируем стандартный запуск обновления ставок.
    """

    launch_sender(tmpdir)
    time.sleep(2)
    delta_ts2 = launch_sender(tmpdir)
    time.sleep(2)
    delta_ts3 = launch_sender(tmpdir)
    time.sleep(2)
    delta_ts4 = launch_sender(tmpdir)

    # last ts must be stored at delta_ts
    with open(str(tmpdir / 'delta.ts'), 'r') as f:
        fact_delta_ts = int(f.read().strip())
    assert fact_delta_ts == delta_ts4

    # there should be generations, taking into account rotation
    gen_name2 = unixtime2generation(delta_ts2)
    gen_name3 = unixtime2generation(delta_ts3)
    gen_name4 = unixtime2generation(delta_ts4)
    assert not os.path.exists(str(tmpdir / gen_name2))
    assert os.path.exists(str(tmpdir / gen_name3 / 'delta.pbuf.sn'))
    assert os.path.exists(str(tmpdir / gen_name4 / 'delta.pbuf.sn'))

    # check that delta_ts was correct in the url for MBI
    with open(str(tmpdir / gen_name4 / 'delta.pbuf.sn'), 'r') as f:
        fact_msg = f.read().strip()
    assert fact_msg == "This mock delta downloaded using delta_ts: {}".format(delta_ts3)

    # make sure that the parameters for saas sender are transmitted correctly
    with open(str(tmpdir / gen_name4 / 'dryrun.cmd'), 'r') as f:
        fact_msg = f.read().strip()
    assert re.match(r'^dummy_sender_bin -a dummy_saas_hub_host -s post -f pbsn -m api_qbids .*/delta\.pbuf\.sn$', fact_msg)


def test_using_generation_timestamp(tmpdir):
    """
    Тестируем обновление таймстепов ставок
    """
    launch_sender(tmpdir)
    time.sleep(2)
    delta_ts = launch_sender(tmpdir, use_generation_timestamp=True)
    gen_name = unixtime2generation(delta_ts)

    # убеждаемся, что параметры для saas sender передаются верно
    with open(str(tmpdir / gen_name / 'dryrun.cmd')) as f:
        fact_msg = f.read().strip()
    assert re.match(
        r'^dummy_sender_bin -a dummy_saas_hub_host -s post -f pbsn -m api_qbids --qbid-timestamp {} .*/delta\.pbuf\.sn$'.format(delta_ts),
        fact_msg)


def test_using_pq_sender(tmpdir):
    """
    Тестируем передачу параметров для отправки через pq
    """
    # secret
    tvm_secret_path = str(tmpdir / 'secret.tvm')
    with open(tvm_secret_path, 'wt') as f:
        f.write('tvmsecret\n')

    launch_sender(tmpdir)
    time.sleep(2)
    pq_options = {
        'pq_topic': 'test/topic',
        'pq_host': 'logbroker',
        'pq_port': 3333,
        'pq_source_id': 1,
        'pq_tvm_id': 2,
        'pq_tvm_secret_path': tvm_secret_path,
        'pq_log_level': 'DEBUG',
    }
    delta_ts = launch_sender(tmpdir, **pq_options)
    gen_name = unixtime2generation(delta_ts)

    with open(str(tmpdir / gen_name / 'dryrun.cmd')) as f:
        fact_msg = f.read().strip()
    regex = r'^dummy_sender_bin --pq-host logbroker --pq-port 3333 --pq-topic test/topic --pq-source-id 1'
    regex += r' --pq-tvm-id 2 --pq-tvm-secret-path {} --pq-log-level DEBUG -f pbsn -m api_qbids .*/delta\.pbuf\.sn$'
    assert re.match(regex.format(tvm_secret_path), fact_msg)


def test_timeout(tmpdir):
    """
    Тестируем обновление ставок в случае слишком большой дельты по времени
    в этом случае - дельта должна обрезаться лимитом времени.
    Лимит 2 секунды, обновление запускается через 4 секунды, но интервал скачивания дельты должен быть 2 секунды
    """
    launch_sender(tmpdir, max_delta_period_sec=2)
    time.sleep(4)
    new_delta_ts = launch_sender(tmpdir, max_delta_period_sec=2)

    gen_name = unixtime2generation(new_delta_ts)
    with open(str(tmpdir / gen_name / 'delta.pbuf.sn')) as f:
        msg = f.read().strip()
        msg = msg.replace("This mock delta downloaded using delta_ts: ", "")
        fact_delta_interval = new_delta_ts - int(msg)

    assert fact_delta_interval == 2

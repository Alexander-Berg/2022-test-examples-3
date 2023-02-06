#!/usr/bin/env python
# -*- coding: utf-8 -*-

import subprocess
import tempfile
import socket
import json
import time
import inspect
import os
import requests
import re
import sys
from os.path import join, isfile
from contextlib import closing
from yatest.common import binary_path, network

import google.protobuf.text_format as text_format
import search.begemot.rules.preclassifier_knn.proto.preclassifier_knn_pb2 as pre_pb2


APPHOST_SERVANT_PATH = binary_path('apphost/tools/servant_client/servant_client')
EVLOGDUMP_PATH = binary_path('web/daemons/begemot/event_log_dump/event_log_dump')
EVENTLOG_PATH = 'current-begemot-worker'
YASM_SIGNALS_URL = '/admin?action=golovan'
FILE_PREFIX = 'web_preclassifiers_'
IMAGES_ITS_FILE = FILE_PREFIX + 'ratio_images.txt'
VIDEO_ITS_FILE = FILE_PREFIX + 'ratio_video.txt'
GEOV_ITS_FILE = FILE_PREFIX + 'ratio_geov.txt'
BEGEMOT_REQUEST_INIT_DATA_ROOT = binary_path('search/begemot/data/RequestInit')
BEGEMOT_REQUEST_INIT_DATA_DIR = join(BEGEMOT_REQUEST_INIT_DATA_ROOT, 'search', 'wizard', 'data', 'wizard')
BEGEMOT_REQUEST_INIT_BINARY = binary_path('search/daemons/begemot/request_init/request_init')
CONFIG_RULE_NAME = 'PreclassifierKnnConfig'


class ConfigEvent:
    def __init__(self):
        self.model_name = None
        self.requested_perc = None
        self.selected_perc = None
        self.threshold = None


class TestCtx:
    def __init__(self):
        self.full_config = None
        self.port = None
        self.its_dir = None
        self.its_update_period = None


def wait_till_port_is_open(port):
    sys.stderr.write('Waiting for port {} to start accepting connections...\n'.format(port))
    while True:
        with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
            sock.settimeout(10)
            if sock.connect_ex(('127.0.0.1', port)) == 0:
                sys.stderr.write('Port {} is opened\n'.format(port))
                return


def get_std_ammo():
    ammo = {
        "answers": [{"name": "CONFIG", "results": [{"requested_rules": [CONFIG_RULE_NAME], "type": "begemot_config"}]}]
    }
    return json.dumps(ammo)


def update_ctx_from_config(ctx):
    with open(join(BEGEMOT_REQUEST_INIT_DATA_DIR, CONFIG_RULE_NAME, 'config.pb.txt'), 'r') as f:
        config = text_format.Parse(f.read(), pre_pb2.TPreclassifierKnnConfig())
        ctx.full_config = config
        ctx.its_dir = config.ItsDir
        ctx.its_update_period = config.ItsUpdatePeriod


def run_begemot(port):
    tmp = tempfile.NamedTemporaryFile(delete=False)

    process = subprocess.Popen(
        [
            BEGEMOT_REQUEST_INIT_BINARY,
            '--data',
            BEGEMOT_REQUEST_INIT_DATA_DIR,
            '--port',
            str(port),
            '--cache_size',
            '0',
            '--log',
            EVENTLOG_PATH,
            '--cfg',
            tmp.name,
        ]
    )

    wait_till_port_is_open(port)
    return process


def get_preclassifier_config_events(evlog):
    res_events = []
    for event in evlog.split('\n'):
        event_sp = event.split('\t')
        if len(event_sp) >= 3 and event_sp[2] == 'TPreclassifierKnnItsThresholdInfo':
            ev = ConfigEvent()
            ev.model_name = event_sp[3]
            ev.requested_perc = int(event_sp[4])
            ev.selected_perc = int(event_sp[5])
            ev.threshold = float(event_sp[6])
            res_events.append(ev)
    return res_events


def print_raw_evlog(name, evlog):
    print('raw evlog for check "{}":\n{}'.format(name, evlog))


def get_yasm_stats(port):
    ans = requests.get('http://localhost:{}{}'.format(port, YASM_SIGNALS_URL))
    assert ans
    return ans.content.decode()


def shoot_ang_get_evlog(port, ammo):
    time_before = time.time() * 1000000
    servant_process = subprocess.Popen(
        [APPHOST_SERVANT_PATH, '-a', 'localhost:{}'.format(port), '-c', ammo],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    out, err = servant_process.communicate()

    time.sleep(2)
    time_after = time.time() * 1000000

    evlogdump_process = subprocess.Popen(
        [EVLOGDUMP_PATH, '-s', str(int(time_before)), '-e', str(int(time_after)), EVENTLOG_PATH],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    out = evlogdump_process.communicate()[0].decode()

    print_raw_evlog(inspect.stack()[1][3], out)
    return out


def check_no_its(test_ctx):
    out = shoot_ang_get_evlog(test_ctx.port, get_std_ammo())
    events = get_preclassifier_config_events(out)
    assert not events


def check_std_its_usage(test_ctx):
    to_check = [(IMAGES_ITS_FILE, 'images_knn'), (VIDEO_ITS_FILE, 'video_knn'), (GEOV_ITS_FILE, 'geov_knn')]
    for filename, model_prefix in to_check:
        its_file_path = join(test_ctx.its_dir, filename)
        try:
            with open(its_file_path, 'w') as f:
                f.write('42')

            time.sleep(test_ctx.its_update_period * 4)

            out = shoot_ang_get_evlog(test_ctx.port, get_std_ammo())
            events = get_preclassifier_config_events(out)
            assert len(events) > 0
            for ev in events:
                assert ev.model_name.startswith(model_prefix)
                assert ev.requested_perc == 42
                assert ev.selected_perc <= 42 and ev.selected_perc > 32
                assert ev.threshold >= 0.0
                config_threshold = (
                    test_ctx.full_config.Preclassifiers[ev.model_name].InfoByRatio[ev.selected_perc].Threshold
                )
                assert abs(ev.threshold - config_threshold) < 0.0001
                stats = get_yasm_stats(test_ctx.port)
                assert re.search('ItsPerc-{}_axxx",{}]'.format(ev.model_name, ev.selected_perc), stats)
        finally:
            if isfile(its_file_path):
                os.remove(its_file_path)


def test_preclassifier_config_its():
    try:
        pm = network.PortManager()

        test_ctx = TestCtx()
        test_ctx.port = pm.get_port(11888)
        update_ctx_from_config(test_ctx)

        begemot_process = run_begemot(test_ctx.port)

        # checks order is important

        check_no_its(test_ctx)

        os.makedirs(test_ctx.its_dir, exist_ok=True)

        check_std_its_usage(test_ctx)
    finally:
        begemot_process.terminate()

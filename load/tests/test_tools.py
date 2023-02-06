import os
import mock
import json
import pytest

from load.projects.tank_finder.lib.app import app
from load.projects.tank_finder.lib.config import TestingtConfig
from load.projects.tank_finder.lib.tools import get_host_dc, get_rtc_tanks, get_rtc_targets, get_conductor_tanks
from yatest import common

PATH = common.source_path('load/projects/tank_finder/tests/')


@pytest.mark.parametrize('host, networks', [
    ('kv1.tanks.yandex.net', 'networks.json'),
    ('87.250.235.226', 'networks.json'),
    ('2a02:6b8:0:1416::2', 'networks.json'),
])
def test_get_host_dc(host, networks):
    with open(os.path.join(PATH, networks), 'rb') as nets:
        with mock.patch('load.projects.tank_finder.lib.tools.get_from_cache', return_value=None):
            with mock.patch('load.projects.tank_finder.lib.tools.set_to_cache', return_value=None):
                dc = get_host_dc(host, json.loads(nets.read()))
                assert dc == 'MYT'


def test_get_rtc_tanks_from_cache():
    with open(os.path.join(PATH, 'rtc_tanks_list.json')) as f:
        tanks_list = json.load(f)
        with mock.patch('load.projects.tank_finder.lib.tools.get_from_cache', return_value=tanks_list):
            app.config.from_object(TestingtConfig)
            assert get_rtc_tanks(app) == tanks_list


def test_get_rtc_tanks_without_cache():
    with open(os.path.join(PATH, 'rtc_tanks.json')) as t:
        with open(os.path.join(PATH, 'rtc_tanks_list.json')) as f:
            rtc_tanks = json.load(t)
            tanks_list = json.load(f)
            requests = mock.Mock()
            requests.status_code = 200
            requests.json = mock.Mock(return_value=rtc_tanks)
            with mock.patch('load.projects.tank_finder.lib.tools.get_from_cache', return_value=None):
                with mock.patch('load.projects.tank_finder.lib.tools.set_to_cache', return_value=None):
                    with mock.patch('load.projects.tank_finder.lib.tools.make_request', return_value=requests):
                        app.config.from_object(TestingtConfig)
                        assert get_rtc_tanks(app) == tanks_list


@pytest.mark.parametrize('targets, target_port', [
    ([
        'man1-9337-all-rcloud-tanks-30169.gencfg-c.yandex.net',
        'iva1-0033-all-rcloud-tanks-30169.gencfg-c.yandex.net',
        'sas1-8786-a4e-all-rcloud-tanks-30169.gencfg-c.yandex.net'
    ],
    30169),
])
def test_get_rtc_targets(targets, target_port):
    with open(os.path.join(PATH, 'rtc_tanks.json')) as t:
        rtc_tanks = json.load(t)
        requests = mock.Mock()
        requests.status_code = 200
        requests.json = mock.Mock(return_value=rtc_tanks)
        with mock.patch('load.projects.tank_finder.lib.tools.make_request', return_value=requests):
            assert get_rtc_targets(app, '') == ['{}:{}'.format(target, target_port) for target in targets]


def test_get_conductor_tanks():
    with open(os.path.join(PATH, 'c_tanks_list.json')) as f:
        tanks_list = json.load(f)
        with mock.patch('load.projects.tank_finder.lib.tools.get_from_cache', return_value=tanks_list):
            app.config.from_object(TestingtConfig)
            assert get_conductor_tanks(app) == tanks_list


def test_get_conductor_tanks_without_cache():
    with open(os.path.join(PATH, 'c_tanks.txt')) as t:
        with open(os.path.join(PATH, 'c_tanks_list.json')) as f:
            c_tanks = t.read()
            tanks_list = json.load(f)
            requests = mock.Mock()
            requests.status_code = 200
            requests.content = c_tanks
            with mock.patch('load.projects.tank_finder.lib.tools.get_from_cache', return_value=None):
                with mock.patch('load.projects.tank_finder.lib.tools.set_to_cache', return_value=None):
                    with mock.patch('load.projects.tank_finder.lib.tools.make_request', return_value=requests):
                        app.config.from_object(TestingtConfig)
                        assert get_conductor_tanks(app) == tanks_list

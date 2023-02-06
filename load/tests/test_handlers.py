import json
import mock
import os
import pytest

from load.projects.tank_finder.lib.app import create_app

from yatest import common


PATH = common.source_path('load/projects/tank_finder/tests/')


@pytest.fixture()
def get_cache():
    with open(os.path.join(PATH, 'tanks_list.json')) as f:
        tanks_list = json.load(f)
        return tanks_list


@pytest.fixture
def test_client(mocker):
    with open(os.path.join(PATH, 'networks.json'), 'rb') as nets:
        mocker.patch('load.projects.tank_finder.lib.handlers.get_rt_networks', return_value=json.loads(nets.read()))
        mocker.patch('load.projects.tank_finder.lib.handlers.get_from_cache', return_value=None)

        flask_app = create_app('tank_finder')

        with flask_app.test_client() as testing_client:
            with flask_app.app_context():
                yield testing_client


@pytest.fixture()
def test_networks():
    with open(os.path.join(PATH, 'networks.json'), 'rb') as nets:
        return json.loads(nets.read())


def test_ping(test_client):
    response = test_client.get('/ping')
    assert response.status_code == 200
    assert b"Ping!" in response.data


def test_check_bad_hosts(test_client):
    response = test_client.get('/check_hosts')
    assert response.status_code == 404


@pytest.mark.parametrize('tank, target, status_code, result', [
    ('kv1.tanks.yandex.net', 'buratino.tanks.yandex.net', 200, '{"result":true}\n'),
    ('87.250.235.226', 'buratino.tanks.yandex.net', 200, '{"result":true}\n'),
    ('lee.tanks.yandex.net', 'buratino.tanks.yandex.net', 404, '{"result": "error", "error_msg": "Ip address or hostname not found"}'),
])
def test_check_good_hosts(test_client, tank, target, status_code, result):
    response = test_client.get('/check_hosts?tank={}&target={}'.format(tank, target))
    assert response.status_code == status_code
    assert response.data == result


def test_bad_tanks_list(cache, test_client):
    response = test_client.get('/tanks/list.json?target=')
    assert response.status_code == 404
    assert 'Empty public tanks status' in response.data


@pytest.mark.parametrize('target, service', [
    ('kv1.tanks.yandex.net', ''),
])
def test_c_tanks_list(target, service, test_client):
    with open(os.path.join(PATH, 'c_myt_tanks_list.json')) as f:
        c_tanks_list = json.load(f)
        with mock.patch('load.projects.tank_finder.lib.handlers.get_conductor_tanks', return_value=c_tanks_list):
            response = test_client.get('/tanks/list.json?target={}&service={}'.format(target, service))
            assert response.status_code == 200
            assert json.dumps(c_tanks_list) == response.data


@pytest.mark.parametrize('target, service', [
    ('nanny:some.host', ''),
    ('deploy:some.host', ''),
])
def test_cloud_tanks_list(target, service, test_client):
    with open(os.path.join(PATH, 'c_myt_tanks_list.json')) as f:
        c_tanks_list = json.load(f)
        with mock.patch('load.projects.tank_finder.lib.handlers.get_conductor_tanks', return_value=c_tanks_list):
            with mock.patch('load.projects.tank_finder.lib.handlers.get_rtc_targets', return_value=['kv1.tanks.yandex.net:8083']):
                with mock.patch('load.projects.tank_finder.lib.handlers.get_deploy_hosts', return_value=('kv1.tanks.yandex.net', '8083')):
                    response = test_client.get('/tanks/list.json?target={}&service={}'.format(target, service))
                    assert response.status_code == 200
                    assert json.dumps(c_tanks_list) == response.data


@pytest.mark.parametrize('target, dc', [
    ('kv1.tanks.yandex.net', '{"datacenter": "MYT"}'),
    ('steam.tanks.yandex.net', '{"datacenter": "MYT"}'),
])
def test_target_dc(target, dc, test_client):
    response = test_client.get('/target_dc?target={}'.format(target))
    assert response.status_code == 200
    assert dc == response.data

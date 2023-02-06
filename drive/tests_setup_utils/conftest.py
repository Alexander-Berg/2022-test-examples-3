import json
import os

import pytest

from drive.services.userver_libraries import tests_setup_utils


def pytest_addoption(parser):
    parser.addoption('--redis-host', action='store')
    parser.addoption('--redis-sentinel-port', type=int, action='store')
    parser.addoption('--redis-master-port', type=int, action='store')


def pytest_generate_tests(metafunc):
    if 'redis' in metafunc.fixturenames:
        metafunc.parametrize('redis', [{
            'host': metafunc.config.option.redis_host,
            'master-port': metafunc.config.option.redis_master_port,
            'sentinel-port': metafunc.config.option.redis_sentinel_port,
        }])
        path_splitted = os.getcwd().split('/')
        service_name = path_splitted[path_splitted.index('services') + 1].replace('_', '-')
        os.environ['SECDIST_CONFIG'] = json.dumps({
            'redis_settings': {
                service_name: {
                    'password': '',
                    'sentinels': [
                        {
                            'host': metafunc.config.option.redis_host,
                            'port': metafunc.config.option.redis_sentinel_port,
                        },
                    ],
                    'shards': [
                        {'name': 'test_master0'}
                    ]
                }
            }
        })


@pytest.fixture(name='service')
def _service() -> None:
    path_splitted = os.getcwd().split('/')
    service_name = path_splitted[path_splitted.index('services') + 1]
    with tests_setup_utils.setup_and_start(service_name) as port:
        yield {'port': port}

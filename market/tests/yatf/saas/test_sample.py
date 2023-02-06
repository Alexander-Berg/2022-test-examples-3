# coding: utf-8

import pytest
import requests
from market.idx.yatf.test_envs.saas_env import SaasEnv


@pytest.fixture(scope="module")
def saas():
    with SaasEnv() as saas:
        yield saas


def test_saas_ping(saas):
    requests.post(
        'http://{host}:{port}/service/{hash}'.format(
            host=saas.host, port=saas.indexer_port,
            hash=saas.get_service_hash('json_to_rty')
        ),
        json={'prefix': 0, 'action': 'reopen'}
    ).raise_for_status()
    requests.get(
        'http://{host}:{port}/ping'.format(
            host=saas.host, port=saas.search_port
        )
    ).raise_for_status()

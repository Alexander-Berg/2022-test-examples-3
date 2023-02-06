# coding: utf-8
import time

import pytest
import requests
from hamcrest import assert_that, equal_to

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.resources.tokens import StubFile


MONITORING_CHECK = 'shopsdat_freshness'


@pytest.fixture(scope='module')
def shopsdat():
    return StubFile()


@pytest.fixture(scope='module')
def miner_config(shopsdat):
    cfg = MinerConfig()

    http_input = cfg.create_http_processor()
    monitorings = cfg.create_monitorings_http_handler_processor(
        MONITORING_CHECK,
        shopsdat.path,
    )

    cfg.create_link(http_input, monitorings)

    return cfg


@pytest.fixture(scope="module")
def miner(miner_config):
    resources = {
        'miner_cfg': miner_config,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        time.sleep(5)  # wait a bit, while http interface is initializing
        yield miner


def test_monitoring_unknwon_command(miner):
    url = 'http://{host}:{port}/monitoring/unknown'.format(
        host=miner.host,
        port=miner.http_port
    )
    post_request = requests.post(url)
    assert_that(post_request, HasStatus(400))

    get_request = requests.get(url)
    assert_that(get_request, HasStatus(400))


def test_monitoring_freshness(miner):
    url = 'http://{host}:{port}/monitoring/{check}'.format(
        host=miner.host,
        port=miner.http_port,
        check=MONITORING_CHECK,
    )

    post_req = requests.post(url)
    assert_that(post_req, HasStatus(200))
    assert_that(post_req.text, equal_to('0;OK'))

    get_req = requests.get(url)
    assert_that(get_req, HasStatus(200))
    assert_that(get_req.text, equal_to('0;OK'))

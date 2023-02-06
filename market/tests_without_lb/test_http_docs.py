# coding: utf-8
from hamcrest import assert_that
import pytest

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus


@pytest.fixture(scope='module')
def config(yt_server):
    cfg = {
        'general': {
            'color': 'white',
        },
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        config=cfg)


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config):
    resources = {
        'config': config,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


def test_http_swagger(routines_http):
    response = routines_http.get('/swagger')
    assert_that(response, HasStatus(200))


def test_http_docs(routines_http):
    response = routines_http.get('/docs/')
    assert_that(response, HasStatus(200))

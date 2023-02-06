# coding: utf-8

import pytest

from hamcrest import assert_that
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.datacamp.yatf.matchers.matchers import HasStatus


@pytest.fixture(scope='module')
def scanner(log_broker_stuff, yt_server, color, scanner_resources):
    with make_scanner(yt_server, log_broker_stuff, color, **scanner_resources) as scanner_env:
        yield scanner_env


def test_state(scanner):
    result = scanner.get('/state?path=//home/state')
    assert_that(result, HasStatus(404))

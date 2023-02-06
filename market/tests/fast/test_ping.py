# coding: utf-8

import pytest
import six

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
    ) as stroller_env:
        yield stroller_env


def test_ping(stroller):
    resp = stroller.get('/ping')
    assert resp.status_code == 200
    assert six.ensure_str(resp.data) == '0;OK'

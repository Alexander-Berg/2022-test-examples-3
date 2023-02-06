# coding: utf-8

import six
from hamcrest import assert_that
from market.idx.datacamp.yatf.matchers.matchers import HasStatus


def do_get_request(client, url):
    return client.get(url)


def test_monitoring(picrobot_server):
    resp = do_get_request(picrobot_server, '/monitoring')
    assert_that(resp, HasStatus(200))


def test_ping(picrobot_server):
    resp = do_get_request(picrobot_server, '/ping')
    assert resp.status_code == 200
    assert six.ensure_str(resp.data) == '0;OK'


def test_stat(picrobot_server):
    resp = do_get_request(picrobot_server, '/stat')
    assert resp.status_code == 200

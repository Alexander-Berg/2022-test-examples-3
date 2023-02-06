# -*- encoding: utf-8 -*-
import pytest


@pytest.mark.gen_test
def test_ping(http_client, base_url):
    response = yield http_client.fetch(base_url + '/ping')
    assert response.code == 200
    assert response.body.decode('utf8') == 'OK'

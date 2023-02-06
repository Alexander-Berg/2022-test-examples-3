# -*- encoding: utf-8 -*-
import pytest


@pytest.mark.gen_test
def test_version(http_client, base_url):
    response = yield http_client.fetch(base_url + '/version')
    assert response.code == 200

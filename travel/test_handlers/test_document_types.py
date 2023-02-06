# -*- encoding: utf-8 -*-
import pytest


@pytest.mark.gen_test
def test_get(http_client, base_url, header):
    path = '/document_types'
    response = yield http_client.fetch(base_url + path, headers=header)

    assert response.code == 200, response.body

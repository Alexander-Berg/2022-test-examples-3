# -*- encoding: utf-8 -*-
import pytest

from travel.avia.travelers.tests.conftest import USER_UID


@pytest.mark.gen_test
def test_get(http_client, base_url, header, data_sync_client):
    traveler = data_sync_client.get_traveler()
    data_sync_client.set_traveler(traveler)

    path = '/travelers/{}'.format(USER_UID)
    response = yield http_client.fetch(base_url + path, headers=header)

    assert response.code == 200

# -*- encoding: utf-8 -*-
import json
import pytest

from travel.avia.travelers.tests.conftest import USER_UID


@pytest.mark.gen_test
def test_get(http_client, base_url, faker, header):
    path = '/travelers/{}/passengers/{}/bonus-cards'.format(USER_UID, faker.uuid4())
    response = yield http_client.fetch(base_url + path, headers=header)

    assert response.code == 200, response.body


@pytest.mark.gen_test
def test_get_one(http_client, base_url, faker, header):
    path = '/travelers/{}/passengers/{}/bonus-cards/{}'.format(USER_UID, faker.uuid4(), faker.uuid4())
    response = yield http_client.fetch(base_url + path, headers=header)

    assert response.code == 200, response.body


@pytest.mark.gen_test
def test_post(http_client, base_url, faker, header):
    bonus_card = json.dumps(dict(
        number=faker.pystr_format('900##########', letters='0123456789'),
        title='РЖД Бонус',
        type='rzd_bonus',
    ))
    path = '/travelers/{}/passengers/{}/bonus-cards'.format(USER_UID, faker.uuid4())
    response = yield http_client.fetch(
        base_url + path,
        method='POST',
        headers=header,
        raise_error=False,
        body=bonus_card,
    )

    assert response.code == 200, response.body

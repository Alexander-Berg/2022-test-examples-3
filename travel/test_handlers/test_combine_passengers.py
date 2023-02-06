# -*- encoding: utf-8 -*-
import json
import pytest

from travel.avia.travelers.application.schemas import CombinePassengersSchema
from travel.avia.travelers.tests.conftest import USER_UID
from travel.avia.travelers.tests.factory import DictFactory


@pytest.mark.gen_test
def test_get(http_client, base_url, faker, header):
    path = '/travelers/{}/passengers/{}/combine'.format(USER_UID, faker.uuid4())

    params = DictFactory.combine_passengers()
    params = CombinePassengersSchema().dump(params)

    response = yield http_client.fetch(
        base_url + path,
        method='POST',
        headers=header,
        raise_error=False,
        body=json.dumps(params),
    )

    assert response.code == 200, response.body

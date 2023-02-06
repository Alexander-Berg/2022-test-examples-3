# coding: utf-8

import json
import pytest
import requests

from hamcrest import assert_that, equal_to, starts_with

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.api.SyncAddPicture_pb2 import AddPictureResponse
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from google.protobuf.json_format import MessageToDict

from market.idx.pictures.yatf.resources.api import PicrobotApi

SHOP_ID = 1

EXPECTED_PICTURE = {
    'namespace': 'marketpictesting',
    'id': 'xxxdddooo',
    'status': 'AVAILABLE',
    'mdsHost': 'avatars.mds.yandex.net',
    'groupId': '1000',
    'original': {
        'url': '//avatars.mds.yandex.net/get-marketpictesting/1000/market_xxxdddooo/orig',
        'width': 200,
        'containerHeight': 200,
        'containerWidth': 200,
        'height': 200
    }
}


@pytest.fixture(scope='module')
def picrobot_responses():
    picture = {
        'uid': 'xxxdddooo',
        'original': {
            'width': 200,
            'height': 200,
            'url': '//avatars.mds.yandex.net/get-marketpictesting/1000/market_xxxdddooo/orig'
        },
        'group_id': 1000,
    }
    return [
        {'code': requests.codes.ok, 'data': json.dumps(picture)}
    ]


@pytest.fixture(scope='module')
def picrobot_api(picrobot_responses):
    yield PicrobotApi(picrobot_responses)


@pytest.yield_fixture(scope='module')
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    picrobot_api
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        picrobot=picrobot_api
    ) as stroller:
        yield stroller


def do_request_add_picture(client, shop_id, data):
    return client.post('/shops/{}/pictures'.format(shop_id), data=data)


def check_picture(serialized_picture):
    import re
    picture = AddPictureResponse()
    picture.ParseFromString(serialized_picture)
    assert re.match(r'datacamp\.market\.yandex\.ru/pictures/%d/[\w]{22}' % SHOP_ID, picture.fake_url)
    assert MessageToDict(picture.market_picture) == EXPECTED_PICTURE


def test_add_picture(stroller):
    """Проверяем аплоад картинки"""

    image = 'somebinarycontent'

    response = do_request_add_picture(stroller, SHOP_ID, image)

    assert_that(response, HasStatus(200))
    check_picture(response.data)
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))

    # Проверяем, что был сделан запрос в ручку пикробота
    assert_that(stroller.resources['picrobot'].processed_requests[0]['url'],
                starts_with('/market/put_picture?format=json&url=datacamp.market.yandex.ru%2Fpictures%2F{shop_id}%2F'.format(shop_id=SHOP_ID)))

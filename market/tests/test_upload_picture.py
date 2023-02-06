# coding: utf-8
import re
import json
from hamcrest import assert_that, equal_to
from market.idx.datacamp.yatf.matchers.matchers import HasStatus

from market.idx.datacamp.proto.api.SyncAddPicture_pb2 import AddPictureResponse
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC


def do_get_request(client, url):
    return client.get(url)


def do_post_request(client, url, data):
    return client.post(url, data=data)


def check_picture(proto_response, shop_id):
    assert re.match(r'datacamp\.market\.yandex\.ru/pictures/%d/[\w]{22}' % shop_id, proto_response.fake_url)

    market_pic = proto_response.market_picture

    expected_url = '//{}/get-marketpic/{}/{}/orig'.format(market_pic.mds_host, market_pic.group_id, market_pic.id)

    assert_that(market_pic.namespace, equal_to('marketpic'))
    assert_that(market_pic.id, equal_to('pic123'))
    assert_that(market_pic.group_id, equal_to(4827))
    assert_that(market_pic.status, equal_to(DTC.MarketPicture.AVAILABLE))
    assert_that(market_pic.original.width, equal_to(602))
    assert_that(market_pic.original.height, equal_to(1024))
    assert_that(market_pic.original.containerWidth, equal_to(602))
    assert_that(market_pic.original.containerHeight, equal_to(1024))
    assert_that(market_pic.original.alias, equal_to('orig'))
    assert_that(market_pic.original.url, equal_to(expected_url))


def check_state_data(state_response):
    resp_json = json.loads(state_response)
    assert_that(resp_json['MdsInfo'][0]['MdsId']['GroupId'], equal_to(4827))
    assert_that(resp_json['MdsInfo'][0]['MdsId']['ImageName'], equal_to('pic123'))
    assert_that(resp_json['MdsInfo'][0]['MdsId']['Namespace'], equal_to('marketpic'))


def test_upload_new_picture(picrobot_server, yt_server, config):
    """Проверяем, что запрос в ручку пикробота на загрузку картинки возвращает данные об изображении"""
    shop_id = 111
    resp = do_post_request(picrobot_server, '/shops/{}/pictures'.format(shop_id), data='BINARYPICTURE')
    assert_that(resp, HasStatus(200))
    assert_that(resp.headers['Content-type'], equal_to('application/x-protobuf'))

    picture = AddPictureResponse()
    picture.ParseFromString(resp.data)
    check_picture(picture, shop_id)

    # Проверяем, что запись появляется в стейте
    resp = do_get_request(picrobot_server, '/state?url={}'.format(picture.fake_url))
    assert_that(resp, HasStatus(200))
    check_state_data(resp.data)


def test_upload_duplicate_picture(picrobot_server, yt_server, config):
    """Проверяем, что при передаче одной и той же картинки ручка возвращает одну и ту же информацию об изображении"""
    shop_id = 222

    resp1 = do_post_request(picrobot_server, '/shops/{}/pictures'.format(shop_id), data='DUPLICATEDBINARYPICTURE')
    assert_that(resp1, HasStatus(200))
    assert_that(resp1.headers['Content-type'], equal_to('application/x-protobuf'))
    picture1 = AddPictureResponse()
    picture1.ParseFromString(resp1.data)

    resp2 = do_post_request(picrobot_server, '/shops/{}/pictures'.format(shop_id), data='DUPLICATEDBINARYPICTURE')
    assert_that(resp2, HasStatus(200))
    assert_that(resp2.headers['Content-type'], equal_to('application/x-protobuf'))
    picture2 = AddPictureResponse()
    picture2.ParseFromString(resp1.data)

    check_picture(picture1, shop_id)
    check_picture(picture2, shop_id)

    assert_that(picture1.fake_url, equal_to(picture2.fake_url))

    resp = do_get_request(picrobot_server, '/state?url={}'.format(picture1.fake_url))
    assert_that(resp, HasStatus(200))
    check_state_data(resp.data)


def test_invalid_request(picrobot_server, yt_server, config):
    """Проверяем, что запрос c неверными параметрами возвращает ответ с кодом ошибки"""

    # некорректный shop_id
    resp = do_post_request(picrobot_server, '/shops/gg/pictures', data='SOMEBINARYDATA')
    assert_that(resp, HasStatus(400))

    # нет body
    resp = do_post_request(picrobot_server, '/shops/333/pictures', data='')
    assert_that(resp, HasStatus(400))

# coding: utf-8
import json
import six

from hamcrest import assert_that, contains_string, equal_to
from market.idx.datacamp.yatf.matchers.matchers import HasStatus


def do_get_request(client, url):
    return client.get(url)


def do_post_request(client, url, data):
    return client.post(url, data=data)


def test_put_picture(picrobot_server, yt_server, config):
    resp = do_post_request(picrobot_server, '/market/put_picture?url=datacamp.market.yandex.net/lalalal', data='BINARYPICTURE')
    assert_that(resp, HasStatus(200))
    put_result = json.loads(resp.data)
    assert_that(put_result['namespace'], equal_to('marketpic'))
    assert_that(put_result['uid'], equal_to('pic123'))
    assert_that(put_result['group_id'], equal_to(4827))
    assert_that(put_result['original']['url'], equal_to('//avatars.mds.yandex.net/get-marketpic/4827/pic123/orig'))
    assert_that(put_result['original']['width'], equal_to(602))
    assert_that(put_result['original']['height'], equal_to(1024))
    assert_that(put_result['original']['containerWidth'], equal_to(900))
    assert_that(put_result['original']['containerHeight'], equal_to(1200))

    resp = do_get_request(picrobot_server, '/state?url=datacamp.market.yandex.net/lalalal')
    assert_that(resp, HasStatus(200))
    assert_that(six.ensure_str(resp.data), contains_string('"ImageName":"pic123"'))
    assert_that(six.ensure_str(resp.data), contains_string('"Namespace":"marketpic"'))
    assert_that(six.ensure_str(resp.data), contains_string('"Md5":"3acb870+17bc95/fb1d0e179cb71136e"'))
    assert_that(six.ensure_str(resp.data), contains_string('"GroupId":4827'))


def test_no_body(picrobot_server):
    resp = do_post_request(picrobot_server, '/market/put_picture?url=datacamp.market.yandex.net/lalalal', data='')
    assert_that(resp, HasStatus(400))


def test_duplicate_key(picrobot_server):
    resp = do_post_request(picrobot_server, '/market/put_picture?url=datacamp.market.yandex.net/duplicate', data='BINARYPICTURE')
    assert_that(resp, HasStatus(200))
    put_result = json.loads(resp.data)
    assert_that(put_result['original']['url'], equal_to('//avatars.mds.yandex.net/get-marketpic/4827/pic123/orig'))

    resp = do_post_request(picrobot_server, '/market/put_picture?url=datacamp.market.yandex.net/duplicate', data='BINARYPICTURE')
    assert_that(resp, HasStatus(409))
    put_result = json.loads(resp.data)
    assert_that(put_result['original']['url'], equal_to('//avatars.mds.yandex.net/get-marketpic/4827/pic123/orig'))

    resp = do_post_request(picrobot_server, '/market/put_picture?url=datacamp.market.yandex.net/duplicate_empty_mdsinfo', data='BINARYPICTURE')
    assert_that(resp, HasStatus(200))
    put_result = json.loads(resp.data)
    assert_that(put_result['original']['url'], equal_to('//avatars.mds.yandex.net/get-marketpic/4827/pic123/orig'))

    resp = do_post_request(picrobot_server, '/market/put_picture?url=datacamp.market.yandex.net/duplicate_other_namespace', data='BINARYPICTURE')
    assert_that(resp, HasStatus(200))
    put_result = json.loads(resp.data)
    assert_that(put_result['original']['url'], equal_to('//avatars.mds.yandex.net/get-marketpic/4827/pic123/orig'))


def test_no_url(picrobot_server):
    resp = do_post_request(picrobot_server, '/market/put_picture', data='BINARYPICTURE')
    assert_that(resp, HasStatus(400))

# coding: utf-8
import json
import pytest
import six

from hamcrest import assert_that, contains_string, equal_to, not_, is_, has_entries
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import TPicrobotState, TPicrobotStateBatch
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.text_matchers import IsSerializedJson


def do_get_request(client, url):
    return client.get(url)


def do_post_request(client, url, data=""):
    return client.post(url, data=data)


def test_get_state_no_url(picrobot_server):
    resp = do_get_request(picrobot_server, '/state')
    assert_that(resp, HasStatus(400))
    assert_that(six.ensure_str(resp.data), contains_string('Parameter \'url\' is required but cannot be found'))


def test_get_state(picrobot_server):
    resp = do_get_request(picrobot_server, '/state?url=https://rt.tr/beshbarmak.jpg')
    assert_that(resp, HasStatus(200))
    assert_that(six.ensure_str(resp.data), contains_string('"ImageName":"beshbarmak"'))
    assert_that(six.ensure_str(resp.data), contains_string('"Namespace":"marketpic"'))


def test_not_canonized_url(picrobot_server):
    resp = do_get_request(picrobot_server, '/state?url=rt.tr/beshbarmak.jpg')
    assert_that(resp, HasStatus(200))
    assert_that(six.ensure_str(resp.data), contains_string('"ImageName":"beshbarmak"'))
    assert_that(six.ensure_str(resp.data), contains_string('"Namespace":"marketpic"'))


def test_get_state_bad_url(picrobot_server):
    resp = do_get_request(picrobot_server, '/state?url=https://bad.url')
    assert_that(resp, HasStatus(200))
    assert_that(resp.data, IsSerializedJson(has_entries({
        "CopierMeta": [],
        "DeleteMeta": [],
        "DownloadMeta": None,
        "LastRequest": {
            "Offer": {
                "Context": None,
                "OfferId": ""
            }
        },
        "MdsInfo": [],
        "RequestInfo": {
            "Namespaces": [],
            "Requests": []
        }
    })))


def test_get_state_batch(picrobot_server):
    resp = do_post_request(picrobot_server, '/state', data=json.dumps(["https://rt.tr/beshbarmak.jpg", "https://rt.tr/kazylyk.jpg"]))
    assert_that(resp, HasStatus(200))

    data_json = json.loads(resp.data)
    assert_that(len(data_json), equal_to(2))
    assert_that(six.ensure_str(resp.data), contains_string('"ImageName":"beshbarmak"'))
    assert_that(six.ensure_str(resp.data), contains_string('"ImageName":"kazylyk"'))
    assert_that(six.ensure_str(resp.data), contains_string('"OriginalImageUrl":"https://rt.tr/beshbarmak.jpg"'))
    assert_that(six.ensure_str(resp.data), contains_string('"OriginalImageUrl":"https://rt.tr/kazylyk.jpg"'))


@pytest.mark.parametrize('ff', ('', '?ff=MdsInfo&ff=DownloadMeta'))
def test_get_state_batch_with_ff(picrobot_server, ff):
    """ Проверяем фильтрацию полей """
    resp = do_post_request(
        picrobot_server,
        '/state{}'.format(ff),
        data=json.dumps(["https://rt.tr/beshbarmak.jpg", "https://rt.tr/kazylyk.jpg"])
    )
    assert_that(resp, HasStatus(200))

    matcher = not_ if ff else is_
    data_json = json.loads(resp.data)
    assert_that(len(data_json), equal_to(2))
    assert_that(six.ensure_str(resp.data), contains_string('MdsInfo'))
    assert_that(six.ensure_str(resp.data), contains_string('DownloadMeta'))
    assert_that(six.ensure_str(resp.data), matcher(contains_string('RequestInfo')))
    assert_that(six.ensure_str(resp.data), contains_string('"OriginalImageUrl":"https://rt.tr/beshbarmak.jpg"'))
    assert_that(six.ensure_str(resp.data), contains_string('"OriginalImageUrl":"https://rt.tr/kazylyk.jpg"'))


def test_get_state_batch_duplicate_url(picrobot_server):
    resp = do_post_request(picrobot_server, '/state', data=json.dumps(["https://rt.tr/beshbarmak.jpg", "rt.tr/beshbarmak.jpg"]))
    assert_that(resp, HasStatus(200))

    data_json = json.loads(resp.data)
    assert_that(len(data_json), equal_to(1))
    assert_that(six.ensure_str(resp.data), contains_string('"ImageName":"beshbarmak"'))
    assert_that(six.ensure_str(resp.data), contains_string('"OriginalImageUrl":"https://rt.tr/beshbarmak.jpg"'))


def test_get_state_batch_bad_url(picrobot_server):
    resp = do_post_request(picrobot_server, '/state', data=json.dumps(["https://rt.tr/beshbarmak.jpg", "https://bad.url"]))
    assert_that(resp, HasStatus(200))

    data_json = json.loads(resp.data)
    assert_that(len(data_json), equal_to(1))
    assert_that(six.ensure_str(resp.data), contains_string('"ImageName":"beshbarmak"'))
    assert_that(six.ensure_str(resp.data), contains_string('"OriginalImageUrl":"https://rt.tr/beshbarmak.jpg"'))


def test_get_state_batch_all_bad_urls(picrobot_server):
    resp = do_post_request(picrobot_server, '/state', data=json.dumps(["https://bad1.url", "https://bad2.url"]))
    assert_that(resp, HasStatus(200))

    data_json = json.loads(resp.data)
    assert_that(len(data_json), equal_to(0))
    assert_that(six.ensure_str(resp.data), equal_to('[]'))


def test_get_state_batch_invalid_request(picrobot_server):
    # Empty body
    resp = do_post_request(picrobot_server, '/state')
    assert_that(resp, HasStatus(400))

    # Invalid json
    resp = do_post_request(picrobot_server, '/state', data='["ulr1.jpg", "url2.jpg")')
    assert_that(resp, HasStatus(400))

    # Empty urls list
    resp = do_post_request(picrobot_server, '/state', data=json.dumps([]))
    assert_that(resp, HasStatus(400))


ALL_NAMESPACES = ['marketpic', 'mrkt_idx_direct', 'yabs_performance']


def test_get_state_proto(picrobot_server):
    resp = do_get_request(picrobot_server, '/state?url=https://rt.tr/beshbarmak.jpg&format=protobuf')
    assert_that(resp, HasStatus(200))
    assert_that(resp.data, IsSerializedProtobuf(TPicrobotState, {
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'beshbarmak'
                }
            } for ns in ALL_NAMESPACES
        ]
    }))


def test_get_state_batch_proto(picrobot_server):
    resp = do_post_request(picrobot_server, '/state?format=protobuf', data=json.dumps(["https://rt.tr/beshbarmak.jpg", "https://rt.tr/kazylyk.jpg"]))
    assert_that(resp, HasStatus(200))
    assert_that(resp.data, IsSerializedProtobuf(TPicrobotStateBatch, {
        'States': IsProtobufMap({
            'https://rt.tr/kazylyk.jpg': {
                'MdsInfo': [
                    {
                        'MdsId': {
                            'Namespace': ns,
                            'GroupId': 1,
                            'ImageName': 'kazylyk'
                        }
                    } for ns in ALL_NAMESPACES
                ]
            },
            'https://rt.tr/beshbarmak.jpg': {
                'MdsInfo': [
                    {
                        'MdsId': {
                            'Namespace': ns,
                            'GroupId': 1,
                            'ImageName': 'beshbarmak'
                        }
                    } for ns in ALL_NAMESPACES
                ]
            }
        })
    }))

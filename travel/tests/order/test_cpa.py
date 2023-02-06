# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import base64

import httpretty
import mock
from django.conf import settings
from hamcrest import assert_that, has_properties


from travel.proto.suburban.cpa_label_pb2 import TSuburbanCpaLabel

from travel.rasp.suburban_selling.selling.order.cpa import get_label_hash_from_redir, make_serialized_label_for_redir


class CreateOrderRequestStub(object):
    META = {
        'HTTP_X_YA_UUID': 'uuid',
        'HTTP_X_YA_DEVICE_ID': 'device_id',
        'HTTP_X_TEST_BUCKETS': 'test_buckets',
        'HTTP_USER_AGENT': 'App: Suburban 10.10(1010); OS: Android 11.2; Lib: okhttp/hhh',
        'HTTP_X_DEVICE': 'device',
        'HTTP_X_REAL_IP': 'ip',
        'HTTP_X_CPA_EXTRA_DATA': '{"extra": "data"}'
    }


class CreateOrderEmptyRequestStub(object):
    META = {
        'HTTP_X_YA_UUID': 'uuid',
        'HTTP_USER_AGENT': 'wrong format'
    }


def _register_label(label, status_code, body=''):
    def request_callback(request, uri, response_headers):
        return [status_code, response_headers, body]

    httpretty.register_uri(
        httpretty.GET, uri='{}suburban/label_to_hash/'.format(settings.REDIR_URL), params={'LabelParams': label},
        status=status_code, body=request_callback
    )


def test_make_serialized_label_for_redir():
    serialized_label = make_serialized_label_for_redir(CreateOrderRequestStub(), 'passport_uid')
    label = TSuburbanCpaLabel.FromString(base64.b64decode(serialized_label))
    assert_that(label, has_properties({
        'Uuid': 'uuid',
        'DeviceId': 'device_id',
        'PassportUid': 'passport_uid',
        'TestBuckets': 'test_buckets',
        'OsType': 'Android',
        'OsVersion': '11.2',
        'AppVersion': '10.10',
        'AppCodeVersion': '1010',
        'Device': 'device',
        'Ip': 'ip',
        'CpaExtraData': '{"extra": "data"}'
    }))

    serialized_label = make_serialized_label_for_redir(CreateOrderEmptyRequestStub(), None)
    label = TSuburbanCpaLabel.FromString(base64.b64decode(serialized_label))

    assert_that(label, has_properties({
        'Uuid': 'uuid',
        'DeviceId': '',
        'PassportUid': '',
        'TestBuckets': '',
        'OsType': '',
        'OsVersion': '',
        'AppVersion': '',
        'AppCodeVersion': '',
        'Device': '',
        'Ip': '',
        'CpaExtraData': ''
    }))


@httpretty.activate
def test_get_label_hash_from_redir():
    _register_label('label', 200, 'some_hash')
    label_hash = get_label_hash_from_redir(CreateOrderRequestStub(), 'passport_uid')
    assert label_hash == 'some_hash'


@httpretty.activate
def test_get_label_hash_from_redir_error():
    _register_label('label', 500)
    with mock.patch('travel.rasp.suburban_selling.selling.order.cpa.get_events') as m_get_events:
        with mock.patch('travel.rasp.suburban_selling.selling.order.cpa.send_events') as m_send_events:
            label_hash = get_label_hash_from_redir(CreateOrderRequestStub(), 'passport_uid')

    assert label_hash == 'ERROR_REDIR_BAD_RESPONSE'
    assert m_get_events.call_count == 1
    assert m_send_events.call_count == 1

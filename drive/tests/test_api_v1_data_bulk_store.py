# WARNING: REDIS DOES NOT FLUSH ITSELF AFTER TEST

import http.client
import json
import time

import pytest

SERVICE_HOST = 'localhost'
BULK_STORE_ENDPOINT = '/api/telematics-cache-api/v1/data/bulk-store'
NOW_UNIX_TIMESTAMP = int(time.time())


def test_data_bulk_store(service):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    request_body = {
        'data_array': [
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP,
                'data_type': 'location',
                'imei': 'imei',
                'location_name': 'some_name_111',
            },
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP+100000,
                'data_type': 'location',
                'imei': 'imei',
                'location_name': 'some_name_222',
            },
        ],
    }
    conn.request('POST', BULK_STORE_ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 1,
        }
        assert resp.read() == str.encode(json.dumps(response_body))


@pytest.mark.parametrize(
    'request_body',
    [
        (
            {
                'data': ['kek'],
            },
        ),
        (
            {
                'data_array': {
                    'some_field': 'some_value',
                },
            },
        ),
        (
            {
                'data_array': {
                    'unix_timestamp': 12312412,
                    'imei': 'imei',
                    'data_type': 'fake_data_type',
                    'data_proto': 'binary_data',
                },
            },
        ),
        (
            {
                'data_array': {
                    'unix_timestamp': 12312412,
                    'imei': 'imei',
                    'data_type': 'heartbeat',
                    'data_proto': 'binary_data',
                },
            },
        ),
    ],
)
def test_data_bulk_store_400(service, request_body):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    conn.request('POST', BULK_STORE_ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == 400


def test_data_bulk_store_multiple_batches(service):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    request_body = {
        'data_array': [
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP,
                'data_type': 'heartbeat',
                'imei': 'imei',
            },
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP,
                'data_type': 'heartbeat',
                'imei': 'imei',
                'heartbeat_name': 'some_name_2222',
            },
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP,
                'data_type': 'heartbeat',
                'imei': 'imei',
                'heartbeat_name': 'some_name_3333',
            },
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP,
                'data_type': 'heartbeat',
                'imei': 'imei',
                'heartbeat_name': 'some_name_4444',
            },
        ],
    }
    conn.request('POST', BULK_STORE_ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 4,
        }
        assert resp.read() == str.encode(json.dumps(response_body))

    request_body = {
        'data_array': [
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP+10,
                'data_type': 'heartbeat',
                'imei': 'imei',
            },
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP+10,
                'data_type': 'heartbeat',
                'imei': 'imei',
                'heartbeat_name': 'some_name_2222',
            },
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP-100,
                'data_type': 'heartbeat',
                'imei': 'imei',
                'heartbeat_name': 'some_name_3333',
            },
            {
                'data_proto': 'some_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP+10,
                'data_type': 'heartbeat',
                'imei': 'imei',
                'heartbeat_name': 'some_name_4444',
            },
        ],
    }
    conn.request('POST', BULK_STORE_ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 3,
        }
        assert resp.read() == str.encode(json.dumps(response_body))

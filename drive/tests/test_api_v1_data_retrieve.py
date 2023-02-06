# WARNING: REDIS DOES NOT FLUSH ITSELF AFTER TEST

import http.client
import json
import time

import pytest

NOW_UNIX_TIMESTAMP = int(time.time())
BULK_STORE_ENDPOINT = '/api/telematics-cache-api/v1/data/bulk-store'
RETRIEVE_ENDPOINT = '/api/telematics-cache-api/v1/data/retrieve'
SERVICE_HOST = 'localhost'


def test_data_retrieve_flow(service):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    retrieve_request_body = {
        'data_type': 'sensor',
        'imeis': ['imei1', 'imei2'],
        'sensor_ids': [
            {
                'id': 1,
                'sub_id': 10,
            },
            {
                'id': 2,
                'sub_id': 20,
            },
        ],
    }
    conn.request('POST', RETRIEVE_ENDPOINT, body=json.dumps(retrieve_request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'data_proto_array': [],
        }
        assert json.loads(resp.read()) == response_body

    bulk_store_request_body = {
        'data_array': [
            {
                'data_proto': 'encoded_binary_data_1',
                'unix_timestamp': NOW_UNIX_TIMESTAMP,
                'data_type': 'sensor',
                'imei': 'imei1',
                'sensor_id': {
                    'id': 1,
                    'sub_id': 10,
                },
            },
            {
                'data_proto': 'encoded_binary_data_4',
                'unix_timestamp': NOW_UNIX_TIMESTAMP,
                'data_type': 'sensor',
                'imei': 'imei2',
                'sensor_id': {
                    'id': 2,
                    'sub_id': 20,
                },
            },
        ],
    }
    conn.request('POST', BULK_STORE_ENDPOINT, body=json.dumps(bulk_store_request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 2,
        }
        assert resp.read() == str.encode(json.dumps(response_body))

    conn.request('POST', RETRIEVE_ENDPOINT, body=json.dumps(retrieve_request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'data_proto_array': [
                {
                    'imei': 'imei1',
                    'data_proto': 'encoded_binary_data_1',
                },
                {
                    'imei': 'imei2',
                    'data_proto': 'encoded_binary_data_4',
                },
            ],
        }
        assert json.loads(resp.read()) == response_body


@pytest.mark.parametrize(
    'data_type, imeis, location_names',
    [
        (None, None, None),
        ('fake_type', ['imei1'], ['name1']),
        ('location', None, ['name1']),
        ('location', [], ['name1']),
        ('location', ['imei1'], None),
        ('location', ['imei1'], []),
    ],
)
def test_data_retrieve_400(service, data_type, imeis, location_names):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    retrieve_request_body = {}
    if data_type is not None:
        retrieve_request_body['data_type'] = data_type
    if imeis is not None:
        retrieve_request_body['imeis'] = imeis
    if location_names is not None:
        retrieve_request_body['location_names'] = location_names
    conn.request('POST', RETRIEVE_ENDPOINT, body=json.dumps(retrieve_request_body))
    with conn.getresponse() as resp:
        assert resp.status == 400


def test_data_retrieve_heartbeat_with_empty_name(service):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    bulk_store_request_body = {
        'data_array': [
            {
                'data_proto': 'encoded_binary_data',
                'unix_timestamp': NOW_UNIX_TIMESTAMP,
                'data_type': 'heartbeat',
                'imei': 'test_imei',
            },
        ],
    }
    conn.request('POST', BULK_STORE_ENDPOINT, body=json.dumps(bulk_store_request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 1,
        }
        assert resp.read() == str.encode(json.dumps(response_body))

    retrieve_request_body = {
        'data_type': 'heartbeat',
        'imeis': ['test_imei'],
    }
    conn.request('POST', RETRIEVE_ENDPOINT, body=json.dumps(retrieve_request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'data_proto_array': [
                {
                    'imei': 'test_imei',
                    'data_proto': 'encoded_binary_data',
                },
            ],
        }
        assert json.loads(resp.read()) == response_body

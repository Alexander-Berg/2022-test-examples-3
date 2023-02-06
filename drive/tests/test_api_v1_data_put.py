# WARNING: REDIS DOES NOT FLUSH ITSELF AFTER TEST

import http.client
import json
import time

import pytest

SERVICE_HOST = 'localhost'
ENDPOINT = '/api/telematics-cache-api/v1/data'
NOW_UNIX_TIMESTAMP = int(time.time())


@pytest.mark.parametrize(
    'unix_timestamp, expected_code',
    [
        (NOW_UNIX_TIMESTAMP, 200),
        (NOW_UNIX_TIMESTAMP + 100500, 400),
    ],
)
def test_data_put(
    service,
    unix_timestamp,
    expected_code,
):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    request_body = {
        'data_proto': 'some_binary_data',
        'unix_timestamp': unix_timestamp,
        'key': 'some_key',
    }
    conn.request('PUT', ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == expected_code
        if expected_code == 400:
            return
        response_body = {
            'written': 1,
        }
        assert resp.read() == str.encode(json.dumps(response_body))


def test_data_put_flow(service):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    request_body = {
        'data_proto': 'some_binary_data',
        'unix_timestamp': NOW_UNIX_TIMESTAMP,
        'key': 'some_key_1',
    }
    conn.request('PUT', ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 1,
        }
        assert resp.read() == str.encode(json.dumps(response_body))

    request_body['unix_timestamp'] = NOW_UNIX_TIMESTAMP + 10
    conn.request('PUT', ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 1,
        }
        assert resp.read() == str.encode(json.dumps(response_body))

    conn.request('PUT', ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 0,
        }
        assert resp.read() == str.encode(json.dumps(response_body))

    request_body.pop('data_proto')
    conn.request('PUT', ENDPOINT, body=json.dumps(request_body))
    with conn.getresponse() as resp:
        assert resp.status == 400
        assert resp.read() == b"Failed to parse request_body: Field 'data_proto' is missing"

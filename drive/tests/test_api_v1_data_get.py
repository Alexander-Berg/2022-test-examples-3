# WARNING: REDIS DOES NOT FLUSH ITSELF AFTER TEST

import http.client
import json
import time

SERVICE_HOST = 'localhost'
ENDPOINT = '/api/telematics-cache-api/v1/data'
NOW_UNIX_TIMESTAMP = int(time.time())


def test_data_get(service):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    conn.request('GET', ENDPOINT + '?key=some_key_1337')
    with conn.getresponse() as resp:
        assert resp.status == 404

    put_request_body = {
        'data_proto': 'some_binary_data',
        'unix_timestamp': NOW_UNIX_TIMESTAMP,
        'key': 'some_key_1337',
    }
    conn.request('PUT', ENDPOINT, body=json.dumps(put_request_body))
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'written': 1,
        }
        assert resp.read() == str.encode(json.dumps(response_body))

    conn.request('GET', ENDPOINT + '?key=some_key_1337')
    with conn.getresponse() as resp:
        assert resp.status == 200
        response_body = {
            'data_proto': 'some_binary_data',
        }
        assert resp.read() == str.encode(json.dumps(response_body))

import http.client

SERVICE_HOST = 'localhost'
ENDPOINT = '/api/telematics-cache-api/v1/test-data'


def test_userver_sample_redis(service, redis):
    conn = http.client.HTTPConnection(SERVICE_HOST, port=service['port'])

    conn.request('PUT', ENDPOINT + '?key=hello&value=world')
    with conn.getresponse() as resp:
        assert resp.status == 200
        assert resp.read() == b'world'

    conn.request('GET', ENDPOINT + '?key=some_fake_key')
    with conn.getresponse() as resp:
        assert resp.status == 404

    conn.request('GET', ENDPOINT + '?key=hello')
    with conn.getresponse() as resp:
        assert resp.status == 200
        assert resp.read() == b'world'

    conn.request('DELETE', ENDPOINT + '?key=hello')
    with conn.getresponse() as resp:
        assert resp.status == 200

    conn.request('GET', ENDPOINT + '?key=hello')
    with conn.getresponse() as resp:
        assert resp.status == 404

import http.client

import sample_utils as sample

SERVICE_HOST = 'localhost'


def test_userver_sample_flatbuf() -> None:
    with sample.setup_and_start('flatbuf_service', rewrite_port=8084) as port:
        conn = http.client.HTTPConnection(SERVICE_HOST, port)
        body = bytearray.fromhex(
            '100000000c00180000000800100004000c000000140000001400000000000000'
            '16000000000000000a00000048656c6c6f20776f72640000',
        )
        conn.request('POST', '/fbs', body=body)
        with conn.getresponse() as resp:
            assert resp.status == 200

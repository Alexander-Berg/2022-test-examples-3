#!/usr/bin/env python2

import os
import xiva_api
import json
import time
import msgpack
from pprint import pprint

client = xiva_api.XivaApiV2(
    host='push-sandbox.yandex.ru', port=443, secure=True)

resp = client.secret_sign(uid=1, service='messenger',
                          token=os.environ['TOKEN'])
assert resp.code == 200
secrets = json.loads(resp.body)

# headers = {'X-Rproxy-Url': 'iva8-420ef85308da.qloud-c.yandex.net:9999'}
headers = {}
(resp, ws) = client.subscribe_websocket(headers,
                                        uid=1, service='messenger', sign=secrets['sign'],
                                        ts=secrets['ts'], client='test_rproxy', session='A')
assert resp.code == 101
print resp.body

msg = ws.recv_message()
print msg.payload

TYPE_DATA = 1
reqid = 1
ws.send_binary_message('\x01' + msgpack.packb([0, reqid]) + '\xAA\xBB\xCC\xDD\xEE\xFF')

time.sleep(1)
msg = ws.recv_message()
assert msg != None
assert msg.opcode == 2
print msg.payload[0], msgpack.unpackb(msg.payload[1:])

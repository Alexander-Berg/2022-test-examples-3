from pycommon.asserts import *
from pycommon.fake_server import *
import msgpack
import io
import json
import numbers
from urlparse import urlparse
from urlparse import parse_qs
from bisect import insort

def fake_hub(host, port):
    handler = FakeHubRequestHandler
    print host
    print port
    hub_server = make_server(handler, host=host, port=port, raw_response='OK')
    return hub_server

class FakeHubRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_ANY(self):
        url = urlparse(self.path)
        params = parse_qs(url.query)
        response = None
        if url.path.startswith('/batch_list_json'):
            services = set(parse_qs(self.body)['services'][0].split(','))
            self.server.listed = set(services)
            resp = self.server.list_json_response
            if isinstance(resp, dict):
                list_resp = []
                for service in services:
                    if service in resp:
                        for item in resp[service]:
                            resp_item = item.copy()
                            resp_item['service'] = service
                            list_resp.append(resp_item)
                resp = list_resp
            response = json.dumps(resp)
        elif url.path.startswith('/batch_binary_notify'):
            svc = params['service'][0]
            unpacker = msgpack.Unpacker(io.BytesIO(self.body))
            msg = unpacker.unpack()
            recipients = unpacker.unpack()
            self.server.batch_msg[svc] = msg
            if not (svc in self.server.notified):
                self.server.notified[svc] = []
            for rec in recipients:
                insort(self.server.notified[svc], rec)
            resp = self.server.batch_binary_notify_response[svc]
            if isinstance(resp, numbers.Number):
                response = resp
            else:
                if isinstance(resp, dict):
                    batch_binary_notify_response = []
                    for key,value in resp.items():
                        batch_binary_notify_response.append({key:value})
                    resp = batch_binary_notify_response

                response_data = []
                for i in range(0, len(recipients)):
                    uid = recipients[i][0]
                    subid = recipients[i][1]
                    for item in resp:
                        mkey,mvalue = item.items()[0]
                        if mkey == uid and (len(subid) == 0 or subid == mvalue[2]):
                            response_data.append([i] + mvalue)
                response = msgpack.packb(response_data)
        elif url.path.startswith('/batch_unsubscribe'):
            svc = params['service'][0]
            self.server.unsubscribed[svc] = set(parse_qs(self.body)['ids'][0].split(','))
            response = self.server.unsubscribe_response[svc]

        if response:
            self.protocol_version = 'HTTP/1.1'
            if isinstance(response, numbers.Number):
                self.send_response(response, '')
                self.send_header('Content-Length', 0)
                response = ''
            else:
                self.send_response(200, 'OK')
                self.send_header('Content-Length', len(response))
            self.send_header('Connection', 'close')
            self.send_header('y-context', 'test_ctx')
            self.end_headers()
            self.wfile.write(response)
        else:
            self.protocol_version = 'HTTP/1.1'
            self.send_response(404, 'NotFound')
            self.send_header('Content-Length', 0)
            self.send_header('Connection', 'close')
            self.end_headers()
        return ''


    def do_POST(self):
        self.body = self.rfile.read(int(self.headers.getheader('content-length', 0)))
        return self.do_ANY()

    def do_GET(self):
        return self.do_ANY()

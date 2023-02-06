from pycommon import *
import BaseHTTPServer
import msgpack
import sys
import socket
from time import sleep

class BatchDelayRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_POST(self):
        sleep(0.05)
        self.protocol_version = 'HTTP/1.1'
        self.send_response(500, 'OK')
        self.send_header('Content-Length', 0)
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write('')
        return ''
    def log_message(self, format, *args):
        return

class TestBatchSendNegative:
    def setup(self):
        global xiva, hub_server, handler
        xiva = XivaApiV2(host='localhost', port=18083)
        hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
        hub_server.shared_data = {}

    def teardown(self):
        hub_server.fini()

    def raw_batch_send(self, body):
        return xiva.POST('/v2/batch_send?token=S001&event=test', body=body)

    def check_400_response(self, test_req, test_req_body, test_resp):
        resp = xiva.POST(test_req, body=test_req_body)
        assert_bad_request(resp, test_resp)

    def test_bad_request(self):
        std_path = '/v2/batch_send?token=S001&event=test'
        test_cases_400 = [
            ('/v2/batch_send', None, 'missing argument "event"'),
            ('/v2/batch_send?token=S001', None, 'missing argument "event"'),
            (std_path, None, 'no data'),
            (std_path, '{"payload":"text"}', 'no recipients'),
            (std_path, '{"payload":"text", "recipients":""}', 'no recipients'),
            (std_path, '{"payload":"text", "recipients":{}}', 'no recipients'),
            (std_path, '{"payload":"text", "recipients":{"a":"b"}}', 'no recipients'),
            (std_path, '{"payload":"text", "recipients":[]}', 'no recipients'),
            (std_path, '{"payload":"text", "recipients":[]}', 'no recipients'),
            (std_path, '{"payload":"text", "recipients":[""]}', 'invalid recipient ""'),
            (std_path, '{"payload":"text", "recipients":[{}]}', 'invalid recipient type'),
            (std_path, '{"payload":"text", "recipients":[[]]}', 'invalid recipient type'),
            (std_path, '{"payload":"text", "recipients":["1",""]}', 'invalid recipient ""'),
            (std_path, '{"payload":"text", "recipients":["1", "", "2"]}', 'invalid recipient ""'),
            (std_path, '{"payload":"text", "recipients":["1", "$"]}', 'invalid recipient "$"'),
            (std_path, '{"payload":"text", "recipients":[{"":"id"}]}', 'invalid recipient ""'),
        ]

        for (req, req_body, resp) in test_cases_400:
            yield self.check_400_response, req, req_body, resp

        for symbol in "$%!:;.,":
            yield self.check_400_response, std_path, '{"payload":"text", "recipients":["' + symbol + '"]}', 'invalid recipient "' + symbol + '"'

        resp = self.raw_batch_send('ADFHDRSTFCVDGWASDFGSRTBVAWTBHE')
        eq_(resp.status, 400)
        assert_in('error while parsing json', resp.body)

        recipients = []
        for i in range(0,10001):
            recipients.append(str(i))
        resp = self.raw_batch_send('{"payload":"text", "recipients":'+json.dumps(recipients)+'}')
        assert_bad_request(resp, 'too many recipients')

    def test_unauthorized(self):
        resp = xiva.POST('/v2/batch_send?event=test')
        assert_unauthorized(resp)

    def test_500_from_hub_for_all_recipients(self):
        hub_server.response.code = 500
        resp = self.raw_batch_send('{"payload":"text", "recipients":["1","2"]}')
        eq_(resp.status, 200)
        eq_(json.loads(resp.body), json.loads('{"results":[{"body":"internal error","code":500},{"body":"internal error","code":500}]}'))


    def test_500_from_hub_with_duplicates(self):
        hub_server.response.code = 500
        resp = self.raw_batch_send('{"payload":"text", "recipients":[{"1":"A"},"1"]}')
        eq_(resp.status, 200)
        eq_(json.loads(resp.body), json.loads('{"results":[{"body":{"duplicate":1},"code":409},{"body":"internal error","code":500}]}'))


    def test_trash_from_hub_for_all_recipients(self):
        hub_server.response.code = 200
        hub_server.response.body = 'aeuirfjamdjbgiuajwkfoanebrfu'
        resp = self.raw_batch_send('{"payload":"text", "recipients":["1","2"]}')
        eq_(resp.status, 200)
        eq_(json.loads(resp.body), json.loads('{"results":[{"body":"internal error","code":500},{"body":"internal error","code":500}]}'))

    def test_400_from_hub_for_all_recipients(self):
        hub_server.response.code = 400
        hub_server.response.body = 'bad request'
        resp = self.raw_batch_send('{"payload":"text", "recipients":["1","2"]}')
        eq_(resp.status, 200)
        eq_(json.loads(resp.body), json.loads('{"results":[{"body":"internal error","code":500},{"body":"internal error","code":500}]}'))

class TestBatchSendTransitID:
    def setup(self):
        global xiva, hub_server, handler
        xiva = XivaApiV2(host='localhost', port=18083)
        handler = BatchDelayRequestHandler
        hub_server = make_server(handler, host='localhost', port=17081, raw_response='OK')
        hub_server.shared_data = {}
    def teardown(self):
        hub_server.fini()

    def args(self):
        return {'token': "S001", 'event': 'test'}

    def test_receive_transit_id_at_request_start(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect(('localhost', 18083))
        body = json.dumps({"payload": "test", "recipients": ["1","2"]})

        s.sendall('POST ' + xiva.prepare_url('/v2/batch_send?', **self.args())
            + ' HTTP/1.1\r\nContent-Length:' + str(len(body)) + '\r\n\r\n' + body + '\r\n')

        s.settimeout(0.2)
        data = s.recv(1024)
        headers = {}
        for line in data.split('\r\n'):
            splitted = line.split(':')
            if len(splitted) > 1:
                headers[splitted[0]] = splitted[1]
        assert_in('TransitID', headers)
        assert_not_equals(headers['TransitID'], "")
        sleep(0.1)
        data = s.recv(1024)
        assert_greater(len(data), 0)
        s.close()

from pycommon import *
import json
import base64

def setUp(self):
    global xiva, hub_server
    xiva = XivaApiV2(host='localhost', port=18085, secure=True)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    global hub_server
    hub_server.fini()

class TestSend:
    def setup(self):
        resp = xiva.GET("/v2/vapid_key", {})
        self.public_key = resp.body
        self.uuid = "test_instance_id"

        hub_server.set_response(raw_response="[]")
        self.webpushapi = XivaWebPushApi(host='localhost', port=18085, secure=True)
        self.webpushapi.connect()
        resp = self.webpushapi.subscribe()
        self.subscription = resp["subscription"]
        self.push_resource = resp["push_resource"]
        self.push_resource = self.push_resource[self.push_resource.rfind('/')+1:]

        self.requests = []
        hub_server.set_request_hook(self.request_hook)

    def request_hook(self, req):
        self.requests.append(req.path.split('?')[0])

    def decode_uid(self, sub):
        ret = sub[::-1]
        missing_padding = len(ret) % 4
        if missing_padding > 0:
            ret += b'='* (4 - missing_padding)
        return base64.b64decode(ret)

    def test_send_negative(self):
        resp = xiva.POST("/webpushapi/send/", {})
        assert_not_found(resp)

        resp = xiva.POST("/webpushapi/send/a", {})
        assert_bad_request(resp)

        resp = xiva.POST("/webpushapi/send/a", {
            "Content-Encoding":"aesgcm"
        })
        assert_not_found(resp)

        resp = xiva.POST("/webpushapi/send/" + self.push_resource, {
            "Content-Encoding":"aesgcm"
        })
        assert_gone(resp)

        hub_response = [{
            "uid" : self.decode_uid(self.subscription),
            "id" : "id",
            "session_key" : "fake",
        }]

        hub_server.set_response(raw_response=json.dumps(hub_response))
        resp = xiva.POST("/webpushapi/send/" + self.push_resource, {
            "Content-Encoding":"aesgcm"
        })
        assert_unauthorized(resp)



    def test_external_request_id(self):
        resp = xiva.POST("/webpushapi/send/" + self.push_resource, {
            "Content-Encoding":"aesgcm",
            "X-Request-ID":"TESTID"
        })
        assert_in('transitid', resp.headers)
        assert_in('TESTID', resp.headers['transitid'])
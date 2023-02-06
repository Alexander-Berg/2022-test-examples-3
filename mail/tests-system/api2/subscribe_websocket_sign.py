from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.hub import *
from pycommon.fake_server import *
import time

class TestSubscribeWebsocketWithSign:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()

    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH", "format": "json"
        }
        self._sign_uid_service = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1" })
        self._sign_uid_two_services = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1,tst2" })
        self._sign_uid_two_services_multiauth = self.xiva_client.secret_sign(**{ "token" : "L123456,L001", "uid" : "200", "service" : "autoru,tst2" })
        self._sign_two_uids_two_services = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200,300", "service" : "tst1,tst2" })
        self._sign_uid_topic_service = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "topic" : "mytopic", "service" : "tst1" })

    def teardown(self):
        error_in_hook = self.hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def conn_stat(self):
        stat = self.xiva_client.stat()
        return int(stat['stat']['modules']['processor']['subscribers'])

    def service_conn_stat(self, service):
        stat = self.xiva_client.stat()
        return int(stat['stat']['modules']['processor']['service_subscribers'][service])

    def test_with_no_arguments_fails_with_4400(self):
        (resp, ws) = self.xiva.subscribe_websocket()
        assert_ws_bad_request(resp, ws, 'missing argument "user (uid)"')

    def test_without_sign_fails_with_4401(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args())
        assert_ws_unauthorized(resp, ws)

    def test_bad_sign_fails_with_4401(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(sign="a", ts="99999999999"))
        assert_ws_unauthorized(resp, ws)

    def test_with_expired_ts_fails_with_4401(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(sign=self._sign_uid_service["sign"], ts="111"))
        assert_ws_unauthorized(resp, ws)

    def test_success_with_correct_args(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_ws_ok(resp, ws)

    def test_success_with_user_arg_instead_of_uid(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="", user="200", **self._sign_uid_service))
        assert_ws_ok(resp, ws)

    def test_ping_is_sent_on_success(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        ws_message = ws.recv_message()
        assert_equals(ws_message.status_code, 0)
        parsed_msg = json.loads(ws_message.payload)
        assert_in("operation", parsed_msg)
        assert_equals(parsed_msg["operation"], "ping")
        assert_in("server-interval-sec", parsed_msg)

    def test_subscribed_is_sent_on_success(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        ws_message = ws.recv_message() # skip ping
        ws_message = ws.recv_message()
        assert_equals(ws_message.status_code, 0)
        parsed_msg = json.loads(ws_message.payload)
        assert_in("event", parsed_msg)
        assert_in("operation", parsed_msg)
        assert_equals(parsed_msg["event"], "subscribed")
        assert_equals(parsed_msg["operation"], "subscribed")

    def test_subscribed_is_sent_on_success_multiauth(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service="autoru,tst2", **self._sign_uid_two_services_multiauth))
        time.sleep(0.15)
        assert_equals(resp.code, 101)
        ws_message = ws.recv_message() # skip ping
        ws_message = ws.recv_message()
        assert_equals(ws_message.status_code, 0)
        parsed_msg = json.loads(ws_message.payload)
        assert_in("event", parsed_msg)
        assert_in("operation", parsed_msg)
        assert_equals(parsed_msg["event"], "subscribed")
        assert_equals(parsed_msg["operation"], "subscribed")

    def test_success_changes_status(self):
        assert_equals(self.conn_stat(), 0)
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        time.sleep(0.15)
        assert_equals(self.conn_stat(), 1)
        assert_equals(self.service_conn_stat('tst1'), 1)

    def test_success_with_two_services_increments_status_twice(self):
        assert_equals(self.conn_stat(), 0)
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service="tst1,tst2", **self._sign_uid_two_services))
        time.sleep(0.15)
        assert_equals(self.conn_stat(), 2)
        assert_equals(self.service_conn_stat('tst1'), 1)
        assert_equals(self.service_conn_stat('tst2'), 1)

    def test_success_with_two_services_increments_status_twice_multiauth(self):
        assert_equals(self.conn_stat(), 0)
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service='autoru,tst2', **self._sign_uid_two_services_multiauth))
        time.sleep(0.15)
        assert_equals(self.conn_stat(), 2)
        assert_equals(self.service_conn_stat('autoru'), 1)
        assert_equals(self.service_conn_stat('tst2'), 1)

    def test_no_connections_in_status_when_disconnect(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service="tst1,tst2", **self._sign_uid_two_services))
        (resp, ws) = (None, None)
        time.sleep(0.15)
        assert_equals(self.conn_stat(), 0)
        assert_equals(self.service_conn_stat('tst1'), 0)
        assert_equals(self.service_conn_stat('tst2'), 0)

    def test_no_connections_in_status_when_disconnect_multiauth(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service='autoru,tst2', **self._sign_uid_two_services_multiauth))
        (resp, ws) = (None, None)
        time.sleep(0.15)
        assert_equals(self.conn_stat(), 0)
        assert_equals(self.service_conn_stat('autoru'), 0)
        assert_equals(self.service_conn_stat('tst2'), 0)

    def test_notification_should_be_delivered_for_this_user(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        ws_message = ws.recv_message()
        notification = [b'200', b'', b'tst1', b'', b'', b'', {b'\rmethod_id': b'', b'\rsz': b'1825'}, b'1234567890', True, b'fLJU600KTa61', 3, 1470054101, []]
        self.xiva_client.notify("tst1", notification)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        payload = json.loads(ws_message.payload)
        assert_equals(payload['uid'], '200')
        assert_equals(payload['service'], 'tst1')

    def test_no_notification_on_user_mismatch(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        ws_message = ws.recv_message()
        notification = [b'300', b'', b'tst1', b'', b'', b'', {b'\rmethod_id': b'', b'\rsz': b'1825'}, b'1234567890', True, b'fLJU600KTa61', 3, 1470054101, []]
        self.xiva_client.notify("tst1", notification)
        ws_message = ws.recv_message() # subscribed
        ws_message = ws.recv_message() # skip
        assert_equals(ws_message, None)

    def test_returns_bad_sign_on_uid_list_mismatch(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200,300,400", **self._sign_two_uids_two_services))
        assert_ws_unauthorized(resp, ws, "bad sign")

    def test_success_on_uid_list_reordered(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="300,200", service="tst1,tst2", **self._sign_two_uids_two_services))
        assert_ws_ok(resp, ws)

    def test_fails_with_topic_and_multiple_uids(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="300,200", topic="mytopic", service="tst1", **self._sign_uid_topic_service))
        assert_ws_bad_request(resp, ws, "request with topic should contain only one user id")

    def test_fails_with_topic_and_multiple_services(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200", topic="mytopic", service="tst1,tst2", **self._sign_uid_topic_service))
        assert_ws_bad_request(resp, ws, "request with topic should contain only one service")

    def test_fails_with_topic_and_nontopic_sign(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200", topic="mytopic", service="tst1", **self._sign_uid_service))
        assert_ws_unauthorized(resp, ws, "bad sign")

    def test_success_with_topic(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200", topic="mytopic", service="tst1", **self._sign_uid_topic_service))
        assert_ws_ok(resp, ws)

    def test_receive_notification_for_topic(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200", topic="mytopic", service="tst1", **self._sign_uid_topic_service))
        assert_equals(resp.code, 101)
        ws_message = ws.recv_message() # ping
        ws_message = ws.recv_message() # subscribed
        notification = [b'topic:mytopic', b'', b'tst1', b'', b'', b'', {b'\rmethod_id': b'', b'\rsz': b'1825'}, b'1234567890', True, b'fLJU600KTa61', 3, 1470054101, []]
        self.xiva_client.notify("tst1", notification)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.status_code, 0)
        payload = json.loads(ws_message.payload)
        print payload
        assert_not_in("uid", payload)
        assert_equals(payload["topic"], "mytopic")

    def test_no_notification_only_for_uid(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200", topic="mytopic", service="tst1", **self._sign_uid_topic_service))
        assert_equals(resp.code, 101)
        notification = [b'200', b'', b'tst1', b'', b'', b'', {b'\rmethod_id': b'', b'\rsz': b'1825'}, b'1234567890', True, b'fLJU600KTa61', 3, 1470054101, []]
        assert_equals(self.xiva_client.notify("tst1", notification).status, 205)

    def test_receive_binary_notification(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        ws_message = ws.recv_message() # ping
        ws_message = ws.recv_message() # subscribed
        message = make_message(uid='200', service='tst1', raw_data=TestData.binary_payload)
        self.xiva_client.notify("tst1", message)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        (content_type, hdr, payload) = unpack_binary_frame(ws_message.payload)
        assert_equals(content_type, 3) # type push_frame
        assert_equals(hdr[0], '200') # uid
        assert_equals(hdr[1], 'tst1') # uid
        assert_equals(hdr[2], 'test-event') # event
        assert_not_equals(hdr[3], '') # transit_id
        assert_equals(payload, TestData.binary_payload) # binary payload

    def test_receive_notification_multiauth(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service='autoru,tst2', **self._sign_uid_two_services_multiauth))
        assert_equals(resp.code, 101)
        ws_message = ws.recv_message() # ping
        ws_message = ws.recv_message() # subscribed

        message = make_message(uid='200', service='tst2', raw_data='Hello, World!')
        self.xiva_client.notify('tst2', message)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        payload = json.loads(ws_message.payload)
        assert_equals(payload['uid'], '200')
        assert_equals(payload['service'], 'tst2')
        assert_equals(payload['operation'], 'test-event')
        assert_equals(payload['message'], 'Hello, World!')

        message = make_message(uid='200', service='autoru', raw_data='Hello, again!')
        self.xiva_client.notify('autoru', message)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        payload = json.loads(ws_message.payload)
        assert_equals(payload['uid'], '200')
        assert_equals(payload['service'], 'autoru')
        assert_equals(payload['operation'], 'test-event')
        assert_equals(payload['message'], 'Hello, again!')

    def test_multiple_subscriptions_with_uniq_id(self):
        (resp, ws1) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        (resp, ws2) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        ws1.recv_message() # skip ping
        ws1.recv_message() # skip subscribed
        ws2.recv_message() # skip ping
        ws2.recv_message() # skip subscribed
        message = make_message(uid='200', service='tst1', raw_data='Hello, World!')
        self.xiva_client.notify('tst1', message)
        msg1 = ws1.recv_message()
        msg2 = ws2.recv_message()
        assert_equals(msg1.status_code, 0)
        assert_equals(msg2.status_code, 0)
        parsed_msg = json.loads(msg1.payload)
        assert_in("operation", parsed_msg)
        assert_equals(parsed_msg["operation"], "test-event")
        parsed_msg = json.loads(msg2.payload)
        assert_in("operation", parsed_msg)
        assert_equals(parsed_msg["operation"], "test-event")

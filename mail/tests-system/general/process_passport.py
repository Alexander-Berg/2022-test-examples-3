from pycommon import *
from helpers import *
import BaseHTTPServer
import msgpack
import sys
import time

def setUp(self):
    global hub_server, api, reaper
    hub_server = fake_hub(host='::', port=17081)
    api = Client('', '')
    reaper = RawClient('localhost', '16080')

def tearDown(self):
    global hub_server
    hub_server.fini()

class PassportBase:
    def debug_print(self):
        print 'total_requests %s' % hub_server.total_requests
        print 'listed %s' % hub_server.listed
        print 'notified %s' % hub_server.notified
        print 'messages %s' % hub_server.batch_msg
        print 'unsubscribed %s' % hub_server.unsubscribed

    def send_event(self, event, uid = '123'):
        body = msgpack.packb(api.make_message(uid, 'passport', {}, json.dumps(event), 'transit-id', 0, 0))
        return reaper.post('/passport_hook', body=body)

class TestAccountInvalidate(PassportBase):
    def setup(self):
        real_time = int(time.time())
        default_sub_time = real_time - 100
        self.event = {'uid': '123', 'name': 'account.invalidate', 'timestamp': real_time}
        hub_server.reset_state()
        hub_server.unsubscribed = {}
        hub_server.notified = {}
        hub_server.batch_msg = {}
        hub_server.listed = set()
        self.default_list_json = {'mail': [{'url': 'xivamob:xxx', 'id': 'xxx', 'init_time': default_sub_time},
                                           {'url': 'xivamob:yyy', 'id': 'yyy', 'init_time': default_sub_time}],
                              'calendar': [{'url':'zzz', 'id': 'zzz', 'init_time': default_sub_time},
                                           {'url': 'webpush:www', 'id': 'www', 'init_time': default_sub_time}]}
        hub_server.list_json_response = self.default_list_json
        self.default_batch_binary_notify = {'calendar': [{'123': [200, 'OK', 'zzz']}, {'123': [200, 'OK', 'www']}]}
        hub_server.batch_binary_notify_response = self.default_batch_binary_notify
        self.default_unsubscribe = {'mail': 200, 'calendar': 200}
        hub_server.unsubscribe_response = self.default_unsubscribe

    def teardown(self):
        pass

    def test_account_ivnalidate_ok(self):
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_ok(res)
        assert_equals(hub_server.total_requests, 4)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {'calendar': sorted([['123', 'zzz'], ['123', 'www']])})
        assert_equals(hub_server.unsubscribed, {'calendar': {'zzz', 'www'}, 'mail': {'xxx', 'yyy'}})
        # check ignore_filters flag
        assert_equals(hub_server.batch_msg['calendar'][15], 1)

    def test_account_ivnalidate_no_push_ok(self):
        hub_server.list_json_response['calendar'] = hub_server.list_json_response['mail']
        hub_server.unsubscribe_response['calendar'] = hub_server.unsubscribe_response['mail']
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_ok(res)
        assert_equals(hub_server.total_requests, 3)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {})
        assert_equals(hub_server.unsubscribed, {'calendar': {'xxx', 'yyy'}, 'mail': {'xxx', 'yyy'}})

    def test_account_ivnalidate_ok_notify_400(self):
        hub_server.batch_binary_notify_response = {
            'calendar': [{'123': [400, 'bad', 'zzz']}, {'123': [200, 'ok', 'www']}]}
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_ok(res)
        assert_equals(hub_server.total_requests, 4)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {'calendar': sorted([['123', 'zzz'], ['123', 'www']])})
        assert_equals(hub_server.unsubscribed, {'calendar': {'zzz', 'www'}, 'mail': {'xxx', 'yyy'}})

    def test_account_ivnalidate_check_sub_timestamp(self):
        timestamp = int(time.time())
        self.event['timestamp'] = timestamp + 200
        hub_server.list_json_response['calendar'][1]['init_time'] = timestamp + 300;
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_ok(res)
        assert_equals(hub_server.total_requests, 4)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {'calendar': [['123', 'zzz']]})
        assert_equals(hub_server.unsubscribed, {'calendar': {'zzz',}, 'mail': {'xxx', 'yyy'}})

    def test_dont_unsubscribe_removed_subscriptions(self):
        hub_server.batch_binary_notify_response = {
            'calendar': [{'123': [204, 'no sub', 'zzz']}, {'123': [205, 'hub unsub', 'www']}]}
        del hub_server.unsubscribe_response['calendar']
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_ok(res)
        assert_equals(hub_server.total_requests, 3)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {'calendar': sorted([['123', 'zzz'], ['123', 'www']])})
        assert_equals(hub_server.unsubscribed, {'mail': {'xxx', 'yyy'}})

    def test_fail_on_failed_list(self):
        hub_server.list_json_response = 500
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_equals(res.status, 500)
        assert_equals(hub_server.total_requests, 1)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {})
        assert_equals(hub_server.unsubscribed, {})

    def test_fail_on_bad_list(self):
        hub_server.list_json_response['calendar'] = [{'bad': 'sub'}]
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_equals(res.status, 500)
        assert_equals(hub_server.total_requests, 1)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {})
        assert_equals(hub_server.unsubscribed, {})

    def test_fail_on_failed_notify(self):
        hub_server.batch_binary_notify_response['calendar'] = 500
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_equals(res.status, 500)
        assert_equals(hub_server.total_requests, 3)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {'calendar': sorted([['123', 'zzz'], ['123', 'www']])})
        assert_equals(hub_server.unsubscribed, {'mail': {'xxx', 'yyy'}})

    def test_retry_on_notify_response(self):
        hub_server.batch_binary_notify_response = {
            'calendar': [{'123': [200, 'ok', 'zzz']}, {'123': [500, 'fail', 'www']}]}
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_ok(res)
        assert_equals(hub_server.total_requests, 6)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {'calendar': sorted([['123', 'zzz'], ['123', 'www'], ['123', 'www'], ['123', 'www']])})
        assert_equals(hub_server.unsubscribed, {'mail': {'xxx', 'yyy'}, 'calendar': {'zzz','www'}})

    def test_fail_on_failed_unsubscribe(self):
        hub_server.unsubscribe_response['calendar'] = 500
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_equals(res.status, 500)
        assert_equals(hub_server.total_requests, 4)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {'calendar': sorted([['123', 'zzz'], ['123', 'www']])})
        assert_equals(hub_server.unsubscribed, {'calendar': {'zzz', 'www'}, 'mail': {'xxx', 'yyy'}})

    def check_bad_event(self, event):
        res = self.send_event(event=event)
        assert_equals(res.status, 400)

    def test_fail_on_bad_event(self):
        event = {'uid': '123', 'name': 'account.invalidate', 'timestamp': int(time.time())}
        events = [event, event.copy(), event.copy(), event.copy()]
        del events[0]['name']
        events[1]['timestamp'] = 'invalid_time'
        del events[2]['timestamp']
        del events[3]['uid']
        for e in events:
            yield self.check_bad_event, e

    def test_ignore_event_not_supported(self):
        event = {'uid': '123', 'name': 'invalid_name', 'timestamp': int(time.time())}
        res = self.send_event(event=event)
        assert_equals(res.status, 202)

    def test_nothing_to_do(self):
        hub_server.list_json_response = {'mail': [], 'calendar': []}
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_ok(res)
        assert_equals(hub_server.total_requests, 1)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {})
        assert_equals(hub_server.unsubscribed, {})


class TestSessionInvalidate(PassportBase):
    def setup(self):
        real_time = int(time.time())
        default_sub_time = real_time - 100
        self.event = {'uid': '123', 'name': 'session.invalidate', 'timestamp': real_time, 'connection_id': 'qwerty'}
        hub_server.reset_state()
        hub_server.unsubscribed = {}
        hub_server.notified = {}
        hub_server.batch_msg = {}
        hub_server.listed = set()
        self.default_list_json = {'mail': [{'url': 'xivamob:xxx', 'id': 'xxx', 'connection_id': 'qwerty', 'init_time': default_sub_time},
                                           {'url': 'xivamob:yyy', 'id': 'yyy', 'connection_id': 'asdfgh', 'init_time': default_sub_time}],
                              'calendar': [{'url':'zzz', 'id': 'zzz', 'connection_id': 'qwerty', 'init_time': default_sub_time},
                                           {'url': 'webpush:www', 'id': 'www', 'connection_id': '', 'init_time': default_sub_time},
                                           {'url': 'webpush:qaz', 'id': 'qaz', 'init_time': default_sub_time}]}
        hub_server.list_json_response = self.default_list_json
        self.default_batch_binary_notify = {'calendar': [{'123': [200, 'OK', 'zzz']}, {'123': [200, 'OK', 'www']}]}
        hub_server.batch_binary_notify_response = self.default_batch_binary_notify
        self.default_unsubscribe = {'mail': 200, 'calendar': 200}
        hub_server.unsubscribe_response = self.default_unsubscribe

    def teardown(self):
        pass

    def test_session_ivnalidate_ok(self):
        res = self.send_event(event=self.event)
        self.debug_print()
        assert_ok(res)
        assert_equals(hub_server.total_requests, 4)
        assert_equals(hub_server.listed, {'mail', 'calendar'})
        assert_equals(hub_server.notified, {'calendar': [['123', 'zzz']]})
        assert_equals(hub_server.unsubscribed, {'calendar': {'zzz',}, 'mail': {'xxx',}})

    def test_fail_on_bad_event(self):
        del self.event['connection_id']
        res = self.send_event(event=self.event)
        assert_equals(res.status, 400)

from pycommon import *
from subscriber import *
from urlparse import parse_qsl, urlparse
from json import dumps, loads
from time import sleep
import string
import random

# JSON key constants
KEY_XIVA = 'xiva'
KEY_POSITION = 'pos'
KEY_SERVICE = 'svc'
KEY_USER = 'usr'
KEY_DEVICE = 'device'

def setUp(self):
    global hub
    global mobile
    mobile = FakeXivaMobile(Testing.mobile_port())
    mobile.start()
    # Protect from flaps while mobile is starting.
    sleep(0.25)

    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())
    hub.unsubscribe_all(Testing.uid2(), Testing.service2())

def tearDown(self):
    global mobile
    mobile.stop()


class TestApnsQueue:
    def setup(self):
        self.device = 'DEV-ICE-IDDQD'
        self.queue_service = 'apns_queue'
        self.app = 'ru.yandex.app'
        self.queue_id = '%s_%s' % (self.device, self.app)
        self._subscribe_args = {'uid': Testing.uid1(), 'service': Testing.service1(),
            'device_id': self.device, 'session_key': 'asdfg',
            'platform': 'apns' }

        self.default_payload = {'info': 'data', 'more': {'a': 'b', 'c': 'd'}}
        self.default_message = make_message(Testing.uid1(), Testing.service1(),
            {}, dumps(self.default_payload), 'transit-id', 0, 0, 'news')
        self.default_aps = {'sound': 'beep', 'content': 'smth'}
        self.default_message[13] = {'apns': dumps({'aps': self.default_aps})}

    def teardown(self):
        mobile.requests = []
        mobile.bodies = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uid2(), Testing.service2())
        hub.unsubscribe_all(self.queue_id, 'apns_queue')
        hub.unsubscribe_all('test', 'mail')

    def args(self, **kwargs):
        args = self._subscribe_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def subscribe_apns_queue(self, args):
        # Imitate xiva-server 2-call subscribe process.
        impl_args = { 'service': self.queue_service, 'uid': self.queue_id,
            'callback': 'xivamob:%s/%s' % (self.app, 'QWERTYUI'), 'platform': 'apnsqueue',
            'session_key': args['device_id'], 'device_id': args['device_id']}
        hub.subscribe_mobile(**impl_args)

        args['callback'] = 'apnsqueue:%s/%s/%s' % (self.queue_service, self.app, args['device_id'])
        hub.subscribe_mobile(**args)

    def message(self, uid=None, service=None, raw_data=None, repack=None, ttl=None, event=None):
        msg = self.default_message[:]
        if uid is not None: msg[0] = uid
        if service is not None: msg[2] = service
        if event is not None: msg[3] = event
        if raw_data is not None: msg[7] = raw_data
        if repack is not None: msg[13] = repack
        if ttl is not None: msg[14] = ttl
        transit_id = ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(8))
        msg[9] = transit_id
        print 'transit id: ' + transit_id
        return msg

    def get_mobile_request(self, n):
        params = dict(parse_qsl(urlparse(mobile.requests[n].path).query))
        params.update(dict(parse_qsl(mobile.bodies[n])))
        params['payload'] = loads(params['payload'])
        return params

    def test_push_is_forwarded_to_mobile(self):
        self.subscribe_apns_queue(self.args())

        original = self.message(ttl=1234)
        hub.binary_notify(original)
        wait(lambda: len(mobile.requests) == 1, 5)
        assert_equals(len(mobile.requests), 1)
        assert_in('push/apns?', mobile.requests[0].path)

        message = self.get_mobile_request(0)
        assert_in(int(message['ttl']), range(1232, 1235))
        assert_equals(loads(message['x-aps']), self.default_aps)
        assert(all(item in message['payload'].items() for item in self.default_payload.items()))
        assert_in(KEY_XIVA, message['payload'])
        xiva = message['payload'][KEY_XIVA]
        assert_in(KEY_POSITION, xiva)
        assert_equals(xiva[KEY_USER], Testing.uid1())
        assert_equals(xiva[KEY_SERVICE], Testing.service1())
        assert_equals(xiva[KEY_DEVICE], self.device[-10:])
        assert_equals(message['transit_id'], original[9])

    def test_silent_push_has_ttl_0_and_no_position(self):
        self.subscribe_apns_queue(self.args())

        hub.binary_notify(self.message(repack={}))
        wait(lambda: len(mobile.requests) == 1, 5)
        assert_equals(len(mobile.requests), 1)

        message = self.get_mobile_request(0)
        assert_equals(int(message['ttl']), 0)
        assert_in(KEY_XIVA, message['payload'])
        xiva = message['payload'][KEY_XIVA]
        assert_not_in(KEY_POSITION, xiva)

    def test_silent_by_filter(self):
        self.subscribe_apns_queue(self.args(filter='{"default_action":"send_silent","rules":[{"action":"send_silent","condition":"$has_tags","value":["x"]}]}'))

        hub.binary_notify(self.message())
        wait(lambda: len(mobile.requests) == 1, 5)
        assert_equals(len(mobile.requests), 1)

        message = self.get_mobile_request(0)
        assert_equals(int(message['ttl']), 0)
        assert_in(KEY_XIVA, message['payload'])
        xiva = message['payload'][KEY_XIVA]
        assert_not_in(KEY_POSITION, xiva)

    def test_silent_by_ttl(self):
        self.subscribe_apns_queue(self.args())

        # ttl = 0 messages are sent with /fast_binary_notify by xiva-server.
        hub.fast_binary_notify(self.message(ttl=0))
        wait(lambda: len(mobile.requests) == 1, 5)
        assert_equals(len(mobile.requests), 1)

        message = self.get_mobile_request(0)
        assert_equals(int(message['ttl']), 0)
        assert_in(KEY_XIVA, message['payload'])
        xiva = message['payload'][KEY_XIVA]
        assert_not_in(KEY_POSITION, xiva)

    def test_multiservice(self):
        "queue push from multiple services goes to single queue with consistent numbeging, excluding silent pushes"
        self.subscribe_apns_queue(self.args())
        self.subscribe_apns_queue(self.args(uid=Testing.uid2(), service=Testing.service2()))

        hub.binary_notify(self.message())
        wait(lambda: len(mobile.requests) == 1, 5)
        assert_equals(len(mobile.requests), 1)
        message1 = self.get_mobile_request(0)

        hub.binary_notify(self.message(repack={}))
        wait(lambda: len(mobile.requests) == 2, 5)
        assert_equals(len(mobile.requests), 2)
        message2 = self.get_mobile_request(1)

        hub.binary_notify(self.message(uid=Testing.uid2(), service=Testing.service2()))
        wait(lambda: len(mobile.requests) == 3, 5)
        assert_equals(len(mobile.requests), 3)
        message3 = self.get_mobile_request(2)

        assert_in(KEY_POSITION, message1['payload'][KEY_XIVA])
        assert_not_in(KEY_POSITION, message2['payload'][KEY_XIVA])
        assert_in(KEY_POSITION, message3['payload'][KEY_XIVA])
        assert_equals(int(message1['payload'][KEY_XIVA][KEY_POSITION]) + 1, int(message3['payload'][KEY_XIVA][KEY_POSITION]))

    def test_unsubscribe(self):
        "/unsubscribe_mobile removes queue-subscription, leaving impl-subscription"
        self.subscribe_apns_queue(self.args())
        assert_equals(len(hub.list(self._subscribe_args['uid'], self._subscribe_args['service'])), 1)
        assert_equals(len(hub.list(self.queue_id, self.queue_service)), 1)

        hub.get_200("/unsubscribe_mobile", {'uid': self._subscribe_args['uid'],
            'service': self._subscribe_args['service'], 'uuid': self._subscribe_args['session_key']})
        assert_equals(len(hub.list(self._subscribe_args['uid'], self._subscribe_args['service'])), 0)
        assert_equals(len(hub.list(self.queue_id, self.queue_service)), 1)

    def test_unsubscribe_by_205(self):
        "attempting to send without impl-subscription leads to unsubscribe by 205 code"
        self.subscribe_apns_queue(self.args())
        assert_equals(len(hub.list(self._subscribe_args['uid'], self._subscribe_args['service'])), 1)
        assert_equals(len(hub.list(self.queue_id, self.queue_service)), 1)

        # Delay for drop_overlap_sec.
        sleep(1.25)
        hub.unsubscribe_all(self.queue_id, self.queue_service)
        assert_equals(len(hub.list(self._subscribe_args['uid'], self._subscribe_args['service'])), 1)
        assert_equals(len(hub.list(self.queue_id, self.queue_service)), 0)

        resp = hub.batch_binary_notify(self.message(), [[self._subscribe_args['uid'], ""]])
        assert_equals(resp, [[0, 205, 'no subscription', 'mob:asdfg']])
        sleep(0.25)
        assert_equals(len(hub.list(self._subscribe_args['uid'], self._subscribe_args['service'])), 0)
        assert_equals(len(hub.list(self.queue_id, self.queue_service)), 0)

    def test_mail_two_pushes(self):
        "mail insert generates two pushes, bright and silent"
        self.subscribe_apns_queue(self.args(uid='test', service='mail'))
        hub.binary_notify(self.message(uid='test', service='mail', event='insert', repack={}))
        wait(lambda: len(mobile.requests) == 2, 5)

        assert_equals(len(mobile.requests), 2)
        assert_equals(len([req for req in mobile.requests if 'ttl=0' in req.path]), 1)
        assert_equals(len([req for req in mobile.requests if 'ttl=0' not in req.path]), 1)

    def test_repeat_message(self):
        "repeat_messages sends old messages again"
        self.subscribe_apns_queue(self.args())

        hub.binary_notify(self.message(raw_data=dumps({'message-id': 1})))
        hub.binary_notify(self.message(raw_data=dumps({'message-id': 2})))
        hub.binary_notify(self.message(raw_data=dumps({'message-id': 3})))
        wait(lambda: len(mobile.requests) == 3, 5)

        assert_equals(len(mobile.requests), 3)
        message1 = self.get_mobile_request(0)
        message2 = self.get_mobile_request(1)
        message3 = self.get_mobile_request(2)
        pos1 = int(message1['payload'][KEY_XIVA][KEY_POSITION])
        pos2 = int(message2['payload'][KEY_XIVA][KEY_POSITION])
        pos3 = int(message3['payload'][KEY_XIVA][KEY_POSITION])
        assert_equals(pos3 - pos2, 1)
        assert_equals(pos2 - pos1, 1)
        print message1
        print message2
        print message3

        # Request to repeat 2 messages from position of 3rd message as if we missed 1st and 2nd messages.
        response = hub.raw().get('/repeat_messages',
            {'uid': self.queue_id, 'position': pos1,
             'count': 2, 'service': self.queue_service})
        assert_ok(response)
        print 'repeated %s' % (response.body,)
        assert_equals(response.body, '2')
        wait(lambda: len(mobile.requests) == 5, 5)

        assert_equals(len(mobile.requests), 5)
        message1_repeat = self.get_mobile_request(3)
        message2_repeat = self.get_mobile_request(4)
        print message1_repeat
        print message2_repeat
        pos1_repeat = int(message1_repeat['payload'][KEY_XIVA][KEY_POSITION])
        pos2_repeat = int(message2_repeat['payload'][KEY_XIVA][KEY_POSITION])
        assert_equals(pos1_repeat, pos3 + 1)
        assert_equals(pos2_repeat, pos3 + 2)
        for message in [message1, message1_repeat, message2, message2_repeat]:
            del message['payload'][KEY_XIVA][KEY_POSITION]
            del message['ttl']
        assert_equals(message1, message1_repeat)
        assert_equals(message2, message2_repeat)

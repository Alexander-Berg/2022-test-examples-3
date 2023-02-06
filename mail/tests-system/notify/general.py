from pycommon import *
from subscriber import *
from time import sleep

def setUp(self):
    global hub
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())

def tearDown(self):
    sleep(0.15)

class TestGeneral:
    def setup(self):
        self.message = hub.make_message(Testing.uid1(), Testing.service1(), {}, 'test', 'transit-id-abc', 0, 0)

    def teardown(self):
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    def test_200_for_service_in_path(self):
        response = hub.raw().post('/fast_binary_notify/'  + Testing.service1(), {}, msgpack.packb(self.message))
        assert_ok(response)
        response = hub.raw().post('/binary_notify/'  + Testing.service1(), {}, msgpack.packb(self.message))
        assert_ok(response)

    def test_400_for_missmatched_service_in_path(self):
        response = hub.raw().post('/fast_binary_notify/'  + Testing.service2(), {}, msgpack.packb(self.message))
        assert_bad_request(response, 'service name from message and from path do not match')
        response = hub.raw().post('/binary_notify/'  + Testing.service2(), {}, msgpack.packb(self.message))
        assert_bad_request(response, 'service name from message and from path do not match')


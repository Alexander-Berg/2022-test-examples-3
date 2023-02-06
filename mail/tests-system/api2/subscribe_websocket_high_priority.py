from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import time

class HighPriorityBase(object):
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()

    def setup(self):
        self._good_args = { 'uid': "200", 'client': "test", 'session': "ABCD-EFGH"}
        self.method_to_check = iter(['/subscribe', '/unsubscribe'])
        self.hub_server.set_request_hook(self.check_hub_request)

    def teardown(self):
        error_in_hook = self.hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook
        # Verify that all necessary methods were called.
        with assert_raises(StopIteration):
            next(self.method_to_check)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def check_hub_request(self, req):
        check_caught_request(req, next(self.method_to_check), priority=self._good_args["priority"])

    def _sign(self, service):
        tokens = {
            'tests-system-high-priority-websockets' : 'highpriowebsockets',
            'bass': 'bass123456'
        }
        return self.xiva_client.secret_sign(**{
            "token" :  tokens[service],
            "uid" : "200",
            "service" : service
        })

    def test_high_priority_subscribe_for_whitelisted_service(self):
        self._good_args["priority"] = "high"
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200",
            service="tests-system-high-priority-websockets",
            **self._sign("tests-system-high-priority-websockets")))
        time.sleep(0.05)
        ws.close()
        time.sleep(0.05)

    def test_low_priority_subscribe_for_normal_service(self):
        self._good_args["priority"] = "low"
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200",
            service="bass", **self._sign("bass")))
        time.sleep(0.05)
        ws.close()
        time.sleep(0.05)

from pycommon.xiva_api import *
from pycommon.fake_server import *
import time

class TestSend:
    @classmethod
    def setup_class(cls):
        cls.mesh = MeshApi(host='localhost', port=11080)
        cls.hub_server = fake_server(host='localhost', port=11081, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()

    def setup(self):
        self.attempts = 0
        self.hub_server.reset_state()

    def teardown(self):
        error_in_hook = self.hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def hook(self, request):
        self.attempts += 1
        self.path = request.path

    def test_with_no_required_arguments_fails_with_400(self):
        resp = self.mesh.send()
        assert_bad_request(resp, 'missing argument "gid_from"')
        resp = self.mesh.send(gid_from=0)
        assert_bad_request(resp, 'missing argument "gid_to"')

    def test_with_invalid_message_fails_with_400(self):
        resp = self.mesh.send(gid_from=0,gid_to=65536)
        assert_bad_request(resp, 'invalid message')

    def test_success_with_correct_args(self):
        self.hub_server.set_request_hook(self.hook)
        notification = [b'', b'', b'mesh-autotest', b'', b'', b'', {}, b'test-data', True, b'fLJU600KTa61', 3, 1470054101, []]
        resp = self.mesh.send(msg=notification, gid_from=0, gid_to=65536)
        assert_ok(resp)
        assert_equal(resp.body, "21/21")

    def test_sends_to_all_subscribed(self):
        self.hub_server.set_request_hook(self.hook)
        notification = [b'', b'', b'mesh-autotest', b'', b'', b'', {}, b'test-data', True, b'fLJU600KTa61', 3, 1470054101, []]
        resp = self.mesh.send(msg=notification, gid_from=0, gid_to=65536)
        assert_equal(self.attempts, 21)

    def test_sends_to_all_subscribed_limited_range_in_multiple_shards(self):
        self.hub_server.set_request_hook(self.hook)
        notification = [b'', b'', b'mesh-autotest', b'', b'', b'', {}, b'test-data', True, b'fLJU600KTa61', 3, 1470054101, []]
        resp = self.mesh.send(msg=notification, gid_from=5, gid_to=65531)
        assert_equal(self.attempts, 5) # 1:5, 2:5, 65535:65531, 65534:65531, 3:11

    def test_sends_to_all_subscribed_limited_range_in_single_shard(self):
        self.hub_server.set_request_hook(self.hook)
        notification = [b'', b'', b'mesh-autotest', b'', b'', b'', {}, b'test-data', True, b'fLJU600KTa61', 3, 1470054101, []]
        resp = self.mesh.send(msg=notification, gid_from=65520, gid_to=65531)
        assert_equal(self.attempts, 2) # 65534:65531, 65535:65531

    def test_passes_hub_url_params(self):
        self.hub_server.set_request_hook(self.hook)
        notification = [b'', b'', b'mesh-autotest', b'', b'', b'', {}, b'test-data', True, b'fLJU600KTa61', 3, 1470054101, []]
        resp = self.mesh.send(msg=notification, gid_from=11, gid_to=11)
        assert_equal(self.attempts, 1)
        assert_equal(self.path, '/mesh-autotest?uid=3&event_ts=1470054101&service=mesh-autotest&uidservice=3mesh-autotest')

    def test_sends_to_all_with_retries(self):
        self.hub_server.set_request_hook(self.hook)
        self.hub_server.set_fail_strategy([5,7,9,11])
        notification = [b'', b'', b'mesh-autotest', b'', b'', b'', {}, b'test-data', True, b'fLJU600KTa61', 3, 1470054101, []]
        resp = self.mesh.send(msg=notification, gid_from=0, gid_to=65536)
        assert_equal(self.attempts, 25)
        assert_equal(resp.body, "21/21")

    def test_sends_to_19_if_single_fail(self):
        self.hub_server.set_request_hook(self.hook)
        self.hub_server.set_fail_strategy([2,3])
        notification = [b'', b'', b'mesh-autotest', b'', b'', b'', {}, b'test-data', True, b'fLJU600KTa61', 3, 1470054101, []]
        resp = self.mesh.send(msg=notification, gid_from=0, gid_to=65536)
        assert_equal(self.attempts, 22)
        assert_equal(resp.body, "20/21")

    def test_sends_with_tag_to_20_because_1_is_filtered_by_this_tag(self):
        self.hub_server.set_request_hook(self.hook)
        notification = [b'', b'', b'mesh-autotest', b'', b'', b'', {}, b'test-data', True, b'fLJU600KTa61', 3, 1470054101, ["tagA"]]
        resp = self.mesh.send(msg=notification, gid_from=0, gid_to=65536)
        assert_equal(resp.body, "20/21")

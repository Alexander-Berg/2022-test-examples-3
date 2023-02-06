from base import TestBase
from drive.backend.api import client as api
from utils.args import get_args
from utils.endpoints import get_endpoint
from utils.helpers import *
from utils.wrappers import skip_unless_has_tags
from utils.timeout import timeout


class BlockUserSuite(TestBase):
    args = get_args()

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.client = api.BackendClient(endpoint=get_endpoint(cls.args.endpoint),
                                        public_token=cls.args.client_public_token,
                                        private_token=cls.args.private_token)

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()

    def setUp(self):
        super().setUp()

    def tearDown(self):
        super().tearDown()

    @skip_unless_has_tags(args, ["prestable", "testing"])
    @timeout(args.timeout_per_test)
    def test_block_user(self):
        block_user(self.client)
        time.sleep(5)
        status = get_current_session(self.client).user.status
        assert status == "blocked"
        unblock_user(self.client)
        time.sleep(5)
        status = get_current_session(self.client).user.status
        assert status == "active"

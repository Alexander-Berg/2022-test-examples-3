import logging
import time

import pytest

from drive.backend.api import client as api
from helpers.helpers import client_initial_state, block_user, get_current_session, unblock_user
from utils.base_tests import BaseTestClass
from utils.oauth import get_drive_token
from utils.proxied_backend_api import BackendClientAutotests
from utils.timeout import timeout
from utils.tus import unlock_tus_account


class TestBlockUser(BaseTestClass):

    @classmethod
    def setup_class(cls):
        logging.info("starting class: {} execution".format(cls.__name__))

    @classmethod
    def teardown_class(cls):
        logging.info("starting class: {} execution".format(cls.__name__))

    @pytest.fixture(autouse=True)
    def client(self, endpoint, secret, account):
        self.client = BackendClientAutotests(endpoint, public_token=get_drive_token(account.login, account.password),
                                        private_token=secret.get("value")["PRIVATE_DRIVE_TOKEN"])
        client_initial_state(self.client)
        yield
        unlock_tus_account(account.uid)

    @pytest.mark.prod
    @timeout(BaseTestClass.DEFAULT_TIMEOUT)
    def test_block_user(self):
        block_user(self.client)
        time.sleep(5)
        status = get_current_session(self.client).user.status
        assert status == "blocked"
        unblock_user(self.client)
        time.sleep(5)
        status = get_current_session(self.client).user.status
        assert status == "active"

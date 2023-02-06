import logging
import os

import httpretty
from hamcrest import assert_that, equal_to

from calendar_attach_processor.service.blackbox import BlackboxService, User
import calendar_attach_processor.arc as arc

logging.basicConfig(level=logging.DEBUG)
PATH = os.path.abspath(__file__)


@httpretty.activate
def test_two_logins():
    with open(arc.test_path("service/blackbox/input", PATH, "not_ml_with_attr.json")) as resp1:
        with open(arc.test_path("service/blackbox/input", PATH, "not_ml.json")) as resp2:
            test_url = "http://test-two-logins/"
            httpretty.register_uri(httpretty.GET, test_url,
                                   responses=[httpretty.Response(status=200, body=resp1.read()),
                                              httpretty.Response(status=200, body=resp2.read())])

            users = BlackboxService(test_url, "1234").resolve_uids_by_logins(["login1", "login2"])
            assert_that(list(users), equal_to([User("1120000000001387", "maillist", email="login1"),
                                               User("1120000000017656", "not_maillist", email="login2")]))


@httpretty.activate
def test_one_request_failed():
    with open(arc.test_path("service/blackbox/input", PATH, "not_ml_with_attr.json")) as resp1:
        test_url = "http://test-one-request-failed/"
        httpretty.register_uri(httpretty.GET, test_url, responses=[httpretty.Response(status=200, body=resp1.read()),
                                                                   httpretty.Response(status=504, body="")])

        users = BlackboxService(test_url, "1234").resolve_uids_by_logins(["login1", "login2"])
        assert_that(list(users), equal_to([User("1120000000001387", "maillist", email="login1")]))

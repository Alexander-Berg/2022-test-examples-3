import json
import os

import pytest
from hamcrest import assert_that, equal_to

from calendar_attach_processor.service.ml import Subscriber, Maillist, MlError
import calendar_attach_processor.arc as arc

PATH = os.path.abspath(__file__)


@pytest.mark.parametrize("file_name, expected", [
    ("maillist_ok.json", [Subscriber("user1@yandex-team.ru", False, "kateogar"),
                                Subscriber("test-maillist@yandex-team.ru", False, "furita-test-29"),
                                Subscriber("user2@yandex-team.ru", False, "stassiak")]),
    ("no_ml.json", []),
    ("no_email_in_ml.json", [])
])
def test_expand_maillist_ok(file_name, expected):
    json_path = arc.test_path("service/ml/input", PATH, file_name)
    with open(json_path) as resp:
        assert_that(Maillist.get_subscribers(json.loads(resp.read()), "mail-testing@yandex-team.ru"), equal_to(expected))

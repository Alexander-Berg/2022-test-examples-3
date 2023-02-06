import json
import logging
import os

import pytest
from hamcrest import assert_that, equal_to

from calendar_attach_processor.service.blackbox import BlackboxService, User, BlackboxError
import calendar_attach_processor.arc as arc

logging.basicConfig(level=logging.DEBUG)
PATH = os.path.abspath(__file__)


@pytest.mark.parametrize("file_name, expected", [
    ("ml.json", User("1120000000001387", "maillist", True)),
    ("not_ml_with_attr.json", User("1120000000001387", "maillist")),
    ("not_ml.json", User("1120000000017656", "not_maillist")),
    ("not_pg_not_ml.json", User("1120000000017656", "not_maillist", is_pg=False)),
    ("two_users_select_first.json", User("4000019104", "abdul")),
    ("with_garbage.json", User("4000019104", "abdul"))
])
def test_bb_response_parsing(file_name, expected):
    json_path = arc.test_path("service/blackbox/input", PATH, file_name)
    with open(json_path) as resp:
        assert_that(BlackboxService.extract_user(json.loads(resp.read())), equal_to(expected))


@pytest.mark.parametrize("file_name", [
    "invalid_params.json",
    "garbage.json",
])
def test_bb_response_parsing_failed(file_name):
    json_path = arc.test_path("service/blackbox/input", PATH, file_name)
    with open(json_path) as resp:
        with pytest.raises(BlackboxError):
            BlackboxService.extract_user(json.loads(resp.read()))

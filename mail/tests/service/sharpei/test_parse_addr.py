import json
import os

import pytest
from hamcrest import assert_that, equal_to

from calendar_attach_processor.service.sharpei import Sharpei, SharpeiError
import calendar_attach_processor.arc as arc

PATH = os.path.abspath(__file__)


@pytest.mark.parametrize("file_name, expected", [
    (
            "all_alive.json",
            "host=xdb-test02e.cmail.yandex.net port=6432 dbname=maildb user=lb-pg password=simple_pwd"
    ),
    (
            "one_dead.json",
            "host=xdb-test02e.cmail.yandex.net port=6432 dbname=maildb user=lb-pg password=simple_pwd"
    )
])
def test_parse_addr(file_name, expected):
    json_path = arc.test_path("service/sharpei/input", PATH, file_name)
    with open(json_path) as resp:
        assert_that(Sharpei.parse_addr(json.loads(resp.read()), "lb-pg", "simple_pwd"), equal_to(expected))


def test_all_dead():
    json_path = arc.test_path("service/sharpei/input", PATH, 'all_dead.json')
    with open(json_path) as resp:
        with pytest.raises(SharpeiError):
            Sharpei.parse_addr(json.loads(resp.read()), "lb-pg", "simple_pwd")


@pytest.mark.parametrize("file_name",
                         ["all_dead.json", "no_address.json"])
def test_parse_bad_addr(file_name):
    json_path = arc.test_path("service/sharpei/input", PATH, file_name)
    with open(json_path) as resp:
        with pytest.raises(SharpeiError):
            Sharpei.parse_addr(json.loads(resp.read()), "lb-pg", "simple_pwd")

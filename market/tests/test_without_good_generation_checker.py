# coding: utf-8

from mock import patch
import pytest
from datetime import timedelta, datetime
from market.idx.admin.mi_agent.lib.checkers import without_good_generation_checker
import market.idx.pylibrary.mindexer_core.geninfo.geninfo as geninfo
from collections import namedtuple

Generation = namedtuple('Generation', ['status', 'end_date'])
Config = namedtuple('Config', ['indexing_without_good_generation_threshold_minutes'])

MITYPE = 'gibson'
DATASOURCES = 'datasources'
THRESHOLD = 200

GOOD_GENERATION = Generation(
    status=geninfo.STATUS_COMPLETED,
    end_date=datetime.now() - timedelta(minutes=10)
)
GOOD_BUT_OLD_GENERATION = Generation(
    status=geninfo.STATUS_COMPLETED,
    end_date=datetime.now() - timedelta(minutes=THRESHOLD + 10)
)

mi_agent_config = Config(indexing_without_good_generation_threshold_minutes=THRESHOLD)


def get_generations_mock(generation, *args, **kwargs):
    def check_key(name, value):
        assert name in kwargs and kwargs[name] is value

    check_key('only_successfull', True)
    check_key('skip_empty', True)
    check_key('only_for_publish', True)
    check_key('show_full', True)
    check_key('allow_half', False)
    check_key('allow_scale', False)

    return generation


@pytest.mark.parametrize("generation, expected", [
    ([GOOD_GENERATION], True),
    ([GOOD_BUT_OLD_GENERATION], False),
])
def test_check(generation, expected):
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: get_generations_mock(generation, **kwargs)):
        sensor = without_good_generation_checker.WithoutGoodGenerationChecker(MITYPE, DATASOURCES, mi_agent_config)
        assert expected == sensor.check()

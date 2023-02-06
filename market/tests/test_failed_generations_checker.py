# coding: utf-8

from mock import patch
import pytest
from collections import namedtuple
import market.idx.pylibrary.mindexer_core.geninfo.geninfo as geninfo
from market.idx.admin.mi_agent.lib.checkers import failed_generations_checker

Generation = namedtuple('Generation', ['status', 'fail_reason', 'name'])
Config = namedtuple('Config', ['failed_generation_threshold'])

CANCELLED_GEN = Generation(geninfo.STATUS_CANCELLED, '', '')
COMPLETED_GEN = Generation(geninfo.STATUS_COMPLETED, '', '')
FAILED_GEN = Generation(geninfo.STATUS_FAILED, '', '')
INPROGRESS_GEN = Generation(geninfo.STATUS_INPROGRESS, '', '')

FAILED_GENERATION_THRESHOLD = 3
MITYPE = 'gibson'
DATASOURCES = 'datasources'

mi_agent_config = Config(failed_generation_threshold=FAILED_GENERATION_THRESHOLD)


@pytest.mark.parametrize("generation", [
    [COMPLETED_GEN, FAILED_GEN, FAILED_GEN, FAILED_GEN],  # есть успешное поколение свежее упавших
    [CANCELLED_GEN, FAILED_GEN, FAILED_GEN, COMPLETED_GEN],  # количество упавших поколений < порога
    [CANCELLED_GEN, COMPLETED_GEN, FAILED_GEN, FAILED_GEN, FAILED_GEN],  # есть успешное поколение свежее упавших
])
def test_ok(generation):
    """Здесь и ниже: делается заглушка ответа geninfo.get_generations в виде наборов поколений разной
    успешности. Проверяется выхлоп FailedGenerationsCheck в зависимости от содержания этих наборов.
    """
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: generation):
        sensor = failed_generations_checker.FailedGenerationsChecker(MITYPE, DATASOURCES, mi_agent_config)
        assert sensor.check()


@pytest.mark.parametrize("generation", [
    [FAILED_GEN, FAILED_GEN, FAILED_GEN],  # количество упавших поколений >= порога
    [INPROGRESS_GEN, CANCELLED_GEN, FAILED_GEN, FAILED_GEN, FAILED_GEN],  # есть inprogress, но количество упавших поколений >= порога
])
def test_fail(generation):
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: generation):
        sensor = failed_generations_checker.FailedGenerationsChecker(MITYPE, DATASOURCES, mi_agent_config)
        assert not sensor.check()

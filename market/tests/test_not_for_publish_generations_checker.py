# coding: utf-8

from mock import patch
import pytest
from collections import namedtuple
import market.idx.pylibrary.mindexer_core.geninfo.geninfo as geninfo
from market.idx.admin.mi_agent.lib.checkers import not_for_publish_generations_checker

Generation = namedtuple('Generation', ['status', 'not_for_publish', 'name'])
Config = namedtuple('Config', ['not_for_publish_generations_threshold'])

CANCELLED_GEN = Generation(geninfo.STATUS_CANCELLED, '', '')
COMPLETED_OK_GEN = Generation(geninfo.STATUS_COMPLETED, False, '')
COMPLETED_NOT_FOR_PUBLISH_GEN = Generation(geninfo.STATUS_COMPLETED, True, '')
FAILED_GEN = Generation(geninfo.STATUS_FAILED, '', '')
INPROGRESS_GEN = Generation(geninfo.STATUS_INPROGRESS, '', '')

MITYPE = 'gibson'
DATASOURCES = 'datasources'

mi_agent_config = Config(not_for_publish_generations_threshold=2)


@pytest.mark.parametrize("generation", [
    [COMPLETED_OK_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN, FAILED_GEN],  # есть хорошее поколение свежее плохих
    [CANCELLED_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN, CANCELLED_GEN, COMPLETED_OK_GEN],  # количество плохих поколений < порога
    [CANCELLED_GEN, COMPLETED_OK_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN],  # есть успешное поколение свежее плохих
])
def test_ok(generation):
    """Здесь и ниже: делается заглушка ответа geninfo.get_generations в виде наборов поколений разной
    успешности. Проверяется выхлоп NotForPublishGenerationsChecker в зависимости от содержания этих наборов.
    """
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: generation):
        sensor = not_for_publish_generations_checker.NotForPublishGenerationsChecker(MITYPE, DATASOURCES, mi_agent_config)
        assert sensor.check()


@pytest.mark.parametrize("generation", [
    [COMPLETED_NOT_FOR_PUBLISH_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN, COMPLETED_OK_GEN],  # количество плохих поколений >= порога
    [INPROGRESS_GEN, CANCELLED_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN, COMPLETED_NOT_FOR_PUBLISH_GEN],  # есть inprogress, но количество плохих поколений >= порога
])
def test_fail(generation):
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: generation):
        sensor = not_for_publish_generations_checker.NotForPublishGenerationsChecker(MITYPE, DATASOURCES, mi_agent_config)
        assert not sensor.check()

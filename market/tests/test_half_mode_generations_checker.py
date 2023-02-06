# coding: utf-8

from mock import patch
import pytest
from collections import namedtuple
import market.idx.pylibrary.mindexer_core.geninfo.geninfo as geninfo
from market.idx.admin.mi_agent.lib.checkers import half_mode_generations_checker

Generation = namedtuple('Generation', ['status', 'name', 'half_mode'])

INPROGRESS_HALF_GEN = Generation(geninfo.STATUS_INPROGRESS, 'half', True)
INPROGRESS_FULL_GEN = Generation(geninfo.STATUS_INPROGRESS, 'full', False)

MITYPE = 'gibson'
DATASOURCES = 'datasources'


@pytest.mark.parametrize(
    "test_data",
    [
        {
            'generations': [INPROGRESS_HALF_GEN],
            'expected': False
        },
        {
            'generations': [INPROGRESS_FULL_GEN],
            'expected': True
        },
        {
            'generations': [],
            'expected': False
        }
    ],
    ids=[
        'half_mode',
        'full',
        'empty'
    ]
)
def test_half_mode_checker(test_data):
    """Делается заглушка ответа geninfo.get_generations в виде наборов разных поколений
    Проверяется ответ HalfModeGenerationsChecker в зависимости от этого набора
    """
    with patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_generations', side_effect=lambda *args, **kwargs: test_data['generations']):
        checker = half_mode_generations_checker.HalfModeGenerationsChecker(MITYPE, DATASOURCES)
        assert test_data['expected'] == checker.check()

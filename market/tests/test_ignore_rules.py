# coding: utf-8

import pytest
from collections import namedtuple
from market.idx.admin.mi_agent.lib.core.core import Core

Check = namedtuple('Check', ['check', 'name'])
Action = namedtuple('SwitchMaster', ['execute', 'name'])


@pytest.mark.parametrize("sensors, conditions, ignore_rules, expected", [
    (
        # На активном давно не было хороших поколений (WithoutGoodGenerationChecker).
        # При этом на резервном есть замедление сборки (IndexingTimeChecker).
        # Задаём правило, что для таких случаев мы игнорим проверки резервного и переключаем мастера.
        [
            Check(check=lambda: False, name='WithoutGoodGenerationChecker')
        ],
        [
            Check(check=lambda: False, name='IndexingTimeChecker'),
        ],
        {
            "WithoutGoodGenerationChecker": [
                "IndexingTimeChecker",
            ]
        },
        True
    ),
    (
        # На активном давно не было хороших поколений (WithoutGoodGenerationChecker).
        # При этом на резервном есть замедление сборки (IndexingTimeChecker) и недавно было переключение мастера (MasterLastModifiedChecker).
        # В таком случае мы не переключаем мастера, чтобы mi_agent не конфликтовал с ручным переключением дежурного.
        [
            Check(check=lambda: False, name='WithoutGoodGenerationChecker')
        ],
        [
            Check(check=lambda: False, name='IndexingTimeChecker'),
            Check(check=lambda: False, name='MasterLastModifiedChecker'),
        ],
        {
            "WithoutGoodGenerationChecker": [
                "IndexingTimeChecker",
            ]
        },
        False
    ),
    (
        # На активном давно не было хороших поколений (WithoutGoodGenerationChecker) и сработали разладки (NotForPublishGenerationsChecker).
        # При этом на резервном есть замедление сборки (IndexingTimeChecker).
        # В правилах стоит, что мы игнорим проверку на замедление сборки.
        # В таком случае мы переключаем мастера.
        [
            Check(check=lambda: False, name='WithoutGoodGenerationChecker'),
            Check(check=lambda: False, name='NotForPublishGenerationsChecker')
        ],
        [
            Check(check=lambda: False, name='IndexingTimeChecker'),
        ],
        {
            "WithoutGoodGenerationChecker": [
                "IndexingTimeChecker",
            ]
        },
        True
    ),
    (
        # На активном давно не было хороших поколений (WithoutGoodGenerationChecker).
        # При этом на резервном есть замеделенеи сборки (IndexingTimeChecker).
        # Правила игнорирование проверок не подгрузились.
        # В таком случае мы не преключаем мастера.
        [
            Check(check=lambda: False, name='WithoutGoodGenerationChecker')
        ],
        [
            Check(check=lambda: False, name='IndexingTimeChecker'),
        ],
        None,
        False
    )
])
def test_core(sensors, conditions, ignore_rules, expected):
    action_called = [False]

    def call_action(called):
        called[0] = True

    actions = [Action(execute=lambda: call_action(action_called), name='')]

    core = Core(sensors, conditions, actions, ignore_rules)
    core.run_agent()

    assert action_called[0] == expected

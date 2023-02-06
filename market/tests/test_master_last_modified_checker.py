# coding: utf-8

import time
from datetime import timedelta
from market.idx.admin.mi_agent.lib.checkers import master_last_modified_checker

from collections import namedtuple
import pytest

Zk = namedtuple('Zk', ['get_current_master_type_last_modified'])
PERIOD = 8
Config = namedtuple('Config', ['master_change_period'])
mi_agent_config = Config(master_change_period=PERIOD)


@pytest.mark.parametrize("test_input, expected", [
    (-1, False),
    (0, False),
    (6, False),
    (8, False),
    (9, True),
])
def test_master_last_modified(test_input, expected):
    """Делаем mock ответа zookeeper о времени изменения ноды master_mitype.
    Смотрим на ответ проверки условия о том, что мастер менялся достаточно давно
    """
    zk = Zk(lambda: int((time.time() - timedelta(hours=test_input).total_seconds())))
    import logging
    logging.info(zk.get_current_master_type_last_modified())
    condition = master_last_modified_checker.MasterLastModifiedChecker(zk, mi_agent_config)
    assert condition.check() == expected

# -*- coding: utf-8 -*-

import pytest

from market.idx.cron.update_cron_available.src.interval import Interval, On
from market.idx.cron.update_cron_available.src.scheduler import (
    SchedulerItem,
    ExternalTask,
    MindexerCltTask,
)


@pytest.mark.parametrize('item, is_valid', [
    (SchedulerItem(task=MindexerCltTask(), interval=Interval()), True),
    (SchedulerItem(task=MindexerCltTask(), interval=Interval(minute=On(5))), True),
    (SchedulerItem(task=MindexerCltTask(user='bzz13'), interval=Interval(minute=On(5))), True),
    (SchedulerItem(task=MindexerCltTask(user='bzz13', log_file='special.log'), interval=Interval(minute=On(5))), True),
    (SchedulerItem(task=ExternalTask(user='root', binary='rm -rf /'), interval=Interval(minute=On(5))), True),
    (SchedulerItem(task=ExternalTask(user='root', binary='rm -rf /', args=''), interval=Interval(minute=On(5))), True),
    (SchedulerItem(task=ExternalTask(user='root', binary='rm -rf /', args=None), interval=Interval(minute=On(5))), True),
    (SchedulerItem(task=ExternalTask(user='root', binary='rm', args='-rf /'), interval=Interval(minute=On(5))), True),

    (SchedulerItem(task=MindexerCltTask(user=''), interval=Interval(minute=On(5))), False),
    (SchedulerItem(task=MindexerCltTask(user=None), interval=Interval(minute=On(5))), False),
    (SchedulerItem(task=ExternalTask(user='root', binary=''), interval=Interval(minute=On(5))), False),
    (SchedulerItem(task=ExternalTask(user='root', binary=None), interval=Interval(minute=On(5))), False),
    (SchedulerItem(task=ExternalTask(user='root', binary='', args='-rf /'), interval=Interval(minute=On(5))), False),
    (SchedulerItem(task=ExternalTask(user='root', binary=None, args='-rf /'), interval=Interval(minute=On(5))), False),
])
def test_scheduler_item_is_valid(item, is_valid):
    assert item.is_valid == is_valid


@pytest.mark.parametrize('item, cron_str', [
    (
        SchedulerItem(task=MindexerCltTask(), interval=Interval()),
        '* * * * *\tcorba\t/usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/cron.log'
    ),
    (
        SchedulerItem(task=MindexerCltTask(), interval=Interval(minute=On(5))),
        '5 * * * *\tcorba\t/usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/cron.log'
    ),
    (
        SchedulerItem(task=MindexerCltTask(user='bzz13'), interval=Interval(minute=On(5))),
        '5 * * * *\tbzz13\t/usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/cron.log'
    ),
    (
        SchedulerItem(task=MindexerCltTask(user='bzz13', log_file='/var/log/marketindexer/cron/special.log'), interval=Interval(minute=On(5))),
        '5 * * * *\tbzz13\t/usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/cron/special.log'
    ),
    (
        SchedulerItem(task=ExternalTask(user='root', binary='rm -rf /'), interval=Interval(minute=On(5))),
        '5 * * * *\troot\trm -rf /'
    ),
    (
        SchedulerItem(task=ExternalTask(user='root', binary='rm -rf /', args=''), interval=Interval(minute=On(5))),
        '5 * * * *\troot\trm -rf /'
    ),
    (
        SchedulerItem(task=ExternalTask(user='root', binary='rm -rf /', args=None), interval=Interval(minute=On(5))),
        '5 * * * *\troot\trm -rf /'
    ),
    (
        SchedulerItem(task=ExternalTask(user='root', binary='rm', args='-rf /'), interval=Interval(minute=On(5))),
        '5 * * * *\troot\trm -rf /'
    ),

    (
        SchedulerItem(task=MindexerCltTask(user=''), interval=Interval(minute=On(5))),
        '5 * * * *\t\t/usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/cron.log'
    ),
    (
        SchedulerItem(task=MindexerCltTask(user=None), interval=Interval(minute=On(5))),
        '5 * * * *\t\t/usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/cron.log'
    ),
    (
        SchedulerItem(task=ExternalTask(user='root', binary=''), interval=Interval(minute=On(5))),
        '5 * * * *\troot\t'
    ),
    (
        SchedulerItem(task=ExternalTask(user='root', binary=None), interval=Interval(minute=On(5))),
        '5 * * * *\troot\t'
    ),
    (
        SchedulerItem(task=ExternalTask(user='root', binary='', args='-rf /'), interval=Interval(minute=On(5))),
        '5 * * * *\troot\t-rf /'
    ),
    (
        SchedulerItem(task=ExternalTask(user='root', binary=None, args='-rf /'), interval=Interval(minute=On(5))),
        '5 * * * *\troot\t-rf /'
    ),
])
def test_scheduler_item_cron_str(item, cron_str):
    assert item.cron_str == cron_str

# -*- coding: utf-8 -*-

import pytest

from market.idx.cron.update_cron_available.src.interval import Interval, On
from market.idx.cron.update_cron_available.src.scheduler import (
    ExternalTask,
    SchedulerItem,
    Scheduler
)


@pytest.mark.parametrize('scheduler, is_valid', [
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='rm', args='-rf /'),
                    interval=Interval()
                ),
            ]
        ),
        True
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='rm', args='-rf /'),
                    interval=Interval()
                ),
                SchedulerItem(
                    task=ExternalTask(user='root', binary='run', args='--dry-run'),
                    interval=Interval(minute=On(0), hour=On(12))
                ),
            ]
        ),
        True
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='rm', args='-rf /'),
                    interval=Interval()
                ),
                SchedulerItem(
                    task=ExternalTask(user='root', binary='run', args='--dry-run'),
                    interval=Interval(minute=On(0), hour=On(12))
                ),
            ]
        ),
        True
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='rm', args='-rf /'),
                    interval=Interval(minute=On(0), hour=On(32))
                ),
            ]
        ),
        False
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='', binary='rm', args='-rf /'),
                    interval=Interval(minute=On(0), hour=On(12))
                ),
            ]
        ),
        False
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='run', args='--dry-run'),
                    interval=Interval(minute=On(0), hour=On(12)),
                ),
                SchedulerItem(
                    task=ExternalTask(user='root', binary='run', args='--dry-run'),
                    interval=Interval(minute=On(0), hour=On(14)),
                ),
            ]
        ),
        False
    ),
])
def test_scheduler_is_valid(scheduler, is_valid):
    assert scheduler.is_valid == is_valid


@pytest.mark.parametrize('scheduler, cron_str', [
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='rm', args='-rf /'),
                    interval=Interval()
                ),
            ]
        ),
        '''MAILTO=""
* * * * *\troot\trm -rf /
'''
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='rm', args='-rf /'),
                    interval=Interval()
                ),
                SchedulerItem(
                    task=ExternalTask(user='root', binary='run', args='--dry-run'),
                    interval=Interval(hour=On(12))
                ),
            ]
        ),
        '''MAILTO=""
* * * * *\troot\trm -rf /
* 12 * * *\troot\trun --dry-run
'''
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='rm', args='-rf /'),
                    interval=Interval()
                ),
                SchedulerItem(
                    task=ExternalTask(user='root', binary='run', args='--dry-run'),
                    interval=Interval(hour=On(12))
                ),
            ]
        ),
        '''MAILTO=""
* * * * *\troot\trm -rf /
* 12 * * *\troot\trun --dry-run
'''
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='rm', args='-rf /'),
                    interval=Interval(hour=On(32))
                ),
            ]
        ),
        '''MAILTO=""
* 32 * * *\troot\trm -rf /
'''
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='', binary='rm', args='-rf /'),
                    interval=Interval(hour=On(12))
                ),
            ]
        ),
        '''MAILTO=""
* 12 * * *\t\trm -rf /
'''
    ),
    (
        Scheduler(
            items=[
                SchedulerItem(
                    task=ExternalTask(user='root', binary='run', args='--dry-run'),
                    interval=Interval(hour=On(12))
                ),
                SchedulerItem(
                    task=ExternalTask(user='root', binary='run', args='--dry-run'),
                    interval=Interval(hour=On(14))
                ),
            ]
        ),
        '''MAILTO=""
* 12 * * *\troot\trun --dry-run
* 14 * * *\troot\trun --dry-run
'''
    ),
])
def test_scheduler_cron_str(scheduler, cron_str):
    assert scheduler.cron_str == cron_str

# -*- coding: utf-8 -*-

import os
import re

import yatest.common

from market.idx.cron.update_cron_available.src.generator import SCHEDULER

from market.idx.cron.update_cron_available.src.scheduler import (
    Scheduler,
    SchedulerItem,
    ExternalTask,
    Restrictions,
    MonRun,
)

from market.idx.cron.update_cron_available.src.interval import (
    Interval,
)
from market.idx.cron.update_cron_available.src.enums import (
    EnvType,
    DcType,
    IdxType,
    Color,
)
from market.idx.cron.update_cron_available.src.idx_structure import make_clear_struct


def split_and_normalize_content(content):
    splitted = content.split('\n')
    lines = []
    for line in splitted:
        # scip comments and empty lines
        if line.startswith('#') or not bool(line):
            continue
        cropped = re.sub(r"\s+", " ", line)
        lines.append(cropped)
    # cron.d must have new line in end
    lines.append('')
    return lines


def read_all_file_content(path):
    with open(path, 'r') as f:
        return f.read()


def _debug_print(gens):
    from pprint import pprint
    for v in gens.values():
        print('\n', v['name'], ':')
        pprint(v['items'])
    # raise RuntimeError(gens)


def test_generate_files():
    scheduler = Scheduler(
        items=[
            SchedulerItem(
                task=ExternalTask(user='root', binary='all'),
                interval=Interval(),
                restrictions=Restrictions()
            ),
        ]
    )

    work_path = yatest.common.work_path('cron_generated')
    scheduler.generate_configs(work_path)
    files = os.listdir(work_path)
    idx_struct = make_clear_struct()

    known_files = [v['name'] for v in idx_struct.values()]
    assert len(files) == len(known_files)
    assert sorted(files) == sorted(known_files)


def test_generate_files_content_env():
    work_path = yatest.common.work_path('cron_generated')

    scheduler = Scheduler(
        items=[
            SchedulerItem(
                task=ExternalTask(user='root', binary='all'),
                interval=Interval(),
                restrictions=Restrictions()
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='testing'),
                interval=Interval(),
                restrictions=Restrictions(envs=EnvType.Testing)
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='testing_production'),
                interval=Interval(),
                restrictions=Restrictions(
                    envs=[EnvType.Testing, EnvType.Production],
                )
            ),
        ]
    )

    gens = scheduler.generate_configs(work_path)
    _debug_print(gens)

    assert read_all_file_content(os.path.join(work_path, 'development.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
'''
    assert read_all_file_content(os.path.join(work_path, 'production.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\ttesting_production
'''
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\ttesting_production
'''
    assert read_all_file_content(os.path.join(work_path, 'production.gibson.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\ttesting_production
'''
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.gibson.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\ttesting_production
'''
    assert read_all_file_content(os.path.join(work_path, 'testing.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\ttesting
* * * * *\troot\ttesting_production
'''
    assert read_all_file_content(os.path.join(work_path, 'testing.planeshift.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\ttesting
* * * * *\troot\ttesting_production
'''


def test_generate_files_content_dc():
    work_path = yatest.common.work_path('cron_generated')

    scheduler = Scheduler(
        items=[
            SchedulerItem(
                task=ExternalTask(user='root', binary='all'),
                interval=Interval(),
                restrictions=Restrictions()
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='stratocaster'),
                interval=Interval(),
                restrictions=Restrictions(dcs=DcType.Stratocaster)
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='gibson'),
                interval=Interval(),
                restrictions=Restrictions(dcs=DcType.Gibson)
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='stratocaster_gibson'),
                interval=Interval(),
                restrictions=Restrictions(dcs=[DcType.Stratocaster, DcType.Gibson])
            ),
        ]
    )

    gens = scheduler.generate_configs(work_path)
    _debug_print(gens)

    assert read_all_file_content(os.path.join(work_path, 'development.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tstratocaster
* * * * *\troot\tgibson
* * * * *\troot\tstratocaster_gibson
'''
    assert read_all_file_content(os.path.join(work_path, 'production.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tstratocaster
* * * * *\troot\tstratocaster_gibson
'''
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tstratocaster
* * * * *\troot\tstratocaster_gibson
'''
    assert read_all_file_content(os.path.join(work_path, 'production.gibson.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tgibson
* * * * *\troot\tstratocaster_gibson
'''
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.gibson.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tgibson
* * * * *\troot\tstratocaster_gibson
'''
    assert read_all_file_content(os.path.join(work_path, 'testing.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tstratocaster
* * * * *\troot\tstratocaster_gibson
'''
    assert read_all_file_content(os.path.join(work_path, 'testing.planeshift.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tstratocaster
* * * * *\troot\tstratocaster_gibson
'''


def test_generate_files_content_color():
    work_path = yatest.common.work_path('cron_generated')

    scheduler = Scheduler(
        items=[
            SchedulerItem(
                task=ExternalTask(user='root', binary='all'),
                interval=Interval(),
                restrictions=Restrictions()
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='white'),
                interval=Interval(),
                restrictions=Restrictions(colors=Color.White)
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='turbo'),
                interval=Interval(),
                restrictions=Restrictions(colors=Color.Turbo)
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='fresh'),
                interval=Interval(),
                restrictions=Restrictions(colors=Color.Fresh)
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='stabletesting'),
                interval=Interval(),
                restrictions=Restrictions(colors=Color.Stabletesting)
            ),
        ]
    )

    gens = scheduler.generate_configs(work_path)
    _debug_print(gens)
    assert read_all_file_content(os.path.join(work_path, 'development.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\twhite
* * * * *\troot\tturbo
* * * * *\troot\tfresh
* * * * *\troot\tstabletesting
'''
    assert read_all_file_content(os.path.join(work_path, 'production.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\twhite
'''
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\twhite
'''
    assert read_all_file_content(os.path.join(work_path, 'production.gibson.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\twhite
'''
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.gibson.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\twhite
'''
    assert read_all_file_content(os.path.join(work_path, 'testing.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\twhite
'''
    assert read_all_file_content(os.path.join(work_path, 'testing.planeshift.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\twhite
'''


def test_generate_files_content_idx():
    work_path = yatest.common.work_path('cron_generated')

    scheduler = Scheduler(
        items=[
            SchedulerItem(
                task=ExternalTask(user='root', binary='all'),
                interval=Interval(),
                restrictions=Restrictions()
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='planeshift'),
                interval=Interval(),
                restrictions=Restrictions(idxs=IdxType.Planeshift)
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='main'),
                interval=Interval(),
                restrictions=Restrictions(idxs=IdxType.Main)
            ),
        ]
    )

    gens = scheduler.generate_configs(work_path)
    _debug_print(gens)

    assert read_all_file_content(os.path.join(work_path, 'development.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tplaneshift
* * * * *\troot\tmain
'''
    assert read_all_file_content(os.path.join(work_path, 'production.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tmain
'''
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tplaneshift
'''
    assert read_all_file_content(os.path.join(work_path, 'production.gibson.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tmain
'''
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.gibson.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tplaneshift
'''
    assert read_all_file_content(os.path.join(work_path, 'testing.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tmain
'''
    assert read_all_file_content(os.path.join(work_path, 'testing.planeshift.stratocaster.cron.d')) == '''MAILTO=""
* * * * *\troot\tall
* * * * *\troot\tplaneshift
'''


white_config = '''[cronstatus_all]
command = /usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/monitors/cron_monitors.log --mode=monitor check_cron_status_for all
execution_interval = 60
execution_timeout = 60

[cronstatus_white]
command = /usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/monitors/cron_monitors.log --mode=monitor check_cron_status_for white
execution_interval = 60
execution_timeout = 60

'''


dev_config = '''[cronstatus_all]
command = /usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/monitors/cron_monitors.log --mode=monitor check_cron_status_for all
execution_interval = 60
execution_timeout = 60

[cronstatus_white]
command = /usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/monitors/cron_monitors.log --mode=monitor check_cron_status_for white
execution_interval = 60
execution_timeout = 60

[cronstatus_turbo]
command = /usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/monitors/cron_monitors.log --mode=monitor check_cron_status_for turbo
execution_interval = 60
execution_timeout = 60

[cronstatus_fresh]
command = /usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/monitors/cron_monitors.log --mode=monitor check_cron_status_for fresh
execution_interval = 60
execution_timeout = 60

[cronstatus_stabletesting]
command = /usr/lib/yandex/marketindexer/mindexer_clt.py --log-file /var/log/marketindexer/monitors/cron_monitors.log --mode=monitor check_cron_status_for stabletesting
execution_interval = 60
execution_timeout = 60

'''


def test_generate_monrun_files_content_color():
    work_path = yatest.common.work_path('cron_monrun_generated')

    scheduler = Scheduler(
        items=[
            SchedulerItem(
                task=ExternalTask(user='root', binary='all'),
                interval=Interval(),
                restrictions=Restrictions(),
                monrun=MonRun(name='all'),
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='white'),
                interval=Interval(),
                restrictions=Restrictions(colors=Color.White),
                monrun=MonRun(name='white'),
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='turbo'),
                interval=Interval(),
                restrictions=Restrictions(colors=Color.Turbo),
                monrun=MonRun(name='turbo'),
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='fresh'),
                interval=Interval(),
                restrictions=Restrictions(colors=Color.Turbo),
                monrun=MonRun(name='fresh'),
            ),
            SchedulerItem(
                task=ExternalTask(user='root', binary='stabletesting'),
                interval=Interval(),
                restrictions=Restrictions(colors=Color.Stabletesting),
                monrun=MonRun(name='stabletesting'),
            ),
        ]
    )

    gens = scheduler.generate_cron_monrun_configs(work_path)
    _debug_print(gens)
    assert read_all_file_content(os.path.join(work_path, 'development.cron.monrun.ini')) == dev_config
    assert read_all_file_content(os.path.join(work_path, 'production.stratocaster.cron.monrun.ini')) == white_config
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.stratocaster.cron.monrun.ini')) == white_config
    assert read_all_file_content(os.path.join(work_path, 'production.gibson.cron.monrun.ini')) == white_config
    assert read_all_file_content(os.path.join(work_path, 'production.planeshift.gibson.cron.monrun.ini')) == white_config

    assert read_all_file_content(os.path.join(work_path, 'testing.stratocaster.cron.monrun.ini')) == white_config

    assert read_all_file_content(os.path.join(work_path, 'testing.planeshift.stratocaster.cron.monrun.ini')) == white_config


def test_scheduler_valid():
    assert SCHEDULER.is_valid

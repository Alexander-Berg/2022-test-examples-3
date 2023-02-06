# coding: utf-8

from __future__ import unicode_literals, print_function

import logging
from datetime import datetime

import pytz
import pytest
import freezegun

from travel.rasp.admin.admin.admin_tasks import TaskLog, get_instance_task_logs
from common.settings import WorkInstance
from tester.factories import create_station
from tester.utils.replace_setting import replace_setting


def task(arg, kwarg):
    print('All is Done {}-{}'.format(arg, kwarg))


@freezegun.freeze_time('2016-01-01')
@pytest.mark.dbuser
def test_create_task_log_from_object(tmpdir):
    station = create_station(id=10000)
    log_paths = {WorkInstance.code: str(tmpdir)}
    with replace_setting('LOG_PATHS', log_paths), replace_setting('INSTANCE_ROLE', WorkInstance):
        from travel.rasp.library.python.common23.date import environment
        from common.utils.date import MSK_TZ
        na = environment.now_aware().astimezone(MSK_TZ)
        assert na == pytz.UTC.localize(datetime(2016, 1, 1))

        task_log = TaskLog.from_object_action(station, 'do_something')
        assert task_log.created_at == pytz.UTC.localize(datetime(2016, 1, 1))
        assert task_log.name == 'www/station/10000/do_something'
        assert task_log.path == str(tmpdir) + '/special/tasks/www/station/10000/do_something/2016-01-01_030000.log'


@freezegun.freeze_time('2016-01-01')
@pytest.mark.dbuser
def test_task_log_from_log_path(tmpdir):
    station = create_station(id=10000)
    log_paths = {WorkInstance.code: str(tmpdir)}
    with replace_setting('LOG_PATHS', log_paths), replace_setting('INSTANCE_ROLE', WorkInstance):
        task_log = TaskLog.from_object_action(station, 'do_something')
        from_log_path = TaskLog.from_log_path(task_log.path)

        assert from_log_path.name == task_log.name
        assert from_log_path.path == task_log.path
        assert from_log_path.created_at == task_log.created_at


@freezegun.freeze_time('2016-01-01')
@pytest.mark.dbuser
def test_task_log_from_log_path_bad_path(tmpdir):
    station = create_station(id=10000)
    log_paths = {WorkInstance.code: str(tmpdir)}
    with replace_setting('LOG_PATHS', log_paths), replace_setting('INSTANCE_ROLE', WorkInstance):
        task_log = TaskLog.from_object_action(station, 'do_something')
        base = task_log.path.rsplit('/', 1)[0]

        with pytest.raises(TaskLog.BadLogPath):
            TaskLog.from_log_path('/a/')

        with pytest.raises(TaskLog.BadLogDateFormat):
            TaskLog.from_log_path(base + '/2016------.log')


@pytest.mark.dbuser
def test_get_instance_task_logs(tmpdir):
    station = create_station(id=10000)
    log_paths = {WorkInstance.code: str(tmpdir)}
    log = logging.getLogger('aaa')
    task_name = TaskLog.get_object_task_name(station, 'do_something')
    with replace_setting('LOG_PATHS', log_paths), replace_setting('INSTANCE_ROLE', WorkInstance):
        with freezegun.freeze_time('2016-01-01'):
            task_log = TaskLog.from_object_action(station, 'do_something')
            with task_log.capture_log(log):
                log.info(u'111')

        with freezegun.freeze_time('2016-01-02'):
            task_log = TaskLog.from_object_action(station, 'do_something')
            with task_log.capture_log(log):
                log.info(u'333')

        task_logs = get_instance_task_logs(task_name, WorkInstance)

    assert len(task_logs) == 2


@freezegun.freeze_time('2016-01-01')
@pytest.mark.dbuser
def test_task_log_from_log_path(tmpdir):
    station = create_station(id=10000)
    log_paths = {WorkInstance.code: str(tmpdir)}
    domain_names = {WorkInstance.code: 'raspadmin.org'}
    log_url_paths = {WorkInstance.code: '/raspadmin/logs/'}
    with replace_setting('LOG_PATHS', log_paths), replace_setting('INSTANCE_ROLE', WorkInstance),\
            replace_setting('LOG_URL_PATHS', log_url_paths), replace_setting('DOMAIN_NAMES', domain_names):
        task_log = TaskLog.from_object_action(station, 'do_something')

        assert task_log.url == 'https://raspadmin.org/raspadmin/logs/' \
                               'special/tasks/www/station/10000/do_something/2016-01-01_030000.log'

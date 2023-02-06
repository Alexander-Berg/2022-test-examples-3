# -*- coding: utf-8 -*-
import os

import mock

from travel.rasp.admin.admin.admin_tasks import TaskLog, run_task
from tester.utils.replace_setting import replace_setting


def task(arg, kwarg):
    print 'All is Done {}-{}'.format(arg, kwarg)


class TaskLogStub(TaskLog):
    path = None


@replace_setting('ADMIN_TASK_RUN_IN_SEPARATE_PROCESS', True)
def test_run_task_with_fork(tmpdir):
    # loc_settings_path = str(tmpdir.join('task_local_settings.py'))
    # with open(loc_settings_path, 'w') as f:
    #     f.write('print("task_local_settings loaded")')

    out_log = tmpdir.mkdir('logs').join('out.log')
    log_path = str(out_log)
    task_log = TaskLogStub('test_run_task')
    task_log.path = log_path

    # Use local_settings or manage_local_settings (for common_recipe/recipe.inc)
    local_settings_module = 'local_settings'
    try:
        __import__(local_settings_module)
    except ImportError:
        local_settings_module = 'tests.manage_local_settings'

    with mock.patch.dict(os.environ, {'DJANGO_SETTINGS_MODULE': local_settings_module}):
        proc = run_task(task_log, task, args=(10,), kwargs={'kwarg': 20}, auto_collect_zombie_process=False)
    proc.wait()

    log_content = out_log.read()
    assert 'Error' not in log_content
    assert 'All is Done 10-20' in log_content



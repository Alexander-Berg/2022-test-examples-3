# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import time

import mock
import pytest

from common.celery.task import single_launch_task
from common.utils.date import MSK_TZ, UTC_TZ

pytestmark = pytest.mark.mongouser('module')


def _as_msk(dt):
    return UTC_TZ.localize(dt).astimezone(MSK_TZ).replace(tzinfo=None)


def _dummy_shared_task(*args, **kwargs):
    def shared_task(f):
        f.decorated = True
        return f
    return shared_task


@pytest.yield_fixture(autouse=True)
def fix_now():
    """Подменяем shared_task, так как внутри он кладет задачу в реестр celery с ключем на основе имени задачи.
    Таким образом, если мы положили туда subject с замкнутым precision=timedelta(minutes=1), то эта версия subject
    будет потом использована во всех тестах."""
    with mock.patch('common.celery.task.shared_task', new_callable=lambda: _dummy_shared_task):
        yield


def test_single_launch_task_launched_one_time():
    @single_launch_task(expire=1)
    def subject():
        return True

    assert subject.decorated
    assert subject()
    assert subject() is None


def test_single_launch_task_launched_two_times():
    @single_launch_task(expire=0.1)
    def subject():
        return True

    assert subject.decorated
    assert subject()
    time.sleep(0.3)
    assert subject()

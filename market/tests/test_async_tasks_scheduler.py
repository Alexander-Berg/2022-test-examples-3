# coding: utf-8

import logging
import os
import pytest
import shutil
import time

from market.pylibrary.async_tasks_scheduler.lib.async_tasks_scheduler import (
    AsyncTasksScheduler,
    AsyncUniqueTasksScheduler,
    BaseTask,
    TaskConfig,
    TaskIntervalUnit,
)

import yatest

TEST_FILE_NAME = "test_file_name.txt"
TEST_FILE_CONTENT = "hello!"


@pytest.fixture
def file_path():
    dir_path = yatest.common.test_output_path()
    if os.path.exists(dir_path):
        shutil.rmtree(dir_path)
    os.makedirs(dir_path)
    return os.path.join(dir_path, TEST_FILE_NAME)


@pytest.fixture(scope="module")
def log():
    """A real logger that passes all requests to a null handler.
    """
    logger = logging.getLogger()
    # logger.propagate = False
    # logger.setLevel(logging.DEBUG)
    # logger.addHandler(logging.NullHandler())
    return logger


class WriteToFileTask(BaseTask):
    def __init__(self, file_path, string_to_write):
        self._file_path = file_path
        self._string_to_write = string_to_write

    def run(self):
        target_string = self._string_to_write
        if os.path.exists(self._file_path):
            target_string = "\n{}".format(self._string_to_write)
        with open(self._file_path, "a+") as fp:
            fp.write(target_string)


class WriteToFileTask2(WriteToFileTask):
    pass


class PerpetualWorkingTask(BaseTask):

    def run(self):
        while True:
            time.sleep(1)


def mock_minutes(self):
    self.unit = 'seconds'
    return self


def test__run_all__task_executed(file_path):
    string_content = TEST_FILE_CONTENT
    interval = 2

    task_config = TaskConfig(
        task_class=WriteToFileTask,
        interval=interval,
        interval_unit=TaskIntervalUnit.SECONDS,
        task_constructor_kwargs={"file_path": file_path, "string_to_write": string_content}
    )

    scheduler = AsyncTasksScheduler()
    scheduler.add_task(task_config)
    scheduler.run_all()

    time.sleep(1)
    while scheduler.get_running_tasks():
        time.sleep(1)

    assert os.path.exists(file_path)


def test__run_all__task_hanged__task_timed_out():
    interval = 2
    timeout = 2
    task_config = TaskConfig(
        task_class=PerpetualWorkingTask,
        interval=interval,
        interval_unit=TaskIntervalUnit.SECONDS,
        timeout=timeout
    )

    scheduler = AsyncTasksScheduler()
    scheduler.add_task(task_config)
    scheduler.run_all()
    time.sleep(timeout + 1)

    assert not scheduler.get_running_tasks()


def test__run_pending__task_executed(file_path):
    string_content = TEST_FILE_CONTENT
    interval = 2

    task_config = TaskConfig(
        task_class=WriteToFileTask,
        interval=interval,
        interval_unit=TaskIntervalUnit.SECONDS,
        task_constructor_kwargs={"file_path": file_path, "string_to_write": string_content}
    )

    scheduler = AsyncTasksScheduler()
    scheduler.add_task(task_config)

    time.sleep(interval + 1)
    scheduler.run_pending()

    time.sleep(1)

    while scheduler.get_running_tasks():
        time.sleep(1)

    assert os.path.exists(file_path)


def test_async_unique_tasks_scheduler(file_path):
    string_content = TEST_FILE_CONTENT
    interval = 2

    first_filepath = file_path + '_1'
    second_filepath = file_path + '_2'

    task_config1 = TaskConfig(
        task_class=WriteToFileTask,
        interval=interval,
        interval_unit=TaskIntervalUnit.SECONDS,
        task_constructor_kwargs={"file_path": first_filepath, "string_to_write": string_content}
    )
    task_config2 = TaskConfig(
        task_class=WriteToFileTask2,
        interval=interval + 1,
        interval_unit=TaskIntervalUnit.SECONDS,
        task_constructor_kwargs={"file_path": second_filepath, "string_to_write": string_content}
    )

    scheduler = AsyncUniqueTasksScheduler()
    scheduler.add_task(task_config1)
    scheduler.add_task(task_config2)
    scheduler.run_all()

    time.sleep(1)
    while scheduler.get_running_tasks():
        time.sleep(1)

    assert os.path.exists(first_filepath)
    assert os.path.exists(second_filepath)

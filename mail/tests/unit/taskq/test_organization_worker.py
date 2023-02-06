from dataclasses import asdict

import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.entities.enums import TaskType
from mail.ipa.ipa.core.entities.task import Task
from mail.ipa.ipa.taskq.app import OrganizationWorker


@pytest.fixture
def worker(test_logger):
    return OrganizationWorker(logger=test_logger)


@pytest.fixture
def parse_csv_task_params(general_init_import_params):
    return {
        'csv_key': 'csvkey',
        'import_params': asdict(general_init_import_params),
    }


@pytest.fixture
def task_entity(parse_csv_task_params, general_init_import_params, org_id):
    import datetime
    return Task(task_type=TaskType.PARSE_CSV,
                entity_id=org_id,
                run_at=datetime.datetime(2019, 10, 1, 0, 0, 0),
                params=parse_csv_task_params)


@pytest.fixture
async def task(storage, task_entity, organization):
    return await storage.task.create(task_entity)


def test_get_params(worker, parse_csv_task_params, general_init_import_params, task):
    assert_that(
        worker.get_params(task),
        equal_to({
            'csv_key': parse_csv_task_params['csv_key'],
            'import_params': general_init_import_params,
        }),
    )


@pytest.mark.asyncio
async def test_fetch_task_for_work(worker, storage, task):
    assert_that(
        await worker.fetch_task_for_work(storage),
        equal_to(task),
    )

import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_entries, has_properties, not_none

from mail.ipa.ipa.core.entities.enums import EventType, TaskState, TaskType


class TestStopImport:
    @pytest.fixture
    async def response(self, app, org_id):
        return await app.post(f'/import/{org_id}/stop/')

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response_status(self, response):
        assert 200 == response.status

    def test_response(self, response_json):
        assert_that(
            response_json,
            has_entries({
                'code': 200,
                'status': 'success',
                'data': has_entries({
                    'task_id': not_none(),
                }),
            }),
        )

    @pytest.fixture
    async def task(self, response, storage, org_id):
        tasks = await alist(storage.task.find(filters={'entity_id': org_id}))
        return tasks[0]

    def test_creates_task(self, task, org_id):
        assert_that(
            task,
            has_properties({
                'entity_id': org_id,
                'task_type': TaskType.STOP_IMPORT,
                'state': TaskState.PENDING,
                'params': {'org_id': org_id},
                'meta_info': {},
            })
        )

    @pytest.mark.asyncio
    async def test_creates_event(self, storage, response, org_id):
        events = await alist(storage.event.find(org_id=org_id, order_by=('-event_id',), limit=1))
        assert_that(events[0].event_type, equal_to(EventType.STOP))

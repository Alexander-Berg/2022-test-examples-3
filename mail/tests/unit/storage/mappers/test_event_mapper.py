import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_length, has_properties, not_none

from mail.ipa.ipa.core.entities.enums import EventType
from mail.ipa.ipa.core.entities.event import Event


@pytest.mark.asyncio
async def test_create_event(storage, organization, org_id):
    data = {'test': True}
    event = Event(event_type=EventType.START, org_id=org_id, data=data, revision=2)

    assert_that(
        await storage.event.create(event),
        has_properties({
            'event_id': not_none(),
            'event_type': event.event_type,
            'org_id': org_id,
            'data': data,
            'revision': 2,
        })
    )


@pytest.mark.asyncio
async def test_get_event(storage, organization, org_id):
    data = {'test': True}
    event = Event(event_type=EventType.START, org_id=org_id, data=data, revision=3)
    event = await storage.event.create(event)

    assert_that(
        await storage.event.get(event.event_id),
        equal_to(event),
    )


class TestFind:
    @pytest.fixture
    def org_id(self, org_id, organization):
        return org_id

    @pytest.mark.asyncio
    async def test_limit(self, create_event, storage, org_id):
        for _ in range(3):
            await create_event()

        assert_that(
            await alist(storage.event.find(org_id=org_id, limit=2)),
            has_length(2)
        )

    @pytest.mark.asyncio
    async def test_offset(self, create_event, storage, org_id):
        for _ in range(4):
            await create_event()

        assert_that(
            await alist(storage.event.find(org_id=org_id, offset=1)),
            has_length(3),
        )

    @pytest.mark.asyncio
    async def test_filter_org_id(self, create_event, storage, org_id, other_org_id):
        await create_event(org_id=org_id)

        assert_that(
            await alist(storage.event.find(org_id=other_org_id)),
            has_length(0),
        )

    @pytest.mark.asyncio
    async def test_filter_event_type(self, create_event, storage, org_id):
        await create_event(org_id=org_id, event_type=EventType.START)
        stop = await create_event(org_id=org_id, event_type=EventType.STOP)

        assert_that(
            await alist(storage.event.find(org_id=org_id, event_type=EventType.STOP)),
            equal_to([stop]),
        )

    @pytest.mark.asyncio
    async def test_order(self, create_event, storage, org_id):
        await create_event(org_id=org_id, event_type=EventType.STOP)
        start = await create_event(org_id=org_id, event_type=EventType.START)

        assert_that(
            await alist(storage.event.find(org_id=org_id, order_by=('event_type',), limit=1)),
            equal_to([start]),
        )

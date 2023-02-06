import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_properties

from mail.ipa.ipa.core.entities.enums import EventType
from mail.ipa.ipa.core.exceptions import BaseCoreError


class ReturnedAndExcInfoContract:
    @pytest.fixture
    def before_action_hook(self):
        pass

    @pytest.fixture
    def expected_exc_type(self):
        return BaseCoreError

    @pytest.fixture
    async def returned(self, loop, action_coro, before_action_hook):
        return await action_coro

    @pytest.fixture
    async def exc_info(self, loop, action_coro, expected_exc_type):
        with pytest.raises(expected_exc_type) as exc_info:
            return await action_coro

        return exc_info


class ActionTestContract(ReturnedAndExcInfoContract):
    @pytest.fixture
    def action_class(self):
        raise NotImplementedError

    @pytest.fixture
    def params(self):
        return {}

    @pytest.fixture
    def action(self, action_class, params):
        return action_class(**params)

    @pytest.fixture
    def action_coro(self, action):
        return action.run()


class GeneratesStartEventContract:
    @pytest.mark.asyncio
    async def test_generates_event(self, returned, storage, org_id):
        events = await alist(storage.event.find(org_id=org_id))
        assert_that(
            events[0], has_properties({
                'event_type': EventType.START,
                'revision': 1,
            })
        )

    class TestUpgradesRevisionWhenStopIsFound:
        @pytest.fixture
        async def before_action_hook(self, create_event, organization):
            await create_event(event_type=EventType.STOP, revision=1)

        @pytest.mark.asyncio
        async def test_generates_revision(self, returned, storage, org_id):
            events = await alist(storage.event.find(org_id=org_id, event_type=EventType.START))
            assert_that(
                events[0], has_properties({
                    'event_type': EventType.START,
                    'revision': 2,
                })
            )

        @pytest.mark.asyncio
        async def test_upgrades_revision(self, returned, storage, org_id, organization):
            new_org = await storage.organization.get(org_id)
            assert_that(new_org.revision, equal_to(organization.revision + 1))

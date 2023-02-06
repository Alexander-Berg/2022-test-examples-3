from unittest.mock import AsyncMock

import pytest
from crm.supskills.common.tvm2 import TvmServiceMiddleware
from crm.supskills.direct_skill.src.core.models.general_topic_intent import SimpleIntent
from crm.supskills.direct_skill.src.intent_storage import IntentStorage
from crm.supskills.direct_skill.src.intents.ad.turbo_site import TurboSiteCreateNew
from crm.supskills.direct_skill.src.intents.general.ask_topic import AskTopic
from crm.supskills.direct_skill.src.intents.general.call_operator_intent import CallOperator
from crm.supskills.direct_skill.src.intents.general.empty_intent import Empty
from crm.supskills.direct_skill.src.intents.general.greetings import Greetings
from crm.supskills.direct_skill.src.intents.general.rejection_intent import Rejection
from crm.supskills.direct_skill.src.intents.moderation.moderation_no_decline import ModerationGeneralTopic
from crm.supskills.direct_skill.src.intents.pay.pay_reimburse import PayReimburseGeneral


@pytest.fixture
async def handler():
    return AsyncMock(spec=[])


@pytest.fixture
async def tvm_client():
    class TvmClientMock:
        parse_service_ticket = AsyncMock()
        parse_user_ticket = AsyncMock()
        get_service_ticket = AsyncMock()

    return TvmClientMock()


@pytest.fixture
def middleware_tvm_on(tvm_client):
    return TvmServiceMiddleware(tvm_client=tvm_client, tvm2_active=True)


@pytest.fixture
def middleware_tvm_off(tvm_client):
    return TvmServiceMiddleware(tvm_client=tvm_client, tvm2_active=False)


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


class TestIntent(SimpleIntent):
    def __init__(self, bunker):
        super(TestIntent, self).__init__(bunker)
        self.name = 'test_intent'


class TestIntentWithState(SimpleIntent):
    def __init__(self, bunker):
        super(TestIntentWithState, self).__init__(bunker)
        self.name = 'test_intent'
        self.state = 'test_state'


@pytest.fixture
def storage():
    class FakeBunker:
        @staticmethod
        async def get_node(path, node, *args, **kwargs):
            return path, node

    storage = IntentStorage(5, FakeBunker(), 'direct5_client', 'direc4_client')
    storage.intents = {
        'test_intenttest_state': TestIntentWithState(storage.bunker),
        'test_intent': TestIntent(storage.bunker),
        'call_operator': CallOperator(storage.bunker),
        'ask_topic': AskTopic(storage.bunker),
        'turbo_site_create_new': TurboSiteCreateNew(storage.bunker),
        'empty_intent': Empty(storage.bunker),
        'YANDEX_REJECT': Rejection(storage.bunker),
        'reimburse_general_topic': PayReimburseGeneral(storage.bunker),
        'moderation_general_topic': ModerationGeneralTopic(storage.bunker),
        'greetings': Greetings(storage.bunker),
    }
    return storage

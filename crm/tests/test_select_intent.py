import pytest
from crm.supskills.direct_skill.src.core.models.conversation import Conversation


testdata = [
    pytest.param(
        {"session": {"new": True}, "request": {"nlu": {"intents": {}}}}, {},
        {"state": "", "empty_intents_count": 0}, 'empty_intent',
        id='test_empty_intent'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {"test_intent": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'test_intent',
        id='test_existing_intent'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {"test_intent": {"slots": {}}}}}},
        {}, {"state": "test_state", "empty_intents_count": 0}, 'test_intenttest_state',
        id='test_existing_intent_and_state'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {"test_intent": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 2}, 'test_intent',
        id='test_detected_intent_after_two_empty_intents'),
    pytest.param(
        {"session": {"new": False},
         "request": {"nlu": {"intents": {"test_intent_general_topic": {"slots": {}}, "test_intent": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'test_intent',
        id='test_specific_intent'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {"reimburse_general_topic": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'reimburse_general_topic',
        id='test_general_intent'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {
            "test_intent_general_topic": {"slots": {}},
            "test_intent": {"slots": {}}}}}},
        {}, {"state": "test_state", "empty_intents_count": 0}, 'test_intenttest_state',
        id='test_specific_intent_with_state'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {
            "YANDEX.REJECT": {"slots": {}},
            "YANDEX_REJECT": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'YANDEX_REJECT',
        id='test_specific_intent_builtin_yandex'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {"moderation_general_topic": {"slots": {}},
                                                                    "create_campaign_general_topic": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'ask_topic',
        id='test_from_two_generals'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {
            "YANDEX.BOOK.CONTINUE": {"slots": {}},
            "reimburse_general_topic": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'reimburse_general_topic',
        id='test_from_general_and_builtin'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {
            "turbo_site_create_new": {"slots": {}},
            "greetings": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'ask_topic',
        id='test_from_two_specific'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {
            "create_campaign_general_topic": {"slots": {}},
            "turbo_site_create_new": {"slots": {}},
            "reimburse_general_topic": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'turbo_site_create_new',
        id='test_from_two_generals_and_specific'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {"greetings": {"slots": {}},
                                                                    "test_intenttest_state": {"slots": {}},
                                                                    "call_operator": {"slots": {}},
                                                                    "turbo_site_create_new": {"slots": {}},
                                                                    "reimburse_general_topic": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'ask_topic',
        id='test_many_intents'),
    pytest.param(
        {"session": {"new": False}, "request": {"nlu": {"intents": {"greetings": {"slots": {}},
                                                                    "test_intent": {"slots": {}},
                                                                    "test_intenttest_state": {"slots": {}},
                                                                    "call_operator": {"slots": {}},
                                                                    "ask_topic": {"slots": {}},
                                                                    "turbo_site_create_new": {"slots": {}},
                                                                    "YANDEX_REJECT": {"slots": {}}}}}},
        {}, {"state": "", "empty_intents_count": 0}, 'call_operator',
        id='test_too_many_intents')
]


class TestSelectIntent:
    @pytest.mark.parametrize('c_req, c_res, c_state, expected_intent', testdata)
    async def test_select_intent(self, storage, c_req, c_res, c_state, expected_intent):
        conversation = Conversation(c_req, c_res, c_state)
        intent, params = await storage.select_intent(conversation)

        for k, v in storage.intents.items():
            if v == intent:
                assert k == expected_intent

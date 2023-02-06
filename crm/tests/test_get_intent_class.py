from crm.supskills.direct_skill.src.intent_storage import IntentStorage
from crm.supskills.direct_skill.tests.config import app_config

import pytest

testdata = [
    pytest.param('test_intent', '', 'Test Intent Class', id='test_existing_intent'),
    pytest.param('test_intent', '_and_state', 'Test Intent Class', id='test_existing_intent_with_non_existing_state'),
    pytest.param('non_existing_intent', '', 'Call Operator Class', id='test_non_existing_intent'),
    pytest.param('non_existing_intent', '_and_state', 'Call Operator Class', id='test_non_existing_intent_and_state'),
    pytest.param('some_intent', '_and_state', 'Intent and State Class', id='test_existing_intent_with_existing_state'),
]


class TestSkillSelectIntent:
    storage = IntentStorage(app_config)
    storage.intents = {'call_operator': 'Call Operator Class',
                       'test_intent': 'Test Intent Class',
                       'some_intent_and_state': 'Intent and State Class'}

    @pytest.mark.parametrize('intent, state, expected_class', testdata)
    def test_get_intent_class(self, intent, state, expected_class):
        intent_class = self.storage.get_intent_instance(intent, state)
        assert intent_class == expected_class

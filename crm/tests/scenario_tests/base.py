from crm.supskills.direct_skill.src.core.models.conversation import Conversation
from resource_loader import get_json
from crm.supskills.common.direct_client.api_v5 import Direct5, ClientGetItem, AdGetItem, AdGroupGetItem, \
    CheckResponseModified, FeedGetItem, TurboPageGetItem, BidModifierGetItem, KeywordGetItem, AudienceTargetGetItem, \
    CampaignGetItem, DirectKeyError, DirectAPIError
from crm.supskills.common.direct_client.api_v4 import Direct4, Account
from crm.supskills.common.direct_client.structs.general import from_dict
from unittest.mock import patch, AsyncMock
from copy import deepcopy
import json


class FakeBunker:
    @staticmethod
    def get_node(path, node):
        return path, node


class SpecialTestException(Exception):
    pass


class TesterWithAioApp:
    def __init__(self, test_case, data_path):
        self.bunker = FakeBunker()
        self.test_case = test_case
        self.conversation = Conversation({}, {}, {})
        self.expected_conversation = Conversation({}, {}, {})
        self.direct_response: dict = {}
        self.set_tester(data_path)

    def parse_direct_response(self, direct: dict):
        methods = {'get_campaign': CampaignGetItem,
                   'get_campaigns': CampaignGetItem,
                   'get_campaigns_by_ids': CampaignGetItem,
                   'get_audiencetargets': AudienceTargetGetItem,
                   'get_ads': AdGetItem,
                   'get_ads_by_ids': AdGetItem,
                   'get_ad': AdGetItem,
                   'get_ad_groups': AdGroupGetItem,
                   'get_ad_groups_by_ids': AdGroupGetItem,
                   'get_changes_in_campaign': CheckResponseModified,
                   'get_feeds': FeedGetItem,
                   'get_turbopage': TurboPageGetItem,
                   'get_keywords': KeywordGetItem,
                   'get_bidmodifiers': BidModifierGetItem,
                   'get_single_campaign': CampaignGetItem,
                   'get_client': ClientGetItem,
                   'get_account_management': Account}
        for method, structs_type in methods.items():
            if method in direct.keys():
                self.direct_response[method] = []
                for item in direct[method]:
                    if item == 'DirectKeyError':
                        self.direct_response[method].append(DirectKeyError('TestError'))
                    elif item == 'DirectAPIError':
                        self.direct_response[method].append(DirectAPIError('TestError'))
                    elif item == 'SpecialTestException':
                        self.direct_response[method].append(SpecialTestException('TestError'))
                    elif isinstance(item, dict):
                        self.direct_response[method].append(from_dict(structs_type, item))
                    elif isinstance(item, list):
                        self.direct_response[method].append([])
                        for j in item:
                            self.direct_response[method][-1].append(from_dict(structs_type, j))
                    else:
                        self.direct_response[method].append(item)
            else:
                self.direct_response[method] = [None]

    def set_tester(self, path: str):
        self.init_default_conversation()
        request = json.loads(get_json(path + '/request.json'))
        if request != {}:
            self.conversation.request.update(request)
        state = get_json(path + '/state.json')
        if state != '{}':
            self.conversation.user_state['state'] = state
        self.parse_direct_response(json.loads(get_json(path + '/direct.json')))
        self.expected_conversation = deepcopy(self.conversation)

    def init_default_conversation(self):
        self.conversation = Conversation({
            'session': {
                'floyd_user': {'login': 'FakeLogin', 'operator_chat_id': 'Some oper Id'}},
            'state': {'session': {'state': '', 'empty_intents_count': 0}},
            'request': {
                'nlu': {
                    'tokens': ['привет'],
                    'intents': {'show_absent': {'slots': {}}},
                    'entities': []},
                'original_utterance': 'Вообще не важно что тут! В 2к21 было :)'}}, {},
            {'state': '', 'empty_intents_count': 0})

    def mock_method(self, method: str):
        def side_effect_direct_mock(*args, **kwargs):
            ans = self.direct_response[method][0]
            self.direct_response[method].pop(0)
            if isinstance(ans, Exception):
                raise ans
            return ans

        return side_effect_direct_mock

    async def check_response(self, intent):
        with patch.multiple(Direct5, get_campaign=AsyncMock(side_effect=self.mock_method('get_campaign')),
                            get_campaigns=AsyncMock(side_effect=self.mock_method('get_campaigns')),
                            get_campaigns_by_ids=AsyncMock(side_effect=self.mock_method('get_campaigns_by_ids')),
                            get_single_campaign=AsyncMock(side_effect=self.mock_method('get_single_campaign')),
                            get_ads=AsyncMock(side_effect=self.mock_method('get_ads')),
                            get_ads_by_ids=AsyncMock(side_effect=self.mock_method('get_ads_by_ids')),
                            get_ad=AsyncMock(side_effect=self.mock_method('get_ad')),
                            get_ad_groups=AsyncMock(side_effect=self.mock_method('get_ad_groups')),
                            get_ad_groups_by_ids=AsyncMock(side_effect=self.mock_method('get_ad_groups_by_ids')),
                            get_changes_in_campaign=AsyncMock(side_effect=self.mock_method('get_changes_in_campaign')),
                            get_feeds=AsyncMock(side_effect=self.mock_method('get_feeds')),
                            get_turbopage=AsyncMock(side_effect=self.mock_method('get_turbopage')),
                            get_keywords=AsyncMock(side_effect=self.mock_method('get_keywords')),
                            get_bidmodifiers=AsyncMock(side_effect=self.mock_method('get_bidmodifiers')),
                            get_audiencetargets=AsyncMock(side_effect=self.mock_method('get_audiencetargets')),
                            get_client=AsyncMock(side_effect=self.mock_method('get_client'))):
            with patch.multiple(Direct4, get_account_management=AsyncMock(
                    side_effect=self.mock_method('get_account_management'))):
                await intent.act(self.conversation)
                self.test_case.assertEqual(self.conversation.response, self.expected_conversation.response)
                self.test_case.assertEqual(self.conversation.user_state, self.expected_conversation.user_state)

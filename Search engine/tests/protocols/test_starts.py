from dataclasses import field, dataclass
from itertools import chain
from typing import List
import asyncio

import pytest
import sqlalchemy as sa

from bot.aiowarden import Functionality
from bot.modules.protocols.const import Role
from bot.modules.protocols.models import Protocol
from mocks.bot import DChat, TChat, TUser, TMessage, TCallbackQuery
from mocks.context import temp_components, fast_component, temp_users, user

test_functionalities = [
    {
        "id": "starts_functionality1",
        "duty": [
            {
                "dutyRule": {
                    "abc": {"abcService": "service1f"},
                    "dutyTeam": [
                        "duty",
                        "flow"
                    ]
                },
                "onDuty": {
                    "objects": [
                        {"role": "role1f", "login": "login1f"},
                        {"role": "role1f", "login": "login2f"}
                    ]
                }
            },
            {
                "dutyRule": {
                    "calendar": {
                        "calendarId": "11111"
                    },
                    "type": "calendar",
                    "dutyTeam": [
                        "flow"
                    ]
                },
                "onDuty": {
                    "objects": [
                        {"role": "duty", "login": "login1f"},
                        {"role": "duty", "login": "login11f"}
                    ]
                }
            },
            {
                "dutyRule": {
                    "user": {"login": "login3f"},
                    "type": "user",
                    "dutyTeam": ["duty"],
                    "roleAlias": "role2f"
                },
                "onDuty": {
                    "objects": [
                        {"role": "role2f", "login": "login3f"}
                    ]
                }
            }
        ],
        "rewrite_component_duty": True
    },
    {
        "id": "starts_duty_only_functionality",
        "duty": [
            {
                "dutyRule": {
                    "user": {"login": "login8f"},
                    "type": "user",
                    "dutyTeam": ["duty"],
                    "roleAlias": "role4f"
                },
                "onDuty": {
                    "objects": [
                        {"role": "role4f", "login": "login8f"}
                    ]
                }
            }
        ],
        "rewrite_component_duty": True
    },
    {
        "id": "starts_flow_only_functionality",
        "duty": [
            {
                "dutyRule": {
                    "user": {"login": "login9f"},
                    "type": "user",
                    "dutyTeam": ["flow"],
                    "roleAlias": "role4f"
                },
                "onDuty": {
                    "objects": [
                        {"role": "role4f", "login": "login9f"}
                    ]
                }
            }
        ],
        "rewrite_component_duty": True
    },
    {
        "id": "starts_functionality2",
        "duty": [
            {
                "dutyRule": {
                    "user": {"login": "login8f"},
                    "type": "user",
                    "dutyTeam": ["duty"],
                    "roleAlias": "role4f"
                },
                "onDuty": {
                    "objects": [
                        {"role": "role4f", "login": "login8f"}
                    ]
                }
            }
        ]
    },
]


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context, monkeypatch):
    components = [
        fast_component(
            name='parent',
            owners=['test_parent_owner'],  # todo: create test user
            spi_chat='https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA',
            onduty=['test_parent_duty_a', 'test_parent_duty_b'],
            support=['test_support_duty'],
            flow=['test_flow_duty'],
            pr=['test_pr_duty'],
            functionalities=[Functionality('parent_functionality', 'функциональность родителя')]
        ),
        fast_component(
            name='test_with_chat',
            parent_name='parent',
            owners=['test_child_owner'],
            spi_chat='https://t.me/joinchat/BBBBBBBBBBBBBBBBBBBBBB',
            onduty=['test_child_duty_a', 'test_child_duty_b'],
            functionalities=[Functionality('child_functionality_a', 'сломалось всё')]
        ),
        fast_component(
            name='test_without_chat',
            parent_name='parent',
            owners=['test_child_owner'],
            onduty=['test_child_duty_a', 'test_child_duty_b'],
            functionalities=[Functionality('child_functionality_b', 'весь мир театр, а мы в нем актеры')],
        ),
        fast_component(
            name='test_functionalities',
            parent_name='parent',
            owners=['test_child_owner'],
            onduty=['test_child_duty_a', 'test_child_duty_b'],
            functionalities=[Functionality.from_data(x, dict(enable_new_duty_field=True)) for x in test_functionalities],
        ),
        fast_component(
            name='test_parent_duty',
            parent_name='parent',
            owners=['test_child_owner'],
            onduty=[],
            functionalities=[Functionality('child_functionality_c', 'сломалось всё')],
        ),
        fast_component(
            name='parent_noduty',
            owners=['test_parent_owner'],
            onduty=[],
            functionalities=[Functionality('parent_functionality_b', 'сломалось всё')],
        ),
        fast_component(
            name='test_owner_duty',
            parent_name='parent_noduty',
            owners=['test_child_owner'],
            onduty=[],
            functionalities=[Functionality('child_functionality_d', 'сломалось всё')],
        )
    ]

    _users = set()
    for component in components:
        _users.update(component.owners)
        _users.update(component.duty)
        _users.update(component.flow)
        _users.update(component.support)
        _users.update(component.pr)
        _users.update(component.smm)
        _users.update(chain(*(f.duty + f.flow for f in component.functionality_list)))

    with temp_users(
        get_context,
        *(user(x) for x in _users),
        user('test_marty', is_marty=True),
        user('marty', is_marty=True)
    ):
        with temp_components(get_context, *components):
            yield None


@dataclass
class Expectations:
    chat_title: str
    incident_id: str
    summary: str
    component_name: str
    parent_component_name: str = ''

    duty: List[str] = field(default_factory=list)
    manager: List[str] = field(default_factory=list)
    pr: List[str] = field(default_factory=list)
    support: List[str] = field(default_factory=list)


@dataclass
class MockCheck:
    host: str = ''
    service: str = ''


@dataclass
class MockNewIncidentRequest:
    incident_id: str
    description: str
    functionality_id: str
    ticket_task_id: str = ''
    check: MockCheck = None
    test_mode: bool = False


@pytest.mark.parametrize("mockrequest, expected", [
    (
        MockNewIncidentRequest(
            incident_id='test:incident7148125:1oASIDIAfuasj_sa',
            description='сломалось всё',
            functionality_id='child_functionality_a',
            check=MockCheck('test_host_check', 'test_service_check'),
        ),
        Expectations(
            chat_title='сломалось всё',
            incident_id='test:incident7148125:1oASIDIAfuasj_sa',
            summary='сломалось всё',
            component_name='test_with_chat',
            parent_component_name='parent',
            duty=['test_child_duty_a', 'test_child_duty_b']
        )
    ),
    (
        MockNewIncidentRequest(
            incident_id='test:incident11111:aaaaaaaa',
            description='сломалось всё',
            functionality_id='child_functionality_c',
            check=MockCheck('test_host_check', 'test_service_check'),
        ),
        Expectations(
            chat_title='сломалось всё',
            incident_id='test:incident11111:aaaaaaaa',
            summary='сломалось всё',
            component_name='test_parent_duty',
            parent_component_name='parent',
            duty=['test_flow_duty']
        )
    ),
    (
        MockNewIncidentRequest(
            incident_id='test:incident11111:aaaaaaaa',
            description='сломалось всё',
            functionality_id='child_functionality_d',
            check=MockCheck('test_host_check', 'test_service_check'),
        ),
        Expectations(
            chat_title='сломалось всё',
            incident_id='test:incident11111:aaaaaaaa',
            summary='сломалось всё',
            component_name='test_owner_duty',
            parent_component_name='parent_noduty',
            duty=['test_child_owner']
        )
    ),
    (
        MockNewIncidentRequest(
            incident_id='test:incident11111:aaaaaaaa',
            description='сломалось всё',
            functionality_id='starts_functionality1',
            check=MockCheck('test_host_check', 'test_service_check'),
        ),
        Expectations(
            chat_title='сломалось всё',
            incident_id='test:incident11111:aaaaaaaa',
            summary='сломалось всё',
            component_name='test_functionalities',
            parent_component_name='parent',
            duty=['login1f', 'login2f', 'login11f']
        )
    ),
    (
        MockNewIncidentRequest(
            incident_id='test:incident11111:aaaaaaaa',
            description='сломалось всё',
            functionality_id='starts_duty_only_functionality',
            check=MockCheck('test_host_check', 'test_service_check'),
        ),
        Expectations(
            chat_title='сломалось всё',
            incident_id='test:incident11111:aaaaaaaa',
            summary='сломалось всё',
            component_name='test_functionalities',
            parent_component_name='parent',
            duty=['login8f']
        )
    ),
    (
        MockNewIncidentRequest(
            incident_id='test:incident11111:aaaaaaaa',
            description='сломалось всё',
            functionality_id='starts_flow_only_functionality',
            check=MockCheck('test_host_check', 'test_service_check'),
        ),
        Expectations(
            chat_title='сломалось всё',
            incident_id='test:incident11111:aaaaaaaa',
            summary='сломалось всё',
            component_name='test_functionalities',
            parent_component_name='parent',
            duty=['login9f']
        )
    ),
    (
        MockNewIncidentRequest(
            incident_id='test:incident11111:aaaaaaaa',
            description='сломалось всё',
            functionality_id='starts_functionality2',
            check=MockCheck('test_host_check', 'test_service_check'),
        ),
        Expectations(
            chat_title='сломалось всё',
            incident_id='test:incident11111:aaaaaaaa',
            summary='сломалось всё',
            component_name='test_functionalities',
            parent_component_name='parent',
            duty=['login8f', 'test_child_duty_a', 'test_child_duty_b']
        )
    ),
])
@pytest.mark.asyncio
async def test_new_incident(proto_module, get_context, mockrequest, expected, use_flag_enable_functionalities_duty):
    await proto_module.proto_context.incidents.start_incident(mockrequest, ctx=None)

    async with await get_context.data.connect() as conn:
        proto = await Protocol.filter(conn, sa.select([Protocol]).where(Protocol.incident_id == expected.incident_id))
        assert proto

    proto = proto[0]
    assert proto.chat_id

    chat: DChat = get_context.bot.find_chat(proto.chat_id)
    assert chat

    data = await proto.to_json(get_context)

    # common

    assert data['level'] == 'green'
    assert data['in_progress']
    assert data['etag']
    assert data['summary'] == expected.summary
    assert data['component_name'] == expected.component_name
    assert data['parent_component_name'] == expected.parent_component_name

    # roles
    assert len(expected.duty) == len(data['roles']['duty'])
    assert len(expected.manager) == len(data['roles']['manager'])
    assert len(expected.pr) == len(data['roles']['pr'])
    assert len(expected.support) == len(data['roles']['support'])

    for attr in ['duty', 'manager', 'pr', 'support']:
        for user_ in data['roles'][attr]:
            username = user_['username']
            assert username in getattr(expected, attr)
            assert chat.admin_ranks[username] == getattr(Role, attr).human_single

    assert data['chat']
    assert data['chat']['join_url']

    # [:2] because of random emoji at the start
    assert data['chat']['title'][2:] == expected.chat_title
    assert data['chat']['pin_message_id'] == str(chat.pinned_message)


@pytest.mark.asyncio
async def test_callback_race(proto_module, get_context):
    created_chat = DChat(TChat())
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    keyboard = created_chat.messages[-1]

    callback = TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='protocols|start_new_chat:green::1'
    )

    await asyncio.gather(*[get_context.bot.call_callback_query(callback) for _ in range(4)])

    async with await get_context.data.connect() as conn:
        protocols = await Protocol.list_all(conn)
    assert len(protocols) == 1

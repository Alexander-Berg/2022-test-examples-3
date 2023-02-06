import pytest

from bot.aiowarden import Component, OnDuty


@pytest.fixture(scope='class')
def warden_components(get_context):
    result = get_context.warden
    result.init_components(
        [
            Component.from_data(dict(name='parent', parentComponentName='')),
            Component.from_data(dict(name='parent_component2', parentComponentName='')),
            Component.from_data(dict(name='component_parent', parentComponentName='')),
            Component.from_data(dict(name='component1', parentComponentName='parent')),
            Component.from_data(dict(name='component2', parentComponentName='parent_component2')),
            Component.from_data(dict(name='component3', parentComponentName='parent_component2')),
            Component.from_data(dict(name='component4', parentComponentName='component_parent')),
            Component.from_data(dict(name='web', humanReadableName='Поиск (web)')),
            Component.from_data(dict(name='images', humanReadableName='Поиск (images)'))
        ]
    )
    yield result


duty_components = [
    {
        'name': 'service',
        'dutyList': {
            'objects': [
                {
                    'abcService': 'service',
                    'onDuty': [
                        {'role': 'role1', 'login': 'login1'},
                        {'role': 'role1', 'login': 'login4'},
                    ],
                    'dutyTeam': ['duty']
                },
                {
                    'abcService': 'service',
                    'onDuty': [
                        {'role': 'role2', 'login': 'login2'}
                    ],
                    'dutyTeam': ['flow']
                },
                {
                    'abcService': 'service',
                    'onDuty': [
                        {'role': 'role3', 'login': 'login3'}
                    ],
                    'dutyTeam': ['flow', 'duty']
                }
            ],
            'onDuty': [
                {'role': 'role1', 'login': 'login1'},
                {'role': 'role2', 'login': 'login2'},
                {'role': 'role3', 'login': 'login3'},
                {'role': 'role1', 'login': 'login4'},
            ]
        },
        'duty': [
            {
                "dutyRule": {
                    "abc": {"abcService": "service"},
                    "dutyTeam": ["duty"],
                    "roleAlias": "role1"
                },
                "onDuty": {
                    "objects": [
                        {'role': 'role1', 'login': 'login1'},
                        {'role': 'role1', 'login': 'login4'},
                    ]
                },
            },
            {
                "dutyRule": {
                    "abc": {"abcService": "service"},
                    "dutyTeam": ["flow"],
                    "roleAlias": "role2"
                },
                "onDuty": {"objects": [{'role': 'role2', 'login': 'login2'}]},
            },
            {
                "dutyRule": {
                    "abc": {"abcService": "service"},
                    "dutyTeam": ["duty", "flow"],
                    "roleAlias": "role3"
                },
                "onDuty": {"objects": [{'role': 'role3', 'login': 'login3'}]},
            },
        ],
        'ownerList':
            [
                {"id": "1", "login": "login5"},
            ],
        'functionalityList': [
            {
                "id": "functionality1",
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
                            "dutyTeam": ["flow"],
                            "roleAlias": "role2f"
                        },
                        "onDuty": {
                            "objects": [
                                {"role": "role2f", "login": "login3f"}
                            ]
                        }
                    }
                ]
            },
            {
                "id": "duty_only_functionality",
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
            {
                "id": "flow_only_functionality",
                "duty": [
                    {
                        "dutyRule": {
                            "user": {"login": "login8f"},
                            "type": "user",
                            "dutyTeam": ["flow"],
                            "roleAlias": "role4f"
                        },
                        "onDuty": {
                            "objects": [
                                {"role": "role4f", "login": "login8f"}
                            ]
                        }
                    }
                ]
            }

        ]
    },
    {
        'name': 'service_wo_duty',
        'ownerList':
            [
                {"id": "1", "login": "owner"},
            ],
        'functionalityList': [
            {'id': 'empty_functionality'}
        ]
    },
    {
        'name': 'service_no_roles_duty',
        'ownerList':
            [
                {"id": "1", "login": "owner"},
            ],
        'dutyList': {
            'objects': [
                {
                    "login": "login1"
                }
            ],
        },
        'duty': [
            {
                "dutyRule": {
                    "user": {"login": "login1"},
                    "type": "user"
                },
                "onDuty": {
                    "objects": [
                        {"role": "user_duty", "login": "login1"}
                    ]
                }
            },
        ]
    },
    {
        'name': 'service_duty_teams',
        'ownerList':
            [
                {"id": "1", "login": "owner"},
            ],
        'dutyList': {
            'objects': [
                {
                    "login": "login1",
                    "dutyTeam": ["support"]
                },
                {
                    "abcService": "notes_pr",
                    "dutyTeam": ["pr"]
                }
            ],
        },
        'duty': [
            {
                "dutyRule": {
                    "user": {
                        "login": "login1"
                    },
                    "type": "user",
                    "dutyTeam": [
                        "support"
                    ]
                },
                "onDuty": {
                    "objects": [
                        {
                            "role": "user_duty",
                            "login": "login1"
                        }
                    ]
                },
            },
            {
                "dutyRule": {
                    "abc": {
                        "abcService": "notes_pr"
                    },
                    "dutyTeam": [
                        "pr"
                    ]
                },
                "onDuty": {
                    "objects": [
                        {
                            "role": "pr-role1",
                            "login": "login3"
                        },
                        {
                            "role": "pr-role1",
                            "login": "login2"
                        }
                    ]
                },
            },
        ]
    }
]


@pytest.fixture(scope='class')
def warden_duty_components(get_context):
    result = get_context.warden
    flags = dict(enable_new_duty_field=True)
    result.init_components([Component.from_data(x, flags) for x in duty_components])
    yield result


@pytest.fixture(scope='class')
def get_duty_components():
    flags = dict(enable_new_duty_field=False)
    return {x['name']: Component.from_data(x, flags) for x in duty_components}


@pytest.fixture(scope='class')
def get_duty_components_from_new_field():
    flags = dict(enable_new_duty_field=True)
    return {x['name']: Component.from_data(x, flags) for x in duty_components}


@pytest.fixture(scope='class')
def get_duty_functionalities(get_duty_components_from_new_field):
    return {x.id: x for x in get_duty_components_from_new_field['service'].functionality_list}


class TestWardenSearch:
    test_cases = [
        ('', '', ''),
        (None, '', ''),
        ('abc', '', ''),
        ('', 'parent', ''),

        ('component1', '', 'component1'),
        ('component1 ', '', 'component1'),
        ('component2', '', 'component2 component3 parent_component2'),
        ('parent_component2', '', 'component2 component3 parent_component2'),
        ('parent_component2/', '', 'component2 component3'),
        ('parent_component2/component2', '', 'component2'),
        ('parent_', '', 'parent_component2 component2 component3'),
        ('component', '', 'component1 component2 component3 component4 parent_component2 component_parent'),
        ('c', '', 'component1 component2 component3 component4 parent_component2 component_parent'),
        ('n', '', 'component1 component2 component3 component4 parent parent_component2 component_parent'),

        ('component1', 'parent', 'component1'),
        ('parent/component1', '', 'component1'),
        ('parent/component1', '', 'component1'),
        ('component2', 'parent', ''),

        ('web', '', 'web'),
        ('поиск', '', 'web images'),
    ]

    @pytest.mark.parametrize("name,parent,expected", test_cases)
    def test_search_components(self, name, parent, expected, warden_components):
        assert set([c.name for c in warden_components.search_components(name, parent)]) == set(expected.split())


@pytest.mark.asyncio
async def test_warden_duty(warden_duty_components):
    service = await warden_duty_components.find_component(name='service_wo_duty')
    assert service.owners
    assert service.owners == ['owner']
    assert service.onduty
    assert service.onduty == [OnDuty(role='owner', login='owner')]

    service = await warden_duty_components.find_component(name='service_no_roles_duty')
    assert service.onduty
    assert service.onduty != [OnDuty(role='owner', login='owner')]


@pytest.mark.asyncio
async def test_components_onduty(get_duty_components, get_duty_components_from_new_field):
    service = get_duty_components['service']
    assert service.onduty
    assert service.flow
    no_flag = service

    service = get_duty_components_from_new_field['service']
    assert service.onduty
    assert service.flow

    assert set(service.flow) == set(no_flag.flow)
    assert {x.login for x in no_flag.onduty}.difference(x.login for x in service.onduty) == {'login2'}
    # onduty should not include `flow` only team

    service = get_duty_components['service_duty_teams']
    assert service.onduty
    assert service.onduty == [OnDuty(role='owner', login='owner')]
    assert service.support
    no_flag = service

    service = get_duty_components_from_new_field['service_duty_teams']
    assert service.onduty
    assert service.onduty == [OnDuty(role='owner', login='owner')]
    assert service.support
    assert service.support == no_flag.support
    assert service.pr
    assert set(service.pr) == {'login2', 'login3'}


@pytest.mark.asyncio
async def test_functionalities_onduty(get_duty_components, get_duty_components_from_new_field, get_duty_functionalities):
    service = get_duty_components['service']
    assert service.functionality_list
    for functionality in service.functionality_list:
        assert functionality.flow == functionality.onduty == []

    service = get_duty_components_from_new_field['service']
    assert service.functionality_list
    assert len(service.functionality_list[0].flow) == 4
    assert set(service.functionality_list[0].flow) == {'login1f', 'login2f', 'login3f', 'login11f'}

    func = get_duty_functionalities['functionality1']
    assert func.onduty == [OnDuty(role='role1f', login='login1f'), OnDuty(role='role1f', login='login2f')]

    func = get_duty_functionalities['duty_only_functionality']
    assert func.flow == []
    assert func.onduty == [OnDuty(role='role4f', login='login8f', type='user')]

    func = get_duty_functionalities['flow_only_functionality']
    assert func.onduty == []
    assert func.flow == ['login8f']

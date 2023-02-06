# coding: utf-8

import pytest

from hamcrest import (
    assert_that,
    equal_to,
)

from .util import convert_yt_str

from market.idx.tools.pbsn2yt.yatf.messages import Messages
from market.idx.tools.pbsn2yt.yatf.resources.pbsn_input import PbsnInput
from market.idx.tools.pbsn2yt.yatf.resources.yt_output import YtOutput
from market.idx.tools.pbsn2yt.yatf.test_env import Pbsn2YtTestEnv


@pytest.fixture(scope='module', params=[
    {
        'type': 'with_array',
        'name': 'empty_string_array',
        'expand': 'array_value',
        'input': [
            {
                'string_value': 'string',
                'array_value': []
            }
        ],
        'output': [
        ]
    },
    {
        'type': 'with_array',
        'name': 'string_array',
        'expand': 'array_value',
        'input': [
            {
                'string_value': 'string',
                'array_value': ['one', 'two', 'three']
            }
        ],
        'output': [
            {
                'string_value': 'string',
                'array_value': 'one'
            },
            {
                'string_value': 'string',
                'array_value': 'two'
            },
            {
                'string_value': 'string',
                'array_value': 'three'
            }
        ]
    },
    {
        'type': 'with_messages',
        'name': 'empty_sub_messages',
        'expand': 'messages_value',
        'input': [
            {
                'string_value': 'string',
                'messages_value': []
            }
        ],
        'output': [
        ]
    },
    {
        'type': 'with_messages',
        'name': 'sub_messages',
        'expand': 'messages_value',
        'input': [
            {
                'string_value': 'string',
                'messages_value': [
                    {
                        'string_value': 'string1',
                    },
                    {
                        'bytes_value': 'deadface'
                    }
                ]
            }
        ],
        'output': [
            {
                'string_value': 'string',
                'messages_value.string_value': 'string1',
            },
            {
                'string_value': 'string',
                'messages_value.bytes_value': b'deadface',
            }
        ]
    }
])
def data(request):
    return request.param


@pytest.yield_fixture(scope='module')
def pbsn2yt_workflow(data, yt_server):
    resources = {
        'input': PbsnInput(
            data=Messages(type=data['type'], data=data['input'])
        ),
        'output': YtOutput(
            yt_server.get_server(),
            yt_server.get_yt_client(),
            # we use different tables in case tests are run in
            # parallel
            '//home/table_expand_{}'.format(data['name']),
            format='yson',
            expand=data['expand']
        ),
    }
    with Pbsn2YtTestEnv(**resources) as e:
        e.execute()
        e.verify()
        yield e


def test_json(data, pbsn2yt_workflow):
    res = pbsn2yt_workflow.result
    for fact, exp in zip(res, data['output']):
        if 'messages_value.bytes_value' in exp:
            assert_that('messages_value.bytes_value' in fact, "Bytes field is presented.")
            fact['messages_value.bytes_value'] = convert_yt_str(
                fact['messages_value.bytes_value']
            )
        assert_that(fact, equal_to(exp))

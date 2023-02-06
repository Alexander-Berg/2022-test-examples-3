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
        'type': 'simple',
        'name': 'empty',
        'input': [{}],
        'output': [{}],
    },
    {
        'type': 'simple',
        'name': 'message_full',
        'input': [
            {
                'int32_value': 100,
                'int64_value': 500,
                'uint32_value': 100,
                'uint64_value': 500,
                'string_value': 'string',
                'bytes_value': 'deadbeef',
            }
        ],
        'output': [
            {
                'int32_value': 100,
                'int64_value': 500,
                'uint32_value': 100,
                'uint64_value': 500,
                'string_value': 'string',
                'bytes_value': b'deadbeef',
            }
        ]
    },
    {
        'type': 'simple',
        'name': 'message_partial',
        'input': [
            {
                'string_value': 'string',
                'bytes_value': 'deadface',
            }
        ],
        'output': [
            {
                'string_value': 'string',
                'bytes_value': b'deadface',
            }
        ]
    },
    {
        'type': 'with_array',
        'name': 'empty_string_array',
        'input': [
            {
                'string_value': 'string',
                'array_value': []
            }
        ],
        'output': [
            {
                'string_value': 'string',
                'array_value': '[]'
            }
        ]
    },
    {
        'type': 'with_array',
        'name': 'string_array',
        'input': [
            {
                'string_value': 'string',
                'array_value': ['one', 'two', 'three']
            }
        ],
        'output': [
            {
                'string_value': 'string',
                'array_value': '["one","two","three"]'
            }
        ]
    },
    {
        'type': 'with_message',
        'name': 'empty_sub_message',
        'input': [
            {
                'string_value': 'string'
            }
        ],
        'output': [
            {
                'string_value': 'string'
            }
        ]
    },
    {
        'type': 'with_message',
        'name': 'sub_message',
        'input': [
            {
                'string_value': 'string',
                'message_value': {
                    'bytes_value': 'deadbeef'
                }
            }
        ],
        'output': [
            {
                'string_value': 'string',
                'message_value': '{"bytes_value":"deadbeef"}'
            }
        ]
    },
    {
        'type': 'with_messages',
        'name': 'empty_sub_messages',
        'input': [
            {
                'string_value': 'string',
                'messages_value': []
            }
        ],
        'output': [
            {
                'string_value': 'string',
                'messages_value': '[]'
            }
        ]
    },
    {
        'type': 'with_messages',
        'name': 'sub_messages',
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
                'messages_value': '[{"string_value":"string1"},{"bytes_value":"deadface"}]'
            }
        ]
    }
])
def data(request):
    return request.param


@pytest.yield_fixture(scope='module')
def pbsn2yt_workflow(data, yt_server):
    resources = {
        'input': PbsnInput(data=Messages(type=data['type'], data=data['input'])),
        'output': YtOutput(yt_server.get_server(),
                           yt_server.get_yt_client(),
                           # we use different tables in case tests are run in
                           # parallel
                           '//home/table_json_{}'.format(data['name']),
                           # we use json because simple output insert default
                           # values for fields missing in protobuf
                           format='json')
    }
    with Pbsn2YtTestEnv(**resources) as e:
        e.execute()
        e.verify()
        yield e


def test_json(data, pbsn2yt_workflow):
    res = pbsn2yt_workflow.result
    for fact, exp in zip(res, data['output']):
        if 'bytes_value' in data['input'][0]:
            assert_that('bytes_value' in fact, "Bytes field is presented.")
            fact['bytes_value'] = convert_yt_str(fact['bytes_value'])
        assert_that(fact, equal_to(exp))

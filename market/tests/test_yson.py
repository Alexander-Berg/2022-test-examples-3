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
        'data': [{}],
        'output': [{}],
    },
    {
        'type': 'simple',
        'name': 'message_full',
        'data': [
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
        'data': [
            {
                'string_value': 'string',
                'bytes_value': b'deadface',
            }
        ]
    },
    {
        'type': 'with_array',
        'name': 'empty_string_array',
        'data': [
            {
                'string_value': 'string',
                'array_value': []
            }
        ]
    },
    {
        'type': 'with_array',
        'name': 'string_array',
        'data': [
            {
                'string_value': 'string',
                'array_value': ['one', 'two', 'three']
            }
        ]
    },
    {
        'type': 'with_message',
        'name': 'empty_sub_message',
        'data': [
            {
                'string_value': 'string'
            }
        ]
    },
    {
        'type': 'with_message',
        'name': 'sub_message',
        'data': [
            {
                'string_value': 'string',
                'message_value': {
                    'bytes_value': b'deadbeef'
                }
            }
        ]
    },
    {
        'type': 'with_messages',
        'name': 'empty_sub_messages',
        'data': [
            {
                'string_value': 'string',
                'messages_value': []
            }
        ]
    },
    {
        'type': 'with_messages',
        'name': 'sub_messages',
        'data': [
            {
                'string_value': 'string',
                'messages_value': [
                    {
                        'string_value': 'string1',
                    },
                    {
                        'bytes_value': b'deadface'
                    }
                ]
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
            data=Messages(type=data['type'], data=data['data'])
        ),
        'output': YtOutput(
            yt_server.get_server(),
            yt_server.get_yt_client(),
            # we use different tables in case tests are run in
            # parallel
            '//home/table_yson_{}'.format(data['name']),
            format='yson'
        ),
    }
    with Pbsn2YtTestEnv(**resources) as e:
        e.execute()
        e.verify()
        yield e


def test_yson(data, pbsn2yt_workflow):
    res = pbsn2yt_workflow.result
    for fact, exp in zip(res, data['data']):
        if 'message_value' in fact:
            mes_value = fact['message_value']
            if 'bytes_value' in mes_value:
                mes_value['bytes_value'] = convert_yt_str(mes_value['bytes_value'])
            fact['message_value'] = mes_value
        if 'messages_value' in fact:
            mes_values = fact['messages_value']
            msg = []
            for value in mes_values:
                if 'bytes_value' in value:
                    value['bytes_value'] = convert_yt_str(value['bytes_value'])
                msg.append(value)
            fact['messages_value'] = msg
        if 'bytes_value' in exp:
            assert_that('bytes_value' in fact, "Bytes field is presented.")
            fact['bytes_value'] = convert_yt_str(fact['bytes_value'])
        assert_that(fact, equal_to(exp))

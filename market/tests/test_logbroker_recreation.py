# coding: utf-8

import pytest
import random
import string
from hamcrest import (
    assert_that,
    calling,
    equal_to,
    raises,
)
from mock import patch

from kikimr.public.sdk.python.persqueue.errors import (
    ActorNotReadyException,
    ActorTerminatedException,
    SessionClosedException,
)


def random_string(stringLength=10):
    """Generate a random string of fixed length """
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) for i in range(stringLength))


@pytest.fixture(
    scope='module',
    params=[
        {
            'effect': ActorNotReadyException('ActorNotReadyException'),
            'expected_call_count': 4,
        },
        {
            'effect': ActorTerminatedException('ActorTerminatedException'),
            'expected_call_count': 4,
        },
        {
            'effect': SessionClosedException('SessionClosedException'),
            'expected_call_count': 4,
        },
        {
            'effect': RuntimeError('RuntimeError'),
            'expected_call_count': 1,
        },
        {
            'effect': ValueError('ValueError'),
            'expected_call_count': 1,
        },
    ],
    ids=[
        'RetryableActorNotReadyException',
        'RetryableActorTerminatedException',
        'RetryableSessionClosedException',
        'RuntimeError',
        'ValueError',
    ],
)
def raised_effect(request):
    return request.param


def test_write(
        raised_effect,
        lbk_client,
        log_broker_stuff
):
    effect = raised_effect['effect']
    topic_name = random_string(10)
    log_broker_stuff.create_topic(topic_name)
    data = ['testMessage']

    with patch(
        (
            'kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api.'
            'PQStreamingProducer._send_request'
        ),
        autospec=True,
        side_effect=effect
    ) as mock_func:
        assert_that(
            calling(
                lbk_client.simple_write
            ).with_args(
                data=data,
                topic=topic_name,
                source_id='LbkTestWriter',
            ),
            raises(type(effect), str(effect))
        )
        assert_that(1, equal_to(mock_func.call_count))


def test_read_with_effect_without_commit(
        raised_effect,
        lbk_client,
        log_broker_stuff
):
    effect = raised_effect['effect']
    expected_call_count = raised_effect['expected_call_count']
    topic_name = random_string(10)

    log_broker_stuff.create_topic(topic_name)
    data = ['testMessage']
    lbk_client.simple_write(
        data=data,
        topic=topic_name,
        source_id='LbkTestWriter',
    )

    with patch(
        (
            'kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api.'
            'PQStreamingConsumer.next_event'
        ),
        autospec=True,
        side_effect=effect
    ) as mock_func:
        assert_that(
            calling(
                lbk_client.simple_read
            ).with_args(
                topic_name,
                'LbkTestReader',
                count=len(data),
                do_commit=False
            ),
            raises(type(effect), str(effect))
        )
        assert_that(expected_call_count, equal_to(mock_func.call_count))


def test_read_with_effect_with_commit(
        raised_effect,
        lbk_client,
        log_broker_stuff
):
    effect = raised_effect['effect']
    expected_call_count = raised_effect['expected_call_count']
    topic_name = random_string(10)

    log_broker_stuff.create_topic(topic_name)
    data = ['testMessage']
    lbk_client.simple_write(
        data=data,
        topic=topic_name,
        source_id='LbkTestWriter',
    )

    with patch(
        (
            'kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api.'
            'PQStreamingConsumer.commit'
        ),
        autospec=True,
        side_effect=effect
    ) as mock_func:
        assert_that(
            calling(
                lbk_client.simple_read
            ).with_args(
                topic_name,
                'LbkTestReader',
                count=len(data),
                do_commit=True
            ),
            raises(type(effect), str(effect))
        )
        assert_that(expected_call_count, equal_to(mock_func.call_count))

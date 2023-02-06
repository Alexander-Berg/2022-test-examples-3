# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, has_properties

from common.email_sender import guaranteed_send_email, EmailIntent

pytestmark = pytest.mark.mongouser('module')


def preprocessor():
    pass


DEFAULT_KWARGS = {
    'key': 'foo',
    'to_email': 'bar',
    'args': {'quz': 'qux'},
    'campaign': 'camp'
}


@pytest.mark.parametrize('kwargs, expected', [
    (
        {'attachments': None, 'data': None, 'log_context': None, 'preprocessor': None},
        {'attachments': [], 'data': {}, 'log_context': {}, 'preprocessor': None}
    ),
    (
        {'attachments': [], 'data': {'foo': 1}, 'log_context': {'bar': 2}, 'preprocessor': 'foo.bar'},
        {'attachments': [], 'data': {'foo': 1}, 'log_context': {'bar': 2}, 'preprocessor': 'foo.bar'}
    )
])
def test_guaranteed_send_email_ok(kwargs, expected):
    kwargs.update(DEFAULT_KWARGS)
    with mock.patch('common.email_sender.sender.send_email', autospec=True) as m_send_email:
        email_id = guaranteed_send_email(**kwargs)
    assert m_send_email.mock_calls == [mock.call.apply_async([str(email_id)])]
    email_intent = EmailIntent.objects(id=email_id).get()
    assert_that(email_intent, has_properties(**expected))


def test_guaranteed_send_email_preprocessor_as_func():
    with mock.patch('common.email_sender.sender.send_email', autospec=True):
        email_id = guaranteed_send_email(preprocessor=preprocessor, **DEFAULT_KWARGS)
    preprocessor_fq_name = '{}.{}'.format(preprocessor.__module__, preprocessor.__name__)
    assert EmailIntent.objects(id=email_id).get().preprocessor == preprocessor_fq_name


def test_guaranteed_send_email_failed():
    with mock.patch('common.email_sender.sender.send_email', autospec=True):
        email_id = guaranteed_send_email(**DEFAULT_KWARGS)
        assert email_id is not None

    with mock.patch('common.email_sender.sender.send_email', autospec=True) as m_send_email:
        email_id = guaranteed_send_email(**DEFAULT_KWARGS)
        assert email_id is None
        assert not m_send_email.call_count

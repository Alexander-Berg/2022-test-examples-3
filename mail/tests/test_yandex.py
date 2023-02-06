from cpp import isYandexEmail
from mail.catdog.catdog.src import yandex
from mail.catdog.catdog.src import config
from mail.catdog.catdog.src.parser import RecipientAddress, Recipient
import mail.catdog.catdog.tests.data.yandex as data
from mail.catdog.catdog.tests.mock_context import MockContext
from cpp import domainsInit

import mock
import pytest


def test_is_yandex_email_positive():
    assert isYandexEmail(RecipientAddress(domain='yandex.ru').domain) is True


def test_is_yandex_email_negative():
    assert isYandexEmail(RecipientAddress(domain='yandex.not').domain) is False


def test_is_yandex_email_pdd():
    domainsInit(['pdd'])
    assert isYandexEmail(RecipientAddress(domain='pdd').domain) is True


@pytest.mark.parametrize(('blackbox', 'ava', 'warn'), [
    (data.with_ava, '24700/51364272-3439956', False),
    (data.with_ava_and_bad_karma, '24700/51364272-3439956', True),
    (data.without_ava, None, False),
    (data.without_ava_and_bad_karma, None, True),
    (data.no_user, None, True),
    (data.error, None, False),
    (data.not_json, None, False)
])
def test_parse_blackbox(blackbox, ava, warn):
    ctx = MockContext()
    ctx.check_warn = True
    assert (ava, warn, False) == yandex.parse_blackbox(blackbox, ctx)


def test_parse_bb_check_warn_off():
    assert ('24700/51364272-3439956', False, False) == yandex.parse_blackbox(data.with_ava_and_bad_karma, MockContext())


@pytest.mark.parametrize(('blackbox', 'uid', 'is_self'), [
    (data.with_ava, '51364272', True),
    (data.with_ava, '123', False),
    (data.with_ava, None, False),
    (data.without_ava, '123', False),
    (data.no_user, '123', False),
    (data.error, '123', False),
    (data.not_json, '123', False)
])
def test_parse_self(blackbox, uid, is_self):
    ctx = MockContext()
    ctx.uid = uid
    _, _, res = yandex.parse_blackbox(blackbox, ctx)
    assert res is is_self


@pytest.mark.asyncio
async def test_yandex_with_ava():
    with mock.patch('mail.catdog.catdog.src.yandex.Client', autospec=True) as http_mock:
        http_mock.return_value.get = get_mock_with_ava
        assert yandex.Client is http_mock
        config.instance.yandex_avatar_mask = '{0} {1}'
        config.instance.yandex_avatar_size = 'size'
        config.instance.yandex_avatar_size_ie = 'size_ie'
        expected = '24700/51364272-3439956 size'
        expected_ie = '24700/51364272-3439956 size_ie'
        ctx = MockContext()
        ctx.uid = '51364272'
        recipient = Recipient(RecipientAddress('dn', 'l', 'd'), ctx)
        bb_response = await yandex.call_bb(recipient, ctx)
        email = yandex.do(recipient, ctx, bb_response)
        assert email.ava.type == 'avatar'
        assert email.ava.url == expected
        assert email.ava.url_small == expected_ie
        assert email.ava.url_mobile == expected
        assert email.warn is False
        assert email.is_self is True


async def get_mock_with_ava(dst, ctx, url):
    return data.with_ava

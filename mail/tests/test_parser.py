import pytest

from mail.catdog.catdog.src import parser
from mail.catdog.catdog.src.icon import Icon, IconTraits
from cpp import Avatar
from mail.catdog.catdog.tests.mock_context import MockContext


@pytest.mark.parametrize(('raw', 'display_name', 'local', 'domain', 'valid'), [
    (' "dn" <l@d>', 'dn', 'l', 'd', True),
    ('"dn" l@d', 'dn', 'l', 'd', True),
    ('"dn" l+x@d', 'dn', 'l', 'd', True),
    ('"dn" <ld>', None, None, None, False),
    ('xxx', None, None, None, False)
])
def test_parse_email(raw, display_name, local, domain, valid):
    ctx = MockContext()
    emails = list(parser.parse_email(raw, ctx))
    assert len(emails) == 1
    email = emails[0]
    assert email.valid == valid
    assert email.display_name == display_name
    assert email.local == local
    assert email.domain == domain


def test_set_ava_link():
    ctx = MockContext()
    r = parser.Recipient(parser.RecipientAddress(), ctx)
    avatar = Avatar('common link', 'link for ie')
    r.set_ava(avatar)

    assert r.ava.type == 'avatar'
    assert r.ava.url == 'common link'
    assert r.ava.url_small == 'link for ie'
    assert r.ava.url_mobile == 'common link'


def test_set_ava_icon():
    ctx = MockContext()
    r = parser.Recipient(parser.RecipientAddress(), ctx)
    traits = IconTraits("icon.{0}.png")
    icon = Icon('icon_name', traits)
    r.set_ava(icon)

    assert r.ava.type == 'icon'
    assert r.ava.name == 'icon_name'
    assert r.ava.url_mobile == 'icon.icon_name.png'

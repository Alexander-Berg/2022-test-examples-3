from mail.catdog.catdog.src import top200
from mail.catdog.catdog.src.parser import RecipientAddress, Recipient
from mail.catdog.catdog.tests.mock_context import MockContext
from mail.catdog.catdog.tests.mock_s3 import MockS3
from mail.catdog.catdog.src.icon import IconTraits
from mail.catdog.catdog.src.config import cfg
from cpp import getDomainWithoutSubdomains, getKey
from mail.catdog.catdog.src.data.top200 import map_of_top_senders_icons

import pytest


@pytest.mark.parametrize(('email', 'domain'), [
    (RecipientAddress(domain='domain'), 'domain'),
    (RecipientAddress(domain='doma.in'), 'doma.in'),
    (RecipientAddress(domain='do.ma.in'), 'ma.in'),
    (RecipientAddress(domain='d.o.ma.in'), 'ma.in'),
    (RecipientAddress(domain='service.org.spb.ru'), 'org.spb.ru')
])
def test_get_domain_without_subdomains(email, domain):
    assert getDomainWithoutSubdomains(email.domain) == domain


@pytest.mark.parametrize(('email', 'key'), [
    (RecipientAddress(domain='a.ru'), 'a.ru'),
    (RecipientAddress(domain='b.c.ru'), 'c.ru'),
    (RecipientAddress(domain='d.e.ru'), 'd.e.ru')
])
def test_get_key(email, key):
    assert getKey(email, {
        'a.ru': '1',
        'c.ru': '1',
        'd.e.ru': '1'
    }) == key


def test_get_key_none():
    assert getKey(RecipientAddress(domain='something'), {'some': 'thing'}) is None


def test_is_top_positive():
    assert top200.is_top(RecipientAddress(domain='mvideo.ru')) is True


def test_is_top_negative():
    assert top200.is_top(RecipientAddress(domain='hz-kto.ru')) is False
    assert top200.is_top(RecipientAddress(domain='mail.ru')) is False


def test_do():
    ctx = MockContext()
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    email = top200.do(Recipient(RecipientAddress(domain='mvideo.ru'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'mvideo.ru'
    assert email.ava.url_mobile == 'static_mobile_icon_link.mvideo.ru.png'
    assert email.warn is False
    assert email.is_self is False


def test_do_noreply_negative():
    ctx = MockContext()
    ctx.check_warn = True
    email = top200.do(Recipient(RecipientAddress(domain='mvideo.ru'), ctx), ctx)
    assert email.warn is False
    assert email.is_self is False


def test_do_noreply_positive():
    ctx = MockContext()
    ctx.check_warn = True
    email = top200.do(Recipient(RecipientAddress(local='noreply', domain='mvideo.ru'), ctx), ctx)
    assert email.warn is True
    assert email.is_self is False


def test_do_three_level():
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    ctx = MockContext()
    ctx.check_warn = True
    email = top200.do(Recipient(RecipientAddress(local='noreply', domain='daily.afisha.ru'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'daily.afisha.ru'
    assert email.ava.url_mobile == 'static_mobile_icon_link.daily.afisha.ru.png'
    assert email.warn is True
    assert email.is_self is False


def test_do_after_reload():
    ctx = MockContext()
    s3 = MockS3()
    s3.add_dictionary('services.json', map_of_top_senders_icons)
    s3.add_key('services.json', 'doctor.who.tardis.ru', 'doctor.who.tardis.ru')
    top200.load(s3)
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    email = top200.do(Recipient(RecipientAddress(domain='doctor.who.tardis.ru'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'doctor.who.tardis.ru'
    assert email.ava.url_mobile == 'static_mobile_icon_link.doctor.who.tardis.ru.png'
    assert email.warn is False
    assert email.is_self is False

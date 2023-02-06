import pytest

from mail.catdog.catdog.src import yasrv
from mail.catdog.catdog.src.parser import Recipient, RecipientAddress
from mail.catdog.catdog.tests.mock_context import MockContext
from mail.catdog.catdog.tests.mock_s3 import MockS3
from mail.catdog.catdog.src.config import cfg
from mail.catdog.catdog.src.icon import IconTraits
from mail.catdog.catdog.src.data.yasrv import map_of_yandex_services_icons_by_domain, map_of_yandex_services_icons_by_email


@pytest.mark.parametrize(('email', 'domain_normalized'), [
    (RecipientAddress(domain='domain'), 'domain'),
    (RecipientAddress(domain='yandex.ru'), 'Y'),
    (RecipientAddress(domain='yandex.com.tr'), 'Y'),
    (RecipientAddress(domain='taxi.yandex.ru'), 'taxi.Y'),
    (RecipientAddress(domain='mail.yandex.ru'), 'mail.Y'),
    (RecipientAddress(domain='mail.yandex.com.tr'), 'mail.Y'),
    (RecipientAddress(domain='more.mail.yandex.ru'), 'more.mail.Y'),
    (RecipientAddress(domain='more.mail.yandex.com.tr'), 'more.mail.Y'),
    (RecipientAddress(domain='mail.ru'), 'mail.ru'),
    (RecipientAddress(domain='yandex.me'), 'yandex.me')
])
def test_get_normalized_domain(email, domain_normalized):
    assert yasrv.get_normalized_domain(email) == domain_normalized


def test_is_yasrv_positive():
    assert yasrv.is_yasrv(RecipientAddress(domain='taxi.yandex.ru')) is True
    assert yasrv.is_yasrv(RecipientAddress(domain='taxi.yandex.com.tr')) is True


def test_is_yasrv_negative():
    assert yasrv.is_yasrv(RecipientAddress(domain='maps.google.com')) is False


def test_do():
    ctx = MockContext()
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    email = yasrv.do(Recipient(RecipientAddress(domain='taxi.yandex.ru'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'ya-taxi'
    assert email.ava.url_mobile == 'static_mobile_icon_link.ya-taxi.png'
    assert email.warn is False
    assert email.is_self is False


def test_do_with_extra_addr():
    ctx = MockContext()
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    email = yasrv.do(Recipient(RecipientAddress(local='disk-news', domain='yandex.ru'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'ya-disk'
    assert email.ava.url_mobile == 'static_mobile_icon_link.ya-disk.png'
    assert email.warn is False
    assert email.is_self is False


def test_do_with_noreply():
    ctx = MockContext()
    ctx.check_warn = True
    email = yasrv.do(Recipient(RecipientAddress(local='noreply', domain='taxi.yandex.ru'), ctx), ctx)
    assert email.warn is True
    assert email.is_self is False


def test_do_with_national_domain():
    ctx = MockContext()
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    ctx.check_warn = True
    email = yasrv.do(Recipient(RecipientAddress(local='taxi', domain='support.yandex.by'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'ya-taxi'
    assert email.ava.url_mobile == 'static_mobile_icon_link.ya-taxi.png'
    assert email.warn is False
    assert email.is_self is False


def test_do_after_reload():
    ctx = MockContext()
    s3 = MockS3()
    s3.add_dictionary('yandex_domains.json', map_of_yandex_services_icons_by_domain)
    s3.add_dictionary('yandex_addresses.json', map_of_yandex_services_icons_by_email)
    s3.add_key('yandex_domains.json', 'mock.Y', 'ya-mock')
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    yasrv.load(s3)
    email = yasrv.do(Recipient(RecipientAddress(domain='mock.yandex.ru'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'ya-mock'
    assert email.ava.url_mobile == 'static_mobile_icon_link.ya-mock.png'
    assert email.warn is False
    assert email.is_self is False

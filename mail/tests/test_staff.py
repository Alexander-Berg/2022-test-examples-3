from mail.catdog.catdog.src.parser import Recipient, RecipientAddress
from mail.catdog.catdog.src import staff
from mail.catdog.catdog.src import config
from mail.catdog.catdog.tests.mock_context import MockContext
from mail.catdog.catdog.tests.mock_s3 import MockS3
from mail.catdog.catdog.src.config import cfg
from mail.catdog.catdog.src.icon import IconTraits
from mail.catdog.catdog.src.data.ytsrv import map_of_yandex_team_services

import pytest


def test_is_yandex_team_positive():
    assert staff.is_yandex_team(RecipientAddress(domain='yandex-team.ru')) is True
    assert staff.is_yandex_team(RecipientAddress(domain='k50.ru')) is True
    assert staff.is_yandex_team(RecipientAddress(domain='openyard.ru')) is True


def test_is_yandex_team_negative():
    assert staff.is_yandex_team(RecipientAddress(domain='corp.mail.ru')) is False
    assert staff.is_yandex_team(RecipientAddress(local='corp', domain='yandex-team.ru')) is False


def test_is_yandex_team_srv():
    assert staff.is_yandex_team(RecipientAddress(local='noreply', domain='github.yandex-team.ru')) is True


@pytest.mark.parametrize(('login', 'normalized'), [
    ('abc', 'abc'),
    ('a-b-c', 'a-b-c'),
    ('a.b.c', 'a-b-c'),
    ('a-b.c', 'a-b-c')
])
def test_normalize(login, normalized):
    assert staff.normalize_staff_login(login) == normalized


def test_do():
    l = 'http://center-api.yandex-team.ru/api/v1/user/vo-lozh/avatar/200.jpg'
    l_ie = 'http://center-api.yandex-team.ru/api/v1/user/vo-lozh/avatar/50.jpg'
    ctx = MockContext()
    ctx.check_warn = True
    config.instance.staff_avatar_size = 200
    config.instance.staff_avatar_size_ie = 50
    email = staff.do(Recipient(RecipientAddress(local='vo.lozh', domain='yandex-team.ru'), ctx), ctx)
    assert email.ava.type == 'avatar'
    assert email.ava.url == l
    assert email.ava.url_small == l_ie
    assert email.ava.url_mobile == l
    assert email.warn is False
    assert email.is_self is False


def test_do_srv():
    ctx = MockContext()
    ctx.check_warn = True
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    email = staff.do(Recipient(RecipientAddress(local='help', domain='yandex-team.ru'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'ya-default'
    assert email.ava.url_mobile == 'static_mobile_icon_link.ya-default.png'
    assert email.warn is False
    assert email.is_self is False


def test_do_after_reload():
    ctx = MockContext()
    ctx.check_warn = True
    s3 = MockS3()
    s3.add_dictionary('yandex_team.json', map_of_yandex_team_services)
    s3.add_key('yandex_team.json', 'doctor.who.tardis@yandex-team.ru', 'ya-doctor.who.tardis')
    staff.load(s3)
    cfg().icon_traits = IconTraits('static_mobile_icon_link.{0}.png')
    email = staff.do(Recipient(RecipientAddress(local='doctor.who.tardis', domain='yandex-team.ru'), ctx), ctx)
    assert email.ava.type == 'icon'
    assert email.ava.name == 'ya-doctor.who.tardis'
    assert email.ava.url_mobile == 'static_mobile_icon_link.ya-doctor.who.tardis.png'
    assert email.warn is False
    assert email.is_self is False

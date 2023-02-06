import mail.catdog.catdog.src.organizations
import mail.catdog.catdog.src.staff
import mail.catdog.catdog.src.top200
import mail.catdog.catdog.src.yahoo
import mail.catdog.catdog.src.yandex
import mail.catdog.catdog.src.yasrv
from mail.catdog.catdog.src.parser import RecipientAddress
from mail.catdog.catdog.src import logic
from mail.catdog.catdog.src import mono
from mail.catdog.catdog.src import config
from mail.catdog.catdog.tests.mock_context import MockContext

import pytest


@pytest.mark.parametrize(('email', 'expected_type', 'ava_type'), [
    (RecipientAddress(domain='taxi.yandex.ru'), mail.catdog.catdog.src.yasrv.Yasrv, mono.type1),         # yasrv
    (RecipientAddress(domain='mvideo.ru'), mail.catdog.catdog.src.top200.Top200, mono.type1),            # top200
    (RecipientAddress(domain='yandex.ru'), mail.catdog.catdog.src.yandex.Yandex, mono.type2),            # yandex
    (RecipientAddress(domain='1c.ru'), mail.catdog.catdog.src.organizations.Organizations, mono.type1),  # org
    (RecipientAddress(domain='yahoo.com'), mail.catdog.catdog.src.yahoo.Yahoo, mono.type2),              # yahoo
    (RecipientAddress(domain='somedomain'), logic.Other, mono.type2)                                     # other
])
def test_choose_handlers_without_staff(email, expected_type, ava_type):
    handler_type = type(logic.choose_handlers(email, MockContext()))
    assert handler_type.ava == ava_type
    assert handler_type == expected_type


def test_choose_handlers_with_staff():
    config.instance.use_staff = True
    handler = logic.choose_handlers(RecipientAddress(domain='yandex-team.ru'), MockContext())
    assert type(handler).ava == mono.type2
    assert type(handler) == mail.catdog.catdog.src.staff.Staff

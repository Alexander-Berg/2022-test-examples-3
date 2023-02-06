import string
import random
import pytest
from hamcrest import (
    anything,
    assert_that,
    equal_to,
    has_entries,
)
from tests_common.mdb import user_connection
from mail.devpack.lib.components.fakebb import FakeBlackbox


def generate_login(length=8):
    return generate_name(length) + '@yandex.ru'


def generate_name(length=8):
    return ''.join(random.choice(string.ascii_letters) for _ in range(length))


def create_user(coordinator):
    response = coordinator.components[FakeBlackbox].register(generate_login())
    assert_that(response.status_code, equal_to(200), response.text)
    assert_that(response.json(), has_entries(uid=anything(), status='ok'), response.text)
    return int(response.json()['uid'])


@pytest.fixture(scope="function")
def newly_created_uid(context):
    return create_user(context.barbet)


def change_user_state(context, uid, state):
    with user_connection(context, uid) as _:
        context.maildb.execute('''
            UPDATE
                mail.users
            SET
                state = '{state}'
            WHERE
                uid = {uid}
        '''.format(uid=uid, state=state))

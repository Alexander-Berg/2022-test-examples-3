import pytest
import random
import string

from mail.mops.devpack.components.application import Mops
from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.devpack.tests.helpers.fixtures import coordinator_context
from pymdb.vegetarian import fill_messages_in_folder, SAMPLE_STIDS
from pymdb.queries import Queries
from yatest.common import source_path
from tests_common.mdb import user_connection
from tests_common.user import make_user_oneline
from tests_common.pytest_bdd import context as ctx
from tests_common.coordinator_context import (
    fill_coordinator_context,
)
from tests_common.holders import (
    UIDHolder,
    UIDRanges,
    UsersHolder,
)
from hamcrest import (
    assert_that,
    has_length,
    equal_to,
    has_entries,
    anything,
)


@pytest.fixture(scope="session")
def mops_coordinator():
    with coordinator_context(Mops) as coord:
        yield coord


@pytest.fixture(scope="session", autouse=True)
def context():
    return ctx


@pytest.fixture(scope="session", autouse=True)
def feature_setup(context, mops_coordinator):
    before_all(context, mops_coordinator)


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request = request


def before_all(context, mops_coordinator):
    fill_coordinator_context(context, mops_coordinator)
    context.mops = context.coordinator.components[Mops]
    context.get_free_uid = UIDHolder(
        UIDRanges.system,
        sharddb_conn=context.sharddb_conn,
    )
    context.users = UsersHolder()


def pytest_bdd_before_scenario(request, feature, scenario):
    ctx.params = {}
    ctx.set_example_params(scenario)


def pytest_bdd_after_scenario(request, feature, scenario):
    ctx.users.forget()


def get_path(resource):
    path = source_path(resource).split("/")
    path.pop()
    return "/".join(path)


def get_inbox_messages(context, count):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        folder = qs.folder_by_type('inbox')
        messages = qs.messages(fid=folder.fid)
        assert_that(messages, has_length(count))
        return messages


def make_new_user(context):
    make_user_oneline(context, empty=True)
    uid = context.user.uid
    context.params['uid'] = uid
    return uid


def fill_folder_with_messages(context, uid, folder_type, msg_count):
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        folder = qs.folder_by_type(folder_type)
        fill_messages_in_folder(conn, uid, folder, msg_count, SAMPLE_STIDS)
        return folder


def make_new_user_with_messages(context, msg_count):
    uid = make_new_user(context)
    folder = fill_folder_with_messages(context, uid, 'inbox', msg_count)
    return uid, folder.fid


def wait_for_completion_all_async_tasks(context, sec):
    import time

    no_active_tasks = lambda resp: response and response.status_code == 200 and response.text.strip() == '{"tasks":[]}'

    response = None
    mops_api = context.mops.api()
    for _ in range(0, sec):
        response = mops_api.stat(uid=context.user.uid)
        if no_active_tasks(response):
            return
        time.sleep(1)
    assert no_active_tasks(response)


def create_user(mops_coordinator):
    response = mops_coordinator.components[FakeBlackbox].register(generate_login())
    assert_that(response.status_code, equal_to(200), response.text)
    assert_that(response.json(), has_entries(uid=anything(), status='ok'), response.text)
    return int(response.json()['uid'])


def get_message_count_in_folder(context, fid):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        return len(qs.messages(fid=fid))


def get_mids_in_folder(context, fid):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        return [m['mid'] for m in qs.messages(fid=fid)]


def get_fid(context, folder_type):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        return qs.folder_by_type(folder_type).fid


def generate_login(length=8):
    return generate_name(length) + '@yandex.ru'


def generate_name(length=8):
    return ''.join(random.choice(string.ascii_letters) for _ in range(length))

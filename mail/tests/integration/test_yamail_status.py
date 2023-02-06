import pytest
import json

from pytest_bdd import scenarios
from .conftest import get_path

from yatest.common import network

from mail.devpack.lib.coordinator import Coordinator
from mail.devpack.tests.helpers.env import TestEnv
from mail.hound.devpack.components.application import Hound
from mail.devpack.lib.components.sharpei import Sharpei
from mail.devpack.lib.components.tvmapi import TvmApi


from library.python.testing.pyremock.lib.pyremock import (
    MockHttpServer,
    MatchRequest,
    MockResponse,
)

from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    has_entry,
)

from tests_common.pytest_bdd import (
    given,
    when,
    then,
)


scenarios(
    "yamail_status.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/yamail_status.feature"),
    strict_gherkin=False
)


@pytest.fixture(scope="module", autouse=True)
def local_hound_mock_sharpei_setup(request, context):
    port_manager = network.PortManager()
    context.local_coordinator = Coordinator(TestEnv(port_manager, Hound, 'devpack_yamail_status'), Hound)
    context.local_coordinator.start(TvmApi, with_deps=False)
    context.local_coordinator.start(Hound, with_deps=False)
    context.local_hound = context.local_coordinator.components[Hound]
    context.local_hound_started = True

    context.pyremock = MockHttpServer(context.local_coordinator.components[Sharpei].webserver_port())
    context.pyremock.start()
    context.pyremock_started = True

    def local_hound_mock_sharpei_teardown():
        context.pyremock.stop()
        context.pyremock_started = False

        context.local_coordinator.stop()
        context.local_hound_started = False

    request.addfinalizer(local_hound_mock_sharpei_teardown)


@given(u'mocked sharpei')
def step_given_mocked_sharpei(context):
    assert context.pyremock and context.pyremock_started, 'Pyremock should be started'
    context.pyremock.reset()


@given(u'local hound')
def step_given_local_hound(context):
    assert context.local_hound and context.local_hound_started, 'Local hound should be started'


@ given(u'fake uid')
def step_given_fake_uid(context):
    context.uid = 42


def expect_sharpei_response(context, status, body):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/conninfo'),
            params=has_entries(
                mode=equal_to(['all']),
                uid=equal_to([str(context.uid)]),
            ),
        ),
        response=MockResponse(
            status=status,
            body=body,
        ),
    )


SHARPEI_RESPONSE_TYPES = {
    'all alive': [('master', 'alive'), ('replica', 'alive'), ('replica', 'alive')],
    'master dead': [('master', 'dead'), ('replica', 'alive'), ('replica', 'alive')],
    'master and replica dead': [('master', 'dead'), ('replica', 'dead'), ('replica', 'alive')],
    'replica dead': [('master', 'alive'), ('replica', 'dead'), ('replica', 'alive')],
    'both replicas dead': [('master', 'alive'), ('replica', 'dead'), ('replica', 'dead')],
    'all dead': [('master', 'dead'), ('replica', 'dead'), ('replica', 'dead')],
}


def make_sharpei_response(shard, resp_type):
    resp = {
        'id': 666,
        'name': shard,
        'databases': [
            {
                'address': {'host': 'h', 'port': 1, 'dbname': 'd', 'dataCenter': 'dc'},
                'state': {'lag': 0},
                'role': db[0],
                'status': db[1],
            } for db in SHARPEI_RESPONSE_TYPES[resp_type]
        ]
    }

    return json.dumps(resp)


@given(u'mocked sharpei respond with status "{status:d}"')
def step_given_sharpei_response_by_status(context, status):
    expect_sharpei_response(context, status=status, body='')


@given(u'mocked sharpei respond with "{shard}" shard and "{resp_type}" in response')
def step_given_sharpei_response(context, shard, resp_type):
    expect_sharpei_response(context, status=200, body=make_sharpei_response(shard, resp_type))


@when(u'we request "yamail_status" from local hound')
def step_request_hound(context):
    context.response = context.local_hound.request_get('v2/yamail_status', uid=context.uid)


@then(u'state for "{shard}" is "{state}"')
def check_status_response(context, shard, state):
    resp = context.response.json()
    assert_that(resp, has_entry(
        'database_info', has_entries(
            name=equal_to(shard),
            state=equal_to(state),
        )
    ))

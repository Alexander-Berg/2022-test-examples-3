import pytest
import sys

from alice.megamind.protos.scenarios.request_pb2 import TScenarioApplyRequest, TScenarioRunRequest
from alice.megamind.protos.scenarios.response_pb2 import TScenarioCommitResponse, TScenarioRunResponse
from mail.ciao.ciao.tests.common_conftest import *  # noqa
from mail.ciao.ciao.tests.interactions import *  # noqa
from mail.ciao.protos.arguments_pb2 import TArguments
from mail.ciao.protos.state_pb2 import TState


@pytest.fixture
def app_id(rands, settings):
    app_id = rands()
    settings.ALLOWED_APP_IDS = (app_id,)
    return app_id


@pytest.fixture
def uuid(rands):
    return rands()


@pytest.fixture
def run_scenario_request(app, user, app_id, uuid):
    async def _inner(state, frame):
        proto = TScenarioRunRequest()
        proto.BaseRequest.ClientInfo.Timezone = str(user.timezone)
        proto.BaseRequest.ClientInfo.AppId = app_id
        proto.BaseRequest.ClientInfo.Uuid = uuid
        if state is not None:
            proto.BaseRequest.State.Pack(state)
        proto.Input.SemanticFrames.extend([frame])

        response = await app.post(
            '/megamind/run',
            headers={
                'Content-Type': 'application/protobuf',
                'X-Ya-Service-Ticket': 'dbg',
                'X-Ya-User-Ticket': user.user_ticket,
            },
            data=proto.SerializeToString(),
        )
        assert response.status == 200
        response_data = await response.read()
        response_proto = TScenarioRunResponse()
        response_proto.ParseFromString(response_data)
        return response_proto

    return _inner


@pytest.fixture
def commit_scenario_request(app, user, app_id, uuid):
    async def _inner(state, arguments):
        proto = TScenarioApplyRequest()
        proto.BaseRequest.ClientInfo.Timezone = str(user.timezone)
        proto.BaseRequest.ClientInfo.AppId = app_id
        proto.BaseRequest.ClientInfo.Uuid = uuid
        if state is not None:
            proto.BaseRequest.State.Pack(state)
        if arguments is not None:
            proto.Arguments.Pack(arguments)

        response = await app.post(
            '/megamind/commit',
            headers={
                'Content-Type': 'application/protobuf',
                'X-Ya-Service-Ticket': 'dbg',
                'X-Ya-User-Ticket': user.user_ticket,
            },
            data=proto.SerializeToString(),
        )
        assert response.status == 200
        response_data = await response.read()
        response_proto = TScenarioCommitResponse()
        response_proto.ParseFromString(response_data)
        return response_proto

    return _inner


@pytest.fixture
def run_scenario(app, run_scenario_request, commit_scenario_request):
    async def _inner(frames, verbose=False):
        state = None
        for frame in frames:
            response_proto = await run_scenario_request(state, frame)
            if verbose:
                print(file=sys.stderr)
                print(response_proto, file=sys.stderr)
            if response_proto.HasField('CommitCandidate'):
                arguments = TArguments()
                response_proto.CommitCandidate.Arguments.Unpack(arguments)
                commit_response = await commit_scenario_request(state, arguments)
                if commit_response.HasField('Error'):
                    return commit_response
            state = TState()
            response_proto.ResponseBody.State.Unpack(state)
        return response_proto

    return _inner

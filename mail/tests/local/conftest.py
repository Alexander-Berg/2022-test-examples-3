import os
import sys
import logging
import pytest

from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.devpack.lib.components.pyremock import PyremockComponent
from mail.devpack.tests.helpers.fixtures import coordinator_context

from mail.furita.devpack.components.furita import FuritaComponent as Furita
from mail.pg.furitadb.devpack.components.furitadb import FuritaDb
from mail.hound.devpack.components.application import Hound
from mail.mdbsave.devpack.components.mdbsave import MdbSaveComponent as MdbSave
from mail.notsolitesrv.devpack.components.nsls import NslsLocalComponent
from mail.notsolitesrv.devpack.components.relay import RelayComponent

from library.python.testing.pyremock.lib.pyremock import (
    MatchRequest,
    MockHttpServer,
    MockResponse,
    HttpMethod
)
from hamcrest import is_, contains_string
from client_api_wrapper import ClientApiWrapper


@pytest.fixture(scope='session', autouse=True)
def context():
    with coordinator_context(NslsLocalComponent, devpack_root=os.environ.get('NSLS_DEVPACK_ROOT')) as coord:
        coord.request_id = 'nsls_local_tests'
        coord.log = logging.getLogger('nsls_local_tests')

        coord.fbb = coord.components[FakeBlackbox]
        coord.furita = coord.components[Furita]
        coord.furitadb = coord.components[FuritaDb]
        coord.hound = coord.components[Hound]
        coord.mdbsave = coord.components[MdbSave]
        coord.nsls = coord.components[NslsLocalComponent]
        coord.pyremock = coord.components[PyremockComponent]
        coord.relay = coord.components[RelayComponent]

        coord.hound_api = ClientApiWrapper(port=coord.hound.webserver_port())
        coord.furita_api = ClientApiWrapper(port=coord.furita.webserver_port())

        stderr_handler = logging.StreamHandler(sys.stderr)
        stderr_handler.setFormatter(logging.Formatter('[%(asctime)s] %(message)s'))
        coord.log.addHandler(stderr_handler)
        coord.log.setLevel(logging.DEBUG)

        yield coord


@pytest.fixture(scope='session', autouse=True)
def so_check_form_mock(request, context):
    def teardown():
        if context.so_check_form_mock:
            context.so_check_form_mock.stop()
            context.so_check_form_mock = None

    context.so_check_form_mock = MockHttpServer(context.furita.so_check_form_port)
    context.so_check_form_mock.start()

    http_request = MatchRequest(method=is_(HttpMethod.POST), path=contains_string('/check-json'))
    mock_response = MockResponse(status=200, body='<spam>0</spam>')
    context.so_check_form_mock.expect(http_request, mock_response, times=9999)
    request.addfinalizer(teardown)


@pytest.fixture(scope='function', autouse=True)
def reset_pyremock(context):
    context.pyremock.reset()

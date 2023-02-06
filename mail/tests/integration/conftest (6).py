import logging
import pytest
import sys
import os

from mail.devpack.lib.components.pyremock import PyremockComponent
from mail.devpack.tests.helpers.fixtures import coordinator_context
from mail.notsolitesrv.devpack.components.nsls import NslsComponent
from mail.notsolitesrv.devpack.components.relay import RelayComponent
from mail.notsolitesrv.devpack.components.root import NslsService


@pytest.fixture(scope='session', autouse=True)
def context():
    with coordinator_context(NslsService, devpack_root=os.environ.get('NSLS_DEVPACK_ROOT')) as coord:
        coord.request_id = 'nsls_tests'
        coord.log = logging.getLogger('nsls_integration_tests')

        coord.relay = coord.components[RelayComponent]
        coord.nsls = coord.components[NslsComponent]
        coord.pyremock = coord.components[PyremockComponent]

        stderr_handler = logging.StreamHandler(sys.stderr)
        stderr_handler.setFormatter(logging.Formatter('[%(asctime)s] %(message)s'))
        coord.log.addHandler(stderr_handler)
        coord.log.setLevel(logging.DEBUG)
        yield coord


@pytest.fixture(scope='function', autouse=True)
def reset_pyremock(context):
    context.pyremock.reset()

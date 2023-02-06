import pytest
import furita_helpers as helpers

from library.python.testing.pyremock.lib.pyremock import (
    MockHttpServer,
)
from furita_api import FuritaApi
from furita import (
    FuritaRequests,
    get_furita,
    get_default_params
)
from library.python.testing.pyremock.lib.pyremock import (
    MatchRequest,
    MockResponse,
    HttpMethod
)
from hamcrest import is_, contains_string


@pytest.fixture(scope="session", autouse=True)
def context():
    """ Тут мы будем хранить контекст всех тестов """
    class Context(object):

        def __init__(self):
            self.users = {}
            self.furita_api = None
            self.furita = None
            self.devpack = {}
            self.invalid_users = {}

        def generate_unused_uid(self):
            for invalid_uid in range(1111, 2111):
                if invalid_uid not in list(self.users.values()) + list(self.invalid_users.values()):
                    return invalid_uid

        def create_user(self, name):
            assert name not in self.users
            if name.startswith('InvalidUser'):
                self.invalid_users[name] = self.generate_unused_uid()
                return self.invalid_users[name]
            else:
                uid = helpers.create_user(name, self.devpack)
                assert uid
                if uid in self.invalid_users.values():
                    kv = list(filter(lambda x: x[1] == uid, self.invalid_users.items()))[0]
                    self.invalid_users[kv[0]] = self.generate_unused_uid()
                self.users[name] = uid
                return uid

        def get_uid(self, name):
            if name in self.users:
                return self.users[name]
            elif name in self.invalid_users:
                return self.invalid_users[name]
            return None

    return Context()


@pytest.fixture(scope="session", autouse=True)
def environment_setup(request, context):
    """ Эта хрень нужна для того, чтобы поднимать окружение до тестов и гасить после """
    context.devpack = helpers.devpack_start()

    def environment_teardown():
        helpers.devpack_stop(context.devpack)

    request.addfinalizer(environment_teardown)


@pytest.fixture(scope="module")
def params(request, context):
    yield get_default_params(context.devpack)


@pytest.fixture(scope="module", autouse=True)
def furita_setup(request, context, params):
    def furita_teardown():
        if context.furita:
            context.furita.stop()
            context.furita = None
            context.furita_api = None

    context.furita = get_furita(params)
    context.furita_api = FuritaApi(FuritaRequests(context.furita.furita_host))

    context.furita.search_port = params["__SEARCH_LOCAL_PORT__"]
    context.furita.mops_port = params["__MOPS_LOCAL_PORT__"]
    context.furita_db_port = params["__FURITA_DB_PORT__"]
    context.furita.smtp_port = params["__SMTP_LOCAL_PORT__"]
    request.addfinalizer(furita_teardown)


@pytest.fixture(scope="module", autouse=True)
def so_check_form_mock(request, context, params):
    def teardown():
        if context.so_check_form_mock:
            context.so_check_form_mock.stop()
            context.so_check_form_mock = None

    context.so_check_form_mock = MockHttpServer(params['__SO_CHECK_FORM_LOCAL_PORT__'])
    context.so_check_form_mock.start()

    http_req = MatchRequest(method=is_(HttpMethod.POST), path=contains_string('/check-json'))
    mock_response = MockResponse(status=200, body='<spam>0</spam>')
    context.so_check_form_mock.expect(http_req, mock_response, times=9999)
    request.addfinalizer(teardown)


@pytest.fixture(scope="module", autouse=True)
def tupita_rules_mock(request, context, params):
    def teardown():
        if context.tupita_rules_mock:
            context.tupita_rules_mock.stop()
            context.tupita_rules_mock = None

    context.tupita_rules_mock = MockHttpServer(params['__TUPITA_LOCAL_PORT__'])
    context.tupita_rules_mock.start()
    request.addfinalizer(teardown)


@pytest.fixture(scope="module", autouse=True)
def blackbox_mock(request, context, params):
    def teardown():
        if context.blackbox_mock:
            context.blackbox_mock.stop()
            context.blackbox_mock = None

    context.blackbox_mock = MockHttpServer(params['__BLACKBOX_LOCAL_PORT__'])
    context.blackbox_mock.start()
    request.addfinalizer(teardown)

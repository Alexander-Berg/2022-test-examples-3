import pytest
from hamcrest import assert_that, equal_to, has_property
from helpers.api import call_internal_api, InternalApiException
from webserver import WebServer

FAKE_INTERNAL_API_PORT = 10080


@pytest.fixture(scope="module")
def fake_internal_api(request):
    internal_api = WebServer(FAKE_INTERNAL_API_PORT)
    internal_api.start()

    def fin():
        internal_api.shutdown()

    request.addfinalizer(fin)
    return internal_api


@pytest.fixture()
def internal_api(fake_internal_api):
    fake_internal_api.queries = []
    return fake_internal_api


def operations():
    return [
        {"path": "/folders", "args": {}},
        {"path": "/labels", "args": {}},
        {"path": "/next_message_chunk", "args": {"mid": "0", "count": "2"}},
    ]


@pytest.fixture(params=operations())
def operation(request, dst_user):
    request.param["args"]["uid"] = str(dst_user["uid"])
    return request.param


def test_internal_not_acquired_operations(
    internal_api, operation, collectors_internal_url, service_ticket, tvm_client
):
    with pytest.raises(InternalApiException) as e:
        call_internal_api(
            collectors_internal_url + operation["path"], operation["args"], service_ticket
        )
    assert_that(e.value, has_property("code", equal_to(410)))


def test_internal_with_bad_service_ticket(internal_api, operation, collectors_internal_url):
    with pytest.raises(InternalApiException) as excinfo:
        operation["args"]["uid"] = "0"
        bad_service_ticket = "BAD_SERVICE_TICKET"
        call_internal_api(
            collectors_internal_url + operation["path"], operation["args"], bad_service_ticket
        )
    assert_that(len(internal_api.queries), equal_to(0))
    e = excinfo.value
    assert_that(e.code, equal_to(401))


def test_internal_without_service_ticket(internal_api, operation, collectors_internal_url):
    with pytest.raises(InternalApiException) as excinfo:
        call_internal_api(collectors_internal_url + operation["path"], operation["args"])
    assert_that(len(internal_api.queries), equal_to(0))
    e = excinfo.value
    assert_that(e.code, equal_to(401))
    assert_that(e.content, equal_to("no_auth_token"))


def test_internal_with_invalid_uid(
    internal_api, operation, collectors_internal_url, service_ticket
):
    with pytest.raises(InternalApiException) as excinfo:
        operation["args"]["uid"] = "0"
        call_internal_api(
            collectors_internal_url + operation["path"], operation["args"], service_ticket
        )
    assert_that(len(internal_api.queries), equal_to(0))
    e = excinfo.value
    assert_that(e.code, equal_to(500))
    assert_that(e.content, equal_to("user not found"))

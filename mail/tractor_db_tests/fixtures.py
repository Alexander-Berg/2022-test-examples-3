from tractor.models import ExternalProvider
from _pytest.fixtures import SubRequest
import pytest
import uuid
from typing import Any


CONNINFO = "host=localhost port=5432 user=tractor_disk dbname=postgres"

ORG_ID = "test_org_id"
NONEXISTENT_ORG_ID = "nonexistent_" + ORG_ID
DOMAIN = "test_domain"
DOMAIN_2 = DOMAIN + "_2"
PROVIDER = ExternalProvider.GOOGLE
PROVIDER_2 = ExternalProvider.MICROSOFT
ENCRYPTED_SECRET = b"7E575EC"
ENCRYPTED_SECRET_2 = ENCRYPTED_SECRET + b"2"
WORKER_ID = "test_worker_id"


@pytest.fixture
def org_id(request: pytest.FixtureRequest):
    from types import FunctionType

    assert isinstance(request.function, FunctionType)
    return ORG_ID + "_" + request.function.__name__ + "_" + str(uuid.uuid4())[:8]


@pytest.fixture
def org_id_2(org_id: str):
    return org_id + "_2"


@pytest.fixture
def login():
    return "test_login" + str(uuid.uuid4())[:8]


@pytest.fixture
def login_2(login: str):
    return login + "_2"


def get_fixture_param(request: pytest.FixtureRequest) -> Any:
    assert isinstance(request, SubRequest), type(request)
    return request.param

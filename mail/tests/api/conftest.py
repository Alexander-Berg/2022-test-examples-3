import json
import urllib.request, urllib.parse, urllib.error

from django.conf import settings

import pytest
from rest_framework.test import APIClient

from fan_ui.api.authentication import TVMAuthentication
from fan.tests.conftest import *


@pytest.fixture
def api_client():
    return APIClient()


@pytest.fixture
def auth_api_client(auth_session, api_client):
    """
    API client with session (cookie) authentication credentials.
    """
    cookie_data = {
        "max-age": None,
        "path": "/",
        "domain": settings.SESSION_COOKIE_DOMAIN,
        "secure": settings.SESSION_COOKIE_SECURE or None,
        "expires": None,
    }

    session_cookie = settings.SESSION_COOKIE_NAME
    api_client.cookies[session_cookie] = auth_session.session_key
    api_client.cookies[session_cookie].update(cookie_data)

    return api_client


@pytest.fixture
def disable_auth_on_loopback():
    """
    This fixture temporarily allows API calls without any authentication
    for loopback (localhost) requests.
    """
    original_value = settings.REQUIRE_AUTH_ON_LOOPBACK
    settings.REQUIRE_AUTH_ON_LOOPBACK = False
    yield
    settings.REQUIRE_AUTH_ON_LOOPBACK = original_value


@pytest.fixture
def tvm_api_client(auth_session, api_client):
    """
    API client with TVM authentication credentials.
    """
    api_client.credentials(HTTP_X_YA_SERVICE_TICKET=settings.TVM_UNITTEST_TICKET)
    return api_client


@pytest.fixture
def tvm_api_client_with_damaged_ticket(auth_session, api_client):
    """
    API client with damaged TVM authentication credentials.
    """
    DAMAGED_TVM_TICKET = "THIS_IS_DAMAGED_TVM_TICKET_FOR_TESTING"
    api_client.credentials(HTTP_X_YA_SERVICE_TICKET=DAMAGED_TVM_TICKET)
    return api_client


@pytest.fixture
def tvm_clear_api_v1_allowed_services_list():
    """
    This fixture is used to temporary clear settings.API_V1_TVM_ALLOWED_SOURCE_IDS.
    """
    original_value = settings.API_V1_TVM_ALLOWED_SOURCE_IDS
    settings.API_V1_TVM_ALLOWED_SOURCE_IDS = tuple()
    yield
    settings.API_V1_TVM_ALLOWED_SOURCE_IDS = original_value


@pytest.fixture
def tvm_clear_api_organization_allowed_services_list():
    original_value = settings.API_ORGANIZATION_TVM_ALLOWED_SOURCE_IDS
    settings.API_ORGANIZATION_TVM_ALLOWED_SOURCE_IDS = tuple()
    yield
    settings.API_ORGANIZATION_TVM_ALLOWED_SOURCE_IDS = original_value


@pytest.fixture
def make_request():
    def _make_request(client, url, form_data=None, json_data=None, meta=None):
        post_kwargs = {"path": url}

        if form_data:
            post_kwargs["data"] = urllib.parse.urlencode(form_data)
            post_kwargs["content_type"] = "application/x-www-form-urlencoded"
        else:
            post_kwargs["data"] = json.dumps(json_data or {})
            post_kwargs["content_type"] = "application/json"

        post_kwargs.update(meta or {})

        return client.post(**post_kwargs)

    return _make_request

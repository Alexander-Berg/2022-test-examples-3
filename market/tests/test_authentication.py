# -*- coding: utf-8 -*-
import pytest
from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.authentication import Authentication
from market.idx.api.backend.marketindexer.storage.storage import Storage
from hamcrest import assert_that
from utils import (
    is_success_response,
    is_error_response
)
import tvmauth

SERVICE_TICKET_HEADER = 'X-Ya-Service-Ticket'

# https://a.yandex-team.ru/arc_vcs/library/recipes/tvmapi
IDXAPI_TVM_ID = 1000502
OTHER_SERVICE_TVM_ID = 1000501


TVMAPI_PORT_FILE = "tvmapi.port"


def _get_tvmapi_port():
    with open(TVMAPI_PORT_FILE) as f:
        return int(f.read())


@pytest.fixture(scope='module')
def tvm_client_external():
    cs = tvmauth.TvmApiClientSettings(
        self_tvm_id=1000501,
        self_secret='bAicxJVa5uVY7MjDlapthw',
        dsts={'Idxapi backend': 1000502,
              'Blackmagic backend': 1000505},
        enable_service_ticket_checking=False,
        localhost_port=_get_tvmapi_port()
    )

    client = tvmauth.TvmClient(cs)
    assert client.status == tvmauth.TvmClientStatus.Ok

    yield client


@pytest.fixture(scope='module')
def tvm_ticket_to_idxapi(tvm_client_external):
    yield tvm_client_external.get_service_ticket_for(tvm_id=IDXAPI_TVM_ID)


@pytest.fixture(scope='module')
def tvm_ticket_to_other_service(tvm_client_external):
    yield tvm_client_external.get_service_ticket_for(tvm_id=1000505)


@pytest.fixture(scope='module')
def tvm_client_idxapi():
    cs = tvmauth.TvmApiClientSettings(
        self_tvm_id=IDXAPI_TVM_ID,
        self_secret='e5kL0vM3nP-nPf-388Hi6Q',
        dsts={'Some other backend': 1000503},
        enable_service_ticket_checking=True,
        localhost_port=_get_tvmapi_port()
    )

    client = tvmauth.TvmClient(cs)
    assert client.status == tvmauth.TvmClientStatus.Ok

    yield client


@pytest.fixture(scope="module")
def app_check_tvm_enabled(tvm_client_idxapi):
    app = create_flask_app(Storage(), tvm_client=tvm_client_idxapi)
    Authentication(app, tvm_client_idxapi, enable_check_tvm=True, enable_abort_on_tvm_check_fail=True)
    yield app


@pytest.fixture(scope="module")
def app_abort_on_tvm_check_fail_disabled(tvm_client_idxapi):
    app = create_flask_app(Storage(), tvm_client=tvm_client_idxapi)
    Authentication(app, tvm_client_idxapi, enable_check_tvm=True, enable_abort_on_tvm_check_fail=False)
    yield app


@pytest.fixture(scope="module")
def app_check_disabled(tvm_client_idxapi):
    app = create_flask_app(Storage(), tvm_client=tvm_client_idxapi)
    Authentication(app, tvm_client_idxapi, enable_check_tvm=False)
    yield app


def test_tvm_check_successfull(app_check_tvm_enabled, tvm_ticket_to_idxapi):
    with app_check_tvm_enabled.test_client() as client:
        resp = client.get('/admin/bin_version', headers={SERVICE_TICKET_HEADER: tvm_ticket_to_idxapi})
        assert_that(resp, is_success_response())


def test_tvm_check_wrong_destination_401(app_check_tvm_enabled, tvm_ticket_to_other_service):
    with app_check_tvm_enabled.test_client() as client:
        resp = client.get('/admin/bin_version', headers={SERVICE_TICKET_HEADER: tvm_ticket_to_other_service})
        assert_that(resp, is_error_response(code=401))


def test_tvm_check_no_token_401(app_check_tvm_enabled):
    with app_check_tvm_enabled.test_client() as client:
        resp = client.get('/admin/bin_version')
        assert_that(resp, is_error_response(code=401))


def test_tvm_check_wrong_destination_abort_disabled_200(app_abort_on_tvm_check_fail_disabled, tvm_ticket_to_other_service):
    with app_abort_on_tvm_check_fail_disabled.test_client() as client:
        resp = client.get('/admin/bin_version', headers={SERVICE_TICKET_HEADER: tvm_ticket_to_other_service})
        assert_that(resp, is_success_response())


def test_tvm_check_no_token_abort_disabled_200(app_abort_on_tvm_check_fail_disabled):
    with app_abort_on_tvm_check_fail_disabled.test_client() as client:
        resp = client.get('/admin/bin_version')
        assert_that(resp, is_success_response())


def test_tvm_check_no_token_check_disabled_200(app_check_disabled):
    with app_check_disabled.test_client() as client:
        resp = client.get('/admin/bin_version')
        assert_that(resp, is_success_response())

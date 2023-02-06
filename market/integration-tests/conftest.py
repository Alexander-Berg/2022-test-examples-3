from __future__ import unicode_literals, absolute_import

import os
from typing import Dict  # noqa

import pytest
import six.moves.urllib.parse as urlparse
from infra.nanny.nanny_services_rest.nanny_services_rest.client import ServiceRepoClient
from infra.nanny.yp_lite_api.py_stubs.pod_sets_api_stub import YpLiteUIPodSetsServiceStub
from infra.nanny.yp_lite_api.py_stubs.pod_reallocation_api_stub import YpLiteReallocationServiceStub
from infra.nanny.yp_lite_api.py_stubs.endpoint_sets_api_stub import YpLiteUIEndpointSetsServiceStub
from library.python.vault_client.instances import Testing as VaultClient
from nanny_repo.repo_api_stub import RepoServiceStub
from nanny_rpc_client import RetryingRpcClient
from sandbox.common.proxy import OAuth
from sandbox.common.rest import Client as SandboxClient

from market.sre.tools.rtc.nanny.manager import ServiceRepoManager
from market.sre.tools.rtc.nanny.models.service import Service
from market.sre.tools.rtc.nanny.sandbox_proxy import SandboxProxyClient


@pytest.fixture("module")
def real_nanny_url():  # type: () -> unicode
    """A real Nanny url. It must be used for functional tests only."""
    return "https://nanny.yandex-team.ru"


@pytest.fixture("module")
def real_nanny_token():  # type: () -> unicode
    """A real oauth token. It must be used for functional tests only."""
    return os.getenv("NANNY_TOKEN_FOR_TESTS")


@pytest.fixture("module")
def real_sandbox_token():  # type: () -> unicode
    """A real oauth token. It must be used for functional tests only."""
    return os.getenv("SANDBOX_TOKEN_FOR_TESTS")


@pytest.fixture(scope="module")
def real_login():  # type: () -> unicode
    """A real user login. It must be used for functional tests only."""
    return os.getenv("USER")


@pytest.fixture
def real_nanny_service_id():  # type: () -> unicode
    """A real Nanny service. It must be used for functional tests only."""
    return "testing_market_template_service_for_java_iva"


@pytest.fixture(scope="module")
def real_new_category():  # type: () -> unicode
    """A real Nanny category. It must be used for functional tests only."""
    return "/junk/functional-tests"


@pytest.fixture(scope="module")
def real_new_nanny_service_id(real_login):  # type: (unicode) -> unicode
    """A real Nanny service. It must be used for functional tests only."""
    return "test_service_{}".format(real_login)


@pytest.fixture("module")
def real_rest_client(
    real_nanny_url, real_nanny_token
):  # type: (unicode, unicode) -> ServiceRepoClient
    """A real rest_client. It must be used for functional tests only."""
    return ServiceRepoClient(real_nanny_url, real_nanny_token)


@pytest.fixture("module")
def real_repo_stub(
    real_nanny_url, real_nanny_token
):  # type: (unicode, unicode) -> RepoServiceStub
    """A real repo_stub. It must be used for functional tests only."""
    rpc_client = RetryingRpcClient(
        urlparse.urljoin(real_nanny_url, "/api/repo"), real_nanny_token
    )
    return RepoServiceStub(rpc_client)


@pytest.fixture("module")
def real_yp_lite_stub(
    real_nanny_token
):  # type: (unicode) -> YpLiteUIPodSetsServiceStub
    """A real repo_stub. It must be used for functional tests only."""
    yp_lite_rpc_client = RetryingRpcClient("https://yp-lite-ui.nanny.yandex-team.ru/api/yplite/pod-sets/",
                                           real_nanny_token)
    return YpLiteUIPodSetsServiceStub(yp_lite_rpc_client)


@pytest.fixture("module")
def real_yp_lite_reallocation_stub(
    real_nanny_token
):  # type: (unicode) -> YpLiteReallocationServiceStub
    """A real repo_stub. It must be used for functional tests only."""
    yp_lite_reallocation_rpc_client = RetryingRpcClient(
        "https://yp-lite-ui.nanny.yandex-team.ru/api/yplite/pod-reallocation/",
        real_nanny_token
    )
    return YpLiteReallocationServiceStub(yp_lite_reallocation_rpc_client)


@pytest.fixture("module")
def real_yp_lite_endpoint_sets_stub(
    real_nanny_token
):  # type: (unicode) -> YpLiteUIEndpointSetsServiceStub
    """A real repo_stub. It must be used for functional tests only."""
    yp_lite_endpoint_sets_rpc_client = RetryingRpcClient(
        "https://yp-lite-ui.nanny.yandex-team.ru/api/yplite/endpoint-sets/",
         real_nanny_token
    )
    return YpLiteUIEndpointSetsServiceStub(yp_lite_endpoint_sets_rpc_client)


@pytest.fixture("module")
def real_sandbox_client(real_sandbox_token):  # type: (unicode) -> SandboxClient
    """A real sandbox_client. It must be used for functional tests only."""
    return SandboxClient(auth=OAuth(real_sandbox_token))


@pytest.fixture("module")
def real_vault_client(real_nanny_token):  # type: (unicode) -> VaultClient
    """A real sandbox_client. It must be used for functional tests only."""
    return VaultClient(authorization=real_nanny_token, decode_files=True)


@pytest.fixture("module")
def real_sandbox_proxy(real_sandbox_token):  # type: (unicode) -> SandboxProxyClient
    """A real sandbox_client. It must be used for functional tests only."""
    return SandboxProxyClient(real_sandbox_token)


@pytest.fixture("module")
def real_manager(
    real_rest_client,  # type: ServiceRepoClient
    real_repo_stub,  # type: RepoServiceStub
    real_yp_lite_stub,  # type: RepoServiceStub
    real_yp_lite_reallocation_stub,  # type: YpLiteReallocationServiceStub
    real_yp_lite_endpoint_sets_stub,  # type: YpLiteUIEndpointSetsServiceStub
    real_sandbox_client,  # type: SandboxClient
    real_vault_client,  # type: SandboxProxyClient,
    real_sandbox_proxy,  # type: VaultClient
):  # type: (...) -> ServiceRepoManager
    """A real ServiceRepoManager. It must be used for functional tests only."""
    return ServiceRepoManager(
        nanny_rest_client=real_rest_client,
        nanny_repo_stub=real_repo_stub,
        yp_lite_stub=real_yp_lite_stub,
        sandbox_rest_client=real_sandbox_client,
        vault_client=real_vault_client,
        sandbox_proxy=real_sandbox_proxy,
    )


@pytest.fixture(scope="module")
def real_new_nanny_service(
    nanny_service_dict,  # type: Dict
    real_new_nanny_service_id,  # type: unicode
    real_new_category,  # type: unicode
    real_manager,  # type: ServiceRepoManager
):  # type: (...) -> Service
    """A real Nanny service. It must be used for functional tests only."""
    service = Service.from_dict(nanny_service_dict)
    service.id = "{}_for_attributes".format(real_new_nanny_service_id)
    service.info_attrs.category = real_new_category
    service.auth_attrs.owners.logins = []
    service.auth_attrs.owners.groups = []
    yield real_manager.create_service(service, "create service for functional tests")
    real_manager.delete_service(service)

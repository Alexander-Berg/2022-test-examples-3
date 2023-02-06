from __future__ import unicode_literals, absolute_import

import json
import os
from typing import Dict, Text  # noqa

import pytest
import six.moves.urllib.parse as urlparse
from _pytest.monkeypatch import MonkeyPatch  # noqa
from infra.nanny.nanny_services_rest.nanny_services_rest.client import ServiceRepoClient
from infra.nanny.yp_lite_api.py_stubs.endpoint_sets_api_stub import YpLiteUIEndpointSetsServiceStub
from infra.nanny.yp_lite_api.py_stubs.pod_reallocation_api_stub import YpLiteReallocationServiceStub
from infra.nanny.yp_lite_api.py_stubs.pod_sets_api_stub import YpLiteUIPodSetsServiceStub
from library.python.vault_client.instances import Testing as VaultClient
from nanny_repo.repo_api_stub import RepoServiceStub
from nanny_rpc_client import RetryingRpcClient
from sandbox.common.proxy import NoAuth
from sandbox.common.rest import Client as SandboxClient

from market.sre.tools.rtc.nanny.manager import ServiceRepoManager
from market.sre.tools.rtc.nanny.models.attributes.auth_attrs import AuthAttrs
from market.sre.tools.rtc.nanny.models.attributes.current_state import CurrentState
from market.sre.tools.rtc.nanny.models.attributes.info_attrs import InfoAttrs
from market.sre.tools.rtc.nanny.models.attributes.runtime_attrs import RuntimeAttrs
from market.sre.tools.rtc.nanny.models.dashboard import Dashboard
from market.sre.tools.rtc.nanny.models.service import Service
from market.sre.tools.rtc.nanny.sandbox_proxy import SandboxProxyClient
from market.sre.tools.rtc.nanny.scenarios.clone_service import CloneService
from market.sre.tools.rtc.nanny.scenarios.common_service import CommonService
from market.sre.tools.rtc.nanny.scenarios.java_service import JavaService
from market.sre.tools.rtc.nanny.scenarios.nodejs_service import NodejsService

# support pycharm run
try:
    import yatest.common
except ImportError:
    yatest = None


@pytest.fixture
def rest_client(
    monkeypatch, nanny_service_dict
):  # type: (MonkeyPatch, Dict) -> ServiceRepoClient
    # mock any request
    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient._request",
        lambda _, a, b, c, d: {},
    )
    # mock get_service()
    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.get_service",
        lambda _, x: nanny_service_dict,
    )
    return ServiceRepoClient("https://localhost", "token")


@pytest.fixture
def repo_stub():  # type: () -> RepoServiceStub
    rpc_client = RetryingRpcClient(
        urlparse.urljoin("https://localhost", "/api/repo"), "token"
    )
    return RepoServiceStub(rpc_client)


@pytest.fixture
def yp_lite_stub(monkeypatch, pods_groups_dict):  # type: (MonkeyPatch, Dict) -> YpLiteUIPodSetsServiceStub
    yp_lite_rpc_client = RetryingRpcClient("https://localhost/api/yplite/pod-sets/", "token")

    return YpLiteUIPodSetsServiceStub(yp_lite_rpc_client)


@pytest.fixture
def yp_lite_reallocation_stub(monkeypatch,
                              pods_groups_dict):  # type: (MonkeyPatch, Dict) -> YpLiteReallocationServiceStub
    yp_lite_reallocation_rpc_client = RetryingRpcClient("https://localhost/api/yplite/pod-reallocation/", "token")

    # monkeypatch.setattr(
    #    "infra.nanny.yp_lite_api.py_stubs.pod_sets_api_stub.YpLiteUIPodSetsServiceStub.list_pods_groups",
    #    lambda _, __: json_format.ParseDict(pods_groups_dict, pod_sets_api_pb2.ListPodsGroupsResponse()),
    # )
    return YpLiteReallocationServiceStub(yp_lite_reallocation_rpc_client)


@pytest.fixture
def yp_lite_endpoint_sets_stub(monkeypatch,
                               pods_groups_dict):  # type: (MonkeyPatch, Dict) -> YpLiteUIEndpointSetsServiceStub
    yp_lite_endpoint_sets_rpc_client = RetryingRpcClient("https://localhost/api/yplite/endpoint-sets/", "token")

    # monkeypatch.setattr(
    #    "infra.nanny.yp_lite_api.py_stubs.pod_sets_api_stub.YpLiteUIPodSetsServiceStub.list_pods_groups",
    #    lambda _, __: json_format.ParseDict(pods_groups_dict, pod_sets_api_pb2.ListPodsGroupsResponse()),
    # )
    return YpLiteUIEndpointSetsServiceStub(yp_lite_endpoint_sets_rpc_client)


@pytest.fixture
def sandbox_client():  # type: () -> SandboxClient
    return SandboxClient(auth=NoAuth())


@pytest.fixture
def sandbox_proxy_client():  # type: () -> SandboxProxyClient
    return SandboxProxyClient(token="token")


@pytest.fixture
def vault_client():  # type: () -> VaultClient
    return VaultClient(check_status=False)


@pytest.fixture
def manager(
    rest_client,  # type: ServiceRepoClient
    repo_stub,  # type: RepoServiceStub
    yp_lite_stub,  # type: YpLiteUIPodSetsServiceStub
    yp_lite_reallocation_stub,  # type: YpLiteReallocationServiceStub
    yp_lite_endpoint_sets_stub,  # type: YpLiteUIEndpointSetsServiceStub
    sandbox_client,  # type: SandboxClient,
    sandbox_proxy_client,  # type: SandboxProxyClient,
    vault_client,  # type: VaultClient
):  # type: (...) -> ServiceRepoManager
    return ServiceRepoManager(
        nanny_rest_client=rest_client,
        nanny_repo_stub=repo_stub,
        yp_lite_stub=yp_lite_stub,
        yp_lite_reallocation_stub=yp_lite_reallocation_stub,
        yp_lite_endpoint_sets_stub=yp_lite_endpoint_sets_stub,
        sandbox_rest_client=sandbox_client,
        vault_client=vault_client,
        sandbox_proxy=sandbox_proxy_client,
    )


@pytest.fixture(scope="module")
def fixtures_dir():  # type: () -> unicode
    if yatest is None:
        return os.path.join(os.path.dirname(os.path.abspath(__file__)), 'fixtures')
    else:
        return yatest.common.source_path("market/sre/tools/rtc/nanny/tests/fixtures")


@pytest.fixture(scope="module")
def pods_groups_dict(fixtures_dir):  # type: (unicode) -> Dict
    with open(os.path.join(fixtures_dir, "pods_groups.json")) as f:
        return json.load(f)


@pytest.fixture(scope="module")
def nanny_service_dict(fixtures_dir):  # type: (unicode) -> Dict
    with open(os.path.join(fixtures_dir, "service.json")) as f:
        return json.load(f)


@pytest.fixture(scope="module")
def endpoint_sets_dict(fixtures_dir):  # type: (unicode) -> Dict
    with open(os.path.join(fixtures_dir, "endpoint_sets.json")) as f:
        return json.load(f)


@pytest.fixture(scope="module")
def pods_reallocation_dict(fixtures_dir):  # type: (unicode) -> Dict
    with open(os.path.join(fixtures_dir, "pods_reallocation.json")) as f:
        return json.load(f)


@pytest.fixture(scope="module")
def ui_config_js(fixtures_dir):  # type: (unicode) -> Text
    with open(os.path.join(fixtures_dir, "ui_config.js"), "rb") as f:
        return f.read().decode("utf-8")


@pytest.fixture
def nanny_service(nanny_service_dict):  # type: (Dict) -> Service
    return Service.from_dict(nanny_service_dict)


@pytest.fixture
def info_attrs_dict(fixtures_dir):  # type: (unicode) -> Dict
    """
    :return A Info Attributes from /v2/services/{}/info_attrs
    """
    with open(os.path.join(fixtures_dir, "info_attrs.json")) as f:
        return json.load(f)


@pytest.fixture
def info_attrs(info_attrs_dict):  # type: (Dict) -> InfoAttrs
    return InfoAttrs.from_dict(info_attrs_dict)


@pytest.fixture
def runtime_attrs_dict(fixtures_dir):  # type: (unicode) -> Dict
    """
    :return A Runtime Attributes from /v2/services/{}/runtime_attrs
    """
    with open(os.path.join(fixtures_dir, "runtime_attrs.json")) as f:
        return json.load(f)


@pytest.fixture
def runtime_attrs(runtime_attrs_dict):  # type: (Dict) -> RuntimeAttrs
    return RuntimeAttrs.from_dict(runtime_attrs_dict)


@pytest.fixture
def auth_attrs_dict(fixtures_dir):  # type: (unicode) -> Dict
    """
    :return A Info Attributes from /v2/services/{}/auth_attrs
    """
    with open(os.path.join(fixtures_dir, "auth_attrs.json")) as f:
        return json.load(f)


@pytest.fixture
def auth_attrs(auth_attrs_dict):  # type: (Dict) -> AuthAttrs
    return AuthAttrs.from_dict(auth_attrs_dict)


@pytest.fixture
def current_state_dict(fixtures_dir):
    with open(os.path.join(fixtures_dir, "current_state.json")) as f:
        return json.load(f)


@pytest.fixture
def current_state(current_state_dict):
    return CurrentState.from_dict(current_state_dict)


@pytest.fixture
def dashboard_dict(fixtures_dir):  # type: (unicode) -> Dict
    """
    :return A Nanny Dashboard
    """
    with open(os.path.join(fixtures_dir, "dashboard.json")) as f:
        return json.load(f)


@pytest.fixture
def dashboard(dashboard_dict):  # type: (Dict) -> Dashboard
    return Dashboard.from_dict(dashboard_dict)


@pytest.fixture
def java_service(manager):  # type: (ServiceRepoManager) -> JavaService
    return JavaService(
        manager=manager, template_service_id="template", app_sandbox_resource_id=0
    )


@pytest.fixture
def common_service(manager):  # type: (ServiceRepoManager) -> CommonService
    return CommonService(manager=manager, template_service_id="template")


@pytest.fixture
def nodejs_service(manager):  # type: (ServiceRepoManager) -> NodejsService
    return NodejsService(
        manager=manager,
        template_service_id="template",
        app_sandbox_resource_id=0,
        conf_sandbox_resource_id=1,
        application_name="nodejs_service_test"
    )


@pytest.fixture
def clone_service(manager):  # type: (ServiceRepoManager) -> CloneService
    return CloneService(manager=manager, template_service_id="template")

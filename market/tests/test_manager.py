from __future__ import unicode_literals, absolute_import

from typing import Dict, Text

from _pytest.monkeypatch import MonkeyPatch  # noqa
from google.protobuf import json_format
from infra.nanny.yp_lite_api.proto import (
    pod_sets_api_pb2,
    pod_reallocation_api_pb2,
    endpoint_sets_api_pb2,
)
from nanny_rpc_client.exceptions import BadRequestError

from market.sre.tools.rtc.nanny.manager import ServiceRepoManager  # noqa


def test_list_service_ids(monkeypatch, manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """

    class FakeResponse:
        class FakeService:
            def __init__(self, service_id):
                self.service_id = service_id

        def __init__(self, service_ids):
            self.value = [self.FakeService(s) for s in service_ids]

    expected = ["service1", "service2", "service3"]
    monkeypatch.setattr(
        "nanny_repo.repo_api_stub.RepoServiceStub.list_summaries",
        lambda a, b: FakeResponse(expected),
    )

    assert manager.list_service_ids("test_login") == expected


def test_get_service(manager):  # type: (ServiceRepoManager) -> None
    service = manager.get_service("test_service")

    assert service.runtime_attrs
    assert service.runtime_attrs.content
    assert service.runtime_attrs.snapshot_id
    assert service.info_attrs
    assert service.info_attrs.content
    assert service.info_attrs.snapshot_id
    assert service.auth_attrs
    assert service.auth_attrs.content
    assert service.auth_attrs.snapshot_id
    assert service.id


def test_create_service(monkeypatch, nanny_service, nanny_service_dict, manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type nanny_service: market.sre.tools.rtc.nanny.models.Service
    :type nanny_service_dict: dict
    :type manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """

    def test_request(request_dict):
        assert request_dict.get("id")
        assert request_dict.get("comment")
        assert request_dict.get("info_attrs")
        assert request_dict.get("runtime_attrs")
        assert request_dict.get("auth_attrs")

        assert request_dict["runtime_attrs"].get("resources")

        return nanny_service_dict

    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.create_service",
        lambda _, b: test_request(b),
    )
    manager.create_service(nanny_service, "test")


def test_get_info_attrs(monkeypatch, info_attrs_dict, manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type info_attrs_dict: dict
    :type manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """
    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.get_info_attrs",
        lambda _, x: info_attrs_dict,
    )
    info_attrs = manager.get_info_attrs("test_service")

    assert info_attrs.content
    assert info_attrs.snapshot_id


def test_put_info_attrs(monkeypatch, info_attrs_dict, info_attrs, manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type info_attrs_dict: dict
    :type info_attrs: market.sre.tools.rtc.nanny.models.InfoAttrs
    :type real_manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """

    def test_request(request_dict):
        assert request_dict.get("snapshot_id")
        assert request_dict.get("comment")
        assert request_dict.get("content")

        assert request_dict["content"].get("category")

        return info_attrs_dict

    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.put_info_attrs",
        lambda _, a, b: test_request(b),
    )
    response_info_attrs = manager.put_info_attrs("test_service", info_attrs, "test")

    assert response_info_attrs.content
    assert response_info_attrs.snapshot_id


def test_get_runtime_attrs(monkeypatch, runtime_attrs_dict, manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type runtime_attrs_dict: dict
    :type manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """
    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.get_runtime_attrs",
        lambda _, x: runtime_attrs_dict,
    )
    runtime_attrs = manager.get_runtime_attrs("test_service")

    assert runtime_attrs.content
    assert runtime_attrs.snapshot_id


def test_put_runtime_attrs(monkeypatch, runtime_attrs_dict, runtime_attrs, manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type runtime_attrs_dict: dict
    :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
    :type real_manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """

    def test_request(request_dict):
        assert request_dict.get("snapshot_id")
        assert request_dict.get("comment")
        assert request_dict.get("content")

        assert request_dict["content"].get("resources")

        return runtime_attrs_dict

    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.put_runtime_attrs",
        lambda _, a, b: test_request(b),
    )
    response_runtime_attrs = manager.put_runtime_attrs(
        "test_service", runtime_attrs, "test"
    )

    assert response_runtime_attrs.content
    assert response_runtime_attrs.snapshot_id


def test_get_auth_attrs(monkeypatch, auth_attrs_dict, manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type auth_attrs_dict: dict
    :type manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """
    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.get_auth_attrs",
        lambda _, x: auth_attrs_dict,
    )
    auth_attrs = manager.get_auth_attrs("test_service")

    assert auth_attrs.content
    assert auth_attrs.snapshot_id


def test_put_auth_attrs(monkeypatch, auth_attrs_dict, auth_attrs, manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type auth_attrs_dict: dict
    :type auth_attrs: market.sre.tools.rtc.nanny.models.AuthAttrs
    :type real_manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """

    def test_request(request_dict):
        assert request_dict.get("snapshot_id")
        assert request_dict.get("comment")
        assert request_dict.get("content")

        assert request_dict["content"].get("owners")

        return auth_attrs_dict

    monkeypatch.setattr(
        "infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.put_auth_attrs",
        lambda _, a, b: test_request(b),
    )
    response_auth_attrs = manager.put_auth_attrs("test_service", auth_attrs, "test")

    assert response_auth_attrs.content
    assert response_auth_attrs.snapshot_id


def test_get_pods_groups(
    monkeypatch,  # type: MonkeyPatch
    manager,  # type: ServiceRepoManager
    pods_groups_dict  # type: Dict
):  # type: (...) -> None
    monkeypatch.setattr(
        "infra.nanny.yp_lite_api.py_stubs.pod_sets_api_stub.YpLiteUIPodSetsServiceStub.list_pods_groups",
        lambda _, __: json_format.ParseDict(pods_groups_dict, pod_sets_api_pb2.ListPodsGroupsResponse()),
    )

    pods_groups = manager.get_pods_groups("SAS", "test")

    assert pods_groups
    assert pods_groups.pods_groups
    assert pods_groups.pods_groups[0]
    assert pods_groups.pods_groups[0].grouper
    assert pods_groups.pods_groups[0].summaries
    assert pods_groups.pods_groups[0].allocation_request


def test_list_service_endpoint_sets(
    monkeypatch,  # type: MonkeyPatch
    manager,  # type: ServiceRepoManager
    endpoint_sets_dict  # type: Dict
):  # type: (...) -> None
    monkeypatch.setattr(
        "infra.nanny.yp_lite_api.py_stubs."
        "endpoint_sets_api_stub.YpLiteUIEndpointSetsServiceStub.list_endpoint_sets",
        lambda _, __: json_format.ParseDict(endpoint_sets_dict, endpoint_sets_api_pb2.ListEndpointSetsResponse()),
    )

    endpoint_sets = manager.list_service_endpoint_sets("SAS", "test")
    assert endpoint_sets
    assert endpoint_sets.endpoint_sets
    assert endpoint_sets.endpoint_sets[0]
    assert endpoint_sets.endpoint_sets[0].meta
    assert endpoint_sets.endpoint_sets[0].spec


def test_pods_reallocation(
    monkeypatch,  # type: MonkeyPatch
    manager,  # type: ServiceRepoManager
    pods_reallocation_dict  # type: Dict
):  # type: (...) -> None
    monkeypatch.setattr(
        "infra.nanny.yp_lite_api.py_stubs."
        "pod_reallocation_api_stub.YpLiteReallocationServiceStub.get_pod_reallocation_spec",
        lambda _, __: json_format.ParseDict(pods_reallocation_dict,
                                            pod_reallocation_api_pb2.GetPodReallocationSpecResponse()),
    )

    pods_reallocation = manager.get_pods_reallocation("SAS")

    assert pods_reallocation
    assert pods_reallocation.spec
    assert pods_reallocation.spec.id
    assert pods_reallocation.spec.pod_spec

    def ex(*args, **kwargs):
        raise BadRequestError("Reallocation spec not found")

    monkeypatch.setattr(
        "infra.nanny.yp_lite_api.py_stubs."
        "pod_reallocation_api_stub.YpLiteReallocationServiceStub.get_pod_reallocation_spec",
        ex
    )

    try:
        manager.get_pods_reallocation("SAS")
        assert False, "Exception expected"
    except BadRequestError as exc:
        assert str(exc) == "Reallocation spec not found"

    assert manager.get_pods_reallocation("SAS", fail_silent=True) is None


def test_get_stable_instancectl(
    monkeypatch,  # type: MonkeyPatch
    manager,  # type: ServiceRepoManager
    ui_config_js  # type: Text
):  # type: (...) -> None
    class FakeResponse:
        text = ui_config_js

        def raise_for_status(self):
            pass

    def request(*args, **kwargs):
        return FakeResponse()

    monkeypatch.setattr(
        manager._rest_client._session,
        "request",
        request,
    )

    instancectl = manager.get_stable_instancectl()

    assert instancectl["version"] == "2.66"

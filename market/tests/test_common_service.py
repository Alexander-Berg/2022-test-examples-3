from itertools import chain, cycle

import pytest
from _pytest.monkeypatch import MonkeyPatch  # noqa
from infra.nanny.yp_lite_api.proto import pod_sets_api_pb2

from market.sre.tools.rtc.nanny.models.attributes.current_state import CurrentState
from market.sre.tools.rtc.nanny.models.service import Service  # noqa
from market.sre.tools.rtc.nanny.scenarios.common_service import CommonService  # noqa


@pytest.mark.parametrize(
    "gencfg_group,engine,gencfg_release",
    [
        ("IVA_MARKET_TEST_NOT_EXIST", "ISS_MSK", "trunk"),
        ("VLA_MARKET_TEST_NOT_EXIST", "ISS_VLA", "trunk"),
        ("SAS_MARKET_TEST_NOT_EXIST", "ISS_SAS", "trunk"),
        ("VLA_MARKET_TEST_NOT_EXIST", "ISS_VLA", "tags/stable-123-1"),
    ],
)
def test_step_update_instances_gencfg(
    nanny_service,  # type: Service
    common_service,  # type: CommonService
    gencfg_group,  # type: unicode
    engine,  # type: unicode
    gencfg_release,  # type: unicode
):  # type: (...) -> None
    common_service._step_update_instances(
        service=nanny_service,
        group=gencfg_group,
        gencfg_release=gencfg_release if gencfg_release != "None" else None,
        is_yp=False,
    )
    assert any(
        g["name"] == gencfg_group
        for g in nanny_service.runtime_attrs.instances.extended_gencfg_groups.groups
    )
    assert nanny_service.runtime_attrs.engines.engine_type == engine
    assert any(
        g["release"] == gencfg_release
        for g in nanny_service.runtime_attrs.instances.extended_gencfg_groups.groups
    )


def test_step_update_instances_yp(
    nanny_service,  # type: Service
    common_service,  # type: CommonService
):  # type: (...) -> None
    group = CommonService.Group(nanny_service.id, 0)

    common_service._step_update_instances(
        service=nanny_service,
        group=group,
        gencfg_release=None,
        is_yp=True,
    )
    assert nanny_service.runtime_attrs.engines.engine_type == 'YP_LITE'
    assert nanny_service.runtime_attrs.instances.chosen_type == 'YP_POD_IDS'
    assert 'groups' not in nanny_service.runtime_attrs.instances.content


def test_step_update_owners(
    nanny_service, common_service
):  # type: (Service, CommonService) -> None
    logins = ["test_loging"]
    groups = [123]
    common_service._step_update_owners(
        service=nanny_service, owners_logins=logins, owners_groups=groups
    )
    assert set(logins).issubset(nanny_service.auth_attrs.owners.logins)
    assert set(groups).issubset(nanny_service.auth_attrs.owners.groups)


@pytest.mark.xfail
def test_step_update_sandbox_files(
    nanny_service, common_service
):  # type: (Service, CommonService) -> None
    # datasources_resource = SandboxResource(
    #     id=123,
    #     resource_type='TEST_RESOURCE_TYPE',
    #     task=SandboxTask(id=456, task_type='TEST_TASK_TYPE')
    # )
    assert False
    # TODO: mock sandbox client
    # common_service._step_update_sandbox_files(
    #     service=nanny_service, datasources_resource=datasources_resource
    # )


@pytest.mark.parametrize("environment", ['"testing"', '"prestable"', '"stable"'])
def test_step_update_tickets_integration(
    nanny_service,  # type: Service
    common_service,  # type: CommonService
    environment,  # type: unicode
):  # type: (...) -> None
    common_service._step_update_tickets_integration(nanny_service)
    all(
        environment in r["filter_params"]["expression"]
        for r in nanny_service.info_attrs.tickets_integration.service_release_rules
    )


@pytest.mark.parametrize(
    "resource_type",
    ["MARKET_DATASOURCES_{}".format(env) for env in ["TESTING", "PRESTABLE", "STABLE"]],
)
def test_step_update_tickets_integration_rule_for_datasources(
    nanny_service,  # type: Service
    common_service,  # type: CommonService
    resource_type,  # type: unicode
):  # type: (...) -> None
    from market.sre.tools.rtc.nanny.models.sandbox import SandboxResource, SandboxTask

    datasources_resource = SandboxResource(
        id=0,
        resource_type=resource_type,
        task=SandboxTask(id=1, task_type="BUILD_MARKET_DATASOURCES"),
    )
    common_service._step_update_tickets_integration_rule_for_datasources(
        service=nanny_service, datasources_resource=datasources_resource
    )
    assert any(
        r["sandbox_resource_type"] == resource_type
        for r in nanny_service.info_attrs.tickets_integration.service_release_rules
    )


@pytest.mark.xfail
def test_get_datasources_resource(common_service):  # type: (CommonService) -> None
    # TODO: mock sandbox client
    assert False


def test_step_update_labels(nanny_service):  # type: (Service) -> None
    labels = nanny_service.info_attrs.labels
    expected_1 = [{"key": "geo"}, {"key": "ctype"}, {"key": "itype"}, {"key": "prj"}]
    assert nanny_service.info_attrs.labels == expected_1
    map_labels = {"geo": "sas", "ctype": "testing"}
    for label in labels:
        if map_labels.get(label.get("key")):
            label["value"] = map_labels[label["key"]]
    expected_2 = [{"key": "geo", "value": "sas"},
                  {"key": "ctype", "value": "testing"},
                  {"key": "itype"},
                  {"key": "prj"}]
    assert nanny_service.info_attrs.labels == expected_2


def test_step_update_pods(monkeypatch,
                          nanny_service,
                          common_service):  # type: (MonkeyPatch, Service, CommonService) -> None
    group = CommonService.Group(nanny_service.id, 0)

    class MockCurrentState:

        def __init__(self):
            self.results = chain([
                self.s({}),
                self.s({"summary": {"value": "OFFLINE"}}),
                self.s({"summary": {"value": "OFFLINE"}, "active_snapshots": [{}]}),
                self.s({"active_snapshots": [{"state": "COMMITTED", "snapshot_id": "s1"}]}),
                self.s({"active_snapshots": [{"state": "ACTIVE", "snapshot_id": "s1"}]}),
                self.s({}, rel={"state": {"message": "IN_PROGRESS"}}),
            ], cycle([self.s({}, rel={"state": {"message": "DONE"}})]))
            self.counter = 0

        def __call__(self, *args, **kwargs):
            self.counter += 1
            return next(self.results)

        @classmethod
        def s(cls, upd, rel=None):
            c = {"summary": {"value": "ONLINE"}, "active_snapshots": []}
            c.update(upd)
            return CurrentState.from_dict(
                {"content": c, "_id": {}, "reallocation": {} if rel is None else rel}
            )

    def mock_return_true(*args, **kwargs):
        return True

    def mock_return_args_0(*args, **kwargs):
        return args[0]

    def mock_get_pods_groups(*args, **kwargs):
        class MockPodsGroups:
            class MockPodsGroup:
                class MockSummaries:
                    def __init__(self, _id):
                        self.id = _id

                summaries = [MockSummaries('test1'), MockSummaries('test2')]

            pods_groups = [MockPodsGroup]

        return MockPodsGroups

    get_pods_reallocation_counter = []

    def moc_get_pods_reallocation(*args, **kwargs):
        class Reallocation:
            class Spec:
                id = 'rid1'

            spec = Spec

        get_pods_reallocation_counter.append(True)
        return Reallocation

    mock_current_state = MockCurrentState()

    monkeypatch.setattr(common_service._manager, "get_current_state", mock_current_state)
    monkeypatch.setattr(common_service._manager, "reallocate_pods", mock_return_true)
    monkeypatch.setattr(common_service._manager, "create_service_pods", mock_return_true)
    monkeypatch.setattr(common_service._manager, "get_pods_groups", mock_get_pods_groups)
    monkeypatch.setattr(common_service._manager, "update_service", mock_return_args_0)
    monkeypatch.setattr(common_service._manager, "remove_service_pods", mock_return_true)
    monkeypatch.setattr(common_service._manager, "get_pods_reallocation", moc_get_pods_reallocation)
    monkeypatch.setattr("market.sre.tools.rtc.nanny.scenarios.common_service.PODS_REALLOCATION_INTERVAL", 1)

    try:
        common_service._step_update_pods(nanny_service, group, 0, 1)
        assert False, "Exception expected"
    except ValueError as exc:
        assert str(exc) == "Can't change instance number for running service"

    service = common_service._step_update_pods(nanny_service, group, 1, 1)
    assert [p.pod_id for p in service.runtime_attrs.instances.yp_pod_ids.pods] == ['test1', 'test2']

    try:
        common_service._step_update_pods(nanny_service, group, 1, 0)
        assert False, "Exception expected"
    except ValueError as exc:
        assert str(exc) == ("Can't change pods configuration, service must be offline without any active "
                            "snapshots to recreate pods or must be online to update pods without changing "
                            "number of them")

    try:
        common_service._step_update_pods(nanny_service, group, 1, 0)
        assert False, "Exception expected"
    except ValueError as exc:
        assert str(exc) == "Active snapshot not found, can't reallocate pods"

    common_service._step_update_pods(nanny_service, group, 1, 0)
    assert len(get_pods_reallocation_counter) == 1
    assert mock_current_state.counter == 7


def test_group():
    g1 = CommonService.Group("testing_some_thing_some_thing_sas", 0)

    al = g1.generate_allocation_request()

    assert g1.dc == "SAS"
    assert g1.ctype == "testing"
    assert not g1.enumerate
    assert al.root_volume_storage_class == "hdd"

    g2 = CommonService.Group("testing_some_thing_sas", 0, "itype", 1, 2000, 2048, "_test_", "SSD", 1, 3,
                             "/s", "hdd_5_10", 5120, "/b", "ssd_4", 6144, "/c", "hdd_4", 6144)

    al = g2.generate_allocation_request()

    assert g2.enumerate
    assert g2.itype == "itype"
    assert al.replicas == 1
    assert al.vcpu_guarantee == 2000
    assert al.memory_guarantee_megabytes == 2048
    assert al.anonymous_memory_limit_megabytes == 2048 - 64
    assert al.network_macro == "_TEST_"
    assert al.work_dir_quota_megabytes == 3072
    assert al.root_bandwidth_guarantee_megabytes_per_sec == 30
    assert al.root_bandwidth_limit_megabytes_per_sec == 30
    assert al.pod_naming_mode == pod_sets_api_pb2.AllocationRequest.ENUMERATE
    for v in al.persistent_volumes:
        if v.mount_point == "/s":
            assert v.disk_quota_megabytes == 5 * 1024
            assert v.storage_class == "hdd"
            assert v.bandwidth_guarantee_megabytes_per_sec == 5
            assert v.bandwidth_limit_megabytes_per_sec == 10
        elif v.mount_point == "/b":
            assert v.disk_quota_megabytes == 6 * 1024
            assert v.storage_class == "ssd"
            assert v.bandwidth_guarantee_megabytes_per_sec == 4
            assert v.bandwidth_limit_megabytes_per_sec == 4
        elif v.mount_point == "/c":
            assert v.storage_class == "hdd"
            assert v.bandwidth_guarantee_megabytes_per_sec == 4
            assert v.bandwidth_limit_megabytes_per_sec == 8

    g4 = CommonService.Group("testing_some_thing_sas", 0, "itype", 1, 2000, 1024, "_test_", "hdd", 1, 3,
                             "/r", "", 5120, "/t", "", 5120, "/y", "", 5120)

    al = g4.generate_allocation_request()

    assert al.root_bandwidth_guarantee_megabytes_per_sec == 4
    assert al.root_bandwidth_limit_megabytes_per_sec == 8
    for v in al.persistent_volumes:
        if v.mount_point == "/r":
            assert v.bandwidth_guarantee_megabytes_per_sec == 4
            assert v.bandwidth_limit_megabytes_per_sec == 8
        if v.mount_point == "/t":
            assert v.bandwidth_guarantee_megabytes_per_sec == 4
            assert v.bandwidth_limit_megabytes_per_sec == 8
        if v.mount_point == "/y":
            assert v.bandwidth_guarantee_megabytes_per_sec == 3
            assert v.bandwidth_limit_megabytes_per_sec == 6

    g3 = CommonService.Group("testing_some_thing_sas", 0, "itype", 1, 2000, 10240, "_test_", "hdd", 1, 3,
                             "/s", "", 5120)

    al = g3.generate_allocation_request()

    assert al.anonymous_memory_limit_megabytes == 10240 - 64
    assert al.root_bandwidth_guarantee_megabytes_per_sec == 8
    assert al.root_bandwidth_limit_megabytes_per_sec == 16
    for v in al.persistent_volumes:
        if v.mount_point == "/s":
            assert v.disk_quota_megabytes == 5 * 1024
            assert v.storage_class == "hdd"
            assert v.bandwidth_guarantee_megabytes_per_sec == 7
            assert v.bandwidth_limit_megabytes_per_sec == 14

    try:
        g1.parse_volumes(["/", "hdd", 1, "/sss"])
        assert False, "Exception expected"
    except ValueError as exc:
        assert str(exc) == "invalid volumes format"

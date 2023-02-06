import collections

from crypta.utils.rtmr_resource_service.bin.releaser.lib.resource_releaser import ResourceReleaser
from crypta.utils.rtmr_resource_service.lib.mock_db_client import MockDbClient
from crypta.utils.rtmr_resource_service.lib.mock_reports_client import MockReportsClient
from crypta.utils.rtmr_resource_service.lib.resource import Resource


Case = collections.namedtuple("Case", ["id", "release_type", "resource_name", "versions", "oks"])


class MockSandboxClient(object):
    def __init__(self, releases):
        self.releases = releases

    def get_release_type(self, resource_id):
        return self.releases.get(resource_id)["release_type"]

    def release(self, resource_id, release_type, new_ttl=None):
        new_release = self.releases.setdefault(resource_id, {})
        new_release["release_type"] = release_type
        if new_ttl:
            new_release["ttl"] = new_ttl


def test_release():
    sequence = ["testing", "prestable", "stable"]
    get_thresholds = {
        "testing": 50,
        "prestable": 100,
    }
    ok_rate_thresholds = {
        "testing": 0.8,
        "prestable": 0.8,
    }

    cases = [
        Case(10, "testing", "testing_only", 50, 40),
        Case(20, "prestable", "prestable_only", 100, 80),
        Case(30, "stable", "stable_only", 200, 200),
        Case(40, "prestable", "testing_and_prestable", 100, 100),
        Case(41, "testing", "testing_and_prestable", 50, 50),
        Case(50, "testing", "testing_not_enough_gets", 45, 49),
        Case(60, "testing", "testing_not_enough_oks", 100, 79),
    ]

    resources = [
        Resource(resource_name, resource_name, ttls={"stable": "inf"})
        for resource_name in set(case.resource_name for case in cases)
    ]
    db_state = {}
    reports_ok = {}
    reports_version = {}
    for case in cases:
        db_state.setdefault(case.release_type, {})[case.resource_name] = case.id
        reports_ok.setdefault(case.release_type, {}).setdefault(case.resource_name, {})[case.id] = set(range(case.oks))
        reports_version.setdefault(case.release_type, {}).setdefault(case.resource_name, {})[case.id] = set(range(case.versions))

    db_client = MockDbClient(db_state)
    reports_client = MockReportsClient(reports_ok=reports_ok, reports_version=reports_version)

    sandbox_client = MockSandboxClient({
        case.id: dict(release_type=case.release_type, ttl=1)
        for case in cases
    })

    releaser = ResourceReleaser(resources, db_client, reports_client, sandbox_client, get_thresholds, ok_rate_thresholds, sequence, True)
    releaser.release()

    return {
        "releases": sandbox_client.releases,
    }

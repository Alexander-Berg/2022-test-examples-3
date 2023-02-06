import collections
import logging

import pytest
import yaml
import yatest.common

from crypta.lib.python.spine import arcadia_ci
from crypta.spine import spine


logger = logging.getLogger(__name__)

ARCANUM_CI_OAUTH_SECRET = "sec-01efps66gnbmejg9wvc9ptvhmc"
SANDBOX_OWNER = "CRYPTA"


def get_releases():
    registry = spine.get_config_registry()
    result = collections.defaultdict(list)

    for release in registry.get_configs(arcadia_ci.Release.TAG):
        result[(release.project_title, release.abc_service)].append(release)
    print(result)
    return result


@pytest.mark.parametrize("title,releases", [
    pytest.param(title, releases, id=title.replace(" ", "_"))
    for (title, abc_service), releases in get_releases().iteritems()
])
def test_arcadia_ci(title, releases):
    config = {
        "service": "cryptadev",
        "title": title,
        "ci": {
            "secret": ARCANUM_CI_OAUTH_SECRET,
            "runtime": {
                "sandbox-owner": SANDBOX_OWNER,
            },
            "releases": {release.id: release.render() for release in releases},
            "flows": {release.flow.id: release.flow.render() for release in releases},
        },
    }
    filepath = yatest.common.test_output_path("a.yaml")

    with open(filepath, "w") as f:
        yaml.dump(config, f)

    return yatest.common.canonical_file(filepath, local=True)

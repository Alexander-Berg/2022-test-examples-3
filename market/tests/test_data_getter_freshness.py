import json
import pytest
import time

from juggler.bundles.types import Status
from market.sre.juggler.bundles.checks.data_getter_freshness.bin.main import check_data_getter_freshness


@pytest.mark.parametrize('now_delta, expected_status', [
    (10, Status.OK),
    (50, Status.WARN),
    (100, Status.CRIT),
])
def test_freshness(tmp_path, now_delta, expected_status):
    with open(tmp_path / 'meta.json', 'w') as fd:
        json.dump({
            'creation_time': int(time.time()) - now_delta
        }, fd)

    status, _ = check_data_getter_freshness(tmp_path / 'meta.json', warning_threshold=25, critical_threshold=75)
    assert status == expected_status

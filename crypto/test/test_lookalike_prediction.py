import pytest

from crypta.audience.lib.tasks.lookalike import prediction
from crypta.lib.proto.user_data.user_data_stats_pb2 import TUserDataStats


UNKNOWN_REGION = 0


@pytest.mark.parametrize("region,result", [
    pytest.param(
        [
            TUserDataStats.TRegionCount(Region=UNKNOWN_REGION, Count=1),
        ],
        True,
        id="unknown-region-only",
    ),
    pytest.param(
        [
            TUserDataStats.TRegionCount(Region=1, Count=1),
        ],
        False,
        id="known-region-only",
    ),
    pytest.param(
        [
            TUserDataStats.TRegionCount(Region=1, Count=1),
            TUserDataStats.TRegionCount(Region=UNKNOWN_REGION, Count=1),
        ],
        False,
        id="mixed-known-first",
    ),
    pytest.param(
        [
            TUserDataStats.TRegionCount(Region=UNKNOWN_REGION, Count=1),
            TUserDataStats.TRegionCount(Region=1, Count=1),
        ],
        False,
        id="mixed-unknown-first",
    ),
])
def test_has_only_unknown_region(region, result):
    user_data_stats = TUserDataStats(
        Attributes=TUserDataStats.TAttributesStats(
            Region=region,
        ),
    )
    assert result == prediction.has_only_unknown_region(user_data_stats)

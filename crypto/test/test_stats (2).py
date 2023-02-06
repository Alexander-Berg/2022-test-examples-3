from crypta.graph.fuzzy.lib.common.statistics_sender import to_stats_sender
from datetime import date
import pytest
import os


@pytest.mark.skipif(
    os.environ.get("TEST_STATS_SENDER", None) is None, reason="Stats sender is not to be test regularly"
)
def test_stat_sender():
    to_stats_sender("test_report", [{"metric": 15.89}], date=date.today().isoformat())

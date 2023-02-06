import time

import pytest

from crypta.lib.python import period


@pytest.mark.parametrize("period_seconds,sleep_seconds", [
    [7, 4],
    [4, 7]
])
def test_period_with_short_body(period_seconds, sleep_seconds):
    start_time = time.time()
    with period.period(seconds=period_seconds):
        time.sleep(sleep_seconds)
    elapsed_seconds = time.time() - start_time
    assert max(period_seconds, sleep_seconds) <= elapsed_seconds < max(period_seconds, sleep_seconds) + 1

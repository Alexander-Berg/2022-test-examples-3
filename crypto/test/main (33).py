import pytest

import helpers


@pytest.mark.parametrize("test_name", [
    "ArchiveWithOlderTimestampThanState",
    "FreshExists__StateExists",
    "FreshExists__StateMissing",
    "FreshMissing__StateExists",
    "FreshMissing__StateMissing",
    "TariffChanging",
    "ValidInvalidMix"
])
def test_zero_rc(yt_stuff, test_name):
    return helpers.execute_binary(yt_stuff, test_name)

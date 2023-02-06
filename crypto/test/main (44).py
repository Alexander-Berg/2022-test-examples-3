import os

from crypta.lib.python import time_utils
from crypta.lib.python.yt import path_utils


def test_path_utils():
    base_path = "//yt/test/path"
    frozen_ts = "1111111111"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = frozen_ts
    assert "{}/{}".format(base_path, frozen_ts) == path_utils.get_ts_table_path(base_path)
    assert "{}/table_{}".format(base_path, frozen_ts) == path_utils.get_ts_table_path(base_path, "table")

from mpfs.common.util.generator import cloud_request_id
import re


def test_cloud_request_id():
    assert re.match('mpfs-[0-9|a-f]{32}-.+', cloud_request_id())

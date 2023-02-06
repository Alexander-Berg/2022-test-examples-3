import dateutil.parser
import mock
import pytest

from crypta.dmp.adobe.monitorings.check_s3_bucket.lib import watcher


class Key(object):
    def __init__(self, name, last_modified):
        self.name = name
        self.last_modified = last_modified


@pytest.mark.parametrize("keys,destination_ids,status", [
    (
        [],
        [],
        watcher.BucketStatus([], 0, 0)
    ),
    (
        [Key(".verifyACL", "2019-01-03T08:45:07.000Z")],
        [],
        watcher.BucketStatus([], 0, 0)
    ),
    (
        [Key("12", "2019-01-03T08:45:07.000Z")],
        [],
        watcher.BucketStatus(["Invalid filename format: '12'"], 0, 0)
    ),
    (
        [Key("S3_80811_20915__full_1544622793000.info", "2019-01-03T08:45:07.000Z")],
        [],
        watcher.BucketStatus([], 0, 1)
    ),
    (
        [Key("S3_80811_20915__full_1544622793000.info", "2019-01-03T08:45:07.000Z"),
         Key(".verifyACL", "2019-01-03T08:40:07.000Z"),
         Key("./verifyACL", "2019-01-03T08:40:07.000Z")],
        [80811],
        watcher.BucketStatus([], 300, 0)
    ),
    (
        [
            Key("S3_80811_20915__full_1544622793000.info", "2019-01-03T08:49:07.000Z"),
            Key("S3_80811_20915__full_1544622793000.sync.gz", "2019-01-03T08:48:07.000Z"),
            Key("S3_80812_20915__full_1544622793000.info", "2019-01-03T08:47:07.000Z"),
            Key("S3_80813_20915__full_1544622793000.info", "2019-01-03T08:46:07.000Z"),
            Key("S3_80814_20915__full_1544622793000.sync", "2019-01-03T08:45:07.000Z"),
            Key("12", "2019-01-03T08:45:07.000Z"),
        ],
        [80811, 80814],
        watcher.BucketStatus(["Invalid filename format: '12'"], 300, 2)
    ),
])
def test_get_bucket_status(keys, destination_ids, status):
    with mock.patch("crypta.dmp.adobe.monitorings.check_s3_bucket.lib.watcher.get_now_datetime", return_value=dateutil.parser.parse("2019-01-03T08:50:07.000Z")):
        assert status == watcher.get_bucket_status(keys, destination_ids)

import datetime
import hashlib
import logging
import os

import yatest.common
import yt.wrapper as yt

logger = logging.getLogger(__name__)


def file_md5(filename):
    md5 = hashlib.md5()
    with open(filename, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            md5.update(chunk)
    return md5.hexdigest()


def get_crypta_diff_tool_path():
    return yatest.common.binary_path("crypta/utils/yt_diff/bin/crypta-yt-diff")


def create_yt_dirs(yt_stuff, dirs):
    for path in dirs:
        yt_stuff.yt_wrapper.create("map_node", path, recursive=True)


def get_attr_with_log(logger, attr_name, yt_client, cypress_path, attr_type):
    attr_name = "@" + attr_name
    logger.info("Get %s of %s", attr_name, cypress_path)
    path = yt.ypath_join(cypress_path, attr_name)
    value = yt_client.get(path)
    logger.info("%s of %s is %s", attr_name, cypress_path, value)
    return value if attr_type is None else attr_type(value)


def get_unexpired_ttl_days(begin_date):
    return max(
        (datetime.datetime.now() - begin_date + datetime.timedelta(days=2)).days,
        1,
    )


def get_unexpired_ttl_days_for_daily(name):
    return get_unexpired_ttl_days(datetime.datetime.strptime(name, "%Y-%m-%d"))


class FileSource(object):
    def to_abs_file_path(self, dir_path):
        if not os.path.dirname(self.file_path):
            self.file_path = os.path.join(dir_path, self.file_path)


def write_yson_table_from_file(yt_client, file_path, table_path):
    table_path = yt.TablePath(table_path)

    logger.info("Write %s to %s", file_path, table_path)
    format = yt.format.YsonFormat(format="text")

    yt_client.create("table", path=table_path, recursive=True, force=True)

    with open(file_path, "rb") as input_stream:
        yt_client.write_table(table_path, input_stream, format=format, raw=True)

    return table_path

import json
import yt.yson as yson
from os.path import join
import yatest.common
from mapreduce.yt.python.yt_stuff import yt_stuff


__avatars_db_schema = yson.YsonList([
    {'name': 'KiwiKey', 'type': 'string', 'required': True, 'sort_order': 'ascending'},
    {'name': 'HttpResponse', 'type': 'string', 'required': False},
    {'name': 'Added', 'type': 'string', 'required': True},
    {'name': 'Uploaded', 'type': 'string', 'required': False},
])


__frames_db_schema = yson.YsonList([
    {'name': 'KiwiKey', 'type': 'string', 'required': True, 'sort_order': 'ascending'},
    {'name': 'Url', 'type': 'string', 'required': True, 'sort_order': 'ascending'},
    {'name': 'CanoUrl', 'type': 'string', 'required': False},
    {'name': 'GroupId', 'type': 'int64', 'required': False},
    {'name': 'Timestamp', 'type': 'int64', 'required': True},
    {'name': 'LastModified', 'type': 'int64', 'required': True},
    {'name': 'SelRank', 'type': 'double', 'required': True},
    {'name': 'BestByAlgo', 'type': 'string', 'required': False},
])


__frames_portion_schema = yson.YsonList([
    {'name': 'KiwiKey', 'type': 'string', 'required': True, 'sort_order': 'ascending'},
    {'name': 'Url', 'type': 'string', 'required': True, 'sort_order': 'ascending'},
    {'name': 'CanoUrl', 'type': 'string', 'required': False},
    {'name': 'Timestamp', 'type': 'int64', 'required': True},
    {'name': 'LastModified', 'type': 'int64', 'required': True},
    {'name': 'SelRank', 'type': 'double', 'required': True},
])


def upload_table(yt_client, test_data_prefix, table_prefix, table, schema=None):
    table_path = join(table_prefix, table)
    yt_client.create('table', table_path, recursive=True, ignore_existing=True, attributes={"schema": schema})
    with open(join(test_data_prefix, table)) as fd:
        yt_client.write_table(
            table_path,
            [json.loads(line) for line in fd.readlines()]
        )


def dump_table(yt_client, table_path, filename):
    with open(filename, "w") as outfile:
        for rec in yt_client.read_table(table_path):
            outfile.write(str(rec))
            outfile.write("\n")


def test_merge(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    yt_server = yt_stuff.get_server()
    yt_env = {
        'YT_STORAGE': 'yes',
        'YT_PROXY': yt_server,
        'YT_PATH': '//',
    }


    print "uploading input"
    upload_table(yt_client, yatest.common.data_path("extsearch/video/robot/frames/merge"), "//avatars", "avatars_db", __avatars_db_schema)
    upload_table(yt_client, yatest.common.data_path("extsearch/video/robot/frames/merge"), "//frames", "frames_db", __frames_db_schema)
    upload_table(yt_client, yatest.common.data_path("extsearch/video/robot/frames/merge"), "//frames", "frames_portion", __frames_portion_schema)

    print "executing"
    yatest.common.execute(
        [
            yatest.common.binary_path("extsearch/video/robot/frames/merge/frames_merge_db"),
            "--src", "//frames/frames_db",
            "--src", "//frames/frames_portion",
            "--avatars", "//avatars/avatars_db",
            "--dst", "//frames/frames_db_result",
            "--export", "//frames/export_result",
            "--errors", "//frames/errors_result",
            "--canonize-config-dir", yatest.common.source_path("yweb/webscripts/video/canonize/config"),
            "--now", "1492523259",
        ],
        env=yt_env,
    )

    dump_table(yt_client, "//frames/frames_db_result", "frames_db_result")
    dump_table(yt_client, "//frames/export_result", "export_result")

    return [yatest.common.canonical_file(fn) for fn in ("frames_db_result", "export_result")]

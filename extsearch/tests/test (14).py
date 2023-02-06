import json
import shutil
import yt.yson as yson
from os import makedirs
from os.path import join
import yatest.common
from mapreduce.yt.python.yt_stuff import yt_stuff


__avatars_db_schema = yson.YsonList([
    {'name': 'KiwiKey', 'type': 'string', 'required': True, 'sort_order': 'ascending'},
    {'name': 'HttpResponse', 'type': 'string', 'required': False},
    {'name': 'Added', 'type': 'string', 'required': True},
    {'name': 'Uploaded', 'type': 'string', 'required': False},
])

__avatars_db_input_schema = yson.YsonList([
    {'name': 'KiwiKey', 'type': 'string', 'required': True},
    {'name': 'Image', 'type': 'string', 'required': True},
])

__avatars_upload_result_portion_schema = yson.YsonList([
    {'name': 'KiwiKey', 'type': 'string', 'required': True},
    {'name': 'HttpResponse', 'type': 'string', 'required': True},
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
    work_path = yatest.common.work_path("cwd")
    makedirs(work_path)
    shutil.copy(yatest.common.source_path("extsearch/video/robot/avatars/merge/tests/config.json"), join(work_path, "config.json"))

    yt_client = yt_stuff.get_yt_client()
    yt_server = yt_stuff.get_server()
    yt_env = {
        'YT_STORAGE': 'yes',
        'YT_PROXY': yt_server,
        'YT_PATH': '//',
    }

    yt_client.create('map_node', '//db')
    yt_client.create('map_node', '//db/input')
    yt_client.create('map_node', '//upload')
    yt_client.create('map_node', '//upload/input')
    yt_client.create('map_node', '//upload/output')

    print "uploading data"
    upload_table(yt_client, yatest.common.data_path("extsearch/video/robot/avatars/merge"), "//db", "curbase", __avatars_db_schema)
    upload_table(yt_client, yatest.common.data_path("extsearch/video/robot/avatars/merge"), "//db/input", "db_input", __avatars_db_input_schema)
    upload_table(yt_client, yatest.common.data_path("extsearch/video/robot/avatars/merge"), "//upload/output", "upload_results", __avatars_upload_result_portion_schema)


    yatest.common.execute(
        [
            yatest.common.binary_path("extsearch/video/robot/avatars/merge/avatars_dbmerge")
        ],
        env=yt_env,
        cwd=work_path,
    )

    result_path = join(work_path, "result")
    makedirs(result_path)

    dump_table(yt_client, "//db/curbase", join(result_path, "curbase"))
    for table in yt_client.list('//upload/input'):
        dump_table(yt_client, '//upload/input/{0}'.format(table), join(result_path, 'upload_queue_{0}'.format(table)))

    return [yatest.common.canonical_dir(result_path)]

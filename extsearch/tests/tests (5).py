import os
import yatest.common
import yt_utils
import tempfile
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


def get_schema():
    schema = [
        {'name': 'Host', 'required': False, 'sort_order': 'ascending', 'type': 'string'},
        {'name': 'Path', 'required': False, 'sort_order': 'ascending', 'type': 'string'},
        {'name': 'source', 'required': False, 'sort_order': 'ascending', 'type': 'string'},
        {'name': 'itemhash', 'required': False, 'sort_order': 'ascending', 'type': 'string'},
        {'name': 'ts', 'required': False, 'sort_order': 'ascending', 'type': 'uint64'},
        {'name': 'antispamdata', 'required': False, 'type': 'string'},
        {'name': 'appearTime', 'required': False, 'type': 'uint64'},
        {'name': 'canoUrl', 'required': False, 'type': 'string'},
        {'name': 'createTime', 'required': False, 'type': 'uint64'},
        {'name': 'data', 'required': False, 'type': 'string'},
        {'name': 'duration', 'required': False, 'type': 'uint64'},
        {'name': 'errorDump', 'required': False, 'type': 'string'},
        {'name': 'errors', 'required': False, 'type': 'string'},
        {'name': 'expirationTime', 'required': False, 'type': 'uint64'},
        {'name': 'firstUpdateTime', 'required': False, 'type': 'uint64'},
        {'name': 'isArchived', 'required': False, 'type': 'boolean'},
        {'name': 'isDNSExpired', 'required': False, 'type': 'boolean'},
        {'name': 'isDeleted', 'required': False, 'type': 'boolean'},
        {'name': 'isSpam', 'required': False, 'type': 'boolean'},
        {'name': 'islast', 'required': False, 'type': 'boolean'},
        {'name': 'noIndex', 'required': False, 'type': 'boolean'},
        {'name': 'notAllowed', 'required': False, 'type': 'boolean'},
        {'name': 'platformFlags', 'required': False, 'type': 'uint64'},
        {'name': 'playerId', 'required': False, 'type': 'string'},
        {'name': 'playerIdUrl', 'required': False, 'type': 'string'},
        {'name': 'serialEpisode', 'required': False, 'type': 'uint64'},
        {'name': 'serialName', 'required': False, 'type': 'string'},
        {'name': 'serialSeason', 'required': False, 'type': 'uint64'},
        {'name': 'sourcePriority', 'required': False, 'type': 'int64'},
        {'name': 'spokLangs', 'required': False, 'type': 'string'},
        {'name': 'srcurl', 'required': False, 'type': 'string'},
        {'name': 'synthUrl', 'required': False, 'type': 'string'},
        {'name': 'tags', 'required': False, 'type': 'string'},
        {'name': 'thumbUrl', 'required': False, 'type': 'string'},
        {'name': 'updateTime', 'required': False, 'type': 'uint64'},
        {'name': 'visUrl', 'required': False, 'type': 'string'}
    ]
    return schema


def test_fetcher(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ['YT_PREFIX'] = '//'
    yt_client = yt_stuff.get_yt_client()

    yt_client.create('map_node', path='//player', recursive=True, ignore_existing=True)
    yt_client.smart_upload_file(
        yatest.common.source_path('yweb/webscripts/video/player/connectors_urlbase.xml'),
        destination='//player/connectors_urlbase.xml',
        placement_strategy='replace')
    yt_client.smart_upload_file(
        yatest.common.source_path('yweb/webscripts/video/player/generators.xml'),
        destination='//player/generators.xml',
        placement_strategy='replace')

    spec = TableSpec(
        'fetcher.test.table',
        table_name='player/media.raw',
        mapreduce_io_flags=['-format', '<format=text>yson'],
        sort_on_load=True,
        sortby=['Host', 'Path', 'source', 'itemhash', 'ts'],
        attrs_on_load={'schema': get_schema()})
    yt_utils.write_to_local(spec, yt_stuff=yt_stuff)

    binary = yatest.common.binary_path('extsearch/video/robot/crawling/partner_api/bin/partner_api')
    args = ['--api-urls', 'fetcher.test.api', '--cano-hosts', 'fetcher.test.host', '--proxy',
        yt_server, '--connectors', 'player/connectors_urlbase.xml', '--generators',
        'player/generators.xml', '--db-path', 'player/media.raw']

    fout, nout = tempfile.mkstemp()
    yatest.common.execute([binary] + args, stdout=fout)

    return yatest.common.canonical_file(nout)

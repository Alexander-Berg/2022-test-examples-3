from extsearch.audio.deepdive.tools.merge_tables.lib import lib

import yatest.common as yc
from mapreduce.yt.python.yt_stuff import YtConfig
import pytest


CYPRESS_DIR = 'extsearch/audio/deepdive/tools/merge_tables/tests/cypress_dir'


@pytest.fixture(scope='module')
def yt_config(request):
    return YtConfig(
        local_cypress_dir=yc.source_path(CYPRESS_DIR)
    )


def test_merge(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    done = [{'film_id': '100', 'uuid': '111-1'}]
    data_tables = {'second': '//data/second'}
    lib.merge(yt_client, 'second', done, data_tables=data_tables)
    assert not yt_client.exists('//data/second/100_111-1')
    assert yt_client.exists('//data/second/100_111-2')
    return list(yt_client.read_table('//data/second_full'))


def test_merge_several(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    data_tables = {'data': ['//data/data1', '//data/data2']}
    done = [{'film_id': '100', 'uuid': '111-2'}]
    lib.merge(yt_client, 'data', done, data_tables=data_tables)
    assert not yt_client.exists('//data/data1/111-2')
    assert not yt_client.exists('//data/data2/111-2')
    return [
        list(yt_client.read_table('//data/data1_full')),
        list(yt_client.read_table('//data/data2_full'))
    ]


def test_merge_combine_chunks(yt_stuff):
    yt_client = yt_stuff.get_yt_client()

    data_before = list(yt_client.read_table('//data/control_table'))
    data_before.sort(key=lambda x: x['uuid'])
    lib.combine_chunks(yt_client, ['//data/control_table'])
    data_after = list(yt_client.read_table('//data/control_table'))
    data_after.sort(key=lambda x: x['uuid'])
    assert data_before == data_after

    done = [{'film_id': '100', 'uuid': '111-1'}]
    data_tables = {'second': '//data/second'}
    lib.merge(yt_client, 'second', done, data_tables=data_tables, combine_chunks=True)
    assert not yt_client.exists('//data/second/100_111-1')
    assert yt_client.exists('//data/second/100_111-2')
    return list(yt_client.read_table('//data/second_full'))

import pytest
from yt.wrapper import YtClient

from crypta.lib.python.bt.yt import (
    AbstractDynamicStorage,
)


@pytest.yield_fixture(scope="session")
def local_yt():
    from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
    yt = YtStuff(config=YtConfig(wait_tablet_cell_initialization=True))
    yt.start_local_yt()
    yt_client = YtClient(proxy=yt.get_server(), token='')
    try:
        yield yt_client
    finally:
        yt.stop_local_yt()


def test_storage(local_yt):
    path = "//tmp/storage"
    import yt.yson as yson
    yt_client = local_yt

    schema = yson.to_yson_type(
        [
            dict(
                name='hash',
                type='uint64',
                expression='farm_hash(({}))'.format('key'),
                sort_order='ascending'
            ),
            dict(
                name='key',
                type='string',
                sort_order='ascending'
            ),
            dict(
                name='value',
                type='string'
            ),
        ],
        attributes=dict(unique_keys=True),
    )
    storage = AbstractDynamicStorage(yt_client, path, schema=schema)
    storage.create(n_tablets=10)
    storage.insert_rows(
        [{'key': 'key{}'.format(i), 'value': 'value{}'.format(i)} for i in range(100)]
    )
    assert yt_client.lookup_rows(path, [{'key': 'key1'}]).next()['value'] == 'value1'

    yt_client.unmount_table(path)

    storage.insert_rows([
        {'key': 'key1', 'value': 'value_new'}]
    )
    assert yt_client.lookup_rows(path, [{'key': 'key1'}]).next()['value'] == 'value_new'

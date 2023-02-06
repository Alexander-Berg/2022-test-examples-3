import pytest

from market.idx.yatf.resources.yt_stuff_resource import yt_server   # noqa

import market.idx.pylibrary.mindexer_core.yt_attribute_checks.yt_attribute_checks as yt_attribute_checks


@pytest.yield_fixture
def yt_client(yt_server):   # noqa
    yt_client = yt_server.get_yt_client()
    if not yt_client.exists('//tmp'):
        yt_client.mkdir('//tmp')

    yield yt_client

    yt_client.remove('//tmp', recursive=True)


def create_table(yt_client, yt_path, **attrs):
    schema = [{'name': 'key', 'type': 'string'}]
    attributes = {'dynamic': True, 'schema': schema}
    attributes.update(attrs)
    return yt_client.create('table', yt_path, attributes=attributes, recursive=True)


def test_check_auto_compaction_period(yt_client):
    """ Check that tables with @max_data_ttl set should also have @auto_compaction_period """
    # arrange
    create_table(yt_client, '//tmp/ttl_no_auto_compaction', max_data_ttl=100500)
    create_table(yt_client, '//tmp/no_ttl')
    create_table(yt_client, '//tmp/ttl_with_auto_compaction', max_data_ttl=100500, auto_compaction_period=100600)

    # act
    tables = yt_attribute_checks.check_auto_compaction_period(yt_client, '//tmp')

    # assert
    assert tables == ['//tmp/ttl_no_auto_compaction']


def test_check_merge_rows_on_flush(yt_client):
    """ Check that tables with atomicity=none should also have merge_rows_on_flush=true """
    # arrange
    create_table(yt_client, '//tmp/atomicity_none_no_merge_rows', atomicity='none')
    create_table(yt_client, '//tmp/atomicity_full_no_merge_rows', atomicity='full')
    create_table(yt_client, '//tmp/atomicity_default_full_no_merge_rows')
    create_table(yt_client, '//tmp/atomicity_none_merge_rows', atomicity='none', merge_rows_on_flush=True)
    create_table(yt_client, '//tmp/atomicity_none_merge_rows_false', atomicity='none', merge_rows_on_flush=False)

    # act
    tables = yt_attribute_checks.check_merge_rows_on_flush(yt_client, '//tmp')

    # assert
    assert list(sorted(tables)) == ['//tmp/atomicity_none_merge_rows_false', '//tmp/atomicity_none_no_merge_rows']


def create_node(yt_client, yt_path, compression_settings=None):
    yt_client.create('map_node', yt_path,  recursive=True)
    if compression_settings is not None:
        yt_client.set(yt_path + '/@nightly_compression_settings', compression_settings)


def test_check_pool(yt_client):
    """ Check that @nightly_compression_settigs have pool filled """
    # arrange
    nightly_compression_settings_pool = {'pool': 'indexer-testing-batch'}
    nightly_compression_settings_no_pool = {'enabled': True}

    create_node(yt_client, '//tmp/1/node_no_settings')
    create_node(yt_client, '//tmp/2/node_with_settings_wrong_pool', nightly_compression_settings_pool)
    create_node(yt_client, '//tmp/3/node_with_settings_no_pool', nightly_compression_settings_no_pool)
    create_node(yt_client, '//tmp/4/node_with_settings_with_pool', nightly_compression_settings_pool)
    # act
    tables = yt_attribute_checks.check_pool_in_nightly_compression_settings(yt_client, '//tmp')
    # assert
    assert list(sorted(tables)) == ['//tmp/3/node_with_settings_no_pool']


def test_check_all(yt_client):
    # arrange
    create_table(yt_client, '//tmp/ttl_no_auto_compaction', max_data_ttl=100500)
    create_table(yt_client, '//tmp/atomicity_none_no_merge_rows', atomicity='none')
    nightly_compression_settings = {'enabled': True}
    create_node(yt_client, '//tmp/node_with_settings_no_pool', nightly_compression_settings)

    # act
    res = yt_attribute_checks.check_all(yt_client, '//tmp')

    # assert
    expected = {
        'Missing @auto_compaction_period': ['//tmp/ttl_no_auto_compaction'],
        'Missing @merge_rows_on_flush': ['//tmp/atomicity_none_no_merge_rows'],
        'Missing pool in @nightly_compression_settings': ['//tmp/node_with_settings_no_pool']
    }
    assert res == expected


def test_check_all_ok(yt_client):
    # act
    res = yt_attribute_checks.check_all(yt_client, '//tmp')

    # assert
    assert res == {}

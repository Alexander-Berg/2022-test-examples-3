# -*- coding: utf-8 -*-
import pytest
import uuid

import yt.wrapper as yt
import mapreduce.yt.python.yt_stuff

import market.idx.pylibrary.mindexer_core.utils.utils as utils


class StubConfig(object):
    """Stub config class, only needed because object() doesn't have __dict__."""

    pass


@pytest.fixture()
def config():
    """Default config."""
    config = StubConfig()
    config.yt_proxy = 'test_yt_proxy'
    config.yt_tokenpath = 'test_yt_tokenpath'
    config.yt_home_dir = yt.ypath_join('//fake_home_dir/testing', str(uuid.uuid4()))
    config.yt_compress_chunks_tables = 'history,offers'
    return config


def get_table_names():
    return ['test_name1', 'history', 'test_name2', 'offers', 'test_name3']


def create_default_yt_snapshot(yt_stuff, config, table_names):
    """Creates default home_dir snapshot."""
    yt_client = yt_stuff.get_yt_client()

    for table_name in table_names:
        yt_client.create("map_node", yt.ypath_join(config.yt_home_dir, table_name), recursive=True)

    return yt_client


def test_set_up_suppress_nightly_merge(yt_stuff, config):
    """Check that all child nodes will have suppress_nightly_merge attribute except selected ones."""
    table_names = get_table_names()
    yt_compress_chunks_tables = frozenset(config.yt_compress_chunks_tables.split(','))

    yt_client = create_default_yt_snapshot(yt_stuff, config, table_names)

    utils.yt_attribute_for_children(
        yt_client=yt_client,
        parent_dir_path=config.yt_home_dir,
        yt_attribute_name='@suppress_nightly_merge',
        yt_attribute_value=True,
        except_children=yt_compress_chunks_tables,
    )

    for table_name in table_names:
        path_to_attribute = yt.ypath_join(config.yt_home_dir, table_name, '@suppress_nightly_merge')
        if table_name in yt_compress_chunks_tables:
            assert not yt_client.exists(path_to_attribute)
        else:
            assert yt_client.exists(path_to_attribute)
            assert yt_client.get(path_to_attribute)


def test_unset_suppress_nightly_merge(yt_stuff, config):
    """Checks that all child will NOT have suppress_nightly_merge attribute."""
    table_names = get_table_names()
    yt_compress_chunks_tables = frozenset()

    yt_client = create_default_yt_snapshot(yt_stuff, config, table_names)

    utils.yt_attribute_for_children(
        yt_client=yt_client,
        parent_dir_path=config.yt_home_dir,
        yt_attribute_name='@suppress_nightly_merge',
        yt_attribute_value=False,
        except_children=yt_compress_chunks_tables,
    )

    for table_name in table_names:
        path_to_attribute = yt.ypath_join(config.yt_home_dir, table_name, '@suppress_nightly_merge')
        assert yt_client.exists(path_to_attribute)
        assert not yt_client.get(path_to_attribute)


def test_set_up_sample_attribute_for_all_children(yt_stuff, config):
    """checks that all child nodes will have needed attribute."""
    table_names = get_table_names()
    yt_client = create_default_yt_snapshot(yt_stuff, config, table_names)

    test_attr_name = '@test_attr_name'
    test_attr_value = 'test_attr_value'

    utils.yt_attribute_for_children(
        yt_client=yt_client,
        parent_dir_path=config.yt_home_dir,
        yt_attribute_name=test_attr_name,
        yt_attribute_value=test_attr_value,
    )

    for table_name in table_names:
        path_to_attribute = yt.ypath_join(config.yt_home_dir, table_name, test_attr_name)
        assert yt_client.exists(path_to_attribute)
        assert yt_client.get(path_to_attribute) == test_attr_value

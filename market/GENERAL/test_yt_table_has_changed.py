# -*- coding: utf-8 -*-
import os
import pytest
import yt.wrapper as yt
from market.pylibrary.yt_tables_has_changed.has_changed import YtTablesHasChanged


@pytest.fixture(scope='module')
def yt_client():
    return yt.YtClient(proxy=os.environ.get("YT_PROXY"))


def check_changed(paths, state):
    yt_proxy = os.environ.get("YT_PROXY")
    has_changed = YtTablesHasChanged(yt_proxy=yt_proxy, yt_token='', tables=paths, stat_node=state)
    assert has_changed.has_changes()
    has_changed.set_change()
    assert not has_changed.has_changes()


def check_not_changed(paths, state):
    yt_proxy = os.environ.get("YT_PROXY")
    has_changed = YtTablesHasChanged(yt_proxy=yt_proxy, yt_token='', tables=paths, stat_node=state)
    assert not has_changed.has_changes()
    has_changed.set_change()
    assert not has_changed.has_changes()


def test_empty():
    state = '//state_empty'
    check_changed([], state)
    check_not_changed([], state)


def test_non_existant_tables():
    state = '//state_non_existant'
    table1 = '//ne/table1'
    table2 = '//ne/table2'

    check_changed([table1, table2], state)
    check_not_changed([table1, table2], state)
    check_not_changed([table2, table1], state)  # change table order


def test_empty_tables(yt_client):
    state = '//state_non_empty'
    table1 = '//empty/table1'
    table2 = '//empty/table2'
    table3 = '//empty/table3'

    check_changed([table1, table2], state)
    check_not_changed([table1, table2], state)
    check_not_changed([table2, table1], state)

    yt_client.create('table', table1, recursive=True)
    check_changed([table1, table2], state)
    check_not_changed([table1, table2], state)
    check_not_changed([table2, table1], state)

    yt_client.create('table', table2, recursive=True)
    check_changed([table1, table2], state)
    check_not_changed([table1, table2], state)
    check_not_changed([table2, table1], state)

    check_changed([table1], state)
    check_not_changed([table1], state)

    check_changed([table1, table2, table3], state)
    check_not_changed([table1, table2, table3], state)
    check_not_changed([table1, table3, table2], state)


def test_table_changed(yt_client):
    state = '//state_changed'
    table1 = '//changed/table1'
    table2 = '//changed/table2'

    check_changed([table1, table2], state)
    check_not_changed([table1, table2], state)

    yt_client.create('table', table1, recursive=True)
    yt_client.create('table', table2, recursive=True)

    check_changed([table1, table2], state)
    check_not_changed([table2, table1], state)

    yt_client.write_table(table1, [{'x': 1, 'y': 0}])
    check_changed([table1, table2], state)
    check_not_changed([table1, table2], state)


def test_traverse_dir(yt_client):
    state = '//state_dir'
    table1 = '//dir/dir2/table1'
    table2 = '//dir/table2'
    table3 = '//dir2/table3'

    check_changed(['//dir', '//dir2'], state)
    check_not_changed(['//dir', '//dir2'], state)
    check_not_changed(['//dir2', '//dir'], state)

    yt_client.create('table', table1, recursive=True)

    check_changed(['//dir', '//dir2'], state)
    check_not_changed(['//dir2', '//dir'], state)

    yt_client.create('table', table2, recursive=True)

    check_changed(['//dir', '//dir2'], state)
    check_not_changed(['//dir2', '//dir'], state)

    yt_client.write_table(table1, [{'x': 1, 'y': 0}])

    check_changed(['//dir', '//dir2'], state)
    check_not_changed(['//dir', '//dir2'], state)

    yt_client.create('table', table3, recursive=True)
    yt_client.write_table(table1, [{'x': 1, 'y': 0}])

    check_changed(['//dir', '//dir2'], state)
    check_not_changed(['//dir', '//dir2'], state)


def test_link(yt_client):
    state = '//state_link1'
    table1 = '//real/table1'
    table2 = '//real/table2'
    link = '//link/table'

    yt_client.create('table', table1, recursive=True)
    yt_client.link(table1, link, recursive=True)

    check_changed([link], state)
    check_not_changed([link], state)

    yt_client.link(table2, link, recursive=True, force=True)

    check_changed([link], state)
    check_not_changed([link], state)

    yt_client.create('table', table2, recursive=True)
    check_changed([link], state)
    check_not_changed([link], state)

    yt_client.write_table(table2, [{'x': 1, 'y': 0}])
    check_changed([link], state)
    check_not_changed([link], state)

    yt_client.create('table', link, recursive=True, force=True)
    yt_client.write_table(yt.TablePath(link, append=True), [{'x': 1, 'y': 0}])
    check_changed([link], state)
    check_not_changed([link], state)

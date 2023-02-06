import json
import logging
import os
import random
import string
import time
from typing import Tuple

import pytest
import yt.wrapper as yt

log = logging.getLogger(__name__)

from search.plutonium.admin.lib import realtime_config, exceptions

REALTIME_CONFIG_TABLE_SCHEMA = [
    {'name': 'Timestamp', 'type': 'uint64', 'sort_order': 'ascending'},
    {'name': 'Data', 'type': 'string'},
]


@pytest.fixture
def empty_table() -> Tuple[yt.YtClient, str]:
    yt_client = yt.YtClient(os.environ["YT_PROXY"])
    table_path = _get_random_table_path()

    _create_realtime_config_table(yt_client, table_path)

    yield yt_client, table_path


def test_update_empty_table(empty_table):
    yt_client, table = empty_table

    current_revision, current_config = realtime_config.get_last_config(yt_client, table)
    assert current_revision == 0
    assert current_config == {}
    config = {'test': 'test'}
    new_revision, new_config = realtime_config.update_config(yt_client, table, current_revision, config)
    assert new_revision == 1
    assert new_config == config
    current_revision, current_config = realtime_config.get_last_config(yt_client, table)
    assert current_revision == new_revision
    assert current_config == config


@pytest.fixture
def not_empty_table() -> Tuple[yt.YtClient, str]:
    yt_client = yt.YtClient(os.environ["YT_PROXY"])
    table_path = _get_random_table_path()

    _create_realtime_config_table(yt_client, table_path)

    yt_client.insert_rows(table_path, [{'Timestamp': 22, 'Data': json.dumps({'test': 'old_config'})}])

    yield yt_client, table_path


def test_update_nonempty_table(not_empty_table):
    yt_client, table = not_empty_table

    current_revision, current_config = realtime_config.get_last_config(yt_client, table)
    assert current_revision != 0
    assert current_config

    config = {'foo': 'bar'}
    new_revision, new_config = realtime_config.update_config(yt_client, table, current_revision, config)

    assert new_revision != current_revision
    assert new_config == config

    current_revision, current_config = realtime_config.get_last_config(yt_client, table)
    assert current_revision == new_revision
    assert current_config == config


def test_race(not_empty_table):
    yt_client, table = not_empty_table

    current_revision, current_config = realtime_config.get_last_config(yt_client, table)

    realtime_config.update_config(yt_client, table, current_revision, {'foo': 'bar'})
    with pytest.raises(exceptions.RealtimeConfigException):
        realtime_config.update_config(yt_client, table, current_revision, {'fizz': 'buzz'})


def _create_realtime_config_table(yt_client, table_path):
    table_attr = {'dynamic': True, 'schema': REALTIME_CONFIG_TABLE_SCHEMA}
    yt_client.create(type='table', path=table_path, attributes=table_attr)
    while yt_client.get(table_path + '/@tablet_state') != 'mounted':
        yt_client.mount_table(table_path)
        log.debug('waiting for %s to mount', table_path)
        time.sleep(1)
    log.debug('Table %s mounted', table_path)


def _get_random_table_path(k=6):
    return '//' + ''.join(random.choices(string.ascii_letters, k=k))

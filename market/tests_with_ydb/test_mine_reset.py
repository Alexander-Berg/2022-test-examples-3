# coding: utf-8
from hamcrest import assert_that, has_items, is_not, has_entries
import pytest

from market.idx.datacamp.routines.lib.tasks.sender_to_miner import yt_table_state_path
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.resources.yt_states_table_mock import YtStatesTableMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.yatf.matchers.env_matchers import IsSerializedJson


ROWS = [
    {
        'key': 1,
        'state': '{"mbi_params_dramatically_changed": 1.0}'
    },
    {
        'key': 2,
        'state': '{"force": "true"}'
    },
    {
        'key': 3,
        'state': '{"mbi_params_dramatically_changed": 1.0, "force": "true"}'
    }
]


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff):
    return RoutinesConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config={})


@pytest.fixture(scope='module')
def states_table(yt_server, config):
    tablepath = yt_table_state_path(config)
    return YtStatesTableMock(yt_server,
                             tablepath,
                             data=ROWS)


@pytest.yield_fixture(scope='module')
def sender_to_miner(yt_server, config, states_table):
    resources = {
        'config': config,
        'states_table': states_table
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as miner_env:
        miner_env.verify()
        yield miner_env


def test_reset_force(sender_to_miner, states_table):
    states_table.update(ROWS)

    sender_to_miner.post('/reset_mine?type=force&full')

    states_table.load()
    assert_that(states_table.data, is_not(has_items(*[
        has_entries({
            'key': 2,
            'state': IsSerializedJson({'force': 'true'})
        }),
        has_entries({
            'key': 3,
            'state': IsSerializedJson({'force': 'true'})
        }),
    ])))


def test_reset_mbi(sender_to_miner, states_table):
    states_table.update(ROWS)

    sender_to_miner.post('/reset_mine?type=mbi_params&full')

    states_table.load()
    assert_that(states_table.data, is_not(has_items(*[
        has_entries({
            'key': 1,
            'state': IsSerializedJson({"mbi_params_dramatically_changed": 1.0})
        }),
        has_entries({
            'key': 3,
            'state': IsSerializedJson({"mbi_params_dramatically_changed": 1.0})
        }),
    ])))


def test_reset_all(sender_to_miner, states_table):
    states_table.update(ROWS)

    sender_to_miner.post('/reset_mine?type=all&full')

    states_table.load()
    assert_that(states_table.data, is_not(has_items(*[
        has_entries({
            'key': 1,
            'state': IsSerializedJson({"mbi_params_dramatically_changed": 1.0})
        }),
        has_entries({
            'key': 2,
            'state': IsSerializedJson({"force": "true"})
        }),
        has_entries({
            'key': 3,
            'state': IsSerializedJson({"mbi_params_dramatically_changed": 1.0, "force": "true"})
        }),
    ])))


def test_reset_all_with_business_id(sender_to_miner, states_table):
    states_table.update(ROWS)

    sender_to_miner.post('/reset_mine?type=all&business_id=3')

    states_table.load()
    assert_that(states_table.data, is_not(has_items(*[
        has_entries({
            'key': 3,
            'state': IsSerializedJson({"mbi_params_dramatically_changed": 1.0, "force": "true"})
        }),
    ])))

    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 1,
            'state': IsSerializedJson({"mbi_params_dramatically_changed": 1.0})
        }),
        has_entries({
            'key': 2,
            'state': IsSerializedJson({"force": "true"})
        })
    ]))

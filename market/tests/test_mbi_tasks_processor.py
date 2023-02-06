# coding: utf-8
import pytest
import time

from datetime import datetime, timedelta
from hamcrest import assert_that, has_entries, has_items

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.TechCommands_pb2 import TechCommand, DatacampTechCommandType, TechCommandParams
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import MbiTasksProcessorEnv
from market.idx.datacamp.routines.lib.tasks.sender_to_miner import yt_table_state_path
from market.idx.datacamp.routines.yatf.resources.yt_states_table_mock import YtStatesTableMock
from market.idx.yatf.matchers.env_matchers import IsSerializedJson
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json

time_pattern = "%Y-%m-%dT%H:%M:%SZ"

NOW = datetime.utcnow()
UNTOUCHABLE_BUSINESS_ID = 6


@pytest.fixture(scope='module')
def mbi_tasks_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def states_table(yt_server, config):
    tablepath = yt_table_state_path(config)
    return YtStatesTableMock(yt_server, tablepath, data=[
        {
            'key': UNTOUCHABLE_BUSINESS_ID,
            'state': '{{"force": "false", "last_touch_time": {touch_time}}}'.format(touch_time=time.mktime(NOW.timetuple()))
        }
    ])


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, mbi_tasks_topic):
    cfg = {
        'tasks_from_mbi': {
            'topic': mbi_tasks_topic.topic
        }
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture(scope='module')
def workflow(yt_stuff, config, mbi_tasks_topic, states_table):
    resources = {
        'config': config,
        'mbi_tasks_topic': mbi_tasks_topic,
        'states_table': states_table
    }
    with MbiTasksProcessorEnv(yt_stuff, **resources) as env:
        yield env


@pytest.fixture()
def commands_processed(workflow):
    return workflow.processor.commands_processed


def test_mark_for_mining(workflow, mbi_tasks_topic, states_table, commands_processed):
    business_id = 5

    dm = DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json(NOW.strftime(time_pattern)),
                command_type=DatacampTechCommandType.FORCE_MINE_FOR_SHOP,
                command_params=TechCommandParams(
                    business_id=business_id
                )
            )
        ]
    )
    mbi_tasks_topic.write(dm.SerializeToString())

    wait_until(lambda: workflow.processor.commands_processed >= commands_processed + 1)

    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': business_id,
            'state': IsSerializedJson({
                'force': 'true'
            })
        }),
    ]))


def test_do_not_mine_recenty_mined_shop(workflow, mbi_tasks_topic, states_table, commands_processed):
    dm = DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json((NOW - timedelta(hours=12)).strftime(time_pattern)),
                command_type=DatacampTechCommandType.FORCE_MINE_FOR_SHOP,
                command_params=TechCommandParams(
                    shop_id=UNTOUCHABLE_BUSINESS_ID
                )
            )
        ]
    )
    mbi_tasks_topic.write(dm.SerializeToString())

    wait_until(lambda: workflow.processor.commands_processed >= commands_processed + 1)

    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': UNTOUCHABLE_BUSINESS_ID,
            'state': IsSerializedJson({
                'force': 'false'
            })
        }),
    ]))

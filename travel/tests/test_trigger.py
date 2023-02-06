# -*- coding: utf-8 -*-

from dataclasses import dataclass, replace
from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import DefaultDict, Helper

from travel.hotels.content_manager.config.stage_config import ClusterizationStageConfig, WLGetHotelInfoStageConfig
from travel.hotels.content_manager.data_model.options import (
    NirvanaWorkflowOptions, StageOptions, TolokaPoolOptions, TriggerOptions
)
from travel.hotels.content_manager.data_model.stage import ClusterizationData
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageHotelWL, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import EXCEPTIONAL_ENTITY_PRIORITY, uint
from travel.hotels.content_manager.lib.common import dc_from_dict
from travel.hotels.content_manager.lib.trigger import EntityGroupingKey


START_TS = int(datetime(2019, 10, 25).timestamp())


def test_nothing_to_process(helper: Helper):
    hotels_wl = [
        StorageHotelWL(
            permalink=uint(0),
            partner_id='partner_id_0',
            original_id='original_id_0',
            status_wl_get_hotel_info=StageStatus.TO_BE_PROCESSED,
            status_wl_get_hotel_info_ts=uint(START_TS + 1),
        ),
        StorageHotelWL(
            permalink=uint(0),
            partner_id='partner_id_1',
            original_id='original_id_1',
            status_wl_get_hotel_info=StageStatus.TO_BE_PROCESSED,
            status_wl_get_hotel_info_ts=uint(START_TS + 1),
        ),
        StorageHotelWL(
            permalink=uint(1),
            partner_id='partner_id_2',
            original_id='original_id_2',
            status_wl_get_hotel_info=StageStatus.TO_BE_PROCESSED,
            status_wl_get_hotel_info_ts=uint(START_TS + 1),
        ),
    ]

    permalinks_wl = [
        StoragePermalinkWL(
            permalink=uint(0),
        ),
        StoragePermalinkWL(
            permalink=uint(1),
        ),
    ]

    trigger = helper.get_trigger(WLGetHotelInfoStageConfig, start_ts=START_TS)
    helper.prepare_storage(hotels_wl=hotels_wl, permalinks_wl=permalinks_wl)

    trigger.process()

    assert not trigger.persistence_manager.upstream_changed


def test_total_priority(helper: Helper):
    trigger_options = TriggerOptions(
        workflow_options=NirvanaWorkflowOptions(
            singleProjectId='0',
            singlePoolId='0',
        ),
        pool_options=TolokaPoolOptions(
            reward_per_assignment=0,
        ),
    )
    options = StageOptions(triggers=DefaultDict(trigger_options))

    permalinks_wl = [
        StoragePermalinkWL(
            permalink=uint(0),
            priority=uint(1),
            grouping_key='grouping_key_1',
            clusterization_iteration=uint(2),
            status_clusterization=StageStatus.TO_BE_PROCESSED,
        ),
        StoragePermalinkWL(
            permalink=uint(1),
            priority=uint(1),
            grouping_key='grouping_key_1',
            clusterization_iteration=uint(2),
            status_clusterization=StageStatus.TO_BE_PROCESSED,
        ),
        StoragePermalinkWL(
            permalink=uint(2),
            priority=uint(2),
            grouping_key='grouping_key_2',
            clusterization_iteration=uint(1),
            status_clusterization=StageStatus.TO_BE_PROCESSED,
        ),
        StoragePermalinkWL(
            permalink=uint(3),
            priority=uint(2),
            grouping_key='grouping_key_2',
            clusterization_iteration=uint(1),
            status_clusterization=StageStatus.TO_BE_PROCESSED,
        ),
        StoragePermalinkWL(
            permalink=uint(4),
            priority=uint(3),
            grouping_key='grouping_key_3',
            clusterization_iteration=uint(1),
            status_clusterization=StageStatus.TO_BE_PROCESSED,
        ),
        StoragePermalinkWL(
            permalink=uint(5),
            priority=EXCEPTIONAL_ENTITY_PRIORITY,
            grouping_key='grouping_key_4',
            clusterization_iteration=uint(1),
            status_clusterization=StageStatus.TO_BE_PROCESSED,
        ),
    ]
    helper.prepare_storage(permalinks_wl=permalinks_wl)

    stage_config = replace(ClusterizationStageConfig, jobs_max=2)
    trigger = helper.get_trigger(stage_config, start_ts=START_TS, options=options)
    trigger.process()

    # trigger data
    assert 3 == len(helper.persistence_manager.list(trigger.stage_input_path))

    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '0', 'hotels')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 1 == len(data)

    rec: ClusterizationData = dc_from_dict(ClusterizationData, data[0])
    assert '5' == rec.input.permalink

    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '1', 'hotels')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 1 == len(data)

    rec: ClusterizationData = dc_from_dict(ClusterizationData, data[0])
    assert '4' == rec.input.permalink

    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '2', 'hotels')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 2 == len(data)

    # permalink 0 info
    rec: ClusterizationData = dc_from_dict(ClusterizationData, data[0])
    assert '2' == rec.input.permalink

    # permalink 1 info
    rec: ClusterizationData = dc_from_dict(ClusterizationData, data[1])
    assert '3' == rec.input.permalink

    helper.persistence_manager.delete(trigger.stage_input_path)


def test_priority_grouping_key():
    keys = [
        EntityGroupingKey(uint(1), 'group_1'),
        EntityGroupingKey(uint(2), 'group_1'),
        EntityGroupingKey(uint(3), 'group_2'),
        EntityGroupingKey(uint(4), 'group_2'),
    ]
    exp = [
        EntityGroupingKey(uint(4), 'group_2'),
        EntityGroupingKey(uint(3), 'group_2'),
        EntityGroupingKey(uint(2), 'group_1'),
        EntityGroupingKey(uint(1), 'group_1'),
    ]
    assert exp == sorted(keys, reverse=True)

    keys = [
        EntityGroupingKey(uint(1), 'group_1'),
        EntityGroupingKey(uint(2), 'group_2'),
        EntityGroupingKey(uint(3), 'group_1'),
        EntityGroupingKey(uint(4), 'group_2'),
    ]
    exp = [
        EntityGroupingKey(uint(4), 'group_2'),
        EntityGroupingKey(uint(3), 'group_1'),
        EntityGroupingKey(uint(2), 'group_2'),
        EntityGroupingKey(uint(1), 'group_1'),
    ]
    assert exp == sorted(keys, reverse=True)

    keys = [
        EntityGroupingKey(uint(2), 'group_2'),
        EntityGroupingKey(uint(1), 'group_1'),
    ]
    exp = [
        EntityGroupingKey(uint(2), 'group_2'),
        EntityGroupingKey(uint(1), 'group_1'),
    ]
    assert exp == sorted(keys, reverse=True)

    keys = [
        EntityGroupingKey(uint(1), 'group_4'),
        EntityGroupingKey(uint(1), 'group_2'),
        EntityGroupingKey(uint(1), 'group_3'),
        EntityGroupingKey(uint(1), 'group_1'),
    ]
    exp = [
        EntityGroupingKey(uint(1), 'group_4'),
        EntityGroupingKey(uint(1), 'group_3'),
        EntityGroupingKey(uint(1), 'group_2'),
        EntityGroupingKey(uint(1), 'group_1'),
    ]
    assert exp == sorted(keys, reverse=True)


def test_get_batches(helper: Helper):
    @dataclass(order=True)
    class Entity:
        ts: int

    entities = [
        Entity(11),
        Entity(10),
        Entity(9),
        Entity(8),
        Entity(7),
        Entity(6),
        Entity(5),
        Entity(4),
        Entity(3),
        Entity(2),
        Entity(1),
        Entity(0),
    ]
    sorted_entities = sorted(entities)
    batch_size = 7
    options = StageOptions(triggers=dict(), job_batching_delay_max=5)

    trigger = helper.get_trigger(WLGetHotelInfoStageConfig, start_ts=10, options=options)
    batches = trigger.get_batches(entities, batch_size, 'ts', options.job_batching_delay_max)
    batches = list(batches)
    assert [sorted_entities[:batch_size]] == batches

    trigger = helper.get_trigger(WLGetHotelInfoStageConfig, start_ts=15, options=options)
    batches = trigger.get_batches(entities, batch_size, 'ts', options.job_batching_delay_max)
    batches = list(batches)
    assert [sorted_entities[:batch_size], sorted_entities[batch_size:]] == batches

# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import WLClusterizedHotelsStageConfig
from travel.hotels.content_manager.data_model.stage import WLGetHotelInfoPermalinkData
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import StageResult, uint
from travel.hotels.content_manager.lib.common import dc_from_dict


START_TS = int(datetime(2019, 10, 25).timestamp())

PERMALINKS_WL = [
    StoragePermalinkWL(
        permalink=uint(0),
        clusterization_result=StageResult.SUCCESS,
        status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED
    ),
    StoragePermalinkWL(
        permalink=uint(1),
        clusterization_result=StageResult.SUCCESS,
        status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED
    ),
    StoragePermalinkWL(
        permalink=uint(2),
        finished_stages='actualization,clusterization',
        actualization_result=StageResult.FAILED,
        status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED
    ),
    StoragePermalinkWL(
        permalink=uint(3),
        finished_stages='actualization,clusterization',
        clusterization_result=StageResult.FAILED,
        status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED
    ),
]


def test_run(helper: Helper):
    trigger = helper.get_trigger(WLClusterizedHotelsStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks_wl=PERMALINKS_WL)

    success_permalinks_table = helper.persistence_manager.join(trigger.stage_input_path, '0', 'permalinks')
    failed_permalinks_table = helper.persistence_manager.join(trigger.stage_input_path, '0', 'failed_permalinks')

    trigger.process()

    # trigger data
    # success permalinks
    data = list(helper.persistence_manager.read(success_permalinks_table))

    assert 2 == len(data)

    permalink: WLGetHotelInfoPermalinkData = dc_from_dict(WLGetHotelInfoPermalinkData, data[0])
    assert 0 == permalink.permalink

    permalink: WLGetHotelInfoPermalinkData = dc_from_dict(WLGetHotelInfoPermalinkData, data[1])
    assert 1 == permalink.permalink

    # failed permalinks
    data = list(helper.persistence_manager.read(failed_permalinks_table))

    assert 2 == len(data)

    permalink: WLGetHotelInfoPermalinkData = dc_from_dict(WLGetHotelInfoPermalinkData, data[0])
    assert 2 == permalink.permalink

    permalink: WLGetHotelInfoPermalinkData = dc_from_dict(WLGetHotelInfoPermalinkData, data[1])
    assert 3 == permalink.permalink

    # storage data
    storage = helper.get_storage()

    # storage_permalink 0
    storage_permalink: StoragePermalinkWL = storage.permalinks_wl[0]
    assert StageStatus.IN_PROCESS == storage_permalink.status_wl_clusterized_hotels
    assert START_TS == storage_permalink.status_wl_clusterized_hotels_ts

    # storage_permalink 1
    storage_permalink: StoragePermalinkWL = storage.permalinks_wl[1]
    assert StageStatus.IN_PROCESS == storage_permalink.status_wl_clusterized_hotels
    assert START_TS == storage_permalink.status_wl_clusterized_hotels_ts

    # storage_permalink 2
    storage_permalink: StoragePermalinkWL = storage.permalinks_wl[2]
    assert StageStatus.IN_PROCESS == storage_permalink.status_wl_clusterized_hotels
    assert START_TS == storage_permalink.status_wl_clusterized_hotels_ts

    # storage_permalink 3
    storage_permalink: StoragePermalinkWL = storage.permalinks_wl[3]
    assert StageStatus.IN_PROCESS == storage_permalink.status_wl_clusterized_hotels
    assert START_TS == storage_permalink.status_wl_clusterized_hotels_ts

# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import WLClusterizedHotelsStageConfig
from travel.hotels.content_manager.data_model.stage import WLNewHotelsHotelData, WLNewHotelsPermalinkData
from travel.hotels.content_manager.data_model.storage import StageStatus
from travel.hotels.content_manager.data_model.types import StageResult, WLResult, uint
from travel.hotels.content_manager.lib.common import dc_to_dict, get_dc_yt_schema
from travel.hotels.content_manager.lib.storage import HotelWLKey, StorageHotelWL, StoragePermalinkWL


START_TS = int(datetime(2019, 10, 25).timestamp())

STORAGE_PERMALINKS_WL = [
    StoragePermalinkWL(
        permalink=uint(0),
    ),
    StoragePermalinkWL(
        permalink=uint(1),
    ),
    StoragePermalinkWL(
        permalink=uint(2),
    ),
    StoragePermalinkWL(
        permalink=uint(3),
    ),
    StoragePermalinkWL(
        permalink=uint(4),
        finished_stages='actualization',
        actualization_result=StageResult.FAILED,
    ),
    StoragePermalinkWL(
        permalink=uint(5),
        finished_stages='actualization,clusterization',
        clusterization_result=StageResult.FAILED,
    ),
]

STORAGE_HOTELS_WL = [
    StorageHotelWL(
        permalink=uint(0),
        partner_id='partner_id_0',
        original_id='original_id_0',
        grouping_key='group_0',
        is_wl_approved=False,
    ),
    StorageHotelWL(
        permalink=uint(1),
        partner_id='partner_id_0',
        original_id='original_id_0',
        grouping_key='group_1',
        is_wl_approved=False,
    ),
    StorageHotelWL(
        permalink=uint(2),
        partner_id='partner_id_0',
        original_id='original_id_0',
        grouping_key='group_1',
        is_wl_approved=True,
    ),
    StorageHotelWL(
        permalink=uint(3),
        partner_id='partner_id_0',
        original_id='original_id_0',
        grouping_key='group_1',
        is_wl_approved=True,
    ),
    StorageHotelWL(
        permalink=uint(4),
        partner_id='partner_id_0',
        original_id='original_id_0',
        grouping_key='group_1',
        is_wl_approved=True,
    ),
    StorageHotelWL(
        permalink=uint(5),
        partner_id='partner_id_0',
        original_id='original_id_0',
        grouping_key='group_1',
        is_wl_approved=True,
    ),
]

STAGE_SUCCESS_PERMALINKS_WL = [
    WLNewHotelsPermalinkData(
        permalink=uint(0),
    ),
    WLNewHotelsPermalinkData(
        permalink=uint(1),
    ),
    WLNewHotelsPermalinkData(
        permalink=uint(2),
    ),
    WLNewHotelsPermalinkData(
        permalink=uint(3),
    ),
]

STAGE_FAILED_PERMALINKS_WL = [
    WLNewHotelsPermalinkData(
        permalink=uint(3),
    ),
    WLNewHotelsPermalinkData(
        permalink=uint(4),
    ),
    WLNewHotelsPermalinkData(
        permalink=uint(5),
    ),
]

STAGE_HOTELS_WL = [
    WLNewHotelsHotelData(
        permalink=uint(0),
        partner_id='partner_id_0',
        original_id='original_id_0',
        grouping_key='group_0',
    ),
    WLNewHotelsHotelData(
        permalink=uint(1),
        partner_id='partner_id_0',
        original_id='original_id_0',
        grouping_key='group_1',
    ),
    WLNewHotelsHotelData(
        permalink=uint(1),
        partner_id='partner_id_1',
        original_id='original_id_1',
        grouping_key='group_1',
    ),
    WLNewHotelsHotelData(
        permalink=uint(2),
        partner_id='partner_id_1',
        original_id='original_id_1',
        grouping_key='group_1',
    ),
]


def test_run(helper: Helper):
    updater = helper.get_updater(WLClusterizedHotelsStageConfig, start_ts=START_TS)
    updater_args = helper.get_updater_args(WLClusterizedHotelsStageConfig.name)
    output_path, _ = updater_args
    success_permalinks_table = helper.persistence_manager.join(output_path, 'permalinks')
    failed_permalinks_table = helper.persistence_manager.join(output_path, 'failed_permalinks')
    hotels_table = helper.persistence_manager.join(output_path, 'hotels')

    schema = get_dc_yt_schema(WLNewHotelsPermalinkData)
    success_permalinks = (dc_to_dict(p) for p in STAGE_SUCCESS_PERMALINKS_WL)
    helper.persistence_manager.write(success_permalinks_table, success_permalinks, schema)

    failed_permalinks = (dc_to_dict(p) for p in STAGE_FAILED_PERMALINKS_WL)
    helper.persistence_manager.write(failed_permalinks_table, failed_permalinks, schema)

    hotels_wl = (dc_to_dict(p) for p in STAGE_HOTELS_WL)
    helper.persistence_manager.write(hotels_table, hotels_wl, get_dc_yt_schema(WLNewHotelsHotelData))

    helper.prepare_storage(permalinks_wl=STORAGE_PERMALINKS_WL, hotels_wl=STORAGE_HOTELS_WL)

    updater.run(*updater_args)

    storage = helper.get_storage()

    assert 6 == len(storage.permalinks_wl)

    permalink: StoragePermalinkWL = storage.permalinks_wl[0]
    assert WLResult.SUCCESS == permalink.wl_result
    assert StageStatus.NOTHING_TO_DO == permalink.status_wl_clusterized_hotels
    assert START_TS == permalink.status_wl_clusterized_hotels_ts

    permalink: StoragePermalinkWL = storage.permalinks_wl[1]
    assert WLResult.SUCCESS == permalink.wl_result
    assert StageStatus.NOTHING_TO_DO == permalink.status_wl_clusterized_hotels
    assert START_TS == permalink.status_wl_clusterized_hotels_ts

    permalink: StoragePermalinkWL = storage.permalinks_wl[2]
    assert WLResult.SUCCESS == permalink.wl_result
    assert StageStatus.NOTHING_TO_DO == permalink.status_wl_clusterized_hotels
    assert START_TS == permalink.status_wl_clusterized_hotels_ts

    permalink: StoragePermalinkWL = storage.permalinks_wl[3]
    assert WLResult.SUCCESS == permalink.wl_result
    assert StageStatus.NOTHING_TO_DO == permalink.status_wl_clusterized_hotels
    assert START_TS == permalink.status_wl_clusterized_hotels_ts

    permalink: StoragePermalinkWL = storage.permalinks_wl[4]
    assert WLResult.FAILED == permalink.wl_result
    assert StageStatus.NOTHING_TO_DO == permalink.status_wl_clusterized_hotels
    assert START_TS == permalink.status_wl_clusterized_hotels_ts

    permalink: StoragePermalinkWL = storage.permalinks_wl[5]
    assert WLResult.FAILED == permalink.wl_result
    assert StageStatus.NOTHING_TO_DO == permalink.status_wl_clusterized_hotels
    assert START_TS == permalink.status_wl_clusterized_hotels_ts

    assert 8 == len(storage.hotels_wl)

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_0', 'original_id_0')]
    assert hotel.is_wl_approved
    assert 'group_0' == hotel.grouping_key

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(1), 'partner_id_0', 'original_id_0')]
    assert hotel.is_wl_approved
    assert 'group_1' == hotel.grouping_key

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(1), 'partner_id_1', 'original_id_1')]
    assert hotel.is_wl_approved
    assert 'group_1' == hotel.grouping_key

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(2), 'partner_id_0', 'original_id_0')]
    assert not hotel.is_wl_approved
    assert 'group_1' == hotel.grouping_key

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(2), 'partner_id_1', 'original_id_1')]
    assert hotel.is_wl_approved
    assert 'group_1' == hotel.grouping_key

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(3), 'partner_id_0', 'original_id_0')]
    assert not hotel.is_wl_approved
    assert 'group_1' == hotel.grouping_key

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(4), 'partner_id_0', 'original_id_0')]
    assert not hotel.is_wl_approved
    assert 'group_1' == hotel.grouping_key

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(5), 'partner_id_0', 'original_id_0')]
    assert not hotel.is_wl_approved
    assert 'group_1' == hotel.grouping_key

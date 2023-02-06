# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import WLNewHotelsStageConfig
from travel.hotels.content_manager.data_model.stage import WLNewHotelsHotelData
from travel.hotels.content_manager.data_model.storage import StageStatus
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_to_dict, get_dc_yt_schema
from travel.hotels.content_manager.lib.storage import HotelWLKey, StorageHotelWL, StoragePermalinkWL


START_TS = int(datetime(2019, 10, 25).timestamp())

STORAGE_HOTELS_WL = [
    StorageHotelWL(
        permalink=uint(0),
        partner_id='partner_id_0',
        original_id='original_id_0',
        is_wl_approved=True,
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
        permalink=uint(0),
        partner_id='partner_id_1',
        original_id='original_id_1',
        grouping_key='group_1',
    ),
]


def test_run(helper: Helper):
    updater = helper.get_updater(WLNewHotelsStageConfig, start_ts=START_TS)
    updater_args = helper.get_updater_args(WLNewHotelsStageConfig.name)
    output_path, _ = updater_args
    hotels_table = helper.persistence_manager.join(output_path, 'hotels')

    hotels_wl = (dc_to_dict(p) for p in STAGE_HOTELS_WL)
    helper.persistence_manager.write(hotels_table, hotels_wl, get_dc_yt_schema(WLNewHotelsHotelData))

    helper.prepare_storage(hotels_wl=STORAGE_HOTELS_WL)

    updater.run(*updater_args)

    storage = helper.get_storage()

    assert 2 == len(storage.hotels_wl)

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_0', 'original_id_0')]
    assert 'partner_id_0' == hotel.partner_id
    assert 'original_id_0' == hotel.original_id
    assert 'group_0' == hotel.grouping_key
    assert hotel.is_wl_approved
    assert StageStatus.TO_BE_PROCESSED == hotel.status_wl_get_hotel_info
    assert START_TS == hotel.status_wl_get_hotel_info_ts

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_1', 'original_id_1')]
    assert 'partner_id_1' == hotel.partner_id
    assert 'original_id_1' == hotel.original_id
    assert 'group_1' == hotel.grouping_key
    assert not hotel.is_wl_approved
    assert StageStatus.TO_BE_PROCESSED == hotel.status_wl_get_hotel_info
    assert START_TS == hotel.status_wl_get_hotel_info_ts

    assert 1 == len(storage.permalinks_wl)

    permalink: StoragePermalinkWL = storage.permalinks_wl[0]
    assert '' == permalink.address

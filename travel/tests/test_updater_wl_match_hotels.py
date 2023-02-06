# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import WLMatchHotelsStageConfig
from travel.hotels.content_manager.data_model.stage import (
    WLMatchHotelsData, WLMatchHotelsDataInput, WLMatchHotelsDataOutput
)
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
        status_wl_match_hotels=StageStatus.IN_PROCESS,
    ),
    StorageHotelWL(
        permalink=uint(0),
        partner_id='partner_id_1',
        original_id='original_id_1',
        status_wl_match_hotels=StageStatus.IN_PROCESS,
    ),
]

STORAGE_PERMALINKS_WL = [
    StoragePermalinkWL(
        permalink=uint(0),
    ),
]

PROCESSOR_RESULT = [
    WLMatchHotelsData(
        input=WLMatchHotelsDataInput(
            permalink='0',
            partner_id='partner_id_0',
            original_id='original_id_0',
        ),
        output=WLMatchHotelsDataOutput(
            result='double',
        ),
    ),
    WLMatchHotelsData(
        input=WLMatchHotelsDataInput(
            permalink='0',
            partner_id='partner_id_1',
            original_id='original_id_1',
        ),
        output=WLMatchHotelsDataOutput(
            result='no_double',
        ),
    ),
]


def test_run(helper: Helper):
    updater = helper.get_updater(WLMatchHotelsStageConfig, start_ts=START_TS)
    updater_args = helper.get_updater_args(WLMatchHotelsStageConfig.name)
    output_path, _ = updater_args

    hotels_table = helper.persistence_manager.join(output_path, 'hotels')
    hotels_wl = (dc_to_dict(p) for p in PROCESSOR_RESULT)
    helper.persistence_manager.write(hotels_table, hotels_wl, get_dc_yt_schema(WLMatchHotelsData))

    helper.prepare_storage(hotels_wl=STORAGE_HOTELS_WL, permalinks_wl=STORAGE_PERMALINKS_WL)

    updater.run(*updater_args)

    storage = helper.get_storage()

    # storage
    assert 2 == len(storage.hotels_wl)

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_0', 'original_id_0')]
    assert 'partner_id_0' == hotel.partner_id
    assert 'original_id_0' == hotel.original_id
    assert hotel.is_wl_approved
    assert StageStatus.NOTHING_TO_DO == hotel.status_wl_match_hotels
    assert START_TS == hotel.status_wl_match_hotels_ts

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_1', 'original_id_1')]
    assert 'partner_id_1' == hotel.partner_id
    assert 'original_id_1' == hotel.original_id
    assert not hotel.is_wl_approved
    assert StageStatus.NOTHING_TO_DO == hotel.status_wl_match_hotels
    assert START_TS == hotel.status_wl_match_hotels_ts

# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import WLGetHotelInfoStageConfig
from travel.hotels.content_manager.data_model.stage import WLGetHotelInfoHotelData, WLGetHotelInfoPermalinkData
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageHotelWL, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict
from travel.hotels.content_manager.lib.storage import HotelWLKey


START_TS = int(datetime(2019, 10, 25).timestamp())

HOTELS_WL = [
    StorageHotelWL(
        permalink=uint(0),
        partner_id='partner_id_0',
        original_id='original_id_0',
        status_wl_get_hotel_info=StageStatus.TO_BE_PROCESSED,
    ),
    StorageHotelWL(
        permalink=uint(0),
        partner_id='partner_id_1',
        original_id='original_id_1',
        status_wl_get_hotel_info=StageStatus.TO_BE_PROCESSED,
    ),
    StorageHotelWL(
        permalink=uint(1),
        partner_id='partner_id_2',
        original_id='original_id_2',
        status_wl_get_hotel_info=StageStatus.TO_BE_PROCESSED,
    ),
]

PERMALINKS_WL = [
    StoragePermalinkWL(
        permalink=uint(0),
    ),
    StoragePermalinkWL(
        permalink=uint(1),
    ),
]


def test_run(helper: Helper):
    trigger = helper.get_trigger(WLGetHotelInfoStageConfig, start_ts=START_TS)
    helper.prepare_storage(hotels_wl=HOTELS_WL, permalinks_wl=PERMALINKS_WL)

    hotels_table = helper.persistence_manager.join(trigger.stage_input_path, '0', 'hotels')
    permalinks_table = helper.persistence_manager.join(trigger.stage_input_path, '0', 'permalinks')

    trigger.process()

    # trigger data
    # hotels
    data = list(helper.persistence_manager.read(hotels_table))

    assert 3 == len(data)

    hotel: WLGetHotelInfoHotelData = dc_from_dict(WLGetHotelInfoHotelData, data[0])
    assert 0 == hotel.permalink
    assert 'partner_id_0' == hotel.partner_id
    assert 'original_id_0' == hotel.original_id

    hotel: WLGetHotelInfoHotelData = dc_from_dict(WLGetHotelInfoHotelData, data[1])
    assert 0 == hotel.permalink
    assert 'partner_id_1' == hotel.partner_id
    assert 'original_id_1' == hotel.original_id

    hotel: WLGetHotelInfoHotelData = dc_from_dict(WLGetHotelInfoHotelData, data[2])
    assert 1 == hotel.permalink
    assert 'partner_id_2' == hotel.partner_id
    assert 'original_id_2' == hotel.original_id

    # permalinks
    data = list(helper.persistence_manager.read(permalinks_table))

    assert 2 == len(data)

    permalink: WLGetHotelInfoPermalinkData = dc_from_dict(WLGetHotelInfoPermalinkData, data[0])
    assert 0 == permalink.permalink

    permalink: WLGetHotelInfoPermalinkData = dc_from_dict(WLGetHotelInfoPermalinkData, data[1])
    assert 1 == permalink.permalink

    # storage data
    storage = helper.get_storage()

    # hotels
    assert 3 == len(storage.hotels_wl)

    # hotel 0
    storage_hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_0', 'original_id_0')]
    assert StageStatus.IN_PROCESS == storage_hotel.status_wl_get_hotel_info
    assert START_TS == storage_hotel.status_wl_get_hotel_info_ts

    # hotel 1
    storage_hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_1', 'original_id_1')]
    assert StageStatus.IN_PROCESS == storage_hotel.status_wl_get_hotel_info
    assert START_TS == storage_hotel.status_wl_get_hotel_info_ts

    # hotel 2
    storage_hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(1), 'partner_id_2', 'original_id_2')]
    assert StageStatus.IN_PROCESS == storage_hotel.status_wl_get_hotel_info
    assert START_TS == storage_hotel.status_wl_get_hotel_info_ts

    assert 2 == len(storage.permalinks_wl)

    # storage_permalink 0
    storage_permalink: StoragePermalinkWL = storage.permalinks_wl[0]
    assert 0 == storage_permalink.permalink

    # storage_permalink 1
    storage_permalink: StoragePermalinkWL = storage.permalinks_wl[1]
    assert 1 == storage_permalink.permalink

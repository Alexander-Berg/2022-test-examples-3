# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import WLGetHotelInfoStageConfig
from travel.hotels.content_manager.data_model.stage import WLGetHotelInfoHotelData, WLGetHotelInfoPermalinkData
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
        status_wl_get_hotel_info=StageStatus.IN_PROCESS,
    ),
    StorageHotelWL(
        permalink=uint(0),
        partner_id='partner_id_1',
        original_id='original_id_1',
        status_wl_get_hotel_info=StageStatus.IN_PROCESS,
    ),
]

STORAGE_PERMALINKS_WL = [
    StoragePermalinkWL(
        permalink=uint(0),
        grouping_key='grouping_key_0',
        hotel_name='hotel_name_0',
    ),
]

STAGE_HOTELS_WL = [
    WLGetHotelInfoHotelData(
        permalink=uint(0),
        partner_id='partner_id_0',
        original_id='original_id_0',
        address='address_0_0',
        category='category_0_0',
        company_name='company_name_0_0',
        country='country_0_0',
        latitude=0.0,
        longitude=0.0,
        phone='phone_0_0',
        photo=['photo_0_0'],
        url='url_0_0',
    ),
    WLGetHotelInfoHotelData(
        permalink=uint(0),
        partner_id='partner_id_1',
        original_id='original_id_1',
        address='address_0_1',
        category='category_0_1',
        company_name='company_name_0_1',
        country='country_0_1',
        latitude=1.0,
        longitude=1.0,
        phone='phone_0_1',
        photo=['photo_0_1'],
        url='url_0_1',
    ),
]

STAGE_PERMALINKS_WL = [
    WLGetHotelInfoPermalinkData(
        permalink=uint(0),
        address='address_0',
        category='category_0',
        company_name='company_name_0',
        country='country_0',
        latitude=2.0,
        longitude=2.0,
        phone='phone_0',
        photo=['photo_0'],
        url='url_0',
    ),
]


def test_run(helper: Helper):
    updater = helper.get_updater(WLGetHotelInfoStageConfig, start_ts=START_TS)
    updater_args = helper.get_updater_args(WLGetHotelInfoStageConfig.name)
    output_path, _ = updater_args

    hotels_table = helper.persistence_manager.join(output_path, 'hotels')
    hotels_wl = (dc_to_dict(p) for p in STAGE_HOTELS_WL)
    helper.persistence_manager.write(hotels_table, hotels_wl, get_dc_yt_schema(WLGetHotelInfoHotelData))

    permalinks_table = helper.persistence_manager.join(output_path, 'permalinks')
    permalinks_wl = (dc_to_dict(p) for p in STAGE_PERMALINKS_WL)
    helper.persistence_manager.write(permalinks_table, permalinks_wl, get_dc_yt_schema(WLGetHotelInfoPermalinkData))

    helper.prepare_storage(hotels_wl=STORAGE_HOTELS_WL, permalinks_wl=STORAGE_PERMALINKS_WL)

    updater.run(*updater_args)

    storage = helper.get_storage()

    assert 2 == len(storage.hotels_wl)

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_0', 'original_id_0')]
    assert 'partner_id_0' == hotel.partner_id
    assert 'original_id_0' == hotel.original_id
    assert 'address_0_0' == hotel.address
    assert 'category_0_0' == hotel.category
    assert 'company_name_0_0' == hotel.company_name
    assert 'country_0_0' == hotel.country
    assert 0.0 == hotel.latitude
    assert 0.0 == hotel.longitude
    assert 'phone_0_0' == hotel.phone
    assert ['photo_0_0'] == hotel.photo
    assert 'url_0_0' == hotel.url
    assert StageStatus.NOTHING_TO_DO == hotel.status_wl_get_hotel_info
    assert StageStatus.TO_BE_PROCESSED == hotel.status_wl_match_hotels
    assert START_TS == hotel.status_wl_match_hotels_ts

    hotel: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_1', 'original_id_1')]
    assert 'partner_id_1' == hotel.partner_id
    assert 'original_id_1' == hotel.original_id
    assert 'address_0_1' == hotel.address
    assert 'category_0_1' == hotel.category
    assert 'company_name_0_1' == hotel.company_name
    assert 'country_0_1' == hotel.country
    assert 1.0 == hotel.latitude
    assert 1.0 == hotel.longitude
    assert 'phone_0_1' == hotel.phone
    assert ['photo_0_1'] == hotel.photo
    assert 'url_0_1' == hotel.url
    assert StageStatus.NOTHING_TO_DO == hotel.status_wl_get_hotel_info
    assert StageStatus.TO_BE_PROCESSED == hotel.status_wl_match_hotels
    assert START_TS == hotel.status_wl_match_hotels_ts

    assert 1 == len(storage.permalinks_wl)

    permalink: StoragePermalinkWL = storage.permalinks_wl[0]
    assert 'address_0' == permalink.address
    assert 'category_0' == permalink.category
    assert 'company_name_0' == permalink.company_name
    assert 'country_0' == permalink.country
    assert 2.0 == permalink.latitude
    assert 2.0 == permalink.longitude
    assert 'phone_0' == permalink.phone
    assert ['photo_0'] == permalink.photo
    assert 'url_0' == permalink.url
    assert 'grouping_key_0' == permalink.grouping_key
    assert 'hotel_name_0' == permalink.hotel_name

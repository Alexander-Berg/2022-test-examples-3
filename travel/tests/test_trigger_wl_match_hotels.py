# -*- coding: utf-8 -*-

from dataclasses import asdict
from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import StageConfig, WLMatchHotelsStageConfig
from travel.hotels.content_manager.data_model.options import NirvanaWorkflowOptions, StageOptions, TriggerOptions
from travel.hotels.content_manager.data_model.stage import WLMatchHotelsData
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageHotelWL, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict
from travel.hotels.content_manager.lib.storage import HotelWLKey


START_TS = int(datetime(2019, 10, 25).timestamp())

HOTELS_WL = [
    StorageHotelWL(
        permalink=uint(0),
        partner_id='partner_id_0',
        original_id='original_id_0',
        latitude=45.55,
        longitude=77.27,
        photo=['photo_0_0'],
        status_wl_match_hotels=StageStatus.TO_BE_PROCESSED,
    ),
    StorageHotelWL(
        permalink=uint(0),
        partner_id='partner_id_1',
        original_id='original_id_1',
        latitude=45.56,
        longitude=77.27,
        photo=['photo_0_1'],
        status_wl_match_hotels=StageStatus.TO_BE_PROCESSED,
    ),
    StorageHotelWL(
        permalink=uint(1),
        partner_id='partner_id_2',
        original_id='original_id_2',
        latitude=45.55,
        longitude=77.26,
        photo=['photo_1_2'],
        status_wl_match_hotels=StageStatus.TO_BE_PROCESSED,
    ),
    StorageHotelWL(
        permalink=uint(1),
        partner_id='partner_id_3',
        original_id='original_id_3',
        latitude=45.54,
        longitude=77.27,
        photo=['photo_1_3'],
        status_wl_match_hotels=StageStatus.TO_BE_PROCESSED,
    ),
]

PERMALINKS_WL = [
    StoragePermalinkWL(
        permalink=uint(0),
        latitude=45.55,
        longitude=77.27,
        photo=[
            'photo_0_0',
            'photo_0_1',
            'photo_0_5',
        ],
    ),
    StoragePermalinkWL(
        permalink=uint(1),
        latitude=45.55,
        longitude=77.27,
        photo=[
            'photo_1_2',
            'photo_1_7',
        ],
    ),
]


def test_run(helper: Helper):
    config_dict = asdict(WLMatchHotelsStageConfig)
    config_dict['job_size'] = 3
    config = StageConfig(**config_dict)

    trigger_options = {
        'default': TriggerOptions(
            workflow_options=NirvanaWorkflowOptions(
                singleProjectId='5',
                singlePoolId='1',
            ),
        ),
    }
    options = StageOptions(triggers=trigger_options)

    trigger = helper.get_trigger(config, start_ts=START_TS, options=options)
    helper.prepare_storage(hotels_wl=HOTELS_WL, permalinks_wl=PERMALINKS_WL)

    trigger.process()

    storage = helper.get_storage()

    assert 4 == len(storage.hotels_wl)

    hotel_wl: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_0', 'original_id_0')]
    assert StageStatus.IN_PROCESS == hotel_wl.status_wl_match_hotels
    assert START_TS == hotel_wl.status_wl_match_hotels_ts

    hotel_wl: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(0), 'partner_id_1', 'original_id_1')]
    assert StageStatus.IN_PROCESS == hotel_wl.status_wl_match_hotels
    assert START_TS == hotel_wl.status_wl_match_hotels_ts

    hotel_wl: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(1), 'partner_id_2', 'original_id_2')]
    assert StageStatus.IN_PROCESS == hotel_wl.status_wl_match_hotels
    assert START_TS == hotel_wl.status_wl_match_hotels_ts

    hotel_wl: StorageHotelWL = storage.hotels_wl[HotelWLKey(uint(1), 'partner_id_3', 'original_id_3')]
    assert StageStatus.IN_PROCESS == hotel_wl.status_wl_match_hotels
    assert START_TS == hotel_wl.status_wl_match_hotels_ts

    # trigger data
    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '0', 'hotels')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 3 == len(data)

    rec: WLMatchHotelsData = dc_from_dict(WLMatchHotelsData, data[0])
    assert '0' == rec.input.permalink
    assert 'partner_id_0' == rec.input.partner_id
    assert 'original_id_0' == rec.input.original_id
    assert '45.55' == rec.input.latitude_1
    assert '77.27' == rec.input.longitude_1
    assert '45.55' == rec.input.latitude_2
    assert '77.27' == rec.input.longitude_2
    assert .0 == rec.input.distance
    assert ['photo_0_1', 'photo_0_5'] == sorted(rec.input.photo_1)
    assert ['photo_0_0'] == rec.input.photo_2

    rec: WLMatchHotelsData = dc_from_dict(WLMatchHotelsData, data[1])
    assert '0' == rec.input.permalink
    assert 'partner_id_1' == rec.input.partner_id
    assert 'original_id_1' == rec.input.original_id
    assert '45.55' == rec.input.latitude_1
    assert '77.27' == rec.input.longitude_1
    assert '45.56' == rec.input.latitude_2
    assert '77.27' == rec.input.longitude_2
    assert abs(1112.0 - rec.input.distance) < 1
    assert ['photo_0_0', 'photo_0_5'] == sorted(rec.input.photo_1)
    assert ['photo_0_1'] == rec.input.photo_2

    rec: WLMatchHotelsData = dc_from_dict(WLMatchHotelsData, data[2])
    assert '1' == rec.input.permalink
    assert 'partner_id_2' == rec.input.partner_id
    assert 'original_id_2' == rec.input.original_id
    assert '45.55' == rec.input.latitude_1
    assert '77.27' == rec.input.longitude_1
    assert '45.55' == rec.input.latitude_2
    assert '77.26' == rec.input.longitude_2
    assert abs(779.0 - rec.input.distance) < 1
    assert ['photo_1_7'] == sorted(rec.input.photo_1)
    assert ['photo_1_2'] == rec.input.photo_2

    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '1', 'hotels')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 1 == len(data)

    rec: WLMatchHotelsData = dc_from_dict(WLMatchHotelsData, data[0])
    assert '1' == rec.input.permalink
    assert 'partner_id_3' == rec.input.partner_id
    assert 'original_id_3' == rec.input.original_id
    assert '45.55' == rec.input.latitude_1
    assert '77.27' == rec.input.longitude_1
    assert '45.54' == rec.input.latitude_2
    assert '77.27' == rec.input.longitude_2
    assert abs(1112.0 - rec.input.distance) < 1
    assert ['photo_1_2', 'photo_1_7'] == sorted(rec.input.photo_1)
    assert ['photo_1_3'] == rec.input.photo_2

    options_path = helper.persistence_manager.join(trigger_data_path, '@_workflow_options')
    workflow_options = helper.persistence_manager.get(options_path)
    assert dc_to_dict(options.triggers['default'].workflow_options) == workflow_options

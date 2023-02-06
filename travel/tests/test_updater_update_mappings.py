# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import UpdateMappingsStageConfig
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageMapping, StoragePermalink
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_to_dict, get_dc_yt_schema
from travel.hotels.content_manager.lib.storage import MappingKey


START_TS = int(datetime(2019, 10, 25).timestamp())

STORAGE_PERMALINKS = [
    StoragePermalink(
        id=uint(1),
    ),
    StoragePermalink(
        id=uint(2),
    ),
    StoragePermalink(
        id=uint(3),
        status_update_mappings=StageStatus.IN_PROCESS,
    ),
    # permalink with no offers
    StoragePermalink(
        id=uint(4),
        status_update_mappings=StageStatus.IN_PROCESS,
    ),
]

STAGE_PERMALINKS = [
    StoragePermalink(
        id=uint(3),
        status_update_mappings=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(4),
        status_update_mappings=StageStatus.IN_PROCESS,
    ),
]

MAPPINGS = [
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_TRAVELLINE',
        orig_hotel_id='0',
        mapping_key='mapping_key_0',
        permaroom_id=None,
        orig_room_name='orig_room_name_0',
    ),
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_TRAVELLINE',
        orig_hotel_id='1',
        mapping_key='mapping_key_1',
        permaroom_id=None,
        orig_room_name='orig_room_name_1',
    ),
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_NOT_TRAVELLINE',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=None,
        orig_room_name='orig_room_name_2',
    ),
]


def test_run(helper: Helper):
    updater = helper.get_updater(UpdateMappingsStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks=STORAGE_PERMALINKS)
    updater_args = helper.get_updater_args(UpdateMappingsStageConfig.name)
    output_path, _ = updater_args
    permalinks_table = helper.persistence_manager.join(output_path, 'permalinks')
    mappings_table = helper.persistence_manager.join(output_path, 'mappings')

    stage_permalinks = (dc_to_dict(d) for d in STAGE_PERMALINKS)
    helper.persistence_manager.write(permalinks_table, stage_permalinks, get_dc_yt_schema(StoragePermalink))
    mappings = (dc_to_dict(d) for d in MAPPINGS)
    helper.persistence_manager.write(mappings_table, mappings, get_dc_yt_schema(StorageMapping))

    updater.run(*updater_args)

    storage = helper.get_storage()

    assert 4 == len(storage.permalinks)

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_update_mappings
    assert START_TS == permalink.status_update_mappings_ts

    # permalink 3 mappings
    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 2 == len(permalink_other_mappings)

    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_TRAVELLINE', '0', 'mapping_key_0')]
    assert StageStatus.TO_BE_PROCESSED == mapping.status_offer_prioritization
    assert START_TS == mapping.status_offer_prioritization_ts

    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_TRAVELLINE', '1', 'mapping_key_1')]
    assert StageStatus.TO_BE_PROCESSED == mapping.status_offer_prioritization
    assert START_TS == mapping.status_offer_prioritization_ts

    # permalink 4
    permalink = storage.permalinks[uint(4)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_update_mappings
    assert START_TS == permalink.status_update_mappings_ts

    # permalink 4 mappings
    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 0 == len(permalink_other_mappings)

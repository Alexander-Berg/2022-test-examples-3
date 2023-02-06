# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import OfferPrioritizationStageConfig
from travel.hotels.content_manager.data_model.storage import (
    StageStatus, StorageMapping, StoragePermalink, StoragePermaroom
)
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_to_dict, get_dc_yt_schema
from travel.hotels.content_manager.lib.storage import MappingKey


START_TS = int(datetime(2019, 10, 25).timestamp())

STORAGE_PERMALINKS = [
    StoragePermalink(id=uint(3)),
]

STORAGE_PERMAROOMS = [
    StoragePermaroom(
        id=0,
        permalink=uint(3),
        name='permaroom_name_0',
        comment='permaroom_comment_0',
        alternative_names='alternative_names_0',
    ),
]

STORAGE_MAPPINGS = [
    # Should be sent to yang
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_0',
        orig_hotel_id='0',
        mapping_key='mapping_key_0',
        permaroom_id=None,
        orig_room_name='orig_room_name_0',
        status_offer_prioritization=StageStatus.IN_PROCESS,
    ),
    # Should be deleted
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_2',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=None,
        orig_room_name='orig_room_name_2',
        status_offer_prioritization=StageStatus.IN_PROCESS,
    ),
    # Nothing happens
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_hidden',
        orig_hotel_id='hidden',
        mapping_key='mapping_key_hidden',
        permaroom_id=None,
        orig_room_name='orig_room_name_hidden',
        status_offer_prioritization=StageStatus.NOTHING_TO_DO,
        status_yang_mappings=StageStatus.NOTHING_TO_DO,
    ),
    # Nothing happens
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_3',
        orig_hotel_id='3',
        mapping_key='mapping_key_3',
        permaroom_id=0,
        orig_room_name='orig_room_name_3',
        status_offer_prioritization=StageStatus.NOTHING_TO_DO,
        status_yang_mappings=StageStatus.NOTHING_TO_DO,
    ),
]

KEEP_MAPPINGS = [
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_0',
        orig_hotel_id='0',
        mapping_key='mapping_key_0',
        permaroom_id=None,
        orig_room_name='orig_room_name_0',
        status_offer_prioritization=StageStatus.IN_PROCESS,
    ),
]

REMOVE_MAPPINGS = [
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_2',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=None,
        orig_room_name='orig_room_name_2',
        status_offer_prioritization=StageStatus.IN_PROCESS,
    ),
]


def test_run(helper: Helper):
    helper.prepare_storage(permalinks=STORAGE_PERMALINKS, permarooms=STORAGE_PERMAROOMS, mappings=STORAGE_MAPPINGS)
    updater = helper.get_updater(OfferPrioritizationStageConfig, start_ts=START_TS)
    updater_args = helper.get_updater_args(OfferPrioritizationStageConfig.name)
    output_path, _ = updater_args

    keep_table = helper.persistence_manager.join(output_path, 'mappings_to_keep')
    remove_table = helper.persistence_manager.join(output_path, 'mappings_to_remove')

    keep_mappings = (dc_to_dict(d) for d in KEEP_MAPPINGS)
    helper.persistence_manager.write(keep_table, keep_mappings, get_dc_yt_schema(StorageMapping))
    remove_mappings = (dc_to_dict(d) for d in REMOVE_MAPPINGS)
    helper.persistence_manager.write(remove_table, remove_mappings, get_dc_yt_schema(StorageMapping))

    updater.run(*updater_args)

    storage = helper.get_storage()

    assert 1 == len(storage.permalinks)

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)

    # ensure one mapping deleted
    assert 2 == len(permalink_other_mappings)

    # ensure sending to yang
    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_0', '0', 'mapping_key_0')]
    assert 'orig_room_name_0' == mapping.orig_room_name
    assert StageStatus.NOTHING_TO_DO == mapping.status_offer_prioritization
    assert START_TS == mapping.status_offer_prioritization_ts
    assert StageStatus.TO_BE_PROCESSED == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts

    # ensure stays the same
    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_hidden', 'hidden', 'mapping_key_hidden')]
    assert 'orig_room_name_hidden' == mapping.orig_room_name
    assert StageStatus.NOTHING_TO_DO == mapping.status_offer_prioritization
    assert 0 == mapping.status_offer_prioritization_ts
    assert StageStatus.NOTHING_TO_DO == mapping.status_yang_mappings
    assert 0 == mapping.status_yang_mappings_ts

    # ensure stays the same
    permalink_permarooms = storage.get_permalink_permarooms(permalink)
    permaroom_mappings = storage.get_permaroom_mappings(permalink_permarooms[0])
    mapping = permaroom_mappings[MappingKey(uint(3), 'OI_3', '3', 'mapping_key_3')]
    assert 'orig_room_name_3' == mapping.orig_room_name
    assert StageStatus.NOTHING_TO_DO == mapping.status_offer_prioritization
    assert 0 == mapping.status_offer_prioritization_ts
    assert StageStatus.NOTHING_TO_DO == mapping.status_yang_mappings
    assert 0 == mapping.status_yang_mappings_ts

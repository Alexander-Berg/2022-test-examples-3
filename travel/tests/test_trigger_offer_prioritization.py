# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import OfferPrioritizationStageConfig
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageMapping, StoragePermalink
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.storage import MappingKey

START_TS = int(datetime(2019, 10, 25).timestamp())

PERMALINKS = [
    StoragePermalink(
        id=uint(3),
    ),
]

MAPPINGS = [
    # Good
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_1',
        orig_hotel_id='1',
        mapping_key='mapping_key_1',
        permaroom_id=None,
        orig_room_name='orig_room_name_1',
        status_offer_prioritization=StageStatus.TO_BE_PROCESSED,
    ),
    # Should not be processed due to the status
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_2',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=None,
        orig_room_name='orig_room_name_2',
        status_offer_prioritization=StageStatus.NOTHING_TO_DO,
    ),
]


def test_run(helper: Helper):
    trigger = helper.get_trigger(OfferPrioritizationStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks=PERMALINKS, mappings=MAPPINGS)

    trigger.process()

    # trigger data
    mappings_table = helper.persistence_manager.join(trigger.stage_input_path, '0', 'mappings')
    data = list(helper.persistence_manager.read(mappings_table))

    storage = helper.get_storage()

    assert 1 == len(data)

    # mapping (3, OI_2, 2, mapping_key_2)
    mapping = StorageMapping(**data[0])
    assert 'mapping_key_1' == mapping.mapping_key

    # storage data
    assert 1 == len(storage.permalinks)

    # permalink 3
    permalink = storage.permalinks[uint(3)]

    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 2 == len(permalink_other_mappings)

    # mapping (3, 'OI_1', '1', 'mapping_key_1')
    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_1', '1', 'mapping_key_1')]
    assert StageStatus.IN_PROCESS == mapping.status_offer_prioritization
    assert START_TS == mapping.status_offer_prioritization_ts

    # mapping (3, 'OI_2', '2', 'mapping_key_2')
    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_2', '2', 'mapping_key_2')]
    assert StageStatus.NOTHING_TO_DO == mapping.status_offer_prioritization
    assert 0 == mapping.status_offer_prioritization_ts

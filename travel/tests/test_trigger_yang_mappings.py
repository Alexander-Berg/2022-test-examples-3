# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import YangMappingsStageConfig
from travel.hotels.content_manager.data_model.options import NirvanaWorkflowOptions, StageOptions, TriggerOptions
from travel.hotels.content_manager.data_model.stage import YangMappingsData
from travel.hotels.content_manager.data_model.storage import (
    StageStatus, StorageMapping, StoragePermalink, StoragePermaroom
)
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict
from travel.hotels.content_manager.lib.storage import MappingKey


START_TS = int(datetime(2019, 10, 25).timestamp())

PERMALINKS = [
    StoragePermalink(
        id=uint(1),
    ),
    StoragePermalink(
        id=uint(2),
    ),
    StoragePermalink(
        id=uint(3),
        hotel_url='hotel_url_3',
    ),
    StoragePermalink(
        id=uint(4),
        hotel_url='hotel_url_4',
    ),
]

PERMAROOMS = [
    StoragePermaroom(
        id=0,
        permalink=uint(3),
        name='permaroom_name_0',
        comment='permaroom_comment_0',
        alternative_names='alternative_names_0',
    ),
    StoragePermaroom(
        id=1,
        permalink=uint(3),
        name='hidden',
        comment='permaroom_comment_1',
        alternative_names='alternative_names_1',
    ),
    StoragePermaroom(
        id=2,
        permalink=uint(4),
        name='permaroom_name_2',
        comment='permaroom_comment_2',
        alternative_names='alternative_names_2',
    ),
    StoragePermaroom(
        id=3,
        permalink=uint(4),
        name='permaroom_name_3',
        comment='permaroom_comment_3',
        alternative_names='alternative_names_3',
    ),
]

MAPPINGS = [
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_0',
        orig_hotel_id='0',
        mapping_key='mapping_key_0',
        permaroom_id=None,
        orig_room_name='orig_room_name_0',
        status_yang_mappings=StageStatus.TO_BE_PROCESSED,
    ),
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_1',
        orig_hotel_id='1',
        mapping_key='mapping_key_1',
        permaroom_id=None,
        orig_room_name='orig_room_name_1',
        status_yang_mappings=StageStatus.TO_BE_PROCESSED,
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_2',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=None,
        orig_room_name='orig_room_name_2',
        status_yang_mappings=StageStatus.TO_BE_PROCESSED,
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_3',
        orig_hotel_id='3',
        mapping_key='mapping_key_3',
        permaroom_id=None,
        orig_room_name='orig_room_name_3',
        status_yang_mappings=StageStatus.NOTHING_TO_DO,
    ),
]


def test_run(helper: Helper):
    options = StageOptions(
        triggers={
            'default': TriggerOptions(
                workflow_options=NirvanaWorkflowOptions(
                    singleProjectId='5',
                    singlePoolId='1',
                ),
            ),
        }
    )

    trigger = helper.get_trigger(YangMappingsStageConfig, start_ts=START_TS, options=options)
    helper.prepare_storage(permalinks=PERMALINKS, permarooms=PERMAROOMS, mappings=MAPPINGS)

    trigger.process()

    storage = helper.get_storage()

    assert 4 == len(storage.permalinks)

    # permalink 3
    permalink = storage.permalinks[uint(3)]

    # permalink 3 mappings
    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 2 == len(permalink_other_mappings)

    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_0', '0', 'mapping_key_0')]
    assert StageStatus.IN_PROCESS == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts

    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_1', '1', 'mapping_key_1')]
    assert StageStatus.IN_PROCESS == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts

    # permalink 4
    permalink = storage.permalinks[uint(4)]

    # permalink 4 mappings
    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 2 == len(permalink_other_mappings)

    mapping = permalink_other_mappings[MappingKey(uint(4), 'OI_2', '2', 'mapping_key_2')]
    assert StageStatus.IN_PROCESS == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts

    mapping = permalink_other_mappings[MappingKey(uint(4), 'OI_3', '3', 'mapping_key_3')]
    assert StageStatus.NOTHING_TO_DO == mapping.status_yang_mappings
    assert 0 == mapping.status_yang_mappings_ts

    # trigger data
    trigger_data_path = helper.persistence_manager.join(trigger.stage_input_path, '0', 'yang_mappings')
    data = helper.persistence_manager.read(trigger_data_path)
    data = list(data)

    assert 3 == len(data)

    rec: YangMappingsData = dc_from_dict(YangMappingsData, data[0])
    assert '3' == rec.input.permalink
    assert 'OI_0' == rec.input.operator_id
    assert '0' == rec.input.orig_hotel_id
    assert 'mapping_key_0' == rec.input.mapping_key
    assert 'orig_room_name_0' == rec.input.offer_room_name

    assert 1 == len(rec.input.permarooms)
    permaroom = rec.input.permarooms[0]
    assert '0' == permaroom.permaroom_id
    assert 'permaroom_name_0' == permaroom.permaroom_name
    assert 'permaroom_comment_0' == permaroom.permaroom_comment
    assert 'alternative_names_0' == permaroom.alternative_names

    rec = dc_from_dict(YangMappingsData, data[1])
    assert '3' == rec.input.permalink
    assert 'OI_1' == rec.input.operator_id
    assert '1' == rec.input.orig_hotel_id
    assert 'mapping_key_1' == rec.input.mapping_key
    assert 'orig_room_name_1' == rec.input.offer_room_name

    assert 1 == len(rec.input.permarooms)
    permaroom = rec.input.permarooms[0]
    assert '0' == permaroom.permaroom_id
    assert 'permaroom_name_0' == permaroom.permaroom_name
    assert 'permaroom_comment_0' == permaroom.permaroom_comment
    assert 'alternative_names_0' == permaroom.alternative_names

    rec = dc_from_dict(YangMappingsData, data[2])
    assert '4' == rec.input.permalink
    assert 'OI_2' == rec.input.operator_id
    assert '2' == rec.input.orig_hotel_id
    assert 'mapping_key_2' == rec.input.mapping_key
    assert 'orig_room_name_2' == rec.input.offer_room_name

    assert 2 == len(rec.input.permarooms)
    permaroom = rec.input.permarooms[0]
    assert '2' == permaroom.permaroom_id
    assert 'permaroom_name_2' == permaroom.permaroom_name
    assert 'permaroom_comment_2' == permaroom.permaroom_comment
    assert 'alternative_names_2' == permaroom.alternative_names
    permaroom = rec.input.permarooms[1]
    assert '3' == permaroom.permaroom_id
    assert 'permaroom_name_3' == permaroom.permaroom_name
    assert 'permaroom_comment_3' == permaroom.permaroom_comment
    assert 'alternative_names_3' == permaroom.alternative_names

    options_path = helper.persistence_manager.join(trigger_data_path, '@_workflow_options')
    workflow_options = helper.persistence_manager.get(options_path)
    assert dc_to_dict(options.triggers['default'].workflow_options) == workflow_options

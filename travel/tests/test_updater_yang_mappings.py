# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import YangMappingsStageConfig
from travel.hotels.content_manager.data_model.stage import (
    MappingTaskInfo, YangMappingsData, YangMappingsInputData, YangMappingsOutputData
)
from travel.hotels.content_manager.data_model.storage import (
    StageStatus, StorageMapping, StoragePermalink, StoragePermaroom
)
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict, get_dc_yt_schema, ts_to_str_msk_tz
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
    StoragePermalink(
        id=uint(5),
        hotel_url='hotel_url_5',
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
        name='permaroom_name_1',
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
        status_yang_mappings=StageStatus.IN_PROCESS,
    ),
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_1',
        orig_hotel_id='1',
        mapping_key='mapping_key_1',
        permaroom_id=None,
        orig_room_name='orig_room_name_1',
        status_yang_mappings=StageStatus.IN_PROCESS,
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_2',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=None,
        orig_room_name='orig_room_name_2',
        status_yang_mappings=StageStatus.IN_PROCESS,
        need_new_permaroom=True,
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_3',
        orig_hotel_id='3',
        mapping_key='mapping_key_3',
        permaroom_id=None,
        orig_room_name='orig_room_name_3',
        status_yang_mappings=StageStatus.IN_PROCESS,
    ),
    StorageMapping(
        permalink=uint(5),
        operator_id='OI_4',
        orig_hotel_id='4',
        mapping_key='mapping_key_4',
        permaroom_id=None,
        orig_room_name='orig_room_name_4',
        status_yang_mappings=StageStatus.IN_PROCESS,
        counters={'need_new_permaroom': 2},
    ),
]

TASK_INFO = MappingTaskInfo(
    assignment_id='assignment_id_0',
    create_ts=uint(START_TS),
    pool_id='',
    reward=1000,
    status='ACCEPTED',
    submit_ts=uint(START_TS + 1000),
    worker_id='worker_id_0',
)

PROCESSOR_RESULT = [
    YangMappingsData(
        input=YangMappingsInputData(
            permalink='3',
            operator_id='OI_0',
            orig_hotel_id='0',
            mapping_key='mapping_key_0',
        ),
        output=YangMappingsOutputData(
            result='0',
            offer_comment='offer_comment_0',
        ),
        info=TASK_INFO,
    ),
    YangMappingsData(
        input=YangMappingsInputData(
            permalink='3',
            operator_id='OI_1',
            orig_hotel_id='1',
            mapping_key='mapping_key_1',
        ),
        output=YangMappingsOutputData(
            result='unknown',
            offer_comment='offer_comment_1',
        ),
        info=TASK_INFO,
    ),
    YangMappingsData(
        input=YangMappingsInputData(
            permalink='4',
            operator_id='OI_2',
            orig_hotel_id='2',
            mapping_key='mapping_key_2',
        ),
        output=YangMappingsOutputData(
            result='2',
            offer_comment='offer_comment_2',
        ),
        info=TASK_INFO,
    ),
    YangMappingsData(
        input=YangMappingsInputData(
            permalink='4',
            operator_id='OI_3',
            orig_hotel_id='3',
            mapping_key='mapping_key_3',
        ),
        output=YangMappingsOutputData(
            result='new_permaroom',
            offer_comment='offer_comment_3',
        ),
        info=TASK_INFO,
    ),
    YangMappingsData(
        input=YangMappingsInputData(
            permalink='5',
            operator_id='OI_4',
            orig_hotel_id='4',
            mapping_key='mapping_key_4',
        ),
        output=YangMappingsOutputData(
            result='new_permaroom',
            offer_comment='offer_comment_4',
        ),
        info=TASK_INFO,
    ),
]


def test_run(helper: Helper):
    updater = helper.get_updater(YangMappingsStageConfig, start_ts=START_TS)
    helper.prepare_storage(permalinks=PERMALINKS, permarooms=PERMAROOMS, mappings=MAPPINGS)
    updater_args = helper.get_updater_args(YangMappingsStageConfig.name)
    output_path, _ = updater_args

    processor_result_path = helper.persistence_manager.join(output_path, 'yang_mappings')
    processor_result = (dc_to_dict(pr) for pr in PROCESSOR_RESULT)
    helper.persistence_manager.write(processor_result_path, processor_result, get_dc_yt_schema(YangMappingsData))

    updater.run(*updater_args)

    # log data
    exp_task_info: MappingTaskInfo = TASK_INFO
    exp_task_info.stage = YangMappingsStageConfig.name

    table_name = f'mappings {ts_to_str_msk_tz(START_TS)}'
    logs_table = helper.persistence_manager.join(helper.path_info.logs_path, 'task_info', table_name)
    for log_rec, result in zip(helper.persistence_manager.read(logs_table), PROCESSOR_RESULT):
        task_info: MappingTaskInfo = dc_from_dict(MappingTaskInfo, log_rec)
        assert int(result.input.permalink) == task_info.permalink
        assert result.input.operator_id == task_info.operator_id
        assert result.input.orig_hotel_id == task_info.orig_hotel_id
        assert result.input.mapping_key == task_info.mapping_key

        assert exp_task_info.assignment_id == task_info.assignment_id
        assert exp_task_info.create_ts == task_info.create_ts
        assert exp_task_info.pool_id == task_info.pool_id
        assert exp_task_info.reward == task_info.reward
        assert exp_task_info.stage == task_info.stage
        assert exp_task_info.status == task_info.status
        assert exp_task_info.submit_ts == task_info.submit_ts
        assert exp_task_info.worker_id == task_info.worker_id

        assert isinstance(task_info.reward, float)

    storage = helper.get_storage()

    assert 5 == len(storage.permalinks)

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_yang_rooms
    assert 0 == permalink.status_yang_rooms_ts

    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 1 == len(permalink_other_mappings)
    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_1', '1', 'mapping_key_1')]
    assert 'orig_room_name_1' == mapping.orig_room_name
    assert StageStatus.NOTHING_TO_DO == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts
    assert 'offer_comment_1' == mapping.comment
    assert mapping.is_hidden

    # permalink 3 permarooms
    permalink_permarooms = storage.get_permalink_permarooms(permalink)
    assert 2 == len(permalink_permarooms)

    permaroom = permalink_permarooms[0]
    assert 'alternative_names_0' == permaroom.alternative_names

    permaroom_mappings = storage.get_permaroom_mappings(permaroom)
    mapping = permaroom_mappings[MappingKey(uint(3), 'OI_0', '0', 'mapping_key_0')]
    assert 'orig_room_name_0' == mapping.orig_room_name
    assert StageStatus.NOTHING_TO_DO == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts
    assert 'offer_comment_0' == mapping.comment

    permaroom = permalink_permarooms[1]
    assert 'alternative_names_1' == permaroom.alternative_names

    permaroom_mappings = storage.get_permaroom_mappings(permaroom)
    assert 0 == len(permaroom_mappings)

    # permalink 4
    permalink = storage.permalinks[uint(4)]
    assert StageStatus.TO_BE_PROCESSED == permalink.status_yang_rooms
    assert START_TS == permalink.status_yang_rooms_ts

    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 1 == len(permalink_other_mappings)
    mapping = permalink_other_mappings[MappingKey(uint(4), 'OI_3', '3', 'mapping_key_3')]
    assert 'orig_room_name_3' == mapping.orig_room_name
    assert StageStatus.NOTHING_TO_DO == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts
    assert 'offer_comment_3' == mapping.comment
    assert mapping.need_new_permaroom
    assert not mapping.is_hidden
    assert 1 == mapping.counters['need_new_permaroom']

    # permalink 4 permarooms
    permalink_permarooms = storage.get_permalink_permarooms(permalink)
    assert 2 == len(permalink_permarooms)

    permaroom = permalink_permarooms[2]
    assert 'alternative_names_2' == permaroom.alternative_names

    permaroom_mappings = storage.get_permaroom_mappings(permaroom)

    mapping = permaroom_mappings[MappingKey(uint(4), 'OI_2', '2', 'mapping_key_2')]
    assert 'orig_room_name_2' == mapping.orig_room_name
    assert StageStatus.NOTHING_TO_DO == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts
    assert 'offer_comment_2' == mapping.comment
    assert not mapping.need_new_permaroom

    permaroom = permalink_permarooms[3]
    assert 'alternative_names_3' == permaroom.alternative_names

    permaroom_mappings = storage.get_permaroom_mappings(permaroom)
    assert 0 == len(permaroom_mappings)

    # permalink 5
    permalink = storage.permalinks[uint(5)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_yang_rooms
    assert 0 == permalink.status_yang_rooms_ts

    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 1 == len(permalink_other_mappings)
    mapping = permalink_other_mappings[MappingKey(uint(5), 'OI_4', '4', 'mapping_key_4')]
    assert 'orig_room_name_4' == mapping.orig_room_name
    assert StageStatus.NOTHING_TO_DO == mapping.status_yang_mappings
    assert START_TS == mapping.status_yang_mappings_ts
    assert 'offer_comment_4' == mapping.comment
    assert not mapping.need_new_permaroom
    assert not mapping.is_hidden
    assert mapping.is_banned
    assert 3 == mapping.counters['need_new_permaroom']

    # permalink 4 permarooms
    permalink_permarooms = storage.get_permalink_permarooms(permalink)
    assert 0 == len(permalink_permarooms)

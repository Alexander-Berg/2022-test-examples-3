# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import YangRoomsStageConfig
from travel.hotels.content_manager.data_model.stage import (
    AvailablePermaroom, PermalinkTaskInfo, YangRoomsData, YangRoomsInputData, YangRoomsOutputData,
)
from travel.hotels.content_manager.data_model.storage import (
    StageStatus, StorageMapping, StoragePermalink, StoragePermaroom
)
from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict, get_dc_yt_schema, ts_to_str_msk_tz
from travel.hotels.content_manager.lib.storage import MappingKey
from travel.hotels.content_manager.stages.yang_rooms.updater import PERMAROOM_ID_OFFSET


START_TS = int(datetime(2019, 10, 25).timestamp())

PERMALINKS = [
    StoragePermalink(
        id=uint(1),
        status_yang_rooms=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(2),
        status_yang_rooms=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(3),
        hotel_url='hotel_url_3',
        status_yang_rooms=StageStatus.IN_PROCESS,
    ),
    StoragePermalink(
        id=uint(4),
        hotel_url='hotel_url_4',
        status_yang_rooms=StageStatus.IN_PROCESS,
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
        id=2,
        permalink=uint(3),
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
    StoragePermaroom(
        id=4,
        permalink=uint(4),
        name='permaroom_name_4',
        comment='permaroom_comment_4',
        alternative_names='alternative_names_4',
    ),
]

MAPPINGS = [
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_0',
        orig_hotel_id='0',
        mapping_key='mapping_key_0',
        permaroom_id=0,
        orig_room_name='orig_room_name_0',
    ),
    StorageMapping(
        is_hidden=True,
        permalink=uint(3),
        operator_id='OI_1',
        orig_hotel_id='1',
        mapping_key='mapping_key_1',
        permaroom_id=None,
        orig_room_name='orig_room_name_1',
    ),
    StorageMapping(
        permalink=uint(3),
        operator_id='OI_NO_DATA',
        orig_hotel_id='no_data',
        mapping_key='mapping_key_no_data',
        permaroom_id=None,
        orig_room_name='',
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_2',
        orig_hotel_id='2',
        mapping_key='mapping_key_2',
        permaroom_id=None,
        orig_room_name='orig_room_name_2',
    ),
    StorageMapping(
        permalink=uint(4),
        operator_id='OI_3',
        orig_hotel_id='3',
        mapping_key='mapping_key_3',
        permaroom_id=3,
        orig_room_name='orig_room_name_3',
    ),
    StorageMapping(
        is_banned=True,
        permalink=uint(4),
        operator_id='OI_4',
        orig_hotel_id='4',
        mapping_key='mapping_key_4',
        permaroom_id=None,
        orig_room_name='orig_room_name_4',
    ),
]

TASK_INFO = PermalinkTaskInfo(
    assignment_id='assignment_id_0',
    create_ts=uint(START_TS),
    pool_id='',
    reward=1000,
    status='ACCEPTED',
    submit_ts=uint(START_TS + 1000),
    worker_id='worker_id_0',
)

PROCESSOR_RESULT = [
    YangRoomsData(
        input=YangRoomsInputData(
            permalink='3',
            available_permarooms=[
                AvailablePermaroom(
                    permaroom_id='0',
                    permaroom_name='permaroom_name_0',
                    alternative_names='alternative_names_0',
                    permaroom_comment='permaroom_comment_0',
                ),
                AvailablePermaroom(
                    permaroom_id='2',
                    permaroom_name='permaroom_name_2',
                    alternative_names='alternative_names_2',
                    permaroom_comment='permaroom_comment_2',
                ),
            ],
        ),
        output=YangRoomsOutputData(
            result_permarooms=[
                # permaroom_name_0 was deleted
                AvailablePermaroom(
                    # permaroom_name_1 was added
                    permaroom_id=None,
                    permaroom_name='permaroom_name_1',
                    alternative_names='new_alternative_names_1',
                    permaroom_comment='new_permaroom_comment_1',
                ),
                AvailablePermaroom(
                    # permaroom_name_2 was updated
                    permaroom_id='2',
                    permaroom_name='permaroom_name_2',
                    alternative_names='updated_alternative_names_2',
                    permaroom_comment='updated_permaroom_comment_2',
                ),
            ],
        ),
        info=TASK_INFO,
    ),
    YangRoomsData(
        input=YangRoomsInputData(
            permalink='4',
            available_permarooms=[
                AvailablePermaroom(
                    permaroom_id='3',
                    permaroom_name='permaroom_name_3',
                    alternative_names='alternative_names_3',
                    permaroom_comment='permaroom_comment_3',
                ),
                AvailablePermaroom(
                    permaroom_id='4',
                    permaroom_name='permaroom_name_4',
                    alternative_names='alternative_names_4',
                    permaroom_comment='permaroom_comment_4',
                ),
            ],
        ),
        output=YangRoomsOutputData(
            result_permarooms=[
                AvailablePermaroom(
                    # permaroom_name_3 was renamed
                    permaroom_id='3',
                    permaroom_name='renamed_permaroom_name_3',
                    alternative_names='renamed_alternative_names_3',
                    permaroom_comment='renamed_permaroom_comment_3',
                ),
                AvailablePermaroom(
                    # permaroom_name_4 was not changed
                    permaroom_id='4',
                    permaroom_name='permaroom_name_4',
                    alternative_names='alternative_names_4',
                    permaroom_comment='permaroom_comment_4',
                ),
            ],
        ),
        info=TASK_INFO,
    ),
]


def test_run(helper: Helper):
    helper.prepare_storage(permalinks=PERMALINKS, permarooms=PERMAROOMS, mappings=MAPPINGS)
    updater = helper.get_updater(YangRoomsStageConfig, start_ts=START_TS)
    updater_args = helper.get_updater_args(YangRoomsStageConfig.name)
    output_path, _ = updater_args

    processor_result_path = helper.persistence_manager.join(output_path, 'yang_rooms')
    processor_result = (dc_to_dict(pr) for pr in PROCESSOR_RESULT)
    helper.persistence_manager.write(processor_result_path, processor_result, get_dc_yt_schema(YangRoomsData))

    updater.run(*updater_args)

    # log data
    exp_task_info: PermalinkTaskInfo = TASK_INFO
    exp_task_info.stage = YangRoomsStageConfig.name

    table_name = f'permalinks {ts_to_str_msk_tz(START_TS)}'
    logs_table = helper.persistence_manager.join(helper.path_info.logs_path, 'task_info', table_name)
    for log_rec, result in zip(helper.persistence_manager.read(logs_table), PROCESSOR_RESULT):
        task_info: PermalinkTaskInfo = dc_from_dict(PermalinkTaskInfo, log_rec)
        assert int(result.input.permalink) == task_info.permalink

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

    assert 4 == len(storage.permalinks)

    # permalink 3
    permalink = storage.permalinks[uint(3)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_yang_rooms
    assert START_TS == permalink.status_yang_rooms_ts

    # permalink 3 permarooms
    permalink_permarooms = storage.get_permalink_permarooms(permalink)
    assert 3 == len(permalink_permarooms)

    permaroom = permalink_permarooms[0]
    assert permaroom.is_deleted
    assert 'permaroom_name_0' == permaroom.name
    assert 'alternative_names_0' == permaroom.alternative_names
    assert 'permaroom_comment_0' == permaroom.comment

    permaroom = permalink_permarooms[PERMAROOM_ID_OFFSET + len(PERMAROOMS)]
    assert not permaroom.is_deleted
    assert 'permaroom_name_1' == permaroom.name
    assert 'new_alternative_names_1' == permaroom.alternative_names
    assert 'new_permaroom_comment_1' == permaroom.comment

    permaroom = permalink_permarooms[2]
    assert not permaroom.is_deleted
    assert 'permaroom_name_2' == permaroom.name
    assert 'updated_alternative_names_2' == permaroom.alternative_names
    assert 'updated_permaroom_comment_2' == permaroom.comment

    # permalink 3 mappings
    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 3 == len(permalink_other_mappings)

    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_0', '0', 'mapping_key_0')]
    assert StageStatus.TO_BE_PROCESSED == mapping.status_offer_prioritization
    assert START_TS == mapping.status_offer_prioritization_ts

    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_1', '1', 'mapping_key_1')]
    assert StageStatus.NOTHING_TO_DO == mapping.status_offer_prioritization
    assert 0 == mapping.status_offer_prioritization_ts

    mapping = permalink_other_mappings[MappingKey(uint(3), 'OI_NO_DATA', 'no_data', 'mapping_key_no_data')]
    assert StageStatus.NOTHING_TO_DO == mapping.status_offer_prioritization
    assert 0 == mapping.status_offer_prioritization_ts

    # permalink 4
    permalink = storage.permalinks[uint(4)]
    assert StageStatus.NOTHING_TO_DO == permalink.status_yang_rooms
    assert START_TS == permalink.status_yang_rooms_ts

    # permalink 4 permarooms
    permalink_permarooms = storage.get_permalink_permarooms(permalink)
    assert 2 == len(permalink_permarooms)

    permaroom = permalink_permarooms[3]
    assert not permaroom.is_deleted
    assert 'renamed_permaroom_name_3' == permaroom.name
    assert 'renamed_alternative_names_3' == permaroom.alternative_names
    assert 'renamed_permaroom_comment_3' == permaroom.comment

    mapping = storage.get_permaroom_mappings(permaroom)[MappingKey(uint(4), 'OI_3', '3', 'mapping_key_3')]
    assert StageStatus.NOTHING_TO_DO == mapping.status_offer_prioritization
    assert 0 == mapping.status_offer_prioritization_ts

    permaroom = permalink_permarooms[4]
    assert not permaroom.is_deleted
    assert 'permaroom_name_4' == permaroom.name
    assert 'alternative_names_4' == permaroom.alternative_names
    assert 'permaroom_comment_4' == permaroom.comment

    # permalink 4 mappings
    permalink_other_mappings = storage.get_permalink_other_mappings(permalink)
    assert 2 == len(permalink_other_mappings)

    mapping = permalink_other_mappings[MappingKey(uint(4), 'OI_2', '2', 'mapping_key_2')]
    assert StageStatus.TO_BE_PROCESSED == mapping.status_offer_prioritization
    assert START_TS == mapping.status_offer_prioritization_ts

    mapping = permalink_other_mappings[MappingKey(uint(4), 'OI_4', '4', 'mapping_key_4')]
    assert StageStatus.NOTHING_TO_DO == mapping.status_offer_prioritization
    assert 0 == mapping.status_offer_prioritization_ts

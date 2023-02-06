# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import SCUpdateDescriptionsStageConfig
from travel.hotels.content_manager.data_model.stage import (
    DescriptionTaskInfo, SCUpdateDescriptionsData, SCUpdateDescriptionsDataInput, SCUpdateDescriptionsDataOutput
)
from travel.hotels.content_manager.data_model.storage import StageStatus, StorageSCDescription
from travel.hotels.content_manager.data_model.types import SCDescriptionResult, uint
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict, get_dc_yt_schema, ts_to_str_msk_tz
from travel.hotels.content_manager.lib.storage import SCDescriptionKey


START_TS = int(datetime(2019, 10, 25).timestamp())

STORAGE_DESCRIPTIONS = [
    # not sent to stage
    StorageSCDescription(
        carrier_code='carrier_code_0',
        car_type_code='car_type_code_0',
        sc_code='sc_code_0',
        data_source='data_source_0_0',
        sc_description='sc_description_0_0',
        sc_description_original='sc_description_original_0_0',
        sc_description_result=SCDescriptionResult.UPDATED,
        status_sc_update_descriptions=StageStatus.NOTHING_TO_DO,
    ),
    # no description
    StorageSCDescription(
        carrier_code='carrier_code_1',
        car_type_code='car_type_code_1',
        sc_code='sc_code_1',
        data_source='data_source_1_0',
        sc_description='',
        sc_description_original='',
        status_sc_update_descriptions=StageStatus.IN_PROCESS,
    ),
    # has description ACTUAL
    StorageSCDescription(
        carrier_code='carrier_code_2',
        car_type_code='car_type_code_2',
        sc_code='sc_code_2',
        data_source='data_source_2_0',
        sc_description='sc_description_2_0',
        sc_description_original='sc_description_original_2_0',
        status_sc_update_descriptions=StageStatus.IN_PROCESS,
    ),
    # has description UPDATED
    StorageSCDescription(
        carrier_code='carrier_code_3',
        car_type_code='car_type_code_3',
        sc_code='sc_code_3',
        data_source='data_source_3_0',
        sc_description='sc_description_3_0',
        sc_description_original='sc_description_original_3_0',
        status_sc_update_descriptions=StageStatus.IN_PROCESS,
    ),
    # has description NO_DATA
    StorageSCDescription(
        carrier_code='carrier_code_4',
        car_type_code='car_type_code_4',
        sc_code='sc_code_4',
        data_source='data_source_4_0',
        sc_description='sc_description_4_0',
        sc_description_original='sc_description_original_4_0',
        status_sc_update_descriptions=StageStatus.IN_PROCESS,
    ),
    # has description UPDATED is_source_official == true
    StorageSCDescription(
        carrier_code='carrier_code_5',
        car_type_code='car_type_code_5',
        sc_code='sc_code_5',
        data_source='data_source_5_0',
        sc_description='sc_description_5_0',
        sc_description_original='sc_description_original_5_0',
        status_sc_update_descriptions=StageStatus.IN_PROCESS,
    ),
]

TASK_INFO = DescriptionTaskInfo(
    assignment_id='assignment_id_0',
    create_ts=uint(START_TS),
    pool_id='',
    reward=1000,
    status='ACCEPTED',
    submit_ts=uint(START_TS + 1000),
    worker_id='worker_id_0',
)

PROCESSOR_RESULT = [
    SCUpdateDescriptionsData(
        input=SCUpdateDescriptionsDataInput(
            carrier_code='carrier_code_1',
            car_type_code='car_type_code_1',
            sc_code='sc_code_1',
            url='url_1',
        ),
        output=SCUpdateDescriptionsDataOutput(
            result=SCDescriptionResult.UPDATED,
            data_source='data_source_1_1',
            sc_description='sc_description_1_0',
            sc_description_original='sc_description_original_1_0',
            sc_description_specific=[{'car_numbers': '1|2|5', 'description': 'sc_description_specific_1_0'}]
        ),
        info=TASK_INFO,
    ),
    # has description ACTUAL
    SCUpdateDescriptionsData(
        input=SCUpdateDescriptionsDataInput(
            carrier_code='carrier_code_2',
            car_type_code='car_type_code_2',
            sc_code='sc_code_2',
            url='url_2',
        ),
        output=SCUpdateDescriptionsDataOutput(
            result=SCDescriptionResult.ACTUAL,
            sc_description='',
        ),
        info=TASK_INFO,
    ),
    # has description UPDATED
    SCUpdateDescriptionsData(
        input=SCUpdateDescriptionsDataInput(
            carrier_code='carrier_code_3',
            car_type_code='car_type_code_3',
            sc_code='sc_code_3',
            url='url_3',
        ),
        output=SCUpdateDescriptionsDataOutput(
            result=SCDescriptionResult.UPDATED,
            data_source='data_source_3_1',
            sc_description='sc_description_3_1',
            sc_description_original='sc_description_original_3_1',
        ),
        info=TASK_INFO,
    ),
    # has description NO_DATA
    SCUpdateDescriptionsData(
        input=SCUpdateDescriptionsDataInput(
            carrier_code='carrier_code_4',
            car_type_code='car_type_code_4',
            sc_code='sc_code_4',
            url='url_4',
        ),
        output=SCUpdateDescriptionsDataOutput(
            result=SCDescriptionResult.NO_DATA,
            sc_description='',
        ),
        info=TASK_INFO,
    ),
    # has description UPDATED is_source_official == True
    SCUpdateDescriptionsData(
        input=SCUpdateDescriptionsDataInput(
            carrier_code='carrier_code_5',
            car_type_code='car_type_code_5',
            sc_code='sc_code_5',
            url='url_5',
        ),
        output=SCUpdateDescriptionsDataOutput(
            result=SCDescriptionResult.UPDATED,
            is_source_official=True,
            sc_description='sc_description_5_1',
            sc_description_original='sc_description_original_5_1',
        ),
        info=TASK_INFO,
    ),
]


def test_run(helper: Helper):
    updater = helper.get_updater(SCUpdateDescriptionsStageConfig, start_ts=START_TS)
    helper.prepare_storage(sc_descriptions=STORAGE_DESCRIPTIONS)
    updater_args = helper.get_updater_args(SCUpdateDescriptionsStageConfig.name)
    output_path, _ = updater_args

    processor_result_path = helper.persistence_manager.join(output_path, 'descriptions')
    processor_result = (dc_to_dict(pr) for pr in PROCESSOR_RESULT)
    helper.persistence_manager.write(
        processor_result_path, processor_result, get_dc_yt_schema(SCUpdateDescriptionsData)
    )

    updater.run(*updater_args)

    # log data
    exp_task_info: DescriptionTaskInfo = TASK_INFO
    exp_task_info.stage = SCUpdateDescriptionsStageConfig.name

    table_name = f'sc_descriptions {ts_to_str_msk_tz(START_TS)}'
    logs_table = helper.persistence_manager.join(helper.path_info.logs_path, 'task_info', table_name)
    for log_rec, result in zip(helper.persistence_manager.read(logs_table), PROCESSOR_RESULT):
        task_info: DescriptionTaskInfo = dc_from_dict(DescriptionTaskInfo, log_rec)
        assert result.input.carrier_code == task_info.carrier_code
        assert result.input.car_type_code == task_info.car_type_code
        assert result.input.sc_code == task_info.sc_code

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

    assert 6 == len(storage.sc_descriptions)

    # not sent to stage
    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_0', 'car_type_code_0', 'sc_code_0')]
    assert SCDescriptionResult.UPDATED == description.sc_description_result
    assert 'data_source_0_0' == description.data_source
    assert 'sc_description_0_0' == description.sc_description
    assert 'sc_description_original_0_0' == description.sc_description_original
    assert StageStatus.NOTHING_TO_DO == description.status_sc_update_descriptions

    # no description
    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_1', 'car_type_code_1', 'sc_code_1')]
    assert SCDescriptionResult.UPDATED == description.sc_description_result
    assert 'data_source_1_1' == description.data_source
    assert 'sc_description_1_0' == description.sc_description
    assert 'sc_description_original_1_0' == description.sc_description_original
    exp_value = [{'car_numbers': '1|2|5', 'description': 'sc_description_specific_1_0'}]
    assert exp_value == description.sc_description_specific
    assert StageStatus.NOTHING_TO_DO == description.status_sc_update_descriptions

    # has description ACTUAL
    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_2', 'car_type_code_2', 'sc_code_2')]
    assert SCDescriptionResult.ACTUAL == description.sc_description_result
    assert 'data_source_2_0' == description.data_source
    assert 'sc_description_2_0' == description.sc_description
    assert 'sc_description_original_2_0' == description.sc_description_original
    assert StageStatus.NOTHING_TO_DO == description.status_sc_update_descriptions

    # has description UPDATED
    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_3', 'car_type_code_3', 'sc_code_3')]
    assert SCDescriptionResult.UPDATED == description.sc_description_result
    assert 'data_source_3_1' == description.data_source
    assert 'sc_description_3_1' == description.sc_description
    assert 'sc_description_original_3_1' == description.sc_description_original
    assert StageStatus.NOTHING_TO_DO == description.status_sc_update_descriptions

    # has description NO_DATA
    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_4', 'car_type_code_4', 'sc_code_4')]
    assert SCDescriptionResult.NO_DATA == description.sc_description_result
    assert 'data_source_4_0' == description.data_source
    assert 'sc_description_4_0' == description.sc_description
    assert 'sc_description_original_4_0' == description.sc_description_original
    assert StageStatus.NOTHING_TO_DO == description.status_sc_update_descriptions

    # has description UPDATED is_source_official == true
    description = storage.sc_descriptions[SCDescriptionKey('carrier_code_5', 'car_type_code_5', 'sc_code_5')]
    assert SCDescriptionResult.UPDATED == description.sc_description_result
    assert 'url_5' == description.data_source
    assert 'sc_description_5_1' == description.sc_description
    assert 'sc_description_original_5_1' == description.sc_description_original
    assert StageStatus.NOTHING_TO_DO == description.status_sc_update_descriptions

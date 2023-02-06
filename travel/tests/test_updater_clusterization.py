# -*- coding: utf-8 -*-

from datetime import datetime
# noinspection PyUnresolvedReferences
import pytest

# noinspection PyUnresolvedReferences
from conftest import UpdaterChecker, Helper

from travel.hotels.content_manager.config.stage_config import ClusterizationStageConfig
from travel.hotels.content_manager.data_model.options import StageOptions
from travel.hotels.content_manager.data_model.stage import (
    ClusterizationData, ClusterizationDataInput, ClusterizationDataOutput, ClusterizationDataOutputResult,
    PermalinkTaskInfo
)
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import AssigneeSkill, StageResult, uint
from travel.hotels.content_manager.lib.attributes import Attributes
from travel.hotels.content_manager.lib.common import dc_from_dict, dc_to_dict, get_dc_yt_schema, ts_to_str_msk_tz


START_TS = uint(int(datetime(2019, 10, 25).timestamp()))
DELAY = 5

PERMALINKS_WL = [
    StoragePermalinkWL(
        permalink=uint(0),
        required_stages='clusterization',
        status_clusterization=StageStatus.IN_PROCESS,
        grouping_key='grouping_key',
        priority=uint(10),
        clusterization_iteration=uint(1),
        comments='comments_0',
    ),
    StoragePermalinkWL(
        permalink=uint(1),
        required_stages='clusterization',
        status_clusterization=StageStatus.IN_PROCESS,
        grouping_key='grouping_key',
        priority=uint(10),
        clusterization_iteration=uint(1),
        comments='comments_1',
    ),
    StoragePermalinkWL(
        permalink=uint(2),
        required_stages='clusterization',
        status_clusterization=StageStatus.IN_PROCESS,
        grouping_key='grouping_key',
        priority=uint(10),
        clusterization_iteration=uint(1),
        comments='comments_2',
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
    # SUCCESS
    ClusterizationData(
        input=ClusterizationDataInput(
            permalink='0',
        ),
        output=ClusterizationDataOutput(
            result=ClusterizationDataOutputResult.SUCCESS,
            permalink='0',
            assignee_skill=AssigneeSkill.BASIC.value,
            comments='output_comments_0',
        ),
        info=TASK_INFO,
    ),
    # FAILED
    ClusterizationData(
        input=ClusterizationDataInput(
            permalink='1',
        ),
        output=ClusterizationDataOutput(
            result=ClusterizationDataOutputResult.FAILED,
            permalink='1',
            assignee_skill=AssigneeSkill.ADVANCED.value,
            comments='output_comments_1',
        ),
        info=TASK_INFO,
    ),
    # IN_PROCESS
    ClusterizationData(
        input=ClusterizationDataInput(
            permalink='2',
        ),
        output=ClusterizationDataOutput(
            result=ClusterizationDataOutputResult.IN_PROCESS,
            permalink='2',
            assignee_skill=AssigneeSkill.EDIT.value,
            comments='output_comments_2',
        ),
        info=TASK_INFO,
    ),
]


def test_run(helper: Helper):
    options = {
        ClusterizationStageConfig.name: StageOptions(
            triggers=dict(),
            delay=DELAY,
        )
    }
    helper.prepare_storage(permalinks_wl=PERMALINKS_WL)
    updater = helper.get_updater(ClusterizationStageConfig, options=options, start_ts=START_TS)
    updater_args = helper.get_updater_args(ClusterizationStageConfig.name)
    output_path, _ = updater_args

    processor_result_path = helper.persistence_manager.join(output_path, 'hotels')
    processor_result = (dc_to_dict(pr) for pr in PROCESSOR_RESULT)
    helper.persistence_manager.write(processor_result_path, processor_result, get_dc_yt_schema(ClusterizationData))

    updater.run(*updater_args)

    # log data
    exp_task_info: PermalinkTaskInfo = TASK_INFO
    exp_task_info.stage = ClusterizationStageConfig.name

    table_name = f'permalinks {ts_to_str_msk_tz(START_TS)}'
    logs_table = helper.persistence_manager.join(helper.path_info.logs_path, 'task_info', table_name)
    for rec in helper.persistence_manager.read(logs_table):
        task_info: PermalinkTaskInfo = dc_from_dict(PermalinkTaskInfo, rec)
        task_info.permalink = 0
        assert exp_task_info == task_info
        assert isinstance(task_info.reward, float)

    # storage
    storage = helper.get_storage()

    assert 3 == len(storage.permalinks_wl)

    # permalink 0 - success
    permalink = storage.permalinks_wl[0]
    assert StageResult.SUCCESS == permalink.clusterization_result
    assert 'grouping_key' == permalink.grouping_key
    assert 10 == permalink.priority
    assert AssigneeSkill.BASIC == permalink.assignee_skill
    assert 0 == permalink.clusterization_iteration
    assert 'output_comments_0' == permalink.comments
    assert StageStatus.NOTHING_TO_DO == permalink.status_clusterization
    assert START_TS == permalink.status_clusterization_ts
    assert StageStatus.TO_BE_PROCESSED == permalink.status_wl_clusterized_hotels
    assert START_TS == permalink.status_wl_clusterized_hotels_ts

    # permalink 1 - success -> failed
    permalink = storage.permalinks_wl[1]
    assert StageResult.FAILED == permalink.clusterization_result
    assert 'grouping_key' == permalink.grouping_key
    assert 10 == permalink.priority
    assert AssigneeSkill.ADVANCED == permalink.assignee_skill
    assert 0 == permalink.clusterization_iteration
    assert 'output_comments_1' == permalink.comments
    assert StageStatus.NOTHING_TO_DO == permalink.status_clusterization
    assert START_TS == permalink.status_clusterization_ts
    assert StageStatus.TO_BE_PROCESSED == permalink.status_wl_clusterized_hotels
    assert START_TS == permalink.status_wl_clusterized_hotels_ts

    # permalink 2 - in_process
    permalink = storage.permalinks_wl[2]
    assert 'output_comments_2' == permalink.comments
    assert 'grouping_key' == permalink.grouping_key
    assert 10 == permalink.priority
    assert AssigneeSkill.EDIT == permalink.assignee_skill
    assert 2 == permalink.clusterization_iteration
    assert StageStatus.TO_BE_PROCESSED == permalink.status_clusterization
    assert START_TS + DELAY == permalink.status_clusterization_ts
    assert StageStatus.NOTHING_TO_DO == permalink.status_wl_clusterized_hotels
    assert 0 == permalink.status_wl_clusterized_hotels_ts


@pytest.fixture
def checker(helper):
    options = {
        ClusterizationStageConfig.name: StageOptions(
            triggers=dict(),
            delay=DELAY,
        )
    }

    return UpdaterChecker(
        helper=helper,
        stage_config=ClusterizationStageConfig,
        entity_cls=StoragePermalinkWL,
        processor_result_cls=StageResult,
        output_table_name='hotels',
        options=options,
        start_ts=START_TS,
    )


def test_new_permalinks(checker):
    checker.check_permalink_wl(
        input_permalinks=[
            StoragePermalinkWL(
                permalink=uint(0),
                required_stages='clusterization',
                route='clusterization',
                status_clusterization=StageStatus.IN_PROCESS,
                clusterization_iteration=uint(1),
            ),
            StoragePermalinkWL(
                permalink=uint(2),
                required_stages='actualization',
                route='actualization',
                status_actualization=StageStatus.IN_PROCESS,
                actualization_iteration=uint(1),
                comments='previous_comments_2'
            ),
        ],
        processor_result=ClusterizationData(
            input=ClusterizationDataInput(
                permalink='0',
            ),
            output=ClusterizationDataOutput(
                result=ClusterizationDataOutputResult.SUCCESS,
                permalink='0',
                new_permalinks=['1', '2 ', '\n'],
                assignee_skill=AssigneeSkill.ADVANCED.value,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=[
            StoragePermalinkWL(
                permalink=uint(0),
                required_stages='clusterization',
                finished_stages='clusterization',
                route='clusterization,wl_clusterized_hotels',
                required_attributes=Attributes.allowed_attributes,
                assignee_skill=AssigneeSkill.ADVANCED,
                status_clusterization=StageStatus.NOTHING_TO_DO,
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
                status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED,
                status_wl_clusterized_hotels_ts=START_TS,
                clusterization_result=StageResult.SUCCESS,
                comments='output_comments_0',
            ),
            StoragePermalinkWL(
                permalink=uint(1),
                required_stages='clusterization',
                route='clusterization',
                required_attributes=Attributes.allowed_attributes,
                assignee_skill=AssigneeSkill.ADVANCED,
                status_clusterization=StageStatus.TO_BE_PROCESSED,
                status_clusterization_ts=START_TS + DELAY,
                clusterization_iteration=uint(1),
                comments='output_comments_0',
            ),
            StoragePermalinkWL(
                permalink=uint(2),
                required_stages='actualization,clusterization',
                route='actualization',
                status_actualization=StageStatus.IN_PROCESS,
                comments='output_comments_0'
            ),
        ],
    )


def test_changed_permalink_non_existent(checker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='clusterization',
            route='clusterization',
            status_clusterization=StageStatus.IN_PROCESS,
            clusterization_iteration=uint(1),
            hotel_name='hotel_name_0',
        ),
        processor_result=ClusterizationData(
            input=ClusterizationDataInput(
                permalink='0',
            ),
            output=ClusterizationDataOutput(
                result=ClusterizationDataOutputResult.SUCCESS,
                permalink='1',
                assignee_skill=AssigneeSkill.ADVANCED.value,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=[
            StoragePermalinkWL(
                permalink=uint(0),
                route='clusterization',
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
                hotel_name='hotel_name_0',
            ),
            StoragePermalinkWL(
                permalink=uint(1),
                required_stages='clusterization',
                finished_stages='clusterization',
                route='clusterization,wl_clusterized_hotels',
                required_attributes=Attributes.allowed_attributes,
                assignee_skill=AssigneeSkill.ADVANCED,
                status_clusterization=StageStatus.NOTHING_TO_DO,
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
                status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED,
                status_wl_clusterized_hotels_ts=START_TS,
                clusterization_result=StageResult.SUCCESS,
                comments='output_comments_0',
            ),
        ],
    )


def test_changed_permalink_existent(checker):
    checker.check_permalink_wl(
        input_permalinks=[
            StoragePermalinkWL(
                permalink=uint(0),
                priority=uint(10),
                required_stages='clusterization',
                route='clusterization',
                status_clusterization=StageStatus.IN_PROCESS,
                clusterization_iteration=uint(1),
                hotel_name='hotel_name_0',
            ),
            StoragePermalinkWL(
                permalink=uint(1),
                priority=uint(5),
                required_stages='clusterization',
                route='clusterization',
                status_clusterization=StageStatus.IN_PROCESS,
                clusterization_iteration=uint(1),
                hotel_name='hotel_name_1',
            ),
        ],
        processor_result=ClusterizationData(
            input=ClusterizationDataInput(
                permalink='0',
            ),
            output=ClusterizationDataOutput(
                result=ClusterizationDataOutputResult.SUCCESS,
                permalink='1',
                assignee_skill=AssigneeSkill.ADVANCED.value,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=[
            StoragePermalinkWL(
                permalink=uint(0),
                priority=uint(10),
                route='clusterization',
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
                hotel_name='hotel_name_0',
            ),
            StoragePermalinkWL(
                permalink=uint(1),
                priority=uint(10),
                required_stages='clusterization',
                finished_stages='clusterization',
                route='clusterization,wl_clusterized_hotels',
                required_attributes=Attributes.allowed_attributes,
                assignee_skill=AssigneeSkill.ADVANCED,
                status_clusterization=StageStatus.NOTHING_TO_DO,
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
                status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED,
                status_wl_clusterized_hotels_ts=START_TS,
                clusterization_result=StageResult.SUCCESS,
                comments='output_comments_0',
            ),
        ],
    )


def test_actualization_required(checker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='clusterization',
            route='clusterization',
            status_clusterization=StageStatus.IN_PROCESS,
            clusterization_iteration=uint(1),
        ),
        processor_result=ClusterizationData(
            input=ClusterizationDataInput(
                permalink='0',
            ),
            output=ClusterizationDataOutput(
                result=ClusterizationDataOutputResult.SUCCESS,
                stage_actualization_required=True,
                permalink='0',
                assignee_skill=AssigneeSkill.ADVANCED.value,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=[
            StoragePermalinkWL(
                permalink=uint(0),
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
            ),
            StoragePermalinkWL(
                permalink=uint(0),
                required_stages='actualization,clusterization',
                finished_stages='clusterization',
                route='clusterization,actualization',
                required_attributes=Attributes.allowed_attributes,
                assignee_skill=AssigneeSkill.ADVANCED,
                status_actualization=StageStatus.TO_BE_PROCESSED,
                status_actualization_ts=START_TS,
                status_clusterization=StageStatus.NOTHING_TO_DO,
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
                clusterization_result=StageResult.SUCCESS,
                comments='output_comments_0',
            ),
        ],
    )


def test_call_center_required(checker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='clusterization',
            route='clusterization',
            status_clusterization=StageStatus.IN_PROCESS,
            clusterization_iteration=uint(1),
        ),
        processor_result=ClusterizationData(
            input=ClusterizationDataInput(
                permalink='0',
            ),
            output=ClusterizationDataOutput(
                result=ClusterizationDataOutputResult.SUCCESS,
                stage_call_center_required=True,
                permalink='0',
                assignee_skill=AssigneeSkill.ADVANCED.value,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=[
            StoragePermalinkWL(
                permalink=uint(0),
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
            ),
            StoragePermalinkWL(
                permalink=uint(0),
                required_stages='call_center,clusterization',
                finished_stages='clusterization',
                route='clusterization,call_center',
                required_attributes=Attributes.allowed_attributes,
                assignee_skill=AssigneeSkill.ADVANCED,
                status_call_center=StageStatus.TO_BE_PROCESSED,
                status_call_center_ts=START_TS,
                status_clusterization=StageStatus.NOTHING_TO_DO,
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
                clusterization_result=StageResult.SUCCESS,
                comments='output_comments_0',
            ),
        ],
    )


def test_actualization_and_call_center_required(checker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='clusterization',
            route='clusterization',
            status_clusterization=StageStatus.IN_PROCESS,
            clusterization_iteration=uint(1),
        ),
        processor_result=ClusterizationData(
            input=ClusterizationDataInput(
                permalink='0',
            ),
            output=ClusterizationDataOutput(
                result=ClusterizationDataOutputResult.SUCCESS,
                stage_actualization_required=True,
                stage_call_center_required=True,
                permalink='0',
                assignee_skill=AssigneeSkill.ADVANCED.value,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=[
            StoragePermalinkWL(
                permalink=uint(0),
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
            ),
            StoragePermalinkWL(
                permalink=uint(0),
                required_stages='call_center,clusterization',
                finished_stages='clusterization',
                call_center_required_stages='actualization',
                route='clusterization,actualization',
                required_attributes=Attributes.allowed_attributes,
                assignee_skill=AssigneeSkill.ADVANCED,
                status_actualization=StageStatus.TO_BE_PROCESSED,
                status_actualization_ts=START_TS,
                status_clusterization=StageStatus.NOTHING_TO_DO,
                status_clusterization_ts=START_TS,
                clusterization_iteration=uint(0),
                clusterization_result=StageResult.SUCCESS,
                comments='output_comments_0',
            ),
        ],
    )

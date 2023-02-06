# -*- coding: utf-8 -*-

from datetime import datetime
# noinspection PyUnresolvedReferences
import pytest

# noinspection PyUnresolvedReferences
from conftest import UpdaterChecker, Helper

from travel.hotels.content_manager.config.stage_config import ActualizationStageConfig
from travel.hotels.content_manager.data_model.stage import (
    ActualizationData, ActualizationDataInput, ActualizationDataOutput, ActualizationDataOutputResult, PermalinkTaskInfo
)
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import ActualizableAttribute, AssigneeSkill, StageResult, uint


START_TS = uint(int(datetime(2019, 10, 25).timestamp()))

TASK_INFO = PermalinkTaskInfo(
    assignment_id='assignment_id_0',
    create_ts=uint(START_TS),
    pool_id='',
    reward=1000,
    status='ACCEPTED',
    submit_ts=uint(START_TS + 1000),
    worker_id='worker_id_0',
)


@pytest.fixture
def checker(helper: Helper):
    return UpdaterChecker(
        helper=helper,
        stage_config=ActualizationStageConfig,
        entity_cls=StoragePermalinkWL,
        processor_result_cls=StageResult,
        output_table_name='assignments',
        start_ts=START_TS,
    )


def test_success(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            required_attributes='contacts,features,location',
            checked_attributes='location',
            status_actualization=StageStatus.IN_PROCESS,
            actualization_iteration=uint(1),
            comments='permalink_comments_0',
        ),
        processor_result=ActualizationData(
            input=ActualizationDataInput(
                permalink='0',
            ),
            output=ActualizationDataOutput(
                result=ActualizationDataOutputResult.SUCCESS,
                checked_attributes=[
                    ActualizableAttribute.CONTACTS,
                    ActualizableAttribute.FEATURES,
                ],
                assignee_skill=AssigneeSkill.ADVANCED.value,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_attributes='contacts,features,location',
            checked_attributes='contacts,features,location',
            required_stages='actualization',
            finished_stages='actualization',
            assignee_skill=AssigneeSkill.ADVANCED,
            status_actualization=StageStatus.NOTHING_TO_DO,
            status_actualization_ts=START_TS,
            status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED,
            status_wl_clusterized_hotels_ts=START_TS,
            actualization_iteration=uint(0),
            actualization_result=StageResult.SUCCESS,
            comments='output_comments_0',
        ),
    )


def test_in_process_clusterization(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            status_actualization=StageStatus.IN_PROCESS,
            actualization_iteration=uint(1),
        ),
        processor_result=ActualizationData(
            input=ActualizationDataInput(
                permalink='0',
            ),
            output=ActualizationDataOutput(
                result=ActualizationDataOutputResult.STAGE_CLUSTERIZATION_REQUIRED,
                assignee_skill=AssigneeSkill.ADVANCED.value,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            finished_stages='',
            actualization_required_stages='clusterization',
            assignee_skill=AssigneeSkill.ADVANCED,
            status_actualization=StageStatus.NOTHING_TO_DO,
            status_actualization_ts=START_TS,
            status_clusterization=StageStatus.TO_BE_PROCESSED,
            status_clusterization_ts=START_TS,
            actualization_iteration=uint(2),
            comments='output_comments_0',
        ),
    )


def test_in_process_call_center(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            required_attributes='contacts',
            status_actualization=StageStatus.IN_PROCESS,
            actualization_iteration=uint(1),
        ),
        processor_result=ActualizationData(
            input=ActualizationDataInput(
                permalink='0',
            ),
            output=ActualizationDataOutput(
                result=ActualizationDataOutputResult.STAGE_CALL_CENTER_REQUIRED,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            finished_stages='',
            required_attributes='contacts',
            actualization_required_stages='call_center',
            status_actualization=StageStatus.NOTHING_TO_DO,
            status_actualization_ts=START_TS,
            status_call_center=StageStatus.TO_BE_PROCESSED,
            call_center_iteration=uint(0),
            status_call_center_ts=START_TS,
            actualization_iteration=uint(2),
            comments='output_comments_0',
        ),
    )


def test_failed(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            status_actualization=StageStatus.IN_PROCESS,
            actualization_iteration=uint(1),
        ),
        processor_result=ActualizationData(
            input=ActualizationDataInput(
                permalink='0',
            ),
            output=ActualizationDataOutput(
                result=ActualizationDataOutputResult.FAILED,
                comments='output_comments_0',
            ),
            info=TASK_INFO,
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            finished_stages='actualization',
            status_actualization=StageStatus.NOTHING_TO_DO,
            status_actualization_ts=START_TS,
            actualization_iteration=uint(0),
            actualization_result=StageResult.FAILED,
            status_wl_clusterized_hotels=StageStatus.TO_BE_PROCESSED,
            status_wl_clusterized_hotels_ts=START_TS,
            comments='output_comments_0',
        ),
    )

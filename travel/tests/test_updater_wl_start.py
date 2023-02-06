# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
import pytest

# noinspection PyUnresolvedReferences
from conftest import UpdaterChecker, Helper

from travel.hotels.content_manager.config.stage_config import WLStartStageConfig
from travel.hotels.content_manager.data_model.stage import PermalinkTaskInfo, WLStartData
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import (
    AssigneeSkill, ActualizationStartReason, ClusterizationStartReason, StageResult, uint
)
from travel.hotels.content_manager.lib.attributes import Attributes


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
        stage_config=WLStartStageConfig,
        entity_cls=StoragePermalinkWL,
        processor_result_cls=StageResult,
        output_table_name='permalinks',
        start_ts=START_TS,
    )


def test_non_existent(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=[],
        processor_result=WLStartData(
            permalink=uint(1),
            required_stages='clusterization',
            hotel_name='hotel_name_1',
            grouping_key='grouping_key_1',
            priority=uint(51),
            actualization_start_reason=ActualizationStartReason.RUBRIC,
            clusterization_start_reason=ClusterizationStartReason.UPDATE,
            clusterization_iteration=uint(3),
            assignee_skill=AssigneeSkill.ADVANCED,
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(1),
            required_stages='clusterization',
            finished_stages='',
            route='clusterization',
            clusterization_required_stages='',
            hotel_name='hotel_name_1',
            actualization_start_reason=ActualizationStartReason.RUBRIC,
            clusterization_start_reason=ClusterizationStartReason.UPDATE,
            assignee_skill=AssigneeSkill.ADVANCED,
            clusterization_iteration=uint(3),
            clusterization_result=StageResult.UNKNOWN,
            comments='',
            grouping_key='grouping_key_1',
            priority=uint(51),
            required_attributes=Attributes.allowed_attributes,
            status_clusterization=StageStatus.TO_BE_PROCESSED,
            status_clusterization_ts=START_TS,
            status_wl_clusterized_hotels=StageStatus.NOTHING_TO_DO,
            status_wl_clusterized_hotels_ts=uint(0),
        ),
    )


def test_existent_in_process(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='clusterization',
            finished_stages='',
            route='clusterization',
            clusterization_required_stages='',
            hotel_name='hotel_name_0',
            clusterization_start_reason=ClusterizationStartReason.COMMON,
            assignee_skill=AssigneeSkill.BASIC,
            clusterization_iteration=uint(1),
            clusterization_result=StageResult.UNKNOWN,
            grouping_key='grouping_key_0',
            priority=uint(50),
            status_clusterization=StageStatus.IN_PROCESS,
            status_clusterization_ts=uint(0),
            status_wl_clusterized_hotels=StageStatus.NOTHING_TO_DO,
            status_wl_clusterized_hotels_ts=uint(0),
        ),
        processor_result=WLStartData(
            permalink=uint(0),
            required_stages='actualization',
            grouping_key='grouping_key_0_1',
            priority=uint(55),
            actualization_start_reason=ActualizationStartReason.PUBLISHING_STATUS,
            clusterization_start_reason=ClusterizationStartReason.BOY_HOTELS,
            hotel_name='hotel_name_0',
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization,clusterization',
            finished_stages='',
            route='clusterization,actualization',
            clusterization_required_stages='',
            hotel_name='hotel_name_0',
            actualization_start_reason=ActualizationStartReason.PUBLISHING_STATUS,
            clusterization_start_reason=ClusterizationStartReason.BOY_HOTELS,
            assignee_skill=AssigneeSkill.BASIC,
            clusterization_iteration=uint(1),
            clusterization_result=StageResult.UNKNOWN,
            grouping_key='grouping_key_0_1',
            priority=uint(55),
            status_actualization=StageStatus.TO_BE_PROCESSED,
            status_actualization_ts=START_TS,
            status_clusterization=StageStatus.IN_PROCESS,
            status_wl_clusterized_hotels=StageStatus.NOTHING_TO_DO,
        ),
    )


def test_existent_finished(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='clusterization',
            finished_stages='clusterization',
            route='wl_start',
            clusterization_required_stages='',
            hotel_name='hotel_name_0',
            clusterization_start_reason=ClusterizationStartReason.COMMON,
            assignee_skill=AssigneeSkill.BASIC,
            clusterization_iteration=uint(1),
            clusterization_result=StageResult.UNKNOWN,
            grouping_key='grouping_key_0',
            priority=uint(50),
        ),
        processor_result=WLStartData(
            permalink=uint(0),
            required_stages='actualization',
            grouping_key='grouping_key_0_1',
            priority=uint(55),
            actualization_start_reason=ActualizationStartReason.PUBLISHING_STATUS,
            clusterization_start_reason=ClusterizationStartReason.BOY_HOTELS,
            hotel_name='hotel_name_0',
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            finished_stages='',
            route='actualization',
            clusterization_required_stages='',
            hotel_name='hotel_name_0',
            actualization_start_reason=ActualizationStartReason.PUBLISHING_STATUS,
            clusterization_start_reason=ClusterizationStartReason.BOY_HOTELS,
            assignee_skill=AssigneeSkill.BASIC,
            clusterization_iteration=uint(1),
            clusterization_result=StageResult.UNKNOWN,
            grouping_key='grouping_key_0_1',
            priority=uint(55),
            required_attributes=Attributes.allowed_attributes,
            status_actualization=StageStatus.TO_BE_PROCESSED,
            status_actualization_ts=START_TS,
        ),
    )


def test_failed_finished(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization,clusterization',
            finished_stages='clusterization',
            route='clusterization',
            clusterization_required_stages='',
            hotel_name='hotel_name_0',
            clusterization_start_reason=ClusterizationStartReason.COMMON,
            assignee_skill=AssigneeSkill.BASIC,
            clusterization_iteration=uint(1),
            clusterization_result=StageResult.FAILED,
            grouping_key='grouping_key_0',
            priority=uint(50),
            status_clusterization=StageStatus.NOTHING_TO_DO,
            status_clusterization_ts=uint(0),
            status_wl_clusterized_hotels=StageStatus.NOTHING_TO_DO,
            status_wl_clusterized_hotels_ts=uint(0),
        ),
        processor_result=WLStartData(
            permalink=uint(0),
            required_stages='actualization,clusterization',
            actualization_required_stages='clusterization',
            grouping_key='grouping_key_0_1',
            priority=uint(55),
            actualization_start_reason=ActualizationStartReason.PUBLISHING_STATUS,
            clusterization_start_reason=ClusterizationStartReason.BOY_HOTELS,
            hotel_name='hotel_name_0',
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization,clusterization',
            actualization_required_stages='clusterization',
            finished_stages='',
            route='clusterization',
            clusterization_required_stages='',
            hotel_name='hotel_name_0',
            actualization_start_reason=ActualizationStartReason.PUBLISHING_STATUS,
            clusterization_start_reason=ClusterizationStartReason.BOY_HOTELS,
            assignee_skill=AssigneeSkill.BASIC,
            clusterization_iteration=uint(1),
            clusterization_result=StageResult.UNKNOWN,
            grouping_key='grouping_key_0_1',
            priority=uint(55),
            required_attributes=Attributes.allowed_attributes,
            status_actualization=StageStatus.NOTHING_TO_DO,
            status_clusterization=StageStatus.TO_BE_PROCESSED,
            status_clusterization_ts=START_TS,
            status_wl_clusterized_hotels=StageStatus.NOTHING_TO_DO,
        ),
    )

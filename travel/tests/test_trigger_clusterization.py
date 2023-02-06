# -*- coding: utf-8 -*-

from datetime import datetime

# noinspection PyUnresolvedReferences
import pytest
# noinspection PyUnresolvedReferences
from conftest import Helper, TriggerChecker

from travel.hotels.content_manager.config.stage_config import ClusterizationStageConfig
from travel.hotels.content_manager.data_model.stage import ClusterizationDataInput
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import AssigneeSkill, ClusterizationStartReason, uint


START_TS = int(datetime(2019, 10, 25).timestamp())


@pytest.fixture
def checker(helper: Helper) -> TriggerChecker:
    return TriggerChecker(
        helper=helper,
        stage_config=ClusterizationStageConfig,
        entity_cls=StoragePermalinkWL,
        input_data_cls=ClusterizationDataInput,
        input_table_name='hotels',
        start_ts=START_TS,
    )


def test_iteration_1(checker: TriggerChecker):
    checker.check_permalink_wl(
        input_permalink=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            requirements='',
            hotel_name='hotel_name_0',
            assignee_skill=AssigneeSkill.ADVANCED,
            comments='',
            clusterization_iteration=uint(1),
            clusterization_start_reason=ClusterizationStartReason.COMMON,
            grouping_key='group_0',
            status_clusterization=StageStatus.TO_BE_PROCESSED,
        ),
        expected_permalink=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            requirements='',
            hotel_name='hotel_name_0',
            assignee_skill=AssigneeSkill.ADVANCED,
            comments='',
            clusterization_iteration=uint(1),
            clusterization_start_reason=ClusterizationStartReason.COMMON,
            grouping_key='group_0',
            status_clusterization=StageStatus.IN_PROCESS,
            status_clusterization_ts=uint(START_TS),
        ),
        expected_input=ClusterizationDataInput(
            permalink='0',
            altay_url='https://altay.yandex-team.ru/cards/perm/0',
            requirements=[''],
            prev_comments=[''],
            hotel_name='hotel_name_0',
            stage_actualization_required=True,
            stage_call_center_required=False,
        ),
    )


def test_iteration_2(checker: TriggerChecker):
    checker.check_permalink_wl(
        input_permalink=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            requirements='requirements_0\nrequirements_1',
            hotel_name='hotel_name_0',
            assignee_skill=AssigneeSkill.ADVANCED,
            comments='comments_0\ncomments_1',
            clusterization_iteration=uint(2),
            clusterization_start_reason=ClusterizationStartReason.COMMON,
            grouping_key='group_0',
            status_clusterization=StageStatus.TO_BE_PROCESSED,
        ),
        expected_permalink=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            requirements='requirements_0\nrequirements_1',
            hotel_name='hotel_name_0',
            assignee_skill=AssigneeSkill.ADVANCED,
            comments='comments_0\ncomments_1',
            clusterization_iteration=uint(2),
            clusterization_start_reason=ClusterizationStartReason.COMMON,
            grouping_key='group_0',
            status_clusterization=StageStatus.IN_PROCESS,
            status_clusterization_ts=uint(START_TS),
        ),
        expected_input=ClusterizationDataInput(
            permalink='0',
            altay_url='https://altay.yandex-team.ru/cards/perm/0',
            requirements=['requirements_0', 'requirements_1'],
            prev_comments=['comments_0', 'comments_1'],
            hotel_name='hotel_name_0',
            assignee_skill=AssigneeSkill.ADVANCED,
            stage_actualization_required=True,
            stage_call_center_required=False,
        ),
    )

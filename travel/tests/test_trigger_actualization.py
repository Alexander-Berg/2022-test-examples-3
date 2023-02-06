# -*- coding: utf-8 -*-

from datetime import datetime
# noinspection PyUnresolvedReferences
import pytest

# noinspection PyUnresolvedReferences
from conftest import Helper, TriggerChecker

from travel.hotels.content_manager.config.stage_config import ActualizationStageConfig
from travel.hotels.content_manager.data_model.stage import ActualizationDataInput
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import AssigneeSkill, ActualizationStartReason, uint


START_TS = uint(int(datetime(2019, 10, 25).timestamp()))


@pytest.fixture
def checker(helper: Helper) -> TriggerChecker:
    return TriggerChecker(
        helper=helper,
        stage_config=ActualizationStageConfig,
        entity_cls=StoragePermalinkWL,
        input_data_cls=ActualizationDataInput,
        input_table_name='assignments',
        start_ts=START_TS,
    )


def test_all_fields(checker: TriggerChecker):
    checker.check_permalink_wl(
        input_permalink=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            requirements='make all good\nbe happy',
            required_attributes='publishing_status,rubrics,contacts',
            checked_attributes='rubrics',
            hotel_name='hotel_name_0',
            assignee_skill=AssigneeSkill.ADVANCED,
            comments='huston\nwe have a problem',
            clusterization_iteration=uint(1),
            actualization_start_reason=ActualizationStartReason.RUBRIC,
            grouping_key='group_0',
            status_actualization=StageStatus.TO_BE_PROCESSED,
        ),
        expected_permalink=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            requirements='make all good\nbe happy',
            required_attributes='publishing_status,rubrics,contacts',
            checked_attributes='rubrics',
            hotel_name='hotel_name_0',
            assignee_skill=AssigneeSkill.ADVANCED,
            comments='huston\nwe have a problem',
            clusterization_iteration=uint(1),
            actualization_start_reason=ActualizationStartReason.RUBRIC,
            grouping_key='group_0',
            status_actualization=StageStatus.IN_PROCESS,
            status_actualization_ts=START_TS,
        ),
        expected_input=ActualizationDataInput(
            permalink='0',
            altay_url='https://altay.yandex-team.ru/cards/perm/0',
            required_attributes=['contacts', 'publishing_status'],
            requirements=['make all good', 'be happy'],
            prev_comments=['huston', 'we have a problem'],
            hotel_name='hotel_name_0',
            assignee_skill=AssigneeSkill.ADVANCED,
        ),
    )

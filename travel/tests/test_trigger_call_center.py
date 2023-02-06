# -*- coding: utf-8 -*-

from datetime import datetime
# noinspection PyUnresolvedReferences
import pytest

# noinspection PyUnresolvedReferences
from conftest import Helper, TriggerChecker

from travel.hotels.content_manager.config.stage_config import CallCenterStageConfig
from travel.hotels.content_manager.data_model.stage import CallCenterDataInput
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import uint


START_TS = uint(int(datetime(2019, 10, 25).timestamp()))


@pytest.fixture
def checker(helper: Helper) -> TriggerChecker:
    return TriggerChecker(
        helper=helper,
        stage_config=CallCenterStageConfig,
        entity_cls=StoragePermalinkWL,
        input_data_cls=CallCenterDataInput,
        input_table_name='assignments',
        start_ts=START_TS,
    )


def test_all_fields(checker: TriggerChecker):
    checker.check_permalink_wl(
        input_permalink=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='call_center',
            required_attributes='publishing_status,rubrics,contacts',
            checked_attributes='rubrics',
            clusterization_iteration=uint(1),
            grouping_key='group_0',
            status_call_center=StageStatus.TO_BE_PROCESSED,
            comments='comments_0',
            call_center_request_id=uint(15),
            call_center_iteration=uint(2),
        ),
        expected_permalink=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='call_center',
            required_attributes='publishing_status,rubrics,contacts',
            checked_attributes='rubrics',
            clusterization_iteration=uint(1),
            grouping_key='group_0',
            status_call_center=StageStatus.IN_PROCESS,
            status_call_center_ts=START_TS,
            comments='comments_0',
            call_center_request_id=uint(15),
            call_center_iteration=uint(2),
        ),
        expected_input=CallCenterDataInput(
            permalink=0,
            required_attributes=['contacts', 'publishing_status'],
            comments='comments_0',
            request_id=uint(15),
        ),
    )

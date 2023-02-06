# -*- coding: utf-8 -*-

from datetime import datetime
# noinspection PyUnresolvedReferences
import pytest

# noinspection PyUnresolvedReferences
from conftest import UpdaterChecker, Helper

from travel.hotels.content_manager.config.stage_config import CallCenterStageConfig
from travel.hotels.content_manager.data_model.stage import (
    CallCenterData, CallCenterDataInput, CallCenterDataOutput, CallCenterDataOutputResult, PermalinkTaskInfo
)
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalinkWL
from travel.hotels.content_manager.data_model.types import StageResult, uint


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
        stage_config=CallCenterStageConfig,
        entity_cls=StoragePermalinkWL,
        processor_result_cls=StageResult,
        output_table_name='assignments',
        start_ts=START_TS,
    )


def test_finished_with_call(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            call_center_request_id=uint(0),
            permalink=uint(0),
            required_stages='actualization',
            status_call_center=StageStatus.IN_PROCESS,
            actualization_required_stages='call_center',
            checked_attributes='location',
            comments="phone,address",
            call_center_iteration=uint(1),
        ),
        processor_result=CallCenterData(
            input=CallCenterDataInput(
                request_id=0,
                permalink=0,
                comments="",
            ),
            output=CallCenterDataOutput(
                result=CallCenterDataOutputResult.SUCCESS_WITH_CALL,
            ),
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            checked_attributes='location',
            required_stages='actualization',
            finished_stages='call_center',
            status_call_center=StageStatus.NOTHING_TO_DO,
            status_call_center_ts=START_TS,
            status_actualization=StageStatus.TO_BE_PROCESSED,
            status_actualization_ts=START_TS,
            actualization_required_stages='call_center',
            call_center_iteration=uint(0),
            call_center_result=StageResult.SUCCESS,
            comments="phone,address\nоператор КЦ дозвонился до отеля",
        ),
    )


def test_finished_without_call(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            call_center_request_id=uint(0),
            permalink=uint(0),
            required_stages='actualization',
            status_call_center=StageStatus.IN_PROCESS,
            actualization_required_stages='call_center',
            checked_attributes='location',
            comments="",
            call_center_iteration=uint(1),
        ),
        processor_result=CallCenterData(
            input=CallCenterDataInput(
                request_id=0,
                permalink=0,
                comments="",
            ),
            output=CallCenterDataOutput(
                result=CallCenterDataOutputResult.SUCCESS_WITHOUT_CALL,
            ),
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            checked_attributes='location',
            required_stages='actualization',
            finished_stages='call_center',
            status_call_center=StageStatus.NOTHING_TO_DO,
            status_call_center_ts=START_TS,
            status_actualization=StageStatus.TO_BE_PROCESSED,
            status_actualization_ts=START_TS,
            actualization_required_stages='call_center',
            call_center_iteration=uint(0),
            call_center_result=StageResult.SUCCESS,
            comments="оператор КЦ не дозвонился до отеля"
        ),
    )


def test_failed(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            call_center_request_id=uint(0),
            permalink=uint(0),
            required_stages='actualization',
            actualization_required_stages='call_center',
            status_call_center=StageStatus.IN_PROCESS,
            comments="",
            call_center_iteration=uint(1),
        ),
        processor_result=CallCenterData(
            input=CallCenterDataInput(
                request_id=0,
                permalink=0,
                comments="",
            ),
            output=CallCenterDataOutput(
                result=CallCenterDataOutputResult.FAILED,
            ),
        ),
        expected_permalinks=StoragePermalinkWL(
            call_center_request_id=uint(0),
            permalink=uint(0),
            required_stages='actualization',
            finished_stages='',
            status_call_center=StageStatus.TO_BE_PROCESSED,
            actualization_required_stages='call_center',
            status_call_center_ts=START_TS,
            call_center_result=StageResult.FAILED,
            comments="",
            call_center_iteration=uint(2),
        ),
    )


def test_in_work(checker: UpdaterChecker):
    checker.check_permalink_wl(
        input_permalinks=StoragePermalinkWL(
            call_center_request_id=uint(0),
            permalink=uint(0),
            required_stages='actualization',
            actualization_required_stages='call_center',
            status_call_center=StageStatus.IN_PROCESS,
            comments="",
            call_center_iteration=uint(2),
        ),
        processor_result=CallCenterData(
            input=CallCenterDataInput(
                request_id=0,
                permalink=0,
                comments="",
            ),
            output=CallCenterDataOutput(
                result=CallCenterDataOutputResult.IN_PROCESS,
                request_id=15,
            ),
        ),
        expected_permalinks=StoragePermalinkWL(
            permalink=uint(0),
            required_stages='actualization',
            finished_stages='',
            comments="",
            actualization_required_stages='call_center',
            status_call_center=StageStatus.TO_BE_PROCESSED,
            status_call_center_ts=START_TS,
            call_center_result=StageResult.UNKNOWN,
            call_center_request_id=uint(15),
            call_center_iteration=uint(3)
        ),
    )

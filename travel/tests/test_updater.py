# -*- coding: utf-8 -*-

import dataclasses

# noinspection PyUnresolvedReferences
from conftest import Helper

from travel.hotels.content_manager.config.stage_config import StageConfig
from travel.hotels.content_manager.data_model.storage import StageStatus, StoragePermalink
from travel.hotels.content_manager.data_model.types import StageResult
from travel.hotels.content_manager.lib.updater import Updater


ALL_STAGES = {
    'stage_a',
    'stage_b',
    'stage_c',
    'stage_f',
}


@dataclasses.dataclass
class Entity:
    required_stages: str = ''
    finished_stages: str = ''
    route: str = ''
    stage_a_required_stages: str = ''
    stage_b_required_stages: str = ''
    stage_c_required_stages: str = ''
    stage_f_required_stages: str = ''
    status_stage_a: StageStatus = StageStatus.NOTHING_TO_DO
    status_stage_b: StageStatus = StageStatus.NOTHING_TO_DO
    status_stage_c: StageStatus = StageStatus.NOTHING_TO_DO
    status_stage_f: StageStatus = StageStatus.NOTHING_TO_DO
    stage_a_result: StageResult = StageResult.UNKNOWN
    stage_b_result: StageResult = StageResult.UNKNOWN
    stage_c_result: StageResult = StageResult.UNKNOWN


def check_exact_stages_running(entity: Entity, *args: str) -> None:
    for stage in args:
        status_field = f'status_{stage}'
        actual_value = getattr(entity, status_field)
        if StageStatus.TO_BE_PROCESSED != actual_value:
            raise Exception(f'{status_field}: expected StageStatus.TO_BE_PROCESSED but got {actual_value}')
    for stage in ALL_STAGES - set(args):
        status_field = f'status_{stage}'
        actual_value = getattr(entity, status_field)
        if StageStatus.NOTHING_TO_DO != actual_value:
            raise Exception(f'{status_field}: expected StageStatus.NOTHING_TO_DO but got {actual_value}')


class Config(StageConfig):
    name = 'stage_x'
    updater = Updater


# noinspection PyTypeChecker
def test_dispatching_all_done(helper: Helper):
    updater = helper.get_updater(Config)
    entity: StoragePermalink = Entity()

    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')


# noinspection PyTypeChecker
def test_dispatching_one_required(helper: Helper):
    updater = helper.get_updater(Config)
    entity = Entity(
        required_stages='stage_a',
    )

    entity.status_stage_a = StageStatus.TO_BE_PROCESSED
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_a')

    entity.finished_stages = 'stage_a'
    entity.status_stage_a = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')


# noinspection PyTypeChecker
def test_dispatching_two_parallel(helper: Helper):
    updater = helper.get_updater(Config)
    entity = Entity(
        required_stages='stage_a,stage_b',
    )

    entity.status_stage_a = StageStatus.TO_BE_PROCESSED
    entity.status_stage_b = StageStatus.TO_BE_PROCESSED
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_a', 'stage_b')

    entity.finished_stages = 'stage_a'
    entity.status_stage_a = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_b')
    check_exact_stages_running(entity, 'stage_b')

    entity.finished_stages = 'stage_a,stage_b'
    entity.status_stage_b = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')


# noinspection PyTypeChecker
def test_dispatching_two_sequential(helper: Helper):
    updater = helper.get_updater(Config)
    entity = Entity(
        required_stages='stage_a',
    )

    entity.status_stage_a = StageStatus.TO_BE_PROCESSED
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_a')

    entity.required_stages = 'stage_a,stage_b'
    entity.finished_stages = 'stage_a'
    entity.status_stage_a = StageStatus.NOTHING_TO_DO
    entity.status_stage_b = StageStatus.TO_BE_PROCESSED
    updater.dispatch_entities(Entity, [entity], 'stage_b')
    check_exact_stages_running(entity, 'stage_b')

    entity.finished_stages = 'stage_a,stage_b'
    entity.status_stage_b = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')


# noinspection PyTypeChecker
def test_dispatching_line(helper: Helper):
    updater = helper.get_updater(Config)
    entity = Entity(
        required_stages='stage_b',
        stage_b_required_stages='stage_c',
    )

    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_c')

    entity.finished_stages = 'stage_c'
    entity.status_stage_c = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_b')

    entity.finished_stages = 'stage_b'
    entity.status_stage_b = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')


# noinspection PyTypeChecker
def test_dispatching_skip(helper: Helper):
    updater = helper.get_updater(Config)
    entity = Entity(
        required_stages='stage_b',
        stage_b_required_stages='stage_c',
    )

    entity.status_stage_b = StageStatus.TO_BE_PROCESSED
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_b', 'stage_c')

    entity.finished_stages = 'stage_c'
    entity.status_stage_c = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_b')

    entity.finished_stages = 'stage_b'
    entity.status_stage_b = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')


# noinspection PyTypeChecker
def test_dispatching_converging(helper: Helper):
    updater = helper.get_updater(Config)
    entity = Entity(
        required_stages='stage_a,stage_b',
        stage_a_required_stages='stage_c',
        stage_b_required_stages='stage_c',
    )

    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_c')

    entity.finished_stages = 'stage_c'
    entity.status_stage_c = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_a', 'stage_b')

    entity.finished_stages = 'stage_a,stage_c'
    entity.status_stage_a = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_b')

    entity.finished_stages = 'stage_a,stage_b,stage_c'
    entity.status_stage_b = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')


# noinspection PyTypeChecker
def test_dispatching_diverging(helper: Helper):
    updater = helper.get_updater(Config)
    entity = Entity(
        required_stages='stage_a',
        stage_a_required_stages='stage_b,stage_c',
    )

    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_b', 'stage_c')

    entity.finished_stages = 'stage_b'
    entity.status_stage_b = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_c')

    entity.finished_stages = 'stage_b,stage_c'
    entity.status_stage_c = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_a')

    entity.finished_stages = 'stage_a,stage_b,stage_c'
    entity.status_stage_a = StageStatus.NOTHING_TO_DO
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')


# noinspection PyTypeChecker
def test_dispatching_failed(helper: Helper):
    updater = helper.get_updater(Config)
    entity = Entity(
        required_stages='stage_b',
        stage_b_required_stages='stage_c',
    )

    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_c')

    entity.finished_stages = 'stage_c'
    entity.status_stage_c = StageStatus.NOTHING_TO_DO
    entity.stage_c_result = StageResult.FAILED
    updater.dispatch_entities(Entity, [entity], 'stage_f')
    check_exact_stages_running(entity, 'stage_f')

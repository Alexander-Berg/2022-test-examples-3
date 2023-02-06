# coding: utf-8

"""
В тестах проверяется корректность работы утилиты market/idx/models/bin/model_transitions_dumper,
которая по заданным на входе таблицам, приходящим от МБО (пример таблиц можно посмотреть здесь:
//home/market/production/mstat/dictionaries/mbo/model_transitions/*, причем как на Арнольде, так и на
Хане), готовит pb-файлик формата market/proto/indexer/entity_mapper.proto::TEntityMapper
Узнать, зачем нужен этот проект, можно здесь:
https://wiki.yandex-team.ru/users/myhellsing/Proekty-v-infre-2018/Pereezdy-id-shnikov/Texnicheskie-detali/
"""

import os
import pytest

from hamcrest import assert_that, equal_to, has_entries, has_key, is_in
from market.idx.yatf.resources.model_ids import ModelIds
from market.idx.models.yatf.test_envs.model_transitions_dumper import ModelTransitionsDumperTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_tables.entity_transitions_table import EntityTransitionsTable
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def model_transitions(yt_server):
    return EntityTransitionsTable(
        yt_stuff=yt_server,
        path=ypath_join(get_yt_prefix(), 'mstat/dictionaries/mbo/model_transitions/latest'),
        data=[
            {
                'id': 0,
                'old_entity_id': 1,
                'new_entity_id': 2,
                'date': '2019-10-04',
                'primary_transition': True,
            },
            {
                'id': 1,
                'old_entity_id': 10,
                'new_entity_id': 20,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                # Такой маппинг должен отбрасываться, т.к. new_entity_id == NULL
                'id': 2,
                'old_entity_id': 100,
                'new_entity_id': None,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                # Такой маппинг должен отбрасываться, т.к. old_entity_id == NULL
                'id': 3,
                'old_entity_id': None,
                'new_entity_id': 110,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                # Такой маппинг должен отбрасываться, т.к. old_entity_id == new_entity_id
                'id': 4,
                'old_entity_id': 120,
                'new_entity_id': 120,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                # Такой маппинг должен отбрасываться, если включена фильтрация по размеру id (--drop-long-ids),
                # т.к. old_entity_id не влезает в ui32
                'id': 5,
                'old_entity_id': 123,
                'new_entity_id': 4294967296,  # == std::numeric_limits<ui32>::max() + 1
                'date': '2019-10-04',
                'primary_transition': True,
            },
            {
                # Такой маппинг должен отбрасываться, если включена фильтрация по размеру id (--drop-long-ids),
                # т.к. new_entity_id не влезает в ui32
                'id': 6,
                'old_entity_id': 4294967297,  # == std::numeric_limits<ui32>::max() + 2
                'new_entity_id': 456,
                'date': '2019-10-04',
                'primary_transition': True,
            },

            # Реальный кейс из выгрузки #1:
            {
                'id': 99241,
                'old_entity_id': 172748725,
                'new_entity_id': 1971384257,
                'date': '2019-10-04',
                'primary_transition': True,
            },
            {
                'id': 99242,
                'old_entity_id': 172748725,
                'new_entity_id': 566653347,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                'id': 99243,
                'old_entity_id': 172748725,
                'new_entity_id': 566653358,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                'id': 99244,
                'old_entity_id': 172748725,
                'new_entity_id': 436066609,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            # Из этих ^ данных как primary_id должен сохраниться лишь 1 переход: 172748725 -> 1971384257 (самый первый)

            # Реальный кейс из выгрузки #2:
            {
                'id': 99256,
                'old_entity_id': 172748713,
                'new_entity_id': 576489680,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                'id': 99257,
                'old_entity_id': 172748713,
                'new_entity_id': 248432739,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                'id': 99258,
                'old_entity_id': 172748713,
                'new_entity_id': 577955814,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                'id': 99259,
                'old_entity_id': 172748713,
                'new_entity_id': 564224185,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            # Из этих ^ данных как primary_id должен сохраниться лишь 1 (любой)

            # Несколько primary_transition'ов:
            {
                'id': 200,
                'old_entity_id': 40,
                'new_entity_id': 79,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                'id': 200,
                'old_entity_id': 40,
                'new_entity_id': 80,
                'date': '2019-10-04',
                'primary_transition': True,
            },
            {
                'id': 201,
                'old_entity_id': 40,
                'new_entity_id': 81,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                'id': 202,
                'old_entity_id': 40,
                'new_entity_id': 82,
                'date': '2019-10-04',
                'primary_transition': False,
            },
            {
                'id': 203,
                'old_entity_id': 40,
                'new_entity_id': 83,
                'date': '2019-10-08',
                'primary_transition': True,
            },
            # Из этих ^ данных как primary_id должен сохраниться лишь тот, у которого самая поздняя дата (последний)
        ],
    )


@pytest.fixture(scope="module")
def model_ids():
    return ModelIds(
        ids=[
            2,  # old_entity_id: 1

            566653347, 566653358,  # old_entity_id: 172748725

            79, 80,  # old_entity_id: 40

            # Такой id=4294967297 не может появиться в реальном файле, здесь он для проверки логики:
            4294967297,  # == std::numeric_limits<ui32>::max() + 2
            456,
        ],
        blue_ids=[
            20,  # old_entity_id: 10

            81,  # old_entity_id: 40
        ]
    )


@pytest.fixture(scope="module")
def model_transitions_dumper_workflow(yt_server, model_transitions):
    """
    Дефолтное окружение, ничего не фильтрует, проверяет лишь коррекность записей. Сейчас применяется для MSKU.
    """
    resources = {
        'yt_input_table': model_transitions,
    }

    with ModelTransitionsDumperTestEnv(yt_server, **resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.fixture(scope="module")
def model_transitions_dumper_workflow_with_filtering(yt_server, model_transitions, model_ids):
    """
    Создаем окружение, которое фильтрует двумя способами:
        1. Все вхождения по файлику model_ids, т.е. если new_entity_id нет в файли model_ids, то такое вхождение
           отбрасывается (параметр --entity-ids-gz-path).
        2. Все проверяем, что old_entity_id и new_entity_id можно представить типом ui32, если нет, то вхождение
           отбрасывается (параметр --drop-long-ids).
    Сейчас применяется для моделей.
    """
    resources = {
        'yt_input_table': model_transitions,
        'model_ids': model_ids,
    }

    with ModelTransitionsDumperTestEnv(yt_server, **resources) as env:
        env.execute(drop_long_ids=True)
        env.verify()
        yield env


def _check_old_to_new_entry(old_to_new, expected_old_id, allowed_primary_ids, allowed_secondary_ids):
    assert_that(old_to_new, has_key(expected_old_id))
    new_entity = old_to_new[expected_old_id]

    assert_that(new_entity, has_key('primary_id'))
    assert_that(new_entity['primary_id'], is_in(allowed_primary_ids))

    expected_secondary_ids = allowed_secondary_ids - {new_entity['primary_id']}
    assert_that(new_entity, has_key('secondary_id'))
    assert_that(new_entity['secondary_id'], equal_to(expected_secondary_ids))


def test_dumper_has_output(model_transitions_dumper_workflow):
    """
    Проверяем, что дампер в принципе выдает что-то на выходе.
    """
    output_path = model_transitions_dumper_workflow.transitions.path
    assert_that(os.path.exists(output_path))


def test_dumper_has_transitions(model_transitions_dumper_workflow):
    """
    Проверяем, что выходной pb-файлик, полученный на выходе дампера,
    содержит корректные данные.
    """
    transitions = model_transitions_dumper_workflow.transitions

    assert_that(transitions.old_to_new, has_entries({
        # У перехода есть лишь одна строка с флагом primary_transition
        1: {'primary_id': 2, 'secondary_id': set()},

        # У перехода нет строк с primary_transition, поэтому он будет выбран как primary_id:
        10: {'primary_id': 20, 'secondary_id': set()},

        # У перехода есть единственная строка с флагом primary_transition и набор строк без него:
        172748725: {'primary_id': 1971384257, 'secondary_id': {566653347, 566653358, 436066609}},

        123: {'primary_id': 4294967296, 'secondary_id': set()},

        4294967297: {'primary_id': 456, 'secondary_id': set()},
    }))

    # У перехода нет строк с флагом primary_transition, зато много строк без него. В таком случае, за primary_id
    # можно брать любой из secondary_id:
    _check_old_to_new_entry(
        transitions.old_to_new,
        expected_old_id=172748713,
        allowed_primary_ids={576489680, 248432739, 577955814, 564224185},
        allowed_secondary_ids={576489680, 248432739, 577955814, 564224185},
    )

    # У перехода несколько строк с флагом primary_transition. В таком случае, за primary_id берется тот, у которого
    # указана самая поздняя дата:
    _check_old_to_new_entry(
        transitions.old_to_new,
        expected_old_id=40,
        allowed_primary_ids={83},
        allowed_secondary_ids={79, 80, 81, 82},
    )

    # Всего должно быть 7 вхождений в old_to_new:
    assert_that(len(transitions.old_to_new), equal_to(7))

    # Проверяем, что обратное отображение построено корректно:
    assert_that(transitions.new_to_old, equal_to({
        2: 1,
        20: 10,

        1971384257: 172748725,
        566653347: 172748725,
        566653358: 172748725,
        436066609: 172748725,

        564224185: 172748713,
        576489680: 172748713,
        248432739: 172748713,
        577955814: 172748713,

        79: 40,
        80: 40,
        81: 40,
        82: 40,
        83: 40,

        4294967296: 123,
        456: 4294967297,
    }))


def test_dumper_has_transitions_with_filtering(model_transitions_dumper_workflow_with_filtering):
    """
    Проверяем, что выходной pb-файлик, полученный на выходе дампера,
    содержит корректные данные, если в дампер был передан файл model_ids.gz.
    """
    transitions = model_transitions_dumper_workflow_with_filtering.transitions

    assert_that(transitions.old_to_new, has_entries({
        # У перехода есть лишь одна строка с флагом primary_transition
        1: {'primary_id': 2, 'secondary_id': set()},

        # У перехода нет строк с primary_transition, поэтому он будет выбран как primary_id:
        10: {'primary_id': 20, 'secondary_id': set()},

        # Строка с primary_transition и с более поздним new_entity_id будет отфильтрована по model_ids, поэтому
        # выбранный primary_id будет более ранним:
        40: {'primary_id': 80, 'secondary_id': {79, 81}},
    }))

    # Строка c primary_transition будет отфильтрована по model_ids, останутся лишь secondary_ids:
    _check_old_to_new_entry(
        transitions.old_to_new,
        expected_old_id=172748725,
        allowed_primary_ids={566653347, 566653358},
        allowed_secondary_ids={566653347, 566653358},
    )

    # Всего должно быть 4 вхождения в old_to_new, все остальные -- отфильтруются:
    assert_that(len(transitions.old_to_new), equal_to(4))

    # Проверяем, что обратное отображение построено корректно (не содержит отфильтрованных данных):
    assert_that(transitions.new_to_old, equal_to({
        2: 1,
        20: 10,

        566653347: 172748725,
        566653358: 172748725,

        79: 40,
        80: 40,
        81: 40,
    }))

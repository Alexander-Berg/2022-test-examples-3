# coding: utf-8

"""
Тест проверяет построение индекса CatEngine.
На вход подается:
- mmap-файл с профилями моделей (результат стадии препроцессинга),
- файл на YT с IDF. Пример файла,
https://yt.yandex-team.ru/hahn/navigation?path=//home/market/production/yamarec/master/cat_engine/result/report/market_idfs.bin
- путь к индексу. Из него берется файл indexaa, который служит фильтром по моделям, т.е. получаем в итоге
шардированный CatEngine-индекс.

На выходе получаем индекс CatEngine, пригодный для использования.
Подробнее можно почитать здесь https://wiki.yandex-team.ru/advmachine/vsjoprofaktory/catengine/

"""

import os
import pytest

from tempfile import NamedTemporaryFile

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.generation.catengine.yatf.resources.index_table import IndexTable
from market.idx.generation.catengine.yatf.resources.idfs import Idfs
from market.idx.generation.catengine.yatf.resources.model2profile import Model2Profile
from market.idx.generation.catengine.yatf.test_envs.catengine_builder import CatEngineBuilderTestEnv
from market.idx.generation.catengine.yatf.utils.catengine import ECategoryType, CatMachineHyperIds, CatMachineIndexData


LOCAL_ID_0 = 0
LOCAL_ID_1 = 1

MODEL_ID_1 = 1
MODEL_ID_2 = 2
MODEL_ID_20 = 20

MODEL_1 = {
    'model_id': MODEL_ID_1,
    'catentries': [
        {'type': int(ECategoryType.Gender), 'id': 1, 'tf': 0.4},
        {'type': int(ECategoryType.MarketCategoryID), 'id': 10, 'tf': 0.3},
        {'type': int(ECategoryType.HistoryBeruViewModelID), 'id': 100, 'tf': 0.55},
    ]
}

MODEL_20 = {
    'model_id': MODEL_ID_20,
    'catentries': [
        {'type': int(ECategoryType.Gender), 'id': 2, 'tf': 0.6},
        {'type': int(ECategoryType.MarketCategoryID), 'id': 20, 'tf': 0.7},
        {'type': int(ECategoryType.HistoryBeruViewModelID), 'id': 200, 'tf': 0.44},
    ]
}

MODEL2PROFILE = [
    MODEL_1,
    MODEL_20,
]


IDFS = [
    (ECategoryType.Gender, 1, 1.0),
    (ECategoryType.MarketCategoryID, 10, 1.0),
    (ECategoryType.HistoryBeruViewModelID, 100, 1.0),
]


INDEX = [
    {'id': str(MODEL_ID_1), 'local_id': LOCAL_ID_0, 'shard_id': 0},
    {'id': str(MODEL_ID_2), 'local_id': LOCAL_ID_1, 'shard_id': 0},
]


@pytest.fixture(scope='module')
def idfs(yt_server):
    return Idfs(yt_server, IDFS)


@pytest.yield_fixture(scope='module')
def model2profile():
    with NamedTemporaryFile(suffix='.idfs') as f:
        yield Model2Profile(f.name, MODEL2PROFILE)


@pytest.yield_fixture(scope='module')
def index_table(yt_server):
    path = os.path.join(get_yt_prefix(), 'in', 'index')
    return IndexTable(yt_server, path, INDEX)


@pytest.yield_fixture(scope='module')
def catengine_workflow(yt_server, idfs, model2profile):
    resources = {
        'idfs': idfs,
        'model2profile': model2profile,
    }

    with CatEngineBuilderTestEnv(yt_server, index_from_yt=False, **resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def catengine_index_from_yt_workflow(yt_server, idfs, index_table, model2profile):
    resources = {
        'idfs': idfs,
        'model2profile': model2profile,
        'indexaa': index_table,
    }

    with CatEngineBuilderTestEnv(yt_server, index_from_yt=True, **resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def catengine_index_filtered_by_type_workflow(yt_server, idfs, model2profile):
    resources = {
        'idfs': idfs,
        'model2profile': model2profile,
    }

    with CatEngineBuilderTestEnv(yt_server, index_from_yt=False, type=['HistoryBeruViewModelID'], **resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def catengine_index_without_type_filter_workflow(yt_server, idfs, model2profile):
    resources = {
        'idfs': idfs,
        'model2profile': model2profile,
    }

    with CatEngineBuilderTestEnv(yt_server, index_from_yt=False, **resources) as env:
        env.execute()
        env.verify()
        yield env


def test_catengine_indexer_must_include_in_index_model_that_is_in_shard(catengine_workflow):
    reader = CatMachineHyperIds(os.path.join(catengine_workflow.index_dir, 'index.catm_hyper2vec'))
    # В стабе market.idx.generation.catengine/yatf/resources/stubs/indexaa есть модель с идентификатором 1
    assert reader.get_doc_id(MODEL_ID_1) is not None


def test_catengine_indexer_should_not_include_in_index_model_that_is_not_in_shard(catengine_workflow):
    reader = CatMachineHyperIds(os.path.join(catengine_workflow.index_dir, 'index.catm_hyper2vec'))
    # В стабе market.idx.generation.catengine/yatf/resources/stubs/indexaa нет модели с идентификатором 20
    assert reader.get_doc_id(MODEL_ID_20) is None


def test_catengine_indexer_must_include_in_index_model_that_is_in_shard___index_from_yt(catengine_index_from_yt_workflow):
    reader = CatMachineHyperIds(os.path.join(catengine_index_from_yt_workflow.index_dir, 'index.catm_hyper2vec'))
    # В индексе есть модель с идентификатором 1
    assert reader.get_doc_id(MODEL_ID_1) is not None


def test_catengine_indexer_should_not_include_in_index_model_that_is_not_in_shard___index_from_yt(catengine_index_from_yt_workflow):
    reader = CatMachineHyperIds(os.path.join(catengine_index_from_yt_workflow.index_dir, 'index.catm_hyper2vec'))
    # В нет модели с идентификатором 20
    assert reader.get_doc_id(MODEL_ID_20) is None


def test_catengine_indexer_should_include_in_index_one_category(catengine_index_filtered_by_type_workflow):
    index = CatMachineIndexData(catengine_index_filtered_by_type_workflow.index_dir.encode())
    assert index.get_categories_count() == 1


def test_catengine_indexer_should_include_in_index_three_categories(catengine_index_without_type_filter_workflow):
    index = CatMachineIndexData(catengine_index_without_type_filter_workflow.index_dir.encode())
    assert index.get_categories_count() == 3

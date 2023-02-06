# coding: utf-8

"""
Тест проверяет преобразование таблицы профилей моделей, например,
https://yt.yandex-team.ru/hahn/navigation?path=//home/market/production/yamarec/master/cat_engine/result/report/market.profiles
в компактное представление в виде mmap файла, который будет использован для построения CatEngine индекса.

"""

import os
import pytest

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.generation.catengine.yatf.test_envs.catengine_preprocessor import CatEnginePreprocessorTestEnv
from market.idx.generation.catengine.yatf.resources.cat_engine_profile_table import CatEngineProfileTable
from market.idx.generation.catengine.yatf.utils.catengine import ECategoryType


MODEL_ID_1 = 1
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

CAT_ENGINE_PROFILES = [
    MODEL_1,
    MODEL_20,
]


@pytest.fixture(scope='module')
def profiles(yt_server):
    path = os.path.join(get_yt_prefix(), 'in', 'profiles')
    return CatEngineProfileTable(yt_server, path, CAT_ENGINE_PROFILES)


@pytest.yield_fixture(scope='module')
def catengine_workflow(yt_server, profiles):
    resources = {
        'profiles': profiles,
    }

    with CatEnginePreprocessorTestEnv(yt_server, **resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.mark.parametrize('model_id', [MODEL_ID_1, MODEL_ID_20])
def test_preprocessor_should_prepare_profiles_properly_model_ids(catengine_workflow, model_id):
    model2profile = catengine_workflow.outputs.get('model2profile')
    model2profile.load([model_id])
    assert model2profile.profiles[0]['model_id'] == model_id


@pytest.mark.parametrize('model', [MODEL_1, MODEL_20])
def test_preprocessor_should_prepare_profiles_properly_catentries(catengine_workflow, model):
    model2profile = catengine_workflow.outputs.get('model2profile')
    model2profile.load([model['model_id']])
    catentries = list(sorted(model2profile.profiles[0]['catentries'], key=lambda f: f['id']))
    expected_catentries = list(sorted(model['catentries'], key=lambda f: f['id']))
    assert catentries == expected_catentries

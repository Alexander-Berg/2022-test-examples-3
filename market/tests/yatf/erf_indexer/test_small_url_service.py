#!/usr/bin/env python
# coding: utf-8

"""
Проверяется, что в случае присутствия
сервиса (того, что до знака вопроса) в урле
из списка документов, отсутствия его в
урлах из вебовских данных и присутствия
в обоих списках следующего сервиса из урлов
вебовских данных этот следующий сервис
не скипнется.
"""

import pytest

from hamcrest import assert_that, equal_to
from market.proto.report.web_features_pb2 import TErfFeatures, THerfFeatures

from market.idx.generation.yatf.resources.erf_indexer.web_features import WebFeatures
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType
from market.idx.generation.yatf.test_envs.erf_indexer import IndexerfTestEnv
from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv


@pytest.yield_fixture(scope="module")
def offers_index(yt_server):
    offers = [
        {'offer_id': '1', 'url': 'beru.ru/1'},
        {'offer_id': '2', 'url': 'beru.ru/2'},
        {'offer_id': '3', 'url': 'beru.ru/3'},
        {'offer_id': '4', 'url': 'beru.ru/15'},
        {'offer_id': '5', 'url': 'market.yandex.ru/17'},
    ]

    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers)
        build_env.verify()

        resourses = {
            'merge_options': MrMindexerMergeOptions(
                input_portions_path=build_env.yt_index_portions_path,
                part=0,
                index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
            ),
        }

        with MrMindexerMergeTestEnv(**resourses) as env:
            env.execute(yt_server)
            env.verify()
            env.outputs['indexarc'].load()
            yield env


@pytest.yield_fixture(scope="module")
def web_features():
    erfs = [
        [
            TErfFeatures(NormalizedHost='beru.ru',
                         Host='beru.ru',
                         Service='/1',
                         Query='',
                         AddTime=11),
            TErfFeatures(NormalizedHost='beru.ru',
                         Host='beru.ru',
                         Service='/3',
                         Query='',
                         AddTime=15)
        ],
        [
            TErfFeatures(NormalizedHost='market.yandex.ru',
                         Host='market.yandex.ru',
                         Service='/0',
                         Query='',
                         AddTime=2)
        ],
    ]

    herf = [
        THerfFeatures(NormalizedHost='beru.ru',
                      Host='beru.ru',
                      AddTimeMP=4),
        THerfFeatures(NormalizedHost='market.yandex.ru',
                      Host='market.yandex.ru',
                      AddTimeMP=9),
    ]
    return WebFeatures(erfs, ['beru.ru.pbuf.sn', 'market.yandex.ru.pbuf.sn'], herf)


@pytest.yield_fixture(scope="module")
def workflow(offers_index, web_features):
    resources = {
        'web_features': web_features
    }
    with IndexerfTestEnv(**resources) as env:
        env.execute(offers_index.output_dir)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def out_erf(workflow):
    return workflow.outputs.get('erf')


@pytest.fixture(scope='module')
def out_herf(workflow):
    return workflow.outputs.get('herf')


def test_small_url_service(out_erf, out_herf):
    assert_that(out_erf.get_erfs(), equal_to([
        {'AddTime': 11, 'HostId': 0},  # beru.ru/1
        {'AddTime': 0, 'HostId': 0},  # beru.ru/2

        # beru.ru/3; AddTime непустой засчёт фикса MARKETINDEXER-15890
        {'AddTime': 15, 'HostId': 0},

        {'AddTime': 0, 'HostId': 0},  # beru.ru/15
        {'AddTime': 0, 'HostId': 1}  # market.yandex.ru/17
    ]))
    assert_that(out_herf.get_herfs(), equal_to([
        {'AddTimeMP': 4},  # beru.ru
        {'AddTimeMP': 9}  # market.yandex.ru
    ]))

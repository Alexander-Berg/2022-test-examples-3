# coding: utf-8

import time

from hamcrest import assert_that, equal_to

import pytest
from market.idx.models.yatf.test_envs.contex_msku_mmap_dumper import ContexMskuDumperTestEnv
from market.idx.yatf.resources.msku_table import MskuContexTable

from yt.wrapper import ypath_join

SESSION_ID = int(time.time())


def __make_blue_offer(msku, msku_exp, msku_experiment_id, feed, offer_id, experimental_model_id):
    return {
        'msku': msku,
        'msku_exp': msku_exp,
        'msku_experiment_id': msku_experiment_id,
        'experimental_model_id': experimental_model_id,
        'feed_id': feed,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': '',
        'uc': '',
    }


BLUE_OFFERS = [
    __make_blue_offer(msku, msku_exp, msku_experiment_id, feed, offer_id, experimental_model_id)
    for (msku, msku_exp, msku_experiment_id, feed, offer_id, experimental_model_id) in [
        (1, 0, '',        1000, 'offer1', 0),
        (2, 0, '2_3_exp', 2000, 'offer2', 12),
        (3, 2, '2_3_exp', 3000, 'offer3', 13),
    ]
]


@pytest.yield_fixture(scope='module')
def source_msku_contex():
    return [
        {
            'msku': blue_offer['msku'],
            'msku_exp': blue_offer['msku_exp'],
            'msku_experiment_id': blue_offer['msku_experiment_id'],
            'experimental_model_id': blue_offer['experimental_model_id'],
            'feed_id': blue_offer['feed_id'],
            'offer_id': blue_offer['offer_id'],
            'session_id': blue_offer['session_id'],
            'offer': blue_offer['offer'],
            'uc': blue_offer['uc'],
        }
        for blue_offer in BLUE_OFFERS
    ]


@pytest.yield_fixture(scope='module')
def msku_contex_table(yt_server, source_msku_contex):
    return MskuContexTable(
        yt_stuff=yt_server,
        path=ypath_join('some', 'path', 'to', 'msku_contex'),
        data=source_msku_contex,
    )


@pytest.yield_fixture(scope='module')
def contex_msku_dumper_workflow(yt_server, source_msku_contex):
    resources = {
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join('//some', 'path', 'to', 'msku_contex'),
            data=source_msku_contex,
        ),
    }
    with ContexMskuDumperTestEnv(yt_server, **resources) as e:
        e.execute()
        e.verify()
        yield e


def test_simple(contex_msku_dumper_workflow):
    expected = {
        'experimentModelId2baseModelId': [
            {
                'base_msku_id': 3,
                'experiment_msku_id': 2,
                'experimental_model_id': 13,
            }
        ],
        'experiments': [
            {
                'base_msku_id': 3,
                'experiment_id': '2_3_exp',
                'experiment_msku_id': 2
            }
        ]
    }

    assert_that(contex_msku_dumper_workflow.outputs['result'].load(), equal_to(expected))

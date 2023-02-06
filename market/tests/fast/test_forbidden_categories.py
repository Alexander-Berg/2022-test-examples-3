# coding: utf-8

import pytest
from hamcrest import assert_that, contains_inanyorder, equal_to
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller

EXPECTED = [
    90829,
    15797254,
    10682497,
    90925,
    90924,
    90927,
    90942,
    15683167,
    987260,
    283662,
    91216,
    91197,
    17728967,
    90802,
    91441,
    15754673,
    16155381,
    90490,
    7308012,
    10530882,
    91051,
    16033851,
    17725598,
    91735,
]


@pytest.fixture(scope='module')
def stroller(
    config,
    yt_server,
    log_broker_stuff
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff
    ) as stroller:
        yield stroller


def test_forbidden_categories_list(stroller):
    response = stroller.get('/categories/forbidden_for_smb')
    assert_that(response.status_code, equal_to(200))
    assert_that(response.json()['forbidden_categories'], contains_inanyorder(*EXPECTED))

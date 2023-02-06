# coding: utf-8

import json
import pytest
import six
from hamcrest import assert_that, equal_to, has_entry

from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller


@pytest.yield_fixture(scope='module')
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    partners_table,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        shopsdat_cacher=True,
        partners_table=partners_table,
    ) as stroller_env:
        yield stroller_env


def do_request_generate(client, business_id, shop_id, promo_id, format=None):
    return client.post('/shops/{}/generate/promo?promo_id={}&business_id={}{}'.format(
        shop_id,
        promo_id,
        business_id,
        ('&format={}'.format(format) if format else '')
    ))


def test_generate_promo(stroller):
    """Проверяем работоспособность ручки генерации файла с офферам, участвующими в акции"""
    response = do_request_generate(stroller, business_id=1, shop_id=1, promo_id='promo1', format='json')
    assert_that(response, HasStatus(404))
    assert_that(json.loads(response.data), has_entry('Message', 'Failed to generate file'))

    response = do_request_generate(stroller, business_id=1, shop_id=1, promo_id='promo1')
    assert_that(response, HasStatus(404))
    assert_that(six.ensure_str(response.data), equal_to(''))

# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    contains_inanyorder,
    equal_to
)

import logging
import pytest

log = logging.getLogger()


def do_publish(mindexer_clt, generation):
    res = mindexer_clt.execute('async_publish_full', generation, '--force')
    assert_that(res.exit_code, equal_to(0))


def get_generations_by_name(reusable_zk):
    generations = reusable_zk.safe_get_children('/publisher/generations/by_name', default=[])
    log.debug('Content of /publisher/generations/by_name is: %s', generations)
    return generations


@pytest.fixture()
def mindexer_clt(mindexer_clt, request, reusable_mysql, reusable_zk):
    mindexer_clt.env_type = 'production'
    mindexer_clt.make_local_config({})
    mindexer_clt.config.is_production = True
    mindexer_clt.add_generation_to_super('20180101_0101')
    mindexer_clt.add_generation_to_super('20180101_0201')
    mindexer_clt.add_generation_to_super('20180301_0301')

    res = mindexer_clt.execute('make_me_master', '--both', '--no-publish')
    assert_that(res.exit_code, equal_to(0))

    res = mindexer_clt.execute('reconfigure_publisher')
    assert_that(res.exit_code, equal_to(0))

    do_publish(mindexer_clt, '20180101_0101')
    do_publish(mindexer_clt, '20180101_0201')
    do_publish(mindexer_clt, '20180301_0301')

    return mindexer_clt


def test_zk_cleaner(mindexer_clt, reusable_zk):
    assert_that(get_generations_by_name(reusable_zk), contains_inanyorder('20180101_0101', '20180101_0201', '20180301_0301'))

    current_time = 1519863320  # 2018.03.01, 03:15:20
    seconds_per_month = 2628000
    res = mindexer_clt.execute(
        'cleanup_zk',
        '--current-time',
        str(current_time),
        '--delete-older-than',
        str(seconds_per_month)
    )
    assert_that(res.exit_code, equal_to(0))

    assert_that(get_generations_by_name(reusable_zk), equal_to(['20180301_0301']))

# -*- coding: utf-8 -*-

from market.pylibrary.mindexerlib.dist_info_center import create_info_center, InfoCenterKind
from market.pylibrary.mindexerlib import sql
from market.pylibrary import database

from hamcrest import assert_that, empty, contains, has_entries, calling, raises, has_length

from collections import namedtuple
import tempfile


GENERATION_NAME = '20200202_2002'
DIST_NAME = 'search-stats-base-2'
DATACENTER = 'SAS'
RBTORRENT = 'rbtorrent:9a07a8d6dc11df164f83f3c3224a43e2a94a88db'
COPY_TASK_ID = 436215721
SANDBOX_TTL = 42
RESOURCE_ID = 134483949


def test_dummy_info_center():
    Config = namedtuple('Config', [
        'info_center_kind',
        'envtype',
        'mitype',
        'generation_color',
    ])
    config = Config(
        info_center_kind=InfoCenterKind.DUMMY.name,
        envtype='production',
        mitype='stratocaster',
        generation_color='blue',
    )
    dummy_center = create_info_center(config)

    dummy_center.register_dist(GENERATION_NAME, DIST_NAME, DATACENTER)
    dummy_center.update_dist_share_completed(GENERATION_NAME, DIST_NAME, DATACENTER, RBTORRENT)
    dummy_center.update_dist_copy_started(GENERATION_NAME, DIST_NAME, DATACENTER, COPY_TASK_ID, SANDBOX_TTL)
    dummy_center.update_dist_sandbox_ready(GENERATION_NAME, DIST_NAME, DATACENTER, RESOURCE_ID)
    dummy_center.delete_dist(GENERATION_NAME, DIST_NAME, DATACENTER)
    assert_that(dummy_center.get_dist_info_list(), empty())


def test_sql_info_center(reusable_mysql):
    Config = namedtuple('Config', [
        'info_center_kind',
        'datasources',
        'envtype',
        'mitype',
        'generation_color',
    ])

    with tempfile.NamedTemporaryFile() as tmp:
        datasources_path = tmp.name
        reusable_mysql.write_datasources(datasources_path)
        config = Config(
            info_center_kind=InfoCenterKind.SQL.name,
            datasources=database.load_datasources_from_config(datasources_path),
            envtype='production',
            mitype='stratocaster',
            generation_color='blue',
        )
        sql_center = create_info_center(config)
        connection = sql.make_connection_to_super(config)

        with connection.begin():
            assert_that(sql_center.get_dist_info_list(), empty())

            sql_center.register_dist(GENERATION_NAME, DIST_NAME, DATACENTER)
            info_list = sql_center.get_dist_info_list()
            assert_that(info_list, contains(
                has_entries(
                    envtype=config.envtype,
                    mitype=config.mitype,
                    color=config.generation_color,
                    generation_name=GENERATION_NAME,
                    dist_name=DIST_NAME,
                    datacenter=DATACENTER,
                    rbtorrent=None,
                    copy_task_id=None,
                    sandbox_ttl=None,
                    resource_id=None,
                )
            ))

            assert_that(
                calling(sql_center.register_dist).with_args(GENERATION_NAME, DIST_NAME, DATACENTER),
                raises(Exception)
            )

            sql_center.update_dist_share_completed(GENERATION_NAME, DIST_NAME, DATACENTER, RBTORRENT)
            info_list = sql_center.get_dist_info_list()
            assert_that(info_list, contains(
                has_entries(
                    rbtorrent=RBTORRENT,
                )
            ))

            sql_center.update_dist_copy_started(GENERATION_NAME, DIST_NAME, DATACENTER, COPY_TASK_ID, SANDBOX_TTL)
            info_list = sql_center.get_dist_info_list()
            assert_that(info_list, contains(
                has_entries(
                    copy_task_id=COPY_TASK_ID,
                    sandbox_ttl=SANDBOX_TTL,
                )
            ))

            sql_center.update_dist_sandbox_ready(GENERATION_NAME, DIST_NAME, DATACENTER, RESOURCE_ID)
            info_list = sql_center.get_dist_info_list()
            assert_that(info_list, contains(
                has_entries(
                    resource_id=RESOURCE_ID,
                )
            ))

            sql_center.delete_dist(GENERATION_NAME, 'unknown', DATACENTER)
            assert_that(sql_center.get_dist_info_list(), has_length(1))

            sql_center.delete_dist(GENERATION_NAME, DIST_NAME, DATACENTER)
            assert_that(sql_center.get_dist_info_list(), empty())

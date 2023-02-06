#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import pytest
import yatest.common

from datetime import (
    datetime,
    timedelta,
)
from hamcrest import (
    any_of,
    assert_that,
    equal_to,
    greater_than,
    has_item,
    has_items,
    is_not,
)

from yt.wrapper import ypath_join

from market.idx.marketindexer.marketindexer import dump_jump_table
from market.idx.marketindexer.yatf.test_env import MarketIndexer
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub
from market.idx.marketindexer.yatf.resources.common_ini import CommonIni
from market.tools.jump_table_dumper.yatf.resources.jump_table_flat import (
    JumpTableFlat,
)
from market.tools.jump_table_dumper.yatf.resources.jump_table import (
    JumpTable,
)
from market.proto.msku.jump_table_filters_pb2 import JumpTableFilter
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)


def create_jump_table_filter(show_on, values):
    jump_filter = JumpTableFilter()
    jump_filter.show_on = show_on
    for field, value in values:
        value_proto = jump_filter.values.add()
        setattr(value_proto, field, value)
    return jump_filter.SerializeToString()


@pytest.fixture(scope='module')
def jump_table_data():
    return [
        {
            'model_id': 322,
            'param_id': 3221,
            'msku': 1,
            'jump_filter': create_jump_table_filter(
                show_on=1,
                values=[
                    ('value', 111),
                    ('value', 112),
                    ('value', 113),
                ]
            ),
        }, {
            'model_id': 322,
            'param_id': 3221,
            'msku': 2,
            'jump_filter': create_jump_table_filter(
                show_on=1,
                values=[
                    ('value', 112),
                    ('value', 113),
                ]
            ),
        }, {
            'model_id': 322,
            'param_id': 3221,
            'msku': 3,
            'jump_filter': create_jump_table_filter(
                show_on=1,
                values=[
                    ('value', 113),
                    ('value', 111),
                ]
            ),
        }, {
            'model_id': 323,
            'param_id': 3221,
            'msku': 1,
            'jump_filter': create_jump_table_filter(
                show_on=2,
                values=[
                    ('numeric_value', 32.2),
                ]
            ),
        }, {
            'model_id': 323,
            'param_id': 3221,
            'msku': 3,
            'jump_filter': create_jump_table_filter(
                show_on=2,
                values=[
                    ('numeric_value', 32.1),
                    ('numeric_value', 32.2),
                    ('numeric_value', 32.3),
                ]
            ),
        }, {
            'model_id': 323,
            'param_id': 3222,
            'msku': 2,
            'jump_filter': create_jump_table_filter(
                show_on=3,
                values=[
                    ('hypothesis_value', 'some_value'),
                    ('hypothesis_value', 'some_other_value'),
                ]
            ),
        },
    ]


@pytest.fixture(scope='module')
def expected_models():
    return [
        322,
        323,
    ]


@pytest.yield_fixture(scope='module')
def absolute_dumper_bin_path():
    relative_dumper_bin_path = os.path.join(
        'market',
        'tools',
        'jump_table_dumper',
        'bin',
        'dumper',
        'jump-table-dumper',
    )
    return yatest.common.binary_path(relative_dumper_bin_path)


@pytest.yield_fixture(scope='module')
def absolute_reducer_bin_path():
    relative_reducer_bin_path = os.path.join(
        'market',
        'tools',
        'jump_table_dumper',
        'bin',
        'reducer',
        'jump-table-reducer',
    )
    return yatest.common.binary_path(relative_reducer_bin_path)


@pytest.fixture(scope="module")
def old_table_names():
    hours_deltas = range(1, 10)[::-1]
    table_name_pattern = "%Y%m%d_%H%M"
    names = [
        (datetime.now() - timedelta(hours=hour)).strftime(
            table_name_pattern
        ) for hour in hours_deltas
    ]

    return names


def create_old_tables(yt_server, old_table_names):
    yt_dir = ypath_join(get_yt_prefix(), 'in/reduced_jump_table')
    for name in old_table_names:
        client = yt_server.get_yt_client()
        client.create(
            'table',
            os.path.join(yt_dir, name),
            ignore_existing=True,
            recursive=True,
        )

    client.link(
        os.path.join(yt_dir, old_table_names[-1]),
        os.path.join(yt_dir, 'recent'),
    )


@pytest.fixture(scope="module")
def jump_table_dump_dir():
    return os.path.join(yatest.common.work_path(), 'dump_dir')


@pytest.yield_fixture(scope='function')
def workflow(
        yt_server,
        jump_table_data,
        absolute_dumper_bin_path,
        absolute_reducer_bin_path,
        jump_table_dump_dir,
        old_table_names
):
    yt_token_path = os.path.join(yatest.common.work_path(), 'token')
    resources = {
        'input_reduced_jump_table': JumpTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'in/jump_table/recent'),
            data=jump_table_data
        ),
        'yt_token': YtTokenStub(yt_token_path),
        'common_ini': CommonIni(
            os.path.join(yatest.common.work_path(), 'common.ini'),
            yatest.common.work_path(),
            misc={
                'jump_table_dump_enabled': 'true',
                'jump_table_dumper_dir': jump_table_dump_dir,
            },
            yt={
                'home_dir': get_yt_prefix(),
                'yt_proxy_primary': yt_server.get_server(),
                'yt_tokenpath': yt_token_path,
            },
            bin={
                'jump_table_dumper': absolute_dumper_bin_path,
                'jump_table_reducer': absolute_reducer_bin_path,
            },
        ),
    }

    with MarketIndexer(yt_server, **resources) as env:
        create_old_tables(yt_server, old_table_names)
        env.execute(clt_command_args_list=['dump_jump_table'])
        yield env


def test_table_rotations(workflow, yt_server, old_table_names):
    client = yt_server.get_yt_client()
    yt_dir = ypath_join(get_yt_prefix(), 'in/reduced_jump_table')
    table_names_after = client.list(yt_dir)
    keep_count = dump_jump_table.KEEP_COUNT
    expected_deleted = old_table_names[:-keep_count]
    expected_available = old_table_names[-keep_count:]
    assert_that(
        table_names_after,
        has_items(*expected_available),
        'Latest old tables are presented'
    )
    assert_that(
        table_names_after,
        is_not(any_of(
            has_item(name)
            for name in expected_deleted
        )),
        'The oldest tables are removed'
    )


def get_file_modification_time(path):
    return os.path.getmtime(path)


def check_fb_file_content(file_path, expected_models, mark='default'):
    fb_file_data = JumpTableFlat(file_path).load()
    actual_model_ids = [
        entry['ModelContent']['ModelId']
        for entry in fb_file_data
    ]
    assert_that(
        len(fb_file_data),
        equal_to(len(expected_models)),
        'Number of models in fb file matches, check marker: {}'.format(mark)
    )
    assert_that(
        expected_models,
        has_items(*actual_model_ids),
        'Latest old tables are presented, check marker: {}'.format(mark)
    )


def test_fb_file(workflow, expected_models, jump_table_dump_dir):
    file_path = os.path.join(jump_table_dump_dir, 'jump_table.fb')
    check_fb_file_content(file_path, expected_models, 'before')

    old_modification_tm = get_file_modification_time(file_path)
    # update file with one more execution
    workflow.execute(clt_command_args_list=['dump_jump_table'])
    new_modification_tm = get_file_modification_time(file_path)

    assert_that(
        new_modification_tm,
        greater_than(old_modification_tm),
        'File was successfully modified'
    )
    check_fb_file_content(file_path, expected_models, 'after')

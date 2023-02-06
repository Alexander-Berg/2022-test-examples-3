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
    has_item,
    has_items,
    is_not,
)

from yt.wrapper import ypath_join

from market.idx.marketindexer.marketindexer import msku_hiding_rules_report
from market.idx.marketindexer.yatf.test_env import MarketIndexer
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub
from market.idx.marketindexer.yatf.resources.common_ini import CommonIni
from market.tools.msku_hiding_rules_report.yatf.resources.simple_sku_export_table import (
    SimpleSkuExportTable
)
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.yatf.resources.stop_word_hiding_rules import StopWordHidingRulesNotPacked
from market.idx.yatf.resources.tovar_tree_pb import (
    TovarTreePb,
    MboCategory,
)


@pytest.fixture(scope='module')
def mbo_export_data():
    return [{
        'category_id': 1,
        'model_id': 322,
        'title': 'Forbidden word on the first position!'
    }, {
        'category_id': 1,
        'model_id': 323,
        'title': 'Good title!'
    }, {
        'category_id': 1,
        'model_id': 324,
        'title': 'Even better one!'
    }]


@pytest.fixture(scope='function')
def mbo_table_name():
    table_name_pattern = "%Y%m%d_%H%M"
    name = datetime.now().strftime(table_name_pattern)
    return name


@pytest.fixture(scope='function')
def expected_data(mbo_table_name):
    return [{
        'comment': '',
        'user_id': 0L,
        'title': 'Forbidden word on the first position!',
        'mbo_stuff_version': mbo_table_name,
        'msku_id': 322L,
        'stop_word': 'forbidden',
        'category_id': 1L,
        'user_name': '',
        'creation_time': 0L,
    }]


@pytest.yield_fixture(scope='module')
def absolute_bin_path():
    relative_bin_path = os.path.join(
        'market',
        'tools',
        'msku_hiding_rules_report',
        'msku_hiding_rules_report',
    )
    return yatest.common.binary_path(relative_bin_path)


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
    yt_dir = ypath_join(get_yt_prefix(), 'in/hidden_msku_psku')
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


@pytest.fixture(scope="function")
def create_mbo_export_dir(yt_server, mbo_table_name):
    yt_dir = ypath_join(get_yt_prefix(), 'mbo/export')
    client = yt_server.get_yt_client()
    client.create(
        'map_node',
        os.path.join(yt_dir, mbo_table_name),
        ignore_existing=True,
        recursive=True,
    )
    client.link(
        os.path.join(yt_dir, mbo_table_name),
        os.path.join(yt_dir, 'recent'),
    )


@pytest.yield_fixture(scope='module')
def create_stop_words_file():
    rules = StopWordHidingRulesNotPacked()
    rules.add_rule(
        word='forbidden',
        tags=['title'],
        rgb=[1]  # только синий
    )
    rules.add_rule(
        word='good',
        tags=['title'],
        rgb=[0]  # только белый
    )
    os.makedirs(
        os.path.join(
            yatest.common.work_path(),
            'abo/recent'
        )
    )
    rules.dump(
        os.path.join(
            yatest.common.work_path(),
            'abo/recent',
            'stop-word-hiding-rules.json'
        )
    )


@pytest.yield_fixture(scope="module")
def tovar_tree_categories():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.yield_fixture(scope='module')
def tovar_tree(tovar_tree_categories):
    tovar_tree_path = os.path.join(yatest.common.work_path(), 'tovar-tree.pb')
    res = TovarTreePb(tovar_tree_categories)
    res.dump(tovar_tree_path)
    return res


@pytest.yield_fixture(scope='function')
def workflow(
        yt_server,
        mbo_export_data,
        absolute_bin_path,
        old_table_names,
        create_stop_words_file,
        tovar_tree,
        create_mbo_export_dir
):
    yt_token_path = os.path.join(yatest.common.work_path(), 'token')
    resources = {
        'input_mbo_export_table': SimpleSkuExportTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'mbo/export/recent', 'models/sku'),
            data=mbo_export_data
        ),
        'yt_token': YtTokenStub(yt_token_path),
        'common_ini': CommonIni(
            os.path.join(yatest.common.work_path(), 'common.ini'),
            yatest.common.work_path(),
            misc={
                'msku_hiding_rules_report_enabled': 'true',
            },
            yt={
                'home_dir': get_yt_prefix(),
                'yt_proxy_primary': yt_server.get_server(),
                'yt_tokenpath': yt_token_path,
                'mbo_export_path': ypath_join(get_yt_prefix(), 'mbo/export/recent')
            },
            bin={
                'hiding_rules_report': absolute_bin_path,
            },
            general={
                'categories_tree_path': os.path.join(yatest.common.work_path(), 'tovar-tree.pb')
            },
            getter={
                'data_dir': yatest.common.work_path()
            },
        ),
    }

    with MarketIndexer(yt_server, **resources) as env:
        create_old_tables(yt_server, old_table_names)
        env.execute(clt_command_args_list=['create_msku_hiding_rules_report'])
        yield env


@pytest.fixture(scope='function')
def result_yt_table(workflow):
    path = ypath_join(get_yt_prefix(), 'in/hidden_msku_psku/recent')
    return workflow.get_table_resource_data(path)


def test_hiding_rules_content(result_yt_table, expected_data):
    assert_that(result_yt_table, equal_to(expected_data))


def test_table_rotations(workflow, yt_server, old_table_names):
    client = yt_server.get_yt_client()
    yt_dir = ypath_join(get_yt_prefix(), 'in/hidden_msku_psku')
    table_names_after = client.list(yt_dir)
    keep_count = msku_hiding_rules_report.KEEP_COUNT
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

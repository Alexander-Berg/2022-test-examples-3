#!/usr/bin/env python
# -*- coding: utf-8 -*-
import pytest
import mock
from hamcrest import assert_that, equal_to

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtDynTableResource
from market.idx.marketindexer.marketindexer import workflow

from yt.wrapper import ypath_join


MIN_MAX_DATA_TTL = 3 * 3600 * 1000
MAX_MAX_DATA_TTL = 12 * 3600 * 1000


class MiConfig(object):
    def __init__(
            self,
            yt_proxy,
            yt_tables,
            dynamic_ttl_enabled,
            ignore_not_for_publish
    ):
        self.yt_proxy = yt_proxy
        self.yt_tokenpath = None

        self.ignore_not_for_publish = ignore_not_for_publish

        self.fresh_collection_dynamic_ttl_enabled = dynamic_ttl_enabled
        self.fresh_collection_yt_proxies = [yt_proxy]
        self.fresh_collection_yt_tables = yt_tables
        self.fresh_collection_min_max_data_ttl = MIN_MAX_DATA_TTL
        self.fresh_collection_max_max_data_ttl = MAX_MAX_DATA_TTL


@pytest.fixture(
    scope='function',
    params=[
        {
            'miconfig': {
                'yt_tables': [
                    ypath_join(get_yt_prefix(), 't1'),
                    ypath_join(get_yt_prefix(), 't2'),
                ],
                'dynamic_ttl_enabled': True,
                'ignore_not_for_publish': False,
            },
            'initial_max_data_ttl': MIN_MAX_DATA_TTL,
            'sussess': True,
            'is_not_for_publish': False,
            'expected_max_data_ttl': MIN_MAX_DATA_TTL,  # full generation success, no increasing ttl
        },
        {
            'miconfig': {
                'yt_tables': [
                    ypath_join(get_yt_prefix(), 't1'),
                    ypath_join(get_yt_prefix(), 't2')
                ],
                'dynamic_ttl_enabled': True,
                'ignore_not_for_publish': False,
            },
            'initial_max_data_ttl': MIN_MAX_DATA_TTL,
            'sussess': False,
            'is_not_for_publish': True,
            'expected_max_data_ttl': 2*MIN_MAX_DATA_TTL,  # full generation failed, no publishing, twice ttl
        },
        {
            'miconfig': {
                'yt_tables': [
                    ypath_join(get_yt_prefix(), 't1'),
                    ypath_join(get_yt_prefix(), 't2'),
                ],
                'dynamic_ttl_enabled': True,
                'ignore_not_for_publish': True,
            },
            'initial_max_data_ttl': MIN_MAX_DATA_TTL,
            'sussess': False,
            'is_not_for_publish': True,
            'expected_max_data_ttl': MIN_MAX_DATA_TTL,  # full generation failed, published, no increasing ttl
        },
        {
            'miconfig': {
                'yt_tables': [
                    ypath_join(get_yt_prefix(), 't1'),
                    ypath_join(get_yt_prefix(), 't2'),
                ],
                'dynamic_ttl_enabled': False,
                'ignore_not_for_publish': False,
            },
            'initial_max_data_ttl': MIN_MAX_DATA_TTL,
            'sussess': False,
            'is_not_for_publish': True,
            'expected_max_data_ttl': MIN_MAX_DATA_TTL,  # full generation failed, no publishing, dynamic ttl disabled
        },
        {
            'miconfig': {
                'yt_tables': [
                    ypath_join(get_yt_prefix(), 't1'),
                    ypath_join(get_yt_prefix(), 't2'),
                ],
                'dynamic_ttl_enabled': True,
                'ignore_not_for_publish': False,
            },
            'initial_max_data_ttl': MAX_MAX_DATA_TTL,
            'sussess': True,
            'is_not_for_publish': False,
            'expected_max_data_ttl': MIN_MAX_DATA_TTL,  # full generation success, decrese ttl
        },
        {
            'miconfig': {
                'yt_tables': [
                    ypath_join(get_yt_prefix(), 't1'),
                    ypath_join(get_yt_prefix(), 't2')
                ],
                'dynamic_ttl_enabled': True,
                'ignore_not_for_publish': False,
            },
            'initial_max_data_ttl': MAX_MAX_DATA_TTL,
            'sussess': False,
            'is_not_for_publish': True,
            'expected_max_data_ttl': MAX_MAX_DATA_TTL,  # full generation failed, no publishing, max ttl
        },
    ],
    ids=[
        'success_no_increase',
        'not_for_publish',
        'not_for_publish_but_publish',
        'disabled_dynamic_ttl',
        'success_decrease',
        'not_for_publish_max_ttl',
    ]
)
def parameters(request):
    return request.param


@pytest.fixture(scope='function')
def miconfig(yt, parameters):
    return MiConfig(
        yt.get_server(),
        parameters['miconfig']['yt_tables'],
        parameters['miconfig']['dynamic_ttl_enabled'],
        parameters['miconfig']['ignore_not_for_publish']
    )


@pytest.fixture(scope='function')
def attributes(parameters):
    return {
        'max_data_ttl': parameters['initial_max_data_ttl'],
        'schema': [
            dict(name='id', type='int64'),
        ],
    }


@pytest.fixture(scope='function')
def data():
    return [
        {'id': 1},
        {'id': 2},
    ]


@pytest.fixture(scope='function')
def tables(yt_server, parameters, miconfig, attributes, data):
    result = [
        YtDynTableResource(
            yt_server,
            path,
            attributes=attributes,
            data=data
        )
        for path in miconfig.fresh_collection_yt_tables
    ]
    for r in result:
        r.create()
    return result


def test_dynamic_ttl(yt_server, parameters, miconfig, tables):
    yt_client = yt_server.get_yt_client()

    for table in miconfig.fresh_collection_yt_tables:
        assert_that(
            yt_client.exists(table),
            equal_to(True)
        )

    for table in tables:
        assert_that(
            workflow.get_max_data_ttl(yt_client, table.get_path()),
            equal_to(parameters['initial_max_data_ttl'])
        )

    with mock.patch('market.idx.marketindexer.marketindexer.workflow.zk.am_i_master', return_value=True):
        workflow.change_fresh_collection_ttl(
            miconfig,
            parameters['sussess'],
            parameters['is_not_for_publish'],
            yt_client=yt_client
        )

    for table in tables:
        assert_that(
            workflow.get_max_data_ttl(yt_client, table.get_path()),
            equal_to(parameters['expected_max_data_ttl'])
        )

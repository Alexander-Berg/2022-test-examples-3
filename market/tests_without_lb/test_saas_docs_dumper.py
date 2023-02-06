# coding: utf-8

from hamcrest import assert_that, equal_to
import mock
import pytest

import yt.wrapper as yt

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import SaasDumperEnv, SaasPublisherEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'yt_home': '//home/datacamp/united',
            },
            'saas_dumper': {
                'enable': True,
                'enable_publisher': True,
                'yt_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'output_dir': 'saas_out',
                'ferryman_host': 'ferryman_stub'
            }
        })
    return config


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'T600',
                'feed_id': 1000,
            }
        } for business_id in range(1, 5)
    ] + [
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T100',
            }
        }
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': 'T600',
                'warehouse_id': 0,
                'shop_id': business_id * 10,
                'feed_id': 1000,
            }
        } for business_id in range(1, 5)
    ] + [
        {
            'identifiers': {
                'business_id': 6,
                'offer_id': 'T100',
                'warehouse_id': 0,
                'shop_id': 60,
                'feed_id': 1000,
            },
            'meta': {
                'rgb': DTC.DIRECT_SEARCH_SNIPPET_GALLERY,
            }
        }
    ]


@pytest.fixture(scope='module')
def history(yt_server, config):
    yt_client = yt_server.get_yt_client()
    for i in range(7):
        yt_client.create('table', yt.ypath_join(config.saas_dumper_output_dir, '20190101_010' + str(i)), recursive=True)


@pytest.fixture(scope='module')
def ferryman_uploader_mock():
    mocked_uploader = mock.MagicMock()
    mocked_uploader.deliver_table = mock.Mock(
        return_value='batch_id_12345'
    )
    mocked_uploader.wait_until_completed = mock.Mock()

    with mock.patch('market.idx.datacamp.routines.lib.tasks.saas_dumper.Uploader', return_value=mocked_uploader) as patcher:
        yield patcher


@pytest.yield_fixture(scope='module')
def dumper(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        history,
        ferryman_uploader_mock
):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'config': config,
    }
    with SaasDumperEnv(yt_server, **resources) as routines_env:
        yield routines_env


@pytest.yield_fixture(scope='module')
def publisher(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        history,
        ferryman_uploader_mock,
        dumper
):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'config': config,
    }
    with SaasPublisherEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_dumper(yt_server, config, dumper):
    yt_client = yt_server.get_yt_client()
    tables = yt_client.list(config.saas_dumper_output_dir)
    assert_that(len(tables), equal_to(7))

    results = list(yt_client.read_table(yt.ypath_join(config.saas_dumper_output_dir, 'recent')))
    assert_that(len(results), equal_to(4))


def test_publisher(yt_server, publisher, ferryman_uploader_mock):
    yt_client = yt_server.get_yt_client()
    recent_table = yt_client.get_attribute(yt.ypath_join(publisher.config.saas_dumper_output_dir, 'recent'), 'path')

    assert_that(yt_client.get_attribute(recent_table, 'ferryman_batch_id'), equal_to('batch_id_12345'))

    ferryman_uploader_mock.assert_called_with(ferryman_host='ferryman_stub')
    ferryman_uploader_mock.return_value.deliver_table.assert_called_with(
        table_path=recent_table,
        namespace='0',
        cluster=yt_server.yt_proxy,
        sync=False
    )
    ferryman_uploader_mock.return_value.wait_until_completed.assert_called_with(
        batch_id='batch_id_12345'
    )

    # проверяем, что публикация запускается поллинг статуса индексации при следующем запуске
    ferryman_uploader_mock.return_value.deliver_table.reset_mock()
    ferryman_uploader_mock.return_value.wait_until_completed.reset_mock()
    publisher.task.run(publisher.config, publisher.config.config_path)
    ferryman_uploader_mock.return_value.deliver_table.assert_not_called()
    ferryman_uploader_mock.return_value.wait_until_completed.assert_called_with(
        batch_id='batch_id_12345'
    )

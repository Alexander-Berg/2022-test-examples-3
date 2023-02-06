# coding: utf-8

from hamcrest import assert_that, equal_to
import json
import mock
import os
import pytest

from market.idx.datacamp.parser.lib.input_topic import InputTask
from market.idx.datacamp.parser.yatf.env import make_check_task
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.parser.yatf.qp_mocks import (
    FeedCheckerQparserScriptMock,
)
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    ShopsDatParameters,
    FEED_CLASS_UPDATE,
    FEED_CLASS_STOCK,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
)
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.pylibrary.feed.utils import dump_feed_parsing_task
from market.idx.yatf.common import get_binary_path, get_source_path
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.pylibrary.s3.s3.stub.s3_bucket_emulation import S3BucketEmulation

import yatest.common


BUSINESS_ID = 10
CHECK_RESULT_MAGIC = 'FCHR'
OFFER_COUNT = 5

IGNORED_BUSSINESS = 20001
IGNORED_SHOP = 30001
IGNORED_FEED = 40001


@pytest.fixture(scope='module')
def currency_rates():
    path = os.path.join(
        get_source_path(),
        'market', 'idx', 'feeds', 'qparser', 'tests', 'data', 'currency_rates.xml'
    )
    return FileResource(path)


@pytest.fixture(scope='module')
def check_task_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def check_task_result_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server, check_task_topic, check_task_result_topic):
    cfg = {
        'feedchecker': {
            'mds_bucket': 'test_check_feed',
            'input_topic': check_task_topic.topic,
            'output_topic': check_task_result_topic.topic,
        },
        'qparser': {
            'enabled': True,
            'qparser_bin': get_binary_path(os.path.join('market', 'idx', 'feeds', 'qparser', 'bin', 'qparser', 'qparser')),
            'preprocessor_bin': get_binary_path(os.path.join('market', 'idx', 'feeds', 'qparser', 'bin', 'preprocessor', 'preprocessor')),
            'with_logging': True,
        }
    }

    config = PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg,
    )
    config.s3_bucket_emulation = S3BucketEmulation()
    return config


@pytest.fixture(scope='module')
def mds(tmpdir_factory, config):
    return FakeMds(tmpdir_factory.mktemp('mds'), config)


@pytest.yield_fixture(scope='module')
def qparser_mock_config(config, currency_rates):
    qparser_config = {
        'yt': {
            'enable': False
        },
        'data': {
            'rates': currency_rates.path
        },
        'explanation_log': {
            'enable': True,
            'filename': 'feed_errors.pbuf.sn',
            'log_level': 'warning',
        },
        'logbroker': {
            "enable_log_error_limit": True,
            "log_error_limit": OFFER_COUNT * 2
        },
        "feature": {
            "enable_required_fields_verification": True
        },
        "ignore_lists" : {
            "business" : str(IGNORED_BUSSINESS),
            "shop" : str(IGNORED_SHOP),
            "feed" : str(IGNORED_FEED)
        }
    }
    qparser_config_path = os.path.join(config.qparser_config_dir, 'common.json')
    dump_feed_parsing_task(qparser_config_path, json.dumps(qparser_config))

    with mock.patch("market.idx.feeds.qparser.bin.executor.qparser.build_config_paths", return_value=[qparser_config_path]):
        yield qparser_config_path


@pytest.yield_fixture(scope='module')
def qparser_mock_config_united_catalog(config, currency_rates):
    qparser_config = {
        'yt': {
            'enable': False
        },
        'data': {
            'rates': currency_rates.path
        },
        'explanation_log': {
            'enable': True,
            'filename': 'feed_errors.pbuf.sn',
            'log_level': 'warning',
        },
        'logbroker': {
            "enable_log_error_limit": True,
            "log_error_limit": OFFER_COUNT * 2
        },
        "feature": {
            "enable_required_fields_verification": True,
        },
        "ignore_lists" : {
            "business" : str(IGNORED_BUSSINESS),
            "shop" : str(IGNORED_SHOP),
            "feed" : str(IGNORED_FEED)
        }
    }
    qparser_config_path = os.path.join(config.qparser_config_dir, 'common.json')
    dump_feed_parsing_task(qparser_config_path, json.dumps(qparser_config))

    with mock.patch("market.idx.feeds.qparser.bin.executor.qparser.build_config_paths", return_value=[qparser_config_path]):
        yield qparser_config_path


@pytest.mark.parametrize('color', ['white', 'blue'])
@pytest.mark.parametrize('feed_type', ['assortment_feed', 'price_feed', 'stock_feed'])
@pytest.mark.parametrize('with_vendor_model', ['true', 'false'])
def test_qparser_feed_vendor_model(config, mds, qparser_mock_config_united_catalog, color, feed_type, with_vendor_model):
    """Проверяем чекфид для ценового, стокового и ассортиментного фидов без vendor и model - только ассортиментный фид требует их наличия"""
    shop_id = 11000
    feed_id = 174423
    warehouse_id = 145
    if with_vendor_model == 'true':
        feed_filename = 'MARKETINDEXER-44416-with-vendor-model.xml'
    else:  # with_vendor_model == 'false':
        feed_filename = 'MARKETINDEXER-44416-without-vendor-model.xml'
    mds.setup_push_feed(
        feed_id,
        yatest.common.source_path(os.path.join('market/idx/datacamp/parser/tests/feeds/data', feed_filename))
    )

    # parser return codes: qparser/exit_status.h
    # ценовой фид - не требует vendor и model
    if feed_type == 'price_feed':
        feed_class = FEED_CLASS_UPDATE
        expected_return_code = 0  # FeedParserError = 0 OK
    # стоковый фид - не требует vendor и model
    elif feed_type == 'stock_feed':
        feed_class = FEED_CLASS_STOCK
        expected_return_code = 0  # FeedParserError = 0 OK
    # ассортиментный фид - требует vendor и model
    else:  # feed_type == 'assortment_feed'
        feed_class = FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
        expected_return_code = 3  # FeedParserError = 3 CriticalErrors
    # если vendor и model заданы, все типы фидов должны быть приняты без ошибок
    if with_vendor_model == 'true':
        expected_return_code = 0  # FeedParserError = 0 OK

    if color == 'white':
        dtc_color = DTC.WHITE
        is_dbs = True
    else:  # color == 'blue':
        dtc_color = DTC.BLUE
        is_dbs = False
    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=10,
            color=dtc_color
        ),
        task_type=feed_class,
        is_dbs=is_dbs
    )

    task = InputTask(check_feed_task)

    feed_checker_qparser_script = FeedCheckerQparserScriptMock(
        config=config,
        task=task,
        mds=mds
    )
    rc = feed_checker_qparser_script.run()
    assert_that(rc, equal_to(expected_return_code))

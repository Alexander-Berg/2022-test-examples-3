# coding: utf-8

from hamcrest import assert_that, equal_to, has_items
import json
import mock
import os
import pytest
import shutil
import six

from market.idx.datacamp.parser.lib.input_topic import InputTask
from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_check_task, CheckTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.parser.yatf.qp_mocks import (
    FeedCheckerQparserScriptMock,
    FeedCheckerTrashFeedInfo,
    FeedCheckerDownloadMock,
)

from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    CheckFeedTaskParameters,
    ShopsDatParameters,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
)
from market.idx.datacamp.proto.common.extension_pb2 import ERequiredValueType
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.pylibrary.feed.utils import dump_feed_parsing_task
from market.idx.yatf.common import get_binary_path, get_source_path
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.matchers.env_matchers import HasAssortmentInputOffers
from market.idx.feeds.feedparser.yatf.resources.pb_result import CheckerOutput
from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.pylibrary.s3.s3.stub.s3_bucket_emulation import (
    S3BucketEmulation,
    StubS3Client,
)

import yatest.common


MI_TYPE = 'test.only'
FS_FALLBACK_DIR = 'fs_fallback_dir'
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


def create_fs_fallback_dir():
    dir_path = yatest.common.test_output_path('fs_fallback_dir')
    if os.path.exists(dir_path):
        shutil.rmtree(dir_path)
    os.makedirs(dir_path)
    return dir_path


@pytest.fixture(scope="module")
def host_name():
    return "test_host"


@pytest.fixture(scope="module")
def bucket_name():
    return "test_bucket"


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server, check_task_topic, check_task_result_topic, host_name, bucket_name):
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
        },
        'sessions_logs_uploader': {
            'fs_fallback_dir': create_fs_fallback_dir(),
            'enabled': True,
            's3_host': host_name,
            's3_access_key_path': 'some_path',
            's3_bucket_name': bucket_name,
            'cleaner_workers': 2
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
            "enable_required_fields_verification": True,
            "enable_original_partner_content_validation": True,
            "write_feed_messages_at_the_beginning": True,
            "use_only_datacamp_offer_in_check_result_for_csv": True,
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


@pytest.fixture
def feed_downloader_mock(config):
    yield FeedCheckerDownloadMock(
        config=config
    )


@pytest.fixture
def s3_mock(config):
    class S3Mock(object):
        def __init__(self, config):
            self.config = config

        def get_s3_client(self):
            return StubS3Client(config)

        def init_mds_client(self):
            self.mds_host = self.config.s3_host
            self.mds_access_path = self.config.s3_access_path
            self.mds_bucket = self.config.feedchecker_mds_bucket
            self.mds_client = self.get_s3_client()

    return S3Mock(config)


@pytest.fixture()
def push_parser(monkeypatch, config, qparser_mock_config, feed_downloader_mock, s3_mock):
    with monkeypatch.context() as m:
        m.setattr("market.idx.pylibrary.scripts.check_feed_base_script.CheckFeedBaseScript.download", feed_downloader_mock.download)
        m.setattr("market.idx.datacamp.parser.lib.scripts.qparser_checkfeed_script.FeedCheckerQparserScript.init_mds_client", s3_mock.init_mds_client)
        m.setattr("market.idx.datacamp.parser.lib.parser_engine.session_logs_uploader.SessionLogsUploader._get_s3_client", s3_mock.get_s3_client)

        yield WorkersEnv(
            config=config,
            parsing_service=CheckTaskServiceMock
        )


@pytest.fixture(scope='module')
def trash_feed_info_script():
    yield FeedCheckerTrashFeedInfo()


def run_feedchecker_qparser_script(config, mds, task, ret_code):
    feed_checker_qparser_script = FeedCheckerQparserScriptMock(
        config=config,
        task=task,
        mds=mds
    )
    rc = feed_checker_qparser_script.run()
    assert_that(rc, equal_to(ret_code))

    return feed_checker_qparser_script.uploaded_url


def load_checker_output(data, tmpdir_factory):
    result_file_name = os.path.join(
        str(tmpdir_factory.mktemp('result')), 'result.pbuf.sn'
    )
    with open(result_file_name, 'wb') as result_file:
        result_file.write(six.ensure_binary(data))

    checker = CheckerOutput(result_file_name)
    checker.load()
    return checker.proto_results


@pytest.mark.parametrize('feed_type', ['yml', 'csv'])
def test_do_not_send_legacy_offer(config, push_parser, mds, tmpdir_factory, feed_type):
    """
    Проверяем, что при включенном флаге use_only_datacamp_offer_in_check_result_for_csv
    для csv фида отправляем в результатах только DatacampOffer, легаси Offer не передаем
    """
    shop_id = 111
    feed_id = 222
    warehouse_id = 145
    mbi_validation_id = 333
    offers = [
        {
            # has no one required field (WARNING, WARNING)
            'id': '1',
            'shop-sku': '1',
        },
        {
            # has only one required field and invalid price (ERROR)
            'id': '2',
            'shop-sku': '2',
            'price': 'gg',
            'description': 'almost good offer',
        },
        {
            # has all required fields (no errors)
            'id': '3',
            'shop-sku': '3',
            'description': 'very good offer',
            'barcode': '12345'
        },
        {
            # has only one required field and some invalid tag (ERROR, WARNING)
            'id': '4',
            'shop-sku': '4',
            'barcode': '12345',
            'step-quantity': 'mnoga',
        },
    ]
    mds.generate_feed(feed_id=feed_id, is_blue=True, is_csv=feed_type=='csv', force_offers=offers)

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=7,
            color=DTC.BLUE,
        ),
        validation_id=mbi_validation_id,
        check_feed_task_parameters=CheckFeedTaskParameters(required_fields=[
            ERequiredValueType.RVT_DESCRIPTION,
            ERequiredValueType.RVT_BARCODE
        ]),
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        streaming_check_result=True
    )
    task = InputTask(check_feed_task)
    uploaded_url = run_feedchecker_qparser_script(config, mds, task, 0)

    assert uploaded_url
    data = mds.read(config.feedchecker_mds_bucket, uploaded_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)

    if feed_type == 'yml':  # для yml передаем оффер в легаси формате
        expected_offer_list = [
            {'shop_sku': offer['shop-sku']} for offer in offers
        ]
        assert_that(
            checker_results,
            has_items(*[HasAssortmentInputOffers([offer]) for offer in expected_offer_list]),
            'Check feed offers are OK'
        )
    else:  # для csv передаем только DatacampOffer, легаси формат не передается
        expected_offers = [offer['shop-sku'] for offer in offers]
        result_offers = [result.input_feed.datacamp_offer[0].identifiers.offer_id for result in checker_results if len(result.input_feed.datacamp_offer) > 0]

        assert len(expected_offers) == len(result_offers), "result should contain all offers"
        for result_offer_id, expected_offer_id in zip(result_offers, expected_offers):
            assert result_offer_id == expected_offer_id, "result for csv should be stored in datacamp_offer"

        # офферов в легаси секции быть не должно
        for result in checker_results:
            assert len(result.input_feed.offer) == 0, "csv feed result should not contain legacy offer with use_only_datacamp_offer_in_check_result_for_csv option"

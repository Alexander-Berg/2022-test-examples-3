# coding: utf-8

from hamcrest import assert_that, equal_to, is_not, has_items
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
from market.idx.yatf.matchers.env_matchers import HasAssortmentLogMessage, HasAssortmentParseStats
from market.proto.indexer.BlueAssortment_pb2 import ParseStats

from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    CheckFeedTaskParameters,
    ShopsDatParameters,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
    FF_PROGRAM_NO,
    PROGRAM_STATUS_REAL,
)
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.pylibrary.feed.utils import dump_feed_parsing_task
from market.idx.yatf.common import get_binary_path, get_source_path
from market.idx.yatf.resources.resource import FileResource
from market.idx.feeds.feedparser.yatf.resources.pb_result import CheckerOutput
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.proto.common.extension_pb2 import ERequiredValueType

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


@pytest.fixture(
    scope='module',
    params=[
        {
            'shop_id': 111,
            'feed_id': 999,
            'warehouse_id': 145,
            'is_blue': True,
            'shops_dat': ShopsDatParameters(
                ignore_stocks=False,
                is_discounts_enabled=True,
                enable_auto_discounts=False,
                direct_shipping=True,
                vat=7,
                color=DTC.BLUE,
                ff_program=FF_PROGRAM_NO,
                cpa=PROGRAM_STATUS_REAL,
            ),
            'expected_feed_info': {
                'shop_id': 111,
                'feed_id': 999,
                'market_color': 'blue',
                'warehouse_id': 145,
                'ignore_stocks': False,
                'is_discounts_enabled': True,
                'enable_auto_discounts': False,
                'direct_shipping': True,
                'vat': 7,
                'ff_program': 'NO',
                'cpa': 'REAL'
            }
        },
    ],
    ids=[
        'base_blue_yml_feed_01',
        'base_blue_yml_feed_02',
        'base_direct_yml_feed_01',
        'base_foreign_yml_feed_01',
    ],
)
def feed_info_generating_data(request):
    return request.param


def required_field_error(offer_id, field):
    return {
        'offer_supplier_sku': offer_id,
        'code': '396',
        'level': 2,
        'text': 'Offer has no required field',
        'details': '{"offer":"\\"' + offer_id + '\\"","code":"396","field":"' + field + '"}'
    }


def setup_module():
    os.environ['MI_TYPE'] = MI_TYPE


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


def test_qparser_correctly_counts_ignored_offers(config, push_parser, mds, tmpdir_factory):
    """Проверяем, что в парсере будут корректно посчитаны проигнорированные офферы"""
    shop_id = 111
    feed_id = 999
    warehouse_id = 145
    mds.setup_push_feed(
        feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/MARKETINDEXER-47140.xml')
    )

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=7,
            color=DTC.BLUE,
            cpa=PROGRAM_STATUS_REAL,
        ),
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        streaming_check_result=True,
        dontclean=True,
    )

    task = InputTask(check_feed_task)
    uploaded_url = run_feedchecker_qparser_script(config, mds, task, 0)

    assert uploaded_url
    data = mds.read(config.feedchecker_mds_bucket, uploaded_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)

    stats = checker_results[-1].parse_stats
    assert stats.accepted_offers == stats.total_offers


def test_qparser_required_fields_with_parsing_fields(config, push_parser, mds, tmpdir_factory):
    """Проверяем, что валидация обязательных полей офферов не работает в режиме `Выбора полей`"""
    shop_id = 111
    feed_id = 222
    warehouse_id = 145
    mbi_validation_id = 333
    offers = [
        {
            'id': '2',
            'shop-sku': '2',
            'description': 'almost good offer'
        },
    ]
    mds.generate_feed(feed_id=feed_id, is_blue=True, force_offers=offers, is_csv=True)

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
        streaming_check_result=True,
        parsing_fields=['description']
    )
    task = InputTask(check_feed_task)
    uploaded_url = run_feedchecker_qparser_script(config, mds, task, 0)

    assert uploaded_url
    data = mds.read(config.feedchecker_mds_bucket, uploaded_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)

    # description и barcode - required поля. description мы передали => не получим на него warning автоматически
    # barcode не передали, но warning не получим, т.к передали хотя бы один элемент в parsingFields
    not_expected_log_messages = [
        required_field_error('1', 'barcode'),
    ]
    assert_that(
        checker_results,
        is_not(has_items(*[HasAssortmentLogMessage(msg) for msg in not_expected_log_messages]))
    )
    assert_that(
        checker_results[-1],
        HasAssortmentParseStats(
            ParseStats(
                total_offers=1,
                accepted_offers=1,
                offers_with_supplier_sku=1,
                declined_offers=0,
                price_increase_hit_threshold_count=0,
                price_decrease_hit_threshold_count=0
            )
        ),
        'Parse stats are OK'
    )

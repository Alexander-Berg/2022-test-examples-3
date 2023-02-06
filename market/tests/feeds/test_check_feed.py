# coding: utf-8

from google.protobuf.json_format import MessageToDict
from hamcrest import assert_that, equal_to, has_entries, is_not, has_item, has_items
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
from market.idx.datacamp.proto.api.GeneralizedMessage_pb2 import GeneralizedMessage
from market.idx.pylibrary.datacamp.utils import generate_feed_info_from_parsing_task
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedGeneralizedMessage

from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    CheckFeedTaskParameters,
    ShopsDatParameters,
    FEED_CLASS_STOCK,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
    FF_PROGRAM_NO,
    FF_PROGRAM_REAL,
    PROGRAM_STATUS_NO,
    PROGRAM_STATUS_REAL,
)
from market.idx.datacamp.proto.common.extension_pb2 import ERequiredValueType
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.pylibrary.feed.utils import dump_feed_parsing_task
from market.idx.yatf.common import get_binary_path, get_source_path
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.matchers.env_matchers import HasAssortmentLogMessage, HasAssortmentParseStats, HasAssortmentInputOffers
from market.idx.feeds.feedparser.yatf.resources.pb_result import CheckerOutput
from market.idx.yatf.resources.lbk_topic import LbkTopic

import market.proto.common.process_log_pb2 as PL
from market.proto.indexer.BlueAssortment_pb2 import ParseStats

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
        {
            'shop_id': 222,
            'feed_id': 888,
            'warehouse_id': 147,
            'is_blue': True,
            'shops_dat': ShopsDatParameters(
                ignore_stocks=True,
                is_discounts_enabled=False,
                vat=15,
                color=DTC.BLUE,
                ff_program=FF_PROGRAM_REAL,
                cpa=PROGRAM_STATUS_REAL,
            ),
            'expected_feed_info': {
                'shop_id': 222,
                'feed_id': 888,
                'market_color': 'blue',
                'warehouse_id': 147,
                'ignore_stocks': True,
                'is_discounts_enabled': False,
                'vat': 15,
                'ff_program': 'REAL',
                'cpa': 'REAL',
            }
        },
        {
            'shop_id': 333,
            'feed_id': 999,
            'warehouse_id': 147,
            'is_blue': False,
            'shops_dat': ShopsDatParameters(
                color=DTC.DIRECT,
            ),
            'expected_feed_info': {
                'shop_id': 333,
                'feed_id': 999,
                'market_color': 'direct',
                'warehouse_id': 147,
            }
        },
        {
            'shop_id': 444,
            'feed_id': 777,
            'warehouse_id': 147,
            'is_blue': False,
            'shops_dat': ShopsDatParameters(
                color=DTC.FOREIGN,
            ),
            'expected_feed_info': {
                'shop_id': 444,
                'feed_id': 777,
                'market_color': 'foreign',
                'warehouse_id': 147,
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


def setup_module():
    os.environ['MI_TYPE'] = MI_TYPE


def test_feed_info_generating(config, mds, feed_info_generating_data):
    """Проверяем корректную генерацию feed_info.json по заданию на проверки фида.
    Файл важен для корректной работы qparser."""
    shop_id = feed_info_generating_data['shop_id']
    feed_id = feed_info_generating_data['feed_id']
    warehouse_id = feed_info_generating_data['warehouse_id']
    mds.generate_feed(
        feed_id=feed_id,
        is_blue=feed_info_generating_data['is_blue'],
        offer_count=OFFER_COUNT,
    )

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        shops_dat_parameters=feed_info_generating_data['shops_dat']
    )

    task = InputTask(check_feed_task)
    feed_checker_qparser_script = FeedCheckerQparserScriptMock(
        config=config,
        task=task,
        mds=mds
    )
    result = generate_feed_info_from_parsing_task(
        task=check_feed_task,
        xls2csv_output=feed_checker_qparser_script.xls2csv_output
    )
    assert_that(result, has_entries(feed_info_generating_data['expected_feed_info']))


@pytest.fixture(
    scope='module',
    params=[
        {
            'shop_id': 333,
            'feed_id': 777,
            'warehouse_id': 145,
            'is_blue': True,
            'is_csv_feed': False,
            'shops_dat': ShopsDatParameters(
                ignore_stocks=False,
                is_discounts_enabled=True,
                enable_auto_discounts=False,
                direct_shipping=True,
                vat=7,
                color=DTC.BLUE,
            ),
            'expected_ret_code': 0,
        },
        {
            'shop_id': 444,
            'feed_id': 666,
            'warehouse_id': 145,
            'is_blue': True,
            'is_csv_feed': True,
            'shops_dat': ShopsDatParameters(
                ignore_stocks=False,
                is_discounts_enabled=True,
                enable_auto_discounts=False,
                direct_shipping=True,
                vat=7,
                color=DTC.BLUE,
                cpa=PROGRAM_STATUS_REAL,
            ),
            'expected_ret_code': 0,
        },
        {
            'shop_id': 555,
            'feed_id': 555,
            'warehouse_id': 145,
            'is_blue': True,
            'is_csv_feed': False,
            'is_bad': True,
            'shops_dat': ShopsDatParameters(
                ignore_stocks=False,
                is_discounts_enabled=True,
                enable_auto_discounts=False,
                direct_shipping=True,
                vat=7,
                color=DTC.BLUE,
                cpa=PROGRAM_STATUS_REAL,
            ),
            'expected_ret_code': 3,
        },
        {
            'shop_id': 666,
            'feed_id': 666,
            'warehouse_id': 145,
            'is_blue': True,
            'is_csv_feed': True,
            'is_bad': True,
            'shops_dat': ShopsDatParameters(
                ignore_stocks=False,
                is_discounts_enabled=True,
                enable_auto_discounts=False,
                direct_shipping=True,
                vat=7,
                color=DTC.BLUE,
                cpa=PROGRAM_STATUS_REAL,
            ),
            'expected_ret_code': 3,
        },
    ],
    ids=[
        'base_blue_yml_feed',
        'base_blue_csv_feed',
        'blue_yml_feed_with_wrong_filled_fields',
        'blue_csv_feed_with_wrong_filled_fields',
    ],
)
def check_feed_data(request):
    return request.param


def required_field_error(offer_id, field):
    return {
        'offer_supplier_sku': offer_id,
        'code': '396',
        'level': 2,
        'text': 'Offer has no required field',
        'details': '{"offer":"\\"' + offer_id + '\\"","code":"396","field":"' + field + '"}'
    }


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


def test_base_check_feed_functionality(config, mds, check_feed_data, qparser_mock_config, tmpdir_factory):
    """Проверяем, что qparser запускается, возвращает ожидаемый код ответа на хороших входных данных
       и генерирует правильный протобаф и загружает его в mds
    """
    shop_id = check_feed_data['shop_id']
    feed_id = check_feed_data['feed_id']
    warehouse_id = check_feed_data['warehouse_id']
    is_bad = 'is_bad' in check_feed_data and check_feed_data['is_bad']
    mds.generate_feed(
        feed_id=feed_id,
        is_blue=check_feed_data['is_blue'],
        offer_count=OFFER_COUNT,
        is_csv=check_feed_data['is_csv_feed'],
        bad=is_bad,
    )

    check_parameters = check_feed_data['check_feed_task_parameters'] \
        if 'check_feed_task_parameters' in check_feed_data else None

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        shops_dat_parameters=check_feed_data['shops_dat'],
        check_feed_task_parameters=check_parameters,
        dontclean=True,
        streaming_check_result=True
    )

    task = InputTask(check_feed_task)
    uploaded_url = run_feedchecker_qparser_script(config, mds, task, check_feed_data['expected_ret_code'])

    # проверяем, что получили урл для скачивания proto-файла из mds
    assert uploaded_url
    data = mds.read(config.feedchecker_mds_bucket, uploaded_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)

    # проверяем, что файл содержит нужную информацию
    if is_bad:
        expected_log_messages = [
            {
                'code': '452',
                'level': 3,
                'feed_id': feed_id,
                'offer_supplier_sku': '{}xXx{}'.format(feed_id, i)
            } for i in range(OFFER_COUNT)
        ]
        assert_that(
            checker_results,
            has_items(*[HasAssortmentLogMessage(msg) for msg in expected_log_messages]),
            'Log messages are OK'
        )

    assert_that(
        checker_results[-1],
        HasAssortmentParseStats(
            ParseStats(
                total_offers=OFFER_COUNT,
                accepted_offers=0 if is_bad else OFFER_COUNT,
                offers_with_supplier_sku=OFFER_COUNT,
                declined_offers=OFFER_COUNT if is_bad else 0,
                price_increase_hit_threshold_count=0,
                price_decrease_hit_threshold_count=0
            )
        ),
        'Parse stats are OK'
    )

    expected_offer_list = [
        {
            'shop_sku': '{}xXx{}'.format(feed_id, i),
            'disabled': False,
        } for i in range(OFFER_COUNT)
    ]

    for i, expected_offer in enumerate(expected_offer_list):
        if is_bad:
            expected_offer.update({
                'title': 'offer {}xXx{}'.format(feed_id, i),
                'price': 'qwe',
                'vat': '7',
                'shop_sku': '{}xXx{}'.format(feed_id, i),
            })
        else:
            expected_offer.update({
                'title': 'offer {}xXx{}'.format(feed_id, i),
                'price': '1034.00',
                'currency': 'RUR',
                'url': 'http://www.1.ru/?ID={}xXx{}'.format(feed_id, i),
                'vat': '7',
            })

    assert_that(
        checker_results,
        has_items(*[HasAssortmentInputOffers([offer]) for offer in expected_offer_list]),
        'Check feed offers are OK'
    )


def test_base_check_feed_functionality_for_white_feed(config, mds, qparser_mock_config):
    """Проверяем, что qparser запускается, возвращает ожидаемый код ответа при плохих входных данных.
       Например, белый фид"""
    shop_id = 1234
    feed_id = 5678
    mds.generate_feed(
        feed_id=feed_id,
        is_blue=False,
        offer_count=OFFER_COUNT,
        cpa=False,
    )

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=10,
            color=DTC.WHITE,
            cpa=PROGRAM_STATUS_NO,
        ),
    )

    task = InputTask(check_feed_task)
    uploaded_url = run_feedchecker_qparser_script(config, mds, task, ret_code=0)

    # проверяем, что получили урл для скачивания proto-файла из mds
    assert uploaded_url
    data = mds.read(config.feedchecker_mds_bucket, uploaded_url)
    assert data


def test_base_errors_in_check_feed_functionality_wrong_feed_path(monkeypatch, config, mds, qparser_mock_config, trash_feed_info_script):
    """Проверяем, что qparser запускается, возвращает ожидаемый код ответа при плохих входных данных.
       Например, плохой feed_info.json"""
    shop_id = 5678
    feed_id = 1234
    warehouse_id = 145
    mds.generate_feed(
        feed_id=feed_id,
        is_blue=True,
        offer_count=OFFER_COUNT,
    )

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=10,
            color=DTC.BLUE,
        ),
    )

    task = InputTask(check_feed_task)
    with monkeypatch.context() as m:
        m.setattr("market.idx.feeds.qparser.bin.executor.qparser.QParser._preproc_feed_info", trash_feed_info_script.trash_feed_info)
        run_feedchecker_qparser_script(config, mds, task, ret_code=112)


def test_upload_check_results_if_retcode_is_114(config, mds, qparser_mock_config, trash_feed_info_script):
    """qparser-у передаем невалидный фид (не парсится yml).
       Проверяем, что qparser return code = 114, и загружается файл результата"""
    shop_id = 5678
    feed_id = 1234
    warehouse_id = 145
    mds.generate_feed(
        feed_id=feed_id,
        is_corrupted=True,
        offer_count=OFFER_COUNT,
    )

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=10,
            color=DTC.WHITE,
        ),
    )

    task = InputTask(check_feed_task)
    upload_url = run_feedchecker_qparser_script(config, mds, task, ret_code=114)
    assert upload_url


def test_basic_pipeline_feedchecker(push_parser, check_task_topic, check_task_result_topic, mds, feed_downloader_mock):
    """Проверяем, что задание на парсинг фида проезжает через парсер и пишет ответ в выходной топик"""
    shop_id = 1000
    feed_id = 2000
    warehouse_id = 145
    mbi_validation_id = 1500
    mds.generate_feed(feed_id=feed_id, is_blue=True, offer_count=OFFER_COUNT)

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
    )
    task = InputTask(check_feed_task)
    feed_downloader_mock.set_task(task)

    generalized_message = GeneralizedMessage()
    generalized_message.feed_parsing_task.CopyFrom(check_feed_task)
    check_task_topic.write(generalized_message.SerializeToString())

    push_parser.run(total_sessions=1)

    data = check_task_result_topic.read(count=1)

    assert_that(data, HasSerializedGeneralizedMessage([{
        'feed_parsing_task_report': {
            'parser_return_code': 0,
            'feed_parsing_task': {
                'feed_id': feed_id,
                'shop_id': shop_id,
                'check_feed_task_identifiers': {
                    'validation_id': mbi_validation_id
                },
            },
        },
    }]))


def test_basic_pipeline_feedchecker_530(push_parser, check_task_topic, check_task_result_topic, mds, feed_downloader_mock):
    """Проверяем, что задание на парсинг фида проезжает через парсер и пишет ответ в выходной топик"""
    shop_id = 1000
    feed_id = 2000
    warehouse_id = 145
    mbi_validation_id = 1600
    categories = mds.generate_categories(5)
    categories[2]['parentId'] = '4'
    mds.generate_feed(feed_id=feed_id, is_blue=True, offer_count=OFFER_COUNT, categories=categories)

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
    )
    task = InputTask(check_feed_task)
    feed_downloader_mock.set_task(task)

    generalized_message = GeneralizedMessage()
    generalized_message.feed_parsing_task.CopyFrom(check_feed_task)
    check_task_topic.write(generalized_message.SerializeToString())

    push_parser.run(total_sessions=1)

    data = check_task_result_topic.read(count=1)

    generalized_message.ParseFromString(data[0])

    assert_that(data, HasSerializedGeneralizedMessage([{
        'feed_parsing_task_report': {
            'parser_return_code': 114,  # FeedParserError = 114  qparser/exit_status.h
            'feed_parsing_task': {
                'feed_id': feed_id,
                'shop_id': shop_id,
                'check_feed_task_identifiers': {
                    'validation_id': mbi_validation_id
                },
            },
            'feed_parsing_error_messages': [
                {'code': '530'},
            ],
            'url_to_parser_output': ''
        },
    }]))


def test_qparser_stocks_feed(push_parser, check_task_topic, check_task_result_topic, mds, feed_downloader_mock):
    """Проверяем чекфид для стокового-фида"""
    shop_id = 10000
    feed_id = 20000
    warehouse_id = 145
    mbi_validation_id = 15000
    mds.setup_push_feed(
        feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/stock_feed.csv')
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
        ),
        validation_id=mbi_validation_id,
        task_type=FEED_CLASS_STOCK,
        dontclean=True,
    )
    task = InputTask(check_feed_task)
    feed_downloader_mock.set_task(task)

    generalized_message = GeneralizedMessage()
    generalized_message.feed_parsing_task.CopyFrom(check_feed_task)
    check_task_topic.write(generalized_message.SerializeToString())

    push_parser.run(total_sessions=1)

    data = check_task_result_topic.read(count=1)

    assert_that(data, HasSerializedGeneralizedMessage([{
        'feed_parsing_task_report': {
            'parser_return_code': 0,
            'feed_parsing_task': {
                'feed_id': feed_id,
                'shop_id': shop_id,
                'check_feed_task_identifiers': {
                    'validation_id': mbi_validation_id
                },
            },
        },
    }]))


def test_qparser_feed_with_many_disabled_offers(push_parser, check_task_topic, check_task_result_topic, mds, feed_downloader_mock):
    """Проверяем чекфид для нормального фида с кучей отключенных офферов"""
    shop_id = 11000
    feed_id = 22000
    warehouse_id = 145
    mbi_validation_id = 15200
    mds.setup_push_feed(
        feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/MBI-56468.613244.csv')
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
        ),
        validation_id=mbi_validation_id,
        dontclean=True,
    )
    task = InputTask(check_feed_task)
    feed_downloader_mock.set_task(task)

    generalized_message = GeneralizedMessage()
    generalized_message.feed_parsing_task.CopyFrom(check_feed_task)
    check_task_topic.write(generalized_message.SerializeToString())

    push_parser.run(total_sessions=1)

    data = check_task_result_topic.read(count=1)
    generalized_message.ParseFromString(data[0])
    # raise RuntimeError(
    MessageToDict(generalized_message, preserving_proto_field_name=True)
    # )

    assert_that(data, HasSerializedGeneralizedMessage([{
        'feed_parsing_task_report': {
            'parser_return_code': 0,
            'feed_parsing_task': {
                'feed_id': feed_id,
                'shop_id': shop_id,
                'check_feed_task_identifiers': {
                    'validation_id': mbi_validation_id
                },
            },
        },
    }]))


def test_qparser_feed_with_many_bad_offers(push_parser, check_task_topic, check_task_result_topic, mds, feed_downloader_mock):
    """Проверяем чекфид для кривого фида"""
    shop_id = 11000
    feed_id = 22000
    warehouse_id = 145
    mbi_validation_id = 15200
    mds.setup_push_feed(
        feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/MBI-56468.613244.bad.csv')
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
        ),
        validation_id=mbi_validation_id,
        dontclean=True,
    )
    task = InputTask(check_feed_task)
    feed_downloader_mock.set_task(task)

    generalized_message = GeneralizedMessage()
    generalized_message.feed_parsing_task.CopyFrom(check_feed_task)
    check_task_topic.write(generalized_message.SerializeToString())

    push_parser.run(total_sessions=1)

    data = check_task_result_topic.read(count=1)
    generalized_message.ParseFromString(data[0])

    # raise RuntimeError(
    MessageToDict(generalized_message, preserving_proto_field_name=True)
    # )

    assert_that(data, HasSerializedGeneralizedMessage([{
        'feed_parsing_task_report': {
            'parser_return_code': 3,
            'feed_parsing_task': {
                'feed_id': feed_id,
                'shop_id': shop_id,
                'check_feed_task_identifiers': {
                    'validation_id': mbi_validation_id
                },
            },
            'feed_parsing_error_messages': [
                {'code': '550'},
            ]
        },
    }]))


def test_qparser_dsbs_stocks_feed(push_parser, check_task_topic, check_task_result_topic, mds, feed_downloader_mock):
    """Проверяем чекфид для стокового-фида"""
    shop_id = 10785722
    feed_id = 20000
    warehouse_id = 234
    mbi_validation_id = 15000
    mds.setup_push_feed(
        feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/stock_dsbs_feed.csv')
    )
    check_feed_task = make_check_task(
        mds,
        feed_id,
        10614662,
        shop_id,
        warehouse_id=warehouse_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=7,
            color=DTC.WHITE,
            is_mock=False,
            is_upload=True,
            local_region_tz_offset=10800,
        ),
        validation_id=mbi_validation_id,
        task_type=FEED_CLASS_STOCK,
        dontclean=True,
        is_dbs=True,
    )
    task = InputTask(check_feed_task)
    feed_downloader_mock.set_task(task)
    generalized_message = GeneralizedMessage()
    generalized_message.feed_parsing_task.CopyFrom(check_feed_task)
    check_task_topic.write(generalized_message.SerializeToString())
    push_parser.run(total_sessions=1)
    data = check_task_result_topic.read(count=1)
    assert_that(data, HasSerializedGeneralizedMessage([{
        'feed_parsing_task_report': {
            'parser_return_code': 0,
            'feed_parsing_task': {
                'feed_id': feed_id,
                'shop_id': shop_id,
                'check_feed_task_identifiers': {
                    'validation_id': mbi_validation_id
                },
            },
        },
    }]))


@pytest.mark.parametrize('color', ['blue', 'white'])
def test_checkfeed_log_message_limit(push_parser, check_task_topic, check_task_result_topic, mds, feed_downloader_mock, config, tmpdir_factory, color):
    """Проверяем, что в логе чекфида не больше заданного лимита ошибок
    На вход подаем фид с количеством невалидных офферов больше лимита ошибок
    Проверяем, что лимит сработал и количество ошибок в результате не больше лимита
    Если был достигнут лимит по количеству ошибок, то офферы в формате datacampOffer не передаем вообще
    Статистики и оффер в старом формате остаются без изменений
    """
    shop_id = 123
    feed_id = 1234
    is_bad=True
    is_csv=True
    FEED_OFFERS_COUNT = OFFER_COUNT * 3
    MESSAGE_LIMIT = OFFER_COUNT * 2  # лимит ошибок задан выше в конфиге парсера, log_message_limit
    mds.generate_feed(
        feed_id=feed_id,
        is_blue=(color=='blue'),
        offer_count=FEED_OFFERS_COUNT,
        is_csv=is_csv,
        bad=is_bad,
    )

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=10,
            color=DTC.BLUE if color=='blue' else DTC.WHITE,
        ),
        dontclean=True
    )

    task = InputTask(check_feed_task)
    uploaded_url = run_feedchecker_qparser_script(config, mds, task, 3)

    # проверяем, что получили урл для скачивания proto-файла из mds
    assert uploaded_url
    data = mds.read(config.feedchecker_mds_bucket, uploaded_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)[0]

    # проверяем, что в файле есть положенные по лимиту MESSAGE_LIMIT ошибки, не больше
    checker_results_log_messages = [MessageToDict(msg, preserving_proto_field_name=True) for msg in checker_results.log_message]
    # считаем офферные ошибки, для фида сгенеренного generate_feed они все начинаются на 4 (+есть фидовые на 5, их игнорим)
    checker_offer_errors = [msg for msg in checker_results_log_messages if msg['code'].startswith('4')]
    assert (len(checker_offer_errors) == MESSAGE_LIMIT)

    # старая запись офферов - без ограничения
    checker_results_input_offers = [MessageToDict(msg, preserving_proto_field_name=True) for msg in checker_results.input_feed.offer]
    assert (len(checker_results_input_offers) == FEED_OFFERS_COUNT)

    # новая запись офферов - ни одного
    checker_results_input_datacamp_offers = [MessageToDict(msg, preserving_proto_field_name=True) for msg in checker_results.input_feed.datacamp_offer]
    assert (len(checker_results_input_datacamp_offers) == 0)

    # статистика не пострадала, там корректное количество офферов FEED_OFFERS_COUNT
    assert_that(
        checker_results,
        HasAssortmentParseStats(
            ParseStats(
                total_offers=FEED_OFFERS_COUNT,
                accepted_offers=0,
                offers_with_supplier_sku=FEED_OFFERS_COUNT,
                declined_offers=FEED_OFFERS_COUNT,
                price_increase_hit_threshold_count=0,
                price_decrease_hit_threshold_count=0
            )
        ),
        'Parse stats are OK'
    )

    # есть метка о том, что мы достигли лимита ошибок, и в файле лежат не все ошибки и офферы
    # метка используется для отображения партнеру в ПИ
    error_limit_reached_mark = checker_results.error_limit_reached
    assert error_limit_reached_mark


def test_qparser_required_fields(config, push_parser, mds, tmpdir_factory):
    """Проверяем, что корректно работает валидация обязательных полей офферов"""
    shop_id = 111
    feed_id = 222
    warehouse_id = 145
    mbi_validation_id = 333
    offers = [
        {
            # has no one required field
            'id': '1',
            'shop-sku': '1',
        },
        {
            # has only one required field and zero price (invalid offer)
            'id': '2',
            'shop-sku': '2',
            'price': 0,
            'description': 'almost good offer'
        },
        {
            # has all required fields
            'id': '3',
            'shop-sku': '3',
            'description': 'very good offer',
            'barcode': '12345'
        }
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
        streaming_check_result=True
    )
    task = InputTask(check_feed_task)
    uploaded_url = run_feedchecker_qparser_script(config, mds, task, 0)

    assert uploaded_url
    data = mds.read(config.feedchecker_mds_bucket, uploaded_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)

    expected_log_messages = [
        required_field_error('1', 'description'),
        required_field_error('1', 'barcode'),
    ]
    assert_that(
        checker_results,
        has_items(*[HasAssortmentLogMessage(msg) for msg in expected_log_messages])
    )

    not_expected_log_messages = [
        required_field_error('2', 'description'),
        required_field_error('2', 'barcode'),
        required_field_error('3', 'description'),
        required_field_error('3', 'barcode'),
    ]
    for msg in not_expected_log_messages:
        assert_that(
            checker_results,
            is_not(has_item(HasAssortmentLogMessage(msg))),
            'Log messages are OK'
        )

    # last element contains parsing stats
    assert_that(
        checker_results[-1],
        HasAssortmentParseStats(
            ParseStats(
                total_offers=3,
                accepted_offers=2,
                offers_with_supplier_sku=3,
                declined_offers=1,
                price_increase_hit_threshold_count=0,
                price_decrease_hit_threshold_count=0
            )
        ),
        'Parse stats are OK'
    )


@pytest.mark.parametrize('feed_type', ['yml', 'csv'])
def test_sorted_errors_warnings(config, push_parser, mds, tmpdir_factory, feed_type):
    """Проверяем, что ошибки и предупреждения идут в верном порядке"""
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
    ] + [
        # добавляю побольше офферов, чтобы проверять корректность сортировки для csv по Position
        {
            # has all required fields (no errors)
            'id': str(i),
            'shop-sku': str(i),
            'description': 'need more offers for test {}'.format(i),
            'barcode': '12345'
        } for i in range(5, 13)
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

    def check_errors_order_in_offer(log_messages):
        for msg in log_messages:
            assert msg.namespace == PL.OFFER, "log_messages should not contains feed errors"
        if len(log_messages) > 1:
            for i in range(len(log_messages) - 1):
                assert log_messages[i].level >= log_messages[i + 1].level, "log_messages should be sorted from errors to warnings"

    if feed_type == 'yml':  # для yml должны сортировать
        offers_worse_error = []
        for result in checker_results:
            offer_id = None
            log_message = None
            if len(result.input_feed.offer) > 0:
                offer_id = result.input_feed.offer[0].shop_sku
            if len(result.log_message) > 0:
                # Проверяем, что ошибки в оффере отсортированы в порядке уменьшения уровня ошибки
                check_errors_order_in_offer(result.log_message)
                log_message = result.log_message[0]
            if offer_id:
                offers_worse_error.append(log_message)

        # Проверяем, что оффера идут в порядке уменьшения уровня ошибки
        assert len(offers_worse_error) == len(offers)
        for i in range(len(offers_worse_error) - 1):
            curr_message = offers_worse_error[i]
            next_message = offers_worse_error[i + 1]
            curr_level = curr_message.level if curr_message else 0
            next_level = next_message.level if next_message else 0
            assert curr_level >= next_level, "offers should be sorted from errors to warnings"
    else:  # для csv не должны менять порядок офферов
        result_offers_order = []
        for result in checker_results:
            if len(result.input_feed.offer) > 0:
                result_offers_order.append(result.input_feed.offer[0].shop_sku)

        original_offers_order = [offer["shop-sku"] for offer in offers]
        assert len(result_offers_order) == len(original_offers_order), "check result should contains all offers"

        for original_offer_id, result_offer_id in zip(original_offers_order, result_offers_order):
            assert original_offer_id == result_offer_id, "for csv feed offers should save original order"


def test_feed_messages_at_the_beginning(config, push_parser, mds, tmpdir_factory):
    """Проверяем, что ошибки фида идут перед поофферными"""
    shop_id = 111
    feed_id = 222
    warehouse_id = 145
    mbi_validation_id = 333
    offers = [
        {
            # has only one required field and some invalid tag (ERROR, WARNING)
            # + unknown tag (FEED_ERROR)
            'id': '4',
            'shop-sku': '4',
            'barcode': '12345',
            'step-quantity': 'mnoga',
            'some-unknown-tag': 'unknown',
        },
    ]
    mds.generate_feed(feed_id=feed_id, is_blue=True, is_csv=False, force_offers=offers)

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
    uploaded_url = run_feedchecker_qparser_script(config, mds, task, 2)

    assert uploaded_url
    data = mds.read(config.feedchecker_mds_bucket, uploaded_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)

    feed_result = checker_results[0]
    for msg in feed_result.log_message:
        assert msg.namespace != PL.OFFER, "feed errors should be at the beginning"

    for result in checker_results[1:-1]:
        for msg in result.log_message:
            assert msg.namespace == PL.OFFER, "second result should contains only offer errors"

    last_message = checker_results[-1]
    assert_that(
        last_message,
        HasAssortmentParseStats(
            ParseStats(
                accepted_offers=1,
                declined_offers=0,
                total_offers=1,
                offers_with_supplier_sku=1,
                price_increase_hit_threshold_count=0,
                price_decrease_hit_threshold_count=0,
            )
        ),
        'Parse stats are OK'
    )


FEED_PARSING_TASK_IGNORED = 118


@pytest.mark.parametrize('business_id, shop_id, feed_id', [(IGNORED_BUSSINESS, 123, 1234), (BUSINESS_ID, IGNORED_SHOP, 1234), (BUSINESS_ID, 123, IGNORED_FEED)])
def test_feed_ignored(mds, config, business_id, shop_id, feed_id):
    is_bad=True
    is_csv=True
    FEED_OFFERS_COUNT = OFFER_COUNT * 3
    mds.generate_feed(
        feed_id=feed_id,
        is_blue=False,
        offer_count=FEED_OFFERS_COUNT,
        is_csv=is_csv,
        bad=is_bad,
    )

    check_feed_task = make_check_task(
        mds,
        feed_id,
        business_id,
        shop_id,
        shops_dat_parameters=ShopsDatParameters(
            vat=10,
            color=DTC.WHITE,
        ),
        dontclean=True
    )

    task = InputTask(check_feed_task)
    run_feedchecker_qparser_script(config, mds, task, FEED_PARSING_TASK_IGNORED)


def test_qparser_deletes_partner_content_with_nulls(config, push_parser, mds, tmpdir_factory):
    """Проверяем, что в парсере будут удалены невалидные строки с \0 и будет показана ошибка в случае чек-фида"""
    shop_id = 111
    feed_id = 222
    warehouse_id = 145
    mds.setup_push_feed(
        feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/MARKETINDEXER-45131-partner-content-with-nulls.csv')
    )

    check_feed_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
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

    def string_with_nulls(offer_id):
        return {
            'offer_supplier_sku': offer_id,
            'code': '35p',
            'level': 2,
            'text': 'Partner content contains nulls',
        }

    expected_log_messages = [
        string_with_nulls('2'),
    ]
    assert_that(
        checker_results,
        has_items(*[HasAssortmentLogMessage(msg) for msg in expected_log_messages])
    )

    not_expected_log_messages = [
        string_with_nulls('3'),
    ]
    for msg in not_expected_log_messages:
        assert_that(
            checker_results,
            is_not(has_item(HasAssortmentLogMessage(msg))),
            'Log messages are OK'
        )

    # last element contains parsing stats
    assert_that(
        checker_results[-1],
        HasAssortmentParseStats(
            ParseStats(
                accepted_offers=2,
                declined_offers=0,
                total_offers=2,
                offers_with_supplier_sku=2,
                price_increase_hit_threshold_count=0,
                price_decrease_hit_threshold_count=0,
            )
        ),
        'Parse stats are OK'
    )


def test_qparser_on_demand(config, push_parser, mds, tmpdir_factory, check_task_result_topic):
    """Проверяем, что в парсере будут удалены невалидные строки с \0 и будет показана ошибка в случае чек-фида"""
    shop_id = 111
    feed_id = 222
    warehouse_id = 145
    mds.generate_feed(
        feed_id,
        is_blue=False,
        is_csv=False,
        force_offers=[
            {
                'id': 'with-on-demand',
                'shop-sku': 'with-on-demand',
                'type': 'on.demand'
            },
        ],
    )

    check_feed_not_dbs_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        streaming_check_result=True,
        dontclean=True,
    )
    not_dbs_task = InputTask(check_feed_not_dbs_task)
    uploaded_no_dbs_url = run_feedchecker_qparser_script(config, mds, not_dbs_task, 0)
    assert uploaded_no_dbs_url

    check_feed_dbs_task = make_check_task(
        mds,
        feed_id,
        BUSINESS_ID,
        shop_id,
        warehouse_id=warehouse_id,
        task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
        streaming_check_result=True,
        dontclean=True,
        is_dbs=True,
    )
    dbs_task = InputTask(check_feed_dbs_task)
    uploaded_dbs_url = run_feedchecker_qparser_script(config, mds, dbs_task, 0)
    assert uploaded_dbs_url

    data = mds.read(config.feedchecker_mds_bucket, uploaded_no_dbs_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)

    def string_with_nulls(offer_id):
        return {
            'offer_supplier_sku': offer_id,
            'code': '49t',
            'level': 3,
            'text': 'Model is not allowed for on demand',
        }

    expected_log_messages = [
        string_with_nulls('with-on-demand'),
    ]
    assert_that(
        checker_results,
        has_items(*[HasAssortmentLogMessage(msg) for msg in expected_log_messages])
    )

    # last element contains parsing stats
    assert_that(
        checker_results[-1],
        HasAssortmentParseStats(
            ParseStats(
                accepted_offers=1,
                declined_offers=0,
                total_offers=1,
                offers_with_supplier_sku=1,
                price_increase_hit_threshold_count=0,
                price_decrease_hit_threshold_count=0,
            )
        ),
        'Parse stats are OK'
    )

    data = mds.read(config.feedchecker_mds_bucket, uploaded_dbs_url)
    assert data
    checker_results = load_checker_output(data, tmpdir_factory)

    assert_that(
        checker_results,
        is_not(has_items(*[HasAssortmentLogMessage(msg) for msg in expected_log_messages]))
    )

    # last element contains parsing stats
    assert_that(
        checker_results[-1],
        HasAssortmentParseStats(
            ParseStats(
                accepted_offers=1,
                declined_offers=0,
                total_offers=1,
                offers_with_supplier_sku=1,
                price_increase_hit_threshold_count=0,
                price_decrease_hit_threshold_count=0,
            )
        ),
        'Parse stats are OK'
    )

# WARNING: добавляйте новые тесты в test_check_feed2.py. Здесь много тестов и новые тесты вызывают таймауты, которые мьютятся

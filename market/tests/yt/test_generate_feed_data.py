# coding: utf-8

import pytest

from hamcrest import assert_that, has_key, has_entries, is_not, equal_to, none
from google.protobuf.timestamp_pb2 import Timestamp

from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import ProcessJobMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.parser.yatf.env import WorkersEnv, UpdateTaskServiceMock, make_input_task
from market.idx.datacamp.proto.api.UpdateTask_pb2 import ShopsDatParameters, FF_PROGRAM_REAL, PROGRAM_STATUS_NO
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerAdditionalInfo
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import BLUE
from market.proto.offersrobot.FeedFulfillment_pb2 import FeedFulfillment
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.datacamp.yatf.utils import dict2tskv


BUSINESS_ID = 10

PARTNER_DATA = [
    {
        'shop_id': 123,
        'status': 'publish',
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': 123,
                'datafeed_id': 100,
                'warehouse_id': 145,
                'regions': '213',
                'is_push_partner': 'true',
                'ff_program': 'REAL',
                'ff_virtual_id': 1234,
            }),
            dict2tskv({
                'shop_id': 123,
                'datafeed_id': 200,
                'warehouse_id': 146,
                'regions': '213',
                'is_push_partner': 'true',
                'ff_program': 'REAL',
                'ff_virtual_id': 1234,
            })]
        )
    },
    {
        'shop_id': 1234,
        'status': 'publish',
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': 1234,
                'datafeed_id': 300,
                'ff_program': 'NO',
                'regions': '213',
                'market_delivery_courier': 'true',
            })
        ])
    },
    {
        'shop_id': 1235,
        'status': 'publish',
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': 1235,
                'datafeed_id': 400,
                'is_push_partner': 'true',
            })
        ]),
        'partner_additional_info': PartnerAdditionalInfo(
            disabled_since_ts=Timestamp(seconds=100)
        ).SerializeToString()
    }
]


@pytest.fixture()
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def technical_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, input_topic, technical_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker_technical': {
            'topic': technical_topic.topic,
        },
    }

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture()
def mds(tmpdir_factory, config):
    return FakeMds(tmpdir_factory.mktemp('mds'), config)


@pytest.fixture()
def partner_table(config, yt_server):
    table = DataCampPartnersTable(yt_server, config.yt_partners_tablepath, PARTNER_DATA)
    table.create()
    return table


@pytest.fixture()
def process_job_mock():
    return ProcessJobMock()


@pytest.fixture()
def push_parser(monkeypatch, config, partner_table, process_job_mock):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.parser_engine.process_job", process_job_mock.process_job)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_mbi_params(push_parser, input_topic, mds, process_job_mock):
    """
    Тест проверяет, что параметры партнера были взяты из таблицы партнера и корректно передались на парсинг,
    а также произошло корректное проставление fulfillment данных
    """
    input_topic.write(make_input_task(mds, 200, BUSINESS_ID, 123, warehouse_id=146).SerializeToString())
    push_parser.run(total_sessions=1)

    assert_that(process_job_mock.feed_data.mbi_params, has_entries({
        'shop_id': '123',
        'datafeed_id': 200,
        'warehouse_id': 146,
    }))

    fulfillment_expected_data = FeedFulfillment.Fulfillment(
        feed_id='300',
        shop_id='1234',
        market_delivery_courier=True,
    )

    assert_that(process_job_mock.feed_data.fulfillment, fulfillment_expected_data)


@pytest.mark.parametrize('flag, skip_feedurl', [(True, True), (False, False)])
def test_mbi_params_from_protobuf(push_parser, input_topic, mds, process_job_mock, flag, skip_feedurl):
    """
    Если параметры партнера не были найдены в таблице partners, то они берутся из shops_dat_parameters
    """
    feed_id = 205
    url = 'http://some_url.ru'
    input_topic.write(make_input_task(
        mds,
        feed_id=feed_id,
        business_id=BUSINESS_ID,
        shop_id=125,
        shops_dat_parameters=ShopsDatParameters(
            ignore_stocks=flag,
            is_discounts_enabled=flag,
            enable_auto_discounts=flag,
            direct_shipping=flag,
            vat=7,
            color=BLUE,
            ff_program=FF_PROGRAM_REAL,
            url=url,
            blue_status='REAL',
            supplier_type='3',
            is_mock=flag,
            is_upload=flag,
            prefix='some_prefix',
            cpa=PROGRAM_STATUS_NO,
        ),
        skip_feedurl=skip_feedurl,
    ).SerializeToString())
    push_parser.run(total_sessions=1)

    # Данные из описания задания на парсинг
    fields_from_task = {
        'shop_id': '125',
        'business_id': str(BUSINESS_ID),
        'datafeed_id': 205,
    }
    assert_that(process_job_mock.feed_data.mbi_params, has_entries(fields_from_task))

    # Данные из shops_dat_parameters
    flag_str = 'true' if flag else 'false'
    fields_from_shops_dat_parameters = {
        'is_discounts_enabled': flag_str,
        'enable_auto_discounts': flag_str,
        'vat': '7',
        'url': str(mds.get_feed_addr(int(feed_id))) if not skip_feedurl else url,
        'blue_status': 'REAL',
        'supplier_type': '3',
        'is_mock': flag_str,
        'is_upload': flag_str,
        'prefix': 'some_prefix',
        'cpa': 'NO',
    }
    assert_that(process_job_mock.feed_data.mbi_params, has_entries(fields_from_shops_dat_parameters))

    # Данные из shops_dat_parameters, которые не должны попадать в mbi_params
    assert_that(process_job_mock.feed_data.mbi_params, is_not(has_key('ignore_stocks')))
    assert_that(process_job_mock.feed_data.mbi_params, is_not(has_key('direct_shipping')))
    assert_that(process_job_mock.feed_data.mbi_params, is_not(has_key('ff_program')))

    # Данные, проставляемые со значением по умолчанию
    fields_with_default_values = {
        'is_online': 'true',
        'is_push_partner': 'true',
        'prepay_requires_vat': 'true',
        'vat_source': 'WEB_AND_FEED'
    }
    assert_that(process_job_mock.feed_data.mbi_params, has_entries(fields_with_default_values))

    # Проверяем, что не проставлено ничего лишнего
    assert_that(len(process_job_mock.feed_data.mbi_params), equal_to(
        len(fields_from_task) + len(fields_from_shops_dat_parameters) + len(fields_with_default_values)
    ))


def test_empty_mbi_params(push_parser, input_topic, technical_topic, mds, process_job_mock):
    """
    Проверяем, что задание на парсинг отправляется в технический топик,
    если параметров нет ни в таблице партнеров ни в задание на парсинг
    """
    # shops_dat_parameters is None
    input_topic.write(make_input_task(
        mds,
        200,
        BUSINESS_ID,
        300,
        warehouse_id=146
    ).SerializeToString())

    # shops_dat_parameters is empty
    input_topic.write(make_input_task(
        mds,
        201,
        BUSINESS_ID,
        301,
        warehouse_id=146,
        shops_dat_parameters=ShopsDatParameters()
    ).SerializeToString())
    push_parser.run(total_sessions=2)

    assert_that(process_job_mock.feed_data, none())

    technical_topic.read(count=2)
    assert_that(technical_topic, HasNoUnreadData())


def test_shop_disabled_since_ts(push_parser, input_topic, technical_topic, mds, process_job_mock):
    """
    Проверяем, что shop_disabled_since_ts заполняется в feed_info
    """

    input_topic.write(make_input_task(
        mds,
        400,
        BUSINESS_ID,
        1235,
        warehouse_id=0,
        shops_dat_parameters=ShopsDatParameters()
    ).SerializeToString())
    push_parser.run(total_sessions=1)

    assert_that(process_job_mock.feed_data.shop_disabled_since_ts, 100)

# coding: utf-8

import pytest
import six
from hamcrest import assert_that, not_

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.proto.ir.UltraController_pb2 as UC

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData
from market.idx.pylibrary.taxes.taxes import ETaxSystem
from market.idx.yatf.matchers.env_matchers import ContainsOfferTraceLogRecord
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from yatest.common.network import PortManager


OFFER_IDS = [
    {
        'shop_id': i,
        'offer_id': 'T{}'.format(i),
        'warehouse_id': 100 * i, 'market_sku_id': 1
    }
    for i in range(1, 7)
]

UC_DATA = [
    {
        'enrich_type': UC.EnrichedOffer.ET_APPROVED,
        'market_sku_published_on_market': False
    }
]


@pytest.yield_fixture(scope='module')
def uc_server():
    with PortManager() as pm:
        port = pm.get_port()
        server = UcHTTPData.from_dict(UC_DATA, port=port)
        yield server


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': 1,
            'mbi':  dict2tskv({
                'shop_id': 1,
                'datafeed_id': 1110,
                'warehouse_id': 11100,
                'tax_system': ETaxSystem.OSN.value,
            }),
            'status': 'publish'
        }
    ]


@pytest.fixture(scope='module')  # noqa
def miner_config(
        log_broker_stuff,
        uc_server,
        yt_server,
        input_topic,
        output_topic,
        offers_blog_topic,
        yt_token,
        partner_info_table_path,
):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=True)
    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    uc_enricher = cfg.create_blue_uc_enricher_processor()
    offer_validator = cfg.create_offer_validator()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, uc_enricher)
    cfg.create_link(uc_enricher, offer_validator)
    cfg.create_link(offer_validator, writer)
    return cfg


@pytest.yield_fixture(scope='module')
def miner(
        miner_config,
        input_topic,
        output_topic,
        uc_server,
        offers_blog_topic,
        yt_server,
        partner_info_table_path,
        partner_data
):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'uc_server': uc_server,
        'offers_blog_topic': offers_blog_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data)
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def write_offer(id, input_topic, output_topic, offer_color=None, otrace_flag=None, wait_output=False):
    identifiers = DTC.OfferIdentifiers(
        shop_id=id['shop_id'],
        offer_id=id['offer_id'],
        warehouse_id=id['warehouse_id'],
    )
    content = DTC.OfferContent(
        binding=DTC.ContentBinding(
            approved=DTC.Mapping(
                market_sku_id=id['market_sku_id']
            )
        )
    )
    meta = None
    tech_info = None
    if offer_color is not None:
        meta = DTC.OfferMeta(
            rgb=offer_color
        )
    if otrace_flag is not None:
        tech_info = DTC.OfferTechInfo(
            otrace_info=DTC.OtraceInfo(
                should_trace=DTC.Flag(
                    flag=otrace_flag
                )
            )
        )

    offer = UnitedOffer(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(offer_id=id['offer_id']),
            content=content,
        ),
        service={
            id['shop_id']: DTC.Offer(
                identifiers=identifiers,
                meta=meta,
                tech_info=tech_info,
            )
        }
    )

    request = UnitedOffersBatch()
    request.offer.extend([offer])
    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())
    if not wait_output:
        return
    return output_topic.read(count=1)


def make_trace_log_record(msg_text, offer_id, http_code='200', error_code=None):
    result = {
        'http_code': http_code,
        'request_method': msg_text,
        'kv.offer_id': offer_id
    }
    if error_code:
        result.update({'error_code': error_code})
    return result


def test_trace_log_contains_blue_start_record(miner, input_topic, output_topic):
    """Проверяем, что в trace log попадает запись о начале обработки оффера синего оффера"""
    write_offer(OFFER_IDS[0], input_topic, output_topic, offer_color=DTC.BLUE, wait_output=True)
    start_parsing_record = make_trace_log_record('Offer processing started', OFFER_IDS[0]['offer_id'])
    assert_that(miner, ContainsOfferTraceLogRecord(start_parsing_record),
                six.ensure_text('Запись о старте парсинга синего оффера попадает в лог'))


def test_trace_log_not_contains_white_start_record(miner, input_topic, output_topic):
    """Проверяем, что в trace log не попадает запись о начале обработки белого оффера без флага трейса"""
    write_offer(OFFER_IDS[1], input_topic, output_topic, offer_color=DTC.WHITE, wait_output=True)
    start_parsing_record = make_trace_log_record('Offer processing started', OFFER_IDS[1]['offer_id'])
    assert_that(miner, not_(ContainsOfferTraceLogRecord(start_parsing_record)),
                six.ensure_text('Нет записи о старте парсинга белого оффера без флага трейса'))


def test_trace_log_contains_white_with_flag_start_record(miner, input_topic, output_topic):
    """Проверяем, что в trace log попадает запись о начале обработки белого оффера с флагом трейса"""
    write_offer(OFFER_IDS[2], input_topic, output_topic, offer_color=DTC.WHITE, otrace_flag=True, wait_output=True)
    start_parsing_record = make_trace_log_record('Offer processing started', OFFER_IDS[2]['offer_id'])
    assert_that(miner, ContainsOfferTraceLogRecord(start_parsing_record),
                six.ensure_text('Запись о старте парсинга белого оффера с флагом трейса попадает в лог'))


def test_trace_log_not_contains_white_finish_record(miner, input_topic, output_topic):
    """Проверяем, что в trace log не попадает запись об окончании обработки белого оффера без флага трейса"""
    write_offer(OFFER_IDS[4], input_topic, output_topic, offer_color=DTC.WHITE, wait_output=True)
    acceptance_record = make_trace_log_record("Offer processing finished", OFFER_IDS[4]['offer_id'])
    assert_that(miner, not_(ContainsOfferTraceLogRecord(acceptance_record)),
                six.ensure_text('Нет записи об окончании парсинга белого оффера без флага трейса'))

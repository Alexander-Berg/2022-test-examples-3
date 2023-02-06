# coding: utf-8
import pytest
import six
import socket
from hamcrest import assert_that

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.utils import LogBrokerEvenlyWriter
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.env_matchers import ContainsOfferTraceLogRecord
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.logbroker_resource import log_broker_stuff  # noqa
from market.idx.yatf.resources.yt_stuff_resource import yt_server  # noqa
from market.idx.yatf.utils.utils import create_pb_timestamp
from market.proto.common.common_pb2 import PriceExpression

import yatest.common

OFFERS = [
    {
        'feed_id': 1,
        'offer_id': six.ensure_text('1001я'),
    }
]

# количество партиций в топике
PARTITIONS_COUNT = 3
TS_CREATED = create_pb_timestamp()


@pytest.fixture(scope='session')
def offers():
    return [DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            feed_id=offer['feed_id'],
            offer_id=offer['offer_id'],
            shop_id=offer['feed_id']
        ),
        meta=DTC.OfferMeta(
            ts_created=TS_CREATED,
        ),
        status=DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=True,
                )
            ]
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                master_data=DTC.MasterData(),
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(),
                    description=DTC.StringValue(),
                    type_prefix=DTC.StringValue(),
                    vendor=DTC.StringValue(),
                    model=DTC.StringValue(),
                    vendor_code=DTC.StringValue(),
                    barcode=DTC.StringListValue(),
                    offer_params=DTC.ProductYmlParams(),
                    group_id=DTC.Ui32Value(),
                    type=DTC.ProductType(),
                    downloadable=DTC.Flag(),
                    adult=DTC.Flag(),
                    age=DTC.Age(),
                    url=DTC.StringValue(),
                    condition=DTC.Condition(),
                    manufacturer_warranty=DTC.Flag(),
                    expiry=DTC.Expiration(),
                    country_of_origin=DTC.StringListValue(),
                    weight=DTC.PreciseWeight(),
                    dimensions=DTC.PreciseDimensions(),
                    supplier_info=DTC.SupplierInfo(),
                    price_from=DTC.Flag(),
                    isbn=DTC.StringListValue(),
                ),
                original_terms=DTC.OriginalTerms(
                    sales_notes=DTC.StringValue(),
                    quantity=DTC.Quantity(),
                    seller_warranty=DTC.Warranty(),
                ),
                actual=DTC.ProcessedSpecification(
                    title=DTC.StringValue(),
                    description=DTC.StringValue(),
                    country_of_origin_id=DTC.I64ListValue(),
                    offer_params=DTC.ProductYmlParams(),
                    price_from=DTC.Flag(),
                    adult=DTC.Flag(),
                    age=DTC.Age(),
                    barcode=DTC.StringListValue(),
                    expiry=DTC.Expiration(),
                    manufacturer_warranty=DTC.Flag(),
                    url=DTC.StringValue(),
                    weight=DTC.PreciseWeight(),
                    dimensions=DTC.PreciseDimensions(),
                    downloadable=DTC.Flag(),
                    sales_notes=DTC.StringValue(),
                    type_prefix=DTC.StringValue(),
                    type=DTC.ProductType(),
                    quantity=DTC.Quantity(),
                    seller_warranty=DTC.Warranty(),
                    isbn=DTC.StringListValue(),
                ),
            ),
            market=DTC.MarketContent(),
            binding=DTC.ContentBinding(),
        ),
        pictures=DTC.OfferPictures(
            partner=DTC.PartnerPictures(),
            market=DTC.MarketPictures(),
        ),
        price=DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=PriceExpression(
                    id='RUR',
                    price=10,
                )
            )
        ),
        delivery=DTC.OfferDelivery(
            specific=DTC.SpecificDeliveryOptions(),
            calculator=DTC.DeliveryCalculatorOptions(),
            delivery_info=DTC.DeliveryInfo(),
        ),
        order_properties=DTC.OfferOrderProperties(),
        bids=DTC.OfferBids(),
        partner_info=DTC.PartnerInfo(),
        stock_info=DTC.OfferStockInfo(),
    ) for offer in OFFERS]


@pytest.fixture(scope='session')
def lbk_topic(log_broker_stuff):  # noqa
    topic = LbkTopic(log_broker_stuff, partitions_count=PARTITIONS_COUNT)
    topic.create()
    return topic


@pytest.fixture(scope='session')
def trace_log_path():
    return yatest.common.output_path('trace.log')


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, lbk_topic, trace_log_path):  # noqa
    cfg = {
        'general': {
            'worker_count': 3,
            'batch_size': 3,
        },
        'logbroker': {
            'max_read_count': 3,
            'offers_topic': lbk_topic.topic,
        },
        'trace_log': {
            'is_enabled': True,
            'trace_log_path': trace_log_path
        }
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def controller(yt_server, log_broker_stuff, config, lbk_topic, trace_log_path):  # noqa
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
        'trace_log_path': trace_log_path
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as controller_env:
        controller_env.verify()
        yield controller_env


@pytest.fixture(scope='module')
def inserter(offers, controller, lbk_topic):
    writer = LogBrokerEvenlyWriter(lbk_topic)
    for offer in offers:
        writer.write(offer.SerializeToString())

    wait_until(lambda: controller.offers_processed >= len(offers))


@pytest.mark.skip(reason="rework for new piper")
def test_processing_start_record(inserter, offers, controller):  # noqa
    expected = {
        "target_host": socket.gethostname(),
        "request_method": 'Offer processing started',
        "error_code": '',
        "http_code": '200',
        "kv.offer_id": '1001я',
        "kv.feed_id": '1\n'
    }
    assert_that(controller, ContainsOfferTraceLogRecord(expected))


@pytest.mark.skip(reason="rework for new piper")
def test_processing_finished_record(inserter, offers, controller):  # noqa
    expected = {
        "target_host": socket.gethostname(),
        "request_method": 'Offer processing finished',
        "error_code": '',
        "http_code": '200',
        "kv.offer_id": '1001я',
        "kv.feed_id": '1\n'
    }
    assert_that(controller, ContainsOfferTraceLogRecord(expected))

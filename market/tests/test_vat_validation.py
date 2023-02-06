# coding: utf-8

from datetime import(
    datetime,
    timedelta
)
from hamcrest import assert_that
import pytest

from market.idx.datacamp.proto.offer.OfferBlog_pb2 import OfferBlog
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    DeliveryCalculatorOptions,
    Flag,
    Offer as DataCampOffer,
    OfferDelivery,
    OfferExtraIdentifiers,
    OfferIdentifiers,
    OfferPrice,
    OfferStatus,
    MARKET_IDX,
    PriceBundle,
    UpdateMeta
)
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedProtos
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.pylibrary.taxes.taxes import (
    EVat,
    ETaxSystem
)

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.proto.common import process_log_pb2 as ProcessLog
from market.proto.common.common_pb2 import (
    EComponent,
    PriceExpression
)


INCONSISTENT_OFFER_IDENTIFIERS = {
    'shop_id': 2,
    'offer_id': 'InconsistentVatOffer',
    'extra': {
        'market_sku_id': 10003,
        'ware_md5': 'KXGI8T3GP_pqjgdd7HfoHQ',
        'classifier_good_id': '23',
        'classifier_magic_id2': '203'
    }
}

OFFERS = [
    {
        'shop_id': 1,
        'offer_id': 'NoVatOffer',
        'market_sku_id': 10001,
        'ware_md5': 'BH8EPLtKmdLQhLUasgaOnA',
        'classifier_good_id': '11',
        'classifier_magic_id2': '101',
        'price': 100,
        'vat': EVat.NO_VAT.value
    },
    {
        'shop_id': 1,
        'offer_id': 'Vat18Offer',
        'market_sku_id': 10002,
        'ware_md5': 'bpQ3a9LXZAl_Kz34vaOpSg',
        'classifier_good_id': '12',
        'classifier_magic_id2': '102',
        'price': 150,
        'vat': EVat.VAT_18.value
    },
    {
        'shop_id': INCONSISTENT_OFFER_IDENTIFIERS['shop_id'],
        'offer_id': INCONSISTENT_OFFER_IDENTIFIERS['offer_id'],
        'market_sku_id': INCONSISTENT_OFFER_IDENTIFIERS['extra']['market_sku_id'],
        'ware_md5': INCONSISTENT_OFFER_IDENTIFIERS['extra']['ware_md5'],
        'classifier_good_id': INCONSISTENT_OFFER_IDENTIFIERS['extra']['classifier_good_id'],
        'classifier_magic_id2': INCONSISTENT_OFFER_IDENTIFIERS['extra']['classifier_magic_id2'],
        'price': 200,
        'vat': EVat.VAT_0.value
    },
    {
        'shop_id': 2,
        'offer_id': 'AnotherNoVatOffer',
        'market_sku_id': 10004,
        'ware_md5': 'yRgmzyBD4j8r4rkCby6Iuw',
        'classifier_good_id': '24',
        'classifier_magic_id2': '204',
        'price': 250,
        'vat': EVat.NO_VAT.value
    }
]

BLUE_EXPECTED_MESSAGES = [{
    'identifiers': INCONSISTENT_OFFER_IDENTIFIERS,
    'errors': {
        'error': [{
            'code': '45h',
            'level': ProcessLog.ERROR,
            'namespace': ProcessLog.OFFER,
            'source': EComponent.MINER
        }]
    }
}] + [{
    'errors': {
        'error': [{
            'code': '49i',
            'level': ProcessLog.ERROR,
            'source': EComponent.MINER
        }]
    }
}] * 12  # по 3 ошибки 49i (no_warehouse_id, no_supplier_id, no_market_sku_id) на каждый из 4 офферов


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': 1,
            'mbi': dict2tskv({
                'shop_id': 1,
                'tax_system': ETaxSystem.OSN.value
            }),
            'status': 'publish'
        },
        {
            'shop_id': 2,
            'mbi': dict2tskv({
                'shop_id': 2,
                'tax_system': ETaxSystem.USN.value
            }),
            'status': 'publish'
        }
    ]


@pytest.fixture(scope='module', params=['blue'])
def color(request):
    return request.param


@pytest.fixture(scope='module')
def miner_config(
    yt_server,
    log_broker_stuff,
    input_topic,
    output_topic,
    offers_blog_topic,
    yt_token,
    partner_info_table_path,
    color
):
    cfg = MinerConfig()

    cfg.create_datacamp_logger_initializer(
        log_broker_stuff=log_broker_stuff,
        offers_blog_topic=offers_blog_topic
    )

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(
        log_broker_stuff=log_broker_stuff,
        topic=input_topic,
        united=True,
    )
    writer = cfg.create_lbk_writer_processor(
        log_broker_stuff=log_broker_stuff,
        topic=output_topic
    )
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    shopsdat_enricher = cfg.create_shopsdat_enricher(color=color)
    offer_validator = cfg.create_offer_validator(color=color)

    cfg.create_link(link_from=reader, link_to=unpacker)
    cfg.create_link(link_from=unpacker, link_to=adapter_converter)
    cfg.create_link(link_from=adapter_converter, link_to=shopsdat_enricher)
    cfg.create_link(link_from=shopsdat_enricher, link_to=offer_validator)
    cfg.create_link(link_from=offer_validator, link_to=writer)

    return cfg


@pytest.fixture(scope='module')
def miner(
    yt_server,
    miner_config,
    input_topic,
    output_topic,
    offers_blog_topic,
    partner_info_table_path,
    partner_data
):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'offers_blog_topic': offers_blog_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data
        )
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.fixture(scope='module')
def offers_blog_messages(offers_blog_topic, color):
    if color == 'blue':
        return offers_blog_topic.read(len(BLUE_EXPECTED_MESSAGES))


@pytest.fixture(scope='module')
def workflow(miner, input_topic, output_topic):
    request = UnitedOffersBatch()
    for o in OFFERS:
        old_time = (datetime.utcnow() - timedelta(minutes=45)).strftime('%Y-%m-%dT%H:%M:%SZ')
        offer = UnitedOffer(
            basic=DataCampOffer(
                identifiers=OfferIdentifiers(
                    offer_id=o['offer_id'],
                    extra=OfferExtraIdentifiers(
                        market_sku_id=o['market_sku_id'],
                        classifier_good_id=o['classifier_good_id'],
                        classifier_magic_id2=o['classifier_magic_id2']
                    )
                ),
            ),
            service={
                o['shop_id']: DataCampOffer(
                    identifiers=OfferIdentifiers(
                        shop_id=o['shop_id'],
                        offer_id=o['offer_id'],
                        extra=OfferExtraIdentifiers(
                            ware_md5=o['ware_md5'],
                            market_sku_id=o['market_sku_id'],
                            classifier_good_id=o['classifier_good_id'],
                            classifier_magic_id2=o['classifier_magic_id2']
                        )
                    ),
                    meta=create_meta(10, color=DTC.BLUE),
                    status=OfferStatus(
                        disabled=[Flag(
                            flag=False,
                            meta=UpdateMeta(
                                source=MARKET_IDX,
                                timestamp=create_timestamp_from_json(old_time)
                            )
                        )]
                    ),
                    price=OfferPrice(
                        basic=PriceBundle(
                            binary_price=PriceExpression(price=int(o['price']*10**7)),
                            vat=o['vat']
                        )
                    ),
                    delivery=OfferDelivery(
                        calculator=DeliveryCalculatorOptions(delivery_bucket_ids=[12345, 67890])
                    )
                )
            },
        )
        request.offer.extend([offer])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    yield output_topic.read(count=1)


def test_offer_messages(workflow, offers_blog_messages, color):
    if color == 'blue':
        assert_that(
            offers_blog_messages,
            HasSerializedProtos(items=BLUE_EXPECTED_MESSAGES, proto_cls=OfferBlog)
        )

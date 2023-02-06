# coding: utf-8

import uuid
import pytest

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPartnersTable,
    DataCampExternalBasicOffersTable,
    DataCampExternalServiceOffersTable,
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampMskuTable
)
from market.idx.yatf.resources.yt_tables.stock_sku_table import StockSkuTable
from datetime import datetime

import market.idx.datacamp.proto.tables.PiperQueueDumpSchema_pb2 as PQDS
from market.click_n_collect.tools.yt_tables_deployer.library.protobuf_format import get_schema


@pytest.fixture(scope='module', params=['white'])
def color(request):
    return request.param


@pytest.fixture(scope='module')
def partners_table_data():
    return []


@pytest.fixture(scope='module')
def partners_table_path():
    return '//home/partners' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def partners_table(yt_server, partners_table_path, partners_table_data):
    return DataCampPartnersTable(
        yt_server,
        partners_table_path,
        data=partners_table_data
    )


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return []


@pytest.fixture(scope='module')
def basic_offers_table_path():
    return '//home/basic_offers' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, basic_offers_table_path, basic_offers_table_data):
    return DataCampBasicOffersTable(
        yt_server,
        basic_offers_table_path,
        data=basic_offers_table_data
    )


@pytest.fixture(scope='module')
def service_offers_table_data():
    return []


@pytest.fixture(scope='module')
def service_offers_table_path():
    return '//home/service_offers' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def service_offers_table(yt_server, service_offers_table_path, service_offers_table_data):
    return DataCampServiceOffersTable(
        yt_server,
        service_offers_table_path,
        data=service_offers_table_data
    )


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return []


@pytest.fixture(scope='module')
def actual_service_offers_table_path():
    return '//home/actual_service_offers' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, actual_service_offers_table_path, actual_service_offers_table_data):
    return DataCampServiceOffersTable(
        yt_server,
        actual_service_offers_table_path,
        data=actual_service_offers_table_data
    )


@pytest.fixture(scope='module')
def lavka_basic_offers_table_data():
    return []


@pytest.fixture(scope='module')
def lavka_basic_offers_table_path():
    return '//home/lavka_basic_offers' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def lavka_basic_offers_table(yt_server, lavka_basic_offers_table_path, lavka_basic_offers_table_data):
    return DataCampExternalBasicOffersTable(
        yt_server,
        lavka_basic_offers_table_path,
        data=lavka_basic_offers_table_data
    )


@pytest.fixture(scope='module')
def lavka_service_offers_table_data():
    return []


@pytest.fixture(scope='module')
def lavka_service_offers_table_path():
    return '//home/lavka_service_offers' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def lavka_service_offers_table(yt_server, lavka_service_offers_table_path, lavka_service_offers_table_data):
    return DataCampExternalServiceOffersTable(
        yt_server,
        lavka_service_offers_table_path,
        data=lavka_service_offers_table_data
    )


@pytest.fixture(scope='module')
def eda_basic_offers_table_data():
    return []


@pytest.fixture(scope='module')
def eda_basic_offers_table_path():
    return '//home/eda_basic_offers' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def eda_basic_offers_table(yt_server, eda_basic_offers_table_path, eda_basic_offers_table_data):
    return DataCampExternalBasicOffersTable(
        yt_server,
        eda_basic_offers_table_path,
        data=eda_basic_offers_table_data
    )


@pytest.fixture(scope='module')
def eda_service_offers_table_data():
    return []


@pytest.fixture(scope='module')
def eda_service_offers_table_path():
    return '//home/eda_service_offers' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def eda_service_offers_table(yt_server, eda_service_offers_table_path, eda_service_offers_table_data):
    return DataCampExternalServiceOffersTable(
        yt_server,
        eda_service_offers_table_path,
        data=eda_service_offers_table_data
    )


@pytest.fixture(scope='module')
def datacamp_msku_table_data():
    return []


@pytest.fixture(scope='module')
def datacamp_msku_table_path():
    return '//home/msku' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def datacamp_msku_table(yt_server, datacamp_msku_table_path, datacamp_msku_table_data):
    return DataCampMskuTable(
        yt_server,
        datacamp_msku_table_path,
        data=datacamp_msku_table_data
    )


@pytest.fixture(scope='module')
def abo_hidings_table_path():
    return '//home/abo_hidings' + str(uuid.uuid4()) + '/2021-01-01T00:00:00'


@pytest.fixture(scope='module')
def abo_hidings_table_data():
    return []


@pytest.fixture(scope='module')
def abo_hidings_table(yt_server, abo_hidings_table_path, abo_hidings_table_data):
    return YtTableResource(
        yt_server,
        abo_hidings_table_path,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='business_id', type='int64'),
                dict(name='shop_id', type='int64'),
                dict(name='shop_sku', type='string'),
                dict(name='market_sku', type='int64'),
                dict(name='reason', type='string'),
                dict(name='timestamp', type='int64'),
                dict(name='is_deleted', type='boolean'),
                dict(name='verdict', type='string'),
            ]
        ),
        data=abo_hidings_table_data
    )


@pytest.fixture(scope='module')
def mbo_hidings_table_path():
    return '//home/mbo_hidings' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def mbo_hidings_table_data():
    return []


@pytest.fixture(scope='module')
def mbo_hidings_table(yt_server, mbo_hidings_table_path, mbo_hidings_table_data):
    return YtTableResource(
        yt_server,
        mbo_hidings_table_path,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='raw_supplier_id', type='uint32'),
                dict(name='raw_shop_sku', type='string'),
                dict(name='supplier_id', type='uint32'),
                dict(name='shop_sku', type='string'),
                dict(name='erp_real_supplier_id', type='string'),
                dict(name='market_sku_id', type='uint64'),
                dict(name='warehouse_id', type='uint64'),
                dict(name='data', type='string'),
            ]
        ),
        data=mbo_hidings_table_data
    )


@pytest.fixture(scope='module')
def promo_table_path():
    return '//home/promo' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def promo_table_data():
    yield []


@pytest.fixture(scope='module')
def promo_table(yt_server, promo_table_path, promo_table_data):
    return YtTableResource(
        yt_server,
        promo_table_path,
        data=promo_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='shop_id', type='uint64'),
                dict(name='warehouse_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='promos', type='string')
            ]
        )
    )


@pytest.fixture(scope='module')
def vertical_approved_table_path():
    return '//home/vertical_approved' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def vertical_approved_table_data():
    yield []


@pytest.fixture(scope='module')
def vertical_approved_table(yt_server, vertical_approved_table_path, vertical_approved_table_data):
    return YtTableResource(
        yt_server,
        vertical_approved_table_path,
        data=vertical_approved_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='timestamp', type='int64'),
                dict(name='business_id', type='uint32'),
                dict(name='shop_id', type='uint32'),
                dict(name='offer_id', type='string'),
                dict(name='vertical_approved', type='boolean')
            ]
        )
    )


@pytest.fixture(scope='module')
def offers_diff_table_path():
    return '//home/offers_diff' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def offers_diff_table_data():
    yield []


@pytest.fixture(scope='module')
def offers_diff_table(yt_server, offers_diff_table_path, offers_diff_table_data):
    return YtTableResource(
        yt_server,
        offers_diff_table_path,
        data=offers_diff_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='business_id', type='uint32'),
                dict(name='offer_id', type='string'),
                dict(name='offer', type='string'),
            ]
        )
    )


@pytest.fixture(scope='module')
def piper_queue_dump_table_path():
    return '//home/piper_queue_dump' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def piper_queue_dump_table_data():
    yield []


@pytest.fixture(scope='module')
def piper_queue_dump_table(yt_server, piper_queue_dump_table_path, piper_queue_dump_table_data):
    return YtTableResource(
        yt_server,
        piper_queue_dump_table_path,
        data=piper_queue_dump_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=get_schema(PQDS.PiperQueueDumpRow)
        )
    )


@pytest.fixture(scope='module')
def stock_sku_table_path():
    NOW_UTC = datetime.utcnow()
    time_pattern = "%Y-%m-%dT"
    return '//home/stock_sku' + str(uuid.uuid4()) + "/" + NOW_UTC.strftime(time_pattern)


@pytest.fixture(scope='module')
def stock_sku_table_data():
    yield []


@pytest.fixture(scope='module')
def stock_sku_table(yt_server, stock_sku_table_path, stock_sku_table_data):
    return StockSkuTable(yt_server, stock_sku_table_path, stock_sku_table_data)


@pytest.fixture(scope='module')
def report_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, topic='rty-white-0-0')
    return topic


@pytest.fixture(scope='module')
def report_turbo_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, topic='rty-turbo-0-0')
    return topic


@pytest.fixture(scope='module')
def report_topic_shards(log_broker_stuff):
    return [LbkTopic(log_broker_stuff, topic='rty-white-{0}-{0}'.format(_)) for _ in range(16)] +\
           [LbkTopic(log_broker_stuff, topic='rty-turbo-{0}-{0}'.format(_)) for _ in range(16)]


@pytest.fixture(scope='module')
def iris_subscription_internal_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, topic='iris-subscription-internal-topic')
    return topic


@pytest.fixture(scope='module')
def offer_status_table_path():
    return '//home/offer_status' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def offer_status_table_data():
    yield []


@pytest.fixture(scope='module')
def offer_status_table(yt_server, offer_status_table_path, offer_status_table_data):
    return YtTableResource(
        yt_server,
        offer_status_table_path,
        data=offer_status_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='offer', type='string')
            ]
        )
    )


@pytest.fixture(scope='module')
def offer_status_batched_table_path():
    return '//home/offer_status_batched' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def offer_status_batched_table_data():
    yield []


@pytest.fixture(scope='module')
def offer_status_table_batched(yt_server, offer_status_batched_table_path, offer_status_batched_table_data):
    return YtTableResource(
        yt_server,
        offer_status_batched_table_path,
        data=offer_status_batched_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='batch', type='string')
            ]
        )
    )


@pytest.fixture(scope='module')
def fresh_offer_status_batched_table_path():
    return '//home/fresh_offer_status_batched' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def fresh_offer_status_batched_table_data():
    yield []


@pytest.fixture(scope='module')
def fresh_offer_status_table_batched(yt_server, fresh_offer_status_batched_table_path, fresh_offer_status_batched_table_data):
    return YtTableResource(
        yt_server,
        fresh_offer_status_batched_table_path,
        data=fresh_offer_status_batched_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='batch', type='string')
            ]
        )
    )


@pytest.fixture(scope='module')
def saas_diff_table_path():
    return '//home/saas_diff' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def saas_diff_table_data():
    yield []


@pytest.fixture(scope='module')
def saas_diff_table(yt_server, saas_diff_table_path, saas_diff_table_data):
    return YtTableResource(
        yt_server,
        saas_diff_table_path,
        data=saas_diff_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='offer', type='string')
            ]
        )
    )


@pytest.fixture(scope='module')
def resolved_redirect_table_path():
    return '//home/resolved_redirect' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def resolved_redirect_table_data():
    return []


@pytest.fixture(scope='module')
def resolved_redirect_table(yt_server, resolved_redirect_table_path, resolved_redirect_table_data):
    return YtTableResource(
        yt_server,
        resolved_redirect_table_path,
        data=resolved_redirect_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='offer', type='string')
            ]
        )
    )


# https://yt.yandex-team.ru/arnold/navigation?path=//home/market/production/mbo/export/recent/models/sku
@pytest.fixture(scope='module')
def mbo_msku_table_path():
    return '//home/mbo_msku_table' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def mbo_msku_table_data():
    return []


@pytest.fixture(scope='module')
def mbo_msku_table(yt_server, mbo_msku_table_path, mbo_msku_table_data):
    return YtTableResource(
        yt_server,
        mbo_msku_table_path,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='category_id', type='uint64'),
                dict(name='vendor_id', type='uint64'),
                dict(name='model_id', type='uint64'),
                dict(name='parent_id', type='uint64'),
                dict(name='source_type', type='string'),
                dict(name='current_type', type='string'),
                dict(name='sku_parent_model_id', type='uint64'),
                dict(name='is_sku', type='boolean'),
                dict(name='title', type='string'),
                dict(name='published', type='boolean'),
                dict(name='blue_published', type='boolean'),
                dict(name='created_date', type='string'),
                dict(name='deleted_date', type='string'),
                dict(name='data', type='string'),
            ]
        ),
        data=mbo_msku_table_data
    )


@pytest.fixture(scope='module')
def delivery_diff_table_path():
    return '//home/delivery_diff' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def delivery_diff_table_data():
    yield []


@pytest.fixture(scope='module')
def delivery_diff_table(yt_server, delivery_diff_table_path, delivery_diff_table_data):
    return YtTableResource(
        yt_server,
        delivery_diff_table_path,
        data=delivery_diff_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='business_id', type='uint32'),
                dict(name='offer_id', type='string'),
                dict(name='shop_id', type='uint32'),
                dict(name='warehouse_id', type='uint32'),
                dict(name='offer', type='string'),
            ]
        )
    )


@pytest.fixture(scope='module')
def sortdc_updates_table_path():
    return '//home/sortdc_updates' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def sortdc_updates_table_data():
    yield []


@pytest.fixture(scope='module')
def sortdc_updates_table(yt_server, sortdc_updates_table_path, sortdc_updates_table_data):
    return YtTableResource(
        yt_server,
        sortdc_updates_table_path,
        data=sortdc_updates_table_data,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='BusinessId', type='uint32'),
                dict(name='OfferId', type='string'),
                dict(name='OfferYabsId', type='uint64'),
                dict(name='ShopId', type='uint32'),
                dict(name='WarehouseId', type='uint32'),
                dict(name='UrlHashFirst', type='uint64'),
                dict(name='UrlHashSecond', type='uint64'),
                dict(name='Rank', type='float'),
                dict(name='Data', type='string')
            ]
        )
    )


@pytest.fixture(scope='module')
def scanner_resources(
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    datacamp_msku_table,
    partners_table,
    promo_table,
    vertical_approved_table,
    offers_diff_table,
    piper_queue_dump_table,
    abo_hidings_table,
    mbo_hidings_table,
    stock_sku_table,
    report_topic,
    report_turbo_topic,
    offer_status_table,
    offer_status_table_batched,
    fresh_offer_status_table_batched,
    mbo_msku_table,
    report_topic_shards,
    iris_subscription_internal_topic,
    lavka_basic_offers_table,
    lavka_service_offers_table,
    eda_basic_offers_table,
    eda_service_offers_table,
    saas_diff_table,
    delivery_diff_table,
    sortdc_updates_table,
    resolved_redirect_table,
):
    return {
        'partners_table': partners_table,
        'promo_table': promo_table,
        'vertical_approved_table': vertical_approved_table,
        'offers_diff_table': offers_diff_table,
        'piper_queue_dump_table': piper_queue_dump_table,
        'abo_hiding_table': abo_hidings_table,
        'mbo_hiding_table': mbo_hidings_table,
        'stock_sku_table': stock_sku_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'datacamp_msku_table': datacamp_msku_table,
        'report_topic': report_topic,
        'report_turbo_topic': report_turbo_topic,
        'offer_status_table': offer_status_table,
        'offer_status_table_batched': offer_status_table_batched,
        'fresh_offer_status_table_batched': fresh_offer_status_table_batched,
        'mbo_msku_table': mbo_msku_table,
        'report_topic_shards': report_topic_shards,
        'iris_subscription_internal_topic': iris_subscription_internal_topic,
        'lavka_basic_offers_table': lavka_basic_offers_table,
        'lavka_service_offers_table': lavka_service_offers_table,
        'eda_basic_offers_table': eda_basic_offers_table,
        'eda_service_offers_table': eda_service_offers_table,
        'saas_diff_table': saas_diff_table,
        'delivery_diff_table': delivery_diff_table,
        'sortdc_updates_table': sortdc_updates_table,
        'resolved_redirect_table': resolved_redirect_table,
    }

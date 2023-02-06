# coding: utf-8

import uuid
import pytest
import yatest
import os

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBusinessStatusTable,
    DataCampPartnersTable,
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPromoTable,
    DataCampBasicSearchOffersTable,
    DataCampServiceSearchOffersTable,
    DataCampActualServiceSearchOffersTable
)
from market.idx.datacamp.controllers.stroller.yatf.resources.config_mock import StrollerConfigMock
from market.idx.yatf.resources.lbk_topic import LbkTopic


@pytest.fixture(scope='module', params=['white'])
def color_name(request):
    return request.param


@pytest.fixture(scope='module', params=[False])
def enable_search_tables(request):
    return request.param


@pytest.fixture(scope='module')
def config(color_name, yt_server, log_broker_stuff, enable_search_tables):
    cfg = StrollerConfigMock(
        yt_server,
        log_broker_stuff,
        config={
            'general': {
                'color': color_name,
            }
        }
    )
    cfg.tvm_client_id = 0
    cfg.report_topic = 'topic-to-report' + str(uuid.uuid4())
    cfg.loyalty_promos_topic = 'topic-to-loyalty-promos' + str(uuid.uuid4())
    cfg.united_offers_topic = 'topic-to-united-offers' + str(uuid.uuid4())
    cfg.refresh_file_minutes = 1
    cfg.currency_rates_file_path = os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'yatf', 'resources', 'stubs', 'getter', 'mbi', 'currency_rates.xml'
    )
    cfg.enable_search_tables_select = enable_search_tables
    return cfg


@pytest.fixture(scope='module')
def united_offers_topic(config, log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, topic=config.united_offers_topic)
    return topic


@pytest.fixture(scope='module')
def partners():
    return []


@pytest.fixture(scope='module')
def basic_offers():
    return []


@pytest.fixture(scope='module')
def service_offers():
    return []


@pytest.fixture(scope='module')
def actual_service_offers():
    return []


@pytest.fixture(scope='module')
def business_status():
    return []


@pytest.fixture(scope='module')
def promo():
    return []


@pytest.fixture(scope='module')
def basic_search_offers():
    return []


@pytest.fixture(scope='module')
def service_search_offers():
    return []


@pytest.fixture(scope='module')
def actual_service_search_offers():
    return []


@pytest.fixture(scope='module')
def partners_table(yt_server, config, partners):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, data=partners)


@pytest.fixture(scope='module')
def business_status_table(yt_server, config, business_status):
    return DataCampBusinessStatusTable(yt_server, config.yt_business_status_tablepath, data=business_status)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config, basic_offers):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=basic_offers)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config, service_offers):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=service_offers)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config, actual_service_offers):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=actual_service_offers)


@pytest.fixture(scope='module')
def promo_table(yt_server, config, promo):
    return DataCampPromoTable(yt_server, config.yt_promo_tablepath, data=promo)


@pytest.fixture(scope='module')
def basic_search_offers_table(yt_server, config, basic_search_offers):
    return DataCampBasicSearchOffersTable(yt_server, config.yt_basic_search_offers_tablepath, data=basic_search_offers)


@pytest.fixture(scope='module')
def service_search_offers_table(yt_server, config, service_search_offers):
    return DataCampServiceSearchOffersTable(yt_server, config.yt_service_search_offers_tablepath, data=service_search_offers)


@pytest.fixture(scope='module')
def actual_service_search_offers_table(yt_server, config, actual_service_search_offers):
    return DataCampActualServiceSearchOffersTable(yt_server, config.yt_actual_service_search_offers_tablepath, data=actual_service_search_offers)

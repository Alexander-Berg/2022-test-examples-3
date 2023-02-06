# coding: utf-8

import pytest
import yatest
import os
import uuid

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPartnersTable,
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampBusinessStatusTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.test_envs.saas_env import SaasEnv
from market.idx.datacamp.controllers.stroller.yatf.resources.config_mock import StrollerConfigMock


@pytest.fixture(scope='module', params=['white'])
def color_name(request):
    return request.param


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def config(color_name, yt_server, log_broker_stuff, subscription_service_topic):
    cfg = StrollerConfigMock(
        yt_server,
        log_broker_stuff,
        config={
            'general': {
                'color': color_name,
                'enable_subscription_service_sender': True,
            },
            'logbroker': {
                'subscription_service_topic': subscription_service_topic.topic
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
    return cfg


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
def saas():
    rel_path = os.path.join('market', 'idx', 'yatf', 'resources', 'saas', 'stubs', 'market-idxapi')
    with SaasEnv(saas_service_configs=yatest.common.source_path(rel_path), prefixed=True) as saas:
        yield saas

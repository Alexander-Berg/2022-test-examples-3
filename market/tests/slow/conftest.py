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
from market.idx.yatf.test_envs.saas_env import SaasEnv
from market.idx.datacamp.controllers.stroller.yatf.resources.config_mock import StrollerConfigMock


@pytest.fixture(params=['white'])
def color_name(request):
    return request.param


@pytest.fixture()
def config(color_name, yt_server, log_broker_stuff):
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
    return cfg


@pytest.fixture()
def partners():
    return []


@pytest.fixture()
def basic_offers():
    return []


@pytest.fixture()
def service_offers():
    return []


@pytest.fixture()
def actual_service_offers():
    return []


@pytest.fixture()
def business_status():
    return []


@pytest.fixture()
def partners_table(yt_server, config, partners):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, data=partners)


@pytest.fixture()
def business_status_table(yt_server, config, business_status):
    return DataCampBusinessStatusTable(yt_server, config.yt_business_status_tablepath, data=business_status)


@pytest.fixture()
def basic_offers_table(yt_server, config, basic_offers):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=basic_offers)


@pytest.fixture()
def service_offers_table(yt_server, config, service_offers):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=service_offers)


@pytest.fixture()
def actual_service_offers_table(yt_server, config, actual_service_offers):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=actual_service_offers)


@pytest.fixture(scope='module')
def saas():
    rel_path = os.path.join('market', 'idx', 'yatf', 'resources', 'saas', 'stubs', 'market-idxapi')
    with SaasEnv(saas_service_configs=yatest.common.source_path(rel_path), prefixed=True) as saas:
        yield saas

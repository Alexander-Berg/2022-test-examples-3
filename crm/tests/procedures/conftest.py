import pytest
import datetime
import os
from decimal import Decimal
from crm.agency_cabinet.agencies.server.src.db import models
from crm.agency_cabinet.agencies.server.src.db.engine import db
from crm.agency_cabinet.agencies.server.src.config import db_config
from crm.agency_cabinet.common.testing import run_alembic_command


pytest_plugins = ['crm.agency_cabinet.agencies.server.tests.plugin']


RELATIVE_ALEMBIC_BIN_PATH = os.path.join('crm/agency_cabinet/agencies/server/migrations/', 'agencies-migrations')


@pytest.fixture(scope='session', autouse=True)
async def db_bind():
    await db.set_bind(bind=str(db_config.db['dsn']))
    run_alembic_command(RELATIVE_ALEMBIC_BIN_PATH, 'upgrade head')


@pytest.fixture(scope='module')
async def fixture_agency():
    res = await models.Agency.create(
        name='Королевство полной луны',
        phone='999-999-999',
        email='moon@light.com',
        site='moon.light.com',
        actual_address='',
        legal_address=''
    )
    yield res
    await res.delete()


@pytest.fixture(scope='module')
async def fixture_agency2():
    res = await models.Agency.create(
        name='Гранд Будапешт',
        phone='123-123-123',
        email='grand@budapest.com',
        site='grand.budapest.com',
        actual_address='',
        legal_address=''
    )
    yield res
    await res.delete()


@pytest.fixture(scope='module')
async def fixture_agency3():
    res = await models.Agency.create(
        name='Fantastic Mr. Fox',
        phone='777-777-777',
        email='fantastic@misterfox.com',
        site='fantastic.misterfox.com',
        actual_address='',
        legal_address=''
    )
    yield res
    await res.delete()


@pytest.fixture(scope='module')
async def fixture_client(fixture_agency: models.Agency):
    res = await models.Client.create(
        name='Билл Мюррей',
        agency_id=fixture_agency.id,
        login='billy',
    )
    yield res
    await res.delete()


@pytest.fixture(scope='module')
async def fixture_client2(fixture_agency: models.Agency):
    res = await models.Client.create(
        name='Оуэн Уилсон',
        agency_id=fixture_agency.id,
        login='wilsono',
    )
    yield res
    await res.delete()


@pytest.fixture(scope='module')
async def fixture_client3(fixture_agency2: models.Agency):
    res = await models.Client.create(
        name='Эдвард Нортон',
        agency_id=fixture_agency2.id,
        login='norton',
    )
    yield res
    await res.delete()


@pytest.fixture(scope='module')
async def fixture_analytics(
    fixture_agency3,
    fixture_agency2,
    fixture_agency,
    fixture_client,
    fixture_client2,
    fixture_client3
):
    rows = [
        {
            'agency_id': fixture_agency.id,
            'client_id': fixture_client.id,
            'all_money': Decimal(100000),
            'check_ctg': 0,
            'cohort_date': datetime.datetime(2020, 2, 2),
            'month': datetime.date(2020, 2, 1),
            'domain': 'test',
            'epoch': 0,
            'role': 'agency',
            'tier': 't1'
        },
        {
            'agency_id': fixture_agency.id,
            'client_id': fixture_client.id,
            'all_money': Decimal(200000),
            'check_ctg': 1,
            'cohort_date': datetime.datetime(2020, 2, 2),
            'month': datetime.date(2020, 3, 1),
            'domain': 'test',
            'epoch': 1,
            'role': 'agency',
            'tier': 't1'
        },
        {
            'agency_id': fixture_agency.id,
            'client_id': fixture_client2.id,
            'all_money': Decimal(150000),
            'check_ctg': 1,
            'cohort_date': datetime.datetime(2020, 2, 2),
            'month': datetime.date(2020, 3, 1),
            'domain': 'test',
            'epoch': 0,
            'role': 'agency',
            'tier': 't1'
        },
        {
            'agency_id': fixture_agency.id,
            'client_id': fixture_client2.id,
            'all_money': Decimal(850000),
            'check_ctg': 1,
            'cohort_date': datetime.datetime(2020, 2, 2),
            'month': datetime.date(2020, 4, 1),
            'domain': 'test',
            'epoch': 1,
            'role': 'agency',
            'tier': 't1'
        },
        {
            'agency_id': fixture_agency2.id,
            'client_id': fixture_client3.id,
            'all_money': Decimal(10000),
            'check_ctg': 1,
            'cohort_date': datetime.datetime(2020, 2, 2),
            'month': datetime.date(2020, 3, 1),
            'domain': 'test',
            'epoch': 0,
            'role': 'agency',
            'tier': 't1'
        },
        {
            'agency_id': fixture_agency2.id,
            'client_id': fixture_client3.id,
            'all_money': Decimal(1000000),
            'check_ctg': 1,
            'cohort_date': datetime.datetime(2020, 2, 2),
            'month': datetime.date(2020, 4, 1),
            'domain': 'test',
            'epoch': 1,
            'role': 'agency',
            'tier': 't1'
        }
    ]
    res = await models.AgencyAnalytics.bulk_insert(rows)
    yield res
    await models.AgencyAnalytics.delete.gino.status()

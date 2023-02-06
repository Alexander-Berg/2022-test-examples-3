import pytest
from crm.agency_cabinet.agencies.server.src.handler import Handler
from crm.agency_cabinet.agencies.common import structs

pytest_plugins = ['crm.agency_cabinet.agencies.server.tests.plugin']


@pytest.fixture(scope='session')
async def handler():
    return Handler()


@pytest.fixture(scope='session')
async def all_agencies_struct():
    return [
        structs.AgencyInfo(agency_id=1,
                           name='test',
                           phone='+71111111111',
                           email='test@test.test',
                           site='www.test.test',
                           actual_address='test',
                           legal_address='test'),
        structs.AgencyInfo(agency_id=2,
                           name='test2',
                           phone='+71111111112',
                           email='test2@test.test',
                           site='www.test2.test',
                           actual_address='test2',
                           legal_address='test2'),
        structs.AgencyInfo(agency_id=3,
                           name='test3',
                           phone='+71111111113',
                           email='test3@test.test',
                           site='www.test3.test',
                           actual_address='test3',
                           legal_address='test3')
    ]


@pytest.fixture(scope='session')
async def all_clients_struct():
    return [
        structs.ClientInfo(
            id=1,
            name='Билл Мюррей',
            login='billy',
        ),
        structs.ClientInfo(
            id=2,
            name='Оуэн Уилсон',
            login='wilsono',
        )
    ]

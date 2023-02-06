import pytest

from datetime import datetime, timezone

from crm.agency_cabinet.common.consts.contract import ContractType
from crm.agency_cabinet.rewards.server.src.db import models


@pytest.fixture(scope='package')
def base_contract_id():
    return 888887


@pytest.fixture(scope='package')
def contract_id():
    return 888888


@pytest.fixture(scope='package')
def contract_aggregator_id():
    return 999999


@pytest.fixture(scope='package')
async def contract(contract_id):
    yield await models.Contract.create(
        id=contract_id,
        eid='11111/11',
        agency_id=882154,
        payment_type='postpayment',
        type=ContractType.prof.value,
        inn='7736602705',
        finish_date=datetime(2022, 3, 1, tzinfo=timezone.utc),
        services=['direct', 'media', 'video', 'business'],
        person_id=370283,
    )

    # clean-up
    await models.Contract.delete.where(
        models.Contract.id == contract_id
    ).gino.status()


@pytest.fixture(scope='package')
async def base_contract(base_contract_id):
    yield await models.Contract.create(
        id=base_contract_id,
        eid='21112/11',
        agency_id=882153,
        payment_type='postpayment',
        type=ContractType.base.value,
        inn='7736602704',
        finish_date=datetime(2022, 3, 1, tzinfo=timezone.utc),
        services=['direct', 'media', 'video', 'business'],
        person_id=370283,
    )

    # clean-up
    await models.Contract.delete.where(
        models.Contract.id == base_contract_id
    ).gino.status()


@pytest.fixture(scope='package')
async def calculator_data_media(contract):
    data = await models.CalculatorData.create(
        contract_id=contract.id,
        service='media',
        version='2022',
        data={'months': [
            {
                'grades': None,
                'indexes': [
                    {'index_id': 'early_payment', 'revenue': 15451.06},
                    {'index_id': 'revenue', 'revenue': 15451.06}
                ],
                'period_from': '2022-01-01T00:00:00',
                'predict': True
            }
        ]}
    )

    yield data

    await data.delete()


@pytest.fixture(scope='package')
async def calculator_data_media_base(base_contract):
    data = await models.CalculatorData.create(
        contract_id=base_contract.id,
        service='media',
        version='2022',
        data={'months': [
            {
                'grades': None,
                'indexes': [
                    {'index_id': 'early_payment', 'revenue': 15451.06},
                    {'index_id': 'revenue', 'revenue': 15451.06}
                ],
                'period_from': '2022-01-01T00:00:00',
                'predict': True
            }
        ]}
    )

    yield data

    await data.delete()


@pytest.fixture(scope='package')
async def contract_aggregator(contract_aggregator_id):
    yield await models.Contract.create(
        id=contract_aggregator_id,
        eid='11111/12',
        agency_id=882155,
        payment_type='postpayment',
        type=ContractType.aggregator.value,
        inn='7736602705',
        finish_date=datetime(2022, 3, 1, tzinfo=timezone.utc),
        services=['direct', 'media', 'video'],
        person_id=370284,
    )

    # clean-up
    await models.Contract.delete.where(
        models.Contract.id == contract_aggregator_id
    ).gino.status()


@pytest.fixture(scope='package')
async def calculator_data_meta_aggregator(contract_aggregator):
    data = await models.CalculatorData.create(
        contract_id=contract_aggregator.id,
        service='aggregator',
        version='2022',
        data={'months': [
            {
                'grades': [
                    {
                        'domains_count': 1,
                        'grade_id': 'D',
                        'revenue_average': 100.0
                    },
                    {
                        'domains_count': 2,
                        'grade_id': 'C',
                        'revenue_average': 150100.0
                    },
                    {
                        'domains_count': 3,
                        'grade_id': 'B',
                        'revenue_average': 1000100.0
                    },
                    {
                        'domains_count': 4,
                        'grade_id': 'A',
                        'revenue_average': 5000100.0
                    }
                ],
                'indexes': [],
                'period_from': '2022-01-01T00:00:00',
                'predict': True
            }
        ]}
    )

    yield data

    await data.delete()

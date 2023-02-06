import pytest
import typing

from crm.agency_cabinet.common.consts import Services
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


@pytest.fixture(scope='module')
async def fixture_calculator_data(fixture_contracts: typing.List[models.Contract]):
    contract = fixture_contracts[2]

    rows = [
        {
            'contract_id': contract.id,
            'service': Services.direct.value,
            'data': 'stub',
            'version': '2021'
        },
    ]

    data = await models.CalculatorData.bulk_insert(rows)

    yield data

    await models.CalculatorData.delete.where(models.CalculatorData.id.in_([d.id for d in data])).gino.status()


@pytest.fixture(scope='module')
async def handler():
    return Handler()

import pytest
import typing
from crm.agency_cabinet.common.consts import Services
from crm.agency_cabinet.rewards.server.src.db import models


@pytest.fixture(scope='module')
async def fixture_calculator_data2(fixture_contracts: typing.List[models.Contract]):

    rows = [
        {
            'contract_id': fixture_contracts[3].id,
            'service': Services.direct.value,
            'data': 'stub',
            'version': '2021'
        },
        {
            'contract_id': fixture_contracts[4].id,
            'service': Services.media.value,
            'data': 'stub',
            'version': '2022'
        }
    ]

    data = await models.CalculatorData.bulk_insert(rows)
    yield data

    await models.CalculatorData.delete.where(models.CalculatorData.id.in_([d.id for d in data])).gino.status()

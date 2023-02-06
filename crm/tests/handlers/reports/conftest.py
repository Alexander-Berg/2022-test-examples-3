import pytest
import typing
from datetime import datetime

from crm.agency_cabinet.common.consts import ReportsTypes, ReportsStatuses, Services
from crm.agency_cabinet.rewards.server.src.db import models


@pytest.fixture(scope='module')
async def fixture_file():
    yield await models.S3MdsFile.create(bucket='test', name='test', display_name='test')


@pytest.fixture(scope='module')
async def fixture_reports(fixture_contracts: typing.List[models.Contract], fixture_file: models.S3MdsFile):
    rows = [
        {
            'name': 'Новый отчет',
            'contract_id': fixture_contracts[0].id,
            'type': ReportsTypes.month.value,
            'service': Services.direct.value,
            'created_at': datetime(2021, 3, 1),
            'period_from': datetime(2021, 1, 1),
            'period_to': datetime(2021, 2, 1),
            'status': ReportsStatuses.ready.value,
            'clients_ids': [1, 2],
            'file_id': fixture_file.id
        },
        {
            'name': 'Другой отчет',
            'contract_id': fixture_contracts[1].id,
            'type': ReportsTypes.custom.value,
            'service': Services.direct.value,
            'created_at': datetime(2021, 3, 1),
            'period_from': datetime(2021, 1, 5),
            'period_to': datetime(2021, 2, 15),
            'status': ReportsStatuses.in_progress.value,
            'clients_ids': [],
            'file_id': None
        },
        {
            'name': 'Отчет',
            'contract_id': fixture_contracts[2].id,
            'type': ReportsTypes.custom.value,
            'service': Services.direct.value,
            'created_at': datetime(2021, 3, 1),
            'period_from': datetime(2021, 1, 5),
            'period_to': datetime(2021, 2, 15),
            'status': ReportsStatuses.ready.value,
            'clients_ids': [],
            'file_id': None
        },
    ]
    yield await models.ReportMetaInfo.bulk_insert(rows)

    await models.ReportMetaInfo.delete.gino.status()

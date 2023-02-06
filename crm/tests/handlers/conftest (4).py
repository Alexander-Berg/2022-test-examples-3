import pytest
from datetime import datetime
from dateutil.relativedelta import relativedelta
from sqlalchemy import or_

from crm.agency_cabinet.common.consts.contract import PaymentType, ContractType
from crm.agency_cabinet.common.consts import Services, get_end_of_current_fin_year
from crm.agency_cabinet.rewards.server.src.db import models


@pytest.fixture(scope='module')
async def fixture_contracts():
    rows = [
        {
            'id': 1111,
            'eid': 'test4',
            'agency_id': 125,
            'payment_type': PaymentType.postpayment.value,
            'type': ContractType.prof.value,
            'inn': None,
            'services': [Services.direct.value, Services.zen.value],
            'finish_date': get_end_of_current_fin_year(datetime.now().date())
        },
        {
            'id': 2222,
            'eid': 'test5',
            'agency_id': 127,
            'payment_type': PaymentType.postpayment.value,
            'type': ContractType.prof.value,
            'inn': None,
            'services': [],
            'finish_date': get_end_of_current_fin_year(datetime.now().date())
        },
        {
            'id': 3333,
            'eid': 'test6',
            'agency_id': 123456,
            'payment_type': PaymentType.prepayment.value,
            'type': ContractType.prof.value,
            'inn': '123456789',
            'services': [Services.direct.value, Services.media.value, Services.video.value],
            'finish_date': get_end_of_current_fin_year(datetime.now().date())
        },
        {
            'id': 4444,
            'eid': 'test_4444',
            'agency_id': 44,
            'payment_type': PaymentType.prepayment.value,
            'type': ContractType.prof.value,
            'inn': '123456789',
            'services': [Services.direct.value, Services.business.value],
            'finish_date': get_end_of_current_fin_year(datetime.now().date())
        },
        {
            'id': 4445,
            'eid': 'test_4445',
            'agency_id': 44,
            'payment_type': PaymentType.prepayment.value,
            'type': ContractType.prof.value,
            'inn': '123456789',
            'services': [Services.direct.value, Services.media.value],
            'finish_date': get_end_of_current_fin_year(datetime.now().date())
        },
        {
            'id': 4446,
            'eid': 'test_4446',
            'agency_id': 44,
            'payment_type': PaymentType.prepayment.value,
            'type': ContractType.prof.value,
            'inn': '123456789',
            'services': [Services.direct.value, Services.media.value],
            'finish_date': get_end_of_current_fin_year(datetime.now().date() - relativedelta(years=1))
        },
        {
            'id': 4447,
            'eid': 'test_4447',
            'agency_id': 47,
            'payment_type': PaymentType.prepayment.value,
            'type': ContractType.prof.value,
            'inn': '123456789',
            'services': [],
            'finish_date': get_end_of_current_fin_year(datetime.now().date())
        },
        {
            'id': 4448,
            'eid': 'test_4448',
            'agency_id': 48,
            'payment_type': PaymentType.prepayment.value,
            'type': ContractType.prof.value,
            'inn': '123456789',
            'services': [Services.direct.value, Services.media.value],
            'finish_date': get_end_of_current_fin_year(datetime.now().date() - relativedelta(years=1))
        },
    ]

    yield await models.Contract.bulk_insert(rows)

    await models.Contract.delete.where(
        or_(
            models.Contract.id == 1111,
            models.Contract.id == 2222,
            models.Contract.id == 3333,
            models.Contract.id == 4444,
            models.Contract.id == 4445,
            models.Contract.id == 4446,
            models.Contract.id == 4447,
            models.Contract.id == 4448,
        )
    ).gino.status()

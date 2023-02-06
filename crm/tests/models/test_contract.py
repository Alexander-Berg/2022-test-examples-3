# -*- coding: utf-8 -*-
import typing

import pytest
from sqlalchemy.engine.result import RowProxy

from crm.agency_cabinet.common.consts.contract import ContractType
from crm.agency_cabinet.rewards.server.src.db.models import Contract


@pytest.fixture
async def fixture_contract():
    yield await Contract.create(
        eid='test',
        agency_id=123,
        payment_type='post',
        type=ContractType.prof.value,
        is_crisis=True,
    )

    await Contract.delete.gino.status()


@pytest.fixture
async def fixture_contracts():
    rows = [
        {
            'eid': 'test9',
            'agency_id': 123321,
            'payment_type': 'post',
            'type': ContractType.prof.value,
            'is_crisis': True,
        },
        {
            'eid': 'test10',
            'agency_id': 123321,
            'payment_type': 'post',
            'type': ContractType.prof.value,
            'is_crisis': True,
        },
        {
            'eid': 'test11',
            'agency_id': 124421,
            'payment_type': 'post',
            'type': ContractType.prof.value,
            'is_crisis': False,
        },
    ]
    mdls = await Contract.bulk_insert(rows)
    yield mdls

    await Contract.delete.where(Contract.id.in_([r.id for r in mdls])).gino.status()


async def test_create_person(fixture_contract: Contract) -> None:
    assert fixture_contract.eid == 'test'


async def test_create_bulk_insert(fixture_contracts: typing.List[RowProxy]):
    for fixture_contract in fixture_contracts:
        assert fixture_contract.eid in ['test9', 'test10', 'test11']

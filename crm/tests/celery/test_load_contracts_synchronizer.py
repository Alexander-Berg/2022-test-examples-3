from sqlalchemy import and_

from crm.agency_cabinet.documents.server.src.db import models
from crm.agency_cabinet.documents.server.src.celery.tasks.load_contracts.synchronizer import ContractsSynchronizer


async def test_load_contracts_synchronizer():
    contract_id = 1
    agency_id = 1
    eid = 'test_contract_1'
    inn = '12345678'
    agreement_ids= (1, 2, 3)
    collaterals = {
        '0': {
            'id': agreement_ids[0],
            'collateral_type_id': ContractsSynchronizer.COLLATERAL_TYPE_ID_SERVICES,
            'dt': '2022-01-01T00:00:00',
            'is_faxed': '2022-01-01T00:00:00',
            'sent_dt': '2022-01-01T00:00:00',
            'finish_dt': '2022-01-01T00:00:00',
            'is_signed': '2022-01-01T00:00:00',
            'payment_type': 2,
            'num': '1',
        },
        '1': {
            'id': agreement_ids[1],
            'collateral_type_id': ContractsSynchronizer.COLLATERAL_TYPE_ID_PROLONGATION,
            'dt': '2022-01-02T00:00:00',
            'is_faxed': '2022-01-02T00:00:00',
            'sent_dt': '2022-01-02T00:00:00',
            'finish_dt': '2022-01-02T00:00:00',
            'is_signed': '2022-01-02T00:00:00',
            'payment_type': 3,
            'num': 'Ф-2',
        },
        '2': {
            'id': agreement_ids[2],
            'collateral_type_id': ContractsSynchronizer.COLLATERAL_TYPE_ID_PROLONGATION,
            'dt': '2022-01-03T00:00:00',
            'is_faxed': '2022-01-03T00:00:00',
            'sent_dt': '2022-01-03T00:00:00',
            'finish_dt': '2022-01-03T00:00:00',
            'is_signed': '2022-01-03T00:00:00',
            'payment_type': 3,
            'num': '3',
        },
    }

    await ContractsSynchronizer().process_data(
        [
            (contract_id, agency_id, eid, collaterals, inn),
        ]
    )

    contract = await models.Contract.query.where(
        models.Contract.id == contract_id
    ).gino.first()
    assert contract is not None
    assert contract.agency_id == agency_id
    assert contract.eid == eid
    assert contract.inn == inn

    agreement0 = await models.Agreement.query.where(
        and_(
            models.Agreement.id == agreement_ids[0],
            models.Agreement.contract_id == contract_id,
        )
    ).gino.first()
    assert agreement0 is None  # первый агримент не должен быть сохранен

    agreement1 = await models.Agreement.query.where(
        and_(
            models.Agreement.id == agreement_ids[1],
            models.Agreement.contract_id == contract_id,
        )
    ).gino.first()
    assert agreement1 is None  # фиктивный агримент не должен быть сохранен

    agreement2 = await models.Agreement.query.where(
        and_(
            models.Agreement.id == agreement_ids[2],
            models.Agreement.contract_id == contract_id,
        )
    ).gino.first()
    assert agreement2 is not None
    assert agreement2.name == ContractsSynchronizer.COLLATERAL_TYPE_TO_NAME_MAP[ContractsSynchronizer.COLLATERAL_TYPE_ID_PROLONGATION]
    assert agreement2.got_scan is True
    assert agreement2.got_original is True

    await contract.delete()  # с контрактом удалятся и агрименты

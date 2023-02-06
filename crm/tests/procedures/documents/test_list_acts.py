from decimal import Decimal

import pytest
from crm.agency_cabinet.documents.common.structs import Act
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.documents.acts import ListActs
from crm.agency_cabinet.grants.common.structs import AccessLevel
from smb.common.testing_utils import dt


@pytest.fixture
def procedure(service_discovery):
    return ListActs(service_discovery)


@pytest.fixture
def input_params():
    return dict(
        agency_id=22,
        invoice_id=1,
        contract_id=1,
        limit=10,
        offset=10,
        date_to='2022-09-22T10:10:10.000Z',
        date_from='2022-02-22T10:10:10.000Z',
        search_query=None,
    )


async def test_returns_act_list_if_access_allowed(
    procedure, input_params, service_discovery
):
    acts = [
        Act(
            act_id=1,
            invoice_id=1,
            contract_id=1,
            currency='RUR',
            amount=Decimal('66.6'),
            date=dt('2022-3-1 00:00:00'),
            eid='test_eid',
            contract_eid='test_contract_eid',
            invoice_eid='test_invoice_eid',
        ),
        Act(
            act_id=2,
            invoice_id=1,
            contract_id=1,
            currency='RUR',
            amount=Decimal('77.7'),
            date=dt('2022-8-1 00:00:00'),
            eid='test_eid',
            contract_eid='test_contract_eid',
            invoice_eid='test_invoice_eid',
        )
    ]

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.documents.list_acts.return_value = acts

    got = await procedure(yandex_uid=123, **input_params)

    assert got == acts


async def test_calls_other_services_for_info(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.documents.list_acts.assert_called_with(
        **input_params
    )


async def test_raises_if_access_denied(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)


async def test_does_not_call_documents_if_access_denied(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)

    service_discovery.documents.list_acts.assert_not_called()

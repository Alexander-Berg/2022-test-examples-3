import pytest
from crm.agency_cabinet.common.consts import PaymentType, Services
from crm.agency_cabinet.documents.common.structs import Contract, ContractStatus
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.documents.contracts import ListContracts
from crm.agency_cabinet.grants.common.structs import AccessLevel
from smb.common.testing_utils import dt


@pytest.fixture
def procedure(service_discovery):
    return ListContracts(service_discovery)


@pytest.fixture
def input_params():
    return dict(agency_id=22)


async def test_returns_contract_list_if_access_allowed(
    procedure, input_params, service_discovery
):
    contracts = [
        Contract(
            contract_id=1,
            eid="1234/56",
            status=ContractStatus.valid,
            payment_type=PaymentType.prepayment.value,
            services=[Services.zen.value, Services.video.value],
            signing_date=dt('2021-3-1 00:00:00'),
            finish_date=dt('2022-3-1 00:00:00'),
        ),
        Contract(
            contract_id=2,
            eid="1234/89",
            status=ContractStatus.valid,
            payment_type=PaymentType.prepayment.value,
            services=[Services.sprav.value, Services.business.value],
            signing_date=dt('2021-5-1 00:00:00'),
            finish_date=dt('2022-5-1 00:00:00'),
        ),
    ]

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.documents.list_contracts.return_value = contracts

    got = await procedure(yandex_uid=123, **input_params)

    assert got == contracts


async def test_calls_other_services_for_info(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.documents.list_contracts.assert_called_with(
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

    service_discovery.documents.list_contracts.assert_not_called()

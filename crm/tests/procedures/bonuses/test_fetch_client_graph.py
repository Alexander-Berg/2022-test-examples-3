from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.client import ClientNotFound
from crm.agency_cabinet.client_bonuses.common.structs import (
    ClientGraph,
    GraphPoint,
    ProgramBonusesGraph,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound
from crm.agency_cabinet.gateway.server.src.procedures.bonuses import (
    FetchClientBonusesGraph,
)
from crm.agency_cabinet.grants.common.structs import AccessLevel


@pytest.fixture
def procedure(service_discovery):
    return FetchClientBonusesGraph(service_discovery)


async def test_returns_list_clients_if_access_allowed(procedure, service_discovery):
    client_graph = ClientGraph(
        bonuses_available=Decimal(1000),
        overall_spent=[
            GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("100.500")),
            GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("200")),
        ],
        overall_accrued=[
            GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("100.500")),
            GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("200")),
        ],
        programs=[
            ProgramBonusesGraph(
                program_id=1,
                historical_monthly_data=[
                    GraphPoint(
                        point=dt("2021-10-01 00:00:00"), value=Decimal("100.500")
                    ),
                    GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("200")),
                ],
            )
        ],
    )

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.fetch_client_graph.return_value = client_graph

    got = await procedure(yandex_uid=42, agency_id=22, client_id=42)

    assert got == client_graph


async def test_calls_other_services_for_info(procedure, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, agency_id=22, client_id=42)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.client_bonuses.fetch_client_graph.assert_called_with(
        agency_id=22, client_id=42
    )


async def test_raises_if_access_denied(procedure, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, agency_id=22, client_id=42)


async def test_does_not_call_clients_bonuses_if_access_denied(
    procedure, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, agency_id=22, client_id=42)

    service_discovery.client_bonuses.fetch_client_graph.assert_not_called()


async def test_raises_if_client_not_found(procedure, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.fetch_client_graph.side_effect = ClientNotFound(
        agency_id=22, client_id=42
    )

    with pytest.raises(NotFound):
        await procedure(yandex_uid=123, agency_id=22, client_id=42)

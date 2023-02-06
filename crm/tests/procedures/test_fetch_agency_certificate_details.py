from decimal import Decimal

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificateDetails,
    AgencyCertificateDetailsRequest,
    DirectBonusPoint,
    DirectCertificationCondition,
    DirectCertificationScores,
    DirectKPI,
)
from crm.agency_cabinet.certificates.server.lib.exceptions import (
    AgencyCertificateNotFound,
)
from crm.agency_cabinet.certificates.server.lib.procedures import (
    FetchAgencyCertificateDetails,
)
from crm.agency_cabinet.certificates.server.tests.factory import Factory
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture()
def procedure(db):
    return FetchAgencyCertificateDetails(db)


@pytest.fixture
async def certificate(factory: Factory):
    cert = await factory.create_agency_certificate(
        id_=4321,
        agency_id=1234,
        project="Метрика",
        expiration_time=dt("2020-06-07 00:00:00"),
    )
    return cert["id"], cert["agency_id"]


@pytest.fixture
async def kpis(certificate, factory: Factory):
    await factory.create_direct_kpi(
        agency_id=certificate[1],
        name="Количество чего-то где-то",
        max_value=Decimal("2.0"),
        value=Decimal("1.5"),
        group="Поиск (РСЯ)",
    )
    await factory.create_direct_kpi(
        agency_id=certificate[1],
        name="Количество чего-то другого где-то еще",
        max_value=Decimal("3.0"),
        value=Decimal("0.5"),
        group="Поиск (РСЯ)",
    )


@pytest.fixture
async def bonuses(certificate, factory: Factory):
    await factory.create_direct_bonus_point(
        agency_id=certificate[1],
        name="Количество чего-то где-то",
        threshold="2.0",
        value="3.5",
        is_met=True,
        score=Decimal("2.5"),
    )
    await factory.create_direct_bonus_point(
        agency_id=certificate[1],
        name="Количество чего-то другого где-то еще",
        threshold="2.0",
        value="3.5",
        is_met=True,
        score=Decimal("2.5"),
    )


@pytest.fixture
async def conditions(certificate, factory: Factory):
    await factory.create_certificate_condition(
        agency_id=certificate[1],
        name="Договор с Яндексом",
        threshold="-",
        value="присутствует",
        is_met=True,
    )
    await factory.create_certificate_condition(
        agency_id=certificate[1],
        name="Количество активных клиентов",
        threshold="10",
        value="8",
        is_met=False,
    )


@pytest.fixture
async def scores(certificate, factory: Factory):
    await factory.create_prolongation_score(
        agency_id=certificate[1],
        project="direct",
        target_score=Decimal("6.0"),
        current_score=Decimal("5.0"),
        score_group="general",
    )
    await factory.create_prolongation_score(
        agency_id=certificate[1],
        project="direct",
        target_score=Decimal("5.0"),
        current_score=Decimal("6.0"),
        score_group="rsya",
    )


async def test_raises_if_no_certification_data_found_with_existing_certificate(
    procedure, certificate
):
    with pytest.raises(AgencyCertificateNotFound) as err:
        await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert err.value == AgencyCertificateNotFound(agency_id=1234)


async def test_raises_if_wrong_agency_id(procedure, certificate):
    with pytest.raises(AgencyCertificateNotFound) as err:
        await procedure(AgencyCertificateDetailsRequest(agency_id=9999))

    assert err.value == AgencyCertificateNotFound(agency_id=9999)


async def test_returns_full_certificate_data_correctly(
    procedure,
    certificate,
    kpis,
    bonuses,
    conditions,
    scores,
):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result == AgencyCertificateDetails(
        agency_id=1234,
        conditions=[
            DirectCertificationCondition(
                name="Договор с Яндексом",
                threshold="-",
                value="присутствует",
                is_met=True,
            ),
            DirectCertificationCondition(
                name="Количество активных клиентов",
                threshold="10",
                value="8",
                is_met=False,
            ),
        ],
        kpis=[
            DirectKPI(
                name="Количество чего-то где-то",
                max_value=Decimal("2.0"),
                value=Decimal("1.5"),
                group="Поиск (РСЯ)",
            ),
            DirectKPI(
                name="Количество чего-то другого где-то еще",
                max_value=Decimal("3.0"),
                value=Decimal("0.5"),
                group="Поиск (РСЯ)",
            ),
        ],
        bonus_points=[
            DirectBonusPoint(
                name="Количество чего-то где-то",
                threshold="2.0",
                value="3.5",
                is_met=True,
                score=Decimal("2.5"),
            ),
            DirectBonusPoint(
                name="Количество чего-то другого где-то еще",
                threshold="2.0",
                value="3.5",
                is_met=True,
                score=Decimal("2.5"),
            ),
        ],
        scores=[
            DirectCertificationScores(
                score_group="general",
                value=Decimal("5.0"),
                threshold=Decimal("6.0"),
                is_met=False,
            ),
            DirectCertificationScores(
                score_group="rsya",
                value=Decimal("6.0"),
                threshold=Decimal("5.0"),
                is_met=True,
            ),
        ],
    )


async def test_returns_kpi_data_as_expected_if_has_kpis(procedure, certificate, kpis):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.kpis == [
        DirectKPI(
            name="Количество чего-то где-то",
            max_value=Decimal("2.0"),
            value=Decimal("1.5"),
            group="Поиск (РСЯ)",
        ),
        DirectKPI(
            name="Количество чего-то другого где-то еще",
            max_value=Decimal("3.0"),
            value=Decimal("0.5"),
            group="Поиск (РСЯ)",
        ),
    ]


async def test_returns_bonuses_data_as_expected_if_has_bonuses(
    procedure, certificate, bonuses
):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.bonus_points == [
        DirectBonusPoint(
            name="Количество чего-то где-то",
            threshold="2.0",
            value="3.5",
            is_met=True,
            score=Decimal("2.5"),
        ),
        DirectBonusPoint(
            name="Количество чего-то другого где-то еще",
            threshold="2.0",
            value="3.5",
            is_met=True,
            score=Decimal("2.5"),
        ),
    ]


async def test_returns_conditions_data_as_expected_if_has_conditions(
    procedure, certificate, conditions
):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.conditions == [
        DirectCertificationCondition(
            name="Договор с Яндексом",
            threshold="-",
            value="присутствует",
            is_met=True,
        ),
        DirectCertificationCondition(
            name="Количество активных клиентов",
            threshold="10",
            value="8",
            is_met=False,
        ),
    ]


async def test_returns_scores_data_as_expected_if_has_scores(
    procedure, certificate, scores
):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.scores == [
        DirectCertificationScores(
            score_group="general",
            value=Decimal("5.0"),
            threshold=Decimal("6.0"),
            is_met=False,
        ),
        DirectCertificationScores(
            score_group="rsya",
            value=Decimal("6.0"),
            threshold=Decimal("5.0"),
            is_met=True,
        ),
    ]


async def test_returns_no_bonuses_data_as_expected_if_has_no_bonuses(
    procedure,
    certificate,
    kpis,
    conditions,
    scores,
):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.bonus_points == []


async def test_returns_no_kpis_data_as_expected_if_has_no_kpis(
    procedure,
    certificate,
    bonuses,
    conditions,
    scores,
):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.kpis == []


async def test_returns_no_conditions_data_as_expected_if_has_no_conditions(
    procedure, certificate, kpis, bonuses, scores
):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.conditions == []


async def test_returns_no_scores_data_as_expected_if_has_no_scores(
    procedure, certificate, kpis, bonuses, conditions
):
    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.scores == []


async def test_returns_no_scores_data_if_scores_are_given_not_for_direct(
    procedure, certificate, kpis, bonuses, conditions, factory
):
    await factory.create_prolongation_score(
        agency_id=certificate[1],
        project="media",
        target_score=Decimal("6.0"),
        current_score=Decimal("5.0"),
        score_group="general",
    )

    result = await procedure(AgencyCertificateDetailsRequest(agency_id=1234))

    assert result.scores == []

from decimal import Decimal

import pytest

from crm.agency_cabinet.certificates.common.structs import AgencyCertificate
from crm.agency_cabinet.certificates.server.lib.procedures import ListAgencyCertificates
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def procedure(db):
    return ListAgencyCertificates(db)


async def test_returns_agency_actual_certificates(procedure, factory):
    await factory.create_agency_certificate(
        id_=111,
        external_id="external_111",
        project="Метрика",
        expiration_time=dt("2019-10-20 00:00:00"),
    )
    await factory.create_prolongation_score(
        project="Метрика",
        current_score=Decimal("2.5"),
        target_score=Decimal("5.0"),
        score_group="general",
    )
    await factory.create_agency_certificate(
        id_=222,
        external_id="external_222",
        project="Директ",
        expiration_time=dt("2020-11-20 00:00:00"),
    )
    await factory.create_prolongation_score(
        project="Директ",
        current_score=Decimal("5.0"),
        target_score=Decimal("5.0"),
        score_group="general",
    )

    result = await procedure(agency_id=22)

    assert result == [
        AgencyCertificate(
            id=222,
            project="Директ",
            expiration_time=dt("2020-11-20 00:00:00"),
            auto_renewal_is_met=True,
        ),
        AgencyCertificate(
            id=111,
            project="Метрика",
            expiration_time=dt("2019-10-20 00:00:00"),
            auto_renewal_is_met=False,
        ),
    ]


@pytest.mark.parametrize(
    "current_score",
    [
        "5.0",  # equal
        "6.0",  # grater
    ],
)
async def test_returns_true_as_auto_renewal_if_current_general_score_gt_target_score(
    current_score, procedure, factory
):
    await factory.create_agency_certificate(id_=222, project="Директ")
    await factory.create_prolongation_score(
        project="Директ",
        current_score=current_score,
        target_score=Decimal("5.0"),
        score_group="general",
    )

    result = await procedure(agency_id=22)

    assert len(result) == 1
    assert result[0].auto_renewal_is_met is True


async def test_returns_false_as_auto_renewal_if_current_general_score_lt_target_score(
    procedure, factory
):
    await factory.create_agency_certificate(id_=222, project="Директ")
    await factory.create_prolongation_score(
        project="Директ",
        current_score=Decimal("2.0"),
        target_score=Decimal("5.0"),
        score_group="general",
    )

    result = await procedure(agency_id=22)

    assert len(result) == 1
    assert result[0].auto_renewal_is_met is False


async def test_returns_false_as_auto_renewal_if_no_info_about_agency_score(
    procedure, factory
):
    await factory.create_agency_certificate(id_=222)

    result = await procedure(agency_id=22)

    assert len(result) == 1
    assert result[0].auto_renewal_is_met is False


async def test_returns_false_as_auto_renewal_if_no_info_about_general_scores_given(
    procedure, factory
):
    await factory.create_agency_certificate(id_=222, project="Метрика")
    await factory.create_prolongation_score(
        project="Директ",
        current_score=Decimal("6.0"),
        target_score=Decimal("5.0"),
        score_group="РСЯ",
    )

    result = await procedure(agency_id=22)

    assert len(result) == 1
    assert result[0].auto_renewal_is_met is False


@pytest.mark.parametrize(
    "general_score, rsya_score, expected_result",
    [
        (Decimal("6.0"), Decimal("4.0"), True),
        (Decimal("4.0"), Decimal("6.0"), False),
    ],
)
async def test_non_general_prolongation_scores_do_not_affect_autorenewal(
    general_score, rsya_score, expected_result, procedure, factory
):
    await factory.create_agency_certificate(id_=222, project="Директ")
    await factory.create_prolongation_score(
        project="Директ",
        current_score=general_score,
        target_score=Decimal("5.0"),
        score_group="general",
    )
    await factory.create_prolongation_score(
        project="Директ",
        current_score=rsya_score,
        target_score=Decimal("5.0"),
        score_group="РСЯ",
    )

    result = await procedure(agency_id=22)

    assert len(result) == 1
    assert result[0].auto_renewal_is_met is expected_result


async def test_returns_last_actual_certificate_for_project(procedure, factory):
    await factory.create_agency_certificate(
        id_=222, project="Директ", expiration_time=dt("2020-10-20 00:00:00")
    )
    await factory.create_agency_certificate(
        id_=333, project="Директ", expiration_time=dt("2020-11-20 00:00:00")
    )
    await factory.create_agency_certificate(
        id_=444, project="Метрика", expiration_time=dt("2020-10-10 00:00:00")
    )
    await factory.create_agency_certificate(
        id_=555, project="Метрика", expiration_time=dt("2020-11-10 00:00:00")
    )

    result = await procedure(agency_id=22)

    assert [r.id for r in result] == [333, 555]


async def test_does_not_return_other_agencies_certificates(procedure, factory):
    await factory.create_agency_certificate(id_=222, agency_id=333)

    result = await procedure(agency_id=22)

    assert result == []

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    CertifiedEmployee,
    CertifiedEmployees,
    EmployeeCertificate,
    EmployeeCertificateStatus,
    ListEmployeesCertificatesRequest,
    Pagination,
)
from crm.agency_cabinet.certificates.server.lib.procedures import (
    ListEmployeesCertificates,
)
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time(dt("2020-01-01 18:00:00"))]


@pytest.fixture
def procedure(db):
    return ListEmployeesCertificates(db=db)


async def test_returns_certificates_grouped_by_employee(factory, procedure):
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_dzen",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="Альпак Альпакыч",
        project="Дзен",
        start_time=dt("2020-01-06 18:00:00"),
        expiration_time=dt("2020-02-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=444,
        external_id="alpaca_direct",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="Альпак Альпакыч",
        project="Директ",
        start_time=dt("2020-09-06 18:00:00"),
        expiration_time=dt("2020-10-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert got == CertifiedEmployees(
        employees=[
            CertifiedEmployee(
                name="Альпак Альпакыч",
                email="alpaca@yandex.ru",
                agency_id=8765,
                certificates=[
                    EmployeeCertificate(
                        project="Дзен",
                        start_time=dt("2020-01-06 18:00:00"),
                        expiration_time=dt("2020-02-06 18:00:00"),
                        external_id="alpaca_dzen",
                        status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
                    ),
                    EmployeeCertificate(
                        project="Директ",
                        start_time=dt("2020-09-06 18:00:00"),
                        expiration_time=dt("2020-10-06 18:00:00"),
                        external_id="alpaca_direct",
                        status=EmployeeCertificateStatus.ACTIVE,
                    ),
                ],
            )
        ]
    )


@pytest.mark.parametrize("name", ["Альпак Альпакыч", None])
async def test_does_not_group_employees_with_different_emails(name, factory, procedure):
    await factory.create_employee_certificate(
        id_=222,
        external_id="capibara_direct",
        agency_id=8765,
        employee_email="capibara@yandex.ru",
        employee_name=name,
        project="Директ",
        start_time=dt("2020-07-06 18:00:00"),
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_dzen",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name=name,
        project="Дзен",
        start_time=dt("2020-01-06 18:00:00"),
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert got == CertifiedEmployees(
        employees=[
            CertifiedEmployee(
                name=name,
                email="alpaca@yandex.ru",
                agency_id=8765,
                certificates=[
                    EmployeeCertificate(
                        project="Дзен",
                        start_time=dt("2020-01-06 18:00:00"),
                        expiration_time=dt("2020-02-06 18:00:00"),
                        external_id="alpaca_dzen",
                        status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
                    ),
                ],
            ),
            CertifiedEmployee(
                name=name,
                email="capibara@yandex.ru",
                agency_id=8765,
                certificates=[
                    EmployeeCertificate(
                        project="Директ",
                        start_time=dt("2020-07-06 18:00:00"),
                        expiration_time=dt("2020-08-06 18:00:00"),
                        external_id="capibara_direct",
                        status=EmployeeCertificateStatus.ACTIVE,
                    ),
                ],
            ),
        ]
    )


@pytest.mark.parametrize(
    "expiration_time, expected_status",
    [
        (
            dt("2019-01-01 18:00:00"),
            EmployeeCertificateStatus.EXPIRED,
        ),  # сертификат просрочен
        (
            dt("2020-02-02 18:00:00"),
            EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
        ),  # сертификат действителен, но будет просрочен в течение полугода
        (
            dt("2021-01-01 18:00:00"),
            EmployeeCertificateStatus.ACTIVE,
        ),  # сертификат будет действителен более полугода
    ],
)
async def test_computes_correct_status(
    expiration_time, expected_status, factory, procedure
):
    await factory.create_employee_certificate(
        id_=1234, agency_id=8765, expiration_time=expiration_time
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert got.employees[0].certificates[0].status == expected_status


async def test_uses_last_added_employee_name(factory, procedure):
    await factory.create_employee_certificate(
        id_=222,
        external_id="capibara_direct",
        agency_id=8765,
        employee_name="Капибар Капибарыч",
        project="Директ",
    )
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_dzen",
        agency_id=8765,
        employee_name="Альпак Альпакыч",
        project="Дзен",
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert len(got.employees) == 1
    assert got.employees[0].name == "Альпак Альпакыч"


async def test_sorts_employees_by_name(factory, procedure):
    await factory.create_employee_certificate(
        id_=222,
        external_id="capibara_direct",
        agency_id=8765,
        employee_email="capibara@yandex.ru",
        employee_name="Капибар Капибарыч",
        project="Директ",
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=111,
        external_id="lama_metrica",
        agency_id=8765,
        employee_email="lama@yandex.ru",
        employee_name=None,
        project="Метрика",
        expiration_time=dt("2020-01-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=333,
        external_id="alpaca_dzen",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="Альпак Альпакыч",
        project="Дзен",
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert [employee.name for employee in got.employees] == [
        "Альпак Альпакыч",
        "Капибар Капибарыч",
        None,
    ]


async def test_sorts_certificates_for_employee_by_project(factory, procedure):
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_direct",
        agency_id=8765,
        project="Директ",
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=222,
        external_id="alpaca_metrica",
        agency_id=8765,
        project="Метрика",
        expiration_time=dt("2020-01-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=333,
        external_id="alpaca_dzen",
        agency_id=8765,
        project="Дзен",
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert [certificate.project for certificate in got.employees[0].certificates] == [
        "Дзен",
        "Директ",
        "Метрика",
    ]


async def test_returns_empty_list_if_nothing_found_for_agency(factory, procedure):
    await factory.create_employee_certificate(id_=222, agency_id=1111)

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert got == CertifiedEmployees(employees=[])


async def test_limit_offset_single_employee(factory, procedure):
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_direct",
        agency_id=8765,
        project="Директ",
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=222,
        external_id="alpaca_metrica",
        agency_id=8765,
        project="Метрика",
        expiration_time=dt("2020-01-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=333,
        external_id="alpaca_dzen",
        agency_id=8765,
        project="Дзен",
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=1, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert len(got.employees) == 1

    assert len(got.employees[0].certificates) == 3
    assert got.employees[0].certificates == [
        EmployeeCertificate(
            external_id="alpaca_dzen",
            project="Дзен",
            expiration_time=dt("2020-02-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        ),
        EmployeeCertificate(
            external_id="alpaca_direct",
            project="Директ",
            expiration_time=dt("2020-08-06 18:00:00"),
            status=EmployeeCertificateStatus.ACTIVE,
            start_time=dt("2020-06-28 18:00:00"),
        ),
        EmployeeCertificate(
            external_id="alpaca_metrica",
            project="Метрика",
            expiration_time=dt("2020-01-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        ),
    ]

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=1, offset=1),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert len(got.employees) == 0


async def test_limit_offset_multiple_employees(factory, procedure):
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_direct",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="alpaca",
        project="Директ",
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=222,
        external_id="alpaca_metrica",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="alpaca",
        project="Метрика",
        expiration_time=dt("2020-01-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=333,
        external_id="rivera_dzen",
        employee_email="rivera@yandex.ru",
        employee_name="rivera",
        agency_id=8765,
        project="Дзен",
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=1, offset=0),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert len(got.employees) == 1

    assert len(got.employees[0].certificates) == 2
    assert got.employees[0].certificates == [
        EmployeeCertificate(
            external_id="alpaca_direct",
            project="Директ",
            expiration_time=dt("2020-08-06 18:00:00"),
            status=EmployeeCertificateStatus.ACTIVE,
            start_time=dt("2020-06-28 18:00:00"),
        ),
        EmployeeCertificate(
            external_id="alpaca_metrica",
            project="Метрика",
            expiration_time=dt("2020-01-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        ),
    ]

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=1),
            project=None,
            search_query=None,
            status=None,
        )
    )

    assert len(got.employees) == 1

    assert len(got.employees[0].certificates) == 1
    assert got.employees[0].certificates == [
        EmployeeCertificate(
            external_id="rivera_dzen",
            project="Дзен",
            expiration_time=dt("2020-02-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        )
    ]


async def test_search_query(factory, procedure):
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_direct",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="Alpaca AGENCY",
        project="Директ",
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=222,
        external_id="alpaca_metrica",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="Alpaca AGENCY",
        project="Метрика",
        expiration_time=dt("2020-01-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=333,
        external_id="rivera_dzen",
        employee_email="rivera_agency@yandex.ru",
        employee_name="rivera",
        agency_id=8765,
        project="Дзен",
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query="agency",
            status=None,
        )
    )

    assert len(got.employees) == 2

    assert len(got.employees[0].certificates) == 2
    assert got.employees[0].certificates == [
        EmployeeCertificate(
            external_id="alpaca_direct",
            project="Директ",
            expiration_time=dt("2020-08-06 18:00:00"),
            status=EmployeeCertificateStatus.ACTIVE,
            start_time=dt("2020-06-28 18:00:00"),
        ),
        EmployeeCertificate(
            external_id="alpaca_metrica",
            project="Метрика",
            expiration_time=dt("2020-01-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        )
    ]

    assert len(got.employees[1].certificates) == 1
    assert got.employees[1].certificates == [
        EmployeeCertificate(
            external_id="rivera_dzen",
            project="Дзен",
            expiration_time=dt("2020-02-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        )
    ]


async def test_status(factory, procedure):
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_direct",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="alpaca",
        project="Директ",
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=222,
        external_id="alpaca_metrica",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="alpaca",
        project="Метрика",
        expiration_time=dt("2020-01-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=333,
        external_id="rivera_dzen",
        employee_email="rivera@yandex.ru",
        employee_name="rivera",
        agency_id=8765,
        project="Дзен",
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project=None,
            search_query=None,
            status="expires_in_semiyear",
        )
    )

    assert len(got.employees) == 2

    assert len(got.employees[0].certificates) == 1
    assert got.employees[0].certificates == [
        EmployeeCertificate(
            external_id="alpaca_metrica",
            project="Метрика",
            expiration_time=dt("2020-01-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        )
    ]

    assert len(got.employees[1].certificates) == 1
    assert got.employees[1].certificates == [
        EmployeeCertificate(
            external_id="rivera_dzen",
            project="Дзен",
            expiration_time=dt("2020-02-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        )
    ]


async def test_project(factory, procedure):
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_direct",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="alpaca",
        project="Директ",
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=222,
        external_id="alpaca_metrica",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="alpaca",
        project="Метрика",
        expiration_time=dt("2020-01-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=333,
        external_id="rivera_dzen",
        employee_email="rivera@yandex.ru",
        employee_name="rivera",
        agency_id=8765,
        project="Дзен",
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=100, offset=0),
            project="Метрика",
            search_query=None,
            status=None,
        )
    )

    assert len(got.employees) == 1

    assert len(got.employees[0].certificates) == 1
    assert got.employees[0].certificates == [
        EmployeeCertificate(
            external_id="alpaca_metrica",
            project="Метрика",
            expiration_time=dt("2020-01-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        )
    ]


async def test_all_filters_limit_offset(factory, procedure):
    await factory.create_employee_certificate(
        id_=111,
        external_id="alpaca_direct",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="Alpaca AGENCY",
        project="Директ",
        expiration_time=dt("2020-08-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=222,
        external_id="alpaca_metrica",
        agency_id=8765,
        employee_email="alpaca@yandex.ru",
        employee_name="Alpaca AGENCY",
        project="Метрика",
        expiration_time=dt("2020-01-06 18:00:00"),
    )
    await factory.create_employee_certificate(
        id_=333,
        external_id="rivera_direct",
        employee_email="rivera_agency@yandex.ru",
        employee_name="rivera",
        agency_id=8765,
        project="Директ",
        expiration_time=dt("2020-02-06 18:00:00"),
    )

    got = await procedure(
        ListEmployeesCertificatesRequest(
            agency_id=8765,
            pagination=Pagination(limit=1, offset=0),
            project="Директ",
            search_query="agency",
            status="expires_in_semiyear",
        )
    )

    assert len(got.employees) == 1

    assert len(got.employees[0].certificates) == 1
    assert got.employees[0].certificates == [
        EmployeeCertificate(
            external_id="rivera_direct",
            project="Директ",
            expiration_time=dt("2020-02-06 18:00:00"),
            status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
            start_time=dt("2020-06-28 18:00:00"),
        )
    ]

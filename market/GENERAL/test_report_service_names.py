import pytest
from market.pylibrary.nanny_service_names.report import report_nanny_services
from market.pylibrary.nanny_service_names.types import (
    Environment,
    ReportSubType,
    Datacenter,
)


def test_report_nanny_services_all():
    return '\n'.join(sorted(sn.name for sn in report_nanny_services()))


@pytest.mark.parametrize("env, report_sub_type, datacenter, is_snippet_report", [
    # change snippet
    (Environment.PRODUCTION, ReportSubType.MARKET, Datacenter.SAS, None),
    (Environment.PRODUCTION, ReportSubType.MARKET, Datacenter.SAS, True),
    (Environment.PRODUCTION, ReportSubType.MARKET, Datacenter.SAS, False),
    # change env
    (Environment.PRESTABLE, ReportSubType.MARKET, Datacenter.SAS, None),
    (Environment.TESTING, ReportSubType.MARKET, Datacenter.SAS, None),
    # change change sub type
    (Environment.PRODUCTION, ReportSubType.BLUE_MARKET, Datacenter.SAS, None),
    (Environment.PRODUCTION, ReportSubType.PARALLEL, Datacenter.SAS, None),
    # change change dc
    (Environment.PRODUCTION, ReportSubType.MARKET, Datacenter.IVA, None),
    (Environment.PRODUCTION, ReportSubType.TURBO, Datacenter.VLA, None),
    # only one variable fixed
    (Environment.PRODUCTION, None, None, None),
    (Environment.EXPERIMENTAL, None, None, None),
    (None, ReportSubType.MARKET, None, None),
    (None, None, Datacenter.SAS, None),
    (None, None, None, True),
])
def test_report_nanny_services_filter(env, report_sub_type, datacenter, is_snippet_report):
    nanny_services = '\n'.join(sorted(sn.name for sn in report_nanny_services(
        env,
        report_sub_type,
        datacenter,
        is_snippet_report,
    )))

    nanny_services_as_str = '\n'.join(report_nanny_services(
        env,
        report_sub_type,
        datacenter,
        is_snippet_report,
        as_str=True,
    ))

    assert nanny_services == nanny_services_as_str
    return nanny_services

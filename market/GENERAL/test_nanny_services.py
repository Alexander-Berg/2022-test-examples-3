import pytest
from hamcrest import assert_that, same_instance, equal_to, has_properties
from market.pylibrary.nanny_service_names.names import ReportServiceName
from market.pylibrary.nanny_service_names.types import (
    Environment,
    ReportSubType,
    Datacenter,
)


@pytest.mark.parametrize("name, env, report_sub_type, datacenter, is_snippet_report", [
    ('test_report_market_snippet_vla', Environment.TESTING, ReportSubType.MARKET, Datacenter.VLA, True),
    ('test_report_market_vla', Environment.TESTING, ReportSubType.MARKET, Datacenter.VLA, False),
    ('prep_report_api_iva', Environment.PRESTABLE, ReportSubType.API, Datacenter.IVA, False),
    ('unst_report_daas_iva', Environment.DEVELOPMENT, ReportSubType.DAAS, Datacenter.IVA, False),
    ('unst_report_meta_daas_vla', Environment.DEVELOPMENT, ReportSubType.META_DAAS, Datacenter.VLA, False),
    ('unst_report_meta_daas_sas', Environment.DEVELOPMENT, ReportSubType.META_DAAS, Datacenter.SAS, False),
    ('prod_report_shadow_sas', Environment.PRIEMKA, ReportSubType.SHADOW, Datacenter.SAS, False),
    ('prod_report_parallel_myt', Environment.PRODUCTION, ReportSubType.PARALLEL, Datacenter.MYT, False),
    ('prod_report_mbo_vla', Environment.PRODUCTION, ReportSubType.MBO, Datacenter.VLA, False),
    ('test_report_meta_market_man', Environment.TESTING, ReportSubType.META_MARKET, Datacenter.MAN, False),
    ('prod_report_meta_market_man', Environment.PRODUCTION, ReportSubType.META_MARKET, Datacenter.MAN, False),
    ('prod_report_meta_market_kraken_vla', Environment.PRODUCTION, ReportSubType.META_MARKET_KRAKEN, Datacenter.VLA, False),
    ('prod_report_meta_parallel_man', Environment.PRODUCTION, ReportSubType.META_PARALLEL, Datacenter.MAN, False),
    ('prod_report_meta_int_man', Environment.PRODUCTION, ReportSubType.META_INT, Datacenter.MAN, False),
    ('prod_report_api_exp1_man', Environment.EXPERIMENTAL, ReportSubType.API, Datacenter.MAN, False),
    ('prod_report_meta_api_exp1_man', Environment.EXPERIMENTAL, ReportSubType.META_API, Datacenter.MAN, False),
    ('test_report_fresh_base_vla', Environment.TESTING, ReportSubType.FRESH_BASE, Datacenter.VLA, False),
    ('test_report_fresh_base_sas', Environment.TESTING, ReportSubType.FRESH_BASE, Datacenter.SAS, False),
    ('prep_report_meta_api_man', Environment.PRESTABLE, ReportSubType.META_API, Datacenter.MAN, False),
    # blue != blue_market
    ('test_report_blue_sas', Environment.TESTING, ReportSubType.BLUE, Datacenter.SAS, False),
    ('prod_report_blue_market_sas', Environment.PRODUCTION, ReportSubType.BLUE_MARKET, Datacenter.SAS, False),
    # empty group name
    ('', None, None, None, False),
    # unknown values
    ('super_report_market_vla', None, ReportSubType.MARKET, Datacenter.VLA, False),
    ('prod_report_suprermarket_vla', Environment.PRODUCTION, None, Datacenter.VLA, False),
    ('prod_report_market_fol', Environment.PRODUCTION, ReportSubType.MARKET, None, False),
    # missing values
    ('prod', Environment.PRODUCTION, None, None, False),
    ('report', None, None, None, False),
    ('report_iva', None, None, Datacenter.IVA, False),
])
def test_nanny_service_name(name, env, report_sub_type, datacenter, is_snippet_report):
    service_name = ReportServiceName(name)
    assert_that(service_name, has_properties({
        'env': same_instance(env),
        'datacenter': same_instance(datacenter),
        'report_sub_type': same_instance(report_sub_type),
        'is_snippet_report': equal_to(is_snippet_report)
    }))

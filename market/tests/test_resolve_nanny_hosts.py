# coding: utf8


import pytest
from market.sre.tools.balancer_regenerate.lib.service.nanny import NannyService

MTN = 'MTN_ENABLED'
NO_MTN = 'NO_NETWORK_ISOLATION'
UNKNOWN = 'UNKNOWN'
TEST_CONTAINER_NAME = "sas2-1264-sas-market-prod-grade-m-c69-11875.gencfg-c.yandex.net"
TEST_HOSTNAME = "sas2-1264.search.yandex.net"
TEST_PORT = 11875


def get_test_service(network):
    """
    Тестовые данные на основе https://nanny.yandex-team.ru/v2/services/prod_grade_market_banner_sas/current_state/instances/
    """
    return {
        "engine": "",
        "network_settings": network,
        "container_hostname": TEST_CONTAINER_NAME,
        "hostname": TEST_HOSTNAME,
        "port": TEST_PORT,
        "itags": [
            "a_line_sas-2.3.2",
            "a_topology_cgset-memory.limit_in_bytes=1178599424",
            "a_geo_sas",
            "cgset_memory_recharge_on_pgfault_1",
            "a_topology_version-stable-110-r45",
            "a_topology_stable-110-r45",
            "a_itype_marketgrademarketbanner",
            "SAS_MARKET_PROD_GRADE_MARKET_BANNER",
            "a_tier_none",
            "a_metaprj_market",
            "a_ctype_production",
            "a_topology_cgset-memory.low_limit_in_bytes=1073741824",
            "a_dc_sas",
            "a_topology_group-SAS_MARKET_PROD_GRADE_MARKET_BANNER",
            "a_prj_market",
            "use_hq_spec",
            "enable_hq_report",
            "enable_hq_poll"
        ]
    }


def test_resolve_mtn_service():
    """ К контейнеру с MTN нужно обращаться по имени контейнера """
    service = NannyService('test', 'iva')
    assert service._get_nanny_service_instances(
        json={"result": [get_test_service(MTN)]}
    ) == [{
        'name': TEST_CONTAINER_NAME,
        'port': TEST_PORT,
    }]


def test_resolve_non_mtn_service():
    """ К контейнеру без MTN нужно обращаться по имени железного хоста """
    service = NannyService('test', 'iva')
    assert service._get_nanny_service_instances(
        json={"result": [get_test_service(NO_MTN)]}
    ) == [{
        'name': TEST_HOSTNAME,
        'port': TEST_PORT,
    }]


def test_resolve_unknown_mtn_service():
    """ Для контейнера с неизвестным состоянием MTN рейзится RuntimeError """
    with pytest.raises(RuntimeError):
        service = NannyService('test', 'iva')
        service._get_nanny_service_instances(json={"result": [get_test_service(UNKNOWN)]})

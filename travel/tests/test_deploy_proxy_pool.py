from mock import patch

from travel.avia.library.python.proxy_pool.proxy_pool import ProxyHost
from travel.avia.library.python.proxy_pool.deploy_proxy_pool import DeployProxyPool


@patch('travel.library.python.yp.endpoints.YpEndpoints.get_ip6_addresses_by_dc')
def test_get_proxies(mock_get_ip6_addresses_by_dc):
    mock_get_ip6_addresses_by_dc.return_value = {
        'sas': ['2606:2800:220:1:248:1893:25c8:1946']
    }

    pool = DeployProxyPool('development', 'sas', 'login', 'password')
    host = pool.get_proxy()

    assert isinstance(host, ProxyHost)
    assert host._host == '2606:2800:220:1:248:1893:25c8:1946'


@patch('travel.library.python.yp.endpoints.YpEndpoints.get_ip6_addresses_by_dc')
def test_get_proxies_fallback(mock_get_ip6_addresses_by_dc):
    mock_get_ip6_addresses_by_dc.return_value = {
        'sas': [],
        'iva': ['2606:2800:220:1:248:1893:iva:1946'],
        'vla': ['2606:2800:220:1:248:1893:vla:1946'],
    }

    pool = DeployProxyPool('development', 'sas', 'login', 'password')
    host = pool.get_proxy()

    assert isinstance(host, ProxyHost)
    assert host._host == '2606:2800:220:1:248:1893:vla:1946'

import pytest
from mock import patch

from travel.avia.library.python.proxy_pool.proxy_pool import ProxyPool
from travel.avia.library.python.proxy_pool.tests.utils import check_proxy_host

ADDRESSES = [
    '2606:2800:220:1:248:1893:25c8:1946',
    '2606:2800:220:1:248:1893:25c8:1946',
]


@pytest.mark.parametrize('login, password', [
    (None, None),
    ('login', None),
    (None, 'password'),
    ('login', 'password'),
])
@patch('socket.getaddrinfo')
def test_proxy_pool(mock_getaddrinfo, login, password):
    mock_getaddrinfo.return_value = [
        (10, 1, 6, '', (ADDRESSES[0], 80, 0, 0)),
        (10, 2, 17, '', (ADDRESSES[1], 80, 0, 0)),
    ]
    pool = ProxyPool(login, password, proxies=ADDRESSES)

    check_proxy_host(pool.get_proxy(), ADDRESSES[0], login, password)
    check_proxy_host(pool.get_proxy(), ADDRESSES[0], login, password)

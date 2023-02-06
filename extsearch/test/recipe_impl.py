import urllib

import pytest
import yatest.common

from extsearch.geo.kernel.pymod.runserver.manage import WarmupError
from extsearch.geo.kernel.pymod.runserver.recipe import RunServerRecipe


def app():
    return yatest.common.binary_path('extsearch/geo/kernel/pymod/runserver/test/app/app')


def ping(sockaddr):
    url = sockaddr.get_url('/ping')
    assert urllib.urlopen(url).read() == 'pong'


def test_ipv4():
    r = RunServerRecipe('app_ipv4', [app(), '--port', 0])
    r.start()
    ping(r.sockaddr)
    r.stop()


def test_ipv6():
    r = RunServerRecipe('app_ipv6', [app(), '--port', 0, '--ipv6'])
    r.start()
    ping(r.sockaddr)
    r.stop()


def test_ipv4_shutdown():
    r = RunServerRecipe(
        'app_ipv4_shutdown',
        [app(), '--port', 0],
        ping_path='/ping',
        shutdown_path='/admin?action=shutdown',
    )
    r.start()
    ping(r.sockaddr)
    r.stop()


def test_ipv6_shutdown():
    r = RunServerRecipe(
        'app_ipv6_shutdown',
        [app(), '--port', 0, '--ipv6'],
        ping_path='/ping',
        shutdown_path='/admin?action=shutdown',
    )
    r.start()
    ping(r.sockaddr)
    r.stop()


def test_crash():
    r = RunServerRecipe('app_crash', [app(), '--port', 0, '--crash'])
    with pytest.raises(WarmupError) as excinfo:
        r.start()
    assert 'died on startup' in str(excinfo.value)
    r.stop()

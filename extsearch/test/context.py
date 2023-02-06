import pytest
import urllib

import yatest.common
from yatest.common import network

from extsearch.geo.kernel.pymod.runserver.manage import WarmupError
from extsearch.geo.kernel.pymod.runserver.service import Service


def app():
    return yatest.common.binary_path('extsearch/geo/kernel/pymod/runserver/test/app/app')


def test_success():
    with Service([app(), '--port', '0']) as service:
        sockaddr = service.warm_up('yandsearch')
        url = sockaddr.get_url('/ping')
        assert urllib.urlopen(url).read() == 'pong'

    with network.PortManager() as pm:
        port = pm.get_port()
        with Service([app(), '--port', str(port)]) as service:
            sockaddr = service.warm_up('yandsearch', port=port)
            url = sockaddr.get_url('/ping')
            assert urllib.urlopen(url).read() == 'pong'


def test_crash():
    with network.PortManager() as pm:
        port = pm.get_port()
        with pytest.raises(yatest.common.ExecutionError):
            with Service([app(), '--port', str(port), '--crash']):
                pass

        with pytest.raises(WarmupError):
            with Service([app(), '--port', str(port), '--crash']) as service:
                service.warm_up('yandsearch')


def test_bad_shutdown():
    with pytest.raises(yatest.common.ExecutionError):
        with Service([app(), '--port', '0', '--ignore-shutdown']) as service:
            service.warm_up('yandsearch')

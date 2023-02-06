import os
import pytest

import yatest.common
from yatest.common import network

from extsearch.geo.kernel.pymod.runserver.service import Service
from extsearch.geo.meta.tests.env import common as envcommon


@pytest.fixture(scope='session')
def metasearches():
    geometasearch_v2 = yatest.common.binary_path('extsearch/geo/meta/v2/daemon/geometasearch_v2')

    os.symlink(yatest.common.binary_path('extsearch/geo/meta/tests/env/stable/configs/geohosts.json'), 'geohosts.json')
    os.symlink(yatest.common.binary_path('extsearch/geo/meta/tests/env/stable/configs/controls'), 'controls')

    with network.PortManager() as pm:
        port = pm.get_port(8031)

        cmdline = envcommon.set_up_v2(
            geometasearch_v2,
            port,
            [
                yatest.common.binary_path('extsearch/geo/meta/tests/env/stable/configs/upper.cfg'),
            ],
        )

        with Service(cmdline) as service:
            sockaddr = service.warm_up('ping', port=port)
            yield envcommon.Metasearches(middle=None, upper=sockaddr.get_url(), log_paths=None)

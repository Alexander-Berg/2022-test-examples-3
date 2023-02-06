import os
import pytest

import yatest.common
from yatest.common import network

from extsearch.geo.kernel.pymod.runserver.service import Service
from extsearch.geo.meta.tests.env import common as envcommon


@pytest.fixture(scope='session')
def metasearches():
    geometasearch = yatest.common.binary_path('extsearch/geo/meta/daemon/geometasearch')

    envcommon.create_rearrange_data()
    os.symlink(yatest.common.binary_path('extsearch/geo/meta/tests/env/stable/configs/geohosts.json'), 'geohosts.json')
    os.symlink(yatest.common.binary_path('extsearch/geo/meta/tests/env/stable/configs/controls'), 'controls')

    with network.PortManager() as pm:
        upper_port = pm.get_port_range(8031, 5)
        middle_port = upper_port + 1

        middle_cmdline = envcommon.set_up_middle(
            geometasearch,
            middle_port,
            [
                '-V',
                'LockRearrangeData=no',
                yatest.common.binary_path('extsearch/geo/meta/tests/env/stable/configs/middle.cfg'),
            ],
        )

        upper_cmdline = envcommon.set_up_upper(
            geometasearch,
            upper_port,
            [
                '-V',
                'MiddlePort={}'.format(middle_port),
                '-V',
                'LockRearrangeData=no',
                yatest.common.binary_path('extsearch/geo/meta/tests/env/stable/configs/upper.cfg'),
            ],
        )

        # Metasearch is running in the cloud, no logs are available
        log_paths = None

        with Service(middle_cmdline) as middle_service:
            middle_sockaddr = middle_service.warm_up('yandsearch?info=getconfig', port=middle_port)
            with Service(upper_cmdline) as upper_service:
                upper_sockaddr = upper_service.warm_up('yandsearch?info=getconfig', port=upper_port)
                yield envcommon.Metasearches(middle_sockaddr.get_url(), upper_sockaddr.get_url(), log_paths)

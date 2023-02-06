import os
import pytest

import yatest.common
from yatest.common import network

from extsearch.geo.kernel.pymod.runserver.service import Service
from extsearch.geo.meta.tests.env import common as envcommon


def _get_tvmapi_port():
    with open('tvmapi.port') as f:
        return int(f.read())


@pytest.fixture(scope='session')
def metasearches():
    geometasearch = yatest.common.binary_path('extsearch/geo/meta/daemon/geometasearch')

    envcommon.create_rearrange_data()
    os.symlink(yatest.common.source_path('extsearch/geo/meta/tests/env/fixed/configs/geohosts.json'), 'geohosts.json')

    debug_port = yatest.common.get_param('DEBUG_PORT')
    is_debug = debug_port is not None

    with network.PortManager() as pm:
        upper_port = int(debug_port) if is_debug else pm.get_port_range(8031, 5)
        middle_port = upper_port + 1
        unused_port = upper_port + 2

        middle_cmdline = envcommon.set_up_middle(
            geometasearch,
            middle_port,
            [
                '-V',
                'RemoteWizardSockAddr={}'.format(os.environ['RECIPE_WIZARD_SOCKADDR']),
                '-V',
                'GeocoderSearchPrefix=http://{}/maps'.format(os.environ['RECIPE_GEOCODER_SOCKADDR']),
                '-V',
                'Business0SearchPrefix=http://localhost:{}/'.format(unused_port),  # TODO
                '-V',
                'Business1SearchPrefix=http://localhost:{}/'.format(unused_port),  # TODO
                yatest.common.source_path('extsearch/geo/meta/tests/env/fixed/configs/middle.cfg'),
            ],
        )

        # see library/recipes/tvmapi/README.md to check TVM ids and secrets
        os.environ['TVM_SECRET_1000510'] = 'LUTTSCreg1f976_B_EHKzg'

        upper_cmdline = envcommon.set_up_upper(
            geometasearch,
            upper_port,
            [
                '-V',
                'MiddlePort={}'.format(middle_port),
                '-V',
                'EnableTvm=true',
                '-V',
                'TvmClientId=1000510',
                '-V',
                'TvmApiLocalPort={}'.format(_get_tvmapi_port()),
                yatest.common.source_path('extsearch/geo/meta/tests/env/fixed/configs/upper.cfg'),
            ],
        )

        log_paths = envcommon.get_log_paths()
        with Service(middle_cmdline, debug=is_debug) as middle_service:
            middle_sockaddr = middle_service.warm_up('yandsearch?info=getconfig', port=middle_port)
            with Service(upper_cmdline, debug=is_debug) as upper_service:
                upper_sockaddr = upper_service.warm_up('yandsearch?info=getconfig', port=upper_port)
                yield envcommon.Metasearches(middle_sockaddr.get_url(), upper_sockaddr.get_url(), log_paths)

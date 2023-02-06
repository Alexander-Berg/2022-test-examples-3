"""
    Test for ISS Api
"""

import requests_mock
from urllib import parse as urlparse

from search.mon.wabbajack.libs.modlib.api_wrappers.iss import ISS

# import test data
from . import STATUS, VERSION

HOST = 'watchman.search.yandex.net'
PORT = 25536


class TestISSApi:
    """
        test suite for iss api wrapper
    """

    props_endpoints_map = (
        ('version', 'version', VERSION),
        ('status', 'status', STATUS)
    )

    def setup_class(self):
        self.iss = None
        self.version = None
        self.status = None

        with requests_mock.Mocker() as m:
            for epname, uri, test_responce in self.props_endpoints_map:
                m.register_uri('GET', urlparse.urljoin(f'http://{HOST}:{PORT}', uri), json=test_responce,
                               status_code=200)
            self.iss = ISS(HOST, PORT)

            self.version = self.iss.version
            self.status = self.iss.status

    def test_responce(self):
        assert self.version == VERSION
        assert self.status == STATUS

    def test_enpoints_avail(self):
        for endpoint, uri in ISS.Meta.endpoints:
            with requests_mock.Mocker() as m:
                m.register_uri('GET', urlparse.urljoin(f'http://{HOST}:{PORT}', uri), json={}, status_code=200)
                assert hasattr(self.iss, endpoint)
                assert getattr(self.iss, endpoint, None) is not None

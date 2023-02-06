"""
    Test for Spellbook Api
"""

import requests_mock
from urllib import parse as urlparse

from search.mon.wabbajack.libs.modlib.api_wrappers.spellbook.__init__ import LitanyApiWrapper, Spell

from search.mon.wabbajack.libs.modlib.tests.spellbook import ANSWER, QUERY, RESULT_DICT, SPELLS

HOST = 'localhost'
PORT = 5001


class TestSpellbookApi:
    """
    test suite for Spellbook API wrapper
    """

    def setup_class(self):
        self.iss = None
        self.version = None
        self.status = None

        test_spells = [
            Spell.factory('place_fs', 'space', directory='/place'),
            Spell.factory('ssd_fs', 'space', directory='/ssd'),
            Spell.factory('webcache_size', 'dir_size', directory='/place/db/bsconfig/webcache'),
            Spell.factory('webcache_users', 'dir_users', directory='/place/db/bsconfig/webcache'),
            Spell.factory('disk_usage', 'disks_io', max_atop_age=48, atop_start_date='12:13', atop_stop_date='12:14'),
            Spell.factory('pid_to_slot', 'pid_to_slot'),
            Spell.factory(
                'proc_disk_u', 'process_disk_usage', max_atop_age=48, atop_start_date='12:13', atop_stop_date='12:14'
            ),
        ]

        with requests_mock.Mocker() as m:
            m.register_uri(
                'GET', urlparse.urljoin(f'http://{HOST}:{PORT}', '/api/spells/'), json=SPELLS, status_code=200
            )
            m.register_uri(
                'POST', urlparse.urljoin(f'http://{HOST}:{PORT}', '/api/litany/'), json=ANSWER, status_code=200
            )
            self.litany = LitanyApiWrapper(timeout=300)
            self.litany.hosts(
                'sas1-0535.search.yandex.net', 'watchman.search.yandex.net'
            ).spells(*test_spells).from_user('naumbi4').cast()

    def test_response(self):
        assert self.litany.request == QUERY
        assert self.litany.result == ANSWER['data']
        assert self.litany.dict == RESULT_DICT
        assert isinstance(self.litany.dict, dict)

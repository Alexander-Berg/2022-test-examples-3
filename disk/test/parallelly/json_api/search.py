import urllib2

from mock import patch

from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import DiskSearchStub
from test.parallelly.json_api.base import CommonJsonApiTestCase


class SearchJsonApiTestCase(CommonJsonApiTestCase):
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {DiskSearchStub})

    def test_new_search_HTTP_429_Too_Many_Requests_propagated_to_client(self):
        """
        https://st.yandex-team.ru/CHEMODAN-70630
        """
        http_429_error = urllib2.HTTPError('http://search-disk/search', 429, 'Too Many Requests', {}, None)
        with patch('mpfs.engine.http.client.open_url', side_effect=http_429_error):
            self.json_error('new_search', {'uid': self.uid, 'path': '/disk', 'query': 'folder'}, status=429)

import mock

from attrdict import AttrDict
from hamcrest import assert_that, has_key

from mpfs.common.static import tags
from mpfs.config import settings
from mpfs.platform.auth import (
    OAuthAuth,
    PassportCookieAuth,
)
from test.unit.base import NoDBTestCase


class PassportCookieAuthTestCase(NoDBTestCase):
    fake_platform = {
        'auth': [
            {
                'name': 'test',
                'enabled': True,
                'auth_methods': ['cookie'],
                'allowed_origin_hosts': [r'^yandex\.%(tlds)s$'],
                'oauth_client_id': 'oauth_client_id',
                'oauth_client_name': 'oauth_client_name',
            },
        ],
        'tlds': ['ru', 'com', 'com.tr']
    }

    def test_tld_replacement(self):
        with mock.patch.dict(settings.platform, self.fake_platform):
            auth = PassportCookieAuth()

        assert_that(auth.client_info_by_host, has_key(r'^yandex\.(ru|com|com\.tr)$'))

    def test_get_credentials(self):
        with mock.patch.dict(settings.platform, self.fake_platform):
            auth = PassportCookieAuth()

        request = AttrDict({
            'mode': tags.platform.EXTERNAL,
            'raw_headers': {
                'Host': 'test_host',
                'Origin': 'test_origin',
                'Cookie': 'Session_id=test_cookie;sessionid2=test_cookie',
                'X-Uid': '123',
            },
            'remote_addr': '127.0.0.1',
            'cookie_auth_client_id': None,
        })

        credentials = auth._get_credentials(request)
        assert credentials is not None
        assert credentials.x_uid == '123'


class OAuthAuthTestCase(NoDBTestCase):
    def test_get_credentials(self):
        auth = OAuthAuth()

        request = AttrDict({
            'mode': tags.platform.EXTERNAL,
            'raw_headers': {
                'Authorization': 'OAuth test',
            },
            'remote_addr': '127.0.0.1',
        })

        credentials = auth._get_credentials(request)
        assert credentials is not None

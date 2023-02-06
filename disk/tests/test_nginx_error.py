import pytest
from nginx_errors.nginx_errors import groupped_vhost
from nginx_errors.nginx_errors import api_request_normalizer


class TestRegex:
    test_vhost = [
        ('disk.yandex.net', 'disk.yandex.<TLD>'),
        ('cloud-api.yandex.net', 'cloud-api.yandex.<TLD>'),
        ('cloud-api.yandex.net:443', 'cloud-api.yandex.<TLD>'),
        ('intapi.disk.yandex.net', 'intapi.disk.yandex.<TLD>'),
    ]

    test_uri = [
        ('/v1/disk/resources/924587529:931f81b43a7380e222a7797727c6137cba115dd95322a9ce3bf3e94258def18a/image_metadata',
         '/v1/disk/resources/<RESOURCE_ID>/image_metadata'
         ),
        ('/v1/11111/personality/profile/events/flights/actual/' +
         'd52cf6c7b51ef8897e6980c11e6474ff5fbeaaf552973289cd122bed2178b87d.1569744000000',
         '/v1/<INT>/personality/profile/events/flights/actual/<RESOURCE_ID>/'
         ),
        ('/v1/yaid-713616441570097392/personality/profile/market/delivery_addresses',
         '/v1/<INT>/personality/profile/market/delivery_addresses'
         ),
    ]

    @pytest.mark.parametrize("host,expected", test_vhost)
    def test_groupped_vhost(self, host, expected):
        assert groupped_vhost(host) == expected

    @pytest.mark.parametrize("uri,expected", test_uri)
    def test_api_request_normalizer(self, uri, expected):
        assert api_request_normalizer(uri) == expected

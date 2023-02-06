# coding: utf-8
import urlparse

import mock

from hamcrest import assert_that
from mpfs.core.services.mpfsproxy_service import mpfsproxy


def test_get_album():
    with mock.patch.object(mpfsproxy, 'open_url', return_value='{}') as open_url_mock:
        mpfsproxy.get_album(uid='123', album_id='asd', amount=0)
        url = open_url_mock.call_args[0][0]
        parsed_url = urlparse.urlparse(url)
        qs_params = urlparse.parse_qs(parsed_url.query, keep_blank_values=True)

        assert_that(qs_params['uid'] == ['123'])
        assert_that(qs_params['album_id'] == ['asd'])
        assert_that(qs_params['amount'] == ['0'])

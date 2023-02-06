# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty
import pytest
from django.conf import settings
from django.test.client import RequestFactory
from django.utils.encoding import force_text
from hamcrest import assert_that, has_entries, contains_inanyorder

from common.tester.utils.replace_setting import replace_setting
from common.utils.blackbox_wrapper import get_blackbox_oauth_info, get_blackbox_oauth_args, get_session


BLACKBOX_RESPONSE = """<?xml version="1.0" encoding="UTF-8"?>
<doc>
    <OAuth>
        <uid>424242</uid>
        <token_id>5073324</token_id>
        <device_id/>
        <device_name/>
        <ctime>2018-10-26 12:57:25</ctime>
        <issue_time>2018-10-26 12:57:25</issue_time>
        <expire_time>2019-10-26 12:57:25</expire_time>
        <is_ttl_refreshable>1</is_ttl_refreshable>
        <client_id>8a2ce96bbfcd4a5fa4936ec74fe178b7</client_id>
        <client_name>Яндекс.Электрички Dev</client_name>
        <client_icon/>
        <client_homepage/>
        <client_ctime>2018-10-26 11:44:34</client_ctime>
        <client_is_yandex>0</client_is_yandex>
        <xtoken_id/>
        <meta/>
    </OAuth>
    <uid hosted="0">424242</uid>
    <login>suburbanselling</login>
    <have_password>1</have_password>
    <have_hint>1</have_hint>
    <karma confirmed="0">0</karma>
    <karma_status>0</karma_status>
    <status id="0">VALID</status>
    <connection_id>t:5073324</connection_id>
    <user_ticket>userticket123</user_ticket>
    <address-list>
        <address validated="1" default="1" rpop="0" silent="0" unsafe="0" native="1" born-date="2018-05-29 15:50:51">
            suburbanselling@yandex.ru
        </address>
        <address validated="1" default="1" rpop="0" silent="0" unsafe="0" native="1" born-date="2018-05-29 15:50:51">
            suburbanselling42@yandex.ru
        </address>
    </address-list>
</doc>
"""


BLACKBOX_SESSION_RESPONSE = """<?xml version="1.0" encoding="UTF-8"?>
<doc>
    <error>OK</error>
    <status id="0">VALID</status>
    <uid hosted="0">4005</uid>
    <auth>
        <secure>1</secure>
    </auth>
</doc>"""


class TestGetBlackboxOauthInfo(object):
    def test_valid(self):
        # valid answer
        def request_callback(request, uri, response_headers):
            assert request.headers['Authorization'] == 'OAuth: oauth_token'
            assert request.headers['X-Ya-Service-Ticket'] == 'ticket'
            assert request.querystring['some_key'][0] == 'value'
            assert request.querystring['get_user_ticket'][0] == 'yes'
            assert request.querystring['emails'][0] == 'getdefault'
            assert request.querystring['attributes'][0] == '42,43'

            return [200, response_headers, BLACKBOX_RESPONSE.encode('utf8')]

        httpretty.register_uri(httpretty.GET, body=request_callback, content_type='application/xml',
                               uri='{}?userip=real_ip&dbfields=&method=oauth'.format(settings.BLACKBOX_URL))

        info = get_blackbox_oauth_info(
            oauth_header='OAuth: oauth_token', user_ip='ip', tvm_ticket='ticket',
            get_user_ticket='yes',
            attributes=[42, 43],
            some_key='value',
        )

        assert_that(info, has_entries({
            'uid': '424242',
            'status': 'VALID',
            'user_ticket': 'userticket123',
            'emails': contains_inanyorder(
                has_entries({'address': 'suburbanselling@yandex.ru'}),
                has_entries({'address': 'suburbanselling42@yandex.ru'}),
            )
        }))

    def test_bad_args(self):
        with pytest.raises(Exception) as ex:
            get_blackbox_oauth_info(oauth_header='header42', user_ip='')
        assert 'не пустыми' in force_text(ex)

    def test_bad_bb_answer(self):
        httpretty.register_uri(httpretty.GET, body='', content_type='application/xml',
                               uri='{}?userip=real_ip&dbfields=&method=oauth'.format(settings.BLACKBOX_URL_YATEAM))

        with pytest.raises(Exception) as ex:
            get_blackbox_oauth_info('header', 'ip', blackbox_url=settings.BLACKBOX_URL_YATEAM, tvm_ticket='ticket')
        assert 'no element' in force_text(ex)


@replace_setting('REMOTE_ADDR_META_VARIABLE', 'REMOTE_ADDR')
def test_get_blackbox_oauth_args():
    request = RequestFactory().get(
        'yandex.ru/some/url',
        HTTP_AUTHORIZATION='OAuth:    mytoken42  ',
        REMOTE_ADDR='1.1.1.1',
    )

    assert get_blackbox_oauth_args(request) == {
        'user_ip': '1.1.1.1',
        'oauth_header': 'OAuth:    mytoken42  '
    }

    # invalid auth
    request = RequestFactory().get(
        'yandex.ru/some/url',
        HTTP_AUTHORIZATION='',
        REMOTE_ADDR='1.1.1.1',
    )

    with pytest.raises(ValueError):
        get_blackbox_oauth_args(request)


class TestGetBlackboxSessionID(object):
    def test_valid(self):
        def request_callback(request, uri, response_headers):
            assert request.headers['X-Ya-Service-Ticket'] == 'session_ticket'

            return [200, response_headers, BLACKBOX_SESSION_RESPONSE.encode('utf8')]

        httpretty.register_uri(httpretty.GET, body=request_callback, content_type='application/xml',
                               uri='{}?method=sessionid&sessionid=id&dbfields=userinfo.lang.uid,accounts.login.uid,'
                                   'subscription.login.669&userip=ip&regname=yes&host=host&'
                                   'aliases=all'.format(settings.BLACKBOX_URL_YATEAM))

        info = get_session(
            'ip', 'host', 'id',
            blackbox_url=settings.BLACKBOX_URL_YATEAM,
            tvm_ticket='session_ticket'
        )

        assert_that(info, has_entries({
            'uid': '4005',
            'status': 'VALID'
        }))

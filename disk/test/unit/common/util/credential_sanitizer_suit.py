# -*- coding: utf-8 -*-
import re

from nose_parameterized import parameterized

from test.unit.base import NoDBTestCase
from werkzeug.datastructures import EnvironHeaders

from mpfs.common.util.credential_sanitizer import CredentialSanitizer
from mpfs.platform.utils import parse_cookie


class CredentialSanitizerTestCase(NoDBTestCase):
    full_user_ticket = '3:user:CPgoEMDOmdwFGj4KBgi82Ib6DhC82Ib6DhoTY2xvdWRfYXBpOmRpc2suaW5mbxoTY2xvdWRfYXBpOmRpc2sucm' \
        'VhZCC9iXooAQ:FJ88E8SESlik3pclMXTrKD_KoVHvceScLdozD8okg_eAUMIJ3fEFjTWKBwZSr8EYuo5bMl0e5OJ-d7VMPmAu1udjyYVGZN2' \
        'zv5akWiep5kv3fmnlnoVdl9anptkYleDII_EY8mY83WGJA6LqoI71jDqED6PrmZLWyq7YvoEKuRwx'

    sanitized_user_ticket = '3:user:CPgoEMDOmdwFGj4KBgi82Ib6DhC82Ib6DhoTY2xvdWRfYXBpOmRpc2suaW5mbxoTY2xvdWRfYXBpOmRpc2sucm' \
        'VhZCC9iXooAQ:'

    @parameterized.expand([
        ('OAuth test_oauth', re.compile('^OAuth .+$', re.IGNORECASE)),
        ('oauth test_oauth', re.compile('^OAuth .+$', re.IGNORECASE)),
        ('OAuth test_oauth; other=123', re.compile('^OAuth .+; other=123$', re.IGNORECASE)),
        ('other=123; OAuth test_oauth', re.compile('^other=123; OAuth .+$', re.IGNORECASE)),
        ('Xiva test_oauth', re.compile('^Xiva .+$', re.IGNORECASE)),
        ('xiva test_oauth', re.compile('^Xiva .+$', re.IGNORECASE)),
        ('Xiva test_oauth; other=123', re.compile('^Xiva .+; other=123$', re.IGNORECASE)),
        ('other=123; Xiva test_oauth', re.compile('^other=123; Xiva .+$', re.IGNORECASE)),
    ])
    def test_authorization_sanitizing_credentials(self, authorization, correct_pattern):
        hashed_authorization = CredentialSanitizer._hash_authorization_header_values(authorization)
        assert authorization != hashed_authorization
        assert re.match(correct_pattern, hashed_authorization)

    @parameterized.expand([
        ('Session_id=test.unique_sign', 'Session_id=test',),
        ('Session_id=test.several.dots.unique_sign', 'Session_id=test.several.dots',),
        ('sessionid2=test.unique_sign2', 'sessionid2=test',),
        ('Session_id=test.unique_sign; sessionid2=test.unique_sign2', 'Session_id=test; sessionid2=test',),
        ('sessionid2=test.unique_sign2; Session_id=test.unique_sign', 'sessionid2=test; Session_id=test',),
        ('yandex_gid=213; Session_id=test.unique_sign; sessionid2=test.unique_sign', 'yandex_gid=213; Session_id=test; sessionid2=test',),
        ('yandex_gid=213; Session_id=test.unique_sign', 'yandex_gid=213; Session_id=test',),
        ('Session_id=test; sessionid2=test.unique_sign2; yandex_gid=213', 'Session_id=test; sessionid2=test; yandex_gid=213',),
        ('sessionid2=test.unique_sign2; yandex_gid=213', 'sessionid2=test; yandex_gid=213',),
        ('Session_id=test.unique_sign; yandex_gid=213; sessionid2=test.unique_sign2', 'Session_id=test; yandex_gid=213; sessionid2=test',),
        ('_ym_isad=2; Session_id=test.unique_sign; yandex_gid=213; sessionid2=test.unique_sign2; mda=0', '_ym_isad=2; Session_id=test; yandex_gid=213; sessionid2=test; mda=0',),
    ])
    def test_cookie_sanitizing_credentials(self, cookie, correct_logged_cookie):
        logged_cookie = CredentialSanitizer._truncate_cookie_header_values(cookie)
        parsed_logged_cookie = parse_cookie(logged_cookie)
        parsed_correct_logged_cookie = parse_cookie(correct_logged_cookie)
        assert sorted(parsed_logged_cookie.items()) == sorted(parsed_correct_logged_cookie.items())

    def test_sanitize_tvm_2_user_ticket(self):
        assert self.sanitized_user_ticket == CredentialSanitizer._sanitize_tvm_2_user_ticket(self.full_user_ticket)

    def test_headers_sanitizing(self):
        session_id = 'Session_id.unique_sign'
        session_id2 = 'sessionid2.unique_sign'
        oauth = 'oauth'
        fake = 'any_fake_value'
        headers = EnvironHeaders({
            'HTTP_COOKIE': 'Session_id=%s;sessionid2=%s' % (session_id, session_id2),
            'HTTP_ORIGIN': 'https://example.com',
            'HTTP_AUTHORIZATION': 'OAuth %s; fake %s' % (oauth, fake),
            'HTTP_YANDEX_AUTHORIZATION': 'OAuth %s' % oauth,
            'HTTP_OTHER': 'stuff',
            'HTTP_TICKET': 'ticket',
            'HTTP_X_YA_SERVICE_TICKET': 'x_ya_service_ticket',
            'HTTP_X_YA_USER_TICKET': self.full_user_ticket,
            'HTTP_X-FORWARDED-USER': 'OAuth %s' % oauth,
            'HTTP_DISK_PUBLIC_PASSWORD': 'secret'
        })
        hashed_headers = dict(CredentialSanitizer.get_headers_list_with_sanitized_credentials(headers))

        assert re.search(re.compile('OAuth ([^; $]*)'), hashed_headers['X-Forwarded-User']).group(1) != oauth
        assert re.search(re.compile('OAuth ([^; $]*)'), hashed_headers['Authorization']).group(1) != oauth
        assert re.search(re.compile('OAuth ([^; $]*)'), hashed_headers['Yandex-Authorization']).group(1) != oauth
        assert re.search(re.compile('fake ([^; $]*)'), hashed_headers['Authorization']).group(1) == fake

        assert re.search(re.compile('Session_id=([^; $]*)'), hashed_headers['Cookie']).group(1) != session_id
        assert re.search(re.compile('sessionid2=([^; $]*)'), hashed_headers['Cookie']).group(1) != session_id2

        assert hashed_headers['Ticket'] != headers['Ticket']
        assert hashed_headers['X-Ya-Service-Ticket'] != headers['X-Ya-Service-Ticket']
        assert hashed_headers['X-Ya-User-Ticket'] == self.sanitized_user_ticket

        assert hashed_headers['Other'] == headers['Other']
        assert hashed_headers['Origin'] == headers['Origin']
        assert hashed_headers['Disk-Public-Password'] != headers['Disk-Public-Password']
        assert hashed_headers['Disk-Public-Password'] == ''

    @parameterized.expand([
        ('http://example.org', 'http://example.org'),
        ('http://example.org?uid=1', 'http://example.org?uid=1'),
        ('http://example.org?oauth_token=1', 'http://example.org?oauth_token=1JfRuuWXR9idaQwmGHRDUev7nR8%3D'),
        ('http://example.org?sessionid=1.uniquie_sign', 'http://example.org?sessionid=1'),
        ('http://example.org?sslsessionid=1.unique_sign', 'http://example.org?sslsessionid=1'),
        ('http://example.org?oauth_token=1&sessionid=2.unique_sign', 'http://example.org?oauth_token=1JfRuuWXR9idaQwmGHRDUev7nR8%3D&sessionid=2'),
        ('http://example.org?sessionid=2.unique_sign&oauth_token=1', 'http://example.org?sessionid=2&oauth_token=1JfRuuWXR9idaQwmGHRDUev7nR8%3D'),
        ('http://example.org?oauth_token=2&oauth_token=1', 'http://example.org?oauth_token=%2FcPefr%2BG4aINummYYBZSZUeSHYw%3D&oauth_token=1JfRuuWXR9idaQwmGHRDUev7nR8%3D'),
        ('http://example.org?oauth_token=1&sessionid=2.unique_sign&sslsessionid=3.unique_sign3', 'http://example.org?oauth_token=1JfRuuWXR9idaQwmGHRDUev7nR8%3D&sessionid=2&sslsessionid=3'),
    ])
    def test_sanitizing_credentials_in_qs(self, url, hashed_url):
        # Правильные значение hashed_url зависят от использованной хеш функции здесь
        url = CredentialSanitizer.sanitize_qs_parameter_in_url('oauth_token', url)
        url = CredentialSanitizer.sanitize_qs_parameter_in_url('sessionid', url)
        url = CredentialSanitizer.sanitize_qs_parameter_in_url('sslsessionid', url)
        assert url == hashed_url

# -*- coding: utf-8 -*-

import app_host
import base64
import datetime
import urllib
import urlparse
import json
import jsonschema
import logging
import os
import re
import socket
import time
import zlib

from copy import deepcopy
from collections import OrderedDict
import uuid

import pytest
import requests

from runtime_tests.report.proto3.http_pb2 import THttpRequest
from runtime_tests.report.proto3.post_data_pb2 import TPostData
from runtime_tests.util.proto.http.stream import HTTPReaderException
from runtime_tests.util.proto.handler.server.http import HTTPConfig
from runtime_tests.util.predef.handler.server.http import SimpleConfig
from runtime_tests.util.predef.http.response import ok
from protobuf_to_dict import protobuf_to_dict

from report.const import *
from report.util import *

# Логин "robbitter-5151382063" c паролем "123456qwerty"
# Логин "robbitter-5151382063@закодированный.домен" c паролем "123456qwerty"
cur_time = str(int(time.time()))
cookie_auth = (
    '3:' + cur_time + '.5.0.' + cur_time +
    '542:sVQ5TgwPRiYIBAAAuAYCKg:4c.1|447119192.0.2|211407.175358.qrs2XwICOeeb8-a8SRjT12F99ng; '
    'L=QmsFYlp6RAFAd39jVHdcdV5AclltdG5oBx4pKjk5EiAwZF1lc0FSdQVBQlI=.1577708976.14095.385073.4e0cfb2226c22ce2696d8db3434e21e8; '
    'yandex_login=robbitter-5151382063; '
)

cookie_noauth = 'noauth:1427381588'


class PassportSession(object):
    """
    https://wiki.yandex-team.ru/serp/report/Testovye-akkaunty/ - session_id for test-report-integration
    """
    api_url = 'https://passport.yandex.ru/auth'
    api_params = {
        'retpath': 'http://passport.yandex.ru/profile',
        'twoweeks': '1'
    }
    login = 'yandex-team-test-report'
    password = '123456qwerty'

    session = requests.Session()

    user_name = 'Test Reportovich'
    id = '886656875'
    avatar = '/get-yapic/60687/kgncwzxYWdiKwqhJyWqVCxbQgM-1/'

    def properties(self):
        return {
            "login": self.login,
            "user_name": self.user_name,
            "avatar": self.avatar,
            "id": self.id
        }

    def auth(self):
        self.session.post(
            self.api_url,
            params=self.api_params,
            data={
                'login': self.login,
                'passwd': self.password,
            },
            allow_redirects=False,
        )

    def get_cokies(self):
        return self.session.cookies

    def __init__(self, login=None, password=None):
        if login is not None or password is not None:
            self.login = login
            self.password = password
        self.auth()


PS = PassportSession()

# HTTP request body, implicitly used in `send_query` when converting query object to string
QUERY = """{method} {uri} HTTP/1.1
{headers}
{data}"""

YP_EXPIRE = '2427381588'


_HTTP_ADAPTER_SOURCES = [
    'INVERTED_REPORT',
    # sources below are aliased by INVERTED_REPORT
    # 'HTML_DUMP',
    # 'POST_SEARCH_TEMPLATE_DATA',
    # 'PRE_SEARCH_TEMPLATE_DATA',
    'REPORT_DUMP',
    # 'REPORT_INIT_AND_HEADERS',
    # 'REPORT_POST_SEARCH',
    # 'REPORT_PRE_SEARCH',
]


def _generate_broken_chars(is_unicode=0):
    '''
    берем символы из alpha и дважды их кодируем в utf8
    и создаем из полученных символов регулярку
    в результате для русской буквы ф(u"\x0444", "\xd1\x84") должны получить 4 байта "\xc3\x91\xc2\x84"
    '''
    alpha = u'абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЬЪЭЮЯ'

    chars = []

    for i in range(0, 32):
        c = chr(i)
        if c not in ['\t', '\n', '\r']:
            chars.append(chr(i))

    for c in alpha:
        temp = ''
        for b in c.encode("utf-8"):
            temp += unichr(ord(b))
        chars.append(temp if is_unicode else temp.encode("utf-8"))

    assert chars
    return chars


RE_BROKEN_CHARS = re.compile('(' + '|'.join(_generate_broken_chars()) + ')')


class Path():
    def __init__(self):
        self.url = SEARCH
        self.set_params({'text': TEXT})

    def set_url(self, url):
        self.url = url or SEARCH
        if '?' in self.url:
            self.set_params({})

    def __parse_params(self, params):
        if not isinstance(params, list) or filter(lambda x: isinstance(x, tuple), params):
            return params

        # XXX convert flat params list to pairs list to feed OrderedDict
        pairs, counter = [], 0
        for value in params:
            counter += 1
            if counter % 2:
                pairs.append([value])
            else:
                pairs[-1].append(value)

        return pairs

    def get_param(self, param):
        return self.params.get(param, None)

    def set_params(self, params):
        pairs = params and deepcopy(self.__parse_params(params)) or {}
        self.params = OrderedDict(pairs)

    def add_params(self, params):
        if not params:
            return
        assert isinstance(params, dict), 'NOT IMPLEMENTED non-dict params: ' + str(params)
        for pk, pv in params.items():
            if pk in self.params:
                v = self.params[pk]
                v = v if isinstance(v, list) else [v]
                v.append(pv)
                self.params[pk] = v
            else:
                self.params[pk] = pv

    def replace_params(self, params):
        if not params:
            return
        self.params.update(OrderedDict(self.__parse_params(params)))

    def remove_params(self, *params):
        if not params:
            return
        for p in params:
            self.params.pop(p, None)

    def set(self, url=None, params=None):
        self.url = url or SEARCH
        self.set_params(params)

    def __str__(self):
        url = self.url
        if not self.params:
            return url

        params = []
        for pk, pv in self.params.items():
            pv = pv if isinstance(pv, list) else [pv]
            for v in pv:
                if v is None:
                    ev = ''
                elif isinstance(v, unicode):
                    ev = v.encode('utf-8')
                else:
                    ev = str(v)

                tk = urllib.quote_plus(str(pk))
                tv = urllib.quote_plus(ev)
                params.append(tk + '=' + tv)

        url += '&' if '?' in url else '?'
        url += '&'.join(params)

        return url


class YP(dict):
    def __init__(self):
        pass

    def set_sp(self, value):
        if 'sp' in self:
            self['sp'] = self['sp'] + value
        else:
            self['sp'] = value

    def set_cr(self, value):
        self['cr'] = value

    def set_sz(self, value):
        self['sz'] = value

    def set_szm(self, value):
        self['szm'] = value

    def set_ygu(self, value):
        self['ygu'] = value

    def set_gpauto(self, lat, lon, precision='100', device='1', timestamp=int(time.time())):
        self['gpauto'] = ':'.join((
            lat.replace('.', '_'),
            lon.replace('.', '_'),
            str(precision),
            str(device),
            str(timestamp),
        ))

    def __str__(self):
        s = ''
        for k in self:
            s = ''.join((s, YP_EXPIRE, '.', k, '.', urllib.quote(str(self[k])), '#'))
        return s[:-1]


class Cookie(dict):
    def __init__(self):
        self['Session_id'] = cookie_noauth
        self['yp'] = YP()

    def set_yandex_gid(self, value):
        self['yandex_gid'] = value

    def set_yandexuid(self, value):
        self['yandexuid'] = value

    def set_my(self, value):
        self['my'] = value

    def set_mda(self, value):
        self['mda'] = value

    def set_session_id(self):
        self['Session_id'] = cookie_auth

    def set_no_session_id(self):
        self['Session_id'] = cookie_noauth

    def reset_session_id(self):
        del self['Session_id']

    def set_b(self, value):
        self['b'] = value

    def set_ys(self, value):
        self['ys'] = value

    @property
    def yp(self):
        return self['yp']

    def __str__(self):
        return '; '.join(["%s=%s" % (k, v) for (k, v) in self.items()])


class Headers(dict):
    def __init__(self):
        self['Host'] = 'yandex.ru'
        self['User-Agent'] = USER_AGENT_DESKTOP
        self['Accept'] = 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
        self['Accept-Language'] = 'ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3'
        self['Connection'] = 'close'
        self['Cookie'] = Cookie()
        self['X-Yandex-Internal-Request'] = 1

    @property
    def cookie(self):
        return self['Cookie']

    def set_content_length(self, l):
        self['Content-Length'] = str(l)

    def set_exp_params(self, params):
        self['X-Yandex-ExpSplitParams'] = base64.b64encode(json.dumps(params))

    def is_https(self):
        return self.get('X-Yandex-HTTPS') == 'yes'

    def set_https(self, enabled=True):
        if enabled:
            self['X-Yandex-HTTPS'] = 'yes'
        else:
            self.pop('X-Yandex-HTTPS', None)

    def set_user_agent(self, user_agent):
        self['User-Agent'] = user_agent

    def set_custom_headers(self, headers):
        self.update(headers)

    def set_internal(self, value=True):
        self.set_external(not value)

    def set_external(self, value=True):
        if value:
            self['X-Yandex-Internal-Request'] = 0
            self['X-Forwarded-For-Y'] = EXTERNAL_IP
        else:
            self['X-Yandex-Internal-Request'] = 1
            self.pop('X-Forwarded-For-Y', None)

    def set_forward_for_y(self, value):
        self['X-Forwarded-For-Y'] = value

    def set_forward_for(self, value):
        self['X-Forwarded-For'] = value

    def set_raw_cookie(self, cookie):
        self['Cookie'] = cookie

    def get_tld(self):
        host = self['Host']
        assert host
        m = re.search(r'(?<=yandex\.)(.+)$', host)
        assert m, 'Unknown host: ' + host
        return m.group(1).lower()

    def set_host(self, host):
        if not host:
            host = RU
        if host in YA_TLD:
            self['Host'] = 'yandex.' + host
        else:
            self['Host'] = host

    def set_region(self, region_id):
        if not region_id:
            region_id = 213
        self['X-Region-City-Id'] = region_id

    def set_content_type(self, value):
        self['Content-Type'] = value

    def __str__(self):
        s = ''
        for k in self:
            s = ''.join((s, k, ': ', str(self[k]), '\n'))
        return s

    def curl(self):
        s = ''
        for k in self:
            h = ''.join((k, ': ', str(self[k]))).replace('"', '\\"')
            s += ' -H "%s"' % h
        return s


class Query():
    def __init__(self, host, port, additional_flags):
        self.path = Path()
        self.headers = Headers()
        self.set_external(True)  # SERP-31656
        self.set_https()
        self.method = 'GET'
        self.data = ''
        self.flags = {'validate_templates_perc': 0}  # XXX avoid tests blinking
        self.additional_flags = additional_flags
        self.target_host = host
        self.target_port = port
        self.exp_params = EXP_PARAMS_DESKTOP  # WEBREPORT-57
        self.http_adapter_enabled = False  # default mode
        # self.dump_eventlog = False  # default mode

        # generate custom request marker for logs testing purpose
        self.reqid = str(uuid.uuid1())
        self.headers.set_custom_headers({'X-Yandex-Test-UUID': self.reqid})

    def set_beta_domain(self, beta_domain = 'hamster.yandex.ru'):
        tld = self.headers.get_tld();
        m = re.search(r'^(.*?yandex\.)(.+)$', beta_domain)
        if m:
            host = '{dn}{tld}'.format(dn=m.group(1).lower(), tld=tld)
            self.headers.set_host(host)


    def set_method(self, method):
        self.method = method
        if method == 'POST' and not self.headers.get('Content-type'):
            self.headers.set_content_type('application/x-www-form-urlencoded')

    def set_post_params(self, data):
        if isinstance(data, dict):
            self.data = urllib.urlencode(data)
        else:
            self.data = data
        self.headers.set_content_length(len(self.data))

    def set_query_type(self, query_type):
        if query_type in [XML]:
            self.set_internal()
        if query_type in [GATEWAY_TOUCH, GATEWAY]:
            self.replace_params({'rpt': 'gateway'})
            self.set_internal()
        if query_type == GATEWAY_TOUCH:
            self.replace_params({'banner_ua': USER_AGENT_TOUCH})
            self.set_internal()

        self.set_url(URL_BY_TYPE[query_type])
        self.set_user_agent(USERAGENT_BY_TYPE[query_type])

        # WEBREPORT-57
        self.exp_params = EXP_PARAMS_BY_TYPE[query_type]
        self.headers.set_exp_params(self.exp_params)

    def set_yandexuid(self, value=YANDEXUID):
        self.replace_params({'yandexuid': value})
        self.headers.cookie.set_yandexuid(value)

    def reset_flags(self):
        self.flags = {}
        return self

    def add_flags(self, *args):
        if not args:
            return
        elif len(args) > 1:
            flags = dict([(x, 1) for x in args if x])
        else:
            if isinstance(args[0], dict):
                flags = args[0]
            else:
                flags = {args[0]: 1}

        # update complicated flags
        for f in ['srcrwr', 'app_host', 'app_host_srcask', 'app_host_srcskip', 'app_host_path', 'app_host_source']:
            old_flag = self.flags.get(f)
            if not old_flag:
                continue
            new_flag = flags.get(f)
            if not new_flag:
                continue
            old_flag.update(flags[f])
            flags[f] = old_flag

        self.flags.update(flags)

    def set_http_adapter(self, enabled=True):
        logging.info("Enabled http_adapter mode: %s", enabled)
        self.http_adapter_enabled = enabled
        self.path.add_params({
            'http_proxy': 'HTTP_ADAPTER',
        })

    def set_dump_eventlog(self, enabled=True):
        logging.info("Enabled eventlog dump: %s", enabled)
        # self.dump_eventlog = enabled  # currently unused, as not set in flags
        self.path.add_params({
            'dump': 'eventlog',
        })

    def set_timeouts(self):
        self.path.add_params({
            'timeout': '999999',
            'waitall': '999999',
        })

    # for tests speedup
    def set_offline(self):
        self.replace_params({'no-template': 1})
        self.add_params({'srcrwr': ','.join([x + ':localhost:1' for x in ['UPPER', 'APP_HOST']])})

    def merge_addition_flags(self, left, right):
        if not left:
            return right

        if isinstance(right, dict):
            for k in right.keys():
                left[k] = self.merge_addition_flags(left.get(k), right[k])
            return left
        elif isinstance(right, list):
            left.extend(right)
            return left
        else:
            return right

    def get_ip_6(self, host, port=80):
        # discard the (family, socktype, proto, canonname) part of the tuple
        # and make sure the ips are unique
        alladdr = list(
            set(
                map(
                    lambda x: x[4],
                    socket.getaddrinfo(host, port)
                )
            )
        )
        ip6 = filter(
            lambda x: ':' in x[0],  # means its ip6
            alladdr
        )
        return ip6

    def __str__(self):
        """
        String representation of Query. Please not that it is used in `send_query`
        in very implicit form to prepare complete http request, so it cannot be modified.
        """
        headers = deepcopy(self.headers)
        flags = deepcopy(self.flags)
        additional_flags = deepcopy(self.additional_flags)
        inverted_report = os.environ.get('REPORT_INVERTED')
        if inverted_report == '0':
            inverted_report = None

        srcrwr = flags.get('srcrwr', {})
        for k in ['APP_HOST_WEB', 'WIZARD', 'YABS_SETUP']:
            if not srcrwr.get(k):
                srcrwr[k] = '::1000'

        for k in ['APP_HOST_PRE']:
            if not srcrwr.get(k):
                srcrwr[k] = '::130'

        if not os.environ.get('BETA_HOST'):
            # REPORTINFRA-269, REPORTINFRA-324
            # Entire inverted scheme on betas should be enabled via beta configuration
            # in case of `black box` beta testing with functional tests.
            report_port = os.environ.get('PORT1', '80')
            my_ip = os.environ.get('CLIENT_IPV6_ADDRESS')
            if not my_ip:
                my_ip = self.get_ip_6(host=socket.gethostname(), port=report_port)[0][0]

            # be careful here: non-superuser report starts on 8081 by default
            if my_ip:
                rwr_value = '[{ip}]:{{port}}:2000'.format(ip=my_ip)
                for k in _HTTP_ADAPTER_SOURCES:
                    srcrwr[k] = rwr_value.format(port=report_port)

                if os.environ.get('FLAGS_JSON_TEST'):
                    srcrwr['FLAGS_PROVIDER'] = rwr_value.format(port=os.environ.get('FLAGS_PROVIDER_PORT', '16300'))
            else:
                logging.error("CLIENT_IPV6_ADDRESS is not set, cannot set HTTP_ADAPTER sources rewrites. ")

        if inverted_report:
            for source in ['APP_HOST', 'UPPER']:
                if source in srcrwr:
                    srcrwr.pop(source, None)

        # if self.dump_eventlog:
        #    flags['dump'] = 'eventlog'

        flags['srcrwr'] = srcrwr
        add_init_meta = {
            'pass-yabs-request-to-report': 1
        }
        if not inverted_report:
            add_init_meta.update({ 'use-device_config-setup': 0 })
            if 'app_host_path' not in flags:
                flags['app_host_path'] = {}
            flags['app_host_path']['TEMPLATE_PRE_SEARCH'] = '/_ctx/_json/templates-pre_search'
            flags['app_host_path']['TEMPLATE_POST_SEARCH'] = '/_ctx/_json/templates-post_search'

        if 'init_meta' in flags:
            for k in add_init_meta:
                flags['init_meta'][k] = add_init_meta[k]
        else:
            flags['init_meta'] = add_init_meta
        logging.info("inverted_report: %s, FLAGS: %s", inverted_report, json.dumps(flags))

        # flags['disable_flag_sections'] = 195190

        if flags:
            if 'X-Yandex-Internal-Flags' in headers:
                flags.update(json.loads(base64.b64decode(headers['X-Yandex-Internal-Flags'])))
            headers['X-Yandex-Internal-Flags'] = base64.b64encode(json.dumps(flags))
        if additional_flags:
            old = None
            if headers['X-Yandex-Internal-Flags']:
                old = json.loads(base64.b64decode(headers['X-Yandex-Internal-Flags']))
            self.merge_addition_flags(additional_flags, old)
            headers['X-Yandex-Internal-Flags'] = base64.b64encode(json.dumps(additional_flags))

        self.path.replace_params({'reqinfo': 'report-tests'})
        self.path.replace_params({'rps-limiter-quota': 'soy'})
        self.path.replace_params({'stoker-quota': 'flags_testing'})

        url = str(self.path)

        EXP_BOXES_KEY = 'X-Yandex-ExpBoxes'
        # disable inverted report with special test-id specified in apache config
        if not inverted_report:
            if EXP_BOXES_KEY in headers:
                headers[EXP_BOXES_KEY] = headers[EXP_BOXES_KEY] + ";126800,0,100"
            else:
                headers[EXP_BOXES_KEY] = "126800,0,100"

        return QUERY.format(
            method=self.method,
            uri=url,
            headers=str(headers),
            data=self.data,
        )

    def __repr__(self):
        return self.scheme() + self.hamster_host() + str(self.path)

    def hamster_host(self):
        tld = 'com.tr' if self.headers.get_tld() == 'com.tr' else 'ru'
        return 'hamster.yandex.' + tld

    def hamster_url(self):
        host = self.hamster_host()
        if self.method == 'GET':
            uri = str(self.path)
            uri += '&' if '?' in uri else '?'
            uri += 'no-tests=1'
            uri += '&myreqid=' + self.reqid
            return self.scheme() + host + uri
        else:
            return self.curl(host)

    def set_post_body_multipart_form_data(self, fields, files):
        """
        fields is a sequence of (name, value) elements for regular form fields.
        files is a sequence of (name, filename, content-type, value) elements for data to be uploaded as files
        """
        BOUNDARY = '----------ThIs_Is_tHe_bouNdaRY_$'
        CRLF = '\r\n'
        L = []
        for (key, value) in fields:
            L.append('--' + BOUNDARY)
            L.append('Content-Disposition: form-data; name="%s"' % key)
            L.append('')
            L.append(value)
        for (key, filename, content_type, value) in files:
            L.append('--' + BOUNDARY)
            L.append('Content-Disposition: form-data; name="%s"; filename="%s"' % (key, filename))
            L.append('Content-Type: %s' % content_type)
            L.append('')
            L.append(value)
        L.append('--' + BOUNDARY + '--')
        L.append('')
        body = CRLF.join(L)

        self.set_post_params(body + '\r\n\r\n')

    def scheme(self):
        scheme = 'https' if self.headers.is_https() else 'http'
        return scheme + '://'

    def curl(self, host):
        url = self.scheme() + host + str(self.path)
        curl = 'curl %s "%s"' % (self.headers.curl(), url)
        if self.method == 'POST':
            curl += ' -X POST --data "%s"' % (self.data.replace('"', '\\"'))
        curl += ' --compress -g -k -v'
        return curl

    def set_ajax(self):
        assert 'yandexuid' not in self.headers['Cookie']
        self.headers.cookie.set_yandexuid(YANDEXUID)
        self.path.replace_params({'callback': CALLBACK, 'ajax': '{}', 'yu': YANDEXUID})

    # shortcuts for self
    def set_flags(self, *args):
        return self.add_flags(*args)

    def set_flag(self, *args):
        return self.add_flags(*args)

    def add_flag(self, *args):
        return self.add_flags(*args)

    # shortcut for Path
    def set_url(self, *args):
        self.path.set_url(*args)

    def set_params(self, *args):
        self.path.set_params(*args)
        self.update_exp_params_region_from_cgi()

    def add_params(self, *args):
        self.path.add_params(*args)
        self.update_exp_params_region_from_cgi()

    def replace_params(self, *args):
        self.path.replace_params(*args)
        self.update_exp_params_region_from_cgi()

    def remove_params(self, *args):
        self.path.remove_params(*args)

    # shortcut for Headers
    def set_host(self, *args):
        self.headers.set_host(*args)

    def set_region(self, region_id):
        if not region_id:
            region_id = 213
        self.headers.set_region(region_id)
        self.update_exp_params_region(region_id)

    def set_user_agent(self, *args):
        self.headers.set_user_agent(*args)
        self.update_exp_params_user_agent(*args)

    def set_auth(self):
        self.headers.cookie.set_session_id()

    def get_passport(self):
        return PS.properties()

    def set_auth2(self):
        for c in PS.session.cookies:
            self.headers.cookie[c.name] = c.value

    def set_noauth(self):
        self.headers.cookie.set_no_session_id()

    def reset_auth(self):
        self.headers.cookie.reset_session_id()

    def set_internal(self, *args):
        self.headers.set_internal(*args)

    def set_external(self, *args):
        self.headers.set_external(*args)

    def set_https(self, *args):
        self.headers.set_https(*args)

    def set_content_type(self, *args):
        self.headers.set_content_type(*args)

    def set_custom_headers(self, *args):
        self.headers.set_custom_headers(*args)

    def update_exp_params_region_from_cgi(self):
        self.update_exp_params_region(self.path.get_param('lr'))

    def update_exp_params_region(self, region):
        if region is not None and region != self.exp_params["r"]:
            self.exp_params = deepcopy(self.exp_params)
            self.exp_params["r"] = region
            self.headers.set_exp_params(self.exp_params)

    def update_exp_params_user_agent(self, ua):
        ua_type = None
        for ua_it in USERAGENT_BY_TYPE:
            if ua == USERAGENT_BY_TYPE[ua_it]:
                ua_type = ua_it
        if ua_type is not None and ua_type in EXP_PARAMS_BY_TYPE:
            d = EXP_PARAMS_BY_TYPE[ua_type]["d"]
            if d is not None and d != self.exp_params["d"]:
                self.exp_params = deepcopy(self.exp_params)
                self.exp_params["d"] = d
                self.headers.set_exp_params(self.exp_params)

class ExcReportInternalError(Exception):
    pass


class ExcReportInvalidResponse(Exception):
    pass


class ExcReportInvalidContent(Exception):
    pass


class ReportSourceEmpty():
    def __init__(self, name):
        self.name = name

    def __getattr__(self, *args):
        raise AssertionError(repr(self))

    def __getitem__(self, *args):
        raise AssertionError(repr(self))

    def __repr__(self):
        return "No request was made to source " + self.name

    def __nonzero__(self):
        return False


class ReportBackendResponse():
    def __init__(self, name, backend):
        self.name = name
        self.requests = []
        self.count = backend.state.accepted.value
        assert self.count, 'TODO use ReportSourceEmpty class instead'
        reqs = backend.state.requests
        for _ in xrange(reqs.qsize()):
            self.requests.append(ReportSourceResponse(name, reqs.get()))

    def __getattr__(self, *args):
        if args[0] in ['requests', 'name', 'count'] or args[0].startswith('_'):
            return object.__getattr__(self, *args)
        assert self.count == 1, (
            'There was %d requests to "%s"\nHandle it all via "requests" attribute instead of: %s' %
            (self.count, self.name, args[0])
        )
        return getattr(self.requests[0], *args)


class ReportSourceResponse():
    def __init__(self, name, r):
        self.name = name
        self.count = 1

        path = r.request.request_line.path.split('?')
        args = urlparse.parse_qs(path[1]) if len(path) > 1 else {}
        # split container params only
        ParseUpperParams(None, args)

        self.request = r.request
        self.request_raw = repr(r.raw_request)

        self.method = r.request.request_line.method
        self.headers = r.request.headers
        self.path = path[0]
        self.args = args

        data = r.request.data
        content = data.content

        try:
            content = zlib.decompress(content)
        except Exception:
            pass

        if re.search('_yson', self.path):
            import yt.yson
            content = json.dumps(yt.yson.yson_to_json(yt.yson.loads(content)))

        self.content = content

        # parse frequently used sources data
        if name in ('TEMPLATES', 'TEMPLATES_LOWLOAD'):
            self.data = content and validate_json(content) or ReportSourceEmpty(name)
        elif name.startswith('APP_HOST'):
            if not (re.search('_json', self.path) or re.search('_yson', self.path)):
                content = app_host.convert_request_proto(content)

            self.data = content and validate_json(content) or ReportSourceEmpty(name)
            if self.data:
                for x in self.data:
                    if x['name'] == 'NOAPACHE_SETUP':
                        ctx = x['results'][0]
                        self.glob = ctx['global_ctx']
                        self.clients = ctx['client_ctx']
                        assert 'raw_ctx' not in ctx, 'TODO ParseUpperParams, then set self.experiments'
                        # split container params
                        ParseUpperParams(None, self.glob)
                        map(lambda x: ParseUpperParams(*x), self.clients.items())
            else:
                self.glob = ReportSourceEmpty(name)
                self.clients = ReportSourceEmpty(name)
                self.experiments = ReportSourceEmpty(name)
        elif name in ['UPPER', '__APP_HOST']:
            self.data = content and self._parse_upper_contexts() or ReportSourceEmpty(name)
            if self.data:
                assert 'client_ctx' in self.data or 'raw_ctx' in self.data
                if 'global_ctx' in self.data:
                    self.glob = self.data['global_ctx']
                    if 'client_ctx' in self.data:
                        self.clients = self.data['client_ctx']
                    elif 'raw_ctx' in self.data:
                        self.experiments = self.data['raw_ctx']
            else:
                self.glob = ReportSourceEmpty(name)
                self.clients = ReportSourceEmpty(name)
                self.experiments = ReportSourceEmpty(name)
        else:
            self.glob = ReportSourceEmpty('UPPER or APP_HOST')
            self.clients = ReportSourceEmpty('UPPER or APP_HOST')
            self.experiments = ReportSourceEmpty('UPPER or APP_HOST')

    def _parse_upper_contexts(self):
        def _parse_raw_ctx(registry, glob, client):
            assert glob
            name = glob['context_name'][0]
            assert name not in registry

            assert client
            map(lambda x: ParseUpperParams(*x), client.items())
            registry[name] = client

            return registry

        def _get_contexts_app_host(content):
            context = validate_json(content)
            ctx = {}
            ctx['raw_ctx'] = {}
            for x in context:
                if x['name'] == 'NOAPACHE_SETUP':
                    ctx = x['results'][0]
                    if 'global_ctx' in ctx:
                        ParseUpperParams(None, ctx['global_ctx'])
                    if 'client_ctx' in ctx:
                        map(lambda x: ParseUpperParams(*x), ctx['client_ctx'].items())

                    if 'raw_ctx' in ctx:
                        raw_ctx = {}
                        for o in ctx['raw_ctx']:
                            _parse_raw_ctx(raw_ctx, o['global_ctx'], o['client_ctx'])
                        assert raw_ctx
                        ctx['raw_ctx'] = raw_ctx

                    return ctx
                elif x['name'].startswith('NOAPACHE_SETUP-'):
                    o = x['results'][0]
                    _parse_raw_ctx(ctx['raw_ctx'], o['client_ctx']['UPPER'], o['client_ctx'])
                    if 'global_ctx' not in ctx:
                        ctx['global_ctx'] = o['global_ctx']
            return ctx

        def _get_contexts_json(content):
            ctx = {}
            res = {}
            rows = content.split('\n')
            assert rows
            for r in rows:
                parts = r.split('\t')
                assert parts
                if parts[0] == 'raw_ctx':
                    raw_ctx = {}
                    assert len(parts) == 3
                    _parse_raw_ctx(raw_ctx, glob=validate_json(parts[1]), client=validate_json(parts[2]))
                    assert raw_ctx
                    ctx['raw_ctx'] = raw_ctx
                else:
                    assert len(parts) == 2
                    part_ctx = res[parts[0]] = validate_json(parts[1])

                    if parts[0] == 'global_ctx':
                        ctx['global_ctx'] = part_ctx
                        ParseUpperParams(None, ctx['global_ctx'])
                    if parts[0] == 'client_ctx':
                        ctx['client_ctx'] = part_ctx
                        map(lambda x: ParseUpperParams(*x), ctx['client_ctx'].items())

            return ctx

        content, headers = self.content, self.headers

        if (
            re.search('/_ctx/', self.path) and
            not re.search('/_json/', self.path) and
            not re.search('/_yson/', self.path)
        ):
            return _get_contexts_app_host(app_host.convert_request_proto(content))
        elif content.startswith('['):
            return _get_contexts_app_host(content)
        elif len(content) and '\n' in content and '\t' in content:
            assert headers
            assert headers.get_one('Content-Type') == 'application/ya-multi-json'
            return _get_contexts_json(content)
        else:
            raise ValueError('Unsupported UPPER content: ' + content[0:512])


class ReportResponse():
    def __init__(self, response, request, request_str, parent):
        # XXX can't pass fixtures to this helper class due to no direct inheritance in TestClass
        self.schema_dir = parent.schema_dir

        self.request_str = request_str
        self.response_raw = response
        self.ts_start = None
        self.ts_end = None
        self.reqid = request.reqid if isinstance(request, Query) else None

        if isinstance(response, tuple):
            self.ts_start = response[1]
            self.ts_end = response[2]
            response = response[0]

        if response:
            self.status = response.status
            self.code = response.status
            self.headers = response.headers
            self.content = response.data.content
            self.reqid = response.headers.get_one('X-Yandex-ReqId')
            ignore_list = ['x-content-type-options', 'set-cookie', 'expires', 'x-yandex-sts-plus', 'x-yandex-items-count', 'cache-control']
            for hName in response.headers.get_names():
                if hName not in ignore_list:
                    assert response.headers.get_one(hName)
        else:
            self.status = self.code = None
            self.headers = None
            self.content = ''

        self._data = None
        self.sources_raw = {}
        self.sources = {}

    @property
    def source_raw(self):
        if not self.sources_raw:
            return
        assert len(self.sources_raw) == 1, (
            self.request_str + "\n\nThere are many sources requested: " + ', '.join(sorted(self.sources_raw.keys())) +
            "\nUse resp.sources_raw['NAME'] instead"
        )
        return self.sources_raw.values()[0]

    @property
    def source(self):
        if not self.sources:
            return
        assert len(self.sources) == 1, (
            self.request_str + "\n\nThere are many sources requested: " +
            ', '.join(sorted(self.sources.keys())) + "\nUse resp.sources['NAME'] instead"
        )
        return self.sources.values()[0]

    @property
    def data(self):
        if self._data is None:
            self._data = validate_json(self.content)
        return self._data

    @property
    def ajax_data(self, callback=CALLBACK):
        if self._data is None:
            match = re.search(r'^' + callback + r'\((.*)\)$', self.content)
            assert match, "ajax callback removed"
            if match:
                self._data = validate_json(match.group(1))
        return self._data

    def __nonzero__(self):
        return bool(self.response_raw or self.sources)

    def parse_response(self, **kwargs):
        self._parse_sources(kwargs['backends'])
        self._validate_response(**kwargs)
        self._validate_content(**kwargs)

    def _parse_sources(self, backends):
        for name, backend in backends.items():
            self.sources_raw[name] = backend
            self.sources[name] = (
                ReportBackendResponse(name, backend) if backend.state.accepted.value else ReportSourceEmpty(name)
            )

        # autodetect noapache_json_req=app_host
        if '__APP_HOST' in self.sources:
            upper = self.sources['UPPER']
            app_host = self.sources.pop('__APP_HOST')
            if not upper and app_host:
                self.sources['UPPER'] = app_host

    def _validate_response(self, **kwargs):
        if not self.response_raw:
            return

        require_status = kwargs['require_status'] if 'require_status' in kwargs else 200
        if not require_status:
            return

        if self.status != require_status:
            errmsg = self.request_str + "\n\nExpected HTTP status " + str(require_status) + ". Got " + str(self.status)
            if self.status == 302:
                errmsg += "\nLocation: " + str(self.headers.get_one('location'))
            raise AssertionError(errmsg)

        if require_status >= 300 and require_status <= 399 and 'location' not in self.headers:
            raise ExcReportInvalidResponse(self.request_str + "\n\nNo response header 'Location'")

        cookie_names = {}
        for set_cookie in self.headers.get_all('Set-Cookie'):
            for cookie_part in set_cookie.replace(' ', '').split(';'):
                if cookie_part.startswith('domain='):
                    assert cookie_part.startswith('domain=.yandex.')
            name = set_cookie.replace(' ', '').split(';')[0].split('=')[0]
        #    assert name not in cookie_names
            cookie_names[name] = 1
        # assert len(self.headers.get_all('Content-Type')) == 1

    def _validate_content(self, **kwargs):
        if 'validate_content' in kwargs and not kwargs['validate_content']:
            return

        content = self.content
        if content is None or not len(content):
            return

        # TODO sdch validation
        if self.headers.get_one('Content-Encoding') in ['sdch', 'deflate']:
            return

        m = re.search(r'(' + FATAL_ERROR_MARKER + r'.*?)\n', content)
        if m:
            raise ExcReportInternalError(self.request_str + "\n\n" + m.group(1))

        if not kwargs.get('validate_content'):
            return  # TODO remove

        m = RE_BROKEN_CHARS.search(content)
        if m:
            SNIPPET_WIDTH = 512
            broken = m.group(1)
            pos = m.start()
            start = pos - SNIPPET_WIDTH if pos >= SNIPPET_WIDTH else 0
            end = pos + len(broken)
            snippet = self.content[start:pos] + 'HERE->' + broken + '<-HERE' + self.content[end:end + SNIPPET_WIDTH]

            raise ExcReportInvalidContent(
                "%s\n\n%s\nFound invalid symbol: %s at position %d checkout snippet above with markers HERE" %
                (self.request_str, snippet, repr(broken), pos)
            )

    def validate_schema(self, schema, schema_dir=None):
        assert schema is not None, 'No schema'

        if isinstance(schema, basestring):
            filename = schema if schema.startswith('/') else os.path.join(self.schema_dir, schema)
            with open(filename) as f:
                content = f.read()
                schema = validate_json(content)
            schema_dir = os.path.dirname(filename)
        elif isinstance(schema, dict):
            schema = deepcopy(schema)
        else:
            raise ValueError(str(schema)[:512] + '\nInvalid schema')

        if not schema_dir:
            schema_dir = self.schema_dir

        # support relative path "$ref": "file://file.json#something
        assert 'id' not in schema, "Invalid json schema: explicit property 'id' in schema is forbidden!"
        schema['id'] = 'file://' + schema_dir + '/'
        schema['$schema'] = "http://json-schema.org/schema#"

        jsonschema.validate(self.data, schema)

        return True

    def access_log(self):
        return AccessLog().get(self.reqid)

    def reqans_log(self):
        return ReqansLog().get(self.reqid)

    def xmlreqans_log(self):
        return XmlReqansLog().get(self.reqid)

    def profile_log(self):
        return ProfileLog().get(self.reqid)

    def eventlog(self):
        return EventLog().get(self.ts_start, self.ts_end)


class BaseReportTest(object):
    @classmethod
    @pytest.fixture(scope='class', autouse=True)
    def setup(cls, request, local_ip, report_ip, report_port, sock_family, schema_dir, global_sources):
        cls.report_host = request.config.option.host
        cls.additional_flags = None
        if request.config.option.flags:
            cls.additional_flags = json.loads(request.config.option.flags)
        cls.host = report_ip
        cls.port = report_port
        cls.family = sock_family
        cls.RETRIES = 3
        cls.MAX_RETRIES = 10
        cls.RETRY_TESTS_COUNT = 0
        cls.schema_dir = schema_dir
        cls.sources = global_sources

        hostname = socket.getfqdn()
        if '.yandex.' in hostname or '.yndx.' in hostname:
            cls.curr_host = hostname
        else:
            assert local_ip, 'No local_ip for hostname: ' + hostname
            cls.curr_host = 'localhost'

    @classmethod
    @pytest.fixture(scope='function')
    def query(cls):
        return Query(cls.host, cls.port, cls.additional_flags)

    @pytest.fixture(scope='function', autouse=True)
    def __setup_fs_manager(self, fs_manager):
        self.__fs_manager = fs_manager

    @property
    def fs(self):
        return self.__fs_manager

    @classmethod
    @pytest.fixture(scope='class', autouse=True)
    def __setup_connection_manager(cls, session_connection_manager):
        cls.__connection_manager = session_connection_manager

    @pytest.fixture(scope='function', autouse=True)
    def __setup_backend_manager(self, backend_manager):
        self.__backend_manager = backend_manager

    def log(self, filename, data):
        resp_file = self.fs.create_file(filename)
        with open(resp_file, 'wb') as f:
            f.write(str(data))

    def start_backend(self, config, port):
        return self.__backend_manager.start(config, port, self.family)

    def app_host_request(self, query, **kwargs):
        sources = kwargs.pop('sources', None) or kwargs.pop('source', None)
        kwargs['sources'] = sources or ['APP_HOST']
        query.add_flags({
            'app_host': 'web',
            'app_host_srcskip': 'APP_HOST=0',
            'app_host_source': '=APP_HOST',
            'noapache_json_req': 1,
            'noapache_json_res': 1,
            "srcrwr": {
                "TEMPLATE_RENDERER": "RENDERER_WEBEXP",
                "TEMPLATE-POST_SEARCH-LOWLOAD": "RENDERER_WEBEXP",
                "TEMPLATE-PRE_SEARCH-LOWLOAD": "RENDERER_WEBEXP"
            }
        })
        return self.request(query, **kwargs)

    def start_source(self, config, port):
        return self.start_backend(config, port=port)

    def send_query(self, query, **kwargs):
        if isinstance(query, basestring):
            assert kwargs.get('host') and kwargs.get('port'), (
                'Host and port is required while query is raw string: ' + query
            )

        host = kwargs.get('host') or query.target_host
        port = kwargs.get('port') or query.target_port

        do_log = kwargs['debug'] if 'debug' in kwargs else True  # enable by default
        marker = "{datetime}_{host}_{port}".format(
            datetime=datetime.datetime.now().strftime('%Y.%m.%d-%H.%M.%S.%f'),
            host=host,
            port=port,
        )
        if do_log:
            self.log(
                filename="request_{marker}.txt".format(marker=marker),
                data=query,
            )
        if os.environ.get('CURL') == '1' :
            D({"CURL": str(query)})
        conn = self.__connection_manager.create(host=host, port=port, newline=['\r\n', '\n'], timeout=30)
        stream = conn.create_stream()

        ts_start = int(time.time() * 1000000)
        ts_end = None

        try:
            stream.write(str(query))  # implicit QUERY usage
            if kwargs.get('require_response', True):
                resp = stream.read_response()
                ts_end = int(time.time() * 1000000)
                if do_log:
                    self.log(filename="response_{marker}.txt".format(marker=marker), data=resp)
                return (resp, ts_start, ts_end)
        except HTTPReaderException, e:
            raise Exception(marker + ': ' + str(e))
        finally:
            conn.close()

    def request(self, query, **kwargs):
        for o in kwargs.keys():
            assert o in [
                'host', 'port',       # куда отправлять запрос Репорту
                'source', 'sources',  # подслушать запросы в Репортные источники / подсунуть в Репорт mock-контент
                'success_on',         # callback для определения успешности запросы
                'require_response',   # требуем непустой контент от Репорта
                'require_status',     # требуем HTTP-код ответа
                'validate_content',   # валидировать контент от Репорта общимим проверками
                'debug',              # режим отладки запросов (применяется в sandbox в основном)
            ]

        sources = kwargs.pop('source', None) or kwargs.pop('sources', None) or []
        backends = {}

        if sources:
            def _start_backend(source, cfg, query):
                assert source in ['UPPER', 'INFO_REQUEST'] or source in self.sources, (
                    "\nrequest.json file sources:\n" + ", ".join(sorted(self.sources.keys())) +
                    "\nUnknown source: " + source
                )
                assert source not in backends, "Duplicated source '%s'" % (source)

                if isinstance(cfg, HTTPConfig):
                    config = cfg
                elif isinstance(cfg, basestring):
                    config = SimpleConfig(ok(data=cfg))
                    if source.startswith('APP_HOST'):
                        config.HANDLER_TYPE = app_host.AppHostHTTPHandler
                elif cfg is None:
                    config = SimpleConfig()
                else:
                    raise ValueError('TODO implement cfg class: ' + str(cfg.__class__))

                port = GenerateBackendPort(source)
                backend = self.start_backend(config, port)
                query.add_flags({
                    'srcrwr': {
                        source: '{host}:{port}'.format(host=urllib.quote('localhost'), port=str(port))
                    }
                })

                if source == 'WIZARD':
                    query.set_flags({
                        'app_host_srcskip': 'WIZARD=0',
                        'app_host_source': '=APP_HOST',
                    })

                return backend

            assert not kwargs.get('host'), "Can't sniff source request on remote host: " + str(kwargs.get('host'))

            if not isinstance(sources, list):
                sources = [sources]

            query = deepcopy(query)
            _response_cfg = {}  # autodetect noapache_json_req=app_host
            for src in sources:
                if isinstance(src, basestring):
                    source, cfg = src, None
                else:
                    source, cfg = src

                assert source not in _response_cfg, 'Duplicated source backend: ' + source
                assert source != 'APP_HOST' or 'UPPER' not in backends, (
                    "You should't mock both UPPER and APP_HOST. Rewrite the test please using source='UPPER' only"
                )
                assert source != 'UPPER' or 'APP_HOST' not in backends, (
                    "You should't mock both UPPER and APP_HOST. Rewrite the test please using source='UPPER' only"
                )

                backends[source] = _start_backend(source, cfg, query)
                _response_cfg[source] = cfg  # autodetect noapache_json_req=app_host

            # autodetect noapache_json_req=app_host
            if 'UPPER' in backends and 'APP_HOST' not in backends:
                backends['__APP_HOST'] = _start_backend('APP_HOST', _response_cfg['UPPER'], query)

        beta_host = os.environ.get('BETA_HOST')
        if beta_host:
            logging.info("Set host/port from BETA_HOST %s:80", beta_host)
            kwargs['host'] = beta_host
            kwargs['port'] = 80
            query.set_beta_domain(beta_host)

        # MAIN CALL
        _response = self._request(query, **kwargs)
        # MAIN CALL

        resp = ReportResponse(_response, request=query, request_str=self.curl(query), parent=self)

        resp.parse_response(backends=backends, **kwargs)

        for b in backends.values():
            b.stop()

        return resp

    def _request(self, query, **kwargs):
        success_on = kwargs.get('success_on', None)
        cls = self.__class__

        if cls.RETRY_TESTS_COUNT < cls.MAX_RETRIES and self.RETRIES > 1:
            logging.info(
                "MULTIPLE RTC: %s MR: %s R: %s QUERY: %s",
                cls.RETRY_TESTS_COUNT, cls.MAX_RETRIES, cls.RETRIES, query,
            )
            for _ in range(self.RETRIES):
                try:
                    out = self.send_query(query, debug=True, **kwargs)
                    if success_on:
                        success_on(out[0])
                    if out:
                        return out
                except HTTPReaderException, e:
                    msg = "Test request error %s. Retring" % str(e)
                    print msg
                    logging.warning(msg)
                    time.sleep(0.5)
                except AssertionError as e:
                    msg = 'RETRY[#%s] %s' % (_, str(e))
                    print msg
                    logging.warning(msg)
                    time.sleep(5)
            cls.RETRY_TESTS_COUNT += 1

        logging.info("RTC: %s MR: %s R: %s QUERY: %s", cls.RETRY_TESTS_COUNT, cls.MAX_RETRIES, cls.RETRIES, query)

        out = self.send_query(query, **kwargs)
        if success_on:
            success_on(out[0])
        return out


    def decode_raw_by_path(self, b64_raw, message, type):
        raw = base64.b64decode(b64_raw)
        message.ParseFromString(raw)
        d = protobuf_to_dict(message)
        if (type == "yabs_setup"):
            # This is hack. we need change code in the banner setup, it must encode symbol ; SEARCH-10008
            d['Path'] = re.sub(';', '%3B', d['Path'])
        # RUNTIMETESTS-11
        # fix only yabs
        if (type == "yabs_setup"):
            # urlparse.parse_qs(u'text=bayrak+dire%C4%9Fi') -> {u'text': [u'bayrak dire\xc4\x9fi']} it is wrong
            # we expect {u'text': [u'bayrak dire\u011fi']}
            data = urlparse.parse_qs(d['Path'].encode("ASCII"))
            for k in data:
                for i in range(len(data[k])):
                    data[k][i] = data[k][i].decode("utf-8")
        else:
            data = urlparse.parse_qs(d['Path'])
        data['type'] = type
        return data


    def decode_binary_ctx(self, b64, type):
        raw = base64.b64decode(b64)
        assert(raw.startswith(bytes('p_')))
        indx = raw.find('?') + 1
        if indx == 0:
            indx = 9
        raw = raw[indx:]

        result = {}
        for pair in raw.strip('&').split('&'):
            kv = pair.split('=', 2)
            if len(kv) == 1:
                result[kv[0]] = 1
            else:
                val = urllib.unquote_plus(kv[1])
                if val.startswith('{') or val.startswith('[{'):
                    result[kv[0]] = json.loads(val)
                else:
                    result[kv[0]] = val.decode('utf-8', 'ignore')
        result['type'] = type
        return result


    def json_dump_ctxs(self, query):
        if not os.environ.get('REPORT_INVERTED') or os.environ.get('REPORT_INVERTED') != '1':
            return self.json_dump_ctxs_old(query)
        def explore_ctxs(path, tree):
            if isinstance(tree,list):
                tree = {'response': tree}
            ctxs = {}
            for source in tree.keys():
                if source == 'response':
                    for ctx in tree[source]:
                        if '__content_type' in ctx and ctx['__content_type'] == 'yson' and 'binary' in ctx:
                            ctx = ctx['binary']
                        if '__content_type' in ctx and ctx['__content_type'] == 'protobuf' and 'binary' in ctx:
                            if 'type' in ctx and ctx['type'] == 'yabs_setup':
                                ctx = self.decode_raw_by_path(ctx['binary'], THttpRequest(), ctx['type'])
                        if ("type" in ctx) and ctx['type']:
                            type = ctx['type']
                            val = self.decode_binary_ctx(ctx["__binary__"], type) if "__binary__" in ctx else ctx
                            if type != 'experiments' or val['contexts'] is not None:
                                if type not in ctxs:
                                    ctxs[type] = [val]
                                else:
                                    ctxs[type].append(val)
                else:
                    for (type, val) in explore_ctxs('{}/{}'.format(path, source), tree[source]).items():
                        if type not in ctxs:
                            ctxs[type] = val
                        else:
                            for item in val:
                                ctxs[type].append(item)
            return ctxs

        resp = self.json_dump_responses(query)
        ctxs = explore_ctxs('ROOT', resp)
        return ctxs


    def json_dump_ctxs_old(self, query):
        resp = self.json_dump_request(query, 'search.app_host.sources')
        ctxs = {}
        for source in resp:
            if source["name"] and source["name"] not in ["FILTER-APP_HOST_PRE"]:
                for ctx in source["results"]:
                    if ("type" in ctx) and ctx['type']:
                        type = ctx['type']
                        if '__content_type' in ctx and ctx['__content_type'] == 'protobuf' and 'binary' in ctx:
                            if 'type' in ctx and ctx['type'] == 'yabs_setup':
                                ctx = self.decode_raw_by_path(ctx['binary'], THttpRequest(), ctx['type'])
                        val = self.decode_binary_ctx(ctx["__binary__"], type) if "__binary__" in ctx else ctx
                        if type not in ctxs:
                            ctxs[type] = [val]
                        else:
                            ctxs[type].append(val)

        return ctxs

    def json_dump_context(self, query, type_filter=[]):
        if not os.environ.get('REPORT_INVERTED') or os.environ.get('REPORT_INVERTED') != '1':
            return self.json_dump_context_old(query, type_filter)
        ctxs = self.json_dump_ctxs(query)
        result = list()
        for flt in type_filter:
            if flt in ctxs:
                for ctx in ctxs[flt]:
                    result.append(ctx)
        return result

    def json_dump_context_old(self, query, type_filter=[]):
        resp = self.json_dump_request(query, 'search.app_host.sources')

        ctxs = list()
        for source in resp:
            for ctx in source["results"]:
                if "type" in ctx:
                    if ctx['type'] == 'yabs_setup':
                        ctx = self.decode_raw_by_path(ctx['__binary__'], THttpRequest(), ctx['type'])
                    if "__binary__" in ctx:
                        ctxs.append(self.decode_binary_ctx(ctx["__binary__"], ctx['type']))
                    else:
                        ctxs.append(ctx)

        result = list()
        for ctx in ctxs:
            if ((len(type_filter) == 0) or (ctx["type"] in type_filter)):
                result.append(ctx)

        return result

    def json_dump_responses(self, query, **kwargs):
        query = deepcopy(query)
        query.replace_params({'json_dump_responses': ['INIT','SETUPS','WEB_GLOBAL_SETUP','WEB_SRC_SETUP', 'NOAPACHE_ADJUSTER', 'NOAPACHE_CTX', 'YABS_SETUP', 'FLAGS_MERGER', 'BLENDER', 'BUILD_REQUEST', 'REWRITE_CONTEXT', 'INIT1', 'INIT2']})
        query.set_internal()
        resp = self.request(query, **kwargs)
        return resp.data

    def json_dump_request(self, query, dump_key, **kwargs):
        query = deepcopy(query)
        query.replace_params({'json_dump': dump_key})
        query.set_internal()
        resp = self.request(query, **kwargs)
        assert dump_key in resp.data
        return resp.data[dump_key]

    def json_request(self, query, **kwargs):
        query = deepcopy(query)
        query.replace_params({'export': 'json'})
        query.set_internal()
        ireq = kwargs.pop('SetXYandexInternalRequestHeader', None)
        if ireq is not None:
            query.set_custom_headers({'X-Yandex-Internal-Request': ireq})
        return self.request(query, **kwargs)

    def gateway_request(self, query, **kwargs):
        query = deepcopy(query)
        query.replace_params({'rpt': 'gateway'})
        query.set_internal()
        return self.request(query, **kwargs)

    def get_param_from_clt_ctx(self, clt_ctx, param):
        if clt_ctx is None:
            return []
        res = []
        for source in clt_ctx:
            if clt_ctx[source] is None:
                continue
            if param in clt_ctx[source]:
                res.append((source, ';'.join(clt_ctx[source][param]).split(';')))
        return res

    def get_docs(self, resp):
        return filter(lambda x: 'WizardPos' not in ''.join(x['_markers']), resp.data[u'searchdata']['docs'])

    def with_noapache(self, query, **kwargs):
        resp = self.request(query, source='UPPER')
        return resp.source.data, resp

    def source_params(self, query, client, param, **kwargs):
        if client == 'YABS':
            yabs_setup = self.json_dump_context(query, ['yabs_setup'])
            assert len(yabs_setup) > 0
            params = yabs_setup[0][param]
            return isinstance(params, list) and params or [params]
        else:
            noapache_setup = self.get_noapache_setup(query)
            params = noapache_setup['client_ctx'][client][param]
            return isinstance(params, list) and params or [params]

    def source_param(self, *args):
        return self.source_params(*args)[0]

    def source_asked_in_noapache_setup(self, query, source):
        return source in self.asked_sources_in_noapache_setup(query)

    def source_asked(self, query, source):
        return source in self.asked_sources(query)

    def asked_sources_in_noapache_setup(self, query):
        noapache_setup = self.get_noapache_setup(query)
        return noapache_setup['client_ctx'].keys()

    def asked_sources(self, query):
        ctx, _ = self.with_noapache(query)
        return ctx['client_ctx'].keys()

    def validate_json_scheme(self, data, scheme):
        # просто сравнивает json по схеме
        # все сравнения по схеме делать через эту функцию
        # все "хаки" для схем - тут

        # support relative path "$ref": "file://file.json#something
        assert 'id' not in scheme, "Invalid json schema: explicit property 'id' in schema is forbidden!"
        scheme['id'] = 'file://' + self.schema_dir + '/'
        scheme['$schema'] = "http://json-schema.org/schema#"

        data = data.data if isinstance(data, ReportResponse) else data
        jsonschema.validate(data, scheme)

        return True

    def json_test(self, query, **kwargs):
        # json тест
        # основа для всех тестов выдачи по схеме!
        # проверяем выдачу на "общую" схему выдачи
        resp = self.json_request(query, **kwargs)
        resp.validate_schema('serp.json')
        return resp

    def validate_json_data_by_context_type(self, data, schema_path_to_contexts):
        for item in data:
            for ctx in item['results']:
                assert 'type' in ctx
                schema_path_to_context = os.path.join(schema_path_to_contexts, ctx['type'] + '.json')
                self.validate_context(ctx, schema_path_to_context)

    def validate_context(self, ctx, schema_path_to_context):
        if os.path.isfile(schema_path_to_context):
            with open(schema_path_to_context) as f:
                content = f.read()
                scheme = validate_json(content)
                self.validate_json_scheme(ctx, scheme)

    def validate_json_data(self, data, json_scheme_file):
        # вспомогательная функция
        # сравниваем что-то по json схеме
        with open(json_scheme_file) as f:
            content = f.read()
            scheme = validate_json(content)
            self.validate_json_scheme(data, scheme)

    def validate_json_sources(self, data_ctx, json_scheme_file):
        # вспомогательная функция для сравнения запросов в источники по схеме
        with open(json_scheme_file) as f:
            content = f.read()
            scheme = validate_json(content)
            keys = scheme.keys()
            assert keys
            for k in keys:
                self.validate_json_scheme(data_ctx[k], scheme[k])

    def curl(self, query):
        if query is None:
            return ''
        elif isinstance(query, Query):
            return query.curl(self.report_host)
        elif isinstance(query, basestring):
            return 'RAW REQUEST: ' + str(query)[:512]
        else:
            raise ValueError("TODO implement curl representation for: " + str(query.__class__) + "\n\n" + str(query))

    def is_blender_reqans_log(self, query):
        flags = self.json_dump_context(query, ['flags'])

        if len(flags)>0 and flags[0].get("all") and flags[0]["all"].get("blender_reqans_log"):
            return True

        return False

    def get_context_from_apphost_eventlog(self, content, toggle, source_name, raw=False):
        filter_name = ''
        if toggle == "request":
            filter_name = 'TSourceRequest'
        else:
            filter_name = 'TSourceResponse'

        prog = re.compile(r'\\t{}\\t{}\\t(.+)",?$'.format(filter_name, source_name))
        for line in (content.splitlines()):
            m = prog.search(line)
            if m:
                if raw:
                    return m.group(1)
                else:
                    return json.loads(json.loads('"'+m.group(1)+'"'))

        return None

    def get_apphost_type(self, source_data, ttype):
        type_list = filter(lambda x: x["type"]==ttype, source_data["answers"])
        for i in range(len(type_list)):
            if type_list[i].get("binary"):
                type_list[i] = type_list[i]["binary"]
        return type_list

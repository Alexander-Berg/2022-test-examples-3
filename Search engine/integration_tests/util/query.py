# -*- coding: utf-8 -*-

import time
from util.const import TLD, USER_AGENT, TEXT, SEARCH, URL_BY_TYPE, USERAGENT_BY_TYPE, EXP_PARAMS_BY_TYPE, YANDEXUID, COOKIE_NOAUTH
from util.const import CALLBACK, YP_EXPIRE, EXTERNAL_IP
from util.helpers import AddResponseHelpers, SoyEncryptor
from util.params import test_id, exp_confs, soy_mode
from requests import Request, Session
from requests.exceptions import ConnectionError, Timeout, HTTPError
from base64 import b64encode
from urllib.parse import quote, unquote
from six import string_types
import logging
import json
import re
import sys
import copy
import shlex


class YP(dict):
    def __init__(self):
        pass

    def set_sp(self, value):
        if 'sp' in self:
            self['sp'] = self['sp'] + value
        else:
            self['sp'] = value

    def set_los(self, value):
        self['los'] = value

    def set_lost(self, value):
        self['lost'] = value

    def set_cr(self, value):
        self['cr'] = value

    def set_sz(self, value):
        self['sz'] = value

    def set_szm(self, value):
        self['szm'] = value

    def set_ygu(self, value):
        self['ygu'] = value

    def set_yu(self, value):
        self['yu'] = value

    def set_gpauto(self, lat, lon, precision='100', device='1', timestamp=int(time.time())):
        self['gpauto'] = ':'.join((
            lat.replace('.', '_'),
            lon.replace('.', '_'),
            str(precision),
            str(device),
            str(timestamp),
        ))

    def set_sdrlv(self, value):
        self['sdrlv'] = value

    def __str__(self):
        s = ''
        for k in self:
            s = ''.join((s, YP_EXPIRE, '.', k, '.', quote(str(self[k])), '#'))
        return s[:-1]

    def parse(self, string):
        c = {}
        for subcookie in string.split('#'):
            (time_, name_, value_) = subcookie.split('.', 2)
            c[name_] = unquote(value_)
        return c


class Query():
    se = SoyEncryptor()
    qid_sequence = 0

    def __init__(self, domain='yandex.{}'.format(TLD.RU), beta_host=None, blackbox=None, scheme='https', port=None):
        self.domain = domain        # Domain
        self.beta_host = beta_host  # Beta host
        self.port = port            # Port
        self.method = 'GET'         # HTTP request method
        self.scheme = scheme        # HTTP scheme
        self.path = SEARCH          # HTTP path
        self.data = None
        self.json = None            # A JSON serializable Python object to send in the body of the Request.
        self.blackbox = blackbox
        self.params = {'text': TEXT, 'reqinfo': 'RUNTIMETESTS-integration-tests'}
        if soy_mode():
            self.params['flag'] = 'restriction_profile=weak_consistency__web__desktop__hamster__tier0_tier1'
        self.headers = {}
        self.rwr_headers = {}
        self.cookies = {}
        self.content_type = None
        self.charset = None
        self.yp = YP()
        self.flags = {}
        self.SetUserAgent(USER_AGENT.DESKTOP)
        self.SetHeaders({'Accept-Encoding': 'identity'})
        self.timeout = 13
        self.json_dump_requests = []
        self.json_dump_responses = []
        self.require_status = [200]
        self.retry = 13
        self.response = None
        self.error_response = None
        self.error = None
        self.query_id = Query.qid_sequence
        self._can_soy_mode = True
        Query.qid_sequence = Query.qid_sequence + 1

    @property
    def ReqYpCookie(self):
        return self.yp

    # query.SetRequireStatus(require_status=[200])
    def SetRequireStatus(self, require_status=[]):
        self.require_status = require_status if isinstance(require_status, list) else [require_status]

    def SetTimeout(self, timeout):
        self.timeout = timeout

    def SetPort(self, port):
        self.port = port

    def SetMethod(self, method):
        self.method = method

    def SetScheme(self, scheme):
        self.scheme = scheme

    def SetPath(self, path):
        self.path = path

    def SetDomain(self, domain):
        if not domain:
            domain = TLD.RU
        if domain in TLD.YA_TLD:
            domain = 'yandex.' + domain
        self.domain = domain

    def SetData(self, data):
        if isinstance(data, string_types):
            self.data = data.encode('utf-8')
        else:
            self.data = data

    def SetCharset(self, charset='utf-8'):
        self.charset = charset

    def SetContentType(self, content_type='text/plain', charset='utf-8'):
        self.content_type = content_type
        self.charset = charset

    def SetJson(self, json):
        self.json = json

    def SetBlackBox(self, blackbox):
        self.blackbox = copy.deepcopy(blackbox)

    # params is [(key, val),..]
    def AddParams(self, params=[]):
        for (k, v) in params.items():
            if isinstance(v, int):
                v = str(v)
            if k in self.params:
                val = self.params[k]
                del self.params[k]
                self.params[k] = [val, v]
            else:
                self.params[k] = v

    # params is {key: val,..}
    def SetParams(self, params):
        for (k, v) in params.items():
            if v is None and k in self.params:
                del self.params[k]
            else:
                if isinstance(v, int):
                    v = str(v)
                if k in self.params:
                    del self.params[k]
                self.params[k] = v

    def ResetParams(self):
        self.params = {}

    # params is [key,..]
    def RmParams(self, params):
        for key in params:
            for pLine in self.params:
                if pLine[0] == key:
                    self.params.remove(pLine)
                    break

    @staticmethod
    def _UpdateHeaders(cur_headers, new_headers):
        for (key, val) in list(new_headers.items()):
            lKey = key.lower()
            for sKey in cur_headers.keys():
                if lKey == sKey.lower():
                    cur_headers.pop(sKey, None)
                    break
            if val is None:
                new_headers.pop(key, None)

    # headers is {key: val,..}
    def SetHeaders(self, headers):
        Query._UpdateHeaders(self.headers, headers)
        self.headers.update(headers)

    # headers is {key: val,..}
    # these will go only inside X-Yandex-Headers-Rwr header
    def SetRwrHeaders(self, headers):
        Query._UpdateHeaders(self.rwr_headers, headers)
        self.rwr_headers.update(headers)

    # flags is {key: val,..}
    def SetFlags(self, flags):
        for (lKey, val) in list(flags.items()):
            for sKey in self.flags.keys():
                if lKey == sKey:
                    self.flags.pop(sKey, None)
                    break
            if val is None:
                flags.pop(lKey, None)
        self.flags.update(flags)

    # cookies is {key: val,..}
    def SetCookies(self, cookies):
        for (lKey, val) in list(cookies.items()):
            for sKey in self.cookies.keys():
                if lKey == sKey:
                    self.cookies.pop(sKey, None)
                    break
            if val is None:
                cookies.pop(lKey, None)
        for (lKey, val) in list(cookies.items()):
            if isinstance(val, int):
                val = str(val)
            self.cookies.update({lKey: val})

    def SetQueryId(self, id=None):
        self.query_id = id

    # Makes URL without QUERY_STRING
    def MakeFormAction(self):
        if self.port is None:
            return "{scheme}://{domain}{path}".format(
                scheme=self.scheme,
                domain=self.beta_host if self.beta_host is not None else self.domain,
                path=self.path)
        else:
            return "{scheme}://{domain}:{port}{path}".format(
                scheme=self.scheme,
                domain=self.beta_host if self.beta_host is not None else self.domain,
                port=self.port,
                path=self.path)

    def PrepareRequest(self, need_encrypt_session_cookies=None):
        if len(self.json_dump_requests) > 0:
            Query.SetParams(self, {'json_dump_requests': self.json_dump_requests})
        if len(self.json_dump_responses) > 0:
            Query.SetParams(self, {'json_dump_responses': self.json_dump_responses})
        yp = str(self.ReqYpCookie)
        if len(yp) > 0:
            self.SetCookies({'yp': yp})

        self.SetHeaders({'Host': self.domain})

        if self.scheme == 'https':
            self.SetHeaders({'X-Yandex-HTTPS': 'yes'})
        else:
            self.SetHeaders({'X-Yandex-HTTPS': None})

        self.SetHeaders({'X-Yandex-Internal-Flags': b64encode(json.dumps(self.flags).encode()).decode()})

        if test_id() is not None:
            self.SetParams({
                'no-tests': None,
                'test-id': test_id(),
                'exp_confs': exp_confs()
            })
        else:
            self.SetParams({
                'no-tests': 'da'
            })

        if not('timeout' in self.params):
            self.SetParams({'timeout': 999999})
        if not('waitall' in self.params):
            self.SetParams({'waitall': 999999})

        self.headers.pop('X-Yandex-Headers-Rwr', None)
        hdrs = {}

        def prepare_rwr_headers(headers, skip=set()):
            for key in headers.keys():
                if key.lower() in skip:
                    continue
                if isinstance(headers[key], int):
                    headers[key] = str(headers[key])
                if key in hdrs:
                    hdrs[key].append(headers[key])
                else:
                    hdrs[key] = [headers[key]]

        prepare_rwr_headers(self.headers, {'user-agent'})
        prepare_rwr_headers(self.rwr_headers)

        self.SetHeaders({'X-Yandex-Headers-Rwr': b64encode(json.dumps(hdrs).encode()).decode()})

        # Установка BETA_HOST. Нужно установить тот же TLD и субдомен, что и у домена
        if self.beta_host is not None:
            m = re.search(r'^(.*?)(?:yandex\.)(.+)$', self.domain)
            sub = m.group(1).lower()
            tld = m.group(2).lower() if m else TLD.RU
            beta_host = self.beta_host
            m2 = re.search(r'^(.*?yandex\.)(.+)$', beta_host)
            if m2:
                beta_host = '{sub}{dn}{tld}'.format(sub=sub, dn=m2.group(1).lower(), tld=tld)
            self.SetHeaders({'Host': beta_host})

        if self.content_type is not None:
            self.SetHeaders({
                'Content-Type': self.content_type if self.charset is None else '{}; {}'.format(self.content_type, self.charset)
            })

        req = Request(
            self.method,
            self.MakeFormAction(),
            params=self.params,
            data=self.data,
            json=self.json,
            headers=self.headers
        )
        ses = self.MakeSession(need_encrypt_session_cookies=need_encrypt_session_cookies)

        # кука yp выставляется паспортом и сервисом по выставлению куки релевантности
        # если не удалить то отправятся две куки, а значение берется из какой-то одной
        for domain in ses.cookies.list_domains():
            for path in ses.cookies.list_paths():
                c = ses.cookies.get_dict(domain=domain, path=path)
                if 'yp' in c:
                    ses.cookies.clear(domain=domain, path=path, name='yp')

        ses.cookies.update(self.cookies)
        pReq = ses.prepare_request(req)
        # TODO (kozunov) is it really need?
        pReq.prepare_cookies(self.cookies)

        return pReq

    def DumpHttp(self):
        req = self.PrepareRequest()
        return "{RLINE}\r\n{HEADERS}\r\n\r\n{BODY}".format(
            RLINE=req.method + ' ' + req.url,
            HEADERS='\r\n'.join('{}: {}'.format(k, v) for k, v in req.headers.items()),
            BODY=req.body if req.method == 'POST' else '')

    def Dump(self):
        req = self.PrepareRequest()
        encoding = 'ascii'
        if 'Content-Type' in req.headers and '; ' in req.headers['Content-Type']:
            encoding = req.headers['Content-Type'].split('; ')[1]
        return "curl -X {method} -H {headers}{data} {url}".format(
            method=req.method,
            headers=" -H ".join(shlex.quote('{}: {}'.format(k, v)) for k, v in req.headers.items()),
            data=" --data {}".format(shlex.quote(req.body.decode(encoding))) if req.method == 'POST' and req.body is not None else '',
            url=shlex.quote(req.url)
        )

    def PrintDump(self):
        logging.debug('QUERY: {}'.format(self.Dump()))

    def MakeSession(self, need_encrypt_session_cookies=None):
        session = copy.deepcopy(self.blackbox.GetSession()) if self.blackbox is not None else Session()

        if need_encrypt_session_cookies is True:
            for domain in session.cookies.list_domains():
                if 'passport' not in domain:
                    for path in session.cookies.list_paths():
                        for (key, val) in session.cookies.get_dict(domain=domain, path=path).items():
                            if key in ['Session_id', 'i', 'L', 'fuid01', 'my', 'sessionid2', 'Secure_session_id']:
                                session.cookies.set(key, self.se.Encrypt(val), domain=domain, path=path)
        return session

    def IsSuccess(self):
        if self.response is None:
            logging.debug('reqid={} not used retries {}: Response is None'.format(self.GetQueryId(), self.retry))
            return False
        elif self.response.status_code == 200 and len(self.response.content) == 0:
            logging.debug('reqid={} not used retries {}: Status code is 200 but response content is empty'.format(self.GetQueryId(), self.retry))
            return False
        elif '/xml' in self.response.headers.get('content-type', '') and \
             (self.response.text is not None) and '<error code="55">' in self.response.text:
            # TODO (kozunov) I think this code need delete
            logging.debug('reqid={} not used retries {}: XML RPS limiter'.format(self.GetQueryId(), self.retry))
            return False
        elif len(self.require_status) > 0 and self.response.status_code in self.require_status:
            return True
        # TODO (kozunov) it's strange that for http 500 we do retry.
        elif self.response.status_code not in [429, 500]:
            return True
        return False

    def SendRequest(self, need_encrypt_session_cookies=None):
        success = False
        try:
            self.response = self.MakeSession(need_encrypt_session_cookies=need_encrypt_session_cookies).send(
                self.PrepareRequest(),
                allow_redirects=False,
                timeout=self.timeout
            )
            success = self.IsSuccess()

        except ConnectionError as e:
            logging.debug('reqid={} not used retries {}: ConnectionError: {}'.format(self.GetQueryId(), self.retry, e))
            time.sleep(1)
        except Timeout as e:
            logging.debug('reqid={} not used retries {}: Timeout: {}'.format(self.GetQueryId(), self.retry, e))
            time.sleep(1)
        except HTTPError as e:
            logging.debug('reqid={} not used retries {}: HTTPError: {}'.format(self.GetQueryId(), self.retry, e))
            time.sleep(13)
        except:
            exc_tuple = sys.exc_info()
            logging.debug("reqid={} not used retries {}: Unexpected error: {} {} - {}".format(self.GetQueryId(), self.retry, exc_tuple[0], exc_tuple[1], self.Dump()))
            time.sleep(1)
        self.retry = self.retry - 1
        if self.retry <= 0:
            success = True
        return success

    def GetRequireStatus(self):
        return self.require_status

    def GetResponse(self, require_status=None):
        if self.response is None:
            # this may be happens:
            # 1) we really don't received answer
            # 2) SoY map_operation_type=scraper(validation enabled)
            #    2.1) some source is broken and SoY can not get it
            #         * for xml query in the SoY error_table we received:
            #             <error code="32">Лимит запросов исчерпан у пользователя
            #             <error code="55">Количество запросов, отправленных в течение секунды (RPS), превысило допустимое значение
            # 3) we can not find response in the SoY tables. Neither in the error_table nor in the output_table
            assert False, 'Response not found'
        status = [require_status] if require_status is not None else self.require_status
        location = ''
        if self.response.status_code >=300 and self.response.status_code <= 399:
            location = ' Location: {}'.format(self.response.headers.get("location"))
        assert len(status) > 0 and self.response.status_code in status, "Returned status code: {} but required {} status code.{}".format(self.response.status_code, status, location)

        return AddResponseHelpers(self.response)

    def GetPassport(self):
        if self.blackbox is not None:
            return self.blackbox.GetProfile()
        return {}

    def GetQueryId(self):
        return self.query_id

    def CanSoyMode(self, mode=None):
        rv = self._can_soy_mode
        if mode is not None:
            self._can_soy_mode = mode

        return rv
        # return True if set(self.GetRequireStatus()) & set([200]) else False


class WebQuery(Query):
    def __init__(self, *args, **kwargs):
        super(WebQuery, self).__init__(*args, **kwargs)

    def SetMode(self, external=None):
        if external is not None:
            if external:
                self.SetExternal()
            else:
                self.SetInternal()

    def SetQueryType(self, platform):
        # if platform in [XML]:
        #     self.set_internal()
        # if platform in [GATEWAY_TOUCH, GATEWAY]:
        #     self.replace_params({'rpt': 'gateway'})
        #     self.set_internal()
        # if platform == GATEWAY_TOUCH:
        #     self.replace_params({'banner_ua': USER_AGENT.TOUCH})
        #     self.set_internal()
        self.SetPath(URL_BY_TYPE[platform])
        self.SetHeaders({
            'User-Agent': USERAGENT_BY_TYPE[platform],
        })
        self.SetRwrHeaders({
            'X-Yandex-ExpSplitParams': b64encode(json.dumps(EXP_PARAMS_BY_TYPE[platform]).encode()).decode()
        })

    def SetUserAgent(self, user_agent):
        self.SetHeaders({'User-Agent': user_agent})

    def SetLr(self, lr):
        self.SetParams({'lr': lr})

    def SetRegion(self, region_id, rwr_only=True):
        if not region_id:
            region_id = 213
        headers = {
            "X-LaaS-Answered": "1",
            'X-Region-City-Id': region_id
        }
        if rwr_only:
            self.SetRwrHeaders(headers)
        else:
            self.SetHeaders(headers)

    def SetYandexUid(self, yandexuid=YANDEXUID):
        self.SetCookies({
            'yandexuid': yandexuid
        })

    def SetYandexGid(self, yandexgid):
        self.SetCookies({
            'yandex_gid': yandexgid
        })

    def SetExternal(self, ip=None):
        self.SetHeaders({'X-Yandex-Internal-Request': 0})
        if ip is None:
            ip = EXTERNAL_IP
        self.SetRwrHeaders({
            'X-Forwarded-For': ip,
            'X-Forwarded-For-Y': ip,
        })

    def SetInternal(self, ip=None):
        self.SetHeaders({'X-Yandex-Internal-Request': 1})
        if ip:
            self.SetRwrHeaders({
                'X-Forwarded-For': ip,
                'X-Forwarded-For-Y': ip
            })

    def SetNoAuth(self):
        self.SetCookies({
            'Session_id': COOKIE_NOAUTH
        })

    def SetDumpFilter(self, req=[], resp=[]):
        if len(req) > 0:
            self.json_dump_requests = req
        if len(resp) > 0:
            self.json_dump_responses = resp

    def SetAjax(self):
        assert 'yandexuid' not in self.cookies
        self.SetYandexUid(YANDEXUID)
        self.SetParams({'callback': CALLBACK, 'ajax': '{}', 'yu': YANDEXUID})

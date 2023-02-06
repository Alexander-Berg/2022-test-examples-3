# -*- coding: utf-8 -*-

import re
import base64
import zlib
import os
import json
import jsonschema
import types
from copy import deepcopy
from six import string_types
import yatest.common
from util.const import TLD
from urllib.parse import parse_qs, urlparse
from util.const import CALLBACK
from base64 import b64decode
from util.protobuf_to_dict import protobuf_to_dict
from util.params import get_soy_public_key
from lib.soy_crypto import soy_encrypt
from lxml import etree
from apphost.lib.proto_answers.http_pb2 import THttpResponse, THttpRequest


class TerminateTest(Exception):
    pass


class TsoyQueryInfo(Exception):
    def __init__(self, yt_output_table=None, query=None):
        query_id = query.GetQueryId()
        curl = query.Dump()
        reqid = None
        message = ""

        if query.response is not None:
            reqid = query.response.headers.get("X-Yandex-Req-Id", None)
        else:
            message += "Response not found\n"
            if query.error_response:
                reqid = query.error_response.headers.get("X-Yandex-Req-Id", None)
                # TODO (kozunov) this needs only for xml
                message += "But in SoY error_table it has. First 270 symbols:\n"
                message += "'{}'\n".format(query.error_response.text[:270])
            else:
                message += "It seems like Soy error. Possible can not find id: {} in SoY output_table and error_table.\n".format(query_id)

        if query.error:
            message += "Error in SoY: {}\n".format(query.error)
            if query.error == 'Failed to acquire AH responses':
                message += "You may be need add vertex TUNNELLER into graph.\n"

        self.txt = "\n{message}X-Yandex-Req-Id: {reqid}\nSoY Output Table: {yt_output_table}\nQueryId: {query_id}\n{curl}".format(
            message=message,
            reqid=reqid,
            yt_output_table=yt_output_table,
            query_id=query_id,
            curl=curl
        )

    def __str__(self):
        return self.txt


def ParseDomain(domain):
    m = re.search(r'^(.*?)(?:yandex\.)(.+)$', domain)
    return {
        'subdomain': m.group(1).lower() if m else '',
        'tld': m.group(2).lower() if m else TLD.RU
    }


class SoyEncryptor:
    public_key = None

    def __init__(self):
        if self.public_key is None:
            pk = get_soy_public_key()
            if pk is not None:
                self.public_key = pk

    def Encrypt(self, message):
        return soy_encrypt(self.public_key, message) if self.public_key is not None else message


class Auth():
    def SetXmlAuth(self, query, xml_auth):
        query.SetExternal(ip=xml_auth['ip'])
        query.SetParams({
            'user': xml_auth['user'],
            'key': xml_auth['key']
        })

    def SetBlackBoxAuth(self, query, blackbox):
        query.SetBlackBox(blackbox)

    def EnableKuka(self, query, kuka):
        # так как кука релевантности выставляется в контейнере yp, а контейнер yp используется в тестах, то надо правильно мерджить
        relev = kuka.session.cookies.get_dict()["yp"]
        c = query.ReqYpCookie.parse(relev)
        query.ReqYpCookie.set_sdrlv(c['sdrlv'])
        query.SetBlackBox(kuka)
        query.SetFlags({
            "new_relev": 1
        })

    def GetMiddleHost(self, query):
        query.SetDumpFilter(resp=[])

    @staticmethod
    def GetCookieRelevanceByTld(blackbox, tld):
        return blackbox.session.get('https://cookie.serp.yandex.{}/superman_cookie?action=set'.format(tld))


class JsonSchemaValidator():
    def validate_schema(self, ctx, schema):
        assert schema is not None, 'No schema'

        # https://wiki.yandex-team.ru/yatool/test/#python
        schema_folder = yatest.common.source_path('search/integration_tests/schema_data')

        if isinstance(schema, string_types):
            filename = os.path.join(schema_folder, schema)
            with open(filename) as f:
                content = f.read()
                schema = json.loads(content)
            schema_folder = os.path.dirname(filename)
        elif isinstance(schema, dict):
            schema = deepcopy(schema)
        else:
            raise ValueError(str(schema)[:512] + '\nInvalid schema')

        # support relative path "$ref": "file://file.json#something
        assert 'id' not in schema, "Invalid json schema: explicit property 'id' in schema is forbidden!"
        schema['id'] = 'file://' + schema_folder + '/'
        schema['$schema'] = "http://json-schema.org/schema#"

        jsonschema.validate(ctx, schema)

        return True


class XmlSchemaValidator():
    def parse_xml(self, xml_data, schema=None):
        parser = etree.XMLParser()
        if schema is not None:
            schema_folder = yatest.common.source_path('search/integration_tests/schema_data')
            schema_str = etree.XMLSchema(file=os.path.join(schema_folder, schema))
            parser = etree.XMLParser(schema=schema_str)

        root = etree.fromstring(xml_data, parser)

        # проверяем, что нет внутренних параметров в выдаче
        invalid_nodes = [
            'internal_nodes=_IsFake', '_HilitedUrl', '_Markers', '_MetaSearcherHostname',
            '_MimeType', '_SearcherHostname', 'geo', 'geoa'
        ]
        for node in invalid_nodes:
            res = root.xpath('//%s' % node)
            if type(res) == bool:
                assert res is False, 'Found invalid node(for internal use only): %s' % node
            else:
                assert len(res) == 0, 'Found invalid node(for internal use only): %s' % node
        return root


class EventLogFrameParser():
    _eventlog_sep = re.compile(r'\n+(?=\d+\t)')

    def get_eventlog_frames(self, content):
        frames_bin = []
        for x in re.findall(r'<!-- //EventLogFrame=(\d+)?$(.*?)//EventLogFrame=', content, re.S | re.M):
            frames_bin.append(x[1])
        assert frames_bin, 'No EventLogFrame markers'

        frames_raw = [zlib.decompress(base64.decodestring(x), zlib.MAX_WBITS | 16) for x in frames_bin]
        assert frames_raw

        records = [y for x in frames_raw for y in re.split(self._eventlog_sep, x)]
        frames = filter(bool, [filter(len, map(str.strip, x.split('\t'))) for x in records])

        assert frames

        return frames


def AddResponseHelpers(resp):
    assert resp is not None

    def GetLocation(self):
        location = self.headers.get('location', None)
        assert location is not None
        return urlparse(location)

    def GetLocationParams(self):
        return parse_qs(self.GetLocation().query)

    def GetLocationTld(self):
        return ParseDomain(self.GetLocation().hostname)['tld']

    def GetSetCookie(self):
        result = {}
        set_cookie = self.headers.get('Set-Cookie')
        m = re.split(' *(domain=[^ ,]*),? *', set_cookie)
        for i in range(1, len(m), 2):
            c = {}
            for kv in [m[i]] + re.split('; *', m[i-1]):
                kv_arr = re.split('=', kv, 2)
                if len(kv_arr[0]) > 0:
                    if len(kv_arr) == 1:
                        kv_arr.append(True)
                    if kv_arr[0].lower() in ['domain', 'path', 'expires', 'max-age', 'secure', 'httponly']:
                        kv_arr[0] = kv_arr[0].lower()
                    c[kv_arr[0]] = kv_arr[1]
            domain = c.pop('domain', None)
            path = c.pop('path', None)
            expires = c.pop('expires', None)
            max_age = c.pop('max-age', None)
            secure = c.pop('secure', None)
            httponly = c.pop('httponly', None)
            for (k, v) in c.items():
                result[k] = {
                    'domain': domain,
                    'path': path,
                    'expires': expires,
                    'max-age': max_age,
                    'secure': secure,
                    'httponly': httponly,
                    'value': v
                }
        return result

    def GetCallbackJson(self):
        result = {}
        data = self.text
        m = re.findall('^' + CALLBACK + r'\((.*?)\)$', data)
        assert m
        try:
            result = json.loads(m[0])
        except:
            raise AssertionError('Bad JSON format: {}'.format(data[0:1000]))
        return result

    def GetCtxs(self):
        def _DecodeRawByPath(b64_raw, message, item_type):
            raw = b64decode(b64_raw)
            message.ParseFromString(raw)
            d = protobuf_to_dict(message)
            if item_type == 'yabs_setup':
                d['Path'] = re.sub(';', '%3B', d['Path'])
                d = parse_qs(d['Path'])

            d['type'] = item_type
            return d

        def explore_ctxs(path, tree):
            if isinstance(tree, list):
                tree = {'response': tree}
            ctxs = {}
            if tree is None or not isinstance(tree, dict):
                return ctxs
            for source in tree.keys():
                if source == 'response':
                    for ctx in tree[source]:
                        # APPHOSTSUPPORT-510 we must not get list, but...
                        if isinstance(ctx, list):
                            continue
                        if ctx.get('__content_type') == 'yson':
                            ctx = ctx.get('binary', ctx)
                        elif ctx.get('__content_type') == 'protobuf':
                            if ctx.get('type') in ['yabs_setup']:
                                ctx = _DecodeRawByPath(ctx['binary'], THttpRequest(), ctx['type'])
                            elif ctx.get('type') in ['http_response']:
                                ctx = _DecodeRawByPath(ctx['binary'], THttpResponse(), ctx['type'])
                        if ("type" in ctx) and ctx['type']:
                            type = ctx['type']
                            val = _DecodeRawByPath(ctx["__binary__"], type) if "__binary__" in ctx else ctx
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
        tree = self.json()
        return explore_ctxs('ROOT', tree)

    def CheckHttpResponseStatus(self, status):
        http_resp = self.GetCtxs()['http_response']
        assert len(http_resp) and "status_code" in http_resp[0] and http_resp[0]["status_code"] == status

    resp.GetLocation = types.MethodType(GetLocation, resp)
    resp.GetLocationParams = types.MethodType(GetLocationParams, resp)
    resp.GetLocationTld = types.MethodType(GetLocationTld, resp)
    resp.GetSetCookie = types.MethodType(GetSetCookie, resp)
    resp.GetCallbackJson = types.MethodType(GetCallbackJson, resp)
    resp.GetCtxs = types.MethodType(GetCtxs, resp)
    resp.CheckHttpResponseStatus = types.MethodType(CheckHttpResponseStatus, resp)

    return resp

# -*- coding: utf-8 -*-

import copy
from requests import Request, Session, Response
from util.params import get_soy_token, get_yt_token, get_soy_pool, get_yt_batch_table_folder, get_soy_map_type, get_soy_result
from util.helpers import TerminateTest
from yt.yson.yson_types import YsonUint64
import yt.wrapper as yt
from yt.wrapper import YtClient
from datetime import datetime
import uuid
# from requests.structures import CaseInsensitiveDict
from requests.utils import get_encoding_from_headers
import json
from urllib3.response import HTTPHeaderDict
import io
import time
import logging
import re

SCRAPER_API = 'https://soyproxy.yandex.net/hahn/soy_api'
# SCRAPER_API = 'https://hahn-api-scraperoveryt2.n.yandex-team.ru/soy_api'
YT_TABLE_FOLDER = get_yt_batch_table_folder()
YT_TABLE_PREFIX = 'batch_'


class SoY(object):
    def GetYtTableName(self):
        return yt.TablePath(
            '{folder}/{name}'.format(
                folder=YT_TABLE_FOLDER,
                name=self.yt_table_name
            ),
            client=self.yt_client
        )

    def GetYtOutputTableName(self):
        return self.yt_output_table_name

    def MakeInputTable(self, queries):
        logging.debug('Create input table')
        try:
            table = self.GetYtTableName()
            logging.debug('Input table: {}'.format(table))
            if self.yt_client.exists(table):
                self.yt_client.remove(table)
            # 1209600000 = 14 * 24 * 3600 * 1000 ms (2 weeks)
            self.yt_client.create('table', table, recursive=True, attributes={"expiration_timeout": 1209600000})
            records = []
            logging.debug('TOTAL SOY QUERIES: {}'.format(len(queries)))
            for query in queries:
                req = query.PrepareRequest(need_encrypt_session_cookies=True)
                plain_headers = []
                for (key, val) in req.headers.items():
                    if key.lower() not in ['connection']:
                        plain_headers.append('{}: {}'.format(key, val))
                require_status = list(map(lambda x: YsonUint64(x), query.GetRequireStatus()))
                records.append({
                    'cookies': [],
                    'headers': plain_headers,
                    "id": query.GetQueryId(),
                    "method": req.method,
                    "postdata": req.body if req.body is not None else "",
                    "uri": req.url,
                    "require_status": require_status,
                    "userdata": {}
                })
            self.yt_client.write_table(table, records, raw=False)
            logging.debug('SoY INPUT TABLE: {}'.format(table))
        except Exception as e:
            raise TerminateTest(e)
        logging.debug('Complete table creation function')

    @staticmethod
    def GetQuery(queries, qid, table=None):
        # TODO (kozunov) we call it for each response, so complexity ~ O(N^2)
        # but we need ~ O(1)
        drop_digits = None
        if get_soy_result() is not None:
            # qid is "454#TestXmlGET::test_xml_cgi_sortby..." when run with SOY_RESULT, me must delete 454#
            drop_digits = re.compile(r"^\d+#")
            qid = drop_digits.sub("", qid)
        for query in queries:
            qid2 = query.GetQueryId()
            if drop_digits:
                qid2 = drop_digits.sub("", qid2)
            if qid == qid2:
                return query
        logging.error("Can not find query from SoY table: {} with id: {}".format(table, qid))
        return None

    @staticmethod
    def SetResponse(query, element):
        fetched_result = copy.deepcopy(element['FetchedResult'])
        resp = Response()
        resp.status_code = int(element['StatusCode'])
        resp.headers = HTTPHeaderDict()
        if element['ReturnedHeaders'] is not None:
            for line in element['ReturnedHeaders'].split("\n"):
                kv = line.replace("\r", '').split(': ', 1)
                if len(kv) == 2:
                    if kv[0] in ['Content-Encoding']:
                        if 'br' not in kv[1]:
                            kv[1] = 'identity'
                    resp.headers.add(kv[0], kv[1])
        resp.encoding = get_encoding_from_headers(resp.headers)
        resp._content = fetched_result if isinstance(fetched_result, bytes) else fetched_result.encode()

        def json_helper(content):
            def json_func():
                try:
                    string = io.StringIO(content)
                    setattr(string, 'release_conn', lambda *args: args)
                    return json.load(string)
                except:
                    logging.debug('Bad JSON format: {}'.format(content))
                    return None
            return json_func

        resp.json = json_helper(resp._content.decode())

        if element['Error'] is None:
            query.response = resp
        else:
            query.error = element['Error']
            query.error_response = resp
            logging.debug("Set query.error: {} for id: {}".format(query.error, query.GetQueryId()))

    def ReadOutputTable(self, table, queries):
        # TODO why set self.yt_output_table_name? it's already set in the SoYStatus()
        self.yt_output_table_name = yt.TablePath(str(table), client=self.yt_client)
        logging.debug('SoY OUTPUT TABLE: {}'.format(self.yt_output_table_name))
        if self.yt_client.exists(self.yt_output_table_name):
            result_gen = self.yt_client.read_table(self.yt_output_table_name, format='json')
            for element in result_gen:
                query = SoY.GetQuery(queries, element['id'], table)
                if query is None:
                    continue

                # TODO (kozunov) seems this code needs for SOY http mode. But we use retries in the Sandbox.
                # let's skip it
                '''
                status_code = int(element['StatusCode'])
                if status_code == 200 and (element['FetchedResult'] is None or len(element['FetchedResult']) == 0):
                    logging.error('Retry an empty response with status_code==200; id={}'.format(element['id']))
                    # TODO (kozunov) SOY doesn't parse response, so it's strange that we do retries
                    while query.retry > 0 and not query.SendRequest():
                        time.sleep(1)
                else:
                    SoY.SetResponse(query, element)
                '''
                SoY.SetResponse(query, element)
        return None

    def ReadErrorTable(self, table, queries):
        logging.debug('SoY ERROR TABLE: {}'.format(table))
        if self.yt_client.exists(table):
            rows = 0
            for element in self.yt_client.read_table(table, format='json'):
                rows += 1
                query = SoY.GetQuery(queries, element['id'], table)
                if query is not None:
                    SoY.SetResponse(query, element)
                logging.debug('SoY ERROR TABLE. id:{} error: {}'.format(element['id'], element['Error']))
            logging.debug('SoY ERROR TABLE: {} has: {} rows'.format(table, rows))

    def SoYCreate(self):
        # https://wiki.yandex-team.ru/scraperoveryt/startguide/#/create
        req = self.ReqPrepare(
            '/create',
            data={
                "input_table": str(self.GetYtTableName()),
                "map_operation_type": get_soy_map_type(),
                "output_in_json" : True,
                "pool": get_soy_pool(),
                "redirect_depth": 0,
                # morozyto@ recommed disable this setting. SOY get optimal value.
                # "max_retries_per_row": 3,
                "has_secret": True,
                "id": self.uuid,
                # mlm use priority 100, we must set greater.
                "priority": 101,
                "queue_timeout": "90m",
                "execution_timeout": "90m"
            }
        )
        return self.ReqSend(req)

    def SoYStatus(self):
        req = self.ReqPrepare(
            method='GET',
            handler='/status',
            params={
                "id": self.uuid
            }
        )
        resp = self.ReqSend(req)
        if 'output_path' in resp:
            self.yt_output_table_name = resp['output_path']
        return resp

    def SoYAbort(self):
        req = self.ReqPrepare(
            method='GET',
            handler='/abort',
            params={
                "id": self.uuid
            }
        )
        return self.ReqSend(req)

    def ReqDump(self, req):
        return "{RLINE}\r\n{HEADERS}\r\n\r\n{BODY}".format(
            RLINE=req.method + ' ' + req.url,
            HEADERS='\r\n'.join('{}: {}'.format(k, v) for k, v in req.headers.items()),
            BODY=req.body if req.method == 'POST' else ''
        )

    def ReqDumpCurl(self, req):
        return "curl -X {method} -H '{headers}' --data '{json}' '{url}'".format(
            method=req.method,
            headers="' -H '".join('{}: {}'.format(k, v) for k, v in req.headers.items()),
            json=req.body if req.method == 'POST' else '',
            url=req.url
        )

    def ReqPrepare(self, handler, method='POST', params={}, data={}, api=SCRAPER_API):
        req = Request(
            method=method,
            url='{api}{handler}'.format(api=api, handler=handler),
            headers=self.PrepareHeaders(),
            json=data if method == 'POST' else None,
            params=params
        )
        return req.prepare()

    def ReqSend(self, req):
        retry = 3
        while(retry > 0):
            try:
                resp = self.session.send(req)
                return resp.json()
            except:
                time.sleep(3)
            retry = retry - 1

    def PrepareHeaders(self, headers={}):
        h = {}
        h.update(self.session.headers)
        h.update(headers)
        for k in list(h.keys()):
            if h[k] is None:
                del h[k]
        return h

    def __init__(self):
        self.soy_token = get_soy_token()
        self.yt_token = get_yt_token()
        self.session = Session()
        self.session.headers.update({
            'Connection': None,
            'Accept-Encoding': None,
            'User-Agent': 'TSoY zhiv bot v0.01',
            'X-Yandex-HTTPS': 'yes',
            'Authorization': 'OAuth {}'.format(self.soy_token)})
        self.yt_client = YtClient(proxy='hahn', token=self.yt_token)
        self.yt_table_name = '{prefix}_{ext}'.format(prefix=YT_TABLE_PREFIX, ext=datetime.now().strftime("%Y-%m-%d_%H%M%S"))
        self.yt_output_table_name = None
        self.uuid = str(uuid.uuid4())

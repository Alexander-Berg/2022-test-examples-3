# -*- coding: utf-8 -*-

from inspect import getargspec
from util.soy import SoY
from util.params import soy_mode, get_soy_result, logging_mode
from util.helpers import TerminateTest
import time
import traceback
import inspect
import logging
import sys
from contextlib import contextmanager
import os

INDENT = ','


def InitLogging():
    logger = logging.getLogger()
    logger.handlers = [logging.StreamHandler(sys.stderr)]
    mode = logging_mode()
    if mode == 'DEBUG':
        logger.setLevel(logging.DEBUG)
    elif mode == 'INFO':
        logger.setLevel(logging.INFO)
    else:
        logger.setLevel(logging.INFO)
    logger.info('logger level set to {}'.format(str(logger.level)))
InitLogging()


def MakeTbId():
    st = traceback.extract_stack()
    if st:
        st = st[:-1]
        buf = []
        for filename, lineno, name, line in st:
            buf.append('  File "%s", line %d, in %s' % (filename, lineno, name))
            if line:
                buf.append('    %s' % line.strip())
        return "\n".join(buf)
    return None


def get_soy_file_name(name):
    path = os.environ.get('PATH_TO_SOY_BATCH')
    filename = None
    if path and name:
        filename = os.path.join(path, name)
    return filename


@contextmanager
def managed_resource(soy, soy_queries):
    try:
        # Code to acquire resource
        resource = None
        if (len(soy_queries)):
            if get_soy_result() is None:
                # can throw exception
                soy.MakeInputTable(soy_queries)
                soy_resp = soy.SoYCreate()
                if soy_resp is None or soy_resp['status'] != 'ok':
                    raise TerminateTest('SoY batch not created: \n{}\n'.format(soy_resp))

                logging.debug("SOY CREATE: {}".format(soy_resp))
                resource = soy
                myfile = get_soy_file_name(soy.uuid)
                if myfile:
                    fh = open(myfile, 'w')
                    fh.write(soy.uuid)
                    fh.close()

        yield resource
    finally:
        # Code to release resource
        if resource is not None:
            resp = resource.SoYAbort()
            logging.error("try aborting soy request: {}".format(resp))
            myfile = get_soy_file_name(resource.uuid)
            if myfile and os.path.exists(myfile) and os.path.isfile(myfile):
                os.remove(myfile)


class TSoY(object):

    mode = 'collect'
    test_queries = []
    yt_output_table_name = None

    @staticmethod
    def Size():
        return len(TSoY.test_queries)

    @staticmethod
    def Queries():
        return TSoY.test_queries

    @staticmethod
    def yield_test(func):
        # request.node.nodeid where request is default fixture
        test_id = '{}(%s)'.format(str(func).split(' ')[1])
        for frame in inspect.stack():
            if frame.function.startswith('Test'):
                test_id = '{}::{}(%s) at {}:{}'.format(
                    str(frame.function),
                    func.__name__,
                    frame.filename,
                    frame.lineno)
                break
        result = [None]  # necessary so exec can "return" objects
        source = []
        add = lambda indent, line: source.append(' ' * indent + line)  # shorthand
        arglist = ', '.join(getargspec(func).args)  # this does not cover keyword or default args
        namespace = {'f': func, 'result': result, 'test_id': test_id, 'arglist': getargspec(func).args, 'logging': logging}
        add(0, 'import sys')
        add(0, 'from util.tsoy import TSoY')
        add(0, 'from jsonschema.exceptions import ValidationError')
        add(0, 'from util.helpers import TsoyQueryInfo')
        add(0, 'from simplejson.errors import JSONDecodeError')
        add(0, 'from lxml.etree import XMLSyntaxError')
        add(0, 'from requests.exceptions import ChunkedEncodingError')
        add(0, 'def helper():')
        add(2, 'generators = {}')
        add(2, 'queries = {}')
        add(2, 'def wrapper(%s):' % (arglist,))
        add(4, 'args = []')
        add(4, 'values = (%s)' % (arglist,))
        add(4, 'for i in range(0, len(arglist)):')
        add(6, 'if values[i] is None or isinstance(values[i], (str, int, float, list, tuple, dict)):')
        add(8, 'args.append("{}={}".format(arglist[i], values[i]))')
        add(4, 'query_id = test_id % ", ".join(args)')
        add(4, 'logging.debug("in yield_test decorator TSoY.mode={}".format(TSoY.mode))')
        add(4, 'if TSoY.mode == "collect":')
        add(6, 'generator = f(%s)' % (arglist,))
        add(6, 'query = next(generator)')
        add(6, 'query.SetQueryId("{}#{}".format(query.GetQueryId(), query_id))')
        add(6, 'assert query_id not in generators, "duplicate key: {}".format(query_id)')
        add(6, 'assert query_id not in queries, "duplicate key: {}".format(query_id)')
        add(6, 'generators[query_id] = generator')
        add(6, 'queries[query_id] = query')
        add(6, 'TSoY.collect(query)')
        add(4, 'else:')
        add(6, 'assert query_id in generators and query_id in queries')
        add(6, 'generator = generators[query_id]')
        add(6, 'query = queries[query_id]')
        add(6, 'try:')
        add(8, 'n = 10')
        add(8, 'while(n > 0):')
        add(10, 'resp = None')
        add(10, 'try:')
        add(12, 'resp = query.GetResponse()')
        add(12, 'q = generator.send(resp)')
        for exception in ['AssertionError', 'KeyError', 'TypeError', 'JSONDecodeError', 'ChunkedEncodingError', 'ValidationError', 'XMLSyntaxError']:
            add(10, 'except {} as e:'.format(exception))
            add(12, 'raise TsoyQueryInfo(yt_output_table=TSoY.yt_output_table_name, query=query) from e')
        add(10, 'n = n - 1')
        add(8, 'for g in generator:')
        add(10, 'pass')
        add(6, 'except StopIteration as e:')
        add(8, 'pass')
        add(2, 'return wrapper')
        add(0, 'result[0] = helper()')
        expr = '\n'.join(source)
        logging.debug("#EXEC in decorator yield_test ({})".format(test_id))
        exec(expr, namespace)
        return result[0]  # this is wrapper

    @staticmethod
    def collect(query):
        TSoY.test_queries.append(query)

    @staticmethod
    def _process_queries_req(query_plan, sleep_secs=0):
        retry_plan = []
        for query in query_plan:
            time.sleep(sleep_secs)
            if query.SendRequest() or query.retry <= 0:
                pass
            else:
                retry_plan.append(query)
        return retry_plan

    @staticmethod
    def process_queries():
        if soy_mode():
            logging.debug('SoY MODE')
            soy = SoY()
            logging.debug("SoY id: {}".format(soy.uuid))
            req_queries = []  # Запросы для requests
            soy_queries = []  # Запросы для SoY
            logging.debug('TOTAL TEST QUERIES: {}'.format(len(TSoY.test_queries)))
            if (not len(TSoY.test_queries)):
                logging.debug('queries are empty')
                return

            for q in TSoY.test_queries:
                if q.CanSoyMode():
                    soy_queries.append(q)
                else:
                    req_queries.append(q)

            # Запуск SoY батча
            logging.debug("TOTAL SOY QUERIES: {}".format(len(soy_queries)))
            with managed_resource(soy, soy_queries):
                # Отрабатываем requests запросы до конца
                logging.debug("TOTAL REQ QUERIES: {}".format(len(req_queries)))
                sleep_secs = 0
                while len(req_queries) > 0:
                    req_queries = TSoY._process_queries_req(req_queries, sleep_secs)
                    sleep_secs = sleep_secs + 0.1

                if (not len(soy_queries)):
                    return

                # Ждем окончания работы soy
                logging.debug("GET_SOY_RESULT: {}".format(get_soy_result()))
                if get_soy_result() is not None:
                    soy.ReadOutputTable(os.path.join(get_soy_result(), "output_table"), soy_queries)
                else:
                    soy_resp = soy.SoYStatus()
                    logging.debug("SOY STATUS: {}".format(soy_resp))
                    while soy_resp is None or (soy_resp['status'] == 'ok' and (('final_status' not in soy_resp) or (soy_resp['final_status'] is None))):
                        if soy_resp is None:
                            logging.error("Somethong bad with soy")
                        time.sleep(15)
                        soy_resp = soy.SoYStatus()
                    if soy_resp['status'] == 'ok' and soy_resp['final_status'] in ['completed']:
                        soy.ReadOutputTable(soy_resp['output_path'], soy_queries)
                    else:
                        raise TerminateTest('SoY batch not completed: \n{}\n'.format(soy_resp))

            TSoY.yt_output_table_name = soy.GetYtOutputTableName()

            if get_soy_result() is not None:
                soy.ReadErrorTable(os.path.join(get_soy_result(), "error_table"), soy_queries)
            else:
                soy.ReadErrorTable(soy_resp['error_path'], soy_queries)

        else:
            req_queries = TSoY.test_queries  # Запросы для requests
            logging.debug("TOTAL REQ QUERIES: {}".format(len(req_queries)))

            sleep_secs = 0
            while len(req_queries) > 0:
                req_queries = TSoY._process_queries_req(req_queries, sleep_secs)
                sleep_secs = sleep_secs + 0.2

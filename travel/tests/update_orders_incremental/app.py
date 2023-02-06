# -*- coding: utf-8 -*-

from typing import Any, Dict, List
import json
import logging
import os

from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import ConsumerMessageType
from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import PQStreamingAPI
from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import ConsumerConfigurator
from pytest_localserver.http import WSGIServer
from werkzeug.wrappers import Request, Response
from yt_helper import TableData, YtHelper
import yatest.common as common


class App:
    timeout_seconds = 5
    topic_notifications = 'topic_notifications'

    def __init__(self, yt_helper: YtHelper):
        self.yt_helper = yt_helper
        self.lb_grpc_port = int(os.getenv('LOGBROKER_PORT'))

        with open(common.source_path('travel/cpa/tests/update_orders_incremental/stock_response.json')) as f:
            self.stock_response_json = f.read()
        with open(common.source_path('travel/cpa/tests/update_orders_incremental/stock_response.xml')) as f:
            self.stock_response_xml = f.read()

        self.stocks_server = WSGIServer(application=self._web_app)

    def run(self, tables: Dict[str, TableData]) -> None:
        self.yt_helper.create_tables()
        self.yt_helper.write_tables(tables)
        bin_root = common.binary_path('travel/cpa/data_processing/update_orders_incremental')

        os.chdir(bin_root)
        self.stocks_server.start()
        res = common.execute(self._get_app_args(bin_root), wait=True)
        logging.info(f'Process finished with {res}')
        self.stocks_server.stop()

    def read_notifications(self):
        data = list()
        api = PQStreamingAPI('localhost', self.lb_grpc_port)
        configurator = ConsumerConfigurator(self.topic_notifications, 'test-client')
        try:
            f = api.start()
            f.result(self.timeout_seconds)

            reader = api.create_consumer(configurator)

            response = reader.start().result(timeout=self.timeout_seconds)
            if not response.HasField('init'):
                raise Exception('Failed to initialize logbroker connection')
            result = reader.next_event().result(timeout=10)
            assert result.type == ConsumerMessageType.MSG_DATA
            extracted_messages = self._process_read_result(result.message)
            data.extend(extracted_messages)
        finally:
            api.stop()
        return data

    def _get_app_args(self, bin_root: str) -> List[Any]:
        app_args = list()

        exec_path = os.path.join(bin_root, 'update_orders_incremental')
        workdir = self.yt_helper.yt_root

        app_args.append(exec_path)
        app_args.extend(['--yt-proxy', self.yt_helper.yt_proxy])
        app_args.extend(['--workdir', workdir])
        app_args.extend(['--dump-dir-public', f'{workdir}/public'])
        app_args.extend(['--dump-dir-private', f'{workdir}/private'])
        app_args.extend(['--stocks-url', self.stocks_server.url])
        app_args.append('--skip-set-medium-attributes')
        app_args.append('--notify-order-change')
        app_args.extend(['--lb-url', 'localhost'])
        app_args.extend(['--lb-port', str(self.lb_grpc_port)])
        app_args.extend(['--lb-topic', self.topic_notifications])
        app_args.append('--fail-fast')

        return app_args

    @Request.application
    def _web_app(self, request: Request):
        return self._get_response(request)

    def _get_response(self, request: Request):
        logging.info(request)

        if request.method != 'GET':
            logging.error(f'{request.method} method instead of GET')
            raise Exception(f'Unexpected request method: {request.method}')

        logging.info(f'request.path = {request.path}')

        # /{stock_id}.xml
        if request.path.endswith('.xml'):
            stock_id = int(request.path[1:-4])
            data = self.stock_response_xml.format(stock_id=stock_id)
            return Response(data, mimetype='application/json')

        # /graph_{stock_id}.json
        elif request.path.endswith('.json'):
            return Response(self.stock_response_json, mimetype='application/json')

        else:
            raise Exception(f'Unexpected request {request}')

    @staticmethod
    def _process_read_result(consumer_message):
        ret = []
        for batch in consumer_message.data.message_batch:
            for message in batch.message:
                ret.extend([json.loads(item) for item in message.data.splitlines()])
        return ret

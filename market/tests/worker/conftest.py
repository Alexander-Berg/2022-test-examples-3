from market.amore.service.api.proto import config_pb2 as api_pb
from market.amore.service.worker2.proto import config_pb2 as worker_pb
from google.protobuf import text_format
import yatest.common as ytc
from yatest.common import network
import pytest
from pathlib import Path
import logging
import os
import psycopg2
import yt.wrapper as yt
import kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api as pqlib
from kikimr.public.sdk.python.persqueue.errors import SessionFailureResult


class LocalWorker:
    def __init__(self, work_dir):
        self.logger = logging.getLogger("test_logger")

        cfg_api = ytc.source_path('market/amore/service/tests/worker/cfg_api')
        self.api_cfg_pb = api_pb.TConfig()
        with open(cfg_api) as f:
            text_format.Parse(f.read(), self.api_cfg_pb)

        cfg_worker = ytc.source_path('market/amore/service/tests/worker/cfg_worker')
        self.worker_cfg_pb = worker_pb.TConfig()
        with open(cfg_worker) as f:
            text_format.Parse(f.read(), self.worker_cfg_pb)

        with network.PortManager() as pm:
            self.api_cfg_pb.Core.Server.Port = pm.get_port()
            self.worker_cfg_pb.Core.Server.Port = pm.get_port()

        print("[TEST] api port - {}".format(self.api_cfg_pb.Core.Server.Port))
        print("[TEST] worker port - {}".format(self.worker_cfg_pb.Core.Server.Port))

        self.worker_cfg_pb.Logbroker.Port = int(os.environ.get('LOGBROKER_PORT'))
        self.worker_cfg_pb.PriceLabs.Server = os.environ.get('YT_PROXY')
        self.worker_cfg_pb.PriceLabs.Table = "//tmp/test_local_worker_pl_table_autostrategy_offers"

        self.worker_cfg_pb.LogbrokerBeru.Port = int(os.environ.get('LOGBROKER_PORT'))
        self.worker_cfg_pb.PriceLabsVendors.Server = os.environ.get('YT_PROXY')
        self.worker_cfg_pb.PriceLabsVendors.Table = "//tmp/test_local_worker_beru_pl_table_autostrategy_offers"

        yt.config['proxy']['url'] = self.worker_cfg_pb.PriceLabs.Server
        self.pl_table_attributes = {'dynamic': True, 'schema': [
            {'name': 'timestamp', 'type': 'int64', 'sort_order': 'ascending', 'required': True},
            {'name': 'shop_id', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'autostrategy_id', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'feed_id', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'type', 'type': 'int8', 'sort_order': 'ascending', 'required': True},
            {'name': 'bid', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'offer_id', 'type': 'string', 'sort_order': 'ascending', 'required': True},
            {'name': 'business_id', 'type': 'int64', 'sort_order': 'ascending', 'required': True},
            {'name': 'unused_column', 'type': 'int32'}
        ]}

        yt.create(type='table', path=self.worker_cfg_pb.PriceLabs.Table, recursive=True,
                  attributes=self.pl_table_attributes)

        yt.mount_table(self.worker_cfg_pb.PriceLabs.Table, sync=True)

        self.worker_cfg_pb.LogbrokerBeru.Port = int(os.environ.get('LOGBROKER_PORT'))
        self.worker_cfg_pb.PriceLabsVendors.Server = os.environ.get('YT_PROXY')
        self.worker_cfg_pb.PriceLabsVendors.Table = "//tmp/test_local_worker_beru_pl_table_autostrategy_offers"

        self.pl_vendor_table_attributes = {'dynamic': True, 'schema': [
            {'name': 'timestamp', 'type': 'int64', 'sort_order': 'ascending', 'required': True},
            {'name': 'id', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'shop_id', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'autostrategy_id', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'feed_id', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'type', 'type': 'int8', 'sort_order': 'ascending', 'required': True},
            {'name': 'bid', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'offer_id', 'type': 'string', 'sort_order': 'ascending', 'required': True},
            {'name': 'business_id', 'type': 'int64', 'sort_order': 'ascending', 'required': True},
            {'name': 'datasource_id', 'type': 'int32', 'sort_order': 'ascending', 'required': True},
            {'name': 'unused_column', 'type': 'int32'}
        ]}

        yt.create(type='table', path=self.worker_cfg_pb.PriceLabsVendors.Table, recursive=True,
                  attributes=self.pl_vendor_table_attributes)

        yt.mount_table(self.worker_cfg_pb.PriceLabsVendors.Table, sync=True)

        pg_port = os.environ.get('PG_LOCAL_PORT')
        pg_dbname = os.environ.get('PG_LOCAL_DATABASE')
        pg_user = os.environ.get('PG_LOCAL_USER')
        pg_pwd = os.environ.get('PG_LOCAL_PASSWORD')
        self.db_con_str = 'host=localhost port={} dbname={} user={} password={}'. \
            format(pg_port, pg_dbname, pg_user, pg_pwd)
        self.worker_cfg_pb.Secret.PgConnectionString = self.db_con_str
        self.api_cfg_pb.Secret.PgConnectionString = self.db_con_str

        sql_script_path = ytc.source_path('market/amore/service/db/master_db.sql')
        with open(sql_script_path, 'r') as f:
            sql_script = f.read()

        with psycopg2.connect(self.db_con_str) as conn:
            with conn.cursor() as cur:
                cur.execute(sql_script)
            conn.commit()

        api_cfg_path = Path(work_dir) / 'api_new_cfg'
        worker_cfg_path = Path(work_dir) / 'worker_new_cfg'

        with open(api_cfg_path, 'w') as f:
            f.write(text_format.MessageToString(self.api_cfg_pb))

        with open(worker_cfg_path, 'w') as f:
            f.write(text_format.MessageToString(self.worker_cfg_pb))

        api_exe_path = ytc.binary_path('market/amore/service/api/bin/amore-api')
        worker_exe_path = ytc.binary_path('market/amore/service/worker2/bin/amore-worker2')

        worker_command = [worker_exe_path, '-c', str(worker_cfg_path)]
        api_command = [api_exe_path, '-c', str(api_cfg_path)]

        self.worker_proc = ytc.execute(worker_command, wait=False)
        self.api_proc = ytc.execute(api_command, wait=False)

        lb_api = pqlib.PQStreamingAPI(self.worker_cfg_pb.Logbroker.Endpoint,
                                      self.worker_cfg_pb.Logbroker.Port)
        lb_api_start_future = lb_api.start()
        lb_start_res = lb_api_start_future.result(timeout=10)
        self.logger.info("LogbrokerApi started with result: {}".format(lb_start_res))
        lb_configurator = pqlib.ConsumerConfigurator(self.worker_cfg_pb.Logbroker.Topic, 'test_client')
        self.lb_consumer = lb_api.create_consumer(lb_configurator)
        lb_consumer_start_future = self.lb_consumer.start()
        lb_consumer_start_result = lb_consumer_start_future.result(timeout=10)

        if not isinstance(lb_consumer_start_result, SessionFailureResult):
            if lb_consumer_start_result.HasField("init"):
                self.logger.info("Consumer start result was: {}".format(lb_consumer_start_result))
            else:
                raise RuntimeError("Bad consumer start result from server: {}.".format(lb_consumer_start_result))
        else:
            raise RuntimeError("Error occurred on start of consumer: {}.".format(lb_consumer_start_result))
        self.logger.info("Consumer started")

    def clear_pg_db(self):
        with psycopg2.connect(self.db_con_str) as conn:
            with conn.cursor() as cur:
                cur.execute('truncate table shops cascade')
            conn.commit()


@pytest.fixture(scope='session')
def local_worker_fixture():
    worker = LocalWorker(ytc.work_path())
    yield worker
    worker.worker_proc.terminate()
    worker.api_proc.terminate()
    worker.worker_proc.wait(check_exit_code=False)
    worker.api_proc.wait(check_exit_code=False)

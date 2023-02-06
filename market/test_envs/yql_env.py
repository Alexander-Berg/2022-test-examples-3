#!/usr/bin/env python
# -*- coding: utf-8 -*-

import logging

from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yql_resource import YtResource
from yql.api.v1.client import YqlClient


class MongoRunner(object):
    def __init__(self, input_dir):
        self._server = None
        self._input_dir = input_dir

    def start(self):
        import os
        import tarfile
        import tempfile
        import uuid
        import subprocess
        from devtools.swag.daemon import run_daemon
        from yql_ports import get_yql_port
        from yql_utils import get_param, yql_output_path
        from mongo_runner import MONGO_TGZ, MONGO_CONFIG_TEMPLATE

        mongo_dir = tempfile.mkdtemp(prefix='mongo_', dir=self._input_dir)
        working_dir = '%s_%s' % (yql_output_path('mongo'), uuid.uuid4())
        os.mkdir(working_dir, 0o755)

        tgz = tarfile.open(MONGO_TGZ)
        tgz.extractall(path=mongo_dir)

        bin_dir = os.path.join(mongo_dir, 'mongodb-linux-x86_64-ubuntu1204-3.2.1', 'bin')
        port = get_yql_port()
        mongod_path = os.path.join(bin_dir, 'mongod')

        if get_param('system_mongo'):
            mongod_path = subprocess.check_output('which mongod', shell=True).strip()

        config_file = os.path.join(working_dir, 'mongo.cfg')
        with open(config_file, 'w') as f:
            f.write(MONGO_CONFIG_TEMPLATE % locals())

        logging.info('Starting mongo at %s' % working_dir)

        cmd = [mongod_path, '--config', config_file]
        self._server = run_daemon(
            cmd,
            cwd=working_dir
        )

        self._server.port = port

    def stop(self):
        if self._server:
            self._server.stop(kill=True)
            self._server = None

    def __enter__(self):
        self.start()

        return self

    def __exit__(self, *args):
        self.stop()

    @property
    def server(self):
        return self._server


class YtRunner(object):
    def __init__(self, yt_resource=None):
        self._yt_config = yt_resource.yt_config
        self._yt_stuff = yt_resource.yt_stuff
        self._instance = None

    def start(self):
        from yt_runner import YT
        from mapreduce.yt.python.yt_stuff import YtConfig

        if not self._yt_config:
            self._yt_config = YtConfig()

        self._instance = YT(self._yt_config)
        if self._yt_stuff:
            # if local yt was already started (using yt_stuff) we should keep the same yt_proxy_port, yt_id and yt_work_dir
            self._instance.yt_proxy_port = self._yt_stuff.yt_proxy_port
            self._instance.yt_id = self._yt_stuff.yt_id
            self._instance.yt_work_dir = self._yt_stuff.yt_work_dir
        else:
            self._instance.start_local_yt()

    def stop(self):
        if not self._yt_stuff and self._instance:
            self._instance.stop_local_yt()
            self._instance = None

    def __enter__(self):
        self.start()

        return self

    def __exit__(self, *args):
        self.stop()

    @property
    def instance(self):
        return self._instance


class YqlApiRunner(object):
    def __init__(self, input_dir, mongo, yt):
        self._input_dir = input_dir
        self._yt = yt
        self._mongo = mongo
        self._api = None

    def start(self):
        import tempfile
        from yql_api import YQLAPI

        tmpdir = tempfile.mkdtemp(prefix='yql_api_', dir=self._input_dir)

        self._api = YQLAPI(mongo=self._mongo.server,
                           tmpdir_module=tmpdir,
                           yt=self._yt.instance,
                           ydb=None,
                           rtmr=None)
        self._api.start()
        self._api.start_worker()
        self._api.wait_ready()

        logging.info('Java api started. Working dir: %s. YqlWorker dir: %s' %
                     (self._api.working_dir, self._api.workers[0].working_dir))

    def stop(self):
        if self._api:
            self._api.stop()
            self._api = None

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *args):
        self.stop()

    @property
    def api(self):
        return self._api


class YqlRequestRunner(object):
    def __init__(self, yql_api, syntax_version=0):
        self._yql_api = yql_api
        self._syntax_version = syntax_version

    def start(self):
        self._client = YqlClient(
            server='localhost',
            port=self._yql_api.api.port,
            db='plato'
        )
        self._client.config.legacy_prepare_cell = True

    def stop(self):
        self._client = None

    def __enter__(self):
        self.start()

        return self

    @property
    def client(self):
        return self._client

    def __exit__(self, *args):
        self.stop()

    def execute(self, request_string):
        request = self._client.query(
            query=request_string,
            syntax_version=self._syntax_version,
        )
        request.run()
        return request.get_results()

    def start_execute(self, request_string):
        request = self._client.query(
            query=request_string,
            syntax_version=self._syntax_version,
        )
        request.run()
        return request

    def validate(self, request_string):
        request = self._client.query(
            query=request_string,
            syntax_version=self._syntax_version,
        )
        return request.run()


class YqlTestEnv(BaseEnv):
    def __init__(self, syntax_version=0, **resources):
        try:
            self._yt = resources.pop('yt')  # prevents deepcopying Yt stuff
        except KeyError:
            self._yt = YtResource()

        super(YqlTestEnv, self).__init__(**resources)
        self._request = self.resources['request']
        self.yql_results = None
        self.syntax_version = syntax_version

    def execute(self, path=None):
        with MongoRunner(self.input_dir) as mongo:
            with YtRunner(self._yt) as yt:
                with YqlApiRunner(self.input_dir, mongo, yt) as yql_api:
                    with YqlRequestRunner(yql_api, self.syntax_version) as runner:
                        self.yql_results = runner.execute(self._request.request)
                        self.yql_results.get_results(wait=True)  # Magic is here, wait for done

    @property
    def description(self):
        return 'yql'


class YqlRunnerTestEnv(BaseEnv):
    def __init__(self, syntax_version=0, **resources):
        try:
            self._yt = resources.pop('yt')  # prevents deepcopying Yt stuff
        except KeyError:
            self._yt = YtResource()

        super(YqlRunnerTestEnv, self).__init__(**resources)
        self.syntax_version = syntax_version

        self.mongo = MongoRunner(self.input_dir)
        self.yt = YtRunner(self._yt)
        self.yql_api = YqlApiRunner(self.input_dir, self.mongo, self.yt)
        self.runner = YqlRequestRunner(self.yql_api, self.syntax_version)

    def __enter__(self):
        BaseEnv.__enter__(self)

        self.mongo.start()
        self.yt.start()
        self.yql_api.start()
        self.runner.start()

        return self

    def __exit__(self, *args):
        self.runner.stop()
        self.yql_api.stop()
        self.yt.stop()
        self.mongo.stop()

        BaseEnv.__exit__(self)

    def validate(self, request):
        return self.runner.validate("EXPLAIN VALIDATE " + request.request)

    def execute(self, request):
        return self.runner.execute(request.request).get_results(wait=True)

    def start_execute(self, request):
        return self.runner.start_execute(request.request)

    @property
    def description(self):
        return 'yql_runner'

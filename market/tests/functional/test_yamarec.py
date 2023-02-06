#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function

import yatest.common

import logging
import os
import collections
import multiprocessing
import re
import sys

import edera.testing
import edera.task
import edera.requisite
import edera.helpers

from edera.coroutine import coroutine
from edera.invokers import MultiProcessInvoker
from edera.exceptions import MasterSlaveInvocationError
from edera.parameterizable import Parameter
from edera.parameterizable import Parameterizable
from edera.qualifiers import List, Instance
from yamarec1.tasks import FillDataStorageViaQuery

import yamarec1.workflow
from yamarec1.beans import settings

from testing_cache_storage import _TestingCacheStorage
from task_list import tasks_to_test

from library.python import resource

logger = logging.getLogger(__name__)

os.environ['YQL_DETERMINISTIC_MODE'] = ''
os.environ['LOCAL_YAMAREC_TEST_RUN'] = '1'


def setup_test_env(yql_api, yt, yql_http_file_server):
    os.environ["YQL_SERVER"] = "localhost:{}".format(yql_api.port)
    os.environ["YT_PROXY"] = "localhost:{}".format(yt.yt_proxy_port)

    logger.info("Setup local yql server on {}".format(os.environ["YQL_SERVER"]))
    logger.info("Setup local yt proxy on {}".format(os.environ["YT_PROXY"]))

    def setting_values(setting):
        for key in setting:
            yield setting[key]

    files_to_serve = {}

    files_to_serve.update({
        "libyamarec_udf.so": resource.find("libyamarec_udf.so")
    })

    files_to_serve.update(
        resource.iteritems(prefix='/yamarec_test_assets/', strip_prefix=True)
    )

    filenames_to_serve = {}

    attachments_path = os.environ.get("YAMAREC_ATTACHMENTS_PATH")
    if attachments_path:
        filenames_to_serve.update({
            attachment.name: os.path.join(attachments_path, attachment.name)
            for attachment in setting_values(settings.yql.attachments)
        })

    yql_http_file_server.register_files(files_to_serve, filenames_to_serve)
    os.environ["LOCAL_FILE_SERVER"] = yql_http_file_server.compose_http_host()
    logger.info("Setup local file server on {}".format(os.environ["LOCAL_FILE_SERVER"]))

    os.environ["HOME"] = yatest.common.output_path()


def normalize_requisites(requisite):
    def flatten(seq):
        for e in seq:
            if isinstance(e, collections.Iterable):
                for nested in flatten(e):
                    yield nested
            else:
                yield e

    def _normalize_requisites(requisite):
        if isinstance(requisite, collections.Mapping):
            for v in requisite.values():
                for r in _normalize_requisites(v):
                    if r in requisite:
                        yield _normalize_requisites(requisite[r])
                    yield r
        elif isinstance(requisite, collections.Iterable):
            for item in requisite:
                yield _normalize_requisites(item)
        elif isinstance(requisite, yamarec1.workflow.Task):
            yield requisite
        elif requisite is None or isinstance(requisite, edera.requisite.Requisite):
            pass
        else:
            assert False, "TODO: Such requesite is not supported {}!".format(requisite)
    return flatten(_normalize_requisites(requisite))


class _TestRunner(edera.task.Task, Parameterizable):

    test = Parameter(Instance[edera.testing.Test])
    requisites = Parameter(List[Instance[edera.task.Task]])

    @coroutine
    def execute(self, cc):
        logger.info("self {}.".format(self))

        for req in self.requisites:
            logger.info("req {}.".format(req))
            cc.embrace(req.execute)()
        cc.embrace(self.test.execute)()


class _TestRunnerProcessWrapper(edera.task.Task, Parameterizable):

    testrunners = Parameter(List[Instance[_TestRunner]])

    def _get_random_color(self, basename):
        return edera.helpers.sha1(basename)[:8]

    def _prepare_exec_func(self, cc, testrunner):
        testname = testrunner.test.subject.name

        color = self._get_random_color(testname)

        def _execute():
            os.environ["EDERA_TEST_CLUSTER_COLOR"] = color
            cc.embrace(testrunner.execute)()

        testbasename = re.match("\w*", testname)
        testbasename = testbasename.group(0) if testbasename else "UnknownTask"
        name = "%s/%s@%s" % (multiprocessing.current_process().name, testbasename, color)

        return name, _execute

    @coroutine
    def execute(self, cc):
        runner_dict = dict(self._prepare_exec_func(cc, tr) for tr in self.testrunners)
        MultiProcessInvoker(runner_dict).invoke[cc]()


_Test = type.__new__(type, "Test", (edera.testing.Test,), {"cache": _TestingCacheStorage()})


def build_test_executer(task):
    assert isinstance(task, yamarec1.workflow.Task), "Expected Task, found {}".format(task)
    for scenario in task.tests:
        test = _Test(scenario=scenario, subject=task)
        requisites = []
        for req in normalize_requisites(task.requisite):
            if task == req:
                continue
            if req in test.scenario.restrictions:
                req_replacement = test.scenario.restrictions[req]
            else:
                req_replacement = req.stub
            if req_replacement is None:
                continue
            assert not req_replacement.restrictions, "Transitive restrictions not allowed!"
            stub = edera.testing.Stub(scenario=req_replacement, subject=req)
            requisites.append(stub)
        yield _TestRunner(test=test, requisites=requisites)


def batch_iter(seq, batch_size, drop_rest=False):
    """
    Transforms flat iterator into iterator over lists of elements.

    Example:
    batch_size = 2: [1, 2, 3, 4, 5] -> [[1, 2], [3, 4], [5]]
    """
    batch = []
    assert isinstance(batch_size, int) and batch_size > 0, "batch_size should be positive number"
    for e in seq:
        batch.append(e)
        if len(batch) >= batch_size:
            yield batch
            batch = []
    if batch and not drop_rest:
        yield batch


def test_yamarec_tasks(tmpdir, yql_api, yt, yql_http_file_server):
    if yatest.common.context.test_stderr:
        logger.addHandler(logging.StreamHandler(stream=sys.stderr))

    setup_test_env(yql_api, yt, yql_http_file_server)

    test_subject_regex = yatest.common.get_param('test_subject')

    def select_task(task):
        if not test_subject_regex:
            return True
        return re.match(test_subject_regex, task.name)

    logger.info("Preparing tests...")
    test_runners = [
        test
        for task in tasks_to_test()
        if select_task(task)
        for test in build_test_executer(task)
    ]
    logger.info("{} tests collected".format(len(test_runners)))

    parallel_pool_size = yatest.common.get_param('parallel_pool_size') or 32
    parallel_pool_size = int(parallel_pool_size)

    test_runners = [
        _TestRunnerProcessWrapper(testrunners=tr)
        for tr in batch_iter(test_runners, parallel_pool_size)
    ]
    logger.info("Preparing tests done.")

    total_tests = len(test_runners)
    test_fails = []
    for current_test_number, test in enumerate(test_runners, 1):
        logger.info("Run batch of tests {test}, [{current}/{total}]".format(
            test=test, current=current_test_number, total=total_tests))
        try:
            test.execute()
        except MasterSlaveInvocationError as e:
            test_fails.append({
                "error": e,
                "test": test,
            })
            logger.error("Failed: {}".format(e))

    # user may want to hold yt server running to manually inspect data
    pause_on_exit = yatest.common.get_param('pause_on_exit') or '0'
    pause_on_exit = int(pause_on_exit) if str.isdigit(pause_on_exit) else pause_on_exit
    if pause_on_exit and yatest.common.context.test_stderr:
        print("Server still running yql: {}, yt: {}.\n"
              "Press CTRL+C to exit..."
              .format(os.environ["YQL_SERVER"], os.environ["YT_PROXY"]),
              file=sys.stderr)

        import signal
        signal.pause()

    assert len(test_fails) == 0, "Tests failed!"

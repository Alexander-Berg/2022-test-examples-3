import json
import os
import random
import re
import unittest

import bottle
import multiprocessing
import time

import util.sandbox_report_builder as sandbox_report_builder

SANDBOX_PORT = 2000 + os.getuid() % 30000

EXISTING_VERSIONS = {'2017.3.18'}
TASKS = {}


class Task(object):
    def __init__(self, task_id, report_version):
        self.id = task_id
        self.report_version = report_version


@bottle.route('/task', method='POST')
def fake_create_task_method():
    arcadia_path = bottle.request.json["context"]["checkout_arcadia_from_url"]
    report_version = re.search('report-(.+?)/', arcadia_path).group(1)

    task_id = random.randint(1, 10 ** 9)

    task = Task(task_id, report_version)
    TASKS[task.id] = task

    return json.dumps({'id': task.id})


@bottle.route('/task/<task_id:int>', method='GET')
def fake_check_task_status(task_id):
    if task_id not in TASKS:
        return bottle.abort(404, json.dumps({"reason": "Task #{} not found.".format(task_id)}))

    if TASKS[task_id].report_version not in EXISTING_VERSIONS:
        return json.dumps({'status': 'FAILURE'})

    return json.dumps({'status': 'SUCCESS'})


@bottle.route('/batch/tasks/start', method='PUT')
def fake_start_task():
    result = []
    for task_id in bottle.request.json["id"]:
        if task_id not in TASKS:
            result.append({"status": "ERROR", "message": "Task not exists", "id": task_id})
        else:
            result.append({"status": "SUCCESS", "message": "Task Started", "id": task_id})

    return json.dumps(result)


class T(unittest.TestCase):
    server_process = None

    @classmethod
    def setUpClass(cls):
        sandbox_report_builder.API_HOST = 'http://{host}:{port}/'.format(host='localhost', port=SANDBOX_PORT)
        cls.server_process = multiprocessing.Process(target=bottle.run,
                                                     kwargs={'host': 'localhost', 'port': SANDBOX_PORT})
        cls.server_process.start()
        time.sleep(5)

    @classmethod
    def tearDownClass(cls):
        cls.server_process.terminate()

    def test_success_build(self):
        task_id = sandbox_report_builder.run_build_report_task('2017.3.18', '17.3.18.1', 'MARKETOUT-9999')
        success = sandbox_report_builder.wait_for_task(task_id)
        self.assertTrue(success)

    def test_wrong_version(self):
        task_id = sandbox_report_builder.run_build_report_task('2014.3.18', '14.3.18.1', 'MARKETOUT-9999')
        success = sandbox_report_builder.wait_for_task(task_id)
        self.assertFalse(success)

if __name__ == '__main__':
    unittest.main()

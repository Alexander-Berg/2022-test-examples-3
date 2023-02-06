# -*- coding: utf-8 -*-

import unittest

from context import daas
from daas.button_report.cluster_store import ClusterStore
from daas.button_report.robot import create_options, create_robot
from daas.button_report.data_model import *
from daas.button_report.index_processor import *
from daas.button_report.task_processor import *
from daas.button_report.report_processor import *
import os
import init_data
import time


class Test(unittest.TestCase):

    def _generate_inital_data(self):
        self._options = init_data.gen_init_data()
        self._cluster_store = ClusterStore(self._options)
        init_connection(self._options)
        create_tables(self._options)
'''
    def test_task_processig(self):
        self._generate_inital_data()
        hd = MiniCluster.get(name='hd')
        task = ButtonTask.create(index='101010_2020', cluster=hd,
                                 state=TASK_STATE_ACTIVE, on_disk=False, user='robot',
                                 report_binary_source='COMMIT',
                                 report_binary_location='')

        robot = create_robot(self._options, self._cluster_store)
        robot.start()
        notifier = robot.get_notifier()
        notifier.notify_task_changed()
        time.sleep(1)
        robot.stop()
        report_task = ReportTask.get(ReportTask.button_task == task.id)
        self.assertEqual(report_task.state, REPORT_STATE_QUEUED_UPLOAD)
'''

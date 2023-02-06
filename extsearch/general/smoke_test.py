#!/usr/bin/env python

import yatest.common

import subprocess
import unittest


class SmokeTest(unittest.TestCase):

    def test_initial(self):
        subprocess.check_call([yatest.common.binary_path("extsearch/images/tools/yt_infra/yt_state_tool/yt_state_tool"), "-h"])
        subprocess.check_call([yatest.common.binary_path("extsearch/images/tools/yt_infra/yt_queue_tool/yt_queue_tool"), "-h"])
        subprocess.check_call([yatest.common.binary_path("extsearch/images/tools/yt_infra/yt_operation_tool/yt_operation_tool"), "-h"])

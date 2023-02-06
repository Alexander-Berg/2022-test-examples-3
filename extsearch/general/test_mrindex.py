#!/usr/bin/env python
# -*- coding: utf-8 -*-
# to run:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long
# to run and canonize:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long -Z
#  svn commit ...

import os
import sys

import testlib
from extsearch.images.robot.index.it.pack import index_pack

print sys.path
print os.environ


class TestMrindex(testlib.YtRegressionTestBase):
    pack = index_pack

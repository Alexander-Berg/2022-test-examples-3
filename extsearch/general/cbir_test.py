#!/usr/bin/env python
# -*- coding: utf-8 -*-
# to run:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long
# to run and canonize:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long -Z
#  svn commit ...

import testlib
from extsearch.images.robot.index.cbir.it.pack import cbir_pack


class TestCbir(testlib.YtRegressionTestBase):
    pack = cbir_pack

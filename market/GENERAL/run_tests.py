#!/usr/bin/python
# -*- coding: utf-8 -*-

import logging
import os.path
import sys
import unittest
import os
import subprocess
import tempfile
import shutil
import time
import glob

modules = []
for path in glob.glob(os.path.join('daas/tests', 'test_*.py')):
    path = os.path.splitext(os.path.basename(path))[0]
    module = '.'.join(['daas.tests', path])
    modules.append(module)

tests = unittest.TestLoader().loadTestsFromNames(modules)
t = unittest.TextTestRunner(verbosity=2)
result = t.run(tests)

if not result.wasSuccessful():
    sys.exit(1)

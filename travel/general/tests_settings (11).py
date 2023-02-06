# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.tasks.settings import *  # noqa

from common.settings.configuration import Configuration

Configuration().apply(globals())
import os
os.environ.setdefault('RASP_TEST_APPLIED_CONFIG', globals()['APPLIED_CONFIG'])

from common.tester.settings import *  # noqa

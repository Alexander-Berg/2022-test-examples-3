# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

from travel.rasp.rasp_scripts.settings import *  # noqa
from common.settings.configuration import Configuration

Configuration().apply(globals())
os.environ.setdefault('RASP_TEST_APPLIED_CONFIG', globals()['APPLIED_CONFIG'])

from common.tester.settings import *  # noqa

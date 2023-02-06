import logging
import pytest
import yatest.common
import os

from test_bundle_codecs_common import *

def test_stripped_bundles():
    checker = TBundleCodecChecker("stripped", 100000)
    checker.run()

def test_stripped_bundles_v2():
    checker = TBundleCodecChecker("stripped_v2", 100000)
    checker.run()

def test_reg_stripped_bundles_v2():
    checker = TBundleCodecChecker("stripped_v2", 20000, bundles_path = input_reg_bundles)
    checker.run()

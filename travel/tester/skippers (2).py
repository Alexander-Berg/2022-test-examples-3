# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os
import sys

import pytest

route_search_skip_reason = 'There is no route_search submodule'
try:
    import route_search  # noqa
    HAS_ROUTE_SEARCH = True
except ImportError:
    HAS_ROUTE_SEARCH = False

has_route_search = pytest.mark.skipif(not HAS_ROUTE_SEARCH, reason=route_search_skip_reason)


stationschedule_skip_reason = 'There is no stationschedule submodule'
try:
    import stationschedule  # noqa
    HAS_STATIONSCHEDULE = True
except ImportError:
    HAS_STATIONSCHEDULE = False

has_stationschedule = pytest.mark.skipif(not HAS_STATIONSCHEDULE, reason=stationschedule_skip_reason)


hemi_skip_reason = 'hemi is not installed'
try:
    import hemi  # noqa
    HAS_HEMI_INSTALLED = True
except ImportError:
    HAS_HEMI_INSTALLED = False

has_hemi = pytest.mark.skipif(not HAS_HEMI_INSTALLED, reason=hemi_skip_reason)


skip_in_arcadia = pytest.mark.skipif(os.getenv('YA_TEST_RUNNER', False), reason="Can't run in Arcadia")
not_macos = pytest.mark.skipif(sys.platform.startswith('darwin'), reason="Can't run on MacOS")

# -*- coding: utf-8 -*-
from __future__ import absolute_import

import pytest
from checks import create_manifest


@pytest.fixture
def manifest():
    return create_manifest()

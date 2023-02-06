# -*- coding: utf-8 -*-
from __future__ import absolute_import


import os
import pytest
import checks.nginx as nginx_check

from utils import CLOUD_TYPE_VAR


BSCONFIG_IDIR = "/tmp/"


@pytest.mark.parametrize(
    argnames="cloud_type,result",
    argvalues=(
        ("nanny", BSCONFIG_IDIR + "pids/nginx.pid"),
        ("deploy", "/var/run/nginx.pid"),
    )
)
def test_get_pidfile(cloud_type, result):
    os.environ["BSCONFIG_IDIR"] = BSCONFIG_IDIR
    os.environ[CLOUD_TYPE_VAR] = cloud_type

    assert nginx_check.get_pidfile() == result
